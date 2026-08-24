# Design decisions

Established invariants. An agent that believes one of these is wrong says so to
the user and waits — it does not quietly implement the opposite.

Each entry says what was decided and, briefly, why.

## World structure

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

**Recruitment costs 100,000 coins.** *Why:* it equals the top vanilla
settlement expansion tier, so an endgame player pays a meaningful but reachable
lump sum. It is the single largest NPC purchase in the mod on purpose.

**The payment IS the recruitment.** It is a real settlement recruitment that
turns him into a `HumanShop` settler on the Surface. **It is never the purchase
of a spawn item or a spawn egg.** *Why:* a spawn item makes him an object the
player owns; recruitment makes him a person who moved in.

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
