# Roadmap — Stairway to Heaven

Milestones are scoped so that every release is a complete, playable, save-compatible
increment. Versioning: `MAJOR.MINOR.PATCH`; content milestones bump MINOR.

## v0.1.0 — "First Ascent" (current)

The complete core loop.

- [x] Skyreach dimension (`+1`, infinite, seeded, persistent) with region generation
- [x] Stairway to Heaven / return stairway object pair (vanilla ladder netcode)
- [x] 3 sub-biomes painted into the biome layer: Driftlands, Stormveil, Aurora Shoals
- [x] Tiles: Cloudturf, Skystone, Stormslate, Mistsea (liquid)
- [x] Objects: Skystone Rock, Aetherium Rock, Storm Crystal, Aurora Bloom, Skyreeds
- [x] Enemies: Zephyr Ray, Storm Wisp, Skystone Golem (biome spawn tables)
- [x] Items: Skystone, Aetherium Ore/Bar, Storm Shard, Windsilk, Aurora Petal,
      Tempest Edge (sword), Galehowl (bow)
- [x] Recipes & loot tables, English + German localization
- [x] Reproducible pixel-art asset pipeline (`tools/asset_generator/`)
- [x] Headless dedicated-server integration test (`scripts/`)

## v0.2.0 — "The Warden's Call" (released)

Story, quests, and a reason to build. The Skyreach gets a resident.

- [x] **Render-correctness pass**: terrain tiles on the real `_splat` autotile format,
      ore overlay in the correct variant-strip format, Mistsea liquid splats
- [x] **The Warden's Spire**: unique ruined tower stamped once per world in the
      Driftlands (deterministic, save-persistent placement)
- [x] **The Sky Warden** NPC: interact-driven dialogue, 4-stage quest chain
      (find him → rekindle the beacon → bring the cats home → forge the anchor),
      server-authoritative item turn-ins, journal hint on first ascent
- [x] **Spire cats Siggi & Peanut**: unique friendly critters hidden in the Stormveil /
      Aurora Shoals, brought home with Cloudpuff Treats, live at the spire afterwards
- [x] **"Nightfell & Skylight" building set**: Skystone Brick wall + door, Nightfell
      wall, Checkered Marble + Gloomwood floors, Wrought Iron fence + gate, Warden's
      Candelabra, Mistglass Lantern, Gloomwillow, Raven Statue, Flickerlight Garland,
      Cat Basket, Skywatch Banner (quest-exclusive pieces stay earned)
- [x] Warden's shop (opens after stage 2) selling the building set
- [x] Spire visibly evolves with quest progress (beacon, basket, anchor)

## v0.3.0 — "Storm Season" (next)

Weather, gear depth, quality of life.

- [ ] **Aetherium armor set** (head/chest/boots; Tungsten-tier set bonus: fall-themed
      mobility perk, e.g. brief glide/slow-fall visual + speed after damage)
- [ ] **Storm events** in the Stormveil: periodic level event (lightning strikes,
      increased Storm Wisp spawns, bonus Storm Shard yield) — mirrors the deep-cave
      Spirit Corrupted event pattern
- [ ] Mistsea **fishing loot table** (new fish + rare catches)
- [ ] More decor objects (drift stones, cloud tufts), Mistsea ambience particles
- [ ] Sound pass: wind ambience, mob sounds (reuse-plus-pitch first, custom later)
- [ ] Balance pass from playtest feedback

## v0.4.0 — "The Aviary"

Making the sky a place to live.

- [ ] Sky **structures/presets**: more ruined skystone spires with loot, small
      abandoned observatories (uses the vanilla preset generation system)
- [ ] **Settlement support**: allow claiming a Skyreach settlement flag; settler pathing
      audit around Mistsea; sky-specific settler dialogue lines
- [ ] Wandering **Skyward Trader** with sky-exclusive stock
- [ ] Journal/quest hooks expanded into a full sky questline

## v0.5.0 — "Crown of the Sky"

The endgame of the sky arc.

- [ ] **Boss: the Storm Sovereign** — summoned at a Stormveil altar with a crafted item;
      arena-style fight over the Mistsea; trophy + relic drops (teased by the Warden's
      finale dialogue)
- [ ] Post-boss weapon/relic tier (bridges into vanilla incursion-era power)
- [ ] Unique boss music slot (custom track if available, curated vanilla list otherwise)

## v1.0.0 — Release

- [ ] Full localization sweep (all vanilla-supported languages where feasible)
- [ ] Steam Workshop packaging, workshop art, trailer GIFs
- [ ] Performance audit (region generation profiling, spawn-table load)
- [ ] Public modding notes: how to extend the Skyreach from other mods

## Compatibility policy

- Never modify vanilla registries' existing entries; additive registration only.
- Keep save compatibility within a MAJOR version; migrations documented in CHANGELOG.
- Track Necesse updates: re-verify against each game patch (decompiled-API notes in
  `docs/research/` record which game version each finding was verified on).
