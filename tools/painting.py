from PIL import Image

from geckolib_model import texels


def jitter(rng, color, shade=0, spread=5, alpha=255):
    return tuple(max(0, min(255, c + shade + rng.randint(-spread, spread))) for c in color) + (alpha,)


def mix(a, b, k):
    k = max(0.0, min(1.0, k))
    return tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))


def ramp(palette, level):
    return palette[int(round(max(0.0, min(1.0, level)) * (len(palette) - 1)))]


def hash01(x, y, salt):
    v = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    return ((v * 2654435761) & 0xFFFFFFFF) / 0xFFFFFFFF


def paint_model(bones, size, texel, path, glow_path=None):
    skin = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for cube, face, x, y, fw, fh, at in texels(bones):
        px, lit = texel(cube, face, x, y, fw, fh)
        if px is not None:
            skin.putpixel(at, px)
        if lit is not None:
            glow.putpixel(at, lit)
    skin.save(path)
    if glow_path:
        glow.save(glow_path)


def alpha_glow(px, glow):
    if px is None:
        return None, None
    return tuple(px) + (0 if glow < 0 else 255,), (255, 255, 255, abs(glow)) if glow else None


GLASS = (196, 222, 236, 255)
GLASS_SHADE = (138, 168, 190, 255)
BOTTLE_INSIDE = (40, 62, 70, 150)
CORK = [(150, 108, 70), (132, 94, 60), (164, 120, 80)]
BOTTLE_BODY = {4: (7, 8), 5: (7, 8), 6: (5, 10), 7: (4, 11), 8: (4, 11), 9: (4, 11), 10: (4, 11), 11: (4, 11), 12: (5, 10)}


def glass_bottle(rng):
    bottle = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(6, 10):
        for y in (1, 2):
            bottle.putpixel((x, y), jitter(rng, rng.choice(CORK), spread=6))
    for x in (6, 9):
        bottle.putpixel((x, 3), GLASS)
    for x in (7, 8):
        bottle.putpixel((x, 3), GLASS_SHADE)
    for y, (x0, x1) in BOTTLE_BODY.items():
        for x in range(x0, x1 + 1):
            bottle.putpixel((x, y), BOTTLE_INSIDE)
        bottle.putpixel((x0 - 1, y), GLASS)
        bottle.putpixel((x1 + 1, y), GLASS_SHADE)
    for x in range(5, 11):
        bottle.putpixel((x, 13), GLASS_SHADE)
    for x, y in ((5, 7), (5, 8), (6, 6)):
        bottle.putpixel((x, y), (236, 248, 255, 255))
    return bottle
