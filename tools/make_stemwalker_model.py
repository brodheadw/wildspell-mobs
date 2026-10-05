import math

import numpy as np

from geckolib_model import Model, anim, c, rot_matrix, write

ASSETS = "src/main/resources/assets/wildspellmobs"
GEO_OUT = f"{ASSETS}/geo/entity/stemwalker.geo.json"
ANIM_OUT = f"{ASSETS}/animations/entity/stemwalker.animation.json"
TEX = 256

SEGMENTS = 12
HEIGHT = SEGMENTS * 16

MODEL = Model("geometry.stemwalker", TEX, 16, 16, [0, 6, 0])
BONES = MODEL.bones
bone = MODEL.bone
geometry = MODEL.geometry

PADS = [
    (2, 1, 0, -30),
    (4, 0, 1, 25),
    (5, -1, -1, -10),
    (7, -1, 0, -48),
    (7, 1, -1, 42),
    (8, 1, 1, 12),
    (9, 0, -1, -22),
    (10, 1, 0, 34),
]
WET_PAD = 0

bone("root", None, (0, 0, 0))
bone("shudder", "root", (0, 0, 0))
for k in range(SEGMENTS):
    parent = "shudder" if k == 0 else f"seg{k - 1}"
    y = 16 * k
    bone(f"seg{k}", parent, (0, y, 0))
    bone(f"seg{k}_skin", f"seg{k}", (0, y, 0), cubes=[c((-8, y, -8), (16, 16, 16), f"stem{k}")])
    if k:
        bone(f"seg{k}_joint", f"seg{k}", (0, y, 0), cubes=[c((-7.5, y - 2, -7.5), (15, 4, 15), "joint", 0.3)])
bone("crown", f"seg{SEGMENTS - 1}", (0, HEIGHT, 8))
bone("crown_skin", "crown", (0, HEIGHT, 0), cubes=[c((-8, HEIGHT, -8), (16, 16, 16), "crown")])

for i, (k, dx, dz, _) in enumerate(PADS):
    y = 16 * k
    bone(f"pad{i}", f"seg{k}", (0, y + 8, 0))
    inner = (8 * dx, y + 8, 8 * dz)
    bone(f"pad{i}_body", f"pad{i}", inner, cubes=[c((16 * dx - 8, y, 16 * dz - 8), (16, 16, 16), f"pad{i}")])

MODEL.pack()


def bend_sign():
    return 1.0 if (rot_matrix(10, 0, 0) @ np.array([0, 1, 0]))[2] < 0 else -1.0


def yaw_to_front(dx, dz, extra):
    rest = math.degrees(math.atan2(dx, -dz))
    best, err = 0.0, 1e9
    for step in range(-3600, 3601):
        a = step / 10.0
        v = rot_matrix(0, a, 0) @ np.array([dx, 0.0, dz], float)
        ang = math.degrees(math.atan2(v[0], -v[2]))
        e = abs((ang - extra + 180) % 360 - 180)
        if e < err - 1e-9 or (abs(e - err) < 1e-9 and abs(a) < abs(best)):
            best, err = a, e
    return round(best, 1), rest


SIGN = bend_sign()


def profile(total, p):
    w = [(k + 1) ** p for k in range(SEGMENTS)]
    s = sum(w)
    return [SIGN * total * v / s for v in w]


def reach(total, p):
    x = y = phi = 0.0
    for a in profile(total, p):
        phi += abs(a)
        x += math.sin(math.radians(phi))
        y += math.cos(math.radians(phi))
    return x + 0.5 * math.sin(math.radians(phi)), y + 0.5 * math.cos(math.radians(phi))


LEAN = profile(85, 2.2)
SLAM = profile(190, 0.5)
SWIVEL = [yaw_to_front(dx, dz, extra)[0] for (_, dx, dz, extra) in PADS]


def segs(angles):
    return {f"seg{k}": [round(a, 2), 0, 0] for k, a in enumerate(angles)}


def pads_awake():
    return {f"pad{i}": [0, SWIVEL[i], 0] for i in range(len(PADS))}


def pads_rest():
    return {f"pad{i}": [0, 0, 0] for i in range(len(PADS))}


def still(rotations, length):
    return {name: {"rotation": {0: v, length: v}} for name, v in rotations.items()}


def breathe():
    L = 6.0
    bones = {}
    for i, (_, dx, dz, _) in enumerate(PADS):
        amp = 0.045 if i == WET_PAD else 0.02
        phase = (i * 0.37) % 1.0
        frames = {}
        for n in range(7):
            t = round(L * n / 6, 3)
            s = 1 + amp * 0.5 * (1 - math.cos(2 * math.pi * (n / 6 - phase)))
            frames[t] = [round(s if dx else 1 + (s - 1) * 0.4, 4), round(1 + (s - 1) * 0.3, 4), round(s if dz else 1 + (s - 1) * 0.4, 4)]
        bones[f"pad{i}_body"] = {"scale": frames}
    return anim(L, True, bones)


def idle():
    L = 4.0
    return anim(L, True, still(pads_rest() | segs([0] * SEGMENTS), L) | {"root": {"position": {0: [0, 0, 0], L: [0, 0, 0]}}})


def wake():
    L = 1.5
    bones = {}
    for i, (k, _, _, _) in enumerate(PADS):
        start = round(0.08 * k, 3)
        bones[f"pad{i}"] = {"rotation": {0: [0, 0, 0], start: [0, 0, 0], round(start + 0.55, 3): [0, SWIVEL[i] * 1.06, 0],
                                         L: [0, SWIVEL[i], 0]}}
    return anim(L, False, bones)


def awake():
    L = 4.0
    bones = still(pads_awake() | segs([0] * SEGMENTS), L)
    bones["seg8"] = {"rotation": {0: [0, 0, 0], 2.0: [SIGN * 0.8, 0, 0], L: [0, 0, 0]}}
    return anim(L, True, bones)


def settle():
    L = 1.5
    bones = {}
    for i, (k, _, _, _) in enumerate(PADS):
        start = round(0.08 * (SEGMENTS - k), 3)
        bones[f"pad{i}"] = {"rotation": {0: [0, SWIVEL[i], 0], start: [0, SWIVEL[i], 0], round(min(L, start + 0.6), 3): [0, 0, 0], L: [0, 0, 0]}}
    return anim(L, False, bones)


def sink():
    L = 1.5
    bones = still(pads_awake(), L)
    bones["root"] = {"position": {0: [0, 0, 0], 0.2: [0, 1.5, 0], 0.5: [0, -40, 0], L: [0, -HEIGHT - 20, 0]}}
    bones["seg0"] = {"rotation": {0: [0, 0, 0], 0.3: [0, 0, 2], 0.6: [0, 0, -2], 0.9: [0, 0, 1.5], L: [0, 0, 0]}}
    return anim(L, False, bones)


def rise():
    L = 1.5
    bones = still(pads_awake(), L)
    bones["root"] = {"position": {0: [0, -HEIGHT - 20, 0], 1.0: [0, -12, 0], 1.3: [0, 1, 0], L: [0, 0, 0]}}
    bones["seg0"] = {"rotation": {0: [0, 0, -2], 0.5: [0, 0, 2], 1.0: [0, 0, -1], L: [0, 0, 0]}}
    return anim(L, False, bones)


def lean():
    L = 1.0
    bones = {f"seg{k}": {"rotation": {0: [0, 0, 0], 0.25: [round(-0.08 * a, 2), 0, 0], 0.8: [round(a * 1.04, 2), 0, 0], L: [round(a, 2), 0, 0]}}
             for k, a in enumerate(LEAN)}
    bones["crown"] = {"rotation": {0: [0, 0, 0], 0.5: [0, 0, 0], L: [-SIGN * 34, 0, 0]}}
    bones.update(still(pads_awake(), L))
    return anim(L, False, bones)


def slam():
    L = 0.25
    bones = {f"seg{k}": {"rotation": {0: [round(LEAN[k], 2), 0, 0], L: [round(SLAM[k], 2), 0, 0]}} for k in range(SEGMENTS)}
    bones["crown"] = {"rotation": {0: [-SIGN * 34, 0, 0], 0.15: [-SIGN * 10, 0, 0], L: [0, 0, 0]}}
    bones.update(still(pads_awake(), L))
    return anim(L, False, bones)


def recover():
    L = 1.0
    bones = {f"seg{k}": {"rotation": {0: [round(SLAM[k], 2), 0, 0], 0.15: [round(SLAM[k] * 0.98, 2), 0, 0],
                                      0.7: [round(SLAM[k] * 0.12 - LEAN[k] * 0.05, 2), 0, 0], L: [0, 0, 0]}} for k in range(SEGMENTS)}
    bones.update(still(pads_awake(), L))
    return anim(L, False, bones)


def hurt():
    L = 0.4
    return anim(L, False, {"shudder": {"rotation": {0: [0, 0, 0], 0.05: [SIGN * 1.6, 0, 0.8], 0.15: [-SIGN * 1.1, 0, -0.9],
                                                     0.25: [SIGN * 0.5, 0, 0.4], L: [0, 0, 0]}}})


def die():
    L = 1.0
    return anim(L, False, {"root": {"rotation": {0: [0, 0, 0], 0.3: [0, 0, 6], 0.75: [0, 0, 55], L: [0, 0, 88]}}})


def animations():
    return {"format_version": "1.8.0", "animations": {
        "animation.stemwalker.breathe": breathe(),
        "animation.stemwalker.idle": idle(),
        "animation.stemwalker.wake": wake(),
        "animation.stemwalker.awake": awake(),
        "animation.stemwalker.settle": settle(),
        "animation.stemwalker.sink": sink(),
        "animation.stemwalker.rise": rise(),
        "animation.stemwalker.lean": lean(),
        "animation.stemwalker.slam": slam(),
        "animation.stemwalker.recover": recover(),
        "animation.stemwalker.hurt": hurt(),
        "animation.stemwalker.die": die(),
    }}


if __name__ == "__main__":
    write(GEO_OUT, geometry())
    write(ANIM_OUT, animations())
    print("stemwalker model and animations written; slam reach %.2f blocks, crown at %.2f" % reach(190, 0.5))
