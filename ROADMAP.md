# Roadmap — Stairway to Heaven

Every release is a complete, playable, save-compatible increment. Versioning is
`MAJOR.MINOR.PATCH`; content milestones bump MINOR.

**This file was rewritten on 2026-08-30.** It had described the pre-v0.5
direction for four releases, and its milestone names no longer matched what
actually shipped under those version numbers — v0.4.0 and v0.5.0 both released
with different content than the plan below them. What shipped is now in
`CHANGELOG.md`; what is left is here, and nowhere else.

## Released

| version | name | what it added |
|---|---|---|
| 0.1.0 | First Ascent | The Skyreach dimension, the stairway pair, three sub-biomes, the first enemies and materials |
| 0.2.0 | The Warden's Call | The Warden's Spire, the Sky Warden, Siggi and Peanut, the "Nightfell & Skylight" building set, real `_splat` autotiles |
| 0.3.0 | The Veil Below | The Séance Circle, the Veil dimension, Gloomfen and Ashen Reach |
| 0.4.0 | The Living Sky | Per-biome fill, the tree families, more critters and ores |
| 0.4.1 | — | Palette and colour-identity pass |
| 0.5.0 | The Skywatch Opens | The stairway became a portal to one canonical hub; recruitment replaced the fetch chain |
| 0.6.0 | The Working Sky | The Skyway Passages biome, livestock, settlement workstations, five more weapons, the Stormsteel set, the Cat Basket, surface POIs, the icon and world-sprite passes |

Detail for each is in `CHANGELOG.md`. **Nothing in 0.6.0 has been played by a
human yet** — see the verification states in `docs/IMPLEMENTATION_RULES.md` §14.

## Done since this file was last rewritten

- **The bosses landed** (2026-09-03, `docs/FOGKEY_AND_BOSSPORTALS.md`). Five
  realms, one each, at summoning stones scattered through their own band and
  woken by a key piece the Warden pays for and you stand in your settlement:
  57 240 → 127 200 → 157 520 → 161 100 → 208 000 HP. This supersedes the
  "a boss for the Outland portals" entry that stood here.
- **The areas were measured** (2026-09-05). `docs/AREA_OVERVIEW.md` and
  `tools/area_census.py`: cast, spawn density, NPCs, quests and POIs per realm,
  read off the code. It found Eden's guard packs were dead code and Steinfeld
  had no inhabitant; both are fixed.
- **An existing save can be tested from A to Z** (2026-09-05). `/swhreset` and
  `docs/SAVE_COMPAT.md`.

## Next — the ambient life, and Hell

Two holes the census can prove, in order of what they cost the player.

**Only the Skyreach has critters or animals.** Four realms have no ambient life
at all. Steinfeld's answer is already written and merely unbuilt —
`WORLD_DESIGN` §A3.4: *"The ghosts here are mostly not enemies. Some simply
stand. Some walk without purpose. One might walk the same path between two
gravestones forever."*

**Hell is a hole with four buildings in it.** The 0.80–1.00 band paints as
Crooked, and the four Hell POI presets stand in Crooked ground. It is the only
realm with no biome, no cast, no NPC, no quest and no boss rung.

## Then — Chapter 01: the Skyreach has one building

This is the largest open item and it is already designed, not merely wished for:

- `docs/design/chapter-01-skyreach-pois.md` — **14 points of interest** with
  tile-level room plans, object lists with rotations, and one concrete reward
  each.
- `docs/design/chapter-01-skyreach-cast.md` — **three settler types** (a thief,
  a brewer, a scholar), three enemies that belong to a specific place, and eight
  unique rewards.
- `docs/WORLDBUILDING_LOOP.md` and `.claude/commands/chapter.md` — the process
  for building it: designer → POI architect → art in parallel → integrator, with
  a completeness checklist and every gate.

The player's own words after v0.5.1: *"solche Orte und Häuser mit NPCs, unique
Objekten usw"* — the roads and gardens landed, the buildings did not.

## Then — the direction, in priority order

**Endgame pressure.** Shipped power stops at Tungsten and most enemies are easy.
New content sits at Aetherium and above, toward incursion-tier pressure, and its
loot should be a new **ability** rather than a bigger number.

**Outland chapters.** *(Rewritten 2026-08-31. This entry used to read "The
Veil, properly" and describe a second dimension. The player retired that plan —
"das wird zu viel arbeit, wir machen nur sky region" — so the Veil's material
now lives in the Skyreach as the distance-gated Beetle Outlands, and everything
below is a list of themes for FURTHER Outland regions rather than for a
separate layer.)*

Contrast rather than darkness: poison green on violet,
bone white on black, stripes, checkerboard, spirals, sickly pink, brass and
verdigris — black is outline and shadow, never fill. One saturated accent per
set that nothing else has, and one funny piece per chapter. Open: the Model
Town, the Office of Eternity, the zombie quarter on the Ashen Reach with the
Ashwyrm, Mortimer and Vesper, and the "Haunted & Homely" deco set.

**A surface biome of its own.** Something you stumble into mid-to-late, with base
materials and textures found nowhere else, its own inhabitants and its own
trouble. It has to feel *found*, not like a second Skyreach; the Veil seeping
upward is the established hook (`docs/DESIGN.md` Part IV §26).

**The Storm Sovereign.** Summoned at a Stormveil altar, fought over the Mistsea;
trophy and relic drops, and a post-boss tier that bridges into incursion-era
power. Teased by the Warden's finale dialogue and still unbuilt.

## Carried over, still true

- **Weather**: Radiance, Overcast Drift, Tempest, Mist Surge — seeded, announced,
  on the vanilla level-event pattern. The Skyfall event shipped in 0.6.0; the
  rest did not.
- **Mistsea fishing** loot table (`Biome.getFishingLootTable` is verified).
- **Settlement support in the sky**: claiming a flag up there, settler pathing
  around the Mistsea.
- **Sound pass**: wind ambience and mob sounds.
- **Cooking and utility**: Cloudberry Jam, the Cloud Charm, a Windsilk glider —
  the traverse problem the Mistsea creates still has no answer.

## v1.0.0

- Full localization sweep beyond EN/DE
- Steam Workshop packaging, workshop art, trailer GIFs
- Performance audit (region generation profiling, spawn-table load)
- Public modding notes: how to extend the Skyreach from another mod

## Compatibility policy

- Never modify existing vanilla registry entries; additive registration only.
- Keep save compatibility within a MAJOR version; document migrations in
  `CHANGELOG.md`.
- Re-verify against every Necesse patch. `docs/research/` records which game
  version each API finding was checked against.
