# Architecture

How Stairway to Heaven hooks into Necesse 1.3.2. Everything here was verified against
the decompiled game (see `docs/research/` for the underlying notes); file references are
mod sources unless stated otherwise.

## Engine model (the 30-second version)

Since the "one world" update, Necesse's main world is a set of **infinite, region-streamed
levels** addressed by string `LevelIdentifier`s. The vertical layers are one-world
dimensions registered in `LevelIdentifier.IDENTIFIER_TO_DIMENSION`:
`surface = 0`, `cave = -1`, `deepcave = -2`. Levels are created lazily by a chain of
`WorldGenerator`s; terrain appears per region via `Level.generateRegion(Region)`; biomes
are painted per tile into a region's **biome layer**, and spawns/music/loot resolve
through `Level.getBiome(x, y)`.

This mod adds `skyreach = +1` using exactly those mechanisms — no bytecode patches, no
vanilla entries modified, additive registrations only.

**And it adds exactly one.** `docs/PLAN_ONE_PLANE.md` (2026-09-02) retired the five
other modded levels that briefly existed. Eden, Steinfeld, the Ghost Realm, Crooked
Beyond and the Veil are **bands of `skyreach2`**, picked per tile by
`worldgen/RealmDepth` from distance to the Old Warden Spire, because
`docs/WORLD_DESIGN.md` §3 asks for overlapping biome *weights* and a dimension is the
one thing that cannot overlap. `SkyTerrainPainter.describeTile` dispatches to each
realm's band painter; the region's biome layer carries the realm's own sub-biomes, so
spawn tables, music and crate loot resolve through `Level.getBiome(x, y)` exactly as
they did per-dimension.

## Lifecycle (StairwayToHeavenMod)

| Phase | What we do | Why there |
|---|---|---|
| `init()` | register dimension mapping, `LevelRegistry.registerLevel("skylevel", SkyLevel.class)`, biomes, tiles, objects, mobs, items | all game registries close immediately after mod `init()` |
| `initResources()` | load mob `GameTexture`s | client-only phase; never runs on servers |
| `postInit()` | recipes (`Recipes.registerModRecipe`), the `WorldGenerator` hook, the `skyreachstatus` command | recipe/world-generator/command registries stay open through `postInit()` |

## The dimension (level/, worldgen/)

- **`SkyRegistry`** — one facade holding every registered ID/instance (mirrors vanilla's
  `TileRegistry.dirtID` style). Populated once in `init()`, read-only afterwards.
- **`SkyLevel extends BiomeGeneratorStackLevel`** — persists like cave levels. It MUST
  expose the `(LevelIdentifier, int, int, WorldEntity)` constructor: `LevelRegistry`
  reconstructs levels reflectively through exactly that signature on save-load.
  `isCave` stays false → the sky follows the world's day/night ambient light.
  `canRain()` is false (storms are a v0.2 feature).
  - Seeding: the lazy level-creation path passes no seed, so `getWorldGenSeed()` derives
    a per-world seed from `WorldEntity.worldSeed` (persisted with the save), salted so
    the sky never mirrors another layer.
- **`WorldGenerator` hook** (in the mod entry): returns a `SkyLevel` for the `skyreach`
  identifier and nothing else; generators are consulted newest-first, vanilla's fallback
  handles the rest.
- **`RealmDepth`** — distance → `realmDepth` (0 at the spire, 1 at `DEPTH_SCALE` 6000)
  → a weighted realm pick over six overlapping trapezoid bands, plus the `distortion`
  field. The single dial for the world's size, and the only place the realm question is
  answered.
- **`SkyTerrainPainter.REALM_WATERLINE` / `waterlineAt`** — the plane has ONE island
  field and ONE coastline, but the waterline is blended across the realm weights at each
  depth, so Eden is broad and lagoon-cut (0.40), Steinfeld is nearly continuous country
  (0.34) and the Skyreach stays an archipelago (0.48). Blending on depth alone keeps it
  continuous, so no coastline steps at a realm border.
- **`RealmLanding`** — where a door onto a realm puts the player down: the middle of that
  realm's band, in the direction the door was opened from, on the first open ground. The
  realm gates and the séance rift are same-plane doors now, not level teleports.
- **`SkyNoise`** — allocation-free deterministic value noise (hash → bilinear → fBm).
  Pure functions of (seed, tileX, tileY): region borders are seamless by construction.
- **`SkyTerrainPainter.paintRegion`** — the world-gen core, mirroring
  `CaveLevel.generateRegion`'s structure:
  1. per tile: sub-biome mask (low-frequency fBm) → `region.biomeLayer.setBiomeByRegion`
  2. island mask (mid-frequency fBm, threshold) → Mistsea liquid vs. island ground
  3. ground selection (Cloudturf / Stormslate / Skystone outcrop patches)
  4. object roll (single per-tile roll, exclusive probability bands per biome)

  Two placement rules learned the hard way (both verified by the integration test):
  - **Crystal clusters are 2×1 multi-tile objects** (base + `...r` counterpart to the
    right). Both halves must be written or `Region.checkGenerationValid` removes them.
  - Right after generation the liquid height map hasn't settled, so `Level.isShore` is
    true across fresh islands; naturally-placed decor must set `canPlaceOnShore = true`
    or the same validity sweep deletes it.

## Stairway pair (objects/)

- **`SkywardStairwayObject extends LadderDownObject`** — the craftable surface half.
  Vanilla gives us: surface-only placement, portal interaction, counterpart cleanup on
  destroy, map tooltips. We only override the object entity.
- **`SkywardStairwayObjectEntity extends PortalObjectEntity`** — reimplements the vanilla
  ladder-entity flow (blocked-exit check → `teleportClientToAroundDestination` with lazy
  level creation → counterpart placement → mob clearing → ladder-use stat) with one
  change: arrival over the Mistsea forms a 3×3 **Cloudturf** landing instead of vanilla's
  dirt fill.
- **`SkySideStairwayObject extends GameObject`** — functional twin of the vanilla
  (package-private) `LadderUpObject`: auto-placed on first ascent, restricted to the
  Skyreach, descends via a vanilla `LadderUpObjectEntity` targeting the surface.
  Both directions therefore ride the standard `changeLevelCheck` netcode — no custom
  packets anywhere.

## Biomes, mobs, items

- **`SkyBiome`** base + five subclasses (Driftlands, Stormveil, Aurora Shoals, Skyway
  Passages, the Outlands) provide `MobSpawnTable`s (ticket-based, vanilla
  class), disable rain, and make tile placement over the Mistsea reclaim Cloudturf
  (`getUnderLiquidTile`).
- Mobs use vanilla AI trees only: `ConfusedCollisionPlayerChaserWandererAI` for melee
  (Zephyr Ray flies via `getFlyingHeight()`, golem walks), and the Cryo-Flake pattern
  (`CollisionShooterPlayerChaserWandererAI` + `FlyingAIMover` + `AscendedBoltProjectile`)
  for the Storm Wisp. Mob classes keep public no-arg constructors (MobRegistry
  instantiates reflectively) and hold their own static `GameTexture`s, loaded in
  `initResources()` only. Spawn-light thresholds are raised so sky mobs spawn in
  daylight, like crypt bats do in lit crypts.
- Items are vanilla classes (`MatItem`, `SwordToolItem`, `BowProjectileToolItem`);
  weapon numbers sit deliberately next to the Tungsten weapons (see DESIGN.md §5).
  Recipes land in `postInit()`; the entry recipe costs the same tungsten as the Deep
  Cave Ladder plus a cave catalyst (quartz), so the sky unlocks alongside the deep caves.

## Diagnostics & testing

- **`skyreachstatus`** (admin server command, non-cheat): forces generation around the
  origin and prints tile/biome/object statistics, expected-vs-actual object counts
  (recomputed straight from the painter's math), and placement-validity diagnostics.
- **`scripts/integration_test.sh`**: full black-box test on the official dedicated
  server — boots headless with the built jar, creates a world, runs the command through
  the server console, asserts generation output and a clean log. This test caught three
  real release blockers during development (a bad item category crashing startup, the
  multi-tile crystal rule, and the shore-sweep rule).

## Asset pipeline (tools/asset_generator/)

Deterministic Python/Pillow generators produce every PNG in vanilla sheet formats
(exact formats: `docs/research/asset-formats.md`; style rules:
`docs/assets-style-guide.md`). Same input → byte-identical output, so art changes diff
cleanly. Run `python3 tools/asset_generator/generate_assets.py` after editing.

## Extension points for future milestones

- New sub-biomes: add a `SkyBiome` subclass + a band in the painter's biome mask.
- Storm events (v0.2): mirror `DeepCaveLevel`'s Spirit-Corrupted pattern in
  `SkyLevel.serverTick`.
- Structures (v0.3): the painter already runs inside the preset-generation bracket
  (`startPresetGenerationInRegion` / `runPresetGenerationInRegion`), ready for preset
  spawning.
- Boss (v0.4): summon at a Stormveil altar object; arena via a dedicated identifier or
  an `IncursionLevel`-style instanced level.
