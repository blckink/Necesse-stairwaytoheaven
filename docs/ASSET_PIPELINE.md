# From a generated image to a shipped sprite

**The one page for: "ich will bei ChatGPT Grafiken generieren und previewen,
ohne manuelles Rumgebastel in Photoshop."**

Three commands, no image editor.

```sh
python3 tools/asset_templates.py            # 1. get the template for the class
#    ... generate art on it ...
python3 tools/asset_intake.py               # 2. check + preview everything new
python3 tools/asset_intake.py --apply       # 3. ship what passed
```

---

## Why a template at all

An image model draws a nice picture. The engine does not read a picture — it
reads *cells*, and it reads them in an order nobody guesses. A wall sheet's
columns are tile HALVES whose meaning changes by row. A mob sheet's rows are
Up/Right/Down/Left. A terrain splat is 21 cells each with a required alpha
shape. Get any of those wrong and the art is not "slightly off", it is rubble
— and no amount of Photoshop nudging fixes a picture that was composed for the
wrong grid.

So the template is the fix: a canvas at the exact shipped size with every cell
labelled. Hand it to the generator *as an input image* and it composes into the
cells instead of across them.

| class | template | shipped size |
|---|---|---|
| wall set (wall + doors + window) | `docs/references/wall-template-map.png` | 352×128 |
| walking mob | `docs/references/template-mob.png` | 384×320 |
| terrain / liquid ground | `docs/references/template-splat.png` | 224×(96·variants) |
| object (prop, plant, statue, furniture) | `docs/references/template-object.png` | 32·variants × 32·N |
| item icon | `docs/references/template-item.png` | 32×32 |

Regenerate them any time with `python3 tools/asset_templates.py`
(walls: `python3 tools/wall_template_map.py`).

---

## Step 1 — generate

**Attach the template PNG** and use a brief of this shape. The parts that
matter are the last three lines; without them you get a smooth illustration and
the pipeline will tell you to redraw.

> Pixel art in the style of the game Necesse. Use the attached template as the
> exact canvas: **output must be exactly `<W>×<H>` pixels** and every element
> must sit inside the cell the template labels for it.
>
> Subject: `<what it is, 1–2 sentences>`
> Palette: `<3–5 hex colours>`, nothing outside them.
>
> Hard requirements: true pixel art on a 1:1 pixel grid, **no anti-aliasing, no
> gradients, no blur, no soft shadows**. Flat colour areas, at most 4 shade
> steps per material. Light comes from the top-left. Transparent background —
> no backdrop, no frame, no drop shadow, no text or labels in the output.

**If the tool cannot output an exact size** (most cannot): ask for an exact
integer multiple — 2×, 3× or 4× the template — and nothing else. `asset_intake`
downsamples an integer upscale losslessly by taking the most common colour of
each block. A 1.5× or "roughly 400px" render cannot be recovered and is
refused.

**The palette line is what makes or breaks it.** Shipped Necesse art carries
19–38 distinct colours per sheet. A default generator render carries ten to
thirty *thousand*. Naming 3–5 exact colours is the single most effective
instruction in the whole brief.

## Step 2 — drop the file in and check it

Put the file in `src/main/resources/kk-sprites/` and name it after the sprite
it replaces:

```
<name>.png                    replaces our sprite called <name>
<name>-new-<ourname>.png      new sprite <ourname>, drawn on vanilla <name>
```

Then:

```sh
python3 tools/asset_intake.py
```

Per file it prints the resolved target, the class, whether the size works, the
colour count, and a preview path. It **never writes into the mod** without
`--apply`.

The prefix in a filename is *not* trusted — a supplied pair once arrived with
`items-` on the object sheet and `objects-` on the icon. Where a name matches
more than one shipped sprite, the **size** decides, and the report says so.

**What it will tell you, and what each means:**

| report | what to do |
|---|---|
| `size exact` / `downsampled 3x (modal)` | nothing, it worked |
| `REFUSED: … not an integer upscale` | regenerate at the template size or an exact 2×/3×/4× |
| `REPACK, not a resize` | same pixels, different arrangement — needs a per-asset repack function, ask me |
| `colours 14523` | it is a smoothed render, not pixel art. Re-prompt with the palette line |
| `fully opaque` | the background was not transparent |
| wall `FIX seam …` | two cells do not join; `--fix` blends near-misses, big ones need a redraw |
| `no shipped sprite of that name` | new art — tell me where it goes, or rename it |

## Step 3 — look at the preview, then apply

Every file gets `build/qa/intake/<name>_preview.png`: **4× on a dark ground,
4× on a light ground, and 1× underneath**. The 1× strip is the honest one —
that is the size a player sees it at. If it only reads at 4×, it is not
finished.

Wall sheets additionally get real composed scenes next to vanilla walls
(`build/qa/conform_<name>_dark.png`): solid blocks, corners, free-standing
runs, doors and windows in both orientations, with `stonewall` and `woodwall`
drawn one strip down for comparison.

```sh
python3 tools/asset_intake.py --apply
```

writes only the files whose checks passed. Then tell me, and I add them to
`generate_assets.py`'s `CONVERTED` guard — otherwise the next generator run
overwrites your art. (That exact mistake shipped once; the guard exists to
catch it.)

---

## The one thing that cannot be automated

Whether the art is *good*. The tools can prove a sheet is on-format, tiles
without seams, and sits in vanilla's texture bands. They cannot tell you the
silhouette reads, the palette belongs to its realm, or the thing looks like
what it is meant to be. That is what the 1× preview is for, and ultimately what
playing it is for — `docs/PLAYTEST_LOG.md` is where your verdicts live, and
anything marked KEEP there outranks every measurement in this file.

## If you want art that fits without generating it

`tools/asset_generator/` draws every shipped sprite deterministically, in
Python, from the palettes in `palette.py`. For anything geometric — walls,
floors, fences, tiles — that route is usually faster and always on-format.
Generated art is for the things a program is bad at: creatures, faces,
attitude, ornament.
