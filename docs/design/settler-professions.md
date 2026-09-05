# Settler professions — eight special tasks the game does not have

**Scope:** new *special tasks* for settlers, at endgame tier, in the mod's own
realms. Not new vendors — the mod already has nine of those. One of the eight
is built (§4.1, Sky Voyages); the other seven are costed against the same
verified hooks and are ready to be picked up in order.

**The bar this document holds itself to.** Vanilla ships nine special tasks:
the Farmer fertilises, the Hunter hunts, the Animal Keeper does husbandry, the
Mage enchants, the Angler fishes, the Explorer explores, the Stylist restyles,
the Miner mines, the Trader trades. Everything else — hauling, crafting,
forestry, farming — every settler does. **Eight of this mod's nine settlers
borrow one of those nine** (`docs/settlers.md`): Eveleen calls
`enableProfession("fertilize")`, Mortimer `"hunting"`, Eleanor `"husbandry"`,
Magpie and Knott `"tradingmission"`. A tenth settler with a tenth borrowed job
would not be a new profession. Everything below adds work that did not exist
in the game before the mod was installed.

---

## 1. What a special task can actually be, read out of the 1.3.2 jar

Four shapes, and the cost of a proposal is decided almost entirely by which
one it needs. All four were checked against the decompiled server, not
assumed. **VERIFIED [jar]** throughout.

### Shape A — a new expedition category

The settler leaves the settlement, is gone for a while, and comes back with a
pack full of something.

| what | where | open to mods? |
|---|---|---|
| the mission itself | `SettlerExpedition` subclass | yes, plain abstract class |
| registering it | `ExpeditionMissionRegistry.registerExpedition` | yes, public static |
| the category name | `ExpeditionMissionRegistry.categoryDisplayNames` | yes, public static `LinkedHashMap` |
| who may be sent | `HumanMob.canDoExpedition` / `getPossibleExpeditions` | yes, overridable (`MinerHumanMob.java:146`) |

No client code and no new job type: vanilla's `expeditions` job type is
enabled for every settler by default (`JobTypeRegistry.java:23` passes
`defaultDisabledBySettler = false`), so `canDoExpedition` alone decides who
can go. The mission board iterates **every** registered expedition
(`MissionBoardContainer.java:359`) and groups them into buttons by category
(`MissionBoardContainerForm.java:459`), so a mod category appears as a fourth
button with nothing written for it.

**The one hard constraint, and it kills a design already on the roadmap:** a
settler only walks out on a posted mission when the settlement has a mission
board, and `ServerSettlementData.getMissionBoardTile` (:1499) checks the
object's string ID against the literal `"missionboard"`. **A modded object can
never be a mission board.** The Kite Rack that
`docs/design/chapter-01-skyreach-cast.md` proposes as Magpie's departure
station therefore cannot be one; sky voyages ride on vanilla's own board, and
the Rack has to become decoration or a processing station if it is ever built.

*Cost:* one class plus locale. Fully provable on the dedicated server.

### Shape B — a new job type with a new level job

The settler does work **inside** the settlement, on a schedule, at a priority
the player sets.

`JobTypeRegistry.registerType(stringID, new JobType(canChangePriority,
defaultDisabledBySettler, name, tip))` accepts mod types — the registry is
open through every mod's `init()` and closes with the rest afterwards
(`GlobalData.java:347`). The second flag is the profession mechanism itself:
`true` withholds the job from every settler until one of them turns it off for
itself, which is all vanilla's Farmer, Hunter, Animal Keeper, Angler and
Trader each do in one line. `SettlementWorkPrioritiesForm.java:95` filters on
nothing but `canChangePriority`, so **a mod job type appears in the settlement
work-priority list by itself**, with its own name and tooltip.

Then `LevelJobRegistry.registerJob(stringID, jobClass, handlerGenerator,
jobTypeStringID)` files the actual work under it.

*Cost:* a job class with a working `JobSequence`, which is the real work. The
job also has to be **published** by something — see shape C.

### Shape C — who publishes the work

The finding that makes shape B affordable at all. Job publishing is **not**
hardcoded in `ServerSettlementData.tickJobs`. Vanilla publishes from three
places, and a mod can use all three:

| publisher | vanilla example | what a mod would do |
|---|---|---|
| a **work zone** the player paints | `SettlementFertilizeZone.java:29`, `SettlementForestryZone.java:94` | `SettlementWorkZoneRegistry.registerZone` is public; the zone class needs three constructors (no-arg, `LoadData`, `PacketReader`) |
| a **tile** | `LiquidTile.java:157` returns the fishing job for every water tile | a mod tile returns its own job from `getLevelJobs` |
| a **mob** | `CritterMob.java:60` builds its own `HuntMobLevelJob` | a mod mob publishes work about itself |

A painted zone is the strongest of the three for a profession, because it is
the thing that reads to the player as "this is where my specialist works" —
the same as vanilla's fertilize zones.

*Note:* `SettlementWorkZoneRegistry.registerZone` throws for client-only mods.
This mod is not client-only (`clientside false`), so that is not a constraint
here.

### Shape D — a workstation

Not a special task at all. `SettlementStorageManager.assignWorkstation` takes
anything implementing `SettlementWorkstationObject`, and the resulting work
files under the vanilla **crafting** priority every settler already has. This
is what `SkyProfessions` already does for the loom, forge and kiln. Listed
only so proposals are not costed as shape B when shape D would do.

---

## 2. What the mod's own design already asked for

Two documents want professions and neither has one:

- `docs/WORLD_DESIGN.md` **§27** is a table of nine NPCs against jobs, and
  every job in it is a vanilla one. It ends with *"do not invent an own AI
  unless necessary"* — which these proposals honour: shapes A, B and C are all
  vanilla's own machinery, not a parallel AI.
- `docs/WORLD_DESIGN.md` **§32** asks for Botanist / Spirit / Crooked /
  Infernal / Cross-Realm expeditions. That is shape A, and §4.1 below is it.

`docs/design/chapter-01-skyreach-cast.md` casts Magpie as the Explorer
archetype and Halda as the station-runner, and leaves both mechanisms as open
questions. §4.1 and §4.3 answer them.

---

## 3. The ladder, at a glance

Ordered by build cost, cheapest first. "Shape" refers to §1.

| # | profession | EN / DE | shape | realm | status |
|---|---|---|---|---|---|
| 4.1 | Sky Courier | Sky Voyage / Himmelsfahrt | A | all | **BUILT** |
| 4.2 | Reliquary | Séance / Séance | A | Ghost | proposed |
| 4.3 | Cellarer | Cellaring / Kellerarbeit | B+C zone | Skyreach | proposed |
| 4.4 | Aetherwright | Salvage / Bergung | B+C zone | Skyreach | proposed |
| 4.5 | Stormcaller | Weatherwork / Wetterdienst | B+C tile | Stormveil | proposed |
| 4.6 | Cartographer | Surveying / Vermessung | A | all | proposed |
| 4.7 | Doorkeeper | Doorkeeping / Türdienst | B+C zone | Crooked | proposed |
| 4.8 | Fogwright | Fogwork / Nebelarbeit | B+C zone | Ghost | proposed |

Every ID gets an English **and** a German name, per `AGENTS.md` 10.

---

## 4. The eight

### 4.1 The Sky Courier — Sky Voyages / Himmelsfahrten — **BUILT**

**Shape A.** A fourth expedition category beside vanilla's Expedition, Mining
trip and Fishing trip, with one voyage per realm the mod ships and a capstone
that needs the whole road open.

**Whose it is.** Magpie, the courier who kept the cargo — the character
chapter-01 already wrote for exactly this and then could not wire, because the
question "can a settler drive vanilla's expedition mechanism" was open. It
can, and she does.

**The ladder.** The Skyreach (900 coins), Garden of Eden (1800), The Quiet
Reach (2600), The Aftergarden (3600), Crooked Beyond (4800), The Long Round
(9000). Each pays that realm's own materials, drawn against a coin-equivalent
budget the same way vanilla's mining trips are, plus a happiness object —
rare, epic for the two deep realms, and the mod's only **legendary** on the
Long Round.

**The gate, and why it is endgame.** A realm is voyage-able once its key piece
has been built, which is `SkywatchWorldData.bossPortalsUnlocked` — the record
`docs/FOGKEY_AND_BOSSPORTALS.md` §B2 already keeps. Vanilla's own expeditions
gate on `storyProgressSuccessChance`, which can only see vanilla's major story
objectives and cannot tell that a player has opened the Ghost Realm. The
success curve is vanilla's shape though: a run into the tier you have only
just opened is risky and gets safer as you climb past it (0.7 → 0.85 → 1.0,
one notch above vanilla's 0.6/0.8/1.0 because our entry rung has no cheaper
rung beneath it).

**Why it is new wind and not a reskin.** It is the only thing in the mod that
turns a realm the player has *finished* into a renewable income without
walking there — which is the §40 problem ("the player must keep RETURNING to
earlier realms") answered by a settler instead of by travel time. `RealmDepth`
notes that the world is 6000 tiles deep and the mod has no travel system;
until it does, the courier is the travel system.

**Open question for the user, deliberately not decided:** Magpie keeps her
trading missions as well as this. Splitting them — Magpie voyages, Knott
trades — would give the two couriers distinct identities and costs one line in
each constructor, and `TypePriority.disabledBySettler` is not saved, so it
needs no save migration. Taking a job away from a settler somebody paid 12,000
coins for is a decision, not a side effect.

Built in `settlement/SkyVoyages.java`; see that class for the full mechanism.

---

### 4.2 The Reliquary — Séance / Séance

**Shape A**, second category. **Ghost Realm.**

The settler does not travel — she sits at a Séance Circle inside the
settlement and calls. Mechanically an expedition (she is away, then she is
back with a pack), narratively the opposite of one, and vanilla cannot tell
the difference: `SettlerExpedition` never says the settler goes anywhere.

**What she brings back.** Not materials. **Names.** Each séance returns one
sealed keepsake belonging to somebody who died in the realm the circle was
tuned to — a Ghost-tier trinket, a page of the Ledger of Undelivered Post
(chapter-01 §3.3, currently a design with nothing behind it), or a lead: the
map coordinates of one un-entered POI in that realm. That last one is the
Skywatch Signet's job (chapter-01 §3.8) as a repeatable service rather than a
one-off trinket.

**The gate.** The circles already stand at fixed worldgen positions since
2026-08-31 and now have the Storm Sovereign behind them. A séance costs Veil
Essence rather than coins — `SettlerExpedition.getBaseCost` is coins only, so
the cost lands as an *availability condition* on stored Veil Essence instead,
which `isAvailable(ServerSettlementData)` can ask the settlement's storage
directly.

**Who.** Eleanor on the STAY ending, or a new resident. Eleanor is the better
answer: it gives the STAY ending a mechanical payoff beyond husbandry, which
is currently the thin half of a choice `WORLD_DESIGN` §11 wants to feel even.

*Cost:* one expedition class, one settlement-storage predicate, locale. No new
mechanism. **This is the next one to build.**

---

### 4.3 The Cellarer — Cellaring / Kellerarbeit

**Shape B + C (zone).** **Skyreach.** Halda, and the answer to her Fermentation
Vat.

**The special task.** A painted **Cellar Zone**. Barrels standing inside it are
work: the Cellarer turns them, tastes them, and moves them on a real clock —
a barrel filled on day 40 is not ready on day 41. That is the one thing a
workstation (shape D) genuinely cannot express, and the reason this is worth a
job type rather than another cheese press: vanilla's processing stations
finish on a tick counter that runs whether or not anyone is competent, and
brewing that nobody tends is just a slow furnace.

**Job type** `cellaring` / *Kellerarbeit*, `defaultDisabledBySettler = true`.
The zone publishes one job per barrel that is due, exactly the way
`SettlementFertilizeZone` publishes one per fertilisable crop.

**Endgame payoff.** Chapter-01 already specifies the four brews and the
never-consumed Mother. The endgame rung is **The Warden's Round** — story-gated,
the best consumable in the mod — and the by-product is **Spent Grain**, which
is animal feed, which is the supply line the sky troughs do not have. A
profession that feeds two other systems is worth more than one that ends in an
item.

**Why it is new wind.** It is the first thing in the game that makes *time* a
settlement resource. Necesse has no ageing anywhere.

*Cost:* job type, zone class (three constructors), job class with a sequence,
a barrel object with state. The largest of the eight. Build it after 4.4 has
proven the zone shape.

---

### 4.4 The Aetherwright — Salvage / Bergung

**Shape B + C (zone).** **Skyreach.** Ossian Vane, and the answer to his
Drafting Table.

**The special task.** A painted **Salvage Yard**. Wreckage the player drags
home — the sky is full of `skywatchrubble`, `aeronautwreck`, `skycache`,
`skycrate` — is broken down *in place* by the settler into parts, and parts
build the **Aetherwright's Casing**, the gate material for the first weapon
tier past Stormsteel that chapter-01 §3.7 names and the arsenal is missing.

**Job type** `salvage` / *Bergung*. The zone publishes one job per salvageable
object standing in it. The settler carries nothing to a station; the yard *is*
the station, which is what makes it a zone and not shape D.

**Endgame payoff, and the reason it is not just a recycler.** The yard is
throughput-limited by **Storm Lens Cores**: none and it does not run, one and
it runs, two and it doubles — chapter-01's own rule, which keeps the Stormveil
test range worth revisiting for a second Core forever.

**Why it is new wind.** It gives every piece of scenery in the Skyreach a
second life, and it is the only loop in the mod that rewards *hauling junk
home* — a verb the game already has and never pays for.

*Cost:* job type, zone class, job class. Simpler than 4.3 (no clock, no state
on the object). **Build this one first of the zone shapes**, as the proof.

---

### 4.5 The Stormcaller — Weatherwork / Wetterdienst

**Shape B + C (tile).** **Stormveil.**

**The special task.** A **Lightning Conductor** object, and a settler who
stands under it when the weather turns. Two effects, and the second is the
interesting one:

1. During a storm the conductor yields Fulgurite and Storm Shards — the
   Stormveil's own materials, farmed at home instead of walked to.
2. A tended conductor **grounds the settlement**: storm events stop damaging
   objects and stunning settlers inside its radius.

**Job type** `weatherwork` / *Wetterdienst*. Published by the conductor's own
object entity, i.e. shape C's mob/tile route rather than a zone — there is
nothing to paint, the work is one tile and a weather state.

**Why it is new wind.** It is the first defensive profession. Every vanilla
special task produces goods; this one prevents loss, and it makes weather —
which the Stormveil has and does nothing with — into a thing the player
prepares for rather than waits out.

*Cost:* job type, object with an entity, job class. The gate is weather state,
which is read-only and cheap.

---

### 4.6 The Cartographer — Surveying / Vermessung

**Shape A**, third category. **All realms.**

**The special task.** The settler is sent to *survey* rather than to fetch, and
comes home with knowledge: unentered POIs of one realm marked on the player's
map, plus one Skyway Writ-style permanent upgrade per realm fully surveyed.

**Why it exists.** The player's own complaint, quoted verbatim in
chapter-01: *"es fehlen weiterhin jegliche POIs, NPCs, besondere Plätze,
Häuser etc."* — half of which is a discovery problem, not a content problem.
The realm POI system now places 14 kinds of site across six bands and a player
walking 6000 tiles will meet a fraction of them. Chapter-01 §3.8 already
proposes the Skywatch Signet as a one-off answer; this is the repeatable one,
and it is a **reward** rather than a UI feature, which is the form that
document explicitly asks for.

**Endgame rung.** A fully surveyed world unlocks the Long Round's map: every
boss portal, every settlement, every gate, on one screen.

*Cost:* one expedition class plus a map-marking reward, which is the only part
that is not already proven — rewards are `List<InventoryItem>`, so "reveals
map" has to be an item that does it on use rather than a direct effect. That
is a normal item, not a new mechanism.

---

### 4.7 The Doorkeeper — Doorkeeping / Türdienst

**Shape B + C (zone).** **Crooked Beyond.** Mr. Knott, and the thing his shop
is currently a placeholder for.

**The special task.** A **Door Yard** zone in the settlement. Crooked doors
standing in it are maintained by the settler, and a maintained door is a
working one: a two-way link between the player's settlement and one realm's
landing. The settler is not a portal — the settler is the *reason the portal
keeps working*, and an untended yard degrades back to inert doors.

**Job type** `doorkeeping` / *Türdienst*.

**Why it is new wind, and the caution that comes with it.** `RealmDepth`'s own
note: the world is 6000 tiles deep, deliberately *half* what §42.1 first
sketched, and the reason recorded there is that the mod **has no travel system
at all yet** — with the explicit instruction *"revisit this the day travel
lands."* This is that day, and it should therefore be built **last of the
eight**, because it changes the size the world should be. It is the single
biggest change to how the mod plays that any of these proposals could make.

*Cost:* job type, zone, job, a door object with a linked destination, and a
balance pass on `DEPTH_SCALE`. Not a first pass.

---

### 4.8 The Fogwright — Fogwork / Nebelarbeit

**Shape B + C (zone).** **Ghost Realm.**

**The special task.** The Veil's fog is currently something the player carries
chalk against. A **Warding Ring** zone lets a settler hold it back: inside the
ring, fog does not accumulate, Gloom Shades do not spawn, and the ring's edge
is visible. Outside it, nothing changes.

**Job type** `fogwork` / *Nebelarbeit*. The ring is painted; the settler walks
its perimeter and renews it, and it decays if nobody does.

**Endgame payoff.** It is what makes a settlement *in* the Ghost Realm
possible at all, which nothing currently is — every settler the mod finds up
there moves down to the Surface, per `DESIGN_DECISIONS.md`. This is the
profession that would let that decision be revisited on the player's terms
rather than the engine's.

**Why it is new wind.** It is the only proposal that changes where the player
can *live*.

*Cost:* job type, zone, job, plus a fog-suppression hook in the realm's own
tick. Build after 4.4 and 4.3.

---

## 5. Build order, and why

1. **4.4 Aetherwright / Salvage** — the cheapest zone-shaped profession, and
   the proof that shape B+C works end to end. Everything below depends on that
   answer.
2. **4.2 Reliquary / Séance** — cheapest of all (shape A is already proven by
   4.1), and it pays off an existing thin choice.
3. **4.3 Cellarer** — the largest single build, and the one chapter-01 wants
   most.
4. **4.5 Stormcaller** — first defensive profession, small.
5. **4.6 Cartographer** — needs the map-marking item first.
6. **4.8 Fogwright** — changes where the player lives.
7. **4.7 Doorkeeper** — changes how big the world should be. Last.

## 6. What this document did not decide

Numbers on buffs, exact prices, class names, sprites, and whether Magpie keeps
her trading missions (§4.1). Those are the integrator's and the user's.
