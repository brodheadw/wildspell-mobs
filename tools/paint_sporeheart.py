from PIL import Image

OUT = "src/main/resources/assets/wildspellmobs/textures/block"
STEM = [(176, 168, 156), (200, 193, 180), (218, 212, 199), (232, 227, 216)]
GILL = [(30, 12, 16), (52, 20, 24), (78, 32, 34)]
FLESH = [(96, 18, 20), (130, 28, 28), (164, 44, 38)]
PORE_DIM = (118, 104, 92)
PORE_LIT = (226, 240, 188)
TAN = [(150, 120, 86), (176, 146, 106), (198, 170, 128)]
SLIT = {7: (6, 9), 6: (6, 9), 5: (6, 8), 4: (7, 8), 8: (6, 9), 9: (6, 9), 10: (6, 9), 11: (7, 8), 3: (7, 8), 12: (7, 8)}
PORES = [(7, 6), (8, 9), (6, 10)]


def h(x, y, salt):
    v = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    return ((v * 2654435761) & 0xFFFFFFFF) / 0xFFFFFFFF


def side(active):
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            grain = h(x, 0, 3) * 0.6 + h(x, y // 3, 4) * 0.4
            color = STEM[min(3, int(grain * 4))]
            if h(x, y, 5) > 0.93:
                color = STEM[0]
            span = SLIT.get(y)
            if span and span[0] <= x <= span[1]:
                edge = x in span
                color = FLESH[2 if active else 1] if edge else GILL[(x + y) % 2 + (1 if active else 0)]
            elif span and (x == span[0] - 1 or x == span[1] + 1):
                color = tuple(int(c * 0.82) for c in color)
            image.putpixel((x, y), color + (255,))
    for (x, y) in PORES:
        image.putpixel((x, y), (PORE_LIT if active else PORE_DIM) + (255,))
    return image


def top():
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            color = TAN[min(2, int(h(x, y, 7) * 3))]
            if r > 6.5:
                color = STEM[2]
            elif r < 2.5:
                color = GILL[1] if r < 1.5 else FLESH[0]
            image.putpixel((x, y), color + (255,))
    return image


if __name__ == "__main__":
    side(False).save(f"{OUT}/sporeheart.png")
    side(True).save(f"{OUT}/sporeheart_active.png")
    top().save(f"{OUT}/sporeheart_top.png")
