"""Terrain/liquid "_splat" atlas builder — vanilla 1.3.2 format.

Verified cell map (docs/research/splat-format.md §5.3): each 224x96 block is
a 7x3 grid of 32px cells. Cells (3..6, 0) are four fully opaque tile
variants (cell (3,0) doubles as the item icon); the remaining 17 cells are
marching-square blend pieces whose alpha must be high toward the edges named
by their trigger and low elsewhere. Width = 224 * frames (liquids animate,
terrain ships 1 frame); height = 96 * variant sections.

The material is painted once across each block (position-locked, seamless),
then masked per cell — so full tiles and blend edges always agree.
"""

import math

from px import Canvas, Rng
import palette

CELL = 32
BLOCK_W = 224
BLOCK_H = 96

# Disc anchors for the directional alpha masks, in cell-local coordinates.
_N = (16, -14, 34)
_S = (16, 46, 34)
_E = (46, 16, 34)
_W = (-14, 16, 34)

# The diagonal-only pieces are a small NUB anchored IN the named corner. They
# are drawn on a tile that only touches ours corner-to-corner, so they are the
# smallest pieces on the sheet: measured over 66 vanilla `_splat` sheets they
# cover 0.8%-29.3% of their cell (median 12.5%).
#
# They used to be discs of radius 26 parked at (8,8)/(24,24) INSIDE the cell,
# which covers 83%-89% and only bites a notch out of the far corner — the exact
# complement of the intended shape. Every one of our tiles therefore repainted
# each of its four diagonal neighbours almost completely, so laying a single
# floor tile read as a 3x3 blob. That is the "white floor places huge"
# playtest report; placement itself was always one tile. Keep these small, and
# keep tools/tile_behaviour_audit.py in the loop.
_NUB = 15
_SE = (32, 32, _NUB)
_SW = (0, 32, _NUB)
_NE = (32, 0, _NUB)
_NW = (0, 0, _NUB)

# (col,row) -> None for opaque, or list of discs to union
CELL_MASKS = {
    (3, 0): None, (4, 0): None, (5, 0): None, (6, 0): None,   # full variants
    (1, 1): None,                                              # all-4 core
    (1, 0): [_S],
    (1, 2): [_N],
    (2, 1): [_W],
    (0, 1): [_E],
    (3, 1): [_N, _W],
    (4, 1): [_N, _E],
    (3, 2): [_S, _W],
    (4, 2): [_E, _S],
    (5, 1): [_N, _E, _W],
    (6, 1): [_N, _E, _S],
    (5, 2): [_E, _S, _W],
    (6, 2): [_N, _S, _W],
    # isolated diagonal pieces: a nub in the named corner
    (0, 0): [_SE],
    (2, 0): [_SW],
    (0, 2): [_NE],
    (2, 2): [_NW],
}

# Subtractive "eyes", punched out after the additive mask.
#
# Vanilla never paints a blend piece solid once three or four sides are
# covered: it keeps a ragged eye of the UNDERLYING tile showing, pulled toward
# whichever side is still open ((1,1) is bordered on all four, so its eye sits
# dead centre). Measured vanilla coverage is 60.5%-90.2% for the three-side
# pieces and 51.6%-81.6% for (1,1). Unioning our directional discs alone gives
# 98%-100%: the S disc, say, reaches far enough to cover what the N+E+W union
# misses, so the piece goes solid and the tile underneath disappears. Without
# these eyes a lone gap inside a floor is painted over completely and the floor
# reads one tile wider than it was laid.
CELL_EYES = {
    (1, 1): (16, 16, 11),   # all four sides  -> eye centred
    (5, 1): (16, 25, 10),   # N+E+W, open S   -> eye toward S
    (6, 1): (7, 16, 10),    # N+E+S, open W   -> eye toward W
    (5, 2): (16, 7, 10),    # E+S+W, open N   -> eye toward N
    (6, 2): (25, 16, 10),   # N+S+W, open E   -> eye toward E
}


def _disc_alpha(discs, x, y, salt):
    """255 inside any disc core; dithered 4px falloff band; 0 outside."""
    best = -1.0
    for (cx, cy, r) in discs:
        d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
        best = max(best, r - d)
    if best >= 4:
        return 255
    if best <= 0:
        return 0
    t = best / 4.0
    speck = Rng((x * 7349 + y * 12611) ^ (salt * 0x9E37)).float()
    return 255 if speck < t else 0


def _mask_alpha(discs, eye, x, y, salt):
    """Additive disc union minus the cell's eye, both dithered at the rim."""
    a = 255 if discs is None else _disc_alpha(discs, x, y, salt)
    if a and eye is not None:
        a = 255 - _disc_alpha([eye], x, y, salt ^ 0x5EED)
    return a


def build_splat(path, material, variants=3, salt=0x51A7, frames=1, features=None):
    """features(block, x0, y0, salt, k): optional motif painter run on the four
    FULL-variant cells (3..6, 0) only — vanilla's trick: blend cells stay a calm
    base texture, while each full variant carries its own detail cluster."""
    sheet = Canvas(BLOCK_W * frames, BLOCK_H * variants)
    for frame in range(frames):
        for v in range(variants):
            vsalt = salt + v * 7919
            bx, by = frame * BLOCK_W, v * BLOCK_H
            # paint the material once across the whole block...
            block = Canvas(BLOCK_W, BLOCK_H)
            for cx in range(7):
                for cy in range(3):
                    material(block, cx * CELL, cy * CELL, vsalt + cx * 17 + cy * 53, frame)
            if features is not None:
                for k, fcx in enumerate((3, 4, 5, 6)):
                    features(block, fcx * CELL, 0, vsalt + 0xF17 + k * 977, k)
            # ...then mask each cell by its blend shape
            for cx in range(7):
                for cy in range(3):
                    if (cx, cy) not in CELL_MASKS:
                        continue
                    discs = CELL_MASKS[(cx, cy)]
                    eye = CELL_EYES.get((cx, cy))
                    for x in range(CELL):
                        for y in range(CELL):
                            a = _mask_alpha(discs, eye, x, y, vsalt + cx * 31 + cy * 71)
                            if a > 0:
                                r, g, b, _ = block.get(cx * CELL + x, cy * CELL + y)
                                sheet.put(bx + cx * CELL + x, by + cy * CELL + y, (r, g, b, a))
    sheet.save(path)


# --- material painters (32x32 cells, seamless via position-locked speckle) ---

# --- natural ground: clumped mottle, built the way vanilla builds it ---------
#
# Measured over vanilla-sprites/tiles/ (grass, dirt, ash, rock, snow,
# deepstonefloor): vanilla natural ground is NOT per-pixel speckle. It is a
# low-contrast MOTTLE of 2-5px clumps covering roughly a third of the cell,
# sometimes with a darker contour network of plate/crack lines laid over it.
# grass_splat is one darker-green clump layer at 29% coverage and nothing
# else; deepstonefloor adds rectangular slab outlines; ash adds big lumps.
# Detail density (pixels that are not the cell's modal colour, out of 1024)
# runs 294 on snow — the calmest natural ground in the game — to 993.
#
# Our three sky grounds used to be `_speckle_cell(density=0.04..0.06)`, i.e.
# 4-6% stray pixels, and measured 63/70/88. That is a fifth of vanilla's
# quietest ground: from a few tiles away the Driftlands read as flat colour.
#
# Everything below is a function of (gx, gy) = position modulo the 32px tile
# and of the per-SECTION salt only. Two consequences, both required:
#  * the field wraps exactly on both axes, so neighbouring tiles never seam;
#  * every cell of a 224x96 block paints the same material, so the marching
#    -square blend pieces still match the full-tile variants they fade into.

_MOTTLE_CACHE = {}


def _block_salt(x0, y0, salt):
    """Recover the per-section salt this cell's salt was derived from.

    Mirrors build_splat's `material(block, cx*CELL, cy*CELL,
    vsalt + cx*17 + cy*53, frame)`. A material must not vary from cell to cell
    inside a block, so it works from this value and never from the raw salt.
    """
    return salt - (x0 // CELL) * 17 - (y0 // CELL) * 53


def _mottle(salt, blur=2, passes=2):
    """Seamless 32x32 clump field, wrapping on both axes.

    White noise smoothed by wrapping box blurs: `blur` sets the clump radius
    (1 -> ~3px specks, 4 -> broad drifts), `passes` how round they come out.
    Cached: build_splat calls a material 21 times per block.
    """
    key = (salt, blur, passes)
    got = _MOTTLE_CACHE.get(key)
    if got is not None:
        return got
    f = [[Rng((x * 7349 + y * 12611) ^ ((salt * 2654435761) & 0xFFFFFFFF)).float()
          for x in range(32)] for y in range(32)]
    span = 2 * blur + 1
    area = float(span * span)
    for _ in range(passes):
        g = [[0.0] * 32 for _ in range(32)]
        for y in range(32):
            for x in range(32):
                s = 0.0
                for dy in range(-blur, blur + 1):
                    row = f[(y + dy) % 32]
                    for dx in range(-blur, blur + 1):
                        s += row[(x + dx) % 32]
                g[y][x] = s / area
        f = g
    _MOTTLE_CACHE[key] = f
    return f


def _blend_fields(fine, coarse, w):
    """Clumps riding on broad tonal drift — one field, so coverage stays exact."""
    return [[(1.0 - w) * fine[y][x] + w * coarse[y][x] for x in range(32)]
            for y in range(32)]


def _bander(field, fracs):
    """Quantile thresholds -> a band(gx, gy) function.

    `fracs` are cumulative coverages, so the tone mix is set in percent of the
    cell rather than guessed from threshold values, and it is identical in
    every section however that section's noise happened to fall.
    """
    vals = sorted(v for row in field for v in row)
    cuts = [vals[min(1023, int(f * 1024))] for f in fracs]

    def band(gx, gy):
        v = field[gy][gx]
        b = 0
        for cut in cuts:
            if v < cut:
                break
            b += 1
        return b
    return band


def _contour(band, gx, gy):
    """True on a band boundary: the plate/crack network vanilla stone floors
    draw over their mottle. Boundaries of a wrapping field are closed loops,
    so they cross tile edges without a seam."""
    b = band(gx, gy)
    return (band((gx + 1) % 32, gy) != b) or (band(gx, (gy + 1) % 32) != b)


# --- the 2x2 block grid: vanilla's actual tone unit ---------------------------
#
# Measured over every vanilla natural-ground `_splat` (grass, dirt, ash, mud,
# rock, snow): the four full-variant cells are 100.0% coherent on the 2x2 pixel
# block grid. Not "mostly" — every single 2x2 block on the even grid is one
# flat colour, features included. Vanilla ground is authored at half
# resolution and doubled; a lone off-tone pixel does not exist in it.
#
# Our sheets measured 75%/48%/48%, i.e. per-pixel mottle, and that is half of
# why they read as static rather than as ground. Everything below therefore
# evaluates its noise, its contours and its hashes at BLOCK coordinates
# (gx & ~1, gy & ~1) and fills all four pixels with the result. 32 is even, so
# the block grid survives the modulo wrap and cells still start on a block
# boundary — seamlessness is unaffected.
#
# The second half is contrast. Vanilla carries a whole ground texture inside a
# handful of RGB levels (grass 5, mud 12, rock 13); our beds ran 27-35 mean
# because the ramps had no near-base step. The `grain_d`/`grain_l` palette
# steps (~7 RGB either side of base) exist for exactly this: they carry the
# BULK of the bed, while `deep`/`light`/`hi` are spent only on the features.


def _blk(gx, gy):
    """Snap a tile-local coordinate pair onto the 2x2 block grid."""
    return gx & ~1, gy & ~1


def _bander_blocks(field, fracs):
    """`_bander`, but quantised over the 256 blocks rather than 1024 pixels.

    Taking the cuts from the block sublattice is what makes the coverage
    percentages exact once the field is only ever sampled at block origins —
    pixel quantiles would drift by a few percent and put the density gate out
    of reach by luck of the noise.
    """
    vals = sorted(field[y][x] for y in range(0, 32, 2) for x in range(0, 32, 2))
    cuts = [vals[min(255, int(f * 256))] for f in fracs]

    def band(bx, by):
        v = field[by][bx]
        b = 0
        for cut in cuts:
            if v < cut:
                break
            b += 1
        return b
    return band


def _contour_blocks(band, bx, by):
    """`_contour` on the block grid: steps two pixels, so the seam it marks is
    a 2px-wide line rather than a 1px hairline that would break coherence."""
    b = band(bx, by)
    return (band((bx + 2) % 32, by) != b) or (band(bx, (by + 2) % 32) != b)


def _block_snap(features):
    """Wrap a `features_*` painter so its motifs land on the 2x2 block grid.

    The motifs themselves are untouched — this is a dilation, not a redraw: any
    block containing at least one painted pixel is filled with that pixel's
    colour, so a 1px crack becomes a 2px crack and a lone speck becomes a 2x2
    dot. That is what vanilla debris looks like up close, and it is the only
    way a cell that carries features can be 100% block-coherent.

    Ties are broken by frequency and then by colour tuple order, never by
    iteration accident: same seed -> same bytes stays true.
    """
    def wrapped(c, x0, y0, salt, k):
        overlay = Canvas(c.width, c.height)
        features(overlay, x0, y0, salt, k)
        for by in range(0, c.height - 1, 2):
            for bx in range(0, c.width - 1, 2):
                votes = {}
                for dy in (0, 1):
                    for dx in (0, 1):
                        px = overlay.get(bx + dx, by + dy)
                        if px[3] > 0:
                            votes[px] = votes.get(px, 0) + 1
                if not votes:
                    continue
                col = max(sorted(votes), key=votes.get)
                for dy in (0, 1):
                    for dx in (0, 1):
                        c.put(bx + dx, by + dy, col)
    wrapped.__name__ = getattr(features, "__name__", "features") + "_blocked"
    return wrapped


def _speckle_cell(c, x0, y0, ramp, salt, density=0.10):
    for x in range(32):
        for y in range(32):
            gx, gy = (x0 + x) % 32, (y0 + y) % 32
            r = Rng((gx * 7349 + gy * 12611) ^ (salt & 0xFFFF0000))
            v = r.float()
            if v < density * 0.45:
                c.put(x0 + x, y0 + y, ramp["deep"])
            elif v < density:
                c.put(x0 + x, y0 + y, ramp["light"])
            else:
                c.put(x0 + x, y0 + y, ramp["base"])


def material_cloudturf(c, x0, y0, salt, frame=0):
    """Silver-green meadow turf, built like vanilla `grass_splat`: a quiet
    clumped mottle of slightly darker and slightly lighter turf riding on a
    broad tonal drift, with a lit lip along the top of every shadowed clump so
    the ground reads as tussocky rather than as noise.

    The whole bed lives on `grain_d`/`grain_l`, ~7 RGB either side of base.
    Vanilla `grass_splat` carries its ENTIRE texture inside 5 RGB levels at 300
    off-modal pixels; this cell answers with 7 levels at ~350. The previous
    version put the bulk on `light` (32 RGB away) and measured a mean deviation
    of 35 — seven times vanilla's, which is why it read as camouflage blotches
    instead of ground. `light`/`deep`/`hi`/`tuft` are now spent exclusively on
    the features, where their contrast buys a readable motif.
    """
    ramp = palette.CLOUDTURF
    vs = _block_salt(x0, y0, salt)
    field = _blend_fields(_mottle(vs ^ 0xC10D, blur=2, passes=2),
                          _mottle(vs ^ 0x9A55, blur=4, passes=2), 0.34)
    # 17% shadow clumps, 66% base, 17% lit turf, measured on the block grid.
    band = _bander_blocks(field, (0.17, 0.83))
    tone = (ramp["grain_d"], ramp["base"], ramp["grain_l"])
    for x in range(32):
        for y in range(32):
            gx, gy = (x0 + x) % 32, (y0 + y) % 32
            bx, by = _blk(gx, gy)
            b = band(bx, by)
            col = tone[b]
            # sunlit lip where a hollow gives way upward: a structural edge
            # that turns a dark clump into a blade cluster. It repaints dark
            # blocks as light ones, so the off-base coverage is unchanged.
            if b == 0 and band(bx, (by - 2) % 32) > 0:
                col = ramp["grain_l"]
            c.put(x0 + x, y0 + y, col)


def features_cloudturf(c, x0, y0, salt, k):
    ramp = palette.CLOUDTURF
    rng = Rng(salt)

    def tuft(x, y):
        # chunky 4px-tall blade cluster with rooted shadow (vanilla-scale)
        lean = rng.pick((-1, 1))
        c.put(x - 1, y, ramp["tuft"])
        c.put(x, y, ramp["tuft"])
        c.put(x + 1, y, ramp["tuft"])
        c.put(x - lean, y - 1, ramp["tuft"])
        c.put(x + lean, y - 1, ramp["tuft"])
        c.put(x + lean, y - 2, ramp["tuft"])
        c.put(x + lean * 2, y - 3, ramp["hi"])
        c.put(x - 1, y + 1, ramp["deep"])
        c.put(x, y + 1, ramp["deep"])

    if k == 0:  # plain turf: the bed alone, like skystone's and slate's k == 0
        # This used to drop a single `hi` speck here. `hi` is 64 RGB from base,
        # and cell (3,0) is the variant the contrast gate measures, so that one
        # pixel set the whole tile's max deviation at 64 against vanilla
        # ground's 5-33. The variant stays distinct: k=1..3 all carry motifs.
        return
    if k == 1:  # two tuft clusters, each of two neighboring tufts
        for _ in range(2):
            tx, ty = x0 + rng.range(6, 24), y0 + rng.range(7, 26)
            tuft(tx, ty)
            tuft(tx + rng.range(4, 7), ty + rng.pick((-2, 2)))
    elif k == 2:  # soft cloud-moss patch
        mx, my = x0 + rng.range(10, 21), y0 + rng.range(10, 21)
        c.blob(mx, my, 4, ramp["light"], rng, lumps=3)
        c.blob(mx - 1, my - 1, 2, ramp["hi"], rng, lumps=2)
        c.put(mx + 3, my + 3, ramp["deep"])
        for _ in range(3):
            c.put(x0 + rng.range(3, 28), y0 + rng.range(3, 28), ramp["hi"])
    else:  # tuft + pale pebbles
        tuft(x0 + rng.range(5, 26), y0 + rng.range(6, 26))
        for _ in range(3):
            px, py = x0 + rng.range(3, 27), y0 + rng.range(3, 27)
            c.put(px, py, ramp["light"])
            c.put(px + 1, py, ramp["deep"])


def material_skystone(c, x0, y0, salt, frame=0):
    """Pale weathered skystone, built like vanilla `rock_splat`: a clumped grit
    mottle with a broken plate network drawn over it. The plates come from the
    contour of a coarse field, so they are closed loops that cross tile borders
    — the same read as vanilla's slab outlines, without hand-placing a line
    that would repeat every 32px.

    Both the grit and the seams are drawn in `grain_d`/`grain_l` (~7 RGB from
    base). Vanilla `rock_splat` runs mean 9.7 / max 13 over five tones; the
    seams used to be `deep` and `light`, 35 and 28 RGB out, which is what made
    this tile read as crazy paving seen through static. The loud steps are the
    features' budget now.
    """
    ramp = palette.SKYSTONE
    vs = _block_salt(x0, y0, salt)
    grit = _blend_fields(_mottle(vs ^ 0x51A9, blur=1, passes=2),
                         _mottle(vs ^ 0x33C7, blur=4, passes=2), 0.40)
    # 13% shadow pockets, 74% base, 13% lit grit — leaner than cloudturf's bed
    # because the plate seams below spend the rest of the density budget.
    band = _bander_blocks(grit, (0.13, 0.87))
    tone = (ramp["grain_d"], ramp["base"], ramp["grain_l"])
    plates = _mottle(vs ^ 0x7E11, blur=6, passes=2)
    pband = _bander_blocks(plates, (0.5,))
    for x in range(32):
        for y in range(32):
            gx, gy = (x0 + x) % 32, (y0 + y) % 32
            bx, by = _blk(gx, gy)
            col = tone[band(bx, by)]
            # Plate seams are BROKEN, not a continuous web: an unbroken contour
            # network reads as crazy paving, which is a floor pattern, not
            # weathered ground. The gate is hashed at BLOCK coordinates, so a
            # seam block is broken or kept whole — never half of each, which
            # would put a lone pixel back on the grid.
            seam = Rng((bx * 4177 + by * 8623) ^ 0x7E11BEEF).float()
            if _contour_blocks(pband, bx, by) and seam < 0.45:
                col = ramp["grain_d"]
            elif _contour_blocks(pband, bx, (by + 2) % 32) and seam < 0.25:
                col = ramp["grain_l"]                  # lit lip above the seam
            c.put(x0 + x, y0 + y, col)


def features_skystone(c, x0, y0, salt, k):
    ramp = palette.SKYSTONE
    rng = Rng(salt)

    def crack(x, y, length):
        step = rng.pick((-1, 1))
        for i in range(length):
            c.put(x + i, y, ramp["deep"])
            if rng.chance(0.45):
                y += step
        c.put(x + length - 1, y + 1, ramp["light"])

    if k == 0:  # plain slab
        return
    if k == 1:  # one long meandering fissure
        crack(x0 + rng.range(3, 12), y0 + rng.range(8, 24), rng.range(9, 14))
    elif k == 2:  # chipped facet + pebbles
        fx, fy = x0 + rng.range(8, 20), y0 + rng.range(8, 20)
        for i in range(4):
            for j in range(4 - i):
                c.put(fx + i, fy + j, ramp["light"])
        c.put(fx, fy, ramp["hi"])
        c.put(fx + 2, fy + 4, ramp["deep"])
        c.put(fx + 4, fy + 2, ramp["deep"])
        for _ in range(2):
            px, py = x0 + rng.range(3, 27), y0 + rng.range(3, 27)
            c.put(px, py, ramp["light"])
            c.put(px + 1, py + 1, ramp["deep"])
    else:  # two hairline cracks + a glint
        crack(x0 + rng.range(3, 12), y0 + rng.range(5, 14), rng.range(6, 9))
        crack(x0 + rng.range(8, 18), y0 + rng.range(18, 26), rng.range(6, 9))
        c.put(x0 + rng.range(6, 26), y0 + rng.range(6, 26), ramp["hi"])


def material_stormslate(c, x0, y0, salt, frame=0):
    """Layered night-violet slate: a clumped mineral bed with the tile's
    signature dashed diagonal strata (period 16, which divides the 32px tile,
    so it stays seamless) drawn over it.

    v0.5 halved the speckle because the old double-noise read as static, then
    the density pass overcorrected into a `deep`/`light` field 27 RGB from base
    — loud enough to camouflage anything standing on it. The strata stay, they
    are what makes this ground read as SLATE, but bed and strata are both drawn
    in `grain_d`/`grain_l` now. Compare vanilla `dirt_splat`, whose whole
    texture is four tones inside 33 RGB, and `mud_splat`, eleven tones inside
    12.
    """
    ramp = palette.STORMSLATE
    vs = _block_salt(x0, y0, salt)
    field = _blend_fields(_mottle(vs ^ 0x570A, blur=1, passes=2),
                          _mottle(vs ^ 0x2B93, blur=4, passes=2), 0.42)
    # 15% shadowed bedding, 70% base, 15% lit face; the strata add the rest.
    band = _bander_blocks(field, (0.15, 0.85))
    tone = (ramp["grain_d"], ramp["base"], ramp["grain_l"])
    for x in range(32):
        for y in range(32):
            gx, gy = (x0 + x) % 32, (y0 + y) % 32
            bx, by = _blk(gx, gy)
            c.put(x0 + x, y0 + y, tone[band(bx, by)])
    for x in range(32):
        for y in range(32):
            gx, gy = (x0 + x) % 32, (y0 + y) % 32
            bx, by = _blk(gx, gy)
            # On the block grid (bx + by) is always even, so the lit dash sits
            # at m == 2 — at m == 1 it would be dead code and the strata would
            # lose their sunlit side entirely.
            m = (bx + by) % 16
            h = Rng((bx * 5081 + by * 947) ^ 0x570A5EED)
            # The shadowed bedding line is the ONE step this material spends
            # outside the grain: `deep` sits exactly 25 RGB from base, the
            # contrast ceiling, and vanilla `rock_splat` runs a mean of 9.7 for
            # the same reason. Drawn in `grain_d` the strata vanished, which
            # cost the tile the layered read it is named for. The lit side
            # stays on `grain_l`: `light` is 27 RGB out and would break the
            # ceiling. Recolouring these blocks costs no density — they are
            # already off-modal either way.
            if m == 0 and h.chance(0.45):
                c.put(x0 + x, y0 + y, ramp["deep"])
            elif m == 2 and h.chance(0.18):
                c.put(x0 + x, y0 + y, ramp["grain_l"])


def features_stormslate(c, x0, y0, salt, k):
    ramp = palette.STORMSLATE
    rng = Rng(salt)
    if k == 0:
        return
    if k == 1:  # charge vein: crack with electric violet pinpricks
        x, y = x0 + rng.range(4, 14), y0 + rng.range(6, 22)
        for i in range(rng.range(8, 12)):
            c.put(x + i, y, ramp["deep"])
            if rng.chance(0.5):
                y += rng.pick((-1, 1))
            if i in (2, 5, 8):
                c.put(x + i, y, ramp["charge"])
        c.put(x + 1, y - 1, ramp["hi"])
    elif k == 2:  # raised shard ridge catching the light
        rx, ry = x0 + rng.range(4, 16), y0 + rng.range(8, 22)
        w = rng.range(7, 11)
        for i in range(w):
            c.put(rx + i, ry, ramp["light"])
            c.put(rx + i, ry + 1, ramp["deep"])
        c.put(rx, ry, ramp["hi"])
    else:  # short crack + lone charge spark + pebble
        x, y = x0 + rng.range(5, 18), y0 + rng.range(5, 24)
        for i in range(rng.range(5, 8)):
            c.put(x + i, y + i // 2, ramp["deep"])
        c.put(x0 + rng.range(4, 27), y0 + rng.range(4, 27), ramp["charge"])
        c.put(x0 + rng.range(4, 27), y0 + rng.range(4, 27), ramp["light"])


def material_gloomwood(c, x0, y0, salt, frame=0):
    """Dark plank floor: horizontal boards, staggered seams, sparse grain."""
    ramp = palette.GLOOMWOOD
    rng = Rng(salt)
    for x in range(32):
        for y in range(32):
            gy = (y0 + y) % 32
            board = gy // 8
            tone = ramp["base"] if board % 2 == 0 else ramp["light"]
            if gy % 8 == 0:
                tone = ramp["deep"]
            c.put(x0 + x, y0 + y, tone)
    for board in range(4):
        seam_x = (Rng(0x600D + board * 31 + (y0 // 96) * 7).next() % 4) * 8 + 4
        for yy in range(board * 8 + 1, board * 8 + 8):
            c.put(x0 + (seam_x + x0) % 32, y0 + yy, ramp["deep"])
    for _ in range(rng.range(1, 2)):
        c.put(x0 + rng.range(1, 30), y0 + rng.range(1, 30),
              ramp["hi"] if rng.chance(0.3) else ramp["deep"])


def features_gloomwood(c, x0, y0, salt, k):
    ramp = palette.GLOOMWOOD
    rng = Rng(salt)
    if k == 0:
        return
    if k == 1:  # knot in one board
        kx, ky = x0 + rng.range(6, 24), y0 + rng.range(0, 3) * 8 + 4
        c.put(kx, ky, ramp["deep"])
        c.put(kx + 1, ky, ramp["deep"])
        c.put(kx - 1, ky, ramp["light"])
        c.put(kx + 2, ky, ramp["light"])
        c.put(kx, ky - 1, ramp["light"])
        c.put(kx, ky + 1, ramp["deep"])
    elif k == 2:  # nail heads at a board seam
        for _ in range(2):
            nx, ny = x0 + rng.range(4, 27), y0 + rng.range(0, 3) * 8 + 1
            c.put(nx, ny, ramp["hi"])
            c.put(nx + 1, ny + 1, ramp["deep"])
    else:  # grain streaks along a board
        by = y0 + rng.range(0, 3) * 8 + rng.range(2, 6)
        for _ in range(3):
            gx = x0 + rng.range(2, 24)
            for i in range(rng.range(3, 6)):
                c.put(gx + i, by, ramp["deep"])
            by += rng.pick((-1, 1))


# --- Mistsea: a rolling cloud deck ------------------------------------------
#
# How the engine plays these frames (TerrainSplatterTile.getSplattingTexture,
# read from the decompiled source): for a `frames`-wide _splat it computes
#     frame = GameUtils.getAnim(localTime, frames * 2 - 2, frames * 400)
#     if frame >= frames: frame = frames * 2 - frame - 2
# so our 8 frames run 0,1..7,6..1 — a PING-PONG over 3.2 s, ~229 ms a step.
# Motion therefore has to read naturally played in either direction: a roll and
# a swell, never a one-way conveyor.
#
# Two seamlessness rules follow, and the code below obeys both strictly:
#  * every spatial term is an integer harmonic of the 32 px tile, so the field
#    wraps exactly in x and y — neighbouring tiles never show a seam;
#  * every per-frame motion is a multiple of 4 px, so eight frames advance
#    exactly 32 px = one full tile period. Frame 8 lands back on frame 0, so
#    the loop closes whether the engine ping-pongs it or cycles it.
#
# Why this was rebuilt: the previous field was a three-layer puff mottle
# translating 4 px a frame. It measured as moving (13 mean abs RGB delta per
# frame, five times vanilla water) yet did not READ as moving, because nothing
# in it was larger than a few pixels — there was no edge for the eye to track,
# so it only shimmered. The dominant layer is now a long rolling ridge that
# stays continuous across tile borders, so the whole deck visibly rolls.

_FIELD_CACHE = {}
_TWO_PI = math.pi * 2.0


def _billow_centres(salt, grid, rmin, rmax, layer_salt):
    centres = []
    step = 32.0 / grid
    for i in range(grid):
        for j in range(grid):
            r = Rng((salt >> 16) ^ (i * 733 + j * 2711 + layer_salt) * 40503)
            cx = (i + 0.5) * step + (r.float() - 0.5) * step * 0.9
            cy = (j + 0.5) * step + (r.float() - 0.5) * step * 0.9
            centres.append((cx % 32, cy % 32, rmin + r.float() * (rmax - rmin)))
    return centres


def _torus_d(ax, ay, bx, by):
    dx = abs(ax - bx)
    dy = abs(ay - by)
    return (min(dx, 32 - dx) ** 2 + min(dy, 32 - dy) ** 2) ** 0.5


def _mist_field(salt, frame):
    """32x32 cloud-deck height field for one frame, normalised to about 0..1.

    roll   long rolling ridges advancing exactly one ridge spacing per 8
           frames. This is the readable structure: because it is built from
           integer harmonics it stays continuous across tile borders, so a
           whole screen of Mistsea rolls as one surface.
    big    toroidal billow masses drifting 4 px/frame, breaking the ridges into
           organic lobes so the roll never reads as a sine grating.
    mid    a second billow layer drifting 4 px/frame diagonally, so lobes
           deform as they travel instead of sliding rigidly.
    fine   a low-weight detail layer counter-drifting 4 px/frame: texture
           without noise.
    """
    key = (salt, frame)
    cached = _FIELD_CACHE.get(key)
    if cached is not None:
        return cached

    big_c = _billow_centres(salt, 2, 13, 18, 0)
    mid_c = _billow_centres(salt, 3, 8, 11, 0x77)
    fine_c = _billow_centres(salt, 6, 3, 5, 0x33)
    phase = _TWO_PI * frame / 8.0
    dx_big = (frame * 4) % 32
    dx_mid = (frame * 4) % 32
    dy_mid = (frame * 4) % 32
    dx_fine = (-frame * 4) % 32

    field = [[0.0] * 32 for _ in range(32)]
    for y in range(32):
        for x in range(32):
            # meandering ridge: both warp terms are integer harmonics of 32,
            # so adding them inside the phase keeps the field exactly seamless
            warp = (4.4 * math.sin(_TWO_PI * y / 32.0)
                    + 2.2 * math.sin(_TWO_PI * 2 * x / 32.0)
                    + 1.4 * math.sin(_TWO_PI * 3 * y / 32.0 + 1.7))
            roll = 0.5 + 0.5 * math.sin(_TWO_PI * ((x + warp) + y) / 32.0 + phase)
            bx = (x + dx_big) % 32
            big = 0.0
            for (cx, cy, r) in big_c:
                big = max(big, 1.0 - _torus_d(bx, y, cx, cy) / r)
            mx, my = (x + dx_mid) % 32, (y + dy_mid) % 32
            mid = 0.0
            for (cx, cy, r) in mid_c:
                mid = max(mid, 1.0 - _torus_d(mx, my, cx, cy) / r)
            fx = (x + dx_fine) % 32
            fine = 0.0
            for (cx, cy, r) in fine_c:
                fine = max(fine, 1.0 - _torus_d(fx, y, cx, cy) / r)
            field[y][x] = (0.72 * roll + 0.18 * max(0.0, big)
                           + 0.07 * max(0.0, mid) + 0.03 * max(0.0, fine))
    _FIELD_CACHE[key] = field
    return field


# Band edges are quantiles of the field measured over all 8 frames, chosen so
# the two mid-light tones carry the surface (deep 7%, base 17%, light 28%,
# hi 28%, top 22%). A background surface the player stands on wants its mass
# in the middle of the ramp: the dark trough and the sunlit crest are accents
# that make the roll readable, not the bulk of the picture.
_MIST_BANDS = (0.14, 0.24, 0.50, 0.79)


def material_mist(deep):
    """The Mistsea as a rolling CLOUD deck, not water: sunlit crests riding
    over shadowed troughs, the whole deck rolling one ridge-spacing per loop.
    Deep = the open cloudsea (full contrast between crest and trough);
    shallow = the thin shore band, compressed to the light end of the ramp and
    combed by wisps that travel twice as fast as the deck itself."""
    # build_splat hands the painter a DIFFERENT salt per cell (cx*17 + cy*53);
    # deriving the cloud layout from it would give every cell its own billows
    # and seam them together. One constant per surface instead — and the two
    # surfaces get different constants, so the shore band is not just a
    # recoloured copy of the open cloudsea it borders.
    field_salt = 0x51D30000 if deep else 0x2A870000
    # One banding of the field, two tone LUTs. The shore band is THIN mist, so
    # it maps the same five bands onto a compressed, lighter run of the ramp —
    # fewer distinct steps means fewer hard contours, which is what keeps the
    # shallow reading as haze instead of as a graphic lattice.
    ramp = palette.MISTSEA
    if deep:
        lut = (ramp["deep"], ramp["base"], ramp["light"], ramp["hi"], ramp["top"])
    else:
        lut = (ramp["base"], ramp["light"], ramp["light"], ramp["hi"], ramp["top"])
    wisp_gate = 0.955 if deep else 0.930

    def painter(c, x0, y0, salt, frame=0):
        field = _mist_field(field_salt, frame)
        wphase = _TWO_PI * 2.0 * frame / 8.0   # 2 cycles per loop: still exact

        def band(gx, gy):
            v = field[gy % 32][gx % 32]
            for i, edge in enumerate(_MIST_BANDS):
                if v < edge:
                    return i
            return 4

        def edge_gap(gx, gy, b):
            """How close this pixel sits to the ramp border above/below it."""
            v = field[gy % 32][gx % 32]
            up = _MIST_BANDS[b] - v if b < 4 else 9.0
            dn = v - _MIST_BANDS[b - 1] if b > 0 else 9.0
            return up, dn

        for x in range(32):
            for y in range(32):
                gx, gy = (x0 + x) % 32, (y0 + y) % 32
                b = band(gx, gy)
                above = band(gx, gy - 1)
                tone = b
                # sunlit crest: the pixel where a lobe first rises above its
                # neighbour to the north. It travels with the roll, and that
                # travelling highlight is what makes the motion legible.
                if b >= 3 and above < b:
                    tone = min(4, b + 1)
                # and the shaded underside of the lobe above it
                elif b <= 1 and above > b:
                    tone = max(0, b - 1)
                # wisps: thin filaments combing across the deck at twice the
                # deck's own rate. Integer harmonics, so seamless; 2 cycles per
                # 8 frames, so they land back on frame 0 with the rest.
                elif b >= 2 and math.sin(_TWO_PI * (2 * gx - gy) / 32.0 + wphase) > wisp_gate:
                    tone = min(4, b + 1)
                else:
                    # sparse single-pixel dither, ONLY at the ramp borders —
                    # the house rule, and here it does real work: it breaks the
                    # smooth band contours into pixel art and stops the deck
                    # looking airbrushed. Position-locked, so it stays seamless;
                    # the borders themselves travel, so the dither travels too.
                    up, dn = edge_gap(gx, gy, b)
                    if up < 0.024 and (gx + gy) % 2 == 0:
                        tone = b + 1
                    elif dn < 0.024 and (gx + gy) % 2 == 1:
                        tone = b - 1
                c.put(x0 + x, y0 + y, lut[tone])
    return painter


# --- v0.4 buildable wood floors (gloomwoodfloor pattern: position-locked
# boards + staggered seams; character lives in the full-variant features) ----

def _plank_flecks(c, x0, y0, salt, ramp, chance_hi=0.3):
    """1-2 free speckles per cell (the only non-position-locked detail,
    same budget the gloomwood floor uses)."""
    rng = Rng(salt)
    for _ in range(rng.range(1, 2)):
        c.put(x0 + rng.range(1, 30), y0 + rng.range(1, 30),
              ramp["hi"] if rng.chance(chance_hi) else ramp["deep"])


def material_nimbusfloor(c, x0, y0, salt, frame=0):
    """Pale nimbuswood planks: horizontal boards, staggered seams, a sunlit
    lip on alternating boards. Boards stay CALM (vanilla floor rule); grain
    is a few position-locked dashes per board, not per-pixel noise."""
    ramp = palette.NIMBUSWOOD
    for x in range(32):
        for y in range(32):
            gy = (y0 + y) % 32
            board = gy // 8
            tone = ramp["base"] if board % 2 == 0 else ramp["light"]
            if gy % 8 == 0:
                tone = ramp["deep"]
            elif gy % 8 == 1 and board % 2 == 1:
                tone = ramp["hi"]                     # sunlit plank lip
            c.put(x0 + x, y0 + y, tone)
    for board in range(4):
        h = Rng(0x81B + board * 31 + (y0 // 96) * 7)
        seam_x = (h.next() % 4) * 8 + 4
        for yy in range(board * 8 + 1, board * 8 + 8):
            c.put(x0 + (seam_x + x0) % 32, y0 + yy, ramp["deep"])
        for _ in range(2):                            # grain dashes
            gx = h.next() % 26 + 2
            gy2 = board * 8 + 3 + h.next() % 4
            for i in range(3 + h.next() % 2):
                c.put(x0 + (gx + i) % 32, y0 + gy2, ramp["deep"])
    _plank_flecks(c, x0, y0, salt, ramp)


def features_nimbusfloor(c, x0, y0, salt, k):
    ramp = palette.NIMBUSWOOD
    rng = Rng(salt)
    if k == 0:
        return
    if k == 1:  # knot in one board
        kx, ky = x0 + rng.range(6, 24), y0 + rng.range(0, 3) * 8 + 4
        c.put(kx, ky, ramp["deep"])
        c.put(kx + 1, ky, ramp["deep"])
        c.put(kx - 1, ky, ramp["light"])
        c.put(kx + 2, ky, ramp["light"])
        c.put(kx, ky - 1, ramp["hi"])
        c.put(kx, ky + 1, ramp["deep"])
    elif k == 2:  # pegs at a board seam
        for _ in range(2):
            nx, ny = x0 + rng.range(4, 27), y0 + rng.range(0, 3) * 8 + 1
            c.put(nx, ny, ramp["hi"])
            c.put(nx + 1, ny + 1, ramp["deep"])
    else:  # wandering grain streaks along a board
        by = y0 + rng.range(0, 3) * 8 + rng.range(2, 6)
        for _ in range(3):
            gx = x0 + rng.range(2, 24)
            for i in range(rng.range(3, 6)):
                c.put(gx + i, by, ramp["deep"])
            by += rng.pick((-1, 1))


def material_charfloor(c, x0, y0, salt, frame=0):
    """Charwood planks: VERTICAL boards (turned 90 degrees from the pale
    floors — each wood reads distinct at a glance). Calm boards; char grain
    is a few position-locked vertical scorch dashes per board."""
    ramp = palette.CHARWOOD
    for x in range(32):
        for y in range(32):
            gx = (x0 + x) % 32
            board = gx // 8
            tone = ramp["base"] if board % 2 == 0 else ramp["light"]
            if gx % 8 == 0:
                tone = ramp["deep"]
            elif gx % 8 == 1 and board % 2 == 0:
                tone = ramp["light"]                  # lit left plank edge
            c.put(x0 + x, y0 + y, tone)
    for board in range(4):
        h = Rng(0xC4A + board * 31 + (y0 // 96) * 7)
        seam_y = (h.next() % 4) * 8 + 4
        for xx in range(board * 8 + 1, board * 8 + 8):
            c.put(x0 + xx, y0 + (seam_y + y0) % 32, ramp["deep"])
        for _ in range(2):                            # vertical scorch dashes
            gy = h.next() % 26 + 2
            gx2 = board * 8 + 3 + h.next() % 4
            for i in range(3 + h.next() % 2):
                c.put(x0 + gx2, y0 + (gy + i) % 32, ramp["deep"])
    _plank_flecks(c, x0, y0, salt, ramp, chance_hi=0.25)


def features_charfloor(c, x0, y0, salt, k):
    ramp = palette.CHARWOOD
    rng = Rng(salt)
    if k == 0:
        return
    if k == 1:  # charred crack down a board
        cx = x0 + rng.range(0, 3) * 8 + rng.range(2, 6)
        cy = y0 + rng.range(4, 14)
        for i in range(rng.range(8, 12)):
            c.put(cx, cy + i, ramp["deep"])
            if rng.chance(0.4):
                cx += rng.pick((-1, 1))
        c.put(cx + 1, cy + 4, ramp["hi"])
    elif k == 2:  # one dying ember caught in the grain (this floor's charm)
        ex, ey = x0 + rng.range(6, 25), y0 + rng.range(6, 25)
        c.put(ex, ey, ramp["ember"])
        c.put(ex + 1, ey, ramp["deep"])
        c.put(ex - 1, ey + 1, ramp["deep"])
        c.put(ex, ey - 1, ramp["hi"])
    else:  # scorch streaks + a peg
        bx = x0 + rng.range(0, 3) * 8 + rng.range(2, 5)
        by = y0 + rng.range(3, 20)
        for i in range(rng.range(3, 5)):
            c.put(bx, by + i, ramp["deep"])
            c.put(bx + 1, by + i + 1, ramp["deep"])
        c.put(x0 + rng.range(4, 27), y0 + rng.range(4, 27), ramp["hi"])


def material_prismfloor(c, x0, y0, salt, frame=0):
    """Polished prismwood planks: horizontal boards; each board carries one
    long position-locked sheen streak (polished glint) and one grain dash —
    calm otherwise."""
    ramp = palette.PRISMWOOD
    for x in range(32):
        for y in range(32):
            gy = (y0 + y) % 32
            board = gy // 8
            tone = ramp["base"] if board % 2 == 0 else ramp["light"]
            if gy % 8 == 0:
                tone = ramp["deep"]
            c.put(x0 + x, y0 + y, tone)
    for board in range(4):
        h = Rng(0x981 + board * 31 + (y0 // 96) * 7)
        seam_x = (h.next() % 4) * 8 + 4
        for yy in range(board * 8 + 1, board * 8 + 8):
            c.put(x0 + (seam_x + x0) % 32, y0 + yy, ramp["deep"])
        gx = h.next() % 22 + 2                        # polished sheen streak
        gy2 = board * 8 + 2 + h.next() % 2
        for i in range(5 + h.next() % 3):
            c.put(x0 + (gx + i) % 32, y0 + gy2, ramp["hi"])
        dx_ = h.next() % 24 + 3                       # one grain dash
        dy_ = board * 8 + 5 + h.next() % 2
        for i in range(3):
            c.put(x0 + (dx_ + i) % 32, y0 + dy_, ramp["deep"])
    _plank_flecks(c, x0, y0, salt, ramp, chance_hi=0.45)


def features_prismfloor(c, x0, y0, salt, k):
    ramp = palette.PRISMWOOD
    leaf = palette.PRISMLEAF
    rng = Rng(salt)
    if k == 0:
        return
    if k == 1:  # iridescent inlay glints at a seam (teal + rose, sparse)
        nx, ny = x0 + rng.range(5, 24), y0 + rng.range(0, 3) * 8 + 3
        c.put(nx, ny, leaf["teal"])
        c.put(nx + 1, ny + 1, ramp["deep"])
        mx, my = x0 + rng.range(5, 24), y0 + rng.range(0, 3) * 8 + 5
        c.put(mx, my, leaf["rose"])
        c.put(mx - 1, my + 1, ramp["deep"])
    elif k == 2:  # pale knot with a bright ring
        kx, ky = x0 + rng.range(6, 24), y0 + rng.range(0, 3) * 8 + 4
        c.put(kx, ky, ramp["deep"])
        c.put(kx + 1, ky, ramp["deep"])
        c.put(kx - 1, ky, ramp["hi"])
        c.put(kx + 2, ky, ramp["hi"])
        c.put(kx, ky - 1, ramp["hi"])
        c.put(kx, ky + 1, ramp["light"])
    else:  # long polished glint streak
        by = y0 + rng.range(0, 3) * 8 + 2
        gx = x0 + rng.range(3, 18)
        for i in range(rng.range(5, 9)):
            c.put(gx + i, by, ramp["hi"])
        c.put(gx - 1, by + 1, ramp["deep"])


# Vanilla's features are block-aligned too (ash's debris, grass's tufts): the
# 2x2 unit is the whole sheet's, not just the bed's. Wrapping here rather than
# inside build_splat keeps every other material on this sheet — and gen_veil's,
# which call build_splat too — bit-for-bit unchanged.
features_cloudturf = _block_snap(features_cloudturf)
features_skystone = _block_snap(features_skystone)
features_stormslate = _block_snap(features_stormslate)
