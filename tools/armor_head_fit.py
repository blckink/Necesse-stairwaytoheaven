#!/usr/bin/env python3
"""Bring an armor head sheet onto vanilla's half-resolution grid and size. No AI.

Two defects with one cause, found 2026-09-19. Vanilla player art is 32x32
art upscaled 2x with NEAREST -- every 2x2 block on even coordinates is one
uniform colour, zero exceptions across `ironhelmet`, `copperhelmet` and
`magehat` (gen_armor.py states the rule; the check below proves it). The
Twilight Merchant's nine Halloween heads were drawn at true 64 px instead:
24-28% of their blocks carry four different pixels. That is twice the detail
of the body underneath, and it is why they read wrong on a player -- on top
of simply being too wide (up to 43 px against vanilla's 24).

Both are one operation. Scaling the content down and landing it on the
half-res grid is the same pass, because we scale straight INTO half-res and
upscale 2x afterwards -- conformance by construction, no snap step:

  1. MEASURE every cell, take ONE shared factor for the sheet from the widest
     of them. Per-cell fitting is what makes a walk cycle wobble (the lesson
     `resheet_mob.py` already writes down): the four directions have to stay
     the same mask.
  2. DOWNSAMPLE each cell's content straight to half size. A target pixel is
     opaque when at least half its source box is, and takes the MOST COMMON
     opaque colour in that box -- so the palette stays the drawn one and no
     transparent black is averaged into an edge.
  3. ANCHOR against a vanilla head sheet, cell by cell: same centre x, same
     bottom y (the chin line the engine draws the head at). Reading the
     anchor per cell carries vanilla's walk bob over for free. The paste
     origin is rounded down to even, or the grid built in step 2 breaks.

    python3 tools/armor_head_fit.py src/main/resources/player/armor/skeletonhead.png
    python3 tools/armor_head_fit.py IN.png --target-width 30 -o OUT.png
    python3 tools/armor_head_fit.py IN.png --check      # report only, write nothing

`--check` prints the off-grid percentage and the widest cell, which is the
same pair of numbers that found this in the first place. Verify a written
sheet by running `--check` on it again: it has to come back 0.0% and inside
vanilla's width band.
"""
import argparse
import collections
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VANILLA_REF = os.path.join(
    os.path.expanduser("~"), "dev", "Necesse sprites", "player", "armor", "ironhelmet.png")
CELL_W = 64
ROWS = 4
#: Vanilla helmets sit at 24 px wide, `magehat` at 40 because a hat may flare.
#: A Halloween mask is a helmet, so 28 leaves a little room without leaving
#: the band.
DEFAULT_TARGET_W = 28


def cells(im):
    """Yield (row, col, box) for the 7x4 grid the engine reads."""
    cell_h = im.height // ROWS
    for row in range(ROWS):
        for col in range(im.width // CELL_W):
            yield row, col, (col * CELL_W, row * cell_h,
                             (col + 1) * CELL_W, (row + 1) * cell_h)


def off_grid(im):
    """Fraction of 2x2 blocks that are not one uniform colour."""
    px = im.load()
    bad = total = 0
    for y in range(0, im.height - 1, 2):
        for x in range(0, im.width - 1, 2):
            quad = {(0, 0, 0, 0) if px[a, b][3] == 0 else px[a, b]
                    for a, b in ((x, y), (x + 1, y), (x, y + 1), (x + 1, y + 1))}
            total += 1
            if len(quad) != 1:
                bad += 1
    return bad / total if total else 0.0


def widest(im):
    """Widest content box over all cells -- what the shared factor comes from."""
    out = 0
    for _, _, box in cells(im):
        bb = im.crop(box).split()[3].getbbox()
        if bb:
            out = max(out, bb[2] - bb[0])
    return out


def half_res(crop, tw, th):
    """Pass 2 -- straight to half size, majority opaque, modal colour."""
    src = crop.load()
    out = Image.new("RGBA", (max(tw, 1), max(th, 1)), (0, 0, 0, 0))
    dst = out.load()
    sx, sy = crop.width / max(tw, 1), crop.height / max(th, 1)
    for y in range(out.height):
        y0, y1 = int(y * sy), max(int(y * sy) + 1, int((y + 1) * sy))
        for x in range(out.width):
            x0, x1 = int(x * sx), max(int(x * sx) + 1, int((x + 1) * sx))
            seen = [src[px, py]
                    for py in range(y0, min(y1, crop.height))
                    for px in range(x0, min(x1, crop.width))]
            solid = [c for c in seen if c[3] > 0]
            if seen and len(solid) * 2 >= len(seen) and solid:
                dst[x, y] = collections.Counter(solid).most_common(1)[0][0]
    return out


def anchors(path):
    """Per-cell (centre x, bottom y) of a vanilla head sheet, or {} if absent."""
    if not os.path.exists(path):
        return {}
    ref = Image.open(path).convert("RGBA")
    out = {}
    for row, col, box in cells(ref):
        bb = ref.crop(box).split()[3].getbbox()
        if bb:
            out[(row, col)] = ((bb[0] + bb[2]) / 2.0, bb[3])
    return out


def fit(im, target_w, ref):
    wide = widest(im)
    if not wide:
        return im, 1.0
    scale = target_w / float(wide)
    out = Image.new("RGBA", im.size, (0, 0, 0, 0))
    cell_h = im.height // ROWS
    for row, col, box in cells(im):
        cell = im.crop(box)
        bb = cell.split()[3].getbbox()
        if not bb:
            continue
        crop = cell.crop(bb)
        # Half-res target, then NEAREST 2x: on the grid by construction.
        tw = max(1, int(round(crop.width * scale / 2.0)))
        th = max(1, int(round(crop.height * scale / 2.0)))
        small = half_res(crop, tw, th)
        big = small.resize((tw * 2, th * 2), Image.NEAREST)
        # Vanilla's anchor for this very cell; its own box only as fallback.
        cx, by = ref.get((row, col), ((bb[0] + bb[2]) / 2.0, bb[3]))
        px = int(round(cx - big.width / 2.0)) & ~1
        py = int(round(by - big.height)) & ~1
        px = max(0, min(px, CELL_W - big.width))
        py = max(0, min(py, cell_h - big.height))
        out.paste(big, (box[0] + px, box[1] + py))
    return out, scale


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("files", nargs="+")
    ap.add_argument("-o", "--out", help="write here instead of in place (single file only)")
    ap.add_argument("--target-width", type=int, default=DEFAULT_TARGET_W)
    ap.add_argument("--ref", default=VANILLA_REF, help="vanilla head sheet to take anchors from")
    ap.add_argument("--check", action="store_true", help="report only, write nothing")
    args = ap.parse_args()

    if args.out and len(args.files) != 1:
        sys.exit("armor_head_fit: -o takes exactly one input file")
    ref = anchors(args.ref)
    if not ref and not args.check:
        print("armor_head_fit: no anchors from %s -- falling back to each cell's own box"
              % args.ref, file=sys.stderr)

    for path in args.files:
        im = Image.open(path).convert("RGBA")
        name = os.path.basename(path)
        if args.check:
            print("%-28s %5.1f%% off-grid, widest cell %2d px"
                  % (name, 100 * off_grid(im), widest(im)))
            continue
        out, scale = fit(im, args.target_width, ref)
        dest = args.out or path
        out.save(dest)
        print("%-28s x%.2f -> %5.1f%% off-grid, widest cell %2d px"
              % (name, scale, 100 * off_grid(out), widest(out)))


if __name__ == "__main__":
    main()
