"""Mob sprite sheets.

Walking/flying 4-direction sheets are 6 columns x 4 rows of 64x64 cells
(384x256): columns = idle, walk 1-4, in-liquid; rows = Up, Right, Down, Left
(see docs/research/asset-formats.md). The Storm Wisp uses the vanilla
flying-spirit layout: column 0 = 4 stacked body frames, column 1 = matching
glow overlays (128x256).
"""

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

def _ray_base(wing_spread, frame_seed):
    """Top view, head up. wing_spread in [0..1]: 0 folded, 1 full."""
    c = Canvas(CELL, CELL)
    ramp = palette.ZEPHYR
    rng = Rng(0x2A7 + frame_seed)
    cx, cy = 32, 32
    half_span = round(12 + 14 * wing_spread)
    sweep = round(6 + 3 * (1 - wing_spread))
    # wings: broad triangles from the body out and slightly back
    for side in (-1, 1):
        tip_x = cx + side * half_span
        tip_y = cy + sweep
        for t in range(0, 101, 4):
            f = t / 100.0
            x_edge = round(cx + side * 4 + (tip_x - cx - side * 4) * f)
            y_front = round(cy - 9 + (tip_y - (cy - 9)) * f)
            y_back = round(cy + 8 + (tip_y - (cy + 8)) * f * 1.05)
            for y in range(min(y_front, y_back), max(y_front, y_back) + 1):
                c.put(x_edge, y, ramp["base"])
    # body: sleek lens with head up
    c.ellipse(cx, cy, 5.5, 11, ramp["base"])
    c.ellipse(cx, cy - 2, 4, 7, ramp["light"])
    # head tip + eye ridges
    c.ellipse(cx, cy - 10, 2.5, 2.5, ramp["light"])
    c.put(cx - 2, cy - 10, palette.OUTLINE)
    c.put(cx + 2, cy - 10, palette.OUTLINE)
    # tail whip
    for i in range(9):
        c.put(cx + (1 if i % 3 == 2 else 0), cy + 11 + i, ramp["deep"] if i > 4 else ramp["base"])
    # wing finger ridges radiating toward the tips (drawn before shading)
    for side in (-1, 1):
        for k, back in ((0.45, 2), (0.75, 5)):
            ex = cx + side * round(half_span * k)
            ey = cy + round(sweep * k) + back
            c.line(cx + side * 4, cy + back - 2, ex, ey, ramp["deep"])
    # spine pattern: diamond chain down the back
    for i, sy in enumerate(range(cy - 7, cy + 8, 4)):
        c.put(cx, sy, ramp["deep"])
        c.put(cx - 1, sy + 1, ramp["deep"])
        c.put(cx + 1, sy + 1, ramp["deep"])
        c.put(cx, sy + 1, ramp["hi"])
        c.put(cx, sy + 2, ramp["deep"])
    # wing shading: leading edge light, trailing dark
    c.shade_topleft(ramp["hi"], ramp["deep"])
    c.outline(palette.OUTLINE)
    # accents after the outline: teal spot rows + bright wing-tip rims
    for side in (-1, 1):
        for k in (0.5, 0.72, 0.9):
            sx = cx + side * round((4 + (half_span - 4) * k))
            sy = cy + round(sweep * k) + 1
            c.put(sx, sy, ramp["accent"])
        tipx = cx + side * half_span
        c.put(tipx, cy + sweep - 1, ramp["hi"])
    c.put(cx - 2, cy - 10, palette.OUTLINE)
    c.put(cx + 2, cy - 10, palette.OUTLINE)
    c.put(cx - 2, cy - 9, ramp["accent"])
    c.put(cx + 2, cy - 9, ramp["accent"])
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
    # Zephyr Ray: top view, head up — matches the in-world silhouette
    c = _icon_canvas()
    z = palette.ZEPHYR
    for side in (-1, 1):
        c.line(16 + side * 3, 14, 16 + side * 13, 19, z["base"])
        c.line(16 + side * 3, 17, 16 + side * 13, 20, z["base"])
        c.line(16 + side * 3, 15, 16 + side * 12, 19, z["light"])
        c.line(16 + side * 3, 16, 16 + side * 13, 20, z["deep"])
    c.ellipse(16, 16, 4, 8, z["base"])
    c.ellipse(16, 14, 3, 5, z["light"])
    for i in range(6):
        c.put(16, 24 + i, z["deep"])
    c.outline(palette.OUTLINE)
    c.put(14, 10, palette.OUTLINE)
    c.put(18, 10, palette.OUTLINE)
    c.put(10, 17, z["accent"])
    c.put(22, 17, z["accent"])
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
