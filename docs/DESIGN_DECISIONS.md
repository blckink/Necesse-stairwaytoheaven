# Design decisions

Established invariants. An agent that believes one of these is wrong says so to
the user and waits — it does not quietly implement the opposite.

Each entry says what was decided and, briefly, why.

## World structure

**The whole sky world is ONE level.** `skylevel` / `skyreach2` is the only
modded level; every realm from Skyreach to Hell is a depth BAND on it, and the
bands overlap. *Why:* `WORLD_DESIGN.md` §3 sets biome WEIGHTS from
`realmDepth`, not zones — separate dimensions give hard borders, no gradient and
no anti-rush gate. This was implemented as six dimensions once and reverted on
2026-09-02; the law is in `docs/PLAN_ONE_PLANE.md`. **A change that adds a
`registerLevel` call breaks it.**

**Rushing is stopped by Soul Exposure, not by walls.** Standing in a band you
have not earned stacks a named, visible debuff. *Why:* §8 names the abuse case
— a blocked tile is defeated by a teleport, so the check is against the world
REGION. And a short step over the line must stay possible: that is how the
player learns the next realm exists.


**The Surface stays the player's main world.** Base, settlement, NPCs and
vanilla progression all live there. *Why:* the mod is an expansion, not a
replacement. A second full base splits the player's attention and makes the
vanilla settlement systems irrelevant.

**Skyreach is a persistent exploration layer, not a second main base.** The
player travels up, explores, fights, gathers, and comes home. *Why:* it keeps
the sky an event rather than a chore, and it means sky content can grow without
having to re-implement settlement systems up there.

**The Stairway is functionally a portal, not a coordinate ladder.** *Why:* a
coordinate ladder scattered players across an empty sky with nothing to find.
Routing everyone to one landmark gives the layer a centre.
**Do not revert this to Surface-coordinate-based routing.**

**Every Surface Stairway routes to the one canonical Old Warden Spire hub.**
The hub position is a pure function of the world-generation seed
(`worldgen/SkyOrigin`), so worldgen, the surface stairways and the spire
stamper all agree without shared state, and every player in a multiplayer world
arrives at the same place. *Why:* determinism without coordination, and a
shared meeting point.

**Skyreach progression expands radially from that hub.** Distance band drives
resource density and, later, spawn intensity. *Why:* travelling outward has to
pay, or there is no reason to leave the hub.

**Skyreach persists normally.** *Why:* it is a real part of the save.
**Deleting `skyreach.dat` and `levels/regions/skyreach/` must never become part
of a normal update.** A one-off manual deletion was accepted once, from a
backup, during the v0.5 transition. That was an exception and is over.

**Surface data is never touched by Skyreach migration.** Any migration code
that could reset Surface state is a bug, not a trade-off.

## The Warden

**Recruitment costs 30,000 coins.** *Why:* it is still the single largest NPC
purchase in the mod, but reachable well before the top vanilla
settlement-expansion tier — the Warden is the ENTRY to the Skyreach's content,
so a price that gates him behind endgame wealth gates the whole layer behind it.

*This value changed and the record did not follow it.* It read 100,000 here,
justified as matching the top expansion tier, while `SkyWardenMob.RECRUIT_COST`
had been 30,000 since `5ce05ae` — a deliberate change ("drop the Warden's fee to
30,000"), verified in that commit's integration run as `price=coinx30000`, which
updated `PLAYER_JOURNEY.md` and left this file, `CURRENT_STATE.md` and the code
comment above the constant all still explaining 100,000. Four records against
one number. The code was right; the records are corrected to it. `CHANGELOG.md`
keeps its 100,000 line because that is what v0.5 actually shipped.

**The payment IS the recruitment.** It is a real settlement recruitment that
turns him into a `HumanShop` settler on the Surface. **It is never the purchase
of a spawn item or a spawn egg.** *Why:* a spawn item makes him an object the
player owns; recruitment makes him a person who moved in.

**The recruitment runs through VANILLA's hiring flow, not our own.** He is a
`HumanShop` whose `getRecruitItems` returns the coin price; the shop container's
recruit page states it and `ShopContainer.payForRecruit` takes it, server-side,
only on the button press. Vanilla then teleports the mob to the settlement's
level and moves him in as a settler. **There is exactly ONE Warden mob in a
world** — recruiting him must never spawn a second mob anywhere.
*Why:* v0.5.0 hand-rolled the payment inside `interact()` and spawned a second
mob at home. That produced three player-facing bugs from one cause — coins
taken by talking with no dialogue option, the Warden "disappearing" and turning
up in the village as a stranger to be recruited again, and no bed assignment
possible in between. Do NOT reintroduce a bespoke payment path.

**A `HumanMob`'s settler key and a `Settler`'s mob ID are different
namespaces.** `HumanMob(hp, hp, key)` takes the `SettlerRegistry` key;
`Settler(id)` takes the `MobRegistry` ID. Getting the first one wrong silently
disables recruitment (`getSettler()` is null, vanilla answers `notsettler`),
which is exactly how the hand-rolled path came to exist. `/skyreachstatus`
asserts the live wiring per mob.

**He is a unique, permanent settler.** Modelled on the Elder: never spawns on
his own, never moves out, cannot be banished, no random replacement after
death, and excluded from vanilla's "one of every settler" achievement. *Why:*
he cost a fortune and he is a character, not a role.

**He becomes the progression interface** once settled. *Why:* the sky needs a
voice at home, or the player has no reason to come back to him.

## Characters

**Siggi and Peanut must never be permanently lost.** They are recurring
characters, not ambient critters. Immortality and save persistence are load
bearing and coupled — see `docs/TECHNICAL_LEARNINGS.md` before touching
`SpireCatMob`. *Why:* losing them permanently to a stray attack or a despawn
would be an unrecoverable loss of something the player is attached to.

**Their long-term home is with the recruited Warden**, once the player builds
cat furniture: roaming, resting, and eventually a small charming or useful
behaviour. Their existing progression state (`blackHome` / `tabbyHome`) is kept.
*Why:* it completes the story of bringing them home. It is not urgent enough to
justify risky architecture in a build the player is about to test.

**The PLAYER decides where that is, by placing a Cat Basket** (2026-08-28,
`feature/catbasket`, from the report in `docs/PLAYTEST_LOG.md`). A placed basket
is the cats' home on whatever level it stands, the newest one wins, and breaking
the active one returns them to the spire. *Why:* the player asked for the cats
to live in their town, and a home the mod picks is a home the player cannot
find. The record is a `WorldData` (`SkywatchWorldData`), tile **plus** level
identifier — a home in a Surface town is not a fact about the Skyreach and must
not live in that level's data. **Only a cat that has been coaxed home with a
Cloudpuff Treat ever moves**: a basket must never skip the quest step, or
finding Siggi and Peanut stops being worth anything.

## Scope

**The Veil exists but is not the current priority.** Do not expand it; do not
remove it. *Why:* the sky's first loop has to be good before a second layer
competes for attention.

**The playable experience leads.** This project has a tendency to expand faster
than the thing you can actually play improves. Prefer finishing a loop over
adding a system.

## Craft and content

**Assets are generated.** Every PNG in `src/main/resources/` comes from
`tools/asset_generator/`. Editing a PNG by hand desynchronises it from the
generator and the next regeneration silently reverts it.

**No franchise names, no private references in the repository.** Documentation
stays generally phrased. Style and tropes are fine; 1:1 copies of another
property's designs are not.

**`vanilla-sprites/` is local reference material and is gitignored on purpose.
It must never be committed.**

**Multiplayer must work.** Server-authoritative logic, per-player state keyed by
authentication, no client-trusting shortcuts.
