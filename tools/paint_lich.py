import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import make_lich_model as model  # noqa: E402
import painting  # noqa: E402
from geckolib_model import texels  # noqa: E402
from painting import mix  # noqa: E402

OUT = "src/main/resources/assets/wildspellmobs/textures"
rng = random.Random(11)


def jitter(color, shade=0, spread=5):
    return painting.jitter(rng, color, shade, spread)


BONE = [(216, 228, 236), (204, 218, 230), (224, 234, 242)]
BONE_SHADOW = (150, 168, 192)
ROBE = [(28, 36, 72), (24, 30, 62), (34, 44, 84)]
ROBE_DEEP = (14, 18, 38)
RIME = [(190, 226, 248), (170, 212, 242), (208, 236, 252), (152, 198, 236)]
TRIM = (120, 182, 232)
PLATE = [(58, 70, 110), (50, 62, 100), (66, 80, 120)]
SOCKET = (12, 16, 32)
CAVITY = (16, 20, 40)
EYES = ((120, 240, 255), (200, 255, 255), (70, 200, 255))
SOUL = [(40, 120, 170), (54, 150, 200), (70, 190, 235)]
SPIKE_SHADES = [[(120, 200, 250), (100, 184, 246), (146, 214, 252)],
                [(76, 146, 228), (62, 128, 214), (96, 166, 238)],
                [(168, 226, 254), (148, 214, 250), (190, 236, 255)],
                [(58, 118, 208), (48, 102, 194), (78, 138, 222)]]
SHAFT = [(70, 80, 112), (48, 56, 84), (60, 68, 98)]
FROST_BAND = (180, 226, 250)
CRYSTAL = [(150, 220, 255), (120, 200, 250), (90, 180, 240), (210, 245, 255)]

FLESH = [(186, 160, 168), (174, 148, 160), (198, 174, 180)]
FLESH_FOLD = (132, 100, 116)
FLESH_DEEP = (84, 58, 78)

FACE_SHADE = {"top": 14, "bottom": -26, "front": 0, "right": -8, "left": -8, "back": -14}

lich = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
lich_glow = Image.new("RGBA", (128, 128), (0, 0, 0, 0))


def frost_speckle(y, fh, strength=0.6):
    return strength * (y / max(1, fh - 1)) ** 2


def robe_px(face, x, y, fw, fh, fold_offset=0, frost=0.08):
    shade = FACE_SHADE[face]
    if face not in ("top", "bottom"):
        k = (x + fold_offset) % 4
        shade += {0: -9, 1: 0, 2: 6, 3: 2}[k]
        shade += int(6 - 10 * y / max(1, fh))
    if face != "bottom" and y >= fh - 3 and rng.random() < frost_speckle(y, fh, frost):
        return jitter(rng.choice(RIME), shade - 30, 6)
    return jitter(rng.choice(ROBE), shade, 4)


def rime_px(face, x, y, fw, fh, shade=0):
    return jitter(rng.choice(RIME), FACE_SHADE[face] + shade, 6)


def bone_px(face, x, y, fw, fh, shade=0):
    s = FACE_SHADE[face] + shade
    if face in ("front", "back", "left", "right") and fh > 2:
        s += int(4 - 12 * y / (fh - 1))
    return jitter(rng.choice(BONE), s, 5)


def ice_px(face, x, y, fw, fh, palette, shade=0):
    s = FACE_SHADE[face] + shade
    if face in ("front", "back", "left", "right"):
        if x == 0 and fw > 1:
            return jitter(palette[2], s + 26 - 6 * y, 4)
        if x == fw - 1 and fw > 1:
            return jitter(palette[1], s - 22, 4)
        s += int(10 - 18 * y / max(1, fh - 1))
    px = jitter(rng.choice(palette), s, 5)
    if face in ("front", "left") and x == 0 and y == 0:
        px = (236, 250, 255, 255)
    return px


def m_skirt(face, x, y, fw, fh, cube):
    if face == "front" and x in (fw // 2 - 1, fw // 2):
        return jitter(rng.choice(RIME), -10 - 3 * y, 6), False
    if face == "front" and x in (fw // 2 - 2, fw // 2 + 1):
        return jitter(ROBE_DEEP, 0, 3), False
    return robe_px(face, x, y, fw, fh, 1, 0.25), False


def m_skirt_low(face, x, y, fw, fh, cube):
    return robe_px(face, x, y, fw, fh, 2, 0.5), False


def m_strip(face, x, y, fw, fh, cube):
    if face in ("top",):
        return jitter(ROBE_DEEP, 0, 3), False
    if face == "bottom":
        return jitter(rng.choice(RIME), -40, 6), False
    col_seed = (cube["uv"][0] * 7 + x * 13 + {"front": 0, "back": 3, "left": 5, "right": 9}[face]) % 5
    if y == fh - 1 and col_seed in (0, 3):
        return None, False
    if y >= fh - 2 - col_seed % 2:
        return jitter(rng.choice(RIME), FACE_SHADE[face] - 8, 6), False
    if y >= fh - 4 and rng.random() < 0.35:
        return jitter(rng.choice(RIME), FACE_SHADE[face] - 36, 6), False
    return robe_px(face, x, y, fw, fh, col_seed, 0), False


def m_ribs(face, x, y, fw, fh, cube):
    if face == "front":
        edge = min(x, fw - 1 - x)
        rib_row = y in (1, 3, 5, 7) if edge > 0 else y in (2, 4, 6, 8)
        sternum = x in (fw // 2 - 1, fw // 2) and y < 7
        if sternum:
            return jitter(rng.choice(BONE), -44 + (10 if x == fw // 2 - 1 else 0), 4), False
        if rib_row:
            return jitter(rng.choice(BONE), (-18 if edge == 1 else -40 if edge == 0 else -26) - 3 * y, 4), False
        d = math.hypot((x - (fw - 1) / 2) / 1.4, y - 3.2)
        if d < 3.4:
            return (SOUL[2] if d < 1.3 else SOUL[1] if d < 2.4 else SOUL[0]) + (255,), True
        return jitter(CAVITY, 0, 3), False
    if face in ("left", "right"):
        if y % 2 == 0 and y < fh - 1:
            return jitter(rng.choice(BONE), -20, 5), False
        return jitter(CAVITY, 0, 3), False
    if face == "back":
        if x in (fw // 2 - 1, fw // 2):
            return jitter(rng.choice(BONE), -24, 4), False
        return jitter(CAVITY, 0, 3), False
    return jitter(CAVITY, 0, 3), False


def chest_open_half(y):
    return 4.1 - 0.42 * y


def m_robe_chest(face, x, y, fw, fh, cube):
    if face == "front":
        cx = (fw - 1) / 2
        half = chest_open_half(y)
        off = abs(x - cx)
        if off < half:
            return None, False
        if off < half + 1.0:
            return jitter(TRIM, 10 - 4 * y, 6), False
        if off < half + 2.0:
            return jitter(TRIM, -70, 5), False
    if face == "top":
        return jitter(rng.choice(ROBE), 4, 4), False
    return robe_px(face, x, y, fw, fh, 0, 0.12), False


def flesh_px(face, x, y, fw, fh, seg, phase=0):
    s = FACE_SHADE[face]
    if face == "top":
        if rng.random() < 0.55:
            return jitter(rng.choice(RIME), -12, 8)
        return jitter(rng.choice(FLESH), s, 6)
    if face == "bottom":
        return jitter(FLESH_DEEP if (x + y) % 2 else FLESH_FOLD, -8, 4)
    k = (y + phase) % seg
    if k == seg - 1:
        if fw > 2 and 0 < x < fw - 1:
            return jitter(FLESH_FOLD, 8, 4)
        return jitter(FLESH_DEEP if x == fw - 1 else FLESH_FOLD, 0, 4)
    if k == 0:
        s += 10
    if fw > 1 and x == fw - 1:
        s -= 16
    if rng.random() < 0.12:
        return jitter(rng.choice(RIME), s - 24, 6)
    return jitter(rng.choice(FLESH), s - 3 * (k if seg > 2 else 0), 5)


def m_gut(face, x, y, fw, fh, cube):
    if face in ("front", "back", "left", "right") and y == 0 and rng.random() < 0.6:
        return jitter(rng.choice(RIME), FACE_SHADE[face] - 6, 6), False
    return flesh_px(face, x, y, fw, fh, 2, cube["uv"][0] % 2), False


def m_gut_strand(face, x, y, fw, fh, cube):
    if face in ("front", "back", "left", "right") and y >= fh - 2:
        return jitter(rng.choice(RIME), FACE_SHADE[face] - 10 * (fh - 1 - y), 6), False
    return flesh_px(face, x, y, fw, fh, 3, cube["uv"][1] % 3), False


def m_icicle(face, x, y, fw, fh, cube):
    return ice_px(face, x, y, fw, fh, RIME, 10), False


def m_belt(face, x, y, fw, fh, cube):
    if face == "front" and x in (fw // 2 - 1, fw // 2):
        return jitter((230, 248, 255), -6 * y, 4), False
    if face in ("top", "bottom"):
        return jitter(ROBE_DEEP, 0, 3), False
    return jitter(TRIM, -8 - 18 * y + (6 if x % 3 == 0 else 0), 6), False


def m_mantle(face, x, y, fw, fh, cube):
    if face == "front":
        cx = (fw - 1) / 2
        half = 3.2 - 0.9 * y
        off = abs(x - cx)
        if off < half:
            return None, False
        if off < half + 1.2:
            return jitter(rng.choice(RIME), -6, 6), False
    if face == "top":
        if rng.random() < 0.45 + 0.08 * (fh - y):
            return jitter(rng.choice(RIME), 4, 8), False
        return jitter(rng.choice(ROBE), 10, 4), False
    if face == "bottom":
        return jitter(ROBE_DEEP, 0, 3), False
    if y == fh - 1:
        if (x * 5 + cube["uv"][0]) % 3 == 0:
            return None, False
        return jitter(rng.choice(RIME), FACE_SHADE[face] - 10, 6), False
    if y == 0:
        return jitter(rng.choice(RIME), FACE_SHADE[face], 6), False
    return robe_px(face, x, y, fw, fh, 3, 0.3), False


def m_collar(face, x, y, fw, fh, cube):
    if face in ("front", "back"):
        jag = [2, 0, 1, 3, 0, 1, 2, 0, 1, 0, 2, 1][(x + (6 if face == "back" else 0)) % 12]
        if y < jag:
            return None, False
    if face == "front":
        return jitter(rng.choice(ROBE), -10 + 6 * (y < 2), 4), False
    if y <= 2:
        return ice_px("front", x % 2, y, 2, 3, SPIKE_SHADES[2], FACE_SHADE[face]), False
    return jitter(rng.choice(SPIKE_SHADES[1]), FACE_SHADE[face] - 8 * (y - 2), 5), False


def m_pauldron(face, x, y, fw, fh, cube):
    s = FACE_SHADE[face]
    if face == "top":
        if rng.random() < 0.55:
            return jitter(rng.choice(RIME), 4, 8), False
        return jitter(rng.choice(PLATE), 14, 5), False
    if face == "bottom":
        return jitter(ROBE_DEEP, 0, 3), False
    if y == 0:
        return jitter(rng.choice(RIME), s, 6), False
    if y == fh - 1:
        return jitter(TRIM, s - 20, 5), False
    if (x + y) % 5 == 0:
        return jitter(rng.choice(PLATE), s + 16, 4), False
    return jitter(rng.choice(PLATE), s - 4 * y, 4), False


def m_ice(face, x, y, fw, fh, cube):
    return ice_px(face, x, y, fw, fh, spike_palette(cube)), False


def m_bone(face, x, y, fw, fh, cube):
    return bone_px(face, x, y, fw, fh), False


SKULL_FRONT = {
    (1, 1): "brow", (2, 1): "brow", (4, 1): "brow", (5, 1): "brow",
    (1, 2): "eye_mid", (2, 2): "eye_bright", (1, 3): "socket", (2, 3): "eye_dim",
    (4, 2): "eye_bright", (5, 2): "eye_mid", (4, 3): "eye_dim", (5, 3): "socket",
    (3, 4): "socket", (0, 4): "hollow", (6, 4): "hollow", (0, 5): "hollow", (6, 5): "hollow",
    (1, 6): "tooth", (2, 6): "gap", (3, 6): "tooth", (4, 6): "gap", (5, 6): "tooth",
    (4, 0): "crack", (5, 1): "crack",
}


def m_skull(face, x, y, fw, fh, cube):
    if face == "front":
        key = SKULL_FRONT.get((x, y))
        if key == "brow":
            return jitter(rng.choice(BONE), -34, 4), False
        if key == "socket":
            return SOCKET + (255,), False
        if key and key.startswith("eye"):
            c = {"eye_mid": EYES[0], "eye_bright": EYES[1], "eye_dim": EYES[2]}[key]
            return c + (255,), True
        if key == "hollow":
            return jitter(BONE_SHADOW, -6, 4), False
        if key == "tooth":
            return jitter((236, 244, 250), 0, 3), False
        if key == "gap":
            return jitter((64, 82, 104), 0, 3), False
        if key == "crack":
            return (96, 132, 170, 255), False
        return bone_px(face, x, y, fw, fh, 6 if y == 0 else 0), False
    if face in ("left", "right"):
        if y in (4, 5) and x in ((0, 1) if face == "left" else (fw - 2, fw - 1)):
            return jitter(BONE_SHADOW, -10, 4), False
        if y in (2, 3) and 2 <= x <= 4:
            return jitter(rng.choice(BONE), -16, 4), False
    if face == "top" and (x, y) in ((4, 6), (4, 5), (3, 4), (3, 3)):
        return (96, 132, 170, 255), False
    return bone_px(face, x, y, fw, fh), False


def m_jaw(face, x, y, fw, fh, cube):
    if face == "front":
        if y == 0:
            if x == 3:
                return jitter((40, 54, 80), 0, 3), False
            return (jitter((236, 244, 250), 0, 3) if x % 2 == 0 else jitter((64, 82, 104), 0, 3)), False
        if x == 2:
            return (96, 132, 170, 255), False
        return bone_px(face, x, y, fw, fh, -10), False
    if face == "left" and (x, y) == (1, 1) or face == "right" and (x, y) == (fw - 2, 1):
        return (96, 132, 170, 255), False
    return bone_px(face, x, y, fw, fh, -12), False


def m_crown_band(face, x, y, fw, fh, cube):
    if face == "top":
        return jitter(SPIKE_SHADES[0][0], 10, 6), False
    if face == "bottom":
        return jitter(SPIKE_SHADES[3][1], -30, 5), False
    if face == "front" and x in (3, 4):
        return ((230, 252, 255) if y == 0 else (120, 220, 252)) + (255,), y == 0
    c = SPIKE_SHADES[0 if y == 0 else 3]
    return jitter(rng.choice(c), FACE_SHADE[face] + (14 if y == 0 else -4) + (6 if x % 3 == 0 else 0), 6), False


_spike_index = {}


def spike_palette(cube):
    key = tuple(cube["uv"])
    if key not in _spike_index:
        _spike_index[key] = len(_spike_index)
    return SPIKE_SHADES[_spike_index[key] % len(SPIKE_SHADES)]


def m_spike(face, x, y, fw, fh, cube):
    return ice_px(face, x, y, fw, fh, spike_palette(cube)), False


def m_spike_tip(face, x, y, fw, fh, cube):
    return ice_px(face, x, y, fw, fh, SPIKE_SHADES[2], 16), False


def m_sleeve(face, x, y, fw, fh, cube):
    if face == "bottom":
        return jitter(ROBE_DEEP, -4, 3), False
    return robe_px(face, x, y, fw, fh, 1, 0.2), False


def m_cuff(face, x, y, fw, fh, cube):
    if face == "bottom":
        return jitter((8, 10, 22), 0, 2), False
    if face == "top":
        return jitter(rng.choice(ROBE), 0, 4), False
    if y == fh - 1 and (x * 3 + (1 if face in ("left", "back") else 0)) % 4 == 1:
        return None, False
    if y >= fh - 2:
        return jitter(rng.choice(RIME), FACE_SHADE[face] - 6, 6), False
    return robe_px(face, x, y, fw, fh, 2, 0.5), False


def m_hand(face, x, y, fw, fh, cube):
    if face in ("front", "back") and x == 1:
        return jitter(BONE_SHADOW, 0, 4), False
    return bone_px(face, x, y, fw, fh, -8), False


def m_claw(face, x, y, fw, fh, cube):
    if y == fh - 1:
        return jitter((150, 200, 235), -10, 5), False
    if y % 2 == 1 and face != "top":
        return jitter(BONE_SHADOW, 8, 4), False
    return bone_px(face, x, y, fw, fh, -4), False


def m_shaft(face, x, y, fw, fh, cube):
    if face in ("top", "bottom"):
        return jitter(SHAFT[1], 0, 3), False
    if face in ("front", "right"):
        c = SHAFT[0] if (y // 3) % 4 else SHAFT[2]
    else:
        c = SHAFT[1]
    if rng.random() < 0.06:
        return jitter(FROST_BAND, -20, 6), False
    return jitter(c, 0, 5), False


def m_staff_band(face, x, y, fw, fh, cube):
    return jitter(FROST_BAND, FACE_SHADE[face], 6), False


def m_staff_cap(face, x, y, fw, fh, cube):
    return bone_px(face, x, y, fw, fh, -20), False


def m_staff_prong(face, x, y, fw, fh, cube):
    if y == 0:
        return jitter((200, 236, 252), 0, 4), False
    return bone_px(face, x, y, fw, fh, -26), False


def m_crystal(face, x, y, fw, fh, cube):
    if face == "bottom":
        return CRYSTAL[2] + (255,), True
    if face == "top":
        return CRYSTAL[3] + (255,), True
    core = fw >= 3 and x == 1 and 1 <= y <= fh - 2
    if core:
        return (240, 255, 255, 255), True
    c = CRYSTAL[0] if x == 0 else CRYSTAL[1] if face in ("front", "left") else CRYSTAL[2]
    if y == 0 and x == 0:
        c = CRYSTAL[3]
    return c + (255,), True


def m_crystal_tip(face, x, y, fw, fh, cube):
    return (230, 252, 255, 255), True


MATERIALS = {name[2:]: fn for name, fn in globals().items() if name.startswith("m_")}


for cube, face, x, y, fw, fh, at in texels(model.BONES):
    px, glows = MATERIALS[cube["material"]](face, x, y, fw, fh, cube)
    if px is None:
        continue
    lich.putpixel(at, px)
    if glows:
        lich_glow.putpixel(at, px)
lich.save(f"{OUT}/entity/ice_lich.png")
lich_glow.save(f"{OUT}/entity/ice_lich_glowmask.png")
print("ice lich textures written")
