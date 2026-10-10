import random

from PIL import Image

import make_scorpion_model as model
from geckolib_model import texel_point
from painting import hash01 as h, jitter, mix, paint_model, ramp

OUT = "src/main/resources/assets/wildspellmobs/textures"

STRAW = [(128, 98, 50), (158, 126, 70), (186, 154, 92), (208, 178, 114), (226, 200, 140), (238, 218, 166)]
DUSK = (104, 78, 42)
CREAM = (232, 214, 170)
EYE = (18, 14, 12)
GLINT = (120, 112, 100)
AMBER = (196, 128, 52)
STING = [(34, 18, 12), (70, 34, 20), (112, 58, 30)]
FINGER = [(86, 46, 26), (120, 68, 34), (150, 96, 48)]
SHADE = {"top": 0.1, "front": 0.0, "back": -0.04, "left": -0.06, "right": -0.06, "bottom": 0.0}


def straw(face, x, y, salt, level=0.6):
    level += SHADE[face] + 0.12 * (h(x, y, salt) - 0.5)
    if h(x, y, salt + 3) > 0.9:
        level -= 0.18
    return ramp(STRAW, level)


def carapace(cube, face, x, y, p, salt):
    if face == "bottom":
        return CREAM
    color = straw(face, x, y, salt, 0.6)
    if face == "top":
        front = p[2] < -6
        if front and (p[0] < -1.6 or p[0] > 1.6):
            return EYE
        if abs(p[0]) < 1.2 and -6.5 < p[2] < -3.2:
            color = mix(color, DUSK, 0.45)
        if abs(p[0]) > 1.9:
            color = mix(color, DUSK, 0.3)
    elif face != "back":
        color = mix(color, DUSK, 0.2)
    return color


def tubercle(cube, face, x, y, p, salt):
    if face == "top":
        return EYE if p[2] < -4 else mix(straw(face, x, y, salt), DUSK, 0.5)
    if face == "front":
        return GLINT if x == 0 else EYE
    return mix(straw(face, x, y, salt), DUSK, 0.6)


def chelicera(cube, face, x, y, p, salt):
    return mix(CREAM, FINGER[1], 0.6) if face == "front" else straw(face, x, y, salt, 0.75)


def tergite(cube, face, x, y, p, salt):
    if face == "bottom":
        return mix(CREAM, STRAW[3], 0.4 if int(p[2]) % 2 else 0.1)
    row = int(p[2] + 1)
    color = straw(face, x, y, salt, 0.58 + (0.06 if row % 2 else 0.0))
    if face == "top":
        lane = p[0]
        if abs(lane) > 2.3 or 0.9 < abs(lane) < 1.6:
            color = mix(color, DUSK, 0.55)
        elif abs(lane) < 0.5:
            color = mix(color, DUSK, 0.3)
    elif face in ("left", "right"):
        color = mix(color, CREAM, 0.35) if y == 1 else mix(color, DUSK, 0.25)
    return color


def leg(cube, face, x, y, p, salt):
    return straw(face, x, y, salt, 0.78 if face != "bottom" else 0.85)


def tarsus(cube, face, x, y, p, salt):
    return mix(straw(face, x, y, salt, 0.85), CREAM, 0.4)


def palp(cube, face, x, y, p, salt):
    color = straw(face, x, y, salt, 0.66)
    return mix(color, DUSK, 0.25) if face == "top" and h(x, y, salt) > 0.5 else color


def chela(cube, face, x, y, p, salt):
    color = straw(face, x, y, salt, 0.72)
    if face == "top" and h(x, y, salt + 5) > 0.7:
        color = mix(color, DUSK, 0.2)
    if face == "front":
        color = mix(color, FINGER[2], 0.5)
    return color


def finger(cube, face, x, y, p, salt):
    if face == "front":
        return FINGER[0]
    return ramp(FINGER, min(1.0, 0.3 + 0.7 * (p[2] - cube["origin"][2]) / cube["size"][2]))


def tail(cube, face, x, y, p, salt):
    segment = int(round((cube["origin"][2] - 6) / 2))
    level = 0.62 - 0.05 * segment
    color = straw(face, x, y, salt, level)
    if segment >= 4:
        color = mix(color, DUSK, 0.45)
    start = p[2] < cube["origin"][2] + 0.6
    if face in ("left", "right", "top", "bottom") and start:
        color = mix(color, DUSK, 0.5)
    if face == "top" and not start and h(x, y, salt) > 0.55:
        color = mix(color, DUSK, 0.3)
    return color


def vesicle(cube, face, x, y, p, salt):
    color = mix(AMBER, STRAW[4], 0.3 + 0.3 * h(x, y, salt))
    return mix(color, DUSK, 0.3) if face in ("bottom", "back") else color


def aculeus(cube, face, x, y, p, salt):
    along = p[2] - cube["origin"][2]
    return ramp(STING, 0.9 - 0.45 * along)


MATERIALS = {"carapace": carapace, "tubercle": tubercle, "chelicera": chelicera, "tergite": tergite, "leg": leg,
             "tarsus": tarsus, "palp": palp, "chela": chela, "finger": finger, "tail": tail, "vesicle": vesicle,
             "aculeus": aculeus}


def texel(cube, face, x, y, fw, fh):
    material = cube["material"]
    salt = sum(map(ord, material)) + int(cube["origin"][0] * 7 + cube["origin"][1] * 13 + cube["origin"][2] * 17)
    p = texel_point(cube, face, x, y)
    return tuple(MATERIALS[material](cube, face, x, y, p, salt)) + (255,), None


def stinger_item(rng):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    bulb = {5: (5, 9), 6: (4, 10), 7: (4, 11), 8: (4, 11), 9: (5, 11), 10: (6, 10)}
    for y, (x0, x1) in bulb.items():
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (5, 10)
            img.putpixel((x, y), jitter(rng, mix(AMBER, DUSK, 0.45) if edge else mix(AMBER, STRAW[4], 0.35 * ((x + y) % 3 == 0)), 0, 6))
    for x, y in ((6, 6), (7, 6)):
        img.putpixel((x, y), jitter(rng, STRAW[5], 0, 4))
    for x, y, k in ((11, 9, 0.2), (11, 10, 0.3), (12, 10, 0.4), (12, 11, 0.6), (12, 12, 0.8), (11, 13, 1.0)):
        img.putpixel((x, y), ramp(STING, 1.0 - k) + (255,))
    for x, y in ((3, 7), (3, 8), (2, 8), (2, 9), (1, 9)):
        img.putpixel((x, y), jitter(rng, mix(STRAW[2], DUSK, 0.4), 0, 5))
    return img


def main():
    paint_model(model.BONES, model.TEX, texel, f"{OUT}/entity/scorpion.png")
    stinger_item(random.Random(41)).save(f"{OUT}/item/scorpion_stinger.png")


if __name__ == "__main__":
    main()
