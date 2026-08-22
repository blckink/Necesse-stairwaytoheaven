"""Terrain "_splat" atlas builder — the 1.3.2 renderer's primary tile format.

Layout per 224x96 variant block (verified against vanilla atlases like
ash_splat.png and the TerrainSplatterTile draw path):
- right 128x96 area (4x3 cells of 32px): full ground tiles — the random
  variants picked by NEW_FULL_TILE_SPRITES
- left 96x96 area: one organic splatter blob with soft, ragged alpha edges —
  stamped over neighboring tiles for blending

Multiple variants stack vertically (height = 96 * variants); animation frames
extend horizontally (width = 224 * frames; we ship 1 frame).

A tile is described by a `material(px_canvas, x0, y0, salt)` painter that
fills a 32x32 cell; the builder derives both the full tiles and the blob fill
from it, so the art stays in one place.
"""

from px import Canvas, Rng
import palette

BLOCK_W = 224
BLOCK_H = 96
BLOB_SIZE = 96


def _blob_alpha(x, y, salt):
    """Soft organic blob mask over 96x96: full core, ragged falloff edge."""
    cx = cy = BLOB_SIZE / 2.0
    dx = (x - cx) / (BLOB_SIZE / 2.0)
    dy = (y - cy) / (BLOB_SIZE / 2.0)
    d = (dx * dx + dy * dy) ** 0.5
    # noisy edge radius between ~0.72 and ~1.0
    ang_bucket = int(((__import__("math").atan2(dy, dx) + 3.14159) / 6.2832) * 24)
    wobble = Rng(salt * 977 + ang_bucket * 131).float()
    edge = 0.74 + 0.24 * wobble
    if d <= edge - 0.16:
        return 255
    if d >= edge:
        return 0
    t = (edge - d) / 0.16
    # dither the falloff instead of smooth alpha: pixel-art style edges
    speck = Rng((x * 7349 + y * 12611) ^ (salt * 0x9E37)).float()
    return 255 if speck < t else 0


def build_splat(path, material, variants=3, salt=0x51A7):
    sheet = Canvas(BLOCK_W, BLOCK_H * variants)
    for v in range(variants):
        vsalt = salt + v * 7919
        base_y = v * BLOCK_H
        # right: 4x3 full tiles
        for cx in range(4):
            for cy in range(3):
                material(sheet, 96 + cx * 32, base_y + cy * 32, vsalt + cx * 17 + cy * 53)
        # left: blob — paint material across 96x96, then mask by blob alpha
        blob = Canvas(BLOB_SIZE, BLOB_SIZE)
        for cx in range(3):
            for cy in range(3):
                material(blob, cx * 32, cy * 32, vsalt + 900 + cx * 17 + cy * 53)
        for x in range(BLOB_SIZE):
            for y in range(BLOB_SIZE):
                a = _blob_alpha(x, y, vsalt)
                if a > 0:
                    r, g, b, _ = blob.get(x, y)
                    sheet.put(x, base_y + y, (r, g, b, a))
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


def material_cloudturf(c, x0, y0, salt):
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


def material_skystone(c, x0, y0, salt):
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


def material_stormslate(c, x0, y0, salt):
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


def material_checkered(c, x0, y0, salt):
    """Black/white marble checker, 16px squares, subtle veining."""
    rng = Rng(salt)
    for x in range(32):
        for y in range(32):
            dark = ((x0 + x) // 16 + (y0 + y) // 16) % 2 == 0
            c.put(x0 + x, y0 + y, palette.MARBLE_DARK if dark else palette.MARBLE_LIGHT)
    for _ in range(rng.range(2, 4)):
        x = x0 + rng.range(2, 28)
        y = y0 + rng.range(2, 28)
        dark = ((x) // 16 + (y - y0 + y0) // 16) % 2 == 0
        vein = (74, 72, 84) if ((x // 16 + y // 16) % 2 == 0) else (198, 196, 204)
        for i in range(rng.range(2, 4)):
            c.put(x + i, y + i // 2, vein)


def material_gloomwood(c, x0, y0, salt):
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
    # staggered vertical seams per board
    for board in range(4):
        seam_x = (Rng(0x600D + board * 31 + (y0 // 96) * 7).next() % 4) * 8 + 4
        for yy in range(board * 8 + 1, board * 8 + 8):
            c.put(x0 + (seam_x + (x0 % 32)) % 32, y0 + yy, ramp["deep"])
    for _ in range(rng.range(3, 5)):
        x = x0 + rng.range(1, 30)
        y = y0 + rng.range(1, 30)
        c.put(x, y, ramp["hi"] if rng.chance(0.3) else ramp["deep"])


def material_mist(deep):
    def painter(c, x0, y0, salt):
        ramp = palette.MISTSEA
        base = ramp["deep"] if deep else ramp["base"]
        rng = Rng(salt)
        for x in range(32):
            for y in range(32):
                c.put(x0 + x, y0 + y, base)
        for _ in range(rng.range(4, 6)):
            x = rng.range(0, 20)
            y = y0 + rng.range(2, 29)
            tone = rng.pick((ramp["light"], ramp["hi"]) if not deep else (ramp["base"], ramp["light"]))
            for i in range(rng.range(6, 14)):
                c.put(x0 + (x + i) % 32, y, tone)
        c.put(x0 + rng.range(2, 29), y0 + rng.range(2, 29), ramp["hi"])
    return painter
