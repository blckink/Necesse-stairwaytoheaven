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

def _marble_plate(c, ramp):
    """Walkable 32x32 base cell: pale marble plate with corner balusters."""
    iron = palette.IRONWORK
    c.rect(2, 2, 28, 28, ramp["base"])
    c.rect(2, 2, 28, 1, ramp["light"])
    c.rect(2, 29, 28, 1, ramp["deep"])
    c.rect(2, 2, 1, 28, ramp["light"])
    c.rect(29, 2, 1, 28, ramp["deep"])
    # subtle checker sheen
    for x in range(3, 29):
        for y in range(3, 29):
            if ((x // 6) + (y // 6)) % 2 == 0 and (x + y) % 7 == 0:
                c.put(x, y, ramp["light"])
    # worn chips + corner balusters
    c.put(6, 27, ramp["deep"])
    c.put(25, 5, ramp["light"])
    for (bx, by) in ((4, 4), (27, 4), (4, 27), (27, 27)):
        c.put(bx, by, iron["base"])
        c.put(bx, by - 1, iron["light"])


def _flight(c, y_bottom, y_top, wide_at_bottom, curve, ramp, rail_side):
    """A grand stone flight between two heights: 6px steps, perspective taper,
    slight S-curve, iron handrail with posts on one side."""
    iron = palette.IRONWORK
    steps = list(range(y_bottom, y_top, -6))
    n = max(len(steps) - 1, 1)
    rail = []
    for i, sy in enumerate(steps):
        t = i / n
        half = round((13 - 6 * t) if wide_at_bottom else (7 + 6 * t))
        off = round(curve * (t * (1 - t)) * 4)
        cx = 16 + off
        # riser block
        for y in range(sy - 5, sy + 1):
            for dx in range(-half, half + 1):
                tone = ramp["base"]
                if dx <= -half + 1:
                    tone = ramp["light"]
                elif dx >= half - 1:
                    tone = ramp["deep"]
                c.put(cx + dx, y, tone)
        # tread: bright top face + shadow under the nose
        for dx in range(-half, half + 1):
            c.put(cx + dx, sy - 5, ramp["light"])
            c.put(cx + dx, sy - 4, ramp["hi"] if dx % 4 == 0 else ramp["light"])
            c.put(cx + dx, sy + 1, ramp["deep"])
        rail.append((cx + rail_side * (half - 1), sy - 5))
    # handrail: posts + rail line following the flight
    for (rx, ry) in rail:
        for y in range(ry - 5, ry):
            c.put(rx, y, iron["deep"])
        c.put(rx, ry - 6, iron["light"])
    for i in range(len(rail) - 1):
        (x0, y0), (x1, y1) = rail[i], rail[i + 1]
        c.line(x0, y0 - 6, x1, y1 - 6, iron["base"])


def _cloud_puff(c, cx, cy, rx, ry):
    mist = palette.MISTSEA
    c.ellipse(cx, cy, rx, ry, mist["hi"])
    c.ellipse(cx - rx * 0.4, cy - 1, rx * 0.5, ry * 0.8, mist["top"])
    c.ellipse(cx + rx * 0.5, cy + 1, rx * 0.45, ry * 0.7, mist["light"])
    c.put(int(cx - rx * 0.2), int(cy - ry), mist["top"])


def gen_stairway_down(path):
    """Surface-side Stairway to Heaven: a grand pale flight rising through a
    cloud ring into a soft light burst (32x96; bottom 32x32 = floor cell is
    drawn last so its outline stays local)."""
    c = Canvas(32, 96)
    ramp = palette.STAIRLIGHT
    _flight(c, 88, 46, wide_at_bottom=True, curve=2, ramp=ramp, rail_side=1)
    # cloud ring wrapping the flight
    _cloud_puff(c, 7, 62, 6.5, 3.5)
    _cloud_puff(c, 26, 70, 6, 3.2)
    _cloud_puff(c, 24, 52, 5, 2.8)
    c.outline(palette.OUTLINE)
    # light burst at the top (after outline so the glow floats)
    for (gx, gy, a) in ((16, 38, 220), (13, 36, 160), (19, 35, 160),
                        (16, 33, 130), (11, 40, 110), (21, 40, 110)):
        c.put(gx, gy, with_alpha(ramp["glow"], a))
    c.put(16, 30, ramp["hi"])
    c.put(12, 33, ramp["hi"])
    c.put(20, 32, ramp["hi"])
    _marble_plate(c, ramp)
    c.save(path)


def gen_stairway_up(path):
    """Sky-side return stairway: the flight descends from the island's edge
    into the cloud deck below."""
    c = Canvas(32, 96)
    ramp = palette.STAIRLIGHT
    _flight(c, 88, 46, wide_at_bottom=False, curve=-2, ramp=ramp, rail_side=-1)
    # the cloud deck swallows the lowest steps
    _cloud_puff(c, 8, 88, 7.5, 4)
    _cloud_puff(c, 24, 91, 8, 4.2)
    _cloud_puff(c, 16, 94, 9, 3.5)
    c.outline(palette.OUTLINE)
    for (gx, gy, a) in ((16, 40, 180), (12, 38, 120), (20, 37, 120)):
        c.put(gx, gy, with_alpha(ramp["glow"], a))
    _marble_plate(c, ramp)
    c.save(path)


def gen_windwheat(path):
    """128x32 grass strip: 4 variants of tall wheat-grass with seed heads."""
    sheet = Canvas(128, 32)
    W = palette.WINDWHEAT
    for v in range(4):
        c = Canvas(32, 32)
        rng = Rng(0x3EA7 + v * 131)
        # 2px stalks (the outline pass eats 1px lines) with fat seed heads
        for s in range(rng.range(4, 5)):
            x = 6 + s * 6 + rng.range(-1, 1)
            h = rng.range(12, 18)
            lean = rng.pick((-2, -1, 1, 2))
            top_x = x
            for i in range(h):
                top_x = x + round(lean * (i / h))
                # 3px stalk: inside-outline eats both edge columns, the bright
                # core must survive on its own
                mid = W["base"] if i < h - 4 else W["light"]
                c.put(top_x - 1, 29 - i, W["deep"])
                c.put(top_x, 29 - i, mid)
                c.put(top_x + 1, 29 - i, W["deep"] if i < 3 else W["base"])
            # seed head: plump 3x4 grain cluster
            hy = 29 - h
            for (dx, dy) in ((0, 0), (1, 0), (-1, 1), (0, 1), (1, 1), (2, 1),
                             (-1, 2), (0, 2), (1, 2), (0, 3), (1, 3)):
                c.put(top_x + dx, hy - 3 + dy, W["head"])
            c.put(top_x, hy - 4, W["light"])
            c.put(top_x + 2, hy - 1, W["base"])
        # base tuft shadow
        for dx in range(6, 27, 2):
            c.put(dx, 30, W["deep"])
        c.outline(palette.OUTLINE)
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_cloudberrybush(path):
    """64x32 strip: 2 variants of a low berry bush with amber cloudberries."""
    sheet = Canvas(64, 32)
    B = palette.CLOUDBERRY
    for v in range(2):
        c = Canvas(32, 32)
        rng = Rng(0xBE44 + v * 977)
        # foliage mound with lumpy top
        c.ellipse(16, 22, 10, 6, B["leaf"])
        for bx in (-6, -1, 4, 8):
            c.ellipse(16 + bx, 17 + rng.range(0, 2), 3, 2.4, B["leaf"])
        c.ellipse(13, 24, 7, 3.5, B["leaf_deep"])
        # stem base
        c.put(15, 28, B["leaf_deep"])
        c.put(16, 28, B["leaf_deep"])
        # berries: amber clusters with a single highlight each
        for _ in range(rng.range(5, 7)):
            bx = 16 + rng.range(-8, 8)
            by = 18 + rng.range(-3, 4)
            c.put(bx, by, B["berry"])
            c.put(bx + 1, by, B["berry_deep"])
            c.put(bx, by - 1, B["berry_hi"])
        c.outline(palette.OUTLINE)
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


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
