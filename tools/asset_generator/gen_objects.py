"""Object sprites: stairway pair, crystal clusters, sky reeds.

Formats (docs/research/asset-formats.md):
- Stairway (ladder pair): 32 wide, height = 32 + upper art. Top-left 32x32 is
  the floor part drawn on the tile; everything below y=32 is drawn standing on
  the tile (bottom-aligned).
- Crystal cluster: variants * 64 wide, 48 tall, bottom-anchored. Each variant
  slot holds two 32px columns: even = normal object, odd = the "r" variant.
- GrassObject: N * 32 wide, 32 tall (undergroundPixels = 0).
"""

import math

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
    """128x32 grass strip: 4 variants of a wheat-grass CLUMP. Stalks fan out
    of one root and arc toward their lean (t^2 bend), heights vary widely,
    heavy seed heads nod sideways, and 1px awn whiskers poke off the grain.
    Like vanilla grass there is no generic outline pass — the deep ramp edge
    carries the silhouette (an outline pass would eat the awns and tips)."""
    sheet = Canvas(128, 32)
    W = palette.WINDWHEAT
    for v in range(4):
        c = Canvas(32, 32)
        rng = Rng(0x3EA7 + v * 131)
        root = 15 + rng.pick((-2, 0, 1))
        n = rng.range(5, 6)
        stalks = []
        for s in range(n):
            bx = root + round((s - (n - 1) / 2.0) * 3.2) + rng.range(-1, 1)
            h = rng.range(9, 18)
            lean = round((s - (n - 1) / 2.0) * 2.2) + rng.range(-2, 2)
            lean = max(-6, min(6, lean))
            if lean == 0:
                lean = rng.pick((-1, 1))
            stalks.append((h, bx, lean, rng.chance(0.8)))
        for (h, bx, lean, headed) in sorted(stalks):     # tall stalks in front
            base_y = 29 + rng.pick((0, 0, -1))
            top_x = bx
            for i in range(h):
                t = i / h
                top_x = bx + round(lean * t * t)         # arc, not a tilt
                mid = W["base"] if i < h - 4 else W["light"]
                c.put(top_x - 1, base_y - i, W["deep"])
                c.put(top_x, base_y - i, mid)
                if i < h - 2:
                    c.put(top_x + 1, base_y - i, W["deep"] if i < 3 else W["base"])
            hy = base_y - h
            if headed:
                nod = (1 if lean > 0 else -1) * (2 if abs(lean) > 3 else 1)
                hx = top_x + nod
                for (dx, dy) in ((0, 0), (1, 0), (-1, 1), (0, 1), (1, 1), (2, 1),
                                 (-1, 2), (0, 2), (1, 2), (0, 3), (1, 3)):
                    c.put(hx + dx, hy - 3 + dy, W["head"])
                c.put(hx, hy - 4, W["light"])
                c.put(hx + (2 if nod > 0 else -1), hy - 1, W["base"])
                c.put(hx + nod, hy - 5, W["deep"])        # awn whiskers
                c.put(hx + nod * 2, hy - 4, W["deep"])
            else:
                c.put(top_x, hy - 1, W["light"])          # bare blade tip
        # irregular root tufts (no dotted line)
        for _ in range(rng.range(3, 4)):
            tx = root + rng.range(-9, 9)
            c.put(tx, 30, W["deep"])
            c.put(tx + 1, 30, W["deep"])
            c.put(tx + rng.pick((0, 1)), 29, W["deep"])
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_cloudberrybush(path):
    """64x32 strip: 2 variants of a low cloudberry bush. Canopy built from a
    ring of jittered lobes (lumpy silhouette, mirror-free), dappled with a
    broken leaf-scale texture; berries sit in clusters of 2-3 like vanilla
    berry bushes, each a 2x2 ball with glint and rim shadow."""
    sheet = Canvas(64, 32)
    B = palette.CLOUDBERRY
    for v in range(2):
        c = Canvas(32, 32)
        rng = Rng(0xBE44 + v * 977)
        lobes = [(16, 21, 7, 4.5)]
        for (lx, ly) in ((9, 20), (13, 17), (18, 16), (23, 19), (25, 22), (7, 23)):
            lobes.append((lx + rng.range(-1, 1), ly + rng.range(0, 1),
                          rng.range(3, 4), 2.6))
        for (lx, ly, r, ry) in lobes:            # deep under-lobes = volume
            c.ellipse(lx + 1, ly + 1, r, ry, B["leaf_deep"])
        for (lx, ly, r, ry) in lobes:
            c.ellipse(lx, ly, r, ry, B["leaf"])
        # dappled leaf-scale texture, broken so it never reads as a grid
        for x in range(3, 29):
            for y in range(12, 28):
                if c.get(x, y)[:3] == B["leaf"][:3] and (x + 2 * y) % 5 == 0 \
                        and rng.chance(0.8):
                    c.put(x, y, B["leaf_deep"])
        c.ellipse(13, 26, 7, 2, B["leaf_deep"])  # shaded underside
        c.put(15, 28, B["leaf_deep"])            # stem base peeking out
        c.put(16, 28, B["leaf_deep"])
        # berries: two clusters + one loner
        pts = []
        for (bx0, by0) in ((11 + rng.range(-1, 1), 20), (21 + rng.range(-1, 1), 17)):
            for k in range(rng.range(2, 3)):
                pts.append((bx0 + (k * 3) % 5 - 1, by0 + (k * 2) % 3))
        pts.append((16 + rng.pick((-5, 4)), 23))
        for (bx, by) in pts:
            c.rect(bx, by, 2, 2, B["berry"])
            c.put(bx + 1, by + 1, B["berry_deep"])
            c.put(bx, by, B["berry_hi"])
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
    """N*32 strip: silky reeds in TWO uneven clumps. Blades fan outward from
    their clump root, arc with per-blade curvature, and half of them hook
    over at the tip like vanilla tall grass. No outline pass (grass rule)."""
    ramp = palette.WINDSILK
    tuft = palette.CLOUDTURF
    sheet = Canvas(variants * 32, 32)
    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0x4EED + v * 313)
        # two uneven grounding tufts over a deep shadow that anchors the
        # pale blades on pale cloudturf at 1x
        c.ellipse(12 + rng.range(-2, 1), 30, 6.5, 1.6, tuft["deep"])
        c.ellipse(21 + rng.range(-1, 2), 30.5, 5.5, 1.4, tuft["deep"])
        c.ellipse(11 + rng.range(-2, 1), 29, 6, 1.6, tuft["tuft"])
        c.ellipse(22 + rng.range(-1, 2), 30, 5, 1.4, tuft["tuft"])
        blades = []
        for (cx0, cnt) in ((rng.range(9, 12), rng.range(3, 4)),
                           (rng.range(19, 22), rng.range(2, 3))):
            for _ in range(cnt):
                blades.append((rng.range(8, 19), cx0 + rng.range(-2, 2), cx0))
        for (h, bx, cx0) in sorted(blades):          # tall blades in front
            base_y = rng.range(27, 29)
            out = 1 if bx >= cx0 else -1
            lean = out * rng.range(2, 5)
            bend = rng.pick((1.6, 2.0, 2.4))
            hook = rng.chance(0.5)
            x = bx
            y = base_y
            for i in range(h):
                t = i / h
                x = bx + round(lean * (t ** bend))
                if hook and i > h - 3:
                    x += out * (i - (h - 3))         # tip curls over
                y = base_y - i
                tone = ramp["deep"] if t < 0.35 else (ramp["base"] if t < 0.7 else ramp["light"])
                c.put(x, y, tone)
                if t < 0.6:
                    c.put(x + 1, y, ramp["deep"] if t < 0.45 else ramp["base"])
            c.put(x, y, ramp["hi"])                  # glint ON the tip pixel
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


# --- World-map icons ---------------------------------------------------------

def gen_mapicons(dir_path):
    """32x32 world-map icons in the vanilla ui/mapicons style: chunky flat
    silhouette, 2-3 tones, dark outline — readable at map zoom."""
    # Warden's Spire: slim tower with a pointed roof and teal beacon light
    c = Canvas(32, 32)
    st = palette.SKYSTONE
    au = palette.AURORA
    c.rect(11, 12, 10, 15, st["base"])          # tower body
    c.rect(11, 12, 3, 15, st["light"])          # lit left face
    c.rect(19, 12, 2, 15, st["deep"])           # shaded right edge
    c.rect(9, 26, 14, 3, st["deep"])            # footing
    for i in range(5):                          # pointed roof
        c.rect(11 + i, 10 - i, 10 - 2 * i, 2, st["deep"])
    c.rect(13, 3, 6, 3, au["teal"])           # beacon light at the tip
    c.rect(14, 2, 4, 1, au["hi"])
    c.rect(14, 17, 4, 6, st["deep"])            # tall door
    c.put(15, 19, au["teal"])                   # lit doorway glint
    c.outline(palette.OUTLINE)
    c.save(f"{dir_path}/skyspire.png")

    # Return stairway: a zigzag of four distinct marble steps on a cloud puff,
    # rising left -> right. Each step is a light top slab over a mid riser
    # with a deep shadow under the slab so the steps separate at map zoom.
    c = Canvas(32, 32)
    sl = palette.STAIRLIGHT
    mi = palette.MISTSEA
    steps = ((3, 21), (10, 16), (17, 11), (24, 6))
    for (sx, sy) in steps:
        c.rect(sx, sy + 2, 7, 4, sl["base"])    # riser front
        c.rect(sx, sy + 5, 7, 1, sl["deep"])    # foot shadow line
        c.rect(sx, sy, 8, 2, sl["light"])       # top slab
        c.rect(sx, sy + 2, 8, 1, sl["deep"])    # shadow under the slab lip
    c.rect(10, 22, 21, 2, sl["deep"])           # side face under the flight
    c.rect(17, 17, 14, 5, sl["base"])
    c.rect(24, 12, 7, 5, sl["base"])
    c.ellipse(9, 27, 8, 3, mi["hi"])            # cloud puff at the base
    c.ellipse(20, 28, 8, 2.8, mi["hi"])
    c.ellipse(15, 28, 5, 2.2, mi["light"])
    c.outline(palette.OUTLINE)
    c.save(f"{dir_path}/skystairs.png")


# --- v0.4 "The Living Sky" flora ---------------------------------------------
# GrassObject strips (N * 32 wide, 32 tall, ground-anchored). Flowers and
# grasses follow the vanilla no-outline rule (windwheat/skyreeds build: the
# deep ramp edge carries the silhouette; a generic outline pass would eat
# stems and petal tips). Static moss is a solid mound object and DOES keep
# the outline. Shade steps for the single-tone SKYTULIP colors borrow their
# dark/hi partners from AURORA / CLOUDBERRY / WINDSILK (all in palette.py).

def _ground_tufts(c, rng, ramp_deep, ramp_tuft, cx, spread=8):
    """Two uneven grounding tufts + shadow (the skyreeds anchor trick)."""
    c.ellipse(cx - rng.range(2, 4), 30, spread * 0.7, 1.5, ramp_deep)
    c.ellipse(cx + rng.range(3, 5), 30.5, spread * 0.55, 1.3, ramp_deep)
    c.ellipse(cx - rng.range(1, 3), 29.5, spread * 0.6, 1.4, ramp_tuft)
    c.ellipse(cx + rng.range(2, 4), 30, spread * 0.45, 1.2, ramp_tuft)


def _arc_stem(c, x0, y0, h, lean, deep, light, bend=2.0, thick=True):
    """Arced 2px stem rising from (x0, y0); returns the tip (x, y)."""
    x = x0
    for i in range(h):
        t = i / max(h - 1, 1)
        x = x0 + round(lean * (t ** bend))
        y = y0 - i
        c.put(x, y, light if t > 0.4 else deep)
        if thick:
            c.put(x + 1, y, deep)
    return x, y0 - h + 1


def gen_cloudbell(path, variants=2):
    """64x32: nodding blue bell flowers on arced stems (Driftlands)."""
    sheet = Canvas(variants * 32, 32)
    B = palette.CLOUDBELL
    S = palette.SKYTULIP
    turf = palette.CLOUDTURF

    def bell(c, bx, by):
        """One hanging bell, 5x6, mouth down: light left, deep right/mouth."""
        for dy in range(5):
            for dx in range(5):
                if dy == 0 and dx in (0, 4):
                    continue
                tone = B["base"]
                if dx == 0:
                    tone = B["light"]
                elif dx >= 3:
                    tone = B["deep"]
                if dy >= 4:
                    tone = B["deep"]
                c.put(bx + dx, by + dy, tone)
        c.put(bx + 1, by + 1, B["hi"])                 # glint
        c.put(bx, by + 5, B["deep"])                   # flared mouth tips
        c.put(bx + 4, by + 5, B["deep"])
        c.put(bx + 2, by + 5, B["hi"])                 # clapper peeking out

    for v in range(2):
        c = Canvas(32, 32)
        rng = Rng(0xC10B + v * 977)
        root = 15 + rng.pick((-2, 1))
        _ground_tufts(c, rng, turf["deep"], turf["tuft"], root)
        # two nodding stems, one tall one short, opposite leans
        for (dx0, h, lean) in ((-2, rng.range(15, 18), rng.range(4, 6)),
                               (3, rng.range(9, 12), -rng.range(3, 5))):
            x0 = root + dx0
            tip = _arc_stem(c, x0, 29, h, lean, S["stem_deep"], S["stem"])
            # the stem hooks over; the bell hangs from the hook tip
            hx = tip[0] + (2 if lean > 0 else -2)
            c.put(tip[0] + (1 if lean > 0 else -1), tip[1], S["stem"])
            bell(c, hx - 2, tip[1] + 1)
        # one mid-stem bud (closed bell)
        bx = root + rng.pick((-4, 5))
        c.put(bx, 21, B["deep"])
        c.put(bx + 1, 21, B["base"])
        c.put(bx, 20, B["light"])
        c.put(bx + 1, 20, B["light"])
        # leaf blades from the root
        for lean in (-3, 2):
            _arc_stem(c, root + lean, 30, rng.range(5, 7), lean,
                      S["stem_deep"], S["stem"], thick=False)
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_skytulip(path):
    """96x32: sky tulips in three color variants (rose / gold / white).
    Single-tone petal colors take their shade partners from the AURORA,
    CLOUDBERRY and WINDSILK ramps."""
    sheet = Canvas(96, 32)
    S = palette.SKYTULIP
    turf = palette.CLOUDTURF
    colorways = (
        (S["rose"], palette.AURORA["base"], palette.AURORA["hi"]),
        (S["gold"], palette.CLOUDBERRY["berry_deep"], palette.CLOUDBERRY["berry_hi"]),
        (S["white"], palette.WINDSILK["base"], palette.WINDSILK["hi"]),
    )

    def tulip(c, tx, ty, base, deep, hi):
        """Classic cup with three petal tips, 5 wide x 6 tall, top at ty."""
        for dy in range(1, 5):
            for dx in range(5):
                tone = base if dx < 3 and dy < 3 else deep
                if dx == 0 and dy < 4:
                    tone = base
                c.put(tx + dx, ty + dy, tone)
        c.put(tx, ty, base)                            # side petal tips
        c.put(tx + 4, ty, deep)
        c.put(tx + 2, ty, base)                        # center petal tip
        c.put(tx + 1, ty + 1, hi)                      # top-left glint
        c.put(tx, ty + 5, deep)                        # rounded cup bottom
        c.put(tx + 4, ty + 5, deep)
        for dx in range(1, 4):
            c.put(tx + dx, ty + 5, deep)

    for v, (base, deep, hi) in enumerate(colorways):
        c = Canvas(32, 32)
        rng = Rng(0x7011F + v * 1499)
        root = 15 + rng.pick((-1, 1))
        _ground_tufts(c, rng, turf["deep"], turf["tuft"], root)
        # one tall + one short tulip, opposite leans
        for (dx0, h, lean) in ((-3, rng.range(12, 14), rng.range(1, 2)),
                               (4, rng.range(7, 9), -rng.range(1, 2))):
            x0 = root + dx0
            tip = _arc_stem(c, x0, 29, h, lean, S["stem_deep"], S["stem"])
            tulip(c, tip[0] - 2, tip[1] - 5, base, deep, hi)
            # one leaf blade per stem
            blean = -3 if dx0 < 0 else 3
            _arc_stem(c, x0 + (1 if blean > 0 else 0), 29, rng.range(5, 7),
                      blean, S["stem_deep"], S["stem"], thick=False)
        # a closed bud low between them
        bx = root + rng.pick((0, 1))
        c.put(bx, 24, S["stem_deep"])
        c.put(bx, 23, deep)
        c.put(bx, 22, base)
        c.put(bx + 1, 22, deep)
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_staticmoss(path, variants=2):
    """64x32: low glowing moss mounds with static-charge sparks (Stormveil).
    Solid ground-hugging object -> KEEPS the outline pass."""
    sheet = Canvas(variants * 32, 32)
    M = palette.STATICMOSS
    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0x57A7 + v * 733)
        mounds = [(15 + rng.range(-2, 2), 26, 9, 4.5),
                  (7 + rng.range(-1, 1), 28, 5, 2.6),
                  (24 + rng.range(-1, 1), 27.5, 6, 3.2)]
        for (mx, my, r, ry) in mounds:                 # deep under-bodies
            c.ellipse(mx + 1, my + 1, r, ry, M["deep"])
        for (mx, my, r, ry) in mounds:
            c.ellipse(mx, my, r, ry, M["base"])
        for (mx, my, r, ry) in mounds:                 # lit top-left crowns
            c.ellipse(mx - r * 0.3, my - ry * 0.4, r * 0.5, ry * 0.5, M["light"])
        # mossy scale dapple, broken so it never reads as a grid
        for x in range(2, 30):
            for y in range(20, 31):
                if c.get(x, y)[:3] == M["base"][:3] and (x + 2 * y) % 5 == 0 \
                        and rng.chance(0.7):
                    c.put(x, y, M["deep"])
        c.outline(palette.OUTLINE)
        # sparks AFTER the outline: charge motes crawling on the moss
        for (sx, sy) in ((mounds[0][0] - 3, 22), (mounds[0][0] + 4, 24),
                         (mounds[2][0], 25)):
            sx += rng.range(-1, 1)
            c.put(sx, sy, M["spark"])
        # one tiny 2px arc jumping off the crown + faint floating mote
        ax = mounds[0][0] + rng.pick((-2, 2))
        c.put(ax, 19, M["spark"])
        c.put(ax + 1, 18, with_alpha(M["spark"], 170))
        c.put(mounds[1][0], 23, with_alpha(M["spark"], 120))
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_thunderbloom(path, variants=2):
    """64x32: sparking violet flower (Stormveil). Spiky petals are drawn
    silhouette-first (deep mass, then lit core) — no outline pass."""
    sheet = Canvas(variants * 32, 32)
    T = palette.THUNDERBLOOM
    slate = palette.STORMSLATE

    def bloom(c, rng, hx, hy, r):
        """Petal burst around (hx, hy): 5 spikes, deep mass + lit core."""
        angs = (90, 30, 150, -20, 200)
        for k, a in enumerate(angs):
            ln = r if k < 3 else r - 1
            dx = math.cos(math.radians(a))
            dy = -math.sin(math.radians(a))
            for i in range(ln):                        # deep spike mass
                x, y = round(hx + dx * i), round(hy + dy * i)
                c.put(x, y, T["deep"])
                c.put(x + 1, y, T["deep"])
            for i in range(ln - 1):                    # lit core
                x, y = round(hx + dx * i), round(hy + dy * i)
                c.put(x + (1 if dx > 0.3 else 0), y, T["base"] if i > 0 else T["light"])
            tx, ty = round(hx + dx * ln), round(hy + dy * ln)
            c.put(tx, ty, T["light"] if k % 2 else T["spark"])
        c.put(hx, hy, T["spark"])                      # charged heart
        c.put(hx + 1, hy, T["light"])
        c.put(hx, hy - 1, T["light"])

    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0x7B100 + v * 1237)
        root = 15 + rng.pick((-1, 2))
        # charged-slate rubble anchor instead of turf
        c.ellipse(root, 30, 7, 1.6, slate["deep"])
        c.ellipse(root - 3, 29.5, 3, 1.2, slate["light"])
        c.ellipse(root + 4, 30, 3, 1.1, slate["base"])
        lean = rng.pick((-3, 3))
        tip = _arc_stem(c, root, 29, rng.range(11, 13), lean,
                        T["stem"], T["stem"])
        bloom(c, rng, tip[0], tip[1] - 2, 5)
        # smaller side bud on a short stem
        tip2 = _arc_stem(c, root + (3 if lean < 0 else -4), 29,
                         rng.range(6, 8), -lean, T["stem"], T["stem"])
        c.put(tip2[0], tip2[1] - 1, T["deep"])
        c.put(tip2[0] + 1, tip2[1] - 1, T["base"])
        c.put(tip2[0], tip2[1] - 2, T["light"])
        # one stray spark crackling off the bloom
        c.put(tip[0] + rng.pick((-5, 5)), tip[1] - 4, T["spark"])
        c.put(tip[0] + rng.pick((-3, 4)), tip[1] - 6,
              with_alpha(T["spark"], 160))
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_glowfern(path, variants=2):
    """64x32: arced light-emitting fern fronds (Aurora Shoals). No outline;
    2px leaflet ticks keep every free-standing line >= 2px."""
    sheet = Canvas(variants * 32, 32)
    G = palette.GLOWFERN
    turf = palette.CLOUDTURF
    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0x610F + v * 1877)
        root = 15 + rng.pick((-1, 1))
        _ground_tufts(c, rng, turf["deep"], turf["tuft"], root)
        fronds = []
        for k in range(rng.range(3, 4)):
            lean = (-8, 7, -3, 4)[k] + rng.range(-1, 1)
            fronds.append((rng.range(12, 17), lean))
        for (h, lean) in sorted(fronds):               # tall fronds in front
            x0 = root + (1 if lean > 0 else -1) * rng.range(0, 2)
            base_y = 29
            spine = []
            for i in range(h):
                t = i / h
                x = x0 + round(lean * (t ** 1.7))
                y = base_y - i
                spine.append((x, y, t))
            for (x, y, t) in spine:                    # spine: deep -> base
                c.put(x, y, G["deep"] if t < 0.5 else G["base"])
            # paired leaflet ticks, longest mid-frond, none at the tip curl
            for i in range(2, h - 3, 2):
                x, y, t = spine[i]
                ln = 2 if 0.2 < t < 0.7 else 1
                for d in range(1, ln + 1):
                    c.put(x - d, y, G["base"] if d == 1 else G["deep"])
                    c.put(x + d, y, G["light"] if d == 1 else G["base"])
            # curled glowing tip
            tx, ty, _ = spine[-1]
            hook = 1 if lean > 0 else -1
            c.put(tx + hook, ty, G["light"])
            c.put(tx + hook, ty + 1, G["hi"])
            c.put(tx, ty - 1, G["hi"])
        # soft spore glow motes drifting above the fronds
        c.put(root + rng.pick((-4, 4)), 12, with_alpha(G["hi"], 170))
        c.put(root + rng.pick((-2, 6)), 8, with_alpha(G["hi"], 110))
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_auroralily(path, variants=2):
    """64x32: glow lily with a bright core (Aurora Shoals). No outline."""
    sheet = Canvas(variants * 32, 32)
    L = palette.AURORALILY
    turf = palette.CLOUDTURF

    def lily(c, hx, hy):
        """Open six-petal cup seen 3/4 from above, bright core center."""
        # back petals (deep, peeking over the top)
        for (dx, dy) in ((-2, -3), (2, -3), (0, -4)):
            c.put(hx + dx, hy + dy, L["deep"])
            c.put(hx + dx + 1, hy + dy, L["deep"])
        # cup body: base mass with lit top-left rim
        c.ellipse(hx, hy, 3.4, 2.4, L["base"])
        c.ellipse(hx - 1, hy - 1, 2.2, 1.4, L["light"])
        # petal notches: deep separations radiating from the core
        c.put(hx - 3, hy + 1, L["deep"])
        c.put(hx + 3, hy + 1, L["deep"])
        c.put(hx - 1, hy + 2, L["deep"])
        c.put(hx + 2, hy + 2, L["deep"])
        # glowing heart + halo
        c.put(hx, hy, L["core"])
        c.put(hx + 1, hy, L["core"])
        c.put(hx, hy - 1, with_alpha(L["core"], 200))
        c.put(hx - 1, hy, with_alpha(L["core"], 150))
        c.put(hx + 1, hy - 1, with_alpha(L["core"], 120))

    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0xA10A + v * 641)
        root = 15 + rng.pick((-1, 1))
        _ground_tufts(c, rng, turf["deep"], turf["tuft"], root)
        lean = rng.pick((-2, 2))
        tip = _arc_stem(c, root, 29, rng.range(12, 15), lean,
                        L["stem"], L["stem"])
        lily(c, tip[0], tip[1] - 2)
        # drooping unopened bud on a short arc
        tip2 = _arc_stem(c, root + (4 if lean < 0 else -4), 29,
                         rng.range(6, 8), -lean * 2, L["stem"], L["stem"])
        c.put(tip2[0], tip2[1], L["deep"])
        c.put(tip2[0] + 1, tip2[1], L["base"])
        c.put(tip2[0], tip2[1] + 1, L["light"])
        c.put(tip2[0] + 1, tip2[1] + 1, with_alpha(L["core"], 150))
        # one leaf blade
        _arc_stem(c, root + rng.pick((-2, 2)), 30, rng.range(5, 6),
                  rng.pick((-3, 3)), L["stem"], L["stem"], thick=False)
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


# --- v0.4 ore overlays (aetheriumore format: N x 32x32 pattern variants
# on transparency; the engine masks them onto the parent rock) ---------------

def gen_fulguriteore(path, variants=2):
    """64x32: fused lightning-glass veins — jagged branching zigzags with
    glassy nodes, the Stormveil's fulgurite."""
    sheet = Canvas(variants * 32, 32)
    F = palette.FULGURITE
    for v in range(variants):
        x0 = v * 32
        rng = Rng(0xF16 + v * 389)
        starts = ((x0 + rng.range(4, 8), rng.range(3, 7), 1),
                  (x0 + rng.range(18, 22), rng.range(12, 16), -1))
        for (sx, sy, sway) in starts:
            x, y = sx, sy
            step_x = sway
            for i in range(rng.range(10, 13)):
                sheet.put(x, y, F["base"])
                sheet.put(x + 1, y, F["deep"])
                if i % 3 == 0:
                    sheet.put(x, y - 1, F["light"])    # glassy top glint
                if i % 4 == 2:                         # lightning jag
                    step_x = -step_x
                    sheet.put(x - step_x, y, F["deep"])
                x += step_x if i % 2 else 0
                y += 1
                if i == 5:                             # branch spur
                    bx, by = x, y
                    for k in range(3):
                        sheet.put(bx + (k + 1) * -step_x, by + k, F["base"])
                        sheet.put(bx + (k + 1) * -step_x + 1, by + k, F["deep"])
                    sheet.put(bx + 3 * -step_x, by + 2, F["hi"])
            sheet.put(sx, sy, F["hi"])                 # fused node at the top
            sheet.put(sx - 1, sy + 1, F["light"])
        # loose glass beads between the veins
        for _ in range(rng.range(3, 4)):
            bx, by = x0 + rng.range(3, 28), rng.range(3, 28)
            sheet.put(bx, by, F["light"])
            sheet.put(bx + 1, by, F["deep"])
            sheet.put(bx, by - 1, F["hi"] if rng.chance(0.4) else F["light"])
    sheet.save(path)


def gen_prismshardore(path, variants=2):
    """64x32: prismshard crystal clusters — faceted mini-shards with the
    Shoals' teal accent, scattered like vanilla ore nuggets."""
    sheet = Canvas(variants * 32, 32)
    P = palette.PRISMSHARD
    for v in range(variants):
        x0 = v * 32
        rng = Rng(0x9515 + v * 271)
        spots = ((9, 9), (23, 6), (6, 22), (18, 18), (27, 25))
        for (cx, cy) in spots:
            cx += x0 + rng.range(-2, 2)
            cy += rng.range(-2, 2)
            h = rng.range(5, 8)
            lean = rng.pick((-2, -1, 1, 2))
            base_y = cy + h // 2
            for i in range(h):                          # mini faceted shard
                t = i / max(h - 1, 1)
                half = max(1, round(2 * (1.0 - abs(t * 2 - 1))))
                px_ = cx + round(lean * t)
                for dx in range(-half, half + 1):
                    tone = P["deep"] if dx in (-half, half) else P["base"]
                    sheet.put(px_ + dx, base_y - i, tone)
                if 0.3 < t < 0.85:
                    sheet.put(px_ - half + 1, base_y - i, P["light"])
            sheet.put(cx + lean, base_y - h + 1, P["hi"])
            if rng.chance(0.6):                        # teal refraction glint
                sheet.put(cx - 1, base_y - 1, P["teal"])
        for _ in range(rng.range(2, 4)):               # sparkle dust
            sheet.put(x0 + rng.range(3, 28), rng.range(3, 28), P["light"])
    sheet.save(path)
