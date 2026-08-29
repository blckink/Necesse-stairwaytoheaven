# Current state

Short, current, and rewritten as things change. History belongs in
`CHANGELOG.md` and `docs/PLAYTEST_LOG.md`, not here.

**Version:** 0.6.0 · **Game:** Necesse 1.3.2 · **Branch:** `master`
**Branch:** `master`
**Updated:** 2026-08-30, v0.6.0 released

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
quest; paying 30,000 coins recruits him, which places a real `HumanShop`
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
  `recruit check: skywarden settler=WardenSettler price=coinx30000` and
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
- `python3 tools/rotation_variety_audit.py` reports 123 rotation/state
  comparisons in which no cell the engine reads separately repeats another's
  picture. It caught the Skywatch Banner, whose four `PaintingObject` rows
  were one cell pasted four times — the sheet the player meant by "lässt
  sich nicht ausrichten". `tools/rotation_preview.py` renders every one of
  those cells where the engine puts it into `build/qa/rotations/`.
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

## Sky Arsenal (content/arsenal) — IMPLEMENTED, awaiting player confirmation

The mod shipped two weapons for four releases (`tempestedge`, `galehowl`).
`stairwaytoheaven/arsenal/` adds five more, one per play style, all crafted at
the Tungsten Workstation out of mod materials, and four enemies that drop what
they are made of.

- **Skyreave** (glaive, sweeps a circle) · **Thunderhead** (greatbow, charge
  scaled) · **Prismcaller** (staff + `prismbolt` projectile) · **Skywatch
  Whistle** (summons the Watch Mote) · **Stormdisc** (returning thrown ring,
  three at a time). Each is calibrated against the vanilla weapon of the SAME
  class at the deep-cave tier — quartz glaive, tungsten greatbow, quartz staff,
  cryo staff, tungsten boomerang — named in its class comment.
- **Rime Sentry** (Stormveil + Skyway) · **Aurora Flake** (Aurora Shoals +
  Stormveil) · **Fen Wraith** (Gloomfen + Ashen Reach) · **Cinder Cantor**
  (Ashen Reach + Gloomfen). Each subclasses the vanilla mob whose behaviour it
  wants and wears that mob's own sheet from `MobRegistry.Textures` — no new mob
  art, only a bestiary icon each. All four use `SkySpawnRules.daylightSpawn`
  and the integration test now asserts their accepted lit/dark counts.
- Gates: `./gradlew build`, all five audits and `scripts/integration_test.sh`
  pass; the generator still reproduces every earlier PNG byte-identically.
- **Nobody has swung any of it in the real client.**

## Item polish (content/itempolish) — IMPLEMENTED, not player-confirmed

The player's report: "Aurorablatt usw steht nicht unter itemname in Inventar
etc was es ist.. also Nahrung, Mineral, erz usw.. und es muss in richtige
Kategorie einsortiert sein ... und wir brauchen sinnvolle Sachen die man daraus
herstellen kann wie Accessoires, Rüstungen".

**Every material now says what it is.** `stairwaytoheaven/items/SkyMatItem`
appends one line from `itemtooltip.<stringID>tip`, each opening with the KIND —
Mineral, Ore, Metal bar, Log, Cloth, Food, Mob drop, Quest key. 33 items across
7 item classes carry one in both locales, and `tools/locale_audit.py` fails if
one of them loses it (it finds the described classes by looking for the
`ItemDescription` call, not from a hand-kept list).

**Four items were in the wrong bin, and the bin is what the chest sort reads**
(`Item.compareTo` → `Inventory.sortItems`; settlement storage does not read
categories at all — see TECHNICAL_LEARNINGS): `aurorapetal`
minerals → **materials/flowers** (where vanilla files every picked flower),
`skyweave` mobdrops → **materials** (it comes off a loom, not off a mob),
`cloudpufftreat` mobdrops → **materials** (it is crafted), `silverbell`
minerals → **misc/questitems** (vanilla's own bin for a quest key).

**Stormsteel is no longer a dead end.** It was the Aether Forge's headline
product and nothing in the game consumed it. Four consumers now: the
**Stormsteel set** — helm 25 / cuirass 26 / greaves 16 armour at enchant cost
1300, `Item.Rarity.UNCOMMON`, calibrated against vanilla's tungsten set
(24/25/15, 1300, UNCOMMON) and deliberately under glacial (24/24/16, 1450), at
the Tungsten Anvil beside tungsten's own armour — and the **Stormsteel
Vambrace**. A `SimpleSetBonusBuff` gives the set +15 max resilience and +5%
movement speed, under `GlacialHelmetBonusBuff`'s +20 / +20%.

**Three accessories**, real `TrinketItem`s on `SimpleTrinketBuff`s with no
tooltip key, so the ENGINE prints the numbers: Stormsteel Vambrace
(resilience gain +50% = vanilla `vambrace`, plus max resilience +25 = half of
`chainshirt`), Aurora Locket (+30 max health = 60% of `frozenheart`, +0.5
combat regen = `regenpendant`), Zephyr Harness (+10% speed = `trackerboot`,
+30% stamina = 60% of `zephyrcharm`). All three at the Tungsten Workstation,
where vanilla puts `manica`, `lifependant` and `bonehilt`.

The Dew Snail, the mod's other dead end, now makes Cloudpuff Treats.

Art: `tools/asset_generator/gen_skygear.py` — five `player/armor` sheets and
six 32px icons, drawn on `gen_armor`'s measured human anatomy and
`gen_professions`' stormsteel ramp, QA'd on 6x contact sheets against the
vanilla piece each answers to AND composited onto a real player body.
**Nobody has worn any of it in the real client.**

## Item icons — thin-icon batch (IMPLEMENTED, not player-confirmed)

`tools/size_audit.py` is a hand-maintained mapping, so a sprite with no row is
never measured. **100 of 307 shipped PNGs had a row; 207 did not** — and among
the uncovered were 94 32x32 item icons, **47 of them below the thinnest vanilla
item icon in the dump**. Vanilla's 32x32 icons carry 288-712 opaque px (median
440); the mod shipped `tempestedge` — one of its two original weapons — at
**45 px**, a hairline whose blade core was single stacked pixels per diagonal
step. `docs/REVIEW-2026-08-24.md` listed widening exactly that blade as art
action **#1**; it had stayed undone since.

Twelve icons redrawn through the generator, each briefed against a named
vanilla analogue and its measured mass: `flickerlightgarland` 29→379,
`tempestedge` 45→334, `veilessence` 70→402, `ghostlantern` 77→448,
`wardencandelabra` 78→456, `stormshard` 85→505, `aeronautwreck` 101→466,
`fulgurite` 101→451, `galehowl` 101→310, `glowfern` 101→655,
`withershrub` 113→500, `aurorapetal` 117→461. `player/weapons/tempestedge.png`
and `player/weapons/galehowl.png` change with them because they share the
`_tempest_blade` / `_galehowl_bow` helpers — intended, and now watched.

**The gate changed too, which is the durable half.** `size_audit.py` gained a
row per redrawn icon, so none can silently thin out again — and two real
defects in the gate itself were fixed:

- It **passed by measuring nothing.** `--vanilla` defaulted to a dev-container
  path, so on this machine **0 of 122 rows compared** and it still printed
  "0 sprite(s) flagged" and exited 0. That green tick was quoted in this file as
  verified. It now prefers the checkout's own `vanilla-sprites/`, prints how
  many rows actually compared, and **fails** when nothing was measured.
- The two held weapon sprites are deliberately **manual** rows, not ratios:
  they sit on a 32x32 canvas while every later mod weapon matches vanilla's much
  larger held sheets (`skyreave` 96x95 vs `quartzglaive` 104x88). A mass ratio
  between canvases differing 3x measures the canvas, not the drawing.

Art produced by Codex under brief (`codex exec`, see TECHNICAL_LEARNINGS);
reviewed, gated and integrated here. **Nobody has seen any of it in the real
client.**

### Follow-up: 35 icons still below the thinnest vanilla icon

Measured, uncovered by the audit, and deliberately NOT in this batch — one
coherent set per pass, per the bounded-art rule in `AGENTS.md`. Worst first:
`cloudbell` 120, `thunderbloom` 124, `skywatchtelescope` 129, `aurorabloom` 141,
`prismshard` 141, `stormcrystal` 141, `skyballoon` 146, `auroralily` 148,
`cloudberry` 149, `gloomwillow` 149, `cloudpufftreat` 157, `silverbell` 161,
`skywatchastrolabe` 166, `cinderpearl` 178, `aetheriumore` 181, `aetheriumbar`
184, `catbasket` 186, `cloudberrybush` 186, `skystone` 189, `windsilk` 198,
`starfall` 203, `skytulip` 207, `skyreeds` 210, `mistglasslantern` 213,
`charwood`/`nimbuswood`/`prismwood` 225, `staticmoss` 230, `stormscreed` 239,
`skystonerock` 245, `seraphstatue` 247, `seancecircle` 251, `windwheat` 255,
`skywatchchalice` 267, `skyparcel` 283.

`aurorabloom` (141) is the one to take first: the redrawn `aurorapetal` (461)
now sits beside it in the inventory, and the flower should not read thinner than
a petal picked off it.

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
- **A placed Cat Basket IS the cats' home, on whatever level it stands**
  (`feature/catbasket`). "ich habe beide gerade platziert und die sind weg oder
  irgendwo anders dann erschienen wo ich es nicht weiss" — the basket was a bare
  `FurnitureObject` with no connection to the cats at all, and their home was
  hard-wired to the spire tile in the Skyreach. Now `objects/CatBasketObject`
  claims the tile on `placeObject` and releases it on `onDestroyed`,
  `quest/CatHome` records it in `SkywatchWorldData` (tile **and**
  `LevelIdentifier`, because a home in a Surface town is not a fact about the
  Skyreach), and `SpireCatMob` travels to it with vanilla's `TeleportEvent` when
  it is on another level. Newest basket wins; breaking the active one sends them
  back to the spire; only cats that have actually been coaxed home move; every
  case says so in chat in both locales. Measured every integration-test run,
  including across a restart.
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
- **Beetlefreak wall rebuilt** (`art/beetlewall`). "Die Wandtexturen sind
  komplett für'n Arsch von der Beetle wall, da stimmt kein Rand, Fenster oder
  sonst was von Layout" — the supplied sheet was one continuous illustration
  painted across the 4x8 auto-tile block, so no cell met its neighbour; its
  eight door cells held lamp posts and partial arches rather than door frames;
  and the window's two views were swapped (a front-facing pane sat in the rows
  the engine draws as the wall's roof). `sheet_format_audit.py` passed on all
  of it, because that audit guards cell geometry, not whether the art tiles.
  `tools/asset_generator/gen_beetlewall.py` redraws the sheet on the layout the
  renderer actually reads, keeping the supplied art's identity (violet stone,
  swirls, cream-and-black bead trim, brass lanterns with green flame, the arch,
  magenta glass, the skull over the door). `tools/wall_render_preview.py` is
  the new gate: it ports `WallObject.addWallDrawOptions` and composes real
  scenes, so "does it tile" is a picture, not an inference. Verified against
  vanilla `stonewall` through the same port. **Not yet seen in game.**
- **Beetlefreak wall, second pass** (`art/beetlewall2`). The player found two
  more faults that every gate called clean, both compositional rather than
  geometric. (1) The doors read as hatches: the door cells' bounding boxes were
  byte-identical to `stonewall`'s, but a cream bead band ran across each leaf
  and the edge-on cells (5, 9 — the doors in every left and right wall) put a
  lantern-topped stub of masonry above the tile edge and a 3px sliver of leaf
  below it, where vanilla runs ONE leaf the full 58px and puts all the ornament
  on the crown above row 96. (2) The side-wall window still showed a
  front-facing pane: rows 0-1 are `getWindowDir == 1`, a north-south wall seen
  from ABOVE, and vanilla draws a slot cut along the wall's top that you look
  down into. Both are now redrawn to vanilla's own grammar (decoded in
  `docs/TECHNICAL_LEARNINGS.md`). `tools/wall_render_preview.py` now renders
  every scene for our sheet AND for vanilla `stonewall` and `woodwall` directly
  beneath it — a scene showing only our own sheet cannot reveal "shorter than
  vanilla" or "wrong view", which is why both faults survived. **Not yet seen
  in game.**
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
- `swh_beacon` (BeaconDeliveryQuest) is registered and never handed out — the
  beacon is lit by recruitment now, so the delivery chapter has no place in the
  chain. Either give it a place or retire it.
- Cat behaviour once home is still only "wander near the basket". Where that
  basket is is now the player's choice (see the Cat Basket entry above), but the
  cats do nothing charming or useful there yet, and nothing ties them to the
  recruited Warden specifically.
- The Cat Basket is a quest reward with no recipe, so a player has exactly one.
  If moving house is meant to be easy, it wants a craft.
- ~~`ROADMAP.md` still describes the pre-v0.5 direction.~~ Rewritten 2026-08-30:
  released milestones tabulated, Chapter 01 named as the next piece, the rest
  reordered by priority.
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

## Skywatch professions — IMPLEMENTED, not player-confirmed

Three settlement workstations a settler runs unattended, in
`stairwaytoheaven/settlement/` and registered by `SkyProfessions`, plus the
four spire furniture pieces the layout wanted.

- **Windsilk Loom** (`windsilkloom`, `CraftingStationObject`) — weaves
  `windsilk` into **Skyweave** (`skyweave`, new) and spins `windwheat` into
  windsilk at 2:1 against the hand recipe's 3:1.
- **Aether Forge** (`aetherforge`, the `ProcessingForgeObject` pattern:
  `GameObject implements SettlementWorkstationObject` over an
  `AnyLogFueledProcessingTechInventoryObjectEntity`) — burns logs to smelt
  `aetheriumore` into `aetheriumbar` at 2:1 (the vanilla forge does 3:1) and is
  the only source of **Stormsteel** (`stormsteelbar`, new).
- **Stormglass Kiln** (`stormglasskiln`, the `CheesePressObject` pattern:
  unfueled `ProcessingTechInventoryObjectEntity`) — fires `fulgurite` and
  `skystone` into **Stormglass** (`stormglass`, new).
- **No new work zone**, deliberately: a zone is a painted area for forestry /
  husbandry / fertilize, while a workstation is found by
  `SettlementStorageManager.assignWorkstation` on an `instanceof` test and its
  job is filed under vanilla's **crafting** priority. See
  `docs/TECHNICAL_LEARNINGS.md`.
- **Spire furniture** in `SkyFurnitureSet`: `skywatchbookshelf`
  (`BookshelfObject`), `skywatchcabinet` (`CabinetObject`), `skywatchclock`
  (`ClockObject`), `skywatchdisplay` (`DisplayStandObject`) — real storage,
  real clock, real display stand, all on the vanilla base classes and at the
  oak family's exact per-rotation row bands. The bookshelf and cabinet spend
  Skyweave; the clock and display stand spend Stormglass, so the professions
  have a consumer inside the mod.
- Art is generated by `tools/asset_generator/gen_professions.py`, which reuses
  the Skywatch family's drawing vocabulary from `gen_skyfurniture`.

Gates: `furniture_audit` now covers 17 pieces and knows the four new base
classes; `locale_audit` now checks `[tech]` display names; `sheet_format_audit`
now checks rotation-column bands, workstation cells, the forge's fire strip and
the kiln's lit sheet; `size_audit` carries 23 new rows;
`scripts/integration_test.sh` asserts, per station, that it is a
`SettlementWorkstationObject`, whether it is a processing inventory, and what
its Tech actually makes. Nothing here has been seen in the real client.
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
