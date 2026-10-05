import math

from geckolib_model import Model, anim, c, write

ASSETS = "src/main/resources/assets/wildspellmobs"
GEO_OUT = f"{ASSETS}/geo/entity/stemwalker.geo.json"
ANIM_OUT = f"{ASSETS}/animations/entity/stemwalker.animation.json"
TEX = 128

MODEL = Model("geometry.stemwalker", TEX, 3, 4, [0, 1.5, 0])
BONES = MODEL.bones
bone = MODEL.bone
geometry = MODEL.geometry

THREADS = [(-2.0, -1.5, 4), (0.0, -0.5, 6), (2.0, -1.5, 3)]

bone("root", None, (0, 0, 0))
bone("body", "root", (0, 25, 0))
bone("hips", "body", (0, 25, 0), cubes=[c((-3, 23, -1.5), (6, 3, 3), "trunk")])
for side, sx in (("right", -1), ("left", 1)):
    x0 = -3.5 if side == "right" else 1.5
    bone(f"{side}_leg", "hips", (2.5 * sx, 24, 0), cubes=[c((x0, 12, -1), (2, 12, 2), "thigh")])
    bone(f"{side}_shin", f"{side}_leg", (2.5 * sx, 12, 0), cubes=[c((x0, 1, -1), (2, 11, 2), "shin", -0.1)])
    bone(f"{side}_foot", f"{side}_shin", (2.5 * sx, 1, 0), cubes=[
        c((x0 - 0.5, 0, -3), (3, 1, 4), "root"),
        c((x0 + 0.5 * sx, 0, 1), (1, 1, 2), "root", -0.1),
    ])

bone("torso", "body", (0, 26, 0), [16, 0, -4], [
    c((-2, 26, -1.5), (4, 11, 3), "trunk"),
    c((1.5, 28, -0.5), (1, 9, 1), "stem_thin", 0.1),
    c((-3.0, 29, 0.0), (1, 7, 1), "stem_thin", 0.1),
    c((-1.5, 35, 1.0), (1, 6, 1), "shard"),
    c((0.5, 34, 0.5), (1, 4, 1), "shard"),
    c((-2.5, 33, 1.5), (3, 1, 2), "cap"),
])
bone("growth", "torso", (3, 36, 0), cubes=[
    c((2.5, 36, -1.5), (2, 1, 2), "cap"),
    c((3.0, 35, -1.0), (1, 1, 1), "stem_thin"),
    c((4.0, 35.5, 0.5), (1, 1, 1), "cap"),
])
for side, sx, top, fore in (("right", -1, 36, 15), ("left", 1, 35, 12)):
    x0 = -5 if side == "right" else 3
    elbow = top - 11
    wrist = elbow - fore
    bone(f"{side}_arm", "torso", (4 * sx, top, 0), [-12, 0, 6 * sx], [c((x0, elbow, -1), (2, 11, 2), "stem")])
    bone(f"{side}_forearm", f"{side}_arm", (4 * sx, elbow, 0), [-8, 0, 0], [c((x0, wrist, -1), (2, fore, 2), "stem", -0.15)])
    bone(f"{side}_hand", f"{side}_forearm", (4 * sx, wrist, 0), cubes=[
        c((x0 - 0.5, wrist - 5, -1.5), (1, 5, 1), "finger", -0.1),
        c((x0 + 0.5, wrist - 6, -0.5), (1, 6, 1), "finger", -0.1),
        c((x0 + 1.5 if sx < 0 else x0 - 0.5, wrist - 4, 0.5), (1, 4, 1), "finger", -0.1),
    ])

bone("neck", "torso", (0, 37, 0), [8, 0, 6], [c((-1, 37, -1), (2, 2, 2), "stem")])
bone("head", "neck", (0, 39, 0), [-6, 0, -8], [
    c((-1, 40, -1), (2, 7, 3), "gills"),
    c((-1, 39, 1), (2, 9, 1), "stem"),
    c((-1, 39, -1), (2, 1, 2), "stem"),
    c((2.0, 44, -1.5), (3, 1, 3), "bracket"),
    c((2.0, 46, -1.0), (2, 1, 2), "bracket"),
    c((-4.5, 44, 0.0), (2, 1, 2), "bracket"),
])
bone("head_left", "head", (-1, 39, 0), cubes=[c((-3, 39, -2), (2, 11, 4), "head_half")])
bone("head_right", "head", (1, 39, 0), cubes=[c((1, 39, -2), (2, 8, 4), "head_half")])
for i, (x, z, length) in enumerate(THREADS):
    bone(f"thread_{i}", "head", (x, 39, z), cubes=[c((x - 0.5, 39 - length, z - 0.5), (1, length, 1), "thread", -0.35)])

MODEL.pack()


def loop(length, frames):
    return {round(t, 3): v for t, v in frames} | {length: frames[0][1]}


def threads(length, amp):
    out = {}
    for i in range(len(THREADS)):
        phase = 0.7 * i
        frames = {}
        for k in range(4):
            t = round(length * k / 4, 3)
            frames[t] = [round(amp * math.sin(2 * math.pi * k / 4 + phase), 2), 0, round(amp * 0.6 * math.cos(2 * math.pi * k / 4 + phase), 2)]
        frames[length] = frames[0]
        out[f"thread_{i}"] = {"rotation": frames}
    return out


def idle():
    L = 4.0
    bones = {
        "body": {"rotation": loop(L, [(0, [0, 0, 1.5]), (2, [0, 0, -1.5])])},
        "torso": {"rotation": loop(L, [(0, [0, 0, 0]), (2, [2, 0, 0])])},
        "head": {"rotation": loop(L, [(0, [0, 0, 0]), (1.25, [0, 0, 0]), (1.32, [4, 22, -6]), (2.6, [4, 22, -6]), (2.66, [-2, -8, 3]), (3.4, [0, 0, 0])])},
        "right_arm": {"rotation": loop(L, [(0, [4, 0, 0]), (2, [-3, 0, 0])])},
        "left_arm": {"rotation": loop(L, [(0, [-3, 0, 0]), (2, [4, 0, 0])])},
        "right_hand": {"rotation": loop(L, [(0, [0, 0, 0]), (3.0, [0, 0, 0]), (3.05, [0, 0, 18]), (3.3, [0, 0, 0])])},
        "head_left": {"rotation": loop(L, [(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.08, [0, -9, 0]), (2.5, [0, -9, 0]), (2.7, [0, 0, 0])])},
        "head_right": {"rotation": loop(L, [(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.08, [0, 9, 0]), (2.5, [0, 9, 0]), (2.7, [0, 0, 0])])},
    }
    bones.update(threads(L, 6))
    return anim(L, True, bones)


def walk():
    L = 1.2
    h = L / 2
    bones = {
        "body": {"position": loop(L, [(0, [0, 0, 0]), (L / 4, [0, 1.2, 0]), (h, [0, 0, 0]), (3 * L / 4, [0, 1.2, 0])]),
                 "rotation": loop(L, [(0, [0, 0, 3]), (h, [0, 0, -3])])},
        "right_leg": {"rotation": loop(L, [(0, [-28, 0, 0]), (h, [26, 0, 0])])},
        "right_shin": {"rotation": loop(L, [(0, [6, 0, 0]), (L / 4, [40, 0, 0]), (h, [4, 0, 0]), (3 * L / 4, [10, 0, 0])])},
        "left_leg": {"rotation": loop(L, [(0, [26, 0, 0]), (h, [-28, 0, 0])])},
        "left_shin": {"rotation": loop(L, [(0, [4, 0, 0]), (L / 4, [10, 0, 0]), (h, [6, 0, 0]), (3 * L / 4, [40, 0, 0])])},
        "right_arm": {"rotation": loop(L, [(0, [18, 0, 0]), (h, [-16, 0, 0])])},
        "right_forearm": {"rotation": loop(L, [(0, [-4, 0, 0]), (0.4, [-22, 0, 0]), (h, [-10, 0, 0]), (1.0, [0, 0, 0])])},
        "left_arm": {"rotation": loop(L, [(0, [-16, 0, 0]), (h, [18, 0, 0])])},
        "left_forearm": {"rotation": loop(L, [(0, [-10, 0, 0]), (0.4, [0, 0, 0]), (h, [-4, 0, 0]), (1.0, [-22, 0, 0])])},
        "head": {"rotation": loop(L, [(0, [0, 0, 0]), (0.5, [0, 0, 0]), (0.54, [6, -12, 4]), (0.8, [6, -12, 4]), (0.84, [0, 0, 0])])},
    }
    bones.update(threads(L, 10))
    return anim(L, True, bones)


def attack():
    L = 0.8
    return anim(L, False, {
        "torso": {"rotation": {0: [0, 0, 0], 0.35: [-14, 0, 0], 0.48: [24, 0, 0], 0.8: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 0.35: [-165, 0, 12], 0.48: [-40, 0, 4], 0.8: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 0.35: [-165, 0, -12], 0.48: [-40, 0, -4], 0.8: [0, 0, 0]}},
        "right_forearm": {"rotation": {0: [0, 0, 0], 0.35: [-20, 0, 0], 0.48: [0, 0, 0], 0.8: [0, 0, 0]}},
        "left_forearm": {"rotation": {0: [0, 0, 0], 0.35: [-20, 0, 0], 0.48: [0, 0, 0], 0.8: [0, 0, 0]}},
        "head": {"rotation": {0: [0, 0, 0], 0.35: [-18, 0, 0], 0.48: [20, 0, 0], 0.8: [0, 0, 0]}},
        "head_left": {"rotation": {0: [0, 0, 0], 0.3: [0, -30, 0], 0.5: [0, -30, 0], 0.62: [0, 0, 0]}},
        "head_right": {"rotation": {0: [0, 0, 0], 0.3: [0, 30, 0], 0.5: [0, 30, 0], 0.62: [0, 0, 0]}},
    })


def shrug():
    L = 0.5
    return anim(L, False, {
        "body": {"rotation": {0: [0, 0, 0], 0.05: [-8, 6, 4], 0.15: [5, -4, -3], 0.3: [-2, 2, 1], 0.5: [0, 0, 0]}},
        "head": {"rotation": {0: [0, 0, 0], 0.05: [-14, -20, 0], 0.2: [10, 14, 0], 0.5: [0, 0, 0]}},
        "head_left": {"rotation": {0: [0, 0, 0], 0.05: [0, -16, 0], 0.3: [0, 0, 0]}},
        "head_right": {"rotation": {0: [0, 0, 0], 0.05: [0, 16, 0], 0.3: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 0.08: [-30, 0, 20], 0.5: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 0.08: [-26, 0, -18], 0.5: [0, 0, 0]}},
    })


def emerge():
    L = 1.5
    return anim(L, False, {
        "root": {"position": {0: [0, -50, 0], 0.5: [0, -30, 0], 0.55: [0, -26, 0], 1.1: [0, -4, 0], 1.5: [0, 0, 0]}},
        "head": {"rotation": {0: [50, 0, 0], 1.1: [40, 0, 0], 1.25: [-10, 10, 0], 1.5: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [-170, 0, 10], 0.9: [-150, 0, 10], 1.5: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [-170, 0, -10], 1.0: [-140, 0, -10], 1.5: [0, 0, 0]}},
    })


def sink():
    L = 1.5
    return anim(L, False, {
        "root": {"position": {0: [0, 0, 0], 0.4: [0, -2, 0], 1.5: [0, -50, 0]}},
        "head": {"rotation": {0: [0, 0, 0], 0.4: [40, 0, 0], 1.5: [50, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 0.6: [-150, 0, 10], 1.5: [-170, 0, 10]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 0.7: [-140, 0, -10], 1.5: [-170, 0, -10]}},
    })


def crumble():
    L = 1.2
    return anim(L, False, {
        "root": {"position": {0: [0, 0, 0], 0.3: [0, -3, 0], 1.2: [0, -14, 0]}},
        "body": {"rotation": {0: [0, 0, 0], 0.3: [-6, 0, 4], 1.2: [55, 0, 12]}},
        "right_leg": {"rotation": {0: [0, 0, 0], 0.4: [-50, 0, 0], 1.2: [-80, 0, 0]}},
        "right_shin": {"rotation": {0: [0, 0, 0], 0.4: [70, 0, 0], 1.2: [100, 0, 0]}},
        "left_leg": {"rotation": {0: [0, 0, 0], 0.5: [-40, 0, 0], 1.2: [-75, 0, 0]}},
        "left_shin": {"rotation": {0: [0, 0, 0], 0.5: [60, 0, 0], 1.2: [95, 0, 0]}},
        "head": {"rotation": {0: [0, 0, 0], 0.3: [-20, 0, 0], 1.2: [45, 20, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 1.2: [-40, 0, 30]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 1.2: [-20, 0, -40]}},
    })


def animations():
    return {"format_version": "1.8.0", "animations": {
        "animation.stemwalker.idle": idle(),
        "animation.stemwalker.walk": walk(),
        "animation.stemwalker.attack": attack(),
        "animation.stemwalker.shrug": shrug(),
        "animation.stemwalker.emerge": emerge(),
        "animation.stemwalker.sink": sink(),
        "animation.stemwalker.crumble": crumble(),
    }}


if __name__ == "__main__":
    write(GEO_OUT, geometry())
    write(ANIM_OUT, animations())
