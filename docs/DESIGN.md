# Stairway to Heaven — Design Document

> Necesse mod · game version 1.3.2 · design owner: project maintainers
> Companion documents: [ROADMAP.md](../ROADMAP.md), [ARCHITECTURE.md](ARCHITECTURE.md), research notes in [docs/research/](research/)

## 1. Vision

Necesse's vertical progression goes **down**: surface → caves (Cave Ladder) → deep caves
(Deep Cave Ladder, gated behind the Pirate Captain). This mod completes the vertical axis
by going **up**.

The **Stairway to Heaven** is a third, craftable stairway. Placed on the surface, it
pierces the cloud ceiling and ascends into the **Skyreach** — a wind-blasted layer of
floating islands drifting above an endless sea of mist.

> **RETRACTED 2026-08-31.** This paragraph used to read *"The Skyreach is not a
> fluffy heaven fantasy: it is cold air, pale light, storm static and old stone
> … cool, muted, a little hostile."* **That is now wrong**, and the player said
> so directly: *"ich habe festgestellt dass viele Vorgaben für Skyreach nicht
> passen zb alles nur entsättigt sein soll.. das ist falsch"*.
>
> `docs/WORLD_DESIGN.md` §4 is the brief: white, cream, light blue, pink, warm
> gold and pastels; **explicitly not a desaturated fantasy look**; the sky must
> read alive and friendly. Skyreach is Tier 0 of nine — the *idealised* end of
> the road, and the cold/hostile register belongs to Steinfeld and beyond.
>
> The storm register is not deleted, it is demoted: Stormveil stays as a
> *variant* inside Skyreach (`WORLD_DESIGN.md` §41.3), not as the whole sky's
> mood.

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
- **Palette discipline**: coherence per realm, NOT global desaturation.
  *(Corrected 2026-08-31 — this line used to demand "muted bases" and forbid
  "gold-trimmed clouds"; gold-trimmed clouds are now the Skyreach brief.)*
  Each realm has its own defined palette and stays inside it —
  `docs/WORLD_DESIGN.md` §36: Heaven bright with golden highlights, Eden highly
  saturated, Ghost petrol/violet/poison-green, Crooked neon on monochrome, Hell
  brass/black/red. What carries over from the old rule is that each sub-biome
  keeps ONE accent nothing else has.
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

---

# Part III — v0.3 "The Living Sky"

First in-game playtests delivered the verdict that shapes this milestone: the systems
work, but the world reads **bare** — sprites are too flat next to vanilla's detail
density, the Mistsea reads as blue water instead of cloud, and the biomes offer too
few things to gather, mine and discover. v0.3 is the "make it feel alive" release:
an art overhaul to full vanilla fidelity, denser and more diverse biomes, sky
weather, and stronger ties between the sky and the world below.

## 18. Quality bar

A screenshot of a player-built base in the Skyreach must sit next to a screenshot of
a decorated vanilla town without looking like modded content. Concretely: vanilla
detail density (3–6 micro-details per 32 px cell), cute rounded silhouettes, zero
flat two-tone fills, and every category matched against 2–3 vanilla references of
the same kind. The process is codified in `.claude/skills/necesse-pixel-art/` and
`docs/assets-style-guide.md`; in-game screenshots from playtests are the acceptance
test for every art batch.

## 19. Art overhaul (every existing sprite gets a detail pass)

Priority = screen area × visibility:

1. **Terrain splats** (Cloudturf, Skystone, Stormslate, floors) — richer variants:
   tufts, cracks, pebbles, edge-blend character. The ground is 70% of every frame.
2. **The Mistsea → a true cloudsea** (see §20).
3. **Rocks, ore, crystals, plants** — silhouette + micro-detail pass, more variants
   per sheet so fields stop tiling visibly.
4. **Mobs and NPCs** — cleaner silhouettes, readable faces, idle charm.
5. **Building set & furniture** — the pieces players stare at longest in bases.
6. **Items** — icon polish last (smallest on screen).

Each batch ships only after the 4× contact-sheet QA gate plus an in-context mock,
and gets verified against real gameplay screenshots.

## 20. The Mistsea, recast

Not water: a **rolling cloud deck** seen from above. Bright puffy tops with soft
self-shadowed billows, slow 8-frame drift, mist wisps curling at island shores.
Swimming becomes wading chest-deep through cloud (same mechanics, new read).
Additions: drifting **cloud shadows** on terrain (visual), **Mist Lily** pads at
shorelines, and fishing into the mist (v0.3 fishing table: mist-dwelling catches).

## 21. Biome life & resources (more to gather, mine and find)

Density tuning first (higher object rolls, more sheet variants), then new content
so each biome has its own forage/mining loop:

| Biome | New nodes/plants | Yields |
|---|---|---|
| Driftlands | Windwheat grass, Cloudberry bush, Drift Boulder, Nimbus Tuft | plant fiber/Windsilk chance, food, stone + rare Aetherium, Cloudfluff (new soft material) |
| Stormveil | Fulgurite Spire, Charged Slate node, Static Bloom | Fulgurite Glass (new), extra Storm Shards, alchemy reagent |
| Aurora Shoals | Prismshell node, Aurora Kelp (mist edge), Chimeflower | Prismshell (new deco/craft material), food/reagent, ambience + petals |
| Mistsea | Mist Lily, fishing table | walk-near-shore decor, fish + rare catches |

New materials feed the building set (Cloudfluff → bed/carpets, Fulgurite Glass →
windows/lamps, Prismshell → furniture trim) so gathering has visible payoff.

## 22. Sky weather ("the sky has moods")

A seeded, level-wide weather cycle in the Skyreach (visual layer + event hooks,
built on the same level-event pattern vanilla uses for its surface events):

- **Radiance** — brilliant clear sky: brighter light, aurora shimmer over the
  Shoals, small gathering luck bonus. The "beautiful day" state.
- **Overcast Drift** — the default; drifting cloud shadows.
- **Tempest** — a storm event centered on the Stormveil: darkened palette,
  lightning strikes (telegraphed AoE), Storm Wisp spawns up, Storm Shard yield up.
  Danger and reward spike together; other biomes stay playable.
- **Mist Surge** — the Mistsea "rises": fog particles, reduced sight range, mist
  creatures surface near shores. Fishing improves.

Weather states announce themselves with a short localized chat line and a palette/
particle shift, never with a UI popup. (A sandstorm-style event stays a surface
concept — in the sky its equivalent is the Tempest.)

## 23. Ties to the world below

The sky should feel connected to the rest of the game, not parallel to it:

- **The cats become recruitable.** After the quest finale the Warden offers to let
  Siggi and Peanut move down to the player's settlement — implemented on the
  vanilla settlement-pet path (each cat individually, via a carrier item he hands
  over; unique looks preserved, baskets placeable in the base, the spire keeps
  spare baskets as their "visiting" spot). Declining keeps them at the spire.
- **Fallen stars**: rare skyfall events drop a small Aetherium-bearing meteor onto
  surface islands after the beacon is lit — surface players see the sky exists.
- **Surface-ingredient requests**: one optional repeatable Warden task asks for
  common surface goods (wood kinds, desert glass, ocean shells) in exchange for
  sky materials — pulling players back down and up again.
- **Skyward Trader** (moved here from v0.4): occasionally visits claimed surface
  settlements once the beacon burns, selling a rotating sliver of sky stock.

---

# Part IV — "The Veil Below" (the afterlife layer)

The mod's second world-pole. Where the Skyreach is the bright vertical, the
**Veil** is its shadow: a gothic-comedy afterlife of moonlit marshes, dusty
bureaucracy and mischievous ghosts — night violet, acid-green ghostlight,
black-and-violet stripes, cute-macabre silhouettes. Everything here is
original design in that mood; reference material stays outside the repo.

Priority note: after playtest enthusiasm, this part is scheduled as **v0.3**,
ahead of Part III ("The Living Sky", now v0.4). The two share the art
direction, and rolling art batches continue through both.

## 24. Getting in: the Séance Circle

No new surface ladder — the Veil is entered by **ritual**. The player crafts a
**Séance Circle** (chalk ring, candle stubs, and one key reagent: the
**Silver Bell** the Warden gives when both cats are home — the sky quest
literally hands you the key to the underworld). Placing and using the circle
at night tears a **Rift**: a swirling portal object that persists (one-world
dimension via the same proven `LevelIdentifier` pattern as the Skyreach,
dimension below the deep caves). The Rift's other side lands in the Veil's
arrival shrine; the way back mirrors it. Multiplayer rides the same portal
netcode as the stairways.

## 25. The Veil (level + sub-biomes)

Infinite, region-streamed, seeded per world — the Skyreach engine reused.
Permanent night; its "weather" is mood variants of darkness.

| Sub-biome | Ground | Look | Signature |
|---|---|---|---|
| **Gloomfen** (common) | murkmoss, black peat | moonlit marsh under a green moon, crooked bare trees, will-o-wisps | ghost-lantern light, whisper reeds |
| **Ashen Reach** (uncommon) | ash sand, cinder rock | grey dune waste with bone-dry ridges | home of the **Ashwyrm** (see §27) |
| **The Model Town** (structure) | cobble miniature streets | a town built at doll scale — walk its streets like a giant | miniature-house loot, Model Wardens |
| **Office of Eternity** (structure) | checkered linoleum | an endless waiting room: benches, flickering signage, a NOW SERVING board that is always wrong | ticket-number quest humor |

## 26. Surface integration (the Veil leaks into the overworld)

The player's first contact happens in the NORMAL world, before any portal:

- **Wormsign in the desert**: once a world's Rift has ever been opened (or a
  vanilla mid-game boss has fallen), vanilla desert islands can roll a
  **Wormground** patch — trembling sand, half-buried ribcages, eggs. Standing
  there too long calls a juvenile Ashwyrm eruption (set-piece fight, not a
  wandering spawn — desert stays vanilla-compatible).
- **A wandering huckster**: **Mortimer the Broker**, a lanky trickster ghost
  in a black-and-violet striped suit (original design: ashen face, crooked
  top hat, absolutely no manners), occasionally visits claimed settlements
  selling haunted deco — and sells the Séance chalk that starts everything.
- **The medium**: **Vesper**, a deadpan gothic seer NPC at the Veil's arrival
  shrine (and later recruitable), handles the quest chain: she reads the
  ticket numbers, translates the Office's forms, and dryly comments on
  everything the player drags home.

## 27. The Ashwyrm

The Veil's mid-boss and the desert set-piece creature: a colossal burrowing
worm with **ember-cracked ash-grey hide** (glowing fissures between armor
rings — deliberately its own design language: no stripes), erupting in
telegraphed arcs. Veil version is the full fight at a summoning altar in the
Ashen Reach; desert juveniles are the appetizer. Drops: Wyrmash chitin
(armor/deco material), Cinder Pearl (light source), a trophy segment.

## 28. "Haunted & Homely" deco set (craftable, overworld-usable)

The gothic building set the sky started, completed by its dark half — all
usable in normal bases so the theme travels home:

ghost lantern (green flame, flickers), striped fence + gate (black/violet),
zigzag runner carpet (violet/black), crooked iron gate, self-playing haunted
piano (plays a bar when walked past), model-house set (3 sizes; the largest
is **enterable** — a doll-scale instanced room, the "walk-in dollhouse"),
wanted-poster board, ticket dispenser (dispenses "Form 13-K: Welcome to the
Hereafter", a flavor-text book item), ghost-train platform pieces (bench,
sign, lamp, rail buffer) for a station diorama.

## 29. Quest sketch: "Three Stamps for Eternity"

1. *Wormsign* — investigate the desert patch, survive the juvenile, find a
   sealed Form 13-K → Mortimer appears, sells chalk, grins too much.
2. *The Séance* — build the circle (needs the Silver Bell → sky quest tie-in),
   open the Rift, meet Vesper.
3. *Take a Number* — the Office of Eternity refuses to stamp your form;
   fetch-and-deliver chain through Gloomfen and the Model Town, each stage a
   new absurd requirement (in triplicate).
4. *The Third Stamp* — the final clerk is asleep inside the Ashwyrm. Summon,
   defeat, stamp, done: the Veil recognizes the player as "properly deceased
   (honorary)" — unlocks Mortimer's full stock and Vesper's recruitment.

Tone rule for all dialogue: dry, warm, a little morbid, never gory.


# Part V — v0.4 "The Living Sky": per-biome fill

Playtest verdict driving this part: the sky reads empty next to vanilla —
every biome needs its own trees, plants, animals, enemies, blocks and ores,
plus inhabited structures. Rules: every addition is unique to its biome,
comes in variants, and feeds at least one loop (build, craft, cook or fight).

## Driftlands — pastoral sky meadows
- **Nimbus Willow** (tree, 2 variants): fluffy cloud-canopy tree; axe →
  3-6 Nimbuswood (+ sapling later). **Nimbuswood floor** buildable tile.
- **Cloudbell** (plant, 2 variants): blue bell flower, pickable material.
- **Sky Tulip** (plant, 3 color variants): pickable deco flower.
- **Zephyr Finch** (critter): tiny darting bird, ambient life.
- **Galehound** (enemy, night): wind-wolf pack hunter, mid HP/speed —
  Driftlands finally has a night threat of its own.

## Stormveil — charged slate
- **Fulgur Pine** (tree, 2 variants): lightning-charred pine; axe → Charwood.
  **Charwood floor** buildable tile.
- **Static Moss** (plant): faint glowing ground moss, pickable.
- **Thunderbloom** (plant, 2 variants): sparking flower, harvestable material.
- **Fulgurite** (ore): fused lightning glass in slate outcrops — material for
  future storm-tier recipes.
- Enemies: Storm Wisp + Zephyr Ray + (since 0.3.4) Skystone Golem — covered.

## Aurora Shoals — pastel glow
- **Prisma Birch** (tree, 2 variants): pale trunk, iridescent canopy; axe →
  Prismwood. **Prismwood floor** buildable tile.
- **Glowfern** (plant, 2 variants): light-emitting fern.
- **Aurora Lily** (plant): glow flower, pickable.
- **Dew Snail** (critter): slow glowing snail.
- **Dawnpiercer** (enemy): crystal-feathered dive bird — fast, fragile,
  burst damage; the Shoals' counterpart to the golem's tankiness.
- **Prismshard** (ore): crystal vein in shoal rock.

## Structures & the second NPC
- **Sky Cottage** (Driftlands, rare preset): small nimbuswood homestead.
  Resident: the **Cloud Shepherd** — second friendly NPC; shop (seeds,
  cloud puff treats, wool trade) and one small journal quest (lost lamb).
- **Storm Ruin** (Stormveil preset): broken watchtower, loot chest, golem
  guards.
- **Aurora Shrine** (Aurora Shoals preset): prism arch, deco landmark.

## Meadow carpets — the density calibration
Reference playtest screenshot (vanilla swamp): lush areas cover 30-60% of
the ground with WALK-THROUGH tall grass the player visibly wades through —
density comes from carpets, not scattered clumps. Therefore each biome gets
a dense tall grass built to tile edge-to-edge (Tall Cloudgrass / Storm
Sedge / Prism Grass, 4 variants each), and the terrain painter gets a
low-frequency MEADOW mask: inside a meadow patch tall-grass coverage is
~70%, outside the sparse per-tile rolls apply as before. Separated hazard/
accent pools remain a candidate for the weather pass.

Ship order: (a) flora + wood/plank blocks + ores, (b) fauna, (c) structures
+ NPC. Each sub-batch passes the pixel-art QA gate and the headless
integration test before release.
