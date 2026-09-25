# Art rounds — what came back and what the player said

One row per job. Minutes come from `build/codex_runs.tsv` (written by
`tools/codex_art.sh`). "first pass" = shippable without another Codex round;
"fix" = what it took. After a round, the lesson goes into
`docs/art/CODEX_ART.md` the same day.

| date | job | assets | min | first pass | fix | player |
|---|---|---|---|---|---|---|
| 2026-09-17 | veteran4 wire + codexwall veteranbarricade | fence 160x64, gate 192x64, 2 icons; wall set | ~5 / ~4 | 2/2 | brief named the vanilla sheet as a HARD MASK ("build with a script that uses ref1/ref2 as masks") → posts pixel-exact, rails became wire strands on the first try; the old barricade (417 colours, 39 seams) could not be conformed — codex_wall canvas + "20-38 colours, rows on fixed y" came back at 37 colours, 0 seams over tolerance | shipped |
| 2026-09-19 | veteran cannon follow-up: cannonball | 1 projectile 16x16 | 1.2 | 1/1 | brief demanded radial symmetry ("must look right while SPINNING around its own centre: no flat side, no nub, no fuse") because CannonBallProjectile rotates the texture; `--bg 255,255,255` ate the near-white glint at every tolerance — flood fill from the border instead | shipped, not yet seen in game |
| 2026-09-19 | veteran catapult -> CANNON, 4 directions | 1 object sheet 384x512 + 32x32 icon | 1.4 | 3/3 masters | brief handed the OLD asset as ref1 and said explicitly what NOT to inherit ("we are REPLACING this; camera/scale/palette only; draw NO catapult: no throwing arm, no rope, no bucket") -- without that sentence Codex inherits the shape, as the woodfence mask did; size as a NUMBER (">=90% of cell width, the old machine is too small, draw chunkier") took the art from 65-84 px to 86-92 px of 96 on the first try; SLOTS=3 ran north/east/south in parallel in 1.4 min, west = per-CELL mirror of east (mirroring the whole 384 px row reverses the frame order -- the old assemble.py did that) | shipped, not yet seen in game |
| 2026-09-16 | codexsplat gloomwoodfloor_v2 | 8 floor cells | 3.1 | 1/1 | — (one 32 px periodic weave, seam audit 0.00) | shipped |
| 2026-09-16 | codexsplat cloudturf_v3 | 24 ground cells | 4.1 | 0/1 | top pixel row carried hill tips → replaced by row below (audit 4.81 → 0.83); bushes all sit low → faint rows remain | shipped |
| 2026-09-16 | codexwall nightfell2 | wall set | ~3 | 1/1 | conform --fix --rebuild-roof-slot | shipped |
| 2026-09-16 | codexsplat nimbusfloor / prismfloor | floor cells | 3.5 / 2.6 | 2/2 | — | shipped |
| 2026-09-16 | codexwall nightfell | wall set | ~6 | 0/1 | cut clean, but flat: near-uniform roof, little detail → redo with more shading | not shown yet |
| 2026-09-16 | codexsplat cloudturf r1 | 24 ground cells | 3.2 | 0/1 | seams gone, but flat 2 px frame draws a faint grid, colours too dark → cloudturf_v2 | not shown yet |
| 2026-09-16 | codexwall skystonebrick | wall set | 2.8 | 1/1 (review) | notch/junction harmonised; conform --fix --rebuild-roof-slot --quantize 38 | shipped |
| 2026-09-16 | twilight4 m3 | clock side view, electric chair | 4.5 | 2/2 | — | shipped for play |
| 2026-09-16 | twilight4 m2 | coffin bed + mask, wall eye | 5.6 | 1/2 | eye side rows shifted to wall edge (code) | shipped for play |
| 2026-09-16 | twilight4 m1 | clock back/sides, candelabra, electric chair | 6.4 | 1/3 | clock col 1 broken, chair unreadable on dark floor → m3 | candelabra shipped |
| 2026-09-16 | player upload | 12 recolours (walls, grounds, plants, mobs) | — | 11/12 | megashark smoothed by Photoshop → mapped back to its 57-colour palette | "Farben wie davor" |
| 2026-09-15 | twilight3 night run | 11 outfits, furniture | — | outfits later 11/11 | furniture PIL shapes rejected | outfits "mega gut" |
| 2026-09-15 | twilight decor/outfits (1st) | all | — | 0/all | PIL rectangles as final art | "riesen Rückschritt" |

| 2026-09-16 | veteran siege set (9 assets, hard time limit) | catapult+turret sheets, barbed wire, barricade, stone, 4 icons | ~1.5 | 9/9 (masters) | catapult 4-frame sheet: 1 extra codex round asked for "same base, only arm moves" on top of the idle master; base still drifts slightly between frame 0 and frames 1-3 (wheel spacing, stance) — shipped anyway, time-boxed | shipped |
| 2026-09-16 | veteran catapult+turret → 4-direction sheets (hard 23:20 stop) | 2 object sheets, 384x512 + 128x192 | ~2 | 4/4 rows after 1 extra call | one Codex call per new direction (not per object) keeps a row's 4 frames on one shared base — worked for catapult up/down, turret up/right in a single generation each; rotating a non-square 32x48 cell 90° in PIL to fake a missing direction squashes the round sandbag ring into an oval (naive pad-to-square-then-crop-back loses the post-rotation offset) — do not rotate asymmetric cells to fake a direction; asked Codex for the real turret DOWN view instead (1 more ~1.5 min call) and it landed clean on the first try | shipped |

## What this says so far

- Per asset, a Codex round costs about two minutes including its own checks;
  one asset per job with two slots in parallel is the fastest safe setting.
- First-pass rate on 2026-09-16 was 4/7 in the round and 7/7 after one fix.
  The failures were layout (a broken side view, a centred side row) and
  context (dark on dark), not style.
| 2026-09-25 | stormcrystal cluster (player: "sieht aus wie Federn oder Blätter") | 256x64 sheet + 32x32 icon | 5.4 | 1/1 | old brief-less needles replaced by faceted prisms with lightning veins on a stone base; brief said "no thin tapered spikes / leaf / feather" and named the in-game blue ground — first pass shipped; variants 1 and 4 a little alike | shipped |
| 2026-09-25 | hauntedscarecrow (Twilight Merchant, Objekt ohne Bild) | 32x64 sheet + 32x32 icon | 4.5 | 1/1 | jack-o-lantern head with witch hat, ragged coat on crossbar, straw tufts; brief gave refs (own tombstone for camera, pumpkin outfit/vanilla pumpkin for colour) and number targets (>=900 px, 56-62 px tall, crossbar >=26 px) - met first pass (927 px, 62 tall, 32 wide); coat is busy/noisy and the grin is small at 1x | shipped |
