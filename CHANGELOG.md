# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com); versions follow the ROADMAP milestones.

## [0.4.1] — 2026-08-24

Art quality pass + full project review.

### Changed
- **Color identity & contrast pass** across the sky roster (deterministic
  palette regeneration, 18 textures): the **Zephyr Ray**'s back ramp was
  deepened two steps — its old base sat at the Mistsea's luminance and the
  ray vanished against open cloud — with a strengthened teal accent; the
  **Galehound** gained a storm-dark back, brighter highlights and detached
  cyan wind flecks off mane and tail (night-readable color identity); the
  **Skystone Golem**'s plates separate through deeper shadows with greener
  moss and brighter eye glow; the **Sky Warden** wears a dotted gold hem
  trim as one warm accent; **Cloudturf** shifted hue to silver-green so
  islands read as living meadows against the pale Mistsea.

### Added
- `docs/REVIEW-2026-08-24.md`: full professional review — verification of the
  modding structure against the official wiki, asset-quality findings, and a
  prioritized expansion plan (map division with distance rings + landmarks,
  random events/weather, NPCs, recipes, weapons, fishing).

## [0.4.0] — "The Living Sky" — 2026-08-24

The fill release: every sky biome gets its own trees, meadows, flowers,
ores and animals — plus a size correction across the old art.

### Added
- **Trees, wood and floors, one family per biome**: the **Nimbus Willow**
  (Driftlands, cloud-lobe canopy with weeping strands), the **Fulgur Pine**
  (Stormveil, charred tiers with live embers — one variant lightning-split)
  and the **Prisma Birch** (Aurora Shoals, banded bark, iridescent crown).
  Full vanilla tree mechanics: axe them for their own logs, saplings drop
  and replant, leaf particles drift on the wind. Each wood crafts its own
  buildable plank floor (Workstation).
- **Meadow carpets**: large walk-through tall-grass fields (~70% ground
  coverage inside meadow patches, calibrated against vanilla lush areas) —
  Tall Cloudgrass, Storm Sedge and Prism Grass, tiling edge-to-edge so
  whole stretches read as one living meadow you wade through.
- **Six pickable plants**: Cloudbell, Sky Tulip (3 colors), Static Moss,
  Thunderbloom, Glowfern, Aurora Lily — all drop materials.
- **Two new ores**: **Fulgurite** (lightning glass, Stormveil) and
  **Prismshard** (crystal veins, Aurora Shoals), mined off skystone
  deposits like Aetherium.
- **Four new animals**: the **Galehound** (night pack hunter of the
  Driftlands meadows), the **Dawnpiercer** (glass-cannon dive bird of the
  Shoals), and two critters — the **Zephyr Finch** and the **Dew Snail**.
- All new content localized (en/de) and fed into world generation with
  per-biome density bands.

### Changed
- **Size correction pass**: a new audit tool (`tools/size_audit.py`)
  measures every mod sprite against its vanilla analogue; ten undersized
  sprites were rebuilt to vanilla mass — most dramatically the Séance
  Circle (2% of a vanilla ritual altar before), the Skywatch Banner (now a
  proper two-tile wall drop), the gloom willow, streetlamp, wall lantern,
  raven statue, gloomshroom, both crystal clusters, windwheat and
  skyreeds. The size law is codified in the art pipeline rules.

## [0.3.4] — 2026-08-23

Playtest fixes: real journal quests, correct walls, a warden with presence.

### Added
- **Real HUD quests**: the whole Warden chain now runs through the vanilla
  quest journal — "The Warden's Call" (find the spire), "Light the Beacon"
  and "Anchor the Island" (delivery quests with live per-item progress bars),
  and "The Spire Cats" (two-cat checklist) appear in the tracked sidebar and
  the quest log, sync in multiplayer, support team-sharing and abandoning,
  and are given/completed at the exact same dialogue points as before. Chat
  lines remain as flavor; the objectives no longer live only in chat.

### Fixed
- **Wall inner corners no longer break**: the wall sheet's autotile blob was
  decoded cell-by-cell against the game's actual draw code — the face halves
  sat one row off and the inner-corner/junction pieces (rows 5-7) contained
  the wrong art, which rendered as misplaced squares wherever walls met in an
  L or T. Both wall sets regenerate with the correct piece semantics.
- **The Sky Warden has real presence**: rebuilt on vanilla chibi proportions
  (the player's head alone is ~26px wide; the old warden's whole coat was
  ~11px, reading a third as wide as the player).
- **Quest structures are unbreakable**: the beacon (lit and unlit) and the
  sky anchor can no longer be mined — an older jar let a pickaxe remove the
  beacon with no drop, soft-locking the chain. Worlds where it already
  happened self-heal: the level re-places the beacon in its correct
  quest-stage state.
- **The spire now spawns near your arrival**: it anchors to the first ascent
  stairway (radius sweep from ~20 tiles) instead of the world origin, which
  could put it hundreds of tiles from a base far from spawn. Existing worlds
  keep their placed spire (the map marker covers finding it).
- **Golems actually appear**: they patrol the Stormveil crystal fields too
  now (they were exclusive to the rarest biome, the Aurora Shoals, where
  their cap is also raised slightly).

## [0.3.3] — 2026-08-23

Never lose the tower again.

### Added
- **World-map markers**: on a player's first ascent, the **Warden's Spire**
  and **their arrival stairway** are pinned on the world map (M) with their
  own icons — the vanilla cartographer-map mechanism, no cheats. Delivered
  once per player (persisted server-side), and markers live in the client's
  per-world map data like any hand-placed marker, so deleting one is
  respected. Players already in the sky can run `skyreachstatus` once to get
  the spire pinned retroactively; the stairway marker follows on their next
  ascent. Both icons are also available in the map's own marker editor.
- Two new 32×32 map icons in the vanilla `ui/mapicons` style (spire with
  teal beacon, marble stairway on a cloud).

## [0.3.2] — 2026-08-23

Art release: living sprites, organic construction.

### Changed
- **The Storm Wisp is finally alive**: rebuilt on the vanilla flying-spirit
  pattern with a 4-frame animation — a flame-teardrop spirit whose trailing
  tendrils undulate, whose top lick sways, and whose rim lightning crawls
  around the body, with a breathing glow halo synced to the crackle. It was a
  static orb ("an unanimated dot") before.
- Storm Wisp bestiary icon redrawn to match the new flame-teardrop body.
- **Zephyr Ray rebuilt as an organic manta**: wings are curved membranes now —
  convex leading edge, scalloped trailing edge, shading bands that follow the
  sweep — with buried wing roots, cephalic lobes, mantle spots and an
  S-curved tail whip; folded frames sweep the tips down and back, so the flap
  visibly curls through the stroke instead of just scaling. Icon matches.
- **Gloom Shade hem now genuinely undulates**: stable hood with a floppy
  point, asymmetric drifting cloak masses, and a ragged tendril hem that
  re-poses on every walk frame (the vanilla spirit construction), with claw
  arms and glowing eyes applied after the outline.
- **Flora made organic across both dimensions**: windwheat arcs in a fanning
  clump with nodding seed heads (was a picket fence of straight stalks);
  skyreeds and whisperreeds got uneven clumps, curved blades and hooked or
  fluffed tips; the cloudberry bush a lumpy lobed canopy with berry clusters;
  gloomshrooms bell-dome caps with wavy skirts and broken glow-gills (were
  flying saucers); ash bones individually bowed ribs, ash mounds and a real
  skull; the dead tree / gloomwillow a continuous S-curve trunk with weeping,
  fully connected branches and the raven re-perched on a branch elbow.
  Grass-class sprites now skip the outline pass like vanilla grasses.

### Fixed
- **Build works on the newest JDKs**: Gradle wrapper upgraded 8.10.2 → 9.7.1
  (checksum-pinned), so building with JDK 24/25 no longer crashes in Groovy
  (`Could not initialize class ...ReflectionCache`). The shared build script
  no longer uses the internal `org.gradle.internal.os.OperatingSystem` API.
  README troubleshooting documents the crash signature and the
  wrong-folder trap (building the bare upstream template instead of the
  mod branch yields an empty `1.0` jar).
- README troubleshooting now covers BOTH artifacts a broken sky/Veil level
  leaves in the world save — `levels/<level>.dat` AND the
  `levels/regions/<level>/` folder — plus the `worlds/` folder layout some
  co-op tools use, and clarifies that in multiplayer only the host's save
  holds world data (joining players have nothing to repair).

## [0.3.1] — 2026-08-23

Playtest feedback release: real livestock, real landmasses.

### Changed
- **The Cloudlamb is now actual livestock**, not a critter: it extends the
  vanilla sheep, so **rope-catching, feeding troughs, breeding, growing up
  and shearing** (yields wool) all work exactly like surface animals — build
  them a pen in the sky or lead them home. Redrawn at **vanilla sheep scale**
  (it was bug-sized) on the vanilla sheet layout, with a sheared look and
  fleece death particles; sky twist: grown lambs drop windsilk.
- **Much more land**: island noise retuned in both dimensions (Skyreach
  threshold 0.55 → 0.48, scale 42 → 54; Veil 0.50 → 0.44, 46 → 56) — bigger,
  more frequent landmasses, so travel is walking with occasional swims
  instead of aimless paddling through clouds. Applies to newly generated
  regions; for a fully coherent layout, reset the level per the README's
  troubleshooting section (existing explored regions keep their old shape).

## [0.3.0] — "The Veil Below, Part 1" — 2026-08-23

The mod's second world opens: a gothic-comedy afterlife layer below the deep
caves, entered by ritual. Part 1 ships the dimension, its first biomes, its
first resident enemy and the signature light; Mortimer, Vesper, the quest
chain, the Model Town, the Office of Eternity and the Ashwyrm follow in 0.3.x.

### Added
- **The Séance Circle**: craft it (6 Storm Shards + 4 Windsilk + 2 Aurora
  Petals, Tungsten Workstation), place it, and use it while carrying the
  **Silver Bell** from the Warden's cat quest — the bell is the key and is
  never consumed. The circle tears open a persistent **Rift** (vanilla
  ladder netcode, multiplayer-safe, auto-placed return rift on the far side).
- **The Veil** (dimension −3, below the deep caves): infinite, seeded,
  region-streamed, and in **permanent night** — the vanilla "light = safety"
  rule shapes the whole layer. Two sub-biomes: the **Gloomfen** (dark moss,
  black peat, whisper reeds, glowing gloomshrooms, dead trees) and the
  **Ashen Reach** (ash dunes, bone-strewn, veilrock ridges). Between the
  landmasses lies still black **Murkwater** (animated, bridgeable — placing
  tiles reclaims murkmoss).
- **The Gloom Shade**: the Veil's first enemy, built on the new organic-mass
  construction standard — a hooded fen ghost with a hollow glowing face,
  claw arms and a wispy tail. Drops **Veil Essence**.
- **Forage & light**: gloomshrooms are replantable natural light; **Ash
  Bones** ribcages break into **Cinder Pearls**, which craft the
  **Ghost Lantern** (green-flame streetlamp, wire-toggleable, usable in any
  base). Veilrock drops plain stone.
- `veilstatus` admin diagnostics command; the integration test now generates
  and verifies the Veil too. Locale: English + German for everything.

## [0.2.7] — 2026-08-23

### Changed
- **Skystone Golem rebuilt from scratch** on vanilla mob construction
  principles (studied from the game's own heavy creatures): the body is now
  organic overlapping boulder masses with per-mass volumetric shading instead
  of stacked rectangles. Ribbed armor plates across the belly, shoulder-cap
  stones, articulated stomp walk (legs lift, arms swing), a head sunk between
  the shoulders under a heavy brow ledge with deep eye sockets and glowing
  pupils, chest cracks, moss clumps and a solid aetherium crystal spur with a
  lit tip. The profile view is a proper hunched ape posture with a big
  leading arm reaching toward the ground.
- Style guide gains the "round overlapping masses" construction rule; the
  same template is earmarked for the Ashen Reach creatures of v0.3.

## [0.2.6] — 2026-08-23

The "living world" release: light is safe again, the sky gets wildlife and
forage, and the stairway finally looks like its name.

### Fixed
- **Torchlight protects again.** v0.1 raised the mobs' spawn-light threshold
  so they could appear in daylight — which silently broke the game's core
  "light = safety" contract: enemies kept spawning inside lit, cleared areas.
  All three enemies are back on vanilla spawn-light rules (they spawn only in
  darkness), and every biome table now uses local caps (`addLimited`), so a
  cleared area STAYS calm instead of refilling endlessly.
- Difficulty is tiered per biome: Driftlands stay light (few Zephyr Rays),
  the Stormveil is the pressure biome (Wisps + Rays), Aurora Shoals spawn
  rare-but-hard Golems.

### Added
- **Ambient wildlife**, one species per biome, huntable for small forage
  drops: the **Cloudlamb** (Driftlands, windsilk), the **Glowmoth** (Aurora
  Shoals, aurora petal — the moths from the Warden's cat story), and the
  **Sparkbeetle** (Stormveil, storm shard). Bestiary icons included.
- **Forage plants** in the Driftlands: **Windwheat** (harvest 3, spin into
  1 Windsilk by hand) and the **Cloudberry Bush**, dropping **Cloudberries**
  — the sky's first food item (vanilla forage-food pattern, spoils).

### Changed
- **Stairway pair completely redrawn**: a grand pale flight with readable
  6px steps, iron handrail with posts, a cloud ring wrapping the climb, a
  soft light burst at the top and a marble base plate with corner balusters
  (sky-side version descends into the cloud deck). Bold new inventory icon.
- **Enemy detail pass** to the vanilla character bar: the Zephyr Ray gains
  wing-finger ridges, a diamond spine pattern and teal accent rows; the
  Skystone Golem gains armor-plate seams, pauldron caps, knuckle grooves, an
  aetherium crystal spur, moss clumps and a glowing chest rune; the Storm
  Wisp gains hollow eye voids with bright pupils, a jagged mouth crack,
  forked lightning arcs and longer trailing streamers.

## [0.2.5] — 2026-08-23

Art batch 2 — the quest landmarks reach vanilla detail density (measured
against vanilla banners and character sprites from playtest screenshots).

### Changed
- **Sky Warden redrawn**: the coat palette sat so close to the outline color
  that the figure rendered as a black cone. New material-separated look:
  lifted violet-grey coat ramp with shoulder rim light, center and skirt
  creases, iron belt with buckle, hip satchel, ragged hem with pale lining,
  two-row feather collar with bright tips, full hair crown + curtains, long
  beard, and a taller staff crowned by a riveted iron cage lantern with a
  glowing core and floating halo (all four facings + walk/swim frames).
- **Warden's beacon rebuilt** (off + on): two-step masonry base with mortar
  joints and cracks, tapered riveted iron column, flared brazier bowl, a
  four-rib cage with finial. Off shows dead coals, soot streaks and one
  faint ember; on burns with a white-teal core, flame tongues, sparks and a
  glow halo, with light catching the rivets and bands.
- **Skywatch banner redrawn**: iron mounting rod with rings and end caps,
  silver-bordered night cloth with fold shading, a bold two-pixel crescent
  moon + star emblem, swallowtail bottom with fringe tips.

## [0.2.4] — 2026-08-23

Art batch 1, driven by playtest + reference feedback: terrain with real material
character, an actual raven, chunkier clouds, and the gothic night-violet
direction made official (see the style guide's new "Art direction" section).

### Changed
- **Terrain splats rebuilt on vanilla's construction principle**: calm base +
  per-variant detail clusters (before: uniform speckle with all four full
  variants identical — "recolored" instead of distinct). Cloudturf gets chunky
  tuft clusters and cloud-moss patches, Skystone gets fissures and chipped
  facets, Stormslate becomes layered night-violet slate with electric charge
  veins, Gloomwood gets knots, nail heads and grain streaks.
- **Stormslate palette shifted to night violet** — the walkable purple night sky.
- **Mistsea clouds v2**: bigger rounded lobes, hard sunlit rim on every lobe's
  upper edge, dither removed — crisp cartoon-cloud read.
- **Gloomraven statue redrawn as an actual raven**: profile pose with a big pale
  hooked beak, eye ring, folded wing with sheen, long tail, two-step plinth.
  The inventory icon follows automatically.

### Fixed
- Sprite gallery: the viewer's CSS reset squashed oversized sheets — the Mistsea
  animation appeared to flicker and wide sheets rendered blurry/downscaled. All
  sheets now render unscaled and pixel-crisp.

## [0.2.3] — 2026-08-23

First real-client playtest feedback release.

### Fixed
- **The Sky Warden (and the cats) could not be talked to at all**: `Mob.canInteract`
  defaults to `false` in the engine, so the client never offered the interact
  prompt and the server dropped interact packets — the entire quest chain was
  unreachable in-game. Both NPCs now override `canInteract` for players. (Found
  only in client play; the headless test has no player and now this is documented
  as its known blind spot.)

### Changed
- **The Mistsea now looks like a rolling cloud deck** instead of flat water:
  layered puffy billows with sunlit tops and self-shadowed valleys, seamless
  8-frame drift (big masses east, detail wisps west), thinner/lighter shore band.
- **World density up ~1.6x** across all three biomes (playtests: the sky read as
  bare) — more rocks, crystals, blooms, reeds and ore. Applies to newly generated
  regions; already-explored areas keep their objects.

### Dev
- Added `.claude/skills/necesse-pixel-art/` (the distilled, verified style +
  format + QA knowledge) and a `pixel-artist` agent definition that uses it.
- DESIGN.md Part III + reworked ROADMAP v0.3: "The Living Sky" concept (full art
  overhaul, cloudsea, per-biome gathering loops, sky weather cycle, cats as
  recruitable settlement companions, fallen-star events).

## [0.2.2] — 2026-08-22

Cheat-free wayfinding — the quest is now fully navigable without any commands.

### Added
- The first-ascent hint names the compass direction toward the Warden's Spire
  ("Something flickers over the mist to the north-west…"), so finding the
  Warden needs no coordinates. The spire is now stamped during the very first
  ascent instead of on the level's next tick, making the pointer exact.
- The Warden points toward each runaway cat: the cat-quest briefing gains a
  direction line, and his reminder line now names the bearing to Siggi and
  Peanut instead of just their biomes.

## [0.2.1] — 2026-08-22

### Added
- `skyreachstatus` now prints a **locator** when run by a player standing in the
  Skyreach: your tile position plus distance and compass direction to the
  Warden's Spire and to each cat that hasn't been brought home yet.

### Changed
- `skyreachstatus` NPC count is labeled "(loaded regions only)" — a cat resting
  in an unloaded region is not missing, it just isn't loaded right now.

## [0.2.0] — "The Warden's Call" — 2026-08-22

The Skyreach gets a resident, a memory, and things worth building. Built and
integration-tested headless against Necesse **1.3.2** (structure stamping,
NPC spawning and quest data verified on the dedicated server).

### Added
- **The Sky Warden**: a stationary, invulnerable keeper NPC living in the
  **Warden's Spire** — a ruined 15x15 Skywatch tower stamped exactly once per
  world at a seed-deterministic spot in the Driftlands. Placement runs lazily
  on the level's first server tick, so worlds started on v0.1 get the spire
  too. Persisted via a new `skywatchquest` level-data component.
- **Quest chain "The Warden's Call"** (interact-driven, multiplayer-safe,
  server-authoritative turn-ins using the vanilla check-then-remove idiom):
  1. *A Light over the Mist* — find the Warden (chat hint on first ascent)
  2. *The Dark Lighthouse* — deliver 12 Storm Shards + 8 Windsilk; the
     spire's beacon visibly ignites
  3. *Where the Cats Wander* — craft Cloudpuff Treats and bring **Siggi**
     (black, Stormveil) and **Peanut** (white-tabby, Aurora Shoals) home;
     they stay as permanent spire residents
  4. *Anchor of the Sky* — deliver 5 Aetherium Bars + 20 Skystone; the
     island anchor appears; finale dialogue teases the Storm Sovereign
  Dialogue plays as speech bubbles (PacketMobChat) plus chat lines, fully
  localized in English and German.
- **Spire cats**: two unique invulnerable critters with HomesickCritterAI
  home tethering, hidden at seed-deterministic lairs, moved home with a
  cloud-puff when fed a treat.
- **"Nightfell & Skylight" building set**: Skystone Brick and Nightfell
  walls (each with auto-generated door + window via the vanilla wall
  system), Checkered Marble floor (world-locked pattern), Gloomwood floor,
  Wrought-iron sky fence + gate, Warden's Candelabra (streetlamp, wire-
  toggleable), Mistglass Lantern and Flickerlight Garland (wall lights with
  4 attach orientations), Gloomraven Statue, Gloomwillow dead tree, Cat
  Basket, Skywatch Banner (painting). Garland/basket/banner are
  quest-exclusive; everything else has Workstation recipes in sky materials.
- Items: Cloudpuff Treat (+ recipe), Silver Bell trophy.
- `skyreachstatus` now also reports quest state, spire integrity and NPC
  counts; the integration test asserts them.

### Changed
- **Terrain render upgrade**: Cloudturf, Skystone, Stormslate and the new
  Gloomwood floor moved to the modern `_splat` autotile atlas format
  (verified cell map; 4 full-tile variants + 17 marching-square blend
  cells); the Mistsea gained real animated 8-frame liquid splats instead of
  the flat-color fallback.
- Ore overlay sheets corrected to the vanilla Nx32x32 pattern-strip format.

## [0.1.0] — "First Ascent" — 2026-08-22

First playable release. Built and integration-tested against Necesse **1.3.2**.

### Added
- **The Skyreach**: persistent one-world sky dimension (`skyreach`, dimension `+1`),
  infinite and region-streamed like the cave layers, with a per-world seed derived from
  the world seed.
- **Stairway to Heaven** object pair: craftable surface stairway (Tungsten Workstation,
  8 Tungsten Bars + 15 Quartz — deliberately gated alongside the Deep Cave Ladder) and
  the auto-placed sky-side return stairway; both ride the vanilla ladder/portal netcode.
  Ascending onto the Mistsea forms a Cloudturf landing instead of vanilla's dirt fill.
- **Sub-biomes** painted into the biome layer: Driftlands (common), Stormveil
  (uncommon), Aurora Shoals (rare) — each with its own spawn table and object set.
- **Tiles**: Cloudturf (organic), Skystone, Stormslate, and the swimmable Mistsea
  liquid; placing tiles over the Mistsea reclaims Cloudturf.
- **Objects**: Skystone Rock, Aetherium Rock (ore), Storm Crystal and Aurora Bloom
  (2×1 glowing clusters), Sky Reeds.
- **Enemies** (Tungsten-era tuning): Zephyr Ray (220 HP melee flier), Storm Wisp
  (280 HP ranged, spark bolts), Skystone Golem (520 HP armored bruiser). All spawn in
  daylight via raised spawn-light thresholds.
- **Items**: Skystone, Aetherium Ore/Bar, Storm Shard, Windsilk, Aurora Petal;
  **Tempest Edge** (sword, 68 dmg/290 ms) and **Galehowl** (bow, 62 dmg/480 ms,
  +10% arrow velocity) as Tungsten-tier sidegrades, with recipes and loot tables.
- **Localization**: English and German.
- **`skyreachstatus`** admin command: world-gen statistics + placement diagnostics for
  bug reports and automated testing.
- **Asset pipeline**: deterministic Python/Pillow generator producing all 34 textures in
  vanilla sheet formats (`tools/asset_generator/`).
- **Headless integration test** (`scripts/integration_test.sh`): boots the official
  dedicated server with the mod, generates a world, drives `skyreachstatus` via the
  console and asserts clean logs + expected generation.
- Documentation set: DESIGN, ARCHITECTURE, ROADMAP, asset style guide, and a
  `docs/research/` knowledge base (verified engine notes: modding API, registration
  APIs, asset formats, community patterns, progression reference).

### Changed
- Build modernized to Gradle 8.10 (runs on current JDKs; still emits Java 8 bytecode via
  `options.release`). Added `NECESSE_GAME_DIR` env override and dedicated-server
  (`Server.jar`) fallback so CI/headless environments can build without a Steam install.

### Fixed (during pre-release testing, caught by the integration test)
- Startup crash: `materials.ores` is not a vanilla item category — Aetherium Ore now
  registers under `materials.ore`.
- Storm Crystals/Aurora Blooms vanished at world-gen: crystal clusters are 2×1
  multi-tile objects; the painter now writes both halves and reserves the partner tile.
- All natural sky decor vanished at world-gen: freshly generated islands report
  `isShore` everywhere until the liquid height map settles; sky decor now sets
  `canPlaceOnShore`.
- Sky Reeds refused to generate: Cloudturf is now flagged organic soil.
- Skyreach terrain was identical in every world: the lazy level-creation path passes no
  seed; the level now derives its seed from the persisted world seed.
