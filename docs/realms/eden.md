# Garden of Eden — implemented core

Status: `[run]` for dimension registration and terrain generation; `[build]`
for the five hostile mobs and the first material loop. The dedicated-server
integration test generates a 161×161 area and requires all three biomes, Eden
terrain, dimension `+2`, and the mob/item registrations to exist.

## What ships

- `eden2`, an infinite region-streamed `EdenLevel` at dimension `+2`.
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

## Deliberately still open

The three POI lattices are described by the terrain/pressure code but no Eden
presets are registered yet. There is no player-facing Skyreach↔Eden gate,
Eden Press, farming family, livestock, fishing, Keeper boss or Eden quest
chapter yet. Those are not hidden behind a `[run]` label: they remain future
content even though the realm itself now generates.
