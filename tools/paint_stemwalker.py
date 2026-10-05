import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import make_stemwalker_model as model  # noqa: E402
from geckolib_model import texels  # noqa: E402
from painting import mix  # noqa: E402

OUT = "src/main/resources/assets/wildspellmobs/textures/entity"

STEM = [(122, 114, 108), (160, 152, 142), (190, 182, 170), (210, 203, 190), (226, 220, 208)]
MYCELIUM = [(70, 60, 74), (98, 88, 102), (128, 118, 130), (156, 148, 156)]
CAP = [(64, 10, 12), (92, 16, 18), (122, 24, 24), (150, 34, 30), (176, 52, 44)]
SPOT = (206, 192, 170)
GILL = [(34, 14, 18), (54, 22, 26), (78, 34, 36), (104, 52, 50)]
PORE = (222, 236, 186)
PORES = {("gill_face", 2, 1), ("gill_face", 5, 2), ("gills", 3, 6)}


def h(x, y, salt):
    v = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    return ((v * 2654435761) & 0xFFFFFFFF) / 0xFFFFFFFF


def ramp(palette, level):
    return palette[int(round(max(0.0, min(1.0, level)) * (len(palette) - 1)))]


SHADE = {"top": 0.12, "front": 0.0, "back": -0.06, "left": -0.04, "right": -0.04, "bottom": -0.25}


def stem(face, x, y, fw, fh, salt, dark=0.0):
    grain = h(x, 0, salt) * 0.5 + h(x, y // 3, salt + 1) * 0.25
    level = 0.62 + 0.25 * grain - 0.18 * (y / max(1, fh - 1)) ** 2 + SHADE[face] - dark
    if h(x, y, salt + 2) > 0.94:
        level -= 0.25
    color = ramp(STEM, level)
    if face == "bottom" or (y >= fh - 1 and fh > 3):
        color = mix(color, MYCELIUM[2], 0.5)
    return color


def trunk(face, x, y, fw, fh, salt):
    twist = (x + y // 2) % max(2, fw)
    color = veined(face, twist, y, fw, fh, salt, 0.3)
    if face != "front" and h(twist, y // 3, salt + 11) > 0.8:
        color = mix(color, MYCELIUM[1], 0.55)
    if face == "front" and x == fw // 2 and 2 <= y <= fh - 2:
        color = mix(color, GILL[0], 0.75)
    if face == "front" and abs(x - fw // 2) == 1 and 3 <= y <= fh - 3 and h(x, y, salt) > 0.5:
        color = mix(color, GILL[2], 0.4)
    return color


def root(face, x, y, fw, fh, salt):
    level = 0.35 + 0.5 * h(x, y, salt) + SHADE[face]
    return ramp(MYCELIUM, level)


def finger(face, x, y, fw, fh, salt):
    color = stem(face, x, y, fw, fh, salt, 0.12)
    return mix(color, MYCELIUM[1], (y / max(1, fh - 1)) ** 1.5)


def cap(face, x, y, fw, fh, salt):
    if face == "bottom":
        return gills(face, x, y, fw, fh, salt)
    light = 0.55 + SHADE[face] * 1.5 + 0.2 * (h(x // 2, y // 2, salt) - 0.5)
    if face != "top":
        light -= 0.25 * (y / max(1, fh - 1))
    color = ramp(CAP, light)
    if h(x // 2, y // 2, salt + 7) > 0.92 and h(x, y, salt + 3) > 0.35:
        color = mix(color, SPOT, 0.8 if face == "top" else 0.6)
    if face == "top" and h(x, y, salt + 9) > 0.97:
        color = mix(color, (240, 190, 180), 0.5)
    return color


def gills(face, x, y, fw, fh, salt):
    if face == "top":
        return ramp(CAP, 0.4)
    lamella = (x % 2 == 0) if face in ("front", "back", "bottom") else (y % 2 == 0)
    level = (0.6 if lamella else 0.15) + 0.2 * (h(x, y, salt) - 0.5)
    return ramp(GILL, level)


def veined(face, x, y, fw, fh, salt, reach):
    color = stem(face, x, y, fw, fh, salt, 0.04)
    climb = (y / max(1, fh - 1)) - (1.0 - reach)
    vein = h(x, 0, salt + 5) > 0.45 and h(x, y // 2, salt + 6) > 0.35
    if climb > 0 and (vein or climb > 0.5):
        color = mix(color, MYCELIUM[1 if vein else 2], min(0.85, 0.35 + climb))
    return color


def shard(face, x, y, fw, fh, salt):
    color = stem(face, x, y, fw, fh, salt, 0.12)
    return mix(color, GILL[1], 0.6) if y == 0 or face == "top" else color


def thread(face, x, y, fw, fh, salt):
    return mix(ramp(MYCELIUM, 0.5 + 0.3 * h(x, y, salt)), STEM[3], (y / max(1, fh - 1)) * 0.4)


MATERIALS = {
    "stem": lambda f, x, y, fw, fh, s: veined(f, x, y, fw, fh, s, 0.35), "stem_thin": lambda f, x, y, fw, fh, s: stem(f, x, y, fw, fh, s, 0.06), "trunk": trunk, "root": root, "finger": finger,
    "thigh": lambda f, x, y, fw, fh, s: veined(f, x, y, fw, fh, s, 0.25),
    "shin": lambda f, x, y, fw, fh, s: veined(f, x, y, fw, fh, s, 0.85),
    "shard": shard,
    "cap": cap, "cap_brim": cap, "gills": gills, "gill_face": gills, "thread": thread,
}


def main():
    image = Image.new("RGBA", (model.TEX, model.TEX), (0, 0, 0, 0))
    glow = Image.new("RGBA", (model.TEX, model.TEX), (0, 0, 0, 0))
    for index, (cube, face, x, y, fw, fh, (u, v)) in enumerate(texels(model.BONES)):
        material = cube["material"]
        salt = sum(map(ord, material)) + int(cube["origin"][0] * 7 + cube["origin"][1] * 13 + cube["origin"][2] * 17)
        color = MATERIALS[material](face, x, y, fw, fh, salt)
        if (material, x, y) in PORES and face in ("front", "bottom"):
            color = PORE
            glow.putpixel((u, v), PORE + (255,))
        image.putpixel((u, v), tuple(color) + (255,))
    image.save(f"{OUT}/stemwalker.png")
    glow.save(f"{OUT}/stemwalker_glowmask.png")


if __name__ == "__main__":
    main()
