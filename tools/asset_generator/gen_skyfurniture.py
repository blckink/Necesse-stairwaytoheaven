"""The Skywatch furniture family: pale skystone, sky-iron and windsilk.

Thirteen pieces that furnish a Skywatch hall — chair, bench, modular table,
dinner table, desk, dresser, bed, candelabra, carpet and four table
decorations — plus their 32x32 inventory icons.

Everything here is deterministic (fixed `Rng` seeds, no `random`) and every
colour comes from `palette`, with three derived constants declared below:
`SHADOW`/`UNDER` (the translucent ground shadow and the solid dark mass
vanilla furniture packs between its legs) and the two mask sheets, which are
pure alpha geometry the engine multiplies and so carry no colour at all.

Sheet formats are the law of `docs/research/furniture-formats.md`, verified
against the decompiled 1.3.2 renderers:

- rotation sheets are **4 columns of 32 px**, bottom-anchored at
  `drawY - height + 32`.  Rotation order is `dir()` order: **0 up, 1 right,
  2 down, 3 left**, so column 0 is the piece seen from BEHIND (the sitter
  faces away), column 2 is the front, columns 1/3 are the side views with the
  backrest on the left / right respectively.  Read off `oakchair.png` and
  confirmed against `ChairObject.modifyHumanDrawOptions` and `dungeonchair`.
- bench / dinner table / bed are 128x128 four-view sheets, but NOT four
  columns: two 64x64 blocks at (0,0) and (0,64) carry the horizontal views and
  two 32x96 strips at (64,32) and (96,32) carry the vertical views.  Which
  view lands in which region differs per class — see each `gen_*` below.
- the modular table is a 96x64 atlas of 16 px autotile cells; it has no
  rotations at all, and cells (4,1)/(5,1) are used as both the top and the
  bottom quadrant of a vertical run, so they must be vertically seamless.
- the carpet is a `ModularCarpetObject`: a 64x64 seamlessly tiling *pattern*
  in `objects/carpets/` plus a 64x64 alpha-only edge mask.

Style notes that came out of a side-by-side with the vanilla dump:

- vanilla furniture is drawn on a chunky ~2 px form grid with only ~10% of
  its pixels carrying 1 px detail.  Grain here is deliberately sparse and one
  ramp step from the base; dense speckle reads as noise, not as stone.
- vanilla never leaves the space under a piece of furniture transparent.  It
  fills it with a dark mass and draws the legs as lighter strokes on top,
  which is what stops legs looking like stilts.  That is `_under_mass`.
- the family accent is `SKYIRON["patina"]` teal: piping, pulls, inlays and the
  crescent-and-star mark the mod's wall banner already carries.  Without it
  a pale stone family against pale stone brick walls goes to mush.

Run standalone:  python3 gen_skyfurniture.py <objects_dir> <items_dir>
"""

import os
import sys

from px import Canvas, Rng, mix, with_alpha
import palette

# --- material identity --------------------------------------------------

STONE = palette.SKYSTONE        # pale grey-blue temple stone: the body
IRON = palette.SKYIRON          # sky-iron: frames, legs, fittings
SILK = palette.WINDSILK         # windsilk: cushions, bedding, carpet, wax
GLOW = palette.STAIRLIGHT["glow"]
GLOW_HI = palette.STAIRLIGHT["hi"]
OUT = palette.OUTLINE
PATINA = IRON["patina"]
PATINA_HI = IRON["patina_hi"]
BERRY = palette.CLOUDBERRY

# Derived, not straight palette entries.  Vanilla furniture casts a
# translucent cool ground shadow (oakmodulartable uses (0,19,26,126)) and
# fills the space under itself with a near-black mass (oakchair's under-seat
# rows).  Both here are tints of the mod's own outline tone; if they are
# wanted project-wide, lift them into palette.py as FURNITURE_SHADOW /
# FURNITURE_UNDER.
SHADOW = with_alpha(OUT, 112)
SHADOW_SOFT = with_alpha(OUT, 60)
UNDER = mix(OUT, IRON["deep"], 0.45)

# One ramp step, not three: this is all grain is allowed to shift by.
STONE_G_HI = mix(STONE["base"], STONE["light"], 0.75)
STONE_G_LO = mix(STONE["base"], STONE["deep"], 0.55)


# --- small shared drawing helpers ---------------------------------------


def _grain(c, x, y, w, h, rng, hi=None, lo=None, density=34):
    """Sparse single-pixel grain, ~1 px per `density` px of area, shifted by
    one ramp step.  Vanilla's oak table carries ~7% grain in a barely darker
    tone; anything denser reads as noise rather than as stone."""
    hi = hi or STONE_G_HI
    lo = lo or STONE_G_LO
    for _ in range(max(1, (w * h) // density)):
        gx = x + rng.range(0, w - 1)
        gy = y + rng.range(0, h - 1)
        if c.filled(gx, gy):
            c.put(gx, gy, hi if rng.chance(0.45) else lo)


def _slab(c, x, y, w, h, rng, ramp=None, grain=True):
    """A carved stone block: lit top face, lit left chamfer, deep bottom and
    right, sparse grain in the middle."""
    ramp = ramp or STONE
    c.rect(x, y, w, h, ramp["base"])
    c.rect(x, y, w, 2, ramp["light"])
    c.rect(x, y, w, 1, ramp["hi"])
    c.rect(x, y, 1, h, ramp["light"])
    c.rect(x + w - 1, y, 1, h, ramp["deep"])
    c.rect(x, y + h - 1, w, 1, ramp["deep"])
    if grain and w > 3 and h > 4:
        _grain(c, x + 1, y + 2, w - 2, h - 3, rng)


def _panel(c, x, y, w, h, rng):
    """A recessed stone panel inside a frame: darker, shadowed top and left."""
    c.rect(x, y, w, h, mix(STONE["base"], STONE["deep"], 0.4))
    c.rect(x, y, w, 1, STONE["deep"])
    c.rect(x, y, 1, h, STONE["deep"])
    c.rect(x + w - 1, y + 1, 1, h - 1, STONE["light"])
    c.rect(x, y + h - 1, w, 1, STONE["light"])
    if w > 3 and h > 3:
        _grain(c, x + 1, y + 1, w - 2, h - 2, rng,
               hi=STONE["base"], lo=STONE["deep"], density=40)


def _iron_post(c, x, y, w, h, ramp=None):
    """A sky-iron leg or stile: bright left edge, dark right edge."""
    ramp = ramp or IRON
    c.rect(x, y, w, h, ramp["base"])
    c.rect(x, y, 1, h, ramp["light"])
    if w > 2:
        c.rect(x + w - 1, y, 1, h, ramp["deep"])
    c.rect(x, y, w, 1, ramp["hi"])


def _iron_rail(c, x, y, w, h, ramp=None):
    """A horizontal sky-iron band."""
    ramp = ramp or IRON
    c.rect(x, y, w, h, ramp["base"])
    c.rect(x, y, w, 1, ramp["light"])
    if h > 1:
        c.rect(x, y + h - 1, w, 1, ramp["deep"])


def _under_mass(c, x, y, w, h, legs, leg_w=4):
    """The dark mass under a piece of furniture, with the legs drawn on top of
    it.  Vanilla fills this space rather than leaving it transparent, which is
    the whole difference between chunky furniture and stilts."""
    c.rect(x, y, w, h, UNDER)
    c.rect(x, y, w, 1, mix(UNDER, IRON["base"], 0.45))
    for lx in legs:
        c.rect(lx, y, leg_w, h - 1, IRON["light"])
        c.rect(lx, y, 1, h - 1, IRON["hi"])
        c.rect(lx + leg_w - 1, y, 1, h - 1, IRON["base"])
        c.rect(lx, y + h - 2, leg_w, 2, IRON["deep"])
        c.put(lx + 1, y + h - 2, IRON["base"])


def _cushion(c, x, y, w, h, rng):
    """Windsilk seat cushion: pale cloth with patina piping along the front,
    which is what stops it reading as a blown-out white bar."""
    c.rect(x, y, w, h, SILK["base"])
    c.rect(x, y, w, 1, SILK["hi"])
    c.rect(x, y + 1, w, 1, SILK["light"])
    c.rect(x, y, 1, h, SILK["light"])
    c.rect(x + w - 1, y, 1, h, SILK["deep"])
    c.rect(x, y + h - 2, w, 1, SILK["deep"])
    c.rect(x, y + h - 1, w, 1, PATINA)                   # piping
    for i in range(3, w - 3, 7):                         # tufting
        c.put(x + i, y + h // 2, SILK["deep"])
        c.put(x + i + 1, y + h // 2, SILK["hi"])
    _grain(c, x + 1, y + 2, max(1, w - 2), max(1, h - 4), rng,
           hi=SILK["hi"], lo=SILK["light"], density=26)


def _bedding(c, x, y, w, h, rng):
    """A quilted windsilk blanket with a patina hem.  Deliberately a step
    darker than `_bed_pillow` so the two do not merge into one white slab."""
    body = mix(SILK["base"], SILK["deep"], 0.45)
    c.rect(x, y, w, h, body)
    c.rect(x, y, w, 2, SILK["base"])
    c.rect(x, y, w, 1, SILK["light"])
    c.rect(x, y, 1, h, SILK["base"])
    c.rect(x + w - 1, y, 1, h, SILK["deep"])
    for i in range(1, w - 3, 6):                         # quilt ticks
        c.put(x + 1 + i, y + h // 3, SILK["deep"])
        c.put(x + 2 + i, y + h // 3, SILK["light"])
        c.put(x + 3 + i, y + 2 * h // 3, SILK["deep"])
    c.rect(x, y + h - 3, w, 1, PATINA)
    c.rect(x, y + h - 2, w, 2, SILK["deep"])
    _grain(c, x + 1, y + 2, max(1, w - 2), max(1, h - 5), rng,
           hi=SILK["base"], lo=SILK["deep"], density=30)


def _crescent(c, cx, cy, color, star=True, star_color=None):
    """The Skywatch mark: a crescent moon with a small star, the same emblem
    the mod's wall banner carries.  Drawn as a solid mass so the outline pass
    cannot eat it, and always applied AFTER outlining."""
    for dy in range(-3, 4):
        for dx in range(-3, 4):
            if dx * dx + dy * dy <= 10 and (dx - 2) ** 2 + dy * dy > 8:
                c.put(cx + dx, cy + dy, color)
    if star:
        sc = star_color or color
        for dx, dy in ((3, -3), (4, -3), (5, -3), (4, -4), (4, -2)):
            c.put(cx + dx, cy + dy, sc)


def _flame(c, cx, bottom, height, rng, seed_shift=0):
    """A pale Skywatch flame: dark-cored teardrop first, hot core on top, so
    the outline pass cannot swallow the 1 px tip."""
    top = bottom - height
    for y in range(top, bottom + 1):                     # dark teardrop mass
        t = (y - top) / max(1, height)
        w = 1 + int(round(2.4 * (t ** 0.7)))
        for dx in range(-w, w + 1):
            c.put(cx + dx, y, mix(GLOW, OUT, 0.62))
    for y in range(top, bottom):                         # bright body
        t = (y - top) / max(1, height)
        w = int(round(1.9 * (t ** 0.7)))
        for dx in range(-w, w + 1):
            c.put(cx + dx, y, GLOW)
    for y in range(top + 1, bottom):                     # hot core
        c.put(cx, y, GLOW_HI)
        if y > top + 2:
            c.put(cx - 1, y, mix(GLOW_HI, GLOW, 0.4))
    c.put(cx, top, GLOW_HI)
    c.put(cx + (1 if (seed_shift % 2) else -1), top - 1, with_alpha(GLOW, 150))


def _ground_shadow(c, cx, cy, rx, ry):
    c.ellipse(cx, cy, rx + 1, ry + 1, SHADOW_SOFT)
    c.ellipse(cx, cy, rx, ry, SHADOW)


def _paste_col(sheet, cell, col):
    sheet.paste(cell, col * 32, 0)


# =======================================================================
# 1. Chair - 128x64, four rotation columns
# =======================================================================
#
# Seat top row 34, seat bottom row 45, feet row 55.  The ChairObject sit
# anchor is tile-local y=14 (sprite row 46), i.e. exactly the bottom of the
# seat, so a settler's hips land on the cushion.  The narrow waist between
# backrest and seat is what gives a chair a chair silhouette instead of a
# cabinet one - vanilla's oakchair does the same thing at rows 28..33.


def _chair_front(rng):
    c = Canvas(32, 64)
    _ground_shadow(c, 16, 55, 12, 3)
    _under_mass(c, 7, 46, 18, 10, (7, 21))
    _slab(c, 4, 40, 24, 6, rng)                          # seat apron
    _iron_rail(c, 4, 44, 24, 2)
    _cushion(c, 5, 34, 22, 6, rng)
    _slab(c, 9, 30, 14, 5, rng)                          # narrow waist
    _slab(c, 4, 16, 5, 15, rng)                          # left stile
    _slab(c, 23, 16, 5, 15, rng)                         # right stile
    _panel(c, 9, 16, 14, 15, rng)                        # recessed back panel
    _slab(c, 4, 12, 24, 5, rng)                          # crest rail
    c.rect(9, 10, 14, 2, STONE["light"])
    c.rect(10, 9, 12, 1, STONE["hi"])
    _iron_rail(c, 4, 14, 24, 2)
    c.put(5, 11, PATINA_HI)
    c.put(26, 11, PATINA_HI)
    return c


def _chair_back(rng):
    c = Canvas(32, 64)
    _ground_shadow(c, 16, 55, 12, 3)
    _under_mass(c, 7, 46, 18, 10, (7, 21))
    _slab(c, 4, 24, 24, 22, rng)                         # outer face of the back
    _iron_rail(c, 4, 42, 24, 2)
    _slab(c, 4, 20, 24, 5, rng)                          # crest rail
    c.rect(9, 18, 14, 2, STONE["light"])
    c.rect(10, 17, 12, 1, STONE["hi"])
    _iron_rail(c, 4, 22, 24, 2)
    c.rect(15, 26, 2, 16, STONE["light"])                # centre rib
    c.rect(17, 26, 1, 16, STONE["deep"])
    _grain(c, 5, 27, 22, 14, rng)
    c.put(5, 19, PATINA_HI)
    c.put(26, 19, PATINA_HI)
    return c


def _chair_side(rng):
    """Rotation 1: side view, backrest on the LEFT, sitter faces right.

    Matched against `dungeonchair.png`'s side column: the back is ONE tall
    continuous slab with a small finial, tapering slightly toward the top, and
    the seat is a short block hung off it at mid height.  Breaking the back
    into a capped post plus a rail (the first attempt) read as a lamp."""
    c = Canvas(32, 64)
    _ground_shadow(c, 16, 55, 11, 3)
    _under_mass(c, 6, 46, 20, 10, (6, 21))
    _slab(c, 8, 40, 20, 6, rng)                          # seat apron
    _iron_rail(c, 8, 44, 20, 2)
    _cushion(c, 9, 34, 18, 6, rng)
    _slab(c, 5, 14, 9, 28, rng)                          # back slab, edge on
    _slab(c, 6, 10, 7, 5, rng)                           # tapered upper back
    c.rect(7, 8, 5, 2, STONE["light"])                   # finial
    c.rect(8, 7, 3, 1, STONE["hi"])
    _iron_rail(c, 5, 16, 9, 2)
    _panel(c, 7, 19, 5, 20, rng)                         # carved back channel
    c.rect(5, 19, 2, 21, STONE["light"])                 # lit front edge
    c.rect(12, 19, 2, 21, STONE["base"])
    c.rect(13, 19, 1, 21, STONE["deep"])
    _slab(c, 14, 30, 12, 4, rng)                         # side rail to the front
    c.put(9, 8, PATINA_HI)
    return c


def gen_chair(path):
    rng = Rng(0x5C4A17)
    sheet = Canvas(128, 64)
    side = _chair_side(rng)
    for i, cell in enumerate((_chair_back(rng), side,
                              _chair_front(rng), side.mirrored())):
        cell.outline(OUT)
        _paste_col(sheet, cell, i)
    _crescent(sheet, 2 * 32 + 15, 23, PATINA_HI, star_color=GLOW)
    # the back view gets the same mark cut into the stone, not painted on:
    # a shadow crescent with a lit one offset up-left reads as a carving
    _crescent(sheet, 0 * 32 + 16, 34, STONE["deep"], star=False)
    _crescent(sheet, 0 * 32 + 15, 33, STONE["hi"], star=False)
    sheet.save(path)


# =======================================================================
# 2. Bench - 128x128
# =======================================================================
#
# BenchObject cell map, from the decompiled draw code and confirmed against
# oakbench.png:  rotation 0 -> strip (64,32) [faces right, backrest LEFT];
# rotation 1 -> block (0,64) [faces down = FRONT];  rotation 2 -> strip
# (96,32) [faces left, backrest RIGHT];  rotation 3 -> block (0,0) [BACK].


def _bench_front(rng):
    c = Canvas(64, 64)
    _ground_shadow(c, 32, 59, 27, 3)
    _under_mass(c, 6, 52, 52, 8, (6, 52), leg_w=6)
    _slab(c, 3, 45, 58, 7, rng)                          # seat apron
    _iron_rail(c, 3, 50, 58, 2)
    _cushion(c, 4, 38, 56, 7, rng)
    for x in (3, 29, 55):                                # stiles
        _slab(c, x, 16, 6, 22, rng)
    for x in (9, 35):                                    # recessed panels
        _panel(c, x, 16, 20, 22, rng)
    _slab(c, 3, 12, 58, 5, rng)                          # crest rail
    c.rect(9, 10, 46, 2, STONE["light"])
    c.rect(11, 9, 42, 1, STONE["hi"])
    _iron_rail(c, 3, 14, 58, 2)
    _iron_rail(c, 3, 36, 58, 2)
    for x in (4, 31, 58):
        c.put(x, 11, PATINA_HI)
    return c


def _bench_back(rng):
    c = Canvas(64, 64)
    _ground_shadow(c, 32, 59, 27, 3)
    _under_mass(c, 6, 52, 52, 8, (6, 52), leg_w=6)
    _slab(c, 3, 24, 58, 28, rng)                         # outer face of the back
    _iron_rail(c, 3, 48, 58, 2)
    _slab(c, 3, 20, 58, 5, rng)                          # crest rail
    c.rect(9, 18, 46, 2, STONE["light"])
    c.rect(11, 17, 42, 1, STONE["hi"])
    _iron_rail(c, 3, 22, 58, 2)
    for x in (20, 41):                                   # panel ribs
        c.rect(x, 26, 2, 22, STONE["light"])
        c.rect(x + 2, 26, 1, 22, STONE["deep"])
    _grain(c, 4, 27, 56, 20, rng)
    for x in (4, 31, 58):
        c.put(x, 19, PATINA_HI)
    return c


def _bench_side(rng):
    """32x96 strip: a bench running north-south, backrest on the LEFT.  The
    long cushion gets cross seams and a shaded near edge, otherwise 45 px of
    unbroken windsilk reads as a white pillar rather than a seat."""
    c = Canvas(32, 96)
    _ground_shadow(c, 16, 88, 12, 4)
    _under_mass(c, 11, 80, 17, 12, (11, 23))
    _slab(c, 11, 74, 18, 7, rng)                         # seat front edge
    _iron_rail(c, 11, 79, 18, 2)
    _cushion(c, 11, 30, 17, 45, rng)
    for y in (44, 58):                                   # seam between seats
        c.rect(11, y, 17, 1, SILK["deep"])
        c.rect(11, y + 1, 17, 1, SILK["hi"])
    c.rect(25, 31, 3, 43, mix(SILK["base"], SILK["deep"], 0.5))  # shaded near edge
    c.rect(27, 31, 1, 43, SILK["deep"])
    _slab(c, 4, 20, 7, 56, rng)                          # long backrest, edge on
    _slab(c, 3, 16, 9, 5, rng)
    c.rect(4, 14, 7, 2, STONE["light"])
    c.rect(5, 13, 5, 1, STONE["hi"])
    _iron_rail(c, 3, 18, 9, 2)
    c.rect(5, 22, 2, 53, STONE["light"])
    c.rect(10, 22, 1, 53, STONE["deep"])
    for y in (34, 46, 58, 70):
        _iron_rail(c, 4, y, 7, 2)
    c.put(4, 15, PATINA_HI)
    return c


def gen_bench(path):
    rng = Rng(0xB3C401)
    sheet = Canvas(128, 128)
    front, back, side = _bench_front(rng), _bench_back(rng), _bench_side(rng)
    for cell in (front, back, side):
        cell.outline(OUT)
    sheet.paste(back, 0, 0)                              # rotation 3, faces up
    sheet.paste(front, 0, 64)                            # rotation 1, faces down
    sheet.paste(side, 64, 32)                            # rotation 0, faces right
    sheet.paste(side.mirrored(), 96, 32)                 # rotation 2, faces left
    for cx in (17, 43):
        _crescent(sheet, cx, 64 + 26, PATINA_HI, star_color=GLOW)
    _crescent(sheet, 31, 35, STONE["deep"], star=False)
    _crescent(sheet, 30, 34, STONE["hi"], star=False)
    for cy in (74, 98):
        _crescent(sheet, 64 + 7, cy, PATINA_HI, star=False)
        _crescent(sheet, 96 + 24, cy, PATINA_HI, star=False)
    sheet.save(path)


# =======================================================================
# 3. Modular table - 96x64, the 16 px autotile atlas (NO rotations)
# =======================================================================
#
# Cell map straight out of ModularTableObject.addDrawables.  A tile draws four
# 16 px quadrants at drawY-14 plus, only when nothing connects below, a 16 px
# apron at drawY+26.  Cells (4,1)/(5,1) are used as BOTH the top and the
# bottom quadrant of a vertical run, so they are vertically uniform; the
# (4..5, 2..3) block is the apron and is never a table-top quadrant.
#
# The surface carries nothing but grain and every decoration lives on the open
# edges, so a 3x3 table reads as one continuous slab instead of a grid.

_FRONT_TOP = 24      # block row where the lit front lip starts


def _table_grain(rng):
    """One 32x32 grain map shared by every cell, so quadrants line up."""
    g = {}
    for _ in range(22):
        g[(rng.range(2, 29), rng.range(2, 29))] = rng.chance(0.45)
    return g


def _table_block(open_top, open_left, open_right, open_bottom, grain):
    """One tile's 32x32 slice of table top with the requested outer edges.

    Order matters: the bands are laid down first and the transparent cuts are
    taken LAST, otherwise a left-edge band paints back into the four rows the
    top edge just cleared and the table grows stray whiskers above itself.
    The inlay lines are mitred against whichever other edges are open, so a
    3x3 table gets one continuous border instead of one per tile."""
    clear = (0, 0, 0, 0)
    c = Canvas(32, 32)
    c.rect(0, 0, 32, 32, STONE["base"])
    for (gx, gy), hi in grain.items():
        c.put(gx, gy, STONE_G_HI if hi else STONE_G_LO)
    if open_bottom:
        c.rect(0, _FRONT_TOP, 32, 2, STONE["hi"])        # lit front lip
        c.rect(0, _FRONT_TOP + 2, 32, 4, STONE["deep"])  # front face
        c.rect(0, _FRONT_TOP + 3, 32, 1, STONE["base"])
        c.rect(0, _FRONT_TOP + 6, 32, 2, OUT)
    if open_top:
        c.rect(0, 4, 32, 2, OUT)
        c.rect(0, 6, 32, 1, STONE["hi"])                 # lit far chamfer
        c.rect(0, 7, 32, 1, STONE["light"])
    if open_left:
        c.rect(2, 0, 2, 32, OUT)
        c.rect(4, 0, 1, 32, STONE["light"])
    if open_right:
        c.rect(28, 0, 2, 32, OUT)
        c.rect(27, 0, 1, 32, STONE["deep"])
    # inlaid patina border, mitred against the open edges
    ix0, ix1 = (7 if open_left else 0), (24 if open_right else 31)
    iy0, iy1 = (10 if open_top else 0), (21 if open_bottom else 31)
    if open_top:
        c.rect(ix0, 10, ix1 - ix0 + 1, 1, PATINA)
    if open_bottom:
        c.rect(ix0, 21, ix1 - ix0 + 1, 1, PATINA)
    if open_left:
        c.rect(7, iy0, 1, iy1 - iy0 + 1, PATINA)
    if open_right:
        c.rect(24, iy0, 1, iy1 - iy0 + 1, PATINA)
    # transparent cuts, always last
    if open_top:
        c.rect(0, 0, 32, 4, clear)
    if open_left:
        c.rect(0, 0, 2, 32, clear)
    if open_right:
        c.rect(30, 0, 2, 32, clear)
    # The side ground shadow starts below the table's own far edge, exactly as
    # vanilla does: cell (0,0) has none, cell (4,1) has it on every row.
    if open_left:
        for y in (range(10, 32) if open_top else range(0, 32)):
            c.put(0, y, SHADOW)
            c.put(1, y, SHADOW)
    if open_right:
        for y in (range(10, 32) if open_top else range(0, 32)):
            c.put(30, y, SHADOW)
            c.put(31, y, SHADOW)
    return c


def _quad(block, qx, qy):
    out = Canvas(16, 16)
    out.img = block.img.crop((qx * 16, qy * 16, qx * 16 + 16, qy * 16 + 16))
    out.px = out.img.load()
    return out


def _table_apron_half(closed_end, right_side):
    """16x16 apron half.  A closed end carries the leg; a continuing end is
    nothing but ground shadow, so a wide table has legs only at its ends."""
    c = Canvas(16, 16)
    # rows 0..5 sit on top of the table's own front face; only shade from row 6
    # down, which is the open gap under the table
    c.rect(0, 6, 16, 8, SHADOW)
    if closed_end:
        lx = 2 if not right_side else 8
        c.rect(lx, 0, 6, 13, UNDER)
        c.rect(lx + 1, 0, 4, 13, IRON["light"])
        c.rect(lx + 1, 0, 1, 13, IRON["hi"])
        c.rect(lx + 4, 0, 1, 13, IRON["base"])
        c.rect(lx, 11, 6, 2, IRON["deep"])
        c.put(lx + 2, 11, IRON["base"])
        c.put(lx + 2, 2, PATINA_HI)
    return c


def gen_modulartable(path):
    rng = Rng(0x7AB1E5)
    grain = _table_grain(rng)
    sheet = Canvas(96, 64)

    plain = _table_block(False, False, False, False, grain)
    top = _table_block(True, False, False, False, grain)
    left = _table_block(False, True, False, False, grain)
    right = _table_block(False, False, True, False, grain)
    bottom = _table_block(False, False, False, True, grain)
    tl = _table_block(True, True, False, False, grain)
    tr = _table_block(True, False, True, False, grain)
    bl = _table_block(False, True, False, True, grain)
    br = _table_block(False, False, True, True, grain)

    def put(col, row, cell):
        sheet.paste(cell, col * 16, row * 16)

    # top-left quadrant family: (up, left) -> cell
    put(0, 0, _quad(tl, 0, 0))          # no up, no left
    put(2, 2, _quad(top, 0, 0))         # no up, left
    put(4, 1, _quad(left, 0, 0))        # up, no left  (also the BL of a run)
    put(4, 0, _quad(left, 0, 0))        # up, no left, up-left present
    put(0, 2, _quad(plain, 0, 0))       # interior
    # top-right quadrant family
    put(1, 0, _quad(tr, 1, 0))
    put(3, 2, _quad(top, 1, 0))
    put(5, 1, _quad(right, 1, 0))
    put(5, 0, _quad(right, 1, 0))
    put(1, 2, _quad(plain, 1, 0))
    # bottom-left quadrant family: (down, left) -> cell
    put(0, 1, _quad(bl, 0, 1))          # no down, no left
    put(2, 3, _quad(bottom, 0, 1))      # no down, left
    put(0, 3, _quad(plain, 0, 1))       # interior
    # bottom-right quadrant family
    put(1, 1, _quad(br, 1, 1))
    put(3, 3, _quad(bottom, 1, 1))
    put(1, 3, _quad(plain, 1, 1))

    def notch(col, row, edged, qx, qy, corner):
        """The concave-corner cells: an interior quadrant with the outer
        corner of the missing diagonal wrapped around its corner point."""
        cell = _quad(plain, qx, qy)
        edge = _quad(edged, qx, qy)
        for y in range(16):
            for x in range(16):
                near_x = x < 6 if corner[1] == "L" else x >= 10
                near_y = y < 6 if corner[0] == "T" else y >= 10
                if near_x and near_y:
                    cell.put(x, y, edge.get(x, y))
        put(col, row, cell)

    notch(2, 0, tl, 0, 0, "TL")
    notch(3, 0, tr, 1, 0, "TR")
    notch(2, 1, bl, 0, 1, "BL")
    notch(3, 1, br, 1, 1, "BR")

    put(4, 2, _table_apron_half(True, False))
    put(5, 2, _table_apron_half(True, True))
    put(4, 3, _table_apron_half(False, False))
    put(5, 3, _table_apron_half(False, True))
    sheet.save(path)


# =======================================================================
# 4. Dinner table - 128x128
# =======================================================================
#
# DinnerTableObject cell map: rotation 0 -> strip (96,32), 1 -> block (0,64),
# 2 -> strip (64,32), 3 -> block (0,0).  A table has no facing, so vanilla
# makes both blocks identical and both strips identical; so do we.


def _dinner_block(rng):
    """64x64: the table lying east-west across two tiles."""
    c = Canvas(64, 64)
    _ground_shadow(c, 32, 55, 28, 4)
    _under_mass(c, 5, 46, 54, 12, (5, 53), leg_w=6)
    _slab(c, 2, 20, 60, 22, rng)                         # top slab
    c.rect(2, 20, 60, 2, STONE["hi"])
    c.rect(4, 19, 56, 1, STONE["light"])
    c.rect(2, 25, 60, 1, PATINA)                         # inlaid border
    c.rect(2, 38, 60, 1, PATINA)
    c.rect(6, 21, 1, 21, PATINA)
    c.rect(57, 21, 1, 21, PATINA)
    c.rect(2, 42, 60, 2, STONE["hi"])                    # lit front lip
    c.rect(2, 44, 60, 4, STONE["deep"])                  # front face
    c.rect(2, 45, 60, 1, STONE["base"])
    _grain(c, 8, 27, 48, 11, rng)
    return c


def _dinner_strip(rng):
    """32x96: the table lying north-south across two tiles."""
    c = Canvas(32, 96)
    _ground_shadow(c, 16, 86, 13, 4)
    _under_mass(c, 4, 78, 24, 12, (4, 22), leg_w=6)
    _slab(c, 2, 22, 28, 52, rng)
    c.rect(2, 22, 28, 2, STONE["hi"])
    c.rect(4, 21, 24, 1, STONE["light"])
    c.rect(6, 23, 1, 51, PATINA)
    c.rect(25, 23, 1, 51, PATINA)
    c.rect(2, 27, 28, 1, PATINA)
    c.rect(2, 70, 28, 1, PATINA)
    c.rect(2, 74, 28, 2, STONE["hi"])
    c.rect(2, 76, 28, 4, STONE["deep"])
    c.rect(2, 77, 28, 1, STONE["base"])
    _grain(c, 8, 29, 17, 40, rng)
    return c


def gen_dinnertable(path):
    rng = Rng(0xD177AB)
    sheet = Canvas(128, 128)
    block, strip = _dinner_block(rng), _dinner_strip(rng)
    block.outline(OUT)
    strip.outline(OUT)
    sheet.paste(block, 0, 0)
    sheet.paste(block, 0, 64)
    sheet.paste(strip, 64, 32)
    sheet.paste(strip, 96, 32)
    for oy in (0, 64):
        _crescent(sheet, 30, oy + 31, PATINA_HI, star_color=GLOW)
    for ox in (64, 96):
        _crescent(sheet, ox + 14, 78, PATINA_HI, star_color=GLOW)
    sheet.save(path)


# =======================================================================
# 5. Desk - 128x64
# =======================================================================


def _desk_front(rng):
    c = Canvas(32, 64)
    _ground_shadow(c, 16, 57, 14, 3)
    _under_mass(c, 4, 47, 24, 11, (4, 24))
    _slab(c, 3, 34, 26, 13, rng)                         # drawer bank
    c.rect(4, 35, 24, 11, STONE["deep"])                 # drawer recess
    for dy in (35, 41):                                  # two shallow drawers
        c.rect(5, dy, 22, 5, STONE["light"])
        c.rect(5, dy, 22, 1, STONE["hi"])
        c.rect(5, dy, 1, 5, STONE["hi"])
        c.rect(5, dy + 4, 22, 1, STONE["deep"])
        c.rect(26, dy + 1, 1, 4, STONE["deep"])
        _iron_rail(c, 12, dy + 1, 8, 2)
        c.put(13, dy + 1, PATINA_HI)
    _slab(c, 1, 28, 30, 6, rng)                          # desktop
    c.rect(1, 28, 30, 2, STONE["hi"])
    c.rect(1, 32, 30, 1, PATINA)
    _slab(c, 3, 20, 26, 8, rng)                          # raised back gallery
    c.rect(6, 18, 20, 2, STONE["light"])
    c.rect(7, 17, 18, 1, STONE["hi"])
    _panel(c, 7, 21, 18, 5, rng)
    _iron_rail(c, 3, 26, 26, 2)
    c.put(4, 19, PATINA_HI)
    c.put(27, 19, PATINA_HI)
    return c


def _desk_back(rng):
    c = Canvas(32, 64)
    _ground_shadow(c, 16, 57, 14, 3)
    _under_mass(c, 4, 47, 24, 11, (4, 24))
    _slab(c, 2, 24, 28, 23, rng)
    _iron_rail(c, 2, 42, 28, 2)
    _slab(c, 3, 20, 26, 5, rng)
    c.rect(6, 18, 20, 2, STONE["light"])
    c.rect(7, 17, 18, 1, STONE["hi"])
    _iron_rail(c, 2, 26, 28, 2)
    c.rect(15, 28, 2, 14, STONE["light"])
    c.rect(17, 28, 1, 14, STONE["deep"])
    _grain(c, 3, 29, 26, 12, rng)
    c.put(4, 19, PATINA_HI)
    c.put(27, 19, PATINA_HI)
    return c


def _desk_side(rng):
    c = Canvas(32, 64)
    _ground_shadow(c, 16, 57, 12, 3)
    _under_mass(c, 6, 47, 20, 11, (6, 22))
    _slab(c, 6, 34, 20, 13, rng)                         # bank seen edge on
    _panel(c, 8, 36, 16, 9, rng)
    _slab(c, 4, 28, 24, 6, rng)                          # desktop
    c.rect(4, 28, 24, 2, STONE["hi"])
    c.rect(4, 32, 24, 1, PATINA)
    _slab(c, 5, 20, 8, 8, rng)                           # gallery, edge on
    c.rect(6, 18, 6, 2, STONE["light"])
    c.rect(7, 17, 4, 1, STONE["hi"])
    _iron_rail(c, 5, 26, 8, 2)
    c.put(6, 19, PATINA_HI)
    return c


def gen_desk(path):
    rng = Rng(0xDE5C01)
    sheet = Canvas(128, 64)
    side = _desk_side(rng)
    for i, cell in enumerate((_desk_back(rng), side,
                              _desk_front(rng), side.mirrored())):
        cell.outline(OUT)
        _paste_col(sheet, cell, i)
    _crescent(sheet, 2 * 32 + 15, 23, PATINA_HI, star_color=GLOW)
    _crescent(sheet, 0 * 32 + 17, 36, STONE["deep"], star=False)
    _crescent(sheet, 0 * 32 + 16, 35, STONE["hi"], star=False)
    sheet.save(path)


# =======================================================================
# 6. Dresser - 128x64
# =======================================================================


def _dresser_front(rng):
    c = Canvas(32, 64)
    _ground_shadow(c, 16, 58, 15, 3)
    _under_mass(c, 3, 52, 26, 7, (3, 25))
    _slab(c, 2, 20, 28, 32, rng)                         # carcass
    for dy in (24, 38):                                  # two deep drawers
        c.rect(4, dy, 24, 12, STONE["light"])
        c.rect(4, dy, 24, 1, STONE["hi"])
        c.rect(4, dy, 1, 12, STONE["hi"])
        c.rect(4, dy + 11, 24, 1, STONE["deep"])
        c.rect(27, dy + 1, 1, 11, STONE["deep"])
        _grain(c, 5, dy + 1, 22, 10, rng,
               hi=STONE["hi"], lo=STONE["base"], density=30)
        _iron_rail(c, 11, dy + 5, 10, 3)                 # pull
        c.rect(12, dy + 5, 8, 1, IRON["hi"])
        c.put(12, dy + 6, PATINA_HI)
        c.put(19, dy + 6, PATINA)
    _slab(c, 1, 15, 30, 6, rng)                          # top slab with a lip
    c.rect(1, 15, 30, 2, STONE["hi"])
    c.rect(3, 14, 26, 1, STONE["light"])
    c.rect(1, 19, 30, 1, PATINA)
    _iron_rail(c, 2, 21, 28, 2)
    _iron_rail(c, 2, 50, 28, 2)
    return c


def _dresser_back(rng):
    c = Canvas(32, 64)
    _ground_shadow(c, 16, 58, 15, 3)
    _under_mass(c, 3, 52, 26, 7, (3, 25))
    _slab(c, 2, 20, 28, 32, rng)
    _slab(c, 1, 15, 30, 6, rng)
    c.rect(1, 15, 30, 2, STONE["hi"])
    c.rect(3, 14, 26, 1, STONE["light"])
    c.rect(1, 19, 30, 1, PATINA)
    _iron_rail(c, 2, 21, 28, 2)
    _iron_rail(c, 2, 50, 28, 2)
    c.rect(15, 23, 2, 27, STONE["light"])
    c.rect(17, 23, 1, 27, STONE["deep"])
    _grain(c, 3, 24, 26, 25, rng)
    return c


def _dresser_side(rng):
    c = Canvas(32, 64)
    _ground_shadow(c, 16, 58, 13, 3)
    _under_mass(c, 5, 52, 22, 7, (5, 21))
    _slab(c, 4, 20, 24, 32, rng)
    _panel(c, 7, 24, 18, 25, rng)
    _slab(c, 3, 15, 26, 6, rng)
    c.rect(3, 15, 26, 2, STONE["hi"])
    c.rect(5, 14, 22, 1, STONE["light"])
    c.rect(3, 19, 26, 1, PATINA)
    _iron_rail(c, 4, 21, 24, 2)
    _iron_rail(c, 4, 50, 24, 2)
    return c


def gen_dresser(path):
    rng = Rng(0xD8E55E)
    sheet = Canvas(128, 64)
    side = _dresser_side(rng)
    for i, cell in enumerate((_dresser_back(rng), side,
                              _dresser_front(rng), side.mirrored())):
        cell.outline(OUT)
        _paste_col(sheet, cell, i)
    _crescent(sheet, 0 * 32 + 17, 37, STONE["deep"], star=False)
    _crescent(sheet, 0 * 32 + 16, 36, STONE["hi"], star=False)
    _crescent(sheet, 1 * 32 + 14, 34, PATINA_HI, star_color=GLOW)
    _crescent(sheet, 3 * 32 + 14, 34, PATINA_HI, star_color=GLOW)
    sheet.save(path)


# =======================================================================
# 7. Bed - 128x128 + 128x128 mask
# =======================================================================
#
# BedObject cell map: rotation 0 -> strip (96,32) [head at the NEAR end],
# 1 -> block (0,64) [head LEFT], 2 -> strip (64,32) [head at the FAR end],
# 3 -> block (0,0) [head RIGHT].  The _mask sheet uses exactly the same four
# regions and is pure alpha: opaque marks the bedding that must hide the
# sleeping settler, clear marks the pillow where their head shows through.
# Measured against oakbed_mask.png, which is opaque everywhere except a
# rectangle over the pillow.


def _bed_pillow(c, x, y, w, h, rng):
    c.rect(x, y, w, h, SILK["light"])
    c.rect(x, y, w, 2, SILK["hi"])
    c.rect(x, y, 1, h, SILK["hi"])
    c.rect(x + w - 1, y, 1, h, SILK["base"])
    c.rect(x, y + h - 1, w, 1, SILK["deep"])
    c.rect(x + 2, y + h - 2, w - 4, 1, SILK["base"])
    _grain(c, x + 1, y + 2, max(1, w - 2), max(1, h - 3), rng,
           hi=SILK["hi"], lo=SILK["base"], density=30)


def _bed_strip(rng, head_far):
    """32x96 vertical bed.  head_far: the tall headboard is at the top
    (rotation 2); otherwise the head end is nearest the camera (rotation 0)."""
    c = Canvas(32, 96)
    _ground_shadow(c, 16, 90, 14, 4)
    c.rect(2, 70, 28, 22, UNDER)                         # under-bed mass
    c.rect(2, 70, 28, 1, mix(UNDER, IRON["base"], 0.45))
    for x in (2, 26):
        _iron_post(c, x, 70, 4, 21)
        c.rect(x, 89, 4, 2, IRON["deep"])
    if head_far:
        _slab(c, 2, 16, 28, 13, rng)                     # tall headboard
        _panel(c, 6, 19, 20, 8, rng)
        c.rect(5, 14, 22, 2, STONE["light"])
        c.rect(7, 13, 18, 1, STONE["hi"])
        _iron_rail(c, 2, 27, 28, 2)
        c.put(3, 15, PATINA_HI)
        c.put(28, 15, PATINA_HI)
        _bed_pillow(c, 4, 32, 24, 13, rng)
        _bedding(c, 3, 45, 26, 22, rng)
        _slab(c, 2, 66, 28, 5, rng)                      # low footboard
        c.rect(2, 66, 28, 1, STONE["hi"])
    else:
        _slab(c, 2, 22, 28, 7, rng)                      # low far footboard
        c.rect(5, 20, 22, 2, STONE["light"])
        _iron_rail(c, 2, 27, 28, 2)
        _bedding(c, 3, 31, 26, 19, rng)
        _bed_pillow(c, 4, 50, 24, 11, rng)
        _slab(c, 2, 61, 28, 10, rng)                     # tall near headboard
        _panel(c, 6, 63, 20, 5, rng)
        c.rect(2, 61, 28, 1, STONE["hi"])
        c.put(3, 62, PATINA_HI)
        c.put(28, 62, PATINA_HI)
    c.rect(3, 29, 26, 2, STONE["deep"])                  # mattress rail
    c.rect(3, 29, 26, 1, STONE["light"])
    return c


def _bed_block(rng):
    """64x64 horizontal bed with the head end on the RIGHT (rotation 3)."""
    c = Canvas(64, 64)
    _ground_shadow(c, 32, 58, 29, 4)
    c.rect(3, 44, 58, 14, UNDER)
    c.rect(3, 44, 58, 1, mix(UNDER, IRON["base"], 0.45))
    for x in (3, 57):
        _iron_post(c, x, 44, 4, 13)
        c.rect(x, 55, 4, 2, IRON["deep"])
    _slab(c, 2, 22, 7, 23, rng)                          # foot post (left)
    c.rect(2, 22, 7, 1, STONE["hi"])
    c.put(3, 23, PATINA_HI)
    _slab(c, 55, 16, 7, 29, rng)                         # head post (right)
    _panel(c, 56, 19, 5, 12, rng)
    c.rect(55, 16, 7, 1, STONE["hi"])
    c.put(56, 17, PATINA_HI)
    c.rect(9, 28, 46, 2, STONE["deep"])                  # side rail
    c.rect(9, 28, 46, 1, STONE["light"])
    _bedding(c, 9, 30, 30, 14, rng)
    _bed_pillow(c, 39, 30, 16, 14, rng)
    return c


def gen_bed(path, mask_path):
    rng = Rng(0xBED5A1)
    sheet = Canvas(128, 128)
    far, near, block = _bed_strip(rng, True), _bed_strip(rng, False), _bed_block(rng)
    for cell in (far, near, block):
        cell.outline(OUT)
    sheet.paste(block, 0, 0)                             # rot 3, head right
    sheet.paste(block.mirrored(), 0, 64)                 # rot 1, head left
    sheet.paste(far, 64, 32)                             # rot 2, head far
    sheet.paste(near, 96, 32)                            # rot 0, head near
    _crescent(sheet, 64 + 15, 55, PATINA_HI, star_color=GLOW)
    _crescent(sheet, 96 + 15, 98, PATINA_HI, star_color=GLOW)
    _crescent(sheet, 58, 26, PATINA_HI, star=False)      # head post, rot 3
    _crescent(sheet, 5, 90, PATINA_HI, star=False)       # head post, rot 1
    sheet.save(path)

    # --- mask: alpha geometry only, no colour ---
    m = Canvas(128, 128)
    white = (255, 255, 255, 255)

    def block_mask(x0, y0, pillow_x0, pillow_x1):
        for y in range(64):
            for x in range(64):
                if pillow_x0 <= x <= pillow_x1 and 28 <= y <= 46:
                    continue
                if pillow_x0 - 2 <= x <= pillow_x1 + 2 and y < 28:
                    continue
                m.put(x0 + x, y0 + y, white)

    block_mask(0, 0, 39, 54)                             # rot 3: pillow right
    block_mask(0, 64, 9, 24)                             # rot 1: pillow left

    def strip_mask(x0, start):
        for y in range(start, 96):
            inset = max(0, start + 4 - y) * 4
            for x in range(inset, 32 - inset):
                m.put(x0 + x, 32 + y, white)

    strip_mask(64, 45)                                   # rot 2: below the pillow
    strip_mask(96, 61)                                   # rot 0: the near headboard
    m.save(mask_path)


# =======================================================================
# 8. Candelabra - 128x64 lit + 128x64 unlit
# =======================================================================


def _candelabra(rng, lit, single):
    c = Canvas(32, 64)
    _ground_shadow(c, 16, 58, 12, 3)
    _slab(c, 7, 51, 18, 6, rng)                          # stone foot
    c.rect(7, 51, 18, 1, STONE["hi"])
    c.rect(7, 55, 18, 1, PATINA)
    _slab(c, 10, 47, 12, 5, rng)
    _iron_post(c, 13, 30, 6, 18)                         # stem
    c.rect(14, 31, 1, 16, IRON["hi"])
    _iron_rail(c, 11, 37, 10, 3)                         # collar
    c.put(12, 38, PATINA_HI)
    c.put(19, 38, PATINA)
    if single:
        _iron_rail(c, 11, 25, 10, 5)                     # bar seen end on
        c.rect(12, 23, 8, 2, IRON["light"])
        cups, heights = ((16, 23),), (17,)
    else:
        _iron_rail(c, 3, 25, 26, 5)                      # cross bar
        c.rect(4, 23, 24, 2, IRON["light"])
        c.put(4, 26, PATINA_HI)
        c.put(27, 26, PATINA_HI)
        cups, heights = ((7, 23), (16, 23), (25, 23)), (12, 17, 12)
    for (cx, cy), h in zip(cups, heights):
        c.rect(cx - 4, cy - 1, 8, 3, IRON["base"])       # cup
        c.rect(cx - 4, cy - 1, 8, 1, IRON["light"])
        c.rect(cx - 4, cy + 1, 8, 1, IRON["deep"])
        top = cy - h
        c.rect(cx - 3, top, 6, h, SILK["base"])          # windsilk wax
        c.rect(cx - 3, top, 6, 2, SILK["hi"])
        c.rect(cx - 3, top, 1, h, SILK["light"])
        c.rect(cx + 2, top + 1, 1, h - 1, SILK["deep"])
        for dy in (h // 3, 2 * h // 3):                  # wax drips
            c.put(cx - 2, top + dy, SILK["hi"])
            c.put(cx + 1, top + dy + 1, SILK["deep"])
        if not lit:
            c.rect(cx - 1, top - 1, 2, 1, IRON["deep"])  # cold wick
    return c, cups, heights


def gen_candelabra(path, lit):
    rng = Rng(0xCA9DE1 if lit else 0xCA9DE0)
    sheet = Canvas(128, 64)
    built = []
    for i in range(4):
        cell, cups, heights = _candelabra(rng, lit, single=i in (1, 3))
        cell.outline(OUT)
        _paste_col(sheet, cell, i)
        built.append((i, cups, heights))
    if lit:
        for i, cups, heights in built:
            for n, ((cx, cy), h) in enumerate(zip(cups, heights)):
                _flame(sheet, i * 32 + cx, cy - h - 1, 8 if h > 13 else 6,
                       rng, seed_shift=n + i)
    sheet.save(path)


# =======================================================================
# 9. Carpet - objects/carpets/<id>.png 64x64 pattern + <id>mask.png 64x64
# =======================================================================
#
# ModularCarpetObject tiles ONE 32 px pattern cell across a 96x64 "part"
# texture and multiplies the mask over it, so the pattern must be seamless
# with itself at a 32 px period; the mask is geometry only.


def gen_carpet(path):
    """A windsilk rug with an all-over sky-iron lattice, like vanilla's
    goldgridcarpet: no drawn frame, because the engine's mask makes the edges
    and a framing band would read as a tiled floor instead of a rug.  The
    weave has to stay within a few value units of the base - a 2 px two-tone
    checker at any real contrast reads as an alpha checkerboard, not cloth."""
    rng = Rng(0xCA89E7)
    c = Canvas(64, 64)
    base = SILK["base"]
    warp = mix(base, SILK["light"], 0.30)
    weft = mix(base, SILK["deep"], 0.16)
    for y in range(64):
        for x in range(64):
            v = base
            if y % 4 == 1:
                v = warp
            elif x % 4 == 2:
                v = weft
            c.put(x, y, v)
    for _ in range(46):                                  # slubs in the weave
        c.put(rng.range(0, 63), rng.range(0, 63), SILK["light"])
    for _ in range(30):
        c.put(rng.range(0, 63), rng.range(0, 63), mix(base, SILK["deep"], 0.45))
    # a 32 px diamond lattice: every 32x32 sub-cell has identical structure, so
    # whichever cell the engine picks tiles seamlessly with any other
    lat = mix(SILK["deep"], PATINA, 0.55)
    lat_hi = mix(SILK["light"], PATINA_HI, 0.35)
    for oy in (0, 32):
        for ox in (0, 32):
            for i in range(16):
                for sx, sy in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
                    x = (ox + 16 + sx * i) % 64
                    y = (oy + 16 + sy * (16 - i)) % 64
                    c.put(x, y, lat)
                    c.put((x + sx) % 64, y, lat)
                    c.put(x, (y + sy) % 64, lat_hi)
    for oy in (16, 48):
        for ox in (16, 48):
            _crescent(c, ox, oy, PATINA, star_color=PATINA_HI)
    c.save(path)


def gen_carpet_mask(path):
    """64x64 alpha geometry: a 4x4 grid of 16 px cells.  Clear outside the
    rug, one 2 px band of ~third-alpha dither, then opaque.  Cell (0,0) is the
    outer corner (two clear edges), (2..3, 2..3) are the diagonal notches.
    Measured targets from the vanilla mask: ~48% opaque for a two-edge corner
    cell, ~69% for a one-edge cell, ~89-95% for a notch cell."""
    m = Canvas(64, 64)
    full = (255, 255, 255, 255)
    dith = (0, 0, 0, 91)

    def wobble(u):
        """Edge inset along the edge; period 32 so the (2,0)/(3,0) pair and
        the (0,2)/(0,3) pair tile when the rug runs on."""
        return (2, 4, 4, 2, 2, 4, 4, 4)[(u // 4) % 8]

    CLEAR = {
        (0, 0): "TL", (1, 0): "TR", (0, 1): "LB", (1, 1): "RB",
        (2, 0): "T", (3, 0): "T", (2, 1): "B", (3, 1): "B",
        (0, 2): "L", (1, 2): "R", (0, 3): "L", (1, 3): "R",
    }
    NOTCH = {(2, 2): "TL", (3, 2): "TR", (2, 3): "BL", (3, 3): "BR"}
    QUAD = {  # which quadrant of the conceptual 32x32 tile each cell is
        (0, 0): (0, 0), (1, 0): (1, 0), (0, 1): (0, 1), (1, 1): (1, 1),
        (2, 0): (0, 0), (3, 0): (1, 0), (2, 1): (0, 1), (3, 1): (1, 1),
        (0, 2): (0, 0), (1, 2): (1, 0), (0, 3): (0, 1), (1, 3): (1, 1),
        (2, 2): (0, 0), (3, 2): (1, 0), (2, 3): (0, 1), (3, 3): (1, 1),
    }
    for (cx, cy), (qx, qy) in QUAD.items():
        clear = CLEAR.get((cx, cy), "")
        notch = NOTCH.get((cx, cy))
        for y in range(16):
            for x in range(16):
                tx, ty = qx * 16 + x, qy * 16 + y
                a = full
                if "T" in clear:
                    k = wobble(tx)
                    a = None if ty < k else (dith if ty < k + 2 else a)
                if a is not None and "B" in clear:
                    k = wobble(tx + 16)
                    a = None if ty >= 32 - k else (dith if ty >= 30 - k else a)
                if a is not None and "L" in clear:
                    k = wobble(ty)
                    a = None if tx < k else (dith if tx < k + 2 else a)
                if a is not None and "R" in clear:
                    k = wobble(ty + 16)
                    a = None if tx >= 32 - k else (dith if tx >= 30 - k else a)
                if notch:
                    nx = x if notch[1] == "L" else 15 - x
                    ny = y if notch[0] == "T" else 15 - y
                    if nx + ny < 3:
                        a = None
                    elif nx + ny < 6:
                        a = dith
                if a is not None:
                    m.put(cx * 16 + x, cy * 16 + y, a)
    m.save(path)


# =======================================================================
# 10-13. Table decorations - 32x32, bottom-anchored
# =======================================================================


def gen_chalice(path):
    """A skystone cup on a sky-iron stem with a stormshard in the knop.
    Vanilla's goldchalice is a small ROUNDED cup, not a cone - the first pass
    tapered from the rim and read as a martini glass."""
    rng = Rng(0xC4A11C)
    c = Canvas(32, 32)
    _ground_shadow(c, 16, 29, 8, 2)
    c.ellipse(16, 28, 7, 3, IRON["deep"])                # foot
    c.ellipse(16, 27, 6, 2, IRON["base"])
    c.ellipse(15, 26, 5, 1, IRON["light"])
    _iron_post(c, 14, 19, 4, 8)                          # stem
    c.rect(11, 20, 10, 4, IRON["base"])                  # knop
    c.rect(11, 20, 10, 1, IRON["light"])
    c.rect(11, 23, 10, 1, IRON["deep"])
    c.rect(14, 21, 4, 2, PATINA)                         # stormshard, set in
    c.rect(15, 21, 2, 1, PATINA_HI)
    # bowl: near-vertical sides with a rounded bottom, dark silhouette first
    # so the outline pass cannot eat the rim
    PROFILE = ((6, 0), (7, 0), (8, 0), (9, 0), (10, 0), (11, 0), (12, 0),
               (13, 1), (14, 1), (15, 2), (16, 3), (17, 5), (18, 6))
    for dy, inset in PROFILE:
        c.rect(7 + inset, dy, 18 - 2 * inset, 1, IRON["deep"])
    for dy, inset in PROFILE:
        if 18 - 2 * inset > 2:
            c.rect(8 + inset, dy, 16 - 2 * inset, 1, STONE["base"])
    c.rect(8, 7, 16, 2, STONE["hi"])
    for dy, inset in PROFILE:
        if 18 - 2 * inset > 3 and dy > 8:
            c.put(8 + inset, dy, STONE["light"])
            c.put(23 - inset, dy, STONE["deep"])
    _grain(c, 9, 10, 14, 6, rng, density=16)
    _iron_rail(c, 7, 6, 18, 2)                           # iron rim
    c.rect(8, 6, 16, 1, IRON["light"])
    c.outline(OUT)
    c.rect(9, 9, 14, 4, with_alpha(GLOW, 150))           # the drink, glowing
    c.rect(10, 9, 12, 1, with_alpha(GLOW_HI, 200))
    c.put(11, 10, GLOW_HI)
    c.put(19, 11, with_alpha(GLOW_HI, 170))
    c.save(path)


def gen_candle(path):
    rng = Rng(0xCA9D1E)
    c = Canvas(32, 32)
    _ground_shadow(c, 16, 29, 8, 2)
    c.ellipse(16, 28, 8, 3, IRON["deep"])                # dish
    c.ellipse(16, 27, 7, 2, IRON["base"])
    c.ellipse(15, 26, 6, 1, IRON["light"])
    c.rect(9, 25, 14, 1, IRON["deep"])
    c.put(11, 26, PATINA_HI)
    c.put(20, 26, PATINA)
    c.rect(10, 11, 13, 15, SILK["base"])                 # pillar of wax
    c.rect(10, 11, 13, 2, SILK["hi"])
    c.rect(10, 13, 2, 13, SILK["light"])
    c.rect(21, 13, 2, 13, SILK["deep"])
    for y in (15, 19, 23):                               # wax drips
        c.rect(12, y, 2, 1, SILK["hi"])
        c.rect(19, y + 1, 2, 1, SILK["deep"])
    c.rect(12, 10, 9, 1, SILK["light"])
    c.rect(10, 24, 13, 1, PATINA)
    c.outline(OUT)
    c.rect(16, 9, 1, 2, IRON["deep"])                    # wick
    _flame(c, 16, 9, 8, rng)
    c.save(path)


def gen_tome(path):
    rng = Rng(0x70BE01)
    c = Canvas(32, 32)
    _ground_shadow(c, 16, 29, 12, 2)
    _slab(c, 3, 23, 26, 6, rng)                          # stone lectern block
    c.rect(3, 23, 26, 1, STONE["hi"])
    c.rect(3, 27, 26, 1, PATINA)
    cover = mix(IRON["base"], palette.STORMCRYSTAL["base"], 0.5)
    cover_hi = mix(IRON["light"], palette.STORMCRYSTAL["light"], 0.55)
    c.rect(3, 9, 26, 15, IRON["deep"])                   # cover silhouette
    c.rect(4, 10, 24, 13, cover)
    c.rect(4, 10, 24, 2, cover_hi)
    c.rect(4, 10, 2, 13, cover_hi)
    c.rect(26, 12, 2, 11, IRON["deep"])
    c.rect(4, 22, 24, 1, IRON["deep"])
    c.rect(21, 10, 7, 13, SILK["light"])                 # page block
    c.rect(21, 10, 7, 2, SILK["hi"])
    for y in range(13, 23, 2):
        c.rect(21, y, 7, 1, SILK["deep"])
    _iron_rail(c, 3, 13, 5, 3)                           # clasps
    _iron_rail(c, 3, 19, 5, 3)
    c.put(4, 14, PATINA_HI)
    c.put(4, 20, PATINA_HI)
    _grain(c, 7, 13, 12, 8, rng, hi=cover_hi, lo=IRON["deep"], density=22)
    c.outline(OUT)
    _crescent(c, 13, 16, PATINA_HI, star_color=GLOW)
    c.save(path)


def gen_pottedcloudberry(path):
    rng = Rng(0xB077E5)
    c = Canvas(32, 32)
    _ground_shadow(c, 16, 29, 10, 2)
    c.ellipse(16, 11, 11, 8, BERRY["leaf_deep"])         # bush
    c.ellipse(15, 10, 10, 7, BERRY["leaf"])
    c.ellipse(14, 8, 7, 4, BERRY["leaf_light"])
    for _ in range(16):
        lx, ly = rng.range(6, 25), rng.range(4, 17)
        if c.filled(lx, ly):
            c.put(lx, ly, BERRY["leaf_light"] if rng.chance(0.5) else BERRY["leaf_deep"])
    berries = []
    for _ in range(8):
        bx, by = rng.range(7, 23), rng.range(5, 16)
        if c.filled(bx, by) and c.filled(bx + 1, by + 1):
            berries.append((bx, by))
            c.rect(bx, by, 2, 2, BERRY["berry_deep"])
            c.put(bx, by, BERRY["berry"])
    _slab(c, 6, 17, 20, 11, rng)                         # skystone pot
    c.rect(6, 17, 20, 2, STONE["hi"])
    c.rect(8, 16, 16, 1, STONE["light"])
    _iron_rail(c, 5, 18, 22, 3)                          # rim band
    c.put(6, 19, PATINA_HI)
    c.put(25, 19, PATINA)
    c.rect(8, 22, 16, 5, STONE["base"])
    _grain(c, 8, 22, 16, 5, rng, density=14)
    c.rect(8, 26, 16, 1, STONE["deep"])
    c.outline(OUT)
    for bx, by in berries:                               # berry highlight last
        c.put(bx, by, BERRY["berry_hi"])
    _crescent(c, 14, 24, PATINA_HI, star=False)
    c.save(path)


# =======================================================================
# Item icons - 32x32 product shots that must read at 1x in a slot
# =======================================================================


def _icon_chair(path):
    rng = Rng(0x1C0001)
    c = Canvas(32, 32)
    _under_mass(c, 8, 24, 16, 6, (8, 20))
    _slab(c, 5, 19, 22, 5, rng)
    _iron_rail(c, 5, 22, 22, 2)
    _cushion(c, 6, 14, 20, 5, rng)
    _slab(c, 9, 11, 14, 4, rng)
    _slab(c, 5, 5, 5, 8, rng)
    _slab(c, 22, 5, 5, 8, rng)
    _panel(c, 10, 5, 12, 8, rng)
    _slab(c, 5, 2, 22, 4, rng)
    _iron_rail(c, 5, 4, 22, 2)
    c.outline(OUT)
    _crescent(c, 15, 9, PATINA_HI, star_color=GLOW)
    c.save(path)


def _icon_bench(path):
    rng = Rng(0x1C0002)
    c = Canvas(32, 32)
    _under_mass(c, 3, 25, 26, 6, (3, 25), leg_w=5)
    _slab(c, 1, 20, 30, 5, rng)
    _iron_rail(c, 1, 23, 30, 2)
    _cushion(c, 2, 15, 28, 5, rng)
    for x in (1, 14, 27):
        _slab(c, x, 4, 4, 11, rng)
    for x in (5, 18):
        _panel(c, x, 4, 9, 11, rng)
    _slab(c, 1, 1, 30, 4, rng)
    _iron_rail(c, 1, 3, 30, 2)
    c.outline(OUT)
    for cx in (9, 22):
        _crescent(c, cx, 9, PATINA_HI, star=False)
    c.save(path)


def _icon_modulartable(path):
    rng = Rng(0x1C0003)
    c = Canvas(32, 32)
    _under_mass(c, 4, 21, 24, 10, (4, 24), leg_w=5)
    _slab(c, 1, 5, 30, 14, rng)
    c.rect(1, 5, 30, 2, STONE["hi"])
    c.rect(1, 9, 30, 1, PATINA)
    c.rect(5, 6, 1, 13, PATINA)
    c.rect(26, 6, 1, 13, PATINA)
    c.rect(1, 19, 30, 2, STONE["hi"])
    c.rect(1, 21, 30, 2, STONE["deep"])
    _grain(c, 7, 11, 18, 7, rng, density=20)
    c.outline(OUT)
    c.save(path)


def _icon_dinnertable(path):
    rng = Rng(0x1C0004)
    c = Canvas(32, 32)
    _under_mass(c, 3, 20, 26, 11, (3, 24), leg_w=5)
    _slab(c, 0, 4, 32, 14, rng)
    c.rect(0, 4, 32, 2, STONE["hi"])
    c.rect(0, 8, 32, 1, PATINA)
    c.rect(0, 15, 32, 1, PATINA)
    c.rect(4, 5, 1, 13, PATINA)
    c.rect(27, 5, 1, 13, PATINA)
    c.rect(0, 18, 32, 2, STONE["hi"])
    c.rect(0, 20, 32, 2, STONE["deep"])
    _grain(c, 6, 10, 20, 5, rng, density=20)
    c.outline(OUT)
    _crescent(c, 15, 12, PATINA_HI, star_color=GLOW)
    c.save(path)


def _icon_desk(path):
    rng = Rng(0x1C0005)
    c = Canvas(32, 32)
    _under_mass(c, 3, 24, 26, 7, (3, 25))
    _slab(c, 2, 13, 28, 12, rng)
    for dy in (14, 19):
        c.rect(4, dy, 24, 4, STONE["light"])
        c.rect(4, dy, 24, 1, STONE["hi"])
        c.rect(4, dy + 3, 24, 1, STONE["deep"])
        _iron_rail(c, 11, dy + 1, 10, 2)
        c.put(12, dy + 1, PATINA_HI)
    _slab(c, 0, 8, 32, 5, rng)
    c.rect(0, 8, 32, 2, STONE["hi"])
    c.rect(0, 11, 32, 1, PATINA)
    _slab(c, 3, 0, 26, 8, rng)
    _panel(c, 6, 2, 20, 5, rng)
    _iron_rail(c, 3, 6, 26, 2)
    c.outline(OUT)
    _crescent(c, 14, 4, PATINA_HI, star_color=GLOW)
    c.save(path)


def _icon_dresser(path):
    rng = Rng(0x1C0006)
    c = Canvas(32, 32)
    _under_mass(c, 3, 27, 26, 4, (3, 25))
    _slab(c, 2, 6, 28, 21, rng)
    for dy in (9, 18):
        c.rect(4, dy, 24, 7, STONE["light"])
        c.rect(4, dy, 24, 1, STONE["hi"])
        c.rect(4, dy + 6, 24, 1, STONE["deep"])
        _grain(c, 5, dy + 1, 22, 5, rng, hi=STONE["hi"], lo=STONE["base"], density=24)
        _iron_rail(c, 11, dy + 2, 10, 3)
        c.rect(12, dy + 2, 8, 1, IRON["hi"])
        c.put(12, dy + 3, PATINA_HI)
    _slab(c, 1, 1, 30, 6, rng)
    c.rect(1, 1, 30, 2, STONE["hi"])
    c.rect(1, 5, 30, 1, PATINA)
    _iron_rail(c, 2, 7, 28, 2)
    c.outline(OUT)
    c.save(path)


def _icon_bed(path):
    rng = Rng(0x1C0007)
    c = Canvas(32, 32)
    _under_mass(c, 2, 23, 28, 7, (2, 26))
    _slab(c, 1, 5, 6, 18, rng)                           # head post left
    _panel(c, 2, 8, 4, 8, rng)
    c.rect(1, 5, 6, 1, STONE["hi"])
    _slab(c, 25, 10, 6, 13, rng)                         # foot post right
    c.rect(25, 10, 6, 1, STONE["hi"])
    c.rect(7, 12, 18, 2, STONE["deep"])
    c.rect(7, 12, 18, 1, STONE["light"])
    _bed_pillow(c, 7, 14, 9, 9, rng)
    _bedding(c, 16, 14, 9, 9, rng)
    c.outline(OUT)
    _crescent(c, 3, 11, PATINA_HI, star=False)
    c.save(path)


def _icon_candelabra(path):
    rng = Rng(0x1C0008)
    c = Canvas(32, 32)
    _slab(c, 8, 24, 16, 6, rng)
    c.rect(8, 24, 16, 1, STONE["hi"])
    c.rect(8, 28, 16, 1, PATINA)
    _iron_post(c, 13, 13, 6, 12)
    _iron_rail(c, 11, 18, 10, 3)
    c.put(12, 19, PATINA_HI)
    _iron_rail(c, 3, 10, 26, 4)
    c.rect(4, 8, 24, 2, IRON["light"])
    cups, heights = ((7, 8), (16, 8), (25, 8)), (5, 8, 5)
    for (cx, cy), h in zip(cups, heights):
        c.rect(cx - 3, cy - 1, 7, 2, IRON["base"])
        c.rect(cx - 3, cy - h, 6, h, SILK["base"])
        c.rect(cx - 3, cy - h, 6, 2, SILK["hi"])
        c.rect(cx + 2, cy - h + 1, 1, h - 1, SILK["deep"])
    c.outline(OUT)
    for n, ((cx, cy), h) in enumerate(zip(cups, heights)):
        _flame(c, cx, cy - h - 1, 4 if h < 7 else 6, rng, seed_shift=n)
    c.save(path)


def _icon_carpet(path):
    """A rolled-flat swatch with fringed ends: the same read as vanilla's
    woolcarpet icon, not a shrunk-down screenshot of the pattern sheet."""
    rng = Rng(0x1C0009)
    c = Canvas(32, 32)
    base = SILK["base"]
    warp = mix(base, SILK["light"], 0.30)
    weft = mix(base, SILK["deep"], 0.16)
    for y in range(3, 29):
        for x in range(2, 30):
            v = base
            if y % 4 == 1:
                v = warp
            elif x % 4 == 2:
                v = weft
            c.put(x, y, v)
    for _ in range(20):
        c.put(rng.range(3, 28), rng.range(4, 27), SILK["light"])
    lat = mix(SILK["deep"], PATINA, 0.55)
    lat_hi = mix(SILK["light"], PATINA_HI, 0.35)
    for i in range(12):
        for sx, sy in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
            x, y = 16 + sx * i, 16 + sy * (12 - i)
            if 2 <= x <= 29 and 3 <= y <= 28:
                c.put(x, y, lat)
                c.put(x + sx, y, lat)
                c.put(x, y + sy, lat_hi)
    c.rect(2, 3, 28, 1, mix(base, SILK["deep"], 0.5))
    c.rect(2, 28, 28, 1, mix(base, SILK["deep"], 0.5))
    for x in range(3, 30, 3):                            # fringe
        c.rect(x, 29, 1, 2, SILK["light"])
        c.rect(x, 1, 1, 2, SILK["light"])
    c.outline(OUT)
    _crescent(c, 16, 16, PATINA, star_color=PATINA_HI)
    c.save(path)


def gen_item_icons(items_dir):
    _icon_chair(f"{items_dir}/skywatchchair.png")
    _icon_bench(f"{items_dir}/skywatchbench.png")
    _icon_modulartable(f"{items_dir}/skywatchmodulartable.png")
    _icon_dinnertable(f"{items_dir}/skywatchdinnertable.png")
    _icon_desk(f"{items_dir}/skywatchdesk.png")
    _icon_dresser(f"{items_dir}/skywatchdresser.png")
    _icon_bed(f"{items_dir}/skywatchbed.png")
    _icon_candelabra(f"{items_dir}/skywatchcandelabra.png")
    _icon_carpet(f"{items_dir}/skywatchcarpet.png")
    # the four table decorations already read at 32x32, so the world sprite
    # and the inventory icon are the same drawing
    gen_chalice(f"{items_dir}/skywatchchalice.png")
    gen_candle(f"{items_dir}/skywatchcandle.png")
    gen_tome(f"{items_dir}/skywatchtome.png")
    gen_pottedcloudberry(f"{items_dir}/pottedcloudberry.png")


# =======================================================================
# entry point
# =======================================================================


def generate(objects_dir, items_dir):
    carpets_dir = os.path.join(objects_dir, "carpets")
    os.makedirs(objects_dir, exist_ok=True)
    os.makedirs(items_dir, exist_ok=True)
    os.makedirs(carpets_dir, exist_ok=True)

    gen_chair(f"{objects_dir}/skywatchchair.png")
    gen_bench(f"{objects_dir}/skywatchbench.png")
    gen_modulartable(f"{objects_dir}/skywatchmodulartable.png")
    gen_dinnertable(f"{objects_dir}/skywatchdinnertable.png")
    gen_desk(f"{objects_dir}/skywatchdesk.png")
    gen_dresser(f"{objects_dir}/skywatchdresser.png")
    gen_bed(f"{objects_dir}/skywatchbed.png", f"{objects_dir}/skywatchbed_mask.png")
    gen_candelabra(f"{objects_dir}/skywatchcandelabra.png", True)
    gen_candelabra(f"{objects_dir}/skywatchcandelabra_off.png", False)
    gen_carpet(f"{carpets_dir}/skywatchcarpet.png")
    gen_carpet_mask(f"{carpets_dir}/skywatchcarpetmask.png")
    gen_chalice(f"{objects_dir}/skywatchchalice.png")
    gen_candle(f"{objects_dir}/skywatchcandle.png")
    gen_tome(f"{objects_dir}/skywatchtome.png")
    gen_pottedcloudberry(f"{objects_dir}/pottedcloudberry.png")

    gen_item_icons(items_dir)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("usage: gen_skyfurniture.py <objects_dir> <items_dir>")
        raise SystemExit(2)
    generate(sys.argv[1], sys.argv[2])
    print(f"Skywatch furniture written to {sys.argv[1]} / {sys.argv[2]}")
