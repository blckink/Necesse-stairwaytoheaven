# Realm POIs — implementation contract

Status: **implemented, server-verified; client visual review pending**.

All structures generate on `skyreach2`. Realms remain bands of the same level.
`RealmPoiWorldPreset` queues them on Necesse's `villages` collision board, so
they only enter unexplored regions and cannot overlap another reserved POI.

## Catalogue

| Realm | In-game preset | Size | Layout purpose |
|---|---|---:|---|
| Skyreach | Skyrealm Arch Tower | 49×55 | stepped arch silhouette, central aisle, furnished wings |
| Skyreach | Wolkenhain district | 57×41 | road cross, plaza, pond/bench, four irregular occupied parcels |
| Skyreach | Cloudstream toll bridge | 31×23 | 3-wide bridge continues the road across a 5-wide stream |
| Skyreach | Last Updraft Inn | 17×15 | L-plan, two entrances, tables/chairs, counter, kitchen, bedroom |
| Eden | Crown Garden hamlet | 45×35 | road cross, two homes, large planted clearing, seed shrine |
| Eden | Fermentation house | 19×17 | bent workhouse, dining area, barrels, storage, seed basin |
| Steinfeld | Memorial court | 23×23 | four-way path, monument, graves, columns and lights |
| Ghost | Lantern archive | 25×21 | irregular three-wing archive, reading tables, shelves and basin |
| Crooked | False-door bazaar | 27×21 | three separated shops; no meaningless door pile |
| Hell | Border Office 666-B | 23×19 | through-route, desks, archive, secure storage |
| Hell | Infernal administration | 61×45 | four furnished wings around a public street cross |
| Hell | Brim forge block | 29×23 | public approach, anvils, forge station and stores |
| Hell | Hell carnival | 39×31 | street cross, fenced central attraction and four stalls |

## Placement and layout rules

- Reserve roads before buildings. A building footprint never occupies a road.
- Put buildings beside roads and connect each exterior door by a 1–3 tile path.
- Keep at least one orthogonal tile clear from every entrance to every room.
- Connect adjacent rooms with doors; distribute windows symmetrically and not
  on corners.
- Mix rectangular rooms with L/T/stepped unions. Landmarks may be symmetric;
  ordinary homes should not all be boxes.
- Chairs face their table. Inns need tables, chairs, counter, work area,
  storage and sleeping space rather than decorative emptiness.
- Bridges continue the same road over the complete liquid width. The toll
  bridge only queues where both stream edges are liquid and both road ends land.
- Large footprints pass a 3×3 land sample before queuing. All sites stay at
  least 100 tiles from the Warden Spire and share the existing collision board.
- Hell currently uses the documented Crooked terrain fallback. Its four POIs
  are real and explorable, but their surrounding biome remains provisional
  until the Hell painter exists.

## Review gate for Claude

Check the client view at 1× scale: roof readability, entrance visibility,
furniture orientation, path continuity and whether all rooms are reachable.
Fix layout/placement code in `RealmPoiPresets`; do not solve a layout problem by
adding a new level or generating replacement art.
