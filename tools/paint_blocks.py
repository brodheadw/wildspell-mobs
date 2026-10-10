import math
import random

from PIL import Image

import painting
from painting import jitter

OUT = "src/main/resources/assets/wildspellmobs/textures"
rng = random.Random(11)


IRON = [(46, 52, 70), (58, 64, 84), (38, 42, 58)]
RIME = (196, 226, 244)
VIAL = [(150, 206, 238), (170, 220, 246), (132, 190, 228)]


def paint_phylactery(warded):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(8):
        for y in (*range(6), *range(8, 16)):
            c = rng.choice(IRON)
            if y in (0, 2) or (y >= 8 and (x in (0, 7) or y in (8, 15)) and rng.random() < 0.5):
                c = RIME if rng.random() < 0.6 else c
            img.putpixel((x, y), jitter(rng, c, spread=6))
    for x in range(8, 14):
        for y in range(14):
            edge = x in (8, 13) or y in (0, 7, 8, 13)
            c = rng.choice(VIAL)
            alpha = 215 if edge else 150
            if warded and rng.random() < 0.35:
                c, alpha = RIME, 230
            if not edge and x == 9 and y % 8 in (1, 2, 3):
                c, alpha = (236, 250, 255), 200
            img.putpixel((x, y), jitter(rng, c, alpha=alpha, spread=6))
    core = [(140, 245, 255), (200, 255, 255), (90, 215, 255)] if warded else [(90, 170, 220), (120, 200, 240), (70, 140, 200)]
    for x in range(12, 15):
        for y in range(12, 15):
            img.putpixel((x, y), jitter(rng, core[1] if (x, y) == (13, 13) else rng.choice(core), spread=4))
    return img


paint_phylactery(False).save(f"{OUT}/block/frozen_phylactery.png")
paint_phylactery(True).save(f"{OUT}/block/frozen_phylactery_warded.png")

ward = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
ICE_DARK, ICE_MID, ICE_LIGHT, ICE_GLOW = (70, 140, 200), (120, 190, 235), (180, 228, 250), (225, 250, 255)
for cx, height, half in [(8, 14, 2.4), (4, 9, 1.8), (12, 10, 1.8), (6, 6, 1.4), (11, 5, 1.3)]:
    for y in range(16 - height, 16):
        t = (15 - y) / height
        w = half * (1.0 - t) + 0.4
        for x in range(16):
            dx = x + 0.5 - cx
            if abs(dx) <= w:
                shade = ICE_LIGHT if dx < -w * 0.2 else ICE_MID if dx < w * 0.5 else ICE_DARK
                if abs(dx) < 0.6 and t > 0.3:
                    shade = ICE_GLOW
                ward.putpixel((x, y), jitter(rng, shade, alpha=235, spread=5))
ward.save(f"{OUT}/block/rime_ward.png")

wisp = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for x in range(16):
    for y in range(16):
        r = math.hypot(x - 7.5, y - 7.5) / 7.5
        if r < 1.0:
            k = 1.0 - r
            wisp.putpixel((x, y), painting.mix((60, 170, 255), (240, 255, 255), k) + (int(255 * min(1.0, k * 1.6)),))
wisp.save(f"{OUT}/entity/lich_wisp.png")
print("blocks painted")

soul_rng = random.Random(41)
soul = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for x in range(8):
    for y in range(8):
        r = math.hypot(x - 3.5, y - 3.5) / 4.6
        alpha = int(110 * max(0.0, 1.0 - r) ** 1.4)
        soul.putpixel((x, y), (150, 230, 255, alpha))
CORE_EDGE, CORE, CORE_LIGHT, HOLLOW = (130, 215, 245), (200, 245, 255), (240, 255, 255), (40, 90, 130)
for x in range(6):
    for y in range(6):
        edge = x in (0, 5) or y in (0, 5)
        c = CORE_EDGE if edge else (CORE_LIGHT if (x, y) in ((2, 1), (3, 1)) else CORE)
        soul.putpixel((9 + x, 1 + y), jitter(soul_rng, c, spread=4, alpha=200 if edge else 235))
for x, y in ((1, 2), (4, 2), (2, 4), (3, 4)):
    soul.putpixel((9 + x, 1 + y), HOLLOW + (240,))
soul.save(f"{OUT}/block/frozen_soul.png")
print("frozen soul painted")

orb = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
orb_rng = random.Random(53)
for x in range(16):
    for y in range(16):
        r = math.hypot(x - 7.5, y - 7.5)
        spike = (x in (7, 8) or y in (7, 8)) and r < 7.8
        if r < 7.0 or spike:
            k = max(0.0, 1.0 - r / 7.0)
            base = (70, 170, 240) if r > 4.5 else (150, 225, 255) if r > 2.2 else (240, 255, 255)
            orb.putpixel((x, y), jitter(orb_rng, base, spread=8, alpha=int(255 * min(1.0, 0.35 + k * 1.3))))
orb.save(f"{OUT}/entity/frost_orb.png")
print("frost orb painted")
