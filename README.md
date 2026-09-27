# Wildspell Mobs

Cold-cave monsters for YUNG's Cave Biomes' Frosted Caves, and a glowing moth for the Lush Caves, NeoForge 1.21.1. MIT licensed.
The player-facing description is [publish/DESCRIPTION.md](publish/DESCRIPTION.md); Modrinth project
`wildspell-mobs` (id `EAr8sZ9J`).

- **Rime Skull**: floating skull that circles, gnashes, lunges and spits frost shards; three subtle
  variants; spawns in the Frosted Caves, anywhere dark with something under it to hover over (ice
  included, where vanilla spawns no ground mob). Catches fire in sunlight, like a skeleton.
- **Frozen Zombie**: zombies that linger in the Frosted Caves freeze into one. Laboured stop-start
  gait; variants: whole, one-armed, and ice-bound (sunk into the ice block it froze on, throws
  snowballs, freed if the ice breaks). Separately, most have a torn brow down to the skull with a
  glowing socket; one in three kept a whole face. Sunlight or fire thaws one back into a plain zombie
  (which then burns in the sun); on fire it melts twice as fast. It backs away from open flame and from
  anyone holding fire (the `wildspellmobs:frightens_the_cold` item tag: torches, flint and steel...).
- **Ice Lich**: a floating frost-lich boss (GeckoLib model) bound to a **Frozen Phylactery** in a
  crypt generated in the Frosted Caves. Each crypt is dug into solid rock, with one tunnel out that
  opens onto a cave (the site is chosen from the terrain noise, and the mouth is dug through any cave
  ice). It's built after the caves' decoration, and YUNG's frost sheets are kept off its masonry. Very rarely (`lichAmbushChance`, per second within 64 blocks
  of a crypt) it rises behind a player and hunts them, blinking after them when it loses sight. It
  fires frost-shard volleys, sweeps a frost beam that pillars block, channels minions (hit it to break
  the channel; at most 3), spins its staff overhead and looses a **Frost Orb** (a slow burst of cold
  that can be struck back, like a Ghast's fireball), tosses its staff hand to hand now and then (a
  moment to hit it), and below half health calls up telegraphed ice bursts. Frozen entrails hang from
  its robe. Struck down while
  its phylactery stands, it drops nothing: its soul flies home through the rock, glowing through
  walls, and it re-forms 20 seconds later to hunt again. Walking into the crypt lights its soul-fire
  braziers and calls it home; there the braziers burn down with its health. 120 health. The phylactery
  can't be moved while any of the crypt's four **Rime Wards** stand; unwarded, mining it takes it as an
  item. Only fire destroys it (nothing else harms it, it never despawns, and lost to the void it goes
  back to its altar); while it's carried, the lich re-forms beside it and hunts whoever bears it,
  anywhere. Burned, the lich's last form rises from the flames, mortal and enraged; only that drops
  the **Frostbound Staff** (fires frost shards). Its fall breaks its hold on the caves: nearby Frozen
  Zombies and Rime Skulls crumble, snow and frost melt back from the crypt, its braziers burn as
  ordinary fire and its candles light, its hoard is left in a chest on the altar, the souls it held
  linger through the caves as glowing **Frozen Souls** (they drop when broken, and count as a soul-fire
  base: `#minecraft:soul_fire_base_blocks`, so they make soul torches, lanterns and campfires, and
  anything modded built from those), and the crypt's
  surroundings (64 blocks) become a safe zone where zombies don't freeze and Rime Skulls don't spawn.
  A lich's soul state lives in world saved data (`LichSouls`), so it follows its phylactery anywhere.
  Setting a phylactery down in the End earns the hidden challenge **Eternal Damnation**.
- **Crown Fragment** and **Soulseeker**: striking the lich down (while it's bound, as a player) leaves a
  Crown Fragment, a shard of its crown still bound to its soul. Bound in Rime Shards, an Enchanted Ice
  Crystal and Frost Lilies it makes the Soulseeker: its needle points to that lich's phylactery,
  wherever it is, and using it says how far, and whether above or below.
- **Luminous Moth**: a small, peaceful glowing moth (ambient) in the Lush Caves
  (`#wildspellmobs:luminous_moth_spawns`). It mostly sits settled on a plant
  (`#wildspellmobs:luminous_moth_perches`), any solid surface or a wall, between short jinking
  flights; anything moving within a few blocks flushes it (a sneaking player only right next to it).
  It brightens moss near it into Luminous Moss, which
  fades back once no moth has been near for a while. It follows anyone holding a lure
  (`#wildspellmobs:luminous_moth_lures`: Spore Blossom). A glass bottle catches it; released
  somewhere dark (light below 8), it keeps to that spot and lights it. Its light is real block light:
  invisible `moth_glow` blocks that remove themselves once no moth is keeping them.
- **Electric Eel**: an underground water creature in flooded caves anywhere in the Overworld
  (`#wildspellmobs:electric_eel_spawns`; the glow squid's spawn rules: deep, dark water). It takes a
  crevice near its spawn as its den and lies in it. Swimmers within 6 blocks of the den are hunted
  (it senses them, no line of sight needed), and so is anything that hurts it. It hunts fish only when
  hungry, catching one and swallowing it whole; a meal keeps it fed for 3-4.5 minutes. Against
  threats (never fish) it winds up a discharge (1.5 s of
  crackle and brightening glow), then shocks everything in the water within 5 blocks (4 damage plus
  heavy Slowness, `wildspellmobs:eel_shock`, no knockback) except other eels, and bites in between.
  Someone on the bank within 4 blocks gets a leap and a 6-damage contact shock. Stranded, it flops
  toward water. It drops nothing but XP.
- **Pegasus**: a winged horse (creature), a little bigger than a horse, of the Aether's meadows and groves
  (`#wildspellmobs:pegasus_spawns`, on `#wildspellmobs:pegasus_spawnable_on`: Aether grass; the biome
  ids are optional, so it spawns nowhere without the Aether or Deep Aether). Tamed, fed, bred and saddled
  like a horse; it also eats Aether berries (`#wildspellmobs:pegasus_treats`) as apples. Saddled and
  ridden, jump takes off and holding it climbs; forward flies where the rider looks (faster in a dive),
  and letting go glides down at 0.12 blocks/tick. It has no charged horse jump and never takes fall
  damage, and anything walking off an island edge glides. Aloft under a rider it's no-gravity, so
  servers without `allow-flight` don't kick the rider for floating a vehicle. A wild adult now and
  then (and when hurt) spirals up one turn and back down another, landing about where it rose. The
  rider's jump key is read through an access transformer (`LivingEntity.jumping`).
  Coats: a herd is all white-and-gold, all pure white or all black; any pegasus is pink with
  dusk-toned wings (indigo, purple, lavender, blues, pinks) one time in 200
  (`Pegasus.PINK_ONE_IN`), and a pink parent passes it on one time in four. It sheds tiny dust
  sparkles in its coat's colours while it flies. The wings are bird-built (humerus, forearm, hand;
  tertials, secondaries, primaries, two covert rows), spread in the air and folded along the flank
  on the ground.
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
  - `moth/`: the moth's glow block, Luminous Moss and the bottled moth; the moth itself is in `entity/`.
  - `client/`: models, renderers, the frost mote particle.
- `src/main/resources/`: textures, sounds, lang, loot tables, biome modifiers and biome tags.
- `tools/`: generators for the art and sound. Edit these, not the PNG/OGG files directly.
  - `paint_textures.py` (needs Pillow): every texture, including the skull variants and the Frozen
    Zombie skin, painted from scratch.
  - `make_sounds.py` (needs numpy and soundfile): the Frozen Zombie's crunch and shatter sounds, as
    mono Ogg Vorbis (Minecraft only fades mono sounds with distance).
  - `make_lich_model.py`, `paint_lich.py`, `preview_lich.py`: the Ice Lich's GeckoLib model and
    animations, its texture and glowmask, and a software preview renderer to check them without a game.
  - `paint_moth.py` (needs Pillow): the Luminous Moth and its glow layer, the Luminous Moss overlay
    and the bottled moth.
  - `paint_blocks.py`: the phylactery, Rime Ward and lich-soul textures.
  - `paint_eel.py` (needs Pillow): the Electric Eel and its glow layer (the electric organ).
  - `paint_pegasus.py` (needs Pillow and the gradle-unpacked 1.21.1 client jar): the Pegasus's white,
    black and pink coats, the vanilla white horse recoloured, with its feathers beside it on the sheet.
  - `make_arena.py`: the empty gametest arena structures (the small arena, and the tall sky arena flight
    tests need).
- `publish/`: Modrinth/CurseForge page text, icon and gallery image.

## Build

Needs JDK 21 (`JAVA_HOME`).

    ./gradlew build          # jar in build/libs/

## Test

Gametests cover the mobs' spawning, AI and variants, zombie freezing and thawing (sun and fire), fear of
fire, Rime Skulls burning in the sun, the spawn balance, the Ice Lich (volleys, minions, the
interruptible summon, enraged bursts, re-forming at its phylactery, wards, carrying and burning the
phylactery, the last form, the cleansing and safe zone, the crypt waking, ambushes, the Crown Fragment
and Soulseeker, and the crypt built right in every orientation), frost and leather, the staff, the
new drops, and the Luminous Moth (its light following it and clearing up after it, perching and
brightening moss, settling on walls, being flushed but not by a sneaking player, the moss fading,
following a Spore Blossom, bottling and releasing in the dark), and the Electric Eel (a discharge hits
everything in the water but nothing ashore, only swimmers in its territory are hunted, fish only when
hungry, caught and swallowed, leaping at someone on the bank, taking a crevice as its den), and the
Pegasus (gliding down unhurt, a rider climbing and gliding, the wild soar coming back to where it rose,
tamed ones staying put, spawn ground, breeding, coats saved and inherited). They need
YUNG's Cave Biomes and its
dependencies at dev runtime: put `YungsCaveBiomes`, `YungsApi`, `geckolib` and `TerraBlender` jars
for NeoForge 1.21.1 in `libs/` (gitignored, never shipped; GeckoLib is compiled against from its Maven, so a
clean checkout builds without them). Add `CreeperOverhaul`, `resourcefulconfig`
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
