# Art-direction references

Look references for assets that are drawn in `tools/asset_generator/`. They are
**not** shippable assets and are never loaded by the game — they are here so an
art pass can be checked against what was actually asked for.

Every one of these is an anti-aliased render, not pixel art: thousands of
distinct colours, horizontal run lengths dominated by 1 px, no alpha channel
(transparency is painted black). They cannot be downsampled into a game sheet;
they get redrawn at the real format in the generator.

| file | for | measured |
|---|---|---|
| `skyway-floor-reference.png` | Skyway Passages ground tile + `_splat` | 1354×1161 RGB, 77,052 colours |
| `cloudmarble-wall-reference.png` | Cloudmarble wall set (wall + door + window) | 983×1600 RGB, 105,012 colours |
| `cloudmarble-door-fence-reference.png` | Cloudmarble door, archway, railing + gate | 2078×757 RGB, 110,039 colours |
