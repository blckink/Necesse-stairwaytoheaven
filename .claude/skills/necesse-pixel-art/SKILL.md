---
name: necesse-pixel-art
description: Produce or review pixel-art sprites for the Stairway to Heaven Necesse mod so they read 1:1 like vanilla Necesse. Load this BEFORE creating, editing, or reviewing any texture/sprite/sheet, before touching tools/asset_generator/, and before judging in-game screenshots of mod art.
---

# Necesse Pixel Art — how to hit the vanilla style

You are producing sprites for Necesse 1.3.2. The bar is: a player cannot tell mod
sprites from vanilla ones. Everything below was verified against the game's own
assets and decompiled renderers — follow it, don't improvise formats.

## Ground truth, in priority order

1. **A vanilla sprite dump**, if present on this machine (check
   `~/necesse-game/sprites/` or ask). Before drawing a category (tile, wall, mob,
   furniture…), open 2–3 vanilla sprites of the SAME category and match their
   construction, not just their palette.
2. `docs/assets-style-guide.md` — palette ramps + sheet-format cheat sheet.
3. `docs/research/asset-formats.md` and `docs/research/splat-format.md` — exact
   pixel layouts (splat cell map, rock quadrant autotile, wall 352×128, ore strips,
   mob sheets). These are the law for file dimensions.
4. `tools/asset_generator/` — the deterministic Python/Pillow pipeline that renders
   every shipped PNG. Art changes go INTO the generator (or replace a PNG and stop
   regenerating it); never hand-edit a PNG that the generator will overwrite.

## Style DNA (what makes it "look like Necesse")

- **32 px = 1 tile.** Objects grow in 32 px steps, mob frames are 64 px cells.
- **Soft dark outline** `(34,34,46)` around objects/mobs/items — never pure black,
  never around terrain fills.
- **2–4 flat shade steps** per material, light from **top-left**, no gradients.
- **Detail density is the difference between "vanilla" and "placeholder".** Vanilla
  packs 3–6 micro-details into every 32 px cell: speckles, cracks, tufts, a
  2-px highlight dot, a darker crevice line. A flat two-tone fill reads as
  unfinished. When in doubt, add one more readable micro-detail, not one more color.
- **Cute roundness:** corners rounded 1–2 px, blob-like silhouettes, slightly
  oversized heads/eyes on creatures. Necesse is friendly, not gritty.
- **Dithering:** sparse single-pixel checker ONLY at ramp borders, never as texture.
- **Saturation discipline:** dusty bases, few saturated accents. The accent colors
  are what players remember — keep each sub-biome's accent exclusive.
- **Readability at 1×:** every sprite must read at game zoom against both a light
  tile (Cloudturf) and a dark tile (Stormslate). If it only reads at 4×, it fails.

## Hard-won traps (each of these shipped a bug once)

- The generic **outline pass eats 1–2 px diagonals** (sword blades, reeds, strings):
  draw a dark silhouette MASS first, then lay the bright core on top.
- Face/eye details go **after** the outline pass, or they get overwritten.
- Crystal clusters are **2×1 multi-tiles**: even column = base, odd = the `<name>r`
  right half. Both halves must exist.
- Ore overlays are **N×32×32 pattern strips** (engine masks them onto rocks), NOT
  rock-grid sheets.
- Terrain/floors use the **`_splat` atlas** (224 × 96·variants; 4 full variants at
  cells (3..6,0), 17 marching-square blend cells — exact map in
  `docs/research/splat-format.md` §5.3). **Liquids REQUIRE a `_splat`** (8 frames,
  224·8 wide) or they render as flat color.
- Walking-mob sheets: 6 cols (idle, walk×4, swim) × 4 rows in order **Up, Right,
  Down, Left**, 64 px cells.
- Wall sheets are **352×128** (4×8@16px blob + 2×8 window insert + 8 door frames);
  wall lights are **64×128** (on/off cols × 4 attach-orientation rows).
- Every placeable the player can hold also needs an **`items/<id>.png`** icon.
- The world-locked checker floor (`marblechecker`) deliberately has **no** `_splat`.

## Workflow (non-negotiable)

1. Read the relevant format spec + look at 2–3 vanilla references of the category.
2. Implement in `tools/asset_generator/` (deterministic: same seed → same bytes).
3. Regenerate: `python3 tools/asset_generator/generate_assets.py`.
4. **QA gate before shipping, every time:**
   - Render a 4× nearest-neighbor contact sheet of everything you touched, on a
     dark AND a light backdrop, and actually look at it.
   - Checklist: silhouette reads at 1×? no orphan single pixels? diagonals ≥2 px
     wide? light consistently top-left? colors only from `palette.py`? enough
     micro-detail (compare the vanilla reference side by side)? correct sheet
     dimensions for the category?
   - Compose a quick in-context mock: sprite pasted onto Cloudturf and Stormslate
     tiles. That is how players see it.
5. If a sprite fails QA, fix the generator and rerun — never ship "close enough".

## Review vocabulary (for screenshot feedback)

When judging in-game screenshots, name problems precisely: "flat fill, needs
micro-detail", "silhouette mushy at 1×", "outline swallowed the diagonal",
"wrong sheet layout (rendering shows sub-rect misalignment)", "palette drift
(color not in ramp)", "density too sparse (world reads empty)". Each maps to a
concrete generator fix above.

## Size law: measure vanilla FIRST (mandatory since v0.3.4)

Playtests proved we systematically undershoot mass: the warden shipped at a
third of the player's width, the seance circle at 2% of a vanilla ritual
altar's opaque pixels. Vanilla Necesse art is CHUNKY — big heads, thick
props, set pieces that fill their tiles.

Before drawing ANY asset:
1. Find the closest vanilla analogue in the sprite dump and MEASURE its
   opaque-pixel bounding box and pixel count (one representative cell).
2. Target >= 80% of the analogue's opaque mass. When in doubt, go bigger.
3. After drawing, run `python3 tools/size_audit.py` (add a mapping row for
   every new asset class) — a FIX flag blocks shipping.

Measured anchors (opaque bbox, representative cells):
- Player bare head 28px wide; torso 20px; hair pushes heads to 32-36px.
- Settler-type humanoids: figure ~46-52px tall in the 64 cell, hem 22-24px.
- Trees: 128px cells; vanilla deadwood deco fills 62x116 of a 64x128 cell.
- Wall banner objects: 64x96 opaque (two tiles tall).
- Streetlamps: ~24x86 opaque in the 32x96 half.
- Wall torch: ~12x26 in a 32 cell. Mushroom: 24x28 (TALL, not squat).
- Grass clump: ~26x30 and 500+ opaque px (SOLID, not wispy).
- Ritual/set-piece objects (altars): 80x80+ multi-tile presence.
