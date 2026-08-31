# Agent entrypoint

Every agent — Claude, Codex, Ox, or anything else — reads this file before
modifying anything in this repository.

## Read these first, in order

1. `AGENTS.md` (this file)
2. **`docs/WORLD_DESIGN.md` — the mod's constitution.** The player's final
   concept for the whole world: nine realms from Skyreach to Hell, the
   realmDepth worldgen, the two gates, every realm's cast, economy and palette,
   the full quest order. **It outranks every other design document here.** Where
   `docs/DESIGN.md`, `docs/ART_DIRECTION.md` or `docs/assets-style-guide.md`
   disagree with it, they are stale and it wins. Part B of that file is this
   repo's own review: what the concept overturns in the existing code, the gaps
   it does not answer, and the recommended build order. Read both halves.
3. `docs/VANILLA_ASSET_MAP.md` — every vanilla asset the mod borrows, per realm.
   The working method is: build with vanilla stand-ins now, the player replaces
   them all in one pass later. **A borrowed asset that is not in that file
   breaks the swap**, so add the row in the same commit that uses it.
4. **`docs/STATUS.md` — what actually WORKS right now.** One page: which
   biomes, quests, NPCs, stations and structures are live, what is registered
   but dead, and what is not built at all. Every row carries a verification
   state. Start here before claiming anything works.
5. `docs/CURRENT_STATE.md` — the longer narrative of where the project is
6. `docs/DESIGN_DECISIONS.md` — invariants you must not silently reverse
7. `docs/TECHNICAL_LEARNINGS.md` — verified Necesse behaviour, so you do not
   rediscover the same APIs and bugs
8. `docs/IMPLEMENTATION_RULES.md` — production rules for complete, vanilla-like
   content families, tool behaviour, UI/icon completeness, worldgen composition,
   and verification states
9. the domain doc for what you are touching — `docs/ART_DIRECTION.md`,
   `docs/PLAYTEST_LOG.md`, `docs/ARCHITECTURE.md`, `docs/DESIGN.md`,
   `docs/research/`
10. `docs/WORLDBUILDING_LOOP.md` if you are expanding the world rather than
   fixing something — what already exists, the texture law on one page, and the
   seven-role loop the `.claude/agents/` definitions implement
11. recent git history (`git log --oneline -20` and the diff of anything your
   task touches)

## Rules

**Colour: there is no global "muted" rule any more.** It was retired on
2026-08-31 — the player: *"ich habe festgestellt dass viele Vorgaben für
Skyreach nicht passen zb alles nur entsättigt sein soll.. das ist falsch"*.
Saturation is per realm (`WORLD_DESIGN.md` §36). Skyreach is bright white,
cream, light blue, pink and warm gold; Eden is highly saturated; only Steinfeld
is pale. If you find a doc telling you to desaturate globally, it is stale —
fix it rather than following it.

**The repository overrides your assumptions.** A handoff message, an old
summary, or your own memory of this project can be stale. What is on disk and
in `git log` is the truth. Check before you act on a belief.

**Verify API behaviour before implementing.** Read the real class before
calling it. Never write "should be" reasoning into code.

**You can always get a game install — do not stand down for the lack of one.**
The Necesse **dedicated server** is a free public download, no account and no
purchase: `scripts/fetch_dedicated_server.sh` fetches and unpacks it and prints
the `NECESSE_GAME_DIR` to export. That one file unlocks `buildModJar`,
`scripts/integration_test.sh`, `scripts/tile_sprite_check.sh`,
`scripts/sky_map_render.sh` **and** `./gradlew decompileToSources
-PuseDecompiledSources=true`, which writes `$NECESSE_GAME_DIR/decompiled/` —
6,464 readable classes, i.e. every API question answered from source instead of
from memory. A session that reports "no game install, cannot verify" has simply
not run the script.

What the server does NOT bring is **sprites**: it never renders, so `Server.jar`
ships zero PNGs. The vanilla sprite dump the art tooling wants
(`wall_render_preview --vanilla stonewall`, `size_audit`'s reference sheets)
still needs a client install, and without it those comparisons are honestly
unavailable rather than merely skipped.

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
scripts/fetch_dedicated_server.sh     # free download; prints the path to export
export NECESSE_GAME_DIR=/path/to/necesse-dedicated-server   # contains Server.jar
./gradlew decompileToSources -PuseDecompiledSources=true    # -> $DIR/decompiled/
./gradlew buildModJar                 # or: ./gradlew clean buildModJar
scripts/integration_test.sh           # boots a real server, generates, restarts
scripts/tile_sprite_check.sh          # client-side tile sprite indices (headless)
scripts/java_syntax_check.sh          # NO game install? javac syntax-only gate
python3 tools/size_audit.py           # sprite mass vs vanilla; must print 0 flags
python3 tools/locale_audit.py         # every registered ID named in both locales
python3 tools/sheet_format_audit.py   # sheets the engine reads at fixed offsets
python3 tools/rotation_variety_audit.py # a cell the engine reads apart holds its own art
python3 tools/template_audit.py       # sprite templates match the cards that spec them
python3 tools/content_ledger.py --check # nothing registered ships undescribed
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
