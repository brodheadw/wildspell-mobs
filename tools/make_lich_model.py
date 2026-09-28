import math

import numpy as np

from geckolib_model import Model, anim, c, kf, rot_matrix, write

GEO_OUT = "src/main/resources/assets/wildspellmobs/geo/entity/ice_lich.geo.json"
ANIM_OUT = "src/main/resources/assets/wildspellmobs/animations/entity/ice_lich.animation.json"
TEX = 128

MODEL = Model("geometry.ice_lich", TEX, 5, 4.5, [0, 2, 0])
BONES = MODEL.bones
bone = MODEL.bone
geometry = MODEL.geometry


bone("root", None, (0, 0, 0))
bone("body", "root", (0, 20, 0))
bone("hem", "body", (0, 20, 0), cubes=[
    c((-6, 14, -4), (12, 6, 8), "skirt"),
    c((-5, 10, -3.5), (10, 4, 7), "skirt_low"),
])
STRIPS = [
    ("front_0", -5, 3, 8, "front"), ("front_1", -2, 2, 9, "front"), ("front_2", 0, 3, 7, "front"), ("front_3", 3, 2, 8, "front"),
    ("back_0", -5, 2, 7, "back"), ("back_1", -3, 3, 9, "back"), ("back_2", 0, 2, 8, "back"), ("back_3", 2, 3, 7, "back"),
    ("right_0", -3.5, 2, 8, "right"), ("right_1", -1.5, 3, 7, "right"), ("right_2", 1.5, 2, 9, "right"),
    ("left_0", -3.5, 3, 7, "left"), ("left_1", -0.5, 2, 9, "left"), ("left_2", 1.5, 2, 8, "left"),
]
FLARE = {"front": (-9, 0, 0), "back": (9, 0, 0), "right": (0, 0, 9), "left": (0, 0, -9)}
STRIP_TOP = 13
for suffix, start, width, length, side in STRIPS:
    top = STRIP_TOP
    if side in ("front", "back"):
        z = -4 if side == "front" else 3
        origin, size, pivot = (start, top - length, z), (width, length, 1), (start + width / 2, top, z + 0.5)
    else:
        x = -5.5 if side == "right" else 4.5
        origin, size, pivot = (x, top - length, start), (1, length, width), (x + 0.5, top, start + width / 2)
    wobble = (len(suffix) * 7 + width * 3 + length) % 5 - 2
    rx, ry, rz = FLARE[side]
    rot = [rx + (wobble if side in ("front", "back") else 0), 0, rz + (wobble if side in ("right", "left") else 0)]
    bone("hem_" + suffix, "hem", pivot, rot, [c(origin, size, "strip")])

bone("torso", "body", (0, 20, 0), cubes=[
    c((-4, 20, -2.5), (8, 9, 5), "ribs"),
    c((-5, 19, -3), (10, 11, 6), "robe_chest"),
    c((-5.5, 18, -3.5), (11, 2, 7), "belt"),
])

bone("entrails", "torso", (0, 20, -4), cubes=[
    c((-1, 20, -4.5), (2, 3, 2), "gut"),
    c((-2, 16.5, -5.5), (4, 2, 2), "gut"),
    c((-2, 15.5, -5), (1, 1, 1), "icicle"),
    c((2.5, 15, -5.5), (1, 1, 1), "icicle"),
])
bone("entrails_loop_0", "entrails", (-0.5, 19, -5), [8, 0, -26], [c((-4.5, 18.5, -6), (5, 2, 2), "gut")])
bone("entrails_loop_1", "entrails", (1, 18, -5.5), [-10, 0, 22], [c((-0.5, 17, -6.5), (4, 2, 2), "gut"),
                                                                  c((3, 16, -6), (1, 1, 1), "icicle")])
ENTRAILS = [
    (-2.5, 16.5, -5.0, 2, 6, (-8, 0, -6), True),
    (-0.5, 16.5, -5.5, 1, 4, (-12, 0, 4), False),
    (1.5, 16.0, -5.5, 2, 9, (-6, 0, 3), True),
    (3.0, 16.0, -5.0, 1, 5, (-10, 0, 10), False),
]
for i, (x, y, z, w, length, rot, tip) in enumerate(ENTRAILS):
    cubes = [c((x - w / 2, y - length, z - w / 2), (w, length, w), "gut_strand")]
    if tip:
        cubes.append(c((x - 0.5, y - length - 1, z - 0.5), (1, 1, 1), "icicle"))
    bone(f"entrail_{i}", "entrails", (x, y, z), list(rot), cubes)

bone("mantle", "torso", (0, 29, 0), cubes=[c((-8, 27, -4), (16, 4, 8), "mantle", 0.25)])
bone("collar", "mantle", (0, 30, 2.5), [-18, 0, 0], [c((-5, 30, 2), (10, 6, 2), "collar")])
for side, sx in (("right", -1), ("left", 1)):
    x0 = -11 if side == "right" else 5
    bone(f"{side}_pauldron", "mantle", (7 * sx, 30, 0), [0, 0, 16 * sx], [c((x0, 27, -4.5), (6, 4, 9), "pauldron")])
    for i, (dx, z, rot, size) in enumerate([(2.5, -2, (8, 0, 28), (2, 5, 2)), (1, 1.5, (-22, 0, 16), (1, 4, 1)),
                                            (3.5, 2, (-8, 0, 48), (1, 3, 1))]):
        px = 7 * sx + dx * sx
        rot = [rot[0], 0, rot[2] * sx]
        o = (px - size[0] / 2, 30.5, z - size[2] / 2)
        bone(f"{side}_shard_{i}", f"{side}_pauldron", (px, 30.5, z), rot, [c(o, size, "ice")])

bone("neck", "torso", (0, 30, 0), cubes=[c((-1, 29, -1), (2, 4, 2), "bone")])
bone("head", "neck", (0, 32, -0.5), cubes=[c((-3.5, 33, -4), (7, 7, 7), "skull")])
bone("jaw", "head", (0, 33.5, 0), [10, 0, 0], [c((-3, 31.5, -4), (6, 2, 5), "jaw")])
bone("crown", "head", (0, 40, -0.5), cubes=[c((-4, 39, -4.5), (8, 2, 8), "crown_band")])
SPIKES = [
    ("front", 0, -4, (7, 0, 0), 2, 3, 2),
    ("front_right", -2.6, -3.6, (6, 0, -12), 2, 2, 2),
    ("front_left", 2.6, -3.6, (5, 0, 14), 2, 2, 1),
    ("right", -3.6, -0.5, (0, 0, -30), 2, 2, 2),
    ("left", 3.6, 0, (-4, 0, 26), 2, 3, 1),
    ("back_right", -2, 2.6, (-16, 0, -12), 1, 3, 0),
    ("back_left", 2, 2.6, (-12, 0, 10), 1, 2, 0),
]
for name, x, z, rot, bw, bh, th in SPIKES:
    cubes = [c((x - bw / 2, 40.5, z - bw / 2), (bw, bh, bw), "spike")]
    if th:
        cubes.append(c((x - 0.5, 40.5 + bh, z - 0.5), (1, th, 1), "spike_tip"))
    bone("spike_" + name, "crown", (x, 41, z), list(rot), cubes)

for side, sx in (("right", -1), ("left", 1)):
    x_sleeve = -9 if side == "right" else 5
    x_cuff = -9.5 if side == "right" else 4.5
    x_bone = -8 if side == "right" else 6
    rot = [-15, 0, 0] if side == "right" else [-6, 0, -5]
    bone(f"{side}_arm", "torso", (7 * sx, 29, 0), rot, [
        c((x_sleeve, 20, -2), (4, 9, 4), "sleeve"),
        c((x_cuff, 17, -2.5), (5, 4, 5), "cuff"),
        c((x_bone, 13, -1), (2, 5, 2), "bone"),
    ])
    x_palm = -8.5 if side == "right" else 5.5
    bone(f"{side}_hand", f"{side}_arm", (7 * sx, 13, 0), cubes=[c((x_palm, 11, -1.5), (3, 2, 3), "hand")])

for i, x in enumerate((-8.5, -7.5, -6.5)):
    bone(f"right_finger_{i}", "right_hand", (x + 0.5, 11.5, -1.5), [30, 0, 0], [c((x, 9.5, -2.5), (1, 2, 1), "bone")])
for i, (x, rz, length) in enumerate([(5.5, 10, 4), (6.5, 3, 5), (7.5, -4, 5), (8.5, -12, 4)]):
    bone(f"left_finger_{i}", "left_hand", (x + 0.5, 11, 0), [-10, 0, rz], [c((x, 11 - length, -0.5), (1, length, 1), "claw")])

bone("staff", "right_arm", (-7, 12, 0), [15, 0, 0], [
    c((-7.5, 1, -0.5), (1, 37, 1), "shaft", 0.25),
    c((-8, 20, -1), (2, 1, 2), "staff_band"),
    c((-8, 28, -1), (2, 1, 2), "staff_band"),
    c((-8, 36, -1), (2, 2, 2), "staff_cap"),
    c((-9, 38, -0.5), (1, 4, 1), "staff_prong"),
    c((-6, 38, -0.5), (1, 4, 1), "staff_prong"),
    c((-7.5, 0, -0.5), (1, 1, 1), "staff_band"),
])
bone("staff_crystal", "staff", (-7, 40.5, 0), [0, 45, 0], [
    c((-8.5, 38.5, -1.5), (3, 5, 3), "crystal"),
    c((-8, 43.5, -1), (2, 1, 2), "crystal"),
    c((-7.5, 44.5, -0.5), (1, 1, 1), "crystal_tip"),
])


MODEL.pack()


def ch(**frames):
    return frames


def strip_sway(amp, phase, length, flare=0.0):
    bones = {}
    for i, (suffix, _start, _w, _l, side) in enumerate(STRIPS):
        steps = 4
        frames = {}
        for k in range(steps + 1):
            t = length * k / steps
            s = [0, 1, 0, -1][(k + i + phase) % 4] * amp
            out = [0.0, 0.0, 0.0]
            if side == "front":
                out = [-flare + s, 0, s * 0.3]
            elif side == "back":
                out = [flare + s, 0, s * 0.3]
            elif side == "right":
                out = [s, 0, flare + s * 0.5]
            else:
                out = [s, 0, -flare + s * 0.5]
            frames[t] = [round(v, 2) for v in out]
        frames[length] = frames[0]
        bones["hem_" + suffix] = {"rotation": frames}
    return bones


def idle():
    L = 4.0
    bones = {
        "body": {"position": {0: [0, 0, 0], 1: [0, 0.6, 0], 2: [0, 1.2, 0], 3: [0, 0.6, 0], 4: [0, 0, 0]}},
        "hem": {"rotation": {0: [2, 0, 0], 1: [0, 0, 1.5], 2: [-2, 0, 0], 3: [0, 0, -1.5], 4: [2, 0, 0]}},
        "torso": {"rotation": {0: [1, 0, 0], 2: [-1, 0, 0], 4: [1, 0, 0]}},
        "neck": {"rotation": {0: [0, 0, 0], 2.4: [0, 0, 0], 2.9: [4, 0, -7], 3.5: [2, 0, -5], 4: [0, 0, 0]}},
        "jaw": {"rotation": {0: [0, 0, 0], 2.8: [0, 0, 0], 3.1: [6, 0, 0], 3.5: [0, 0, 0], 4: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 2: [-3, 0, 1], 4: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 1.3: [-4, 0, -2], 2.7: [2, 0, 1], 4: [0, 0, 0]}},
        "staff": {"rotation": {0: [0, 0, 0], 2: [2, 0, -1.5], 4: [0, 0, 0]}},
        "left_finger_0": {"rotation": {0: [0, 0, 0], 1.5: [-12, 0, 0], 3: [0, 0, 0], 4: [0, 0, 0]}},
        "left_finger_1": {"rotation": {0: [0, 0, 0], 1.7: [-14, 0, 0], 3.2: [0, 0, 0], 4: [0, 0, 0]}},
        "left_finger_2": {"rotation": {0: [0, 0, 0], 1.9: [-12, 0, 0], 3.4: [0, 0, 0], 4: [0, 0, 0]}},
        "left_finger_3": {"rotation": {0: [0, 0, 0], 2.1: [-10, 0, 0], 3.6: [0, 0, 0], 4: [0, 0, 0]}},
    }
    bones.update(strip_sway(3, 0, L))
    bones.update(entrail_sway(L, 5, 3))
    return anim(L, True, bones)


def cast():
    return anim(0.75, False, {
        "torso": {"rotation": {0: [0, 0, 0], 0.15: [-6, 8, 0], 0.3: [10, -6, 0], 0.5: [6, -3, 0], 0.75: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 0.15: [-10, 0, -8], 0.3: [-55, 0, 0], 0.5: [-50, 0, 0], 0.75: [0, 0, 0]}},
        "staff": {"rotation": {0: [0, 0, 0], 0.15: [-15, 0, 0], 0.3: [100, 0, 0], 0.5: [90, 0, 0], 0.75: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 0.2: [-30, 0, -25], 0.32: [-72, 0, 18], 0.5: [-40, 0, 6], 0.75: [0, 0, 0]}},
        "left_hand": {"rotation": {0: [0, 0, 0], 0.2: [20, 0, 0], 0.32: [-35, 0, 0], 0.5: [-10, 0, 0], 0.75: [0, 0, 0]}},
        "jaw": {"rotation": {0: [0, 0, 0], 0.3: [14, 0, 0], 0.6: [0, 0, 0], 0.75: [0, 0, 0]}},
        "hem": {"rotation": {0: [0, 0, 0], 0.3: [6, 0, 0], 0.55: [-3, 0, 0], 0.75: [0, 0, 0]}},
    })


def summon():
    L = 1.5
    bones = {
        "body": {"position": {0: [0, 1.5, 0], 0.75: [0, 2.5, 0], 1.5: [0, 1.5, 0]}},
        "torso": {"rotation": {0: [-8, 0, 0], 0.75: [-10, 0, 0], 1.5: [-8, 0, 0]}},
        "neck": {"rotation": {0: [-22, 0, 0], 0.75: [-26, 0, 3], 1.5: [-22, 0, 0]}},
        "jaw": {"rotation": {0: [18, 0, 0], 0.75: [24, 0, 0], 1.5: [18, 0, 0]}},
        "right_arm": {"rotation": {0: [-150, 0, -14], 0.75: [-156, 0, -18], 1.5: [-150, 0, -14]}},
        "staff": {"rotation": {0: [150, 0, 0], 0.75: [154, 0, 0], 1.5: [150, 0, 0]},
                  "position": {0: [0, 11.6, 3.1], 1.5: [0, 11.6, 3.1]}},
        "left_arm": {"rotation": {0: [-150, 0, 24], 0.75: [-157, 0, 28], 1.5: [-150, 0, 24]}},
        "left_hand": {"rotation": {0: [-10, 0, 0], 0.75: [-20, 0, 0], 1.5: [-10, 0, 0]}},
        "left_finger_0": {"rotation": {0: [-30, 0, 18], 1.5: [-30, 0, 18]}},
        "left_finger_3": {"rotation": {0: [-30, 0, -18], 1.5: [-30, 0, -18]}},
        "hem": {"rotation": {0: [-3, 0, 0], 0.75: [3, 0, 0], 1.5: [-3, 0, 0]}},
    }
    bones.update(strip_sway(6, 1, L, flare=22))
    bones.update(entrail_sway(L, 14, 9, lift=10))
    return anim(L, True, bones)


def beam():
    L = 1.0
    return anim(L, True, {
        "body": {"position": {0: [0, 0, -1], 0.5: [0, 0.4, -1], 1: [0, 0, -1]}},
        "torso": {"rotation": {0: [10, 0, 0], 0.25: [10.5, 0.8, 0], 0.5: [9.5, 0, 0], 0.75: [10.5, -0.8, 0], 1: [10, 0, 0]}},
        "neck": {"rotation": {0: [-10, 0, 0], 1: [-10, 0, 0]}},
        "jaw": {"rotation": {0: [16, 0, 0], 0.5: [20, 0, 0], 1: [16, 0, 0]}},
        "right_arm": {"rotation": {0: [-35, -20, 0], 0.5: [-36, -20, 0], 1: [-35, -20, 0]}},
        "staff": {"rotation": {0: [110, 20, 0], 0.25: [109, 21.5, 0], 0.5: [111, 20, 0], 0.75: [109, 18.5, 0], 1: [110, 20, 0]},
                  "position": {0: [1.7, 3.4, 4.6], 1: [1.7, 3.4, 4.6]}},
        "left_arm": {"rotation": {0: [-25, 35, 25], 0.5: [-26, 35, 25], 1: [-25, 35, 25]}},
        "left_hand": {"rotation": {0: [-20, 0, 0], 1: [-20, 0, 0]}},
        "hem": {"rotation": {0: [-6, 0, 0], 0.5: [-8, 0, 0], 1: [-6, 0, 0]}},
    })


def burst():
    bones = {
        "body": {"position": {0: [0, 0, 0], 0.3: [0, 1.5, 0], 0.45: [0, -1.5, 0], 0.7: [0, -1, 0], 1: [0, 0, 0]}},
        "torso": {"rotation": {0: [0, 0, 0], 0.3: [-10, -10, 0], 0.45: [20, 8, 0], 0.7: [16, 6, 0], 1: [0, 0, 0]}},
        "neck": {"rotation": {0: [0, 0, 0], 0.3: [-8, 0, 0], 0.45: [6, 0, 0], 1: [0, 0, 0]}},
        "jaw": {"rotation": {0: [0, 0, 0], 0.45: [22, 0, 0], 0.8: [4, 0, 0], 1: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 0.3: [-165, 0, -10], 0.45: [-28, 0, 4], 0.7: [-32, 0, 4], 1: [0, 0, 0]}},
        "left_hand": {"rotation": {0: [0, 0, 0], 0.3: [-10, 0, 0], 0.45: [35, 0, 0], 0.7: [30, 0, 0], 1: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 0.3: [8, 0, -10], 0.45: [-10, 0, -6], 1: [0, 0, 0]}},
        "hem": {"rotation": {0: [0, 0, 0], 0.3: [-6, 0, 0], 0.45: [10, 0, 0], 0.7: [4, 0, 0], 1: [0, 0, 0]}},
    }
    swing = {0.3: [8, 0, 3], 0.45: [-16, 0, -4], 0.55: [-44, 0, 8], 0.7: [-8, 0, -6], 0.82: [-22, 0, 3],
             0.92: [-12, 0, -1]}
    for i in range(len(ENTRAILS)):
        frames = {0: [0, 0, 0], 1: [0, 0, 0]}
        for t, v in swing.items():
            frames[min(0.97, round(t + 0.03 * i, 3))] = [round(x * (1 - 0.12 * i), 2) for x in v]
        bones[f"entrail_{i}"] = {"rotation": frames}
    return anim(1.0, False, bones)


def euler_candidates(M):
    F = np.diag([-1, 1, 1])
    N = F @ M @ F
    b = math.asin(max(-1.0, min(1.0, -N[2, 0])))
    a = math.atan2(N[2, 1], N[2, 2])
    g = math.atan2(N[1, 0], N[0, 0])
    first = np.degrees([-a, -b, g])
    second = np.degrees([-(a + math.pi), -(math.pi - b), g + math.pi])
    return first, second


def euler_near(M, prev):
    best = None
    for e in euler_candidates(M):
        e = e + 360 * np.round((prev - e) / 360)
        if best is None or np.abs(e - prev).sum() < np.abs(best - prev).sum():
            best = e
    return best


BONE_BY_NAME = {b["name"]: b for b in BONES}


def world_of(name, pose):
    b = BONE_BY_NAME[name]
    rot = np.array(b["rotation"] or [0, 0, 0], float) + np.array(pose.get(name, {}).get("rotation", [0, 0, 0]), float)
    pos = np.array(pose.get(name, {}).get("position", [0, 0, 0]), float)
    piv = np.array(b["pivot"], float)
    R = rot_matrix(*rot)
    t = piv + pos - R @ piv
    if b["parent"] is None:
        return R, t
    PR, Pt = world_of(b["parent"], pose)
    return PR @ R, PR @ t + Pt


def hand_hold(side, pose):
    R, t = world_of(f"{side}_arm", pose)
    grip = np.array([-7.0, 12, 0] if side == "right" else [7.0, 12, 0])
    rest = np.array(BONE_BY_NAME[f"{side}_arm"]["rotation"], float)
    return R @ grip + t, R @ rot_matrix(*rest).T


STAFF_PIVOT = np.array(BONE_BY_NAME["staff"]["pivot"], float)
STAFF_REST = np.array(BONE_BY_NAME["staff"]["rotation"], float)
STAFF_MID = 11.0
STAFF_TIP = 33.5


def solve_staff(pose, grip_world, W, prev):
    R, t = world_of("right_arm", pose)
    pos = R.T @ (grip_world - t) - STAFF_PIVOT
    e = euler_near(R.T @ W, prev + STAFF_REST) - STAFF_REST
    return pos, e


def quat(M):
    w = math.sqrt(max(0.0, 1 + M[0, 0] + M[1, 1] + M[2, 2])) / 2
    x = math.copysign(math.sqrt(max(0.0, 1 + M[0, 0] - M[1, 1] - M[2, 2])) / 2, M[2, 1] - M[1, 2])
    y = math.copysign(math.sqrt(max(0.0, 1 - M[0, 0] + M[1, 1] - M[2, 2])) / 2, M[0, 2] - M[2, 0])
    z = math.copysign(math.sqrt(max(0.0, 1 - M[0, 0] - M[1, 1] + M[2, 2])) / 2, M[1, 0] - M[0, 1])
    return np.array([w, x, y, z])


def quat_matrix(q):
    w, x, y, z = q / np.linalg.norm(q)
    return np.array([[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                     [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                     [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]])


def slerp(A, B, k):
    qa, qb = quat(A), quat(B)
    if qa @ qb < 0:
        qb = -qb
    d = min(1.0, float(qa @ qb))
    th = math.acos(d)
    if th < 1e-6:
        return A
    return quat_matrix((math.sin((1 - k) * th) * qa + math.sin(k * th) * qb) / math.sin(th))


def smooth(k):
    k = min(1.0, max(0.0, k))
    return k * k * (3 - 2 * k)


def track(keys, t):
    ts = sorted(keys)
    if t <= ts[0]:
        return np.array(keys[ts[0]], float)
    if t >= ts[-1]:
        return np.array(keys[ts[-1]], float)
    i = max(j for j in range(len(ts) - 1) if ts[j] <= t)
    p = [np.array(keys[ts[max(0, min(len(ts) - 1, j))]], float) for j in (i - 1, i, i + 1, i + 2)]
    u = (t - ts[i]) / (ts[i + 1] - ts[i])
    return 0.5 * (2 * p[1] + (p[2] - p[0]) * u + (2 * p[0] - 5 * p[1] + 4 * p[2] - p[3]) * u * u
                  + (-p[0] + 3 * p[1] - 3 * p[2] + p[3]) * u ** 3)


def sample_times(length, fine=()):
    ts = {round(k * 0.05, 3) for k in range(int(round(length / 0.05)) + 1)}
    for a, b in fine:
        ts |= {round(a + k * 0.025, 3) for k in range(int(round((b - a) / 0.025)) + 1)}
    return sorted(ts)


def dense(length, sparse, staff_at, fine=()):
    out = {name: {ch_: {} for ch_ in chans} for name, chans in sparse.items()}
    out["staff"] = {"position": {}, "rotation": {}}
    prev = np.zeros(3)
    for t in sample_times(length, fine):
        pose = {name: {ch_: track(keys, t) for ch_, keys in chans.items()} for name, chans in sparse.items()}
        grip, W = staff_at(t, pose)
        pos, rot = solve_staff(pose, grip, W, prev)
        prev = rot
        if t in (0, length):
            pos, rot = np.zeros(3), np.zeros(3)
        out["staff"]["position"][t] = [round(float(v), 2) + 0.0 for v in pos]
        out["staff"]["rotation"][t] = [round(float(v), 1) + 0.0 for v in rot]
        for name, chans in sparse.items():
            for ch_ in chans:
                out[name][ch_][t] = [round(float(v), 2) + 0.0 for v in pose[name][ch_]]
    return out


def entrail_sway(length, amp_x, amp_z, cycles=1, lift=0.0, steps=8):
    bones = {}
    for i, (_x, _y, _z, _w, strand, _rot, _tip) in enumerate(ENTRAILS):
        lag = 0.35 + 0.12 * strand + 0.9 * i
        scale = 0.5 + 0.08 * strand
        frames = {}
        for k in range(steps):
            a = 2 * math.pi * cycles * k / steps - lag
            frames[round(length * k / steps, 3)] = [round(-lift + amp_x * scale * math.sin(a), 2),
                                                    0, round(amp_z * scale * math.cos(a + 0.7 * i), 2)]
        frames[length] = frames[0]
        bones[f"entrail_{i}"] = {"rotation": frames}
    return bones


def ballistic(c0, c1, s, apex, bulge):
    arc = 4 * s * (1 - s)
    return c0 + (c1 - c0) * s + np.array([0, apex * arc, -bulge * arc])


def toss():
    L = 1.5
    R_OUT, R_CATCH = 0.24, 0.56
    L_OUT, L_CATCH = 0.9, 1.2
    sparse = {
        "torso": {"rotation": {0: [0, 0, 0], 0.2: [0, 8, 0], 0.4: [-3, 0, 0], 0.6: [0, -10, 0], 0.8: [0, -8, 0],
                               0.95: [0, -4, 0], 1.25: [0, 8, 0], 1.5: [0, 0, 0]}},
        "neck": {"rotation": {0: [0, 0, 0], 0.4: [-14, 0, 0], 0.6: [-4, 10, 0], 0.8: [0, 6, 0], 1.05: [-12, 0, 0],
                              1.25: [-2, -8, 0], 1.5: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 0.14: [-16, 0, 10], R_OUT: [-62, 0, -26], 0.4: [-40, 0, -14],
                                   0.8: [-18, 0, -4], 1.05: [-30, 0, -14], L_CATCH: [-40, 0, -22],
                                   1.3: [-22, 0, -8], 1.5: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 0.3: [-26, 0, 10], R_CATCH: [-42, 0, 24], 0.66: [-30, 0, 14],
                                  0.8: [-26, 0, 8], L_OUT - 0.1: [-18, 0, -8], L_OUT: [-60, 0, 26], 1.05: [-30, 0, 8],
                                  1.25: [-10, 0, 0], 1.5: [0, 0, 0]}},
        "left_hand": {"rotation": {0: [0, 0, 0], 0.45: [-10, 0, 0], R_CATCH: [10, 0, 0], 0.7: [0, 0, 0],
                                   1.1: [0, 0, 0], 1.5: [0, 0, 0]}},
        "body": {"position": {0: [0, 0, 0], 0.3: [0, 1, 0], 0.6: [0, -0.5, 0], 0.9: [0, 0.8, 0], 1.2: [0, -0.5, 0],
                              1.5: [0, 0, 0]}},
    }
    mid = np.array([0, STAFF_MID, 0])
    twirl_l = {0.56: 0, 0.75: -18, 0.84: -6, L_OUT: 20}
    tilt_r = {0: 0, 0.14: -14, R_OUT: 24}

    def held(side, pose, tilt):
        grip, _ = hand_hold(side, pose)
        R, _t = world_of("torso", pose)
        return grip, R @ rot_matrix(0, 0, tilt)

    def flight(t, pose, t0, t1, src, dst, src_tilt, dst_tilt, spin, apex, bulge):
        pose0 = {n: {c_: track(k, t0) for c_, k in ch_.items()} for n, ch_ in sparse.items()}
        pose1 = {n: {c_: track(k, t1) for c_, k in ch_.items()} for n, ch_ in sparse.items()}
        g0, W0 = held(src, pose0, src_tilt)
        g1, W1 = held(dst, pose1, dst_tilt)
        c0, c1 = g0 + W0 @ mid, g1 + W1 @ mid
        s = (t - t0) / (t1 - t0)
        W = rot_matrix(0, 0, spin * s) @ slerp(W0, W1, s)
        return ballistic(c0, c1, s, apex, bulge) - W @ mid, W

    def staff_at(t, pose):
        if t <= R_OUT:
            return held("right", pose, float(np.interp(t, list(tilt_r), list(tilt_r.values()))))
        if t < R_CATCH:
            return flight(t, pose, R_OUT, R_CATCH, "right", "left", tilt_r[R_OUT], twirl_l[R_CATCH], 360, 13, 6)
        if t <= L_OUT:
            return held("left", pose, float(np.interp(t, list(twirl_l), list(twirl_l.values()))))
        if t < L_CATCH:
            return flight(t, pose, L_OUT, L_CATCH, "left", "right", twirl_l[L_OUT], -10, -360, 10, 6)
        return held("right", pose, -10 * (1 - smooth((t - L_CATCH) / (L - L_CATCH))))

    bones = dense(L, sparse, staff_at, fine=((R_OUT, R_CATCH), (L_OUT, L_CATCH)))
    return anim(L, False, bones)


def spin():
    L = 1.5
    UP, SPIN_END, THRUST = 0.3, 1.05, 1.25
    sparse = {
        "body": {"position": {0: [0, 0, 0], UP: [0, 1.5, 0], 0.7: [0, 2.2, 0], SPIN_END: [0, 1.5, 0],
                              THRUST: [0, -0.5, -2.5], 1.38: [0, -0.3, -1.5], L: [0, 0, 0]}},
        "torso": {"rotation": {0: [0, 0, 0], UP: [-6, 0, 0], SPIN_END: [-8, 0, 0], 1.15: [-10, 10, 0],
                               THRUST: [14, -6, 0], 1.38: [8, -3, 0], L: [0, 0, 0]}},
        "neck": {"rotation": {0: [0, 0, 0], UP: [-16, 0, 0], SPIN_END: [-18, 0, 0], THRUST: [-10, 0, 0], L: [0, 0, 0]}},
        "jaw": {"rotation": {0: [0, 0, 0], UP: [10, 0, 0], 0.9: [16, 0, 0], THRUST: [22, 0, 0], 1.4: [4, 0, 0],
                             L: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], UP: [-160, 0, -22], 0.6: [-164, 0, -24], 0.85: [-160, 0, -22],
                                   SPIN_END: [-162, 0, -22], 1.15: [-128, 0, -12], THRUST: [-86, 6, -6],
                                   1.38: [-60, 0, -4], L: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [0, 0, 0], UP: [-40, 0, -30], SPIN_END: [-44, 0, -32], 1.15: [-30, 0, -20],
                                  THRUST: [-10, 0, -40], 1.38: [-8, 0, -20], L: [0, 0, 0]}},
        "hem": {"rotation": {0: [0, 0, 0], UP: [-3, 0, 0], SPIN_END: [-3, 0, 0], THRUST: [8, 0, 0], 1.38: [3, 0, 0],
                             L: [0, 0, 0]}},
    }
    flat = rot_matrix(0, 0, -90)
    FORWARD_YAW = 90.0
    for yaw in (90.0, -90.0):
        if (rot_matrix(0, yaw, 0) @ flat @ np.array([0, 1, 0]))[2] < -0.99:
            FORWARD_YAW = yaw
    start_yaw = FORWARD_YAW + 990
    thrust_W = rot_matrix(0, FORWARD_YAW, 0) @ flat

    def staff_at(t, pose):
        hand, W_rest = hand_hold("right", pose)
        if t <= UP:
            k = smooth(t / UP)
            W = slerp(W_rest, rot_matrix(0, start_yaw, 0) @ flat, k)
            return hand - W @ np.array([0, STAFF_MID * k, 0]), W
        if t <= SPIN_END:
            k = (t - UP) / (SPIN_END - UP)
            yaw = start_yaw + (FORWARD_YAW - start_yaw) * (k * (2 - k) * 0.35 + k * 0.65)
            W = rot_matrix(0, yaw, 0) @ flat
            return hand - W @ np.array([0, STAFF_MID, 0]), W
        if t <= THRUST:
            k = smooth((t - SPIN_END) / (THRUST - SPIN_END))
            W = thrust_W
            return hand - W @ np.array([0, STAFF_MID * (1 - k), 0]), W
        k = smooth((t - THRUST) / (L - THRUST))
        return hand, slerp(thrust_W, W_rest, k)

    bones = dense(L, sparse, staff_at, fine=((UP, SPIN_END),))
    swing = {0: [0, 0, 0], UP: [-10, 0, 4], SPIN_END: [-8, 0, -4], 1.18: [8, 0, 0], THRUST: [-28, 0, 6],
             1.36: [6, 0, -3], L: [0, 0, 0]}
    for i in range(len(ENTRAILS)):
        bones[f"entrail_{i}"] = {"rotation": {min(L, round(t + 0.03 * i, 3)) if 0 < t < L else t:
                                              [round(v * (1 - 0.1 * i), 2) for v in val] for t, val in swing.items()}}
    return anim(L, False, bones)


def animations():
    return {"format_version": "1.8.0", "animations": {
        "animation.ice_lich.idle": idle(),
        "animation.ice_lich.cast": cast(),
        "animation.ice_lich.summon": summon(),
        "animation.ice_lich.beam": beam(),
        "animation.ice_lich.burst": burst(),
        "animation.ice_lich.toss": toss(),
        "animation.ice_lich.spin": spin(),
    }}


if __name__ == "__main__":
    write(GEO_OUT, geometry())
    write(ANIM_OUT, animations())
    print("ice lich model and animations written")
