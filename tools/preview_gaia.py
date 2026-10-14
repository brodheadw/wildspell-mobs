import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

from preview_lich import pose_at, render, world_faces

ASSETS = Path("src/main/resources/assets/wildspellmobs")
OUT = Path("../../outputs")
geo = json.loads((ASSETS / "geo/entity/gaia_manifestation.geo.json").read_text())["minecraft:geometry"][0]
anims = json.loads((ASSETS / "animations/entity/gaia_manifestation.animation.json").read_text())
tex = np.array(Image.open(ASSETS / "textures/entity/gaia_manifestation.png").convert("RGBA"), float)
glow = np.zeros_like(tex)
out = Image.new("RGB", (1860, 660))
for i, yaw in enumerate((0, 35, 90)):
    view = render(world_faces(geo), tex, glow, yaw, 10, size=(620, 660), cy=24, ground=True)
    ImageDraw.Draw(view).text((18, 18), ("FRONT", "THREE QUARTER", "SIDE")[i], fill=(230, 235, 230))
    out.paste(view, (i * 620, 0))
out.save(OUT / "gaia-manifestation-model.png")
frames = [render(world_faces(geo, pose_at(anims, "animation.gaia_manifestation.idle", t)), tex, glow,
                 25, 10, size=(560, 580), cy=24) for t in range(20)]
frames[0].save(OUT / "gaia-manifestation-idle.gif", save_all=True, append_images=frames[1:], duration=1000, loop=0)
print("Rendered actual model and texture; animation preview uses linear keyframe interpolation")
