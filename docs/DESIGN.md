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

---

# Part II — v0.2 "The Warden's Call"

v0.1 built the place; v0.2 gives it a memory. Goal: the Skyreach stops being "caves,
but up" and becomes its own world with a resident, a past, and things worth building.

## 11. Story skeleton

Long before the first ladder was nailed together, the **Skywatch** kept the balance
between the layers. Their spires anchored the islands against the wind. Then the Great
Storm came, the anchors cracked, and the Skywatch scattered — all but one.

**The Sky Warden** still keeps his half-collapsed spire in the Driftlands: a tall, thin,
slightly crooked old keeper in a feather-trimmed coat — more raven than man by now,
politely grumpy, quietly kind. His lighthouse crystal is shattered, his two cats ran off
during the last storm, and he has opinions about visitors who stomp on his cloudturf.

Tone: dry, melancholic, a little gothic, never silly. The story is told through short
dialogue lines, item descriptions and the spire itself changing as you help him.

## 12. Quest chain (4 stages, save-persistent, multiplayer-safe)

Progress is stored server-side on the spire (one shared world state, like vanilla world
events); rewards are granted to the delivering player. Turn-ins validate and consume
items server-side on interact — no trust in the client.

| # | Quest | Type | Player does | Reward / world change |
|---|---|---|---|---|
| 1 | **A Light over the Mist** | find someone | On first ascent, a journal/chat hint points to a flicker over the mist. Find the spire, talk to the Warden. | Windsilk bundle; spire location pinned on map; stage 2 opens |
| 2 | **The Dark Lighthouse** | collect & deliver | Bring **12 Storm Shards + 8 Windsilk** so he can rekindle the beacon. | The spire's **Wardenlight ignites** (visible object swap); his **shop opens** (building set); Flickerlight Garlands as a gift |
| 3 | **Where the Cats Wander** | find + item use | His cats fled: **Siggi** (black, hides in the Stormveil) and **Peanut** (white-tabby, chases glowmoths in the Aurora Shoals). Craft **Cloudpuff Treats**, find each cat, offer a treat. | Each cat travels home and stays as a resident of the spire; both home → **Cat Basket** deco + **Silver Bell** trophy |
| 4 | **Anchor of the Sky** | collect & deliver | Bring **5 Aetherium Bars + 20 Skystone** — he reforges a spire anchor. | **Skywatch Banner** deco + reward bundle; the anchor appears at the spire; closing dialogue teases the Storm Sovereign (v0.5 boss) |

Design notes:
- Stage gating is linear; stages 3 and 4 unlock together after 2 (players can do them in
  either order; the finale line plays once both are done).
- Every "delivery" is also a **visible change at the spire** — the world reacts.
- All interaction is interact-driven (chat bubbles + server-side inventory checks); a
  container/dialog UI is used only where research shows it is robust in MP.

## 13. The Warden's Spire (structure)

A ~17×17 ruined round tower stamped **once per world** in the Driftlands, 40–120 tiles
from the ascent origin (deterministic: first suitable land patch on a seed-derived
spiral; position recorded in level data so it never re-stamps).

Composition: cracked **Skystone Brick** walls, a **checkered marble** core floor,
**Gloomwood** planks, wrought-iron fence stubs, two Warden's Candelabras (one dead),
the unlit **Wardenlight** at the top, a small loot chest with flavor items, the Warden
inside, and an empty spot where a cat basket will stand.

## 14. Building & deco set — "Nightfell & Skylight"

Gothic-meets-sky: crooked silhouettes, wrought iron, cold stone, warm little lights.
Craftable at a Workstation from sky materials once unlocked; garland/basket/banner stay
quest-exclusive so they feel earned. All pieces carry proper room properties so they
work in settlements.

| Piece | Kind | Notes |
|---|---|---|
| Skystone Brick Wall (+ Door) | wall | pale weathered stone |
| Nightfell Wall | wall | near-black violet stone, gothic set base |
| Checkered Marble Floor | floor tile | black/white checker |
| Gloomwood Floor | floor tile | dark creaking planks |
| Wrought Iron Fence (+ Gate) | fence | spiked, crypt-style silhouette |
| Warden's Candelabra | lamp (on/off) | cold blue-white flame, streetlamp format |
| Mistglass Lantern | wall lamp | soft cool light |
| Gloomwillow | deco tree | crooked bare tree; also rare natural spawn in the Stormveil |
| Raven Statue | deco statue | small skystone raven |
| Flickerlight Garland | wall deco, light | string of tiny colored lights, gently flickering (quest gift) |
| Cat Basket | deco | quest reward; the spire cats nap in it |
| Skywatch Banner | wall deco | quest finale reward |

## 15. The cats

Two unique, invulnerable friendly critters with their own sprites and personalities:

- **Siggi** — pitch-black, amber eyes; skittish; found sheltering between storm
  crystals in the Stormveil.
- **Peanut** — white with tabby patches; curious; found pouncing at glowmoths in the
  Aurora Shoals.

Found cats idle near a fixed lair point per world (seed-derived, discovered by
exploring). Interacting with a **Cloudpuff Treat** (recipe from the Warden: 1 Windsilk
+ 2 Aurora Petals → 3) sends the cat home in a puff of cloud. Homed cats live at the
spire permanently: wander a small radius, sit, nap in the basket. They are deliberately
pettable-adjacent ambient life — no combat role, no despawn.

## 16. New items (v0.2)

`cloudpufftreat` (quest consumable), `silverbell` (trophy, high vendor value, tooltip
lore), deco/object items for every set piece above. No new combat gear in v0.2 — this
release adds depth, not numbers.

## 17. Render-correctness fixes shipped with v0.2

Verified against the vanilla sprite reference: terrain tiles move to the real `_splat`
autotile atlas format (the 1.3.2 renderer's primary path), the ore overlay becomes the
correct N×32×32 pattern-variant strip, and the Mistsea gets proper liquid splats.
