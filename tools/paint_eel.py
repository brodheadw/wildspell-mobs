"""Paints the Electric Eel's textures: the eel and its glow layer (the electric organ, shown as bright as
the eel's charge). Run from the repo root: python3 tools/paint_eel.py"""
import random

from PIL import Image

OUT = "src/main/resources/assets/wildspellmobs/textures/entity"
rng = random.Random(17)

# A real electric eel: dark olive-grey above, an orange-yellow throat and belly up front, pale
# sensory pits dotted along the head and flanks.
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
    return tuple(max(0, min(255, c + rng.randint(-spread, spread))) for c in color) + (255,)


def side_color(row, rows, front):
    """Down a side face: back colour on top, flank, then belly on the lowest row."""
    if row == 0:
        return jitter(rng.choice(BACK))
    if row == rows - 1:
        return jitter(rng.choice(BELLY if front else BELLY_REAR))
    return jitter(rng.choice(FLANK))


def paint_box(skin, u, v, w, h, d, front=False, pits=False):
    """All six faces of a w x h x d box at texOffs (u, v), laid out as in ModelPart.Cube."""
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
    """The electric organ along the lower flanks: a dashed band of light on both side faces."""
    row = v + d + h - 1 if h <= 2 else v + d + h - 2
    for side_u in (u, u + d + w):
        for x in range(d):
            if x % 3 != 2:
                glow.putpixel((side_u + x, row), (GLOW if x % 3 == 0 else GLOW_SOFT) + (255,))


def paint_fin(skin, glow, u, v, h, d):
    """A flat ribbon fin at texOffs (u, v): dark membrane, a pale trailing edge along the bottom."""
    for side_u in (u, u + d):
        for x in range(d):
            for y in range(h):
                color = FIN_EDGE if y == h - 1 else rng.choice(FIN)
                skin.putpixel((side_u + x, v + d + y), jitter(color, 4))
            glow.putpixel((side_u + x, v + d), GLOW_SOFT + (255,))


skin = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
glow = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

# Forebody, head and jaw: the orange throat, pits along the head.
paint_box(skin, 0, 0, 3, 3, 5, front=True)
paint_box(skin, 0, 8, 3, 2, 5, front=False, pits=True)
paint_box(skin, 0, 15, 3, 1, 5, front=True)
for x in range(3):                             # inside of the mouth (the jaw's top face)
    for y in range(5):
        skin.putpixel((5 + x, 15 + y), jitter(MOUTH, 8))
# Eyes, small and dark, a pixel back from the snout on each side (front at the inner edge).
for x in (3, 9):
    skin.putpixel((x, 13), EYE + (255,))
    glow.putpixel((x, 13), GLOW_SOFT + (255,))

# Body segments back to the tail; the organ runs the length of all of them.
for u, v, w, h, d in ((16, 0, 3, 3, 5), (32, 0, 2, 3, 5), (46, 0, 2, 3, 5), (16, 8, 2, 2, 5), (32, 8, 1, 2, 4)):
    paint_box(skin, u, v, w, h, d)
    paint_organ(glow, u, v, w, h, d)
paint_organ(glow, 0, 0, 3, 3, 5)

# The ribbon fin under each rear segment, and the deeper fin wrapping the tail.
paint_fin(skin, glow, 16, 15, 2, 5)
paint_fin(skin, glow, 26, 15, 2, 5)
paint_fin(skin, glow, 36, 15, 2, 5)
paint_fin(skin, glow, 54, 15, 2, 5)
paint_fin(skin, glow, 46, 15, 4, 4)

skin.save(f"{OUT}/electric_eel.png")
glow.save(f"{OUT}/electric_eel_glow.png")
