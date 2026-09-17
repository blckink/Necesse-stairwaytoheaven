#!/usr/bin/env python3
"""Find mod classes that MUST be registered but are not.

Why this exists: on 2026-09-17 talking to the War Veteran dropped the player
back to the menu. `VeteranDefense extends WorldData` and `VeteranAmmoDialogue
extends SettlerDialogue` were never registered, and both resolve their registry
ID inside their own constructor -- so the server threw "Cannot construct
unregistered WorldData class VeteranDefense" the moment his shop built his
dialogue list. Nothing caught it: the build is green either way, and no test
opens a settler's shop.

The rule this checks is mechanical: for every class in src/main/java that
extends one of the registered base types below, some source file must name that
class in the matching register call.

    PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/registry_gate.py

Exit 0 = every such class is registered. Exit 1 = the list of misses.
"""
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, "src", "main", "java")

# base class -> the register call that must name the subclass
RULES = {
    "WorldData": "registerWorldData",
    "LevelData": "registerLevelData",
    "SettlerDialogue": "registerSettlerDialogue",
    "GameQuest": "registerQuest",
}


def java_files():
    for root, _dirs, files in os.walk(SRC):
        for f in files:
            if f.endswith(".java"):
                yield os.path.join(root, f)


def main():
    sources = {p: open(p, encoding="utf-8").read() for p in java_files()}
    whole = "\n".join(sources.values())
    misses = []
    for path, text in sources.items():
        m = re.search(r"\bclass\s+(\w+)\s+extends\s+([\w.]+)", text)
        if not m:
            continue
        cls, base = m.group(1), m.group(2).split(".")[-1]
        call = RULES.get(base)
        if not call:
            continue
        # abstract bases of the mod's own are registered per concrete subclass
        if re.search(r"\babstract\s+class\s+" + cls + r"\b", text):
            continue
        if re.search(call + r"\s*\([^;]*\b" + cls + r"\.class", whole, re.S):
            continue
        misses.append((cls, base, call, os.path.relpath(path, REPO)))

    for cls, base, call, path in sorted(misses):
        print("!! %s extends %s but no %s(...) names it -- %s" % (cls, base, call, path))
        print("   constructing it throws \"Cannot construct unregistered %s class %s\"" % (base, cls))
    print("%d unregistered class(es) across %d source files" % (len(misses), len(sources)))
    return 1 if misses else 0


if __name__ == "__main__":
    sys.exit(main())
