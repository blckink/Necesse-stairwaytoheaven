#!/usr/bin/env python3
"""Gate: every SkyDecoObject/GhostDecoObject sheet is a whole number of strips.

SkyDecoObject cuts its sheet into strips of `variantWidth` and draws the
full sheet height. When a sheet grows (size rework) and the number in the
constructor does not, the game draws half a tree or a one-winged statue
(deadtree 48 -> 64 in 13413ba, heavenslab 32 -> 48 in a5003da).

Checks: sheet width % variantWidth == 0; every setCells(h, col,row...) cell
lies inside the sheet; a sheet 256 px or taller (vanilla stacks several trees
on top of each other, deadwood/willowtree are 512) must use setCells, or the
whole stack is drawn as one strip. Vanilla sheets are looked up in the local
sprite dump and skipped when it is missing.

Run: PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/deco_width_gate.py
Exit 1 on any mismatch.
"""
import os
import re
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, "src/main/java")
ROOTS = [os.path.join(REPO, "src/main/resources/objects"),
         "/home/blackoffset/dev/Necesse sprites/objects"]
PATTERN = re.compile(r'new\s+(?:Sky|Ghost)DecoObject\(\s*"([a-z0-9_]+)",\s*(?:"[a-z0-9_]+",\s*)?(\d+)')
CELLS = re.compile(r'\.setCells\(([\d,\s]+)\)')
TALL = 256


def find_sheet(name):
    for root in ROOTS:
        for dirpath, _, files in os.walk(root):
            if name + ".png" in files:
                return os.path.join(dirpath, name + ".png")
    return None


def main():
    bad = 0
    checked = 0
    for dirpath, _, files in os.walk(SRC):
        for f in files:
            if not f.endswith(".java"):
                continue
            path = os.path.join(dirpath, f)
            text = open(path, encoding="utf-8").read()
            for m in PATTERN.finditer(text):
                name, width = m.group(1), int(m.group(2))
                line = text.count("\n", 0, m.start()) + 1
                sheet = find_sheet(name)
                if sheet is None:
                    print(f"skip  {name} ({os.path.relpath(path, REPO)}:{line}): sheet not found")
                    continue
                w, h = Image.open(sheet).size
                checked += 1
                where = f"({os.path.relpath(path, REPO)}:{line})"
                if w % width != 0:
                    bad += 1
                    print(f"FAIL  {name} variantWidth={width} but sheet is {w}x{h} {where}")
                # setCells belongs to this registration only if it comes
                # before the statement ends.
                end = text.find(";", m.end())
                cells = CELLS.search(text, m.end(), end)
                if cells:
                    nums = [int(x) for x in cells.group(1).replace(" ", "").split(",") if x]
                    ch, pairs = nums[0], nums[1:]
                    for col, row in zip(pairs[0::2], pairs[1::2]):
                        if (col + 1) * width > w or (row + 1) * ch > h:
                            bad += 1
                            print(f"FAIL  {name} cell ({col},{row}) of {width}x{ch} "
                                  f"outside sheet {w}x{h} {where}")
                elif h >= TALL:
                    bad += 1
                    print(f"FAIL  {name} sheet {w}x{h} is a stack but has no setCells {where}")
    print(f"{checked} deco objects checked, {bad} mismatches")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
