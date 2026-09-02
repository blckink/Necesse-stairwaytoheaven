# Steinfeld / The Quiet Reach

Verification state: **built**. The classes compile against Necesse 1.3.2 and
the repository audits (locale, content ledger, tile behaviour) cover every
registration. Runtime world generation remains to be proven by the
integration test.

`docs/WORLD_DESIGN.md` §7 and A3.4 are the brief: **"the place where the sky
stops working properly."** Near the gate the ground is still Eden's own green
and the sky's own bright stone; walk out and the grass pales, the stone
dulls, the trees die and the fog comes in. Everything below is that gradient,
computed as one field in `SteinfeldTerrainPainter` rather than laid out by
hand.

## Playable core

- Level `steinfeldlevel`, identifier `steinfeld2`, dimension **+3** — between
  the Garden of Eden (+2) and the Ghost Realm (+4) on the mod's own ladder.
  Not a cave: it follows the world's day/night light, and the fog is the
  ground's own colour rather than darkness.
- Biomes: **Quiet Meadow** (inner), **Slab Fields** (middle), **Grave Heath**
  (outer) — three concentric bands radiating from a fixed gate point
  (`SteinfeldTerrainPainter.ORIGIN_X/Y`), painted per tile into the level's
  own biome layer rather than grown from vanilla's biome weights, with a
  low-frequency warp so the border between bands is a coastline, not a
  bullseye.
- Four hostile roles, one of each archetype on `SteinfeldTier`'s own ladder
  (realm row 2100 HP / 200 damage / 50 armour, drop value x1.6): **Lost
  Pilgrim** (fast), **Stone Mourner** (standard, the ladder's unmodified
  floor), **Hollow Angel** (elite), **Grave Crow** (ranged).
- Two authored landmarks — **Graveyard** and **Ruined Chapel** — hand-laid
  `Preset`s on their own rare lattice (`SteinfeldSites`), independent of
  `SteinfeldTerrainPainter`'s own organic POI system, which grows a grave
  field, a ruined chapel or a statue field at every site it rolls (roughly
  one per 230x230 tiles).
- Guard packs placed at world generation around every POI, organic and
  hand-authored alike (`SteinfeldLevel.placeGuardPacks`), per
  `docs/WORLD_DESIGN.md` A4.1: the walk between places is quiet
  (`SteinfeldPressure` returns 0 tickets across most open ground), arriving
  at one is not (600 tickets on the ground a POI stands on).
- Economy: **Pale Stone** (building), **Grave Salt** (alchemy), **Spirit
  Moss** and **Echo Shard** — the two ingredients §9's séance quest (A CALL
  TO THE OTHER SIDE) already asks for, so neither sits unspent.
- Fog, not weather: `SteinfeldLevel.canRain` and `SteinfeldBiome.canRain` are
  both hard `false`. Rain over a dead heath would read as life.

## Borrowed visual sources

No image is generated or recoloured. These names are literal engine resource
paths — this mod's own where noted, otherwise vanilla's — and are also
recorded in `docs/VANILLA_ASSET_MAP.md`.

| what it is | source path | what it stands in for |
|---|---|---|
| Pale Grass | `tiles/cloudturf_splat` (this mod, Skyreach) | the middle band's grass, the sky's own turf gone pale |
| Weathered Stone | `tiles/skystone_splat` (this mod, Skyreach) | bright heaven-stone lying in the open near the gate |
| Cracked Heaven Marble | `tiles/stonetiledfloor_splat` (vanilla) | the big buried slabs the Slab Fields are named for — registered as TERRAIN, not the floor vanilla built it as |
| Dead Soil | `tiles/dirt_splat` (vanilla) | bare earth where the grass has given up |
| Ash Grass | `tiles/murkmoss_splat` (this mod, the Veil) | the outer band's grey-green mat, on the ground the realm leads into |
| Mist Stone | `tiles/rock_splat` (vanilla) | the same stone as Weathered Stone, dulled where the fog sits on it |
| Grave Soil | `tiles/cryptash_splat` (this mod, Ghost Realm) | near-black turned earth, what a grave field is cut into |
| Withered Tuft | `objects/witheredgrass` (vanilla) | the realm's commonest scatter, in all three bands |
| Pale Reed | `objects/skyreeds` (this mod, Skyreach) | Slab Fields reed-grass on open soil |
| Widow Flower | `objects/cragbloom` (this mod, Ghost Realm's Bone Orchard) | the single flower near Eden, A3.4's own image |
| Dead Heaven Bloom | `objects/aurorabloom` (this mod, Skyreach) | a dying sky-flower of the Slab Fields |
| Ghost Mushroom | `objects/gloomshroom` (this mod, Ghost Realm / the Veil) | pale fungus of the Grave Heath |
| Spirit Moss Patch | `objects/staticmoss` (this mod, Skyreach) | the séance material growing wild — the only one of the five flora with a real drop |
| Pale Stone Rock | `objects/skystonerock`, `items/skystonerock` (this mod, Skyreach) | the realm's mineable building stone, on vanilla's `RockObject` |
| Grave Salt Rock | `objects/veilrock`, `items/veilrock` (this mod, the Veil) | the realm's mineable alchemy mineral, on vanilla's `RockObject` |
| Mourner Statue | `objects/statues/mossymonkstatue` (vanilla) | a statue that has not woken yet — the Stone Mourner's still image |
| Broken Angel | `objects/statues/seraph` (this mod, Skyreach's Seraph Statue) | the realm's central image, named directly in §7 and A3.4 |
| Chapel Column | `objects/cryptcolumn` (vanilla) | a ruined chapel's colonnade pillar |
| Heaven Slab | `objects/skywatchrubble` (this mod, Skyreach) | a fallen roof slab, scattered through open country and both landmarks |
| Grave Fence *(alias, not a new registration)* | `objects/cryptfence` (vanilla) | the walled plot's wall — the same sheet Ghost Realm's own Sunken Graveyard uses |
| Gravestone *(alias, not a new registration)* | `objects/cryptgravestone1` (vanilla) | a lone gravestone — reusing vanilla's own `GravestoneObject` directly is what makes it a real container: `GravestoneObject.getLootTable` answers `level.getCrateLootTable` for any stone the player did not place |
| Graveyard gate *(used locally by `GraveyardPreset`)* | `objects/cryptfencegate` (vanilla) | the walled plot's single gap in the wall |
| Second gravestone variant *(used locally by `GraveyardPreset`)* | `objects/cryptgravestone2` (vanilla) | alternates with the aliased Gravestone so a grave field reads as graves, not tiled floor |
| Pale Stone (item icon) | `items/cryptstone` (vanilla) | a chunk of building stone |
| Grave Salt (item icon) | `items/alchemyshard` (vanilla) | a mineral crust for alchemy |
| Spirit Moss (item icon) | `items/phantomdust` (vanilla) | the séance material, drawn ghostly rather than plant-green |
| Echo Shard (item icon) | `items/pearlescentshard` (vanilla) | a pale, shard-shaped fragment of an apparition |
| Lost Pilgrim (body) | `mobs/deepcavespirit` (vanilla, `DeepCaveSpiritMob` base) | a small hooded, translucent, floating figure — the mod's one "person who stopped being alive but kept the shape" |
| Stone Mourner (body) | `mobs/ancientarmoredskeleton` (vanilla, `AncientArmoredSkeletonMob` base) | a rigid, faceless figure in full plate — the closest vanilla body to "a statue that still walks" |
| Hollow Angel (body) | `mobs/crystalgolem` (vanilla, `CrystalGolemMob` base) | a tall crystalline construct with a charge-and-beam attack — also the mod's own measured tier-1 floor |
| Grave Crow (body) | `mobs/crazedraven` (vanilla, `CrazedRavenMob` base) | a black corvid firing a feather spread — the closest vanilla body to a crow |

## Deferred

- **The world-event ghosts** (§7, A3.4) — transparent, unattackable figures
  that walk to a grave, a door, or the map edge. Needs its own invulnerable,
  untargetable, destination-seeking mob archetype, which is a real feature in
  its own right and not one of this pass's four residents. `SteinfeldLevel`
  leaves the hook out cleanly rather than half-wired.
- **A player-facing entry object.** Exactly like the Garden of Eden — the
  dimension directly below this one on the same ladder — Steinfeld is
  registered as a dimension index and a level class and nothing walks the
  player there yet. Building a "Fallen Gate" here while Eden still has none
  would not make Steinfeld more reachable than the realm underneath it; both
  stay deferred together (see `SkyRegistry.java`'s own note at the end of the
  Steinfeld block).
- **A crafting station and recipe economy.** Pale Stone, Grave Salt, Spirit
  Moss and Echo Shard are loot and mining drops today, the same state Crooked
  Beyond's own six materials shipped in. Spirit Moss and Echo Shard already
  have a real consumer — §9's séance quest — so nothing here is unspent by
  design; a Steinfeld-side recipe list is future work, not a hole.
- **Custom realm art.** Every entry in the table above remains on the
  player's later replacement list, exactly like every other realm's borrowed
  table.
