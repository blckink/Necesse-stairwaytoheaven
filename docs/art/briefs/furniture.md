# Brief: furniture with rotations

Source of truth: the game code (`sprite(rotation % 4, 0, 32, height)`).

| class | sheet | columns / quadrants |
|---|---|---|
| ClockObject, ChairObject, CandelabraObject | 128 × height (64) | col 0 = BACK (faces up), col 1 = faces RIGHT (narrow side view), col 2 = FRONT (faces down), col 3 = faces LEFT (mirror of col 1 allowed) |
| BedObject | 128×128 + `<id>_mask.png` | rot 1 = x0–63 y64–127, rot 3 = x0–63 y0–63 (lying), rot 0 = x96–127 y32–127, rot 2 = x64–95 y32–127 (standing, two tiles) — copy the placement of `oakbed.png` / `oakbed_mask.png` exactly |

- Nothing may cross the 32 px column edge (it is cut off in game). Touching it is fine.
- CandelabraObject loads no `_off` texture — do not deliver one.
- Vanilla to measure: `objects/oakclock.png`, `boneclock.png`, `oakchair.png`,
  `bonechair.png`, `oakcandelabra.png`, `bonecandelabra.png`, `oakbed.png`.
- One image_gen master per VIEW (back, side, front), then place them.
- Mock: every column on the floor it will stand on (dark AND light wood), next
  to the vanilla column. A side view must be one closed body.
- Deliver `items/<id>.png` 32×32 as a compact glyph, not the shrunken sheet.
