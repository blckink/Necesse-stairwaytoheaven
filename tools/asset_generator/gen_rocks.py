"""Rock autotile sheets in the vanilla RockObject quadrant format.

Decoded from RockObject.addRockDrawables (see docs/research/asset-formats.md):
- the sheet is a grid of 16x16 cells
- width = variants * 32 (each variant = two 16px half-columns: left, right)
- 13 rows with fixed meanings; every tile on screen is assembled from four
  16px quadrants picked by neighbor adjacency:

  row  meaning (for the LEFT half; the right half mirrors "outer side")
   0   top cap, outer side exposed (rounded outer-top corner)
   1   top half: interior below a top neighbor, outer side exposed
   2   bottom half (has bottom neighbor), outer side exposed
   3   front face upper, outer side exposed
   4   front face lower (ground contact), outer side exposed
   5   top cap, side continues (straight top edge)
   6   top half: full interior
   7   bottom half: full interior
   8   front face upper, side continues
   9   front face lower, side continues
  10   bottom half: inner corner (side continues, diagonal missing)
  11   top half: outer side exposed, but diagonal neighbor connects
  12   top half: inner corner (side continues, diagonal missing)

The ore overlay sheet (RockOreObject) uses the exact same grid; the engine
draws it with identical indices, so veins stay glued to the rock. The top-left
32x32 of variant 0 is also the source of the auto-generated ore item icon.

v0.6 rock family (playtest: "rectangular tombstones", "shadows far too long
and dark"). Three fixes, all measured off vanilla rock.png:

1. FORM, not jitter: 8 variants each carry a geological character (slab,
   strata, boulder domes, fracture crack, split fissure, rubble chips,
   weathered pits, broken terrace) painted into every quadrant, plus 1-2px
   bites carved out of exposed top/side edges so the perimeter is irregular.
2. LIGHT faces: the old face cells were ~86% ramp-deep — one hard dark band
   across every rock, which read as a long black shadow. Faces are now
   base-dominant with deep edges, like vanilla.
3. GROUNDED base: vanilla bakes a soft contact skirt into the bottom face
   rows (row 9 measured: alpha 195/195/113/78/55/29 over the last 6px, and
   NO bottom outline). We bake the same fade instead of a hard outline, so
   the rock dissolves into its shadow the way vanilla's does.

The engine picks the variant PER TILE: RockObject computes
`variants = texture.getWidth() / 32` and draws
`seeded(getTileSeed(x, y) * 4621).nextInt(variants) * 2` (verified by
disassembling Server.jar 1.3.2), so any variant count works without code
changes. Neighbouring tiles of one formation mix variants freely, which is
what breaks the "same slab everywhere" repetition.
"""

from px import Canvas, Rng, with_alpha
import palette

ROWS = 13
CELL = 16

# Vanilla's universal shadow tone (same constant gen_trees uses).
SHADOW = (18, 32, 32)
# Bottom fade profile for the ground-contact rows 4/9, measured off vanilla
# rock.png row 9: y10..15 -> alpha 195,195,113,78,55,29 and no outline.
_FADE = {10: 195, 11: 195, 12: 113, 13: 78, 14: 55, 15: 29}


def _surface(canvas, x0, y0, ramp, salt):
    """16x16 seamless top-surface fill (phase-locked speckle)."""
    for x in range(CELL):
        for y in range(CELL):
            r = Rng((x * 7349 + y * 12611) ^ salt)
            v = r.float()
            if v < 0.05:
                canvas.put(x0 + x, y0 + y, ramp["deep"])
            elif v < 0.11:
                canvas.put(x0 + x, y0 + y, ramp["light"])
            else:
                canvas.put(x0 + x, y0 + y, ramp["base"])


def _face(canvas, x0, y0, ramp, salt, upper):
    """16x16 front-cliff fill. v0.6: base-dominant (the old 86%-deep fill
    was the 'long dark shadow' band the playtest flagged); the upper row
    carries the lit lip where the top surface overhangs it."""
    for x in range(CELL):
        for y in range(CELL):
            r = Rng((x * 9173 + y * 14591) ^ salt ^ 0xFACE)
            v = r.float()
            base = ramp["base"]
            if v < 0.22:
                base = ramp["deep"]
            elif v < 0.28:
                base = ramp["light"]
            canvas.put(x0 + x, y0 + y, base)
    if upper:
        for x in range(CELL):
            canvas.put(x0 + x, y0, ramp["light"])
            if Rng(x * 331 ^ salt).chance(0.5):
                canvas.put(x0 + x, y0 + 1, ramp["base"])


# --- per-variant geological character ----------------------------------------
#
# Each painter decorates ONE 16x16 quadrant for a variant. They never touch
# fills/outlines/adjacency (that stays in _cell), so every variant still
# tiles correctly in all 13 adjacency rows. All randomness flows from
# (salt, variant, row) so output stays deterministic.

def _crack_line(c, ramp, rng, horizontal=True):
    """One wandering 1px deep crack with a 1px lit lip below/beside it."""
    x, y = rng.range(2, 13), rng.range(2, 13)
    for _ in range(rng.range(7, 12)):
        c.put(x, y, ramp["deep"])
        if horizontal:
            if c.filled(x, y + 1):
                c.put(x, y + 1, ramp["light"])
            x += rng.pick((1, 1, 0))
            if x > 15:
                break
            y = max(0, min(15, y + rng.pick((-1, 0, 0, 1))))
        else:
            if c.filled(x + 1, y):
                c.put(x + 1, y, ramp["light"])
            y += rng.pick((1, 1, 0))
            if y > 15:
                break
            x = max(0, min(15, x + rng.pick((-1, 0, 0, 1))))


def _fissure(c, ramp, rng):
    """2-3px split with a lit inner lip — reads as the stone having parted."""
    x = rng.range(3, 12)
    for y in range(CELL):
        w = 2 + (1 if (y // 4) % 2 == 0 else 0)
        for dx in range(w):
            if 0 <= x + dx < CELL:
                c.put(x + dx, y, ramp["deep"])
        if 0 <= x + w < CELL and c.filled(x + w, y):
            c.put(x + w, y, ramp["light"])
        x = max(1, min(13, x + rng.pick((-1, 0, 0, 1))))


def _strata(c, ramp, salt, face=False):
    """Sediment bands: two lit bands and one deep band with dithered edges."""
    for band_y in (3, 9, 13):
        tone = ramp["deep"] if band_y == 9 else ramp["light"]
        for x in range(CELL):
            r = Rng((x * 613 ^ salt) + band_y)
            if r.chance(0.85):
                c.put(x, band_y, tone)
            elif r.chance(0.4) and c.filled(x, band_y + 1):
                c.put(x, band_y + 1, tone)


def _domes(c, ramp, rng):
    """Shaded boulder domes: deep crease arcs with lit top-left crescents."""
    import math
    for _ in range(2):
        dx, dy = rng.range(4, 11), rng.range(4, 11)
        r = rng.range(2, 4)
        for deg in range(150, 340, 12):
            px_ = round(dx + r * 0.8 * math.cos(math.radians(deg)))
            py_ = round(dy + r * 0.7 * math.sin(math.radians(deg)))
            if c.filled(px_, py_):
                c.put(px_, py_, ramp["deep"])
        for deg in range(200, 300, 14):
            px_ = round(dx - 1 + (r - 1) * math.cos(math.radians(deg)))
            py_ = round(dy - 1 + (r - 1) * math.sin(math.radians(deg)))
            if c.filled(px_, py_):
                c.put(px_, py_, ramp["light"])


def _chips(c, ramp, rng, n):
    """Loose stones lying on the surface: 1-3px, lit top, deep rim."""
    for _ in range(n):
        x, y = rng.range(1, 13), rng.range(2, 13)
        w = rng.range(1, 3)
        for dx in range(w):
            if c.filled(x + dx, y):
                c.put(x + dx, y, ramp["light"])
                if c.filled(x + dx, y + 1):
                    c.put(x + dx, y + 1, ramp["deep"])


def _pits(c, ramp, rng):
    """Weathering: pale patches and small deep pits."""
    for _ in range(3):
        x, y = rng.range(1, 12), rng.range(1, 12)
        for dx in range(rng.range(2, 4)):
            for dy in range(2):
                if c.filled(x + dx, y + dy):
                    c.put(x + dx, y + dy, ramp["light"])
    for _ in range(4):
        x, y = rng.range(1, 14), rng.range(1, 14)
        if c.filled(x, y):
            c.put(x, y, ramp["deep"])
            if rng.chance(0.5) and c.filled(x + 1, y + 1):
                c.put(x + 1, y + 1, ramp["deep"])


def _decorate(c, ramp, variant, salt, face):
    """Dispatch one quadrant's character feature for `variant`."""
    rng = Rng(salt ^ (0xC0DE + variant * 7919) ^ (0xF if face else 0))
    if variant == 0:                                   # slab
        if not face:
            _crack_line(c, ramp, rng, horizontal=True)
    elif variant == 1:                                 # strata
        _strata(c, ramp, salt, face)
    elif variant == 2:                                 # boulder domes
        _domes(c, ramp, rng)
    elif variant == 3:                                 # fracture
        _crack_line(c, ramp, rng, horizontal=rng.chance(0.5))
        if rng.chance(0.6):
            _crack_line(c, ramp, rng, horizontal=rng.chance(0.5))
    elif variant == 4:                                 # split
        if face:
            _crack_line(c, ramp, rng, horizontal=False)
        else:
            _fissure(c, ramp, rng)
    elif variant == 5:                                 # rubble
        _chips(c, ramp, rng, rng.range(4, 7))
    elif variant == 6:                                 # weathered pits
        _pits(c, ramp, rng)
    else:                                              # broken terrace
        _strata(c, ramp, salt, face)
        _chips(c, ramp, rng, 2)


def _carve_edge(c, salt, variant, row):
    """Bite 1-2px out of exposed edges so silhouettes stop reading as ruled
    rectangles. Only rows with a genuinely exposed side/top are carved
    (rows 0-4 and 11: outer side; rows 0/5: top). Bites get a fresh 1px
    inner outline so they read as chipped edges, not holes."""
    rng = Rng(salt ^ (0xB17E + variant * 3571) ^ (row * 97))

    if row in (0, 1, 2, 3, 4, 11):                     # side bites
        for _ in range(rng.range(1, 3)):
            y = rng.range(2, CELL - 3)
            depth = 1
            h = rng.range(2, 5)
            for dy in range(h):
                c.put(0, min(CELL - 1, y + dy), (0, 0, 0, 0))
            for dy in range(-1, h + 1):
                yy = min(CELL - 1, max(0, y + dy))
                if c.filled(1, yy):
                    c.put(1, yy, palette.OUTLINE)
    if row in (0, 5):                                  # top notches
        for _ in range(rng.range(1, 3)):
            x = rng.range(3, CELL - 4)
            w = rng.range(2, 5)
            for dx in range(w):
                c.put(x + dx, 0, (0, 0, 0, 0))
            for dx in range(-1, w + 1):
                xx = min(CELL - 1, max(0, x + dx))
                if c.filled(xx, 1):
                    c.put(xx, 1, palette.OUTLINE)


def _cell(ramp, salt, row, outer_left, variant=0):
    """Render one LEFT-half cell; the right half is its mirror."""
    c = Canvas(CELL, CELL)
    o = palette.OUTLINE
    is_face = row in (3, 4, 8, 9)
    if is_face:
        _face(c, 0, 0, ramp, salt + row * 31, upper=row in (3, 8))
    else:
        _surface(c, 0, 0, ramp, salt)

    # geological character (before outlines so features stay inside)
    _decorate(c, ramp, variant, salt + row * 31, face=is_face)

    side_exposed = row in (0, 1, 2, 3, 4, 11)
    if row in (0, 5):  # top cap: outline + lit rim just below
        for x in range(CELL):
            c.put(x, 0, o)
            c.put(x, 1, ramp["hi"])
    if side_exposed:
        for y in range(CELL):
            c.put(0, y, o)
            if not is_face:
                c.put(1, y, ramp["light"])
    if row == 0:  # rounded outer-top corner
        c.put(0, 0, (0, 0, 0, 0))
        c.put(1, 0, o)
        c.put(0, 1, o)
        c.put(1, 1, o)
        c.put(2, 1, ramp["hi"])
        c.put(1, 2, ramp["light"])
    if row == 11:  # side exposed but diagonal connects: soften the top of the edge
        c.put(0, 0, ramp["base"])
        c.put(1, 0, ramp["base"])
    if row == 12:  # inner corner at top-outer
        c.put(0, 0, o)
        c.put(1, 0, o)
        c.put(0, 1, o)
    if row == 10:  # inner corner at bottom-outer
        c.put(0, CELL - 1, o)
        c.put(1, CELL - 1, o)
        c.put(0, CELL - 2, o)
    if row == 4:  # rounded outer-bottom corner of the face
        c.put(0, CELL - 1, (0, 0, 0, 0))
        c.put(1, CELL - 1, o)
        c.put(0, CELL - 2, o)

    # perimeter carving (after outline so bites get a fresh inner edge)
    _carve_edge(c, salt, variant, row)

    # vanilla contact skirt: the bottom face rows fade out through
    # alpha 195/195/113/78/55/29 with NO hard bottom outline (measured off
    # vanilla row 9). This is what makes rocks sit ON the ground instead of
    # ending in a dark rule — the playtest's "long dark shadow".
    if row in (4, 9):
        for y in _FADE:
            for x in range(CELL):
                if c.filled(x, y):
                    c.put(x, y, with_alpha(SHADOW, _FADE[y]))
    return c if outer_left else c.mirrored()


def gen_rock_sheet(path, ramp, variants=8, salt=0xACE):
    """v0.6: 8 geological variants by default (vanilla rock ships 4, vanilla
    caverock 8; we shipped 2, which the playtest read as repetition)."""
    sheet = Canvas(variants * 32, ROWS * CELL)
    for v in range(variants):
        vsalt = salt + v * 7919
        for row in range(ROWS):
            left = _cell(ramp, vsalt, row, outer_left=True, variant=v)
            right = _cell(ramp, vsalt + 13, row, outer_left=False, variant=v)
            sheet.paste(left, v * 32, row * CELL)
            sheet.paste(right, v * 32 + CELL, row * CELL)
    sheet.save(path)



# --- ore overlay ------------------------------------------------------------
#
# Engine contract, read from the decompiled RockOreObject.loadTextures():
# objects/<ore>.png is a strip of N 32x32 pattern cells (width/32 == N). For
# each variant i the engine COPIES objects/oremask.png (128x208 — the rock
# quadrant grid itself) and multiplies pattern cell i over it in 32x32 steps,
# colour AND alpha. Rendering then indexes that masked texture with the exact
# same 16px quadrant coordinates as the rock, so ore stays glued to stone.
#
# Three consequences drive the art:
#  * oremask rows 6/7 (full rock interior) are alpha 255 and are the common
#    on-screen case, so a 32x32 pattern reproduces intact there. Rows 3/4/8/9
#    (the cliff faces) are alpha 80 -> face ore renders at ~31%; the density
#    that players read has to live on the top surface.
#  * rock row r samples the pattern's TOP half when r is even and its BOTTOM
#    half when r is odd, and neighbour adjacency decides which rows pair up —
#    the two halves get recombined in either order. Every 16x16 quadrant must
#    therefore stand on its own, so one crystal cluster is anchored per
#    quadrant rather than composing a single centred motif.
#  * the left/right 16px halves of a variant are always drawn side by side, so
#    detail may cross x=16 freely; crossing y=16 must stay rare (vanilla
#    ironore crosses on only 6 columns).
#
# Size law: vanilla objects/ironore.png carries 328 / 292 opaque px per 32x32
# cell (30% coverage) and copperore 364 / 380. The old speckle-scatter version
# of this sheet sat at 120 px (11%), which is why the node read as empty.

# The pattern is authored TOROIDALLY (see _put). Both axes matter: on screen a
# tile draws pattern x 0-15 at drawX and x 16-31 at drawX+16 while the next
# tile starts again at x 0, and rows 6/7 put pattern y 0-15 above y 16-31 with
# the tile below repeating from y 0 — so the pattern already tiles at 32px in
# both axes across a rock body. Wrapping it means the vein reads as one
# continuous field instead of a grid of stamps, and it also makes the
# half-swapped cases (cliff-face rows 3/4) join cleanly.

# Crystal shard stamps. Vanilla crystal ores (objects/frostshardore.png,
# glacialore.png) are chunky 45-degree bars with a pale core down the axis, a
# lit facet on one long side and a dark rim on the other — not round nuggets.
# L = lit facet, H = bright core, B = shadow facet, D = deep contact rim.
# "NE" bars run up-right, so their up-left long side takes the light; "SE" bars
# run down-right and are edge-on to it, so the light rides their upper side.
_FAT_NE = (
    ".....LHHBBB",
    "....LHHBBB.",
    "...LHHBBB..",
    "..LHHBBB...",
    ".LHHBBB....",
    "LHHBBB.....",
)
_FAT_SE = (
    "BBBHHL.....",
    ".BBBHHL....",
    "..BBBHHL...",
    "...BBBHHL..",
    "....BBBHHL.",
    ".....BBBHHL",
)
_LONG_NE = (
    "......LHBB",
    ".....LHBB.",
    "....LHBB..",
    "...LHBB...",
    "..LHBB....",
    ".LHBB.....",
    "LHBB......",
)
_LONG_SE = (
    "BBHL......",
    ".BBHL.....",
    "..BBHL....",
    "...BBHL...",
    "....BBHL..",
    ".....BBHL.",
    "......BBHL",
)
_STEEP_NE = (
    "...LHBB",
    "...LHBB",
    "..LHBB.",
    "..LHBB.",
    ".LHBB..",
    ".LHBB..",
)
_STEEP_SE = (
    "BBHL...",
    "BBHL...",
    ".BBHL..",
    ".BBHL..",
    "..BBHL.",
    "..BBHL.",
)
_SHORT_NE = (
    "....LHB",
    "...LHB.",
    "..LHB..",
    ".LHB...",
    "LHB....",
)
_SHORT_SE = (
    "BHL....",
    ".BHL...",
    "..BHL..",
    "...BHL.",
    "....BHL",
)
_CHIP = (
    "LH",
    "BB",
)
_GRAIN = (
    "LB",
)

# grouped by chunk: FAT reads as a solid crystal block, MED/STEEP as slivers,
# SHORT as the small crystals crowding a cluster's root. Mixing all three is
# what stops the field reading as uniform diagonal hatching.
_FAT = (_FAT_NE, _FAT_SE)
_MED = (_LONG_NE, _LONG_SE, _STEEP_NE, _STEEP_SE)
_SHORT = (_SHORT_NE, _SHORT_SE)


def _put(c, x, y, col):
    c.put(x % 32, y % 32, col)


def _stamp(c, gem, x, y, ramp, rim=True):
    """Blit a shard. `rim` lays the ramp's deep tone into the empty pixels
    around it FIRST — the soft dark contour the style guide asks for. It is
    what makes a bright crystal read against pale skystone as well as against
    dark rock, and it stops adjacent shards fusing into one pale mass. It never
    overwrites anything already drawn, so seams and earlier crystals survive."""
    tones = {"H": ramp["hi"], "L": ramp["light"], "B": ramp["base"], "D": ramp["deep"]}
    cells = [(x + i, y + j, tones[ch])
             for j, line in enumerate(gem)
             for i, ch in enumerate(line) if ch != "."]
    if rim:
        body = {(px % 32, py % 32) for px, py, _ in cells}
        for px, py, _ in cells:
            for nx, ny in ((px - 1, py), (px + 1, py), (px, py - 1), (px, py + 1)):
                if (nx % 32, ny % 32) in body or c.filled(nx % 32, ny % 32):
                    continue
                _put(c, nx, ny, ramp["deep"])
    for px, py, col in cells:
        _put(c, px, py, col)


def _cluster(c, cx, cy, ramp, rng, big):
    """A long shard with a shorter one nested alongside it (vanilla's paired
    -shard idiom) plus a chip at the root — the chunky cluster silhouette."""
    k = rng.range(0, 1)                      # 0 = runs up-right, 1 = down-right
    main = _FAT[k] if big else rng.pick(_MED if rng.chance(0.65) else _SHORT)
    _stamp(c, main, cx - len(main[0]) // 2, cy - len(main) // 2, ramp)
    if big:
        # nest the second shard ALONGSIDE the first (offset perpendicular to
        # the bar), never through it — overlapping stamps read as mush
        d = rng.pick((-7, -6, 6, 7))
        ox, oy = (d, d) if k == 0 else (d, -d)
        second = _SHORT[k]
        _stamp(c, second, cx + ox - len(second[0]) // 2, cy + oy - len(second) // 2, ramp)
    if rng.chance(0.45):
        _stamp(c, _CHIP, cx + rng.range(-8, 6), cy + rng.range(-8, 6), ramp)


def _seam(c, x, y, dx, dy, length, ramp, rng):
    """A thin mineral seam threading between clusters — vanilla ironore's
    connective tissue, and what turns scattered nuggets into one vein."""
    for _ in range(length):
        _put(c, x, y, ramp["base"] if rng.chance(0.72) else ramp["light"])
        if rng.chance(0.5):
            _put(c, x + 1, y + 1, ramp["deep"])
        x += dx
        y += dy
        if rng.chance(0.34):  # wander, so seams never read as ruled lines
            if dy == 0:
                y += rng.pick((-1, 1))
            elif dx == 0:
                x += rng.pick((-1, 1))
            else:
                x += rng.pick((0, 1)) - 1


def _ore_cell(ramp, salt):
    """One 32x32 ore pattern. Anchors come off a jittered 3x3 lattice so
    coverage stays even in every 16x16 quadrant (the renderer recombines
    quadrants independently, so each has to carry its own mass) without the
    stamps lining up into visible rows."""
    c = Canvas(32, 32)
    rng = Rng(salt)
    step = 32.0 / 3.0

    anchors = []
    for j in range(3):
        for i in range(3):
            ax = int((i + 0.5) * step + (rng.float() - 0.5) * step * 0.8) % 32
            ay = int((j + 0.5) * step + (rng.float() - 0.5) * step * 0.8) % 32
            anchors.append((ax, ay))
    order = list(range(9))
    for i in range(8, 0, -1):                      # deterministic shuffle
        j = rng.range(0, i)
        order[i], order[j] = order[j], order[i]

    # 1) seams first, so crystals sit on top of their own vein
    for k in range(3):
        ax, ay = anchors[order[k]]
        dx, dy = rng.pick(((1, 1), (1, -1), (1, 0), (0, 1)))
        _seam(c, ax, ay, dx, dy, rng.range(9, 14), ramp, rng)

    # 2) two paired clusters and four single shards over six of the nine
    #    anchors — the three empty anchors are deliberate: vanilla always
    #    leaves bare stone between shards, and that gap is what lets each
    #    crystal read instead of fusing into a crust
    for rank, idx in enumerate(order[:6]):
        _cluster(c, anchors[idx][0], anchors[idx][1], ramp, rng, big=rank < 2)

    # 3) chips in the remaining gaps, kept clear of the cluster cores
    for _ in range(5):
        x, y = rng.range(0, 31), rng.range(0, 31)
        if any(min(abs(x - ax), 32 - abs(x - ax)) < 5
               and min(abs(y - ay), 32 - abs(y - ay)) < 5 for ax, ay in anchors[:6]):
            continue
        _stamp(c, _GRAIN, x, y, ramp, rim=False)

    # 4) row equaliser: the renderer can pair any two 16px bands, so no row may
    #    be left bare. Deterministic, and it is what kills the banded look.
    for y in range(32):
        row = sum(1 for x in range(32) if c.filled(x, y))
        tries = 0
        while row < 4 and tries < 10:
            x = rng.range(0, 31)
            tries += 1
            if not c.filled(x, y):
                c.put(x, y, ramp["base"] if rng.chance(0.65) else ramp["light"])
                _put(c, x + 1, y + 1, ramp["deep"])
                row = sum(1 for xx in range(32) if c.filled(xx, y))

    # 5) single-pixel sparkle grit — the last 5% vanilla ore always carries
    for _ in range(6):
        x, y = rng.range(0, 31), rng.range(0, 31)
        if not c.filled(x, y):
            c.put(x, y, ramp["light"] if rng.chance(0.55) else ramp["deep"])
    return c


def gen_ore_sheet(path, ramp, variants=2, salt=0xE77):
    """Ore pattern strip in the vanilla format (verified against ironore.png,
    64x32): N variants of 32x32 side by side, on transparency. RockOreObject
    multiplies this through objects/oremask.png onto the parent rock at
    runtime; variant 0 also feeds the auto-generated ore item icon."""
    sheet = Canvas(variants * 32, 32)
    for v in range(variants):
        sheet.paste(_ore_cell(ramp, salt + v * 131), v * 32, 0)
    sheet.save(path)
