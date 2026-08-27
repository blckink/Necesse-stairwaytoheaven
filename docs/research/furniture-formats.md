# Vanilla furniture: classes, sheet formats, registration

Ground truth read out of the decompiled 1.3.2 sources
(`necesse/level/gameObject/furniture/`) plus a measurement pass over the vanilla
sprite dump. Everything here is what the engine actually does — not a guess from
how the sprites look.

Why this file exists: a "table" that is a plain decoration object does not count
as a table. Room scoring, settler jobs, chairs facing tables, and table
decorations all key off the vanilla base classes below. Draw a beautiful table
sprite on a `GameObject` and the game sees a rock.

## 1. What each base class buys you

| Class | `furnitureType` | What it enables |
|---|---|---|
| `FurnitureObject` | (subclass sets it) | room property `furniture`, furniture item/crafting category, replaceable by other furniture |
| `TableObject implements TableObjectInterface` | `table` | counts as a table for room scoring; chairs orient toward it |
| `ModularTableObject extends TableObject` | `table` | + `TorchHolderInterface`, `DecorationHolderInterface` — this is the one that **carries table decorations and candles** |
| `DinnerTableObject` | `table` | 1×2 multi-tile table, also a decoration holder |
| `ChairObject implements ObjectUsersObject, ChairObjectInterface` | `chair` | **sittable** (`ChairObjectEntity`), emits a `SitDownTileLevelJob`, `facesTable` |
| `BenchObject` | `bench` | sittable 1×2 side-multi-tile, two seats |
| `DeskObject` | `desk` | desk room role |
| `DresserObject` | `dresser` | + torch/decoration holder on top |
| `BedObject` / `SettlerBedObject` | `bed` | **settlers can be assigned to it**; 1×2 multi-tile with a `_mask` sheet |
| `CarpetObject` / `ModularCarpetObject` | `carpet` | floor decoration, 2×2 static multi-tile / autotiling |
| `CandelabraObject extends LampObject` | `candelabra` | light source that counts as furniture; needs `<id>` and `<id>_off` sheets |
| `TableDecorationObject` | – | placed **on** a decoration holder, layer `FENCE_AND_TABLE_DECOR` |
| `PotTableDecorationObject` | – | as above + optional `flower`/`plant` room properties |

`ObjectUsersObject` + `ChairObjectInterface` is what makes a seat a seat. There is
no "sittable" flag to set on a decoration.

## 2. Sheet formats (measured on the vanilla dump)

All furniture textures load from `objects/<textureName>.png`.

| Piece | Sheet | Layout |
|---|---|---|
| chair | **128×64** | 4 rotation columns × 32 px, full height; drawn at `drawY - height + 32` |
| desk | **128×64** | same as chair |
| dresser | **128×64** | same as chair |
| toilet / clock / chest | 128×64 | same |
| bench | **128×128** | 4 rotations; registered as a pair (`<id>` + `<id>2`) |
| dinner table | **128×128** | 4 rotations; pair (`<id>` + `<id>2`) |
| bed | **128×128** + `<id>_mask` **128×128** | pair (`<id>` + `<id>2`) |
| double bed | 128×192 + `_mask` | pair |
| candelabra | **128×64** + `<id>_off` **128×64** | 4 rotations, lit and unlit sheets |
| modular table | **96×64** | 16 px autotile atlas — see §3 |
| carpet (static) | 64×64 | 2×2 tiles |
| table decoration | 32×32 (or `N*32`×H for N rotations) | `sprite(rotation, 0, 32, height)` |
| display stand | 128×32 | 4 rotations |

Rule of thumb: **4 rotation columns of 32 px**, height = however tall the piece
is, anchored so its *bottom* row sits on the tile. A 64 px sheet is a piece that
rises one tile above its floor tile.

## 3. `ModularTableObject`: the 96×64 atlas

6 columns × 4 rows of **16 px** cells. The object draws four 16 px quadrants
plus, when nothing connects below, a 16 px front apron 10 px lower.
`adj` is the 8-neighbour array: `0`=up-left `1`=up `2`=up-right `3`=left
`4`=right `5`=down-left `6`=down `7`=down-right.

Top-left quadrant (`drawX, drawY`), by (up, left):

| up | left | cell |
|---|---|---|
| no | no | (0,0) |
| no | yes | (2,2) |
| yes | no | (4,0) if up-left else (4,1) |
| yes | yes | (2,0) if **no** up-left else (0,2) |

Top-right (`drawX+16, drawY`), by (up, right): (1,0) / (3,2) /
(5,0)·(5,1) / (3,0)·(1,2) — mirror of the above.

Bottom-left (`drawX, drawY+16`), by (down, left): (0,1) / (2,3) / (4,1) /
(2,1) if no down-left else (0,3).

Bottom-right (`drawX+16, drawY+16`), by (down, right): (1,1) / (3,3) / (5,1) /
(3,1) if no down-right else (1,3).

Front apron, only when nothing is below, at `drawY + 26`:

| left | right | cells |
|---|---|---|
| yes | yes | (4,3)+(5,3) |
| yes | no | (4,3) then (5,2) |
| no | yes | (4,2) then (5,3) |
| no | no | (4,2)+(5,2) |

Note cells (4,1) and (5,1) do double duty (top quadrant with an up-neighbour,
*and* bottom quadrant with a down-neighbour), and the (4..5, 2..3) block doubles
as the apron. Draw them so they read both ways: a plain table-top edge.

Top-left of the whole draw is at `drawY - 14`, so the table body sits 14 px above
its tile and the apron hangs to `drawY + 26`.

## 4. Registration

Single-tile pieces go through the normal path:

```java
ObjectRegistry.registerObject("<id>", new ChairObject("<id>", mapColor, category), 5.0F, true);
```

Multi-tile pieces have static helpers that register **two** objects (the second
is the other half, broker value `0.0F`, not obtainable) and wire their
`counterID`s:

```java
BenchObject.registerBench("<id>", "<id>", mapColor, 10.0F, category);        // + "<id>2"
BedObject.registerBed("<id>", "<id>", mapColor, 100.0F, category);           // + "<id>2"
DinnerTableObject.registerDinnerTable("<id>", "<id>", mapColor, 20.0F, cat); // + "<id>2"
CarpetObject.registerCarpet("<id>", "<id>", mapColor, 25.0F);                // + 3 more
```

Vanilla broker values for a wood family: chair 5, desk/dresser/bench/bookshelf/
cabinet/clock/candelabra/chest 10, dinner table 20, display 20, bed 100,
double bed 150, table decorations 20.

Item categories vanilla uses: `{"objects","furniture",<material>}` for the
furniture family, `{"objects","lighting"}` for candelabra when no category is
passed, `{"objects","landscaping","tabledecorations"}` for decorations,
`{"objects","decorations","carpets"}` for carpets.

## 5. Traps

- The second half of a multi-tile piece (`<id>2`) is registered for you. Do **not**
  register it yourself, do **not** give it a recipe, and do **not** expect an
  `items/<id>2.png` — it is not obtainable.
- `<id>2` still needs no localization of its own; it inherits.
- A bed needs `objects/<id>_mask.png` at the same size or it will not draw the
  sleeping settler correctly.
- A candelabra needs `objects/<id>_off.png` or the unlit state falls back to the
  error texture.
- `ModularTableObject` sets `replaceRotations = false` — it has no rotations, it
  autotiles. Do not draw it as 4 rotation columns.
- Anything the player can hold still needs `items/<id>.png`, as always.

## 6. Carpets: `CarpetObject.registerCarpet` is dead code in 1.3.2

Do not use it. All four quarter classes (`CarpetObject`, `CarpetRObject`,
`CarpetDObject`, `CarpetDRObject`) pass `isMaster = true` into their
`StaticMultiTile`, and `MultiTile.validate` rejects a set with more than one
master. Registering one crashes the dedicated server at registry close:

```
IllegalStateException: <id> has invalid multi tile setup: Has multiple master objects
    at ObjectRegistry.onRegistryClose(ObjectRegistry.java:1319)
```

Nothing in vanilla's `ObjectRegistry` registers one, which is why the bug has
never surfaced in the shipped game. Every carpet in 1.3.2 is a
**`ModularCarpetObject`**, which autotiles instead of using a 2×2 multi-tile:

- `objects/carpets/<id>.png` — **64×64**, a repeating 2×2 block of 32 px
  pattern cells. This is the only part that carries your design.
- `objects/carpets/<id>mask.png` — **64×64**, the edge mask. Geometry only, no
  colour: the engine multiplies it onto the pattern.

`loadTextures` tiles one pattern cell across a `(mask.width/32 + 1) * 32` wide
"part" texture (96×64 for a 64×64 mask) and multiplies the mask over it at the
origin — so the right third stays unmasked, which is exactly the fully-interior
cells the autotiler wants solid.

The mask's 4×4 grid of 16 px cells, measured on the canonical vanilla mask
(7 of 12 vanilla carpets share this exact alpha geometry):

| | col0 | col1 | col2 | col3 |
|---|---|---|---|---|
| row0 | 48% opaque | 48% | 69% | 69% |
| row1 | 48% | 48% | 69% | 69% |
| row2 | 69% | 69% | 89% | 89% |
| row3 | 69% | 69% | 89% | 89% |

The edge itself is a **2 px stepped dither**, not a gradient: fully clear
outside, one band of 2×2 blocks at roughly a third alpha, then fully opaque.
Cell (0,0) is the outer corner with two clear edges.
