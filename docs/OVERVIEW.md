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
| **Hell** | 0.80–1.00 | 4800–6000+ | — | **falls back to Crooked** | **TODO** |

20 biomes registered. The Veil is no longer a world: its three biomes moved
into the Ghost and Crooked bands per §41.5.

**Waterline.** One island field, per-realm waterlines blended by realm weight
(`SkyTerrainPainter.REALM_WATERLINE`: Skyreach 0.48, Eden 0.40, Steinfeld 0.34,
rest 0.44) so no band border shows a coastline step.

**Anti-rush.** `veil/SoulExposureBuff` + `VeilRegion` + `VeilGate`. Standing in
a band you have not earned stacks a named debuff: vision → slow → health drain →
heavy damage. The check is against the world REGION, not the tile, so a teleport
past the edge does not help (§8's abuse case).

**Travel.** The Séance Circle is fast travel to the Ghost band
(`RealmLanding.find`), not a door to another world. The Warden's-house anchors
of §A2.3 are **TODO**: `edengateup`, `ghostgateup`, `crookeddoorup` and
`veilriftup` are registered and nothing places them.

---

## 2. POIs — 13 presets, all of them placed

None is orphaned: every `Preset` subclass has a live call site.

| preset | realm | contains | furnished? |
|---|---|---|---|
| `WardenSpirePreset` | Skyreach hub | 4 tables, 4 chairs + bench, 2 beds, 2 desks, 2 dressers, 9 lights, banner, statues, carpet, beacon | **richest POI in the mod** |
| `HauntedManorPreset` | Ghost | 2 tables, 4 chairs, 2 candelabra, bone chest | table + chair + light |
| `InvertedHousePreset` | Crooked | 4 long chairs, 2 lanterns, clock, window, barrel | chair + light |
| `LongTablePreset` | Crooked | **34 chairs**, 4 lanterns, 2 clocks — and **zero actual tables** | chair + light |
| `DoorYardPreset` | Crooked | 11 free-standing doors, 2 bent lanterns, clock | light only |
| `MausoleumPreset` | Ghost | 4 columns, coffin, 4 candles, 2 urns, 4 gravestones | light only |
| `CrookedHousePreset` | Beetlefreak Hollow | 3 ghost lanterns, 2 raven statues, rubble | light only |
| `AeronautCampPreset` | **Surface** | wreck, balloon, 4 tents, campfire, chest, lantern | light only |
| `SkywardShrinePreset` | **Surface** | seraph statue, 2 lamps, railings, crystals | light only |
| `GraveyardPreset` | Steinfeld | grave fence, mourner statue, 2 gravestones, crate | **empty shell** |
| `RuinedChapelPreset` | Steinfeld | broken angel, chapel column, heaven slab, crate | **empty shell** |
| `SunkenGraveyardPreset` | Ghost | crypt fence, 2 gravestones, bone chest | **empty shell** |
| `SkyFragmentCraterPreset` | **Surface** | ore, rocks, crystals, starfall, chest | **empty shell** |

**Furniture reality:** 9 of 13 stamp at least one table/chair/bed/light — but a
table appears in only **2**, a bed in only **1** (the Spire), and 5 of the 9 are
"furnished" by a light and nothing else. `SkyFurnitureSet`'s 17 pieces are still
almost unused outside the Spire.

**Eden has no preset at all.** Its three POI cells (Knowledge Grove, Lagoon
Shrine, Orchard Ring, `EdenTerrainPainter.java:100-113`) are read only by
`EdenPressure.siteDistance` to raise spawn tickets. Guarded ground with nothing
standing on it — the only one of the four new realms without a building.

---

## 3. NPCs — 9 named humans + 2 cats

| who | realm / where found | recruit | shop | quest |
|---|---|---|---|---|
| **The Warden** | Skyreach, Old Warden Spire (stamped on first ascent) | 30 000 coins | 2× Silver Bell @5 000, after settling | the whole Warden's Call chain |
| **Eveleen**, Eden Botanist | Eden, beside a Knowledge Tree (0.35/region + tree) | 7 000 → **free** after her quest | seeds, saplings, fertiliser, queen bee | `swh_edenplants` |
| **Mortimer**, Undertaker | Ghost, beside a gravestone | 8 000 | gravestones, sarcophagus, Bonewood furniture | — |
| **Caspern**, Spirit Smith | Ghost, beside a gravestone | 14 000 | Nightsteel ore/bar, phantom dust, bone arrows | — |
| **Eleanor**, Lost Soul | Ghost, beside a gravestone | 5 000 (STAY only) | flowers, lanterns | `swh_eleanor`, two endings |
| **Mr. Knott**, Doorman | Crooked, at a Door Yard | 22 000 | void cube, runestone, 3 masks | `swh_crookeddoor` |
| **Magpie** | Skyreach, beside a mod workstation | 12 000 | buys sky salvage above broker | — |
| **Halda**, Cellarer | Skyreach, same | 9 000 | the mod's 3 crafted materials | — |
| **Ossian Vane** | Skyreach, same | 18 000 | rotating incursion-exclusive loot (3 of 8) | — |
| **Spire Cats** ×2 | Skyreach lairs | not recruitable | — | objective of `swh_cats` |

**Arrivals.** Eveleen, Mortimer and Caspern also travel to the settlement the
vanilla way once a condition is met (9+ Eden tiles / 3+ gravestones / an Aether
Forge). Eleanor, Knott, Magpie, Halda and Ossian never travel — they must be
found.

**No generic settlers.** Every human this mod adds is a unique, named,
one-per-world individual. There is no "a farmhand arrives" event of its own.

---

## 4. Quests — 10 registered, 9 live

| id | giver | do | reward |
|---|---|---|---|
| `swh_findspire` | first ascent | find the Spire | signpost only |
| `swh_recruitwarden` | Warden | pay 30 000 | the Warden |
| `swh_cats` | Warden | coax both cats home with Cloudpuff Treats | cat basket, 2× flickerlight garland, 10× stormsteel bar |
| `swh_anchor` | Warden | 20× aetherium bar, 80× skystone, 8× stormsteel bar | Skywatch banner, 5× aurora petal, Stormsteel Vambrace |
| `swh_edenreach` | Eden Gate | find Eveleen | signpost only |
| `swh_edenplants` | Eveleen | 1× Eden berry, moon melon, sun grape | 3× knowledge cutting, 10× stormsteel bar, **her fee waived** |
| `swh_eleanor` | Eleanor | PASS ON with 12× veil essence, or recruit | PASS ON: will-o'-wisp lantern + 14× spiritsteel bar (she is deleted **permanently**) · STAY: 14× spiritsteel bar |
| `swh_crookedarrival` | Crooked Door | find Knott | signpost only |
| `swh_crookeddoor` | Knott | 5× reality shard, 8× warp resin, 8× strange fabric | Zephyr Harness, 12× stormsteel bar, 6× reality shard |
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

**Two consequences worth stating plainly:**

1. **All five profession slots the engine offers are already taken.** There is
   no sixth. A genuinely NEW job (a "Skywright", a "Bell-ringer") would need a
   `JobTypeRegistry.registerType` call plus its own `LevelJob` handler — the mod
   makes none today.
2. **There is no miner profession in vanilla 1.3.2.** The registry has no
   `mining` type; `expeditions` exists but is NOT withheld, so every settler can
   already run one. A "Bergarbeiter der als einziger die Mine nutzt" is not a
   thing the base game has.

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
| **Séance Circle** | craft + place anywhere | **On the surface, holding the Silver Bell** (checked, never consumed — it is a key you keep): the ring tears open into a Veil Rift and lands you in the **Ghost band** via `RealmLanding.find`. **In the Beetle Outlands**: it becomes a Crooked Door instead — the boss-portal site. **Already at Ghost depth or beyond**: it tells you there is nowhere left to send you. **Anywhere else in the sky**: silent. |
| **Soul Basin** | craft + place anywhere | The Ghost Realm's counterpart. Wants **12× ectoplasm and CONSUMES it** — that is the deliberate difference from the Circle: the bell is a key, ectoplasm is a price. Opens the way to the Aftergarden. |
| **Eden Seed Basin** | craft + place anywhere | The Eden Threshold. **6 seeds** grow a way into the Garden. Built because Eden had a settler and a quest chain and no door. |
| **Skywatch Gate** | fixed at the Spire | The only way home. Routes each player back to the **surface stairway they personally ascended from** (per-player server-side binding). Unbreakable — mining your way home must not be possible. |
| **Skyward Stairway** | surface | The ascent. First use stamps the Warden Spire and gives `swh_findspire`. |
| **Warden Beacon** | Spire | Lights on recruiting the Warden. |
| **Aether Forge** · **Windsilk Loom** · **Stormglass Kiln** | settlement | The three settler-operable workstations — see §5. |
| **Eden / Ghost / Crooked gates** | band borders | Move you between bands on the plane, destination computed by `RealmLanding`, not by a dimension change. Their **return halves** (`edengateup`, `ghostgateup`, `crookeddoorup`, `veilriftup`) are registered and **placed by nothing**. |

**Silver Bell** is the Warden's gift and the Séance Circle's key. It is
`misc.questitems` and is the only mod item whose whole job is to be carried.

### Equipment and materials, in one glance

75 items, 106 objects, 55 mobs registered.

| kind | what ships |
|---|---|
| **Weapons (5, all craftable)** | Skyreave (glaive) · Thunderhead (greatbow) · Prismcaller (magic staff) · Skywatch Whistle (summon) · Stormdisc (melee). Plus Tempest Edge (sword) and Galehowl (ranged). |
| **Armour** | Stormsteel helmet / chestplate / boots · Glimmerstrides (boots) · Skywatch Hood, Warden Mantle, Warden Boots (cosmetic) |
| **Trinkets (3)** | Aurora Locket · Stormsteel Vambrace (Anchor quest) · Zephyr Harness (Knott's quest) |
| **Bars & ore** | Aetherium ore → Aetherium bar · **Stormsteel bar** (Aether Forge only) · Nightsteel (Caspern's shop) · Spiritsteel (Eleanor's quest) |
| **Minerals** | Skystone · Storm shard · Stormglass · Fulgurite · Prism shard · Cinder pearl |
| **Woods (5)** | Cloudwood · Nimbuswood · Prismwood · Seraphwood · Charwood — all register as `anylog`, so vanilla recipes accept them |
| **Cloth** | Windsilk → Skyweave (Windsilk Loom) |
| **Mob drops** | Aurora fleece (Glimmergoat shear) · Veil essence · Dewsnail |
| **Livestock** | **Nimbus Yak** — milk, no shear · **Glimmergoat** — shear for aurora fleece, no milk. Both eat cloudberry and wheat, by hand or trough. |
| **Food** | Nimbus milk → cheese press · Cloud custard · Sky curd · Nimbus draught · Cloudberry · Cloudpuff Treat (the cats' bait) |
| **Quest item** | Silver Bell |

---

## 7. Sprites — ours vs. vanilla

**350 PNGs ship with the mod**: 129 items · 96 objects · 29 mobs + 26 bestiary
icons · 21 tiles · 16 kk-sprites · 11 armour · 5 weapons · 5 particles ·
4 projectiles · 3 map icons · 2 statues · 2 carpets · 1 preview.

**55 literal `GameTexture.fromFile` paths**, of which **38 resolve to our own
files** and **17 to the game's own resources** — one flat resource map serves
both, so a literal path is not evidence of borrowing:

```
mobs/bee  mobs/cow  mobs/crocodile  mobs/dragonwhelp  mobs/dryadsentinel
mobs/scorpion  mobs/stabbybush
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

1. **Hell is not built** (§17–23). The 0.80–1.00 band paints as Crooked. One
   `case` in `SkyTerrainPainter.java:1071` to delete once a painter exists.
2. **Eden gets no guard packs.** `SkyLevel.placeGuardPacks` has branches for
   Skyreach, Steinfeld, Ghost and Crooked and **none for Eden**, so
   `EdenGardenBiome.getGuard()`, `EdenCanopyBiome.getGuard()` and
   `EdenShallowsBiome.getGuard()` — bloommaw, forbidden serpent, jealous vine,
   golden hornet — are dead code. Eden is the only realm whose guarded ground
   has no guards.
3. **Eden has no building.** No `Preset` class exists for it at all.
4. **Buildings are thinly furnished.** A table appears in 2 presets of 13, a bed
   in 1. Five "furnished" presets have a light and nothing else.
5. **The Warden's-house travel anchors are unplaced** (§A2.3). Four gate objects
   registered, nothing places them; no route is fast travel yet.
6. **Magpie, Halda and Ossian are near-unfindable.** Their placement needs a
   *player-built* workstation to already stand within 3 tiles of a region that
   is generating for the FIRST time — the opposite of how anyone builds. The
   other five residents key off naturally-painted landmarks and are fine.
7. **`distortion` is threaded to every band painter and read by none.** §3's
   calm/mad variants do not exist yet.
8. **5 of 9 named settlers have no dialogue** beyond shop and recruit: Mortimer,
   Caspern, Magpie, Halda, Ossian have no `interact()` override.
9. **`swh_beacon` is a registered dead quest**, kept only for old-save
   deserialization.
10. **`LongTablePreset` contains no table.** Thirty-four chairs, zero tables.

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
