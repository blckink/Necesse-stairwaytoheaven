# Brief: mobs

- Walking mobs: 6 columns (idle, walk×4, swim) × 4 rows, rows **Up, Right,
  Down, Left**; cell size from the sheet you replace (64 or larger).
- One image_gen master per facing; walk frames by small leg/body offsets of the
  same master, not four new drawings.
- Keep every frame's feet on the same baseline and the body centred in the cell.
- The striped megashark (`mobs/stripedmegashark.png`) is the quality bar.
- Mock: the down-facing idle frame on Cloudturf and Stormslate at 1x and 3x,
  next to the vanilla mob it replaces.
