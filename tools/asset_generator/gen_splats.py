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


def build_splat(path, material, variants=3, salt=0x51A7, frames=1, features=None):
    """features(block, x0, y0, salt, k): optional motif painter run on the four
    FULL-variant cells (3..6, 0) only — vanilla's trick: blend cells stay a calm
    base texture, while each full variant carries its own detail cluster."""
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
            if features is not None:
                for k, fcx in enumerate((3, 4, 5, 6)):
                    features(block, fcx * CELL, 0, vsalt + 0xF17 + k * 977, k)
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
    """Calm silver-green turf. All character lives in the full-variant motifs
    (vanilla construction: quiet base, clustered features per variant)."""
    _speckle_cell(c, x0, y0, palette.CLOUDTURF, 0xC10D0000, density=0.04)


def features_cloudturf(c, x0, y0, salt, k):
    ramp = palette.CLOUDTURF
    rng = Rng(salt)

    def tuft(x, y):
        # chunky 4px-tall blade cluster with rooted shadow (vanilla-scale)
        lean = rng.pick((-1, 1))
        c.put(x - 1, y, ramp["tuft"])
        c.put(x, y, ramp["tuft"])
        c.put(x + 1, y, ramp["tuft"])
        c.put(x - lean, y - 1, ramp["tuft"])
        c.put(x + lean, y - 1, ramp["tuft"])
        c.put(x + lean, y - 2, ramp["tuft"])
        c.put(x + lean * 2, y - 3, ramp["hi"])
        c.put(x - 1, y + 1, ramp["deep"])
        c.put(x, y + 1, ramp["deep"])

    if k == 0:  # near-plain
        c.put(x0 + rng.range(6, 26), y0 + rng.range(6, 26), ramp["hi"])
        return
    if k == 1:  # two tuft clusters, each of two neighboring tufts
        for _ in range(2):
            tx, ty = x0 + rng.range(6, 24), y0 + rng.range(7, 26)
            tuft(tx, ty)
            tuft(tx + rng.range(4, 7), ty + rng.pick((-2, 2)))
    elif k == 2:  # soft cloud-moss patch
        mx, my = x0 + rng.range(10, 21), y0 + rng.range(10, 21)
        c.blob(mx, my, 4, ramp["light"], rng, lumps=3)
        c.blob(mx - 1, my - 1, 2, ramp["hi"], rng, lumps=2)
        c.put(mx + 3, my + 3, ramp["deep"])
        for _ in range(3):
            c.put(x0 + rng.range(3, 28), y0 + rng.range(3, 28), ramp["hi"])
    else:  # tuft + pale pebbles
        tuft(x0 + rng.range(5, 26), y0 + rng.range(6, 26))
        for _ in range(3):
            px, py = x0 + rng.range(3, 27), y0 + rng.range(3, 27)
            c.put(px, py, ramp["light"])
            c.put(px + 1, py, ramp["deep"])


def material_skystone(c, x0, y0, salt, frame=0):
    """Calm pale stone; cracks and chips are per-variant features."""
    _speckle_cell(c, x0, y0, palette.SKYSTONE, 0x51A90000, density=0.05)


def features_skystone(c, x0, y0, salt, k):
    ramp = palette.SKYSTONE
    rng = Rng(salt)

    def crack(x, y, length):
        step = rng.pick((-1, 1))
        for i in range(length):
            c.put(x + i, y, ramp["deep"])
            if rng.chance(0.45):
                y += step
        c.put(x + length - 1, y + 1, ramp["light"])

    if k == 0:  # plain slab
        return
    if k == 1:  # one long meandering fissure
        crack(x0 + rng.range(3, 12), y0 + rng.range(8, 24), rng.range(9, 14))
    elif k == 2:  # chipped facet + pebbles
        fx, fy = x0 + rng.range(8, 20), y0 + rng.range(8, 20)
        for i in range(4):
            for j in range(4 - i):
                c.put(fx + i, fy + j, ramp["light"])
        c.put(fx, fy, ramp["hi"])
        c.put(fx + 2, fy + 4, ramp["deep"])
        c.put(fx + 4, fy + 2, ramp["deep"])
        for _ in range(2):
            px, py = x0 + rng.range(3, 27), y0 + rng.range(3, 27)
            c.put(px, py, ramp["light"])
            c.put(px + 1, py + 1, ramp["deep"])
    else:  # two hairline cracks + a glint
        crack(x0 + rng.range(3, 12), y0 + rng.range(5, 14), rng.range(6, 9))
        crack(x0 + rng.range(8, 18), y0 + rng.range(18, 26), rng.range(6, 9))
        c.put(x0 + rng.range(6, 26), y0 + rng.range(6, 26), ramp["hi"])


def material_stormslate(c, x0, y0, salt, frame=0):
    """Layered night-violet slate: position-locked dashed diagonal strata
    (period 16 divides the 32px tile, so it stays seamless)."""
    ramp = palette.STORMSLATE
    _speckle_cell(c, x0, y0, ramp, 0x570A0000, density=0.08)
    for x in range(32):
        for y in range(32):
            gx, gy = (x0 + x) % 32, (y0 + y) % 32
            m = (gx + gy) % 16
            h = Rng((gx * 5081 + gy * 947) ^ 0x570A5EED)
            if m == 0 and h.chance(0.72):
                c.put(x0 + x, y0 + y, ramp["deep"])
            elif m == 1 and h.chance(0.35):
                c.put(x0 + x, y0 + y, ramp["light"])


def features_stormslate(c, x0, y0, salt, k):
    ramp = palette.STORMSLATE
    rng = Rng(salt)
    if k == 0:
        return
    if k == 1:  # charge vein: crack with electric violet pinpricks
        x, y = x0 + rng.range(4, 14), y0 + rng.range(6, 22)
        for i in range(rng.range(8, 12)):
            c.put(x + i, y, ramp["deep"])
            if rng.chance(0.5):
                y += rng.pick((-1, 1))
            if i in (2, 5, 8):
                c.put(x + i, y, ramp["charge"])
        c.put(x + 1, y - 1, ramp["hi"])
    elif k == 2:  # raised shard ridge catching the light
        rx, ry = x0 + rng.range(4, 16), y0 + rng.range(8, 22)
        w = rng.range(7, 11)
        for i in range(w):
            c.put(rx + i, ry, ramp["light"])
            c.put(rx + i, ry + 1, ramp["deep"])
        c.put(rx, ry, ramp["hi"])
    else:  # short crack + lone charge spark + pebble
        x, y = x0 + rng.range(5, 18), y0 + rng.range(5, 24)
        for i in range(rng.range(5, 8)):
            c.put(x + i, y + i // 2, ramp["deep"])
        c.put(x0 + rng.range(4, 27), y0 + rng.range(4, 27), ramp["charge"])
        c.put(x0 + rng.range(4, 27), y0 + rng.range(4, 27), ramp["light"])


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
    for _ in range(rng.range(1, 2)):
        c.put(x0 + rng.range(1, 30), y0 + rng.range(1, 30),
              ramp["hi"] if rng.chance(0.3) else ramp["deep"])


def features_gloomwood(c, x0, y0, salt, k):
    ramp = palette.GLOOMWOOD
    rng = Rng(salt)
    if k == 0:
        return
    if k == 1:  # knot in one board
        kx, ky = x0 + rng.range(6, 24), y0 + rng.range(0, 3) * 8 + 4
        c.put(kx, ky, ramp["deep"])
        c.put(kx + 1, ky, ramp["deep"])
        c.put(kx - 1, ky, ramp["light"])
        c.put(kx + 2, ky, ramp["light"])
        c.put(kx, ky - 1, ramp["light"])
        c.put(kx, ky + 1, ramp["deep"])
    elif k == 2:  # nail heads at a board seam
        for _ in range(2):
            nx, ny = x0 + rng.range(4, 27), y0 + rng.range(0, 3) * 8 + 1
            c.put(nx, ny, ramp["hi"])
            c.put(nx + 1, ny + 1, ramp["deep"])
    else:  # grain streaks along a board
        by = y0 + rng.range(0, 3) * 8 + rng.range(2, 6)
        for _ in range(3):
            gx = x0 + rng.range(2, 24)
            for i in range(rng.range(3, 6)):
                c.put(gx + i, by, ramp["deep"])
            by += rng.pick((-1, 1))


_PUFF_CACHE = {}


def _puff_field(hsalt, big_shift, small_shift):
    """32x32 toroidal 'cloudiness' map: 3x3 big puffs + 5x5 small detail puffs.

    Both layers wrap (distances mod 32), so the tile is seamless; shifting the
    sample point by multiples of 4 px per frame keeps the 8-frame loop seamless
    too (8 * 4 = 32 = one full period).
    """
    key = (hsalt, big_shift, small_shift)
    cached = _PUFF_CACHE.get(key)
    if cached is not None:
        return cached
    layers = []
    # big billow masses, a medium layer breaking their edges, and fine detail
    for grid, rmin, rmax, layer_salt in ((2, 12, 17, 0), (3, 7, 10, 0x77), (5, 3, 5, 0x33)):
        centers = []
        step = 32.0 / grid
        for i in range(grid):
            for j in range(grid):
                r = Rng((hsalt >> 16) ^ (i * 733 + j * 2711 + layer_salt) * 40503)
                cx = (i + 0.5) * step + (r.float() - 0.5) * step * 0.9
                cy = (j + 0.5) * step + (r.float() - 0.5) * step * 0.9
                centers.append((cx % 32, cy % 32, rmin + r.float() * (rmax - rmin)))
        layers.append(centers)

    def torus_d(ax, ay, bx, by):
        dx = abs(ax - bx)
        dy = abs(ay - by)
        dx = min(dx, 32 - dx)
        dy = min(dy, 32 - dy)
        return (dx * dx + dy * dy) ** 0.5

    field = [[0.0] * 32 for _ in range(32)]
    for y in range(32):
        for x in range(32):
            bx, by = (x + big_shift) % 32, y
            mx, my = (x + big_shift) % 32, (y + 5) % 32
            sx, sy = (x + small_shift) % 32, (y + 11) % 32
            big = 0.0
            for (cx, cy, r) in layers[0]:
                big = max(big, 1.0 - torus_d(bx, by, cx, cy) / r)
            mid = 0.0
            for (cx, cy, r) in layers[1]:
                mid = max(mid, 1.0 - torus_d(mx, my, cx, cy) / r)
            small = 0.0
            for (cx, cy, r) in layers[2]:
                small = max(small, 1.0 - torus_d(sx, sy, cx, cy) / r)
            field[y][x] = max(0.0, big) * 0.75 + max(0.0, mid) * 0.45 + max(0.0, small) * 0.22
    _PUFF_CACHE[key] = field
    return field


def material_mist(deep):
    """The Mistsea as a rolling CLOUD deck, not water: bright puffy tops with
    self-shadowed billows. Big puffs drift east, the detail layer drifts west
    (counter-parallax); both loop seamlessly over the 8 liquid frames.
    Deep = the open cloudsea (full contrast between sunlit tops and shadowed
    valleys); shallow = the thinner shore band (compressed to lighter tones)."""
    def painter(c, x0, y0, salt, frame=0):
        ramp = palette.MISTSEA
        hsalt = salt & 0xFFFF0000
        field = _puff_field(hsalt, (frame * 4) % 32, (-frame * 4) % 32)

        def band(gx, gy):
            v = field[gy % 32][gx % 32]
            if not deep:
                v = 0.30 + v * 0.75  # shore mist: thinner, floor-lit
            if v > 0.80:
                return 3
            if v > 0.52:
                return 2
            if v > 0.30:
                return 1
            return 0

        tones = (ramp["deep"], ramp["base"], ramp["light"], ramp["hi"])
        for x in range(32):
            for y in range(32):
                gx, gy = (x0 + x) % 32, (y0 + y) % 32
                b = band(gx, gy)
                col = tones[b]
                # hard sunlit rim on the upper edge of every brightest lobe —
                # the crisp cartoon-cloud top edge
                if b == 3 and band(gx, gy - 1) < 3:
                    col = ramp["top"]
                elif b == 2 and band(gx, gy - 1) < 2:
                    col = ramp["hi"]
                c.put(x0 + x, y0 + y, col)
    return painter
