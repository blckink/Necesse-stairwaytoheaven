"""Sky Warden clothing as real Necesse player-armor sheets.

The Warden stops being a bespoke 64px mob sprite and becomes a HumanMob wearing
registered armor items, exactly the way vanilla builds the Elder (HumanShop +
elderhat/eldershirt/eldershoes drawn onto the standard human body).

Everything here was measured against the 1.3.2 sprite dump and the decompiled
renderer before a single pixel was drawn; the notes below are the format law
this module implements.

FORMAT (verified against `player/skin/*` and `player/armor/*`)
--------------------------------------------------------------
* Cells are 64x64, 7 columns x 4 or 5 rows.
* Row = facing direction: 0 up(back), 1 right, 2 down(front), 3 left
  (`Mob.getAnimSprite` -> `new Point(0, dir)`).
* Column = animation frame: 0 idle, 1-4 walk, 5 in-liquid, 6 downed
  (`Mob.getAnimSprite`: `p.x = 5` in liquid, `distanceRan % 4 + 1` walking;
  `HumanMob.getAnimSprite` forces `sprite.x = 6` while downed).
* Row 4 of a 5-row sheet is NOT a fifth direction.  It is addressed at 32px
  resolution: `ChestArmorItem.getAttackArmSprite` returns
  `new GameSprite(armorTexture, 0, 8, 32)` -> the pixel block (0,256)-(32,288),
  the sleeve drawn over the rotating attack arm (the bare arm underneath is
  `new GameSprite(bodyTexture, 0, 8, 32)` in HumanDrawOptions).  Sprite (1,8)
  carries the cuff over the back of the hand.  Arms, boots and `_back` sheets
  leave row 4 empty in vanilla, and so do we.
* **All player art is 32x32 half-resolution art scaled 2x.**  Every 2x2 block
  on even coordinates in skin/*, armor/* and facialfeature/* is either fully
  transparent or one uniform opaque colour - checked exhaustively, zero
  exceptions.  Drawing at true 64px would read as higher-resolution than the
  body it sits on, so every cell here is authored at 32x32 and upscaled 2x
  with NEAREST.
* 4-6 colours per piece.  The darkest ramp step doubles as both silhouette
  contour and internal fold line (magerobe: 3 blues + 1 gold, total; the robe
  skirt is literally alternating dark/cloth vertical stripes).  A full outline
  ring is wrong here: an arm cap is 3 pixels wide, and ringing it leaves
  nothing but outline.

REPEAT PATTERN (cells compared byte-wise, ignoring transparent RGB)
-------------------------------------------------------------------
* head / helmet / hood, and chest rows 0+2 : [0, 0^, 0, 0^, 0, 0, 0]
  where `0^` is column 0 shifted UP 2px - the walk bob, nothing more.
  Exact on skin/head, skin/body, elderhat, leatherhood, eldershirt row 2.
* chest rows 1+3 (side)  : [0, 1, 2, 3, 4, 4, 6]  - hem sway per frame.
* arms                   : [0, 1, 0, 3, 0, 0, 6]
* feet / boots           : [0, 1, 2, 3, 4, 0, 6]  - the feet actually step.
* row 3 == mirror(row 1) for every symmetric piece (verified on skin/head,
  skin/body, eldershirt, leatherhood, skin/feet), so side views are authored
  once and mirrored.

ANCHORS (half-res = 64px cell coordinates / 2)
-----------------------------------------------
head  down x9..22 y7..20        eyes down x12..19 y14..17
beard (facialfeature) down x11..20 y17..23 - the hood leaves the lower face
      open, and the item should use FacialFeatureDrawMode.UNDER_FACIAL_FEATURE
      so the beard renders over the cowl.
body  down x11..20 y20..23      feet down x11..20 y24..27
arms  down: left shoulder x19..20 y20, right shoulder x11..12 y20
"""

import os

from PIL import Image

import palette
import px

H = 32          # half-res cell
FULL = 64       # rendered cell

# ---------------------------------------------------------------------------
# Palette.  Every colour already exists in palette.py: the Warden's storm-blue
# coat ramp, the pale weathered grey-white that carried his identity as a mob
# sprite, and the one warm gold trim accent.
# ---------------------------------------------------------------------------
OUT = palette.OUTLINE                  # ( 34,  34,  46)
DEEP = palette.WARDEN["coat_deep"]     # ( 48,  52,  76)
BASE = palette.WARDEN["coat"]          # ( 68,  76, 104)
LIT = palette.WARDEN["coat_light"]     # ( 94, 104, 136)
HI = palette.WARDEN["coat_hi"]         # (120, 132, 166)
PALE_D = palette.WARDEN["patch"]       # (134, 142, 160)
PALE = palette.WARDEN["hair_shade"]    # (178, 182, 196)
PALE_L = palette.WARDEN["feather_hi"]  # (196, 194, 212)
GOLD = palette.WARDEN["trim"]          # (204, 160,  82)
GOLD_H = palette.WARDEN["trim_hi"]     # (238, 202, 124)


# ---------------------------------------------------------------------------
# Half-res cell
# ---------------------------------------------------------------------------

class Cell:
    """A 32x32 half-res cell that upscales into a 64px armor frame."""

    def __init__(self):
        self.c = px.Canvas(H, H)

    def put(self, x, y, color):
        self.c.put(x, y, color)

    def get(self, x, y):
        return self.c.get(x, y)

    def filled(self, x, y):
        return self.c.filled(x, y)

    def image(self):
        return self.c.img.resize((FULL, FULL), Image.NEAREST)

    def shifted(self, dy):
        out = Cell()
        for x in range(H):
            for y in range(H):
                p = self.get(x, y)
                if p[3] > 0:
                    out.put(x, y + dy, p)
        return out

    def mirrored(self):
        out = Cell()
        for x in range(H):
            for y in range(H):
                p = self.get(x, y)
                if p[3] > 0:
                    out.put(H - 1 - x, y, p)
        return out


def expand(table):
    """[(y, x0, x1), ...] -> set of (x, y)."""
    out = set()
    for y, x0, x1 in table:
        for x in range(x0, x1 + 1):
            out.add((x, y))
    return out


def contour(cell, mass, color, skip=()):
    """Dark contour on the OUTER perimeter only.

    `skip` holds cut-out cells (a hood's face opening); vanilla rims a face
    opening with the garment's inner shade, never with the contour colour.
    """
    skip = set(skip)
    edge = []
    for x, y in mass:
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if (nx, ny) in mass or (nx, ny) in skip:
                continue
            edge.append((x, y))
            break
    for x, y in edge:
        cell.put(x, y, color)


def plane_shade(cell, mass, ox, oy, lit_at, deep_at, slope=1.3):
    """Flat top-left lighting: three planes, hard boundaries, no gradient."""
    for x, y in mass:
        d = (x - ox) + (y - oy) * slope
        cell.put(x, y, LIT if d < lit_at else (DEEP if d > deep_at else BASE))


def paint(cell, mass, table, color):
    for x, y in expand(table):
        if (x, y) in mass:
            cell.put(x, y, color)


def sheet(rows, cols=7):
    img = Image.new("RGBA", (FULL * cols, FULL * len(rows)), (0, 0, 0, 0))
    for r, row in enumerate(rows):
        for c, cell in enumerate(row):
            if cell is not None:
                img.alpha_composite(cell.image(), (c * FULL, r * FULL))
    return img


def bob_row(base):
    """Vanilla helmet / chest-front repeat: [0, 0^, 0, 0^, 0, 0, 0]."""
    up = base.shifted(-1)
    return [base, up, base, up, base, base, base]


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


# ===========================================================================
# 1. Skywatch Hood  ->  player/armor/skywatchhood.png   448x256 (7 x 4)
# ===========================================================================
#
# Built on the vanilla cowl construction measured off leatherhood - closed
# dome for the back view, horseshoe for the front, asymmetric crescent for the
# sides, face opening sized so the eye strip (x12..19, y14..17) sits clear -
# but one pixel deeper, with lappets that hang past the jaw beside the beard.
# The pale weathered lining ringing the opening is what now carries the
# grey-white note the old mob sprite got from its hair mass.

HOOD_DOWN = [
    (4, 13, 18), (5, 11, 20), (6, 10, 21), (7, 10, 21), (8, 9, 22),
    (9, 9, 22), (10, 8, 23), (11, 8, 23), (12, 8, 23), (13, 8, 23),
    (14, 8, 23), (15, 8, 23), (16, 8, 23), (17, 8, 22),
    (18, 9, 22), (19, 9, 21), (20, 10, 20),
]
HOLE_DOWN = [
    (12, 15, 16), (13, 13, 18), (14, 11, 20), (15, 11, 20), (16, 11, 20),
    (17, 11, 20), (18, 12, 19), (19, 13, 18), (20, 14, 17),
]

HOOD_UP = HOOD_DOWN
HOOD_RIGHT = HOOD_DOWN + [(21, 9, 13), (22, 10, 12)]
HOLE_RIGHT = [
    (12, 17, 18), (13, 15, 19), (14, 13, 20), (15, 13, 20), (16, 13, 20),
    (17, 13, 20), (18, 14, 20), (19, 15, 19), (20, 16, 18),
]


def _hood_cell(direction):
    cell = Cell()
    dome = {"up": HOOD_UP, "down": HOOD_DOWN, "right": HOOD_RIGHT}[direction]
    hole = {"up": [], "down": HOLE_DOWN, "right": HOLE_RIGHT}[direction]
    holes = expand(hole)
    mass = expand(dome) - holes

    plane_shade(cell, mass, 15.0, 12.0, lit_at=-6.0, deep_at=7.0)

    # crown highlight: one controlled cluster, top-left, never a gradient
    paint(cell, mass, ((6, 12, 14), (7, 11, 14), (8, 11, 13), (9, 10, 12)), HI)

    # weathered cloth creases, drawn before the contour so they cannot be eaten
    creases = {
        "up": ((11, 15, 16), (12, 15, 16), (13, 15, 16), (14, 15, 16),
               (15, 15, 16), (16, 15, 16), (13, 11, 11), (14, 11, 11),
               (13, 20, 20), (14, 20, 20)),
        "down": ((9, 20, 21), (10, 20, 21), (11, 21, 22)),
        "right": ((9, 20, 21), (10, 20, 21), (12, 10, 10), (13, 10, 10),
                  (14, 10, 10), (15, 10, 10), (16, 10, 10), (17, 10, 10)),
    }[direction]
    paint(cell, mass, creases, DEEP)

    # nape shadow so the back view is not a flat disc
    if direction == "up":
        paint(cell, mass, ((17, 9, 22), (18, 10, 21), (19, 10, 20)), DEEP)
        paint(cell, mass, ((20, 11, 19),), OUT)

    # the single warm accent: a small Skywatch plaque pinned to the brow
    plaque = {"up": ((10, 14, 17), (11, 15, 16)),
              "down": ((10, 14, 17), (11, 15, 16)),
              "right": ((10, 12, 15), (11, 13, 14))}[direction]
    paint(cell, mass, plaque, GOLD)
    paint(cell, mass, {"up": ((10, 15, 16),), "down": ((10, 15, 16),),
                       "right": ((10, 13, 14),)}[direction], GOLD_H)

    contour(cell, mass, OUT, skip=holes)

    # pale weathered lining ringing the face opening: shadow under the brow,
    # bright down the lit side, mid on the shaded side
    for x, y in sorted(holes):
        for nx, ny in ((x, y - 1), (x, y + 1), (x - 1, y), (x + 1, y)):
            if (nx, ny) in holes or (nx, ny) not in mass:
                continue
            if ny < y:
                cell.put(nx, ny, DEEP)
            elif ny > y:
                cell.put(nx, ny, PALE_D)
            elif nx < x:
                cell.put(nx, ny, PALE_L if direction == "down" else PALE)
            else:
                cell.put(nx, ny, PALE if direction == "down" else PALE_L)
    return cell


def gen_skywatchhood(path):
    right = _hood_cell("right")
    save(sheet([bob_row(_hood_cell("up")),
                bob_row(right),
                bob_row(_hood_cell("down")),
                bob_row(right.mirrored())]), path)


# ===========================================================================
# 2. Warden Mantle  ->  player/armor/wardenmantle.png   448x320 (7 x 5)
# ===========================================================================
#
# A shoulder mantle over a long weathered robe.  Proportions follow magerobe:
# shoulder flare only on one or two rows (x10..21), then a narrow skirt
# (x12..19) hanging below the body, shorter at the front than at the back and
# sides so the boots still read head-on.  Skirt volume comes from alternating
# dark fold stripes, the way vanilla does it, not from extra colours.

# Rows are painted explicitly, one colour code per pixel, the way vanilla robe
# skirts are built: alternating dark fold stripes over a cloth field, light
# from the left, no outline along the hem (magerobe's bottom row is
# "A B B A A B B A", not a dark bar).  Codes: A outline, B deep, C base,
# D lit, p/P/Q pale ramp, g/G gold.
CODES = {"A": OUT, "B": DEEP, "C": BASE, "D": LIT, "E": HI,
         "p": PALE_D, "P": PALE, "Q": PALE_L, "g": GOLD, "G": GOLD_H}

MANTLE_ROWS = {
    "down": [
        (20, 11, "AAD"), (20, 18, "BAA"),
        (21, 10, "APDCCCCCCBBA"),
        (22, 10, "ApDCCCCCCCBA"),
        (23, 11, "AgCgCCgCgA"),
        (24, 11, "ADCACCACBA"),
        (25, 11, "ADCACCACBA"),
        (26, 11, "ADCACCACBA"),
        (27, 12, "ACACCACA"),
    ],
    "up": [
        (19, 11, "AA"), (19, 19, "AA"),
        (20, 10, "APDCCCCCCBBA"),
        (21, 10, "ApDCCCCCCCBA"),
        (22, 10, "ACDCCCCCCCBA"),
        (23, 11, "AgCgCCgCgA"),
        (24, 11, "ADCACCACBA"),
        (25, 11, "ADCACCACBA"),
        (26, 11, "ADCACCACBA"),
        (27, 11, "ADCACCACBA"),
        (28, 12, "ACACCACA"),
    ],
    "right": [
        (20, 12, "AAD"), (20, 19, "AA"),
        (21, 10, "APDCCCCCCCBA"),
        (22, 10, "ApDCCCCCCBA"),
        (23, 11, "AgCgCCgCgA"),
        (24, 11, "ADCACCACBA"),
        (25, 11, "ADCACCACB"),
        (26, 11, "ADCACCACB"),
        (27, 11, "ADCACCACB"),
        (28, 12, "ACACCAC"),
    ],
}

SKIRT_TOP = 25          # rows at or below this sway with the walk cycle


def _mantle_cell(direction, sway=0, downed=False):
    """Paint one mantle frame.

    `sway` slides the skirt one pixel left or right, which is the whole of the
    per-frame variation vanilla chest sheets carry on their side rows; `downed`
    is the column-6 pose, a shortened hem like eldershirt's.
    """
    cell = Cell()
    hem = max(y for y, _, _ in MANTLE_ROWS[direction])
    cut = hem if not downed else hem - 2
    for y, x0, pattern in MANTLE_ROWS[direction]:
        if y > cut:
            continue
        ox = x0 + (sway if y >= SKIRT_TOP else 0)
        for i, code in enumerate(pattern):
            cell.put(ox + i, y, CODES[code])
    if downed:
        # close the shortened hem so it does not read as a cut-off robe
        row = [r for r in MANTLE_ROWS[direction] if r[0] == cut][0]
        y, x0, pattern = row
        for i in range(1, len(pattern) - 1):
            if i % 3 == 1:
                cell.put(x0 + i, y, DEEP)
    return cell


def _mantle_attack_row():
    """Row 4: the two 32px sleeve sprites the attack animation reads.

    `getAttackArmSprite` -> GameSprite(texture, 0, 8, 32) = half-res x0..15 /
    y0..15 of this row; the bare arm it covers occupies half-res x4..10, y6..9
    inside that block.  Sprite (1,8) sits over the back of the hand at
    half-res x21..27, y5..12.
    """
    a = Cell()
    sleeve = expand(((6, 7, 10), (7, 6, 10), (8, 6, 10), (9, 7, 10)))
    for x, y in sleeve:
        a.put(x, y, LIT if y <= 6 else (DEEP if y >= 9 else BASE))
    a.put(6, 7, PALE_D)
    a.put(10, 8, GOLD)
    contour(a, sleeve, OUT)

    b = Cell()
    cuff = expand(((5, 7, 9), (6, 7, 10), (7, 7, 10), (8, 6, 10)))
    for x, y in cuff:
        b.put(x, y, LIT if y <= 5 else (DEEP if y >= 8 else BASE))
    contour(b, cuff, OUT)
    return [a, b, None, None, None, None, None]


def gen_wardenmantle(path):
    side = [
        _mantle_cell("right", sway=0),
        _mantle_cell("right", sway=1).shifted(-1),
        _mantle_cell("right", sway=1),
        _mantle_cell("right", sway=-1).shifted(-1),
        _mantle_cell("right", sway=-1),
    ]
    downed = _mantle_cell("right", sway=0, downed=True)
    row_right = [side[0], side[1], side[2], side[3], side[4], side[4], downed]
    save(sheet([bob_row(_mantle_cell("up")),
                row_right,
                bob_row(_mantle_cell("down")),
                [c.mirrored() for c in row_right],
                _mantle_attack_row()]), path)


# ===========================================================================
# 3. Warden Mantle back  ->  player/armor/wardenmantle_back.png  448x320
# ===========================================================================
#
# `ArmorItem.loadArmorTexture` auto-loads "<texture>_back" and the combiner
# draws it before feet and body, so this is the cloak trailing BEHIND.  Vanilla
# robe backs are a single flat dark silhouette (magerobe_back is literally one
# colour); cryowitchrobe_back proves a full cloak in the side rows is
# legitimate, so the Warden gets one - his cloak is half his identity.

CLOAK_DOWN = [
    (22, 11, 20), (23, 10, 21), (24, 9, 22), (25, 9, 22),
    (26, 9, 22), (27, 9, 22), (28, 10, 21), (29, 12, 19),
]
CLOAK_SIDE = [
    (20, 10, 13), (21, 8, 13), (22, 7, 13), (23, 7, 14),
    (24, 7, 14), (25, 7, 14), (26, 7, 14), (27, 8, 14), (28, 9, 13),
]


def _cloak_cell(table, sway=0, downed=False):
    cell = Cell()
    rows = []
    for y, x0, x1 in table:
        if downed and y >= 27:
            continue
        if sway and y >= 25:
            x0 += sway
            x1 += sway
        rows.append((y, x0, x1))
    mass = expand(rows)
    for x, y in mass:
        cell.put(x, y, DEEP)
    # edge light down the lit side, and creases so the cloak is not a slab
    lo = min(x for x, _ in mass)
    for x, y in mass:
        if x <= lo + 1 and y >= 22:
            cell.put(x, y, BASE)
    paint(cell, mass, tuple((y, x, x) for y in range(24, 29) for x in (13, 18)), OUT)
    contour(cell, mass, OUT)
    return cell


def gen_wardenmantle_back(path):
    side = [
        _cloak_cell(CLOAK_SIDE, sway=0),
        _cloak_cell(CLOAK_SIDE, sway=1).shifted(-1),
        _cloak_cell(CLOAK_SIDE, sway=1),
        _cloak_cell(CLOAK_SIDE, sway=-1).shifted(-1),
        _cloak_cell(CLOAK_SIDE, sway=-1),
    ]
    downed = _cloak_cell(CLOAK_SIDE, sway=0, downed=True)
    row_right = [side[0], side[1], side[2], side[3], side[4], side[4], downed]
    empty = [None] * 7
    save(sheet([empty, row_right, bob_row(_cloak_cell(CLOAK_DOWN)),
                [c.mirrored() for c in row_right], empty]), path)


# ===========================================================================
# 4. Mantle sleeves  ->  wardenmantlearms_left.png / _right.png   448x320
# ===========================================================================
#
# Sleeve caps sitting on the shoulder end of the arm.  Every span below is the
# measured skin/arms_* outline for that (row, column), widened one pixel each
# side - a mantle sleeve is looser than the arm inside it.  Column pattern is
# the vanilla arms pattern [0, 1, 0, 3, 0, 0, 6]; where the bare arm is hidden
# (arms_left row 1 col 1, arms_right row 3 col 1) the sleeve is empty too.
# No contour ring: vanilla arm caps are three pixels of light-to-dark ramp.

SLEEVE_LEFT = {
    0: [((20, 10, 13), (21, 9, 13), (22, 9, 11)),
        ((19, 10, 13), (20, 10, 14), (21, 10, 13)),
        ((19, 10, 13), (20, 9, 13), (21, 10, 11)),
        ((20, 10, 13), (21, 9, 13), (22, 10, 11))],
    1: [((21, 18, 21), (22, 19, 22), (23, 20, 21)),
        None,
        ((20, 18, 22), (21, 19, 23), (22, 20, 22)),
        ((21, 18, 22), (22, 19, 22))],
    2: [((20, 18, 21), (21, 19, 22), (22, 20, 22)),
        ((19, 18, 21), (20, 18, 21), (21, 19, 22)),
        ((19, 18, 22), (20, 17, 22), (21, 17, 21)),
        ((20, 18, 22), (21, 17, 22), (22, 18, 21))],
    3: [((21, 17, 20), (22, 16, 21), (23, 17, 20)),
        ((20, 14, 20), (21, 13, 20), (22, 14, 18)),
        ((20, 17, 20), (21, 17, 21), (22, 18, 21)),
        ((21, 17, 20), (22, 15, 21), (23, 16, 20))],
}
SLEEVE_RIGHT = {
    0: [((20, 18, 21), (21, 18, 22), (22, 20, 22)),
        ((19, 18, 21), (20, 18, 22), (21, 19, 22)),
        ((19, 18, 21), (20, 17, 21), (21, 18, 21)),
        ((20, 18, 21), (21, 18, 22), (22, 20, 21))],
    1: [((21, 11, 14), (22, 10, 15), (23, 11, 14)),
        ((20, 11, 17), (21, 11, 18), (22, 13, 17)),
        ((20, 11, 14), (21, 10, 14), (22, 10, 13)),
        ((21, 11, 14), (22, 10, 16), (23, 11, 16))],
    2: [((20, 10, 13), (21, 9, 12), (22, 9, 11)),
        ((19, 10, 13), (20, 9, 14), (21, 10, 14)),
        ((19, 10, 13), (20, 10, 13), (21, 9, 12)),
        ((20, 10, 13), (21, 9, 14), (22, 10, 14))],
    3: [((21, 10, 13), (22, 9, 12), (23, 10, 11)),
        None,
        ((20, 9, 13), (21, 8, 12), (22, 9, 11)),
        ((21, 9, 13), (22, 8, 12), (23, 9, 10))],
}


def _sleeve_cell(table, outward):
    """outward: -1 when the arm hangs to the left of the shoulder, +1 right."""
    if table is None:
        return None
    cell = Cell()
    mass = expand(table)
    top = min(y for y, _, _ in table)
    bottom = max(y for y, _, _ in table)
    lo = min(x0 for _, x0, _ in table)
    hi = max(x1 for _, _, x1 in table)
    for x, y in mass:
        near = (x - lo) if outward > 0 else (hi - x)
        if y == top and near <= 1:
            c = LIT
        elif y == bottom or near >= (hi - lo) - 1:
            c = DEEP
        else:
            c = BASE
        cell.put(x, y, c)
    # pale mantle edge catching the light, one pixel, and a gold cuff stud
    cell.put(lo if outward > 0 else hi, top, PALE)
    cell.put(hi if outward > 0 else lo, bottom, GOLD)
    # the outermost pixel of the last row reads as contour, like vanilla
    for x, y in mass:
        if y == bottom and (x == lo or x == hi):
            cell.put(x, y, OUT)
    return cell


def _arms_sheet(table, outward):
    rows = []
    for r in range(4):
        c0, c1, c3, c6 = [_sleeve_cell(t, outward) for t in table[r]]
        rows.append([c0, c1, c0, c3, c0, c0, c6])
    rows.append([None] * 7)
    return sheet(rows)


def gen_wardenmantlearms(path_left, path_right):
    save(_arms_sheet(SLEEVE_LEFT, +1), path_left)
    save(_arms_sheet(SLEEVE_RIGHT, -1), path_right)


# ===========================================================================
# 5. Warden Boots  ->  player/armor/wardenboots.png   448x320
# ===========================================================================
#
# Vanilla shoes are the foot silhouette raised one half-res pixel to swallow
# the ankle, carrying the whole [0,1,2,3,4,0,6] step cycle because the feet
# actually move.  The tables are the measured foot outlines per (row, column);
# the boot drawn inside them is ours - dark storm-leather with a pale scuffed
# shaft, a gold buckle and a lit sole, built like eldershoes with a dark seam
# splitting the two boots.

FOOT_UP = {
    0: [(24, 12, 19), (25, 12, 19), (26, 12, 19), (27, 11, 20)],
    1: [(23, 12, 19), (24, 11, 19), (25, 15, 18), (26, 15, 18), (27, 15, 19)],
    2: [(24, 12, 19), (25, 13, 19), (26, 12, 18), (27, 15, 18), (28, 15, 19)],
    3: [(23, 12, 19), (24, 12, 20), (25, 13, 16), (26, 13, 16), (27, 12, 16)],
    4: [(24, 12, 19), (25, 12, 18), (26, 13, 19), (27, 13, 16), (28, 12, 16)],
    6: [(24, 12, 19), (25, 13, 18)],
}
FOOT_RIGHT = {
    0: [(24, 13, 19), (25, 13, 19), (26, 13, 19), (27, 13, 20), (28, 13, 17)],
    1: [(23, 13, 20), (24, 13, 20), (25, 12, 21), (26, 12, 15), (27, 12, 15),
        (28, 12, 16)],
    2: [(24, 13, 19), (25, 14, 20), (26, 13, 20), (27, 13, 20), (28, 17, 21)],
    3: [(23, 13, 19), (24, 14, 19), (25, 15, 18), (26, 15, 19), (27, 15, 18),
        (28, 15, 19)],
    4: [(24, 13, 20), (25, 13, 19), (26, 14, 20), (27, 14, 17), (28, 14, 18)],
    6: [(23, 20, 20), (24, 13, 21), (25, 14, 21), (26, 15, 22), (27, 15, 19)],
}
FOOT_DOWN = {
    0: [(24, 12, 19), (25, 12, 19), (26, 12, 19), (27, 11, 20)],
    1: [(23, 12, 19), (24, 13, 19), (25, 14, 19), (26, 13, 19), (27, 16, 20)],
    2: [(24, 12, 19), (25, 13, 19), (26, 15, 18), (27, 15, 18), (28, 15, 19)],
    3: [(23, 12, 19), (24, 12, 18), (25, 12, 17), (26, 12, 18), (27, 11, 15)],
    4: [(24, 12, 19), (25, 12, 18), (26, 13, 16), (27, 13, 16), (28, 12, 16)],
    6: [(24, 11, 20), (25, 12, 19), (26, 11, 20)],
}


def _boot_cell(spans):
    cell = Cell()
    top = min(y for y, _, _ in spans)
    bottom = max(y for y, _, _ in spans)
    rows = list(spans)
    # the boot shaft rises one half-res pixel over the ankle, like vanilla shoes
    y0, x0, x1 = spans[0]
    rows.insert(0, (top - 1, x0, x1))
    mass = expand(rows)
    shaft = top - 1
    for x, y in mass:
        cell.put(x, y, DEEP if y <= shaft + 1 else BASE)
    # pale scuffed shaft highlight and a gold buckle on each boot
    for y, a, b in rows[:1]:
        for x in range(a, b + 1):
            if (x - a) % 4 in (1, 2):
                cell.put(x, y, PALE_D)
    for x in (13, 18):
        if (x, shaft + 1) in mass:
            cell.put(x, shaft + 1, GOLD)
    # dark seam between the two boots, and a lit sole
    for x, y in mass:
        if x in (15, 16) and y > shaft + 1:
            cell.put(x, y, OUT)
        elif y == bottom:
            cell.put(x, y, LIT)
    contour(cell, mass, OUT)
    return cell


def gen_wardenboots(path):
    def row(table):
        c = {k: _boot_cell(v) for k, v in table.items()}
        return [c[0], c[1], c[2], c[3], c[4], c[0], c[6]]

    row_right = row(FOOT_RIGHT)
    save(sheet([row(FOOT_UP), row_right, row(FOOT_DOWN),
                [c.mirrored() for c in row_right], [None] * 7]), path)


# ===========================================================================
# 6. Item icons  ->  items/{skywatchhood,wardenmantle,wardenboots}.png  32x32
# ===========================================================================
#
# Vanilla armor icons are full-resolution 32px drawings (NOT the half-res sheet
# art), 5-7 colours, filling nearly the whole tile: elderhat 560 opaque px,
# eldershirt 568, leatherhood 504, magerobe 552.  These match that mass so they
# do not read small in the inventory grid.


class Icon:
    def __init__(self):
        self.c = px.Canvas(32, 32)

    def fill(self, mass, ox, oy, lit_at, deep_at, slope=1.2):
        for x, y in mass:
            d = (x - ox) + (y - oy) * slope
            self.c.put(x, y, LIT if d < lit_at else (DEEP if d > deep_at else BASE))

    def paint(self, mass, table, color):
        for x, y in expand(table):
            if (x, y) in mass:
                self.c.put(x, y, color)

    def contour(self, mass, skip=()):
        skip = set(skip)
        edge = []
        for x, y in mass:
            for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if (nx, ny) in mass or (nx, ny) in skip:
                    continue
                edge.append((x, y))
                break
        for x, y in edge:
            self.c.put(x, y, OUT)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.c.save(path)


def gen_skywatchhood_icon(path):
    ic = Icon()
    dome = [
        (1, 12, 19), (2, 10, 21), (3, 8, 23), (4, 7, 24), (5, 6, 25),
        (6, 5, 26), (7, 4, 27), (8, 4, 27), (9, 3, 28), (10, 3, 28),
        (11, 2, 29), (12, 2, 29), (13, 2, 29), (14, 2, 29), (15, 2, 29),
        (16, 2, 29), (17, 2, 29), (18, 2, 29), (19, 2, 29), (20, 3, 28),
        (21, 3, 28), (22, 4, 27), (23, 5, 26), (24, 6, 25), (25, 7, 24),
        (26, 9, 22), (27, 11, 20), (28, 13, 18),
    ]
    hole = [
        (13, 15, 16), (14, 13, 18), (15, 11, 20), (16, 10, 21), (17, 9, 22),
        (18, 9, 22), (19, 9, 22), (20, 9, 22), (21, 10, 21), (22, 11, 20),
        (23, 12, 19), (24, 13, 18), (25, 14, 17), (26, 15, 16),
    ]
    holes = expand(hole)
    mass = expand(dome) - holes
    ic.fill(mass, 14.0, 13.0, lit_at=-9.0, deep_at=9.0, slope=1.3)
    ic.paint(mass, ((3, 11, 17), (4, 9, 16), (5, 8, 14), (6, 7, 12), (7, 7, 10)), HI)
    ic.paint(mass, ((9, 25, 27), (10, 26, 28), (11, 26, 28), (12, 27, 28),
                    (19, 26, 28), (20, 25, 27), (2, 15, 17)), DEEP)
    # gold Skywatch plaque on the brow
    ic.paint(mass, ((9, 12, 19), (10, 13, 18), (11, 14, 17)), GOLD)
    ic.paint(mass, ((9, 14, 17), (10, 15, 16)), GOLD_H)
    ic.contour(mass, skip=holes)
    for x, y in sorted(holes):
        for nx, ny in ((x, y - 1), (x, y + 1), (x - 1, y), (x + 1, y)):
            if (nx, ny) in holes or (nx, ny) not in mass:
                continue
            if ny < y:
                ic.c.put(nx, ny, DEEP)
            elif ny > y:
                ic.c.put(nx, ny, PALE_D)
            elif nx < x:
                ic.c.put(nx, ny, PALE_L)
            else:
                ic.c.put(nx, ny, PALE)
    ic.save(path)


def gen_wardenmantle_icon(path):
    ic = Icon()
    body = [
        (1, 12, 19), (2, 11, 20), (3, 10, 21),
        (4, 6, 25), (5, 4, 27), (6, 3, 28), (7, 2, 29), (8, 2, 29),
        (9, 2, 29), (10, 2, 29), (11, 3, 28), (12, 4, 27), (13, 5, 26),
        (14, 6, 25), (15, 7, 24), (16, 7, 24),
        (17, 6, 25), (18, 6, 25), (19, 5, 26), (20, 5, 26), (21, 4, 27),
        (22, 4, 27), (23, 3, 28), (24, 3, 28), (25, 3, 28), (26, 3, 28),
        (27, 4, 27), (28, 5, 26), (29, 8, 23),
    ]
    mass = expand(body)
    ic.fill(mass, 15.0, 15.0, lit_at=-10.0, deep_at=10.0, slope=1.0)
    # pale weathered collar
    ic.paint(mass, ((1, 12, 19), (2, 11, 20)), PALE)
    ic.paint(mass, ((3, 11, 20),), PALE_D)
    # mantle cape edge with its gold trim line
    ic.paint(mass, ((14, 6, 25),), GOLD)
    ic.paint(mass, ((15, 7, 24),), DEEP)
    # robe folds and the pale centre placket
    for y in range(16, 29):
        ic.paint(mass, ((y, 9, 9), (y, 21, 21)), OUT)
        ic.paint(mass, ((y, 15, 16),), PALE_D if y % 2 else PALE)
    for x in range(3, 29):
        if (x, 28) in mass and x % 4 == 0:
            ic.c.put(x, 28, GOLD)
    ic.paint(mass, ((11, 5, 5), (11, 26, 26)), GOLD_H)
    ic.contour(mass)
    ic.save(path)


def gen_wardenboots_icon(path):
    ic = Icon()
    boot = []
    for y in range(3, 23):
        boot.append((y, 3, 13))
        boot.append((y, 18, 28))
    for y in range(23, 29):
        boot.append((y, 2, 14))
        boot.append((y, 17, 29))
    mass = expand(boot)
    for x, y in mass:
        left = x < 16
        anchor = 6 if left else 21
        d = (x - anchor) + (y - 14.0) * 0.55
        ic.c.put(x, y, LIT if d < -3.5 else (DEEP if d > 5.0 else BASE))
    # pale weathered cuff at the top of each shaft
    ic.paint(mass, ((3, 3, 13), (3, 18, 28)), PALE)
    ic.paint(mass, ((4, 3, 13), (4, 18, 28)), PALE_D)
    # gold buckle strap
    ic.paint(mass, ((12, 3, 13), (12, 18, 28)), GOLD)
    ic.paint(mass, ((13, 3, 13), (13, 18, 28)), DEEP)
    ic.paint(mass, ((12, 5, 6), (12, 20, 21)), GOLD_H)
    # sole
    ic.paint(mass, ((27, 2, 14), (27, 17, 29), (28, 2, 14), (28, 17, 29)), DEEP)
    ic.contour(mass)
    ic.save(path)


# ===========================================================================

def gen_all(res_root):
    armor = os.path.join(res_root, "player", "armor")
    items = os.path.join(res_root, "items")
    os.makedirs(armor, exist_ok=True)
    os.makedirs(items, exist_ok=True)
    gen_skywatchhood(os.path.join(armor, "skywatchhood.png"))
    gen_wardenmantle(os.path.join(armor, "wardenmantle.png"))
    gen_wardenmantle_back(os.path.join(armor, "wardenmantle_back.png"))
    gen_wardenmantlearms(os.path.join(armor, "wardenmantlearms_left.png"),
                         os.path.join(armor, "wardenmantlearms_right.png"))
    gen_wardenboots(os.path.join(armor, "wardenboots.png"))
    gen_skywatchhood_icon(os.path.join(items, "skywatchhood.png"))
    gen_wardenmantle_icon(os.path.join(items, "wardenmantle.png"))
    gen_wardenboots_icon(os.path.join(items, "wardenboots.png"))
