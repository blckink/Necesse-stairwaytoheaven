"""Sky Arsenal — the craftable weapon tier and its bestiary icons.

Everything here is deterministic (fixed seeds, no time/random imports), so a
regeneration is byte-identical.

Sizes were measured off the vanilla analogue each piece answers to
(`python3 tools/size_audit.py` holds the pairs and enforces the ratio):

  items/skyreave.png        <- items/quartzglaive.png        (440 opaque px)
  items/thunderhead.png     <- items/tungstengreatbow.png    (328)
  items/prismcaller.png     <- items/quartzstaff.png         (352)
  items/skywatchwhistle.png <- items/batcage.png             (400)
  items/stormdisc.png       <- items/tungstenboomerang.png   (464)

The three `player/weapons/` sheets are the mid-attack sprites the engine
rotates around (attackXOffset, attackYOffset); their canvas sizes mirror the
vanilla weapon of the same class exactly (glaive 108x92 -> ours 96x96 with a
centre pivot, greatbow 24x64, staff 50x50). The boomerang deliberately has
none: ThrowToolItem.getAttackSprite returns null, and vanilla ships no
player/weapons/tungstenboomerang.png either.

Projectile sprites follow ProjectileRegistry's own convention
(projectiles/<name>.png plus an optional <name>_shadow.png at alpha 70/30,
measured off projectiles/frostboomerang_shadow.png).
"""

from px import Canvas, Rng, with_alpha
import palette


OUT = palette.OUTLINE


def _finish(c):
    c.outline(OUT)
    return c


# ---------------------------------------------------------------------------
# shared shapes


def _crystal_blade(c, x0, y0, x1, y1, half, ramp, edge_key="hi"):
    """A tapered blade drawn silhouette-first.

    The generic outline pass eats 1-2px diagonals (the trap that shipped the
    first Tempest Edge as a hairline), so the dark mass goes down first and the
    bright core is laid on top of it.
    """
    steps = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(steps + 1):
        t = i / max(steps, 1)
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        w = max(1, round(half * (1.0 - t * 0.75)))
        for k in range(-w - 1, w + 2):
            c.put(x + k, y, OUT)
    for i in range(steps + 1):
        t = i / max(steps, 1)
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        w = max(1, round(half * (1.0 - t * 0.75)))
        for k in range(-w, w + 1):
            if k <= -w + 0:
                tone = ramp[edge_key]
            elif k >= w:
                tone = ramp["deep"]
            else:
                tone = ramp["base"] if (k + i) % 3 else ramp["light"]
            c.put(x + k, y, tone)


def _haft(c, x0, y0, x1, y1, wood, thick=2):
    """Wrapped wooden shaft: two-tone, with binding rings every few pixels."""
    steps = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(steps + 1):
        t = i / max(steps, 1)
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        for k in range(thick):
            tone = wood["light"] if k == 0 else (wood["base"] if k < thick - 1 else wood["deep"])
            if i % 7 == 3:
                tone = wood["deep"]
            c.put(x + k, y, tone)


# ---------------------------------------------------------------------------
# 1. Skyreave — Aetherium glaive on a cloudwood haft


def _skyreave(c, cx, cy, reach, half):
    """Double-headed glaive, drawn on the leading diagonal like vanilla's."""
    wood = palette.CLOUDWOOD if hasattr(palette, "CLOUDWOOD") else palette.WOOD
    steel = palette.AETHERIUM
    ful = palette.FULGURITE
    # haft, corner to corner through the pivot
    _haft(c, cx - reach + 8, cy + reach - 8, cx + reach - 9, cy - reach + 9, wood, thick=3)
    # brass collars where the blades seat
    for sign in (-1, 1):
        bx = cx + sign * (reach - 10)
        by = cy - sign * (reach - 10)
        for d in range(-3, 4):
            c.put(bx + d, by + d, ful["base"] if d % 2 else ful["light"])
            c.put(bx + d + 1, by + d, ful["deep"])
    # the two crescent blades
    _crystal_blade(c, cx + reach - 12, cy - reach + 12, cx + reach - 1, cy - reach + 1, half, steel)
    _crystal_blade(c, cx + reach - 12, cy - reach + 12, cx + reach - 3, cy - reach + 13, half - 1, steel)
    _crystal_blade(c, cx - reach + 12, cy + reach - 12, cx - reach + 1, cy + reach - 1, half, steel)
    _crystal_blade(c, cx - reach + 12, cy + reach - 12, cx - reach + 3, cy + reach - 13, half - 1, steel)
    # storm-crystal core at the grip
    st = palette.STORMCRYSTAL
    c.ellipse(cx, cy, 3.2, 3.2, st["base"])
    c.ellipse(cx - 1, cy - 1, 1.8, 1.8, st["light"])
    c.put(cx - 1, cy - 2, st["hi"])


def gen_skyreave_icon(path):
    """The inventory icon is its own composition, not a shrunk attack sprite.

    Vanilla's quartzglaive icon carries 440 opaque px in its 32 cell by
    showing ONE head large across the diagonal rather than the whole pole
    small; ours does the same.
    """
    c = Canvas(32, 32)
    wood = palette.CLOUDWOOD
    steel = palette.AETHERIUM
    ful = palette.FULGURITE
    st = palette.STORMCRYSTAL
    # haft up the lower-left diagonal, five px across so it reads at 1x
    for off in range(3):
        _haft(c, 2 + off, 29, 15 + off, 16, wood, thick=4)
    # brass collar, three rows deep
    for row in range(3):
        for d in range(-5, 6):
            tone = ful["light"] if row == 0 else (ful["base"] if row == 1 else ful["deep"])
            c.put(17 + d + row, 15 + d, tone)
    # one broad crescent head filling the upper right
    _crystal_blade(c, 18, 14, 30, 2, 6, steel)
    _crystal_blade(c, 18, 15, 30, 12, 5, steel)
    _crystal_blade(c, 17, 13, 20, 1, 4, steel)
    c.ellipse(24, 8, 4.4, 4.0, steel["base"])       # web between the blades
    c.ellipse(23, 7, 2.6, 2.4, steel["light"])
    c.put(22, 6, steel["hi"])
    # storm-crystal counterweight at the butt
    c.ellipse(4, 28, 4.0, 3.6, st["base"])
    c.ellipse(3, 27, 2.4, 2.2, st["light"])
    c.put(3, 26, st["hi"])
    c.save(path)


def gen_skyreave_attack(path):
    """96x96, pivot at (48, 48) — matches the item's attackXOffset/YOffset.

    Vanilla's quartzglaive attack sheet carries 1192 opaque px across 108x92:
    a POLE, thick enough to read while it spins, with a full head at each end.
    That mass is the target, so the haft is seven px across and both crescents
    are drawn at the icon's scale rather than shrunk to fit.
    """
    c = Canvas(96, 96)
    wood = palette.CLOUDWOOD
    steel = palette.AETHERIUM
    ful = palette.FULGURITE
    st = palette.STORMCRYSTAL
    # haft, corner to corner through the pivot, seven px across
    for off in range(6):
        _haft(c, 16 + off, 80, 74 + off, 22, wood, thick=3)
    # brass collars where the blades seat
    for sign in (-1, 1):
        bx = 48 + sign * 26
        by = 48 - sign * 26
        for row in range(4):
            for d in range(-6, 7):
                tone = ful["light"] if row < 2 else ful["deep"]
                c.put(bx + d + row, by + d, tone)
    # two crescent heads
    for sign in (-1, 1):
        rx, ry = 48 + sign * 28, 48 - sign * 28
        tx, ty = 48 + sign * 45, 48 - sign * 45
        _crystal_blade(c, rx, ry, tx, ty, 7, steel)
        _crystal_blade(c, rx, ry, tx + sign * 2, ty + sign * 14, 6, steel)
        _crystal_blade(c, rx - sign * 2, ry - sign * 2, tx - sign * 14, ty - sign * 2, 5, steel)
        c.ellipse(48 + sign * 36, 48 - sign * 36, 6.5, 6.0, steel["base"])
        c.ellipse(48 + sign * 36 - 2, 48 - sign * 36 - 2, 3.8, 3.4, steel["light"])
        c.put(48 + sign * 36 - 3, 48 - sign * 36 - 4, steel["hi"])
    # storm-crystal core at the grip
    c.ellipse(48, 48, 6.0, 6.0, st["base"])
    c.ellipse(46, 46, 3.4, 3.4, st["light"])
    c.put(45, 44, st["hi"])
    c.save(path)


# ---------------------------------------------------------------------------
# 2. Thunderhead — seraphwood greatbow, windsilk-strung


def _thunderhead(c, w, h):
    """Shared by the attack sheet. Vanilla's greatbow attack sprite carries
    584 opaque px in 24x64, so the limb is drawn six px across, not three."""
    wood = palette.SERAPHWOOD
    silk = palette.WINDSILK
    steel = palette.AETHERIUM
    ful = palette.FULGURITE
    top, bot = 1, h - 2
    span = bot - top
    belly = w - 9            # how far right the limb bows
    pts = []
    for i in range(span + 1):
        t = i / span
        bow = 1.0 - abs(t * 2 - 1) ** 1.6
        x = 3 + round(belly * bow)
        pts.append((x, top + i))
    for (x, y) in pts:                        # silhouette mass first
        for k in range(-1, 7):
            c.put(x + k, y, OUT)
    for idx, (x, y) in enumerate(pts):        # six-wide limb
        c.put(x, y, wood["hi"])
        c.put(x + 1, y, wood["light"])
        c.put(x + 2, y, wood["light"])
        c.put(x + 3, y, wood["base"])
        c.put(x + 4, y, wood["base"])
        c.put(x + 5, y, wood["deep"])
        if idx % 5 == 2:                      # fulgurite banding
            c.put(x, y, ful["light"])
            c.put(x + 1, y, ful["base"])
            c.put(x + 2, y, ful["deep"])
    for y in (top, bot):                      # aetherium nocks
        for d in range(-1, 6):
            c.put(3 + d, y, steel["base"])
            c.put(3 + d, y + (1 if y == top else -1), steel["deep"])
        c.put(3, y, steel["hi"])
        c.put(4, y, steel["light"])
    for y in range(top, bot + 1):             # windsilk string
        c.put(3, y, silk["light"] if y % 3 else silk["hi"])
    for y in range(h // 2 - 6, h // 2 + 7):   # grip wrap on the riser
        c.put(2 + belly, y, silk["deep"])
        c.put(3 + belly, y, silk["base"])
        c.put(4 + belly, y, silk["light"])
        c.put(5 + belly, y, silk["base"])
        c.put(6 + belly, y, silk["deep"])


def gen_thunderhead_icon(path):
    """Vanilla's tungstengreatbow icon is a 18x32 bow at 328 opaque px — a
    greatbow limb is THICK, so ours is drawn four pixels across with a nocked
    arrow beside it rather than as a thin arc."""
    c = Canvas(32, 32)
    wood = palette.SERAPHWOOD
    silk = palette.WINDSILK
    steel = palette.AETHERIUM
    ful = palette.FULGURITE
    top, bot, belly = 1, 30, 15
    span = bot - top
    pts = []
    for i in range(span + 1):
        t = i / span
        bow = 1.0 - abs(t * 2 - 1) ** 1.6
        pts.append((6 + round(belly * bow), top + i))
    for (x, y) in pts:                       # silhouette mass first
        for k in range(-1, 7):
            c.put(x + k, y, OUT)
    for idx, (x, y) in enumerate(pts):       # six-wide greatbow limb
        c.put(x, y, wood["hi"])
        c.put(x + 1, y, wood["light"])
        c.put(x + 2, y, wood["light"])
        c.put(x + 3, y, wood["base"])
        c.put(x + 4, y, wood["base"])
        c.put(x + 5, y, wood["deep"])
        if idx % 5 == 2:
            c.put(x, y, ful["light"])
            c.put(x + 1, y, ful["base"])
            c.put(x + 2, y, ful["deep"])
    for y in (top, bot):                     # aetherium nocks
        for d in range(-1, 6):
            c.put(6 + d, y, steel["base"])
            c.put(6 + d, y + (1 if y == top else -1), steel["deep"])
        c.put(6, y, steel["hi"])
        c.put(7, y, steel["light"])
    for y in range(top, bot + 1):            # windsilk string
        c.put(6, y, silk["light"] if y % 3 else silk["hi"])
    for y in range(11, 21):                  # grip wrap on the riser
        c.put(5 + belly, y, silk["deep"])
        c.put(6 + belly, y, silk["base"])
        c.put(7 + belly, y, silk["light"])
        c.put(8 + belly, y, silk["deep"])
    for x in range(1, 7):                    # nocked arrow behind the string
        c.put(x, 15, wood["light"])
        c.put(x, 16, wood["base"])
        c.put(x, 17, wood["deep"])
    for d in range(-2, 3):                   # arrowhead
        c.put(1 - abs(d) // 2, 16 + d, steel["base"])
    c.put(0, 16, steel["hi"])
    _finish(c).save(path)


def gen_thunderhead_attack(path):
    """24x64 like vanilla's tungstengreatbow attack sheet."""
    c = Canvas(24, 64)
    _thunderhead(c, 24, 64)
    _finish(c).save(path)


# ---------------------------------------------------------------------------
# 3. Prismcaller — prismwood staff with a prismshard head


def _prismcaller(c, tip_x, tip_y, foot_x, foot_y, head_r):
    wood = palette.PRISMWOOD if hasattr(palette, "PRISMWOOD") else palette.WOOD
    pr = palette.PRISMSHARD
    st = palette.STORMCRYSTAL
    # shaft, silhouette-first (a 2px diagonal would be eaten otherwise)
    steps = max(abs(foot_x - tip_x), abs(foot_y - tip_y))
    for i in range(steps + 1):
        t = i / max(steps, 1)
        x = round(tip_x + (foot_x - tip_x) * t)
        y = round(tip_y + (foot_y - tip_y) * t)
        for k in range(-2, 6):
            c.put(x + k, y, OUT)
    for i in range(steps + 1):
        t = i / max(steps, 1)
        x = round(tip_x + (foot_x - tip_x) * t)
        y = round(tip_y + (foot_y - tip_y) * t)
        c.put(x, y, wood["hi"])
        c.put(x + 1, y, wood["light"])
        c.put(x + 2, y, wood["base"])
        c.put(x + 3, y, wood["base"])
        c.put(x + 4, y, wood["deep"])
        if i % 8 == 4:
            c.put(x, y, st["light"])
            c.put(x + 1, y, st["base"])
            c.put(x + 2, y, st["deep"])
    # crown: a ring of shards around a floating prism
    for a in range(8):
        ang = a * 45
        dx = round(head_r * 0.95 * _cos(ang))
        dy = round(head_r * 0.95 * _sin(ang))
        c.ellipse(tip_x + dx, tip_y + dy, 2.0, 2.0, pr["deep"])
    for a in range(8):
        ang = a * 45
        dx = round(head_r * 0.95 * _cos(ang))
        dy = round(head_r * 0.95 * _sin(ang))
        c.ellipse(tip_x + dx, tip_y + dy, 1.4, 1.4, pr["base"] if a % 2 else pr["light"])
    c.ellipse(tip_x, tip_y, head_r * 0.62, head_r * 0.62, pr["deep"])
    c.ellipse(tip_x, tip_y, head_r * 0.45, head_r * 0.45, pr["base"])
    c.ellipse(tip_x - 1, tip_y - 1, head_r * 0.26, head_r * 0.26, pr["light"])
    c.put(tip_x - 1, tip_y - 2, pr["hi"])
    c.put(tip_x + 1, tip_y + 1, pr["teal"])


_COS = (1.0, 0.7071, 0.0, -0.7071, -1.0, -0.7071, 0.0, 0.7071)


def _cos(deg):
    return _COS[(deg // 45) % 8]


def _sin(deg):
    return _COS[((deg // 45) + 6) % 8]


def gen_prismcaller_icon(path):
    c = Canvas(32, 32)
    _prismcaller(c, 11, 10, 26, 30, 9.0)
    _finish(c).save(path)


def gen_prismcaller_attack(path):
    """50x50 like vanilla's quartzstaff attack sheet."""
    c = Canvas(50, 50)
    _prismcaller(c, 15, 13, 42, 45, 11.0)
    _finish(c).save(path)


def gen_prismbolt(path):
    """18x18, the size ProjectileRegistry's own bolt sprites use.

    The shadow is deliberately NOT generated: the registration reuses vanilla's
    shared projectiles/bolt_shadow, exactly as QuartzBoltProjectile does.
    """
    c = Canvas(18, 18)
    pr = palette.PRISMSHARD
    cx = cy = 9
    for a in range(8):                        # radiating shard points
        ang = a * 45
        dx, dy = _cos(ang), _sin(ang)
        length = 9 if a % 2 == 0 else 8
        for i in range(length):
            w = 2 if i < length - 4 else (1 if i < length - 2 else 0)
            for k in range(-w, w + 1):
                c.put(cx + round(dx * i) - round(dy * k), cy + round(dy * i) + round(dx * k),
                      pr["deep"] if i > length - 3 else pr["base"])
    c.ellipse(cx, cy, 6.0, 6.0, pr["base"])
    c.ellipse(cx, cy, 4.2, 4.2, pr["light"])
    c.ellipse(cx - 1, cy - 1, 2.4, 2.4, pr["hi"])
    c.put(cx + 2, cy + 2, pr["teal"])
    c.save(path)


# ---------------------------------------------------------------------------
# 4. Skywatch Whistle — the summon focus


def gen_skywatchwhistle_icon(path):
    """Answers to vanilla's batcage summon focus (400 opaque px in a 32 cell):
    a solid object filling most of the icon, not a thin instrument."""
    c = Canvas(32, 32)
    steel = palette.AETHERIUM
    ful = palette.FULGURITE
    silk = palette.WINDSILK
    st = palette.STORMCRYSTAL
    # brass bell, wide-mouthed, opening up-right
    c.ellipse(19, 19, 10.0, 9.0, ful["deep"])
    c.ellipse(19, 18, 8.6, 7.6, ful["base"])
    c.ellipse(16, 14, 5.0, 3.8, ful["light"])
    c.put(14, 11, ful["hi"])
    c.put(15, 11, ful["hi"])
    for i in range(8):                        # fluted ribs
        c.put(13 + i * 2, 22 + (i % 2), ful["deep"])
    # aetherium mouthpiece and stem, lower left
    for i in range(10):
        c.put(4 + i, 26 - i, steel["light"])
        c.put(5 + i, 26 - i, steel["base"])
        c.put(6 + i, 26 - i, steel["deep"])
    c.ellipse(4, 27, 3.4, 3.0, steel["base"])
    c.ellipse(3, 26, 2.0, 1.8, steel["light"])
    c.put(3, 25, steel["hi"])
    # bell throat: the storm the whistle keeps
    c.ellipse(23, 13, 3.6, 3.2, OUT)
    c.ellipse(23, 13, 2.6, 2.2, st["base"])
    c.ellipse(23, 13, 1.4, 1.2, st["light"])
    c.put(22, 12, st["hi"])
    for i in range(7):                        # windsilk lanyard down the side
        c.put(9 + i, 12 + (i % 3), silk["base"])
        c.put(9 + i, 13 + (i % 3), silk["deep"])
    for (lx, ly) in ((28, 7), (26, 4), (30, 10)):   # escaping motes
        c.put(lx, ly, st["light"])
        c.put(lx + 1, ly, st["base"])
        c.put(lx, ly + 1, st["deep"])
    _finish(c).save(path)


# ---------------------------------------------------------------------------
# 5. Stormdisc — thrown ring


def _stormdisc(c, cx, cy, r):
    steel = palette.AETHERIUM
    st = palette.STORMCRYSTAL
    cin = palette.GHOSTFLAME
    c.ellipse(cx, cy, r, r, steel["deep"])
    c.ellipse(cx, cy, r - 1.2, r - 1.2, steel["base"])
    c.ellipse(cx - 1, cy - 1, r - 2.6, r - 2.6, steel["light"])
    c.ellipse(cx, cy, r - 4.2, r - 4.2, st["deep"])
    c.ellipse(cx, cy, r - 5.4, r - 5.4, (0, 0, 0, 0))
    # four swept cutting vanes on the rim
    for a in (0, 2, 4, 6):
        ang = a * 45
        dx, dy = _cos(ang), _sin(ang)
        px_ = round(cx + dx * r)
        py_ = round(cy + dy * r)
        for i in range(4):
            c.put(px_ + round(dy * i), py_ - round(dx * i), steel["hi"] if i < 2 else steel["light"])
            c.put(px_ + round(dy * i) - round(dx), py_ - round(dx * i) - round(dy), steel["base"])
    # cinderpearl heart, green flame
    c.ellipse(cx, cy, 2.4, 2.4, cin["deep"])
    c.ellipse(cx, cy, 1.6, 1.6, cin["glow"])
    c.put(cx, cy - 1, cin["core"])
    c.put(cx - 3, cy - 3, st["hi"])


def gen_stormdisc_icon(path):
    c = Canvas(32, 32)
    _stormdisc(c, 16, 16, 13.0)
    _finish(c).save(path)


def gen_stormdisc_projectile(path):
    c = Canvas(32, 32)
    _stormdisc(c, 16, 16, 13.0)
    _finish(c).save(path)


def gen_shadow_from(src_path, out_path):
    """Vanilla projectile shadows are the silhouette in black at alpha 70 with
    a one-pixel alpha-30 skirt (measured on projectiles/frostboomerang_shadow
    and projectiles/bolt_shadow)."""
    from PIL import Image
    src = Image.open(src_path).convert("RGBA")
    w, h = src.size
    sp = src.load()
    out = Canvas(w, h)
    for x in range(w):
        for y in range(h):
            if sp[x, y][3] > 24:
                out.put(x, y, (0, 0, 0, 70))
    skirt = []
    for x in range(w):
        for y in range(h):
            if out.get(x, y)[3] == 0:
                for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                    if 0 <= nx < w and 0 <= ny < h and out.get(nx, ny)[3] == 70:
                        skirt.append((x, y))
                        break
    for x, y in skirt:
        out.put(x, y, (0, 0, 0, 30))
    out.save(out_path)


# ---------------------------------------------------------------------------
# Bestiary icons (32x32, the size MobRegistry.loadMobIcons reads)
#
# These are ours even though the mobs wear vanilla body sheets: loadIcon is
# hard-wired to mobs/icons/<our stringID> and there is no setter, so the
# alternative would be shipping a copy of vanilla art, which this repository
# does not do.


def gen_rimesentry_icon(path):
    c = Canvas(32, 32)
    st = palette.AETHERIUM
    ful = palette.FULGURITE
    # a squat ice-crystal turret bud on a stubby plinth
    c.ellipse(16, 22, 10.0, 7.0, st["deep"])
    c.ellipse(16, 21, 8.4, 5.6, st["base"])
    c.ellipse(13, 19, 4.6, 3.0, st["light"])
    for i in range(13):                       # crown spike
        w = max(1, 6 - i // 2)
        for k in range(-w, w + 1):
            c.put(16 + k, 18 - i, st["base"] if k else st["light"])
        if i > 3:
            c.put(16 - w, 18 - i, st["deep"])
    c.put(15, 5, st["hi"])
    c.put(16, 6, st["hi"])
    for (bx, by) in ((8, 25), (24, 25), (11, 27), (21, 27)):   # rime shards
        c.ellipse(bx, by, 2.4, 1.8, st["deep"])
        c.put(bx, by - 1, st["light"])
    c.rect(12, 28, 9, 2, ful["deep"])         # brass footing
    c.rect(12, 28, 9, 1, ful["base"])
    _finish(c).save(path)


def gen_auroraflake_icon(path):
    c = Canvas(32, 32)
    pr = palette.PRISMSHARD
    au = palette.AURORA
    cx = cy = 16
    # Vanilla's cryoflake icon carries 448 opaque px: a SOLID six-arm crystal,
    # not a hairline snowflake. Arms are three px across with two-px barbs.
    for a in range(8):
        ang = a * 45
        dx, dy = _cos(ang), _sin(ang)
        arm = 15 if a % 2 == 0 else 12
        for i in range(arm):
            w = 2 if i < arm - 5 else 1
            for k in range(-w, w + 1):
                c.put(cx + round(dx * i) - round(dy * k), cy + round(dy * i) + round(dx * k),
                      pr["base"] if i < arm - 3 else pr["deep"])
        for i in (arm // 3, arm // 2, arm - 4):          # barbs
            for j in (2, 3):
                c.put(cx + round(dx * i) + round(dy * j), cy + round(dy * i) - round(dx * j), pr["light"])
                c.put(cx + round(dx * i) - round(dy * j), cy + round(dy * i) + round(dx * j), pr["light"])
    c.ellipse(cx, cy, 7.6, 7.6, pr["base"])
    c.ellipse(cx, cy, 5.4, 5.4, pr["light"])
    c.ellipse(cx - 1, cy - 1, 2.8, 2.8, pr["hi"])
    c.put(cx + 3, cy + 3, au["teal"])
    _finish(c).save(path)


def gen_fenwraith_icon(path):
    c = Canvas(32, 32)
    gf = palette.GHOSTFLAME
    sh = palette.SHADE
    # hooded wraith head and shoulders, hollow-eyed
    c.ellipse(16, 14, 9.0, 10.0, gf["deep"])
    c.ellipse(16, 13, 7.4, 8.4, gf["glow"])
    c.ellipse(13, 9, 3.6, 3.0, gf["core"])
    for i in range(9):                        # shoulders and trailing veil
        w = 12 - max(0, i - 4) * 2
        ragged = (0, 0, 0, 0, 0, 1, 1, 2, 3)[i]   # hem frays downward, not in bands
        for k in range(-w, w + 1):
            if i >= 5 and (abs(k) + ragged) % 6 == 0:
                continue
            c.put(16 + k, 22 + i, gf["glow"] if abs(k) < w - 2 else gf["deep"])
    c.ellipse(16, 24, 8.0, 3.0, gf["core"])   # lit shoulder line
    c.ellipse(11, 14, 2.4, 3.0, sh["deep"])   # eye sockets
    c.ellipse(21, 14, 2.4, 3.0, sh["deep"])
    c.put(11, 14, sh["eye"])
    c.put(21, 14, sh["eye"])
    for (ax, ay) in ((5, 20), (27, 20)):      # raised claws
        for i in range(4):
            c.put(ax, ay - i, gf["glow"])
            c.put(ax + 1, ay - i, gf["deep"])
    _finish(c).save(path)


def gen_cindercantor_icon(path):
    c = Canvas(32, 32)
    bone = palette.BONEASH
    gf = palette.GHOSTFLAME
    ash = palette.ASHSAND
    # masked skull over a hood, the way the bestiary crops a caster's head.
    # Vanilla's ancientskeletonmage icon fills 28x28 at 632 opaque px, so the
    # head runs nearly the full cell and the collar carries real shoulders.
    c.ellipse(16, 14, 12.0, 12.0, bone["deep"])
    c.ellipse(16, 13, 10.4, 10.4, bone["base"])
    c.ellipse(12, 9, 5.0, 4.0, bone["light"])
    c.put(10, 6, bone["hi"])
    c.put(11, 6, bone["hi"])
    for d in range(-9, 10):                   # mask lattice
        if d % 3 == 0:
            for y in range(6, 23):
                c.put(16 + d, y, bone["light"])
    c.ellipse(11, 15, 3.0, 3.6, ash["deep"])  # eye holes with green fire
    c.ellipse(21, 15, 3.0, 3.6, ash["deep"])
    c.ellipse(11, 15, 1.4, 1.8, gf["glow"])
    c.ellipse(21, 15, 1.4, 1.8, gf["glow"])
    c.put(11, 14, gf["core"])
    c.put(21, 14, gf["core"])
    for i in range(8):                        # collar and shoulders
        w = 14 - max(0, i - 4)
        for k in range(-w, w + 1):
            c.put(16 + k, 24 + i, ash["base"] if (k + i) % 4 else ash["light"])
    c.rect(2, 23, 29, 2, ash["hi"])
    _finish(c).save(path)


def gen_watchmote_icon(path):
    c = Canvas(32, 32)
    st = palette.AETHERIUM
    sc = palette.STORMCRYSTAL
    cx = cy = 16
    for a in range(4):                        # four fat rime blades
        ang = a * 90
        dx, dy = _cos(ang), _sin(ang)
        for i in range(14):
            w = max(1, 4 - i // 4)
            for k in range(-w, w + 1):
                c.put(cx + round(dx * i) - round(dy * k), cy + round(dy * i) + round(dx * k),
                      st["base"] if i < 9 else st["deep"])
    for a in range(4):                        # short diagonal spurs
        ang = 45 + a * 90
        dx, dy = _cos(ang), _sin(ang)
        for i in range(7):
            c.put(cx + round(dx * i), cy + round(dy * i), st["light"])
    c.ellipse(cx, cy, 4.4, 4.4, sc["base"])
    c.ellipse(cx, cy, 3.0, 3.0, sc["light"])
    c.ellipse(cx - 1, cy - 1, 1.5, 1.5, sc["hi"])
    _finish(c).save(path)


# ---------------------------------------------------------------------------


def generate(out):
    gen_skyreave_icon(f"{out}/items/skyreave.png")
    gen_skyreave_attack(f"{out}/player/weapons/skyreave.png")
    gen_thunderhead_icon(f"{out}/items/thunderhead.png")
    gen_thunderhead_attack(f"{out}/player/weapons/thunderhead.png")
    gen_prismcaller_icon(f"{out}/items/prismcaller.png")
    gen_prismcaller_attack(f"{out}/player/weapons/prismcaller.png")
    gen_prismbolt(f"{out}/projectiles/prismbolt.png")
    gen_skywatchwhistle_icon(f"{out}/items/skywatchwhistle.png")
    gen_stormdisc_icon(f"{out}/items/stormdisc.png")
    gen_stormdisc_projectile(f"{out}/projectiles/stormdisc.png")
    gen_shadow_from(f"{out}/projectiles/stormdisc.png", f"{out}/projectiles/stormdisc_shadow.png")

    gen_rimesentry_icon(f"{out}/mobs/icons/rimesentry.png")
    # mobs/icons/auroraflake.png is NOT generated any more. The Aurora Flake
    # wears the player's own sheet, and a drawn icon drifts from a supplied
    # body the moment the body changes -- this one was still a pale
    # four-point star while the mob had become a violet eyed crystal.
    # tools/convert_biome_art.py cuts the icon out of mobs/auroraflake.png
    # instead. gen_auroraflake_icon stays below as the record of what the
    # generated one was.
    gen_fenwraith_icon(f"{out}/mobs/icons/fenwraith.png")
    gen_cindercantor_icon(f"{out}/mobs/icons/cindercantor.png")
    gen_watchmote_icon(f"{out}/mobs/icons/watchmote.png")
