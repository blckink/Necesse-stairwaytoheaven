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
- Twilight outfits: chest and boots must show THE costume of the head, named
  in every job (2026-09-17: a generic brown coat replaced Freddy's sweater):
  twilightsuit = black/white vertical stripes suit; skeleton = ribcage and
  spine on black; grinclown = white ruffled clown suit, red pompoms;
  hockeyslasher = worn olive work jacket; dreamstalker = red/green
  horizontal striped sweater, brown trousers; screamrobe = black hooded robe;
  pincushion = black leather cassock; widowgown = black lace mourning dress;
  stitchedmonster = too-short black jacket, grey-green skin, bolts;
  pumpkinscarecrow = patched burlap, straw tufts; hauntedpuppet = striped
  shirt under blue denim overalls, red sneakers.
