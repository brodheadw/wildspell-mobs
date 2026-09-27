"""Paints the Pegasus's three coats: the vanilla white horse's sheet recoloured (coat, mane and tail, hooves,
eyes), with its wing feathers painted around it on a 128x128 sheet (see PegasusModel for the layout).
White and black are the usual coats; pink, with rainbow wings, is the rare one. Needs the 1.21.1 client
jar the gradle build unpacks. Run from the repo root: python3 tools/paint_pegasus.py"""
import colorsys
import glob
import io
import os
import random
import zipfile

from PIL import Image

OUT = "src/main/resources/assets/wildspellmobs/textures/entity/pegasus_{}.png"
CLIENT = glob.glob(os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"))[0]


def hue(h, s=0.55, v=1.0):
    return tuple(round(c * 255) for c in colorsys.hsv_to_rgb(h, s, v))


# Per coat: the coat's shading runs from `coat_dark` to `coat_light`; feathers are `feather` with a `shaft`,
# an `edge` down each outer vane and along the coverts, and `tip` at the flight feathers' ends. `primaries`
# and `secondaries` (colour per row band, root to tip) override the feather colour where given.
COATS = {
    "white": dict(
        coat_dark=(206, 204, 198), coat_light=(255, 252, 244),
        mane=[(250, 232, 168), (244, 222, 150), (252, 240, 190), (238, 214, 140)],
        hoof=[(214, 184, 104), (198, 168, 92)],
        pupil=(24, 22, 30), iris=(46, 62, 104),
        feather=[(250, 248, 242), (244, 242, 236), (252, 251, 247)],
        shaft=(214, 210, 200), edge=(206, 206, 214), root=(242, 238, 226), tip=(176, 198, 230),
    ),
    "black": dict(
        coat_dark=(18, 18, 24), coat_light=(62, 60, 72),
        mane=[(16, 16, 20), (28, 28, 34), (40, 40, 50), (22, 22, 28)],
        hoof=[(70, 70, 78), (58, 58, 66)],
        pupil=(8, 8, 10), iris=(176, 128, 44),
        feather=[(34, 32, 40), (40, 38, 48), (30, 28, 36)],
        shaft=(78, 76, 90), edge=(66, 66, 84), root=(26, 24, 30), tip=(46, 62, 118),
    ),
    "pink": dict(
        coat_dark=(232, 170, 194), coat_light=(255, 224, 236),
        mane=[hue(h, 0.35) for h in (0.0, 0.08, 0.15, 0.33, 0.55, 0.75)], mane_bands=True,
        hoof=[(250, 244, 250), (236, 228, 240)],
        pupil=(40, 20, 44), iris=(150, 70, 170),
        feather=[(255, 236, 244), (252, 230, 240), (255, 242, 248)],
        shaft=(240, 206, 222), edge=(226, 190, 214), root=(255, 232, 242), tip=(255, 255, 255),
        # the primaries run red to violet across the hand; the secondaries blend pink into lavender
        primaries=[hue(h, 0.5) for h in (0.97, 0.07, 0.14, 0.33, 0.55, 0.75)],
        secondaries=[hue(0.92, 0.35), hue(0.83, 0.35), hue(0.72, 0.35)],
    ),
}

with zipfile.ZipFile(CLIENT) as jar:
    HORSE = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/entity/horse/horse_white.png"))).convert("RGBA")


def paint(name, c):
    rng = random.Random(7)

    def jitter(color, spread=5):
        return tuple(max(0, min(255, v + rng.randint(-spread, spread))) for v in color) + (255,)

    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    img.paste(HORSE, (0, 0))
    px = img.load()

    # The horse's eyes are one dark pixel a side, on the head's side faces (rows 20-24).
    eyes = [(x, y) for x in range(0, 26) for y in range(20, 25) if px[x, y][3] and sum(px[x, y][:3]) < 200]

    # Recolour the coat by its shading, leaving the tack (the saddle and bridle's leather, top rows) and the
    # darkest pixels (eyes, nostrils) as they are.
    for x in range(64):
        for y in range(64):
            r, g, b, a = px[x, y]
            lum = (r + g + b) / 3
            if a and not (y < 21 and (r - b) > 20) and abs(r - b) < 24 and lum > 90:
                t = min(1.0, (lum - 90) / 110)
                px[x, y] = tuple(round(d + (l - d) * t) for d, l in zip(c["coat_dark"], c["coat_light"])) + (a,)

    def recolor(x0, y0, w, h, palette, rows=None):
        for x in range(x0, x0 + w):
            for y in range(y0, y0 + h):
                if px[x, y][3] and (rows is None or rows(y)):
                    px[x, y] = jitter(rng.choice(palette))

    # Eyes two tall, so they read from a distance: a pupil over an iris.
    for x, y in eyes:
        px[x, y] = c["pupil"] + (255,)
        px[x, y + 1] = c["iris"] + (255,)
    def hair(x0, y0, w, h):
        if not c.get("mane_bands"):
            recolor(x0, y0, w, h, c["mane"])
            return
        # rainbow in bands down its length (the side faces' rows), the ends too
        for y in range(y0, y0 + h):
            band = c["mane"][(y - y0) * len(c["mane"]) // h]
            for x in range(x0, x0 + w):
                if px[x, y][3]:
                    px[x, y] = jitter(band, 4)

    hair(56, 36, 8, 18)  # mane (texOffs 56,36: 2x16x2)
    hair(42, 36, 14, 18)  # tail (texOffs 42,36: 3x14x4)
    recolor(48, 21, 16, 15, c["hoof"], rows=lambda y: y >= 33)  # the legs' bottom rows and soles

    def feather(u, v, length, tip, tip_rows, colours=None):
        """One feather face at (u, v): 3 columns wide (column 0 toward the body when spread), `length` rows,
        row 0 the tip (a face's first row is its back edge, see ModelPart.Cube). A shaft, a shade down the
        outer vane so overlapping feathers read apart, a rounded point, a tip tint fading into the feather.
        `colours`, root to tip, replaces the feather's own colour in bands."""
        for r in range(length):
            for col in range(3):
                if r == 0 and col != 1:
                    continue  # the rounded point
                if colours:
                    band = min(len(colours) - 1, (length - 1 - r) * len(colours) // length)
                    body = colours[band]
                else:
                    body = rng.choice(c["feather"])
                if r < tip_rows:
                    t = r / tip_rows
                    base = tuple(round(tc + (fc - tc) * t) for tc, fc in zip(tip, body))
                elif r > length - 3 and not colours:
                    base = c["root"]
                else:
                    base = body
                if col == 1 and 1 < r < length - 1:
                    base = c["shaft"] if not colours else tuple(min(255, x + 20) for x in base)
                elif col == 2:
                    base = tuple(max(0, x - 22) for x in base) if sum(base) > 300 else tuple(min(255, x + 14) for x in base)
                px[u + col, v + r] = jitter(base, 2)

    def coverts(u, v, width, depth, scallop):
        """A row of coverts: rounded feather ends (every `scallop` columns) along the back edge, row 0, each
        outlined, plain feather above them."""
        for x in range(width):
            k = x % scallop
            edge = k == 0 or k == scallop - 1
            for r in range(depth):
                if r == 0 and edge:
                    continue
                outline = (r == 0) or (r == 1 and edge) or k == scallop - 1
                px[u + x, v + r] = jitter(c["edge"] if outline else rng.choice(c["feather"]), 2)

    # The wing's flat faces, where PegasusModel maps them.
    feather(64, 40, 14, c["feather"][0], 1)  # tertials
    feather(68, 40, 20, c["edge"], 2, c.get("secondaries"))  # secondaries
    for i, length in enumerate((20, 22, 24, 26, 28, 30)):  # primaries, the outer ones tinted furthest
        primary = c.get("primaries")
        feather(72 + 4 * i, 40, length, c["tip"], 4 + i, [primary[i]] if primary else None)
    for (u, v), width in zip(((100, 40), (100, 50), (100, 60)), (10, 12, 8)):
        coverts(u, v, width, 9, 3)
    for (u, v), width in zip(((64, 12), (76, 12), (90, 12)), (10, 12, 8)):
        coverts(u, v, width, 4, 2)

    img.save(OUT.format(name))
    print("wrote", OUT.format(name))


for coat_name, coat in COATS.items():
    paint(coat_name, coat)
