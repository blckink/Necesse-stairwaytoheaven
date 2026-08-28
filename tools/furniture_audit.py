#!/usr/bin/env python3
"""Furniture audit: does our furniture actually behave like vanilla furniture?

A table that is not a TableObject is not a table. The game decides room
scores, settler jobs, chair-facing and table-decoration placement from the
vanilla base classes in necesse.level.gameObject.furniture, so a beautiful
sprite on a generic decoration object is worth exactly nothing.

This gate reads SkyFurnitureSet.java and asserts, for every piece:

  1. it is constructed on the approved vanilla base class for its role
  2. objects/<id>.png exists at the exact size that class's renderer reads
  3. its auxiliary sheets exist (_mask for beds, _off for candelabra)
  4. items/<id>.png exists (32x32) - otherwise the icon is the engine's ERR tile
  5. it has a crafting recipe
  6. it has an en.lang and a de.lang name
  7. the auto-registered multi-tile halves have NO icon and NO recipe,
     because they are not obtainable

Run: python3 tools/furniture_audit.py
"""

import os
import re
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, "src/main/java/stairwaytoheaven/SkyFurnitureSet.java")
OBJECTS = os.path.join(ROOT, "src/main/resources/objects")
ITEMS = os.path.join(ROOT, "src/main/resources/items")
LOCALE = os.path.join(ROOT, "src/main/resources/locale")

# vanilla base class -> (sheet size, extra sheets, auto-registered half suffixes)
#
# Sizes measured on the vanilla sprite dump; see
# docs/research/furniture-formats.md for what each renderer reads.
CLASSES = {
    "ChairObject":            ((128, 64),  [],       []),
    "BenchObject":            ((128, 128), [],       ["2"]),
    "ModularTableObject":     ((96, 64),   [],       []),
    "DinnerTableObject":      ((128, 128), [],       ["2"]),
    "DeskObject":             ((128, 64),  [],       []),
    "DresserObject":          ((128, 64),  [],       []),
    "BedObject":              ((128, 128), ["_mask"], ["2"]),
    "CandelabraObject":       ((128, 64),  ["_off"], []),
    "ModularCarpetObject":    ((64, 64),   ["mask"], []),
    "TableDecorationObject":  ((32, 32),   [],       []),
    "PotTableDecorationObject": ((32, 32), [],       []),
    # The spire pieces. BookshelfObject and CabinetObject live in
    # gameObject/container, not gameObject/furniture, but both are
    # FurnitureObjects with a furnitureType ("bookshelf"/"cabinet") and an
    # InventoryObjectEntity behind them, so they are furniture AND storage.
    # Sizes measured on oakbookshelf/oakcabinet/oakclock/oakdisplay.
    "BookshelfObject":        ((128, 128), [],       []),
    "CabinetObject":          ((128, 128), [],       []),
    "ClockObject":            ((128, 64),  [],       []),
    "DisplayStandObject":     ((128, 32),  [],       []),
}

# Roles we require to exist at all. A "furniture set" without a sittable chair
# or a decoration-holding table is not a furniture set - and, since the spire
# plan asks for them by name, without the four storage/reading pieces either.
REQUIRED_CLASSES = {"ChairObject", "ModularTableObject", "BenchObject", "BedObject",
                    "BookshelfObject", "CabinetObject", "ClockObject",
                    "DisplayStandObject"}

# Base classes that are NOT furniture, however table-shaped the sprite is.
# Registering one of these from SkyFurnitureSet is the exact mistake this
# gate exists to catch.
BANNED = {"SkyDecoObject", "GameObject", "StatueObject", "PaintingObject"}


def parse_pieces(src):
    """Return [(stringID, className)] for every piece SkyFurnitureSet registers."""
    pieces = []
    # ObjectRegistry.registerObject("id", new SomeObject("id", ...)
    for sid, cls in re.findall(
            r'registerObject\(\s*"([a-z0-9]+)"\s*,\s*\n?\s*new\s+(\w+)\(', src):
        pieces.append((sid, cls))
    # Static helpers: BenchObject.registerBench("id", "id", ...)
    for cls, sid in re.findall(r'(\w+)\.register\w+\(\s*\n?\s*"([a-z0-9]+)"', src):
        if cls in CLASSES:
            pieces.append((sid, cls))
    return pieces


def main():
    src = open(JAVA, encoding="utf-8").read()
    pieces = parse_pieces(src)
    if not pieces:
        print("FAIL: SkyFurnitureSet registers nothing")
        return 1

    en = open(os.path.join(LOCALE, "en.lang"), encoding="utf-8").read()
    de = open(os.path.join(LOCALE, "de.lang"), encoding="utf-8").read()
    recipes = set(re.findall(r'new Recipe\("([a-z0-9]+)"', src))

    fails = []
    seen_classes = set()

    for sid, cls in sorted(pieces):
        if cls in BANNED:
            fails.append(f"{sid}: registered as {cls}, which the game does not "
                         f"treat as furniture at all")
            continue
        if cls not in CLASSES:
            fails.append(f"{sid}: unknown base class {cls} - add it to CLASSES "
                         f"with its measured sheet size, or use a vanilla one")
            continue
        seen_classes.add(cls)
        size, extras, halves = CLASSES[cls]

        # 2/3: object sheets. Carpets are the one family the engine reads
        # from a subdirectory.
        subdir = "carpets" if cls == "ModularCarpetObject" else ""
        for suffix in [""] + extras:
            path = os.path.join(OBJECTS, subdir, f"{sid}{suffix}.png")
            if not os.path.exists(path):
                fails.append(f"{sid}: missing objects/{os.path.join(subdir, sid + suffix)}.png "
                             f"({cls} reads it)")
                continue
            got = Image.open(path).size
            if got != size:
                fails.append(f"{sid}: objects/{os.path.join(subdir, sid + suffix)}.png is {got}, "
                             f"{cls} needs {size}")

        # 4: item icon
        icon = os.path.join(ITEMS, f"{sid}.png")
        if not os.path.exists(icon):
            fails.append(f"{sid}: missing items/{sid}.png - the inventory icon "
                         f"would render as the engine's ERR tile")
        else:
            got = Image.open(icon).size
            if got != (32, 32):
                fails.append(f"{sid}: items/{sid}.png is {got}, expected (32, 32)")

        # 5: recipe
        if sid not in recipes:
            fails.append(f"{sid}: no crafting recipe - the player can never get it")

        # 6: names
        if not re.search(rf"^{sid}=", en, re.M):
            fails.append(f"{sid}: no en.lang [object] name")
        if not re.search(rf"^{sid}=", de, re.M):
            fails.append(f"{sid}: no de.lang [object] name")

        # 7: the auto-registered halves must stay unobtainable
        for suffix in halves:
            half = sid + suffix
            if os.path.exists(os.path.join(ITEMS, f"{half}.png")):
                fails.append(f"{half}: has an item icon, but {cls}'s register "
                             f"helper registers it as not obtainable")
            if half in recipes:
                fails.append(f"{half}: has a recipe, but it is the auto-registered "
                             f"other half of {sid}")

    missing_roles = REQUIRED_CLASSES - seen_classes
    for cls in sorted(missing_roles):
        fails.append(f"the set has no {cls} - a furniture family without one "
                     f"does not furnish a room")

    if fails:
        print(f"furniture audit: {len(fails)} problem(s) in {len(pieces)} pieces\n")
        for f in fails:
            print("  FAIL " + f)
        return 1

    print(f"furniture audit: {len(pieces)} pieces OK "
          f"({len(seen_classes)} vanilla base classes, all sheets, icons, "
          f"recipes and names present)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
