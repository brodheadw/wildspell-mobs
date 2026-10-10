import math

from PIL import Image

import make_watcher_model as model
from geckolib_model import texel_point
from painting import hash01 as h, mix, paint_model, ramp

OUT = "src/main/resources/assets/wildspellmobs/textures"

ASH = [(34, 31, 33), (52, 47, 47), (74, 67, 64), (98, 90, 84), (124, 115, 106), (150, 140, 128)]
HIDE = [(30, 25, 27), (44, 37, 38), (60, 51, 51), (78, 68, 66)]
MEMBRANE = [(104, 80, 86), (128, 100, 104), (150, 122, 124), (170, 144, 142)]
VEIN = (82, 46, 56)
SCLERA = [(206, 196, 168), (224, 216, 190), (236, 230, 208)]
LID = (58, 36, 40)
PUPIL = (16, 11, 10)
SHADE = {"top": 0.1, "front": 0.0, "back": -0.06, "left": -0.03, "right": -0.03, "bottom": -0.2}

ACCENT = {"athoth": (214, 204, 180), "eloaiou": (96, 100, 114), "astaphaios": (150, 126, 58), "yao": (64, 124, 104),
          "sabaoth": (98, 86, 146), "adonin": (198, 162, 70), "sabbataios": (196, 84, 40)}

HEAD = {
    "athoth": {"face": [(120, 108, 94), (152, 140, 122), (184, 172, 152), (206, 196, 178)],
               "wool": [(172, 162, 142), (198, 190, 170), (220, 214, 196), (234, 230, 216)]},
    "eloaiou": {"face": [(66, 60, 56), (90, 82, 76), (114, 106, 98), (136, 128, 118)],
                "muzzle": [(150, 142, 130), (178, 170, 158), (200, 194, 182)], "mane": [(28, 24, 24), (40, 35, 34), (54, 48, 46)]},
    "astaphaios": {"face": [(108, 86, 58), (138, 112, 76), (166, 138, 96), (186, 160, 116)],
                   "muzzle": [(26, 21, 20), (38, 31, 29), (52, 43, 40)], "mane": [(34, 28, 24), (50, 41, 34), (66, 54, 44)],
                   "jaw": [(60, 26, 28), (92, 40, 40), (120, 58, 56)]},
    "yao": {"scale": [(30, 40, 28), (44, 58, 38), (60, 78, 48), (80, 98, 60), (104, 120, 76)],
            "muzzle": [(120, 132, 92), (146, 156, 112), (170, 178, 134)]},
    "sabaoth": {"face": [(48, 54, 44), (66, 74, 58), (86, 94, 72), (108, 114, 88)],
                "muzzle": [(30, 24, 26), (44, 36, 38), (58, 48, 50)], "jaw": [(76, 70, 54), (100, 94, 72), (124, 118, 92)],
                "horn": [(98, 88, 70), (140, 130, 106), (184, 174, 148), (214, 206, 182)]},
    "adonin": {"face": [(46, 34, 26), (62, 46, 35), (80, 60, 45), (98, 74, 56)],
               "brow": [(30, 22, 18), (42, 31, 25), (56, 42, 33)], "muzzle": [(104, 80, 66), (130, 102, 84), (152, 122, 102)]},
    "sabbataios": {"flame_core": [(96, 26, 12), (142, 44, 16), (186, 70, 22), (220, 106, 34)],
                   "flame": [(150, 34, 14), (206, 72, 20), (240, 128, 34), (252, 190, 76), (255, 236, 168)]},
}

state = {"face": None, "bones": {}}


def feathers(face, x, y, fw, fh, salt, cube):
    row = 3
    r, k = divmod(y, row)
    shift = (r * 2) % 3
    quill = (x + shift) % 3
    level = 0.42 + 0.22 * h(x // 3, r, salt) + SHADE[face]
    level += 0.14 if k == row - 1 else (-0.08 if k == 0 else 0.0)
    level -= 0.1 if quill == 0 else 0.0
    color = ramp(ASH, level)
    if h(x, y, salt + 3) > 0.9:
        color = mix(color, ASH[1], 0.5)
    tip = y / max(1, fh - 1)
    return mix(color, ACCENT[state["face"]], 0.08 + 0.34 * tip ** 2)


def eye_at(bone, face, x, y):
    for wing, inner, u, v, size in model.MODELS[state["face"]].eyes:
        if wing == bone and inner == face:
            dx, dy = x - u, y - v
            reach = {1: 0, 2: 1, 3: 2}[size]
            if dy == 0 and abs(dx) <= reach or size > 1 and dy == -1 and abs(dx) <= reach - 1 or size == 3 and dy == 1 and abs(dx) <= 1:
                return "sclera", dx, dy
            if abs(dx) <= reach + 1 and -2 <= dy - (0 if size < 3 else 0) <= (1 if size < 3 else 2) and abs(dx) + abs(dy) <= reach + 2:
                return "lid", dx, dy
    return None


def membrane(face, x, y, fw, fh, salt, cube):
    bone = state["bones"][id(cube)]
    eye = eye_at(bone, face, x, y)
    if eye and eye[0] == "sclera":
        return ramp(SCLERA, 0.6 - 0.25 * abs(eye[1]) + 0.2 * h(x, y, salt)), 110
    if eye:
        return mix(LID, MEMBRANE[0], 0.35 * h(x, y, salt + 1)), None
    level = 0.45 + 0.25 * h(x // 2, y // 2, salt) + 0.1 * math.sin(y * 0.7 + x)
    color = ramp(MEMBRANE, level)
    if (x + y * 2 + int(4 * h(0, y // 4, salt))) % 7 == 0 and h(x, y, salt + 5) > 0.3:
        color = mix(color, VEIN, 0.55)
    return color, None


def ragged(cube, face, x, y):
    (x0, y0, z0), (w, hh, d) = cube["origin"], cube["size"]
    p = texel_point(cube, face, x, y)
    bottom = p[1] - y0
    if w == 1:
        far, along = z0 + d - p[2], p[1]
        across = p[2]
    else:
        far = (x0 + w - p[0]) if x0 >= 0 else (p[0] - x0)
        along, across = p[1], abs(p[0])
    if bottom < 1 and h(int(across), 1, 7) > 0.45:
        return True
    if bottom < 2 and h(int(across), 2, 7) > 0.8:
        return True
    tip = state["bones"][id(cube)].split("_")[0] in ("hand", "foot") or "_tip" in state["bones"][id(cube)]
    return tip and far < 1 and int(along) % 3 == 0


def wing(face, x, y, fw, fh, salt, cube):
    bone = state["bones"][id(cube)]
    if ragged(cube, face, x, y):
        return None, None
    inner = next((i for w, i, _, _, _ in model.MODELS[state["face"]].eyes if w == bone), None)
    if inner is None:
        inner = "right" if bone.endswith("_right") else "left"
    if face == inner:
        return membrane(face, x, y, fw, fh, salt, cube)
    if face in ("top", "bottom") and cube["size"][0] == 1:
        return ramp(ASH, 0.2), None
    return feathers(face, x, y, fw, fh, salt, cube), None


def hide(face, x, y, fw, fh, salt, cube):
    level = 0.45 + 0.3 * h(x, y // 2, salt) + SHADE[face]
    if y % 3 == 0:
        level -= 0.2
    return ramp(HIDE, level), None


def sealed(cube, face, x, y, fw, fh):
    bone = state["bones"][id(cube)]
    if bone != "head" or cube is not model_head_skull(cube):
        return False
    if state["face"] in ("adonin", "sabbataios"):
        return face == "front" and y == (2 if state["face"] == "adonin" else 1) and 0 < x < fw - 1 and x != fw // 2
    front = fw - 1 if face == "right" else 0
    return face in ("left", "right") and y == 1 and abs(x - front) <= 1


def model_head_skull(cube):
    m = model.MODELS[state["face"]]
    for b in m.bones:
        if b["name"] == "head" and b["cubes"]:
            return b["cubes"][0]
    return None


def head_part(material):
    def paint(face, x, y, fw, fh, salt, cube):
        palette = HEAD[state["face"]][material]
        level = 0.5 + 0.28 * (h(x, y, salt) - 0.5) + SHADE[face] * 1.4
        glow = None
        if material == "wool":
            level = 0.4 + 0.5 * h(x // 2 + y, y // 2, salt)
        elif material == "scale":
            level = 0.55 + SHADE[face] + (0.15 if (x + (y // 2) % 2) % 2 == 0 else -0.1) + 0.1 * h(x, y, salt)
            if face == "bottom":
                return ramp(HEAD["yao"]["muzzle"], 0.4), None
        elif material == "flame":
            level = 1.0 - y / max(1, fh) * 0.8 + 0.25 * (h(x, y, salt) - 0.5)
            if face == "bottom":
                level = 0.95
            glow = 255
        elif material == "flame_core":
            level = 0.5 + 0.4 * (y / max(1, fh)) + 0.2 * (h(x, y, salt) - 0.5)
            glow = 200
        elif material == "horn":
            level = 0.3 + 0.6 * (x / max(1, fw - 1) if face in ("top", "bottom", "left", "right") else 0.5) + 0.1 * h(x, y, salt)
        if state["face"] == "astaphaios" and material == "face" and h(x // 2, y // 2, salt + 9) > 0.72:
            return mix(ramp(palette, level), HEAD["astaphaios"]["mane"][0], 0.7), None
        color = ramp(palette, level)
        if state["face"] == "adonin" and material == "face" and face == "front" and y >= 3:
            color = mix(color, HEAD["adonin"]["muzzle"][1], 0.7)
        if state["face"] in ("eloaiou", "astaphaios") and material == "muzzle" and face == "front" and y == fh // 2 and x in (0, fw - 1):
            color = (12, 9, 9)
        return color, glow
    return paint


MATERIALS = {"wing_hood": wing, "wing_hood_tip": wing, "wing_foot": wing, "wing_arm": wing, "wing_hand": wing, "hide": hide}


def texel(cube, face, x, y, fw, fh):
    material = cube["material"]
    salt = sum(map(ord, material)) + int(cube["origin"][0] * 7 + cube["origin"][1] * 13 + cube["origin"][2] * 17)
    if material == "pupil":
        return PUPIL + (255,), None
    painter = MATERIALS.get(material) or head_part(material)
    color, glow = painter(face, x, y, fw, fh, salt, cube)
    if color is None:
        return None, None
    if sealed(cube, face, x, y, fw, fh):
        color = (110, 90, 70) if state["face"] == "sabbataios" else LID
        glow = None
    return tuple(color) + (255,), ((255, 255, 255, glow) if glow else None)


def icon(path, draw):
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y in range(18):
        for x in range(18):
            px = draw(x - 8.5, y - 8.5)
            if px:
                img.putpixel((x, y), px + (255,))
    img.save(path)


def weighed(x, y):
    if -6 <= x <= 6 and -7 <= y <= -1:
        edge = abs(x) == 6 or y in (-7, -1)
        return (58, 54, 60) if edge else ramp([(86, 82, 90), (110, 106, 112), (132, 128, 134)], 0.5 + 0.4 * h(int(x), int(y), 3) - 0.3)
    if 1 <= y <= 2 and abs(x) <= 7 - (y - 1) * 2:
        return (150, 120, 96)
    if 3 <= y <= 7 and abs(x) <= 1:
        return (120, 92, 72)
    return None


def bound(x, y):
    r = math.hypot(x, y * 1.6)
    if 4.5 <= r <= 7.0:
        strand = (int((math.atan2(y, x) + math.pi) * 4) % 2)
        return (92, 112, 64) if strand else (58, 76, 40)
    return None


def glare(x, y):
    r = math.hypot(x, y)
    if r <= 3.5:
        return (255, 250, 226)
    angle = math.atan2(y, x) * 6 / math.pi
    if r <= 8 and abs(angle - round(angle)) < 0.18 * (8 - r) / 4:
        return (250, 214, 120)
    return None


def main():
    for face, m in model.MODELS.items():
        state["face"] = face
        state["bones"] = {id(cube): b["name"] for b in m.bones for cube in b["cubes"]}
        paint_model(m.bones, model.TEX, texel, f"{OUT}/entity/watcher_{face}.png", f"{OUT}/entity/watcher_{face}_glowmask.png")
    icon(f"{OUT}/mob_effect/weighed.png", weighed)
    icon(f"{OUT}/mob_effect/bound.png", bound)
    icon(f"{OUT}/mob_effect/glare.png", glare)


if __name__ == "__main__":
    main()
