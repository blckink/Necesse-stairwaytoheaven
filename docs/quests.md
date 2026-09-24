# Quests — every chain, in the order a player meets it

## The whole table (read off the code, 2026-09-24)

Twenty quests are registered; nineteen are handed out, and `swh_beacon` is the
dead legacy one (registered only so pre-0.5 saves load, see below). "Rung" is
the realm the ask sends you to, in `docs/WORLD_DESIGN.md`'s order. Where a
reward changed in the 2026-09-24 tuning pass, the old value is struck through
next to it; the reasons are in "The 2026-09-24 tuning pass" at the end.

| # | id | giver | ask | reward | rung |
|---|---|---|---|---|---|
| 1 | `swh_findspire` | first climb of the Skyward Stairway | find the Old Warden Spire | map pin (signpost) | Skyreach |
| 2 | `swh_recruitwarden` | Sky Warden | recruit him, 30 000 coins | he moves in + **Silver Bell** (the Veil's key) + the beacon lights | Skyreach |
| 3 | `swh_cats` | Sky Warden | coax Siggi and Peanut home with Cloudpuff Treats | Cat Basket, 2x Flickerlight Garland, 10x Stormsteel Bar | Skyreach |
| 4 | `swh_anchor` | Sky Warden | 20x Aetherium Bar, 80x Skystone, 8x Stormsteel Bar | **Wolkengleve** ~~Stormsteel Vambrace~~, Skywatch Banner, 5x Aurora Petal | Skyreach (finale) |
| 5 | `swh_keyskyreach` | Sky Warden | 10x Storm Shard, 5x Fulgurite | Skyreach Watchfire, 4x Stormsteel Bar, **4x** ~~1x~~ Stormdisc, Skywatch Hood | Skyreach → Cryo Queen |
| 6 | `swh_edenreach` | Eden Gate, first use | find Eveleen | signpost | Eden |
| 7 | `swh_edenplants` | Eveleen | 1x Eden Berry, 1x Moon Melon, 1x Sun Grape | 3x Knowledge Cutting, 10x Stormsteel Bar, her 7 000 fee waived | Eden |
| 8 | `swh_keyeden` | Sky Warden | 8x Eden Sap, 6x Golden Pollen | Eden Garden Stair, 5x Stormsteel Bar, Galehowl, Warden's Mantle | Eden → Moonlight Dancer |
| 9 | `swh_steinfeldvigil` | Ives | 14x Grave Salt, 10x Spirit Moss | his 11 000 fee waived, **12x** ~~10x~~ Stormsteel Bar | Steinfeld |
| 10 | `swh_keysteinfeld` | Sky Warden | 8x Echo Shard, 20x Pale Stone | Mourning Angel, 6x Stormsteel Bar, Tempest Edge, Warden's Boots | Steinfeld → Ascended Wizard |
| 11 | `swh_eleanor` | Eleanor | PASS ON: hold 12x Veil Essence · STAY: recruit her (5 000) | PASS ON: Will-o'-Wisp Lantern + **10x** ~~14x~~ Spiritsteel Bar · STAY: she moves in + **10x** ~~14x~~ | Ghost Realm |
| 12 | `swh_mortimerrites` | Mortimer | 12x Soul Thread, 10x Bonewood | his 8 000 fee waived, 6x Spiritsteel Bar | Ghost Realm |
| 13 | `swh_caspernforge` | Caspern | 12x Spectral Ore, 8x Veil Essence | his 14 000 fee waived, 6x Spiritsteel Bar | Ghost Realm |
| 14 | `swh_keyghostrealm` | Sky Warden | 12x Bonewood, 8x Spectral Ore | Raven Perch, 6x Spiritsteel Bar, **Spiritsteel Reaver** ~~Skyreave~~, Stormsteel Vambrace | Ghost → Pest Warden |
| 15 | `swh_crookedarrival` | Crooked Door, first use | find Mr. Knott | signpost | Crooked Beyond |
| 16 | `swh_crookeddoor` | Mr. Knott | 5x Reality Shard, 8x Warp Resin, 8x Strange Fabric | Zephyr Harness, 12x Spiritsteel Bar, 6x Reality Shard | Crooked Beyond |
| 17 | `swh_keycrookedbeyond` | Sky Warden | 16x Oddwood, 8x Reality Shard | Knott's Crooked Door, 8x Spiritsteel Bar, **Gravewind Bow** ~~Prismcaller~~, Aurora Locket | Crooked → Crystal Dragon |
| 18 | `swh_keyhell` | Sky Warden | 16x Reality Shard, 24x Oddwood | Infernal Seal, **16x** ~~10x~~ Spiritsteel Bar ~~+ Thunderhead + Zephyr Harness~~ | Hell → Mutant Hydra |
| — | `swh_beacon` | *nobody* | — | dead: registered so old saves load, never handed out | — |

**How the chains hand each other on.** The Warden's chain is strictly linear
(`SkyWardenMob.chapterFor`: RECRUIT → CATS → CATS_TURNIN → ANCHOR → DONE), and
the six region keys start only at DONE and are offered one at a time in boss
order. The realm chains (Eveleen, Ives, Mortimer, Caspern, Eleanor, Knott) are
side lines layered on top: each is handed out by its own person the first time
they are met, and two of them (Eden, Crooked) open with a signpost handed out
by the gate object you walk through. Nothing in the table can be handed out and
then never turned in — see "A dead end this pass found and fixed" and the
region-key co-op sweep below.

**The two weapons this pass added, and where they come from.**

- **Wolkengleve** — only from `swh_anchor`. The finale of the mod's first
  chain now pays a weapon nobody can craft, above the craftable Skyreave (165
  vs 150) and below the Ghost Realm's Spiritsteel Reaver (176).
  `arsenal/CloudGlaiveToolItem` has the calibration.
- **Himmelslanze** — deliberately NOT a quest reward. Crafted at the Tungsten
  Workstation from 3x Aetherwright's Casing + 10x Eden Bronze Bar + 8x Golden
  Pollen + 6x Aetherium Bar: the Casing is the material
  `docs/design/chapter-01-skyreach-cast.md` §3 reserved for "the first weapon
  tier past Stormsteel", and neither it nor Eden Bronze had a consumer before.
  `arsenal/SkyLanceToolItem` has the calibration.


Four new HUD quest lines, ten `Quest` classes in
`src/main/java/stairwaytoheaven/quest/`, each registered in
`QuestRegistry` from `StairwayToHeavenMod.init()`:

| registry id | class | chain |
|---|---|---|
| `swh_edenreach` | `EdenArrivalQuest` | Eden, step 1 |
| `swh_edenplants` | `EdenPlantsQuest` | Eden, step 2 |
| `swh_eleanor` | `EleanorQuest` | Ghost Realm |
| `swh_crookedarrival` | `CrookedArrivalQuest` | Crooked Beyond, step 1 |
| `swh_crookeddoor` | `CrookedDoorQuest` | Crooked Beyond, step 2 |
| `swh_keyskyreach` | `SkyreachKeyQuest` | region keys, 1 of 5 |
| `swh_keyeden` | `EdenKeyQuest` | region keys, 2 of 5 |
| `swh_keysteinfeld` | `SteinfeldKeyQuest` | region keys, 3 of 5 |
| `swh_keyghostrealm` | `GhostKeyQuest` | region keys, 4 of 5 |
| `swh_keycrookedbeyond` | `CrookedKeyQuest` | region keys, 5 of 5 |

Companion doc: `docs/settlers.md` covers the five settlers, three of whom hand
these out, in full (profession, shop, where each is found).

## Why "registered" was never the hard part

`docs/CONTENT_LEDGER.md` already carries one cautionary tale: `swh_beacon`
(`BeaconDeliveryQuest`) is registered in `QuestRegistry` and has never once
been handed to a player — nothing in the source calls
`new BeaconDeliveryQuest()` outside the class's own constructor. A registered
quest is not a reachable one. Every quest below was checked against that
standard specifically: registered, handed out by a real code path, and — this
is the part `swh_beacon` never got to — actually completable once handed out.
See "A dead end this pass found and fixed" below for the one place that last
check caught something.

## The Eden chain — Eveleen

**Step 1 — `swh_edenreach`, "Into the Garden".** A pure signpost, no tracked
state (the same shape as the mod's original `FindSpireQuest`). Handed out by
`EdenGateObjectEntity.use()` the first time a player steps through the Eden
Gate — guarded on `!SkywatchWorldData.edenPlantsGiven(server)` so a player who
already finished the chain is never handed the signpost again on a later
visit. Objective: find whoever tends the Garden of Eden (a Knowledge Tree is
the landmark; finding the tree IS finding Eveleen, since that is where she
stands). Cleared — not "completed", the same as `FindSpireQuest` — the moment
`EveleenMob.interact` first runs `advanceEdenChain`, which removes it and
issues step 2 in the same call.

**Step 2 — `swh_edenplants`, "A Taste of Eden".** A `DeliverItemsQuest`:
1x `edenberry` + 1x `moonmelon` + 1x `sungrape`. §5's own unlock line —
"collecting three Eden plants" — is read as one of each of the realm's three
named fruits rather than three of one kind, doubling as a light tour of all
three Eden biomes; all three are ordinary ambient drops, nothing gated behind
a recipe. Handed out and turned in by `EveleenMob.interact` →
`advanceEdenChain`.

**Reward.** 3x Knowledge Cutting (closes the loop the chain opened with "find
the Knowledge Tree") + 10x Stormsteel Bar, **and** her recruit fee (normally
7000 coins) is waived permanently for the world
(`SkywatchWorldData.edenPlantsGiven`; `EveleenMob.getRecruitItems` returns an
empty list once it is set).

**Benchmark.** The Skyreach's own chain (`SpireCatsQuest` +
`AnchorDeliveryQuest`, driven from `SkyWardenMob`) pays 10 Stormsteel Bar for
the cats, then the Wolkengleve for the anchor. 10 Stormsteel Bar matches the
cats exactly — see `docs/BALANCE.md` §7 for why Stormsteel is
the right currency to match rather than beat here: Eden sits at the bottom of
this pass's three new chains.

## The Ghost Realm — Eleanor

**`swh_eleanor`, "Why She Stayed".** One quest, two endings, both real —
§11's own text: "Two endings: **Pass on** (reward: strong trinket) or
**Stay** (Eleanor becomes a recruitable settler)." Handed out by
`EleanorMob.interact` the first time she is found (while she is neither
settler nor visitor). A pure signpost otherwise — everything that decides
which ending happens lives in `EleanorMob` itself, not in the quest class.

- **PASS ON.** Hold 12x `veilessence` in the selected inventory slot and talk
  to her — holding it in the hand rather than merely owning it is what makes
  it a deliberate choice, the same shape `SpireCatMob` already uses for a
  Cloudpuff Treat. Removed by `EleanorMob.interact`'s PASS ON branch, which
  also removes her from the world and records
  `SkywatchWorldData.markEleanorPassedOn` — permanent; no second Eleanor ever
  spawns in this world again.
  **Reward:** 1x Will-o'-Wisp Lantern (§11's "strong trinket" — a vanilla
  stand-in, recorded in `docs/VANILLA_ASSET_MAP.md`) + 10x Spiritsteel Bar.
- **STAY.** Talk to her without Veil Essence selected — opens vanilla's own
  recruit page. Paying 5000 coins recruits her; `EleanorMob.onRecruited`
  removes the quest and pays the **same** 10x Spiritsteel Bar bonus, so
  neither ending is the materially poorer one.

**Benchmark.** 10 (14 until 2026-09-24). Fourteen was measured against the
Skyreach cats' 10 Stormsteel and never against its neighbours, and against
those it was the table's outlier: more Spiritsteel than Mr. Knott one realm
further out (12), more than twice Mortimer's and Caspern's 6 for bigger asks,
and more than the Ghost key's 6. Ten keeps her the best-paid Ghost Realm
resident — hers is the chain with a permanent choice in it — under Knott.

## Steinfeld — Ives, the Verger

**`swh_steinfeldvigil`, "The Vigil".** Steinfeld's first quest that is not a
region key, and its first NPC — see `docs/AREA_OVERVIEW.md` for the measurement
that made the case, and `WORLD_DESIGN` Part B for the hole it had already
recorded ("Steinfeld has no NPC, no boss and no station"). Handed out by
`IvesMob.interact` on the first conversation and turned in on any later one.

**The ask:** 14x `gravesalt`, 10x `spiritmoss`. Both Steinfeld-only, per the
rule `SkyreachKeyQuest` states — and both chosen because they were **the two
materials in the realm nothing consumed**. `swh_keysteinfeld` already takes Echo
Shard and Pale Stone; no recipe anywhere named Grave Salt or Spirit Moss.

**Reward:** his 11 000 recruit fee waived + 12x Stormsteel Bar (10 until
2026-09-24). The same shape as `swh_edenplants`, because the two are the same
beat one band apart — and two bars more, because it is one band further out
and asks for 24 drops where Eveleen asks for three fruit: a person found in a realm who joins for free once you have
proved you can work their ground.

**Where the character comes from.** §A3.4, not invention: *"Hier landen Dinge,
die nicht mehr richtig zum Himmel gehören"*, and the ghosts out here who
*"simply stand"* or *"walk without purpose"*. A verger keeps a churchyard. He is
the one person out here who has decided that is somebody's job.

## The Ghost Realm — Mortimer and Caspern

Both had a shop, a greeting line and nothing to do. `docs/OVERVIEW.md` §8.7 had
listed it for months and `docs/AREA_OVERVIEW.md` put a number on it: four named
people in the band and two live quests, one of them a region key.

**`swh_mortimerrites`, "The Last Rites".** 12x `soulthread` + 10x `bonewood` —
a shroud is thread and a coffin is wood, and the Undertaker is the only person
in the mod who would think of them in that order. Reward: his 8 000 fee waived
+ 6x Spiritsteel Bar. Soul Thread had **no consumer at all** before this.

**`swh_caspernforge`, "The Cold Forge".** 12x `spectralore` + 8x `veilessence`
— the ore feeds his fire, the essence quenches it. Reward: his 14 000 fee
waived + 6x Spiritsteel Bar. This is the first thing in the mod that sends a
player into the **Gloomfen and the Ashen Reach** on purpose: Veil Essence only
drops there, and those two ex-Veil biomes had no quest reason to enter at all.
Veil Essence also had three buyers and no consumer, which made it money rather
than a material.

**Six, not ten.** `swh_keyghostrealm` pays ten Spiritsteel, and these sit below
it for the reason `SkyreachKeyQuest` gives about its own curve: the key's real
payout is the tier-9 boss it unlocks, and a side chain matching it would make
the key look like the smaller errand. The two match each other bar for bar
because they are the same rung of the same realm; what separates them is the
fee, and Caspern's is nearly twice Mortimer's.

**Bonewood and Spectral Ore are asked for twice** — here and by
`swh_keyghostrealm` — and that is deliberate. A realm whose materials have
exactly one buyer each is a realm you farm once.

**One shared state machine.** All three of these, plus Eveleen's and Knott's,
run `SkyQuests.advanceResidentChain`: ask once, take once, pay once, guarded by
`SkywatchWorldData.residentChainsDone`. Deliberately NOT gated on
`!isSettler()` — that gate is the bug `EveleenMob.interact` documents at
length, and it would make a chain unfinishable for anyone who recruited the
giver at full price on the first meeting.

## The Crooked chain — Mr. Knott

**Step 1 — `swh_crookedarrival`, "A Door That Goes Somewhere".** Pure
signpost, same shape as `swh_edenreach`. Handed out by
`CrookedDoorObjectEntity.use()` the first time a player steps through a
Crooked Door, guarded on `!SkywatchWorldData.crookedDoorwayOpened(server)`.
Objective: find whoever keeps the Door Yard (a free-standing red door is the
landmark — §15 made literal). Cleared the moment `KnottMob.interact` first
runs `advanceDoorChain`, which removes it and issues step 2.

**Step 2 — `swh_crookeddoor`, "Convince the Door".** A `DeliverItemsQuest`:
5x `realityshard` + 8x `warpresin` + 8x `strangefabric` — the realm's own
registered economy, nothing invented, chosen because they read as the Reality
Stitcher's eventual first recipe (that station is itself deferred; this is a
small down payment on it). Handed out and turned in by `KnottMob.interact` →
`advanceDoorChain`.

**Reward.** 1x Zephyr Harness (one of the mod's three EPIC trinkets, already
registered in `SkyItems`, reused here rather than inventing a Crooked-native
one) + 12x Spiritsteel Bar + 6x Reality Shard (a small seed fund back, for
whichever future pass builds the Stitcher).

**Benchmark.** Crooked Beyond sits at incursion tier 10 on
`docs/BALANCE.md`'s own realm ladder — the ceiling the Skyreach itself topped
out at before the mod's endgame rescale — so its chain is the largest of the
three this pass adds: 12 Spiritsteel Bar, above the Skyreach cats' 10
Stormsteel, plus the Reality Shard seed fund on top of the trinket. (It paid
Stormsteel until 2026-09-15, a tier below what the Ghost Realm before it pays.)

**Important: this chain is not a recruitment gate.** Unlike Eveleen, Knott is
recruitable from the moment he is found, regardless of whether the chain is
finished — §15 names no arrival or recruitment condition for him at all. The
quest chain is a second, independent reward track layered on top of
recruiting him, not a precondition for it.

---

## The region keys — the Sky Warden

`docs/FOGKEY_AND_BOSSPORTALS.md` §B1-B2. Five `DeliverItemsQuest`s, one per
realm that has a boss portal, each paying a **buildable key piece** that wakes
that realm's summoning stones when it is stood up inside a settlement. Handed
out and turned in by `SkyWardenMob.advanceRegionKeys`, one at a time, in the
order of §B4's own boss ladder.

| quest | asks for | pays | wakes |
|---|---|---|---|
| `swh_keyskyreach`, "The Watchfire" | 10x Storm Shard + 5x Fulgurite | Skyreach Watchfire + 4x Stormsteel Bar + 4x Stormdisc + Skywatch Hood | Cryo Queen, tier 8 |
| `swh_keyeden`, "The Garden Stair" | 8x Eden Sap + 6x Golden Pollen | Eden Garden Stair + 5x Stormsteel Bar + Galehowl + Warden's Mantle | Moonlight Dancer, tier 8 |
| `swh_keysteinfeld`, "The Mourning Angel" | 8x Echo Shard + 20x Pale Stone | Steinfeld Mourning Angel + 6x Stormsteel Bar + Tempest Edge + Warden's Boots | Ascended Wizard, tier 9 |
| `swh_keyghostrealm`, "The Raven Perch" | 12x Bonewood + 8x Spectral Ore | Aftergarden Raven Perch + 6x Spiritsteel Bar + Spiritsteel Reaver + Stormsteel Vambrace | Pest Warden, tier 9 |
| `swh_keycrookedbeyond`, "A Door of Your Own" | 16x Oddwood + 8x Reality Shard | Knott's Crooked Door + 8x Spiritsteel Bar + Gravewind Bow + Aurora Locket | Crystal Dragon, tier 10 |
| `swh_keyhell`, "Everything Appears to Be in Order" | 16x Reality Shard + 24x Oddwood | The Infernal Seal + 16x Spiritsteel Bar | Mutant Hydra |

The single source for this table is `SkyWardenMob.RegionKey`; the quest
classes' own docs only explain it. Boss HP lives in `docs/BALANCE.md` §10.
The Skywatch Hood, Mantle and Boots are only paid when their armour sheets
shipped (`WardenIdentity.armorSheetsExist`).

**Every ask is region-exclusive, and that was checked rather than assumed.**
§B1 wants a region's quest to be a reason to go to that region, so each of the
ten materials was traced to every one of its drop sites in the source and to
every recipe that could produce it. All ten are drop-only (no recipe makes any
of them) and every drop site of each sits inside its own realm's package.
**Ectoplasm was rejected for exactly this reason**: it looks like the
Aftergarden's signature material, but it is a *vanilla* item
(`ItemRegistry.java:929`, VERIFIED [jar]) that vanilla's own swamp hands out, so
a quest for it could be finished without ever entering the realm. Bonewood — the
mod's own, Ghost-only — replaced it.

**The key piece is not the unlock.** Turning the quest in gives you the object;
the unlock happens when you *place* it (`RegionKeyObject.placeObject` →
`SkywatchWorldData.unlockBossPortals`), and only inside a settlement
(`RegionKeyObject.canPlace`, the same shape `SeanceCircleObject` uses). Those
are two separate world flags on purpose — `regionKeysEarned` and
`bossPortalsUnlocked` — because collapsing them would either pay the reward
twice or delete §B2's whole "stand it up in your base" beat. Mining the key
piece afterwards does not re-lock the realm.

**Each key piece wears its own realm's portal sheet.** §B3 asks a summoning
stone to look like the key piece; that is a statement about two objects, and it
is only true if both read one file. `RegionKeyObject.SHEET_*` is
`BossPortalObject.SPRITE_*` spelled again. Every borrow, including the five
inventory icons, has a row in `docs/VANILLA_ASSET_MAP.md` §1.6/§1.6b with its
pixel size. No new art.

### Why the Warden gives these and not the Elder

§B1 says *"the reward of an Elder quest"*, and the vanilla Elder cannot be given
one. This was traced to the end before falling back:

- `ElderHumanMob.getQuests(ServerClient)` **returns `null`**
  (`ElderHumanMob.java:400-402`), `completeQuest` returns `false` (`:405-407`)
  and `skipQuest` returns `false` (`:410-412`) — all VERIFIED [jar]. That is the
  whole `ContainerQuest` seam `ShopContainer`/`ShopQuestsForm` would draw a
  quests tab from, overridden to nothing on this one mob.
- The mob cannot be swapped for a subclass either:
  `GameRegistry.register` throws `IllegalStateException` on a duplicate stringID
  (`GameRegistry.java:57-58`) and `GameRegistry.replaceObj` is `protected final`
  (`:71`), so `MobRegistry` exposes no override a mod can reach.
- His real *"quests with unique rewards"* are `StoryObjective`s, and
  `StoryObjectiveRegistry.registerObjective` **is** public and mod-callable. But
  `StoryObjectiveManager` only ever surfaces the first objective in registration
  order that is not both completed and claimed (`getCurrentObjective`, `:414`;
  `getVisibleObjectives`, `:486-494`), and a mod's objectives sort after all 24
  of vanilla's (`StoryObjectiveRegistry.compareTo`, `:197-203`, compares raw
  registry IDs). So the *ask* would stay invisible until a player had finished
  **and claimed** the entire vanilla story line through `defeatascendedwizard` —
  and only one region could ever be in progress at a time. A quest the player
  cannot see is `swh_beacon` again with extra steps, so it was not shipped.
  Reordering with `showBeforeObjective` was rejected too: that comparator is
  partial (it only answers for the one named neighbour) and re-sorting vanilla's
  own tutorial line around it is not a trade this slice is entitled to make.

So the giver is the mod's own Sky Warden — already its quest-giver, already the
source of the Ghost Chalk, and already holding the chain these five continue.
`advanceRegionKeys` runs on **every** conversation, settler or not, exactly like
`offerChalk`, and is gated on his own chain reaching `Chapter.DONE`: the keys
are what comes *after* "The Warden's Call".

**The co-op dead end this line was built to avoid.** The earned-record is
world-scoped (a key piece is a building, one per world — the same reading
`bossPortalsUnlocked` already takes). That means a second player can be left
holding a quest for a realm somebody else finished, and the only turn-in path is
guarded on that same record — the exact `swh_beacon` shape. So
`advanceRegionKeys` opens with a sweep that removes any held key quest whose
realm the world has already moved past, before it offers or turns in anything.

---

## A dead end this pass found and fixed

Both `EveleenMob.interact` and `KnottMob.interact` originally only ran their
chain-advancing logic (`advanceEdenChain` / `advanceDoorChain`) while
`!this.isSettler() && !this.isVisitor()` — on the theory that once someone
had moved in, their chain "was either already finished or moot."

That theory is false for both of them, and in a way a player could easily hit
by accident:

- Eveleen's recruit page is *always* available (only her **fee** is
  conditional on the chain), and it opens on the very same first meeting that
  hands out `EdenPlantsQuest` — one bubble line before it. A player who pays
  the ordinary 7000-coin fee on that first visit, before ever having an Eden
  Berry, Moon Melon or Sun Grape in hand, would recruit her immediately.
- Knott's own class doc is explicit that he is "recruitable the moment he is
  found" — by design, since his chain is meant to be independent of
  recruitment. A player who recruits him on first meeting, before gathering 5
  Reality Shards + 8 Warp Resin + 8 Strange Fabric, is an entirely expected
  sequence, not an edge case.

In both cases, once the settler moved in, `!isSettler()` became permanently
false, so `advanceEdenChain`/`advanceDoorChain` — the *only* code path that
can ever complete `EdenPlantsQuest` or `CrookedDoorQuest` — could never run
again. The quest would sit in the player's journal forever, with no way to
turn it in and no way to collect its reward: a live, reachable, registered
quest that becomes exactly as dead as `swh_beacon` the moment a very ordinary
player action happens first.

**The fix** (`mobs/EveleenMob.java`, `mobs/KnottMob.java`): drop the
`!isSettler() && !isVisitor()` guard from `interact()` entirely and let
`advanceEdenChain`/`advanceDoorChain` run on every interaction. Both methods
were already idempotent by their own internal guards — the world-flag check
at the top (`edenPlantsGiven` / `crookedDoorwayOpened`) and the
`SkyQuests.findHeld` check before issuing a fresh quest — so running them
unconditionally costs nothing and was always safe; the settler-state guard
was protecting against nothing. A player who recruits either of them early
and comes back later with the delivery in hand now completes the quest and
collects the reward exactly as if they had waited — they only lose Eveleen's
free-recruit perk, which she can no longer offer once she is already paid
for.

Eleanor was never at risk of the same bug: her STAY ending's cleanup
(`SkyQuests.removeAllOfType(..., EleanorQuest.class)` and the Spiritsteel Bar
payout) lives in `onRecruited`, a callback that fires on actual recruitment
regardless of `interact()`'s own guards — so recruiting her early is not an
edge case at all, it *is* one of her two intended endings.

## Deferred

- ~~No dedicated quest chain for Mortimer or Caspern.~~ **Built 2026-09-05**,
  above. §11 gives them only an arrival condition and no delivery quest; the
  case for inventing one is `docs/AREA_OVERVIEW.md`'s measurement, not §11.
- The Architect and the Hell chain (§16-§25) are a separate, later pass.
- Steinfeld has no ARRIVAL quest to match `swh_edenreach` and
  `swh_crookedarrival`. Both of those are handed out by a gate OBJECT the player
  uses, and Steinfeld has no gate — building one is a whole content family
  (object + item + recipe + art), so Ives hands out his own quest when found,
  the same way Eleanor does.
- The Reality Stitcher recipe that `swh_crookeddoor`'s materials are a down
  payment on is not built yet.

## The 2026-09-24 tuning pass

The player asked for a new glaive as a Skyreach quest reward and for the
quest rewards and chains to be fine-tuned. What changed, and why:

| quest | before | after | why |
|---|---|---|---|
| `swh_anchor` | Stormsteel Vambrace + banner + 5 petals | **Wolkengleve** + banner + 5 petals | The finale of the mod's first chain paid a trinket the player could already craft (6 Stormsteel + 4 Skyweave + 2 Aetherium) and that the Ghost key pays again. The glaive has no other source. |
| `swh_keyskyreach` | 1x Stormdisc | 4x Stormdisc | A Stormdisc is a set of four (`stackSize`), and `BoomerangToolItem.isEnchantable` refuses a partial stack: one disc could be thrown but never forged. |
| `swh_keyghostrealm` | Skyreave | Spiritsteel Reaver | The Skyreave (150, Skyreach craftable) was paid on the fourth rung, after the anchor's 165 Wolkengleve. The Reaver (176, enchant 2400) is the Ghost Realm's own weapon. |
| `swh_keycrookedbeyond` | Prismcaller | Gravewind Bow | The Prismcaller is the Skyreach's lowest-hitting craftable (118); the Gravewind Bow is Ghost tier (2400). |
| `swh_keyhell` | Thunderhead + Zephyr Harness + 10 Spiritsteel | 16 Spiritsteel | Thunderhead: a Skyreach craftable on the last rung. Harness: a duplicate of Knott's reward. 16 is what `HellKeyQuest`'s own doc always promised. No Hell-tier weapon exists yet; give this rung one when it does. |
| `swh_steinfeldvigil` | 10 Stormsteel | 12 Stormsteel | Paid the same as Eden's three fruit, one realm further out for 24 drops. |
| `swh_eleanor` | 14 Spiritsteel | 10 Spiritsteel | The table's outlier: more than Knott one realm further out, more than twice Mortimer/Caspern, more than the Ghost key. |

**Deliberately NOT changed.** `swh_edenplants` still asks for one of each fruit:
that is `WORLD_DESIGN.md` §5's own "collecting three Eden plants", and the
constitution outranks a tuning pass. The resident chains still pay more bars
than the region keys of the same realm: a key pays the key piece (which unlocks
a boss) plus a weapon and a wearable, a resident chain pays bars and a waived
fee, and that split is the one each class already documents.

**Old saves.** A world that finished `swh_anchor` before this pass got the
Vambrace. On the next conversation with the Warden it is handed the
Wolkengleve once, with its own line (`misc.wardenglaivecatchup`); the record is
`SkywatchWorldData.REWARD_CLOUD_GLAIVE` in the world's `residentChainsDone` set,
so it is paid once per world and `/swhreset quests` clears it with the rest.
The key-reward changes apply to keys turned in from now on; keys already turned
in keep what they paid (nothing is taken back). A held `swh_eleanor` or
`swh_steinfeldvigil` pays the new numbers when it is turned in — the reward is
paid by the NPC at turn-in, not stored in the quest.

**Status.** VERIFIED [jar] for the numbers (built, and the changed classes
read back). The hand-outs themselves are HYPOTHESIS until someone turns them
in on a real client; the integration test boots and generates but does not
drive a conversation.
