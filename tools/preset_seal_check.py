#!/usr/bin/env python3
"""Gate: a house preset drawn as a character map must actually be sealed.

A wall map is easy to read and easy to get wrong -- one missing '#' at a step
in the silhouette and the building has a hole nothing in the build catches,
because a preset compiles and stamps happily either way. This floods the map
from outside the bounding box and asserts that no interior cell ('.') is
reachable without passing through a wall, window or door.

Run: python3 tools/preset_seal_check.py
"""
import re
import sys
from collections import deque
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# file -> name of the String[] field holding the map
PRESETS = {
    "src/main/java/stairwaytoheaven/worldgen/CrookedHousePreset.java": "PLAN",
}

SOLID = set("#OD")
INTERIOR = set(".")


def extract_map(path: Path, field: str):
    src = path.read_text(encoding="utf-8")
    m = re.search(r"String\[\]\s+" + re.escape(field) + r"\s*=\s*\{(.*?)\};", src, re.S)
    if not m:
        raise SystemExit(f"FAIL {path.name}: no String[] {field} found")
    return re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(1))


def check(path_str: str, field: str) -> bool:
    path = ROOT / path_str
    rows = extract_map(path, field)
    if not rows:
        print(f"FAIL {path.name}: {field} is empty")
        return False

    widths = {len(r) for r in rows}
    if len(widths) != 1:
        print(f"FAIL {path.name}: rows have differing widths {sorted(widths)}")
        return False
    w, h = widths.pop(), len(rows)

    # Flood the outside from a one-cell margin around the map.
    seen = [[False] * w for _ in range(h)]
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if rows[y][x] not in SOLID and not seen[y][x]:
                seen[y][x] = True
                q.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            if rows[y][x] not in SOLID and not seen[y][x]:
                seen[y][x] = True
                q.append((x, y))

    while q:
        x, y = q.popleft()
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and not seen[ny][nx] and rows[ny][nx] not in SOLID:
                seen[ny][nx] = True
                q.append((nx, ny))

    leaks = [(x, y) for y in range(h) for x in range(w)
             if rows[y][x] in INTERIOR and seen[y][x]]
    interior = sum(row.count(".") for row in rows)
    doors = sum(row.count("D") for row in rows)

    if leaks:
        print(f"FAIL {path.name}/{field}: {len(leaks)} interior tile(s) reachable "
              f"from outside, first at {leaks[0]}")
        for y, row in enumerate(rows):
            print("   " + "".join("!" if (x, y) in leaks else c for x, c in enumerate(row)))
        return False
    if doors == 0:
        print(f"FAIL {path.name}/{field}: sealed but has no door")
        return False
    print(f"OK   {path.name}/{field}: {w}x{h}, {interior} interior tiles, "
          f"{doors} door(s), sealed")
    return True


def main() -> int:
    ok = True
    for path_str, field in PRESETS.items():
        ok &= check(path_str, field)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
