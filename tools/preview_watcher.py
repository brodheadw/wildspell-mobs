import json
import sys

import numpy as np
from PIL import Image

from preview_lich import pose_at, render, world_faces

ASSETS = "src/main/resources/assets/wildspellmobs"


def main():
    out_dir, faces = sys.argv[1], sys.argv[2].split(",")
    poses = sys.argv[3:] or ["watch@0"]
    for face in faces:
        geo = json.load(open(f"{ASSETS}/geo/entity/watcher_{face}.geo.json"))["minecraft:geometry"][0]
        anims = json.load(open(f"{ASSETS}/animations/entity/watcher_{face}.animation.json"))
        tex = np.array(Image.open(f"{ASSETS}/textures/entity/watcher_{face}.png").convert("RGBA"), float)
        glow = np.array(Image.open(f"{ASSETS}/textures/entity/watcher_{face}_glowmask.png").convert("RGBA"), float)
        glow[..., :3] = tex[..., :3]
        for spec in poses:
            name, t = spec.split("@")
            pose = pose_at(anims, f"animation.watcher.{name}", float(t))
            mesh = world_faces(geo, pose)
            views = [render(mesh, tex, glow, yaw, 9, ground=True, size=(460, 560), cy=24) for yaw in (0, 35, 90, 180)]
            views.append(render(mesh, tex, glow, 20, 9, night=True, size=(460, 560), cy=24))
            out = Image.new("RGB", (460 * len(views), 560))
            for i, v in enumerate(views):
                out.paste(v, (i * 460, 0))
            out.save(f"{out_dir}/watcher_{face}_{name}_{t}.png")
    print("previews written to", out_dir)


if __name__ == "__main__":
    main()
