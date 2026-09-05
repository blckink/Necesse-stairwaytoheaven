---
description: Run ONE complete chapter of the worldbuilding loop — design, layout, art, integration, documentation — and leave the repo green and pushed.
argument-hint: "[theme or realm, e.g. 'veil zombie apocalypse' or 'surface biome'] (optional — picks the next open one if omitted)"
---

Run **one complete chapter** of `docs/WORLDBUILDING_LOOP.md`. A chapter is
finished content in the player's hands, not a design and not a pile of PNGs.

Theme: **$ARGUMENTS** — if that is empty, pick the next open one from
`docs/WORLDBUILDING_LOOP.md` §7 and say which you picked and why.

## Before anything

```bash
git fetch && git log --oneline -15
cat docs/OVERVIEW.md
[ -n "$NECESSE_GAME_DIR" ] || scripts/fetch_dedicated_server.sh
```

The game install is a free download and it is not optional — without it nothing
here can be compiled or tested. Read `AGENTS.md`,
`docs/DESIGN_DECISIONS.md` and the `docs/TECHNICAL_LEARNINGS.md` sections for
whatever you are about to touch.

## The phases

Run 1 and 2 in sequence, the four art agents in **parallel** (they own separate
generator modules), then 4 alone.

1. `biome-designer` — the brief: where it attaches, the palette with its own
   exclusive accent, the cast, two resources, the story, the hook per piece.
2. `poi-architect` — **at least 8 POIs** with tile-grid plans, object lists with
   rotations, a counted lighting rhythm, and a stated reward each.
3. In parallel: `art-walls-tiles` · `art-wearables` · `art-creatures` ·
   `art-props`, plus `codex-artist` for any sprite where style matters more than
   speed. One family per agent per run, 6–12 sprites, one correction pass.
4. `content-integrator` — registration, worldgen, recipes, locale, every gate,
   the commits, and the chapter report.

## Done means all of this

Per new family, `IMPLEMENTATION_RULES.md` §1 and §12:

- native archetype and correct registry category — verified by **reading the
  decompiled class**, not from memory
- world sprite / mob sheet **and** `items/<id>.png`
- EN + DE names, tooltip where it helps
- a recipe at the right station, **or a written reason there is none**
- correct tool type and tier, HP, drops, light, collision, persistence
- **reachable**: worldgen places it, or a quest hands it over, somewhere a
  player will actually find it
- tameable animals legible as tameable: husbandry path, a feed they can eat,
  breeding true, a tooltip that says what they are for

Gates, all of them, and state honestly which ran:

```bash
./gradlew buildModJar && scripts/integration_test.sh
python3 tools/size_audit.py && python3 tools/locale_audit.py
python3 tools/sheet_format_audit.py && python3 tools/rotation_variety_audit.py
python3 tools/tile_behaviour_audit.py && python3 tools/furniture_audit.py
python3 tools/asset_generator/generate_assets.py && git status   # determinism
python3 tools/content_ledger.py --check                          # nothing undocumented
```

Then **open** `build/qa/` and the sprite gallery. Green numbers are not the bar;
every art bug this project shipped passed the numeric gates.

## Documentation is part of the chapter, not after it

Every single thing added — item, object, mob, tile, quest, sprite, recipe —
gets a row in `docs/CONTENT_LEDGER.md` in the same commit that adds it.
`tools/content_ledger.py --check` fails on anything registered and undescribed,
so this is enforced rather than remembered. Also update
`docs/OVERVIEW.md`, append anything newly proven to
`docs/TECHNICAL_LEARNINGS.md`, and add the chapter to `CHANGELOG.md`.

## Finish

Commit in small self-contained pieces with messages that say **why**, push the
branch, and report: what is new in the player's words, where to find it in game,
every gate with its result, what is verified at which level
(`IMPLEMENTATION_RULES.md` §14), and what is still open.

**Do not stop early.** A chapter that ends with design but no content, or art
but no registration, is not a chapter — finish it or say precisely what blocked
you and why nothing else could proceed without it.
