import math

from geckolib_model import Model, anim, c, write

ASSETS = "src/main/resources/assets/wildspellmobs"
GEO_OUT = f"{ASSETS}/geo/entity/apollo.geo.json"
ANIM_OUT = f"{ASSETS}/animations/entity/apollo.animation.json"
TEX = 128

MODEL = Model("geometry.apollo", TEX, 6, 6, [0, 2.5, 0])
BONES = MODEL.bones
bone = MODEL.bone
geometry = MODEL.geometry

DISC = (0, 48.5, 6)
RING = 20
RAYS = 12
RAY_BASE = 12.5
RAY_LONG = 19
RAY_SHORT = 13


def ray_length(i):
    return RAY_LONG if i % 2 == 0 else RAY_SHORT


bone("root", None, (0, 0, 0))
bone("body", "root", (0, 24, 0))
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
    angle = -360.0 * i / RAYS
    length = ray_length(i)
    bone(f"ray_{i}", "disc", DISC, [0, 0, angle])
    base = (DISC[0], DISC[1] + RAY_BASE, DISC[2] + 0.1)
    width = 4 if i % 2 == 0 else 3
    bone(f"ray_{i}_blade", f"ray_{i}", base, cubes=[
        c((base[0] - width / 2, base[1], base[2]), (width, length, 0), "ray_long" if i % 2 == 0 else "ray_short"),
    ])


MODEL.pack()


def loop(length, frames):
    return {round(t, 3): v for t, v in frames} | {length: frames[0][1]}


def rays(fold=None, length=None, stagger=0.0):
    out = {}
    for i in range(RAYS):
        if fold is not None:
            frames = {}
            for t, v in fold.items():
                key = t if t in (0, length) else min(length - 0.01, round(t + stagger * i, 3))
                frames[key] = v
            out[f"ray_{i}_blade"] = {"rotation": frames}
    return out


def spin(length, turns):
    return {"rotation": {0: [0, 0, 0], length / 2: [0, 0, -180 * turns], length: [0, 0, -360 * turns]}}


def idle():
    L = 6.0
    bones = {
        "body": {"position": loop(L, [(0, [0, 0, 0]), (1.5, [0, 0.8, 0]), (3, [0, 1.4, 0]), (4.5, [0, 0.8, 0])])},
        "disc": spin(L, 0.25),
        "head": {"rotation": loop(L, [(0, [0, 0, 0]), (3, [2, 0, 0])])},
        "right_arm": {"rotation": loop(L, [(0, [0, 0, 0]), (3, [-1.5, 0, 0])])},
        "left_arm": {"rotation": loop(L, [(0, [0, 0, 0]), (3, [1.5, 0, 0])])},
    }
    breath = {}
    for i in range(RAYS):
        phase = 2 * math.pi * i / RAYS
        frames = {}
        for k in range(6):
            t = L * k / 6
            frames[round(t, 3)] = [round(-6 * math.sin(2 * math.pi * k / 6 + phase), 2), 0, 0]
        frames[L] = frames[0]
        breath[f"ray_{i}_blade"] = {"rotation": frames}
    bones.update(breath)
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
        "body": {"position": {0: [0, 0, 0], 1.2: [0, 1.5, 1.5], 1.5: [0, 0.5, -1], 1.8: [0, 0, 0]}},
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
        "body": {"position": loop(L, [(0, [0, -4, 0]), (2, [0, -4.6, 0])])},
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
        "body": {"position": loop(L, [(0, [0, -3, 0]), (0.3, [0.3, -3.2, 0]), (0.6, [-0.3, -2.8, 0]), (0.9, [0.2, -3.1, 0])])},
        "torso": {"rotation": loop(L, [(0, [-6, 0, 0]), (0.6, [-8, 0, 1])])},
        "head": {"rotation": loop(L, [(0, [-14, 0, 0]), (0.6, [-16, 3, 0])])},
        "right_arm": {"rotation": loop(L, [(0, [-110, 20, -20]), (0.3, [-112, 22, -22]), (0.6, [-108, 18, -19])])},
        "right_forearm": {"rotation": loop(L, [(0, [-50, 0, 0])])},
        "left_arm": {"rotation": loop(L, [(0, [-30, 0, 30]), (0.6, [-32, 0, 32])])},
        "disc": {"rotation": loop(L, [(0, [0, 0, 0]), (0.15, [0, 0, 6]), (0.3, [0, 0, -4]), (0.6, [0, 0, 3])])},
    }
    flicker = {}
    for i in range(RAYS):
        start = (i * 5 % RAYS) / RAYS * L
        frames = {0: [-70, 0, 0], L: [-70, 0, 0]}
        frames[round(min(L - 0.05, start + 0.05), 3)] = [-10, 0, 0]
        frames[round(min(L - 0.02, start + 0.2), 3)] = [-70, 0, 0]
        flicker[f"ray_{i}_blade"] = {"rotation": frames}
    out.update(flicker)
    return anim(L, True, out)


def animations():
    return {"format_version": "1.8.0", "animations": {
        "animation.apollo.idle": idle(),
        "animation.apollo.throw": throw(),
        "animation.apollo.focus": focus(),
        "animation.apollo.glare": glare(),
        "animation.apollo.concede": concede(),
        "animation.apollo.warned": warned(),
    }}


if __name__ == "__main__":
    write(GEO_OUT, geometry())
    write(ANIM_OUT, animations())
    print("apollo model and animations written")
