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


def cell_extent(px, height, x0, x1):
    rows = [y for y in range(height)
            if any(px[x, y][3] > 0 for x in range(x0, x1))]
    return (min(rows), max(rows)) if rows else (None, None)


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

    for p in problems:
        print(f"FIX  {p}")
    if problems:
        print(f"\n{len(problems)} sheet cell(s) out of format.")
        return 1
    print(f"OK: {checked} wall-sheet door and window cells sit at the extents "
          f"the engine draws them at.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
