# Current state

Short, current, and rewritten as things change. History belongs in
`CHANGELOG.md` and `docs/PLAYTEST_LOG.md`, not here.

**Version:** 0.6.0 (+ unreleased) · **Game:** Necesse 1.3.2
**Branch:** `codex/realms-integration` (destined for `master`)
**Updated:** 2026-09-02 — Veil gate, four settlers, Crooked Beyond and the
Ghost Realm integrated; Eden and Steinfeld remain isolated WIP branches.

## Architecture in one screen

Four extra dimensions are registered through
`LevelIdentifier.IDENTIFIER_TO_DIMENSION`: **Skyreach** (`skyreach2`, +1),
**The Veil** (`veil2`, −3), **Ghost Realm** (`ghost2`, +4), and **Crooked
Beyond** (`crooked2`, +5). All four are `BiomeGeneratorStackLevel`s that stream
regions, so they are effectively infinite and generate lazily. Ghost and
Crooked compile and register on a real server; their own generated maps have
not yet been exercised by the integration command.

The **Stairway is a portal**. Wherever it is built on the Surface it routes to
one canonical Skyreach origin computed from the world-generation seed
(`worldgen/SkyOrigin`), where the Old Warden Spire stands. Terrain radiates
from that origin: the hub is clamped to walkable land, and ore density widens
with distance band. The return gate resolves each player's own bound stairway
from `quest/SkywatchQuestData`, which persists per-player bindings.

The **Warden** is the progression NPC. Meeting him completes the find-the-spire
quest; paying 30,000 coins recruits him, which places a real `HumanShop`
settler (`mobs/WardenSettlerMob`, registered as settler type
`settlement/WardenSettler`) on the Surface at the player's stairway and hands
over the Silver Bell.

Generated assets are maintained through `tools/asset_generator/`. Player-
supplied replacement PNGs such as the current splats are authoritative inputs
and are explicitly excluded from generator output.

## Green — verified working

- Mod loads on a dedicated server; Skyreach and Veil generate; no log errors.
- World survives a server restart: spire returns at identical coordinates,
  Warden and both cats still present (asserted every test run).
- The Warden's recruit path is live per mob, not just registered:
  `/skyreachstatus` reports
  `recruit check: skywarden settler=WardenSettler price=coinx30000` and
  `recruit check: wardensettler settler=WardenSettler price=free`. Those two
  values are exactly what was null before, and null is what made vanilla's
  recruit button impossible.
- Every registered ID resolves to a display name, including the key classes
  the engine builds rather than our source writing down:
  `name check: skywarden=Test the Sky Warden | wardensettler=Test the Sky Warden`.
- Siggi and Peanut are unkillable and save-persistent by native means.
- `python3 tools/size_audit.py` reports 0 flags.
- Marble Checker floor no longer crashes clients (`ca2ddad`); `scripts/tile_sprite_check.sh`
  proves it headlessly.
- `python3 tools/locale_audit.py` reports 213 registered IDs and 70 literal
  keys named in both locales (re-measured 2026-09-02; was 129/42 when this
  line was written — the count grows every content pass, so treat any number
  here as a snapshot, not a target), and fails if a new registration helper
  appears that it does not know how to see through.
- `python3 tools/tile_behaviour_audit.py` reports 13 tiles (5 floors, 6
  terrain, 2 liquid) matching their declared role and 949 splat cells inside
  the bands measured off vanilla's own sheets.
- `python3 tools/sheet_format_audit.py` reports the 16 wall-sheet door cells
  at the extents the engine draws them at.
- `python3 tools/rotation_variety_audit.py` reports 123 rotation/state
  comparisons in which no cell the engine reads separately repeats another's
  picture. It caught the Skywatch Banner, whose four `PaintingObject` rows
  were one cell pasted four times — the sheet the player meant by "lässt
  sich nicht ausrichten". `tools/rotation_preview.py` renders every one of
  those cells where the engine puts it into `build/qa/rotations/`.
- v0.6 sprint gates (2026-08-25): generator output byte-identical on
  regeneration, `buildModJar` builds, `scripts/integration_test.sh` passes on
  this Mac against the Downloads dedicated-server install.

## v0.6 visual sprint — shipped, NOT yet player-confirmed

Everything below regenerated through the pipeline and inspected on contact
sheets (`build/qa/`, `build/sprite-gallery.html`); no human has seen it in
game yet.

- **Rock family**: 8 Skystone / 6 Veilrock variants with real geological
  characters (slab, strata, boulder domes, fracture, split, rubble, pits,
  terrace), carved irregular perimeters, base-dominant face fills, and the
  vanilla soft-alpha ground skirt instead of the old dark band.
- **Storm Shards**: complete redesign — 4 asymmetric 64px cluster formations
  of tilted, overlapping, value-alternating crystal blades on a shared rubble
  bed; deep violet planes, restrained pale edges.
- **Tree volume pass**: shared `_canopy_volume` (overlap shadows between
  lobes/tiers, one global light field, sheen demotion on the shadow side,
  trunk collar) applied to Nimbus Willow, Prismabirch, Fulgur Pine. Size and
  silhouettes untouched.
- **Cloudberry bush**: rebuilt as a dense leaf-clump dome (~30x20) with woody
  stems and sunk amber berry clusters; greener leaf ramp.
- **Warden**: storm-blue coat ramp (matches the settler's pinned livery),
  hood-down cowl behind the hair, brass collar clasp, weathered mend patches,
  cheek lines. Mob renderer and the HumanShop settler renderer untouched.
- **Spire hero kit**: beacon rebuilt as observatory machinery (sigil plinth,
  banded pillar with a snapped armature, brass yoke, faceted storm lens);
  new `skywatchtelescope` and `skywatchastrolabe` hero accents.
- **Stormveil prop families**: `stormscreed`, `skywatchrubble`,
  `chargecrystal` (lit), `withershrub` — craftable, worldgen-composable later.
- **Aurora accents**: `aurorashards` (lit), `starfall` (lit).
- **Sky oddity seeds** (registered + craftable, deliberately NOT in worldgen):
  `skyballoon`, `aeronautwreck`, `skyparcel`.

## Endgame rebalance — the ladder is written down (`docs/BALANCE.md`)

The player is ten incursions deep and asked for the mod to start "mindestens
auf Niveau der 1. incursion für die schwächsten gegner". **The mod is becoming
endgame-only**, and `docs/BALANCE.md` is the reference every balance change now
points at. It is `VERIFIED [jar]` — read out of the decompiled 1.3.2 sources —
and **not player-confirmed**; nobody has fought anything at these values.

The load-bearing finding: incursion difficulty is a **level modifier**, not mob
stats. `BiomeMissionIncursionData.initModifiers()` (lines 66-69, 117) builds
`ENEMY_MAX_HEALTH`, `ENEMY_DAMAGE` and `LOOT` out of two cumulative arrays that
both **open with `0.0F`**, so **tier 1 applies no multiplier at all** — it is
the raw strength of the classes vanilla spawns in an incursion. Tier 10, for
comparison, is HP x4.00 / damage x2.15 / loot x2.50.

That makes the floor concrete rather than abstract: **1000 HP / 130 damage / 40
armour** (`AscendedGolemMob.MAX_HEALTH` at Classic; `CrystalGolemMob`'s damage
and armour), and for gear **Arcanic — 29 chest / 1900 enchant / EPIC**. The
ladder from there:

| realm | ~incursion | HP | damage | armour | drop value |
|---|---|---|---|---|---|
| Skyreach | 1 | 1000 | 130 | 40 | x1.0 |
| Eden | 3 | 1500 | 165 | 45 | x1.3 |
| Steinfeld | 5 | 2100 | 200 | 50 | x1.6 |
| Ghost Realm | 7 | 2800 | 230 | 55 | x1.9 |
| Crooked Beyond | 10 | 4000 | 280 | 60 | x2.5 |
| Hell | past 10 | 5500 | 340 | 70 | x3.2 |

Role modifiers, per-realm worked examples, the `MaxHealthGetter` difficulty
spread, the gear ladder and a re-derivation recipe are all in `docs/BALANCE.md`.

**This supersedes the deep-cave calibration described in the two sections
below.** Sky Arsenal weapons are calibrated against deep-cave vanilla weapons
and Stormsteel is documented as sitting under glacial (25/26/16, enchant 1300,
UNCOMMON); those statements are now historical — **Stormsteel itself was
retuned the same day** (commit `c18c2f1`, "Stormsteel, the trinkets and the two
blades move up to the incursion tier") to 26/29/19 at enchant 1900, EPIC,
matching `docs/BALANCE.md` §7's gear ladder rather than merely targeting it.
`docs/BALANCE.md` remains a **target, not a record** in general — it is vanilla
arithmetic, not a changelog — but for Stormsteel specifically the target and
the shipped class now agree; read the class for what is actually shipped.

## Sky Arsenal (content/arsenal) — IMPLEMENTED, awaiting player confirmation

The mod shipped two weapons for four releases (`tempestedge`, `galehowl`).
`stairwaytoheaven/arsenal/` adds five more, one per play style, all crafted at
the Tungsten Workstation out of mod materials, and four enemies that drop what
they are made of.

- **Skyreave** (glaive, sweeps a circle) · **Thunderhead** (greatbow, charge
  scaled) · **Prismcaller** (staff + `prismbolt` projectile) · **Skywatch
  Whistle** (summons the Watch Mote) · **Stormdisc** (returning thrown ring,
  three at a time). Each is calibrated against the vanilla weapon of the SAME
  class at the deep-cave tier — quartz glaive, tungsten greatbow, quartz staff,
  cryo staff, tungsten boomerang — named in its class comment.
- **Rime Sentry** (Stormveil + Skyway) · **Aurora Flake** (Aurora Shoals +
  Stormveil) · **Fen Wraith** (Gloomfen + Ashen Reach) · **Cinder Cantor**
  (Ashen Reach + Gloomfen). Each subclasses the vanilla mob whose behaviour it
  wants and wears that mob's own sheet from `MobRegistry.Textures` — no new mob
  art, only a bestiary icon each. All four use `SkySpawnRules.daylightSpawn`
  and the integration test now asserts their accepted lit/dark counts.
- Gates: `./gradlew build`, all five audits and `scripts/integration_test.sh`
  pass; the generator still reproduces every earlier PNG byte-identically.
- **Nobody has swung any of it in the real client.**

## Item polish (content/itempolish) — IMPLEMENTED, not player-confirmed

The player's report: "Aurorablatt usw steht nicht unter itemname in Inventar
etc was es ist.. also Nahrung, Mineral, erz usw.. und es muss in richtige
Kategorie einsortiert sein ... und wir brauchen sinnvolle Sachen die man daraus
herstellen kann wie Accessoires, Rüstungen".

**Every material now says what it is.** `stairwaytoheaven/items/SkyMatItem`
appends one line from `itemtooltip.<stringID>tip`, each opening with the KIND —
Mineral, Ore, Metal bar, Log, Cloth, Food, Mob drop, Quest key. 33 items across
7 item classes carry one in both locales, and `tools/locale_audit.py` fails if
one of them loses it (it finds the described classes by looking for the
`ItemDescription` call, not from a hand-kept list).

**Four items were in the wrong bin, and the bin is what the chest sort reads**
(`Item.compareTo` → `Inventory.sortItems`; settlement storage does not read
categories at all — see TECHNICAL_LEARNINGS): `aurorapetal`
minerals → **materials/flowers** (where vanilla files every picked flower),
`skyweave` mobdrops → **materials** (it comes off a loom, not off a mob),
`cloudpufftreat` mobdrops → **materials** (it is crafted), `silverbell`
minerals → **misc/questitems** (vanilla's own bin for a quest key).

**Stormsteel is no longer a dead end.** It was the Aether Forge's headline
product and nothing in the game consumed it. Four consumers now: the
**Stormsteel set** and the **Stormsteel Vambrace**, at the Tungsten Anvil
beside tungsten's own armour.

> **The numbers below are from launch (this section) and were superseded the
> same day by the endgame rebalance above.** Stormsteel is now **helm 26 /
> chest 29 / greaves 19** at enchant cost **1900**, `Item.Rarity.EPIC`
> (`StormsteelArmor.java`, commit `c18c2f1`) — measured against **Nightsteel**,
> the melee set of the incursion tier (28/29/17), not tungsten. The
> "deliberately under glacial" framing is retired along with the deep-cave
> calibration; `docs/BALANCE.md` §7's gear ladder is no longer just a target
> for this set, it is what shipped. The set-bonus buff and its numbers below
> were not touched by that commit and are believed still current.

<!-- Numbers as they launched, kept for the delta above: helm 25 / cuirass 26
     / greaves 16 at enchant cost 1300, UNCOMMON, calibrated against vanilla's
     tungsten set (24/25/15, 1300, UNCOMMON) and deliberately under glacial
     (24/24/16, 1450). -->
A `SimpleSetBonusBuff` gives the set +15 max resilience and +5%
movement speed, under `GlacialHelmetBonusBuff`'s +20 / +20%.

**Three accessories**, real `TrinketItem`s on `SimpleTrinketBuff`s with no
tooltip key, so the ENGINE prints the numbers: Stormsteel Vambrace
(resilience gain +50% = vanilla `vambrace`, plus max resilience +25 = half of
`chainshirt`), Aurora Locket (+30 max health = 60% of `frozenheart`, +0.5
combat regen = `regenpendant`), Zephyr Harness (+10% speed = `trackerboot`,
+30% stamina = 60% of `zephyrcharm`). All three at the Tungsten Workstation,
where vanilla puts `manica`, `lifependant` and `bonehilt`.

The Dew Snail, the mod's other dead end, now makes Cloudpuff Treats.

Art: `tools/asset_generator/gen_skygear.py` — five `player/armor` sheets and
six 32px icons, drawn on `gen_armor`'s measured human anatomy and
`gen_professions`' stormsteel ramp, QA'd on 6x contact sheets against the
vanilla piece each answers to AND composited onto a real player body.
**Nobody has worn any of it in the real client.**

## Item icons — thin-icon batch (IMPLEMENTED, not player-confirmed)

`tools/size_audit.py` is a hand-maintained mapping, so a sprite with no row is
never measured. **100 of 307 shipped PNGs had a row; 207 did not** — and among
the uncovered were 94 32x32 item icons, **47 of them below the thinnest vanilla
item icon in the dump**. Vanilla's 32x32 icons carry 288-712 opaque px (median
440); the mod shipped `tempestedge` — one of its two original weapons — at
**45 px**, a hairline whose blade core was single stacked pixels per diagonal
step. `docs/REVIEW-2026-08-24.md` listed widening exactly that blade as art
action **#1**; it had stayed undone since.

Twelve icons redrawn through the generator, each briefed against a named
vanilla analogue and its measured mass: `flickerlightgarland` 29→379,
`tempestedge` 45→334, `veilessence` 70→402, `ghostlantern` 77→448,
`wardencandelabra` 78→456, `stormshard` 85→505, `aeronautwreck` 101→466,
`fulgurite` 101→451, `galehowl` 101→310, `glowfern` 101→655,
`withershrub` 113→500, `aurorapetal` 117→461. `player/weapons/tempestedge.png`
and `player/weapons/galehowl.png` change with them because they share the
`_tempest_blade` / `_galehowl_bow` helpers — intended, and now watched.

**The gate changed too, which is the durable half.** `size_audit.py` gained a
row per redrawn icon, so none can silently thin out again — and two real
defects in the gate itself were fixed:

- It **passed by measuring nothing.** `--vanilla` defaulted to a dev-container
  path, so on this machine **0 of 122 rows compared** and it still printed
  "0 sprite(s) flagged" and exited 0. That green tick was quoted in this file as
  verified. It now prefers the checkout's own `vanilla-sprites/`, prints how
  many rows actually compared, and **fails** when nothing was measured.
- The two held weapon sprites are deliberately **manual** rows, not ratios:
  they sit on a 32x32 canvas while every later mod weapon matches vanilla's much
  larger held sheets (`skyreave` 96x95 vs `quartzglaive` 104x88). A mass ratio
  between canvases differing 3x measures the canvas, not the drawing.

Art produced by Codex under brief (`codex exec`, see TECHNICAL_LEARNINGS);
reviewed, gated and integrated here. **Nobody has seen any of it in the real
client.**

### Follow-up: 34 icons still below the thinnest vanilla icon

Measured, uncovered by the audit, and deliberately NOT in this batch — one
coherent set per pass, per the bounded-art rule in `AGENTS.md`. Worst first:
`cloudbell` 120, `thunderbloom` 124, `skywatchtelescope` 129, `aurorabloom` 141,
`prismshard` 141, `stormcrystal` 141, `skyballoon` 146, `auroralily` 148,
`cloudberry` 149, `gloomwillow` 149, `cloudpufftreat` 157, `silverbell` 161,
`skywatchastrolabe` 166, `cinderpearl` 178, `aetheriumore` 181, `aetheriumbar`
184, `cloudberrybush` 186, `skystone` 189, `windsilk` 198,
`starfall` 203, `skytulip` 207, `skyreeds` 210, `mistglasslantern` 213,
`charwood`/`nimbuswood`/`prismwood` 225, `staticmoss` 230, `stormscreed` 239,
`skystonerock` 245, `seraphstatue` 247, `seancecircle` 251, `windwheat` 255,
`skywatchchalice` 267, `skyparcel` 283.

*(`catbasket` was on this list at 186px when it was written; re-measured
2026-09-02 at 344px — over the >= 300 target — so it has since been redrawn
and is dropped from the list. `docs/design/asset-work-order.md` batch E had
independently drifted to a different stale number, 277, for the same icon;
both are corrected.)*

`aurorabloom` (141) is the one to take first: the redrawn `aurorapetal` (461)
now sits beside it in the inventory, and the flower should not read thinner than
a petal picked off it.

## The tile pass (2026-08-30) — IMPLEMENTED, not player-confirmed

Every ground surface in the mod, measured against the vanilla tile dump for the
first time. The dump had been sitting beside the repo unused: `vanilla-sprites/`
held items, mobs and objects but no tiles, so no tile had ever been compared to
anything.

**All six natural terrains were flat** — 63-114 density where vanilla's
sparsest natural ground, snow, carries 294. The four craftable floors were the
opposite problem: density fine, but louder than any vanilla floor and built on
single pixels. And `murkwater` was the flattest surface in the game at 70.

|  | density | mean \|dRGB\| | 2x2 |
|---|---|---|---|
| cloudturf | 63 → 378 | 7.0 | 100% |
| skystone | 70 → 396 | 7.0 | 100% |
| stormslate | 88 → 406 | 10.7 | 100% |
| blackpeat | 64 → 399 | 8.7 | 100% |
| murkmoss | 64 → 364 | 10.1 | 100% |
| ashsand | 114 → 372 | 9.9 | 100% |
| charfloor | 548 → 650 | 34.8 → 11.8 | 62% → 100% |
| gloomwoodfloor | 590 → 642 | 28.1 → 11.6 | 68% → 100% |
| nimbusfloor | 608 → 668 | 40.9 → 18.4 | 61% → 100% |
| prismfloor | 610 → 686 | 39.9 → 20.3 | 59% → 100% |
| murkwater ×2 | 70 → 361/362 | 27/21 → 10.5/9.2 | 84% → 100% |

**A new tile: `aurorashoaltile` (Dawnturf).** The Aurora Shoals had no ground of
their own — the painter's else-branch handed them cloudturf, so the rarest biome
wore the commonest floor. Full family: ramp, material, tile class at priority
215, registration, painter branch, both locales, ledger row, audit role. Proven
placed by `scripts/sky_map_render.sh`: 967 tiles over a 400x400 window.

**The gate changed too.** `tile_behaviour_audit.py` now checks density, mean
loudness and 2x2 block coherence per role, with bands measured off vanilla.
Verified by making it fail on the pre-fix cloudturf.

**Deliberately not touched:** `beetlefreak` and `skyway` are converted from the
user's own reference art — a painting is not judged by a procedural rule.
`mistsea` is in `KNOWN_UNFIXED`: density is fine (617-704) but it runs mean
31.8-49.7 at 40-48% coherence. It is the sky's signature surface and a cloud
deck legitimately has more relief than still water, so it wants its own pass
with the player's eyes on it, not a drive-by.

**Nobody has walked on any of this.**

## The world concept is now law (2026-08-31, later the same day)

The player supplied the final concept for the whole mod and asked that it live
in the repo so it never has to be briefed again. It is **`docs/WORLD_DESIGN.md`**
and `AGENTS.md` reads it second, before anything else. It outranks every other
design document here.

Nine realms on one road: **Skyreach → Eden → Steinfeld → the Veil →
Ghost Realm → Crooked Beyond → Infernal Fringe → Hell Antechamber → Hell**, with
only two real gates (the Veil fog, the Hell Gate) and everything else blending
by distance.

Three things landed with it:

- **The global "muted" palette rule is retired.** `DESIGN.md` used to open with
  *"the Skyreach is not a fluffy heaven fantasy … cool, muted, a little
  hostile"*, the style guide banned "gold-trimmed clouds", and the pixel-art
  skill made "dusty bases" a law. The player: *"alles nur entsättigt sein soll..
  das ist falsch"*. Saturation is now **per realm** (`WORLD_DESIGN` §36); only
  Steinfeld keeps the old register. All four files are corrected in place and
  each says what it used to claim.
- **`docs/VANILLA_ASSET_MAP.md`** — the working method is build with vanilla
  stand-ins now, the player swaps them all in one pass later. §1 is what the
  code borrows today, verified against the source; §2 is the shopping list per
  realm, checked against the real dump.
- **`vanilla-sprites/` is installed** (6,121 files, gitignored). `size_audit`
  has teeth for the first time: 126 of 129 rows compared, 0 flagged.

## The realm field — IMPLEMENTED, not player-confirmed

`worldgen/RealmDepth` is the spine of `WORLD_DESIGN` §3: distance from the spire
becomes a depth 0..1, depth becomes overlapping biome WEIGHTS, and a coarse
noise field picks between the realms that overlap. Measured over 3 seeds:

| tiles | depth | realms present |
|---|---|---|
| 0 | 0.00 | Skyreach 100% |
| 1000 | 0.17 | Skyreach 62% · Eden 38% |
| 1800 | 0.30 | Eden 100% |
| 2500 | 0.42 | Eden 44% · Steinfeld 56% |
| 3200 | 0.53 | Steinfeld 98% · Ghost 2% |
| 4600 | 0.77 | Ghost 70% · Crooked 30% |
| 5200 | 0.87 | Crooked 100% |
| 5800 | 0.97 | Hell 100% |

Pure realms in the middle of each band, real blending at the seams — the "no
concentric rings" the concept asks for. The integration test asserts the two
ends (depth 0 is Skyreach, the far end is Hell) and reports the rest.

**`DEPTH_SCALE = 6000` is the one dial**, and it is a decision, not a default:
`WORLD_DESIGN` §42.1 first sketched 12000, and §42.2 records that this mod has
**no travel system at all**, while §40 requires the player to keep returning to
Eden from Hell. Six thousand is the largest number that stays honest until
waypoints exist.

**What is NOT wired yet**: only Skyreach and Crooked Beyond have any content, so
`RealmDepth` currently decides nothing the painter reads. Crooked Beyond also
still sits at 900 tiles where it belongs at 4210, because Eden, Steinfeld and
the Ghost Realm do not exist and moving it out would empty the near world and
bring back the complaint that created it. That compromise is named in
`SkyOutlands.WRONG_START`, and the status command prints
`crookedNow=900 crookedTrue=4210` every run so it cannot quietly become
permanent.

## The Veil's fog and Soul Exposure — IMPLEMENTED, not player-confirmed

`WORLD_DESIGN` §8 asked for this from the start and nothing had been built.
As of 2026-09-02 the gate exists, in `src/main/java/stairwaytoheaven/veil/`.

**What it is.** Past realm depth **0.581** — about **3486 tiles** from the
spire — a permanent fog stands on the Skyreach. A player inside it without the
Veil Mark accumulates **Soul Exposure**, one stack per second, and gives one
back per second once out (after a three-second grace, so stepping over the line
and back does not shake it off). §8's table is the code:

| seconds | what happens |
|---|---|
| 1-3 | sight dims (blindness 0.10 → 0.20) |
| 4-7 | and slowed (0.15 → 0.30) |
| 8-12 | and life drains (10 → 30 dps, health regen forced to zero) |
| 13+ | 150 dps — a few seconds, after twelve seconds of warning |

The bands are cumulative and the ceiling is 16 stacks. The slow and the regen
shutdown are installed as high-priority `setMinModifier`/`setMaxModifier`
floors, the way `StarvingBuff` does it, so no trinket can shrug the fog off:
§8 says the only way through is §9's Mark.

**Where the depth comes from.** It is derived, not typed:
`VeilRegion.deriveVeilDepth()` scans `RealmDepth.weightOf` for the first depth
where Steinfeld stops being wholly itself and the Ghost Realm has any weight —
i.e. exactly §8's place between §7 and §10, and §38's step from *06 Whispers
Beyond the Stones* to *07 Into the Mist*. Retune the bands and the fog line
follows. This is also **the first thing in the mod that the realm field
actually decides**; until now `RealmDepth` was a pure function nothing read.

**The teleport answer (§8 is explicit about it).** There is no wall, no blocked
tile, no boundary event and no "entered the fog" hook anywhere in the package.
`VeilWorldData.tick()` — a `WorldData`, so `WorldEntity.serverTick` runs it
every tick — asks each online player once a second where they are standing and
applies the buffs on the answer. Rope, portal, bed respawn, mount, admin
teleport, log out inside and back in: same question, same answer.

**What gates the unlock, today.** §9's séance questline — Madame Orla, the
Séance Table, the Ferryman, five ingredients — **is not built**. The Mark is a
set of `ServerClient.authentication` values in `VeilWorldData` (a `WorldData`,
like `SkywatchWorldData`, so it survives a generation bump and cannot be
dropped as an item), per character as §9 requires, and the only thing that
writes it is the admin command **`/veilmark [player] [1/0]`**. When §9 lands,
the Ferryman calls `VeilWorldData.grantMark` and nothing else changes.
`/veilmark` with no arguments reports where the wall is, how deep the player is
standing and how many seconds of exposure they carry — which is the answer to
"why is nothing happening", because it is almost always "you are 3000 tiles
short".

**One gate, not two.** §42.4 says Soul Exposure and the Infernal Visa are the
same mechanic described twice and must not become two code paths. The split is
along the line where they differ and nowhere else: `VeilRegion` = where,
`SoulExposureBuff` = what it costs, `VeilWorldData` = the clock and the unlock
ledger. Hell's gate is a second threshold, a second buff table and a second
auth set in the same three files. Nothing was generalised past that.

**No new art.** The debuff wears vanilla's `buffs/spirithaunted` icon and the
fog is vanilla's own `particles/fog` sheet, both by literal path, both recorded
in `docs/VANILLA_ASSET_MAP.md` §1.3b.

### Two things to know before playing it

- **The fog currently overlaps the deep Beetle Outlands.** The Outlands sit at
  900 tiles where `RealmDepth` puts the Crooked Beyond at 4210 — the
  compromise named in `SkyOutlands.WRONG_START` — so everything past 3486
  tiles is now inside the Veil, including the outer Outlands. That is
  *correct* against the concept (§39 gates the Crooked Beyond behind the Veil
  Mark) and it resolves itself the day the Outlands move out to their true
  band. Until then a player who walks far enough out into the Outlands will
  start taking Soul Exposure.
- **There is nothing behind the fog yet.** Eden, Steinfeld and the Ghost Realm
  are not built, so past the wall is more Skyreach and more Outlands. The gate
  is real; the place it gates is not there yet. That is the signal that the
  next chapter belongs behind it.

### Not implemented from §9

The Mark disables Soul Exposure and leaves the fog visible, as §9 requires. It
does **not** yet "part locally around the player when crossing" — the fog is
drawn client-side by an invisible marker buff that does not know whether its
owner is marked. Cosmetic, and a later pass.

## Direction change (2026-08-31) — ONE world, not two

The player's call, and it reshapes the roadmap:

> *"aktuell ist veil ja als dunkles 2. gebiet gedacht mit dem seance circle
> aber das wird zu viel arbeit, wir machen nur sky region ... bitte nichts
> wegwerfen der bestehenden sachen sondern auf eine welt eindampfen statt
> skyreach und veil."*

**The Veil stops being a destination and becomes material.** Its ground, props,
mobs and its one building now appear IN the Skyreach, gated by distance from the
spire. The Veil dimension itself is still registered and still generates —
deliberately, because un-registering it would strand every save that has been
there — but no new content goes into it, and nothing new should point a player
at it.

What this replaces: "The Veil, properly" in `ROADMAP.md` is no longer a
separate layer to build out. Read that entry as a list of *themes for Outland
chapters* instead (the Model Town, the Office of Eternity, Mortimer and Vesper).

## The Beetle Outlands — IMPLEMENTED, not player-confirmed

The answer to *"das gebiet ist einfach zu weiß und hell und wir brauchen
kontrast"*. `worldgen/SkyOutlands` cuts wrong regions out of the sky, and what
decides them is DISTANCE, not the biome noise.

- **Hard floor at 900 tiles.** Inside it the answer is no for every seed — the
  spire's surroundings cannot roll one. Past it the patch threshold falls
  linearly to 3000 tiles, from the Veil's own measured 0.82 to 0.62.
- **Measured** over 5 seeds and 956,566 land tiles (`Probe`, pure-function
  sampling of `SkyOutlands.isWrong`):

  | distance | share of land that is Outland |
  |---|---|
  | 400 / 800 | 0.00% (the floor holds) |
  | 900 | 0.40% |
  | 1000 | 1.13% |
  | 1400 | 2.39% |
  | 2000 | 7.68% |
  | 2600 | 17.67% |
  | 3200 | 25.16% |
  | 4000 | 26.53% |

- **Built from what already existed**: beetlefreak ground interrupted by
  blackpeat, dead trees, ash bones, gloom shrooms, the Gloom Shade, the Fen
  Wraith and the Cinder Cantor. `biomes/OutlandsBiome` carries its own crate
  loot (veilessence, gloomshroom, charwood) so an Outland crate says where you
  are.
- **The Crooked House now scatters in the sky too** (`CrookedHouseWorldPreset`
  fires on both identifiers; the sky site test goes through `describeTile`, so
  it cannot drop a house on a road or in the Mistsea).
- **`evilwall`** — the one new object, from supplied art. Crystal massifs built
  on the existing outcrop formation field, which is why they come out as
  ridges and knots with walkable gaps rather than as a maze. 2.7-5.5% of
  Outland tiles, measured above.
- **Seance Circles STAND in the world**: one hashed site per 260-tile lattice
  cell that lands on wrong ground — "an bestimmten stellen, nicht random". 2
  portals over the 956,566 tiles sampled.
- The Outlands answer for their own tiles and return early in `describeTile`,
  because everything below that point (outcrops, aurora colonies, wrecks,
  workshops, meadows, scree) is the BRIGHT world's furniture.

**Nobody has walked into one.** Gates: `buildModJar` against the real 1.3.2
Server.jar, all five python audits, and `scripts/integration_test.sh` on a real
server boot — which now covers this region rather than merely surviving it:

- `outlands check:` reports the floor and the ramp, measured off
  `describeTile` at real world positions. The floor is tested as the promise
  ITSELF rather than through a proxy: the whole disc inside 900 tiles is swept
  and the wrong-tile count must be an **exact zero**, out of a land count that
  proves the sweep found ground (`inside=0/13582` on the test world). "The
  spire's surroundings are safe" is a promise this mod makes out loud, and a
  promise that holds most of the time is a different promise. A second
  assertion requires wrongness to have arrived by 3200, so the region cannot
  silently become unreachable.
  - The first version of this gate sampled ±60 tiles around nominal radii of
    200/600/850 and **failed at 850** — because that window reaches 910 and
    legitimately crosses the floor. The world was right and the gate was
    wrong. Measuring true distance per tile removed the whole class of error.
  - The per-radius numbers in the line are ONE seed through a small window, so
    they swing (`r2000=0/1008` here against 7.68% over five seeds offline).
    Only the floor and the by-3200 arrival are asserted, for that reason.
- `spawn check:` now probes `crookedgolem`, `rarecrookedgolem` and
  `crookedarmadillo` — our own classes since the art pass below, but each
  inherits its spawn rule from the vanilla mob it subclasses rather than
  declaring one, so only the live registry can show the entries still place.
  The assertion is deliberately different from the arsenal block's: those must
  accept in DAYLIGHT, these must accept in the DARK, because
  `HostileMob.isValidSpawnLocation` calls `checkLightThreshold`. Asserting
  daylight here would be asserting a bug.

### What is NOT done here, and is the next agent's job

1. **The boss portal has no boss.** In the sky a circle now says
   `misc.seancesilent` ("nothing under this sky answers - yet") instead of
   opening a Veil rift. That is a deliberate honest dead end, not an oversight:
   wiring the summon needs a boss mob to exist first. The Storm Sovereign in
   `ROADMAP.md` is the obvious candidate.
2. **Nothing tunes the wall density.** 2.7-5.5% of Outland tiles is a first
   number, not a balanced one. Re-run the probe after changing it.
3. **`evilwall` drops `crystalstone`** (the player's call, 2026-08-31 —
   *"crystalstone passt doch, sieht gleich aus.. nicht vanilla droppen"*). That
   is a deliberate progression change and the one thing here worth watching in
   a playtest: the sky now has a source of a deep-cave material. It is a LATE
   source — the Outlands start past 900 tiles and only reach ~25% of land by
   3200 — but nobody has played it. Vanilla's `toolTier 10` is still NOT
   copied; pickaxe-gating a whole biome is a separate decision, still open.
4. **Difficulty: PARTLY addressed 2026-08-31, still open.** The player, after
   finishing incursion 10: *"mir ist langweilig! alles zu einfach überall"*.
   The numbers agreed — everything this mod ships tops out near the Skystone
   Golem's 520 HP / 70 damage, while vanilla's ORDINARY ascended mobs sit at
   1000 HP (Classic) and 130 damage behind 40 armour. The Outlands now spawn
   `crookedgolem`, `rarecrookedgolem` and `crookedarmadillo` — our own classes
   wearing our own sheets, each a subclass of the vanilla mob the biome used to
   name by string ID and each inheriting every number from it, so
   `HostileMob.isValidSpawnLocation` is still the live implementation and the
   entries are live rather than inert. What is still open:
   the rest of the mod is untouched, and the sky's own weapons and armour are
   still calibrated at deep-cave tier, so the Outlands now out-scale the gear
   you can craft to enter them.
5. **No Outland-specific loot or structures beyond the Crooked House.**
   The region is the Veil's furniture plus three mobs that are now ours in art
   and identity but still vanilla's ascended cast in every number and
   behaviour — the ladder in `docs/BALANCE.md` did not move for them.
6. **The player has more art coming** ("ich liefere dir gleich noch weitere
   böden usw"). See `docs/design/asset-work-order.md` for what a full biome
   actually needs in tiles.

## Known issues — open

Ordered by the player's own priority. Full detail in `docs/PLAYTEST_LOG.md`.

**P1 — still open**
- Warden frequently stands facing north, so the player sees his back during
  the introduction. (Behaviour fix owned by another agent.)
- Warden's first dialogue dumps too much lore at once. (Another agent.)
- ~~Old Warden Spire layout reads as a small ordinary house.~~ **Superseded
  on 2026-08-31**: the spire was rebuilt as the furnished 21x21 hall described
  further down this file (`worldgen/WardenSpirePreset`, the user's own
  `warden-tower-layout.script`). This row and that one contradicted each other
  for a release; the rebuilt hall is the true one. Still not player-confirmed.

**P1 — fixed, not yet player-confirmed**
- Rock/ore worldgen now uses a formation field (`7ef6486`).
- Aurora flora now grows in colonies (`7ef6486`).
- Galehound silhouette rebuilt (`080ea26`).
- Every registered object and tile has a display name, gated by
  `tools/locale_audit.py` (`eb76cb2`); all six tree and sapling item icons
  exist (`b90dc2a`).
- Harvest tools audited object-by-object against vanilla archetypes; flora,
  bones and wooden oddities no longer need the pickaxe (`a58e43b`, gated by
  the integration test's tool-audit assertions).
- Dewsnail is catchable with the net via the native `NetableMob` pattern
  (asserted by the integration test; not yet swung in the real client).
- The spire's cat basket exists as a real object on the tile the quest calls
  the cats' home, placed once per world including in existing saves; a coaxed
  cat is at it after a save/load round trip and stays within ~7 tiles of it
  (asserted every integration-test run).
- **A placed Cat Basket IS the cats' home, on whatever level it stands**
  (`feature/catbasket`). "ich habe beide gerade platziert und die sind weg oder
  irgendwo anders dann erschienen wo ich es nicht weiss" — the basket was a bare
  `FurnitureObject` with no connection to the cats at all, and their home was
  hard-wired to the spire tile in the Skyreach. Now `objects/CatBasketObject`
  claims the tile on `placeObject` and releases it on `onDestroyed`,
  `quest/CatHome` records it in `SkywatchWorldData` (tile **and**
  `LevelIdentifier`, because a home in a Surface town is not a fact about the
  Skyreach), and `SpireCatMob` travels to it with vanilla's `TeleportEvent` when
  it is on another level. Newest basket wins; breaking the active one sends them
  back to the spire; only cats that have actually been coaxed home move; every
  case says so in chat in both locales. Measured every integration-test run,
  including across a restart.
- The Warden's Spire is a furnished 21x21 hall (`worldgen/WardenSpirePreset`),
  rebuilt to the layout the user supplied
  (`docs/references/presets/warden-tower-layout.script`): a double cloudmarble
  wall ring with a circulation corridor between them, eight doors on the axes,
  an octagonal beacon chamber left deliberately open, and four furnished corner
  rooms — refectory, council table, the Warden's quarters and an archive — off
  the corridor. Everything in it is on a vanilla furniture base class, so the
  tables count as tables, the chairs are sittable and turned to them, the bed
  is assignable to a settler and the table decorations stand on the tables.
  The player now arrives on the railed pad outside the grand door
  (`SkyOrigin.ARRIVAL_OFFSET_Y = 9`) rather than inside the building.
- The Warden's quest chain is a pure function of the world record
  (`SkyWardenMob.chapterFor`); eight reachable save states are enumerated and
  asserted to be owed a chapter (`chain check: ... no-dead-ends`). The
  cross-dimension read no longer gives up when the Skyreach happens to be
  unloaded, which is what made the earlier hand-out fix unreachable in the
  ordinary case.
- ~~The Cloud Lamb is a coherent husbandry animal: shears for Windsilk, breeds
  true, is named Cloudlamb at every age, and eats cloudberries (a `GrainItem`
  now) as well as vanilla wheat — hand-fed or from a feeding trough. All four
  values are measured by `/skyreachstatus` and asserted by the test.~~
  **Contradicted by this file's own livestock section below, and now moot
  either way: the Cloud Lamb never actually bred (`mate=NONE` — no male of its
  species existed) and was removed 2026-09-01/02.** The Glimmergoat replaces
  it as a husbandry animal that does breed. Left here, struck through, so the
  contradiction between this row and the livestock section is visible rather
  than silently resolved by deleting one side.
- v0.6 sprint (list above): rock variants + shadows, Storm Shards, tree
  volume, Cloudberry, Warden visuals, Spire hero kit, Stormveil/Aurora props,
  oddity seeds.
- **Beetlefreak wall rebuilt** (`art/beetlewall`). "Die Wandtexturen sind
  komplett für'n Arsch von der Beetle wall, da stimmt kein Rand, Fenster oder
  sonst was von Layout" — the supplied sheet was one continuous illustration
  painted across the 4x8 auto-tile block, so no cell met its neighbour; its
  eight door cells held lamp posts and partial arches rather than door frames;
  and the window's two views were swapped (a front-facing pane sat in the rows
  the engine draws as the wall's roof). `sheet_format_audit.py` passed on all
  of it, because that audit guards cell geometry, not whether the art tiles.
  `tools/asset_generator/gen_beetlewall.py` redraws the sheet on the layout the
  renderer actually reads, keeping the supplied art's identity (violet stone,
  swirls, cream-and-black bead trim, brass lanterns with green flame, the arch,
  magenta glass, the skull over the door). `tools/wall_render_preview.py` is
  the new gate: it ports `WallObject.addWallDrawOptions` and composes real
  scenes, so "does it tile" is a picture, not an inference. Verified against
  vanilla `stonewall` through the same port. **Not yet seen in game.**
- **Beetlefreak wall, second pass** (`art/beetlewall2`). The player found two
  more faults that every gate called clean, both compositional rather than
  geometric. (1) The doors read as hatches: the door cells' bounding boxes were
  byte-identical to `stonewall`'s, but a cream bead band ran across each leaf
  and the edge-on cells (5, 9 — the doors in every left and right wall) put a
  lantern-topped stub of masonry above the tile edge and a 3px sliver of leaf
  below it, where vanilla runs ONE leaf the full 58px and puts all the ornament
  on the crown above row 96. (2) The side-wall window still showed a
  front-facing pane: rows 0-1 are `getWindowDir == 1`, a north-south wall seen
  from ABOVE, and vanilla draws a slot cut along the wall's top that you look
  down into. Both are now redrawn to vanilla's own grammar (decoded in
  `docs/TECHNICAL_LEARNINGS.md`). `tools/wall_render_preview.py` now renders
  every scene for our sheet AND for vanilla `stonewall` and `woodwall` directly
  beneath it — a scene showing only our own sheet cannot reveal "shorter than
  vanilla" or "wrong view", which is why both faults survived. **Not yet seen
  in game.**
- Fence and fence gate rebuilt against the engine's own column contract
  (`FenceObject` / `FenceGateObject`, cell-by-cell against vanilla
  `ironfence`/`ironfencegate`). The old sheets were drawn to an invented
  layout, so a fence connecting north grew a horizontal rail, every vertical
  run was a 3px hairline, and the west and east runs were on each other's
  side of the tile. Both item icons redrawn (47 -> 672 and 132 -> 864 opaque
  px against vanilla's 576 and 652).
- Fence PLACEMENT: rings are 4-connected (`SkyLandscape.discRing`), road-side
  fence bands are at least `FENCE_MIN_THICKNESS` (1.6 tiles), gate wings start
  at their pillar instead of floating beside it, and a road crossing a ring
  now carries a real fence gate. Lone posts 3.9% -> 0.2%, dead ends
  26.2% -> 6.0% over the offline painter dumps for three seeds.
- The grey `skystone` ground (14.7% of all land) is no longer empty:
  `SkyTerrainPainter.screeObject` gives it a lichen-bed formation field and
  three new objects - Skystone Lichen, Cragbloom, Sky Scree - plus boulders
  and one lit biome accent. 0.032/0.044/0.099 objects per tile -> 0.304/0.352/
  0.356, against 0.311-0.384 on the vegetated grounds.
- The **Skyway Passages** are a real generated biome (`biomes/SkywayBiome`,
  `SkyTerrainPainter.BIOME_SKYWAY`), cut out of the biome field's 0.40-0.47
  band so it borders Stormveil. It carries `skywaytile` as its ground, grows
  the Sky Seraph wild in its frost form at 1 per 85 land tiles, and builds its
  roads out of Cloudmarble: balustrades the length of every passage, fence
  gates where a carriageway breaks one, piers at the gates and Seraph statues
  at the junctions and along the causeways. 14.6% of the sky's land at 0.371
  objects/tile, the densest ground in the world by a small margin (Driftlands
  0.358, Aurora 0.322, Stormveil 0.307), measured over eight seeds and
  2,197,075 natural land tiles.

**P2**
- Tree canopy volume addressed by the v0.6 pass — awaiting player judgement
  (size and silhouettes were never touched).
- Cloudberry bush rebuilt in v0.6 — awaiting player judgement.
- Aurora plant placement was addressed by colonies (`7ef6486`); the new
  shard/starfall accents await player judgement.

- The Veil's ghost lantern object sprite is thin: its item icon carries 77
  opaque px against vanilla `copperstreetlamp`'s 240. The error icon is gone,
  but the sprite itself wants more mass.

**Deferred**
- Warden's shop is empty. The building set is fully craftable at a workstation,
  so nothing is missing — but the recruited Warden currently does nothing.
- `swh_beacon` (BeaconDeliveryQuest) is registered and never handed out — the
  beacon is lit by recruitment now, so the delivery chapter has no place in the
  chain. Either give it a place or retire it.
- Cat behaviour once home is still only "wander near the basket". Where that
  basket is is now the player's choice (see the Cat Basket entry above), but the
  cats do nothing charming or useful there yet, and nothing ties them to the
  recruited Warden specifically.
- The Cat Basket is a quest reward with no recipe, so a player has exactly one.
  If moving house is meant to be easy, it wants a craft.
- ~~`ROADMAP.md` still describes the pre-v0.5 direction.~~ Rewritten 2026-08-30:
  released milestones tabulated, Chapter 01 named as the next piece, the rest
  reordered by priority.
- Wiring the new Stormveil/Aurora prop families into `SkyTerrainPainter`
  (registered + craftable now; worldgen composition is a later, tuned pass).

## Last player-tested state

v0.5.0 build, played extensively in a real long-running Windows save on
2026-08-24. That session produced everything in `docs/PLAYTEST_LOG.md` under
that date, including the Marble Checker save-blocker. Nothing from the v0.6
sprint has been played yet.

## NOT player-verified

Do not describe any of these as working:

- everything in the v0.6 visual sprint list above
- Skystone Golem in game
- the complete Warden settlement lifecycle (recruit → move in → bed →
  happiness)
- cat progression after being brought home
- resource drops across the board
- outer-distance difficulty scaling
- travel/progression end to end
- building materials and custom floors other than Marble Checker

## Skywatch professions — IMPLEMENTED, not player-confirmed

Three settlement workstations a settler runs unattended, in
`stairwaytoheaven/settlement/` and registered by `SkyProfessions`, plus the
four spire furniture pieces the layout wanted.

- **Windsilk Loom** (`windsilkloom`, `CraftingStationObject`) — weaves
  `windsilk` into **Skyweave** (`skyweave`, new) and spins `windwheat` into
  windsilk at 2:1 against the hand recipe's 3:1.
- **Aether Forge** (`aetherforge`, the `ProcessingForgeObject` pattern:
  `GameObject implements SettlementWorkstationObject` over an
  `AnyLogFueledProcessingTechInventoryObjectEntity`) — burns logs to smelt
  `aetheriumore` into `aetheriumbar` at 2:1 (the vanilla forge does 3:1) and is
  the only source of **Stormsteel** (`stormsteelbar`, new).
- **Stormglass Kiln** (`stormglasskiln`, the `CheesePressObject` pattern:
  unfueled `ProcessingTechInventoryObjectEntity`) — fires `fulgurite` and
  `skystone` into **Stormglass** (`stormglass`, new).
- **No new work zone**, deliberately: a zone is a painted area for forestry /
  husbandry / fertilize, while a workstation is found by
  `SettlementStorageManager.assignWorkstation` on an `instanceof` test and its
  job is filed under vanilla's **crafting** priority. See
  `docs/TECHNICAL_LEARNINGS.md`.
- **Spire furniture** in `SkyFurnitureSet`: `skywatchbookshelf`
  (`BookshelfObject`), `skywatchcabinet` (`CabinetObject`), `skywatchclock`
  (`ClockObject`), `skywatchdisplay` (`DisplayStandObject`) — real storage,
  real clock, real display stand, all on the vanilla base classes and at the
  oak family's exact per-rotation row bands. The bookshelf and cabinet spend
  Skyweave; the clock and display stand spend Stormglass, so the professions
  have a consumer inside the mod.
- Art is generated by `tools/asset_generator/gen_professions.py`, which reuses
  the Skywatch family's drawing vocabulary from `gen_skyfurniture`.

Gates: `furniture_audit` now covers 17 pieces and knows the four new base
classes; `locale_audit` now checks `[tech]` display names; `sheet_format_audit`
now checks rotation-column bands, workstation cells, the forge's fire strip and
the kiln's lit sheet; `size_audit` carries 23 new rows;
`scripts/integration_test.sh` asserts, per station, that it is a
`SettlementWorkstationObject`, whether it is a processing inventory, and what
its Tech actually makes. Nothing here has been seen in the real client.
## The sky livestock layer (`content/livestock`, IMPLEMENTED — not player-confirmed)

> **Rewritten 2026-09-02 — this section described the layer as it shipped on
> 2026-08-28 and had gone stale in three ways: it launched with three animals,
> not two; its art was "zero new PNGs, all recoloured" and no longer is; and
> the Cloud Lamb it kept comparing against is gone. See `docs/OVERVIEW.md` §3,
> which is read off the code and is the reference for this layer now.**

Two farmable animals on their real vanilla archetypes, in
`src/main/java/stairwaytoheaven/livestock/` behind one registration class
(`SkyLivestock.register` / `registerItems` / `loadTextures`):

| animal | base | biome | product | taken with |
|---|---|---|---|---|
| Nimbus Yak | `CowMob` | Driftlands | Nimbus Milk | bucket |
| Glimmergoat | `SheepMob` | Aurora Shoals | Aurora Fleece | shears |

The layer shipped with a third animal, the **Thunderquill Fowl** (`ChickenMob`,
Stormveil, Storm Down) — removed since ("a third animal was one too many",
`docs/OVERVIEW.md` §3). The **Cloud Lamb**, a separate and older husbandry mob
that predates this whole package, is also gone: it reported `mate=NONE` (no
male of its species existed, and vanilla's ram only accepts the string
`"sheep"`) and still inherited `Mob`'s `return false`, so its Driftlands
spawn-table entry was permanently inert. The Glimmergoat is its replacement —
it breeds (see `SkyBreed`) and its `validSpawnLocation` is implemented.

Seven recipes now hang off the two products (down from nine — the Fowl's
Thunderplume Cowl line went with it): **Nimbus Milk** — Skycurd (cheese
press), Cloudberry Custard (cooking pot), Nimbus Draught (alchemy); **Aurora
Fleece** — Glimmerstride Boots (tungsten anvil), Skywatch Carpet (carpenter),
Cloud Puff Treats (inventory), and vanilla's own `net` (workstation, aurora
fleece standing in for wool at vanilla's count — see the class comment on
`SkyLivestock.registerItems`).

**Zero new PNGs was true at launch and stopped being true on 2026-09-02.**
Both animals now wear dedicated drawn sheets, `GameTexture.fromFile`, nothing
recoloured: the Glimmergoat has all five of its states (`gimmergoat-doe[_shorn]`,
`gimmergoat-ram[_shorn]`, `glimmergoat-lamb`) and the Nimbus Yak its three ages
(`nimbusyak`, `_bull`, `_calf`) — see `docs/VANILLA_ASSET_MAP.md` §4. Only the
shadow sheets stay vanilla's, because a shadow is a black blob.

Measured every integration-test run (`/skyreachstatus`): each animal's product,
offspring, display name at every age, mate, feed and `validSpawnLocation`.

Nothing here has been seen in a real client: the armour on a player body is
server-invisible, and nobody has watched a Yak or a Glimmergoat move in a
window yet either.
