"""Skyreach gear: the Stormsteel plate set and the three sky accessories.

WHAT THIS DRAWS
---------------
* player/armor/stormsteelhelmet.png      448x256  (7 cols x 4 rows)
* player/armor/stormsteelchest.png       448x320  (7 x 5; row 4 is the attack
                                                   sleeve, addressed at 32px)
* player/armor/stormsteelarms_left.png   448x256
* player/armor/stormsteelarms_right.png  448x256
* player/armor/stormsteelboots.png       448x256
* items/{stormsteelhelmet,stormsteelchestplate,stormsteelboots}.png   32x32
* items/{stormsteelvambrace,auroralocket,zephyrharness}.png           32x32

FORMAT
------
The sheet format is the one `gen_armor` documents and implements, and this
module imports its machinery rather than restating it: 64px cells, row = facing
(up / right / down / left), column = animation frame
(idle, walk x4, in-liquid, downed), all art authored at 32x32 half-res and
upscaled 2x with NEAREST because every 2x2 block in vanilla's player art is
uniform. `gen_armor` also holds the MEASURED human anatomy — the per-frame foot
outlines and the per-frame arm-cap spans — which is body geometry, not Warden
geometry, so the boots and the pauldrons here stand on exactly the same bones.

GEOMETRY, measured off the vanilla dump before drawing
------------------------------------------------------
Every span table below was read out of `player/armor/tungsten*.png` at half-res
(the tier this set is calibrated against), cell (col 0, row r):

    helmet down  y4..20,  widest x8..23 at y14-15
    helmet up    y4..21,  same dome, closed at the neck
    helmet right y5..20,  widest x8..24 at y12-13 (the cheek plate juts)
    chest  down  y18..25, shoulders x9..21, waist x12..19, hem x15..16
    chest  up    y17..25
    chest  right y19..25
    boots  down  y23..27, x12..19

The tungsten helmet is a CLOSED great helm — no face opening at all — which is
why this one is too, and why it takes `HairDrawMode.NO_HAIR`.

The attack row: `ChestArmorItem.getAttackArmSprite` returns
`new GameSprite(armorTexture, 0, 8, 32)`, and `GameSprite(texture, x, y, res)`
addresses a 32px grid, so that is the pixel block (0,256)-(32,288) — the TOP-LEFT
QUARTER of cell (0, row 4), i.e. half-res x0..15, y0..15. Vanilla puts the
sleeve at half-res x8..11 / y7..11 and a second cluster at x23..27 / y4..7,
both inside the FIRST 64px cell. This module copies that placement.

SIZE
----
`tools/size_audit.py` carries a row for each file here, measured against the
vanilla tungsten piece it answers to and against the vanilla trinket icon each
accessory answers to.
"""

import os

import palette
import px
import gen_armor
from gen_professions import GLASS, STORMSTEEL

# --- palette ---------------------------------------------------------------
# Stormsteel is the ramp `gen_professions` already derived for the bar the
# armour is forged from (iron pulled toward storm-shard violet), imported so a
# breastplate can never drift away from the ingot in the player's other hand.
# The warm accent is the same stormglass the set is glazed with.
OUT = palette.OUTLINE
DEEP = STORMSTEEL["deep"]
BASE = STORMSTEEL["base"]
LIT = STORMSTEEL["light"]
HI = STORMSTEEL["hi"]
# Vanilla's tungsten helmet spans luminance 38 (its most common colour, 37% of
# the sheet) up to 187 — a DARK piece with bright highlights. The stormsteel
# ramp on its own only spans 65..193, so the plate would read as one flat mid
# purple. SHADE is the missing bottom step, and `plate`/`Icon.fill` below are
# four-plane because of it.
SHADE = px.mix(STORMSTEEL["deep"], palette.OUTLINE, 0.55)
GLOW_D = GLASS["deep"]
GLOW = GLASS["base"]
GLOW_L = GLASS["light"]
GLOW_H = GLASS["hi"]

# Aurora Locket / Zephyr Harness accents, from the ramps their materials
# already use elsewhere in the mod.
AUR = palette.AURORA
SILK = palette.WINDSILK

Cell = gen_armor.Cell
expand = gen_armor.expand
contour = gen_armor.contour
sheet = gen_armor.sheet
bob_row = gen_armor.bob_row
save = gen_armor.save


def plate(cell, mass, ox, oy, lit_at, deep_at, slope=1.3):
    """Flat top-left lighting: four planes, hard boundaries, no gradient."""
    for x, y in mass:
        d = (x - ox) + (y - oy) * slope
        if d < lit_at - 4.0:
            c = HI
        elif d < lit_at:
            c = LIT
        elif d > deep_at + 4.0:
            c = SHADE
        elif d > deep_at:
            c = DEEP
        else:
            c = BASE
        cell.put(x, y, c)


def paint(cell, mass, table, color):
    for x, y in expand(table):
        if (x, y) in mass:
            cell.put(x, y, color)


# ===========================================================================
# 1. Stormsteel Helm  ->  player/armor/stormsteelhelmet.png   448x256
# ===========================================================================

HELM_DOWN = [
    (4, 14, 17), (5, 13, 18), (6, 13, 18), (7, 12, 19), (8, 11, 20),
    (9, 10, 21), (10, 10, 21), (11, 10, 21), (12, 10, 21), (13, 9, 22),
    (14, 8, 23), (15, 8, 23), (16, 9, 22), (17, 10, 21), (18, 11, 20),
    (19, 12, 19), (20, 13, 18),
]
HELM_UP = HELM_DOWN + [(21, 14, 17)]
HELM_RIGHT = [
    (5, 13, 18), (6, 11, 20), (7, 10, 21), (8, 9, 22), (9, 9, 22),
    (10, 8, 21), (11, 8, 23), (12, 8, 24), (13, 8, 24), (14, 8, 23),
    (15, 8, 22), (16, 9, 21), (17, 10, 21), (18, 9, 21), (19, 9, 20),
    (20, 10, 19),
]

# The visor slit, per facing. Nothing on the back of the head.
VISOR_DOWN = ((13, 11, 20), (14, 10, 21))
VISOR_RIGHT = ((12, 14, 22), (13, 15, 23))


def _helm_cell(direction):
    """One helmet frame.

    Composed as four horizontal zones rather than as a shaded dome, because a
    shaded dome is what a first pass produced and on a composited body it read
    as a purple balloon with a gold stripe. Vanilla's own tungsten helm gets
    its silhouette from CONTRAST, not from outline: a dark crown, a dark brow
    band, a BRIGHT face plate and a dark chin, which is what makes the head
    read as armour and not as a hood. The zones are:

        crown  (top ~55%)  BASE, one HI cluster top-left, SHADE down the right
        brow                SHADE with rivets
        visor               SHADE, with stormglass glints
        face plate          LIT — the bright band that gives the helm its face
        chin / neck         SHADE, so the helm sits ON the shoulders
    """
    cell = Cell()
    table = {"up": HELM_UP, "down": HELM_DOWN, "right": HELM_RIGHT}[direction]
    mass = expand(table)
    plate(cell, mass, 14.0, 11.0, lit_at=-6.0, deep_at=7.0)

    # crown ridge: one controlled cluster top-left, the way vanilla lights a
    # rounded metal dome, never a gradient
    if direction == "right":
        paint(cell, mass, ((6, 12, 17), (7, 11, 15), (8, 10, 13), (9, 10, 12)), HI)
    else:
        paint(cell, mass, ((5, 13, 16), (6, 13, 16), (7, 12, 15), (8, 11, 14)), HI)

    if direction == "up":
        # the back of the head: no face, so a plain occipital plate and a seam
        paint(cell, mass, ((14, 0, 31),), DEEP)
        for y in range(15, 20):
            paint(cell, mass, ((y, 15, 16),), DEEP)
    else:
        brow = {"down": 12, "right": 11}[direction]
        paint(cell, mass, ((brow, 0, 31),), SHADE)
        for x in range(0, 32, 3):
            if (x, brow) in mass:
                cell.put(x, brow, LIT)
        slit = {"down": VISOR_DOWN, "right": VISOR_RIGHT}[direction]
        paint(cell, mass, slit, SHADE)
        row = slit[0]
        # ONE small glint, off centre. A repeating row of glints across the
        # slit reads as a row of TEETH once the helm is composited onto a body,
        # and the curved slot pattern that used to sit under it completed the
        # grin. Both are gone; the face plate gets a vertical nasal instead,
        # which is what a real great helm has there.
        if (row[1] + 2, row[0]) in mass:
            cell.put(row[1] + 2, row[0], GLOW_L)
        if (row[1] + 3, row[0]) in mass:
            cell.put(row[1] + 3, row[0], GLOW_D)
        face_top = row[0] + 2
        for y in range(face_top, face_top + 4):
            paint(cell, mass, ((y, 0, 31),), LIT if y < face_top + 3 else BASE)
        nasal = 16 if direction == "down" else 18
        for y in range(face_top, face_top + 4):
            paint(cell, mass, ((y, nasal - 1, nasal),), DEEP)
            paint(cell, mass, ((y, nasal - 2, nasal - 2),), HI)

    # neck guard shadow so the helm sits ON the shoulders
    hem = max(y for y, _, _ in table)
    paint(cell, mass, ((hem, 0, 31),), SHADE)
    paint(cell, mass, ((hem - 1, 0, 31),), DEEP)
    contour(cell, mass, OUT)
    return cell


def gen_stormsteelhelmet(path):
    row_right = bob_row(_helm_cell("right"))
    save(sheet([bob_row(_helm_cell("up")),
                row_right,
                bob_row(_helm_cell("down")),
                [c.mirrored() for c in row_right]]), path)


# ===========================================================================
# 2. Stormsteel Cuirass  ->  player/armor/stormsteelchest.png   448x320
# ===========================================================================
#
# Vanilla's tungsten cuirass is asymmetric (one pauldron); this one is a
# symmetric plate, which is the other silhouette vanilla uses (glacialchest,
# myceliumchest). Rows and widths are tungsten's own.

CHEST_DOWN = [
    (18, 10, 12), (18, 19, 21),
    (19, 10, 13), (19, 18, 21),
    (20, 10, 21), (21, 11, 20), (22, 11, 20),
    (23, 12, 19), (24, 12, 19), (25, 13, 18),
]
CHEST_UP = [
    (18, 10, 13), (18, 18, 21),
    (19, 10, 13), (19, 18, 21),
    (20, 10, 21), (21, 11, 20), (22, 12, 19),
    (23, 12, 19), (24, 12, 19), (25, 14, 17),
]
CHEST_RIGHT = [
    (19, 11, 14), (20, 10, 15), (21, 10, 19), (22, 11, 19),
    (23, 12, 19), (24, 12, 19), (25, 15, 17),
]

SKIRT_TOP = 23          # rows at or below this sway with the walk cycle


def _chest_cell(table, sway=0, downed=False):
    cell = Cell()
    rows = []
    hem = max(y for y, _, _ in table)
    cut = hem if not downed else hem - 2
    for y, x0, x1 in table:
        if y > cut:
            continue
        if sway and y >= SKIRT_TOP:
            x0 += sway
            x1 += sway
        rows.append((y, x0, x1))
    mass = expand(rows)
    plate(cell, mass, 14.0, 20.0, lit_at=-5.0, deep_at=6.0)

    # pauldron caps catch the light; the breast plate carries one glazed
    # storm-lens boss and a pair of dark plate seams so it is not a slab
    top = min(y for y, _, _ in rows)
    paint(cell, mass, ((top, 0, 31),), LIT)
    paint(cell, mass, ((top + 1, 10, 11), (top + 1, 20, 21)), HI)
    lens = ((21, 15, 16), (22, 15, 16))
    paint(cell, mass, lens, GLOW)
    paint(cell, mass, ((21, 15, 15),), GLOW_H)
    for y in range(top + 2, cut + 1):
        paint(cell, mass, ((y, 12, 12), (y, 19, 19)), DEEP)
    paint(cell, mass, ((cut, 0, 31),), SHADE)
    contour(cell, mass, OUT)
    return cell


def _chest_attack_row():
    """Row 4: the sleeve the rotating attack arm wears, at 32px sprite (0, 8).

    Both clusters live inside the FIRST 64px cell — half-res x0..31 — because
    the sprite grid is 32px, not 64px. Placement copied from vanilla's own
    `tungstenchest` row 4.
    """
    a = Cell()
    sleeve = expand(((7, 9, 10), (8, 8, 11), (9, 8, 11), (10, 9, 11), (11, 10, 11)))
    for x, y in sleeve:
        a.put(x, y, LIT if y <= 8 else (DEEP if y >= 11 else BASE))
    a.put(8, 8, HI)
    cuff = expand(((4, 24, 27), (5, 23, 27), (6, 23, 26), (7, 24, 25)))
    for x, y in cuff:
        a.put(x, y, LIT if y <= 5 else (DEEP if y >= 7 else BASE))
    a.put(24, 5, GLOW_L)
    contour(a, sleeve, OUT)
    contour(a, cuff, OUT)
    return [a, None, None, None, None, None, None]


def gen_stormsteelchest(path):
    side = [
        _chest_cell(CHEST_RIGHT, sway=0),
        _chest_cell(CHEST_RIGHT, sway=1).shifted(-1),
        _chest_cell(CHEST_RIGHT, sway=1),
        _chest_cell(CHEST_RIGHT, sway=-1).shifted(-1),
        _chest_cell(CHEST_RIGHT, sway=-1),
    ]
    downed = _chest_cell(CHEST_RIGHT, sway=0, downed=True)
    row_right = [side[0], side[1], side[2], side[3], side[4], side[4], downed]
    save(sheet([bob_row(_chest_cell(CHEST_UP)),
                row_right,
                bob_row(_chest_cell(CHEST_DOWN)),
                [c.mirrored() for c in row_right],
                _chest_attack_row()]), path)


# ===========================================================================
# 3. Pauldrons  ->  player/armor/stormsteelarms_{left,right}.png   448x256
# ===========================================================================
#
# The spans are `gen_armor`'s, which are the measured skin/arms_* outlines
# widened one pixel — human anatomy, shared by every shoulder piece in the mod.
# A plate pauldron is a lit cap with a hard dark lower lip, no contour ring:
# the cap is three pixels tall and ringing it would leave only ring.


def _pauldron_cell(table, outward):
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
            c = HI
        elif y == bottom:
            c = OUT
        elif near >= (hi - lo) - 1:
            c = DEEP
        else:
            c = BASE if y > top else LIT
        cell.put(x, y, c)
    # one glazed rivet where the pauldron straps to the cuirass
    cell.put(hi if outward > 0 else lo, top, GLOW)
    return cell


def _arms_sheet(table, outward):
    rows = []
    for r in range(4):
        c0, c1, c3, c6 = [_pauldron_cell(t, outward) for t in table[r]]
        rows.append([c0, c1, c0, c3, c0, c0, c6])
    return sheet(rows)


def gen_stormsteelarms(path_left, path_right):
    save(_arms_sheet(gen_armor.SLEEVE_LEFT, +1), path_left)
    save(_arms_sheet(gen_armor.SLEEVE_RIGHT, -1), path_right)


# ===========================================================================
# 4. Stormsteel Greaves  ->  player/armor/stormsteelboots.png   448x256
# ===========================================================================
#
# Same measured per-frame foot outlines the Warden's boots stand on
# (`gen_armor.FOOT_*`), raised one half-res pixel over the ankle the way every
# vanilla shoe is, with a plate shin, a glazed buckle and a lit sole.


def _greave_cell(spans):
    cell = Cell()
    top = min(y for y, _, _ in spans)
    bottom = max(y for y, _, _ in spans)
    rows = list(spans)
    y0, x0, x1 = spans[0]
    rows.insert(0, (top - 1, x0, x1))
    mass = expand(rows)
    shaft = top - 1
    for x, y in mass:
        cell.put(x, y, HI if y <= shaft else (DEEP if y >= bottom - 1 else LIT))
    for y, a, b in rows[:1]:
        for x in range(a, b + 1):
            if (x - a) % 3 == 1:
                cell.put(x, y, HI)
    for x in (13, 18):
        if (x, shaft + 1) in mass:
            cell.put(x, shaft + 1, GLOW)
    for x, y in mass:
        if x in (15, 16) and y > shaft + 1:
            cell.put(x, y, OUT)
        elif y == bottom:
            cell.put(x, y, LIT)
    contour(cell, mass, OUT)
    return cell


def gen_stormsteelboots(path):
    def row(table):
        c = {k: _greave_cell(v) for k, v in table.items()}
        return [c[0], c[1], c[2], c[3], c[4], c[0], c[6]]

    row_right = row(gen_armor.FOOT_RIGHT)
    save(sheet([row(gen_armor.FOOT_UP), row_right, row(gen_armor.FOOT_DOWN),
                [c.mirrored() for c in row_right]]), path)


# ===========================================================================
# 5. Item icons  ->  items/*.png   32x32
# ===========================================================================
#
# Vanilla armor and trinket icons are FULL-resolution 32px drawings that nearly
# fill the tile: tungstenhelmet 588 opaque px, tungstenchestplate 560,
# tungstenboots 360, vambrace 452, frozenheart 480, airvessel 428. These match
# that mass, which is what keeps an icon from reading as a sticker in the grid.


class Icon:
    def __init__(self):
        self.c = px.Canvas(32, 32)

    def fill(self, mass, ox, oy, lit_at, deep_at, slope=1.2,
             lit=None, base=None, deep=None, shade=None, hi=None):
        """Four flat planes, matching the value spread vanilla armour uses."""
        lit, base, deep = lit or LIT, base or BASE, deep or DEEP
        shade, hi = shade or SHADE, hi or HI
        for x, y in mass:
            d = (x - ox) + (y - oy) * slope
            if d < lit_at - 5.0:
                c = hi
            elif d < lit_at:
                c = lit
            elif d > deep_at + 5.0:
                c = shade
            elif d > deep_at:
                c = deep
            else:
                c = base
            self.c.put(x, y, c)

    def paint(self, mass, table, color):
        for x, y in expand(table):
            if (x, y) in mass:
                self.c.put(x, y, color)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.c.outline(OUT)
        self.c.save(path)


def _band(y0, y1, x0, x1):
    return [(y, x0, x1) for y in range(y0, y1 + 1)]


def _mirror_rows(rows):
    """(y, x0, x1) spans mirrored about the 32px tile's centre."""
    return [(y, 31 - x1, 31 - x0) for y, x0, x1 in rows]


def gen_helmet_icon(path):
    """A great helm: combed skull, glazed visor band, cheek plates, chin.

    Drawn as a real silhouette rather than a disc — the first pass was a
    circle, and next to vanilla's `tungstenhelmet` on a 6x contact sheet a
    circle reads as a ball, not as a helmet. The skull is widest at the brow
    (y10..15), the jaw tapers to a 6px chin, and a comb rises out of the crown.
    """
    ic = Icon()
    dome = [
        (0, 14, 17), (1, 13, 18), (2, 12, 19), (3, 10, 21), (4, 8, 23),
        (5, 7, 24), (6, 6, 25), (7, 5, 26), (8, 4, 27), (9, 4, 27),
        (10, 3, 28), (11, 3, 28), (12, 3, 28), (13, 3, 28), (14, 3, 28),
        (15, 3, 28), (16, 4, 27), (17, 4, 27), (18, 5, 26), (19, 5, 26),
        (20, 6, 25), (21, 6, 25), (22, 7, 24), (23, 8, 23), (24, 9, 22),
        (25, 10, 21), (26, 11, 20), (27, 12, 19), (28, 13, 18),
    ]
    mass = expand(dome)
    ic.fill(mass, 12.0, 12.0, lit_at=-9.0, deep_at=8.0, slope=1.15)

    # comb: a raised ridge from the crown down the centre of the skull
    comb = expand(((0, 14, 17), (1, 14, 17), (2, 14, 17), (3, 14, 17),
                   (4, 14, 17), (5, 14, 17), (6, 14, 17), (7, 14, 17),
                   (8, 14, 17), (9, 14, 17)))
    for x, y in comb:
        ic.c.put(x, y, HI if x <= 15 else LIT)
    ic.paint(mass, ((0, 17, 17), (1, 17, 17), (2, 17, 17), (3, 17, 17),
                    (4, 17, 17), (5, 17, 17), (6, 17, 17), (7, 17, 17),
                    (8, 17, 17), (9, 17, 17)), DEEP)
    # crown highlight, one controlled cluster top-left
    ic.paint(mass, ((4, 9, 13), (5, 8, 13), (6, 7, 12), (7, 6, 11), (8, 5, 10)), HI)

    # brow band and the glazed visor slit under it
    ic.paint(mass, ((11, 3, 28), (12, 3, 28)), DEEP)
    for x in range(5, 28, 4):
        ic.c.put(x, 11, HI)
    ic.paint(mass, ((13, 3, 28), (14, 3, 28), (15, 3, 28)), GLOW_D)
    ic.paint(mass, ((14, 4, 27),), GLOW)
    for x in range(5, 27, 3):
        ic.c.put(x, 14, GLOW_L)
    ic.c.put(6, 14, GLOW_H)
    ic.paint(mass, ((16, 4, 27),), DEEP)

    # cheek plates: a seam each side, breathing slots down the centre
    for y in range(17, 27):
        ic.paint(mass, ((y, 9, 9), (y, 22, 22)), DEEP)
    ic.paint(mass, ((19, 13, 18), (21, 13, 18), (23, 14, 17)), DEEP)
    ic.paint(mass, ((18, 11, 12), (20, 11, 12), (22, 12, 13)), LIT)
    # chin flange
    ic.paint(mass, ((27, 12, 19), (28, 13, 18)), DEEP)
    ic.save(path)


def gen_chestplate_icon(path):
    """A cuirass: two pauldrons over a neck opening, tapered waist, flared hem.

    Vanilla's `tungstenchestplate` reads as a torso because the pauldrons stand
    clear of the neck and the waist pinches. The first pass had neither and
    read as a bucket.
    """
    ic = Icon()
    body = [
        (1, 4, 11), (1, 20, 27),
        (2, 3, 12), (2, 19, 28),
        (3, 2, 13), (3, 18, 29),
        (4, 2, 13), (4, 18, 29),
        (5, 2, 13), (5, 18, 29),
        (6, 3, 28), (7, 4, 27), (8, 4, 27),
        (9, 5, 26), (10, 5, 26), (11, 5, 26),
        (12, 6, 25), (13, 6, 25), (14, 7, 24),
        (15, 7, 24), (16, 8, 23), (17, 8, 23),
        (18, 9, 22), (19, 9, 22),
        (20, 8, 23), (21, 7, 24), (22, 6, 25),
        (23, 5, 26), (24, 5, 26), (25, 5, 26),
        (26, 6, 25), (27, 8, 23), (28, 11, 20),
    ]
    mass = expand(body)
    ic.fill(mass, 13.0, 14.0, lit_at=-9.0, deep_at=9.0, slope=1.0)

    # pauldron caps and the collar that runs between them
    ic.paint(mass, ((1, 0, 31), (2, 0, 31)), LIT)
    ic.paint(mass, ((1, 4, 8), (2, 3, 7)), HI)
    ic.paint(mass, ((5, 2, 13), (5, 18, 29)), DEEP)
    ic.paint(mass, ((6, 3, 28), (7, 4, 27)), DEEP)
    for x in (5, 9, 22, 26):
        ic.c.put(x, 3, HI if x < 16 else LIT)

    # storm-lens boss on the breast
    lens = expand(((10, 14, 17), (11, 13, 18), (12, 13, 18), (13, 14, 17)))
    for x, y in lens:
        ic.c.put(x, y, GLOW)
    ic.paint(mass, ((11, 13, 15),), GLOW_L)
    ic.c.put(13, 11, GLOW_H)

    # plate seams down the flanks and a stepped abdomen
    for y in range(8, 20):
        ic.paint(mass, ((y, 10, 10), (y, 21, 21)), DEEP)
    ic.paint(mass, ((15, 11, 20), (18, 11, 20)), DEEP)
    ic.paint(mass, ((16, 11, 20), (19, 11, 20)), LIT)
    # fluted tasset hem
    for x in range(7, 25, 3):
        ic.paint(mass, ((23, x, x), (24, x, x), (25, x, x), (26, x, x)), DEEP)
    ic.paint(mass, ((20, 8, 23), (21, 7, 24)), LIT)
    ic.paint(mass, ((27, 8, 23), (28, 11, 20)), DEEP)
    ic.save(path)


def gen_boots_icon(path):
    """A pair of plate greaves WITH FEET.

    The first pass drew two rectangles, which is what makes an armour icon read
    as a fence post. Vanilla's boots read because the toe steps out of the
    shaft; these do the same, each toe pointing away from the pair.
    """
    ic = Icon()
    left = []
    left += [(2, 4, 12), (3, 3, 13)]
    left += _band(4, 6, 3, 13)
    left += _band(7, 20, 4, 12)
    left += [(21, 3, 13), (22, 3, 13)]
    left += [(23, 1, 13), (24, 0, 13), (25, 0, 13), (26, 0, 13),
             (27, 0, 13), (28, 1, 13)]
    right = _mirror_rows(left)
    mass = expand(left + right)
    for x, y in mass:
        anchor = 5 if x < 16 else 26
        d = (x - anchor) * (1 if x < 16 else -1) + (y - 13.0) * 0.6
        ic.c.put(x, y, LIT if d < -3.0 else (DEEP if d > 5.5 else BASE))

    # knee cop
    ic.paint(mass, ((2, 0, 31), (3, 0, 31)), HI)
    ic.paint(mass, ((4, 0, 31),), LIT)
    ic.paint(mass, ((6, 0, 31),), DEEP)
    # glazed buckle strap across both shafts
    ic.paint(mass, ((12, 0, 31),), GLOW_D)
    ic.paint(mass, ((13, 0, 31),), DEEP)
    ic.paint(mass, ((12, 5, 7), (12, 24, 26)), GLOW_L)
    ic.c.put(5, 12, GLOW_H)
    # lamellar shin plates
    for y in (9, 16, 19):
        ic.paint(mass, ((y, 0, 31),), DEEP)
        ic.paint(mass, ((y + 1, 0, 31),), LIT)
    # ankle collar and the sabaton sole
    ic.paint(mass, ((21, 0, 31), (22, 0, 31)), LIT)
    ic.paint(mass, ((27, 0, 31), (28, 0, 31)), DEEP)
    ic.save(path)


def gen_vambrace_icon(path):
    """A bracer seen face on: curved cuffs, a centre ridge, buckled side straps.

    Three passes were needed and the failures are worth recording, because each
    one is a general lesson about 32px icons:

    * axis-aligned box + horizontal stripes -> reads as a chest of drawers;
    * bare diagonal band + horizontal lames -> reads as a staircase, because
      the stripes cut across the stepped edge and every step gets a tread;
    * diagonal cylinder with the sleeve showing only at the corners -> same
      staircase, for the same reason.

    What fixes it is a silhouette that could not be furniture: strongly curved
    top and bottom cuffs, a pinched waist, and two straps that stick OUT of the
    body with buckles on them. Vanilla `vambrace` (452 opaque px) is the mass
    anchor.
    """
    ic = Icon()
    body = [
        (3, 11, 20), (4, 10, 21), (5, 9, 22), (6, 8, 23), (7, 8, 23),
        (8, 8, 23), (9, 8, 23),
    ]
    body += _band(10, 21, 9, 22)
    body += [(22, 8, 23), (23, 8, 23), (24, 8, 23), (25, 8, 23),
             (26, 9, 22), (27, 10, 21), (28, 12, 19)]
    mass = expand(body)
    ic.fill(mass, 11.0, 14.0, lit_at=-7.0, deep_at=7.0, slope=1.0)

    # the two cuffs: a lit lip and a dark shadow under each
    ic.paint(mass, ((3, 0, 31), (4, 0, 31)), HI)
    ic.paint(mass, ((5, 0, 31),), LIT)
    ic.paint(mass, ((8, 0, 31),), DEEP)
    ic.paint(mass, ((22, 0, 31),), HI)
    ic.paint(mass, ((27, 0, 31), (28, 0, 31)), DEEP)

    # centre ridge with storm-lens rivets down it
    for y in range(5, 27):
        ic.paint(mass, ((y, 15, 16),), HI if y % 2 else LIT)
        ic.paint(mass, ((y, 17, 17),), DEEP)
    for y in (9, 15, 21):
        ic.paint(mass, ((y, 15, 16),), GLOW)
        ic.c.put(15, y, GLOW_H)

    # skyweave straps sticking out of both flanks, each with a buckle
    straps = expand(((11, 4, 8), (12, 4, 8), (13, 4, 8),
                     (11, 23, 27), (12, 23, 27), (13, 23, 27),
                     (18, 4, 8), (19, 4, 8), (20, 4, 8),
                     (18, 23, 27), (19, 23, 27), (20, 23, 27)))
    for x, y in straps:
        ic.c.put(x, y, SILK["light"] if y % 3 == 2 else SILK["base"])
    for x, y in straps:
        if y in (13, 20):
            ic.c.put(x, y, SILK["deep"])
    for x, y in ((5, 12), (26, 12), (5, 19), (26, 19)):
        ic.c.put(x, y, GLOW)
        ic.c.put(x, y - 1, GLOW_L)
    ic.save(path)


def gen_harness_icon(path):
    """A flight bandolier: one broad windsilk band, a storm-glass buckle plate,
    down tufts at the shoulder end and a frayed tail at the hip.

    The two earlier passes drew a rig with shoulder pads, straps and a belt,
    and both read as furniture (an X on a stand, then an I-beam) because at
    32px four separate thin parts become four separate thin lines. One broad
    diagonal band with one big buckle is a shape the eye resolves at 1x.
    Vanilla `airvessel` (428 opaque px) is the mass anchor.
    """
    ic = Icon()
    # the band: a broad diagonal from the shoulder (top left) to the hip
    band_rows = []
    for y in range(2, 30):
        t = (y - 2) / 27.0
        cx = 7.0 + 17.0 * t
        half = 5.0
        band_rows.append((y, int(round(cx - half)), int(round(cx + half))))
    band = expand(band_rows)
    for y, x0, x1 in band_rows:
        for x in range(x0, x1 + 1):
            near = x - x0
            ic.c.put(x, y, SILK["light"] if near <= 1
                     else (SILK["deep"] if near >= (x1 - x0) - 1 else SILK["base"]))
    # stitch line down the middle of the band
    for y, x0, x1 in band_rows:
        if y % 2 == 0:
            ic.c.put((x0 + x1) // 2, y, SILK["hi"])
    # frayed tail at the hip end
    ic.paint(band, ((28, 0, 31), (29, 0, 31)), SILK["deep"])
    for x in range(0, 32, 2):
        if (x, 29) in band:
            ic.c.put(x, 29, SILK["base"])

    # storm-down tufts tucked under the shoulder end
    tuft = expand(((1, 3, 9), (2, 1, 11), (3, 0, 12), (4, 1, 11), (5, 3, 9)))
    for x, y in tuft:
        ic.c.put(x, y, SILK["hi"] if y <= 2 else SILK["light"])
    ic.paint(tuft, ((5, 0, 31),), SILK["base"])

    # the buckle plate where the band crosses the chest
    buckle = [(11, 11, 20), (12, 9, 22)]
    buckle += _band(13, 18, 8, 23)
    buckle += [(19, 9, 22), (20, 11, 20)]
    plateau = expand(buckle)
    ic.fill(plateau, 10.0, 14.0, lit_at=-6.0, deep_at=6.0, slope=1.0)
    ic.paint(plateau, ((11, 0, 31), (12, 0, 31)), HI)
    ic.paint(plateau, ((19, 0, 31), (20, 0, 31)), DEEP)
    # glazed lens in the middle of the plate
    lens = expand(((14, 13, 18), (15, 12, 19), (16, 12, 19), (17, 13, 18)))
    for x, y in lens:
        ic.c.put(x, y, GLOW)
    ic.paint(lens, ((14, 13, 15), (15, 12, 14)), GLOW_L)
    ic.c.put(13, 14, GLOW_H)
    ic.paint(lens, ((17, 15, 18),), GLOW_D)
    # four rivets at the plate's corners
    for x, y in ((10, 13), (21, 13), (10, 18), (21, 18)):
        if (x, y) in plateau:
            ic.c.put(x, y, HI)
    ic.save(path)


def gen_locket_icon(path):
    """An aurora locket: a fleece cord and a glazed petal-lit medallion.

    Mass target is vanilla `frozenheart` (480 opaque px), which is a pendant of
    the same shape, so the medallion fills most of the tile and the cord is a
    real two-pixel braid rather than a hairline.
    """
    ic = Icon()
    # fleece cord: two strands per side, running from the knot at the top all
    # the way DOWN to the medallion's rim. The first pass left a two-pixel gap
    # between the cord and the disc, so the arcs read as detached eyebrows.
    cord = set()
    for x in range(13, 19):
        cord.add((x, 0))
    for y, lx in enumerate((12, 11, 10, 9, 8, 8, 7, 7, 6, 6, 6, 6), start=1):
        for w in (0, 1):
            cord.add((lx - w, y))
            cord.add((31 - lx + w, y))
    for x, y in sorted(cord):
        ic.c.put(x, y, SILK["base"] if x < 16 else SILK["deep"])
        if (x + y) % 3 == 0:
            ic.c.put(x, y, SILK["light"] if x < 16 else SILK["base"])
    # medallion: an ellipse filling the lower two thirds of the tile
    disc = []
    for y in range(8, 30):
        dy = (y - 19) / 11.0
        half = int(round(12.0 * (1.0 - dy * dy) ** 0.5)) if abs(dy) <= 1 else 0
        if half > 0:
            disc.append((y, 16 - half, 15 + half))
    mass = expand(disc)
    ic.fill(mass, 11.0, 15.0, lit_at=-7.0, deep_at=6.0, slope=1.1)
    # aurora petal suspended inside the glass
    petal = expand(((12, 14, 17), (13, 12, 19), (14, 11, 20), (15, 10, 21),
                    (16, 10, 21), (17, 10, 21), (18, 10, 21), (19, 10, 21),
                    (20, 11, 20), (21, 11, 20), (22, 12, 19), (23, 13, 18),
                    (24, 14, 17), (25, 15, 16)))
    for x, y in petal:
        if (x, y) in mass:
            ic.c.put(x, y, AUR["base"] if (x + y) % 2 else AUR["light"])
    ic.paint(mass, ((13, 13, 16), (14, 12, 15)), AUR["hi"])
    ic.paint(mass, ((22, 13, 18), (23, 14, 17)), AUR["deep"])
    # glazed bezel: lit rim above, shadow below, three glass sparks
    ic.paint(mass, ((9, 10, 21), (10, 7, 24)), HI)
    ic.paint(mass, ((11, 5, 26),), LIT)
    ic.paint(mass, ((27, 8, 23), (28, 10, 21)), DEEP)
    for x, y in ((6, 17), (23, 11), (25, 22)):
        if (x, y) in mass:
            ic.c.put(x, y, GLOW_H)
    ic.save(path)


# ===========================================================================

def generate(res_root):
    armor = os.path.join(res_root, "player", "armor")
    items = os.path.join(res_root, "items")
    os.makedirs(armor, exist_ok=True)
    os.makedirs(items, exist_ok=True)
    gen_stormsteelhelmet(os.path.join(armor, "stormsteelhelmet.png"))
    gen_stormsteelchest(os.path.join(armor, "stormsteelchest.png"))
    gen_stormsteelarms(os.path.join(armor, "stormsteelarms_left.png"),
                       os.path.join(armor, "stormsteelarms_right.png"))
    gen_stormsteelboots(os.path.join(armor, "stormsteelboots.png"))
    gen_helmet_icon(os.path.join(items, "stormsteelhelmet.png"))
    gen_chestplate_icon(os.path.join(items, "stormsteelchestplate.png"))
    gen_boots_icon(os.path.join(items, "stormsteelboots.png"))
    gen_vambrace_icon(os.path.join(items, "stormsteelvambrace.png"))
    gen_locket_icon(os.path.join(items, "auroralocket.png"))
    gen_harness_icon(os.path.join(items, "zephyrharness.png"))


if __name__ == "__main__":
    import sys
    generate(sys.argv[1] if len(sys.argv) > 1 else "src/main/resources")
