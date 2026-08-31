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
