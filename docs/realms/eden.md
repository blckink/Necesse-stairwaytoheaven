# Garden of Eden — implemented core

Status: `[run]` for dimension registration and terrain generation; `[build]`
for the five hostile mobs and the first material loop. The dedicated-server
integration test generates a 161×161 area and requires all three biomes, Eden
terrain, dimension `+2`, and the mob/item registrations to exist.

## What ships

- Eden is a BAND of the one sky plane (`skylevel` / `skyreach2`), depth 0.10-0.48, roughly 600-2880 tiles from SkyOrigin. It is not a dimension. See `docs/PLAN_ONE_PLANE.md`.
- Eden Garden, Eden Canopy and Eden Shallows.
- Eden Grass (the supplied sheet), Rich Eden Soil, Eden Moss, Root Floor,
  Paradise Sand and shallow lagoon water.
- Eden Serpent, Bloom Maw, Jealous Vine, Golden Hornet and Forbidden Serpent.
- Eden Copper Ore → Eden Bronze at a Tungsten Workstation, plus the mobs' and
  caches' first material/fruit drops.
- A deterministic pressure field: broad calm ground, louder authored-site and
  Knowledge-Tree neighbourhoods.

## Borrowed stand-ins

No new bitmap art was created. Terrain uses the literal vanilla sheets listed
in `docs/VANILLA_ASSET_MAP.md`. Vegetation is made from vanilla registry
objects (`grass`, `swampgrass`, flower patches, fruit trees, berry bushes,
palms, reeds, seashells, `dryadtree`, `rock`, `ivyoreswamp`) so their native
renderer, tool response and drops stay intact. Eden item icons borrow literal
vanilla item paths; the five hostile bodies borrow `crocodile`, `stabbybush`,
`dryadsentinel`, `bee` and `dragonwhelp`.

## Settler and quest chain

Eveleen the Eden Botanist (`eveleensettler`) stands beside a Knowledge Tree,
one per world, and also travels to a settlement once Eden grass grows in it
(`SkyArrivals.EDEN_PATCH`). `EdenArrivalQuest` (handed out on first use of the
Eden Gate) and `EdenPlantsQuest` (an Eden Berry, a Moon Melon and a Sun Grape,
handed to Eveleen) are her two-step chain; completing it waives her recruit
fee and pays out Knowledge Cuttings plus Stormsteel bars. See
`docs/settlers.md` and `docs/quests.md`.

## Deliberately still open

The three POI lattices are described by the terrain/pressure code but no Eden
presets are registered yet. The Eden Gate now exists — an `EdenSeedBasinObject`
(the Eden Threshold) seeded with 6 Eden Grass Seeds grows into it, the same
shape the Ghost Gate uses — but the Eden Press, farming family, livestock,
fishing and Keeper boss remain future content even though the realm itself now
generates.
