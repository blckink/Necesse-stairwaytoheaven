#!/usr/bin/env python3
"""Turn a 2 MB generated render into a shipped Necesse sprite. No AI.

The problem this exists for: an image generator hands back a 1024x1024 or
1536x1536 PNG carrying ten to thirty THOUSAND colours, soft edges and a
half-transparent halo. A shipped Necesse sheet is 384x320 with 19-38 colours,
hard alpha and every tone on a 2x2 block. Nothing in between is a resize.

Four passes, all arithmetic:

  1. FIT      down to the target size. An exact integer multiple is
              downsampled by taking the MOST COMMON colour of each block,
              which is lossless for art that was upscaled from pixels. A
              non-integer ratio (1024 -> 384 is 2.67x) has no such shortcut,
              so it goes through a box average first and is then quantised --
              the average is what stops a single stray pixel deciding a block.
  2. PALETTE  median-cut down to --colours (default 32, the middle of
              vanilla's 19-38). This is the single biggest difference between
              a render and a sprite and it is worth more than any other pass.
  3. ALPHA    hard 0 or 255 at a threshold. A generated edge fades out over
              several pixels; the engine wants an edge.
  4. SNAP     optional 2x2 block unification, for ground splats. Off unless
              asked, because on a mob sheet it would blur the silhouette.

    python3 tools/autofit.py IN.png --target 384x320
    python3 tools/autofit.py IN.png --like src/main/resources/mobs/gloomshade.png
    python3 tools/autofit.py inbox/*.png --outdir out/ --colours 24

`--like` is the easy one: it reads the size off a sprite you already ship, so
you never have to remember whether a mob sheet is 384x320 or a splat 224x576.

What this does NOT do is decide where the frames sit. Once the file is the
right size, `tools/resheet_mob.py` aligns a mob sheet's cells and
`tools/fix_splat.py` puts a ground on 2x2 blocks.
"""
import argparse
import collections
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def modal_block(im, tw, th):
    """Downsample by taking each block's most common colour.

    Correct when the input is an integer upscale of pixel art: every block is
    one original pixel repeated, so the mode IS that pixel. Also the right
    choice for a near-integer ratio, where averaging would invent colours that
    were never drawn.
    """
    src = im.load()
    out = Image.new("RGBA", (tw, th))
    dst = out.load()
    sx, sy = im.width / tw, im.height / th
    for y in range(th):
        y0, y1 = int(y * sy), max(int(y * sy) + 1, int((y + 1) * sy))
        for x in range(tw):
            x0, x1 = int(x * sx), max(int(x * sx) + 1, int((x + 1) * sx))
            counts = collections.Counter(
                src[px, py] for py in range(y0, min(y1, im.height))
                for px in range(x0, min(x1, im.width)))
            dst[x, y] = counts.most_common(1)[0][0]
    return out


def fit(im, tw, th):
    """Pass 1 -- land on the target size, by the cheapest correct route."""
    if im.width == tw and im.height == th:
        return im, "already %dx%d" % (tw, th)
    if im.width % tw == 0 and im.height % th == 0:
        n = im.width // tw
        return modal_block(im, tw, th), "downsampled %dx (modal, lossless)" % n
    # Non-integer. Box-average to twice the target first so each final pixel is
    # decided by a real neighbourhood rather than by whichever pixel a
    # nearest-neighbour pick happened to land on, then take the mode of each 2x2.
    mid = im.resize((tw * 2, th * 2), Image.BOX)
    return modal_block(mid, tw, th), ("resampled %.2fx (box, then modal)"
                                      % (im.width / tw))


def quantize(im, colours):
    """Pass 2 -- median-cut to `colours`, alpha held out of the cut.

    Alpha is separated first because a median cut over RGBA spends its palette
    budget on transparency levels instead of on the art. The alpha comes back
    untouched afterwards, and pass 3 hardens it.
    """
    alpha = im.getchannel("A")
    rgb = im.convert("RGB")
    q = rgb.quantize(colors=colours, method=Image.MEDIANCUT, dither=Image.NONE)
    out = q.convert("RGBA")
    out.putalpha(alpha)
    return out


def harden_alpha(im, threshold):
    """Pass 3 -- every pixel fully in or fully out."""
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255 if a >= threshold else 0)
    return im


def snap2x2(im):
    """Pass 4 -- each aligned 2x2 block becomes its own dominant colour."""
    px = im.load()
    for by in range(0, im.height - 1, 2):
        for bx in range(0, im.width - 1, 2):
            quad = [px[bx, by], px[bx + 1, by], px[bx, by + 1], px[bx + 1, by + 1]]
            counts = collections.Counter(quad)
            best = max(counts.values())
            winner = next(c for c in quad if counts[c] == best)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                px[bx + dx, by + dy] = winner
    return im


def n_colours(im):
    return len({p[:3] for p in im.get_flattened_data() if p[3] > 0})


def run(path, tw, th, colours, threshold, snap, outdir):
    im = Image.open(path).convert("RGBA")
    name = os.path.basename(path)
    before_kb = os.path.getsize(path) / 1024
    print("%s  %dx%d  %d colours  %.0f KB" % (name, im.width, im.height,
                                              n_colours(im), before_kb))

    out, how = fit(im, tw, th)
    print("  fit      %s" % how)
    out = quantize(out, colours)
    out = harden_alpha(out, threshold)
    if snap:
        out = snap2x2(out)
        print("  snap     2x2 blocks unified")

    os.makedirs(outdir, exist_ok=True)
    dest = os.path.join(outdir, name)
    out.save(dest, optimize=True)
    after_kb = os.path.getsize(dest) / 1024
    print("  ->       %dx%d  %d colours  %.0f KB  (%.0f%% smaller)"
          % (out.width, out.height, n_colours(out), after_kb,
             (1 - after_kb / before_kb) * 100 if before_kb else 0))
    print("  wrote    %s" % os.path.relpath(dest, REPO))
    return dest


def parse_size(text):
    w, _, h = text.lower().partition("x")
    return int(w), int(h)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("paths", nargs="+", help="the oversized PNGs")
    ap.add_argument("--target", help="target size, e.g. 384x320")
    ap.add_argument("--like", help="a sprite already in the mod to copy the size from")
    ap.add_argument("--colours", type=int, default=32,
                    help="palette size (default 32; shipped sheets carry 19-38)")
    ap.add_argument("--alpha", type=int, default=128,
                    help="alpha at or above this becomes opaque (default 128)")
    ap.add_argument("--snap", action="store_true",
                    help="also put every 2x2 block on one colour -- for GROUND "
                         "splats. Do not use on a mob sheet")
    ap.add_argument("--outdir", default=os.path.join(REPO, "build", "qa", "autofit"),
                    help="where the fitted files go (never into the mod directly)")
    args = ap.parse_args()

    if args.like:
        with Image.open(args.like) as ref:
            tw, th = ref.size
        print("target %dx%d, read off %s\n" % (tw, th, os.path.basename(args.like)))
    elif args.target:
        tw, th = parse_size(args.target)
    else:
        print("give --target WxH or --like <a sprite you already ship>")
        return 2

    for p in args.paths:
        if not os.path.exists(p):
            print("%s: not found" % p)
            continue
        run(p, tw, th, args.colours, args.alpha, args.snap, args.outdir)
    return 0


if __name__ == "__main__":
    sys.exit(main())
