import json
from pathlib import Path

from PIL import Image

from geckolib_model import faces_of

ASSETS = Path("src/main/resources/assets/wildspellmobs")
geometry = ASSETS / "geo/entity/gaia_manifestation.geo.json"
assert geometry.exists(), "Missing leaf manifestation model"
model = json.loads(geometry.read_text())["minecraft:geometry"][0]
texture = Image.open(ASSETS / "textures/entity/gaia_manifestation.png").convert("RGBA")
width, height = texture.size
assert (width, height) == (model["description"]["texture_width"], model["description"]["texture_height"])
bones = {bone["name"]: bone for bone in model["bones"]}
assert len(bones) == len(model["bones"]), "Duplicate bone"
assert all(bone.get("parent", "root") in bones for bone in bones.values()), "Unresolved bone parent"
for bone in bones.values():
    for cube in bone.get("cubes", []):
        assert all(size > 0 for size in cube["size"]), "Degenerate cube"
        for x, y, w, h in faces_of(cube).values():
            assert 0 <= x < x + w <= width and 0 <= y < y + h <= height, "UV outside texture"
            assert texture.crop((x, y, x + w, y + h)).getchannel("A").getextrema() == (255, 255), "Missing cube-face texels"
clips = json.loads((ASSETS / "animations/entity/gaia_manifestation.animation.json").read_text())["animations"]
idle = clips["animation.gaia_manifestation.idle"]
assert idle["loop"] is True
assert "root" not in idle["bones"] and "ground_roots" not in idle["bones"], "Ground attachments must stay planted"
assert idle["bones"].keys() <= bones.keys(), "Animation targets absent bone"
for channels in idle["bones"].values():
    for frames in channels.values():
        assert frames["0.0"]["post"] == frames[f'{idle["animation_length"]:.1f}']["post"], "Idle loop discontinuity"
print(f'Gaia assets valid: {len(bones)} bones, {sum(len(b.get("cubes", [])) for b in bones.values())} cubes, {width}x{height} texture')
