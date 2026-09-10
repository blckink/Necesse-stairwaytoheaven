# The Fog Key and the Boss Portals

**The player's design, 2026-09-03.** This is the spec. It replaces every earlier
idea about realm gates as travel — **travel is already solved by vanilla's
Portal Flask** (`PortalFlaskItem` places a `HomePortalMob` pair and works on
every level, VERIFIED [jar]). Nothing here re-implements travel.

What was actually missing is a way **through the fog**, and a reason to go.

---

## Part A — Chalk, the Séance Circle, and the Ghost Guide

### A1. The chalk

`ghostchalk`, an item. **The Warden hands it over** the first time that player
has stood in Soul Exposure fog, and sells replacements from then on.

> **The multiplayer problem the player raised, and the answer.** "ein NPC der in
> der nähe spawnt" is a race in multiplayer: two players reach the fog together,
> one NPC spawns, one chalk. So the grant is **per player**, stored on the
> player's own client data, never in `SkywatchWorldData`. Every player earns
> their own piece the first time *they* touch fog. The Warden — who is already
> the mentor, already has a shop, already gave the Silver Bell — is the source,
> so no NPC has to spawn at all and nothing can be taken by someone else. A lost
> piece is never a dead end because he restocks it.

### A2. The Séance Circle, rebuilt

The circle stops being a teleporter. It becomes the thing you draw at home.

| rule | why |
|---|---|
| Placeable **only inside a settlement** | the player: *"zuhause in der basis (sonst geht es nicht)"* |
| **Minable with a pickaxe** | it is furniture, not a fixture |
| **Buyable from the Warden** as a replacement | a destroyed circle must not brick the run |
| Placed from `ghostchalk` | the chalk is consumed |

### A3. The Ghost Guide

Using the circle summons a **Ghost Guide** (`ghostguide`).

1. **First use — he unlocks you.** From then on the Ghost band's Soul Exposure
   does not apply to that player: you can walk through the fog. This is the
   whole point of the chalk.
2. **Every use after — he trades.** He sells **ghost weapons**, and he does not
   take coins. He takes:
   - valuables out of the Ghost region (ectoplasm, veil essence, spiritsteel), or
   - **high-quality cooked food the player made** (`Settler.FOOD_FINE`).
3. The same ghost weapons also **drop randomly in the Ghost region**, so a
   player who never trades still finds them.

---

## Part B — Elder quests, region keys, and the boss portals

### B1. The Elder gives the keys

Each region's **key piece** is the reward of an Elder quest tied to that region.
There may be **several per region**. The key piece is a buildable object:
Mr. Knott's red door for Crooked, a statue for Steinfeld, and so on.

### B2. Building the key unlocks that region's portals

Stand the key piece in your base and that region's **boss portals** unlock.
Before that they are inert.

### B3. The boss portals

- Scattered through worldgen, in their own region only.
- **Not minable.** Ever.
- They look like the region's key piece, so a player recognises what they need.
- Using an unlocked one **spawns the region's boss**, incursion-style, with
  valuable loot.

### B4. The boss ladder

Vanilla's own incursion bosses, VERIFIED [jar] from each `IncursionBiome`'s
`bossMobStringID`. Scaling is vanilla's own incursion curve
(`BiomeMissionIncursionData`, cumulative `healthScalingPerTier` /
`damageScalingPerTier`), applied per mob through a permanent buff carrying
`BuffModifiers.MAX_HEALTH` and `ALL_DAMAGE` — never through `LevelModifiers`,
which would buff the whole plane.

| tier | ×HP | ×damage |
|---|---|---|
| 8 | 3.18 | 1.87 |
| 9 | 3.58 | 2.00 |
| 10 | 4.00 | 2.15 |

| realm | boss | vanilla incursion | base HP | tier | final HP |
|---|---|---|---|---|---|
| **Skyreach** | `cryoqueen` | Snow Deep Cave | 18 000 | 8 | 57 240 |
| **Eden** | `moonlightdancer` | Moon Arena | 40 000 | 8 | 127 200 |
| **Steinfeld** | `ascendedwizard` | Settlement Ruins | 44 000 | 9 | 157 520 |
| **Ghost** | `pestwarden` | Swamp Deep Cave | 45 000 | 9 | 161 100 |
| **Crooked** | `crystaldragon` | Crystal Hollow | 52 000 | 10 | 208 000 |
| **Hell** | `mutanthydra` | Scrapyard | 80 000 | 10 | 320 000 |

> **The last column is now the tier curve alone, not what ships.** Since
> 2026-09-07 each row also carries a per-realm ascension uplift on top —
> ×1.30 / ×1.35 / ×1.40 / ×1.45 / ×1.55 / ×1.65 — so the six bosses actually
> walk out with **74 412 / 171 720 / 220 528 / 233 595 / 322 400 / 528 000 HP**.
> The uplift lives
> in `SkyBossLadder.Boss`, is derived in `docs/BALANCE.md` §10, and is checked
> against the built jar by `scripts/balance_check.sh`. The tiers below are
> unchanged: the uplift is a second factor, because vanilla's damage array runs
> out at tier 10.
**Hell's row was a reservation until 2026-09-10.** It read *"later / reserved"*,
because Hell had no painter and `SkyBossLadder.forRealm` answered `null` there.
`realms/hell/HellTerrainPainter` closed that hole, so the reservation became the
sixth real row — same boss, same base HP, at tier 10 rather than at no tier.
Tier 10 and not higher for the reason the note above gives: past ten, vanilla's
damage array is exhausted and a tier buys almost nothing but health, so Hell is
raised through the steepest uplift column instead (×1.65 health, ×1.36 damage,
the same pair its mobs carry).

Base HP is the CLASSIC world-difficulty column of each boss's
`MaxHealthGetter`. The ladder is monotone on purpose: 57k → 127k → 158k → 161k
→ 208k → 320k. The player's floor was *"mindestens Niveau der 1. Incursion"* and
*"grundsätzlich sollen die bosse auf incursion level 8-10 sein"*.

Other incursion bosses left unused for now, and why: `reaper` (11 000) and
`motherslime` (52 000) break the ladder or the theme; `sunlightchampion`,
`spiderempress`, `sageandgrit` and `nightswarm` are held for later regions.
`mutanthydra` was on that list and is not any more — it is Hell's row above.

### B4a. Coverage, measured

VERIFIED [run], from the headless integration test:

```
outlands check: ... portals=5/5 onland (2 drowned) biome=Beetle Outlands
```

Seven hashed portal sites in range, five on land, **all five carrying the
portal**, two lost to sea. That last number is what the lattice pays for being
a hash rather than a search: one site per 260x260 cell, and some cells are
mostly ocean. It is a loss, not a bug.

The check reads `portals=<carried>/<on land> onland (<n> drowned)` for a
reason. The two kinds of miss used to be one number, and that is how a real
defect hid: the sites were answered AFTER `describeBand`'s shoreline-rim test,
so half of them painted nothing, and the count looked plausible. Do not compare
readings between runs -- each run generates a fresh world, so the sites are
different places. Only `carried == on land` means anything.

### B5. Spawning

`RoyalEggObject.spawnBoss` is the vanilla pattern and it is level-agnostic:
`MobRegistry.getMob(id, level)` then `level.entityManager.addMob(...)` at an
offset. `BossSpawnPortalMob` is NOT reusable — it removes itself unless the
level is an `IncursionLevel` (`BossSpawnPortalMob.java:162-169`).

---

## What this deletes

The four registered-but-never-placed portal return halves — `veilriftup`,
`edengateup`, `ghostgateup`, `crookeddoorup` — are the last of the two-level
ladder design. Travel is the Portal Flask's job; passage is the chalk's. They go.
