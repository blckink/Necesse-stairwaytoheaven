#!/usr/bin/env python3
"""Post-process the Twilight Merchant's sheets (2026-09-16 player review).

The shipped outfits and decor carried a painted contour about nine pixels
thick (a 5 px slate band (34,34,46) outside a 4 px near-black band) and read
too dark in game; every head sheet also had its north and south rows swapped
(vanilla: row 0 = facing up / back of head, row 2 = facing down / face).

This script
  * outfits: removes the slate band and peels the near-black band under it
    to a 1 px rim,
  * furniture (--no-peel): keeps the inner slate layer, drops the outer one,
  * lifts the colours with a mild gamma,
  * recolours the remaining contour like vanilla: a darker shade of the
    motif colour beside it, not black (2026-09-17: removing the whole contour
    tore the hanging tree apart, its twigs were drawn in contour colour),
  * with --swap-head-rows, swaps rows 0 and 2 (64 px cells) of head sheets.

--no-peel keeps the dark band (furniture: thin black legs and rims are part
of the motif and were eaten).

Usage: twilight_cleanup.py [--swap-head-rows] [--no-peel] FILE...   (edits in place; run it on the ORIGINAL sheets, e.g. from git 70946f7)
"""
import sys

from PIL import Image

SLATE = (34, 34, 46)
DARK_LUMA = 34
PEEL_PASSES = 3
GAMMA = 0.75
SELOUT = 0.5


def luma(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def neighbours(x, y, w, h):
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nx, ny = x + dx, y + dy
        if 0 <= nx < w and 0 <= ny < h:
            yield nx, ny


def touches_air(px, x, y, w, h):
    return any(px[n][3] == 0 for n in neighbours(x, y, w, h)) or x in (0, w - 1) or y in (0, h - 1)


def selout(px, rim, w, h):
    """Vanilla-style contour: each rim pixel takes a darker shade of the
    motif colour next to it instead of black/slate. Rim pixels with no motif
    neighbour (thin twigs drawn only in contour) inherit from the nearest
    coloured rim pixel."""
    base = {}
    for k in rim:
        cols = [px[n] for n in neighbours(*k, w, h) if px[n][3] and n not in rim]
        if cols:
            base[k] = tuple(sum(c[i] for c in cols) // len(cols) for i in range(3))
    frontier = list(base)
    while frontier:
        nxt = []
        for k in frontier:
            for n in neighbours(*k, w, h):
                if n in rim and n not in base:
                    base[n] = base[k]
                    nxt.append(n)
        frontier = nxt
    for k in rim:
        c = base.get(k, (60, 45, 50))
        px[k] = (int(c[0] * SELOUT), int(c[1] * SELOUT), int(c[2] * SELOUT), 255)


def clean(im, peel=True):
    w, h = im.size
    px = {(x, y): im.getpixel((x, y)) for y in range(h) for x in range(w)}

    # 1. Slate contour (1-2 px): distance of every slate pixel from the motif.
    slate = {k for k, p in px.items() if p[3] and p[:3] == SLATE}
    dist = {}
    frontier = [k for k in slate if any(px[n][3] and n not in slate for n in neighbours(*k, w, h))]
    for k in frontier:
        dist[k] = 1
    while frontier:
        nxt = []
        for k in frontier:
            for n in neighbours(*k, w, h):
                if n in slate and n not in dist:
                    dist[n] = dist[k] + 1
                    nxt.append(n)
        frontier = nxt

    if peel:
        # Outfits: the slate goes entirely, the 4 px near-black band under it
        # is peeled to a 1 px rim (dark motifs without slate stay untouched).
        for k in slate:
            px[k] = (0, 0, 0, 0)
        band = set()
        frontier = [n for s in slate for n in neighbours(*s, w, h)
                    if px[n][3] and luma(px[n]) < DARK_LUMA]
        for _ in range(PEEL_PASSES + 1):
            nxt = []
            for n in frontier:
                if n not in band:
                    band.add(n)
                    nxt.extend(m for m in neighbours(*n, w, h)
                               if m not in band and px[m][3] and luma(px[m]) < DARK_LUMA)
            frontier = nxt
        for _ in range(PEEL_PASSES):
            drop = [k for k in band if px[k][3] and touches_air(px, *k, w, h)
                    and any(px[n][3] and luma(px[n]) < DARK_LUMA for n in neighbours(*k, w, h))]
            if not drop:
                break
            for k in drop:
                px[k] = (0, 0, 0, 0)
        # Stray rim specks left by the peel: at most one opaque neighbour.
        for _ in range(2):
            for k in [k for k in band if px[k][3]
                      and sum(1 for n in neighbours(*k, w, h) if px[n][3]) <= 1]:
                px[k] = (0, 0, 0, 0)
        rim = {k for k in band if px[k][3] and touches_air(px, *k, w, h)}
    else:
        # Furniture: keep the inner contour layer (it carries thin twigs and
        # legs), drop only the outer one.
        for k in slate:
            if dist.get(k, 1) > 1:
                px[k] = (0, 0, 0, 0)
        rim = {k for k in slate if px[k][3]}

    # 2. Lift the colours, then recolour the rim. Semi-transparent pixels
    #    (ground shadows) keep their colour and alpha: lifted and made opaque
    #    they turned into a teal slab under the sarcophagus.
    lut = [round(255 * (v / 255) ** GAMMA) for v in range(256)]
    for k, p in px.items():
        if p[3] == 255:
            px[k] = (lut[p[0]], lut[p[1]], lut[p[2]], 255)
    selout(px, rim, w, h)

    out = Image.new("RGBA", (w, h))
    out.putdata([px[(x, y)] if px[(x, y)][3] else (0, 0, 0, 0) for y in range(h) for x in range(w)])
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
