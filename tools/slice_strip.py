#!/usr/bin/env python3
"""Cut a Codex 4-frame strip on white into equal cells, one scale per strip, base-aligned.
usage: slice_strip.py master.png out_row.png cellW cellH [maxW maxH]"""
import sys
from PIL import Image
src, dst, cw, ch = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
maxw = int(sys.argv[5]) if len(sys.argv) > 5 else cw - 4
maxh = int(sys.argv[6]) if len(sys.argv) > 6 else ch - 4
im = Image.open(src).convert("RGBA")
W, H = im.size
px = im.load()
def fg(x, y):
    r, g, b, a = px[x, y]
    return a > 128 and not (r > 225 and g > 225 and b > 225)
cols = [any(fg(x, y) for y in range(0, H, 2)) for x in range(W)]
segs, s = [], None
for x, c in enumerate(cols + [False]):
    if c and s is None: s = x
    if not c and s is not None:
        if x - s > 20: segs.append((s, x))
        s = None
# merge tiny gaps until 4 segments
while len(segs) > 4:
    gaps = [(segs[i+1][0] - segs[i][1], i) for i in range(len(segs) - 1)]
    _, i = min(gaps); segs[i:i+2] = [(segs[i][0], segs[i+1][1])]
assert len(segs) == 4, segs
frames = []
for a, b in segs:
    fr = im.crop((a, 0, b, H)).copy()
    p = fr.load()
    for y in range(fr.height):
        for x in range(fr.width):
            r, g, bb, al = p[x, y]
            p[x, y] = (r, g, bb, 0) if (al < 128 or (r > 225 and g > 225 and bb > 225)) else (r, g, bb, 255)
    bbox = fr.getchannel("A").getbbox()
    # base x-centre: pixels in the lowest 30 % of the art
    low = fr.crop((0, bbox[3] - (bbox[3] - bbox[1]) * 3 // 10, fr.width, bbox[3])).getchannel("A").getbbox()
    frames.append((fr, bbox, (low[0] + low[2]) / 2))
top = min(f[1][1] for f in frames); bot = max(f[1][3] for f in frames)
left = max(f[2] - f[1][0] for f in frames); right = max(f[1][2] - f[2] for f in frames)
k = min(maxw / (2 * max(left, right)), maxh / (bot - top))
if len(sys.argv) > 7:
    fk = float(sys.argv[7]); assert fk <= k + 1e-9, f"fixed scale {fk} too big, max {k:.3f}"; k = fk
out = Image.new("RGBA", (cw * 4, ch), (0, 0, 0, 0))
for i, (fr, bbox, cx) in enumerate(frames):
    half = max(left, right)
    c = fr.crop((int(round(cx - half)), top, int(round(cx + half)), bot))
    nw, nh = max(1, round(c.width * k)), max(1, round(c.height * k))
    c = c.resize((nw, nh), Image.NEAREST)
    out.alpha_composite(c, (i * cw + (cw - nw) // 2, ch - 2 - nh))
a = out.getchannel("A").point(lambda v: 255 if v >= 128 else 0); out.putalpha(a)
out.save(dst); print(dst, "scale", round(k, 3), "art", round(2 * max(left, right) * k), "x", round((bot - top) * k))
