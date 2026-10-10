import math
import random

from PIL import Image

import make_flytrap_model as model
from painting import jitter, mix, paint_model

OUT = "src/main/resources/assets/wildspellmobs/textures"
rng = random.Random(23)



GREEN = [(84, 150, 52), (76, 140, 46), (92, 160, 58)]
GREEN_DARK = [(52, 104, 34), (46, 96, 30), (58, 112, 38)]
RIM = [(170, 196, 70), (160, 188, 62), (182, 204, 80)]
RED = [(178, 36, 44), (164, 30, 40), (190, 46, 50)]
RED_DEEP = (118, 18, 32)
TRIGGER = (236, 206, 176)
TOOTH = [(196, 214, 118), (184, 206, 104), (210, 224, 136)]
STALK = [(110, 168, 70), (100, 158, 62), (120, 176, 78)]
LEAF = [(62, 124, 40), (56, 116, 36), (70, 132, 46)]
MIDRIB = (132, 178, 86)

FACE_SHADE = {"top": 12, "bottom": -22, "front": 0, "right": -8, "left": -8, "back": -14}



def lobe_inside(x, y, fw, fh, face):
    row = y if face == "top" else fh - 1 - y
    edge = min(x, fw - 1 - x, row)
    if edge == 0:
        return jitter(rng, rng.choice(RIM), 0, 5)
    if edge == 1:
        return jitter(rng, mix(rng.choice(RIM), RED[0], 0.55), -6, 5)
    if (x, row) in {(2, 3), (fw - 3, 3), (fw // 2, 6)}:
        return jitter(rng, TRIGGER, 0, 4)
    toward_hinge = row / (fh - 1)
    return jitter(rng, mix(rng.choice(RED), RED_DEEP, 0.2 + 0.6 * toward_hinge), 0, 6)


def lobe_outside(face, y, blush=None):
    px = rng.choice(GREEN)
    if y == blush:
        px = mix(px, RED[0], 0.35)
    if rng.random() < 0.06:
        px = rng.choice(GREEN_DARK)
    return jitter(rng, px, FACE_SHADE[face], 5)


def jaw(face, x, y, fw, fh, inside, rim, blush):
    if face == inside:
        return lobe_inside(x, y, fw, fh, face)
    if face == "front":
        return jitter(rng, rng.choice(RIM), 6, 5) if y == rim else lobe_outside(face, y, blush)
    if face in ("right", "left"):
        front_col = fw - 1 if face == "right" else 0
        if y == rim:
            return jitter(rng, rng.choice(RIM), FACE_SHADE[face], 5)
        if abs(x - front_col) <= 1:
            return jitter(rng, mix(rng.choice(GREEN), RED[0], 0.3), FACE_SHADE[face], 5)
        return lobe_outside(face, y, blush)
    return lobe_outside(face, y)


def m_jaw_upper(face, x, y, fw, fh, cube):
    return jaw(face, x, y, fw, fh, "bottom", fh - 1, fh - 2)


def m_jaw_lower(face, x, y, fw, fh, cube):
    return jaw(face, x, y, fw, fh, "top", 0, 1)


def m_hinge(face, x, y, fw, fh, cube):
    return jitter(rng, rng.choice(GREEN_DARK), FACE_SHADE[face], 5)


def m_tooth(face, x, y, fw, fh, cube):
    return jitter(rng, rng.choice(TOOTH), FACE_SHADE[face], 6)


def m_stalk(face, x, y, fw, fh, cube):
    s = FACE_SHADE[face]
    if face in ("front", "back", "left", "right"):
        s += 8 if x % 2 == 0 else -4
        s += int(-10 * y / max(1, fh - 1))
    return jitter(rng, rng.choice(STALK), s, 5)


def m_leaf_small(face, x, y, fw, fh, cube):
    if face == "top":
        return jitter(rng, rng.choice(LEAF), 10 - (10 if x in (0, fw - 1) else 0), 5)
    if face == "bottom":
        return jitter(rng, mix(rng.choice(LEAF), MIDRIB, 0.3), -8, 5)
    return jitter(rng, rng.choice(GREEN_DARK), FACE_SHADE[face], 4)


MATERIALS = {name[2:]: fn for name, fn in globals().items() if name.startswith("m_")}

paint_model(model.BONES, 64, lambda cube, face, x, y, fw, fh: (MATERIALS[cube["material"]](face, x, y, fw, fh, cube), None),
            f"{OUT}/entity/flytrap_head.png")
print("flytrap head texture written")

item = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
cx, cy, rx, ry = 7.5, 12.0, 6.2, 8.6
outline = (40, 70, 26, 255)
for y in range(16):
    for x in range(16):
        dx, dy = (x + 0.5 - cx - 0.5) / rx, (y + 0.5 - cy) / ry
        d = dx * dx + dy * dy
        if y > 12 or d > 1.0:
            continue
        if d > 0.78 or y == 12:
            item.putpixel((x, y), outline)
        elif d > 0.55:
            item.putpixel((x, y), jitter(rng, rng.choice(RIM), 0, 5))
        else:
            item.putpixel((x, y), jitter(rng, mix(rng.choice(RED), RED_DEEP, (y - 4) / 8), 0, 6))
for angle in (12, 32, 52, 72, 90, 108, 128, 148, 168):
    ux, uy = math.cos(math.radians(angle)), -math.sin(math.radians(angle))
    t, left = 0.0, 0
    while left < 2 and t < 20:
        t += 0.25
        x, y = int(cx + 0.5 + ux * t), int(cy + uy * t)
        if not (0 <= x < 16 and 0 <= y < 16):
            break
        if item.getpixel((x, y))[3] == 0:
            item.putpixel((x, y), jitter(rng, rng.choice(TOOTH), 0, 6))
            left += 1
for x, y in ((6, 8), (9, 8), (8, 6)):
    item.putpixel((x, y), TRIGGER + (255,))
item.save(f"{OUT}/item/trap_jaw.png")
print("trap jaw painted")


leaves = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for k in range(6):
    angle = math.radians(k * 60 + 15)
    ux, uy = math.cos(angle), math.sin(angle)
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - 8, y + 0.5 - 8
            along = px * ux + py * uy
            across = -px * uy + py * ux
            if along < 0.5 or along > 7.8:
                continue
            half = 2.3 * math.sin(math.pi * min(1.0, along / 7.8)) ** 0.8
            if abs(across) > half:
                continue
            if abs(across) < 0.5 and along > 1.5:
                color = jitter(rng, MIDRIB, 0, 5)
            elif abs(across) > half - 0.8:
                color = jitter(rng, rng.choice(GREEN_DARK), 0, 4)
            else:
                color = jitter(rng, rng.choice(LEAF), int(8 - along), 5)
            leaves.putpixel((x, y), color)
leaves.save(f"{OUT}/block/flytrap_leaves.png")

stem = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y in range(16):
    for x in range(16):
        stem.putpixel((x, y), jitter(rng, rng.choice(STALK), (8 if x % 2 == 0 else -4) - int(10 * y / 15), 5))
for y in range(4):
    for x in range(10, 14):
        ring = x in (10, 13) or y in (0, 3)
        stem.putpixel((x, y), jitter(rng, rng.choice(STALK), 6, 4) if ring else jitter(rng, (170, 200, 110), 0, 5))
stem.save(f"{OUT}/block/flytrap_stem.png")
print("flytrap block textures written")

sprout = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y in range(8, 14):
    sprout.putpixel((7, y), jitter(rng, rng.choice(STALK), 0, 5))
for x, y in ((3, 14), (4, 14), (5, 13), (6, 13), (8, 13), (9, 13), (10, 14), (11, 14), (12, 14),
             (4, 13), (11, 13), (2, 15), (13, 15)):
    sprout.putpixel((x, y), jitter(rng, rng.choice(LEAF), 0, 5))
for x in range(4, 11):
    sprout.putpixel((x, 3), jitter(rng, rng.choice(GREEN), 0, 5))
    sprout.putpixel((x, 7), jitter(rng, rng.choice(GREEN), -10, 5))
    for y in (4, 5, 6):
        edge = x in (4, 10)
        sprout.putpixel((x, y), jitter(rng, rng.choice(RIM), 0, 5) if edge else jitter(rng, rng.choice(RED), 0, 6))
for x in (4, 6, 8, 10):
    sprout.putpixel((x, 2), jitter(rng, rng.choice(TOOTH), 0, 5))
for x in (5, 7, 9):
    sprout.putpixel((x, 8), jitter(rng, rng.choice(TOOTH), 0, 5))
sprout.save(f"{OUT}/item/flytrap_sprout.png")
print("flytrap sprout painted")
