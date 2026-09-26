"""Software preview of the Ice Lich GeckoLib model. Run from the repo root:
    python3 tools/preview_lich.py OUT_DIR [animation_name@seconds ...]

Loads the geo JSON, texture and glowmask, applies bone pivots/rotations (and optionally one animation
pose, interpolated linearly between keyframes) the way GeckoLib does, and z-buffer rasterises every
textured, alpha-tested cube face from several angles. Writes one PNG per pose: front, 3/4, side, back,
a dark "night" view where only the glowmask is lit, and a small in-game-sized thumbnail.
"""
import json
import math
import sys

import numpy as np
from PIL import Image

GEO = "src/main/resources/assets/wildspellmobs/geo/entity/ice_lich.geo.json"
ANIM = "src/main/resources/assets/wildspellmobs/animations/entity/ice_lich.animation.json"
TEX = "src/main/resources/assets/wildspellmobs/textures/entity/ice_lich.png"
GLOW = "src/main/resources/assets/wildspellmobs/textures/entity/ice_lich_glowmask.png"


def rot_matrix(rx, ry, rz):
    """Bedrock rotation (degrees) as a matrix acting on Bedrock-space points. GeckoLib negates X of
    positions and the X/Y rotation angles, then applies Rz * Ry * Rx in Java space; conjugating by the
    X flip gives this matrix in Bedrock space."""
    a, b, g = (math.radians(v) for v in (-rx, -ry, rz))
    Rx = np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])
    Ry = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    Rz = np.array([[math.cos(g), -math.sin(g), 0], [math.sin(g), math.cos(g), 0], [0, 0, 1]])
    F = np.diag([-1, 1, 1])
    return F @ Rz @ Ry @ Rx @ F


def cube_faces(cube):
    """Six textured faces of a box-UV cube in Bedrock space: (P0, Pu, Pv, u0, v0, fw, fh, normal).
    P0 is the corner at the texture's top-left, Pu the top-right, Pv the bottom-left. The net is
    [right side (-X)][front (-Z)][left side (+X)][back (+Z)], each seen from outside, unmirrored."""
    (x0, y0, z0), (w, h, d) = cube["origin"], cube["size"]
    i = cube.get("inflate", 0)
    X0, Y0, Z0, X1, Y1, Z1 = x0 - i, y0 - i, z0 - i, x0 + w + i, y0 + h + i, z0 + d + i
    u, v = cube["uv"]
    w, h, d = int(w), int(h), int(d)
    P = lambda x, y, z: np.array([x, y, z], float)
    return [
        (P(X0, Y1, Z1), P(X0, Y1, Z0), P(X0, Y0, Z1), u, v + d, d, h, (-1, 0, 0)),                  # right side
        (P(X0, Y1, Z0), P(X1, Y1, Z0), P(X0, Y0, Z0), u + d, v + d, w, h, (0, 0, -1)),              # front
        (P(X1, Y1, Z0), P(X1, Y1, Z1), P(X1, Y0, Z0), u + d + w, v + d, d, h, (1, 0, 0)),           # left side
        (P(X1, Y1, Z1), P(X0, Y1, Z1), P(X1, Y0, Z1), u + 2 * d + w, v + d, w, h, (0, 0, 1)),       # back
        (P(X0, Y1, Z1), P(X1, Y1, Z1), P(X0, Y1, Z0), u + d, v, w, d, (0, 1, 0)),                   # top
        (P(X0, Y0, Z0), P(X1, Y0, Z0), P(X0, Y0, Z1), u + d + w, v, w, d, (0, -1, 0)),              # bottom
    ]


def sample(channel, t):
    times = sorted(channel, key=float)
    vals = [np.array(channel[k]["post"] if isinstance(channel[k], dict) else channel[k], float) for k in times]
    ts = [float(k) for k in times]
    if t <= ts[0]:
        return vals[0]
    for (ta, va), (tb, vb) in zip(zip(ts, vals), zip(ts[1:], vals[1:])):
        if ta <= t <= tb:
            k = (t - ta) / (tb - ta) if tb > ta else 0
            return va + (vb - va) * k
    return vals[-1]


def world_faces(geo, pose=None):
    bones = {b["name"]: b for b in geo["bones"]}
    cache = {}

    def transform(name):
        if name in cache:
            return cache[name]
        b = bones[name]
        rot = np.array(b.get("rotation", [0, 0, 0]), float)
        pos = np.zeros(3)
        if pose and name in pose:
            rot = rot + pose[name].get("rotation", 0)
            pos = pos + pose[name].get("position", 0)
        piv = np.array(b["pivot"], float)
        R = rot_matrix(*rot)
        local = lambda p: R @ (p - piv) + piv + pos
        parent = transform(b["parent"]) if "parent" in b else (lambda p: p)
        cache[name] = lambda p: parent(local(p))
        return cache[name]

    faces = []
    for b in geo["bones"]:
        tf = transform(b["name"])
        for cube in b.get("cubes", []):
            for (P0, Pu, Pv, u0, v0, fw, fh, n) in cube_faces(cube):
                a, bu, bv = tf(P0), tf(Pu), tf(Pv)
                normal = np.cross(bu - a, bv - a)
                ln = np.linalg.norm(normal)
                faces.append((a, bu, bv, u0, v0, fw, fh, normal / ln if ln else np.zeros(3)))
    return faces


def render(faces, tex, glow, yaw, scale, night=False, size=(620, 660), ground=None, cy=23):
    """Orthographic view from in front of the lich, turned by `yaw` degrees (positive shows his left
    side). Screen right is his left (+X) in the front view."""
    W, H = size
    img = np.zeros((H, W, 3), float)
    top = np.array([28, 34, 48]) if not night else np.array([6, 8, 14])
    bot = np.array([60, 66, 78]) if not night else np.array([10, 12, 20])
    for y in range(H):
        img[y] = top + (bot - top) * y / H
    zbuf = np.full((H, W), np.inf)
    c, s = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))

    def proj(p):
        x, y, z = p
        xr = x * c - z * s
        zr = x * s + z * c
        return np.array([W / 2 + xr * scale, H / 2 - (y - cy) * scale, zr])

    if ground is not None:
        gy = int(H / 2 + cy * scale)
        img[gy:gy + 2, :] = [90, 110, 130]
    light = np.array([0.35, 0.8, -0.5])
    light = light / np.linalg.norm(light)
    th, tw = tex.shape[:2]
    for (a, bu, bv, u0, v0, fw, fh, n) in faces:
        if fw == 0 or fh == 0:
            continue
        A, U, V = proj(a), proj(bu), proj(bv)
        M = np.array([[U[0] - A[0], V[0] - A[0]], [U[1] - A[1], V[1] - A[1]]])
        det = np.linalg.det(M)
        if abs(det) < 1e-6:
            continue
        Minv = np.linalg.inv(M)
        corners = [A, U, V, U + V - A]
        xs = [p[0] for p in corners]
        ys = [p[1] for p in corners]
        x0, x1 = max(0, int(math.floor(min(xs)))), min(W, int(math.ceil(max(xs))) + 1)
        y0, y1 = max(0, int(math.floor(min(ys)))), min(H, int(math.ceil(max(ys))) + 1)
        if x0 >= x1 or y0 >= y1:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1) + 0.5, np.arange(y0, y1) + 0.5)
        dx, dy = gx - A[0], gy - A[1]
        st_s = Minv[0, 0] * dx + Minv[0, 1] * dy
        st_t = Minv[1, 0] * dx + Minv[1, 1] * dy
        inside = (st_s >= 0) & (st_s < 1) & (st_t >= 0) & (st_t < 1)
        if not inside.any():
            continue
        depth = A[2] + (U[2] - A[2]) * st_s + (V[2] - A[2]) * st_t
        tu = np.clip((u0 + st_s * fw).astype(int), 0, tw - 1)
        tv = np.clip((v0 + st_t * fh).astype(int), 0, th - 1)
        texel = tex[tv, tu]
        g = glow[tv, tu]
        ok = inside & (texel[..., 3] > 25) & (depth < zbuf[y0:y1, x0:x1])
        if not ok.any():
            continue
        shade = 0.55 + 0.45 * max(0.0, float(np.dot(n, light))) if not night else 0.22
        # no-cull: back faces use the same shading as the flipped normal
        if np.dot(n, light) < 0:
            shade = 0.55 + 0.45 * max(0.0, float(np.dot(-n, light))) * 0.4 if not night else 0.22
        col = texel[..., :3] * shade
        ga = g[..., 3:4] / 255.0
        col = col * (1 - ga) + g[..., :3] * ga
        region = img[y0:y1, x0:x1]
        region[ok] = col[ok]
        zbuf[y0:y1, x0:x1][ok] = depth[ok]
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))


def pose_at(anims, name, t):
    a = anims["animations"][name]
    pose = {}
    for bone, chans in a["bones"].items():
        pose[bone] = {k: sample(v, t) for k, v in chans.items() if k in ("rotation", "position")}
    return pose


def sheet(faces, tex, glow, title):
    views = [render(faces, tex, glow, 0, 12, ground=True), render(faces, tex, glow, 35, 12, ground=True),
             render(faces, tex, glow, -35, 12, ground=True), render(faces, tex, glow, 90, 12, ground=True),
             render(faces, tex, glow, 180, 12, ground=True), render(faces, tex, glow, 25, 12, night=True)]
    W, H = views[0].size
    out = Image.new("RGB", (W * 3, H * 2))
    for i, v in enumerate(views):
        out.paste(v, ((i % 3) * W, (i // 3) * H))
    return out


def main():
    out_dir = sys.argv[1]
    geo = json.load(open(GEO))["minecraft:geometry"][0]
    anims = json.load(open(ANIM))
    tex = np.array(Image.open(TEX).convert("RGBA"), float)
    try:
        glow = np.array(Image.open(GLOW).convert("RGBA"), float)
    except FileNotFoundError:
        glow = np.zeros_like(tex)
    faces = world_faces(geo)
    sheet(faces, tex, glow, "rest").save(f"{out_dir}/lich_rest.png")
    # Close-up of the head and chest, front and 3/4.
    close = Image.new("RGB", (1200, 700))
    for i, yaw in enumerate((0, 30)):
        full = render(faces, tex, glow, yaw, 30, size=(600, 1300))
        close.paste(full.crop((0, 0, 600, 700)), (i * 600, 0))
    close.save(f"{out_dir}/lich_close.png")
    # Distance check: roughly how big he is on screen a few blocks away.
    small = render(faces, tex, glow, 25, 3, size=(160, 170), ground=True)
    small.resize((480, 510), Image.NEAREST).save(f"{out_dir}/lich_small.png")
    for spec in sys.argv[2:]:
        name, t = spec.split("@")
        full = f"animation.ice_lich.{name}"
        faces = world_faces(geo, pose_at(anims, full, float(t)))
        views = [render(faces, tex, glow, yaw, 8, ground=True, size=(520, 720), cy=32) for yaw in (0, 40, 90)]
        out = Image.new("RGB", (520 * 3, 720))
        for i, v in enumerate(views):
            out.paste(v, (i * 520, 0))
        out.save(f"{out_dir}/lich_{name}_{t}.png")
    print("previews written to", out_dir)


if __name__ == "__main__":
    main()
