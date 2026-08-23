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

## v0.3.0 — "The Veil Below" (next)

The afterlife layer: gothic-comedy underworld, entered by ritual, leaking into
the overworld. Full concept: DESIGN.md Part IV. Rolling art batches (terrain,
mobs, building sets — see the style guide's art direction) continue throughout.

- [x] **Séance Circle ritual** (crafted circle + the Silver Bell as the
      un-consumed key) opening a persistent **Rift** portal to the Veil
      (one-world dimension below the deep caves) — shipped in 0.3.0
- [x] **The Veil** level: permanent-night region streaming; sub-biomes
      **Gloomfen** (green-moon marsh) and **Ashen Reach** (ash waste) —
      shipped in 0.3.0 with fen flora, ash bones and the Gloom Shade
- [ ] Structures: **The Model Town** (doll-scale streets) and the
      **Office of Eternity** (waiting-room dungeon, ticket-number humor)
- [ ] **Ashwyrm**: summoned mid-boss in the Ashen Reach + juvenile
      **Wormground** set-piece eruptions on vanilla desert islands
- [ ] NPCs: **Mortimer the Broker** (wandering trickster ghost peddler, sells
      the séance chalk) and **Vesper** (deadpan medium, quest handler)
- [ ] Quest chain **"Three Stamps for Eternity"** (Wormsign → Séance →
      Take a Number → The Third Stamp)
- [ ] **"Haunted & Homely" deco set**, usable in overworld bases: ghost
      lantern, striped fence/gate, zigzag runner, crooked gate, self-playing
      piano, model-house set (largest is enterable), ticket dispenser +
      Form 13-K, ghost-train platform pieces
- [ ] New materials: Wyrmash chitin, Cinder Pearl

## v0.4.0 — "The Living Sky"

Full vanilla-fidelity art across the sky, a dense living world, weather, and
ties to the surface. Full concept: DESIGN.md Part III; the per-biome content
lock (trees, plants, animals, enemies, ores, structures, second NPC) is
DESIGN.md **Part V**.

- [ ] **Per-biome fill (Part V lock)**: one tree family per biome with its own
      wood + plank floor (Nimbus Willow / Fulgur Pine / Prisma Birch), 2 new
      plants per biome, a new critter and a new enemy where a biome lacks one
      (Zephyr Finch + Galehound, Dew Snail + Dawnpiercer), 2 new ores
      (Fulgurite, Prismshard), and generated structures: Sky Cottage with the
      **Cloud Shepherd NPC** (shop + small journal quest), Storm Ruin with
      loot + golem guards, Aurora Shrine
- [ ] **Art overhaul** of every existing sprite to vanilla detail density
      (terrain splats → Mistsea → nodes/plants → mobs/NPCs → building set → items),
      each batch gated by the contact-sheet QA process
      (`.claude/skills/necesse-pixel-art/`) and verified on gameplay screenshots
- [ ] **Mistsea recast as a cloudsea**: puffy animated cloud deck, shore wisps,
      Mist Lilies, drifting cloud shadows
- [ ] **World density & diversity**: higher decor/node density, more sheet variants,
      and new per-biome gathering loops — Windwheat, Cloudberry, Drift Boulder,
      Nimbus Tuft, Fulgurite Spire, Charged Slate, Static Bloom, Prismshell,
      Aurora Kelp, Chimeflower; new materials (Cloudfluff, Fulgurite Glass,
      Prismshell) feeding new building/furniture pieces
- [ ] **Sky weather cycle**: Radiance, Overcast Drift, Tempest (Stormveil storm
      event with lightning + shard yield), Mist Surge — seeded, announced via chat
      line + palette/particles, vanilla level-event pattern
- [ ] **Cats as settlement companions**: after the finale, Siggi and Peanut become
      individually recruitable to the player's settlement (vanilla pet path,
      carrier item from the Warden; declining keeps them at the spire)
- [ ] **Fallen stars**: post-beacon skyfall event drops small Aetherium meteors on
      surface islands
- [ ] Mistsea **fishing loot table** (new fish + rare catches)
- [ ] Balance pass from playtest feedback

## v0.5.0 — "The Aviary"

Making the sky a place to live.

- [ ] **Aetherium armor set** (head/chest/boots; Tungsten-tier set bonus: fall-themed
      mobility perk, e.g. brief glide/slow-fall visual + speed after damage)
- [ ] Sky **structures/presets**: more ruined skystone spires with loot, small
      abandoned observatories (uses the vanilla preset generation system)
- [ ] **Settlement support**: allow claiming a Skyreach settlement flag; settler pathing
      audit around Mistsea; sky-specific settler dialogue lines
- [ ] Wandering **Skyward Trader** visiting surface settlements with sky-exclusive stock
- [ ] **Surface-ingredient requests**: repeatable Warden task trading surface goods
      for sky materials
- [ ] Sound pass: wind ambience, mob sounds (reuse-plus-pitch first, custom later)
- [ ] Journal/quest hooks expanded into a full sky questline

## v0.6.0 — "Crown of the Sky"

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
