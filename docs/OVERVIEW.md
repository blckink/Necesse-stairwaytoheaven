# Stairway to Heaven — what exists, where, and what is playable

Read off the CODE and off a live headless run, not off the other docs.
Version 0.6.0 · Necesse 1.3.2 · `master` @ this commit.

**This is the only status document.** `STATUS.md` and `CURRENT_STATE.md` were
deleted; three files each claiming to be the current state is how an agent ends
up building against a stale one.

Legend: **PLAY** = the integration test proves a player reaches it ·
**IN** = in the world, not covered by a test · **DEAD** = registered, nothing
reaches it · **TODO** = not built.

---

## 1. The world is ONE plane

`LevelRegistry.registerLevel("skylevel", SkyLevel.class)` at
`StairwayToHeavenMod.java:140` is the ONLY modded level. Every realm from
Skyreach to Hell is a depth BAND on it, chosen per tile by
`worldgen/RealmDepth` from the distance to the Old Warden Spire. Bands overlap;
that is what dissolves the hard borders `WORLD_DESIGN` §3 forbids. The law is
`docs/PLAN_ONE_PLANE.md`.

VERIFIED [run] — live from the headless server:

```
Skyreach OK: class=SkyLevel identifier=skyreach2 dimension=1 isCave=false
realm check: scale=6000  0=skyreach 1800=eden 3200=steinfeld
             4000=ghostrealm 5200=crookedbeyond 5800=hell
outlands check: floor=4200 inside=0/66231 biome=Beetle Outlands
```

| realm | depth band | tiles from origin | sub-biomes | painter | state |
|---|---|---|---|---|---|
| **Skyreach** | 0.00–0.30 | 0–1800 | Driftlands, Stormveil, Skyway, Aurora Shoals | `SkyTerrainPainter` | **PLAY** |
| **Eden** | 0.10–0.48 | 600–2880 | Eden Garden, Eden Shallows, Eden Canopy | `EdenTerrainPainter` | **IN** |
| **Steinfeld** | 0.32–0.70 | 1920–4200 | Quiet Meadow, Slab Fields, Grave Heath | `SteinfeldTerrainPainter` | **IN** |
| **Ghost Realm** | 0.48–0.88 | 2880–5280 | Aftergarden, Bone Orchard, Ectomarsh + **Gloomfen, Ashen Reach** (ex-Veil) | `GhostTerrainPainter` | **IN** |
| **Crooked Beyond** | 0.70–0.94 | 4200–5640 | Checkerworks, Spiral Fields, Striped Waste + **Beetlefreak Hollow** (ex-Veil) + Beetle Outlands rim | `CrookedTerrainPainter` | **IN** |
| **Hell** | 0.80–1.00 | 4800–6000+ | Infernal Fringe, Furnace Reach | Infernal Clerk, Ash Spirit, Ticket Imp, Boiler Hound; Mutant Hydra at the portal | **works**, on its own two grounds `cinderash`/`furnaceslag` since 2026-09-10 (no materials, no demon NPCs) |

20 biomes registered. The Veil is no longer a world: its three biomes moved
into the Ghost and Crooked bands per §41.5.

**Waterline.** One island field, per-realm waterlines blended by realm weight
(`SkyTerrainPainter.REALM_WATERLINE`: Skyreach 0.48, Eden 0.40, Steinfeld 0.34,
rest 0.44) so no band border shows a coastline step.

**Anti-rush.** `veil/SoulExposureBuff` + `VeilRegion` + `VeilGate`. There is
exactly **one** wall: the Veil at `VeilRegion.VEIL_DEPTH` (derived, 0.581 at
today's bands, ~3 486 tiles out) between Steinfeld and the Ghost Realm. Standing
past it without the Veil Mark stacks a named debuff: vision → slow → health
drain → heavy damage. Eden and Steinfeld have no barrier of their own. The check is against the world REGION, not the tile, so a teleport
past the edge does not help (§8's abuse case).

**Travel.** The Séance Circle is fast travel to the Ghost band
(`RealmLanding.find`), not a door to another world. The Warden's-house anchors
of §A2.3 are **TODO**: `edengateup`, `ghostgateup`, `crookeddoorup` and
`veilriftup` are registered and nothing places them.

---

## 2. POIs — 26 presets, all wired into worldgen

None is orphaned: every `Preset` subclass has a live call site.

| preset | realm | contains | furnished? |
|---|---|---|---|
| `WardenSpirePreset` | Skyreach hub | 3 tables (2 + 1 dining), 11 chairs + 4 benches, 1 bed, 2 desks, 2 dressers, **15 lights**, 10 banners, 5 railings, statues, carpet, chalice, tome, beacon | **furnished** — the hub, untouched by the 2026-09-24 pass |
| `HauntedManorPreset` | Ghost | the Headless Butler's manor: dining hall with the long table laid for guests on a velvet rug (dinner table ×2 pairs, 5 chairs), tea corner, sideboard; master bedroom (bed, dresser, clock, rug); study (desk + chair, shelves, display, the bone chest). 4 lights | **furnished** — plan, 2026-09-24 |
| `InvertedHousePreset` | Crooked | sealed masonry core; the rooms laid out around it on rugs: bedroom (bed, dresser), kitchen (pot, counter, pantry barrel, sack), parlour (dinner table + 6 chairs, 2 long chairs as sofas, ground window), bathtub + toilet in the open, clock, 2 lanterns | **furnished** — plan |
| `LongTablePreset` | Crooked | **a real table**: 17 bone tables in one line laid with a rotten feast (17 decorations), 34 long chairs down both sides, the one ordinary chair at the head, the barrel behind it, 4 lanterns, 2 clocks, 2 ground windows | **furnished** — plan |
| `DoorYardPreset` | Crooked | 11 free-standing doors, each with a red doormat; the Doorman's desk + chair, side table, bench, hinge crate; 3 long chairs queueing at the centre door; barrel, 4 lanterns, clock | **furnished** — plan |
| `MausoleumPreset` | Ghost | family altar (bone chest between 2 urns, 2 wall candles), 4 crypt columns, the sarcophagus, **2 two-tile crypt coffins** (were one-tile halves until 2026-09-24), 2 candle pedestals, 4 graves outside | **furnished** — plan |
| `CrookedHousePreset` | Beetlefreak Hollow | the watcher's house: dinner table for six, bed, dresser, shelves, clock between 2 ghost lanterns, kitchen (pot, candelabra, cabinet, barrel, sack), 2 ravens (one at the east window on a rug) | **furnished** — plan |
| `AeronautCampPreset` | **Surface** | wreck, balloon, tent, campfire with a bench, camp table with the log + chair, repair anvil, silk sack, chest, barrel, parcels, windbreak, sign | **furnished** (code-built, 2026-09-24 additions) |
| `SkywardShrinePreset` | **Surface** | seraph on its plinth, offering table with a candle, 2 pilgrim benches facing it, 2 lamps, railings, crystals, broken ring | **furnished** (code-built) |
| `GraveyardPreset` | Steinfeld | walk to the mourner on a plinth between 2 candle pedestals, 8 graves each with a flower, visitors' bench, gravedigger's chest + stone pile, 2 lanterns; **loot now in a chest** (it was on a `skycrate`, which has no inventory) | **furnished** — plan |
| `RuinedChapelPreset` | Steinfeld | angel between 2 candle pedestals, altar table with dark-gold chalices, 2 mourners, lectern, 7 pews + 1 crushed under a slab, 8 columns, weeds on pale grass, sacristy chest (loot moved off the `skycrate`) | **furnished** — plan |
| `SunkenGraveyardPreset` | Ghost | cryptpath walk to the raised centre: chest between 2 grave candles, an open coffin (grave goods), 3 grave rows, the old north row with its mourner, mourners' bench, withered shrubs, 2 lanterns at the gate | **furnished** — plan |
| `SkyFragmentCraterPreset` | **Surface** | ore, rocks, crystals, starfall, strongbox; a prospector's oil lantern and sample sack on the rim | natural site — **one human trace** (deliberately no furniture, §0.5) |

**Furniture reality (2026-09-24):** all 13 carry furniture that says what the
place is for; 9 of them are now `RealmPoiPresets.plan` maps, so their windows,
both halves of every bed/bench/dinner table/coffin, every chair's facing and
every table decoration are checked when the registries close
(`onRegistryClosed` of each realm's world preset builds them once). `[run]`:
a temporary all-layer census stamp (every object layer, not only layer 0)
applied each of the twelve to dry Skyreach ground and read back
`missing=0` for all of them (`hauntedmanor placed=108/108` … `skyfragmentcrater
placed=19/19`). `[game]`: nobody has looked at one in a client yet.

**The ten old code-built catalogue kinds are plans too (2026-09-24)** — no two
furnished alike, each a named place with a use. Same footprints, same
ordinals.

| kind | what it is now | furnished? |
|---|---|---|
| `skytower` | the Skywatch chapter house: assembly hall with pews and banners, refectory (kitchen + 2 dinner tables), library, acolytes' dormitory (5 beds), chart room (map table, cartographer's table, astrolabe, telescope), warden's study, observatory, the seraph's lantern; every door in a wall | **furnished** — plan |
| `edencrowngarden` | head gardener's L-house (kitchen for 8, bedroom, potting room), the Crown (seed basin in a flower ring, 4 trees of plenty, 2 benches, one gate), fenced orchard of fruit bushes on farmland, seed-keeper's house (porch, seed store, room) | **furnished** — plan |
| `edenfermenthouse` | fermenting hall (4 kegs, barrels, 2 pots, counter, sacks), tasting room (table for 8, cabinet, clock, 2 displays), vintner's office (desk, shelves, bed), seed basin in a flower bed outside | **furnished** — plan |
| `steinfeldmemorial` | fallen angel on a dark-gold plinth with 4 candle pedestals; memorial flower bed, two grave rows with flowers, pilgrims' rest (bench, offering table + chair), mason's corner (slabs, chest, stone pile); 4 columns, 4 lanterns | **furnished** — plan |
| `ghostarchive` | reading hall (stacks, soul basin on a rug, 2 reading tables for 12), catalogue (card cabinets, clerk's desk, crystal ball), archivist's rooms (bed, dresser, clock, desk, cabinet, barrel); **`badwindows` 6 → 0** (they were beetle windows in nightfell walls) | **furnished** — plan |
| `crookedbazaar` | the Doorman's counter (free-standing doors for sale), the clockmaker, the grocer (shop floor + store room), 2 market stalls on the street; `badwindows` 2 → 0 | **furnished** — plan |
| `hellborderoffice` | Border Office 666-B: the road runs through it on a red runner past 2 counters with 4 clerks, cabinets, a stopped clock; waiting benches, the 400-year skeleton, a ticket machine; records room with the secure chest | **furnished** — plan |
| `helladministration` | four departments round the cross: Eternal Waiting (4 counters, 6 bench rows, skeletons, ticket machine), Records & Seals (5 double stacks, seal table), Moxie's canteen (kitchen, serving counter, 4 tables of 6), the Director (throne behind his desk on velvet, banners) + clerks' dormitory (8 bunks) | **furnished** — plan |
| `hellforge` | Brim's forge: 5 forges, 6 anvils, fuel barrels and skull encasings, 6 armour stands, 4 dummies; shop counter to the public way, 4 weapon stands, tungsten anvil, stock chests, the hands' table for 12; Brim's quarters | **furnished** — plan |
| `hellcarnival` | carousel (jail-fence ring, 4 gates, 8 sheep-chairs round the lamp), Moxie's food stall + picnic tables, strength/shooting booth with plush prizes, fortune teller, Vex's contraband (stolen Skywatch goods, a Seraph) | **furnished** — plan |

`[run]`, 2026-09-24: forced stamps of the nearest queued place of each of the
ten read `missing=0` on the census's layer-0 comparison and on the temporary
all-layer one (e.g. `kind=helladministration ... placed=591/591 missing=0`,
all layers `707/707`). The census's window ratchet is paid off: every kind is
now held to `badwindows=0`.

**The inhabited catalogue adds 27 presets, and they are no longer all the same
rarity.** `RealmPoiWorldPreset` places **twenty-four** of them on the lattice —
fifteen in Skyreach, two in Eden, one each in Steinfeld/Ghost/Crooked, four in
the reserved Hell band. The other **three are once per world**
(`SkyLandmarkPois`, 2026-09-10): the Skyway Toll-House (§2.12), the Grange
Cellar (§2.13) and the Test Range (§2.14) are stamped lazily off
`SkyLevel.ensureWardenSpire`, from a seed-derived site in a stated distance band
and biome, and remembered in `SkywatchQuestData.landmarksStamped`. That is
§0.6's third rarity, and it had never been built: a kind on the lattice IS
§0.6's "common" row by definition — one designed place per 72×72 cell, two cells
in three — and the toll-house rode it from 2026-09-09 to 2026-09-10, so a world
stood up four ledger rooms for a Magpie there is one of. The large sites include actual street networks, buildings
beside rather than on those streets, non-rectangular room unions, doors,
windows, dense functional furniture and clear circulation. Full catalogue and
review rules: `docs/design/realm-poi-worldgen.md`.

**Sky Town, Sky Inn and Sky Toll Bridge are plans since 2026-09-23** (the
player: "keine schönen angeordneten Häuser … zu oft wiederholte Anordnungen").
They were the last Skyreach kinds built from `building()` rectangles plus the
same `furnishHome` six pieces in every house; now `TOWN_PLAN`, `INN_PLAN` and
`TOLL_BRIDGE_PLAN` go through `plan()` and its §0.2–0.4 checks. Five trades in
the town (bakery, store, scholar, weaver, family), each multi-room; an inn with
two guest rooms, kitchen and a 16-seat common room; two different toll houses.
Footprints unchanged (57×41, 17×15, 31×23). **VERIFIED [run]** on the dedicated
server: `realmpoi stamp: kind=skytown … placed=557/557 missing=0`, and with a
one-off local census tweak (not committed) `kind=skyinn … placed=128/128
missing=0` and `kind=skytollbridge … placed=92/92 missing=0`; `badwindows=0`
for all three, so their old one-window debt is gone from the integration
ratchet. **Not seen in a client** — no screenshot yet.

**All fourteen of the dossier's designed places are now built**
(`docs/design/chapter-01-skyreach-pois.md`). The
Skyway Toll-House (§2.12) was transcribed into `setObject` calls by hand on
2026-09-09. The Skywatch Wayside (§2.1), the Dew-Keeper's Hut (§2.11), the
Shepherd's Fold (§2.2), the Institute of Applied Falling (§2.3), the Passage
Wayhouse (§2.7), and — on 2026-09-10 — the Nightfell Redoubt (§2.4), the Aether
Manufactory (§2.5), the Sovereign's Anvil (§2.6), the Unopened Gate (§2.8), the
Prism Choir (§2.9) and the Serpent's Reef (§2.10)
are not: `RealmPoiPresets.plan(Preset, String[], Legend)` reads the dossier's ASCII
map character for character and implements the dossier's §0.2–§0.4 rules **once**
— both halves of a multi-tile piece, wall decor on `WALL_DECOR` and never on
masonry, table decorations only on a real `TableObjectInterface`, a chair turned
toward its table, a window only mid-run in a straight wall, no lone fence post.
Three more rules landed with §2.4–§2.6, which the earlier plans did not exercise:
a per-tile rotation (`Legend.turns`, because both those buildings hang ONE
lantern character on all four inner faces of their shell), a carpet on
`TILE_LAYER` (`Legend.rug`, §2.5's 65-tile runner) and a scattered formation
(`Legend.scatter`, §2.6's 92-tile rim at 55% coverage — writing a rock on all 92
would seal the arena, since `RockObject` is solid).
§2.8 added one more (`Legend.paves`, the ground under a character that writes
none of its own — the Gate stands five pieces INSIDE its chequer dais, and
without it each would punch a skyway-paved hole in the one accent surface it
has).
Every breach throws at load, because `onRegistryClosed` builds all twenty-seven
kinds; each rule was confirmed to fire by breaking it and booting a server.
`tools/plan_transcription_audit.py` proves the arrays in the code are still
character-identical to the sections in the dossier. §2.13 the Grange Cellar and
§2.14 the Test Range landed on 2026-09-10 and closed the list; both are
plan-built, and with them Magpie, Halda and Ossian each stand in a place of
their own. Every §4 piece of
new art the eleven built ones ask for — the
steles, `cloudspringfont`, `skywaywaystone`, Wren's wardrobe, `sovereignaltar`,
`prismchime`, `reefmaw`, the Skywatch Revenant, the Fulgur Shade and the three
`sovereignshard` — is left
out rather than faked; each is named in the preset's own javadoc. Without the
altar the Anvil is its rim, its ring and its Seraphs and no wave fight, which is
what §2.6 asks for by name. The Prism Choir pays the same price hardest: its
seven `prismchime` and its `cloudspringfont` are ALL of its centrepiece and all
of its light, so §2.9 ships today as a ring, a floor, four gates and a pedestal,
with the lap-the-ring puzzle and Sovereign Shard III still on the art queue.
§2.9 offers a fallback (chimes built from `aurorabloom` + `starfall` + a
pedestal each) and it is deliberately NOT taken: that is three objects on one
tile, which no plan cell can hold. **`[run]`, not `[game]`.**

**Chapter 02 adds six guarded-treasure places (2026-09-24), kinds 27–32**
(`docs/design/chapter-02-hoards-and-mimics.md`): the Counterfeit Treasury and
the Fallen Observatory (Skyreach), the Hedge Labyrinth (Eden), the Pilgrims'
Ossuary (Steinfeld), the Wedding Feast (Ghost Realm) and the Hall of Many Doors
(Crooked Beyond). All plan-built; the catalogue is now **thirty lattice
kinds** plus the three landmarks. The four outer ones sit in their band's row;
the two Skyreach ones on a sparser second lattice of their own
(`RealmPoiWorldPreset.SKY_HOARD_KINDS`, 16% of cells), because adding them to
the Skyreach row diluted its ~36 rank-dealt sites until the Sky Tower stood
nowhere on seed 1486191071. Each one is
a prize chest behind a guardian (that realm's elite, lifted by
`BossScaling.applyTier` to tier 5: ×2.12 HP, ×1.52 damage) and boxes around it
of which some are mimics: the new `hoardmimic` (vanilla `MimicMob` on the
Skyreach elite row, lifted onto the Eden/Steinfeld/Ghost rows where it stands),
or the realm's own `doormimic`/`possessedchair`. Everything is placed once, at
generation, by `RealmPoiHoards`, read off the same plan rows the preset is
built from. The Ossuary is the first place in the mod with vanilla pressure
plates wired to arrow traps; the Observatory the first with a room that has no
door. `/skyreachstatus pois` stamps each kind's nearest site and prints a
`realmpoi hoard` line (cast, mimics, mimic loot, guardian lifted, chests
filled); the integration test fails on any of them short. **`[run]`, not
`[game]`.**

**And they now stand in the world — counted, not assumed.** From 2026-09-04 to
2026-09-07 they were registered and largely absent, and no gate looked: the POI
counts in `scripts/integration_test.sh` were the SURFACE catalogue's, a
different system. `/skyreachstatus pois` (`RealmPoiCensus`) walks the whole
realm disc through the placement decision itself and then through the preset
regions the world really built, and the integration test fails on anything less
than 24/24 — the three once-per-world places are counted separately, by the
`realmpoi landmark` lines, which check the same thing plus the one question a
lattice census cannot ask: is the PERSON in it. Measured over six seeds on 2026-09-07: **13/13 on all of them**,
~1,450 places in a 6144-tile disc, nearest one **126–430 tiles** from the
arrival pad. On 2026-09-10, seed 1524002983: **25/25 accepted and 25/25
queued**, 1,439 places, nearest one 155 tiles out. `[run]`, not `[game]` —
nothing has looked at one yet. Before the
fix the same census read 11/13 and 419 tiles.

**The census stamps the Serpent's Reef by name, on top of the nearest place.**
Every other kind stands on ground the terrain painter already made, so one
force-generated stamp answers the same question for all of them. §2.10 is the
only kind placed in OPEN MISTSEA, painting its own 123 land tiles into 625 of
water, and "does an object survive on a tile whose neighbour the same preset has
just turned into sea" is a question only it asks. It was worth asking: the first
run of that gate found **14 of the Reef's 30 objects swept away** between the
preset writing them and the census reading them back. See
`docs/TECHNICAL_LEARNINGS.md` — two separate engine rules, both now respected.

**How a kind gets a cell changed on 2026-09-10, and it was a real defect.**
Outside Skyreach the rotation is a per-cell hash; that spreads the mix without
guaranteeing it. Skyreach is the one band bounded by its own depth — `RealmDepth`
gives it weight only below depth 0.30, i.e. 1800 tiles — and it now carries
thirteen kinds in the **38 sites** the band offers. Thirty-eight draws over
thirteen bins leave a bin empty about half the time, and on seed 1524204744 two
kinds stood nowhere in the world at all (`passagewayhouse`, `sovereignsanvil`),
with `badground=0` everywhere: the catalogue gate had been a coin flip since the
band passed ten kinds, failing on arithmetic rather than on a defect in a place.
`RealmPoiWorldPreset.skyreachRotate` now COUNTS instead of hashing — whether a
cell holds a site and which realm it falls in are pure functions of the seed, so
the band's sites can be walked in scan order and cell *n* takes kind `n % kinds`.
Measured after: every one of the thirteen holds **2–4 accepted sites**. What it does not check is whether
a footprint's INTERIOR is solid: `validSite` samples nine points, so a 57×41
Sky Town can still straddle Mistsea between them.

**The census grew two assertions on 2026-09-09**, because "queued" was still
too weak a word. `presetobjects=` counts what each kind's preset really carries
— only the NEAREST place is force-generated, so fifteen kinds could have
resolved to empty rectangles and every number stayed green — and `badwindows=`
asks `WallWindowObject.getWindowDir` about every window each preset places. The
stamp now compares the generated world with the preset tile by tile
(`placed=N/M missing=K`) instead of counting non-zero objects. The first thing
that found: **the Sky Tower had shipped two windows since 2026-09-04 that the
engine deleted on sight**, both on the row where the nave's own rectangle turns
the transept's north wall into interior floor. Fixed by moving them to the
transept's south wall; the gate now fails on `badwindows` above 0 and on
`missing` above 0 for any kind.

**Skyreach's band is thin, and seven kinds now share it.** The census disc
holds only ~35 candidate cells in the home realm against ~780 in Hell, so each
Skyreach kind lands 1–9 times per world. On one seed in six, the 49×55 Sky
Tower — the kind whose ground test is hardest to pass — drew zero and the gate
went red. Nothing about the placement rules changed for it; the band is simply
under-supplied for the number of kinds in it, and the dossier's "common"
(§0.6, one per 72×72 road cell) is a different lattice from the 220-tile one
these ride.

Eden therefore has two buildings now. Its older Knowledge Grove, Lagoon Shrine
and Orchard Ring cells still remain pressure/terrain sites rather than presets.

---

## 3. NPCs — 10 named residents + Dorian + 2 cats (+ 4 profession/visitor NPCs, see `KOMPLETTUEBERSICHT.md` §3.14)

| who | realm / where found | recruit | shop | quest |
|---|---|---|---|---|
| **The Warden** | Skyreach, Old Warden Spire (stamped on first ascent) | 30 000 coins | after settling: Ghost Chalk @1 200; once earned on the world, the chain rewards — Wolkengleve @4 500, Skywatch Banner @800 (anchor done), Cat Basket @500, Flickerlight Garland @500 (both cats home) | the whole Warden's Call chain |
| **Eveleen**, Eden Botanist | Eden, beside a Knowledge Tree (0.35/region + tree) | 7 000 → **free** after her quest | seeds, saplings, fertiliser, queen bee | `swh_edenplants` |
| **Mortimer**, Undertaker | Ghost, beside a gravestone | 8 000 → **free** after his quest | gravestones, sarcophagus, Bonewood furniture | `swh_mortimerrites` |
| **Caspern**, Spirit Smith | Ghost, beside a gravestone | 14 000 → **free** after his quest | Nightsteel ore/bar, phantom dust, bone arrows | `swh_caspernforge` |
| **Eleanor**, Lost Soul | Ghost, beside a gravestone | 5 000 (STAY only) | flowers, lanterns | `swh_eleanor`, two endings |
| **Mr. Knott**, Doorman | Crooked, at a Door Yard | 22 000 | void cube, runestone, 3 masks | `swh_crookeddoor` |
| **Magpie** | Skyreach, **Skyway Toll-House** (once per world, guarded by the Tollwright) | 12 000 **+ Bonded Lockbox** | buys sky salvage above broker | — |
| **Halda**, Cellarer | Skyreach, **the Grange Cellar** (once per world, guarded by the Sourvat Bloom) | 9 000 **+ The Mother** | the mod's 3 crafted materials | — |
| **Ossian Vane** | Skyreach, **the Stormveil Test Range** (once per world, guarded by Prototype Nine) | 18 000 **+ Storm Lens Core** | rotating incursion-exclusive loot (3 of 8) | — |
| **Ives**, Verger of the Quiet Reach | **Steinfeld**, beside a broken angel | 11 000 → **free** after his quest | the realm's four materials (buys), gravestones/candles/urn/stone fence + Pale Stone (sells) | `swh_steinfeldvigil` |
| **Spire Cats** ×2 | Skyreach lairs | not recruitable | — | objective of `swh_cats` |
| **Dorian**, the Nightbound | nowhere in the world — arrives when a coffin stands in the settlement | 11 000 | none | — |

**Arrivals.** Eveleen, Mortimer and Caspern also travel to the settlement the
vanilla way once a condition is met (9+ Eden tiles / 3+ gravestones / an Aether
Forge). Eleanor, Knott, Magpie, Halda, Ossian and Ives never travel — they must
be found. Ives has no arrival gate because Steinfeld offers no settlement
condition one could key off, the same call Eleanor and Knott make.

**No generic settlers.** Every human this mod adds is a unique, named,
one-per-world individual. There is no "a farmhand arrives" event of its own.

---

## 4. Quests — 20 registered, 19 live (rewards retuned 2026-09-24, full table in `docs/quests.md`)

| id | giver | do | reward |
|---|---|---|---|
| `swh_findspire` | first ascent | find the Spire | signpost only |
| `swh_recruitwarden` | Warden | pay 30 000 | the Warden |
| `swh_cats` | Warden | coax both cats home with Cloudpuff Treats | cat basket, 2× flickerlight garland, 10× stormsteel bar |
| `swh_anchor` | Warden | 20× aetherium bar, 80× skystone, 8× stormsteel bar | **Wolkengleve** (its only source), Skywatch banner, 5× aurora petal. Worlds that anchored earlier get the glaive once on their next word with the Warden |
| `swh_edenreach` | Eden Gate | find Eveleen | signpost only |
| `swh_edenplants` | Eveleen | 1× Eden berry, moon melon, sun grape | 3× knowledge cutting, 10× stormsteel bar, **her fee waived** |
| `swh_eleanor` | Eleanor | PASS ON with 12× veil essence, or recruit | PASS ON: will-o'-wisp lantern + 10× spiritsteel bar (she is deleted **permanently**) · STAY: 10× spiritsteel bar |
| `swh_crookedarrival` | Crooked Door | find Knott | signpost only |
| `swh_crookeddoor` | Knott | 5× reality shard, 8× warp resin, 8× strange fabric | Zephyr Harness, 12× spiritsteel bar, 6× reality shard |
| `swh_keyskyreach` … `swh_keyhell` | Warden, after the Call is DONE | two materials of that realm, one realm at a time in boss-ladder order | key piece + 4 / 5 / 6 Stormsteel, 6 / 8 / 16 Spiritsteel, plus a weapon and a wearable on the first five (4× Stormdisc, Galehowl, Tempest Edge, Spiritsteel Reaver, Gravewind Bow). Standing the piece in a settlement wakes that realm's boss portals |
| `swh_steinfeldvigil` | **Ives** | 14× grave salt, 10× spirit moss | his 11 000 fee waived, 12× stormsteel bar |
| `swh_mortimerrites` | **Mortimer** | 12× soul thread, 10× bonewood | his 8 000 fee waived, 6× spiritsteel bar |
| `swh_caspernforge` | **Caspern** | 12× spectral ore, 8× veil essence | his 14 000 fee waived, 6× spiritsteel bar |
| `swh_beacon` | **nobody** | — | — · **DEAD**: registered, never handed out; kept only so pre-0.5 saves deserialize |

---

## 5. Settler jobs — what the mod actually adds

**The honest mechanism, VERIFIED [jar].** Necesse 1.3.2 registers 12 job types
(`JobTypeRegistry.java:23-34`). Exactly **five** carry
`defaultDisabledBySettler = true` — those five ARE "professions" in the sense of
"only this settler can do it":

| vanilla profession | vanilla carrier | the mod's carrier |
|---|---|---|
| `fertilize` | `FarmerHumanMob` | **Eveleen** |
| `husbandry` | `AnimalKeeperHumanMob` | **Eleanor** (STAY ending) |
| `fishing` | `AnglerHumanMob` | **Halda** |
| `hunting` | `HunterHumanMob` | **Mortimer** |
| `tradingmission` | `TraderHumanMob` | **Magpie** and **Knott** |

The mod flips exactly the same flag vanilla does
(`SkySettlerMob.enableProfession`, `mobs/SkySettlerMob.java:110`), and
`refuseJob` is the Guard's move in reverse — Mortimer, Caspern and Knott refuse
farming and forestry because those characters would not do it.

**Correction, 2026-09-03.** An earlier version of this section said "all five
profession slots the engine offers are already taken, there is no sixth" and
"there is no miner profession in vanilla". **Both were wrong.** They came from
reading only `JobTypeRegistry` and treating a settlement work-priority as the
whole of a profession. It is one of FIVE separate mechanisms vanilla uses, and
the other four are all open to a mod:

| # | mechanism | vanilla example | open to a mod? |
|---|---|---|---|
| 1 | **Work priority** — a withheld `JobType` | Farmer fertilises | **taken**: all 5 in use |
| 2 | **Expeditions** — `ExpeditionMissionRegistry` | **Miner** is sent on mining trips; **Explorer** on cave trips; **Angler** on fishing trips; **Trader** on trading missions | **YES, wide open** |
| 3 | **A unique player-facing service** | **Mage** enchants trinkets/weapons; **Stylist** restyles you; **Elder** gives quests and sells revival potions | **YES** |
| 4 | **Stats** | **Guard** carries +500 base HP and its own `getRegenFlat` | **YES** |
| 5 | **Shop inventory + recruit gate** | every NPC type | **YES**, and the mod already uses it |

**Mechanism 2 is the one that matches "only the miner works the mine".**
`ExpeditionMissionRegistry` is a `GameRegistry<SettlerExpedition>` with a fully
public API — `registerExpedition`, `registerMinerExpedition`,
`registerMiningTrip`, `registerFishingTrip`, `registerExplorerExpedition`
(`ExpeditionMissionRegistry.java:184-214`). Exclusivity is
`HumanMob.canDoExpedition(SettlerExpedition)`: `MinerHumanMob` returns true only
for ids in `miningTripExpeditionIDs`, and `getPossibleExpeditions()` builds that
NPC's own menu. A new sky profession is therefore: **one `SettlerExpedition`
subclass + one settler that is the only one who returns true for it.** Nothing
in the engine stands in the way. The mod registers none today.

**VERIFIED [run]**, live job lists from the headless server:

```
skywarden      30000  crafting,farming,forestry,hauling
wardensettler   free  crafting,farming,forestry,hauling
magpiesettler  12000  crafting,farming,forestry,hauling      (+ tradingmission, UI-hidden)
haldasettler    9000  crafting,farming,FISHING,forestry,hauling
ossiansettler  18000  crafting,hauling
eveleensettler  7000  crafting,farming,FERTILIZE,forestry,hauling
mortimersettler 8000  crafting,hauling,HUNTING
caspernsettler 14000  crafting,hauling
eleanorsettler  5000  crafting,farming,forestry,hauling,HUSBANDRY
```

`tradingmission` does not appear in that list because vanilla gives it
`JobType(canChangePriority=false, …, displayName=null)` — it has no job row in
the settlement UI at all, for vanilla's Trader as much as for ours. The
capability is real; the UI row is not.

### The three workstations

They are NOT professions: all three implement `SettlementWorkstationObject`, and
`LevelJobRegistry` files every workstation job under the shared vanilla
**crafting** priority — the same bucket as vanilla's forge and cheese press. Any
settler with crafting on can staff any of them.

| station | what a settler makes on it |
|---|---|
| **Windsilk Loom** | wind wheat → windsilk (2:1), windsilk → Skyweave cloth |
| **Aether Forge** | aetherium ore → bar (2:1, better than vanilla's 3:1); ore + storm shard → **Stormsteel bar** (the mod's only source) |
| **Stormglass Kiln** | fulgurite + skystone → Stormglass panes |

---

## 6. The things that DO something

These are the objects with real behaviour, as opposed to deco and furniture.

| object | where it works | what it does |
|---|---|---|
| **Séance Circle** | drawn from Ghost Chalk, only inside a settlement | Summons the Ghost Guide: the first use lifts the Ghost band's Soul Exposure for that player, every use after he trades ghost weapons (FOGKEY A2/A3). Not a teleporter any more. |
| **Soul Basin** | craft + place anywhere | The Ghost Realm's counterpart. Wants **12× ectoplasm and CONSUMES it** — that is the price of the door. Opens the way to the Aftergarden. |
| **Eden Seed Basin** | craft + place anywhere | The Eden Threshold. **6 seeds** grow a way into the Garden. Built because Eden had a settler and a quest chain and no door. |
| **Skywatch Gate** | fixed at the Spire | The only way home. Routes each player back to the **surface stairway they personally ascended from** (per-player server-side binding). Unbreakable — mining your way home must not be possible. |
| **Skyward Stairway** | surface | The ascent. First use stamps the Warden Spire and gives `swh_findspire`. |
| **Warden Beacon** | Spire | Lights on recruiting the Warden. |
| **Aether Forge** · **Windsilk Loom** · **Stormglass Kiln** | settlement | The three settler-operable workstations — see §5. |
| **Spirit Forge** · **Soul Loom** | settlement | Ghost Realm stations (Spiritsteel bar/armour; Soul Thread). Had **no source at all** until 2026-09-24; now Workstation recipes from Ghost-Realm-only materials (Bonewood, Ectoplasm, Spectral Ore / Soul Thread). IMPLEMENTED — awaiting player confirmation; recipe verified registered (`swhcat item spiritforge … recipe=workstation`, VERIFIED [run]). |
| **Eden / Ghost / Crooked gates** | band borders | Move you between bands on the plane, destination computed by `RealmLanding`, not by a dimension change. Their **return halves** (`edengateup`, `ghostgateup`, `crookeddoorup`, `veilriftup`) are registered and **placed by nothing**. |

**Categories and natural drops (2026-09-24).** Every mod item now sorts into a
real vanilla category (server gate `swhcat census: … bad=0`), and ten pieces of
worldgen scenery — dead tree, scree, rubble, three crystals, withershrub, two
reeds and Eden grass — break into material (or vanilla's wild-grass drop)
when the WORLD placed them, and into themselves only when a player did.
`docs/ITEM_CATEGORIES.md`. VERIFIED [run] on the server; FIXED — awaiting
player confirmation in the client.

**Silver Bell** is retired (2026-09-24): its door, the Veil Rift, went with
PLAN_ONE_PLANE. Nothing gives, sells or drops it; it stays registered only so
bells already in a save still load, with a keepsake tooltip.

### Equipment and materials, in one glance

94 items, 119 objects, 60 mobs registered.

**Point 8n (2026-09-10) added eight of those items and the mod's first art on
its own residents.** Five band trophies — `skystoneheart` (Skyreach, 12% off a
Skystone Golem), `bloomfang` (Eden, Bloom Maw), `mourningband` (Steinfeld,
Stone Mourner), `soulcollar` (Ghost, Mourning Bride, the one line
`GhostLoot.bride()` adds over `elite()`), `stripedhorn` (Crooked Beyond, 20%
off a Door Mimic) — craft into nothing and exist to be carried home, which
Magpie pays over broker for. Hell has none: 4n's uplift table, which is what
"per realm band" names, lists five bands. And Magpie, Halda and Ossian Vane
now wear `magpiecap`, `haldakerchief` and `vanecowl` instead of vanilla's
`trapperhat`, `battlechefhat` and `runichat` — recoloured from those very
sheets, so the silhouette the player knows is unchanged and only the art is
ours. Each sells their spare. `[run]`, not `[game]`.

| kind | what ships |
|---|---|
| **Weapons (5 craftable + 2 past Stormsteel)** | Skyreave (glaive) · Thunderhead (greatbow) · Prismcaller (magic staff) · Skywatch Whistle (summon) · Stormdisc (melee). Plus Tempest Edge (sword) and Galehowl (ranged). **Wolkengleve** (glaive, 165, reward of `swh_anchor` only) and **Himmelslanze** (beam lance, 95/tick, crafted from Aetherwright's Casing + Eden Bronze) — both VERIFIED [jar] registered and built; their load-time white-gold recolour of vanilla `cryoglaive`/`dragonlance` is HYPOTHESIS until seen on a client. |
| **Armour** | Stormsteel helmet / chestplate / boots · Glimmerstrides (boots) · Skywatch Hood, Warden Mantle, Warden Boots (cosmetic) |
| **Trinkets (3)** | Aurora Locket (Crooked key) · Stormsteel Vambrace (Ghost key) · Zephyr Harness (Knott's quest) — all three also craftable |
| **Bars & ore** | Aetherium ore → Aetherium bar · **Stormsteel bar** (Aether Forge only) · Nightsteel (Caspern's shop) · Spiritsteel (Spirit Forge from spectral ore + ectoplasm; also Eleanor's quest and the Ghost Guide's shop) |
| **Minerals** | Skystone · Storm shard · Stormglass · Fulgurite · Prism shard · Cinder pearl |
| **Woods (5)** | Cloudwood · Nimbuswood · Prismwood · Seraphwood · Charwood — all register as `anylog`, so vanilla recipes accept them. Eden's **Edenwood** carries `anylog` too since 2026-09-24 (`EdenRealm.registerItems`) |
| **Cloth** | Windsilk → Skyweave (Windsilk Loom) |
| **Mob drops** | Aurora fleece (Glimmergoat shear) · Veil essence · Dewsnail |
| **Livestock** | **Nimbus Yak** — milk, no shear · **Glimmergoat** — shear for aurora fleece, no milk. Both eat cloudberry and wheat, by hand or trough. |
| **Food** | Nimbus milk → cheese press · Cloud custard · Sky curd · Nimbus draught · Cloudberry · Cloudpuff Treat (the cats' bait) |
| **Quest item** | — (Silver Bell retired 2026-09-24) |
| **Unique rewards (8)** | One per place, none of them a bigger number on an existing item. Toll-House: **Bonded Lockbox** (Magpie's key) · **Skyway Writ** · **Ledger of Undelivered Post**. Grange Cellar: **The Mother** (Halda's key, Sourvat Bloom loot) · **The Warden's Round** · **Skywatch Signet** (trinket, reveals sky structures). Test Range: **Storm Lens Core** (Vane's key, Prototype Nine loot) · **Aetherwright's Casing** ×2–4. Six sit in containers, two drop off the guard on **every** kill. All eight borrow a vanilla icon — see `VANILLA_ASSET_MAP.md` §1.7. |

---

## 7. Sprites — ours vs. vanilla

**389 PNGs ship with the mod**: 143 items · 107 objects · 38 mobs + 31 bestiary
icons · 21 tiles · 16 art/supplied · 14 armour · 5 weapons · 5 particles ·
4 projectiles · 3 map icons · 2 statues · 2 carpets · 1 preview.

The remaining literal Vanilla `GameTexture.fromFile` paths include:

```
mobs/cow  mobs/scorpion
mobs/icons/{blacksmith,exoticmerchant,farmer,pawnbroker,stylist}human
tiles/{cryptash,ravenfloor,stonebrickfloor,swampgrass,swamprock}_splat
```

The locale audit adds: **210 holdable IDs have a real icon file**, 57 of them
recoloured from vanilla art and checked against the dump.

**Where the art gap is worst:** Steinfeld has **zero tile art of its own** —
all eight of its ground surfaces are either a Skyreach/Eden splat reused or a
bare vanilla path. Eden's five non-Garden surfaces are all vanilla
(`sand`, `mud`, `ancientroots`, `overgrowngrass`, `saltwater_*`), and Crooked's
five (`ascendedcorruption`, `ascendedgrowth`, `deepstonetiledfloor`,
`ascendedvoid`, `ooze`) likewise. The full shopping list with exact pixel sizes
is `docs/ASSET_REQUESTS.md`.

---

## 8. What is missing — ranked by what it costs the player

**Correction, 2026-09-03.** An earlier version of this list opened with
"the two farm animals spawn nowhere". **That was wrong** and is retracted.
`SkyLevel.placeLivestockHerds` (`SkyLevel.java:231-243`) places both:

| animal | ground | chance/region | herd |
|---|---|---|---|
| Nimbus Yak | cloudturf (Driftlands) | `YAK_REGION_CHANCE = 0.085F` | 2–5, persistent |
| Glimmergoat | aurorashoal (Aurora Shoals) | `GOAT_REGION_CHANCE = 0.070F` | 2–5, persistent |

Livestock deliberately does NOT ride a spawn table — `placeLivestockHerds`'s own
comment says why, quoting the player: *"wertvolle Tiere nicht an jeder Ecke"*.
It follows vanilla's `GenerationTools.spawnMobHerds` instead. The audit that
produced the false claim grepped for the quoted literals `"nimbusyak"` /
`"glimmergoat"`; `placeHerd` is called with the CONSTANTS
`SkyLivestock.NIMBUS_YAK` / `.GLIMMERGOAT`, so the grep missed the one call site
that matters. **A grep for a string literal is not proof that a thing is
unreachable.** The player had both animals in their base at the time.

What DOES still follow: `nimbusmilk`, `aurorafleece`, `skycurd`, `cloudcustard`,
`nimbusdraught` and `glimmerstrides` are reachable, since their animals are.

1. **Hell is half built** (§17–23). The band has its own painter, two sub-biomes, four hostiles one rung past Crooked Beyond, a region key and a boss portal that wakes the Mutant Hydra at 528 000 HP. Since 2026-09-10 it also has the two grounds of its own that 6n deferred — `cinderashtile` and `furnaceslagtile`, stamped through `ashsand`'s and `blackpeat`'s alpha rather than drawn, so the Veil's and the Gloomfen's tiles keep flooring the Ghost band and Crooked Beyond unchanged. What it still lacks is §20's six materials, §19's three friendly demons and Clerk 666-B, §21's machines, §23's crops and §25's Auditor; its loot is Crooked Beyond's currency in Hell's quantities until they land. One
   `case` in `SkyTerrainPainter.java:1071` to delete once a painter exists.
2. ~~**Eden gets no guard packs.**~~ **FIXED 2026-09-05.** `placeGuardPacks`
   had branches for four realms and none for Eden, so `EdenGardenBiome`,
   `EdenCanopyBiome` and `EdenShallowsBiome`'s `getGuard()` were dead code —
   while `EdenPressure` had already been written with the discs for them. Three
   `placePacksOf` calls on Eden's own grove / lagoon / orchard lattices closed
   it: 0 → 11.4 guarded sites per 1000x1000 (`docs/AREA_OVERVIEW.md`). An
   existing save needs `/swhreset world` to see them.
3. **The original 13 POIs remain thinly furnished.** The new inhabited
   catalogue is dense, but the older shells listed in §2 are unchanged.
4. **The Warden's-house travel anchors are unplaced** (§A2.3). Four gate objects
   registered, nothing places them; no route is fast travel yet.
5. ~~**Magpie, Halda and Ossian are near-unfindable.**~~ Resolved 2026-09-10:
   each now stands in a landmark the world stamps exactly once (Skyway
   Toll-House, Grange Cellar, Stormveil Test Range). The old workstation route
   still exists alongside (`SkyLevel.placeResident`).
6. **`distortion` is threaded to every band painter and read by none.** §3's
   calm/mad variants do not exist yet.
7. **3 of 10 named settlers have no `interact()` of their own**: Magpie, Halda
   and Ossian. (Mortimer and Caspern have one since 2026-09-05, with
   `swh_mortimerrites` and `swh_caspernforge`.) (All ten do have talk lines --
   `talkKey()` is on the base class -- so "no dialogue" was always too strong;
   what they lacked was anything to DO.)
8. **`swh_beacon` is a registered dead quest**, kept only for old-save
   deserialization.
9. **Two realms' materials have no RECIPE.** Crooked's six (`oddwood`,
    `warpresin`, `strangefabric`, `eyeseed`, `stripedshell`, `realityshard`) and
    Steinfeld's four (`palestone`, `gravesalt`, `spiritmoss`, `echoshard`) are
    named by no recipe anywhere — neither realm even defines a
    `registerRecipes()`. All four Steinfeld materials now have quest or shop
    DEMAND (`swh_steinfeldvigil` takes grave salt and spirit moss,
    `swh_keysteinfeld` takes echo shard and pale stone, Ives buys all four),
    which is not the same thing as being craftable with.
10. **Five items have no source at all.** `skyanchor` is registered unbreakable
    and placed by nothing. `skywatchhood`, `wardenmantle` and `wardenboots` are
    obtainable-flagged armour with no recipe, shop or gift — they exist only as
    what the Warden NPC wears.
11. **`LongTablePreset` contains no table.** Thirty-four chairs, zero tables — its
    own comment concedes the joke.
12. **`DoorYardPreset`'s javadoc says eleven doors; its `PLAN` grid stamps eight.**
    Doc drift inside the same file as the code.

### Harmless leftovers, recorded so nobody re-discovers them

`SkyRegistry` still declares five `LevelIdentifier` constants from the
pre-one-plane design (`eden2`, `steinfeld2`, `ghost2`, `crooked2`, `veil2`).
None is registered with `LevelRegistry`; three are passed into vanilla
`LadderDownObject` super-constructors in a parameter slot whose value each
gate's `ObjectEntity` then overrides with `SKYREACH_IDENTIFIER` plus a
`RealmLanding` tile. Dead, not broken.

---

## Gates

```
./gradlew buildModJar                                        exit 0
python3 tools/locale_audit.py --vanilla vanilla-sprites      356 IDs, locales in sync
python3 tools/content_ledger.py --check                      350 IDs, 0 undescribed
python3 tools/tile_behaviour_audit.py --vanilla vanilla-sprites
                                       42 tiles, 1619 splat cells in the vanilla bands
python3 tools/asset_generator/generate_assets.py             exit 0, no diff
scripts/integration_test.sh                                  exit 0, 0 FAIL
```

`--vanilla vanilla-sprites` is mandatory on both audits that take it. Without
the dump they report every borrowed texture as missing — 27 phantom errors once.
