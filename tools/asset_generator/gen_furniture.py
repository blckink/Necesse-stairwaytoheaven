"""Nightfell & Skylight building-set sprites.

Formats verified against vanilla references:
- streetlamp: 32 wide x 192 tall = two 96px states stacked (ON above, OFF below)
- small statue (freyastatue-class size): 32-64 wide, tall, bottom-anchored
- simple deco objects: 32xH bottom-anchored columns
"""

import math

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
    c.rect(cx - 9, base_y - 4, 18, 5, palette.SKYSTONE["base"])
    c.rect(cx - 9, base_y - 4, 18, 1, palette.SKYSTONE["light"])
    c.rect(cx - 9, base_y, 18, 1, palette.SKYSTONE["deep"])
    c.rect(cx - 6, base_y - 8, 12, 4, palette.SKYSTONE["base"])
    c.rect(cx - 6, base_y - 8, 12, 1, palette.SKYSTONE["light"])
    c.rect(cx - 4, base_y - 11, 8, 3, palette.SKYSTONE["base"])
    c.rect(cx - 4, base_y - 11, 8, 1, palette.SKYSTONE["light"])
    # solid wrought post: 3px with lit left edge
    for y in range(26, base_y - 10):
        c.put(cx - 3, y, iron["light"])
        c.put(cx - 2, y, iron["light"])
        c.put(cx - 1, y, iron["base"])
        c.put(cx, y, iron["base"])
        c.put(cx + 1, y, iron["base"])
        c.put(cx + 2, y, iron["deep"])
        c.put(cx + 3, y, iron["deep"])
    # collar rings
    for ry in (38, 58, 76):
        for dx in range(-5, 6):
            c.put(cx + dx, ry, iron["hi"] if abs(dx) < 3 else iron["base"])
            c.put(cx + dx, ry + 1, iron["base"] if abs(dx) < 4 else iron["deep"])
    # crown: crossbar with three cup arms
    for dx in range(-11, 12):
        c.put(cx + dx, 27, iron["deep"])
        c.put(cx + dx, 26, iron["base"])
        c.put(cx + dx, 25, iron["light"] if dx % 3 else iron["base"])
    for side in (-1, 1):  # curled arm ends rising to the outer cups
        for dy in range(0, 4):
            c.put(cx + side * 11, 25 - dy, iron["base"])
            c.put(cx + side * 12, 25 - dy, iron["deep"])
    # three sockets (cups)
    # central glass housing: the lamp head carries most of the visual mass
    c.rect(cx - 5, 8, 11, 12, iron["base"])
    c.rect(cx - 4, 9, 9, 10, palette.STAIRLIGHT["glow"])
    c.rect(cx - 5, 8, 11, 1, iron["light"])
    c.rect(cx - 5, 19, 11, 1, iron["deep"])
    for hx in (cx - 5, cx, cx + 5):
        for hy in range(9, 19):
            c.put(hx, hy, iron["base"])
    c.rect(cx - 7, 5, 15, 3, iron["base"])          # roof cap
    c.rect(cx - 7, 5, 15, 1, iron["light"])
    c.rect(cx - 2, 2, 5, 3, iron["base"])           # finial
    cups = ((cx - 11, 21), (cx + 11, 21))
    for fx, fy in cups:
        c.rect(fx - 3, fy, 7, 3, iron["base"])
        c.rect(fx - 3, fy, 7, 1, iron["light"])
        c.rect(fx - 3, fy + 2, 7, 1, iron["deep"])
        c.rect(fx - 1, fy - 2, 3, 2, palette.STAIRLIGHT["light"])   # wax stub
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
    # Size law: this deco tree measured 351 opaque px against a vanilla dead
    # tree's ~4600 — it read as a twig. Grown to a 64x112 cell (variantWidth
    # 64 on the Java side) with a heavier trunk and a fuller weeping crown.
    sheet = Canvas(variants * 64, 112)
    wood = palette.GLOOMWOOD
    for v in range(variants):
        c = Canvas(64, 112)
        rng = Rng(0x610C + v * 313)
        base_x = 30 + rng.pick((-2, 2))
        base_y = 106
        top_y = 22
        H = base_y - top_y
        # soft ground shadow so the tree sits in the world
        c.ellipse(base_x + 4, base_y + 3, 14, 3, with_alpha((20, 19, 26), 80))
        # crooked trunk as one CONTINUOUS S-curve (no hard kinks), taper 6->2
        amp = rng.pick((3.5, 4.2, 5.0))
        ph = rng.float() * 3.0
        drift = rng.pick((-6, -5, 5, 6))
        path_x = {}
        for y in range(base_y, top_y - 1, -1):
            t = (base_y - y) / H
            x = base_x + round(amp * math.sin(t * 3.2 + ph) + drift * t)
            path_x[y] = x
            width = max(4, round(10 - 5.5 * t))
            for dx in range(width):
                tone = wood["light"] if dx == 0 else (wood["deep"] if dx >= width - 1 else wood["base"])
                c.put(x + dx, y, tone)
            # bark notches + a rare sheen fleck
            if y % 6 == 0 and width > 3:
                c.put(x + rng.range(1, width - 2), y, wood["deep"])
            if y % 9 == 4 and width > 4:
                c.put(x + 1, y, wood["hi"])
        top_x = path_x[top_y] + 1
        # branches: WEEPING curved limbs (per-step turn keeps them
        # 8-connected) that open outward and droop, each with a claw twig
        perch = (top_x + 4, top_y + 2)
        for bi, (start_dy, ang0, turn, ln) in enumerate(
                ((1, 115, 6, 20), (4, 62, -6, 19), (9, 95, 4, 15),
                 (14, 48, -5, 14), (20, 132, 6, 12), (30, 70, -6, 11),
                 (40, 148, -8, 9))):              # low upturned snag
            fx = float(path_x[top_y + start_dy] + 1)
            fy = float(top_y + start_dy)
            ang = ang0 + rng.range(-8, 8)
            for i in range(ln):
                ang += turn * (2.5 if i >= ln - 2 else 1)   # hooked tip
                fx += math.cos(math.radians(ang))
                fy -= math.sin(math.radians(ang))
                x, yy = round(fx), round(fy)
                c.put(x, yy, wood["base"])
                if i < ln * 0.6:
                    c.put(x + 1, yy, wood["deep"])
                if bi == 1 and i == 3:
                    perch = (x + 1, yy - 1)       # the raven sits here
                if ln > 6 and i in (round(ln * 0.45), round(ln * 0.8)):
                    sx, sy = fx, fy
                    sang = ang + (55 if i == round(ln * 0.45) else -50)
                    for _k in range(3 + rng.range(0, 2)):
                        sx += math.cos(math.radians(sang))
                        sy -= math.sin(math.radians(sang))
                        c.put(round(sx), round(sy), wood["deep"])
                # weeping veil: hanging strands make this read as a WILLOW
                # (and carry the mass a bare frame was missing)
                if i >= 3 and i % 3 == 0:
                    vx = float(x)
                    for k in range(rng.range(7, 15)):
                        vy = yy + 1 + k
                        if vy > base_y - 2:
                            break
                        vx += 0.35 * math.sin((k + i) * 0.5)
                        vxi = round(vx)
                        tone = wood["base"] if k < 3 else wood["deep"]
                        c.put(vxi, vy, tone)
                        if k % 2 == 0:
                            c.put(vxi + 1, vy, wood["deep"])
        # flared roots
        for side in (-1, 1):
            for i in range(9):
                rx = base_x + (4 if side > 0 else 0) + side * (3 + i)
                c.put(rx, base_y - i // 3, wood["deep"])
                c.put(rx, base_y + 1 - i // 3, wood["base"])
                c.put(rx, base_y + 2 - i // 3, wood["deep"])
        # one perched tiny raven on variant 0, sitting on the branch elbow
        if v == 0:
            px_, py_ = perch
            c.ellipse(px_, py_ - 1, 2.4, 2, (20, 19, 26))
            c.put(px_ - 2, py_ - 2, (20, 19, 26))      # head tuck
            c.put(px_ - 3, py_ - 2, (44, 43, 52))      # beak glint
            c.put(px_ + 2, py_ - 3, (20, 19, 26))      # tail tip up
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
        # wall plate + arm bracket (chunky, vanilla wall-light mass)
        c.rect(13, 2, 6, 4, iron["base"])
        c.rect(13, 2, 6, 1, iron["light"])
        for y in range(4, 10):
            c.rect(14, y, 4, 1, iron["base"])
            c.put(14, y, iron["light"])
            c.put(17, y, iron["deep"])
        # glass housing: broad lantern box with cage bars and a roof cap
        c.rect(8, 8, 16, 3, iron["base"])                    # roof
        c.rect(8, 8, 16, 1, iron["light"])
        c.rect(9, 11, 14, 14, iron["deep"])                  # frame
        c.rect(10, 12, 12, 12, glow["glow"] if lit else (60, 66, 78))
        if lit:
            c.rect(13, 14, 6, 8, glow["hi"])
        for bx in (10, 15, 21):                              # cage bars
            for by in range(12, 24):
                c.put(bx, by, iron["base"])
        c.rect(9, 25, 14, 2, iron["base"])                   # base tray
        c.rect(9, 25, 14, 1, iron["light"])
        c.rect(14, 27, 4, 2, iron["deep"])                   # drop finial
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
    # Windows: the pane insert sits at x 64-96 of the wall sheet. Vanilla ships
    # items/stonewindow.png alongside its wall and door, and WallObject's
    # registerWallObjects creates a <prefix>window object for every set -- an ID
    # that appears nowhere in our source, which is how both of these went
    # missing and showed the engine error icon in the crafting menu.
    mini_from(f"{obj}/skystonebrickwall.png", (64, 0, 96, 32), "skystonebrickwindow.png")
    mini_from(f"{obj}/nightfellwall.png", (64, 0, 96, 32), "nightfellwindow.png")
    # Veil pieces: both are craftable and both were falling through to the
    # engine's error icon.
    # The lantern head only: the full 80px post scaled into a 32px icon left a
    # 6px-wide stick (48 opaque px against vanilla copperstreetlamp's 240).
    mini_from(f"{obj}/ghostlantern.png", (6, 14, 27, 52), "ghostlantern.png")
    mini_from(f"{obj}/seancecircle.png", (0, 8, 32, 64), "seancecircle.png")
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
        # Size law: a vanilla wall torch fills ~236 opaque px per cell; the
        # old 10x12 box came to ~127, reading as a pinprick on the wall.
        c.rect(cx - 8, cy - 9, 16, 3, iron["base"])                  # roof cap
        c.rect(cx - 8, cy - 9, 16, 1, iron["light"])
        c.rect(cx - 7, cy - 6, 14, 14, iron["deep"])                 # frame
        c.rect(cx - 6, cy - 5, 12, 12, glow["glow"] if lit else (60, 66, 78))
        if lit:
            c.rect(cx - 3, cy - 3, 6, 8, glow["hi"])
        for bx in (cx - 6, cx - 1, cx + 5):                          # cage bars
            for by in range(cy - 5, cy + 7):
                c.put(bx, by, iron["base"])
        c.rect(cx - 7, cy + 8, 14, 2, iron["base"])                  # base tray
        c.rect(cx - 7, cy + 8, 14, 1, iron["light"])
        c.put(cx, cy - 10, iron["light"])
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
    """64x96 single-pose StatueObject sheet (vanilla ravenstatue dimensions).

    A REAL raven in profile, facing right: hunched shoulders, big pale
    slightly-hooked beak, eye ring, folded wing with layered sheen, long tail
    sweeping down-left — perched on a three-step skystone plinth. Dark
    night-violet body (the mod's gothic direction), stone-pale beak.

    Every bird part is placed relative to PERCH (the plinth's top edge), so
    growing the plinth can never again leave the beak, wing or legs behind.
    """
    c = Canvas(64, 96)
    stone = palette.SKYSTONE
    body = palette.NIGHTFELL
    rng = Rng(0x6B1D)

    PERCH = 62          # y of the column's top face — the bird stands here
    d = PERCH - 76      # shift of the whole bird vs. the original two-step base

    # --- three-step plinth (size law: vanilla statue cells are plinth-heavy) ---
    c.rect(8, 84, 48, 10, stone["base"])            # ground step
    c.rect(8, 84, 48, 2, stone["light"])
    c.rect(8, 92, 48, 2, stone["deep"])
    c.rect(12, 74, 40, 10, stone["base"])           # mid step
    c.rect(12, 74, 40, 2, stone["light"])
    c.rect(12, 82, 40, 2, stone["deep"])
    c.rect(17, PERCH, 30, 12, stone["base"])        # column block
    c.rect(17, PERCH, 30, 2, stone["light"])
    c.rect(17, PERCH, 2, 12, stone["light"])
    c.rect(45, PERCH, 2, 12, stone["deep"])
    for py in range(PERCH + 3, 74, 3):              # masonry courses
        c.rect(19, py, 26, 1, stone["deep"])
    c.put(11, 88, stone["deep"])   # plinth chips
    c.put(52, 87, stone["light"])

    # --- raven silhouette (dark mass first — outline pass eats thin shapes) ---
    # tail: thick wedge from the body's rear, sweeping down-left
    for i in range(14):
        t = 5 - i // 4
        for w in range(max(2, t)):
            c.put(22 - i, 57 + d + i // 2 + w, body["base"])
    # body, chest and head as one overlapping mass (no "two balls" gap)
    c.ellipse(31, 62 + d, 12, 10, body["base"])
    c.ellipse(36, 54 + d, 9, 8.5, body["base"])
    c.ellipse(40, 45 + d, 7.5, 6.5, body["base"])
    # crown + hunched nape
    c.ellipse(37, 42 + d, 6, 4.5, body["base"])

    # --- big pale beak, slightly hooked at the tip ---
    for i in range(10):
        top = 42 + d + i // 4
        bot = 47 + d - i // 5
        for y in range(top, bot + 1):
            c.put(46 + i, y, stone["light"])
    c.put(55, 46 + d, stone["base"])   # hook
    c.put(54, 47 + d, stone["base"])
    for i in range(9):                 # beak shadow seam
        c.put(46 + i, 46 + d - i // 5, stone["base"])

    # --- folded wing over the body ---
    for i in range(14):            # wing edge line
        c.put(29 - i // 2, 52 + d + i, body["deep"])
    for i in range(10):            # upper sheen arc
        c.put(31 - i // 2, 54 + d + i, body["hi"])
    for i in range(7):             # lower sheen arc
        c.put(29 - i // 2, 60 + d + i, body["light"])
    for fx, fy in ((22, 69), (26, 70), (30, 71)):   # feather tips
        c.put(fx, fy + d, body["deep"])
        c.put(fx + 1, fy + d, body["deep"])
        c.put(fx, fy + d - 1, body["light"])

    # --- legs + claws standing ON the plinth top ---
    c.rect(30, 71 + d, 2, 6, body["deep"])
    c.rect(37, 70 + d, 2, 7, body["deep"])
    c.put(32, 76 + d, body["deep"])
    c.put(39, 76 + d, body["deep"])

    c.shade_topleft(body["hi"], body["deep"], strength=0.6)
    c.outline(palette.OUTLINE)

    # --- face details AFTER the outline pass (it would eat them) ---
    c.put(41, 43 + d, stone["hi"])     # eye ring
    c.put(42, 43 + d, stone["hi"])
    c.put(41, 44 + d, stone["hi"])
    c.put(42, 44 + d, body["deep"])    # pupil
    c.put(41, 42 + d, stone["light"])
    c.put(43, 43 + d, body["deep"])
    for i in range(9):             # solid beak top edge, one clean light line
        c.put(46 + i, 42 + d + i // 4, stone["hi"])
    for i in range(14):            # wing fold sheen (readable on the dark mass)
        c.put(30 - i // 2, 53 + d + i, body["hi"])
    c.put(35, 40 + d, body["hi"])      # crown sheen
    c.put(36, 40 + d, body["hi"])
    c.put(34, 41 + d, body["hi"])
    c.save(path)


def gen_banner_painting(path):
    """32x128 PaintingObject sheet: 4 rotation rows of one 32x32 banner —
    iron rod, bordered night cloth with fold shading, crescent-and-star
    emblem, swallowtail bottom with fringe (vanilla banner detail bar)."""
    def banner_cell():
        c = Canvas(32, 32)
        cloth = palette.NIGHTFELL
        trim = palette.STAIRLIGHT
        iron = palette.IRONWORK
        # mounting rod with end caps and rings
        c.rect(2, 1, 28, 3, iron["base"])
        c.rect(2, 1, 28, 1, iron["light"])
        c.put(1, 2, iron["light"])
        c.put(30, 2, iron["light"])
        c.put(6, 4, iron["deep"])
        c.put(25, 4, iron["deep"])
        # cloth field with border trim
        # Size law: vanilla wall banners fill ~776 opaque px per 32px cell;
        # the old 19x20 cloth came to 491 and read as a pennant.
        for y in range(4, 26):
            for x in range(4, 29):
                tone = cloth["base"]
                if x in (4, 28) or y == 4:
                    tone = trim["base"]            # silver border
                elif x in (10, 22):
                    tone = cloth["deep"]           # fold shadows
                elif x in (7, 16, 25):
                    tone = cloth["light"]          # fold ridges
                c.put(x, y, tone)
        # swallowtail: two tails + notch
        for i in range(6):
            for x in range(4, 15 - i):
                c.put(x, 26 + i, cloth["base"] if x > 4 else trim["base"])
            for x in range(18 + i, 29):
                c.put(x, 26 + i, cloth["base"] if x < 28 else trim["base"])
        for x in range(14, 19):                    # notch upper edge
            c.put(x, 24, cloth["deep"])
        # fringe tips
        for (fx, fy) in ((8, 29), (10, 28), (12, 27), (20, 27), (22, 28), (24, 29)):
            c.put(fx, fy, trim["base"])
        # crescent moon (2px stroke, opens right) + four-point star
        for (mx, my) in ((15, 8), (16, 8), (14, 9), (15, 9), (13, 10), (14, 10),
                         (13, 11), (14, 11), (13, 12), (14, 12), (13, 13), (14, 13),
                         (14, 14), (15, 14), (15, 15), (16, 15), (17, 15), (17, 8)):
            c.put(mx, my, trim["hi"])
        c.put(20, 10, trim["hi"])                  # star core
        c.put(20, 9, trim["base"])
        c.put(20, 11, trim["base"])
        c.put(19, 10, trim["base"])
        c.put(21, 10, trim["base"])
        c.put(18, 19, trim["base"])                # small companion star
        c.outline(palette.OUTLINE)
        return c
    sheet = Canvas(32, 128)
    cell = banner_cell()
    for row in range(4):
        sheet.paste(cell, 0, row * 32)
    sheet.save(path)


def gen_beacon(path, lit):
    """32x96 bottom-anchored beacon, v0.6 hero redesign (playtest: the spire
    reads as 'a small normal house' — the old beacon read as a streetlamp).

    Weathered Skywatch machinery now: a wide observatory plinth carrying a
    carved stair-sigil, a banded pillar with one intact armature ring and one
    SNAPPED stub (ancient damage), and a brass yoke cradling a big faceted
    storm-glass lens. ON = cold-teal core burning inside the lens with halo
    and sparks; OFF = dark lens, faint violet sheen, soot below the yoke."""
    c = Canvas(32, 96)
    stone = palette.SKYSTONE
    iron = palette.IRONWORK
    glow = palette.STAIRLIGHT
    trim = palette.WARDEN["trim"]
    trim_hi = palette.WARDEN["trim_hi"]

    # --- observatory plinth: three stone steps + carved sigil ---
    c.rect(2, 91, 28, 4, stone["base"])
    c.rect(2, 91, 28, 1, stone["light"])
    c.rect(2, 94, 28, 1, stone["deep"])
    c.rect(5, 86, 22, 5, stone["base"])
    c.rect(5, 86, 22, 1, stone["light"])
    c.rect(5, 90, 22, 1, stone["deep"])
    c.rect(8, 82, 16, 4, stone["base"])
    c.rect(8, 82, 16, 1, stone["light"])
    for (sx, sy) in ((12, 88), (15, 86), (18, 88)):   # stair sigil, deep-cut
        for dx in range(3):
            c.put(sx + dx, sy, stone["deep"])
            c.put(sx + dx, sy + 1, stone["light"] if dx == 0 else stone["deep"])
    c.put(9, 84, stone["deep"])                        # weathering pits
    c.put(22, 85, stone["deep"])

    # --- pillar: stone core, iron banding, one brass band ---
    for y in range(56, 82):
        t = (y - 56) / 26.0
        half = 4 + round(2 * t)
        for dx in range(-half, half + 1):
            tone = stone["base"]
            if dx <= -half + 1:
                tone = stone["light"]
            elif dx >= half - 1:
                tone = stone["deep"]
            c.put(16 + dx, y, tone)
    for band_y in (60, 74):                             # iron bands
        half = 4 + round(2 * (band_y - 56) / 26.0) + 1
        for dx in range(-half, half + 1):
            c.put(16 + dx, band_y, iron["deep"])
            c.put(16 + dx, band_y - 1, iron["base"])
        c.put(16 - half, band_y - 1, iron["light"])
    for dx in range(-6, 7):                             # brass band
        c.put(16 + dx, 66, trim if abs(dx) < 5 else iron["deep"])
        c.put(16 + dx, 65, trim_hi if abs(dx) < 3 else iron["base"])
    # armature: intact ring arm left, snapped stub right (ancient damage)
    for ang in range(0, 360, 20):
        import math
        x = 9 + int(3.4 * math.cos(math.radians(ang)))
        y = 62 + int(3.4 * math.sin(math.radians(ang)))
        c.put(x, y, iron["base"])
    c.put(6, 60, iron["light"])
    for k in range(5):                                  # snapped stub
        c.put(23 + (k % 2), 61 + k, iron["base"] if k < 3 else iron["deep"])
    c.put(24, 66, iron["deep"])
    c.put(23, 66, trim)                                 # torn bolt

    # --- brass yoke cradling the storm-glass lens ---
    for i in range(14):                                 # yoke arms
        lx = 16 - 8 + i // 2
        ly = 40 - i
        c.put(lx, ly, trim if i > 4 else iron["base"])
        c.put(lx + 1, ly, trim_hi if i > 4 else iron["base"])
        rx = 16 + 8 - i // 2
        c.put(rx, ly, trim if i > 4 else iron["deep"])
        c.put(rx - 1, ly, iron["deep"])
    c.rect(8, 40, 16, 2, iron["base"])                  # yoke collar
    c.rect(8, 40, 16, 1, iron["light"])
    c.rect(13, 36, 6, 4, iron["base"])                  # lens mount
    c.put(14, 36, trim)
    c.put(15, 36, trim_hi)

    # the lens: 14px faceted sphere
    if lit:
        c.ellipse(16, 27, 7, 7, glow["glow"])
        c.ellipse(16, 27, 5, 5, glow["hi"])
        c.ellipse(14, 25, 2.4, 2.2, (255, 255, 255))
        for (fx, fy) in ((12, 27), (20, 27), (16, 21), (16, 33)):
            c.put(fx, fy, glow["glow"])                 # facet seams
        c.put(11, 24, glow["hi"])
        c.put(21, 30, glow["glow"])
    else:
        c.ellipse(16, 27, 7, 7, palette.NIGHTFELL["deep"])
        c.ellipse(16, 27, 5, 5, palette.NIGHTFELL["base"])
        c.ellipse(14, 25, 2.2, 2.0, palette.NIGHTFELL["hi"])  # dead sheen
        c.put(20, 30, palette.STORMCRYSTAL["deep"])          # violet ghost
        c.put(12, 30, palette.STORMCRYSTAL["deep"])
        for k in range(4):                                   # soot under yoke
            c.put(13 + k, 35, iron["deep"])
            c.put(14 + k, 34, iron["deep"])

    c.outline(palette.OUTLINE)
    if lit:
        # halo + sparks float outside the silhouette (after outline)
        for (hx, hy, a) in ((4, 26, 150), (28, 24, 150), (16, 12, 170),
                            (8, 16, 130), (24, 15, 130), (12, 9, 110), (21, 8, 110)):
            c.put(hx, hy, with_alpha(glow["glow"], a))
        c.put(16, 10, with_alpha(glow["hi"], 200))
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
