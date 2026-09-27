"""Paints the Pegasus's four coats: the vanilla white horse's sheet recoloured (coat, mane and tail, hooves,
eyes), with its wing feathers painted around it on a 128x128 sheet (see PegasusModel for the layout).
White-and-gold, pure white and black are the usual coats; pink, with dusk-toned wings, is the rare one. Needs the 1.21.1 client
jar the gradle build unpacks. Run from the repo root: python3 tools/paint_pegasus.py"""
import glob
import io
import os
import random
import zipfile

from PIL import Image

OUT = "src/main/resources/assets/wildspellmobs/textures/entity/pegasus_{}.png"
CLIENT = glob.glob(os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"))[0]


DUSK = [  # the pink coat's feathers: dusk tones, indigo through to baby pink
    (74, 64, 170),  # indigo
    (124, 84, 196),  # purple
    (176, 150, 228),  # lavender
    (132, 156, 232),  # periwinkle
    (170, 208, 246),  # baby blue
    (248, 186, 214),  # baby pink
    (226, 140, 186),  # rose
]


def dusk_run(rng, count):
    """`count` dusk colours with no two neighbours alike."""
    out = []
    while len(out) < count:
        pick = rng.choice(DUSK)
        if not out or pick != out[-1]:
            out.append(pick)
    return out


# Per coat: the coat's shading runs from `coat_dark` to `coat_light`; feathers are `feather` with a `shaft`,
# an `edge` down each outer vane and along the coverts, and `tip` at the flight feathers' ends. `primaries`
# and `secondaries` (colour per row band, root to tip) override the feather colour where given.
COATS = {
    "white": dict(  # white with a golden mane and hooves, sky-tipped primaries
        coat_dark=(226, 224, 218), coat_light=(255, 252, 244),
        mane=[(250, 232, 168), (244, 222, 150), (252, 240, 190), (238, 214, 140)],
        hoof=[(214, 184, 104), (198, 168, 92)],
        pupil=(24, 22, 30), iris=(46, 62, 104),
        feather=[(250, 248, 242), (244, 242, 236), (252, 251, 247)],
        shaft=(214, 210, 200), edge=(206, 206, 214), root=(242, 238, 226), tip=(176, 198, 230),
    ),
    "pure": dict(  # pure white, nothing but pale greys in its shading
        coat_dark=(234, 234, 236), coat_light=(255, 255, 255),
        mane=[(252, 252, 252), (244, 244, 246), (236, 236, 240), (248, 248, 250)],
        hoof=[(232, 230, 228), (220, 218, 216)],
        pupil=(24, 22, 30), iris=(46, 62, 104),
        feather=[(252, 252, 252), (246, 246, 248), (255, 255, 255)],
        shaft=(222, 222, 228), edge=(214, 214, 222), root=(246, 246, 248), tip=(236, 238, 244),
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
        hoof=[(250, 244, 250), (236, 228, 240)],
        pupil=(40, 20, 44), iris=(150, 70, 170),
        # crystal eyes, 2 wide and 3 tall: facets rows top to bottom, (back column, front column)
        gem_eyes=[((255, 255, 255), (196, 232, 255)),
                  ((150, 122, 240), (86, 78, 214)),
                  ((122, 212, 250), (58, 40, 148))],
        feather=[(255, 236, 244), (252, 230, 240), (255, 242, 248)],
        shaft=(240, 206, 222), edge=(226, 190, 214), root=(255, 232, 242), tip=(255, 255, 255),
        # dusk tones (DUSK), every feather and covert its own, shuffled so no two neighbours match
        dusk=True,
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
        if c.get("gem_eyes"):
            # The two side faces mirror each other: widen each eye toward the same side of the head.
            front = x - 1 if x < 13 else x + 1
            for row, (back_colour, front_colour) in enumerate(c["gem_eyes"]):
                px[x, y + row] = back_colour + (255,)
                px[front, y + row] = front_colour + (255,)
        else:
            px[x, y] = c["pupil"] + (255,)
            px[x, y + 1] = c["iris"] + (255,)
    bands = dusk_run(rng, 6) if c.get("dusk") else None

    def hair(x0, y0, w, h):
        if not bands:
            recolor(x0, y0, w, h, c["mane"])
            return
        # dusk-toned bands down its length (the side faces' rows), the ends too
        for y in range(y0, y0 + h):
            band = bands[(y - y0) * len(bands) // h]
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
        outlined, plain feather above them (or, dusk-toned, each covert its own colour)."""
        colours = dusk_run(rng, width // scallop + 1) if c.get("dusk") else None
        for x in range(width):
            k = x % scallop
            edge = k == 0 or k == scallop - 1
            for r in range(depth):
                if r == 0 and edge:
                    continue
                outline = (r == 0) or (r == 1 and edge) or k == scallop - 1
                if colours:
                    body = colours[x // scallop]
                    colour = tuple(max(0, v - 40) for v in body) if outline else body
                else:
                    colour = c["edge"] if outline else rng.choice(c["feather"])
                px[u + x, v + r] = jitter(colour, 2)

    # The wing's flat faces, where PegasusModel maps them: covert rows (marginal, median, greater) over each
    # bone (humerus, forearm, hand) to the right of the horse, flight feathers in one row below it.
    for u, width in zip((64, 76, 90), (10, 12, 8)):
        for v, depth, scallop in ((0, 4, 2), (6, 6, 3), (14, 9, 3)):
            coverts(u, v, width, depth, scallop)
    tertials = (11, 12, 13, 14, 15)
    secondaries = (18, 18, 19, 19, 20, 20, 21, 21)
    primaries = (20, 21.5, 23, 24.5, 26, 27, 28, 29, 30)
    lengths = [int(n) for n in tertials + secondaries + primaries]
    dusk = dusk_run(rng, len(lengths) + 1) if c.get("dusk") else None
    for i, length in enumerate(lengths):
        u = 30 + 3 * i
        primary = i >= len(tertials) + len(secondaries)
        tip_rows = 3 + (i - len(tertials) - len(secondaries)) if primary else 2
        if dusk:
            # each its own colour, tipped with its neighbour's
            feather(u, 64, length, dusk[i + 1], tip_rows, [dusk[i]])
        else:
            feather(u, 64, length, c["tip"] if primary else c["edge"], tip_rows)

    img.save(OUT.format(name))
    print("wrote", OUT.format(name))


for coat_name, coat in COATS.items():
    paint(coat_name, coat)
