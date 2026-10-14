from pathlib import Path

from PIL import Image

from geckolib_model import Model, anim, animations, c, faces_of, loop, write

ASSETS = Path("src/main/resources/assets/wildspellmobs")
MODEL = Model("geometry.gaia_manifestation", 256, 5, 4, [0, 1.5, 0])
bone = MODEL.bone

bone("root", None, (0, 0, 0))
bone("ground_roots", "root", (0, 0, 0), cubes=[
    c((-15, 0, -7), (30, 3, 12), "bark"),
    c((-11, 1, -15), (7, 4, 20), "old"),
    c((4, 1, -14), (7, 4, 19), "old"),
    c((-19, 0, -4), (6, 2, 15), "bark"),
    c((13, 0, -7), (7, 2, 12), "bark"),
    c((-7, 0, 4), (5, 2, 12), "bark"),
    c((4, 0, 4), (4, 2, 10), "bark"),
])
bone("lap", "root", (0, 10, 0), cubes=[
    c((-10, 5, -5), (20, 10, 11), "dark"),
    c((-13, 8, -17), (8, 7, 16), "old"),
    c((5, 8, -16), (8, 7, 15), "dark"),
    c((-14, 4, -19), (12, 5, 8), "dark"),
    c((1, 4, -18), (13, 5, 8), "old"),
    c((-6, 8, -11), (12, 5, 9), "new"),
])
bone("torso", "root", (0, 15, 0), cubes=[
    c((-6, 14, -3), (12, 18, 7), "old"),
    c((-8, 27, -4), (16, 7, 8), "dark"),
    c((-7, 18, -5), (7, 12, 2), "new"),
    c((0, 16, -5), (6, 13, 2), "dark"),
    c((-8, 14, -5), (6, 5, 3), "dark"),
    c((2, 13, -5), (6, 7, 3), "old"),
    c((-2, 32, -1), (4, 5, 4), "bark"),
])
bone("head", "torso", (0, 35, 0), [3, -3, 0], [
    c((-5, 35, -4), (10, 11, 8), "old"),
    c((-4, 36, -5), (8, 8, 1), "face"),
    c((-5, 44, -4), (10, 3, 8), "dark"),
    c((-2, 47, -2), (3, 2, 4), "new"),
    c((-5, 42, -5), (3, 4, 2), "dark"),
    c((2, 43, -5), (3, 3, 2), "new"),
    c((-6, 36, -5), (2, 7, 3), "new"),
    c((4, 35, -5), (2, 8, 3), "dark"),
    c((-3, 35, -5), (6, 2, 2), "dark"),
])
for side, sign in (("left", -1), ("right", 1)):
    x = -12 if sign < 0 else 8
    bone(f"{side}_arm", "root", (sign * 9, 30, 0), [0, 0, sign * 6], [
        c((x, 19, -1), (4, 12, 4), "bark"),
        c((x - 1, 25, -2), (6, 8, 6), "dark"),
        c((x, 20, -2), (4, 7, 2), "new"),
    ])
    bone(f"{side}_forearm", "root", (sign * 11, 20, 0), [-35, 0, sign * 3], [
        c((x, 10, -2), (4, 11, 4), "bark"),
        c((x - 1, 13, -3), (6, 6, 2), "old"),
    ])
    bone(f"{side}_hand", "root", (sign * 11, 12, -9), cubes=[
        c((x - 1, 10, -13), (6, 4, 7), "old"),
        c((x, 8, -17), (1, 4, 6), "bark"),
        c((x + 2, 8, -17), (1, 4, 6), "bark"),
        c((x + 4, 7, -16), (1, 5, 5), "bark"),
    ])
    bone(f"{side}_fronds", "head", (sign * 5, 43, 2), [0, 0, sign * 5], [
        c((sign * 5 - 2, 29, 2), (4, 15, 3), "dark"),
        c((sign * 6 - 2, 25, 3), (4, 8, 2), "old"),
        c((sign * 7 - 1, 34, 1), (3, 9, 2), "new"),
    ])
bone("back_fronds", "torso", (0, 31, 4), [4, 0, 0], [
    c((-8, 17, 4), (16, 15, 3), "dark"),
    c((-7, 7, 5), (6, 12, 2), "old"),
    c((1, 9, 5), (6, 10, 2), "new"),
])
MODEL.pack()


def main():
    write(str(ASSETS / "geo/entity/gaia_manifestation.geo.json"), MODEL.geometry())
    length = 20.0
    idle = {
        "torso": {"rotation": loop(length, [(0, [0, 0, 0]), (10, [0.6, 0, 0])])},
        "head": {"rotation": loop(length, [(0, [0, 0, 0]), (8, [-1, 2, 0]), (14, [-1, 2, 0])])},
        "left_fronds": {"rotation": loop(length, [(0, [0, 0, 0]), (9, [1, 0, -1])])},
        "right_fronds": {"rotation": loop(length, [(0, [0, 0, 0]), (11, [-1, 0, 1])])},
        "back_fronds": {"rotation": loop(length, [(0, [0, 0, 0]), (12, [1.5, 0, 0])])},
    }
    write(str(ASSETS / "animations/entity/gaia_manifestation.animation.json"),
          animations("gaia_manifestation", {"idle": anim(length, True, idle)}))

    source = Image.open("tools/art/gaia-materials.png").convert("RGBA")
    w, h = source.size
    materials = {}
    for material, (x, y) in {"dark": (0, 0), "new": (1, 0), "bark": (0, 1), "old": (1, 1)}.items():
        materials[material] = source.crop((x * w // 2, y * h // 2, (x + 1) * w // 2, (y + 1) * h // 2)).resize((16, 16), Image.Resampling.NEAREST)
    texture = Image.new("RGBA", (MODEL.tex, MODEL.tex))
    face = Image.open("tools/art/gaia-face.png").convert("RGBA").resize((8, 8), Image.Resampling.BOX)
    for index, b in enumerate(MODEL.bones):
        for cube in b["cubes"]:
            tile = materials.get(cube["material"], materials["old"])
            for side, (x0, y0, fw, fh) in faces_of(cube).items():
                if cube["material"] == "face" and side == "front":
                    texture.paste(face, (x0, y0))
                    continue
                for y in range(fh):
                    for x in range(fw):
                        texture.putpixel((x0 + x, y0 + y), tile.getpixel(((x + index * 3) % 16, (y + index * 5) % 16)))
    texture.save(ASSETS / "textures/entity/gaia_manifestation.png")


if __name__ == "__main__":
    main()
