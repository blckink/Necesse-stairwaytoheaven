"""Mist Serpent — segmented worm-mob sheet for the Mistsea.

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


# Blunt serpent skull: rounded snout, wide cheeks/jaw, neck collar at the rear.
HEAD_PROFILE = [(7, 4), (9, 8), (12, 12), (15, 15), (19, 17), (23, 18),
                (27, 20), (31, 22), (35, 23), (39, 23), (43, 22), (47, 20),
                (51, 17), (55, 12), (58, 7)]

# Tail: tube tapering to a point, then a small caudal fin flare.
# The tail TAPERS. An earlier profile pinched to 4 and then flared back out to
# 10, which at 1x read as a goblet rather than a tail tip. The caudal fin is now
# a small lift near the end, never wider than the tube above it.
TAIL_PROFILE = [(8, 16), (12, 17), (16, 17), (20, 16), (24, 14), (28, 12),
                (32, 9), (36, 7), (40, 5), (44, 4), (47, 6), (50, 5),
                (52, 3), (54, 1)]


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


# --- cells ------------------------------------------------------------------

def _head_cell(glow):
    c = Canvas(CELL, CELL)
    rng = Rng(0x51E3_0001)

    rows = [(y, _profile(HEAD_PROFILE, y)) for y in range(7, 59)]
    filled = _fill_body(c, rows, rng, chevron=0.32, plate_phase=0)

    # Heavy brow band + jaw grooves: this is what makes the cell read as a
    # skull rather than a rounder body segment.
    for x in range(17, 48):
        d = abs(x - CX)
        c.put(x, 22 + d // 7, SERPENT["shadow"])
        c.put(x, 23 + d // 7, SERPENT["deep"])
    for side in (-1, 1):
        for k in range(9):
            c.put(CX + side * (10 + k), 33 + k // 2, SERPENT["shadow"])
            c.put(CX + side * (10 + k), 34 + k // 2, SERPENT["deep"])
    for y in range(52, 58):
        hwi = int(round(_profile(HEAD_PROFILE, y)))
        for x in range(CX - hwi, CX + hwi + 1):
            if abs(x - CX) > 5 and rng.chance(0.7):
                c.put(x, y, SERPENT["deep"])

    # Eye sockets sunk into the skull (mirrored about x=32).
    for ex in (21, 39):
        for i in range(5):
            for j in range(4):
                c.put(ex + i, 24 + j, SERPENT["shadow"])

    _spine_ridge(c, filled, rng, glow=glow, crystal_rows=tuple(range(15, 56, 9)))
    c.outline(OUT)

    # --- details after the outline pass so it cannot overwrite them ---
    # Snout: chevron mouth line with two fangs, plus nostril slits.
    for x in range(25, 40):
        d = abs(x - CX)
        c.put(x, 19 + d // 5, OUT)
    c.put(28, 21, SERPENT["hi"])
    c.put(35, 21, SERPENT["hi"])
    c.put(29, 13, OUT)
    c.put(34, 13, OUT)

    # Teal eye glow.
    for ex in (21, 39):
        for i in range(5):
            for j in range(4):
                c.put(ex + i, 24 + j, TEAL["base"])
        for i in range(1, 4):
            for j in range(1, 3):
                c.put(ex + i, 24 + j, TEAL["light"])
        c.put(ex + 1, 25, TEAL["hi"])
        c.put(ex + 2, 25, TEAL["hi"])
        c.put(ex + 2, 26, TEAL["hi"])
        for i in range(5):
            for j in range(4):
                glow.put(ex + i, 24 + j, (255, 255, 255, 255))
        for i in range(-1, 6):
            glow.put(ex + i, 23, (255, 255, 255, 120))
            glow.put(ex + i, 28, (255, 255, 255, 120))

    _wisps(c, filled, rng, density=0.26)
    return c


def _segment_cell(index, half_w):
    c = Canvas(CELL, CELL)
    rng = Rng(0x51E3_0100 + index)
    rows = [(y, _seg_profile(y, half_w)) for y in range(6, 58)]
    filled = _fill_body(c, rows, rng, chevron=0.10, plate_phase=index)
    _spine_ridge(c, filled, rng, crystal_rows=tuple(range(11 + index, 58, 9)))
    c.outline(OUT)
    _wisps(c, filled, rng, density=0.34, back_only=False)
    return c


def _tail_cell():
    c = Canvas(CELL, CELL)
    rng = Rng(0x51E3_0200)
    rows = [(y, _profile(TAIL_PROFILE, y)) for y in range(8, 57)]
    filled = _fill_body(c, rows, rng, chevron=0.14, plate_phase=3)
    _spine_ridge(c, filled, rng, crystal_rows=tuple(range(13, 46, 9)))
    # Caudal fin membrane reads brighter than the tube.
    for y in range(44, 53):
        hwi = int(round(_profile(TAIL_PROFILE, y)))
        for x in range(CX - hwi, CX + hwi + 1):
            if abs(x - CX) > 1 and rng.chance(0.55):
                c.put(x, y, SERPENT["light"] if rng.chance(0.5) else TEAL["light"])
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
    for i, hw in enumerate((24, 23, 21, 19)):
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
    c = Canvas(32, 32)
    rng = Rng(0x51E3_0400)
    prof = [(2, 3), (4, 6), (6, 8), (9, 10), (12, 11), (15, 12), (18, 11),
            (21, 10), (24, 10), (27, 10), (29, 8)]
    icx = 16
    filled = []
    for y in range(2, 30):
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
            if 1 <= step <= 3 and rng.chance(0.09):
                step = max(0, step - 1)
            c.put(x, y, RAMP[step])
    # spine ridge
    for y, hwi in filled:
        for x in range(icx - 2, icx + 3):
            d = abs(x - icx)
            c.put(x, y, TEAL["light"] if d <= 1 else TEAL["base"] if d == 2 else TEAL["deep"])
        if y % 5 == 0:
            c.put(icx, y, TEAL["hi"])
    for ex in (10, 19):
        for i in range(3):
            for j in range(2):
                c.put(ex + i, 11 + j, SERPENT["shadow"])
    c.outline(OUT)
    for ex in (10, 19):
        for i in range(3):
            for j in range(2):
                c.put(ex + i, 11 + j, TEAL["base"])
        c.put(ex + 1, 11, TEAL["hi"])
        c.put(ex + 1, 12, TEAL["light"])
    for x in range(13, 20):
        c.put(x, 8, SERPENT["shadow"])
    _wisps(c, filled, rng, density=0.0)
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
