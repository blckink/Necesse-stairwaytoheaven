#!/usr/bin/env python3
"""Check one sheet against the contract for its type -- measured, not assumed.

Every earlier fixer here answered "is this file the right size". That is the
easy half. The half that actually breaks in game is **how the cells join**: a
tile whose right edge does not continue into its neighbour's left edge draws a
grid across the floor, and no size check sees it.

The thresholds below are not opinions. They were measured on the vanilla dump:

    tile edge continuity, mean channel difference along the joining edge
        grass_splat  R->L 0.6   B->T 1.2
        snow_splat   R->L 1.5   B->T 4.1
        dirt_splat   R->L 6.3   B->T 8.1     <- noisiest material, loosest fit
    ... so vanilla never exceeds ~8, and the four full-tile variants are
    mutually compatible: every variant's right edge continues into every
    other variant's left edge, which is what lets the engine pick one at
    random per tile without showing a seam.

Run over our own shipped splats, that same measure says 11 of 15 are outside
the band, worst `cloudturf_splat` at 28.3/21.8 -- the Skyreach's main ground.

Usage:
    python3 tools/sheet_check.py src/main/resources/tiles/skystone_splat.png
    python3 tools/sheet_check.py --all tiles          check every shipped splat
    python3 tools/sheet_check.py file.png --type wall
"""
import argparse
import glob
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VANILLA = os.environ.get("NECESSE_VANILLA_SPRITES",
                         "/home/blackoffset/dev/Necesse sprites")

# Measured on the vanilla dump; see the module docstring for the numbers.
EDGE_LIMIT = 8.5          # dirt_splat, the loosest vanilla material, sits at 8.1
COLOUR_BAND = (19, 38)    # shipped Necesse sheets

CONTRACTS = {
    # name: (width, height rule, cell, description)
    "splat": {
        "width": 224,
        "height_multiple": 96,
        "cell": 32,
        "grid": "7 columns x 3 rows per 96px variant block",
        "source": "docs/research/splat-format.md 5.3 (verified against "
                  "dirt/snow/ash splats)",
    },
    "wall": {
        "width": 352,
        "height_exact": 128,
        "cell": 16,
        "grid": "4x8@16px blob + 2x8 window insert + 8 door frames",
        "source": ".claude/skills/necesse-pixel-art (verified format law)",
    },
    "mob": {
        "width": None,
        "height_exact": None,
        "cell": None,
        "grid": "6 columns (idle, walk x4, swim) x 4 direction rows "
                "Up/Right/Down/Left; cell 64px on the standard family",
        "source": ".claude/skills/necesse-pixel-art; several sheets "
                  "deliberately break the default -- read the row in "
                  "docs/ASSET_REQUESTS.md rather than assuming",
    },
}


def infer_type(path, im):
    n = os.path.basename(path).lower()
    if "_splat" in n and im.width == 224:
        return "splat"
    if im.size == (352, 128):
        return "wall"
    if n.startswith("mobs") or "/mobs/" in path.replace(os.sep, "/"):
        return "mob"
    if im.size == (32, 32):
        return "item"
    return None


def edges(cell):
    px = cell.load()
    t = cell.width
    return {"L": [px[0, y] for y in range(t)],
            "R": [px[t - 1, y] for y in range(t)],
            "T": [px[x, 0] for x in range(t)],
            "B": [px[x, t - 1] for x in range(t)]}


def edge_diff(a, b):
    n = tot = 0
    for p, q in zip(a, b):
        if p[3] < 128 and q[3] < 128:
            continue
        tot += 1
        n += abs(p[0] - q[0]) + abs(p[1] - q[1]) + abs(p[2] - q[2])
    return n / float(tot * 3) if tot else None


def check_splat(im, path, out):
    ok = True
    c = CONTRACTS["splat"]
    if im.width != c["width"] or im.height % c["height_multiple"]:
        out.append(("FAIL", "size %dx%d -- must be %d wide and a multiple of %d tall"
                    % (im.width, im.height, c["width"], c["height_multiple"])))
        return False
    blocks = im.height // c["height_multiple"]
    out.append(("ok", "size %dx%d = %d variant block(s) of 7x3 cells"
                % (im.width, im.height, blocks)))

    t = c["cell"]

    def cell(cx, cy, block):
        return im.crop((cx * t, block * 96 + cy * t,
                        (cx + 1) * t, block * 96 + (cy + 1) * t))

    # The four full-tile variants must be mutually joinable: the engine picks
    # one at random per tile, so ANY pair can end up side by side.
    worst_h = worst_v = 0.0
    worst_pair = None
    for b in range(blocks):
        cs = [cell(3 + i, 0, b) for i in range(4)]
        es = [edges(x) for x in cs]
        for i in range(4):
            for j in range(4):
                h = edge_diff(es[i]["R"], es[j]["L"])
                v = edge_diff(es[i]["B"], es[j]["T"])
                if h is not None and h > worst_h:
                    worst_h, worst_pair = h, (b, i, j, "R->L")
                if v is not None and v > worst_v:
                    worst_v = v
    tag = "ok" if max(worst_h, worst_v) <= EDGE_LIMIT else "FAIL"
    ok &= tag == "ok"
    out.append((tag, "tile edge continuity: horizontal %.1f, vertical %.1f "
                "(vanilla stays under %.1f; grass 0.6/1.2, dirt 6.3/8.1)"
                % (worst_h, worst_v, EDGE_LIMIT)))
    if tag == "FAIL" and worst_pair:
        b, i, j, side = worst_pair
        out.append(("", "  worst join: block %d, variant %d %s variant %d -- "
                    "a visible grid on the floor" % (b, i, side, j)))

    # Variant blocks must differ, but as one material. Measured on vanilla:
    # 15-30%; a different crop of the same texture lands at 80-90%.
    if blocks > 1:
        def block_diff(a, b):
            A, B = im.crop((0, a * 96, 224, (a + 1) * 96)), im.crop((0, b * 96, 224, (b + 1) * 96))
            pa, pb = A.load(), B.load()
            d = tot = 0
            for y in range(96):
                for x in range(224):
                    p, q = pa[x, y], pb[x, y]
                    if p[3] < 128 and q[3] < 128:
                        continue
                    tot += 1
                    if abs(p[0] - q[0]) + abs(p[1] - q[1]) + abs(p[2] - q[2]) > 20:
                        d += 1
            return 100.0 * d / tot if tot else 0.0
        ds = [block_diff(0, i) for i in range(1, blocks)]
        lo, hi = min(ds), max(ds)
        if hi < 8:
            tag = "FAIL"
            note = "blocks are near-identical -- the ground will repeat 1:1"
        elif lo > 45:
            tag = "FAIL"
            note = "blocks differ like different materials, not variants"
        else:
            tag = "ok"
            note = "in vanilla's band (dirt 21.4, snow 15.1, grass 15.0/1.5/13.2)"
        ok &= tag == "ok"
        out.append((tag, "variant spread %.1f-%.1f%% -- %s" % (lo, hi, note)))

    # The four full-tile cells are the only ones that must be fully opaque.
    for b in range(blocks):
        for i in range(4):
            a = cell(3 + i, 0, b).getchannel("A")
            if a.getextrema()[0] < 250:
                out.append(("FAIL", "block %d full-tile variant %d is not fully "
                            "opaque -- it is the plain ground, holes show the void"
                            % (b, i)))
                ok = False
    return ok


def check_generic(im, path, kind, out):
    c = CONTRACTS.get(kind)
    ok = True
    if c:
        if c.get("width") and im.width != c["width"]:
            out.append(("FAIL", "width %d, contract says %d" % (im.width, c["width"])))
            ok = False
        if c.get("height_exact") and im.height != c["height_exact"]:
            out.append(("FAIL", "height %d, contract says %d"
                        % (im.height, c["height_exact"])))
            ok = False
        out.append(("", "grid: %s" % c["grid"]))
        out.append(("", "source: %s" % c["source"]))
    else:
        out.append(("", "no contract for this type yet -- size and palette only"))
    return ok


def check(path, kind=None):
    from PIL import Image
    im = Image.open(path).convert("RGBA")
    kind = kind or infer_type(path, im)
    out = []
    ok = True

    if kind == "splat":
        ok &= check_splat(im, path, out)
    else:
        ok &= check_generic(im, path, kind, out)

    colours = len(im.getcolors(1 << 24) or [])
    lo, hi = COLOUR_BAND
    if colours > hi * 3:
        out.append(("FAIL", "%d colours -- shipped Necesse sheets carry %d-%d"
                    % (colours, lo, hi)))
        ok = False
    else:
        out.append(("ok" if lo <= colours <= hi else "", "%d colours" % colours))

    if im.getchannel("A").getextrema()[0] == 255 and kind != "splat":
        out.append(("", "fully opaque -- correct only if this type has no cut-out"))

    print("%s  [%s]" % (os.path.relpath(path, REPO) if path.startswith(REPO) else path,
                        kind or "unknown type"))
    for tag, line in out:
        print("   %-4s %s" % (tag, line))
    return ok


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("files", nargs="*")
    ap.add_argument("--type", choices=sorted(CONTRACTS) + ["item"])
    ap.add_argument("--all", metavar="DIR",
                    help="check every png under src/main/resources/<DIR>")
    args = ap.parse_args()

    files = list(args.files)
    if args.all:
        files += sorted(glob.glob(os.path.join(REPO, "src/main/resources",
                                               args.all, "*.png")))
    if not files:
        ap.error("give a file or --all <dir>")

    bad = 0
    for f in files:
        try:
            if not check(f, args.type):
                bad += 1
        except Exception as exc:
            print("%s: unreadable (%s)" % (f, exc))
            bad += 1
        print()
    print("%d of %d sheet(s) fail their contract." % (bad, len(files)))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
