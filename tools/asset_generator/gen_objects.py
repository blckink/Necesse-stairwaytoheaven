"""Object sprites: stairway pair, crystal clusters, sky reeds.

Formats (docs/research/asset-formats.md):
- Stairway (ladder pair): 32 wide, height = 32 + upper art. Top-left 32x32 is
  the floor part drawn on the tile; everything below y=32 is drawn standing on
  the tile (bottom-aligned).
- Crystal cluster: variants * 64 wide, 48 tall, bottom-anchored. Each variant
  slot holds two 32px columns: even = normal object, odd = the "r" variant.
- GrassObject: N * 32 wide, 32 tall (undergroundPixels = 0).
"""

from px import Canvas, Rng, with_alpha
import palette


# --- Stairway to Heaven ------------------------------------------------------

def _stair_steps(c, rng, ramp, x_start, y_bottom, rising_right):
    """One connected staircase body seen from the side, glowing softly.

    The flight is drawn as a single filled stepped polygon (tread + riser per
    step, mass continuous down to the flight's underside), so it reads as one
    solid silhouette instead of floating slabs. Treads get a light lip, risers
    fall into shadow, and a faint glow seam runs along the leading edge.
    """
    stone = palette.SKYSTONE
    dir_ = 1 if rising_right else -1
    tread_w = 7
    rise = 7
    steps = []
    x = x_start
    y = y_bottom
    for i in range(8):
        steps.append((x, y))
        x += dir_ * 4
        x = max(2, min(30 - tread_w, x))
        y -= rise
    # filled body: every step column extends down to just below the next
    # step's level, forming a continuous zig-zag mass ~2 steps thick
    for i, (sx, sy) in enumerate(steps):
        depth = rise + 6
        for dx in range(tread_w):
            for dy in range(depth):
                c.put(sx + dx, sy + dy - 0, ramp["base"])
    # shading pass: treads light, undersides dark
    for i, (sx, sy) in enumerate(steps):
        for dx in range(tread_w):
            c.put(sx + dx, sy, ramp["light"])
            c.put(sx + dx, sy - 1, ramp["hi"] if dx % 3 == 0 else ramp["light"])
            c.put(sx + dx, sy + rise + 4, stone["deep"])
            c.put(sx + dx, sy + rise + 5, stone["deep"])
        # glow seam on the leading edge of each tread
        lead = sx + (tread_w - 1 if rising_right else 0)
        c.put(lead, sy - 1, ramp["glow"])
        c.put(lead, sy, ramp["glow"])
    c.outline(palette.OUTLINE)
    # crowning shimmer above the top step
    tx, ty = steps[-1]
    c.put(tx + tread_w // 2, ty - 4, ramp["hi"])
    c.put(tx + tread_w // 2 - 2, ty - 3, ramp["glow"])
    c.put(tx + tread_w // 2 + 2, ty - 5, ramp["glow"])


def _floor_plate(c, ramp, y0=0):
    """32x32 floor part: a worn skystone ring with a faint glow seam."""
    stone = palette.SKYSTONE
    rng = Rng(0xF10E)
    c.ellipse(15.5, y0 + 15.5, 13, 10, stone["base"])
    c.ellipse(15.5, y0 + 14.5, 10, 7.5, stone["light"])
    c.ellipse(15.5, y0 + 14.0, 6.5, 4.5, ramp["base"])
    c.ellipse(15.5, y0 + 13.5, 4.0, 2.6, ramp["glow"])
    for _ in range(10):
        x = rng.range(4, 27)
        yy = y0 + rng.range(7, 24)
        c.put(x, yy, stone["deep"])
    c.outline(palette.OUTLINE)


def gen_stairway_down(path):
    """Surface-side stairway: floor plate + stairs rising into a cloud wisp."""
    c = Canvas(32, 96)
    ramp = palette.STAIRLIGHT
    rng = Rng(0x57A1)
    # upper section (y 32..95), bottom-aligned on the tile
    _stair_steps(c, rng, ramp, 4, 90, rising_right=True)
    # cloud wisp around the top of the flight
    cloud = palette.MISTSEA
    c.ellipse(18, 38, 9, 4, with_alpha(cloud["hi"], 245))
    c.ellipse(9, 41, 6, 3, with_alpha(cloud["light"], 230))
    c.ellipse(26, 41, 5, 2.5, with_alpha(cloud["light"], 210))
    # floor plate LAST so the outline pass stays local to the top-left cell
    _floor_plate(c, ramp)
    c.save(path)


def gen_stairway_up(path):
    """Sky-side stairway: same silhouette, steps descending into the clouds."""
    c = Canvas(32, 96)
    ramp = palette.STAIRLIGHT
    rng = Rng(0x57A2)
    _stair_steps(c, rng, ramp, 15, 90, rising_right=False)
    cloud = palette.MISTSEA
    c.ellipse(9, 86, 7, 3.5, with_alpha(cloud["hi"], 245))
    c.ellipse(23, 90, 8, 4, with_alpha(cloud["light"], 230))
    _floor_plate(c, ramp)
    c.save(path)


# --- Crystal clusters --------------------------------------------------------

def _shard(c, x, y_base, h, w, ramp, lean=0):
    """One faceted shard: a leaning diamond with a light facet line."""
    for i in range(h):
        t = i / max(h - 1, 1)
        half = max(1, round(w * (1.0 - abs(t * 2 - 1))))
        cx = x + round(lean * t)
        for dx in range(-half, half + 1):
            tone = ramp["base"]
            if dx == -half or dx == half:
                tone = ramp["deep"]
            c.put(cx + dx, y_base - i, tone)
        if 0.25 < t < 0.9:
            c.put(cx - max(0, half - 1), y_base - i, ramp["light"])
    c.put(x + round(lean * 0.9), y_base - h + 1, ramp["hi"])


def _crystal_column(salt, ramp):
    c = Canvas(32, 48)
    rng = Rng(salt)
    ground = palette.SKYSTONE
    # rubble base
    c.ellipse(15.5, 43, 9, 3, ground["base"])
    c.ellipse(12, 44, 4, 2, ground["deep"])
    c.ellipse(21, 44, 4, 2, ground["light"])
    # 3 shards, tallest centered
    _shard(c, 15, 42, rng.range(20, 26), 3, ramp, lean=rng.pick((-2, 2)))
    _shard(c, 9, 43, rng.range(12, 16), 2, ramp, lean=rng.pick((-2, -1)))
    _shard(c, 22, 43, rng.range(13, 17), 2, ramp, lean=rng.pick((1, 2)))
    c.outline(palette.OUTLINE)
    # glow motes
    for _ in range(3):
        c.put(rng.range(6, 26), rng.range(14, 34), ramp["hi"])
    return c


def gen_crystal_cluster(path, ramp, salt, variants=2):
    sheet = Canvas(variants * 64, 48)
    for v in range(variants):
        normal = _crystal_column(salt + v * 101, ramp)
        alt = _crystal_column(salt + v * 101 + 55, ramp).mirrored()
        sheet.paste(normal, v * 64, 0)
        sheet.paste(alt, v * 64 + 32, 0)
    sheet.save(path)


def gen_aurorabloom(path, variants=2):
    """Crystalline bloom: geometric petals around a bright core, on a stem."""
    ramp = palette.AURORA
    sheet = Canvas(variants * 64, 48)
    for v in range(variants):
        for col in range(2):
            c = Canvas(32, 48)
            rng = Rng(0xB100 + v * 101 + col * 55)
            cx = 15 + rng.pick((-1, 0, 1))
            top = 28 + rng.pick((-1, 0, 1))
            # short curved stem, 2px thick, head nodding slightly right
            for y in range(top + 2, 45):
                t = (y - top) / 17.0
                sx = cx + round(3 * t * t)
                c.put(sx, y, ramp["deep"])
                c.put(sx + 1, y, ramp["deep"])
            c.ellipse(cx + 3, 44, 4, 1.5, palette.CLOUDTURF["tuft"])
            # crystalline petals: three faceted spikes fanning up-left,
            # up, and right — deliberately asymmetric, with real mass
            for (dx, dy, h, w) in ((-5, -2, 10, 3), (0, -4, 13, 3), (5, -1, 9, 3)):
                for i in range(h):
                    t = i / max(h - 1, 1)
                    half = max(1, round(w * (1.0 - abs(t * 2 - 1))))
                    px_ = cx + dx + round(dx * 0.35 * t)
                    py_ = top + dy - i
                    for ddx in range(-half, half + 1):
                        c.put(px_ + ddx, py_, ramp["deep"] if ddx in (-half, half) else ramp["base"])
                    if 0.3 < t < 0.85:
                        c.put(px_ - half + 1, py_, ramp["light"])
                c.put(cx + dx + round(dx * 0.35), top + dy - h, ramp["hi"])
            # teal core where the petals meet + one hanging bud
            c.ellipse(cx, top - 1, 2.2, 2.0, ramp["teal"])
            c.put(cx, top - 2, ramp["hi"])
            c.put(cx + 5, top + 4, ramp["base"])
            c.put(cx + 5, top + 5, ramp["teal"])
            c.outline(palette.OUTLINE)
            sheet.paste(c if col == 0 else c.mirrored(), v * 64 + col * 32, 0)
    sheet.save(path)


# --- Sky reeds ---------------------------------------------------------------

def gen_skyreeds(path, variants=4):
    ramp = palette.WINDSILK
    tuft = palette.CLOUDTURF
    sheet = Canvas(variants * 32, 32)
    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0x4EED + v * 313)
        # grounding tuft so the reeds sit in the turf
        c.ellipse(16, 29, 8, 2, tuft["tuft"])
        for _ in range(rng.range(5, 7)):
            x = rng.range(7, 23)
            base_y = rng.range(26, 29)
            h = rng.range(12, 19)
            lean = rng.pick((-3, -2, 2, 3))
            for i in range(h):
                t = i / h
                sx = x + round(lean * t * t * 1.6)
                # 2px thick blades that thin to 1px at the tip
                tone = ramp["deep"] if t < 0.3 else (ramp["base"] if t < 0.7 else ramp["light"])
                c.put(sx, base_y - i, tone)
                if t < 0.65:
                    c.put(sx + 1, base_y - i, tone)
            c.put(x + round(lean * 1.6), base_y - h, ramp["hi"])
        sheet.paste(c, v * 32, 0)
    sheet.save(path)
