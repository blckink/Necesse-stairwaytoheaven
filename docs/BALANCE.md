# Balance reference

The numbers every rebalance change in this mod points at, and where each one
came from. Nothing here is invented: every value below was read out of the
decompiled game or out of this repository's own source, and each row names the
class it was read from so the next agent can check it instead of trusting it.

**Status: VERIFIED [jar]** — proven from the decompiled 1.3.2 sources under
`$NECESSE_GAME_DIR/decompiled/src`. **Not player-confirmed.** Nobody has fought
any mob at these values, and no number here has been seen on a real client. Do
not upgrade this document's status without a playtest entry in
`docs/PLAYTEST_LOG.md`. Verification language is `docs/IMPLEMENTATION_RULES.md`
§14.

**Written:** 2026-08-31, against Necesse 1.3.2 (`necesse-server-1-3-2-24650233`).

## Why this document exists

The player, ten incursions deep:

> "bitte Schwierigkeit und Wertigkeit startet mindestens auf Niveau der 1.
> incursion für die schwächsten gegner .. wir sind mittlerweile bei 10 durch und
> brauchen Herausforderung für danach"

That is a decision about *where the mod sits in the game*, not a tuning tweak.
The Skyreach was calibrated against the **deep-cave / tungsten** tier — the
Stormsteel set is documented in `StormsteelArmor` as one step above tungsten and
deliberately still under glacial, and each Sky Arsenal weapon names a deep-cave
vanilla weapon in its class comment. Everything in `docs/CURRENT_STATE.md`
describing that calibration is now historical: **the mod becomes endgame-only**,
its weakest enemy standing where vanilla's incursion tier 1 stands, and its gear
scaling with it.

This file is the shared arithmetic for that move. It does not decide what any
individual mob or item becomes — that stays with the class — it decides what the
targets are, and proves the targets are real.

## 1. How incursion scaling actually works

Incursion difficulty is **not** written into the mobs. Vanilla spawns the same
mob classes and multiplies them at the *level*, through `LevelModifiers` built in
`BiomeMissionIncursionData.initModifiers()`:

```
necesse/level/maps/incursion/BiomeMissionIncursionData.java
 65:    protected int tabletTier;
 66:    public float[] damageScalingPerTier = {0.0F, 0.15F, 0.14F, 0.13F, 0.12F, 0.11F, 0.1F, 0.12F, 0.13F, 0.15F};
 67:    public float[] healthScalingPerTier = {0.0F, 0.25F, 0.27F, 0.29F, 0.31F, 0.33F, 0.35F, 0.38F, 0.4F, 0.42F};
 68:    public float undefinedDamageScalingPerTier = 0.04F;
 69:    public float undefinedHealthScalingPerTier = 0.45F;
117:        float lootPercentIncreasePerTier = 15.0F;
```

`initModifiers()` (line 112) turns those into exactly three level modifiers —
`LevelModifiers.ENEMY_MAX_HEALTH`, `LevelModifiers.ENEMY_DAMAGE` and
`LevelModifiers.LOOT`. Loot is a flat `lootPercentIncreasePerTier * tabletTier`
(line 125); the other two come from `getDamageIncrease()` / `getHealthIncrease()`
(lines 163-186), each of which is a **cumulative sum of the array up to the
tier**:

```java
for (int i = 0; i < this.tabletTier && i < this.healthScalingPerTier.length; ++i)
    healthIncrease += this.healthScalingPerTier[i];
```

The arrays are increments, not absolutes. The loop is bounded by `tabletTier`,
so tier *n* sums the first *n* entries.

### Derived tier table

| tablet tier | enemy HP | enemy damage | loot |
|---|---|---|---|
| 1 | **x1.00** | **x1.00** | x1.15 |
| 2 | x1.25 | x1.15 | x1.30 |
| 3 | x1.52 | x1.29 | x1.45 |
| 4 | x1.81 | x1.42 | x1.60 |
| 5 | x2.12 | x1.54 | x1.75 |
| 6 | x2.45 | x1.65 | x1.90 |
| 7 | x2.80 | x1.75 | x2.05 |
| 8 | x3.18 | x1.87 | x2.20 |
| 9 | x3.58 | x2.00 | x2.35 |
| 10 | **x4.00** | **x2.15** | **x2.50** |

Two structural notes that matter beyond the table:

- `tabletTier >= 4` and `>= 8` raise `modifiersToAdd` (lines 131-137), so higher
  tiers also carry more `UniqueIncursionModifier`s and more shared completion
  rewards. Difficulty past tier 4 is not only bigger numbers.
- **Past tier 10 the arrays run out** and the `undefined*` fields take over at
  `+0.45` HP and `+0.04` damage per tier (lines 161-162, 175-176). Health keeps
  climbing hard; damage almost flattens. Tier 13 is HP x5.35 / damage x2.27.

## 2. The finding that sets the floor: tier 1 applies no multiplier

Both arrays open with `0.0F`, and the loop for tier 1 sums exactly one element.
So **incursion tier 1 multiplies enemy health by 1.00 and enemy damage by 1.00**
— it is the unmodified strength of the mob classes vanilla puts in an incursion
biome. Only loot moves at tier 1, by +15%.

That is what "Niveau der 1. Incursion" resolves to, and it is good news for the
work: the target is not an abstract multiplier, it is **the printed stats of the
classes vanilla spawns in incursions**. Read them off the classes and that is the
floor. A mod mob at or above that line is at or above the player's stated
minimum, on every tablet he owns.

## 3. Measured floor — the mobs vanilla puts in an incursion

All read from `$NECESSE_GAME_DIR/decompiled/src/necesse/entity/mobs/hostile/`:

| class | file:line | HP | damage | armour | speed |
|---|---|---|---|---|---|
| `AscendedGolemMob` | `AscendedGolemMob.java:35` | `MaxHealthGetter(400, 750, 1000, 1300, 1800)` | — | — | — |
| `CrystalGolemMob` | `CrystalGolemMob.java:57, 70, 72, 73` | 500 flat | 130 | 40 | 20 |
| `NightSwarmBatMob` | `bosses/NightSwarmBatMob.java:73, 76, 79, 81` | from `NightSwarmLevelEvent.BAT_MAX_HEALTH` | 115 | 40 | 100 |
| `CrystalArmadillo` | `CrystalArmadillo.java:40, 65, 66, 72, 73` | — | 90 | 60 rolled up / 40 rolling | 20 / 200 |
| `AscendedBatMob` | `AscendedBatMob.java:44, 55, 57` | — | 90 | 40 | 175 |

`MaxHealthGetter`'s five arguments are in difficulty order — casual, adventure,
**classic**, hard, brutal (`entity/mobs/MaxHealthGetter.java:19-21`) — so the
golem's headline 1000 is its **Classic** value, and Classic is the difficulty the
ladder below is written in.

**The floor is 1000 HP / 130 damage / 40 armour.** HP comes from the heaviest of
these archetypes (`AscendedGolemMob`, Classic); damage and armour from
`CrystalGolemMob`, the hardest-hitting ordinary incursion mob of the five.
`CrystalGolemMob`'s own 500 HP is the light end of the same tier — a mob that is
fast or ranged is allowed to sit under the floor's HP, which is exactly what the
role modifiers in §6 encode.

## 4. Measured gear floor

From `$NECESSE_GAME_DIR/decompiled/src/necesse/inventory/item/armorItem/`:

| set | helm | chest | greaves | enchant cost | rarity |
|---|---|---|---|---|---|
| Tungsten | 24 | 25 | 15 | 1300 | UNCOMMON |
| Glacial | 24 | 24 | 16 | 1450 | UNCOMMON |
| **Arcanic** (incursion) | **23** | **29** | **17** | **1900** | **EPIC** |

Arcanic is the incursion-tier armour: `ArcanicHelmetArmorItem.java:22`,
`ArcanicChestplateArmorItem.java:18`, `ArcanicBootsArmorItem.java:21`. It is also
the first set in this comparison that draws from `IncursionArmorSetsLootTable` /
`IncursionHeadArmorLootTable` and is `EPIC` rather than `UNCOMMON`, so the rarity
step is vanilla's own marker for crossing into incursion gear.

The mod's set today, `stairwaytoheaven/items/StormsteelArmor`: helm 25 / chest 26
/ greaves 16 at enchant 1300, `UNCOMMON` — deliberately, per its own class
comment, *below glacial*. That is three tiers under where the mod now needs to
sit.

**The gear floor is Arcanic: 29 chest armour / 1900 enchant cost / EPIC.**

## 5. The realm ladder

The spec every rebalance unit implements. HP is the **Classic** value; damage is
the mob's primary `GameDamage`; drop value is a multiplier on the realm's loot
worth, relative to the Skyreach floor.

| realm | ~ incursion tier | HP | damage | armour | drop value |
|---|---|---|---|---|---|
| Skyreach (floor) | 1 | 1000 | 130 | 40 | x1.0 |
| Eden | 3 | 1500 | 165 | 45 | x1.3 |
| Steinfeld | 5 | 2100 | 200 | 50 | x1.6 |
| Ghost Realm | 7 | 2800 | 230 | 55 | x1.9 |
| Crooked Beyond | 10 | 4000 | 280 | 60 | x2.5 |
| Hell | past 10 | 5500 | 340 | 70 | x3.2 |

How well that tracks vanilla's own curve, floor-relative (the §1 table divided
through by tier 1):

| realm | HP wanted | HP at that tier | damage wanted | damage at that tier |
|---|---|---|---|---|
| Skyreach | x1.00 | x1.00 | x1.00 | x1.00 |
| Eden | x1.50 | x1.52 | x1.27 | x1.29 |
| Steinfeld | x2.10 | x2.12 | x1.54 | x1.54 |
| Ghost Realm | x2.80 | x2.80 | x1.77 | x1.75 |
| Crooked Beyond | x4.00 | x4.00 | x2.15 | x2.15 |

The five realms up to the Crooked Beyond *are* vanilla's curve, rounded to
numbers a human can read. Two places the ladder leaves it on purpose, stated
rather than hidden:

- **Hell is not a vanilla tier.** Its HP x5.50 lands near tier 13 on the
  `undefinedHealthScalingPerTier` extrapolation, but its damage x2.62 is far past
  anything the `+0.04`/tier damage extrapolation reaches — tier 14 is still only
  x2.31. Hell is *after* the game, and it hits harder than the game's own
  arithmetic would ever produce.
- **Drop value runs slightly ahead of `LevelModifiers.LOOT`.** Floor-relative,
  vanilla's loot at the mapped tiers is x1.26 / x1.52 / x1.78 / x2.17, against
  the ladder's x1.3 / x1.6 / x1.9 / x2.5. The realms are the reward for a whole
  climb, not for one tablet, so the last rung in particular is deliberately
  generous. That is a design choice, not a rounding error.

## 6. Role modifiers

Applied to the realm row, and not stacked with each other — a mob picks the one
that describes it:

| role | HP | damage |
|---|---|---|
| elite / heavy | x1.4 | x1.0 |
| ranged | x0.7 | x0.85 |
| fast / swarm | x0.6 | x0.8 |

Which has vanilla's shape behind it: `CrystalGolemMob` is the slow 130-damage
anchor at speed 20, while `AscendedBatMob` (speed 175) and the rolling
`CrystalArmadillo` (speed 200) both drop to 90 damage.

Worked out per realm so no unit has to redo the multiplication (round to the
nearest 5 — the ladder is a band, not a checksum):

| realm | base HP / dmg | elite | ranged | fast |
|---|---|---|---|---|
| Skyreach | 1000 / 130 | 1400 / 130 | 700 / 110 | 600 / 104 |
| Eden | 1500 / 165 | 2100 / 165 | 1050 / 140 | 900 / 132 |
| Steinfeld | 2100 / 200 | 2940 / 200 | 1470 / 170 | 1260 / 160 |
| Ghost Realm | 2800 / 230 | 3920 / 230 | 1960 / 195 | 1680 / 184 |
| Crooked Beyond | 4000 / 280 | 5600 / 280 | 2800 / 238 | 2400 / 224 |
| Hell | 5500 / 340 | 7700 / 340 | 3850 / 289 | 3300 / 272 |

### Filling a `MaxHealthGetter`

The ladder's HP is Classic. `AscendedGolemMob`'s five values give the spread
vanilla uses at this tier — `400/750/1000/1300/1800`, i.e.
**x0.40 / x0.75 / x1.00 / x1.30 / x1.80** — so a mob at a realm's base HP is:

| realm | `MaxHealthGetter(...)` |
|---|---|
| Skyreach | `(400, 750, 1000, 1300, 1800)` |
| Eden | `(600, 1125, 1500, 1950, 2700)` |
| Steinfeld | `(840, 1575, 2100, 2730, 3780)` |
| Ghost Realm | `(1120, 2100, 2800, 3640, 5040)` |
| Crooked Beyond | `(1600, 3000, 4000, 5200, 7200)` |
| Hell | `(2200, 4125, 5500, 7150, 9900)` |

## 7. The gear ladder

Chest armour is the anchor; enchant cost and rarity move with it.

| set | realm | chest | enchant | rarity |
|---|---|---|---|---|
| Stormsteel | Skyreach | 29 | 1900 | EPIC |
| Spiritsteel | Ghost Realm | 34 | 2400 | EPIC |
| (Crooked set) | Crooked Beyond | 38 | 3000 | EPIC |
| Hellsteel | Hell | 42 | 3600 | LEGENDARY |

Within a set: **helm ≈ chest − 3, greaves ≈ chest − 10**. Vanilla's own spreads
bracket that choice — tungsten is 24/25/15 (−1 / −10) and arcanic is 23/29/17
(−6 / −12) — so −3 / −10 sits between them and keeps the helm from being the
piece a player skips.

Stormsteel starting *at* Arcanic rather than above it is the point: the mod's
entry set equals the game's incursion set, and the whole climb happens inside the
mod.

## 8. How to re-derive this

Everything above can be reproduced in about five minutes. Do that rather than
trusting this file, especially after a Necesse version bump — the arrays are
public mutable fields and nothing promises they are stable.

```bash
export NECESSE_GAME_DIR=/opt/necesse-server/necesse-server-1-3-2-24650233
# writes $NECESSE_GAME_DIR/decompiled/ ; see AGENTS.md
./gradlew decompileToSources -PuseDecompiledSources=true
D=$NECESSE_GAME_DIR/decompiled/src

# section 1 — the scaling arrays and the three level modifiers
grep -n "ScalingPerTier\|lootPercentIncrease\|initModifiers" \
  "$D/necesse/level/maps/incursion/BiomeMissionIncursionData.java"

# section 3 — the floor mobs
grep -n "MAX_HEALTH\|GameDamage\|setArmor\|setSpeed" \
  "$D/necesse/entity/mobs/hostile/CrystalGolemMob.java" \
  "$D/necesse/entity/mobs/hostile/AscendedGolemMob.java" \
  "$D/necesse/entity/mobs/hostile/AscendedBatMob.java" \
  "$D/necesse/entity/mobs/hostile/CrystalArmadillo.java" \
  "$D/necesse/entity/mobs/hostile/bosses/NightSwarmBatMob.java"

# section 4 — the gear floor
grep -rn "super(" "$D/necesse/inventory/item/armorItem/arcanic/" \
                  "$D/necesse/inventory/item/armorItem/tungsten/" \
                  "$D/necesse/inventory/item/armorItem/glacial/"
```

The tier table is then only a running sum:

```python
dmg = [0.00, 0.15, 0.14, 0.13, 0.12, 0.11, 0.10, 0.12, 0.13, 0.15]
hp  = [0.00, 0.25, 0.27, 0.29, 0.31, 0.33, 0.35, 0.38, 0.40, 0.42]
for t in range(1, 11):
    print(t, round(1 + sum(hp[:t]), 2), round(1 + sum(dmg[:t]), 2), round(1 + 0.15 * t, 2))
```

Three things to check specifically, because they are where a re-derivation goes
wrong:

1. **The arrays are increments.** Reading `healthScalingPerTier[4]` as tier 4's
   multiplier gives x1.31 instead of x1.81.
2. **The loop bound is `tabletTier`, not `tabletTier - 1`** — which is why tier 1
   sums one element (`0.0`) and lands on x1.00 rather than being skipped
   entirely.
3. **`MaxHealthGetter`'s third argument is Classic.** Quoting a mob's first
   argument gives its casual HP, 40% of the real number.

## 9. What this file does and does not decide

- It is the **arithmetic**, not the fiction. Realm names, order and meaning come
  from `docs/WORLD_DESIGN.md`; where the two disagree about what a realm *is*,
  the world document wins and this one gets corrected.
- It is a **target, not a record**. A row here does not mean a mob has that
  value; it means it should. What is actually implemented lives in
  `docs/CURRENT_STATE.md` and in the classes themselves.
- Every balance change should cite the section it implements, so a later reader
  can tell a deliberate ladder position from a number somebody liked.
