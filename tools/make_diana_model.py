import math

from geckolib_model import Model, anim, c, write

ASSETS = "src/main/resources/assets/wildspellmobs"
GEO_OUT = f"{ASSETS}/geo/entity/diana.geo.json"
ANIM_OUT = f"{ASSETS}/animations/entity/diana.animation.json"
TEX = 128

MODEL = Model("geometry.diana", TEX, 4, 4, [0, 1.5, 0])
BONES = MODEL.bones
bone = MODEL.bone
geometry = MODEL.geometry

MOON_CENTER = (0, 33.5, 0)
GRIP = (5, 11, 0)
BOW_RADIUS = 11
BOW_SEGMENTS = 11
BOW_SPAN = 75
TIP_Y = BOW_RADIUS * math.sin(math.radians(BOW_SPAN))
TIP_Z = BOW_RADIUS * (1 - math.cos(math.radians(BOW_SPAN)))
DRAW_BACK = 6.0

bone("root", None, (0, 0, 0))
bone("body", "root", (0, 17, 0))
bone("hips", "body", (0, 17, 0), cubes=[
    c((-4, 13, -2.5), (8, 6, 5), "skirt"),
    c((-4.5, 11, -3), (9, 2, 6), "skirt_hem"),
])
for side, sx in (("right", -1), ("left", 1)):
    x0 = -3.25 if side == "right" else 0.25
    bone(f"{side}_leg", "hips", (1.75 * sx, 16, 0), cubes=[c((x0, 8, -1.5), (3, 8, 3), "thigh", -0.1)])
    bone(f"{side}_shin", f"{side}_leg", (1.75 * sx, 8, 0), cubes=[c((x0, 0, -1.5), (3, 8, 3), "shin", -0.3)])

bone("torso", "body", (0, 19, 0), cubes=[c((-3.5, 19, -2), (7, 9, 4), "chiton")])
bone("neck", "torso", (0, 28, 0), cubes=[c((-1, 28, -1), (2, 2, 2), "skin")])
bone("head", "neck", (0, 30, 0), cubes=[
    c((-3.5, 30, -3.5), (7, 7, 7), "moon"),
    c((-2.5, 31, -4.5), (5, 5, 1), "moon"),
    c((-2.5, 31, 3.5), (5, 5, 1), "moon"),
    c((3.5, 31, -2.5), (1, 5, 5), "moon"),
    c((-4.5, 31, -2.5), (1, 5, 5), "moon"),
    c((-2.5, 37, -2.5), (5, 1, 5), "moon"),
])
bone("veil_0", "torso", (0, 28, 2.5), [4, 0, 0], [c((-4.5, 19, 2), (9, 9, 1), "veil")])
bone("veil_1", "veil_0", (0, 19, 2.5), [4, 0, 0], [c((-4.5, 10, 2), (9, 9, 1), "veil")])
bone("veil_2", "veil_1", (0, 10, 2.5), [4, 0, 0], [c((-4, 1, 2), (8, 9, 1), "veil_end")])

for side, sx in (("right", -1), ("left", 1)):
    x0 = -6 if side == "right" else 4
    bone(f"{side}_arm", "torso", (5 * sx, 27, 0), [0, 0, 4 * sx], [c((x0, 19, -1), (2, 8, 2), "skin")])
    bone(f"{side}_forearm", f"{side}_arm", (5 * sx, 19, 0), cubes=[c((x0, 12, -1), (2, 7, 2), "bracer")])
    bone(f"{side}_hand", f"{side}_forearm", (5 * sx, 12, 0), cubes=[c((x0, 10, -1), (2, 2, 2), "skin")])

bone("bow", "left_hand", GRIP)
for i in range(BOW_SEGMENTS):
    phi = BOW_SPAN * (2 * i / (BOW_SEGMENTS - 1) - 1)
    y = GRIP[1] + BOW_RADIUS * math.sin(math.radians(phi))
    z = GRIP[2] - 1 + BOW_RADIUS * (1 - math.cos(math.radians(phi)))
    tip = i in (0, BOW_SEGMENTS - 1)
    thick = 2 if abs(i - (BOW_SEGMENTS - 1) / 2) < 2.5 else 1
    material = "horn" if tip else "bow"
    bone(f"bow_{i}", "bow", (GRIP[0], y, z), [phi, 0, 0], [
        c((GRIP[0] - 0.5, y - 1.5, z - thick + 0.5), (1, 3, thick), material, 0.1),
    ])
TOP = (GRIP[0], GRIP[1] + TIP_Y, GRIP[2] - 1 + TIP_Z)
BOTTOM = (GRIP[0], GRIP[1] - TIP_Y, GRIP[2] - 1 + TIP_Z)
bone("string_top", "bow", TOP, cubes=[c((TOP[0] - 0.5, TOP[1] - TIP_Y, TOP[2] - 0.5), (1, int(round(TIP_Y)), 1), "string", -0.4)])
bone("string_bottom", "bow", BOTTOM, cubes=[c((BOTTOM[0] - 0.5, BOTTOM[1], BOTTOM[2] - 0.5), (1, int(round(TIP_Y)), 1), "string", -0.4)])


MODEL.pack()

STRING_ANGLE = math.degrees(math.atan2(DRAW_BACK, TIP_Y))


def loop(length, frames):
    return {round(t, 3): v for t, v in frames} | {length: frames[0][1]}


def veil(length, amp, lift=0.0):
    out = {}
    for i in range(3):
        frames = {}
        for k in range(4):
            t = length * k / 4
            frames[round(t, 3)] = [round(lift * (i + 1) + amp * (1 + 0.4 * i) * math.sin(2 * math.pi * k / 4 - 0.9 * i), 2), 0,
                                   round(amp * 0.4 * math.cos(2 * math.pi * k / 4 - 0.9 * i), 2)]
        frames[length] = frames[0]
        out[f"veil_{i}"] = {"rotation": frames}
    return out


POISE = {
    "left_leg": {"rotation": {0: [-40, 0, 0]}},
    "left_shin": {"rotation": {0: [60, 0, 0]}},
    "right_leg": {"rotation": {0: [8, 0, 0]}},
    "right_shin": {"rotation": {0: [14, 0, 0]}},
}


def held(bones, length):
    for name, channels in POISE.items():
        bones.setdefault(name, {"rotation": {0: channels["rotation"][0], length: channels["rotation"][0]}})
    return bones


DRAWN = {
    "torso": [0, 55, 0],
    "head": [0, -50, 0],
    "left_arm": [-90, -55, 0],
    "left_forearm": [0, 0, 0],
    "bow": [90, 0, 0],
    "right_arm": [-80, -62, 0],
    "right_forearm": [-95, 0, 0],
    "string_top": [-STRING_ANGLE, 0, 0],
    "string_bottom": [STRING_ANGLE, 0, 0],
}


def idle():
    L = 4.0
    bones = {
        "body": {"position": loop(L, [(0, [0, 0, 0]), (2, [0, 1.2, 0])])},
        "torso": {"rotation": loop(L, [(0, [4, 0, 0]), (2, [3, 0, 0])])},
        "head": {"rotation": loop(L, [(0, [0, 0, 0]), (1.4, [0, 14, 0]), (2.4, [0, 14, 0]), (3.2, [0, -10, 0])])},
        "left_arm": {"rotation": loop(L, [(0, [-12, 0, -4]), (2, [-14, 0, -4])])},
        "right_arm": {"rotation": loop(L, [(0, [6, 0, 6]), (2, [4, 0, 6])])},
    }
    bones.update(veil(L, 3, 1))
    return anim(L, True, held(bones, L))


def draw():
    L = 0.5
    bones = {name: {"rotation": {0: [0, 0, 0], 0.3: [v * 0.8 for v in value], L: value}} for name, value in DRAWN.items()}
    bones["string_top"]["rotation"] = {0: [0, 0, 0], 0.3: [0, 0, 0], L: DRAWN["string_top"]}
    bones["string_bottom"]["rotation"] = {0: [0, 0, 0], 0.3: [0, 0, 0], L: DRAWN["string_bottom"]}
    bones.update(veil(L, 2, 3))
    return anim(L, False, held(bones, L))


def hold():
    L = 1.0
    bones = {name: {"rotation": loop(L, [(0, value)])} for name, value in DRAWN.items()}
    bones["left_arm"] = {"rotation": loop(L, [(0, DRAWN["left_arm"]), (0.25, [-90.6, -55, 0.4]), (0.5, [-89.6, -55, -0.3]),
                                              (0.75, [-90.3, -55, 0.3])])}
    bones["body"] = {"position": loop(L, [(0, [0, 0, 0]), (0.5, [0, 0.3, 0])])}
    bones.update(veil(L, 2, 3))
    return anim(L, True, held(bones, L))


def loose():
    L = 0.4
    bones = {name: {"rotation": {0: value, L: value}} for name, value in DRAWN.items()}
    bones["right_arm"] = {"rotation": {0: DRAWN["right_arm"], 0.08: [-100, -60, 10], L: [-60, -20, 10]}}
    bones["right_forearm"] = {"rotation": {0: DRAWN["right_forearm"], 0.08: [-40, 0, 0], L: [-20, 0, 0]}}
    bones["left_arm"] = {"rotation": {0: DRAWN["left_arm"], 0.08: [-98, -55, 0], L: [-80, -40, 0]}}
    bones["string_top"] = {"rotation": {0: DRAWN["string_top"], 0.04: [STRING_ANGLE * 0.3, 0, 0], 0.1: [0, 0, 0], L: [0, 0, 0]}}
    bones["string_bottom"] = {"rotation": {0: DRAWN["string_bottom"], 0.04: [-STRING_ANGLE * 0.3, 0, 0], 0.1: [0, 0, 0], L: [0, 0, 0]}}
    return anim(L, False, held(bones, L))


def slash():
    L = 0.6
    return anim(L, False, held({
        "torso": {"rotation": {0: [0, 0, 0], 0.15: [0, 50, 0], 0.3: [10, -40, 0], 0.6: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 0.15: [-80, 70, 0], 0.3: [-80, -60, 0], 0.45: [-60, -40, 0], 0.6: [0, 0, 0]}},
        "bow": {"rotation": {0: [0, 0, 0], 0.15: [0, 0, 90], 0.3: [0, 0, 90], 0.6: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 0.3: [-20, 0, 30], 0.6: [0, 0, 0]}},
        "head": {"rotation": {0: [0, 0, 0], 0.15: [0, -30, 0], 0.3: [0, 20, 0], 0.6: [0, 0, 0]}},
    }, L))


KNEEL = {
    "body": {"position": [0, -6, 0]},
    "torso": {"rotation": [18, 0, 0]},
    "head": {"rotation": [26, 0, 0]},
    "left_leg": {"rotation": [-80, 0, 0]},
    "left_shin": {"rotation": [80, 0, 0]},
    "right_leg": {"rotation": [-10, 0, 0]},
    "right_shin": {"rotation": [95, 0, 0]},
    "left_arm": {"rotation": [-30, 0, -10]},
    "bow": {"rotation": [-60, 0, 0]},
    "right_arm": {"rotation": [-40, 0, 20]},
    "right_forearm": {"rotation": [-30, 0, 0]},
}


def yield_():
    L = 1.0
    bones = {}
    for name, channels in KNEEL.items():
        for ch, value in channels.items():
            start = POISE.get(name, {}).get("rotation", {0: [0, 0, 0]})[0] if ch == "rotation" else [0, 0, 0]
            bones.setdefault(name, {})[ch] = {0: start, 0.6: [v * 1.1 for v in value], L: value}
    bones.update(veil(L, 2, 2))
    return anim(L, False, bones)


def kneel():
    L = 3.0
    bones = {name: {ch: loop(L, [(0, value)]) for ch, value in channels.items()} for name, channels in KNEEL.items()}
    bones["head"] = {"rotation": loop(L, [(0, KNEEL["head"]["rotation"]), (1.5, [30, 0, 0])])}
    bones.update(veil(L, 2, 2))
    return anim(L, True, bones)


def animations():
    return {"format_version": "1.8.0", "animations": {
        "animation.diana.idle": idle(),
        "animation.diana.draw": draw(),
        "animation.diana.hold": hold(),
        "animation.diana.loose": loose(),
        "animation.diana.slash": slash(),
        "animation.diana.yield": yield_(),
        "animation.diana.kneel": kneel(),
    }}


if __name__ == "__main__":
    write(GEO_OUT, geometry())
    write(ANIM_OUT, animations())
    print("diana model and animations written")
