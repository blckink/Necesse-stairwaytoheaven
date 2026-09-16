# Art rounds — what came back and what the player said

One row per job. Minutes come from `build/codex_runs.tsv` (written by
`tools/codex_art.sh`). "first pass" = shippable without another Codex round;
"fix" = what it took. After a round, the lesson goes into
`docs/art/CODEX_ART.md` the same day.

| date | job | assets | min | first pass | fix | player |
|---|---|---|---|---|---|---|
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

## What this says so far

- Per asset, a Codex round costs about two minutes including its own checks;
  one asset per job with two slots in parallel is the fastest safe setting.
- First-pass rate on 2026-09-16 was 4/7 in the round and 7/7 after one fix.
  The failures were layout (a broken side view, a centred side row) and
  context (dark on dark), not style.
