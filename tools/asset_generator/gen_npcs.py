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
    """Old, gentle, slightly stooped keeper on VANILLA CHIBI proportions:
    big round head (~26px wide with the long gray hair curtains), 19px
    shoulders flaring to a 23px ragged hem, two-row feather collar, belt +
    satchel, long beard, and an iron cage lantern on a planted staff.

    Construction is vanilla-style: the head silhouette is one big HAIR MASS,
    the face is a skin opening painted into it, the beard a mass on top.
    Face + glow details go in AFTER the outline pass."""
    c = Canvas(64, 64)
    w = palette.WARDEN
    iron = palette.IRONWORK
    cx = 32
    feet_y = 56
    bob = 1 if step != 0 else 0
    hd = -bob                    # head/collar cluster bobs up on mid-steps
    sway = step

    def sway_off(y):
        # skirt sway builds from the belt down to +/-2 at the hem
        return round(sway * 2 * max(0, y - 39) / 16.0)

    def draw_staff(px_):
        """Planted walking staff: pole cols px_/px_+1, 7px iron cage lantern."""
        for y in range(12, feet_y):
            c.put(px_, y, w["staff_hi"])
            c.put(px_ + 1, y, w["staff"])
        left = px_ - 2                          # cage x: left..left+6
        c.rect(px_, 2, 2, 2, iron["light"])     # finial
        for x in range(left, left + 7):         # top / bottom bars
            c.put(x, 4, iron["base"])
            c.put(x, 11, iron["deep"] if x in (left, left + 6) else iron["base"])
        c.put(left, 4, iron["light"])
        c.put(left + 6, 4, iron["light"])
        for y in range(5, 11):                  # glass + three cage posts
            for x in (left + 1, left + 2, left + 4, left + 5):
                c.put(x, y, w["lanternglow"])
            for x in (left, left + 3, left + 6):
                c.put(x, y, iron["base"])
        c.put(left, 5, iron["light"])
        c.put(left + 6, 5, iron["light"])
        for x in (left + 1, left + 2, left + 4, left + 5):   # bright core
            c.put(x, 7, palette.STAIRLIGHT["hi"])
            c.put(x, 8, palette.STAIRLIGHT["hi"])

    def halo(px_):
        mid = px_ + 1
        for (hx, hy, a) in ((mid - 6, 6, 140), (mid + 6, 8, 140), (mid - 5, 11, 140),
                            (mid + 5, 3, 140), (mid - 1, 0, 140), (mid + 2, 13, 140),
                            (mid - 5, 4, 200), (mid + 5, 10, 200)):
            c.put(hx, hy, with_alpha(w["lanternglow"], a))

    def coat_frontback(satchel_side):
        shoulder_y, belt_y = 30, 38
        for y in range(shoulder_y, feet_y):
            t = (y - shoulder_y) / float(feet_y - 1 - shoulder_y)
            half = 9 + round(2 * t)             # 19px shoulders -> 23px hem
            off = sway_off(y)
            for dx in range(-half, half + 1):
                tone = w["coat"]
                if dx <= -half + 1:
                    tone = w["coat_light"]
                elif dx >= half - 1:
                    tone = w["coat_deep"]
                elif dx == 1:
                    tone = w["coat_deep"]        # center crease
                elif abs(dx) == 7 and shoulder_y < y <= belt_y + 1:
                    tone = w["coat_deep"]        # sleeve seams
                elif y > belt_y + 3 and dx in (-4, 5):
                    tone = w["coat_deep"]        # skirt creases
                elif y > belt_y + 3 and dx in (-3, 6):
                    tone = w["coat_light"]       # crease ridges
                c.put(cx + dx + off, y, tone)
            if y == shoulder_y:                  # rim light on the shoulders
                for dx in range(-half, half + 1):
                    if abs(dx) >= 3:
                        c.put(cx + dx, y, w["coat_hi"])
        for dx in range(-9, 10):                 # belt
            c.put(cx + dx, belt_y, iron["base"])
            c.put(cx + dx, belt_y + 1, iron["deep"])
        if satchel_side > 0:                     # buckle only on the front side
            c.rect(cx - 1, belt_y, 3, 2, iron["light"])
        sx0 = cx + 5 if satchel_side > 0 else cx - 10
        c.rect(sx0, 41, 6, 6, palette.WOOD["deep"])       # hip satchel
        c.rect(sx0, 41, 6, 1, palette.WOOD["light"])
        c.put(sx0 + 2, 44, iron["light"])
        c.put(sx0 + 2, 40, palette.WOOD["deep"])          # strap stub
        rng = Rng(0x9AEF + step)
        for dx in range(-11, 12):                # ragged hem with pale lining
            c.put(cx + dx + sway_off(54), 54, w["coat_light"])
            if rng.chance(0.6):
                c.put(cx + dx + sway_off(55), 55, w["coat_deep"])
            if dx % 3 == 0:                      # dotted gold hem trim (v0.4.1)
                c.put(cx + dx + sway_off(53), 53, w["trim"])
                if dx % 6 == 0:
                    c.put(cx + dx + sway_off(52), 52, w["trim_hi"])

    def collar(ccx):
        for dx in range(-10, 11):                # scallop tips, row 1
            if dx % 2 == 0:
                c.put(ccx + dx, 27 + hd, w["feather_hi"])
        for dy in (28, 29):
            for dx in range(-11, 12):
                c.put(ccx + dx, dy + hd, w["feather"])
        for dx in range(-9, 10):                 # scallop tips, row 2
            if dx % 2 != 0:
                c.put(ccx + dx, 30 + hd, w["feather_hi"])
        for dx in range(-10, 11):
            c.put(ccx + dx, 31 + hd, w["feather"])

    def hood_back(ccx, wide=14.5):
        """v0.6: the Skywatch storm hood, worn DOWN — a cowl mass behind the
        hair (drawn BEFORE the hair so the white hair spills out of it).
        This is the silhouette the warden icon always showed; on the world
        sprite it is what breaks the 'generic wizard' read."""
        c.ellipse(ccx, 18.2 + hd, wide, 12.2, w["coat_deep"])
        c.ellipse(ccx, 21.0 + hd, wide - 1.5, 10.2, w["coat"])
        for dx in range(-int(wide) + 1, int(wide)):     # lit rim, light TL
            if abs(dx) > wide - 5:
                c.put(ccx + dx, 10 + hd, w["coat_light"])
        c.put(ccx - int(wide) + 2, 11 + hd, w["coat_light"])
        c.put(ccx + int(wide) - 3, 11 + hd, w["coat_light"])

    def weather(ccx, y0, y1, seed):
        """Pale weathered mend patches on the coat — a few 2-3px clusters,
        never uniform, plus one stitched tear."""
        rng = Rng(seed)
        for _ in range(3):
            px_, py_ = ccx + rng.range(-7, 7), rng.range(y0, y1)
            for k in range(rng.range(2, 4)):
                if c.filled(px_ + k, py_) and c.get(px_ + k, py_)[:3] in (
                        w["coat"], w["coat_light"]):
                    c.put(px_ + k, py_, w["patch"])
        tx = ccx + rng.pick((-6, 5))
        for k in range(3):
            c.put(tx + k, y1 - 2 + (k % 2), w["coat_deep"])   # tear
        c.put(tx, y1 - 2, w["patch"])                          # stitch

    def clasp(ccx, cy):
        """Brass Skywatch clasp holding the hood at the throat."""
        c.rect(ccx - 1, cy, 3, 2, w["trim"])
        c.put(ccx - 1, cy, w["trim_hi"])
        c.put(ccx + 1, cy + 1, w["coat_deep"])

    def staff_arm(side):
        """Sleeve wedge from the shoulder out toward the pole."""
        for i, y in enumerate(range(32, 36)):
            a = side * (9 + i)
            b = side * (12 + i)
            for x in range(min(a, b), max(a, b) + 1):
                c.put(cx + x, y, w["coat_light"] if i == 0 else w["coat"])
            c.put(cx + b, y, w["coat_deep"])     # under-edge of the sleeve

    def staff_hand(side, pole_x):
        hx0 = pole_x if side < 0 else pole_x - 1
        c.rect(hx0, 36, 3, 2, w["skin"])         # hand gripping over the pole

    # ------------------------------------------------------------------ build
    if facing in ("down", "up"):
        pole_x = cx - 16 if facing == "down" else cx + 15
        staff_side = -1 if facing == "down" else 1
        coat_frontback(satchel_side=1 if facing == "down" else -1)
        staff_arm(staff_side)
        if facing == "down":                     # hanging hand on the free arm
            c.rect(cx + 8, 40, 2, 2, w["skin"])
        collar(cx)
        hood_back(cx)
        # --- head: hair mass -> face opening -> beard mass ---
        c.ellipse(cx, 17.5 + hd, 12.8, 10.0, w["hair"])    # 25px hair dome
        for y in range(25 + hd, 28 + hd):                  # jaw band under dome
            for dx in range(-9, 10):
                c.put(cx + dx, y, w["hair"])
        for y in range(13 + hd, 28 + hd):                  # curtains taper out:
            i = y - 13 - hd                                # L 5px, R 4px wide
            lo = -10 if i == 0 else (-12 if i == 1 else -13)
            hi = 10 if i == 0 else (11 if i == 1 else 12)
            for dx in range(lo, -8):
                c.put(cx + dx, y, w["hair_shade"] if (dx <= -12 and y > 19 + hd) else w["hair"])
            for dx in range(9, hi + 1):
                c.put(cx + dx, y, w["hair_shade"] if (dx >= 12 and y > 20 + hd) else w["hair"])
        for y in range(28 + hd, 30 + hd):                  # curtain tips
            for dx in range(-13, -10):
                c.put(cx + dx, y, w["hair_shade"])
        c.rect(cx + 10, 28 + hd, 3, 1, w["hair_shade"])
        for (dxx, yy) in ((7, 11), (8, 12), (9, 13), (9, 14)):
            c.put(cx + dxx, yy + hd, w["hair_shade"])      # dome shade, light TL
        if facing == "down":
            c.ellipse(cx, 19.5 + hd, 8.0, 7.2, w["skin"])  # face opening
            for i, bw in enumerate((7, 7, 7, 7, 6, 6, 5, 5, 4, 4, 2, 2)):
                for dx in range(-bw, bw + 1):              # long beard mass
                    c.put(cx + dx, 24 + i + hd, w["hair"])
        else:  # up: no face — broken strand shadows down the hair sheet
            rng = Rng(0x9A44 + step)
            for dx in (-9, -6, -3, 1, 4, 7):
                y0 = 12 + rng.range(0, 3)
                for y in range(y0, min(27, y0 + rng.range(6, 12))):
                    c.put(cx + dx, y + hd, w["hair_shade"])
                    if dx in (-6, 4) and y % 2 == 0:       # thicker mid strands
                        c.put(cx + dx + 1, y + hd, w["hair_shade"])
            for dx in range(-10, 11):                      # ragged hair bottom
                if rng.chance(0.45):
                    c.put(cx + dx, 27 + hd, w["hair_shade"])
                if rng.chance(0.3):
                    c.put(cx + dx, 28 + hd, w["hair"])
        draw_staff(pole_x)
        staff_hand(staff_side, pole_x)
    else:  # right profile, stooped forward
        pole_x = cx + 15
        shoulder_y, belt_y = 30, 38
        for y in range(shoulder_y, feet_y):
            t = (y - shoulder_y) / float(feet_y - 1 - shoulder_y)
            half = 8 + round(2 * t)              # 17px -> 21px in profile
            off = (2 if y < 34 else 1 if y < 40 else 0) + sway_off(y)
            for dx in range(-half, half + 1):
                tone = w["coat"]
                if dx <= -half + 1:
                    tone = w["coat_light"]
                elif dx >= half - 1:
                    tone = w["coat_deep"]
                elif y > belt_y + 3 and dx in (0, -4):
                    tone = w["coat_deep"]        # skirt creases
                elif y > belt_y + 3 and dx == -3:
                    tone = w["coat_light"]
                c.put(cx + dx + off, y, tone)
            if y == shoulder_y:
                for dx in range(-half + 1, half):
                    c.put(cx + dx + off, y, w["coat_hi"])
        for dx in range(-8, 9):                  # belt, buckle at the front
            c.put(cx + dx + 1, belt_y, iron["base"])
            c.put(cx + dx + 1, belt_y + 1, iron["deep"])
        c.rect(cx + 5, belt_y, 2, 2, iron["light"])
        c.rect(cx - 10, 41, 5, 5, palette.WOOD["deep"])    # satchel at the back
        c.rect(cx - 10, 41, 5, 1, palette.WOOD["light"])
        rng = Rng(0x9AEE + step)
        for dx in range(-10, 11):
            c.put(cx + dx + sway_off(54), 54, w["coat_light"])
            if rng.chance(0.6):
                c.put(cx + dx + sway_off(55), 55, w["coat_deep"])
        collar(cx + 2)
        hood_back(cx - 2, 12.5)
        # --- head: back-heavy hair mass, face opening at the front ---
        c.ellipse(cx + 1, 17.5 + hd, 10.8, 9.8, w["hair"])
        for y in range(24 + hd, 27 + hd):                  # jaw band
            for dx in range(-5, 3):
                c.put(cx + dx, y, w["hair"])
        for y in range(12 + hd, 28 + hd):                  # back curtain
            for dx in range(-8, -4):
                c.put(cx + dx, y, w["hair_shade"] if (dx <= -7 and y > 19 + hd) else w["hair"])
        c.rect(cx - 8, 28 + hd, 3, 2, w["hair_shade"])     # curtain tip
        c.ellipse(cx + 4, 19.5 + hd, 6.8, 7.0, w["skin"])  # face opening
        for dx in range(-3, 10):                           # fringe over the brow
            c.put(cx + dx, 13 + hd, w["hair"])
        for dx in range(-3, 3):                            # swept-back part
            c.put(cx + dx, 14 + hd, w["hair"])
        c.put(cx + 5, 11 + hd, w["hair_shade"])            # dome shade, light TL
        c.put(cx + 6, 12 + hd, w["hair_shade"])
        c.rect(cx + 11, 17 + hd, 3, 1, w["skin"])          # big hooked nose
        c.rect(cx + 11, 18 + hd, 3, 1, w["skin"])
        c.rect(cx + 11, 19 + hd, 2, 1, w["skin_shade"])
        for i, (a, b) in enumerate(((5, 11), (4, 11), (3, 10), (3, 10), (4, 10),
                                    (4, 10), (5, 9), (5, 9), (6, 8), (6, 8), (6, 8))):
            for x in range(a, b + 1):                      # mustache under the nose,
                c.put(cx + x, 20 + i + hd, w["hair"])      # beard jutting forward
        staff_arm(1)
        draw_staff(pole_x)
        staff_hand(1, pole_x)

    c.outline(palette.OUTLINE)

    # ---------------------------------------------- after-outline detail pass
    if facing == "down":
        clasp(cx, 29 + hd)                       # brass pin on the collar
        weather(cx, 40, 52, 0x9A11)
        for dx in list(range(-6, -2)) + list(range(3, 7)):
            c.put(cx + dx, 15 + hd, w["skin_shade"])       # hooded brow shade
        for dx in list(range(-5, -2)) + list(range(3, 6)):
            c.put(cx + dx, 16 + hd, palette.OUTLINE)       # heavy brow bars
        for dx in (-5, -4, 4, 5):
            c.put(cx + dx, 17 + hd, w["eye"])              # bright 2x2 eyes
            c.put(cx + dx, 18 + hd, w["eye"])
            c.put(cx + dx, 19 + hd, w["skin_shade"])       # under-eye bags
        c.rect(cx - 1, 17 + hd, 2, 3, w["skin"])           # nose bridge
        c.rect(cx - 1, 20 + hd, 2, 2, w["skin_shade"])     # nose tip
        c.put(cx - 7, 19 + hd, w["skin_shade"])            # age lines
        c.put(cx + 6, 19 + hd, w["skin_shade"])
        for (sx_, y0, y1) in ((-4, 25, 28), (-1, 24, 32), (2, 26, 30), (5, 25, 27)):
            for y in range(y0, y1 + 1):                    # beard streaks
                c.put(cx + sx_, y + hd, w["hair_shade"])
        c.put(cx, 22 + hd, w["hair_shade"])                # mustache part
        c.put(cx, 23 + hd, w["hair_shade"])
        halo(cx - 16)
    elif facing == "up":
        halo(cx + 15)
    else:
        for dx in range(6, 11):
            c.put(cx + dx, 15 + hd, w["skin_shade"])       # brow shade
        for dx in range(7, 10):
            c.put(cx + dx, 16 + hd, palette.OUTLINE)       # brow bar
        c.put(cx + 10, 16 + hd, w["skin"])                 # temple above the nose
        for dx in (8, 9):
            c.put(cx + dx, 17 + hd, w["eye"])              # eye close to the front
            c.put(cx + dx, 18 + hd, w["eye"])
            c.put(cx + dx, 19 + hd, w["skin_shade"])
        c.put(cx + 11, 19 + hd, w["skin_shade"])           # nostril
        c.put(cx + 7, 21 + hd, w["skin_shade"])            # cheek line
        c.put(cx + 8, 22 + hd, w["skin_shade"])
        clasp(cx + 3, 29 + hd)                   # brass pin on the collar
        weather(cx, 40, 52, 0x9A12)
        for (sx_, y0, y1) in ((6, 23, 26), (8, 22, 25)):
            for y in range(y0, y1 + 1):                    # beard streaks
                c.put(cx + sx_, y + hd, w["hair_shade"])
        halo(pole_x)
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
    # The keeper's bestiary icon — it shows up in thought bubbles, chat glyphs
    # and journal entries, so it has to read at 32px. Built to the vanilla
    # settler-icon recipe (elderhuman 656 opaque px, farmerhuman 772, both
    # filling the frame corner to corner): head fills the frame, and hood,
    # hair, beard and face each sit on their OWN tone with a dark separator
    # between them. The earlier version drew hood, hair and beard in one
    # near-white, so the beard disappeared and he read as a helmet.
    hood = w["coat"]                             # the cowl is coat cloth
    hood_hi = w["coat_hi"]
    hood_deep = w["coat_deep"]
    hair = w["hair_shade"]                       # mid grey, the mane mass
    beard = (240, 242, 246)                      # brighter than the mane
    beard_shade = (198, 202, 212)
    c = Canvas(32, 32)

    # cowl: a broad hood shell reaching the frame edges, with the mane and
    # face inset into its opening
    c.ellipse(16, 13.0, 14.4, 12.4, hood)
    for y in range(12, 32):                      # shoulders of the cowl
        for x in range(1, 31):
            c.put(x, y, hood)
    for yy in range(1, 14):                      # lit top-left plane
        for xx in range(2, 30):
            if (xx - 16) * 0.7 + (yy - 13) * 1.4 < -7.0 and c.get(xx, yy)[:3] == hood:
                c.put(xx, yy, hood_hi)
    for yy in range(14, 32):                     # shaded right flank
        for xx in range(24, 31):
            if c.get(xx, yy)[:3] == hood:
                c.put(xx, yy, hood_deep)
    for x in (13, 15, 17, 19):                   # gold trim across the chest
        c.put(x, 30, w["trim"])
        c.put(x, 29, w["trim_hi"])

    # the mane inside the hood opening, then the face inset into that
    c.ellipse(16, 14.0, 10.4, 10.0, hair)
    c.ellipse(16, 15.0, 7.6, 7.4, w["skin"])
    for i, bw in enumerate((6, 6, 5, 4, 3)):     # beard, its own tone
        for dx in range(-bw, bw + 1):
            c.put(16 + dx, 21 + i, beard)
    for dx in range(-4, 5):                      # moustache
        c.put(16 + dx, 20, beard_shade)

    c.outline(palette.OUTLINE)

    for x in range(8, 25):                       # hood rim shadow on the mane
        if c.get(x, 8)[:3] == hair:
            c.put(x, 8, hood_deep)
    for x in list(range(10, 14)) + list(range(19, 23)):
        c.put(x, 11, w["skin_shade"])            # brow shade
    for x in list(range(10, 14)) + list(range(19, 23)):
        c.put(x, 12, palette.OUTLINE)            # heavy brow bars
    for x in (11, 12, 20, 21):
        c.put(x, 13, w["eye"])                   # teal 2x2 eyes
        c.put(x, 14, w["eye"])
        c.put(x, 15, w["skin_shade"])            # under-eye bags
    c.rect(15, 13, 2, 4, w["skin"])              # nose bridge
    c.rect(15, 17, 2, 2, w["skin_shade"])        # nose tip
    for x in range(10, 23):                      # cheek seam under the moustache
        if c.get(x, 19)[:3] == w["skin"]:
            c.put(x, 19, w["skin_shade"])
    for (sx_, y0, y1) in ((13, 22, 25), (19, 22, 25)):
        for y in range(y0, y1 + 1):              # beard streaks
            c.put(sx_, y, beard_shade)
    c.save(f"{dir_path}/skywarden.png")

    # The same man after the recruitment, as the settlement UI shows him: the
    # feather-collar regalia is gone, replaced by the storm-blue settler shirt
    # he wears on the surface (WardenSettlerMob pins that exact color), with
    # the gold trim kept as his one warm accent.
    #
    # Built to the vanilla settler-icon recipe (elderhuman/farmerhuman): the
    # head fills the frame corner to corner, and hair / beard / face each sit
    # on their OWN tone with a dark separator between them. The earlier draft
    # drew hair and beard in one near-white and the beard simply vanished.
    shirt = (86, 96, 122)
    shirt_hi = (114, 126, 156)
    shirt_deep = (58, 66, 88)
    hair_base = w["hair_shade"]                  # mid grey — the mass
    hair_lit = w["hair"]                         # near-white — top-left plane
    beard = (240, 242, 246)                      # brighter than the hair
    beard_shade = (196, 200, 210)
    c = Canvas(32, 32)

    for x in range(3, 30):                       # shoulder line, near full width
        c.put(x, 27, shirt_hi)
    c.rect(2, 28, 29, 4, shirt)
    c.rect(2, 28, 3, 4, shirt_hi)
    c.rect(28, 28, 3, 4, shirt_deep)
    for x in range(13, 20):                      # open collar notch
        c.put(x, 28, shirt_deep)
    for x in (14, 16, 18):                       # gold placket studs
        c.put(x, 30, w["trim"])
    c.rect(14, 25, 5, 3, w["skin_shade"])        # neck into the collar

    c.ellipse(16, 12.0, 12.4, 11.0, hair_base)   # hair mass, frame to frame
    for y in range(11, 24):                      # curtains falling past the jaw
        for x in range(3, 8):
            c.put(x, y, hair_base)
        for x in range(25, 30):
            c.put(x, y, hair_base)
    for yy in range(2, 12):                      # lit top-left plane
        for xx in range(4, 30):
            if (xx - 16) * 0.7 + (yy - 12) * 1.5 < -6.0 and c.get(xx, yy)[:3] == hair_base:
                c.put(xx, yy, hair_lit)

    c.ellipse(16, 14.5, 9.0, 8.4, w["skin"])     # face opening
    for i, bw in enumerate((7, 7, 6, 5, 4)):     # beard, own tone, own shape
        for dx in range(-bw, bw + 1):
            c.put(16 + dx, 20 + i, beard)
    for dx in range(-4, 5):                      # moustache above the beard
        c.put(16 + dx, 19, beard_shade)

    c.outline(palette.OUTLINE)

    for x in range(9, 24):                       # hairline shadow onto the brow
        if c.get(x, 9)[:3] == hair_lit:
            c.put(x, 9, hair_base)
    for x in list(range(10, 14)) + list(range(19, 23)):
        c.put(x, 10, w["skin_shade"])            # brow shade
    for x in list(range(10, 14)) + list(range(19, 23)):
        c.put(x, 11, palette.OUTLINE)            # heavy brow bars
    for x in (11, 12, 20, 21):
        c.put(x, 12, w["eye"])                   # 2x2 eyes
        c.put(x, 13, w["eye"])
        c.put(x, 14, w["skin_shade"])            # under-eye bags
    c.rect(15, 12, 2, 4, w["skin"])              # nose bridge
    c.rect(15, 16, 2, 2, w["skin_shade"])        # nose tip
    for x in range(9, 24):                       # cheek seam under the moustache
        if c.get(x, 18)[:3] == w["skin"]:
            c.put(x, 18, w["skin_shade"])
    for x in (11, 12, 13, 19, 20, 21):           # cheek shade beside the beard
        c.put(x, 17, w["skin_shade"])
    for (sx_, y0, y1) in ((13, 21, 24), (19, 21, 24)):
        for y in range(y0, y1 + 1):              # beard streaks
            c.put(sx_, y, beard_shade)
    c.save(f"{dir_path}/wardensettler.png")

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


# --- The three Skyreach residents -------------------------------------------
# One parameterised portrait rather than three bespoke ones. A settler icon is
# 32x32 and read at 1x in the settlement list, so what has to differ between
# them is the SILHOUETTE of the headgear and the value of the hair against the
# skin -- not the pixel-level face work the Warden's own icon carries.

RESIDENTS = (
    # id,               skin,             hair,            head-gear, accent, eye
    ("magpiesettler",  (214, 176, 148), (58, 46, 42),  "brim",  (92, 84, 74),  (96, 122, 108)),
    ("haldasettler",   (226, 190, 160), (176, 148, 96), "cap",   (198, 186, 172), (124, 108, 76)),
    ("ossiansettler",  (206, 182, 166), (188, 190, 206), "hood",  (86, 78, 128), (108, 196, 186)),
)


def _shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def gen_resident_icons(dir_path):
    """A face per resident, distinguished by headgear silhouette first."""
    for name, skin, hair, gear, accent, eye in RESIDENTS:
        c = Canvas(32, 32)
        skin_dk = _shade(skin, 0.82)
        hair_lit = _shade(hair, 1.18)
        acc_dk = _shade(accent, 0.72)

        c.ellipse(16, 17, 8.5, 8.0, skin)              # head
        c.ellipse(16, 12, 8.6, 6.0, hair)              # hair mass
        c.ellipse(15, 10, 6.0, 3.4, hair_lit)          # lit top-left

        if gear == "brim":                             # courier's brimmed hat
            c.rect(5, 11, 22, 2, accent)
            c.rect(5, 13, 22, 1, acc_dk)
            c.ellipse(16, 8, 6.5, 4.0, accent)
            c.ellipse(15, 6, 4.5, 2.2, _shade(accent, 1.2))
        elif gear == "cap":                            # cellarer's linen cap
            c.ellipse(16, 8, 7.5, 4.6, accent)
            c.ellipse(15, 6, 5.0, 2.6, _shade(accent, 1.14))
            c.rect(8, 11, 16, 1, acc_dk)
        else:                                          # scholar's hood
            # The hood has to WRAP: a coloured cap over the crown alone reads
            # as dyed hair. Cowl first, face cut out of it, then the two
            # falling sides that make it a hood at 1x.
            c.ellipse(16, 14, 11.0, 11.0, accent)
            c.ellipse(15, 8, 6.0, 3.2, _shade(accent, 1.22))
            c.ellipse(16, 18, 7.6, 7.2, skin)          # face opening
            for y in range(14, 27):                    # falling sides
                c.put(5 + (y - 14) // 6, y, acc_dk)
                c.put(6 + (y - 14) // 6, y, accent)
                c.put(25 - (y - 14) // 6, y, accent)
                c.put(26 - (y - 14) // 6, y, acc_dk)

        # Brow bar before the eyes, the way the Warden's icon builds a face:
        # a flat 2x2 eye on a flat cheek reads as a doll at 1x.
        for x in list(range(11, 15)) + list(range(18, 22)):
            c.put(x, 15, palette.OUTLINE)              # brow
            c.put(x, 16, skin_dk)                      # socket shade
        for x in (12, 13, 19, 20):
            c.put(x, 17, eye)                          # iris
            c.put(x, 18, (46, 42, 54))                 # lash line
        c.put(12, 17, _shade(eye, 1.5))                # catchlight, left eye
        c.put(19, 17, _shade(eye, 1.5))
        c.rect(15, 17, 2, 3, skin)                     # nose bridge
        c.rect(15, 20, 2, 1, skin_dk)                  # nose tip
        for x in range(13, 20):                        # mouth
            c.put(x, 23, skin_dk)
        c.put(13, 23, palette.OUTLINE)
        c.put(19, 23, palette.OUTLINE)
        for x in (10, 11, 21, 22):                     # cheek shade
            c.put(x, 21, skin_dk)
        c.outline(palette.OUTLINE)
        c.save(f"{dir_path}/{name}.png")
