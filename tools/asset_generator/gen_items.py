"""32x32 item icons and held-weapon sprites (player/weapons/)."""

from px import Canvas, Rng, with_alpha
import palette


def _finish(c):
    c.outline(palette.OUTLINE)
    return c


# --- Materials ---------------------------------------------------------------

def gen_skystone(path):
    c = Canvas(32, 32)
    r = palette.SKYSTONE
    rng = Rng(0x5709)
    c.blob(15, 18, 8, r["base"], rng)
    c.blob(21, 21, 4, r["base"], rng)
    c.ellipse(13, 15, 4, 3, r["light"])
    c.put(11, 13, r["hi"])
    for _ in range(6):
        c.put(rng.range(9, 24), rng.range(14, 24), r["deep"])
    _finish(c).save(path)


def gen_aetheriumore(path):
    c = Canvas(32, 32)
    stone = palette.SKYSTONE
    ore = palette.AETHERIUM
    rng = Rng(0xAE07)
    c.blob(16, 19, 8, stone["base"], rng)
    c.ellipse(13, 16, 4, 3, stone["light"])
    for cx, cy in ((12, 19), (19, 16), (17, 23)):
        c.ellipse(cx, cy, 2.4, 2, ore["deep"])
        c.ellipse(cx, cy - 1, 1.6, 1.3, ore["base"])
        c.put(cx, cy - 2, ore["light"])
    c.put(12, 16, ore["hi"])
    _finish(c).save(path)


def gen_aetheriumbar(path):
    c = Canvas(32, 32)
    r = palette.AETHERIUM
    # classic trapezoid ingot: front face, top face, right side
    # front face (wide base)
    for y in range(19, 25):
        c.line(6, y, 24, y, r["base"])
    # top face (narrower, shifted up-right)
    for i in range(5):
        c.line(9 + i, 18 - i, 21 + i, 18 - i, r["light"])
    c.line(13, 14, 25, 14, r["hi"])
    # right side face
    for i in range(5):
        c.line(24 + min(i, 1), 19 + i, 25, 19 + i, r["deep"])
    c.line(6, 24, 24, 24, r["deep"])
    # glint
    c.put(12, 16, r["hi"])
    c.put(11, 17, r["hi"])
    _finish(c).save(path)


def gen_stormshard(path):
    c = Canvas(32, 32)
    r = palette.STORMCRYSTAL
    for i in range(16):
        t = i / 15.0
        half = max(1, round(4 * (1.0 - abs(t * 2 - 1))))
        for dx in range(-half, half + 1):
            c.put(15 + dx + round(t * 3), 25 - i, r["base"] if dx > -half else r["deep"])
        if 0.2 < t < 0.85:
            c.put(13 + round(t * 3), 25 - i, r["light"])
    c.put(17, 11, r["hi"])
    c.put(21, 18, r["hi"])
    _finish(c).save(path)


def gen_windsilk(path):
    c = Canvas(32, 32)
    r = palette.WINDSILK
    # folded silk skein with a wind-loop
    c.ellipse(16, 19, 9, 6, r["base"])
    c.ellipse(15, 17, 7, 4, r["light"])
    for x in range(9, 24):
        y = 19 + round(2.2 * ((x % 6) - 2.5) ** 2 / 6 - 2)
        c.put(x, y, r["deep"])
    c.ellipse(22, 12, 4, 2.5, r["light"])
    c.put(24, 11, r["hi"])
    c.put(12, 15, r["hi"])
    _finish(c).save(path)


def gen_aurorapetal(path):
    c = Canvas(32, 32)
    r = palette.AURORA
    # single faceted petal, teardrop leaning right
    for i in range(17):
        t = i / 16.0
        half = max(1, round(5 * (1.0 - abs(t * 2 - 1)) + 1 - t))
        cx = 14 + round(t * 5)
        for dx in range(-half, half + 1):
            c.put(cx + dx, 25 - i, r["base"])
    c.ellipse(15, 20, 2.5, 3.5, r["light"])
    c.put(17, 12, r["hi"])
    c.put(14, 23, r["teal"])
    _finish(c).save(path)


# --- Weapons -----------------------------------------------------------------

def _tempest_blade(c, grip_x, grip_y):
    """Silhouette-first: outline mass underneath, bright blade core on top —
    the auto-outline pass would eat a thin diagonal blade otherwise."""
    steel = palette.AETHERIUM
    wood = palette.WOOD
    # 1. dark silhouette mass (5px wide diagonal)
    for i in range(18):
        x = grip_x + 3 + i
        y = grip_y - 3 - i
        for k in range(5):
            c.put(x - k, y + k - 1, palette.OUTLINE)
    # 2. blade core on top: hi edge, light, base
    for i in range(16):
        x = grip_x + 4 + i
        y = grip_y - 4 - i
        c.put(x, y - 1, steel["hi"])
        c.put(x - 1, y, steel["light"])
        c.put(x - 2, y + 1, steel["base"])
    # tapered bright tip
    c.put(grip_x + 20, grip_y - 21, steel["hi"])
    c.put(grip_x + 19, grip_y - 20, steel["light"])
    # storm-crystal crossguard, perpendicular to the blade
    for d in range(-3, 4):
        c.put(grip_x + 2 - d, grip_y - 2 - d, palette.OUTLINE)
    for d in range(-2, 3):
        c.put(grip_x + 2 - d, grip_y - 2 - d, palette.STORMCRYSTAL["light"] if d % 2 else palette.STORMCRYSTAL["base"])
    # wrapped grip + crystal pommel
    for i in range(5):
        c.put(grip_x - i, grip_y + i, wood["base"] if i % 2 else wood["deep"])
        c.put(grip_x - i - 1, grip_y + i, palette.OUTLINE)
        c.put(grip_x - i + 1, grip_y + i + 1, palette.OUTLINE)
    c.put(grip_x - 5, grip_y + 5, palette.STORMCRYSTAL["light"])
    c.put(grip_x - 6, grip_y + 6, palette.STORMCRYSTAL["base"])


def gen_tempestedge_icon(path):
    c = Canvas(32, 32)
    _tempest_blade(c, 8, 24)
    c.save(path)


def gen_tempestedge_held(path):
    c = Canvas(32, 32)
    _tempest_blade(c, 6, 26)
    c.save(path)


def _galehowl_bow(c):
    wood = palette.WOOD
    silk = palette.WINDSILK
    steel = palette.AETHERIUM
    # vertical recurve bow (2px thick limbs), string well to the left
    for i in range(24):
        t = i / 23.0
        x = 17 + round(7 * (1 - abs(t * 2 - 1) ** 1.7))
        y = 4 + i
        c.put(x, y, wood["light"])
        c.put(x + 1, y, wood["base"])
        c.put(x + 2, y, wood["deep"])
    # aetherium limb tips
    for (ty, up) in ((4, True), (27, False)):
        c.put(17, ty, steel["light"])
        c.put(17, ty - 1 if up else ty + 1, steel["hi"])
    # windsilk string
    c.line(17, 4, 10, 16, silk["light"])
    c.line(10, 16, 17, 27, silk["light"])
    c.put(10, 16, silk["hi"])
    c.put(11, 16, silk["hi"])
    # wrapped grip in the middle of the bow
    for y in range(14, 19):
        c.put(23, y, silk["base"])
        c.put(24, y, silk["deep"])


def gen_galehowl_icon(path):
    c = Canvas(32, 32)
    _galehowl_bow(c)
    _finish(c).save(path)


def gen_galehowl_held(path):
    c = Canvas(32, 32)
    _galehowl_bow(c)
    _finish(c).save(path)


# --- Object items ------------------------------------------------------------

def gen_skystonerock_item(path):
    c = Canvas(32, 32)
    r = palette.SKYSTONE
    rng = Rng(0x50CE)
    c.blob(16, 20, 9, r["base"], rng)
    c.ellipse(13, 16, 5, 3.5, r["light"])
    c.put(11, 14, r["hi"])
    for _ in range(5):
        c.put(rng.range(10, 23), rng.range(16, 25), r["deep"])
    _finish(c).save(path)


def gen_skyreeds_item(path):
    c = Canvas(32, 32)
    r = palette.WINDSILK
    rng = Rng(0x4EE)
    # silhouette-first: dark blade shadows one pixel behind each reed
    for k in range(4):
        x = 11 + k * 3
        h = rng.range(13, 18)
        lean = rng.pick((-2, -1, 1, 2))
        for i in range(h):
            t = i / h
            sx = x + round(lean * t * t * 2)
            c.put(sx - 1, 27 - i, palette.OUTLINE)
            c.put(sx + 2, 27 - i, palette.OUTLINE)
    rng = Rng(0x4EE)
    for k in range(4):
        x = 11 + k * 3
        h = rng.range(13, 18)
        lean = rng.pick((-2, -1, 1, 2))
        for i in range(h):
            t = i / h
            sx = x + round(lean * t * t * 2)
            tone = r["base"] if t < 0.55 else r["light"]
            c.put(sx, 27 - i, tone)
            if t < 0.7:
                c.put(sx + 1, 27 - i, tone)
        c.put(x + round(lean * 1.7), 27 - h, r["hi"])
    # binding tie
    c.rect(12, 21, 10, 3, palette.WOOD["base"])
    c.rect(12, 22, 10, 1, palette.WOOD["deep"])
    c.save(path)


def gen_stairway_item(path):
    c = Canvas(32, 32)
    r = palette.STAIRLIGHT
    iron = palette.IRONWORK
    mist = palette.MISTSEA
    # mini grand flight rising right with handrail (bold: icons must read at 1x)
    steps = [(2, 24, 11), (9, 18, 10), (16, 12, 9), (22, 6, 8)]
    for sx, sy, w in steps:
        for dx in range(w):
            for dy in range(8):
                c.put(sx + dx, sy + dy, r["base"])
        for dx in range(w):
            c.put(sx + dx, sy, r["light"])
            c.put(sx + dx, sy - 1, r["hi"] if dx % 3 == 0 else r["light"])
            c.put(sx + dx, sy + 5, r["deep"])
    for sx, sy, w in steps:            # handrail posts + line
        c.put(sx + w - 1, sy - 4, iron["deep"])
        c.put(sx + w - 1, sy - 3, iron["deep"])
        c.put(sx + w - 1, sy - 2, iron["deep"])
    c.line(12, 21, 29, 6, iron["base"])
    # cloud puff at the base + glow at the top
    c.ellipse(6, 29, 5, 2.2, mist["hi"])
    c.ellipse(11, 30, 4, 1.8, mist["light"])
    _finish(c).save(path)
    from px import with_alpha as _wa
    from PIL import Image as _I
    img = _I.open(path).convert("RGBA")
    px = img.load()
    for (gx, gy, a) in ((27, 4, 220), (25, 2, 150), (29, 6, 150)):
        pr, pg, pb = palette.STAIRLIGHT["glow"]
        px[gx, gy] = (pr, pg, pb, a)
    img.save(path)


def gen_windwheat_item(path):
    """Tied bundle of wheat-grass."""
    c = Canvas(32, 32)
    W = palette.WINDWHEAT
    for i, (x0, lean) in enumerate(((11, -2), (15, 0), (19, 2), (13, -1), (17, 1))):
        for y in range(26, 8 + i % 3, -1):
            t = (26 - y) / 18.0
            px_ = x0 + round(lean * t)
            c.put(px_ - 1, y, W["deep"])
            c.put(px_, y, W["base"] if y > 14 else W["light"])
            c.put(px_ + 1, y, W["base"])
        hx = x0 + lean
        for (dx, dy) in ((0, 0), (1, 0), (-1, 1), (0, 1), (1, 1), (0, 2), (1, 2), (0, 3)):
            c.put(hx + dx, 6 + i % 3 + dy, W["head"])
    # tie band
    for x in range(12, 20):
        c.put(x, 22, palette.WOOD["deep"])
    c.put(13, 21, palette.WOOD["light"])
    _finish(c).save(path)


def gen_cloudberrybush_item(path):
    c = Canvas(32, 32)
    B = palette.CLOUDBERRY
    c.ellipse(16, 20, 9, 6, B["leaf"])
    c.ellipse(13, 16, 4, 3, B["leaf"])
    c.ellipse(20, 17, 4, 3, B["leaf"])
    c.ellipse(13, 23, 6, 3, B["leaf_deep"])
    for (bx, by) in ((11, 18), (17, 15), (21, 20), (14, 21), (19, 23)):
        c.put(bx, by, B["berry"])
        c.put(bx + 1, by, B["berry_deep"])
        c.put(bx, by - 1, B["berry_hi"])
    _finish(c).save(path)


def gen_cloudberry_item(path):
    """Three plump amber berries with a leaf."""
    c = Canvas(32, 32)
    B = palette.CLOUDBERRY
    for (bx, by, r_) in ((13, 18, 4.5), (20, 16, 4), (17, 22, 4)):
        c.ellipse(bx, by, r_, r_, B["berry"])
        c.ellipse(bx + 1, by + 1, r_ * 0.6, r_ * 0.6, B["berry_deep"])
        c.put(bx - 1, by - 2, B["berry_hi"])
        c.put(bx - 2, by - 1, B["berry_hi"])
    c.put(20, 11, B["leaf"])
    c.put(21, 10, B["leaf"])
    c.put(22, 11, B["leaf_deep"])
    _finish(c).save(path)
def gen_crystal_item(path, ramp, salt):
    c = Canvas(32, 32)
    rng = Rng(salt)
    base_y = 27
    for x, h, w, lean in ((15, 16, 3, 1), (9, 10, 2, -2), (22, 11, 2, 2)):
        for i in range(h):
            t = i / max(h - 1, 1)
            half = max(1, round(w * (1.0 - abs(t * 2 - 1))))
            cx = x + round(lean * t)
            for dx in range(-half, half + 1):
                c.put(cx + dx, base_y - i, ramp["deep"] if dx in (-half, half) else ramp["base"])
            if 0.25 < t < 0.9:
                c.put(cx - max(0, half - 1), base_y - i, ramp["light"])
        c.put(x + lean, base_y - h + 1, ramp["hi"])
    _finish(c).save(path)


# --- Tree and sapling item icons ---------------------------------------------
#
# GameObject.generateItemTexture() loads items/<stringID>, and TreeObject and
# TreeSaplingObject do not override it. Our trees and saplings shipped with an
# objects/ sprite but no items/ icon, so every one of them showed the engine's
# error texture in the crafting menu and the inventory (playtest 2026-08-24
# reported it on the Prism sapling; it was in fact all six).
#
# Vanilla's sapling item icon is a byte-identical copy of its object sprite --
# verified against objects/birchsapling.png and items/birchsapling.png -- so
# the saplings copy. Trees get their own compact glyph, the way vanilla draws
# items/oaktree.png rather than shrinking the 256x512 object sheet.

def gen_sapling_item_icons(objects_dir, items_dir):
    """Copy each 32x32 sapling object sprite to its item icon, vanilla-style."""
    from PIL import Image
    for name in ("nimbussapling", "fulgursapling", "prismasapling"):
        src = Image.open(f"{objects_dir}/{name}.png").convert("RGBA")
        src.save(f"{items_dir}/{name}.png")


def _tree_item(path, wood, leaf, conifer=False):
    c = Canvas(32, 32)
    for y in range(20, 31):                      # trunk, lit on the left
        for x in range(14, 18):
            c.put(x, y, wood["base"])
        c.put(14, y, wood["light"])
        c.put(17, y, wood["deep"])
    for y in range(27, 31):                      # root flare
        w = (y - 26)
        for x in range(14 - w, 18 + w):
            c.put(x, y, wood["deep"] if x < 14 or x > 17 else wood["base"])
    if conifer:
        # Tiers WIDEN downward and overlap by a row, so the needle mass reads
        # as one connected cone. Drawing them as separate shrinking discs left
        # gaps between the tiers and the icon looked like a stacked lamp.
        for (top, half) in ((4, 3), (9, 6), (14, 8), (18, 10)):
            for dy in range(6):
                w = half - abs(dy - 4) if dy > 4 else half
                w = max(1, w)
                for x in range(16 - w, 16 + w + 1):
                    c.put(x, top + dy, leaf["base"])
            for dy in range(3):                  # lit left flank of the tier
                w = max(1, half - dy)
                for x in range(16 - w, 16 - w + 3):
                    c.put(x, top + dy, leaf["light"])
            for x in range(16 + 1, 16 + half + 1):   # shaded skirt
                c.put(x, top + 4, leaf["deep"])
        c.put(16, 2, leaf["light"])              # leader spike
        c.put(16, 3, leaf["base"])
    else:
        c.ellipse(16, 13, 10.0, 8.0, leaf["base"])
        c.ellipse(12, 10, 5.5, 4.5, leaf["light"])   # lit top-left mass
        c.ellipse(21, 17, 5.0, 4.0, leaf["deep"])    # shaded underside
        c.ellipse(11, 18, 4.0, 3.2, leaf["deep"])
        c.ellipse(19, 8, 4.5, 3.5, leaf["light"])
        for (x, y) in ((10, 8), (14, 6), (22, 12), (8, 14), (18, 20)):
            c.put(x, y, leaf["hi"] if "hi" in leaf else leaf["light"])
    return _finish(c).save(path)


def gen_tree_item_icons(items_dir):
    _tree_item(f"{items_dir}/nimbuswillow.png", palette.NIMBUSWOOD, palette.NIMBUSLEAF)
    _tree_item(f"{items_dir}/fulgurpine.png", palette.CHARWOOD, palette.FULGURPINE_NEEDLE,
               conifer=True)
    _tree_item(f"{items_dir}/prismabirch.png", palette.PRISMWOOD, palette.PRISMLEAF)
