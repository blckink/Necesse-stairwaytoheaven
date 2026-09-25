# Brief: wall pieces and paintings

| class | sheet | rows |
|---|---|---|
| LargePaintingObject (2 tiles wide) | 64×256 | row 0 front, row 1 side wall (content hugs the RIGHT edge, x≈48–64), row 2 back, row 3 other side wall (content hugs the LEFT edge, x≈0–16) |
| PaintingObject (1 tile) | 32×128 | four 32×32 views, same idea |

- Free-form shapes without a frame are fine.
- Height is the hard limit: the wall face is band-y 32..64, so the large
  frame's front sits at y34..62 (28 px, never above y33), the back at y28..56,
  the side views centred on y32 (`tools/align_wall_piece.py`,
  `tools/draw_rect_audit.py` fails anything above y32). Compose WIDE and short,
  up to ~44 px wide; a tall motif has to be shrunk until it is mush.
- State the mass as a number: measure the neighbour piece (e.g. `magicmirror`
  front 762 opaque px, sides 165, after alignment) and aim at or above it.
- Mock: all pieces side by side on a wall at 1x and 3x.
