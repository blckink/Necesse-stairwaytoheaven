# Asset Style Guide

Rules every Stairway to Heaven sprite follows so the mod reads 1:1 like vanilla
Necesse. The generator in `tools/asset_generator/` implements these rules; hand-drawn
replacements should follow them too.

## Pixel language

- **Grid:** 32 px = one tile. Objects grow in 32 px steps; mob frames are 64 px cells.
- **Outline:** soft dark outline `(34, 34, 46)` on object/mob/item silhouettes — never
  pure black, never on terrain tiles (they fill their square).
- **Shading:** 2–4 flat steps per material, light from the **top-left**. No gradients.
- **Dithering:** sparse single-pixel checker only at shade borders, never as texture.
- **Thin shapes** (blades, reeds, strings): draw a dark silhouette mass first, then lay
  the bright core on top. A generic outline pass eats 1–2 px diagonals (learned the
  hard way — see `gen_items._tempest_blade`).
- **Readability first:** every object must read at 1× zoom against both Cloudturf and
  Stormslate.

## Palette (see tools/asset_generator/palette.py)

Muted bases, few saturated accents. "Cool, not kitschy": weathered stone, cold air,
electric light — no rainbows, no gold-trimmed clouds.

| Material | Ramp (dark → light) | Accent |
|---|---|---|
| Cloudturf | 128/143/138 → 222/231/222 | tuft green 146/168/152 |
| Skystone | 84/92/108 → 176/185/199 | — |
| Stormslate | 48/51/66 → 112/118/138 | charge violet 150/140/220 |
| Mistsea | 156/170/186 → 228/236/242 | — |
| Aetherium | 44/116/124 → 198/244/243 | — |
| Storm crystal | 66/54/130 → 206/196/255 | spark 255/250/210 |
| Aurora | 140/62/104 → 255/212/227 | teal 108/196/186 |
| Stairlight | 168/178/200 → 250/252/255 | glow 186/226/230 |

Sub-biome accents stay exclusive: cyan/silver = Driftlands, indigo/violet = Stormveil,
rose/teal = Aurora Shoals. Aetherium cyan is the "reward color" across all biomes.

## Sheet formats (short version — full spec: research/asset-formats.md)

| Asset | File | Layout |
|---|---|---|
| Terrain tile | `tiles/<name>.png` | 32 px wide strip, N rows = variants; engine auto-blends |
| Liquid | `tiles/<name>_shallow/_deep.png` | legacy strips like terrain (flat-color fallback if absent) |
| Stairway pair | `objects/<name>down/up.png` | 32 wide × (32 + upper H); top-left 32×32 = floor part |
| Rock | `objects/<name>.png` | variants×32 wide × 13 rows of 16 px quadrant cells (autotile) |
| Ore overlay | `objects/<ore>.png` | same grid as rock; top-left 32×32 doubles as icon source |
| Crystal cluster | `objects/<name>.png` | variants×64 wide × 48 tall; even col = base, odd = "r" half |
| Grass | `objects/<name>.png` | N×32 variants, 32 tall |
| Item icon | `items/<id>.png` | 32×32, straight alpha |
| Held weapon | `player/weapons/<id>.png` | 32×32, grip toward bottom-left |
| Walking mob | `mobs/<name>.png` | 6 cols (idle, walk×4, swim) × 4 rows (**Up, Right, Down, Left**), 64 px cells |
| Simple flyer | `mobs/<name>.png` | 64 wide; row 0 = body, row 1 = glow |
| Bestiary icon | `mobs/icons/<id>.png` | 32×32 |
| Mod preview | `preview.png` | 268×268 |

## Workflow

```bash
python3 tools/asset_generator/generate_assets.py   # regenerates src/main/resources
```

- Generation is fully deterministic (seeded) — rerunning produces byte-identical PNGs,
  so art changes show up as clean diffs.
- To replace a sprite by hand, just overwrite the PNG (keep the layout above) and stop
  regenerating that file, or better: port your changes into the generator.
- QA habit: upscale sheets 4× with nearest-neighbor and review on a dark backdrop
  before shipping (that pass caught every art bug so far).

## v0.2 additions (format cheat sheet)

| Asset | File | Layout |
|---|---|---|
| Terrain/floor splat | `tiles/<name>_splat.png` | 224×(96·variants); cells (3..6,0) = full variants, 17 marching-square blend cells (see research/splat-format.md §5.3) |
| Liquid splat | `tiles/<name>_splat.png` | 224·8 frames × 96; hard 8-frame loop |
| Wall set | `objects/<name>.png` | 352×128: 4×8@16px autotile blob + 2×8 window insert + 8×(32×128) door frames |
| Fence / gate | `objects/<name>.png` | 160×64 (5 cols) / 192×64 (6 cols) |
| Streetlamp | `objects/<id>.png` | 32×192: two 32×96 rows (on above, off below) |
| Wall light | `objects/<id>.png` | 64×128: 2 cols on/off × 4 attach-orientation rows |
| Statue | `objects/statues/<name>.png` | frameWidth × spriteCount columns (gloomraven: 64×96, 1 pose) |
| Painting/banner | `objects/<texturePath>.png` | 32×128: 4 rotation rows of 32×32 |
| Legacy checker floor | `tiles/<name>.png` | 64×64 world-locked 2×2 grid — deliberately NO `_splat` |
