#!/usr/bin/env python3
"""Checks every GrassObject variant strip cell by cell.

Usage:  PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/variant_strip_audit.py
        ... --strict     # also fail on the gutter and coverage warnings

Why this exists
---------------
`GrassObject.loadTextures()` (decompiled 1.3.2) derives the variant count from
the SHEET, not from the constructor:

    int sprites = spriteTexture.getWidth() / 32;

and `addDrawables` then picks one uniformly per tile:

    addGrassDrawable(..., 0, this.textures.length - 1);

So a 256x32 strip is ALWAYS eight variants, and every empty 32px cell is a
plant that renders as nothing on that tile. The second constructor argument is
`density` (max neighbouring objects in `densityCheck`) and has nothing to do
with the variant count -- a confusion that already cost one shipped bug.

`objects/giantmonstera.png` shipped on 2026-09-09 with cells
[0, 0, 216, 267, 261, 198, 0, 0]: four of eight empty, so half of all placed
plants were invisible in game. `tools/asset_intake.py` passed it (exit 0) --
it checks canvas size, colour count and alpha, never per-cell content --
and `tools/size_audit.py` has no row for it, so both gates were green.
This script is the gate that would have caught it.

Reference: all seven vanilla GrassObject strips in the sprite dump fill 8/8
cells, minimum mass 156 opaque px, and NONE of them touches a cell's left or
right edge -- vanilla always keeps a transparent gutter.
"""
import ast
import os
import re
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")
JAVA = os.path.join(REPO, "src", "main", "java")

CELL = 32
# Vanilla's lightest variant is 156 opaque px. Anything under this is not a
# drawn plant, it is a speck -- but keep the floor below vanilla so a
# legitimately sparse variant does not trip it.
MIN_MASS = 100

# Constructors and helpers whose FIRST string literal is a grass strip name.
CALLS = (
    re.compile(r'GrassObject\(\s*"([a-z0-9_]+)"'),
    re.compile(r'registerPickable\(\s*"([a-z0-9_]+)"'),
    re.compile(r'registerMeadowGrass\(\s*"([a-z0-9_]+)"'),
)


def strip_names():
    """Every object name the code registers as a GrassObject."""
    names = set()
    for root, _, files in os.walk(JAVA):
        for f in files:
            if not f.endswith(".java"):
                continue
            src = open(os.path.join(root, f), encoding="utf-8").read()
            for pat in CALLS:
                names.update(pat.findall(src))
    return sorted(names)


def cell_report(path):
    """(cells, [opaque px per cell], [cells touching a side edge])."""
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    cells = w // CELL
    masses, touching = [], []
    for i in range(cells):
        c = im.crop((i * CELL, 0, (i + 1) * CELL, h))
        alpha = c.getchannel("A")
        masses.append(sum(1 for v in alpha.get_flattened_data() if v > 0))
        box = c.getbbox()
        if box and (box[0] == 0 or box[2] == CELL):
            touching.append(i)
    return cells, masses, touching, (w, h)


def converted_without_audit():
    """Supplied art that no size_audit.py row covers -- i.e. unverified."""
    gen = os.path.join(REPO, "tools", "asset_generator", "generate_assets.py")
    tree = ast.parse(open(gen, encoding="utf-8").read())
    conv = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Assign) and getattr(node.targets[0], "id", None) == "CONVERTED":
            conv = [e.value for e in node.value.elts]
    audit = open(os.path.join(REPO, "tools", "size_audit.py"), encoding="utf-8").read()
    return conv, [n for n in conv if os.path.basename(n)[:-4] not in audit]


def main():
    strict = "--strict" in sys.argv
    errors, warnings = [], []

    print("== GrassObject variant strips (cells = sheet width / 32) ==")
    for name in strip_names():
        path = os.path.join(RES, "objects", name + ".png")
        if not os.path.exists(path):
            errors.append("%s: objects/%s.png fehlt" % (name, name))
            print("  FEHLT  %s" % name)
            continue
        cells, masses, touching, (w, h) = cell_report(path)
        empty = [i for i, m in enumerate(masses) if m == 0]
        thin = [i for i, m in enumerate(masses) if 0 < m < MIN_MASS]

        state = "OK  "
        if empty or thin:
            state = "FEHLER"
        elif touching:
            state = "WARN"
        print("  %-6s %-18s %3dx%-3d %d Zellen  %s" % (state, name, w, h, cells, masses))

        if empty:
            errors.append(
                "%s: Zelle(n) %s sind leer -- die Engine zieht gleichverteilt aus "
                "%d Varianten, diese Tiles bleiben im Spiel unsichtbar"
                % (name, empty, cells))
        if thin:
            errors.append(
                "%s: Zelle(n) %s haben unter %d sichtbare Pixel -- zu wenig fuer "
                "eine gezeichnete Variante" % (name, thin, MIN_MASS))
        if touching:
            warnings.append(
                "%s: Zelle(n) %s beruehren den Zellenrand -- vanilla laesst immer "
                "einen transparenten Rand; die Pflanze wird an der Kante glatt "
                "abgeschnitten" % (name, touching))

    conv, missing = converted_without_audit()
    print("\n== Deckung von size_audit.py ==")
    print("  %d von %d gelieferten Assets (CONVERTED) haben KEINE Zeile in "
          "size_audit.py." % (len(missing), len(conv)))
    print("  Fuer diese sagt ein gruenes size_audit NICHTS aus.")
    if missing:
        warnings.append(
            "%d gelieferte Assets ohne size_audit-Zeile -- ungeprueft, nicht "
            "geprueft-und-gut" % len(missing))
        if strict:
            for m in missing:
                print("     ungeprueft: %s" % m)

    print()
    for w_ in warnings:
        print("WARNUNG: %s" % w_)
    for e in errors:
        print("FEHLER:  %s" % e)

    if errors:
        print("\n%d Fehler." % len(errors))
        return 1
    if warnings and strict:
        print("\n%d Warnung(en), --strict." % len(warnings))
        return 1
    print("\nAlle Varianten-Streifen sind vollstaendig gefuellt.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
