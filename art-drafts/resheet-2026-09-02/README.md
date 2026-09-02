# Resheet drafts, 2026-09-02 — for correction

Output of `tools/resheet_mob.py` on four supplied mob sheets. **Not shipped.**
The player reported these are not 100% right and is fixing them by hand; the
corrected files come back here and the fixes then go into the tool, so the next
batch does not need the same hand pass.

| file | what it is |
|---|---|
| `SOURCE_calf.png` | the plain yak — it is the CALF, not the cow (player) |
| `SOURCE_cow.png` | the flower-crowned one: the COW |
| `SOURCE_bull.png` | the horned one: the BULL |
| `SOURCE_*.png` | exactly what was supplied, untouched |
| `*_384x320.png` | what the tool made of it — the file to correct |
| `PREVIEW_*.png` | the same at 3× on dark and light with the 64px grid drawn over it |

## The best input is a FOLDER of cut frames

The player supplied the spirit wraith that way — `row-N/sprite-M.png`, every
frame already free of its neighbours — and it skips every hard part at once.
All the detection in this tool exists only because a flat sheet hides where one
frame ends and the next begins: rows that overlap, sprites that touch, a bottom
strip that may be gibs or may be extra poses. A folder answers all of it.

```sh
python3 tools/resheet_mob.py path/to/folder -o mobs/<id>.png
```

Folder name = row, file name = column, last folder = the gib strip when there
are more folders than directions. Crumbs left by the export (row-1 carried an
8×11 speck after its six real frames) are dropped by area.

`spiritwraith_384x320.png` is that path; the `wraith_*` files are the same
creature through the flat-sheet path, kept for comparison.

## How each was produced

```sh
python3 tools/resheet_mob.py SOURCE_calf.png   # rows detected
python3 tools/resheet_mob.py SOURCE_cow.png    # 2 rows split evenly
python3 tools/resheet_mob.py SOURCE_bull.png   # rows + 2 rows split evenly
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

5. **Frames were placed as a row strip, not one by one.** *"du musst
   eigentlich jeden einzelnen frame ausschneiden und dann einfach ausrichten
   horizontal und vertikal im jeweiligen 64x64px ausschnitt"*. Each frame is now
   cut out and centred in its own cell. Vertical is still measured from the
   ROW's floor, not the frame's own — snapping each frame to its cell floor
   would drop a lifted hoof back to the ground and kill the step.
6. **Nothing guaranteed a frame FIT its cell.** The engine draws
   `sprite(col, row, 64)`, so anything past the cell edge is not on the sheet at
   all. At the span scale the frames measured: calf 47 px (fine), wraith 70,
   flowers 64×66, blue yak **115**. The scale is now capped by the widest and
   tallest frame.
7. **Fused columns and folded rows.** The blue yak had a 419 px "frame" (two
   sprites touching, against a 216 px median) and only two row bands for four
   directions. A column band far wider than its siblings now forces an even
   split, and a tall band is divided by its own height — 432 and 416 px against
   a 212 px mean row recovers 2 + 2 exactly. `--rows y0,y1,y2,y3,y4` remains as
   the escape hatch.

`--mirror-left` was added for the player's other fix: building the LEFT row by
mirroring RIGHT cell by cell, when a generator drew the two side views as two
different animals. Cell by cell, never the whole strip — flipping the strip
would reverse the column order and put the idle pose in column 5. It flips the
light direction, which is a real cost worth checking.

## Target when corrected

384×320: 6 cols × 4 rows of 64 px (Up / Right / Down / Left), then the 32 px gib
strip at y256, then 32 px of padding. See `docs/references/template-mob.png`.

## Shipped 2026-09-02

All four went in. `spiritwraith_384x320.png` -> `mobs/fenwraith.png`;
`cow`/`bull`/`calf` -> `mobs/nimbusyak{,_bull,_calf}.png`, which replaces the
`SkyPelt` recolour of vanilla's cow, bull and calf. The files here stay as the
before/after record and as the regression input for `tools/resheet_mob.py`.
