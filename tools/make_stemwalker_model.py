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

THREADS = [(-4.5, -5.5, 5), (-1.5, -6.5, 7), (2.5, -6.0, 4), (5.5, -2.0, 6), (-6.0, 1.5, 5), (3.0, 5.5, 4), (-2.0, 5.5, 6)]

bone("root", None, (0, 0, 0))
bone("body", "root", (0, 25, 0))
bone("hips", "body", (0, 25, 0), cubes=[c((-3, 23, -1.5), (6, 3, 3), "trunk")])
for side, sx in (("right", -1), ("left", 1)):
    x0 = -3.5 if side == "right" else 1.5
    bone(f"{side}_leg", "hips", (2.5 * sx, 24, 0), cubes=[c((x0, 12, -1), (2, 12, 2), "stem")])
    bone(f"{side}_shin", f"{side}_leg", (2.5 * sx, 12, 0), cubes=[c((x0, 1, -1), (2, 11, 2), "stem", -0.1)])
    bone(f"{side}_foot", f"{side}_shin", (2.5 * sx, 1, 0), cubes=[
        c((x0 - 0.5, 0, -3), (3, 1, 4), "root"),
        c((x0 + 0.5 * sx, 0, 1), (1, 1, 2), "root", -0.1),
    ])

bone("torso", "body", (0, 26, 0), [6, 0, 0], [
    c((-2.5, 26, -1.5), (5, 11, 3), "trunk"),
    c((2.0, 28, -0.5), (1, 9, 1), "stem_thin", 0.1),
    c((-3.5, 30, 0.0), (1, 6, 1), "stem_thin", 0.1),
])
for side, sx in (("right", -1), ("left", 1)):
    x0 = -5 if side == "right" else 3
    bone(f"{side}_arm", "torso", (4 * sx, 36, 0), [0, 0, 5 * sx], [c((x0, 25, -1), (2, 11, 2), "stem")])
    bone(f"{side}_forearm", f"{side}_arm", (4 * sx, 25, 0), cubes=[c((x0, 13, -1), (2, 12, 2), "stem", -0.15)])
    bone(f"{side}_hand", f"{side}_forearm", (4 * sx, 13, 0), cubes=[
        c((x0 - 0.5, 8, -1.5), (1, 5, 1), "finger", -0.1),
        c((x0 + 0.5, 7, -0.5), (1, 6, 1), "finger", -0.1),
        c((x0 + 1.5 if sx < 0 else x0 - 0.5, 9, 0.5), (1, 4, 1), "finger", -0.1),
    ])

bone("neck", "torso", (0, 37, 0), [14, 0, 0], [c((-1, 37, -1), (2, 3, 2), "stem")])
bone("head", "neck", (0, 40, 0), [12, 0, 0], [
    c((-1.5, 38, -1.5), (3, 4, 3), "stem"),
    c((-5, 42, -5), (10, 3, 10), "cap"),
    c((-4, 45, -4), (8, 2, 8), "cap"),
    c((-2.5, 47, -2.5), (5, 1, 5), "cap"),
    c((-5, 39, -6), (10, 4, 1), "cap_brim"),
    c((-6, 40, -5), (1, 3, 10), "cap_brim"),
    c((5, 40, -5), (1, 3, 10), "cap_brim"),
    c((-5, 41, 5), (10, 2, 1), "cap_brim"),
    c((-4.5, 41.5, -4.5), (9, 1, 9), "gills"),
    c((-3.5, 38.5, -4.9), (7, 3, 1), "gill_face"),
])
for i, (x, z, length) in enumerate(THREADS):
    top = 40 if z < -4 else 41
    bone(f"thread_{i}", "head", (x, top, z), cubes=[c((x - 0.5, top - length, z - 0.5), (1, length, 1), "thread", -0.35)])

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
    })


def shrug():
    L = 0.5
    return anim(L, False, {
        "body": {"rotation": {0: [0, 0, 0], 0.05: [-8, 6, 4], 0.15: [5, -4, -3], 0.3: [-2, 2, 1], 0.5: [0, 0, 0]}},
        "head": {"rotation": {0: [0, 0, 0], 0.05: [-14, -20, 0], 0.2: [10, 14, 0], 0.5: [0, 0, 0]}},
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
