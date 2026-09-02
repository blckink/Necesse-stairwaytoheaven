# Resheet drafts, 2026-09-02 — for correction

Output of `tools/resheet_mob.py` on four supplied mob sheets. **Not shipped.**
The player reported these are not 100% right and is fixing them by hand; the
corrected files come back here and the fixes then go into the tool, so the next
batch does not need the same hand pass.

| file | what it is |
|---|---|
| `SOURCE_calf.png` | the plain yak — it is the CALF, not the cow (player) |
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

## Corrected 2026-09-02 after the player's hand pass

The first version was wrong in three ways; all three are fixed in
`tools/resheet_mob.py` and these files are regenerated.

1. **Scale.** It cropped every sprite and re-fitted it to its cell, which came
   out ~20% too large (0.353 against the correct 0.288 on the calf). The player's
   method is right: scale the WHOLE sheet once so its content spans 384, then
   only slide rows. Measured, the four rows individually want 0.2995 / 0.2927 /
   0.3048 / 0.2931 and the whole image wants 0.2876 — one number serves.
2. **Horizontal offsets were destroyed.** Centring each sprite in its cell irons
   the walk cycle flat; a hoof planted slightly forward on frame 2 IS the
   animation. Now only the row's drift is corrected, never the sprite's place
   within the row.
3. **The gib strip was dropped entirely.** The five death chunks now land in
   32 px cells at y256, where FleshParticle reads them.

4. **Overlapping rows dragged their neighbours along.** The player, on the
   wraith: *"in zeile 2 siehst du, dass dann von zeile 1 und zeile 3 der untere
   und obere teil mit reinragen, die man entfernen muss eigentlich"*. A
   rectangular crop cannot separate rows that overlap. Rows are now cut apart by
   BLOB OWNERSHIP — each connected component goes to the band holding most of
   its pixels — so a trail that belongs to its sprite stays attached while a
   stray piece from the row above goes back to the row above. `--no-isolate`
   skips it.

`--mirror-left` was added for the player's other fix: building the LEFT row by
mirroring RIGHT cell by cell, when a generator drew the two side views as two
different animals. Cell by cell, never the whole strip — flipping the strip
would reverse the column order and put the idle pose in column 5. It flips the
light direction, which is a real cost worth checking.

## Target when corrected

384×320: 6 cols × 4 rows of 64 px (Up / Right / Down / Left), then the 32 px gib
strip at y256, then 32 px of padding. See `docs/references/template-mob.png`.
