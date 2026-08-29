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
    cloth = palette.AURORA
    # Decorativepot1 fills a broad grounded oval. This keeps that construction
    # but opens it into a high-backed woven pet bed rather than a closed pot.
    c.ellipse(16, 21, 13, 9, wick["deep"])
    c.ellipse(15, 19, 11, 7, wick["base"])
    c.ellipse(15, 17, 9, 4, wick["deep"])
    c.ellipse(16, 21, 9, 5, cloth["deep"])
    c.ellipse(15, 20, 7, 3.5, cloth["light"])
    c.ellipse(13, 19, 3.5, 1.5, cloth["hi"])
    # Thick braided rim and readable over-under weave on the basket face.
    for x in range(5, 28):
        y = 14 + abs(16 - x) // 5
        c.put(x, y, wick["light"] if x % 3 else wick["base"])
        c.put(x, y + 1, wick["base"])
    for x in (7, 11, 15, 19, 23, 26):
        for y in range(23, 28):
            if c.filled(x, y):
                c.put(x, y, wick["light"] if (x + y) % 2 else wick["deep"])
    c.rect(8, 27, 17, 2, wick["deep"])
    c.outline(palette.OUTLINE)
    c.put(8, 16, wick["light"])
    c.put(21, 24, cloth["deep"])
    c.put(12, 20, cloth["hi"])
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


def gen_flickerlightgarland_icon(path):
    """Three open light swags: cord, hanging bulbs, and real negative space."""
    c = Canvas(32, 32)
    wire = palette.GLOOMWOOD
    silk = palette.WINDSILK

    def swag(y0, sag):
        points = []
        for x in range(3, 29):
            t = (x - 3) / 25.0
            y = y0 + round(sag * 4 * t * (1 - t))
            c.put(x, y, wire["deep"])
            c.put(x, y + 1, wire["base"])
            points.append((x, y))
        return points

    runs = (swag(3, 6), swag(11, 5), swag(19, 5))
    c.outline(palette.OUTLINE)

    bulb_sites = ((0, 5), (0, 11), (0, 18), (0, 25),
                  (1, 7), (1, 15), (1, 23),
                  (2, 5), (2, 11), (2, 20), (2, 27))
    for index, (run, x) in enumerate(bulb_sites):
        y = runs[run][x - 3][1]
        # A short drop makes each coloured shape a hanging bulb, not a bead.
        c.put(x, y + 1, palette.OUTLINE)
        c.put(x, y + 2, palette.OUTLINE)
        c.ellipse(x, y + 4, 2.5, 2.5, palette.OUTLINE)
        color = palette.GARLAND_LIGHTS[index % len(palette.GARLAND_LIGHTS)]
        c.ellipse(x, y + 4, 1.5, 1.5, color)
        c.put(x - 1, y + 3, silk["hi"])
    c.save(path)


def gen_ghostlantern_icon(path):
    """Tall clock-like portrait of the crooked post and green cage."""
    c = Canvas(32, 32)
    iron = palette.IRONWORK
    flame = palette.GHOSTFLAME
    stone = palette.VEILROCK
    # Oak clock construction: stacked plinth, upright body, broad face, cap.
    c.rect(6, 27, 20, 4, stone["deep"])
    c.rect(8, 25, 16, 3, stone["base"])
    c.rect(8, 25, 16, 1, stone["light"])
    c.rect(13, 17, 7, 9, iron["base"])
    c.rect(13, 17, 2, 9, iron["light"])
    c.rect(18, 17, 2, 9, iron["deep"])
    c.rect(7, 6, 18, 13, iron["deep"])
    c.rect(9, 8, 14, 9, flame["deep"])
    c.rect(10, 8, 8, 8, flame["glow"])
    c.rect(12, 9, 6, 6, flame["core"])
    c.rect(6, 4, 20, 3, iron["base"])
    c.rect(7, 4, 18, 1, iron["light"])
    c.rect(12, 2, 9, 3, iron["base"])
    for x in (9, 15, 22):
        c.rect(x, 7, 2, 11, iron["base"])
        c.put(x, 7, iron["light"])
    c.outline(palette.OUTLINE)
    c.put(11, 9, flame["core"])
    c.put(22, 13, iron["hi"])
    c.put(10, 26, stone["hi"])
    c.save(path)


def gen_wardencandelabra_icon(path):
    """Three-light candelabra with oak clock's stacked vertical hierarchy."""
    c = Canvas(32, 32)
    iron = palette.IRONWORK
    glow = palette.STAIRLIGHT
    stone = palette.SKYSTONE
    c.rect(6, 27, 20, 4, stone["deep"])
    c.rect(8, 25, 16, 3, stone["base"])
    c.rect(8, 25, 16, 1, stone["light"])
    c.rect(13, 11, 7, 15, iron["base"])
    c.rect(13, 11, 2, 15, iron["light"])
    c.rect(18, 11, 2, 15, iron["deep"])
    c.rect(4, 9, 25, 5, iron["base"])
    c.rect(4, 9, 25, 1, iron["light"])
    c.rect(4, 13, 25, 1, iron["deep"])
    # Central lantern and two broad candle cups make three distinct lights.
    c.rect(11, 2, 11, 10, iron["deep"])
    c.rect(13, 4, 7, 6, glow["glow"])
    c.rect(14, 4, 4, 5, glow["hi"])
    for x in (5, 24):
        c.rect(x - 3, 6, 7, 5, iron["base"])
        c.rect(x - 2, 4, 5, 3, glow["light"])
        c.put(x, 3, glow["hi"])
        c.put(x - 1, 4, glow["glow"])
    c.outline(palette.OUTLINE)
    c.put(14, 4, glow["hi"])
    c.put(5, 3, glow["glow"])
    c.put(24, 3, glow["glow"])
    c.put(10, 26, stone["hi"])
    c.save(path)


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
    gen_wardencandelabra_icon(f"{dir_path}/wardencandelabra.png")
    mini_from(f"{obj}/gloomwillow.png", (0, 16, 48, 80), "gloomwillow.png")
    mini_from(f"{obj}/catbasket.png", (0, 6, 32, 30), "catbasket.png")
    mini_from(f"{obj}/mistglasslantern.png", (0, 0, 32, 32), "mistglasslantern.png")
    gen_flickerlightgarland_icon(f"{dir_path}/flickerlightgarland.png")
    mini_from(f"{obj}/skywatchbanner.png", (0, 0, 32, 32), "skywatchbanner.png")
    mini_from(f"{base}/objects/statues/gloomraven.png", (8, 34, 56, 92), "gloomravenstatue.png")
    # walls: crop a front-face piece; doors: rotation-0 closed leaf
    for wall in ("skystonebrickwall", "nightfellwall"):
        mini_from(f"{obj}/{wall}.png", (0, 64, 32, 96), f"{wall}.png")
    # Door icon: crop the rotation-0 CLOSED cell, which occupies y88..127 of the
    # sheet and nothing above it -- the same 40px the engine draws. The old crop
    # started at y24 because the generator used to paint doors over the full
    # 128px cell; against a correctly sized cell that crop is 64px of empty air.
    mini_from(f"{obj}/skystonebrickwall.png", (96, 88, 128, 128), "skystonebrickdoor.png")
    mini_from(f"{obj}/nightfellwall.png", (96, 88, 128, 128), "nightfelldoor.png")
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
    gen_ghostlantern_icon(f"{dir_path}/ghostlantern.png")
    mini_from(f"{obj}/seancecircle.png", (0, 8, 32, 64), "seancecircle.png")
    # The fence and the gate get DRAWN icons rather than a crop of their object
    # sheet. Vanilla does the same -- items/ironfence.png is a post with a rail
    # run leaving on both sides (576 opaque px), not a picture of one column --
    # and the old crop shipped 47 and 132 opaque px against those 576 and 652.
    gen_skyironfence_icon(f"{dir_path}/skyironfence.png")
    gen_skyironfencegate_icon(f"{dir_path}/skyironfencegate.png")


# =====================================================================
# Sky Iron fence + gate.
#
# The five/six columns of these two sheets are NOT a layout of our own: the
# engine addresses them at fixed sub-rects and fixed offsets, read out of
# necesse.level.gameObject.FenceObject.addDrawables and
# FenceGateObject.addDrawables (Necesse 1.3.2).
#
#   objects/skyironfence.png     160x64, every cell drawn bottom-anchored at
#                                drawY = tileDrawY - height + 32, so sheet row
#                                32 is the tile's TOP edge and row 63 its
#                                bottom edge.
#     col 0  post         always drawn
#     col 1  north joint  drawn when the tile ABOVE attaches; it is drawn
#                         BEFORE col 0, so it must stay inside the post's
#                         own footprint or it shows through as loose art
#     col 2  south rail   drawn when the tile BELOW attaches, and drawn a
#                         SECOND time at drawY-24 to bridge into a wall or
#                         rock standing above the fence
#     col 3  west rails   must reach x=0  — it is the run to the west
#     col 4  east rails   must reach x=31 — it is the run to the east
#
#   objects/skyironfencegate.png 192x64
#     col 0  open,   horizontal run (rotation 0/2)
#     col 1  closed, horizontal run (rotation 0/2)
#     col 2  vertical gate post, drawn TWICE: at drawY-14 and at drawY+14
#     col 3  latch piece, drawn at drawY+14, rotation 3 only
#     col 4  closed vertical leaf, drawn at drawY-14
#     col 5  open   vertical leaf, drawn at (drawX-16, drawY+14)
#
# What shipped before this pass was drawn to an invented convention — "post /
# horizontal run / top cap / left connector / right connector" — so the engine
# drew a full-width horizontal rail whenever a fence connected NORTH, a 3px
# hairline for the whole vertical run, and the west and east runs on each
# other's side of the tile. That is the "perspektivisch schrecklich" report.
#
# Geometry and value structure are measured off vanilla objects/ironfence.png
# and objects/ironfencegate.png, cell by cell:
#
#   cell        vanilla ironfence solid extent   mass
#   col 0 post  x10..21  y22..51                 424 px (+ soft skirt to y55)
#   col 1 joint x10..21  y26..33                 128 px
#   col 2 rail  x12..19  y34..63                 360 px
#   col 3 west  x0..9    y30..47                 220 px (+ skirt to y53)
#   col 4 east  x22..31  y30..47                 228 px (+ skirt to y53)
#
# Two things carry the top-down lean the game draws fences in, and the old
# sheet had neither: a horizontal rail is a LIT TOP band with a DARK FRONT
# FACE under it (never a symmetric bar), and every piece stands on a baked
# soft-alpha ground skirt (alpha 74 then 29) rather than an opaque shadow.
# =====================================================================

# Everything below is painted in 2x2 blocks, which is how vanilla's fence
# sheets are built: no odd rows, no single stray pixels.
_SI_KEYS = {"d": "deep", "m": "base", "l": "light", "H": "hi",
            "p": "patina", "P": "patina_hi"}


def _si(key):
    return palette.SKYIRON[_SI_KEYS[key]]


def _si_blk(c, x, y, w, h, color):
    c.rect(x, y, w, h, color)


def _si_row(c, x0, y, keys, out_left=True, out_right=True, out=None):
    """One 2px-tall band: optional 2px outline columns, then 2px ramp blocks."""
    out = out or palette.OUTLINE
    x = x0
    if out_left:
        _si_blk(c, x, y, 2, 2, out)
        x += 2
    for k in keys:
        _si_blk(c, x, y, 2, 2, out if k == "#" else _si(k))
        x += 2
    if out_right:
        _si_blk(c, x, y, 2, 2, out)


def _si_skirt(c, x, y, w, h, alpha):
    """Baked soft-alpha ground shadow — vanilla's rocks and fences both use
    this instead of an opaque dark band (docs/TECHNICAL_LEARNINGS.md)."""
    c.rect(x, y, w, h, with_alpha(palette.OUTLINE, alpha))


# Post shaft, top to bottom, as 2px bands of four 2px interior columns.
# Bright and alternating where it stands clear of the ground; the shaft goes
# dark below the tile's top edge because that is the part in its own shadow;
# verdigris creeps up from the foot.
_SI_POST = [
    "mHlm",   # y24
    "lHlm",   # y26
    "HHHH",   # y28  bright collar ring — the mod's own detail
    "mllm",   # y30
    "lddm",   # y32  <- tile top edge
    "lddm",   # y34
    "lddl",   # y36
    "HllH",   # y38  lower collar
    "lddm",   # y40
    "lddm",   # y42
    "lpPm",   # y44  verdigris
    "pPPl",   # y46
    "ppPp",   # y48
]


def _si_post_cell(c, x0=10, top=22):
    """The 12px post body at x0..x0+11, cap at top, foot at y50, skirt to y55."""
    _si_blk(c, x0 + 2, top, 8, 2, palette.OUTLINE)          # narrow domed cap
    for i, band in enumerate(_SI_POST):
        _si_row(c, x0, top + 2 + i * 2, band)
    foot = top + 2 + len(_SI_POST) * 2                       # y50
    _si_blk(c, x0 + 2, foot, 8, 2, palette.OUTLINE)          # ground line
    # ground skirt: alpha 74 core, alpha 29 spread, exactly vanilla's fade
    _si_skirt(c, x0, foot, 2, 2, 74)
    _si_skirt(c, x0 + 10, foot, 2, 2, 74)
    _si_skirt(c, x0 + 2, foot + 2, 8, 2, 74)
    _si_skirt(c, x0, foot + 2, 2, 2, 29)
    _si_skirt(c, x0 + 10, foot + 2, 2, 2, 29)
    _si_skirt(c, x0 + 2, foot + 4, 8, 2, 29)
    _si_skirt(c, x0 - 2, top + 22, 2, 10, 29)                # y44..53 flanks
    _si_skirt(c, x0 + 12, top + 22, 2, 10, 29)


def _si_rail_run(c, x0, keys_far, keys_near, width=10):
    """A pair of horizontal rails leaving the post toward one tile edge.

    Each rail is four 2px bands: outline, LIT TOP SURFACE, DARK FRONT FACE,
    outline. That top/face split is the whole perspective — a fence in this
    game is seen from above and slightly in front, never edge on."""
    for y, keys in ((30, keys_far), (40, keys_near)):
        c.rect(x0, y, width, 2, palette.OUTLINE)
        for i, k in enumerate(keys):
            _si_blk(c, x0 + i * 2, y + 2, 2, 2, _si(k))
        for i, k in enumerate(keys):
            face = "p" if k == "P" else ("d" if k != "p" else "p")
            _si_blk(c, x0 + i * 2, y + 4, 2, 2, _si(face))
        c.rect(x0, y + 6, width, 2, palette.OUTLINE)
    c.rect(x0, 48, width, 4, with_alpha(palette.OUTLINE, 74))
    c.rect(x0, 52, width, 2, with_alpha(palette.OUTLINE, 29))


def gen_skyironfence(path):
    """160x64. Columns are the engine's: post / north joint / south rail /
    west run / east run. See the block comment above."""
    sheet = Canvas(160, 64)

    # --- col 0: the post ------------------------------------------------
    c = Canvas(32, 64)
    _si_post_cell(c)
    sheet.paste(c, 0, 0)

    # --- col 1: north joint --------------------------------------------
    # Drawn under the post, 8 rows straddling the tile's top edge, so the run
    # continues past the boundary instead of ending in a notch. Same
    # cross-section as the post's own shaft at those rows.
    c = Canvas(32, 64)
    for i, band in enumerate(("lHlm", "HHHH", "mllm", "lddm")):
        _si_row(c, 10, 26 + i * 2, band)
    _si_skirt(c, 8, 26, 2, 8, 29)
    _si_skirt(c, 22, 26, 2, 8, 29)
    sheet.paste(c, 32, 0)

    # --- col 2: south rail ---------------------------------------------
    # An 8px bar running the FULL height of the tile, so stacked tiles join
    # seamlessly, and so the copy the engine draws at drawY-24 reaches into a
    # wall standing above the fence.
    c = Canvas(32, 64)
    _si_skirt(c, 14, 32, 4, 2, 29)
    c.rect(14, 34, 4, 2, palette.OUTLINE)                    # rounded top
    _si_skirt(c, 12, 34, 2, 2, 29)
    _si_skirt(c, 18, 34, 2, 2, 29)
    bar = "lmmHlmmHlmpldd"                                   # left column
    for i, k in enumerate(bar):
        y = 36 + i * 2
        c.rect(12, y, 2, 2, palette.OUTLINE)
        _si_blk(c, 14, y, 2, 2, _si(k))
        _si_blk(c, 16, y, 2, 2, _si("p" if k == "p" else "d"))
        c.rect(18, y, 2, 2, palette.OUTLINE)
        _si_skirt(c, 10, y, 2, 2, 29)
        _si_skirt(c, 20, y, 2, 2, 29)
    sheet.paste(c, 64, 0)

    # --- col 3: west run, reaching x=0 ----------------------------------
    c = Canvas(32, 64)
    _si_rail_run(c, 0, "mlHlH", "HlmlP")
    sheet.paste(c, 96, 0)

    # --- col 4: east run, reaching x=31 ---------------------------------
    # Not a mirror of col 3: the light stays top-left, so only the dash phase
    # and the skirt overhang change sides.
    c = Canvas(32, 64)
    _si_rail_run(c, 22, "HlmHl", "lHPlm")
    _si_skirt(c, 20, 48, 2, 4, 74)
    _si_skirt(c, 20, 52, 2, 2, 29)
    sheet.paste(c, 128, 0)

    sheet.save(path)


# --- gate ------------------------------------------------------------------

def _si_gate_post(c, x0, cap_inset=2, top=24, bands=None, width=8):
    """A gate post: outline columns, 2 interior blocks, cap, foot and skirt."""
    bands = bands or ("mH", "lH", "HH", "ml", "ld", "ld", "ll",
                      "Hl", "ld", "lp", "pP", "pp")
    inner = (width - 4) // 2
    _si_blk(c, x0 + 2, top, width - 4, 2, palette.OUTLINE)
    for i, band in enumerate(bands):
        _si_row(c, x0, top + 2 + i * 2, band)
    foot = top + 2 + len(bands) * 2
    _si_blk(c, x0 + 2, foot, width - 4, 2, palette.OUTLINE)
    _si_skirt(c, x0, foot, 2, 2, 74)
    _si_skirt(c, x0 + width - 2, foot, 2, 2, 74)
    _si_skirt(c, x0 + 2, foot + 2, width - 4, 2, 74)
    _si_skirt(c, x0, foot + 2, 2, 2, 29)
    _si_skirt(c, x0 + width - 2, foot + 2, 2, 2, 29)
    _si_skirt(c, x0 + 2, foot + 4, width - 4, 2, 29)
    return inner


def _si_gate_leaf(c, x0, x1, crown_x0, crown_x1):
    """The gate leaf seen head-on: crown, lit top rail, a dark lattice with
    two open windows, and a verdigris kick rail on the ground line."""
    out = palette.OUTLINE
    w = x1 - x0
    c.rect(crown_x0, 26, crown_x1 - crown_x0, 2, out)             # crown
    c.rect(x0, 28, w, 2, out)                                     # rail top
    top_keys = "mlHllHlm"
    n = w // 2
    for i in range(n):                                            # lit top face
        _si_blk(c, x0 + i * 2, 30, 2, 2, _si(top_keys[i % len(top_keys)]))
    _si_blk(c, x0, 32, 4, 2, _si("l"))                            # under the crown
    c.rect(x0 + 4, 32, w - 8, 2, out)
    _si_blk(c, x1 - 4, 32, 4, 2, _si("l"))
    for y in (34, 42):                                            # windows
        _si_blk(c, x0, y, 2, 2, _si("d"))
        c.rect(x0 + 2, y, 2, 2, out)
        c.rect(x1 - 4, y, 2, 2, out)
        _si_blk(c, x1 - 2, y, 2, 2, _si("d"))
    for y in (36, 40, 44):                                        # cross rails
        _si_blk(c, x0, y, 2, 2, _si("d"))
        c.rect(x0 + 2, y, w - 4, 2, out)
        _si_blk(c, x1 - 2, y, 2, 2, _si("d"))
    _si_blk(c, x0, 38, 2, 2, _si("p"))                            # dark middle
    c.rect(x0 + 2, 38, w - 4, 2, _si("d"))
    _si_blk(c, x1 - 2, 38, 2, 2, _si("d"))
    # Kick rail: metal first, verdigris as an ACCENT. A rail painted patina
    # end to end reads as a green stripe at 1x instead of a weathered gate.
    kick = "mlPlmlPl"
    for i in range(n):
        _si_blk(c, x0 + i * 2, 46, 2, 2, _si(kick[i % len(kick)]))
        _si_blk(c, x0 + i * 2, 48, 2, 2, _si("p" if i % 4 == 2 else "d"))
    c.rect(x0, 50, w, 2, with_alpha(out, 74))
    c.rect(x0, 52, w, 2, with_alpha(out, 29))


def gen_skyironfencegate(path):
    """192x64. Columns are the engine's; see the block comment above."""
    sheet = Canvas(192, 64)
    out = palette.OUTLINE

    # --- col 1: closed, horizontal (drawn whole, at the tile) -----------
    closed = Canvas(32, 64)
    _si_gate_post(closed, 0)
    _si_gate_post(closed, 24)
    _si_gate_leaf(closed, 8, 24, 12, 20)
    sheet.paste(closed, 32, 0)

    # --- col 0: open, horizontal ---------------------------------------
    # The leaf has swung out of the opening and now stands edge-on: a tall
    # narrow bar beside the hinge post. Vanilla's open cell reaches y14, well
    # above the closed one, which is what sells the swing.
    c = Canvas(32, 64)
    _si_gate_post(c, 0)
    _si_gate_post(c, 24)
    c.rect(8, 14, 2, 2, out)
    leaf = "mmmmlHlHdddlddpPp"
    for i, k in enumerate(leaf):
        y = 16 + i * 2
        c.rect(6, y, 2, 2, out)
        _si_blk(c, 8, y, 2, 2, _si(k))
        c.rect(10, y, 2, 2, out)
    _si_skirt(c, 12, 34, 2, 16, 29)                    # its shadow on the ground
    sheet.paste(c, 0, 0)

    # --- col 2: vertical gate post (drawn twice, at drawY-14 and +14) ---
    c = Canvas(32, 64)
    _si_gate_post(c, 12, top=24)
    _si_skirt(c, 10, 46, 2, 6, 29)
    _si_skirt(c, 20, 46, 2, 6, 29)
    sheet.paste(c, 64, 0)

    # --- col 3: latch piece, rotation 3 only, drawn at drawY+14 ---------
    c = Canvas(32, 64)
    _si_skirt(c, 14, 30, 2, 2, 29)
    c.rect(14, 32, 2, 2, out)
    for i, k in enumerate("mlHldpdp"):
        y = 34 + i * 2
        c.rect(12, y, 2, 2, out)
        _si_blk(c, 14, y, 2, 2, _si(k))
        c.rect(16, y, 2, 2, out)
        _si_skirt(c, 10, y, 2, 2, 29)
        _si_skirt(c, 18, y, 2, 2, 29)
    sheet.paste(c, 96, 0)

    # --- col 4: closed vertical leaf, drawn at drawY-14 -----------------
    c = Canvas(32, 64)
    _si_skirt(c, 14, 28, 4, 2, 29)
    c.rect(12, 30, 8, 2, out)
    for i, k in enumerate("mmmmmlHddpdpPp"):
        y = 32 + i * 2
        c.rect(12, y, 2, 2, out)
        _si_blk(c, 14, y, 2, 2, _si(k))
        _si_blk(c, 16, y, 2, 2, _si("d" if k not in "pP" else "p"))
        c.rect(18, y, 2, 2, out)
        _si_skirt(c, 10, y, 2, 2, 29)
        _si_skirt(c, 20, y, 2, 2, 29)
    sheet.paste(c, 128, 0)

    # --- col 5: open vertical leaf, drawn at (drawX-16, drawY+14) -------
    c = Canvas(32, 64)
    _si_gate_leaf(c, 8, 28, 16, 22)
    _si_skirt(c, 6, 48, 2, 4, 29)
    sheet.paste(c, 160, 0)

    sheet.save(path)


def _si_cap_rail_end(c, x, keys_len=5):
    """Close a rail run's open end with an outline column — an icon shows a
    piece of fence, not a rail sawn off at the frame edge."""
    for y in (32, 34, 42, 44):
        c.rect(x, y, 2, 2, palette.OUTLINE)


def gen_skyironfence_icon(path):
    """items/skyironfence.png: one post with a rail run leaving on each side,
    which is how vanilla draws items/ironfence.png (576 opaque px, post at
    rows 2..29, rails at rows 8..25). The old icon was a crop of the object
    sheet's post column and shipped 47 opaque px."""
    c = Canvas(32, 64)
    _si_rail_run(c, 2, "mlHlH", "HlmlP")
    _si_rail_run(c, 20, "HlmHl", "lHPlm")
    _si_cap_rail_end(c, 2)
    _si_cap_rail_end(c, 28)
    _si_post_cell(c)
    icon = Canvas(32, 32)
    icon.img.alpha_composite(c.img.crop((0, 20, 32, 52)), (0, 0))
    icon.px = icon.img.load()
    icon.save(path)


def gen_skyironfencegate_icon(path):
    """items/skyironfencegate.png: the closed leaf between its two posts,
    matching vanilla items/ironfencegate.png (652 opaque px)."""
    c = Canvas(32, 64)
    _si_gate_post(c, 0)
    _si_gate_post(c, 24)
    _si_gate_leaf(c, 8, 24, 12, 20)
    icon = Canvas(32, 32)
    icon.img.alpha_composite(c.img.crop((0, 22, 32, 54)), (0, 0))
    icon.px = icon.img.load()
    icon.save(path)


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
            for x in range(2, 30):
                t = (x - 2) / 28.0
                y = (4 + round(5 * (4 * t * (1 - t))) if orientation == 0
                     else 27 - round(5 * (4 * t * (1 - t))))
                c.put(x, y - 1, wire["deep"])
                c.put(x, y, wire["deep"])
                c.put(x, y + 1, wire["base"])
                pts.append((x, y))
            for mx in (2, 29):
                c.rect(mx - 1, pts[0 if mx == 2 else -1][1] - 2, 3, 4, iron["base"])
                c.put(mx - 1, pts[0 if mx == 2 else -1][1] - 2, iron["light"])
            bulb_dir = (0, 1 if orientation == 0 else -1)
        else:
            side = -1 if orientation == 1 else 1
            for y in range(2, 30):
                t = (y - 2) / 28.0
                x = (27 - round(5 * (4 * t * (1 - t))) if orientation == 1
                     else 4 + round(5 * (4 * t * (1 - t))))
                c.put(x - 1, y, wire["deep"])
                c.put(x, y, wire["deep"])
                c.put(x + 1, y, wire["base"])
                pts.append((x, y))
            for my in (2, 29):
                px_ = pts[0 if my == 2 else -1][0]
                c.rect(px_ - 2, my - 1, 4, 3, iron["base"])
                c.put(px_ - 2, my - 1, iron["light"])
            bulb_dir = (side, 0)
        for i, idx in enumerate((2, 6, 10, 14, 18, 22, 26)):
            px_, py_ = pts[idx]
            color = palette.GARLAND_LIGHTS[i % len(palette.GARLAND_LIGHTS)]
            dx, dy = bulb_dir
            c.put(px_ + dx, py_ + dy, palette.OUTLINE)
            c.put(px_ + 2 * dx, py_ + 2 * dy, palette.OUTLINE)
            bx, by = px_ + 4 * dx, py_ + 4 * dy
            c.ellipse(bx, by, 2.5, 2.5, palette.OUTLINE)
            c.ellipse(bx, by, 1.5, 1.5, color if lit else palette.NIGHTFELL["base"])
            if lit:
                c.put(bx - 1, by - 1, palette.WINDSILK["hi"])
                c.put(bx, by, color)
            else:
                c.put(bx, by, color)
                c.put(bx - 1, by - 1, iron["light"])
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
    """32x64 reforged island anchor (SkyDecoObject).

    Thickened, not redesigned. The previous version had the right anatomy --
    open ring, shank, stock, curved arms into flukes -- drawn entirely in 1px
    lines, which came to 143 opaque px, 0.36 of vanilla `bannerstand`. A pass
    that chased the number instead replaced the ring with a filled disc and the
    flukes with two rectangles: 764 px, 1.93x the reference, and no longer
    readable as an anchor at 1x. That is the "mass without form" failure
    docs/ART_DIRECTION.md names. So every element below is the OLD element,
    given real width and a lit/dark side -- the ring stays an annulus you can
    see through, the stock stays a crossbar, and the arms still curve into
    points.
    """
    import math
    c = Canvas(32, 64)
    a = palette.AETHERIUM
    iron = palette.IRONWORK

    # Ring: an annulus, not a disc. Outer r 7.5, inner r 4.5, so the hole
    # survives at 1x -- a filled circle reads as a knob on a post.
    for yy in range(4, 26):
        for xx in range(4, 29):
            d = math.hypot(xx - 16, yy - 15)
            if 4.6 <= d <= 7.6:
                # lit on the upper-left of the ring, deep on the lower-right
                c.put(xx, yy, iron["light"] if (xx - 16) + (yy - 15) < -1
                      else iron["deep"] if (xx - 16) + (yy - 15) > 3
                      else iron["base"])

    # Shank: 5px wide, lit left edge / dark right edge.
    for y in range(21, 50):
        c.put(14, y, a["light"])
        c.put(15, y, a["light"])
        c.put(16, y, a["base"])
        c.put(17, y, a["base"])
        c.put(18, y, a["deep"])

    # Stock (crossbar): 3px tall and wide enough to read as a bar.
    for x in range(7, 26):
        c.put(x, 27, iron["light"])
        c.put(x, 28, iron["base"])
        c.put(x, 29, iron["deep"])
    c.put(6, 28, iron["base"])
    c.put(26, 28, iron["base"])

    # Arms: curved, 3px thick, sweeping out and up into pointed flukes.
    for i in range(12):
        t = i / 11.0
        dx = int(round(11 * t))
        dy = int(round(9 * t * t))          # curve: shallow first, then lifts
        for k in range(3):
            c.put(15 - dx, 49 - dy + k, a["light"] if k == 0 else a["base"] if k == 1 else a["deep"])
            c.put(17 + dx, 49 - dy + k, a["base"] if k == 0 else a["deep"] if k == 1 else a["deep"])
    # Fluke points: a triangle at each arm tip so it ends in a barb.
    for k in range(4):
        for j in range(4 - k):
            c.put(4 + j, 40 + k, a["base"])
            c.put(27 - j, 40 + k, a["deep"])
    c.put(4, 39, a["hi"])
    c.put(27, 39, a["hi"])

    c.outline(palette.OUTLINE)
    # Highlights after the outline pass, or it eats them.
    c.put(13, 12, iron["hi"])
    c.put(15, 23, a["hi"])
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
