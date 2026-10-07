import math

from PIL import Image

import make_apollo_model as model
from geckolib_model import perimeter_x, texel_point, texels, wrap_angle
from painting import mix, ramp

OUT = "src/main/resources/assets/wildspellmobs/textures/entity"

GOLD = [(58, 28, 12), (96, 52, 20), (146, 92, 34), (194, 136, 52), (230, 182, 92), (250, 222, 148), (255, 244, 206)]
HAIR = [(44, 22, 10), (78, 42, 16), (120, 70, 26), (178, 118, 44), (226, 174, 86)]
MADDER = [(54, 10, 12), (88, 16, 16), (124, 24, 20), (160, 38, 26), (190, 62, 34)]
LIGHT = (255, 246, 214)
EMBER = (255, 206, 120)
CORONA = (255, 196, 104)
KEY = 0.55

skin = Image.new("RGBA", (model.TEX, model.TEX), (0, 0, 0, 0))
mask = Image.new("RGBA", (model.TEX, model.TEX), (0, 0, 0, 0))


def lit(theta, yn, face, gain=0.0):
    if face == "top":
        return 0.78 + gain
    if face == "bottom":
        return 0.16 + gain
    facing = math.cos(theta - KEY)
    return 0.40 + 0.30 * facing + 0.34 * max(0.0, facing) ** 12 - 0.16 * yn + gain


def metal(cube, face, x, y, fw, fh, gain=0.0):
    theta = wrap_angle(cube, face, x) if face not in ("top", "bottom") else 0.0
    return ramp(GOLD, lit(theta, y / max(1, fh - 1), face, gain))


CRACKS = {
    "chest": {(8, 1), (8, 2), (7, 3), (7, 4), (8, 5), (6, 5), (5, 6)},
    "face": {(6, 4), (6, 5), (5, 6)},
    "thigh": {(2, 2), (2, 3), (3, 4)},
}


def crack(material, face, x, y, cube):
    if face != "front" or material not in CRACKS:
        return False
    if material == "thigh" and cube["origin"][0] < 0:
        return False
    return (x, y) in CRACKS[material]


def m_face(face, x, y, fw, fh, cube):
    if face == "front":
        eyes = {(1, 3): 0.6, (2, 3): 1.0, (5, 3): 1.0, (6, 3): 0.6}
        if (x, y) in eyes:
            return mix(EMBER, LIGHT, eyes[(x, y)]), 255
        if y == 2 and 1 <= x <= 6 and x not in (3, 4):
            return ramp(GOLD, 0.30), 0
        if y == 3 and x in (0, 7):
            return GOLD[2], 0
        if y == 6 and x in (2, 5):
            return GOLD[1], 0
        if y == 6 and x in (3, 4):
            return EMBER, 200
        if y == 7 and x in (3, 4):
            return GOLD[2], 0
        if y == 5 and x in (2, 5):
            return GOLD[5], 0
        if y == 8:
            return ramp(GOLD, 0.62), 0
        if crack("face", face, x, y, cube):
            return LIGHT, 230
        gain = 0.08 if y in (0, 1) else 0.12 if x in (2, 5) and y in (4, 5) else 0.0
        return ramp(GOLD, 0.58 + gain - 0.03 * abs(x - 3.5)), 0
    if face in ("left", "right"):
        if y <= 6:
            return hair_px(face, x, y, 0.0), 0
        return metal(cube, face, x, y, fw, fh, -0.05), 0
    if face == "back":
        return hair_px(face, x, y, -0.1), 0
    return metal(cube, face, x, y, fw, fh), 0


def strand(x, y):
    if x % 2 == 1:
        return 0.12
    return 0.62 if y % 2 == 0 else 0.42


def hair_px(face, x, y, gain):
    base = strand(x, y) + gain - 0.02 * y
    if face == "bottom":
        base = 0.18
    return ramp(HAIR, base)


def m_nose(face, x, y, fw, fh, cube):
    if face == "bottom":
        return GOLD[1], 0
    return metal(cube, face, x, y, fw, fh, 0.06), 0


def m_fillet(face, x, y, fw, fh, cube):
    if face in ("top", "bottom"):
        return GOLD[1], 0
    if face == "front" and x in (4, 5):
        return LIGHT, 255
    return ramp(GOLD, 0.82 if x % 3 else 0.62), 0


def m_crown_hair(face, x, y, fw, fh, cube):
    if face == "top":
        ring = math.hypot(x - (fw - 1) / 2, y - (fh - 1) / 2)
        return ramp(HAIR, 0.38 + (0.34 if int(ring * 1.3 + 0.5) % 2 == 0 else 0.0)), 0
    if face == "front":
        curl = x % 2
        return ramp(HAIR, 0.78 if curl == 0 else 0.30), 0
    if face == "bottom":
        return HAIR[0], 0
    return hair_px(face, x, y, 0.0), 0


def m_back_hair(face, x, y, fw, fh, cube):
    if face in ("bottom", "front"):
        return HAIR[0], 0
    shade = strand(x, y) - 0.14 * (y / max(1, fh - 1))
    if face == "back":
        shade -= 0.04
    return ramp(HAIR, shade), 0


def m_lock(face, x, y, fw, fh, cube):
    if face in ("top", "bottom"):
        return HAIR[1], 0
    bead = y % 2 == 0
    return ramp(HAIR, (0.74 if bead else 0.34) - 0.02 * y), 0


def m_neck(face, x, y, fw, fh, cube):
    return metal(cube, face, x, y, fw, fh, -0.1 + 0.08 * (y / max(1, fh - 1))), 0


def m_chest(face, x, y, fw, fh, cube):
    if face == "front":
        if crack("chest", face, x, y, cube):
            return LIGHT, 240
        if (x, y) in ((6, 6), (6, 7)):
            return mix(EMBER, LIGHT, 0.5), 255
        if y == 6 and x != 6:
            return ramp(GOLD, 0.28), 0
        if x == 6 and y >= 7:
            return ramp(GOLD, 0.34), 0
        if y in (0, 1) and x in (2, 3, 9, 10):
            return ramp(GOLD, 0.82), 0
        swell = 0.10 if 2 <= y <= 5 and x != 6 else 0.0
        return metal(cube, face, x, y, fw, fh, swell), 0
    if face == "back":
        if x in (5, 6, 7) and y > 1:
            return ramp(GOLD, 0.30 if x == 6 else 0.42), 0
        return metal(cube, face, x, y, fw, fh, -0.04), 0
    return metal(cube, face, x, y, fw, fh), 0


def m_waist(face, x, y, fw, fh, cube):
    if face == "front":
        if x == 4:
            return ramp(GOLD, 0.30), 0
        if y == 2:
            return ramp(GOLD, 0.34), 0
        if x in (1, 7):
            return ramp(GOLD, 0.40), 0
        return ramp(GOLD, 0.62 - 0.04 * y), 0
    return metal(cube, face, x, y, fw, fh, -0.06), 0


MEANDER = (
    (1, 1, 1, 1, 1, 0),
    (0, 0, 0, 0, 1, 0),
    (1, 1, 1, 0, 1, 0),
    (1, 0, 1, 0, 1, 0),
    (1, 0, 1, 1, 1, 0),
)


def pleat(x, y, gain=0.0):
    return ramp(MADDER, 0.56 + gain + (0.14 if x % 3 == 0 else -0.06 if x % 3 == 2 else 0.0) - 0.04 * y)


def m_kilt(face, x, y, fw, fh, cube):
    if face in ("top", "bottom"):
        return MADDER[0], 0
    px = perimeter_x(cube, face, x)
    if y >= 1:
        on = MEANDER[y - 1][px % 6]
        return (ramp(GOLD, 0.72 if face == "front" else 0.6) if on else MADDER[1]), 0
    return pleat(px, y), 0


def m_kilt_hem(face, x, y, fw, fh, cube):
    if face == "top":
        return MADDER[1], 0
    if face == "bottom":
        return MADDER[0], 0
    px = perimeter_x(cube, face, x)
    if y == 0:
        return ramp(GOLD, 0.7), 0
    if y == fh - 1:
        return ramp(GOLD, 0.48), 0
    return pleat(px, y, -0.06), 0


def m_belt(face, x, y, fw, fh, cube):
    if face == "front" and x in (5, 6):
        return LIGHT, 255
    if face in ("top", "bottom"):
        return GOLD[2], 0
    return ramp(GOLD, 0.86 if y == 0 else 0.6), 0


def m_thigh(face, x, y, fw, fh, cube):
    if crack("thigh", face, x, y, cube):
        return LIGHT, 230
    if face == "front" and y == fh - 2 and x in (1, 2):
        return ramp(GOLD, 0.34), 0
    return metal(cube, face, x, y, fw, fh, 0.04), 0


def m_shin(face, x, y, fw, fh, cube):
    if face == "front" and y in (0, 1) and x in (1, 2):
        return ramp(GOLD, 0.80), 0
    return metal(cube, face, x, y, fw, fh, -0.02), 0


def m_foot(face, x, y, fw, fh, cube):
    if face == "front":
        return ramp(GOLD, 0.66 if x % 2 == 0 else 0.42), 0
    if face == "top":
        return ramp(GOLD, 0.7 - 0.05 * (fh - 1 - y)), 0
    return metal(cube, face, x, y, fw, fh, -0.08), 0


def m_upper_arm(face, x, y, fw, fh, cube):
    return metal(cube, face, x, y, fw, fh, 0.06 if y < 4 else 0.0), 0


def m_forearm(face, x, y, fw, fh, cube):
    return metal(cube, face, x, y, fw, fh, -0.02), 0


def m_fist(face, x, y, fw, fh, cube):
    if face == "front" and y == 0:
        return ramp(GOLD, 0.8 if x % 2 == 0 else 0.5), 0
    if face == "bottom":
        palm = math.hypot(x - 1.5, y - 1.5) < 1.2
        return (mix(EMBER, LIGHT, 0.4), 220) if palm else (GOLD[2], 0)
    return metal(cube, face, x, y, fw, fh, -0.04), 0


def m_nimbus(face, x, y, fw, fh, cube):
    r = math.hypot(x + 0.5 - fw / 2, y + 0.5 - fh / 2)
    if 18.8 <= r < 20.0:
        return LIGHT, 255
    if 15.0 <= r < 16.9:
        return (GOLD[5] if r < 15.6 else LIGHT), 255
    if r < 15.0:
        haze = (15.0 - r) / 15.0
        return CORONA, -int(60 + 150 * haze ** 1.2)
    return None, 0


def ray_px(face, x, y, fw, fh, wavy):
    if face not in ("front", "back"):
        return GOLD[4], 0
    t = y / max(1, fh - 1)
    center = (fw - 1) / 2
    if wavy:
        center += 0.5 * (1 if (y // 2) % 2 == 0 else -1)
    half = 0.6 + (fw / 2 - 0.6) * min(1.0, t * 1.8) ** 0.8
    off = abs(x - center)
    if off > half:
        return None, 0
    core = off < half * 0.45
    if core:
        return LIGHT, 255
    return mix(GOLD[5], EMBER, 0.4), int(150 + 90 * t)


def m_ray_long(face, x, y, fw, fh, cube):
    return ray_px(face, x, y, fw, fh, False)


def m_ray_short(face, x, y, fw, fh, cube):
    return ray_px(face, x, y, fw, fh, True)




# The chariot: gilded like him, the breastwork a madder panel in a meander frame, the wheels four-spoked.
def m_car_floor(face, x, y, fw, fh, cube):
    if face == "top":
        return ramp(GOLD, 0.44 + (0.06 if (x + y) % 2 == 0 else 0.0)), 0
    return metal(cube, face, x, y, fw, fh, -0.1), 0


def m_car_rail(face, x, y, fw, fh, cube):
    if face in ("front", "back"):
        if y == 0 or x in (0, fw - 1):
            return ramp(GOLD, 0.84 if face == "front" else 0.6), 0
        if y == fh - 1:
            return ramp(GOLD, 0.5), 0
        if y in (1, 2, 3, 4, 5):
            on = MEANDER[y - 1][x % 6]
            return (ramp(GOLD, 0.72 if face == "front" else 0.56) if on else MADDER[1]), 0
        return pleat(x, y - 6, -0.08 if face == "back" else 0.0), 0
    return metal(cube, face, x, y, fw, fh, 0.1 if face == "top" else 0.0), 0


def m_car_side(face, x, y, fw, fh, cube):
    if face in ("left", "right"):
        if y == 0 or x == fw - 1:
            return ramp(GOLD, 0.8), 0
        if y == fh - 1:
            return ramp(GOLD, 0.48), 0
        return (ramp(GOLD, 0.66) if (x + y) % 2 == 0 else MADDER[2]), 0
    return metal(cube, face, x, y, fw, fh), 0


def m_car_side_low(face, x, y, fw, fh, cube):
    if face in ("left", "right"):
        return ramp(GOLD, 0.78 if y == 0 else 0.56), 0
    return metal(cube, face, x, y, fw, fh), 0


def m_axle(face, x, y, fw, fh, cube):
    return metal(cube, face, x, y, fw, fh, -0.14), 0


def m_pole(face, x, y, fw, fh, cube):
    return metal(cube, face, x, y, fw, fh, -0.06 + (0.1 if face == "top" else 0.0)), 0


def m_yoke(face, x, y, fw, fh, cube):
    return metal(cube, face, x, y, fw, fh, 0.02), 0


def m_wheel(face, x, y, fw, fh, cube):
    if face not in ("left", "right"):
        return ramp(GOLD, 0.5), 0
    cx = x + 0.5 - fw / 2
    cy = y + 0.5 - fh / 2
    r = math.hypot(cx, cy)
    if r >= fw / 2:
        return None, 0
    if r >= fw / 2 - 1.6:
        return ramp(GOLD, 0.74 if cy < 0 else 0.5), 0
    if r < 1.6:
        return mix(EMBER, LIGHT, 0.5), 220
    if abs(cx) < 0.75 or abs(cy) < 0.75:
        return ramp(GOLD, 0.62 if abs(cy) < 0.75 else 0.56), 0
    return None, 0


# The team: four gilded horses with the same light in their seams, manes and tails of light, ember eyes.
HORSE_CRACKS = {(6, 2), (7, 3), (7, 4), (8, 5), (15, 3), (16, 4), (16, 5)}


def m_horse_body(face, x, y, fw, fh, cube):
    if face in ("left", "right") and (x, y) in HORSE_CRACKS:
        return LIGHT, 220
    if face == "top":
        return ramp(GOLD, 0.74 - 0.03 * abs(x - fw / 2 + 0.5)), 0
    return metal(cube, face, x, y, fw, fh, 0.02 if y < 2 else -0.02), 0


def m_horse_neck(face, x, y, fw, fh, cube):
    if face in ("left", "right") and (x, y) in {(2, 4), (3, 5), (3, 9)}:
        return LIGHT, 220
    return metal(cube, face, x, y, fw, fh, 0.03), 0


def m_horse_mane(face, x, y, fw, fh, cube):
    if face in ("top", "bottom"):
        return GOLD[5], 0
    return (LIGHT if y % 2 == 0 else mix(GOLD[5], EMBER, 0.4)), 200 + (0 if y % 2 else 55)


def m_horse_head(face, x, y, fw, fh, cube):
    if face in ("left", "right"):
        z0 = cube["origin"][2]
        depth = texel_point(cube, face, x, y)[2] - z0
        if y == 1 and fw - 4 <= depth < fw - 2:
            return mix(EMBER, LIGHT, 0.8), 255
        if y >= fh - 2 and depth < 5:
            return ramp(GOLD, 0.42), 0
    if face == "front":
        return ramp(GOLD, 0.52 if y >= fh - 2 else 0.66), 0
    return metal(cube, face, x, y, fw, fh, 0.04), 0


def m_horse_ear(face, x, y, fw, fh, cube):
    return metal(cube, face, x, y, fw, fh, 0.05), 0


def m_horse_leg(face, x, y, fw, fh, cube):
    if y == fh - 1:
        return ramp(GOLD, 0.34), 0
    return metal(cube, face, x, y, fw, fh, -0.03), 0


def m_horse_tail(face, x, y, fw, fh, cube):
    if face in ("front", "back"):
        return GOLD[5], 0
    return (LIGHT if x % 2 == 0 else mix(GOLD[5], EMBER, 0.5)), 190 + (60 if x % 2 == 0 else 0)


MATERIALS = {name[2:]: fn for name, fn in globals().items() if name.startswith("m_")}


def paint_body():
    for cube, face, x, y, fw, fh, at in texels(model.BONES):
        px, glow = MATERIALS[cube["material"]](face, x, y, fw, fh, cube)
        if px is None:
            continue
        skin.putpixel(at, tuple(px) + (0 if glow < 0 else 255,))
        if glow:
            mask.putpixel(at, (255, 255, 255, abs(glow)))
    skin.save(f"{OUT}/apollo.png")
    mask.save(f"{OUT}/apollo_glowmask.png")


def paint_ray():
    img = Image.new("RGBA", (32, 8), (0, 0, 0, 0))
    for x in range(32):
        t = x / 31
        for y in range(8):
            off = abs(y - 3.5) / 3.5
            width = 0.35 + 0.65 * min(1.0, t * 1.6) if t < 0.9 else 1.0 - (t - 0.9) * 6
            if off > width:
                continue
            core = off < width * 0.4
            color = mix(LIGHT, (255, 255, 255), 0.5) if core else mix(EMBER, GOLD[5], t)
            alpha = int(255 * min(1.0, 0.15 + t * 1.3) * (1.0 if core else 0.75))
            img.putpixel((x, y), color + (alpha,))
    img.save(f"{OUT}/solar_ray.png")


if __name__ == "__main__":
    paint_body()
    paint_ray()
    print("apollo textures written")
