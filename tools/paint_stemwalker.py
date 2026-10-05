import glob
import io
import math
import os
import random
import sys
import zipfile

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import make_stemwalker_model as model  # noqa: E402
from geckolib_model import texels  # noqa: E402
from painting import mix  # noqa: E402

OUT = "src/main/resources/assets/wildspellmobs/textures/entity"
CLIENT = glob.glob(os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"))[0]

with zipfile.ZipFile(CLIENT) as jar:
    def vanilla(name):
        return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")
    STEM = vanilla("mushroom_stem")
    CAP = vanilla("red_mushroom_block")
    INSIDE = vanilla("mushroom_block_inside")

SEAM_SEGMENTS = range(1, 11)
SEAM_COLUMN = 6
STAIN_FACE = "left"
SEAM_FACE = "back"
TOWARD_STEM = {(1, 0): "right", (-1, 0): "left", (0, 1): "front", (0, -1): "back"}


def hash01(*v):
    h = 2166136261
    for n in v:
        h = ((h ^ (n & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
    return h / 0xFFFFFFFF


def px(img, x, y):
    return img.getpixel((x % 16, y % 16))[:3]


def shade(color, k):
    return tuple(max(0, min(255, int(c * k))) for c in color)


def stem_side(k, face, x, y):
    color = px(STEM, x, y)
    if face == SEAM_FACE and k in SEAM_SEGMENTS:
        wobble = 1 if hash01(k, y // 5, 3) > 0.7 else 0
        col = SEAM_COLUMN + wobble
        broken = hash01(k, y, 11) > 0.86
        if x == col and not broken:
            color = mix(color, (150, 128, 122), 0.42)
        elif x == col + 1 and not broken and hash01(k, y, 5) > 0.45:
            color = mix(color, (226, 220, 214), 0.5)
    if face == STAIN_FACE:
        drop = (1 - k) * 16 + y
        if 0 <= drop < 26:
            centre = 8.5 + 0.8 * math.sin(drop * 0.45)
            width = 2.6 * (1 - drop / 30)
            d = abs(x + 0.5 - centre)
            if d < width:
                wet = (1 - d / width) * (1 - drop / 34)
                color = mix(color, (172, 160, 136), 0.55 * wet)
                if d < 0.6 and hash01(x, drop, 9) > 0.55:
                    color = mix(color, (238, 234, 226), 0.7)
    return color


def raw_inside(x, y, wet):
    color = px(INSIDE, x, y)
    if wet:
        d = math.hypot(x - 7.5, y - 7.5) / 8
        color = mix(color, (150, 98, 78), max(0.0, 0.55 - 0.45 * d))
        if hash01(x, y, 21) > 0.9:
            color = mix(color, (244, 226, 206), 0.6)
    return color


def gills(x, y):
    dx, dy = x - 7.5, y - 7.5
    r = math.hypot(dx, dy)
    if r < 1.6:
        return (196, 172, 150)
    a = math.atan2(dy, dx)
    lamella = 0.5 + 0.5 * math.cos(a * 14 + 0.4 * math.sin(r))
    base = mix((68, 34, 30), (170, 120, 96), lamella ** 1.6)
    base = mix(base, (120, 38, 34), max(0.0, (r - 6.0) / 2.5))
    if lamella > 0.92 and hash01(x, y, 31) > 0.5:
        base = mix(base, (250, 222, 210), 0.55)
    return base


def cap(i, x, y, face, wet):
    rot = (i * 5 + {"top": 0, "front": 1, "back": 2, "left": 3, "right": 1, "bottom": 2}[face]) % 4
    sx, sy = [(x, y), (15 - y, x), (15 - x, 15 - y), (y, 15 - x)][rot]
    color = px(CAP, sx, sy)
    if not wet:
        return color
    spot = color[1] > 120
    color = shade(color, 0.86) if not spot else mix(color, (236, 150, 146), 0.55)
    color = mix(color, (150, 14, 22), 0.25)
    if face != "bottom" and y < 2 and hash01(x, y, i) > 0.6:
        color = mix(color, (255, 214, 206), 0.6)
    if face not in ("top", "bottom") and y > 12:
        color = shade(color, 0.85 - 0.04 * (y - 12))
        if y == 15 and x in (5, 11):
            color = mix(color, (230, 120, 110), 0.6)
    return color


def joint(x, y):
    strand = 0.5 + 0.5 * math.sin(x * 1.9 + 3 * hash01(x, 0, 41))
    color = mix((128, 108, 96), (214, 200, 186), strand)
    if hash01(x, y, 43) > 0.82:
        color = mix(color, (174, 120, 110), 0.5)
    return color


def glint(i, face, x, y):
    if i != model.WET_PAD or face in ("bottom",):
        return 0
    if (face, x, y) in {("top", 4, 9), ("top", 11, 3), ("left", 3, 0), ("front", 12, 1), ("back", 6, 0)}:
        return 120
    return 0


def paint():
    img = Image.new("RGBA", (model.TEX, model.TEX), (0, 0, 0, 0))
    glow = Image.new("RGBA", (model.TEX, model.TEX), (0, 0, 0, 0))
    pads = {f"pad{i}": (i, dx, dz) for i, (_, dx, dz, _) in enumerate(model.PADS)}
    for cube, face, x, y, fw, fh, (u, v) in texels(model.BONES):
        m = cube["material"]
        if m.startswith("stem"):
            k = int(m[4:])
            if face in ("top", "bottom"):
                color = raw_inside(x, y, k == model.SEGMENTS - 1 and face == "top")
            else:
                color = stem_side(k, face, x, y)
        elif m == "crown":
            color = gills(x, y) if face == "bottom" else cap(len(model.PADS), x, y, face, False)
        elif m == "joint":
            color = joint(x, y)
        else:
            i, dx, dz = pads[m]
            wet = i == model.WET_PAD
            if TOWARD_STEM.get((dx, dz)) == face:
                color = raw_inside(x, y, wet)
            else:
                color = cap(i, x, y, face, wet)
            a = glint(i, face, x, y)
            if a:
                glow.putpixel((u, v), (255, 226, 220, a))
        img.putpixel((u, v), color + (255,))
    os.makedirs(OUT, exist_ok=True)
    img.save(f"{OUT}/stemwalker.png")
    glow.save(f"{OUT}/stemwalker_glowmask.png")


if __name__ == "__main__":
    random.seed(7)
    paint()
    print("stemwalker textures written")
