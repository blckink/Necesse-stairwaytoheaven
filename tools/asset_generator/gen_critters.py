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


def _lamb_frame(facing, col):
    c = Canvas(32, 32)
    L = palette.LAMB
    step = {0: 0, 1: 1, 2: 0, 3: -1, 4: 0, 5: 0}[col]
    cx, ground = 16, 26
    # legs first (behind body)
    for i, lx in enumerate((-5, -2, 2, 5)):
        off = step if i % 2 == 0 else -step
        c.rect(cx + lx, ground - 4 + (1 if off > 0 else 0), 2, 4, L["face_dark"])
    # fluffy body: base blob + puff bumps along the top
    c.ellipse(cx, ground - 8, 7.5, 5, L["wool"])
    rng = Rng(0x1A3B + col)
    for bx in (-5, -2, 1, 4):
        c.ellipse(cx + bx, ground - 12 + rng.range(0, 1), 2.4, 2, L["wool"])
    c.ellipse(cx - 2, ground - 6, 4.5, 2.5, L["wool_shade"])   # belly shade
    # cotton tail
    c.ellipse(cx - 8, ground - 9, 1.8, 1.6, L["wool"])
    # head at the front (right when walking right, centered for up/down)
    hx = cx + (8 if facing == "right" else 0)
    hy = ground - 12 if facing != "right" else ground - 11
    if facing == "up":
        c.ellipse(hx, hy, 3, 3, L["wool"])                     # wool from behind
    else:
        c.ellipse(hx, hy, 2.8, 3, L["face"])
        c.put(hx - 3, hy - 2, L["face_dark"])                  # ears
        c.put(hx + 3, hy - 2, L["face_dark"])
        c.ellipse(hx, hy - 3, 2.4, 1.4, L["wool"])             # wool tuft on top
    c.outline(palette.OUTLINE)
    if facing == "down":
        c.put(hx - 1, hy, L["face_dark"])
        c.put(hx + 1, hy, L["face_dark"])
    elif facing == "right":
        c.put(hx + 1, hy, L["face_dark"])
    if col == 5:
        _mist_clip(c, 22)
    return c


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
    _gen_sheet(f"{mob_dir}/cloudlamb.png", _lamb_frame)
    _gen_sheet(f"{mob_dir}/glowmoth.png", _moth_frame)
    _gen_sheet(f"{mob_dir}/sparkbeetle.png", _beetle_frame)


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
