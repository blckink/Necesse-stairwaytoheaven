# Brief: wearables (head / chest / arms / boots)

- Sheets as vanilla `player/armor/*` (448 wide = 7 × 64 cells); rows are the
  facing directions: row 0 = facing UP (back of head/body), row 1 = right,
  row 2 = facing DOWN (face/front), row 3 = left. Lay column 0 next to
  vanilla `goldhelmet.png` / `captainsshirt.png` before delivering. Check the sizes of the sheet you replace.
- Draw each FACE (front, side, back) as its own image_gen master, then place.
- A mask or helmet must cover the vanilla skin head: head box per cell is
  x18–46, y14–38 of `player/skin/head.png`; less than 5 % skin may show
  (`build/twilightheads/verify_heads.py`).
- Mock: the outfit worn on the vanilla body (body, feet, boots, chest, head,
  helmet, arms) — `tools/scene_preview.py` `figure()` does this.
- Player verdict 2026-09-16: the eleven Twilight outfits are the reference for
  "good".
