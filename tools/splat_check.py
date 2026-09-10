#!/usr/bin/env python3
"""The four ways a generated splat has actually broken, as one gate.

`tools/splat_from_texture.py` (commit 49ea020) exists because a model asked for
"a splat" returns the one part of the file that was never art. The commit
message names the four failures it measured on the skystone replacement, all at
once, on one file:

    wanted  224x480, alpha carrying the 21 cell shapes, a handful of colours
    got     857x1836 -- a 3.826x scale, not an integer, unrecoverable --
            220020 colours, and the transparent holes painted as black

Stamping makes all four unreachable *by construction*. This says so out loud,
per file, so "unreachable by construction" is a printed measurement rather than
an argument -- which is what the shipped sheet's own DoD needs.

    python3 tools/splat_check.py src/main/resources/tiles/emberash_splat.png \\
            --like src/main/resources/tiles/ashsand_splat.png

    ... --colours 38    the cap (shipped sheets carry 19-38)

Exit 0 when every check passes, 1 otherwise. `--like` is optional: without it
the geometry is checked against the format itself (224 wide, a multiple of 96
tall) rather than against a specific reference, which is all a sheet that was
not stamped from one can be held to.
"""
import argparse
import os
import sys

TILE = 32
WIDTH = 224           # 7 * 32 -- the engine's own "is this a splat" check
BLOCK_H = 96          # 3 rows of 32: one variant block


def load(path):
    from PIL import Image
    return Image.open(path).convert("RGBA")


def check(path, like=None, cap=38):
    im = load(path)
    name = os.path.basename(path)
    fails = []

    def say(ok, line):
        print("  %s %s" % ("ok  " if ok else "FAIL", line))
        if not ok:
            fails.append(line)

    print("%s -- %dx%d" % (name, im.width, im.height))

    # 1. Nicht ganzzahliger Massstab. A splat that was resized off its own grid
    #    cannot be recovered, so the size is checked against the format and,
    #    when there is one, against the exact reference it was stamped through.
    geom = im.width == WIDTH and im.height % BLOCK_H == 0
    say(geom, "scale: %d wide (want %d), %d tall (want a multiple of %d)"
        % (im.width, WIDTH, im.height, BLOCK_H))
    ref = None
    if like:
        ref = load(like)
        say(im.size == ref.size, "scale: size %dx%d == reference %s %dx%d"
            % (im.width, im.height, os.path.basename(like), ref.width, ref.height))

    # 2. Alpha als schwarze Flaeche. The transparent holes painted as black
    #    pixels is what a model returns when it has no alpha channel to give.
    #    Byte-identical alpha is the strong form; without a reference, the weak
    #    form is that the sheet has transparency at all and no black-and-opaque
    #    field standing in for it.
    alpha = im.getchannel("A")
    avals = list(alpha.get_flattened_data())
    clear = sum(1 for v in avals if v == 0)
    total = im.width * im.height
    if ref is not None:
        same = alpha.tobytes() == ref.getchannel("A").tobytes()
        say(same, "alpha: byte-identical to the reference")
        rvals = list(ref.getchannel("A").get_flattened_data())
        holes = [i for i, v in enumerate(rvals) if v == 0]
        bled = sum(1 for i in holes if avals[i] != 0)
        say(bled == 0, "alpha: %d of %d reference holes still transparent"
            % (len(holes) - bled, len(holes)))
    else:
        say(clear > 0, "alpha: %d transparent px (a splat is not a full rectangle)"
            % clear)
    px = im.load()
    black_op = sum(1 for y in range(im.height) for x in range(im.width)
                   if px[x, y][3] == 255 and px[x, y][:3] == (0, 0, 0))
    say(black_op * 20 < total,
        "alpha: %d opaque pure-black px (%.1f%%) -- holes painted black would be a field"
        % (black_op, 100.0 * black_op / total))

    # 3. Sechsstellige Farbzahl. Shipped Necesse sheets carry 19-38; a render
    #    carries tens of thousands. Counted on the finished file, so the
    #    stamping's own blends are included rather than sneaking past.
    colours = len(set(im.get_flattened_data()))
    say(colours <= cap, "colours: %d distinct RGBA (cap %d)" % (colours, cap))

    # 4. Verlorenes Zellraster. The 21 cells are the format: four fully opaque
    #    plain variants in row 0 columns 3-6, blend pieces everywhere else. A
    #    sheet drawn as one picture loses exactly this, and it is invisible in
    #    a thumbnail.
    blocks = im.height // BLOCK_H if geom else 0
    grid_ok = blocks > 0
    for by in range(blocks):
        for cx in range(3, 7):
            box = (cx * TILE, by * BLOCK_H, cx * TILE + TILE, by * BLOCK_H + TILE)
            cell = im.crop(box).getchannel("A")
            if min(cell.get_flattened_data()) != 255:
                grid_ok = False
                say(False, "grid: block %d plain cell (%d,0) is not fully opaque"
                    % (by, cx))
    if blocks:
        say(grid_ok, "grid: %d block(s) x 21 cells, %d plain variants each opaque"
            % (blocks, 4 * blocks))
    else:
        say(False, "grid: not a splat geometry, cells cannot be read")

    return fails


def main():
    ap = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("splats", nargs="+")
    ap.add_argument("--like", help="the correct splat whose alpha was reused")
    ap.add_argument("--colours", type=int, default=38)
    args = ap.parse_args()

    bad = 0
    for p in args.splats:
        fails = check(p, args.like, args.colours)
        bad += len(fails)
        print("")
    print("%d check(s) failed" % bad)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
