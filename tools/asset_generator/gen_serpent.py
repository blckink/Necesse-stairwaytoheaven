"""Mist Serpent — segmented worm-mob sheet for the Mistsea.

CONSTRUCTION is taken from vanilla ``mobs/crystaldragon.png``; the sheet FORMAT
is taken from ``mobs/sandworm.png``. Those are two different things and the
first version of this file only did the second, which is why it read as a
beetle: an armoured capsule with a plated skull.

What actually makes the crystal dragon read as a creature, measured off its
sheet: a COMPACT rounded cranium carrying two very large dark eyes, and every
bit of the silhouette's width supplied by a fan of pale blades radiating out
from behind it. Its body segments are the same — a small core inside a big fan.
Ours are vapour fronds rather than crystal shards, in the Mistsea's blue-white
instead of the dragon's pink-violet: that is the "trimmed towards clouds" part.

Format is taken 1:1 from vanilla ``mobs/sandworm.png`` (measured, not guessed):

    mobs/mistserpent.png          128x384  = 2 cols x 6 rows of 64px cells
    mobs/mistserpent_mask.png      64x64   = ONE cell, head only
    mobs/mistserpent_shadow.png    44x24
    mobs/icons/mistserpent.png     32x32

Column 0, top to bottom: row 0 = head, rows 1-4 = body segments (each a little
narrower than the last, exactly like vanilla's chain), row 5 = tail.
Column 0 measured on sandworm.png: head 44x44 / 1328 opaque, segments 48x50 /
1476 and 44x50 / 1428, tail 36x34 / 780.

Column 1 rows 0-2 hold the small separate decoration pieces.  On sandworm those
are chitin leg-spines: two small pieces per cell (one around y8-26, one around
y36-54) packed against the LEFT of the cell, at three widths 16 / 14 / 12 px so
they match the three segment scales.  We keep that layout and that intent, but
draw fins instead of spines.

The engine draws worm mobs with ``addAngledDrawable`` and rotates the sprite to
the movement angle, so this sheet is authored in ONE orientation only:
**top-down, travelling UP (-Y)** — the same orientation vanilla's sandworm uses
(its mandibles open toward the top of the cell, its tail tapers toward the
bottom).  Because the whole sprite rotates, the value structure is radial about
the travel axis (dark flanks, light spine) with only a gentle top-left bias, so
it survives being spun to any heading.

Everything is deterministic: fixed seeds, no wall-clock, no dict ordering.
"""

import math
import os

from PIL import Image
from px import Canvas, Rng, with_alpha, mix
import palette

CELL = 64
COLS = 2
ROWS = 6
CX = 32  # cell centre-line; every profile is symmetric about it

OUT = palette.OUTLINE
TEAL = palette.AETHERIUM

# Pale cloud-white to silver over a storm-blue shadowed underside.  The Mistsea
# it swims in measures 156..247 in value (mistsea_deep_splat full variants), so
# the serpent's MASS is deliberately weighted to the dark half of this ramp and
# the pale steps are spent only on plate lips and the spine — otherwise it
# dissolves into the cloud.
SERPENT = {
    "shadow": (58, 78, 110),
    "deep":   (86, 110, 146),
    "mid":    (124, 148, 180),
    "base":   (166, 186, 208),
    "light":  (208, 222, 234),
    "hi":     (243, 248, 252),
}
RAMP = [SERPENT["shadow"], SERPENT["deep"], SERPENT["mid"],
        SERPENT["base"], SERPENT["light"], SERPENT["hi"]]

VAPOUR = palette.MISTSEA["top"]

PLATE_PITCH = 7  # rows between overlapping body plates


# --- profiles ---------------------------------------------------------------

def _profile(points, y):
    """Half-width of the silhouette at row y, linearly interpolated."""
    if y < points[0][0] or y > points[-1][0]:
        return 0.0
    for i in range(len(points) - 1):
        y0, w0 = points[i]
        y1, w1 = points[i + 1]
        if y0 <= y <= y1:
            if y1 == y0:
                return float(w0)
            t = (y - y0) / float(y1 - y0)
            return w0 + (w1 - w0) * t
    return 0.0


# Cranium only. The head's WIDTH comes from the frond fan, not from the skull --
# that is the whole construction of vanilla's crystaldragon head: a compact
# rounded braincase carrying two big eyes, with every bit of spread supplied by
# blades radiating out from underneath it. The old profile was a wide armoured
# skull filling the cell edge to edge, which read as a beetle shell at 1x.
HEAD_PROFILE = [(7, 5), (9, 9), (12, 12), (15, 14), (19, 16), (25, 16),
                (30, 15), (35, 13), (39, 10), (42, 6)]

# Tail: tube tapering to a point, then a small caudal fin flare.
# The tail TAPERS. An earlier profile pinched to 4 and then flared back out to
# 10, which at 1x read as a goblet rather than a tail tip. The caudal fin is now
# a small lift near the end, never wider than the tube above it.
TAIL_PROFILE = [(10, 11), (14, 13), (18, 14), (22, 13), (26, 11), (30, 9),
                (34, 7), (38, 6), (42, 4), (46, 3), (50, 2), (53, 1)]


def _seg_profile(y, half_w, y0=6, y1=57):
    """Barrel tube slice: flat-sided with rounded caps."""
    cy = (y0 + y1) / 2.0
    hh = (y1 - y0) / 2.0
    t = (y - cy) / hh
    if abs(t) >= 1.0:
        return 0.0
    return half_w * (1.0 - abs(t) ** 3) ** 0.5


# --- shared body shading ----------------------------------------------------

def _step_for(t):
    """Radial value step across the tube: |t| 0 = spine, 1 = flank edge."""
    a = abs(t)
    if a > 0.88:
        return 0
    if a > 0.64:
        return 1
    if a > 0.36:
        return 2
    if a > 0.14:
        return 3
    return 4


def _fill_body(c, rows, rng, chevron=0.0, plate_phase=0):
    """Fill a profile (list of (y, half_width)) with the tube value structure.

    Returns the list of (y, half_width_int) actually filled so callers can hang
    details (spine crystals, wisps) off the same geometry.
    """
    filled = []
    for y, hw in rows:
        hwi = int(round(hw))
        if hwi <= 0:
            continue
        filled.append((y, hwi))
        for x in range(CX - hwi, CX + hwi + 1):
            # Gentle top-left light bias: shift the bright core 2px left.
            t = (x - (CX - 2)) / float(hwi + 2)
            step = _step_for(t)

            # Overlapping plates: the row that starts a plate catches light on
            # its lip, the row above it is the crease where the plates meet.
            band_y = y + int(abs(x - CX) * chevron)
            phase = (band_y - plate_phase) % PLATE_PITCH
            if phase == 0:
                step = min(5, step + 1)
            elif phase == PLATE_PITCH - 1:
                step = max(0, step - 2)

            # Scale speckle micro-detail on the flanks only.
            if 1 <= step <= 3 and rng.chance(0.085):
                step = max(0, step - 1)
            elif step >= 3 and rng.chance(0.05):
                step = min(5, step + 1)

            c.put(x, y, RAMP[step])
    return filled


def _spine_ridge(c, filled, rng, glow=None, crystal_rows=()):
    """Crystalline aetherium ridge along the travel axis.

    A dark dorsal seam with DISCRETE crystals standing on it — deliberately not
    a continuous saturated stripe, which reads as a glowing pipe and flattens
    the tube it is supposed to sit on.
    """
    widths = dict(filled)
    for y, hwi in filled:
        if hwi < 3:
            continue
        c.put(CX, y, TEAL["deep"])
        c.put(CX - 1, y, SERPENT["deep"])
        c.put(CX + 1, y, SERPENT["deep"])
        if rng.chance(0.16):
            c.put(CX, y, TEAL["base"])

    for sy in crystal_rows:
        if widths.get(sy, 0) < 7:
            continue
        # dark gap above, so neighbouring crystals never fuse into a stripe
        for x in range(CX - 2, CX + 3):
            if (sy - 3) in widths:
                c.put(x, sy - 3, SERPENT["shadow"])
        for j, half in enumerate((1, 2, 3, 2, 1)):
            y = sy - 2 + j
            if y not in widths:
                continue
            for x in range(CX - half, CX + half + 1):
                d = abs(x - CX)
                if j == 4:
                    col = TEAL["deep"]
                elif d == 0:
                    col = TEAL["hi"]
                elif d == 1:
                    col = TEAL["light"]
                else:
                    col = TEAL["base"]
                c.put(x, y, col)
            if glow is not None:
                for x in range(CX - half, CX + half + 1):
                    glow.put(x, y, (255, 255, 255, 255) if half >= 2
                             else (255, 255, 255, 150))


def _wisps(c, filled, rng, density=0.30, back_only=True):
    """Vapour trailing off the flanks — drawn AFTER the outline pass so the
    outline cannot eat these 1px streaks (known trap)."""
    if not filled:
        return
    ymin = filled[0][0]
    ymax = filled[-1][0]
    for y, hwi in filled:
        if hwi < 4:
            continue
        if back_only and y < ymin + (ymax - ymin) * 0.35:
            continue
        if not rng.chance(density):
            continue
        side = -1 if rng.chance(0.5) else 1
        x = CX + side * (hwi + 2)
        length = rng.range(2, 5)
        for k in range(length):
            a = 130 - k * 24
            if a <= 12:
                break
            px_x = x + side * (k // 2)
            px_y = y + k
            if c.get(px_x, px_y)[3] == 0:
                c.put(px_x, px_y, with_alpha(VAPOUR if k % 2 == 0 else SERPENT["hi"], a))
    # A short plume straight off the rear.
    for k in range(4):
        a = 110 - k * 26
        if a <= 10:
            break
        for x in range(CX - 3 + k, CX + 4 - k):
            if c.get(x, ymax + 1 + k)[3] == 0:
                c.put(x, ymax + 1 + k, with_alpha(VAPOUR, a))


# --- frond fan --------------------------------------------------------------
# Construction lifted from vanilla mobs/crystaldragon.png (measured, not
# guessed): every segment of that boss is a compact core with long tapered
# blades radiating backwards, and the silhouette a player actually reads is the
# FAN, not the core. Ours are vapour fronds rather than crystal shards -- softer
# tips, a cloud-white body, a cold edge -- which is the "mehr auf Wolken
# getrimmt" part. Angles are degrees from straight-back (+Y); the sheet is
# authored travelling UP, so a frond at 0 deg trails directly behind.

def _fan(c, rng, x0, y0, reach, spread, blades, glow=None, seed=0, depth=0.34):
    """One connected frond fan, drawn as a MASS rather than as separate blades.

    Separate blades were the first two attempts and both failed the same way:
    ``Canvas.outline`` traces every disconnected shape, so blades that part
    company anywhere along their length each pick up their own black contour
    and the result reads as an insect's legs, not a mane. Vanilla's
    crystaldragon fan is one continuous pale mass -- the blades are told apart
    by darker seams running out from the root and by the scalloped rim, not by
    gaps. So: fill a polar silhouette whose radius is scalloped once per blade,
    shade it root-dark to rim-light, then cut the seams in.

    ``reach`` is the radius straight back, ``spread`` the half-angle in degrees
    (0 = straight back, since the sheet is authored travelling up).
    """
    rng_local = Rng(0x51E3_0F00 + seed)
    span = math.radians(spread)
    step = 2.0 * span / blades

    def radius(a):
        t = min(1.0, abs(a) / span) if span > 0 else 0.0
        base = reach * (1.0 - 0.34 * t ** 1.7)
        u = (a + span) / (2.0 * span)
        # Lobe crests at u = k/blades, notches halfway between. A shallow
        # scallop reads as a lumpy blob; the notches have to cut a third of
        # the radius before the eye separates the blades.
        scallop = 1.0 - depth * (0.5 - 0.5 * math.cos(2.0 * math.pi * blades * u))
        return base * scallop

    lo = int(x0 - reach - 2), int(y0 - reach - 2)
    hi = int(x0 + reach + 3), int(y0 + reach + 3)
    for y in range(max(0, lo[1]), min(c.height, hi[1])):
        for x in range(max(0, lo[0]), min(c.width, hi[0])):
            dx, dy = x - x0, y - y0
            r = math.hypot(dx, dy)
            if r < 0.5:
                continue
            a = math.atan2(dx, dy)          # 0 = straight back (+Y)
            if abs(a) > span:
                continue
            rr = radius(a)
            if r > rr:
                continue
            t = r / rr
            if t > 0.90:
                tone = TEAL["light"] if rng_local.chance(0.35) else SERPENT["light"]
            elif t > 0.68:
                tone = SERPENT["light"] if rng_local.chance(0.7) else SERPENT["base"]
            elif t > 0.40:
                tone = SERPENT["base"] if rng_local.chance(0.75) else SERPENT["mid"]
            else:
                tone = SERPENT["mid"] if rng_local.chance(0.7) else SERPENT["deep"]
            c.put(x, y, tone)
            if glow is not None and t > 0.93 and rng_local.chance(0.25):
                glow.put(x, y, (255, 255, 255, 90))

    # Seams: one darker ray per blade boundary, so the mass separates visually
    # without ever separating physically.
    for k in range(blades):
        a = -span + (k + 0.5) * step          # the notches, not the crests
        rr = radius(a)
        for i in range(int(rr * 2)):
            r = 4.0 + (rr - 4.0) * i / max(1.0, rr * 2 - 1)
            if r > rr - 0.6:
                break
            x = int(round(x0 + math.sin(a) * r))
            y = int(round(y0 + math.cos(a) * r))
            if 0 <= x < c.width and 0 <= y < c.height and c.filled(x, y):
                c.put(x, y, SERPENT["deep"] if r / rr < 0.75 else SERPENT["mid"])


# --- cells ------------------------------------------------------------------

def _head_cell(glow):
    c = Canvas(CELL, CELL)
    rng = Rng(0x51E3_0001)

    # Fan first, cranium over it, so the blades read as growing from underneath.
    # Seven pairs, 14 degrees apart, each wide enough at the root to overlap
    # its neighbour: vanilla's fan is a continuous mass that only separates
    # into blades near the tips. Widely spaced thin blades read as legs.
    # Five pairs, all swept BACK. Two things decide whether this reads as a
    # creature's mane or as insect legs, and neither is the count: the aspect
    # ratio (vanilla's blades are about 1 wide to 6 long -- stubby ones read as
    # claws) and the sweep (nothing may point sideways or forward, or the fan
    # turns into a row of legs). Vanilla's own head measures 14x90 per blade.
    # Rooted BEHIND the cranium's centre and held under 70 degrees of
    # spread: rooted at the middle with a wide spread, the lobes emerge
    # level with the eyes and the head reads as a sun with rays.
    _fan(c, rng, CX, 30, 34, 76, 6, glow, seed=1, depth=0.24)

    rows = [(y, _profile(HEAD_PROFILE, y)) for y in range(8, 43)]
    filled = _fill_body(c, rows, rng, chevron=0.30, plate_phase=0)
    _spine_ridge(c, filled, rng, glow=glow, crystal_rows=tuple(range(14, 42, 8)))
    c.outline(OUT)

    # --- details after the outline pass so it cannot overwrite them ---
    # Brow ridge over the eyes.
    for x in range(21, 44):
        d = abs(x - CX)
        c.put(x, 13 + d // 8, SERPENT["deep"])

    # Big almond eyes. Vanilla's crystaldragon carries two 20px eyes on a 224px
    # head; scaled to our 64px cell that is roughly 6x8, which is exactly what
    # makes the thing read as a creature and not a shell.
    for side, ex in ((-1, 21), (1, 37)):
        for j in range(8):
            half = (2, 3, 3, 3, 3, 3, 2, 1)[j]
            cxe = ex + 3
            for i in range(cxe - half, cxe + half + 1):
                c.put(i, 16 + j, OUT)
        # pupil: cold slit, brightest at the top
        for j in range(1, 6):
            c.put(ex + 3 - (1 if side < 0 else 0), 16 + j,
                  TEAL["hi"] if j <= 2 else TEAL["light"])
        c.put(ex + 2 + (1 if side < 0 else 0), 17, TEAL["base"])
        for j in range(-1, 9):
            glow.put(ex + 3, 16 + j, (255, 255, 255, 110))
        for i in range(-1, 8):
            glow.put(ex + i, 20, (255, 255, 255, 110))

    # Nostril slits on the snout.
    c.put(30, 11, OUT)
    c.put(34, 11, OUT)

    _wisps(c, filled, rng, density=0.26)
    return c


def _segment_cell(index, half_w):
    c = Canvas(CELL, CELL)
    rng = Rng(0x51E3_0100 + index)
    # Each segment carries its own smaller fan, which is what gives the chain a
    # feathered edge instead of a row of identical capsules.
    # Barely any taper between the four segment sprites: a 14-part chain
    # cycles through them, so a strong taper never reads as one -- it just
    # makes half the body thin. Vanilla's sandworm segments measure 1476,
    # 1476, 1428, 1428 opaque, i.e. essentially the same size.
    reach = 1.0 - index * 0.03
    # One modest frill per segment. Four small fans per segment was the
    # previous try and at 1x the chain read as fuzz -- high-frequency
    # scallops all along a 14-segment worm turn into noise, whatever they
    # look like zoomed in. Big lobes, shallow notches, close to the body.
    _fan(c, rng, CX, 29, 29 * reach, 76, 4, seed=10 + index, depth=0.26)
    rows = [(y, _seg_profile(y, half_w, y0=12, y1=52)) for y in range(12, 53)]
    filled = _fill_body(c, rows, rng, chevron=0.10, plate_phase=index)
    _spine_ridge(c, filled, rng, crystal_rows=tuple(range(13 + index, 55, 9)))
    c.outline(OUT)
    _wisps(c, filled, rng, density=0.34, back_only=False)
    return c


def _tail_cell():
    c = Canvas(CELL, CELL)
    rng = Rng(0x51E3_0200)
    _fan(c, rng, CX, 22, 25, 72, 3, seed=30, depth=0.26)
    # Trailing streamers: the tail's silhouette is two long fronds, not a fin
    # membrane. An earlier version flared back out at the very tip and read as
    # a goblet at 1x.
    _fan(c, rng, CX, 34, 26, 38, 2, seed=31, depth=0.30)
    rows = [(y, _profile(TAIL_PROFILE, y)) for y in range(8, 57)]
    filled = _fill_body(c, rows, rng, chevron=0.14, plate_phase=3)
    _spine_ridge(c, filled, rng, crystal_rows=tuple(range(13, 46, 9)))
    c.outline(OUT)
    _wisps(c, filled, rng, density=0.40, back_only=False)
    return c


def _fin(c, xr, y0, w, h, rng):
    """One swept crystalline fin: leaf-shaped membrane, teal leading edge,
    darker fin rays, a wisp off the tip.  Attaches on its right edge."""
    filled = []
    for j in range(h):
        t = (j + 0.5) / h
        # Broad root at the leading (front) edge sweeping to a trailing point.
        ww = int(round(w * ((1.0 - t) ** 0.55)))
        if ww <= 0:
            continue
        y = y0 + j
        filled.append((y, ww))
        for k in range(ww):
            x = xr - k
            d = k / float(ww)
            if d > 0.82:
                col = SERPENT["deep"]
            elif d > 0.52:
                col = SERPENT["mid"]
            elif d > 0.24:
                col = SERPENT["base"]
            else:
                col = SERPENT["light"]
            c.put(x, y, col)
        # Teal crystalline leading edge along the outer sweep.
        c.put(xr - ww + 1, y, TEAL["light"])
        if j < 2:
            c.put(xr - ww + 2, y, TEAL["base"])
    # Fin rays fanning back from the attachment root.
    for r in range(3):
        yy = y0 + 2 + r * max(2, h // 4)
        ww = 0
        for y, w2 in filled:
            if y == yy:
                ww = w2
        for k in range(2, max(3, int(ww * 0.9))):
            c.put(xr - k, yy + (k // 3), SERPENT["deep"])
    if filled:
        ty, tw = filled[-1]
        c.put(xr - tw, ty + 1, TEAL["hi"])
    return filled


def _fin_cell(index, w, h):
    """Column-1 decoration cell: two fin pieces packed left, matching the
    vanilla sandworm leg-spine layout (upper piece, lower piece)."""
    c = Canvas(CELL, CELL)
    rng = Rng(0x51E3_0300 + index)
    top = _fin(c, 10 + w, 8, w, h, rng)
    bot = _fin(c, 10 + w, 54 - h, w, h, rng)
    c.outline(OUT)
    for filled in (top, bot):
        for y, ww in filled[-4:]:
            for k in range(2):
                x = 10 + w - ww - 1 - k
                if c.get(x, y + k + 1)[3] == 0:
                    c.put(x, y + k + 1, with_alpha(VAPOUR, 110 - k * 35))
    return c


# --- sheets -----------------------------------------------------------------

def gen_sheet(path):
    sheet = Canvas(CELL * COLS, CELL * ROWS)
    glow = Canvas(CELL, CELL)

    sheet.paste(_head_cell(glow), 0, 0)
    for i, hw in enumerate((17, 17, 16, 16)):
        sheet.paste(_segment_cell(i, hw), 0, CELL * (1 + i))
    sheet.paste(_tail_cell(), 0, CELL * 5)

    for i, (w, h) in enumerate(((16, 19), (14, 17), (12, 15))):
        sheet.paste(_fin_cell(i, w, h), CELL, CELL * i)

    sheet.save(path)
    return glow


def gen_mask(path, glow):
    """The dive mask -- NOT a glow map.

    WormMobHead.getAngledDrawable feeds this to addMaskShader as
    MaskShaderOptions(mask, 0, 0, 0, -height - yOffset): the mask is offset
    vertically by the mob's flight height, so it CLIPS the sprite as the worm
    sinks and reveals it as the worm rises. That is the whole surfacing-and-
    diving effect.

    Vanilla's sandworm_mask is therefore a near-solid silhouette -- measured at
    3832 of 4096 pixels opaque, full 64-wide from y=8 down, with only the top
    rows cut back around the maw. A sparse glow map here would clip the serpent
    down to a handful of pixels and make it all but invisible.
    """
    del glow  # the eye/crest glow is not what this file is for
    c = Canvas(64, 64)
    # Top rows taper in to match the head's snout, so the serpent's nose breaks
    # the cloud surface before its shoulders do.
    for y in range(64):
        if y < 8:
            inset = 26 - y * 3
        elif y < 12:
            inset = 2
        else:
            inset = 0
        for x in range(inset, 64 - inset):
            c.put(x, y, (255, 255, 255, 255))
    c.save(path)


def gen_shadow(path):
    c = Canvas(44, 24)
    c.ellipse(21.5, 11.5, 21.0, 11.0, (0, 0, 0, 29))
    c.ellipse(21.5, 11.5, 19.0, 9.5, (0, 0, 0, 70))
    c.save(path)


def gen_icon(path):
    """32x32 mob icon: the HEAD, not a body segment. The icon is what names the
    creature in the bestiary, so it carries the two things that identify it --
    the cranium with its pale eyes and the frond fan behind it."""
    c = Canvas(32, 32)
    rng = Rng(0x51E3_0400)
    icx = 16

    _fan(c, rng, icx, 15, 16, 70, 5, seed=40, depth=0.24)

    prof = [(3, 3), (5, 6), (7, 8), (10, 9), (14, 9), (17, 8), (20, 7), (22, 4)]
    filled = []
    for y in range(3, 23):
        hwi = int(round(_profile(prof, y)))
        if hwi <= 0:
            continue
        filled.append((y, hwi))
        for x in range(icx - hwi, icx + hwi + 1):
            t = (x - (icx - 1)) / float(hwi + 1)
            step = _step_for(t)
            if y % 5 == 0:
                step = min(5, step + 1)
            elif y % 5 == 4:
                step = max(0, step - 1)
            c.put(x, y, RAMP[step])
    for y, hwi in filled:
        for x in range(icx - 1, icx + 2):
            c.put(x, y, TEAL["light"] if x == icx else TEAL["base"])
    c.outline(OUT)

    for ex in (10, 19):
        for j in range(5):
            half = (1, 2, 2, 2, 1)[j]
            for i in range(ex - half, ex + half + 1):
                c.put(i, 8 + j, OUT)
        c.put(ex, 9, TEAL["hi"])
        c.put(ex, 10, TEAL["light"])
    for x in range(13, 20):
        c.put(x, 6, SERPENT["deep"])
    c.save(path)


def gen_mistserpent(mobs_dir, icons_dir):
    """Entry point: writes all four Mist Serpent files."""
    glow = gen_sheet(os.path.join(mobs_dir, "mistserpent.png"))
    gen_mask(os.path.join(mobs_dir, "mistserpent_mask.png"), glow)
    gen_shadow(os.path.join(mobs_dir, "mistserpent_shadow.png"))
    gen_icon(os.path.join(icons_dir, "mistserpent.png"))


if __name__ == "__main__":
    import argparse
    import sys
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "..", "..",
        "src", "main", "resources"))
    args = ap.parse_args()
    mobs = os.path.join(args.out, "mobs")
    icons = os.path.join(mobs, "icons")
    os.makedirs(icons, exist_ok=True)
    gen_mistserpent(mobs, icons)
    print("wrote mistserpent sheet/mask/shadow/icon into", os.path.abspath(mobs))
