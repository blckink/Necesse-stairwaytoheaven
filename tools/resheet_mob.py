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


def label_components(im, alpha=40):
    """Connected components of the opaque pixels -> (labels, count).

    numpy-only iterative flood fill (scipy is not a dependency here). 8-way,
    because a sprite's outline diagonals must not split it into pieces.
    """
    import numpy as np
    a = np.array(im)[:, :, 3] >= alpha
    h, w = a.shape
    labels = np.zeros((h, w), dtype=np.int32)
    n = 0
    ys, xs = np.nonzero(a)
    for sy, sx in zip(ys, xs):
        if labels[sy, sx]:
            continue
        n += 1
        stack = [(sy, sx)]
        labels[sy, sx] = n
        while stack:
            y, x = stack.pop()
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    ny, nx = y + dy, x + dx
                    if 0 <= ny < h and 0 <= nx < w and a[ny, nx] and not labels[ny, nx]:
                        labels[ny, nx] = n
                        stack.append((ny, nx))
    return labels, n


def isolate_rows(im, row_spans, alpha=40):
    """One image per row band, holding ONLY the blobs that belong to it.

    A generated sheet's rows overlap: the wraith's smoke trails from row 0 hang
    down into row 1, and row 2's legs poke up into it. A rectangular crop takes
    the neighbours with it, and the player caught exactly that -- "in zeile 2
    siehst du, dass dann von zeile 1 und zeile 3 der untere und obere teil mit
    reinragen, die man entfernen muss eigentlich".

    So a blob is assigned to whichever band holds the MOST of its pixels, and a
    band's image is drawn from its own blobs alone. A trail that genuinely
    belongs to its sprite stays attached (it is the same blob); a stray piece
    from the row above is a separate blob and goes to the row above.
    """
    import numpy as np
    labels, n = label_components(im, alpha)
    if n == 0:
        return [im.copy() for _ in row_spans], 0
    owner = {}
    for lab in range(1, n + 1):
        ys = np.nonzero((labels == lab).any(axis=1))[0]
        if not len(ys):
            continue
        best, best_n = 0, -1
        for ri, (y0, y1) in enumerate(row_spans):
            c = int(((ys >= y0) & (ys < y1)).sum())
            if c > best_n:
                best, best_n = ri, c
        owner[lab] = best
    src = np.array(im)
    outs, moved = [], 0
    for ri in range(len(row_spans)):
        mine = np.isin(labels, [l for l, o in owner.items() if o == ri])
        buf = src.copy()
        buf[~mine] = 0
        outs.append(Image.fromarray(buf, "RGBA"))
        moved += sum(1 for l, o in owner.items()
                     if o == ri and not _within(labels, l, row_spans[ri]))
    return outs, moved


def _within(labels, lab, span):
    import numpy as np
    ys = np.nonzero((labels == lab).any(axis=1))[0]
    return len(ys) and ys.min() >= span[0] and ys.max() < span[1]


def split_even(lo, hi, n):
    """n equal slices of [lo, hi) -> [(start, end)]."""
    step = (hi - lo) / float(n)
    return [(int(round(lo + i * step)), int(round(lo + (i + 1) * step)))
            for i in range(n)]


def analyse(im, want_rows=ROWS, cols=COLS, alpha=40, force_rows=None):
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
    if force_rows:
        # The escape hatch, and some sheets need it. Detection assumes the four
        # animation rows are the tall bands and anything below them is the gib
        # strip. A sheet whose bottom row holds EXTRA POSES (the blue yak ships
        # sleeping and lying frames down there, at full size) breaks that
        # assumption: the tall-band test counts five, the even split then spans
        # the wrong range and folds two directions into one band. Rather than
        # guess harder, --rows takes the boundaries from whoever can see them.
        use_rows = [(force_rows[i], force_rows[i + 1])
                    for i in range(len(force_rows) - 1)][:want_rows]
        notes.append("rows given explicitly")
    elif True:
        # drop the gib strip and anything else short: the animation rows are
        # the tall ones, and there are want_rows of them.
        # 0.6, not 0.5: the blue yak's bottom strip is 220px because it holds
        # extra poses at full size, against 432px animation bands. At 0.5 that
        # strip counted as an animation row and the split came out 3+1 instead
        # of 2+2. 0.6 keeps it out while still admitting a genuinely short
        # direction row.
        tall = [b for b in row_bands if (b[1] - b[0]) >= 0.6 * max(
            (x[1] - x[0]) for x in row_bands)] if row_bands else []
        if len(tall) == want_rows:
            use_rows = tall
            notes.append("rows detected")
        else:
            bb = opaque_bbox(im, (0, 0, im.width, im.height), alpha)
            if bb is None:
                return [], "nothing opaque", [], []
            # Split each TALL BAND by its own height, not the whole block
            # evenly. Two directions whose art touches read as one band twice
            # the height of a single row, and splitting the block evenly then
            # folds a direction into its neighbour -- measured on the blue yak,
            # whose two bands are 432 and 416 px against a 212 px mean row.
            # Dividing each band by that mean recovers 2 + 2 exactly.
            if tall and len(tall) < want_rows:
                total = sum(b[1] - b[0] for b in tall)
                mean = total / float(want_rows)
                use_rows, left_over = [], want_rows
                for i, b in enumerate(tall):
                    n = (left_over if i == len(tall) - 1
                         else max(1, min(left_over - (len(tall) - 1 - i),
                                         int(round((b[1] - b[0]) / mean)))))
                    use_rows += split_even(b[0], b[1], n)
                    left_over -= n
                use_rows = use_rows[:want_rows]
                notes.append("%d band(s) split by height into %d rows"
                             % (len(tall), len(use_rows)))
            else:
                top = tall[0][0] if tall else bb[1]
                short = [b for b in row_bands if b not in tall and b[0] > top]
                bottom = short[0][0] if short else bb[3]
                use_rows = split_even(top, bottom, want_rows)
                notes.append("rows split evenly (%d bands found, wanted %d) -- "
                             "if a direction looks folded into another, pass "
                             "--rows" % (len(row_bands), want_rows))

    out = []
    even_cols = 0
    for (ry0, ry1) in use_rows:
        _, cols_mask = projections(im, (0, ry0, im.width, ry1), alpha)
        col_bands = bands(cols_mask, im.width, gap_min=3, run_min=6)
        # A band far wider than its siblings is not one sprite, it is two that
        # touch. Measured on the blue yak: one "frame" came out 419 px against
        # the row's ~216 median, i.e. two fused, which then blew the scale.
        if len(col_bands) == cols:
            widths = sorted(b[1] - b[0] for b in col_bands)
            median = widths[len(widths) // 2]
            if widths[-1] > 1.5 * median:
                col_bands = []
        if len(col_bands) != cols:
            bb = opaque_bbox(im, (0, ry0, im.width, ry1), alpha)
            if bb is None:
                continue
            col_bands = split_even(bb[0], bb[2], cols)
            even_cols += 1
        cells = []
        for (cx0, cx1) in col_bands[:cols]:
            bb = opaque_bbox(im, (cx0, ry0, cx1, ry1), alpha) or (cx0, ry0, cx1, ry1)
            # opaque_bbox already searches inside the column, so this only
            # documents the invariant the scale depends on: a cell is never
            # wider than its column.
            cells.append((max(bb[0], cx0), bb[1], min(bb[2], cx1), bb[3]))
        out.append(cells)
    # The gib strip: the SHORT band below the animation rows, which the row
    # pass deliberately excluded. Its chunks are small and well separated, so
    # plain detection is enough.
    gib = []
    if use_rows:
        after = max(r[1] for r in use_rows)
        for (gy0, gy1) in [b for b in row_bands if b[0] >= after]:
            _, gm = projections(im, (0, gy0, im.width, gy1), alpha)
            for (gx0, gx1) in bands(gm, im.width, gap_min=3, run_min=4):
                bb = opaque_bbox(im, (gx0, gy0, gx1, gy1), alpha)
                if bb:
                    gib.append(bb)
    if even_cols:
        notes.append("%d row(s) split into %d even columns" % (even_cols, cols))
    if gib:
        notes.append("%d gib chunk(s) found" % len(gib))
    return out, "; ".join(notes), gib, list(use_rows)


def mirror_row(sheet, src_row, dst_row, cell=CELL, cols=COLS):
    """Copy one direction row onto another, flipping each CELL on its own.

    The player's own fix: "ich habe die 4. zeile von der 2. einfach kopiert und
    gespiegelt, da die andere nicht einheitlich war sonst". A generator draws
    the two side views independently and they come out as two different animals
    -- different bell, different leg timing -- which reads as a glitch in game
    because the player sees them one after the other when they turn around.

    Each cell is flipped separately, never the whole strip: flipping the strip
    would also reverse the COLUMN order, putting the idle pose in column 5.

    The cost is real and worth naming: a mirrored row has its light coming from
    the wrong side. Vanilla accepts that on plenty of mobs; if the shading is
    strong enough to notice, draw the row instead.
    """
    band = sheet.crop((0, src_row * cell, cols * cell, src_row * cell + cell))
    out = Image.new("RGBA", (cols * cell, cell), (0, 0, 0, 0))
    for c in range(cols):
        one = band.crop((c * cell, 0, c * cell + cell, cell))
        out.alpha_composite(one.transpose(Image.FLIP_LEFT_RIGHT), (c * cell, 0))
    sheet.paste((0, 0, 0, 0), (0, dst_row * cell, cols * cell, dst_row * cell + cell))
    sheet.alpha_composite(out, (0, dst_row * cell))
    return sheet


def resheet(im, rows, cell=CELL, cols=COLS, want_rows=ROWS, gib=None,
            row_spans=None, alpha=40, isolate=True):
    """Scale the WHOLE sheet once so its content spans exactly cols*cell, then
    slide each row down into its own band.

    Two separate decisions, and the player corrected me on both in turn.

    SCALE is global: one factor from the full content width, never per sprite.
    My first version fitted each sprite to its cell and came out ~20% too large
    (0.353 against the correct 0.288 on the calf). One factor is also what keeps
    the four directions the same animal. Measured on the calf, the four rows
    individually want 0.2995 / 0.2927 / 0.3048 / 0.2931 against the whole
    image's 0.2876 -- close enough that one number serves.

    PLACEMENT is per frame: "du musst eigentlich jeden einzelnen frame
    ausschneiden und dann einfach ausrichten horizontal und vertikal im
    jeweiligen 64x64px ausschnitt". My second version placed the whole row as
    one strip to preserve each frame's offset, on the theory that those offsets
    are the walk cycle. On hand-drawn art they would be; on a generated sheet
    they are the generator's spacing accidents, and keeping them leaves frames
    straddling cell borders. So each frame is cut out and centred in its own
    cell.

    Vertical is NOT per frame, and that is the one thing not to simplify: every
    frame keeps its height above the ROW's floor line. Snapping each frame to
    its own cell floor would drop a lifted hoof back to the ground and kill the
    step.
    """
    usable = rows[:want_rows]
    if len(usable) < want_rows:
        return None, ("found %d rows, need %d -- check --inspect"
                      % (len(usable), want_rows))

    left = min(b[0] for r in usable for b in r)
    right = max(b[2] for r in usable for b in r)
    span_scale = (cols * cell) / float(right - left)

    # The span rule alone does NOT guarantee a frame fits its cell, and a frame
    # that does not fit is not merely cropped in the preview -- the engine draws
    # sprite(col, row, 64), so anything past the cell edge is not on the sheet
    # at all. Measured at the span scale: calf 47px (fine), wraith 70px,
    # flowers 64x66, blue yak 115px. So the widest and tallest frame cap it.
    wmax = max(b[2] - b[0] for r in usable for b in r)
    hmax = max(b[3] - b[1] for r in usable for b in r)
    fit_scale = min(cell / float(wmax), cell / float(hmax))
    scale = min(span_scale, fit_scale)
    capped = scale < span_scale - 1e-6

    # Cut the rows apart by BLOB OWNERSHIP before cropping, so a neighbour's
    # overhang does not ride along in the rectangle.
    layers = None
    if isolate and row_spans and len(row_spans) >= want_rows:
        layers, _ = isolate_rows(im, row_spans[:want_rows], alpha)

    sheet = Image.new("RGBA", TARGET, (0, 0, 0, 0))
    for ri, row in enumerate(usable):
        src = layers[ri] if layers else im
        # The row's own floor: every frame keeps its height ABOVE this line, so
        # a lifted hoof stays lifted instead of dropping to the cell floor.
        floor = max(b[3] for b in row)
        for ci, bb in enumerate(row[:cols]):
            frame = src.crop(bb)
            nw = max(1, round(frame.width * scale))
            nh = max(1, round(frame.height * scale))
            frame = frame.resize((nw, nh), Image.LANCZOS)
            # Horizontal: centred in its OWN cell -- the player's instruction,
            # and right for a generated sheet, where the gaps between frames are
            # accidents of the generator rather than animation.
            dx = ci * cell + (cell - nw) // 2
            # Vertical: the frame's distance from the row's floor, preserved.
            lift = round((floor - bb[3]) * scale)
            dy = ri * cell + cell - nh - lift
            dy = max(ri * cell, min(dy, ri * cell + cell - nh))
            sheet.alpha_composite(frame, (dx, dy))

    note = "scale %.4f" % scale
    note += (" (capped so the widest %dpx frame fits a %dpx cell; the span rule "
             "wanted %.4f)" % (wmax, cell, span_scale)) if capped else (
             " from the full %dpx content width" % (right - left))
    if layers:
        note += "; rows isolated by blob ownership"

    # The gib strip: 32px cells at y256, up to five of them (FleshParticle
    # reads sprite row 8). Generators put it under the animation rows; it is
    # placed at its own scale because it is not part of the walk.
    if gib:
        gleft = min(b[0] for b in gib)
        gtop = min(b[1] for b in gib)
        gbot = max(b[3] for b in gib)
        gscale = min(32.0 / max(1, gbot - gtop),
                     32.0 / max(b[2] - b[0] for b in gib))
        for i, bb in enumerate(gib[:5]):
            chunk = im.crop(bb)
            nw = max(1, round(chunk.width * gscale))
            nh = max(1, round(chunk.height * gscale))
            chunk = chunk.resize((nw, nh), Image.LANCZOS)
            sheet.alpha_composite(chunk, (i * 32 + (32 - nw) // 2,
                                          256 + (32 - nh) // 2))
        note += "; %d gib chunk(s) at y256" % len(gib[:5])
    return sheet, note


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
    ap.add_argument("--rows", default=None,
                    help="explicit row boundaries as y0,y1,y2,y3,y4 (5 numbers "
                         "for 4 rows). Use when --inspect shows a direction "
                         "folded into another band.")
    ap.add_argument("--no-isolate", action="store_true",
                    help="skip blob-ownership row separation (faster; only "
                         "safe when the rows genuinely do not overlap)")
    ap.add_argument("--mirror-left", action="store_true",
                    help="build the LEFT row by mirroring RIGHT cell by cell, "
                         "when the two side views did not come out as the same "
                         "animal (flips the light direction -- see mirror_row)")
    ap.add_argument("--alpha", type=int, default=40,
                    help="alpha above which a pixel counts as art (default 40; "
                         "raise it for art painted over a soft glow)")
    args = ap.parse_args()

    im = Image.open(args.src).convert("RGBA")
    keyed, note = key_background(im, args.tol)
    print("%s  %dx%d" % (os.path.basename(args.src), im.width, im.height))
    print("  %s" % note)

    force = [int(v) for v in args.rows.split(",")] if args.rows else None
    rows, how, gib, spans = analyse(keyed, alpha=args.alpha, force_rows=force)
    print("  %s" % how)
    for i, r in enumerate(rows):
        hs = [b[3] - b[1] for b in r]
        print("  row %d: %2d sprites, heights %d-%d" % (i, len(r), min(hs), max(hs)))
    if args.inspect:
        return 0

    sheet, msg = resheet(keyed, rows, gib=gib, row_spans=spans,
                         alpha=args.alpha, isolate=not args.no_isolate)
    print("  %s" % msg)
    if sheet is None:
        return 1
    if args.mirror_left:
        sheet = mirror_row(sheet, 1, 3)
        print("  LEFT row rebuilt by mirroring RIGHT, cell by cell")
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
