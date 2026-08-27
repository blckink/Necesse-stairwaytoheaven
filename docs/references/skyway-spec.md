# Skyway Passages: white-and-gold cloud set

Design taken from `skyway-floor-reference.png` and
`cloudmarble-wall-reference.png`. Palette values below were sampled out of the
references, then quantized to a 4-step ramp the way the rest of the mod's
materials are built.

## Palette

```python
CLOUDMARBLE = {          # the white cloud-stone body
    "deep":  (186, 206, 224),
    "base":  (214, 228, 236),
    "light": (233, 240, 243),
    "hi":    (247, 249, 250),
}
CLOUDMORTAR = (195, 219, 234)   # pale blue between the stones
CLOUDGLYPH  = (176, 208, 228)   # the soft blue swirl drawn into each stone
SKYGOLD = {              # the rims, cornices, arches and four-point stars
    "deep":  (166, 140,  96),
    "base":  (200, 176, 128),
    "light": (216, 196, 150),
    "hi":    (236, 222, 186),
}
```

Sampled evidence: the reference's dominant non-black colours quantize to
(195,219,234) → (206,224,235) → (220,233,238) → (242..247 near-white); the gold
accent clusters at (200,176,128)–(208,184,136); the swirl lines at
(176..184, 204..212, 228).

## Motifs (what makes it read as *this* set and not generic white stone)

- Rounded cloud cobbles with a **1 px gold rim on the lit (top-left) edge only**
  — not a full outline. The rim is what carries the "gold" identity at 1×.
- A soft blue **swirl glyph** curled inside the larger stones, roughly one per
  2–3 stones, never on the small ones.
- A **four-point star** (tall diamond, thin) as the set's punctuation: centred
  on door leaves, sparse on the floor, and on the wall's cornice.
- On the wall: a **golden arcade** of arches along the front face, and a thin
  gold cornice line along the top cap.

## What each asset must actually be

The references are laid out in the *spirit* of the vanilla sheets but at
render resolution. The real formats are non-negotiable:

- **Floor**: 32 px terrain tile + `<id>_splat.png` at 224×(96·variants). See
  `docs/research/splat-format.md` §5.3 for the cell map, and
  `tools/tile_behaviour_audit.py` for the diagonal-corner coverage bands — a
  terrain tile whose diagonal cells are too full repaints its neighbours.
- **Wall**: one 352×128 sheet, and it has **three** readers — wall body
  (16 px grid, cols 0–3), window insert (cols 4–5), and eight 32×128 door
  cells. See `docs/research/asset-formats.md`, and note the perspective rule
  recorded in `docs/TECHNICAL_LEARNINGS.md`: window rows 0–1 are the wall's
  **roof seen from above** (opaque), rows 2–4 stay empty, rows 5–7 are the
  **front** with the see-through opening. The reference's golden arcade belongs
  on the front; its cornice belongs on the cap.
