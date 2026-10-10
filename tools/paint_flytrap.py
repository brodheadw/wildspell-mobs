import math
import random

from PIL import Image

import make_flytrap_model as model
from geckolib_model import texel_point
from painting import hash01, jitter, mix, paint_model

OUT = "src/main/resources/assets/wildspellmobs/textures"
rng = random.Random(23)



GREEN = [(118, 180, 50), (110, 172, 46), (126, 188, 56)]
GREEN_LIGHT = (162, 206, 66)
GREEN_DARK = [(66, 122, 38), (60, 114, 34), (72, 130, 42)]
RIM = [(204, 228, 96), (196, 222, 88), (212, 234, 106)]
RED = [(202, 38, 60), (190, 32, 54), (212, 48, 68)]
RED_PINK = (236, 98, 116)
RED_DEEP = (122, 14, 40)
TRIGGER = (232, 112, 128)
TOOTH = [(232, 240, 186), (224, 236, 172), (240, 246, 200)]
TOOTH_BASE = (198, 222, 120)
STALK = [(104, 166, 60), (96, 156, 54), (112, 174, 66)]
LEAF = [(84, 152, 46), (78, 144, 42), (90, 160, 50)]
MIDRIB = (150, 196, 84)

FACE_SHADE = {"top": 12, "bottom": -22, "front": 0, "right": -8, "left": -8, "back": -14}


def steady(color, at, shade=0, spread=5):
    k = hash01(int(round(at[0] * 4)) + 97, int(round(at[1] * 4 + at[2] * 52)) + 97, 23)
    return tuple(max(0, min(255, v + shade + int((2 * k - 1) * spread))) for v in color) + (255,)


def rim_steps(x, z):
    for d in (1, 2):
        for dx, dz in ((d, 0), (-d, 0), (0, -d)):
            if not model.on_lobe(x + dx, z + dz):
                return d
    return 3


def m_lobe(face, x, y, fw, fh, cube):
    at = texel_point(cube, face, x, y)
    upper = cube["origin"][1] >= model.HINGE[1]
    if face == ("bottom" if upper else "top"):
        steps = rim_steps(at[0], at[2])
        if steps == 1:
            return steady(rng.choice(RIM), at, 0, 4)
        if (math.floor(at[0]), math.floor(at[2])) in ({(-2, -6), (1, -4)} if upper else {(1, -6), (-2, -3)}):
            return steady(TRIGGER, at, 0, 3)
        depth = (at[2] + 9) / 10
        base = mix(RED_PINK, rng.choice(RED), 0.35) if steps == 2 else rng.choice(RED)
        return steady(mix(base, RED_DEEP, 0.1 + 0.6 * depth * depth), at, 0, 6)
    if face == "back":
        return steady(rng.choice(GREEN_DARK), at, FACE_SHADE[face], 4)
    if face in ("top", "bottom"):
        return steady(rng.choice(GREEN), at, FACE_SHADE[face] // 2, 5)
    return steady(rng.choice(RIM), at, FACE_SHADE[face] // 2, 4)


def m_dome(face, x, y, fw, fh, cube):
    at = texel_point(cube, face, x, y)
    crown = 1 - min(1.0, (at[0] / 5) ** 2 + ((at[2] + 4) / 6) ** 2)
    px = mix(rng.choice(GREEN), GREEN_LIGHT, 0.7 * crown if face in ("top", "bottom") else 0)
    if rng.random() < 0.05:
        px = rng.choice(GREEN_DARK)
    return steady(px, at, FACE_SHADE[face] // 2, 5)


def cilia(face, x, y, fw, fh, cube, upper):
    at = texel_point(cube, face, x, y)
    along = at[0] if face in ("front", "back") else at[2]
    if face in ("top", "bottom") or math.floor(along) % 2 != (0 if upper else 1):
        return None
    root = y == 0 if upper else y == fh - 1
    return steady(TOOTH_BASE if root else rng.choice(TOOTH), at, 0, 4)


def m_upper_cilia(face, x, y, fw, fh, cube):
    return cilia(face, x, y, fw, fh, cube, True)


def m_lower_cilia(face, x, y, fw, fh, cube):
    return cilia(face, x, y, fw, fh, cube, False)


def m_hinge(face, x, y, fw, fh, cube):
    at = texel_point(cube, face, x, y)
    if face == "front":
        return steady(mix(RED_DEEP, GREEN_DARK[0], 0.25), at, -10, 4)
    return steady(mix(rng.choice(GREEN), GREEN_DARK[0], 0.4), at, FACE_SHADE[face] // 2, 5)


def m_stalk(face, x, y, fw, fh, cube):
    s = FACE_SHADE[face]
    if face in ("front", "back", "left", "right"):
        s += 8 if x % 2 == 0 else -4
        s += int(-10 * y / max(1, fh - 1))
    return jitter(rng, rng.choice(STALK), s, 5)


def m_leaf(face, x, y, fw, fh, cube):
    if face not in ("top", "bottom"):
        return None
    at = texel_point(cube, face, x, y)
    (ox, _, oz), (w, _, d) = cube["origin"], cube["size"]
    u = abs(at[0] - (ox if ox >= 0 else ox + w)) / w
    v = (at[2] - oz) / d - 0.5
    if abs(v) > 0.5 * math.sin(math.pi * min(1.0, 0.15 + u)) ** 0.6 + 0.01:
        return None
    if abs(v) < 0.2:
        return steady(MIDRIB, at, 0, 4)
    return steady(rng.choice(LEAF), at, 0, 5)


MATERIALS = {name[2:]: fn for name, fn in globals().items() if name.startswith("m_")}

paint_model(model.BONES, model.TEX, lambda cube, face, x, y, fw, fh: (MATERIALS[cube["material"]](face, x, y, fw, fh, cube), None),
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
