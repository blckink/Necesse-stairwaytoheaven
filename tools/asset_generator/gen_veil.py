"""All Veil (v0.3) sprites: terrain splat materials, murkwater, fen flora,
seance circle, the rift pair, the ghost lantern and the Gloom Shade."""

from px import Canvas, Rng, with_alpha
import palette
import gen_splats

CELL = 64


# --- terrain splat materials --------------------------------------------------

def material_murkmoss(c, x0, y0, salt, frame=0):
    gen_splats._speckle_cell(c, x0, y0, palette.MURKMOSS, 0x3E110000, density=0.05)


def features_murkmoss(c, x0, y0, salt, k):
    m = palette.MURKMOSS
    rng = Rng(salt)
    if k == 0:
        c.put(x0 + rng.range(6, 26), y0 + rng.range(6, 26), m["hi"])
        return
    if k == 1:  # moss tuft clusters
        for _ in range(2):
            tx, ty = x0 + rng.range(5, 26), y0 + rng.range(6, 26)
            for (dx, dy) in ((0, 0), (1, 0), (-1, 0), (0, -1), (1, -2)):
                c.put(tx + dx, ty + dy, m["tuft"])
            c.put(tx, ty + 1, m["deep"])
    elif k == 2:  # wet sheen patch + spore glints
        mx, my = x0 + rng.range(10, 21), y0 + rng.range(10, 21)
        c.blob(mx, my, 4, m["light"], rng, lumps=3)
        c.put(mx, my, m["hi"])
        c.put(x0 + rng.range(4, 27), y0 + rng.range(4, 27), palette.GHOSTFLAME["deep"])
    else:  # root line + tuft
        rx, ry = x0 + rng.range(4, 16), y0 + rng.range(8, 24)
        for i in range(rng.range(7, 11)):
            c.put(rx + i, ry + (i // 3), m["deep"])
        c.put(x0 + rng.range(5, 26), y0 + rng.range(5, 26), m["tuft"])


def material_blackpeat(c, x0, y0, salt, frame=0):
    gen_splats._speckle_cell(c, x0, y0, palette.BLACKPEAT, 0xB1AC0000, density=0.06)


def features_blackpeat(c, x0, y0, salt, k):
    p = palette.BLACKPEAT
    rng = Rng(salt)
    if k == 0:
        return
    if k == 1:  # drying cracks
        x, y = x0 + rng.range(4, 14), y0 + rng.range(8, 24)
        for i in range(rng.range(8, 12)):
            c.put(x + i, y, p["deep"])
            if rng.chance(0.4):
                y += rng.pick((-1, 1))
        c.put(x + 2, y + 1, p["light"])
    elif k == 2:  # marsh-gas bubbles
        for _ in range(3):
            bx, by = x0 + rng.range(5, 26), y0 + rng.range(5, 26)
            c.put(bx, by, p["light"])
            c.put(bx, by - 1, p["hi"])
    else:
        rx, ry = x0 + rng.range(6, 20), y0 + rng.range(6, 20)
        for i in range(5):
            c.put(rx + i, ry, p["deep"])
        c.put(rx + 5, ry + 1, p["hi"])


def material_ashsand(c, x0, y0, salt, frame=0):
    A = palette.ASHSAND
    gen_splats._speckle_cell(c, x0, y0, A, 0xA5E50000, density=0.05)
    for x in range(32):
        for y in range(32):
            gx, gy = (x0 + x) % 32, (y0 + y) % 32
            m = (gx + 2 * gy) % 16
            h = Rng((gx * 7013 + gy * 331) ^ 0xA5E5D00D)
            if m == 0 and h.chance(0.55):
                c.put(x0 + x, y0 + y, A["light"])   # wind ripples
            elif m == 1 and h.chance(0.3):
                c.put(x0 + x, y0 + y, A["deep"])


def features_ashsand(c, x0, y0, salt, k):
    A = palette.ASHSAND
    rng = Rng(salt)
    if k == 0:
        return
    if k == 1:  # bone flecks
        for _ in range(2):
            bx, by = x0 + rng.range(5, 26), y0 + rng.range(5, 26)
            c.put(bx, by, palette.BONEASH["base"])
            c.put(bx + 1, by, palette.BONEASH["light"])
    elif k == 2:  # ember speck
        c.put(x0 + rng.range(6, 26), y0 + rng.range(6, 26), palette.GHOSTFLAME["deep"])
        c.put(x0 + rng.range(6, 26), y0 + rng.range(6, 26), A["hi"])
    else:  # small dune crest
        dx0, dy0 = x0 + rng.range(6, 18), y0 + rng.range(8, 24)
        for i in range(rng.range(6, 9)):
            c.put(dx0 + i, dy0 + i // 4, A["hi"])
            c.put(dx0 + i, dy0 + i // 4 + 1, A["deep"])


def material_murkwater(deep):
    """Black marsh water: near-still, with a few thick drifting ripple bands
    and rare green glints. 8-frame loop (bands shift 4px/frame, period 32)."""
    def painter(c, x0, y0, salt, frame=0):
        M = palette.MURKWATER
        base = M["deep"] if deep else M["base"]
        mid = M["base"] if deep else M["light"]
        lite = M["light"] if deep else M["hi"]
        shift = (frame * 4) % 32
        for x in range(32):
            for y in range(32):
                c.put(x0 + x, y0 + y, base)
        # two lazy ripple bands per tile, broken by hash gaps
        for band, gy in ((0, 6), (1, 21)):
            for x in range(32):
                gx = (x0 + x + shift * (1 if band == 0 else -1)) % 32
                h = Rng((gx * 733 + band * 7919) ^ 0x3E77A00D)
                wob = ((x0 + x) // 7 + band) % 2
                if h.chance(0.78):
                    c.put(x0 + x, y0 + (gy + wob) % 32, mid)
                    if h.chance(0.4):
                        c.put(x0 + x, y0 + (gy + wob + 1) % 32, lite)
        rng = Rng(salt & 0xFFFF0000)
        gx = rng.range(3, 28)
        gy2 = rng.range(3, 28)
        if frame in (2, 3):
            c.put(x0 + (gx + shift) % 32, y0 + gy2, M["glint"])
    return painter


# --- flora + deco -------------------------------------------------------------

def gen_whisperreeds(path, variants=4):
    W = palette.MURKMOSS
    pale = palette.BONEASH
    sheet = Canvas(variants * 32, 32)
    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0x3EED + v * 313)
        c.ellipse(16, 29, 8, 2, W["deep"])
        for s in range(rng.range(4, 6)):
            x = 7 + s * 5 + rng.range(-1, 1)
            h = rng.range(11, 17)
            lean = rng.pick((-2, -1, 1, 2))
            for i in range(h):
                t = i / h
                sx = x + round(lean * t * t * 1.5)
                mid = W["base"] if t < 0.6 else W["tuft"]
                c.put(sx - 1, 29 - i, W["deep"])
                c.put(sx, 29 - i, mid)
                c.put(sx + 1, 29 - i, W["deep"] if t < 0.5 else W["base"])
            c.put(sx, 29 - h, pale["light"])      # pale whisper-tip
            c.put(sx, 29 - h - 1, pale["hi"])
        c.outline(palette.OUTLINE)
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_gloomshroom(path):
    """64x32: 2 variants — a fat glowing cap shroom plus a small companion."""
    sheet = Canvas(64, 32)
    G = palette.GHOSTFLAME
    S = palette.NIGHTFELL
    for v in range(2):
        c = Canvas(32, 32)
        rng = Rng(0x5A00 + v * 977)

        def shroom(cx, ground, r):
            # stem
            c.rect(cx - 1, ground - r - 1, 3, r + 1, S["light"])
            c.put(cx + 1, ground - 2, S["base"])
            # cap: wide dome sitting ON the stem
            c.ellipse(cx, ground - r - 2, r + 3, r * 0.8 + 1.5, S["base"])
            c.ellipse(cx - 1, ground - r - 3, r + 1.5, r * 0.6 + 1, S["light"])
            c.ellipse(cx - 2, ground - r - 4, r * 0.6, r * 0.35, S["hi"])
            # glowing gill line under the rim + spots on the cap
            for dx in range(-r - 2, r + 3, 2):
                c.put(cx + dx, ground - r - 1, G["glow"])
            c.put(cx - r + 1, ground - r - 4, G["glow"])
            c.put(cx + 2, ground - r - 5, G["core"])

        big = rng.range(5, 6)
        if v == 0:
            shroom(11, 29, big)
            shroom(23, 29, 3)
        else:
            shroom(20, 29, big)
            shroom(8, 30, 3)
        c.outline(palette.OUTLINE)
        c.put(6 + v * 18, 14, with_alpha(G["glow"], 150))
        c.put(26 - v * 12, 18, with_alpha(G["glow"], 130))
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_ashbones(path):
    sheet = Canvas(64, 32)
    B = palette.BONEASH
    for v in range(2):
        c = Canvas(32, 32)
        rng = Rng(0xB0E5 + v * 131)
        cx = 16 + rng.pick((-2, 2))
        # half-buried ribcage: 4 arcs rising from the ash
        for i, rx in enumerate((10, 8, 6, 4)):
            h = 12 - i * 2
            x = cx - rx + i * (1 if v == 0 else 2)
            for j in range(h):
                t = j / max(h - 1, 1)
                px_ = x + round(rx * 0.9 * t)
                c.put(px_, 28 - j, B["base"] if j < h - 2 else B["light"])
                c.put(px_ + 1, 28 - j, B["deep"])
        # spine ridge + skull hint on variant 0
        for i in range(6):
            c.put(cx - 8 + i * 3, 28, B["deep"])
        if v == 0:
            c.ellipse(cx + 8, 26, 3.4, 3, B["base"])
            c.ellipse(cx + 8, 24, 2.6, 1.6, B["light"])
            c.put(cx + 7, 26, palette.OUTLINE)
            c.put(cx + 10, 26, palette.OUTLINE)
        c.outline(palette.OUTLINE)
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_seancecircle(path):
    c = Canvas(32, 32)
    chalk = palette.BONEASH
    G = palette.GHOSTFLAME
    # dashed chalk ring
    import math
    for a in range(0, 360, 9):
        if (a // 9) % 3 == 2:
            continue
        x = 16 + round(11 * math.cos(math.radians(a)))
        y = 20 + round(6.5 * math.sin(math.radians(a)))
        c.put(x, y, chalk["hi"])
        if a % 27 == 0:
            c.put(x, y + 1, chalk["base"])
    # center rune: small crescent
    for (mx, my) in ((15, 18), (14, 19), (14, 20), (15, 21), (16, 21)):
        c.put(mx, my, chalk["light"])
    # three candle stubs with tiny green flames
    for (cx_, cy_) in ((7, 14), (25, 15), (16, 26)):
        c.rect(cx_ - 1, cy_, 3, 4, chalk["light"])
        c.put(cx_ - 1, cy_ + 3, chalk["base"])
        c.put(cx_, cy_ - 1, G["glow"])
        c.put(cx_, cy_ - 2, G["core"])
    c.outline(palette.OUTLINE)
    c.put(16, 20, with_alpha(G["glow"], 160))
    c.save(path)


def _rift(path, brighter):
    """32x96 rift: floor ring plate (top cell) + a bright swirling vortex."""
    c = Canvas(32, 96)
    G = palette.GHOSTFLAME
    N = palette.NIGHTFELL
    stone = palette.VEILROCK
    import math
    # vortex column (y 40..92): each layer = dark disc + THICK bright swirl arc
    for i, cy in enumerate(range(85, 42, -7)):
        t = i / 6.0
        rx = 12 - round(5 * t)
        ry = 5 - round(2 * t)
        c.ellipse(16, cy, rx, ry, (18, 20, 26))
        c.ellipse(16, cy, rx * 0.8, ry * 0.8, N["deep"])
        # visible funnel rim: pale arc along each layer's top edge
        for a in range(15, 166, 6):
            x = 16 + round(rx * math.cos(math.radians(a)))
            y = cy - round(ry * math.sin(math.radians(a)))
            c.put(x, y, palette.SHADE["hi"])
        phase = i * 55
        for a in range(0, 200, 7):
            x = 16 + round((rx - 1) * math.cos(math.radians(a + phase)))
            y = cy - round((ry - 0.5) * math.sin(math.radians(a + phase)))
            tone = G["glow"] if a < 120 else G["deep"]
            c.put(x, y, tone)
            if a < 60:
                c.put(x, y + 1, G["deep"])
    # blazing core seam
    for y in range(46, 88):
        c.put(16, y, G["core"] if (y % 3 or brighter) else G["glow"])
        if y % 4 == 0:
            c.put(15, y, G["glow"])
            c.put(17, y + 1, G["glow"])
    c.outline(palette.OUTLINE)
    for (mx, my, a) in ((4, 52, 190), (28, 60, 190), (7, 74, 150), (26, 44, 150),
                        (16, 36, 220), (12, 32, 150), (21, 33, 150)):
        c.put(mx, my, with_alpha(G["glow"], a))
    # floor cell: cracked stone plate with a glowing ring
    c.rect(3, 6, 26, 22, stone["base"])
    c.rect(3, 6, 26, 1, stone["light"])
    c.rect(3, 27, 26, 1, stone["deep"])
    for a in range(0, 360, 8):
        x = 16 + round(10 * math.cos(math.radians(a)))
        y = 17 + round(7 * math.sin(math.radians(a)))
        c.put(x, y, G["glow"] if (a // 8) % 3 else G["core"])
    c.put(10, 12, stone["deep"])
    c.put(22, 21, stone["deep"])
    c.save(path)


def gen_riftdown(path):
    _rift(path, brighter=True)


def gen_riftup(path):
    _rift(path, brighter=False)


def gen_ghostlantern(path):
    """32x192 streetlamp sheet: green-flame lantern (on above, off below)."""
    sheet = Canvas(32, 192)
    iron = palette.IRONWORK
    G = palette.GHOSTFLAME
    stone = palette.VEILROCK
    for state in range(2):
        lit = state == 0
        c = Canvas(32, 96)
        # stone foot
        c.rect(9, 86, 14, 5, stone["base"])
        c.rect(9, 86, 14, 1, stone["light"])
        c.rect(11, 82, 10, 4, stone["base"])
        c.rect(11, 82, 10, 1, stone["light"])
        # crooked post with a shepherd's-crook top
        for y in range(30, 82):
            x = 16 + (1 if 40 < y < 60 else 0)
            c.put(x - 1, y, iron["light"])
            c.put(x, y, iron["base"])
            c.put(x + 1, y, iron["deep"])
        for i, (dx, dy) in enumerate(((0, -1), (1, -2), (2, -2), (3, -1), (3, 0), (3, 1))):
            c.put(16 + dx, 30 + dy, iron["base"])
            c.put(16 + dx, 31 + dy, iron["deep"])
        # hanging cage lantern
        lx = 20
        c.put(lx, 32, iron["light"])          # hook
        c.rect(lx - 3, 33, 7, 9, iron["base"])
        c.rect(lx - 2, 34, 5, 7, N_dark := palette.NIGHTFELL["deep"])
        c.put(lx - 3, 33, iron["light"])
        c.put(lx + 3, 41, iron["deep"])
        c.put(lx, 43, iron["light"])          # finial drop
        if lit:
            c.rect(lx - 1, 35, 3, 5, G["glow"])
            c.put(lx, 37, G["core"])
        else:
            c.put(lx, 38, G["deep"])          # dead ember
        c.outline(palette.OUTLINE)
        if lit:
            for (mx, my, a) in ((lx - 5, 36, 150), (lx + 5, 38, 150), (lx, 30, 130), (lx - 2, 45, 110)):
                c.put(mx, my, with_alpha(G["glow"], a))
        sheet.paste(c, 0, state * 96)
    sheet.save(path)


# --- the Gloom Shade ----------------------------------------------------------

def _shade_frame(facing, step, swim=False):
    """Hooded fen shade built from round masses: hood, shroud, wispy tail,
    claw arms, hollow face with glowing eyes."""
    c = Canvas(CELL, CELL)
    S = palette.SHADE
    G = palette.GHOSTFLAME
    cx = 32
    drift = step  # sway with the float cycle
    top = 16 + (1 if step != 0 else 0)

    def mass(mx, my, rx, ry):
        c.ellipse(mx + 1, my + 1, rx, ry, S["deep"])
        c.ellipse(mx, my, rx, ry, S["base"])
        c.ellipse(mx - rx * 0.3, my - ry * 0.3, rx * 0.55, ry * 0.5, S["light"])

    # shroud body: tapering stacked masses, tail curls at the bottom
    mass(cx + drift, top + 26, 8, 9)
    mass(cx, top + 16, 10, 9)
    # wispy tail: three curls fading down
    for i, (ox, oy) in enumerate(((-4, 34), (2, 38), (-1, 42))):
        r = 3 - i
        if r > 0:
            c.ellipse(cx + ox + drift * 2, top + oy, r + 1, r, S["deep"])
            c.ellipse(cx + ox + drift * 2 - 1, top + oy - 1, r, r - 0.5 if r > 1 else 1, S["base"])
    # hood: big rounded mass with a folded point
    mass(cx, top + 4, 9, 8)
    for i in range(4):                        # hood point flops to one side
        c.put(cx - 2 + i, top - 4 + (i // 2), S["base"])
        c.put(cx - 2 + i, top - 3 + (i // 2), S["deep"])
    # claw arms
    if facing in ("down", "up"):
        for side in (-1, 1):
            ax = cx + side * 11
            ay = top + 18 + (step * side)
            mass(ax, ay, 3, 4)
            for j in range(3):                # three claw fingers
                c.put(ax + side * 2, ay + 3 + j, S["light"])
                c.put(ax + side * 1, ay + 4 + j, S["deep"])
    else:
        ax = cx + 11
        ay = top + 17 + step
        mass(ax, ay, 3.5, 4)
        for j in range(3):
            c.put(ax + 2, ay + 3 + j, S["light"])
    c.outline(palette.OUTLINE)
    # face: hollow void + glowing eyes (after outline)
    if facing == "down":
        c.ellipse(cx, top + 5, 6, 5, (16, 16, 26))
        c.put(cx - 3, top + 4, S["eye"])
        c.put(cx - 2, top + 4, S["eye"])
        c.put(cx + 2, top + 4, S["eye"])
        c.put(cx + 3, top + 4, S["eye"])
        c.put(cx - 2, top + 3, G["core"])
        c.put(cx + 3, top + 3, G["core"])
        c.put(cx, top + 8, S["eye"])          # faint mouth glow
    elif facing == "right":
        c.ellipse(cx + 4, top + 5, 4, 4.5, (16, 16, 26))
        c.put(cx + 5, top + 4, S["eye"])
        c.put(cx + 6, top + 4, S["eye"])
        c.put(cx + 6, top + 3, G["core"])
    # up: hood back only — no face
    if swim:
        import gen_mobs
        gen_mobs._mist_overlay(c)
    return c


def gen_gloomshade(path):
    from PIL import Image
    steps = {0: 0, 1: 1, 2: 0, 3: -1, 4: 0, 5: 0}
    sheet = Canvas(6 * CELL, 4 * CELL)
    for col in range(6):
        swim = col == 5
        up = _shade_frame("up", steps[col], swim)
        right = _shade_frame("right", steps[col], swim)
        down = _shade_frame("down", steps[col], swim)
        left = right.mirrored()
        for row, sprite in enumerate((up, right, down, left)):
            sheet.paste(sprite, col * CELL, row * CELL)
    sheet.save(path)


def gen_shade_icon(path):
    c = Canvas(32, 32)
    S = palette.SHADE
    G = palette.GHOSTFLAME
    c.ellipse(16, 20, 8, 9, S["base"])
    c.ellipse(16, 11, 7, 6.5, S["base"])
    c.ellipse(13, 9, 4, 3.5, S["light"])
    c.put(15, 4, S["base"])
    c.put(16, 3, S["deep"])
    c.outline(palette.OUTLINE)
    c.ellipse(16, 12, 4.5, 4, (16, 16, 26))
    c.put(14, 11, S["eye"])
    c.put(19, 11, S["eye"])
    c.put(14, 10, G["core"])
    c.save(path)


# --- item icons ---------------------------------------------------------------

def gen_veil_item_icons(items_dir):
    G = palette.GHOSTFLAME
    # veilessence: a curling wisp
    c = Canvas(32, 32)
    S = palette.SHADE
    import math
    for a in range(0, 540, 14):
        r = 3 + a / 60.0
        x = 16 + round(r * math.cos(math.radians(a)))
        y = 17 + round(r * 0.75 * math.sin(math.radians(a)))
        c.put(x, y, S["light"] if a % 42 else G["glow"])
        c.put(x + 1, y, S["base"])
    c.put(16, 17, G["core"])
    c.outline(palette.OUTLINE)
    c.save(f"{items_dir}/veilessence.png")

    # cinderpearl: pearl with green inner fire
    c = Canvas(32, 32)
    B = palette.BONEASH
    c.ellipse(16, 17, 7.5, 7.5, B["base"])
    c.ellipse(14, 15, 4.5, 4.5, B["light"])
    c.ellipse(17, 18, 3.5, 3.5, G["deep"])
    c.put(17, 18, G["glow"])
    c.put(18, 19, G["core"])
    c.put(12, 13, B["hi"])
    c.outline(palette.OUTLINE)
    c.put(24, 11, with_alpha(G["glow"], 150))
    c.save(f"{items_dir}/cinderpearl.png")


def gen_deadtree(path):
    """96x80: 2 variants of a bone-grey crooked dead tree (gloomwillow build,
    Veil colorway, no perched raven)."""
    import gen_furniture
    import palette as _p
    saved = _p.GLOOMWOOD
    _p.GLOOMWOOD = {
        "deep": (44, 38, 46),
        "base": (70, 62, 70),
        "light": (96, 88, 94),
        "hi": (122, 114, 118),
    }
    try:
        gen_furniture.gen_gloomwillow(path, variants=2)
    finally:
        _p.GLOOMWOOD = saved
