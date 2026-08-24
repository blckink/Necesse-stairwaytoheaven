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


def _mass(c):
    """Opaque-pixel count at the size-audit threshold (alpha > 24)."""
    return sum(1 for x in range(c.width) for y in range(c.height)
               if c.px[x, y][3] > 24)


def gen_windwheat(path):
    """128x32 grass strip: 4 variants of a DENSE wheat-grass clump at vanilla
    swampgrass mass (26x30 bbox, 500+ opaque px per variant — size law).
    A solid skirt of short overlapping blades grounds the clump, 8-9 full
    stalks fan out of the root and arc toward their lean (t^2 bend), heavy
    seed heads nod sideways with 1px awn whiskers. Like vanilla grass there
    is no generic outline pass — the deep ramp edge carries the silhouette
    (an outline pass would eat the awns and tips)."""
    sheet = Canvas(128, 32)
    W = palette.WINDWHEAT
    for v in range(4):
        c = Canvas(32, 32)
        rng = Rng(0x3EA7 + v * 131)
        root = 15 + rng.pick((-1, 0, 1))
        # solid ground ribbon under the skirt anchors the clump at 1x
        c.ellipse(root, 30, 11, 2.2, W["deep"])
        c.ellipse(root - 2, 29, 7, 1.5, W["deep"])
        c.ellipse(root + 3, 29.5, 6, 1.4, W["deep"])
        # solid ground skirt: overlapping short arcs = the clump's dark body
        for _ in range(18):
            bx = root + rng.range(-8, 8)
            sh = rng.range(3, 7)
            lean = rng.pick((-3, -2, -1, 1, 2, 3))
            for i in range(sh):
                t = i / sh
                x = min(28, max(2, bx + round(lean * t * t)))
                c.put(x, 30 - i, W["deep"])
                c.put(x + 1, 30 - i, W["deep"] if i < sh - 2 else W["base"])
        n = rng.range(10, 11)
        stalks = []
        for s in range(n):
            bx = root + round((s - (n - 1) / 2.0) * 2.2) + rng.range(-1, 1)
            h = rng.range(12, 20)
            lean = round((s - (n - 1) / 2.0) * 1.8) + rng.range(-2, 2)
            lean = max(-5, min(5, lean))
            if lean == 0:
                lean = rng.pick((-1, 1))
            stalks.append((h, bx, lean, rng.chance(0.8)))
        for (h, bx, lean, headed) in sorted(stalks):     # tall stalks in front
            base_y = 29 + rng.pick((0, 0, -1))
            top_x = bx
            for i in range(h):
                t = i / h
                top_x = min(27, max(4, bx + round(lean * t * t)))  # arc, not a tilt
                mid = W["base"] if i < h - 4 else W["light"]
                c.put(top_x - 1, base_y - i, W["deep"])
                c.put(top_x, base_y - i, mid)
                if i < h - 2:
                    c.put(top_x + 1, base_y - i, W["deep"] if i < 4 else W["base"])
                if i < h * 0.4:                          # 4px root third
                    c.put(top_x - 2, base_y - i, W["deep"])
            hy = base_y - h
            if headed:
                nod = (1 if lean > 0 else -1) * (2 if abs(lean) > 3 else 1)
                hx = min(25, max(5, top_x + nod))
                # plump grain head: 3-wide kernel stack with shaded right edge
                for (dx, dy) in ((0, 0), (1, 0), (-1, 1), (0, 1), (1, 1), (2, 1),
                                 (-1, 2), (0, 2), (1, 2), (2, 2), (-1, 3), (0, 3),
                                 (1, 3), (0, 4), (1, 4)):
                    c.put(hx + dx, hy - 4 + dy, W["head"])
                c.put(hx, hy - 5, W["light"])
                c.put(hx + 1, hy - 3, W["light"])         # kernel glint
                c.put(hx + (2 if nod > 0 else -1), hy - 1, W["base"])
                c.put(min(27, max(4, hx + nod)), hy - 6, W["deep"])       # awns
                c.put(min(27, max(4, hx + nod * 2)), hy - 5, W["deep"])
            else:
                c.put(top_x, hy - 1, W["light"])          # bare blade tip
        # irregular root tufts (no dotted line)
        for _ in range(rng.range(3, 4)):
            tx = root + rng.range(-9, 9)
            c.put(tx, 30, W["deep"])
            c.put(tx + 1, 30, W["deep"])
            c.put(tx + rng.pick((0, 1)), 29, W["deep"])
        # size-law top-up: grow the ground skirt until the clump carries
        # vanilla swampgrass mass (500+ opaque px), never past the margins
        for _ in range(24):
            if _mass(c) >= 505:
                break
            bx = root + rng.range(-9, 9)
            sh = rng.range(4, 7)
            lean = rng.pick((-3, -2, 2, 3))
            for i in range(sh):
                x = min(28, max(3, bx + round(lean * (i / sh) ** 2)))
                c.put(x, 30 - i, W["deep"])
                c.put(x + 1, 30 - i, W["base"] if i >= sh - 2 else W["deep"])
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

def _shard(c, x, y_base, h, w, ramp, lean=0, seam=False, cut=True, bright=False):
    """One CHUNKY faceted crystal (vanilla amethystcluster construction):
    kite profile — widest about a third of the way up, tapering to a point —
    deep rim edges, a light facet stripe up the left-center, 2px hi tip.
    w is the max HALF-width, so a w=5 shard is 11px across at its belly.
    cut=True stamps a 1px OUTLINE seam wherever this shard overlaps art
    already on the canvas, and bright shards use the light ramp step as
    their body — neighboring shards alternate value like vanilla amethyst,
    so touching spikes never fuse into one cone (the blob failure mode)."""
    body = ramp["light"] if bright else ramp["base"]
    stripe = ramp["hi"] if bright else ramp["light"]
    tip_x = x
    for i in range(h):
        t = i / max(h - 1, 1)
        if t < 0.3:
            frac = 0.45 + 0.55 * (t / 0.3)           # base -> belly
        else:
            frac = (1.0 - (t - 0.3) / 0.7) ** 0.85   # belly -> point
        half = max(1, round(w * frac))
        cx = x + round(lean * t)
        tip_x = cx
        if cut:
            for sx in (cx - half - 1, cx + half + 1):
                if c.filled(sx, y_base - i):
                    c.put(sx, y_base - i, palette.OUTLINE)
        for dx in range(-half, half + 1):
            tone = body
            if dx == -half or dx == half:
                tone = ramp["deep"]
            elif dx == -half + 1 and 0.08 < t < 0.85:
                tone = stripe                         # lit facet stripe
            elif seam and dx == max(1, half - 2) and t < 0.6:
                tone = ramp["deep"] if not bright else ramp["base"]
            c.put(cx + dx, y_base - i, tone)
    c.put(tip_x, y_base - h + 1, ramp["hi"])          # 2px bright tip cap
    c.put(tip_x, y_base - h, ramp["hi"])


def _crystal_column(salt, ramp):
    """32x48 half of a 2x1 crystal cluster, rebuilt at vanilla mass
    (amethystcluster half: ~30x58, ~1170 opaque; crystalwall cell: 28x42,
    1116). A dark slate rubble bed grounds 6-7 FAT shards in a staggered
    skyline — every shard its own spike, separated by cut seams."""
    c = Canvas(32, 48)
    rng = Rng(salt)
    ground = palette.STORMSLATE
    # rubble bed: overlapping rock mounds with lit tops and corner stones
    c.ellipse(16, 43.5, 15, 4.4, ground["deep"])
    c.ellipse(9, 42, 6.5, 3.0, ground["base"])
    c.ellipse(22, 42.5, 7, 3.2, ground["base"])
    c.ellipse(16, 45, 9, 2.6, ground["deep"])
    c.ellipse(9, 40.5, 4, 1.4, ground["light"])
    c.ellipse(23, 41, 3.5, 1.3, ground["light"])
    c.rect(2, 43, 4, 3, ground["base"])
    c.rect(26, 43, 4, 3, ground["deep"])
    c.put(2, 43, ground["light"])
    c.put(3, 43, ground["light"])
    c.put(26, 43, ground["base"])
    # shards back-to-front, staggered heights + alternating bright/dark
    # bodies so the skyline reads as separate spikes, never one cone
    _shard(c, 10, 40, rng.range(25, 28), 5, ramp, lean=rng.pick((-2, -1)))
    _shard(c, 21, 40, rng.range(19, 22), 5, ramp, lean=rng.pick((1, 2)), bright=True)
    _shard(c, 16, 43, rng.range(36, 40), 7, ramp, lean=rng.pick((-1, 1)), seam=True, bright=True)
    _shard(c, 7, 44, rng.range(17, 20), 4, ramp, lean=rng.pick((-2, -1)), bright=True)
    _shard(c, 24, 44, rng.range(27, 30), 6, ramp, lean=rng.pick((1, 2)), seam=True)
    _shard(c, 11, 46, rng.range(9, 11), 3, ramp, lean=rng.pick((-1, 1)), bright=True)
    _shard(c, 20, 47, rng.range(7, 8), 2, ramp, lean=1)
    # size-law top-up: sprout extra front nubs out of the rubble until the
    # cluster carries vanilla crystal mass (~900 opaque px per half)
    for (nx, ny) in ((4, 47), (15, 47), (24, 47), (8, 47), (19, 47), (28, 46)):
        if _mass(c) >= 900:
            break
        _shard(c, nx, ny, rng.range(6, 9), 2, ramp, lean=rng.pick((-1, 1)),
               bright=rng.chance(0.5))
    c.outline(palette.OUTLINE)
    # glow motes drifting in EMPTY air beside the tips (never on the mass)
    placed = 0
    while placed < 3:
        mx, my = rng.range(3, 29), rng.range(5, 18)
        if not c.filled(mx, my):
            c.put(mx, my, with_alpha(ramp["hi"], 170 - placed * 30))
            placed += 1
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
    """Crystalline bloom cluster, rebuilt at crystal-cluster mass (size law:
    the old single bloom was 22% of the vanilla crystal refs). Each 32x48
    half now carries a mounded tuft base, a BIG bloom — thick stem, broad
    blade leaves, five fat faceted petals around a teal core — plus a
    smaller second bloom leaning outward and a sprouting bud."""
    ramp = palette.AURORA
    sheet = Canvas(variants * 64, 48)
    for v in range(variants):
        for col in range(2):
            c = Canvas(32, 48)
            rng = Rng(0xB100 + v * 101 + col * 55)

            # grounding mound of rose-tinged turf
            tuft = palette.CLOUDTURF
            c.ellipse(15, 44, 14.5, 3.4, tuft["deep"])
            c.ellipse(11, 42.5, 7.5, 2.6, tuft["tuft"])
            c.ellipse(21, 43, 7, 2.4, tuft["tuft"])
            c.ellipse(16, 45, 9, 2.2, tuft["deep"])
            c.put(6, 40, tuft["tuft"])
            c.put(24, 40, tuft["tuft"])
            c.put(15, 41, tuft["tuft"])
            c.put(10, 40, tuft["tuft"])

            def bloom(cx, top, scale, lean):
                # stem: 3px thick, 4px near the root, arcing toward its lean
                for y in range(top + 2, 45):
                    t = (y - top) / float(45 - top)
                    sx = cx + round(lean * t * t)
                    c.put(sx - 1, y, ramp["deep"])
                    c.put(sx, y, palette.AURORALILY["stem"])
                    c.put(sx + 1, y, ramp["deep"])
                    if t > 0.6:
                        c.put(sx + 2, y, ramp["deep"])
                # broad blade leaves off the lower stem
                for (side, ly) in ((-1, 40), (1, 38)):
                    for i in range(round(6 * scale)):
                        lx = cx + round(lean * 0.5) + side * (1 + i)
                        yy = ly - (i * i) // 5
                        c.put(lx, yy, palette.AURORALILY["stem"])
                        c.put(lx, yy + 1, ramp["deep"])
                # five fat faceted petals fanning from the head, drawn
                # back-to-front (sides, diagonals, then the tall center) —
                # the shard cut seams keep each petal a separate blade
                petals = ((-7, 1, round(10 * scale), 3), (7, 1, round(10 * scale), 3),
                          (-5, -2, round(14 * scale), 3), (5, -2, round(13 * scale), 3),
                          (0, -5, round(18 * scale), 4))
                for k, (dx, dy, h, w) in enumerate(petals):
                    _shard(c, cx + dx, top + dy, max(5, h), w, ramp,
                           lean=round(dx * 0.4), bright=(k % 2 == 0))
                # rimmed teal core over the petal bases + hanging bud
                c.ellipse(cx, top - 1, 2.8, 2.4, ramp["deep"])
                c.ellipse(cx, top - 1, 2.0, 1.7, ramp["teal"])
                c.put(cx - 1, top - 2, ramp["hi"])
                c.put(cx + 6, top + 4, ramp["deep"])
                c.rect(cx + 6, top + 5, 2, 3, ramp["base"])
                c.put(cx + 6, top + 5, ramp["light"])
                c.put(cx + 6, top + 8, ramp["teal"])

            bloom(12 + rng.pick((-1, 0, 1)), 24 + rng.pick((-1, 0, 1)), 1.0,
                  rng.pick((-3, -2)))
            bloom(23 + rng.pick((-1, 0)), 33 + rng.pick((-1, 0, 1)), 0.8,
                  rng.pick((3, 4)))
            # sprouting bud poking out of the mound on its own stalk
            bx = 5 + rng.pick((0, 1))
            c.put(bx, 41, ramp["deep"])
            c.put(bx, 40, ramp["deep"])
            c.rect(bx - 1, 37, 3, 3, ramp["base"])
            c.put(bx - 1, 37, ramp["light"])
            c.put(bx, 36, ramp["teal"])
            # two fallen petal shards resting on the mound
            _shard(c, 17, 45, 6, 2, ramp, lean=3, bright=True)
            _shard(c, 8, 46, 5, 2, ramp, lean=-3)
            # size-law top-up: extra crystal buds sprouting from the mound
            # until each half carries vanilla crystal mass (~900 opaque px)
            for (nx, ny) in ((4, 46), (14, 47), (19, 46), (27, 45), (10, 47),
                             (24, 47), (16, 46)):
                if _mass(c) >= 900:
                    break
                _shard(c, nx, ny, rng.range(6, 9), 2, ramp,
                       lean=rng.pick((-1, 1)), bright=rng.chance(0.5))
            c.outline(palette.OUTLINE)
            # drifting aurora motes (after outline so they float)
            c.put(rng.range(6, 26), rng.range(8, 14), with_alpha(ramp["hi"], 150))
            c.put(rng.range(6, 26), rng.range(15, 20), with_alpha(ramp["teal"], 130))
            sheet.paste(c if col == 0 else c.mirrored(), v * 64 + col * 32, 0)
    sheet.save(path)


# --- Sky reeds ---------------------------------------------------------------

def gen_skyreeds(path, variants=4):
    """N*32 strip: silky reeds in TWO uneven clumps, rebuilt at vanilla
    tall-grass mass (500+ opaque px per variant — size law). Wide grounding
    tufts anchor 10-12 blades; every blade stays 2px thick for most of its
    height (3px near the root), fans outward from its clump root, arcs with
    per-blade curvature, and half hook over at the tip like vanilla tall
    grass. No outline pass (grass rule)."""
    ramp = palette.WINDSILK
    tuft = palette.CLOUDTURF
    sheet = Canvas(variants * 32, 32)
    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0x4EED + v * 313)
        # wide uneven grounding tufts over a deep shadow that anchors the
        # pale blades on pale cloudturf at 1x
        c.ellipse(15, 30.5, 11, 2.0, tuft["deep"])
        c.ellipse(12 + rng.range(-2, 1), 30, 7.5, 2.0, tuft["deep"])
        c.ellipse(21 + rng.range(-1, 2), 30.5, 6.5, 1.8, tuft["deep"])
        c.ellipse(11 + rng.range(-2, 1), 29, 7, 1.9, tuft["tuft"])
        c.ellipse(22 + rng.range(-1, 2), 30, 6, 1.7, tuft["tuft"])
        c.ellipse(16 + rng.range(-1, 1), 30, 4, 1.4, tuft["deep"])
        blades = []
        for (cx0, cnt) in ((rng.range(9, 11), rng.range(10, 11)),
                           (rng.range(20, 22), rng.range(8, 9))):
            for _ in range(cnt):
                blades.append((rng.range(14, 28), cx0 + rng.range(-3, 3), cx0))
        for (h, bx, cx0) in sorted(blades):          # tall blades in front
            base_y = rng.range(27, 29)
            out = 1 if bx >= cx0 else -1
            lean = out * rng.range(2, 4)
            bend = rng.pick((1.6, 2.0, 2.4))
            hook = rng.chance(0.5)
            x = bx
            y = base_y
            for i in range(h):
                t = i / h
                x = min(28, max(3, bx + round(lean * (t ** bend))))
                if hook and i > h - 3:
                    x += out * (i - (h - 3))         # tip curls over
                y = base_y - i
                tone = ramp["deep"] if t < 0.35 else (ramp["base"] if t < 0.7 else ramp["light"])
                c.put(x, y, tone)
                if t < 0.95:                         # 2px body almost to tip
                    c.put(x + 1, y, ramp["deep"] if t < 0.4 else ramp["base"])
                if t < 0.5:                          # 3px root half
                    c.put(x - 1, y, ramp["deep"])
                if t < 0.2:                          # 4px flare at the root
                    c.put(x + 2, y, ramp["deep"])
            c.put(x, y, ramp["hi"])                  # glint ON the tip pixel
        # size-law top-up: extra short sprouts around the roots until the
        # sheaf carries vanilla tall-grass mass (500+ opaque px)
        for _ in range(20):
            if _mass(c) >= 505:
                break
            bx = rng.pick((8, 10, 12, 15, 19, 21, 23))
            sh = rng.range(5, 9)
            lean = rng.pick((-2, -1, 1, 2))
            for i in range(sh):
                t = i / sh
                x = min(28, max(3, bx + round(lean * t * t)))
                tone = ramp["deep"] if t < 0.5 else ramp["base"]
                c.put(x, 29 - i, tone)
                if t < 0.7:
                    c.put(x + 1, 29 - i, ramp["deep"])
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
        # two nodding stems; the short one draws FIRST so the tall stem and
        # its bell stay in front (windwheat's tall-in-front rule)
        for (dx0, h, lean) in ((3, rng.range(9, 12), -rng.range(3, 5)),
                               (-2, rng.range(15, 18), rng.range(4, 6))):
            x0 = root + dx0
            tip = _arc_stem(c, x0, 29, h, lean, S["stem_deep"], S["stem"])
            # the stem hooks over; the bell hangs from the hook tip
            hx = tip[0] + (2 if lean > 0 else -2)
            c.put(tip[0] + (1 if lean > 0 else -1), tip[1], S["stem"])
            bell(c, hx - 2, tip[1] + 1)
        # one closed bud nodding off its own short stalk
        bx = root + rng.pick((-4, 5))
        _arc_stem(c, bx, 29, 6, 1 if bx < root else -1,
                  S["stem_deep"], S["stem"], thick=False)
        c.put(bx, 23, B["deep"])
        c.put(bx + 1, 23, B["base"])
        c.put(bx, 22, B["light"])
        c.put(bx + 1, 22, B["light"])
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
        for (mx, my, r, ry) in mounds:                 # broad lit crown caps
            c.ellipse(mx - r * 0.22, my - ry * 0.4, r * 0.62, ry * 0.55,
                      M["light"])
        # mossy scale dapple: deep pits lower-right, spark-lit velvet on the
        # crowns — this glow dapple is what says "charged moss", not rock
        for x in range(2, 30):
            for y in range(18, 31):
                p = c.get(x, y)[:3]
                if p == M["base"][:3] and (x + 2 * y) % 5 == 0 and rng.chance(0.7):
                    c.put(x, y, M["deep"])
                elif p == M["light"][:3] and (2 * x + y) % 7 == 0 and rng.chance(0.6):
                    c.put(x, y, M["spark"])
        # tiny moss-fuzz hairs poking off the crowns (pre-outline: they
        # pick up the outline and read as soft fuzz, not stray pixels)
        for (mx, my, r, ry) in mounds[:2]:
            fx = mx + rng.range(-2, 2)
            c.put(fx, int(my - ry) - 1, M["base"])
            c.put(fx + rng.pick((-1, 1)), int(my - ry), M["light"])
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
                core = T["light"] if (i == 0 or i >= ln - 2) else T["base"]
                c.put(x + (1 if dx > 0.3 else 0), y, core)
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
            # alternating leaflet ticks (left row, then right row) so the
            # frond reads feathered, not ladder-rigid; longest mid-frond
            for i in range(2, h - 3):
                x, y, t = spine[i]
                ln = 2 if 0.2 < t < 0.7 else 1
                if i % 2 == 0:
                    for d in range(1, ln + 1):
                        c.put(x - d, y, G["base"] if d == 1 else G["deep"])
                    c.put(x - ln, y - 1, G["light"] if t > 0.4 else G["base"])
                else:
                    for d in range(1, ln + 1):
                        c.put(x + d, y, G["light"] if d == 1 else G["base"])
                    c.put(x + ln, y - 1, G["light"])
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
        for _ in range(rng.range(4, 6)):
            bx, by = x0 + rng.range(3, 28), rng.range(3, 28)
            sheet.put(bx, by, F["light"])
            sheet.put(bx + 1, by, F["deep"])
            sheet.put(bx, by - 1, F["hi"] if rng.chance(0.4) else F["light"])
        # one short fused stub vein for density (vanilla ore fills the face)
        sx, sy = x0 + rng.range(10, 20), rng.range(20, 24)
        for k in range(4):
            sheet.put(sx + (k % 2), sy + k, F["base"])
            sheet.put(sx + (k % 2) + 1, sy + k, F["deep"])
        sheet.put(sx, sy - 1, F["hi"])
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


# --- v0.4 item icons (32x32, established icon style: chunky read at 1x,
# soft dark outline; thin stems drawn 3px so the outline leaves a core) ------

def gen_cloudbell_item(path):
    """A picked cloudbell sprig: arced stem with two hanging bells."""
    c = Canvas(32, 32)
    B = palette.CLOUDBELL
    S = palette.SKYTULIP
    # arced stem (3px mass so the outline pass keeps a lit core)
    arc = []
    for i in range(18):
        t = i / 17.0
        x = 7 + round(16 * t)
        y = 22 - round(15 * (t - 0.5 * t * t) * 1.5)
        arc.append((x, y))
        c.put(x, y, S["stem"] if t < 0.75 else S["stem_deep"])
        c.put(x, y + 1, S["stem_deep"])
        c.put(x + 1, y, S["stem_deep"])
    # leaf pair at the cut end
    for d in range(4):
        c.put(6 - d, 23 + d // 2, S["stem"])
        c.put(6 - d, 24 + d // 2, S["stem_deep"])
    # two bells hanging FROM the arc: short stalk pixels tie each bell top
    # to its arc point so nothing floats
    hang = ((arc[9], 3), (arc[16], 4))
    bells = []
    for ((ax, ay), s) in hang:
        c.put(ax, ay + 1, S["stem_deep"])
        c.put(ax, ay + 2, S["stem_deep"])
        bells.append((ax - (s + 2) // 2 + 1, ay + 3, s))
    for (bx, by, s) in bells:
        for dy in range(s + 3):
            for dx in range(s + 2):
                if dy == 0 and dx in (0, s + 1):
                    continue
                tone = B["base"]
                if dx == 0:
                    tone = B["light"]
                elif dx >= s:
                    tone = B["deep"]
                if dy >= s + 2:
                    tone = B["deep"]
                c.put(bx + dx, by + dy, tone)
        c.put(bx + 1, by + 1, B["hi"])
        c.put(bx, by + s + 3, B["deep"])            # flared mouth
        c.put(bx + s + 1, by + s + 3, B["deep"])
        c.put(bx + (s + 1) // 2, by + s + 3, B["hi"])  # clapper
    c.outline(palette.OUTLINE)
    c.save(path)


def gen_skytulip_item(path):
    """One tall rose sky tulip with a small gold companion."""
    c = Canvas(32, 32)
    S = palette.SKYTULIP
    A = palette.AURORA
    CB = palette.CLOUDBERRY

    def head(tx, ty, w, h, base, deep, hi):
        for dy in range(1, h):
            for dx in range(w):
                tone = base if dx < w - 2 and dy < h - 2 else deep
                c.put(tx + dx, ty + dy, tone)
        for dx in (0, w // 2, w - 1):               # petal tips
            c.put(tx + dx, ty, base if dx < w - 1 else deep)
        c.put(tx + 1, ty + 1, hi)
        c.put(tx + 1, ty + 2, hi)
        for dx in range(w):                          # rounded cup bottom
            c.put(tx + dx, ty + h, deep)

    # stems (3px masses)
    for i in range(14):
        c.put(12, 27 - i, S["stem"])
        c.put(13, 27 - i, S["stem_deep"])
        c.put(11, 27 - i, S["stem_deep"])
    for i in range(8):
        c.put(21 + i // 4, 27 - i, S["stem"])
        c.put(22 + i // 4, 27 - i, S["stem_deep"])
    # leaves
    for d in range(5):
        c.put(10 - d // 1, 26 - d, S["stem"])
        c.put(10 - d, 27 - d, S["stem_deep"])
    for d in range(4):
        c.put(16 + d, 25 - d, S["stem"])
        c.put(16 + d, 26 - d, S["stem_deep"])
    head(8, 5, 9, 9, S["rose"], A["base"], A["hi"])
    head(19, 13, 7, 7, S["gold"], CB["berry_deep"], CB["berry_hi"])
    c.outline(palette.OUTLINE)
    c.save(path)


def gen_thunderbloom_item(path):
    """Sparking violet bloom head; spark tips re-added after the outline."""
    c = Canvas(32, 32)
    T = palette.THUNDERBLOOM
    hx, hy = 16, 13
    # stem
    for i in range(13):
        c.put(15 + i // 6, 28 - i, T["stem"])
        c.put(16 + i // 6, 28 - i, T["stem"])
        c.put(17 + i // 6, 28 - i, T["stem"])
    # petal spikes: chunky 3px masses radiating
    for (a, ln) in ((90, 8), (30, 7), (150, 7), (-15, 6), (195, 6), (60, 7), (120, 7)):
        dx = math.cos(math.radians(a))
        dy = -math.sin(math.radians(a))
        for i in range(ln):
            x, y = round(hx + dx * i), round(hy + dy * i)
            c.put(x, y, T["deep"])
            c.put(x + 1, y, T["deep"])
            c.put(x, y + 1, T["deep"])
        for i in range(ln - 1):
            x, y = round(hx + dx * i), round(hy + dy * i)
            c.put(x, y, T["light"] if i >= ln - 3 else T["base"])
    c.ellipse(hx, hy, 2.6, 2.4, T["deep"])
    c.ellipse(hx, hy, 1.6, 1.4, T["light"])
    c.outline(palette.OUTLINE)
    # charged heart + crackling spark tips (after outline so they float)
    c.put(hx, hy, T["spark"])
    c.put(hx + 1, hy, T["spark"])
    c.put(hx, hy - 1, T["light"])
    for (a, ln) in ((90, 8), (30, 7), (150, 7), (-15, 6), (195, 6)):
        x = round(hx + math.cos(math.radians(a)) * (ln + 1))
        y = round(hy - math.sin(math.radians(a)) * (ln + 1))
        c.put(x, y, T["spark"])
    c.put(hx + 8, hy - 6, with_alpha(T["spark"], 170))
    c.put(hx - 8, hy + 2, with_alpha(T["spark"], 150))
    c.save(path)


def gen_auroralily_item(path):
    """Glow lily bloom: open cup with a radiant core."""
    c = Canvas(32, 32)
    L = palette.AURORALILY
    hx, hy = 15, 12
    # stem with one leaf
    for i in range(14):
        c.put(15 + i // 5, 28 - i, L["stem"])
        c.put(16 + i // 5, 28 - i, L["stem"])
        c.put(17 + i // 5, 28 - i, L["stem"])
    for d in range(5):
        c.put(18 + d, 23 - d + d // 3, L["stem"])
        c.put(18 + d, 24 - d + d // 3, L["stem"])
    # back petals peeking over the cup
    for (dx, dy) in ((-4, -4), (3, -5), (0, -6)):
        c.put(hx + dx, hy + dy, L["deep"])
        c.put(hx + dx + 1, hy + dy, L["deep"])
        c.put(hx + dx + 1, hy + dy - 1, L["base"])
    # cup body
    c.ellipse(hx, hy, 6.4, 4.6, L["base"])
    c.ellipse(hx - 2, hy - 2, 3.8, 2.6, L["light"])
    # petal separations
    c.put(hx - 5, hy + 2, L["deep"])
    c.put(hx - 2, hy + 4, L["deep"])
    c.put(hx + 2, hy + 4, L["deep"])
    c.put(hx + 5, hy + 2, L["deep"])
    c.outline(palette.OUTLINE)
    # radiant heart + halo, after the outline so the glow floats
    c.put(hx, hy, L["core"])
    c.put(hx + 1, hy, L["core"])
    c.put(hx, hy - 1, L["core"])
    c.put(hx + 1, hy - 1, with_alpha(L["core"], 200))
    c.put(hx - 1, hy, with_alpha(L["core"], 160))
    c.put(hx + 2, hy + 1, with_alpha(L["core"], 120))
    c.put(hx, hy - 3, with_alpha(L["core"], 110))
    c.save(path)


def gen_glowfern_item(path):
    """A cut glowfern frond (silhouette-first, no outline pass — the
    skyreeds-item trick keeps the feathered leaflets crisp)."""
    c = Canvas(32, 32)
    G = palette.GLOWFERN
    spine = []
    for i in range(22):
        t = i / 21.0
        x = 8 + round(16 * (t + 0.2 * t * t))
        y = 27 - round(20 * (t - 0.28 * t * t))
        spine.append((x, y, t))
    # dark silhouette mass behind spine + leaflets
    for (x, y, t) in spine:
        c.put(x - 1, y, palette.OUTLINE)
        c.put(x + 1, y, palette.OUTLINE)
        c.put(x, y + 1, palette.OUTLINE)
        c.put(x, y - 1, palette.OUTLINE)
    for i in range(2, 20, 2):
        x, y, t = spine[i]
        ln = 4 if 0.15 < t < 0.5 else (3 if t < 0.7 else 2)
        for d in range(1, ln + 1):
            sx = x - d if i % 4 == 0 else x + d
            sy = y - d // 2
            c.put(sx, sy, palette.OUTLINE)
            c.put(sx, sy + 1, palette.OUTLINE)
    # lit frond on top
    for (x, y, t) in spine:
        c.put(x, y, G["deep"] if t < 0.4 else G["base"])
    for i in range(2, 20, 2):
        x, y, t = spine[i]
        ln = 4 if 0.15 < t < 0.5 else (3 if t < 0.7 else 2)
        for d in range(1, ln + 1):
            sx = x - d if i % 4 == 0 else x + d
            sy = y - d // 2
            c.put(sx, sy, G["light"] if d == ln else G["base"])
    tx, ty, _ = spine[-1]
    c.put(tx, ty, G["hi"])                          # curled glowing tip
    c.put(tx - 1, ty - 1, G["hi"])
    c.put(spine[0][0], spine[0][1] + 1, G["hi"])    # clean-cut stem end
    c.put(spine[0][0] - 1, spine[0][1] + 1, G["light"])
    c.put(9, 6, with_alpha(G["hi"], 150))           # spore mote
    c.save(path)


def gen_staticmoss_item(path):
    """A pulled clump of static moss, roots dangling, sparks crawling."""
    c = Canvas(32, 32)
    M = palette.STATICMOSS
    mounds = [(15, 14, 8, 5), (9, 17, 4.5, 3), (22, 16, 5, 3.4)]
    for (mx, my, r, ry) in mounds:
        c.ellipse(mx + 1, my + 1, r, ry, M["deep"])
    for (mx, my, r, ry) in mounds:
        c.ellipse(mx, my, r, ry, M["base"])
    for (mx, my, r, ry) in mounds:
        c.ellipse(mx - r * 0.25, my - ry * 0.4, r * 0.6, ry * 0.55, M["light"])
    # scale dapple + spark velvet
    for x in range(4, 28):
        for y in range(8, 22):
            p = c.get(x, y)[:3]
            if p == M["base"][:3] and (x + 2 * y) % 5 == 0:
                c.put(x, y, M["deep"])
            elif p == M["light"][:3] and (2 * x + y) % 7 == 0:
                c.put(x, y, M["spark"])
    # dangling root threads (2px masses)
    for (rx, ln) in ((11, 4), (16, 6), (21, 3)):
        for k in range(ln):
            c.put(rx, 20 + k, M["deep"])
            c.put(rx + 1, 20 + k, M["deep"])
        c.put(rx, 20 + ln, M["base"])
    c.outline(palette.OUTLINE)
    # crawling sparks after the outline
    c.put(12, 10, M["spark"])
    c.put(20, 12, M["spark"])
    c.put(16, 8, with_alpha(M["spark"], 180))
    c.put(24, 9, with_alpha(M["spark"], 130))
    c.save(path)


def gen_fulgurite_item(path):
    """A fulgurite shard: fused lightning-glass, jagged and branching."""
    c = Canvas(32, 32)
    F = palette.FULGURITE
    # main zigzag column, 4-5px wide; kinks stay 1px so rows keep >=3px of
    # overlap and the outline pass cannot shred the column into dashes
    x = 16
    kinks = (0, 0, -1, 0, 0, 1, 0, 1, 0, -1, 0, 1, 0, -1, -1, 0, 1, 0, 0, -1)
    for i, k in enumerate(kinks):
        y = 27 - i
        x += k
        w = 5 if 4 < i < 14 else 4
        for dx in range(w):
            tone = F["base"]
            if dx == 0:
                tone = F["light"]
            elif dx >= w - 1:
                tone = F["deep"]
            c.put(x + dx, y, tone)
        if k != 0:                                  # glint at each kink
            c.put(x + 1, y, F["hi"])
    # branch spur (2px thick so it survives the outline)
    bx, by = x + 2, 14
    for k in range(4):
        c.put(bx + 2 + k, by - k, F["base"])
        c.put(bx + 3 + k, by - k, F["base"])
        c.put(bx + 3 + k, by - k + 1, F["deep"])
    c.put(bx + 6, by - 4, F["light"])
    # hollow glassy tip (fulgurites are tubes)
    c.put(x + 1, 8, F["deep"])
    c.put(x + 2, 8, F["deep"])
    c.put(x + 1, 7, F["hi"])
    c.outline(palette.OUTLINE)
    c.put(x - 1, 12, F["hi"])                       # outer glass sparkle
    c.put(16, 22, F["hi"])
    c.save(path)


def gen_prismshard_item(path):
    """Prismshard crystal: faceted shards with the Shoals' teal glint."""
    c = Canvas(32, 32)
    P = palette.PRISMSHARD
    for (sx, h, w, lean) in ((15, 16, 3, 1), (9, 10, 2, -2), (22, 11, 2, 2)):
        for i in range(h):
            t = i / max(h - 1, 1)
            half = max(1, round(w * (1.0 - abs(t * 2 - 1))))
            cx = sx + round(lean * t)
            for dx in range(-half, half + 1):
                c.put(cx + dx, 27 - i, P["deep"] if dx in (-half, half) else P["base"])
            if 0.25 < t < 0.9:
                c.put(cx - max(0, half - 1), 27 - i, P["light"])
        c.put(sx + lean, 27 - h + 1, P["hi"])
    c.outline(palette.OUTLINE)
    # teal refraction glints after the outline
    c.put(14, 20, P["teal"])
    c.put(15, 21, P["teal"])
    c.put(23, 22, P["teal"])
    c.put(10, 23, P["teal"])
    c.put(17, 13, P["hi"])
    c.save(path)


# --- v0.4 walk-through tall-grass carpets ------------------------------------
# Vanilla deepswamptallgrass construction: blade FEET spread across the FULL
# 32px cell width (edge blades clip mid-arc into the neighbor cell), a layer
# of darker back blades fills between the lit front blades, and everything
# reaches y<=2 so tiled cells read as one continuous meadow. Grass rule: no
# outline pass — the deep ramp edge carries the silhouette.

def _carpet_cell(c, rng, back_tone, foot_tone, blade_tones, spark=None):
    """One 32x32 carpet cell. blade_tones(rng, k) -> (deep, mid, tip) per
    front blade; spark=(tone, chance) optionally electrifies one tip.
    Leans stay small and bends high so blades keep standing shoulder to
    shoulder all the way up — wide fans thin out the cell top and tiled
    rows band apart (caught in the carpet-mock QA)."""
    # near-solid dark baseline: the feet zone must stay covered or tiled
    # rows band apart (bright ground stripe at every cell seam)
    for x in range(32):
        if rng.chance(0.8):
            c.put(x, 30, foot_tone)
        if rng.chance(0.75):
            c.put(x, 31, foot_tone)
        if rng.chance(0.3):
            c.put(x, 29, foot_tone)
    # back layer: thin dark blades between the front ones, two height bands
    for k in range(10):
        bx = round((k + 0.5) * 32 / 10.0) + rng.range(-2, 2)
        h = rng.range(15, 20) if k % 2 else rng.range(21, 27)
        lean = rng.pick((-3, -2, 2, 3))
        base_y = 30 + rng.pick((0, 1))
        for i in range(h):
            t = i / h
            x = bx + round(lean * t * t)
            c.put(x, base_y - i, back_tone)
    # front layer: tall lit blades, gentle arcs + short hooks, tall-last.
    # Heights stagger widely so blade tips distribute vertically — same-tall
    # blades put every bright tip on one line and tiled rows band again.
    blades = []
    for k in range(12):
        bx = round((k + 0.5) * 32 / 12.0 - 0.5) + rng.range(-1, 1)
        h = rng.range(24, 29) if k % 3 else rng.range(18, 23)
        blades.append((h, bx, k))
    sparked = False
    for (h, bx, k) in sorted(blades):
        deep, mid, tip = blade_tones(rng, k)
        base_y = 29 + rng.pick((0, 1, 1))
        lean = rng.pick((-4, -3, -2, 2, 3, 4))
        out = 1 if lean > 0 else -1
        bend = rng.pick((2.0, 2.3, 2.6))
        hook = rng.chance(0.4)
        x = bx
        y = base_y
        for i in range(h):
            t = i / h
            x = bx + round(lean * (t ** bend))
            if hook and i > h - 3:
                x += out * (i - (h - 3))
            y = base_y - i
            tone = deep if t < 0.35 else (mid if t < 0.72 else tip)
            c.put(x, y, tone)
            if t < 0.55:
                c.put(x + 1, y, deep)
        if spark is not None and not sparked and rng.chance(spark[1]):
            c.put(x, y - 1, spark[0])               # charged tip mote
            c.put(x + out, y, spark[0])
            sparked = True
        else:
            c.put(x, y, tip)


def gen_tallcloudgrass(path, variants=4):
    """128x32: lush sky-pale tall grass carpet (Driftlands). Windwheat-family
    greens with white-lit tips; tiles into a continuous half-hiding meadow."""
    sheet = Canvas(variants * 32, 32)
    W = palette.WINDWHEAT
    turf = palette.CLOUDTURF

    def tones(rng, k):
        if rng.chance(0.2):                         # paler seed-silk blade
            return (W["deep"], W["light"], W["head"])
        return (W["deep"], W["base"], W["light"])

    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0x7A11C + v * 509)
        _carpet_cell(c, rng, turf["tuft"], W["deep"], tones)
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_stormsedge(path, variants=4):
    """128x32: blue-gray sedge carpet (Stormveil) with an occasional violet
    slate-tint blade and a rare static spark on one tip."""
    sheet = Canvas(variants * 32, 32)
    N = palette.FULGURPINE_NEEDLE
    slate = palette.STORMSLATE
    spark = palette.STATICMOSS["spark"]

    def tones(rng, k):
        if rng.chance(0.2):                         # charged violet blade
            return (slate["base"], slate["light"], slate["charge"])
        return (N["deep"], N["base"], N["light"] if rng.chance(0.7) else N["hi"])

    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0x570A6E + v * 613)
        _carpet_cell(c, rng, slate["light"], N["deep"], tones,
                     spark=(spark, 0.35))
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_prismgrass(path, variants=4):
    """128x32: pale iridescent grass carpet (Aurora Shoals) with sparse
    teal/rose accent blades — the Shoals' exclusive shimmer."""
    sheet = Canvas(variants * 32, 32)
    P = palette.PRISMWOOD
    L = palette.PRISMLEAF

    def tones(rng, k):
        r = rng.float()
        if r < 0.12:                                # teal accent blade
            return (L["deep"], L["teal"], P["hi"])
        if r < 0.24:                                # rose accent blade
            return (L["deep"], L["rose"], P["hi"])
        return (P["deep"], P["base"], P["hi"] if rng.chance(0.25) else P["light"])

    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0x9816A + v * 431)
        _carpet_cell(c, rng, L["deep"], P["deep"], tones)
        sheet.paste(c, v * 32, 0)
    sheet.save(path)
