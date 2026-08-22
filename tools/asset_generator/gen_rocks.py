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
"""

from px import Canvas, Rng
import palette

ROWS = 13
CELL = 16


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
    """16x16 front-cliff fill; upper row carries the lip highlight."""
    for x in range(CELL):
        for y in range(CELL):
            r = Rng((x * 9173 + y * 14591) ^ salt ^ 0xFACE)
            v = r.float()
            base = ramp["deep"]
            if v < 0.14:
                base = ramp["base"]
            canvas.put(x0 + x, y0 + y, base)
    if upper:
        for x in range(CELL):
            canvas.put(x0 + x, y0, ramp["light"])
            if Rng(x * 331 ^ salt).chance(0.5):
                canvas.put(x0 + x, y0 + 1, ramp["base"])


def _cell(ramp, salt, row, outer_left):
    """Render one LEFT-half cell; the right half is its mirror."""
    c = Canvas(CELL, CELL)
    o = palette.OUTLINE
    is_face = row in (3, 4, 8, 9)
    if is_face:
        _face(c, 0, 0, ramp, salt + row * 31, upper=row in (3, 8))
    else:
        _surface(c, 0, 0, ramp, salt + (0 if row in (6, 7) else 0))

    side_exposed = row in (0, 1, 2, 3, 4, 11)
    if row in (0, 5):  # top cap
        for x in range(CELL):
            c.put(x, 0, o)
        for x in range(CELL):
            c.put(x, 1, ramp["hi"])
    if side_exposed:
        for y in range(CELL):
            c.put(0, y, o)
        for y in range(CELL):
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
    if row in (4, 9):  # ground contact
        for x in range(CELL):
            c.put(x, CELL - 1, o)
            c.put(x, CELL - 2, ramp["deep"] if Rng(x * 17 ^ salt).chance(0.7) else ramp["base"])
    if row == 4:  # rounded outer-bottom corner of the face
        c.put(0, CELL - 1, (0, 0, 0, 0))
        c.put(1, CELL - 1, o)
        c.put(0, CELL - 2, o)
    return c if outer_left else c.mirrored()


def gen_rock_sheet(path, ramp, variants=2, salt=0xACE):
    sheet = Canvas(variants * 32, ROWS * CELL)
    for v in range(variants):
        vsalt = salt + v * 7919
        for row in range(ROWS):
            left = _cell(ramp, vsalt, row, outer_left=True)
            right = _cell(ramp, vsalt + 13, row, outer_left=False)
            sheet.paste(left, v * 32, row * CELL)
            sheet.paste(right, v * 32 + CELL, row * CELL)
    sheet.save(path)


def gen_ore_sheet(path, ramp, variants=2, salt=0xE77):
    """Transparent overlay with vein clusters, same grid as the rock sheet."""
    sheet = Canvas(variants * 32, ROWS * CELL)
    surface_rows = (0, 1, 2, 5, 6, 7, 10, 11, 12)
    face_rows = (3, 8)
    for v in range(variants):
        for half in range(2):
            x0 = v * 32 + half * CELL
            for row in range(ROWS):
                rng = Rng(salt + v * 131 + half * 17 + row * 719)
                on_surface = row in surface_rows
                on_face = row in face_rows
                if not (on_surface or on_face):
                    continue
                # guarantee veins in the icon source region (variant 0, rows 0-1)
                guaranteed = v == 0 and row in (0, 1)
                if not guaranteed and not rng.chance(0.6 if on_surface else 0.3):
                    continue
                for _ in range(rng.range(1, 2)):
                    cx = x0 + rng.range(4, 11)
                    cy = row * CELL + rng.range(4 if row in (0, 5) else 2, 11)
                    sheet.ellipse(cx, cy, 2.2, 1.8, ramp["deep"])
                    sheet.ellipse(cx, cy - 1, 1.4, 1.2, ramp["base"])
                    sheet.put(cx, cy - 2, ramp["light"])
                    sheet.put(cx - 1, cy - 1, ramp["light"])
                    sheet.put(cx, cy - 3, ramp["hi"])
    sheet.save(path)
