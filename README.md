# Wildspell Mobs

Cold-cave monsters for YUNG's Cave Biomes' Frosted Caves, NeoForge 1.21.1. MIT licensed.
The player-facing description is [publish/DESCRIPTION.md](publish/DESCRIPTION.md); Modrinth project
`wildspell-mobs` (id `EAr8sZ9J`).

- **Rime Skull**: floating skull that circles, gnashes, lunges and spits frost shards; three subtle
  variants; spawns in the Frosted Caves.
- **Frozen Zombie**: zombies that linger in the Frosted Caves freeze into one. Laboured stop-start
  gait; variants: whole, one-armed, and ice-bound (sunk into the ice block it froze on, throws
  snowballs, freed if the ice breaks). Separately, most have a torn brow down to the skull with a
  glowing socket; one in three kept a whole face.
- **Ice Lich**: a floating frost-lich boss (GeckoLib model) bound to a **Frozen Phylactery** in a
  crypt generated in the Frosted Caves. Very rarely (`lichAmbushChance`, per second within 64 blocks
  of a crypt) it rises behind a player and hunts them, blinking after them when it loses sight. It
  fires frost-shard volleys, sweeps a frost beam that pillars block, channels minions (hit it to break
  the channel; at most 3), and below half health calls up telegraphed ice bursts. Struck down while
  its phylactery stands, it drops nothing: its soul flies home through the rock, glowing through
  walls, and it re-forms 20 seconds later to hunt again. Walking into the crypt lights its soul-fire
  braziers and calls it home; there the braziers burn down with its health. The phylactery can't be
  broken while any of the crypt's four **Rime Wards** stand; shattered, it leaves the lich mortal (and
  if it had no body at that moment, it takes one last form on the altar). Only a mortal lich drops
  the **Frostbound Staff** (fires frost shards).
- **Electric Eel**: an underground water creature in flooded caves anywhere in the Overworld
  (`#wildspellmobs:electric_eel_spawns`; the glow squid's spawn rules: deep, dark water). It takes a
  crevice near its spawn as its den and lies in it. Swimmers within 6 blocks of the den are hunted
  (it senses them, no line of sight needed); it also hunts fish. It winds up a discharge (1.5 s of
  crackle and brightening glow), then shocks everything in the water within 5 blocks (4 damage plus
  heavy Slowness, `wildspellmobs:eel_shock`, no knockback) except other eels, and bites in between.
  Someone on the bank within 4 blocks gets a leap and a 6-damage contact shock. Stranded, it flops
  toward water. It drops nothing but XP.
- **Frost**: every frost hit builds vanilla freezing (the shards a little at a time); like powder snow,
  any piece of leather armour keeps it off.
- **Ice Cube drops**: YUNG's Ice Cubes have no loot of their own; this gives them 0-2 Ice.
- **Spawn balance**: thins creepers and other monsters in caves (no skylight), with a local cap.
  Tunable in `config/wildspellmobs-common.toml`.
  Creepers are made super rare in the Frosted Caves (a custom `reweigh_spawns` biome modifier); with
  Creeper Overhaul installed, the only ones there are its Snowy Creepers.
- **Enchanted Ice Crystal**: YUNG's Enchanted Ice drops one when mined without Silk Touch (a global
  loot modifier), on top of its XP. Silk Touch still gives the block.

## Layout

- `src/main/java/com/wildspell/mobs/`: registration (`WildspellMobs`), cave spawn balancing
  (`SpawnBalance`), zombie freezing (`ZombieFreezing`), gametests (`WildspellMobsTests`, `ElectricEelTests`).
  - `entity/`: `RimeSkull`, `FrozenZombie`, `IceLich`, `LichWisp` (its soul in flight), `FrostShard`
    (every frost projectile), `Frost` (the shared freezing rules), `ElectricEel`.
  - `crypt/`: the crypt structure and its piece, the phylactery block and its block entity (ambushes,
    re-forming, the braziers, the wards).
  - `item/`: `FrostboundStaffItem`.
  - `client/`: models, renderers, the frost mote particle.
- `src/main/resources/`: textures, sounds, lang, loot tables, biome modifiers and biome tags.
- `tools/`: generators for the art and sound. Edit these, not the PNG/OGG files directly.
  - `paint_textures.py` (needs Pillow): every texture, including the skull variants and the Frozen
    Zombie skin, painted from scratch.
  - `make_sounds.py` (needs numpy and soundfile): the Frozen Zombie's crunch and shatter sounds, as
    mono Ogg Vorbis (Minecraft only fades mono sounds with distance).
  - `make_lich_model.py`, `paint_lich.py`, `preview_lich.py`: the Ice Lich's GeckoLib model and
    animations, its texture and glowmask, and a software preview renderer to check them without a game.
  - `paint_blocks.py`: the phylactery, Rime Ward and lich-soul textures.
  - `paint_eel.py` (needs Pillow): the Electric Eel and its glow layer (the electric organ).
  - `make_arena.py`: the empty gametest arena structure.
- `publish/`: Modrinth/CurseForge page text, icon and gallery image.

## Build

Needs JDK 21 (`JAVA_HOME`).

    ./gradlew build          # jar in build/libs/

## Test

Gametests cover the mobs' spawning, AI and variants, zombie freezing, the spawn balance, the Ice Lich
(volleys, minions, the interruptible summon, enraged bursts, re-forming at its phylactery, wards,
shattering, the crypt waking, ambushes, and the crypt built right in every orientation), frost and
leather, the staff, the new drops, and the Electric Eel (a discharge hits everything in the water
but nothing ashore, only swimmers in its territory are hunted, leaping at someone on the bank, taking
a crevice as its den). They need YUNG's Cave Biomes and its
dependencies at dev runtime: put `YungsCaveBiomes`, `YungsApi`, `geckolib` and `TerraBlender` jars
for NeoForge 1.21.1 in `libs/` (GeckoLib is also a compile dependency) (gitignored, never shipped). Add `CreeperOverhaul`, `resourcefulconfig`
and `resourcefullib` too to cover the Creeper Overhaul rules (only snowy creepers, and rarely, in the
Frosted Caves); the creeper test checks whichever setup it runs in. Adding `ftb-quests`,
`ftb-library`, `ftb-teams` and `architectury` lets the test server load the modpack's quest book
(write it into `run/config/ftbquests/quests/` with the modpack repo's `tools/make_quests.py <folder>`)
and log any quest that fails to parse.

    ./gradlew runGameTestServer

`./gradlew runShowcase` opens the dev client straight into `run/saves/showcase`, a staged world for
looking at the mobs. The world is local-only (under `run/`, gitignored).

## Release

Bump `mod_version` in `gradle.properties`, build, and upload the jar as a new version of the Modrinth
project (NeoForge, 1.21.1, GeckoLib as a required dependency, YUNG's Cave Biomes as an optional one).
