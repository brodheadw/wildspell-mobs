from geckolib_model import Model, anim, animations, c, loop, write

ASSETS = "src/main/resources/assets/wildspellmobs"
GEO_OUT = f"{ASSETS}/geo/entity/scorpion.geo.json"
ANIM_OUT = f"{ASSETS}/animations/entity/scorpion.animation.json"
TEX = 64

MODEL = Model("geometry.scorpion", TEX, 2, 1, [0, 0.25, 0])
BONES = MODEL.bones
bone = MODEL.bone

LEGS = [(-5.5, 3, 3, 2, 50), (-4.3, 3, 3, 2, 20), (-3.1, 3, 4, 2, -15), (-1.9, 4, 4, 3, -45)]
TAIL = [(2, 62), (2, 34), (2, 32), (2, 32), (3, 30)]
BURIED_DEPTH = -4.2

bone("root", None, (0, 0, 0))
bone("body", "root", (0, 3, 0), cubes=[
    c((-2.5, 2, -6), (5, 2, 5), "carapace"),
    c((-2, 2, -7), (4, 2, 1), "carapace"),
    c((-1, 4, -5), (2, 1, 2), "tubercle"),
    c((-1.5, 2.5, -8), (1, 1, 1), "chelicera"),
    c((0.5, 2.5, -8), (1, 1, 1), "chelicera"),
    c((-3, 2, -1), (6, 2, 6), "tergite"),
    c((-2, 2, 5), (4, 2, 1), "tergite"),
])

for side, s in (("right", -1), ("left", 1)):
    for i, (z, femur, tibia, tarsus, fan) in enumerate(LEGS):
        name = f"{side}_leg_{i}"
        x0 = 2.5 * s
        bone(name, "body", (x0, 2.5, z), [0, fan * s, 0])
        reach = x0 + femur * s
        bone(f"{name}_femur", name, (x0, 2.5, z), [0, 0, -38 * s],
             [c((x0 if s > 0 else x0 - femur, 2, z - 0.5), (femur, 1, 1), "leg")])
        bone(f"{name}_tibia", f"{name}_femur", (reach, 2.5, z), [0, 0, 100 * s],
             [c((reach if s > 0 else reach - tibia, 2, z - 0.5), (tibia, 1, 1), "leg")])
        foot = reach + tibia * s
        bone(f"{name}_tarsus", f"{name}_tibia", (foot, 2.5, z), [0, 0, -30 * s],
             [c((foot if s > 0 else foot - tarsus, 2, z - 0.5), (tarsus, 1, 1), "tarsus", -0.1)])

    x0 = 2 * s
    bone(f"{side}_palp", "body", (x0, 3, -6.5), [0, 40 * s, -12 * s],
         [c((x0 if s > 0 else x0 - 4, 2.5, -7), (4, 1, 1), "palp")])
    knee = x0 + 4 * s
    bone(f"{side}_patella", f"{side}_palp", (knee, 3, -6.5), [0, -15 * s, 0],
         [c((knee - 0.5, 2.5, -10.5), (1, 1, 4), "palp")])
    bone(f"{side}_chela", f"{side}_patella", (knee, 3, -10.5), [0, -30 * s, 0], [
        c((knee - 1, 2, -13.5), (2, 2, 3), "chela"),
        c((knee + (0 if s > 0 else -1), 2.5, -16.5), (1, 1, 3), "finger"),
    ])
    hinge = knee + (-1 if s > 0 else 1)
    bone(f"{side}_finger", f"{side}_chela", (hinge, 3, -13.5), [0, 0, 0],
         [c((hinge - (0 if s > 0 else 1), 2.5, -16), (1, 1, 3), "finger", -0.05)])

parent, z = "body", 6
for i, (length, bend) in enumerate(TAIL):
    bone(f"tail_{i}", parent, (0, 3, z), [bend, 0, 0], [c((-1, 2, z), (2, 2, length), "tail")])
    parent, z = f"tail_{i}", z + length
bone("telson", parent, (0, 3, z), [30, 0, 0], [c((-1, 2, z), (2, 2, 2), "vesicle")])
bone("aculeus", "telson", (0, 3, z + 2), [35, 0, 0], [c((-0.5, 2.5, z + 2), (1, 1, 2), "aculeus")])

MODEL.pack()

LEFT = [f"left_leg_{i}" for i in range(4)]
RIGHT = [f"right_leg_{i}" for i in range(4)]
SET_A = [LEFT[0], RIGHT[1], LEFT[2], RIGHT[3]]
SET_B = [RIGHT[0], LEFT[1], RIGHT[2], LEFT[3]]


def stride(length, swing, lift):
    bones = {}
    half = length / 2
    for group, phase in ((SET_A, 0.0), (SET_B, half)):
        for name in group:
            s = 1 if name.startswith("left") else -1
            fan = [(phase, [0, swing * s, 0]), (phase + half, [0, -swing * s, 0])]
            up = [(phase, [0, 0, 0]), (phase + half, [0, 0, 0]), (phase + 0.75 * length, [0, 0, -lift * s])]
            bones[name] = {"rotation": cycle(length, fan)}
            bones[f"{name}_femur"] = {"rotation": cycle(length, up)}
    return bones


def cycle(length, frames):
    wrapped = sorted((round(t % length, 3), v) for t, v in frames)
    if wrapped[0][0] != 0:
        wrapped.insert(0, (0.0, wrapped[-1][1]))
    return loop(length, wrapped)


def idle():
    L = 6.0
    return anim(L, True, {
        "tail_4": {"rotation": loop(L, [(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.15, [3, 2, 0]), (2.6, [0, 0, 0])])},
        "telson": {"rotation": loop(L, [(0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.15, [5, 0, 0]), (2.6, [0, 0, 0])])},
        "left_chela": {"rotation": loop(L, [(0, [0, 0, 0]), (4.0, [0, 0, 0]), (4.2, [0, 6, 0]), (5.0, [0, 0, 0])])},
        "left_finger": {"rotation": loop(L, [(0, [0, 0, 0]), (4.0, [0, 0, 0]), (4.15, [0, 22, 0]), (4.5, [0, 0, 0])])},
        "right_finger": {"rotation": loop(L, [(0, [0, 0, 0]), (4.3, [0, 0, 0]), (4.45, [0, -22, 0]), (4.8, [0, 0, 0])])},
    })


def walk():
    L = 0.5
    bones = stride(L, 14, 18)
    bones["body"] = {"rotation": loop(L, [(0, [0, 1.5, 0]), (L / 2, [0, -1.5, 0])])}
    bones["tail_0"] = {"rotation": loop(L, [(0, [0, 2, 0]), (L / 2, [0, -2, 0])])}
    return anim(L, True, bones)


def sting():
    L = 0.5
    curl = {0: [0, 0, 0], 0.1: [6, 0, 0], 0.2: [-15, 0, 0], 0.5: [0, 0, 0]}
    bones = {f"tail_{i}": {"rotation": curl} for i in range(1, 5)}
    bones["tail_0"] = {"rotation": {0: [0, 0, 0], 0.1: [-8, 0, 0], 0.2: [40, 0, 0], 0.5: [0, 0, 0]}}
    bones["telson"] = {"rotation": {0: [0, 0, 0], 0.1: [-10, 0, 0], 0.2: [10, 0, 0], 0.5: [0, 0, 0]}}
    for side, s in (("left", 1), ("right", -1)):
        bones[f"{side}_palp"] = {"rotation": {0: [0, 0, 0], 0.1: [0, 8 * s, 0], 0.22: [0, -10 * s, 0], 0.5: [0, 0, 0]}}
        bones[f"{side}_finger"] = {"rotation": {0: [0, 0, 0], 0.08: [0, 28 * s, 0], 0.2: [0, 0, 0], 0.5: [0, 0, 0]}}
    bones["body"] = {"rotation": {0: [0, 0, 0], 0.2: [6, 0, 0], 0.5: [0, 0, 0]},
                     "position": {0: [0, 0, 0], 0.12: [0, 0, 0.6], 0.2: [0, 0, -0.8], 0.5: [0, 0, 0]}}
    return anim(L, False, bones)


def buried_pose():
    bones = {"root": {"position": [0, BURIED_DEPTH, 0]}}
    for i in range(5):
        bones[f"tail_{i}"] = {"rotation": [-60, 18, 0] if i == 0 else [-31, 30, 0]}
    bones["telson"] = {"rotation": [-22, 24, 0]}
    for side, s in (("left", 1), ("right", -1)):
        bones[f"{side}_palp"] = {"rotation": [0, 6 * s, -10 * s]}
        bones[f"{side}_chela"] = {"rotation": [-8, 0, 0]}
    for name in LEFT + RIGHT:
        s = 1 if name.startswith("left") else -1
        bones[f"{name}_femur"] = {"rotation": [0, 0, 20 * s]}
    return bones


def blend(pose, times):
    out = {}
    for name, chans in pose.items():
        out[name] = {}
        for k, v in chans.items():
            out[name][k] = {t: [round(x * w, 3) for x in v] for t, w in times}
    return out


def buried():
    return anim(1.0, True, blend(buried_pose(), [(0, 1.0), (1.0, 1.0)]))


def dig():
    L = 1.0
    bones = blend(buried_pose(), [(0, 0.0), (0.25, 0.3), (0.6, 0.75), (1.0, 1.0)])
    bones["body"] = {"rotation": {0: [0, 0, 0], 0.15: [0, 0, 5], 0.3: [0, 0, -5], 0.45: [0, 0, 5], 0.6: [0, 0, -4], 0.8: [0, 0, 3], 1.0: [0, 0, 0]}}
    return anim(L, False, bones)


def emerge():
    L = 0.4
    bones = blend(buried_pose(), [(0, 1.0), (0.25, 0.35), (0.4, 0.0)])
    bones["body"] = {"rotation": {0: [0, 0, 0], 0.15: [-6, 0, 0], 0.4: [0, 0, 0]}}
    return anim(L, False, bones)


if __name__ == "__main__":
    write(GEO_OUT, MODEL.geometry())
    write(ANIM_OUT, animations("scorpion", {"idle": idle(), "walk": walk(), "sting": sting(), "dig": dig(),
                                            "buried": buried(), "emerge": emerge()}))
