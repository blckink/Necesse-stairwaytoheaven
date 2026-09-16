#!/usr/bin/env python3
"""Post-process the Twilight Merchant's sheets (2026-09-16 player review).

The shipped outfits and decor carried a painted contour about nine pixels
thick (a 5 px slate band (34,34,46) outside a 4 px near-black band) and read
too dark in game; every head sheet also had its north and south rows swapped
(vanilla: row 0 = facing up / back of head, row 2 = facing down / face).

This script
  * removes the slate band where it touches transparency,
  * peels the near-black band down to a 1 px rim,
  * lifts the remaining colours with a mild gamma,
  * with --swap-head-rows, swaps rows 0 and 2 (64 px cells) of head sheets.

--no-peel keeps the dark band (furniture: thin black legs and rims are part
of the motif and were eaten).

Usage: twilight_cleanup.py [--swap-head-rows] [--no-peel] FILE...   (edits in place)
"""
import sys

from PIL import Image

SLATE = (34, 34, 46)
DARK_LUMA = 34
PEEL_PASSES = 3
GAMMA = 0.75


def luma(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def neighbours(x, y, w, h):
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nx, ny = x + dx, y + dy
        if 0 <= nx < w and 0 <= ny < h:
            yield nx, ny


def touches_air(px, x, y, w, h):
    return any(px[n][3] == 0 for n in neighbours(x, y, w, h)) or x in (0, w - 1) or y in (0, h - 1)


def clean(im, peel=True):
    w, h = im.size
    px = {(x, y): im.getpixel((x, y)) for y in range(h) for x in range(w)}

    # 1. Slate band: flood from the air through exact slate pixels.
    stack = [(x, y) for (x, y), p in px.items() if p[3] and p[:3] == SLATE and touches_air(px, x, y, w, h)]
    seen = set(stack)
    while stack:
        x, y = stack.pop()
        px[(x, y)] = (0, 0, 0, 0)
        for n in neighbours(x, y, w, h):
            if n not in seen and px[n][3] and px[n][:3] == SLATE:
                seen.add(n)
                stack.append(n)

    # 2. The dark band is only what lay right under the slate band: dark
    #    pixels at most PEEL_PASSES+1 steps in from it. Dark motifs (black
    #    robes, dark wood) without a slate band are left alone. Peeling
    #    PEEL_PASSES layers of a 4 px band leaves a 1 px rim.
    band = set()
    frontier = [n for s in seen for n in neighbours(*s, w, h)
                if px[n][3] and luma(px[n]) < DARK_LUMA]
    for _ in range(PEEL_PASSES + 1 if peel else 0):
        nxt = []
        for n in frontier:
            if n not in band:
                band.add(n)
                nxt.extend(m for m in neighbours(*n, w, h)
                           if m not in band and px[m][3] and luma(px[m]) < DARK_LUMA)
        frontier = nxt
    for _ in range(PEEL_PASSES if peel else 0):
        drop = []
        for (x, y) in band:
            p = px[(x, y)]
            if not p[3] or not touches_air(px, x, y, w, h):
                continue
            inner = [px[n] for n in neighbours(x, y, w, h) if px[n][3]]
            if any(luma(q) < DARK_LUMA for q in inner):
                drop.append((x, y))
        if not drop:
            break
        for k in drop:
            px[k] = (0, 0, 0, 0)

    # 3. Lift the colours; alpha stays hard.
    lut = [round(255 * (v / 255) ** GAMMA) for v in range(256)]
    out = Image.new("RGBA", (w, h))
    out.putdata([(lut[p[0]], lut[p[1]], lut[p[2]], 255) if p[3] else (0, 0, 0, 0)
                 for p in (px[(x, y)] for y in range(h) for x in range(w))])
    return out


def swap_rows(im, a=0, b=2, cell=64):
    w = im.size[0]
    ra = im.crop((0, a * cell, w, (a + 1) * cell))
    rb = im.crop((0, b * cell, w, (b + 1) * cell))
    im.paste(rb, (0, a * cell))
    im.paste(ra, (0, b * cell))
    return im


def main(argv):
    swap = "--swap-head-rows" in argv
    peel = "--no-peel" not in argv
    for path in [a for a in argv if not a.startswith("--")]:
        im = clean(Image.open(path).convert("RGBA"), peel)
        if swap and path.endswith("head.png") and "/player/armor/" in path:
            im = swap_rows(im)
        im.save(path)
        print("cleaned", path)


if __name__ == "__main__":
    main(sys.argv[1:])
