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
| White floor places huge | One of the floors (white tile) places far larger than one block. | **FIXED — NOT YET PLAYER CONFIRMED.** The Skystone hypothesis was wrong and is retracted: every mod floor is a real `isFloor` floor at PRIORITY_FLOOR, and `TileItem.onPlace` always places exactly one tile. The cause was in the `_splat` atlases — see the 2026-08-27 floors section below. |
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

---

## 2026-08-27 — v0.5.0 · the floors

> "fix die ganzen Boden tiles die sich anders verhalten als jeder normale Boden"

The follow-up to "White floor places huge" above: not one floor, all of them.

| Area | Observation | Status |
|---|---|---|
| Floors bleed into their neighbours | Every mod ground tile paints far past the cell it was placed on. | **FIXED — NOT YET PLAYER CONFIRMED.** The four diagonal-only cells of every `_splat` atlas were the complement of their intended shape: a disc of radius 26 parked *inside* the cell, covering 83%-89% of it, where vanilla paints a nub in the named corner covering 0.8%-29.3% (measured over 66 vanilla sheets). `SplattingOptions` draws that cell on every tile that touches ours corner-to-corner, so one placed tile repainted its four diagonal neighbours almost completely and read as a 3x3 blob. Same root cause on all 14 sheets, terrain and liquids included. Fixed in `tools/asset_generator/gen_splats.py`; the three-side and all-four cells also kept their vanilla "eye" of the tile underneath, which they had lost by going solid. |
| Is any floor secretly terrain? | The standing hypothesis was that a floor had been built as a terrain tile. | **NOT REPRODUCED — hypothesis retracted.** All five craftable floors (`marblecheckertile`, `gloomwoodfloortile`, `nimbusfloortile`, `charfloortile`, `prismfloortile`) pass `isFloor=true` and splat at PRIORITY_FLOOR 400, exactly like vanilla `stonefloor`. The six terrain tiles and two liquids are terrain and liquid on purpose — they are the ground the Skyreach and the Veil are generated from. Now asserted by `tools/tile_behaviour_audit.py`, which fails if a floor ever becomes terrain again. |

---

## 2026-08-27 (3) — v0.5.1 · one more from the same base

| Area | Observation | Status |
|---|---|---|
| Wall windows | "Fenster in unserer dunklen Wand sehen eigentlich cool aus aber sind halt doppelt so hoch wie die Wände die sonst in Game sind... Gleiches Problem hatten wir ja bei der Tür auch." | **FIXED — NOT YET PLAYER CONFIRMED**. The player's diagnosis was exactly right, including that it was the same bug: same sheet, one strip left of the doors. `WallWindowObject`'s edge-on variant draws rows 2..7 at `drawY-64 … +16`; vanilla leaves rows 2-4 empty so the window is 48px, the height of its wall. Ours filled all six rows: 96px against 48px, precisely double. `sheet_format_audit` covers the window strip now, not just the doors — the door fix left that half unguarded, which is why this shipped. |

---

## 2026-08-27 (4) — v0.5.1 · nothing is out there

| Area | Observation | Status |
|---|---|---|
| No enemies at all | "kein einziger Gegner irgendwie.. weder Rochen noch Golem.. nur Critter." | **FIXED — NOT YET PLAYER CONFIRMED**. `HostileMob` checks AMBIENT + static light against a threshold of 0, and a non-cave level is at 150 in daylight, so every hostile was rejected everywhere while the sun was up. Five sky hostiles now check placed light only, so the sky is dangerous at noon and a lit base is still safe. The Galehound keeps its dark-only rule, so there is still a night shift. Measured: `accepted lit=4/6 dark=4/6`, with the two refusals being the tiles under candelabra. |
| Never seen a sheep | "Schafe auch noch nirgends gesehen." | **FIXED — NOT YET PLAYER CONFIRMED**. Not a light problem: nothing in `SheepMob -> HusbandryMob -> FriendlyRopableMob -> AttackAnimMob` overrides `isValidSpawnLocation`, so `Mob`'s `return false` stands and no sheep can ever be table-spawned. Vanilla places its livestock from the island generator for the same reason. Flocks are now placed at region generation, deterministic and persistent: `npc check: ... cloudlambs=9`, unchanged after a restart. |
| Snails vanish when netted | "Schnecken verschwinden einfach wenn man sie mit Kescher fängt." | **FIXED — NOT YET PLAYER CONFIRMED**. A net catch drops the mob's loot table and nothing else, and the snail's was a 35% chance of a shard. It now drops a Dew Snail item with certainty, the way vanilla's netted critters drop themselves. |
| Siggi and Peanut unfindable | "Siggi und peanut auch noch nirgends gefunden leider." | **FIXED — NOT YET PLAYER CONFIRMED**. The lairs are fixed at world generation and nothing in game ever pointed at them. Both now get a permanent world-map marker when the cats become an objective, with a new cat map icon. Worlds already recruited by an older build get the quest and the markers the next time they talk to the Warden, since recruitment is where they would otherwise have been handed out. |

---

## 2026-08-27 (5) — v0.5.1 · perspective, and what a place should look like

| Area | Observation | Status |
|---|---|---|
| Window perspective | "Ihr habt die Perspektive immer noch nicht verstanden bei assets! ... das Fenster links am Block zeigt weiterhin nach unten. die Fenster an den Seiten nach oben und unten sitzen sozusagen in der dunklen abgeschnittenen Decke der Wand!" | **FIXED — NOT YET PLAYER CONFIRMED**. The two views of the window strip were swapped. `getWindowDir` returns 1 for a north-south wall, which draws the wall's ROOF (opaque, window drawn onto the cap), and 0 for east-west, which draws its FRONT with a see-through opening. We drew one front-facing pane for both. Yesterday's fix corrected only the height of the east-west variant. Both are now built on vanilla's construction and both rules are asserted by the sheet audit. |
| Roads | "strassen sind gut im skyreach" | KEEP |
| Red region | "rote skyreach Region ist auch cool" | KEEP |
| Grey-floor region empty | "die Welt mit grauen Böden viel leerer und hat eigentlich nur paar einzelne Steinblöcke und sonst keine Inhalte im Vergleich zum Rest" | **FIXED — NOT YET PLAYER CONFIRMED**. Measured, not guessed: over three seeds and 235,528 natural land tiles the grey ground (`skystone`, laid down by `isRockPatch`, 14.7% of all land) carried **0.032 objects per tile in the Driftlands and 0.044 in the Aurora Shoals** against 0.311–0.384 on every vegetated ground — and its entire content was `skystoneRock`. Cause: `rollObject` answers `isRockPatch ? 0 : plant` for nearly every plant, and the meadow-carpet and aurora-colony rules are both gated on `!isRockPatch`, so three separate rules switched all growth off and nothing switched anything on. Now `SkyTerrainPainter.screeObject` gives the barrens their own formation field (lichen beds on a lattice, same shape as the aurora colonies) with three new objects — Skystone Lichen, Cragbloom, Sky Scree — plus boulders for vertical relief and one lit biome accent. Re-measured on the same 235k tiles: **0.304 / 0.352 / 0.356**. |
| Fences | "Zäune sind leider perspektivisch noch schrecklich und z.t. nicht sinnvoll aufgebaut in Levels" | **FIXED — NOT YET PLAYER CONFIRMED**. The player diagnosed it exactly right, and it was the same class of error as the wall windows: our fence sheet was drawn to an invented column layout ("post / horizontal run / top cap / left / right") while `FenceObject.addDrawables` addresses those five columns as post / north joint / south rail / WEST run / EAST run. So the engine drew a full-width horizontal rail whenever a fence connected NORTH, a 3px hairline for every vertical run, and the west and east runs on each other's side of the tile. On top of that 79% of the sprite's pixels were literally the outline colour, so there was no lit top surface and no dark front face — no perspective at all. Both sheets are rebuilt cell by cell against vanilla `ironfence.png` / `ironfencegate.png`. Second half of the report: fence rings were rasterised as one-tile-thick annuli, which step diagonally, and `FenceObject` attaches only orthogonally — measured **3.9% lone posts and 28.0% dead ends** across 4,111 placed fence tiles. Now **0.2% and 6.0%** across 5,562, and a road crossing a ring gets a real fence gate instead of a gap. |
| Missing icons | "düsterpilz immer noch ERR Thumbnail, flüsterried auch .. das sind echt basics" | **FIXED — NOT YET PLAYER CONFIRMED** — `0b4dc33` drew the four items that were rendering as the engine's ERR tile, `items/gloomshroom.png` and `items/whisperreeds.png` among them. This row stayed OPEN after the fix landed; corrected on 2026-08-29 after verifying both files exist, both read at healthy mass (`whisperreeds` 483 opaque px, `gloomshroom` 429, against a vanilla item-icon median of 440) and `locale_audit` resolves 149 holdable IDs to a real icon file. |
| Cat vanished after coming home | "hab Siggi jetzt gefunden und Snack gegeben aber danach nie wieder gesehen" | OPEN |
| Warden quests still invisible | "warden gibt weiterhin keine quests die ich finden kann" | OPEN — a fix landed the same day; the player may not have that build. Needs confirming before anything else is changed. |
| Cloud Lambs have no purpose | "Wolkenschafe konnte ich fangen aber was bringen sie jetzt? und es gibt halt schon normale schafe. was muss in Trog bei wolkenschafen?" | OPEN |
| **POIs** | Three reference screenshots of vanilla structures: a walled garden courtyard around a fountain, a dark shrine with statues on pedestals around a glowing centrepiece, and a large multi-room crystal complex with storage and machinery. "solche Orte und Häuser mit NPCs, unique Objekten usw" | OPEN — the roads and gardens landed, but the Skyreach still has exactly one building. This is the next large piece: real presets with interiors, unique objects and inhabitants. |

---

## 2026-08-27 (5) — v0.5.1 · three "what is this for?" reports

All three are the same shape: the mechanic exists and works, and the player
cannot see the point of it.

| Area | Observation | Status |
|---|---|---|
| The cat vanished after being brought home | "hab Siggi jetzt gefunden und Snack gegeben aber danach nie wieder gesehen." | **FIXED — NOT YET PLAYER CONFIRMED**. The cat was never lost: `/skyreachstatus cats` now drives the real travel-home path, and after a full server restart both cats come back at the basket tile with their homesick tether rebuilt around it (`at=127,154 d=0 tether=127,154 AT_BASKET`), and are still within ~7 tiles of it after 25 s of AI. What was missing was everything the player could *see*. `WardenSpirePreset` reserved local (5,6) as the basket tile and **placed nothing on it**, so "home" was a bare floor square; the chat line said "homeward" and named no place; the journal line still read "roams the Stormveil" after the cat had moved in. Now: `SkyLevel` places the real `catbasket` object on that tile once per world (existing saves included), the treat line names the spire with a distance and bearing and re-pins the spire on the world map, the journal flips to "Siggi is home - asleep in the basket at the Warden's Spire", and talking to a settled cat gets a purr instead of "Mrrp?". |
| The Warden's quests are still invisible | "warden gibt weiterhin keine quests die ich finden kann" | **FIXED — NOT YET PLAYER CONFIRMED**. The 50f4480/fe88343 hand-out was real but unreachable in the ordinary case: the catch-up ran only `if (levelManager.isLoaded(skyreach))`, and the server unloads a level nobody is standing on after `unloadLevelsCooldown`. A player who came down from the sky, played on the surface for a minute and walked over to the Warden hit `sky == null` and got nothing — no cats quest, no lair markers, no anchor chapter. Plus three save states with no branch at all (met him under an older build; both cats already home before the quest existed; past the cats with no anchor quest held). The chain is now a pure function of the world record (`SkyWardenMob.chapterFor`) and every reachable state is asserted to be owed a chapter. |
| Cloud Lambs have no purpose | "Wolkenschafe konnte ich fangen aber was bringen sie jetzt? und es gibt halt schon normale schafe. was muss in Trog bei wolkenschafen?" | **FIXED — NOT YET PLAYER CONFIRMED**. They sheared for *vanilla wool*, bred a 50% chance of a *vanilla ram*, called their young "Lamb", and the only thing their trough would accept was surface wheat. Now: shearing yields **Windsilk** (the mod's fibre — Galehowl, Seance Circle, Sky Balloon and the cats' Cloudpuff Treats), they breed true, they are Cloudlambs at every age, and **cloudberries are animal feed** so a trough in the sky can be filled from a Skyreach bush. The animal states both facts in its own hover tooltip. |

---

## 2026-08-28 — Beetlefreak wall, seen in game

| Area | Observation | Status |
|---|---|---|
| Beetlefreak wall | "die Wandtexturen sind komplett für'n arsch von der Beetle wall, da stimmt kein Rand, Fenster oder sonst was von Layout" | **FIXED — NOT YET PLAYER CONFIRMED**. The player is describing three separate faults and all three were real. (1) The 4x8 body block was one continuous illustration — swirls, striped bands and an arch running straight across the 16px cell edges — but the engine reads that block as an auto-tile blob whose columns are tile HALVES, and the column-to-half mapping is not even constant down the sheet: in rows 1 and 2 columns 1 and 2 swap sides against rows 0, 3 and 4. No cell could meet its neighbour, which is exactly "kein Rand stimmt". (2) The eight 32x128 door cells held lamp posts and partial arches instead of eight door frames, and cells 5 and 9 were full-width where vanilla draws a narrow slab against one jamb. (3) The window's two views were swapped again: a front-facing pane with magenta glass sat in rows 0-1, which `getWindowDir == 1` draws as the wall's ROOF seen from above, while the east-west front carried a hanging banner with no opening in it. `tools/sheet_format_audit.py` passed on the whole thing, because it guards cell geometry — which cell, what size, what extent — and cannot see whether the art inside a cell joins. The sheet is now redrawn by `tools/asset_generator/gen_beetlewall.py` on the layout the renderer actually reads, keeping the supplied art's identity (violet stone with swirls, cream-and-black bead trim, brass lanterns with a green flame, the arch, magenta glass, the skull over the door), and `tools/wall_render_preview.py` composes real scenes — solid block, L, T, free-standing tile, doors and windows in both orientations — so that "does it tile" is now a picture. The port was checked against vanilla `stonewall` first. |

---

## 2026-08-28 (2) — Beetlefreak wall, two faults the gates called clean

Both reported after the rebuild above shipped. Neither is a geometry fault —
the sheet's bounding boxes were byte-identical to vanilla `stonewall`'s — so
`sheet_format_audit.py` was green on both, and `wall_render_preview.py` drew
them without complaint because it only ever composed our own sheet.

| Area | Observation | Status |
|---|---|---|
| Beetlefreak doors | "die Türen wirken viel zu kurz" | **FIXED — NOT YET PLAYER CONFIRMED**. The silhouettes were vanilla's exactly; the composition inside them was not. Vanilla runs ONE door leaf the full height of its cell and puts every piece of ornament on the CROWN above sheet row 96 (row 96 = the tile's top edge, because `WallDoorObject` draws at `.pos(drawX, drawY - 96)`): `woodwall`'s edge-on leaf is unbroken from y70 to its threshold at y124 under a 12px pediment, `stonewall`'s from y70 to y123 under an 8px arch cap. Ours banded every leaf with a cream bead rail across its middle, and in the edge-on cells 5 and 9 — the doors in every left and right wall — put a lantern-topped stub of wall masonry above the tile edge, a full-width cream band at row 96, and a 3px sliver of leaf in a slab of roof pixels below. Three unrelated things stacked, the door the smallest of them. Now: one leaf per cell running the whole height, a brass frame around it, one green boss, the skull and the bead on the crown, and the leaf a ramp step lighter above row 96 than below (which is what `stonewall` does — the same leaf is (115,130,151) above the tile edge and (60,65,74) below). The head-on leaf also went from 16px wide between 8px jambs to 22px, against `stonewall`'s 20px opening and `woodwall`'s full-width leaf. |
| Beetlefreak side-wall window | "Fenster an der Seite zeigen nie in Richtung Süden ... das wäre ja mitten in der Wand, nach oben ausgerichtet" — and confirmed a front-style window IS right on the top and bottom walls | **FIXED — NOT YET PLAYER CONFIRMED**. Rows 0-1 of cols 4-5 are `getWindowDir == 1`: a NORTH-SOUTH wall, i.e. the left and right walls of a room, drawn over the band drawY-16..drawY+16, which in a vertical run is unbroken roof. Every vanilla wall draws that as the wall's TOP SURFACE with a slot cut ALONG it that you look down into — 10-12px wide, running almost the full 32px of the cell, dark reveal on the near faces, lit lip on the far one, glass at the bottom of the cut, and no horizontal terminator at either end. Ours had a brass-framed pane with a two-by-two lattice standing upright in the middle of the cell. The previous pass already knew the cell was the roof and tried to fix it by darkening the pane; darkening a front-facing pane does not make it lie down, only the slot shape does. Rows 5-7 (the east-west case) keep the front-facing arched pane, which the player confirmed is correct there. |

**The gate changed too, and that is the point.** A scene rendered from our sheet
alone cannot answer "is this shorter than vanilla" or "is this the wrong view";
both are judgements against vanilla and nobody holds vanilla in their head
across a session. `tools/wall_render_preview.py` now draws every scene for our
sheet AND for vanilla `stonewall` and `woodwall` directly beneath it, same
scene, same scale, same backdrop, with scenes chosen to reach every branch of
the port: windows in the left/right walls and in the top/bottom walls as
separate scenes, closed and open doors in all four rotations, a solid block, an
L corner and free-standing runs. Both faults are unmistakable with the vanilla
strip underneath and invisible without it.

---

## 2026-08-28 (3) — Cat Baskets placed in town, and nothing happened

| Area | Observation | Status |
|---|---|---|
| Cat Basket / where the cats live | "Katzenbetten sollen in normalem Haus platziert werden können etc in der Stadt damit die Katzen dort wohnen. ich habe beide gerade platziert und die sind weg oder irgendwo anders dann erschienen wo ich es nicht weiss 🥺" | **FIXED — NOT YET PLAYER CONFIRMED**. The player was right twice over. The Cat Basket was a bare `FurnitureObject` with `furnitureType = "petbed"` and nothing else: no object entity, no placement hook, no line of code anywhere connecting it to Siggi and Peanut. Placing one was decoration. The cats' home was `SkywatchQuestData.basketX/basketY` — the basket tile inside the Warden's Spire, in the **Skyreach** — and `sendHome` moved them there with `setPos`, which cannot cross a dimension even if the record had said to. So the cats were never lost: they were exactly where the mod had always sent them, one dimension up, and nothing in the game said so. Now a placed basket IS the home, wherever it stands, the coaxed cats travel to it (`TeleportEvent`, the vanilla cross-level mechanism), the newest basket wins, breaking the active one sends them back to the spire, and every one of those says so in chat in both languages. The record lives in `SkywatchWorldData` (a `WorldData`), because a home standing in a Surface town is not a fact about the Skyreach and must survive that level being unloaded or regenerated. |

Measured end to end by `scripts/integration_test.sh`, which now places real
baskets through the player's own placement path and watches the cats move:

```
cat basket place: step=first  at=surface:-10,1062 object=catbasket recordedHome=surface:-10,1062 surfacecats=2 skyreach2cats=0
cat basket place: step=second at=surface:-10,1063 object=catbasket recordedHome=surface:-10,1063 surfacecats=2 skyreach2cats=0
cat basket place: step=brokeold    at=surface:-10,1062 object=air recordedHome=surface:-10,1063 surfacecats=2 skyreach2cats=0
cat basket place: step=brokeactive at=surface:-10,1063 object=air recordedHome=NONE              surfacecats=0 skyreach2cats=2
cat basket place: step=final  at=surface:-10,1064 object=catbasket recordedHome=surface:-10,1064 surfacecats=2 skyreach2cats=0
```

and after a full server restart, both cats are still living in it:

```
cat home check: basket=-196,-314 object=catbasket homeFlags black=true tabby=true worldFlags black=true tabby=true
  home=surface:-10,1064 homeObject=catbasket homeSource=placed
  | spirecattabby on=surface at=-10,1064 d=0 tether=-10,1064 AT_BASKET
  | spirecatblack on=surface at=-10,1064 d=0 tether=-10,1064 AT_BASKET
```

**Nobody has placed one in a real client yet.**

---

## 2026-08-28 (3) — "lässt sich nicht ausrichten"

> "die durch Claude hinzugefügten skyreach Türen und Tore etc lassen sich alle
> nicht ausrichten wie sonst im Game … eigentlich je nach Richtung in die man
> schaut sind Sachen oft am unteren oder oberen Ende des Blocks platziert am
> Rand statt einfach immer an selber Position."

Asked which of the two failure shapes it was, the player picked **"dreht sich
gar nicht"** — nothing changes when they turn it — and **"weiß ich nicht
genau"** for which object, having seen it once rather than swept the set. So
the response is a sweep plus a gate, not a single-object patch.

| Area | Observation | Status |
|---|---|---|
| Skywatch Banner shows one picture on every wall | The reported shape, found by measurement. | **FIXED — NOT YET PLAYER CONFIRMED.** `gen_banner_painting` pasted ONE 32x32 cell into all four `PaintingObject` rotation rows, so a banner on a north wall, a south wall and both side walls drew byte-identical art. Now four real views: face-on for the wall above (the old art, unchanged), a foreshortened over-the-cap view for the wall below, and mirrored edge-on slabs for the two side walls. The engine's own `+8px` / `-32px` nudges are left to the engine — see `docs/TECHNICAL_LEARNINGS.md`. The item icon now crops row 2, the face-on view, instead of row 0. |
| Everything else the engine reads per rotation | Swept, since the player could not name the object. | **NOT REPRODUCED.** All eight door cells of all four wall sheets, both fence gates' six columns, both fences' five columns, both wall lights' state x orientation grid, both streetlamps' on/off rows, the candelabra's lit/unlit pair and every four-column furniture and station sheet hold distinct art. 123 comparisons, one failure, and it was the banner. Doors and gates in particular are vanilla `WallDoorObject`/`FenceGateObject` on distinct cells, so if turning one still does nothing in game the cause is not the sheet and the next step is a screenshot of the piece mid-placement. |
| The gate that was missing | `sheet_format_audit.py` was green on the banner and always would have been. | **ADDED.** `tools/rotation_variety_audit.py` fails when a cell the engine reads separately holds a picture it already read somewhere else; `tools/rotation_preview.py` draws every cell where the engine puts it, over a tile grid, with the wall the rotation names beside it, into `build/qa/rotations/`. Both are in the AGENTS.md gate list. Verified: the audit reports all six banner row pairs against the pre-fix sheet. |
| The 1x2 furniture (bench, bed, dinner table) | Not swept, on purpose. | **OPEN — needs the decompile.** Their sheet size is recorded, the engine read that splits it is not, and their generators paste 64px blocks across two 32px columns. `skywatchdinnertable` has two identical columns under a 4-column reading, which is a hypothesis about a frame nobody has read. Read `DinnerTableObject`/`BenchObject`/`BedObject`, then decide. |

---

## 2026-08-29 — the Warden's Spire, seen from inside

> "Turm ist leider viel zu hell und sieht schrecklich aus. die ganzen Wände
> blenden fast und passen nicht zu braunen Böden und grauen Möbeln. die Fenster
> sind seitlich falsch und nicht wie bei Käferwand gefixt, du hast zu viele
> Zäune in Welt die keinen Sinn ergeben leider.. bitte hier muss irgendwie ein
> bisschen aufgeräumt werden und farblich stimmiges Konzept geschaffen werden
> mit Atmosphäre"

Three reports, all three real, and the first two had one root cause between
them: `objects/cloudmarblewall.png` was not generated at all. It was the
supplied illustration from `kk-sprites/`, copied in as-is.

| Area | Observation | Status |
|---|---|---|
| The tower is blinding and does not sit with the room | "die ganzen Wände blenden fast und passen nicht zu braunen Böden und grauen Möbeln" | **FIXED — NOT YET PLAYER CONFIRMED.** Measured, not guessed: the shipped sheet carried **10,858 distinct colours** against 19 / 19 / 38 for the three drawn walls, and its cap band — the band the engine draws for every tile of a run but the last — had **no dominant tone at all** (commonest: pure white, 6%) at mean luminance **228**, against skystonebrick 52, nightfell 25, Beetlefreak 31. The face averaged 196 where Skywatch furniture averages 107–114. `gen_cloudmarble` has drawn the whole sheet correctly since it was written; the pipeline just never called it. It does now (the same call the Beetlefreak wall already makes, for the same reason), and the ramp was retuned to a stated value law: cap ~85, face ~160, and **the gold above the stone** — at the old stone base of 225 against SKYGOLD's 178 the gold trim was 47 steps *darker* than the marble it decorates, which is most of why the set read flat as well as bright. Now 22 colours, cap 79% one dominant tone. `docs/ART_DIRECTION.md` carries the value law. |
| Side windows still wrong | "die Fenster sind seitlich falsch und nicht wie bei Käferwand gefixt" | **FIXED — NOT YET PLAYER CONFIRMED.** Exactly right, and broader than reported: **three** wall sets had it, not one. `gen_beetlewall` learned that rows 0-1 of the window strip are the wall's ROOF with a slot cut ALONG it, and the fix was never ported — cloudmarble drew a gold-framed 2×2-mullion pane, skystonebrick and nightfell an 18×22 frame with glazing bars, all three standing upright out of the roof. The construction is now one module, `tools/asset_generator/wall_window_slot.py`, used by all four sets, so they cannot drift apart again. A frame seen from above is still a frame; only the slot's shape and its reveals read as an opening. |
| Cloudmarble door rotations | Not reported — found by the gate. | **FIXED.** Switching to the drawn sheet exposed that its "open, edge-on" branch drew rotations 0 and 2 identically: two door views, one picture, so half the door's rotations did nothing. `tools/rotation_variety_audit.py` caught it the first time it ran on this sheet, which is what it exists for. rot 2 is now the mirror, the way the closed edge-on cells already mirrored rot 3. |
| Too many fences | "du hast zu viele Zäune in Welt die keinen Sinn ergeben" | **OPEN — diagnosed, deliberately not changed blind.** The sources are `SkyLandscape`: fenced roadside beds at waypoints where the kind roll lands above `WAYPOINT_MILESTONE` (0.74), i.e. ~26% of waypoints at `WAYPOINT_SPACING` 14 — roughly one fenced bed every 54 tiles of road — plus the passage balustrade fallback and the forecourt ring at radius 13, which roads cut into short stubs. The obvious knob is the bed's share of the waypoint roll. It is NOT being turned here: the last fence pass was justified by measurements over three seeds through the offline painter (`scripts/sky_map_render.sh`, 4,111 → 5,562 fence tiles, 3.9% → 0.2% lone posts), that painter needs `NECESSE_GAME_DIR`, and this session has no game install. Tuning worldgen density by eye and calling it fixed is how the count went up last time. Next session with the game: render the same three seeds, count fence tiles per road tile, then turn the knob once. |

---

## 2026-08-29 (2) — bushes, rays, golems, and "es fehlt alles"

| Area | Observation | Status |
|---|---|---|
| Settlers have professions | "die haben sehr wohl Berufe! es gibt Bauern, Magier (mit Store), Erforscher (kann Expeditionen machen), Angler / Fischer (kann als einziger angeln von npcs)" | **CORRECTION ACCEPTED — the earlier answer was wrong.** Necesse has settler TYPES through `SettlerRegistry`, and this mod already registers one (`WardenSettler`). What the previous note described was the work PRIORITY a workstation job files under, which is a different mechanism; conflating them made "there are no professions" read as "professions do not exist". They do, the mod has the working pattern, and the Thief / Brewer / Scientist are being designed as real settler types. |
| Too many rays, too few golems | "zu viele rochen, zu wenig Golems" | **FIXED — NOT YET PLAYER CONFIRMED.** Read straight off the spawn tables and the report is exact. The Galehound is darkness-only and the Mistserpent is `IN_MISTSEA`, so the Driftlands' DAYTIME LAND roster was the Zephyr Ray at weight 80 out of 80 — **100% of what a daylight player meets in the mod's common biome** — while the Skystone Golem was absent from it entirely (0 here against 25 Stormveil, 55 Aurora, 55 Skyway). The rarest biomes had the bruiser and the one everybody walks through had the flier. Now ray 40 / golem 40, i.e. 50/50, and the ray's local cap drops 3 → 2 because the cap is what a player feels. The golem is placed on the bare skystone scree that already covers 14.7% of Driftlands land. |
| Berry bushes: one berry, no regrowth | "man kriegt nur eine Beere beim Abbauen statt wie bei den Vanilla Büschen die Büsche abbauen kann und wieder aufbauen damit die Beeren nachwachsen" | **PARTIALLY FIXED — the real fix needs the game install.** Root cause found and it is architectural: **the cloudberry bush is not a bush.** It is a `GrassObject` — the trampled-grass archetype — so it is one-shot and gone. Vanilla berry bushes are the `FruitBushObject` family placed by `.placeObjectFruitGrower(...)`, dropping berries **plus a sapling** (`StabbyBushMob`: `blueberry x2 + blueberrysapling`), and the sapling is the growth gate that makes replanting a loop instead of an exploit. Done now: the yield goes 1–2 → 2–4 so the animal-feed loop is bearable. NOT done: the archetype swap and a `cloudberrysapling`, because `FruitBushObject`'s and `SaplingObject`'s constructors cannot be read without the decompiled sources and nothing can be compiled in this session. Written down in full at `SkyObjects.cloudberryLoot`. |
| Bushes far too small | "die Büsche sind auch viel zu klein leider" | **FIXED — NOT YET PLAYER CONFIRMED.** Measured: 31×23 px and 520 opaque in a 32×32 cell — grass-clump mass on a plant the player has to find and harvest to feed animals. Now 32×30 and ~800 opaque, crown raised from row 9 to row 4, with 3–4 berries per cluster instead of 2–3 so the berries still read at 1×. A bush genuinely LARGER than one tile is not reachable from here: `GrassObject` sheets are N×32 wide and 32 tall, so the cell is the ceiling — two tiles of bush is the same `FruitBushObject` swap as above. |
| No POIs, NPCs, special places | "es fehlen weiterhin jegliche POIs, NPCs, besondere Plätze, Häuser etc. es gibt nur die 2-3 POIs die aber nie besonderen Loot haben oder neue Gegner oder irgendwas interessantes" | **IN PROGRESS.** The design phase of `docs/WORLDBUILDING_LOOP.md` is running: one agent on ≥10 Skyreach POIs with room plans and a stated reward each, one on the cast — the three requested settler types, place-bound enemies, and the unique loot the existing POIs are missing. Output lands in `docs/design/`. |

---

## 2026-08-29 (3) — the master-branch mobs, and a window that should never have had a recipe

Reported from the build on `master`. Every line below was re-measured on this
branch before being written down; the player is right on all of them, and on two
counts the measurement is worse than the report.

| Area | Observation | Status |
|---|---|---|
| Windows are separately craftable | "Beetle window unnötig als extra craftbar, bei anderen Wänden setzte man normale Fenster ein und sie passen vom Design sich an. also Sprite ok aber Integration falsch" | **CONFIRMED — not yet fixed.** And it is all four, not just Beetlefreak: `skystonebrickwindow`, `beetlewindow`, `nightfellwindow` and `cloudmarblewindow` each carry their own `WORKSTATION` recipe. The repo already recorded the divergence and kept it — `TECHNICAL_LEARNINGS.md`: *"vanilla leaves its window unnamed because vanilla windows are not separately craftable. Ours are, so ours are named."* Vanilla ships item icons for exactly three obtainable wall pieces (wall, door, locked door); the window is not one of them. Decide deliberately: match vanilla (drop the four recipes, drop the four item icons, let the window come from the wall) or keep the divergence and write down why. Right now it is neither. |
| Cloudlamb reads as a recoloured sheep | "heftig wenig Details und eigentlich gleiches Tier nur mit anderer Wolle... sowas Kirby-ähnliches wär cooler" | **CONFIRMED.** Densest 64px frame is **38×27 at 734 opaque px in 10 colours**. For comparison the mod's own size law puts a *grass clump* at 500+ px. Ten colours across a whole mob sheet is the "wenig Details" quantified. The design note matters more than the pixel count though: a sky sheep that is a sheep with different wool has no reason to exist. It needs its own silhouette. |
| Galehound too few details | | **CONFIRMED.** 62×46, 1205 px, **18 colours** — the best of the critters measured, and still thin for a pack hunter that is meant to be the night threat. |
| Glowmoth does not glow, is not netable, reads as two mini clouds | | **CONFIRMED, all three.** `SkyCritterMob`'s constructor sets **no `lightLevel` at all**, so nothing makes it glow. `/skyreachstatus` reports `net dewsnail=NETABLE` and nothing else — **the Dew Snail is the only netable critter in the mod**. And the sprite is 59×47 in **8 colours**, which is how a moth ends up reading as two blobs. |
| Mistserpent never seen | "sollte mehr aussehen wie Fuchur bzw ein Kristalldrache (gibt's Vanilla in Incursions)" | **EXPLAINED.** It is weighted 28 and gated `IN_MISTSEA`, so it only ever spawns in open cloud sea — a player walking islands will not meet it. The construction note is already in `TECHNICAL_LEARNINGS.md` (the crystal dragon's compact cranium + radiating blade fan); the sprite followed it once and has never been seen in play to confirm. |
| Skystone Golem far too small | "es gibt bei Vanilla auch Golem das genutzt werden könnte bzgl Größe und Details" | **CONFIRMED.** Densest frame **43×50**. A player's bare head is 28px wide and the torso 20px, so our "armoured bruiser" is barely wider than the player it is meant to threaten. Vanilla has golems to measure against — do that first, then rebuild. |
| Sparkbeetle ok but not fancy | | **CONFIRMED as thin.** 48×46 at **484 opaque px in 8 colours** — the lowest mass of any mob measured. |
| Both cats too small, tail missing, eyes unreadable | | **CONFIRMED, and worse than reported.** Densest 32px cell is **24×15 at ~190 opaque px in 8–9 colours** — under half the mass the mod's own size law demands of a *grass clump*. At 15 rows tall there is no room for a readable eye, and a tail cannot survive the outline pass. These are story characters the player is sent to find; they are the smallest sprites in the mod. |

**The pattern across the whole table** is one number: **8–18 colours per mob
sheet**, where the style guide asks for 3–6 readable micro-details in every 32px
cell. The mobs were built to the *format* correctly and to the *density* not at
all — the same class of miss the walls had, one category over.

---

## 2026-08-29 (4) — item icons and world sprites, measured rather than reported

Not a play session: a measurement pass, prompted by re-reading the still-open
"Missing icons" row above. The icon FILES were all there since `0b4dc33`. What
nobody had measured was whether they read.

| Area | Observation | Status |
|---|---|---|
| `size_audit.py` passed by measuring nothing | Its `--vanilla` default was a dev-container path that does not exist on any other machine, so **0 of 122 rows compared** and it still printed "0 sprite(s) flagged" and exited 0. `docs/CURRENT_STATE.md` quotes that green tick as verified. | **FIXED**. Defaults to this checkout's own `vanilla-sprites/`, prints how many rows actually compared, and a run that measures nothing now exits 1. |
| 207 of 307 shipped PNGs had no audit row | `PAIRS` is hand-maintained, so a sprite with no entry is never measured. Among the uncovered: 94 item icons, **47 of them below the thinnest vanilla item icon in the dump**. | **FIXED for this batch** — 14 rows added. The remaining ~35 thin icons are listed in `docs/CURRENT_STATE.md` as the follow-up. |
| Twelve icons far below vanilla mass | Vanilla 32x32 item icons carry 288–712 opaque px (median 440). We shipped `flickerlightgarland` at 29, **`tempestedge` — one of the mod's two original weapons — at 45**, `veilessence` 70, `ghostlantern` 77, `wardencandelabra` 78, `stormshard` 85. `docs/REVIEW-2026-08-24.md` listed widening the Tempest Edge blade as art action #1 four months ago. | **FIXED — NOT YET PLAYER CONFIRMED**. All twelve redrawn in the generator against a named vanilla analogue each; every one now 310–655 px. |
| Held weapon sprites are a third the size of every other weapon's | `player/weapons/tempestedge.png` and `galehowl.png` sit on a 32x32 canvas while `skyreave` is 96x95 against vanilla `quartzglaive`'s 104x88 and `thunderhead` 22x62 against `tungstengreatbow`'s 20x60. | **OPEN** — found, not fixed. Changing the canvas is rendering geometry, not art, and wants its own verified pass. |

The three-way split that produced this: I measured and set the acceptance
numbers, Codex redrew in the generator, I reviewed on contact sheets against the
vanilla analogue and sent one correction pass back. What the numbers passed and
the picture caught: the redrawn `aurorapetal` was a fuller five-petal flower than
`aurorabloom` itself, which sits beside it in the inventory. Corrected to a
single detached petal.

**Nobody has opened an inventory in the real client and looked at any of this.**

## 2026-08-31 — direction feedback on the Skyreach (no play session)

Not a play report: a direction call plus supplied art, in chat.

| Area | Observation | Status |
|---|---|---|
| The sky is too bright | *"das gebiet ist einfach zu weiß und hell und wir brauchen kontrast"* | **ADDRESSED — NOT PLAYER CONFIRMED.** The Beetle Outlands: wrong regions cut into the Skyreach, impossible within 900 tiles of the spire and 25%+ of land past 3200. Measured, see `docs/CURRENT_STATE.md`. |
| Two dimensions is too much work | *"aktuell ist veil ja als dunkles 2. gebiet gedacht ... aber das wird zu viel arbeit, wir machen nur sky region ... auf eine welt eindampfen statt skyreach und veil"* | **ADOPTED.** The Veil's material moved into the sky; the dimension stays registered so existing saves survive, but takes no new content. |
| Seance Circle should be a boss portal | *"seance zirkel würde ich stattdessen als 'boss-portal' nehmen ... an bestimmten stellen, nicht random"* | **HALF DONE.** Circles now stand at fixed hashed sites inside Outlands. The summon is NOT wired — there is no boss. In the sky the circle says so rather than opening a rift. |
| Crazy areas should start away from the tower | *"man spawnt ja immer am turm und da ist alles schön und hell ... und dann muss es diese verrückten gebiete geben sobald man den turm verlässt und ca 1000m davon entfernt ist"* | **DONE** at a 900-tile hard floor. |
| Beetle world first | *"das erste ist beetle world (basierend auf beetlejuice)"* | **STARTED.** Ground, props, mobs and the Crooked House are in. It is still the Veil's furniture in a new place — no Outland-specific cast or loot yet. |
| Supplied art: evil wall | 128x208 sheet + 32x32 icon, drawn on vanilla `crystalwall` | **INTEGRATED** as `evilwall`, a vanilla `RockObject`. See TECHNICAL_LEARNINGS for why it is a rock and not a wall. |
| The mistsea splat ground | *"der bestehende splat boden ist total buggy und hässlich, den kannst du aber nutzen und ich verbessere ihn jetzt"* | OPEN — player is reworking it. Stays in `tile_behaviour_audit.KNOWN_UNFIXED` until then. |
| Cloud areas enclosed, crazy areas outside | *"die wolkengebiete sind halt im idealfall immer geschlossen und außerhalb der wolkengebiete gibt es diese verrückten gebiete"* | **PARTIAL.** Outlands are cut out of the biome field as their own regions, but the cloud biomes are not yet *enclosed* shapes. Open. |
| A garden of eden of the serpent | *"himmel hat schon viele wolken böden usw aber eben auch einen garten eden der schlange"* | OPEN — named, not designed. Needs a chapter brief. |
