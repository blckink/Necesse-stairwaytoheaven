#!/usr/bin/env python3
"""Sheet-format audit: assert layout invariants the size audit cannot see.

The size audit compares opaque MASS against a vanilla analogue. That catches
sprites that read too thin, but it is blind to geometry -- and some sheets are
addressed by the engine at fixed offsets, so a cell painted in the wrong place
renders wrong no matter how much ink is in it. A door drawn over its whole cell
scores BETTER on mass while rendering three tiles too tall.

Checks:

  Wall sheets (352x128), doors. WallDoorObject/WallDoorOpenObject draw eight
  32x128 cells at pos(drawX, drawY - 96), so sheet row 96 is the tile's top
  edge and anything above it sticks out over the wall. The expected top rows
  below were measured off vanilla stonewall.png; a wall segment itself only
  rises 16px above its tile, so a door whose cell starts near row 0 towers over
  the wall it sits in. Bottom row is always 127.

  Wall sheets, windows. Same trap one strip to the left, and it shipped even
  after the doors were fixed, because this audit was only looking at doors.
  WallWindowObject reads x 64..96 as 16px cells and draws TWO windows from it:
  head-on uses rows 0-1 at drawY-16 and drawY; edge-on uses rows 2..7 at
  drawY-64, -48, -32, -16, 0 and +16. That second list can reach two tiles up,
  and vanilla does not use the reach -- on stonewall and cryptwall rows 2-4 are
  EMPTY, so the window ends up 48px, exactly as tall as its wall. Filling those
  rows renders a window 96px tall against a 48px wall.

  Rotation sheets. Every furniture piece and workstation whose renderer reads
  sprite(rotation % 4, 0, 32, height) is four 32px columns at a FIXED vertical
  anchor, and the anchor is not the same for every class:

      BookshelfObject / CabinetObject   pos(drawX, drawY - height + 64)
      ClockObject / CraftingStationObject
                                        pos(drawX, drawY - height + 32)
      DisplayStandObject / processing stations
                                        pos(drawX, drawY - (height - 32))

  Two consequences a mass check cannot see. First, a column left EMPTY is a
  rotation that renders nothing - the player turns the piece and it vanishes.
  Second, vanilla's own bookshelf, cabinet and clock deliberately place each
  rotation at a DIFFERENT row band inside the cell, because a case standing
  against the north wall is drawn higher on screen than the same case turned
  around; drawing all four columns bottom-aligned makes the piece jump a tile
  when it is rotated. The bands below are vanilla's, measured column by column
  off oakbookshelf / oakcabinet / oakclock / oakdisplay.

  The Aether Forge's fire strip. ProcessingForgeObject draws the body as
  sprite(rotation % 4, 0, 32, height - 32) at drawY - 32 and the fire as
  sprite(frame, (height - 32) / 32, 32) at drawY. So the last 32px row of the
  sheet is four animation frames, they must all be non-empty and actually
  differ, and their content has to land inside the mouth the body cut - which
  is rows 4..15 of the fire cell, i.e. body rows 36..47. Fire drawn outside
  that band burns on the masonry.

  The kiln's lit sheet. The cheese-press pattern loads objects/<id>_on through
  GameTexture.fromFileRaw and falls back to the cold sheet on
  FileNotFoundException, so a missing or wrongly-sized _on fails silently: the
  station simply never looks lit. It must exist, match the base sheet's size,
  and differ from it.

Usage:  python3 tools/sheet_format_audit.py
Exit code 1 if anything is out of format.
"""
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")

WALL_SHEETS = ("objects/skystonebrickwall.png", "objects/nightfellwall.png",
               "objects/cloudmarblewall.png", "objects/beetlewall.png")

# 32-cell index -> (highest top row vanilla ever uses, bottom row).
#
# These were originally one sheet's exact values, which was wrong twice over: it
# rejected art that sits comfortably inside what vanilla itself does, and it
# implied a precision the format does not have. Measured across ALL 28 vanilla
# 352x128 wall sheets, each cell's top varies by 20-30px — cell 3 runs 72..94,
# cell 6 runs 48..72 — because a closed door head-on is taller than a leaf seen
# edge-on and every material draws its crown differently.
#
# So the rule is a ceiling, not an equality: content may start no higher than
# the tallest vanilla art in that cell. Anything above that row is drawn off
# the top of the wall it stands in.
#
# Cells 0-2 are the 16px auto-tile blob and the window insert, which do use the
# full height.
DOOR_CELLS = {
    3: (72, 127),   # rot 0 closed, head-on          (vanilla 72..94)
    4: (50, 127),   # rot 0 open, leaf edge-on       (vanilla 50..76)
    5: (50, 127),   # rot 1 closed, edge-on          (vanilla 50..76)
    6: (48, 127),   # rot 1 open, leaf head-on       (vanilla 48..72)
    7: (64, 127),   # rot 2 closed, head-on          (vanilla 64..94)
    8: (68, 127),   # rot 2 open, leaf edge-on       (vanilla 68..76)
    9: (52, 127),   # rot 3 closed, edge-on          (vanilla 52..76)
    10: (72, 127),  # rot 3 open, leaf head-on       (vanilla 72..94)
}


# 16px cell row -> whether the edge-on window may put anything there.
# Rows 2, 3 and 4 are drawn at drawY-64, -48 and -32: two full tiles and more
# above the tile the window sits on. Vanilla leaves all three empty.
WINDOW_MUST_BE_EMPTY = (2, 3, 4)
WINDOW_STRIP_X = (64, 96)


# Rotation sheets: file -> (size, {column: (first row, last row)}, vanilla ref).
#
# The row bands are the vanilla analogue's own, exact. They are an equality,
# not a ceiling, because the whole point is that the piece occupies the same
# screen space as the vanilla piece the engine draws with the same code: a
# bookshelf whose front column starts eight rows lower is eight pixels shorter
# than every other bookshelf in the game, and nothing else would report it.
# Columns are the engine's dir() order - 0 up (back), 1 right, 2 down (front),
# 3 left.
ROTATION_SHEETS = {
    "objects/skywatchbookshelf.png": (
        (128, 128), {0: (36, 99), 1: (18, 99), 2: (16, 77), 3: (18, 99)}, "oakbookshelf"),
    "objects/skywatchcabinet.png": (
        (128, 128), {0: (34, 95), 1: (20, 95), 2: (12, 73), 3: (20, 95)}, "oakcabinet"),
    "objects/skywatchclock.png": (
        (128, 64), {0: (20, 61), 1: (18, 57), 2: (6, 47), 3: (18, 57)}, "oakclock"),
    "objects/skywatchdisplay.png": (
        (128, 32), {0: (0, 31), 1: (0, 31), 2: (0, 31), 3: (0, 31)}, "oakdisplay"),
}

# Workstation sheets. These are not held to a vanilla piece's exact band - they
# are our own designs - but every rotation must exist, must sit ON the bottom
# of the tile rather than floating, and must not run off the top of its cell.
# `bottom` is the last row the body may use; `top` the first.
STATION_SHEETS = {
    # (size, body height, first row allowed, last row allowed)
    "objects/windsilkloom.png": ((128, 64), 64, 4, 60),
    "objects/aetherforge.png": ((128, 96), 64, 4, 60),
    "objects/stormglasskiln.png": ((128, 64), 64, 4, 60),
    "objects/stormglasskiln_on.png": ((128, 64), 64, 4, 60),
}

# The forge's fire strip: file -> (rows the fire may occupy inside its 32px
# cell). Vanilla's forge uses 4..15; ours cuts its mouth at body rows 36..47,
# which is the same band once the two anchors are lined up.
FIRE_STRIP = {"objects/aetherforge.png": (4, 15)}

# Optional lit sheets loaded through GameTexture.fromFileRaw.
LIT_PAIRS = (("objects/stormglasskiln.png", "objects/stormglasskiln_on.png"),)


def cell_extent(px, height, x0, x1, y0=0):
    rows = [y for y in range(y0, height)
            if any(px[x, y][3] > 0 for x in range(x0, x1))]
    return (min(rows), max(rows)) if rows else (None, None)


def check_rotation_sheets(problems):
    """Four columns, each at the exact row band its vanilla analogue uses."""
    checked = 0
    for rel, (size, bands, ref) in sorted(ROTATION_SHEETS.items()):
        path = os.path.join(RES, rel)
        if not os.path.exists(path):
            problems.append(f"{rel}: missing")
            continue
        im = Image.open(path).convert("RGBA")
        if im.size != size:
            problems.append(f"{rel}: {im.size}, expected {size} -- the renderer "
                            f"reads sprite(rotation % 4, 0, 32, height)")
            continue
        px = im.load()
        for col, (want_top, want_bot) in sorted(bands.items()):
            checked += 1
            top, bot = cell_extent(px, im.height, col * 32, col * 32 + 32)
            if top is None:
                problems.append(
                    f"{rel} column {col}: empty -- rotation {col} would render "
                    f"nothing at all")
            elif (top, bot) != (want_top, want_bot):
                problems.append(
                    f"{rel} column {col}: rows {top}..{bot}, {ref} uses "
                    f"{want_top}..{want_bot} -- the engine anchors both with the "
                    f"same code, so a different band is a different height on "
                    f"screen")
    return checked


def check_station_sheets(problems):
    """Every rotation drawn, nothing running off the cell."""
    checked = 0
    for rel, (size, body_h, want_top, want_bot) in sorted(STATION_SHEETS.items()):
        path = os.path.join(RES, rel)
        if not os.path.exists(path):
            problems.append(f"{rel}: missing")
            continue
        im = Image.open(path).convert("RGBA")
        if im.size != size:
            problems.append(f"{rel}: {im.size}, expected {size}")
            continue
        px = im.load()
        for col in range(4):
            checked += 1
            top, bot = cell_extent(px, body_h, col * 32, col * 32 + 32)
            if top is None:
                problems.append(
                    f"{rel} column {col}: empty -- rotation {col} would render "
                    f"nothing at all")
            elif top < want_top:
                problems.append(
                    f"{rel} column {col}: starts at row {top}, above row "
                    f"{want_top} -- that much runs off the top of the cell")
            elif bot > want_bot:
                problems.append(
                    f"{rel} column {col}: ends at row {bot}, below row "
                    f"{want_bot} -- the station would hang over the tile below")
    return checked


def check_fire_strips(problems):
    """Four animation frames, all drawn, all different, all inside the mouth."""
    checked = 0
    for rel, (want_top, want_bot) in sorted(FIRE_STRIP.items()):
        path = os.path.join(RES, rel)
        if not os.path.exists(path):
            problems.append(f"{rel}: missing")
            continue
        im = Image.open(path).convert("RGBA")
        strip_y = im.height - 32
        px = im.load()
        frames = []
        for f in range(4):
            checked += 1
            top, bot = cell_extent(px, im.height, f * 32, f * 32 + 32, strip_y)
            if top is None:
                problems.append(
                    f"{rel} fire frame {f}: empty -- the forge would flicker "
                    f"to nothing every fourth frame")
                frames.append(None)
                continue
            top -= strip_y
            bot -= strip_y
            if top < want_top or bot > want_bot:
                problems.append(
                    f"{rel} fire frame {f}: rows {top}..{bot} of its cell, "
                    f"expected inside {want_top}..{want_bot} -- the fire is "
                    f"drawn at drawY while the body is at drawY - 32, so "
                    f"anything outside that band burns on the masonry")
            frames.append(bytes(bytearray(
                b for y in range(strip_y, im.height)
                for x in range(f * 32, f * 32 + 32) for b in px[x, y])))
        for a in range(4):
            for b in range(a + 1, 4):
                checked += 1
                if frames[a] is not None and frames[a] == frames[b]:
                    problems.append(
                        f"{rel} fire frames {a} and {b} are identical -- the "
                        f"animation would visibly stall")
    return checked


def check_lit_pairs(problems):
    """The optional _on sheet exists, matches, and actually looks different."""
    checked = 0
    for cold_rel, lit_rel in LIT_PAIRS:
        checked += 1
        cold_path = os.path.join(RES, cold_rel)
        lit_path = os.path.join(RES, lit_rel)
        if not os.path.exists(lit_path):
            problems.append(
                f"{lit_rel}: missing -- fromFileRaw would fall back to "
                f"{cold_rel} and the station would never look lit")
            continue
        cold = Image.open(cold_path).convert("RGBA")
        lit = Image.open(lit_path).convert("RGBA")
        if cold.size != lit.size:
            problems.append(f"{lit_rel}: {lit.size}, must match {cold_rel} {cold.size}")
        elif cold.tobytes() == lit.tobytes():
            problems.append(
                f"{lit_rel}: identical to {cold_rel} -- a lit sheet that looks "
                f"exactly like the cold one is not a lit sheet")
    return checked


def main():
    problems = []
    checked = 0
    for rel in WALL_SHEETS:
        path = os.path.join(RES, rel)
        if not os.path.exists(path):
            problems.append(f"{rel}: missing")
            continue
        im = Image.open(path).convert("RGBA")
        if im.size != (352, 128):
            problems.append(f"{rel}: {im.size}, expected (352, 128)")
            continue
        px = im.load()
        # The north-south view is the wall's ROOF, seen from directly above, and
        # a roof has no holes: vanilla's rows 0 and 1 are 512/512 opaque. Ours
        # once drew a front-facing pane here instead, which is what the player
        # meant by "das Fenster links am Block zeigt weiterhin nach unten" --
        # the window faced the camera instead of lying flat.
        for row in (0, 1):
            checked += 1
            opaque = sum(1 for y in range(row * 16, row * 16 + 16)
                         for x in range(*WINDOW_STRIP_X) if px[x, y][3] > 0)
            if opaque != 512:
                problems.append(
                    f"{rel} window row {row}: {opaque}/512 opaque, expected 512 -- "
                    f"this is the wall seen from ABOVE (getWindowDir 1, a "
                    f"north-south wall), so it must be solid roof")

        # The east-west view is the wall's FRONT, and its opening must be a
        # hole you can see the ground through, not a solid pane. Vanilla leaves
        # it fully transparent; ours tints it, so require some non-opaque
        # pixels rather than a specific count.
        see_through = sum(1 for row in (5, 6)
                          for y in range(row * 16, row * 16 + 16)
                          for x in range(*WINDOW_STRIP_X) if px[x, y][3] < 250)
        checked += 1
        if see_through < 40:
            problems.append(
                f"{rel} window rows 5-6: only {see_through} non-opaque pixels -- "
                f"the east-west window is a hole in the wall's face and has to "
                f"be see-through, or it reads as a pane stuck on the wall")

        for row in WINDOW_MUST_BE_EMPTY:
            checked += 1
            opaque = sum(1 for y in range(row * 16, row * 16 + 16)
                         for x in range(*WINDOW_STRIP_X) if px[x, y][3] > 0)
            if opaque:
                drawn_at = {2: -64, 3: -48, 4: -32}[row]
                problems.append(
                    f"{rel} window row {row}: {opaque}/512 opaque, expected empty -- "
                    f"it is drawn at drawY{drawn_at}, so the window would stand "
                    f"{-drawn_at}px above its own tile against a 16px wall")

        for cell, (want_top, want_bot) in DOOR_CELLS.items():
            top, bot = cell_extent(px, 128, cell * 32, cell * 32 + 32)
            checked += 1
            if top is None:
                problems.append(f"{rel} cell {cell}: empty, expected art ending at y{want_bot}")
            elif top < want_top:
                problems.append(
                    f"{rel} cell {cell}: starts at y{top}, {want_top - top}px above the "
                    f"tallest vanilla art in this cell (y{want_top}) -- that much would "
                    f"draw off the top of the wall")
            elif bot != want_bot:
                problems.append(
                    f"{rel} cell {cell}: ends at y{bot}, expected y{want_bot} -- the door "
                    f"must sit on the bottom of its cell")

    rotations = check_rotation_sheets(problems)
    stations = check_station_sheets(problems)
    fires = check_fire_strips(problems)
    lits = check_lit_pairs(problems)

    for p in problems:
        print(f"FIX  {p}")
    if problems:
        print(f"\n{len(problems)} sheet cell(s) out of format.")
        return 1
    print(f"OK: {checked} wall-sheet door and window cells, {rotations} furniture "
          f"rotation columns, {stations} workstation rotation columns, {fires} "
          f"fire-strip checks and {lits} lit-sheet pair(s) sit at the extents "
          f"the engine draws them at.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
