# Codex art jobs — the one page to read

This file is what every image job starts from. It is kept short on purpose and
it is **updated after every art round** (see "Lessons" at the bottom and
`docs/art/RUNS.md`). If a round teaches something, it goes here the same day.

## The route that works (the player's "best texture in the game" came this way)

1. Open 2–3 vanilla sprites of the SAME kind in
   `/home/blackoffset/dev/Necesse sprites/` and measure size, layout, anchor and
   opaque pixel mass. Target ≥ 80 % of the vanilla mass.
2. Make a **master with the built-in image_gen**: one pose / view per image,
   large, plain background, light from top left, Necesse 3/4 top-down view.
3. Down to the target size by **modal colour per block** (not bilinear),
   **≤ 40 colours**, **alpha hard 0/255**, 1 px outline `(34,34,46)`.
   `tools/convert_reference.py --native WxH` does this.
4. Small pixel fixes by hand are fine. **Shapes drawn with PIL
   (`draw.rectangle`, `ellipse`, `line`) are never the final art** — that is
   what the player rejected as "eckig, flach, wie mit Paint".
5. Check before handing in: contact sheet 4x on dark AND light ground, and a
   1x/3x mock next to the vanilla counterpart. Look at it. Write the numbers
   (size, colours, opaque px vs vanilla) and one honest sentence of criticism
   into `codex_last.txt`.

Environment: `PYTHONPATH=/home/blackoffset/dev/pylib python3` — no venv, do not
download anything. Write only under `build/`, never into `src/main/resources`
(the reviewer ships). `.git` is read-only for you; do not try to pull.

## Formats per kind — the brief for your kind is in `docs/art/briefs/`

| kind | brief | the trap |
|---|---|---|
| furniture with rotations (clock, chair, candelabra, bed) | `briefs/furniture.md` | column 0 is the BACK, 1 right, 2 FRONT, 3 left; beds have their own quadrant layout |
| wall pieces / paintings | `briefs/wall-pieces.md` | side rows hug the wall edge, not the cell centre |
| wearables (head/chest/arms/boots) | `briefs/wearables.md` | the mask must cover the vanilla skin head |
| mobs | `briefs/mobs.md` | 6 cols × 4 rows, rows Up/Right/Down/Left |
| ground splats | `docs/art/TILES_CODEX.md` | the game rolls every 32 px cell alone |
| wall sets | `docs/art/WALLS_CODEX.md` | never paint the 352x128 atlas directly |

## Style

Necesse pixel art: chunky, rounded, soft dark outline, 4–5 flat shade steps per
material, light top-left, micro-details (grain, cracks, 1–2 px glints, cobwebs,
drips). Saturation per realm (`docs/WORLD_DESIGN.md` §36). Twilight Merchant:
dark, gothic, Beetlejuice/Addams — but still readable on a dark floor.

## Lessons (newest first — append, do not rewrite)

- 2026-09-16 · **Floors: one pattern with a 32 px period.** The gloomwood weave
  and the nimbus herringbone passed first time because every cell shares the
  same structure; a second gloomwood round with 24 different cells failed.
- 2026-09-16 · **Check the edge rows, not only the look.** Cloudturf's top
  row carried hill tips across the cell edge; the seam audit caught it and a
  one-row copy fixed it. Motifs should also not all sit low in the cell, or
  the field shows rows.
- 2026-09-16 · **Wall sheets out of the canvas need `conform_wall_sheet --fix
  --rebuild-roof-slot --quantize 38`** — blending seams alone pushed one
  sheet from 40 to 357 colours.

- 2026-09-16 · **Ground cells: no frame.** Keeping motifs 2 px off the cell
  edge fixed the seams, but Codex filled that margin with a flat colour while
  the interior was mottled — the flat frame drew a faint grid. The base
  texture (speckle, mottling) must run to the edge; only the motifs keep off.
- 2026-09-16 · **Walls through a hard mask work.** Painting whole shapes on the
  `wall_from_layout` canvas next to the same canvas filled with vanilla
  stonewall gave the first wall the player had not seen as a cut-up picture,
  in under three minutes. Trap: a 16 px cell that appears in two shapes must
  be pixel-identical in both, or the cut refuses (notch vs junction).

- 2026-09-16 · **One view per master image** beat whole sheets: clock back/side
  views, candelabra, coffin bed and chair all came right within two rounds.
- 2026-09-16 · **Rotation columns break silently.** A side view fell apart into
  two thin sticks; a mirror of the good opposite side is an allowed fix.
- 2026-09-16 · **Dark on dark disappears.** A dark-brown chair vanished on the
  dark wood floor; mocks must include the floor the piece will stand on.
- 2026-09-16 · **Mass targets work when they are numbers.** "Like the mirror
  next to it" became "≥ 1400 opaque px in row 0" and was met first time.
- 2026-09-16 · **Wall-piece side rows** were centred instead of against the
  wall edge — a pure shift, but it must be in the brief.
- 2026-09-16 · **Photoshop re-saves smooth pixel art** (57 → 44 000 colours).
  Map back to the sheet's own palette and alpha levels instead of redrawing.
- 2026-09-16 · **Painted ground surfaces tile badly.** The game picks every
  full 32 px cell at random; motifs across cell edges show a grid.
- 2026-09-15 · **Six parallel image_gen runs emptied the quota in four
  minutes.** Two tracks at a time; check `codex.log` for "usage limit".
- 2026-09-15 · **Mask heads sat too high**: 28–46 % of the vanilla skin head
  stayed visible. Head box per cell is x18–46, y14–38 of `player/skin/head.png`.
- 2026-09-15 · **PIL shapes as final art** → rejected across the board.
