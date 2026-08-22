"""NPC sprite sheets: the Sky Warden and the two spire cats.

Both use the standard mob sheet layout (6 cols: idle, walk x4, in-liquid;
4 rows: Up, Right, Down, Left). The Warden uses 64px cells like other tall
mobs; the cats use 32px cells (sheet 192x128).
"""

from PIL import Image
from px import Canvas, Rng, with_alpha
import palette

COLS = 6


def _mist_clip(c, cell, water_y):
    mist = palette.MISTSEA
    for x in range(cell):
        for y in range(water_y, cell):
            if c.filled(x, y):
                c.put(x, y, (0, 0, 0, 0))
    cx = cell // 2
    c.ellipse(cx, water_y, cell * 0.3, 2.6, with_alpha(mist["hi"], 230))
    c.ellipse(cx, water_y + 2, cell * 0.22, 2.0, with_alpha(mist["light"], 200))


# --- The Sky Warden ----------------------------------------------------------

def _warden_frame(facing, step, swim=False):
    """Tall, thin, slightly stooped old keeper in a feather-trimmed coat,
    carrying a mistglass lantern staff. Burton-esque silhouette: narrow,
    crooked, gentle."""
    c = Canvas(64, 64)
    w = palette.WARDEN
    cx = 32
    feet_y = 56
    bob = 1 if step != 0 else 0
    head_top = 12 - bob

    sway = step  # coat sways with the walk cycle

    if facing in ("down", "up"):
        # coat: long, narrow at the shoulders, flaring slightly, hem crooked
        for y in range(head_top + 12, feet_y):
            t = (y - (head_top + 12)) / max(feet_y - head_top - 12, 1)
            half = 5 + round(4 * t)
            off = round(sway * t * 2)
            for dx in range(-half, half + 1):
                tone = w["coat"]
                if dx <= -half + 1:
                    tone = w["coat_light"]
                elif dx >= half - 1:
                    tone = w["coat_deep"]
                c.put(cx + dx + off, y, tone)
        # ragged hem: 1px teeth along the bottom edge
        rng = Rng(0x9AEF + step)
        for dx in range(-9, 10, 2):
            if rng.chance(0.6):
                c.put(cx + dx + round(sway * 2), feet_y, w["coat_deep"])
        # feather collar: two rows of alternating tufts, wider than the shoulders
        for dx in range(-7, 8):
            c.put(cx + dx, head_top + 11, w["feather"])
            if dx % 2 == 0:
                c.put(cx + dx, head_top + 10, w["feather"])
            else:
                c.put(cx + dx, head_top + 12, w["feather"])
        # head: narrow face framed by hanging white hair
        for y in range(head_top, head_top + 11):
            for dx in range(-3, 4):
                c.put(cx + dx, y, w["skin"] if facing == "down" else w["skin_shade"])
        rng = Rng(0x9A12 + step)
        for dx in range(-5, 6):
            c.put(cx + dx, head_top - 1, w["hair"])
            if rng.chance(0.6):
                c.put(cx + dx, head_top - 2, w["hair"])
        for y in range(head_top, head_top + 8):  # hair curtains left/right
            c.put(cx - 4, y, w["hair"])
            c.put(cx + 4, y, w["hair"])
        c.put(cx - 5, head_top + 2, w["hair"])
        c.put(cx + 5, head_top + 4, w["hair"])
        if facing == "up":
            # hair covers the back of the head down to the collar
            for y in range(head_top, head_top + 9):
                for dx in range(-4, 5):
                    c.put(cx + dx, y, w["hair"])
        # staff with mistglass lantern (held to his right = our left on down)
        staff_x = cx + (-10 if facing == "down" else 10)
        for y in range(head_top + 3, feet_y - 1):
            c.put(staff_x, y, w["staff"])
            c.put(staff_x + 1, y, palette.WOOD["deep"])
        # lantern housing: small iron cage with glowing core
        c.rect(staff_x - 1, head_top - 2, 4, 5, palette.IRONWORK["base"])
        c.rect(staff_x, head_top - 1, 2, 3, w["lanternglow"])
        c.put(staff_x, head_top - 3, palette.IRONWORK["light"])
    else:  # right profile
        # stooped back: head juts forward
        lean = 4
        for y in range(head_top + 12, feet_y):
            t = (y - (head_top + 12)) / max(feet_y - head_top - 12, 1)
            half = 4 + round(3 * t)
            off = round(lean * (1 - t)) + round(sway * t)
            for dx in range(-half, half + 1):
                tone = w["coat"]
                if dx <= -half + 1:
                    tone = w["coat_light"]
                elif dx >= half - 1:
                    tone = w["coat_deep"]
                c.put(cx + dx + off, y, tone)
        rng = Rng(0x9AEE + step)
        for dx in range(-7, 8, 2):  # ragged hem
            if rng.chance(0.6):
                c.put(cx + dx + 1, feet_y, w["coat_deep"])
        for dx in range(-5, 7):
            c.put(cx + dx + lean, head_top + 11, w["feather"])
            if dx % 2 == 0:
                c.put(cx + dx + lean, head_top + 10, w["feather"])
        # head leaning forward, long pointed nose
        for y in range(head_top, head_top + 11):
            for dx in range(-3, 4):
                c.put(cx + dx + lean, y, w["skin"])
        c.put(cx + 4 + lean, head_top + 5, w["skin"])
        c.put(cx + 5 + lean, head_top + 5, w["skin"])
        c.put(cx + 6 + lean, head_top + 6, w["skin_shade"])  # the nose
        rng = Rng(0x9A34 + step)
        for dx in range(-4, 4):
            c.put(cx + dx + lean, head_top - 1, w["hair"])
            if rng.chance(0.5):
                c.put(cx + dx + lean, head_top - 2, w["hair"])
        for y in range(head_top, head_top + 8):  # hair at the back of the head
            c.put(cx - 4 + lean, y, w["hair"])
            c.put(cx - 5 + lean, y, w["hair"] if y < head_top + 5 else w["coat_light"])
        # staff in front
        staff_x = cx + 11
        for y in range(head_top + 3, feet_y - 1):
            c.put(staff_x, y, w["staff"])
            c.put(staff_x + 1, y, palette.WOOD["deep"])
        c.rect(staff_x - 1, head_top - 2, 4, 5, palette.IRONWORK["base"])
        c.rect(staff_x, head_top - 1, 2, 3, w["lanternglow"])
        c.put(staff_x, head_top - 3, palette.IRONWORK["light"])

    c.outline(palette.OUTLINE)
    # face details after outline so they stay readable
    if facing == "down":
        c.put(cx - 2, head_top + 4, palette.OUTLINE)
        c.put(cx + 2, head_top + 4, palette.OUTLINE)
        c.put(cx - 2, head_top + 5, w["eye"])
        c.put(cx + 2, head_top + 5, w["eye"])
        # tired brow lines + small frown
        c.put(cx - 3, head_top + 3, w["skin_shade"])
        c.put(cx + 3, head_top + 3, w["skin_shade"])
        c.put(cx, head_top + 8, w["skin_shade"])
    elif facing == "right":
        c.put(cx + 3 + 4, head_top + 5, w["eye"])
    if swim:
        _mist_clip(c, 64, 42)
    return c


def gen_warden(path):
    steps = {0: 0, 1: 1, 2: 0, 3: -1, 4: 0, 5: 0}
    sheet = Canvas(COLS * 64, 4 * 64)
    for col in range(COLS):
        swim = col == 5
        up = _warden_frame("up", steps[col], swim)
        right = _warden_frame("right", steps[col], swim)
        down = _warden_frame("down", steps[col], swim)
        left = right.mirrored()
        for row, sprite in enumerate((up, right, down, left)):
            sheet.paste(sprite, col * 64, row * 64)
    sheet.save(path)


# --- The cats ----------------------------------------------------------------

def _cat_frame(colors, facing, col):
    """32px cat. col 0 = sitting (tail curled), 1-4 walk, 5 = swim."""
    c = Canvas(32, 32)
    step = {0: 0, 1: 1, 2: 0, 3: -1, 4: 0, 5: 0}[col]
    sitting = col == 0
    swim = col == 5
    body = colors.get("base", colors.get("white"))
    shade = colors.get("deep", colors.get("shade"))
    light = colors.get("light", colors.get("white"))
    cx = 16
    ground = 26

    if sitting:
        # upright sit: pear-shaped body, head on top, tail curled around paws
        c.ellipse(cx, ground - 5, 5.5, 6, body)
        c.ellipse(cx, ground - 10, 3.8, 3.6, body)  # chest
        head_y = ground - 15
    else:
        # walking: horizontal body
        c.ellipse(cx, ground - 6, 7, 4.2, body)
        head_y = ground - 9
        # legs (2px stubs, alternating)
        for i, lx in enumerate((-5, -2, 2, 5)):
            off = step if i % 2 == 0 else -step
            c.rect(cx + lx, ground - 3 + (1 if off > 0 else 0), 2, 4, shade)
        head_x_off = 8 if facing == "right" else 0
    # head
    hx = cx if facing in ("down", "up") or sitting else cx + 7
    c.ellipse(hx, head_y, 4.2, 3.8, body)
    # ears (triangles)
    for side in (-1, 1):
        ex = hx + side * 3
        c.put(ex, head_y - 4, shade)
        c.put(ex, head_y - 3, body)
        c.put(ex + (0 if side < 0 else 0), head_y - 5, shade)
    # tail
    if sitting:
        for i in range(7):
            c.put(cx - 5 + i, ground + 1, shade)
        c.put(cx + 2, ground, shade)
    else:
        # low S-curve trailing behind the body, tip flicked up by the walk step
        tx = cx - 7
        curve = ((0, -5), (-1, -6), (-2, -6), (-3, -7), (-4, -7), (-5, -8 - abs(step)))
        for (ox, oy) in curve:
            c.put(tx + ox, ground + oy, shade)
        c.put(tx + curve[-1][0], ground + curve[-1][1] - 1, body)

    # tabby patches for Peanut
    if "tabby" in colors:
        rng = Rng(0xCA7 + col + (1 if facing == "right" else 0))
        patches = [(hx - 2, head_y - 2), (cx + 2, ground - 7), (cx - 3, ground - 5)]
        for px_, py_ in patches:
            c.ellipse(px_, py_, 2.2, 1.8, colors["tabby"])
            c.put(px_, py_, colors["tabby_dark"])
        # striped tail tip
        c.put(cx - 8 + (16 if facing == "left" else 0), ground - 12, colors["tabby"])

    # belly light
    if not sitting:
        c.ellipse(cx, ground - 4, 4, 1.6, light)

    c.outline(palette.OUTLINE)
    # face after outline
    if facing == "down" or sitting:
        c.put(hx - 2, head_y - 1, colors["eye"])
        c.put(hx + 2, head_y - 1, colors["eye"])
        c.put(hx, head_y + 1, colors["nose"])
    elif facing == "right":
        c.put(hx + 2, head_y - 1, colors["eye"])
    if swim:
        _mist_clip(c, 32, 22)
    return c


def _gen_cat(path, colors):
    sheet = Canvas(COLS * 32, 4 * 32)
    for col in range(COLS):
        up = _cat_frame(colors, "up", col)
        right = _cat_frame(colors, "right", col)
        down = _cat_frame(colors, "down", col)
        left = right.mirrored()
        for row, sprite in enumerate((up, right, down, left)):
            sheet.paste(sprite, col * 32, row * 32)
    sheet.save(path)


def gen_cats(black_path, tabby_path):
    _gen_cat(black_path, palette.CAT_BLACK)
    _gen_cat(tabby_path, palette.CAT_TABBY)


def gen_npc_icons(dir_path):
    w = palette.WARDEN
    c = Canvas(32, 32)
    # warden bust: hair, face, feather collar
    c.rect(11, 8, 10, 11, w["skin"])
    rng = Rng(0x1C0)
    for x in range(9, 23):
        c.put(x, 7, w["hair"])
        if rng.chance(0.6):
            c.put(x, 6, w["hair"])
    for x in range(10, 22):
        c.put(x, 19, w["feather"])
        c.put(x, 20, w["coat"])
        c.put(x, 21, w["coat"])
    c.outline(palette.OUTLINE)
    c.put(13, 12, palette.OUTLINE)
    c.put(18, 12, palette.OUTLINE)
    c.put(13, 13, w["eye"])
    c.put(18, 13, w["eye"])
    c.save(f"{dir_path}/skywarden.png")

    for name, colors in (("spirecatblack", palette.CAT_BLACK), ("spirecattabby", palette.CAT_TABBY)):
        c = Canvas(32, 32)
        body = colors.get("base", colors.get("white"))
        shade = colors.get("deep", colors.get("shade"))
        c.ellipse(16, 20, 7, 7, body)
        c.ellipse(16, 12, 5.5, 5, body)
        for side in (-1, 1):
            c.put(16 + side * 4, 6, shade)
            c.put(16 + side * 4, 7, body)
            c.put(16 + side * 5, 5, shade)
        if "tabby" in colors:
            c.ellipse(13, 11, 2.4, 2, colors["tabby"])
            c.ellipse(19, 20, 2.6, 2.2, colors["tabby"])
            c.put(13, 11, colors["tabby_dark"])
        c.outline(palette.OUTLINE)
        c.put(13, 12, colors["eye"])
        c.put(19, 12, colors["eye"])
        c.put(16, 14, colors["nose"])
        c.save(f"{dir_path}/{name}.png")
