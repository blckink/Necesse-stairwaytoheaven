# Playtest log

Append-only record of real in-game feedback. Never rewrite an entry when
something is fixed — change its status and name the fixing commit.

Status values: **KEEP** · **OPEN** · **FIXED** · **REDESIGN** · **FEATURE**

---

## 2026-08-24 — v0.5.0 · first extended play of the new build

Played in a real long-running Windows save. This is the first session with
substantial in-game evidence rather than generated contact sheets, so it
outranks any automated visual metric.

### P0 — save blocker

| Area | Observation | Status |
|---|---|---|
| Marble Checker floor | Placing it in the existing Surface base crashed the client instantly. The tile persisted, so the save could no longer be loaded. `ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 2` at `TerrainSplatterTile.getSplattingTexture:120`. | **FIXED** — `ca2ddad`. Root cause and reasoning in `docs/TECHNICAL_LEARNINGS.md`. |

### KEEP — working, do not "improve"

| Area | Observation | Status |
|---|---|---|
| Skyreach overall | The world is genuinely becoming cool. Do not restart the art direction. | KEEP |
| Cloud edges / world boundary | Atmospheric, reads well. | KEEP |
| Biome palette shifts | Make exploration interesting. | KEEP |
| Mini vegetation density | Works very well. | KEEP |
| Tulips, grasses, wheat/reed plants, small flowers | Fit Necesse, make the world feel alive. | KEEP |
| Small blue birds (Zephyr Finch) | Cute, natural, add life. | KEEP |
| Snails (Dewsnail) | Visually good. | KEEP |
| Zephyr Ray | Looks genuinely cool flying around; movement works. | KEEP |
| Storm Wisp | Attack and presentation are cool. | KEEP |
| Aurora plant core sprite | Cute, good art direction. | KEEP |
| Tree size and silhouettes | Much better than before. Do not undo. | KEEP |

### P1 — fix / redesign

| Area | Observation | Status |
|---|---|---|
| Old Warden Spire | Reads as a small ordinary Necesse house, not the ancient origin of Skyreach. The earlier "18-tile plaza" work does not read as a hero landmark in game. Wants a Skywatch/observatory *complex*: larger footprint, connected rooms, courtyard, paths, floor material changes, archive, beacon machinery, lamps, banners, statues, asymmetric ruins. Flat tile architecture only — no faked verticality. Arrival should read: arrival → path/lights → entrance → Warden. | REDESIGN |
| Warden facing | Frequently stands facing north, so the player sees his back during the most important introduction. Should acknowledge and face a nearby player using native behaviour. | OPEN |
| Warden dialogue | Too much text on first contact: large bubble plus a duplicate-looking chat block, full life story, 100,000 written out as prose. Wants mystery → short context → offer → cost, with lore later. | OPEN |
| Rock / ore worldgen | Skystone blocks are evenly scattered singles — reads as rectangular tombstones on a grid. Wants irregular outcrops: groups of ~3–8, compact formations, small veins, L-shaped clusters, large empty gaps, rare solitary stones. Ore should sit inside and around formations so exploration reads vegetation → outcrop → investigate → reward. | **FIXED** — `7ef6486`. Rocks left the per-tile roll and now belong to a formation field. |
| Rock shadows | Far too long and dark; they occupy more screen than the rock and make small blocks look like pillars. | **FIXED — NOT YET PLAYER CONFIRMED** (v0.6 sprint). Root cause found in the sheet itself: face cells were ~86% deep-ramp with a hard bottom outline — an opaque dark band, zero soft pixels. The sheet now bakes vanilla's measured soft-alpha ground skirt (195/195/113/78/55/29, no bottom outline) and fills faces base-dominant. |
| Storm Shards | Read as a little white wall / row of teeth. Flat and repetitive. Wants individually readable crystal bodies at varying heights, widths and angles on a shared base, dark blue/violet interior faces, pale cyan energetic edges. The problem is volume and silhouette, not scale. | **FIXED — NOT YET PLAYER CONFIRMED** (v0.6 sprint). Rebuilt as 4 asymmetric 64px formations of tilted overlapping blades (angled axis walk + belly profile + cut seams, value-alternating bodies, deep violet planes, restrained pale edge ticks) on a shared rubble bed. Size-audit ratio 0.74 → 1.01 of the crystalwall reference. |
| Galehound | Reads as a grey sausage in actual gameplay. Needs a genuine silhouette redesign: clear canine head and muzzle, chest, narrow waist, distinct legs, storm trail, obvious facing. Legs and body must visibly change pose while moving. | **FIXED** — `080ea26`. Silhouette rebuilt (waist, head, legs); mass went slightly DOWN. |

### P2

| Area | Observation | Status |
|---|---|---|
| Tree canopies | Flat, like stacked coloured pancakes. Wants overlapping canopy masses, dark undersides, bright top-left masses, midtones, shadow between lobes, better trunk integration. True pixel art, no smooth gradients. | **FIXED — NOT YET PLAYER CONFIRMED** (v0.6 sprint). New shared `_canopy_volume` pass: overlap shadows where one lobe sits under a higher one, one canopy-scale light field (lit top-left plane / deep lower-right plane, dithered boundary), per-lobe sheens demoted on the shadow side, trunk collar shadow. Size and silhouettes untouched. |
| Fulgur Pine | Same problem: good concept, horizontal layers too flat. | **FIXED — NOT YET PLAYER CONFIRMED** (v0.6 sprint). Same volume pass over all bough tiers + crown; tiers now cast overlap shadows on each other. |
| Cloudberry bush | Far too small; reads as two mushrooms or stones. Wants an unmistakable low berry-bush silhouette with visible berries. | **FIXED — NOT YET PLAYER CONFIRMED** (v0.6 sprint). Rebuilt as a dense leaf-clump dome (~30x20, vanilla berrybush construction compressed into the grass tile) over woody stems, with amber berry clusters sunk into the mass; leaf ramp pushed into the Driftlands green family. Two distinct silhouettes. |
| Aurora placement | Sprite is good; colonies look mirrored and procedural. Wants colonies of ~1–5, irregular spacing, occasional singles, occasional richer patch. | **FIXED** — `7ef6486`. Lattice colonies of roughly 1-5; sprite untouched. |
| Harvest tools | Trees correctly need an axe — keep that. Much of the remaining flora is pickaxe-harvestable regardless of material. Needs an object-by-object audit against the nearest vanilla equivalent: tool type, tier, HP, speed, drops. | **FIXED — NOT YET PLAYER CONFIRMED** (`a58e43b`). Root cause: every custom deco object inherited the engine's `toolType=PICKAXE`/100 HP default. Audited object by object against the decompiled vanilla archetypes: gloomshroom, withershrub, stormscreed, skyparcel → breakable like vanilla clutter (any tool, 1 HP); ashbones → CowSkeletonObject (any tool, 50 HP); gloomwillow, deadtree, aeronautwreck → axe (woody); skyballoon → any tool (vanilla tent). Stone/crystal/machinery props deliberately stay pickaxe; quest beacon/anchor stay unbreakable. Asserted per object by the integration test. |
| UI / localization | Building recipes display internal IDs, some object names show string IDs, Prism sapling has a missing/error icon, some building and menu entries incomplete. Needs a complete registry audit, not spot fixes. | **FIXED** — `eb76cb2` (18 missing display names, incl. the Stairway itself) and `b90dc2a` (all six tree/sapling item icons, not just Prism). Both now gated by `tools/locale_audit.py`. |
| Snails | Should be catchable with the net using the native critter pattern (butterflies, bees). Players naturally try it. | **FIXED — NOT YET PLAYER CONFIRMED**. The Dewsnail now implements vanilla's `NetableMob` marker — the entire native mechanism (`NetToolItem.canHitMob` checks exactly it; catching removes the mob through the normal death path, so its loot still drops). Asserted by the integration test; an actual net swing in the client is still unobserved. |

### Cats

| Area | Observation | Status |
|---|---|---|
| Siggi and Peanut | Important recurring characters. Must never be permanently killable. Long-term: live with the recruited Warden once the player builds cat-home furniture, roam and rest naturally, eventually a small charming or useful behaviour. Do not rush risky architecture for this. | KEEP (immortality verified) / OPEN (settled behaviour) |

### Not yet player-verified

Skystone Golem · complete Warden settlement lifecycle · Warden bed and
happiness behaviour · complete cat progression · all resource drops · outer
radial difficulty · direct travel progression · all building materials and
floors (Marble Checker blocked testing the others).
