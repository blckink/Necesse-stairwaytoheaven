# Necesse 1.3.2 Terrain/Liquid `_splat` Autotile Format

Source: decompiled game classes at `/home/user/necesse-game/decompiled/necesse/`
(game v1.3.2), read in full, plus pixel-level measurement of the vanilla sprite
pack at `/home/user/necesse-game/sprites/tiles/` (Python/PIL). Every claim
below cites the exact class/method/line it came from, or the exact file(s)
measured. Per policy, no multi-line decompiled method bodies are reproduced —
only single-line constants/signatures, plus derived tables/diagrams built from
reading the logic and measuring pixels.

**Relationship to `docs/research/asset-formats.md`**: that document already
traced `TerrainSplatterTile`/`LiquidTile` loading order correctly (its own
§1, §2) and agrees with everything below. It explicitly flagged two open ambiguities it
could not resolve (its final "Key ambiguities" section): **(1)** the exact
pixel content of the 17 non-full-tile cells in a `_splat.png`, and **(2)**
`splattingmask.png`'s internal grid/shape. This document resolves both, by
combining the `SplattingOptions` selection logic with direct pixel measurement
of multiple vanilla files, and adds the liquid shader-blending model and a
concrete generator recipe that asset-formats.md didn't attempt.

---

## 1. Executive summary (answers to the 5 tasks)

1. **Load order** (`TerrainSplatterTile.generateSplattingTextures`,
   `TerrainSplatterTile.java:132-160`): tries `tiles/<name>_splat.png` first
   (strict load), and only on `FileNotFoundException` falls back to the plain
   `tiles/<name>.png` + shared alpha mask. The public `preferLegacySplatting`
   flag inverts this order but still falls back to whichever format exists.
   **No vanilla tile class sets `preferLegacySplatting = true`** (verified by
   grep across the full decompiled tree) — vanilla always prefers `_splat`
   when present.
2. **Is `_splat` required for terrain to render? No.** The legacy path
   (`generateOldTerrainSplatting`, `TerrainSplatterTile.java:162-186`) is a
   fully working, still-shipping renderer. **Eight real vanilla 1.3.2 tile
   IDs ship with no `_splat` file at all today**: `crystaltile`,
   `factoryfloor`, `ascendedgrowth`, `amethystgravel`, `sapphiregravel`,
   `emeraldgravel`, `rubygravel`, `topazgravel` (§3). They render correctly,
   with real (if cruder) blending, using only a plain grid PNG plus the
   shared `tiles/splattingmask.png` or `tiles/splattingmaskwide.png`.
3. **Is `_splat` required for liquids? Effectively yes, for any textured
   liquid.** `LiquidTile` has **no legacy-grid fallback at all** — it only
   ever tries `_splat` files, and if none load, the liquid renders as a flat
   color quad with no water-like texture whatsoever (§7.1). This is the one
   real asymmetry between terrain and liquid tiles.
4. **Cell grid**: both terrain and liquid `_splat` atlases share one format:
   stacked **224×96 px blocks**, each block a **7-column × 3-row grid of
   32×32 px cells**. Width = `224 × frames` (animation), height =
   `96 × sections` (random per-position visual variants). The meaning of all
   21 cells in a block is fixed engine-wide (not per-tile) and is fully
   mapped in §5, verified against 3 independent vanilla files both
   numerically (alpha-channel sampling) and visually (rendered contact
   sheets).
5. **Generator recipe**: §9 gives exact file names/dimensions/cell content
   for a new terrain (`cloudturf`) and a new liquid (`mistsea`).

---

## 2. `TerrainSplatterTile`: texture loading order

Class: `necesse.level.gameTile.TerrainSplatterTile extends GameTile`
(abstract). Constructor takes `terrainTextureName` and an optional
`alphaMaskTextureName` (default `"splattingmask"`,
`TerrainSplatterTile.java:38-47`). Loading happens in `loadTextures()` →
`generateSplattingTextures()` (`TerrainSplatterTile.java:83-160`).

`generateSplattingTextures()` branches on `preferLegacySplatting`
(`TerrainSplatterTile.java:132-160`):

| `preferLegacySplatting` | Try 1 | Try 2 (on failure) | Try 3 (on failure) |
|---|---|---|---|
| `false` (vanilla default, always) | `tiles/<name>_splat.png` via `fromFileRaw` (strict) → new-style | `tiles/<name>.png` via `fromFile` (never throws; substitutes `GameResources.error`) → legacy | — |
| `true` | `tiles/<name>.png` via `fromFileRaw` (strict) → legacy | `tiles/<name>_splat.png` via `fromFileRaw` (strict) → new-style | `tiles/<name>.png` via `fromFile` (never throws) → legacy |

Both final legacy branches call `generateOldTerrainSplatting(texture)`
(`TerrainSplatterTile.java:162-186`), which is **never actually skipped** —
even if both files are truly absent, `fromFile` silently substitutes the
shared error/checkerboard placeholder texture and the tile still renders
(just with wrong art), it does not crash. The only crash case is a malformed
**alpha mask**: `generateOldTerrainSplatting` requires
`mask.getWidth() == mask.getHeight()` or throws `IllegalStateException`
(`TerrainSplatterTile.java:165-166`) — this can only happen with a
custom/corrupt `alphaMaskTextureName`, since the two built-in vanilla masks
are both square.

`isUsingNewTerrainSplatting` (`TerrainSplatterTile.java:32,89-91`) records
which branch won and is read by neighboring tiles (via `SplattingOptions`) to
decide whether *this* tile's own edge pieces should be drawn using the new
marching-squares selection or the old 4-corner selection.

### Item icon (`generateItemTexture`, `TerrainSplatterTile.java:62-81`)

Tries `_splat` first (icon = cell `(3,0)`, one of the four "full tile"
variants — see §5); falls back to plain `tiles/<name>.png` (icon = cell
`(0,0)`). Either way it is multiplied against the shared `tiles/itemmask.png`
(32×32). Cell `(3,0)` must therefore look acceptable as a **standalone,
un-blended icon**, not just as a background fill.

---

## 3. Legacy format: does it really still render in 1.3.2?

**Yes — unambiguously.** `generateOldTerrainSplatting`
(`TerrainSplatterTile.java:162-186`) is live production code, not a deprecated
stub, and current vanilla 1.3.2 ships tiles that depend on it exclusively.

### 3.1 Mechanism

For a plain `tiles/<name>.png` of size `(32×cols)×(32×rows)`, the engine
builds one pre-blended atlas **per (i,j) source cell** at tile-registry load
time: it stamps that single 32×32 source cell repeatedly across a canvas the
same size as the alpha mask, then multiplies the whole thing by the mask
(`MergeFunction.MULTIPLY`, `TerrainSplatterTile.java:170-184`). At draw time,
`SplattingOptions`' old-style corner logic
(`generateSplattingTopLeft/TopRight/BotRight/BotLeft`,
`SplattingOptions.java:280-362`) picks a 16×16 sub-rectangle out of that
pre-blended atlas per adjacent tile. **The mod author supplies zero blend
art** — every fade shape comes from the one shared mask file, multiplied
against whichever flat variant cell `getTerrainSprite()` selected.

### 3.2 `getTerrainSprite` conventions actually used in vanilla

`TerrainSplatterTile.getTerrainSprite(...)` (`TerrainSplatterTile.java:49-51`)
defaults to `(0,0)`; concrete tiles override it. Two patterns are used in
vanilla, both confirmed by reading every override in
`necesse/level/gameTile/`:

| Pattern | Example classes | Formula | Texture shape |
|---|---|---|---|
| Random vertical strip (most common, ~18 classes) | `DirtTile`, `GrassTile`, `SandTile`, `AshTile`, `SwampGrassTile`, … | `Point(0, seededRandom(tileX,tileY) % (height/32))` | `32 × (32×rows)` — one column, N row-variants, chosen per-tile-position but stable (seeded by `GameTile.getTileSeed`, not per-frame) |
| Deterministic 2D grid, keyed by absolute tile coordinate (no randomness) | `CrystalTile`, `FactoryFloorTile` (3×3); `CrystalGravelTile`, `AscendedGrowthTile` (4×4) | `Point(tileX mod N, tileY mod N)` | `(32×N) × (32×N)` square grid |

### 3.3 Concrete vanilla tiles that are legacy-only *today*

Cross-checked against the actual sprite pack (files present/absent) and
`TileRegistry.java` registration calls:

| Tile ID | Class | Texture file | `_splat` sibling exists? | Alpha mask used |
|---|---|---|---|---|
| `crystaltile` | `CrystalTile` | `tiles/crystaltile.png` (96×96) | No | `splattingmaskwide` |
| `factoryfloor` | `FactoryFloorTile` | `tiles/factoryfloortile.png` (96×96) | No | `splattingmaskwide` |
| `ascendedgrowth` | `AscendedGrowthTile` | `tiles/ascendedgrowth.png` (128×128) | No | `splattingmaskwide` |
| `amethystgravel`, `sapphiregravel`, `emeraldgravel`, `rubygravel`, `topazgravel` | `CrystalGravelTile` (×5, `TileRegistry.java:247-251`) | `tiles/<name>gravel.png` (128×128) | No | `splattingmaskwide` |

(`AshTile`, `CryptAshTile`, `SpiderNestTile` also pass `"splattingmaskwide"`
to their constructor, but all three *do* ship an `_splat.png` sibling today —
`ash_splat.png`, `cryptash_splat.png`, `spidernest_splat.png` — so the mask
argument is currently dead weight for them; it would only activate if their
`_splat` file were ever removed.)

**Practical conclusion**: a mod terrain that ships only a plain
`tiles/<name>.png` strip renders correctly, with genuine (if visibly softer
and less "hand-authored") blending against neighbors, exactly like these 8
vanilla tiles do right now. `_splat` is an upgrade (animation frames, crisper
hand-painted blend art, more visual variants), not a requirement.

### 3.4 The two built-in alpha masks, measured

Both `tiles/splattingmask.png` and `tiles/splattingmaskwide.png` are
**64×64px**, square (satisfying the `IllegalStateException` check), and
divide into a **2×2 arrangement of 32×32 quadrants**, each quadrant a
different soft alpha shape (RGB is flat white in both; only alpha varies).
Measured mean alpha per 32×32 quadrant (0=fully transparent, 255=fully
opaque):

| Quadrant | `splattingmask.png` | `splattingmaskwide.png` | Shape (visual) |
|---|---|---|---|
| Top-left (0,0)–(31,31) | 23 | 57 | Fully/mostly transparent — the "far corner" wipe |
| Top-right (32,0)–(63,31) | 84 | 118 | Soft gradient, opaque at the top edge fading down |
| Bottom-left (0,32)–(31,63) | 84 | 118 | Soft gradient, opaque at the left edge fading right |
| Bottom-right (32,32)–(63,63) | 140 | 169 | Soft **radial blob** (round, brightest at center) |

`splattingmaskwide` reproduces the exact same 4-shape layout with a visibly
wider/softer falloff (every quadrant's mean alpha is higher, i.e. more of it
is filled in) — it's an alternate built-in mask for tiles that want a
broader blend zone, not a hypothetical "bring your own" example. Of the 43
`TerrainSplatterTile` subclasses in 1.3.2, exactly 7 pass `"splattingmaskwide"`
explicitly (`AshTile`, `CryptAshTile`, `SpiderNestTile`, `CrystalTile`,
`FactoryFloorTile`, `AscendedGrowthTile`, `CrystalGravelTile` — the last
backing 5 separate tile IDs, §3.3); the other ~36 use the default
`"splattingmask"`. Either way it's a **generic geometric mask shared across
many tiles** — it carries no tile-specific detail, which is exactly why
legacy blending looks like a plain soft fade rather than the organic "worn
edge" look of `_splat` art.

For comparison, the liquid-only `tiles/shoremask.png` (also 64×64, same 2×2
quadrant addressing, used by the old-style `LiquidSplattingOption` for a
liquid with no `_splat`, `SplattingOptions.java:413-448`) is visually a hard
jagged/dithered "surf" edge, not a soft blur — a different art style for the
same coordinate contract, confirming the shape is pure content, unconstrained
by engine logic beyond "square, and read in 16px sub-steps."

---

## 4. `getTerrainSprite` / priority interaction

`getTerrainPriority()` (abstract, `TerrainSplatterTile.java:53`) returns one
of six documented bands (`TerrainSplatterTile.java:24-29`):

| Constant | Value |
|---|---|
| `PRIORITY_TERRAIN_BOT` | 0 |
| `PRIORITY_TERRAIN` | 100 |
| `PRIORITY_TERRAIN_TOP` | 200 |
| `PRIORITY_FLOOR_BOT` | 300 |
| `PRIORITY_FLOOR` | 400 |
| `PRIORITY_FLOOR_TOP` | 500 |

`comparePriority` (`TerrainSplatterTile.java:55-60`) orders tiles by this
value (tiebreak: tile ID). `SplattingOptions.splatsInto`
(`SplattingOptions.java:368-382`) uses it to decide direction: an adjacent
terrain tile splats **onto** the current tile only if the adjacent tile's
priority is **higher** — e.g. `DirtTile`/`SandTile`/`RockTile` return `0`
(`PRIORITY_TERRAIN_BOT`), `GrassTile`/most others return `100`
(`PRIORITY_TERRAIN`), so grass draws its edge pieces on top of dirt, never the
reverse. `getTerrainSprite` is orthogonal to this — it only ever picks
*which flavor of this tile's own plain/legacy art* to show; it has no say in
who blends onto whom.

---

## 5. The new `_splat` cell grid — verified cell-position → meaning table

### 5.1 How a draw position is resolved

`TerrainSplatterTile.getSplattingTexture` (`TerrainSplatterTile.java:93-115`)
picks **which 224×96 block** to use:

- `frame = width/224` frames; if >1, animated as a **ping-pong** (0→frames-1→0)
  over `frames × 400` ms (`GameUtils.getAnim`, folded back for the return
  trip) — no vanilla *terrain* file currently ships >1 frame (§6).
- `section = seededRandom(tileX, tileY, primeIndex=5) % (height/96)` — a
  deterministic-per-position pick between 0 and `sections-1`.

Within the chosen block, exactly **one more seeded pick** (`primeIndex=9`)
selects which of 4 "fully surrounded" cells to show when nothing is blending
(`NEW_FULL_TILE_SPRITES`, `TerrainSplatterTile.java:23`); everything else in
the block is addressed directly by `SplattingOptions` using fixed
`(spriteX,spriteY)` coordinates baked into engine logic
(`SplattingOptions.java:28-45, 239-253`), always drawn as a **full 32×32
tile with zero offset** directly over the target position — i.e., 100% of
the visible blend shape at a boundary comes from whatever alpha is baked into
that one cell; there is no additional runtime masking for the new format
(contrast with §3.1).

### 5.2 Neighbor indexing and the march-value bit layout

`Level.adjacentGetters` (`Level.java:184-186`) fixes the 8-neighbor order used
everywhere in `SplattingOptions`:

`index: 0=NW  1=N  2=NE  3=W  4=E  5=SW  6=S  7=SE` (relative pixel offsets
`(-1,-1),(0,-1),(1,-1),(-1,0),(1,0),(-1,1),(0,1),(1,1)`).

For the 4 cardinal neighbors, `getMarchValue`
(`SplattingOptions.java:191-210`) packs "does this cardinal neighbor equal
the candidate splatting tile" into a 4-bit value:

`marchValue = N·1 + E·2 + S·4 + W·8` (bit0=N, bit1=E, bit2=S, bit3=W)

`SplattingOptions.newTerrainSprites[marchValue]`
(`SplattingOptions.java:28-45`) then gives the cell(s) to draw. Separately,
each of the 4 **diagonal** neighbors gets its own isolated-corner check
(only fires when neither adjacent cardinal neighbor is the same tile —
`SplattingOptions.java:239-253`).

### 5.3 The verified 21-cell table

Built from the code tables above, then cross-checked by sampling per-cell
alpha (full/quadrant/edge means) on three independent, unrelated vanilla
files — `dirt_splat.png`, `snow_splat.png`, `ash_splat.png` — and by visually
inspecting rendered, labeled, checkerboard-composited contact sheets of the
same cells. All three files agree with the predicted shape family at every
position (full-tile cells measured >250/255 mean alpha with zero variance
across the cell; directional cells measured opaque along exactly the
predicted edge and near-zero along the opposite edge; the 4 corner cells
measured near-zero everywhere except the one predicted quadrant).

Columns 0–2 and 3–6 split the block into "blend pieces" and "plain tile
variants":

| Cell (x,y) | Trigger (when this cell is drawn) | Verified pixel content |
|---|---|---|
| (3,0) (4,0) (5,0) (6,0) | Random pick (`NEW_FULL_TILE_SPRITES`) whenever this tile is fully surrounded by itself — also the **item-icon source**, cell (3,0) | **Fully opaque**, no transparency anywhere — 4 independent plain-tile art variants |
| (1,1) | marchValue 15 — **all 4** cardinal neighbors are this tile | Opaque along all 4 edges; center may carry a small organic hole (artistic noise) but is never required to — safe to make fully opaque |
| (1,0) | marchValue 4 — **S only** (neighbor below matches) | Opaque near **bottom** edge, transparent near **top** edge |
| (1,2) | marchValue 1 — **N only** | Opaque near **top** edge, transparent near **bottom** edge |
| (2,1) | marchValue 8 — **W only** | Opaque near **left** edge, transparent near **right** edge |
| (0,1) | marchValue 2 — **E only** | Opaque near **right** edge, transparent near **left** edge |
| (3,1) | marchValue 9 — **N+W** | Opaque in top-left 3 quadrants, transparent bottom-right quadrant |
| (4,1) | marchValue 3 — **N+E** | Opaque top-left/top-right/bottom-right, transparent bottom-left |
| (3,2) | marchValue 12 — **S+W** | Opaque top-left/bottom-left/bottom-right, transparent top-right |
| (4,2) | marchValue 6 — **E+S** | Opaque top-right/bottom-left/bottom-right, transparent top-left |
| (5,1) | marchValue 11 — **N+E+W** | Opaque along top/left/right edges, transparent-leaning bottom |
| (6,1) | marchValue 7 — **N+E+S** | Opaque along top/right/bottom edges, transparent-leaning left |
| (5,2) | marchValue 14 — **E+S+W** | Opaque along right/bottom/left edges, transparent-leaning top |
| (6,2) | marchValue 13 — **N+S+W** | Opaque along top/bottom/left edges, transparent-leaning right |
| (0,0) | Isolated **SE**-diagonal-only touch | Large rounded blob filling ~3/4 of the cell; notch cut from the **far (NW)** corner |
| (2,0) | Isolated **SW**-diagonal-only touch | Same blob shape; notch cut from the **far (NE)** corner |
| (0,2) | Isolated **NE**-diagonal-only touch | Same blob shape; notch cut from the **far (SW)** corner |
| (2,2) | Isolated **NW**-diagonal-only touch | Same blob shape; notch cut from the **far (SE)** corner |

Small derived diagram of the layout (letters are the mnemonic used above, not
pixel data):

```
col:        0          1          2          3       4       5       6
row0:  [SE-corner] [S-edge]  [SW-corner] [FULL-A][FULL-B][FULL-C][FULL-D]
row1:  [E-edge]    [ALL-4]   [W-edge]    [NW]    [NE]    [NEW]   [NES]
row2:  [NE-corner] [N-edge]  [NW-corner] [SW]    [ES]    [ESW]   [NSW]
```

**Design takeaway for a generator**: the engine does not care about the exact
*shape* of the fade (round vs. jagged vs. hard-edged is a pure art choice —
vanilla favors soft rounded blobs) — it only cares that alpha is high near
the edge(s) implied by the trigger and low elsewhere, so that drawing the
neighbor's cell at zero offset over the current tile looks like that
neighbor's material creeping in from the correct side(s). A generator can
satisfy every cell with a simple procedural rule: "opaque disc centered
outside the cell on the correct side(s)/corner, clipped to the cell,"
without ever hand-painting per-tile detail.

---

## 6. Atlas dimension math — measured across the vanilla pack

`width / 224` = frame count, `height / 96` = section (variant) count — but
**the division is a runtime integer floor-division, not a hard requirement
that the file be an exact multiple**; leftover rows/columns are simply never
sampled. Measured every `*_splat.png` in `/home/user/necesse-game/sprites/tiles/`
(Python/PIL, full listing kept in the research scratchpad):

| Pattern | Width | Height | Frames | Sections | Examples |
|---|---|---|---|---|---|
| Minimum viable | 224 | 96 | 1 | 1 | `ascendedcorruption_splat.png` |
| Typical terrain (no animation) | 224 | 192 / 288 / 384 / 480 / 576 / 672 | 1 | 2 / 3 / 4 / 5 / 6 / 7 | `dirt_splat.png` (2), `ash_splat.png`/`sand_splat.png` (3), `rock_splat.png`/`granite_splat.png` (4), `arcanicfloor_splat.png` (5), `grass_splat.png` (6), `scrapfloor_splat.png` (7) |
| Water liquids | 1792 | 224 | 8 | 2 (+32px unused) | all 8 `*water*_splat.png` files — every one is 1792×224 |
| Lava / Ooze (special liquids) | 1792 | 416 | 8 | 4 (+32px unused) | `lava_splat.png`, `ooze_splat.png` |

**No vanilla terrain `_splat` file uses more than 1 animation frame** —
animation is currently a liquid-only feature in shipped content, even though
`TerrainSplatterTile` fully supports multi-frame terrain. The water/lava/ooze
files' 224px / 416px heights are **not** exact multiples of 96 (they leave
32px of dead space per file) — proof that the engine tolerates this
gracefully; a generator should still prefer exact multiples (`96×N`) purely
to avoid wasting canvas.

### 6.1 Animation formulas

- **Terrain** (`TerrainSplatterTile.getSplattingTexture`,
  `TerrainSplatterTile.java:93-115`): ping-pong across `frames` over a
  `frames × 400` ms cycle (`GameUtils.getAnim`, folded back for the return
  half) — smooth forward/backward sweep, e.g. 4 frames → visits
  0,1,2,3,2,1,0,…
- **Liquid** (`LiquidTile.getNewSplattingFrame`, `LiquidTile.java:134-138`):
  plain forward loop, `frames × animTime` ms per full cycle (`animTime`
  defaults to 250ms per `TextureIndexes`'s 4-arg constructor,
  `LiquidTile.java:473-475`), hard-cuts back to frame 0 — no ping-pong.
- Both use the **level's shared clock** (`level.getLocalTime()`), so every
  tile of the same type animates in sync; the *section* (variant) pick is
  the one that's per-tile-position-seeded and stable, not animated.

---

## 7. Liquids (`LiquidTile` / `WaterTile`)

### 7.1 Constructor pattern and texture indexing

`LiquidTile(Color liquidColor, String... textureNames)`
(`LiquidTile.java:64-73`) stores an arbitrary list of base names.
`loadTextures()` (`LiquidTile.java:86-110`) — **only when
`!preferLegacySplatting`** — strictly tries `tiles/<name>_splat.png` for each
name; any missing file is silently skipped (`isUsingNewTerrainSplatting[i]`
stays `false`, that slot's texture stays `null`), no plain-grid fallback is
ever attempted for a liquid. `getTextureIndexes(level, x, y, biome)`
(overridden per subclass) maps `{freshShallow, freshDeep, saltShallow,
saltDeep}` indices into that name array; `WaterTile` passes **8** names
(fresh/salt × shallow/deep, doubled for a swamp variant,
`WaterTile.java:31-43`) and switches the index quadruple by biome
(`WaterTile.java:85-87`); `LavaTile` passes **one** name (`"lava"`) and
always returns `(0,0,0,0)` (`LavaTile.java:30-31,54-57`) — i.e. the same
`_splat` atlas layered 4× with itself, which is visually identical to a
single layer. This is the template for a minimal custom liquid.

The consequence of skipping `_splat` entirely is spelled out directly in
`LiquidTile.addDrawables` (`LiquidTile.java:365-415`): it branches on
`isUsingNewTerrainSplatting(level, tileX, tileY)`, and the `else` arm
(`LiquidTile.java:403`) draws nothing but a flat 32×32 quad
(`tileBlankTexture`) tinted by `getLiquidColor(...)` — no texture, no
shore-edge piece from this branch at all (the old-style shore edge in §7.5 is
added separately by `SplattingOptions`, not by this method). This is the
concrete code location backing the "effectively required for liquids" claim
in §1.

### 7.2 Liquid `_splat` atlases reuse the exact terrain cell grid

Confirmed by running the §5 classifier against `freshwater_shallow_splat.png`
(1792×224): every one of the 21 cells matches the same trigger/shape mapping
as the terrain files, cell-for-cell. `LiquidTile.getNewSplattingTexture`
(`LiquidTile.java:140-147`) and `addFullDrawables`
(`LiquidTile.java:288-333`) call the identical
`.sprite(spriteX, spriteY, 32)` pattern with the same `NEW_FULL_TILE_SPRITES`
/ `SplattingOptions.newTerrainSprites` coordinates as terrain. **A liquid
`_splat.png` and a terrain `_splat.png` are the same file format** — the only
difference is how many named atlases a `LiquidTile` subclass may load (1 to
N) versus a `TerrainSplatterTile` always loading exactly 1.

### 7.3 Shallow/deep and fresh/salt blending is shader-side, not baked

`LiquidTile.addFullDrawables` (`LiquidTile.java:230-333`) always looks up
**all four** of shallowFresh/deepFresh/shallowSalt/deepSalt (whichever
indices `getTextureIndexes` names) and hands them to the draw call as layered
`ShaderSprite`s (indices 1-3) plus per-corner `advColor` alpha values derived
from biome-blend and depth data (`LiquidTile.java:242-263`, corner alphas
`topLeftAlpha`/`topRightAlpha`/etc. from `BiomeBlendingOptions`). **The actual
interpolation curve executes in a GPU shader** whose GLSL source is not part
of the decompiled Java tree, so it cannot be quoted here — but the Java-side
contract is unambiguous: we only ever ship the 4 (or fewer) distinct
"look" atlases; the engine cross-fades between them per-pixel at render time
based on live depth/biome state. **No extra art is needed for shallow↔deep or
fresh↔salt transitions.**

By contrast, the shore/edge blending between a liquid and adjacent different
tiles (§5) is **not** shader-interpolated — it's the same direct
alpha-in-the-PNG technique as terrain: the engine draws the relevant
`_splat` cell at zero offset and lets its baked alpha do 100% of the work
(`SplattingOptions.NewLiquidSplattingOption`, `SplattingOptions.java:450-510`).

### 7.4 `watershallow.png` / `waterdeep.png` (32×192): unrelated cosmetic overlay

`WaterTile.loadTextures()` (`WaterTile.java:46-50`) always loads these two
**plain** (non-`_splat`, non-grid-contract) textures regardless of whether
`_splat` loading succeeded. They are a strip of `height/32 = 6` frames at
32px width, sampled only by `WaterTile.addLiquidTopDrawables`
(`WaterTile.java:124-157`): a per-tile 15%-chance roll draws one random frame
as a small brightened "glint/bobbing" overlay on top of the liquid, offset by
`getLiquidBobbing()`. This has **nothing to do with the splat/blend system**
— it's a `WaterTile`-specific decorative extra. A new liquid (e.g. `mistsea`)
does not need this file; `LiquidTile.addLiquidTopDrawables` is abstract and
can be implemented as a no-op, exactly as `LavaTile` does
(`LavaTile.java:130-134`).

### 7.5 Old-style liquid shore blending (`shoremask.png`)

When a liquid has **no** `_splat` at all, it isn't blend-less: the old-style
`LiquidSplattingOption` (`SplattingOptions.java:413-448`) still draws a
16×16 sub-piece of the shared `tiles/shoremask.png` (§3.4 — hard jagged surf
shape, not soft) tinted with `getLiquidColor(...)`, at the same corner
positions the legacy terrain system uses. So even a zero-asset custom liquid
gets a recognizable (flat-colored, jagged) shoreline against neighboring
terrain — it just gets no wave/foam **texture**, only a flat-tinted body and
a flat-tinted jagged edge.

### 7.6 Bucket icon — no splat-derived icon needed

`LiquidTile.generateItemTexture()` (`LiquidTile.java:75-84`) composites the
liquid's flat `Color` onto the shared vanilla `tiles/bucket.png` (32×64) —
it never reads any cell out of the `_splat` atlas. A new liquid needs no
icon art at all beyond picking a `Color`.

---

## 8. What's generated at runtime vs. what must ship in the file

| Artifact | Generated at runtime? | Where |
|---|---|---|
| Final packed GPU atlas (`GameTile.generatedTileTexture`) | **Yes**, always, for every tile/format | `GameTile.generateTileTextures` (`GameTile.java:98-101`) calls `SharedGameTexture.generate()`, a bin-packer over every `addTexture(...)`-registered `GameTexture`. Pure engine bookkeeping; irrelevant to asset authoring. |
| Legacy per-cell blend atlas (`splattingTextures[i][j]`) | **Yes**, once per plain-strip tile, at load time | `generateOldTerrainSplatting` (`TerrainSplatterTile.java:162-186`): replicate + multiply by shared mask. Author ships only the flat variant strip. |
| New `_splat` blend shapes (the 17 non-full cells) | **No** | Registered byte-for-byte via `tileTextures.addTexture(terrain)` (`TerrainSplatterTile.java:141,151`); every pixel the engine ever samples for a blend edge is a pixel we drew. Nothing is synthesized. |
| Liquid shallow↔deep / fresh↔salt cross-fade | **Yes**, per-pixel, in a shader | `LiquidTile.addFullDrawables` layers our 4 atlases as `ShaderSprite`s with per-corner `advColor`; interpolation math lives in GLSL, not in the decompiled Java. We only ship the endpoints. |
| Liquid shore blend against terrain | **No** (new-style) / **shared mask** (old-style) | New: our `_splat` cell alpha directly (§7.3 second paragraph). Old: shared `shoremask.png`, not liquid-specific (§7.5). |
| Item icon for a terrain tile | **Yes**, cropped+masked at load time | Cell (3,0) (or (0,0) legacy) × `itemmask.png` (`TerrainSplatterTile.java:62-81`). No separate icon file is ever read for a tile. |
| Item icon for a liquid | **Yes**, composited at load time | Flat color × `bucket.png` (`LiquidTile.java:75-84`). |

**Direct answer to "does alpha blending between tiles use our pixels or
engine masks?"**: it is a mix, split cleanly by format —
- **New `_splat` format** (terrain and liquid alike): 100% our pixels. The
  engine performs an ordinary alpha-composite of a neighbor's `_splat` cell
  directly over the current tile; the shape of every blend is whatever we
  painted.
- **Legacy plain-grid format**: our pixels (color) × one shared engine mask
  (shape) — the shape is never ours, only the color content is.
- **Liquid depth/biome cross-fade only** (not shore blending): a true
  engine-computed interpolation (shader), between two of our full atlases.

---

## 9. Generator recipe

### 9.1 New terrain: `cloudturf`

Ship exactly **one file**: `tiles/cloudturf_splat.png`.

- **Minimum valid size**: 224×96 (1 frame, 1 section). **Recommended**:
  224×192 to 224×384 (2-4 sections) to match vanilla variety norms (§6) —
  pick a multiple of 96 to avoid wasted canvas.
- Do **not** create a plain `tiles/cloudturf.png` — it is only consulted if
  the `_splat` load fails, and if you additionally set
  `preferLegacySplatting = true` it would be tried *first*; leave that flag
  `false` (the default) so `_splat` wins outright.
- **No mask file needed** — new-style blending needs no
  `alphaMaskTextureName` at all (that parameter is only consumed by the
  legacy path).
- For **each** section (repeat the 21-cell layout at `y_offset = section×96`
  for every section you include), populate the 7×3 grid per §5.3:

  | Cells | Content to paint |
  |---|---|
  | (3,0),(4,0),(5,0),(6,0) | 4 distinct fully-opaque "plain cloudturf" tile variants, no transparency. Make (3,0) specifically read well as a standalone 32×32 icon (it becomes the inventory icon). |
  | (1,1) | Fully opaque cloudturf (safe default) — all 4 cardinal sides are cloudturf. |
  | (1,0)/(1,2)/(2,1)/(0,1) | Cloudturf art opaque along the **bottom/top/left/right** edge respectively, fading to transparent by the opposite edge (radial/organic fade, vanilla-style — a hard gradient also works, just looks less organic). |
  | (3,1)/(4,1)/(3,2)/(4,2) | Two-edge combinations (N+W / N+E / S+W / E+S) — opaque in the 3 quadrants adjacent to the named edges, transparent in the 1 opposite quadrant. |
  | (5,1)/(6,1)/(5,2)/(6,2) | Three-edge combinations — opaque along the 3 named edges, fading toward the single opposite edge. |
  | (0,0)/(2,0)/(0,2)/(2,2) | Isolated diagonal corner touches — a rounded blob filling most of the cell with a notch cut from the one **far** corner (SE-notch-at-NW, SW-notch-at-NE, NE-notch-at-SW, NW-notch-at-SE respectively — see §5.3 exact mapping). |

- Register the tile as usual (`new TerrainSplatterTile(false, "cloudturf")`
  subclass, or a `SimpleTerrainTile`-style direct instantiation) and set
  `getTerrainPriority()` relative to neighbors you want it to visually beat
  or lose to (§4) — e.g. match `GrassTile`'s `PRIORITY_TERRAIN` (100) if it's
  meant to behave like a grass-tier ground cover.

### 9.2 New liquid: `mistsea`

Minimal correct version — **one file**: `tiles/mistsea_splat.png`, same
224×96-multiple sizing and identical 21-cell content rules as §9.1 (liquids
use the *exact same* grid, §7.2). No mask file, no legacy fallback exists to
prepare for the liquid case (§7 intro), so this file is not optional if you
want *any* texture.

Java-side (mirrors `LavaTile`'s minimal pattern,
`LavaTile.java:30-31,54-57`):

- Constructor: pass a single name, `super(mistColor, "mistsea")`.
- **No `getTextureIndexes` override needed**: the `LiquidTile` base
  implementation already returns `new TextureIndexes(0, 0, 0, 0)`
  (`LiquidTile.java:120-122`) — all four shallow/deep/fresh/salt slots point
  at index 0, i.e. your one `mistsea_splat.png`, so the shader cross-fade in
  §7.3 blends that atlas with itself (a no-op — indistinguishable from a
  single flat layer). `LavaTile` overrides this method anyway
  (`LavaTile.java:54-57`), but its returned value is functionally identical
  to just inheriting the default — treat that override as optional
  belt-and-braces, not a requirement.
- Two overrides are **mandatory** (both abstract on `LiquidTile`, so the
  class won't compile without them) but need no new art: `getLiquidColor(Level,
  int, int, Biome)` (`LiquidTile.java:162`) — just return your mist tint —
  and `addLiquidTopDrawables(...)` (`LiquidTile.java:417-419`) — implement it
  as an empty no-op body, exactly like `LavaTile.java:130-134` (no
  `watershallow`-style glint asset needed, §7.4).
- No icon art needed — the base `LiquidTile.generateItemTexture()`
  automatically tints the shared `tiles/bucket.png` with your `Color`
  (§7.6).

**Optional upgrade path** (only if shallow/deep should look visually
distinct, à la `WaterTile`): ship additional named atlases
(`tiles/mistsea_deep_splat.png`, etc.) and override `getTextureIndexes` to
point at them per depth/biome, exactly like `WaterTile.getTextureIndexes`
(`WaterTile.java:85-87`) — no code-path changes beyond that are required, the
shader-side cross-fade (§7.3) handles the rest automatically.

### 9.3 What NOT to hand-build

- Don't hand-build a packed multi-tile atlas — `SharedGameTexture` builds the
  real GPU atlas at runtime (§8); ship one PNG per tile/liquid name, as
  vanilla does.
- Don't build any shallow↔deep or fresh↔salt transition art for a liquid —
  that's shader-interpolated (§7.3).
- Don't build a legacy plain-grid file *in addition to* `_splat` "just in
  case" — it will simply never be read while `_splat` loads successfully
  (§2), so it would be dead weight, not a safety net.

---

## Appendix A — Files measured (dimensions)

Full per-file dimension/format table (frames, sections, exact-multiple flag)
for every `tiles/*_splat.png` in the vanilla pack, plus the mask/liquid
helper files, was generated with Python/PIL against
`/home/user/necesse-game/sprites/tiles/`; the condensed version is in §6.
Key non-`_splat` reference files measured directly:

| File | Size | Role |
|---|---|---|
| `splattingmask.png` | 64×64 | Default legacy alpha mask (§3.4) |
| `splattingmaskwide.png` | 64×64 | Alternate wider/softer legacy alpha mask, actively used by 7 vanilla tile classes / 11 tile IDs (§3.3-3.4) |
| `shoremask.png` | 64×64 | Old-style **liquid** shore-edge mask, hard/jagged style (§7.5) |
| `itemmask.png` | 32×32 | Terrain item-icon vignette mask (§2) |
| `bucket.png` | 32×64 | Liquid item-icon base art (§7.6) |
| `watershallow.png` / `waterdeep.png` | 32×192 (6×32px frames) | `WaterTile`-only cosmetic glint overlay, unrelated to splatting (§7.4) |
| `liquidcolors.png` | 6×1 | Shared indexed liquid-color palette lookup (`LiquidTile.getLiquidColor(int)`) |

## Appendix B — 8-neighbor adjacency index (from `Level.adjacentGetters`)

```
index:   0     1     2     3     4     5     6     7
offset: -1,-1  0,-1  1,-1 -1,0  1,0  -1,1  0,1  1,1
name:    NW    N     NE    W    E     SW    S    SE
```
