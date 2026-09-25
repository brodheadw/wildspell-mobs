"""Paints the mod's textures. Run from the repo root: python3 tools/paint_textures.py"""
import math
import random

from PIL import Image

OUT = "src/main/resources/assets/wildspellmobs/textures"
rng = random.Random(7)



def jitter(color, shade=0, spread=5):
    return tuple(max(0, min(255, c + shade + rng.randint(-spread, spread))) for c in color) + (255,)


# --- Rime Skull (64x32), three subtle variants ----------------------------------------------------

SKULL_VARIANTS = [
    {   # 0: pale frost blue, cyan eyes
        "ice": [(222, 242, 252), (206, 234, 248), (190, 224, 242), (232, 247, 255)],
        "cracks": [(10, 2), (11, 3), (12, 3), (13, 4), (2, 10), (3, 11), (3, 12), (29, 9), (30, 10), (31, 10), (19, 9), (20, 10)],
        "eyes": ((120, 240, 255), (200, 255, 255), (70, 200, 255)),
    },
    {   # 1: whiter, heavier rime and more cracks, pale eyes
        "ice": [(236, 246, 252), (226, 240, 250), (244, 250, 255), (214, 232, 246)],
        "cracks": [(9, 1), (10, 2), (11, 2), (15, 5), (16, 6), (1, 9), (2, 10), (2, 11), (3, 13), (27, 12), (28, 12), (29, 13),
                   (33, 9), (34, 10), (21, 12), (22, 13)],
        "eyes": ((200, 238, 255), (245, 255, 255), (150, 205, 245)),
    },
    {   # 2: steelier blue-grey, a crack across the brow, deep blue eyes
        "ice": [(200, 222, 238), (186, 210, 230), (212, 230, 244), (176, 202, 224)],
        "cracks": [(12, 3), (13, 2), (14, 2), (16, 8), (16, 9), (17, 9), (4, 11), (5, 12), (24, 10), (25, 11)],
        "eyes": ((80, 160, 255), (160, 215, 255), (45, 115, 235)),
    },
]
SOCKET = (24, 38, 58, 255)
TOOTH_LIGHT = (246, 250, 252, 255)
TOOTH_MID = (222, 230, 238, 255)
TOOTH_SHADE = (176, 190, 206, 255)
GAP = (64, 82, 104, 255)


def paint_teeth(img, x0, y0, count, lower=False, middle_tooth=False):
    """Two-pixel-wide teeth over two rows, lit on the inside and shaded toward the corners of the
    mouth; the outer two are pointed canines. Right-half teeth are exact mirrors of the left, and on
    an odd-width row a narrow one-pixel tooth sits in the middle to keep the row centred."""
    root, tip = (y0 + 1, y0) if lower else (y0, y0 + 1)
    if middle_tooth:
        img.putpixel((x0 + count, root), TOOTH_LIGHT)
        img.putpixel((x0 + count, tip), TOOTH_MID)
    for i in range(count):
        x = x0 + 2 * i + (1 if middle_tooth and i >= count // 2 else 0)
        canine = i in (0, count - 1)
        # Left-half pixels, outer then inner; the right half flips them.
        root_px = [TOOTH_MID, TOOTH_LIGHT]
        tip_px = [GAP, TOOTH_MID] if canine else [TOOTH_SHADE, TOOTH_LIGHT]
        if i >= count // 2:
            root_px.reverse()
            tip_px.reverse()
        for dx in (0, 1):
            img.putpixel((x + dx, root), root_px[dx])
            img.putpixel((x + dx, tip), tip_px[dx])



for index, variant in enumerate(SKULL_VARIANTS):
    rng = random.Random(100 + index)
    palette = variant["ice"]

    def ice_fill(img, x0, y0, w, h, shade=0):
        for x in range(x0, x0 + w):
            for y in range(y0, y0 + h):
                img.putpixel((x, y), jitter(rng.choice(palette), shade, 6))

    def ice_cube(img, u, v, w, h, d, shade=0):
        ice_fill(img, u + d, v, w, d, shade + 8)             # top
        ice_fill(img, u + d + w, v, w, d, shade - 30)        # bottom
        ice_fill(img, u, v + d, 2 * (d + w), h, shade)       # sides

    skin = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    eyes = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    ice_cube(skin, 0, 0, 9, 8, 8)
    ice_cube(skin, 0, 16, 8, 2, 7, -10)
    ice_cube(skin, 40, 0, 1, 3, 1, 12)
    for (x, y) in variant["cracks"]:
        skin.putpixel((x, y), (120, 175, 210, 255))
    # Face is the north side: x 8..16, y 8..15; x 12 is the centre column.
    for (x, y) in [(9, 10), (10, 10), (9, 11), (10, 11), (14, 10), (15, 10), (14, 11), (15, 11), (11, 13), (13, 13)]:   # eyes, then two nostrils
        skin.putpixel((x, y), SOCKET)
    paint_teeth(skin, 8, 14, 4, middle_tooth=True)   # upper teeth: x 8..16, the full width of the face
    paint_teeth(skin, 7, 23, 4, lower=True)   # lower teeth: x 7..14, the whole jaw front
    mid, bright, dim = variant["eyes"]
    for (x, y), c in {(9, 10): mid, (10, 10): bright, (9, 11): dim, (10, 11): mid,
                      (14, 10): bright, (15, 10): mid, (14, 11): mid, (15, 11): dim}.items():
        eyes.putpixel((x, y), c + (255,))
    skin.save(f"{OUT}/entity/rime_skull_{index}.png")
    eyes.save(f"{OUT}/entity/rime_skull_eyes_{index}.png")

rng = random.Random(7)

# --- Frost mote particle: four sprites, a soft mote shrinking to a speck (tinted blue in code) -----

for i, radius in enumerate((3.2, 2.6, 2.0, 1.3)):
    sprite = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for x in range(8):
        for y in range(8):
            d = math.hypot(x - 3.5, y - 3.5)
            if d <= radius:
                a = int(255 * (1 - (d / radius) ** 2))
                sprite.putpixel((x, y), (255, 255, 255, max(40, a)))
    sprite.save(f"{OUT}/particle/frost_mote_{i}.png")

# --- Frozen zombie ---------------------------------------------------------------------------------
# Painted from scratch on the 64x64 humanoid layout (the zombie model mirrors its right limbs onto the
# left, so only the top half is used): cold grey-green skin, teal shirt, indigo trousers, flecked with
# frost, with the upper right of the face torn away to the skull.

FROST = (185, 225, 245)
BONE_WHITE = [(226, 222, 208), (214, 210, 196), (236, 232, 220)]
SKIN = [(92, 134, 100), (84, 124, 92), (100, 142, 108), (76, 114, 86)]
SHIRT = [(40, 158, 160), (34, 142, 146), (46, 170, 170)]
TROUSERS = [(70, 62, 150), (62, 54, 136), (78, 70, 160)]
SHOES = [(92, 96, 108), (82, 86, 98)]


def mix(a, b, k):
    return tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))


zombie = Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def paint_box(u, v, w, h, d, material):
    """Fill a w x h x d cube's UV net. material(face, x, y) picks a palette; x, y are face-local and y
    runs top to bottom down the sides. Faces are lit from above: top brightest, bottom darkest."""
    faces = {"top": (u + d, v, w, d, 10), "bottom": (u + d + w, v, w, d, -22),
             "right": (u, v + d, d, h, -6), "front": (u + d, v + d, w, h, 0),
             "left": (u + d + w, v + d, d, h, -6), "back": (u + 2 * d + w, v + d, w, h, -10)}
    for face, (x0, y0, fw, fh, shade) in faces.items():
        for x in range(fw):
            for y in range(fh):
                palette = material(face, x, y)
                gradient = -int(10 * y / max(1, fh - 1)) if face not in ("top", "bottom") else 0
                zombie.putpixel((x0 + x, y0 + y), jitter(rng.choice(palette), shade + gradient, 5))


paint_box(0, 0, 8, 8, 8, lambda face, x, y: SKIN)                                   # head
paint_box(16, 16, 8, 12, 4, lambda face, x, y: SKIN if face == "front" and y < 2 and 3 <= x <= 4 else SHIRT)  # body, open collar
paint_box(40, 16, 4, 12, 4, lambda face, x, y: SHIRT if face == "top" or (face not in ("bottom",) and y < 4) else SKIN)  # arm: sleeve then skin
paint_box(0, 16, 4, 12, 4, lambda face, x, y: SHOES if face == "bottom" or (face != "top" and y >= 10) else TROUSERS)  # leg: trousers, shoes

# Face (x 8-15, y 8-15): brow shadow, the left eye, nose shadow and a slack mouth. The right eye is
# replaced by the torn socket below.
for x in range(8, 16):
    zombie.putpixel((x, 11), jitter(SKIN[3], -10, 3))
for p in [(9, 12), (10, 12)]:
    zombie.putpixel(p, (24, 30, 30, 255))
for p in [(11, 13), (12, 13)]:
    zombie.putpixel(p, jitter(SKIN[3], -20, 3))
for x in range(10, 14):
    zombie.putpixel((x, 14), (52, 64, 58, 255))
# Rot: a few darker blotches on the skin.
for (x, y) in [(2, 10), (3, 11), (20, 12), (27, 13), (44, 26), (45, 27), (50, 24), (9, 3), (12, 5)]:
    zombie.putpixel((x, y), jitter((70, 92, 80), spread=4))

for x in range(64):
    for y in range(64):
        r, g, b, a = zombie.getpixel((x, y))
        if a and rng.random() < 0.12:
            zombie.putpixel((x, y), mix((r, g, b), FROST, 0.55) + (a,))   # random frost-bitten pixels

# Torn brow: a ragged patch at the upper right of the face is gone down to the skull, wrapping onto the
# head's side (x 16-17) and the front edge of its top (y 6-7). The right eye is an empty socket that
# glows light blue, like the Rime Skull's (drawn on the emissive layer below). The rows are uneven so
# the tear reads as ripped rather than cut.
TORN_ROWS = {6: (14, 15), 7: (13, 15), 8: (13, 17), 9: (12, 17), 10: (13, 16), 11: (13, 17), 12: (12, 15), 13: (14, 14)}
torn = {(x, y) for y, (x0, x1) in TORN_ROWS.items() for x in range(x0, x1 + 1)} - {(16, 10), (15, 12)}
for (x, y) in torn:
    zombie.putpixel((x, y), jitter(rng.choice(BONE_WHITE), spread=4))
SOCKET_PIXELS = [(13, 11), (14, 11), (13, 12), (14, 12)]
for p in SOCKET_PIXELS:
    zombie.putpixel(p, (24, 30, 44, 255))
for (x, y) in [(15, 9), (16, 10)]:                           # hairline cracks in the bone
    zombie.putpixel((x, y), (150, 160, 170, 255))
rim = {(x + dx, y + dy) for (x, y) in torn for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))} - torn
for (x, y) in rim:
    if (8 <= x < 24 and 8 <= y < 16) or (8 <= x < 16 and 0 <= y < 8):
        # Ragged flesh edge: mostly raw, with the odd darker flap hanging over the bone.
        zombie.putpixel((x, y), jitter(rng.choice([(104, 58, 60), (104, 58, 60), (78, 44, 48), (122, 74, 70)]), spread=6))
eyes = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
for (x, y), c in {(13, 11): (120, 240, 255), (14, 11): (200, 255, 255), (13, 12): (70, 200, 255), (14, 12): (120, 240, 255)}.items():
    eyes.putpixel((x, y), c + (255,))
eyes.save(f"{OUT}/entity/frozen_zombie_eyes.png")
zombie.save(f"{OUT}/entity/frozen_zombie.png")

# Ice crust: a few chunky patches (head top, shoulders, forearms, shins and feet) on the inflated
# outer layer, rather than a full coat. The zombie model mirrors its right-limb UVs onto the left
# limbs, so limb patches are painted once and show up on both sides.
ICE = [(196, 228, 246), (178, 216, 240), (214, 238, 250), (160, 204, 234)]
crust = Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def blob(cx, cy, radius, bounds):
    x0, y0, x1, y1 = bounds
    for x in range(int(cx - radius) - 1, int(cx + radius) + 2):
        for y in range(int(cy - radius) - 1, int(cy + radius) + 2):
            if x0 <= x < x1 and y0 <= y < y1 and math.hypot(x - cx, y - cy) <= radius + rng.uniform(-0.6, 0.4):
                crust.putpixel((x, y), jitter(rng.choice(ICE), spread=8))


# (bounds of a region in the 64x64 humanoid layout, blob centres as fractions of it, radius)
PATCHES = [
    ((8, 0, 16, 8), [(0.3, 0.35), (0.25, 0.8)], 2.6),                # head top, clear of the torn brow
    ((0, 8, 8, 16), [(0.3, 0.2)], 2.2),                              # side of the head
    ((40, 16, 56, 20), [(0.25, 0.5), (0.75, 0.5)], 2.4),             # shoulders (arm tops)
    ((40, 20, 56, 32), [(0.15, 0.3), (0.45, 0.85), (0.8, 0.95)], 2.8),  # arms and hands
    ((0, 20, 16, 32), [(0.2, 0.75), (0.55, 0.95), (0.9, 0.85)], 3.0),   # shins and feet
    ((16, 20, 40, 32), [(0.12, 0.25), (0.62, 0.55), (0.9, 0.9)], 2.6),  # torso
]
for (x0, y0, x1, y1), centres, radius in PATCHES:
    for fx, fy in centres:
        blob(x0 + fx * (x1 - x0), y0 + fy * (y1 - y0), radius, (x0, y0, x1, y1))
crust.save(f"{OUT}/entity/frozen_zombie_crust.png")

# --- Item: a small blue ice crystal --------------------------------------------------------------

shard = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
outline = (40, 90, 150, 255)
for y in range(2, 14):
    half = min(y - 2, 13 - y) // 2 + 1
    for x in range(8 - half, 8 + half):
        edge = x in (8 - half, 8 + half - 1) or y in (2, 13)
        shard.putpixel((x, y), outline if edge else jitter(rng.choice(ICE), -10 if x >= 8 else 10))
for (x, y) in [(7, 5), (7, 6), (6, 8)]:
    shard.putpixel((x, y), (255, 255, 255, 255))
shard.save(f"{OUT}/item/rime_shard.png")

# Item: Enchanted Ice Crystal, a faceted glowing crystal (the item also gets the enchantment glint).
crystal = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
edge = (34, 96, 170, 255)
for y in range(1, 15):
    half = min(y - 1, 14 - y, 4) + 1
    for x in range(8 - half, 8 + half):
        rim = x in (8 - half, 8 + half - 1) or y in (1, 14)
        core = abs(x - 7.5) < 1.6 and 4 <= y <= 11
        crystal.putpixel((x, y), edge if rim else (206, 250, 255, 255) if core else jitter((96, 196, 250), -12 if x >= 8 else 12, 6))
for (x, y) in [(6, 4), (6, 5), (7, 3)]:
    crystal.putpixel((x, y), (255, 255, 255, 255))
crystal.save(f"{OUT}/item/enchanted_ice_crystal.png")
print("textures written")
