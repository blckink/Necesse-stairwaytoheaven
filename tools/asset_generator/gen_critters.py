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
