#!/usr/bin/env python3
"""Preview arsenal/RecolouredVanillaTexture's white-gold ramp without a client.

The Wolkengleve and the Himmelslanze draw VANILLA sprites (cryoglaive,
dragonlance) re-inked at load time on the client. The dedicated server ships
no PNGs, so the real result can only be seen in game - or here, when a vanilla
sprite dump is supplied:

    python3 tools/recolour_preview.py --vanilla /path/to/sprite/dump
    python3 tools/recolour_preview.py               # proxy: the mod's own sprites

Without --vanilla it runs the SAME ramp over sprites this repo ships
(skyreave, prismcaller, ...), which says whether the ramp reads as white and
gold on comparable pixel art. It does not say what the vanilla glaive will
look like - that stays a hypothesis until someone looks at it.

The ramp and the percentile stretch below must stay in step with
RecolouredVanillaTexture.WHITE_GOLD / gradientMap; the Java class is the one
the game runs.

Writes build/qa/recolour_preview.png: each sprite before and after, 4x.
"""
import argparse
import os

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")

# RecolouredVanillaTexture.WHITE_GOLD
WHITE_GOLD = [
    (0, 78, 52, 24),
    (20, 146, 98, 38),
    (38, 212, 160, 62),
    (52, 238, 204, 120),
    (64, 250, 238, 206),
    (80, 255, 251, 240),
    (100, 255, 255, 255),
]

VANILLA = ["items/cryoglaive", "player/weapons/cryoglaive",
           "items/dragonlance", "player/weapons/dragonlance"]
PROXY = ["items/skyreave", "player/weapons/skyreave",
         "items/prismcaller", "player/weapons/prismcaller"]


def luma(r, g, b):
    return max(0, min(255, int(round(0.299 * r + 0.587 * g + 0.114 * b))))


def percentile(hist, total, fraction):
    target = int(round(total * fraction))
    seen = 0
    for value, count in enumerate(hist):
        seen += count
        if seen > target:
            return value
    return 255


def sample(t):
    position = t * 100.0
    for i in range(1, len(WHITE_GOLD)):
        if position <= WHITE_GOLD[i][0]:
            a, b = WHITE_GOLD[i - 1], WHITE_GOLD[i]
            f = (position - a[0]) / max(1.0, b[0] - a[0])
            return tuple(int(round(a[k] + (b[k] - a[k]) * f)) for k in (1, 2, 3))
    return WHITE_GOLD[-1][1:]


def recolour(img):
    img = img.convert("RGBA")
    px = img.load()
    w, h = img.size
    hist = [0] * 256
    total = 0
    for x in range(w):
        for y in range(h):
            r, g, b, a = px[x, y]
            if a:
                hist[luma(r, g, b)] += 1
                total += 1
    out = img.copy()
    if not total:
        return out
    lo = percentile(hist, total, 0.02)
    hi = percentile(hist, total, 0.98)
    span = max(1, hi - lo)
    opx = out.load()
    for x in range(w):
        for y in range(h):
            r, g, b, a = px[x, y]
            if a:
                t = min(1.0, max(0.0, (luma(r, g, b) - lo) / span))
                opx[x, y] = sample(t) + (a,)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--vanilla", help="vanilla sprite dump root")
    args = ap.parse_args()
    if args.vanilla:
        root, names = args.vanilla, VANILLA
    else:
        root, names = RES, PROXY
    pairs = []
    for name in names:
        path = os.path.join(root, *name.split("/")) + ".png"
        if not os.path.exists(path):
            print("-- missing", path)
            continue
        src = Image.open(path).convert("RGBA")
        pairs.append((name, src, recolour(src)))
    if not pairs:
        raise SystemExit("nothing to preview")
    scale = 4
    pad = 8
    bg = (60, 70, 90, 255)
    width = max(s.width for _, s, _ in pairs) * scale * 2 + pad * 3
    height = sum(s.height * scale + pad for _, s, _ in pairs) + pad
    sheet = Image.new("RGBA", (width, height), bg)
    y = pad
    for name, src, dst in pairs:
        big = (src.width * scale, src.height * scale)
        sheet.alpha_composite(src.resize(big, Image.NEAREST), (pad, y))
        sheet.alpha_composite(dst.resize(big, Image.NEAREST), (pad * 2 + big[0], y))
        print("%-28s %dx%d" % (name, src.width, src.height))
        y += big[1] + pad
    out = os.path.join(REPO, "build", "qa", "recolour_preview.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sheet.save(out)
    print("wrote", out)


if __name__ == "__main__":
    main()
