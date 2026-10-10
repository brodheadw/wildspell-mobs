import random

import make_scarab_model as model
from geckolib_model import texel_point
from painting import glass_bottle, hash01 as h, jitter, mix, paint_model, ramp

OUT = "src/main/resources/assets/wildspellmobs/textures"

SHELL = [(10, 16, 12), (16, 30, 20), (24, 48, 30), (40, 70, 36), (80, 100, 40), (140, 140, 56), (196, 176, 80)]
SHEEN = (30, 100, 90)
EYE = (8, 6, 6)
SAND = [(196, 178, 128), (210, 192, 140), (222, 206, 156), (232, 218, 172), (186, 164, 114)]
DUNG = [(56, 42, 28), (70, 54, 34), (84, 66, 40), (98, 78, 48)]
STRAW = (150, 128, 70)
SHADE = {"top": 0.16, "front": 0.0, "back": 0.0, "left": -0.08, "right": -0.08, "bottom": -0.2}


def shell(face, x, y, p, salt, level):
    level += SHADE[face] + 0.08 * (h(x, y, salt) - 0.5)
    color = ramp(SHELL, level)
    if face in ("left", "right"):
        color = mix(color, SHEEN, 0.35)
    return color


def elytra(cube, face, x, y, p, salt):
    if face == "bottom":
        return SHELL[1]
    along = (p[2] + 1.5) / 5
    lane = abs(p[0])
    color = shell(face, x, y, p, salt, 0.4 + 0.18 * along + (0.1 * (1 - lane / 3) if face == "top" else 0))
    if face == "top":
        if lane < 0.5:
            color = mix(color, SHELL[0], 0.6)
        elif 1.0 < lane < 1.5 or 2.5 < lane:
            color = mix(color, SHELL[1], 0.45)
    return color


def pronotum(cube, face, x, y, p, salt):
    color = shell(face, x, y, p, salt, 0.46)
    if face == "top" and -1.5 < p[0] < 0 and p[2] > 4.8:
        color = mix(color, SHELL[6], 0.7)
    if face == "top" and abs(p[0]) > 2.4:
        color = mix(color, SHELL[1], 0.5)
    return color


def clypeus(cube, face, x, y, p, salt):
    rim = p[2] > 7.8
    if (face == "front" or (face == "top" and rim)) and int(p[0] + 2.5) % 2 == 1:
        return None
    if face in ("left", "right"):
        return EYE if p[2] < 7.6 else shell(face, x, y, p, salt, 0.3)
    color = shell(face, x, y, p, salt, 0.36)
    if face == "top" and not rim:
        color = mix(color, SHELL[5], 0.3 * h(x, y, salt))
    return color


def underside(cube, face, x, y, p, salt):
    return shell(face, x, y, p, salt, 0.18)


def leg(cube, face, x, y, p, salt):
    return shell(face, x, y, p, salt, 0.24)


def foretibia(cube, face, x, y, p, salt):
    color = shell(face, x, y, p, salt, 0.3)
    return mix(color, SHELL[5], 0.4) if (x + y) % 2 == 0 and face == "top" else color


def antenna(cube, face, x, y, p, salt):
    return (92, 70, 40)


def sand_ball(cube, face, x, y, p, salt):
    color = ramp(SAND, 0.2 + 0.7 * h(x, y, salt))
    if h(x, y, salt + 4) > 0.86:
        color = mix(color, (150, 128, 90), 0.5)
    return jitter(random.Random(salt * 31 + x * 7 + y), color, int(SHADE[face] * 60), 4)[:3]


def dung_ball(cube, face, x, y, p, salt):
    color = ramp(DUNG, 0.15 + 0.75 * h(x, y, salt))
    if h(x // 2, y, salt + 9) > 0.82:
        color = mix(color, STRAW, 0.55)
    return jitter(random.Random(salt * 31 + x * 7 + y), color, int(SHADE[face] * 50), 3)[:3]


MATERIALS = {"elytra": elytra, "pronotum": pronotum, "clypeus": clypeus, "underside": underside, "leg": leg,
             "foretibia": foretibia, "antenna": antenna}


def painter(ball):
    def texel(cube, face, x, y, fw, fh):
        material = cube["material"]
        salt = sum(map(ord, material)) + int(cube["origin"][0] * 7 + cube["origin"][1] * 13 + cube["origin"][2] * 17)
        paint = ball if material == "ball" else MATERIALS[material]
        color = paint(cube, face, x, y, texel_point(cube, face, x, y), salt)
        return (tuple(color) + (255,) if color is not None else None), None
    return texel


def bottled_item(rng):
    img = glass_bottle(rng)
    for x, y in ((7, 8), (8, 8), (9, 8), (7, 9), (8, 9), (9, 9), (7, 10), (8, 10), (9, 10)):
        img.putpixel((x, y), ramp(SHELL, 0.25 + 0.15 * ((x + y) % 2)) + (255,))
    for x, y in ((8, 7), (7, 7), (9, 7)):
        img.putpixel((x, y), SHELL[2] + (255,))
    img.putpixel((8, 9), SHELL[6] + (255,))
    img.putpixel((7, 8), SHELL[5] + (255,))
    for x, y in ((6, 8), (10, 9), (6, 10), (10, 11), (7, 11), (9, 11)):
        img.putpixel((x, y), SHELL[1] + (255,))
    for x in range(6, 11):
        img.putpixel((x, 12), jitter(rng, rng.choice(SAND), 0, 6))
    return img


def main():
    paint_model(model.BONES, model.TEX, painter(sand_ball), f"{OUT}/entity/scarab.png")
    paint_model(model.BONES, model.TEX, painter(dung_ball), f"{OUT}/entity/scarab_dung.png")
    bottled_item(random.Random(53)).save(f"{OUT}/item/bottled_scarab.png")


if __name__ == "__main__":
    main()
