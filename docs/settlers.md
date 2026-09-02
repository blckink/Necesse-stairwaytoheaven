# Settlers — Eveleen, Mortimer, Caspern, Eleanor, Mr. Knott

The five named residents this pass finished: each was a `SkySettlerMob` class
sitting in `src/main/java/stairwaytoheaven/mobs/` with nothing that actually
stood it in the world. `docs/WORLD_DESIGN.md` §5, §11 and §15 name them;
`settlement/SkySettlers.java` registers their mob type and `Settler` type
together (see that class's own doc for why a mob without a registered
`Settler` can never be recruited — the bug the Sky Warden shipped with once);
this document is where each one actually lives and how a player finds them.

Companion doc: `docs/quests.md` covers the three quest chains three of these
five hand out. The borrowed-sprite table is at the bottom of this file.

## How to read "found" vs. "travels"

Every one of the five is placed **once per world**, deterministically, by
worldgen — a persistent (`canDespawn = false`) mob standing beside a specific
landmark, rolled from the level seed and the region coordinates so the same
world always stands the same person in the same place. Three of the five can
ALSO travel to a settlement on their own, the vanilla way (a visitor arrives,
stays a while, and can be recruited) — see `settlement/SkyArrivals.java`.
`SkywatchWorldData.residentsClaimed` (a `WorldData`, so it survives a
dimension regenerating) is what stops both routes from ever producing two of
the same person: whichever route finds them first claims the name, and the
other refuses it forever after.

| settler | profession | found in the world | travels to a settlement | recruit cost |
|---|---|---|---|---|
| Eveleen, the Eden Botanist | Farming + Forestry + **Fertilising** | Garden of Eden, beside a Knowledge Tree | once the settlement has 9+ tiles of Eden Grass (`SkyArrivals.EDEN_PATCH`) | 7000 coins, **waived** once her quest chain is paid |
| Mortimer, the Undertaker | Hauling + Crafting + **Hunting** | Ghost Realm / Aftergarden, beside a gravestone | once the settlement has 3+ gravestones (`SkyArrivals.GRAVEYARD`) | 8000 coins |
| Caspern, the Spirit Smith | Crafting only (refuses Farming/Forestry) | Ghost Realm / Aftergarden, beside a gravestone | once the settlement has an Aether Forge (`SkyArrivals.FORGE`) | 14000 coins |
| Eleanor, the Lost Soul | **Husbandry** (STAY ending only) | Ghost Realm / Aftergarden, beside a gravestone | never — §11 makes this a choice, not a timer | 5000 coins (STAY), or 12x Veil Essence (PASS ON, no coins) |
| Mr. Knott, the Doorman | **Trading** (refuses Farming/Forestry) | Crooked Beyond, at the Door Yard | never — §15 names no condition | 22000 coins |

Mortimer, Caspern and Eleanor are still called "the Veil trio" in one class's
own name (`settlement/VeilResidents.java`) even though they moved to the Ghost
Realm once it shipped — see that class's doc comment for the history. Their
placement call is `GhostLevel.onRegionGenerated` →
`VeilResidents.placeInGhost`; Eveleen's is `EdenLevel.onRegionGenerated` →
its own private `placeResident`; Knott's is
`CrookedLevel.onRegionGenerated` → `CrookedResidents.place`.

---

## Eveleen, the Eden Botanist

`eveleensettler` — `mobs/EveleenMob.java`

**Profession.** Farming and Forestry are on for every settler by default;
what makes her the settlement's *farmer* specifically is
`enableProfession("fertilize")` — fertilising is withheld from every settler
until one of them can do it, exactly the way vanilla's own `FarmerHumanMob`
works.

**Found.** Beside a Knowledge Tree in the Garden of Eden
(`EdenLevel.placeResident`, 0.35 region chance, requires a Knowledge Tree
within 3 tiles — Knowledge Trees are themselves rare worldgen objects, so the
combined odds land her inside a normal afternoon of exploring). She can also
turn up as a settlement visitor once the settlement already has 9+ tiles of
Eden Grass growing (`SkyArrivals.EDEN_PATCH` — see that class for why "Eden
Grass in the settlement" stands in for §5's "discovering an Eden island" until
the Garden of Eden ships its own farming loop).

**Shop.** Seeds (Eden Grass Seed — the only shop in the game that sells it —
plus wheat, carrot, pumpkin, strawberry), saplings (cloudberry, apple, and
lemon/banana once `sageandgrit` is dead), the growing kit (fertilizer, flower
pot, a Queen Bee for a Bee Hive once `piratecaptain` is dead), and one rare
flower. Buys wind wheat, cloudberry, wheat and sunflower.

**Quest chain.** `EdenArrivalQuest` → `EdenPlantsQuest` — see
`docs/quests.md`. Completing it waives her coin fee entirely
(`getRecruitItems` returns an empty list once
`SkywatchWorldData.edenPlantsGiven` is set).

---

## Mortimer, the Undertaker

`mortimersettler` — `mobs/MortimerMob.java`

**Profession.** Hauling and Crafting stay on (every settler has them); Hunting
is turned on (`enableProfession("hunting")`, withheld from every settler
except vanilla's own Hunter by default) and Farming/Forestry are explicitly
refused (`refuseJob`) — the same shape vanilla's Guard uses to keep its own
character: a settler who does everything has none.

**Found.** Beside a gravestone in the Ghost Realm / Aftergarden
(`settlement/VeilResidents.java`, 0.14 region chance, must stand within 3
tiles of the realm's own gravestone prop). Travels to a settlement once it has
3+ vanilla gravestone-family objects standing inside its bounds
(`SkyArrivals.GRAVEYARD` — §11's "after building a graveyard in the
settlement", read as the literal thing a player would build, since vanilla has
no "graveyard" room type to check against).

**Shop.** Gravestones (all four vanilla varieties), a Sarcophagus (the coffin
of §11, once `swampguardian` is dead), candles and urns, a Spirit Basin (once
`reaper` is dead), and the full eleven-piece Bonewood furniture family. Buys
Bone, Ectoplasm and — uniquely among the shops in the mod — Veil Essence,
giving the Veil's Gloom Shades a buyer who isn't crafting with the drop.

**Quest chain.** None of his own — §11 gives him only an arrival condition,
not a delivery quest.

---

## Caspern, the Spirit Smith

`caspernsettler` — `mobs/CaspernMob.java`

**Profession.** Crafting and Hauling stay on; Farming/Forestry are refused
(`refuseJob`), the same Guard-shaped move Mortimer's specialism uses in the
other direction. Put him on the Aether Forge and he stays there.

**Found.** Beside a gravestone in the Ghost Realm / Aftergarden, same
placement rules as Mortimer (they roll from the same table,
`SkySettlers.VEIL_RESIDENTS`). Travels to a settlement once it has an Aether
Forge built (`SkyArrivals.FORGE` — §11's "build the Spirit Forge", answered
with the forge the mod actually has today).

**Shop.** Spiritsteel and Soul Thread do not exist as their own items yet
(that is Ghost Realm content out of this pass's scope), so §11's "Spiritsteel,
spectral weapon recipes, Soul Thread" is served by the closest vanilla
stand-ins, recorded in `docs/VANILLA_ASSET_MAP.md`: Nightsteel ore/bar for
Spiritsteel, Phantom Dust for Soul Thread, plus Silk, Bone Arrows and two rare
spectral-looking pieces (a Bone Hilt, once `swampguardian` is dead; a
Nightsteel Veil, once `reaper` is dead). Buys Aetherium Bar, Stormsteel Bar,
Bone and Ectoplasm — the Aether Forge's second customer, after Ossian.

**Quest chain.** None of his own, same as Mortimer.

---

## Eleanor, the Lost Soul

`eleanorsettler` — `mobs/EleanorMob.java`

**Profession.** Husbandry only on the STAY ending
(`enableProfession("husbandry")`) — §27's "Spirit Shepherd" row for the Ghost
Realm's Spirit Sheep and Grave Chicken, given to her because §11 files her as
a quest NPC rather than a tradesperson and the STAY ending is what makes her
one.

**Found.** Beside a gravestone in the Ghost Realm / Aftergarden, same table
and placement rules as Mortimer and Caspern. **Never travels** — no
`SkyArrivals` gate names her at all, deliberately: §11 makes her ending a
choice the player has to go make, not something a visitor timer can hand
them.

**Shop.** What people leave at a grave: flowers, a bouquet, potted flowers,
lanterns. Buys Veil Essence and Ectoplasm.

**Her two endings — see `docs/quests.md` for the full quest writeup:**
- **PASS ON.** Hold 12x Veil Essence in the selected slot and talk to her.
  She is removed from the world permanently (recorded in
  `SkywatchWorldData.eleanorPassedOn`, which also stops a second Eleanor ever
  spawning again in this world) and leaves a Will-o'-Wisp Lantern plus 14
  Spiritsteel Bars.
- **STAY.** Talk to her without Veil Essence selected — this opens the
  ordinary vanilla recruit page. Paying the 5000-coin fee recruits her as the
  settlement's husbandry settler, and `onRecruited` pays the same 14
  Spiritsteel Bar bonus on top, so neither ending reads as the materially
  poorer choice.

---

## Mr. Knott, the Doorman

`knottsettler` — `mobs/KnottMob.java`

**Profession.** `tradingmission` — one of the five jobs vanilla withholds from
every settler by default, borrowed here for the settler who deals in access
rather than goods (§27's own speculative "Vex" row uses the same archetype).
Refuses Farming/Forestry, same shape as Caspern.

**Found.** At the Door Yard in Crooked Beyond
(`settlement/CrookedResidents.java`, 0.20 region chance, must stand within 9
tiles of a stamped Door Yard site — asked through `CrookedSites.nearestDoorYard`
rather than an object scan, the same question `CrookedLevel.placeGuardPacks`
already asks it). **Never travels** — §15 names no arrival condition, and
Crooked Beyond is deep enough into the climb that "found in the realm" is
already the honest story (the same call made for Eleanor).

**Shop.** The Reality Stitcher — the station that would make Crooked doors and
keys real inventory — is itself deferred, so §15's "keys, doors, portals" is
not yet anything the game can sell. What ships instead: two vanilla oddities
that read as "weird furniture" without persuasion (a Void Cube, a Small Rune
Stone) and a shelf of vanilla cosmetic masks (§15's line made literal). Buys
Warp Resin, Eye Seed and Reality Shard — the realm's own stranger half of its
economy.

**Quest chain.** `CrookedArrivalQuest` → `CrookedDoorQuest` — see
`docs/quests.md`. **Not a recruitment gate**, unlike Eveleen's chain: he is
recruitable from the moment he is found regardless of quest progress, and the
chain is a separate reward track layered on top. (An earlier version of his
`interact()` accidentally coupled the two anyway — see "A dead end this pass
found and fixed" in `docs/quests.md`.)

---

## Borrowed sprites

No pixel art was drawn for this pass. Every sprite below is an existing
vanilla or mod file, loaded by its literal registry path — checked against
`vanilla-sprites/` — never recoloured or regenerated. `docs/VANILLA_ASSET_MAP.md`
carries the mod-wide version of this table; the rows below are the ones this
pass added or leaned on.

| use | borrowed path / item ID | source | what it actually is |
|---|---|---|---|
| Eveleen's settlement icon | `mobs/icons/farmerhuman` | vanilla | the Farmer's own icon |
| Eveleen's wardrobe (hat / chest / boots) | `dryadhat` / `dryadchestplate` / `dryadboots` | vanilla | Dryad Hat / Chestplate / Boots |
| Mortimer's settlement icon | `mobs/icons/pawnbrokerhuman` | vanilla | the Pawnbroker's own icon |
| Mortimer's wardrobe | `tophat` / `thiefscloak` / `dressshoes` | vanilla | Top Hat / Thief's Cloak / Dress Shoes |
| Caspern's settlement icon | `mobs/icons/blacksmithhuman` | vanilla | the Blacksmith's own icon |
| Caspern's wardrobe | `nightsteelveil` / `smithingapron` / `smithingshoes` | vanilla | Nightsteel Veil / Smithing Apron / Smithing Shoes |
| Eleanor's settlement icon | `mobs/icons/stylisthuman` | vanilla | the Stylist's own icon |
| Eleanor's wardrobe | `snowhood` / `snowcloak` / `clothboots` | vanilla | Snow Hood / Snow Cloak / Cloth Boots |
| Knott's settlement icon | `mobs/icons/exoticmerchanthuman` | vanilla | the Exotic Merchant's own icon |
| Knott's wardrobe | `jesterhat` / `labcoat` / `jesterboots` | vanilla | Jester Hat / Lab Coat / Jester Boots |
| Eden Gate (down) world sprite | `objects/skystairwaydown.png` | **this mod**, reused a 2nd time | the Skyward Stairway's own down sheet (`EdenGateObject`) |
| Eden Return Gate (up) world sprite | `objects/skystairwayup.png` | **this mod**, reused a 2nd time | the Skyward Stairway's own up sheet (`EdenSideGateObject`) |
| Eden Threshold (basin) world + item sprite | `objects/spiritbasin.png`, `items/spiritbasin.png` | vanilla, reused a 2nd time (the Ghost Gate's Soul Basin already uses it once) | vanilla's own Spirit Basin |

Every settler's face itself is generated at runtime by vanilla's own human
look randomiser, seeded to a fixed value per character
(`SkySettlerMob.lookSeed()`) so the same person looks the same in every world
— no icon was drawn for the character's body, only for the settlement-screen
portrait above.
