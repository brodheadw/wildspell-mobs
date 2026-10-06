import random

from PIL import Image

import painting

OUT = "src/main/resources/assets/wildspellmobs/textures/entity"
rng = random.Random(17)

BACK = [(58, 64, 52), (52, 58, 47), (64, 70, 56)]
FLANK = [(74, 78, 62), (68, 72, 57), (80, 84, 66)]
BELLY = [(214, 146, 58), (224, 160, 68), (202, 134, 52)]
BELLY_REAR = [(112, 96, 70), (104, 88, 64)]
PIT = (118, 122, 100)
EYE = (20, 22, 18)
MOUTH = (150, 70, 70)
FIN = [(46, 50, 42), (52, 56, 46)]
FIN_EDGE = (120, 122, 100)
GLOW = (196, 236, 255)
GLOW_SOFT = (104, 176, 255)


def jitter(color, spread=5):
    return painting.jitter(rng, color, spread=spread)


def side_color(row, rows, front):
    if row == 0:
        return jitter(rng.choice(BACK))
    if row == rows - 1:
        return jitter(rng.choice(BELLY if front else BELLY_REAR))
    return jitter(rng.choice(FLANK))


def paint_box(skin, u, v, w, h, d, front=False, pits=False):
    for x in range(w):
        for y in range(d):
            skin.putpixel((u + d + x, v + y), jitter(rng.choice(BACK)))
            skin.putpixel((u + d + w + x, v + y), jitter(rng.choice(BELLY if front else BELLY_REAR)))
    for side_u in (u, u + d + w):
        for x in range(d):
            for y in range(h):
                skin.putpixel((side_u + x, v + d + y), side_color(y, h, front))
            if pits and x % 2 == 1:
                skin.putpixel((side_u + x, v + d), jitter(PIT, 6))
    for end_u in (u + d, u + 2 * d + w):
        for x in range(w):
            for y in range(h):
                skin.putpixel((end_u + x, v + d + y), side_color(y, h, front))


def paint_organ(glow, u, v, w, h, d):
    row = v + d + h - 1 if h <= 2 else v + d + h - 2
    for side_u in (u, u + d + w):
        for x in range(d):
            if x % 3 != 2:
                glow.putpixel((side_u + x, row), (GLOW if x % 3 == 0 else GLOW_SOFT) + (255,))


def paint_fin(skin, glow, u, v, h, d):
    for side_u in (u, u + d):
        for x in range(d):
            for y in range(h):
                color = FIN_EDGE if y == h - 1 else rng.choice(FIN)
                skin.putpixel((side_u + x, v + d + y), jitter(color, 4))
            glow.putpixel((side_u + x, v + d), GLOW_SOFT + (255,))


skin = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
glow = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

paint_box(skin, 0, 0, 3, 3, 5, front=True)
paint_box(skin, 0, 8, 3, 2, 5, front=False, pits=True)
paint_box(skin, 0, 15, 3, 1, 5, front=True)
for x in range(3):
    for y in range(5):
        skin.putpixel((5 + x, 15 + y), jitter(MOUTH, 8))
for x in (3, 9):
    skin.putpixel((x, 13), EYE + (255,))
    glow.putpixel((x, 13), GLOW_SOFT + (255,))

for u, v, w, h, d in ((16, 0, 3, 3, 5), (32, 0, 2, 3, 5), (46, 0, 2, 3, 5), (16, 8, 2, 2, 5), (32, 8, 1, 2, 4)):
    paint_box(skin, u, v, w, h, d)
    paint_organ(glow, u, v, w, h, d)
paint_organ(glow, 0, 0, 3, 3, 5)

for u in (16, 26, 36, 54):
    paint_fin(skin, glow, u, 15, 2, 5)
paint_fin(skin, glow, 46, 15, 4, 4)

skin.save(f"{OUT}/electric_eel.png")
glow.save(f"{OUT}/electric_eel_glow.png")
