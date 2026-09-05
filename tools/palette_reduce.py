#!/usr/bin/env python3
"""Cut a sheet down to a vanilla-sized palette without losing its accents.

Shipped Necesse sheets carry 19-38 distinct colours. A generated or painted
sheet carries tens of thousands, and the obvious fix -- `Image.quantize` --
makes it worse in a specific, repeatable way:

    mobs/nimbusyak.png, 34890 colours, reduced to 16/24/32/48
    -> every level loses the pink noses and the green flower crowns.

The reason is that median cut splits the colour cube by **population**. A yak
is mostly white fleece, so nearly every palette slot goes to shades of white,
and the few hundred saturated pixels that carry the animal's character get
merged into the nearest beige. The picture keeps its shape and loses its face.

So this splits the problem before quantising: pixels that are **saturated**
(the accents) get their own reserved share of the palette, judged on their own
population, while the bulk gets the rest. Neither competes with the other.

    python3 tools/palette_reduce.py in.png -o out.png --colours 38
    ... --accent-share 0.35     how much of the palette accents may claim
    ... --saturation 0.35       above this a pixel counts as an accent
    ... --report                print what was kept, change nothing
"""
import argparse
import collections
import os
import sys


def saturation(r, g, b):
    m, n = max(r, g, b), min(r, g, b)
    return 0.0 if m == 0 else (m - n) / float(m)


def pick(counts, k):
    """Choose k representative colours from {colour: count} by median cut.

    Population-weighted inside its own group, which is the point: accents are
    cut against accents, bulk against bulk.
    """
    if not counts:
        return []
    if len(counts) <= k:
        return list(counts)
    boxes = [list(counts.keys())]
    while len(boxes) < k:
        # Split the box with the widest spread on its widest channel -- the
        # classic median cut, but over unique colours we already have.
        target, chan, spread = None, 0, -1
        for i, box in enumerate(boxes):
            if len(box) < 2:
                continue
            for c in range(3):
                vals = [p[c] for p in box]
                s = max(vals) - min(vals)
                if s > spread:
                    target, chan, spread = i, c, s
        if target is None:
            break
        box = sorted(boxes[target], key=lambda p: p[chan])
        # Split at the weighted median so a small bright cluster is not
        # swallowed by the half it happens to sit in.
        total = sum(counts[p] for p in box)
        run, cut = 0, len(box) // 2
        for i, p in enumerate(box):
            run += counts[p]
            if run >= total / 2:
                cut = max(1, min(i, len(box) - 1))
                break
        boxes[target:target + 1] = [box[:cut], box[cut:]]

    out = []
    for box in boxes:
        w = sum(counts[p] for p in box) or 1
        out.append(tuple(int(round(sum(p[c] * counts[p] for p in box) / w))
                         for c in range(3)))
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("source")
    ap.add_argument("-o", "--out")
    ap.add_argument("--colours", type=int, default=33,
                    help="palette cap; 33 is the player's chosen compromise "
                         "inside vanilla's 19-38 band")
    ap.add_argument("--accent-share", type=float, default=0.35)
    ap.add_argument("--saturation", type=float, default=0.35)
    ap.add_argument("--report", action="store_true")
    args = ap.parse_args()

    from PIL import Image
    im = Image.open(args.source).convert("RGBA")
    px = im.load()

    bulk, accent = collections.Counter(), collections.Counter()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a <= 128:
                continue
            (accent if saturation(r, g, b) >= args.saturation else bulk)[(r, g, b)] += 1

    n_acc = min(len(accent), max(1, int(round(args.colours * args.accent_share))))
    n_bulk = max(1, args.colours - n_acc)
    pal = list(dict.fromkeys(pick(bulk, n_bulk) + pick(accent, n_acc)))

    print("%s: %d unique colours (%d bulk / %d accent px)"
          % (os.path.basename(args.source), len(bulk) + len(accent),
             sum(bulk.values()), sum(accent.values())))
    print("palette: %d bulk + %d accent = %d entries" % (n_bulk, n_acc, len(pal)))
    if args.report:
        for c in sorted(pal, key=lambda c: -saturation(*c))[:10]:
            print("   #%02x%02x%02x  sat %.2f" % (c[0], c[1], c[2], saturation(*c)))
        return 0

    # Map by unique colour, not per pixel: a sheet has far fewer colours than
    # pixels, so this is the difference between a second and a minute.
    cache = {}

    def nearest(c):
        if c not in cache:
            cache[c] = min(pal, key=lambda p: (p[0] - c[0]) ** 2
                           + (p[1] - c[1]) ** 2 + (p[2] - c[2]) ** 2)
        return cache[c]

    out = Image.new("RGBA", im.size, (0, 0, 0, 0))
    o = out.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a <= 128:
                continue
            o[x, y] = nearest((r, g, b)) + (255,)

    dst = args.out or os.path.splitext(args.source)[0] + "_reduced.png"
    out.save(dst)
    print("wrote %s -- %d colours" % (dst, len(out.getcolors(1 << 24) or [])))
    print("On-format is not approved: look at it, then ask.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
