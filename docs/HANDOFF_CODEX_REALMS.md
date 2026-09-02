> **SUPERSEDED 2026-09-02.** Codex took this handoff and finished it. Eden, the
> Ghost Realm and Crooked Beyond are built, integrated and green on
> `claude/aktueller-stand-offene-themen-k4ztas`, as are the Veil fog and the
> four new settlers; `wip/eden`, `wip/ghost`, `wip/crooked`, `wip/veilfog` and
> `wip/npcs` are fully absorbed and can be deleted. Only `wip/steinfeld` still
> holds work that is not on the branch.
>
> What is actually built now is in `docs/OVERVIEW.md`, which is written from
> the code. The art still owed is in `docs/ASSET_REQUESTS.md`. The rules below
> (no new pixel art, vanilla by literal path, never recolour at load time, a
> smaller realm that builds beats a bigger one that does not) still hold for
> every future pass, which is why this file is kept rather than deleted.
>
> One correction learned since: run the locale audit as
> `python3 tools/locale_audit.py --vanilla vanilla-sprites`. Without the dump
> it cannot see vanilla's own resources and reports every legitimate borrowed
> texture as missing -- 27 phantom errors on the first green build.

# Handoff to Codex — finish the four realms, the Veil fog and the settlers

**Written 2026-09-02.** Six parallel agents were building this; all six were
killed mid-flight by an API spend limit. Every one of them had done real work
and none of it was lost — it is committed and pushed as six `wip/*` branches.

Your job is to finish them and merge them. Nothing here needs redesigning.

---

## Where everything is

Base branch, green and pushed: **`claude/aktueller-stand-offene-themen-k4ztas`**

```sh
git fetch origin
git checkout claude/aktueller-stand-offene-themen-k4ztas
git branch -a | grep wip/
```

| branch | what is on it | lines | state |
|---|---|---|---|
| `wip/crooked` | **Crooked Beyond** as its own realm | 3 632 | closest to done |
| `wip/ghost` | **Ghost Realm / Aftergarden** | 3 034 | close |
| `wip/eden` | **Garden of Eden** | 2 094 | needs its Level + Realm |
| `wip/steinfeld` | **Steinfeld / The Quiet Reach** | 1 656 | needs mobs + Realm |
| `wip/veilfog` | **Soul Exposure** fog debuff | 1 628 | close |
| `wip/npcs` | settlers who arrive, with professions | 1 491 | close |

Each branch's last commit is a `WIP:` commit made by the coordinator so nothing
was lost. **None of them is known to compile.** Assume nothing.

---

## The rules every one of these was built under

Break these and the player will reject the work.

1. **Do NOT draw or generate pixel art. Do NOT touch `tools/asset_generator/`.**
   Every sprite is either an existing file under `src/main/resources/` or a
   VANILLA texture loaded by literal path — `GameTexture.fromFile("mobs/x")`
   resolves the game's own resources (see the header of
   `livestock/SkyPelt.java` for why). If neither fits, leave the thing out and
   record it as deferred. The player supplies custom art afterwards, from the
   deferred list.
2. **Never recolour at load time.** `SkyPelt.tint*` is legacy; the player has
   forbidden it for new content.
3. **No farm animals.** Chickens were deliberately cut. Do not add livestock.
4. **A smaller realm that builds beats a bigger one that does not.** If a piece
   is broken or half-designed, delete it cleanly and list it as deferred.
5. Every balance number carries a comment naming its vanilla analogue and the
   measured value. See `arsenal/RimeSentryMob.java` for the house style.
6. Verification language (`docs/IMPLEMENTATION_RULES.md` §14): `VERIFIED [jar]`
   for anything read out of the decompiled game, `VERIFIED [run]` for anything
   observed in an automated run. Never "player confirmed".

---

## What each branch still needs

Read the branch first — these are the known gaps, not a full list.

### `wip/crooked`
Has `CrookedLevel`, `CrookedRealm`, `CrookedTerrainPainter`, three biomes
(Checkerworks, Spiral Fields, Striped Waste), tiles, mobs, objects and three
POI presets (`InvertedHousePreset`, `DoorYardPreset`, `LongTablePreset`).

- Wire `CrookedRealm.register()` / `registerBiomes()` / `registerTiles()` /
  `loadTextures()` into `StairwayToHeavenMod` where the existing realms are called.
- Level id `crookedlevel`, identifier `crooked2`, dimension index **+5**.
- Keep the existing `outlands` biome in the Skyreach as the foreshadowing rim.
- Brief: `docs/WORLD_DESIGN.md` §13–§16, §A3.6. Theme: *reality no longer works properly.*

### `wip/ghost`
Has `GhostLevel`, `GhostTerrainPainter`, three biomes (Aftergarden, Bone
Orchard, Ectomarsh), tiles, seven mobs, Spiritsteel armour, objects and
`MausoleumPreset`.

- **Missing: `GhostRealm.java`** and two more POI presets.
- Level id `ghostlevel`, identifier `ghost2`, dimension index **+4**.
- Soul Loom and Spirit Forge should be settler-operable the way
  `settlement/SkyProfessions.java` builds the existing three workstations. If
  that is large, defer it.
- Brief: §10–§12, §A3.5. Theme: *life is gone.* Palette is petrol, turquoise,
  violet, poison green, black, cold white — **spooky but NOT grey**. Getting
  that wrong is the main way this realm fails.

### `wip/eden`
Has `EdenTerrainPainter`, three biomes (Garden, Shallows, Canopy), tiles, five
hostile mobs, `EdenPressure`, `EdenTiers`, `EdenSpawnRules`.

- **Missing: `EdenLevel.java`, `EdenRealm.java`, and every POI preset.**
  Copy `level/SkyLevel.java` for the Level; copy
  `worldgen/CrookedHousePreset.java` for the presets. Two presets is enough.
- Level id `edenlevel`, identifier `eden2`, dimension index **+2**.
- Art already in the tree and supplied by the player:
  `tiles/overgrowneden_splat.png`, `items/overgrownedenseed.png`, and
  `tiles/beetleground_splat.png` — the last is NEW, shares overgrowneden's exact
  alpha mask (83 428 opaque pixels in both) and **nothing reads it yet**. It is
  a green flowering ground and it is Eden's if Eden wants it.
- Brief: §5, §A3.3. Theme: *perfect nature* — not "green heaven" but an
  exaggerated biological explosion.

### `wip/steinfeld`
Has `SteinfeldLevel`, `SteinfeldTerrainPainter`, three biomes (Quiet Meadow,
Slab Fields, Grave Heath), seven tiles, `SteinfeldPressure`, `SteinfeldTier`.

- **Missing: the four mobs** (Lost Pilgrim, Stone Mourner, Hollow Angel, Grave
  Crow), `SteinfeldRealm.java`, and the POI presets.
- Level id `steinfeldlevel`, identifier `steinfeld2`, dimension index **+3**.
- Brief: §7, §A3.4. Theme: *order decays.* The GRADIENT is the identity — near
  Eden still green with broken angel statues, far out pale grass, slabs,
  gravestones and fog. It must be visible in the terrain painter.
- §7's ghost world event (unattackable ghosts that walk toward a grave, a door
  or the map edge, steering the player to the Veil) is worth building if it is
  cheap on vanilla's event machinery — `surface/SkyfallWorldEvent` is the
  worked example. Otherwise defer it.

### `wip/veilfog`
Has `SoulExposureBuff`, `VeilFogBuff`, `VeilRegion`, `VeilWorldData`,
`VeilGate`, a `VeilMarkCommand`, locale and doc edits.

- `docs/WORLD_DESIGN.md` §8 is the spec. Four bands: 0–3 s vision, 4–7 s slow,
  8–12 s health drain, 12 s+ massive damage. A short step in is possible;
  running through is not.
- **The abuse case §8 names explicitly:** do NOT merely block tiles — the check
  is against the **world region**, so teleporting past the edge does not help.
- The debuff must be visible and named. An unexplained health drain reads as a bug.

### `wip/npcs`
Has `SkySettlerMob`, `SkyArrivals`, `VeilResidents`, and four new NPCs:
`EveleenMob`, `MortimerMob`, `CaspernMob`, `EleanorMob`.

- Three asks: settlers who **arrive** the way vanilla's do; **professions**
  actually assignable in a settlement; and those four NPCs with a shop and a
  home region each.
- Names come from `docs/WORLD_DESIGN.md` §5 (Eveleen, Eden Botanist) and §11
  (Mortimer the **Undertaker**, Caspern the Spirit Smith, Eleanor the Lost Soul).
  `docs/WORLDBUILDING_LOOP.md` names a different Veil cast — it is out of date.
  **`WORLD_DESIGN.md` is the player's constitution and wins.**
- Eleanor has two endings in §11: pass on (strong trinket) or stay (recruitable).

---

## Suggested order

Merge conflicts are mechanical but real: `wip/crooked`, `wip/ghost`, `wip/eden`
and `wip/steinfeld` all edit `SkyRegistry.java`, and `wip/veilfog` and
`wip/npcs` both edit `StairwayToHeavenMod.java` and the locale files.

1. `wip/veilfog` — smallest surface, no realm dependency.
2. `wip/npcs` — depends on nothing being built yet.
3. `wip/crooked` — the most complete realm; it establishes the pattern the
   other three should match.
4. `wip/ghost`, then `wip/eden`, then `wip/steinfeld`.

Finish each ON ITS BRANCH until its gates are green, then merge into
`claude/aktueller-stand-offene-themen-k4ztas` and re-gate. Do not merge a
branch that does not build.

---

## Gates

Per branch, before merging:

```sh
export NECESSE_GAME_DIR=/opt/necesse-server/necesse-server-1-3-2-24650233
./gradlew buildModJar                       # exit 0
python3 tools/locale_audit.py               # must print OK
python3 tools/content_ledger.py --check     # must print OK
python3 tools/tile_behaviour_audit.py
python3 tools/asset_generator/generate_assets.py   # exit 0 -- the CONVERTED guard
```

Once, on the merged result, and only once because it boots a server twice:

```sh
scripts/integration_test.sh > /tmp/itest.log 2>&1; echo $?
grep -c FAIL /tmp/itest.log
```

**Never pipe a gate into `head` or `tail`** — you read the pipe's exit code and
the failures scroll away. Redirect to a file and read the file. That mistake has
cost this project several runs.

One known flake: the `spawn check: … dark=0/6` assertions depend on how lit the
probe tiles happen to be in the second pass. If exactly those fail, re-run once
before believing them.

---

## Locale

Every registered id needs a line in **both** `src/main/resources/locale/en.lang`
and `de.lang`. `tools/locale_audit.py` names precisely what is missing — run it
and obey it rather than guessing the key shape.

---

## What to hand back

The player needs a shopping list to draw art against. For each realm, write
`docs/realms/<name>.md` containing:

1. Every registered id, grouped by kind (tiles, objects, mobs, items, biomes).
2. A table `what it is | source path | what it stands in for` for every borrowed
   sprite — vanilla or reused-from-elsewhere.
3. What was deferred and why.

Then update `docs/OVERVIEW.md` — it is the one status document written from the
code rather than from prose, and it is what the player reads.
