"""All Veil (v0.3) sprites: terrain splat materials, murkwater, fen flora,
seance circle, the rift pair, the ghost lantern and the Gloom Shade."""

import math

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
    """N*32 strip: murk reeds arcing out of two uneven root clumps, tips
    dressed in pale whisper-fluff; one reed per variant is snapped over.
    No generic outline pass (vanilla grass construction) — the deep ramp
    edge carries the silhouette and keeps the 1px fluff whiskers alive."""
    W = palette.MURKMOSS
    pale = palette.BONEASH
    sheet = Canvas(variants * 32, 32)
    for v in range(variants):
        c = Canvas(32, 32)
        rng = Rng(0x3EED + v * 313)
        # uneven mud tufts at the roots
        c.ellipse(12 + rng.range(-2, 1), 29, 6, 1.8, W["deep"])
        c.ellipse(21 + rng.range(-1, 2), 30, 5, 1.5, W["deep"])
        c.put(9 + rng.range(0, 2), 28, W["tuft"])
        c.put(23 + rng.range(-1, 1), 29, W["tuft"])
        blades = []
        for (cx0, cnt) in ((rng.range(9, 12), rng.range(2, 3)),
                           (rng.range(19, 22), rng.range(2, 3))):
            for _ in range(cnt):
                blades.append((rng.range(9, 17), cx0 + rng.range(-2, 2), cx0))
        broken = rng.range(0, len(blades) - 1)       # one reed snapped over
        for bi, (h, bx, cx0) in enumerate(sorted(blades)):
            base_y = rng.range(27, 29)
            out = 1 if bx >= cx0 else -1
            lean = out * rng.range(2, 5)
            bend = rng.pick((1.7, 2.0, 2.3))
            x = bx
            for i in range(h):
                t = i / h
                x = bx + round(lean * (t ** bend))
                if bi == broken and i > h - 4:
                    x += out * (i - (h - 4))         # snapped tip flops over
                y = base_y - i
                mid = W["base"] if t < 0.55 else W["tuft"]
                c.put(x - 1, y, W["deep"])
                c.put(x, y, mid)
                if t < 0.5:
                    c.put(x + 1, y, W["deep"])
            ty = base_y - h
            if bi == broken:
                c.put(x + out, ty + 1, pale["base"])  # husk at the snap
            else:
                c.put(x, ty, pale["light"])           # whisper-fluff tip
                c.put(x, ty - 1, pale["hi"])
                c.put(x + out, ty - 1, pale["base"])
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def _shroom(c, cx, ground, r, tilt=0):
    """One gloomshroom: r = cap half-width, dome height ~ r, cap overhangs the
    stem. Shared by the world sheet and the item icon so the thing a player
    picks up is a portrait of the thing he picked it from - which is how every
    vanilla plant icon relates to its object."""
    G = palette.GHOSTFLAME
    S = palette.NIGHTFELL
    P = palette.BLACKPEAT
    stem_h = r + 6
    cap_y = ground - stem_h            # cap skirt line
    # peat mound the stem grows out of
    c.ellipse(cx, ground, r + 1, 2.0, P["base"])
    c.put(cx - 1, ground - 1, P["light"])
    # stem: 3px pale column with a subtle lean
    for i in range(stem_h + 1):
        sx = cx + (tilt if i > stem_h // 2 else 0)
        c.put(sx - 2, ground - i, S["hi"])
        c.put(sx - 1, ground - i, S["light"])
        c.put(sx, ground - i, S["light"])
        c.put(sx + 1, ground - i, S["base"])
        c.put(sx + 2, ground - i, S["base"])
    # cap: stacked dome rows (quarter-ellipse profile), wavy skirt
    H = max(4, round(r * 1.05))
    for j in range(H + 1):
        u = j / H
        half = max(1, round((r + 1) * math.sqrt(max(0.0, 1 - (1 - u) ** 2))))
        y = cap_y - H + j
        row_off = tilt * round(u * 1.2)
        for dx in range(-half, half + 1):
            c.put(cx + row_off + dx, y, S["base"])
    # cap shading: lit upper-left, pale speckles
    c.ellipse(cx - r * 0.3 + tilt, cap_y - H + H * 0.34, r * 0.52, H * 0.4, S["light"])
    c.ellipse(cx - r * 0.42 + tilt, cap_y - H + H * 0.22, r * 0.26, H * 0.2, S["hi"])
    if r >= 5:
        # wavy skirt dips + dome details only fit the big cap
        for k, dx in enumerate(range(-r, r + 1, 3)):
            c.put(cx + tilt + dx + (k % 2), cap_y + 1, S["base"])
        c.put(cx + tilt + r - 3, cap_y - H + 2, S["hi"])
        c.put(cx + tilt - 1, cap_y - 2, S["hi"])
        # glowing gill dashes under the rim (broken, not a dotted line)
        for dx in range(-r, r + 1):
            if (dx + r) % 4 < 2 and abs(dx - tilt) > 1:
                c.put(cx + tilt + dx, cap_y, G["glow"])
        c.put(cx + tilt - r + 2, cap_y - H + 1, G["glow"])
        c.put(cx + tilt + 2, cap_y - H, G["core"])
    else:
        c.put(cx + tilt - 1, cap_y, G["glow"])       # small gill glint
        c.put(cx + tilt + 2, cap_y, G["glow"])
        c.put(cx + tilt, cap_y - H + 1, G["core"])


def gen_gloomshroom(path):
    """64x32: 2 variants of glowing shrooms with vanilla mushroom
    proportions: a BELL-DOME cap (not a disc) with a wavy skirt over a pale
    curved stem, glowing gill dashes under the rim, a tilted companion and a
    tiny button, all rooted in a dark peat mound."""
    sheet = Canvas(64, 32)
    G = palette.GHOSTFLAME
    for v in range(2):
        c = Canvas(32, 32)
        rng = Rng(0x5A00 + v * 977)
        big = rng.range(8, 9)
        if v == 0:
            _shroom(c, 10, 29, big)
            _shroom(c, 23, 30, 6, tilt=-1)         # companion leans toward the big one
            _shroom(c, 28, 31, 3, tilt=1)          # button at the edge of the clump
        else:
            _shroom(c, 21, 29, big)
            _shroom(c, 8, 30, 6, tilt=1)
            _shroom(c, 3, 31, 3, tilt=-1)
        c.outline(palette.OUTLINE)
        # drifting spore motes (after outline so they float)
        c.put(6 + v * 18, 13, with_alpha(G["glow"], 150))
        c.put(26 - v * 12, 17, with_alpha(G["glow"], 130))
        c.put(15 + v * 2, 8, with_alpha(G["glow"], 110))
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_ashbones(path):
    """64x32: 2 variants of half-buried remains rising from an ash drift:
    individually BOWED ribs (each its own arc, heights varied), a vertebrae
    ridge of knobs, a proper little skull on variant 0 and a fallen long
    bone on variant 1. No parallel hatching — every bone is a curved mass."""
    sheet = Canvas(64, 32)
    B = palette.BONEASH
    A = palette.ASHSAND
    for v in range(2):
        c = Canvas(32, 32)
        rng = Rng(0xB0E5 + v * 131)
        cx = 13 + rng.pick((-2, 1))
        ground = 28
        # ash drift: two low overlapping mounds, not one slug-shaped bar
        c.ellipse(cx - 2, ground + 1, 7, 1.8, A["deep"])
        c.ellipse(cx + 8, ground + 1, 6, 1.6, A["deep"])
        c.ellipse(cx - 3, ground, 6, 1.6, A["base"])
        c.ellipse(cx + 7, ground, 5.5, 1.5, A["base"])
        c.put(cx - 3, ground - 1, A["light"])
        c.put(cx + 5, ground - 1, A["light"])
        # ribs: each bows outward on its own arc and tapers to a point
        n_ribs = 3
        for k in range(n_ribs):
            rx0 = cx - 6 + k * 5 + rng.range(-1, 0)
            rh = 10 - k * 2 + rng.range(-1, 1)
            bow = 3 + (k % 2) + rng.range(0, 1)
            x = rx0
            for j in range(rh + 1):
                u = j / rh
                x = rx0 + round(bow * math.sin(u * math.pi * 0.55))
                y = ground - 1 - j
                c.put(x, y, B["base"] if u > 0.15 else B["deep"])
                if u < 0.8:                              # taper: 2px -> 1px
                    c.put(x + 1, y, B["deep"] if u < 0.7 else B["base"])
                if u < 0.5:
                    c.put(x - 1, y, B["light"])
        # vertebrae ridge: knobs with gaps, not a dotted line
        for (vx, vy) in ((cx - 5, ground), (cx - 1, ground + 1), (cx + 4, ground)):
            c.rect(vx, vy - 1, 2, 2, B["base"])
            c.put(vx, vy - 1, B["light"])
            c.put(vx + 1, vy, B["deep"])
        if v == 0:
            # skull: dome + cheek, sockets and nose slit go on after outline
            sx = cx + 11
            c.ellipse(sx, 25, 3.6, 3.2, B["base"])
            c.ellipse(sx - 1, 23.5, 2.6, 1.8, B["light"])
            c.ellipse(sx - 1, 28, 2.2, 1.4, B["base"])   # jaw half-buried
            c.put(sx - 3, 27, B["deep"])
        else:
            # fallen long bone with knobbed ends
            bx = cx + 9
            for i in range(6):
                y = 25 + i // 3
                c.put(bx + i, y - 1, B["light"])
                c.put(bx + i, y, B["base"])
                c.put(bx + i, y + 1, B["deep"])
            for ex, ey in ((bx - 1, 25), (bx + 6, 26)):
                c.ellipse(ex, ey + 0.5, 1.4, 1.4, B["base"])
                c.put(ex, ey, B["light"])
        c.outline(palette.OUTLINE)
        if v == 0:
            sx = cx + 11
            c.rect(sx - 2, 24, 2, 2, (16, 16, 26))       # eye sockets
            c.rect(sx + 1, 24, 2, 2, (16, 16, 26))
            c.put(sx, 26, B["deep"])                     # nose slit
            c.put(sx - 1, 28, B["deep"])                 # tooth gaps
            c.put(sx + 1, 28, B["deep"])
        # ash spilling over the bone roots (buried feel, after outline)
        c.put(cx - 4, ground - 1, A["base"])
        c.put(cx + 2, ground, A["light"])
        c.put(cx + 8, ground - 1, A["base"])
        sheet.paste(c, v * 32, 0)
    sheet.save(path)


def gen_seancecircle(path):
    """32x64 ritual set piece (bottom 32x32 = the tile, upper half stands up).

    Size law: the old version filled 80 opaque px against a vanilla ritual
    altar's 5280 — it was practically invisible in game. This one fills the
    whole tile with a heavy chalk-and-stone ring, four standing candles, a
    rune plate and rising motes."""
    import math
    c = Canvas(32, 64)
    chalk = palette.BONEASH
    G = palette.GHOSTFLAME
    stone = palette.VEILROCK
    cx, cy = 16, 48          # ring center inside the tile cell
    CANDLES = ((5, cy - 1, 16), (27, cy - 1, 16), (11, cy - 8, 21), (21, cy - 8, 21))

    # --- stone ring plate: broad ellipse band filling the tile ---
    c.ellipse(cx, cy + 1, 15.5, 11, stone["deep"])
    c.ellipse(cx, cy, 15, 10, stone["base"])
    c.ellipse(cx, cy - 1, 13, 8.5, stone["light"])
    c.ellipse(cx, cy - 1, 11.5, 7.5, stone["deep"])
    # cobble texture on the band
    for a in range(0, 360, 18):
        r = math.radians(a)
        x = cx + round(12.5 * math.cos(r))
        y = cy - 1 + round(7.5 * math.sin(r))
        c.put(x, y, stone["light"])
        c.put(x, y + 1, stone["deep"])

    # --- inner chalk field + double rune ring ---
    c.ellipse(cx, cy - 1, 11, 7, chalk["deep"])
    c.ellipse(cx, cy - 2, 10, 6, chalk["base"])
    for rad_x, rad_y, tone in ((9.5, 5.5, chalk["hi"]), (6.0, 3.4, chalk["light"])):
        for a in range(0, 360, 7):
            if (a // 7) % 4 == 3:
                continue                     # dashed chalk, not a solid line
            r = math.radians(a)
            c.put(cx + round(rad_x * math.cos(r)), cy - 2 + round(rad_y * math.sin(r)), tone)

    # --- rune glyphs between the rings (bold 2px marks) ---
    for a in (0, 60, 120, 180, 240, 300):
        r = math.radians(a)
        gx = cx + round(7.8 * math.cos(r))
        gy = cy - 2 + round(4.5 * math.sin(r))
        c.rect(gx - 1, gy, 2, 2, chalk["hi"])
        c.put(gx, gy - 1, chalk["light"])

    # --- centre sigil: crescent over a filled disc ---
    c.ellipse(cx, cy - 2, 3.4, 2.2, chalk["hi"])
    for (mx, my) in ((cx - 2, cy - 4), (cx - 1, cy - 5), (cx, cy - 5),
                     (cx + 1, cy - 4), (cx + 2, cy - 3)):
        c.put(mx, my, chalk["light"])

    # --- four standing candles: thick stems rising out of the tile ---
    for (sx, base_y, h) in CANDLES:
        c.rect(sx - 3, base_y - h, 6, h, chalk["base"])          # 6px wax column
        c.rect(sx - 3, base_y - h, 2, h, chalk["light"])         # lit left edge
        c.rect(sx + 2, base_y - h, 1, h, chalk["deep"])          # shaded right edge
        for dy in range(2, h, 4):                                # wax drips
            c.put(sx + 2, base_y - h + dy, chalk["base"])
        c.rect(sx - 4, base_y - 2, 8, 3, stone["light"])         # holder foot
        c.rect(sx - 4, base_y + 1, 8, 1, stone["deep"])

    c.outline(palette.OUTLINE)

    # --- flames + glow AFTER the outline so they stay bright ---
    for (sx, base_y, h) in CANDLES:
        top = base_y - h
        c.rect(sx - 2, top - 4, 4, 4, G["glow"])
        c.rect(sx - 1, top - 6, 2, 2, G["core"])
        c.put(sx - 1, top - 7, with_alpha(G["glow"], 180))
        c.put(sx, top - 7, with_alpha(G["glow"], 180))
    # rune ring pulse + rising motes
    for a in range(0, 360, 24):
        r = math.radians(a)
        c.put(cx + round(9.5 * math.cos(r)), cy - 2 + round(5.5 * math.sin(r)),
              with_alpha(G["glow"], 190))
    for (mx, my, al) in ((cx - 5, cy - 14, 170), (cx + 6, cy - 18, 150), (cx, cy - 22, 130),
                         (cx - 8, cy - 24, 110), (cx + 3, cy - 28, 95)):
        c.put(mx, my, with_alpha(G["core"], al))
    c.ellipse(cx, cy - 2, 4, 2.4, with_alpha(G["glow"], 120))
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

# Ragged hem per float phase (0=idle, 1-4=walk cycle): (x offset from the
# lower-cloak center, length below the root line, sway dir). Lengths and
# positions shift phase to phase so the hem visibly undulates, matching the
# vanilla deepcavespirit's re-posed trailing tendrils.
_SHADE_HEM = (
    ((-7, 5, 0), (-4, 2, 0), (-1, 9, -1), (2, 4, 0), (5, 7, 1), (7, 3, 0)),
    ((-7, 4, -1), (-4, 8, 0), (-1, 5, 0), (2, 10, 1), (5, 3, 0), (7, 6, 1)),
    ((-7, 3, 0), (-3, 10, -1), (0, 5, 0), (3, 3, 0), (6, 8, 1)),
    ((-7, 7, -1), (-4, 3, 0), (-1, 11, 0), (2, 5, 1), (5, 2, 0), (7, 4, 0)),
    ((-7, 6, 0), (-3, 4, 1), (0, 8, -1), (3, 10, 0), (6, 5, 1)),
)

_SHADE_SWAY = (0, 1, 0, -1, 0)
_SHADE_BOB = (0, 1, 0, 1, 0)


def _shade_frame(facing, step, swim=False):
    """Hooded fen shade after the vanilla ragged-hem spirit: a stable hood
    dome, lumpy asymmetric shoulders, a drifting cloak, and a hem of pointed
    tendrils that re-pose every phase. `step` is the float phase 0..4."""
    c = Canvas(CELL, CELL)
    S = palette.SHADE
    G = palette.GHOSTFLAME
    cx = 32
    phase = step % 5
    sway = _SHADE_SWAY[phase]
    top = 16 + _SHADE_BOB[phase]

    def mass(mx, my, rx, ry):
        c.ellipse(mx + 1, my + 1, rx, ry, S["deep"])
        c.ellipse(mx, my, rx, ry, S["base"])
        c.ellipse(mx - rx * 0.3, my - ry * 0.3, rx * 0.55, ry * 0.5, S["light"])

    profile = facing == "right"
    lean = -2 if profile else 0               # profile drifts, hem trails back
    # --- cloak: shoulders + two drifting body masses (asymmetric on purpose)
    if profile:
        mass(cx - 2, top + 13, 7, 6)                      # shoulder hump
        mass(cx + 3, top + 12, 5.5, 5)                    # chest under the face
    else:
        mass(cx - 6, top + 13, 6.5, 5.5)                  # heavy left shoulder
        mass(cx + 5, top + 12, 6, 5)                      # lighter right
    body_cx = cx + sway + lean
    hem_cx = cx + sway * 2 - 1 + lean * 2
    mass(body_cx, top + 20, 9, 7)
    mass(hem_cx, top + 27, 7.5, 6)
    # --- ragged hem: tapering tendrils rooted inside the lower cloak mass
    root = top + 28
    for (tx, ln, tsway) in _SHADE_HEM[phase]:
        x = hem_cx + tx
        for i in range(ln + 4):
            y = root + i - 4                  # first rows weld into the cloak
            f = max(0, i - 4) / max(1, ln - 1)
            w = 4 if i < 4 else (3 if f < 0.45 else (2 if f < 0.8 else 1))
            if tsway != 0 and i > 6 and i % 3 == 0:
                x += tsway
            tone = S["base"] if f < 0.4 else S["deep"]
            for k in range(w):
                c.put(x - w // 2 + k, y, tone)
    # --- hood: stable dome with a floppy point (flops left = asymmetry)
    hood_cx = cx + (2 if profile else 0)
    mass(hood_cx, top + 11, 6.5, 4)          # cowl drape bridging into the body
    c.ellipse(hood_cx + 1, top + 6, 8.5, 7.5, S["deep"])
    c.ellipse(hood_cx, top + 5, 8.5, 7.5, S["base"])
    c.ellipse(hood_cx - 2, top + 2, 4.5, 3.4, S["light"])  # thin top-left sheen
    c.put(hood_cx - 4, top, S["hi"])
    c.put(hood_cx - 3, top - 1, S["hi"])
    for i, (px_, w) in enumerate(((0, 3), (-2, 3), (-4, 2))):
        for k in range(w):
            c.put(hood_cx + px_ - k, top - 3 - i, S["base"] if k < w - 1 else S["deep"])
    # --- cloth detail: V-collar fold + two wavy drape creases
    collar_y = top + 11
    for dx in range(-4, 5):
        c.put(cx + dx + (1 if profile else 0), collar_y + abs(dx) // 2, S["deep"])
    for fx in (-4, 3):
        for y in range(top + 15, top + 29):
            wx = body_cx + fx + round(1.3 * math.sin((y - top) * 0.5 + fx + phase * 0.7))
            c.put(wx, y, S["deep"])
    # --- claw arms, re-posed with the float phase
    raise_l = (0, -1, 0, 1, 0)[phase]
    if profile:
        arms = ((cx + 9, top + 16 + raise_l),)
    else:
        arms = ((cx - 10, top + 17 + raise_l), (cx + 10, top + 17 - raise_l))
    for (ax, ay) in arms:
        mass(ax, ay, 3, 4.5)
        for j in (-2, 0, 2):
            c.put(ax + j, ay + 5, S["base"])
            c.put(ax + j, ay + 6, S["deep"])
    c.outline(palette.OUTLINE)
    # --- claw glints + hollow face with glowing eyes (after the outline)
    for (ax, ay) in arms:
        for j in (-2, 0, 2):
            c.put(ax + j, ay + 5, S["light"])
    if facing == "down":
        c.ellipse(cx, top + 5, 5.5, 4.5, (16, 16, 26))
        for ex in (cx - 3, cx + 3):
            c.put(ex, top + 4, S["eye"])
            c.put(ex, top + 5, S["eye"])
        c.put(cx - 3, top + 3, G["core"])
    elif facing == "right":
        c.ellipse(cx + 4, top + 5, 3, 3.4, (16, 16, 26))
        c.put(cx + 5, top + 4, S["eye"])
        c.put(cx + 5, top + 5, S["eye"])
        c.put(cx + 5, top + 3, G["core"])
    else:  # up: hood back stays a clean dome; a wavy seam runs down the cloak
        for y in range(top + 14, top + 27):
            c.put(cx + round(1.2 * math.sin(y * 0.5 + phase * 0.7)), y, S["deep"])
    if swim:
        import gen_mobs
        gen_mobs._mist_overlay(c)
    return c


def gen_gloomshade(path):
    from PIL import Image
    # float phases: idle, then the 4-frame walk cycle (each phase re-poses
    # the hem tendrils), swim reuses the idle pose under the mist overlay
    steps = {0: 0, 1: 1, 2: 2, 3: 3, 4: 4, 5: 0}
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
    c.ellipse(16, 18, 8, 8, S["base"])
    c.ellipse(16, 11, 7, 6.5, S["base"])
    c.ellipse(13, 9, 4, 3.5, S["light"])
    c.put(15, 4, S["base"])
    c.put(16, 3, S["deep"])
    # ragged hem drips matching the sheet's tendril skirt
    for tx, ln in ((11, 3), (15, 5), (19, 2), (22, 4)):
        for i in range(ln):
            c.put(tx, 25 + i, S["base"] if i < 2 else S["deep"])
            if i < 2:
                c.put(tx + 1, 25 + i, S["deep"])
    c.outline(palette.OUTLINE)
    c.ellipse(16, 12, 4.5, 4, (16, 16, 26))
    c.put(14, 11, S["eye"])
    c.put(19, 11, S["eye"])
    c.put(14, 10, G["core"])
    c.save(path)


# --- item icons ---------------------------------------------------------------

def _veil_finish(c):
    c.outline(palette.OUTLINE)
    return c


def gen_veil_item_icons(items_dir):
    """The Veil's 32x32 inventory icons.

    The four object icons here are not decoration: ObjectRegistry gives every
    registered object an ObjectItem, and GameObject.generateItemTexture (jar
    1.3.2, GameObject.java:767) loads items/<stringID>.png for it. Whisper
    Reeds, Gloomshroom and the dead tree are all obtainable, so
    GameObject.getLootTable (GameObject.java:278) hands the item straight over
    when the player breaks one - and with no file, GameTexture.fromFile returns
    GameResources.error and he is holding a red ERR tile. Veil Rock is a
    RockObject, which resolves items/<rockTexture> (RockObject.java:111), and
    its rockTexture IS "veilrock". Gated by tools/locale_audit.py.

    Masses are measured against the closest vanilla icon of the same kind
    (>= 80% of it, per the size law): items/reeds.png 440, objects/mushroom.png
    424, items/caverock.png 456, items/cactus.png 356.
    """
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

    # whisperreeds: a cut bundle, built like vanilla items/reeds.png - a DENSE
    # upright clump, not the airy two-tuft spread of the world sprite. The dark
    # blade mass goes down first and the lit cores on top of it, because the
    # outline pass eats 1px diagonals drawn the other way round.
    c = Canvas(32, 32)
    W = palette.MURKMOSS
    pale = palette.BONEASH
    rng = Rng(0x3EE1)
    blades = [(5 + k * 2 + rng.range(0, 1), rng.range(15, 23),
               rng.pick((-3, -2, -1, 1, 2, 3)), rng.range(27, 29))
              for k in range(11)]
    for bx, h, lean, base_y in blades:
        for i in range(h):
            x = bx + round(lean * ((i / h) ** 2))
            for dx in (-1, 0, 1):
                c.put(x + dx, base_y - i, W["deep"])
    for bx, h, lean, base_y in blades:
        for i in range(h):
            t = i / h
            x = bx + round(lean * (t ** 2))
            tone = W["base"] if t < 0.42 else (W["light"] if t < 0.72 else W["tuft"])
            c.put(x, base_y - i, tone)
            if t < 0.6:
                c.put(x + 1, base_y - i, W["base"])
            elif t > 0.8:
                c.put(x, base_y - i, W["hi"] if (i + bx) % 5 == 0 else tone)
    for bx, h, lean, base_y in blades[1::3]:            # whisper-fluff tips
        x = bx + round(lean)
        c.put(x, base_y - h, pale["light"])
        c.put(x, base_y - h - 1, pale["hi"])
        c.put(x + 1, base_y - h, pale["base"])
    c.ellipse(15, 28, 8, 2.0, W["deep"])                # root mud at the cut
    c.ellipse(13, 27, 4, 1.4, palette.BLACKPEAT["base"])
    _veil_finish(c).save(f"{items_dir}/whisperreeds.png")

    # gloomshroom: one grown cap plus a companion, drawn with the world
    # sprite's own _shroom so the picked item is a portrait of the plant.
    c = Canvas(32, 32)
    _shroom(c, 13, 28, 9)
    _shroom(c, 23, 30, 5, tilt=1)
    _veil_finish(c)
    c.put(6, 11, with_alpha(G["glow"], 150))            # spores, after outline
    c.put(27, 16, with_alpha(G["glow"], 120))
    c.save(f"{items_dir}/gloomshroom.png")

    # veilrock: a broken chunk on vanilla items/caverock.png's build - one
    # rounded mass, flat-ish bottom, lit top-left face, two facet lines and
    # grit speckles.
    c = Canvas(32, 32)
    R = palette.VEILROCK
    rng = Rng(0x7E11)
    c.blob(16, 18, 11, R["base"], rng, lumps=5)
    c.ellipse(16, 23, 12, 4.8, R["base"])
    c.ellipse(13, 15, 7.0, 4.0, R["light"])
    c.ellipse(11, 13, 3.5, 2.5, R["hi"])
    for dx in range(0, 14):                             # main crevice, 2px deep
        c.put(7 + dx, 19 + dx // 3, R["deep"])
        c.put(7 + dx, 20 + dx // 3, R["deep"])
        c.put(7 + dx, 18 + dx // 3, R["light"])         # lit lip above it
    c.line(20, 10, 25, 20, R["deep"])
    for dy in range(0, 3):                              # chipped upper corner
        for dx in range(0, 3 - dy):
            c.put(24 + dx, 11 + dy, (0, 0, 0, 0))
    for _ in range(13):
        c.put(rng.range(7, 25), rng.range(11, 26), R["deep"])
    for _ in range(6):
        c.put(rng.range(8, 22), rng.range(11, 19), R["hi"])
    _veil_finish(c).save(f"{items_dir}/veilrock.png")

    # deadtree: a stout crooked trunk with a bare fork, cut from the same
    # bone-grey ramp as the world tree.
    c = Canvas(32, 32)
    D = DEADWOOD
    def trunk_x(i):
        return 15 + round(2.2 * (i / 23.0) ** 2)        # leans right as it rises

    def trunk_half(i):
        return 3 if i < 8 else (2 if i < 17 else 1)

    for i in range(24):                                 # trunk, tapering + lean
        y = 29 - i
        x, half = trunk_x(i), trunk_half(i)
        for dx in range(-half, half + 1):
            c.put(x + dx, y, D["base"])
        c.put(x - half, y, D["light"])
        if half > 1:
            c.put(x - half + 1, y, D["light"] if y % 3 else D["hi"])
        c.put(x + half, y, D["deep"])
    for dx in range(-6, 7):                             # root flare
        top = 29 - abs(dx) // 3
        for dy in range(0, max(1, 2 - abs(dx) // 4)):
            c.put(15 + dx, top + dy, D["base"])
        c.put(15 + dx, top + max(1, 2 - abs(dx) // 4), D["deep"])
        if dx < 0:
            c.put(15 + dx, top, D["light"])
    # Every arm starts ON the trunk edge at its own height: hard-coded start
    # columns left one branch floating free of the tree at 1x.
    for (by, sx, n) in ((23, -1, 8), (18, 1, 7), (13, -1, 7), (10, 1, 5),
                        (27, -1, 5), (25, 1, 6), (8, -1, 4)):
        i = 29 - by
        x, y = trunk_x(i) + sx * trunk_half(i), by
        for i in range(n):
            x += sx
            if i % 2:
                y -= 1
            c.put(x, y, D["base"])
            c.put(x, y - 1, D["light"] if i % 2 else D["base"])
            c.put(x, y - 2, D["hi"] if i % 3 == 1 else D["light"])
            if i < n - 2:
                c.put(x, y + 1, D["deep"])
        c.put(x + sx, y - 2, D["deep"])                 # twig tip
    for (kx, ky) in ((15, 26), (16, 22), (15, 18), (17, 14), (16, 11)):
        c.put(kx, ky, D["deep"])                        # bark cracks
        c.put(kx - 1, ky - 1, D["hi"])
    _veil_finish(c).save(f"{items_dir}/deadtree.png")


# The Veil's dead wood: the Gloomwillow build in a bone-grey colorway. Named
# here rather than inlined because the item icon has to be cut from the same
# ramp as the tree it comes off.
DEADWOOD = {
    "deep": (44, 38, 46),
    "base": (70, 62, 70),
    "light": (96, 88, 94),
    "hi": (122, 114, 118),
}


def gen_deadtree(path):
    """96x80: 2 variants of a bone-grey crooked dead tree (gloomwillow build,
    Veil colorway, no perched raven)."""
    import gen_furniture
    import palette as _p
    saved = _p.GLOOMWOOD
    _p.GLOOMWOOD = DEADWOOD
    try:
        gen_furniture.gen_gloomwillow(path, variants=2)
    finally:
        _p.GLOOMWOOD = saved
