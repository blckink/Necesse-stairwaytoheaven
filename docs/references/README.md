# Art-direction references

Look references for assets that are drawn in `tools/asset_generator/`. They are
**not** shippable assets and are never loaded by the game — they are here so an
art pass can be checked against what was actually asked for.

Every one of these is an anti-aliased render, not pixel art. The second-pass
files carry a real alpha channel, which the first pass did not, but they are no
closer to being usable: the colour count went *up*, 93-96% of horizontal runs
are a single pixel, and a nearest-neighbour round trip at 2x-6x loses 27-63% of
pixels at every factor — so there is no pixel grid at any scale. Alpha is soft
too (256 distinct values; only 7.3% hard on the wall). They cannot be
downsampled into a game sheet; they get redrawn at the real format in the
generator.

For a future export to drop in directly it would need: the exact target size
with no upscaling (wall 352x128, fence 160x64, gate 192x64, tree cell 256x512,
floor splat 224x(96*variants)), nearest-neighbour export with antialiasing off,
roughly 20-40 colours per sheet, and alpha limited to 0 or 255.

| file | for | measured |
|---|---|---|
| `skyway-floor-reference.png` | Skyway Passages ground tile + `_splat` | 1354×1161 RGB, 77,052 colours |
| `cloudmarble-wall-reference.png` | Cloudmarble wall set (wall + door + window) | 983×1600 RGB, 105,012 colours |
| `cloudmarble-door-fence-reference.png` | Cloudmarble door, archway, railing + gate | 2078×757 RGB, 110,039 colours |
| `cloudmarble-wall-reference-v2.png` | same wall set, second pass — **use this one**, the motifs read more clearly | 984×1599 RGBA, 149,832 colours, 92.9% 1px runs |
| `cloudmarble-door-fence-reference-v2.png` | same door/railing set, second pass — **use this one** | 2079×756 RGBA, 164,466 colours, 95.0% 1px runs |
| `skytree-reference.png` | sky trees: 8 designs, gold-orange and white-gold with a halo | 887×1774 RGBA, 275,475 colours, 95.7% 1px runs |
