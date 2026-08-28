"""Beetlefreak masonry — the Veil's purple building set — redrawn at sheet format.

Design source: src/main/resources/kk-sprites/beetlewall.png, the supplied art.
That sheet is the source of record for the set's IDENTITY and stays untouched:

  * deep violet stone with a lighter violet SWIRL embossed into it
  * a cream-and-black BEAD moulding (the "piano key" band) as the trim
  * brass sconces carrying a yellow-green FLAME
  * a bead-rimmed ARCH with a swirled tympanum
  * magenta glass
  * a skull crowning the doorways

What it is NOT is a wall sheet. The supplied file is one continuous
illustration painted across the 4x8 body block, so the engine — which reads
that block as an auto-tile blob whose columns are TILE HALVES, and whose
column-to-half mapping is not even constant down the sheet — reassembles it
into rubble. Everything below is redrawn on the layout the renderer actually
reads, decoded from necesse.level.gameObject.WallObject /
WallWindowObject / WallDoorObject in the 1.3.2 decompile and verified by
composing scenes with tools/wall_render_preview.py.

  x   0.. 64   wall body, 16px grid, cols 0-3 x rows 0-7
  x  64.. 96   window insert, 16px cells, rows 0-7
  x  96..352   eight 32x128 door cells (32-cell indices 3..10)

THE BODY BLOCK IS NOT FOUR VARIANTS. Each tile is drawn as two 16px halves,
the left one at drawX and the right one at drawX+16, over three 16px bands
(drawY-16, drawY, drawY+16), so tiles overlap by 16px vertically. Which column
holds which half DEPENDS ON THE ROW:

    rows 0, 3, 4    col 0 left/closed   col 2 left/open
                    col 1 right/open    col 3 right/closed
    rows 1, 2       col 0 left/closed   col 1 left/open
                    col 2 right/open    col 3 right/closed

Columns 1 and 2 swap roles between the two groups (WallObject draws (1,2) at
drawX but (1,3) at drawX+16). That single fact is why a picture painted
straight across the block cannot tile, and it is the bug this file fixes.

Vertical grammar for a run of N tiles, top to bottom:

    row 0                       the free top cap, 16px  (drawn when nothing above)
    (row 2, row 1) x (N-1)      the repeating roof, 32px per tile
    row 3, row 4                the front FACE, 32px    (the bottom tile only)

So a wall shows its roof for every tile but the last, and its face on the last
one. Rows 5-7 are junction pieces; six of their cells — (2,5) (3,5) (2,6)
(3,6) (2,7) (3,7) — are unreachable, because WallObject's public overload
passes the same boolean[] as adj, sameWall AND isWall and every
`isWall[n] && !sameWall[n]` branch is therefore dead. They are painted anyway,
to match vanilla.

Deterministic: every random draw goes through px.Rng with an explicit seed, so
two runs produce byte-identical PNGs.
"""

import math
import os
import sys

from px import Canvas, Rng, mix, with_alpha
import palette
# Wrap (a canvas that wraps in x, so a motif crossing a tile seam comes back on
# the other side) and swirl are generic and already shipped next door; sharing
# them beats a second copy drifting out of step.
from gen_cloudmarble import Wrap, swirl

OUT = palette.OUTLINE

# ---------------------------------------------------------------------------
# Palette. Sampled off the supplied render and quantized to the 4-step ramps
# this mod builds every material on. Local to this file, the way
# gen_cloudmarble keeps its own — palette.py is shared and owned elsewhere.
#
# The supplied sheet is an anti-aliased render: 16,037 distinct colours in
# 19,471 opaque pixels, against vanilla stonewall's NINETEEN. Quantizing is not
# a simplification of the reference, it is the difference between pixel art and
# a photograph of pixel art.
# ---------------------------------------------------------------------------

STONE = {                       # the violet masonry of the wall's front face
    "deep":  (42, 27, 62),
    "base":  (70, 46, 100),
    "light": (98, 68, 130),
    "hi":    (132, 100, 162),
}
MORTAR = (30, 19, 46)
GLYPH = (118, 84, 150)          # the swirl embossed into the stone

# The CAP is the same masonry seen from directly above and has to read clearly
# DARKER than the face, or a block of wall looks like one flat slab with no
# thickness (vanilla stonewall's cap is near-black against a mid-grey face).
# It gets its own explicit ramp rather than a mix toward OUTLINE: OUTLINE
# (34,34,46) is already as dark as this violet's deepest step, so mixing toward
# it desaturates without darkening and the two ends of the ramp cross over.
CAP = {
    "deep":  (22, 15, 34),
    "base":  (32, 21, 48),
    "light": (44, 30, 64),
    "hi":    (58, 41, 80),
}
CAP_GLYPH = (50, 35, 70)

BONE = {                        # the cream-and-black bead moulding
    "deep":  (118, 110, 102),
    "base":  (190, 180, 158),
    "light": (230, 222, 200),
    "hi":    (255, 250, 234),
}
BEAD_DARK = (18, 12, 26)        # the black half of the bead band

BRASS = {                       # sconces, rims, finials
    "deep":  (100, 76, 36),
    "base":  (168, 132, 58),
    "light": (220, 188, 106),
    "hi":    (255, 238, 170),
}

FLAME = {                       # the Veil's yellow-green fire
    "deep":  (88, 122, 34),
    "base":  (152, 198, 50),
    "glow":  (200, 236, 100),
    "core":  (248, 255, 224),
}

GLASS = {                       # the magenta panes
    "deep":  (92, 40, 118),
    "base":  (154, 72, 184),
    "light": (208, 128, 232),
    "hi":    (248, 204, 255),
}

C = 16          # body cell size
TILE_TOP = 96   # door cells are drawn at drawY-96, so row 96 is the tile's top

SALT = 0x8EE71E


# ---------------------------------------------------------------------------
# Which 16px HALF of the tile each body cell is drawn as, and which 16px BAND
# of the 32px vertical rhythm it occupies. Both read straight off
# WallObject.addWallDrawOptions; see the module docstring for the derivation.
#
# HALF   0 = drawn at drawX, 1 = drawn at drawX+16
# BAND   0 = the tile's upper 16px, 1 = its lower 16px
#
# The band matters as much as the half: it is what lets the roof carry a full
# 32x32 field instead of a 16px strip stamped twice per tile.
# ---------------------------------------------------------------------------
HALF = {
    (0, 0): 0, (2, 0): 0, (1, 0): 1, (3, 0): 1,     # cap band
    (0, 1): 0, (1, 1): 0, (2, 1): 1, (3, 1): 1,     # roof, lower half
    (0, 2): 0, (1, 2): 0, (2, 2): 1, (3, 2): 1,     # roof, upper half
    (0, 3): 0, (2, 3): 0, (1, 3): 1, (3, 3): 1,     # face, upper half
    (0, 4): 0, (2, 4): 0, (1, 4): 1, (3, 4): 1,     # face, lower half
    (0, 5): 0, (2, 5): 0, (3, 5): 0, (1, 5): 1,     # junction, upper half
    (0, 6): 0, (1, 6): 1, (2, 6): 1, (3, 6): 1,     # junction, lower half
    (0, 7): 0, (3, 7): 0, (1, 7): 1, (2, 7): 1,     # junction, lower half
}
# row -> which 16px band of the 32px roof field the cell shows.
ROOF_BAND = {0: 1, 1: 1, 2: 0, 5: 0, 6: 1, 7: 1}


# ---------------------------------------------------------------------------
# Motifs
# ---------------------------------------------------------------------------

def bead_run(target, x0, x1, y, h=2, phase=0, period=4):
    """The set's signature trim: a cream bead, a black gap, repeating.

    Drawn as a run rather than a solid line because the supplied art's whole
    read is that alternation — a plain cream line turns the wall into a
    generic light-trimmed block and loses the Beetlefreak entirely. `period`
    is the full bead+gap pitch, so it must divide 32 for the band to wrap."""
    for x in range(x0, x1):
        lit = ((x - phase) % period) < period // 2
        for dy in range(h):
            if lit:
                target.put(x, y + dy, BONE["hi"] if dy == 0 else BONE["base"])
            else:
                target.put(x, y + dy, BEAD_DARK if dy == 0
                           else mix(BEAD_DARK, STONE["deep"], 0.5))


def flame(target, cx, y, h=3):
    """One small yellow-green flame hanging from a lantern lip at row `y`, drawn
    as a MASS first and cored after, so the tip never ends up an orphan pixel.

    Squat, not tall: a 3-1-1 taper reads as a dagger or an exclamation mark at
    1x, which is exactly what the first pass put every 32px along the wall."""
    for dy in range(h):
        w = 3 if dy < h - 1 else 1
        for dx in range(-(w // 2), w // 2 + 1):
            target.put(cx + dx, y + dy,
                       FLAME["glow"] if dy == 0 else FLAME["base"])
    target.put(cx, y, FLAME["core"])
    target.put(cx - 1, y + h - 2, FLAME["deep"])
    target.put(cx + 1, y + h - 2, FLAME["deep"])


def lantern(target, cx, y, stem=2):
    """A brass lantern hanging off the trim: a stem, a cup, and the flame under
    it. `y` is the row the stem springs from."""
    for dy in range(stem):
        target.put(cx, y + dy, BRASS["base"])
        target.put(cx - 1, y + dy, BRASS["deep"])
    cy = y + stem
    for dx in (-1, 0, 1):                               # the cup
        target.put(cx + dx, cy, BRASS["light"] if dx < 0 else BRASS["base"])
    flame(target, cx, cy + 1, 3)


def bead_arch(target, cx, spring, R, x0, x1, y0,
              rim=None, rim_dark=None, bead_on=None, bead_off=None):
    """The set's arch: a CONTINUOUS rim with a bead rhythm running inside it,
    and a shadow reveal under that.

    Two lessons are baked in. First, alternating bright and black across the
    WHOLE band chops the arch into isolated pebbles at this radius — the ring
    stops being a ring; the rim has to stay continuous and the pattern is
    played one pixel inside it. Second, the contrast has to suit the job: the
    window is a real opening and takes the bone rim, but a blind arcade rimmed
    in bone on violet stone reads as a row of white horseshoes rather than as
    masonry, so the wall face rims its arches one ramp step up in its OWN
    stone and spends the bone on the bead alone."""
    rim = BONE["light"] if rim is None else rim
    rim_dark = BONE["deep"] if rim_dark is None else rim_dark
    bead_on = BONE["hi"] if bead_on is None else bead_on
    bead_off = BEAD_DARK if bead_off is None else bead_off
    for y in range(y0, int(spring) + 1):
        for x in range(x0, x1):
            d = math.hypot(x - cx, y - spring)
            if d >= R or d < R - 3.0:
                continue
            lit = (x - cx) + (y - spring) < 0        # light from the top-left
            if d >= R - 1.0:                         # continuous outer rim
                target.put(x, y, rim if lit else rim_dark)
            elif d >= R - 2.0:                       # the bead rhythm
                ang = math.atan2(spring - y, x - cx)
                on = int((ang + math.pi) * R / 3.0) % 2 == 0
                target.put(x, y, bead_on if on else bead_off)
            else:                                    # reveal shadow
                target.put(x, y, mix(STONE["deep"], OUT, 0.45))


def skull(target, cx, cy):
    """The 9x7 skull that crowns a Beetlefreak doorway. Bone mass first, then
    the sockets punched in — the outline pass eats a 1px socket otherwise."""
    for dy in range(-3, 4):
        for dx in range(-4, 5):
            if abs(dx) == 4 and abs(dy) >= 2:
                continue
            if dy == 3 and abs(dx) > 2:
                continue
            target.put(cx + dx, cy + dy,
                       BONE["hi"] if dy <= -2 else
                       BONE["light"] if dy <= 0 else BONE["base"])
    for ex in (-2, 2):                                  # sockets
        for dy in (-1, 0):
            target.put(cx + ex, cy + dy, BEAD_DARK)
            target.put(cx + ex - 1 if ex < 0 else cx + ex + 1, cy + dy,
                       mix(BEAD_DARK, BONE["deep"], 0.4))
    target.put(cx, cy + 1, BEAD_DARK)                   # nose
    for dx in (-1, 0, 1):                               # jaw line
        target.put(cx + dx, cy + 3, BONE["deep"])


# ---------------------------------------------------------------------------
# The masonry field
# ---------------------------------------------------------------------------

def _brick_px(x, y):
    """One pixel of the wall's violet coursework, as a pure function of
    position, so the face tile and the door jambs draw the SAME field and their
    courses line up instead of drifting half a course apart.

    Courses are 5px apart (4px block, 1px joint) with the joints offset by half
    a block course to course; blocks are 8px wide, which divides 32 so the band
    wraps across a tile seam. Micro-detail per vanilla density: a lit top edge,
    a shaded foot, a sparse highlight fleck, an occasional swirl glyph and the
    set's green seep running out of a joint."""
    course = y // 5 if y >= 0 else -((-y + 4) // 5)
    ry = y - course * 5
    shift = 4 if course % 2 else 0
    bx = (x + shift) % 8
    if ry == 4 or bx == 7:                              # the joint
        return MORTAR
    r = Rng((x * 733 + y * 977) ^ SALT).float()
    if ry == 0:
        tone = STONE["light"]
    elif ry == 3:
        tone = STONE["deep"]
    else:
        tone = STONE["base"]
    if r < 0.045:
        tone = STONE["hi"]
    elif r < 0.085:
        tone = STONE["deep"]
    # a green seep out of one joint in twelve, two rows long
    s = Rng(((x + shift) // 8 * 4127 + course * 6151) ^ SALT).float()
    if s < 0.085 and bx in (0, 1) and ry in (0, 1):
        return FLAME["deep"] if ry == 0 else mix(FLAME["deep"], STONE["deep"], 0.5)
    return tone


def _brick_field(w, h, y0):
    """A wrapping block of coursework covering field rows [y0, y0+h)."""
    t = Wrap(w, h)
    for j in range(h):
        for i in range(w):
            t.put(i, j, _brick_px(i, y0 + j))
    # one swirl glyph every other course band, position-locked so it wraps
    for course in range((y0 // 5) - 1, (y0 + h) // 5 + 1):
        g = Rng((course * 9781) ^ SALT)
        if not g.chance(0.55):
            continue
        gx = g.range(0, w - 1)
        gy = course * 5 + 2 - y0
        if -3 <= gy <= h + 3:
            swirl(t, gx, gy, 2.6, GLYPH, turns=1.15, phase=g.float() * 6.28)
    return t


# ---------------------------------------------------------------------------
# The roof (what the engine calls the cap): the wall seen from straight above
# ---------------------------------------------------------------------------

_ROOF = None


def _roof_field():
    """32x32 of roof, wrapping in x, one full tile.

    Vanilla's cap is 93% one flat tone with a few percent of a second; two
    earlier walls in this repo failed here in opposite directions (dithered
    static, then soft banks that read as corrugation once tiled). This keeps
    vanilla's recipe — flat base, sparse speckle — and spends its detail budget
    on ONE readable micro-detail per tile: the set's violet swirl."""
    global _ROOF
    if _ROOF is not None:
        return _ROOF
    t = Wrap(32, 32, wrap_y=True)
    t.rect(0, 0, 32, 32, CAP["base"])
    rng = Rng(SALT ^ 0xC0FFEE)
    for _ in range(4):                                   # faint slab hints
        bx, by = rng.range(0, 31), rng.range(0, 31)
        for y in range(by, by + rng.range(2, 3)):
            for x in range(bx, bx + rng.range(5, 9)):
                t.put(x, y, CAP["light"])
    # ONE curl per tile, barely above the base. Two of them at glyph contrast
    # turned the roof into wallpaper: at 32px both axes, a legible motif
    # repeats often enough to be read as a printed pattern rather than stone.
    swirl(t, 10, 9, 3.4, CAP_GLYPH, turns=0.95, phase=0.4)
    for y in range(32):                                  # sparse speckle only
        for x in range(32):
            r = Rng((x * 7349 + y * 12611) ^ SALT).float()
            if r < 0.026:
                t.put(x, y, CAP["hi"])
            elif r < 0.058:
                t.put(x, y, CAP["deep"])
    _ROOF = t
    return t


def _roof_pixel(x, y):
    """One roof pixel in field space, for the door and window strips."""
    return _roof_field().get(x % 32, y % 32)


def _roof_cell(c, cell_x, cell_y):
    """Blit the roof field into one body cell, picking the half and the band
    the engine draws that cell as."""
    half = HALF[(cell_x, cell_y)]
    band = ROOF_BAND[cell_y]
    _roof_field().blit_to(c, cell_x * C, cell_y * C,
                          sx=half * C, sy=band * C, w=C, h=C)


# ---------------------------------------------------------------------------
# The face: what you see of the wall's front, on its bottom tile
# ---------------------------------------------------------------------------

_FACE = None


def _face_tile():
    """32x32 of wall FRONT FACE, one whole tile, position-locked so the two
    16px halves join and tile N's right edge meets tile N+1's left edge.

    Top: the lit lip where the roof overhangs, a bead string course, then
    violet coursework. Bottom: ONE ARCH PER TILE, so a wall of any length reads
    as the reference's arcade — bead-moulded rim, swirled tympanum, a pier
    straddling the tile boundary and a brass sconce burning on it."""
    global _FACE
    if _FACE is not None:
        return _FACE
    t = _brick_field(32, 32, 0)

    for x in range(32):                                  # the overhang's lit lip
        t.put(x, 0, STONE["hi"])
    bead_run(t, 0, 32, 1, h=2, phase=1)                  # bead string course

    cx, spring, R = 15.5, 29.0, 10.0

    # The tympanum: the wall standing INSIDE a blind arch. A RECESS is lit the
    # opposite way round to a bulge — the light comes over the top-left rim and
    # falls on the far, bottom-right, inside face, leaving the near top-left in
    # shadow. Shading it like everything else is what made the first pass read
    # as a row of white domes instead of an arcade.
    for y in range(18, 30):
        for x in range(3, 29):
            if math.hypot(x - cx, y - spring) < R - 3.0 and y <= spring:
                sh = (x - cx) / 12.0 + (y - spring) / 12.0
                t.put(x, y, STONE["deep"] if sh < -0.45 else
                      mix(STONE["base"], STONE["deep"], 0.5) if sh < 0.2
                      else STONE["base"])
    swirl(t, 13, 25, 2.8, GLYPH, turns=1.15, phase=0.6)
    swirl(t, 20, 26, 2.4, GLYPH, turns=1.1, phase=3.4)

    bead_arch(t, cx, spring, R, -3, 35, 17,
              rim=STONE["hi"], rim_dark=STONE["base"],
              bead_on=BONE["base"], bead_off=STONE["deep"])

    # The pier straddles the tile boundary: x29,30,31 | 0,1,2 -> a 6px pier at
    # every join. Bone on the lit edge, black on the shaded one; a pier painted
    # solid bone puts a cream stripe every 32px and swamps the arch.
    PIER = ((29, BONE["base"]), (30, STONE["hi"]), (31, STONE["light"]),
            (0, STONE["base"]), (1, STONE["deep"]), (2, BEAD_DARK))
    for y in range(18, 30):
        for x, tone in PIER:
            t.put(x, y, tone)
    for y in (18, 19):                                   # capital
        t.put(28, y, BONE["light"])
        t.put(3, y, BEAD_DARK)
    for x, tone in ((30, BRASS["light"]), (31, BRASS["base"]),
                    (0, BRASS["base"]), (1, BRASS["deep"])):
        t.put(x, 21, tone)                               # brass band on the pier

    # The lamp burns on the PIER, not on the arch crown: the pier straddles the
    # tile join, so a wall run gets one lantern every 32px exactly where the
    # reference puts them, and the arch head stays unbroken.
    lantern(t, 0, 22, stem=2)
    for dx in (-2, -1, 0, 1):                            # bone keystone
        t.put(16 + dx, 18, BONE["light"] if dx < 0 else BONE["base"])
        t.put(16 + dx, 19, BONE["base"] if dx < 0 else BONE["deep"])

    for x in range(32):                                  # foot
        t.put(x, 30, mix(STONE["deep"], OUT, 0.35))
        t.put(x, 31, OUT)
    _FACE = t
    return t


def _face_cell(c, cell_x, cell_y, lower):
    """Blit one 16px half of the face tile into a body cell."""
    _face_tile().blit_to(c, cell_x * C, cell_y * C,
                         sx=HALF[(cell_x, cell_y)] * C,
                         sy=C if lower else 0, w=C, h=C)


# ---------------------------------------------------------------------------
# Rims: how a wall run ends
# ---------------------------------------------------------------------------

# The lit edge is STONE, not BONE. A cream rim is the right trim for a
# HORIZONTAL moulding a few pixels tall, but the same cream run down the free
# end of a wall paints a full-height white stripe on every room's inside face,
# and at 1x that stripe is louder than the wall. The bead already carries the
# set on the crown and the string course; the ends only have to say "the stone
# stops here".
RIM_W = (STONE["hi"], BRASS["deep"])
RIM_E = (mix(STONE["deep"], OUT, 0.35), BEAD_DARK)


def _end_rim(c, x0, y0, h, west):
    """The trim turning down a free end of the wall. West is the lit side and
    takes the bright pair, east is in shade and takes the dark pair."""
    a, b = RIM_W if west else RIM_E
    xa = x0 if west else x0 + C - 1
    xb = x0 + 1 if west else x0 + C - 2
    for y in range(y0, y0 + h):
        c.put(xa, y, a)
        c.put(xb, y, b)


def _hrim(c, cell_x, cell_y, y, x0, x1, lit=False):
    tone = RIM_W[0] if lit else BRASS["deep"]
    for x in range(x0, x1):
        c.put(cell_x * C + x, cell_y * C + y, tone)


def _vrim(c, cell_x, cell_y, x, y0, y1, lit=False):
    tone = RIM_W[0] if lit else BRASS["deep"]
    for y in range(y0, y1):
        c.put(cell_x * C + x, cell_y * C + y, tone)


# ---------------------------------------------------------------------------
# The body block
# ---------------------------------------------------------------------------

def _build_body(c):
    # ---- row 0: the free top cap, with the bead crown and its lamp ---------
    # (0,0) left half / west END, (2,0) left half / continuing,
    # (1,0) right half / continuing, (3,0) right half / east END.
    crown = Wrap(32, 16)
    for y in range(16):
        for x in range(32):
            crown.put(x, y, _roof_pixel(x, 16 + y))       # continues the roof
    bead_run(crown, 0, 32, 0, h=2, phase=0)
    for x in range(32):
        crown.put(x, 2, BRASS["deep"])
    lantern(crown, 16, 3, stem=3)
    for col in (0, 2, 1, 3):
        crown.blit_to(c, col * C, 0, sx=HALF[(col, 0)] * C, sy=0, w=C, h=C)
    _end_rim(c, 0, 0, C, west=True)
    _end_rim(c, 3 * C, 0, C, west=False)

    # ---- rows 1-2: the repeating roof -------------------------------------
    # col 0 carries the west end, col 3 the east end, cols 1-2 are interior.
    for row in (1, 2):
        for col in range(4):
            _roof_cell(c, col, row)
        _end_rim(c, 0, row * C, C, west=True)
        _end_rim(c, 3 * C, row * C, C, west=False)

    # ---- rows 3-4: the front face, one arch per tile -----------------------
    for col in range(4):
        _face_cell(c, col, 3, lower=False)
        _face_cell(c, col, 4, lower=True)
    for col, west in ((0, True), (3, False)):
        for row in (3, 4):
            _end_rim(c, col * C, row * C, C, west=west)

    # ---- rows 5-7: junction / inner-corner pieces -------------------------
    # (0,5)/(0,6) is the left half where the wall continues WEST but not
    # south-west, so the roof has to stop against the neighbour's face; (0,7)
    # is the mirror case where the run below carries on west, so its rim
    # TAPERS OFF halfway down. Geometry follows gen_cloudmarble, decoded from
    # the same draw code and shipped; only the paint is new.
    for row in (5, 6, 7):
        for col in range(4):
            _roof_cell(c, col, row)

    _vrim(c, 0, 5, 0, 0, 16, True); _hrim(c, 0, 5, 0, 0, 6, True)
    _vrim(c, 1, 5, 15, 0, 16); _hrim(c, 1, 5, 0, 10, 16)
    _hrim(c, 2, 5, 0, 11, 16); _vrim(c, 2, 5, 15, 0, 5)
    _hrim(c, 3, 5, 0, 0, 16); _vrim(c, 3, 5, 15, 0, 6)

    _vrim(c, 0, 6, 0, 0, 16, True); _hrim(c, 0, 6, 15, 0, 6, True)
    _vrim(c, 1, 6, 15, 0, 16); _hrim(c, 1, 6, 15, 10, 16)
    _hrim(c, 2, 6, 0, 0, 16)
    _face_tile().blit_to(c, 2 * C, 6 * C + 3, sx=0, sy=19, w=C, h=13)
    _hrim(c, 3, 6, 0, 0, 16)
    _vrim(c, 3, 6, 15, 1, 9)
    _face_tile().blit_to(c, 3 * C, 6 * C + 3, sx=16, sy=19, w=12, h=13)

    _vrim(c, 0, 7, 0, 0, 6, True); _hrim(c, 0, 7, 0, 0, 6, True)
    _vrim(c, 1, 7, 15, 0, 6); _hrim(c, 1, 7, 0, 10, 16)
    _face_tile().blit_to(c, 2 * C + 10, 7 * C + 10, sx=6, sy=26, w=6, h=6)
    _face_tile().blit_to(c, 3 * C, 7 * C + 10, sx=20, sy=26, w=6, h=6)


# ---------------------------------------------------------------------------
# The window insert
#
# WallWindowObject.getWindowDir returns 1 for a NORTH-SOUTH wall and 0 for an
# EAST-WEST one, and the two draw completely different pictures out of the same
# 32px strip:
#
#   dir 1, north-south : rows 0-1 at drawY-16 and drawY. This is the LEFT or
#          RIGHT wall of a room and you are looking down at its TOP. Vanilla is
#          512/512 opaque here — the sheet has no hole in it — but the PICTURE
#          is a hole: a slot cut along the wall, dark reveals on its near
#          faces, a lit lip on the far one, glass at the bottom of the cut.
#          Both earlier passes drew a front-facing pane here instead, once as
#          the supplied art and once as a "flat skylight" that was still a
#          brass frame with glazing bars. Darkening a front-facing pane does
#          not make it lie down; only the slot shape does.
#   dir 0, east-west   : rows 2..7 at drawY-64, -48, -32, -16, 0 and +16.
#          Now it is the wall's FRONT and the opening is a real hole. Rows 2-4
#          reach two tiles above the tile and vanilla leaves them EMPTY; row 5
#          is the cap band, rows 6-7 the tile. The supplied art had a hanging
#          BANNER here, solid to the edges — the two views were swapped.
#
# tools/sheet_format_audit.py asserts rows 0-1 == 512 opaque, rows 2-4 == 0,
# and rows 5-6 not solid.
# ---------------------------------------------------------------------------

def _build_window(c):
    X = 64

    # --- dir 1: the wall's TOP, with a slot cut down through it ------------
    #
    # These two cells are the LEFT and RIGHT walls of a room. They are drawn at
    # drawY-16 and drawY, which is the band showing the tile above's lower half
    # and this tile's upper half — in a north-south run that band is unbroken
    # ROOF for its whole length. So the picture is the wall seen from directly
    # above with a hole in it, and the player is looking DOWN into the hole.
    #
    # Every vanilla wall draws it the same way and the grammar is not subtle:
    # wall top down both sides, an opening running ALONG the wall (tall and
    # narrow in the cell, never a wide pane across it), a dark reveal on the
    # near faces of the cut, a lit lip on the far one, and the glass at the
    # bottom of the cut. Measured off stonewall, brickwall and granitewall
    # cols 4-5 rows 0-1: the opening is 10-12px wide, centred, and runs almost
    # the full 32px of the cell.
    #
    # What was here instead was a brass-framed pane with a two-by-two lattice —
    # a window standing UP out of the roof, facing south. That is the fault the
    # player named: "Fenster an der Seite zeigen nie in Richtung Süden ... das
    # wäre ja mitten in der Wand, nach oben ausgerichtet." Being dark did not
    # save it; what makes a cut read as a cut is its SHAPE and its reveals, and
    # vanilla's own top-down glass is brighter than the roof around it, not
    # darker.
    t = Wrap(32, 32)
    for y in range(32):
        for x in range(32):
            # The cell spans [drawY-16, drawY+16). The half above drawY is the
            # roof field's LOWER band, so the whole cell is that field shifted
            # 16 rows; sampling it unshifted put a visible jump in the stone
            # exactly where the window meets the roof above it.
            t.put(x, y, _roof_pixel(x, y + 16))
    for y in range(32):                                  # the roof's own ends
        t.put(0, y, RIM_W[0])
        t.put(1, y, RIM_W[1])
        t.put(30, y, RIM_E[0])
        t.put(31, y, RIM_E[1])

    SX0, SX1 = 10, 21                                    # the cut, 12px wide
    SY0, SY1 = 2, 29                                     # and 28px long
    for y in range(SY0, SY1 + 1):                        # sink the whole cut
        for x in range(SX0, SX1 + 1):
            t.put(x, y, BEAD_DARK)
    # Reveals. Light comes over the top-left, so it falls on the FAR inside
    # faces — bottom and right — and leaves the near ones in shadow. Getting
    # this backwards is what turns a hole into a lid.
    for x in range(SX0, SX1 + 1):
        t.put(x, SY0, BEAD_DARK)                         # near lip, shaded
        t.put(x, SY0 + 1, CAP["deep"])
        t.put(x, SY1 - 1, STONE["base"])                 # far lip, lit
        t.put(x, SY1, STONE["light"])
    for y in range(SY0, SY1 + 1):
        t.put(SX0, y, BEAD_DARK)                         # near (west) reveal
        t.put(SX0 + 1, y, CAP["base"])
        t.put(SX1 - 1, y, CAP["hi"])
        t.put(SX1, y, STONE["light"])                    # far (east) reveal, lit
    # The identity goes on the RIM of the hole and nowhere else: one bone stud
    # at each corner of the cut. No band across the cell in either direction —
    # a cream band here tiles into a stripe running the whole wall, which is
    # the same mistake as banding a door leaf, and vanilla's top-down windows
    # carry no horizontal terminator at all. The opening just runs until the
    # roof resumes.
    for sx in (SX0 + 1, SX1 - 1):
        for sy in (SY0 + 1, SY1 - 1):
            t.put(sx, sy, BONE["light"])
    # The glass, at the bottom of the cut. It is BRIGHTER than the roof around
    # it, which is what every vanilla wall does here — stonewall's top-down
    # glass is (130,139,152) against a (34,35,35) roof, brickwall's is pale
    # cyan against red brick. Brightness was never what made the old cell read
    # as a standing window; the frame and the glazing bars were. Sheen bands
    # run north-south, because so does the opening and everything in it.
    gcore = mix(GLASS["deep"], GLASS["base"], 0.55)
    for y in range(SY0 + 2, SY1 - 1):
        for x in range(SX0 + 2, SX1 - 1):
            edge = x in (SX0 + 2, SX0 + 3, SX1 - 2, SX1 - 3)
            if y <= SY0 + 4:                             # the near lip's shadow
                tone = GLASS["deep"]
            elif y >= SY1 - 4:                           # light pooling far side
                tone = GLASS["light"] if edge else GLASS["base"]
            else:
                tone = GLASS["light"] if edge else gcore
            if (x * 7 + y * 5) % 17 == 0:
                tone = GLASS["hi"]
            t.put(x, y, tone)
    for x in range(SX0 + 2, SX1 - 1):                    # one saddle bar
        t.put(x, 15, BRASS["base"])
        t.put(x, 16, BRASS["deep"])
    t.blit_to(c, X, 0)

    # --- rows 2-4 stay empty ----------------------------------------------

    # --- dir 0: the front, with the hole in it -----------------------------
    face = _face_tile()
    JAMB = 6
    # row 5 (drawY-16) is the cap band: only the jambs stand there, the middle
    # is open sky over the arch head, exactly as vanilla leaves it.
    for x in list(range(0, JAMB)) + list(range(32 - JAMB, 32)):
        for y in range(16):
            c.put(X + x, 80 + y, _roof_pixel(x, 16 + y))
    bead_run(c, X, X + JAMB, 80, h=2, phase=-X % 4)
    bead_run(c, X + 32 - JAMB, X + 32, 80, h=2, phase=-X % 4)
    for y in range(80, 96):
        c.put(X + 0, y, RIM_W[0])
        c.put(X + 1, y, RIM_W[1])
        c.put(X + 30, y, RIM_E[0])
        c.put(X + 31, y, RIM_E[1])

    # the bead arch head over the opening, offset into sheet space
    acx, aspring, AR = X + 15.5, 100.0, 12.0
    bead_arch(c, acx, aspring, AR, X, X + 32, 82)
    acx -= X

    # rows 6-7: the jambs carry the wall's own face, so the courses line up
    for x in list(range(0, JAMB)) + list(range(32 - JAMB, 32)):
        for y in range(32):
            p = face.get(x, y)
            if p[3]:
                c.put(X + x, 96 + y, p)
    # the opening: magenta glazing you can still see the ground through. The
    # test is INSIDE the arch, not outside it — filling by the complement puts
    # the glaze back over the bone rim it was supposed to sit under.
    for y in range(96, 118):
        for x in range(JAMB, 32 - JAMB):
            if y > aspring or math.hypot(x - acx, y - aspring) < AR - 3.0:
                lit = (x + y) % 9 == 0
                c.put(X + x, y, with_alpha(GLASS["light"] if lit
                                           else GLASS["base"], 150))
    for y in range(96, 118):                             # central mullion
        c.put(X + 15, y, BRASS["base"])
        c.put(X + 16, y, BRASS["deep"])
    for x in range(JAMB, 32 - JAMB):                     # transom
        c.put(X + x, 108, BRASS["base"])
        c.put(X + x, 109, BRASS["deep"])
    for y in range(96, 118):                             # reveals
        c.put(X + JAMB, y, BONE["light"])
        c.put(X + 31 - JAMB, y, BEAD_DARK)
    # row 7's lower part: the sill, then the wall's foot. Solid all the way.
    for x in range(32):
        for y in range(118, 128):
            p = face.get(x, y - 96)
            c.put(X + x, y, p if p[3] else STONE["base"])
    bead_run(c, X + JAMB - 2, X + 34 - JAMB, 118, h=2, phase=0)
    for x in range(JAMB - 2, 34 - JAMB):
        c.put(X + x, 120, BRASS["deep"])
    for x in range(32):
        c.put(X + x, 126, mix(STONE["deep"], OUT, 0.35))
        c.put(X + x, 127, OUT)


# ---------------------------------------------------------------------------
# The doors
#
# WallDoorObject/WallDoorOpenObject draw EVERY one of the eight 32x128 cells at
# pos(drawX, drawY - 96), so row 96 is the tile's top edge and everything above
# it hangs over a wall that only rises 16px above its own tile. The extents
# below are vanilla stonewall's, asserted by tools/sheet_format_audit.py.
#
#   cell 3: y88..127 rot 0 closed, head-on     cell 7: y88..127 rot 2
#   cell 4: y68..127 rot 0 open, leaf edge-on  cell 8: y68..127 rot 2
#   cell 5: y70..127 rot 1 closed, edge-on     cell 9: y70..127 rot 3 (mirrored)
#   cell 6: y68..127 rot 1 open, head-on       cell 10: y90..127 rot 3
#
# Cells 5 and 9 are NOT full-width: vanilla draws the closed edge-on door as a
# narrow slab against one jamb (stonewall's cell 5 spans x14..32, cell 9
# x0..18). The supplied art put a full-width lamp post in both.
#
# WHAT GOES INSIDE THAT SILHOUETTE is a separate question, and matching the
# silhouette exactly is what let the second fault through every gate: the
# bounding boxes were byte-identical to stonewall's and the doors still read as
# hatches. Decoded cell by cell off stonewall and woodwall, the composition
# rule is:
#
#   * ONE leaf, running the FULL height of the cell. woodwall's edge-on leaf
#     is unbroken from y70 to its threshold at y124; stonewall's is unbroken
#     from y70 to y123. Ours had a stub of wall with a lantern on it above the
#     tile edge and a 3px sliver of leaf below — two objects, and neither of
#     them a door.
#   * the CROWN carries the ornament, and the crown is the part ABOVE row 96.
#     Nothing cream, nothing bright and nothing horizontal crosses the leaf
#     itself; the leaf gets its frame, its boards and one boss.
#   * the leaf reads a step LIGHTER above the tile edge than below it, because
#     the part above is the door's top catching the sky and the part below is
#     standing in the doorway's shade. stonewall does this literally: the same
#     leaf is (115,130,151) above row 96 and (60,65,74) below it.
# ---------------------------------------------------------------------------

def _jamb_px(x, y):
    """One pixel of door-surround masonry. Sheet row 96 is the tile's top edge,
    which is where the wall face's own row 0 lands, so the jamb draws the same
    field offset by -96 and its courses agree with the wall beside it."""
    return _brick_px(x % 32, y - TILE_TOP)


SEAM = mix(STONE["deep"], OUT, 0.45)


def _leaf_px(x, y, vertical, shaded=False):
    """The door leaf: violet planking. `vertical` puts the seams on the screen
    axis the leaf is LONG on, which every door cell now is — a tall leaf with
    seams across it is a ladder, not a door. The bone on a leaf is its FRAME
    and its boss, painted over this; a bone grain in the fill turns the door
    into a portcullis.

    `shaded` is the same boarding one ramp step down, for the part of a leaf
    standing inside the doorway rather than above the wall. Vanilla shades the
    two halves of an edge-on leaf exactly this way (stonewall's is (115,130,151)
    above row 96 and (60,65,74) below). Shifting the ramp instead of mixing a
    second one keeps the sheet inside its palette: vanilla wall sheets carry
    19-34 distinct colours in total."""
    n = x if vertical else y
    m = n % 6
    if shaded:
        if m == 0:
            return OUT
        if m == 1:
            return STONE["base"]
        if m == 5:
            return SEAM
        return STONE["deep"]
    if m == 0:
        return SEAM                                      # board seam
    if m == 1:
        return STONE["light"]
    if m == 5:
        return STONE["deep"]
    return STONE["base"]


def _leaf_frame(target, fx0, fx1, fy0, fy1):
    """The brass frame every leaf carries. A swung-open leaf drawn as bare
    boarding does not match the closed one it turns into, and the pair reads as
    two different objects when the player opens the door."""
    for x in range(fx0, fx1 + 1):
        target.put(x, fy0, BRASS["light"])
        target.put(x, fy1, BRASS["deep"])
    for y in range(fy0, fy1 + 1):
        target.put(fx0, y, BRASS["light"])
        target.put(fx1, y, BRASS["deep"])


def _boss(target, cx, cy, r=3):
    """The green diamond boss at the centre of a Beetlefreak leaf."""
    for dy in range(-r - 1, r + 2):
        for dx in range(-r - 1, r + 2):
            u = abs(dx) / (r + 1.0) + abs(dy) / (r + 1.0)
            if u > 1.0:
                continue
            target.put(cx + dx, cy + dy,
                       FLAME["core"] if u < 0.28 else
                       FLAME["glow"] if u < 0.58 else
                       FLAME["base"] if u < 0.85 else FLAME["deep"])


def _leaf_panel(target, lx0, lx1, ly0, ly1, gem_at=0.45, gem_r=3):
    """ONE door leaf, whatever its aspect: boards running the leaf's long axis,
    a brass frame around the whole of it, and a single small green boss.

    The rule this encodes is the one the previous pass broke. NOTHING crosses a
    leaf horizontally. A cream bead rail run across a 30px leaf cuts it into
    two 14px halves and the door stops reading as a door — a header with a
    hatch under it, which is exactly what the player saw ("die Türen wirken
    viel zu kurz"). Vanilla stonewall and woodwall both keep the leaf one
    unbroken field from the crown to the threshold and spend all their trim on
    the frame and the crown above the tile edge. So does this now: the bead and
    the skull live on the crown, the leaf gets the frame, the boards and one
    boss."""
    for y in range(ly0, ly1 + 1):
        for x in range(lx0, lx1 + 1):
            target.put(x, y, _leaf_px(x, y, (ly1 - ly0) >= (lx1 - lx0)))
    _leaf_frame(target, lx0, lx1, ly0, ly1)
    _boss(target, (lx0 + lx1) // 2, ly0 + int((ly1 - ly0) * gem_at), gem_r)


def _crown_bead(target, bands, body):
    """The bead moulding round a door's CROWN, run along the silhouette of the
    whole head rather than along each chamfer course.

    Rimming every course separately draws a stack of parallel cream lines — a
    venetian blind hung over the door, which is what the open leaves wore. The
    head is one shape and gets one outline; `body` is the mass under it, so the
    rim stops where the head meets the leaf instead of drawing a line across
    it."""
    head = {(x, y) for (ay0, ay1, ax0, ax1) in bands
            for y in range(ay0, ay1 + 1) for x in range(ax0, ax1 + 1)}
    by0, by1, bx0, bx1 = body
    solid = head | {(x, y) for y in range(by0, by1 + 1)
                    for x in range(bx0, bx1 + 1)}
    for (x, y) in sorted(head):
        if any((x + dx, y + dy) not in solid
               for dx, dy in ((0, -1), (-1, 0), (1, 0))):
            target.put(x, y, BONE["hi"] if (x + y) % 4 < 2 else BEAD_DARK)


def _leaf_handle(target, x, y):
    """A two-pixel brass pull. Small on purpose: it is the only thing besides
    the boss allowed to interrupt the leaf."""
    target.put(x, y, BRASS["hi"])
    target.put(x, y + 1, BRASS["light"])
    target.put(x, y + 2, BRASS["deep"])


def _build_doors(c):
    def fill(bands, tone):
        for y0, y1, bx0, bx1 in bands:
            for y in range(y0, y1 + 1):
                for x in range(bx0, bx1 + 1):
                    c.put(x, y, tone(x, y))

    # (a per-band top rim used to live here; every crown now goes through
    # _crown_bead, which rims the head's SILHOUETTE instead of each course)

    def floor_line(y, x0, x1):
        for x in range(x0, x1 + 1):
            c.put(x, y, OUT)

    for i in range(8):
        x0 = 96 + i * 32
        rot = i // 2
        is_open = i % 2 == 1
        head_on = (rot in (0, 2)) != is_open

        if not is_open and head_on:
            # Closed, head-on: the doorway. A chamfered violet arch head over a
            # planked leaf, skull on the keystone. 40px tall, i.e. 8px above its
            # tile — deliberately shorter than the wall beside it, as vanilla
            # builds a door. The 8px above row 96 is the CROWN and everything
            # decorative lives there; the 32 below it is one leaf.
            arch = [(88, 89, x0 + 5, x0 + 26), (90, 91, x0 + 3, x0 + 28),
                    (92, 93, x0 + 1, x0 + 30), (94, 127, x0, x0 + 31)]
            fill([(94, 127, x0, x0 + 31)], _jamb_px)
            # The three chamfer courses are ONE arch head: one continuous stone
            # and one bead line following the outer step. Masonry joints and a
            # rim per course read as three slabs stacked over the door.
            for (ay0, ay1, ax0, ax1) in arch[:3]:
                for y in range(ay0, ay1 + 1):
                    for x in range(ax0, ax1 + 1):
                        s_ = (x - (x0 + 16)) / 16.0 + (y - 96) / 16.0
                        c.put(x, y, STONE["hi"] if s_ < -0.55 else
                              STONE["light"] if s_ < -0.1 else STONE["base"])
            # One bead line around the SILHOUETTE of the whole head; outlining
            # each course separately draws nested rectangles, a staircase.
            _crown_bead(c, arch[:3], (94, 127, x0, x0 + 31))
            skull(c, x0 + 16, 91)                         # crowning the arch
            # 22px of leaf in the 32px tile. stonewall's opening is 20px and
            # woodwall's leaf is the full 32; the 16px leaf that was here, set
            # between 8px jambs and two brass reveals, left an opening barely
            # half the tile wide and read as a hatch let into the wall rather
            # than a doorway through it.
            _leaf_panel(c, x0 + 5, x0 + 26, TILE_TOP, 125, gem_at=0.40)
            _leaf_handle(c, x0 + 24, 111)
            for y in range(TILE_TOP, 126):                # reveal beside the leaf
                c.put(x0 + 4, y, BRASS["light"])
                c.put(x0 + 27, y, BRASS["deep"])
            for x in range(x0, x0 + 32):                  # threshold
                c.put(x, 126, mix(STONE["deep"], OUT, 0.35))
            floor_line(127, x0, x0 + 31)

        elif not is_open:
            # Closed, edge-on: the door standing in a NORTH-SOUTH wall, seen
            # from its side, and the cell the player meets on every left and
            # right wall of every room. 58px of it, and vanilla spends all 58
            # on ONE leaf: woodwall's planked leaf runs unbroken from y70 to a
            # threshold at y124, stonewall's from y70 to y123, and only the
            # bottom 32px — the part inside the tile — gains a reveal either
            # side of it.
            #
            # What was here was a stub of wall masonry with a brass lantern
            # burning on it above the tile edge, then a full-width cream bead
            # band at row 96, then a slab of ROOF pixels with a 3px sliver of
            # leaf in it. Three unrelated things stacked, the door being the
            # smallest, which is precisely "die Türen wirken viel zu kurz".
            mirror = rot == 3

            def mx(a, b):                                # cell-local, mirrored
                return (x0 + (31 - b), x0 + (31 - a)) if mirror else (x0 + a,
                                                                      x0 + b)

            lx0, lx1 = mx(18, 27)                        # the leaf, all 58px
            cx0, cx1 = mx(20, 25)                        # its crown cap
            fx0, fx1 = mx(14, 31)                        # the tile's own block
            # The reveal: the doorway cut into the wall, dark, with its top
            # edge catching the light the way stonewall's does.
            for y in range(TILE_TOP, 124):
                for x in range(fx0, fx1 + 1):
                    c.put(x, y, CAP["deep"])
            for x in range(fx0, fx1 + 1):
                c.put(x, TILE_TOP, STONE["light"])
                c.put(x, TILE_TOP + 1, STONE["base"])
            # The leaf. One field, top to bottom; a step lighter above the tile
            # edge (its top, catching the sky) than below it (in the doorway's
            # shade), which is the only thing vanilla changes down its length.
            for y in range(70, 124):
                for x in range(lx0, lx1 + 1):
                    c.put(x, y, _leaf_px(x, y, True, shaded=y >= TILE_TOP))
            for y in range(70, 72):                      # the cap's own width
                for x in range(lx0, lx1 + 1):
                    if not (cx0 <= x <= cx1):
                        c.put(x, y, (0, 0, 0, 0))
            _leaf_frame(c, lx0, lx1, 72, 123)
            for x in range(cx0, cx1 + 1):                # crown: the bone cap
                c.put(x, 70, BONE["hi"])
                c.put(x, 71, BONE["base"])
            _boss(c, (lx0 + lx1) // 2, 110, 2)           # the one green accent
            _leaf_handle(c, lx1 - 2 if mirror else lx0 + 2, 116)
            for x in range(fx0, fx1 + 1):                # threshold
                c.put(x, 124, BONE["deep"])
                c.put(x, 125, STONE["deep"])
                c.put(x, 126, SEAM)
            floor_line(127, fx0, fx1)

        elif not head_on:
            # Open, edge-on: the leaf has swung a quarter turn and stands
            # against the jamb as a narrow slab, threshold below it. Vanilla
            # draws it as one tall post the whole height of the cell —
            # stonewall's runs x14..22 from y68 down past the tile edge, with
            # the wall stub beneath. Ours framed a panel across the middle
            # third of the slab and ran two cream bead bands across it, so the
            # 48px leaf read as a 16px placard on a pole.
            leaf = [(68, 69, x0 + 24, x0 + 29), (70, 115, x0 + 22, x0 + 31)]
            fill(leaf, lambda x, y: _leaf_px(x, y, True, shaded=y >= TILE_TOP))
            _leaf_frame(c, x0 + 22, x0 + 31, 70, 115)
            for x in range(x0 + 24, x0 + 30):            # crown: the bone cap
                c.put(x, 68, BONE["hi"])
                c.put(x, 69, BONE["base"])
            _boss(c, x0 + 27, 96, 2)
            _leaf_handle(c, x0 + 25, 106)
            fill([(116, 126, x0, x0 + 31)], lambda x, y: _roof_pixel(x, y))
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
            _crown_bead(c, arch, (body_top, 126 if rot == 3 else 103,
                                  x0, x0 + 31))
            # Same rule as the closed leaf: the arch cap is the crown and takes
            # the bone, the body is ONE field with a frame round the whole of
            # it and a single boss. The two cream bead bands that used to run
            # across it cut a 36px leaf into three stripes.
            if rot == 1:
                fill([(body_top, 103, x0, x0 + 31)],
                     lambda x, y: _leaf_px(x, y, True))
                _leaf_frame(c, x0 + 1, x0 + 30, body_top, 102)
                _boss(c, x0 + 16, 88, 3)
                _leaf_handle(c, x0 + 27, 94)
                floor_line(103, x0, x0 + 31)
                fill([(104, 126, x0 + 16, x0 + 31)],
                     lambda x, y: _roof_pixel(x, y))
                floor_line(127, x0 + 16, x0 + 31)
            else:
                fill([(body_top, 126, x0, x0 + 31)],
                     lambda x, y: _leaf_px(x, y, True))
                _leaf_frame(c, x0 + 1, x0 + 30, body_top, 125)
                _boss(c, x0 + 16, 110, 3)
                _leaf_handle(c, x0 + 27, 116)
                fill([(120, 126, x0, x0 + 15)],
                     lambda x, y: _roof_pixel(x, y))
                floor_line(127, x0, x0 + 31)
            # the hinge pier the leaf swings from: without it a full-width
            # panel reads as a chest lid rather than a door
            hinge_top = body_top if rot == 1 else top
            hinge_bot = 103 if rot == 1 else 119
            for y in range(hinge_top, hinge_bot + 1):
                c.put(x0, y, BEAD_DARK)
                c.put(x0 + 1, y, BRASS["base"])
                c.put(x0 + 2, y, BRASS["light"] if y % 6 == 2 else BRASS["deep"])

        # A CLOSED door's outer columns are where the leaf meets the wall, so
        # they carry the wall's own end trim. An open leaf stands clear of the
        # wall and keeps its own edges.
        if not is_open:
            for y in range(128):
                for x in (x0, x0 + 31):
                    if c.filled(x, y) and y < 127:
                        c.put(x, y, RIM_E[1] if x == x0 + 31 else RIM_W[0])


# ---------------------------------------------------------------------------
# Item icons — cut from the sheet the wall actually draws from
#
# Vanilla's wall icons are a 20x28 chunk of the material in a 32x32 slot
# (stonewall 560 opaque px, stonedoor 456, stonewindow 536), so these take the
# same crops out of our own sheet rather than inventing a second drawing of the
# same material.
# ---------------------------------------------------------------------------

def _icon_from(sheet, box, out_path, slot=(6, 2, 26, 30)):
    from PIL import Image
    strip = sheet.img.crop(box)
    w, h = slot[2] - slot[0], slot[3] - slot[1]
    icon = Image.new("RGBA", (32, 32))
    fitted = strip.resize((w, h), Image.NEAREST)
    icon.paste(fitted, (slot[0], slot[1]), fitted)
    icon.save(out_path)
    return icon


def build_sheet():
    c = Canvas(352, 128)
    _build_body(c)
    _build_window(c)
    _build_doors(c)
    return c


def generate(objects_dir, items_dir):
    sheet = build_sheet()
    sheet.save(os.path.join(objects_dir, "beetlewall.png"))
    # wall: one whole face tile, cols 0-1 of rows 3-4 -> a contiguous 32x32
    _icon_from(sheet, (0, 48, 32, 80), os.path.join(items_dir, "beetlewall.png"))
    # door: the tile half of cell 7, the closed head-on leaf with its skull
    _icon_from(sheet, (7 * 32, 92, 7 * 32 + 32, 128),
               os.path.join(items_dir, "beetledoor.png"), (6, 4, 26, 30))
    # window: the front rows of the insert, where the opening is
    _icon_from(sheet, (64, 96, 96, 128),
               os.path.join(items_dir, "beetlewindow.png"))


if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    repo = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(repo, "src", "main", "resources")
    generate(os.path.join(out, "objects"), os.path.join(out, "items"))
    print("beetlewall: wrote 4 files into %s" % out)
