# Resheet drafts, 2026-09-02 — for correction

Output of `tools/resheet_mob.py` on four supplied mob sheets. **Not shipped.**
The player reported these are not 100% right and is fixing them by hand; the
corrected files come back here and the fixes then go into the tool, so the next
batch does not need the same hand pass.

| file | what it is |
|---|---|
| `SOURCE_*.png` | exactly what was supplied, untouched |
| `*_384x320.png` | what the tool made of it — the file to correct |
| `PREVIEW_*.png` | the same at 3× on dark and light with the 64px grid drawn over it |

## How each was produced

```sh
python3 tools/resheet_mob.py SOURCE_yak-plain.png     # rows detected
python3 tools/resheet_mob.py SOURCE_yak-flowers.png   # 2 rows split evenly
python3 tools/resheet_mob.py SOURCE_yak-blue.png      # rows + 2 rows split evenly
python3 tools/resheet_mob.py SOURCE_wraith.png --alpha 120
```

## What the tool decided, so a correction can name the wrong decision

- **One shared scale** for all 24 sprites, from the largest source cell. Per-cell
  scaling makes a walk cycle breathe, so this is deliberate — but it does mean a
  row with wide side views shrinks the front views. `wraith` is the clearest case
  (side views 272 px wide against 209 tall).
- **Bottom-anchored**: feet 2 px above the cell floor, centred horizontally.
- **Even split fallback** where sprites touch or rows overlap; the boundaries are
  then arithmetic, not measured, so a sprite can be clipped or off-centre.
- **Hard alpha** at threshold 110 — soft edges become fully opaque or gone.

## Target when corrected

384×320: 6 cols × 4 rows of 64 px (Up / Right / Down / Left), then the 32 px gib
strip at y256, then 32 px of padding. See `docs/references/template-mob.png`.
