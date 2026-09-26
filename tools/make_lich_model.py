"""Builds the Ice Lich's GeckoLib model and animations. Run from the repo root: python3 tools/make_lich_model.py

Bedrock geometry (format 1.12.0, box UV on a 128x128 texture). The model faces north (-Z) and the
lich's right side is -X, as in Blockbench's Bedrock player model. All sizes are whole pixels so the
box-UV nets land on whole texels. Each cube carries a material name; tools/paint_lich.py imports this
module and paints every net from it, so the UV layout lives in one place.

Hierarchy: root > body > {hem > hem strips, torso > {mantle > collar + pauldrons > shards,
neck > head > {jaw, crown > spikes}, right_arm > {right_hand, staff > staff_crystal}, left_arm > fingers}}.
"""
import json
import os

GEO_OUT = "src/main/resources/assets/wildspellmobs/geo/entity/ice_lich.geo.json"
ANIM_OUT = "src/main/resources/assets/wildspellmobs/animations/entity/ice_lich.animation.json"
TEX = 128

BONES = []    # dicts: name, parent, pivot, rotation, cubes; cubes: origin, size, material, inflate, uv


def bone(name, parent, pivot, rotation=None, cubes=()):
    BONES.append({"name": name, "parent": parent, "pivot": list(pivot), "rotation": rotation,
                  "cubes": [{"origin": list(o), "size": list(s), "material": m, "inflate": i} for (o, s, m, i) in cubes]})


def c(origin, size, material, inflate=0):
    return (origin, size, material, inflate)


# --- Body and robe ---------------------------------------------------------------------------------
# He floats: the body pivot sits at the waist and the idle bob moves it. The skirt tapers from the belt
# into a narrower lower robe, and fourteen tattered strips hang from under it, ending 4-6px above y=0.

bone("root", None, (0, 0, 0))
bone("body", "root", (0, 20, 0))
bone("hem", "body", (0, 20, 0), cubes=[
    c((-6, 14, -4), (12, 6, 8), "skirt"),
    c((-5, 10, -3.5), (10, 4, 7), "skirt_low"),
])
# Strips: (bone suffix, x or z start, width, length, side). Front and back strips span x; side strips
# span z. Each flares outward from its top (y=13) so the hem reads as loose cloth, not a skirt box.
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
    wobble = (len(suffix) * 7 + width * 3 + length) % 5 - 2   # a little irregularity per strip
    rx, ry, rz = FLARE[side]
    rot = [rx + (wobble if side in ("front", "back") else 0), 0, rz + (wobble if side in ("right", "left") else 0)]
    bone("hem_" + suffix, "hem", pivot, rot, [c(origin, size, "strip")])

bone("torso", "body", (0, 20, 0), cubes=[
    c((-4, 20, -2.5), (8, 9, 5), "ribs"),
    c((-5, 19, -3), (10, 11, 6), "robe_chest"),
    c((-5.5, 18, -3.5), (11, 2, 7), "belt"),
])

# --- Mantle, collar and pauldrons ------------------------------------------------------------------
bone("mantle", "torso", (0, 29, 0), cubes=[c((-8, 27, -4), (16, 4, 8), "mantle", 0.25)])
bone("collar", "mantle", (0, 30, 2.5), [-18, 0, 0], [c((-5, 30, 2), (10, 6, 2), "collar")])
for side, sx in (("right", -1), ("left", 1)):
    x0 = -11 if side == "right" else 5
    bone(f"{side}_pauldron", "mantle", (7 * sx, 30, 0), [0, 0, 16 * sx], [c((x0, 27, -4.5), (6, 4, 9), "pauldron")])
    # Ice shards jutting up and out of each pauldron: (pivot x offset, z, rotation, size).
    for i, (dx, z, rot, size) in enumerate([(2.5, -2, (8, 0, 28), (2, 5, 2)), (1, 1.5, (-22, 0, 16), (1, 4, 1)),
                                            (3.5, 2, (-8, 0, 48), (1, 3, 1))]):
        px = 7 * sx + dx * sx
        rot = [rot[0], 0, rot[2] * sx]           # top leans outward on both sides
        o = (px - size[0] / 2, 30.5, z - size[2] / 2)
        bone(f"{side}_shard_{i}", f"{side}_pauldron", (px, 30.5, z), rot, [c(o, size, "ice")])

# --- Head, jaw and crown ---------------------------------------------------------------------------
# The renderer turns `head` toward the target, so the animations tilt `neck` instead.
bone("neck", "torso", (0, 30, 0), cubes=[c((-1, 29, -1), (2, 4, 2), "bone")])
bone("head", "neck", (0, 32, -0.5), cubes=[c((-3.5, 33, -4), (7, 7, 7), "skull")])
bone("jaw", "head", (0, 33.5, 0), [10, 0, 0], [c((-3, 31.5, -4), (6, 2, 5), "jaw")])
bone("crown", "head", (0, 40, -0.5), cubes=[c((-4, 39, -4.5), (8, 2, 8), "crown_band")])
# Spikes: (name, base centre x, z, rotation, base w, base h, tip h). Tallest at the front.
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

# --- Arms, hands and the staff ---------------------------------------------------------------------
# Wide bell sleeves over bare arm bones. The right arm is held a little forward and the staff bone
# cancels that angle, so at rest the staff stands upright in the hand.
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

# Right hand: bony fingers curled around the staff in front of the palm.
for i, x in enumerate((-8.5, -7.5, -6.5)):
    bone(f"right_finger_{i}", "right_hand", (x + 0.5, 11.5, -1.5), [30, 0, 0], [c((x, 9.5, -2.5), (1, 2, 1), "bone")])
# Left hand: four long splayed claws.
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


# --- UV packing ------------------------------------------------------------------------------------

def net_size(size):
    w, h, d = size
    return 2 * (w + d), d + h


def pack():
    """Shelf-pack every cube's box-UV net onto the texture, tallest nets first."""
    cubes = [cube for b in BONES for cube in b["cubes"]]
    order = sorted(cubes, key=lambda cube: (-net_size(cube["size"])[1], -net_size(cube["size"])[0]))
    x = y = shelf = 0
    for cube in order:
        nw, nh = net_size(cube["size"])
        if x + nw > TEX:
            x, y, shelf = 0, y + shelf, 0
        if y + nh > TEX:
            raise SystemExit("texture full")
        cube["uv"] = [x, y]
        x += nw
        shelf = max(shelf, nh)


pack()


def geometry():
    bones = []
    for b in BONES:
        out = {"name": b["name"]}
        if b["parent"]:
            out["parent"] = b["parent"]
        out["pivot"] = b["pivot"]
        if b["rotation"]:
            out["rotation"] = b["rotation"]
        if b["cubes"]:
            out["cubes"] = []
            for cube in b["cubes"]:
                cj = {"origin": cube["origin"], "size": cube["size"], "uv": cube["uv"]}
                if cube["inflate"]:
                    cj["inflate"] = cube["inflate"]
                out["cubes"].append(cj)
        bones.append(out)
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.ice_lich", "texture_width": TEX, "texture_height": TEX,
                        "visible_bounds_width": 5, "visible_bounds_height": 4.5, "visible_bounds_offset": [0, 2, 0]},
        "bones": bones}]}


# --- Animations ------------------------------------------------------------------------------------
# Channels are {time: [x, y, z]}; every keyframe is written catmullrom for smooth motion. Rotations are
# degrees added to the rest pose. Negative X swings a hanging limb forward (and tips an upright part
# back); positive Z swings a hanging part toward the lich's right (-X).

def ch(**frames):
    return frames


def kf(frames):
    return {f"{t:.4g}" if t % 1 else f"{t:.1f}": {"post": v, "lerp_mode": "catmullrom"} for t, v in sorted(frames.items())}


def anim(length, loop, bones):
    out = {"loop": loop, "animation_length": length, "bones": {}}
    for name, channels in bones.items():
        out["bones"][name] = {k: kf(v) for k, v in channels.items()}
    return out


def strip_sway(amp, phase, length, flare=0.0):
    """Keyframes for every hem strip: outward flare plus a travelling sway with a per-strip phase."""
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
    return anim(L, True, bones)


def cast():
    # Wind up, thrust the staff crystal at the target at 0.3s, recover by 0.75s.
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
    # Arms flung up, staff raised overhead, head thrown back, rising, hem flared: an obvious channel.
    L = 1.5
    bones = {
        "body": {"position": {0: [0, 1.5, 0], 0.75: [0, 2.5, 0], 1.5: [0, 1.5, 0]}},
        "torso": {"rotation": {0: [-8, 0, 0], 0.75: [-10, 0, 0], 1.5: [-8, 0, 0]}},
        "neck": {"rotation": {0: [-22, 0, 0], 0.75: [-26, 0, 3], 1.5: [-22, 0, 0]}},
        "jaw": {"rotation": {0: [18, 0, 0], 0.75: [24, 0, 0], 1.5: [18, 0, 0]}},
        "right_arm": {"rotation": {0: [-150, 0, -14], 0.75: [-156, 0, -18], 1.5: [-150, 0, -14]}},
        # The staff turns in the hand to stand upright overhead and slides down so the crystal stays low.
        "staff": {"rotation": {0: [150, 0, 0], 0.75: [154, 0, 0], 1.5: [150, 0, 0]},
                  "position": {0: [0, 11.6, 3.1], 1.5: [0, 11.6, 3.1]}},
        "left_arm": {"rotation": {0: [-150, 0, 24], 0.75: [-157, 0, 28], 1.5: [-150, 0, 24]}},
        "left_hand": {"rotation": {0: [-10, 0, 0], 0.75: [-20, 0, 0], 1.5: [-10, 0, 0]}},
        "left_finger_0": {"rotation": {0: [-30, 0, 18], 1.5: [-30, 0, 18]}},
        "left_finger_3": {"rotation": {0: [-30, 0, -18], 1.5: [-30, 0, -18]}},
        "hem": {"rotation": {0: [-3, 0, 0], 0.75: [3, 0, 0], 1.5: [-3, 0, 0]}},
    }
    bones.update(strip_sway(6, 1, L, flare=22))
    return anim(L, True, bones)


def beam():
    # The staff levelled at the target in both hands, body leaning into it, trembling with the strain.
    L = 1.0
    return anim(L, True, {
        "body": {"position": {0: [0, 0, -1], 0.5: [0, 0.4, -1], 1: [0, 0, -1]}},
        "torso": {"rotation": {0: [10, 0, 0], 0.25: [10.5, 0.8, 0], 0.5: [9.5, 0, 0], 0.75: [10.5, -0.8, 0], 1: [10, 0, 0]}},
        "neck": {"rotation": {0: [-10, 0, 0], 1: [-10, 0, 0]}},
        "jaw": {"rotation": {0: [16, 0, 0], 0.5: [20, 0, 0], 1: [16, 0, 0]}},
        # Right hand forward at the hip, staff levelled through it, left hand on the shaft behind it.
        "right_arm": {"rotation": {0: [-35, -20, 0], 0.5: [-36, -20, 0], 1: [-35, -20, 0]}},
        "staff": {"rotation": {0: [110, 20, 0], 0.25: [109, 21.5, 0], 0.5: [111, 20, 0], 0.75: [109, 18.5, 0], 1: [110, 20, 0]},
                  "position": {0: [1.7, 3.4, 4.6], 1: [1.7, 3.4, 4.6]}},
        "left_arm": {"rotation": {0: [-25, 35, 25], 0.5: [-26, 35, 25], 1: [-25, 35, 25]}},
        "left_hand": {"rotation": {0: [-20, 0, 0], 1: [-20, 0, 0]}},
        "hem": {"rotation": {0: [-6, 0, 0], 0.5: [-8, 0, 0], 1: [-6, 0, 0]}},
    })


def burst():
    # The free hand goes up, then slams down at 0.45s as ice erupts under the player, then recovers.
    return anim(1.0, False, {
        "body": {"position": {0: [0, 0, 0], 0.3: [0, 1.5, 0], 0.45: [0, -1.5, 0], 0.7: [0, -1, 0], 1: [0, 0, 0]}},
        "torso": {"rotation": {0: [0, 0, 0], 0.3: [-10, -10, 0], 0.45: [20, 8, 0], 0.7: [16, 6, 0], 1: [0, 0, 0]}},
        "neck": {"rotation": {0: [0, 0, 0], 0.3: [-8, 0, 0], 0.45: [6, 0, 0], 1: [0, 0, 0]}},
        "jaw": {"rotation": {0: [0, 0, 0], 0.45: [22, 0, 0], 0.8: [4, 0, 0], 1: [0, 0, 0]}},
        "left_arm": {"rotation": {0: [0, 0, 0], 0.3: [-165, 0, -10], 0.45: [-28, 0, 4], 0.7: [-32, 0, 4], 1: [0, 0, 0]}},
        "left_hand": {"rotation": {0: [0, 0, 0], 0.3: [-10, 0, 0], 0.45: [35, 0, 0], 0.7: [30, 0, 0], 1: [0, 0, 0]}},
        "right_arm": {"rotation": {0: [0, 0, 0], 0.3: [8, 0, -10], 0.45: [-10, 0, -6], 1: [0, 0, 0]}},
        "hem": {"rotation": {0: [0, 0, 0], 0.3: [-6, 0, 0], 0.45: [10, 0, 0], 0.7: [4, 0, 0], 1: [0, 0, 0]}},
    })


def animations():
    return {"format_version": "1.8.0", "animations": {
        "animation.ice_lich.idle": idle(),
        "animation.ice_lich.cast": cast(),
        "animation.ice_lich.summon": summon(),
        "animation.ice_lich.beam": beam(),
        "animation.ice_lich.burst": burst(),
    }}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=1)
        f.write("\n")


if __name__ == "__main__":
    write(GEO_OUT, geometry())
    write(ANIM_OUT, animations())
    print("ice lich model and animations written")
