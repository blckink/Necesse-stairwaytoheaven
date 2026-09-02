#!/usr/bin/env python3
"""The annotated wall-sheet template: which region of the 352x128 is WHAT.

Made for hand-drawing wall sets outside this repo. The renderer's grammar is
invisible from an image editor -- which columns are tile HALVES, which band of
screen a cell lands in, that a whole group of cells only appears where your
wall meets a DIFFERENT wall, how tall a door actually draws. Every one of those
has shipped as a bug at least once, so this writes the rules onto the template.

    build/qa/wall-template-map.png           annotated 4x map over a sheet
    docs/references/wall-template-map.png    the committed copy

NOTHING ON THE SHEET IS UNUSED. An earlier version of this file marked six
cells "DEAD" -- (2,5) (3,5) (2,6) (3,6) (2,7) (3,7) -- because vanilla's own
public draw path collapses the protected overload's three neighbour arrays
(adj, sameWall, isWall) into one, which makes `isWall[n] && !sameWall[n]`
unsatisfiable FROM THAT PATH. That is a fact about one call site, not about the
format: those cells are what the wall shows where it abuts a wall of ANOTHER
material, and vanilla paints all six fully opaque on every sheet (measured
256/256 px on stonewall, woodwall and brickwall). Leave them empty and two
abutting wall sets punch holes in each other.

Halves and screen bands are ENUMERATED from the engine port in
wall_render_preview (all 256 adjacency combinations x the isWall variants x
forceDrawTop/forceRemoveBot), never hand-asserted. The role text per cell is
read from WallObject.addWallDrawOptions / WallWindowObject / WallDoorObject in
the 1.3.2 decompile.

Usage:
    python3 tools/wall_template_map.py [--sheet path/to/wall.png]
"""
import argparse
import collections
import itertools
import os
import sys

from PIL import Image, ImageDraw, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO, "tools"))
os.environ.setdefault("NECESSE_SPRITES", os.path.join(REPO, "vanilla-sprites"))
import wall_render_preview as wrp  # noqa: E402

VANILLA = os.path.join(REPO, "vanilla-sprites", "objects", "stonewall.png")

Z = 6          # zoom factor: a 16px cell becomes 96px, which is
               # the smallest that fits its label without overrun
C = 16         # engine cell size in the body/window blocks

CO_LEFT = (60, 160, 255)      # left half of a tile
CO_RIGHT = (255, 170, 60)     # right half of a tile
CO_ROOF = (200, 70, 220)      # top-down roof view
CO_OTHER = (120, 235, 205)    # only against a DIFFERENT wall material
CO_WARN = (235, 60, 60)
CO_DOOR = (255, 230, 90)

# What each body cell IS, read from WallObject.addWallDrawOptions. The half and
# the screen band are not written here -- they are enumerated from the port.
ROLE = {
    (0, 0): "cap\nW end",      (1, 0): "cap\ncont.",
    (2, 0): "cap\ncont.",      (3, 0): "cap\nE end",
    (0, 1): "under a\nwindow or\nforeign\nwall",
    (1, 1): "inner\ncorner",   (2, 1): "inner\ncorner",
    (3, 1): "under a\nwindow or\nforeign\nwall",
    (0, 2): "roof",            (1, 2): "roof\n+corner",
    (2, 2): "roof\n+corner",   (3, 2): "roof",
    (0, 3): "FACE top\nW end",  (1, 3): "FACE top\ncont.",
    (2, 3): "FACE top\ncont.",  (3, 3): "FACE top\nE end",
    (0, 4): "FACE bot\nW end",  (1, 4): "FACE bot\ncont.",
    (2, 4): "FACE bot\ncont.",  (3, 4): "FACE bot\nE end",
    (0, 5): "outer\ncorner",   (1, 5): "outer\ncorner",
    (2, 5): "face vs\nFOREIGN\nwall below",
    (3, 5): "face vs\nFOREIGN\nwall below",
    (0, 6): "outer\ncorner\nfoot",
    (1, 6): "outer\ncorner\nfoot",
    (2, 6): "face vs\nFOREIGN\nwall below",
    (3, 6): "face vs\nFOREIGN\nwall below",
    (0, 7): "diagonal\nhook",  (1, 7): "diagonal\nhook",
    (2, 7): "roof vs\nFOREIGN\nwall bel-R",
    (3, 7): "roof vs\nFOREIGN\nwall bel-L",
}

# The cells reachable only when the neighbouring wall is a DIFFERENT material.
OTHER_WALL_CELLS = {(2, 5), (3, 5), (2, 6), (3, 6), (2, 7), (3, 7)}

BAND_NAME = {-16: "abv", 0: "top", 16: "bot"}   # abv = the 16px band ABOVE the tile

ROW_LABEL = {
    0: "row 0  TOP CAP (north rim). 4 variants by neighbour: W end / continuing / E end",
    1: "row 1  cols 0,3 = band under a window or a foreign wall; cols 1,2 = inner corners",
    2: "row 2  ROOF band, drawn on the tile's own upper 16px when a wall stands below",
    3: "row 3  FRONT FACE, upper half (the wall seen head-on)",
    4: "row 4  FRONT FACE, lower half (foot; meets the ground)",
    5: "row 5  cols 0,1 = outer bottom corners; cols 2,3 = face against a FOREIGN wall",
    6: "row 6  cols 0,1 = those corners' feet; cols 2,3 = face against a FOREIGN wall",
    7: "row 7  cols 0,1 = diagonal hooks; cols 2,3 = roof against a FOREIGN wall",
}


def enumerate_cell_usage():
    """(half, bands, uses) per body cell, straight out of the engine port."""
    r = wrp.WallRenderer(None)
    use = collections.defaultdict(lambda: {"half": set(), "band": set(), "n": 0})
    for bits in itertools.product((False, True), repeat=8):
        adj = list(bits)
        for extra in ((), (wrp.B,), (wrp.BL,), (wrp.BR,), (wrp.B, wrp.BL, wrp.BR)):
            is_wall = list(adj)
            for i in extra:
                is_wall[i] = True
            for fdt in (False, True):
                for frb in (False, True):
                    for col, row, dx, dy in r.wall_cells(adj, fdt, frb, is_wall):
                        e = use[(col, row)]
                        e["half"].add("L" if dx == 0 else "R")
                        e["band"].add(dy)
                        e["n"] += 1
    return use


def tint(img, box, colour, alpha=64):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(layer).rectangle(box, fill=colour + (alpha,))
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
    usage = enumerate_cell_usage()
    unused = [(c, r) for r in range(8) for c in range(4) if (c, r) not in usage]

    art_w, art_h = 352 * Z, 128 * Z
    legend_w = 640
    text_h = 640
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
            "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf", 14)
        font_sm = ImageFont.truetype(
            "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf", 12)
        font_big = ImageFont.truetype(
            "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf", 18)
    except OSError:
        font = font_sm = font_big = ImageFont.load_default()

    # --- body blob: half, band and role per cell ---------------------------
    for row in range(8):
        for col in range(4):
            x0, y0 = col * C * Z, row * C * Z
            box = (x0, y0, x0 + C * Z - 1, y0 + C * Z - 1)
            e = usage[(col, row)]
            halves = "".join(sorted(e["half"]))
            if (col, row) in OTHER_WALL_CELLS:
                tint(img, box, CO_OTHER, 56)
            else:
                tint(img, box, CO_LEFT if halves == "L" else CO_RIGHT, 40)
            bands = "+".join(BAND_NAME[b] for b in sorted(e["band"]))
            d.text((x0 + 5, y0 + 2), halves, fill=(255, 255, 255), font=font_big)
            d.text((x0 + 26, y0 + 6), bands, fill=(225, 225, 235), font=font)
            ty = y0 + 26
            for line in ROLE[(col, row)].split("\n"):
                d.text((x0 + 5, ty), line, fill=(240, 240, 245), font=font_sm)
                ty += 14
            d.rectangle(box, outline=(255, 255, 255, 90))

    d.rectangle((2 * C * Z, 5 * C * Z, 4 * C * Z - 1, 8 * C * Z - 1),
                outline=CO_OTHER, width=3)

    # --- window insert 64..96px --------------------------------------------
    wx = 64 * Z
    tint(img, (wx, 0, wx + 32 * Z - 1, 2 * C * Z - 1), CO_ROOF, 70)
    d.rectangle((wx, 0, wx + 32 * Z - 1, 2 * C * Z - 1), outline=CO_ROOF, width=3)
    for i, line in enumerate(("N-S WALL", "(left/right walls", "of a room)", "",
                              "The wall's TOP SURFACE,", "seen from ABOVE, with",
                              "the opening cut ALONG it.", "NOT a standing pane.")):
        d.text((wx + 6, 4 + i * 17), line, fill=(255, 225, 255),
               font=font_big if i == 0 else font)
    d.rectangle((wx, 2 * C * Z, wx + 32 * Z - 1, 8 * C * Z - 1),
                outline=(120, 200, 255), width=3)
    for i, line in enumerate(("E-W WALL", "(top/bottom walls)", "",
                              "The opening seen HEAD-ON,", "96px tall: rows 2-4 rise",
                              "TWO TILES ABOVE the wall,", "rows 5-7 sit at it.", "",
                              "stonewall leaves rows 2-4", "empty, woodwall paints",
                              "them - both are correct.")):
        d.text((wx + 6, 2 * C * Z + 4 + i * 17), line, fill=(210, 235, 255),
               font=font_big if i == 0 else font)

    # --- door strip 96..352px ----------------------------------------------
    # 32px columns 3..10, drawn at pos(drawX, drawY - 96): the whole 128px cell
    # is visible, the tile's own 32px is the BOTTOM of it.
    doors = (("rot0", "closed", True), ("rot0", "open", True),
             ("rot1", "closed", False), ("rot1", "open", False),
             ("rot2", "closed", True), ("rot2", "open", True),
             ("rot3", "closed", False), ("rot3", "open", False))
    for i, (rot, state, mirrored) in enumerate(doors):
        x0 = (96 + i * 32) * Z
        d.rectangle((x0, 0, x0 + 32 * Z - 1, art_h - 1),
                    outline=(255, 255, 255, 80), width=1)
        d.text((x0 + 6, 4), "%s %s" % (rot, state), fill=CO_DOOR, font=font_big)
        d.text((x0 + 6, 26), "E-W wall" if mirrored else "N-S wall",
               fill=(255, 245, 190), font=font)
        if mirrored:
            d.text((x0 + 6, 44), "MIRRORED when", fill=(255, 200, 120), font=font)
            d.text((x0 + 6, 60), "next to another", fill=(255, 200, 120), font=font)
            d.text((x0 + 6, 76), "door", fill=(255, 200, 120), font=font)
        # the tile's own 32px, at the very bottom of the 128 cell
        d.rectangle((x0, 96 * Z, x0 + 32 * Z - 1, 128 * Z - 1),
                    outline=CO_DOOR, width=3)
        d.text((x0 + 6, 98 * Z), "its own tile", fill=CO_DOOR, font=font)

    # --- legend column ------------------------------------------------------
    lx = art_w + 16
    ly = 10
    d.text((lx, ly), "WALL SHEET 352x128 - REGION MAP", fill=(255, 255, 255),
           font=font_big)
    ly += 30
    d.text((lx, ly), "L / R  = which HALF of a tile the cell is",
           fill=(225, 225, 230), font=font)
    ly += 20
    d.text((lx, ly), "abv / top / bot = which 16px BAND of screen",
           fill=(225, 225, 230), font=font)
    ly += 26
    for colour, txt in (
            (CO_LEFT, "LEFT half  (drawn at drawX)"),
            (CO_RIGHT, "RIGHT half (drawn at drawX+16)"),
            (CO_ROOF, "roof seen from ABOVE (N-S window)"),
            (CO_OTHER, "only where a DIFFERENT wall abuts -"),
    ):
        d.rectangle((lx, ly, lx + 22, ly + 16), fill=colour + (150,))
        d.text((lx + 30, ly), txt, fill=(230, 230, 230), font=font)
        ly += 24
    d.text((lx + 30, ly), "still MUST be drawn, vanilla fills all six",
           fill=(190, 245, 230), font=font)
    ly += 28
    d.text((lx, ly), "Cells never drawn by the engine: %s"
           % (", ".join("(%d,%d)" % c for c in unused) if unused else "NONE"),
           fill=(255, 255, 255), font=font)
    ly += 26
    for row in range(8):
        d.text((lx, ly), ROW_LABEL[row], fill=(210, 210, 220), font=font_sm)
        ly += 20
    ly += 10
    d.text((lx, ly), "DOORS: 8 cells of 32x128 at 32-col 3..10.", fill=CO_DOOR, font=font)
    ly += 20
    for line in ("Drawn at drawY-96, so the WHOLE 128px shows:",
                 "the door rises three tiles above its own tile",
                 "(yellow box). rot0/rot2 sit in E-W walls and are",
                 "MIRRORED when the next tile is a door too, so the",
                 "leaf must read both ways. rot1/rot3 sit in N-S",
                 "walls and are never mirrored."):
        d.text((lx, ly), line, fill=(255, 245, 190), font=font_sm)
        ly += 18

    # --- the rules ----------------------------------------------------------
    ty = art_h + 12
    for line in (
        "WHAT MUST REPEAT / MEET (the seams a player sees):",
        "1. A tile is LEFT half + RIGHT half. At the join (x15 of the L cell against x0 of the R cell) the art must",
        "   continue - same courses, same tones.",
        "2. Down a run the engine stacks: row0 cap, then (row2, row1) PER TILE, then row3+row4. So row1's bottom edge",
        "   must meet row2's top edge, and row1's bottom must meet row3's top.",
        "3. Rows 3+4 together are ONE 32px front face: row3's bottom edge continues into row4's top edge, per column.",
        "4. Which column is which HALF is not constant down the sheet - read the L/R printed in each cell. Painting one",
        "   continuous picture across cols 0-3 renders mirrored.",
        "5. Consecutive tiles OVERLAP by 16px: a tile paints 48px of screen (bands above / top / bot).",
        "6. Cols 2-3 of rows 5,6,7 (cyan box) appear ONLY where your wall touches a wall of another material. Vanilla",
        "   paints them solid on every sheet. Empty here = holes wherever two wall sets meet.",
        "7. Window: the N-S cells are the wall's ROOF with a slot cut along it; the E-W cells are a 96px head-on",
        "   elevation whose top two tiles rise above the wall.",
        "8. Doors: the full 32x128 is visible, three tiles of it above the wall tile. rot0/rot2 get mirrored.",
        "",
        "Check any sheet with:  python3 tools/conform_wall_sheet.py <file.png> [--fix]",
        "(measures every seam against five vanilla walls and fixes the mechanical faults)",
    ):
        d.text((14, ty), line, fill=(225, 225, 230), font=font)
        ty += 20

    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    img.save(args.out)
    print("wrote %s (cells never drawn: %s)"
          % (args.out, unused if unused else "none"))


if __name__ == "__main__":
    main()
