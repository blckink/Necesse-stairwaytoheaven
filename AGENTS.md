# Agent entrypoint

Every agent — Claude, Codex, Ox, or anything else — reads this file before
modifying anything in this repository.

## Read these first, in order

1. `AGENTS.md` (this file)
2. `docs/CURRENT_STATE.md` — where the project actually is right now
3. `docs/DESIGN_DECISIONS.md` — invariants you must not silently reverse
4. `docs/TECHNICAL_LEARNINGS.md` — verified Necesse behaviour, so you do not
   rediscover the same APIs and bugs
5. `docs/IMPLEMENTATION_RULES.md` — production rules for complete, vanilla-like
   content families, tool behaviour, UI/icon completeness, worldgen composition,
   and verification states
6. the domain doc for what you are touching — `docs/ART_DIRECTION.md`,
   `docs/PLAYTEST_LOG.md`, `docs/ARCHITECTURE.md`, `docs/DESIGN.md`,
   `docs/research/`
7. recent git history (`git log --oneline -20` and the diff of anything your
   task touches)

## Rules

**The repository overrides your assumptions.** A handoff message, an old
summary, or your own memory of this project can be stale. What is on disk and
in `git log` is the truth. Check before you act on a belief.

**Verify API behaviour before implementing.** The decompiled game sources are
at `$NECESSE_GAME_DIR/../decompiled` (and the sprite dump beside it). Read the
real class before calling it. Never write "should be" reasoning into code.

**Never silently reverse a design decision.** If `docs/DESIGN_DECISIONS.md`
records something and you believe it is wrong, say so to the user and wait.
Do not quietly implement the opposite.

**Build complete content, not isolated assets.** Any new content family must
follow `docs/IMPLEMENTATION_RULES.md`: correct native archetype, registry/category,
world sprite/sheet, item/menu icon where applicable, locale, crafting/obtainability,
tool interaction, drops, persistence/despawn semantics, and relevant QA. A PNG
or compiling class alone is not finished content.

**Real in-game player feedback outranks automated metrics.** `tools/size_audit.py`
is a safety net that catches sprites which are objectively too small. It is not
art direction, and passing it does not mean an asset reads well in game. When a
screenshot and a metric disagree, the screenshot wins.

**Visual asset work must be bounded, never open-ended.** Do not let an LLM or
subagent spend long reasoning loops repeatedly redrawing the same sprites. For a
visual-only task, produce one coherent candidate set, perform at most one focused
correction pass, then stop and hand it to the user for visual review. Do not
spend more than about 10 minutes iterating on generated artwork, and do not burn
large token budgets trying to make art "perfect" through repeated code-driven
redraws. If the task is still visually uncertain after that pass, report what is
uncertain and hand off rather than continuing autonomously. Prefer dedicated
visual/image tooling for the artwork itself; use coding agents for format,
integration, validation, generator reproducibility, and engine constraints.

**Record what you verified.** After finishing, append newly proven behaviour to
`docs/TECHNICAL_LEARNINGS.md`, update `docs/CURRENT_STATE.md` if the state
changed, and append to `docs/PLAYTEST_LOG.md` if the user reported something.
Only write things you actually observed. Mark anything unproven as a hypothesis.

**Do not claim something is verified when it was only read out of source.**
"The decompiled code says X" and "I ran it and saw X" are different claims.
Say which one you have. Use the verification states defined in
`docs/IMPLEMENTATION_RULES.md`.

## Build and test

```bash
export NECESSE_GAME_DIR=/path/to/necesse-dedicated-server   # contains Server.jar
./gradlew buildModJar                 # or: ./gradlew clean buildModJar
scripts/integration_test.sh           # boots a real server, generates, restarts
scripts/tile_sprite_check.sh          # client-side tile sprite indices (headless)
python3 tools/size_audit.py           # sprite mass vs vanilla; must print 0 flags
python3 tools/locale_audit.py         # every registered ID named in both locales
python3 tools/sheet_format_audit.py   # sheets the engine reads at fixed offsets
python3 tools/rotation_variety_audit.py # a cell the engine reads apart holds its own art
python3 tools/tile_behaviour_audit.py # every tile is what it is presented as
python3 tools/wall_render_preview.py  # walls: compose scenes, then LOOK at build/qa/
python3 tools/rotation_preview.py     # rotations: every cell where it lands, then LOOK
```

The server integration test **cannot see client rendering bugs**. See
`docs/TECHNICAL_LEARNINGS.md` for what each gate does and does not cover.

## Working alongside other agents

`docs/AGENT_WORKFLOW.md` defines the phases and the file-ownership rules for
parallel work. The short version: two agents never edit the same file at the
same time, generated assets are changed by editing the generator, never by
editing the PNG, and every new content family must be completed according to
`docs/IMPLEMENTATION_RULES.md` rather than left as an art/code placeholder.
