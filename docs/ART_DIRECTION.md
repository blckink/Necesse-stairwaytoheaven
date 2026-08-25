# Art direction

Derived from real play sessions. For sprite mechanics — sheet layouts, cell
sizes, palette rules — see `.claude/skills/necesse-pixel-art/SKILL.md` and
`docs/assets-style-guide.md`. This file is about what to aim for.

## The governing principle

**In-game readability beats pixel metrics.** `tools/size_audit.py` catches
sprites that are objectively too small to read. It is a safety net, not a brief.
A sprite can pass the audit and still be wrong, and a screenshot showing it is
wrong settles the question. When a metric and a play session disagree, the play
session wins.

Two failure modes this project keeps hitting:

- **Mass without form.** Adding pixels until the audit passes produces a bigger
  blob, not a better sprite. The Galehound passed at 0.80 of a boar's mass and
  still reads as a grey sausage in motion, because the problem is silhouette:
  no waist, no readable head, legs that do not change pose.
- **Volume flattened into layers.** Stacked bands of colour read as pancakes.
  Volume comes from overlapping masses with dark undersides, bright top-left
  faces, midtones between, and shadow where lobes meet — not from more bands
  and not from smooth gradients.

## Identity

An official-looking Necesse expansion with its own visual identity. Not
"AI-generated pixel art", and not "everything is blue-grey because it is in
the sky". The palette shifts that already distinguish the three sky biomes are
working and are worth protecting.

The world reads well at the small scale: silver-green terrain, white and yellow
vegetation, small flowers, the cloud edge at the world boundary. The problems
are concentrated in **big assets, landmarks, rock geology, and a few
creatures**.

## KEEP — verified good in play

Do not touch these to satisfy a metric.

- Overall Skyreach direction and atmosphere; cloud world edges
- Biome palette shifts
- Mini vegetation as a whole: tulips, grasses, wheat and reed plants, small
  flowers, and their density
- Zephyr Finch (small blue birds), Dewsnail (snails)
- Zephyr Ray — reads well and moves well in game
- Storm Wisp — attack and presentation work
- Aurora plant core sprite (its *placement* is the problem, not the art)
- Tree size and silhouettes — recently improved, do not undo

## REDESIGN — named targets

**Galehound.** A storm predator, not a generic wolf and not a blob. The
silhouette must say "fast hunting animal" at normal zoom: readable head and
muzzle, chest, narrow waist, four distinct legs, storm-trail tail, obvious
facing. Legs and body must visibly change pose across the walk cycle.

**Storm Shards.** Currently a flat white wall of teeth. Wants individually
readable crystal bodies at different heights, widths and angles sharing a
grounded base; darker blue/violet interior faces against pale cyan energetic
edges; an uneven cluster silhouette. Scaling them up does not fix this — the
problem is volume.

**Old Warden Spire.** Must read as the ancient origin of the Skyreach, not a
house. Monumentality through flat Necesse architecture only: a larger
footprint, connected rooms, courtyard and forecourt, paths, floor material
changes, thick architectural zones, archive and observatory sections, beacon
machinery, lamps, banners, statues, asymmetric ruin and weathering. No faked
verticality or perspective tricks — they do not belong in this game. The first
thirty seconds should read: ancient, endgame, mysterious, important, with the
eye led arrival → path and lights → entrance → Warden.

**Cloudberry bush.** Currently reads as two mushrooms. Wants an unmistakable
low berry-bush silhouette with visible branches, leaves and berries.

**Rock shadows.** Far too long and dark — they take more screen than the rock
and make small blocks look like pillars. Match vanilla's convention: a shadow
grounds an object, it does not dominate it.

**Tree canopies (incl. Fulgur Pine).** Silhouette and size are right; volume is
missing. Overlapping canopy masses, dark undersides, bright top-left masses,
intermediate midtones, shadow between lobes, better trunk integration.
Controlled highlight clusters, no gradients.

## Worldgen is art direction too

Placement can ruin good sprites.

**Rock and ore** must form geology, not a grid: irregular groups of roughly
3–8, compact formations, small veins, L-shaped and asymmetric outcrops, a
couple of large ones among smaller ones, big empty gaps between formations, and
only occasional solitary stones. Ore belongs inside and around formations, so
exploration reads vegetation → interesting outcrop → investigate → reward, with
rare rich formations as jackpots.

**Aurora plants** need colony variation: groups of roughly 1–5, irregular
spacing, occasional singles, an occasional richer patch. Identical mirrored
copies at even spacing announce that the world is procedural.

## Rules proven in the v0.6 sprint

**Variant sets must differ in FORM, not pixel jitter.** Two rock sheets that
differ only by speckle salt read as one sprite repeated. Give every variant a
geological character (slab, strata, domes, crack, fissure, rubble, pits,
terrace) painted into every quadrant, plus 1-2px bites carved out of genuinely
exposed edges — never into edges that tile against a neighbour. Because the
engine picks the variant per tile, adjacent tiles of one formation mix
characters automatically and the repetition disappears for free.

**Ground with baked soft-alpha skirts, not opaque dark bands.** Vanilla's
"shadow" under rocks is a semi-transparent dissolve (measured: alpha
195/195/113/78/55/29 over the last 6px, no bottom outline), and its cliff
faces are base-dominant. An opaque deep-ramp band plus a hard outline reads
as a long black shadow and turns small rocks into pillars. Copy the fade.

**Canopy volume = overlap shadows + ONE global light field.** Repeating a
lit ellipse per lobe produces "stacked pancakes": every lobe carries its own
bright circle. The fix is canopy-scale value structure — darken the band
where a lobe sits under a higher one, split the whole canopy into a lit
top-left plane and a deep lower-right plane with a dithered boundary, and
demote per-lobe highlights that fall on the shadow side.

## Review procedure

Before calling an art change done:

1. Regenerate through the generator; confirm byte-identical reproduction and no
   collateral change to sprites sharing helper functions.
2. `python3 tools/size_audit.py` — 0 flags.
3. Composite the sprite at **1×** over the actual ground tile it will stand on,
   not on a neutral background. Most readability failures only show there.
4. For animated sheets, verify frames actually differ — measure per-frame pixel
   deltas rather than trusting the eye.
5. Say plainly which checks were run and which were not.
