# Realm POIs — implementation contract

Status: **implemented, server-verified; client visual review pending**.

All structures generate on `skyreach2`. Realms remain bands of the same level.
`RealmPoiWorldPreset` queues them on Necesse's `villages` collision board, so
they only enter unexplored regions and cannot overlap another reserved POI.

## Catalogue

| Realm | In-game preset | Size | Layout purpose |
|---|---|---:|---|
| Skyreach | Skyrealm Arch Tower | 49×55 | ASCII plan (`TOWER_PLAN`, 2026-09-24): the Skywatch chapter house — assembly hall, refectory, library; dormitory, chart room, warden's study; observatory; the seraph's lantern; every door in a wall, axis clear |
| Skyreach | Wolkenhain district | 57×41 | ASCII plan (`TOWN_PLAN`, 2026-09-23): road cross into a square round a pond with four benches; bakery, general store, scholar's house, weaver's house and family home, each multi-room and furnished for its trade; two one-gate gardens |
| Skyreach | Cloudstream toll bridge | 31×23 | ASCII plan (`TOLL_BRIDGE_PLAN`, 2026-09-23): railed 3-wide deck across a 5-wide stream; toll office north, keepers' quarters south |
| Skyreach | Last Updraft Inn | 17×15 | ASCII plan (`INN_PLAN`, 2026-09-23): two guest rooms and a kitchen behind a common room seating sixteen, bar with kegs and stools |
| Skyreach | Skyway Toll-House | 23×19 | dossier §2.12, transcribed by hand: weighing hall, ledger room (Magpie), vault |
| Skyreach | Skywatch Wayside | 11×9 | dossier §2.1, read from its ASCII plan: paved pocket, balustrade, benches, offering cabinet |
| Skyreach | Dew-Keeper's Hut | 13×13 | dossier §2.11, read from its ASCII plan: one-room dwelling, two windows, snail run with 5 Dew Snails |
| Eden | Crown Garden hamlet | 45×35 | ASCII plan (`CROWN_PLAN`, 2026-09-24): lane cross; head gardener's L-house (kitchen, bedroom, potting room); the Crown (seed basin in a flower ring, one gate); fenced orchard on farmland; seed-keeper's house (porch, seed store, room) |
| Eden | Fermentation house | 19×17 | ASCII plan (`FERMENT_PLAN`, 2026-09-24): fermenting hall (kegs, barrels, pots), tasting room (table for eight), vintner's office, seed basin outside |
| Steinfeld | Memorial court | 23×23 | ASCII plan (`MEMORIAL_PLAN`, 2026-09-24): four paths to a fallen angel on a dark-gold plinth; flower bed, grave rows, pilgrims' rest, mason's corner |
| Ghost | Lantern archive | 25×21 | ASCII plan (`ARCHIVE_PLAN`, 2026-09-24): reading hall (stacks, soul basin, reading tables), catalogue, archivist's rooms; windows now nightfell, `badwindows` 6 → 0 |
| Crooked | False-door bazaar | 27×21 | ASCII plan (`BAZAAR_PLAN`, 2026-09-24): the Doorman's counter, the clockmaker, the grocer with store room, two street stalls |
| Hell | Border Office 666-B | 23×19 | ASCII plan (`OFFICE_PLAN`, 2026-09-24): the road runs through on a runner past two counters; waiting benches, the 400-year skeleton, ticket machine, records room |
| Hell | Infernal administration | 61×45 | ASCII plan (`ADMIN_PLAN`, 2026-09-24): Eternal Waiting, Records & Seals, Moxie's canteen, the Director + clerks' dormitory round the street cross |
| Hell | Brim forge block | 29×23 | ASCII plan (`FORGE_PLAN`, 2026-09-24): public way through the hall; forge floor (forges, anvils, armour stands, dummies), shop counter, Brim's quarters |
| Hell | Hell carnival | 39×31 | ASCII plan (`CARNIVAL_PLAN`, 2026-09-24): sheep-chair carousel in a gated ring; food stall, prize booth, fortune teller, Vex's contraband |

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
