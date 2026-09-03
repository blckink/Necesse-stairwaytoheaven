#!/usr/bin/env python3
"""Paint a wall the way it LOOKS. The 352x128 sheet is sliced out of it.

The old way round is why wall sheets are miserable to draw: the sheet is an
atlas of 32 sixteen-pixel cells whose meaning is a nest of neighbour
conditions, several cells appear in TWO different screen bands, and the same
few pixels have to be copied into four places by hand so the seams meet.

This turns it round. You paint whole walls -- a block, a corner, a junction --
as one continuous picture, exactly as they appear in game. The tool then cuts
every cell out of that picture at the position the ENGINE would have drawn it,
because the position comes from `wall_render_preview.WallRenderer.wall_cells`,
a verified port of `WallObject.addWallDrawOptions`. Nothing here is guessed and
nothing is hand-placed.

    python3 tools/wall_from_layout.py --new mywall
        -> build/qa/wallpaint/mywall-paint.png   the canvas, transparent
           build/qa/wallpaint/mywall-guide.png   the same canvas, annotated

    (paint mywall-paint.png -- one picture per shape, no cell thinking)

    python3 tools/wall_from_layout.py build/qa/wallpaint/mywall-paint.png \
            -o src/main/resources/objects/mywall.png
        -> the finished 352x128 sheet

A cell that several shapes produce is checked: if two shapes disagree about
what it looks like, the tool says which cell and refuses to guess. That
disagreement IS the seam bug you would otherwise find in game.

Verify the result the usual way:
    python3 tools/wall_render_preview.py --sheet src/main/resources/objects/mywall.png
"""
import argparse
import collections
import os
import sys

from PIL import Image, ImageDraw, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO, "tools"))
os.environ.setdefault("NECESSE_SPRITES", os.path.join(REPO, "vanilla-sprites"))
import wall_render_preview as wrp  # noqa: E402

OUT = os.path.join(REPO, "build", "qa", "wallpaint")
RES_OBJ = os.path.join(REPO, "src", "main", "resources", "objects")
T = 32                      # a tile is 32x32 of screen
PAD = 48                    # room around a shape for the bands it reaches into
GAP = 40                    # between shapes

# The shapes you paint. Each is an ASCII picture in Scene's own language:
# '#' our wall, 'X' a FOREIGN wall (a different material butting against ours),
# 'O' a window, 'D' a door. Chosen so that between them they produce all 32
# body cells -- the tool asserts that, so a shape cannot quietly stop covering
# something.
SHAPES = [
    ("block", "A solid run: the cap, the roof band and the front face.",
     ["####",
      "####",
      "####"]),
    ("notch", "A plus. Inner corners, and every step where the diagonal is empty.",
     [".##.",
      "####",
      ".##."]),
    ("stub", "A single tile and a lone column: every end at once.",
     ["#.#",
      "..#",
      "..#"]),
    ("junction", "Our wall against a DIFFERENT material (X), beside and below it.",
     ["#####",
      "X###X",
      "#####"]),
]


# The window and the doors have no neighbour logic worth deriving: the window
# is two fixed columns and the door is four rotations x (closed, open). They
# get plain labelled slots rather than a painted scene.
WINDOW_NS = [(4, 0, 0, -16), (5, 0, 16, -16), (4, 1, 0, 0), (5, 1, 16, 0)]
WINDOW_EW = [(c, r, dx, dy) for (c, r, dx, dy) in wrp.WallRenderer(None).window_cells(
    [False] * 8)]
DOOR_LABELS = ["rot0 closed", "rot0 open", "rot1 closed", "rot1 open",
               "rot2 closed", "rot2 open", "rot3 closed", "rot3 open"]


def scene_cells(rows):
    """[(col, row, screen_x, screen_y)] for every cell this shape produces."""
    scene = wrp.Scene("s", rows)
    r = wrp.WallRenderer(None)
    out = []
    for ty in range(scene.h):
        for tx in range(scene.w):
            if scene.at(tx, ty) != "#":
                continue
            adj = [scene.is_wall(tx + dx, ty + dy) for dx, dy in wrp.ADJ]
            is_wall = [scene.at(tx + dx, ty + dy) in "#OX" for dx, dy in wrp.ADJ]
            for col, row, dx, dy in r.wall_cells(adj, False, False, is_wall):
                out.append((col, row, tx * T + dx, ty * T + dy))
    return out


def shape_box(cells):
    xs = [x for _, _, x, _ in cells]
    ys = [y for _, _, _, y in cells]
    return min(xs), min(ys), max(xs) + 16, max(ys) + 16


def plan():
    """Where every shape and slot sits on the canvas, and what it covers."""
    placed, x = [], PAD
    for name, note, rows in SHAPES:
        cells = scene_cells(rows)
        x0, y0, x1, y1 = shape_box(cells)
        placed.append({"name": name, "note": note, "rows": rows, "cells": cells,
                       "ox": x - x0, "oy": PAD - y0, "w": x1 - x0, "h": y1 - y0})
        x += (x1 - x0) + GAP
    return placed, x


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("paint", nargs="?", help="the painted canvas to slice")
    ap.add_argument("--new", metavar="NAME", help="write a fresh canvas to paint")
    ap.add_argument("--from-sheet", metavar="PNG",
                    help="fill the canvas from an existing 352x128 sheet, so you "
                         "paint OVER a working wall instead of from nothing")
    ap.add_argument("-o", "--out", help="where the 352x128 sheet goes")
    ap.add_argument("--selftest", action="store_true",
                    help="seed a canvas from every wall sheet we have, slice it "
                         "straight back, and assert the result is byte-identical")
    args = ap.parse_args()

    placed, canvas_w = plan()
    door_y = PAD + max(p["h"] for p in placed) + GAP + 24
    win_x = PAD
    win_y = door_y + 128 + GAP + 24
    canvas_w = max(canvas_w, PAD + 8 * (32 + 12), 900)
    canvas_h = win_y + 128 + PAD

    covered = collections.Counter()
    for p in placed:
        for col, row, _, _ in p["cells"]:
            covered[(col, row)] += 1
    missing = [(c, r) for r in range(8) for c in range(4) if (c, r) not in covered]
    if missing:
        print("BUG: these body cells are produced by no shape: %s" % missing)
        return 2

    if args.selftest:
        import glob
        bad = 0
        sheets = sorted(glob.glob(os.path.join(REPO, "vanilla-sprites", "objects", "*wall.png"))
                        + glob.glob(os.path.join(RES_OBJ, "*wall.png")))
        for sp in sheets:
            seed = Image.open(sp).convert("RGBA")
            if seed.size != (352, 128):
                continue
            buf = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
            for p2 in placed:
                for col, row, sx, sy in p2["cells"]:
                    buf.paste(seed.crop((col * 16, row * 16, col * 16 + 16, row * 16 + 16)),
                              (p2["ox"] + sx, p2["oy"] + sy))
            out = Image.new("RGBA", (352, 128), (0, 0, 0, 0))
            for p2 in placed:
                for col, row, sx, sy in p2["cells"]:
                    out.paste(buf.crop((p2["ox"] + sx, p2["oy"] + sy,
                                        p2["ox"] + sx + 16, p2["oy"] + sy + 16)),
                              (col * 16, row * 16))
            ref = Image.new("RGBA", (352, 128), (0, 0, 0, 0))
            for r in range(8):
                for c in range(4):
                    ref.paste(seed.crop((c * 16, r * 16, c * 16 + 16, r * 16 + 16)),
                              (c * 16, r * 16))
            ok = out.tobytes() == ref.tobytes()
            bad += 0 if ok else 1
            print("  %-28s %s" % (os.path.basename(sp), "OK" if ok else "MISMATCH"))
        print("\n%d sheet(s) checked, %d mismatch(es)." % (len(sheets), bad))
        return 1 if bad else 0

    if args.new:
        os.makedirs(OUT, exist_ok=True)
        paint = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
        seed = Image.open(args.from_sheet).convert("RGBA") if args.from_sheet else None
        if seed is not None:
            # Forward pass: put every cell where the engine would draw it. This
            # is the exact inverse of the slice below, so a canvas seeded this
            # way and sliced straight back reproduces the sheet byte for byte --
            # which is how this tool is tested.
            for p2 in placed:
                for col, row, sx, sy in p2["cells"]:
                    paint.paste(seed.crop((col * 16, row * 16, col * 16 + 16, row * 16 + 16)),
                                (p2["ox"] + sx, p2["oy"] + sy))
            for i in range(8):
                paint.paste(seed.crop(((3 + i) * 32, 0, (4 + i) * 32, 128)),
                            (PAD + i * (32 + 12), door_y))
            for j, cells in enumerate((WINDOW_NS, WINDOW_EW)):
                bx = win_x + j * (32 + 40)
                for col, row, dx, dy in cells:
                    paint.paste(seed.crop((col * 16, row * 16, col * 16 + 16, row * 16 + 16)),
                                (bx + dx, win_y + 64 + dy))
        guide = Image.new("RGBA", (canvas_w, canvas_h), (26, 28, 36, 255))
        d = ImageDraw.Draw(guide)
        try:
            font = ImageFont.truetype("DejaVuSansMono.ttf", 11)
        except OSError:
            font = ImageFont.load_default()

        for p in placed:
            d.text((p["ox"] + shape_box(p["cells"])[0], PAD - 26),
                   "%s  -- %s" % (p["name"], p["note"]), fill=(150, 200, 255), font=font)
            for col, row, sx, sy in p["cells"]:
                bx, by = p["ox"] + sx, p["oy"] + sy
                d.rectangle([bx, by, bx + 15, by + 15], outline=(70, 90, 120))
                d.text((bx + 1, by + 3), "%d%d" % (col, row), fill=(120, 150, 190), font=font)

        d.text((PAD, door_y - 20), "DOORS -- 8 slots of 32x128. Paint each as a whole "
               "door; rot0/rot2 get MIRRORED, so they must read both ways.",
               fill=(255, 220, 110), font=font)
        for i, lab in enumerate(DOOR_LABELS):
            bx = PAD + i * (32 + 12)
            d.rectangle([bx, door_y, bx + 31, door_y + 127], outline=(200, 170, 70))
            d.text((bx, door_y + 130), lab, fill=(230, 200, 120), font=font)

        d.text((win_x, win_y - 20), "WINDOW -- left column: N-S wall (top surface seen "
               "from ABOVE). right column: E-W wall (96px opening seen head-on).",
               fill=(220, 140, 255), font=font)
        for j, cells in enumerate((WINDOW_NS, WINDOW_EW)):
            bx = win_x + j * (32 + 40)
            for col, row, dx, dy in cells:
                by = win_y + 64 + dy
                d.rectangle([bx + dx, by, bx + dx + 15, by + 15], outline=(180, 110, 210))
                d.text((bx + dx + 1, by + 3), "%d%d" % (col, row),
                       fill=(200, 150, 230), font=font)

        pp = os.path.join(OUT, args.new + "-paint.png")
        gp = os.path.join(OUT, args.new + "-guide.png")
        paint.save(pp)
        guide.save(gp)
        print("paint this : %s  (%dx%d, transparent)" % (os.path.relpath(pp, REPO),
                                                         canvas_w, canvas_h))
        print("look at    : %s" % os.path.relpath(gp, REPO))
        print("\n%d body cells covered by %d shapes; none missing."
              % (len(covered), len(placed)))
        for (c, r), n in sorted(covered.items()):
            if n > 1:
                pass
        print("Then:  python3 tools/wall_from_layout.py %s -o objects/<name>.png"
              % os.path.relpath(pp, REPO))
        return 0

    if not args.paint:
        ap.print_help()
        return 2

    src = Image.open(args.paint).convert("RGBA")
    sheet = Image.new("RGBA", (352, 128), (0, 0, 0, 0))
    seen, clashes = {}, []
    for p in placed:
        for col, row, sx, sy in p["cells"]:
            box = (p["ox"] + sx, p["oy"] + sy, p["ox"] + sx + 16, p["oy"] + sy + 16)
            cell = src.crop(box)
            key = (col, row)
            if key in seen and seen[key][0].tobytes() != cell.tobytes():
                clashes.append((key, seen[key][1], p["name"]))
                continue
            seen[key] = (cell, p["name"])
            sheet.paste(cell, (col * 16, row * 16))

    for i in range(8):
        bx = PAD + i * (32 + 12)
        sheet.paste(src.crop((bx, door_y, bx + 32, door_y + 128)), ((3 + i) * 32, 0))
    for j, cells in enumerate((WINDOW_NS, WINDOW_EW)):
        bx = win_x + j * (32 + 40)
        for col, row, dx, dy in cells:
            by = win_y + 64 + dy
            sheet.paste(src.crop((bx + dx, by, bx + dx + 16, by + 16)),
                        (col * 16, row * 16))

    if clashes:
        print("%d cell(s) painted DIFFERENTLY by two shapes -- that is a seam that "
              "would not meet in game:" % len(clashes))
        for (c, r), a, b in clashes:
            print("  cell (%d,%d): '%s' and '%s' disagree" % (c, r, a, b))
        return 1

    dest = args.out or os.path.join(OUT, "sheet.png")
    if not os.path.isabs(dest):
        cand = os.path.join(REPO, "src", "main", "resources", dest)
        dest = cand if os.path.isdir(os.path.dirname(cand)) else os.path.join(REPO, dest)
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    sheet.save(dest)
    print("wrote %s  (352x128, %d body cells + 8 doors + the window)"
          % (os.path.relpath(dest, REPO), len(seen)))
    print("check it:  python3 tools/wall_render_preview.py --sheet %s"
          % os.path.relpath(dest, REPO))
    return 0


if __name__ == "__main__":
    sys.exit(main())
