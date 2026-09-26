"""Paints the crypt's block textures and the lich wisp. Run from the repo root: python3 tools/paint_blocks.py"""
import math
import random

from PIL import Image

OUT = "src/main/resources/assets/wildspellmobs/textures"
rng = random.Random(11)


def jitter(color, alpha=255, spread=6):
    return tuple(max(0, min(255, c + rng.randint(-spread, spread))) for c in color) + (alpha,)


# --- Frozen Phylactery (16x16 sheet, UVs in models/block/frozen_phylactery.json) -------------------
# Base and cap sides (0,0)-(8,2) and (0,2)-(7,3.5): dark iron banded with frost; base top (0,8)-(8,16);
# finial (0,4)-(2,5.5); ice vial sides (8,0)-(14,8) and top (8,8)-(14,14); soul core (12,12)-(15,15).
IRON = [(46, 52, 70), (58, 64, 84), (38, 42, 58)]
RIME = (196, 226, 244)
VIAL = [(150, 206, 238), (170, 220, 246), (132, 190, 228)]


def paint_phylactery(warded):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            if y < 6 and x < 8 or 8 <= y and x < 8:
                c = rng.choice(IRON)
                # Frost along the top edge of the base and cap bands, and on the base top's rim.
                if (y in (0, 2) and x < 8) or (y >= 8 and (x in (0, 7) or y in (8, 15)) and rng.random() < 0.5):
                    c = RIME if rng.random() < 0.6 else c
                img.putpixel((x, y), jitter(c))
    for x in range(8, 14):
        for y in range(0, 14):
            edge = x in (8, 13) or y in (0, 7, 8, 13)
            c = rng.choice(VIAL)
            alpha = 215 if edge else 150
            if warded and rng.random() < 0.35:
                c, alpha = RIME, 230  # a rime crust while the wards hold
            if not edge and x == 9 and y % 8 in (1, 2, 3):
                c, alpha = (236, 250, 255), 200  # a streak of highlight down the glass
            img.putpixel((x, y), jitter(c, alpha))
    core = [(140, 245, 255), (200, 255, 255), (90, 215, 255)] if warded else [(90, 170, 220), (120, 200, 240), (70, 140, 200)]
    for x in range(12, 15):
        for y in range(12, 15):
            img.putpixel((x, y), jitter(core[1] if (x, y) == (13, 13) else rng.choice(core), spread=4))
    return img


paint_phylactery(False).save(f"{OUT}/block/frozen_phylactery.png")
paint_phylactery(True).save(f"{OUT}/block/frozen_phylactery_warded.png")

# --- Rime Ward: a cluster of ice spikes (cross model, like an amethyst cluster) --------------------
ward = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
ICE_DARK, ICE_MID, ICE_LIGHT, ICE_GLOW = (70, 140, 200), (120, 190, 235), (180, 228, 250), (225, 250, 255)
# (x of the base centre, height, half-width at the base)
for cx, height, half in [(8, 14, 2.4), (4, 9, 1.8), (12, 10, 1.8), (6, 6, 1.4), (11, 5, 1.3)]:
    for y in range(16 - height, 16):
        t = (15 - y) / height  # 0 at the base, 1 at the tip
        w = half * (1.0 - t) + 0.4
        for x in range(16):
            dx = x + 0.5 - cx
            if abs(dx) <= w:
                shade = ICE_LIGHT if dx < -w * 0.2 else ICE_MID if dx < w * 0.5 else ICE_DARK
                if abs(dx) < 0.6 and t > 0.3:
                    shade = ICE_GLOW
                ward.putpixel((x, y), jitter(shade, 235, 5))
ward.save(f"{OUT}/block/rime_ward.png")

# --- Lich wisp: a soft soul-light, white at the heart fading through cyan ---------------------------
wisp = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for x in range(16):
    for y in range(16):
        r = math.hypot(x - 7.5, y - 7.5) / 7.5
        if r < 1.0:
            k = 1.0 - r
            color = tuple(int(a + (b - a) * k) for a, b in zip((60, 170, 255), (240, 255, 255)))
            wisp.putpixel((x, y), color + (int(255 * min(1.0, k * 1.6)),))
wisp.save(f"{OUT}/entity/lich_wisp.png")
print("blocks painted")

# --- Frozen Soul: a freed soul hanging in the air (see models/block/frozen_soul.json) ---------------
# Left 8x8: the faint halo around it, clearest at the middle. Right, (9,1)-(15,7): its core, a pale
# glow with the hollow eyes and open mouth of the vanilla soul wisp.
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
        c = tuple(max(0, min(255, v + soul_rng.randint(-4, 4))) for v in c)
        soul.putpixel((9 + x, 1 + y), c + (200 if edge else 235,))
for x, y in ((1, 2), (4, 2), (2, 4), (3, 4)):  # eyes and mouth
    soul.putpixel((9 + x, 1 + y), HOLLOW + (240,))
soul.save(f"{OUT}/block/frozen_soul.png")
print("frozen soul painted")

# --- Frost orb: the lich's spun-up ball of cold (camera-facing sprite) ------------------------------
# A hard white-blue core in a ring of frost, with a few spikes of ice breaking the outline.
orb = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
orb_rng = random.Random(53)
for x in range(16):
    for y in range(16):
        r = math.hypot(x - 7.5, y - 7.5)
        spike = (x in (7, 8) or y in (7, 8)) and r < 7.8
        if r < 7.0 or spike:
            k = max(0.0, 1.0 - r / 7.0)
            base = (70, 170, 240) if r > 4.5 else (150, 225, 255) if r > 2.2 else (240, 255, 255)
            c = tuple(max(0, min(255, v + orb_rng.randint(-8, 8))) for v in base)
            orb.putpixel((x, y), c + (int(255 * min(1.0, 0.35 + k * 1.3)),))
orb.save(f"{OUT}/entity/frost_orb.png")
print("frost orb painted")
