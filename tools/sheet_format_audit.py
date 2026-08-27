#!/usr/bin/env python3
"""Sheet-format audit: assert layout invariants the size audit cannot see.

The size audit compares opaque MASS against a vanilla analogue. That catches
sprites that read too thin, but it is blind to geometry -- and some sheets are
addressed by the engine at fixed offsets, so a cell painted in the wrong place
renders wrong no matter how much ink is in it. A door drawn over its whole cell
scores BETTER on mass while rendering three tiles too tall.

Checks:

  Wall sheets (352x128). WallDoorObject/WallDoorOpenObject draw eight 32x128
  cells at pos(drawX, drawY - 96), so sheet row 96 is the tile's top edge and
  anything above it sticks out over the wall. The expected top rows below were
  measured off vanilla stonewall.png; a wall segment itself only rises 16px
  above its tile, so a door whose cell starts near row 0 towers over the wall
  it sits in. Bottom row is always 127.

Usage:  python3 tools/sheet_format_audit.py
Exit code 1 if anything is out of format.
"""
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")

WALL_SHEETS = ("objects/skystonebrickwall.png", "objects/nightfellwall.png")

# 32-cell index -> (top row, bottom row). Cells 0-2 are the 16px auto-tile blob
# and the window insert, which do use the full height.
DOOR_CELLS = {
    3: (88, 127),   # rot 0 closed, head-on
    4: (68, 127),   # rot 0 open, leaf edge-on
    5: (70, 127),   # rot 1 closed, edge-on
    6: (68, 127),   # rot 1 open, leaf head-on (swung north)
    7: (88, 127),   # rot 2 closed, head-on
    8: (68, 127),   # rot 2 open, leaf edge-on
    9: (70, 127),   # rot 3 closed, edge-on
    10: (90, 127),  # rot 3 open, leaf head-on (swung south)
}


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
        for cell, (want_top, want_bot) in DOOR_CELLS.items():
            top, bot = cell_extent(px, 128, cell * 32, cell * 32 + 32)
            checked += 1
            if top is None:
                problems.append(f"{rel} cell {cell}: empty, expected y{want_top}..{want_bot}")
            elif (top, bot) != (want_top, want_bot):
                over = want_top - top
                extra = f" ({over}px too tall)" if over > 0 else ""
                problems.append(
                    f"{rel} cell {cell}: y{top}..{bot}, expected y{want_top}..{want_bot}{extra}")

    for p in problems:
        print(f"FIX  {p}")
    if problems:
        print(f"\n{len(problems)} sheet cell(s) out of format.")
        return 1
    print(f"OK: {checked} wall-sheet door cells sit at the extents the engine draws them at.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
