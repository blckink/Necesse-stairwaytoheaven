# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com); versions follow the ROADMAP milestones.

## [Unreleased] — the Veil closes — 2026-09-02

### Added
- **The Veil's fog and the Soul Exposure debuff** (`WORLD_DESIGN` §8), in the
  new `stairwaytoheaven/veil/` package. Past realm depth 0.581 — about 3486
  tiles from the spire — a permanent fog stands, and a player inside it without
  the Veil Mark stacks Soul Exposure at one second per stack: sight dims at
  1-3s, movement slows at 4-7s, life drains at 8-12s, and at 13s it deals 150
  damage a second. Stacks come back at one a second once you are out, after a
  three-second grace, so stepping over the line and back does not clear it.
  A short step in is possible; running through is not.
- `soulexposure` — the visible debuff, on `QuicksandStacksBuff`'s stacking
  shape, with English and German names, a tooltip that prints the band the
  player is actually in, and a death message that says the Veil killed them
  rather than vanilla's fallback "was too buffed".
- `veilfog` — an invisible marker the server puts on anyone past the fog line;
  it draws the drifting mist client-side. It stays after the Mark is earned,
  because §9 requires the border to remain visible once it stops hurting.
- `/veilmark [player] [1/0]` — grants, revokes and reports the Veil Mark, and
  prints where the wall is and how deep the player is standing. §9's séance
  questline is not built, so this is the only thing that writes the unlock
  today; the Ferryman will call the same method.
- `tools/locale_audit.py` now follows `BuffRegistry.registerBuff`. A visible
  buff without a `[buff]` name prints `buff.<id>` in the HUD forever, and
  nothing was checking it. Invisible buffs (everything on vanilla's
  `ArmorBuff` — every trinket and set bonus the mod registers — and any of our
  classes that sets `isVisible = false`) are correctly exempt, because the
  engine never asks for their key. The audit also now resolves an ID given as
  a `static final String` constant rather than skipping it.

### Design notes
- Built as **one** gate mechanic, per §42.4: `VeilRegion` decides where,
  `SoulExposureBuff` decides what it costs, `VeilWorldData` holds the clock and
  the unlock ledger. The Infernal Visa (§18) is a second threshold, a second
  buff table and a second auth set in those same three files — not a second
  implementation.
- **The region check is a region check.** §8 forbids blocking tiles and calls
  out teleport abuse by name, so there is no wall, no boundary event and no
  entry hook: a `WorldData` tick asks every online player once a second where
  they are standing. However they got there, the answer is the same.
- The fog line is **derived** from `RealmDepth`'s own bands rather than typed,
  so retuning the concept's table moves the fog with it. It is also the first
  thing in the mod that the realm field actually decides.
- **No new art.** The icon is vanilla's `buffs/spirithaunted` and the mist is
  vanilla's `particles/fog`, both loaded by literal path and recorded in
  `docs/VANILLA_ASSET_MAP.md` §1.3b.

### Known consequence
- The fog overlaps the deep Beetle Outlands, which sit at 900 tiles where the
  realm field puts the Crooked Beyond at 4210. Correct against §39, which gates
  the Crooked Beyond behind the Veil Mark, and it resolves itself when the
  Outlands move out to their true band — but until then, walking far enough
  into the Outlands means taking Soul Exposure.

## [Unreleased] — the Outlands get their own faces — 2026-09-01

### Changed
- **The Beetle Outlands' three ascended mobs are the mod's own now.** They used
  to be vanilla's `crystalgolem`, `ascendedgolem` and `crystalarmadillo` placed
  by string ID, which meant crystal-cave art standing on striped violet ground.
  The player drew replacement sheets, so the spawn table now names
  `crookedgolem`, `rarecrookedgolem` and `crookedarmadillo` — `CrookedGolemMob`,
  `RareCrookedGolemMob` and `CrookedArmadilloMob`, each a subclass of the vanilla
  mob it replaces. **Nothing but the sprite moved**: every stat, ability, AI tree
  and spawn rule is inherited untouched, weights and caps in the table are
  unchanged, and `docs/BALANCE.md` did not need a line. This is an art and
  identity pass, not a rebalance.
- The three probe entries in `SkyreachStatusCommand` and the matching assertion
  in `scripts/integration_test.sh` follow the new IDs.

### Added
- `mobs/icons/{crookedgolem,rarecrookedgolem,crookedarmadillo}.png` — bestiary
  icons, drawn deterministically in the new `tools/asset_generator/gen_outlands.py`
  from `palette.CROOKED_*` ramps sampled out of the supplied sheets.
  `MobRegistry`'s `loadIcon` is hard-wired to `mobs/icons/<stringID>.png` with no
  setter, so without these the bestiary would draw the engine's ERR tile.
- English and German names for all three, ledger rows, and `size_audit` rows
  against `mobs/icons/crystalgolem.png` and `mobs/icons/crystalarmadillo.png`.

### Learned, and written down
- **A vanilla mob's texture is not overridable — only its `addDrawables` is.**
  All three vanilla classes read a static `MobRegistry.Textures.*` field inline
  inside `addDrawables`, and `Mob` has no per-instance texture hook
  (`VERIFIED [jar]`). Assigning into that static field would repaint vanilla's
  own crystal golems in vanilla's own caves, so each subclass re-implements
  `addDrawables` line for line and changes only the sampled `GameTexture`. The
  override cannot call `super` — that IS the vanilla body draw — and nothing is
  lost by skipping it, because the overridable `Mob.addDrawables` has an EMPTY
  body; vanilla's own `AscendedGolemMob` already skips it for the same reason.
- **Death gibs are cut from the mob's own sheet**, so `spawnDeathParticles` had
  to be overridden too or a Crooked Golem would shatter into crystal shards. The
  supplied sheets carry the gib strip in vanilla's own place (32 px row 8,
  columns 0-3), so the sprite indices did not change.
- All of the above is written up in `docs/TECHNICAL_LEARNINGS.md` under "A
  vanilla mob's sheet is reachable only through `addDrawables`", including where
  a second draw pass can hide a mob's whole minimum-light floor.

### Known gap
- **The armadillo lost its glow pass.** Vanilla draws `crystalarmadillo` twice —
  the body at raw ambient light, then `crystalarmadillo_light` over it at a
  `minLevelCopy(100)` floor, a dithered mask over 45% of the body that makes its
  crystal shell shine in the dark. We have one sheet and not two, so the second
  pass is dropped rather than faked by drawing our own body over itself, which
  would self-illuminate the whole animal instead of its shards. What did NOT get
  dropped with it is the light floor: that overlay is where vanilla's armadillo
  keeps its entire minimum-light guarantee, so the single pass here carries
  `minLevelCopy(100.0F)` — the same value, applied to the whole animal instead of
  to 45% of it in a dither. Not pixel-identical to vanilla, and much closer to it
  than an invisible 200-speed charger would have been. Drawing
  `mobs/crookedarmadillo_light.png` restores the exact behaviour.

## [Unreleased] — the endgame ladder — 2026-08-31

Alongside the tile pass below, in the same unreleased version.

### Decided
- **The mod becomes endgame-only.** The player, ten incursions deep: "bitte
  Schwierigkeit und Wertigkeit startet mindestens auf Niveau der 1. incursion
  für die schwächsten gegner .. wir sind mittlerweile bei 10 durch und brauchen
  Herausforderung für danach". Every mob, drop and piece of gear moves from the
  deep-cave / tungsten tier it was built at to a six-rung ladder that starts at
  vanilla's incursion floor and ends past tier 10. The Skyreach's weakest enemy
  becomes the game's incursion baseline; Hell's is the rung vanilla does not
  have.

### Added
- **`docs/BALANCE.md`** — the reference every later balance change points at,
  and the arithmetic behind the ladder: the incursion scaling arrays with their
  source lines, the derived tier 1-10 table for HP / damage / loot, the measured
  floor mobs and gear with the class each number was read from, the realm
  ladder, the role modifiers worked out per realm, the `MaxHealthGetter`
  difficulty spread, the gear ladder, and a recipe for re-deriving all of it
  after a version bump rather than trusting the file. `VERIFIED [jar]`, and
  explicitly **not** player-confirmed.

### Learned, and written down
- **Incursion difficulty is a level modifier, not mob stats.**
  `BiomeMissionIncursionData.initModifiers()` builds exactly three —
  `ENEMY_MAX_HEALTH`, `ENEMY_DAMAGE`, `LOOT` — from two cumulative arrays plus a
  flat 15% loot per tablet tier. The same mob classes are spawned at every tier.
- **Incursion tier 1 applies no multiplier at all.** Both arrays open with
  `0.0F` and the sum loop is bounded by `tabletTier`, so tier 1 is HP x1.00 /
  damage x1.00 with only loot moving (+15%). "Niveau der 1. Incursion" is
  therefore not an abstract multiplier but a set of printed stats that can be
  read straight off vanilla's classes: **1000 HP / 130 damage / 40 armour**
  (`AscendedGolemMob` at Classic, `CrystalGolemMob`'s damage and armour), and
  **Arcanic's 29 chest / 1900 enchant / EPIC** for gear. Tier 10 is HP x4.00 /
  damage x2.15 / loot x2.50.
- **Past tier 10 the arrays run out** and vanilla falls back to +0.45 HP and
  +0.04 damage per tier — health keeps climbing, damage nearly flattens. The
  ladder's Hell rung deliberately ignores that flattening, which is why it is
  documented as the one rung that is not a vanilla tier.

### Not done in this entry, on purpose
- **No Java changed.** This is the specification and its evidence; the mob,
  drop and gear retunes that implement it are separate work, and until they land
  `docs/BALANCE.md` is a target rather than a record. The deep-cave calibration
  still described in `docs/CURRENT_STATE.md`'s Sky Arsenal and item-polish
  sections is now marked as historical there.

## [Unreleased] — one world, and it stops being nice — 2026-08-31

### Changed — the direction
- **The Veil stops being a second dimension to build out.** The player's call:
  *"das wird zu viel arbeit, wir machen nur sky region ... bitte nichts
  wegwerfen der bestehenden sachen sondern auf eine welt eindampfen"*. Its
  ground, props, mobs and its one building now appear in the Skyreach instead.
  The Veil dimension stays registered and still generates — un-registering it
  would strand every save that has been there — but no new content goes into it.

### Added
- **The Beetle Outlands** (`worldgen/SkyOutlands`, `biomes/OutlandsBiome`): the
  sky's wrong ground, gated by DISTANCE from the spire rather than by the biome
  noise. Impossible within 900 tiles — a hard floor, not a low probability —
  then rising linearly to 3000. Measured over 5 seeds and 956,566 land tiles:
  0.00% at 800, 0.40% at 900, 1.13% at 1000, 2.39% at 1400, 7.68% at 2000,
  17.67% at 2600, 25.16% at 3200, 26.53% at 4000.
- **`evilwall`** — crystal massifs, from supplied hand-drawn art. Registered as
  a vanilla `RockObject`, which is what actually owns the `crystalwall` sheet
  the art was drawn on (read out of the 1.3.2 jar, see TECHNICAL_LEARNINGS).
  Placed through the existing outcrop formation field, so massifs come out as
  ridges and knots with walkable gaps: 2.7-5.5% of Outland tiles.
- **Seance Circles now stand in the world**, one hashed site per 260-tile
  lattice cell that lands on wrong ground — "an bestimmten stellen, nicht
  random", as asked.
- **The Crooked House scatters in the sky**, not only in the fen. Its sky site
  test runs through `describeTile`, so it cannot land on a road or in the
  Mistsea.

### Changed
- In the sky a Seance Circle is a summoning ring rather than a door. The sky has
  no boss yet, so it says so (`misc.seancesilent`) instead of opening a rift
  into the layer this direction is folding away. Deliberately an honest dead
  end; wiring the summon needs a boss mob first.

### Not done, on purpose
- The boss the portals call does not exist.
- `evilwall` sets no tool tier, where vanilla's own `crystalrock` uses tool
  tier 10. Pickaxe-gating a whole biome is a separate decision.

### Added — the Outlands hit at ascended tier
- `crystalgolem`, `ascendedgolem` and `crystalarmadillo` spawn in the Beetle
  Outlands, by vanilla string ID — no new classes and no new art. The player
  had finished incursion 10 and reported everything too easy; the numbers
  agreed, since this mod topped out near 520 HP / 70 damage against vanilla's
  ordinary ascended mobs at 1000 HP (Classic) and 130 damage behind 40 armour.
  `HostileMob.isValidSpawnLocation` is implemented, so unlike the Cloud Lamb's
  inert table entry these actually place. All three are dark-spawners, so the
  Outlands are uneasy by day and dangerous after dark.
- This gives the sky a non-incursion source of ascended-tier fights and their
  drops, gated behind distance rather than behind a boss. Deliberate.

### Progression note
- **`evilwall` drops `crystalstone`**, so the sky now has a source of a
  deep-cave material. Deliberate, and the player's call. It is a late source —
  the Outlands begin past 900 tiles and only reach ~25% of land by 3200 — but
  it has not been played.

## [Unreleased] — the ground the player walks on — 2026-08-30

### Fixed
- **Every natural terrain tile was flat.** 63-114 pixels of texture per
  full-tile cell, against 294 for `snow_splat`, the sparsest natural ground in
  vanilla Necesse, and a median of 603 across 37 of them. cloudturf alone is 54%
  of the Skyreach's land. Now 364-406, at vanilla's loudness.
- **The four craftable floors** had the opposite fault: density already in band,
  but louder than any vanilla floor (mean 28-41 against 7.9-27.0) and built on
  single pixels. Boards, courses and tile seams still read; checked on a 4x4
  field against vanilla `deadwoodfloor`, `bamboofloor` and
  `deepstonetiledfloor`.
- **`murkwater`** was the flattest surface in the mod at 70, missed by the
  terrain pass because it is a liquid. Its green glint is gone, deliberately:
  Necesse's animation time is global, so it made every water tile in view blink
  on the same tick.

### Added
- **`aurorashoaltile` (Dawnturf / Morgengras)** — the Aurora Shoals had no
  ground of their own and wore the Driftlands' cloudturf, the commonest floor in
  the sky. Complete family: palette ramp, generator material and features, tile
  class at terrain priority 215, registration, the painter branch that was
  missing, both locales, ledger row and audit role. Proven placed, not merely
  registered: 967 tiles over a 400x400 window from the offline painter dump.
- **`tools/tile_behaviour_audit.py` now checks the texture itself** — density,
  mean loudness and 2x2 block coherence, per role, against bands measured off
  the vanilla dump. Verified by making it fail on the pre-fix cloudturf.

### Changed
- `palette.py` gained `grain_d` / `grain_l` at ~7 RGB either side of base on
  every ground ramp. The smallest existing step was d25-d32 where vanilla
  carries its whole grass texture inside d5, so any density built from the old
  ramps was automatically several times too loud. Same hues; no biome palette
  shift, and no other sprite changed.

### Learned, and written down
- Every vanilla splat in the game is **100% coherent on a 2x2 pixel block
  grid** — natural ground and crafted floor alike, without one exception. That
  is how vanilla reaches its density without dithering. This mod broke it on all
  fifteen sheets.
- Density alone is a **gameable** target. The first repair pass hit the band
  exactly (cloudturf 345 against vanilla grass's 344) and looked like camouflage
  netting, because the number counts pixels and says nothing about how far they
  deviate. Full reasoning in `docs/TECHNICAL_LEARNINGS.md`.

### Not done, on purpose
- `beetlefreak` and `skyway` are converted from supplied reference art; holding
  a painting to a procedural rule measures the wrong thing.
- `mistsea` is recorded in the audit's `KNOWN_UNFIXED` rather than silently
  skipped. Its density is fine but it runs mean 31.8-49.7 at 40-48% coherence.
  A cloud deck legitimately carries more relief than still water, so it wants a
  deliberate pass with the player's eyes on it.

## [0.6.0] — "The Working Sky" — 2026-08-30

The release that gives the sky a reason to be lived in rather than visited: a
fourth biome that was **built** rather than grown, animals and workstations that
produce, five more weapons and a real armour set, the cats living wherever you
put their basket — and an art pass that finally measured itself.

Counted from the registries: **73 objects · 46 items · 15 tiles · 26 mobs ·
7 biomes · 5 journal quests · 2 dimensions · 92 recipes**, every ID named in
English and German (371 locale entries, both in sync).

Three things in here are worth reading as process rather than content. The
Marble Checker crash was a vanilla bug nothing else in the game could reach.
`size_audit.py` was passing by comparing **nothing at all** on any machine
without one specific path. And the mod's banner drew the same picture in all
four rotation rows for months, which is what hid an asymmetry in the audit that
was supposed to catch it. Each is written up in
`docs/TECHNICAL_LEARNINGS.md`.

**Nothing in this release has been played by a human yet.** Everything below is
verified by the gates listed at the end, which cannot see client rendering.

### The worldbuilding loop, the wall sets and the spire — 2026-08-29

Landed on a parallel branch and merged in for this release.

#### Fixed
- **The spire was white because its wall was never generated.** The preset
  referenced a wall the worldgen never placed.
- **The Warden's price was wrong in four records.** Four documents still said
  100,000 coins; he has cost 30,000 since `5ce05ae`. The records now match the
  code.
- **The common biome had exactly one daylight enemy**, and the Cloudberry bush
  was still not a bush — one fix covered both halves of that report: it now
  regrows AND has a bush's silhouette, plus a Cloudberry sapling.
- **The rotation-variety bug**: sheets that drew the right cell at the right
  size and *the same picture* in every rotation. `objects/skywatchbanner.png`
  drew 738 identical pixels in all four rotation rows; it now has four real
  views (over-the-cap, edge, face-on, edge), the shape vanilla's own
  `bannerofpeace` has.
- **The side-wall window is a slot in the roof**, and three wall sets never got
  that fix. Cloudmarble, Nightfell and Skystone Brick corrected.

#### Added
- `tools/rotation_variety_audit.py` — every cell the engine reads separately
  must actually differ. This is the gate that catches "right size, same
  picture", which no mass or format check can see.
- `tools/template_audit.py` and `docs/CODEX_SPRITE_TEMPLATES.md` — a sprite
  template treated as a specification that can be checked.
- `tools/content_ledger.py` + `docs/CONTENT_LEDGER.md` — every registered ID
  needs one line saying what it is, read out of the source rather than a
  hand-kept list, so nothing can ship undescribed.
- `scripts/fetch_dedicated_server.sh` — the game install is a free download, so
  "cannot verify on this machine" stopped being true.
- `scripts/sky_map_render.sh` — renders the painter's output so placement can be
  judged at screen scale instead of argued about.
- `docs/WORLDBUILDING_LOOP.md`, `.claude/commands/chapter.md` and the chapter
  design documents (`docs/design/chapter-01-skyreach-*.md`) — the process for
  running world expansion as a loop rather than as one-off sprints.

### Item icons that read, and a gate that measures — 2026-08-29

#### Fixed
- **Twelve item icons redrawn.** Every vanilla 32x32 item icon carries 288-712
  opaque px (median 440). The mod shipped `tempestedge` — one of its two
  original weapons — at **45 px**: `gen_items._tempest_blade` laid a 1px core on
  top of its silhouette mass, so the finished blade was a hairline.
  `docs/REVIEW-2026-08-24.md` listed widening that exact blade as art action #1
  and it had stayed undone. Redrawn against a named vanilla analogue each:
  `flickerlightgarland` 29→379, `tempestedge` 45→334, `veilessence` 70→402,
  `ghostlantern` 77→448, `wardencandelabra` 78→456, `stormshard` 85→505,
  `aeronautwreck` 101→466, `fulgurite` 101→451, `galehowl` 101→310,
  `glowfern` 101→655, `withershrub` 113→500, `aurorapetal` 117→461.
  `player/weapons/tempestedge.png` and `player/weapons/galehowl.png` move with
  them through the shared `_tempest_blade` / `_galehowl_bow` helpers.
- **`size_audit.py` passed by measuring nothing.** Its `--vanilla` default was a
  dev-container path, so anywhere else every pair reported "vanilla ref missing",
  the flag count stayed 0, and it printed "0 sprite(s) flagged" and exited 0 —
  **0 of 122 rows actually compared** on this checkout. It now prefers the
  checkout's own `vanilla-sprites/`, reports how many rows compared, and fails
  when nothing was measured. A gate that compares no sprite is not a pass.

#### Changed
- `size_audit.py` gained a row for each redrawn icon, so none can thin out
  again unseen. The two held weapon sprites are **manual** rows on purpose:
  they are on a 32x32 canvas while every later mod weapon matches vanilla's
  much larger held sheets, and a mass ratio across a 3x canvas difference
  measures the canvas, not the drawing. That canvas mismatch is recorded as
  open in `docs/CURRENT_STATE.md`, not silently folded into an art change.

#### Known / follow-up
- **35 item icons remain below the thinnest vanilla icon** and are still
  uncovered by the audit; measured and listed worst-first in
  `docs/CURRENT_STATE.md`. `aurorabloom` (141 px) is first in line — the
  redrawn `aurorapetal` (461 px) now sits next to it in the inventory.
- The audit maps 100 of 307 shipped PNGs; the other 207 are unmeasured.

#### Verified
- `buildModJar`; `scripts/integration_test.sh` (boots, generates, restarts,
  spire/warden/cats persist, no log errors); `size_audit` 0 flags with 119 of
  122 rows genuinely compared; `locale_audit` 203 IDs; `tile_behaviour_audit`,
  `sheet_format_audit`, `furniture_audit` all OK; the generator reproduces
  `src/main/resources` byte-for-byte and exactly the 14 intended PNGs differ.
- **Not seen in the real client by anyone.**

### Cat Basket: the cats live where you put their basket — 2026-08-28

Player report: *"Katzenbetten sollen in normalem Haus platziert werden können
etc in der Stadt damit die Katzen dort wohnen. ich habe beide gerade platziert
und die sind weg oder irgendwo anders dann erschienen wo ich es nicht weiss"*.

#### Fixed
- **Placing a Cat Basket now makes that tile the cats' home**, on whatever level
  it stands. It was a bare `FurnitureObject` with no object entity, no placement
  hook and no connection to Siggi and Peanut at all — placing one was pure
  decoration, and the cats' home stayed hard-wired to the basket tile inside the
  Warden's Spire, in the Skyreach. The cats were never lost; nothing in the game
  ever said where they had gone.

#### Added
- `objects/CatBasketObject` — a real object that claims its tile on
  `placeObject` and releases it on `onDestroyed`, still a `FurnitureObject` with
  `furnitureType = "petbed"` so it keeps counting toward a settlement room's
  furniture score.
- `quest/CatHome` — the server-side home record and every consequence of
  changing it. The newest basket wins; breaking the active one sends the cats
  back to the spire; breaking a spare one changes nothing; only cats that have
  actually been coaxed home with a Cloudpuff Treat ever move, so a wild cat
  stays wild and the quest step keeps its point.
- The home lives in `SkywatchWorldData` as a tile **plus** a `LevelIdentifier`.
  `SkywatchQuestData` is `LevelData` on the Skyreach and cannot hold a home that
  stands in a Surface town.
- `SpireCatMob` travels across dimensions with vanilla's `TeleportEvent` and
  rebuilds its homesick tether in `onLevelChanged()` — a mob that changes level
  never gets `init()` again.
- Five chat lines in both locales, so placing a basket always answers what
  happened: they moved in, they moved here from the older basket, they are still
  out there and want a treat first, or the basket is gone and they went back to
  the spire.

#### Changed
- `/skyreachstatus`'s cat probe prints the home actually in effect with its
  level identifier (`home=surface:-10,1064 homeSource=placed`), measures every
  cat against it, and looks for the cats on the home level as well as the
  Skyreach. The NPC census counts them across both levels too.
- `skysurfacestatus basket` places real baskets on the Surface through the
  player's own placement path, and `scripts/integration_test.sh` asserts the
  cats actually move — and are still living there after a restart.
- `scripts/integration_test.sh` saves the world explicitly and waits for it
  before sending `stop`. Vanilla's stop path can silently write nothing (see
  `docs/TECHNICAL_LEARNINGS.md`), which is what the test's "flaky persistence
  assertions" have really been.

### v0.6 visual production sprint — 2026-08-25

The large visual assets finally match the mini vegetation, critters and
atmosphere that already worked. Everything regenerated through the pipeline;
inspected on contact sheets; NOT yet played by a human.

#### Changed
- **Rock family rebuilt** (`gen_rocks.py`): 8 Skystone / 6 Veilrock variants
  with real geological characters (slab, strata, boulder domes, fracture,
  split fissure, rubble, weathered pits, broken terrace) instead of two
  speckle-jittered twins; exposed edges are carved so perimeters stop reading
  as ruled rectangles. Faces are base-dominant and the base fades out through
  vanilla's measured soft-alpha skirt (195/195/113/78/55/29, no bottom
  outline) — the old opaque deep-ramp band WAS the "far too long and dark"
  rock shadow. Engine side needed no changes: `RockObject` picks
  `width/32` variants per tile (verified by disassembly).
- **Storm Shards redesigned** (`gen_objects.py`): 4 asymmetric 64x64 cluster
  formations of tilted, overlapping, value-alternating crystal blades with
  deep violet internal planes and restrained pale edges on a shared rubble
  bed — no more row of teeth. Size-audit ratio 0.74 → 1.01 of vanilla
  `crystalwall`.
- **Tree volume pass** (`gen_trees.py`): new `_canopy_volume` shared by Nimbus
  Willow, Prismabirch and Fulgur Pine — overlap shadows where one lobe/tier
  sits under a higher one, one canopy-scale light field, per-lobe sheens
  demoted on the shadow side, trunk collar. Fixes "stacked pancakes" without
  touching the size and silhouettes the playtest approved.
- **Cloudberry bush rebuilt** (`gen_objects.py`): a dense leaf-clump dome
  (~30x20) over woody stems with amber berry clusters sunk into the mass —
  no longer two mushrooms. Greener leaf ramp in `palette.py`.
- **Warden identity pass** (`gen_npcs.py`, `palette.py`): storm-blue coat
  ramp matching the recruited settler's pinned livery, hood-down cowl behind
  the hair (the icon's silhouette), brass Skywatch collar clasp, weathered
  mend patches, cheek lines. Renderers untouched.
- **Warden beacon rebuilt** (`gen_furniture.py`): observatory machinery —
  sigil plinth, banded pillar with a snapped armature, brass yoke cradling a
  faceted storm-glass lens (burning cold-teal when lit).

#### Added
- **Spire hero accents**: `skywatchtelescope` (brass refractor on a stone
  tripod) and `skywatchastrolabe` (navigation table with armillary rings).
- **Stormveil prop families**: `stormscreed` (scorched ground decal),
  `skywatchrubble` (broken carved stone), `chargecrystal` (lit), `withershrub`
  — registered, craftable, ready for worldgen composition.
- **Aurora accents**: `aurorashards` (lit, teal/rose), `starfall` (lit).
- **Sky oddity seeds** — `skyballoon`, `aeronautwreck`, `skyparcel` —
  registered + craftable but deliberately absent from worldgen; rare
  encounters for later.
- New props live in `tools/asset_generator/gen_props.py` with item icons
  (`gen_prop_icons`) so no crafting entry shows the error texture.
- 11 new locale entries in both languages (locale audit now gates 77 IDs).

#### Verified
- Regeneration byte-identical; `size_audit` 0 flags; `locale_audit` 77/77;
  `buildModJar` builds; `scripts/integration_test.sh` passes (generate,
  restart, spire/warden/cats persist, no errors).

## [0.5.0] — "The Skywatch Opens" — 2026-08-24

The Skyreach becomes a place you travel TO. The surface stays your world —
your base, your settlement, your progression — and the sky is the layer you
ascend into to explore, fight and gather before coming home again.

### Changed
- **The Stairway is a portal, not a ladder.** Wherever it is built, it routes
  to ONE canonical Skyreach origin: the Old Warden Spire hub. `SkyOrigin`
  derives that position from the world-generation seed alone, so the terrain
  painter, the surface stairways and the spire stamper all agree without any
  shared state — and everyone in a multiplayer world arrives at the same
  landmark instead of scattering across an empty sky.
- **Terrain radiates from the hub.** The island mask and biome mask are clamped
  inside `HUB_RADIUS`, so the first ascent always lands on walkable, safe,
  recognizable ground; the immediate spire grounds stay an open plaza so the
  landmark reads clean. Ore density then widens with the distance band, which
  makes travelling outward pay.
- **Recruitment replaced the fetch chain.** The Warden's four-stage delivery
  quest is gone. He is now a single goal: find him, then pay 100,000 coins —
  the top vanilla settlement-expansion tier — and he leaves the sky to join
  your surface settlement. The payment IS the recruitment; there is no spawn
  item to buy.

### Added
- `SkywatchGateObjectEntity`: the return gate resolves each player's own bound
  stairway, and re-places it if it was broken while they were away.
- `WardenSettlerMob` / `WardenSettler`: the Warden as a real Necesse settler
  (a `HumanShop` on the same branch as the Elder), with his own settlement
  icon — the same man bare-headed, in the storm-blue Skywatch shirt.
- `/skyreachstatus` reports the settler wiring alongside the world state, so a
  broken registration is visible from the server console instead of only in a
  playtest.

### Fixed
- **The recruited Warden could never move in.** `"wardensettler"` was never
  registered with `SettlerRegistry`, so `HumanMob.getSettler()` resolved to
  null, the vanilla recruit path answered "not a settler", and the payoff of
  the whole sky progression dead-ended at the doorstep. He is now a registered
  settler type modelled on the Elder — never spawns on his own, never moves
  out, cannot be banished — and moving in costs nothing, because the fee was
  already paid in the sky.
- **The settler arrived thousands of tiles from home.** `findTeleportLocation`
  already returns pixel coordinates; the placement multiplied them by the tile
  size a second time, so a 100,000-coin payment dropped the Warden deep in the
  wilderness.
- **The Veil was unreachable.** The Seance Circle checks for the Silver Bell,
  but the quest that awarded it disappeared with the old fetch chain. The
  Warden now hands it over as part of the recruitment.
- `SkyOrigin`'s documented ±192 range did not match its arithmetic (Java's `%`
  keeps the dividend's sign, so the real box is -576..+192). The behaviour is
  now part of the world contract and is documented as such rather than
  "corrected" — changing it would move the spire in every existing save.

### Notes
- Siggi and Peanut are unkillable and save-persistent by native means, and
  `SpireCatMob` now records WHY: `canTakeDamage() == false` gates every damage
  path in `Mob`, and `CritterMob.shouldSave()` is `shouldSave && !canDespawn()`
  — so the `canDespawn = false` line is also what keeps them in the save file.

## [0.4.1] — 2026-08-24

Art quality pass + full project review.

### Changed
- **Color identity & contrast pass** across the sky roster (deterministic
  palette regeneration, 18 textures): the **Zephyr Ray**'s back ramp was
  deepened two steps — its old base sat at the Mistsea's luminance and the
  ray vanished against open cloud — with a strengthened teal accent; the
  **Galehound** gained a storm-dark back, brighter highlights and detached
  cyan wind flecks off mane and tail (night-readable color identity); the
  **Skystone Golem**'s plates separate through deeper shadows with greener
  moss and brighter eye glow; the **Sky Warden** wears a dotted gold hem
  trim as one warm accent; **Cloudturf** shifted hue to silver-green so
  islands read as living meadows against the pale Mistsea.

### Added
- `docs/REVIEW-2026-08-24.md`: full professional review — verification of the
  modding structure against the official wiki, asset-quality findings, and a
  prioritized expansion plan (map division with distance rings + landmarks,
  random events/weather, NPCs, recipes, weapons, fishing).

## [0.4.0] — "The Living Sky" — 2026-08-24

The fill release: every sky biome gets its own trees, meadows, flowers,
ores and animals — plus a size correction across the old art.

### Added
- **Trees, wood and floors, one family per biome**: the **Nimbus Willow**
  (Driftlands, cloud-lobe canopy with weeping strands), the **Fulgur Pine**
  (Stormveil, charred tiers with live embers — one variant lightning-split)
  and the **Prisma Birch** (Aurora Shoals, banded bark, iridescent crown).
  Full vanilla tree mechanics: axe them for their own logs, saplings drop
  and replant, leaf particles drift on the wind. Each wood crafts its own
  buildable plank floor (Workstation).
- **Meadow carpets**: large walk-through tall-grass fields (~70% ground
  coverage inside meadow patches, calibrated against vanilla lush areas) —
  Tall Cloudgrass, Storm Sedge and Prism Grass, tiling edge-to-edge so
  whole stretches read as one living meadow you wade through.
- **Six pickable plants**: Cloudbell, Sky Tulip (3 colors), Static Moss,
  Thunderbloom, Glowfern, Aurora Lily — all drop materials.
- **Two new ores**: **Fulgurite** (lightning glass, Stormveil) and
  **Prismshard** (crystal veins, Aurora Shoals), mined off skystone
  deposits like Aetherium.
- **Four new animals**: the **Galehound** (night pack hunter of the
  Driftlands meadows), the **Dawnpiercer** (glass-cannon dive bird of the
  Shoals), and two critters — the **Zephyr Finch** and the **Dew Snail**.
- All new content localized (en/de) and fed into world generation with
  per-biome density bands.

### Changed
- **Size correction pass**: a new audit tool (`tools/size_audit.py`)
  measures every mod sprite against its vanilla analogue; ten undersized
  sprites were rebuilt to vanilla mass — most dramatically the Séance
  Circle (2% of a vanilla ritual altar before), the Skywatch Banner (now a
  proper two-tile wall drop), the gloom willow, streetlamp, wall lantern,
  raven statue, gloomshroom, both crystal clusters, windwheat and
  skyreeds. The size law is codified in the art pipeline rules.

## [0.3.4] — 2026-08-23

Playtest fixes: real journal quests, correct walls, a warden with presence.

### Added
- **Real HUD quests**: the whole Warden chain now runs through the vanilla
  quest journal — "The Warden's Call" (find the spire), "Light the Beacon"
  and "Anchor the Island" (delivery quests with live per-item progress bars),
  and "The Spire Cats" (two-cat checklist) appear in the tracked sidebar and
  the quest log, sync in multiplayer, support team-sharing and abandoning,
  and are given/completed at the exact same dialogue points as before. Chat
  lines remain as flavor; the objectives no longer live only in chat.

### Fixed
- **Wall inner corners no longer break**: the wall sheet's autotile blob was
  decoded cell-by-cell against the game's actual draw code — the face halves
  sat one row off and the inner-corner/junction pieces (rows 5-7) contained
  the wrong art, which rendered as misplaced squares wherever walls met in an
  L or T. Both wall sets regenerate with the correct piece semantics.
- **The Sky Warden has real presence**: rebuilt on vanilla chibi proportions
  (the player's head alone is ~26px wide; the old warden's whole coat was
  ~11px, reading a third as wide as the player).
- **Quest structures are unbreakable**: the beacon (lit and unlit) and the
  sky anchor can no longer be mined — an older jar let a pickaxe remove the
  beacon with no drop, soft-locking the chain. Worlds where it already
  happened self-heal: the level re-places the beacon in its correct
  quest-stage state.
- **The spire now spawns near your arrival**: it anchors to the first ascent
  stairway (radius sweep from ~20 tiles) instead of the world origin, which
  could put it hundreds of tiles from a base far from spawn. Existing worlds
  keep their placed spire (the map marker covers finding it).
- **Golems actually appear**: they patrol the Stormveil crystal fields too
  now (they were exclusive to the rarest biome, the Aurora Shoals, where
  their cap is also raised slightly).

## [0.3.3] — 2026-08-23

Never lose the tower again.

### Added
- **World-map markers**: on a player's first ascent, the **Warden's Spire**
  and **their arrival stairway** are pinned on the world map (M) with their
  own icons — the vanilla cartographer-map mechanism, no cheats. Delivered
  once per player (persisted server-side), and markers live in the client's
  per-world map data like any hand-placed marker, so deleting one is
  respected. Players already in the sky can run `skyreachstatus` once to get
  the spire pinned retroactively; the stairway marker follows on their next
  ascent. Both icons are also available in the map's own marker editor.
- Two new 32×32 map icons in the vanilla `ui/mapicons` style (spire with
  teal beacon, marble stairway on a cloud).

## [0.3.2] — 2026-08-23

Art release: living sprites, organic construction.

### Changed
- **The Storm Wisp is finally alive**: rebuilt on the vanilla flying-spirit
  pattern with a 4-frame animation — a flame-teardrop spirit whose trailing
  tendrils undulate, whose top lick sways, and whose rim lightning crawls
  around the body, with a breathing glow halo synced to the crackle. It was a
  static orb ("an unanimated dot") before.
- Storm Wisp bestiary icon redrawn to match the new flame-teardrop body.
- **Zephyr Ray rebuilt as an organic manta**: wings are curved membranes now —
  convex leading edge, scalloped trailing edge, shading bands that follow the
  sweep — with buried wing roots, cephalic lobes, mantle spots and an
  S-curved tail whip; folded frames sweep the tips down and back, so the flap
  visibly curls through the stroke instead of just scaling. Icon matches.
- **Gloom Shade hem now genuinely undulates**: stable hood with a floppy
  point, asymmetric drifting cloak masses, and a ragged tendril hem that
  re-poses on every walk frame (the vanilla spirit construction), with claw
  arms and glowing eyes applied after the outline.
- **Flora made organic across both dimensions**: windwheat arcs in a fanning
  clump with nodding seed heads (was a picket fence of straight stalks);
  skyreeds and whisperreeds got uneven clumps, curved blades and hooked or
  fluffed tips; the cloudberry bush a lumpy lobed canopy with berry clusters;
  gloomshrooms bell-dome caps with wavy skirts and broken glow-gills (were
  flying saucers); ash bones individually bowed ribs, ash mounds and a real
  skull; the dead tree / gloomwillow a continuous S-curve trunk with weeping,
  fully connected branches and the raven re-perched on a branch elbow.
  Grass-class sprites now skip the outline pass like vanilla grasses.

### Fixed
- **Build works on the newest JDKs**: Gradle wrapper upgraded 8.10.2 → 9.7.1
  (checksum-pinned), so building with JDK 24/25 no longer crashes in Groovy
  (`Could not initialize class ...ReflectionCache`). The shared build script
  no longer uses the internal `org.gradle.internal.os.OperatingSystem` API.
  README troubleshooting documents the crash signature and the
  wrong-folder trap (building the bare upstream template instead of the
  mod branch yields an empty `1.0` jar).
- README troubleshooting now covers BOTH artifacts a broken sky/Veil level
  leaves in the world save — `levels/<level>.dat` AND the
  `levels/regions/<level>/` folder — plus the `worlds/` folder layout some
  co-op tools use, and clarifies that in multiplayer only the host's save
  holds world data (joining players have nothing to repair).

## [0.3.1] — 2026-08-23

Playtest feedback release: real livestock, real landmasses.

### Changed
- **The Cloudlamb is now actual livestock**, not a critter: it extends the
  vanilla sheep, so **rope-catching, feeding troughs, breeding, growing up
  and shearing** (yields wool) all work exactly like surface animals — build
  them a pen in the sky or lead them home. Redrawn at **vanilla sheep scale**
  (it was bug-sized) on the vanilla sheet layout, with a sheared look and
  fleece death particles; sky twist: grown lambs drop windsilk.
- **Much more land**: island noise retuned in both dimensions (Skyreach
  threshold 0.55 → 0.48, scale 42 → 54; Veil 0.50 → 0.44, 46 → 56) — bigger,
  more frequent landmasses, so travel is walking with occasional swims
  instead of aimless paddling through clouds. Applies to newly generated
  regions; for a fully coherent layout, reset the level per the README's
  troubleshooting section (existing explored regions keep their old shape).

## [0.3.0] — "The Veil Below, Part 1" — 2026-08-23

The mod's second world opens: a gothic-comedy afterlife layer below the deep
caves, entered by ritual. Part 1 ships the dimension, its first biomes, its
first resident enemy and the signature light; Mortimer, Vesper, the quest
chain, the Model Town, the Office of Eternity and the Ashwyrm follow in 0.3.x.

### Added
- **The Séance Circle**: craft it (6 Storm Shards + 4 Windsilk + 2 Aurora
  Petals, Tungsten Workstation), place it, and use it while carrying the
  **Silver Bell** from the Warden's cat quest — the bell is the key and is
  never consumed. The circle tears open a persistent **Rift** (vanilla
  ladder netcode, multiplayer-safe, auto-placed return rift on the far side).
- **The Veil** (dimension −3, below the deep caves): infinite, seeded,
  region-streamed, and in **permanent night** — the vanilla "light = safety"
  rule shapes the whole layer. Two sub-biomes: the **Gloomfen** (dark moss,
  black peat, whisper reeds, glowing gloomshrooms, dead trees) and the
  **Ashen Reach** (ash dunes, bone-strewn, veilrock ridges). Between the
  landmasses lies still black **Murkwater** (animated, bridgeable — placing
  tiles reclaims murkmoss).
- **The Gloom Shade**: the Veil's first enemy, built on the new organic-mass
  construction standard — a hooded fen ghost with a hollow glowing face,
  claw arms and a wispy tail. Drops **Veil Essence**.
- **Forage & light**: gloomshrooms are replantable natural light; **Ash
  Bones** ribcages break into **Cinder Pearls**, which craft the
  **Ghost Lantern** (green-flame streetlamp, wire-toggleable, usable in any
  base). Veilrock drops plain stone.
- `veilstatus` admin diagnostics command; the integration test now generates
  and verifies the Veil too. Locale: English + German for everything.

## [0.2.7] — 2026-08-23

### Changed
- **Skystone Golem rebuilt from scratch** on vanilla mob construction
  principles (studied from the game's own heavy creatures): the body is now
  organic overlapping boulder masses with per-mass volumetric shading instead
  of stacked rectangles. Ribbed armor plates across the belly, shoulder-cap
  stones, articulated stomp walk (legs lift, arms swing), a head sunk between
  the shoulders under a heavy brow ledge with deep eye sockets and glowing
  pupils, chest cracks, moss clumps and a solid aetherium crystal spur with a
  lit tip. The profile view is a proper hunched ape posture with a big
  leading arm reaching toward the ground.
- Style guide gains the "round overlapping masses" construction rule; the
  same template is earmarked for the Ashen Reach creatures of v0.3.

## [0.2.6] — 2026-08-23

The "living world" release: light is safe again, the sky gets wildlife and
forage, and the stairway finally looks like its name.

### Fixed
- **Torchlight protects again.** v0.1 raised the mobs' spawn-light threshold
  so they could appear in daylight — which silently broke the game's core
  "light = safety" contract: enemies kept spawning inside lit, cleared areas.
  All three enemies are back on vanilla spawn-light rules (they spawn only in
  darkness), and every biome table now uses local caps (`addLimited`), so a
  cleared area STAYS calm instead of refilling endlessly.
- Difficulty is tiered per biome: Driftlands stay light (few Zephyr Rays),
  the Stormveil is the pressure biome (Wisps + Rays), Aurora Shoals spawn
  rare-but-hard Golems.

### Added
- **Ambient wildlife**, one species per biome, huntable for small forage
  drops: the **Cloudlamb** (Driftlands, windsilk), the **Glowmoth** (Aurora
  Shoals, aurora petal — the moths from the Warden's cat story), and the
  **Sparkbeetle** (Stormveil, storm shard). Bestiary icons included.
- **Forage plants** in the Driftlands: **Windwheat** (harvest 3, spin into
  1 Windsilk by hand) and the **Cloudberry Bush**, dropping **Cloudberries**
  — the sky's first food item (vanilla forage-food pattern, spoils).

### Changed
- **Stairway pair completely redrawn**: a grand pale flight with readable
  6px steps, iron handrail with posts, a cloud ring wrapping the climb, a
  soft light burst at the top and a marble base plate with corner balusters
  (sky-side version descends into the cloud deck). Bold new inventory icon.
- **Enemy detail pass** to the vanilla character bar: the Zephyr Ray gains
  wing-finger ridges, a diamond spine pattern and teal accent rows; the
  Skystone Golem gains armor-plate seams, pauldron caps, knuckle grooves, an
  aetherium crystal spur, moss clumps and a glowing chest rune; the Storm
  Wisp gains hollow eye voids with bright pupils, a jagged mouth crack,
  forked lightning arcs and longer trailing streamers.

## [0.2.5] — 2026-08-23

Art batch 2 — the quest landmarks reach vanilla detail density (measured
against vanilla banners and character sprites from playtest screenshots).

### Changed
- **Sky Warden redrawn**: the coat palette sat so close to the outline color
  that the figure rendered as a black cone. New material-separated look:
  lifted violet-grey coat ramp with shoulder rim light, center and skirt
  creases, iron belt with buckle, hip satchel, ragged hem with pale lining,
  two-row feather collar with bright tips, full hair crown + curtains, long
  beard, and a taller staff crowned by a riveted iron cage lantern with a
  glowing core and floating halo (all four facings + walk/swim frames).
- **Warden's beacon rebuilt** (off + on): two-step masonry base with mortar
  joints and cracks, tapered riveted iron column, flared brazier bowl, a
  four-rib cage with finial. Off shows dead coals, soot streaks and one
  faint ember; on burns with a white-teal core, flame tongues, sparks and a
  glow halo, with light catching the rivets and bands.
- **Skywatch banner redrawn**: iron mounting rod with rings and end caps,
  silver-bordered night cloth with fold shading, a bold two-pixel crescent
  moon + star emblem, swallowtail bottom with fringe tips.

## [0.2.4] — 2026-08-23

Art batch 1, driven by playtest + reference feedback: terrain with real material
character, an actual raven, chunkier clouds, and the gothic night-violet
direction made official (see the style guide's new "Art direction" section).

### Changed
- **Terrain splats rebuilt on vanilla's construction principle**: calm base +
  per-variant detail clusters (before: uniform speckle with all four full
  variants identical — "recolored" instead of distinct). Cloudturf gets chunky
  tuft clusters and cloud-moss patches, Skystone gets fissures and chipped
  facets, Stormslate becomes layered night-violet slate with electric charge
  veins, Gloomwood gets knots, nail heads and grain streaks.
- **Stormslate palette shifted to night violet** — the walkable purple night sky.
- **Mistsea clouds v2**: bigger rounded lobes, hard sunlit rim on every lobe's
  upper edge, dither removed — crisp cartoon-cloud read.
- **Gloomraven statue redrawn as an actual raven**: profile pose with a big pale
  hooked beak, eye ring, folded wing with sheen, long tail, two-step plinth.
  The inventory icon follows automatically.

### Fixed
- Sprite gallery: the viewer's CSS reset squashed oversized sheets — the Mistsea
  animation appeared to flicker and wide sheets rendered blurry/downscaled. All
  sheets now render unscaled and pixel-crisp.

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
