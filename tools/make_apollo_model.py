import math

from geckolib_model import Model, anim, animations, c, loop, write

ASSETS = "src/main/resources/assets/wildspellmobs"
GEO_OUT = f"{ASSETS}/geo/entity/apollo.geo.json"
ANIM_OUT = f"{ASSETS}/animations/entity/apollo.animation.json"
TEX = 256

MODEL = Model("geometry.apollo", TEX, 11, 6, [0, 2.0, -1.5])
BONES = MODEL.bones
bone = MODEL.bone

DISC = (0, 48.5, 6)
RING = 20
RAYS = 12
RAY_BASE = 12.5
RAY_LONG = 19
RAY_SHORT = 13

bone("root", None, (0, 0, 0))
bone("ride", "root", (0, 0, 0))
bone("body", "ride", (0, 24, 0))
bone("hips", "body", (0, 24, 0), cubes=[
    c((-5.5, 21, -3), (11, 6, 6), "kilt"),
    c((-6, 19, -3.5), (12, 3, 7), "kilt_hem"),
    c((-5.5, 26, -3), (11, 1, 6), "belt", 0.4),
])
for side, sx, z, rx in (("right", -1, 0.5, 4), ("left", 1, -1.5, -10)):
    x0 = -5 if side == "right" else 1
    bone(f"{side}_leg", "hips", (2.5 * sx, 22, z), [rx, 0, 1.5 * sx], [
        c((x0, 11, z - 2), (4, 11, 4), "thigh"),
    ])
    bone(f"{side}_shin", f"{side}_leg", (2.5 * sx, 11, z), [-rx * 0.5, 0, 0], [
        c((x0 + 0.5, 2, z - 1.5), (3, 9, 3), "shin", 0.25),
    ])
    bone(f"{side}_foot", f"{side}_shin", (2.5 * sx, 2, z), [32, 0, 0], [
        c((x0 + 0.5, 0, z - 3.5), (3, 2, 5), "foot", 0.25),
    ])

bone("torso", "body", (0, 27, 0), cubes=[
    c((-4.5, 27, -2.5), (9, 4, 5), "waist"),
    c((-6.5, 31, -3), (13, 10, 6), "chest"),
])
bone("neck", "torso", (0, 41, 0), cubes=[c((-1.5, 40.5, -1.5), (3, 4, 3), "neck")])
bone("head", "neck", (0, 44, 0), cubes=[
    c((-4, 44, -4), (8, 9, 8), "face"),
    c((-1, 46.5, -5), (2, 2, 1), "nose"),
    c((-4.5, 51, -4.5), (9, 1, 9), "fillet"),
    c((-4, 52, -4), (8, 1, 8), "crown_hair", 0.4),
    c((-4.5, 41, 2), (9, 11, 3), "back_hair"),
])
for side, sx in (("right", -1), ("left", 1)):
    for k, x in enumerate((2.5, 3.75)):
        px = x * sx
        bone(f"{side}_lock_{k}", "head", (px, 46, -3.5), [-4, 0, 3 * sx], [
            c((px - 0.5, 37 - k, -4.25), (1, 9 + k, 1), "lock"),
        ])

for side, sx in (("right", -1), ("left", 1)):
    x_upper = -10.5 if side == "right" else 6.5
    x_fore = -10 if side == "right" else 7
    bone(f"{side}_arm", "torso", (8.5 * sx, 39.5, 0), [0, 0, 2 * sx], [
        c((x_upper, 30, -2), (4, 11, 4), "upper_arm"),
    ])
    bone(f"{side}_forearm", f"{side}_arm", (8.5 * sx, 30, 0), cubes=[
        c((x_fore, 21, -1.5), (3, 9, 3), "forearm", 0.25),
    ])
    bone(f"{side}_hand", f"{side}_forearm", (8.5 * sx, 21, 0), cubes=[
        c((x_fore - 0.5, 17.5, -2), (4, 4, 4), "fist"),
    ])

bone("disc", "head", DISC)
bone("nimbus", "disc", DISC, cubes=[c((DISC[0] - RING, DISC[1] - RING, DISC[2]), (2 * RING, 2 * RING, 0), "nimbus")])
for i in range(RAYS):
    long = i % 2 == 0
    bone(f"ray_{i}", "disc", DISC, [0, 0, -360.0 * i / RAYS])
    base = (DISC[0], DISC[1] + RAY_BASE, DISC[2] + 0.1)
    width = 4 if long else 3
    bone(f"ray_{i}_blade", f"ray_{i}", base, cubes=[
        c((base[0] - width / 2, base[1], base[2]), (width, RAY_LONG if long else RAY_SHORT, 0), "ray_long" if long else "ray_short"),
    ])


WHEEL = 18
AXLE_Y = -2.75
AXLE_Z = 2.25
RAIL = 13
HORSES = (-15, -5, 5, 15)
WITHERS = (26, -52)
TEAM_PIVOT = (0, 17, -45)
POLE_FROM = (-1.75, -6)
POLE_LENGTH = math.hypot(WITHERS[0] - POLE_FROM[0], WITHERS[1] - POLE_FROM[1])
POLE_PITCH = math.degrees(math.atan2(WITHERS[0] - POLE_FROM[0], POLE_FROM[1] - WITHERS[1]))

bone("chariot", "ride", (0, 0, 0))
bone("car", "chariot", (0, 0, 0), cubes=[
    c((-8, -1, -6), (16, 1, 12), "car_floor"),
    c((-8, 0, -7), (16, RAIL, 1), "car_rail"),
    c((-8, 0, -6), (1, RAIL - 2, 6), "car_side"),
    c((7, 0, -6), (1, RAIL - 2, 6), "car_side"),
    c((-8, 0, 0), (1, 5, 5), "car_side_low"),
    c((7, 0, 0), (1, 5, 5), "car_side_low"),
    c((-12.5, AXLE_Y - 1, AXLE_Z - 1), (25, 2, 2), "axle"),
])
for side, sx in (("right", -1), ("left", 1)):
    x = 12.5 * sx
    bone(f"{side}_wheel", "car", (x, AXLE_Y, AXLE_Z), cubes=[
        c((x - 0.5, AXLE_Y - WHEEL / 2, AXLE_Z - WHEEL / 2), (1, WHEEL, WHEEL), "wheel"),
    ])
bone("pole", "car", (0, POLE_FROM[0], POLE_FROM[1]), [-POLE_PITCH, 0, 0], [
    c((-1, POLE_FROM[0] - 1, POLE_FROM[1] - round(POLE_LENGTH)), (2, 2, round(POLE_LENGTH)), "pole"),
])
bone("team", "chariot", TEAM_PIVOT, cubes=[c((-17, WITHERS[0], WITHERS[1] - 1), (34, 2, 2), "yoke")])
for i, hx in enumerate(HORSES):
    bone(f"horse_{i}", "team", (hx, 17, -45), cubes=[c((hx - 4, 16, -56), (8, 10, 22), "horse_body")])
    bone(f"horse_{i}_tail", f"horse_{i}", (hx, 24, -34), [25, 0, 0], [c((hx - 0.5, 23, -34), (1, 2, 10), "horse_tail")])
    neck_pitch = 40
    bone(f"horse_{i}_neck", f"horse_{i}", (hx, 22, -54), [neck_pitch, 0, 0], [
        c((hx - 2.5, 22, -57), (5, 14, 6), "horse_neck"),
        c((hx - 0.5, 23, -51), (1, 15, 2), "horse_mane"),
    ])
    top_y = 22 + 14 * math.cos(math.radians(neck_pitch))
    top_z = -54 - 14 * math.sin(math.radians(neck_pitch))
    bone(f"horse_{i}_head", f"horse_{i}", (hx, top_y, top_z), [25, 0, 0], [
        c((hx - 2.5, top_y - 2.5, top_z - 12), (5, 6, 13), "horse_head"),
        c((hx - 2.5, top_y + 3, top_z - 2), (1, 3, 1), "horse_ear"),
        c((hx + 1.5, top_y + 3, top_z - 2), (1, 3, 1), "horse_ear"),
    ])
    for leg, lz, pitch in (("fore", -53, -50), ("hind", -37, 40)):
        for side, sx in (("right", -1), ("left", 1)):
            lx = hx + 2.5 * sx
            bone(f"horse_{i}_{leg}_{side}", f"horse_{i}", (lx, 17, lz), [pitch, 0, 0], [
                c((lx - 1, 1, lz - 1), (2, 16, 2), "horse_leg"),
            ])


MODEL.pack()


def rays(fold, length, stagger=0.0):
    out = {}
    for i in range(RAYS):
        frames = {}
        for t, v in fold.items():
            key = t if t in (0, length) else min(length - 0.01, round(t + stagger * i, 3))
            frames[key] = v
        out[f"ray_{i}_blade"] = {"rotation": frames}
    return out


def spin(length, turns):
    return {"rotation": {0: [0, 0, 0], length / 2: [0, 0, -180 * turns], length: [0, 0, -360 * turns]}}


def roll(length, turns):
    return {"rotation": {0: [0, 0, 0], length / 2: [-180 * turns, 0, 0], length: [-360 * turns, 0, 0]}}


def team_idle(length):
    out = {}
    for i in range(len(HORSES)):
        shift = length * (i * 3 % 4) / 4
        frames = {}
        for k in range(4):
            t = round((shift + length * k / 4) % length, 3)
            frames[t] = [0, round(0.7 * math.sin(math.pi * k / 2), 2), 0]
        frames[length] = frames.get(0, [0, 0, 0])
        out[f"horse_{i}"] = {"position": frames}
        out[f"horse_{i}_head"] = {"rotation": loop(length, [(0, [0, 0, 0]), (length / 2, [-4 if i % 2 else 3, 2 * (i - 1.5), 0])])}
    return out


def idle():
    L = 6.0
    bones = {
        "ride": {"position": loop(L, [(0, [0, 0, 0]), (1.5, [0, 0.8, 0]), (3, [0, 1.4, 0]), (4.5, [0, 0.8, 0])])},
        "disc": spin(L, 0.25),
        "right_wheel": roll(L, 1.5),
        "left_wheel": roll(L, 1.5),
        "head": {"rotation": loop(L, [(0, [0, 0, 0]), (3, [2, 0, 0])])},
        "right_arm": {"rotation": loop(L, [(0, [0, 0, 0]), (3, [-1.5, 0, 0])])},
        "left_arm": {"rotation": loop(L, [(0, [0, 0, 0]), (3, [1.5, 0, 0])])},
    }
    for i in range(RAYS):
        phase = 2 * math.pi * i / RAYS
        frames = {}
        for k in range(6):
            t = L * k / 6
            frames[round(t, 3)] = [round(-6 * math.sin(2 * math.pi * k / 6 + phase), 2), 0, 0]
        frames[L] = frames[0]
        bones[f"ray_{i}_blade"] = {"rotation": frames}
    bones.update(team_idle(L))
    return anim(L, True, bones)


def throw():
    L = 1.0
    return anim(L, False, {
        "torso": {"rotation": {0: [0, 0, 0], 0.3: [-8, -14, 0], 0.5: [10, 10, 0], 0.75: [4, 4, 0], 1: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 0.25: [-170, 0, -10], 0.4: [-175, 0, -6], 0.55: [-70, 0, 4],
                                   0.8: [-40, 0, 0], 1: [0, 0, 0]}},
        "right_forearm": {"rotation": {0: [0, 0, 0], 0.25: [-60, 0, 0], 0.4: [-70, 0, 0], 0.55: [0, 0, 0], 1: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 0.3: [-30, 0, 14], 0.55: [10, 0, 8], 1: [0, 0, 0]}},
        "head": {"rotation": {0: [0, 0, 0], 0.3: [-6, 8, 0], 0.55: [4, -4, 0], 1: [0, 0, 0]}},
        "disc": {"rotation": {0: [0, 0, 0], 0.4: [0, 0, 12], 0.55: [0, 0, -20], 1: [0, 0, 0]}},
    })


def focus():
    L = 2.0
    return anim(L, True, {
        "torso": {"rotation": loop(L, [(0, [4, 10, 0]), (1, [5, 11, 0])])},
        "left_arm": {"rotation": loop(L, [(0, [-90, -12, 0]), (0.5, [-91, -11, 0]), (1, [-89, -12, 0]), (1.5, [-90, -13, 0])])},
        "left_forearm": {"rotation": loop(L, [(0, [0, 0, 0])])},
        "left_hand": {"rotation": loop(L, [(0, [-70, 0, 0]), (1, [-74, 0, 0])])},
        "right_arm": {"rotation": loop(L, [(0, [8, 0, -10]), (1, [9, 0, -11])])},
        "head": {"rotation": loop(L, [(0, [6, -8, 0]), (1, [7, -8, 0])])},
        "disc": {"rotation": {0: [0, 0, 0], 1: [0, 0, -45], 2: [0, 0, -90]}},
    })


def glare():
    L = 1.8
    out = {
        "ride": {"position": {0: [0, 0, 0], 1.2: [0, 1.5, 1.5], 1.5: [0, 0.5, -1], 1.8: [0, 0, 0]}},
        "team": {"rotation": {0: [0, 0, 0], 1.0: [-14, 0, 0], 1.2: [-18, 0, 0], 1.5: [4, 0, 0], 1.8: [0, 0, 0]}},
        "torso": {"rotation": {0: [0, 0, 0], 1.2: [-16, 0, 0], 1.35: [8, 0, 0], 1.8: [0, 0, 0]}},
        "head": {"rotation": {0: [0, 0, 0], 1.2: [-22, 0, 0], 1.35: [6, 0, 0], 1.8: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 1.0: [-20, 0, 95], 1.2: [-24, 0, 110], 1.35: [-40, 0, 80], 1.8: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 1.0: [-20, 0, -95], 1.2: [-24, 0, -110], 1.35: [-40, 0, -80], 1.8: [0, 0, 0]}},
        "disc": {"rotation": {0: [0, 0, 0], 1.2: [0, 0, -300], 1.35: [0, 0, -360], 1.8: [0, 0, -360]}},
    }
    out.update(rays({0: [0, 0, 0], 1.0: [24, 0, 0], 1.2: [30, 0, 0], 1.35: [-10, 0, 0], 1.8: [0, 0, 0]}, L))
    return anim(L, False, out)


def concede():
    L = 4.0
    out = {
        "ride": {"position": loop(L, [(0, [0, -4, 0]), (2, [0, -4.6, 0])])},
        "team": {"rotation": loop(L, [(0, [6, 0, 0]), (2, [7, 0, 0])])},
        "torso": {"rotation": loop(L, [(0, [14, 0, 0]), (2, [15, 0, 0])])},
        "head": {"rotation": loop(L, [(0, [24, 0, 0]), (2, [26, 0, 0])])},
        "right_arm": {"rotation": loop(L, [(0, [-28, 0, 34]), (2, [-30, 0, 36])])},
        "left_arm": {"rotation": loop(L, [(0, [-28, 0, -34]), (2, [-30, 0, -36])])},
        "right_hand": {"rotation": loop(L, [(0, [0, 70, 0])])},
        "left_hand": {"rotation": loop(L, [(0, [0, -70, 0])])},
        "right_leg": {"rotation": loop(L, [(0, [-20, 0, 0])])},
        "right_shin": {"rotation": loop(L, [(0, [40, 0, 0])])},
        "left_leg": {"rotation": loop(L, [(0, [16, 0, 0])])},
        "left_shin": {"rotation": loop(L, [(0, [30, 0, 0])])},
        "disc": {"rotation": loop(L, [(0, [12, 0, 0]), (2, [13, 0, -4])])},
    }
    out.update(rays(loop(L, [(0, [-62, 0, 0]), (2, [-66, 0, 0])]), L, 0.05))
    return anim(L, True, out)


def warned():
    L = 1.2
    out = {
        "ride": {"position": loop(L, [(0, [0, -3, 0]), (0.3, [0.3, -3.2, 0]), (0.6, [-0.3, -2.8, 0]), (0.9, [0.2, -3.1, 0])])},
        "torso": {"rotation": loop(L, [(0, [-6, 0, 0]), (0.6, [-8, 0, 1])])},
        "head": {"rotation": loop(L, [(0, [-14, 0, 0]), (0.6, [-16, 3, 0])])},
        "right_arm": {"rotation": loop(L, [(0, [-110, 20, -20]), (0.3, [-112, 22, -22]), (0.6, [-108, 18, -19])])},
        "right_forearm": {"rotation": loop(L, [(0, [-50, 0, 0])])},
        "left_arm": {"rotation": loop(L, [(0, [-30, 0, 30]), (0.6, [-32, 0, 32])])},
        "disc": {"rotation": loop(L, [(0, [0, 0, 0]), (0.15, [0, 0, 6]), (0.3, [0, 0, -4]), (0.6, [0, 0, 3])])},
    }
    for i in range(RAYS):
        start = (i * 5 % RAYS) / RAYS * L
        frames = {0: [-70, 0, 0], L: [-70, 0, 0]}
        frames[round(min(L - 0.05, start + 0.05), 3)] = [-10, 0, 0]
        frames[round(min(L - 0.02, start + 0.2), 3)] = [-70, 0, 0]
        out[f"ray_{i}_blade"] = {"rotation": frames}
    return anim(L, True, out)


if __name__ == "__main__":
    write(GEO_OUT, MODEL.geometry())
    write(ANIM_OUT, animations("apollo", {"idle": idle(), "throw": throw(), "focus": focus(), "glare": glare(),
                                          "concede": concede(), "warned": warned()}))
    print("apollo model and animations written")
