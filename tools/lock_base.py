#!/usr/bin/env python3
"""Freeze the base of every frame to frame 0: only pixels near a silhouette change
(alpha XOR with frame 0, dilated) come from frame k. Removes per-frame redraw jitter.
usage: lock_base.py sheet.png cellW cellH frames rows [dilate]"""
import sys
from PIL import Image, ImageChops, ImageFilter
p, cw, ch, fr, rows = sys.argv[1], *map(int, sys.argv[2:6])
dil = int(sys.argv[6]) if len(sys.argv) > 6 else 5
COLTH = int(sys.argv[7]) if len(sys.argv) > 7 else 120
im = Image.open(p).convert("RGBA")
for r in range(rows):
    base = im.crop((0, r * ch, cw, (r + 1) * ch))
    for c in range(1, fr):
        box = (c * cw, r * ch, (c + 1) * cw, (r + 1) * ch)
        f = im.crop(box)
        ma = ImageChops.difference(base.getchannel("A"), f.getchannel("A")).point(lambda v: 255 if v else 0)
        # strong colour change inside the silhouette (arm/stone over the chassis); noise stays below
        rgb = ImageChops.difference(base.convert("RGB"), f.convert("RGB"))
        r_, g_, b_ = rgb.split()
        mc = ImageChops.add(ImageChops.add(r_, g_), b_).point(lambda v: 255 if v > COLTH else 0)
        mc = mc.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.MaxFilter(3))  # drop specks
        m = ImageChops.lighter(ma, mc).filter(ImageFilter.MaxFilter(dil))
        out = base.copy()
        out.paste(Image.new("RGBA", (cw, ch), (0, 0, 0, 0)), (0, 0), m)
        out.paste(f, (0, 0), ImageChops.multiply(m, f.getchannel("A")))
        im.paste(out, box[:2])
im.save(p)
