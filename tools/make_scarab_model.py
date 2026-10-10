from geckolib_model import Model, anim, animations, c, loop, write

ASSETS = "src/main/resources/assets/wildspellmobs"
GEO_OUT = f"{ASSETS}/geo/entity/scarab.geo.json"
ANIM_OUT = f"{ASSETS}/animations/entity/scarab.animation.json"
TEX = 64

MODEL = Model("geometry.scarab", TEX, 1, 1, [0, 0.25, 0])
BONES = MODEL.bones
bone = MODEL.bone

TILT = -25
BALL = (0, 3.5, -4)
LEGS = {"fore": (5.5, 2, 3, -65, 10, 30), "mid": (3.0, 2, 3, -30, 0, 60), "hind": (1.5, 3, 4, 90, 60, 120)}

bone("root", None, (0, 0, 0))
bone("ball", "root", BALL, cubes=[
    c((-3.5, 1, -6.5), (7, 5, 5), "ball"),
    c((-2.5, 0, -6.5), (5, 7, 5), "ball"),
    c((-2.5, 1, -7.5), (5, 5, 7), "ball"),
    c((-3, 0.5, -7), (6, 6, 6), "ball"),
])
bone("dance", "root", (0, 1, 3.5))
bone("beetle", "dance", (0, 0.5, 8), [TILT, 0, 0], [
    c((-3, 1, -1.5), (6, 3, 5), "elytra"),
    c((-3, 1.5, 3.5), (6, 2, 3), "pronotum"),
    c((-2.5, 1.5, 6.5), (5, 1, 2), "clypeus"),
    c((-2.5, 0.5, -1), (5, 1, 7), "underside"),
])
for side, s in (("right", -1), ("left", 1)):
    bone(f"{side}_antenna", "beetle", (2 * s, 2, 7), [0, 30 * s, 0], [c((2 * s - 0.5, 1.5, 7), (1, 1, 1), "antenna")])
    for pair, (z, femur, tibia, fan, lift, drop) in LEGS.items():
        name = f"{side}_{pair}_leg"
        x0 = 2.5 * s
        bone(name, "beetle", (x0, 1.5, z), [0, fan * s, 0])
        bone(f"{name}_femur", name, (x0, 1.5, z), [0, 0, -lift * s],
             [c((x0 if s > 0 else x0 - femur, 1, z - 0.5), (femur, 1, 1), "leg")])
        knee = x0 + femur * s
        bone(f"{name}_tibia", f"{name}_femur", (knee, 1.5, z), [0, 0, drop * s],
             [c((knee if s > 0 else knee - tibia, 1, z - 0.5), (tibia, 1, 1), "foretibia" if pair == "fore" else "leg")])

MODEL.pack()

PAIRS = [("left", "fore"), ("right", "mid"), ("left", "hind"), ("right", "fore"), ("left", "mid"), ("right", "hind")]


def gait(length, swing, lift):
    bones = {}
    half = length / 2
    for i, (side, pair) in enumerate(PAIRS):
        s = 1 if side == "left" else -1
        phase = 0.0 if i < 3 else half
        name = f"{side}_{pair}_leg"
        if pair == "hind":
            bones[f"{name}_tibia"] = {"rotation": cycle(length, [(phase, [0, 0, -lift * s]), (phase + half, [0, 0, lift * s])])}
            continue
        bones[name] = {"rotation": cycle(length, [(phase, [0, -swing * s, 0]), (phase + half, [0, swing * s, 0])])}
        bones[f"{name}_femur"] = {"rotation": cycle(length, [(phase, [0, 0, 0]), (phase + half, [0, 0, 0]),
                                                             (phase + 0.75 * length, [0, 0, -lift * s])])}
    return bones


def cycle(length, frames):
    wrapped = sorted((round(t % length, 3), v) for t, v in frames)
    if wrapped[0][0] != 0:
        wrapped.insert(0, (0.0, wrapped[-1][1]))
    return loop(length, wrapped)


def idle():
    L = 4.0
    return anim(L, True, {
        "left_antenna": {"rotation": loop(L, [(0, [0, 0, 0]), (1.0, [0, 0, 0]), (1.1, [-15, 10, 0]), (1.6, [0, 0, 0])])},
        "right_antenna": {"rotation": loop(L, [(0, [0, 0, 0]), (2.6, [0, 0, 0]), (2.7, [-15, -10, 0]), (3.2, [0, 0, 0])])},
        "beetle": {"rotation": loop(L, [(0, [0, 0, 0]), (2.0, [-2, 0, 0])])},
    })


def roll():
    L = 1.0
    bones = gait(L, 16, 14)
    bones["ball"] = {"rotation": {0: [0, 0, 0], 0.25: [90, 0, 0], 0.5: [180, 0, 0], 0.75: [270, 0, 0], 1.0: [360, 0, 0]}}
    bones["beetle"] = {"rotation": loop(L, [(0, [0, 0, 1.5]), (L / 2, [0, 0, -1.5])])}
    return anim(L, True, bones)


def dance():
    L = 2.5
    up = [0, 5.6, -9.4]
    spin = {0: [0, 0, 0], 0.6: [0, 0, 0], 1.0: [0, 90, 0], 1.3: [0, 140, 0], 1.6: [0, 230, 0], 1.9: [0, 300, 0], 2.1: [0, 360, 0], 2.5: [0, 360, 0]}
    bones = {
        "dance": {"position": {0: [0, 0, 0], 0.5: up, 2.1: up, 2.5: [0, 0, 0]}, "rotation": spin},
        "beetle": {"rotation": {0: [0, 0, 0], 0.5: [-TILT, 0, 0], 2.1: [-TILT, 0, 0], 2.5: [0, 0, 0]}},
    }
    for side, s in (("left", 1), ("right", -1)):
        bones[f"{side}_hind_leg_tibia"] = {"rotation": {0: [0, 0, 0], 0.5: [0, 0, 60 * s], 2.1: [0, 0, 60 * s], 2.5: [0, 0, 0]}}
        bones[f"{side}_antenna"] = {"rotation": {0: [0, 0, 0], 0.7: [-25, 0, 0], 1.6: [-30, 15 * s, 0], 2.5: [0, 0, 0]}}
    return anim(L, False, bones)


def dig():
    L = 2.0
    return anim(L, False, {
        "root": {"position": {0: [0, 0, 0], 0.4: [0, -1, 0], 2.0: [0, -9, 0]}},
        "ball": {"rotation": {0: [0, 0, 0], 2.0: [-40, 0, 0]}},
        "beetle": {"rotation": {0: [0, 0, 0], 0.3: [0, 0, 4], 0.6: [0, 0, -4], 0.9: [0, 0, 4], 1.2: [0, 0, -4], 2.0: [0, 0, 0]}},
    })


if __name__ == "__main__":
    write(GEO_OUT, MODEL.geometry())
    write(ANIM_OUT, animations("scarab", {"idle": idle(), "roll": roll(), "dance": dance(), "dig": dig()}))
