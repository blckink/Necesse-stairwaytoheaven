"""v0.6 environmental prop families + hero accents + sky-oddity seeds.

All sheets are plain bottom-anchored SkyDecoObject strips (one variant each
unless noted): the object class centres `variantWidth` on the tile and rises
the sprite above it, so any width/height works without engine changes.

Families (playtest: the stone/storm region felt empty; the green Driftlands
did not — these give Stormveil and the Aurora Shoals the same reusable small-
prop language the Driftlands already has):

- Spire hero accents: skywatchtelescope, skywatchastrolabe
- Stormveil: stormscreed (lightning-scorched ground), skywatchrubble (broken
  carved Skywatch stone), chargecrystal (small charged formation),
  withershrub (dead twisted shrub)
- Aurora Shoals: aurorashards (teal/rose crystal cluster), starfall (fallen
  star fragment)
- Sky oddities (rare-encounter seeds, NOT in worldgen): skyballoon,
  aeronautwreck, skyparcel
"""

from px import Canvas, Rng, with_alpha
import palette


def _stone_patch(c, ramp, rng, cx, cy, rx, ry):
    """A weathered stone mound with a lit top and a deep skirt."""
    c.ellipse(cx, cy + 1, rx, ry, ramp["deep"])
    c.ellipse(cx, cy, rx - 0.5, ry - 0.5, ramp["base"])
    for _ in range(3):
        px_ = cx + rng.range(-int(rx) // 2, int(rx) // 2)
        py_ = cy + rng.range(-1, 1)
        c.put(px_, py_ - 1, ramp["light"])


def gen_skywatchtelescope(path):
    """32x72 hero accent: an old Skywatch refractor on a stone tripod —
    brass tube angled up-right, gimbal mount, eyepiece, lens glint."""
    c = Canvas(32, 72)
    stone = palette.SKYSTONE
    iron = palette.IRONWORK
    trim = palette.WARDEN["trim"]
    trim_hi = palette.WARDEN["trim_hi"]
    import math
    # stone tripod: three splayed legs + hub
    for (x0, x1) in ((9, 4), (23, 28)):
        for i in range(14):
            x = x0 + round((x1 - x0) * i / 13)
            c.put(x, 70 - i, iron["base"])
            c.put(x + 1, 70 - i, iron["deep"])
        c.put(x1, 70, iron["deep"])
    for i in range(14):
        c.put(16, 70 - i, iron["base"])
        c.put(17, 70 - i, iron["deep"])
    rng = Rng(0x5C0FE)
    _stone_patch(c, stone, rng, 16, 57, 8, 2.5)
    # gimbal hub
    c.ellipse(16, 52, 4.5, 3.6, iron["base"])
    c.ellipse(15, 51, 2.2, 1.8, iron["light"])
    c.put(16, 49, trim)
    # brass tube: angled up-right, THICK body with banding and a shade edge
    ang = math.radians(38)
    dx, dy = math.cos(ang), -math.sin(ang)
    for i in range(30):
        tx = 17 + round(dx * i)
        ty = 50 + round(dy * i) - i // 6
        w = 4 if i < 8 else (5 if i < 22 else 4)
        for o in range(-w, w + 1):
            tone = trim
            if o >= w - 1:
                tone = palette.WOOD["deep"]
            elif o <= -w + 1:
                tone = trim_hi
            elif i % 9 == 0:
                tone = iron["base"]
            c.put(tx + o // 2, ty + (o + 1) // 2, tone)
    c.rect(26, 30, 5, 4, iron["base"])                  # objective housing
    c.put(26, 30, iron["light"])
    c.ellipse(30, 32, 2, 2, (210, 235, 240))            # objective lens
    c.put(30, 31, (245, 252, 252))
    c.rect(4, 46, 4, 4, iron["base"])                   # eyepiece
    c.put(4, 46, iron["light"])
    c.put(5, 49, trim)
    c.outline(palette.OUTLINE)
    c.save(path)


def gen_skywatchastrolabe(path):
    """32x56 hero accent: a weathered celestial navigation table — stone slab
    on feet, brass armillary rings on a pin, small instruments."""
    c = Canvas(32, 56)
    stone = palette.SKYSTONE
    iron = palette.IRONWORK
    trim = palette.WARDEN["trim"]
    trim_hi = palette.WARDEN["trim_hi"]
    import math
    c.rect(4, 44, 24, 4, stone["base"])
    c.rect(4, 44, 24, 1, stone["light"])
    c.rect(4, 47, 24, 1, stone["deep"])
    for fx in (6, 23):
        c.rect(fx, 48, 3, 4, stone["base"])
        c.put(fx, 48, stone["light"])
    c.rect(7, 40, 18, 4, stone["base"])                 # tabletop
    c.rect(7, 40, 18, 1, stone["light"])
    for x in (10, 16, 22):
        c.put(x, 42, stone["deep"])                     # slab joints
    # armillary: three rings on a brass pin
    cx, cy = 16, 28
    for ang in range(0, 360, 10):
        c.put(cx + int(9 * math.cos(math.radians(ang))),
              cy + int(3.2 * math.sin(math.radians(ang))), trim)
        c.put(cx + int(4.2 * math.cos(math.radians(ang))),
              cy + int(9 * math.sin(math.radians(ang))),
              trim_hi if ang % 30 < 10 else trim)
    for ang in range(0, 360, 15):
        x = cx + int(9.5 * math.cos(math.radians(ang)))
        y = cy + int(9.5 * math.sin(math.radians(ang)) * 0.35)
        c.put(x, y - 6, iron["base"])
    c.put(cx, cy - 2, iron["light"])                    # centre pin
    c.put(cx, cy - 1, trim_hi)
    c.put(10, 38, trim)                                 # small instruments
    c.put(21, 38, iron["base"])
    c.put(22, 38, iron["light"])
    c.outline(palette.OUTLINE)
    c.put(9, 22, with_alpha(palette.STAIRLIGHT["hi"], 160))   # faint gleam
    c.save(path)


def gen_stormscreed(path):
    """32x32 flat ground decal: a lightning-scorched blast scar — radial
    dark scorch, pale ash fringe, two fulgurite nubs. Walk-through."""
    c = Canvas(32, 32)
    rng = Rng(0x5C0EE)
    g = palette.STORMSLATE
    import math
    c.ellipse(16, 22, 13, 5.5, g["deep"])
    c.ellipse(16, 22, 9, 3.6, (30, 26, 44))
    c.ellipse(15, 21, 5, 2.2, (22, 19, 34))
    for ang in range(0, 360, 30):                       # radial scorch arms
        for i in range(3):
            x = 16 + round((10 + i * 2.4) * math.cos(math.radians(ang + i * 4)))
            y = 22 + round((4.4 + i * 1.2) * math.sin(math.radians(ang + i * 4)))
            c.put(x, y, g["deep"])
    for _ in range(10):                                 # ash fringe
        x, y = rng.range(3, 28), rng.range(17, 27)
        if not c.filled(x, y):
            c.put(x, y, (96, 92, 116) if rng.chance(0.5) else g["base"])
    for (fx, fy) in ((11, 19), (21, 24)):               # fulgurite nubs
        c.put(fx, fy - 1, palette.FULGURITE["light"])
        c.put(fx, fy, palette.FULGURITE["base"])
        c.put(fx + 1, fy, palette.FULGURITE["deep"])
    c.save(path)


def gen_skywatchrubble(path):
    """32x40: a broken carved Skywatch stone — snapped pillar stub with two
    surviving carved bands, a chipped top and a fallen block beside it."""
    c = Canvas(32, 40)
    stone = palette.SKYSTONE
    trim = palette.WARDEN["trim"]
    rng = Rng(0x5C0DD)
    top = 12
    for y in range(top, 38):
        for x in range(8, 20):
            tone = stone["base"]
            if x <= 9:
                tone = stone["light"]
            elif x >= 18:
                tone = stone["deep"]
            c.put(x, y, tone)
    for x in range(8, 20):                              # jagged break
        h = (x * 7 % 4)
        for k in range(h):
            c.put(x, top + k - 1, (0, 0, 0, 0))
        c.put(x, top + h - 1, stone["light"] if h < 2 else stone["base"])
    for band_y in (24, 30):                             # carved bands
        for x in range(8, 20):
            c.put(x, band_y, stone["deep"])
            if band_y == 24 and x % 4 == 1:
                c.put(x, band_y - 1, trim)              # surviving brass inlay
    for (px_, py_) in ((11, 20), (16, 27), (13, 34)):   # pits
        c.put(px_, py_, stone["deep"])
    c.rect(21, 32, 8, 6, stone["base"])                 # fallen block
    c.rect(21, 32, 8, 1, stone["light"])
    c.rect(21, 37, 8, 1, stone["deep"])
    c.put(24, 34, stone["deep"])
    _stone_patch(c, stone, rng, 16, 38, 13, 2)
    c.outline(palette.OUTLINE)
    c.save(path)


def gen_chargecrystal(path):
    """32x40: a small charged crystal formation — 3 violet nubs on slate,
    one leaning, with pale charge ticks. Faint light via setLight."""
    c = Canvas(32, 40)
    ramp = palette.STORMCRYSTAL
    ground = palette.STORMSLATE
    rng = Rng(0x5C0CC)
    c.ellipse(16, 36, 13, 3.6, ground["deep"])
    c.ellipse(11, 34.5, 6, 2.4, ground["base"])
    c.ellipse(22, 35, 5, 2.2, ground["base"])
    c.put(7, 33, ground["light"])
    from gen_objects import _blade
    _blade(c, 12, 35, 22, 3, ramp, lean_deg=-10, bright=True)
    _blade(c, 20, 35, 16, 3, ramp, lean_deg=14)
    _blade(c, 26, 37, 9, 2, ramp, lean_deg=28)
    c.outline(palette.OUTLINE)
    for _ in range(3):                                  # charge ticks
        x, y = rng.range(6, 26), rng.range(10, 22)
        if not c.filled(x, y):
            c.put(x, y, with_alpha(ramp["hi"], 170))
    c.save(path)


def gen_withershrub(path):
    """32x40: a dead twisted shrub — gnarled charred branches, curling side
    twigs, sparse grey leaf tufts over a root shadow."""
    c = Canvas(32, 40)
    wood = palette.CHARWOOD
    rng = Rng(0x5C0BB)
    import math
    fx, fy = 15.0, 38.0
    ang = -90.0
    tips = []
    for i in range(22):
        ang += rng.pick((-9, -4, 4, 9))
        fx += math.cos(math.radians(ang)) * 1.05
        fy += math.sin(math.radians(ang)) * 1.05
        x, y = round(fx), round(fy)
        w = 3 if i < 8 else (2 if i < 16 else 1)
        for dx in range(w):
            c.put(x + dx, y, wood["light"] if dx == 0 and i % 3 == 0
                  else wood["base"] if dx < w - 1 else wood["deep"])
        if i in (6, 13):
            tips.append((x, y, rng.pick((-1, 1))))
    for (tx, ty, sd) in tips:                           # curling side twigs
        bx, by = float(tx), float(ty)
        bang = -60.0 * sd
        for k in range(8):
            bang += rng.pick((-8, 8))
            bx += math.cos(math.radians(bang)) * sd
            by += math.sin(math.radians(bang))
            c.put(round(bx), round(by), wood["base"])
            c.put(round(bx) + 1, round(by), wood["deep"])
        c.put(round(bx), round(by) - 1, wood["deep"])
    for _ in range(5):                                  # sparse grey tufts
        x, y = rng.range(6, 26), rng.range(14, 30)
        if c.filled(x, y - 1) and not c.filled(x, y):
            c.put(x, y, wood["hi"])
    c.ellipse(16, 38, 8, 2, wood["deep"])               # root shadow
    c.outline(palette.OUTLINE)
    c.save(path)


def gen_aurorashards(path):
    """32x36: small Aurora crystal cluster — teal and rose shards sharing a
    turf mound (the Shoals' restrained teal/rose pairing). Faint light."""
    c = Canvas(32, 36)
    ramp = dict(palette.AURORA)
    tuft = palette.CLOUDTURF
    from gen_objects import _blade
    c.ellipse(16, 32, 12, 3.2, tuft["deep"])
    c.ellipse(11, 30.5, 6, 2.2, tuft["tuft"])
    c.ellipse(21, 31, 5, 2, tuft["tuft"])
    _blade(c, 13, 31, 20, 3, ramp, lean_deg=-8, bright=True)
    _blade(c, 20, 31, 14, 3, ramp, lean_deg=12)
    _blade(c, 25, 33, 8, 2, ramp, lean_deg=26)
    teal = {"deep": (74, 138, 130), "base": (108, 196, 186),
            "light": (150, 220, 210), "hi": (208, 246, 240)}
    _blade(c, 8, 33, 11, 2, teal, lean_deg=-24, bright=True)
    c.outline(palette.OUTLINE)
    c.put(10, 10, with_alpha(ramp["hi"], 150))
    c.save(path)


def gen_starfall(path):
    """32x32: a fallen star fragment — a teal four-point stub half-buried in
    turf with a soft glow. Readable resource accent for the Shoals."""
    c = Canvas(32, 32)
    tuft = palette.CLOUDTURF
    teal = palette.PRISMSHARD
    c.ellipse(16, 26, 12, 3.4, tuft["deep"])
    c.ellipse(16, 25, 9, 2.6, tuft["tuft"])
    pts = [(16, 8), (19, 15), (26, 17), (20, 21), (22, 26), (16, 24),
           (10, 26), (12, 21), (6, 17), (13, 15)]
    for i in range(len(pts)):
        x0, y0 = pts[i]
        x1, y1 = pts[(i + 1) % len(pts)]
        steps = max(abs(x1 - x0), abs(y1 - y0), 1)
        for k in range(steps + 1):
            x = x0 + round((x1 - x0) * k / steps)
            y = min(26, y0 + round((y1 - y0) * k / steps))
            edge = k < 2 or k > steps - 2
            c.put(x, y, teal["deep"] if edge else teal["base"])
    c.ellipse(15, 16, 3, 3, teal["light"])
    c.put(14, 15, teal["hi"])
    c.put(19, 20, teal["deep"])
    c.outline(palette.OUTLINE)
    for (gx, gy, a) in ((8, 10, 150), (25, 12, 140), (16, 4, 160)):
        c.put(gx, gy, with_alpha(teal["hi"], a))
    c.save(path)


def gen_skyballoon(path):
    """32x52 sky oddity: a lost weather balloon — deflating pale envelope
    snagged on a crag, tiny instrument basket dangling, string taut."""
    c = Canvas(32, 52)
    silk = palette.WINDSILK
    iron = palette.IRONWORK
    wood = palette.WOOD
    stone = palette.SKYSTONE
    rng = Rng(0x5C088)
    _stone_patch(c, stone, rng, 12, 48, 9, 2.6)
    c.ellipse(18, 18, 9, 11, silk["base"])              # limp envelope
    c.ellipse(15, 15, 6, 7, silk["light"])
    c.ellipse(20, 22, 5, 5, silk["deep"])
    c.ellipse(16, 13, 2.5, 2, silk["hi"])
    for (px_, py_) in ((22, 12), (25, 17), (23, 24)):   # seam arcs
        c.put(px_, py_, silk["deep"])
    c.put(11, 22, silk["deep"])
    c.rect(17, 28, 3, 3, silk["deep"])                  # neck
    for i in range(10):                                 # string
        c.put(18 - i // 4, 31 + i, iron["deep"])
    c.rect(12, 41, 7, 6, wood["base"])                  # instrument basket
    c.rect(12, 41, 7, 1, wood["light"])
    c.rect(12, 46, 7, 1, wood["deep"])
    c.put(14, 43, wood["deep"])
    c.put(16, 44, iron["light"])                        # gauge glint
    c.rect(11, 47, 2, 2, iron["base"])                  # little wheels
    c.rect(18, 47, 2, 2, iron["base"])
    c.outline(palette.OUTLINE)
    c.save(path)


def gen_aeronautwreck(path):
    """48x56 sky oddity: a battered flying-machine wreck — snapped wooden
    wing spar with torn canvas, a bent brass propeller, scattered bolts."""
    c = Canvas(48, 56)
    wood = palette.WOOD
    canvas_c = (222, 214, 196)
    canvas_d = (188, 178, 158)
    trim = palette.WARDEN["trim"]
    trim_hi = palette.WARDEN["trim_hi"]
    iron = palette.IRONWORK
    rng = Rng(0x5C077)
    for i in range(30):                                 # snapped spar
        x = 6 + i
        y = 40 - i // 2
        w = 4 if i < 20 else 2
        for dx in range(w):
            c.put(x, y + dx, wood["light"] if dx == 0 and i % 4 == 0
                  else wood["base"] if dx < w - 1 else wood["deep"])
    for k in range(4):                                  # splinters
        c.put(36 + k, 26 - k // 2, wood["light"])
        c.put(36 + k, 27 - k // 2, wood["deep"])
    for y in range(30, 44):                             # torn canvas panel
        span = 10 - abs(y - 36) // 2
        for x in range(14, 14 + span):
            if x == 14 + span - 1 and (x + y) % 3 == 0:
                continue
            c.put(x, y, canvas_d if x == 14 else canvas_c)
    for (px_, py_) in ((18, 33), (21, 38)):             # patch seams
        c.put(px_, py_, canvas_d)
        c.put(px_ + 1, py_, canvas_d)
    c.ellipse(38, 44, 3, 3, iron["base"])               # bent propeller
    c.ellipse(37, 43, 1.4, 1.4, iron["light"])
    for i in range(9):
        c.put(38 + i, 44 - i - (i // 4), trim if i % 3 else trim_hi)
        c.put(38 + i, 45 - i - (i // 4), trim)
    for i in range(6):
        c.put(36 - i, 45 + i // 2, trim_hi if i % 2 else trim)
    for _ in range(5):                                  # scattered bolts
        x, y = rng.range(6, 42), rng.range(46, 52)
        c.put(x, y, iron["base"])
        c.put(x, y + 1, iron["deep"])
    c.ellipse(30, 50, 3, 2, iron["base"])               # guy-rope ring
    c.ellipse(30, 50, 1.6, 1, iron["deep"])
    c.save(path)


def gen_skyparcel(path):
    """32x30 sky oddity: a sky-mail parcel — small wooden crate, brass
    wing-stamp, wax seal, one strap. Subtle absurdity, not meme spam."""
    c = Canvas(32, 30)
    wood = palette.WOOD
    trim = palette.WARDEN["trim"]
    trim_hi = palette.WARDEN["trim_hi"]
    iron = palette.IRONWORK
    c.rect(6, 12, 20, 14, wood["base"])
    c.rect(6, 12, 20, 2, wood["light"])
    c.rect(6, 24, 20, 2, wood["deep"])
    for x in (10, 16, 22):                              # plank joints
        c.put(x, 15, wood["deep"])
        c.put(x, 20, wood["deep"])
    c.rect(14, 12, 4, 14, wood["deep"])                 # strap
    c.rect(15, 12, 1, 14, palette.WOOD["light"])
    for (wx, wy) in ((8, 17), (9, 16), (10, 17), (20, 17), (21, 16), (22, 17)):
        c.put(wx, wy, trim)                             # brass wing-stamp
    c.put(11, 17, trim_hi)
    c.put(19, 17, trim_hi)
    c.ellipse(16, 19, 2, 2, trim)                       # wax seal
    c.put(15, 18, trim_hi)
    c.put(9, 8, iron["light"])                          # wayward feather
    c.put(8, 9, iron["base"])
    c.put(10, 9, iron["base"])
    c.outline(palette.OUTLINE)
    c.save(path)


def gen_withershrub_icon(path):
    """Dense picked-shrub portrait built like vanilla's broad sapling crown."""
    c = Canvas(32, 32)
    wood = palette.CHARWOOD

    def thick_line(x0, y0, x1, y1, radius, color):
        for ox in range(-radius, radius + 1):
            for oy in range(-radius, radius + 1):
                if ox * ox + oy * oy <= radius * radius:
                    c.line(x0 + ox, y0 + oy, x1 + ox, y1 + oy, color)

    # Five distinct gnarled limbs share a heavy root crown, leaving readable
    # notches between them instead of becoming a uniformly fat twig.
    c.ellipse(16, 27, 10, 3, wood["deep"])
    c.ellipse(14, 25, 8, 3, wood["base"])
    limbs = ((16, 26, 10, 14, 7, 5), (15, 25, 19, 14, 25, 6),
             (16, 22, 8, 19, 4, 13), (18, 23, 25, 20, 29, 14),
             (16, 19, 14, 9, 17, 3))
    for x0, y0, x1, y1, x2, y2 in limbs:
        thick_line(x0, y0, x1, y1, 2, wood["deep"])
        thick_line(x1, y1, x2, y2, 2, wood["deep"])
    for x0, y0, x1, y1, x2, y2 in limbs:
        thick_line(x0 - 1, y0 - 1, x1 - 1, y1 - 1, 1, wood["base"])
        thick_line(x1 - 1, y1 - 1, x2 - 1, y2 - 1, 1, wood["light"])
    c.outline(palette.OUTLINE)
    # Cut ends, bark scars and pale dead-leaf flecks.
    for x, y in ((7, 5), (25, 6), (4, 13), (29, 14), (17, 3)):
        c.put(x, y, wood["hi"])
        c.put(x + 1, y + 1, wood["deep"])
    c.put(10, 17, wood["hi"])
    c.put(20, 13, wood["deep"])
    c.put(13, 24, wood["light"])
    c.put(21, 22, wood["hi"])
    c.save(path)


def gen_aeronautwreck_icon(path):
    """A compact wreck portrait using airvessel's overlapping body masses."""
    c = Canvas(32, 32)
    canvas = palette.WINDSILK
    wood = palette.WOOD
    iron = palette.IRONWORK
    trim = palette.WARDEN
    # Torn canvas wing: large rounded upper-left plane and deep lower-right
    # plane, with an asymmetric broken trailing edge.
    c.ellipse(15, 16, 13, 9, canvas["deep"])
    c.ellipse(13, 13, 10, 7, canvas["base"])
    c.ellipse(10, 10, 6, 4, canvas["light"])
    c.rect(5, 17, 21, 7, canvas["deep"])
    c.rect(6, 17, 15, 4, canvas["base"])
    # Heavy snapped spar crosses the wing; the lit edge stays upper-left.
    for i in range(24):
        x = 4 + i
        y = 25 - i // 2
        c.rect(x, y, 3, 4, wood["deep"])
        c.put(x, y, wood["light"])
        c.put(x + 1, y + 1, wood["base"])
    # Bent brass propeller and iron hub remain clear of the canvas mass.
    c.ellipse(24, 21, 4, 4, iron["base"])
    c.ellipse(23, 20, 2, 2, iron["light"])
    for x0, y0, x1, y1 in ((23, 20, 29, 7), (23, 20, 10, 27)):
        for off in (-1, 0, 1):
            c.line(x0 + off, y0, x1 + off, y1, trim["trim"])
    c.outline(palette.OUTLINE)
    # Tears, stitches, bolt heads and a single brass glint.
    c.line(8, 15, 13, 18, canvas["deep"])
    c.line(17, 9, 20, 14, canvas["deep"])
    c.put(10, 16, canvas["hi"])
    c.put(18, 12, canvas["light"])
    c.put(23, 20, trim["trim_hi"])
    c.put(7, 24, iron["hi"])
    c.put(27, 25, iron["base"])
    c.save(path)


def gen_prop_icons(items_dir):
    """32x32 item icons for the prop objects (GameObject.generateItemTexture
    resolves items/<stringID> for any object registered with createItem=true;
    without these the crafting menu shows the engine error texture — the same
    bug class b90dc2a fixed for the trees). Miniatures cropped and scaled
    from the freshly generated object sprites, like gen_set_icons."""
    import os
    from PIL import Image

    def mini_from(src_name, box, out_name):
        base = os.path.dirname(items_dir)
        src = os.path.join(base, "objects", src_name)
        im = Image.open(src).convert("RGBA").crop(box)
        w, h = im.size
        scale = min(28 / w, 28 / h, 1.0)
        nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
        im = im.resize((nw, nh), Image.NEAREST)
        icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        icon.alpha_composite(im, ((32 - nw) // 2, (32 - nh) // 2))
        icon.save(os.path.join(items_dir, out_name))

    mini_from("skywatchtelescope.png", (0, 24, 32, 72), "skywatchtelescope.png")
    mini_from("skywatchastrolabe.png", (2, 16, 30, 52), "skywatchastrolabe.png")
    mini_from("stormscreed.png", (2, 15, 30, 29), "stormscreed.png")
    mini_from("skywatchrubble.png", (4, 8, 30, 40), "skywatchrubble.png")
    mini_from("chargecrystal.png", (2, 10, 30, 40), "chargecrystal.png")
    gen_withershrub_icon(os.path.join(items_dir, "withershrub.png"))
    mini_from("aurorashards.png", (2, 8, 30, 36), "aurorashards.png")
    mini_from("starfall.png", (4, 2, 28, 30), "starfall.png")
    mini_from("skyballoon.png", (2, 4, 30, 52), "skyballoon.png")
    gen_aeronautwreck_icon(os.path.join(items_dir, "aeronautwreck.png"))
    mini_from("skyparcel.png", (2, 4, 30, 28), "skyparcel.png")


def gen_all(dir_path):
    gen_skywatchtelescope(f"{dir_path}/skywatchtelescope.png")
    gen_skywatchastrolabe(f"{dir_path}/skywatchastrolabe.png")
    gen_stormscreed(f"{dir_path}/stormscreed.png")
    gen_skywatchrubble(f"{dir_path}/skywatchrubble.png")
    gen_chargecrystal(f"{dir_path}/chargecrystal.png")
    gen_withershrub(f"{dir_path}/withershrub.png")
    gen_aurorashards(f"{dir_path}/aurorashards.png")
    gen_starfall(f"{dir_path}/starfall.png")
    gen_skyballoon(f"{dir_path}/skyballoon.png")
    gen_aeronautwreck(f"{dir_path}/aeronautwreck.png")
    gen_skyparcel(f"{dir_path}/skyparcel.png")
    gen_skycrate(f"{dir_path}/skycrate.png")
    gen_skycratedebris(f"{dir_path}/skycratedebris.png")


def gen_skycrate(path):
    """192x64 salvage crate for the Skyreach, on vanilla `crates.png`'s layout:
    six variants across, each 32 wide, all BOTTOM-ANCHORED (vanilla's sit at
    y=24..60 and end on the same ground line, which is what makes a scattered
    field of them read as standing on the floor rather than floating).

    Vanilla's crates are warm brown cooperage. These are what falls off a sky
    freighter: pale skystone-grey boards lashed with windsilk cord and cornered
    in brass. Measured target is vanilla's own 440-752 opaque px per variant.
    """
    wood = palette.WOOD
    silk = palette.WINDSILK
    iron = palette.IRONWORK
    trim = palette.WARDEN["trim"]
    trim_hi = palette.WARDEN["trim_hi"]
    c = Canvas(192, 64)

    def box(ox, w, h, staves=True):
        """A crate body ending on the ground line at y=60.

        Vanilla's construction, measured off `crates.png`: the LID is a large
        lit face in the same wood covering roughly the top third, and the front
        below it is the base tone with vertical stave shadows. A first cut put a
        thin bright windsilk band there instead and the crates read as brown
        boxes with a white stripe -- the lid has to be a surface, not a line.
        """
        top = 60 - h
        lid = max(5, h // 3)
        c.rect(ox, top, w, h, wood["base"])
        c.rect(ox, top, w, lid, wood["light"])       # the lit lid face
        c.rect(ox + 1, top + 1, w - 2, 1, silk["deep"])   # its sunlit front lip
        c.rect(ox, top + lid, w, 1, wood["deep"])    # lid/front joint
        c.rect(ox, 57, w, 3, wood["deep"])           # shadowed foot
        if staves:
            for sx in range(ox + 3, ox + w - 2, 4):
                c.rect(sx, top + lid + 2, 1, h - lid - 5, wood["deep"])
        for cx in (ox, ox + w - 1):                  # brass corner posts
            c.rect(cx, top, 1, h, trim)
            c.put(cx, top + 1, trim_hi)
        return top

    def cord(ox, w, top, h):
        """One lashing across the belly.

        Drawn DARK. Vanilla's crates carry no bright band at all; a windsilk
        strap in `silk["light"]` read as a white stripe painted across a brown
        box and was the loudest thing on the sheet. A rope reads as a rope by
        being a shadow line with one lit pixel on its upper edge.
        """
        y = top + h // 2 + 2
        c.rect(ox, y, w, 1, wood["deep"])
        c.rect(ox, y + 1, w, 1, palette.OUTLINE)
        c.put(ox + w // 2, y, silk["deep"])

    # v0 — a plain crate
    t = box(3, 26, 26); cord(3, 26, t, 26)
    # v1 — tall crate with a smaller one stacked on it
    t = box(35, 22, 30); cord(35, 22, t, 30)
    c.rect(39, t - 12, 14, 12, wood["base"])
    c.rect(39, t - 12, 14, 2, silk["deep"])
    c.rect(39, t - 2, 14, 2, wood["deep"])
    c.rect(39, t - 12, 1, 12, trim); c.rect(52, t - 12, 1, 12, trim)
    # v2 — a narrow upright
    t = box(70, 18, 26); cord(70, 18, t, 26)
    # v3 — two crates side by side
    t = box(99, 15, 28); cord(99, 15, t, 28)
    t2 = box(115, 13, 22); cord(115, 13, t2, 22)
    # v4 — a lashed bale. Its top is a LIT WOOD face like vanilla's barrels,
    # not a pale cap: a grey dome read as a mushroom on the first cut.
    c.ellipse(147, 46, 14, 13, wood["base"])
    c.ellipse(147, 42, 13, 8, wood["light"])
    c.ellipse(146, 41, 9, 5, wood["light"])
    c.ellipse(146, 40, 6, 3, silk["deep"])
    for sx in range(136, 159, 4):
        c.rect(sx, 47, 1, 10, wood["deep"])
    c.rect(133, 45, 28, 1, trim)                     # the lashing, one line
    c.put(147, 45, trim_hi)
    c.rect(134, 53, 26, 1, iron["base"])
    # v5 — a barrel. Sized against vanilla's own smallest crate (440 px at
    # 24x24); the first cut came out at 299 and would have read as a pebble in
    # a field of proper crates.
    c.ellipse(176, 47, 12, 12, wood["base"])
    c.ellipse(176, 41, 11, 6, wood["light"])
    c.ellipse(175, 40, 8, 4, wood["light"])
    c.ellipse(175, 39, 5, 2, silk["deep"])
    for sx in range(166, 188, 3):
        c.rect(sx, 44, 1, 13, wood["deep"])
    c.rect(164, 43, 24, 2, trim)
    c.put(176, 43, trim_hi)
    c.rect(164, 53, 24, 1, iron["base"])
    c.rect(165, 56, 22, 2, wood["deep"])

    c.outline(palette.OUTLINE)
    c.save(path)


def gen_skycratedebris(path):
    """192x32, one debris cell per crate variant — vanilla `cratesdebris.png`'s
    layout. Splinters of board and a curl of cut windsilk."""
    wood = palette.WOOD
    silk = palette.WINDSILK
    c = Canvas(192, 32)
    for v in range(6):
        ox = v * 32
        rng = Rng(0x5C0 + v * 977)
        cx, cy = ox + 16, 22
        c.blob(cx, cy, 5 - (v % 2), wood["base"], rng, lumps=3)
        c.blob(cx - 2, cy - 2, 2, wood["light"], rng, lumps=2)
        for _ in range(3):
            c.put(cx + rng.range(-6, 6), cy + rng.range(-4, 4), wood["deep"])
        c.put(cx + 3, cy - 3, silk["light"])
        c.put(cx + 4, cy - 2, silk["deep"])
    c.outline(palette.OUTLINE)
    c.save(path)
