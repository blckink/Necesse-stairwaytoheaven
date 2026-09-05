#!/usr/bin/env python3
"""Prepare one asset for the image generator: the vanilla original + a brief.

This is the step the player has been doing by hand -- look up which sprite is
still borrowed, find the vanilla original, and write out what the replacement
has to be. The rules in the brief are the player's own, in the player's own
order, because they are what makes a returned PNG usable:

  * the vanilla file is the **base**, not an inspiration,
  * frame position, alignment, spacing and size stay exactly as vanilla has
    them -- that grid is a contract with the engine, not a layout choice,
  * shape may deviate within reason, as long as it stays inside its frame,
  * **no icons** -- those get cut from the finished sheet later.

What it writes, per asset, under build/asset-briefs/<id>/:

    <vanilla>.png   the original, copied out to attach to the generator
    brief.md        the text to send with it
    grid.png        the original with its frame grid drawn on top, as a
                    reading aid for a human -- never send this one as the base

Usage:
    PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/asset_brief.py edenserpent
    ... --limit 5              prepare the next five open assets
    ... --realm eden --limit 3
"""
import argparse
import os
import re
import shutil
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO, "tools"))
OUT = os.path.join(REPO, "build", "asset-briefs")

from asset_worklist import build as build_worklist  # noqa: E402

CELL_RE = re.compile(r"cell(?:\s+size)?\s*[:=]?\s*(\d+)\s*px", re.IGNORECASE)
COLSxROWS_RE = re.compile(r"(\d+)\s*(?:×|x)\s*(\d+)\s*px\s*animation", re.IGNORECASE)


def guess_cell(item):
    """Work out the frame size, and say how confident that is.

    Order matters: an explicit "cell 64px" in the notes beats anything
    derived, because several sheets in this list deliberately break the
    family default (a 382-wide sheet, a single-row turret strip).
    """
    notes = item.get("notes", "") or ""
    m = CELL_RE.search(notes)
    if m:
        return int(m.group(1)), "stated in the notes"
    m = COLSxROWS_RE.search(notes)
    if m:
        return int(m.group(2)), "from the animation-column note"
    w, h = item.get("actual_size") or item.get("declared_size") or (0, 0)
    # The walking-mob contract: four direction rows over N columns.
    for cell in (128, 64, 32):
        if w % cell == 0 and h % cell == 0 and h // cell >= 4:
            return cell, "inferred from %dx%d being a clean multiple" % (w, h)
    for cell in (32, 16, 8):
        if w % cell == 0 and h % cell == 0:
            return cell, "inferred, weakly -- check against vanilla before drawing"
    return None, "could not be determined; read the vanilla file yourself"


def draw_grid(src, dst, cell):
    from PIL import Image, ImageDraw
    with Image.open(src) as im:
        im = im.convert("RGBA")
        scale = 2 if max(im.size) < 400 else 1
        if scale > 1:
            im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
        d = ImageDraw.Draw(im)
        step = cell * scale
        for x in range(0, im.width + 1, step):
            d.line([(x, 0), (x, im.height)], fill=(255, 0, 128, 255), width=1)
        for y in range(0, im.height + 1, step):
            d.line([(0, y), (im.width, y)], fill=(255, 0, 128, 255), width=1)
        im.save(dst)


# Which shipped files speak for a realm. Measuring the palette off the mod's
# own art beats naming colours from a design doc: the doc says "high
# saturation", the files say #003000.
REALM_KEYS = {
    "eden": ("eden", "paradise", "serpent", "sungrape", "goldenorchid", "knowledge"),
    "skyreach": ("sky", "cloud", "nimbus", "storm", "aurora", "gale", "zephyr"),
    "ghost": ("ghost", "spirit", "bonewood", "spectral", "mourning", "coffin"),
    "steinfeld": ("steinfeld", "grave", "hollow", "pilgrim", "mourner"),
    "crooked": ("crooked", "gloom", "wrongway", "checker"),
    "veil": ("veil", "fog", "mist"),
}


def realm_palette(realm, limit=8):
    """The dominant colours of the realm's own shipped art, measured."""
    import collections
    import glob
    key = None
    low = (realm or "").lower()
    for name, _ in REALM_KEYS.items():
        if name in low:
            key = name
            break
    if key is None:
        return []
    from PIL import Image
    counts = collections.Counter()
    files = [p for p in glob.glob(os.path.join(REPO, "src/main/resources/**/*.png"),
                                  recursive=True)
             if any(k in os.path.basename(p).lower() for k in REALM_KEYS[key])]
    for p in files[:25]:
        try:
            with Image.open(p) as im:
                im = im.convert("RGBA")
                px = im.load()
                for y in range(0, im.height, 2):
                    for x in range(0, im.width, 2):
                        r, g, b, a = px[x, y]
                        if a >= 128:
                            counts[(r // 16 * 16, g // 16 * 16, b // 16 * 16)] += 1
        except Exception:
            continue
    return ["#%02x%02x%02x" % c for c, _ in counts.most_common(limit)]


BRIEF = """# {id} — {what}

**Realm:** {realm}
**Base file (attached):** `{standin}` — vanilla {w}×{h}, mode {mode}
**Output must be exactly {w}×{h} pixels.**
{gridline}

## What to do

Take the attached vanilla PNG as the **base** and rebuild it as *{what}* for
{realm}. It stays the same sheet, the same animation, the same creature-shape
budget — it becomes a different thing wearing that skeleton.

## Rules that cannot be bent

1. **Do not move anything off its frame.** Frame position, alignment, spacing
   and size stay exactly where vanilla put them. The engine reads this file as
   a grid of cells at fixed offsets — a subject drawn 3px left of where vanilla
   drew it renders 3px off in game, every frame, forever.
2. **Same canvas.** Output exactly {w}×{h}. Not "about", not a nicer aspect —
   the size is a contract. If your tool cannot hit it, render an exact integer
   multiple (2×, 3×, 4×) and nothing else; a 1.5× or "roughly 900px" render
   cannot be recovered and will be refused.
3. **Shape may change, within its frame.** A different silhouette, different
   limbs, a different head are all fine and wanted — as long as the subject
   stays inside the cell vanilla used and keeps that cell's ground line. Read
   deviation as "a different animal in the same pose", not "a bigger animal".
4. **No icons.** Draw the sheet only. The inventory/bestiary icon is cut from
   the finished sheet afterwards, automatically — an icon drawn by hand would
   not match the sheet it is supposed to represent.
5. **True pixel art.** 1:1 pixel grid, no anti-aliasing, no gradients, no blur,
   no soft shadows. Flat colour areas, at most four shade steps per material.
   Light from the top-left. **Transparent background** — no backdrop, no frame,
   no drop shadow, no text or labels anywhere in the output.
6. **Few colours.** Shipped Necesse art carries 19–38 distinct colours per
   sheet. Name and hold a palette of 3–5 base colours plus their shades. This
   single rule is what separates a usable sheet from a smooth illustration.

## The realm it has to belong to

{palette}

## How it is checked when it comes back

`tools/asset_intake.py` measures the returned file: exact size or an exact
integer downsample, colour count, whether the background is really
transparent, and per-frame occupancy against the vanilla original. It refuses
what it cannot fix and says why. Nothing reaches the game unreviewed — the
finished sheet goes to the player for approval first, with a 1× preview,
because no measurement can tell whether the art reads.
"""


def prepare(item, outdir):
    os.makedirs(outdir, exist_ok=True)
    src = item["vanilla_path"]
    base = os.path.basename(src)
    shutil.copy2(src, os.path.join(outdir, base))

    w, h = item.get("actual_size") or item.get("declared_size") or (0, 0)
    cell, how = guess_cell(item)
    gridline = ""
    if cell:
        gridline = ("**Frame grid:** %dpx cells — %d columns × %d rows (%s). "
                    "`grid.png` next to this file shows them drawn on the original."
                    % (cell, w // cell, h // cell, how))
        try:
            draw_grid(src, os.path.join(outdir, "grid.png"), cell)
        except Exception as exc:
            gridline += "\n\n*(grid overlay failed: %s)*" % exc

    pal = realm_palette(item["realm"])
    if pal:
        palette = ("**%s already ships these colours** (most used first):\n\n"
                   "`%s`\n\n"
                   "Draw to that list. It is measured off the art this realm already\n"
                   "ships, not copied out of a design document -- so the sprite lands\n"
                   "next to its neighbours instead of next to a description of them."
                   % (item["realm"], "`  `".join(pal)))
    else:
        palette = ("No shipped art was found for **%s** yet, so there is no measured\n"
                   "palette. Read `docs/WORLD_DESIGN.md` §36 for the realm's intent and\n"
                   "name 3-5 exact colours before drawing." % item["realm"])
    palette += "\n\n### Context for the drawing\n\n" + (item.get("notes") or "(none recorded)")
    text = BRIEF.format(id=item["id"], what=item["what"] or item["id"],
                        realm=item["realm"], standin=item["standin"],
                        w=w, h=h, mode=item.get("mode", "?"),
                        gridline=gridline,
                        palette=palette)
    with open(os.path.join(outdir, "brief.md"), "w", encoding="utf-8") as f:
        f.write(text)
    return base, cell


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("ids", nargs="*", help="asset ids from tools/asset_worklist.py")
    ap.add_argument("--limit", type=int, help="prepare the next N open assets")
    ap.add_argument("--realm", help="restrict to one realm")
    ap.add_argument("--out", default=OUT)
    args = ap.parse_args()

    rows, _, _ = build_worklist()
    if args.realm:
        rows = [r for r in rows if args.realm.lower() in (r["realm"] or "").lower()]
    if args.ids:
        byid = {r["id"]: r for r in rows}
        missing = [i for i in args.ids if i not in byid]
        if missing:
            print("not open in the worklist: %s" % ", ".join(missing), file=sys.stderr)
            return 1
        rows = [byid[i] for i in args.ids]
    elif args.limit:
        rows = rows[:args.limit]
    else:
        ap.error("give an id, or --limit N")

    for r in rows:
        if not r.get("vanilla_path"):
            print("skipped %s: no vanilla base" % r["id"], file=sys.stderr)
            continue
        d = os.path.join(args.out, r["id"])
        base, cell = prepare(r, d)
        print("%-18s %s" % (r["id"], os.path.relpath(d, REPO)))
        print("    attach: %s   cell: %s" % (base, cell or "unknown"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
