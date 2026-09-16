# Brief: wall pieces and paintings

| class | sheet | rows |
|---|---|---|
| LargePaintingObject (2 tiles wide) | 64×256 | row 0 front, row 1 side wall (content hugs the RIGHT edge, x≈48–64), row 2 back, row 3 other side wall (content hugs the LEFT edge, x≈0–16) |
| PaintingObject (1 tile) | 32×128 | four 32×32 views, same idea |

- Free-form shapes without a frame are fine.
- State the mass as a number: measure the neighbour piece (e.g. `magicmirror`
  front 1394 opaque px, sides 287) and aim at or above it.
- Mock: all pieces side by side on a wall at 1x and 3x.
