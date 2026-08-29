# Handoff prompt for the working instance

Paste §2 to a Claude Code session already running in this repository. §1 is the
evidence behind it, kept here so the prompt can stay short and the numbers can be
re-checked rather than believed.

---

## 1. What the numbers actually say about Skyreach walls and buildings

Measured on this branch, not remembered:

| wall set | colours | cap lum | cap dominant | face lum | face dominant | worldgen uses |
|---|---|---|---|---|---|---|
| `cloudmarblewall` | 24 | 85.5 | 79% | 159.7 | 16% | 4 |
| `skystonebrickwall` | 24 | 52.1 | 91% | 114.6 | 58% | **0** |
| `nightfellwall` | 24 | 24.6 | 91% | 44.6 | 59% | 1 |
| `beetlewall` | 38 | 30.9 | 78% | 63.9 | 18% | 1 |

The **format** is now healthy across all four: every sheet sits in the 20–40
colour band vanilla uses, every cap has a dominant tone, all four draw their
side-wall window as a slot cut into the roof, and every door cell holds its own
picture.

The **problem is not the walls, it is that almost nothing is built out of them.**
`skystonebrickwall` — a complete registered family with door, window and item
icons — is referenced by worldgen **zero times**. Nightfell and Beetlefreak get
one use each. The Skyreach contains exactly **one** building: the Warden's Spire.
Three plank floors (`charfloortile`, `nimbusfloortile`, `prismfloortile`) have
never been placed either.

So "bei Wänden und Gebäuden ist noch nicht alles gut" reads, in the data, as:
*the material exists and the world does not use it.* Prettier sheets will not fix
that. Buildings will.

Two things about the walls are still genuinely open and should be checked rather
than assumed fixed:

- **Face variety.** Cloudmarble's and Beetlefreak's front faces have only 16–18%
  dominant tone against skystone's and nightfell's 58–59%. That may be right —
  those two sets carry an arcade and swirl motifs — or it may be noise. It
  cannot be settled without vanilla sheets to compare against, and the dedicated
  server ships none.
- **Nothing has been seen in game since the cloudmarble rebuild.** Every wall
  claim on this branch is `FIXED — NOT YET PLAYER CONFIRMED`.

---

## 2. The prompt

> You are continuing work on the **Stairway to Heaven** Necesse mod in this
> repository. A parallel session has just landed a large amount of
> infrastructure and design; your job is to pick it up, keep building, and not
> stop after a burst.
>
> **First, orient — in this order, and treat them as law:**
> `AGENTS.md` · `docs/CURRENT_STATE.md` · `docs/DESIGN_DECISIONS.md` ·
> `docs/IMPLEMENTATION_RULES.md` · `docs/WORLDBUILDING_LOOP.md` (the complete
> setup: roster, process, texture law, open themes) · `docs/PLAYTEST_LOG.md`
> (everything marked KEEP is player-approved — build toward it, never over it).
>
> **Get the toolchain before anything else.** The game install is a **free
> download** and nothing here can be verified without it:
> ```bash
> scripts/fetch_dedicated_server.sh          # prints the path to export
> export NECESSE_GAME_DIR=<that path>
> ./gradlew decompileToSources -PuseDecompiledSources=true
> ```
> That gives you 6,464 readable engine classes at
> `$NECESSE_GAME_DIR/decompiled/`. **Read the real class before calling it.** Three
> sessions in a row reported "cannot verify" and worked around it; that excuse is
> gone. What the server does *not* bring is sprites — it never renders — so the
> vanilla comparison strips in `wall_render_preview` and `size_audit` are
> honestly *unavailable*, not "skipped".
>
> **Then run the loop and keep it running:**
> ```
> /loop /chapter
> ```
> `.claude/commands/chapter.md` is one whole chapter: designer → POI architect →
> four art agents in parallel (plus `codex-artist` where style matters) →
> integrator, with the completeness checklist and every gate. It refuses to stop
> at "design done" or "art done". A chapter ends **green and pushed**, or it ends
> **loudly** with what blocked it written into `docs/CURRENT_STATE.md` so the next
> iteration picks that up first.
>
> **What is already designed and waiting to be built** — do not redesign it:
> `docs/design/chapter-01-skyreach-pois.md` (14 POIs with tile-grid plans, object
> lists and a stated reward each) and `docs/design/chapter-01-skyreach-cast.md`
> (three settler types, three place-bound enemies, eight unique rewards). That is
> the next chapter's work order.
>
> **YOU MAY AND SHOULD IMPROVE EXISTING ASSETS.** If something we shipped is not
> good, fix it — that is explicitly wanted, not a detour. Three guardrails, and
> only three:
> 1. Never redraw anything marked **KEEP** in `docs/PLAYTEST_LOG.md`.
> 2. Never hand-edit a PNG under `src/main/resources/` — the generator owns it
>    and will overwrite you silently. Change `tools/asset_generator/`, regenerate,
>    diff.
> 3. Never silently reverse `docs/DESIGN_DECISIONS.md`. Say so and stop.
>
> **On the Skyreach's walls and buildings specifically**, because that was named:
> the four wall sets are now *formally* correct — 24–38 colours each, dominant
> caps, the side window is a slot in the roof on all four, every door cell is its
> own picture. **The real fault is that the world barely uses them.**
> `skystonebrickwall`, a complete registered family, is referenced by worldgen
> **zero times**; nightfell and beetlewall once each; three plank floors have
> never been placed; and the entire Skyreach holds exactly **one** building. So
> the highest-value work is not another sheet — it is *building things out of what
> already exists*, which is exactly what the 14-POI dossier is for. Two wall
> questions are still open and worth an eye: cloudmarble's and beetlewall's front
> faces have only 16–18% dominant tone against the other two sets' 58–59%, and
> nothing has been seen in game since the cloudmarble rebuild.
>
> **Direction.** Bias to the **endgame** — the shipped power band stops at
> Tungsten and the roster is mostly easy. New content sits at or past Aetherium,
> toward incursion pressure, with loot that is a new capability rather than a
> bigger number. Beyond the Skyreach, the two named fronts are:
> - **The Veil**, in Beetlejuice/Burton register — and **Burton is contrast, not
>   darkness**. Acid green on violet, bone white on black, stripes,
>   checkerboards, spirals, sickly pink, brass and verdigris. Black is an outline
>   colour and a shadow, never a fill. One saturated accent per set that is
>   nobody else's, and one funny thing per chapter. Open: the Model Town, the
>   Office of Eternity, a zombie quarter at the Ashen Reach with the Ashwyrm,
>   Mortimer and Vesper, the "Haunted & Homely" deco set.
> - **A surface biome of our own** — something a player stumbles into mid-game
>   that yields base materials and textures available nowhere else, with its own
>   inhabitants and its own trouble. It must read as *found*, not as a second
>   Skyreach; the Veil leaking upward is the established hook (`DESIGN.md` Part
>   IV §26).
>
> **Every gate, every chapter**, and state honestly which ran:
> ```bash
> ./gradlew buildModJar && scripts/integration_test.sh
> python3 tools/size_audit.py && python3 tools/locale_audit.py
> python3 tools/sheet_format_audit.py && python3 tools/rotation_variety_audit.py
> python3 tools/tile_behaviour_audit.py && python3 tools/furniture_audit.py
> python3 tools/content_ledger.py --check
> python3 tools/asset_generator/generate_assets.py && git status   # determinism
> ```
> Then **open** `build/qa/` and the sprite gallery. Green numbers are not the bar:
> every art bug this project has shipped passed the numeric gates and was obvious
> in a picture.
>
> **Documentation is part of the content, not after it.** Every item, object,
> mob, tile, quest, sprite and recipe gets a row in `docs/CONTENT_LEDGER.md` **in
> the same commit that adds it** — `content_ledger.py --check` reads registrations
> out of the source and fails on anything undescribed. Also update
> `docs/CURRENT_STATE.md`, append newly proven engine behaviour to
> `docs/TECHNICAL_LEARNINGS.md`, and add each chapter to `CHANGELOG.md`.
>
> Use the verification states from `IMPLEMENTATION_RULES.md` §14 and never
> upgrade an automated result to player-confirmed.
