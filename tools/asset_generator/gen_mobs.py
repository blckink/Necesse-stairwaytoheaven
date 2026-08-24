"""Mob sprite sheets.

Walking/flying 4-direction sheets are 6 columns x 4 rows of 64x64 cells
(384x256): columns = idle, walk 1-4, in-liquid; rows = Up, Right, Down, Left
(see docs/research/asset-formats.md). The Storm Wisp uses the vanilla
flying-spirit layout: column 0 = 4 stacked body frames, column 1 = matching
glow overlays (128x256).
"""

import math

from PIL import Image
from px import Canvas, Rng, with_alpha, mix
import palette

CELL = 64
COLS = 6  # idle, walk x4, swim
ROWS = 4  # Up, Right, Down, Left


def _rotated(canvas, transpose):
    out = Canvas(canvas.width, canvas.height)
    out.img = canvas.img.transpose(transpose)
    out.px = out.img.load()
    return out


def _mist_overlay(c):
    """Half-sunk-in-mist look for the in-liquid column."""
    mist = palette.MISTSEA
    for x in range(CELL):
        for y in range(40, CELL):
            if c.filled(x, y):
                c.put(x, y, (0, 0, 0, 0))
    for i, (rx, ry) in enumerate(((20, 3), (14, 2.4), (9, 2))):
        c.ellipse(32, 40 + i, rx, ry, with_alpha(mist["hi"] if i == 0 else mist["light"], 230 - i * 30))


# --- Zephyr Ray --------------------------------------------------------------

def _ray_wing(c, cx, cy, side, root, span, droop, camber, bite, phase, ramp,
              rng=None):
    """One organic manta wing, top view: convex CURVED leading edge (quadratic
    arc bulging forward), gently scalloped trailing edge (three shallow bites
    between finger points), membrane shaded in bands that follow the sweep.
    Returns the per-column geometry so accents can ride the same curves."""
    tip_y = cy + droop
    lead = (cy - 7.0, cy - 7.0 - camber, tip_y - 0.5)   # bezier root/ctrl/tip
    trail = (cy + 8.0, cy + 9.0 + droop * 0.55, tip_y + 1.0)

    def bez(p, f):
        return (1 - f) * (1 - f) * p[0] + 2 * f * (1 - f) * p[1] + f * f * p[2]

    cols = []
    for xi in range(root, span + 1):
        f = (xi - root) / max(span - root, 1)
        yl = bez(lead, f)
        yt = bez(trail, f)
        if f > 0.12:                       # ragged trailing edge, zero at tips
            g = (f * 3.0 + phase) % 1.0
            yt -= bite * math.sin(g * math.pi)
        yl_i = round(yl)
        yt_i = round(max(yt, yl + (2 if f < 0.97 else 0)))
        cols.append((cx + side * xi, yl_i, yt_i, f))
    for x, yl, yt, _f in cols:
        for y in range(yl, yt + 1):
            c.put(x, y, ramp["base"])
    # membrane shading: lit band inside the leading edge, shadow along the
    # trailing edge, two finger creases at fixed chord fractions — all of it
    # inherits the curvature of the edges, so the bands sweep with the wing
    for x, yl, yt, f in cols:
        h = yt - yl
        if h >= 4:
            c.put(x, yl + 1, ramp["light"])
            if h >= 8:
                c.put(x, yl + 2, ramp["light"])
            c.put(x, yt - 1, ramp["deep"])
            if h >= 7:
                c.put(x, yt - 2, ramp["deep"])
        # finger creases: dashed so they read as membrane folds, not wires,
        # and clamped away from the lit and shadowed bands
        c1 = yl + max(3, round(h * 0.40))
        c2 = yl + round(h * 0.62)
        if 0.14 < f < 0.86 and c1 <= yt - 3 and not (rng and rng.chance(0.3)):
            c.put(x, c1, ramp["deep"])
        if 0.24 < f < 0.93 and c1 + 2 <= c2 <= yt - 3 and not (rng and rng.chance(0.3)):
            c.put(x, c2, ramp["deep"])
        # sparse sheen glints on the lit leading band (light stays top-left)
        if h >= 6 and 0.26 < f < 0.66 and (x + yl) % 5 == 0:
            c.put(x, yl + 1, ramp["hi"])
    return cols


def _ray_base(wing_spread, frame_seed):
    """Top view, head up. wing_spread in [0..1]: 0 folded, 1 full.

    Organic manta build: wings are curved membranes whose tips sweep down and
    back as the spread closes (the flap visibly curls through the stroke),
    wing roots are buried under the body mass so they blend seamlessly, and
    the tail whips in an S-curve that re-poses per frame."""
    c = Canvas(CELL, CELL)
    ramp = palette.ZEPHYR
    rng = Rng(0x2A7 + frame_seed * 7919)
    cx, cy = 32, 30
    span = round(12 + 14 * wing_spread)
    droop = round(3 + 12 * (1 - wing_spread))      # folded wings trail back
    camber = 4 + round(3 * wing_spread)            # spread wings bow forward
    bite = 1.5 + 0.9 * (1 - wing_spread)
    phase = 0.15 + rng.float() * 0.35              # scallops ripple per frame
    wings = {}
    for side in (-1, 1):
        wings[side] = _ray_wing(c, cx, cy, side, 2, span, droop, camber,
                                bite, phase, ramp, rng)
    # body: overlapping round masses over the wing roots (deep under-crescent,
    # base mass, light upper-left sheen — the house volumetric construction)
    c.ellipse(cx + 1, cy + 3, 5.5, 9.5, ramp["deep"])
    c.ellipse(cx, cy + 1, 5.5, 10, ramp["base"])
    c.ellipse(cx, cy - 8, 4, 4.5, ramp["base"])            # head mass
    c.ellipse(cx - 3, cy - 12, 1.7, 2.3, ramp["base"])     # cephalic lobes
    c.ellipse(cx + 3, cy - 12, 1.7, 2.3, ramp["base"])
    c.ellipse(cx - 1, cy - 3, 3.6, 7, ramp["light"])
    c.ellipse(cx - 1, cy - 8, 2.6, 2.8, ramp["light"])
    c.ellipse(cx - 3, cy - 12, 1.1, 1.5, ramp["light"])
    c.ellipse(cx - 2, cy - 5, 1.5, 2.4, ramp["hi"])
    # back pattern: paired dark spots down the mantle (no diamond chain)
    for sx, sy in ((-2, -1), (2, -1), (-3, 4), (3, 4), (0, 7)):
        c.put(cx + sx, cy + sy, ramp["deep"])
    # pelvic fin bumps at the tail root
    c.ellipse(cx - 3, cy + 9, 2, 1.6, ramp["base"])
    c.ellipse(cx + 3, cy + 9, 2, 1.6, ramp["base"])
    c.put(cx - 3, cy + 10, ramp["deep"])
    c.put(cx + 3, cy + 10, ramp["deep"])
    # tail: S-curved whip, 3px silhouette at the root tapering to a dark tip
    tail_phase = 0.9 * frame_seed
    for i in range(13):
        y = cy + 9 + i
        x = cx + round(1.9 * math.sin(i * 0.42 + tail_phase))
        if i < 4:
            c.put(x - 1, y, ramp["deep"])
            c.put(x, y, ramp["base"])
            c.put(x + 1, y, ramp["deep"])
        elif i < 9:
            c.put(x, y, ramp["base"])
            c.put(x + 1, y, ramp["deep"])
        else:
            c.put(x, y, ramp["deep"])
    c.outline(palette.OUTLINE)
    # accents AFTER the outline: teal spot rows riding the wing sweep, a
    # glint just inside each tip, eyes at the lobe bases
    for side in (-1, 1):
        cols = wings[side]
        n = len(cols) - 1
        for k, fr in enumerate((0.45, 0.66, 0.86)):
            x, yl, yt, _f = cols[round(n * fr)]
            mid = yl + (yt - yl) // 2
            c.put(x, mid, ramp["accent"])
            if k == 0:
                c.put(x, mid + 1, ramp["accent"])
        tx, tyl, tyt, _f = cols[max(0, n - 2)]
        c.put(tx, tyl + (tyt - tyl) // 2, ramp["hi"])
    c.put(cx - 3, cy - 9, palette.OUTLINE)
    c.put(cx + 3, cy - 9, palette.OUTLINE)
    c.put(cx - 3, cy - 8, ramp["accent"])
    c.put(cx + 3, cy - 8, ramp["accent"])
    return c


def gen_zephyrray(path):
    spreads = {0: 0.75, 1: 1.0, 2: 0.75, 3: 0.35, 4: 0.75, 5: 0.7}
    sheet = Canvas(COLS * CELL, ROWS * CELL)
    for col in range(COLS):
        up = _ray_base(spreads[col], col)
        if col == 5:
            _mist_overlay(up)
        right = _rotated(up, Image.ROTATE_270)
        down = _rotated(up, Image.ROTATE_180)
        left = _rotated(up, Image.ROTATE_90)
        for row, sprite in enumerate((up, right, down, left)):
            sheet.paste(sprite, col * CELL, row * CELL)
    sheet.save(path)


# --- Skystone Golem ----------------------------------------------------------

def _stone(c, cx, cy, rx, ry, r):
    """One volumetric boulder: deep crescent lower-right, base mass, light
    upper-left sheen — the vanilla 'round overlapping masses' construction."""
    c.ellipse(cx + 1, cy + 1, rx, ry, r["deep"])
    c.ellipse(cx, cy, rx, ry, r["base"])
    c.ellipse(cx - rx * 0.28, cy - ry * 0.28, rx * 0.62, ry * 0.6, r["light"])
    c.ellipse(cx - rx * 0.42, cy - ry * 0.45, rx * 0.3, ry * 0.26, r["hi"])


def _golem_frame(facing, step, swim=False):
    """Skystone Golem rebuilt from organic boulder masses (goblin-construction
    bar): articulated stomp walk, sunken head under a heavy brow, glowing
    eyes, cracks, moss and an aetherium spur."""
    c = Canvas(CELL, CELL)
    r = palette.GOLEM
    aeth = palette.AETHERIUM
    cx = 32
    feet_y = 55
    stomp = 1 if step != 0 else 0

    if facing in ("up", "down"):
        # --- legs: stout boulders with foot masses, one lifted per stride ---
        if not swim:
            for side, s in ((-1, step), (1, -step)):
                lx = cx + side * 7
                lift = 2 if s > 0 else 0
                _stone(c, lx, feet_y - 9 - lift, 4, 6, r)   # overlaps the belly
                _stone(c, lx + side, feet_y - 1 - lift, 4.5, 2.5, r)  # foot
        # --- torso: big chest boulder over a ribbed belly stone ---
        _stone(c, cx, feet_y - 21, 11, 8, r)          # belly
        for i, by in enumerate((feet_y - 24, feet_y - 20, feet_y - 16)):
            half = 10 - i * 2
            for dx in range(-half, half + 1):
                arc = by + (abs(dx) // 4)
                c.put(cx + dx, arc, r["light"])       # plate ridge
                c.put(cx + dx, arc + 1, r["deep"])    # plate seam
        _stone(c, cx, feet_y - 30 + stomp, 13, 10, r)  # chest
        # --- arms: shoulder cap, forearm, fist boulder (swing vs legs) ---
        for side, s in ((-1, -step), (1, step)):
            sw = round(s * 2)
            ax = cx + side * 15
            _stone(c, cx + side * 12, feet_y - 36 + stomp, 5.5, 4.5, r)  # shoulder cap
            _stone(c, ax, feet_y - 29 + sw, 4, 5, r)                      # upper arm
            _stone(c, ax + side, feet_y - 21 + sw, 4.5, 4.5, r)           # fist
            c.put(ax + side - 1, feet_y - 19 + sw, r["deep"])             # knuckles
            c.put(ax + side + 1, feet_y - 19 + sw, r["deep"])
        # --- head: sunk between the shoulders, heavy brow ledge ---
        head_y = feet_y - 39 + stomp
        _stone(c, cx, head_y, 6.5, 5.5, r)
        for dx in range(-5, 6):                                           # brow ledge
            c.put(cx + dx, head_y - 2, r["deep"])
            if -4 <= dx <= 4:
                c.put(cx + dx, head_y - 3, r["light"])
        # --- cracks on the chest ---
        crx, cry = cx - 6, feet_y - 33 + stomp
        for i in range(7):
            c.put(crx + i, cry + (i // 2) - (1 if i > 4 else 0), r["deep"])
        c.put(cx + 4, feet_y - 24, r["deep"])
        c.put(cx + 5, feet_y - 23, r["deep"])
        c.put(cx + 5, feet_y - 22, r["deep"])
        # --- moss clumps on upper surfaces ---
        for (mx, my) in ((cx - 11, feet_y - 39 + stomp), (cx + 8, feet_y - 27), (cx - 4, feet_y - 15)):
            c.put(mx, my, r["moss"])
            c.put(mx + 1, my, r["moss"])
            c.put(mx, my + 1, r["moss"])
        # --- aetherium spur cluster on the right shoulder ---
        spx = cx + 12
        spy = feet_y - 40 + stomp
        for i in range(5):                      # solid crystal wedge (3px base)
            w = max(0, 2 - i // 2)
            for dx in range(-w, w + 1):
                c.put(spx + dx, spy - i, aeth["light"] if dx <= 0 else aeth["base"])
        c.outline(palette.OUTLINE)
        c.put(spx, spy - 5, aeth["hi"])         # crystal tip glint
        # --- face AFTER outline ---
        if facing == "down":
            for ex in (cx - 4, cx + 2):
                c.rect(ex, head_y - 1, 3, 3, r["deep"])                   # socket
                c.rect(ex, head_y, 2, 2, r["eye"])                        # glow
                c.put(ex, head_y, (240, 252, 252))                        # pupil
            for i in range(3):                                            # jaw crack
                c.put(cx - 1 + i, head_y + 3 + (i % 2), r["deep"])
        else:  # up: back plate seam + moss instead of a face
            for dy in range(-1, 9):
                c.put(cx, head_y + dy + 4, r["deep"])
            c.put(cx - 6, head_y + 2, r["moss"])
            c.put(cx - 5, head_y + 2, r["moss"])
            c.put(cx + 5, head_y + 6, r["moss"])
    else:  # right profile: hunched ape posture, big leading arm (ash-golem bar)
        if not swim:
            front = 3 * step
            _stone(c, cx - 8 + front, feet_y - 7, 4.5, 6, r)     # rear leg
            _stone(c, cx - 7 + front, feet_y - 1, 5, 2.5, r)
            _stone(c, cx - 1 - front, feet_y - 6, 4.5, 6, r)     # front leg
            _stone(c, cx - front, feet_y - 1, 5, 2.5, r)
        _stone(c, cx - 4, feet_y - 17, 9, 7, r)                  # haunch
        _stone(c, cx + 2, feet_y - 26 + stomp, 12, 9, r)         # chest, leaning fwd
        for i, by in enumerate((feet_y - 21, feet_y - 17)):      # rib plates
            for dx in range(-7 + i, 6 - i):
                c.put(cx - 2 + dx, by + abs(dx) // 4, r["light"])
                c.put(cx - 2 + dx, by + abs(dx) // 4 + 1, r["deep"])
        _stone(c, cx - 3, feet_y - 33 + stomp, 5.5, 4.5, r)      # rear shoulder hump
        # leading arm: reaches forward and down to the ground
        sw = round(step * 2)
        _stone(c, cx + 10, feet_y - 22 + sw, 4.5, 5.5, r)        # upper arm
        _stone(c, cx + 13, feet_y - 12 + sw, 5, 5, r)            # fist near ground
        c.put(cx + 12, feet_y - 9 + sw, r["deep"])               # knuckles
        c.put(cx + 15, feet_y - 9 + sw, r["deep"])
        # head juts forward from the chest
        head_y = feet_y - 33 + stomp
        _stone(c, cx + 9, head_y, 6, 5, r)
        for dx in range(-3, 6):
            c.put(cx + 9 + dx, head_y - 2, r["deep"])            # brow ledge
            c.put(cx + 9 + dx, head_y - 3, r["light"])
        for dx in range(-2, 3):                                  # neck shadow
            c.put(cx + 6 + dx, head_y + 4, r["deep"])
        # spur on the rear shoulder + moss
        spx, spy = cx - 4, feet_y - 37 + stomp
        for i in range(4):
            w = max(0, 2 - i // 2)
            for dx in range(-w, w + 1):
                c.put(spx + dx, spy - i, aeth["light"] if dx <= 0 else aeth["base"])
        c.put(cx - 8, feet_y - 30, r["moss"])
        c.put(cx - 7, feet_y - 30, r["moss"])
        c.put(cx - 2, feet_y - 12, r["moss"])
        c.outline(palette.OUTLINE)
        c.put(spx, spy - 4, aeth["hi"])
        c.rect(cx + 11, head_y - 1, 3, 3, r["deep"])             # deep socket
        c.rect(cx + 12, head_y, 2, 2, r["eye"])
        c.put(cx + 12, head_y, (240, 252, 252))
    if swim:
        _mist_overlay(c)
    return c


def gen_skystonegolem(path):
    steps = {0: 0, 1: 1, 2: 0, 3: -1, 4: 0, 5: 0}
    sheet = Canvas(COLS * CELL, ROWS * CELL)
    for col in range(COLS):
        swim = col == 5
        up = _golem_frame("up", steps[col], swim)
        right = _golem_frame("right", steps[col], swim)
        down = _golem_frame("down", steps[col], swim)
        left = right.mirrored()
        for row, sprite in enumerate((up, right, down, left)):
            sheet.paste(sprite, col * CELL, row * CELL)
    sheet.save(path)


# --- Storm Wisp --------------------------------------------------------------
# Animated like the vanilla flying-spirit mobs: 4 body frames stacked down
# column 0 (rows 0-3) with matching glow overlays in column 1. The draw code
# picks the row via GameUtils.getAnim(time, 4, ...) — see StormWispMob.

WISP_FRAMES = 4

# Tendril skirt per frame: (x offset from center, length below the root line,
# sway dir). Lengths/sways shift frame to frame so the tail undulates like a
# flame. Tendrils are drawn as tapering wedges rooted INSIDE the body mass.
_WISP_TENDRILS = (
    ((-8, 8, -1), (-2, 13, 0), (4, 10, 1), (9, 6, 1)),
    ((-8, 11, -1), (-2, 9, 1), (4, 13, 0), (9, 5, 1)),
    ((-8, 6, 0), (-2, 14, -1), (4, 8, 1), (9, 9, 0)),
    ((-8, 9, -1), (-2, 11, 1), (4, 12, -1), (9, 7, 1)),
)

# Top flame-lick lean per frame (x offset of the lick tip).
_WISP_LICK = (-2, 0, 2, 0)

# Rim lightning arcs per frame: (rim dx, rim dy, step dx, step dy) with the
# start point ON the head rim so the crackle visibly crawls along the body.
_WISP_ARCS = (
    ((-10, -3, -1, -1), (9, -6, 1, -1)),
    ((-7, -8, -1, -1), (10, 1, 1, 1)),
    ((-10, 3, -1, 1), (5, -9, 1, -1)),
    ((-10, -5, -1, -1), (9, 4, 1, 1)),
)


def _wisp_frame(frame):
    """One 64x64 body frame: a luminous flame-teardrop spirit — bright head
    mass with hollow eyes, a swaying lick on top, and long ragged tendrils
    trailing below, all re-posed per frame."""
    r = palette.WISP
    c = Canvas(CELL, CELL)
    cx, cy = 32, 25  # head center; tendrils fill the lower third
    swell = (0, 1, 0, -1)[frame]
    lick = _WISP_LICK[frame]
    # head: overlapping round masses forming a bumpy teardrop
    c.ellipse(cx - 5, cy + 2, 7, 6.5, r["deep"])
    c.ellipse(cx + 5, cy + 2, 7, 6.5, r["deep"])
    c.ellipse(cx, cy - 2, 10 + swell, 9, r["deep"])
    # lower body tapers toward the tendril roots
    c.ellipse(cx, cy + 8, 8, 6, r["deep"])
    c.ellipse(cx, cy + 12, 6, 4, r["deep"])
    # flame-lick on top, leaning with the frame
    c.ellipse(cx + lick, cy - 11, 3.5, 3.5, r["deep"])
    c.ellipse(cx + lick * 2, cy - 14, 2, 2.5, r["deep"])
    # tendrils: tapering wedges rooted well inside the body (no outline gap),
    # fading base -> deep toward the tips like a dying flame
    for (tx, ln, sway) in _WISP_TENDRILS[frame]:
        x = cx + tx
        root = cy + 10
        for i in range(ln + 4):
            y = root + i - 4  # first 4 rows overlap the body mass
            f = max(0, i - 4) / max(1, ln - 1)
            w = 4 if f < 0.35 else (3 if f < 0.6 else (2 if f < 0.85 else 1))
            if sway != 0 and i > 6 and i % 3 == 0:
                x += sway
            tone = r["base"] if f < 0.45 else r["deep"]
            for k in range(w):
                c.put(x - w // 2 + k, y, tone)
    # volumetric light: bright inner masses in the upper-left of the head
    c.ellipse(cx - 1, cy, 8.5, 7.5, r["base"])
    c.ellipse(cx, cy + 7, 6, 4.5, r["base"])
    c.ellipse(cx + lick, cy - 10, 2.2, 2.4, r["base"])
    c.ellipse(cx - 2, cy - 2, 6.5, 5.5, r["inner"])
    c.ellipse(cx - 3, cy - 3 - (1 if swell > 0 else 0), 3.6, 3.2, r["core"])
    # face: round hollow sockets + bright pupils + jagged mouth crack
    for ex in (cx - 5, cx + 3):
        c.ellipse(ex, cy - 2, 1.8, 2.2, r["deep"])
        c.put(ex, cy - 2, r["core"])
        c.put(ex, cy - 1, r["core"])
    for i, mx in enumerate(range(cx - 4, cx + 4)):
        c.put(mx, cy + 4 + (i % 2), r["deep"])
    c.put(cx - 1, cy + 6, r["inner"])
    c.put(cx + 2, cy + 6, r["inner"])
    c.outline(palette.OUTLINE)
    # accents AFTER the outline: rim arcs (starting on the rim) + pupils
    for arc in _WISP_ARCS[frame]:
        sx, sy, dx, dy = arc
        x, y = cx + sx, cy + sy
        for i in range(4):
            c.put(x, y, r["spark"] if i % 2 == 0 else r["inner"])
            x += dx
            y += dy if i % 2 == 0 else -dy
        c.put(x, y, r["spark"])
    for ex in (cx - 5, cx + 3):
        c.put(ex, cy - 2, r["core"])
        c.put(ex, cy - 1, r["core"])
    return c


def _wisp_glow(frame):
    """Matching 64x64 glow overlay: breathing violet halo centered on the
    head + arc echoes so the additive glow flickers with the crackle."""
    r = palette.WISP
    c = Canvas(CELL, CELL)
    cx, cy = 32, 25
    strength = (140, 175, 140, 110)[frame]
    for x in range(CELL):
        for y in range(CELL):
            dx = (x - cx) / 20.0
            dy = (y - cy) / 19.0
            d = dx * dx + dy * dy
            if d <= 1.0:
                alpha = int(strength * (1.0 - d) ** 2)
                if alpha > 8:
                    c.put(x, y, with_alpha(mix(r["inner"], r["core"], (1.0 - d) * 0.35), alpha))
    for arc in _WISP_ARCS[frame]:
        sx, sy, dx, dy = arc
        x, y = cx + sx, cy + sy
        for i in range(5):
            c.put(x, y, with_alpha(r["spark"], 200))
            x += dx
            y += dy if i % 2 == 0 else -dy
    return c


def gen_stormwisp(path):
    sheet = Canvas(2 * CELL, WISP_FRAMES * CELL)
    for frame in range(WISP_FRAMES):
        sheet.paste(_wisp_frame(frame), 0, frame * CELL)
        sheet.paste(_wisp_glow(frame), CELL, frame * CELL)
    sheet.save(path)


# --- Bestiary icons ----------------------------------------------------------

def _icon_canvas():
    return Canvas(32, 32)


def gen_icons(dir_path):
    # Zephyr Ray: top view, head up — mini version of the curved-wing build
    c = _icon_canvas()
    z = palette.ZEPHYR
    cx, cy = 16, 14
    for side in (-1, 1):
        # same organic wing as the sheet, scaled down (no rng: solid creases)
        _ray_wing(c, cx, cy, side, 1, 13, 1, 5, 1.2, 0.3, z)
    c.ellipse(cx, cy + 1, 3, 5.5, z["base"])
    c.ellipse(cx, cy - 4, 2.4, 2.6, z["base"])
    c.ellipse(cx - 2, cy - 6, 1.1, 1.4, z["base"])
    c.ellipse(cx + 2, cy - 6, 1.1, 1.4, z["base"])
    c.ellipse(cx - 1, cy - 2, 2, 3.6, z["light"])
    for i in range(8):
        y = cy + 7 + i
        x = cx + round(1.6 * math.sin(i * 0.5 + 0.4))
        if i < 3:
            c.put(x - 1, y, z["deep"])
            c.put(x, y, z["base"])
            c.put(x + 1, y, z["deep"])
        elif i < 6:
            c.put(x, y, z["base"])
            c.put(x + 1, y, z["deep"])
        else:
            c.put(x, y, z["deep"])
    c.outline(palette.OUTLINE)
    c.put(cx - 2, cy - 5, palette.OUTLINE)
    c.put(cx + 2, cy - 5, palette.OUTLINE)
    c.put(10, cy + 3, z["accent"])
    c.put(22, cy + 3, z["accent"])
    c.save(f"{dir_path}/zephyrray.png")

    # Storm Wisp: mini flame-teardrop matching the animated body
    c = _icon_canvas()
    w = palette.WISP
    c.ellipse(16, 14, 8, 7, w["deep"])
    c.ellipse(16, 20, 5.5, 4, w["deep"])
    c.ellipse(16, 6, 2, 2.5, w["deep"])
    for tx, ln in ((12, 4), (16, 6), (20, 3)):
        for i in range(ln):
            for k in range(2):
                c.put(tx - 1 + k, 22 + i, w["deep"] if i > 1 else w["base"])
    c.ellipse(15, 13, 6, 5.5, w["base"])
    c.ellipse(14, 12, 4, 3.5, w["inner"])
    c.ellipse(14, 11, 2, 2, w["core"])
    c.outline(palette.OUTLINE)
    c.put(25, 10, w["spark"])
    c.put(7, 18, w["spark"])
    c.save(f"{dir_path}/stormwisp.png")

    # Skystone Golem: face block — eyes and moss go on AFTER shading/outline
    c = _icon_canvas()
    g = palette.GOLEM
    c.rect(8, 6, 16, 13, g["light"])
    c.rect(6, 19, 20, 8, g["base"])
    c.shade_topleft(g["hi"], g["deep"])
    c.outline(palette.OUTLINE)
    c.rect(11, 11, 3, 3, g["eye"])
    c.rect(18, 11, 3, 3, g["eye"])
    c.put(12, 11, (240, 252, 252))
    c.put(19, 11, (240, 252, 252))
    c.line(14, 20, 17, 25, g["deep"])
    c.put(10, 22, g["moss"])
    c.put(23, 24, g["moss"])
    c.save(f"{dir_path}/skystonegolem.png")


# --- v0.4 "The Living Sky" fauna ---------------------------------------------
# Galehound (Driftlands night pack hunter) + Dawnpiercer (Aurora Shoals dive
# bird). Standard 6x4/64px walking-mob sheets; the swim column sinks into the
# mist via _mist_overlay. Quadruped construction matched against the vanilla
# wolf/boar sheets: chunky bean body, short 3px legs re-posed per stride, big
# head, ears and tail carrying the silhouette.


def _hound_leg(c, r, hip_x, hip_y, ground, foot_dx, far=False, lift=None):
    """One 3px canine leg, re-posed per stride: the upper segment leans from
    the hip, the lower lands on the displaced paw. `lift=1` mid-steps the paw
    on passing frames."""
    if lift is None:
        lift = 1 if foot_dx > 0 else 0
    foot_x = hip_x + foot_dx
    foot_y = ground - lift
    mid_y = (hip_y + foot_y) // 2
    tone = r["deep"] if far else r["base"]
    ux = hip_x + (foot_x - hip_x) // 2
    for y in range(hip_y, mid_y + 1):
        for k in range(3):
            c.put(ux - 1 + k, y, tone)
    for y in range(mid_y, foot_y + 1):
        for k in range(3):
            c.put(foot_x - 1 + k, y, tone)
    for k in range(3):
        c.put(foot_x - 1 + k, foot_y, r["base"] if far else r["light"])
    c.put(foot_x + (2 if foot_dx >= 0 else -2), foot_y, tone)     # toe


def _hound_plume(c, r, chain):
    """Wind-streamed plume tail: overlapping lobes shrinking toward the tip.
    `chain` = (x, y, radius) triples, root first; lobes 1 and 3 catch light."""
    for i, (px, py, rad) in enumerate(chain):
        c.ellipse(px + 1, py + 1, rad, rad * 0.9, r["deep"])
        c.ellipse(px, py, rad, rad * 0.9, r["base"])
        if i in (1, 3):
            c.ellipse(px - rad * 0.3, py - rad * 0.35, rad * 0.5, rad * 0.45, r["light"])


def _hound_frame(facing, col, swim=False):
    """Galehound: a lean wind-wolf. Vanilla-wolf proportions (big head, short
    legs), wind-swept mane tufts, a drift-plume tail, glowing teal eyes and
    detached mist flecks streaming off the fur — all glow after the outline."""
    step = {0: 0, 1: 1, 2: 0, 3: -1, 4: 0, 5: 0}[col]
    c = Canvas(CELL, CELL)
    r = palette.GALEHOUND
    ground = 52
    bob = 1 if step != 0 else 0
    sway = col * 1.1                                          # tail phase

    if facing == "right":
        cx = 30
        g0 = ground - bob

        def leg_lift(pair):
            # pair 0 = near-front/far-rear, pair 1 = near-rear/far-front
            if col == 2:
                return 1 if pair == 0 else 0
            if col == 4:
                return 1 if pair == 1 else 0
            return None
        # far-side legs behind everything
        if not swim:
            _hound_leg(c, r, cx - 11, ground - 10, ground, 3 * step, far=True, lift=leg_lift(0))
            _hound_leg(c, r, cx + 6, ground - 11, ground, -3 * step, far=True, lift=leg_lift(1))
        # drift-plume tail streaming back off the haunch
        chain = []
        for i, rad in enumerate((2.9, 3.2, 3.0, 2.6, 2.1, 1.6)):
            px = cx - 11 - i * 2
            py = g0 - 16 - round(i * 0.9 + 1.1 * math.sin(i * 0.8 + sway))
            chain.append((px, py, rad))
        _hound_plume(c, r, chain)
        c.ellipse(chain[-1][0] - 3, chain[-1][1] - 1, 1.5, 1.1, r["light"])   # torn-off puff
        # body: chunky bean, shoulder a touch higher (leaning into the wind)
        c.ellipse(cx - 8, g0 - 15, 6.5, 7.0, r["base"])       # haunch
        c.ellipse(cx - 1, g0 - 15, 7.0, 6.2, r["base"])       # barrel
        c.ellipse(cx + 7, g0 - 16, 6.5, 6.5, r["base"])       # chest
        c.ellipse(cx - 8, g0 - 11, 5.0, 2.6, r["deep"])       # haunch shade
        c.ellipse(cx + 1, g0 - 10.5, 6.0, 2.0, r["deep"])     # belly shade
        c.ellipse(cx - 3, g0 - 19, 7.5, 2.6, r["light"])      # back light
        c.ellipse(cx + 7, g0 - 20, 4.5, 2.2, r["light"])
        c.put(cx + 5, g0 - 21, r["hi"])
        c.put(cx + 6, g0 - 21, r["hi"])
        # flank fur streaks
        for fx, fy in ((-11, -14), (-7, -12), (-2, -13), (-9, -17)):
            c.put(cx + fx, g0 + fy, r["deep"])
            c.put(cx + fx + 1, g0 + fy, r["deep"])
        c.put(cx + 2, g0 - 18, r["hi"])
        # neck + big head
        c.ellipse(cx + 11, g0 - 21, 4.5, 4.5, r["base"])
        c.ellipse(cx + 15, g0 - 25, 5.0, 4.5, r["base"])      # skull
        c.ellipse(cx + 13, g0 - 23, 3.8, 3.4, r["base"])      # cheek
        c.ellipse(cx + 14, g0 - 27, 3.4, 2.2, r["light"])     # crown light
        # cheek wind-tufts pointing back
        for i in range(3):
            c.put(cx + 9 - i, g0 - 22 + i // 2, r["base"])
            c.put(cx + 9 - i, g0 - 21 + i // 2, r["deep"])
        # muzzle: short broad wedge, nose drooping at the tip
        for i, x in enumerate(range(cx + 19, cx + 25)):
            top = g0 - 25 + (1 if i >= 4 else 0)
            c.put(x, top, r["light"])
            c.put(x, top + 1, r["base"])
            if i < 4:
                c.put(x, top + 2, r["base"])
        for x in range(cx + 19, cx + 24):                     # mouth line
            c.put(x, g0 - 22, r["deep"])
        # ears: chunky swept-back triangles (far one deep)
        for i in range(4):                                    # far ear
            w = 1 + i // 2
            for k in range(w):
                c.put(cx + 9 - i // 2 + k, g0 - 33 + i, r["deep"])
        for i in range(6):                                    # near ear, i=0 tip
            w = 1 + (i + 1) // 2
            x0 = cx + 11 - (5 - i) // 2
            for k in range(w):
                c.put(x0 + k, g0 - 34 + i, r["light"] if k == w - 1 and i > 1 else r["base"])
        # mane tufts streaming off neck + shoulder
        for tx0, ty0, ln in ((cx + 10, g0 - 28, 4), (cx + 6, g0 - 25, 4), (cx + 2, g0 - 22, 3)):
            for i in range(ln):
                c.put(tx0 - i, ty0 + i // 2, r["light"] if i < 2 else r["base"])
                c.put(tx0 - i, ty0 + 1 + i // 2, r["base"] if i < ln - 1 else r["deep"])
        # near legs + chest fluff
        if not swim:
            _hound_leg(c, r, cx - 7, ground - 9, ground, -3 * step, lift=leg_lift(1))
            _hound_leg(c, r, cx + 10, ground - 10, ground, 3 * step, lift=leg_lift(0))
        for i, (fx, fy) in enumerate(((12, -13), (13, -12), (11, -11), (12, -10), (13, -14))):
            c.put(cx + fx, g0 + fy, r["hi"] if i % 2 else r["light"])
        c.outline(palette.OUTLINE)
        # face + glow after the outline
        c.put(cx + 15, g0 - 27, palette.OUTLINE)              # brow
        c.put(cx + 16, g0 - 27, palette.OUTLINE)
        c.put(cx + 15, g0 - 26, r["eye"])
        c.put(cx + 16, g0 - 26, r["eye"])
        c.put(cx + 15, g0 - 26, r["hi"])
        c.put(cx + 17, g0 - 26, with_alpha(r["eye"], 110))
        c.put(cx + 23, g0 - 24, palette.OUTLINE)              # nostril
        # hi streak riding the plume + drift flecks
        for i in (1, 3):
            px, py, rad = chain[i]
            c.put(px, py - round(rad * 0.8), r["hi"])
        tx, ty, _ = chain[-1]
        c.put(tx - 5, ty - 2, with_alpha(r["hi"], 150))
        c.put(tx - 7, ty, with_alpha(r["hi"], 110))
        c.put(cx + 1, g0 - 24, with_alpha(r["hi"], 120))
    elif facing == "up":
        cx = 32
        g0 = ground - bob
        # hourglass back: head, shoulder cape, waist, wide butt
        c.ellipse(cx, g0 - 11, 9.0, 6.5, r["base"])           # butt
        c.ellipse(cx, g0 - 18, 7.0, 6.0, r["base"])           # waist
        c.ellipse(cx, g0 - 26, 8.0, 4.8, r["base"])           # shoulders
        c.ellipse(cx, g0 - 31, 5.0, 4.2, r["base"])           # head back
        c.ellipse(cx + 4, g0 - 12, 4.5, 3.5, r["deep"])       # rump shade
        c.ellipse(cx + 3, g0 - 20, 3.0, 4.0, r["deep"])       # flank shade
        c.ellipse(cx - 3, g0 - 27, 4.0, 2.6, r["light"])      # shoulder light
        c.ellipse(cx - 2, g0 - 32, 3.2, 2.4, r["light"])      # head light
        c.put(cx - 4, g0 - 33, r["hi"])
        c.put(cx - 5, g0 - 27, r["hi"])
        for dx in range(-3, 4):                               # neck shadow
            c.put(cx + dx, g0 - 28, r["deep"])
        # upright chunky ears, inner edge shaded
        for side in (-1, 1):
            for i in range(5):                                # i=0 tip
                w = 1 + (i + 1) // 2
                x0 = cx + side * 4 - w // 2
                for k in range(w):
                    inner = i >= 2 and k == (0 if side > 0 else w - 1)
                    c.put(x0 + k, g0 - 38 + i, r["deep"] if inner else r["base"])
        # spine + fur chevrons
        for y in range(g0 - 25, g0 - 12):
            c.put(cx, y, r["deep"])
        for y in range(g0 - 23, g0 - 13, 3):
            c.put(cx - 1, y, r["deep"])
            c.put(cx + 1, y + 1, r["deep"])
        # shoulder tufts pointing down-out
        for side in (-1, 1):
            sx = cx + side * 8
            for i in range(3):
                c.put(sx + side * (i // 2), g0 - 24 + i, r["light"] if i == 0 else r["base"])
                c.put(sx + side * (1 + i // 2), g0 - 23 + i, r["deep"] if i == 2 else r["base"])
        # hind legs stride vertically
        if not swim:
            for side, s in ((-1, step), (1, -step)):
                lift = 2 if s > 0 else (1 if (col == 2 and side < 0) or (col == 4 and side > 0) else 0)
                lx = cx + side * 7
                c.rect(lx - 1, ground - 8 - lift, 3, 9, r["base"])
                for k in range(3):
                    c.put(lx - 1 + k, ground - lift, r["light"])
                c.put(lx - 1 + (0 if side < 0 else 2), ground - 4 - lift, r["deep"])
        # plume tail hanging over the butt toward the viewer
        chain = []
        for i, rad in enumerate((2.4, 2.8, 2.6, 2.2, 1.7)):
            px = cx + 1 + round(0.4 * i + 1.2 * math.sin(i * 0.9 + sway))
            py = g0 - 10 + round(i * 2.6)
            chain.append((px, py, rad))
        _hound_plume(c, r, chain)
        c.outline(palette.OUTLINE)
        for i in (1, 3):
            px, py, rad = chain[i]
            c.put(px - round(rad * 0.5), py, r["hi"])
        tx, ty, _ = chain[-1]
        c.put(tx + 2, ty + 3, with_alpha(r["hi"], 150))
        c.put(tx, ty + 5, with_alpha(r["hi"], 110))
    else:  # down
        cx = 32
        g0 = ground - bob
        # plume tail tip rising past the right shoulder (drawn first, behind)
        chain = []
        for i, rad in enumerate((2.4, 2.6, 2.2, 1.7)):
            px = cx + 8 + round(i * 0.9 + 0.9 * math.sin(i * 0.9 + sway))
            py = g0 - 13 - round(i * 2.6)
            chain.append((px, py, rad))
        _hound_plume(c, r, chain)
        # body: chest + haunches bulging at the sides
        c.ellipse(cx, g0 - 13, 7.5, 6.5, r["base"])
        c.ellipse(cx - 8, g0 - 10, 4.5, 4.5, r["base"])
        c.ellipse(cx + 8, g0 - 10, 4.5, 4.5, r["base"])
        c.ellipse(cx - 8, g0 - 8, 3.5, 2.2, r["deep"])
        c.ellipse(cx + 8, g0 - 8, 3.5, 2.2, r["deep"])
        if not swim:
            for side in (-1, 1):                              # rear paws peeking
                c.rect(cx + side * 9 - 1, ground - 2, 3, 2, r["deep"])
        # chest fluff: checker dither under the chin
        for y in range(g0 - 17, g0 - 12):
            for x in range(cx - 3, cx + 4):
                if (x + y) % 2 == 0:
                    c.put(x, y, r["light"])
        c.put(cx - 1, g0 - 17, r["hi"])
        c.put(cx + 1, g0 - 16, r["hi"])
        # big head over the chest
        c.ellipse(cx, g0 - 26, 8.0, 7.0, r["base"])
        c.ellipse(cx - 2, g0 - 30, 4.5, 2.8, r["light"])      # crown
        c.ellipse(cx - 5, g0 - 21, 2.2, 2.4, r["deep"])       # cheek shade
        c.ellipse(cx + 5, g0 - 21, 2.2, 2.4, r["deep"])
        # wide-based ears, dark inner fill
        for side in (-1, 1):
            for i in range(5):                                # i=0 tip
                w = 2 + (i + 1) // 2
                x0 = cx + side * (6 + (1 if i < 2 else 0)) - w // 2
                for k in range(w):
                    c.put(x0 + k, g0 - 37 + i, r["base"])
            for i in range(2, 5):                             # inner shadow
                w = 2 + (i + 1) // 2
                x0 = cx + side * 6 - w // 2
                for k in range(1, w - 1):
                    c.put(x0 + k, g0 - 37 + i, r["deep"])
        # brow stripe + muzzle
        c.rect(cx - 1, g0 - 28, 2, 4, r["light"])
        c.ellipse(cx, g0 - 22, 3.5, 2.6, r["light"])
        # cheek wind-tufts pointing out
        for side in (-1, 1):
            c.put(cx + side * 9, g0 - 25, r["base"])
            c.put(cx + side * 10, g0 - 24, r["base"])
            c.put(cx + side * 10, g0 - 23, r["deep"])
        # front legs
        if not swim:
            for side, s in ((-1, step), (1, -step)):
                lift = 1 if s > 0 else (1 if (col == 2 and side < 0) or (col == 4 and side > 0) else 0)
                lx = cx + side * 4
                c.rect(lx - 1, ground - 8 - lift, 3, 9, r["base"])
                for k in range(3):
                    c.put(lx - 1 + k, ground - lift, r["light"])
                c.put(lx - 1 + (0 if side < 0 else 2), ground - 4 - lift, r["deep"])
        c.outline(palette.OUTLINE)
        # face after the outline: angry brow, glowing eyes, nose, mouth
        for side in (-1, 1):
            c.put(cx + side * 2, g0 - 29, palette.OUTLINE)
            c.put(cx + side * 3, g0 - 29, palette.OUTLINE)
            c.put(cx + side * 4, g0 - 30, palette.OUTLINE)
        for ex in (cx - 4, cx + 3):
            c.rect(ex, g0 - 28, 2, 2, r["eye"])
            c.put(ex, g0 - 28, r["hi"])
        c.put(cx - 6, g0 - 28, with_alpha(r["eye"], 110))
        c.put(cx + 5, g0 - 28, with_alpha(r["eye"], 110))
        c.rect(cx - 1, g0 - 24, 2, 2, palette.OUTLINE)        # nose
        for dx in range(-1, 2):                               # mouth
            c.put(cx + dx, g0 - 21, r["deep"])
        c.put(cx - 2, g0 - 20, r["deep"])
        c.put(cx + 2, g0 - 20, r["deep"])
        # drift flecks off the tail tip
        tx, ty, _ = chain[-1]
        c.put(tx + 2, ty - 3, with_alpha(r["hi"], 140))
        c.put(tx + 4, ty - 5, with_alpha(r["hi"], 110))
        for i in (1, 3):
            px, py, rad = chain[i]
            c.put(px, py - round(rad * 0.7), r["hi"])
    if swim:
        _mist_overlay(c)
    return c


def gen_galehound(path):
    sheet = Canvas(COLS * CELL, ROWS * CELL)
    for col in range(COLS):
        swim = col == 5
        up = _hound_frame("up", col, swim)
        right = _hound_frame("right", col, swim)
        down = _hound_frame("down", col, swim)
        left = right.mirrored()
        for row, sprite in enumerate((up, right, down, left)):
            sheet.paste(sprite, col * CELL, row * CELL)
    sheet.save(path)


# --- Dawnpiercer -------------------------------------------------------------

def _dp_wing(c, cx, cy, side, spread, frame_seed):
    """One feathered bird wing, top view: the inner arm bows forward to the
    wrist, the primaries sweep back as the spread closes. The outer trailing
    edge is cut into feather fingers with radial separations; the inner edge
    stays a smooth secondary curve. Returns the finger-tip points so crystal
    accents can ride them after the outline."""
    r = palette.DAWNPIERCER
    span = round(11 + 15 * spread)
    droop = round(2 + 13 * (1 - spread))
    root = 2
    sh = cy - 5.0
    lead = (sh, sh - 4.5 - 2.5 * spread, sh + droop)
    trail = (cy + 7.0, cy + 8.5, sh + droop + 2.5)

    def bez(p, f):
        return (1 - f) * (1 - f) * p[0] + 2 * f * (1 - f) * p[1] + f * f * p[2]

    phase = 0.12 + 0.27 * ((frame_seed * 5) % 3)
    tips = []
    cols = []
    for xi in range(root, span + 1):
        f = (xi - root) / max(span - root, 1)
        yl = bez(lead, f)
        yt = bez(trail, f)
        g = (f * 3.2 + phase) % 1.0
        if f > 0.42:
            yt -= 3.4 * abs(math.sin(g * math.pi)) ** 0.8     # feather fingers
        elif f > 0.2:
            yt -= 1.0 * abs(math.sin(g * math.pi))            # gentle scallop
        yl_i = round(yl)
        yt_i = round(max(yt, yl + (2 if f < 0.96 else 1)))
        x = cx + side * xi
        cols.append((x, yl_i, yt_i, f, g))
        for y in range(yl_i, yt_i + 1):
            c.put(x, y, r["base"])
    for x, yl, yt, f, g in cols:
        h = yt - yl
        if h >= 4:
            c.put(x, yl + 1, r["light"])
            if h >= 8:
                c.put(x, yl + 2, r["light"])
            c.put(x, yt - 1, r["deep"])
        if f < 0.3 and h >= 6:
            c.put(x, yl + 3, r["light"])
        if f > 0.45 and (g < 0.10 or g > 0.90) and h >= 5:    # feather splits
            for y in range(yl + max(2, h // 2), yt):
                c.put(x, y, r["deep"])
        if f > 0.6 and 0.44 < g < 0.52:
            tips.append((x, yt - 1))
    return tips


def _dp_base(spread, frame_seed):
    """Top view, head up: warm dawn-lit body, dark piercing beak, solid
    crystal crest and tail fan. Wings re-pose per frame."""
    c = Canvas(CELL, CELL)
    r = palette.DAWNPIERCER
    cx, cy = 32, 31
    tips = []
    for side in (-1, 1):
        tips += _dp_wing(c, cx, cy, side, spread, frame_seed)
    # crystal tail fan: solid stubby shards (sides first, center on top)
    sway = (0, 1, 0, -1)[frame_seed % 4]
    shards = ((-4 + sway, 7), (4 + sway, 7), (sway, 9))
    for tdx, ln in shards:
        for i in range(ln):
            x = cx + round(tdx * i / ln)
            y = cy + 7 + i
            w = 4 if i < ln - 3 else (3 if i < ln - 1 else 1)
            for k in range(w):
                tone = r["crystal_deep"] if (k == w - 1 and w > 1) or i >= ln - 2 else r["crystal"]
                c.put(x - w // 2 + k, y, tone)
    # body teardrop over the wing roots + shard bases
    c.ellipse(cx + 1, cy + 1, 5.2, 8.6, r["deep"])
    c.ellipse(cx, cy, 5.0, 8.4, r["base"])
    c.ellipse(cx, cy - 9, 3.9, 4.1, r["base"])
    c.ellipse(cx, cy + 8, 2.8, 2.4, r["base"])
    c.ellipse(cx - 1, cy - 3, 3.2, 5.6, r["light"])
    c.ellipse(cx - 1, cy - 9, 2.6, 2.8, r["light"])
    # back feather scales
    for sx, sy in ((-2, -3), (2, -2), (-1, 0), (2, 2), (-2, 3), (1, 5), (-1, 7)):
        c.put(cx + sx, cy + sy, r["deep"])
        c.put(cx + sx + 1, cy + sy, r["deep"])
    # crystal crest: solid mohawk down the neck, teeth swept back at 45°
    for i in range(6):
        y = cy - 8 + i
        w = (1, 3, 3, 3, 2, 1)[i]
        for k in range(w):
            c.put(cx - w // 2 + k, y, r["crystal"] if k < w - 1 or w == 1 else r["crystal_deep"])
    for side in (-1, 1):
        for i in range(3):
            x = cx + side * (2 + i)
            y = cy - 6 + i
            c.put(x, y, r["crystal"])
            c.put(x, y + 1, r["crystal_deep"])
    # beak: dark piercing wedge
    for i in range(6):
        y = cy - 13 - i
        w = 3 if i < 2 else (2 if i < 4 else 1)
        for k in range(w):
            c.put(cx - w // 2 + k, y, r["beak"])
    c.outline(palette.OUTLINE)
    # after the outline: beak core, eyes, crystal cores + glints
    for i in range(4):
        c.put(cx, cy - 13 - i, r["beak"])
    c.put(cx - 1, cy - 13, r["beak"])
    c.put(cx - 1, cy - 12, r["beak"])
    for ex in (cx - 3, cx + 3):
        c.put(ex, cy - 10, palette.OUTLINE)
        c.put(ex, cy - 11, r["light"])
    for (x, y) in tips:
        c.put(x, y, r["crystal"])
    c.put(cx, cy - 7, r["crystal"])                           # crest cores
    c.put(cx, cy - 6, r["crystal"])
    for side in (-1, 1):
        c.put(cx + side * 3, cy - 5, r["crystal"])
        c.put(cx + side * 4, cy - 4, r["crystal"])
        c.put(cx + side * 5, cy - 3, with_alpha(r["crystal"], 140))
    # tail shard cores: solid crystal spines + tip glints
    for tdx, ln in shards:
        for i in range(1, ln - 2):
            x = cx + round(tdx * i / ln)
            c.put(x, cy + 7 + i, r["crystal"])
            if i < ln - 3:
                c.put(x - 1, cy + 7 + i, r["crystal"])
        c.put(cx + tdx, cy + 8 + ln, with_alpha(r["crystal"], 130))
    return c


def gen_dawnpiercer(path):
    spreads = {0: 0.75, 1: 1.0, 2: 0.7, 3: 0.35, 4: 0.7, 5: 0.75}
    sheet = Canvas(COLS * CELL, ROWS * CELL)
    for col in range(COLS):
        up = _dp_base(spreads[col], col)
        right = _rotated(up, Image.ROTATE_270)
        down = _rotated(up, Image.ROTATE_180)
        left = _rotated(up, Image.ROTATE_90)
        frames = (up, right, down, left)
        if col == 5:
            for f in frames:
                _mist_overlay(f)
        for row, sprite in enumerate(frames):
            sheet.paste(sprite, col * CELL, row * CELL)
    sheet.save(path)


def gen_icons_v04(dir_path):
    # Galehound: head/shoulder crop — big furry head, brow, glowing eyes
    c = _icon_canvas()
    r = palette.GALEHOUND
    c.ellipse(16, 26, 11.0, 6.0, r["base"])       # shoulder ruff
    c.ellipse(16, 28, 8.0, 3.6, r["deep"])
    c.ellipse(16, 14, 9.0, 8.0, r["base"])        # head
    c.ellipse(14, 10, 5.0, 3.0, r["light"])       # crown
    c.ellipse(10, 18, 2.4, 2.6, r["deep"])        # cheek shade
    c.ellipse(22, 18, 2.4, 2.6, r["deep"])
    for side in (-1, 1):                          # wide-based ears, dark inner
        for i in range(5):                        # i=0 tip
            w = 2 + (i + 1) // 2
            x0 = 16 + side * (8 + (1 if i < 2 else 0)) - w // 2
            for k in range(w):
                c.put(x0 + k, 3 + i, r["base"])
        for i in range(1, 5):
            w = 2 + (i + 1) // 2
            x0 = 16 + side * (8 + (1 if i < 2 else 0)) - w // 2
            for k in range(1, w - 1):
                c.put(x0 + k, 3 + i, r["deep"])
        c.put(16 + side * 11, 14, r["base"])      # cheek wind-tufts
        c.put(16 + side * 12, 15, r["base"])
        c.put(16 + side * 12, 16, r["deep"])
    c.rect(15, 11, 2, 4, r["light"])              # brow stripe
    c.ellipse(16, 18, 4.0, 2.8, r["light"])       # muzzle
    for y in range(23, 27):                       # chest fluff checker
        for x in range(12, 21):
            if (x + y) % 2 == 0:
                c.put(x, y, r["light"])
    c.put(15, 23, r["hi"])
    c.put(18, 24, r["hi"])
    c.outline(palette.OUTLINE)
    for side in (-1, 1):                          # angry brow
        c.put(16 + side * 2, 11, palette.OUTLINE)
        c.put(16 + side * 3, 11, palette.OUTLINE)
        c.put(16 + side * 4, 10, palette.OUTLINE)
    for ex in (11, 19):                           # glowing eyes
        c.rect(ex, 12, 2, 2, r["eye"])
        c.put(ex, 12, r["hi"])
    c.put(9, 12, with_alpha(r["eye"], 110))
    c.put(22, 12, with_alpha(r["eye"], 110))
    c.rect(15, 16, 2, 2, palette.OUTLINE)         # nose
    for dx in range(-1, 2):                       # mouth
        c.put(16 + dx, 19, r["deep"])
    c.put(14, 20, r["deep"])
    c.put(18, 20, r["deep"])
    c.save(f"{dir_path}/galehound.png")

    # Dawnpiercer: head/shoulder profile — piercing beak, solid crystal crest
    c = _icon_canvas()
    r = palette.DAWNPIERCER
    c.ellipse(12, 23, 9.0, 6.5, r["base"])        # breast/shoulder
    c.ellipse(9, 25, 5.0, 3.4, r["light"])
    c.ellipse(14, 12, 6.5, 6.0, r["base"])        # head
    c.ellipse(12, 10, 3.8, 3.2, r["light"])
    for i in range(10):                           # long piercing beak
        x = 20 + i
        w = 3 if i < 4 else (2 if i < 7 else 1)
        y0 = 11 - w // 2 + (i // 5)
        for k in range(w):
            c.put(x, y0 + k, r["beak"])
    # crest: three solid crystal shards swept back-left
    for j, (x0, y0, ln) in enumerate(((10, 8, 5), (14, 7, 6), (17, 7, 5))):
        for i in range(ln):
            x = x0 - i // 2 - j
            y = y0 - i
            c.put(x, y, r["crystal"])
            c.put(x + 1, y, r["crystal"])
            c.put(x + 2, y, r["crystal_deep"])
    for sx, sy in ((7, 21), (11, 23), (15, 22), (9, 25)):     # feather flecks
        c.put(sx, sy, r["deep"])
        c.put(sx + 1, sy, r["deep"])
    # folded wing edge with crystal-tipped primaries
    c.ellipse(18, 25, 3.4, 4.2, r["deep"])
    c.put(17, 22, r["light"])
    c.put(18, 23, r["light"])
    c.outline(palette.OUTLINE)
    for i in range(7):                            # beak core stays dark violet
        c.put(21 + i, 11 + (i // 5), r["beak"])
    c.put(21, 10, r["light"])                     # beak-base glint
    c.put(16, 10, palette.OUTLINE)                # eye
    c.put(16, 9, r["light"])
    for j, (x0, y0, ln) in enumerate(((10, 8, 5), (14, 7, 6), (17, 7, 5))):
        for i in range(1, ln - 1):                # crest cores
            c.put(x0 - i // 2 - j + 1, y0 - i, r["crystal"])
    c.put(12, 1, with_alpha(r["crystal"], 140))
    c.put(19, 26, r["crystal"])                   # wing-tip crystal
    c.put(20, 24, r["crystal"])
    c.save(f"{dir_path}/dawnpiercer.png")
