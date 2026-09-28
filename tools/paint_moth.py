import random

from PIL import Image

import painting

OUT = "src/main/resources/assets/wildspellmobs/textures"
rng = random.Random(11)

WING = [(206, 226, 178), (196, 218, 168), (214, 232, 188), (188, 210, 160)]
WING_UNDER = [(222, 234, 204), (214, 228, 196), (228, 238, 212)]
BAND = (128, 158, 104)
EDGE = (104, 132, 90)
FUR = [(236, 226, 196), (226, 214, 182), (242, 234, 208)]
FUR_BAND = (178, 160, 124)
EYE = (34, 38, 44)
GLOW = (150, 255, 214)
GLOW_SOFT = (96, 214, 176)
GOLD = (236, 255, 170)


def jitter(color, spread=6):
    return painting.jitter(rng, color, spread=spread)


def fill(img, x0, y0, w, h, palette, spread=6):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            img.putpixel((x, y), jitter(rng.choice(palette), spread))


FOREWING = [4, 5, 6, 7, 7, 6]
HINDWING = [3, 4, 5, 4]


def paint_wing(skin, glow, u, v, rows, eyespot, under=False):
    for j, length in enumerate(rows):
        for i in range(length):
            rim = i == length - 1 or j in (0, len(rows) - 1) and i >= length - 3
            if under:
                color = jitter(rng.choice(WING_UNDER), 4)
            elif rim:
                color = jitter(EDGE, 6)
            elif i in (2, 3) and j >= 1:
                color = jitter(BAND, 8)
            else:
                color = jitter(rng.choice(WING), 6)
            skin.putpixel((u + i, v + j), color)
            if not under and rim and (i + j) % 2 == 0 and i > 1:
                glow.putpixel((u + i, v + j), GLOW_SOFT + (255,))
    ex, ey = eyespot
    skin.putpixel((u + ex, v + ey), GOLD + (255,))
    glow.putpixel((u + ex, v + ey), GOLD + (255,))
    for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
        x, y = ex + dx, ey + dy
        if 0 <= y < len(rows) and 0 <= x < rows[y]:
            skin.putpixel((u + x, v + y), GLOW + (255,))
            glow.putpixel((u + x, v + y), (GLOW if not under else GLOW_SOFT) + (255,))


skin = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
glow = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

fill(skin, 6, 0, 4, 6, FUR)
fill(skin, 0, 6, 16, 2, FUR)
for z in (3, 5):
    for x in (6, 7):
        skin.putpixel((x, z), jitter(FUR_BAND, 6))
    skin.putpixel((z, 6), jitter(FUR_BAND, 6))
    skin.putpixel((13 - z, 7), jitter(FUR_BAND, 6))
for x, y in ((14, 6), (15, 6), (14, 7), (15, 7)):
    skin.putpixel((x, y), GLOW + (255,))
    glow.putpixel((x, y), GLOW_SOFT + (255,))

fill(skin, 16, 0, 8, 4, FUR)
skin.putpixel((18, 2), jitter(FUR_BAND, 4))
skin.putpixel((19, 2), jitter(FUR_BAND, 4))

for x in range(24, 28):
    for y in range(0, 2):
        skin.putpixel((x, y), jitter(EYE, 4))
for x, y in ((25, 1), (27, 1)):
    skin.putpixel((x, y), (60, 110, 104, 255))
    glow.putpixel((x, y), (40, 120, 100, 255))

for face_u in (20, 22):
    for y in range(4):
        skin.putpixel((face_u, 4 + y), jitter(FUR_BAND, 6))
        if y % 2 == 1:
            skin.putpixel((face_u + 1, 4 + y), jitter(FUR[1], 6))
    skin.putpixel((face_u, 7), GLOW + (255,))
    glow.putpixel((face_u, 7), GLOW + (255,))

paint_wing(skin, glow, 6, 8, FOREWING, eyespot=(4, 3))
paint_wing(skin, glow, 13, 8, FOREWING, eyespot=(4, 3), under=True)
paint_wing(skin, glow, 4, 14, HINDWING, eyespot=(2, 2))
paint_wing(skin, glow, 9, 14, HINDWING, eyespot=(2, 2), under=True)

skin.save(f"{OUT}/entity/luminous_moth.png")
glow.save(f"{OUT}/entity/luminous_moth_glow.png")


rng = random.Random(23)
overlay = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
placed = set()
while len(placed) < 22:
    x, y = rng.randrange(16), rng.randrange(16)
    if any((x + dx, y + dy) in placed for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
        continue
    placed.add((x, y))
    overlay.putpixel((x, y), (GOLD if rng.random() < 0.25 else GLOW) + (255,))
    if rng.random() < 0.3 and x + 1 < 16:
        overlay.putpixel((x + 1, y), GLOW_SOFT + (255,))
overlay.save(f"{OUT}/block/luminous_moss_glow.png")


rng = random.Random(31)
bottle = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
GLASS = (196, 222, 236, 255)
GLASS_SHADE = (138, 168, 190, 255)
INSIDE = (40, 62, 70, 150)
CORK = [(150, 108, 70), (132, 94, 60), (164, 120, 80)]
BODY = {4: (7, 8), 5: (7, 8), 6: (5, 10), 7: (4, 11), 8: (4, 11), 9: (4, 11), 10: (4, 11), 11: (4, 11), 12: (5, 10)}
for x in range(6, 10):
    for y in (1, 2):
        bottle.putpixel((x, y), jitter(rng.choice(CORK), 6))
for x in (6, 9):
    bottle.putpixel((x, 3), GLASS)
for x in (7, 8):
    bottle.putpixel((x, 3), GLASS_SHADE)
for y, (x0, x1) in BODY.items():
    for x in range(x0, x1 + 1):
        bottle.putpixel((x, y), INSIDE)
    bottle.putpixel((x0 - 1, y), GLASS)
    bottle.putpixel((x1 + 1, y), GLASS_SHADE)
for x in range(5, 11):
    bottle.putpixel((x, 13), GLASS_SHADE)
for x, y in ((5, 7), (5, 8), (6, 6)):
    bottle.putpixel((x, y), (236, 248, 255, 255))
for y in (8, 9, 10):
    bottle.putpixel((8, y), FUR[0] + (255,))
for x, y in ((6, 8), (7, 8), (9, 8), (10, 8), (7, 9), (9, 9), (6, 9), (10, 9)):
    bottle.putpixel((x, y), (GLOW if (x + y) % 2 else GLOW_SOFT) + (255,))
bottle.putpixel((7, 10), GOLD + (255,))
bottle.putpixel((9, 10), GOLD + (255,))
bottle.save(f"{OUT}/item/luminous_moth_bottle.png")
