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
