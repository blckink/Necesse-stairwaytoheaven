# Area overview — one page per realm

**What each area of the mod actually contains, and how densely.** Every number
here is measured, not remembered: run `python3 tools/area_census.py` and it
prints the same table off the source. Where this file and any other document
disagree about a count, re-run the tool — it reads the code.

`docs/OVERVIEW.md` remains the status document ("does it work at all"). This one
answers the different question: **is an area full enough to be worth walking
across.**

Measured on `master` @ 2026-09-05, Necesse 1.3.2 — **after** that day's pass,
which closed three of the holes this document was written to find. What it
changed is in §"What this document already fixed" at the end; everything above
it is the state as it stands now.

---

## The one-glance table

| realm | tiles out | biomes | hostiles | critters | animals | NPCs | live quests | POIs | boss |
|---|---|---|---|---|---|---|---|---|---|
| **Skyreach** | 0–1800 | 4 | 8 | 4 | 2 | 7 | 5 | 4 | `cryoqueen` t8 · 57 240 HP |
| **Eden** | 600–2880 | 3 | 5 | **0** | 0 | 1 | 3 | 2 | `moonlightdancer` t8 · 127 200 HP |
| **Steinfeld** | 1920–4200 | 3 | 4 | **0** | 0 | 1 | 2 | 1 | `ascendedwizard` t9 · 157 520 HP |
| **Ghost Realm** | 2880–5280 | 5 | 9 | **0** | 0 | 4 | 4 | 1 | `pestwarden` t9 · 161 100 HP |
| **Crooked Beyond** | 4200–5640 | 5 | 8 | 1 | 0 | 1 | 3 | 1 | `crystaldragon` t10 · 208 000 HP |
| **Hell** | 4800–6000+ | **0** | 0 | 0 | 0 | 0 | 0 | 4 unreachable | — |

Read down the "NPCs" and "live quests" columns and the shape of the mod is
still plain: **the Skyreach is a finished game and the outer four realms are
thinner the further out you go.** Eden and the Crooked Beyond hold one person
each. The Ghost band holds four and, since this pass, four things to do. Nobody
lives past 5640 tiles at all.

---

## How "density" actually works here — read this before judging a number

A `MobSpawnTable` weight does **not** control how often you are attacked. It
controls **which** thing walks up when a spawn happens. Whether a spawn happens
at all is decided by the **tile's spawn tickets**, and this mod deliberately
sets most ground to zero (`worldgen/SkyPressure`, from the player's own
complaint that vanilla attacks every two seconds everywhere).

Three zones, in every realm:

| zone | tickets | how much of the realm |
|---|---|---|
| **guarded site** — the ground around loot | 600 | a disc of r≈7 around each lattice site |
| **its approach** | 100 | the ring out to r≈15 |
| **the wilds** — coarse noise | 45 (Crooked: 30) | ~1/6 of land (Crooked: less) |
| **everywhere else** | **0** | the rest — genuinely silent |

Vanilla's default ground is 100 and its deadest ground (`AshTile`) is 2, so
these sit on vanilla's own scale.

**So the honest density measure is: how many guarded sites per unit of land.**

| realm | site lattices | guarded sites per 1000×1000 tiles |
|---|---|---|
| Skyreach | wreck + workshop | **28.7** |
| Eden | grove + lagoon + orchard | **11.4** |
| Steinfeld | POI + graveyard + chapel | **20.3** |
| Ghost Realm | mausoleum + manor + graveyard | **21.8** |
| Crooked Beyond | door yard + inverted house + long table | **30.9** |
| Hell | — | 0 |

Boss portals are one lattice per realm at cell 600 / chance 0.35 →
**≈0.97 portals per 1000×1000 tiles**, in that realm's band only.

Eden is now the thinnest of the five that have any, at 11.4, and that is a
statement about its lattices rather than a bug: its three sites are rarer than
the Skyreach's workshops by design. Until 2026-09-05 the number was **0** — see
the last section.

---

## Skyreach — 0 to 1800 tiles out

The only realm that is finished.

**Biomes (4).** Driftlands 54% · Stormveil 19% · Skyway Passages 15% ·
Aurora Shoals 13%, plus the skystone barrens and the Mistsea across all four.

**Cast.** 8 hostiles, 4 critters, 2 farm animals — the only realm with either
of the last two.

| biome | table weight | entries | critters | guard pack |
|---|---|---|---|---|
| Driftlands | 425 | 4 | 1 (zephyrfinch) | golem + 2 galehound + ray |
| Stormveil | 460 | 6 | 1 (sparkbeetle) | golem + rime sentry + 2 wisp + ray |
| Skyway Passages | 460 | 5 | 2 (finch, glowmoth) | golem + sentry + 2 hound + ray |
| Aurora Shoals | 460 | 5 | 2 (glowmoth, dewsnail) | golem + dawnpiercer + 2 flake + ray |

Hostiles: `zephyrray` `stormwisp` `skystonegolem` `galehound` `dawnpiercer`
`rimesentry` `auroraflake` `mistserpent`. The Mistserpent carries weight 300
against a land table of ~130 for a reason that is not difficulty — it is the
only entry that can spawn over the cloud sea, so it must win the sea draws.

**Animals.** Nimbus Yak on cloudturf (8.5%/region, herds of 2–5, persistent)
and Glimmergoat on aurorashoal (7.0%/region). Placed by
`SkyLevel.placeLivestockHerds`, deliberately **not** on a spawn table —
*"wertvolle Tiere nicht an jeder Ecke"*.

**NPCs (7).** The Warden (+ his settler form), Magpie, Halda, Ossian Vane,
Siggi and Peanut. Magpie, Halda and Ossian need a *player-built* workstation to
already stand within 3 tiles of a region generating for the **first** time —
which is the opposite of how anyone builds, so in practice they are
near-unfindable.

**Quests (5 live).** `swh_findspire` → `swh_recruitwarden` → `swh_cats` →
`swh_anchor` → `swh_keyskyreach`. Plus `swh_beacon`, registered and dead, kept
so pre-0.5 saves deserialize.

**POIs (4).** Sky Tower, Sky Town, Toll Bridge, Sky Inn — plus the Warden's
Spire, the richest single building in the mod.

**Boss.** Cryo Queen, incursion tier 8, 18 000 base → **57 240 HP**.

---

## Eden — 600 to 2880 tiles out

**Biomes (3).** Eden Garden · Eden Canopy · Eden Shallows.

**Cast.** 5 hostiles, **0 critters**, 0 animals.

| biome | table weight | entries | critters | guard pack |
|---|---|---|---|---|
| Eden Garden | 100 | 3 | 0 | bloommaw + 2 serpent + hornet |
| Eden Canopy | 100 | 4 | 0 | forbidden serpent + vine + serpent + vine + bloommaw |
| Eden Shallows | 100 | 1 | 0 | 2 hornet + 2 serpent |

Eden Shallows has a **single** spawn entry (`goldenhornet`, weight 100) — the
thinnest table in the mod. Eden's tables are also the only ones using plain
`add` rather than `addLimited`, so nothing caps how many stack up in one ring.

**NPCs (1).** Eveleen the Botanist, beside a Knowledge Tree.

**Quests (3 live).** `swh_edenreach` → `swh_edenplants` → `swh_keyeden`.

**POIs (2).** Crown Garden, Ferment House. The older Knowledge Grove, Lagoon
Shrine and Orchard Ring are terrain/pressure sites, not buildings.

**Boss.** Moonlight Dancer, tier 8, 40 000 base → **127 200 HP**.

**Open holes.** No critters — the realm has no ambient life at all. All five
hostiles are registered `countKillStat = false`, so none has a bestiary row and
none counts a kill, while the Skyreach's, Steinfeld's and the Crooked Beyond's
all do. Fixing that is not a one-word change: the three-argument
`MobRegistry.registerMob` passes `countKillStat` through as `createSpawnItem`
too, and `MobRegistry.loadMobIcons` loads `mobs/icons/<id>` for every mob, so
flipping the flag adds five bestiary rows drawn with the engine's ERR texture.
The five icons are the real cost, and they belong in `docs/ASSET_REQUESTS.md`
rather than in a one-line commit.

---

## Steinfeld — 1920 to 4200 tiles out

Until 2026-09-05 the emptiest realm that was nominally finished. It has one
inhabitant now.

**Biomes (3).** Quiet Meadow · Slab Fields · Grave Heath.

**Cast.** 4 hostiles, 0 critters, 0 animals.

| biome | table weight | entries | critters | guard pack |
|---|---|---|---|---|
| Quiet Meadow | 150 | 2 | 0 | mourner + 2 pilgrim + crow (4–5) |
| Slab Fields | 205 | 4 | 0 | angel + 2 mourner + pilgrim + crow (5–7) |
| Grave Heath | 210 | 4 | 0 | 2 angel + 2 mourner + crow + pilgrim (6–8) |

**NPCs (1).** **Ives, the Verger of the Quiet Reach** — found beside a broken
angel, once per world. He is the realm's only vendor: he buys all four of its
materials above broker and sells the churchyard that goes around a grave. His
character is §A3.4's, not an invention — *"Hier landen Dinge, die nicht mehr
richtig zum Himmel gehören"*, and the ghosts out here who *"simply stand"* or
*"walk without purpose"* are what he is trying to lay down.

**Quests (2 live).** `swh_steinfeldvigil` — Ives asks for 14 Grave Salt and 10
Spirit Moss, the two Steinfeld materials that nothing else in the mod consumed;
pays his 11 000 fee waived plus 10 Stormsteel Bar. Then `swh_keysteinfeld`,
which the Warden only offers once the whole Warden's Call is done.

**POIs (1).** Steinfeld Memorial, plus the (unfurnished) Graveyard and Ruined
Chapel shells.

**Boss.** Ascended Wizard, tier 9, 44 000 base → **157 520 HP**.

**Open holes.** Zero critters — §A3.4 asks for exactly the thing that would fix
it (*"The ghosts here are mostly not enemies. Some simply stand. Some walk
without purpose. One might walk the same path between two gravestones
forever."*) and no such mob exists. Of the four materials, `gravesalt` and
`spiritmoss` are now Ives's ask and `echoshard` and `palestone` are the key
quest's, so all four have a demand — but still no *recipe* names any of them.

---

## Ghost Realm — 2880 to 5280 tiles out

The widest cast, the thinnest reason to use it.

**Biomes (5).** Aftergarden · Bone Orchard · Ectomarsh, plus **Gloomfen** and
**Ashen Reach** — the ex-Veil biomes, moved into this band per
`WORLD_DESIGN` §41.5.

**Cast.** 9 hostiles, 0 critters, 0 animals.

| biome | table weight | entries | critters | guard pack |
|---|---|---|---|---|
| Aftergarden | 205 | 4 | 0 | bride + butler + drifter + hound |
| Bone Orchard | 190 | 4 | 0 | bride + 2 widow + butler + crawler |
| Ectomarsh | 200 | 4 | 0 | bride + crawler + drifter + hound + chair |
| Gloomfen | 175 | 3 | 0 | wraith + 2 shade + cantor |
| Ashen Reach | 160 | 3 | 0 | wraith + 2 cantor + shade |

`possessedchair` appears **only** in the Ectomarsh guard pack — it is on no
spawn table anywhere, so outside that one pack it never appears.

**NPCs (4).** Mortimer the Undertaker, Caspern the Spirit Smith, Eleanor the
Lost Soul, and the Ghost Guide (summoned, not found).

**Quests (4 live).** `swh_eleanor`, `swh_keyghostrealm`, and since 2026-09-05
one each for the two who had none: **`swh_mortimerrites`** (12 Soul Thread + 10
Bonewood — shrouds and coffins; his 8 000 fee waived + 6 Spiritsteel Bar) and
**`swh_caspernforge`** (12 Spectral Ore + 8 Veil Essence — the ore feeds his
fire and the essence quenches it; his 14 000 fee waived + 6 Spiritsteel Bar).
Caspern's ask is the first thing in the mod that sends a player into the
Gloomfen and the Ashen Reach on purpose: Veil Essence only drops there.

**POIs (1).** Ghost Archive, plus the Haunted Manor, Mausoleum and Sunken
Graveyard shells.

**Boss.** Pest Warden, tier 9, 45 000 base → **161 100 HP**.

**Open holes.** No critters. Seven of the nine hostiles registered
`countKillStat = false` — same cost as Eden's five, seven icons.

---

## Crooked Beyond — 4200 to 5640 tiles out

**Biomes (5).** Checkerworks · Spiral Fields · Striped Waste, plus
**Beetlefreak Hollow** (ex-Veil) and the **Beetle Outlands** rim.

**Cast.** 8 hostiles, 1 critter (`stripebeetle`), 0 animals.

| biome | table weight | entries | critters | guard pack |
|---|---|---|---|---|
| Beetle Outlands | 260 | 6 | 0 | rare golem + golem + 2 shade + armadillo + wraith |
| Beetlefreak Hollow | 175 | 3 | 0 | wraith + 3 shade + cantor |
| Checkerworks | 160 | 4 | 0 | rare golem + 2 mimic + 2 golem + armadillo |
| Striped Waste | 120 | 3 | 1 | rare golem + mimic + 2 golem + armadillo |
| Spiral Fields | 110 | 2 | 0 | 3 tongueplant + armadillo + golem |

Spiral Fields has two entries and the lowest table weight in the realm.
`doormimic` guards the Striped Waste but is on no Striped Waste spawn table.

The realm's wilds are the mod's rarest (`WILDS_THRESHOLD` 0.74 vs 0.66
elsewhere) and its wild ticket value the lowest (30 vs 45), so between sites it
is the quietest ground outside Eden — deliberately, here.

**NPCs (1).** Mr. Knott the Doorman, at a Door Yard.

**Quests (3 live).** `swh_crookedarrival` → `swh_crookeddoor` →
`swh_keycrookedbeyond`.

**POIs (1).** Crooked Bazaar, plus the Door Yard, Inverted House, Long Table
and Crooked House shells.

**Boss.** Crystal Dragon, tier 10, 52 000 base → **208 000 HP**.

**Open holes.** Six materials (`oddwood`, `warpresin`, `strangefabric`,
`eyeseed`, `stripedshell`, `realityshard`) that no recipe consumes.

---

## Hell — 4800 tiles out and beyond

**Not built.** The 0.80–1.00 band falls through to the Crooked painter
(`SkyTerrainPainter.java:1071`), so walking out that far shows you more Crooked
Beyond. No biome, no cast, no NPC, no quest, no boss rung — `SkyBossLadder`
holds `mutanthydra` (80 000 base) for it and assigns no tier, deliberately.

Four Hell POI presets **are** written and registered — Border Office,
Administration, Forge, Carnival — and `RealmPoiWorldPreset` will place them in
the Hell band. They are the only Hell content that exists, and they stand in
Crooked ground.

---

## What this document already fixed — 2026-09-05

Writing it down was most of the work. Three of the eight holes below were closed
in the same pass, and all three were things the code had been carrying for
months without anyone being able to see them:

| hole | what it was | what closed it |
|---|---|---|
| **Eden had no guarded ground** | `EdenGardenBiome.getGuard()`, `EdenCanopyBiome.getGuard()` and `EdenShallowsBiome.getGuard()` were all dead code: `SkyLevel.placeGuardPacks` had a branch for four realms and none for Eden, while `EdenPressure` had already been written with the discs for them. Every other realm's three lines were carried over when its level was folded into `SkyLevel`; Eden's were not. | three `placePacksOf` calls on Eden's own grove / lagoon / orchard lattices — the same constants `EdenPressure` already reads. 0 → **11.4 guarded sites per 1000×1000** |
| **Steinfeld had nobody in it** | no named human, no settler, no vendor, one quest, in a band 2280 tiles deep | **Ives, the Verger of the Quiet Reach**, and `swh_steinfeldvigil` |
| **Two Ghost NPCs had nothing to say** | Mortimer and Caspern had shops, greeting lines and no quest and no `interact()` | `swh_mortimerrites` and `swh_caspernforge` |

Each new quest asks only for materials its own realm drops — the rule
`SkyreachKeyQuest` states — and each was pointed at a material that had **no
consumer at all**: Grave Salt and Spirit Moss for Ives, Soul Thread for
Mortimer, Veil Essence for Caspern.

**These are additive, and an existing save will not have them.** `Ives` and the
Eden packs are placed at region generation, which fires once per region ever.
`/swhreset world` retrofits them into ground an older build already generated;
`docs/SAVE_COMPAT.md` is the whole picture.

---

## The ranked list of what is still missing, by what it costs the player

1. **Hell is a hole with four buildings in it.** No biome, no cast, no boss
   rung — and four Hell POI presets that stand in Crooked ground.
2. **Only the Skyreach has critters or animals.** Four realms have no ambient
   life at all. Steinfeld's fix is already written in `WORLD_DESIGN` §A3.4 and
   just not built: ghosts that are not enemies, that stand, or that walk the
   same path between two gravestones forever.
3. **Twelve hostiles never enter the bestiary.** Eden's five and Ghost's seven
   are `countKillStat = false` while Skyreach's, Steinfeld's and Crooked's are
   `true`. The blocker is twelve `mobs/icons/*.png`, not the flag — see the Eden
   section for why.
4. **Ten realm materials are named by no recipe.** Crooked's six and
   Steinfeld's four now have quest and shop demand, but nothing is *crafted*
   from any of them.
5. **Eden and the Crooked Beyond hold one person each**, and neither has a
   found-in-the-realm chain beyond the one they already had.
6. **Eden Shallows has a single spawn entry**, and Eden's three tables are the
   only ones in the mod using plain `add` rather than `addLimited`, so nothing
   caps how many stack up in one ring.
7. **`possessedchair` and `doormimic` guard ground they cannot spawn on** —
   both appear in a guard pack and on no spawn table for the same biome.
8. **The original 13 POI shells remain thinly furnished** (`docs/OVERVIEW.md`
   §2), and POI presets are the one thing `/swhreset world` cannot retrofit.

---

## Keeping this file honest

```bash
python3 tools/area_census.py            # the measurements above
python3 tools/area_census.py --markdown # just the summary table
python3 tools/area_census.py --check    # exit 1 while a proven hole is open
```

The tool only reports holes it can **prove** from the source — a `getGuard()`
with no caller, a realm with no critter table, a realm with no NPC, a hostile
with `countKillStat = false`. It has no opinion about whether a realm is fun.

Two of its maps are hand-written and must be kept in step when content lands:
`NPC_REALM` and `QUEST_REALM`. Everything else it reads out of the code.
