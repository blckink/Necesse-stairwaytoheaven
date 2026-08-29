# Sprite templates: the brief for Codex

Codex prepares **empty, correctly-shaped sprite templates** and, with each one, a
spec card naming **the vanilla asset it was modelled on**. That card is what
lets the next agent (or a human artist) see how the object slots into the engine
and how it must behave, before a single pixel is drawn.

Codex does **not** draw finished art and does **not** touch `src/` or `tools/`.
Verification is a separate pass — see §4.

The ready-to-paste prompt is §5.

---

## 1. Why this exists

Every art bug this project has shipped came from the same place: the sheet was
drawn to an invented layout instead of the one the engine reads. A door painted
over its whole cell rendered three tiles tall. A wall's autotile block was drawn
as one continuous illustration and no cell met its neighbour. A side-wall window
was a standing pane where the engine wanted a slot cut into the roof. In each
case the picture was fine and the *format* was wrong.

The cure is to fix the format **before** the drawing: an empty canvas at exactly
the right size, with the cells the engine slices marked, and a card that says
which vanilla class reads it and what each cell means.

The repo already has half of this convention in
`src/main/resources/kk-sprites/readme.md`:

> `<vanilla-name>-new-<our-name>.png` — the first half is not a replacement
> target, it names the vanilla asset the sheet was drawn on, so the setup can be
> looked up and mirrored for ours.

This brief makes that the rule for templates too, and adds the spec card.

## 2. Where it goes

```
art-templates/                     <- repo root, NOT under src/main/resources
  README.md                        <- what this folder is (Codex writes it)
  MANIFEST.md                      <- one row per template (Codex writes it)
  <our-id>/
    <vanilla-ref>-new-<our-id>.png <- the blank template, exact sheet size
    <our-id>.guide.png             <- same size, cell grid + labels + anchor line
    <our-id>.md                    <- the spec card
```

**Not under `src/main/resources/`.** Everything there ships inside the mod jar
and most of it is overwritten by `tools/asset_generator/`. Templates are working
material, not shipped assets.

## 3. What a spec card must contain

Every field must be **read from a source**, never inferred. Cite where each came
from: `[jar]` for the decompiled class, `[docs]` for a research file. If a fact
cannot be established, write `UNKNOWN — <what you looked at>` and leave the
template out rather than guessing a size.

| field | example |
|---|---|
| our id | `skywatchstele` |
| vanilla reference asset | `objects/sign.png` |
| engine class | `necesse.level.gameObject.SignObject` |
| sheet size | `128×32` |
| what the renderer reads | `sprite(rotation % 4, 0, 32, height)` — 4 rotation columns |
| what each cell/row/column MEANS | col 0 up, 1 right, 2 down, 3 left |
| draw anchor | `pos(drawX, drawY - height + 32)` |
| per-cell bands | which rows the art may occupy, if the class is anchor-sensitive |
| behaviour | toolType, objectHealth, light, collision, which layer |
| registration snippet | the real `registerObject(...)` line, vanilla's own if one exists |
| companions | `items/<id>.png`? `<id>_off.png`? a `<id>2` half? a `_mask`? |
| locale keys | which sections need an entry |
| which audits will check it | `sheet_format_audit`, `rotation_variety_audit`, … |

## 4. How it gets checked

`python3 tools/template_audit.py` verifies, for every template:

- the folder layout and the `<vanilla-ref>-new-<our-id>.png` naming
- the blank template is exactly the size its card claims, and is fully
  transparent (a template with pixels in it is finished art in the wrong place)
- the guide is the same size
- the card has every field in §3 filled, each with a `[jar]` or `[docs]`
  citation, and no `UNKNOWN` left in a template that shipped
- the named engine class actually exists in the decompiled sources when they
  are present (`$NECESSE_GAME_DIR/decompiled/`)
- the id does not already exist in `src/main/resources/`

A template that fails is not art to be fixed later; it is a wrong specification
that would have produced wrong art.

## 5. The prompt

Paste this to Codex verbatim.

---

> **Repository:** `\home\blackoffset\dev\Necesse-stairwaytoheaven`
>
> You are preparing **sprite templates** for a Necesse 1.3.2 content mod. You do
> **not** draw finished artwork. You produce, for each sprite the mod still
> needs, an empty canvas at exactly the size the engine reads, a visual guide
> showing the cells, and a spec card naming the vanilla asset and class it is
> modelled on.
>
> **Read first, in this order, and treat them as law:**
> `AGENTS.md` · `docs/CODEX_SPRITE_TEMPLATE_BRIEF.md` (this brief; §2 and §3 are
> your output contract) · `docs/research/asset-formats.md` ·
> `docs/assets-style-guide.md` · `docs/research/structures-furniture.md` ·
> `docs/research/furniture-formats.md` · `docs/research/splat-format.md` ·
> `src/main/resources/kk-sprites/readme.md` (the naming convention you are
> extending) · `docs/TECHNICAL_LEARNINGS.md` (search it for the family you are
> working on before you write its card — it records the traps).
>
> **What to produce.** The work order is `docs/design/chapter-01-skyreach-pois.md`
> §4 and the art the cast brief implies in
> `docs/design/chapter-01-skyreach-cast.md`. Concretely:
>
> - objects: `skywatchstele`, `cloudspringfont`, `prismchime`, `sovereignaltar`,
>   `skywaywaystone`
> - item icons: `wardenledger`, `sovereignshard`, `sovereignkey`,
>   `aeronautcharm`
> - creatures: `skywatchrevenant` (body sheet + bestiary icon),
>   `fulgurshade` and `reefmaw` (bestiary icons only)
> - wearables: `shepherdcowl`, `shepherdsmock`, `shepherdsmockarms_left`,
>   `shepherdsmockarms_right`, plus their two item icons
> - stations from the cast brief: `fermentationvat`, `aethericdraftingtable`,
>   `kiterack` — each also needs its item icon
> - optional: `ui/mapicons/skypoi.png`
>
> **Establish every format from a source, never from memory.** If
> `$NECESSE_GAME_DIR` is set, the decompiled sources are at
> `$NECESSE_GAME_DIR/decompiled/` — read the class's `loadTextures` and
> `addDrawables` and quote the lines. If they are not present, run
> `scripts/fetch_dedicated_server.sh` (a free download, no account) and
> `./gradlew decompileToSources -PuseDecompiledSources=true` to get them. Only if
> that is impossible, fall back to the research docs and cite them as `[docs]`.
> **A size you cannot cite is a size you must not write down.**
>
> For each sprite, in `art-templates/<our-id>/`:
>
> 1. `<vanilla-ref>-new-<our-id>.png` — the blank template. Exact sheet
>    dimensions, **fully transparent**, nothing drawn.
> 2. `<our-id>.guide.png` — the same dimensions, showing: a 1px border on every
>    cell the engine slices, the cell index or meaning as small text where it
>    fits, and a marked line for the tile's top edge / the draw anchor where the
>    class is anchor-sensitive (a wall's door cells, a fruit bush, wall decor).
>    Use a colour that could never be mistaken for art — magenta on transparent.
> 3. `<our-id>.md` — the spec card with every field in §3 of the brief, each fact
>    carrying `[jar]` (decompiled class, quote the line) or `[docs]` (which file,
>    which section). Where vanilla registers an analogous object, paste its real
>    `registerObject(...)` line.
>
> Then write `art-templates/README.md` (what the folder is, the naming rule, how
> to use a template) and `art-templates/MANIFEST.md` (one row per template: our
> id · vanilla reference · engine class · sheet size · which agent draws it).
>
> **Hard rules.**
> - Do not modify anything under `src/` or `tools/`. Do not run the asset
>   generator. Templates never ship in the mod jar.
> - Do not invent a format. `UNKNOWN — <what you checked>` in the card, and leave
>   that template out of the manifest, is a correct and useful answer.
> - Do not draw art. A template with opaque pixels fails the check.
> - One folder per sprite, even for a single 32×32 icon.
> - Do not commit; leave the working tree for review.
>
> **Self-check before you finish:** run `python3 tools/template_audit.py` and fix
> what it reports. Then report: how many templates you produced, which formats
> you established `[jar]` versus `[docs]`, and every `UNKNOWN` you hit and what
> you looked at.

---

## 6. After Codex

The templates are a specification, not a deliverable. The art still gets drawn
in `tools/asset_generator/` — the pipeline is deterministic and a hand-made PNG
that the generator overwrites is a bug waiting to happen
(`IMPLEMENTATION_RULES.md` §6). What the template buys is that the generator
starts from a canvas whose size and cell meaning were established from the
engine rather than assumed.

---

## 7. The round trip, and the thing it cannot do

Codex works in **its own checkout on its own machine**. There is no shared
filesystem with the session reviewing it. **The git remote is the only channel
between the two**, and that has one consequence worth stating plainly before
anyone builds a habit on a wrong assumption:

> **Nothing notifies the reviewer when Codex finishes.** There is no hook, no
> watch, no event. A reviewing session finds out either because a human says so,
> or because it goes and looks.

So the loop is explicit, and every step is a real command:

1. **Codex pushes a branch** carrying `art-templates/`. Name it
   `codex/sprite-templates` so it is obvious what it holds. Codex does not
   commit to the feature branch and never touches `src/` or `tools/`.
2. **A human says the branch is up**, or the reviewer polls
   `git fetch origin codex/sprite-templates`.
3. **The reviewer runs the round trip:**

   ```bash
   scripts/review_templates_branch.sh codex/sprite-templates --push
   ```

   It fetches the branch into a **throwaway worktree** (so reviewing can never
   disturb the branch the reviewer is working on), runs
   `tools/template_audit.py --root <worktree>` against it, writes
   `art-templates/REVIEW.md` — a checkbox fix list, one line per template whose
   files and card disagree — and pushes that review back **onto Codex's own
   branch**.
4. **Codex pulls and starts its next run by reading its own fix list.** That is
   the whole feedback mechanism: a file that travels on the branch, not console
   output somebody has to copy by hand.

`--root` is load-bearing and it is a trap worth naming: `template_audit.py`
derives its paths from its own file location, so a reviewer who forgets it
audits their *own* checkout and reports the branch clean without ever having
looked at it.

Repeat from step 1 until `REVIEW.md` says *Nothing to fix*. Note that **zero
templates is not a pass** — the review says "Nothing produced yet" for an empty
inbox precisely so it cannot be mistaken for one.
