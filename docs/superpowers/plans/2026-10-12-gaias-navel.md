# Gaia's Navel implementation plan

**Goal:** Deliver one deliberately placed root-and-grass sphere containing a persistent seated leaf manifestation, in the full Wildspell pack.

**Architecture:** Mobs owns the configured feature and passive manifestation. Existing GeckoLib model/animation conventions supply the visual. Magic and the shared field engine remain unchanged.

**Tech stack:** Minecraft 1.21.1, NeoForge, Java 21, installed GeckoLib, existing Python model tools.

**Spec:** `../../../../../outputs/gaias-shroud-concept.txt`, with the user-approved name Gaia's Navel. User approved execution on 2026-10-12.

## Contract

- Operator placement: `/place feature wildspellmobs:gaias_navel <x> <y> <z>`.
- No natural generation, spawn egg, boss behavior, communion, progression rewards or avatar powers.
- Roughly 31-block sphere, moss/grass/earth and persistent leaf cover, substantial curved roots, south entrance, walkable interior, skylight, shallow pool and north cradle.
- One `wildspellmobs:gaia_manifestation`, with grounded idle animation, no hostility or loot, and persistent position/orientation.
- Preflight placement, loaded chunks, build height and occupied blocks; failed placement does not leave a partial site.
- Existing Mobs behavior and current pack versions of Magic and the engine stay intact.
- No client launch, installed-profile changes, remote pushes, attribution or em dashes.

## Review focus

Occupied terrain and block entities must not be erased. Repeat placement and reload must not duplicate the occupant. Ordinary leaf decay must not ruin the enclosure. Peaceful mode, pushing and portal behavior must not remove or move the manifestation. The model must have valid UVs and animation bones, with a passable route to its cradle.

## Work and verification

- [x] Server implementation in isolated `work/gaia-navel-server`: `GaiaManifestation.java`, `GaiasNavelFeature.java`, `WildspellMobs.java`, configured-feature JSON, GameTests and a large test fixture.
  Test expected missing feature first, then actual placement, refusal without mutation, entity anchoring, peaceful behavior, save/reload and repeat placement. Build a complete block plan before mutation; preserve original states for a canceled insertion.
- [x] Client implementation in `work/gaias-navel`: `GaiaManifestationRenderer.java`, client registration, `make_gaia_model.py`, geometry, texture, animation and language name.
  Use the existing `Model`, cube packing and animation helpers. Independently validate UV bounds, bone references, opaque cube faces, anchored root pose and silhouette through a headless render of the actual model.
- [x] Integrate the isolated server files and run the full Mobs GameTest suite and build with Java 21. Inspect the actual generated block plan in a headless exterior/cutaway render. Obtain independent review of placement and persistence.
- [x] Release Mobs 1.11.0 in Wildspell 1.8.41. Audit archive differences against 1.8.40, boot an isolated full-pack server on a fresh world and restart its saved world. Copy the verified full `.mrpack` to Downloads and outputs. Document placement and prototype limits.

## Execution notes

Native worktree creation could not target this projectless workspace. Git worktrees were created from the packaged Mobs 1.10.3 source revision `381657a`, leaving the primary 1.9.5 checkout untouched. Mac Studio DNS is unavailable, so verification runs locally.

Final verification: all 78 Mobs GameTests and build passed. Full-pack command placement and saved-world restart preserved one occupant with the same UUID and position. Archive comparison against 1.8.40 changed only the manifest version and Mobs JAR; both delivered copies match. Software previews checked geometry, texture and layout. Client rendering remains unverified.
