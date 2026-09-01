#!/usr/bin/env python3
"""The annotated wall-sheet template: which region of the 352x128 is WHAT.

Made for hand-drawing wall sets outside this repo. The player draws in an
editor over vanilla stonewall and cannot see any of the renderer's rules --
which columns are tile HALVES, that the half mapping SWAPS between row groups,
that two of the window rows are a ROOF, that six cells are dead. Every one of
those has shipped as a bug at least once (docs/PLAYTEST_LOG.md), so this
writes the rules ONTO the template:

    build/qa/wall-template-map.png      annotated 4x map over vanilla stonewall
    docs/references/wall-template-map.png   the committed copy

Region knowledge is the same port wall_render_preview.py carries (decoded from
WallObject/WallWindowObject/WallDoorObject in the 1.3.2 decompile); this file
only draws it.

Usage:
    python3 tools/wall_template_map.py [--sheet path/to/wall.png]
"""
import argparse
import os

from PIL import Image, ImageDraw, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VANILLA = os.path.join(REPO, "vanilla-sprites", "objects", "stonewall.png")

Z = 4          # zoom factor of the map
C = 16         # engine cell size in the body/window blocks

# Role colours (overlay tints, drawn translucent over the art)
CO_LEFT = (60, 160, 255)      # left half of a tile
CO_RIGHT = (255, 170, 60)     # right half of a tile
CO_ROOF = (200, 70, 220)      # top-down roof view
CO_DEAD = (120, 120, 120)     # unreachable cells
CO_CORNER = (90, 220, 120)    # inner-corner hook pieces
CO_WARN = (235, 60, 60)

# The body blob's half mapping, straight from WallObject.addWallDrawOptions:
# rows 0,3,4: col0=left/closed col1=RIGHT/open col2=LEFT/open col3=right/closed
# rows 1,2:   col0=left/closed col1=LEFT/open  col2=RIGHT/open col3=right/closed
HALF_BY_ROWGROUP = {
    "A": {0: ("L", "closed"), 1: ("R", "open"), 2: ("L", "open"), 3: ("R", "closed")},
    "B": {0: ("L", "closed"), 1: ("L", "open"), 2: ("R", "open"), 3: ("R", "closed")},
}
ROWGROUP = {0: "A", 1: "B", 2: "B", 3: "A", 4: "A"}

ROW_LABEL = {
    0: "row 0  TOP CAP (north rim, 16px, drawn one band ABOVE the tile)",
    1: "row 1  roof band LOWER  } repeat DOWN a run: (row2,row1) per tile",
    2: "row 2  roof band UPPER  } row2 top edge must meet row1 bottom edge",
    3: "row 3  front face TOP half (the rim you see head-on)",
    4: "row 4  front face BOTTOM half (foot shadow; meets the ground)",
    5: "row 5  cols 0-1 inner-corner mids; cols 2-3 DEAD",
    6: "row 6  cols 0-1 inner-corner bottoms; cols 2-3 DEAD",
    7: "row 7  cols 0-1 inner-corner top hooks; cols 2-3 diagonal mids",
}

DEAD_CELLS = {(2, 5), (3, 5), (2, 6), (3, 6)}
CORNER_CELLS = {(0, 5), (1, 5), (0, 6), (1, 6), (0, 7), (1, 7), (2, 7), (3, 7)}


def tint(img, box, colour, alpha=64):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.rectangle(box, fill=colour + (alpha,))
    img.alpha_composite(layer)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sheet", default=VANILLA,
                    help="wall sheet to annotate (default: vanilla stonewall)")
    ap.add_argument("--out", default=os.path.join(REPO, "build", "qa",
                                                  "wall-template-map.png"))
    args = ap.parse_args()

    sheet = Image.open(args.sheet).convert("RGBA")
    if sheet.size != (352, 128):
        raise SystemExit("%s is %s, a wall sheet is (352, 128)" % (args.sheet, sheet.size))

    # Canvas: the sheet at 4x on top, a legend column on the right, the rule
    # text underneath. Checker backdrop so alpha reads.
    art_w, art_h = 352 * Z, 128 * Z
    legend_w = 620
    text_h = 560
    img = Image.new("RGBA", (art_w + legend_w, art_h + text_h), (24, 24, 30, 255))
    checker = Image.new("RGBA", (art_w, art_h), (46, 46, 56, 255))
    cd = ImageDraw.Draw(checker)
    for y in range(0, art_h, 16):
        for x in range(0, art_w, 16):
            if ((x + y) // 16) % 2 == 0:
                cd.rectangle((x, y, x + 15, y + 15), fill=(58, 58, 70, 255))
    img.alpha_composite(checker, (0, 0))
    img.alpha_composite(sheet.resize((art_w, art_h), Image.NEAREST), (0, 0))

    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype(
            "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf", 15)
        font_big = ImageFont.truetype(
            "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf", 18)
    except OSError:
        font = ImageFont.load_default()
        font_big = font

    # --- body blob 0..64px: per-cell half/variant roles ---------------------
    for row in range(8):
        for col in range(4):
            x0, y0 = col * C * Z, row * C * Z
            box = (x0, y0, x0 + C * Z - 1, y0 + C * Z - 1)
            if (col, row) in DEAD_CELLS:
                tint(img, box, CO_DEAD, 110)
                d.text((x0 + 6, y0 + 20), "DEAD", fill=(230, 230, 230), font=font)
                continue
            if (col, row) in CORNER_CELLS:
                tint(img, box, CO_CORNER, 46)
                d.text((x0 + 4, y0 + 40), "corner", fill=(210, 250, 220), font=font)
            if row <= 4:
                half, variant = HALF_BY_ROWGROUP[ROWGROUP[row]][col]
                tint(img, box, CO_LEFT if half == "L" else CO_RIGHT, 42)
                d.text((x0 + 5, y0 + 3), half, fill=(255, 255, 255), font=font_big)
                d.text((x0 + 4, y0 + 42), variant, fill=(235, 235, 235), font=font)
            d.rectangle(box, outline=(255, 255, 255, 90))

    # The swap warning across rows 1-2 vs 0,3,4
    d.rectangle((C * Z, 1 * C * Z, 3 * C * Z - 1, 3 * C * Z - 1),
                outline=CO_WARN, width=3)

    # --- window insert 64..96px --------------------------------------------
    wx = 64 * Z
    tint(img, (wx, 0, wx + 32 * Z - 1, 2 * C * Z - 1), CO_ROOF, 70)
    d.rectangle((wx, 0, wx + 32 * Z - 1, 2 * C * Z - 1), outline=CO_ROOF, width=3)
    d.text((wx + 4, 2), "ROOF SLOT", fill=(255, 220, 255), font=font_big)
    d.text((wx + 4, 24), "top-down view!", fill=(255, 220, 255), font=font)
    d.text((wx + 4, 40), "NO pane here", fill=(255, 220, 255), font=font)
    # rows 2-4 must stay empty
    d.rectangle((wx, 2 * C * Z, wx + 32 * Z - 1, 5 * C * Z - 1),
                outline=(255, 255, 255, 120), width=2)
    d.text((wx + 4, 2 * C * Z + 4), "EMPTY", fill=(220, 220, 220), font=font_big)
    d.text((wx + 4, 2 * C * Z + 26), "(transparent,", fill=(200, 200, 200), font=font)
    d.text((wx + 4, 2 * C * Z + 42), "drawn 2 tiles up)", fill=(200, 200, 200), font=font)
    d.rectangle((wx, 5 * C * Z, wx + 32 * Z - 1, 8 * C * Z - 1),
                outline=(120, 200, 255), width=3)
    d.text((wx + 4, 5 * C * Z + 4), "FRONT window", fill=(200, 230, 255), font=font)
    d.text((wx + 4, 5 * C * Z + 20), "(E-W walls)", fill=(200, 230, 255), font=font)

    # --- door strip 96..352px ----------------------------------------------
    door_labels = ("rot0\nclosed", "rot0\nopen", "rot1\nclosed", "rot1\nopen",
                   "rot2\nclosed", "rot2\nopen", "rot3\nclosed", "rot3\nopen")
    for i, lab in enumerate(door_labels):
        x0 = (96 + i * 32) * Z
        d.rectangle((x0, 0, x0 + 32 * Z - 1, art_h - 1),
                    outline=(255, 255, 255, 80), width=1)
        d.text((x0 + 4, 2), lab, fill=(255, 255, 160), font=font)
        # the band the engine actually shows: cells draw at drawY-96, so the
        # tile's own 32px starts at y96; the leaf lives in y~88..128.
        d.rectangle((x0, 88 * Z, x0 + 32 * Z - 1, 128 * Z - 1),
                    outline=(255, 230, 90), width=3)


    # --- legend column ------------------------------------------------------
    lx = art_w + 16
    ly = 10
    d.text((lx, ly), "WALL SHEET 352x128 - REGION MAP", fill=(255, 255, 255),
           font=font_big)
    ly += 34
    for key, colour, txt in (
            ("L", CO_LEFT, "LEFT half of a tile (drawn at drawX)"),
            ("R", CO_RIGHT, "RIGHT half (drawn at drawX+16)"),
            ("", CO_ROOF, "roof seen from ABOVE (N-S window)"),
            ("", CO_CORNER, "inner-corner hook pieces"),
            ("", CO_DEAD, "dead cells - engine never draws them"),
    ):
        d.rectangle((lx, ly, lx + 22, ly + 18), fill=colour + (140,))
        d.text((lx + 30, ly), (key + "  " if key else "") + txt,
               fill=(230, 230, 230), font=font)
        ly += 26
    ly += 10
    d.text((lx, ly), "RED BOX: cols 1+2 SWAP L/R sides in rows 1-2!",
           fill=CO_WARN, font=font_big)
    ly += 24
    d.text((lx, ly), "one picture painted straight across renders mirrored",
           fill=(255, 190, 190), font=font)
    ly += 26
    d.text((lx, ly), "YELLOW BOX on doors: the ~40px the engine shows",
           fill=(255, 230, 90), font=font)
    ly += 22
    d.text((lx, ly), "(leaf + frame); above it the tall frame, mostly hidden",
           fill=(255, 230, 90), font=font)
    ly += 30
    for row in range(8):
        d.text((lx, ly), ROW_LABEL[row], fill=(210, 210, 220), font=font)
        ly += 22

    # --- the rules, written out under the art -------------------------------
    ty = art_h + 12
    rules = [
        "WHAT MUST REPEAT / MEET (the seams a player sees):",
        "1. A tile is LEFT half + RIGHT half. At the join (x15 of the L cell vs x0 of",
        "   the R cell) the art must continue - same courses, same tones.",
        "2. Down a run the engine stacks: row0 cap, then (row2, row1) PER TILE, then",
        "   row3+row4. So row1's bottom edge must meet row2's top edge (the loop),",
        "   row0's bottom must meet row2's top, row1's bottom must meet row3's top.",
        "3. Rows 3+4 together are ONE 32px front face: row3 bottom edge continues",
        "   into row4 top edge, per column.",
        "4. Rows 1-2 vs rows 0,3,4: columns 1 and 2 swap L/R roles (red box). Draw",
        "   the four HALVES as halves, never one continuous picture across cols 0-3.",
        "5. Window insert: rows 0-1 are the ROOF with a slot cut ALONG the wall",
        "   (tall+narrow, dark reveals, lit far lip, glass at the bottom of the cut).",
        "   Rows 2-4 stay TRANSPARENT. Rows 5-7 are the normal front window.",
        "6. Doors: 8 cells of 32x128, rot0-3 x closed/open. Only the yellow band",
        "   (~y88-128) is prominently visible; the leaf must fill it like vanilla's.",
        "7. Consecutive tiles OVERLAP by 16px vertically - a tile paints 48px of",
        "   screen (bands drawY-16, drawY, drawY+16). Nothing may depend on being",
        "   drawn exactly once.",
        "",
        "Check any sheet with:  python3 tools/conform_wall_sheet.py <file.png>",
        "(measures every seam against vanilla stonewall, fixes the mechanical ones)",
    ]
    for line in rules:
        d.text((14, ty), line, fill=(225, 225, 230), font=font)
        ty += 22

    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    img.save(args.out)
    print("wrote", args.out)


if __name__ == "__main__":
    main()
