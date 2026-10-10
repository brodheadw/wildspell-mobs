from geckolib_model import Model, anim, animations, c, write

ASSETS = "src/main/resources/assets/wildspellmobs"
GEO_OUT = f"{ASSETS}/geo/entity/flytrap_head.geo.json"
ANIM_OUT = f"{ASSETS}/animations/entity/flytrap_head.animation.json"
TEX = 128

MODEL = Model("geometry.flytrap_head", TEX, 3, 3, [0, 1, 0])
BONES = MODEL.bones
bone = MODEL.bone


bone("root", None, (0, 0, 0))
LEAN = 12
BEND = 14
bone("neck", "root", (0, 0, 0), [LEAN, 0, 0], [c((-1, 0, -1), (2, 4, 2), "stalk")])
bone("neck_leaf", "neck", (1, 2, 0), [0, -25, -30], [c((1, 2, -1.5), (5, 0, 3), "leaf")])
bone("neck_bend", "neck", (0, 4, 0), [BEND, 0, 0], [c((-1, 3, -1), (2, 4, 2), "stalk", -0.02)])
bone("neck_leaf_2", "neck_bend", (-1, 4, 0), [0, 30, 35], [c((-5, 4, -1), (4, 0, 2), "leaf")])
bone("head", "neck_bend", (0, 7, 0))

HINGE = (0, 9, 1)
STEPS = ((3, -9, 1), (5, -8, 0), (6, -6, -2))
CILIA = 0.12


def on_lobe(x, z):
    return any(abs(x) < hw and front < z < back for hw, front, back in STEPS)


def lobe(sign, fringe):
    """One trap lobe, closed: a plate rounded in steps with a low dome on it, cilia standing on the rim."""
    lip_y = 9 if sign > 0 else 8
    out = lip_y + sign
    y0 = lip_y - 2 if sign > 0 else lip_y + 1
    off = CILIA if sign > 0 else 2 * CILIA
    cubes = [c((-2, out + sign, -5), (4, 1, 3), "dome"), c((-4, out, -7), (8, 1, 6), "dome")]
    inner = 0
    for k, (hw, front, back) in enumerate(STEPS):
        reach = STEPS[k + 1][1] if k + 1 < len(STEPS) else back - 1
        for x0, x1 in ((-hw, -inner), (inner, hw)) if inner else ((-hw, hw),):
            cubes.append(c((x0, lip_y, front), (x1 - x0, 1, back - front), "lobe"))
            cubes.append(c((x0, y0, front - off), (x1 - x0, 2, 0), fringe))
        for x in (-hw - off, hw + off):
            cubes.append(c((x, y0, front), (0, 2, reach - front), fringe))
        inner = hw
    return cubes


bone("pod", "head", (0, 7, 0), [-6, 0, 0], [c((-2.5, 7.5, 0), (5, 3, 2), "hinge"), c((-1.5, 6.5, 0.5), (3, 1, 2), "hinge")])
bone("upper_jaw", "pod", HINGE, [-34, 0, 0], lobe(1, "upper_cilia"))
bone("lower_jaw", "pod", HINGE, [30, 0, 0], lobe(-1, "lower_cilia"))

MODEL.pack()


CLOSED_UPPER = [34, 0, 0]
CLOSED_LOWER = [-30, 0, 0]
STRIKE_NECK = [14, 0, 0]
STRIKE_BEND = [-4, 0, 0]
STRIKE_REACH = [0, 0, -2]
STRIKE_POD = [-16, 0, 0]


def rest():
    still = {0: [0, 0, 0], 1: [0, 0, 0]}
    bones = {name: {"rotation": still} for name in idle()["bones"]}
    bones["neck"]["position"] = still
    return anim(1.0, True, bones)


def idle():
    L = 3.0
    return anim(L, True, {
        "neck": {"rotation": {0: [0, 0, 3], 0.75: [3, 0, 0], 1.5: [0, 0, -3], 2.25: [-3, 0, 0], 3: [0, 0, 3]}},
        "neck_bend": {"rotation": {0: [0, 0, 0], 0.75: [3, 0, 0], 2.25: [-2, 0, 0], 3: [0, 0, 0]}},
        "pod": {"rotation": {0: [-2, 0, -2], 0.75: [0, 0, -3], 1.5: [2, 0, 2], 2.25: [0, 0, 3], 3: [-2, 0, -2]}},
        "upper_jaw": {"rotation": {0: [0, 0, 0], 1.2: [-6, 0, 0], 1.8: [-6, 0, 0], 3: [0, 0, 0]}},
        "lower_jaw": {"rotation": {0: [0, 0, 0], 1.2: [4, 0, 0], 1.8: [4, 0, 0], 3: [0, 0, 0]}},
        "neck_leaf": {"rotation": {0: [0, 0, 0], 1.5: [0, 0, 6], 3: [0, 0, 0]}},
        "neck_leaf_2": {"rotation": {0: [0, 0, 0], 1.5: [0, 0, -5], 3: [0, 0, 0]}},
    })


def lunge():
    L = 0.4
    return anim(L, False, {
        "neck": {"rotation": {0: [0, 0, 0], 0.2: [-26, 0, 0], 0.32: [22, 0, 0], L: STRIKE_NECK},
                 "position": {0: [0, 0, 0], 0.2: [0, 0, 1], 0.32: [0, 0, -2.5], L: STRIKE_REACH}},
        "neck_bend": {"rotation": {0: [0, 0, 0], 0.2: [-14, 0, 0], 0.32: [4, 0, 0], L: STRIKE_BEND}},
        "pod": {"rotation": {0: [0, 0, 0], 0.2: [-2, 0, 0], 0.32: [-20, 0, 0], L: STRIKE_POD}},
        "upper_jaw": {"rotation": {0: [0, 0, 0], 0.2: [-22, 0, 0], 0.32: [-18, 0, 0], L: CLOSED_UPPER}},
        "lower_jaw": {"rotation": {0: [0, 0, 0], 0.2: [14, 0, 0], 0.32: [10, 0, 0], L: CLOSED_LOWER}},
        "neck_leaf": {"rotation": {0: [0, 0, 0], 0.2: [0, 0, 10], L: [0, 0, -10]}},
    })


def hold():
    L = 0.6
    shake = {0: -9, 0.15: 9, 0.3: -9, 0.45: 9, 0.6: -9}
    return anim(L, True, {
        "neck": {"rotation": {0: STRIKE_NECK, 0.3: [8, 0, 0], L: STRIKE_NECK},
                 "position": {0: STRIKE_REACH, L: STRIKE_REACH}},
        "neck_bend": {"rotation": {0: STRIKE_BEND, L: STRIKE_BEND}},
        "pod": {"rotation": {t: [STRIKE_POD[0], y, y * 0.4] for t, y in shake.items()}},
        "upper_jaw": {"rotation": {0: CLOSED_UPPER, L: CLOSED_UPPER}},
        "lower_jaw": {"rotation": {0: CLOSED_LOWER, L: CLOSED_LOWER}},
    })


def wither():
    L = 1.0
    return anim(L, False, {
        "neck": {"rotation": {0: [0, 0, 0], 0.4: [-4, 0, 16], L: [-6, 0, 40]}},
        "neck_bend": {"rotation": {0: [0, 0, 0], 0.4: [6, 0, 6], L: [12, 0, 12]}},
        "pod": {"rotation": {0: [0, 0, 0], 0.5: [16, 0, 8], L: [30, 0, 14]}},
        "upper_jaw": {"rotation": {0: [0, 0, 0], 0.3: [-8, 0, 0], L: [14, 0, 0]}},
        "lower_jaw": {"rotation": {0: [0, 0, 0], 0.3: [6, 0, 0], L: [-4, 0, 0]}},
        "neck_leaf": {"rotation": {0: [0, 0, 0], L: [0, 0, 40]}},
    })


BRANCH_REACH = 14
BRANCH_HEIGHT = 9


def face(uv, texture="#stem"):
    return {"uv": uv, "texture": texture}


def rosette(half):
    lo, hi = 8 - half, 8 + half
    return {"from": [lo, 0.25, lo], "to": [hi, 0.25, hi],
            "faces": {"up": face([0, 0, 16, 16], "#leaves"), "down": face([0, 16, 16, 0], "#leaves")}}


def post(x0, x1, y0, y1, ends=True):
    w = x1 - x0
    faces = {"north": face([0, 16 - y1 + y0, w, 16]), "south": face([2, 16 - y1 + y0, 2 + w, 16]),
             "west": face([4, 16 - y1 + y0, 4 + w, 16]), "east": face([6, 16 - y1 + y0, 6 + w, 16])}
    if ends:
        faces["up"] = face([10, 0, 10 + w, w])
        faces["down"] = face([10, 0, 10 + w, w])
    return {"from": [x0, y0, x0], "to": [x1, y1, x1], "faces": faces}


def arm(x0, x1):
    y0 = BRANCH_HEIGHT - 2
    return {"from": [x0, y0, 7], "to": [x1, BRANCH_HEIGHT, 9],
            "faces": {"north": face([0, 12, x1 - x0, 14]), "south": face([0, 12, x1 - x0, 14]),
                      "up": face([0, 12, x1 - x0, 14]), "down": face([0, 12, x1 - x0, 14]),
                      "west": face([10, 0, 12, 2]), "east": face([10, 0, 12, 2])}}


def block_model(elements):
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "ambientocclusion": False,
            "textures": {"particle": "wildspellmobs:block/flytrap_stem", "stem": "wildspellmobs:block/flytrap_stem",
                         "leaves": "wildspellmobs:block/flytrap_leaves"},
            "elements": elements}


BLOCK_MODELS = {
    "flytrap_sprout": block_model([rosette(5), post(7.5, 8.5, 0, 6)]),
    "flytrap_young": block_model([rosette(8), post(7, 9, 0, 16)]),
    "flytrap_grown": block_model([rosette(12), post(6, 10, 0, 16, ends=False)]),
    "flytrap_stem": block_model([post(6.5, 9.5, 0, 16)]),
    "flytrap_stem_branches": block_model([post(6, 10, 0, 16, ends=False),
                                          arm(8 - BRANCH_REACH - 1, 6), arm(10, 8 + BRANCH_REACH + 1)]),
}

BLOCKSTATES = {
    "flytrap": {"variants": {"age=0": {"model": "wildspellmobs:block/flytrap_sprout"},
                             "age=1": {"model": "wildspellmobs:block/flytrap_young"},
                             "age=2": {"model": "wildspellmobs:block/flytrap_grown"}}},
    "flytrap_stem": {"variants": {"branches=false": {"model": "wildspellmobs:block/flytrap_stem"},
                                  "branches=true": {"model": "wildspellmobs:block/flytrap_stem_branches"}}},
}


if __name__ == "__main__":
    write(GEO_OUT, MODEL.geometry())
    write(ANIM_OUT, animations("flytrap_head", {"rest": rest(), "idle": idle(), "lunge": lunge(), "hold": hold(), "wither": wither()}))
    for name, model in BLOCK_MODELS.items():
        write(f"{ASSETS}/models/block/{name}.json", model)
    for name, state in BLOCKSTATES.items():
        write(f"{ASSETS}/blockstates/{name}.json", state)
    print("flytrap head model, animations, block models and blockstates written")
