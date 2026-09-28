from geckolib_model import Model, anim, c, kf, write

ASSETS = "src/main/resources/assets/wildspellmobs"
GEO_OUT = f"{ASSETS}/geo/entity/flytrap_head.geo.json"
ANIM_OUT = f"{ASSETS}/animations/entity/flytrap_head.animation.json"
TEX = 64

MODEL = Model("geometry.flytrap_head", TEX, 3, 3, [0, 1, 0])
BONES = MODEL.bones
bone = MODEL.bone
geometry = MODEL.geometry


bone("root", None, (0, 0, 0))
bone("neck", "root", (0, 0, 0), [-6, 0, 0], [c((-1, 0, -1), (2, 7, 2), "stalk")])
bone("neck_leaf", "neck", (1, 3, 0), [0, -20, -35], [c((1, 2.5, -1), (4, 1, 2), "leaf_small")])
bone("head", "neck", (0, 7, 0))
bone("pod", "head", (0, 7, 0), [6, 0, 0], [c((-2.5, 6, -0.5), (5, 4, 3), "hinge")])
bone("upper_jaw", "pod", (0, 9, 1.5), [-24, 0, 0], [c((-4, 9, -8), (8, 3, 10), "jaw_upper")])
bone("lower_jaw", "pod", (0, 9, 1.5), [16, 0, 0], [c((-4, 6, -8), (8, 3, 10), "jaw_lower")])

UPPER_FRONT = (-3.5, -1.5, 0.5, 2.5)
LOWER_FRONT = (-2.5, -0.5, 1.5)
UPPER_SIDE = (-6.5, -3.5, -0.5)
LOWER_SIDE = (-5, -2)
for k, x in enumerate(UPPER_FRONT):
    bone(f"upper_tooth_front_{k}", "upper_jaw", (x + 0.5, 9, -8.5), cubes=[c((x, 7, -9), (1, 2, 1), "tooth")])
for k, x in enumerate(LOWER_FRONT):
    bone(f"lower_tooth_front_{k}", "lower_jaw", (x + 0.5, 9, -8.5), cubes=[c((x, 9, -9), (1, 2, 1), "tooth")])
for side, sx in (("right", -5), ("left", 4)):
    for k, z in enumerate(UPPER_SIDE):
        bone(f"upper_tooth_{side}_{k}", "upper_jaw", (sx + 0.5, 9, z + 0.5), cubes=[c((sx, 7, z), (1, 2, 1), "tooth")])
    for k, z in enumerate(LOWER_SIDE):
        bone(f"lower_tooth_{side}_{k}", "lower_jaw", (sx + 0.5, 9, z + 0.5), cubes=[c((sx, 9, z), (1, 2, 1), "tooth")])


MODEL.pack()


CLOSED_UPPER = [24, 0, 0]
CLOSED_LOWER = [-16, 0, 0]
STRIKE_NECK = [40, 0, 0]
STRIKE_REACH = [0, 0, -2]
STRIKE_POD = [-24, 0, 0]


def idle():
    L = 3.0
    return anim(L, True, {
        "neck": {"rotation": {0: [0, 0, 3], 0.75: [3, 0, 0], 1.5: [0, 0, -3], 2.25: [-3, 0, 0], 3: [0, 0, 3]}},
        "pod": {"rotation": {0: [-2, 0, -2], 0.75: [0, 0, -3], 1.5: [2, 0, 2], 2.25: [0, 0, 3], 3: [-2, 0, -2]}},
        "upper_jaw": {"rotation": {0: [0, 0, 0], 1.2: [-6, 0, 0], 1.8: [-6, 0, 0], 3: [0, 0, 0]}},
        "lower_jaw": {"rotation": {0: [0, 0, 0], 1.2: [4, 0, 0], 1.8: [4, 0, 0], 3: [0, 0, 0]}},
        "neck_leaf": {"rotation": {0: [0, 0, 0], 1.5: [0, 0, 6], 3: [0, 0, 0]}},
    })


def lunge():
    L = 0.4
    return anim(L, False, {
        "neck": {"rotation": {0: [0, 0, 0], 0.2: [-24, 0, 0], 0.32: [48, 0, 0], L: STRIKE_NECK},
                 "position": {0: [0, 0, 0], 0.2: [0, 0, 1], 0.32: [0, 0, -2.5], L: STRIKE_REACH}},
        "pod": {"rotation": {0: [0, 0, 0], 0.2: [-6, 0, 0], 0.32: [-28, 0, 0], L: STRIKE_POD}},
        "upper_jaw": {"rotation": {0: [0, 0, 0], 0.2: [-22, 0, 0], 0.32: [-18, 0, 0], L: CLOSED_UPPER}},
        "lower_jaw": {"rotation": {0: [0, 0, 0], 0.2: [14, 0, 0], 0.32: [10, 0, 0], L: CLOSED_LOWER}},
        "neck_leaf": {"rotation": {0: [0, 0, 0], 0.2: [0, 0, 10], L: [0, 0, -10]}},
    })


def hold():
    L = 0.6
    shake = {0: -9, 0.15: 9, 0.3: -9, 0.45: 9, 0.6: -9}
    return anim(L, True, {
        "neck": {"rotation": {0: STRIKE_NECK, 0.3: [34, 0, 0], L: STRIKE_NECK},
                 "position": {0: STRIKE_REACH, L: STRIKE_REACH}},
        "pod": {"rotation": {t: [STRIKE_POD[0], y, y * 0.4] for t, y in shake.items()}},
        "upper_jaw": {"rotation": {0: CLOSED_UPPER, L: CLOSED_UPPER}},
        "lower_jaw": {"rotation": {0: CLOSED_LOWER, L: CLOSED_LOWER}},
    })


def wither():
    L = 1.0
    return anim(L, False, {
        "neck": {"rotation": {0: [0, 0, 0], 0.4: [10, 0, 18], L: [22, 0, 48]}},
        "pod": {"rotation": {0: [0, 0, 0], 0.5: [16, 0, 8], L: [30, 0, 14]}},
        "upper_jaw": {"rotation": {0: [0, 0, 0], 0.3: [-8, 0, 0], L: [14, 0, 0]}},
        "lower_jaw": {"rotation": {0: [0, 0, 0], 0.3: [6, 0, 0], L: [-4, 0, 0]}},
        "neck_leaf": {"rotation": {0: [0, 0, 0], L: [0, 0, 40]}},
    })


def animations():
    return {"format_version": "1.8.0", "animations": {
        "animation.flytrap_head.idle": idle(),
        "animation.flytrap_head.lunge": lunge(),
        "animation.flytrap_head.hold": hold(),
        "animation.flytrap_head.wither": wither(),
    }}


BRANCH_REACH = 14
BRANCH_HEIGHT = 9


def face(uv, texture="#stem", cull=None):
    out = {"uv": uv, "texture": texture}
    if cull:
        out["cullface"] = cull
    return out


def rosette(half):
    lo, hi = 8 - half, 8 + half
    return {"from": [lo, 0.25, lo], "to": [hi, 0.25, hi],
            "faces": {"up": face([0, 0, 16, 16], "#leaves"), "down": face([0, 16, 16, 0], "#leaves")}}


def post(x0, x1, y0, y1, z0=None, z1=None, ends=True):
    z0, z1 = (x0, x1) if z0 is None else (z0, z1)
    w, d = x1 - x0, z1 - z0
    faces = {"north": face([0, 16 - y1 + y0, w, 16]), "south": face([2, 16 - y1 + y0, 2 + w, 16]),
             "west": face([4, 16 - y1 + y0, 4 + d, 16]), "east": face([6, 16 - y1 + y0, 6 + d, 16])}
    if ends:
        faces["up"] = face([10, 0, 10 + w, d])
        faces["down"] = face([10, 0, 10 + w, d])
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}


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
    write(GEO_OUT, geometry())
    write(ANIM_OUT, animations())
    for name, model in BLOCK_MODELS.items():
        write(f"{ASSETS}/models/block/{name}.json", model)
    for name, state in BLOCKSTATES.items():
        write(f"{ASSETS}/blockstates/{name}.json", state)
    print("flytrap head model, animations, block models and blockstates written")
