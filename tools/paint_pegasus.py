"""Paints the Pegasus's texture: the vanilla white horse, pearled, with a pale-gold mane, tail and hooves, and
its two wing panels painted in the right half of a 128x64 sheet (see PegasusModel). Needs the 1.21.1 client jar
the gradle build unpacks. Run from the repo root: python3 tools/paint_pegasus.py"""
import glob
import io
import os
import random
import zipfile

from PIL import Image

OUT = "src/main/resources/assets/wildspellmobs/textures/entity/pegasus.png"
CLIENT = glob.glob(os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"))[0]
rng = random.Random(7)

PEARL = (1.0, 0.985, 0.955)  # multiplies the horse's greys: a faintly warm white
LIFT = 0.65  # and brightens them, keeping this share of each shade's distance from white
MANE = [(240, 222, 160), (232, 210, 142), (246, 232, 180), (222, 198, 128)]
HOOF = [(214, 184, 104), (198, 168, 92)]
FEATHER = [(250, 248, 242), (244, 242, 236), (252, 251, 247)]
SHAFT = (214, 210, 200)
COVERT = [(246, 236, 204), (240, 228, 190)]
TIP = [(214, 226, 240), (200, 216, 236)]
EDGE = (226, 222, 212)


def jitter(color, spread=5):
    return tuple(max(0, min(255, c + rng.randint(-spread, spread))) for c in color) + (255,)


with zipfile.ZipFile(CLIENT) as jar:
    horse = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/entity/horse/horse_white.png"))).convert("RGBA")

img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
img.paste(horse, (0, 0))
px = img.load()

# Pearl the coat, leaving the tack (the saddle and bridle's leather, top rows) as it is.
for x in range(64):
    for y in range(64):
        r, g, b, a = px[x, y]
        if a and not (y < 21 and (r - b) > 20):
            px[x, y] = tuple(round((255 - (255 - c) * LIFT) * k) for c, k in zip((r, g, b), PEARL)) + (a,)


def recolor(x0, y0, w, h, palette, rows=None):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            if px[x, y][3] and (rows is None or rows(y)):
                px[x, y] = jitter(rng.choice(palette))


recolor(56, 36, 8, 18, MANE)  # mane (texOffs 56,36: 2x16x2)
recolor(42, 36, 14, 18, MANE)  # tail (texOffs 42,36: 3x14x4)
recolor(48, 21, 16, 15, HOOF, rows=lambda y: y >= 33)  # the legs' bottom rows and soles


def feathers(u, v, span, chord, primaries, trailing_first):
    """One wing face at (u, v): span columns from the hinge out, chord rows from leading to trailing edge
    (reversed when the face's first row is its trailing edge). Coverts along the front, then long feathers
    with pale shafts every few columns, sky-tinted tips and a ragged trailing edge."""
    for i in range(span):
        for j in range(chord):
            back = j / (chord - 1)  # 0 leading, 1 trailing
            y = v + (chord - 1 - j if trailing_first else j)
            x = u + i
            if back < 0.3:
                color = jitter(rng.choice(COVERT), 4)
            elif back > 0.82 or (primaries and i > span * 0.6 and back > 0.7):
                color = jitter(rng.choice(TIP), 4)
            else:
                color = jitter(rng.choice(FEATHER), 3)
            if back >= 0.3 and i % 3 == 2:
                color = jitter(SHAFT, 3)
            if j == chord - 1 and i % 3 == 1:
                color = (0, 0, 0, 0)  # notch between feather tips
            px[x, y] = color


def edges(u, v, w, d):
    for x in range(u, u + 2 * (w + d)):
        px[x, v + d] = jitter(EDGE, 4)


# Inner panel: texOffs 64,0, box 14x1x12. Up face at (76,0), down face at (90,0), edge row at y=12.
feathers(76, 0, 14, 12, primaries=False, trailing_first=True)
feathers(90, 0, 14, 12, primaries=False, trailing_first=False)
edges(64, 0, 14, 12)
# Outer panel: texOffs 64,14, box 14x1x14. Up face at (78,14), down face at (92,14), edge row at y=28.
feathers(78, 14, 14, 14, primaries=True, trailing_first=True)
feathers(92, 14, 14, 14, primaries=True, trailing_first=False)
edges(64, 14, 14, 14)

img.save(OUT)
print("wrote", OUT)
