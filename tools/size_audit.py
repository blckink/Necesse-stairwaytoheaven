#!/usr/bin/env python3
"""Size audit: measure every mod sprite against its closest vanilla analogue.

Playtest rule this enforces: an asset that reads smaller/thinner than its
vanilla counterpart feels wrong in game (the warden and the seance circle
both shipped undersized before this audit existed). For each mapped pair the
script measures the opaque-pixel bounding box and fill of ONE representative
cell and prints the ratio ours/vanilla; anything under the threshold is
flagged FIX.

Usage:  python3 tools/size_audit.py [--vanilla /path/to/sprite/dump]
The vanilla dump is never committed; default path matches the dev container.
"""
import argparse
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")

# (mod path, mod cell (x, y, w, h) or None=whole, vanilla path, vanilla cell,
#  note). Cells pick a representative variant/frame of each sheet.
PAIRS = [
    ("objects/seancecircle.png", None,
     "objects/fallenaltar.png", ("auto", 32, 64), "ritual set piece (per-tile column)"),
    ("objects/veilriftdown.png", None,
     "objects/ladderdown.png", None, "descending portal"),
    ("objects/skystairwaydown.png", None,
     "objects/ladderdown.png", None, "descending portal"),
    ("objects/statues/gloomraven.png", None,
     "objects/statues/angelicstatue.png", ("auto", 64, 64), "statue (densest 64px cell)"),
    ("objects/wardencandelabra.png", (0, 0, 32, 96),
     "objects/copperstreetlamp.png", (0, 0, 32, 96), "streetlamp (on half)"),
    ("objects/mistglasslantern.png", (0, 0, 32, 32),
     "objects/walltorch.png", ("auto", 32, 32), "wall light (densest cell)"),
    ("objects/skywatchbanner.png", (0, 0, 32, 32),
     "objects/bannerofpeace.png", ("auto", 32, 32), "wall banner (densest 32px cell)"),
    ("objects/gloomshroom.png", (0, 0, 32, 32),
     "objects/mushroom.png", ("auto", 32, 32), "mushroom (densest cell)"),
    ("objects/skystonerock.png", (0, 0, 32, 64),
     "objects/caverock.png", (0, 0, 32, 64), "rock node (full column)"),
    ("objects/stormcrystal.png", (0, 0, 32, 64),
     "objects/crystalwall.png", (0, 0, 32, 48), "crystal cluster (half of 2x1)"),
    ("objects/windwheat.png", (0, 0, 32, 32),
     "objects/swampgrass.png", (0, 0, 32, 32), "grass clump"),
    ("objects/skyreeds.png", (0, 0, 32, 32),
     "objects/deepswamptallgrass.png", (0, 0, 32, 32), "tall grass"),
    ("objects/gloomwillow.png", (0, 0, 64, 112),
     "objects/deadwood.png", ("auto", 32, 112), "dead tree deco (per-tile column)"),
    ("objects/wardenbeaconoff.png", None,
     "objects/bannerstand.png", None, "tall quest prop"),
    ("mobs/skywarden.png", (0, 0, 64, 64),
     None, None, "humanoid: compare by hand vs player (28px head)"),
    # v0.5 art pass: the assets the last playtest called out as too thin.
    # Ore overlays are masked onto the rock, so compare overlay to overlay;
    # mob sheets compare their densest 64px frame against a vanilla quadruped
    # of the same footprint.
    ("objects/aetheriumore.png", ("auto", 32, 32),
     "objects/ironore.png", ("auto", 32, 32), "ore overlay (densest 32px cell)"),
    ("mobs/galehound.png", ("auto", 64, 64),
     "mobs/boar.png", ("auto", 64, 64), "quadruped mob (densest 64px frame)"),
    ("mobs/skystonegolem.png", ("auto", 64, 64),
     "mobs/boar.png", ("auto", 64, 64), "heavy mob (densest 64px frame)"),
    # v0.4 saplings (chunky mini-trees like vanilla, not thin shoots)
    ("objects/nimbussapling.png", None,
     "objects/willowsapling.png", None, "sapling"),
    ("objects/fulgursapling.png", None,
     "objects/pinesapling.png", None, "sapling"),
    ("objects/prismasapling.png", None,
     "objects/birchsapling.png", None, "sapling"),
]

THRESHOLD = 0.75

# Per-sprite accepted minimums: our silhouette is legitimately lighter than
# the closest vanilla analogue (candles + ground ring vs a solid stone altar;
# a perched bird vs a robed figure; slim crystal shards vs a crystal wall).
# Reviewed on 4x contact sheets — they read correctly in game at these masses.
ACCEPTED = {
    "objects/seancecircle.png": 0.55,
    "objects/statues/gloomraven.png": 0.65,
    "objects/stormcrystal.png": 0.70,
    # a bare weeping willow is airier than a solid vanilla dead tree
    "objects/gloomwillow.png": 0.45,
}


def measure(img, cell):
    if cell is not None and cell[0] == "auto":
        # Scan every cell of the given size and measure the DENSEST one:
        # vanilla sheets often leave the top-left cell empty (rotation rows,
        # off-states), which would make a fixed corner crop meaningless.
        _, cw, ch = cell
        best = (0, 0, 0)
        for cx in range(0, max(img.width - cw + 1, 1), cw):
            for cy in range(0, max(img.height - ch + 1, 1), ch):
                got = measure(img.crop((cx, cy, cx + cw, cy + ch)), None)
                if got[2] > best[2]:
                    best = got
        return best
    if cell is not None:
        img = img.crop((cell[0], cell[1], cell[0] + cell[2], cell[1] + cell[3]))
    px = img.load()
    xs, ys, opaque = [], [], 0
    for x in range(img.width):
        for y in range(img.height):
            if px[x, y][3] > 24:
                xs.append(x)
                ys.append(y)
                opaque += 1
    if not xs:
        return (0, 0, 0)
    return (max(xs) - min(xs) + 1, max(ys) - min(ys) + 1, opaque)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vanilla", default="/home/user/necesse-game/sprites")
    args = parser.parse_args()

    rows = []
    for ours, ocell, theirs, vcell, note in PAIRS:
        opath = os.path.join(RES, ours)
        if not os.path.exists(opath):
            rows.append((ours, note, None, "missing mod file"))
            continue
        ow, oh, oarea = measure(Image.open(opath).convert("RGBA"), ocell)
        if theirs is None:
            rows.append((ours, note, None, f"ours {ow}x{oh} ({oarea}px) - manual check"))
            continue
        vpath = os.path.join(args.vanilla, theirs)
        if not os.path.exists(vpath):
            rows.append((ours, note, None, f"vanilla ref missing: {theirs}"))
            continue
        vw, vh, varea = measure(Image.open(vpath).convert("RGBA"), vcell)
        ratio = oarea / varea if varea else 0
        limit = ACCEPTED.get(ours, THRESHOLD)
        verdict = "OK" if ratio >= limit else "FIX"
        rows.append((ours, note, ratio,
                     f"ours {ow}x{oh} ({oarea}px) vs {theirs.split('/')[-1]} {vw}x{vh} ({varea}px) -> {ratio:.2f} {verdict}"))

    flagged = 0
    for ours, note, ratio, detail in rows:
        limit = ACCEPTED.get(ours, THRESHOLD)
        mark = "!!" if ratio is not None and ratio < limit else "  "
        if ratio is not None and ratio < limit:
            flagged += 1
        print(f"{mark} {ours:40s} [{note}] {detail}")
    print(f"\n{flagged} sprite(s) flagged below {THRESHOLD:.0%} of vanilla mass.")
    return 1 if flagged else 0


if __name__ == "__main__":
    sys.exit(main())
