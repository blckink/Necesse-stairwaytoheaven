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
  x 96-352  eight 32x128 door-frame columns (rot0..3, closed/open each)
"""

from px import Canvas, Rng
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
    def window_pane(x0, y0, w, h):
        for x in range(w):
            for y in range(h):
                c.put(x0 + x, y0 + y, mat["glass"])
        for x in range(w):
            c.put(x0 + x, y0 + h // 2, mat["face_deep"])
        for y in range(h):
            c.put(x0 + w // 2, y0 + y, mat["face_deep"])
        _edge_line(c, x0, y0, w, h, False, True, mat["face_hi"])
        _edge_line(c, x0, y0, w, h, False, False, mat["face_deep"])
        _edge_line(c, x0, y0, w, h, True, True, mat["face_hi"])
        _edge_line(c, x0, y0, w, h, True, False, mat["face_deep"])

    # short high window (rows 0-1)
    _face(c, 64, 0, 32, True, mat, salt + 50)
    _face(c, 64, C, 32, False, mat, salt + 50)
    window_pane(68, 6, 24, 16)
    # tall strip (rows 2-7)
    for row in range(2, 8):
        _ceiling(c, 64, row * C, 32, C, mat, salt + 60) if row < 3 else _face(c, 64, row * C, 32, row % 2 == 1, mat, salt + 60 + row)
    window_pane(70, 3 * C + 2, 20, 4 * C - 8)

    # ---- door frames (96-352): 8 columns of 32x128 ----
    for i in range(8):
        x0 = 96 + i * 32
        is_open = i % 2 == 1
        # frame: ceiling strip on top, face pillars at the sides
        _ceiling(c, x0, 0, 32, 24, mat, salt + 70 + i)
        for side_x in (x0, x0 + 24):
            _face(c, side_x, 24, 8, True, mat, salt + 80 + i)
            for yy in range(40, 120, 16):
                _face(c, side_x, yy, 8, (yy // 16) % 2 == 0, mat, salt + 80 + i)
        if not is_open:
            # closed leaf: gloomwood door with iron bands
            for x in range(8, 24):
                for y in range(30, 118):
                    tone = palette.GLOOMWOOD["base"] if (y % 10) not in (0, 1) else palette.GLOOMWOOD["deep"]
                    c.put(x0 + x, y, tone)
            for y in (44, 82):
                for x in range(8, 24):
                    c.put(x0 + x, y, palette.IRONWORK["base"])
            c.put(x0 + 20, 74, palette.IRONWORK["hi"])  # handle
        _edge_line(c, x0, 0, 32, 128, True, True, mat["rim"])
        _edge_line(c, x0, 0, 32, 128, True, False, mat["rim"])
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
