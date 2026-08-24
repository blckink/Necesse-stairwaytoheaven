# Current state

Short, current, and rewritten as things change. History belongs in
`CHANGELOG.md` and `docs/PLAYTEST_LOG.md`, not here.

**Version:** 0.5.0 · **Game:** Necesse 1.3.2 · **Branch:** `master`
**Updated:** 2026-08-24, commit `ca2ddad`

## Architecture in one screen

Two extra dimensions registered through
`LevelIdentifier.IDENTIFIER_TO_DIMENSION`: **Skyreach** (`skyreach`, +1) and
**The Veil** (`veil`, −3). Both are `BiomeGeneratorStackLevel`s that stream
regions, so they are effectively infinite and generate lazily.

The **Stairway is a portal**. Wherever it is built on the Surface it routes to
one canonical Skyreach origin computed from the world-generation seed
(`worldgen/SkyOrigin`), where the Old Warden Spire stands. Terrain radiates
from that origin: the hub is clamped to walkable land, and ore density widens
with distance band. The return gate resolves each player's own bound stairway
from `quest/SkywatchQuestData`, which persists per-player bindings.

The **Warden** is the progression NPC. Meeting him completes the find-the-spire
quest; paying 100,000 coins recruits him, which places a real `HumanShop`
settler (`mobs/WardenSettlerMob`, registered as settler type
`settlement/WardenSettler`) on the Surface at the player's stairway and hands
over the Silver Bell.

Assets are **generated, not hand-drawn**: `tools/asset_generator/` writes every
PNG in `src/main/resources/`. Editing a PNG directly is always wrong.

## Green — verified working

- Mod loads on a dedicated server; Skyreach and Veil generate; no log errors.
- World survives a server restart: spire returns at identical coordinates,
  Warden and both cats still present (asserted every test run).
- Warden settler registration resolves (`/skyreachstatus` reports
  `wardensettler=WardenSettler`).
- Siggi and Peanut are unkillable and save-persistent by native means.
- `python3 tools/size_audit.py` reports 0 flags.
- Marble Checker floor no longer crashes clients (`ca2ddad`); `scripts/tile_sprite_check.sh`
  proves it headlessly.

## Known issues — open

Ordered by the player's own priority. Full detail in `docs/PLAYTEST_LOG.md`.

**P1**
- UI/localization: recipes showing internal IDs, Prism sapling missing icon,
  incomplete building/menu entries. Needs a complete registry audit, not
  spot fixes.
- Warden frequently stands facing north, so the player sees his back during
  the introduction.
- Warden's first dialogue dumps too much lore at once.
- Rock/ore worldgen scatters single blocks on an even grid — reads as a
  graveyard. Needs irregular outcrops and clusters.
- Rock shadows are far too long and dark; they dominate the landscape.
- Storm Shards read as a flat white wall of teeth. Volume and silhouette
  problem, not a size problem.
- Galehound reads as a grey sausage in motion. Needs a silhouette and
  animation redesign.
- Old Warden Spire reads as a small ordinary house, not the origin of the
  Skyreach.

**P2**
- Tree canopies are flat, like stacked pancakes (silhouette and size are good
  — do not undo those).
- Cloudberry bush is far too small; reads as mushrooms.
- Aurora plant placement looks mirrored and procedural.
- Harvest tools: much of the flora is pickaxe-harvestable regardless of
  material. Trees correctly need an axe.
- Feature request: snails catchable with the net, native critter pattern.

**Deferred**
- Warden's shop is empty. The building set is fully craftable at a workstation,
  so nothing is missing — but the recruited Warden currently does nothing.
- Cat home / cat bed furniture and settled cat behaviour (post-playtest).
- Three journal quests are registered but never handed out (`swh_beacon`,
  `swh_cats`, `swh_anchor`).
- `ROADMAP.md` still describes the pre-v0.5 direction.

## Last player-tested state

v0.5.0 build, played extensively in a real long-running Windows save on
2026-08-24. That session produced everything in `docs/PLAYTEST_LOG.md` under
that date, including the Marble Checker save-blocker.

## NOT player-verified

Do not describe any of these as working:

- Skystone Golem in game
- the complete Warden settlement lifecycle (recruit → move in → bed →
  happiness)
- cat progression after being brought home
- resource drops across the board
- outer-distance difficulty scaling
- travel/progression end to end
- building materials and custom floors other than Marble Checker
