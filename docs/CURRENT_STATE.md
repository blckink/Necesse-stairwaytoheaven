# Current state

Short, current, and rewritten as things change. History belongs in
`CHANGELOG.md` and `docs/PLAYTEST_LOG.md`, not here.

**Version:** 0.5.0 (v0.6 visual sprint unreleased) · **Game:** Necesse 1.3.2 ·
**Branch:** `master`
**Updated:** 2026-08-25, v0.6 visual production sprint

## Architecture in one screen

Two extra dimensions registered through
`LevelIdentifier.IDENTIFIER_TO_DIMENSION`: **Skyreach** (`skyreach`, +1) and
**The Veil** (`veil`, −3). Both are `BiomeGeneratorStackLevel`s that stream
regions, so they are effectively infinite and generate lazily.

The **Stairway is a portal**. Wherever it is built on the Surface it routes to
one canonical Skyreach origin computed from the world-generation seed
(`worldgen/SkyOrigin`), where the Old Warden Spire stands. Terrain radiates
from that origin: the hub is clamped to walkable land, and ore density widens
with distance band. The return gate resolves each player's own bound stairway
from `quest/SkywatchQuestData`, which persists per-player bindings.

The **Warden** is the progression NPC. Meeting him completes the find-the-spire
quest; paying 100,000 coins recruits him, which places a real `HumanShop`
settler (`mobs/WardenSettlerMob`, registered as settler type
`settlement/WardenSettler`) on the Surface at the player's stairway and hands
over the Silver Bell.

Assets are **generated, not hand-drawn**: `tools/asset_generator/` writes every
PNG in `src/main/resources/`. Editing a PNG directly is always wrong.

## Green — verified working

- Mod loads on a dedicated server; Skyreach and Veil generate; no log errors.
- World survives a server restart: spire returns at identical coordinates,
  Warden and both cats still present (asserted every test run).
- The Warden's recruit path is live per mob, not just registered:
  `/skyreachstatus` reports
  `recruit check: skywarden settler=WardenSettler price=coinx100000` and
  `recruit check: wardensettler settler=WardenSettler price=free`. Those two
  values are exactly what was null before, and null is what made vanilla's
  recruit button impossible.
- Every registered ID resolves to a display name, including the key classes
  the engine builds rather than our source writing down:
  `name check: skywarden=Test the Sky Warden | wardensettler=Test the Sky Warden`.
- Siggi and Peanut are unkillable and save-persistent by native means.
- `python3 tools/size_audit.py` reports 0 flags.
- Marble Checker floor no longer crashes clients (`ca2ddad`); `scripts/tile_sprite_check.sh`
  proves it headlessly.
- `python3 tools/locale_audit.py` reports 129 registered IDs and 42 literal
  keys named in both locales, and fails if a new registration helper appears
  that it does not know how to see through.
- `python3 tools/tile_behaviour_audit.py` reports 13 tiles (5 floors, 6
  terrain, 2 liquid) matching their declared role and 949 splat cells inside
  the bands measured off vanilla's own sheets.
- `python3 tools/sheet_format_audit.py` reports the 16 wall-sheet door cells
  at the extents the engine draws them at.
- v0.6 sprint gates (2026-08-25): generator output byte-identical on
  regeneration, `buildModJar` builds, `scripts/integration_test.sh` passes on
  this Mac against the Downloads dedicated-server install.

## v0.6 visual sprint — shipped, NOT yet player-confirmed

Everything below regenerated through the pipeline and inspected on contact
sheets (`build/qa/`, `build/sprite-gallery.html`); no human has seen it in
game yet.

- **Rock family**: 8 Skystone / 6 Veilrock variants with real geological
  characters (slab, strata, boulder domes, fracture, split, rubble, pits,
  terrace), carved irregular perimeters, base-dominant face fills, and the
  vanilla soft-alpha ground skirt instead of the old dark band.
- **Storm Shards**: complete redesign — 4 asymmetric 64px cluster formations
  of tilted, overlapping, value-alternating crystal blades on a shared rubble
  bed; deep violet planes, restrained pale edges.
- **Tree volume pass**: shared `_canopy_volume` (overlap shadows between
  lobes/tiers, one global light field, sheen demotion on the shadow side,
  trunk collar) applied to Nimbus Willow, Prismabirch, Fulgur Pine. Size and
  silhouettes untouched.
- **Cloudberry bush**: rebuilt as a dense leaf-clump dome (~30x20) with woody
  stems and sunk amber berry clusters; greener leaf ramp.
- **Warden**: storm-blue coat ramp (matches the settler's pinned livery),
  hood-down cowl behind the hair, brass collar clasp, weathered mend patches,
  cheek lines. Mob renderer and the HumanShop settler renderer untouched.
- **Spire hero kit**: beacon rebuilt as observatory machinery (sigil plinth,
  banded pillar with a snapped armature, brass yoke, faceted storm lens);
  new `skywatchtelescope` and `skywatchastrolabe` hero accents.
- **Stormveil prop families**: `stormscreed`, `skywatchrubble`,
  `chargecrystal` (lit), `withershrub` — craftable, worldgen-composable later.
- **Aurora accents**: `aurorashards` (lit), `starfall` (lit).
- **Sky oddity seeds** (registered + craftable, deliberately NOT in worldgen):
  `skyballoon`, `aeronautwreck`, `skyparcel`.

## Known issues — open

Ordered by the player's own priority. Full detail in `docs/PLAYTEST_LOG.md`.

**P1 — still open**
- Warden frequently stands facing north, so the player sees his back during
  the introduction. (Behaviour fix owned by another agent.)
- Warden's first dialogue dumps too much lore at once. (Another agent.)
- Old Warden Spire layout still reads as a small ordinary house — the asset
  kit is now strong enough (beacon, telescope, astrolabe, rubble), the layout
  itself is another agent's task.

**P1 — fixed, not yet player-confirmed**
- Rock/ore worldgen now uses a formation field (`7ef6486`).
- Aurora flora now grows in colonies (`7ef6486`).
- Galehound silhouette rebuilt (`080ea26`).
- Every registered object and tile has a display name, gated by
  `tools/locale_audit.py` (`eb76cb2`); all six tree and sapling item icons
  exist (`b90dc2a`).
- Harvest tools audited object-by-object against vanilla archetypes; flora,
  bones and wooden oddities no longer need the pickaxe (`a58e43b`, gated by
  the integration test's tool-audit assertions).
- Dewsnail is catchable with the net via the native `NetableMob` pattern
  (asserted by the integration test; not yet swung in the real client).
- The spire's cat basket exists as a real object on the tile the quest calls
  the cats' home, placed once per world including in existing saves; a coaxed
  cat is at it after a save/load round trip and stays within ~7 tiles of it
  (asserted every integration-test run).
- The Warden's Spire is a furnished 21x21 hall (`worldgen/WardenSpirePreset`),
  rebuilt to the layout the user supplied
  (`docs/references/presets/warden-tower-layout.script`): a double cloudmarble
  wall ring with a circulation corridor between them, eight doors on the axes,
  an octagonal beacon chamber left deliberately open, and four furnished corner
  rooms — refectory, council table, the Warden's quarters and an archive — off
  the corridor. Everything in it is on a vanilla furniture base class, so the
  tables count as tables, the chairs are sittable and turned to them, the bed
  is assignable to a settler and the table decorations stand on the tables.
  The player now arrives on the railed pad outside the grand door
  (`SkyOrigin.ARRIVAL_OFFSET_Y = 9`) rather than inside the building.
- The Warden's quest chain is a pure function of the world record
  (`SkyWardenMob.chapterFor`); eight reachable save states are enumerated and
  asserted to be owed a chapter (`chain check: ... no-dead-ends`). The
  cross-dimension read no longer gives up when the Skyreach happens to be
  unloaded, which is what made the earlier hand-out fix unreachable in the
  ordinary case.
- The Cloud Lamb is a coherent husbandry animal: shears for Windsilk, breeds
  true, is named Cloudlamb at every age, and eats cloudberries (a `GrainItem`
  now) as well as vanilla wheat — hand-fed or from a feeding trough. All four
  values are measured by `/skyreachstatus` and asserted by the test.
- v0.6 sprint (list above): rock variants + shadows, Storm Shards, tree
  volume, Cloudberry, Warden visuals, Spire hero kit, Stormveil/Aurora props,
  oddity seeds.
- Fence and fence gate rebuilt against the engine's own column contract
  (`FenceObject` / `FenceGateObject`, cell-by-cell against vanilla
  `ironfence`/`ironfencegate`). The old sheets were drawn to an invented
  layout, so a fence connecting north grew a horizontal rail, every vertical
  run was a 3px hairline, and the west and east runs were on each other's
  side of the tile. Both item icons redrawn (47 -> 672 and 132 -> 864 opaque
  px against vanilla's 576 and 652).
- Fence PLACEMENT: rings are 4-connected (`SkyLandscape.discRing`), road-side
  fence bands are at least `FENCE_MIN_THICKNESS` (1.6 tiles), gate wings start
  at their pillar instead of floating beside it, and a road crossing a ring
  now carries a real fence gate. Lone posts 3.9% -> 0.2%, dead ends
  26.2% -> 6.0% over the offline painter dumps for three seeds.
- The grey `skystone` ground (14.7% of all land) is no longer empty:
  `SkyTerrainPainter.screeObject` gives it a lichen-bed formation field and
  three new objects - Skystone Lichen, Cragbloom, Sky Scree - plus boulders
  and one lit biome accent. 0.032/0.044/0.099 objects per tile -> 0.304/0.352/
  0.356, against 0.311-0.384 on the vegetated grounds.
- The **Skyway Passages** are a real generated biome (`biomes/SkywayBiome`,
  `SkyTerrainPainter.BIOME_SKYWAY`), cut out of the biome field's 0.40-0.47
  band so it borders Stormveil. It carries `skywaytile` as its ground, grows
  the Sky Seraph wild in its frost form at 1 per 85 land tiles, and builds its
  roads out of Cloudmarble: balustrades the length of every passage, fence
  gates where a carriageway breaks one, piers at the gates and Seraph statues
  at the junctions and along the causeways. 14.6% of the sky's land at 0.371
  objects/tile, the densest ground in the world by a small margin (Driftlands
  0.358, Aurora 0.322, Stormveil 0.307), measured over eight seeds and
  2,197,075 natural land tiles.

**P2**
- Tree canopy volume addressed by the v0.6 pass — awaiting player judgement
  (size and silhouettes were never touched).
- Cloudberry bush rebuilt in v0.6 — awaiting player judgement.
- Aurora plant placement was addressed by colonies (`7ef6486`); the new
  shard/starfall accents await player judgement.

- The Veil's ghost lantern object sprite is thin: its item icon carries 77
  opaque px against vanilla `copperstreetlamp`'s 240. The error icon is gone,
  but the sprite itself wants more mass.

**Deferred**
- Warden's shop is empty. The building set is fully craftable at a workstation,
  so nothing is missing — but the recruited Warden currently does nothing.
- Cat home / cat bed furniture and settled cat behaviour (post-playtest).
- `swh_beacon` (BeaconDeliveryQuest) is registered and never handed out — the
  beacon is lit by recruitment now, so the delivery chapter has no place in the
  chain. Either give it a place or retire it.
- Cat behaviour once home is "sleep near the basket in the spire". The cats do
  not yet live with the recruited Warden on the Surface (design decision:
  their long-term home is with him once cat furniture exists).
- `ROADMAP.md` still describes the pre-v0.5 direction.
- Wiring the new Stormveil/Aurora prop families into `SkyTerrainPainter`
  (registered + craftable now; worldgen composition is a later, tuned pass).

## Last player-tested state

v0.5.0 build, played extensively in a real long-running Windows save on
2026-08-24. That session produced everything in `docs/PLAYTEST_LOG.md` under
that date, including the Marble Checker save-blocker. Nothing from the v0.6
sprint has been played yet.

## NOT player-verified

Do not describe any of these as working:

- everything in the v0.6 visual sprint list above
- Skystone Golem in game
- the complete Warden settlement lifecycle (recruit → move in → bed →
  happiness)
- cat progression after being brought home
- resource drops across the board
- outer-distance difficulty scaling
- travel/progression end to end
- building materials and custom floors other than Marble Checker

## The sky livestock layer (`content/livestock`, IMPLEMENTED — not player-confirmed)

Three farmable animals on their real vanilla archetypes, in
`src/main/java/stairwaytoheaven/livestock/` behind one registration class
(`SkyLivestock.register` / `registerItems` / `loadTextures`):

| animal | base | biome | product | taken with |
|---|---|---|---|---|
| Nimbus Yak | `CowMob` | Driftlands | Nimbus Milk | bucket |
| Thunderquill Fowl | `ChickenMob` | Stormveil | Storm Down (+ vanilla eggs) | shears |
| Glimmergoat | `SheepMob` | Aurora Shoals | Aurora Fleece | shears |

Nine recipes hang off the three products: Skycurd (cheese press), Cloudberry
Custard (cooking pot), Nimbus Draught (alchemy); Thunderplume Cowl (tungsten
anvil), windsilk (inventory), net (workstation); Glimmerstride Boots (tungsten
anvil), Skywatch Carpet (carpenter), Cloud Puff Treats (inventory).

**Zero new PNGs.** Every sheet and icon is a vanilla texture recoloured at load
time (`livestock/SkyPelt`), including both sexes, the young and the
sheared/plucked states; `tools/locale_audit.py` grew a check that resolves
every literal texture path against our resources or the vanilla dump.

Measured every integration-test run (`/skyreachstatus`): each animal's product,
offspring, display name at every age, mate, feed and — the thing the Cloud Lamb
still fails — `validSpawnLocation=implemented`.

**Two things this exposed rather than fixed:** the Cloud Lamb reports
`mate=NONE` and cannot actually breed (no male of its species exists, and
vanilla's ram only accepts the string `"sheep"`), and it still inherits `Mob`'s
`return false` so its Driftlands table entry is inert. Both are one small
override away; see `docs/TECHNICAL_LEARNINGS.md`.

Nothing here has been seen in a real client: the recolours, the armour on a
player body and the plucked-bird sprite are all server-invisible.
