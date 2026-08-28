#!/usr/bin/env python3
"""Rotation contact sheet: every cell the engine reads, drawn where it lands.

`rotation_variety_audit.py` proves the cells are not duplicates of each other.
It cannot answer the question a player actually asks -- "does turning this look
right" -- because that is a judgement about pictures. This renders them.

For every piece whose renderer reads more than one cell, this lays the cells out
side by side over a 32px tile grid, with the engine's own vertical offset
applied where the repository has recorded one:

  PaintingObject      rot 0 at drawY+8, rot 2 at drawY-32, rot 1/3 at drawY
                      (docs/research/structures-furniture.md 3.7). A wall block
                      is drawn on the side the rotation names, so "is the banner
                      ON the wall" is visible rather than inferred.
  FenceGateObject     col 2 drawn TWICE at drawY-14 and drawY+14, col 3 at
                      drawY+14 (rotation 3 only), col 4 at drawY-14, col 5 at
                      (drawX-16, drawY+14) (docs/TECHNICAL_LEARNINGS.md).
  Rotation sheets     bookshelf/cabinet at drawY-height+64, clock and the
                      crafting stations at drawY-height+32, display stand and
                      the processing stations at drawY-(height-32).

Where no anchor is recorded -- the wall lights and the streetlamps -- the cells
are drawn at drawY and the strip says so, so nobody reads a guess as a fact.

Doors are NOT here: `tools/wall_render_preview.py` already composes them into
real scenes with vanilla underneath, which is strictly the better picture.

Usage:  python3 tools/rotation_preview.py [--out build/qa] [--zoom 4]
"""
import argparse
import os

from PIL import Image, ImageDraw

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")

BG = (26, 28, 36, 255)
GRID = (44, 48, 60, 255)
WALLBLOCK = (78, 82, 98, 255)
FG = (222, 228, 238, 255)
DIM = (140, 148, 164, 255)


def sheet(rel):
    path = os.path.join(RES, rel)
    return Image.open(path).convert("RGBA") if os.path.exists(path) else None


def stage(w_tiles, h_tiles):
    """A tile-gridded backdrop, w_tiles x h_tiles of 32px."""
    img = Image.new("RGBA", (w_tiles * 32, h_tiles * 32), BG)
    d = ImageDraw.Draw(img)
    for x in range(0, img.width + 1, 32):
        d.line([(x, 0), (x, img.height)], fill=GRID)
    for y in range(0, img.height + 1, 32):
        d.line([(0, y), (img.width, y)], fill=GRID)
    return img


def cell_strip(title, cells, note=""):
    """cells: list of (label, image, paste_x, paste_y, tiles_wide, tiles_high,
    wall_rect or None). Each entry gets its own tile-gridded stage, and the
    column is as wide as the wider of stage and label -- a 96px stage under a
    140px label is how the first version made every caption unreadable."""
    pads = []
    for label, im, px, py, tw, th, wall in cells:
        st = stage(tw, th)
        if wall:
            ImageDraw.Draw(st).rectangle(wall, fill=WALLBLOCK)
        st.alpha_composite(im, (px, py))
        pads.append((label, st))
    probe = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
    widths = [max(st.width, int(probe.textlength(label)) + 6) for label, st in pads]
    gap, lab_h = 12, 12
    W = sum(widths) + gap * (len(pads) + 1)
    H = max(p[1].height for p in pads) + lab_h * 2 + gap + (lab_h if note else 0)
    out = Image.new("RGBA", (W, H), BG)
    d = ImageDraw.Draw(out)
    d.text((gap, 2), title, fill=FG)
    x = gap
    for (label, st), cw in zip(pads, widths):
        out.alpha_composite(st, (x + (cw - st.width) // 2, lab_h + 4))
        d.text((x, lab_h + 6 + st.height), label, fill=DIM)
        x += cw + gap
    if note:
        d.text((gap, H - lab_h), note, fill=DIM)
    return out


def painting_strip(rel):
    """PaintingObject: row = rotation = where the wall is; engine offsets +8/-32.

    The object's own tile is the CENTRE of a 3x3 stage and the wall block goes
    on the neighbour the rotation names, so the two things a reader has to
    judge -- "is this the right view" and "does it land on that wall" -- are
    both in the frame.
    """
    im = sheet(rel)
    if im is None or im.size != (32, 128):
        return None
    # (row, label, dy, wall rectangle around the 32..63 centre tile)
    plan = [
        (0, "rot0  wall below  +8", 8, (32, 64, 63, 95)),
        (1, "rot1  wall left", 0, (0, 32, 31, 63)),
        (2, "rot2  wall above  -32", -32, (32, 0, 63, 31)),
        (3, "rot3  wall right", 0, (64, 32, 95, 63)),
    ]
    cells = []
    for row, label, dy, wall in plan:
        cell = im.crop((0, row * 32, 32, row * 32 + 32))
        cells.append((label, cell, 32, 32 + dy, 3, 3, wall))
    return cell_strip("%s  -- PaintingObject, one view per wall" % rel, cells,
                      "grey block = the wall the rotation names; the banner must "
                      "read as hanging on it")


def wall_light_strip(rel):
    im = sheet(rel)
    if im is None or im.size != (64, 128):
        return None
    names = ("wall above", "wall right", "support below", "wall left")
    cells = []
    for col, state in ((0, "lit"), (1, "unlit")):
        for row in range(4):
            cell = im.crop((col * 32, row * 32, col * 32 + 32, row * 32 + 32))
            cells.append(("%s / %s" % (state, names[row]), cell, 32, 32, 3, 3, None))
    return cell_strip("%s  -- WallTorchObject, sprite(state, orientation)" % rel,
                      cells, "no draw offset recorded for this class: cells shown "
                             "at drawY, do not read placement off this strip")


def streetlamp_strip(rel):
    im = sheet(rel)
    if im is None or im.size != (32, 192):
        return None
    cells = [("lit", im.crop((0, 0, 32, 96)), 32, 0, 3, 3, None),
             ("unlit", im.crop((0, 96, 32, 192)), 32, 0, 3, 3, None)]
    return cell_strip("%s  -- StreetlampObject, row-selected on/off" % rel, cells)


def gate_strip(rel):
    """FenceGateObject's six columns, drawn at the offsets the engine uses."""
    im = sheet(rel)
    if im is None or im.size != (192, 64):
        return None
    col = [im.crop((c * 32, 0, c * 32 + 32, 64)) for c in range(6)]
    stages = []

    def horiz(label, c):
        st = stage(3, 3)
        st.alpha_composite(col[c], (32, 32))
        stages.append((label, st))

    horiz("closed, east-west (col 1)", 1)
    horiz("open, east-west (col 0)", 0)

    st = stage(3, 3)                       # north-south closed: post x2 + leaf
    st.alpha_composite(col[2], (32, 32 - 14))
    st.alpha_composite(col[2], (32, 32 + 14))
    st.alpha_composite(col[4], (32, 32 - 14))
    stages.append(("closed, north-south (2,2,4)", st))

    st = stage(3, 3)                       # north-south open
    st.alpha_composite(col[2], (32, 32 - 14))
    st.alpha_composite(col[2], (32, 32 + 14))
    st.alpha_composite(col[5], (32 - 16, 32 + 14))
    stages.append(("open, north-south (2,2,5)", st))

    st = stage(3, 3)                       # rotation 3 adds the latch
    st.alpha_composite(col[2], (32, 32 - 14))
    st.alpha_composite(col[2], (32, 32 + 14))
    st.alpha_composite(col[3], (32, 32 + 14))
    stages.append(("latch, rot 3 only (col 3)", st))

    probe = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
    widths = [max(st.width, int(probe.textlength(label)) + 6) for label, st in stages]
    gap, lab_h = 12, 12
    W = sum(widths) + gap * (len(stages) + 1)
    H = max(s[1].height for s in stages) + lab_h * 2 + gap
    out = Image.new("RGBA", (W, H), BG)
    d = ImageDraw.Draw(out)
    d.text((gap, 2), "%s  -- FenceGateObject, col 2 is drawn TWICE" % rel, fill=FG)
    x = gap
    for (label, st), cw in zip(stages, widths):
        out.alpha_composite(st, (x + (cw - st.width) // 2, lab_h + 4))
        d.text((x, lab_h + 6 + st.height), label, fill=DIM)
        x += cw + gap
    return out


# Rotation-column pieces: file -> the class's vertical anchor, as a lambda of
# the sheet height. Straight out of sheet_format_audit's table.
ROTATION_ANCHORS = {
    "objects/skywatchbookshelf.png": lambda h: -h + 64,
    "objects/skywatchcabinet.png": lambda h: -h + 64,
    "objects/skywatchclock.png": lambda h: -h + 32,
    "objects/skywatchdisplay.png": lambda h: -(h - 32),
    "objects/windsilkloom.png": lambda h: -(h - 32),
    "objects/stormglasskiln.png": lambda h: -(h - 32),
    "objects/aetherforge.png": lambda h: -(h - 32) - 32,
}
DIRS = ("rot 0  faces up", "rot 1  faces right", "rot 2  faces down",
        "rot 3  faces left")


def rotation_strip(rel, anchor):
    im = sheet(rel)
    if im is None:
        return None
    body_h = im.height - 32 if rel.endswith("aetherforge.png") else im.height
    dy = anchor(body_h)
    tiles_high = max(3, (body_h + 63) // 32 + 1)
    base_y = (tiles_high - 1) * 32
    cells = []
    for c in range(4):
        cell = im.crop((c * 32, 0, c * 32 + 32, body_h))
        cells.append((DIRS[c], cell, 32, base_y + dy, 3, tiles_high, None))
    return cell_strip("%s  -- four rotation columns at drawY%+d" % (rel, dy), cells)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--out", default=os.path.join(REPO, "build", "qa", "rotations"))
    ap.add_argument("--zoom", type=int, default=4)
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)

    jobs = [("skywatchbanner", painting_strip("objects/skywatchbanner.png")),
            ("mistglasslantern", wall_light_strip("objects/mistglasslantern.png")),
            ("flickerlightgarland", wall_light_strip("objects/flickerlightgarland.png")),
            ("wardencandelabra", streetlamp_strip("objects/wardencandelabra.png")),
            ("ghostlantern", streetlamp_strip("objects/ghostlantern.png")),
            ("skyironfencegate", gate_strip("objects/skyironfencegate.png")),
            ("cloudmarblefencegate", gate_strip("objects/cloudmarblefencegate.png"))]
    for rel, anchor in sorted(ROTATION_ANCHORS.items()):
        jobs.append((os.path.basename(rel)[:-4], rotation_strip(rel, anchor)))

    written = 0
    for name, img in jobs:
        if img is None:
            print("  skipped %s (missing or wrong size)" % name)
            continue
        img = img.resize((img.width * args.zoom, img.height * args.zoom), Image.NEAREST)
        img.save(os.path.join(args.out, "%s.png" % name))
        written += 1
    print("%d rotation contact sheets in %s -- now LOOK at them." % (written, args.out))


if __name__ == "__main__":
    main()
