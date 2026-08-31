# Standing asset work order

A queue, not a briefing. A sprite worker picks the **top unchecked item**, does
that one, reports, and stops. The reviewer verifies, ticks the box, commits, and
dispatches the next. This file is the memory between runs — two machine reboots
have already eaten in-flight work, and anything not written here is lost.

**Every target below is measured**, against the vanilla dump in
`vanilla-sprites/` (items, mobs, objects, and — since 2026-08-30 — 120 tiles).
Do not renegotiate a number without re-measuring the analogue and saying so.

## Rules that apply to every item

1. **Read `.claude/skills/necesse-pixel-art/SKILL.md` first.** Then open 2-3
   vanilla sprites of the same category and match their *construction*, not
   just their palette.
2. **Change the generator, never the PNG.** `tools/asset_generator/` owns every
   file under `src/main/resources/`; a hand-edited PNG is silently reverted.
3. **Palette ramps only** (`palette.py`), outline `palette.OUTLINE`, light from
   top-left. Adding a *step* to an existing ramp is allowed and should be said
   out loud; changing a biome's hue is not — the playtest marks those KEEP.
4. **Never redraw anything on the KEEP list** in `docs/PLAYTEST_LOG.md`:
   tulips, grasses, reeds, small flowers, mini vegetation, the Aurora plant core
   sprite, Zephyr Finch, Dewsnail, Zephyr Ray, Storm Wisp, tree sizes and
   silhouettes.
5. **Determinism**: regenerating twice must produce identical bytes.
6. **Nothing else may change.** The reviewer diffs the whole regenerated tree.
7. **No git commands that write.** The reviewer commits.
8. **Look at it.** Render a contact sheet with the vanilla analogue beside it,
   at 1x AND magnified, on the ground it will stand on. Every art fault this
   project has shipped passed its numbers and was obvious in a picture.
9. Pillow is not installed system-wide here:
   `PYTHONPATH=/home/blackoffset/dev/pylib python3 …`

---

## What a whole biome actually needs — answering "was brauchen wir für tiles?"

Asked on 2026-08-31, while supplying Beetle World art. Measured against what
the Skyreach's own biomes ship, so these are the real minimums, not a wish list.

### The one required file per ground: `tiles/<name>_splat.png`

Format, from `docs/research/splat-format.md` (decompiled and pixel-verified):

- Stacked **224x96 blocks**. Each block is a **7-column x 3-row grid of 32x32
  cells** — 21 cells, whose meaning is fixed engine-wide, not per tile.
- **Width = 224 x frames.** One frame (224) for still ground; more only for an
  animated surface like water.
- **Height = 96 x sections.** Each section is a random per-position variant. Two
  or three is normal; one is legal and reads repetitive.
- So the smallest legal new ground is **224x96**, and a good one is
  **224x192** or **224x288**.

Two rules that are not optional, both learned the expensive way and both gated
by `tools/tile_behaviour_audit.py`:

1. **Every vanilla splat in the game is 100% coherent on a 2x2 pixel block
   grid**, natural ground and crafted floor alike, without one exception. That
   is how vanilla gets its density without dithering. Texture drawn on single
   pixels fails, however good the numbers look.
2. **Density is a gameable number.** Natural ground carries 294-712 opaque-ish
   texture pixels per full-tile cell with **mean |dRGB| 7-27** off the base
   colour. Hitting the density while missing the loudness produced camouflage
   netting once already.

A liquid has **no legacy fallback**: without `_splat` it renders as a flat
colour quad. Terrain does fall back to a plain `tiles/<name>.png` + the shared
mask, so a terrain tile can ship without `_splat` in a pinch.

### The rest of the family, per ground

A tile is not finished when the PNG exists. Each of these has failed a release
on its own at least once:

| piece | where | what fails without it |
|---|---|---|
| palette ramp | `tools/asset_generator/palette.py` | texture ends up several times too loud (needs `grain_d`/`grain_l` at ~7 RGB either side of base) |
| tile class | `stairwaytoheaven/tiles/` | `TerrainSplatterTile` subclass with a **terrain priority** — that number decides which ground wins where two meet |
| registration | `StairwayToHeavenMod.registerTiles` | — |
| painter branch | `SkyTerrainPainter.describeTile` | the tile is registered and never placed. The Aurora Shoals wore cloudturf for three releases exactly this way |
| both locales | `en.lang` / `de.lang` | `locale_audit` fails |
| ledger row | `docs/CONTENT_LEDGER.md` | `content_ledger --check` fails |
| audit role | `tools/tile_behaviour_audit.py` | the tile is never measured against vanilla |

### For a whole biome, on top of the ground

What the four shipped sky biomes each carry, as the shape to match:

1. **One ground tile** (above). Optionally a second for its barrens — every sky
   biome lets `skystone` surface through as bare rock.
2. **A `Biome` subclass**: mob spawn table, critter table, `getCrateLootTable`
   (so opening a crate tells you where you are), `getUnderLiquidTile`.
3. **A band in the biome field** — or, like the Outlands, a rule that cuts it
   out of the others.
4. **Objects that only grow there**, at roughly **0.30-0.39 objects per land
   tile**. That is the measured density of every shipped sky ground; below ~0.10
   a biome reads as empty, which is what the grey skystone ground did.
5. **At least one structure**, or the region is scenery you walk through.

### So, concretely, for Beetle World

Supplied so far: `evilwall` (integrated). What would finish it:

- [ ] **`tiles/beetleground_splat.png`** — its own ground. It currently borrows
      `beetlefreaktile`, which was drawn for the Veil's Hollows and is on
      vanilla's `splattingmaskwide` stencil. 224x192 or 224x288.
- [ ] **A second, calmer ground** to interrupt the first, the way `blackpeat`
      does now. Same format.
- [ ] **3-5 objects** that grow nowhere else (32x32 world sheet + 32x32 item
      icon each, >= 300 opaque px per icon).
- [ ] Optional: a **liquid** if the region wants one — then `_splat` is
      mandatory and needs the animation frames.

Anything drawn on a vanilla sheet should keep saying which one in the filename.
That is what made `evilwall` land correctly: the name said `crystalwall`, and
looking that up in the jar is what revealed it is a `RockObject` on 16px cells,
not a wall on 32px ones.

---

## Queue

### 0. Halda's Fermentation Vat — the next content family, NOT art alone
- [ ] `skywatchvat` station + tech, four brews, and the fattening feed

The player, on the residents shipped in `80557e5`: *"die sollen nicht nur Shops
haben für items die sonst auch jeder kauft... der eine soll sich speziell um die
Braufässer kümmern und Bier, met etc an Vanilla Fässern herstellen und
zusätzlich Mastfutter herstellen können an Fässern für Tiere aus skyreach und
normale tier2 (also Schweine etc) dass ihre Produktion steigert."*

**Verified before designing, so nobody re-derives it:**

- Vanilla's `barrel` is an `InventoryObject` — plain storage. It is NOT a
  crafting station, has no tech, and a settler cannot work at it. Brewing "at
  vanilla barrels" is therefore impossible additively; it needs OUR station,
  shaped like a barrel. `RecipeTechRegistry` has no brewing or fermenting tech
  either, so that is ours too. Build it on `StormglassKilnObject`, which is
  already the cheese-press pattern: settler loads it, walks away, collects.
- **The feed bonus has to live in the MOB, not the item.** The trough calls
  `mob.onFed(item)` directly (`FeedingTroughObjectEntity:142`). So:
  - our own animals (NimbusYak, Thunderquill, Glimmergoat, CloudLamb) CAN take
    the bonus from a trough, because we own their `onFed`;
  - **vanilla tier-2 animals cannot** — their `onFed` is vanilla's and the mod
    is additive-only. Do not promise it.
  - The half that DOES reach vanilla animals is hand-feeding:
    `GrainItem.onMobInteract` is overridable, and `HusbandryMob.birthingCooldown`
    is a public field, so a custom feed can shorten a vanilla pig's breeding
    cooldown when fed by hand.
- `canFeed` requires `item.item instanceof GrainItem`, so the feed MUST extend
  `GrainItem` or no trough will accept it.

Art needed: one 32x32 barrel-vat object sheet (+ `_on` lit sibling, optional —
`fromFileRaw` degrades to the cold sheet), one item icon per brew, one for the
feed. Reference vanilla `barrel.png` and the mod's own `stormglasskiln`.


### 1. Skystone Golem — not too small for its cell; too small for a golem
- [ ] **`mobs/skystonegolem.png`** at 96px + the Java change (reviewer's)

The player: *"golem sollte neu gemacht werden weil er viel zu klein ist statt
die grösse eines Golems zu haben im Game."*

**This entry has been wrong twice. The measurements below are the third and
verified set** — taken with SQUARE cells, which is what the renderers use
(`.sprite(x, y, size)`), and with the cell size each mob's own class passes.

| | cell | mass | bbox |
|---|---|---|---|
| vanilla `crystalgolem` | 64 | 1232 | 36x62 |
| vanilla `ascendedgolem` | 64 | 1232 | 36x62 |
| vanilla `boar` | 64 | 1536 | 54x42 |
| **ours** | 64 | **1390** | **43x50** |
| vanilla `furnacegolem` | **96** | **3768** | **70x78** |

Two earlier claims here were false and are retracted: that vanilla golems use
64x80 cells (they do not; cells are square, and `boar` and `sheep` ship the same
384x320 sheet), and that ours is "lighter than a boar" in any meaningful sense
(a boar is wide and low, a golem narrow and tall — the shapes are not
comparable). **Ours is in fact HEAVIER than both small vanilla golems.**

What is actually wrong is HEIGHT. Ours is 50px tall where vanilla's small golems
are 62 — it is stubby, and it does not tower. And the small golems are not what
the player means by "die grösse eines Golems": `furnacegolem` at 96 is.

Target: the **furnace golem's 96px square cell** — sheet **576x384**, 6 columns
x 4 rows, rows Up/Right/Down/Left. Densest frame **>= 2800 opaque px**, bbox at
least **64x74**. Reference `furnacegolem.png` for how vanilla fills a 96 cell.

**The Java is the reviewer's, and it is not optional.** `SkystoneGolemMob`
hardcodes `.sprite(sprite.x, sprite.y, 64)`, `drawX -32`, `drawY -51`. Vanilla's
own 96 golem uses `drawX -48` and `drawY -78`; copy those rather than deriving
them. A PNG-only change produces a mob drawn from the wrong rectangle.

Note for the worker: `CELL = 64` in `gen_mobs.py` is module-wide and used by
every other mob. The golem needs its own constant, not a change to that one.

### 2. Cloud Lamb — a sheep with different wool has no reason to exist
- [ ] **`mobs/cloudlamb.png`**, `mobs/cloudlamb_sheared.png`,
      `mobs/icons/cloudlamb.png`

The player: *"Schafe sollten eher in Furby Richtung gehen oder sowas da es ja
schon Schafe gibt."* Currently 734px at 38x27 against vanilla `sheep` 1180px at
46x40 — smaller than the animal it is meant to be an alternative to, and built
on the same silhouette.

Target: mass >= 950, bbox >= 44x34. **Its own silhouette**: rounder and
squatter than a sheep, an oversized head low on a fat body, big forward-facing
eyes, tiny feet under the mass, tufted ears. The style guide's "cute roundness"
taken further than anywhere else in the mod. Keep the Driftlands palette so it
still belongs to the biome. The sheared sheet must read as the same creature,
deflated — that is where the joke lands.

### 3. The Mistsea — the last loud, non-block-built surface
- [ ] **`tiles/mistsea_shallow_splat.png`**, **`tiles/mistsea_deep_splat.png`**

The whole tile pass landed except this one, which is in
`tile_behaviour_audit.KNOWN_UNFIXED`. Density is fine (617-704); it runs **mean
|dRGB| 31.8-49.7 at 40-48% 2x2 coherence** where every vanilla splat in the game
is 100% coherent and vanilla's only liquid, lava, runs mean 4.3.

Target: keep the density, keep the rolling cloud-deck design and its 8-frame
ping-pong, but put the tone on the **2x2 block grid** and bring the mean to
**<= 14, peak <= 34**. `MISTSEA` will need `grain_d`/`grain_l` steps like the
other grounds. **Read the murkwater work in `gen_veil.py` first** — it solved
exactly this for a liquid, including that a per-frame re-randomised grain boils
instead of flowing, and that a time-keyed bright accent blinks across every tile
at once because animation time is global.
This is the sky's signature surface: one candidate, then stop for review.

### 4. The thin item icons — 35 left under vanilla's floor
- [ ] batch A: `cloudbell` 120, `thunderbloom` 124, `skywatchtelescope` 129,
      `aurorabloom` 141, `prismshard` 141, `stormcrystal` 141
- [ ] batch B: `skyballoon` 146, `auroralily` 148, `cloudberry` 149,
      `gloomwillow` 149, `cloudpufftreat` 157, `silverbell` 161
- [ ] batch C: `skywatchastrolabe` 166, `cinderpearl` 178, `aetheriumore` 181,
      `aetheriumbar` 184, `cloudberrybush` 186, `skystone` 189
- [ ] batch D: `windsilk` 198, `starfall` 203, `skytulip` 207, `skyreeds` 210,
      `mistglasslantern` 213, `charwood`/`nimbuswood`/`prismwood` 225
- [ ] batch E: `staticmoss` 230, `stormscreed` 239, `skystonerock` 245,
      `seraphstatue` 247, `seancecircle` 251, `windwheat` 255,
      `skywatchchalice` 267, `catbasket` 277, `skyparcel` 283

Every vanilla 32x32 item icon in the dump carries **288-712 opaque px, median
440**, in a bbox of at least 16x20 and usually 24x26. Target **>= 300 px, bbox
>= 20x22** each, and name the vanilla analogue you worked from.

Two lessons from the twelve already done, both cost a correction pass:
- **Mass without form fails.** A fattened stroke passes the number and still
  reads as a hairline. Draw the object, then check the number.
- **Say what the SUBJECT is.** "Must read as that plant" produced a full bloom
  for `aurorapetal`, fuller than `aurorabloom` beside it in the inventory. For a
  picked material, draw it picked.
`aurorabloom` in batch A is the pointed one: the petal picked from it is 461 px.

### 5. Held weapon sprites on the wrong canvas
- [ ] **`player/weapons/tempestedge.png`**, **`player/weapons/galehowl.png`**

Both sit on 32x32 while every later mod weapon matches vanilla's larger held
sheets — `skyreave` 96x95 against `quartzglaive` 104x88, `thunderhead` 22x62
against `tungstengreatbow` 20x60. The mod's two original weapons are drawn at
roughly a third of the linear size of everything else in the player's hand.
**Changing the canvas is rendering geometry, not art**: propose the size, do not
ship it, and let the reviewer verify the attack animation first.

### 6. The Veil, in its own language — needs design before art
- [ ] blocked: no chapter brief exists yet

`docs/ROADMAP.md`: contrast, not darkness. Poison green on violet, bone white on
black, stripes, checkerboard, spirals, sickly pink, brass and verdigris — black
is outline and shadow, never fill. One saturated accent per set that nothing
else has, and one funny piece per chapter. The Model Town, the Office of
Eternity, the Ashen Reach zombie quarter, Mortimer and Vesper are all unbuilt.
**Do not start this from the roadmap alone** — it needs a chapter brief naming
the pieces, or it becomes 30 decorative PNGs that never touch the player.

---

## Done

- 12 item icons, 29-117 px -> 310-655 (`13afc95`)
- 6 world sprites incl. the ghost lantern 0.29 -> 1.0 of vanilla (`f6debe4`)
- 6 natural terrains, 63-114 density -> 364-406 at vanilla loudness (`f8e72b2`)
- 4 craftable floors + murkwater onto the 2x2 grid (`6854591`)
- `aurorashoal`, the Aurora Shoals' own ground, new (`ab4a5e7`)
- `skycrate` on vanilla's 6-variant crate layout (`cb60fd1`)
