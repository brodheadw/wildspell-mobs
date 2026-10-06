import math

from PIL import Image

import make_diana_model as model
from geckolib_model import texel_point, texels, wrap_angle
from painting import hash01, mix, ramp

OUT = "src/main/resources/assets/wildspellmobs/textures/entity"
PHASES = 5
KEY = -0.5

MOODS = {
    "diana": dict(
        highland=(236, 240, 246), mare=(146, 156, 178), crater=(255, 255, 255),
        dark=(38, 42, 62), dark_mare=(26, 28, 44),
        robe=[(14, 16, 40), (26, 30, 70), (40, 48, 104), (62, 74, 140), (96, 112, 178)],
        night=[(6, 8, 20), (12, 16, 38), (20, 26, 58), (32, 40, 84)],
        star=(226, 234, 255), star_rate=0.07,
        skin=[(120, 116, 146), (170, 166, 192), (208, 206, 226), (232, 230, 242), (248, 248, 255)],
        silver=[(90, 98, 128), (146, 154, 184), (196, 204, 226), (234, 238, 250)],
        bow=(214, 230, 255), bow_edge=(150, 176, 230), string=(236, 242, 255),
    ),
    "diana_grief": dict(
        highland=(214, 98, 64), mare=(128, 42, 36), crater=(255, 170, 120),
        dark=(36, 18, 22), dark_mare=(24, 10, 14),
        robe=[(22, 20, 28), (40, 36, 48), (62, 56, 72), (86, 80, 98), (112, 104, 124)],
        night=[(10, 4, 8), (20, 8, 14), (34, 12, 20), (52, 18, 26)],
        star=(230, 120, 96), star_rate=0.03,
        skin=[(84, 70, 84), (126, 110, 124), (168, 152, 166), (202, 188, 198), (226, 214, 222)],
        silver=[(60, 40, 44), (104, 70, 72), (150, 106, 104), (192, 148, 140)],
        bow=(232, 150, 120), bow_edge=(170, 80, 66), string=(240, 190, 170),
    ),
}

MARIA = [
    ((-0.30, 0.34), (0.24, 0.19)),
    ((-0.60, -0.02), (0.17, 0.34)),
    ((0.28, 0.30), (0.14, 0.13)),
    ((0.40, 0.04), (0.12, 0.16)),
    ((0.72, 0.22), (0.08, 0.09)),
    ((-0.20, -0.40), (0.17, 0.11)),
]
CRATERS = [(0.18, -0.70)]


def lit(cube, face, x, fh, y, gain=0.0):
    if face == "top":
        return 0.82 + gain
    if face == "bottom":
        return 0.12 + gain
    facing = math.cos(wrap_angle(cube, face, x) - KEY)
    return 0.42 + 0.30 * facing + 0.2 * max(0.0, facing) ** 8 - 0.14 * y / max(1, fh - 1) + gain


def terminator(phase):
    left = 1.0 - phase / (PHASES - 0.6)
    return math.cos(math.pi * left)


def m_moon(m, phase, face, x, y, fw, fh, cube):
    px, py, pz = texel_point(cube, face, x, y)
    n = (px - model.MOON_CENTER[0], py - model.MOON_CENTER[1], pz - model.MOON_CENTER[2])
    length = math.sqrt(sum(v * v for v in n))
    n = tuple(v / length for v in n)
    u, v = -n[0], n[1]
    wobble = 0.25 * (hash01(int(u * 9 + 20), int(v * 9 + 20), 7) - 0.5)
    mare = n[2] < 0.2 and any(((u - cx) / rx) ** 2 + ((v - cy) / ry) ** 2 < 1.0 + wobble for (cx, cy), (rx, ry) in MARIA)
    crater = n[2] < 0 and any(math.hypot(u - cx, v - cy) < 0.11 for cx, cy in CRATERS)
    light = n[0] * 0.96 + n[1] * 0.28 - terminator(phase)
    if phase == 0 or light > 0.08:
        color = m["crater"] if crater else m["mare"] if mare else m["highland"]
        if light < 0.3:
            color = mix(color, m["mare"], 0.35)
        return color, 0
    return (m["dark_mare"] if mare else m["dark"]), 0


def veil_px(m, face, x, y, fw, fh, cube, gain=0.0):
    at = (cube["uv"][0] + x, cube["uv"][1] + y)
    if hash01(at[0], at[1], 3) < m["star_rate"] and face != "bottom":
        bright = hash01(at[0], at[1], 5) < 0.35
        return m["star"], 255 if bright else 130
    level = 0.55 + gain - 0.25 * y / max(1, fh - 1) + (0.12 if x % 3 == 0 else 0.0)
    if face in ("left", "right", "bottom"):
        level -= 0.2
    return ramp(m["night"], level), 0


def m_veil(m, phase, face, x, y, fw, fh, cube):
    return veil_px(m, face, x, y, fw, fh, cube)


def m_veil_end(m, phase, face, x, y, fw, fh, cube):
    if face in ("front", "back") and y >= fh - 3:
        ragged = (x * 7 + 3) % 5
        if y >= fh - 1 - ragged % 3:
            return None, 0
    return veil_px(m, face, x, y, fw, fh, cube, -0.1)


def m_chiton(m, phase, face, x, y, fw, fh, cube):
    if face == "front":
        if y == fh - 1 or (x + y == 5):
            return ramp(m["silver"], 0.9), 0
        fold = 0.12 if x % 2 == 0 else -0.04
        return ramp(m["robe"], lit(cube, face, x, fh, y, fold)), 0
    if face == "top":
        return ramp(m["robe"], 0.75), 0
    return ramp(m["robe"], lit(cube, face, x, fh, y, 0.08 if x % 2 == 0 else -0.04)), 0


def m_skirt(m, phase, face, x, y, fw, fh, cube):
    if face in ("top", "bottom"):
        return m["robe"][0], 0
    return ramp(m["robe"], lit(cube, face, x, fh, y, 0.1 if x % 2 == 0 else -0.08)), 0


def m_skirt_hem(m, phase, face, x, y, fw, fh, cube):
    if face in ("top", "bottom"):
        return m["robe"][0], 0
    if y == fh - 1:
        return ramp(m["silver"], 0.75 if x % 2 else 0.95), 0
    return ramp(m["robe"], lit(cube, face, x, fh, y, -0.05)), 0


def m_skin(m, phase, face, x, y, fw, fh, cube):
    return ramp(m["skin"], lit(cube, face, x, fh, y, 0.05)), 0


def m_thigh(m, phase, face, x, y, fw, fh, cube):
    return ramp(m["skin"], lit(cube, face, x, fh, y, 0.06)), 0


def m_shin(m, phase, face, x, y, fw, fh, cube):
    if face == "bottom":
        return m["silver"][0], 0
    lace = y >= fh - 4 and (x + y) % 2 == 0
    if y >= fh - 1 or lace:
        return ramp(m["silver"], lit(cube, face, x, fh, y, 0.2)), 0
    return ramp(m["skin"], lit(cube, face, x, fh, y, 0.0)), 0


def m_bracer(m, phase, face, x, y, fw, fh, cube):
    if y >= 2:
        return ramp(m["silver"], lit(cube, face, x, fh, y, 0.12 if y % 2 == 0 else -0.05)), 0
    return ramp(m["skin"], lit(cube, face, x, fh, y)), 0


def m_bow(m, phase, face, x, y, fw, fh, cube):
    if face in ("front", "back", "left", "right") and x == 0:
        return m["bow_edge"], 255
    return m["bow"], 255


def m_horn(m, phase, face, x, y, fw, fh, cube):
    return (m["string"] if y == 0 else m["bow"]), 255


def m_string(m, phase, face, x, y, fw, fh, cube):
    return m["string"], 200


MATERIALS = {name[2:]: fn for name, fn in globals().items() if name.startswith("m_")}


def paint(prefix, m, phase):
    skin = Image.new("RGBA", (model.TEX, model.TEX), (0, 0, 0, 0))
    mask = Image.new("RGBA", (model.TEX, model.TEX), (0, 0, 0, 0))
    for cube, face, x, y, fw, fh, at in texels(model.BONES):
        px, glow = MATERIALS[cube["material"]](m, phase, face, x, y, fw, fh, cube)
        if px is None:
            continue
        skin.putpixel(at, tuple(px) + (255,))
        if glow:
            mask.putpixel(at, (255, 255, 255, glow))
    skin.save(f"{OUT}/{prefix}_{phase}.png")
    mask.save(f"{OUT}/{prefix}_{phase}_glowmask.png")


def paint_arrow():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    m = MOODS["diana"]
    for x in range(16):
        t = 1 - x / 15
        for y in range(5):
            spread = 2 if x < 3 else 1 if x > 12 else 0
            if abs(y - 2) > spread:
                continue
            if y == 2:
                img.putpixel((x, y), (255, 255, 255, 255))
            else:
                img.putpixel((x, y), (m["bow_edge"] if x > 12 else m["string"]) + (int(150 + 100 * t),))
    for x in range(5):
        for y in range(5, 10):
            d = math.hypot(x - 2, y - 7)
            if d < 2.5:
                img.putpixel((x, y), m["string"] + (int(255 * (1 - d / 2.5)),))
    img.save(f"{OUT}/moon_arrow.png")


if __name__ == "__main__":
    for prefix, mood in MOODS.items():
        for phase in range(PHASES):
            paint(prefix, mood, phase)
    paint_arrow()
    print("diana textures written")
