# Stairway to Heaven — what exists, where, and what is playable

Read off the CODE, not off the other docs. Version 0.6.0, Necesse 1.3.2.

Legend: **PLAY** = a player reaches it in a normal game and the integration
test proves it · **IN** = in the world, not covered by a test · **DEAD** =
registered but unreachable.

---

## 1. Realms

| realm | level class | registered | dim | status |
|---|---|---|---|---|
| **Skyreach** | `SkyLevel` | `skylevel` / `skyreach2` | +1 | **PLAY** |
| **Veil** | `VeilLevel` | `veillevel` / `veil2` | −3 | **PLAY** |
| Eden, Steinfeld, Ghost, Infernal Fringe, Hell | — | — | — | **not built** (only the realm-depth field exists: depth 0 = Skyreach, far end = Hell) |

## 2. Biomes and who lives in them

| biome | realm | hostiles (weight/cap) | critters | guard pack |
|---|---|---|---|---|
| `driftlands` | Sky | zephyrray 40/3, skystonegolem 40/2, galehound 45/3 | zephyrfinch | golem + 2 hounds + ray, 4–6 |
| `stormveil` | Sky | stormwisp 60/3, zephyrray 25/2, skystonegolem 25/2, rimesentry 30/2, auroraflake 20/2 | sparkbeetle | golem + sentry / 2 wisps + ray, 4–6 |
| `aurorashoals` | Sky | skystonegolem 45/2, zephyrray 40/2, dawnpiercer 40/2, auroraflake 35/2 | glowmoth, dewsnail | golem + dawnpiercer / 2 flakes + ray, 5–7 |
| `skyway` | Sky | skystonegolem 45/2, galehound 45/3, zephyrray 35/2, rimesentry 35/2 | zephyrfinch, glowmoth | golem + sentry / 2 hounds + ray, 4–6 |
| `outlands` | Sky, **distance-gated** (clean out to 900 tiles, present at 3200) | crookedgolem 50/2, rarecrookedgolem 25/1, crookedarmadillo 45/2, gloomshade 70/3, fenwraith 40/2, cindercantor 30/2 | — | rare golem + golem / 2 shades + armadillo + wraith, 6–8 |
| `gloomfen` | Veil | gloomshade 90/3, fenwraith 55/2, cindercantor 30/2 | — | wraith / 2 shades + cantor, 4–6 |
| `ashenreach` | Veil | gloomshade 55/2, cindercantor 70/2, fenwraith 35/2 | — | wraith + cantor / shade + cantor, 4–6 |
| `beetlefreakhollow` | Veil | gloomshade 100/3, fenwraith 40/2, cindercantor 35/2 | — | wraith / 3 shades + cantor, 5–7 |

**Mistsea** (the cloud sea between the islands, 61 % of the sky) carries the
**Mistserpent** in all four sky biomes at weight 300, capped at one per spawn
ring. Sky biomes run spawn rate ×0.55 / cap ×0.75.

**A4.1 placement**: enemies are *placed*, not sprinkled — guarded site 600
tickets, its approach 100, wilds 45, mistsea 16, everything else 0. So they
come in packs around loot and leave the rest of the sky quiet.

## 3. Mobs

| id | role | art |
|---|---|---|
| `skystonegolem`, `zephyrray`, `stormwisp`, `galehound`, `dawnpiercer` | Sky hostiles | ours (generated) |
| `rimesentry`, `auroraflake` | Sky hostiles (arsenal tier) | **auroraflake = your sheet**, rimesentry generated |
| `mistserpent` (+ body, tail) | the sky's roaming worm, mistsea only | **your sheets**, drawn on vanilla's Crystal Dragon format |
| `crookedgolem`, `rarecrookedgolem`, `crookedarmadillo` | Outlands | ours |
| `gloomshade` | Veil | **your sheet** |
| `fenwraith`, `cindercantor` | Veil | **fenwraith = your sheet** |
| `glowmoth`, `sparkbeetle`, `zephyrfinch`, `dewsnail` | critters (dewsnail is netable) | **glowmoth + sparkbeetle = your sheets**, rest generated |
| `spirecatblack`, `spirecattabby` | the two quest cats | **your sheets**, on vanilla's duck layout (384x320) |
| `nimbusyak` | livestock, milk | **your sheets** (cow/bull/calf) |
| `glimmergoat` | livestock, shear | **your sheets** (doe/doe shorn/ram/ram shorn/lamb) |
| `watchmote` | player summon (Skywatch Whistle) | vanilla pet flake |

Removed: **Thunderquill Fowl** (a third animal was one too many) and the
**Cloud Lamb** (the Glimmergoat is the sky's sheep now).

## 4. NPCs — all **PLAY**

| who | how you meet them | what they give you |
|---|---|---|
| **Sky Warden** | stands in the Warden's Spire, one per world | the whole quest chain; recruit for **30 000 coins**; sells silverbell once recruited |
| **Magpie** | 16 % per region, only next to a Skywatch workstation, one per world | sells wormbait, sandstone, coconut, snowball, glass, silverbell; **buys** skystone, windsilk, aetheriumore, stormshard, aurorapetal, fulgurite, prismshard |
| **Halda** | same rule | sells skyweave, stormglass, stormsteelbar, cloudpufftreat, cloudberry; buys windwheat, cloudberry, nimbuswood, charwood |
| **Ossian** | same rule; recruit 18 000 | **daily-rotating incursion stock**, 3 of 8 per day: crystalessence, ascendedshard, voidbullet, arcanic helm/chest/boots, voidbag, eye of the void |

Residents never move out, never get banished, never spawn as settlers on their
own — they are found, not assigned.

## 5. Quest chain — **PLAY**, no dead ends (tested)

```
FIND SPIRE  →  RECRUIT (pay 30k, the beacon lights)  →  CATS (coax Siggi and
Peanut home)  →  CATS TURN-IN (reward: cat basket, 2 flickerlight garlands)
→  ANCHOR (deliver 5 aetheriumbar + 20 skystone; reward: Skywatch banner,
5 aurora petals)  →  DONE
```

Map markers: `skyspire`, `skystairs`, `skycat`. Everything survives a restart
(the test proves the spire coordinates, the Warden, both cats, their home flags,
their tether and the basket all persist).

`swh_beacon` (deliver 12 stormshard + 8 windsilk) is registered but **no code
path hands it out** — the beacon now lights on recruit. **DEAD.**

## 6. What generates in the world

**Skyreach** — all **PLAY** except where noted
- **Warden's Spire** 21×21, one per world, with its paved **Forecourt** and lamp ring
- **Skywatch roads** on a node lattice, with three designed places: Garden Court, Waystation Square, Overlook Terrace
- **Abandoned workshops** (70 % of their lattice cell) and **wreck sites** (40 %) — both guard-packed
- **Salvage crates** (`skycrate`), per-biome loot; **sky caches** (`skycache`), high tier
- Yak herds on cloudturf, goat herds on aurora shoal
- Cat lairs: black in Stormveil, tabby in Aurora Shoals

**Veil**
- **Crooked House** (`swh_crookedhouse`), Beetlefreak Hollow only — barrel with veilessence, cinderpearl, charwood. **IN**

**Surface (the normal world)** — all **PLAY**
- **Sky Fragment Crater** (120 tickets), **Aeronaut Camp** (100), **Skyward Shrine** (70)
- **Skyfall world event**: scatters `skyfallshard`, survives a restart, cleans up after itself

## 7. Workstations and building sets

**Workstations** — all **PLAY**, all settler-operable

| station | makes |
|---|---|
| Windsilk Loom | `skyweave` ×1 |
| Aether Forge | `stormsteelbar` ×1 — the only source |
| Stormglass Kiln | `stormglass` ×2 |

**Building sets** — walls, doors and windows each: Skystone Brick · Nightfell ·
Beetlefreak (Veil) · Cloudmarble (+ fence, gate, seraph statue). Plus Sky-iron
fence and gate.

**Floors**: marble checker, gloomwood, nimbus, char, prism, skyway.
**Lights**: warden candelabra, ghost lantern, mistglass lantern, flickerlight garland.
**Skywatch furniture**, 17 pieces: chair, bench ×2, modular + dinner table, desk,
dresser, bookshelf, cabinet, clock, display, bed, candelabra, carpet, chalice,
candle, tome, potted cloudberry.
**Nature**: 5 tree species with saplings, 9 plants, 3 grasses, 5 rock types,
Veil flora.

## 8. Gear

| kind | items |
|---|---|
| weapons | `skyreave` (glaive), `thunderhead` (greatbow), `prismcaller` (staff), `skywatchwhistle` (summons the Watch Mote), `stormdisc` (boomerang), `tempestedge` (sword), `galehowl` |
| armour | Stormsteel helm / chest / boots — set bonus +30 resilience, +10 % speed |
| trinkets | Stormsteel Vambrace, Aurora Locket, Zephyr Harness (all EPIC), Glimmerstride Boots |

All five arsenal weapons are tested to register with a name and a recipe.

**Difficulty**: the mod's floor is vanilla **incursion tier 1** — 1000 HP /
130 damage / 40 armour — and climbs from there. It is endgame content.

## 9. Gaps — the honest list

| what | state |
|---|---|
| `swh_beacon` quest | **DEAD** — registered, never handed out |
| `skyanchor` object | **DEAD** — no recipe, no loot, never placed |
| `skywatchhood`, `wardenmantle`, `wardenboots` | player-unobtainable by design; they exist to dress the Warden's sprite |
| `catbasket`, `flickerlightgarland` | only obtainable from the cats chapter — no second source |
| `skywaytile` | mined only; the other floors all have recipes |
| Eden, Steinfeld, Ghost, Infernal Fringe, Hell | designed, not built |
| four shipped wall sets | 4–20 marginal corner seams each |
| `projectiles/mistserpentshard.png` | delivered art, no projectile behind it yet |
