#!/usr/bin/env python3
"""Every room plan built in code is still the plan the dossier draws.

`docs/design/chapter-01-skyreach-pois.md` holds fourteen ASCII room plans, one
character per tile. `RealmPoiPresets` carries them as `String[]` arrays and
reads its own width and height off them, so a plan is not a comment beside the
code -- it IS the code. That only holds while the two really match, and nothing
noticed if they drifted: a dropped character shifts a whole building, and the
game happily generates the shifted one.

This audits the transcription and nothing else. It does not check that a plan is
buildable -- `RealmPoiPresets.plan` does that at load, against sections 0.2-0.4
of the same dossier -- and it does not check that a place generates, which is
`scripts/integration_test.sh` (`skyreachstatus pois`).

Exit 0 when every array in the code is character-identical to its section's map.
"""

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
DOSSIER = REPO / "docs/design/chapter-01-skyreach-pois.md"
PRESETS = REPO / "src/main/java/stairwaytoheaven/worldgen/pois/RealmPoiPresets.java"

# Java array name -> the dossier heading whose first fenced block holds the map.
# Add a row here in the same commit that adds a plan, or the new one is unaudited.
PLANS = {
    "WAYSIDE_PLAN": "### 2.1 Skywatch Wayside",
    "HUT_PLAN": "### 2.11 The Dew-Keeper's Hut",
    "FOLD_PLAN": "### 2.2 The Shepherd's Fold",
    "INSTITUTE_PLAN": "### 2.3 The Institute of Applied Falling",
    "WAYHOUSE_PLAN": "### 2.7 The Passage Wayhouse",
    "REDOUBT_PLAN": "### 2.4 Nightfell Redoubt",
    "MANUFACTORY_PLAN": "### 2.5 The Aether Manufactory",
    "ANVIL_PLAN": "### 2.6 The Sovereign's Anvil",
    "GATE_PLAN": "### 2.8 The Unopened Gate",
    "CHOIR_PLAN": "### 2.9 The Prism Choir",
    "REEF_PLAN": "### 2.10 The Serpent's Reef",
}

# The dossier draws each row as "  y12  ..#####..", the leading label being a
# reading aid rather than part of the map.
ROW = re.compile(r"^\s*y\d+\s+(\S+)\s*$")


def dossier_rows(text, heading):
    if heading not in text:
        raise LookupError("no such heading: " + heading)
    block = text.split(heading, 1)[1].split("```")[1]
    return [m.group(1) for m in map(ROW.match, block.splitlines()) if m]


def java_rows(text, name):
    marker = "String[] %s = {" % name
    if marker not in text:
        raise LookupError("no such array: " + name)
    body = text.split(marker, 1)[1].split("};", 1)[0]
    return re.findall(r'"([^"]*)"', body)


def main():
    dossier = DOSSIER.read_text(encoding="utf-8")
    presets = PRESETS.read_text(encoding="utf-8")
    flags = 0
    for name, heading in sorted(PLANS.items()):
        try:
            drawn = dossier_rows(dossier, heading)
            built = java_rows(presets, name)
        except LookupError as error:
            print("FLAG %s: %s" % (name, error))
            flags += 1
            continue
        if not drawn:
            print("FLAG %s: %s has no map rows" % (name, heading))
            flags += 1
            continue
        if drawn == built:
            print("ok   %s: %d rows x %d tiles, identical to %s"
                  % (name, len(drawn), len(drawn[0]), heading))
            continue
        flags += 1
        print("FLAG %s: differs from %s" % (name, heading))
        for y in range(max(len(drawn), len(built))):
            a = drawn[y] if y < len(drawn) else "<missing>"
            b = built[y] if y < len(built) else "<missing>"
            if a != b:
                print("     y%-3d dossier %r" % (y, a))
                print("          code    %r" % (b,))
    print("plan transcription audit: %d flag(s)" % flags)
    return 1 if flags else 0


if __name__ == "__main__":
    sys.exit(main())
