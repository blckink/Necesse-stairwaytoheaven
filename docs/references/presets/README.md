# Supplied POI reference presets

Necesse's in-game preset copy tool puts `preset.getCompressedBase64Script()` on
the clipboard, and `new Preset(String script)` parses it back — so a share-code
is a perfectly good way to hand a POI design over. `tools/preset_decode.py`
turns one into readable layout data (size, tile and object palettes, ASCII
layer maps).

**These are studied, not pasted.** They are built from vanilla tiles and
objects, which the Skyreach does not use; what they teach is composition —
room sizes, where doors go, the rhythm lighting is placed on, how furniture is
grouped rather than scattered, how much of a footprint is left open. Our POIs
get rebuilt from our own set with those proportions.

| file | size | what it teaches |
|---|---|---|
| `stubborn-garden.script` | 25×25 | a walled compound: an open sand courtyard at the centre, a ring of planted beds behind stone kerbs, four gated entrances on the axes, columns marking the corners. Almost half the footprint is deliberately empty. |
| `iceberg.script` | 25×25 | a tiny encounter on open water: 159 of 625 tiles are land, the rest is sea. One inhabited cluster of about a dozen objects, a handful of scattered outlying rocks, and a single narrative prop (a sack, a quill and parchment) that carries the whole story. |
| `warden-tower-layout.script` | 21×21 | **the plan the Warden's Spire is being rebuilt to.** A double wall ring with a circulation corridor between them and one large empty chamber in the middle; 104 wall tiles, 8 doors on the axes (four outer, four inner), 12 candelabra spaced regularly. All the furniture — 5 modular tables, 3 bookshelves, 2 bench pairs, 2 dressers, a clock, a display, a desk, a chair, a cabinet — lives in the corner pockets of the corridor. The centre stays open. |
| `cosy-cabin.script` | 11×9 | a complete liveable home in 99 tiles: bed, dresser, a carpet, a modular table with chairs, a log bench, an alchemy table, a banner, three windows and one door — the smallest useful NPC dwelling, and a good template for a single-settler house. |

The Iceberg is the more useful of the two for the Skyreach, because the
Skyreach *is* islands in a sea: it shows how little land an encounter needs
when the placement is deliberate, and that one readable prop beats a room full
of furniture.
