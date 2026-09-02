#!/usr/bin/env python3
"""Make a hand-drawn splat obey vanilla's three texture rules. No AI, no guessing.

`tools/tile_behaviour_audit.py` measures exactly three numbers per splat cell,
and every one of them is arithmetic on the pixels:

  density    how many pixels differ from the cell's MODAL colour
  mean|dRGB| the average max-channel distance of those pixels from the modal
  coherence  the share of aligned 2x2 blocks whose four pixels are identical

Vanilla scores 100% coherence on every splat in the game, because vanilla's
tone unit is a 2x2 block and never a lone pixel. A generated or hand-painted
texture scores 1-20%, which is what makes it read as noise at 1x however good
it looks zoomed in.

So this tool is three deterministic passes, in this order:

  1. SNAP     each aligned 2x2 block becomes its own dominant colour.
              Coherence goes to 100% by construction.
  2. QUIETEN  every colour is pulled toward the modal until its max-channel
              distance is within the band. Hue is preserved -- the pull is
              along the vector to the modal, so a green stays green and only
              its contrast drops.
  3. THIN     if density is still over the band, the pixels CLOSEST to the
              modal are flattened onto it, 2x2 block at a time, quietest
              first, until it lands. Loud detail survives; near-invisible
              speckle is what goes.

The alpha channel is never touched. A splat's alpha is its marching-square
shape and the engine reads it -- changing one pixel of it breaks the tiling.

    python3 tools/fix_splat.py src/main/resources/tiles/cloudturf_splat.png
    python3 tools/fix_splat.py tiles/*.png --apply
    python3 tools/fix_splat.py X.png --preview build/qa/x.png

Without --apply nothing is written except previews: it prints the before and
after numbers so you can see what it would do.
"""
import argparse
import collections
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# The bands tools/tile_behaviour_audit.py enforces, measured off vanilla.
BANDS = {
    "terrain": (280, 620, 14.0),
    "floor": (440, 780, 24.0),
}
CELL_W, CELL_H = 224, 96


def measure(cell):
    """density, mean |dRGB|, 2x2 coherence -- the audit's own three numbers."""
    px = list(cell.getdata())
    counts = collections.Counter(px)
    modal, modal_n = counts.most_common(1)[0]
    dev = [max(abs(a - b) for a, b in zip(c, modal)) for c in px if c != modal]
    load = cell.load()
    w, h = cell.size
    blocks = ok = 0
    for by in range(0, h - 1, 2):
        for bx in range(0, w - 1, 2):
            blocks += 1
            if len({load[bx, by], load[bx + 1, by],
                    load[bx, by + 1], load[bx + 1, by + 1]}) == 1:
                ok += 1
    return (len(px) - modal_n,
            sum(dev) / len(dev) if dev else 0.0,
            ok / blocks * 100 if blocks else 100.0,
            modal)


def snap_blocks(im):
    """Pass 1 -- every aligned 2x2 block becomes its own dominant colour.

    Ties go to the top-left pixel, which keeps a diagonal edge leaning the way
    it was drawn instead of jittering. Alpha rides along with the colour it
    belongs to, so a block that straddles the shape edge keeps a real edge
    pixel rather than inventing a blend.
    """
    px = im.load()
    w, h = im.size
    for by in range(0, h - 1, 2):
        for bx in range(0, w - 1, 2):
            quad = [px[bx, by], px[bx + 1, by], px[bx, by + 1], px[bx + 1, by + 1]]
            counts = collections.Counter(quad)
            best_n = max(counts.values())
            winner = next(c for c in quad if counts[c] == best_n)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                px[bx + dx, by + dy] = winner
    return im


def quieten(im, modal, mean_max):
    """Pass 2 -- pull every colour toward the modal until the mean is in band.

    The pull is a straight lerp along the vector to the modal, so hue and the
    direction of every shade are preserved; only the contrast comes down. The
    factor is found by bisection rather than assumed, because the mean is over
    the non-modal pixels only and does not scale linearly with the lerp.
    """
    rgb = [(p[0], p[1], p[2]) for p in im.getdata()]
    alpha = [p[3] for p in im.getdata()]
    mr, mg, mb = modal[0], modal[1], modal[2]

    def blend(f):
        return [(round(r + (mr - r) * f), round(g + (mg - g) * f),
                 round(b + (mb - b) * f)) for (r, g, b) in rgb]

    def mean_of(cols):
        dev = [max(abs(c[0] - mr), abs(c[1] - mg), abs(c[2] - mb))
               for c in cols if c != (mr, mg, mb)]
        return sum(dev) / len(dev) if dev else 0.0

    if mean_of(rgb) <= mean_max:
        return im, 0.0
    lo, hi = 0.0, 1.0
    for _ in range(24):
        mid = (lo + hi) / 2
        if mean_of(blend(mid)) > mean_max:
            lo = mid
        else:
            hi = mid
    out = blend(hi)
    im.putdata([(c[0], c[1], c[2], a) for c, a in zip(out, alpha)])
    return im, hi


def thin(im, modal, density_hi):
    """Pass 3 -- flatten the QUIETEST 2x2 blocks onto the modal, until in band.

    Sorted by distance from the modal so the faintest speckle goes first and
    the drawn detail is the last thing to be touched. Whole blocks only, or
    pass 1's coherence would be undone.
    """
    px = im.load()
    w, h = im.size
    mr, mg, mb = modal[0], modal[1], modal[2]
    blocks = []
    for by in range(0, h - 1, 2):
        for bx in range(0, w - 1, 2):
            c = px[bx, by]
            if (c[0], c[1], c[2]) == (mr, mg, mb):
                continue
            d = max(abs(c[0] - mr), abs(c[1] - mg), abs(c[2] - mb))
            blocks.append((d, bx, by))
    blocks.sort()
    # Density is counted, not re-measured: flattening one 2x2 block removes
    # exactly its four pixels from the non-modal count. Re-running the full
    # measure inside this loop made the pass quadratic and it timed out on a
    # 224x576 sheet.
    density = measure(im.convert("RGB"))[0]
    for _, bx, by in blocks:
        if density <= density_hi:
            break
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            a = px[bx + dx, by + dy][3]
            px[bx + dx, by + dy] = (mr, mg, mb, a)
        density -= 4
    return im


def cells(im):
    """EXACTLY the cells tools/tile_behaviour_audit.py measures.

    Not the whole sheet, and not the whole variant band: the audit reads
    `img.crop((c * 32, 0, c * 32 + 32, 32))` for c in 3..6 -- the four
    FULL-TILE variant cells on the top row (tile_behaviour_audit.py:539-540).
    Those four are the only fully opaque cells in a splat; every other cell is
    a marching-square edge, part transparent, and measuring one would count the
    hole as a colour and report a mean |dRGB| in the hundreds. Measuring
    anything else means the numbers here cannot be compared with the gate's.
    """
    if im.width < 7 * 32:
        return [im]
    return [im.crop((c * 32, 0, c * 32 + 32, 32)) for c in range(3, 7)]


def report(im, label):
    rows = [measure(c.convert("RGB")) for c in cells(im)]
    d = sum(r[0] for r in rows) / len(rows)   # the audit averages density
    m = max(r[1] for r in rows)               # and takes the worst of the rest
    c = min(r[2] for r in rows)
    print("  %-8s density %4.0f   mean|dRGB| %5.1f   2x2 coherence %5.1f%%"
          % (label, d, m, c))
    return d, m, c


def fix(path, kind, apply_it, preview_path, quieten_it=False):
    im = Image.open(path).convert("RGBA")
    print(os.path.basename(path))
    before = report(im, "before")
    original = im.copy()

    lo, hi, mean_max = BANDS[kind]
    out = snap_blocks(im.copy())
    if not quieten_it:
        # SNAP ONLY, and this is the default on purpose. Pass 1 is the real
        # Necesse convention and it costs the art almost nothing: it only
        # decides which of four already-adjacent colours a 2x2 block keeps.
        #
        # Passes 2 and 3 are a different matter. On cloudturf the audit's
        # mean |dRGB| <= 14 band needed a 90% pull toward the ground tone, and
        # that erased the snow caps and the grass blades outright -- the sheet
        # came out flat. Vanilla carries its grass in five RGB levels; a
        # painterly sheet does not, and forcing it to is not a fix, it is a
        # deletion. So the loudness band is a JUDGEMENT the player makes on the
        # 1x preview, not something this tool applies behind their back.
        after = report(out, "after")
        if preview_path:
            os.makedirs(os.path.dirname(preview_path), exist_ok=True)
            side_by_side(original, out, preview_path)
            print("  preview %s" % os.path.relpath(preview_path, REPO))
        if apply_it:
            out.save(path)
            print("  WROTE %s" % os.path.relpath(path, REPO))
        return before, after
    # The modal is re-read AFTER snapping: snapping changes which colour is
    # most common, and quietening toward the old modal would pull the sheet
    # toward a colour that is no longer its ground tone.
    modal = measure(cells(out)[0].convert("RGB"))[3]
    out, factor = quieten(out, modal, mean_max)
    out = snap_blocks(out)          # the lerp can round two block members apart
    # thin() works on the whole sheet, the audit measures per cell, so the
    # budget is scaled by the cell count or a tall sheet would be stripped bare.
    # thin() works on the whole sheet; the audit averages four 32x32 cells, so
    # the budget is scaled by how many such cells the sheet holds.
    budget = hi * (out.width // 32) * (out.height // 32)
    if report(out, "mid")[0] > hi:
        out = thin(out, modal, budget)

    after = report(out, "after")
    if factor:
        print("  contrast pulled %.0f%% toward the ground tone" % (factor * 100))

    if preview_path:
        os.makedirs(os.path.dirname(preview_path), exist_ok=True)
        side_by_side(original, out, preview_path)
        print("  preview %s" % os.path.relpath(preview_path, REPO))
    if apply_it:
        out.save(path)
        print("  WROTE %s" % os.path.relpath(path, REPO))
    return before, after


def side_by_side(a, b, path, zoom=3):
    """Before | after, at 1x and at `zoom`, on a mid grey the eye can judge."""
    cw = min(a.width, CELL_W)
    ch = min(a.height, CELL_H * 2)
    ac, bc = a.crop((0, 0, cw, ch)), b.crop((0, 0, cw, ch))
    big = (cw * zoom, ch * zoom)
    canvas = Image.new("RGBA", (cw * 2 + 24 + big[0] * 2 + 24, max(ch, big[1]) + 8),
                       (110, 110, 115, 255))
    x = 4
    for im in (ac, bc):
        canvas.alpha_composite(im, (x, 4))
        x += cw + 8
    x += 16
    for im in (ac, bc):
        canvas.alpha_composite(im.resize(big, Image.NEAREST), (x, 4))
        x += big[0] + 8
    canvas.save(path)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("paths", nargs="+", help="splat PNGs to check or fix")
    ap.add_argument("--kind", choices=sorted(BANDS), default="terrain",
                    help="which vanilla band to hold to (default terrain)")
    ap.add_argument("--apply", action="store_true",
                    help="write the fixed file back; without it nothing is written")
    ap.add_argument("--quieten", action="store_true",
                    help="ALSO pull contrast down into the audit's loudness band "
                         "and thin the density. Off by default: on a painterly "
                         "sheet this flattens the art -- look at the preview "
                         "before you trust it")
    ap.add_argument("--preview", default=None,
                    help="write a before/after PNG here (a directory for many files)")
    args = ap.parse_args()

    failed = 0
    for p in args.paths:
        if not os.path.exists(p):
            print("%s: not found" % p)
            failed += 1
            continue
        prev = None
        if args.preview:
            prev = (os.path.join(args.preview,
                                 os.path.splitext(os.path.basename(p))[0] + "_fix.png")
                    if len(args.paths) > 1 or os.path.isdir(args.preview)
                    else args.preview)
        fix(p, args.kind, args.apply, prev, args.quieten)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
