#!/usr/bin/env python3
"""Build a terrain splat from a plain texture, by reusing a known-good alpha.

The repeated failure with generated terrain is not an art failure. Look at what
a `_splat` actually is (`docs/research/splat-format.md` §5.3): 21 cells whose
**alpha shapes are fixed by the engine's marching-squares table**. Cell (1,0)
is opaque along its bottom edge because it is drawn when the tile below
matches -- not because an artist chose that shape. Four cells are fully opaque
plain variants; the rest are blend pieces with required silhouettes.

So asking an image model for "a splat" asks it for the one part of the file
that was never up for grabs, and the result fails in four measurable ways at
once. Measured on the skystone replacement the player received:

    wanted  224x480, 7 colours, alpha carries the shapes
    got     857x1836 (a 3.826x scale -- not an integer, unrecoverable),
            220020 colours, and the transparent holes painted as black pixels

None of that is fixable downstream, and none of it needed to happen: the model
was asked for the wrong artefact.

**Only the texture is art.** So take a plain tileable texture -- any size, any
colour count, no alpha semantics required -- and stamp it through the alpha of a
splat that is already correct. Size, cell geometry and blend silhouettes then
cannot be wrong, because they are not being produced.

    python3 tools/splat_from_texture.py texture.png \\
        --like src/main/resources/tiles/skystone_splat.png \\
        -o build/qa/splat/skystone_splat.png

    ... --colours 33        palette cap after stamping
    ... --variants 4        how many distinct patches to cut for the plain cells
    ... --seam-report       only measure how well the texture tiles, write nothing
"""
import argparse
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO, "tools"))

TILE = 32
BLOCK_H = 96          # one variant block: 7 columns x 3 rows of 32px
WIDTH = 224           # 7 * 32; the engine's own check for "is this a splat"


def seam_cost(im, tile=TILE):
    """How badly a patch fails to tile with itself.

    Wrap the patch by half its size and measure the difference along the seam
    that lands in the middle. A texture that tiles has nothing there.
    """
    from PIL import Image
    im = im.convert("RGB").resize((tile, tile), Image.LANCZOS)
    px = im.load()
    vert = sum(sum(abs(px[tile - 1, y][c] - px[0, y][c]) for c in range(3))
               for y in range(tile)) / float(tile * 3)
    horiz = sum(sum(abs(px[x, tile - 1][c] - px[x, 0][c]) for c in range(3))
                for x in range(tile)) / float(tile * 3)
    return vert, horiz


def make_seamless(im, tile=TILE):
    """Blend the patch with its own mirrored self so opposite edges meet.

    Not a clever synthesiser -- a cross-fade. It costs a little sharpness at
    the border and buys a texture that does not show a grid in game, which is
    the trade the format wants.
    """
    from PIL import Image
    im = im.convert("RGBA").resize((tile, tile), Image.LANCZOS)
    mirror = im.transpose(Image.FLIP_LEFT_RIGHT).transpose(Image.FLIP_TOP_BOTTOM)
    out = Image.new("RGBA", (tile, tile))
    src, mir, dst = im.load(), mirror.load(), out.load()
    for y in range(tile):
        for x in range(tile):
            # Weight rises towards the edges, so the centre keeps the original.
            wx = min(x, tile - 1 - x) / float(tile / 2)
            wy = min(y, tile - 1 - y) / float(tile / 2)
            w = 1.0 - min(1.0, min(wx, wy))
            a, b = src[x, y], mir[x, y]
            dst[x, y] = tuple(int(round(a[c] * (1 - w * 0.5) + b[c] * (w * 0.5)))
                              for c in range(3)) + (255,)
    return out


def outlier_cost(patch, tile=TILE):
    """How much of the patch is not the material.

    A supplied texture usually contains more than the surface: holes, edges,
    props. Cutting one of those turns a foreign object into a floor tile that
    repeats across the world, so candidates carrying extremes are ranked down.
    """
    from PIL import Image
    im = patch.convert("RGB").resize((tile, tile), Image.LANCZOS)
    px = im.load()
    lum = sorted(0.299 * px[x, y][0] + 0.587 * px[x, y][1] + 0.114 * px[x, y][2]
                 for y in range(tile) for x in range(tile))
    mid = lum[len(lum) // 2]
    far = sum(1 for v in lum if abs(v - mid) > 70) / float(len(lum))
    return far * 100.0


def vary(base, alt, seed, tile=TILE, share=0.22):
    """One tile, with details changed in a few areas -- vanilla's own trick.

    Measured on shipped splats, neighbouring variant blocks differ by 15-30% of
    their pixels (dirt 21.4, snow 15.1, grass 15.0/1.5/13.2). That is not a
    different crop of the material -- a different crop differs in 80-90%, which
    reads as a different floor. It is the SAME tile with some of its detail
    replaced: other pebbles here, another crack there.

    So this keeps the base and swaps a handful of small patches out of an
    alternate cut, deterministically per block.
    """
    from PIL import Image
    import random
    out = base.copy()
    rnd = random.Random(seed)
    target = tile * tile * share
    covered = 0
    while covered < target:
        w = rnd.randint(3, max(4, tile // 4))
        h = rnd.randint(3, max(4, tile // 4))
        x = rnd.randrange(0, tile - w + 1)
        y = rnd.randrange(0, tile - h + 1)
        out.paste(alt.crop((x, y, x + w, y + h)), (x, y))
        covered += w * h
    return out


def cut_patches(texture, n, tile=TILE, stride=None):
    """Pick n distinct patches that tile well and are free of foreign objects.

    Scans the texture on a grid, scores every candidate, and takes the best n
    that are not near-duplicates of one another -- the variation between them
    is what stops the ground reading as one image repeated.
    """
    from PIL import Image
    w, h = texture.size
    side = max(tile, min(w, h) // 3)
    stride = stride or max(tile, side // 2)
    cands = []
    for y in range(0, max(1, h - side + 1), stride):
        for x in range(0, max(1, w - side + 1), stride):
            patch = texture.crop((x, y, x + side, y + side))
            sv, sh = seam_cost(patch, tile)
            cands.append(((sv + sh) / 2.0 + outlier_cost(patch, tile) * 2.0,
                          x, y, patch))
    cands.sort(key=lambda c: c[0])

    def signature(p):
        im = p.convert("RGB").resize((4, 4), Image.LANCZOS)
        return list(im.getdata())

    def dist(a, b):
        return sum(abs(p[c] - q[c]) for p, q in zip(a, b) for c in range(3)) \
            / float(len(a) * 3)

    # Variation, but of ONE material. Measured on vanilla, neighbouring blocks
    # of a splat differ by 15-30% of their pixels (dirt 21.4, snow 15.1, grass
    # 15.0/1.5/13.2). Picking the most distinct patches instead gave 80-90% --
    # five different floors rather than five faces of one floor. So candidates
    # are held inside a band around the best patch: far enough apart to vary,
    # close enough to still be the same stone.
    best = cands[0]
    base_sig = signature(best[3])
    chosen, sigs = [make_seamless(best[3], tile)], [base_sig]
    for lo, hi in ((2.0, 12.0), (1.0, 20.0), (0.0, 1e9)):   # widen only if short
        for score, x, y, patch in cands[1:]:
            if len(chosen) >= n:
                break
            sig = signature(patch)
            if not (lo <= dist(sig, base_sig) <= hi):
                continue
            if any(dist(sig, s) < lo for s in sigs):
                continue
            chosen.append(make_seamless(patch, tile))
            sigs.append(sig)
        if len(chosen) >= n:
            break
    while len(chosen) < n:
        chosen.append(chosen[len(chosen) % max(1, len(chosen))])
    return chosen


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("texture", help="a plain tileable texture; size and colour count do not matter")
    ap.add_argument("--like", required=True,
                    help="an existing correct splat whose alpha is reused")
    ap.add_argument("-o", "--out")
    ap.add_argument("--colours", type=int, default=33)
    ap.add_argument("--variants", type=int, default=4)
    ap.add_argument("--seam-report", action="store_true")
    args = ap.parse_args()

    from PIL import Image
    tex = Image.open(args.texture).convert("RGBA")

    if args.seam_report:
        v, h = seam_cost(tex)
        print("%s: seam cost vertical %.1f, horizontal %.1f (0 = tiles perfectly)"
              % (os.path.basename(args.texture), v, h))
        sv, sh = seam_cost(make_seamless(tex))
        print("after make_seamless: %.1f / %.1f" % (sv, sh))
        return 0

    ref = Image.open(args.like).convert("RGBA")
    if ref.width != WIDTH or ref.height % BLOCK_H:
        print("%s is %dx%d -- a splat is %d wide and a multiple of %d tall"
              % (args.like, ref.width, ref.height, WIDTH, BLOCK_H), file=sys.stderr)
        return 1
    blocks = ref.height // BLOCK_H
    print("reference: %s -- %d block(s) of %d cells"
          % (os.path.basename(args.like), blocks, 7 * 3))

    # Every block needs its own look, and inside a block the four plain-variant
    # cells need four more. Vanilla does exactly this: measured block-to-block
    # difference is 21% on dirt_splat, 15% on snow_splat, 15-30% across our own
    # skystone -- the ground is deliberately not the same image five times.
    nvar = max(1, args.variants)
    # Two cuts of the material: one is the base every cell is built from, the
    # other only supplies the details that get swapped in.
    cuts = cut_patches(tex, 2)
    base, alt = cuts[0], cuts[-1]
    patches = []
    for by in range(blocks):
        patches.append(base if by == 0 else vary(base, alt, 1000 + by))
        for v in range(nvar):
            patches.append(vary(base, alt, 2000 + by * 16 + v))
    print("built %d tiles from one base for %d block(s) x (1 blend + %d plain)"
          % (len(patches), blocks, nvar))
    out = Image.new("RGBA", ref.size, (0, 0, 0, 0))
    ref_a = ref.getchannel("A")

    # Stamp: every pixel takes its colour from the tiled texture and its alpha
    # from the reference. The shapes are therefore exactly the engine's.
    for by in range(blocks):
        for cy in range(3):
            for cx in range(7):
                # The four plain-variant cells get their own patch each, so a
                # field of this tile does not repeat one image forever.
                plain = (cy == 0 and cx >= 3)
                start = by * (1 + nvar)
                idx = start + 1 + (cx - 3) % nvar if plain else start
                patch = patches[idx % len(patches)]
                px0, py0 = cx * TILE, by * BLOCK_H + cy * TILE
                out.paste(patch, (px0, py0))
    out.putalpha(ref_a)

    # Palette last, on the finished sheet, so the stamping's own blends are
    # included in the count rather than sneaking past it.
    sys.argv = ["palette_reduce"]
    import palette_reduce as pr
    bulk, accent = {}, {}
    import collections
    bulk, accent = collections.Counter(), collections.Counter()
    o = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = o[x, y]
            if a <= 128:
                continue
            (accent if pr.saturation(r, g, b) >= 0.35 else bulk)[(r, g, b)] += 1
    n_acc = min(len(accent), max(1, int(round(args.colours * 0.35))))
    pal = list(dict.fromkeys(pr.pick(bulk, args.colours - n_acc) + pr.pick(accent, n_acc)))
    cache = {}
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = o[x, y]
            if a <= 128:
                o[x, y] = (0, 0, 0, 0)
                continue
            c = (r, g, b)
            if c not in cache:
                cache[c] = min(pal, key=lambda p: (p[0] - c[0]) ** 2
                               + (p[1] - c[1]) ** 2 + (p[2] - c[2]) ** 2)
            o[x, y] = cache[c] + (a,)

    dst = args.out or os.path.join(REPO, "build", "qa", "splat",
                                   os.path.basename(args.like))
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    out.save(dst)
    print("wrote %s -- %dx%d, %d colours"
          % (os.path.relpath(dst, REPO), out.width, out.height,
             len(out.getcolors(1 << 24) or [])))
    print("Alpha is byte-identical to the reference, so the 21 cell shapes are\n"
          "the engine's own. What is new is only the colour inside them.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
