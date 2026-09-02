# Quests — the Eden, Ghost and Crooked chains

Three new HUD quest chains, five `Quest` classes in
`src/main/java/stairwaytoheaven/quest/`, each registered in
`QuestRegistry` from `StairwayToHeavenMod.init()`:

| registry id | class | chain |
|---|---|---|
| `swh_edenreach` | `EdenArrivalQuest` | Eden, step 1 |
| `swh_edenplants` | `EdenPlantsQuest` | Eden, step 2 |
| `swh_eleanor` | `EleanorQuest` | Ghost Realm |
| `swh_crookedarrival` | `CrookedArrivalQuest` | Crooked Beyond, step 1 |
| `swh_crookeddoor` | `CrookedDoorQuest` | Crooked Beyond, step 2 |

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

**Benchmark.** The Skyreach's own finale (`SpireCatsQuest` +
`AnchorDeliveryQuest`, driven from `SkyWardenMob`) pays a combined 10
Stormsteel Bar (cats) + a Stormsteel Vambrace (anchor). 10 Stormsteel Bar
matches that floor exactly — see `docs/BALANCE.md` §7 for why Stormsteel is
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
  stand-in, recorded in `docs/VANILLA_ASSET_MAP.md`) + 14x Spiritsteel Bar.
- **STAY.** Talk to her without Veil Essence selected — opens vanilla's own
  recruit page. Paying 5000 coins recruits her; `EleanorMob.onRecruited`
  removes the quest and pays the **same** 14x Spiritsteel Bar bonus, so
  neither ending is the materially poorer one.

**Benchmark.** 14, not 10 — Spiritsteel sits one tier above Stormsteel on
`docs/BALANCE.md`'s own gear ladder (34 chest armour / 2400 enchant vs. 29 /
1900), so "at or above" the Skyreach finale's 10 Stormsteel Bar means *more*
of the mod's own harder bar, not merely matching the count of an easier one.

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
one) + 12x Stormsteel Bar + 6x Reality Shard (a small seed fund back, for
whichever future pass builds the Stitcher).

**Benchmark.** Crooked Beyond sits at incursion tier 10 on
`docs/BALANCE.md`'s own realm ladder — the ceiling the Skyreach itself topped
out at before the mod's endgame rescale — so its chain is the largest of the
three this pass adds: 12 Stormsteel Bar, above the Skyreach finale's 10, plus
the Reality Shard seed fund on top of the trinket.

**Important: this chain is not a recruitment gate.** Unlike Eveleen, Knott is
recruitable from the moment he is found, regardless of whether the chain is
finished — §15 names no arrival or recruitment condition for him at all. The
quest chain is a second, independent reward track layered on top of
recruiting him, not a precondition for it.

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

- No dedicated quest chain for Mortimer or Caspern — §11 gives them only an
  arrival condition (a graveyard, an Aether Forge), not a delivery quest, and
  this pass did not invent one.
- The Architect and the Hell chain (§16-§25) are a separate, later pass.
- The Reality Stitcher recipe that `swh_crookeddoor`'s materials are a down
  payment on is not built yet.
