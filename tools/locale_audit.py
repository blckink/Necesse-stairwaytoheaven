#!/usr/bin/env python3
"""Every registered item, object and tile must have a display name in BOTH
locales, and the two locales must not drift apart.

Internal string IDs reaching the player is not a cosmetic bug: a real play
session found the mod's headline object, the Stairway itself, showing
"skystairwaydown" in game because it had a tooltip but no [object] name. This
catches that class of mistake before a build ships.

Usage: python3 tools/locale_audit.py   (exit 1 on any finding)
"""
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, "src", "main", "java", "stairwaytoheaven")
LOCALE = os.path.join(REPO, "src", "main", "resources", "locale")

# registry call -> the locale section its display name must live in
REGISTRARS = {
    "registerItem": "item",
    "registerObject": "object",
    "registerTile": "tile",
}


def registered_ids():
    found = {section: set() for section in REGISTRARS.values()}
    for root, _dirs, files in os.walk(SRC):
        for name in files:
            if not name.endswith(".java"):
                continue
            with open(os.path.join(root, name), encoding="utf-8") as handle:
                text = handle.read()
            for call, section in REGISTRARS.items():
                for match in re.finditer(call + r'\(\s*"([^"]+)"', text):
                    found[section].add(match.group(1))
    return found


def locale_keys(path):
    keys, section = {}, None
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if line.startswith("[") and line.endswith("]"):
                section = line[1:-1]
            elif line and not line.startswith("//") and "=" in line:
                keys.setdefault(section, set()).add(line.split("=", 1)[0])
    return keys


def main():
    ids = registered_ids()
    langs = {
        name: locale_keys(os.path.join(LOCALE, name + ".lang"))
        for name in ("en", "de")
    }
    problems = 0

    for section in sorted(ids):
        for lang, keys in langs.items():
            missing = sorted(i for i in ids[section] if i not in keys.get(section, set()))
            for key in missing:
                print(f"!! [{section}] {key} has no name in {lang}.lang "
                      f"-- the player would see the raw ID")
                problems += 1

    # A key in one locale but not the other means one language silently falls
    # back to English, or to the ID.
    for section in sorted(set(langs["en"]) | set(langs["de"])):
        en_only = langs["en"].get(section, set()) - langs["de"].get(section, set())
        de_only = langs["de"].get(section, set()) - langs["en"].get(section, set())
        for key in sorted(en_only):
            print(f"!! [{section}] {key} exists in en.lang but not de.lang")
            problems += 1
        for key in sorted(de_only):
            print(f"!! [{section}] {key} exists in de.lang but not en.lang")
            problems += 1

    total = sum(len(v) for v in ids.values())
    if problems:
        print(f"\n{problems} localization problem(s) across {total} registered IDs.")
        return 1
    print(f"OK: {total} registered IDs, all named in en.lang and de.lang, locales in sync.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
