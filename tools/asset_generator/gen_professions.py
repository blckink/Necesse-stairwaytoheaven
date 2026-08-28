"""Skywatch professions: three settlement workstations, four spire furniture
pieces, and the materials the stations make.

Everything here shares the Skywatch furniture family's material identity, so
the drawing helpers come straight out of `gen_skyfurniture` rather than being
copied: a second `_slab` would drift from the first the moment either is
tuned, and the whole point of the family is that a loom, a bookshelf and a
chair are visibly the same masonry.

Sheet formats, each measured off the vanilla sprite it answers to and checked
against the decompiled renderer that reads it:

| file | size | renderer | anchor |
|---|---|---|---|
| `windsilkloom` | 128x64 | `CraftingStationObject` (as `alchemytable`) | `drawY - h + 32` |
| `aetherforge` | 128x96 | `ProcessingForgeObject` (as `forge`) | body `drawY - 32`, fire strip `drawY` |
| `stormglasskiln` (+`_on`) | 128x64 | processing station (as `cheesepress`) | `drawY - (h - 32)` |
| `skywatchbookshelf` | 128x128 | `BookshelfObject` (as `oakbookshelf`) | `drawY - h + 64` |
| `skywatchcabinet` | 128x128 | `CabinetObject` (as `oakcabinet`) | `drawY - h + 64` |
| `skywatchclock` | 128x64 | `ClockObject` (as `oakclock`) | `drawY - h + 32` |
| `skywatchdisplay` | 128x32 | `DisplayStandObject` (as `oakdisplay`) | `drawY` |

Two of those anchors matter for where the art sits inside its cell, and the
row ranges below are not free choices — they are vanilla's, measured column by
column:

- Bookshelf and cabinet are anchored 64px above the tile, and vanilla shifts
  the piece VERTICALLY per rotation, because a case whose back is against the
  north wall stands higher on screen than the same case turned around.
  `oakbookshelf`: back rows 36..99, front 16..77, sides 18..99 in a 12px-wide
  slab hugging the wall edge. `oakcabinet`: 34..95 / 12..73 / 20..95 in a 16px
  slab. Drawing all four columns bottom-aligned instead would make a bookshelf
  jump a tile when the player rotated it.
- The forge's fire strip is addressed as `sprite(frame, height/32, 32)` and
  drawn at `drawY`, while the body is drawn at `drawY - 32`. So the mouth in
  the body's front column has to sit at sheet rows 36..47 for the fire to land
  inside it, and the fire frames occupy rows 4..15 of their own 32px cell.

Rotation order is the engine's `dir()` order — 0 up, 1 right, 2 down, 3 left —
so column 0 is the piece seen from behind, column 2 is its front, and columns
1 and 3 are the two side views (3 is 1 mirrored).

Run standalone:  python3 gen_professions.py <objects_dir> <items_dir>
"""

import os
import sys

from px import Canvas, Rng, mix, with_alpha
import palette

# The Skywatch family's own drawing vocabulary. Imported rather than copied so
# that a change to the family's stone or ironwork reaches these pieces too.
from gen_skyfurniture import (
    STONE, IRON, SILK, GLOW, GLOW_HI, OUT, PATINA, PATINA_HI,
    _grain, _slab, _panel, _iron_post, _iron_rail, _under_mass,
    _crescent, _paste_col,
)

# --- materials this file adds to the family ------------------------------

WOOD = palette.CLOUDWOOD            # cloudwood: loom frame, shelf boards
AETHER = palette.AETHERIUM          # the forge's teal melt
FULG = palette.FULGURITE            # fulgurite amber: the kiln's glass
CRYSTAL = palette.STORMCRYSTAL      # storm shard violet: stormsteel's temper

# Stormsteel is aetherium quenched in storm shard: an iron ladder pulled
# toward the crystal's violet. Derived here rather than added to palette.py so
# this stream owns its own colours.
STORMSTEEL = {
    "deep": mix(IRON["deep"], CRYSTAL["deep"], 0.55),
    "base": mix(IRON["base"], CRYSTAL["base"], 0.50),
    "light": mix(IRON["light"], CRYSTAL["light"], 0.45),
    "hi": mix(IRON["hi"], CRYSTAL["hi"], 0.40),
}

# Stormglass: a pale amber pane, lighter and greener than raw fulgurite so a
# glazed clock face reads as glass rather than as a slab of stone.
GLASS = {
    "deep": mix(FULG["deep"], STONE["deep"], 0.45),
    "base": mix(FULG["base"], STONE["light"], 0.35),
    "light": mix(FULG["light"], (255, 255, 255), 0.25),
    "hi": (255, 250, 226),
}

# Book spines in the archive. Five accents, none of them new: they are the
# colours the rest of the mod already uses, so a shelf reads as the Skywatch's
# own library rather than as a rainbow.
SPINES = (
    mix(palette.AURORA["base"], OUT, 0.25),
    mix(CRYSTAL["base"], OUT, 0.25),
    mix(AETHER["deep"], OUT, 0.15),
    mix(palette.CLOUDBERRY["berry_deep"], OUT, 0.2),
    mix(palette.PRISMSHARD["base"], OUT, 0.3),
)


# =======================================================================
# shared small parts
# =======================================================================


def _wood_board(c, x, y, w, h, rng, grain=True):
    """A cloudwood board: lit top edge, dark bottom, sparse strand grain."""
    c.rect(x, y, w, h, WOOD["light"])
    c.rect(x, y, w, 1, WOOD["hi"])
    c.rect(x, y + h - 1, w, 1, WOOD["deep"])
    c.rect(x, y, 1, h, WOOD["hi"])
    c.rect(x + w - 1, y, 1, h, WOOD["base"])
    if grain and w > 3 and h > 2:
        for _ in range(max(1, (w * h) // 26)):
            gx = x + rng.range(1, w - 2)
            gy = y + rng.range(1, h - 2)
            c.put(gx, gy, WOOD["base"] if rng.chance(0.6) else WOOD["glint"])


def _recess(c, x, y, w, h):
    """The dark inside of a shelf bay, a hearth mouth or a warp bed. Drawn as
    a solid mass FIRST so the outline pass never reaches whatever thin thing
    is laid on top of it (books, warp threads, flames)."""
    c.rect(x, y, w, h, mix(OUT, STONE["deep"], 0.35))
    c.rect(x, y, w, 1, mix(OUT, STONE["deep"], 0.15))
    c.rect(x, y, 1, h, mix(OUT, STONE["deep"], 0.15))


def _books(c, x, y, w, h, rng):
    """A row of shelved tomes. Spines are 2-3px wide with varied heights and
    a lit top edge — the micro-detail that keeps a shelf from reading flat."""
    cx = x
    while cx < x + w - 1:
        sw = rng.range(2, 3)
        if cx + sw > x + w:
            sw = x + w - cx
        lean = rng.chance(0.14)
        top = y + rng.range(0, 3) + (2 if lean else 0)
        col = SPINES[rng.next() % len(SPINES)]
        c.rect(cx, top, sw, y + h - top, col)
        c.rect(cx, top, sw, 1, mix(col, SILK["light"], 0.45))
        c.rect(cx, top, 1, y + h - top, mix(col, SILK["base"], 0.3))
        if sw > 2 and rng.chance(0.5):
            c.put(cx + 1, top + 3, mix(col, SILK["hi"], 0.55))   # title band
        cx += sw + (1 if rng.chance(0.35) else 0)


def _glass_pane(c, x, y, w, h, rng):
    """A leaning stormglass pane: amber body, one bright edge, one glint."""
    c.rect(x, y, w, h, GLASS["base"])
    c.rect(x, y, 1, h, GLASS["light"])
    c.rect(x + w - 1, y, 1, h, GLASS["deep"])
    c.rect(x, y, w, 1, GLASS["light"])
    c.rect(x, y + h - 1, w, 1, GLASS["deep"])
    if w > 2 and h > 4:
        c.put(x + 1, y + 2, GLASS["hi"])
        c.put(x + 1, y + 3, GLASS["hi"])
        _grain(c, x + 1, y + 1, w - 2, h - 2, rng,
               hi=GLASS["light"], lo=GLASS["deep"], density=30)


def _teal_fire(c, cx, bottom, height, rng, phase):
    """Aetherium burning: a dark teal teardrop mass with a hot core, built
    mass-first so the outline pass cannot eat the tip."""
    top = bottom - height
    for y in range(top, bottom + 1):
        t = (y - top) / max(1, height)
        w = 1 + int(round(3.0 * (t ** 0.55)))
        wobble = 1 if (y + phase) % 5 == 0 else 0
        for dx in range(-w, w + 1):
            c.put(cx + dx + wobble, y, AETHER["deep"])
    for y in range(top + 1, bottom):
        t = (y - top) / max(1, height)
        w = int(round(2.1 * (t ** 0.55)))
        for dx in range(-w, w + 1):
            c.put(cx + dx, y, AETHER["base"])
    for y in range(top + 2, bottom):
        c.put(cx, y, AETHER["light"])
    c.put(cx, bottom - 1, AETHER["hi"])
    c.put(cx + (1 if phase % 2 else -1), top, with_alpha(AETHER["light"], 190))


def _ember_bed(c, x, y, w, h, rng, phase):
    """Coals under the fire: a broken band of hot pixels on the hearth floor."""
    for i in range(x, x + w):
        if (i + phase) % 3 == 0:
            c.rect(i, y, 1, h, AETHER["base"])
        elif (i + phase) % 3 == 1:
            c.rect(i, y, 1, h, AETHER["deep"])
        else:
            c.rect(i, y, 1, max(1, h - 1), mix(AETHER["deep"], OUT, 0.4))


# =======================================================================
# 1. Skywatch bookshelf - 128x128, BookshelfObject
# =======================================================================
#
# Row ranges are oakbookshelf's, measured column by column: front 16..77,
# back 36..99, sides 18..99 in a 12px slab. See the module docstring.


def _shelf_front(rng):
    c = Canvas(32, 128)
    _slab(c, 2, 16, 28, 2, rng, grain=False)                  # crown lip
    _slab(c, 0, 18, 32, 10, rng)                              # cornice
    c.rect(0, 26, 32, 2, PATINA)
    _slab(c, 0, 28, 32, 4, rng)                               # frieze
    for top in (32, 48):                                      # two open bays
        _recess(c, 4, top, 24, 14)
        _books(c, 5, top + 1, 22, 13, rng)
        _wood_board(c, 2, top + 14, 28, 2, rng)
    _slab(c, 0, 32, 4, 32, rng)                               # left stile
    _slab(c, 28, 32, 4, 32, rng)                              # right stile
    c.rect(0, 64, 32, 2, mix(OUT, STONE["deep"], 0.3))        # under-shelf line
    _slab(c, 0, 66, 32, 8, rng)                               # base cupboard
    _panel(c, 3, 67, 11, 6, rng)
    _panel(c, 18, 67, 11, 6, rng)
    _iron_rail(c, 14, 66, 4, 8)
    c.put(13, 69, PATINA_HI)
    c.put(18, 69, PATINA_HI)
    _slab(c, 0, 74, 32, 4, rng, grain=False)                  # plinth
    c.rect(0, 76, 32, 2, STONE["deep"])
    return c


def _shelf_back(rng):
    c = Canvas(32, 128)
    _slab(c, 2, 36, 28, 2, rng, grain=False)
    _slab(c, 0, 38, 32, 10, rng)
    c.rect(0, 46, 32, 2, PATINA)
    _slab(c, 0, 48, 32, 46, rng)
    _panel(c, 3, 51, 26, 18, rng)
    _panel(c, 3, 72, 26, 18, rng)
    _iron_rail(c, 0, 69, 32, 2)
    _slab(c, 0, 94, 32, 4, rng, grain=False)
    c.rect(0, 96, 32, 4, STONE["deep"])
    return c


def _shelf_side(rng):
    c = Canvas(32, 128)
    _slab(c, 2, 18, 8, 2, rng, grain=False)                   # crown overhang
    _slab(c, 0, 20, 12, 8, rng)
    c.rect(0, 26, 12, 2, PATINA)
    _slab(c, 0, 28, 12, 66, rng)
    _panel(c, 2, 32, 8, 28, rng)
    _panel(c, 2, 64, 8, 26, rng)
    for y in (34, 40, 50, 56):                                # book edges
        col = SPINES[rng.next() % len(SPINES)]
        c.rect(3, y, 6, 2, col)
        c.rect(3, y, 6, 1, mix(col, SILK["light"], 0.4))
    _iron_rail(c, 0, 62, 12, 2)
    c.rect(0, 94, 12, 6, STONE["deep"])
    c.rect(0, 94, 12, 1, STONE["base"])
    return c


def gen_bookshelf(path):
    rng = Rng(0xB0055E)
    sheet = Canvas(128, 128)
    side = _shelf_side(rng)
    for i, cell in enumerate((_shelf_back(rng), side,
                              _shelf_front(rng), side.mirrored())):
        cell.outline(OUT)
        _paste_col(sheet, cell, i)
    _crescent(sheet, 2 * 32 + 16, 23, PATINA_HI, star_color=GLOW)
    _crescent(sheet, 0 * 32 + 17, 44, STONE["deep"], star=False)
    _crescent(sheet, 0 * 32 + 16, 43, STONE["hi"], star=False)
    sheet.save(path)


# =======================================================================
# 2. Skywatch cabinet - 128x128, CabinetObject
# =======================================================================
#
# oakcabinet's ranges: front 12..73, back 34..95, sides 20..95 in a 16px slab.


def _cabinet_door(c, x, y, w, h, rng, pull_right):
    _panel(c, x, y, w, h, rng)
    c.rect(x, y, w, 1, STONE["deep"])
    c.rect(x + 1, y + 2, w - 2, 1, STONE["light"])            # inner bead
    c.rect(x + 1, y + h - 3, w - 2, 1, STONE["light"])
    px = x + w - 4 if pull_right else x + 1
    _iron_rail(c, px, y + h // 2 - 4, 3, 9)                   # back plate
    c.rect(px, y + h // 2 - 2, 3, 5, IRON["light"])           # the handle
    c.rect(px, y + h // 2 - 2, 1, 5, IRON["hi"])
    c.put(px + 1, y + h // 2 - 3, PATINA_HI)
    c.put(px + 1, y + h // 2 + 3, PATINA)


def _cabinet_front(rng):
    c = Canvas(32, 128)
    _slab(c, 2, 12, 28, 2, rng, grain=False)
    _slab(c, 0, 14, 32, 10, rng)                              # top slab
    c.rect(0, 22, 32, 2, PATINA)
    _slab(c, 0, 24, 32, 4, rng)
    _slab(c, 0, 28, 32, 40, rng)                              # carcass
    _cabinet_door(c, 4, 30, 10, 36, rng, True)
    _cabinet_door(c, 18, 30, 10, 36, rng, False)
    _iron_post(c, 14, 28, 4, 40)
    c.rect(15, 46, 2, 4, PATINA)
    _slab(c, 0, 68, 32, 4, rng, grain=False)                  # base rail
    c.rect(0, 72, 32, 2, STONE["deep"])
    return c


def _cabinet_back(rng):
    c = Canvas(32, 128)
    _slab(c, 2, 34, 28, 2, rng, grain=False)
    _slab(c, 0, 36, 32, 10, rng)
    c.rect(0, 44, 32, 2, PATINA)
    _slab(c, 0, 46, 32, 44, rng)
    _panel(c, 3, 50, 26, 16, rng)
    _panel(c, 3, 70, 26, 16, rng)
    _iron_rail(c, 0, 67, 32, 2)
    _slab(c, 0, 90, 32, 4, rng, grain=False)
    c.rect(0, 94, 32, 2, STONE["deep"])
    return c


def _cabinet_side(rng):
    c = Canvas(32, 128)
    _slab(c, 2, 20, 12, 2, rng, grain=False)
    _slab(c, 0, 22, 16, 8, rng)
    c.rect(0, 28, 16, 2, PATINA)
    _slab(c, 0, 30, 16, 60, rng)
    _panel(c, 3, 34, 10, 24, rng)
    _panel(c, 3, 62, 10, 24, rng)
    _iron_rail(c, 0, 58, 16, 2)
    _slab(c, 0, 90, 16, 2, rng, grain=False)
    c.rect(0, 92, 16, 4, STONE["deep"])
    return c


def gen_cabinet(path):
    rng = Rng(0xCAB17E)
    sheet = Canvas(128, 128)
    side = _cabinet_side(rng)
    for i, cell in enumerate((_cabinet_back(rng), side,
                              _cabinet_front(rng), side.mirrored())):
        cell.outline(OUT)
        _paste_col(sheet, cell, i)
    _crescent(sheet, 2 * 32 + 16, 19, PATINA_HI, star_color=GLOW)
    _crescent(sheet, 0 * 32 + 17, 42, STONE["deep"], star=False)
    _crescent(sheet, 0 * 32 + 16, 41, STONE["hi"], star=False)
    sheet.save(path)


# =======================================================================
# 3. Skywatch clock - 128x64, ClockObject
# =======================================================================
#
# oakclock's ranges: front 6..47, back 20..61, sides 18..57 in a 12px slab.
# Ours is an astronomical dial rather than a pendulum case, which is what a
# watchtower would own, but it keeps vanilla's head-neck-foot silhouette.


def _dial_face(c, cx, cy, rng):
    """The glazed face: a stormglass disc with a crescent hand and hour pips."""
    c.ellipse(cx, cy, 6, 6, GLASS["deep"])
    c.ellipse(cx, cy, 5, 5, GLASS["base"])
    c.ellipse(cx - 1, cy - 1, 3, 3, GLASS["light"])
    for dx, dy in ((0, -5), (5, 0), (0, 5), (-5, 0)):
        c.put(cx + dx, cy + dy, PATINA_HI)
    for dx, dy in ((4, -4), (4, 4), (-4, 4), (-4, -4)):
        c.put(cx + dx, cy + dy, PATINA)
    c.rect(cx, cy - 4, 1, 5, IRON["deep"])                    # long hand
    c.rect(cx, cy, 3, 1, IRON["deep"])                        # short hand
    c.put(cx, cy, GLOW_HI)
    c.put(cx - 2, cy - 3, GLASS["hi"])


def _clock_front(rng):
    c = Canvas(32, 64)
    _slab(c, 12, 6, 8, 2, rng, grain=False)                   # head cap
    _slab(c, 10, 8, 12, 2, rng, grain=False)
    _slab(c, 8, 10, 16, 18, rng)                              # head body
    c.rect(8, 10, 16, 1, STONE["hi"])
    _slab(c, 10, 28, 12, 2, rng, grain=False)
    _slab(c, 12, 30, 8, 2, rng, grain=False)
    _dial_face(c, 16, 19, rng)
    _iron_post(c, 14, 32, 4, 6)                               # neck
    _slab(c, 8, 32, 6, 4, rng)                                # neck wings
    _slab(c, 18, 32, 6, 4, rng)
    _slab(c, 6, 36, 20, 8, rng)                               # foot
    c.rect(6, 40, 20, 2, PATINA)
    _slab(c, 6, 44, 20, 2, rng, grain=False)
    c.rect(6, 46, 20, 2, STONE["deep"])
    return c


def _clock_back(rng):
    c = Canvas(32, 64)
    _slab(c, 12, 20, 8, 2, rng, grain=False)
    _slab(c, 10, 22, 12, 2, rng, grain=False)
    _slab(c, 8, 24, 16, 18, rng)
    c.rect(8, 24, 16, 1, STONE["hi"])
    _slab(c, 10, 42, 12, 2, rng, grain=False)
    _slab(c, 12, 44, 8, 2, rng, grain=False)
    c.ellipse(16, 33, 5, 5, IRON["base"])                     # movement boss
    c.ellipse(15, 32, 3, 3, IRON["light"])
    c.put(14, 31, IRON["hi"])
    c.ellipse(16, 33, 2, 2, PATINA)
    _iron_post(c, 14, 46, 4, 4)
    _slab(c, 6, 50, 20, 6, rng)
    c.rect(6, 54, 20, 2, PATINA)
    _slab(c, 6, 56, 20, 2, rng, grain=False)
    c.rect(6, 58, 20, 4, STONE["deep"])
    return c


def _clock_side(rng):
    c = Canvas(32, 64)
    _slab(c, 2, 18, 8, 2, rng, grain=False)
    _slab(c, 0, 20, 12, 22, rng)                              # head, edge on
    c.rect(0, 20, 12, 1, STONE["hi"])
    _panel(c, 2, 24, 8, 14, rng)
    c.rect(3, 26, 2, 10, GLASS["base"])                       # glazed edge
    c.put(3, 27, GLASS["hi"])
    _slab(c, 2, 42, 8, 2, rng, grain=False)
    _iron_post(c, 4, 44, 4, 4)
    _slab(c, 0, 48, 12, 6, rng)
    c.rect(0, 52, 12, 2, PATINA)
    c.rect(0, 54, 12, 4, STONE["deep"])
    c.rect(0, 54, 12, 1, STONE["base"])
    return c


def gen_clock(path):
    rng = Rng(0xC10C4A)
    sheet = Canvas(128, 64)
    side = _clock_side(rng)
    for i, cell in enumerate((_clock_back(rng), side,
                              _clock_front(rng), side.mirrored())):
        cell.outline(OUT)
        _paste_col(sheet, cell, i)
    sheet.save(path)


# =======================================================================
# 4. Skywatch display stand - 128x32, DisplayStandObject
# =======================================================================
#
# oakdisplay's four columns are byte-identical: the stand is rotationally
# symmetric, so all four rotations draw the same pedestal. Its silhouette is
# reproduced exactly (576 opaque px): plate 0..15, neck 16..19, foot 20..31.
# DisplayStandObject draws the held item centred at drawY + 12, i.e. on the
# plate, which is why the plate's lit top surface is where it is.


def _display_cell(rng):
    c = Canvas(32, 32)
    # top plate, seen from slightly above: a stone salver with a silk mat
    c.rect(10, 0, 12, 2, STONE["base"])
    c.rect(8, 2, 16, 2, STONE["light"])
    _slab(c, 6, 4, 20, 8, rng)
    c.rect(8, 5, 16, 5, SILK["base"])                         # the mat
    c.rect(8, 5, 16, 1, SILK["hi"])
    c.rect(8, 9, 16, 1, SILK["deep"])
    _grain(c, 9, 6, 14, 3, rng, hi=SILK["hi"], lo=SILK["light"], density=18)
    c.rect(6, 12, 20, 2, PATINA)                              # plate rim
    c.rect(6, 14, 20, 2, STONE["deep"])
    # neck
    _slab(c, 8, 16, 16, 4, rng, grain=False)
    _iron_post(c, 14, 16, 4, 4)
    # foot
    _slab(c, 6, 20, 20, 8, rng)
    _iron_post(c, 14, 20, 4, 8)
    c.rect(8, 22, 5, 4, mix(STONE["base"], STONE["deep"], 0.5))
    c.rect(19, 22, 5, 4, mix(STONE["base"], STONE["deep"], 0.5))
    c.rect(8, 2, 16, 1, STONE["hi"])
    c.rect(8, 28, 16, 2, STONE["deep"])
    c.rect(10, 30, 12, 2, mix(STONE["deep"], OUT, 0.5))
    return c


def gen_display(path):
    rng = Rng(0xD15B1A)
    sheet = Canvas(128, 32)
    cell = _display_cell(rng)
    cell.outline(OUT)
    _crescent(cell, 15, 24, PATINA_HI, star=False)
    for i in range(4):
        _paste_col(sheet, cell, i)
    sheet.save(path)


# =======================================================================
# 5. Windsilk Loom - 128x64, CraftingStationObject
# =======================================================================
#
# Two tiles tall like the forge and the alchemy table it answers to. The warp
# is the readable idea: pale threads over a dark bed, with finished cloth
# rolled on the beam at the bottom. Threads are laid on `_recess` mass so the
# outline pass cannot swallow them.


def _warp(c, x, y, w, h, rng):
    _recess(c, x, y, w, h)
    for i in range(x + 1, x + w - 1, 3):
        c.rect(i, y, 1, h, SILK["base"])
        c.rect(i, y, 1, 1, SILK["hi"])
        if rng.chance(0.4):
            c.put(i, y + rng.range(2, h - 3), SILK["light"])
    for i in range(x + 2, x + w - 1, 3):
        c.rect(i, y, 1, h, mix(SILK["deep"], OUT, 0.35))


def _cloth_roll(c, x, y, w, h, rng):
    """Finished skyweave wound on the cloth beam."""
    c.rect(x, y, w, h, SILK["base"])
    c.rect(x, y, w, 1, SILK["hi"])
    c.rect(x, y + 1, w, 1, SILK["light"])
    c.rect(x, y + h - 1, w, 1, SILK["deep"])
    for i in range(x + 2, x + w - 2, 5):
        c.rect(i, y + 1, 1, h - 2, SILK["deep"])              # fold shadows
        c.put(i + 1, y + 2, SILK["hi"])
    c.rect(x, y + h - 2, w, 1, PATINA)


def _loom_front(rng):
    c = Canvas(32, 64)
    _under_mass(c, 4, 53, 24, 5, (4, 24))
    _wood_board(c, 2, 10, 28, 5, rng)                         # top beam
    _iron_rail(c, 2, 14, 28, 2)
    _wood_board(c, 2, 16, 5, 34, rng)                         # left upright
    _wood_board(c, 25, 16, 5, 34, rng)                        # right upright
    _warp(c, 7, 16, 18, 20, rng)
    _iron_rail(c, 4, 26, 24, 3)                               # heddle bar
    c.rect(5, 27, 22, 1, IRON["hi"])
    c.put(6, 28, PATINA_HI)
    c.put(25, 28, PATINA_HI)
    _wood_board(c, 5, 36, 22, 3, rng)                         # breast beam
    _cloth_roll(c, 6, 39, 20, 8, rng)
    _wood_board(c, 4, 47, 24, 3, rng)                         # cloth beam
    _slab(c, 2, 49, 28, 4, rng)                               # stone footing
    c.rect(2, 51, 28, 2, STONE["deep"])
    # the shuttle, parked on the breast beam
    c.rect(18, 33, 7, 3, WOOD["light"])
    c.rect(18, 33, 7, 1, WOOD["hi"])
    c.rect(20, 34, 3, 1, SILK["light"])
    return c


def _loom_back(rng):
    c = Canvas(32, 64)
    _under_mass(c, 4, 53, 24, 5, (4, 24))
    _wood_board(c, 2, 10, 28, 5, rng)
    _iron_rail(c, 2, 14, 28, 2)
    _wood_board(c, 2, 16, 5, 34, rng)
    _wood_board(c, 25, 16, 5, 34, rng)
    _slab(c, 7, 16, 18, 30, rng)                              # boarded back
    _panel(c, 9, 19, 14, 11, rng)
    _iron_rail(c, 7, 31, 18, 2)
    _panel(c, 9, 34, 14, 9, rng)
    _wood_board(c, 4, 46, 24, 4, rng)
    _slab(c, 2, 49, 28, 4, rng)
    c.rect(2, 51, 28, 2, STONE["deep"])
    return c


def _loom_side(rng):
    c = Canvas(32, 64)
    _under_mass(c, 8, 53, 16, 5, (8, 20))
    _wood_board(c, 6, 10, 12, 5, rng)                         # beam end
    _wood_board(c, 8, 15, 8, 35, rng)                         # the single post
    _panel(c, 9, 18, 6, 26, rng)
    _iron_rail(c, 7, 26, 10, 3)
    # the warp seen edge on. Not the front view's threads turned sideways:
    # from here the whole warp is one thin sheet of cloth, so it is drawn as a
    # pale plane with a lit near edge. Drawn as horizontal ticks it read as a
    # ladder rather than as cloth.
    _recess(c, 16, 18, 6, 22)
    c.rect(17, 18, 4, 22, SILK["deep"])
    c.rect(17, 18, 2, 22, SILK["base"])
    c.rect(17, 18, 1, 22, SILK["light"])
    c.rect(17, 18, 4, 1, SILK["hi"])
    for y in (24, 31, 36):
        c.rect(17, y, 4, 1, mix(SILK["deep"], OUT, 0.3))      # weft shadows
    _wood_board(c, 15, 40, 9, 3, rng)
    _cloth_roll(c, 16, 43, 7, 5, rng)
    _wood_board(c, 6, 48, 18, 3, rng)
    _slab(c, 5, 49, 20, 4, rng)
    c.rect(5, 51, 20, 2, STONE["deep"])
    return c


def gen_loom(path):
    rng = Rng(0x100AA1)
    sheet = Canvas(128, 64)
    side = _loom_side(rng)
    for i, cell in enumerate((_loom_back(rng), side,
                              _loom_front(rng), side.mirrored())):
        cell.outline(OUT)
        _paste_col(sheet, cell, i)
    _crescent(sheet, 2 * 32 + 16, 12, PATINA_HI, star_color=GLOW)
    _crescent(sheet, 0 * 32 + 17, 25, STONE["deep"], star=False)
    _crescent(sheet, 0 * 32 + 16, 24, STONE["hi"], star=False)
    sheet.save(path)


# =======================================================================
# 6. Aether Forge - 128x96, ProcessingForgeObject layout
# =======================================================================
#
# Body: four 32x64 rotation columns, rows 0..63, drawn at drawY - 32.
# Fire:  four 32x32 frames on row 2 (sheet rows 64..95), drawn at drawY, so
#        their rows 4..15 land over the body's rows 36..47 - which is exactly
#        where the front column's mouth is cut.


MOUTH_X, MOUTH_W = 9, 14
MOUTH_Y, MOUTH_H = 36, 12


def _forge_flue(c, rng):
    """The chimney. Vanilla's forge is read as a forge almost entirely by its
    flue: a narrow pipe standing clear of the body, not a wide nub. Ours is a
    banded stone stack with an aetherium ember in the throat, rows 8..17."""
    _slab(c, 12, 10, 8, 8, rng)
    _slab(c, 11, 8, 10, 3, rng, grain=False)                  # cap
    c.rect(11, 8, 10, 1, STONE["hi"])
    _recess(c, 13, 8, 6, 2)                                   # vent throat
    c.rect(13, 8, 6, 1, AETHER["deep"])
    c.put(15, 9, AETHER["base"])
    c.put(17, 9, AETHER["deep"])
    _iron_rail(c, 12, 14, 8, 2)                               # flue band


def _forge_front(rng):
    c = Canvas(32, 64)
    _forge_flue(c, rng)
    _slab(c, 5, 18, 22, 4, rng)                               # hood shoulders
    _slab(c, 2, 22, 28, 6, rng)                               # hood
    c.rect(2, 22, 28, 1, STONE["hi"])
    _iron_rail(c, 2, 26, 28, 2)
    _slab(c, 1, 28, 30, 8, rng)                               # mantel
    c.rect(1, 34, 30, 2, PATINA)
    _slab(c, 1, MOUTH_Y, 30, MOUTH_H, rng)                    # firebox face
    _recess(c, MOUTH_X, MOUTH_Y, MOUTH_W, MOUTH_H)            # the arch
    for i in range(MOUTH_W):                                  # arch voussoirs
        c.put(MOUTH_X + i, MOUTH_Y, STONE["light"])
    c.put(MOUTH_X, MOUTH_Y + 1, STONE["base"])
    c.put(MOUTH_X + MOUTH_W - 1, MOUTH_Y + 1, STONE["base"])
    _ember_bed(c, MOUTH_X + 1, MOUTH_Y + MOUTH_H - 2, MOUTH_W - 2, 2, rng, 0)
    _iron_post(c, 5, 37, 3, 10)                               # jamb ironwork
    _iron_post(c, 24, 37, 3, 10)
    _slab(c, 1, 48, 30, 6, rng)                               # hearth apron
    c.rect(1, 48, 30, 1, STONE["hi"])
    _iron_rail(c, 1, 52, 30, 2)
    _slab(c, 2, 54, 28, 4, rng, grain=False)                  # plinth
    c.rect(2, 56, 28, 2, STONE["deep"])
    # an aetherium crucible standing on the apron
    c.ellipse(24, 50, 3, 2, IRON["base"])
    c.ellipse(24, 49, 2, 1, AETHER["light"])
    c.put(24, 49, AETHER["hi"])
    return c


def _forge_back(rng):
    c = Canvas(32, 64)
    _forge_flue(c, rng)
    _slab(c, 5, 18, 22, 4, rng)
    _slab(c, 2, 22, 28, 6, rng)
    _iron_rail(c, 2, 26, 28, 2)
    _slab(c, 1, 28, 30, 26, rng)                              # solid back wall
    c.rect(1, 34, 30, 2, PATINA)
    _panel(c, 4, 38, 24, 12, rng)
    _iron_rail(c, 1, 52, 30, 2)
    _slab(c, 2, 54, 28, 4, rng, grain=False)
    c.rect(2, 56, 28, 2, STONE["deep"])
    return c


def _forge_side(rng):
    c = Canvas(32, 64)
    _forge_flue(c, rng)
    _slab(c, 7, 18, 18, 4, rng)
    _slab(c, 4, 22, 25, 6, rng)
    _iron_rail(c, 4, 26, 25, 2)
    _slab(c, 4, 28, 25, 26, rng)                              # body, edge on
    c.rect(4, 34, 25, 2, PATINA)
    _panel(c, 7, 38, 19, 12, rng)
    _iron_rail(c, 4, 52, 25, 2)
    _slab(c, 4, 54, 25, 4, rng, grain=False)
    c.rect(4, 56, 25, 2, STONE["deep"])
    # the bellows: a windsilk bag on cloudwood arms, hung off the near side
    _wood_board(c, 1, 32, 4, 3, rng)
    _wood_board(c, 1, 44, 4, 3, rng)
    c.rect(1, 35, 4, 9, SILK["base"])
    c.rect(1, 35, 1, 9, SILK["light"])
    c.rect(4, 35, 1, 9, SILK["deep"])
    for y in (37, 40, 42):
        c.rect(1, y, 4, 1, SILK["deep"])
    c.put(2, 36, SILK["hi"])
    return c


def _forge_fire(frame, rng):
    """One 32x32 fire frame.

    Measured off `forge.png`: vanilla's four frames each carry 184 opaque px
    filling rows 4..15 across x 8..23 — 96% of that rectangle. The fire is not
    a few tongues on a dark hearth, it is the whole mouth full of light. So
    this fills the arch (x 9..22, the body column's `_recess`) with a heat
    field and puts the animation into the field and the tongues above it,
    rather than drawing thin flames on transparency.
    """
    c = Canvas(32, 32)
    # the mouth's own dark rim, drawn in the fire frame the way vanilla does,
    # so the fire reads as sitting INSIDE an opening rather than as a lit tile
    for j in range(4, 16):
        for i in range(MOUTH_X, MOUTH_X + MOUTH_W):
            c.put(i, j, mix(OUT, AETHER["deep"], 0.25))
    top, bottom = 5, 15
    for j in range(top, bottom):
        for i in range(MOUTH_X + 1, MOUTH_X + MOUTH_W - 1):
            u = abs(i - (MOUTH_X + (MOUTH_W - 1) / 2.0)) / ((MOUTH_W - 1) / 2.0)
            t = (j - top) / float(bottom - top - 1)
            heat = (1.0 - u * 0.7) * (0.28 + 0.72 * t)
            # the animated part: a slow diagonal ripple through the melt
            heat += 0.13 * (((i * 2 + j * 3 + frame * 5) % 7) - 3) / 3.0
            if heat > 0.78:
                col = AETHER["hi"]
            elif heat > 0.58:
                col = AETHER["light"]
            elif heat > 0.34:
                col = AETHER["base"]
            else:
                col = AETHER["deep"]
            c.put(i, j, col)
    _ember_bed(c, MOUTH_X + 2, 13, MOUTH_W - 4, 2, rng, frame)
    # tongues licking up the arch head. Kept inside rows 4..15, vanilla's own
    # extent, so no flame is ever drawn over the mantel above the mouth.
    for i, dx in enumerate((-4, 0, 4, 2, -2)):
        if (frame + i) % 2 == 0:
            c.put(16 + dx, 4, AETHER["light"])
            c.put(16 + dx, 5, with_alpha(AETHER["hi"], 210))
    return c


def gen_aetherforge(path):
    rng = Rng(0xAE7F09)
    sheet = Canvas(128, 96)
    side = _forge_side(rng)
    for i, cell in enumerate((_forge_back(rng), side,
                              _forge_front(rng), side.mirrored())):
        cell.outline(OUT)
        _paste_col(sheet, cell, i)
    _crescent(sheet, 2 * 32 + 16, 31, PATINA_HI, star_color=GLOW)
    _crescent(sheet, 0 * 32 + 17, 44, STONE["deep"], star=False)
    _crescent(sheet, 0 * 32 + 16, 43, STONE["hi"], star=False)
    for frame in range(4):
        sheet.paste(_forge_fire(frame, rng), frame * 32, 64)
    sheet.save(path)


# =======================================================================
# 7. Stormglass Kiln - 128x64 (+ _on), cheese-press processing pattern
# =======================================================================


def _kiln_dome(c, rng, lit):
    """The beehive dome: corbelled stone courses under two iron hoops."""
    _slab(c, 13, 6, 6, 4, rng, grain=False)                   # vent stack
    c.rect(14, 6, 4, 1, STONE["hi"])
    _recess(c, 14, 6, 4, 2)
    if lit:
        c.rect(14, 6, 4, 1, FULG["light"])
        c.put(15, 7, FULG["hi"])
    _slab(c, 10, 10, 12, 4, rng)                              # crown course
    _slab(c, 7, 14, 18, 6, rng)
    _slab(c, 5, 20, 22, 8, rng)
    _iron_rail(c, 5, 18, 22, 2)                               # upper hoop
    _slab(c, 4, 28, 24, 10, rng)                              # belly
    _iron_rail(c, 4, 32, 24, 2)                               # lower hoop
    c.put(5, 33, PATINA_HI)
    c.put(26, 33, PATINA)


def _kiln_front(rng, lit):
    c = Canvas(32, 64)
    _kiln_dome(c, rng, lit)
    _slab(c, 3, 38, 26, 10, rng)                              # stoke wall
    _recess(c, 11, 38, 10, 10)                                # stoke hole
    for i in range(10):
        c.put(11 + i, 38, STONE["light"])
    if lit:
        _glow_mouth(c, 12, 40, 8, 7)
    else:
        c.rect(12, 44, 8, 3, mix(OUT, FULG["deep"], 0.35))    # cold ash
        c.put(14, 45, FULG["deep"])
        c.put(18, 46, FULG["deep"])
    _iron_post(c, 7, 39, 3, 8)
    _iron_post(c, 22, 39, 3, 8)
    _slab(c, 2, 48, 28, 6, rng)                               # hearth apron
    c.rect(2, 48, 28, 1, STONE["hi"])
    _iron_rail(c, 2, 52, 28, 2)
    _slab(c, 2, 54, 28, 4, rng, grain=False)
    c.rect(2, 56, 28, 2, STONE["deep"])
    _glass_pane(c, 24, 42, 5, 10, rng)                        # a finished pane
    if lit:
        c.put(25, 43, GLASS["hi"])
    return c


def _glow_mouth(c, x, y, w, h):
    """The melt seen through the stoke hole: hot centre, cooler rim."""
    for j in range(h):
        t = j / max(1, h - 1)
        for i in range(w):
            u = abs(i - (w - 1) / 2.0) / ((w - 1) / 2.0)
            heat = (1.0 - u * 0.8) * (0.35 + 0.65 * t)
            if heat > 0.72:
                col = FULG["hi"]
            elif heat > 0.5:
                col = FULG["light"]
            elif heat > 0.3:
                col = FULG["base"]
            else:
                col = FULG["deep"]
            c.put(x + i, y + j, col)


def _kiln_back(rng, lit):
    c = Canvas(32, 64)
    _kiln_dome(c, rng, lit)
    _slab(c, 3, 38, 26, 10, rng)
    _panel(c, 6, 40, 20, 6, rng)
    _iron_rail(c, 3, 46, 26, 2)
    _slab(c, 2, 48, 28, 6, rng)
    c.rect(2, 48, 28, 1, STONE["hi"])
    _iron_rail(c, 2, 52, 28, 2)
    _slab(c, 2, 54, 28, 4, rng, grain=False)
    c.rect(2, 56, 28, 2, STONE["deep"])
    return c


def _kiln_side(rng, lit):
    c = Canvas(32, 64)
    _kiln_dome(c, rng, lit)
    _slab(c, 3, 38, 26, 10, rng)
    _iron_rail(c, 3, 44, 26, 2)
    # the pane rack: three stormglass sheets leaning against the kiln's flank
    for i, (px, ph) in enumerate(((3, 12), (7, 10), (11, 8))):
        _glass_pane(c, px, 50 - ph, 3, ph, rng)
        if lit and i == 0:
            c.put(px + 1, 52 - ph, GLASS["hi"])
    _wood_board(c, 2, 46, 14, 2, rng)                         # rack rail
    _slab(c, 2, 48, 28, 6, rng)
    c.rect(2, 48, 28, 1, STONE["hi"])
    _iron_rail(c, 2, 52, 28, 2)
    _slab(c, 2, 54, 28, 4, rng, grain=False)
    c.rect(2, 56, 28, 2, STONE["deep"])
    return c


def gen_kiln(path, lit):
    rng = Rng(0x611A17 if lit else 0x611A16)
    sheet = Canvas(128, 64)
    side = _kiln_side(rng, lit)
    for i, cell in enumerate((_kiln_back(rng, lit), side,
                              _kiln_front(rng, lit), side.mirrored())):
        cell.outline(OUT)
        _paste_col(sheet, cell, i)
    _crescent(sheet, 2 * 32 + 16, 24, PATINA_HI, star_color=GLOW)
    _crescent(sheet, 0 * 32 + 17, 25, STONE["deep"], star=False)
    _crescent(sheet, 0 * 32 + 16, 24, STONE["hi"], star=False)
    sheet.save(path)


# =======================================================================
# 8. Item icons - 32x32, read at 1x in a slot
# =======================================================================


def _icon_loom(path):
    rng = Rng(0x2C0001)
    c = Canvas(32, 32)
    _under_mass(c, 5, 26, 22, 5, (5, 23))
    _wood_board(c, 2, 2, 28, 4, rng)
    _iron_rail(c, 2, 5, 28, 2)
    _wood_board(c, 2, 7, 5, 18, rng)
    _wood_board(c, 25, 7, 5, 18, rng)
    _warp(c, 7, 7, 18, 10, rng)
    _iron_rail(c, 4, 12, 24, 3)
    _wood_board(c, 5, 17, 22, 2, rng)
    _cloth_roll(c, 6, 19, 20, 5, rng)
    _wood_board(c, 4, 24, 24, 2, rng)
    c.outline(OUT)
    _crescent(c, 16, 3, PATINA_HI, star=False)
    c.save(path)


def _icon_forge(path):
    rng = Rng(0x2C0002)
    c = Canvas(32, 32)
    _slab(c, 11, 1, 10, 3, rng, grain=False)
    _slab(c, 7, 4, 18, 4, rng)
    _iron_rail(c, 7, 7, 18, 2)
    _slab(c, 3, 9, 26, 5, rng)
    c.rect(3, 12, 26, 2, PATINA)
    _slab(c, 2, 14, 28, 10, rng)
    _recess(c, 9, 14, 14, 10)
    for i in range(14):
        c.put(9 + i, 14, STONE["light"])
    _ember_bed(c, 10, 21, 12, 2, rng, 1)
    for dx, h in ((-4, 5), (0, 7), (4, 5)):
        _teal_fire(c, 16 + dx, 22, h, rng, 1)
    _iron_post(c, 5, 15, 3, 8)
    _iron_post(c, 24, 15, 3, 8)
    _slab(c, 2, 24, 28, 5, rng)
    _iron_rail(c, 2, 27, 28, 2)
    c.rect(3, 29, 26, 2, STONE["deep"])
    c.outline(OUT)
    c.save(path)


def _icon_kiln(path):
    rng = Rng(0x2C0003)
    c = Canvas(32, 32)
    _slab(c, 13, 0, 6, 3, rng, grain=False)
    _recess(c, 14, 0, 4, 2)
    c.rect(14, 0, 4, 1, FULG["light"])
    _slab(c, 10, 3, 12, 3, rng)
    _slab(c, 7, 6, 18, 4, rng)
    _slab(c, 4, 10, 24, 6, rng)
    _iron_rail(c, 4, 9, 24, 2)
    _slab(c, 3, 16, 26, 7, rng)
    _iron_rail(c, 3, 15, 26, 2)
    _recess(c, 11, 16, 10, 7)
    for i in range(10):
        c.put(11 + i, 16, STONE["light"])
    _glow_mouth(c, 12, 17, 8, 5)
    _iron_post(c, 7, 17, 3, 5)
    _iron_post(c, 22, 17, 3, 5)
    _slab(c, 2, 23, 28, 5, rng)
    _iron_rail(c, 2, 26, 28, 2)
    c.rect(3, 28, 26, 3, STONE["deep"])
    _glass_pane(c, 24, 18, 5, 9, rng)
    c.outline(OUT)
    c.save(path)


def _icon_bookshelf(path):
    rng = Rng(0x2C0004)
    c = Canvas(32, 32)
    _slab(c, 3, 1, 26, 2, rng, grain=False)
    _slab(c, 1, 3, 30, 5, rng)
    c.rect(1, 7, 30, 1, PATINA)
    for top in (8, 17):
        _recess(c, 4, top, 24, 8)
        _books(c, 5, top + 1, 22, 7, rng)
        _wood_board(c, 2, top + 8, 28, 1, rng, grain=False)
    _slab(c, 1, 8, 3, 18, rng)
    _slab(c, 28, 8, 3, 18, rng)
    _slab(c, 1, 26, 30, 4, rng)
    _iron_rail(c, 14, 26, 4, 4)
    c.rect(1, 30, 30, 1, STONE["deep"])
    c.outline(OUT)
    _crescent(c, 16, 5, PATINA_HI, star=False)
    c.save(path)


def _icon_cabinet(path):
    rng = Rng(0x2C0005)
    c = Canvas(32, 32)
    _slab(c, 3, 1, 26, 2, rng, grain=False)
    _slab(c, 1, 3, 30, 5, rng)
    c.rect(1, 7, 30, 1, PATINA)
    _slab(c, 1, 8, 30, 19, rng)
    _cabinet_door(c, 4, 10, 10, 15, rng, True)
    _cabinet_door(c, 18, 10, 10, 15, rng, False)
    _iron_post(c, 14, 8, 4, 19)
    c.rect(15, 16, 2, 3, PATINA)
    _slab(c, 1, 27, 30, 3, rng, grain=False)
    c.rect(1, 30, 30, 1, STONE["deep"])
    c.outline(OUT)
    _crescent(c, 16, 5, PATINA_HI, star=False)
    c.save(path)


def _icon_clock(path):
    rng = Rng(0x2C0006)
    c = Canvas(32, 32)
    _slab(c, 12, 1, 8, 2, rng, grain=False)
    _slab(c, 9, 3, 14, 2, rng, grain=False)
    _slab(c, 6, 5, 20, 16, rng)
    c.rect(6, 5, 20, 1, STONE["hi"])
    _slab(c, 9, 21, 14, 2, rng, grain=False)
    _dial_face(c, 16, 13, rng)
    c.ellipse(16, 13, 8, 8, STONE["base"])
    c.ellipse(16, 13, 7, 7, STONE["light"])
    _dial_face(c, 16, 13, rng)
    _iron_post(c, 14, 23, 4, 3)
    _slab(c, 5, 26, 22, 4, rng)
    c.rect(5, 29, 22, 1, PATINA)
    c.rect(5, 30, 22, 1, STONE["deep"])
    c.outline(OUT)
    c.save(path)


def _icon_display(path):
    rng = Rng(0x2C0007)
    c = Canvas(32, 32)
    c.rect(9, 1, 14, 2, STONE["base"])
    c.rect(7, 3, 18, 2, STONE["light"])
    _slab(c, 5, 5, 22, 7, rng)
    c.rect(7, 6, 18, 4, SILK["base"])
    c.rect(7, 6, 18, 1, SILK["hi"])
    _grain(c, 8, 7, 16, 2, rng, hi=SILK["hi"], lo=SILK["light"], density=16)
    c.rect(5, 12, 22, 2, PATINA)
    c.rect(5, 14, 22, 1, STONE["deep"])
    _slab(c, 8, 15, 16, 4, rng, grain=False)
    _iron_post(c, 14, 15, 4, 4)
    _slab(c, 5, 19, 22, 8, rng)
    _iron_post(c, 14, 19, 4, 8)
    c.rect(5, 27, 22, 2, STONE["deep"])
    c.rect(8, 29, 16, 2, mix(STONE["deep"], OUT, 0.5))
    c.outline(OUT)
    _crescent(c, 15, 23, PATINA_HI, star=False)
    c.save(path)


def _icon_skyweave(path):
    """A folded bolt of skyweave with a patina selvage."""
    rng = Rng(0x2C0011)
    c = Canvas(32, 32)
    for i, (x, y, w, h) in enumerate(((4, 8, 24, 6), (3, 14, 26, 6), (5, 20, 22, 5))):
        c.rect(x, y, w, h, SILK["base"])
        c.rect(x, y, w, 1, SILK["hi"])
        c.rect(x, y + 1, w, 1, SILK["light"])
        c.rect(x, y + h - 1, w, 1, SILK["deep"])
        c.rect(x, y, 1, h, SILK["light"])
        c.rect(x + w - 1, y, 1, h, SILK["deep"])
        _grain(c, x + 1, y + 1, w - 2, h - 2, rng,
               hi=SILK["hi"], lo=SILK["light"], density=16)
        c.rect(x, y + h - 2, w, 1, PATINA if i == 1 else SILK["deep"])
    # the loose end lifting off the top of the bolt
    for i, x in enumerate(range(6, 26, 2)):
        c.rect(x, 5 + (i % 2), 2, 3, SILK["light"])
        c.put(x, 5 + (i % 2), SILK["hi"])
    c.rect(4, 24, 24, 2, SILK["deep"])
    c.rect(6, 26, 20, 1, mix(SILK["deep"], OUT, 0.4))
    c.outline(OUT)
    c.put(9, 10, SILK["hi"])
    c.put(10, 10, SILK["hi"])
    c.save(path)


def _icon_stormsteel(path):
    """A stormsteel ingot.

    Vanilla's `ironbar` is a 28x26 isometric block carrying 440 opaque px — a
    lump of metal you could drop on your foot. This is the same box (a rhombus
    top face extruded straight down, lit face left, shadow face right), which
    is what keeps a bar from reading as a sticker.
    """
    c = Canvas(32, 32)
    r = STORMSTEEL
    cx, cy, ax, ay, depth = 16, 11, 13, 6, 10

    def edge(x):
        """(top, bottom) row of the top face at column x, or None outside."""
        dx = abs(x - cx)
        if dx > ax:
            return None
        h = ay * (1.0 - dx / float(ax))
        return int(round(cy - h)), int(round(cy + h))

    for x in range(cx - ax, cx + ax + 1):
        span = edge(x)
        if span is None:
            continue
        top, bottom = span
        # side faces: left lit, right in shadow
        side = r["base"] if x < cx else r["deep"]
        for y in range(bottom + 1, bottom + 1 + depth):
            c.put(x, y, side)
        c.put(x, bottom + depth, mix(side, OUT, 0.55))        # foot line
        # top face
        for y in range(top, bottom + 1):
            c.put(x, y, r["light"])
        c.put(x, top, r["hi"])                                # lit upper edge
    # the seam where the two side faces meet, and the bar's crisp near corner
    for y in range(cy + ay + 1, cy + ay + 1 + depth):
        c.put(cx, y, mix(r["base"], r["deep"], 0.5))
    # storm shard temper: a violet vein and two sparks across the top face
    for x, y in ((12, 10), (13, 10), (14, 11), (18, 9), (19, 9), (20, 10)):
        c.put(x, y, CRYSTAL["light"])
    c.put(15, 11, CRYSTAL["hi"])
    c.put(19, 10, CRYSTAL["hi"])
    c.put(8, 10, r["hi"])
    c.outline(OUT)
    c.save(path)


def _icon_stormglass(path):
    """Three stacked stormglass panes seen at a slight angle."""
    rng = Rng(0x2C0013)
    c = Canvas(32, 32)
    _glass_pane(c, 4, 10, 12, 15, rng)
    _glass_pane(c, 13, 7, 12, 16, rng)
    _glass_pane(c, 19, 12, 9, 13, rng)
    c.rect(4, 25, 24, 2, mix(GLASS["deep"], OUT, 0.35))
    c.put(15, 9, GLASS["hi"])
    c.put(16, 9, GLASS["hi"])
    c.put(6, 12, GLASS["hi"])
    c.outline(OUT)
    c.save(path)


def gen_item_icons(items_dir):
    _icon_loom(f"{items_dir}/windsilkloom.png")
    _icon_forge(f"{items_dir}/aetherforge.png")
    _icon_kiln(f"{items_dir}/stormglasskiln.png")
    _icon_bookshelf(f"{items_dir}/skywatchbookshelf.png")
    _icon_cabinet(f"{items_dir}/skywatchcabinet.png")
    _icon_clock(f"{items_dir}/skywatchclock.png")
    _icon_display(f"{items_dir}/skywatchdisplay.png")
    _icon_skyweave(f"{items_dir}/skyweave.png")
    _icon_stormsteel(f"{items_dir}/stormsteelbar.png")
    _icon_stormglass(f"{items_dir}/stormglass.png")


# =======================================================================
# entry point
# =======================================================================


def generate(objects_dir, items_dir):
    os.makedirs(objects_dir, exist_ok=True)
    os.makedirs(items_dir, exist_ok=True)

    gen_bookshelf(f"{objects_dir}/skywatchbookshelf.png")
    gen_cabinet(f"{objects_dir}/skywatchcabinet.png")
    gen_clock(f"{objects_dir}/skywatchclock.png")
    gen_display(f"{objects_dir}/skywatchdisplay.png")

    gen_loom(f"{objects_dir}/windsilkloom.png")
    gen_aetherforge(f"{objects_dir}/aetherforge.png")
    gen_kiln(f"{objects_dir}/stormglasskiln.png", False)
    gen_kiln(f"{objects_dir}/stormglasskiln_on.png", True)

    gen_item_icons(items_dir)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("usage: gen_professions.py <objects_dir> <items_dir>")
        raise SystemExit(2)
    generate(sys.argv[1], sys.argv[2])
    print(f"Skywatch professions written to {sys.argv[1]} / {sys.argv[2]}")
