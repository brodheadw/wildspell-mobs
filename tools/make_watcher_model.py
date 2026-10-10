from geckolib_model import Model, anim, animations, c, loop, texel_point, write

ASSETS = "src/main/resources/assets/wildspellmobs"
TEX = 128
FACES = ("athoth", "eloaiou", "astaphaios", "yao", "sabaoth", "adonin", "sabbataios")
SIDES = (("right", -1), ("left", 1))
HEAD_SCALE = 1.5

WINGS = {
    "hood": {"pivot": (2.5, 34, 1), "cubes": [((2.5, 24, 1), (1, 18, 6))], "inner": "plate",
             "eyes": [(2, 3, 2), (3, 8, 3), (1, 14, 1), (4, 14, 2)]},
    "hood_tip": {"parent": "hood", "pivot": (2.5, 34, 7), "cubes": [((2.5, 26, 7), (1, 15, 4))], "inner": "plate",
                 "eyes": [(1, 4, 1), (2, 9, 2)]},
    "foot": {"pivot": (2.5, 19, 0), "cubes": [((2.5, 3, 0), (1, 16, 6))], "inner": "plate",
             "eyes": [(3, 4, 2), (2, 10, 3), (4, 13, 1)]},
    "arm": {"pivot": (2, 31, 2), "cubes": [((2, 25, 2), (9, 6, 1))], "inner": "front",
            "eyes": [(3, 2, 2), (7, 3, 1)]},
    "hand": {"parent": "arm", "pivot": (11, 31, 2), "cubes": [((11, 19, 2), (11, 12, 1))], "inner": "front",
             "eyes": [(3, 3, 3), (7, 5, 1), (5, 8, 2)]},
}


def mirrored(x, size, sx):
    return x if sx > 0 else -x - size


def head(m, face):
    base = (0, 34, -0.5)

    def grow(p):
        return tuple(base[i] + (p[i] - base[i]) * HEAD_SCALE for i in range(3))

    def h(name, cubes, pivot=base, rotation=None, parent="head"):
        scaled = []
        for o, s, mat, inf in cubes:
            centre = grow(tuple(o[i] + s[i] / 2 for i in range(3)))
            size = tuple(max(1, int(v * HEAD_SCALE + 0.5)) for v in s)
            scaled.append(c(tuple(centre[i] - size[i] / 2 for i in range(3)), size, mat, inf))
        m.bone(name, parent, grow(pivot), rotation, scaled)

    if face == "athoth":
        h("head", [c((-2, 34, -6), (4, 5, 6), "face"), c((-1, 34, -9), (2, 3, 3), "face"), c((-3, 37, -5), (6, 3, 6), "wool", 0.3)],
          parent="neck")
        for side, sx in SIDES:
            h(f"ear_{side}", [c((mirrored(2, 3, sx), 36, -3), (3, 1, 2), "face")], (2 * sx, 36.5, -2), [0, 0, -18 * sx])
    elif face == "eloaiou":
        h("head", [c((-2, 34, -4), (4, 5, 5), "face"), c((-1, 33, -11), (2, 4, 7), "face"), c((-1, 33, -12), (2, 3, 1), "muzzle"),
                   c((-0.5, 34, 0), (1, 6, 2), "mane")], parent="neck")
        for side, sx in SIDES:
            h(f"ear_{side}", [c((mirrored(1, 1, sx), 39, -2), (1, 8, 2), "face")], (1.5 * sx, 39, -1), [-14, 0, 10 * sx])
    elif face == "astaphaios":
        h("head", [c((-2.5, 34, -4), (5, 5, 5), "face"), c((-1.5, 33, -8), (3, 3, 4), "face"), c((-1.5, 33, -9), (3, 2, 1), "muzzle"),
                   c((-0.5, 30, 0), (1, 9, 2), "mane")], parent="neck")
        h("jaw", [c((-1.5, 32, -8), (3, 1, 4), "jaw")], (0, 33, -4), [12, 0, 0])
        for side, sx in SIDES:
            h(f"ear_{side}", [c((mirrored(2, 2, sx), 39, -2), (2, 2, 1), "face")], (2 * sx, 39, -1.5), [0, 0, -12 * sx])
    elif face == "yao":
        h("head", [c((-2, 33, -1.5), (4, 2, 2), "scale")], parent="neck")
        for i in range(7):
            lean = (i - 3) * 26
            x = (i - 3) * 0.5 - 0.5 / HEAD_SCALE
            length = 10 - abs(i - 3) * 1.2
            m.bone(f"neck_{i}", "head", grow((x + 0.5 / HEAD_SCALE, 35, -0.5)), [-12, 0, -lean],
                   [c(grow((x, 35, -1)), (1, int(length * HEAD_SCALE), 1), "scale")])
            top = grow((x, 35, -1))[1] + int(length * HEAD_SCALE)
            nx = grow((x, 35, -1))[0]
            m.bone(f"snake_{i}", f"neck_{i}", (nx + 0.5, top, -1), [18, 0, lean * 0.85],
                   [c((nx - 0.5, top, -4.5), (2, 1, 4), "scale"), c((nx, top, -5.5), (1, 1, 1), "muzzle")])
    elif face == "sabaoth":
        h("head", [c((-2.5, 34, -4), (5, 4, 5), "face"), c((-1.5, 34, -10), (3, 3, 6), "face"), c((-1.5, 35, -11), (3, 1, 1), "muzzle")],
          parent="neck")
        h("jaw", [c((-1.5, 33, -9), (3, 1, 5), "jaw")], (0, 34, -4), [6, 0, 0])
        for side, sx in SIDES:
            h(f"horn_{side}", [c((mirrored(1, 1, sx) + 0.5 * sx, 37, -1), (1, 1, 7), "horn")], (1.5 * sx, 37.5, -1), [28, 0, 0])
            h(f"brow_{side}", [c((mirrored(0.5, 2, sx), 38, -5), (2, 1, 3), "horn")], (1.5 * sx, 38, -4))
        for i, z in enumerate((-3, -1, 1)):
            h(f"crest_{i}", [c((-0.5, 38, z), (1, 2 if i < 2 else 1, 1), "horn")], (0, 38, z))
    elif face == "adonin":
        h("head", [c((-3, 34, -4), (6, 6, 5), "face"), c((-3, 38, -5), (6, 2, 1), "brow"), c((-2, 34, -7), (4, 3, 3), "muzzle"),
                   c((-1, 40, -3), (2, 1, 4), "brow")], parent="neck")
        for side, sx in SIDES:
            h(f"ear_{side}", [c((mirrored(3, 1, sx), 36, -2), (1, 2, 2), "face")], (3 * sx, 37, -1))
    elif face == "sabbataios":
        h("head", [c((-2.5, 34, -4), (5, 4, 5), "flame_core")], parent="neck")
        tongues = [((-2, 38, -3), (4, 3, 4)), ((-1.5, 41, -2), (3, 3, 3)), ((-0.5, 44, -2), (1, 3, 2)),
                   ((-3, 36, -2), (1, 4, 2)), ((2, 37, -3), (1, 4, 2)), ((-2, 41, -1), (1, 2, 1))]
        for i, (o, s) in enumerate(tongues):
            h(f"flame_{i}", [c(o, s, "flame")], (o[0] + s[0] / 2, o[1], o[2] + s[2] / 2))


def build(face):
    m = Model(f"geometry.watcher_{face}", TEX, 4, 4, [0, 1.5, 0])
    m.bone("root", None, (0, 0, 0))
    m.bone("body", "root", (0, 18, 0))
    m.bone("tail", "body", (0, 18, 0), cubes=[c((-1.5, 10, -1), (3, 8, 2), "hide")])
    m.bone("tail_tip", "tail", (0, 10, 0), cubes=[c((-1, 4, -0.5), (2, 6, 1), "hide"), c((-0.5, 0, -0.5), (1, 4, 1), "hide")])
    m.bone("torso", "body", (0, 18, 0), cubes=[c((-2.5, 18, -1.5), (5, 14, 3), "hide")])
    m.bone("neck", "torso", (0, 32, 0), cubes=[c((-1, 32, -1), (2, 3, 2), "hide")])
    head(m, face)
    eyes = []
    for side, sx in SIDES:
        for wing, spec in WINGS.items():
            parent = f"{spec['parent']}_{side}" if "parent" in spec else ("torso" if wing in ("hood", "arm") else "body")
            px, py, pz = spec["pivot"]
            cubes = [c((mirrored(o[0], s[0], sx), o[1], o[2]), s, f"wing_{wing}") for o, s in spec["cubes"]]
            m.bone(f"{wing}_{side}", parent, (px * sx, py, pz), cubes=cubes)
            for i, (u, v, size) in enumerate(spec["eyes"]):
                inner = spec["inner"] if spec["inner"] != "plate" else ("right" if sx < 0 else "left")
                eyes.append((f"{wing}_{side}", inner, u, v, size, f"pupil_{wing}_{side}_{i}"))
    m.pack()
    cubes = {b["name"]: b["cubes"][0] for b in m.bones if b["cubes"]}
    normals = {"left": (1, 0, 0), "right": (-1, 0, 0), "front": (0, 0, -1)}
    for wing, inner, u, v, size, name in eyes:
        x, y, z = texel_point(cubes[wing], inner, u, v)
        nx, ny, nz = normals[inner]
        at = (x + nx * 0.6, y, z + nz * 0.6)
        m.bone(name, wing, at, cubes=[c((at[0] - 0.5, at[1] - 0.5, at[2] - 0.5), (1, 1, 1), "pupil", -0.15)])
        m.bones[-1]["cubes"][0]["uv"] = list(PUPIL_UV)
    m.eyes = [(wing, inner, u, v, size) for wing, inner, u, v, size, _ in eyes]
    return m


PUPIL_UV = (124, 124)

def pose(**left):
    out = {}
    for name, (x, y, z) in left.items():
        out[f"{name}_left"] = [x, y, z]
        out[f"{name}_right"] = [x, -y if y else 0, -z if z else 0]
    return out


CLOSED = pose(hood=(0, 172, 0), hood_tip=(0, 12, 0), foot=(0, 168, 0), arm=(0, -85, 0), hand=(0, 0, 70))
OPEN = pose(hood=(0, 100, 0), hood_tip=(0, 28, 0), foot=(-6, 102, 0), arm=(0, -48, -8), hand=(0, 0, 34))
SPREAD = pose(hood=(0, 78, 0), hood_tip=(0, 18, 0), foot=(-12, 84, 0), arm=(0, -6, -26), hand=(0, 0, -12))


def toward(a, b, k):
    return {n: [round(a[n][i] + (b[n][i] - a[n][i]) * k, 2) for i in range(3)] for n in a}


def held(pose, t=0.0):
    return {n: {"rotation": {t: v}} for n, v in pose.items()}


def blend(*frames):
    out = {}
    for t, pose in frames:
        for n, v in pose.items():
            out.setdefault(n, {}).setdefault("rotation", {})[t] = v
    return out


def merge(*parts):
    out = {}
    for p in parts:
        for bone, chans in p.items():
            for ch, frames in chans.items():
                out.setdefault(bone, {}).setdefault(ch, {}).update(frames)
    return out


def head_life(face, length, amp):
    out = {}
    if face == "yao":
        for i in range(7):
            ph = i * 0.9
            out[f"neck_{i}"] = {"rotation": loop(length, [(0, [0, 0, 0]), (length * (0.25 + 0.05 * (i % 3)), [amp * 0.8, amp * (1 if i % 2 else -1), 0]),
                                                           (length * 0.6, [-amp * 0.5, 0, amp * 0.4 * (1 if i < 3 else -1)])])}
            out[f"snake_{i}"] = {"rotation": loop(length, [(0, [0, 0, 0]), (length * ((ph % 1) * 0.5 + 0.25), [amp, 0, 0])])}
    elif face == "sabbataios":
        for i in range(6):
            out[f"flame_{i}"] = {"scale": loop(length, [(0, [1, 1, 1]), (length * (0.2 + 0.11 * i) % length, [0.85, 1.25, 0.85]),
                                                        (length * (0.55 + 0.07 * i) % length, [1.1, 0.8, 1.1])]),
                                 "rotation": loop(length, [(0, [0, 0, 0]), (length * 0.5, [0, 0, amp * (1 if i % 2 else -1)])])}
    elif face in ("athoth", "eloaiou", "astaphaios", "adonin"):
        for side, sx in SIDES:
            out[f"ear_{side}"] = {"rotation": loop(length, [(0, [0, 0, 0]), (length * 0.7, [0, 0, 0]), (length * 0.75, [-amp, 0, amp * sx]),
                                                            (length * 0.85, [0, 0, 0])])}
    return out


def idle(face):
    L = 4.0
    return anim(L, True, merge(
        {"body": {"position": loop(L, [(0, [0, 0, 0]), (2, [0, 0.8, 0])])},
         "tail": {"rotation": loop(L, [(0, [4, 0, 2]), (2, [-3, 0, -2])])},
         "tail_tip": {"rotation": loop(L, [(0, [6, 0, -3]), (2, [-5, 0, 3])])}},
        blend((0, CLOSED), (2, toward(CLOSED, OPEN, 0.08)), (L, CLOSED)),
        head_life(face, L, 8)))


def watch(face):
    L = 3.0
    return anim(L, True, merge(
        {"body": {"position": loop(L, [(0, [0, 0, 0]), (1.5, [0, 0.6, 0])])},
         "torso": {"rotation": loop(L, [(0, [-4, 0, 0]), (1.5, [-2, 0, 0])])},
         "tail": {"rotation": loop(L, [(0, [6, 0, 3]), (1.5, [2, 0, -3])])},
         "tail_tip": {"rotation": loop(L, [(0, [10, 0, -4]), (1.5, [4, 0, 4])])}},
        blend((0, OPEN), (1.5, toward(OPEN, SPREAD, 0.15)), (L, OPEN)),
        head_life(face, L, 10)))


def windup(face):
    L = 2.0
    return anim(L, "hold_on_last_frame", merge(
        {"torso": {"rotation": {0: [-4, 0, 0], 1.2: [-14, 0, 0], 2.0: [-16, 0, 0]}},
         "body": {"position": {0: [0, 0, 0], 2.0: [0, 3, 0]}},
         "tail": {"rotation": {0: [6, 0, 0], 2.0: [22, 0, 0]}},
         "tail_tip": {"rotation": {0: [8, 0, 0], 2.0: [28, 0, 0]}}},
        blend((0, OPEN), (1.2, toward(OPEN, SPREAD, 0.9)), (2.0, SPREAD)),
        head_life(face, L, 14)))


def work(face):
    L = 1.2
    beat = toward(SPREAD, OPEN, 0.35)
    return anim(L, True, merge(
        {"torso": {"rotation": loop(L, [(0, [-16, 0, 0]), (0.6, [-12, 0, 0])])},
         "body": {"position": loop(L, [(0, [0, 3, 0]), (0.6, [0, 2.2, 0])])},
         "tail": {"rotation": loop(L, [(0, [22, 0, 0]), (0.6, [16, 0, 0])])},
         "tail_tip": {"rotation": loop(L, [(0, [28, 0, 0]), (0.6, [20, 0, 0])])}},
        blend((0, SPREAD), (0.6, beat), (L, SPREAD)),
        head_life(face, L, 16)))


def lost(face):
    L = 2.4
    half = toward(CLOSED, OPEN, 0.55)
    return anim(L, True, merge(
        {"head": {"rotation": loop(L, [(0, [0, 0, 0]), (0.6, [6, 0, 22]), (1.2, [0, 0, 0]), (1.8, [6, 0, -22])])},
         "body": {"position": loop(L, [(0, [0, 0, 0]), (1.2, [0, -0.6, 0])])}},
        blend((0, half), (1.2, toward(half, OPEN, 0.3)), (L, half)),
        head_life(face, L, 6)))


def arrive(face):
    L = 1.6
    return anim(L, False, merge(
        {"root": {"scale": {0: [0.05, 0.05, 0.05], 0.7: [0.9, 1.1, 0.9], 0.9: [1, 1, 1]}}},
        blend((0, CLOSED), (0.8, CLOSED), (1.6, OPEN))))


def withdraw(face):
    L = 1.6
    return anim(L, "hold_on_last_frame", merge(
        {"root": {"scale": {0: [1, 1, 1], 0.8: [1, 1, 1], 1.0: [0.9, 1.1, 0.9], 1.6: [0.02, 0.02, 0.02]}}},
        blend((0, OPEN), (0.8, CLOSED), (1.6, CLOSED))))


def death(face):
    L = 1.4
    limp = {n: [v[0] + 30, v[1] * 0.5, v[2] - 20] for n, v in CLOSED.items()}
    return anim(L, "hold_on_last_frame", merge(
        {"root": {"position": {0: [0, 0, 0], 1.4: [0, -14, 0]}, "rotation": {0: [0, 0, 0], 1.4: [0, 0, 80]}},
         "head": {"rotation": {0: [0, 0, 0], 1.4: [35, 0, 0]}}},
        blend((0, OPEN), (1.4, limp))))


CLIPS = {"idle": idle, "watch": watch, "windup": windup, "work": work, "lost": lost, "arrive": arrive, "withdraw": withdraw,
         "death": death}

MODELS = {face: build(face) for face in FACES}

if __name__ == "__main__":
    for face, m in MODELS.items():
        write(f"{ASSETS}/geo/entity/watcher_{face}.geo.json", m.geometry())
        write(f"{ASSETS}/animations/entity/watcher_{face}.animation.json",
              animations("watcher", {name: clip(face) for name, clip in CLIPS.items()}))
