# Contributing

Thanks for helping build the Skyreach! This page gets you productive fast.

## Setup

1. **Get the game bits.** Any of:
   - a Steam install of Necesse (auto-detected), or
   - the free [dedicated server](https://necessegame.com/server/) — enough to compile
     AND to run the integration test. Point the build at it:
     ```bash
     export NECESSE_GAME_DIR=/path/to/necesse-server   # contains Server.jar (or Necesse.jar)
     ```
2. **JDK 17+** (the build emits Java 8 bytecode itself).
3. Build: `./gradlew buildModJar` → `build/jar/…jar`.
4. Optional IDE niceties: run `./gradlew decompileToSources` and attach the produced
   `Necesse-sources.jar` (never commit or redistribute decompiled sources).

## Before you open a PR

- `./gradlew buildModJar` must pass.
- `scripts/integration_test.sh` must pass (needs `NECESSE_GAME_DIR` with `Server.jar`).
- If you touched art: regenerate via `python3 tools/asset_generator/generate_assets.py`
  (needs `pip install pillow`) and eyeball the changed sheets at 4× (see
  `docs/assets-style-guide.md`). Generated PNGs are committed; the generator is the
  source of truth.
- Update `CHANGELOG.md` under an `Unreleased` heading, and `ROADMAP.md` if you completed
  a milestone item.

## Conventions

- **Scope discipline:** additive registrations only — never modify vanilla registry
  entries, never patch bytecode. Compatibility is a feature.
- **IDs:** lowercase, no separators, prefixed by theme not by mod
  (`skystone`, `stormwisp`); tile IDs end in `tile` (vanilla convention, avoids item ID
  collisions — e.g. `skystonetile` the tile vs `skystone` the material).
- **New content goes through `SkyRegistry`**: register in the mod entry, store IDs in
  the facade, reference IDs from generation code — no string lookups in hot paths.
- **World-gen determinism:** everything in `worldgen/` must be a pure function of
  (seed, tileX, tileY). No `GameRandom.globalRandom`, no wall-clock, no per-region
  state that isn't derived from coordinates.
- **Localization:** every new stringID needs keys in `locale/en.lang` AND `de.lang`
  (categories mirror registry types: `[tile]`, `[object]`, `[item]`, `[mob]`,
  `[biome]`, `[level]`, `[itemtooltip]`).
- **Balance:** anchor numbers to a vanilla reference item/mob of the same tier and note
  the reference in DESIGN.md (v0.1 anchors to the Tungsten tier).
- Java style: match the existing sources (4 spaces, braces on same line, javadoc on
  every public class explaining the "why").

## Knowledge base

`docs/research/` holds verified notes about the engine (API signatures, asset formats,
load order, community patterns) including which game version each finding was checked
against. When you verify something new against the game, write it down there — future
contributors shouldn't have to re-decompile to learn it. Never paste decompiled method
bodies into the repo; document signatures and behavior in prose instead.

## Reporting bugs

Run `skyreachstatus` on the server (admin) and attach its output plus your
`logs/` server log — it contains generation statistics and placement diagnostics that
usually pinpoint world-gen issues immediately.
