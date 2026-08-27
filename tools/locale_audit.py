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


def source_text():
    out = []
    for root, _dirs, files in os.walk(SRC):
        for name in files:
            if name.endswith(".java"):
                with open(os.path.join(root, name), encoding="utf-8") as handle:
                    out.append(handle.read())
    return "\n".join(out)


def registered_ids(text):
    found = {section: set() for section in REGISTRARS.values()}
    for call, section in REGISTRARS.items():
        for match in re.finditer(call + r'\(\s*"([^"]+)"', text):
            found[section].add(match.group(1))

    # Wall sets register SIX object IDs from one call, and not one of them
    # appears as a literal in our source. That is how the Skystone Brick window
    # shipped nameless and with the engine's error icon: nothing was scanning
    # for an ID nobody had written down. Vanilla names the wall, the door and
    # the locked door (see its own en.lang) and leaves the open/unlocked states
    # to inherit; ours also names the window, because ours is craftable.
    for match in re.finditer(r'registerWallObjects\(\s*"([^"]+)"', text):
        prefix = match.group(1)
        found["object"].update({prefix + suffix
                                for suffix in ("wall", "door", "doorlocked", "window")})
    return found


def recipe_outputs(text):
    """Everything the player can craft — it needs a name AND an icon."""
    return {m.group(1) for m in re.finditer(r'new Recipe\(\s*"([^"]+)"', text)}


# Item textures these resolve through a class override rather than
# items/<id>.png: RockObject and RockOreObject build theirs from the rock/ore
# texture name. Verified against the decompiled sources.
TEXTURE_BY_CLASS = {
    "skystonerock", "aetheriumrock", "fulguriterock", "prismshardrock", "veilrock",
}


def resolves_texture_by_class(output, ids):
    """True when the engine builds this item's icon itself.

    TerrainSplatterTile.generateItemTexture crops the tile's own texture and
    merges tiles/itemmask, so every floor tile is covered without an items/
    file. RockObject and RockOreObject resolve through their rock/ore texture
    name. Both verified against the decompiled sources.
    """
    return output in TEXTURE_BY_CLASS or output in ids["tile"]


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
    text = source_text()
    ids = registered_ids(text)
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

    # Anything craftable must also have an icon, or the crafting menu shows the
    # engine's error texture where the item should be.
    for output in sorted(recipe_outputs(text)):
        if resolves_texture_by_class(output, ids):
            continue
        if not os.path.exists(os.path.join(REPO, "src", "main", "resources",
                                           "items", output + ".png")):
            print(f"!! craftable {output} has no items/{output}.png "
                  f"-- the crafting menu would show an error icon")
            problems += 1

    total = sum(len(v) for v in ids.values())
    if problems:
        print(f"\n{problems} localization problem(s) across {total} registered IDs.")
        return 1
    print(f"OK: {total} registered IDs, all named in en.lang and de.lang, locales in sync.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
