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
| Rock / ore worldgen | Skystone blocks are evenly scattered singles — reads as rectangular tombstones on a grid. Wants irregular outcrops: groups of ~3–8, compact formations, small veins, L-shaped clusters, large empty gaps, rare solitary stones. Ore should sit inside and around formations so exploration reads vegetation → outcrop → investigate → reward. | REDESIGN |
| Rock shadows | Far too long and dark; they occupy more screen than the rock and make small blocks look like pillars. | OPEN |
| Storm Shards | Read as a little white wall / row of teeth. Flat and repetitive. Wants individually readable crystal bodies at varying heights, widths and angles on a shared base, dark blue/violet interior faces, pale cyan energetic edges. The problem is volume and silhouette, not scale. | REDESIGN |
| Galehound | Reads as a grey sausage in actual gameplay. Needs a genuine silhouette redesign: clear canine head and muzzle, chest, narrow waist, distinct legs, storm trail, obvious facing. Legs and body must visibly change pose while moving. | REDESIGN |

### P2

| Area | Observation | Status |
|---|---|---|
| Tree canopies | Flat, like stacked coloured pancakes. Wants overlapping canopy masses, dark undersides, bright top-left masses, midtones, shadow between lobes, better trunk integration. True pixel art, no smooth gradients. | OPEN |
| Fulgur Pine | Same problem: good concept, horizontal layers too flat. | OPEN |
| Cloudberry bush | Far too small; reads as two mushrooms or stones. Wants an unmistakable low berry-bush silhouette with visible berries. | REDESIGN |
| Aurora placement | Sprite is good; colonies look mirrored and procedural. Wants colonies of ~1–5, irregular spacing, occasional singles, occasional richer patch. | OPEN |
| Harvest tools | Trees correctly need an axe — keep that. Much of the remaining flora is pickaxe-harvestable regardless of material. Needs an object-by-object audit against the nearest vanilla equivalent: tool type, tier, HP, speed, drops. | OPEN |
| UI / localization | Building recipes display internal IDs, some object names show string IDs, Prism sapling has a missing/error icon, some building and menu entries incomplete. Needs a complete registry audit, not spot fixes. | OPEN |
| Snails | Should be catchable with the net using the native critter pattern (butterflies, bees). Players naturally try it. | FEATURE |

### Cats

| Area | Observation | Status |
|---|---|---|
| Siggi and Peanut | Important recurring characters. Must never be permanently killable. Long-term: live with the recruited Warden once the player builds cat-home furniture, roam and rest naturally, eventually a small charming or useful behaviour. Do not rush risky architecture for this. | KEEP (immortality verified) / OPEN (settled behaviour) |

### Not yet player-verified

Skystone Golem · complete Warden settlement lifecycle · Warden bed and
happiness behaviour · complete cat progression · all resource drops · outer
radial difficulty · direct travel progression · all building materials and
floors (Marble Checker blocked testing the others).
