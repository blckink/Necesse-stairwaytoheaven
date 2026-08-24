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
              rng=None, tip_chord=1.5, root_lead=7.0, root_trail=8.0, curl=0.0):
    """One organic manta wing, top view: convex CURVED leading edge (quadratic
    arc bulging forward), gently scalloped trailing edge (three shallow bites
    between finger points), membrane shaded in bands that follow the sweep.

    v0.5 flap params (the walk columns used to differ only by `span`, so the
    frames read as the same shape scaled):
      `curl`      hooks the outer 45% of the wing back (+) or forward (-),
                  which is what actually changes the silhouette through a
                  stroke rather than the reach alone;
      `tip_chord` fattens the tip — on the upstroke the membrane curls over
                  and you see its underside, so the tip is blunt, not sharp;
      `root_lead`/`root_trail` set the root chord, i.e. the body pitch.
    Returns the per-column geometry so accents can ride the same curves."""
    tip_y = cy + droop
    lead = (cy - root_lead, cy - root_lead - camber, tip_y - tip_chord * 0.5)
    trail = (cy + root_trail, cy + root_trail + 1.0 + droop * 0.55,
             tip_y + tip_chord * 0.5)

    def bez(p, f):
        return (1 - f) * (1 - f) * p[0] + 2 * f * (1 - f) * p[1] + f * f * p[2]

    cols = []
    for xi in range(root, span + 1):
        f = (xi - root) / max(span - root, 1)
        yl = bez(lead, f)
        yt = bez(trail, f)
        if curl and f > 0.55:                  # outer-wing hook (the stroke)
            k = ((f - 0.55) / 0.45) ** 2
            yl += curl * k
            yt += curl * k * 1.15
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


# Flap cycle, top view, head up. One tuple per sheet column:
#   span        wing reach in px from the body centre
#   droop       tip offset along the body axis (+ = swept back toward the tail)
#   camber      forward bow of the leading edge
#   root_lead   chord ahead of the spine at the wing root  ) body pitch
#   root_trail  chord behind the spine at the wing root    )
#   tip_chord   thickness of the wing tip (blunt when curled over)
#   curl        outer-wing hook: - reaches forward, + hooks back
#   body_rx/ry  mantle ellipse (compresses on the power stroke)
#   head_dy     head/cephalic-lobe pitch
#   tail_amp    S-curve amplitude of the whip tail
#   tail_phase  S-curve phase, so the tail lashes rather than translating
# Columns are idle, flap 1-4, in-liquid; the walk columns run
# downstroke -> recovery -> upstroke -> mid-downstroke so every adjacent pair
# differs in reach AND in tip direction AND in body length.
_RAY_POSES = {
    0: (24,  4, 6, 7.0,  8.0, 1.8,  0.0, 5.5, 10.0,  0, 2.2, 0.0),
    1: (27, -2, 9, 5.6,  6.8, 1.4, -3.0, 6.4,  8.4, -2, 1.4, 0.7),
    2: (21,  7, 5, 7.6,  8.6, 3.2,  2.0, 5.2, 10.4,  0, 3.0, 1.9),
    3: (14, 13, 3, 9.0, 10.4, 5.5,  4.0, 4.4, 12.2,  2, 3.4, 3.2),
    4: (23,  1, 8, 6.4,  7.4, 2.0, -1.6, 6.0,  9.0, -1, 2.6, 4.1),
    5: (23,  4, 6, 7.0,  8.0, 2.0,  0.0, 5.5, 10.0,  0, 2.2, 1.1),
}


def _ray_base(pose, frame_seed):
    """Top view, head up, one flap pose (see _RAY_POSES).

    Organic manta build: wings are curved membranes that hook forward on the
    power stroke and curl back and blunt on the recovery, the wing roots are
    buried under the body mass so they blend seamlessly, the mantle stretches
    and compresses with the stroke, and the tail lashes in an S-curve whose
    amplitude and phase are both re-posed per frame."""
    (span, droop, camber, root_lead, root_trail, tip_chord, curl,
     body_rx, body_ry, head_dy, tail_amp, tail_phase) = pose
    c = Canvas(CELL, CELL)
    ramp = palette.ZEPHYR
    rng = Rng(0x2A7 + frame_seed * 7919)
    cx, cy = 32, 30
    bite = 1.4 + 0.06 * droop
    phase = 0.15 + rng.float() * 0.35              # scallops ripple per frame
    wings = {}
    for side in (-1, 1):
        wings[side] = _ray_wing(c, cx, cy, side, 2, span, droop, camber,
                                bite, phase, ramp, rng, tip_chord=tip_chord,
                                root_lead=root_lead, root_trail=root_trail,
                                curl=curl)
    # body: overlapping round masses over the wing roots (deep under-crescent,
    # base mass, light upper-left sheen — the house volumetric construction)
    hy = cy - round(body_ry * 0.80) + head_dy      # head mass centre
    c.ellipse(cx + 1, cy + 3, body_rx, body_ry - 0.5, ramp["deep"])
    c.ellipse(cx, cy + 1, body_rx, body_ry, ramp["base"])
    c.ellipse(cx, hy, body_rx * 0.74, 4.4, ramp["base"])         # head mass
    c.ellipse(cx - 3, hy - 4, 1.8, 2.4, ramp["base"])            # cephalic lobes
    c.ellipse(cx + 3, hy - 4, 1.8, 2.4, ramp["base"])
    c.ellipse(cx - 1, cy - 3, body_rx * 0.66, body_ry * 0.70, ramp["light"])
    c.ellipse(cx - 1, hy, 2.6, 2.8, ramp["light"])
    c.ellipse(cx - 3, hy - 4, 1.1, 1.5, ramp["light"])
    c.ellipse(cx - 2, cy - 5, 1.5, 2.4, ramp["hi"])
    # back pattern: paired dark spots down the mantle (no diamond chain)
    for sx, sy in ((-2, -1), (2, -1), (-3, 4), (3, 4), (0, 7)):
        c.put(cx + sx, cy + sy, ramp["deep"])
    # pelvic fin bumps at the tail root
    tail_root = cy + round(body_ry * 0.86)
    c.ellipse(cx - 3, tail_root - 1, 2, 1.6, ramp["base"])
    c.ellipse(cx + 3, tail_root - 1, 2, 1.6, ramp["base"])
    c.put(cx - 3, tail_root, ramp["deep"])
    c.put(cx + 3, tail_root, ramp["deep"])
    # tail: S-curved whip tapering from a 5px root to a dark tip. Amplitude
    # AND phase move per frame, so it lashes across the axis instead of
    # sliding along with the body. The old 3px root left a 1px core after the
    # outline pass and vanished at 1x — the taper starts wide on purpose.
    for i in range(15):
        y = tail_root - 2 + i
        x = cx + round(tail_amp * math.sin(i * 0.36 + tail_phase))
        w = 5 if i < 3 else (4 if i < 6 else 3)
        for k in range(w):
            xx = x - w // 2 + k
            edge = k == 0 or k == w - 1
            c.put(xx, y, ramp["deep"] if (edge or i >= 11) else ramp["base"])
        if 2 <= i <= 8 and w >= 4:
            c.put(x - w // 2 + 1, y, ramp["light"])
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
    c.put(cx - 3, hy - 5, palette.OUTLINE)
    c.put(cx + 3, hy - 5, palette.OUTLINE)
    c.put(cx - 3, hy - 4, ramp["accent"])
    c.put(cx + 3, hy - 4, ramp["accent"])
    return c


def gen_zephyrray(path):
    sheet = Canvas(COLS * CELL, ROWS * CELL)
    for col in range(COLS):
        up = _ray_base(_RAY_POSES[col], col)
        right = _rotated(up, Image.ROTATE_270)
        down = _rotated(up, Image.ROTATE_180)
        left = _rotated(up, Image.ROTATE_90)
        frames = (up, right, down, left)
        if col == 5:
            # the mist waterline is world-space: apply it AFTER the rotation,
            # or the in-liquid rows show a vertical mist band on their side
            for f in frames:
                _mist_overlay(f)
        for row, sprite in enumerate(frames):
            sheet.paste(sprite, col * CELL, row * CELL)
    sheet.save(path)


# --- Skystone Golem ----------------------------------------------------------

def _stone(c, cx, cy, rx, ry, r, pits=3, seed=0):
    """One faceted STONE PLATE (v0.5 rebuild).

    v0.4 drew soft shaded balls whose highlights blurred into each other, so
    the golem read as one grey rubber mass. Vanilla heavy mobs (ashgolem,
    crystalgolem) build the same volume out of hard-edged plates instead:
    every plate carries its own dark crevice ring, a flat lit face cut by a
    straight facet edge, a 1px bright rim on the top-left arc and a scatter
    of quarry pits. Plates drawn later cut visible grooves into the ones
    behind them, which is what makes the material read as rock."""
    # crevice ring: the seam that separates this plate from its neighbours
    c.ellipse(cx, cy, rx + 1, ry + 1, r["deep"])
    c.ellipse(cx, cy + 0.5, rx, ry, r["base"])          # shadowed plate body
    # lit face: the plate ellipse clipped by a top-left plane, so the light /
    # shadow boundary is a straight facet edge and not a soft gradient
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
            dx = (x - cx) / max(rx, 0.001)
            dy = (y - cy - 0.5) / max(ry, 0.001)
            if dx * dx + dy * dy <= 1.0 and dx + dy < -0.12 + 0.07 * (seed % 5 - 2):
                c.put(x, y, r["light"])
    # bright rim along the top-left arc, inset 1px so the outline pass cannot
    # eat it on silhouette plates
    steps = max(8, int((rx + ry) * 1.6))
    for i in range(steps):
        a = 2.0 * math.pi * i / steps
        nx, ny = math.cos(a), math.sin(a)
        if nx + ny > -0.60:
            continue
        c.put(round(cx + nx * (rx - 1.1)), round(cy + 0.5 + ny * (ry - 1.1)),
              r["hi"])
    # quarry pits: single dark specks on the lit face (vanilla rock density)
    for i in range(pits):
        a = 0.9 + 1.7 * ((seed + i * 5) % 7) / 7.0
        d = 0.30 + 0.34 * ((seed + i * 3) % 5) / 5.0
        c.put(round(cx - rx * d * math.cos(a)), round(cy - ry * d * math.sin(a)),
              r["deep"])


def _golem_bands(c, r, cx, bands):
    """Hard-edged armour plate bands across a torso — the ashgolem read: a
    dark crevice groove with a bright lit lip immediately under it, arcing
    with the barrel of the body. These bands are what separate 'stacked rock
    plates' from 'one smooth grey mass' at 1x."""
    for by, half in bands:
        for dx in range(-half, half + 1):
            arc = by + abs(dx) // 5
            c.put(cx + dx, arc, r["deep"])                       # crevice
            c.put(cx + dx, arc + 1,                              # lit lip
                  r["hi"] if dx < -2 else r["light"])


def _moss(c, r, mx, my, w=3, h=2):
    """A moss patch that reads as vegetation: a lit crown row over a darker
    body, sitting ON the upper surface of a plate with a shadow underneath."""
    for dy in range(h):
        for dx in range(w - dy):
            c.put(mx + dx, my + dy, r["moss"])
    for dx in range(w - 1):                              # lit crown
        c.put(mx + dx, my - 1, mix(r["moss"], r["hi"], 0.45))
    for dx in range(w - h):                              # contact shadow
        c.put(mx + dx, my + h, r["deep"])


def _aeth_cluster(c, aeth, x, y, tall=6, lean=0):
    """Aetherium spur cluster: a tall faceted shard flanked by two stubs, each
    with a lit left facet and a dark right facet so the crystal reads as a
    hard mineral against the matte stone."""
    for i in range(tall):                                # main shard
        w = 3 if i < tall - 3 else (2 if i < tall - 1 else 1)
        bx = x + round(lean * i / max(tall - 1, 1))
        for k in range(w):
            c.put(bx - w // 2 + k, y - i,
                  aeth["light"] if k == 0 else
                  (aeth["deep"] if k == w - 1 and w > 2 else aeth["base"]))
    for sx, sh in ((-3, 3), (3, 4)):                     # flanking stubs
        for i in range(sh):
            w = 2 if i < sh - 1 else 1
            for k in range(w):
                c.put(x + sx - w // 2 + k, y - i,
                      aeth["light"] if k == 0 else aeth["base"])


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
                _stone(c, lx, feet_y - 9 - lift, 4, 6, r, pits=2,
                       seed=1 if side < 0 else 4)         # overlaps the belly
                _stone(c, lx + side, feet_y - 1 - lift, 4.5, 2.5, r, pits=1,
                       seed=2)                            # foot
                for dx in range(-3, 4):                   # knee plate groove
                    c.put(lx + dx, feet_y - 8 - lift, r["deep"])
                    c.put(lx + dx, feet_y - 7 - lift,
                          r["hi"] if dx < 0 else r["light"])
        # --- torso: chest slab over a belly slab, then hard plate bands ---
        _stone(c, cx, feet_y - 21, 11, 8, r, pits=4, seed=3)            # belly
        _stone(c, cx, feet_y - 30 + stomp, 13, 10, r, pits=5, seed=7)   # chest
        _golem_bands(c, r, cx, ((feet_y - 33 + stomp, 10),
                                (feet_y - 27 + stomp, 12),
                                (feet_y - 21, 10),
                                (feet_y - 16, 7)))
        # --- arms: shoulder cap, forearm, fist boulder (swing vs legs) ---
        for side, s in ((-1, -step), (1, step)):
            sw = round(s * 2)
            ax = cx + side * 15
            _stone(c, cx + side * 12, feet_y - 36 + stomp, 5.5, 4.5, r,
                   pits=2, seed=0 if side < 0 else 3)       # shoulder cap
            _stone(c, ax, feet_y - 29 + sw, 4, 5, r, pits=2, seed=4)  # upper arm
            _stone(c, ax + side, feet_y - 21 + sw, 4.5, 4.5, r, pits=2,
                   seed=1)                                  # fist
            c.put(ax + side - 1, feet_y - 19 + sw, r["deep"])             # knuckles
            c.put(ax + side + 1, feet_y - 19 + sw, r["deep"])
        # --- head: sunk between the shoulders, heavy brow ledge ---
        head_y = feet_y - 39 + stomp
        _stone(c, cx, head_y, 6.5, 5.5, r, pits=2, seed=2)
        for dx in range(-5, 6):                                           # brow ledge
            c.put(cx + dx, head_y - 2, r["deep"])
            if -4 <= dx <= 4:
                c.put(cx + dx, head_y - 3, r["light"])
        # --- cracks: a dark fissure with a lit upper lip (top-left light) ---
        crx, cry = cx - 7, feet_y - 31 + stomp
        for i in range(8):
            cy_ = cry + (i // 2) - (1 if i > 5 else 0)
            c.put(crx + i, cy_, r["deep"])
            if i % 3 != 2:
                c.put(crx + i, cy_ - 1, r["hi"])
        for i, (kx, ky) in enumerate(((4, -24), (5, -23), (5, -22), (6, -21))):
            c.put(cx + kx, feet_y + ky, r["deep"])
            if i % 2 == 0:
                c.put(cx + kx - 1, feet_y + ky, r["light"])
        # --- moss patches on the upper (lit) surfaces of the plates ---
        _moss(c, r, cx - 16, feet_y - 38 + stomp, 5, 3)
        _moss(c, r, cx + 6, feet_y - 26, 6, 3)
        _moss(c, r, cx - 8, feet_y - 17, 5, 2)
        _moss(c, r, cx - 13, feet_y - 7, 4, 2)
        _moss(c, r, cx + 13, feet_y - 34 + stomp, 4, 2)
        # --- aetherium spur cluster on the left shoulder ---
        spx = cx - 12
        spy = feet_y - 39 + stomp
        _aeth_cluster(c, aeth, spx, spy, tall=8, lean=-1)
        c.outline(palette.OUTLINE)
        c.put(spx - 1, spy - 8, aeth["hi"])     # crystal tip glint
        c.put(spx - 4, spy - 3, aeth["hi"])
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
            _stone(c, cx - 8 + front, feet_y - 7, 4.5, 6, r, pits=2,
                   seed=1)                                       # rear leg
            _stone(c, cx - 7 + front, feet_y - 1, 5, 2.5, r, pits=1, seed=2)
            _stone(c, cx - 1 - front, feet_y - 6, 4.5, 6, r, pits=2,
                   seed=4)                                       # front leg
            _stone(c, cx - front, feet_y - 1, 5, 2.5, r, pits=1, seed=0)
        _stone(c, cx - 4, feet_y - 17, 9, 7, r, pits=4, seed=3)   # haunch
        _stone(c, cx + 2, feet_y - 26 + stomp, 12, 9, r, pits=5,
               seed=7)                                           # chest, fwd lean
        _golem_bands(c, r, cx + 2, ((feet_y - 29 + stomp, 8),
                                    (feet_y - 24 + stomp, 10),
                                    (feet_y - 19, 9),
                                    (feet_y - 14, 6)))
        _stone(c, cx - 3, feet_y - 33 + stomp, 5.5, 4.5, r, pits=2,
               seed=0)                                           # shoulder hump
        # leading arm: reaches forward and down to the ground
        sw = round(step * 2)
        _stone(c, cx + 10, feet_y - 22 + sw, 4.5, 5.5, r, pits=2,
               seed=4)                                           # upper arm
        _stone(c, cx + 13, feet_y - 12 + sw, 5, 5, r, pits=2,
               seed=1)                                           # fist near ground
        c.put(cx + 12, feet_y - 9 + sw, r["deep"])               # knuckles
        c.put(cx + 15, feet_y - 9 + sw, r["deep"])
        # head juts forward from the chest
        head_y = feet_y - 33 + stomp
        _stone(c, cx + 9, head_y, 6, 5, r, pits=2, seed=2)
        for dx in range(-3, 6):
            c.put(cx + 9 + dx, head_y - 2, r["deep"])            # brow ledge
            c.put(cx + 9 + dx, head_y - 3, r["light"])
        for dx in range(-2, 3):                                  # neck shadow
            c.put(cx + 6 + dx, head_y + 4, r["deep"])
        # aetherium spur cluster on the rear shoulder + moss patches
        spx, spy = cx - 4, feet_y - 36 + stomp
        _aeth_cluster(c, aeth, spx, spy, tall=7, lean=-1)
        _moss(c, r, cx - 12, feet_y - 28, 6, 3)
        _moss(c, r, cx - 6, feet_y - 12, 5, 2)
        _moss(c, r, cx + 4, feet_y - 31 + stomp, 4, 2)
        _moss(c, r, cx + 9, feet_y - 16 + sw, 4, 2)
        c.outline(palette.OUTLINE)
        c.put(spx - 1, spy - 7, aeth["hi"])
        c.put(spx - 4, spy - 3, aeth["hi"])
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
    # Zephyr Ray: top view, head up — a full-cell manta (vanilla bestiary
    # icons fill their 32px cell; the v0.4 icon sat at 360 opaque px, half a
    # vanilla icon, and read as a smudge in the bestiary list)
    c = _icon_canvas()
    z = palette.ZEPHYR
    cx, cy = 16, 13
    for side in (-1, 1):
        _ray_wing(c, cx, cy, side, 1, 15, 2, 6, 1.1, 0.3, z,
                  tip_chord=2.2, root_lead=5.6, root_trail=6.4)
    c.ellipse(cx + 1, cy + 2, 3.6, 7.4, z["deep"])
    c.ellipse(cx, cy + 1, 3.6, 7.4, z["base"])
    c.ellipse(cx, cy - 5, 2.8, 3.0, z["base"])
    c.ellipse(cx - 2, cy - 8, 1.3, 1.7, z["base"])
    c.ellipse(cx + 2, cy - 8, 1.3, 1.7, z["base"])
    c.ellipse(cx - 1, cy - 1, 2.4, 4.6, z["light"])
    c.ellipse(cx - 1, cy - 5, 1.8, 1.9, z["light"])
    c.put(cx - 1, cy - 3, z["hi"])
    for sx, sy in ((-2, 0), (2, 0), (-2, 4), (2, 4)):     # mantle spots
        c.put(cx + sx, cy + sy, z["deep"])
    for i in range(10):                                   # thick whip tail
        y = cy + 7 + i
        x = cx + round(1.8 * math.sin(i * 0.42 + 0.4))
        w = 4 if i < 2 else (3 if i < 6 else 2)
        for k in range(w):
            xx = x - w // 2 + k
            edge = k == 0 or k == w - 1
            c.put(xx, y, z["deep"] if (edge or i >= 7) else z["base"])
    c.outline(palette.OUTLINE)
    c.put(cx - 2, cy - 9, palette.OUTLINE)
    c.put(cx + 2, cy - 9, palette.OUTLINE)
    c.put(cx - 2, cy - 8, z["accent"])
    c.put(cx + 2, cy - 8, z["accent"])
    for side in (-1, 1):                                  # teal spot rows
        for dx, dy in ((7, 0), (10, 1), (12, 2)):
            c.put(cx + side * dx, cy + dy, z["accent"])
        c.put(cx + side * 13, cy + 1, z["hi"])
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

    # Skystone Golem: head + shoulders built from the SAME faceted plates as
    # the sheet (the v0.4 icon was a flat grey box with two teal squares — no
    # plate separation, no material read at all)
    c = _icon_canvas()
    g = palette.GOLEM
    aeth = palette.AETHERIUM
    _stone(c, 5, 22, 6.5, 6.0, g, pits=2, seed=1)         # left shoulder
    _stone(c, 27, 22, 6.5, 6.0, g, pits=2, seed=4)        # right shoulder
    _stone(c, 16, 25, 9.5, 7.5, g, pits=4, seed=7)        # chest slab
    _golem_bands(c, g, 16, ((23, 8), (28, 6)))
    _stone(c, 16, 12, 8.0, 6.5, g, pits=3, seed=2)        # head plate
    for dx in range(-7, 8):                               # heavy brow ledge
        c.put(16 + dx, 8, g["deep"])
        if -6 <= dx <= 6:
            c.put(16 + dx, 9, g["hi"] if dx < 0 else g["light"])
    for i in range(6):                                    # jaw fissure
        c.put(12 + i, 17 + (i % 2), g["deep"])
        if i % 3 != 2:
            c.put(12 + i, 16 + (i % 2), g["hi"])
    _moss(c, g, 2, 17, 4, 2)
    _moss(c, g, 24, 18, 4, 2)
    _moss(c, g, 13, 29, 4, 2)
    _aeth_cluster(c, aeth, 6, 14, tall=7, lean=-1)        # shoulder spur
    c.outline(palette.OUTLINE)
    c.put(5, 7, aeth["hi"])
    for ex in (11, 19):                                   # glowing eye sockets
        c.rect(ex, 11, 3, 3, g["deep"])
        c.rect(ex, 11, 2, 2, g["eye"])
        c.put(ex, 11, (240, 252, 252))
    c.save(f"{dir_path}/skystonegolem.png")
# --- v0.4 "The Living Sky" fauna ---------------------------------------------
# Galehound (Driftlands night pack hunter) + Dawnpiercer (Aurora Shoals dive
# bird). Standard 6x4/64px walking-mob sheets; the swim column sinks into the
# mist via _mist_overlay. Quadruped construction matched against the vanilla
# wolf/boar sheets: chunky bean body, short 3px legs re-posed per stride, big
# head, ears and tail carrying the silhouette.


def _hound_leg(c, r, hip_x, hip_y, ground, foot_dx, far=False, lift=0):
    """One CHUNKY canine leg (vanilla wolf/boar bar, v0.5): a 6px thigh that
    leans out of the hip, a 5px shank and a 6px paw with a toe nub. The near
    pair carries a lit leading column and a shaded trailing column; far-side
    legs are drawn a full shade darker so the near pair reads in front.
    After the outline pass a 6px limb still shows 4px of readable interior —
    the old 3px sticks collapsed to 1px and read as wire."""
    tone = r["deep"] if far else r["base"]
    foot_x = hip_x + foot_dx
    foot_y = ground - lift
    knee_y = hip_y + max(2, (foot_y - hip_y) * 52 // 100)
    knee_x = hip_x + foot_dx // 3

    def column(x, y, w):
        for k in range(w):
            c.put(x - w // 2 + k, y, tone)
        if not far:
            c.put(x - w // 2, y, r["light"])
            c.put(x - w // 2 + w - 1, y, r["deep"])

    for y in range(hip_y, knee_y + 1):
        t = (y - hip_y) / max(knee_y - hip_y, 1)
        column(round(hip_x + (knee_x - hip_x) * t), y, 7 if t < 0.62 else 6)
    ank_y = foot_y - 2
    for y in range(knee_y, ank_y + 1):
        t = (y - knee_y) / max(ank_y - knee_y, 1)
        column(round(knee_x + (foot_x - knee_x) * t), y, 6)
    for dy in (1, 0):                                    # paw block, 7 wide
        for k in range(7):
            c.put(foot_x - 3 + k, foot_y - dy, tone)
    if not far:
        for k in range(7):
            c.put(foot_x - 3 + k, foot_y, r["light"])
        c.put(foot_x - 3, foot_y - 1, r["light"])
    c.put(foot_x + (4 if foot_dx >= 0 else -4), foot_y, tone)   # toe nub
    c.put(foot_x + (4 if foot_dx >= 0 else -4), foot_y - 1, r["deep"])


def _hound_plume(c, r, chain, barbs=True):
    """Wind-streamed plume tail: fat overlapping lobes shrinking toward the
    tip, each with a deep under-crescent and (every other lobe) a lit cap, plus
    2px fur barbs flicking off the upper edge so it reads as fur, not a smear."""
    for i, (px, py, rad) in enumerate(chain):
        c.ellipse(px + 1, py + 1, rad, rad * 0.92, r["deep"])
        c.ellipse(px, py, rad, rad * 0.92, r["base"])
        if i % 2 == 1:
            c.ellipse(px - rad * 0.30, py - rad * 0.38, rad * 0.58, rad * 0.52,
                      r["light"])
        if barbs and i < len(chain) - 1 and rad >= 2.5:
            bx = px - round(rad * 0.5)
            by = py - round(rad * 0.85)
            c.put(bx, by, r["light"])
            c.put(bx - 1, by - 1, r["base"])


def _hound_rim(c, r, y0, y1, width=2):
    """Contour rim-light pass for the head-on views: every horizontal run of
    body gets a lit left edge and a shaded right edge, so the volume follows
    the silhouette instead of sitting in flat elliptical patches (which read
    as coat panels at 1x). Run it before the legs and the outline."""
    for y in range(y0, y1 + 1):
        x = 0
        while x < CELL:
            if not c.filled(x, y):
                x += 1
                continue
            x0 = x
            while x < CELL and c.filled(x, y):
                x += 1
            x1 = x - 1
            if x1 - x0 < 5:
                continue
            for k in range(width):
                c.put(x0 + k, y, r["light"])
                c.put(x1 - k, y, r["deep"])


def _hound_frame(facing, col, swim=False):
    """Galehound v0.5: a heavy wind-wolf rebuilt to vanilla quadruped mass.

    The v0.4 build measured ~14% cell occupancy against a vanilla boar's ~26%
    and read as a wire-legged whippet at 1x. This build follows the boar /
    snowwolf construction instead: a deep three-mass barrel (haunch, ribcage,
    chest) ~17px front-to-back, a heavy skull carried clear of the back line
    on a thick neck, 6px legs with real paws, and a fat plume tail that arcs
    up away from the rump so it separates in silhouette. Wind identity lives
    in the swept mane ruff, the drift plume and the teal eye glow."""
    step = {0: 0, 1: 1, 2: 0, 3: -1, 4: 0, 5: 0}[col]
    c = Canvas(CELL, CELL)
    r = palette.GALEHOUND
    ground = 52
    bob = 1 if step != 0 else 0
    sway = col * 1.1                                          # tail phase

    def lift_for(pair):
        """Passing-pose paw lift on the in-between columns (0, 2, 4)."""
        if col == 2:
            return 1 if pair == 0 else 0
        if col == 4:
            return 1 if pair == 1 else 0
        return 0

    if facing == "right":
        cx = 27
        g0 = ground - bob
        hip = g0 - 12                                         # top of the legs

        # --- far-side legs first, behind the whole body -------------------
        if not swim:
            _hound_leg(c, r, cx - 11, hip - 2, ground, 4 * step, far=True,
                       lift=lift_for(0))
            _hound_leg(c, r, cx + 6, hip - 2, ground, -4 * step, far=True,
                       lift=lift_for(1))

        # --- drift-plume tail: a FAT comma sweeping up and back off the
        # rump, so it clears the back line and reads as fur, not a spike ---
        chain = []
        for i, rad in enumerate((6.2, 5.9, 5.0, 3.9, 2.8)):
            px = cx - 11 - round(i * 2.4)
            py = g0 - 25 - round(i * 3.1 + 1.1 * math.sin(i * 0.85 + sway))
            chain.append((px, py, rad))
        _hound_plume(c, r, chain)

        # --- body: three deep overlapping masses (haunch / ribcage / chest) -
        c.ellipse(cx - 9, g0 - 20, 9.5, 9.0, r["base"])       # haunch
        c.ellipse(cx + 0, g0 - 20, 10.0, 8.5, r["base"])      # ribcage
        c.ellipse(cx + 9, g0 - 21, 9.5, 9.0, r["base"])       # chest
        c.ellipse(cx - 9, g0 - 14, 6.5, 3.4, r["deep"])       # haunch underside
        c.ellipse(cx + 1, g0 - 13, 8.0, 2.8, r["deep"])       # belly shade
        c.ellipse(cx + 9, g0 - 14, 6.0, 3.0, r["deep"])
        c.ellipse(cx - 7, g0 - 25, 7.5, 2.4, r["light"])      # lit rear back
        c.ellipse(cx + 7, g0 - 27, 4.5, 2.2, r["light"])      # lit withers
        for hx in (cx + 6, cx + 7):                           # dashed sheen
            c.put(hx, g0 - 27, r["hi"])
        # haunch swirl + flank fur streaks (vanilla micro-detail density)
        for i, (fx, fy) in enumerate(((-13, -18), (-11, -22), (-6, -15),
                                      (-1, -18), (3, -14), (-9, -12),
                                      (5, -19), (-4, -22))):
            c.put(cx + fx, g0 + fy, r["deep"])
            c.put(cx + fx + 1, g0 + fy + (i % 2), r["deep"])

        # --- far ear first so the skull buries its base -------------------
        for i in range(6):
            w = 2 + i // 2
            for k in range(w):
                c.put(cx + 12 - i // 2 + k, g0 - 38 + i, r["deep"])
        # --- thick neck + heavy skull carried clear of the back -----------
        c.ellipse(cx + 15, g0 - 25, 7.0, 7.0, r["base"])      # neck
        c.ellipse(cx + 20, g0 - 31, 7.5, 6.5, r["base"])      # skull
        c.ellipse(cx + 19, g0 - 27, 6.0, 5.0, r["base"])      # jaw / cheek
        c.ellipse(cx + 18, g0 - 34, 4.4, 2.4, r["light"])     # crown light
        c.ellipse(cx + 16, g0 - 23, 3.4, 2.4, r["deep"])      # throat shade
        c.put(cx + 16, g0 - 35, r["hi"])
        c.put(cx + 17, g0 - 35, r["hi"])
        # muzzle: SHORT broad wedge (a long thin snout read bird-like at 1x)
        for i, x in enumerate(range(cx + 25, cx + 31)):
            top = g0 - 32 + (i + 1) // 2
            c.put(x, top, r["light"])
            for k in range(1, 6 - (i * 2) // 3):
                c.put(x, top + k, r["base"])
        for x in range(cx + 24, cx + 30):                     # mouth line
            c.put(x, g0 - 28, r["deep"])
        c.put(cx + 24, g0 - 27, r["deep"])
        # near ear: chunky swept-back triangle whose base sinks 4px into the
        # skull mass, so it never reads as a detached flag at 1x
        for i in range(8):                                    # i=0 tip
            w = 2 + (i + 1) // 2
            x0 = cx + 17 - (7 - i) // 2
            for k in range(w):
                c.put(x0 + k, g0 - 40 + i,
                      r["light"] if k >= w - 2 and i > 1 else r["base"])
        for i in range(3):                                    # inner-ear shadow
            c.put(cx + 15 + i // 2, g0 - 36 + i, r["deep"])
        # mane RUFF: tufts swept off the throat and shoulder, kept below the
        # nape so the head stays a separate lump in silhouette
        for tx0, ty0, ln in ((cx + 11, g0 - 26, 5), (cx + 8, g0 - 22, 5)):
            for i in range(ln):
                c.put(tx0 - i, ty0 + i // 2, r["light"] if i < 2 else r["base"])
                c.put(tx0 - i, ty0 + 1 + i // 2,
                      r["base"] if i < ln - 1 else r["deep"])

        # --- near legs on top, then the chest ruff ------------------------
        if not swim:
            _hound_leg(c, r, cx - 5, hip, ground, -4 * step, lift=lift_for(1))
            _hound_leg(c, r, cx + 12, hip, ground, 4 * step, lift=lift_for(0))
        for i in range(5):                                    # chest fur strokes
            c.put(cx + 16 - i // 2, g0 - 22 + i, r["light"])
            c.put(cx + 17 - i // 2, g0 - 21 + i, r["hi"] if i == 1 else r["light"])

        c.outline(palette.OUTLINE)
        # --- face + glow AFTER the outline --------------------------------
        c.put(cx + 20, g0 - 34, palette.OUTLINE)              # angry brow
        c.put(cx + 21, g0 - 34, palette.OUTLINE)
        c.put(cx + 22, g0 - 33, palette.OUTLINE)
        c.rect(cx + 20, g0 - 33, 2, 2, r["eye"])
        c.put(cx + 20, g0 - 33, r["hi"])
        c.put(cx + 22, g0 - 32, with_alpha(r["eye"], 110))
        c.put(cx + 30, g0 - 31, palette.OUTLINE)              # nostril
        for i in (1, 3):                                      # plume sheen
            px, py, rad = chain[i]
            c.put(px, py - round(rad * 0.85), r["hi"])
        tx, ty, _ = chain[-1]
        c.put(tx - 3, ty - 2, with_alpha(r["hi"], 150))
        c.put(tx - 5, ty, with_alpha(r["hi"], 110))
        # cyan wind flecks off the mane and the tail tip
        c.put(cx + 7, g0 - 30, r["wind"])
        c.put(cx + 1, g0 - 23, with_alpha(r["wind"], 170))
        c.put(tx - 2, ty - 4, r["wind"])

    elif facing == "up":
        cx = 32
        g0 = ground - bob
        # forepaws peeking past the shoulders (rounded, mostly buried by the
        # shoulder mass — square stubs read as backpack handles at 1x)
        if not swim:
            for side, s_ in ((-1, step), (1, -step)):
                c.ellipse(cx + side * 12, g0 - 24 + (1 if s_ > 0 else 0),
                          2.6, 3.2, r["base"])
        # back mass: broad rump, soft waist, broad shoulders, heavy skull
        c.ellipse(cx, g0 - 13, 11.0, 8.5, r["base"])          # rump
        c.ellipse(cx, g0 - 22, 9.5, 6.5, r["base"])           # waist
        c.ellipse(cx, g0 - 29, 11.0, 6.0, r["base"])          # shoulders
        c.ellipse(cx, g0 - 34, 5.5, 4.0, r["base"])           # nape notch
        c.ellipse(cx, g0 - 38, 7.0, 5.5, r["base"])           # skull
        # BIG pointed ears splaying up and out (matched to the snowwolf back
        # view — short nubs read as horns and killed the canine silhouette)
        for side in (-1, 1):
            for i in range(9):                                # i=0 = tip
                w = 2 + i // 2
                ex = cx + side * (8 - i // 3)
                for k in range(w):
                    c.put(ex - side * (w - 1) + side * k, g0 - 46 + i, r["base"])
        # contour rim: lit left edge, shaded right edge, all the way down
        _hound_rim(c, r, g0 - 46, g0 - 6)
        for side in (-1, 1):                                  # inner-ear bowls
            for i in range(3, 8):
                w = 2 + i // 2
                ex = cx + side * (8 - i // 3)
                for k in range(1, w - 1):
                    c.put(ex - side * (w - 1) + side * k, g0 - 46 + i, r["deep"])
        c.put(cx - 3, g0 - 40, r["hi"])                       # crown glint
        c.put(cx - 2, g0 - 40, r["hi"])
        c.put(cx - 9, g0 - 30, r["hi"])
        c.put(cx - 8, g0 - 15, r["hi"])
        for dx in range(-5, 6):                               # nape shadow
            c.put(cx + dx, g0 - 34, r["deep"])
        # dark mane saddle: a SOLID wedge off the shoulders tapering to the
        # waist, then a dashed spine groove — the marking that makes a back
        # view read as a canine rather than a smooth grey capsule
        for i in range(10):
            half = 7 - (i * 6) // 10
            for dx in range(-half, half + 1):
                c.put(cx + dx, g0 - 32 + i, r["deep"])
            if i < 7:
                c.put(cx - half, g0 - 32 + i, r["base"])
        for i in range(4):                                    # saddle sheen
            c.put(cx - 4 + i, g0 - 31, r["base"])
        for y in range(g0 - 22, g0 - 12):
            if (y % 3) != 2:
                c.put(cx, y, r["deep"])
        for i, (fx, fy) in enumerate(((-7, -19), (6, -18), (-6, -13),
                                      (5, -11), (-4, -9), (4, -22))):
            c.put(cx + fx, g0 + fy, r["deep"])
            c.put(cx + fx + 1, g0 + fy + (i % 2), r["deep"])
        # shoulder ruff tufts pointing down and out (wind identity)
        for side in (-1, 1):
            sx = cx + side * 11
            for i in range(4):
                c.put(sx + side * (i // 2), g0 - 28 + i,
                      r["light"] if i == 0 else r["base"])
                c.put(sx + side * (1 + i // 2), g0 - 27 + i,
                      r["deep"] if i >= 2 else r["base"])
        # fat plume tail draped down the rump between the hind legs — drawn
        # ON TOP of the rump (a tail tucked under it is simply invisible)
        chain = []
        for i, rad in enumerate((3.4, 3.8, 3.2, 2.3)):
            px = cx + 1 + round(1.4 * math.sin(i * 0.9 + sway))
            py = g0 - 15 + round(i * 4.0)
            chain.append((px, py, rad))
        _hound_plume(c, r, chain, barbs=False)
        # hind legs stride vertically
        if not swim:
            for side, s_, pair in ((-1, step, 0), (1, -step, 1)):
                _hound_leg(c, r, cx + side * 8, g0 - 14, ground, 0,
                           lift=(2 if s_ > 0 else lift_for(pair)))
        c.outline(palette.OUTLINE)
        for i in (1, 3):
            px, py, rad = chain[i]
            c.put(px - round(rad * 0.5), py, r["hi"])
        tx, ty, _ = chain[-1]
        c.put(tx + 2, ty + 2, with_alpha(r["hi"], 150))
        c.put(cx - 9, g0 - 36, r["wind"])
        c.put(cx + 9, g0 - 33, with_alpha(r["wind"], 160))

    else:  # down — front view, head-on
        cx = 32
        g0 = ground - bob
        # plume tail tip peeking past the right haunch (behind everything)
        chain = []
        for i, rad in enumerate((3.0, 3.0, 2.4, 1.7)):
            px = cx + 13 + round(i * 1.0 + 0.9 * math.sin(i * 0.9 + sway))
            py = g0 - 16 - round(i * 2.7)
            chain.append((px, py, rad))
        _hound_plume(c, r, chain, barbs=False)
        # rear paws: short stubs tucked under the haunch bulges (drawn first
        # so the haunch mass overlaps them and they stay attached)
        if not swim:
            for side in (-1, 1):
                for k in range(5):
                    c.put(cx + side * 10 - 2 + k, ground - 4, r["deep"])
                    c.put(cx + side * 10 - 2 + k, ground - 3, r["base"])
                    c.put(cx + side * 10 - 2 + k, ground - 2, r["base"])
                    c.put(cx + side * 10 - 2 + k, ground - 1, r["base"])
                    c.put(cx + side * 10 - 2 + k, ground, r["base"])
        # body: deep chest with haunch bulges at the sides
        c.ellipse(cx, g0 - 14, 9.5, 8.0, r["base"])
        c.ellipse(cx - 9, g0 - 11, 5.5, 5.5, r["base"])
        c.ellipse(cx + 9, g0 - 11, 5.5, 5.5, r["base"])
        c.ellipse(cx - 9, g0 - 8, 4.2, 2.8, r["deep"])
        c.ellipse(cx + 9, g0 - 8, 4.2, 2.8, r["deep"])
        c.ellipse(cx + 6, g0 - 15, 3.4, 5.0, r["deep"])       # right-flank shade
        # chest bib: a broad lit ruff that tapers into the belly, with the
        # dither confined to its lower ramp border (never used as texture)
        for i in range(9):
            w = 9 - i
            for k in range(w):
                c.put(cx - w // 2 + k, g0 - 21 + i, r["light"])
        c.put(cx - 2, g0 - 20, r["hi"])
        c.put(cx - 1, g0 - 19, r["hi"])
        for i in range(5):                                    # ruff fur strokes
            c.put(cx - 3 + i, g0 - 18 + (i % 2), r["base"])
        for x in range(cx - 4, cx + 4):
            if (x + g0) % 2 == 0:
                c.put(x, g0 - 13, r["base"])
        # front legs
        if not swim:
            for side, s, pair in ((-1, step, 0), (1, -step, 1)):
                _hound_leg(c, r, cx + side * 5, g0 - 10, ground, 0,
                           lift=(1 if s > 0 else lift_for(pair)))
        # big head over the chest
        c.ellipse(cx, g0 - 29, 9.5, 8.0, r["base"])
        c.ellipse(cx - 3, g0 - 34, 5.2, 3.2, r["light"])      # crown
        c.ellipse(cx - 6, g0 - 24, 2.6, 2.8, r["deep"])       # cheek shade
        c.ellipse(cx + 6, g0 - 24, 2.6, 2.8, r["deep"])
        c.put(cx - 5, g0 - 36, r["hi"])
        c.put(cx - 4, g0 - 36, r["hi"])
        # wide-based ears with a dark inner bowl
        for side in (-1, 1):
            for i in range(6):                                # i=0 tip
                w = 3 + (i + 1) // 2
                x0 = cx + side * (7 + (1 if i < 2 else 0)) - w // 2
                for k in range(w):
                    c.put(x0 + k, g0 - 42 + i, r["base"])
            for i in range(2, 6):
                w = 3 + (i + 1) // 2
                x0 = cx + side * 7 - w // 2
                for k in range(1, w - 1):
                    c.put(x0 + k, g0 - 42 + i, r["deep"])
        # brow blaze + broad muzzle
        c.rect(cx - 1, g0 - 33, 3, 5, r["light"])
        c.ellipse(cx, g0 - 25, 4.6, 3.4, r["light"])
        c.put(cx - 2, g0 - 26, r["hi"])
        c.put(cx - 1, g0 - 26, r["hi"])
        # cheek wind-tufts pointing out
        for side in (-1, 1):
            for i in range(3):
                c.put(cx + side * (10 + i // 2), g0 - 29 + i, r["base"])
                c.put(cx + side * (10 + i // 2), g0 - 28 + i, r["deep"])
        c.outline(palette.OUTLINE)
        # face after the outline: angry brow, glowing eyes, nose, mouth
        for side in (-1, 1):
            c.put(cx + side * 2, g0 - 33, palette.OUTLINE)
            c.put(cx + side * 3, g0 - 33, palette.OUTLINE)
            c.put(cx + side * 5, g0 - 34, palette.OUTLINE)
            c.put(cx + side * 4, g0 - 33, palette.OUTLINE)
        for ex in (cx - 5, cx + 3):
            c.rect(ex, g0 - 32, 3, 2, r["eye"])
            c.put(ex, g0 - 32, r["hi"])
        c.put(cx - 7, g0 - 32, with_alpha(r["eye"], 110))
        c.put(cx + 6, g0 - 32, with_alpha(r["eye"], 110))
        c.rect(cx - 1, g0 - 27, 3, 2, palette.OUTLINE)        # nose
        for dx in range(-2, 3):                               # mouth
            c.put(cx + dx, g0 - 23, r["deep"])
        c.put(cx - 3, g0 - 22, r["deep"])
        c.put(cx + 3, g0 - 22, r["deep"])
        # drift flecks off the tail tip
        tx, ty, _ = chain[-1]
        c.put(tx + 2, ty - 3, with_alpha(r["hi"], 140))
        c.put(tx + 3, ty - 4, with_alpha(r["wind"], 190))
        c.put(cx - 10, g0 - 35, r["wind"])
        for i in (1, 3):
            px, py, rad = chain[i]
            c.put(px, py - round(rad * 0.75), r["hi"])
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
    # Galehound: head/shoulder crop matched to the v0.5 sheet — heavy skull,
    # wide-based ears with a dark inner bowl, a lit ruff instead of a checker
    # field, and the cyan wind flecks that carry its identity
    c = _icon_canvas()
    r = palette.GALEHOUND
    c.ellipse(16, 27, 13.0, 7.0, r["base"])       # shoulder ruff
    c.ellipse(16, 30, 9.5, 4.0, r["deep"])
    for side in (-1, 1):                          # pointed ears, dark inner bowl
        for i in range(8):                        # i=0 tip
            w = 3 + i // 2                        # >=3 so the outline pass
            x0 = 16 + side * (9 - i // 3) - w // 2  # leaves a readable core
            for k in range(w):
                c.put(x0 + k, i, r["base"])
        for i in range(3, 8):
            w = 3 + i // 2
            x0 = 16 + side * (9 - i // 3) - w // 2
            for k in range(1, w - 1):
                c.put(x0 + k, i, r["deep"])
    c.ellipse(16, 15, 10.5, 9.0, r["base"])       # head
    c.ellipse(13, 10, 5.5, 3.4, r["light"])       # crown
    c.ellipse(9, 19, 2.8, 3.0, r["deep"])         # cheek shade
    c.ellipse(23, 19, 2.8, 3.0, r["deep"])
    c.put(11, 6, r["hi"])
    c.put(12, 6, r["hi"])
    for side in (-1, 1):                          # cheek wind-tufts
        for i in range(3):
            c.put(16 + side * (11 + i // 2), 15 + i, r["base"])
            c.put(16 + side * (11 + i // 2), 16 + i, r["deep"])
    c.rect(15, 10, 3, 6, r["light"])              # brow blaze
    c.ellipse(16, 20, 5.0, 3.6, r["light"])       # muzzle
    c.put(13, 19, r["hi"])
    for i in range(9):                            # lit chest ruff, broad bib
        w = 13 - i
        for k in range(w):
            c.put(16 - w // 2 + k, 23 + i, r["light"])
    for i in range(7):                            # ruff fur strokes
        c.put(12 + i, 25 + (i % 2), r["base"])
        c.put(12 + i, 28 + ((i + 1) % 2), r["base"])
    for x in range(12, 20):                       # dither only at the border
        if (x + 31) % 2 == 0:
            c.put(x, 31, r["base"])
    c.outline(palette.OUTLINE)
    for side in (-1, 1):                          # angry brow
        c.put(16 + side * 2, 11, palette.OUTLINE)
        c.put(16 + side * 3, 11, palette.OUTLINE)
        c.put(16 + side * 4, 11, palette.OUTLINE)
        c.put(16 + side * 6, 10, palette.OUTLINE)
    for ex in (10, 19):                           # glowing eyes
        c.rect(ex, 12, 3, 2, r["eye"])
        c.put(ex, 12, r["hi"])
    c.put(8, 12, with_alpha(r["eye"], 110))
    c.put(22, 12, with_alpha(r["eye"], 110))
    c.rect(15, 18, 3, 2, palette.OUTLINE)         # nose
    for dx in range(-2, 3):                       # mouth
        c.put(16 + dx, 22, r["deep"])
    c.put(13, 23, r["deep"])
    c.put(19, 23, r["deep"])
    c.put(4, 8, r["wind"])                        # cyan wind flecks
    c.put(27, 6, with_alpha(r["wind"], 180))
    c.put(29, 13, with_alpha(r["wind"], 140))
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
