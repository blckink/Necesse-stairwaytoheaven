"""Biome trees (vanilla TreeObject format) + falling-leaf particle strips.

Formats (verified against the decompiled TreeObject draw code + vanilla
sheets, see docs/research/asset-formats.md):
- Tree sheet: 128 px cells drawn at (tileDrawX - 48, tileDrawY - 96), so the
  trunk BASE must stand inside the bottom-center 32x32 of its cell
  (x 48..80, y 96..127). Vanilla ships 2 columns (normal + snow) x 4 variant
  rows; the sky islands have no snow layer, so we ship 1 column x 2 variant
  rows -> each sheet is 128x256. Sway comes from the game's wave shader —
  nothing is baked into the frames.
- Leaf particles: five 20 px sprites in one row -> 100x20 (vanilla ships
  4-6 frames of tumbling leaf clumps; the engine picks frames at random).

Construction follows vanilla pinetree/willowtree/birchtree: a soft ground
shadow (18,32,32 @ ~44% alpha) with NO outline, a trunk grounded by root
flares, canopy = few BIG overlapping masses (deep crescent lower-right, base
mass, light upper-left sheen, hard 1 px sunlit rim under the top outline,
deep fold arcs where lobes overlap), micro-detail from leaf-clump ticks —
never uniform noise, never stacked rectangles.
"""

import math

from px import Canvas, Rng, with_alpha
import palette

SHADOW = (18, 32, 32)          # vanilla tree ground-shadow tone (alpha 110)


# --- shared passes -----------------------------------------------------------

def _sunlit_rim(c, hi, light, y_max, outline=palette.OUTLINE):
    """Hard 1 px sunlit rim just inside the outline on upper/left canopy
    edges (the vanilla cartoon-cloud top edge). Runs AFTER the outline pass;
    restricted to y < y_max so trunks keep their own shading."""
    rims = []
    for x in range(c.width):
        for y in range(min(y_max, c.height)):
            p = c.get(x, y)
            if p[3] == 0 or p[:3] == outline:
                continue
            if c.get(x, y - 1)[:3] == outline and not c.filled(x, y - 2):
                rims.append((x, y, hi))
            elif c.get(x - 1, y)[:3] == outline and not c.filled(x - 2, y):
                rims.append((x, y, light))
    for x, y, tone in rims:
        c.put(x, y, tone)


def _lobed_mass(c, lobes, ramp, fold=True, sheen=True):
    """One cohesive canopy from overlapping lobes: deep silhouette body,
    base fill, per-lobe light upper-left sheen, then deep 'fold' arcs along
    each lobe's lower boundary — but only where the arc stays INTERIOR, so
    the mass reads as one volume with cloud folds, not separate balloons."""
    for (cx, cy, rx, ry) in lobes:
        c.ellipse(cx + 1, cy + 2, rx, ry, ramp["deep"])
    for (cx, cy, rx, ry) in lobes:
        c.ellipse(cx, cy, rx, ry, ramp["base"])
    if sheen:
        for (cx, cy, rx, ry) in lobes:
            c.ellipse(cx - rx * 0.28, cy - ry * 0.36, rx * 0.5, ry * 0.45,
                      ramp["light"])
    if not fold:
        return
    for (cx, cy, rx, ry) in lobes:
        for deg in range(25, 156, 4):           # lower arc (screen y down)
            x = round(cx + (rx - 1) * math.cos(math.radians(deg)))
            y = round(cy + (ry - 1) * math.sin(math.radians(deg)))
            # interior only: skip near the silhouette (outline handles that)
            if c.filled(x, y + 2) and c.filled(x + 2, y) and c.filled(x - 2, y):
                c.put(x, y, ramp["deep"])
                if deg % 12 == 1:
                    c.put(x, y - 1, ramp["deep"])


def _canopy_volume(c, lobes, ramp, rng, light_spread=0.55):
    """v0.6 volume pass (playtest: canopies read as 'stacked pancakes').

    Three flat-band passes over an existing _lobed_mass, all true pixel art:
    1. OVERLAP SHADOWS — where one lobe sits under an up-left neighbour, the
       covered band darkens two steps. This is the missing 'shadow where one
       canopy lobe overlaps another'.
    2. GLOBAL LIGHT FIELD — one big lit plane toward the top-left of the whole
       canopy and a deep field toward the lower right (quantised, dithered at
       the boundary), so value structure is canopy-scale instead of repeating
       per lobe.
    3. TRUNK COLLAR — canopy pixels around the trunk entry darken, seating
       the mass on the trunk.
    """
    import math
    # pixel -> topmost lobe index that owns it
    owner = {}
    for i, (cx, cy, rx, ry) in enumerate(lobes):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
                if not c.filled(x, y):
                    continue
                dx = (x - cx) / max(rx, 0.001)
                dy = (y - cy) / max(ry, 0.001)
                if dx * dx + dy * dy <= 1.05 and (x, y) not in owner:
                    owner[(x, y)] = i
    # 1. overlap shadows: a pixel whose up-left neighbour belongs to a
    #    different, higher lobe is under that lobe -> shadow band
    shadow_px = []
    for (x, y), i in owner.items():
        for (ox, oy) in ((-2, -3), (-3, -2), (-2, -2)):
            j = owner.get((x + ox, y + oy))
            if j is None or j == i:
                continue
            if lobes[j][1] < lobes[i][1] - 2:      # j sits higher
                shadow_px.append((x, y))
                break
    for (x, y) in shadow_px:
        c.put(x, y, ramp["deep"])
    # 2. global light field, quantised with a jittered boundary
    xs = [p[0] for p in owner]
    ys = [p[1] for p in owner]
    if not xs:
        return
    mx, my = sum(xs) / len(xs), sum(ys) / len(ys)
    radius = max(max(x for x in xs) - mx, max(y for y in ys) - my, 1)
    for (x, y), i in owner.items():
        d = ((x - mx) * 0.62 + (y - my) * 1.0) / radius
        jitter = (rng.float() - 0.5) * 0.16
        cur = c.get(x, y)[:3]
        if d < -light_spread + jitter and cur not in (palette.OUTLINE, ramp["hi"], ramp["light"]):
            c.put(x, y, ramp["light"])
        elif d > light_spread + jitter:
            # demote the per-lobe sheens on the shadow side too — otherwise
            # every lobe keeps its own bright ellipse and the canopy keeps
            # reading as stacked pancakes
            if cur == ramp["base"] and rng.chance(0.5):
                c.put(x, y, ramp["deep"])
            elif cur == ramp["light"] and rng.chance(0.6):
                c.put(x, y, ramp["base"])
    # 3. trunk collar: darken a small arc where the mass meets the trunk
    bx = min(owner, key=lambda p: abs(p[0] - mx)) if owner else None
    if bx is not None:
        col_x, base_y = bx[0], max(y for (x, y) in owner
                                   if abs(x - bx[0]) <= 6)
        for x in range(col_x - 6, col_x + 7):
            for y in range(base_y - 4, base_y + 1):
                if c.filled(x, y) and rng.chance(0.75):
                    c.put(x, y, ramp["deep"])


def _bottom_edge(c, x, y_from=127):
    """Lowest filled y in column x (or None)."""
    for y in range(y_from, -1, -1):
        if c.filled(x, y):
            return y
    return None


def _trunk(c, rng, ramp, base_x, base_y, top_y, w_base, w_top,
           amp=2.5, drift=0, notch_tone=None):
    """S-curved tapering trunk (gloomwillow build): light left column, base
    fill, deep right column, periodic bark notches. Returns {y: left_x}."""
    H = max(base_y - top_y, 1)
    ph = rng.float() * 3.0
    path_x = {}
    notch = notch_tone if notch_tone is not None else ramp["deep"]
    for y in range(base_y, top_y - 1, -1):
        t = (base_y - y) / H
        x = base_x + round(amp * math.sin(t * 2.6 + ph) + drift * t)
        width = max(w_top, round(w_base - (w_base - w_top) * t))
        x -= width // 2
        path_x[y] = (x, width)
        for dx in range(width):
            tone = ramp["base"]
            if dx <= 0:
                tone = ramp["light"]
            elif dx >= width - 1:
                tone = ramp["deep"]
            elif dx == width - 2 and width > 4:
                tone = ramp["deep"]
            c.put(x + dx, y, tone)
        if y % 7 == 3 and width > 3:
            c.put(x + rng.range(1, width - 2), y, notch)
        if y % 11 == 5 and width > 4:
            c.put(x + 1, y, ramp["hi"])
    return path_x


def _roots(c, rng, ramp, base_x, base_y, half_w):
    """Flared root nubs on both sides of the trunk foot."""
    for side in (-1, 1):
        reach = half_w + rng.range(2, 4)
        for i in range(reach):
            x = base_x + side * (half_w - 1 + i)
            y = base_y - max(0, (reach - i) // 3) + 1
            c.put(x, y, ramp["deep"] if i > reach - 3 else ramp["base"])
            c.put(x, y + 1, ramp["deep"])
        c.put(base_x + side * (half_w + reach - 2), base_y + 1, ramp["deep"])


# --- Nimbus Willow (Driftlands) ---------------------------------------------

def _nimbuswillow_cell(variant):
    """v0.5 art sprint: canopy mass doubled (35% -> ~65% of the cell — the
    vanilla willow reference fills 71%) with four distinct silhouettes."""
    cell = Canvas(128, 128)
    rng = Rng(0x1717B05 + variant * 4099)
    wood = palette.NIMBUSWOOD
    leaf = palette.NIMBUSLEAF
    base_x, base_y = 64 + rng.pick((-2, 2)), 123

    cell.ellipse(base_x + 3, base_y + 2, 30, 5, with_alpha(SHADOW, 110))

    body = Canvas(128, 128)
    _trunk(body, rng, wood, base_x, base_y, 50, 17, 8,
           amp=rng.pick((2.0, 2.5)), drift=rng.pick((-5, 5)))
    _roots(body, rng, wood, base_x, base_y, 9)

    # canopy: BIG heavily-overlapping cloud lobes filling the frame width,
    # crown reaching near the top edge; per-variant mass distribution
    if variant == 0:
        lobes = [(64, 18, 26, 15), (36, 26, 21, 14), (93, 25, 20, 13),
                 (64, 41, 37, 21), (20, 46, 17, 13), (108, 44, 17, 13),
                 (40, 60, 18, 12), (88, 59, 17, 12), (64, 66, 20, 11)]
        strand_top = 74
    elif variant == 1:
        lobes = [(56, 16, 24, 14), (92, 22, 22, 14), (30, 32, 19, 13),
                 (62, 37, 35, 20), (102, 48, 16, 12), (24, 54, 16, 12),
                 (52, 58, 19, 12), (86, 57, 18, 12), (68, 68, 16, 10)]
        strand_top = 78
    elif variant == 2:
        lobes = [(72, 20, 27, 15), (40, 28, 22, 14), (98, 34, 18, 13),
                 (60, 43, 36, 21), (18, 52, 16, 12), (106, 56, 15, 12),
                 (44, 62, 18, 12), (84, 63, 17, 11)]
        strand_top = 80
    else:
        lobes = [(60, 20, 25, 14), (94, 30, 19, 13), (34, 24, 20, 13),
                 (66, 39, 35, 21), (22, 48, 17, 12), (104, 50, 16, 12),
                 (48, 62, 18, 11), (90, 61, 16, 11), (66, 70, 15, 10)]
        strand_top = 82
    _lobed_mass(body, lobes, leaf)
    _canopy_volume(body, lobes, leaf, rng)

    body.outline(palette.OUTLINE)
    _sunlit_rim(body, leaf["hi"], leaf["light"], y_max=strand_top)

    # weeping strands AFTER the outline pass (a generic outline would eat
    # them). Chunky 2px cores with a deep trail edge, strongly staggered
    # lengths (long-short alternation like vanilla willow), each rooted
    # INSIDE the canopy underside and tipped with outline so it grounds.
    long_next = rng.chance(0.5)
    for sx in range(14, 116, 6):
        sx += rng.range(-1, 1)
        edge = _bottom_edge(body, sx, strand_top + 4)
        if edge is None or edge < 26 or not rng.chance(0.92):
            continue
        if long_next:
            ln = rng.range(22, 32) - abs(sx - 64) // 5
        else:
            ln = rng.range(10, 15)
        long_next = not long_next
        x, top = sx, edge - 2
        # light attachment clump where the strand leaves the canopy
        body.put(x, top, leaf["light"])
        body.put(x + 1, top, leaf["light"])
        body.put(x, top - 1, leaf["light"])
        for i in range(ln):
            if i > 2 and i % rng.pick((5, 6, 7)) == 0:
                x += rng.pick((-1, 0, 1))
            tone = leaf["light"] if i < 3 else (
                leaf["base"] if i < ln - 4 else leaf["deep"])
            body.put(x, top + i, tone)
            body.put(x + 1, top + i, leaf["base"] if 2 < i < ln - 6 else leaf["deep"])
            body.put(x + 2, top + i, leaf["deep"])
        body.put(x, top + ln, palette.OUTLINE)
        body.put(x + 1, top + ln, palette.OUTLINE)
        body.put(x + 2, top + ln - 1, palette.OUTLINE)
        body.put(x, top + ln - 1, leaf["deep"])
    # one tiny detached drift-puff beside the crown (sky-island charm)
    px_, py_ = ((112, 12), (14, 14), (110, 18), (18, 10))[variant]
    body.ellipse(px_, py_, 4, 2.4, leaf["base"])
    body.ellipse(px_ - 1, py_ - 1, 2.4, 1.4, leaf["hi"])
    body.put(px_ + 3, py_ + 2, leaf["deep"])

    cell.paste(body, 0, 0)
    return cell


def gen_nimbuswillow(path):
    """128x512 tree sheet: Driftlands cloud-canopy willow, 4 variants."""
    sheet = Canvas(128, 512)
    for v in range(4):
        sheet.paste(_nimbuswillow_cell(v), 0, v * 128)
    sheet.save(path)


# --- Fulgur Pine (Stormveil) -------------------------------------------------

def _bough_tier(body, rng, ndl, cx, cy, span_l, span_r, tilt=0):
    """One needle tier as 3-4 overlapping droopy boughs (ellipse blobs with
    sagging outer tips) — volumetric, never a flat saucer. cy = tier center."""
    boughs = [(-0.62, 0.45, 4.6, 1), (0.58, 0.42, 4.2, 1),
              (-0.2, 0.55, 5.0, -1), (0.25, 0.5, 4.6, -1)]
    lobes = []
    for (px_, w, h, dy) in boughs:
        span = span_l if px_ < 0 else span_r
        lx = cx + px_ * span + rng.range(-1, 1)
        lobes.append((lx, cy + dy + tilt * px_, max(4, w * span), h))
    # sagging tips at the tier's outer ends
    lobes.append((cx - span_l + 2, cy + 2 + rng.range(0, 1), 4, 3.0))
    lobes.append((cx + span_r - 2, cy + 2 + rng.range(0, 1), 4, 2.8))
    _lobed_mass(body, lobes, ndl, fold=False, sheen=False)
    return lobes


def _needle_texture(body, rng, ndl, cx, cy, span_l, span_r, h=9):
    """Post-outline needle-clump ticks: lit 2px ticks upper-left, deep ticks
    lower-right, sparse hi glints along the top."""
    for _ in range((span_l + span_r)):
        x = cx + rng.range(-span_l + 2, span_r - 2)
        y = cy + rng.range(-h // 2, h // 2 + 2)
        p = body.get(x, y)
        if p[3] == 0 or p[:3] == palette.OUTLINE:
            continue
        rel = 0.55 * (x - cx) / max(span_r, 1) + (y - cy) / max(h * 0.6, 1)
        if rel < -0.15:
            body.put(x, y, ndl["light"])
            body.put(x + 1, y, ndl["light"])
            if rng.chance(0.3):
                body.put(x - 1, y - 1, ndl["hi"])
        elif rel > 0.2:
            body.put(x, y, ndl["deep"])
            body.put(x + rng.pick((1, 1, -1)), y + rng.pick((0, 1)), ndl["deep"])


def _needle_fringe(body, rng, ndl, y_limit=118):
    """Ragged hanging needle clumps along the canopy's lower edges, drawn
    after the outline so the silhouette gets vanilla's spiky droop."""
    for x in range(16, 113, 3):
        edge = _bottom_edge(body, x, y_limit)
        if edge is None or edge > y_limit - 2 or not rng.chance(0.7):
            continue
        p = body.get(x, edge)[:3]
        if p != palette.OUTLINE:
            continue
        ln = rng.range(1, 3)
        for k in range(ln):
            body.put(x, edge + 1 + k, ndl["deep"])
            if k == 0 and rng.chance(0.5):
                body.put(x + 1, edge + 1, ndl["deep"])
        body.put(x, edge + 1 + ln, palette.OUTLINE)


def _fulgurpine_cell(variant):
    cell = Canvas(128, 128)
    rng = Rng(0xF7169 + variant * 5077)
    wood = palette.CHARWOOD
    ndl = palette.FULGURPINE_NEEDLE
    base_x, base_y = 64 + rng.pick((-1, 1)), 123

    cell.ellipse(base_x + 1, base_y + 2, 18, 3.6, with_alpha(SHADOW, 110))

    body = Canvas(128, 128)
    top_y = 22 if variant in (0, 2) else 62
    _trunk(body, rng, wood, base_x, base_y, top_y, 10, 3,
           amp=1.5, drift=rng.pick((-2, 2)))
    _roots(body, rng, wood, base_x, base_y, 5)

    if variant in (0, 2):
        # asymmetric cone of overlapping bough tiers, trunk in the gaps
        # (v2 = taller, narrower spire)
        s = 0.85 if variant == 2 else 1.0
        tiers = [(base_x, 103, int(30 * s), 25, 1), (base_x + 1, 87, int(22 * s), 28, -1),
                 (base_x - 1, 71, int(25 * s), 19, 1), (base_x + 1, 56, int(16 * s), 21, -1),
                 (base_x, 42, int(17 * s), 13, 0), (base_x, 30, int(11 * s), 10, 0)]
        tier_lobes = []
        for (cx, cy, sl, sr, tl) in tiers:
            tier_lobes.extend(_bough_tier(body, rng, ndl, cx, cy, sl, sr, tl))
        # pointed crown clump
        crown = [(base_x, 21, 6, 4), (base_x - 1, 15, 4, 3.2),
                 (base_x + 1, 10, 2.6, 2.6)]
        _lobed_mass(body, crown, ndl, fold=False, sheen=False)
        tier_lobes.extend(crown)
        # v0.6 volume: overlap shadows between tiers + one global light field
        _canopy_volume(body, tier_lobes, ndl, rng, light_spread=0.5)
        # re-assert the trunk through the tier gaps (pale bark flecks so the
        # trunk clearly reads between boughs like vanilla pine)
        for gy in (94, 78, 63, 48):
            for dy in range(3):
                body.put(base_x - 1, gy + dy, wood["light"])
                body.put(base_x, gy + dy, wood["base"])
                body.put(base_x + 1, gy + dy, wood["deep"])
            body.put(base_x, gy + 1, wood["hi"])
    else:
        # lightning-split fork: needle side survives, charred snag side dies
        tiers = [(base_x, 105, 28, 24, 1), (base_x - 2, 90, 24, 20, -1),
                 (base_x - 3, 75, 19, 15, 1)]
        tier_lobes = []
        for (cx, cy, sl, sr, tl) in tiers:
            tier_lobes.extend(_bough_tier(body, rng, ndl, cx, cy, sl, sr, tl))
        # living prong: leans up-left with two smaller tiers
        for i in range(22):
            x = base_x - 2 - round(i * 0.45)
            y = 64 - i
            body.put(x, y, wood["light"] if i % 5 == 0 else wood["base"])
            body.put(x + 1, y, wood["base"])
            body.put(x + 2, y, wood["deep"])
        small = [(base_x - 8, 52, 12, 9, -1), (base_x - 12, 38, 9, 7, 0)]
        for (cx, cy, sl, sr, tl) in small:
            tier_lobes.extend(_bough_tier(body, rng, ndl, cx, cy, sl, sr, tl))
        tiers += small
        _canopy_volume(body, tier_lobes, ndl, rng, light_spread=0.5)
        # dead prong: thick charred snag leaning hard right, jagged, with
        # stub branches and a splintered broken tip — the lightning scar
        fx, fy = float(base_x + 2), 66.0
        ang = 64.0
        stubs = []
        for i in range(24):
            ang += rng.pick((-4, -1, 2)) - (6 if i > 18 else 0)
            fx += math.cos(math.radians(ang))
            fy -= math.sin(math.radians(ang))
            x, y = round(fx), round(fy)
            w = 4 if i < 7 else (3 if i < 16 else 2)
            for dx in range(w):
                body.put(x + dx, y,
                         wood["deep"] if dx >= w - 1 else
                         (wood["light"] if dx == 0 and i % 4 == 0 else wood["base"]))
            if i in (6, 13):
                stubs.append((x, y, 1 if i == 6 else -1))
        for (sx_, sy_, sd) in stubs:         # short charred stub branches
            for k in range(4):
                body.put(sx_ + 2 + k, sy_ - (k if sd > 0 else -k // 2), wood["base"])
                body.put(sx_ + 2 + k, sy_ + 1 - (k if sd > 0 else -k // 2), wood["deep"])
            body.put(sx_ + 6, sy_ - (4 if sd > 0 else -2), wood["deep"])
        # splintered break: pale exposed wood + charred spike beside it
        tipx, tipy = round(fx), round(fy)
        body.put(tipx, tipy - 1, wood["hi"])
        body.put(tipx + 1, tipy - 2, wood["hi"])
        body.put(tipx, tipy - 2, wood["light"])
        body.put(tipx + 1, tipy - 1, wood["light"])
        body.put(tipx + 2, tipy, wood["deep"])
        body.put(tipx - 1, tipy - 1, wood["deep"])
        # pale lightning scar zigzagging down the trunk from the split
        sx = base_x + 1
        for k in range(8):
            body.put(sx + (k % 2), 68 + k * 2, wood["hi"])

    body.outline(palette.OUTLINE)
    _sunlit_rim(body, ndl["hi"], ndl["light"], y_max=base_y - 10)
    for (cx, cy, sl, sr, tl) in tiers:
        _needle_texture(body, rng, ndl, cx, cy, sl, sr)
    if variant in (0, 2):
        _needle_texture(body, rng, ndl, base_x, 16, 5, 5, h=12)
        embers = ((base_x - 2, 116), (base_x + 2, 95), (base_x - 3, 64))
    else:
        _needle_fringe(body, rng, ndl)
        # sparse ember clusters still glowing in the charred bark (SPARSE:
        # a bright core pixel + one dimmer neighbor reads at 1x)
        embers = ((base_x + 6, 58), (round(fx) - 2, round(fy) + 3),
                  (base_x - 2, 117))
    for (ex, ey) in embers:
        if body.filled(ex, ey):
            body.put(ex, ey, wood["ember"])
            body.put(ex + 1, ey + 1, with_alpha(wood["ember"], 150))

    cell.paste(body, 0, 0)
    return cell


def gen_fulgurpine(path):
    """128x512 tree sheet: Stormveil lightning-charred pine, 4 variants
    (0/2 = full cone & narrow spire, 1/3 = lightning-split fork)."""
    sheet = Canvas(128, 512)
    for v in range(4):
        sheet.paste(_fulgurpine_cell(v), 0, v * 128)
    sheet.save(path)


# --- Prisma Birch (Aurora Shoals) --------------------------------------------

_BARK_DASH = palette.NIGHTFELL["hi"]     # violet-gray birch bark dashes


def _prismabirch_cell(variant):
    cell = Canvas(128, 128)
    rng = Rng(0xB123C4 + variant * 6151)
    wood = palette.PRISMWOOD
    leaf = palette.PRISMLEAF
    base_x, base_y = 64 + rng.pick((-2, 2)), 123

    cell.ellipse(base_x + 2, base_y + 2, 21, 4, with_alpha(SHADOW, 110))

    body = Canvas(128, 128)
    path_x = _trunk(body, rng, wood, base_x, base_y, 48, 10, 6,
                    amp=1.2, drift=3 if variant else -3,
                    notch_tone=_BARK_DASH)
    _roots(body, rng, wood, base_x, base_y, 5)
    # birch-bark dashes: bold staggered dark strokes down the trunk
    side = 1
    for y in range(56, base_y - 4, 4):
        info = path_x.get(y)
        if info is None:
            continue
        x, width = info
        dx = 1 + (width - 3) * (side + 1) // 2 + rng.range(0, 1)
        ln = rng.range(2, 3)
        for k in range(ln):
            body.put(x + dx + k, y, palette.OUTLINE)
        body.put(x + dx + rng.pick((0, 1)), y + 1, _BARK_DASH)
        side = -side

    # canopy: lumpy mushroom dome of overlapping lobes — v0.5: four distinct
    # silhouettes, mass pushed toward the vanilla birch reference
    if variant == 0:
        dome = [(64, 28, 34, 22), (38, 40, 18, 13), (91, 37, 17, 12),
                (46, 13, 16, 10), (83, 14, 15, 10), (64, 47, 22, 13),
                (22, 32, 11, 8), (106, 28, 10, 8), (112, 40, 8, 7)]
    elif variant == 1:
        dome = [(60, 26, 27, 19), (36, 38, 15, 11), (85, 40, 16, 12),
                (72, 12, 14, 9), (46, 18, 13, 9), (62, 44, 19, 11),
                (98, 26, 10, 9), (24, 44, 9, 7)]
    elif variant == 2:
        dome = [(66, 28, 31, 21), (34, 36, 18, 13), (94, 34, 18, 13),
                (52, 14, 16, 10), (84, 15, 15, 10), (58, 46, 22, 13),
                (20, 30, 11, 8), (108, 26, 10, 8), (76, 50, 14, 9)]
    else:
        dome = [(58, 30, 29, 20), (88, 42, 15, 11), (38, 44, 15, 11),
                (44, 18, 16, 10), (78, 16, 14, 9), (70, 48, 19, 11),
                (104, 32, 9, 8), (22, 36, 9, 7), (64, 14, 12, 8)]
    _lobed_mass(body, dome, leaf)
    _canopy_volume(body, dome, leaf, rng)

    # clustered iridescent leaf texture (clumps, not salt-and-pepper):
    cx0, cy0, rx0, ry0 = dome[0]

    def clump(x, y, tone, size):
        for _ in range(size):
            body.put(x, y, tone)
            body.put(x + 1, y, tone)
            x += rng.pick((-1, 1, 1, 0))
            y += rng.pick((0, 1, -1))

    for _ in range(30):
        x = cx0 + rng.range(-rx0 - 8, rx0 + 8)
        y = cy0 + rng.range(-ry0 - 6, ry0 + 12)
        if not body.filled(x, y) or y > 56:
            continue
        rel = 0.6 * (x - cx0) / rx0 + (y - cy0) / ry0
        if rel < -0.3:
            tone = leaf["hi"] if rng.chance(0.35) else leaf["light"]
        elif rel > 0.35:
            tone = leaf["deep"]
        else:
            tone = leaf["light"] if rng.chance(0.4) else leaf["deep"]
        clump(x, y, tone, rng.range(2, 4))
    # the Shoals' exclusive accents: few distinct teal/rose clusters
    accents = ((-16, 2, "teal"), (10, -8, "rose"), (1, 10, "teal"),
               (-6, -13, "rose"), (19, 4, "teal"), (-24, -3, "rose"))
    for (ax, ay, key) in accents:
        x, y = cx0 + ax + rng.range(-2, 2), cy0 + ay + rng.range(-1, 1)
        if body.filled(x, y) and body.filled(x + 2, y + 1):
            body.put(x, y, leaf[key])
            body.put(x + 1, y, leaf[key])
            body.put(x + rng.pick((0, 1)), y + 1, leaf[key])
            body.put(x + rng.pick((1, 2)), y - 1, leaf[key])
            body.put(x - 1, y - 1, leaf["hi"])       # sparkle catch-light

    body.outline(palette.OUTLINE)
    _sunlit_rim(body, leaf["hi"], leaf["light"], y_max=58)
    # scalloped deep underside so the dome reads shaded over the trunk
    for x in range(cx0 - rx0 - 6, cx0 + rx0 + 7, 2):
        edge = _bottom_edge(body, x, 60)
        if edge is None or edge < 30:
            continue
        if body.get(x, edge)[:3] == palette.OUTLINE and body.filled(x, edge - 1):
            body.put(x, edge - 1, leaf["deep"])
            if x % 6 == 0:
                body.put(x, edge - 2, leaf["deep"])

    cell.paste(body, 0, 0)
    return cell


def gen_prismabirch(path):
    """128x512 tree sheet: Aurora Shoals iridescent birch, 4 variants."""
    sheet = Canvas(128, 512)
    for v in range(4):
        sheet.paste(_prismabirch_cell(v), 0, v * 128)
    sheet.save(path)


# --- Falling-leaf particle strips (100x20: five 20px frames) -----------------

def gen_nimbuswillow_leaves(path):
    """Tiny drifting cloud-puffs shed by the nimbus willow."""
    sheet = Canvas(100, 20)
    leaf = palette.NIMBUSLEAF
    for f in range(5):
        c = Canvas(20, 20)
        rng = Rng(0x11EAF + f * 271)
        cx, cy = 9 + rng.range(-1, 1), 10 + rng.range(-1, 1)
        rx = 3.4 + (f % 3) * 0.5
        c.ellipse(cx, cy, rx, 2.2, leaf["base"])
        c.ellipse(cx + rng.pick((-2, 2)), cy + 1, rx * 0.55, 1.4, leaf["base"])
        c.ellipse(cx - 1, cy - 1, rx * 0.55, 1.2, leaf["hi"])
        c.put(cx + int(rx) - 1, cy + 1, leaf["deep"])
        c.put(cx - int(rx) + 1, cy + 2, leaf["deep"])
        c.put(cx + rng.range(-1, 1), cy + 2, leaf["deep"])
        sheet.paste(c, f * 20, 0)
    sheet.save(path)


def gen_fulgurpine_leaves(path):
    """Charred needle sprigs: a SOLID tuft mass (silhouette-first — dots
    would shred) with pale needle ticks laid on top, tumbled per frame."""
    sheet = Canvas(100, 20)
    ndl = palette.FULGURPINE_NEEDLE
    wood = palette.CHARWOOD
    shapes = (  # (dx, dy) offsets of the solid clump mass, per frame
        ((0, 0), (1, 0), (2, 0), (3, 1), (1, 1), (2, 1), (4, 1), (3, 2), (4, 2), (5, 2)),
        ((0, 2), (1, 2), (1, 1), (2, 1), (3, 1), (2, 0), (3, 0), (4, 0), (2, 2), (3, 2)),
        ((0, 0), (0, 1), (1, 1), (1, 2), (2, 2), (1, 0), (2, 3), (2, 1), (3, 3), (3, 2)),
        ((0, 1), (1, 0), (1, 1), (2, 0), (3, 0), (2, 1), (4, 0), (3, 1), (4, 1), (5, 0)),
        ((0, 3), (1, 2), (1, 3), (2, 1), (2, 2), (3, 1), (3, 0), (4, 0), (2, 3), (3, 2)),
    )
    for f in range(5):
        c = Canvas(20, 20)
        rng = Rng(0xF17EA + f * 353)
        ox, oy = 7, 8 + rng.range(-1, 1)
        for (dx, dy) in shapes[f]:               # deep silhouette mass
            c.put(ox + dx, oy + dy, ndl["deep"])
            c.put(ox + dx, oy + dy + 1, ndl["deep"])
        for i, (dx, dy) in enumerate(shapes[f]):  # lit needle ticks on top
            if i % 3 == 0:
                c.put(ox + dx, oy + dy, ndl["light"])
            elif i % 3 == 1:
                c.put(ox + dx, oy + dy, ndl["base"])
        # twig nub + one hi glint
        (tx, ty) = shapes[f][0]
        c.put(ox + tx - 1, oy + ty + 1, wood["deep"])
        c.put(ox + tx - 2, oy + ty + 2, wood["base"])
        (hx, hy) = shapes[f][rng.range(3, 6)]
        c.put(ox + hx, oy + hy - 1, ndl["hi"])
        sheet.paste(c, f * 20, 0)
    sheet.save(path)


def gen_prismabirch_leaves(path):
    """Tumbling iridescent birch leaves; frames 2 and 4 catch a teal/rose
    glint mid-tumble."""
    sheet = Canvas(100, 20)
    leaf = palette.PRISMLEAF
    for f in range(5):
        c = Canvas(20, 20)
        rng = Rng(0xB1EAF + f * 431)
        cx, cy = 9, 10
        w = (4, 3, 2, 3, 4)[f]                   # tumble squash
        h = (2, 3, 4, 3, 2)[f]
        for dy in range(-h, h + 1):
            span = max(0, round(w * (1 - abs(dy) / (h + 0.5))))
            for dx in range(-span, span + 1):
                edge = abs(dx) == span or abs(dy) == h
                c.put(cx + dx, cy + dy, leaf["deep"] if edge else leaf["base"])
        c.put(cx - 1, cy - 1, leaf["light"])
        c.put(cx, cy - 1, leaf["light"])
        if f == 2:
            c.put(cx, cy, leaf["teal"])
        elif f == 4:
            c.put(cx - 1, cy, leaf["rose"])
        else:
            c.put(cx - w + 1, cy, leaf["light"])
        c.put(cx + rng.pick((-1, 1)), cy + h, leaf["deep"])   # stem nub
        sheet.paste(c, f * 20, 0)
    sheet.save(path)


# --- Log-bundle item icons (32x32) ------------------------------------------

def _log_icon(c, ramp, rng, dash_tone=None, cap_hi=None):
    """One chunky diagonal log, vanilla items/pinelog build: rounded bark
    cylinder lower-left -> upper-right, big ringed end cap facing the
    viewer on the left end, bark dashes along the length."""
    x0, y0, x1, y1 = 10, 19, 24, 12
    steps = 12
    axis = [(x0 + (x1 - x0) * i / steps, y0 + (y1 - y0) * i / steps)
            for i in range(steps + 1)]
    for (cx, cy) in axis:                       # rounded body
        c.ellipse(cx, cy, 4.4, 4.6, ramp["base"])
    for (cx, cy) in axis[2:]:                   # sunlit top-left band
        c.ellipse(cx - 0.5, cy - 2.2, 2.6, 1.5, ramp["light"])
    for (cx, cy) in axis[1:]:                   # shaded belly
        c.ellipse(cx + 0.6, cy + 2.6, 2.8, 1.2, ramp["deep"])
    # far cut end (small, dark)
    c.ellipse(x1 + 1, y1 - 0.5, 1.6, 3.4, ramp["deep"])
    # bark dashes
    for _ in range(3):
        i = rng.range(3, steps - 1)
        gx, gy = int(axis[i][0]), int(axis[i][1]) + rng.range(-1, 2)
        c.put(gx, gy, dash_tone if dash_tone else ramp["deep"])
        c.put(gx + 1, gy + 1, dash_tone if dash_tone else ramp["deep"])
    # big ringed end cap on the near end
    c.ellipse(x0 - 2, y0 + 1, 3.4, 4.4, ramp["deep"])
    c.ellipse(x0 - 2, y0 + 1, 2.4, 3.2, cap_hi if cap_hi else ramp["hi"])
    c.ellipse(x0 - 2, y0 + 1.5, 1.3, 1.8, ramp["light"])
    c.put(x0 - 2, y0, ramp["hi"] if cap_hi is None else cap_hi)
    c.put(x0 - 1, y0 + 2, ramp["deep"])         # growth-ring tick
    c.outline(palette.OUTLINE)


def gen_nimbuswood_item(path):
    c = Canvas(32, 32)
    _log_icon(c, palette.NIMBUSWOOD, Rng(0x817B))
    c.save(path)


def gen_charwood_item(path):
    c = Canvas(32, 32)
    wood = palette.CHARWOOD
    # charred log: pale scorched heartwood cap, cracked bark, one live ember
    _log_icon(c, wood, Rng(0xC4A2), dash_tone=wood["hi"], cap_hi=wood["hi"])
    c.put(17, 14, wood["ember"])
    c.put(20, 17, with_alpha(wood["ember"], 170))
    c.save(path)


def gen_prismwood_item(path):
    c = Canvas(32, 32)
    _log_icon(c, palette.PRISMWOOD, Rng(0x9817), dash_tone=_BARK_DASH)
    c.put(21, 11, palette.PRISMLEAF["teal"])   # iridescent glints on the grain
    c.put(14, 20, palette.PRISMLEAF["rose"])
    c.save(path)


# --- Saplings (32x32 object sprites) -----------------------------------------
# Size law: measured vanilla analogues (cell 0 opaque bbox / pixels) —
# pinesapling 16x28 / 288 px, willowsapling 16x26 / 280 px, birchsapling
# 24x26 / 336 px. Vanilla saplings are chunky MINI-TREES (full crown on a
# thick stub trunk over a dark base mound), not thin sprouts; each of ours
# targets >= 80% of its analogue's opaque mass and is audited in QA.

def _sapling_base(body, rng, wood, mound_top, mound_deep, cx=15):
    """Stub trunk + soil mound, ground-anchored bottom-center. Returns the
    y of the trunk top where the crown sits."""
    lean = rng.pick((-1, 0, 1))
    for i in range(11):                          # trunk stub, 4px wide
        y = 27 - i
        x = cx - 1 + (lean if i > 5 else 0)
        body.put(x, y, wood["light"])
        body.put(x + 1, y, wood["base"])
        body.put(x + 2, y, wood["base"])
        body.put(x + 3, y, wood["deep"])
    body.put(cx, 24, wood["deep"])               # bark notch
    body.put(cx + 1, 20, wood["hi"])
    # rooty soil mound over the trunk foot
    body.ellipse(cx + 1, 28.2, 6.5, 2.6, mound_deep)
    body.ellipse(cx + 1, 27.4, 5.0, 1.8, mound_top)
    body.put(cx - 4, 27, mound_deep)
    body.put(cx + 6, 28, mound_top)
    return 17 + (lean if lean else 0)


def gen_saplings(dir_path):
    """32x32 sapling objects: nimbussapling / fulgursapling / prismasapling.
    Mini versions of their parent trees (same palettes, same construction
    language) standing in a small soil mound with a soft ground shadow."""
    turf = palette.CLOUDTURF
    slate = palette.STORMSLATE

    # --- Nimbus Willow sapling: little cloud crown + two weeping nubs
    cell = Canvas(32, 32)
    rng = Rng(0x5A811)
    cell.ellipse(16, 29, 8.5, 2.2, with_alpha(SHADOW, 110))
    body = Canvas(32, 32)
    _sapling_base(body, rng, palette.NIMBUSWOOD, turf["tuft"], turf["deep"])
    leaf = palette.NIMBUSLEAF
    _lobed_mass(body, [(11, 12, 6.5, 5.0), (20, 11, 6.0, 4.6),
                       (15, 6, 6.0, 4.4), (15, 13, 6.5, 5.0)], leaf)
    body.outline(palette.OUTLINE)
    _sunlit_rim(body, leaf["hi"], leaf["light"], y_max=20)
    for (sx, ln) in ((10, 4), (21, 3)):          # weeping strand nubs
        edge = _bottom_edge(body, sx, 24)
        if edge is not None:
            for i in range(ln):
                body.put(sx, edge - 1 + i, leaf["base"] if i < ln - 1 else leaf["deep"])
                body.put(sx + 1, edge - 1 + i, leaf["deep"])
            body.put(sx, edge - 1 + ln, palette.OUTLINE)
    body.put(13, 4, leaf["hi"])
    cell.paste(body, 0, 0)
    cell.save(f"{dir_path}/nimbussapling.png")

    # --- Fulgur Pine sapling: two droopy needle tiers + spiky top, 1 ember
    cell = Canvas(32, 32)
    rng = Rng(0x5AF16)
    cell.ellipse(16, 29, 8, 2.2, with_alpha(SHADOW, 110))
    body = Canvas(32, 32)
    wood = palette.CHARWOOD
    _sapling_base(body, rng, wood, slate["light"], slate["deep"])
    ndl = palette.FULGURPINE_NEEDLE
    _lobed_mass(body, [(11, 17, 6.0, 3.2), (20, 16, 5.5, 3.0),
                       (15, 16, 5.0, 3.4)], ndl, fold=False, sheen=False)
    _lobed_mass(body, [(13, 10, 5.0, 3.0), (18, 9, 4.6, 2.8)],
                ndl, fold=False, sheen=False)
    for k in range(6):                           # spiky crown tip
        w = max(1, 2 - k // 2)
        for dx in range(-w, w + 1):
            body.put(15 + dx, 6 - k, ndl["deep"] if abs(dx) >= w else ndl["base"])
    body.outline(palette.OUTLINE)
    _sunlit_rim(body, ndl["hi"], ndl["light"], y_max=22)
    for (tx, ty) in ((11, 15), (17, 9), (13, 10)):   # lit needle ticks
        body.put(tx, ty, ndl["light"])
        body.put(tx + 1, ty, ndl["light"])
    for (tx, ty) in ((19, 18), (14, 19), (21, 11)):  # deep clump ticks
        if body.filled(tx, ty):
            body.put(tx, ty, ndl["deep"])
    for x in range(11, 21, 3):                   # hanging fringe nubs
        edge = _bottom_edge(body, x, 23)
        if edge is not None and body.get(x, edge)[:3] == palette.OUTLINE:
            body.put(x, edge + 1, ndl["deep"])
            body.put(x, edge + 2, palette.OUTLINE)
    if body.filled(15, 22):
        body.put(15, 22, wood["ember"])          # one live ember on the stub
    cell.paste(body, 0, 0)
    cell.save(f"{dir_path}/fulgursapling.png")

    # --- Prisma Birch sapling: round dome + accents, dashed pale trunk
    cell = Canvas(32, 32)
    rng = Rng(0x5AB12)
    cell.ellipse(16, 29, 8.5, 2.2, with_alpha(SHADOW, 110))
    body = Canvas(32, 32)
    _sapling_base(body, rng, palette.PRISMWOOD, turf["tuft"], turf["deep"])
    leaf = palette.PRISMLEAF
    _lobed_mass(body, [(15, 9, 8.5, 6.5), (9, 13, 4.5, 3.6),
                       (22, 12, 4.5, 3.8)], leaf)
    body.outline(palette.OUTLINE)
    _sunlit_rim(body, leaf["hi"], leaf["light"], y_max=18)
    for (tx, ty, tone) in ((11, 7, leaf["light"]), (13, 5, leaf["hi"]),
                           (18, 13, leaf["deep"]), (13, 14, leaf["deep"]),
                           (19, 6, leaf["light"])):
        body.put(tx, ty, tone)
        body.put(tx + 1, ty, tone)
    body.put(11, 11, leaf["teal"])               # the Shoals' accents
    body.put(12, 11, leaf["teal"])
    body.put(19, 9, leaf["rose"])
    body.put(20, 9, leaf["rose"])
    body.put(14, 22, palette.OUTLINE)            # birch bark dashes
    body.put(15, 22, _BARK_DASH)
    body.put(15, 25, palette.OUTLINE)
    body.put(16, 25, _BARK_DASH)
    cell.paste(body, 0, 0)
    cell.save(f"{dir_path}/prismasapling.png")
