# Stairway to Heaven — Design Document

> Necesse mod · game version 1.3.2 · design owner: project maintainers
> Companion documents: [ROADMAP.md](../ROADMAP.md), [ARCHITECTURE.md](ARCHITECTURE.md), research notes in [docs/research/](research/)

## 1. Vision

Necesse's vertical progression goes **down**: surface → caves (Cave Ladder) → deep caves
(Deep Cave Ladder, gated behind the Pirate Captain). This mod completes the vertical axis
by going **up**.

The **Stairway to Heaven** is a third, craftable stairway. Placed on the surface, it
pierces the cloud ceiling and ascends into the **Skyreach** — a wind-blasted layer of
floating islands drifting above an endless sea of mist. The Skyreach is *not* a fluffy
heaven fantasy: it is cold air, pale light, storm static and old stone. Think weathered
skystone, silver grass bending in the wind, thunderheads on the horizon — cool, muted,
a little hostile.

Design pillars:

1. **Mirror the underground.** The Skyreach works exactly like the cave layers the player
   already understands: one persistent, infinite level per world, entered through a
   two-way ladder pair, generated region-by-region with multiple sub-biomes painted into
   the biome layer. Familiar mechanics, new world.
2. **Native look & feel.** Every asset follows vanilla conventions (32 px tile grid,
   sheet layouts, outline + flat-shade pixel style, restrained palette). The mod should
   feel like an official content update, not a texture pack.
3. **Slot into progression, don't break it.** Skyreach content is tuned to the
   **Tungsten era** (after the Pirate Captain, alongside the deep caves). It offers an
   alternative gearing path, not a power creep.
4. **Ship playable, grow by roadmap.** v0.1 is a complete, balanced loop:
   ascend → explore 3 sub-biomes → fight 3 enemies → mine a new ore → craft 2 weapons.
   Bosses, armor, structures and settlements arrive in later milestones (see ROADMAP).

## 2. Player journey (v0.1)

1. Player defeats the **Pirate Captain** and reaches Tungsten tech (vanilla).
2. At a Tungsten Workstation they craft the **Stairway to Heaven**
   (8 Tungsten Bars + mid-game catalyst — see §7).
3. Placing it on the surface and using it ascends to the **Skyreach** at the same world
   coordinates (exactly like cave ladders descend). A return stairway is auto-placed.
4. They arrive on a floating island. Between islands lies the **Mistsea** — a swimmable
   cloud-ocean (mechanically a liquid, like vanilla oceans between biomes). Players can
   swim it, bridge it with placed tiles, or hop islands.
5. They explore the three sub-biomes, fight the three sky enemies, mine **Skystone** and
   **Aetherium ore**, and harvest **Storm Crystals** and **Aurora Blooms**.
6. Back at base they smelt **Aetherium Bars** and craft the **Tempest Edge** (sword) and
   **Galehowl** (bow) — sidegrade-plus options for the Tungsten era.

## 3. The Skyreach (level)

| Property | Value |
|---|---|
| Level identifier | `skyreach` (one-world dimension **+1**, above `surface` = 0) |
| Level class | `SkyLevel extends BiomeGeneratorStackLevel` |
| Registration | `LevelRegistry.registerLevel("skylevel", SkyLevel.class)` + `WorldGenerator` hook |
| Generation | Infinite, per-region, seeded from world seed (deterministic) |
| Day/night | Follows world time (`isCave = false`); bright days, real nights |
| Weather | No rain in v0.1 (`canRain() = false`) — storms are a roadmap feature |
| Persistence | Saved like cave levels (seed stored, regions persisted) |

### 3.1 Terrain composition

Generated per tile from layered value noise (own deterministic implementation seeded by
world seed; no vanilla surface-biome coupling):

- **Island mask** (mid-frequency noise, threshold): above threshold → island ground,
  below → **Mistsea** liquid.
- **Island interior**: default ground is **Cloudturf**; a second noise carves
  **Skystone** outcrop patches (rocky plateaus, always near island rims for silhouette).
- **Sub-biome mask** (low-frequency noise): assigns each tile to one of three sub-biomes
  which are painted into the region's **biome layer** (the same mechanism cave levels
  use), so spawns, music and loot resolve per-tile.

### 3.2 Sub-biomes

| | **Driftlands** (common) | **Stormveil** (uncommon) | **Aurora Shoals** (rare) |
|---|---|---|---|
| Mood | Silver-green isles, calm wind | Charcoal slate, static charge | Cold dawn light over shallow mist |
| Ground | Cloudturf + Skystone | **Stormslate** + Skystone | Cloudturf + Skystone |
| Signature objects | Skystone rocks, Skyreeds | **Storm Crystals**, Skystone rocks | **Aurora Blooms**, Aetherium rocks (richer) |
| Enemies | Zephyr Ray | Storm Wisp (+ Zephyr Ray) | Skystone Golem (+ Zephyr Ray) |
| Palette accents | pale cyan / silver | indigo / electric violet | teal / rose-gold |

Biome string IDs: `driftlands`, `stormveil`, `aurorashoals`. Each is a `Biome` subclass
registered in `BiomeRegistry` providing `getMobSpawnTable`, `getLevelMusic` (reuses
fitting vanilla music lists in v0.1) and crate/fishing hooks where applicable.

### 3.3 Tiles

| Tile ID | Type | Notes |
|---|---|---|
| `cloudturf` | terrain | Default island ground; soft, pale turf. Obtainable. |
| `skystonetile` | terrain | Rocky sky-stone ground; mineable. Obtainable. |
| `stormslate` | terrain | Dark slate ground of the Stormveil. Obtainable. |
| `mistsea` | liquid | Cloud-ocean between islands; swimmable like water, bridgeable by placing tiles. Not obtainable. |

### 3.4 Objects

| Object ID | Where | Drops / purpose |
|---|---|---|
| `skystonerock` | all islands | **Skystone** material (+ small Aetherium chance) |
| `aetheriumrock` | islands, denser in Aurora Shoals | **Aetherium Ore** |
| `stormcrystal` | Stormveil | **Storm Shards**; faint light source |
| `aurorabloom` | Aurora Shoals | **Aurora Petals**; faint light source |
| `skyreeds` | Driftlands | decorative wind grass |
| `skystairwaydown` | player-placed on surface | the Stairway to Heaven itself |
| `skystairwayup` | auto-placed in Skyreach | return stairway to the surface |

## 4. Enemies (Tungsten-era tuning)

Vanilla reference points: deep-cave ranged flyer Cryo Flake = 350 HP / 65 dmg / 20 armor.

| Mob ID | Concept | HP | Dmg | Armor | Speed | AI base | Drops |
|---|---|---|---|---|---|---|---|
| `zephyrray` | sleek manta gliding over the mist, swoops in | 220 | 45 melee | 0 | fast | flying melee chaser | Windsilk 1–2 |
| `stormwisp` | crackling storm core, fires spark bolts | 280 | 55 ranged | 10 | medium | flying shooter (Cryo Flake pattern) | Storm Shard 1–2 |
| `skystonegolem` | slow animated skystone hulk | 520 | 70 melee | 30 | slow | ground melee chaser | Skystone 2–4, Aetherium Ore 0–2 |

All three use vanilla AI trees (`BehaviourTreeAI` + existing chaser/shooter nodes +
`FlyingAIMover` where airborne) — no custom pathfinding, maximum engine compatibility.
Spawns resolve through the sub-biome spawn tables; the Mistsea itself spawns nothing.

## 5. Items

| Item ID | Type | Notes |
|---|---|---|
| `skystone` | material | from Skystone rocks/golems; crafting stone of the sky tier |
| `aetheriumore` | material | new ore of the Skyreach |
| `aetheriumbar` | material | smelted 3:1 from ore |
| `stormshard` | material | Stormveil crystal/wisp drop |
| `windsilk` | material | Zephyr Ray drop |
| `aurorapetal` | material | Aurora Bloom harvest |
| `tempestedge` | sword | Aetherium blade, Tungsten-tier sidegrade with wind flavor |
| `galehowl` | bow | fast Aetherium/Windsilk bow |
| (object items) | placeable | stairway pair, harvested tiles |

Weapon tuning targets (validated against vanilla Tungsten weapons at implementation
time): Tempest Edge ≈ tungsten sword damage +5% with slightly faster swing; Galehowl ≈
tungsten-tier bow with +10% projectile velocity. Both sell in the same price band as
vanilla Tungsten gear.

## 6. Crafting

| Recipe | Station / tech | Ingredients |
|---|---|---|
| Stairway to Heaven | Tungsten Workstation | 8 Tungsten Bar + 15 Quartz (§7) |
| Aetherium Bar | Forge | 3 Aetherium Ore |
| Tempest Edge | Tungsten Workstation | 8 Aetherium Bar + 5 Storm Shard |
| Galehowl | Tungsten Workstation | 4 Aetherium Bar + 6 Windsilk + 3 Aurora Petal |

## 7. Progression gating

The vanilla Deep Cave Ladder costs 8 Tungsten Bars at the Tungsten Workstation. The
Stairway to Heaven costs the same tungsten investment **plus** 15 Quartz — a cave
mineral — so the sky unlocks *alongside* — never before — the deep caves.

## 8. Art direction

- **Grid**: 32 px tiles; objects may extend upward in 32 px steps (stairways are tall).
- **Style**: vanilla Necesse pixel language — soft dark outline (not pure black), 2–3
  flat shade steps per material, sparse single-pixel dithering at shade borders, warm
  light from top-left.
- **Palette discipline**: muted bases, few saturated accents (per sub-biome, see §3.2).
  "Cool, not kitschy": no rainbows, no gold-trimmed clouds; weathered stone, cold air,
  electric storm light.
- **Pipeline**: all textures are generated by the reproducible Python pipeline in
  `tools/asset_generator/` (seeded, deterministic). Every sprite can be regenerated or
  hand-replaced; sheet layouts follow `docs/research/asset-formats.md`.

## 9. Multiplayer & compatibility

- All content is registered server+client symmetrically; textures load client-side only.
- Level transfer uses the vanilla `PortalObjectEntity`/`changeLevelCheck` path — the same
  netcode as cave ladders. No custom packets are needed for v0.1.
- The dimension is registered as one-world dimension `+1`; no vanilla IDs are modified,
  no vanilla behavior is patched (no bytecode/method injection) — pure additive registry
  usage for maximum mod compatibility.
- Save-safety: removing the mod leaves the world loadable (levels of unknown identifiers
  are simply not entered; stairway objects/items of unknown IDs are dropped by the game's
  standard unknown-registry handling).

## 10. Out of scope for v0.1 (see ROADMAP)

Boss fight, armor set, sky fishing loot, storm weather events, structures/presets,
settlement support in the sky, custom music/sounds, additional sub-biomes.
