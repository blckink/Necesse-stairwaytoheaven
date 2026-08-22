# Necesse Mod Texture Asset Format Specification

Source: decompiled game classes at `/home/user/necesse-game/decompiled/necesse/`
(game v1.3.2), read directly and traced method-by-method. This is **ground
truth from the actual game code**, not documentation or inference — every
claim below cites the exact class and method it was read from. No method
bodies are reproduced here (only short constant/path strings and derived
numeric facts); read the cited files directly for the real logic.

This document complements `docs/research/modding-api.md` (which was built
from the community wiki + the official `ExampleMod` repo). Where the two
overlap they agree; where `modding-api.md` flagged something as
**UNVERIFIED** or "needs decompiled-source verification" (notably the mob
sprite-sheet row/column mapping), this document resolves it from the
actual `Mob` class logic and says so explicitly.

---

## 0. Foundations that everything else builds on

**World grid unit is 32×32 px.** This is pervasive, not a single constant:
`GameTile`/`TerrainSplatterTile`'s splatting math, `GameObject`'s default
`hoverHitbox = new Rectangle(32, 32)`, and essentially every draw call
below all key off 32px cells.

### How `GameTexture.fromFile(...)` resolves a path

Traced in `necesse.gfx.gameTexture.GameTexture`, private constructor
`GameTexture(String file, boolean outsideGame, boolean updateLoadingScreen)`,
and the static entry points `fromFile` / `fromFileRaw` /
`fromFileRawUnknown`:

1. **Extension**: `GameUtils.formatFileExtension(path, "png")` appends
   `.png` **only if the given filename has no `.` in it at all**. So
   `"objects/foo"` → `objects/foo.png`, but a path you already wrote with
   an extension is left untouched. In practice every call site in the
   engine passes the bare name (no extension), so mods should do the same.
2. **Lookup order**: (a) a loose file at `<game root>/res/<path>` on disk
   (a dev/override folder for the base game — **not** a per-mod folder),
   checked first; else (b) `ResourceEncoder.getResourceBytes(path)`, which
   is the merged in-memory table of the base game's `res.data` plus every
   loaded mod's packaged resources (see §6); else (c) it retries the loose
   file path and lets `FileNotFoundException` propagate if truly absent.
3. **Strict vs. lenient**: `fromFileRaw(path)` throws `FileNotFoundException`
   on failure — engine code commonly wraps this in `try/catch` purely to
   detect "does this *optional* file exist" and branch. Plain `fromFile(path)`
   instead substitutes the shared `GameResources.error` placeholder texture
   and logs a warning (only outside dev mode) — a missing **required**
   texture doesn't crash the game, it just renders as the error texture.
4. Decoded textures are cached process-wide by their resolved path
   (`GameTexture.loadedTextures`), so requesting the same path twice from
   different classes decodes the PNG only once.

### Alpha / blend conventions (answers Q8)

- Sprite draws consistently reset to
  `GL14.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)`
  — see `necesse.gfx.drawOptions.texture.TextureDrawOptionsObj`,
  `necesse.gfx.gameTexture.GameFrameBuffer`, `necesse.gfx.PlayerSprite`.
  The RGB half (`SRC_ALPHA, ONE_MINUS_SRC_ALPHA`) is the textbook
  **straight (non-premultiplied) alpha** blend formula. **PNGs should be
  authored as normal straight-alpha images** — no premultiplication step
  needed, any standard image editor's PNG export works. The alpha-only
  half (`ONE, ONE_MINUS_SRC_ALPHA`) only affects how alpha accumulates
  when rendering into an intermediate framebuffer; it isn't an
  asset-authoring concern.
- Textures default to `GameTexture.BlendQuality.LINEAR` (bilinear
  filtering) unless a call site explicitly requests `NEAREST`
  (`GameTexture.setBlendQuality`). This means a fully-transparent pixel's
  *hidden* RGB value can bleed into visible edges under scaling/rotation.
  The engine has a dedicated fix, `GameTexture.runPreAntialias()` /
  `getSurroundingInvisColor()` (recolors every alpha-0 pixel to the
  average color of its opaque neighbors), but it is **not** run
  automatically on mod resources — it's only invoked by the
  character-customization layers (`GameEyes`, `GameHair`, `GameSkin`) and
  by a standalone offline tool, the top-level `PreAntialiasTextures` class
  (a `main()`-based batch script over a folder of PNGs). `modding-api.md`
  independently found this exposed to modders as the **`preAntialiasTextures`
  Gradle task** in the mod SDK — that task is exactly this class; it is
  opt-in and must be run deliberately before shipping. **Recommendation:**
  keep transparent-pixel RGB close to the adjacent opaque color yourself
  (most pixel-art editors already do this), or run that Gradle task.

---

## 1. Terrain tiles (`TerrainSplatterTile`)

Class: `necesse.level.gameTile.TerrainSplatterTile extends GameTile`.
Constructed with a `terrainTextureName` and (optionally) a custom
`alphaMaskTextureName` (default `"splattingmask"`). Loading happens in
`loadTextures()` → `generateSplattingTextures()`.

The engine supports **two independent formats**, auto-selected by which
files exist (or forced via the public `preferLegacySplatting` boolean
field). A modder only needs to pick one.

### Format A — "legacy": plain strip + auto-generated blending (recommended for mods)

File: **`tiles/<name>.png`** only (no `_splat` file present, or
`preferLegacySplatting = true`).

- Layout: a plain grid of 32×32 "variant" cells — **width = 32 × columns,
  height = 32 × rows**. The simplest valid file is a single 32×32 tile
  (1×1). `getTerrainSprite(...)` (base class always returns cell `(0,0)`;
  real tiles override it, typically to pick a variant column/row at
  random per tile position) selects which cell is a given tile's "plain"
  look.
- Blending is **entirely auto-generated** by `generateOldTerrainSplatting`:
  the engine also loads a shared alpha mask, `tiles/splattingmask.png`
  (a built-in vanilla asset — you only need your own copy if you set a
  custom `alphaMaskTextureName`), which must be **square** (width ==
  height, or `TerrainSplatterTile` throws `IllegalStateException`) and is
  itself a 32px-cell grid. For every one of your variant cells, the
  engine tiles that single variant across a copy of the mask's grid and
  multiplies it against the mask's alpha (`MergeFunction.MULTIPLY`),
  producing every directional edge/corner blend combination
  automatically. **You draw only the flat variant strip; you never draw
  any blended/edge art yourself.**

### Format B — "new": hand-authored auto-tile atlas

File: **`tiles/<name>_splat.png`** (tried first unless
`preferLegacySplatting = true`).

- Layout: stacked **224×96 px sections** (each section is a 7-column ×
  3-row grid of 32×32 cells). **Width must be a multiple of 224** —
  `width / 224` is the animation-frame count (only meaningful if > 1;
  frames are played as a time-based ping-pong, see
  `TerrainSplatterTile.getSplattingTexture`). **Height must be a multiple
  of 96** — `height / 96` is the number of random visual variants,
  selected deterministically per-tile from a seeded hash of the tile's
  coordinates (`GameTile.getTileSeed`).
- Within one 224×96 section, all 21 cells hold a fixed marching-squares
  style set of corner/edge/full-tile pieces whose *positions* are baked
  into engine logic, not derived from a mask: four cells — grid columns
  3, 4, 5, 6 of row 0 (`TerrainSplatterTile.NEW_FULL_TILE_SPRITES`) — are
  the "fully surrounded, nothing to blend" plain-tile variants; the other
  17 cells are corner/edge blend pieces whose selection logic lives in
  `necesse.level.maps.splattingManager.SplattingOptions` (see
  `generateSplattingTopLeft/TopRight/BotRight/BotLeft` and
  `addNewMarchSplattingOptions`/`newTerrainSprites`).
- **Ambiguity flagged**: the exact pixel *content* expected in each of
  those 17 blend cells is authored art baked into the vanilla PNGs, not
  something derivable from Java logic alone. In practice this format is
  only practical by copying/repainting an existing vanilla `_splat.png`
  as a template, not by inventing the grid from scratch.

### Item icon for a terrain tile

`TerrainSplatterTile.generateItemTexture()` (override of
`GameTile.generateItemTexture()`): crops a 32×32 icon from cell `(3,0)`
of the `_splat` texture if it exists, else from cell `(0,0)` of the plain
`tiles/<name>.png`, then multiplies it against the shared
`tiles/itemmask.png`. **No `items/<tileid>.png` file is ever read for a
tile** — see §7 for the general rule.

---

## 2. Liquid tiles (`LiquidTile`, `WaterTile`)

Class: `necesse.level.gameTile.LiquidTile extends GameTile` (abstract),
constructed with a base `liquidColor` and a `String... textureNames`
varargs array (stored as `splatTextureNames`).

- `LiquidTile.loadTextures()`: unless `preferLegacySplatting = true`, it
  tries to **strictly** load `tiles/<name>_splat.png` for **each** name in
  that array — same 224×96-per-section format as terrain Format B above
  (checked via `getWidth()/224` and `getHeight()/96` in
  `getNewSplattingFrame`/`getNewSplattingTexture`). Any name whose file is
  missing is silently skipped (caught `FileNotFoundException`), leaving
  that slot unset.
- **If none of the `_splat` files exist at all** (or
  `preferLegacySplatting = true`, which skips the whole loading loop),
  `isUsingNewTerrainSplatting(...)` is false and `LiquidTile.addDrawables`
  falls back to the simplest possible rendering: a flat 32×32 quad tinted
  by `getLiquidColor(...)`. **A basic custom liquid needs zero texture
  files** — just a `Color` and the abstract `getLiquidColor(...)`
  override.
- `WaterTile` (concrete vanilla example) passes **8** names — fresh/salt
  × shallow/deep, doubled again for a swamp variant — and overrides
  `getTextureIndexes` to pick which of the 8 applies per biome. A simple
  custom liquid would typically pass just one name and either accept the
  `LiquidTile` default (`new TextureIndexes(0, 0, 0, 0)` — one texture set
  used everywhere) or override `getTextureIndexes` similarly.
- `WaterTile.loadTextures()` additionally loads two small **plain**
  (non-`_splat`) helper textures, `tiles/waterdeep.png` /
  `tiles/watershallow.png` — a strip of 32×32 rows
  (`bobbingTexture.getHeight()/32`) used only for an occasional floating
  surface-sparkle overlay (`WaterTile.addLiquidTopDrawables`). This is a
  `WaterTile`-specific extra, not a general `LiquidTile` requirement.
- A shared 1-row, N-columns lookup strip `tiles/liquidcolors.png` is
  loaded once (first liquid tile to load it) for `getLiquidColor(int
  index)`; irrelevant unless you want to reuse an existing palette entry.

---

## 3. Objects (`GameObject` and subclasses)

`necesse.level.gameObject.GameObject.loadTextures()` is **empty** in the
base class — nothing is auto-loaded. Every concrete subclass wires up its
own texture path(s); `"objects/<name>"` is a near-universal convention
across vanilla subclasses, but it is a habit, not an enforced base-class
default (contrast with `Item`, §4).

Three concrete anchor/layout patterns recur in vanilla code, all loading
via `GameTexture.fromFile("objects/" + <a name field>)`:

| Pattern | Example class | Width formula | Height / anchor |
|---|---|---|---|
| Flat ground clutter (no Y-sort) | `TileClutterObject` | `32 × variantCount` (one column per random variant, picked via `GameRandom.seeded(getTileSeed(...))`) | own natural height, drawn **top-left anchored** at the tile's exact position (`pos(drawX, drawY)`) — a value > 32 bleeds downward past the tile with no special handling; vanilla clutter keeps this at 32 |
| Bottom-anchored "stands on the tile" | `TorchObject`, `SingleRockObject` | `32 × states` (Torch: one column per placement rotation) or `64 × variants` (SingleRockObject: reserves a 64px slot per variant, only the left 32px column of each slot is drawn — `sprite(variant*2, 0, 32, height)`) | any height ≥ 32; drawn **bottom-anchored**: `pos(drawX, drawY - texture.getHeight() + 32)`, so the bottom 32px row sits on the tile and any extra height rises upward on screen. Independently confirmed by `modding-api.md`'s measurement of the real `ExampleMod`'s `objects/exampleobject.png` at **32×64**, anchored with the identical `drawY - texture.getHeight() + 32` formula. |
| Adjacency-aware autotile (rock walls/veins) | `RockObject` | bespoke 16px sub-tile pieces per adjacency case | not a template for typical mods — skip unless replicating vanilla rock-wall behavior |

**Switchable-state suffix convention**: `TorchObject.loadTextures()`
loads `objects/<name>.png` (on/lit, non-strict) and
`objects/<name>_off.png` (off/unlit, non-strict) as same-size companions,
plus optional strict `objects/<name>_decor.png` /
`objects/<name>_decor_off.png` (used only when the torch is mounted on a
wall-holder decor layer; silently falls back to the base pair if absent).
The `_off` suffix is the general vanilla convention for a second visual
state of a switchable object.

### Ladders (`LadderDownObject` / `LadderUpObject`)

Loaded as **`objects/<textureName>down.png`** and
**`objects/<textureName>up.png`** — plain string concatenation, no
separator before "down"/"up".

`LadderDownObject.addDrawables` draws **two pieces from the one texture**:

1. `sprite(0, 0, 32)` — the literal **top-left 32×32 block** of the PNG —
   drawn flat at the tile's own position in the non-sorted tile-decal
   layer (the walkable "hole" floor graphic).
2. `section(0, width, 32, height)` — everything from pixel row 32 down to
   the image bottom, full width — drawn in the Y-sorted layer,
   **bottom-anchored** so its bottom edge sits exactly at the tile's
   bottom edge (`drawY = tileDrawY - (height - 32) + 32`), i.e. it rises
   `height - 32` px above the tile.

Both pieces share one horizontal placement,
`drawX = tileDrawX - width/2 + 16` (the whole image is horizontally
centered on the tile). Because the floor block is always sampled from
the image's own left edge (columns 0–31), a width-32 image needs no
thought; a **wider** image only lines its floor-hole graphic up with the
tile if that graphic is deliberately placed in the image's leftmost 32
columns (the centering offset is applied uniformly to both pieces).

**Net formula: `height = 32 (floor row) + H_upper`, `width = 32` for the
simple case** (wider only if the structure needs to visually extend
sideways, with the caveat above).

`LadderUpObject.addDrawables` draws **only** the `section(0, width, 32,
height)` upper part (same bottom-anchor formula) — it does not draw its
own row 0 directly. However, `LadderUpObject.getNewObjectEntity` still
passes `new GameSprite(texture, 0, 0, 32)` into `LadderUpObjectEntity` as
its `mapSprite` field — row 0 of the "up" texture is used as the small
world-map icon for that ladder-up location, not as an in-world floor
decal. **Conclusion: both "down" and "up" textures should follow the
same row-0-is-a-self-contained-32×32-icon convention**, even though each
variant only actively draws part of it in the level itself.

### Object item icon

`GameObject.generateItemTexture()` default: `GameTexture.fromFile("items/"
+ getStringID())` — see §7 for when this default is or isn't used.

---

## 4. Items (`Item`)

`necesse.inventory.item.Item`:

- **Default icon**: `loadItemTextures()` → `itemTexture =
  GameTexture.fromFile("items/" + getStringID())`. Non-strict — a missing
  icon silently becomes the shared error-texture placeholder.
- **Two further optional textures**, same stringID-based naming, both
  strictly loaded and wrapped in `try/catch` (silently `null` if absent):
  - `player/holditems/<stringid>.png` — alternate art for the item shown
    held in a character's hand.
  - `player/weapons/<stringid>.png` — melee/attack swing art. Independently
    measured by `modding-api.md` at **36×36** (sword) and **40×40**
    (staff) in the real example mod — confirms this sprite's size is
    **per-weapon, not fixed to 32×32**.
- **Sizing**: `Item.getItemSprite()` wraps the *whole* `itemTexture` as one
  `GameSprite` — it is not read as a multi-cell grid, and the UI always
  scales it to whatever destination size is requested
  (`Item.drawIcon(...).size(size)`; the in-world dropped-pickup size
  defaults to `Item.worldDrawSize = 24`, itself just a `.size(...)` scale
  of the source icon). **The loader does not enforce a resolution** — but
  every vanilla icon is 32×32, matching the tile/object grid, so that is
  the practical standard to target for visual consistency.
- **Outline/padding**: no automatic rarity-based outline or glow is
  applied to the icon *bitmap*. `Item.Rarity` carries a `color` and an
  `outlineMinHue`/`outlineMaxHue` pair, but the only usage found
  (`necesse.level.maps.hudManager.floatText.ItemPickupText`) applies that
  hue range to the floating pickup-notification **text** color, not to
  the icon image. **A plain flat 32×32 icon with normal straight alpha is
  sufficient — no manual outline or extra padding border is expected.**

---

## 5. Mobs

### Where mob world-textures live and how they're grouped

- `necesse.engine.registries.MobRegistry.MobRegistryElement.loadIcon()`:
  every registered mob additionally gets a separate bestiary/kill-notice
  icon from **`mobs/icons/<stringid>.png`** — distinct from its in-world
  sprite sheet.
- `MobRegistry.Textures` is a static holder for vanilla mobs' in-world
  textures, populated by its own `load()` method whose helper
  `fromFile(path)` always prefixes with `"mobs/"`. So a mob's world
  texture path convention is **`mobs/<name>.png`** (the name is whatever
  the mob class chooses — not necessarily equal to its registered
  stringID). Three recurring texture-grouping types:
  - a plain `GameTexture` — one file, whatever grid the mob's draw code
    expects;
  - `necesse.entity.mobs.MobTexture` — a `body` + `shadow` pair
    (`Textures.fromFiles(name)` loads `mobs/<name>.png` and
    `mobs/<name>_shadow.png`);
  - `HumanTexture` / `HumanTextureFull` — a rigged humanoid look: body
    texture plus separate `_left`/`_right` arm textures (and, for the
    "full" variant, separate head/hair/eyelids/body/arms/feet layers each
    with optional `_back` counterparts). This is the limb-rigged
    `HumanDrawOptions` path used for human-shaped mobs (zombies, cavelings,
    NPCs) — materially different from, and out of scope of, the flat
    directional sprite sheet described below.

### The base directional animation convention (`Mob.getAnimSprite`)

This is the convention any mob inherits **unless it overrides**
`getAnimSprite`/`addDrawables` itself — traced in
`necesse.entity.mobs.Mob`: `getAnimSprite(x, y, dir)` returns a `Point`
used directly as `(spriteX, spriteY)` grid-cell coordinates passed to
`texture.initDraw().sprite(x, y, cellSize)`.

- **Row (Y) = facing direction.** The direction-to-row mapping is fixed
  engine-wide, confirmed via `Mob.getDirVector()` and every `setDir(...)`
  call site (`dir` is network-synced as a 2-bit value, max 3 —
  `reader.getNextMaxValue(3)` — so there are exactly 4 directions):

  | dir value | row | direction |
  |---|---|---|
  | 0 | 0 | **Up** |
  | 1 | 1 | **Right** |
  | 2 | 2 | **Down** |
  | 3 | 3 | **Left** |

  This resolves the exact ordering `modding-api.md` flagged as needing
  decompiled verification. Note it is **Up, Right, Down, Left** — not the
  U/D/L/R guess in the task brief. (`Mob.getSpriteOffset` independently
  corroborates this: it applies a special vertical draw offset when
  `spriteY == 1 || spriteY == 3`, i.e. exactly the two "sideways" rows.)

- **Column (X) = animation frame**:
  - column 0 = idle/standing.
  - columns 1–4 = a 4-frame walk cycle, chosen by **total distance
    traveled** (not elapsed time) divided by the mob's own
    `getRockSpeed()` "step length", mod 4 — so cycle speed automatically
    tracks movement speed.
  - column 5 = a dedicated swimming/in-liquid pose, used whenever
    `Mob.inLiquid(x, y)` is true regardless of movement. A mob that never
    swims should still reserve this column (or override the relevant
    checks) since the base class indexes into it unconditionally.
- **Cell size ("res")** is *not* fixed by the base class — it's whatever
  the mob's own `addDrawables` passes to `.sprite(x, y, res)`. Traced
  examples: `RabbitMob` and `CrystalArmadillo` both use **64px** cells
  (headroom above the 32px tile footprint); `FlyingBugCritterMob`
  defaults to 64px but its `SmallFlyingBugCritterMob` subclass (parent of
  `FireflyMob`/`ButterflyMob`) sets it to **32px**. `modding-api.md`'s
  independently measured `ExampleMob` sheet also uses 64px cells.
- **Shadows** are separate and simpler: the default
  `Mob.getShadowDrawOptions` draws `sprite(dir, 0, res)` from a **shared**
  texture (`MobRegistry.Textures.human_shadow` by default; other mob
  families point at `small_shadow`, `bird_shadow`, etc.) — i.e. a plain
  **4-column, 1-row** strip, one cell per direction, `res` = that shared
  texture's own height. Most simple mods can just reuse an existing
  shared shadow strip instead of drawing a custom one.

**Recommended baseline sheet for a 4-direction walking mob** (no extra
states): **width = 6 × cellSize, height = 4 × cellSize** — columns
0–5 = idle / walk×4 / swim, rows 0–3 = Up / Right / Down / Left.
`CrystalArmadillo` demonstrates this is extensible: it adds two more
columns (6–7) for a 2-frame "rolled into a ball" alternate animation,
still indexed by the same direction rows, while still calling the base
`getAnimSprite` for its normal pose. This 6-vs-8-column difference is
also a plausible explanation for why `modding-api.md`'s example mob sheet
measured **6 columns × 5 rows** rather than 6×4: the base `Mob` class
only ever needs 4 direction rows, so a 5th row is either an unused spare
row or something that specific example mob's own subclass logic adds —
it is not required or explained by the base class.

A same-layout **second texture** is the vanilla convention for an
emissive/glow layer, not extra rows in one file — e.g. `crystalArmadillo`
+ `crystalArmadillo_light` (identical grid, drawn as a second pass at a
reduced light level via `light.minLevelCopy(...)`).

### Recommended layouts for a floating/flying mob

Three concrete, increasingly capable patterns, all traced:

1. **Fully static** (`WillOWispMob`): a single plain image, **no grid at
   all** — no `.sprite()` call in its `addDrawables`. All "glow" comes
   from a dynamic light source (`givesLight(...)`) and a time-based
   brightness-pulse shader (`startGlowOptions`), not from extra art. This
   is the simplest possible flying-mob asset: **just one PNG.**
2. **Animated, directional wing-flap** (`FlyingBugCritterMob`, parent of
   `ButterflyMob`/`FireflyMob`): `getWingFlapSpriteAnim()` returns
   `Point(frame, dir)` — `frame` ping-pongs between columns **0 and 1**
   (a 2-frame flap cycle), `dir` is the same 0–3 Up/Right/Down/Left row
   convention as walking mobs. Cell size = the class's own `spriteRes`
   field (64 by default, 32 for `SmallFlyingBugCritterMob`). **Baseline
   sheet: width = 2 × cellSize, height = 4 × cellSize.** The shadow is a
   separate shared texture (`MobRegistry.Textures.bird_shadow`), not part
   of this sheet.
3. **Optional glow-dot overlay** (`FireflyMob` specifically): reads
   columns **2–3** (same rows) of its *own* body texture — i.e.
   `sprite.x + 2` — as an isolated "light only" cutout of the same two
   flap frames, drawn as a second tinted/alpha pass at the light-emitting
   tip. So the optional convention for a glow accent is **doubling the
   column count** (0–1 = normal frames, 2–3 = a glow-only cutout of the
   same two frames), not adding rows.

### Attack poses (`AttackAnimMob`)

`necesse.entity.mobs.AttackAnimMob` (parent of many hostile mobs,
including `HostileMob`/`CaveMoleMob`) is purely a **timing/state** helper
— it tracks `isAttacking`, `attackTime`, `attackDir` and computes
`getAttackAnimProgress()` (0–1 swing progress). It defines **no texture
layout of its own**. Concrete mobs use that progress value to drive
whatever their own art needs — e.g. `CaveMoleMob.addDrawables` uses a
separate `ItemAttackDrawOptions.armSprite(...)` helper with its own
sub-sprite coordinates from the mole's body texture. Deriving a general
"attack pose" sheet convention would require tracing
`ItemAttackDrawOptions`/`HumanDrawOptions` in full, which was out of
scope here — **treat attack-pose art as per-mob custom, not a fixed
format.**

---

## 6. Mod resource paths (confirms the `resources/` jar root)

Traced in `necesse.gfx.res.ResourceEncoder` and
`necesse.engine.modLoader.LoadedMod`/`GameFileEntry`:

- `ResourceEncoder.jarResourcePath = "resources/"` and
  `ResourceEncoder.addModResources(LoadedMod mod)` iterates every entry
  in the mod's jar file and hands them to `ResourceFolder.addModResources`.
- `ResourceFolder.addModResources`: only jar entries whose path **starts
  with `resources/`** are considered at all (excluding
  `resources/preview.png`, which is the mod's store thumbnail, handled
  separately). The `resources/` prefix is then **stripped**, and the
  remainder becomes the lookup key — e.g. jar entry
  `resources/objects/foo.png` is registered under the key
  `objects/foo.png`, which is exactly what
  `GameTexture.fromFile("objects/foo")` looks up.
- Only files whose extension is one of **`png`, `ogg`, `glsl`, `ttf`**
  (`ResourceEncoder.fileExtensions`) are imported from that tree — other
  files under `resources/` are ignored entirely.
- **Confirmed root folder inside the jar: a top-level `resources/`
  directory, mirroring the same relative sub-path the code requests.**
  This exactly matches the file tree `modding-api.md` independently
  confirmed against the real `DrFair/ExampleMod` GitHub repo
  (`resources/items/`, `resources/tiles/`, `resources/objects/`,
  `resources/mobs/`, etc., sibling to `resources/locale/` and
  `resources/preview.png`).
- **Override behavior**: `ResourceFolder.addModResources` always
  overwrites an existing entry at the same key (tracking and logging
  which paths were overridden) — a later-loaded mod (or the same mod
  re-adding a path) can deliberately replace a vanilla or another mod's
  texture at an identical relative path; this is how "texture pack"-style
  reskins work.

---

## 7. Tile item textures — auto-generated or `items/<id>.png`?

Traced in `necesse.inventory.item.placeableItem.tileItem.TileItem` and
`necesse.inventory.item.placeableItem.objectItem.ObjectItem`:

- **`TileItem.loadItemTextures()`** unconditionally overrides the base
  and calls `this.getTile().generateItemTexture()`. **`items/<tileid>.png`
  is never consulted for a tile's own item icon** — the icon is always
  programmatically cropped/masked from the tile's own world texture (see
  §1, and `LiquidTile.generateItemTexture()` which instead composites a
  colored overlay onto `tiles/bucket.png`). A custom `GameTile` subclass
  *could* override `generateItemTexture()` itself to load an `items/...`
  file manually — nothing stops it — but no vanilla base class does this
  automatically.
- **`ObjectItem.loadItemTextures()`** likewise always calls
  `this.getObject().generateItemTexture()`. Unlike tiles, though, the
  **base** `GameObject.generateItemTexture()` default genuinely *is*
  `GameTexture.fromFile("items/" + getStringID())` — so a plain custom
  object with no override **does** get its icon from
  `items/<objectstringid>.png` by default, same as a generic `Item`. Only
  object subclasses that explicitly override `generateItemTexture()`
  (rocks via their shared rock-texture name, ladders, liquids-as-objects,
  etc.) bypass that file in favor of deriving the icon from world art.

**Practical rule of thumb: tiles never need (or use) an `items/` icon
file; objects do, unless the object class you're extending overrides
`generateItemTexture()` itself (check the specific base class you
extend).**

---

## 8. The `_padding` trick (`AscendedVoidTile.loadTexture`) — scope and relevance

Traced in `necesse.level.gameTile.AscendedVoidTile`. This is a **bespoke
helper local to one vanilla tile class**, used only for its four
auxiliary "parallax" overlay layers (`swirls`/`grime`/`stars`/`fog`
loaded from `tiles/ascendedvoid_swirls.png` etc.), not a general
asset-format requirement:

- `AscendedVoidTile.loadTexture(path)` loads the plain PNG, then builds a
  **new texture 2px larger in each dimension** entirely in Java at
  runtime (not something the artist does in the source PNG), copies the
  original into the 1px-inset interior, and extrudes each edge row/column
  outward by 1px (top, bottom, left, right — not the corners).
- The padded texture is what's stored in the shared tile atlas
  (`tileTextures.addTexture(...)`); everywhere it's actually sampled
  (`AscendedVoidTile.addParallaxDrawOptions`), the code trims exactly 1px
  back off (`GameTextureSection.section(1, w-1, 1, h-1)`) to recover the
  original content. The 1px border exists purely so the parallax shader's
  UV-scrolling/bilinear sampling can never read a pixel belonging to a
  neighboring, unrelated tile packed into the same shared atlas.
- **This is only relevant if you're building a custom shader-driven,
  scrolling/parallax overlay layer sampled from a shared atlas with
  wrapping UVs.** Ordinary mod terrain/object/item/mob PNGs are not run
  through this and need no manual padding.

---

## Cheat sheet

| Content type | File path (relative to mod `resources/`) | PNG size formula | Notes |
|---|---|---|---|
| Terrain tile, legacy (recommended) | `tiles/<name>.png` | `32×cols` wide × `32×rows` tall (1×1 minimum) | Blending auto-generated from `tiles/splattingmask.png`; you supply only flat variant art. See §1A. |
| Terrain tile, new/hand-authored | `tiles/<name>_splat.png` | `224×frames` wide × `96×variants` tall | Must match vanilla auto-tile cell template; copy an existing `_splat.png`. See §1B. |
| Terrain tile splatting-mask override | `tiles/<custom alphaMaskTextureName>.png` | square, multiple of 32 | Only needed if you pass a custom mask name; default `splattingmask` is built in. |
| Liquid tile splat texture(s) | `tiles/<name>_splat.png` (one per name passed to `LiquidTile(...)`) | `224×frames` wide × `96×variants` tall | Optional — omit entirely for a flat color-tinted liquid. See §2. |
| Object, generic | `objects/<name>.png` | `32×32` (simple) up to `32×N` wide (variants) × any height | Top-anchored if in tile-decal layer, bottom-anchored (`h-32` rises up) if in sorted layer. See §3. |
| Object, switchable state | `objects/<name>.png` + `objects/<name>_off.png` (+ optional `_decor`/`_decor_off`) | same size as base | `_off` suffix = secondary visual state. |
| Ladder down | `objects/<textureName>down.png` | width ≥ 32 (32 typical); height = `32 + H_upper` | Row 0 (top 32px) = floor/hole icon; rest = upraised structure, bottom-anchored. See §3. |
| Ladder up | `objects/<textureName>up.png` | same shape as its "down" pair | Row 0 = world-map icon only (not drawn in-level); rest = structure. |
| Item icon (default) | `items/<stringid>.png` | `32×32` recommended (any size technically loads; UI always rescales) | Straight alpha; no outline/padding needed. See §4. |
| Item held-in-hand art (optional) | `player/holditems/<stringid>.png` | design-dependent | Optional; falls back silently if absent. |
| Item attack/weapon art (optional) | `player/weapons/<stringid>.png` | design-dependent (36×36 / 40×40 seen in vanilla example) | Optional; falls back silently if absent. |
| Tile-as-item icon | *(none — auto-generated)* | — | `TileItem` always calls `tile.generateItemTexture()`; `items/<tileid>.png` is never read. See §7. |
| Object-as-item icon | `items/<objectstringid>.png` (default) or none (if overridden) | `32×32` recommended | Only used when the `GameObject` subclass doesn't override `generateItemTexture()`. See §7. |
| Mob bestiary/kill icon | `mobs/icons/<stringid>.png` | `32×32` recommended | Separate from the in-world sheet. |
| Mob world sheet, walking (4-dir) | `mobs/<name>.png` | `6×cellSize` wide × `4×cellSize` tall (cellSize commonly 32 or 64) | Cols 0/1-4/5 = idle/walk×4/swim; rows 0-3 = Up/Right/Down/Left. See §5. |
| Mob shadow (optional custom) | `mobs/<name>_shadow.png` | `4×res` wide × `res` tall | One column per direction, single row; or just reuse a shared vanilla shadow. |
| Mob, static floating (simplest) | `mobs/<name>.png` | any single-image size, no grid | No `.sprite()` call at all; glow via light/shader, not art. |
| Mob, animated flying critter | `mobs/<name>.png` | `2×cellSize` wide × `4×cellSize` tall (optionally `4×cellSize` wide with cols 2-3 = glow cutout) | Cols = 2-frame wing flap (or 4 with glow); rows = Up/Right/Down/Left. See §5. |
| Mob glow/emissive layer | `mobs/<name>_light.png` (naming is per-mob, not enforced) | identical grid to the base sheet | Drawn as a second pass at reduced light, not extra rows. |

**Key ambiguities to flag to the team**: (1) the exact pixel content of
the 17 non-"full-tile" cells in a terrain `_splat.png`'s auto-tile grid
is authored art, not logic — reverse-engineer from an existing vanilla
`_splat.png` rather than from this document; (2) the shared
`tiles/splattingmask.png`'s exact internal grid/shape was not
inspectable from Java source (it's a binary asset) — its width/height
are only constrained to be equal and a multiple of 32; (3) `HumanDrawOptions`/
limb-rigged humanoid mobs and `ItemAttackDrawOptions` attack-pose sheets
were intentionally not traced in full (out of scope) — treat those as
needing separate research if a humanoid or attack-animated mod mob is
needed.
