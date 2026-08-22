"""Nightfell & Skylight building-set sprites.

Formats verified against vanilla references:
- streetlamp: 32 wide x 192 tall = two 96px states stacked (ON above, OFF below)
- small statue (freyastatue-class size): 32-64 wide, tall, bottom-anchored
- simple deco objects: 32xH bottom-anchored columns
"""

from px import Canvas, Rng, with_alpha
import palette


# --- Warden's Candelabra (streetlamp format) --------------------------------

def _candelabra_state(lit):
    c = Canvas(32, 96)
    iron = palette.IRONWORK
    glow = palette.STAIRLIGHT
    base_y = 90
    cx = 16
    # stone foot: chunky two-step plinth
    c.rect(cx - 7, base_y - 3, 14, 4, palette.SKYSTONE["base"])
    c.rect(cx - 7, base_y - 3, 14, 1, palette.SKYSTONE["light"])
    c.rect(cx - 5, base_y - 6, 10, 3, palette.SKYSTONE["base"])
    c.rect(cx - 5, base_y - 6, 10, 1, palette.SKYSTONE["light"])
    # solid wrought post: 3px with lit left edge
    for y in range(26, base_y - 6):
        c.put(cx - 1, y, iron["light"])
        c.put(cx, y, iron["base"])
        c.put(cx + 1, y, iron["deep"])
    # collar rings
    for ry in (38, 58, 76):
        for dx in range(-3, 4):
            c.put(cx + dx, ry, iron["hi"] if abs(dx) < 2 else iron["base"])
    # crown: crossbar with three cup arms
    for dx in range(-9, 10):
        c.put(cx + dx, 26, iron["base"])
        c.put(cx + dx, 25, iron["light"] if dx % 3 else iron["base"])
    for side in (-1, 1):  # curled arm ends
        c.put(cx + side * 10, 25, iron["deep"])
        c.put(cx + side * 10, 24, iron["base"])
    # three sockets (cups)
    cups = ((cx - 8, 23), (cx, 21), (cx + 8, 23))
    for fx, fy in cups:
        c.rect(fx - 2, fy, 5, 2, iron["base"])
        c.put(fx - 2, fy, iron["light"])
        c.put(fx + 2, fy + 1, iron["deep"])
    c.outline(palette.OUTLINE)
    # flames after outline
    for fx, fy in cups:
        if lit:
            c.put(fx, fy - 1, glow["hi"])
            c.put(fx, fy - 2, glow["hi"])
            c.put(fx - 1, fy - 1, glow["glow"])
            c.put(fx + 1, fy - 1, glow["glow"])
            c.put(fx, fy - 3, glow["glow"])
            c.put(fx - 1, fy - 3, with_alpha(glow["glow"], 150))
            c.put(fx + 1, fy - 3, with_alpha(glow["glow"], 150))
        else:
            c.put(fx, fy - 1, iron["deep"])
    return c


def gen_candelabra(path):
    sheet = Canvas(32, 192)
    sheet.paste(_candelabra_state(True), 0, 0)
    sheet.paste(_candelabra_state(False), 0, 96)
    sheet.save(path)


# --- Raven statue (small statue, bottom-anchored 32x48) ---------------------

def gen_ravenstatue(path):
    c = Canvas(32, 48)
    stone = palette.SKYSTONE
    # plinth
    c.rect(8, 40, 16, 6, stone["base"])
    c.rect(8, 40, 16, 2, stone["light"])
    c.rect(8, 45, 16, 1, stone["deep"])
    # raven: hunched silhouette facing left
    c.ellipse(16, 32, 6, 5, stone["deep"])      # body
    c.ellipse(13, 26, 3.4, 3, stone["deep"])    # head
    c.put(9, 26, stone["deep"])                  # beak
    c.put(8, 26, stone["base"])
    # folded wing line + tail
    for i in range(5):
        c.put(15 + i, 30 + i // 2, stone["base"])
    for i in range(4):
        c.put(21 + i, 35 + i // 3, stone["deep"])
    # feet on plinth
    c.put(14, 39, stone["deep"])
    c.put(18, 39, stone["deep"])
    c.outline(palette.OUTLINE)
    c.put(13, 25, palette.STORMCRYSTAL["light"])  # faint gem eye
    c.save(path)


# --- Gloomwillow (crooked bare tree, bottom-anchored 48x80) -----------------

def gen_gloomwillow(path, variants=2):
    sheet = Canvas(variants * 48, 80)
    wood = palette.GLOOMWOOD
    for v in range(variants):
        c = Canvas(48, 80)
        rng = Rng(0x610C + v * 313)
        base_x = 22 + rng.pick((-2, 2))
        base_y = 76
        # thick crooked trunk: tapering 6->3px, with two hard kinks
        x = base_x
        kink1 = rng.range(50, 58)
        kink2 = rng.range(32, 40)
        for y in range(base_y, 24, -1):
            if y == kink1:
                x += rng.pick((-3, 3))
            if y == kink2:
                x += rng.pick((-4, 4))
            if y % 9 == 0:
                x += rng.pick((-1, 0, 1))
            x = max(8, min(38, x))
            t = (base_y - y) / (base_y - 24)
            width = max(3, round(6 - 3 * t))
            for dx in range(width):
                tone = wood["light"] if dx == 0 else (wood["deep"] if dx >= width - 1 else wood["base"])
                c.put(x + dx, y, tone)
            # bark notches
            if y % 6 == 0 and width > 3:
                c.put(x + rng.range(1, width - 2), y, wood["deep"])
        top_x, top_y = x + 1, 24
        # gnarled branches: 2px crooked limbs ending in claw twigs
        for (ox, oy, ln, lean) in ((-1, 4, 11, -2), (2, 0, 10, 3), (0, 9, 8, -3)):
            bx, by = top_x + ox, top_y + oy
            for i in range(ln):
                bx += lean if i % 3 == 0 else (1 if lean > 0 else -1) * (i % 2)
                by -= 1
                bx = max(2, min(45, bx))
                c.put(bx, by, wood["base"])
                c.put(bx + 1, by, wood["deep"])
            # claw fork at the tip
            c.put(bx - 1, by - 1, wood["deep"])
            c.put(bx - 2, by - 2, wood["deep"])
            c.put(bx + 2, by - 1, wood["deep"])
            c.put(bx + 3, by - 2, wood["deep"])
        # flared roots
        for side in (-1, 1):
            for i in range(5):
                c.put(base_x + (2 if side > 0 else 0) + side * (2 + i), base_y - i // 3, wood["deep"])
                c.put(base_x + (2 if side > 0 else 0) + side * (2 + i), base_y + 1 - i // 3, wood["base"])
        # one perched tiny raven on variant 0
        if v == 0:
            c.ellipse(top_x + 4, top_y + 2, 2.4, 2, (20, 19, 26))
            c.put(top_x + 2, top_y + 1, (20, 19, 26))
            c.put(top_x + 1, top_y + 1, (44, 43, 52))
        c.outline(palette.OUTLINE)
        sheet.paste(c, v * 48, 0)
    sheet.save(path)


# --- Flickerlight Garland (wall-mounted string of lights, 64x32) ------------

def gen_garland(path):
    c = Canvas(64, 32)
    wire = palette.GLOOMWOOD
    # sagging wire across two tiles
    prev = 8
    for x in range(2, 62):
        t = (x - 2) / 60.0
        y = int(8 + 10 * (4 * t * (1 - t)))
        c.put(x, y, wire["deep"])
        prev = y
    # bulbs at intervals, alternating colors
    rng = Rng(0x11F5)
    for i, x in enumerate(range(6, 62, 7)):
        t = (x - 2) / 60.0
        y = int(8 + 10 * (4 * t * (1 - t))) + 2
        color = palette.GARLAND_LIGHTS[i % len(palette.GARLAND_LIGHTS)]
        c.put(x, y, color)
        c.put(x, y + 1, tuple(int(v * 0.7) for v in color))
        c.put(x, y - 1, wire["deep"])
        if rng.chance(0.6):
            c.put(x + 1, y, with_alpha(color, 130))
            c.put(x - 1, y, with_alpha(color, 130))
    # mounting hooks
    c.put(2, 7, palette.IRONWORK["light"])
    c.put(61, 7, palette.IRONWORK["light"])
    c.save(path)


# --- Cat basket (32x32 floor deco) ------------------------------------------

def gen_catbasket(path):
    c = Canvas(32, 32)
    wick = palette.WOOD
    # oval woven basket
    c.ellipse(16, 20, 11, 7, wick["base"])
    c.ellipse(16, 19, 8.5, 5, wick["deep"])
    c.ellipse(16, 20, 8, 4.5, (120, 100, 80))
    # weave texture on the rim
    for i, x in enumerate(range(6, 27, 2)):
        y = 14 + abs(11 - i) // 3
        c.put(x, y + 6 - 6, wick["light"] if i % 2 else wick["deep"])
    # cushion inside
    c.ellipse(16, 20, 6.5, 3.5, palette.AURORA["light"])
    c.ellipse(15, 19, 4, 2, palette.AURORA["hi"])
    c.outline(palette.OUTLINE)
    c.save(path)


# --- Skywatch banner (wall-mounted, 32x64) ----------------------------------

def gen_banner(path):
    c = Canvas(32, 64)
    cloth_deep = palette.NIGHTFELL["deep"]
    cloth = palette.NIGHTFELL["base"]
    trim = palette.STAIRLIGHT["glow"]
    # hanging rod
    for x in range(4, 28):
        c.put(x, 6, palette.IRONWORK["base"])
    c.put(3, 6, palette.IRONWORK["light"])
    c.put(28, 6, palette.IRONWORK["light"])
    # cloth: flat fill, darker side folds, light trim borders
    for y in range(7, 52):
        for x in range(7, 25):
            tone = cloth
            if x in (7, 24):
                tone = cloth_deep
            elif x in (8, 23):
                tone = palette.NIGHTFELL["light"]
            c.put(x, y, tone)
    # symmetric swallowtail: two tapering points with a notch in the middle
    for i in range(6):
        for x in range(7, 14 - i):
            c.put(x, 52 + i, cloth if x > 7 else cloth_deep)
        for x in range(18 + i, 25):
            c.put(x, 52 + i, cloth if x < 24 else cloth_deep)
    # emblem: the stairway sigil — three ascending steps + star
    for (sx, sy) in ((10, 40), (13, 34), (16, 28)):
        for dx in range(6):
            c.put(sx + dx, sy, trim)
            c.put(sx + dx, sy + 1, palette.STAIRLIGHT["base"])
    c.put(21, 21, palette.STAIRLIGHT["hi"])
    c.put(20, 22, trim)
    c.put(22, 22, trim)
    c.put(21, 23, trim)
    c.outline(palette.OUTLINE)
    c.save(path)


# --- Mistglass lantern (wall lamp, 32x32) -----------------------------------

def gen_mistglasslantern(path):
    sheet = Canvas(64, 32)  # col 0 = lit, col 1 = unlit
    for col, lit in ((0, True), (1, False)):
        c = Canvas(32, 32)
        iron = palette.IRONWORK
        glow = palette.STAIRLIGHT
        # bracket
        for y in range(6, 12):
            c.put(15, y, iron["base"])
        c.put(15, 5, iron["light"])
        # glass housing
        c.rect(11, 12, 10, 12, iron["deep"])
        c.rect(12, 13, 8, 10, glow["glow"] if lit else (60, 66, 78))
        if lit:
            c.rect(14, 15, 4, 5, glow["hi"])
        c.put(15, 24, iron["base"])
        c.put(16, 25, iron["deep"])
        c.outline(palette.OUTLINE)
        sheet.paste(c, col * 32, 0)
    sheet.save(path)


# --- Item icons for v0.2 -----------------------------------------------------

def gen_v2_item_icons(dir_path):
    from px import Canvas, Rng
    # Cloudpuff Treat: a fluffy cloud-morsel with petal sprinkle
    c = Canvas(32, 32)
    mist = palette.MISTSEA
    rng = Rng(0x7EA7)
    c.blob(15, 19, 7, mist["light"], rng)
    c.blob(20, 17, 4, mist["hi"], rng)
    c.blob(11, 16, 3.5, mist["hi"], rng)
    c.put(14, 14, palette.AURORA["light"])
    c.put(19, 21, palette.AURORA["light"])
    c.put(16, 18, palette.AURORA["teal"])
    c.outline(palette.OUTLINE)
    c.save(f"{dir_path}/cloudpufftreat.png")

    # Silver Bell: small bell with windsilk ribbon
    c = Canvas(32, 32)
    iron = palette.IRONWORK
    silk = palette.WINDSILK
    for y in range(12, 22):
        t = (y - 12) / 9.0
        half = round(3 + 5 * t)
        for dx in range(-half, half + 1):
            tone = iron["hi"] if dx < -half + 2 else (iron["light"] if dx < half - 1 else iron["base"])
            c.put(16 + dx, y, tone)
    c.rect(8, 22, 17, 2, iron["base"])
    c.put(8, 22, iron["light"])
    c.put(16, 25, iron["deep"])  # clapper
    c.put(16, 26, iron["base"])
    # ribbon bow on top
    c.put(15, 10, silk["light"])
    c.put(17, 10, silk["light"])
    c.put(16, 11, silk["base"])
    c.put(14, 9, silk["base"])
    c.put(18, 9, silk["base"])
    c.outline(palette.OUTLINE)
    c.put(13, 15, (246, 249, 252))  # glint
    c.save(f"{dir_path}/silverbell.png")


def gen_set_icons(dir_path):
    """32x32 item icons for the placeable set pieces (miniatures cropped and
    scaled from the freshly generated object sprites)."""
    from PIL import Image
    import os

    def mini_from(src_path, box, out_name):
        im = Image.open(src_path).convert("RGBA").crop(box)
        w, h = im.size
        scale = min(28 / w, 28 / h, 1.0)
        nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
        im = im.resize((nw, nh), Image.NEAREST)
        icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        icon.alpha_composite(im, ((32 - nw) // 2, (32 - nh) // 2))
        icon.save(f"{dir_path}/{out_name}")

    base = os.path.dirname(dir_path)
    obj = f"{base}/objects"
    mini_from(f"{obj}/wardencandelabra.png", (0, 8, 32, 96), "wardencandelabra.png")
    mini_from(f"{obj}/gloomwillow.png", (0, 16, 48, 80), "gloomwillow.png")
    mini_from(f"{obj}/catbasket.png", (0, 6, 32, 30), "catbasket.png")
    mini_from(f"{obj}/mistglasslantern.png", (0, 0, 32, 32), "mistglasslantern.png")
    mini_from(f"{obj}/flickerlightgarland.png", (0, 0, 32, 32), "flickerlightgarland.png")
    mini_from(f"{obj}/skywatchbanner.png", (0, 0, 32, 32), "skywatchbanner.png")
    mini_from(f"{base}/objects/statues/gloomraven.png", (8, 34, 56, 92), "gloomravenstatue.png")
    # walls: crop a front-face piece; doors: rotation-0 closed leaf
    for wall in ("skystonebrickwall", "nightfellwall"):
        mini_from(f"{obj}/{wall}.png", (0, 64, 32, 96), f"{wall}.png")
    mini_from(f"{obj}/skystonebrickwall.png", (96, 24, 128, 120), "skystonebrickdoor.png")
    mini_from(f"{obj}/nightfellwall.png", (96, 24, 128, 120), "nightfelldoor.png")
    mini_from(f"{obj}/skyironfence.png", (0, 20, 32, 64), "skyironfence.png")
    mini_from(f"{obj}/skyironfencegate.png", (32, 20, 64, 64), "skyironfencegate.png")


# =====================================================================
# v0.2 format-correct pieces (fence, gate, wall lights 64x128, statue
# 64x96, painting-banner 32x128, beacon, anchor, checker floor)
# =====================================================================

def _iron_post(c, x, base_y, height, spike=True):
    iron = palette.IRONWORK
    for y in range(base_y - height, base_y):
        c.put(x, y, iron["light"])
        c.put(x + 1, y, iron["base"])
        c.put(x + 2, y, iron["deep"])
    if spike:
        c.put(x + 1, base_y - height - 1, iron["base"])
        c.put(x + 1, base_y - height - 2, iron["hi"])


def _iron_rails(c, x0, x1, base_y):
    iron = palette.IRONWORK
    for rail_y in (base_y - 18, base_y - 8):
        for x in range(x0, x1):
            c.put(x, rail_y, iron["base"])
            c.put(x, rail_y + 1, iron["deep"])
    # pickets between the rails, spiked
    for x in range(x0 + 2, x1, 6):
        for y in range(base_y - 20, base_y - 2):
            c.put(x, y, iron["deep"])
        c.put(x, base_y - 21, iron["light"])
        c.put(x, base_y - 22, iron["hi"])


def gen_skyironfence(path):
    """160x64: post / horizontal run / top cap / left connector / right connector."""
    sheet = Canvas(160, 64)
    base_y = 58
    # col 0: freestanding post
    c = Canvas(32, 64)
    _iron_post(c, 14, base_y, 30)
    c.ellipse(15, base_y, 5, 2, palette.IRONWORK["deep"])
    c.outline(palette.OUTLINE)
    sheet.paste(c, 0, 0)
    # col 1: horizontal run (rails across the full cell)
    c = Canvas(32, 64)
    _iron_rails(c, 0, 32, base_y)
    c.outline(palette.OUTLINE)
    sheet.paste(c, 32, 0)
    # col 2: top cap (post with a downward connection stub)
    c = Canvas(32, 64)
    _iron_post(c, 14, base_y, 30)
    for y in range(base_y - 14, base_y - 4):
        c.put(15, y + 4, palette.IRONWORK["deep"])
    c.outline(palette.OUTLINE)
    sheet.paste(c, 64, 0)
    # col 3: left connector (rails entering from the right, ending in a post)
    c = Canvas(32, 64)
    _iron_rails(c, 12, 32, base_y)
    _iron_post(c, 10, base_y, 30)
    c.outline(palette.OUTLINE)
    sheet.paste(c, 96, 0)
    # col 4: right connector (mirror)
    sheet.paste(c.mirrored(), 128, 0)
    sheet.save(path)


def gen_skyironfencegate(path):
    """192x64: 6 columns — frame post, closed leaf, open leaf, then the
    vertical-orientation trio."""
    sheet = Canvas(192, 64)
    base_y = 58
    iron = palette.IRONWORK
    # col 0: gate post pair
    c = Canvas(32, 64)
    _iron_post(c, 4, base_y, 32)
    _iron_post(c, 25, base_y, 32)
    c.outline(palette.OUTLINE)
    sheet.paste(c, 0, 0)
    # col 1: closed leaf between posts
    c = Canvas(32, 64)
    _iron_post(c, 2, base_y, 32)
    _iron_post(c, 27, base_y, 32)
    _iron_rails(c, 5, 27, base_y)
    c.put(16, base_y - 13, iron["hi"])
    c.outline(palette.OUTLINE)
    sheet.paste(c, 32, 0)
    # col 2: open (leaves swung back, posts only + hinge stubs)
    c = Canvas(32, 64)
    _iron_post(c, 2, base_y, 32)
    _iron_post(c, 27, base_y, 32)
    for x in (5, 26):
        c.put(x, base_y - 16, iron["base"])
    c.outline(palette.OUTLINE)
    sheet.paste(c, 64, 0)
    # cols 3-5: vertical orientation (posts top/bottom, leaf runs vertically)
    for i, mode in enumerate(("post", "closed", "open")):
        c = Canvas(32, 64)
        _iron_post(c, 14, 24, 16)
        _iron_post(c, 14, base_y + 2, 16)
        if mode == "closed":
            for y in range(14, base_y - 8):
                c.put(14, y, iron["deep"])
                c.put(16, y, iron["deep"])
            for y in range(16, base_y - 10, 8):
                c.put(15, y, iron["base"])
        elif mode == "open":
            c.put(12, 20, iron["base"])
            c.put(18, base_y - 6, iron["base"])
        c.outline(palette.OUTLINE)
        sheet.paste(c, 96 + i * 32, 0)
    sheet.save(path)


def _wall_light_cell(kind, orientation, lit):
    """32x32 cell for WallTorchObject sheets. orientation: 0 hang-from-top,
    1 mounted-on-right-wall, 2 standing-on-support, 3 mounted-on-left-wall."""
    c = Canvas(32, 32)
    iron = palette.IRONWORK
    glow = palette.STAIRLIGHT
    if kind == "lantern":
        cx, cy = 16, 16
        if orientation == 0:
            for y in range(2, 9):
                c.put(cx, y, iron["base"])
            cy = 15
        elif orientation == 2:
            for y in range(24, 30):
                c.put(cx, y, iron["base"])
            cy = 17
        else:
            bx = 29 if orientation == 1 else 2
            step = -1 if orientation == 1 else 1
            for i in range(6):
                c.put(bx + step * i, 12, iron["base"])
            cy = 16
        c.rect(cx - 5, cy - 6, 10, 12, iron["deep"])
        c.rect(cx - 4, cy - 5, 8, 10, glow["glow"] if lit else (60, 66, 78))
        if lit:
            c.rect(cx - 2, cy - 3, 4, 5, glow["hi"])
        c.put(cx, cy - 7, iron["light"])
        c.outline(palette.OUTLINE)
    else:  # garland
        wire = palette.GLOOMWOOD
        pts = []
        if orientation in (0, 2):
            yb = 6 if orientation == 0 else 20
            for x in range(2, 30):
                t = (x - 2) / 28.0
                y = int(yb + 9 * (4 * t * (1 - t)))
                c.put(x, y, wire["deep"])
                pts.append((x, y))
            c.put(2, yb - 1, iron["light"])
            c.put(29, yb - 1, iron["light"])
        else:
            xb = 25 if orientation == 1 else 6
            side = -1 if orientation == 1 else 1
            for y in range(2, 30):
                t = (y - 2) / 28.0
                x = int(xb + side * 8 * (4 * t * (1 - t)))
                c.put(x, y, wire["deep"])
                pts.append((x, y))
        for i, (px_, py_) in enumerate(pts[3::5]):
            color = palette.GARLAND_LIGHTS[i % len(palette.GARLAND_LIGHTS)]
            if not lit:
                color = tuple(int(v * 0.35) for v in color)
            off = (0, 2) if orientation in (0, 2) else ((2, 0) if orientation == 3 else (-2, 0))
            c.put(px_ + off[0], py_ + off[1], color)
            if lit:
                c.put(px_ + off[0], py_ + off[1] + (1 if orientation in (0, 2) else 0),
                      tuple(int(v * 0.7) for v in color))
    return c


def gen_wall_light(path, kind):
    """64x128 WallTorchObject sheet: 2 cols (lit/unlit) x 4 orientation rows."""
    sheet = Canvas(64, 128)
    for row in range(4):
        sheet.paste(_wall_light_cell(kind, row, True), 0, row * 32)
        sheet.paste(_wall_light_cell(kind, row, False), 32, row * 32)
    sheet.save(path)


def gen_gloomraven_statue(path):
    """64x96 single-pose StatueObject sheet (vanilla ravenstatue dimensions)."""
    c = Canvas(64, 96)
    stone = palette.SKYSTONE
    cx = 32
    # two-step plinth
    c.rect(cx - 14, 82, 28, 8, stone["base"])
    c.rect(cx - 14, 82, 28, 2, stone["light"])
    c.rect(cx - 10, 76, 20, 6, stone["base"])
    c.rect(cx - 10, 76, 20, 2, stone["light"])
    # raven, hunched, facing left, tail sweeping right
    c.ellipse(cx + 1, 62, 12, 10, stone["base"])
    c.ellipse(cx - 6, 48, 7, 6, stone["base"])
    c.ellipse(cx - 3, 58, 8, 7, stone["light"])   # wing mass
    for i in range(7):                            # beak
        c.put(cx - 14 - i // 2, 48 + i // 3, stone["deep"])
    for i in range(10):                           # tail
        c.put(cx + 12 + i // 2, 66 + i // 2, stone["deep"])
    for lx in (cx - 4, cx + 5):                   # legs
        c.rect(lx, 72, 2, 5, stone["deep"])
    c.shade_topleft(stone["hi"], stone["deep"])
    c.outline(palette.OUTLINE)
    c.put(cx - 8, 46, palette.STORMCRYSTAL["light"])  # gem eye
    c.save(path)


def gen_banner_painting(path):
    """32x128 PaintingObject sheet: 4 rotation rows of one 32x32 banner."""
    def banner_cell():
        c = Canvas(32, 32)
        cloth = palette.NIGHTFELL["base"]
        cloth_deep = palette.NIGHTFELL["deep"]
        trim = palette.STAIRLIGHT["glow"]
        for x in range(6, 26):
            c.put(x, 3, palette.IRONWORK["base"])
        for y in range(4, 24):
            for x in range(8, 24):
                tone = cloth
                if x in (8, 23):
                    tone = cloth_deep
                c.put(x, y, tone)
        for i in range(4):
            for x in range(8, 15 - i):
                c.put(x, 24 + i, cloth)
            for x in range(17 + i, 24):
                c.put(x, 24 + i, cloth)
        for (sx, sy) in ((11, 18), (13, 14), (15, 10)):
            for dx in range(4):
                c.put(sx + dx, sy, trim)
        c.put(19, 7, palette.STAIRLIGHT["hi"])
        c.outline(palette.OUTLINE)
        return c
    sheet = Canvas(32, 128)
    cell = banner_cell()
    for row in range(4):
        sheet.paste(cell, 0, row * 32)
    sheet.save(path)


def gen_beacon(path, lit):
    """32x96 bottom-anchored beacon pylon (SkyDecoObject)."""
    c = Canvas(32, 96)
    stone = palette.SKYSTONE
    glow = palette.STAIRLIGHT
    # base + tapering pylon
    c.rect(8, 86, 16, 6, stone["base"])
    c.rect(8, 86, 16, 2, stone["light"])
    for y in range(40, 86):
        t = (y - 40) / 46.0
        half = round(3 + 3 * t)
        for dx in range(-half, half + 1):
            tone = stone["base"]
            if dx == -half:
                tone = stone["light"]
            elif dx == half:
                tone = stone["deep"]
            c.put(16 + dx, y, tone)
    # crown cradle
    c.rect(9, 34, 14, 6, palette.IRONWORK["base"])
    c.rect(9, 34, 14, 2, palette.IRONWORK["light"])
    c.outline(palette.OUTLINE)
    if lit:
        # cold steady wardenlight
        c.ellipse(16, 26, 6, 7, glow["glow"])
        c.ellipse(16, 25, 3.5, 4.5, glow["hi"])
        c.put(16, 18, glow["glow"])
        c.put(13, 20, with_alpha(glow["glow"], 160))
        c.put(19, 21, with_alpha(glow["glow"], 160))
    else:
        # dead shattered crystal stub
        for (sx, sy, h) in ((13, 33, 4), (17, 33, 6), (20, 33, 3)):
            for i in range(h):
                c.put(sx, sy - i, palette.STORMCRYSTAL["deep"])
            c.put(sx, sy - h, palette.STORMCRYSTAL["base"])
    c.save(path)


def gen_skyanchor(path):
    """32x64 reforged island anchor (SkyDecoObject)."""
    c = Canvas(32, 64)
    a = palette.AETHERIUM
    iron = palette.IRONWORK
    # ring
    for ang in range(0, 360, 12):
        import math
        x = 16 + int(6.5 * math.cos(math.radians(ang)))
        y = 16 + int(6.5 * math.sin(math.radians(ang)))
        c.put(x, y, iron["light"])
    # shank
    for y in range(22, 50):
        c.put(15, y, a["light"])
        c.put(16, y, a["base"])
        c.put(17, y, a["deep"])
    # flukes
    for i in range(9):
        c.put(15 - i, 50 - i // 2, a["base"])
        c.put(16 + i, 50 - i // 2, a["deep"])
    c.put(5, 44, a["hi"])
    c.put(26, 44, a["hi"])
    # crossbar
    for x in range(9, 24):
        c.put(x, 28, iron["base"])
    c.outline(palette.OUTLINE)
    c.put(16, 24, a["hi"])
    c.save(path)


def gen_marblechecker(path):
    """64x64 legacy tile for SimpleTiledFloorTile: 2x2 world-locked checker."""
    c = Canvas(64, 64)
    from px import Rng
    for x in range(64):
        for y in range(64):
            dark = (x // 32 + y // 32) % 2 == 0
            c.put(x, y, palette.MARBLE_DARK if dark else palette.MARBLE_LIGHT)
    rng = Rng(0xC4EC)
    for _ in range(14):
        x = rng.range(2, 60)
        y = rng.range(2, 60)
        dark = (x // 32 + y // 32) % 2 == 0
        vein = (74, 72, 84) if dark else (198, 196, 204)
        for i in range(rng.range(2, 5)):
            c.put(x + i, y + i // 2, vein)
    c.save(path)
