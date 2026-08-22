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
    """32x32 item icons for the placeable set pieces (miniatures)."""
    from PIL import Image
    import os
    # Candelabra: crop+scale the lit state
    def mini_from(src_path, box, out_name):
        im = Image.open(src_path).convert("RGBA").crop(box)
        w, h = im.size
        scale = min(28 / w, 28 / h)
        nw, nh = max(1, int(w * scale)), max(1, int(h * scale))
        im = im.resize((nw, nh), Image.NEAREST)
        icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        icon.alpha_composite(im, ((32 - nw) // 2, (32 - nh) // 2))
        icon.save(f"{dir_path}/{out_name}")
    # these read the freshly generated object sprites
    base = os.path.dirname(dir_path)
    mini_from(f"{base}/objects/wardencandelabra.png", (0, 8, 32, 96), "wardencandelabra.png")
    mini_from(f"{base}/objects/gloomwillow.png", (0, 16, 48, 80), "gloomwillow.png")
    mini_from(f"{base}/objects/skywatchbanner.png", (0, 0, 32, 64), "skywatchbanner.png")
    mini_from(f"{base}/objects/ravenstatue.png", (4, 18, 28, 48), "ravenstatue.png")
    mini_from(f"{base}/objects/flickerlightgarland.png", (0, 0, 64, 32), "flickerlightgarland.png")
    mini_from(f"{base}/objects/catbasket.png", (0, 6, 32, 30), "catbasket.png")
    mini_from(f"{base}/objects/mistglasslantern.png", (0, 0, 32, 32), "mistglasslantern.png")
