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

---

## 2026-08-27 — v0.5.0 · second pass, hub and building set

Reported from the same Windows save after the P0 fix landed.

| Area | Observation | Status |
|---|---|---|
| Spire door height | "Die Tür vom Warden-Turm ist irgendwie 3x so hoch wie normale Türen, aber der Rest der Wand ja nicht." | **FIXED — NOT YET PLAYER CONFIRMED**. Root cause in the wall sheet, not the preset: `WallDoorObject` draws its 32x128 cell at `drawY - 96`, so sheet row 96 is the tile's top edge. Our generator painted all eight door cells from row 0, giving a 128px door against a 48px wall. Cells rebuilt at the extents measured off vanilla `stonewall.png` (closed head-on 40px, i.e. 8px above the tile). Gated by `tools/sheet_format_audit.py`. |
| Return ladder destination | Taking the ladder back down landed the player at their SKY coordinates on the surface instead of at their own stairway. | **FIXED** — `4948ed2`. The gate resolved the right destination and then teleported to the entity's dummy one. |
| Paying the Warden | "Wie gibt man dem Warden denn das Geld? Ich sehe keinen Dialog o.Ä." | **FIXED** — `4948ed2`. There was no confirmation step at all: the first interaction silently took 100,000 coins. Now states the cost, then asks, then charges. |
| Lamps and light sources | All lamps/lights should sort into the right workbench category and count as light sources. | **OPEN — needs a specific case.** Checked against vanilla: the mod's lights register as `StreetlampObject`/wall lights with light values and sit in the lights category. Which lamp is in the wrong place is not yet known. |
| White floor places huge | One of the floors (white tile) places far larger than one block. | **OPEN — unconfirmed hypothesis.** Likely Skystone (`SkystoneTile` is a terrain tile, and terrain blends into its neighbours rather than placing a single cell). Not reproduced in game; needs the exact floor name. |
| Skyreach difficulty | Enemies too easy so far. | **PARTIALLY ADDRESSED** — `0ec56dd` + `7062ce4` add the Mistserpent, a 1500 HP worm chain that swims the cloud sea. The existing roster is untouched. |
| Mistserpent look | "Crystaldragon + crystaldragonhead wäre cooler Gegner der in Wolkenmeer rumschwimmt, bitte als Vorlage nehmen und mehr auf Wolken trimmen." | **FIXED — NOT YET PLAYER CONFIRMED**. The first sprite took the sandworm's format AND its construction, and read as a beetle. Rebuilt on the crystal dragon's construction — compact cranium, two large eyes, a radiating frond fan carrying the silhouette — in the Mistsea's blue-white rather than the dragon's pink-violet. |

---

## 2026-08-27 (2) — v0.5.1 · the Warden, played through recruitment

The first session that actually paid the 100,000 and followed him home.

| Area | Observation | Status |
|---|---|---|
| Paying the Warden | "das Geld nimmt er einfach sobald man ihn anspricht, das nicht gut, muss Dialog Option sein ihn zu rekrutieren und nicht Geld einfach weg sein!" | **FIXED — NOT YET PLAYER CONFIRMED**. He is hired through Necesse's own recruit page now: it states the price and `ShopContainer.payForRecruit` takes the coins server-side only on the button press. No coins can move by talking. |
| Warden disappears, must be accepted again | "dann ist er verschwunden und ist jetzt irgendwann später als Bewohner im Dorf sichtbar! aber jetzt muss ich ihn erst annehmen." Expected: hire him up there, he arrives as a resident — the normal vanilla process. | **FIXED — NOT YET PLAYER CONFIRMED**. Root cause: his settler key was never registered, so vanilla's recruit path answered "notsettler" and the old code hand-spawned a SECOND mob at home. There is one Warden mob in a world now; vanilla teleports him to the settlement level and moves him in. |
| Cannot assign a bed / don't know where he is | Follows from the above — he was not a settler until the second recruitment. | **FIXED — NOT YET PLAYER CONFIRMED**. He is a settler the moment the button is pressed. A chat line names the settlement he moved into. |
| Name shows as `mob.wardensettlername` | "sowas darf bei nichts mehr passieren!" | **FIXED — NOT YET PLAYER CONFIRMED** (`0eef9ce`). The audit was checking `mob.<id>`; the engine displays named humans through `mob.<id>name`. The audit now checks that class of key, plus six more it could not see, and fails if a new registration helper appears that it does not know about. It found a second shipped instance in the same sweep: `skyironfencegate` had no name either. |
| Generic villager dialogue | "wieso hat er normale Dialoge" — he greeted the player with "Ich denke oft über die großen Fragen des Lebens nach." | **FIXED — NOT YET PLAYER CONFIRMED**. `HumanMob.getMessages` defaults to `mobmsg.humantalk1..5`; he has his own six lines, and his recruitment pitch moved to the top of the dialogue window where the price is. |
| No quests anywhere in the journal | "und wo sind seine quests? die quests von ihm seh ich auch nirgends im Journal oder sonst wo." | **FIXED — NOT YET PLAYER CONFIRMED**. `FindSpireQuest` was the only quest ever handed out, and meeting him removed it without giving anything back. Registration is not hand-out: `SpireCatsQuest` and `AnchorDeliveryQuest` were fully implemented and never given to anyone. The chain now runs stairway → find the spire → hire him (price in the objective line) → bring both cats home → anchor the island, each turned in by talking to him. |
| Silver Bell | Received. | KEEP — it is handed over at recruitment, which is its only source. |
