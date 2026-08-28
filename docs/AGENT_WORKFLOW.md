# Agent workflow

How work runs here, alone or in parallel.

## The six phases

**1 — Sync.** `git status`, `git log --oneline -20`, `git fetch`. Read
`AGENTS.md`, `docs/CURRENT_STATE.md`, `docs/DESIGN_DECISIONS.md`,
`docs/TECHNICAL_LEARNINGS.md`. A handoff message can be stale; the repository
is not. If they disagree, the repository wins and you say so.

**2 — Plan.** Name the files you will own. Check nobody else owns them. If two
tasks want the same file, sequence them instead of running both.

**3 — Implement.** Coherent small batches. Verify each API against the
decompiled sources before calling it. Build after each batch, not at the end.

**4 — Test.**

```bash
./gradlew buildModJar          # or clean buildModJar after structural changes
scripts/integration_test.sh    # server boot, generation, restart persistence
scripts/tile_sprite_check.sh   # client-side tile sprite indices, headless
python3 tools/size_audit.py    # 0 flags required
python3 tools/locale_audit.py  # no registered ID may show as a raw string ID
python3 tools/sheet_format_audit.py  # sheets the engine reads at fixed offsets
python3 tools/tile_behaviour_audit.py  # every tile is what it is presented as
```

Match the gate to the change. A client rendering change is not covered by the
server test — say so rather than implying it passed.

The two sprite audits answer different questions and neither substitutes for
the other. `size_audit` asks "is there enough ink" — it would happily pass a
door painted over its whole cell, because that has *more* ink than a correct
one. `sheet_format_audit` asks "is the ink in the rows the engine draws from",
which is the only thing that catches a sprite rendering at the wrong size or
in the wrong place.

`tile_behaviour_audit` is the third of that family and covers ground tiles on
both of the sides a tile can be wrong on: the Java declaration (is this floor
really `isFloor`, does it splat at PRIORITY_FLOOR, is it craftable) and the
`_splat` atlas the renderer marches over (are the blend cells the size vanilla
draws them at). It exists because a floor that ships as terrain, or a blend
cell painted as the complement of its intended shape, reaches the player as
"this floor does not behave like a normal floor" and neither of the other two
audits can see it.

**5 — Record.** Append proven behaviour to `docs/TECHNICAL_LEARNINGS.md`,
refresh `docs/CURRENT_STATE.md` if the state moved, append player feedback to
`docs/PLAYTEST_LOG.md`. Only what you observed.

**6 — Hand off.** Files changed · tests actually run · known risks · what is
left. Never claim a test you did not run, and never call something
player-verified that only you have seen.

## Parallel agents

**One owner per file.** Two agents editing the same generator or source file at
once will silently clobber each other. Ownership is declared up front and does
not overlap. Shared files — `tools/size_audit.py`, `tools/sprite_gallery.py`,
`CHANGELOG.md`, the shared docs — belong to the coordinator, not to workers.

**Watch for shared helper functions.** File ownership is not enough on its own:
two sprites can share a helper inside one file, and one agent's change to that
helper silently alters the other's sprite. After any art change, regenerate and
`cmp` against `src/main/resources/` to catch collateral damage.

**Research agents report, they do not edit** unless the task explicitly assigns
them files.

**One integration reviewer** verifies and merges the others' output: reruns the
gates, checks determinism, reviews the diffs, writes the commits. Workers do
not commit or push.

**Give every agent its acceptance criterion up front**, as a number or a
command where possible. "Make it bigger" produces a bigger blob; "the densest
64px frame must reach 0.75 of `boar.png` and the walk cycle must change pose"
produces work you can check.

**Worktrees** (`isolation: "worktree"`) when agents would otherwise fight over
the tree. For read-only research or cleanly partitioned files, plain parallel
agents are fine.

## Commits

Small and self-contained. A blocker fix ships alone, never bundled with
unrelated work, so it can be pulled and tested on its own.

The message explains **why**, and states what was verified and how. "Fixed the
crash" is not a record; the root cause, the reason vanilla does not hit it, and
the evidence are. Future agents read these before they read the code.

## Running the worldbuilding loop

The six phases above are how *a* task runs. `docs/WORLDBUILDING_LOOP.md` is how
a **chapter of new world** runs: a design brief, then a POI dossier, then four
art specialists in parallel, then one integrator who wires it up, gates it,
commits it and reports. The seven roles have agent definitions in
`.claude/agents/`, and each one carries its own formats, budget and acceptance
criterion so it does not have to go looking.

Everything on this page still applies inside that loop — one owner per file, the
gates matched to the change, workers do not commit. What the loop adds is the
sequencing between the roles and a hard cap on how long art batches may run.
