# Chapter 01 — Skyreach POI dossier

> "es fehlen weiterhin jegliche POIs, NPCs, besondere Plätze, Häuser etc. es
> gibt nur die 2-3 POIs die aber nie besonderen Loot haben oder neue Gegner
> oder irgendwas interessantes"
> — the player, and the whole brief for this document.

Design intent, not a record of what shipped (`docs/design/README.md`). Eleven
places for the four Skyreach sub-biomes, biased to the endgame, sized and
proportioned against the decoded reference presets in
`docs/references/presets/`. Nothing here is read by the game.

The Skyreach today contains exactly **one** building — the Warden's Spire. Every
other "place" the player finds is a road waystation composed per-tile by
`SkyLandscape`. That is why the world reads as scenery: there is nowhere to go
*into*, nothing to take out, and nobody to meet. These eleven exist to fix that
and nothing else. Every one of them answers the same three questions in writing:
**what does the player do here, what do they leave with, and who is standing in
it.**

---

## Contents

| # | Name | Footprint | Rarity | Sub-biome | The one thing |
|---|---|---|---|---|---|
| 1 | **Skywatch Wayside** | 11×9 (99) | common | Driftlands / any road | a Ledger page, every time |
| 2 | **The Shepherd's Fold** | 21×17 (357) | per region | Driftlands | Wren, the second recruitable NPC |
| 3 | **The Institute of Applied Falling** | 25×21 (525) | once per world | Driftlands | the joke, and the Aeronaut's Charm |
| 4 | **Nightfell Redoubt** | 25×25 (625) | per region | Stormveil | the hostile one; Sovereign Shard I |
| 5 | **The Aether Manufactory** | 27×21 (567) | once per world | Stormveil | the multi-room interior; Shard II |
| 6 | **The Sovereign's Anvil** | 29×29 (841) | once per world | Stormveil | the altar; the endgame arena |
| 7 | **The Passage Wayhouse** | 19×15 (285) | per region | Skyway Passages | the fast-travel network |
| 8 | **The Unopened Gate** | 27×23 (621) | once per world | Skyway Passages | the story beat; a Seraph grove |
| 9 | **The Prism Choir** | 21×21 (441) | per region | Aurora Shoals | the chime puzzle; Shard III |
| 10 | **The Serpent's Reef** | 25×25 (123 land) | common | Mistsea | an encounter on open cloud |
| 11 | **The Dew-Keeper's Hut** | 13×13 (169) | common | Aurora Shoals | a small empty house with a story |
| 12 | **The Skyway Toll-House** | 23×19 (437) | once per world | Skyway Passages | Magpie, the Tollwright, the vault |
| 13 | **The Grange Cellar** | 21×19 (399) | once per world | Driftlands | Halda, the Sourvat Bloom, the Round |
| 14 | **The Test Range** | 27×23 (621) | once per world | Stormveil | Vane, Prototype Nine, the Casing tier |

POIs 1–11 were designed from `DESIGN.md` Part V. **12–14 were added after
`docs/design/chapter-01-skyreach-cast.md` (the chapter brief) landed mid-task**;
they are the layouts for its three recruit sites, and §3.0 reconciles the two
documents line by line where they overlap.

Scale check: a landmark with no building at all (6 and 8), a small dwelling
(2 and 11), a multi-room interior (5, 12 and 14, and 4 secondarily), a hostile
place (4, 6, 12, 13 and 14), and one that is funny (3).

---

## 0. How to read this

### 0.1 Map legend

Every plan is drawn at **one character per tile**, local coordinates, origin
top-left, `x` right, `y` down — the same frame `Preset.setObject(x, y, …)` uses.
Shared characters:

```
  .   the preset writes NOTHING here — the terrain painter's own ground shows through
  ,   paved: the POI's designed floor (named per POI)
  =   interior floor (named per POI)
  +   marblechecker inlay (accent only, never a whole room)
  ;   dressed natural ground (terrace / verge)
  ~   Mistsea
  #   wall        D  door        O  window
  |   fence / balustrade         G  fence gate
  x   rock / rubble rim
```

Per-POI object letters are given in each section's own legend. A letter marks
the tile an object stands on; **wall decor** letters mark the floor tile the
decoration hangs from, never the wall itself.

### 0.2 Rotation, written once

Straight out of `WardenSpirePreset` and `docs/TECHNICAL_LEARNINGS.md`:

- **Furniture rotation is the direction the piece FACES.** `0` up, `1` right,
  `2` down, `3` left. A dresser under a north wall is `2`. A chair at a table is
  turned *toward* the table (`ChairObject.facesTable` checks exactly that tile;
  `DeskObject extends TableObject`, so a chair turned to a desk counts).
- **Wall decor rotation is where the WALL is.** `0` wall below, `1` wall left,
  `2` wall above, `3` wall right. Paintings (`skywatchbanner`) and wall lights
  (`mistglasslantern`, `flickerlightgarland`) live on
  `ObjectLayerRegistry.WALL_DECOR`; **their own tile must not be a wall**, or the
  banner replaces the masonry and opens a hole in the building.
- **Multi-tile pieces need BOTH halves written, with the same rotation.**
  `Preset.applyToLevel` never runs `MultiTile.placeObject`. The counter sits in
  the direction the rotation points. This dossier writes both halves in every
  object table:
  `skywatchbench`/`skywatchbench2`, `skywatchbed`/`skywatchbed2`,
  `skywatchdinnertable`/`skywatchdinnertable2`.
- **Crystal clusters are pairs too.** `CrystalClusterObject.registerCrystalCluster`
  makes `<id>` and `<id>r`; a cluster occupies two adjacent tiles and both are
  written (`stormcrystal` + `stormcrystalr`, `aurorabloom` + `aurorabloomr`).
- **Table decorations** (`skywatchchalice`, `skywatchcandle`, `skywatchtome`,
  `pottedcloudberry`) go on `ObjectLayerRegistry.FENCE_AND_TABLE_DECOR`, on top
  of a `ModularTableObject` tile. They cannot stand on bare floor.

### 0.3 Windows, written once

`WallWindowObject.getWindowDir` accepts a window only where the connected walls
form exactly one opposite pair: wall above **and** below with both sides open,
or wall left **and** right with both ends open. **A window in a corner is
silently deleted.** Every window in every plan below sits mid-run in a straight
wall; the plans were drawn with that rule in hand, and the integrator should
count `walls=n/n windows=m/m` per building the way `veilstatus` does.

### 0.4 Fences, written once

`FenceObject.attachesToObject` looks at the four **orthogonal** neighbours only.

- A **straight, axis-aligned** run one tile wide is a proper connected fence.
  Every straight run in this dossier is axis-aligned.
- A **ring, a curve or a diagonal** must use `SkyLandscape.discRing` — the
  8-neighbour inner boundary — or at minimum a band of
  `SkyLandscape.FENCE_MIN_THICKNESS = 1.6` tiles. A one-tile digital circle is a
  row of lone posts, which is what the player already complained about.
  POIs 6, 8 and 9 use rings; **their plans below were generated by running the
  real `discRing` predicate**, not drawn by eye, and every ring tile in them has
  two orthogonal neighbours.
- Where a road or a path crosses a ring, the opening is **one tile wide and
  holds a real `cloudmarblefencegate`**, so the ring stays a closed loop through
  the gate instead of having a hole punched in it.

### 0.5 The lighting rhythm, measured

Counted off the reference presets:

| reference | lights | tiles | rhythm |
|---|---|---|---|
| `ballroom` | 12 candelabra + 6 wall candles + 2 torches | 529 | 1 per 26 |
| `warden-tower-layout` | 12 candelabra | 441 | 1 per 37 |
| `cosy-cabin` | 0 (three windows instead) | 99 | daylight |
| `stubborn-garden` | 0 | 625 | open air |
| `dark-sword-seal` | 0 | 525 | open air |

**The rule this dossier follows:** interiors carry one light per **25–40** tiles;
open-air designed places light only the path, the threshold and the monument;
natural sites carry none beyond their own lit flora. A hostile place is
deliberately *under* the band (POI 4 runs 1 per 62) and the light is broken where
the building is broken. Every POI below states its count and its ratio.

### 0.6 Rarity, in terms the worldgen already has

| word | mechanism | how often the player meets one |
|---|---|---|
| **common** | a new *kind* on the existing `SkyLandscape` node lattice. `ROAD_CELL = 72`, `STATION_CHANCE = 0.66`, and ~66% of cells find land, so roughly **one designed place per 72×72-tile cell, two cells out of three** — the roads already lead to them. | every few minutes of walking |
| **per region** | a lattice-cell roll at ~1 in 8 cells, i.e. one per ~576×576 tiles | a handful per world; a reason to explore outward |
| **once per world** | a seed-derived site in a stated distance band from `SkyOrigin`, stamped lazily exactly the way `SkyLevel.ensureWardenSpire` stamps the Spire, and recorded in world data so it is never re-stamped | one, and the Ledger tells you where |

The three once-per-world places are deliberately hard to stumble on **and
deliberately findable**: POI 1's collectible marks all three on the map (§3.1).
An infinite world with unfindable content is the same as an empty one.

### 0.7 Power band

The mod's shipped band tops out at Stormsteel (helm 25 / cuirass 26 / greaves 16,
enchant 1300) — vanilla Tungsten. **This chapter sits at and past it.**

| POI | expected player state |
|---|---|
| 1, 2, 10, 11 | any — these are the world's connective tissue |
| 7 | any; the reward is travel, not power |
| 3 | Tungsten-era; the reward is mobility |
| 4, 9 | full Stormsteel set or vanilla Glacial; POI-exclusive elites hit harder than anything in the open sky |
| 5 | post-deep-cave-boss; the hall is the densest fight in the mod |
| 6, 8 | endgame. POI 6 is the arena the Storm Sovereign (ROADMAP v0.6) will be summoned into; it is designed to be complete and worth doing **before** that boss exists (§2.6). |

---

## 1. Reuse before you request — what already exists

Read this before reading a single POI. Almost everything below is built out of
pieces that are already registered, already have art, and already pass the
audits. The new-art order in §4 is short *because* of this table.

### 1.1 Registered and currently UNUSED by worldgen — free wins

These cost zero new pixels and are sitting idle:

| piece | what it is | used here by |
|---|---|---|
| `nightfellwall` / `nightfelldoor` / `nightfellwindow` | the dark half of the building set. **Nothing in worldgen places it today.** | POI 4 — an entire hostile keep for free |
| `charfloortile` | Charwood plank floor | POI 4, POI 6 |
| `nimbusfloortile` | Nimbus plank floor | POI 2, POI 3 |
| `prismfloortile` | Prismwood floor | POI 9, POI 11 |
| `skyballoon`, `aeronautwreck`, `skyparcel` | the sky oddities, deliberately kept out of worldgen so a discovery stays a discovery | POI 3 (their whole reason to exist), POI 10 |
| `skywatchtelescope`, `skywatchastrolabe` | observatory instruments | POI 5, POI 8 |
| `windsilkloom`, `aetherforge`, `stormglasskiln` | the three profession workstations | POI 5 (all three), POI 2 (the loom) |
| `skywatchbookshelf`, `skywatchcabinet`, `skywatchdisplay` | **real storage** (`BookshelfObject` 10 slots, `CabinetObject` 20 slots, `DisplayStandObject` 1 item) | every loot cache in this dossier |
| `stormscreed`, `skywatchrubble`, `chargecrystal`, `withershrub`, `aurorashards`, `starfall` | the v0.6 prop families, "worldgen composition is a later pass" | that pass is this dossier |

### 1.2 The full reuse pool

- **Walls (3 families, each wall + door + window):** `cloudmarble` (white and
  gold), `skystonebrick` (pale), `nightfell` (dark).
- **Floors:** `marblechecker` (accent only — a whole room of it swallows the
  screen), `gloomwoodfloor`, `nimbusfloor`, `charfloor`, `prismfloor`,
  `skywaytile` (terrain), `skyroad` (= vanilla `snowstonepathtile`).
- **Fences:** `skyironfence` + gate, `cloudmarblefence` + gate.
- **Lights:** `wardencandelabra` and `ghostlantern` (`StreetlampObject`),
  `skywatchcandelabra` (`CandelabraObject`, furniture), `mistglasslantern` and
  `flickerlightgarland` (`SkyWallLightObject`, wall decor), plus the lit props
  `chargecrystal`, `aurorashards`, `starfall`, `gloomshroom`.
- **Statues:** `seraphstatue` (96×192, three tiles wide on one tile),
  `gloomravenstatue`.
- **Furniture (the Skywatch family, all on vanilla base classes):** chair,
  bench(+2), modular table, dinner table(+2), desk, dresser, bed(+2),
  candelabra, carpet, chalice, candle, tome, potted cloudberry, bookshelf,
  cabinet, clock, display stand. `skywatchbanner` is the family's painting.
- **Natural:** `skystonerock`, `aetheriumrock`, `fulguriterock`,
  `prismshardrock`, `stormcrystal`(+r), `aurorabloom`(+r), the five trees and
  their saplings, nine pickable plants, three meadow grasses, `skyreeds`,
  `windwheat`, `cloudberrybush`.
- **Vanilla, reachable and already used by `AeronautCampPreset`:** `barrel`,
  `oakchest`, `sign` (+ `SignObjectEntity.setMessage`), `campfire`,
  `feedingtrough`, `cookingpot`, `bigtent` quad, `oillantern`.
- **Loot into any of it:** `Preset.addInventory(LootTable, random, x, y)` fills
  whatever container object entity stands on that tile — this is how
  `CrookedHousePreset` loots its barrel and `AeronautCampPreset` its chest.

### 1.3 Enemies: how to get "new Gegner" for almost nothing

`content/arsenal` already proved the pattern (`docs/TECHNICAL_LEARNINGS.md`,
"A mod mob can wear a vanilla body sheet without shipping any art"): subclass the
vanilla mob whose behaviour you want, wear that mob's own sheet from
`MobRegistry.Textures`, and ship **one 32×32 bestiary icon**. Rime Sentry,
Aurora Flake, Fen Wraith and Cinder Cantor all cost exactly one icon each.

This dossier asks for **one** genuinely new mob sheet — the Skywatch Revenant,
because it appears at four POIs and carries the chapter's story — and takes the
other two POI-exclusive enemies on the arsenal pattern.

---

## 2. The eleven

### 2.1 Skywatch Wayside

**`waysideshrine` · 11×9 = 99 tiles · COMMON · Driftlands and any biome the road
network crosses.**

99 tiles is `cosy-cabin`'s footprint, and the echo is deliberate: this is the
smallest thing in the chapter and it still has to feel authored. It is a paved
pocket off the road with a balustrade on three sides, benches with their backs to
the rail, a stele in the middle and a locked offering cabinet. The player passes
one every few minutes. **It always has something in it.**

```
       0123456789A
  y0   ...........
  y1   .|||||||||.
  y2   .|Bb,,,Bb|.
  y3   .|v,,S,,w|.
  y4   .|,,,,,,,|.
  y5   .|k,,c,,r|.
  y6   .|,,,,,,,|.
  y7   .L,,,,,,,L.
  y8   ...........
```

```
  ,  skyroad paving (= vanilla snowstonepathtile), x1..9 × y2..7
  |  cloudmarblefence — three straight axis-aligned runs, open to the road at the south
  B/b skywatchbench + skywatchbench2      S  skywatchstele        k  skywatchcabinet
  c  skywatchcandelabra                   L  wardencandelabra
  v  skytulip   w  cloudbell   r  cloudberrybush
```

**Objects, with rotations**

| tile | object | rot | note |
|---|---|---|---|
| (1,1)–(9,1) | `cloudmarblefence` ×9 | — | straight run |
| (1,2)–(1,6) | `cloudmarblefence` ×5 | — | straight run |
| (9,2)–(9,6) | `cloudmarblefence` ×5 | — | straight run |
| (2,2) / (3,2) | `skywatchbench` / `skywatchbench2` | 1 / 1 | both halves, same rotation; back to the north rail |
| (7,2) / (8,2) | `skywatchbench` / `skywatchbench2` | 1 / 1 | same |
| (5,3) | `skywatchstele` | 2 | faces down, readable from the road |
| (2,5) | `skywatchcabinet` | 1 | faces east, back to the west rail |
| (5,5) | `skywatchcandelabra` | — | |
| (1,7) / (9,7) | `wardencandelabra` ×2 | — | flank the entrance where the rail stops |
| (2,3) `skytulip`, (8,3) `cloudbell`, (8,5) `cloudberrybush` | | — | planting, not scatter |

**Lighting:** 3 (one candelabra, two streetlamps) in 99 tiles = **1 per 33**.

**What the player does, and leaves with.** Reads the stele — each Wayside carries
one numbered fragment of the **Warden's Ledger**, in the mod's own dry voice —
and opens the offering cabinet. The cabinet always contains one
**Warden's Ledger Page** plus a small cache:

```
LootTable( LootItem.between("skystone", 4, 10),
           ChanceLootItem(0.50, "aetheriumore", 1..3),
           ChanceLootItem(0.40, "stormshard", 1..2),
           ChanceLootItem(0.35, "cloudpufftreat", 1..2),
           ChanceLootItem(0.45, "coin", 60..200),
           LootItem("wardenledger") )
```

Seven distinct pages turned in to the Warden = §3.1. That is the whole point of
making this one common: it is the drip-feed that eventually points at the three
once-per-world places.

**Inhabitants:** none. Zephyr Finches land on the rail.

**New art:** `skywatchstele` only (shared with six other POIs).

---

### 2.2 The Shepherd's Fold

**`shepherdsfold` · 21×17 = 357 tiles · PER REGION · Driftlands.**

The Sky Cottage that `DESIGN.md` Part V promised and never got, plus the fenced
pasture that makes it a *place* instead of a house. Proportions from
`stubborn-garden`: the plot is large and **more than half of it is deliberately
empty grazing**. The cottage itself is `cosy-cabin` scale.

```
       0         1         2
       012345678901234567890
  y0   .....................
  y1   .|||||||||||||||||||.
  y2   .|.......####O####.|.
  y3   .|.T.....#W======#.|.
  y4   .|......LO=====eE#.|.
  y5   .|.......#ch=====#.|.
  y6   .|..w....#=mmh==rO.|.
  y7   .|.....bb#=h=====#.|.
  y8   .|..u...bO=====c=#.|.
  y9   .|.......#k===os=#.|.
  y10  .|.......####D####.|.
  y11  .|ffGff.....,,,....|.
  y12  .|f...f....L,,,L...|.
  y13  .|f...fT....,,,....|.
  y14  .|f...f.....,,,....|.
  y15  .ffffffffffffGffffff.
  y16  .....................
```

> The `|` on the right edge of rows y2..y14 is the plot fence at x19; the cottage
> east wall is x17, with a one-tile alley at x18 between them. Row y15 is the
> plot's south fence, x1..x19, broken by the gate at x13 where the road arrives.
> The north run at y1 passes along the cottage's back wall; a fence tile meeting
> a wall tile is fine — the fence sheet's column 2 exists precisely to bridge
> into one (`docs/TECHNICAL_LEARNINGS.md`).

```
  |  skyironfence (outer plot, straight axis-aligned runs)   G  skyironfencegate
  f  skyironfence (the lambing pen)      ,  skyroad path, x12..14 × y11..15
  #  skystonebrickwall   O  window   D  door    =  nimbusfloortile
  W  windsilkloom   e/E  skywatchbed2 / skywatchbed   r  skywatchdresser
  m  skywatchmodulartable   h  skywatchchair   k  skywatchcabinet
  s  skywatchbookshelf   o  skywatchclock   c  skywatchcandelabra
  L  wardencandelabra   w  cloudspringfont   u  feedingtrough (vanilla)
  T  cloudtree   b  cloudberrybush
```

**The cottage** — outer x9..17 × y2..10, interior x10..16 × y3..9 (49 tiles).
Four windows, all mid-run in a straight wall: (13,2) north (dir 0), (9,4) and
(9,8) west (dir 1), (17,6) east (dir 1). Door on the south at (13,10), so the
path from the gate runs straight to it.

| tile | object | rot | note |
|---|---|---|---|
| (10,3) | `windsilkloom` | 1 | her trade; a settler will actually run it |
| (16,4) / (15,4) | `skywatchbed` / `skywatchbed2` | 3 / 3 | both halves; headboard to the east wall |
| (16,6) | `skywatchdresser` | 3 | back to the east wall |
| (11,6) (12,6) | `skywatchmodulartable` ×2 | — | one two-tile table |
| (11,6) / (12,6) | `skywatchchalice` / `skywatchtome` | — | FENCE_AND_TABLE_DECOR, on the tables |
| (11,5) | `skywatchchair` | 2 | faces the table below |
| (13,6) | `skywatchchair` | 3 | faces the table to its left |
| (11,7) | `skywatchchair` | 0 | faces the table above |
| (10,9) | `skywatchcabinet` | 0 | back to the south wall — **the loot** |
| (15,9) | `skywatchbookshelf` | 0 | back to the south wall |
| (14,9) | `skywatchclock` | 0 | |
| (10,5), (15,8) | `skywatchcandelabra` ×2 | — | |
| (11,3), (15,3) | `skywatchbanner` | 2 (wall above) | WALL_DECOR; own tile is floor |
| carpet x11..13 × y6..8 | `skywatchcarpet` | — | TILE_LAYER, 9 tiles |

**The fold** — everything west of x9. Almost all of it is empty on purpose:
`tallcloudgrass` at ~60% coverage over x2..8 × y2..10 (walk-through, the meadow
read `DESIGN.md` Part V asks for), two `cloudtree`, a `cloudberrybush` cluster at
(7,7)(8,7)(8,8), the `cloudspringfont` at (4,6) and a vanilla `feedingtrough` at
(4,8) — the trough matters, because `cloudberry` is a `GrainItem` and the sky
finally has feed of its own. The lambing pen is x2..6 × y11..14, four straight
fence runs with a gate at (4,11).

**Lighting:** 2 inside (1 per 24 interior tiles) + 3 outside on the path and the
yard = 5 in 357 = 1 per 71 across the plot. The pasture is unlit on purpose.

**Inhabitants.** **Wren, the Cloud Shepherd** — a `HumanShop` on the
`WardenSettlerMob` pattern — stands at **(13,11)**, on the path outside her own
door, facing south down the road so the player meets her face rather than her
back (the Warden's own P1 bug, not repeated). Four to six **Cloud Lambs** and one
**Nimbus Yak** graze the fold; the flock is placed the way
`SkyLevel.placeCloudLambFlock` already places one.

> **Note, 2026-09-02 — the Cloud Lamb this POI is written around is gone.**
> The mob was removed (it could never breed and its spawn table entry was
> inert — see `docs/OVERVIEW.md` §3); the **Glimmergoat** is the sky's fibre
> animal now. `SkyLevel` no longer has a `placeCloudLambFlock` method — the
> current pattern is the generic `placeHerd(region, mobID, groundID, chance,
> salt)`, already called for `SkyLivestock.NIMBUS_YAK` and
> `SkyLivestock.GLIMMERGOAT`. If this POI is built, read the fold as
> Glimmergoats (or reuse both existing herds) rather than resurrecting the
> Cloud Lamb; the brief below is left as written per `docs/design/README.md`.

**What the player does, and leaves with.**

1. **A shop that sells what the sky cannot otherwise buy:** `cloudsapling` and
   `nimbussapling`, `cloudpufftreat`, `windsilk` and `skyweave`, `cloudberry`
   seed stock, and — the headline — **a Cloud Lamb and a Nimbus Yak she will
   part with**, so husbandry in the sky no longer depends on finding a wild one.
2. **"The lost lamb"** — one small journal quest. A lamb wandered onto the
   nearest Stormveil crag and is standing in a Galehound's hunting ground. Bring
   it back: reward is 3 `cloudpufftreat`, a Nimbus Yak calf, and **her
   recruitment unlocked**.
3. **Wren stays in the sky.** She is a shopkeeper and quest-giver, **not** a
   recruitable settler — the chapter brief reserves recruitment for Magpie, Halda
   and Vane, and its rule is that every settler *found* up here *moves down*
   (`DESIGN_DECISIONS.md`: the Skyreach is an exploration layer, not a second
   base). Wren is the exception that proves it: she has a fold, a flock and a
   reason to stay, and she is the friendly face a player meets **before** the
   three recruits, in the common biome, early. `DESIGN.md` Part V promised a
   Cloud Shepherd at a Sky Cottage; this is her, and she does not compete with
   the brief's cast.

**New art:** Wren's wardrobe (§4.4). Everything else is reuse.

---

### 2.3 The Institute of Applied Falling

**`fallinginstitute` · 25×21 = 525 tiles · ONCE PER WORLD · Driftlands, in the
middle distance band from `SkyOrigin`.**

The funny one. 525 tiles is `dark-sword-seal`'s footprint and it keeps that
preset's discipline: **425 of them are untouched Driftlands meadow**, and
the whole POI is a launch ramp, a falling line of increasingly ambitious
machines, and a crater.

`IMPLEMENTATION_RULES.md` §10 names exactly this register for the Skyreach —
"lost aeronaut equipment, improbable sky debris, zeppelin/balloon ideas, odd
historical flying attempts" — and it uses the three sky oddities the mod already
registered and then deliberately kept out of worldgen. This is where they were
always going.

```
       0         1         2
       0123456789012345678901234
  y0   .........................
  y1   .........................
  y2   .....L....L....L.........
  y3   ..s=============.........
  y4   ..=============..........
  y5   .................W.......
  y6   .................s.......
  y7   ...................b.....
  y8   ...................p.....
  y9   ..................W......
  y10  ..................s......
  y11  ................p........
  y12  ...............Wr........
  y13  ..............s..........
  y14  .........Lxxxxxxxxx......
  y15  .........x########x......
  y16  .........xWk######x......
  y17  .........x###Y####x......
  y18  .........x########x......
  y19  .........xxxxxxxxxL......
  y20  .........................
```

```
  =  nimbusfloortile decking — the launch ramp, x3..15 × y3..4
  s  skywatchstele (the numbered test log)     L  wardencandelabra
  W  aeronautwreck   b  skyballoon   p  skyparcel   r  skywatchrubble
  x  skyscree + skystonerock — the crater rim   #  skystonetile crater floor
  k  skywatchcabinet — the reward     Y  the Nimbus Yak (a mob, not an object)
```

**The composition.** A 2×13 plank deck runs east and stops in mid-air. From its
end, six wrecks fall away south-east in a widening arc — `aeronautwreck` (5,17),
`skyballoon` (7,19), `skyparcel` (8,19), `aeronautwreck` (9,18),
`skyparcel` (11,16), `aeronautwreck` (12,15) with `skywatchrubble` (12,16) —
each one further out and further down than the last, and the arc terminates in a
crater of bare skystone. Four steles stand beside the wrecks; each carries one
entry of the test log. Nothing between the deck and the crater is written at all:
the meadow the terrain painter already grows is the space the joke needs.

**Objects, with rotations**

| tile | object | rot |
|---|---|---|
| (3,3)–(15,3), (3,4)–(15,4) | `nimbusfloortile` deck, 26 tiles | — |
| (2,3) | `skywatchstele` — the Institute's plaque | 1 (faces east, up the ramp) |
| (5,2), (10,2), (15,2) | `wardencandelabra` ×3 | — |
| (17,5), (18,9), (15,12) | `aeronautwreck` | — |
| (19,7) | `skyballoon` | — |
| (19,8), (16,11) | `skyparcel` | — |
| (16,12) | `skywatchrubble` | — |
| (17,6), (18,10), (14,13) | `skywatchstele` ×3 — test log II, IV, VI | 2 |
| (9,14), (18,19) | `wardencandelabra` ×2 | — |
| (10,16) | `aeronautwreck` — Test VII, what is left of it | — |
| (11,16) | `skywatchcabinet` | 2 |
| crater rim | `skyscree` / `skystonerock`, the `x` cells | — |

**Lighting:** 5 in 525 = 1 per 105 across the plot — but the ramp, which is the
only *room*, runs 3 lamps over 26 tiles (1 per 9, a lit walkway), and the crater
carries 2. The empty middle is unlit on purpose; that is where you are falling.

**What the player does, and leaves with.** Walks the ramp, reads four steles in
descending order, follows the wreckage down, and finds the cabinet in Test VII.
The joke lands on the last beat: **Test Subject VII is a Nimbus Yak standing in
the exact centre of the crater, completely unharmed, chewing.** It is still
there. It has always been there. The humans are not.

Loot: **the Aeronaut's Charm** (`aeronautcharm`, a `TrinketItem` on a
`SimpleTrinketBuff` so the engine prints its own numbers) plus
`aetheriumbar` 2–4 and `stormsteelbar` 1–2. Design intent for the Charm: the
Institute finally succeeded, posthumously, at *falling slowly* — the wearer
**crosses the Mistsea at walking speed instead of wading**, which in an
archipelago world is a real traversal upgrade. If that is not reachable on a
vanilla buff, the fallback is +12% movement speed and +40% stamina, calibrated
against `trackerboot` and `zephyrcharm` the way the existing three trinkets are.
Its tooltip is the last line of the test log.

**Inhabitants:** one Nimbus Yak at **(13,17)**, on the crater floor. It is not
hostile, it is not a quest, and it does not move away. It is the punchline.

**And it has a consequence.** The last stele, at (14,13), closes the log:
*"the Board has relocated testing to the Stormveil, where the weather is more
honest."* That is the pointer to **POI 14, the Test Range** — same order, second
site, and nobody is laughing there. A joke with a sequel is worth more than two
jokes.

**New art:** `skywatchstele`, `aeronautcharm` icon. The wrecks, the balloon and
the parcel all already exist.

---

### 2.4 Nightfell Redoubt

**`nightfellredoubt` · 25×25 = 625 tiles · PER REGION · Stormveil.**

**The hostile one.** `stubborn-garden`'s exact footprint and exact discipline: a
walled compound, doors on all four axes, 3×3 corner buttresses, and a court that
is left **almost entirely empty**. Counted off the plan: 264 tiles untouched
outside the ring, 115 wall tiles, and **230 tiles of charwood floor inside — 190
of them the open court.** You fight across an empty square. Built out of the `nightfell` wall family, which has been
registered and craftable for four releases and has never once been placed by
worldgen.

```
       0         1         2
       0123456789012345678901234
  y0   .........................
  y1   .........................
  y2   .........................
  y3   ...#####O###D###O#####...
  y4   ...###=q=========q=###...
  y5   ...###=============###...
  y6   ...#=#######=========#...
  y7   ...#q#kk=sc#========q#...
  y8   ...O=#=====#=========O...
  y9   ...#=O=====#=========#...
  y10  ...#=#Bb==v#====L====#...
  y11  ...#=###D###=========#...
  y12  ...D======S=P=S======D...
  y13  ...#=====g=rrr=g=====#...
  y14  ...#=========###D###=#...
  y15  ...#=========#==m=r#=#...
  y16  ...O====L====#eE===#=O...
  y17  ...#q========#==c==#q#...
  y18  ...#=========#k==eEO=#...
  y19  ...###=======#########...
  y20  ...###=q=========q=###...
  y21  ...#####O###D###O#####...
  y22  .........................
  y23  .........................
  y24  .........................
```

```
  #  nightfellwall   O  nightfellwindow   D  nightfelldoor   =  charfloortile
  q  mistglasslantern (WALL_DECOR)     L  wardencandelabra   c  skywatchcandelabra
  k  skywatchcabinet  s  skywatchbookshelf  v  barrel (vanilla)
  B/b skywatchbench + skywatchbench2   e/E skywatchbed2 / skywatchbed
  m  skywatchmodulartable   r  skywatchdresser
  S  stormscreed   r (centre, y13)  skywatchrubble   g  chargecrystal
  P  skywatchdisplay — an EMPTY pedestal
```

**The shell.** Outer ring x3..21 × y3..21 with 3×3 buttresses at each corner
(the spire's own idiom, so the ring reads as masonry rather than a fence). Doors
on the four axes at (12,3), (12,21), (3,12), (21,12). Eight windows, two per
side, each mid-run between a door and a buttress: (8,3) (16,3) (8,21) (16,21)
(3,8) (3,16) (21,8) (21,16). Interior floor `charfloortile`.

**Two blockhouses inside, not one big room.** The armoury x5..11 × y6..11 (door
at (8,11), window at (5,9)) and the bunkroom x13..19 × y14..19 (door at (16,14),
window at (19,18)). Between the outer ring and each blockhouse runs a one-tile
circulation corridor — the same double-ring-with-a-corridor grammar as
`warden-tower-layout`, at a quarter of the ceremony.

**Objects, with rotations**

| tile | object | rot | note |
|---|---|---|---|
| (6,7), (7,7) | `skywatchcabinet` ×2 | 2, 2 | backs to the armoury's north wall — **the vault** |
| (9,7) | `skywatchbookshelf` | 2 | |
| (10,7) | `skywatchcandelabra` | — | |
| (6,10) / (7,10) | `skywatchbench` / `skywatchbench2` | 1 / 1 | both halves |
| (10,10) | `barrel` (vanilla) | — | |
| (15,16) / (14,16) | `skywatchbed` / `skywatchbed2` | 3 / 3 | both halves |
| (18,18) / (17,18) | `skywatchbed` / `skywatchbed2` | 3 / 3 | both halves |
| (14,18) | `skywatchcabinet` | 0 | back to the bunkroom's south wall |
| (16,15) | `skywatchmodulartable` | — | with `skywatchtome` on FENCE_AND_TABLE_DECOR |
| (18,15) | `skywatchdresser` | 3 | back to the east wall |
| (16,17) | `skywatchcandelabra` | — | |
| (12,12) | `skywatchdisplay` | — | **left empty on purpose** |
| (10,12), (14,12) | `stormscreed` ×2 | — | the mast's footings |
| (11,13) (12,13) (13,13) | `skywatchrubble` ×3 | — | where it came down |
| (9,13), (15,13) | `chargecrystal` ×2 | — | lit |
| (7,4), (17,4) | `mistglasslantern` | 2 (wall above) | WALL_DECOR |
| (7,20), (17,20) | `mistglasslantern` | 0 (wall below) | WALL_DECOR |
| (4,7), (4,17) | `mistglasslantern` | 1 (wall left) | WALL_DECOR |
| (20,7), (20,17) | `mistglasslantern` | 3 (wall right) | WALL_DECOR |
| (16,10), (8,16) | `wardencandelabra` ×2 | — | the court |

**Lighting:** 8 `mistglasslantern` on the inner faces of the ring plus 2
`wardencandelabra` in the court = **10 lamps in 625, 1 per 62** — deliberately
*under* the 25–40 band, with 2 `chargecrystal` at the fallen mast as the only
bright thing in the middle. The rhythm is broken where the building is broken:
the ring's eight lanterns are evenly spaced and the court between them is not
lit at all. Each blockhouse runs one candelabra over ~20 interior tiles, so the
two rooms you fight *in* stay legible.

**Inhabitants — and this is the answer to "keine neuen Gegner".**

| mob | count | where it stands |
|---|---|---|
| **Skywatch Revenant** (new) | 4 | (8,8) and (9,9) in the armoury; (12,16) in the court; (16,16) in the bunkroom |
| **Fulgur Shade** (new, arsenal pattern) | 3 | (6,15), (18,7), (12,19) — they drift the empty court |
| Skystone Golem | 2 | (5,13), (19,13), on the corridor ring |
| Rime Sentry | 2 | (12,5), (12,19) flanking the north and south doors |

Nothing spawns inside a lit blockhouse until the player opens its door: the
compound is a fight the player chooses to start, not an ambush they walk into.

**What the player does, and leaves with.** Breaches the compound (any of four
doors — the composition gives the player a choice of approach, which is the point
of putting doors on all four axes), clears the court, then the two blockhouses.
The armoury's two cabinets and the barrel hold:

```
LootTable( LootItem.between("stormsteelbar", 2, 5),
           LootItem.between("aetheriumbar", 2, 4),
           LootItem.between("stormglass", 2, 6),
           ChanceLootItem(0.35, "stormdisc" | "skyreave" | "thunderhead"),
           ChanceLootItem(0.60, "coin", 400..1200),
           LootItem("sovereignshard") )     // Shard I of III
```

The empty pedestal in the middle of the court is the story: somebody already came
for whatever stood on it. That is a **story beat delivered with a prop the mod
already has and zero words**.

**New art:** `skywatchrevenant` sheet + icon; `fulgurshade` icon; `sovereignshard`
icon. Zero new walls, zero new floors, zero new furniture.

---

### 2.5 The Aether Manufactory

**`aethermanufactory` · 27×21 = 567 tiles · ONCE PER WORLD · Stormveil, outer
distance band.**

**The multi-room interior**, and a direct answer to the player's own reference
screenshot: *a large multi-room crystal complex with storage and machinery.* One
building, three rooms, and the `ballroom` lesson applied — **grandeur comes from
one very large continuous floor treatment, not from more furniture.** A single
5×13 Skywatch carpet runs the length of the machine hall (65 tiles, 31% of the
hall), and everything else stands back from it.

```
       0         1         2
       012345678901234567890123456
  y0   ...........................
  y1   ...........................
  y2   ..####O######O######O####..
  y3   ..#===q=====rq===#=kkk==#..
  y4   ..#====:::::=====#====c=#..
  y5   ..#F===:::::====g#======#..
  y6   ..O====:::::===S=D=====vO..
  y7   ..#q===:::::=XX=q#======#..
  y8   ..#====:::::=====#======#..
  y9   ..#K===:::::====g#=ss===#..
  y10  ..D====:::::===S=###D####..
  y11  ..#q===:::::====q#====n=#..
  y12  ..#====:::::=====#=dh==o#..
  y13  ..#W===:::::====gD======#..
  y14  ..O====:::::===S=#====c=O..
  y15  ..#====:::::XX===#===P==#..
  y16  ..#==rr:::::=====#=mm===#..
  y17  ..#===q======q===#======#..
  y18  ..####O##D##########O####..
  y19  ...........................
  y20  ...........................
```

> The partition at x17 runs y3..y17; its two doors read as `D` at (17,6) in row
> y6 and (17,13) in row y13. The east wing's own partition is the `###D####`
> stretch in row y10 — x17 and x24 are the building's walls, x18..x23 is the
> partition with its door at (20,10).

```
  #  skystonebrickwall   O  window   D  door   =  gloomwoodfloortile
  :  skywatchcarpet over gloomwood — one continuous 5×13 runner, x7..11 × y4..16
  F  aetherforge   K  stormglasskiln   W  windsilkloom
  g  chargecrystal   S  stormscreed   X  stormcrystal / stormcrystalr (a pair)
  r  skywatchrubble   q  mistglasslantern (WALL_DECOR)   c  skywatchcandelabra
  k  skywatchcabinet   s  skywatchbookshelf   v  barrel (vanilla)
  d  skywatchdesk   h  skywatchchair   o  skywatchclock   m  skywatchmodulartable
  P  skywatchdisplay   n  skywatchbanner (WALL_DECOR)
```

**The shell.** Outer x2..24 × y2..18. Grand door on the south at (9,18); a
service door on the west at (2,10). Eight windows, all mid-run: (6,2) (13,2)
(20,2) north, (6,18) (20,18) south, (2,6) (2,14) west, (24,6) (24,14) east. A
north-south partition at x17 (y3..17) with doors at (17,6) and (17,13); an
east-west partition at y10 (x18..23) with a door at (20,10).

**Three rooms.**

- **The machine hall**, x3..16 × y3..17 (210 tiles). The three profession
  workstations stand in a row along the west wall, facing into the hall:
  `aetherforge` (3,5) rot 1, `stormglasskiln` (3,9) rot 1, `windsilkloom`
  (3,13) rot 1. Opposite them, against the partition, three `chargecrystal` at
  (16,5) (16,9) (16,13) and three `stormscreed` piles at (15,6) (15,10) (15,14).
  Two `stormcrystal` + `stormcrystalr` pairs burst up through the plank floor at
  (13,7)/(14,7) and (12,15)/(13,15) — **both halves written**, which is what
  gives the room the "crystal complex" read the player asked for. Two
  `skywatchrubble` at (5,16) (6,16) where the roof let go. The carpet runs
  straight down the middle and nothing stands on it.
- **The store**, x18..23 × y3..9 (42 tiles). Three `skywatchcabinet` at (19,3)
  (20,3) (21,3) rot 2, two `skywatchbookshelf` at (19,9) (20,9) rot 0, a vanilla
  `barrel` at (23,6), one `skywatchcandelabra` at (22,4).
- **The counting room**, x18..23 × y11..17 (42 tiles). `skywatchdesk` (19,12)
  rot 1 with `skywatchchair` (20,12) rot 3 turned to it (a `DeskObject` **is** a
  `TableObject`, so the chair genuinely faces a table); `skywatchclock` (23,12)
  rot 3; two `skywatchmodulartable` at (19,16) (20,16) carrying `skywatchtome`
  and `skywatchchalice`; `skywatchcandelabra` (22,14); `skywatchbanner` at
  (22,11) rot 2 (wall above); and on the `skywatchdisplay` at (21,15) —

**What the player leaves with.** The display stand holds **Sovereign Shard II**.
The three cabinets and the barrel hold the deepest material cache in the mod:

```
LootTable( LootItem.between("aetheriumbar", 4, 9),
           LootItem.between("stormsteelbar", 3, 7),
           LootItem.between("stormglass", 4, 10),
           LootItem.between("skyweave", 3, 6),
           ChanceLootItem(0.50, "stormsteelvambrace" | "auroralocket" | "zephyrharness"),
           ChanceLootItem(0.70, "coin", 800..2400) )
```

and the tome on the desk is the last work order the Manufactory ever received:
**assemble the Key.** That is the flavour that tells the player the three Shards
are for something, and where to take them (§3.2).

The hall also **teaches the professions by showing them working**. A player who
has never built an Aether Forge walks into a room with all three stations in a
row, each with its output on the floor beside it. `IMPLEMENTATION_RULES.md` §9
calls that progression relevance; it is also just how vanilla teaches.

**Lighting:** 8 `mistglasslantern` on the hall's four inner faces —
(6,3) (13,3) rot 2, (6,17) (13,17) rot 0, (3,7) (3,11) rot 1, (16,7) (16,11)
rot 3 — plus 3 `chargecrystal` in the hall and 2 `skywatchcandelabra` in the
wings. **13 lights in 567 = 1 per 44; the hall alone is 11 over 210 = 1 per 19.**
This is a working building and it is the brightest interior in the chapter.

**Inhabitants:** 3 Skywatch Revenants at (8,6), (10,12), (20,7); 2 Rime Sentries
at (5,15), (15,4). The hall is the densest fight the mod has.

**New art:** none beyond `skywatchrevenant` (already counted) and the
`sovereignshard` icon (already counted). Every workstation, every stick of
furniture, every wall and floor in this building already exists.

---

### 2.6 The Sovereign's Anvil

**`sovereignsanvil` · 29×29 = 841 tiles · ONCE PER WORLD · Stormveil, far
distance band.**

**A landmark with no building at all** — the second of two, and the endgame one.
`dark-sword-seal` proves a memorable place can be one prop plus deliberate
framing; this is that at arena scale. 352 of the 841 tiles are untouched; the
whole POI is a shattered slate bowl, a balustrade, four Seraphs and **one altar**.

The plan below was generated by running the mod's own `SkyLandscape.discRing`
predicate, so every balustrade tile has exactly two orthogonal neighbours and
there is not one lone post in it.

```
       0         1         2
       01234567890123456789012345678
  y0   .............................
  y1   .............................
  y2   ...........xxxLxxx...........
  y3   .........xxx;;;;;xxx.........
  y4   .......xxx;;;;;;;;;xxx.......
  y5   ......xx;;;|||G|||;;;xx......
  y6   .....xx;;|||,,,,,|||;;xx.....
  y7   ....xx;;||,,,,,,,,,||;;xx....
  y8   ....x;;|A,,,g,,,g,,,A|;;x....
  y9   ...xx;||,,,,,,,,,,,,,||;xx...
  y10  ...x;;|,,,,,,,,,,,,,,,|;;x...
  y11  ..xx;||,,,,,,,,,,,,,,,||;xx..
  y12  ..x;;|,,g,,,+++++,,,g,,|;;x..
  y13  ..x;;|,,,,,,+++++,,,,,,|;;x..
  y14  ..L;;G,,,,,,++V++,,,,,,G;;L..
  y15  ..x;;|,,,,,,+++++,,,,,,|;;x..
  y16  ..x;;|,,g,,,+++++,,,g,,|;;x..
  y17  ..xx;||,,,,,,,,,,,,,,,||;xx..
  y18  ...x;;|,,,,,,,,,,,,,,,|;;x...
  y19  ...xx;||,,,,,,,,,,,,,||;xx...
  y20  ....x;;|A,,,g,,,g,,,A|;;x....
  y21  ....xx;;||,,,,,,,,,||;;xx....
  y22  .....xx;;|||,,,,,|||;;xx.....
  y23  ......xx;;;|||G|||;;;xx......
  y24  .......xxx;;;;;;;;;xxx.......
  y25  .........xxx;;;;;xxx.........
  y26  ...........xxxLxxx...........
  y27  .............................
  y28  .............................
```

```
  x  the rim: skystonerock, skyscree and skywatchrubble on discRing(r=12) — 92 tiles
  ;  stormslatetile terrace with stormsedge — 100 tiles
  |  cloudmarblefence on discRing(r=9) — 64 tiles, one closed loop
  G  cloudmarblefencegate ×4, one tile wide, on the four axes
  ,  charfloortile — the arena floor, 188 tiles, DELIBERATELY EMPTY
  +  marblechecker plinth, 5×5 = 24 tiles (accent scale, never a room)
  V  sovereignaltar     A  seraphstatue ×4     g  chargecrystal ×8
  L  wardencandelabra ×4, one outside each gate
```

**Objects, with rotations**

| tile | object | rot |
|---|---|---|
| (14,14) | `sovereignaltar` | 2 (faces the south approach) |
| (8,8) (20,8) (8,20) (20,20) | `seraphstatue` ×4 | — (statues face out of their own sheet) |
| (16,8) (20,12) (20,16) (16,20) (12,20) (8,16) (8,12) (12,8) | `chargecrystal` ×8 | — |
| (14,5) (14,23) (5,14) (23,14) | `cloudmarblefencegate` ×4 | — |
| (14,2) (14,26) (2,14) (26,14) | `wardencandelabra` ×4 | — |
| the `|` cells | `cloudmarblefence` ×64 | — | 8-neighbour ring, **not** an annulus |
| the `x` cells | `skystonerock` / `skyscree` / `skywatchrubble` | — | scattered as a formation, ~55% coverage |

**Lighting:** 8 charge crystals + 4 gate lamps = **12 in 841 = 1 per 70.** The
arena is dim at the rim and lit at the ring, which is exactly what an arena wants.
The plinth carries no light: the altar is what lights it.

**What the player does, and leaves with.** Brings the assembled **Sovereign Key**
(§3.2) and sets it in the altar. The gates shut, and three waves come — Skywatch
Revenants, then Fulgur Shades, then a Revenant elite. Clear it and the altar pays
out the chapter's top cache and stays usable: **the Anvil becomes a repeatable
arena**, re-armed by re-crafting the Key from Shards that respawn with the
per-region POIs. That is a reason to keep coming back to the sky after the
questline is done, which the mod currently does not have anywhere.

```
LootTable( LootItem.between("aetheriumbar", 8, 16),
           LootItem.between("stormsteelbar", 6, 12),
           ChanceLootItem(1.00, one of: "skyreave" "thunderhead" "prismcaller"
                                       "stormdisc" "skywatchwhistle"),
           ChanceLootItem(0.35, "stormsteelhelmet" | "stormsteelchestplate" | "stormsteelboots"),
           ChanceLootItem(0.90, "coin", 2000..6000) )
```

**The v0.6 hook, stated honestly.** The altar has a **third socket, and it stays
dark.** ROADMAP v0.6 puts the Storm Sovereign here; until that boss exists this
POI is complete and worth doing on the wave fight alone, and the empty socket is
the tease rather than a broken promise. **Do not ship the altar with a summon it
cannot honour.**

**Inhabitants (before the altar is used):** 2 Skystone Golems patrolling the
terrace at (7,7) and (21,21); Storm Wisps drifting the rim. The arena floor is
empty until the player fills it.

**New art:** `sovereignaltar` (1 sheet), `sovereignkey` icon. The Seraphs, the
balustrade, the crystals, the rocks and the floors all exist.

---

### 2.7 The Passage Wayhouse

**`passagewayhouse` · 19×15 = 285 tiles · PER REGION · Skyway Passages, always
built ON an existing causeway.**

The Skyway is the mod's densest ground and its longest corridor, and there is
nowhere on it to stop. This is that place: a cloudmarble inn straddling the road,
with a bed you can sleep in, a shop, and — the headline — **the fast-travel
network**. The road is not rebuilt by this preset; `SkyLandscape` already paves
the passage and runs its balustrade. The preset writes the building, its apron,
and the join.

```
       0         1
       0123456789012345678
  y0   ...................
  y1   ...###O###O###.....
  y2   ...#cn=====eE#.....
  y3   ...#=hT=hT===#..Y..
  y4   ...O==th=th==#.....
  y5   ...#=========#..w..
  y6   ...#k=====c==O.....
  y7   ...#=======eE#.....
  y8   ...#s=====o==#.Y...
  y9   ...#####D#####.....
  y10  ..L,,,y,,,y,,,L....
  y11  ..,,,,,,s,,,,,,....
  y12  ,,,,,,,,,,,,,,,,,,,
  y13  ,,,,,,,,,,,,,,,,,,,
  y14  |||||||||||||||||||
```

> Rows y12..y14 are the **causeway that is already there** — `SkyLandscape`'s
> paving and its south balustrade — drawn for context. The preset writes only the
> apron (y10..y11, `skywaytile`) and leaves the road alone, so a wayhouse never
> cuts a passage in half.

```
  #  cloudmarblewall   O  cloudmarblewindow   D  cloudmarbledoor   =  gloomwoodfloortile
  ,  skywaytile apron        |  cloudmarblefence (the passage balustrade)
  T/t skywatchdinnertable / skywatchdinnertable2   h  skywatchchair
  e/E skywatchbed2 / skywatchbed   k  skywatchcabinet   s (interior) skywatchbookshelf
  o  skywatchclock   c  skywatchcandelabra   n  skywatchbanner (WALL_DECOR)
  y  skywaywaystone   L  wardencandelabra   s (y11) skywatchstele
  Y  cloudtree   w  cloudspringfont
```

**The shell.** Building x3..13 × y1..9, interior x4..12 × y2..8 (63 tiles). Door
on the south at (8,9), facing the road. Four windows, all mid-run: (6,1) and
(10,1) north, (3,4) west, (13,6) east.

**Objects, with rotations**

| tile | object | rot | note |
|---|---|---|---|
| (6,3) / (6,4) | `skywatchdinnertable` / `…table2` | 2 / 2 | both halves |
| (9,3) / (9,4) | `skywatchdinnertable` / `…table2` | 2 / 2 | both halves |
| (5,3) | `skywatchchair` | 1 | turned east to the table |
| (7,4) | `skywatchchair` | 3 | turned west to the table |
| (8,3) | `skywatchchair` | 1 | turned east to the table |
| (10,4) | `skywatchchair` | 3 | turned west to the table |
| (12,2) / (11,2) | `skywatchbed` / `skywatchbed2` | 3 / 3 | both halves — a real assignable bed |
| (12,7) / (11,7) | `skywatchbed` / `skywatchbed2` | 3 / 3 | both halves |
| (4,6) | `skywatchcabinet` | 1 | back to the west wall — Pike's stock |
| (4,8) | `skywatchbookshelf` | 0 | back to the south wall |
| (10,8) | `skywatchclock` | 0 | |
| (4,2), (10,6) | `skywatchcandelabra` ×2 | — | |
| (5,2) | `skywatchbanner` | 2 (wall above) | WALL_DECOR |
| (6,10), (10,10) | **`skywaywaystone`** ×2 | — | lit; flank the door approach |
| (2,10), (14,10) | `wardencandelabra` ×2 | — | |
| (8,11) | `skywatchstele` | 2 | the wayhouse's own board |
| (16,5) | `cloudspringfont` | — | with `cloudtree` at (16,3) and (15,8) |

**Lighting:** 2 candelabra inside (1 per 31 interior tiles), 2 streetlamps and 2
lit waystones outside. 6 in 285 = 1 per 47 overall.

**Inhabitants: nobody, and that is the reconciliation.** An earlier draft put a
Lamplighter NPC here; the chapter brief gives the Skyway to **Magpie** at the
Toll-House (POI 12), and two Skyway NPCs is one too many. The wayhouse is
therefore **empty infrastructure** — which suits a per-region POI better anyway,
and removes an NPC wardrobe from the art order. The waystones are **self-serve**:
standing at an unlit one offers to light it for `aetheriumbar` ×2 +
`stormglass` ×3, paid into the stone.

**What the player does, and leaves with.**

1. **The shortcut, and it is the reason this POI exists.** Lighting this
   wayhouse's waystones adds it to the player's Skyway network; from then on any
   lit waystone travels to any other and back to the Spire. In an infinite
   archipelago this is the single most valuable thing the sky can hand out, and it
   converts every wayhouse the player finds into a permanent asset.
   Mechanically it is vanilla's own waystone flow —
   `Waystone.findTeleportLocation` is already documented in
   `docs/TECHNICAL_LEARNINGS.md` (it returns PIXEL coordinates; do not pass it
   tiles).
2. **A bed and a roof** on a corridor that currently has neither — two real
   `BedObject` beds, so a player caught out at night on a passage has somewhere to
   go. Once Magpie is recruited she restocks these wayhouses on her excursions,
   which is how the brief's courier and this dossier's infrastructure join up.
3. **A cache** in the cabinet at (4,6): lamps, cloudmarble stock, cooked food and
   occasionally a Ledger page picked up off the road.

**New art:** `skywaywaystone` (1 sheet), `skywatchstele` and `cloudspringfont`
(both already counted).

---

### 2.8 The Unopened Gate

**`unopenedgate` · 27×23 = 621 tiles · ONCE PER WORLD · Skyway Passages, at the
far end of the longest passage from `SkyOrigin`.**

**The story beat, and the second landmark with no building.** `DESIGN.md` §6
puts "Gates of Heaven" above the Skyway; this is the sky admitting it does not
have them yet. A causeway runs out to a chequered dais, two colossal Sky Seraphs
stand facing each other across it — and the gate between them **is not there.**
Nobody took it. It was never hung. 272 of the 621 tiles are untouched; the ring
was generated by `discRing(r=10)` and is one closed loop.

```
       0         1         2
       012345678901234567890123456
  y0   ...........................
  y1   ..........|||G|||..........
  y2   ........|||,,L,,|||........
  y3   .......|L,,,,,,,,,L|.......
  y4   ......||,,,,,,,,,,,||......
  y5   .....||,,,,,,,,,,,,,||.....
  y6   ....|L,,,,,,,,,,,,,,,L|....
  y7   ....|,,,,,,,,,,,,,,,,,|....
  y8   ...||,,,,,,,,,,,,,,,,,||...
  y9   ...|,,,,,,+k+++k+,,,,,,|...
  y10  ...|,,,,,,+++++++,,,,,,|...
  y11  ...GL,,,,,A+++++A,,,,,LG...
  y12  ...|,,,,,,+++++++,,,,,,|...
  y13  ...|,,,,,,+++P+++,,,,,,|...
  y14  ...||,,,,,,,,,,,,,,,,,||...
  y15  ....|,,,,,,,,,,,,,,,,,|....
  y16  ....|L,,,,,,,,,,,,,,,L|....
  y17  .....||,,,,,,,,,,,,,||.....
  y18  ......||,,,,,,,,,,,||......
  y19  .......|L,,,,,,,,,L|.......
  y20  ........|||,,L,,|||........
  y21  ..........|||G|||..........
  y22  ...........................
```

```
  |  cloudmarblefence on discRing(r=10) — 68 tiles, one closed loop
  G  cloudmarblefencegate ×4 on the axes, one tile wide each
  ,  skywaytile — 230 tiles, and almost all of it stays empty
  +  marblechecker dais, x10..16 × y9..13 = 32 tiles (5% of the plot)
  A  seraphstatue ×2, at (10,11) and (16,11), facing each other across a 5-tile gap
  P  skywatchdisplay — the pedestal      L  wardencandelabra ×12
  k  skywatchcabinet ×2 at (11,9) and (15,9), flanking the dais
```

**Objects, with rotations**

| tile | object | rot | note |
|---|---|---|---|
| (10,11), (16,11) | `seraphstatue` ×2 | — | 96px wide art on one tile; the 5-tile gap between them at x11..15 **is** the gate |
| (13,13) | `skywatchdisplay` | — | the pedestal, on the dais step |
| 12 `L` cells | `wardencandelabra` ×12 | — | evenly on the ring: (13,2) (18,3) (21,6) (22,11) (21,16) (18,19) (13,20) (8,19) (5,16) (4,11) (5,6) (8,3) |
| (13,1) (13,21) (3,11) (23,11) | `cloudmarblefencegate` ×4 | — | the ring stays closed through each gate |
| outside the ring | `skyseraphtree` ×7, `skyseraphsapling` ×5 | — | a grove, in clusters of 2–3, never a uniform sprinkle |

**Lighting:** 12 lamps in 621 = **1 per 52** — deliberately the same count as
`warden-tower-layout`'s twelve candelabra, and for the same reason: a ring of
regularly spaced lights is what makes a formal place read as formal.

**What the player does, and leaves with.**

- **The story beat.** The pedestal holds **Warden's Ledger Page VII**, the last
  one, and it is the page that names what the gate was for and why the Watch
  never finished it. Everything the player has been collecting at every Wayside
  ends here.
- **A harvest reason to come back:** this is the **only** dense stand of
  `skyseraphtree` in the world. Seraphwood and Sky Seraph saplings, in quantity,
  in one place.
- Two `skywatchcabinet` flank the dais at (11,9) and (15,9) with a guaranteed
  cache: `seraphwood` 8–16, `aetheriumbar` 3–6, `goldbar` 4–10, coins.

**Inhabitants:** 2 Skywatch Revenants standing **on the dais**, one on each side
of the gap, exactly where a pair of gate wardens would stand. They have not left
their posts. They are the only thing at this POI that moves.

**New art:** none new — `skywatchstele` is not even used here; the pedestal, the
Seraphs, the balustrade and the trees all exist. This is the cheapest
once-per-world POI in the chapter and the best image in it.

---

### 2.9 The Prism Choir

**`prismchoir` · 21×21 = 441 tiles · PER REGION · Aurora Shoals.**

The Aurora Shoals' answer to the player's second reference screenshot — *a shrine
with statues on pedestals around a glowing centrepiece.* Seven singing crystal
columns on a ring, a font at the centre, and 152 tiles of empty prism floor
between them. Ring generated by `discRing(r=8)`.

```
       0         1         2
       012345678901234567890
  y0   .....................
  y1   .....................
  y2   ........||G||........
  y3   ......|||,,,|||......
  y4   ....|||,,,,,,,|||....
  y5   ....|,,,,,C,,,,,|....
  y6   ...||,,,,,,,,,,,||...
  y7   ...|,,C,,,P,,,C,,|...
  y8   ..||,,,,,,,,,,,,,||..
  y9   ..|,,,,,,,,,,,,,,,|..
  y10  ..G,,,,,,,w,,,,,,,G..
  y11  ..|,,C,,,,,,,,,C,,|..
  y12  ..||,,,,,,,,,,,,,||..
  y13  ...|,,,,,,,,,,,,,|...
  y14  ...||,,,,,,,,,,,||...
  y15  ....|,,,C,,,C,,,|....
  y16  ....|||,,,,,,,|||....
  y17  ......|||,,,|||......
  y18  ........||G||........
  y19  .....................
  y20  .....................
```

```
  |  cloudmarblefence on discRing(r=8) — 60 tiles, one closed loop
  G  cloudmarblefencegate ×4 on the axes
  ,  prismfloortile — 152 tiles
  C  prismchime ×7 at (10,5) (14,7) (15,11) (12,15) (8,15) (5,11) (6,7)
  w  cloudspringfont at (10,10)      P  skywatchdisplay at (10,7)
```

**Objects, with rotations**

| tile | object | rot |
|---|---|---|
| the 7 `C` cells | `prismchime` | 0, 1, 1, 2, 2, 3, 3 — each turned to face the centre |
| (10,10) | `cloudspringfont` | — |
| (10,7) | `skywatchdisplay` | — |
| (10,2) (10,18) (2,10) (18,10) | `cloudmarblefencegate` ×4 | — |
| (10,1) (10,19) (1,10) (19,10) | `wardencandelabra` ×4 | — outside each gate |
| (3,3) (17,4) (4,17) | `aurorabloom` + `aurorabloomr` pairs | — **both halves** |
| outside the ring | `prismabirch` ×3, `glowfern` and `auroralily` in colonies of 3–5, `prismgrass` meadow at ~55% | — |

**Lighting:** 7 chimes (lit) + 4 gate lamps + the font's own glow = **12 in 441 =
1 per 37**, the `warden-tower-layout` rhythm exactly.

**What the player does, and leaves with.** Walking past a chime lights it and it
sounds a note. **Light all seven before the first one fades** and the font opens:
the `skywatchdisplay` at (10,7) rises carrying **Sovereign Shard III** and a
Prismshard cache. It is a lap of the ring against a timer — no new UI, no
inventory puzzle, readable in three seconds from the entrance.

And it is a **fight**, because the Shoals nest here: **Dawnpiercers** dive at the
player from outside the ring while they run it, and two **Aurora Flakes** hold
the centre. Both already exist. The puzzle's difficulty is the interruption.

```
LootTable( LootItem("sovereignshard"),
           LootItem.between("prismshard", 8, 18),
           LootItem.between("aurorapetal", 6, 12),
           ChanceLootItem(0.45, "auroralocket"),
           ChanceLootItem(0.55, "coin", 500..1500) )
```

**Inhabitants:** 2 Aurora Flakes at (8,10) and (12,10); Dawnpiercers spawn on the
ring's outside and dive in. No friendly NPC — this is a place, not a household.

**New art:** `prismchime` (1 sheet, 3 variants), `cloudspringfont` (already
counted). **Fallback if the art budget bites:** the chimes can be built from
`aurorabloom` + `starfall` + a `skywatchdisplay` pedestal each, at the cost of a
weaker silhouette. Cut it there, not somewhere structural.

---

### 2.10 The Serpent's Reef

**`serpentsreef` · 25×25, of which 123 are land · COMMON · the Mistsea, between
islands, in any biome.**

The `iceberg` lesson, applied to the thing the Skyreach actually is. That preset
puts 159 land tiles in 625 of open water, **one** inhabited cluster of about a
dozen objects, a few outlying rocks, and **one narrative prop that carries the
whole story**. This is that, in cloud, with a Mistserpent in it.

The land mask below is a wobbled crescent, generated rather than drawn, so the
reef reads as geology and not as a shape.

```
       0         1         2
       0123456789012345678901234
  y0   ~~~~~~~~~~~~~~~~~~~~~~~~~
  y1   ~~~~~~~~~~~~~~~~~~~~~~~~~
  y2   ~~~~~~~~~~~~~~~~~~~~~~~~~
  y3   ~~~~~~~~~~~~~~~~~~~~~~~~~
  y4   ~~~~~~~~~~~~~~~~~~~~~~~~~
  y5   ~~~~~~~~~~~~~~~~~~~~~~~~~
  y6   ~~~~~~~~~###=~~~~~~~~~~~~
  y7   ~~~~~~~=#=====~~~~~~~~~~~
  y8   ~~~~~~=#======~~~~~~~~~~~
  y9   ~~~~~~#=====~~~~~~~~~~~~~
  y10  ~~~~~L#===~~~~~~~~~~~~~~~
  y11  ~~~~=====~~~~~~~~~~~~~~~~
  y12  ~~~~=#===~~~~~~~~~~~~~~~~
  y13  ~~~~a#==~~~~~~~~~====~~~~
  y14  ~~~~=#==~~~~~~~~~=p==~~~~
  y15  ~~~~====~~~~~~~~=====~~~~
  y16  ~~~~~=#=~~~~~~~~==p=L~~~~
  y17  ~~~~~~a#=~~~~~~~====~~~~~
  y18  ~~~~~~==f=W=b=W=#==~~~~~~
  y19  ~~~~~~==k#=P=r=#===~~~~~~
  y20  ~~~~~~~====#=#===~~~~~~~~
  y21  ~~~~~~~~~~======~~~~~~~~~
  y22  ~~~~~~~~~~~~=~~~~~~~~~~~~
  y23  ~~~~~~~~~~~~~~~~~~~~~~~~~
  y24  ~~~~~~~~~~~~~~~~~~~~~~~~~
```

```
  ~  mistseatile        =  skystonetile reef        #  the spine: skystonerock + skyscree
  W  aeronautwreck ×2 — the hull, read end-on      b  skyballoon, half-collapsed over it
  f, r  skywatchrubble (two cells)                 k  skywatchcabinet — the hold
  P  skywatchdisplay — THE narrative prop          a  aetheriumrock
  p  prismshardrock    L  wardencandelabra ×2
```

**Objects, with rotations**

| tile | object | rot | note |
|---|---|---|---|
| (10,18), (14,18) | `aeronautwreck` ×2 | 1 | read as one hull, bow east |
| (12,18) | `skyballoon` | — | the envelope, down over the deck |
| (8,18), (13,19) | `skywatchrubble` ×2 | — | |
| (8,19) | `skywatchcabinet` | 0 | the hold — `addInventory` |
| (11,19) | `skywatchdisplay` | — | **the one prop that tells the story** |
| (4,13), (6,17) | `aetheriumrock` ×2 | — | |
| (18,14), (18,16) | `prismshardrock` ×2 | — | |
| (5,10), (20,16) | `wardencandelabra` ×2 | — | the reef's only lights |
| scattered on the reef | `skyreeds`, `skylichen`, `skyscree` | — | clusters of 2–4 at the waterline, never per-tile |

**Lighting:** 2 in 123 land tiles = 1 per 62, plus 3 `starfall` on the spine at
(7,8), (5,12), (16,18). The reef is a thing you see glowing from a shore at
night; that is its whole navigational job.

**What the player does, and leaves with.** Swims or sails out to something
glowing in the cloud. On the pedestal: one **Warden's Ledger Page**, always — the
Reef is the second reliable source of them, so a player who hates roads can still
finish the collection at sea. In the hold: `aetheriumore` 4–10, `prismshard`
2–6, `windsilk`, coins. In the rock: two Aetherium and two Prismshard nodes,
which makes the Reef a **mining stop** as well as a landmark.

**Inhabitants and the encounter.** The Reef is a **Mistserpent feeding ground** —
the worm chain the mod already has and that most players will never meet, because
it spawns `IN_MISTSEA` in open water where nobody swims. Here it is guaranteed,
it circles the crescent, and the reef is the only footing. One **Reefmaw** (new,
arsenal pattern, icon only) sits in the wreck. Fighting a worm from a 123-tile
reef is a genuinely different encounter from anything else in the mod, and it
costs one icon.

**New art:** `reefmaw` icon. Everything else exists.

---

### 2.11 The Dew-Keeper's Hut

**`dewkeepershut` · 13×13 = 169 tiles · COMMON · Aurora Shoals.**

A small dwelling with nobody in it — the counterweight to the Fold. `cosy-cabin`
scale (its interior is 35 tiles against the cabin's 29), one room, a snail run
outside, and a stele by the door explaining where the keeper went. It is common,
so the Shoals stop being a pretty empty biome.

```
       0         1
       0123456789012
  y0   .............
  y1   ...####O####.
  y2   ...#c====eE#.
  y3   ...#==h====#.
  y4   ..sD==mm===O.
  y5   ...#k==h===#.
  y6   ...#o=====r#.
  y7   ...#########.
  y8   ..L..........
  y9   ..ffffGffff..
  y10  ..f.g...g.f..
  y11  ..f..gw...f..
  y12  ..fffffffff..
```

```
  #  skystonebrickwall   O  window   D  door   =  prismfloortile
  c  skywatchcandelabra   e/E  skywatchbed2 / skywatchbed   r  skywatchdresser
  m  skywatchmodulartable   h  skywatchchair   k  skywatchcabinet
  o  cookingpot (vanilla)   s  skywatchstele   L  wardencandelabra
  f  skyironfence   G  skyironfencegate   g  glowfern   w  cloudspringfont
```

**The shell.** Hut x3..11 × y1..7, interior x4..10 × y2..6 (35 tiles). Door on
the west at (3,4). Two windows, both mid-run: (7,1) north (dir 0), (11,4) east
(dir 1).

**Objects, with rotations**

| tile | object | rot | note |
|---|---|---|---|
| (10,2) / (9,2) | `skywatchbed` / `skywatchbed2` | 3 / 3 | both halves; head to the east wall |
| (10,6) | `skywatchdresser` | 3 | back to the east wall — the cache |
| (6,4), (7,4) | `skywatchmodulartable` ×2 | — | with `skywatchtome` and `pottedcloudberry` on FENCE_AND_TABLE_DECOR |
| (6,3) | `skywatchchair` | 2 | turned to the table below |
| (7,5) | `skywatchchair` | 0 | turned to the table above |
| (4,5) | `skywatchcabinet` | 1 | back to the west wall |
| (4,6) | `cookingpot` (vanilla) | — | the Shoals' cooking loop, on site |
| (4,2) | `skywatchcandelabra` | — | |
| (2,4) | `skywatchstele` | 1 | beside the door, faces east |
| (2,8) | `wardencandelabra` | — | |
| (2,9)–(10,9) | `skyironfence` ×9, gate at (6,9) | — | straight run |
| (2,10)–(2,11), (10,10)–(10,11) | `skyironfence` ×4 | — | straight runs, the pen's sides |
| (2,12)–(10,12) | `skyironfence` ×9 | — | straight run; the pen is closed |
| (4,10), (8,10), (5,11) | `glowfern` ×3 | — | lit, in the run |
| (6,11) | `cloudspringfont` | — | the cracked cistern |

**Lighting:** 1 candelabra inside (1 per 35 interior tiles), 1 streetlamp at the
door, 3 glowferns and the font in the run. 6 in 169 = 1 per 28.

**What the player does, and leaves with.**

- **Dew Snails, in a pen, in quantity.** Five of them, netable
  (`NetableMob` is already wired and a catch already drops the snail itself), which
  is a collection loop the mod built and then hid in open terrain where nobody
  finds five at once.
- The dresser and the cabinet hold `prismshard`, `aurorapetal`, `nimbusmilk`,
  `skycurd` and a `cloudcustard` — a **cooking starter kit** beside a working
  cooking pot, so a player who has never used the mod's food chain can make
  something on the spot.
- The stele: the keeper went to the Choir to hear the seven notes and did not come
  back. That is the hook to POI 9, delivered by a common POI to a per-region one —
  which is how a world teaches itself.

**Inhabitants:** 5 Dew Snails in the run. Nobody human. The bed is made.

**New art:** none beyond `skywatchstele` and `cloudspringfont`, both already
counted.

---

## 2A. Three more, added after the chapter brief landed

`docs/design/chapter-01-skyreach-cast.md` (biome-designer) was written in
parallel with this dossier and arrived after §2 was drafted. **The brief owns the
cast, the story, the enemies and the reward list; this dossier owns the
layouts.** §3.0 below reconciles the two line by line. These three are the
layouts for the brief's three recruit sites — the places where, in its own words,
*"the loot is a person"*.

### 2.12 The Skyway Toll-House

**`skywaytollhouse` · 23×19 = 437 tiles · ONCE PER WORLD · Skyway Passages, on
the longest passage.**

The brief's customs post: cargo bonded and never released, a Tollwright still
doing its job with nobody left to bill, and **Magpie** hiding in the ledger room
behind a vault she cannot open. Three rooms, one weighbridge, and a vault whose
door is a monster.

```
       0         1         2
       01234567890123456789012
  y0   .......................
  y1   .......................
  y2   ..####O#########O####..
  y3   ..#q======q=#====ss=#..
  y4   ..#dh=====c=#=dh====#..
  y5   ..#=========D=======O..
  y6   ..O===+++===#=c=P==k#..
  y7   ..#===+++===#=======#..
  y8   ..#===+++===#########..
  y9   ..#===+++r==#=======#..
  y10  ..#===+++===#=k===k=#..
  y11  ..O=========D===P===O..
  y12  ..#Bb=====c=#=v===v=#..
  y13  ..#q======q=#===q===#..
  y14  ..#####D########O####..
  y15  .,,,,L,,,,,,,,,,,L,,,,.
  y16  .,,,,,,,,,,S,,,,,,,,,,.
  y17  ,,,,,,,,,,,,,,,,,,,,,,,
  y18  |||||||||||||||||||||||
```

```
  #  cloudmarblewall   O  window   D  door   =  gloomwoodfloortile
  +  marblechecker weighbridge inlay, x6..x8 × y6..y10 = 15 tiles (accent scale)
  ,  skywaytile apron    |  the passage balustrade (SkyLandscape's, drawn for context)
  q  mistglasslantern (WALL_DECOR)   c  skywatchcandelabra   L  wardencandelabra
  d  skywatchdesk   h  skywatchchair   s  skywatchbookshelf   k  skywatchcabinet
  B/b skywatchbench + skywatchbench2   P  skywatchdisplay   v  barrel (vanilla)
  r  skywatchrubble   S  skywatchstele
```

**The shell.** Building x2..20 × y2..14. Grand door south at (7,14). A partition
at x12 (y3..13) with doors at (12,5) and (12,11); a solid partition at y8
(x13..19) splitting the east wing. Windows, all mid-run: (6,2) and (16,2) north,
(16,14) south, (2,6) and (2,11) west, (20,5) and (20,11) east.

**Three rooms.**

| room | extent | tiles | what is in it |
|---|---|---|---|
| **Weighing hall** | x3..11 × y3..13 | 99 | the chequer weighbridge, the assessor's desk, benches for people who waited |
| **Ledger room** | x13..19 × y3..7 | 35 | two bookshelves, a desk, and **Magpie** |
| **Vault** | x13..19 × y9..13 | 35 | two cabinets, two barrels, and the **Bonded Lockbox** on the pedestal |

**Objects, with rotations**

| tile | object | rot | note |
|---|---|---|---|
| (3,4) | `skywatchdesk` | 1 | the assessor's bench |
| (4,4) | `skywatchchair` | 3 | turned to the desk |
| (3,12) / (4,12) | `skywatchbench` / `skywatchbench2` | 1 / 1 | both halves |
| (9,9) | `skywatchrubble` | — | the roof over the weighbridge |
| (14,4) | `skywatchdesk` | 1 | Magpie's, with `skywatchtome` — the **Ledger of Undelivered Post** |
| (15,4) | `skywatchchair` | 3 | |
| (17,3), (18,3) | `skywatchbookshelf` ×2 | 2, 2 | backs to the north wall |
| (19,6) | `skywatchcabinet` | 3 | back to the east wall |
| (16,6) | `skywatchdisplay` | — | the **Skyway Writ** |
| (14,10), (18,10) | `skywatchcabinet` ×2 | 2, 2 | |
| (14,12), (18,12) | `barrel` (vanilla) ×2 | — | bonded cargo |
| (16,11) | `skywatchdisplay` | — | the **Bonded Lockbox** |
| (10,4), (10,12) | `skywatchcandelabra` ×2 | — | the hall |
| (14,6) | `skywatchcandelabra` | — | the ledger room |
| (3,3), (10,3) | `mistglasslantern` | 2 (wall above) | WALL_DECOR |
| (3,13), (10,13) | `mistglasslantern` | 0 (wall below) | WALL_DECOR |
| (16,13) | `mistglasslantern` | 0 (wall below) | the vault's one light |
| (5,15), (17,15) | `wardencandelabra` ×2 | — | the apron |
| (11,16) | `skywatchstele` | 2 | the tariff board, still legible |

**Lighting:** 5 wall lanterns + 3 candelabra inside (8 over 169 interior tiles =
**1 per 21**, the brightest interior here — it was a working office) and 2 lamps
on the apron. The vault runs **one** lantern over 35 tiles on purpose.

**Inhabitants.** The **Tollwright** stands at **(16,12)**, on the vault floor
directly in front of the Lockbox — it owns the room rather than chasing.
**Magpie** is at **(18,5)** in the ledger room, and does not come out until the
Tollwright is down.

**What the player leaves with.** The Bonded Lockbox (Magpie's recruit key and a
one-time cache), the Skyway Writ, the Ledger of Undelivered Post — and Magpie
herself, who then moves down to the player's Surface settlement. Per the brief,
that is the answer to "POIs never have special loot": here the loot is a person.

**New art:** `skywatchstele` (already counted). The Tollwright is the brief's,
built on vanilla's armoured-golem archetype; the three unique items are the
brief's art order, not this dossier's.

### 2.13 The Grange Cellar

**`grangecellar` · 21×19 = 399 tiles · ONCE PER WORLD · Driftlands.**

The brief's brewhouse that fell in on itself with the vats still warm. Necesse
has no vertical layering inside a level, so the "cellar" is read horizontally:
**a broken outer shell you walk through, and one intact sealed room inside it.**
The contrast between the two wall families does the work — pale skystone brick
for the ruin, `nightfell` for the cellar that survived.

```
       0         1         2
       012345678901234567890
  y0   .....................
  y1   .....................
  y2   .###....######....##.
  y3   .#.......r......r..#.
  y4   .#..%%%%%%%%%%%....#.
  y5   .#..%V=V==%===%....#.
  y6   .#..%=====%=v=%....#.
  y7   .#..%V=V==%===%....#.
  y8   .#..%==Z==D===%....#.
  y9   .#..%q====%=c=%....#.
  y10  .#..%=====%==k%....#.
  y11  .#..%=q===%===%....#.
  y12  .#..%%%D%%%%%%%....#.
  y13  .#.................#.
  y14  .##....######....###.
  y15  .....................
  y16  .....................
  y17  .....................
  y18  .....................
```

```
  #  skystonebrickwall — the ruin shell, drawn with the gaps the collapse left
  %  nightfellwall — the cellar, still sound      =  gloomwoodfloortile
  D  nightfelldoor    r  skywatchrubble (the fallen roof)
  V  fermentationvat ×4   Z  the Sourvat Bloom (a mob, reads as scenery)
  v  barrel (vanilla) — the Warden's Round, on a marblechecker tile
  c  skywatchcandelabra   k  skywatchcabinet   q  mistglasslantern (WALL_DECOR)
```

**The shell.** The ruin is x1..x19 × y2..y14, and **it is deliberately not a
closed shape** — the north face is missing three tiles at x6..x8 and two at
x14..x15, the south face is open in the same places, and the player walks in
through the collapse. Inside it, untouched Driftlands ground and two rubble
piles at (9,3) and (16,3).

**The cellar.** x4..x14 × y4..y12, `nightfell` walls, one door on the south at
(7,12). Interior x5..x13 × y5..y11 (63 tiles), split by a partition at x10 with a
single door at (10,8):

- **Vat room** x5..x9 × y5..y11 (35 tiles) — four `fermentationvat` at (5,5)
  (7,5) (5,7) (7,7), a `mistglasslantern` at (5,9) rot 1 and one at (6,11) rot 0.
- **Deep cell** x11..x13 × y5..y11 (21 tiles) — reachable **only** through
  (10,8). A vanilla `barrel` at (12,6) standing on a single `marblechecker` tile
  (accent scale, one tile): **the Warden's Round**. A `skywatchcandelabra` at
  (12,9) and a `skywatchcabinet` at (13,10) rot 3.

**Lighting:** 2 wall lanterns + 1 candelabra over 63 interior tiles = **1 per
21**. The cellar is the one place in the chapter that is brighter inside than
out, and that is the point: it never stopped being used.

**Inhabitants.** The **Sourvat Bloom** sits at **(7,8)**, between the four vats,
and reads as one more vat until the player is inside its reach — the ambush the
brief asks for, framed by the composition rather than by a script. **Halda** is
in the deep cell at **(12,10)**, behind the partition, which is why she is still
alive.

**What the player leaves with.** The Mother (out of the Bloom, her recruit key),
the Warden's Round, Wild Skyyeast and Spent Grain — and Halda, who moves down.

**New art:** `fermentationvat` — **the brief's own station**, listed here so the
prop agent sees where it stands. Everything else reuses.

### 2.14 The Test Range

**`stormveiltestrange` · 27×23 = 621 tiles · ONCE PER WORLD · Stormveil.**

The brief's crater field around a workshop whose own security woke up and locked
the door from inside. **This is the deliberate sequel to POI 3**, not a
duplicate: the Institute of Applied Falling is the Driftlands comedy, and its
last stele reads *"the Board has relocated testing to the Stormveil, where the
weather is more honest."* This is where they went, and it is where it stopped
being funny.

```
       0         1         2
       012345678901234567890123456
  y0   ...........................
  y1   ...........................
  y2   ...........................
  y3   ................##O#.####..
  y4   ................#t=====a#..
  y5   ...xxxxx........O=======#..
  y6   ...x:W:x........#==P====#..
  y7   ...x:::x........Dk======#..
  y8   ...xx:px........#=d=h==gO..
  y9   ....xxxxx.......#=======#..
  y10  ................#s=====c#..
  y11  ................#########..
  y12  ...........................
  y13  ........xxxxxxx............
  y14  .......x::b:::x............
  y15  .......x:N:W::x............
  y16  ..xxxx.x::::::x............
  y17  .xx::xx.xxxxxx.............
  y18  .x:W:px....................
  y19  .x::::x....................
  y20  ..xxxx.....................
  y21  ...........................
  y22  ...........................
```

```
  #  nightfellwall   O  nightfellwindow   D  nightfelldoor (LOCKED — see below)
  =  charfloortile (the workshop)     :  skystonetile (the crater floors)
  x  crater rim: skystonerock, skyscree, stormscreed
  W  aeronautwreck   b  skyballoon   p  skyparcel
  t  skywatchtelescope   a  skywatchastrolabe   d  skywatchdesk   h  skywatchchair
  k  skywatchcabinet   s  skywatchbookshelf   c  skywatchcandelabra
  g  chargecrystal   P  skywatchdisplay   N  Prototype Nine (a mob, playing dead)
```

**The way in is the story.** The workshop is x16..24 × y3..11, and **its door at
(16,7) is shut and stays shut** — Vane's prototype locked it from inside and the
preset never opens it. The way in is the **gap at (20,3)**, where the north wall
came down. A player who walks the whole west face looking for a handle and then
finds the hole has been told the story without a line of text.

Windows, all mid-run in a straight run: (18,3) north, (16,5) west, (24,8) east.

**Objects, with rotations**

| tile | object | rot | note |
|---|---|---|---|
| (17,4) | `skywatchtelescope` | — | he built the spire's; these are the spares |
| (23,4) | `skywatchastrolabe` | — | |
| (19,6) | `skywatchdisplay` | — | the **Storm Lens Core**, after the fight |
| (17,7) | `skywatchcabinet` | 1 | back to the west wall |
| (18,8) | `skywatchdesk` | 1 | the drafting bench |
| (20,8) | `skywatchchair` | 3 | turned to the desk |
| (23,8) | `chargecrystal` | — | the workshop's power tap |
| (17,10) | `skywatchbookshelf` | 0 | back to the south wall |
| (23,10) | `skywatchcandelabra` | — | |
| (5,6) | `aeronautwreck` | — | crater A, x3..x7 × y5..y9 |
| (6,8) | `skyparcel` | — | |
| (10,14) | `skyballoon` | — | crater B, x7..x14 × y13..y17 — the big one |
| (11,15) | `aeronautwreck` | — | |
| (3,18) | `aeronautwreck` | — | crater C, x1..x6 × y16..y20 |
| (5,18) | `skyparcel` | — | |
| every `x` cell | `skystonerock` / `skyscree` / `stormscreed` | — | ~50% coverage, as a rim formation, never a per-tile sprinkle |

**Lighting:** 1 candelabra + 1 charge crystal over 49 interior tiles = **1 per
25**, and **the crater field carries no light at all.** 621 tiles, two lamps.
That is the register: a lit box in a dark field, visible from a long way off,
which is exactly the "what is that over there?" §8 asks for.

**Inhabitants.** **Prototype Nine** lies at **(9,15)** in the largest crater,
among the wrecks, indistinguishable from them until approached — then it fights
at range. **Ossian Vane** is at **(20,6)** in the workshop, on the far side of a
door he cannot open from inside either.

**What the player leaves with.** The Storm Lens Core (Vane's recruit key and the
thing his Drafting Table will not run without), the prototype cache of
Aetherwright's Casings, and Vane himself.

**New art:** none. Nightfell walls, char floor, the telescope, the astrolabe, the
wrecks, the balloon and the parcels all already exist — this once-per-world POI
costs **zero new pixels** beyond the brief's own enemy icon.

---

## 3. The reward economy

### 3.0 Reconciliation with the chapter brief

`docs/design/chapter-01-skyreach-cast.md` was written in parallel with this file
and arrived after §2.1–§2.11 were drafted. The loop's division of labour settles
every conflict: **the brief owns cast, story, enemies and the reward list; this
dossier owns footprints, plans, object lists and rotations.** Where the two
overlap, here is exactly what happens.

| the brief says | this dossier said | resolution |
|---|---|---|
| Three recruitable settlers: **Magpie**, **Halda**, **Ossian Vane**, each found at one POI and each moving **down** to the Surface | Wren the Cloud Shepherd was recruitable | **Brief wins.** Wren is demoted to a sky-resident shopkeeper and quest-giver (§2.2). `DESIGN.md` Part V's Cloud Shepherd promise is still kept; the brief's rule that settlers move down is untouched. |
| **Magpie** holds the Skyway | this dossier had **Pike the Lamplighter** at the Wayhouse | **Brief wins.** Pike is cut. The Wayhouse is empty infrastructure and its waystones are self-serve (§2.7). One NPC wardrobe leaves the art order. |
| Enemies **Tollwright**, **Sourvat Bloom**, **Prototype Nine**, each in one structure | enemies **Skywatch Revenant**, **Fulgur Shade**, **Reefmaw** | **Both, and they do not overlap.** The brief's three are recruit-site bosses (POIs 12–14); this dossier's three hold the ruins nobody lives in (POIs 4, 5, 6, 8, 10). Six POI-exclusive enemies across fourteen places is the right density, and only one of the six needs a body sheet. |
| **Skywatch Signet** — a trinket that reveals unexplored Skyreach structures on the map | **Warden's Ledger** ×7 → the Warden marks three POIs on the map | **Brief wins on the map reveal.** The Signet does it, as a story-gated reward from the spire archive. The Ledger survives as the **collection** (§3.1) and hands its pages to Magpie instead — it becomes the brief's own *Ledger of Undelivered Post* loop, which needs a collectible and did not have one. |
| **Aetherwright's Casing** — the gate material for the first weapon tier past Stormsteel | **Sovereign Key** ×3 shards → the Anvil arena | **Both, joined.** The Sovereign chain is the ROADMAP v0.6 arc and it owns POI 6; the Anvil's payout now includes Casings, so Vane's tier and the arena feed each other instead of competing for the same slot. |
| Six named places: Spire, Toll-House, Grange Cellar, Test Range, **Kite Mast**, **Anchor Terrace** | eleven places | **Toll-House / Grange Cellar / Test Range are §2.12–§2.14.** The Kite Mast and the Anchor Terrace are small storytelling set-pieces that still need layouts — flagged in §6 as the two open items, not silently dropped. |
| The Test Range is "a crater field of failed flying machines" | POI 3 is a crater field of failed flying machines | **Merged into a sequence, not a duplicate.** POI 3 (Driftlands, comedy, once per world) is the Institute; POI 14 (Stormveil, Vane, Prototype Nine) is where the Board relocated testing to. POI 3's last stele says so. The joke gets a consequence and the chapter gets a spine. |


Three loops tie the eleven places together. None of them is a new system: each is
an item, a loot table and a dialogue branch on machinery the mod already has.

### 3.1 The Warden's Ledger — the answer to "how do I ever find these?"

`wardenledger`, "Warden's Ledger Page", `misc/questitems` (vanilla's own bin for
a quest key — `silverbell` is already filed there). Seven distinct pages.

| where | how |
|---|---|
| **Skywatch Wayside** (common) | one, guaranteed, in every offering cabinet |
| **The Serpent's Reef** (common) | one, guaranteed, on the pedestal |
| **Passage Wayhouse** (per region) | one in the cabinet at (4,6) |
| **The Unopened Gate** (once per world) | page VII, on the pedestal — the last one |

**Seven pages handed to Magpie** and she pays out — this is the brief's own
*Ledger of Undelivered Post* loop, which asked for a collectible and did not have
one. It also finally puts the registered-but-unplaced `skyparcel` on the map: each
page names one, and the Reef, the Institute and the Test Range are where they
ended up.

**Finding the once-per-world POIs is the Signet's job, not the Ledger's** — the
brief hands that to the Skywatch Signet from the spire archive, and that is the
better home for it. The Ledger's job is to make the *common* POIs worth entering
every single time.

It also gives Magpie a second reason to exist beyond her daily stock, and it gives
the player a use for the two commonest POIs in the chapter.

Each page carries one paragraph of the Watch's own record, in the mod's dry
voice. Seven paragraphs is the entire lore load of this chapter — the register
stays pastoral and luminous, the melancholy is in what the record does not say,
and none of the Veil's gothic comedy leaks in (`IMPLEMENTATION_RULES.md` §10).

### 3.2 The Sovereign Key — the cross-biome chain

| item | where |
|---|---|
| `sovereignshard` ×1 | **Nightfell Redoubt** armoury vault (Stormveil, per region) |
| `sovereignshard` ×1 | **Aether Manufactory** counting-room display (Stormveil, once per world) |
| `sovereignshard` ×1 | **Prism Choir**, for solving the chimes (Aurora Shoals, per region) |
| `sovereignkey` | crafted at the **Aether Forge** from 3 shards + 4 `stormsteelbar` |
| the fight | **The Sovereign's Anvil** (Stormveil, once per world) |

Three places, two biomes, one crafted key, one arena. That is a reason to cross a
dimension. The shards respawn with their per-region POIs, so the Anvil is
repeatable rather than a one-shot.

**The Aether Forge is deliberately the crafting station**, because it is the mod's
own endgame workstation and it currently makes exactly two things. This gives the
professions layer a top-end consumer.

**And it joins the brief's tier rather than competing with it:** the Anvil's
payout includes **Aetherwright's Casings**, so the arena feeds Ossian Vane's
Drafting Table and the first weapon tier past Stormsteel has two sources — one you
buy from a settler, one you fight for.

### 3.3 The Skyway network — the shortcut

`skywaywaystone`, two per **Passage Wayhouse**, plus a pair at the Warden's Spire
forecourt (they fit the existing railing line beside the Skywatch Gate) and a pair
at the **Toll-House**. A stone is lit by paying materials into it —
`aetheriumbar` ×2 + `stormglass` ×3, self-serve, no NPC — and any lit waystone
travels to any other lit waystone and to the Spire. Built on vanilla's waystone flow —
`Waystone.findTeleportLocation` is already decoded in
`docs/TECHNICAL_LEARNINGS.md`, **and it returns PIXEL coordinates, not tiles.**

This is the reward that makes the per-region POI worth finding twice.

---

## 4. New art — the consolidated work order

**Nineteen PNGs across four agents**, plus one optional map icon — 5 object
sheets, 4 item icons, 1 mob sheet, 3 bestiary icons, 4 armour sheets and 2
armour icons. Every POI's own section names which of them it needs and what it
reuses instead.

**This order covers POIs 1–11 only.** POIs 12–14 are the chapter brief's recruit
sites, and their art — the three settler wardrobes, the three enemies, the eight
unique items and the two new stations (`fermentationvat`, the Aetheric Drafting
Table) — belongs to that brief's order, not this one. **Between them the layouts
in §2.12–§2.14 need exactly one new prop from this dossier: none.** They are
built entirely from `nightfell` and `cloudmarble` walls, existing floors, the
Skywatch furniture family and the two observatory instruments, plus
`skywatchstele` and `fermentationvat` which are already accounted for. **Nothing here is a wall set, a
floor, a terrain splat, a tree or a piece of furniture** — those all exist, and
three registered floors plus an entire registered wall family were sitting unused
before this chapter.

### 4.1 `art-props` — objects (5 sheets)

| id | format | used by | note |
|---|---|---|---|
| `skywatchstele` | rotating object, **the same 4-column sheet layout vanilla `objects/sign.png` uses** — measure it, do not guess | POIs 1, 2, 3, 5, 7, 10, 11 | **The highest-leverage sprite in the chapter.** Registered on vanilla's `SignObject` so `SignObjectEntity.setMessage(new LocalMessage("misc", key))` works exactly as `AeronautCampPreset` already does it. A carved Skywatch marker: pale skystone, brass rim, a storm sigil. It is how seven POIs speak without a single new mechanic. |
| `cloudspringfont` | 32×64, non-rotating `SkyDecoObject`, **lit** (`setLight`) | POIs 2, 7, 9, 11 | The fountain from the player's own reference screenshot. Cloudmarble basin, gold rim, a slow column of mist rather than water. |
| `prismchime` | 32×64, **3 variants side by side (96×64)**, lit | POI 9 | A standing prism column that rings. Aurora rose/teal only — this accent is the Shoals' and does not borrow from Stormveil. **Cuttable**: see POI 9's fallback. |
| `sovereignaltar` | `StatueObject` sheet, `frameWidth × spriteCount`; **two frames, unlit and lit** | POI 6 | Stormveil slate and stormsteel, three sockets, the third one dark. The one prop the whole 841-tile arena is framed around. |
| `skywaywaystone` | **`StreetlampObject` 32×192** — two 32×96 rows, on above / off below | POIs 7, 8, and the Spire forecourt | Deliberately on the streetlamp sheet so it inherits a format the pipeline already audits. Cloudmarble post, gold cap, a light that is off until Pike lights it. |

### 4.2 `art-props` — item icons (4 × 32×32)

| id | what | note |
|---|---|---|
| `wardenledger` | a folded page with the Watch's seal | §3.1 |
| `sovereignshard` | one third of a broken key, storm-lit | §3.2, stacks to 3 |
| `sovereignkey` | the three shards fused | §3.2 |
| `aeronautcharm` | a brass altimeter on a cord | POI 3; a `TrinketItem`, so **icon only — trinkets need no body sheet**, exactly like `auroralocket` and `zephyrharness` |

### 4.3 `art-creatures` — 1 sheet + 3 icons

| id | format | note |
|---|---|---|
| `skywatchrevenant` | `mobs/skywatchrevenant.png`, **6 columns × 4 rows at 64px, rows in Up / Right / Down / Left order**, plus `mobs/icons/skywatchrevenant.png` | **The only new creature sheet in the chapter, and it earns it: it appears at four POIs (4, 5, 6, 8) and carries the chapter's story.** An empty Skywatch coat held up by the storm the beacon was built to hold — hood with nothing in it, a lantern still burning at the belt, the mantle's cut readable from the Warden's own livery. The walk cycle must actually change pose. |
| `fulgurshade` | `mobs/icons/fulgurshade.png` only | **Arsenal pattern**: subclass the vanilla mob whose behaviour it wants and wear that mob's own sheet from `MobRegistry.Textures`. Zero body art, exactly like Rime Sentry / Aurora Flake / Fen Wraith / Cinder Cantor. |
| `reefmaw` | `mobs/icons/reefmaw.png` only | Arsenal pattern. |

### 4.4 `art-wearables` — Wren, the Cloud Shepherd (4 sheets + 2 icons)

The chapter's one new NPC wardrobe, and the most expensive line on this order.
Built on `gen_armor`'s measured human anatomy, **authored at 32×32 and upscaled
2× NEAREST**, 7 columns × 4 direction rows.

| file | size | note |
|---|---|---|
| `player/armor/shepherdcowl.png` | 448×256 | hood; row 3 is the mirror of row 1 |
| `player/armor/shepherdsmock.png` | 448×256 | the smock body |
| `player/armor/shepherdsmockarms_left.png` | 448×256 | the mantle family already ships these two — see `wardenmantlearms_left/right`; a sleeved chest piece needs both |
| `player/armor/shepherdsmockarms_right.png` | 448×256 | |
| `items/shepherdcowl.png`, `items/shepherdsmock.png` | 32×32 | every holdable needs an icon |

**Reuse `wardenboots` for her feet** — no third sheet. Palette: Driftlands
silver-green and undyed windsilk, warm where the Warden is storm-blue, so the two
NPCs read apart at a glance without a new accent.

**Pike the Lamplighter costs zero PNGs**: she wears `skywatchhood` +
`wardenmantle` **recoloured at load**, on the proven `livestock/SkyPelt` path.

### 4.5 `art-walls-tiles` — nothing

No new wall set, no new floor, no new terrain, no new splat. This is deliberate
and it is the reason the order above is nineteen files instead of eighty.
`nightfellwall` gets its first worldgen use in POI 4; `charfloortile`,
`nimbusfloortile` and `prismfloortile` get theirs in POIs 4/6, 2/3 and 9/11.

### 4.6 Optional, one file

`ui/mapicons/skypoi.png` (32×32) for the three map marks the Ledger unlocks
(§3.1). The mod already ships `ui/mapicons/skyspire.png`, `skycat.png` and
`skystairs.png`, so the slot and the pattern exist.

---

## 5. What this dossier deliberately does not do

- **It does not ship the Storm Sovereign.** POI 6 is designed to be complete and
  worth doing on its wave fight alone, with the third socket left dark as the
  v0.6 hook. An altar with a summon it cannot honour is worse than no altar.
- **It does not add a low tier.** Every reward sits at or past Stormsteel
  (`docs/WORLDBUILDING_LOOP.md` §6: "a new low-tier flower is not what is
  missing").
- **It does not scatter.** Every plan above has a stated empty fraction, and the
  three largest POIs are 31%, 42% and 44% untouched. Uniform per-tile scatter is
  the failure mode this project has already shipped once
  (`IMPLEMENTATION_RULES.md` §8).
- **It does not mix realms.** No gothic comedy, no bureaucratic afterlife, no
  stripes. The Skyreach stays pastoral and luminous; the humour in POI 3 is dry
  and structural, and it is the only joke in eleven places
  (`IMPLEMENTATION_RULES.md` §10).
- **It does not touch a recorded decision.** Portal/`SkyOrigin` routing, the save
  schema, the Warden recruitment architecture, Siggi and Peanut, and the Marble
  Checker fix are all untouched; the Warden gains one dialogue branch (§3.1) and
  nothing else.

## 6. For the integrator — the things most likely to bite

1. **Every multi-tile piece needs both halves written with the same rotation.**
   The object tables above list them as pairs. `Preset.applyToLevel` will not do
   it for you.
2. **`Preset.addInventory` on a `DisplayStandObjectEntity` is unverified.** POIs
   5, 6, 8, 9 and 10 put loot on a `skywatchdisplay`. If the display stand does
   not take a loot table, fall back to a `skywatchcabinet` on the same tile — the
   composition survives it. Check before building four POIs on the assumption.
3. **Count the windows after stamping.** `walls=n/n windows=m/m` per building,
   the way `veilstatus` already does it. A corner window is deleted silently.
4. **Run `tools/preset_seal_check.py` on POIs 2, 4, 5, 7 and 11.** Every enclosed
   interior in this dossier should flood-fill sealed except through its doors.
5. **Fence rings: use `SkyLandscape.discRing`, never an annulus.** POIs 6, 8 and 9
   were generated with the real predicate; do not redraw them by eye.
6. **The three once-per-world POIs need world-data records** so they are stamped
   once and never re-stamped, exactly like `SkywatchQuestData.spirePlaced`.
7. **New enemies need bestiary icons and locale names in both languages** or
   `tools/locale_audit.py` fails, and rightly.
8. **Two places from the chapter brief still have no layout**: the **Kite Mast**
   (small, Skyway or Driftlands — where the undelivered post ended up, a mast hung
   with `skyparcel`) and the **Anchor Terrace** (small, Driftlands, near the spire
   — four chairs, one table, three of them dusty). Both are storytelling
   set-pieces at Wayside scale (roughly 11×9 and 9×9) and both are pure reuse:
   the mast is `skyparcel` plus a `wardencandelabra` on a plinth, the terrace is
   one `skywatchdinnertable` pair and four `skywatchchair`. They are listed here
   rather than dropped.
9. **Do not build two collectibles and two endgame tiers.** §3.0 is the map: the
   Ledger feeds Magpie, the Signet reveals the map, the Sovereign chain owns the
   Anvil, and the Anvil pays Casings into Vane's tier. If any of those four drift
   apart in implementation the player ends up with four half-loops instead of one
   economy.
