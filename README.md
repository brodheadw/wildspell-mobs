# Wildspell Mobs

Cold-cave monsters for YUNG's Cave Biomes' Frosted Caves, a glowing moth for the Lush Caves, and the sun and moon gods
above the Aether, NeoForge 1.21.1. MIT licensed.
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
  ice). It's built after the caves' decoration, and YUNG's frost sheets are kept off its masonry. Very rarely (`lichAmbushChance`, per second within
  `lichAmbushRange` of a crypt, 200 blocks by default) it rises behind a player and hunts them, blinking after them when it loses sight. It
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
  anywhere. Burned, the lich's last form rises from the flames, mortal and enraged (on Peaceful nothing rises and
  its hold breaks at once); only that, or a lich with no phylactery at all (a spawn egg's), drops
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
  wherever it is, and using it says how far, and whether above or below, until the phylactery burns.
  The binding is the `wildspellmobs:soulseeker` recipe type, so it holds for shift-click and the Crafter.
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
  Herds come in twos to fives, and now and then one horse leads its herd up: every untamed adult within 24
  blocks lifts off with it, climbs about 24 blocks and crosses the sky for half a minute in a loose V behind
  the leader, then comes down and lands together. A rider, a leash or taming takes a horse out of the flight.
- **Stemwalker** and **Sporeheart**: the mushroom fields' stem groves (`#wildspellmobs:has_sporehearts`: mushroom
  fields and `#c:is_mushroom`) hide Sporehearts, grown into the base of a tall stem (one chunk in three tries; the
  stem must be at least six tall and stand on soil). A heart is alive only at night and only while it sits in
  its stem (stem above, stem or soil below); alive, it raises up to two Stemwalkers out of the soil 5-12 blocks
  away, out of a hunting player's sight if it can, every 10 seconds while one is within 32 blocks. A Stemwalker is
  a gaunt, hunched stem-creature about three blocks tall whose head is its own stem, split down the face, one half longer than the other,
  over dark gills and two dim spore-pores, with red bracket fungi up one side; it stalks and strikes overhead (5 damage), and never strays far (24 blocks, 40 at most).
  While its heart lives it can't be hurt: a blow makes it shrug, and a thread of mycelium and spores runs along
  the ground back to its heart. Break the heart and its walkers crumble; at dawn they sink back into the soil.
  A spawn-egg walker has no heart and bleeds like anything else (30 health). The heart drops red mushrooms, or
  itself to Silk Touch. `tools/make_stemwalker_model.py`, `paint_stemwalker.py` and `paint_sporeheart.py`
  generate the model, animations and textures.
- **Flytrap**: a snapping plant, half block and half entity. The block (`wildspellmobs:flytrap`,
  `age` 0-2) is the leaves and stalk; its jaws are `wildspellmobs:flytrap_head` entities (GeckoLib
  model) anchored to it, which never leave their place on the plant and turn to follow whatever moves
  near them. It roots only in `#wildspellmobs:flytrap_roots_in` (`#minecraft:dirt`: grass, moss,
  mud...) and grows like a sapling (random ticks in light 9+, about ten minutes a stage, or bone meal):
  a sprout with one small head that snaps only at tiny creatures (`#wildspellmobs:flytrap_sprout_prey`:
  bees, bats, silverfish, endermites, Luminous Moths; or anything 0.7 blocks or smaller; 1.5 reach); a
  young plant a block tall with one head at about player height (3 reach); and a grown plant, its stalk
  three blocks tall (the block and two `flytrap_stem`s, which need the headroom), with a big top head
  (4 reach, 30 health) and two side heads on branches. A head lunges (0.4 s wind-up) at anything
  moving within its reach (players, the sorcerer who grew it, other mobs; never another
  `#wildspellmobs:hostile_growth`), bites (2/4/6) and holds for 1.2 s: Slowness V and a drag along the
  ground toward the jaws. It senses movement, not sight (0.15 blocks over a 4-tick check), so anything
  standing still and any sneaking player slips past; striking it counts as moving. Fire does double
  damage to a head and sets it alight for 8 s, and the plant burns like leaves. Shears used on a head
  that holds someone cut them free. Killing any head breaks the whole plant, as does taking any block
  of it: it drops a **Flytrap Sprout** (which plants a new one) and a **Trap Jaw** per grown stage. The
  heads come a tick after the plant is placed or grows, and go (without dying) when it's gone or
  changes stage, however that happens, set to air included. Natural patches of a few plants at mixed
  stages generate on jungle floors and lush cave floors (`wildspellmobs:flytrap_patch`, placed features
  `flytrap_patch_jungle` and `flytrap_patch_lush_caves`). Wildspell Magic places the block by id at
  any `age` at the edge of runaway growth (placing the grown stage builds its stem) and withers it to
  air on a reversal; `#wildspellmobs:hostile_growth` holds both blocks and the head.
- **The Sun** (`apollo` in code and ids: Apollo and Helios as one figure; the name is the one lang key
  `entity.wildspellmobs.apollo`): a
  boss nothing spawns. The sun only answers a player who has helped slay `apolloWardens` (4) Sun Spirits, the
  Aether's gold-dungeon boss and the sun's wardens: every player within 48 blocks of a Sun Spirit when it dies
  is credited (`wildspellmobs:wardens_slain` in the persisted player data) and told the count. Each second the
  server checks every player: in the god sky (`godSkyDimension`,
  `aether:the_aether` by id, no compile dependency), above `godArrivalHeight` (400, past the Aether's 256 build
  limit, so only flight gets there), with the sun within `godZenithDegrees` (30) of straight up, and their gaze
  within `godGazeDegrees` (5) of the sun's real place in the sky (from the level's time of day, which the Aether's
  own clock drives) for `godGazeSeconds` (3) checks running; a rising tone plays while the gaze holds. Then he
  descends out of the glare beside them (GeckoLib model: a gilded archaic kouros with the archaic smile, light leaking
  from cracks in the gold, beaded hair, a madder kilt with a meander border, and behind his head a radiate crown of
  twelve alternating straight and wavy rays, standing in a Greek chariot: a meander-framed madder breastwork, two
  four-spoked wheels, and a pole to a team of four gilded horses, shoulder-high to him and frozen in the archaic flying
  gallop, with manes and tails of light; the whole chariot bobs, lurches and rears with him, nothing walks or rolls on
  anything). One at a time per server; any player aloft within 96 blocks joins and
  gets his boss bar; once nobody is left aloft near him (dead, fallen below the line, gone, logged out) for 5 seconds he
  withdraws into the sun and the fight resets (he also withdraws if his chunk was saved mid-fight). He keeps himself
  between his target and the sun, so to look at him is to look at the sun. Attacks: he plucks rays from his crown and
  throws them (`wildspellmobs:solar_ray`, 5 damage and fire, deflectable; the crown visibly empties and regrows); he
  holds out a palm and a burning-glass focus crawls toward his target through the air (outfly it, or it burns); and his
  corona swells for 1.8 s and flares: anyone facing him (within 70 degrees) is burned, blinded and dazed, anyone looking
  away is untouched. Close in and a searing pulse throws you off. Faster below half health. 300 health, fire immune.
  At 15% (30% when wary) he concedes: no blow can kill him while he fights, and he offers to buy his life. The choice
  is made in the world: every participant near him holding an empty main hand for 3 s spares him (or 60 s pass without
  a blow, which he takes for mercy); striking him warns (a title, a red message, Darkness, a black ring drawn over the
  real sun for each participant, 30 s) and a second strike in the warning kills him. Sneak was rejected as the gesture
  because it dismounts a Pegasus or drops a creative flyer mid-air. **Spared:** each participant gets `apollo` in the
  string list `wildspellmobs:spared` under `Player.PERSISTED_NBT_TAG` (kept through death; offline participants get it
  at their next login) for Wildspell Magic to read; he grants nothing else, climbs back into the sun, and is wary
  next time. **Killed:** the sun goes out in that world for good (world saved data `wildspellmobs_heavens`): the
  overworld is set to night (time of day 18000) with the daylight cycle off, re-asserted every 2 s so beds and `/time`
  can't bring the day back, and his gaze never calls him again. Whoever struck him in the concession is remembered.
- **Diana** (the moon and the hunt): the same check turned over, at night with the moon near the top of the sky (any
  phase: Aether nights only exist once its Sun Spirit has fallen, and its moon turns once per three-times-longer day,
  so a full-moon rule would make her a once-in-eight-hours event). A goat horn sounds and she hunts whoever called her
  (GeckoLib model: a lean huntress in an indigo chiton under a starry veil, her head the moon itself, and a crescent
  bow). The hunt is world saved data, not the entity: fleeing doesn't end it. She follows the quarry down out of the
  sky and across the land, into other dimensions and back after a logout; when she loses them (another dimension,
  past 112 blocks, or unseen 15 s and far) she steps out through moonlight and finds their trail 5 s later, with the
  horn. She snipes from 24-40 blocks up on a vantage with a clear line: each shot is drawn for 1.5 s with a glint of
  moonlight where she draws (long-distance particles and a chime, so it shows from far off), aimed where the quarry
  stands when she looses, so a watchful player can sidestep or break line of sight; with no line she holds the draw
  1.5 s more, then relaxes. Moonlit arrows (`wildspellmobs:moon_arrow`) fly straight and fast (no gravity, about 8
  damage). Cover is the refuge: with no vantage on her quarry she closes on where she last saw them, and up close slashes
  with the bow's horn and leaps back, which is the moment to turn the hunt. Hurt past 40% her shots come in threes. She
  can't be killed: at a quarter health she yields, kneels and leaves. Her face is her health: the moon on her head wanes
  from full to new as she's hurt (five textures). The boss bar is the night left until dawn in the sky she was called
  in. Surviving to that dawn, or bringing her low, records `diana` in the same `spared` list (pending for anyone
  offline); a quarry who dies is caught and leaves the hunt unmarked. If the sun was killed in that world she grieves:
  a blood-moon face and a mourning chiton, shorter draws, other words, and anyone who struck the sun down is not released
  at dawn: only bringing her low ends their hunt. Struck by a player not yet hunted, she hunts them too.
- **Frost**: every frost hit builds vanilla freezing (the shards a little at a time); like powder snow,
  any piece of leather armour keeps it off.
- **Ice Cube drops**: YUNG's Ice Cubes have no loot of their own; this gives them 0-2 Ice.
- **Spawn balance**: thins creepers and other monsters in caves (no skylight), with a local cap.
  Tunable in `config/wildspellmobs-common.toml`.
  Creepers are made super rare in the Frosted Caves (a custom `reweigh_spawns` biome modifier); with
  Creeper Overhaul installed, the only ones there are its Snowy Creepers.
- **Enchanted Ice Crystal**: YUNG's Enchanted Ice drops one when mined without Silk Touch (a global
  loot modifier), on top of its XP. Silk Touch still gives the block.
- **The magical field**: with [Fundamental Magic](https://github.com/brodheadw/fundamental-magic)
  installed, a lich's crypt feeds its field by data alone (no code dependency): the Frozen Phylactery
  (0.5), its Rime Wards (0.3) and freed Frozen Souls (0.2) are essence sources leaning still, so a
  crypt reads as a strong, unsteady still field to Wildspell Magic's field-sight and Sighting Frame.
  `data/fundamentalmagic/tags/block/essence_sources.json` and `data_maps/block/essence_strength.json`.
  Without the engine these files are ignored (one warning line in the log).

## Layout

- `src/main/java/com/wildspell/mobs/`: registration (`WildspellMobs`), cave spawn balancing
  (`SpawnBalance`), zombie freezing (`ZombieFreezing`), gametests (`WildspellMobsTests`, `ElectricEelTests`).
  - `entity/`: `RimeSkull`, `FrozenZombie`, `IceLich`, `LichWisp` (its soul in flight), `FrostShard`
    (every frost projectile), `Frost` (the shared freezing rules), `ElectricEel`, `FlytrapHead`,
    `ColdEffects` (spot searches and shared effects), `ThrustMoveControl` (the flyers' steering),
    `MotionSense` (what moved, for the moth and the flytrap), `Stemwalker`.
  - `crypt/`: the crypt structure and its piece, the phylactery block and its block entity (ambushes,
    re-forming, the braziers, the wards).
  - `item/`: `FrostboundStaffItem`, `SoulseekerItem` and its recipe type.
  - `moth/`: the moth's glow block, Luminous Moss and the bottled moth; the moth itself is in `entity/`.
  - `flytrap/`: the flytrap's block, its stem and its world-generation patch; its heads are in `entity/`.
  - `gods/`: the sun and moon gods' arrival (`Gaze`: where the sun and moon stand, who is gazing) and their world
    state (`Heavens`: the slain sun and holding the night, the spared marks, Diana's hunt); `Apollo`, `Diana`,
    `SolarRay` and `MoonArrow` are in `entity/`.
  - `grove/`: the Sporeheart block, its block entity (waking at night, raising and felling its walkers) and its
    world-generation feature (the base of a tall grove stem); the Stemwalker is in `entity/`.
  - `client/`: models, renderers, the frost mote particle.
- `src/main/resources/`: textures, sounds, lang, loot tables, biome modifiers and biome tags.
- `tools/`: generators for the art and sound. Edit these, not the PNG/OGG files directly.
  - `geckolib_model.py` and `painting.py`: the shared GeckoLib model builder (bones, box-UV packing,
    keyframes, texel walk) and pixel helpers the model and paint scripts use.
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
  - `make_flytrap_model.py`, `paint_flytrap.py` (needs Pillow): the Flytrap head's GeckoLib model and
    animations and the plant's block models and blockstates; the head, leaf and stalk textures and the
    Flytrap Sprout and Trap Jaw items.
  - `make_apollo_model.py`, `paint_apollo.py` (needs Pillow): the Sun's GeckoLib model and animations (him, his chariot
    and team), his texture and
    glowmask (GeckoLib's glow layer takes each texel's colour from the base texture and its opacity from the mask, so
    the corona's haze is a translucent glow over nothing), and the solar ray.
  - `make_diana_model.py`, `paint_diana.py` (needs Pillow): Diana's GeckoLib model and animations, her ten textures
    (five moon phases, plain and grieving; the moon is lit per texel from its sphere normal) and the moonlit arrow.
  - `make_stemwalker_model.py`, `paint_stemwalker.py`, `paint_sporeheart.py` (needs Pillow and numpy): the
    Stemwalker's GeckoLib model and animations, its texture and glowmask (the pores), and the Sporeheart's
    dormant, alive and top textures.
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
hungry, caught and swallowed, leaping at someone on the bank, taking a crevice as its den, and a
discharge ending cleanly whatever the tick parity), and the
Pegasus (gliding down unhurt, a rider climbing and gliding, the wild soar coming back to where it rose,
tamed ones staying put, spawn ground, breeding, coats saved and inherited), and the Flytrap (growing
through its stages with the right heads and reach, placed at any stage and withered to air, a sprout
biting a moth but not a player, a head biting and holding a moving mob, sneaking or still players and
still mobs slipping past, shears cutting a victim free, double fire damage, heads keeping their place,
killing a head or breaking any part breaking the whole plant with its drops, rooting only in grass, moss
or mud, and its patches generating in jungles and lush caves with their heads), and the gods (where the sun and moon
stand and only a gaze from the god sky's heights counting, Apollo conceding instead of dying, sparing him marking the
player and leaving him wary, the warning blow and the killing blow putting out the sun and the night holding, Diana
yielding instead of dying and the hunt being recorded, dawn releasing the quarry but not the sun's killers, the glint
showing for the whole draw and cover holding the shot, her grief in a sunless world, and the sun answering only
after its wardens fall), the Pegasus herd taking wing in formation, and the Stemwalker and Sporeheart (the heart
waking only at night in its stem, a bound walker shrugging off blows until its heart breaks, the heart raising a
walker on soil near its quarry, dawn sinking the bound, an unbound walker bleeding, and the heart taking only a
tall stem's base). They need
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
project (NeoForge, 1.21.1, GeckoLib as a required dependency, YUNG's Cave Biomes and Fundamental Magic as optional ones).
