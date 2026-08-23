# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com); versions follow the ROADMAP milestones.

## [0.2.3] — 2026-08-23

First real-client playtest feedback release.

### Fixed
- **The Sky Warden (and the cats) could not be talked to at all**: `Mob.canInteract`
  defaults to `false` in the engine, so the client never offered the interact
  prompt and the server dropped interact packets — the entire quest chain was
  unreachable in-game. Both NPCs now override `canInteract` for players. (Found
  only in client play; the headless test has no player and now this is documented
  as its known blind spot.)

### Changed
- **The Mistsea now looks like a rolling cloud deck** instead of flat water:
  layered puffy billows with sunlit tops and self-shadowed valleys, seamless
  8-frame drift (big masses east, detail wisps west), thinner/lighter shore band.
- **World density up ~1.6x** across all three biomes (playtests: the sky read as
  bare) — more rocks, crystals, blooms, reeds and ore. Applies to newly generated
  regions; already-explored areas keep their objects.

### Dev
- Added `.claude/skills/necesse-pixel-art/` (the distilled, verified style +
  format + QA knowledge) and a `pixel-artist` agent definition that uses it.
- DESIGN.md Part III + reworked ROADMAP v0.3: "The Living Sky" concept (full art
  overhaul, cloudsea, per-biome gathering loops, sky weather cycle, cats as
  recruitable settlement companions, fallen-star events).

## [0.2.2] — 2026-08-22

Cheat-free wayfinding — the quest is now fully navigable without any commands.

### Added
- The first-ascent hint names the compass direction toward the Warden's Spire
  ("Something flickers over the mist to the north-west…"), so finding the
  Warden needs no coordinates. The spire is now stamped during the very first
  ascent instead of on the level's next tick, making the pointer exact.
- The Warden points toward each runaway cat: the cat-quest briefing gains a
  direction line, and his reminder line now names the bearing to Siggi and
  Peanut instead of just their biomes.

## [0.2.1] — 2026-08-22

### Added
- `skyreachstatus` now prints a **locator** when run by a player standing in the
  Skyreach: your tile position plus distance and compass direction to the
  Warden's Spire and to each cat that hasn't been brought home yet.

### Changed
- `skyreachstatus` NPC count is labeled "(loaded regions only)" — a cat resting
  in an unloaded region is not missing, it just isn't loaded right now.

## [0.2.0] — "The Warden's Call" — 2026-08-22

The Skyreach gets a resident, a memory, and things worth building. Built and
integration-tested headless against Necesse **1.3.2** (structure stamping,
NPC spawning and quest data verified on the dedicated server).

### Added
- **The Sky Warden**: a stationary, invulnerable keeper NPC living in the
  **Warden's Spire** — a ruined 15x15 Skywatch tower stamped exactly once per
  world at a seed-deterministic spot in the Driftlands. Placement runs lazily
  on the level's first server tick, so worlds started on v0.1 get the spire
  too. Persisted via a new `skywatchquest` level-data component.
- **Quest chain "The Warden's Call"** (interact-driven, multiplayer-safe,
  server-authoritative turn-ins using the vanilla check-then-remove idiom):
  1. *A Light over the Mist* — find the Warden (chat hint on first ascent)
  2. *The Dark Lighthouse* — deliver 12 Storm Shards + 8 Windsilk; the
     spire's beacon visibly ignites
  3. *Where the Cats Wander* — craft Cloudpuff Treats and bring **Siggi**
     (black, Stormveil) and **Peanut** (white-tabby, Aurora Shoals) home;
     they stay as permanent spire residents
  4. *Anchor of the Sky* — deliver 5 Aetherium Bars + 20 Skystone; the
     island anchor appears; finale dialogue teases the Storm Sovereign
  Dialogue plays as speech bubbles (PacketMobChat) plus chat lines, fully
  localized in English and German.
- **Spire cats**: two unique invulnerable critters with HomesickCritterAI
  home tethering, hidden at seed-deterministic lairs, moved home with a
  cloud-puff when fed a treat.
- **"Nightfell & Skylight" building set**: Skystone Brick and Nightfell
  walls (each with auto-generated door + window via the vanilla wall
  system), Checkered Marble floor (world-locked pattern), Gloomwood floor,
  Wrought-iron sky fence + gate, Warden's Candelabra (streetlamp, wire-
  toggleable), Mistglass Lantern and Flickerlight Garland (wall lights with
  4 attach orientations), Gloomraven Statue, Gloomwillow dead tree, Cat
  Basket, Skywatch Banner (painting). Garland/basket/banner are
  quest-exclusive; everything else has Workstation recipes in sky materials.
- Items: Cloudpuff Treat (+ recipe), Silver Bell trophy.
- `skyreachstatus` now also reports quest state, spire integrity and NPC
  counts; the integration test asserts them.

### Changed
- **Terrain render upgrade**: Cloudturf, Skystone, Stormslate and the new
  Gloomwood floor moved to the modern `_splat` autotile atlas format
  (verified cell map; 4 full-tile variants + 17 marching-square blend
  cells); the Mistsea gained real animated 8-frame liquid splats instead of
  the flat-color fallback.
- Ore overlay sheets corrected to the vanilla Nx32x32 pattern-strip format.

## [0.1.0] — "First Ascent" — 2026-08-22

First playable release. Built and integration-tested against Necesse **1.3.2**.

### Added
- **The Skyreach**: persistent one-world sky dimension (`skyreach`, dimension `+1`),
  infinite and region-streamed like the cave layers, with a per-world seed derived from
  the world seed.
- **Stairway to Heaven** object pair: craftable surface stairway (Tungsten Workstation,
  8 Tungsten Bars + 15 Quartz — deliberately gated alongside the Deep Cave Ladder) and
  the auto-placed sky-side return stairway; both ride the vanilla ladder/portal netcode.
  Ascending onto the Mistsea forms a Cloudturf landing instead of vanilla's dirt fill.
- **Sub-biomes** painted into the biome layer: Driftlands (common), Stormveil
  (uncommon), Aurora Shoals (rare) — each with its own spawn table and object set.
- **Tiles**: Cloudturf (organic), Skystone, Stormslate, and the swimmable Mistsea
  liquid; placing tiles over the Mistsea reclaims Cloudturf.
- **Objects**: Skystone Rock, Aetherium Rock (ore), Storm Crystal and Aurora Bloom
  (2×1 glowing clusters), Sky Reeds.
- **Enemies** (Tungsten-era tuning): Zephyr Ray (220 HP melee flier), Storm Wisp
  (280 HP ranged, spark bolts), Skystone Golem (520 HP armored bruiser). All spawn in
  daylight via raised spawn-light thresholds.
- **Items**: Skystone, Aetherium Ore/Bar, Storm Shard, Windsilk, Aurora Petal;
  **Tempest Edge** (sword, 68 dmg/290 ms) and **Galehowl** (bow, 62 dmg/480 ms,
  +10% arrow velocity) as Tungsten-tier sidegrades, with recipes and loot tables.
- **Localization**: English and German.
- **`skyreachstatus`** admin command: world-gen statistics + placement diagnostics for
  bug reports and automated testing.
- **Asset pipeline**: deterministic Python/Pillow generator producing all 34 textures in
  vanilla sheet formats (`tools/asset_generator/`).
- **Headless integration test** (`scripts/integration_test.sh`): boots the official
  dedicated server with the mod, generates a world, drives `skyreachstatus` via the
  console and asserts clean logs + expected generation.
- Documentation set: DESIGN, ARCHITECTURE, ROADMAP, asset style guide, and a
  `docs/research/` knowledge base (verified engine notes: modding API, registration
  APIs, asset formats, community patterns, progression reference).

### Changed
- Build modernized to Gradle 8.10 (runs on current JDKs; still emits Java 8 bytecode via
  `options.release`). Added `NECESSE_GAME_DIR` env override and dedicated-server
  (`Server.jar`) fallback so CI/headless environments can build without a Steam install.

### Fixed (during pre-release testing, caught by the integration test)
- Startup crash: `materials.ores` is not a vanilla item category — Aetherium Ore now
  registers under `materials.ore`.
- Storm Crystals/Aurora Blooms vanished at world-gen: crystal clusters are 2×1
  multi-tile objects; the painter now writes both halves and reserves the partner tile.
- All natural sky decor vanished at world-gen: freshly generated islands report
  `isShore` everywhere until the liquid height map settles; sky decor now sets
  `canPlaceOnShore`.
- Sky Reeds refused to generate: Cloudturf is now flagged organic soil.
- Skyreach terrain was identical in every world: the lazy level-creation path passes no
  seed; the level now derives its seed from the persisted world seed.
