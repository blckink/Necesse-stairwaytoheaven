"""Mob sprite sheets.

Walking/flying 4-direction sheets are 6 columns x 4 rows of 64x64 cells
(384x256): columns = idle, walk 1-4, in-liquid; rows = Up, Right, Down, Left
(see docs/research/asset-formats.md). The Storm Wisp uses the simple stacked
layout its draw code slices: 64 wide, row 0 = body, row 1 = glow.
"""

from PIL import Image
from px import Canvas, Rng, with_alpha, mix
import palette

CELL = 64
COLS = 6  # idle, walk x4, swim
ROWS = 4  # Up, Right, Down, Left


def _rotated(canvas, transpose):
    out = Canvas(canvas.width, canvas.height)
    out.img = canvas.img.transpose(transpose)
    out.px = out.img.load()
    return out


def _mist_overlay(c):
    """Half-sunk-in-mist look for the in-liquid column."""
    mist = palette.MISTSEA
    for x in range(CELL):
        for y in range(40, CELL):
            if c.filled(x, y):
                c.put(x, y, (0, 0, 0, 0))
    for i, (rx, ry) in enumerate(((20, 3), (14, 2.4), (9, 2))):
        c.ellipse(32, 40 + i, rx, ry, with_alpha(mist["hi"] if i == 0 else mist["light"], 230 - i * 30))


# --- Zephyr Ray --------------------------------------------------------------

def _ray_base(wing_spread, frame_seed):
    """Top view, head up. wing_spread in [0..1]: 0 folded, 1 full."""
    c = Canvas(CELL, CELL)
    ramp = palette.ZEPHYR
    rng = Rng(0x2A7 + frame_seed)
    cx, cy = 32, 32
    half_span = round(12 + 14 * wing_spread)
    sweep = round(6 + 3 * (1 - wing_spread))
    # wings: broad triangles from the body out and slightly back
    for side in (-1, 1):
        tip_x = cx + side * half_span
        tip_y = cy + sweep
        for t in range(0, 101, 4):
            f = t / 100.0
            x_edge = round(cx + side * 4 + (tip_x - cx - side * 4) * f)
            y_front = round(cy - 9 + (tip_y - (cy - 9)) * f)
            y_back = round(cy + 8 + (tip_y - (cy + 8)) * f * 1.05)
            for y in range(min(y_front, y_back), max(y_front, y_back) + 1):
                c.put(x_edge, y, ramp["base"])
    # body: sleek lens with head up
    c.ellipse(cx, cy, 5.5, 11, ramp["base"])
    c.ellipse(cx, cy - 2, 4, 7, ramp["light"])
    # head tip + eye ridges
    c.ellipse(cx, cy - 10, 2.5, 2.5, ramp["light"])
    c.put(cx - 2, cy - 10, palette.OUTLINE)
    c.put(cx + 2, cy - 10, palette.OUTLINE)
    # tail whip
    for i in range(9):
        c.put(cx + (1 if i % 3 == 2 else 0), cy + 11 + i, ramp["deep"] if i > 4 else ramp["base"])
    # wing shading: leading edge light, trailing dark
    c.shade_topleft(ramp["hi"], ramp["deep"])
    c.outline(palette.OUTLINE)
    # teal accent spots on the wings
    for side in (-1, 1):
        sx = cx + side * (6 + round(5 * wing_spread))
        c.put(sx, cy, ramp["accent"])
        if rng.chance(0.8):
            c.put(sx + side, cy + 2, ramp["accent"])
    return c


def gen_zephyrray(path):
    spreads = {0: 0.75, 1: 1.0, 2: 0.75, 3: 0.35, 4: 0.75, 5: 0.7}
    sheet = Canvas(COLS * CELL, ROWS * CELL)
    for col in range(COLS):
        up = _ray_base(spreads[col], col)
        if col == 5:
            _mist_overlay(up)
        right = _rotated(up, Image.ROTATE_270)
        down = _rotated(up, Image.ROTATE_180)
        left = _rotated(up, Image.ROTATE_90)
        for row, sprite in enumerate((up, right, down, left)):
            sheet.paste(sprite, col * CELL, row * CELL)
    sheet.save(path)


# --- Skystone Golem ----------------------------------------------------------

def _golem_frame(facing, step, swim=False):
    """facing: 'up' | 'right' | 'down'. step in -1..1 shifts legs/arms."""
    c = Canvas(CELL, CELL)
    r = palette.GOLEM
    cx = 32
    feet_y = 54
    bob = 1 if step != 0 else 0
    top = 18 - bob
    if facing in ("up", "down"):
        # legs
        if not swim:
            c.rect(cx - 9, feet_y - 8, 6, 8 + (1 if step > 0 else 0), r["deep"])
            c.rect(cx + 3, feet_y - 8, 6, 8 + (1 if step < 0 else 0), r["deep"])
        # torso: massive slab, narrower hips
        c.rect(cx - 12, top + 10, 24, 20, r["base"])
        c.rect(cx - 10, top + 6, 20, 6, r["base"])
        # arms: heavy stone fists at the sides
        arm_shift = step * 2
        c.rect(cx - 17, top + 11 + arm_shift, 5, 15, r["base"])
        c.rect(cx + 12, top + 11 - arm_shift, 5, 15, r["base"])
        # head block
        c.rect(cx - 7, top, 14, 10, r["light"])
    else:  # right profile
        if not swim:
            front = 2 * step
            c.rect(cx - 7 + front, feet_y - 8, 6, 8, r["deep"])
            c.rect(cx + 2 - front, feet_y - 8, 6, 8, r["deep"])
        c.rect(cx - 10, top + 10, 20, 20, r["base"])
        c.rect(cx - 8, top + 6, 16, 6, r["base"])
        arm_swing = 3 * step
        c.rect(cx + 3 + arm_swing, top + 12, 5, 13, r["light"])
        c.rect(cx - 5, top, 12, 10, r["light"])
        c.put(cx - 2, top + 24, r["moss"])
    # weathering speckle
    rng = Rng(0x601E + (1 if facing == "up" else 2 if facing == "right" else 3) * 97 + (step + 2) * 31)
    for _ in range(10):
        x = rng.range(cx - 12, cx + 12)
        y = rng.range(top, feet_y - 6)
        if c.filled(x, y):
            c.put(x, y, r["deep"] if rng.chance(0.7) else r["moss"])
    c.shade_topleft(r["hi"], r["deep"])
    c.outline(palette.OUTLINE)
    # face details go on AFTER shading/outline so they stay readable
    if facing == "down":
        c.rect(cx - 4, top + 4, 2, 2, r["eye"])
        c.rect(cx + 2, top + 4, 2, 2, r["eye"])
        c.put(cx - 4, top + 4, (240, 252, 252))
        c.put(cx + 2, top + 4, (240, 252, 252))
        c.line(cx - 1, top + 13, cx + 1, top + 21, r["deep"])
        c.put(cx, top + 17, r["eye"])
    elif facing == "up":
        c.rect(cx - 6, top + 1, 12, 3, r["moss"])
        c.rect(cx - 11, top + 11, 22, 2, r["light"])
    else:
        c.rect(cx + 4, top + 4, 2, 2, r["eye"])
        c.put(cx + 4, top + 4, (240, 252, 252))
    if swim:
        _mist_overlay(c)
    return c


def gen_skystonegolem(path):
    steps = {0: 0, 1: 1, 2: 0, 3: -1, 4: 0, 5: 0}
    sheet = Canvas(COLS * CELL, ROWS * CELL)
    for col in range(COLS):
        swim = col == 5
        up = _golem_frame("up", steps[col], swim)
        right = _golem_frame("right", steps[col], swim)
        down = _golem_frame("down", steps[col], swim)
        left = right.mirrored()
        for row, sprite in enumerate((up, right, down, left)):
            sheet.paste(sprite, col * CELL, row * CELL)
    sheet.save(path)


# --- Storm Wisp --------------------------------------------------------------

def gen_stormwisp(path):
    r = palette.WISP
    sheet = Canvas(64, 128)
    rng = Rng(0x3157)
    cx, cy = 32, 32
    # body: layered storm core
    sheet.ellipse(cx, cy, 13, 12, r["deep"])
    sheet.ellipse(cx - 1, cy - 1, 10, 9, r["base"])
    sheet.ellipse(cx - 2, cy - 2, 6.5, 6, r["inner"])
    sheet.ellipse(cx - 2, cy - 3, 3.2, 3, r["core"])
    # jagged static arcs around the rim
    for _ in range(7):
        ang_x = rng.pick((-1, 1))
        ang_y = rng.pick((-1, 1))
        x = cx + ang_x * rng.range(10, 14)
        y = cy + ang_y * rng.range(6, 12)
        sheet.put(x, y, r["spark"])
        sheet.put(x + ang_x, y - ang_y, r["inner"])
        sheet.put(x + 2 * ang_x, y, r["spark"])
    # trailing wisps below
    sheet.put(cx - 6, cy + 13, r["base"])
    sheet.put(cx - 5, cy + 15, r["deep"])
    sheet.put(cx + 5, cy + 14, r["base"])
    # outline pass for the body region only (glow row must stay outline-free)
    body = Canvas(64, 64)
    body.paste(sheet, 0, 0)
    body.outline(palette.OUTLINE)
    sheet.rect(0, 0, 64, 64, (0, 0, 0, 0))
    sheet.paste(body, 0, 0)
    # glow row: soft halo, drawn additively by the game with a pulsing light
    for x in range(64):
        for y in range(64):
            dx = (x - cx) / 22.0
            dy = (y - cy) / 20.0
            d = dx * dx + dy * dy
            if d <= 1.0:
                alpha = int(150 * (1.0 - d) ** 2)
                if alpha > 8:
                    sheet.put(x, 64 + y, with_alpha(mix(r["inner"], r["core"], 1.0 - d), alpha))
    sheet.save(path)


# --- Bestiary icons ----------------------------------------------------------

def _icon_canvas():
    return Canvas(32, 32)


def gen_icons(dir_path):
    # Zephyr Ray: top view, head up — matches the in-world silhouette
    c = _icon_canvas()
    z = palette.ZEPHYR
    for side in (-1, 1):
        c.line(16 + side * 3, 14, 16 + side * 13, 19, z["base"])
        c.line(16 + side * 3, 17, 16 + side * 13, 20, z["base"])
        c.line(16 + side * 3, 15, 16 + side * 12, 19, z["light"])
        c.line(16 + side * 3, 16, 16 + side * 13, 20, z["deep"])
    c.ellipse(16, 16, 4, 8, z["base"])
    c.ellipse(16, 14, 3, 5, z["light"])
    for i in range(6):
        c.put(16, 24 + i, z["deep"])
    c.outline(palette.OUTLINE)
    c.put(14, 10, palette.OUTLINE)
    c.put(18, 10, palette.OUTLINE)
    c.put(10, 17, z["accent"])
    c.put(22, 17, z["accent"])
    c.save(f"{dir_path}/zephyrray.png")

    # Storm Wisp: mini orb
    c = _icon_canvas()
    w = palette.WISP
    c.ellipse(16, 16, 9, 8.5, w["deep"])
    c.ellipse(15, 15, 6.5, 6, w["base"])
    c.ellipse(14, 14, 4, 3.5, w["inner"])
    c.ellipse(14, 13, 2, 2, w["core"])
    c.put(25, 12, w["spark"])
    c.put(7, 20, w["spark"])
    c.outline(palette.OUTLINE)
    c.save(f"{dir_path}/stormwisp.png")

    # Skystone Golem: face block — eyes and moss go on AFTER shading/outline
    c = _icon_canvas()
    g = palette.GOLEM
    c.rect(8, 6, 16, 13, g["light"])
    c.rect(6, 19, 20, 8, g["base"])
    c.shade_topleft(g["hi"], g["deep"])
    c.outline(palette.OUTLINE)
    c.rect(11, 11, 3, 3, g["eye"])
    c.rect(18, 11, 3, 3, g["eye"])
    c.put(12, 11, (240, 252, 252))
    c.put(19, 11, (240, 252, 252))
    c.line(14, 20, 17, 25, g["deep"])
    c.put(10, 22, g["moss"])
    c.put(23, 24, g["moss"])
    c.save(f"{dir_path}/skystonegolem.png")
