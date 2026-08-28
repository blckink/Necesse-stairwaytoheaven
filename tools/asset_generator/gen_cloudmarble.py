"""Cloudmarble — white-and-gold cloud masonry — and the Skyway Passages ground.

Design source: docs/references/{cloudmarble-wall,cloudmarble-door-fence,
skyway-floor}-reference.png, described in docs/references/skyway-spec.md. Those
are anti-aliased renders (77k-110k colours, 1px runs, transparency painted
black); nothing here is downsampled from them. Everything is redrawn at the real
sheet formats, at 32px/tile, on the 4-step ramps below.

Motifs carried over from the references:
  * rounded cloud cobbles with a 1px GOLD RIM on the lit (top-left) edge only
  * a soft blue SWIRL glyph curled inside the larger stones, never the small ones
  * a FOUR-POINT STAR as punctuation: door leaves, wall cornice, sparse on floor
  * a GOLDEN ARCADE along the wall's front face, a thin GOLD CORNICE on its cap

Sheets produced (formats are docs/research/asset-formats.md + splat-format.md,
never invented here):

  objects/cloudmarblewall.png      352x128  three readers, see _build_wall
  objects/cloudmarblefence.png     160x64   post/N-joint/S-rail/W-run/E-run
  objects/cloudmarblefencegate.png 192x64   six engine-fixed columns
  tiles/skyway.png                 32x32    legacy plain tile (fallback)
  tiles/skyway_splat.png           224x384  4 sections of the 21-cell atlas
  items/cloudmarble{wall,door,window,fence,fencegate}.png, items/skywaytile.png

Deterministic: every random draw goes through px.Rng with an explicit seed, so
two runs produce byte-identical PNGs.
"""

import math
import os
import sys

from px import Canvas, Rng, mix, with_alpha
import palette
import gen_splats

# ---------------------------------------------------------------------------
# Palette. Sampled out of the three reference renders and quantized to the
# 4-step ramp every other material in this mod is built on (skyway-spec.md).
# Deliberately local to this file — palette.py is owned elsewhere.
# ---------------------------------------------------------------------------

CLOUDMARBLE = {                  # the white cloud-stone body
    "deep":  (186, 206, 224),
    "base":  (214, 228, 236),
    "light": (233, 240, 243),
    "hi":    (247, 249, 250),
}
CLOUDMORTAR = (195, 219, 234)    # pale blue between the stones
CLOUDGLYPH = (176, 208, 228)     # the soft blue swirl drawn into each stone
SKYGOLD = {                      # rims, cornices, arches and four-point stars
    "deep":  (166, 140,  96),
    "base":  (200, 176, 128),
    "light": (216, 196, 150),
    "hi":    (236, 222, 186),
}

# Derived, not sampled: the wall's CAP is the same stone seen from directly
# above, so it is the body ramp pushed toward the outline. Keeping it a derived
# mix (rather than a second sampled ramp) is what stops the cap drifting off the
# material as the body ramp is tuned. The cap has to be clearly darker than the
# face or a top-down wall reads as one flat slab with no thickness.
CAP = {
    "deep":  mix(CLOUDMARBLE["deep"], palette.OUTLINE, 0.28),
    "base":  mix(CLOUDMARBLE["deep"], palette.OUTLINE, 0.15),
    "light": mix(CLOUDMARBLE["base"], palette.OUTLINE, 0.08),
    "hi":    mix(CLOUDMARBLE["light"], palette.OUTLINE, 0.03),
}
MORTAR_DEEP = mix(CLOUDMORTAR, palette.OUTLINE, 0.22)
# The shaded rim of a cobble. CLOUDMARBLE["deep"] is LIGHTER than the mortar it
# sits on, so using it as the far edge leaves every stone dissolving into the
# bed; a white material still needs one step below its own ramp to hold an edge.
STONE_SHADE = mix(CLOUDMARBLE["deep"], palette.OUTLINE, 0.16)
MORTAR_HI = mix(CLOUDMORTAR, CLOUDMARBLE["hi"], 0.45)
GLYPH_DEEP = mix(CLOUDGLYPH, palette.OUTLINE, 0.16)

OUT = palette.OUTLINE


# ---------------------------------------------------------------------------
# A canvas that wraps, so a motif drawn across the seam of a tile comes back on
# the other side. Everything position-locked (floor cobbles, wall courses) is
# painted through one of these, which is why the sheets tile without a join.
# ---------------------------------------------------------------------------

class Wrap:
    def __init__(self, w, h, wrap_y=False):
        self.c = Canvas(w, h)
        self.w, self.h = w, h
        self.wrap_y = wrap_y

    def put(self, x, y, color):
        if self.wrap_y:
            self.c.put(x % self.w, y % self.h, color)
        elif 0 <= y < self.h:
            self.c.put(x % self.w, y, color)

    def get(self, x, y):
        if self.wrap_y:
            return self.c.get(x % self.w, y % self.h)
        if 0 <= y < self.h:
            return self.c.get(x % self.w, y)
        return (0, 0, 0, 0)

    def filled(self, x, y):
        return self.get(x, y)[3] > 0

    def rect(self, x, y, w, h, color):
        for j in range(h):
            for i in range(w):
                self.put(x + i, y + j, color)

    def blit_to(self, canvas, dst_x, dst_y, sx=0, sy=0, w=None, h=None):
        w = self.w if w is None else w
        h = self.h if h is None else h
        for j in range(h):
            for i in range(w):
                p = self.get(sx + i, sy + j)
                if p[3]:
                    canvas.put(dst_x + i, dst_y + j, p)


# ---------------------------------------------------------------------------
# Shared motifs
# ---------------------------------------------------------------------------

def star4(target, cx, cy, rx, ry, edge=None, body=None, core=None, sharp=0.62):
    """The set's four-point star: an astroid, so the sides are CONCAVE and the
    four points read as spikes rather than a diamond's corners. `sharp` below 1
    pinches the waist; 0.62 matches the references' proportions.

    Three zones, which is how the v2 references draw it: a gold EDGE, a brighter
    gold BODY, and — on anything big enough to show one — a pale blue CORE. That
    blue centre is the whole reason the star reads as this set's punctuation and
    not as a generic sparkle. Always a filled shape, never a 1px outline: a
    hairline star disappears at the size players see it."""
    edge = SKYGOLD["base"] if edge is None else edge
    body = SKYGOLD["light"] if body is None else body
    for dy in range(-ry, ry + 1):
        for dx in range(-rx, rx + 1):
            u = (abs(dx) / rx) ** sharp + (abs(dy) / ry) ** sharp
            if u > 1.0:
                continue
            if core is not None and u < 0.30:
                target.put(cx + dx, cy + dy, core)
            elif u < 0.66:
                target.put(cx + dx, cy + dy, body)
            else:
                target.put(cx + dx, cy + dy, edge)


def gold_star(target, cx, cy, rx, ry, blue=True):
    """A star in the set's own colours: gold rim, bright gold body, and the
    references' pale blue core once it is at least 3px across."""
    core = CLOUDGLYPH if (blue and rx >= 3 and ry >= 4) else None
    star4(target, cx, cy, rx, ry, SKYGOLD["base"], SKYGOLD["hi"], core)


def gold_lozenge(target, cx, cy, rx, ry):
    """The gold pendant diamond the v2 arcade hangs at each springing point, and
    the one the railing posts carry at mid-height. A plain diamond -- convex
    sides -- so it never competes with the four-point star."""
    for dy in range(-ry, ry + 1):
        for dx in range(-rx, rx + 1):
            u = abs(dx) / rx + abs(dy) / ry
            if u > 1.0:
                continue
            target.put(cx + dx, cy + dy,
                       CLOUDGLYPH if u < 0.34 and rx >= 2 else
                       SKYGOLD["light"] if u < 0.72 else SKYGOLD["base"])


def swirl(target, cx, cy, radius, color, turns=1.35, phase=0.0, steps=None):
    """The soft blue cloud curl. A spiral sampled densely enough to be
    4-connected: a curl with holes in it reads as noise at 1x, not as a glyph."""
    steps = steps or int(26 * radius)
    for i in range(steps + 1):
        t = i / steps
        ang = phase + t * turns * 2.0 * math.pi
        r = radius * (1.0 - 0.78 * t)
        target.put(int(round(cx + r * math.cos(ang))),
                   int(round(cy + r * math.sin(ang))), color)


def _stone_tone(s, floor_ramp=False):
    """Ramp step for the BODY of a cobble. `s` is the position along the
    top-left -> bottom-right light axis. Four flat steps, light from the
    top-left, no gradient.

    Weighted BRIGHT on purpose. This is a white material laid on a pale blue
    bed, so a stone whose lower half sits on the bottom two ramp steps is
    darker than the mortar around it and the wall reads grey. Vanilla can
    afford an even split because its stone is mid-grey on dark mortar; ours
    cannot."""
    if floor_ramp:
        # Terrain runs on a NARROWER ramp than an object of the same material.
        # Calibrated, not eyeballed: mean per-pixel luma spread over the four
        # full-variant cells is 4.1 for vanilla snow (the closest analogue, mean
        # luma 237), 8.4 for dirt and 9.5 for this mod's own cloudturf. The
        # first cut of this ground measured 13.4 at mean luma 216 -- as much
        # local contrast as ash, on a near-white material, which is what "the
        # floor reads noisy at 1x" is in numbers.
        if s < -0.55:
            return CLOUDMARBLE["hi"]
        return CLOUDMARBLE["light"] if s < 0.30 else CLOUDMARBLE["base"]
    if s < -0.30:
        return CLOUDMARBLE["hi"]
    if s < 0.16:
        return CLOUDMARBLE["light"]
    if s < 0.58:
        return CLOUDMARBLE["base"]
    return CLOUDMARBLE["deep"]


def cobble(target, cx, cy, rx, ry, rng, gold=True, glyph=False, shadow=True,
           n=2.0, shade=None, floor_ramp=False, glyph_tone=None,
           gold_pair=None):
    """One rounded cloud stone: four-step body, a 1px GOLD rim on the lit edge
    only (never a full outline -- that is what carries 'gold' at 1x), a 1px
    shaded rim on the far side, a thin mortar joint under it, and optionally the
    blue swirl curled inside.

    `n` is the superellipse exponent: 2 is a true ellipse (the floor's irregular
    cobbles), 4 a rounded rectangle (the wall's coursed blocks). Ellipses do NOT
    tessellate -- four of them leave a diamond of mortar at every corner -- so a
    coursed wall built from ellipses reads as blue mortar with white centres
    rather than as white masonry with blue joints.

    `shade`/`floor_ramp` drop the contrast for GROUND use. A wall is an object
    and carries object contrast; terrain does not. Measured against vanilla's
    own ground sheets (dirt, snow, ash) at 1x, a terrain tile is a nearly flat
    field with a few percent of micro-detail -- snow is the closest analogue to
    this material and is essentially white-on-white with faint pale swirls. The
    same cobble drawn at wall contrast tiles into visual static.

    The rim is measured in PIXELS from the edge, not as a fraction of the
    radius. A fraction-based band is 2-3px thick on a big stone, and a wall of
    big stones then reads as grey mortar with white centres instead of white
    stone with blue joints -- which is what the first pass did."""
    x0, x1 = int(math.floor(cx - rx - 1)), int(math.ceil(cx + rx + 1))
    y0, y1 = int(math.floor(cy - ry - 1)), int(math.ceil(cy + ry + 1))
    scale = min(rx, ry)
    shade = STONE_SHADE if shade is None else shade
    g_lit, g_mid = gold_pair or (SKYGOLD["light"], SKYGOLD["base"])
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            dx, dy = (x - cx) / rx, (y - cy) / ry
            u = abs(dx) ** n + abs(dy) ** n
            d2 = dx * dx + dy * dy
            s = (dx + dy) * 0.5
            if u <= 1.0:
                inset = (1.0 - u ** (1.0 / n)) * scale    # px in from the edge
                if inset < 1.15:
                    tone = (g_lit if gold and s < -0.34
                            else g_mid if gold and s < -0.08
                            else shade)
                else:
                    tone = _stone_tone(s, floor_ramp)
                target.put(x, y, tone)
            elif shadow and u <= 1.30 and s > 0.42:
                target.put(x, y, MORTAR_DEEP)
    target.put(int(round(cx - rx * 0.42)), int(round(cy - ry * 0.48)),
               CLOUDMARBLE["hi"])                          # one catchlight
    if glyph and rx >= 4 and ry >= 3:
        swirl(target, cx + rng.range(-1, 1), cy + rng.range(-1, 1),
              min(rx, ry) - 1.0, glyph_tone or CLOUDGLYPH,
              turns=1.25, phase=rng.float() * 6.28)


# ---------------------------------------------------------------------------
# The Skyway floor
# ---------------------------------------------------------------------------

def _cloudfield(t, salt, density=0.06):
    """The pale blue cloud bed the cobbles are set into: mortar with soft wisps,
    position-locked so every cell of a splat block is identical and the sheet
    tiles seamlessly in both directions."""
    for y in range(t.h):
        for x in range(t.w):
            r = Rng((x * 7349 + y * 12611) ^ salt).float()
            tone = CLOUDMORTAR
            if r < density * 0.34:
                tone = MORTAR_DEEP
            elif r < density:
                tone = MORTAR_HI
            t.put(x, y, tone)
    # a few soft wisps so the bed is cloud, not gravel
    wr = Rng(salt ^ 0xB105)
    for _ in range(5):
        wx, wy = wr.range(0, 31), wr.range(0, 31)
        for i in range(wr.range(5, 10)):
            t.put(wx + i, wy + (i // 4), MORTAR_HI)
            t.put(wx + i, wy + (i // 4) + 1, CLOUDMORTAR)


# Stone sizes for one 32px tile of Skyway paving, as (count, rx range, ry range).
# Read off skyway-floor-reference.png: the bed carries a FEW large slabs, some
# medium stones and a couple of pebbles, with the pale blue cloud showing
# between them as channels. Nine same-sized cobbles per tile instead reads as
# foam at 1x -- uniform bubbles with a gold speck on each -- which is what the
# first two passes did.
# (count, rx range, ry range, gold-rim chance). Gold is deliberately confined to
# the big slabs and rare even there: at 1x a gold rim on a 6px stone is a 3px
# yellow arc, and one per stone over a whole biome reads as speckle rather than
# as the set's accent. Measured against vanilla's ground sheets, an accent motif
# lands about once every one or two tiles (cloudturf's flowers do exactly this).
_SKYWAY_STONES = ((2, (7, 9), (5, 7), 0.36), (2, (4, 6), (3, 5), 0.0))
_FLOOR_GLYPH = mix(CLOUDGLYPH, CLOUDMARBLE["light"], 0.45)


def _skyway_tile(variant):
    """One 32x32 Skyway ground look: cloud bed + slabs, stones and pebbles, the
    larger ones carrying the blue swirl. Wraps in x and y."""
    t = Wrap(32, 32, wrap_y=True)
    salt = 0x5CA9_0000 + variant * 0x2711
    _cloudfield(t, salt)
    rng = Rng(salt ^ 0x51A7)
    stones = []
    for count, (rx0, rx1), (ry0, ry1), gold_p in _SKYWAY_STONES:
        for _ in range(count):
            stones.append((
                rng.range(0, 31), rng.range(0, 31),
                rng.range(rx0, rx1) + rng.float() * 0.7,
                rng.range(ry0, ry1) + rng.float() * 0.7,
                rng.chance(gold_p),
                2.2 + rng.float() * 1.2))            # ellipse .. rounded slab
    stones.sort(key=lambda v: -v[2])                 # big slabs first, under
    for cx, cy, rx, ry, gold, n in stones:
        # Ground contrast, not object contrast: no drop shadow, a far rim only
        # one step below the body, and the body ramp stopped before its darkest
        # step. See cobble()'s note and tools/... vanilla ground comparison.
        cobble(t, cx, cy, rx, ry, rng, gold=gold, glyph=rx >= 4.5, n=n,
               shadow=False, shade=mix(CLOUDMARBLE["deep"], CLOUDMORTAR, 0.35),
               floor_ramp=True,
               # the ground's gold sits one ramp step brighter than the wall's:
               # at this material's luma the darker pair is the single biggest
               # contributor to the 1x noise measurement
               gold_pair=(SKYGOLD["hi"], SKYGOLD["light"]),
               glyph_tone=_FLOOR_GLYPH)
    return t


def _skyway_features(block, x0, y0, salt, k, variant):
    """Sparse punctuation on the four FULL-variant cells only (vanilla's trick:
    blend cells stay a calm base, each full variant carries its own cluster).
    Stars are rare on the ground on purpose — the spec calls them punctuation,
    and a star on every tile would read as a pattern, not as an accent."""
    rng = Rng(salt ^ 0x57A5)
    w = Wrap(32, 32, wrap_y=True)
    if (variant + k) % 5 == 0:
        sx, sy = rng.range(9, 22), rng.range(9, 22)
        gold_star(w, sx, sy, 3, 5)
    if (variant * 3 + k) % 7 == 0:               # a stray gold flake
        w.put(rng.range(3, 28), rng.range(3, 28), SKYGOLD["light"])
        w.put(rng.range(3, 28), rng.range(3, 28), SKYGOLD["base"])
    if k % 2 == 1:                               # one extra loose curl
        swirl(w, rng.range(8, 24), rng.range(8, 24), 3.2, CLOUDGLYPH,
              turns=1.1, phase=rng.float() * 6.28)
    for y in range(32):
        for x in range(32):
            p = w.get(x, y)
            if p[3]:
                block.put(x0 + x, y0 + y, p)


def gen_skyway_splat(path, variants=6, salt=0x5CA9):
    """tiles/skyway_splat.png — the 21-cell auto-tile atlas.

    The cell map and the blend masks come straight from gen_splats (verified in
    docs/research/splat-format.md 5.3 and gated by tools/tile_behaviour_audit.py):
    four fully opaque variants at (3..6,0), 17 marching-square blend pieces, and
    the four DIAGONAL corner cells kept as small nubs. Those nubs are the trap —
    ours once covered 83-89% of their cell, so every placed tile repainted its
    four diagonal neighbours. Reusing gen_splats.CELL_MASKS/CELL_EYES rather
    than restating the geometry is what keeps that from happening twice.

    The loop is local (not gen_splats.build_splat) only because the cobble
    layout differs per VARIANT, and build_splat's material callback cannot see
    which variant it is painting."""
    CELL, BW, BH = gen_splats.CELL, gen_splats.BLOCK_W, gen_splats.BLOCK_H
    sheet = Canvas(BW, BH * variants)
    for v in range(variants):
        vsalt = salt + v * 7919
        tile = _skyway_tile(v)
        block = Canvas(BW, BH)
        for cx in range(7):
            for cy in range(3):
                tile.blit_to(block, cx * CELL, cy * CELL)
        for k, fcx in enumerate((3, 4, 5, 6)):
            _skyway_features(block, fcx * CELL, 0, vsalt + 0xF17 + k * 977, k, v)
        for cx in range(7):
            for cy in range(3):
                if (cx, cy) not in gen_splats.CELL_MASKS:
                    continue
                discs = gen_splats.CELL_MASKS[(cx, cy)]
                eye = gen_splats.CELL_EYES.get((cx, cy))
                for x in range(CELL):
                    for y in range(CELL):
                        a = gen_splats._mask_alpha(discs, eye, x, y,
                                                   vsalt + cx * 31 + cy * 71)
                        if a > 0:
                            r, g, b, _ = block.get(cx * CELL + x, cy * CELL + y)
                            sheet.put(cx * CELL + x, v * BH + cy * CELL + y,
                                      (r, g, b, a))
    sheet.save(path)


def gen_skyway_tile(path):
    """tiles/skyway.png — the plain 32x32 strip. TerrainSplatterTile prefers the
    `_splat` atlas whenever it exists, so this only ever renders on the legacy
    path (preferLegacySplatting), but the format allows both and a tile with no
    plain sibling has no fallback at all."""
    c = Canvas(32, 32)
    _skyway_tile(0).blit_to(c, 0, 0)
    c.save(path)


# ---------------------------------------------------------------------------
# The wall sheet
#
# objects/<wall>.png is 352x128 and has THREE readers, at three different cell
# sizes (docs/TECHNICAL_LEARNINGS.md, "one sheet, three readers"):
#
#   x   0.. 64   wall body, 16px grid, cols 0-3 x rows 0-7
#   x  64.. 96   window insert, 16px cells, rows 0-7
#   x  96..352   eight 32x128 door cells (32-cell indices 3..10)
#
# The body's four columns are NOT four random variants. Read off
# WallObject.addWallDrawOptions, every tile is drawn as two 16px halves, at
# drawX and drawX+16, and the column says WHICH HALF and whether the wall
# continues that way:
#
#   cap  (row 0, at drawY-16)   left: (2,0) if wall to the left else (0,0)
#                               right:(1,0) if wall to the right else (3,0)
#   face (rows 3/4, no wall below)
#                               left: (2,3)/(2,4) continuing, (0,3)/(0,4) W end
#                               right:(1,3)/(1,4) continuing, (3,3)/(3,4) E end
#
# That is what makes the reference's ONE ARCH PER TILE possible: the arch's left
# half lives in cols 0/2 and its right half in cols 1/3, and the pier straddles
# the tile boundary. Treating the columns as interchangeable variants (which is
# how the older sheets in this repo were built) forces the arcade down to a
# 16px period and loses the reference's proportion.
# ---------------------------------------------------------------------------

C = 16          # body cell size
TILE_TOP = 96   # door cells are drawn at drawY-96, so row 96 is the tile's top


# Which 16px HALF of the 32px cap each body cell is. Read off
# WallObject.addWallDrawOptions: a cell drawn at drawX is the tile's LEFT half,
# one drawn at drawX+16 its RIGHT half. Getting this right is what lets the cap
# carry a 32px-period cloud mottle instead of a 16px repeat.
CAP_HALF = {
    (0, 0): 0, (2, 0): 0, (1, 0): 1, (3, 0): 1,
    (0, 1): 0, (1, 1): 0, (2, 1): 1, (3, 1): 1,
    (0, 2): 0, (1, 2): 0, (2, 2): 1, (3, 2): 1,
    (0, 5): 0, (2, 5): 0, (3, 5): 0, (1, 5): 1,
    (0, 6): 0, (1, 6): 1, (2, 6): 1, (3, 6): 1,
    (0, 7): 0, (3, 7): 0, (1, 7): 1, (2, 7): 1,
}


def _cap_field(w, h, salt=0x5CA9C0):
    """The wall seen from directly above: cool cloud stone, and CALM.

    Vanilla's own cap is 93% one flat tone with roughly 6% of a second
    (measured on stonewall.png). Two earlier passes failed here in opposite
    directions: four ramp steps dithered at 20% read as television static, and
    then wide 2:1 soft banks read as diagonal corrugation once the tile
    repeated across a whole wall. What survives both is vanilla's own recipe --
    a flat base, a few percent of speckle, and a SMALL number of readable
    micro-details. Here those details are the set's own blue swirl and a couple
    of pale block hints, so the cap still says 'cloud' rather than 'slate'."""
    t = Wrap(w, h)
    t.rect(0, 0, w, h, CAP["base"])
    rng = Rng(salt)
    # Two details only, both a single ramp step above the base. Anything with
    # its own dark edge turns into a stamped motif once the 32px tile repeats
    # along a whole wall, which is what the corrugation pass looked like.
    for _ in range(2):                                  # pale block hints
        bx, by = rng.range(0, w - 1), rng.range(0, h - 1)
        for y in range(by, by + rng.range(2, 3)):
            for x in range(bx, bx + rng.range(5, 8)):
                t.put(x, y, CAP["light"])
    swirl(t, rng.range(0, w - 1), rng.range(3, h - 4), 2.8,
          CAP["light"], turns=1.15, phase=rng.float() * 6.28)
    for y in range(h):                                  # sparse speckle only
        for x in range(w):
            r = Rng((x * 7349 + y * 12611) ^ salt).float()
            if r < 0.028:
                t.put(x, y, CAP["hi"])
            elif r < 0.060:
                t.put(x, y, CAP["deep"])
    return t


_CAP_FIELD = None


def _cap_field_t():
    global _CAP_FIELD
    if _CAP_FIELD is None:
        _CAP_FIELD = _cap_field(32, 16)
    return _CAP_FIELD


def _cap_texture(target, w, h, salt=0x5CA9C0):
    """Blit the cap field, repeating it, into an arbitrary rectangle."""
    src = _cap_field(32, 16, salt) if (w, h) != (32, 16) or salt != 0x5CA9C0 \
        else _cap_field_t()
    for y in range(h):
        for x in range(w):
            target.put(x, y, src.get(x % 32, y % 16))


def _cap_tile():
    """32x16 cap for the wall's FREE TOP edge (row 0): the thin gold cornice of
    the reference, a four-point star centred on the tile, and small gold studs
    along the moulding. Split into halves by the caller, because the engine
    draws every tile as two 16px cells."""
    t = Wrap(32, 16)
    _cap_texture(t, 32, 16)
    for x in range(32):                                   # the cornice moulding
        t.put(x, 0, SKYGOLD["light"])
        t.put(x, 1, SKYGOLD["base"])
        t.put(x, 2, SKYGOLD["deep"])
    for x in range(2, 32, 8):                             # studs on the moulding
        t.put(x, 1, SKYGOLD["hi"])
        t.put(x + 1, 1, SKYGOLD["hi"])
    gold_star(t, 16, 8, 3, 5)
    for dx in (-6, 6):                                    # flanking specks
        t.put(16 + dx, 7, SKYGOLD["base"])
        t.put(16 + dx, 8, SKYGOLD["light"])
    return t


def _plain_cap_tile():
    """32x16 of interior cap, no cornice: rows 1-2 and the junction pieces."""
    t = Wrap(32, 16)
    _cap_texture(t, 32, 16)
    return t


def _masonry(w, h, salt=0x5CA9_F1, y_phase=0):
    """A wrapping field of cloud cobbles laid in rough courses — the wall's own
    stonework. Courses sit 7px apart with the joints offset course by course,
    each stone gold-rimmed on its lit edge, the larger ones carrying a swirl.
    `y_phase` shifts the course grid so a door's jamb lines up with the courses
    of the wall beside it."""
    t = Wrap(w, h)
    rng = Rng(salt)
    for y in range(h):
        for x in range(w):
            r = Rng((x * 733 + y * 977) ^ salt).float()
            t.put(x, y, MORTAR_DEEP if r < 0.14 else CLOUDMORTAR)
    course = 0
    cy = 5 - y_phase
    while cy < h + 6:
        x = -6 + (course % 3) * 6
        while x < w + 6:
            rx = rng.range(5, 9) + rng.float() * 0.5
            cobble(t, x + rx, cy + rng.range(-1, 1), rx, 3.7, rng,
                   gold=True, glyph=rx >= 6.5 and rng.chance(0.7), n=3.4)
            x += 2 * rx + 0.5
        course += 1
        cy += 7
    return t


def _face_tile():
    """32x32 of wall FRONT FACE, one whole tile wide, position-locked so the two
    16px halves join and so tile N's right edge meets tile N+1's left edge.

    Top: cloud masonry in rough courses. Bottom: the reference's GOLDEN ARCADE.
    v2 makes the arcade's construction explicit and this follows it exactly:
    a RUN of arches (one per tile, so a wall of any length reads as an arcade),
    each springing off a slender pilaster that straddles the tile boundary, a
    four-point star with a pale blue core sitting ON the apex, a gold pendant
    lozenge hanging at each springing point, a pale archivolt inside the gold
    band, and cloud puffs standing in the arch."""
    t = _masonry(32, 32)

    # the lit lip where the cap's front edge overhangs the face
    for x in range(32):
        t.put(x, 0, CLOUDMARBLE["hi"])
        t.put(x, 1, SKYGOLD["deep"])                       # thin string course

    cx, spring, R = 15.5, 29.0, 11.5
    # cloud puffs standing inside the arch, painted before the gold so the
    # mouldings sit in front of them
    for px_, py_, pr in ((10, 29, 6.6), (21, 29, 6.2), (16, 27, 5.8)):
        for y in range(int(py_ - pr) - 1, 30):
            for x in range(int(px_ - pr) - 1, int(px_ + pr) + 2):
                dx, dy = (x - px_) / pr, (y - py_) / (pr * 0.95)
                d2 = dx * dx + dy * dy
                if d2 > 1.0 or math.hypot(x - cx, y - spring) > R - 2.4:
                    continue
                sh = (dx + dy) * 0.5
                t.put(x, y, CLOUDMARBLE["hi"] if sh < -0.45 else
                      CLOUDMARBLE["light"] if sh < 0.1 else CLOUDMARBLE["base"])
    for sx_, sy_ in ((10, 26), (21, 26)):          # the puffs carry the curl too
        swirl(t, sx_, sy_, 3.0, CLOUDGLYPH, turns=1.2, phase=1.1)
    for y in range(16, 31):                                # the arch band
        for x in range(-3, 35):
            if y > spring:
                continue
            d = math.hypot(x - cx, y - spring)
            if R - 2.0 <= d < R:
                lit = (x - cx) + (y - spring) < 0
                t.put(x, y, SKYGOLD["light"] if (d >= R - 1 and lit)
                      else SKYGOLD["base"] if d >= R - 1 else SKYGOLD["deep"])
            elif R - 4.0 <= d < R - 2.0:                   # pale archivolt
                t.put(x, y, CLOUDMARBLE["hi"] if (x - cx) < 0
                      else CLOUDMARBLE["light"])

    # The pilaster straddles the tile boundary: x29,30,31 | 0,1,2 -> a 6px pier
    # across every join, white cloud stone with a gold edge on each side. A pier
    # painted solid gold puts a gold stripe on every 32px of wall; the reference
    # keeps the gold to the edges.
    PIER = ((29, SKYGOLD["light"]), (30, CLOUDMARBLE["hi"]),
            (31, CLOUDMARBLE["light"]), (0, CLOUDMARBLE["base"]),
            (1, CLOUDMARBLE["deep"]), (2, SKYGOLD["deep"]))
    for y in range(17, 30):
        for x, tone in PIER:
            t.put(x, y, tone)
    for y in (17, 18):                                     # capital
        t.put(28, y, SKYGOLD["light"])
        t.put(3, y, SKYGOLD["deep"])
    gold_lozenge(t, 0, 23, 2, 4)                           # pendant on the pier
    gold_star(t, 16, 17, 3, 5)                             # star on the apex

    for x in range(32):                                    # foot
        t.put(x, 30, mix(CLOUDMARBLE["deep"], OUT, 0.30))
        t.put(x, 31, OUT)
    return t


_CAP_TILE = None
_FACE_TILE = None
_JAMB_TILE = None


def _cap_t():
    global _CAP_TILE
    if _CAP_TILE is None:
        _CAP_TILE = _cap_tile()
    return _CAP_TILE


def _face_t():
    global _FACE_TILE
    if _FACE_TILE is None:
        _FACE_TILE = _face_tile()
    return _FACE_TILE


def _jamb_t():
    """48 rows of plain cloud masonry for the door surrounds, phase-locked to
    the wall face's courses (sheet row 96 is the tile's top edge, which is where
    the wall face's own row 0 lands, so the two agree course for course)."""
    global _JAMB_TILE
    if _JAMB_TILE is None:
        _JAMB_TILE = _masonry(32, 48, salt=0x5CA9_D0, y_phase=16)
    return _JAMB_TILE


_PLAIN_CAP = None


def _plain_cap(c, cell_x, cell_y):
    """Interior cap for one body cell, taking the tile half the engine draws
    that cell as (CAP_HALF) so the mottle joins across the 16px seam."""
    global _PLAIN_CAP
    if _PLAIN_CAP is None:
        _PLAIN_CAP = _plain_cap_tile()
    half = CAP_HALF.get((cell_x, cell_y), 0)
    _PLAIN_CAP.blit_to(c, cell_x * C, cell_y * C, sx=half * C, sy=0, w=C, h=C)


def _face_half(c, cell_x, cell_y, half, top_half):
    """Blit one 16px half of the face tile into a body cell. `half` 0 = left,
    1 = right; `top_half` picks face rows 0-15 or 16-31."""
    _face_t().blit_to(c, cell_x * C, cell_y * C,
                      sx=half * C, sy=0 if top_half else C, w=C, h=C)


def _gold_edge(c, x0, y0, h, west):
    """The cornice turning down a free end of the wall. West is the lit side, so
    it takes the bright pair; east is in shade and takes the dark pair."""
    a, b = (SKYGOLD["light"], SKYGOLD["base"]) if west else (SKYGOLD["base"], SKYGOLD["deep"])
    xa = x0 if west else x0 + C - 1
    xb = x0 + 1 if west else x0 + C - 2
    for y in range(y0, y0 + h):
        c.put(xa, y, a)
        c.put(xb, y, b)


def _hrim(c, cell_x, cell_y, y, x0, x1, gold="base"):
    for x in range(x0, x1):
        c.put(cell_x * C + x, cell_y * C + y, SKYGOLD[gold])


def _vrim(c, cell_x, cell_y, x, y0, y1, gold="base"):
    for y in range(y0, y1):
        c.put(cell_x * C + x, cell_y * C + y, SKYGOLD[gold])


def _build_wall(salt=0x5CA9):
    c = Canvas(352, 128)
    cap = _cap_t()

    # ---- row 0: the free top cap, with the gold cornice + star ------------
    # (0,0) left half / west END, (2,0) left half / continuing,
    # (1,0) right half / continuing, (3,0) right half / east END.
    for col, half in ((0, 0), (2, 0), (1, 1), (3, 1)):
        cap.blit_to(c, col * C, 0, sx=half * C, sy=0, w=C, h=C)
    _gold_edge(c, 0, 0, C, west=True)
    _gold_edge(c, 3 * C, 0, C, west=False)

    # ---- rows 1-2: interior cap bands -------------------------------------
    # col 0 carries the west edge, col 3 the east edge, cols 1-2 are interior.
    for row in (1, 2):
        for col in range(4):
            _plain_cap(c, col, row)
        _gold_edge(c, 0, row * C, C, west=True)
        _gold_edge(c, 3 * C, row * C, C, west=False)

    # ---- rows 3-4: the front face, one arch per tile ----------------------
    for col, half in ((0, 0), (2, 0), (1, 1), (3, 1)):
        _face_half(c, col, 3, half, True)
        _face_half(c, col, 4, half, False)
    # the two END columns get a pilaster closing the arcade off
    for col, west in ((0, True), (3, False)):
        for row in (3, 4):
            _gold_edge(c, col * C, row * C, C, west=west)

    # ---- rows 5-7: junction / inner-corner pieces -------------------------
    # Geometry (which cell hooks where) follows gen_walls.py, which was decoded
    # from the same draw code and has shipped; only the paint is new here.
    for row in (5, 6, 7):
        for col in range(4):
            _plain_cap(c, col, row)

    _vrim(c, 0, 5, 0, 0, 16, "light"); _hrim(c, 0, 5, 0, 0, 6, "light")
    _vrim(c, 1, 5, 15, 0, 16); _hrim(c, 1, 5, 0, 10, 16)
    _hrim(c, 2, 5, 0, 11, 16); _vrim(c, 2, 5, 15, 0, 5)
    _hrim(c, 3, 5, 0, 0, 16); _vrim(c, 3, 5, 15, 0, 6)

    _vrim(c, 0, 6, 0, 0, 16, "light"); _hrim(c, 0, 6, 15, 0, 6, "light")
    _vrim(c, 1, 6, 15, 0, 16); _hrim(c, 1, 6, 15, 10, 16)
    _hrim(c, 2, 6, 0, 0, 16)
    _face_t().blit_to(c, 2 * C, 6 * C + 3, sx=0, sy=19, w=C, h=13)
    _hrim(c, 3, 6, 0, 0, 16)
    _vrim(c, 3, 6, 15, 1, 9)
    _face_t().blit_to(c, 3 * C, 6 * C + 3, sx=16, sy=19, w=12, h=13)

    _vrim(c, 0, 7, 0, 0, 6, "light"); _hrim(c, 0, 7, 0, 0, 6, "light")
    _vrim(c, 1, 7, 15, 0, 6); _hrim(c, 1, 7, 0, 10, 16)
    _face_t().blit_to(c, 2 * C + 10, 7 * C + 10, sx=6, sy=26, w=6, h=6)
    _face_t().blit_to(c, 3 * C, 7 * C + 10, sx=20, sy=26, w=6, h=6)

    _build_window(c, salt)
    _build_doors(c, salt)
    return c


# --- window ----------------------------------------------------------------
#
# WallWindowObject.getWindowDir returns 1 for a NORTH-SOUTH wall and 0 for an
# EAST-WEST one, and the two draw completely different pictures out of one 32px
# strip (docs/TECHNICAL_LEARNINGS.md, "which way is the wall facing"):
#
#   dir 1, north-south : rows 0-1 at drawY-16 and drawY. You are looking at the
#          wall's ROOF from directly above. Vanilla stonewall is 512/512 opaque
#          here. There is NO hole: from overhead a window shows you roof, and
#          glass seen from above is looking at the sky, so it reads DARKER than
#          the same glass does edge-on.
#   dir 0, east-west   : rows 2..7 at drawY-64, -48, -32, -16, 0, +16. Now you
#          are looking at the wall's FRONT and the opening is a genuine hole.
#          Rows 2-4 reach two whole tiles above the tile and vanilla leaves them
#          EMPTY; rows 5-6 carry the see-through opening, row 7 the solid sill.
#
# Both halves of this have shipped wrong at least once. tools/sheet_format_audit
# asserts rows 0-1 == 512 opaque, rows 2-4 == 0, and rows 5-6 not solid.

def _build_window(c, salt):
    X = 64

    # --- dir 1: north-south wall, its roof seen from above -----------------
    t = Wrap(32, 32)
    _cap_texture(t, 32, 32)
    for y in range(32):                        # the cap's own east/west rims
        t.put(0, y, SKYGOLD["light"])
        t.put(1, y, SKYGOLD["base"])
        t.put(30, y, SKYGOLD["base"])
        t.put(31, y, SKYGOLD["deep"])
    # The skylight lies FLAT in the roof, so it runs along the wall: tall and
    # narrow in the cell, not wide. Its glass is looking straight up at the sky
    # and is therefore darker than the cap, never a bright pane stuck on top.
    roof_glass = mix(CLOUDGLYPH, palette.OUTLINE, 0.42)
    roof_glass_hi = mix(CLOUDGLYPH, palette.OUTLINE, 0.26)
    for y in range(4, 28):
        for x in range(6, 26):
            t.put(x, y, SKYGOLD["deep"])
    for y in range(6, 26):
        for x in range(8, 24):
            t.put(x, y, roof_glass_hi if (x + y) % 7 == 0 else roof_glass)
    for x in range(6, 26):                     # frame: lit head, shaded foot
        t.put(x, 4, SKYGOLD["light"])
        t.put(x, 5, SKYGOLD["base"])
        t.put(x, 26, SKYGOLD["deep"])
        t.put(x, 27, SKYGOLD["deep"])
    for y in range(4, 28):
        t.put(6, y, SKYGOLD["light"])
        t.put(7, y, SKYGOLD["base"])
        t.put(24, y, SKYGOLD["deep"])
        t.put(25, y, SKYGOLD["deep"])
    for x in range(8, 24):                     # glazing bars
        t.put(x, 15, SKYGOLD["base"])
        t.put(x, 16, SKYGOLD["deep"])
    for y in range(6, 26):
        t.put(15, y, SKYGOLD["base"])
        t.put(16, y, SKYGOLD["deep"])
    gold_star(t, 16, 16, 4, 6)
    t.blit_to(c, X, 0)

    # --- rows 2-4 stay empty ----------------------------------------------
    # Drawn at drawY-64/-48/-32. Filling them is what once made the window
    # 96px tall against a 48px wall.

    # --- dir 0: east-west wall, its front, with the hole in it -------------
    face = _face_t()
    JAMB = 6
    # row 5 (drawY-16) is the cap band: only the jambs stand there, the middle
    # is open sky above the arch, exactly as vanilla stonewall leaves it.
    for x in list(range(0, JAMB)) + list(range(32 - JAMB, 32)):
        for y in range(16):
            p = _cap_pixel(x, y)
            c.put(X + x, 80 + y, p)
    for y in range(80, 96):                    # the strip is 32px, not a 16px cell
        c.put(X + 0, y, SKYGOLD["light"])
        c.put(X + 1, y, SKYGOLD["base"])
        c.put(X + 30, y, SKYGOLD["base"])
        c.put(X + 31, y, SKYGOLD["deep"])
    # the arch head over the opening
    acx, aspring, AR = 15.5, 100.0, 12.0
    for y in range(82, 101):
        for x in range(32):
            if y > aspring:
                continue
            d = math.hypot(x - acx, y - aspring)
            if AR - 2.0 <= d < AR:
                lit = (x - acx) + (y - aspring) < 0
                c.put(X + x, y, SKYGOLD["light"] if (d >= AR - 1 and lit)
                      else SKYGOLD["base"] if d >= AR - 1 else SKYGOLD["deep"])
    # rows 6-7: the jambs carry the wall's own face so the courses line up
    for x in list(range(0, JAMB)) + list(range(32 - JAMB, 32)):
        for y in range(32):
            p = face.get(x, y)
            if p[3]:
                c.put(X + x, 96 + y, p)
    # the opening: a light glaze so the set keeps its blue-cloud identity while
    # still being a hole you can see the ground through
    for y in range(96, 118):
        for x in range(JAMB, 32 - JAMB):
            if math.hypot(x - acx, y - aspring) >= AR - 2.0 or y > aspring:
                c.put(X + x, y, with_alpha(CLOUDGLYPH, 46))
    for y in range(96, 118):                   # central mullion
        c.put(X + 15, y, SKYGOLD["base"])
        c.put(X + 16, y, SKYGOLD["deep"])
    for x in range(JAMB, 32 - JAMB):           # transom
        c.put(X + x, 108, SKYGOLD["base"])
        c.put(X + x, 109, SKYGOLD["deep"])
    gold_star(c, X + 16, 96, 3, 4)
    for y in range(96, 118):                   # reveals: lit left, dark right
        c.put(X + JAMB, y, SKYGOLD["light"])
        c.put(X + 31 - JAMB, y, SKYGOLD["deep"])
    # row 7's lower part: the sill, then the wall's foot. Solid all the way.
    for x in range(32):
        for y in range(118, 128):
            p = face.get(x, y - 96)
            c.put(X + x, y, p if p[3] else CLOUDMARBLE["base"])
    for x in range(JAMB - 2, 34 - JAMB):       # the sill proper
        c.put(X + x, 118, SKYGOLD["light"])
        c.put(X + x, 119, SKYGOLD["base"])
        c.put(X + x, 120, SKYGOLD["deep"])
    for x in range(32):
        c.put(X + x, 126, mix(CLOUDMARBLE["deep"], OUT, 0.30))
        c.put(X + x, 127, OUT)


def _cap_pixel(x, y, salt=0x5CA9C0):
    r = Rng(((x % 16) * 7349 + (y % 16) * 12611) ^ salt).float()
    if r < 0.05:
        return CAP["hi"]
    if r < 0.13:
        return CAP["light"]
    if r < 0.21:
        return CAP["deep"]
    return CAP["base"]


# --- doors -----------------------------------------------------------------
#
# WallDoorObject/WallDoorOpenObject draw EVERY one of the eight 32x128 cells at
# pos(drawX, drawY - 96), so row 96 is the tile's top edge and everything above
# it sticks out over the wall — which only rises 16px above its own tile. The
# extents below were measured off vanilla stonewall.png and are asserted by
# tools/sheet_format_audit.py; a cell painted from row 0 renders a door three
# tiles tall. Do not move a top row.
#
#   cell  3: y88..127  rot 0 closed, head-on          cell  7: y88..127 rot 2
#   cell  4: y68..127  rot 0 open, leaf edge-on       cell  8: y68..127 rot 2
#   cell  5: y70..127  rot 1 closed, edge-on          cell  9: y70..127 rot 3
#   cell  6: y68..127  rot 1 open, head-on (north)    cell 10: y90..127 rot 3 (south)

def _door_masonry(x, y):
    """One pixel of cloudmarble jamb. Sheet row 80 is the jamb tile's row 0, so
    row 96 -- the tile's top edge -- lands on jamb row 16, which is where the
    wall face's own courses sit. A door's stonework therefore lines up with the
    wall it is set into instead of drifting half a course."""
    p = _jamb_t().get(x % 32, y - 80)
    return p if p[3] else CLOUDMARBLE["base"]


def _leaf_px(x, y, vertical):
    """The door leaf: cloudmarble panelling, joints running along the leaf's long
    axis, so a head-on leaf gets vertical seams and an edge-on one horizontal.
    White with a seam every 8px -- the gold on a leaf is its FRAME and its star,
    painted over this; a gold grain in the fill turns the door into a portcullis
    (which is exactly what the first pass looked like)."""
    n = x if vertical else y
    m = n % 8
    if m == 0:
        return STONE_SHADE                     # panel seam
    if m == 1:
        return CLOUDMARBLE["hi"]
    if m == 7:
        return CLOUDMARBLE["deep"]
    return CLOUDMARBLE["light"] if m in (2, 3) else CLOUDMARBLE["base"]


def _door_leaf_panel(target, lx0, lx1, ly0, ly1):
    """The v2 reference's door leaf, top to bottom: a gold frame around a CLOUD
    FIELD, one large four-point star with a pale blue core sitting on the
    frame's lower rail, and a CLOUD-BRICK lower half carrying its own small gold
    arch. lx1/ly1 are inclusive."""
    w = lx1 - lx0 + 1
    mid = ly0 + int((ly1 - ly0) * 0.62)                   # the frame's lower rail
    for y in range(ly0, ly1 + 1):                         # cloud field / bricks
        for x in range(lx0, lx1 + 1):
            if y <= mid:
                r = Rng((x * 733 + y * 977) ^ 0x5CA9_1E).float()
                tone = (CLOUDMARBLE["hi"] if r < 0.12 else
                        CLOUDMARBLE["light"] if r < 0.55 else CLOUDMARBLE["base"])
            else:                                         # coursed cloud brick
                course = (y - mid) // 4
                shift = 4 if course % 2 else 0
                tone = (STONE_SHADE if (y - mid) % 4 == 0 or
                        (x - lx0 + shift) % 8 == 0 else
                        CLOUDMARBLE["light"] if (y - mid) % 4 == 1
                        else CLOUDMARBLE["base"])
            target.put(x, y, tone)
    swirl(target, lx0 + w // 2 - 2, ly0 + 5, 3.4, CLOUDGLYPH, turns=1.2)
    for x in range(lx0, lx1 + 1):                         # gold frame, top+bottom
        target.put(x, ly0, SKYGOLD["light"])
        target.put(x, ly0 + 1, SKYGOLD["base"])
        target.put(x, mid, SKYGOLD["base"])
        target.put(x, mid + 1, SKYGOLD["deep"])
        target.put(x, ly1, SKYGOLD["deep"])
    for y in range(ly0, ly1 + 1):                         # gold frame, sides
        target.put(lx0, y, SKYGOLD["light"])
        target.put(lx0 + 1, y, SKYGOLD["base"])
        target.put(lx1 - 1, y, SKYGOLD["base"])
        target.put(lx1, y, SKYGOLD["deep"])
    # the small arch in the brick half
    acx, aspring, ar = lx0 + w / 2.0 - 0.5, ly1 - 1.0, (w - 5) / 2.0
    for y in range(mid + 2, ly1 + 1):
        for x in range(lx0 + 1, lx1):
            d = math.hypot(x - acx, y - aspring)
            if ar - 2.0 <= d < ar and y <= aspring:
                target.put(x, y, SKYGOLD["light"] if (x - acx) < 0
                           else SKYGOLD["base"])
    gold_star(target, int(acx), mid, max(3, w // 3), max(5, w // 2 - 1))


def _leaf_frame(target, fx0, fx1, fy0, fy1):
    """The gold frame the v2 leaf carries. A swung-open leaf drawn as bare
    boarding does not match the closed one it turns into, and the pair reads as
    two different objects when a player opens the door."""
    for x in range(fx0, fx1 + 1):
        target.put(x, fy0, SKYGOLD["light"])
        target.put(x, fy1, SKYGOLD["deep"])
    for y in range(fy0, fy1 + 1):
        target.put(fx0, y, SKYGOLD["light"])
        target.put(fx1, y, SKYGOLD["deep"])


def _build_doors(c, salt):
    def fill(bands, tone):
        for y0, y1, bx0, bx1 in bands:
            for y in range(y0, y1 + 1):
                for x in range(bx0, bx1 + 1):
                    c.put(x, y, tone(x, y))

    def rim_arch(bands, tone=None):
        tone = SKYGOLD["light"] if tone is None else tone
        for y0, _, bx0, bx1 in bands:
            for x in range(bx0, bx1 + 1):
                c.put(x, y0, tone)

    def floor_line(y, x0, x1):
        for x in range(x0, x1 + 1):
            c.put(x, y, OUT)

    def gold_band(y, x0, x1):
        for x in range(x0, x1 + 1):
            c.put(x, y, SKYGOLD["base"])
            c.put(x, y + 1, SKYGOLD["deep"])

    for i in range(8):
        x0 = 96 + i * 32
        salt_i = salt + 70 + i
        rot = i // 2
        is_open = i % 2 == 1
        head_on = (rot in (0, 2)) != is_open

        if not is_open and head_on:
            # Closed, head-on: the doorway. A chamfered cloudmarble arch around
            # a panelled leaf with the set's four-point star at its centre.
            # 40px tall, i.e. 8px above its tile — deliberately shorter than the
            # wall beside it, exactly as vanilla builds a door.
            arch = [(88, 89, x0 + 5, x0 + 26), (90, 91, x0 + 3, x0 + 28),
                    (92, 93, x0 + 1, x0 + 30), (94, 127, x0, x0 + 31)]
            fill([(94, 127, x0, x0 + 31)], lambda x, y: _door_masonry(x, y))
            # The three chamfer courses above the tile are ONE arch head, so
            # they get one continuous stone and one gold line following the
            # outer step -- masonry joints and a gold rim per course instead
            # read as three slabs stacked over the door, a staircase not an arch.
            for (ay0, ay1, ax0, ax1) in arch[:3]:
                for y in range(ay0, ay1 + 1):
                    for x in range(ax0, ax1 + 1):
                        s_ = (x - (x0 + 16)) / 16.0 + (y - 96) / 16.0
                        c.put(x, y, CLOUDMARBLE["hi"] if s_ < -0.55 else
                              CLOUDMARBLE["light"] if s_ < -0.1
                              else CLOUDMARBLE["base"])
            # One gold line around the SILHOUETTE of the whole head. Outlining
            # each chamfer course separately draws three nested rectangles,
            # which is a staircase over the door however the stone is shaded;
            # only the union's exposed edge reads as an arch.
            head = set()
            for (ay0, ay1, ax0, ax1) in arch[:3]:
                for y in range(ay0, ay1 + 1):
                    for x in range(ax0, ax1 + 1):
                        head.add((x, y))
            solid = head | {(x, y) for y in range(94, 128)
                            for x in range(x0, x0 + 32)}
            for (x, y) in sorted(head):
                if any((x + dx, y + dy) not in solid
                       for dx, dy in ((0, -1), (-1, 0), (1, 0))):
                    c.put(x, y, SKYGOLD["light"] if x < x0 + 16
                          else SKYGOLD["base"])
            gold_star(c, x0 + 16, 93, 3, 4)                # keystone
            _door_leaf_panel(c, x0 + 8, x0 + 23, TILE_TOP + 1, 126)
            c.put(x0 + 21, 110, SKYGOLD["hi"])            # handle
            c.put(x0 + 21, 111, SKYGOLD["light"])
            for y in range(TILE_TOP + 1, 127):            # reveal beside the leaf
                c.put(x0 + 7, y, SKYGOLD["deep"])
                c.put(x0 + 24, y, SKYGOLD["deep"])
            floor_line(127, x0, x0 + 31)

        elif not is_open:
            # Closed, edge-on: only the wall's narrow side is visible, the leaf
            # lying inside the tile footprint. 58px tall.
            mirror = rot == 3
            strip = [(70, 71, 20, 25), (72, 95, 18, 27)]
            foot = (96, 127, 14, 31)
            if mirror:
                strip = [(y0, y1, 31 - b, 31 - a) for y0, y1, a, b in strip]
                foot = (96, 127, 0, 17)
            strip = [(y0, y1, x0 + a, x0 + b) for y0, y1, a, b in strip]
            fill(strip, lambda x, y: _door_masonry(x, y))
            rim_arch(strip)
            fy0, fy1, fa, fb = foot
            fill([(fy0, fy1 - 1, x0 + fa, x0 + fb)], lambda x, y: _cap_pixel(x, y))
            for x in range(x0 + fa, x0 + fb + 1):
                c.put(x, fy0, SKYGOLD["base"])
            leaf_x = x0 + (fa + 4 if mirror else fb - 6)
            for y in range(fy0 + 4, fy1):
                for x in range(leaf_x, leaf_x + 3):
                    c.put(x, y, _leaf_px(x, y, True))
            c.put(leaf_x + 1, 112, SKYGOLD["hi"])
            floor_line(fy1, x0 + fa, x0 + fb)

        elif not head_on:
            # Open, edge-on: the leaf has swung a quarter turn and stands
            # against the jamb as a narrow slab, threshold below it.
            leaf = [(68, 69, x0 + 24, x0 + 29), (70, 115, x0 + 22, x0 + 31)]
            fill(leaf, lambda x, y: _leaf_px(x, y, False))
            rim_arch(leaf, CLOUDMARBLE["hi"])
            gold_band(78, x0 + 22, x0 + 31)
            gold_band(100, x0 + 22, x0 + 31)
            _leaf_frame(c, x0 + 23, x0 + 30, 82, 98)
            gold_star(c, x0 + 27, 89, 3, 5)
            for y in range(70, 116):                      # free edge of the leaf
                c.put(x0 + 22, y, SKYGOLD["deep"])
            fill([(116, 126, x0, x0 + 31)], lambda x, y: _cap_pixel(x, y))
            floor_line(127, x0, x0 + 31)

        else:
            # Open, head-on: the leaf swung across the tile. rot 1 swings north
            # (leaf drawn high), rot 3 swings south (leaf low).
            top = 68 if rot == 1 else 90
            arch = [(top, top + 1, x0 + 5, x0 + 26),
                    (top + 2, top + 3, x0 + 3, x0 + 28),
                    (top + 4, top + 5, x0 + 1, x0 + 30)]
            body_top = top + 6
            fill(arch, lambda x, y: _leaf_px(x, y, True))
            rim_arch(arch, CLOUDMARBLE["hi"])
            if rot == 1:
                fill([(body_top, 103, x0, x0 + 31)], lambda x, y: _leaf_px(x, y, True))
                gold_band(80, x0, x0 + 31)
                gold_band(95, x0, x0 + 31)
                _leaf_frame(c, x0 + 3, x0 + 28, body_top + 2, 101)
                gold_star(c, x0 + 16, 88, 4, 6)
                floor_line(103, x0, x0 + 31)
                fill([(104, 126, x0 + 16, x0 + 31)], lambda x, y: _cap_pixel(x, y))
                floor_line(127, x0 + 16, x0 + 31)
            else:
                fill([(body_top, 126, x0, x0 + 31)], lambda x, y: _leaf_px(x, y, True))
                gold_band(102, x0, x0 + 31)
                gold_band(118, x0, x0 + 31)
                _leaf_frame(c, x0 + 3, x0 + 28, body_top + 2, 124)
                gold_star(c, x0 + 16, 110, 4, 6)
                fill([(120, 126, x0, x0 + 15)], lambda x, y: _cap_pixel(x, y))
                floor_line(127, x0, x0 + 31)
            # hinge pier on the side the leaf swings from: without it a
            # full-width panel reads as a chest lid rather than a door
            hinge_top = body_top if rot == 1 else top
            hinge_bot = 103 if rot == 1 else 119
            for y in range(hinge_top, hinge_bot + 1):
                c.put(x0, y, SKYGOLD["deep"])
                c.put(x0 + 1, y, SKYGOLD["base"])
                c.put(x0 + 2, y, SKYGOLD["light"] if y % 8 == 2 else SKYGOLD["deep"])

        # A CLOSED door's outer columns are where the leaf meets the wall, so
        # they carry the wall's own cornice colour. An open leaf stands clear of
        # the wall and keeps its own edges.
        if not is_open:
            for y in range(128):
                for x in (x0, x0 + 31):
                    if c.filled(x, y) and y < 127:
                        c.put(x, y, SKYGOLD["base"] if x == x0 + 31
                              else SKYGOLD["light"])


def gen_cloudmarble_wall(path):
    _build_wall().save(path)


# ---------------------------------------------------------------------------
# Fence and gate
#
# FenceObject.addDrawables reads objects/<name>.png as FIVE 32-wide columns of
# the full sheet height, all bottom-anchored, so with the vanilla height of 64
# sheet row 32 is the tile's top edge and row 63 its bottom
# (docs/TECHNICAL_LEARNINGS.md, "the fence sheet's five columns"):
#
#   col 0  post          always
#   col 1  north joint   the tile ABOVE attaches — drawn BEFORE col 0 and
#                        inside the post's own footprint
#   col 2  south rail    the tile BELOW attaches; drawn a SECOND time at
#                        drawY-24 to bridge into a wall standing above
#   col 3  west run      must reach x=0
#   col 4  east run      must reach x=31 — cols 3/4 are NOT mirrors in the world
#
# Geometry targets are vanilla objects/stonefence.png (the masonry analogue):
#   col 0 x8..23 y22..53 (576 opaque)   col 1 x10..21 y28..35 (128)
#   col 2 x10..21 y36..63 (456)         col 3 x0..9 y28..51 (228)
#   col 4 x22..31 y28..51 (228)
#
# Two things carry the top-down-with-forward-lean perspective and neither is
# optional: a horizontal rail is 2 rows of outline, 2 rows of LIT TOP SURFACE,
# 2 rows of DARK FRONT FACE, 2 rows of outline; and every piece stands on a
# baked soft-alpha ground skirt (alpha 74 then 29), never an opaque dark band.
# ---------------------------------------------------------------------------

_CM_KEYS = {"d": CLOUDMARBLE["deep"], "b": CLOUDMARBLE["base"],
            "l": CLOUDMARBLE["light"], "H": CLOUDMARBLE["hi"],
            "G": SKYGOLD["light"], "g": SKYGOLD["base"], "k": SKYGOLD["deep"],
            "#": OUT}


def _blk(c, x, y, w, h, color):
    c.rect(x, y, w, h, color)


def _row(c, x0, y, keys):
    """One 2px-tall band: a 2px outline column, len(keys) 2px ramp blocks, then
    a 2px outline column -- so the band is 4 + 2*len(keys) px wide. Painted in
    2x2 blocks like vanilla's own fence sheets: no odd rows, no stray pixels."""
    _blk(c, x0, y, 2, 2, OUT)
    for i, k in enumerate(keys):
        _blk(c, x0 + 2 + i * 2, y, 2, 2, _CM_KEYS[k])
    _blk(c, x0 + 2 + len(keys) * 2, y, 2, 2, OUT)


def _skirt(c, x, y, w, h, alpha):
    """The baked soft-alpha ground shadow vanilla's fences and rocks stand on --
    alpha 74 then 29, never an opaque dark band."""
    c.rect(x, y, w, h, with_alpha(OUT, alpha))


# The v2 references settle what a railing post is: a FLUTED white column with a
# gold cap band, a gold lozenge at mid-height and a gold band at the foot. The
# flutes are what stop a 12px shaft reading as a blank pill at 1x.
_FLUTE = "Hdlbld"      # hi / DEEP groove / light / base / light / deep


# The post's cylinder cross-section, lit from the top-left: bright on the left,
# falling to deep on the right. Gold collars interrupt it, which is the
# reference's pillar.
# The fluted shaft, y26..y49 -- the capital and the head sit above it.
_POST_BANDS = [_FLUTE] * 12 + ["GGgggk"]   # y26..y48 flutes, y50 gold foot band


def _post_cell(c, x0=8, top=18):
    """A cloudmarble pillar: domed head, an overhanging gold CAPITAL, a fluted
    shaft with the reference's gold lozenge at mid-height, a gold foot band and
    the soft ground skirt.

    The capital is not decoration. FenceObject draws the runs (cols 3/4) AFTER
    the post, and a run is a solid 10x24 block that covers the post's own
    outline columns -- so a post whose head stops level with the rail merges
    into it and a fence line reads as one long kerb with no posts in it. The
    capital has to sit clear ABOVE the rail's top row (y28) to read."""
    _blk(c, x0 + 4, top, 8, 2, OUT)                       # domed head
    _blk(c, x0 + 2, top + 2, 2, 2, OUT)
    _blk(c, x0 + 12, top + 2, 2, 2, OUT)
    _blk(c, x0 + 4, top + 2, 8, 2, CLOUDMARBLE["hi"])
    # capital: 20px wide, overhanging the 16px shaft on both sides
    _blk(c, x0 - 2, top + 4, 2, 2, OUT)
    _blk(c, x0 + 16, top + 4, 2, 2, OUT)
    _row(c, x0, top + 4, "GGgggk")
    _blk(c, x0 - 2, top + 6, 2, 2, OUT)
    _blk(c, x0 + 16, top + 6, 2, 2, OUT)
    _row(c, x0, top + 6, "kkgggk")
    for i, band in enumerate(_POST_BANDS):
        _row(c, x0, top + 8 + i * 2, band)
    gold_lozenge(c, x0 + 8, 40, 4, 6)
    foot = top + 8 + len(_POST_BANDS) * 2                 # y52
    _blk(c, x0 + 2, foot, 12, 2, OUT)
    _skirt(c, x0, foot, 2, 2, 74)
    _skirt(c, x0 + 14, foot, 2, 2, 74)
    _skirt(c, x0 + 2, foot + 2, 12, 2, 74)
    _skirt(c, x0, foot + 2, 2, 2, 29)
    _skirt(c, x0 + 14, foot + 2, 2, 2, 29)
    _skirt(c, x0 + 2, foot + 4, 12, 2, 29)
    _skirt(c, x0 - 2, top + 28, 2, 10, 29)
    _skirt(c, x0 + 16, top + 28, 2, 10, 29)


# The v2 reference's low run is not an open balustrade: it is a chunky rounded
# CLOUD RAIL sitting on a plinth, with a thin gold strap where two blocks meet.
# Read top to bottom, it is still the vanilla fence cross-section -- outline,
# LIT TOP SURFACE, DARK FRONT FACE, outline -- which is the whole perspective.
_RAIL = [
    (28, 2, OUT),
    (30, 2, CLOUDMARBLE["hi"]),        # lit top surface
    (32, 2, CLOUDMARBLE["light"]),
    (34, 2, CLOUDMARBLE["base"]),
    (36, 2, CLOUDMARBLE["base"]),      # front face
    (38, 2, CLOUDMARBLE["deep"]),
    (40, 2, CLOUDMARBLE["deep"]),
    (42, 2, STONE_SHADE),
    (44, 2, OUT),                      # the shadow gap under the rail
    (46, 2, CLOUDMARBLE["light"]),     # plinth, lit top
    (48, 2, CLOUDMARBLE["deep"]),
    (50, 2, OUT),
]


def _balustrade(c, x0, width, strap=None):
    """One side of the run between two posts. `strap` is the gold band's centre
    in SHEET x: the run spans x22..41 -- it straddles a tile boundary -- so the
    strap is drawn as two halves. The engine only ever draws col 4 of the left
    tile together with col 3 of the right one, so the halves always meet."""
    for y, h, tone in _RAIL:
        c.rect(x0, y, width, h, tone)
    for x in range(x0, x0 + width):                        # cloud-block joints
        if (x % 16) in (7, 8):
            for y in range(30, 44):
                c.put(x, y, STONE_SHADE if x % 16 == 7 else CLOUDMARBLE["hi"])
    if strap is not None:
        for dx, tone in ((-2, SKYGOLD["light"]), (-1, SKYGOLD["base"]),
                         (0, SKYGOLD["base"]), (1, SKYGOLD["deep"])):
            for y in range(28, 44):
                c.put(strap + dx, y, tone)
    _skirt(c, x0, 52, width, 2, 74)
    _skirt(c, x0, 54, width, 2, 29)


def gen_cloudmarble_fence(path):
    sheet = Canvas(160, 64)

    # --- col 0: the post ---------------------------------------------------
    c = Canvas(32, 64)
    _post_cell(c)
    sheet.paste(c, 0, 0)

    # --- col 1: north joint ------------------------------------------------
    # Drawn BEFORE col 0 and inside its footprint, so it must stay narrower than
    # the post: anything wide here shows past the post and a fence connecting
    # north sprouts a rail across the tile.
    c = Canvas(32, 64)
    for i, band in enumerate(("lHlbdd", "GGgggk", "lHlbdd", "lHlbdd")):
        _row(c, 8, 28 + i * 2, band)
    _skirt(c, 6, 28, 2, 8, 29)
    _skirt(c, 24, 28, 2, 8, 29)
    sheet.paste(c, 32, 0)

    # --- col 2: south rail -------------------------------------------------
    # Runs to the tile's BOTTOM edge so stacked tiles join seamlessly, and still
    # reads when the engine draws the same cell again 24px higher to bridge into
    # a wall. A hairline here turns every north-south run into a thread.
    c = Canvas(32, 64)
    _skirt(c, 14, 34, 4, 2, 29)
    c.rect(12, 34, 8, 2, OUT)
    for i in range(14):
        y = 36 + i * 2
        keys = "GGgk" if i % 4 == 1 else "lHbd"
        c.rect(10, y, 2, 2, OUT)
        for j, k in enumerate(keys):
            _blk(c, 12 + j * 2, y, 2, 2, _CM_KEYS[k])
        c.rect(20, y, 2, 2, OUT)
        _skirt(c, 8, y, 2, 2, 29)
        _skirt(c, 22, y, 2, 2, 29)
    sheet.paste(c, 64, 0)

    # --- col 3: west run, reaching x=0 -------------------------------------
    # The run between two posts spans x22..41 across a tile boundary, so the
    # star's centre is x=32: its right half lands at x0..3 of this cell.
    c = Canvas(32, 64)
    _balustrade(c, 0, 10, strap=1)
    sheet.paste(c, 96, 0)

    # --- col 4: east run, reaching x=31 ------------------------------------
    c = Canvas(32, 64)
    _balustrade(c, 22, 10, strap=33)
    _skirt(c, 20, 52, 2, 2, 74)
    sheet.paste(c, 128, 0)

    sheet.save(path)


def _gate_post(c, x0, keys, top=20):
    """A gate pier: the same fluted-column-with-a-gold-capital as a fence post,
    at gate width, and standing at the SAME head height. `keys` sets the width
    (4 + 2*len px): the two piers of a horizontal gate are 8px so the pair plus
    the leaf fills the 32px cell exactly, while the standalone vertical post is
    12px, matching vanilla ironfencegate's own column 2 (bbox x10..21).

    A gate whose head stops 6px below the posts on either side reads as a dip
    in the fence line rather than as a gate in it."""
    w = 4 + 2 * len(keys[0])
    _blk(c, x0 + 2, top, w - 4, 2, OUT)
    _row(c, x0, top + 2, "G" * len(keys[0]))               # gold capital
    for i, band in enumerate(keys):
        _row(c, x0, top + 4 + i * 2, band)
    foot = top + 4 + len(keys) * 2
    _blk(c, x0 + 2, foot, w - 4, 2, OUT)
    _skirt(c, x0, foot, 2, 2, 74)
    _skirt(c, x0 + w - 2, foot, 2, 2, 74)
    _skirt(c, x0 + 2, foot + 2, w - 4, 2, 74)
    _skirt(c, x0, foot + 2, w, 2, 29)


_PIER_2 = ["gk", "Hb", "Hb", "Hb", "Hb", "Hb", "Hb", "Hb", "Hb",
           "Hb", "Hb", "Hb", "Gg", "kk"]
_PIER_4 = ["ggkk"] + ["Hdbd"] * 11 + ["GGgk", "kkgk"]


def _gate_leaf(c, x0, x1, crown_x0, crown_x1):
    """The gate leaf head-on. The v2 reference makes the gate the one piece of
    this set that is gold-DOMINANT: a gold-filled panel with tracery and a large
    four-point star, framed by a crown above and a kick rail below. That is what
    distinguishes a gate from the plain cloud rail beside it at a glance, which
    is the whole job of a gate sprite in a fence run. Its crown sits level with
    the piers' capitals, not below them."""
    w = x1 - x0
    c.rect(crown_x0, 20, crown_x1 - crown_x0, 2, OUT)      # crown
    c.rect(crown_x0, 22, crown_x1 - crown_x0, 2, SKYGOLD["hi"])
    c.rect(x0, 24, w, 2, OUT)
    c.rect(x0, 26, w, 2, SKYGOLD["light"])                 # LIT TOP SURFACE
    c.rect(x0, 28, w, 2, SKYGOLD["deep"])                  # DARK FRONT FACE
    c.rect(x0, 30, w, 2, OUT)
    for x in range(x0, x1):                                # the gold field
        for y in range(32, 46):
            c.put(x, y, SKYGOLD["deep"] if (x + y) % 2 else
                  mix(SKYGOLD["deep"], SKYGOLD["base"], 0.4))
        if (x - x0) % 4 == 1:                              # tracery
            for y in range(33, 45):
                c.put(x, y, SKYGOLD["base"])
    for y in (33, 44):
        c.rect(x0 + 1, y, w - 2, 1, SKYGOLD["light"])
    gold_star(c, (x0 + x1) // 2, 39, 4, 6)
    for dx in (-1, 1):                                     # sparkles beside it
        c.put((x0 + x1) // 2 + dx * (w // 2 - 2), 36, SKYGOLD["hi"])
        c.put((x0 + x1) // 2 + dx * (w // 2 - 2), 42, SKYGOLD["light"])
    c.rect(x0, 46, w, 2, SKYGOLD["light"])                 # kick rail, lit top
    c.rect(x0, 48, w, 2, SKYGOLD["deep"])
    c.rect(x0, 50, w, 2, OUT)
    _skirt(c, x0, 52, w, 2, 74)
    _skirt(c, x0, 54, w, 2, 29)


def gen_cloudmarble_fencegate(path):
    """192x64. The six columns are the engine's, not the artist's:
    0 open horizontal, 1 closed horizontal, 2 the vertical gate post drawn
    TWICE (drawY-14 and drawY+14), 3 the latch (drawY+14, rotation 3 only),
    4 the closed vertical leaf (drawY-14), 5 the open vertical leaf
    (drawX-16, drawY+14). Column 2 being drawn twice punishes a wrong guess
    hardest -- an "open horizontal gate" there renders as two stacked gates."""
    sheet = Canvas(192, 64)

    # --- col 1: closed, horizontal ----------------------------------------
    closed = Canvas(32, 64)
    _gate_post(closed, 0, _PIER_2)
    _gate_post(closed, 24, _PIER_2)
    _gate_leaf(closed, 8, 24, 12, 20)
    sheet.paste(closed, 32, 0)

    # --- col 0: open, horizontal ------------------------------------------
    # The leaf has swung out of the opening and stands edge-on beside the hinge
    # pier; vanilla's open cell reaches higher than its closed one, which is
    # what sells the swing.
    c = Canvas(32, 64)
    _gate_post(c, 0, _PIER_2)
    _gate_post(c, 24, _PIER_2)
    c.rect(8, 12, 2, 2, OUT)
    for i, k in enumerate("GgHlHlbdbldbGgk"):
        y = 14 + i * 2
        c.rect(6, y, 2, 2, OUT)
        _blk(c, 8, y, 2, 2, _CM_KEYS[k])
        c.rect(10, y, 2, 2, OUT)
    _skirt(c, 12, 34, 2, 16, 29)
    sheet.paste(c, 0, 0)

    # --- col 2: the vertical gate post, drawn at drawY-14 AND drawY+14 -----
    # Vanilla stonefencegate's own column 2 is x8..27, y26..61 and 576 opaque:
    # it is a FULL-HEIGHT pier, because the engine stacks the same cell 28px
    # apart and the two copies have to read as one continuous post. A short
    # 30px post here renders as two beads on a string.
    c = Canvas(32, 64)
    _blk(c, 12, 24, 8, 2, OUT)
    for i, band in enumerate(("lHllbd", "GGgggk", "kkgggk") +
                             ("lHllbd",) * 8 + ("GGgggk", "lHllbd", "lHllbd",
                                                "lHllbd", "GGgggk")):
        _row(c, 8, 26 + i * 2, band)
    gold_lozenge(c, 16, 42, 4, 6)
    _blk(c, 10, 58, 12, 2, OUT)
    _skirt(c, 8, 58, 2, 2, 74)
    _skirt(c, 22, 58, 2, 2, 74)
    _skirt(c, 10, 60, 12, 2, 74)
    _skirt(c, 6, 30, 2, 28, 29)
    _skirt(c, 24, 30, 2, 28, 29)
    sheet.paste(c, 64, 0)

    # --- col 3: latch, rotation 3 only, drawn at drawY+14 ------------------
    # Vanilla stonefencegate's is x8..23, y28..47 (296 opaque) -- a stub of pier
    # with the catch on it, not a wire.
    c = Canvas(32, 64)
    _blk(c, 12, 28, 8, 2, OUT)
    for i, band in enumerate(("GGgggk", "lHllbd", "lHllbd", "lHllbd",
                              "GGgggk", "lHllbd", "lHllbd", "kkgggk")):
        _row(c, 8, 30 + i * 2, band)
    _blk(c, 12, 46, 8, 2, OUT)
    _skirt(c, 6, 32, 2, 14, 29)
    _skirt(c, 24, 32, 2, 14, 29)
    _skirt(c, 10, 46, 12, 2, 74)
    sheet.paste(c, 96, 0)

    # --- col 4: the closed vertical leaf, drawn at drawY-14 ----------------
    c = Canvas(32, 64)
    _skirt(c, 14, 26, 4, 2, 29)
    c.rect(12, 28, 8, 2, OUT)
    for i, k in enumerate("GGlHbdlbdgkHbdd"):
        y = 30 + i * 2
        c.rect(10, y, 2, 2, OUT)
        _blk(c, 12, y, 2, 2, _CM_KEYS[k])
        _blk(c, 14, y, 2, 2, _CM_KEYS["k" if k in "Gg" else "b"])
        _blk(c, 16, y, 2, 2, _CM_KEYS["k" if k in "Gg" else "d"])
        c.rect(18, y, 2, 2, OUT)
        _skirt(c, 8, y, 2, 2, 29)
        _skirt(c, 20, y, 2, 2, 29)
    sheet.paste(c, 128, 0)

    # --- col 5: the open vertical leaf, at (drawX-16, drawY+14) ------------
    c = Canvas(32, 64)
    _gate_leaf(c, 6, 28, 12, 22)
    _skirt(c, 4, 51, 2, 4, 29)
    sheet.paste(c, 160, 0)

    sheet.save(path)


# ---------------------------------------------------------------------------
# Item icons. Vanilla's are chunky 20x28 blocks (items/stonewall.png is 560
# opaque px in x6..25, y2..29); a crop of an object sheet reads as a scrap.
# ---------------------------------------------------------------------------

def _icon_frame(c, x0, y0, w, h):
    """The dark keyline vanilla puts around a placeable's icon."""
    for x in range(x0, x0 + w):
        c.put(x, y0, OUT)
        c.put(x, y0 + h - 1, OUT)
    for y in range(y0, y0 + h):
        c.put(x0, y, OUT)
        c.put(x0 + w - 1, y, OUT)


def gen_wall_icon(path):
    """items/cloudmarblewall.png — a block of wall: the gold-corniced cap over
    the arcaded face, which is what the player is actually placing."""
    c = Canvas(32, 32)
    cap = _cap_t()
    face = _face_t()
    for y in range(8):                                     # cap + gold cornice
        for x in range(20):
            c.put(6 + x, 2 + y, cap.get(6 + x, y))
    for y in range(20):                                    # masonry + the arch
        for x in range(20):
            p = face.get(6 + x, 6 + y)
            if p[3]:
                c.put(6 + x, 10 + y, p)
    _icon_frame(c, 6, 2, 20, 28)
    c.save(path)


def gen_door_icon(path):
    """items/cloudmarbledoor.png -- the leaf itself, drawn by the same routine
    that paints it on the sheet, so the icon and the placed object are the same
    object."""
    c = Canvas(32, 32)
    for y in range(6, 30):
        for x in range(6, 26):
            c.put(x, y, CLOUDMARBLE["base"])
    _door_leaf_panel(c, 6, 25, 6, 29)
    c.put(22, 17, SKYGOLD["hi"])                           # handle
    c.put(22, 18, SKYGOLD["light"])
    _icon_frame(c, 6, 6, 20, 24)
    c.save(path)


def gen_window_icon(path):
    """items/cloudmarblewindow.png — the arched opening seen from the front,
    the view a player recognises the object by."""
    c = Canvas(32, 32)
    cap = _cap_t()
    face = _face_t()
    for y in range(7):
        for x in range(20):
            c.put(6 + x, 2 + y, cap.get(6 + x, y))
    for y in range(21):
        for x in range(20):
            p = face.get(6 + x, 11 + y)
            if p[3]:
                c.put(6 + x, 9 + y, p)
    acx, aspring, AR = 16.0, 27.0, 8.0
    for y in range(12, 28):
        for x in range(8, 25):
            d = math.hypot(x - acx, y - aspring)
            if d < AR - 2.0:
                c.put(x, y, mix(CLOUDGLYPH, CAP["deep"], 0.35))
            elif AR - 2.0 <= d < AR:
                lit = (x - acx) + (y - aspring) < 0
                c.put(x, y, SKYGOLD["light"] if lit else SKYGOLD["base"])
    for y in range(19, 28):
        c.put(16, y, SKYGOLD["base"])
    for x in range(9, 24):
        c.put(x, 19, SKYGOLD["base"])
    for x in range(7, 25):
        c.put(x, 28, SKYGOLD["light"])
        c.put(x, 29, SKYGOLD["base"])
    _icon_frame(c, 6, 2, 20, 28)
    c.save(path)


def gen_fence_icon(path):
    """items/cloudmarblefence.png — one post with a run leaving on each side,
    which is how vanilla draws items/stonefence.png (632 opaque px)."""
    c = Canvas(32, 64)
    # Engine order: FenceObject draws the post first and the runs over it, so
    # the runs cover the post's own outline columns and the three pieces read as
    # one fence. An icon that paints the post last shows a black seam either
    # side of it and reads as three loose parts.
    _post_cell(c)
    _balustrade(c, 2, 8)
    _balustrade(c, 22, 8)
    for y in range(28, 52):                                # cap the free ends
        c.put(2, y, OUT)
        c.put(29, y, OUT)
    icon = Canvas(32, 32)
    # crop so the post's head lands on the icon's own top inset, the way
    # vanilla's items/stonefence.png crops its post
    icon.img.alpha_composite(c.img.crop((0, 16, 32, 48)), (0, 0))
    icon.px = icon.img.load()
    icon.save(path)


def gen_fencegate_icon(path):
    """items/cloudmarblefencegate.png — the closed leaf between its two piers."""
    c = Canvas(32, 64)
    _gate_post(c, 0, _PIER_2)
    _gate_post(c, 24, _PIER_2)
    _gate_leaf(c, 8, 24, 12, 20)
    icon = Canvas(32, 32)
    icon.img.alpha_composite(c.img.crop((0, 18, 32, 50)), (0, 0))
    icon.px = icon.img.load()
    icon.save(path)


def gen_skyway_icon(path):
    """items/skywaytile.png. Note: TerrainSplatterTile.generateItemTexture builds
    a tile's inventory icon from cell (3,0) of the `_splat` and never reads
    items/<tileid>.png, so this file only matters if the Java side registers a
    separate item for it."""
    c = Canvas(32, 32)
    _skyway_tile(0).blit_to(c, 0, 0)
    for x in range(32):                                     # a soft dark keyline
        c.put(x, 0, mix(c.get(x, 0)[:3], OUT, 0.55))
        c.put(x, 31, mix(c.get(x, 31)[:3], OUT, 0.55))
    for y in range(32):
        c.put(0, y, mix(c.get(0, y)[:3], OUT, 0.55))
        c.put(31, y, mix(c.get(31, y)[:3], OUT, 0.55))
    c.save(path)


# ---------------------------------------------------------------------------

def generate(objects_dir, items_dir, tiles_dir):
    gen_cloudmarble_wall(os.path.join(objects_dir, "cloudmarblewall.png"))
    gen_cloudmarble_fence(os.path.join(objects_dir, "cloudmarblefence.png"))
    gen_cloudmarble_fencegate(os.path.join(objects_dir, "cloudmarblefencegate.png"))
    gen_skyway_tile(os.path.join(tiles_dir, "skyway.png"))
    gen_skyway_splat(os.path.join(tiles_dir, "skyway_splat.png"))
    gen_wall_icon(os.path.join(items_dir, "cloudmarblewall.png"))
    gen_door_icon(os.path.join(items_dir, "cloudmarbledoor.png"))
    gen_window_icon(os.path.join(items_dir, "cloudmarblewindow.png"))
    gen_fence_icon(os.path.join(items_dir, "cloudmarblefence.png"))
    gen_fencegate_icon(os.path.join(items_dir, "cloudmarblefencegate.png"))
    gen_skyway_icon(os.path.join(items_dir, "skywaytile.png"))


if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    repo = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(repo, "src", "main", "resources")
    objects_dir = os.path.join(out, "objects")
    items_dir = os.path.join(out, "items")
    tiles_dir = os.path.join(out, "tiles")
    for d in (objects_dir, items_dir, tiles_dir):
        os.makedirs(d, exist_ok=True)
    generate(objects_dir, items_dir, tiles_dir)
    print(f"cloudmarble: wrote 11 files into {out}")
