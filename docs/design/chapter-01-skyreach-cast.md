# Chapter 01 — the Skyreach cast

**Agent:** biome-designer · **Scope:** inhabitants and story for the Skyreach as
it already exists (+1). **No new biome.** The places themselves — footprints,
room plans, object lists — belong to `chapter-01-pois.md` (poi-architect), which
is being written in parallel. This file says *who is in them, what they want, and
why the player cares*.

The player's brief, verbatim: *"es fehlen weiterhin jegliche POIs, NPCs,
besondere Plätze, Häuser etc. es gibt nur die 2-3 POIs die aber nie besonderen
Loot haben oder neue Gegner oder irgendwas interessantes."* Three complaints, one
cause: the sky has buildings but no **inhabitants**, and therefore nothing that
wants anything from the player.

**Where it attaches.** Nothing here is a new region. Three existing sub-biomes
each gain one inhabited structure — Skyway Passages, Driftlands, Stormveil — all
reachable from the existing hub by the roads that already exist and are marked
KEEP. **Every settler found up here moves DOWN to the player's Surface
settlement**, per `DESIGN_DECISIONS.md` ("Skyreach is a persistent exploration
layer, not a second main base"). The sky is where you find people; home is where
they live. Nothing in this brief needs a recorded decision reversed.

Register discipline (`IMPLEMENTATION_RULES.md` §10): this is Skyreach language —
pastoral, luminous, aeronautical, dryly funny about lost post and failed flying
machines. It is **not** the Veil's gothic comedy and **not** the perfect-Heaven
register that belongs above the Skyway.

---

## 1. The three settler types

The mod registers exactly one settler today (`wardensettler`). These are the next
three. Each is a **named individual found at one POI**, recruited through
**vanilla's own recruit page** — the price is `getRecruitItems`, vanilla takes it
server-side on the button press, exactly as the Warden does now. No bespoke
payment path, ever (`DESIGN_DECISIONS.md`, and the three bugs it cost). Each is
excluded from vanilla's one-of-every-settler achievement, like the Warden.

Each recruit price is **coins + one key item that only exists in that POI**. That
is the design answer to "POIs never have special loot": at three of them, the
loot *is a person*.

### Magpie — the courier who kept the cargo
*Closest vanilla archetype:* the **Explorer** (the settler who leaves on
expeditions and comes back with goods), with the Pawnbroker's shop behaviour.

- **Wants:** a settlement to come back to that is not a customs office, and a
  market for the bonded cargo she has been sitting on since the Skywatch fell.
  She was never a member of the order — she was the courier they used.
- **Found:** the derelict **toll-house on the Skyway Passages**, hiding in the
  ledger room behind a vault she cannot open, because a Tollwright is standing on
  it.
- **Met and recruited:** kill the Tollwright, the vault opens, the **Bonded
  Lockbox** is hers — and she then buys it off the player as part of her own
  recruitment fee. Coins + the Lockbox.
- **Station:** the **Kite Rack** — a placed object in the settlement she departs
  from and drops her haul at. She does not craft; she goes away and comes back.
  Build it on vanilla's expedition mechanism if a settler can drive one; if not,
  the fallback is a processing station in the cheese-press shape that eats a
  Travel Chit and returns a Recovered Crate a day later. Either way the player
  sees: rack empty → Magpie gone → rack full.
- **Produces / sells:** a **stock that refreshes daily**, drawn from the vanilla
  shop pools of biomes the player is currently far from — desert, ocean, snow
  goods without the voyage — plus a rotating rare line (a spare Silver Bell,
  incursion-tier trinkets, furniture from settlements that are not yours). She
  also **buys**: she pays over broker for anything with a serial number on it,
  which is what makes the Ledger loop (§3) pay. She never takes from the player's
  own settlement, and that is a hard rule, not a joke to be implemented later.
- **Talks:** dry, clipped, never uses the verb. *"It was already falling. I merely
  arranged where."*

### Halda, the Cellarer — the last of the Skywatch's household
*Closest vanilla archetype:* the **Farmer** (works a station and sells what it
makes). Mechanically she is the settler who runs the cheese press.

- **Wants:** the **Mother** back — the living culture the Skywatch's cellar has
  been fed from since it was founded — and someone to drink with. She kept the
  stores after everyone else left, and stopped counting the years in years.
- **Found:** the **sunken grange cellar in the Driftlands**, under a collapsed
  brewhouse, among vats that are still warm.
- **Met and recruited:** one vat burst a long time ago and what grew out of it ate
  the culture. Kill the **Sourvat Bloom**, recover the Mother from inside it.
  Coins + the Mother. She does not haggle; she hands over the first barrel.
- **Station:** the **Skywatch Fermentation Vat**. Verified in
  `docs/research/registration-api.md` §2.1: vanilla's tech list has **no**
  brewing or fermenting station — COMPOST_BIN, GRAIN_MILL and CHEESE_PRESS are as
  close as it gets — so this needs a tech of our own, registered the way
  `SkyProfessions` already registers the loom, forge and kiln, and built like the
  **cheese press**: the settler loads it, walks away, and collects later.
- **Produces:** four brews and a by-product, in the mod's own material band.
  *Cloudberry Small Beer* (cloudberry + windwheat) — the everyday one, mild and
  cheap. *Windwheat Stout* (windwheat + nimbus milk) — the long food buff.
  *Stormveil Brack* (fulgurite + charwood-smoked windwheat) — storm resistance,
  endgame. *The Warden's Round* (aetherium + the Mother, which is never consumed)
  — the Skywatch's own buff, story-gated, the best consumable in the mod. And
  **Spent Grain**, which is animal feed: the sky troughs finally have a supply
  line that is not hand-picked cloudberries.
- **Sells / hands out:** kegs of all four so the player need not run the vat
  themselves, the vat's own object item, and a free Round each time a Warden
  chapter closes. Intent (flag for the integrator to verify against vanilla's
  happiness metrics): a served brew is a settlement amenity, so she raises
  happiness the way decoration does.
- **Talks:** warm, digressive, measures time in batches. *"That barrel is older
  than the road you came up on. Don't lean on it."*

### Ossian Vane, the Instrumentwright — expelled for arming the watch
*Closest vanilla archetype:* the **Mage** (a settler with a store that sells what
you cannot craft), with the Blacksmith's workbench relationship.

- **Wants:** to finish the work he was thrown out for, and a power supply. He made
  the spire's telescope and astrolabe; he also made the anchor. The order kept the
  instruments and kept the man out.
- **Found:** the **lightning-struck workshop and test range in the Stormveil**, a
  crater field of half-buried prototypes that all failed in interesting ways.
- **Met and recruited:** his own security prototype woke up and owns the building.
  Kill **Prototype Nine**, take the **Storm Lens Core** out of it. Coins + the
  Core. He is the most expensive of the three and says so is fair.
- **Station:** the **Aetheric Drafting Table** — again our own tech, again the
  cheese-press shape: the settler loads salvage (enemy drops, a broken sky weapon,
  aetherium / stormsteel / stormglass) and returns later with an
  **Aetherwright's Casing**. The table will not run without a Storm Lens Core in
  it; a second Core found later doubles its throughput, so the test range stays
  worth revisiting.
- **Produces / sells:** Casings, which are the gate material for the mod's first
  weapons **past** the Stormsteel/tungsten band — the tier the shipped arsenal
  stops at. His shop sells one rotating prototype from a fixed small pool,
  expensive, changing on a slow timer, plus the arsenal's consumables. Nothing he
  sells is craftable elsewhere.
- **Talks:** precise, over-qualified, allergic to the word weapon. *"It is a
  demonstrator. That it removes a golem is incidental to the demonstration."*

*Flag, not a change:* `DESIGN_DECISIONS.md` records the Warden's recruitment at
100,000 coins; `SkyWardenMob.RECRUIT_COST` is 30,000. These three should sit an
order of magnitude below whichever is true (roughly 5,000 / 8,000 / 15,000), so
the Warden stays the largest NPC purchase in the mod. Somebody who owns that
decision should reconcile the two numbers; I am not touching either.

---

## 2. Three enemies that belong to a place

None of these spawn on the open map. Each lives in one structure, and each is the
reason its POI is a fight instead of a container. All three should re-arm after
some days so the place is worth re-entering for its repeatable drop.

| Enemy | Role | Build on | Drops | Spawns |
|---|---|---|---|---|
| **Tollwright** | bruiser — slow, armoured, telegraphed slam, owns a room rather than chasing | vanilla's armoured golem melee archetype (`AshGolemMob`), the same family the Skystone Golem answers to | **Bonded Seal** (once), stormsteel-band scrap, tollplate | inside Skyway toll-houses and bonded warehouses only |
| **Sourvat Bloom** + **Vatlings** | ambusher — reads as scenery until you are close, then bursts and keeps releasing a swarm of floating adds | the Bloom on vanilla's ambushing plant (`StabbyBushMob`), the Vatlings on the small floating flake archetype (`CryoFlakeMob`, which `AuroraFlakeMob` already proves) | **the Mother** (once), Wild Skyyeast (brewing reagent), Spent Grain | cellars and brewhouses in the Driftlands |
| **Prototype Nine** | ranged — plays dead among the wrecks, then fights at distance with a winding arc | vanilla's caster archetype (`AncientSkeletonMageMob`, the pattern the Cinder Cantor already uses) | **Storm Lens Core** (once), **Aetherwright's Casing** (repeatable), scrap | the Stormveil test range and its prototype sheds only |

Between them: one bruiser, one ambush-plus-swarm, one ranged. Nothing in the
current roster fights like the second or third.

---

## 3. Unique loot — what a special place actually gives you

Eight rewards, each tied to one place, all at or past the Aetherium/Stormsteel
band where the shipped arsenal stops. None of them is a bigger number on an
existing item.

1. **Bonded Lockbox** — Skyway toll-house vault. Magpie's recruit key, and a
   one-time cache of another settlement's goods. *For:* unlocking the Thief.
2. **Skyway Writ** — toll-house ledger room. A permanent upgrade to Magpie's
   excursions: her daily stock reaches a further and richer class of settlement.
   *For:* making a settler get better, which nothing in the mod does yet.
3. **Ledger of Undelivered Post** — toll-house. Names a set of sky parcels
   scattered across the Skyreach; each one returned to Magpie pays out, and the
   full set pays a rare. *For:* a collection loop, and it finally puts the
   registered-but-unplaced `skyparcel` into the world.
4. **The Mother** — inside the Sourvat Bloom. Halda's recruit key, and the
   never-consumed ingredient the top brew needs forever, in the Silver Bell's
   shape. *For:* unlocking the Brewer and her best tier.
5. **The Warden's Round** — one aged barrel, deepest cell of the grange cellar,
   once per world. A single unique consumable with a long, strong Skywatch buff;
   afterwards only Halda can make more. *For:* the endgame consumable slot.
6. **Storm Lens Core** — inside Prototype Nine. Vane's recruit key, and the thing
   his Drafting Table will not run without; a second one doubles it. *For:*
   powering a station, and a reason to go back.
7. **Aetherwright's Casing** — the test range's prototype cache, then repeatable
   from Prototype Nine. *For:* the gate material of the first weapon tier past
   Stormsteel — the one the arsenal is currently missing.
8. **Skywatch Signet** — the spire archive, handed over when the household is
   whole. A trinket in the shipped accessory family that reveals unexplored
   Skyreach structures on the map within a radius. *For:* the "I cannot find
   anything up here" problem, as a reward rather than a UI feature.

---

## 4. The story spine

The Skywatch was never one man. It was a household with a road: a watch that kept
the sky, a cellar that fed it, a workshop that instrumented it, and a courier who
carried its post up and down. The Warden is the only one who stayed at his post,
and his chain — find the spire, hire him, bring the cats home, anchor the island —
is finished and player-approved; it is not being replaced. What it gains is an
ending that names the others. Once the island is anchored and the beacon burns,
the old road is passable again and the Warden admits, with visible reluctance,
that three people are still out there: the cellarer who stopped counting years,
the instrumentwright he had expelled, and the courier who kept the cargo. Each is
found in the sky and each moves down into the player's own settlement, so the
payoff of the whole Skyreach is not a bigger sword but a household reassembled at
home — where, according to the mod's own founding decision, the player actually
lives. The last chapter is the four of them under one roof, and the Signet.

One sentence per place, meaning only — layouts belong to the POI dossier:

- **Old Warden Spire** (exists, Driftlands): the only post never abandoned; its
  new job is to name the other three and to hold the archive that pays out the
  Signet. *Hooks: progression, storytelling, navigation.*
- **Skyway Toll-House** (Skyway Passages): the customs post on the road up, where
  cargo was bonded and never released, and where the Tollwright is still doing its
  job with nobody left to bill. *Hooks: encounter, loot, collection, an NPC.*
- **The Grange Cellar** (Driftlands): a brewhouse that fell in on itself with the
  vats still warm, and one cellarer who never left because the cellar still needed
  turning. *Hooks: encounter, harvest, an NPC, storytelling.*
- **The Test Range** (Stormveil): a crater field of failed flying machines around
  a workshop whose own security woke up and locked the door from inside. *Hooks:
  encounter, loot, progression, oddity.*
- **The Kite Mast** (Skyway or Driftlands, small): where the undelivered post
  actually ended up — a mast hung with parcels nobody signed for. *Hooks:
  collection, navigation landmark, oddity.*
- **The Anchor Terrace** (Driftlands, small, near the spire): where the household
  ate before it scattered — four chairs, one table, three of them dusty. *Hooks:
  storytelling, and the place the last chapter resolves.*

---

## What I did not decide

Layouts, footprints and object lists (POI architect). Sprites, sheets and palettes
(art agents). Class names, registry categories, numbers on buffs and exact prices
(integrator). Whether the Warden's recruitment cost is 30,000 or 100,000 — flagged
above, owned by whoever owns `DESIGN_DECISIONS.md`.
