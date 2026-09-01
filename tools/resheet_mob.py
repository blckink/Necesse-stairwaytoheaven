#!/usr/bin/env python3
"""Repack a loosely-laid-out mob sheet onto the exact 384x320 engine grid.

A generated mob sheet arrives at some arbitrary size with its rows roughly, but
not exactly, where the engine wants them: uneven row heights, sprites drifting
left and right, a background that is black or a painted fog instead of alpha.
Nudging 24 cells by hand in Photoshop is the job this replaces.

The method is not a resize. A resize squashes the whole picture and lands
nothing on the grid. Instead:

  1. KEY the background. Alpha if the file has any; otherwise flood from the
     border through near-background colour, so an interior black eye survives
     while the surrounding black goes.
  2. FIND THE ROWS by the horizontal projection of opaque pixels: contiguous
     bands of non-empty scanlines, gaps between them. Row heights may differ --
     that is the whole point.
  3. FIND THE SPRITES IN EACH ROW the same way, by vertical projection inside
     the band, so uneven horizontal spacing does not matter either.
  4. PLACE each sprite in its own cell of the target grid, scaled by ONE shared
     factor (so the four directions stay the same animal), centred
     horizontally, and sitting on a common baseline -- feet at the bottom,
     which is what the engine anchors on.

The shared scale is deliberate. Scaling each sprite to fit its own cell is what
makes a walk cycle wobble: the mob appears to breathe as it steps.

    python3 tools/resheet_mob.py IN.png -o mobs/nimbusyak.png
    python3 tools/resheet_mob.py IN.png --inspect     # report rows/cols, write nothing

Verify the result with the preview it writes, then ship it through
`tools/asset_intake.py`.
"""
import argparse
import collections
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CELL = 64
COLS, ROWS = 6, 4
TARGET = (CELL * COLS, CELL * ROWS + 64)      # 384x320: grid + gib strip + pad


def key_background(im, tol=42):
    """Return a copy with the background made transparent.

    Uses the file's own alpha when it has some. Otherwise floods inward from
    the border through pixels within `tol` of the border colour -- a flood, not
    a global colour test, so a black eye or a dark hoof INSIDE the sprite is
    kept while the black around it goes.
    """
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    if sum(1 for p in im.get_flattened_data() if p[3] < 250) > w * h * 0.02:
        return im, "kept the file's own alpha"

    border = collections.Counter()
    for x in range(w):
        border[px[x, 0][:3]] += 1
        border[px[x, h - 1][:3]] += 1
    for y in range(h):
        border[px[0, y][:3]] += 1
        border[px[w - 1, y][:3]] += 1
    bg = border.most_common(1)[0][0]

    def near(p):
        return max(abs(p[i] - bg[i]) for i in range(3)) <= tol

    seen = bytearray(w * h)
    stack = [(x, y) for x in range(w) for y in (0, h - 1)]
    stack += [(x, y) for y in range(h) for x in (0, w - 1)]
    while stack:
        x, y = stack.pop()
        if not (0 <= x < w and 0 <= y < h) or seen[y * w + x]:
            continue
        if not near(px[x, y]):
            continue
        seen[y * w + x] = 1
        stack.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))
    out = im.copy()
    op = out.load()
    n = 0
    for y in range(h):
        for x in range(w):
            if seen[y * w + x]:
                op[x, y] = (0, 0, 0, 0)
                n += 1
    return out, "keyed background %s, %d px removed" % (bg, n)


def bands(mask, length, gap_min=2, run_min=3):
    """Contiguous runs of True in a boolean projection -> [(start, end)]."""
    out = []
    i = 0
    while i < length:
        if not mask[i]:
            i += 1
            continue
        j = i
        while j < length:
            if mask[j]:
                j += 1
                continue
            k = j
            while k < length and not mask[k]:
                k += 1
            if k - j >= gap_min:
                break
            j = k
        if j - i >= run_min:
            out.append((i, j))
        i = max(j, i + 1)
    return out


def projections(im, box=None, alpha=40):
    """(row occupancy, col occupancy) of opaque pixels."""
    px = im.load()
    x0, y0, x1, y1 = box or (0, 0, im.width, im.height)
    rows = [any(px[x, y][3] >= alpha for x in range(x0, x1)) for y in range(y0, y1)]
    cols = [any(px[x, y][3] >= alpha for y in range(y0, y1)) for x in range(x0, x1)]
    return rows, cols


def opaque_bbox(im, box, alpha=40):
    px = im.load()
    x0, y0, x1, y1 = box
    xs, ys = [], []
    for y in range(y0, y1):
        for x in range(x0, x1):
            if px[x, y][3] >= alpha:
                xs.append(x)
                ys.append(y)
    if not xs:
        return None
    return (min(xs), min(ys), max(xs) + 1, max(ys) + 1)


def split_even(lo, hi, n):
    """n equal slices of [lo, hi) -> [(start, end)]."""
    step = (hi - lo) / float(n)
    return [(int(round(lo + i * step)), int(round(lo + (i + 1) * step)))
            for i in range(n)]


def analyse(im, want_rows=ROWS, cols=COLS, alpha=40):
    """-> ([[bbox per cell] per row], note).

    Detection first, EVEN SPLIT as the fallback -- and the fallback is the
    common case, not the exception. Generated sheets routinely have their side
    views touching each other (the projection then reads one wide blob) or
    their rows overlapping through a soft glow (the projection reads one tall
    band). Both are fine to cut evenly, because the layout IS regular; only the
    exact boundaries are not.
    """
    notes = []
    rows_mask, _ = projections(im, alpha=alpha)
    row_bands = bands(rows_mask, im.height, gap_min=4, run_min=8)
    # drop the gib strip and anything else short: the animation rows are the
    # tall ones, and there are want_rows of them.
    tall = [b for b in row_bands if (b[1] - b[0]) >= 0.5 * max(
        (x[1] - x[0]) for x in row_bands)] if row_bands else []
    if len(tall) == want_rows:
        use_rows = tall
        notes.append("rows detected")
    else:
        bb = opaque_bbox(im, (0, 0, im.width, im.height), alpha)
        if bb is None:
            return [], "nothing opaque"
        top = tall[0][0] if tall else bb[1]
        # the animation block ends where the short strip begins, if there is one
        short = [b for b in row_bands if b not in tall and b[0] > top]
        bottom = short[0][0] if short else bb[3]
        use_rows = split_even(top, bottom, want_rows)
        notes.append("rows split evenly (%d bands found, wanted %d)"
                     % (len(row_bands), want_rows))

    out = []
    even_cols = 0
    for (ry0, ry1) in use_rows:
        _, cols_mask = projections(im, (0, ry0, im.width, ry1), alpha)
        col_bands = bands(cols_mask, im.width, gap_min=3, run_min=6)
        if len(col_bands) != cols:
            bb = opaque_bbox(im, (0, ry0, im.width, ry1), alpha)
            if bb is None:
                continue
            col_bands = split_even(bb[0], bb[2], cols)
            even_cols += 1
        cells = []
        for (cx0, cx1) in col_bands[:cols]:
            cells.append(opaque_bbox(im, (cx0, ry0, cx1, ry1), alpha)
                         or (cx0, ry0, cx1, ry1))
        out.append(cells)
    if even_cols:
        notes.append("%d row(s) split into %d even columns" % (even_cols, cols))
    return out, "; ".join(notes)


def resheet(im, rows, cell=CELL, cols=COLS, want_rows=ROWS, headroom=0.92):
    """Place the first `want_rows` x `cols` sprites into the target grid.

    ONE scale factor for every sprite, derived from the largest one, so the
    four directions stay the same animal and the walk cycle does not breathe.
    Centred horizontally, feet on a common baseline near the bottom of the cell.
    """
    usable = rows[:want_rows]
    if len(usable) < want_rows:
        return None, ("found %d rows, need %d -- check --inspect"
                      % (len(usable), want_rows))
    widest = max(b[2] - b[0] for r in usable for b in r[:cols])
    tallest = max(b[3] - b[1] for r in usable for b in r[:cols])
    scale = min(cell * headroom / widest, cell * headroom / tallest)

    sheet = Image.new("RGBA", TARGET, (0, 0, 0, 0))
    placed = 0
    for ri, row in enumerate(usable):
        for ci, bb in enumerate(row[:cols]):
            sprite = im.crop(bb)
            nw = max(1, round(sprite.width * scale))
            nh = max(1, round(sprite.height * scale))
            sprite = sprite.resize((nw, nh), Image.LANCZOS)
            dx = ci * cell + (cell - nw) // 2
            dy = ri * cell + (cell - nh) - 2        # feet near the cell's floor
            sheet.alpha_composite(sprite, (dx, max(ri * cell, dy)))
            placed += 1
    return sheet, ("placed %d sprites, one shared scale %.3f (source cells up "
                   "to %dx%d)" % (placed, scale, widest, tallest))


def harden_alpha(im, threshold=110):
    """Necesse sprites use hard 0/255 alpha; a soft edge reads as a halo."""
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255) if a >= threshold else (0, 0, 0, 0)
    return im


def preview(im, label, path, zoom=3):
    from PIL import ImageDraw, ImageFont
    try:
        font = ImageFont.truetype(
            "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf", 14)
    except OSError:
        font = ImageFont.load_default()
    w, h = im.size
    zw, zh = w * zoom, h * zoom
    out = Image.new("RGBA", (zw * 2 + 36, zh + 44 + h), (24, 24, 30, 255))
    for i, ground in enumerate(((44, 44, 54, 255), (206, 210, 216, 255))):
        panel = Image.new("RGBA", (zw, zh), ground)
        # the engine's 64px cell grid, so a misplaced sprite is obvious
        pd = ImageDraw.Draw(panel)
        for c in range(1, COLS):
            pd.line((c * CELL * zoom, 0, c * CELL * zoom, zh), fill=(255, 255, 255, 60))
        for r in range(1, ROWS + 1):
            pd.line((0, r * CELL * zoom, zw, r * CELL * zoom), fill=(255, 255, 255, 60))
        panel.alpha_composite(im.resize((zw, zh), Image.NEAREST))
        out.alpha_composite(panel, (12 + i * (zw + 12), 32))
    out.alpha_composite(im, (12, 36 + zh))
    ImageDraw.Draw(out).text((12, 8), "%s  %dx%d  3x on dark / light with the "
                             "64px grid, then 1x" % (label, w, h),
                             fill=(235, 235, 240), font=font)
    out.save(path)
    return path


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("src")
    ap.add_argument("-o", "--out", help="write the 384x320 sheet here")
    ap.add_argument("--inspect", action="store_true",
                    help="report the detected rows and sprites, write nothing")
    ap.add_argument("--tol", type=int, default=42,
                    help="background keying tolerance (default 42)")
    ap.add_argument("--alpha", type=int, default=40,
                    help="alpha above which a pixel counts as art (default 40; "
                         "raise it for art painted over a soft glow)")
    args = ap.parse_args()

    im = Image.open(args.src).convert("RGBA")
    keyed, note = key_background(im, args.tol)
    print("%s  %dx%d" % (os.path.basename(args.src), im.width, im.height))
    print("  %s" % note)

    rows, how = analyse(keyed, alpha=args.alpha)
    print("  %s" % how)
    for i, r in enumerate(rows):
        hs = [b[3] - b[1] for b in r]
        print("  row %d: %2d sprites, heights %d-%d" % (i, len(r), min(hs), max(hs)))
    if args.inspect:
        return 0

    sheet, msg = resheet(keyed, rows)
    print("  %s" % msg)
    if sheet is None:
        return 1
    sheet = harden_alpha(sheet)

    qa = os.path.join(REPO, "build", "qa", "resheet")
    os.makedirs(qa, exist_ok=True)
    stem = os.path.splitext(os.path.basename(args.src))[0]
    print("  preview %s" % os.path.relpath(
        preview(sheet, stem, os.path.join(qa, stem + "_preview.png")), REPO))
    if args.out:
        dest = args.out if os.path.isabs(args.out) else os.path.join(
            REPO, "src", "main", "resources", args.out)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        sheet.save(dest)
        print("  wrote %s" % dest)
    else:
        staged = os.path.join(qa, stem + "_384x320.png")
        sheet.save(staged)
        print("  staged %s  (-o to place it)" % os.path.relpath(staged, REPO))
    return 0


if __name__ == "__main__":
    sys.exit(main())
