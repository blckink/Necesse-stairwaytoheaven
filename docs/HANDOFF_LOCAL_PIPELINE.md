# Handoff to Codex — local art pipeline setup

Everything below the "TO BUILD" line is not built yet. Everything above it is
built, pushed and verified.

## What already works (do not rebuild)

```sh
python3 tools/asset_templates.py      # labelled templates per class -> docs/references/
python3 tools/wall_template_map.py    # the wall one (roles enumerated from the engine port)
python3 tools/asset_intake.py         # resolve + size-check + conform + preview everything new
python3 tools/asset_intake.py --apply # ship what passed
python3 tools/conform_wall_sheet.py X.png --fix [--rebuild-roof-slot]
```

Intake reads `src/main/resources/kk-sprites/`, resolves each file to the sprite
it replaces (**size decides, never the filename prefix**), downsamples integer
upscales losslessly (modal per block), runs class checks, and writes
`build/qa/intake/<name>_preview.png` = 4× dark / 4× light / 1×.
Process doc: `docs/ASSET_PIPELINE.md`.

---

## The honest verdict on ComfyUI + Flux

**Flux cannot produce a finished Necesse sprite sheet.** It has no 1:1 pixel
grid, it anti-aliases, and it carries 10–30k colours where a shipped sheet
carries 19–38. It also cannot hold one creature consistent across 24 cells
(4 directions × walk cycle) — that is not a prompt problem, it is outside what
the model does.

**So do not put it on:** wall sets, tiles/splats, floors, fences, anything
geometric (`tools/asset_generator/` is better *and* faster for those), and any
multi-cell sheet with per-cell semantics.

**It genuinely earns its place on three jobs:**

1. **Silhouette / concept exploration** — 20 variants of a prop, pick one, then
   draw or generate the real thing. Highest value, needs zero integration.
2. **SINGLE-CELL organic art** — one statue, one prop, one item icon, one
   creature pose. These are the things a Python generator is bad at.
3. **Batch + fixed seeds** — reproducible variants, which a chat UI cannot do.

The decisive advantage over a chat UI is that ComfyUI sets an **exact output
resolution**. Use it: generate at an exact **integer multiple** of the target
size (Flux is poor below ~0.5 MP, so pick the multiple that lands near
1024²–1536²), and `asset_intake` downsamples it losslessly. A non-integer size
is refused, by design.

---

## TO BUILD (in this order)

**1. `tools/comfy_recipes.py`** — one table: asset class → target size →
generation size (exact integer multiple, nearest to ~1.2 MP) → prompt scaffold
(subject slot + the fixed palette/no-AA/transparent-background block from
`docs/ASSET_PIPELINE.md` step 1). Pure data + a `--print` mode. Testable
offline; do this first.

**2. `tools/comfy_bridge.py`** — POST a workflow to ComfyUI's HTTP API
(`POST /prompt`, poll `GET /history/<id>`, fetch via `GET /view`), writing
results into `src/main/resources/kk-sprites/_incoming/`. Needs:
- `--dry-run` printing the workflow JSON without a server (so it is testable
  without ComfyUI running),
- template PNG wired in as the img2img / ControlNet input,
- width/height from the recipe, seed as a flag.
Keep the workflow JSON in `tools/comfy/` as a file, not inline.

**3. Extend `asset_intake.py`** to also scan `kk-sprites/_incoming/` and add
`--watch` (poll the folder, process new files). Small change.

**4. Do NOT** reimplement downsampling, quantizing, conforming or previewing as
ComfyUI nodes. That logic lives in `tools/` where it is tested against five
vanilla walls and the whole shipped sprite set. ComfyUI writes a file; the
Python side owns everything after that.

## Division of labour for the local LLMs

Same split the `codex-artist` agent already documents: the image model owns
*style*, this repo owns *format*. Codex/Claude should write generator code,
prompts and gates — never hand-place pixels, never bypass `asset_intake`.

## Gate before any commit

```sh
export NECESSE_GAME_DIR=/opt/necesse-server/necesse-server-1-3-2-24650233
./gradlew buildModJar
python3 tools/locale_audit.py && python3 tools/content_ledger.py --check
python3 tools/tile_behaviour_audit.py && python3 tools/sheet_format_audit.py
scripts/integration_test.sh > /tmp/itest.log 2>&1; echo $?   # unpiped!
```

Any supplied art that lands in `src/main/resources/` must also be added to
`tools/asset_generator/generate_assets.py`'s `CONVERTED` guard, or the next
generator run silently overwrites it. That mistake has shipped once.
