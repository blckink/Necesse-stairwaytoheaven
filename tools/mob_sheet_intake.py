#!/usr/bin/env python3
"""Fit a generated mob sheet onto vanilla's cell grid, frame by frame.

`asset_intake.py` can rescale a whole sheet when it is an exact integer
multiple. A *generated* sheet never is: the model draws six cows in a row at
whatever size it felt like, so X and Y scales differ and the frames drift.
Rescaling the whole image cannot fix that -- each frame has to be moved into
its own cell.

What makes that tractable is that the target geometry is not a guess: the
**vanilla sheet is the specification**. For every cell it says how big the
subject may be and where its feet are. So each generated frame is scaled to
vanilla's envelope for its row and set down on vanilla's ground line.

Measured on the supplied Nimbus Yak (`cow-new-nimbusyak.png`, 1312x1199):

  * five row bands -- four directions plus the 64px gib strip, as vanilla has,
  * the up/down rows segment into six clean blobs,
  * **the side rows do not.** A side-on cow is wide enough to touch its
    neighbour, so those rows come out as two merged blobs, not six.

That last point is why this does not simply look for gaps. The column pitch is
measured on the rows that *do* separate (218.1 and 217.7 px -- they agree), and
that pitch is then applied to the merged rows. Same for the row pitch (241,
241, 244).

Usage:
    PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/mob_sheet_intake.py \\
        <generated.png> --vanilla mobs/cow.png -o build/qa/mobintake
    ... --cols 6 --rows 5      override the expected grid
    ... --colours 32           palette cap after downsampling
"""
import argparse
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VANILLA_DUMP = os.environ.get(
    "NECESSE_VANILLA_SPRITES", "/home/blackoffset/dev/Necesse sprites")

ALPHA = 16          # below this a pixel is background, not faint art


def alpha_bytes(im):
    return im.getchannel("A").tobytes(), im.width, im.height


def runs(flags):
    """Contiguous True stretches as (start, end) inclusive."""
    out, s = [], None
    for i, v in enumerate(flags):
        if v and s is None:
            s = i
        elif not v and s is not None:
            out.append((s, i - 1))
            s = None
    if s is not None:
        out.append((s, len(flags) - 1))
    return out


def row_bands(A, W, H):
    return runs([any(A[y * W + x] > ALPHA for x in range(W)) for y in range(H)])


def col_runs(A, W, y0, y1):
    return runs([any(A[y * W + x] > ALPHA for y in range(y0, y1 + 1))
                 for x in range(W)])


def centres(rs):
    return [(a + b) / 2.0 for a, b in rs]


def pitch_of(vals):
    if len(vals) < 2:
        return None
    d = [vals[i + 1] - vals[i] for i in range(len(vals) - 1)]
    return sum(d) / len(d)


def derive_columns(A, W, bands, want):
    """Column centres, measured where the frames actually separate.

    Returns (centres, pitch, note). Rows whose blob count already equals the
    expected column count vote on the grid; merged rows inherit it.
    """
    clean = []
    for (y0, y1) in bands:
        cr = col_runs(A, W, y0, y1)
        if len(cr) == want:
            clean.append(centres(cr))
    if not clean:
        return None, None, "no row separated into %d frames -- cannot place a grid" % want
    first = [sum(c[i] for c in clean) / len(clean) for i in range(want)]
    p = pitch_of(first)
    return first, p, "grid from %d of %d rows that separated cleanly" % (len(clean), len(bands))


def bbox_in(A, W, x0, y0, x1, y1):
    xs = ys = None
    xe = ye = -1
    for y in range(y0, y1 + 1):
        base = y * W
        for x in range(x0, x1 + 1):
            if A[base + x] > ALPHA:
                if xs is None or x < xs:
                    xs = x
                if x > xe:
                    xe = x
                if ys is None or y < ys:
                    ys = y
                if y > ye:
                    ye = y
    return None if xs is None else (xs, ys, xe, ye)


def vanilla_cells(im, cw, ch):
    """Per-cell bbox of the reference sheet -- the geometry to hit."""
    A, W, H = alpha_bytes(im)
    out = {}
    for r in range(H // ch):
        for c in range(W // cw):
            b = bbox_in(A, W, c * cw, r * ch, c * cw + cw - 1, r * ch + ch - 1)
            out[(r, c)] = None if b is None else (
                b[0] - c * cw, b[1] - r * ch, b[2] - c * cw, b[3] - r * ch)
    return out


def row_envelope(vcells, r, cols):
    """Widest/tallest subject and the ground line, across one vanilla row.

    Per row rather than per cell on purpose: a walk cycle must not breathe --
    scaling each frame to its own bbox would make the animation pulse.
    """
    bs = [vcells[(r, c)] for c in range(cols) if vcells.get((r, c))]
    if not bs:
        return None
    w = max(b[2] - b[0] + 1 for b in bs)
    h = max(b[3] - b[1] + 1 for b in bs)
    ground = max(b[3] for b in bs)
    cx = sum((b[0] + b[2] + 1) / 2.0 for b in bs) / len(bs)
    return w, h, ground, cx


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("source")
    ap.add_argument("--vanilla", required=True,
                    help="reference sheet, e.g. mobs/cow.png (in the dump) or a path")
    ap.add_argument("--cols", type=int, default=6)
    ap.add_argument("--rows", type=int, default=5)
    ap.add_argument("--cell", type=int, default=64)
    ap.add_argument("--colours", type=int, default=32)
    ap.add_argument("-o", "--out", default=os.path.join(REPO, "build", "qa", "mobintake"))
    args = ap.parse_args()

    from PIL import Image

    vpath = args.vanilla
    if not os.path.isfile(vpath):
        vpath = os.path.join(VANILLA_DUMP, args.vanilla)
    if not os.path.isfile(vpath):
        print("vanilla reference not found: %s" % args.vanilla, file=sys.stderr)
        return 1

    van = Image.open(vpath).convert("RGBA")
    cw = ch = args.cell
    if van.width != args.cols * cw or van.height != args.rows * ch:
        print("reference is %dx%d, not %dx%d -- pass --cols/--rows/--cell"
              % (van.width, van.height, args.cols * cw, args.rows * ch), file=sys.stderr)
        return 1
    vcells = vanilla_cells(van, cw, ch)

    src = Image.open(args.source).convert("RGBA")
    A, W, H = alpha_bytes(src)
    if src.getchannel("A").getextrema()[0] == 255:
        print("source is fully opaque -- it has no transparent background to segment",
              file=sys.stderr)
        return 2

    bands = row_bands(A, W, H)
    print("source %dx%d -> %d row band(s)" % (W, H, len(bands)))
    if len(bands) != args.rows:
        print("expected %d rows (4 directions + gib strip); refusing rather than guessing"
              % args.rows, file=sys.stderr)
        for i, (a, b) in enumerate(bands):
            print("  band %d: y %d-%d" % (i, a, b), file=sys.stderr)
        return 2

    ccentres, cpitch, note = derive_columns(A, W, bands, args.cols)
    if ccentres is None:
        print(note, file=sys.stderr)
        return 2
    print("columns: %s (pitch %.1f) -- %s"
          % (" ".join("%.0f" % c for c in ccentres), cpitch, note))

    out = Image.new("RGBA", (args.cols * cw, args.rows * ch), (0, 0, 0, 0))
    report = []
    for r, (y0, y1) in enumerate(bands):
        env = row_envelope(vcells, r, args.cols)
        if env is None:
            report.append("row %d: vanilla row is empty, skipped" % r)
            continue
        maxw, maxh, ground, cx = env
        for c in range(args.cols):
            half = cpitch / 2.0
            x0 = max(0, int(round(ccentres[c] - half)))
            x1 = min(W - 1, int(round(ccentres[c] + half)))
            b = bbox_in(A, W, x0, y0, x1, y1)
            if b is None:
                report.append("r%dc%d: empty in the source" % (r, c))
                continue
            sw, sh = b[2] - b[0] + 1, b[3] - b[1] + 1
            # One scale for both axes: anything else distorts the drawing.
            # The envelope is vanilla's, so the subject lands in vanilla's
            # size band rather than in whatever size it was drawn at.
            k = min(maxw / sw, maxh / sh)
            nw, nh = max(1, int(round(sw * k))), max(1, int(round(sh * k)))
            piece = src.crop((b[0], b[1], b[2] + 1, b[3] + 1)).resize(
                (nw, nh), Image.BOX)          # BOX = area average, the honest downsample
            # Feet on vanilla's ground line, centred on vanilla's centre.
            px = int(round(c * cw + cx - nw / 2.0))
            py = int(round(r * ch + ground + 1 - nh))
            out.alpha_composite(piece, (max(c * cw, min(px, c * cw + cw - nw)),
                                        max(r * ch, min(py, r * ch + ch - nh))))
            report.append("r%dc%d: %dx%d -> %dx%d (k=%.3f)" % (r, c, sw, sh, nw, nh, k))

    os.makedirs(args.out, exist_ok=True)
    name = os.path.splitext(os.path.basename(args.source))[0]
    # Quantise after placing, never before: the resize introduces blends that
    # would otherwise be baked into the palette.
    alpha = out.getchannel("A").point(lambda v: 255 if v > 128 else 0)
    flat = out.convert("RGB").quantize(colors=args.colours, method=Image.MEDIANCUT)
    final = flat.convert("RGBA")
    final.putalpha(alpha)
    dst = os.path.join(args.out, "%s_fitted.png" % name)
    final.save(dst)

    with open(os.path.join(args.out, "%s_report.txt" % name), "w") as f:
        f.write("source: %s\nvanilla: %s\n%s\n\n" % (args.source, vpath, note))
        f.write("\n".join(report) + "\n")

    print("\nwrote %s (%dx%d, %d colours)"
          % (os.path.relpath(dst, REPO), final.width, final.height,
             len(final.getcolors(1 << 24) or [])))
    print("report: %s" % os.path.relpath(
        os.path.join(args.out, "%s_report.txt" % name), REPO))
    print("\nThis is on-format, not approved. Run tools/asset_review.py next --\n"
          "no measurement here can say the art reads.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
