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
| **Hell** | `mutanthydra` | Scrapyard | 80 000 | later | reserved |

Base HP is the CLASSIC world-difficulty column of each boss's
`MaxHealthGetter`. The ladder is monotone on purpose: 57k → 127k → 158k → 161k
→ 208k. The player's floor was *"mindestens Niveau der 1. Incursion"* and
*"grundsätzlich sollen die bosse auf incursion level 8-10 sein"*.

Other incursion bosses left unused for now, and why: `reaper` (11 000) and
`motherslime` (52 000) break the ladder or the theme; `sunlightchampion`,
`spiderempress`, `sageandgrit`, `nightswarm`, `mutanthydra` are held for Hell
and for later regions.

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
