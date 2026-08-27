"""Wall sheets in the vanilla 352x128 WallObject layout, decoded cell by cell
from WallObject.addWallDrawOptions (the draw code is the ground truth — each
16px cell of the left 4x8 blob is a specific autotile piece):

  row 0   free top caps (N rim): (0,0) W-end, (1,0)/(2,0) continuing, (3,0) E-end
  row 1-2 ceiling bands: col 0 W edge, cols 1-2 interior, col 3 E edge
  row 3   front face TOP halves (crenellated rim), 4 variant columns
  row 4   front face BOTTOM halves (foot shadow), same columns
  row 5   (0,5)/(1,5) inner-corner mid pieces (side rim hooking at top);
          (2,5)/(3,5) other-wall-below top strips
  row 6   (0,6)/(1,6) inner-corner bottom pieces (side rim hooking at bottom);
          (2,6)/(3,6) other-wall-below rim + mini face strip
  row 7   (0,7)/(1,7) inner-corner top hooks; (2,7)/(3,7) diagonal-other-wall
          mid pieces (small face nub at the bottom edge)
  x 64-96   window insert (2x8 cells: short high window rows 0-1,
            tall strip rows 2-7)
  x 96-352  eight 32x128 door-frame columns (rot0..3, closed/open each).
            Every one is drawn at pos(drawX, drawY - 96), so row 96 is the
            tile's top edge -- see the door section for the vanilla extents.
"""

from px import Canvas, Rng, with_alpha, mix
import palette


def _ceiling(c, x0, y0, w, h, mat, salt):
    """Dark top-down wall cap with faint texture."""
    for x in range(w):
        for y in range(h):
            r = Rng(((x0 + x) * 7349 + (y0 + y) * 12611) ^ salt)
            v = r.float()
            tone = mat["ceil"]
            if v < 0.06:
                tone = mat["ceil_hi"]
            c.put(x0 + x, y0 + y, tone)


def _face(c, x0, y0, w, top_half, mat, salt):
    """Bright brick front face; top half carries the crenellated rim,
    bottom half the foot shadow."""
    for x in range(w):
        for y in range(16):
            gy = y if top_half else y + 16
            course = gy // 8
            brick_shift = 4 if course % 2 else 0
            in_mortar_h = gy % 8 == 7
            in_mortar_v = (x0 + x + brick_shift) % 8 == 7
            tone = mat["face"]
            if in_mortar_h or in_mortar_v:
                tone = mat["face_deep"]
            elif gy % 8 == 0:
                tone = mat["face_hi"]
            r = Rng(((x0 + x) * 733 + gy * 977) ^ salt)
            if r.float() < 0.04:
                tone = mat["face_deep"]
            c.put(x0 + x, y0 + y, tone)
    if top_half:
        # crenellated rim: raised merlons every 8px
        for x in range(w):
            step = (x0 + x) % 8
            c.put(x0 + x, y0, mat["ceil"] if 2 <= step <= 5 else mat["face_hi"])
            if not (2 <= step <= 5):
                c.put(x0 + x, y0 + 1, mat["face_hi"])
    else:
        for x in range(w):
            c.put(x0 + x, y0 + 14, mat["face_deep"])
            c.put(x0 + x, y0 + 15, palette.OUTLINE)


def _edge_line(c, x0, y0, w, h, vertical, at_start, color):
    if vertical:
        x = x0 if at_start else x0 + w - 1
        for y in range(h):
            c.put(x, y0 + y, color)
    else:
        y = y0 if at_start else y0 + h - 1
        for x in range(w):
            c.put(x0 + x, y, color)


def _mini_face(c, x0, y0, w, h, mat, salt):
    """Small crenellated face strip for the junction pieces of rows 6-7."""
    for x in range(w):
        for y in range(1, h):
            gy = y + 20  # bottom-half brick phase
            course = gy // 8
            brick_shift = 4 if course % 2 else 0
            tone = mat["face"]
            if gy % 8 == 7 or (x0 + x + brick_shift) % 8 == 7:
                tone = mat["face_deep"]
            elif gy % 8 == 0:
                tone = mat["face_hi"]
            r = Rng(((x0 + x) * 733 + (y0 + y) * 977) ^ salt)
            if r.float() < 0.04:
                tone = mat["face_deep"]
            c.put(x0 + x, y0 + y, tone)
    for x in range(w):  # merlon rim on the strip's top row
        step = (x0 + x) % 8
        c.put(x0 + x, y0, mat["ceil"] if 2 <= step <= 5 else mat["face_hi"])


def _build_wall(mat, salt):
    c = Canvas(352, 128)
    C = 16

    # ---- auto-tile blob (0-64): cell semantics from WallObject draw code ----
    # row 0: free top caps
    for col in range(4):
        _ceiling(c, col * C, 0, C, C, mat, salt)
        _edge_line(c, col * C, 0, C, C, False, True, mat["rim"])
    _edge_line(c, 0, 0, C, C, True, True, mat["rim"])
    _edge_line(c, 3 * C, 0, C, C, True, False, mat["rim"])

    # rows 1-2: ceiling bands (W edge, interior x2, E edge)
    for row in (1, 2):
        for col in range(4):
            _ceiling(c, col * C, row * C, C, C, mat, salt + row)
        _edge_line(c, 0, row * C, C, C, True, True, mat["rim"])
        _edge_line(c, 3 * C, row * C, C, C, True, False, mat["rim"])

    # rows 3-4: front face top/bottom halves (4 variant columns)
    for col in range(4):
        _face(c, col * C, 3 * C, C, True, mat, salt + col * 31)
        _face(c, col * C, 4 * C, C, False, mat, salt + col * 31)

    # rows 5-7: junction and inner-corner pieces on a ceiling base
    for row in (5, 6, 7):
        for col in range(4):
            _ceiling(c, col * C, row * C, C, C, mat, salt + 7 * row + col)

    def vrim(cell_x, cell_y, x, y0, y1):
        for y in range(y0, y1):
            c.put(cell_x * C + x, cell_y * C + y, mat["rim"])

    def hrim(cell_x, cell_y, y, x0, x1):
        for x in range(x0, x1):
            c.put(cell_x * C + x, cell_y * C + y, mat["rim"])

    # (0,5)/(1,5): inner-corner mid — side rim hooking toward the center at top
    vrim(0, 5, 0, 0, 16)
    hrim(0, 5, 0, 0, 6)
    vrim(1, 5, 15, 0, 16)
    hrim(1, 5, 0, 10, 16)
    # (2,5): other-wall-below W-end — hook fragment in the top-right corner
    hrim(2, 5, 0, 11, 16)
    vrim(2, 5, 15, 0, 5)
    # (3,5): other-wall-below continuing — full top rim, hook down at right
    hrim(3, 5, 0, 0, 16)
    vrim(3, 5, 15, 0, 6)

    # (0,6)/(1,6): inner-corner bottom — side rim hooking toward center at bottom
    vrim(0, 6, 0, 0, 16)
    hrim(0, 6, 15, 0, 6)
    vrim(1, 6, 15, 0, 16)
    hrim(1, 6, 15, 10, 16)
    # (2,6)/(3,6): other-wall-below band — top rim over a mini face strip
    hrim(2, 6, 0, 0, 16)
    _mini_face(c, 2 * C, 6 * C + 3, C, 13, mat, salt + 61)
    hrim(3, 6, 0, 0, 16)
    vrim(3, 6, 15, 1, 9)
    _mini_face(c, 3 * C, 6 * C + 3, 12, 13, mat, salt + 62)

    # (0,7)/(1,7): inner-corner top hooks
    vrim(0, 7, 0, 0, 6)
    hrim(0, 7, 0, 0, 6)
    vrim(1, 7, 15, 0, 6)
    hrim(1, 7, 0, 10, 16)
    # (2,7)/(3,7): diagonal-other-wall mid — small face nub at the bottom edge
    _mini_face(c, 2 * C + 10, 7 * C + 10, 6, 6, mat, salt + 71)
    _mini_face(c, 3 * C, 7 * C + 10, 6, 6, mat, salt + 72)

    # ---- window insert (64-96) ----
    #
    # WallWindowObject draws TWO different windows from this strip and picks
    # between them by which way the wall runs (getWindowDir):
    #
    #   dir 1  wall runs NORTH-SOUTH  -> rows 0-1, at drawY-16 and drawY.
    #          You are looking down on the wall's ROOF. Vanilla's is fully
    #          opaque -- 512/512 on both rows -- with the window drawn ONTO the
    #          cap as a frame seen from above. There is no hole: from directly
    #          overhead a window shows you roof, not floor.
    #
    #   dir 0  wall runs EAST-WEST    -> rows 2..7, at drawY-64 .. drawY+16.
    #          Now you are looking at the wall's FRONT, and the opening is
    #          genuinely TRANSPARENT: vanilla leaves the middle of rows 5 and 6
    #          empty so the ground shows through, framed by jambs, a mullion
    #          post and a bright sill. Rows 2-4 stay empty (see below).
    #
    # Both halves were wrong and in opposite directions. The strip carried one
    # front-facing glazed pane for both cases, so a window in a north-south wall
    # faced the camera instead of lying flat on the roof -- "das Fenster links
    # am Block zeigt weiterhin nach unten" -- and the east-west one sat as a
    # pane inside the wall's dark cap band instead of being a hole in its face.
    # Getting the HEIGHT right (the previous fix) did not touch either.
    def cap_px(x, y, salt):
        return mat["ceil_hi"] if Rng((x * 7349 + y * 12611) ^ salt).float() < 0.06 else mat["ceil"]

    # --- dir 1: north-south wall, seen from above (rows 0-1) ---
    for y in range(0, 32):
        for x in range(32):
            c.put(64 + x, y, cap_px(64 + x, y, salt + 50))
    for y in range(0, 32):                       # the cap's own rims, east+west
        c.put(64, y, mat["rim"])
        c.put(64 + 31, y, mat["rim"])
    # The window itself, lying flat: a frame with the glass catching the sky.
    # It runs ALONG the wall, so it is tall and narrow in the cell, not wide.
    # Seen from above the glass is looking straight up at the sky, so it reads
    # DARKER than the same glass does edge-on, not brighter. A full-strength
    # pane here looks like a sticker on the roof rather than an opening in it.
    roof_glass = mix(mat["glass"], mat["ceil"], 0.45)
    roof_glass_hi = mix(mat["glass"], mat["ceil"], 0.25)
    for y in range(5, 27):
        for x in range(7, 25):
            c.put(64 + x, y, mat["face"])
    for y in range(7, 25):
        for x in range(9, 23):
            c.put(64 + x, y, roof_glass_hi if (x + y) % 7 == 0 else roof_glass)
    for x in range(7, 25):                       # frame: lit top, shadowed foot
        c.put(64 + x, 5, mat["face_hi"])
        c.put(64 + x, 6, mat["face_hi"])
        c.put(64 + x, 25, mat["face_deep"])
        c.put(64 + x, 26, mat["face_deep"])
    for y in range(5, 27):
        c.put(64 + 7, y, mat["face_hi"])
        c.put(64 + 8, y, mat["face_hi"])
        c.put(64 + 23, y, mat["face_deep"])
        c.put(64 + 24, y, mat["face_deep"])
    for x in range(9, 23):                       # glazing bars: two lights each way
        c.put(64 + x, 15, mat["face_deep"])
        c.put(64 + x, 16, mat["face_deep"])
    for y in range(7, 25):
        c.put(64 + 15, y, mat["face_deep"])
        c.put(64 + 16, y, mat["face_deep"])

    # --- dir 0: east-west wall, seen from the front (rows 5-7) ---
    # Rows 2-4 are drawn at drawY-64/-48/-32 -- two whole tiles above the tile
    # the window sits on. Vanilla leaves them empty and so do we; filling them
    # is what made the window twice the height of its own wall.
    _ceiling(c, 64, 5 * C, 32, C, mat, salt + 60)
    _face(c, 64, 6 * C, 32, True, mat, salt + 61)
    _face(c, 64, 7 * C, 32, False, mat, salt + 62)
    JAMB = 6                                     # vanilla's jambs are 6px
    MULL = (14, 18)                              # central post
    HEAD = 5 * C + 2                             # y82: opening starts
    SILL = 6 * C + 12                            # y108: opening ends
    for y in range(HEAD, SILL):
        for x in range(JAMB, 32 - JAMB):
            if MULL[0] <= x < MULL[1]:
                continue                         # the post stays wall
            # Tinted, not solid. Vanilla leaves this fully transparent; a thin
            # tint keeps the mod's blue-glass identity while still reading as a
            # hole you can see the ground through, which is the whole point of
            # a window in a top-down game.
            c.put(64 + x, y, with_alpha(mat["glass"], 55))
    for y in range(HEAD, SILL):                  # reveals: lit left, dark right
        c.put(64 + JAMB, y, mat["face_hi"])
        c.put(64 + 31 - JAMB, y, mat["face_deep"])
        c.put(64 + MULL[0], y, mat["face_hi"])
        c.put(64 + MULL[1] - 1, y, mat["face_deep"])
    for x in range(JAMB, 32 - JAMB):             # lintel over, sill under
        c.put(64 + x, HEAD - 1, mat["rim"])
        c.put(64 + x, SILL, mat["face_hi"])
        c.put(64 + x, SILL + 1, mat["face_hi"])

    # ---- door frames (96-352): eight 32x128 cells ----
    # WallDoorObject/WallDoorOpenObject draw EVERY one of these cells at
    # pos(drawX, drawY - 96). Row 96 therefore lands on the tile's top edge and
    # everything above row 96 sticks out over the tile. A wall segment only
    # rises 16px above its own tile (its row-0 cap is drawn at drawY - 16), so
    # a cell painted from row 0 renders a door that towers 96px -- three whole
    # tiles -- over the wall it sits in. That is what "die Tuer ist 3x so hoch
    # wie normale Tueren aber der Rest der Wand nicht" was.
    #
    # The extents below were measured off stonewall.png, which is the format's
    # ground truth: closed head-on y88..127, closed edge-on y70..127, open
    # edge-on y68..127, open head-on y68..127 (swung north) / y90..127 (south).
    # Keep these. Changing a door's top row changes how far it sticks up.
    TILE_TOP = 96

    def brick_px(x, y, salt):
        """One pixel of brick face, phase-locked to the sheet so the courses of
        a door line up with the courses of the wall beside it."""
        course = y // 8
        shift = 4 if course % 2 else 0
        if y % 8 == 7 or (x + shift) % 8 == 7:
            return mat["face_deep"]
        if y % 8 == 0:
            return mat["face_hi"]
        return mat["face_deep"] if Rng((x * 733 + y * 977) ^ salt).float() < 0.04 else mat["face"]

    def cap_px(x, y, salt):
        """One pixel of the dark top-down cap: wall thickness and thresholds."""
        return mat["ceil_hi"] if Rng((x * 7349 + y * 12611) ^ salt).float() < 0.06 else mat["ceil"]

    def plank_px(x, y, vertical):
        """Gloomwood leaf. Plank seams run along the leaf's long axis, so a
        head-on leaf gets vertical seams and an edge-on one horizontal."""
        n = x if vertical else y
        if n % 6 == 0:
            return palette.GLOOMWOOD["deep"]
        if n % 6 == 1:
            return palette.GLOOMWOOD["light"]
        return palette.GLOOMWOOD["base"]

    def fill(bands, tone):
        for y0, y1, bx0, bx1 in bands:
            for y in range(y0, y1 + 1):
                for x in range(bx0, bx1 + 1):
                    c.put(x, y, tone(x, y))

    def rim_arch(bands, tone=None):
        """Light the top row of each band so a chamfered top reads as a curve.
        Stone lintels take the wall rim; a swung leaf takes its own highlight,
        or it reads as a lid on a chest instead of the top edge of a door."""
        tone = mat["rim"] if tone is None else tone
        for y0, _, bx0, bx1 in bands:
            for x in range(bx0, bx1 + 1):
                c.put(x, y0, tone)

    def floor_line(y, x0, x1):
        for x in range(x0, x1 + 1):
            c.put(x, y, palette.OUTLINE)

    def iron_band(y, x0, x1):
        for x in range(x0, x1 + 1):
            c.put(x, y, palette.IRONWORK["base"])
            c.put(x, y + 1, palette.IRONWORK["deep"])

    for i in range(8):
        x0 = 96 + i * 32
        salt_i = salt + 70 + i
        rot = i // 2
        is_open = i % 2 == 1
        # rot 0/2 sit in an east-west wall (we see the doorway head-on),
        # rot 1/3 in a north-south wall (we see it edge-on) -- and opening the
        # door swaps which of the two we are looking at.
        head_on = (rot in (0, 2)) != is_open

        if not is_open and head_on:
            # Closed, head-on: the doorway itself. Brick jambs and a chamfered
            # lintel around a planked leaf. 40px tall, so it clears the tile by
            # the same 8px whichever way the wall runs.
            arch = [(88, 89, x0 + 5, x0 + 26), (90, 91, x0 + 3, x0 + 28),
                    (92, 93, x0 + 1, x0 + 30), (94, 127, x0, x0 + 31)]
            fill(arch, lambda x, y: brick_px(x, y, salt_i))
            rim_arch(arch[:3])   # chamfer steps only; a rim on row 94 would stripe the jambs
            fill([(TILE_TOP + 1, 126, x0 + 8, x0 + 23)],
                 lambda x, y: plank_px(x, y, True))
            iron_band(104, x0 + 8, x0 + 23)
            iron_band(118, x0 + 8, x0 + 23)
            c.put(x0 + 21, 111, palette.IRONWORK["hi"])
            c.put(x0 + 21, 112, palette.IRONWORK["hi"])
            for y in range(TILE_TOP + 1, 127):        # jamb shadow beside the leaf
                c.put(x0 + 7, y, mat["face_deep"])
                c.put(x0 + 24, y, mat["face_deep"])
            floor_line(127, x0, x0 + 31)

        elif not is_open:
            # Closed, edge-on: we only see the wall's narrow side, with the leaf
            # lying inside the tile footprint. 58px tall.
            mirror = rot == 3
            strip = [(70, 71, 20, 25), (72, 95, 18, 27)]
            foot = (96, 127, 14, 31)
            if mirror:
                strip = [(y0, y1, 31 - b, 31 - a) for y0, y1, a, b in strip]
                foot = (96, 127, 0, 17)
            strip = [(y0, y1, x0 + a, x0 + b) for y0, y1, a, b in strip]
            fill(strip, lambda x, y: brick_px(x, y, salt_i))
            rim_arch(strip)
            fy0, fy1, fa, fb = foot
            fill([(fy0, fy1 - 1, x0 + fa, x0 + fb)], lambda x, y: cap_px(x, y, salt_i))
            for x in range(x0 + fa, x0 + fb + 1):     # rim where the cap meets air
                c.put(x, fy0, mat["rim"])
            leaf_x = x0 + (fa + 4 if mirror else fb - 6)
            for y in range(fy0 + 4, fy1):             # the closed leaf, seen edgewise
                for x in range(leaf_x, leaf_x + 3):
                    c.put(x, y, plank_px(x, y, True))
            c.put(leaf_x + 1, 112, palette.IRONWORK["hi"])
            floor_line(fy1, x0 + fa, x0 + fb)

        elif not head_on:
            # Open, edge-on: the leaf has swung a quarter turn and now stands
            # against the jamb as a narrow slab, with the threshold below it.
            leaf = [(68, 69, x0 + 24, x0 + 29), (70, 115, x0 + 22, x0 + 31)]
            fill(leaf, lambda x, y: plank_px(x, y, False))
            rim_arch(leaf, palette.GLOOMWOOD["hi"])
            iron_band(78, x0 + 22, x0 + 31)
            iron_band(100, x0 + 22, x0 + 31)
            for y in range(70, 116):                  # free edge of the leaf
                c.put(x0 + 22, y, palette.GLOOMWOOD["deep"])
            fill([(116, 126, x0, x0 + 31)], lambda x, y: cap_px(x, y, salt_i))
            floor_line(127, x0, x0 + 31)

        else:
            # Open, head-on: the leaf swung into view across the tile. rot 1
            # swings north (leaf drawn high), rot 3 swings south (leaf low).
            top = 68 if rot == 1 else 90
            arch = [(top, top + 1, x0 + 5, x0 + 26), (top + 2, top + 3, x0 + 3, x0 + 28),
                    (top + 4, top + 5, x0 + 1, x0 + 30)]
            body_top = top + 6
            fill(arch, lambda x, y: plank_px(x, y, True))
            rim_arch(arch, palette.GLOOMWOOD["hi"])
            if rot == 1:
                fill([(body_top, 103, x0, x0 + 31)], lambda x, y: plank_px(x, y, True))
                iron_band(80, x0, x0 + 31)
                iron_band(94, x0, x0 + 31)
                floor_line(103, x0, x0 + 31)
                fill([(104, 126, x0 + 16, x0 + 31)], lambda x, y: cap_px(x, y, salt_i))
                floor_line(127, x0 + 16, x0 + 31)
            else:
                fill([(body_top, 126, x0, x0 + 31)], lambda x, y: plank_px(x, y, True))
                iron_band(104, x0, x0 + 31)
                iron_band(118, x0, x0 + 31)
                fill([(120, 126, x0, x0 + 15)], lambda x, y: cap_px(x, y, salt_i))
                floor_line(127, x0, x0 + 31)
            # hinge post on the side the leaf swings from: without it a
            # full-width plank panel reads as a chest lid rather than a door.
            hinge_top = body_top if rot == 1 else top
            hinge_bot = 103 if rot == 1 else 119
            for y in range(hinge_top, hinge_bot + 1):
                c.put(x0, y, palette.IRONWORK["deep"])
                c.put(x0 + 1, y, palette.IRONWORK["base"])
                c.put(x0 + 2, y, palette.IRONWORK["light"] if y % 8 == 2 else palette.IRONWORK["deep"])

        # Jamb highlights: on a CLOSED door the cell's outer columns are where
        # the leaf meets the wall, so they carry the wall's rim colour. An open
        # leaf stands clear of the wall and keeps its own edges.
        if not is_open:
            for y in range(128):
                for x in (x0, x0 + 31):
                    if c.filled(x, y) and y < 127:
                        c.put(x, y, mat["rim"])
    return c


SKYSTONE_WALL = {
    "ceil": (44, 48, 58),
    "ceil_hi": (58, 63, 74),
    "rim": (150, 158, 172),
    "face": (116, 126, 143),
    "face_hi": (152, 161, 176),
    "face_deep": (78, 86, 101),
    "glass": (186, 226, 230),
}

NIGHTFELL_WALL = {
    "ceil": (22, 20, 30),
    "ceil_hi": (33, 30, 43),
    "rim": (94, 88, 114),
    "face": (48, 44, 63),
    "face_hi": (72, 66, 90),
    "face_deep": (30, 27, 41),
    "glass": (146, 130, 226),
}


def gen_walls(out_dir):
    _build_wall(SKYSTONE_WALL, 0x5A11).save(f"{out_dir}/skystonebrickwall.png")
    _build_wall(NIGHTFELL_WALL, 0x5A22).save(f"{out_dir}/nightfellwall.png")
