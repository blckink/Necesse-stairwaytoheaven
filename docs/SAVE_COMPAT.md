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

## 0. First: which of the two questions are you asking?

Everything below this section is about a world that **already ran an older build
of this mod**. That is not the same question as installing the mod into a world
that has only ever been vanilla and other mods, and the answers are opposite.

| your world | what it is missing | what to do |
|---|---|---|
| never ran this mod | **nothing** | install it and play. `/swhreset` is not needed |
| ran an older build | whatever that build did not know about, everywhere it has been | §1 onward |

If your world never ran this mod, **§7 is your section** — skip §1–§6 entirely.
The short version is that the mod adds exactly one level, `skyreach2`, your save
does not contain it, and every part of the mod is created the first time it is
asked for rather than at world creation. §7 names each part and where it is
created; the run that measured it on two of the player's own saves is in
`docs/PLAYTEST_LOG.md`, 2026-09-07.

Everything from here to §6 assumes the other case.

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
losing its idempotence.

**What the `+N` counters actually measure, because it is easy to read them
backwards.** `retrofitArea` calls `ensureTilesAreLoaded` over the whole box
*first*, and only then takes `portalsBefore`. So generating the box has already
fired `onRegionGenerated` — and placed everything — by the time the baseline is
counted, and the repair loop afterwards finds its own work already done. A first
run on **ungenerated** ground therefore reports `bossportals=+0` and `nothing
was missing here` while standing up plenty of portals. `+N` means something was
missing from ground that already existed, which is the only case worth
reporting. (An earlier version of this paragraph claimed the opposite — that a
first run on a young world "legitimately places plenty" by the counter. It does
not, and a 2026-09-07 run on a mod-free save is what showed it: `regions=4225
bossportals=+0`, and `portals in 512 tiles of the spire: 1` immediately after.)

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

**Testing it without a person in the chair.** `scripts/save_compat_check.sh
<world-zip> [other-mods-dir]` copies a save, boots the dedicated server on the
copy, walks it through the sequence above and asserts what came out. It works on
an old mod save as well as on a mod-free one — §7 describes it in full.

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

### Sky you have already walked keeps the world it was generated with

This is the same rule seen from the other side, and it has a measured case.
On 2026-09-07 the realm-POI placement was fixed (11 of the 13 inhabited places
stood nowhere; see `docs/PLAYTEST_LOG.md` 2026-09-07 (3)). Running the census
on a **copy** of Friemliburg afterwards counts 13/13 queued — and the nearest of
them stamps 9 objects where a fresh world stamps 74, because the ground under it
was generated by the older build. `/swhreset world` does not repair that: it
generates ground that never existed and leaves existing ground alone, on purpose.

**So a worldgen fix reaches the sky you have not walked yet, not the sky around
your spire.** Nothing is broken by it and nothing you built is at risk; the new
places are simply further out.

### The three once-per-world places are the exception, and they reach every save

2026-09-10 moved the Skyway Toll-House, the Grange Cellar and the Test Range off
the lattice and onto `SkyLevel.ensureWardenSpire` (`SkyLandmarkPois`). That path
force-loads its own site, exactly the way the spire does, so **an existing save
does get all three** the next time anybody ascends or runs `/skyreachstatus` —
they are not "further out", they are stamped on demand.

**Which means the rule above does not hold for them, and this is the one place
in this document where something you built could be at risk.** The site is a
pure function of the seed and `SkyLandmarkPois.ensureAll` does not look at what
is standing there first; the toll-house in particular is `blank()`-built, so it
CLEARS its whole 23x19 footprint before writing, 180-700 tiles from the spire —
which is ground a long-running save has walked. The odds of it landing on
anything are small (the site test wants nine samples of land, and the odds of a
given rectangle holding a player's build are low), but they are not zero and
nothing checks. **Back the save up before the first ascent on a build newer
than 2026-09-10.** The named follow-up is a "walls already stand in this
footprint" pre-check in `ensureAll` that skips the stamp and the settler for
that one landmark.

Two more consequences worth knowing:

- **A save that already stood up a toll-house from the lattice keeps it.** It is
  built ground and nothing removes it, so such a world can hold that one plus
  the new once-per-world one. Only the lattice copy is legacy; the new path can
  never stamp a second, and Magpie is claimed once either way, so there is never
  a second Magpie.
- **The Skyreach lattice re-rolled which kind takes which cell.** Removing the
  toll-house from `REALM_KINDS[0]` took that band from sixteen kinds to fifteen,
  and `skyreachRotate` hands cells out by rank modulo the kind count. Ground you
  have already walked keeps whatever it generated; ground you have not will
  offer a different mix than a pre-2026-09-10 build would have.

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

**One measured caveat on the boss-portal row.** On the player's live world
(`Friemliburg`), `/swhreset world` walked its 4 225 regions, placed 23 mobs and
reported `bossportals=+0`, and the count around the spire stayed at 0. The
lattice says that box holds exactly one site. Either that site has no land under
it in the ground the older build painted — in which case the retrofit is behaving
exactly as this document says it does, because it never re-paints ground — or
`placePortalAt` has a defect on already-generated ground. Nothing in the current
debug commands can tell those apart; it needs a tile readout at the site.
Recorded in `docs/PLAYTEST_LOG.md`, 2026-09-07 (2). Until it is settled, read
this row as "yes, where there is ground to stand on".

---

## 7. Installing the mod into a world that has never had it

Everything above is about an **old mod save**. This section is the other
question, and it is the one a new player actually asks: *I have a world I have
played for weeks with vanilla and three other mods. Can I drop this mod in and
start it from the beginning without throwing that world away?*

**Yes, and nothing has to be repaired first.** No part of this mod is created at
world-creation time; every part is created the first time it is asked for. That
is not a policy statement, it is what each mechanism does:

| part | when it is created | why an existing world gets it |
|---|---|---|
| **the realm plane** (`skyreach2`) | `WorldGenerator.getNewLevel`, registered in `postInit` (`StairwayToHeavenMod.java:305-315`) | the level does not exist in the save; `world.getLevel` generates it on the first ascent. Necesse asks the registered generators for any identifier it cannot load |
| **`swh_realmpois`** and `swh_crookedhouse` | `WorldPresetRegistry.initRegion`, per 1024×1024 preset region **per level identifier** (`WorldPresetsRegion.getLevelRegionsFuture`) | the save holds `levels/presets/<identifier>/` only for identifiers it has visited. It has no `skyreach2` folder, so every sky preset region is computed fresh against the **current** registry |
| **boss portals, guard packs, residents, livestock herds** | `SkyLevel.onRegionGenerated` | it fires once per region *ever*, and no sky region has ever been generated in this world — so all of them fire, with today's content |
| **`SkywatchWorldData`, `VeilWorldData`, `SkyfallWorldData`** | lazily in their own `get(Server)`: `worldEntity.getWorldData(KEY)`, and a `new …()` + `addWorldData` when it comes back null | `WorldEntity.getWorldData` returns null for a key the save never wrote. All three handle that; none assumes it was seeded at creation |
| **`SkywatchQuestData`** (level data) | lazily in `get(Level)`, same shape | a fresh `skyreach2` has no level data at all |
| **the settlers who travel to you** | `Settler.addNewRecruitSettler`, called on every recruit-visitor roll of every settlement (`SkyArrivals`) | it is a runtime override read per roll, not a hook wired at world creation. An existing settlement starts offering them as soon as its gate opens |
| **the way up** | the Skyward Stairway is **craftable** — 8 tungsten bar + 15 quartz at the Tungsten Workstation (`SkyItems.registerRecipes`) | nothing has to have been placed by worldgen for the player to reach the sky |

**`migrateLegacySave` does not fire here.** It is called from
`applyLoadData`, i.e. only when there *is* saved quest data to read. A world
that never had the mod creates its `SkywatchQuestData` with `new`, which never
takes that path — "never existed" and "pre-0.5" are not confused.

### The one thing such a world does not get

The **three surface POIs** — Aeronaut Camp, Skyward Shrine, Sky Fragment Crater.
They are placed by `swhsurfacepois` into *surface* preset regions, and the save
already holds `levels/presets/surface/` for everywhere the player has been. They
appear only where the player has not walked yet. This is §6's "no — they paint
ground" rule, and `/swhreset world` does not help: it only walks the sky.

Everything else in the sky is new ground by definition, so the retrofit command
has nothing to do on such a world either. `/swhreset` on it reports
`stage=0 recruited=false`, which is the correct answer: the story has not
started, and the player is meant to play it from the top.

### And the other direction: taking the mod back out

Seen by accident while testing, and worth writing down because a player who
tries the mod may well remove it again. A world that carries this mod's world
data, opened **without** the mod, does not corrupt and does not refuse to load —
but for each of our keys it prints

```
(ERR) Could not instantiate world data with id swhskyfall
(ERR) java.lang.NullPointerException: ... WorldDataRegistry.getElement(String) is null
	at necesse.engine.registries.WorldDataRegistry.loadWorldData(WorldDataRegistry.java:43)
	at necesse.engine.world.WorldEntity.applyLoadData(WorldEntity.java:441)
```

and carries on with that data **dropped**, then saves the world without it. So
removing the mod is survivable for the surface world and destroys the mod's own
progress — the Warden record, the region keys, the fog ledger. Re-installing
later starts the chain again. That is vanilla's behaviour, not something this mod
can catch, and the stack trace is not a bug in it.

### Checking it, on a copy

`scripts/save_compat_check.sh` is this section as a script. It boots the
dedicated server on a **copy** of a world, optionally with the same mod set it
was played with, and asserts the table above rather than only reading it:

```bash
export NECESSE_GAME_DIR=/path/to/necesse-server-1-3-3   # the dedicated server
./gradlew buildModJar
scripts/save_compat_check.sh backup-saves-<date>/Player1/worlds/<World>.zip [other-mods-dir]
```

Two phases. **A** asks for the Skyreach (`skyreachstatus` — the same
`world.getLevel(skyreach2)` the stairway makes one call deeper), generates ground
out in the other realm bands (`edenstatus`, `veilstatus`), then walks the box
around the spire (`swhreset world`) and reads the report on both sides of it.
**B** restarts on the same world and walks the identical box again, which must
place nothing.

What it then checks: the plane and its regions reached the disk; the preset
catalogue's own records on `skyreach2` (`LevelPresetsRegion` writes each
generated preset's stringID, so this reads `swh_realmpois` and `swh_crookedhouse`
off disk rather than inferring them from tiles); a boss portal and a resident
claim exist where the plane is new; the surface half's region, player and
settlement file counts are unchanged; and no exception names this mod. Other
mods' errors are printed and **not** failed on — they are not ours to fix, and
failing on them would make the check useless in exactly the situation it exists
for.

It copies the zip before opening it and never writes to the original, so it is
safe to aim at a backup. Aim it at a backup. The runs that proved this are in
`docs/PLAYTEST_LOG.md`, 2026-09-07.
