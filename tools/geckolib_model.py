import json
import math
import os


class Model:
    def __init__(self, identifier, tex, bounds_width, bounds_height, bounds_offset):
        self.identifier = identifier
        self.tex = tex
        self.bounds = (bounds_width, bounds_height, bounds_offset)
        self.bones = []

    def bone(self, name, parent, pivot, rotation=None, cubes=()):
        self.bones.append({"name": name, "parent": parent, "pivot": list(pivot), "rotation": rotation,
                           "cubes": [{"origin": list(o), "size": list(s), "material": m, "inflate": i} for (o, s, m, i) in cubes]})

    def pack(self):
        cubes = [cube for b in self.bones for cube in b["cubes"]]
        order = sorted(cubes, key=lambda cube: (-net_size(cube["size"])[1], -net_size(cube["size"])[0]))
        x = y = shelf = 0
        for cube in order:
            nw, nh = net_size(cube["size"])
            if x + nw > self.tex:
                x, y, shelf = 0, y + shelf, 0
            if y + nh > self.tex:
                raise SystemExit("texture full")
            cube["uv"] = [x, y]
            x += nw
            shelf = max(shelf, nh)

    def geometry(self):
        bones = []
        for b in self.bones:
            out = {"name": b["name"]}
            if b["parent"]:
                out["parent"] = b["parent"]
            out["pivot"] = b["pivot"]
            if b["rotation"]:
                out["rotation"] = b["rotation"]
            if b["cubes"]:
                out["cubes"] = []
                for cube in b["cubes"]:
                    cj = {"origin": cube["origin"], "size": cube["size"], "uv": cube["uv"]}
                    if cube["inflate"]:
                        cj["inflate"] = cube["inflate"]
                    out["cubes"].append(cj)
            bones.append(out)
        width, height, offset = self.bounds
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": self.identifier, "texture_width": self.tex, "texture_height": self.tex,
                            "visible_bounds_width": width, "visible_bounds_height": height, "visible_bounds_offset": offset},
            "bones": bones}]}


def c(origin, size, material, inflate=0):
    return (origin, size, material, inflate)


def net_size(size):
    w, h, d = size
    return 2 * (w + d), d + h


def kf(frames):
    return {f"{t:.4g}" if t % 1 else f"{t:.1f}": {"post": v, "lerp_mode": "catmullrom"} for t, v in sorted(frames.items())}


def anim(length, loop, bones):
    return {"loop": loop, "animation_length": length,
            "bones": {name: {k: kf(v) for k, v in channels.items()} for name, channels in bones.items()}}


def loop(length, frames):
    return {round(t, 3): v for t, v in frames} | {length: frames[0][1]}


def animations(name, clips):
    return {"format_version": "1.8.0", "animations": {f"animation.{name}.{clip}": a for clip, a in clips.items()}}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=1)
        f.write("\n")


def faces_of(cube):
    (u, v), (w, h, d) = cube["uv"], [int(s) for s in cube["size"]]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def texels(bones):
    for b in bones:
        for cube in b["cubes"]:
            for face, (x0, y0, fw, fh) in faces_of(cube).items():
                for y in range(fh):
                    for x in range(fw):
                        yield cube, face, x, y, fw, fh, (x0 + x, y0 + y)


def face_corners(x0, y0, z0, x1, y1, z1):
    return {"right": ((x0, y1, z1), (x0, y1, z0), (x0, y0, z1)), "front": ((x0, y1, z0), (x1, y1, z0), (x0, y0, z0)),
            "left": ((x1, y1, z0), (x1, y1, z1), (x1, y0, z0)), "back": ((x1, y1, z1), (x0, y1, z1), (x1, y0, z1)),
            "top": ((x0, y1, z1), (x1, y1, z1), (x0, y1, z0)), "bottom": ((x0, y0, z0), (x1, y0, z0), (x0, y0, z1))}


def texel_point(cube, face, x, y):
    (x0, y0, z0), (w, h, d) = cube["origin"], cube["size"]
    p0, pu, pv = face_corners(x0, y0, z0, x0 + w, y0 + h, z0 + d)[face]
    _, _, fw, fh = faces_of(cube)[face]
    su, sv = (x + 0.5) / fw, (y + 0.5) / fh
    return tuple(p0[i] + (pu[i] - p0[i]) * su + (pv[i] - p0[i]) * sv for i in range(3))


def perimeter_x(cube, face, x):
    w, _, d = [int(s) for s in cube["size"]]
    return {"right": 0, "front": d, "left": d + w, "back": 2 * d + w}[face] + x


def wrap_angle(cube, face, x):
    w, _, d = [int(s) for s in cube["size"]]
    return 2 * math.pi * (perimeter_x(cube, face, x) + 0.5 - (d + w / 2)) / (2 * (w + d))


def rot_matrix(rx, ry, rz):
    import numpy as np
    a, b, g = (math.radians(v) for v in (-rx, -ry, rz))
    Rx = np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])
    Ry = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    Rz = np.array([[math.cos(g), -math.sin(g), 0], [math.sin(g), math.cos(g), 0], [0, 0, 1]])
    F = np.diag([-1, 1, 1])
    return F @ Rz @ Ry @ Rx @ F
