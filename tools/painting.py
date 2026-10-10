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
