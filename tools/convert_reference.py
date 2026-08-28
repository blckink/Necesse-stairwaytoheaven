#!/usr/bin/env python3
"""Convert an anti-aliased reference render into a real Necesse sprite sheet.

Reference art arrives as a large, smoothed render: tens of thousands of
colours, no pixel grid recoverable by point sampling, transparency painted as
flat black. It is still convertible when the render was *made from* pixel art,
because the block structure survives resampling even when the exact pixel
boundaries do not.

The pipeline:

  1. Find the native block period on each axis from the column/row change
     energy (a real pixel grid shows up as a periodic comb), or take it from
     --native if you already know the intended size.
  2. Resample by taking the **modal** colour of each source block rather than
     point-sampling. Point sampling is what makes a naive downscale mushy: it
     picks whichever smeared pixel happens to land on the sample point.
  3. Key the flat background colour out to alpha, with a tolerance, and hard
     0/255 alpha the way game sprites have it.
  4. Quantize to a small palette and snap near-duplicates together.

Whether the result is good enough is a judgement call you make by looking at
the 4x contact sheet it writes, not something this script decides.

    python3 tools/convert_reference.py in.png out.png --native 96x192
    python3 tools/convert_reference.py in.png out.png            # detect size
"""

import argparse
import cmath
import math
import os
from collections import Counter

from PIL import Image


def change_energy(im, axis):
    """Per-column (axis=0) or per-row (axis=1) neighbour-difference energy."""
    w, h = im.size
    px = im.load()
    if axis == 0:
        sig = [0.0] * (w - 1)
        for y in range(0, h, 2):
            for x in range(w - 1):
                a, b = px[x, y], px[x + 1, y]
                sig[x] += abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2])
    else:
        sig = [0.0] * (h - 1)
        for x in range(0, w, 2):
            for y in range(h - 1):
                a, b = px[x, y], px[x, y + 1]
                sig[y] += abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2])
    return sig


def best_period(sig, lo=2.0, hi=26.0, step=0.02):
    """Strongest periodic comb in the change energy — the pixel block size."""
    mean = sum(sig) / len(sig)
    centred = [v - mean for v in sig]
    n = len(centred)
    best = (0.0, lo)
    per = lo
    while per < hi:
        acc = 0j
        for k, v in enumerate(centred):
            acc += v * cmath.exp(-2j * math.pi * k / per)
        score = abs(acc) / n
        if score > best[0]:
            best = (score, per)
        per += step
    return best[1]


def modal_downsample(im, out_w, out_h):
    """Take the most common colour of each source block, not a sample point."""
    w, h = im.size
    px = im.load()
    out = Image.new("RGB", (out_w, out_h))
    op = out.load()
    for oy in range(out_h):
        y0, y1 = oy * h // out_h, max(oy * h // out_h + 1, (oy + 1) * h // out_h)
        for ox in range(out_w):
            x0, x1 = ox * w // out_w, max(ox * w // out_w + 1, (ox + 1) * w // out_w)
            c = Counter()
            for y in range(y0, y1):
                for x in range(x0, x1):
                    c[px[x, y]] += 1
            op[ox, oy] = c.most_common(1)[0][0]
    return out


def key_background(im, bg, tol):
    """Flat painted background -> hard alpha, the way real sprites have it."""
    out = im.convert("RGBA")
    px = out.load()
    w, h = out.size
    for y in range(h):
        for x in range(w):
            r, g, b, _ = px[x, y]
            if abs(r - bg[0]) + abs(g - bg[1]) + abs(b - bg[2]) <= tol:
                px[x, y] = (0, 0, 0, 0)
            else:
                px[x, y] = (r, g, b, 255)
    return out


def quantize_opaque(im, colors):
    """Quantize only the opaque pixels, so keying is not undone."""
    rgb = im.convert("RGB")
    q = rgb.quantize(colors=colors, method=Image.Quantize.MEDIANCUT).convert("RGB")
    out = Image.new("RGBA", im.size)
    src, dst, qp = im.load(), out.load(), q.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            if src[x, y][3] == 0:
                dst[x, y] = (0, 0, 0, 0)
            else:
                r, g, b = qp[x, y]
                dst[x, y] = (r, g, b, 255)
    return out


def contact_sheet(im, path, scale=4):
    """4x on a dark and a light backdrop, side by side — the thing you look at."""
    w, h = im.size
    big = im.resize((w * scale, h * scale), Image.NEAREST)
    sheet = Image.new("RGB", (w * scale * 2 + 24, h * scale), (24, 24, 30))
    light = Image.new("RGB", (w * scale, h * scale), (222, 226, 232))
    sheet.paste(light, (w * scale + 24, 0))
    sheet.paste(big, (0, 0), big)
    sheet.paste(big, (w * scale + 24, 0), big)
    sheet.save(path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("src")
    ap.add_argument("dst")
    ap.add_argument("--native", help="target size WxH; detected if omitted")
    ap.add_argument("--bg", default="0,0,0", help="background colour to key out")
    ap.add_argument("--tol", type=int, default=60, help="background match tolerance")
    ap.add_argument("--colors", type=int, default=24)
    ap.add_argument("--sheet", help="also write a 4x contact sheet here")
    a = ap.parse_args()

    im = Image.open(a.src).convert("RGB")
    w, h = im.size
    if a.native:
        out_w, out_h = (int(v) for v in a.native.lower().split("x"))
    else:
        px = round(w / best_period(change_energy(im, 0)))
        py = round(h / best_period(change_energy(im, 1)))
        out_w, out_h = px, py
        print(f"detected native size: {out_w}x{out_h}")

    bg = tuple(int(v) for v in a.bg.split(","))
    small = modal_downsample(im, out_w, out_h)
    keyed = key_background(small, bg, a.tol)
    final = quantize_opaque(keyed, a.colors)

    os.makedirs(os.path.dirname(os.path.abspath(a.dst)), exist_ok=True)
    final.save(a.dst)

    opaque = sum(1 for p in final.get_flattened_data() if p[3] > 0)
    cols = len({p[:3] for p in final.get_flattened_data() if p[3] > 0})
    print(f"{a.src} {w}x{h} -> {a.dst} {out_w}x{out_h}")
    print(f"  opaque pixels: {opaque}   distinct colours: {cols}")
    if a.sheet:
        contact_sheet(final, a.sheet)
        print(f"  contact sheet: {a.sheet}")


if __name__ == "__main__":
    main()
