new assets manually created

## Naming and what happens to these

`<vanilla-name>-new-<our-name>.png`, e.g. `birchtree-new-cloudtree.png`. The
first half is not a replacement target — it names the vanilla asset the sheet
was drawn on, so the setup can be looked up (`ObjectRegistry` / `TileRegistry`
registration, the class it uses, what that class's renderer reads) and mirrored
for ours.

Files here are the **source of record**. They are never edited in place. Where
the game needs a different arrangement of the same pixels,
`tools/convert_biome_art.py` repacks them into `src/main/resources/` and that
repack is reproducible from this folder.

| supplied | becomes | why it is repacked |
|---|---|---|
| `birchtree-new-cloudtree.png` 256×512 | `objects/cloudtree.png` 128×1024 | vanilla puts the snow form in column 1, and `TreeObject` reaches that column only on vanilla's `snowID`, which the Skyreach has none of. `getTreeSpriteY` is overridable, so the cold forms move to the lower half of a single column and `SkyTreeObject` picks the half from the ground. |
| `nimbuswillow.png` 128×512 | `objects/nimbuswillow.png` | **replacement**, copied in as-is — same 128×512 single column of four variants the generator produced, so no repack is needed. |
| `items-crystalwall-now-evilwall.png` 128×208 | `objects/evilwall.png` | copied in as-is. Already exactly vanilla `RockObject` format — 4 variants (`randomWidth = width/32`), each two 16px sprite columns wide, over the 13 sprite rows `addRockDrawables` reads. Nothing to repack. |
| `objects-crystalwall-now-evilwall.png` 32×32 | `items/evilwall.png` | the item icon. `RockObject.rockTextureName` feeds **both** `objects/<name>.png` and `items/<name>.png`, which is why one name covers the pair. |
| `overgrowngrass_splat-overgrowneden_splatt.png` 224×576 | `tiles/overgrowneden_splat.png` | copied in as-is — already vanilla's splat layout (the doubled `t` in the supplied name is normalised). Registered as `overgrownedentile` on vanilla's `OvergrownGrassTile` setup: grows grass tufts, spreads to dirt, seeds back 4%. Grain is per-pixel rather than 2×2 (density 713, mean dRGB 48.2), exempted in `tile_behaviour_audit` as converted art — the player judges it in game. |
| `overgrowngrassseed-overgrownedenseed.png` 32×32 | `items/overgrownedenseed.png` | the seed's icon. `overgrownedenseed` is vanilla's `GrassSeedItem` plus one override so it plants on Cloudturf as well as dirt; found in sky crates. |

## A second naming form, and the trap in it

Files also arrive as `<folder>-<vanilla-asset>-now-<our-name>.png`, e.g.
`items-crystalwall-now-evilwall.png`. Same idea as `-new-`: the middle name is
the vanilla asset the sheet was drawn on, so its registration can be looked up
and mirrored.

**Do not trust the folder prefix.** In the evilwall pair it was inverted — the
file prefixed `items-` is the 128×208 object sheet and the one prefixed
`objects-` is the 32×32 icon. Go by the pixel dimensions, which cannot lie.

**And do not trust the word in the vanilla name either.** `crystalwall` sounds
like a building wall; every wall sheet in this mod is 352×128 and this one is
128×208, which looked like a mismatch and was not. The object that owns
`objects/crystalwall.png` in the 1.3.2 jar is a **`RockObject`** — a mineable
rock, registered under the object ID `crystalrock`. Look the registration up in
`ObjectRegistry` before matching a sheet to a format; the full contract is in
`docs/TECHNICAL_LEARNINGS.md`.

A file with no `-new-` in its name replaces a sheet we already ship. When that
happens the generator must stop producing it, or the next full run silently
overwrites the supplied art: `generate_assets.py` drops the call and lists the
path in its `converted` guard, which fails loudly if anything writes it again.
The piece's companions (sapling, leaves, log icon) stay generated — only the
supplied sheet changes hands.

## Drawing a wall set? Two tools exist for exactly that

* **`docs/references/wall-template-map.png`** — the annotated 352×128 template:
  every cell labeled with its HALF (L/R), its screen BAND (abv/top/bot) and its
  role, plus both window views and all eight door cells. Draw over it.
  **Nothing on the sheet is unused** — cols 2–3 of rows 5–7 only appear where
  your wall abuts a wall of a *different* material, and vanilla paints all six
  solid; leaving them empty punches holes wherever two wall sets meet.
* **`python3 tools/conform_wall_sheet.py your.png`** — measures the sheet
  against five vanilla walls: size, region alpha, door extents, EVERY seam the
  engine can compose (tolerance = vanilla's own contrast at the same join),
  and the roof-slot test. `--fix` snaps integer upscales, fills alpha holes,
  shifts door leaves into the visible box and blends failing seam edges;
  `--fix --rebuild-roof-slot` rebuilds the N-S window with the shared
  construction, tones sampled from your sheet. Writes 4× compare sheets next
  to vanilla into `build/qa/`.

## The exception: a supplied sheet is only adopted if it is drawn ON the format

Two files here are **design sources only** and are deliberately not shipped,
even though their names carry no `-new-` and the rule above would otherwise
adopt them:

| supplied | shipped instead | why |
|---|---|---|
| `beetlewall.png` 352×128, 16,001 colours | `tools/asset_generator/gen_beetlewall.py`, 37 colours | one continuous illustration painted across the 4×8 body block, which the engine reads as tile HALVES whose column-to-half mapping changes by row. No cell can meet its neighbour. The player saw exactly this in game: *"da stimmt kein Rand, Fenster oder sonst was von Layout"*. |
| `cloudmarblewall.png` 352×128, 10,855 colours | `tools/asset_generator/gen_cloudmarble.py`, 23 colours | same fault, plus a cap band at mean luminance 228 against skystone's 52 and a front-facing pane in the strip that draws the wall's ROOF. Also seen in game: *"die ganzen Wände blenden fast ... die Fenster sind seitlich falsch"*. |

Both were copied in once on 2026-09-01 and reverted the same day. The check
that catches it is cheap and worth doing before adopting any wall sheet:

```sh
python3 -c "
from PIL import Image; im=Image.open(PATH).convert('RGBA')
print(len({c for c in im.get_flattened_data() if c[3]>8}))"
```

A drawn wall sheet in this mod carries **19–38** distinct colours. Four figures
means an illustration, and an illustration of a wall is not a wall sheet. The
supplied file stays the source of record for the set's *identity* — palette,
motifs, mood — which is what the generator draws from.

## Cut frames beat a flat sheet

A mob supplied as `row-N/sprite-M.png` — every frame already free of its
neighbours — needs none of the guessing a flat sheet forces (overlapping rows,
touching sprites, a bottom strip that may be gibs or extra poses):

```sh
python3 tools/resheet_mob.py path/to/folder -o mobs/<id>.png
```

The spirit wraith arrived that way on 2026-09-02 and became `mobs/fenwraith.png`
in one command.

The three Nimbus Yak sheets arrived as flat images the same day and went in
through the flat-sheet path — `mobs/nimbusyak`, `_bull` and `_calf`, replacing
the recolours `livestock/SkyPelt` used to make of vanilla's `mobs/cow`,
`mobs/bull` and `mobs/calf`. Which file was which needed no guessing: the cow
wears a flower crown, the bull has the horn spread (42px across the up-view
against the cow's 35, exactly vanilla's own 42-vs-32 split) and the calf is the
small one.

## Not every mob is drawn on the walking grid

`CryoFlakeMob` and its kin are **64x128**: one column, two rows — a rotating
body over a pulse layer, both spun about the cell's centre. The cell size is
read off the texture's own WIDTH (`getWidth()`, jar CryoFlakeMob.java:133), so a
sheet supplied at the wrong width makes the engine cut the wrong cells out of
it. Use:

```sh
python3 tools/resheet_mob.py IN.png --layout spinner -o mobs/<id>.png
```

Anything off the pivot orbits instead of spinning, so both layers are centred;
one shared scale keeps the glow's sparkles on the body's arm tips.

The Aurora Flake arrived on 2026-09-02 already at 64x128 and already centred —
body span 52x54, glow 50x50, both on (30.5, 30.5), the same centre vanilla's own
flake uses — so it was copied in verbatim and needed no resheet at all. Its
bestiary icon is now cut out of that sheet by `tools/convert_biome_art.py`
instead of drawn, because a drawn icon drifts the moment the body changes: this
one was still a pale four-point star while the mob had become a violet eyed
crystal.
