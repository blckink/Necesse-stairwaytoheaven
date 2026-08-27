"""Ambient critter sheets: Cloudlamb, Glowmoth, Sparkbeetle.

Standard 32px critter layout: 6 cols (idle, walk x4, in-liquid) x 4 rows
(Up, Right, Down, Left).
"""

from px import Canvas, Rng, with_alpha
import palette

COLS = 6


def _mist_clip(c, water_y):
    mist = palette.MISTSEA
    for x in range(32):
        for y in range(water_y, 32):
            if c.filled(x, y):
                c.put(x, y, (0, 0, 0, 0))
    c.ellipse(16, water_y, 9, 2.4, with_alpha(mist["hi"], 230))
    c.ellipse(16, water_y + 2, 7, 1.8, with_alpha(mist["light"], 200))


def _moth_frame(facing, col):
    c = Canvas(32, 32)
    M = palette.MOTH
    flap = col in (1, 3)
    cx, cy = 16, 18
    # wings: two rounded triangles; raised (narrow+tall) on flap frames
    for side in (-1, 1):
        if flap:
            c.ellipse(cx + side * 4, cy - 6, 3.4, 6.5, M["wing"])
            c.ellipse(cx + side * 4, cy - 4, 2.2, 4, M["wing_shade"])
            c.put(cx + side * 4, cy - 8, M["spot"])
        else:
            c.ellipse(cx + side * 7, cy - 2, 6, 4.2, M["wing"])
            c.ellipse(cx + side * 8, cy - 1, 3.6, 2.6, M["wing_shade"])
            c.put(cx + side * 8, cy - 3, M["spot"])
            c.put(cx + side * 9, cy - 2, M["spot"])
    # body + antennae
    c.rect(cx - 1, cy - 4, 2, 9, M["body"])
    c.put(cx - 2, cy - 6, M["body"])
    c.put(cx + 1, cy - 6, M["body"])
    c.put(cx - 3, cy - 7, M["wing_shade"])
    c.put(cx + 2, cy - 7, M["wing_shade"])
    c.outline(palette.OUTLINE)
    # faint glow dots (after outline, alpha so they float)
    c.put(cx - 6, cy + 4, with_alpha(M["spot"], 140))
    c.put(cx + 6, cy + 3, with_alpha(M["spot"], 140))
    if col == 5:
        _mist_clip(c, 22)
    return c


def _beetle_frame(facing, col):
    c = Canvas(32, 32)
    B = palette.BEETLE
    step = {0: 0, 1: 1, 2: 0, 3: -1, 4: 0, 5: 0}[col]
    cx, ground = 16, 25
    # legs
    for i, lx in enumerate((-6, -1, 4)):
        off = step if i % 2 == 0 else -step
        c.put(cx + lx, ground + (1 if off > 0 else 0), B["shell_deep"])
        c.put(cx + lx + 1, ground + 1, B["shell_deep"])
    # dome shell with seam + charge zigzag
    c.ellipse(cx, ground - 5, 6.5, 4.8, B["shell"])
    c.ellipse(cx - 2, ground - 7, 4, 2.6, B["shell_light"])
    for y in range(ground - 9, ground - 1):
        c.put(cx, y, B["shell_deep"])                          # wing seam
    zig = ((cx - 4, ground - 6), (cx - 3, ground - 5), (cx - 2, ground - 6))
    for (zx, zy) in zig:
        c.put(zx, zy, B["charge"])
    # head + antennae at the front
    hx = cx + (7 if facing == "right" else 0)
    c.ellipse(hx, ground - 9 if facing != "right" else ground - 6, 2, 1.8, B["shell_deep"])
    c.put(hx - 2, ground - 12 if facing != "right" else ground - 9, B["shell_deep"])
    c.put(hx + 2, ground - 12 if facing != "right" else ground - 9, B["shell_deep"])
    c.outline(palette.OUTLINE)
    if col in (1, 3):                                          # charge blink
        c.put(cx + 3, ground - 11, with_alpha(B["charge"], 200))
    if col == 5:
        _mist_clip(c, 21)
    return c


def _gen_sheet(path, frame_fn):
    from PIL import Image as _I
    sheet = Canvas(COLS * 32, 4 * 32)
    for col in range(COLS):
        up = frame_fn("up", col)
        right = frame_fn("right", col)
        down = frame_fn("down", col)
        left = right.mirrored()
        for row, sprite in enumerate((up, right, down, left)):
            sheet.paste(sprite, col * 32, row * 32)
    sheet.save(path)


def gen_critters(mob_dir):
    _gen_sheet(f"{mob_dir}/glowmoth.png", _moth_frame)
    _gen_sheet(f"{mob_dir}/sparkbeetle.png", _beetle_frame)
    gen_cloudlamb(f"{mob_dir}/cloudlamb.png", sheared=False)
    gen_cloudlamb(f"{mob_dir}/cloudlamb_sheared.png", sheared=True)


def gen_critter_icons(icon_dir):
    # lamb: fluffy front chibi
    c = Canvas(32, 32)
    L = palette.LAMB
    c.ellipse(16, 20, 9, 7, L["wool"])
    for bx in (-6, -2, 2, 6):
        c.ellipse(16 + bx, 13, 2.6, 2.2, L["wool"])
    c.ellipse(16, 17, 4, 4.4, L["face"])
    c.put(11, 13, L["face_dark"])
    c.put(21, 13, L["face_dark"])
    c.ellipse(16, 12, 3, 1.6, L["wool"])
    c.outline(palette.OUTLINE)
    c.put(14, 17, L["face_dark"])
    c.put(18, 17, L["face_dark"])
    c.save(f"{icon_dir}/cloudlamb.png")

    # moth: spread wings
    c = Canvas(32, 32)
    M = palette.MOTH
    for side in (-1, 1):
        c.ellipse(16 + side * 7, 15, 6.5, 5.5, M["wing"])
        c.ellipse(16 + side * 8, 16, 4, 3.4, M["wing_shade"])
        c.put(16 + side * 8, 13, M["spot"])
        c.put(16 + side * 9, 14, M["spot"])
    c.rect(15, 11, 2, 11, M["body"])
    c.put(13, 8, M["body"])
    c.put(18, 8, M["body"])
    c.outline(palette.OUTLINE)
    c.save(f"{icon_dir}/glowmoth.png")

    # beetle: top view
    c = Canvas(32, 32)
    B = palette.BEETLE
    c.ellipse(16, 18, 8, 9, B["shell"])
    c.ellipse(14, 15, 5, 5, B["shell_light"])
    for y in range(10, 27):
        c.put(16, y, B["shell_deep"])
    c.ellipse(16, 8, 3, 2.4, B["shell_deep"])
    c.put(13, 5, B["shell_deep"])
    c.put(19, 5, B["shell_deep"])
    for (zx, zy) in ((11, 16), (12, 17), (11, 18), (12, 19)):
        c.put(zx, zy, B["charge"])
    c.outline(palette.OUTLINE)
    c.save(f"{icon_dir}/sparkbeetle.png")


# --- the Cloudlamb: REAL livestock at vanilla sheep scale ---------------------
# Sheet mirrors vanilla sheep.png exactly: 6 cols (idle, walk x4, swim) x
# 4 facing rows (Up, Right, Down, Left) of 64px cells, plus a 5th row of five
# 32px fleece chunks for the death particles.

def _puff_body(c, cx, cy, w, h, wool, wool_shade, rng, lumpy=True):
    c.ellipse(cx + 1, cy + 1, w, h, wool_shade)
    c.ellipse(cx, cy, w, h, wool)
    if lumpy:
        for i in range(7):
            a = i / 7.0 * 6.283
            import math
            bx = cx + math.cos(a) * (w - 1)
            by = cy + math.sin(a) * (h - 1)
            r = rng.range(2, 3)
            c.ellipse(bx, by, r + 1, r, wool_shade)
            c.ellipse(bx, by - 1, r, r - 0.5 if r > 1 else 1, wool)
    c.ellipse(cx - w * 0.3, cy - h * 0.4, w * 0.5, h * 0.45, (246, 249, 252))


def _lamb64_frame(facing, col, sheared):
    from px import Canvas, Rng
    import palette
    L = palette.LAMB
    wool = L["wool"]
    shade = L["wool_shade"]
    c = Canvas(64, 64)
    rng = Rng(0x1A3B + col * 7 + (13 if facing == "right" else 0))
    step = {0: 0, 1: 1, 2: 0, 3: -1, 4: 0, 5: 0}[col]
    cx, ground = 32, 50
    bob = 1 if step != 0 else 0

    # legs (behind the body)
    if facing in ("down", "up"):
        legs = ((-10, step), (-4, -step), (4, step), (10, -step))
    else:
        legs = ((-11, step), (-5, -step), (5, step), (11, -step))
    for lx, off in legs:
        lift = 1 if off > 0 else 0
        c.rect(cx + lx - 1, ground - 7 - lift, 3, 8, L["face_dark"])
        c.rect(cx + lx - 1, ground - 1 - lift, 3, 2, L["face"])

    body_w, body_h = (15, 11) if not sheared else (12, 9)
    if facing == "right":
        _puff_body(c, cx - 1, ground - 13 - bob, body_w + 1, body_h, wool, shade, rng, lumpy=not sheared)
        # head on the right, slightly low
        hx, hy = cx + 14, ground - 18 - bob
        c.ellipse(hx, hy, 5, 5.5, L["face"])
        c.put(hx - 2, hy - 6, L["face_dark"])   # ear back
        c.put(hx - 3, hy - 5, L["face_dark"])
        c.ellipse(hx - 2, hy - 4, 4, 2.5, wool)  # wool cap
        tail_x = cx - body_w - 1
        c.ellipse(tail_x, ground - 16 - bob, 2.5, 2, wool)
    elif facing == "down":
        _puff_body(c, cx, ground - 13 - bob, body_w, body_h, wool, shade, rng, lumpy=not sheared)
        hx, hy = cx, ground - 22 - bob
        c.ellipse(hx, hy, 5, 5.5, L["face"])
        for side in (-1, 1):                     # droopy ears
            c.put(hx + side * 5, hy - 2, L["face_dark"])
            c.put(hx + side * 6, hy - 1, L["face_dark"])
        c.ellipse(hx, hy - 4, 4.5, 2.5, wool)    # wool crown
    else:  # up
        _puff_body(c, cx, ground - 13 - bob, body_w, body_h, wool, shade, rng, lumpy=not sheared)
        hx, hy = cx, ground - 22 - bob
        c.ellipse(hx, hy, 5, 5, wool)            # wool from behind
        for side in (-1, 1):
            c.put(hx + side * 5, hy - 1, L["face_dark"])
        c.ellipse(cx, ground - 6 - bob, 2.5, 2, wool)  # tail
    c.outline(palette.OUTLINE)
    # face after outline
    if facing == "down":
        c.put(hx - 2, hy, L["face_dark"])
        c.put(hx + 2, hy, L["face_dark"])
        c.put(hx, hy + 2, (222, 150, 150))       # pink nose
    elif facing == "right":
        c.put(hx + 2, hy - 1, L["face_dark"])
        c.put(hx + 4, hy + 1, (222, 150, 150))
    if col == 5:
        import gen_mobs
        gen_mobs._mist_overlay(c)
    return c


def gen_cloudlamb(path, sheared):
    from px import Canvas, Rng
    import palette
    sheet = Canvas(6 * 64, 5 * 64)
    for col in range(6):
        up = _lamb64_frame("up", col, sheared)
        right = _lamb64_frame("right", col, sheared)
        down = _lamb64_frame("down", col, sheared)
        left = right.mirrored()
        for row, sprite in enumerate((up, right, down, left)):
            sheet.paste(sprite, col * 64, row * 64)
    # 5th row: five 32px fleece chunks for death particles
    L = palette.LAMB
    rng = Rng(0xF1EE)
    for i in range(5):
        chunk = Canvas(32, 32)
        chunk.ellipse(16, 16, 5 + rng.range(0, 3), 4 + rng.range(0, 2), L["wool"])
        chunk.ellipse(14, 14, 3, 2.5, (246, 249, 252))
        chunk.ellipse(18, 18, 2.5, 2, L["wool_shade"])
        chunk.outline(palette.OUTLINE)
        sheet.paste(chunk, i * 64 + 16, 4 * 64 + 16)
    sheet.save(path)


# --- v0.4 "The Living Sky" fauna: Zephyr Finch + Dew Snail --------------------
# Same 32px critter layout as the glowmoth/sparkbeetle above: 6 cols
# (idle, walk x4, in-liquid) x 4 rows (Up, Right, Down, Left).


def _finch_frame(facing, col):
    """Zephyr Finch: tiny darting song-bird. Blue-gray back and cap, pale
    belly, gold beak/feet. Walk frames are a dart cycle: the wings flick open
    on cols 1/3 while the body hops; col 4 settles into a little head-bob."""
    c = Canvas(32, 32)
    F = palette.FINCH
    flick = col in (1, 3)
    hop = 1 if flick else 0
    peck = 1 if col == 4 else 0
    cx, ground = 16, 26

    if facing == "right":
        by = ground - 7 - hop
        hy = by - 4 + peck                       # head center y
        # tail: pointed, behind; flicks up on the settle frame between darts
        tf = 1 if col == 2 else 0
        for i in range(4):
            x = cx - 6 - i
            y = by - 2 - (i // 2) - tf
            c.put(x, y, F["deep"])
            c.put(x, y + 1, F["deep"])
            if i < 2:
                c.put(x, y + 2, F["base"])
        # body + head masses, belly patch, back sheen
        c.ellipse(cx - 1, by, 5.4, 4.2, F["base"])
        c.ellipse(cx + 4, hy, 3.2, 3.0, F["base"])
        c.ellipse(cx + 1, by + 1.5, 3.6, 2.4, F["belly"])
        c.ellipse(cx - 2, by - 2, 3.0, 1.7, F["light"])
        c.put(cx + 3, hy + 2, F["belly"])        # throat
        c.put(cx + 4, hy + 2, F["belly"])
        # dark cap over the head
        for dx in range(-2, 3):
            c.put(cx + 4 + dx, hy - 3 + (1 if abs(dx) == 2 else 0), F["deep"])
        c.put(cx + 3, hy - 2, F["deep"])
        # wing
        if flick:
            for i in range(6):                   # flicked open: swept up-back
                x = cx - i
                y = by - 3 - i // 2
                c.put(x, y, F["base"])
                c.put(x, y + 1, F["deep"])
                if i in (3, 5):
                    c.put(x, y + 2, F["deep"])   # feather fingers
        else:
            c.ellipse(cx - 1, by - 1, 3.2, 2.2, F["deep"])
            c.put(cx - 3, by - 1, F["light"])    # folded feather edges
            c.put(cx - 2, by, F["light"])
            c.put(cx - 1, by + 1, F["light"])
        # beak silhouette mass (gold core repainted after the outline)
        c.put(cx + 7, hy, F["base"])
        c.put(cx + 8, hy, F["base"])
        c.put(cx + 7, hy + 1, F["base"])
        # legs
        if flick:
            c.put(cx, by + 4, F["deep"])         # tucked feet nub
            c.put(cx + 1, by + 4, F["deep"])
        else:
            for lx in (cx - 1, cx + 2):
                c.put(lx, ground - 1, F["deep"])
                c.put(lx, ground, F["deep"])
    elif facing == "down":
        by = ground - 7 - hop
        hy = by - 5 + peck
        c.ellipse(cx, by, 4.6, 4.8, F["base"])   # body
        c.ellipse(cx, hy, 3.4, 3.0, F["base"])   # head
        c.ellipse(cx, by + 2, 3.0, 2.4, F["belly"])
        c.put(cx - 3, by, F["belly"])
        c.put(cx + 3, by, F["belly"])
        for side in (-1, 1):                     # wings
            if flick:
                for i in range(5):
                    x = cx + side * (4 + i)
                    y = by - 1 - i // 2
                    c.put(x, y, F["base"])
                    c.put(x, y + 1, F["deep"])
                    if i in (2, 4):
                        c.put(x, y + 2, F["deep"])
            else:
                c.ellipse(cx + side * 4, by, 1.7, 3.0, F["deep"])
                c.put(cx + side * 4, by - 2, F["light"])
        for dx in range(-2, 3):                  # cap
            c.put(cx + dx, hy - 3 + (1 if abs(dx) == 2 else 0), F["deep"])
        c.put(cx - 1, hy - 3, F["light"])
        # beak nub (center of the face)
        c.put(cx, hy + 1, F["base"])
        c.put(cx, hy + 2, F["base"])
        if not flick:
            for lx in (cx - 2, cx + 1):
                c.put(lx, ground - 1, F["deep"])
                c.put(lx, ground, F["deep"])
    else:  # up
        by = ground - 7 - hop
        hy = by - 5
        c.ellipse(cx, by, 4.6, 4.8, F["base"])
        c.ellipse(cx, hy, 3.4, 3.0, F["base"])
        for dx in range(-2, 3):                  # cap on the back of the head
            c.put(cx + dx, hy - 2 + (1 if abs(dx) == 2 else 0), F["deep"])
            if abs(dx) <= 1:
                c.put(cx + dx, hy - 1, F["deep"])
        c.put(cx - 1, hy - 2, F["light"])        # cap sheen
        if flick:
            for side in (-1, 1):                 # wings out mid-flick
                for i in range(5):
                    x = cx + side * (4 + i)
                    y = by - 1 - i // 2
                    c.put(x, y, F["base"])
                    c.put(x, y + 1, F["deep"])
                    if i in (2, 4):
                        c.put(x, y + 2, F["deep"])
        else:
            for side in (-1, 1):                 # folded wings hugging the sides
                c.ellipse(cx + side * 3, by + 1, 2.0, 3.6, F["deep"])
                c.put(cx + side * 4, by - 1, F["deep"])
                c.put(cx + side * 2, by + 4, F["deep"])   # tips crossing low
            c.put(cx - 1, by - 2, F["light"])    # small shoulder sheen
            c.put(cx - 2, by - 1, F["light"])
            c.put(cx - 1, by - 1, F["light"])
        # pointed tail toward the viewer
        c.rect(cx - 1, by + 4, 3, 2, F["deep"])
        c.put(cx, by + 6, F["deep"])
        c.put(cx + 1, by + 6, F["deep"])
        c.put(cx, by + 7, F["deep"])
        c.put(cx, by + 4, F["base"])
        c.put(cx, by + 5, F["base"])
        if not flick:
            for lx in (cx - 3, cx + 2):
                c.put(lx, ground, F["deep"])
    c.outline(palette.OUTLINE)
    # face, beak and feet after the outline pass
    if facing == "right":
        by = ground - 7 - hop
        hy = by - 4 + peck
        c.put(cx + 7, hy, F["beak"])
        c.put(cx + 8, hy, F["beak"])
        c.put(cx + 7, hy + 1, F["beak"])
        c.put(cx + 5, hy - 1, palette.OUTLINE)   # eye
        c.put(cx + 4, hy, F["belly"])            # cheek
        if not flick:
            for lx in (cx - 1, cx + 2):
                c.put(lx, ground, F["beak"])     # gold feet
    elif facing == "down":
        by = ground - 7 - hop
        hy = by - 5 + peck
        c.put(cx - 2, hy, palette.OUTLINE)
        c.put(cx + 2, hy, palette.OUTLINE)
        c.put(cx, hy + 1, F["beak"])
        c.put(cx, hy + 2, F["beak"])
        if not flick:
            for lx in (cx - 2, cx + 1):
                c.put(lx, ground, F["beak"])
    if col == 5:
        _mist_clip(c, 21)
    return c


def _snail_frame(facing, col):
    """Dew Snail: slow glowing shoal snail. Teal foot with glow dots along the
    rim, sandy spiral shell, glowing eye-stalk tips. Walk cols are a gentle
    inchworm: col 1 gathers into a hump, col 3 stretches out."""
    import math
    c = Canvas(32, 32)
    S = palette.DEWSNAIL
    gather = 2 if col == 1 else 0
    stretch = 2 if col == 3 else 0
    sway = {0: 0, 1: 1, 2: 0, 3: -1, 4: 0, 5: 0}[col]
    cx, ground = 16, 26
    glow_pts = []                                 # painted after the outline
    tips = []

    def spiral(sx, sy, r0, squish=0.92):
        for t in range(26):
            a = 2.6 + t * 0.42
            rr = r0 - t * 0.155
            c.put(round(sx + math.cos(a) * rr), round(sy + math.sin(a) * rr * squish),
                  S["shell_deep"])

    shafts = []                                   # stalk cores, repainted after
    if facing == "right":
        foot_l = cx - 8 + gather - stretch
        foot_r = cx + 11
        fx = (foot_l + foot_r) / 2.0
        c.ellipse(fx, ground - 1.5, (foot_r - foot_l) / 2.0, 2.5, S["base"])
        if gather:
            c.ellipse(fx - 2, ground - 3, 3.5, 2.0, S["base"])     # hump
        for x in range(foot_l + 1, foot_r - 1):                    # lit top edge
            c.put(x, ground - 3 - (1 if gather and abs(x - fx + 2) < 3 else 0), S["light"])
        for x in range(foot_l + 3, foot_r - 2, 3):                 # muscle ripples
            c.put(x, ground - 1, S["deep"])
        # neck + head knob clearly in front of the shell
        c.ellipse(cx + 7, ground - 4, 2.6, 3.0, S["base"])
        c.ellipse(cx + 8, ground - 7, 2.4, 2.4, S["base"])
        c.put(cx + 7, ground - 9, S["light"])
        c.put(cx + 6, ground - 7, S["light"])
        c.put(cx + 9, ground - 4, S["deep"])                       # chin shade
        # eye stalks: 2px masses leaning with the frame
        for sx, ln in ((cx + 6, 3), (cx + 9, 2)):
            for i in range(ln):
                x = sx + (i * sway) // 2
                c.put(x, ground - 9 - i, S["base"])
                c.put(x + 1, ground - 9 - i, S["base"])
                shafts.append((x, ground - 9 - i))
            tips.append((sx + ((ln - 1) * sway) // 2, ground - 10 - ln))
        # shell riding the back, well clear of the head
        shx, shy = cx - 4, ground - 10
        c.ellipse(shx + 1, shy + 1, 5.4, 5.0, S["shell_deep"])
        c.ellipse(shx, shy, 5.2, 4.9, S["shell"])
        spiral(shx, shy, 4.6)
        c.put(shx - 2, shy - 4, S["glow"])       # one dew glint, top-left
        for x in (foot_l + 3, foot_l + 7, foot_r - 4):
            glow_pts.append((x, ground - 1))
    elif facing == "down":
        # shell behind/above, head sliding toward the camera
        shx, shy = cx, ground - 11
        c.ellipse(shx + 1, shy + 1, 5.6, 5.2, S["shell_deep"])
        c.ellipse(shx, shy, 5.4, 5.0, S["shell"])
        spiral(shx, shy, 4.7)
        c.put(shx - 2, shy - 4, S["glow"])
        # wide foot below, head knob centered on it
        c.ellipse(cx, ground - 2, 5.4 + stretch * 0.5 - gather * 0.5, 2.4, S["base"])
        for x in range(cx - 4, cx + 5):
            c.put(x, ground - 4, S["light"])
        c.ellipse(cx, ground - 6, 2.8, 2.8, S["base"])
        c.put(cx - 1, ground - 8, S["light"])
        c.put(cx - 2, ground - 6, S["light"])
        for x in (cx - 3, cx + 2):
            c.put(x, ground - 1, S["deep"])
        # stalks rise in a V above the head
        for side in (-1, 1):
            sx = cx + side * 2
            for i in range(3):
                x = sx + side * (i // 2) + (i * sway) // 2
                c.put(x, ground - 9 - i, S["base"])
                c.put(x + 1, ground - 9 - i, S["base"])
                shafts.append((x, ground - 9 - i))
            tips.append((sx + side + (2 * sway) // 2, ground - 12))
        glow_pts += [(cx - 4, ground - 1), (cx, ground - 1), (cx + 4, ground - 1)]
    else:  # up: the shell dominates, stalk tips peek over the top
        c.ellipse(cx, ground - 1, 5.2 + stretch * 0.5, 2.2, S["base"])
        for x in range(cx - 4, cx + 5, 2):
            c.put(x, ground, S["deep"])
        shx, shy = cx, ground - 8
        c.ellipse(shx + 1, shy + 1, 5.8, 5.4, S["shell_deep"])
        c.ellipse(shx, shy, 5.6, 5.2, S["shell"])
        spiral(shx, shy, 4.9)
        c.put(shx - 2, shy - 4, S["glow"])
        for sx in (cx - 3, cx + 2):              # stalks emerging behind the shell
            for i in range(3):
                x = sx + (i * sway) // 2
                c.put(x, ground - 13 - i, S["base"])
                c.put(x + 1, ground - 13 - i, S["base"])
                if i > 0:
                    shafts.append((x, ground - 13 - i))
            tips.append((sx + (2 * sway) // 2, ground - 16))
        glow_pts += [(cx - 4, ground - 1), (cx + 4, ground - 1)]
    c.outline(palette.OUTLINE)
    # after the outline: stalk cores, glowing tips, dew dots on the foot
    for (px_, py_) in shafts:
        c.put(px_, py_, S["base"])
    for (tx, ty) in tips:
        c.put(tx, ty, S["glow"])
        c.put(tx + 1, ty, S["glow"])
        c.put(tx, ty - 1, with_alpha(S["glow"], 130))
    for pt in glow_pts:
        c.put(pt[0], pt[1], S["glow"])
    if col == 5:
        _mist_clip(c, 21)
    return c


def gen_zephyrfinch(path):
    _gen_sheet(path, _finch_frame)


def gen_dewsnail(path):
    _gen_sheet(path, _snail_frame)


def gen_critters_v04(mob_dir):
    gen_zephyrfinch(f"{mob_dir}/zephyrfinch.png")
    gen_dewsnail(f"{mob_dir}/dewsnail.png")


def gen_critter_icons_v04(icon_dir):
    import math
    # finch: perched side profile, gold beak forward
    c = Canvas(32, 32)
    F = palette.FINCH
    for i in range(6):                            # tail
        x = 8 - i
        y = 15 - i // 2
        c.put(x, y, F["deep"])
        c.put(x, y + 1, F["deep"])
        if i < 3:
            c.put(x, y + 2, F["base"])
    c.ellipse(14, 17, 7.0, 5.4, F["base"])        # body
    c.ellipse(21, 11, 4.4, 4.0, F["base"])        # head
    c.ellipse(16, 19, 4.6, 3.2, F["belly"])       # belly
    c.put(19, 15, F["belly"])
    c.put(20, 15, F["belly"])
    c.ellipse(12, 14, 4.0, 2.2, F["light"])       # back sheen
    for dx in range(-3, 4):                       # cap
        c.put(21 + dx, 7 + (1 if abs(dx) >= 2 else 0), F["deep"])
    c.ellipse(13, 17, 4.2, 2.8, F["deep"])        # folded wing
    c.put(10, 16, F["light"])
    c.put(12, 18, F["light"])
    c.put(14, 19, F["light"])
    for i in range(3):                            # beak mass
        c.put(25 + i, 11, F["base"])
        if i < 2:
            c.put(25 + i, 12, F["base"])
    for lx in (13, 17):                           # legs
        c.put(lx, 23, F["deep"])
        c.put(lx, 24, F["deep"])
    c.outline(palette.OUTLINE)
    c.put(25, 11, F["beak"])
    c.put(26, 11, F["beak"])
    c.put(27, 11, F["beak"])
    c.put(25, 12, F["beak"])
    c.put(22, 10, palette.OUTLINE)                # eye
    c.put(21, 11, F["belly"])                     # cheek
    for lx in (13, 17):
        c.put(lx, 24, F["beak"])
    c.save(f"{icon_dir}/zephyrfinch.png")

    # dew snail: side profile, big spiral shell, glow dots
    c = Canvas(32, 32)
    S = palette.DEWSNAIL
    c.ellipse(15, 24, 9.0, 3.0, S["base"])        # foot
    for x in range(8, 20):
        c.put(x, 21, S["light"])
    for x in range(9, 22, 3):
        c.put(x, 25, S["deep"])
    c.ellipse(23, 19, 3.2, 4.0, S["base"])        # neck
    c.ellipse(24, 15, 2.8, 2.6, S["base"])        # head
    c.put(23, 13, S["light"])
    for sx, ln in ((22, 4), (26, 3)):             # stalks
        for i in range(ln):
            c.put(sx, 12 - i, S["base"])
            c.put(sx + 1, 12 - i, S["base"])
    shx, shy = 13, 13
    c.ellipse(shx + 1, shy + 1, 7.0, 6.6, S["shell_deep"])
    c.ellipse(shx, shy, 6.8, 6.4, S["shell"])
    for t in range(34):
        a = 2.6 + t * 0.38
        rr = 6.0 - t * 0.16
        c.put(round(shx + math.cos(a) * rr), round(shy + math.sin(a) * rr * 0.92),
              S["shell_deep"])
    c.put(shx - 3, shy - 4, S["glow"])
    c.put(shx - 4, shy - 3, S["glow"])
    c.outline(palette.OUTLINE)
    for sx, ln in ((22, 4), (26, 3)):             # stalk cores
        for i in range(1, ln - 1):
            c.put(sx, 12 - i, S["base"])
            c.put(sx + 1, 12 - i, S["base"])
    for (tx, ty) in ((22, 8), (26, 9)):
        c.put(tx, ty, S["glow"])
        c.put(tx + 1, ty, S["glow"])
        c.put(tx, ty - 1, with_alpha(S["glow"], 130))
    for x, y in ((10, 25), (14, 26), (18, 25)):
        c.put(x, y, S["glow"])
    c.save(f"{icon_dir}/dewsnail.png")
    # ...and the same drawing as the ITEM icon. Netting a snail now hands the
    # player a `dewsnail` item (NetToolItem drops the mob's loot table and
    # nothing else, so an item is the only way a catch can pay), and an item
    # with no items/<id>.png shows the engine's error texture. The bestiary
    # icon and the thing in your hand are the same animal, so they are the same
    # drawing rather than two that drift apart.
    c.save(f"{icon_dir}/../../items/dewsnail.png")
