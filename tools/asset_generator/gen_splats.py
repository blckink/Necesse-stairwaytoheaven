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
    # isolated diagonal blobs: big disc toward the named corner, notch far side
    (0, 0): [(24, 24, 26)],  # SE
    (2, 0): [(8, 24, 26)],   # SW
    (0, 2): [(24, 8, 26)],   # NE
    (2, 2): [(8, 8, 26)],    # NW
}


def _mask_alpha(discs, x, y, salt):
    """255 inside any disc core; dithered 4px falloff band; 0 outside."""
    if discs is None:
        return 255
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


def build_splat(path, material, variants=3, salt=0x51A7, frames=1):
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
            # ...then mask each cell by its blend shape
            for cx in range(7):
                for cy in range(3):
                    if (cx, cy) not in CELL_MASKS:
                        continue
                    discs = CELL_MASKS[(cx, cy)]
                    for x in range(CELL):
                        for y in range(CELL):
                            a = _mask_alpha(discs, x, y, vsalt + cx * 31 + cy * 71)
                            if a > 0:
                                r, g, b, _ = block.get(cx * CELL + x, cy * CELL + y)
                                sheet.put(bx + cx * CELL + x, by + cy * CELL + y, (r, g, b, a))
    sheet.save(path)


# --- material painters (32x32 cells, seamless via position-locked speckle) ---

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
    ramp = palette.CLOUDTURF
    _speckle_cell(c, x0, y0, ramp, 0xC10D0000)
    rng = Rng(salt)
    for _ in range(rng.range(4, 6)):
        x = x0 + rng.range(1, 30)
        y = y0 + rng.range(2, 29)
        lean = rng.pick((-1, 0, 1))
        c.put(x, y, ramp["tuft"])
        c.put(x + lean, y - 1, ramp["tuft"])
        if rng.chance(0.5):
            c.put(x + lean, y - 2, ramp["hi"])
    for _ in range(rng.range(2, 3)):
        c.put(x0 + rng.range(2, 29), y0 + rng.range(2, 29), ramp["hi"])


def material_skystone(c, x0, y0, salt, frame=0):
    ramp = palette.SKYSTONE
    _speckle_cell(c, x0, y0, ramp, 0x51A90000)
    rng = Rng(salt)
    for _ in range(rng.range(1, 3)):
        x = x0 + rng.range(3, 26)
        y = y0 + rng.range(3, 26)
        step = rng.pick((-1, 1))
        for i in range(rng.range(3, 6)):
            c.put(x + i, y + (i // 2) * step, ramp["deep"])
    for _ in range(rng.range(2, 4)):
        x = x0 + rng.range(2, 28)
        y = y0 + rng.range(2, 28)
        c.put(x, y, ramp["light"])
        c.put(x + 1, y + 1, ramp["hi"])


def material_stormslate(c, x0, y0, salt, frame=0):
    ramp = palette.STORMSLATE
    _speckle_cell(c, x0, y0, ramp, 0x570A0000)
    rng = Rng(salt)
    for _ in range(rng.range(2, 3)):
        x = x0 + rng.range(2, 22)
        y = y0 + rng.range(2, 22)
        for i in range(rng.range(4, 8)):
            c.put(x + i, y + i, ramp["deep"])
    if rng.chance(0.6):
        c.put(x0 + rng.range(4, 27), y0 + rng.range(4, 27), ramp["charge"])


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
    for _ in range(rng.range(3, 5)):
        c.put(x0 + rng.range(1, 30), y0 + rng.range(1, 30),
              ramp["hi"] if rng.chance(0.3) else ramp["deep"])


def material_mist(deep):
    """Animated drifting mist: streaks shift per frame (8-frame liquid loop)."""
    def painter(c, x0, y0, salt, frame=0):
        ramp = palette.MISTSEA
        base = ramp["deep"] if deep else ramp["base"]
        for x in range(32):
            for y in range(32):
                c.put(x0 + x, y0 + y, base)
        rng = Rng(salt)
        shift = frame * 4
        for _ in range(rng.range(4, 6)):
            x = rng.range(0, 20)
            y = y0 + rng.range(2, 29)
            tone = rng.pick((ramp["light"], ramp["hi"]) if not deep else (ramp["base"], ramp["light"]))
            for i in range(rng.range(6, 14)):
                c.put(x0 + (x + i + shift) % 32, y, tone)
        c.put(x0 + (rng.range(2, 29) + shift) % 32, y0 + rng.range(2, 29), ramp["hi"])
    return painter
