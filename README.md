# Wildspell Mobs

Cold-cave monsters for YUNG's Cave Biomes' Frosted Caves, NeoForge 1.21.1. MIT licensed.
The player-facing description is [publish/DESCRIPTION.md](publish/DESCRIPTION.md); Modrinth project
`wildspell-mobs` (id `EAr8sZ9J`).

- **Rime Skull**: floating skull that circles, gnashes, lunges and spits frost shards; three subtle
  variants; spawns in the Frosted Caves.
- **Frozen Zombie**: zombies that linger in the Frosted Caves freeze into one. Laboured stop-start
  gait; variants: whole, one-armed, and ice-bound (sunk into the ice block it froze on, throws
  snowballs, freed if the ice breaks).
- **Spawn balance**: thins creepers and other monsters in caves (no skylight), with a local cap.
  Tunable in `config/wildspellmobs-common.toml`.

## Layout

- `src/main/java/com/wildspell/mobs/`: registration (`WildspellMobs`), cave spawn balancing
  (`SpawnBalance`), zombie freezing (`ZombieFreezing`), gametests (`WildspellMobsTests`).
  - `entity/`: `RimeSkull`, `FrozenZombie`, `FrostShard` (skull spit and zombie snowballs).
  - `client/`: models, renderers, the frost mote particle.
- `src/main/resources/`: textures, sounds, lang, loot tables, biome modifiers and biome tags.
- `tools/`: generators for the art and sound. Edit these, not the PNG/OGG files directly.
  - `paint_textures.py` (needs Pillow): every texture, including the skull variants and the Frozen
    Zombie skin, painted from scratch.
  - `make_sounds.py` (needs numpy and soundfile): the Frozen Zombie's crunch and shatter sounds, as
    mono Ogg Vorbis (Minecraft only fades mono sounds with distance).
  - `make_arena.py`: the empty gametest arena structure.
- `publish/`: Modrinth/CurseForge page text, icon and gallery image.

## Build

Needs JDK 21 (`JAVA_HOME`).

    ./gradlew build          # jar in build/libs/

## Test

Gametests cover spawning, AI, variants and the spawn balance. They need YUNG's Cave Biomes and its
dependencies at dev runtime: put `YungsCaveBiomes`, `YungsApi`, `geckolib` and `TerraBlender` jars
for NeoForge 1.21.1 in `libs/` (gitignored, never shipped).

    ./gradlew runGameTestServer

`./gradlew runShowcase` opens the dev client straight into `run/saves/showcase`, a staged world for
looking at the mobs. The world is local-only (under `run/`, gitignored).

## Release

Bump `mod_version` in `gradle.properties`, build, and upload the jar as a new version of the Modrinth
project (NeoForge, 1.21.1, YUNG's Cave Biomes as an optional dependency).
