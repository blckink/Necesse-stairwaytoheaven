# Save compatibility — playing an existing world, and testing it from A to Z

**The short version.** The mod never breaks an old save. That is the policy
(`ROADMAP.md`, "Compatibility policy") and it is worth keeping. But it has a
price nobody had written down, and it is the price a tester pays:

| what an old save keeps | what that costs you |
|---|---|
| every quest flag it has ever set | the chain is finished and cannot be replayed |
| every region exactly as the build that generated it left it | content added later is **missing from everywhere you have already been** |

`/swhreset` is the answer to both. It is ADMIN-only, its bare form changes
nothing, and its destructive forms want the word `confirm`.

```
/swhreset                  # report only — what this world holds, and nothing changes
/swhreset world            # retrofit ground an older build generated
/swhreset quests confirm   # put the whole chain back to before the first ascent
/swhreset all confirm      # both, plus clear the one-per-world resident claims
```

---

## 1. Why an old save is missing content, in one paragraph

`SkyLevel.onRegionGenerated` fires **once per region, ever**. Guard packs, boss
portals, residents and livestock herds are all placed from there. A region
generated in July gets July's content and never learns anything else, no matter
how long you play. The clearest case: **boss portals shipped on 2026-09-03**, so
a world walked before that date has none anywhere it has been — and the whole
boss ladder is therefore unreachable in exactly the part of the map the player
knows.

The repair is possible because every lattice in `SkyLevel` is a pure function of
the world seed and the tile. Walking a region again computes the same sites the
original generation would have, so the world ends up as if the content had
always been there. That is what `/swhreset world` does, and it is a retrofit
rather than a re-roll.

## 2. What each mode does

### `/swhreset` — status

Reports and changes nothing. Prints the story stage, the world flags, the region
keys earned, the boss portals unlocked, the resident claims, the fog and chalk
ledgers, how many journal quests are live across all players, which named
residents are standing in the sky, and how many boss portals are within 512
tiles of the spire.

This is the default on purpose: a destructive command whose zero-argument form
is destructive eventually eats somebody's world. The integration test asserts
that the bare form prints "reporting only".

### `/swhreset world` — retrofit

Re-walks the placement lattices over a **1024×1024 box** centred on you (or on
the Warden's Spire, if you are not in the sky), and places whatever the current
build says should be there and is not.

**It also generates any ground in that box that has never existed.** That is
deliberate — generation here is the same deterministic, seed-derived generation
that walking there would trigger, so the box ends up uniformly current instead
of half-repaired. It is also the reason the radius is bounded: about 4 200
regions, several seconds, all written to disk.

**What it repairs:** boss portals · guard packs · the named residents · the
livestock herds.

**What it cannot repair — and this is not a limitation to work around:** it
never re-paints ground that already exists. Terrain, POI presets and the
`WorldPreset` catalogue all write tiles, and tiles you have explored may be your
base by now. A building added after a region generated stays missing in that
region. The honest answer for buildings is to walk further out.

**It is safe to run twice.** Every placement refuses ground that already holds
its own work: `placePortalAt` will not overwrite an occupied tile, `placePackAt`
skips a site that already has a persistent hostile on it, `placeHerd` skips a
region that already has its herd, and both resident paths hold a one-per-world
claim. Run twice over the same box, the second run places nothing and says
`nothing was missing here`. The integration test runs it twice and asserts
exactly that, because it is the only assertion that would catch a placement
losing its idempotence. (A *first* run on a young world legitimately places
plenty — it is generating the box as well as repairing it.)

512 tiles is one call. Repairing a large explored world is several deliberate
calls from different places, not one command that walks an unbounded area.

### `/swhreset quests confirm` — replay the chain

Puts the sky-side story back to before the first ascent:

- `SkywatchQuestData` — stage, recruitment, both cat flags, the anchor, every
  world-map marker.
- `SkywatchWorldData` — the Warden record, Eleanor's ending, Eveleen's plants,
  Knott's doorway, the resident side-chains, **every region key earned and every
  boss-portal unlock**, the cats' home.
- `VeilWorldData` — every Veil Mark, every fog touch, **and the chalk ledger**.
  The three reset together or not at all: the Warden refuses a second chalk to a
  character he has already paid, so a world whose story was reset but whose
  chalk ledger was not would send the player to a Séance Circle they can no
  longer draw.
- Every `stairwaytoheaven.*` quest is removed from every player's journal, live,
  without anyone reconnecting.
- A Sky Warden is put back in the spire if the tower is empty.

**What it deliberately keeps:**

| kept | why |
|---|---|
| the spire's position and every coordinate | the tower is built ground and you may have furnished it; re-stamping the preset would overwrite your work |
| `returnStairs` — each player's way home | clearing it strands whoever is in the sky when the command runs. That is a trap, not progression |
| `basketPlaced` — the cat basket object | it is furniture with no recipe; re-placing it turns a quest reward into a ten-second farm |
| the resident claims (`residentsClaimed`) | see `all` below |

**What a reset cannot undo, because these are objects and items and not flags:**
a Warden who already lives in your settlement, a key piece already built, a
Stormsteel Vambrace already in a chest, a Séance Circle already drawn. The
command's report names every mod resident still standing so you can decide what
to do about them.

### `/swhreset all confirm`

`quests` + `world`, and additionally clears `residentsClaimed`.

**Read this before using it.** `residentsClaimed` is the record that stops a
world holding two Magpies — worldgen and the settlement-visitor roll are two
independent routes that cannot see each other, and that set is what makes them
see each other. Clearing it while the first Magpie is still alive lets worldgen
stand up a second one. Use `all` on a throwaway test world, or after removing
the residents, and use `quests` otherwise.

## 3. Testing the whole mod from an existing save

```
/swhreset                       # look first: what does this world actually hold?
/swhreset world                 # pick up whatever your build added since
/swhreset quests confirm        # and start the chain again
```

Then play it in order. Everything below is reachable from a reset world:

1. **Skyreach** — climb, find the Spire (`swh_findspire`), recruit the Warden
   (30 000), the cats (`swh_cats`), the anchor (`swh_anchor`).
2. **The fog and the chalk** — walk out until Soul Exposure stacks, then talk to
   the Warden: he hands the `ghostchalk` the Séance Circle is drawn from.
   `/veilmark` is the admin shortcut if you want the gate open without the walk.
3. **The region keys** — once the Warden's Call is `DONE` he offers them one at
   a time in boss-ladder order. Each pays a key piece; stand it **in a
   settlement** and that realm's boss portals wake up.
4. **The bosses** — five, one per realm, at their portals. `/swhreset` reports
   how many portals stand near the spire; if it says 0 on an old save, that is
   the retrofit's job.
5. **The realms' own people** — Eveleen in Eden, **Ives in Steinfeld**, Mortimer
   / Caspern / Eleanor in the Ghost band, Knott in the Crooked Beyond. Each of
   the five with a chain waives their recruit fee when you finish it.

`/skyreachstatus`, `/edenstatus`, `/veilstatus` and `/skysurfacestatus` report
what generated; `/swhreset` reports what progressed.

## 4. The one migration that runs by itself

`SkywatchQuestData.migrateLegacySave` — a **v1 (pre-0.5) save has no
`schemaVersion` field**, and its sky-side state used the old fetch-chain stage
semantics and a stairway-anchored spire position, which would leave the current
flow hard-stuck (old `stage >= 2` reads as "already recruited"). Opening such a
save resets the Skyreach-side quest state **once**, idempotently, so the
canonical-origin spire re-stamps cleanly. Surface levels, settlements and
inventories are not part of that object and are never touched.

`SkywatchQuestData.resetProgress` is the deliberate half of the same operation,
and `/swhreset quests` is what calls it.

## 5. The rule everything here obeys

`docs/DESIGN_DECISIONS.md`:

> **Surface data is never touched by Skyreach migration.** Any migration code
> that could reset Surface state is a bug, not a trade-off.

Nothing in `/swhreset` reads or writes a surface level, an inventory, a
settlement or a player's items. That is also why its "standing residents" report
says out loud that it only scanned the sky: a settler who moved into your town
lives on the surface, and this command does not look there.

## 6. Version history, so you know what an old save is missing

| shipped | content | retrofittable? |
|---|---|---|
| 2026-09-05 | Eden's guard packs (they had never been placed at all) | **yes** — `/swhreset world` |
| 2026-09-05 | Ives, Steinfeld's first resident | **yes** |
| 2026-09-05 | Ives's, Mortimer's and Caspern's quest chains | yes — they are handed out on conversation, not by worldgen |
| 2026-09-03 | boss portals, one lattice per realm | **yes**, and this is the big one |
| 2026-09-03 | the region key pieces and their five quests | yes — the Warden hands them out on conversation |
| 2026-09-04 | the 13 inhabited realm POIs | **no** — they paint ground |
| earlier | terrain, biomes, tiles | **no** — walk further out |
