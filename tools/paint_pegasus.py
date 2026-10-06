import io
import os
import random
import zipfile

from PIL import Image

import painting

OUT = "src/main/resources/assets/wildspellmobs/textures/entity/pegasus_{}.png"
CLIENT = os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar")


DUSK = [
    (74, 64, 170),
    (124, 84, 196),
    (176, 150, 228),
    (132, 156, 232),
    (170, 208, 246),
    (248, 186, 214),
    (226, 140, 186),
]


def dusk_run(rng, count):
    out = []
    while len(out) < count:
        pick = rng.choice(DUSK)
        if not out or pick != out[-1]:
            out.append(pick)
    return out


COATS = {
    "white": dict(
        coat_dark=(226, 224, 218), coat_light=(255, 252, 244),
        mane=[(250, 232, 168), (244, 222, 150), (252, 240, 190), (238, 214, 140)],
        hoof=[(214, 184, 104), (198, 168, 92)],
        pupil=(24, 22, 30), iris=(46, 62, 104),
        feather=[(250, 248, 242), (244, 242, 236), (252, 251, 247)],
        shaft=(214, 210, 200), edge=(206, 206, 214), root=(242, 238, 226), tip=(176, 198, 230),
    ),
    "pure": dict(
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
        pupil=(58, 40, 138), iris=(132, 178, 240),
        feather=[(255, 236, 244), (252, 230, 240), (255, 242, 248)],
        shaft=(240, 206, 222), edge=(226, 190, 214), root=(255, 232, 242), tip=(255, 255, 255),
        dusk=True,
    ),
}

with zipfile.ZipFile(CLIENT) as jar:
    HORSE = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/entity/horse/horse_white.png"))).convert("RGBA")


def paint(name, c):
    rng = random.Random(7)

    def jitter(color, spread=5):
        return painting.jitter(rng, color, spread=spread)

    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    img.paste(HORSE, (0, 0))
    px = img.load()

    eyes = [(x, y) for x in range(0, 26) for y in range(20, 25) if px[x, y][3] and sum(px[x, y][:3]) < 200]

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

    for x, y in eyes:
        px[x, y] = c["pupil"] + (255,)
        px[x, y + 1] = c["iris"] + (255,)
    bands = dusk_run(rng, 6) if c.get("dusk") else None

    def hair(x0, y0, w, h):
        if not bands:
            recolor(x0, y0, w, h, c["mane"])
            return
        for y in range(y0, y0 + h):
            band = bands[(y - y0) * len(bands) // h]
            for x in range(x0, x0 + w):
                if px[x, y][3]:
                    px[x, y] = jitter(band, 4)

    hair(56, 36, 8, 18)
    hair(42, 36, 14, 18)
    recolor(48, 21, 16, 15, c["hoof"], rows=lambda y: y >= 33)

    def feather(u, v, length, tip, tip_rows, colours=None):
        for r in range(length):
            for col in range(3):
                if r == 0 and col != 1:
                    continue
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
            feather(u, 64, length, dusk[i + 1], tip_rows, [dusk[i]])
        else:
            feather(u, 64, length, c["tip"] if primary else c["edge"], tip_rows)

    img.save(OUT.format(name))
    print("wrote", OUT.format(name))


for coat_name, coat in COATS.items():
    paint(coat_name, coat)
