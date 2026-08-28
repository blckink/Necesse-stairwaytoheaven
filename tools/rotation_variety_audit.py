#!/usr/bin/env python3
"""Rotation-variety audit: every cell the engine reads separately must carry a
separate picture.

`tools/sheet_format_audit.py` guards cell GEOMETRY -- which cell, what size,
what extent. It is completely blind to a sheet whose cells are the right size,
in the right place, and all the same image. That is a different bug with a
different symptom, and the player names it exactly:

    "die Tueren und Tore lassen sich alle nicht ausrichten wie sonst im Game"

Turning the piece changes the rotation, the engine dutifully reads a different
cell, and the cell holds the same art -- so nothing visibly happens. That is how
the Skywatch Banner shipped: `gen_banner_painting` pasted ONE 32x32 cell into
all four `PaintingObject` rotation rows, so the banner looked identical on a
north wall, a south wall and both side walls. Its geometry was perfect and
`sheet_format_audit` was green.

What this audit checks, per family, is only what the repository has an actual
engine read for (`docs/research/structures-furniture.md`,
`docs/research/furniture-formats.md`, `docs/TECHNICAL_LEARNINGS.md`):

  PaintingObject      32x128, four 32x32 ROWS, row = rotation, and the rotation
                      names where the wall is (0 below, 1 left, 2 above,
                      3 right). Four walls, four views.
  WallTorchObject     64x128, `sprite(active?0:1, sprite, 32)`: two state
                      columns x four attach orientations. The four orientations
                      must differ, and lit must differ from unlit.
  StreetlampObject    32x192, `sprite(0, active?0:1, 32, 96)`: two 32x96 rows,
                      on and off. A lamp whose rows match never looks lit.
  LampObject pairs    `<id>.png` + `<id>_off.png`, same size, four rotation
                      columns each, and the two files must differ.
  WallDoorObject      the wall sheet's eight 32x128 door cells at 32-cell index
                      3..10 -- closed and open, once per rotation.
  FenceGateObject     192x64, six 32px columns (open/closed horizontal, vertical
                      post, latch, closed and open vertical leaf).
  FenceObject         160x64, five 32px columns (post, north joint, south rail,
                      west run, east run).
  Rotation sheets     the four 32px columns of every piece
                      `sheet_format_audit.ROTATION_SHEETS` and `.STATION_SHEETS`
                      already hold to a band -- reusing that list keeps the two
                      audits from drifting apart.

MIRRORED cells count as distinct and are reported, not flagged: vanilla's own
left/right views are mirrors of each other, and so are ours.

Deliberately NOT covered: the 1x2 multi-tile pieces (bench, bed, dinner table).
`docs/research/furniture-formats.md` records their sheet SIZE but not the engine
read that splits it, and their generators paste 64px-wide blocks across two
32px columns -- so "column 2 equals column 3" cannot be judged without the
decompiled draw call. Read it, then add them here; do not guess a frame.

Usage:  python3 tools/rotation_variety_audit.py
Exit code 1 if any family ships fewer distinct pictures than the engine reads.
"""
import itertools
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from sheet_format_audit import ROTATION_SHEETS, STATION_SHEETS  # noqa: E402

WALL_SHEETS = ("objects/skystonebrickwall.png", "objects/nightfellwall.png",
               "objects/cloudmarblewall.png", "objects/beetlewall.png")

PAINTINGS = ("objects/skywatchbanner.png",)
WALL_LIGHTS = ("objects/mistglasslantern.png", "objects/flickerlightgarland.png")
STREETLAMPS = ("objects/wardencandelabra.png", "objects/ghostlantern.png")
LAMP_PAIRS = (("objects/skywatchcandelabra.png",
               "objects/skywatchcandelabra_off.png"),)
FENCE_GATES = ("objects/skyironfencegate.png", "objects/cloudmarblefencegate.png")
FENCES = ("objects/skyironfence.png", "objects/cloudmarblefence.png")

# Vanilla `oakdisplay.png` draws the same pedestal in all four rotation columns
# -- measured, and recorded in sheet_format_audit's band table as
# "0..31 (all four columns identical)". Ours matching it is correct, not a bug.
UNIFORM_BY_VANILLA = {"objects/skywatchdisplay.png"}

DOOR_CELL_NAMES = {3: "rot0 closed", 4: "rot0 open", 5: "rot1 closed",
                   6: "rot1 open", 7: "rot2 closed", 8: "rot2 open",
                   9: "rot3 closed", 10: "rot3 open"}
WALL_LIGHT_ORIENTATIONS = ("wall above", "wall right", "support below", "wall left")
GATE_COLUMNS = ("open horizontal", "closed horizontal", "vertical post",
                "latch", "closed vertical leaf", "open vertical leaf")
FENCE_COLUMNS = ("post", "north joint", "south rail", "west run", "east run")


def _open(rel, problems):
    path = os.path.join(RES, rel)
    if not os.path.exists(path):
        problems.append("%s: missing" % rel)
        return None
    return Image.open(path).convert("RGBA")


def _distinct(rel, cells, problems, what, mirrors_out):
    """cells: list of (label, PIL image). Flags every identical pair."""
    for (la, ia), (lb, ib) in itertools.combinations(cells, 2):
        if ia.tobytes() == ib.tobytes():
            problems.append(
                "%s: %s and %s are the same picture -- the engine reads them as "
                "two different %s, so turning the piece changes nothing"
                % (rel, la, lb, what))
        elif ia.transpose(Image.FLIP_LEFT_RIGHT).tobytes() == ib.tobytes():
            mirrors_out.append("%s: %s is the mirror of %s" % (rel, la, lb))
    return len(cells)


def check_paintings(problems, mirrors):
    """PaintingObject: 32x128, four 32x32 rows, one per wall direction."""
    checked = 0
    for rel in PAINTINGS:
        im = _open(rel, problems)
        if im is None:
            continue
        if im.size != (32, 128):
            problems.append("%s: %s, expected (32, 128) -- PaintingObject reads "
                            "four 32x32 rotation rows" % (rel, im.size))
            continue
        names = ("wall below", "wall left", "wall above", "wall right")
        cells = [(names[r], im.crop((0, r * 32, 32, r * 32 + 32))) for r in range(4)]
        checked += _distinct(rel, cells, problems, "wall directions", mirrors)
    return checked


def check_wall_lights(problems, mirrors):
    """WallTorchObject: sprite(active?0:1, sprite, 32) over 64x128."""
    checked = 0
    for rel in WALL_LIGHTS:
        im = _open(rel, problems)
        if im is None:
            continue
        if im.size != (64, 128):
            problems.append("%s: %s, expected (64, 128) -- two state columns x "
                            "four attach orientations" % (rel, im.size))
            continue
        for col, state in ((0, "lit"), (1, "unlit")):
            cells = [("%s %s" % (state, WALL_LIGHT_ORIENTATIONS[r]),
                      im.crop((col * 32, r * 32, col * 32 + 32, r * 32 + 32)))
                     for r in range(4)]
            checked += _distinct(rel, cells, problems, "attach orientations", mirrors)
        for r in range(4):
            lit = im.crop((0, r * 32, 32, r * 32 + 32))
            off = im.crop((32, r * 32, 64, r * 32 + 32))
            checked += 1
            if lit.tobytes() == off.tobytes():
                problems.append(
                    "%s: the %s cell is identical lit and unlit -- the light "
                    "never looks switched on" % (rel, WALL_LIGHT_ORIENTATIONS[r]))
    return checked


def check_streetlamps(problems, mirrors):
    """StreetlampObject: sprite(0, active?0:1, 32, 96) over 32x192."""
    checked = 0
    for rel in STREETLAMPS:
        im = _open(rel, problems)
        if im is None:
            continue
        if im.size != (32, 192):
            problems.append("%s: %s, expected (32, 192) -- StreetlampObject "
                            "row-selects on/off, it does not use a second file"
                            % (rel, im.size))
            continue
        cells = [("lit", im.crop((0, 0, 32, 96))), ("unlit", im.crop((0, 96, 32, 192)))]
        checked += _distinct(rel, cells, problems, "on/off states", mirrors)
    return checked


def check_lamp_pairs(problems, mirrors):
    """LampObject/CandelabraObject: <id>.png and <id>_off.png, 4 columns each."""
    checked = 0
    for lit_rel, off_rel in LAMP_PAIRS:
        lit = _open(lit_rel, problems)
        off = _open(off_rel, problems)
        if lit is None or off is None:
            continue
        if lit.size != off.size:
            problems.append("%s: %s but %s is %s -- both must match"
                            % (lit_rel, lit.size, off_rel, off.size))
            continue
        checked += 1
        if lit.tobytes() == off.tobytes():
            problems.append("%s and %s are the same image -- the lamp never "
                            "looks switched off" % (lit_rel, off_rel))
        for rel, im in ((lit_rel, lit), (off_rel, off)):
            cells = [("rot%d" % c, im.crop((c * 32, 0, c * 32 + 32, im.height)))
                     for c in range(im.width // 32)]
            checked += _distinct(rel, cells, problems, "rotations", mirrors)
    return checked


def check_doors(problems, mirrors):
    """WallDoorObject / WallDoorOpenObject: eight 32x128 cells, index 3..10."""
    checked = 0
    for rel in WALL_SHEETS:
        im = _open(rel, problems)
        if im is None:
            continue
        cells = [(DOOR_CELL_NAMES[i], im.crop((i * 32, 0, i * 32 + 32, 128)))
                 for i in sorted(DOOR_CELL_NAMES)]
        checked += _distinct(rel, cells, problems, "door views", mirrors)
    return checked


def check_gates_and_fences(problems, mirrors):
    checked = 0
    for rel in FENCE_GATES:
        im = _open(rel, problems)
        if im is None:
            continue
        if im.size != (192, 64):
            problems.append("%s: %s, expected (192, 64) -- six 32px gate columns"
                            % (rel, im.size))
            continue
        cells = [(GATE_COLUMNS[c], im.crop((c * 32, 0, c * 32 + 32, 64)))
                 for c in range(6)]
        checked += _distinct(rel, cells, problems, "gate pieces", mirrors)
    for rel in FENCES:
        im = _open(rel, problems)
        if im is None:
            continue
        if im.size != (160, 64):
            problems.append("%s: %s, expected (160, 64) -- five 32px fence columns"
                            % (rel, im.size))
            continue
        cells = [(FENCE_COLUMNS[c], im.crop((c * 32, 0, c * 32 + 32, 64)))
                 for c in range(5)]
        checked += _distinct(rel, cells, problems, "fence pieces", mirrors)
    return checked


def check_rotation_sheets(problems, mirrors):
    """The four rotation columns of every sheet the format audit already knows.

    Reusing its two tables is deliberate: a piece added there gets this check
    for free, and the two audits cannot end up guarding different sets.
    """
    checked = 0
    sheets = [(rel, size) for rel, (size, _b, _r) in ROTATION_SHEETS.items()]
    sheets += [(rel, size) for rel, (size, _h, _t, _b) in STATION_SHEETS.items()]
    for rel, size in sorted(set(sheets)):
        if rel in UNIFORM_BY_VANILLA:
            continue
        im = _open(rel, problems)
        if im is None or im.size != size:
            continue
        # The forge's last 32px row is its fire animation strip, not body art.
        body_h = im.height - 32 if rel.endswith("aetherforge.png") else im.height
        cells = [("rot%d" % c, im.crop((c * 32, 0, c * 32 + 32, body_h)))
                 for c in range(4)]
        checked += _distinct(rel, cells, problems, "rotations", mirrors)
    return checked


def main():
    problems, mirrors = [], []
    checked = (check_paintings(problems, mirrors)
               + check_wall_lights(problems, mirrors)
               + check_streetlamps(problems, mirrors)
               + check_lamp_pairs(problems, mirrors)
               + check_doors(problems, mirrors)
               + check_gates_and_fences(problems, mirrors)
               + check_rotation_sheets(problems, mirrors))
    for line in mirrors:
        print("  mirror pair (fine): %s" % line)
    if problems:
        print("\nROTATION VARIETY PROBLEMS:\n")
        for p in problems:
            print("  - %s" % p)
        print("\n%d problem(s). A cell the engine reads separately must hold a "
              "separate picture." % len(problems))
        return 1
    print("OK: %d rotation/state comparisons; every cell the engine reads "
          "separately holds its own picture." % checked)
    return 0


if __name__ == "__main__":
    sys.exit(main())
