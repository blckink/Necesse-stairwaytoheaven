#!/usr/bin/env python3
"""No string the engine builds from one of our IDs may reach a player as a raw
key, and the two locales must not drift apart.

A missing key is never silently absent. Localization.getTranslation falls back
to a DebugTranslationElement whose text is literally "<category>.<key>", so the
player sees the internal ID in the world, in chat, in the crafting menu or over
an NPC's head. Two of those have already shipped:

  * the Stairway itself showed "skystairwaydown" because it had a tooltip but
    no [object] name;
  * a settler NPC showed "mob.wardensettlername", because HumanMob.getLocalization
    (jar 1.3.2, HumanMob.java:1653) returns mob.<stringID>NAME - not mob.<stringID> -
    for any human that has been given a settler name, and every HumanMob gets one
    in init() (HumanMob.java:1575). The audit only knew about mob.<stringID>.

Both times the audit passed. The lesson is the same each time: the audit must
follow the ID from the registration call to the key the ENGINE actually asks
for, including every key our registration calls create without our source ever
writing the string down. Each check below exists because of one such path.

Usage: python3 tools/locale_audit.py   (exit 1 on any finding)
"""
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, "src", "main", "java", "stairwaytoheaven")
LOCALE = os.path.join(REPO, "src", "main", "resources", "locale")
LANGS = ("en", "de")

# Direct registry call -> the locale section its display name must live in.
REGISTRARS = {
    "registerItem": "item",
    "registerObject": "object",
    "registerTile": "tile",
    "registerMob": "mob",
    # Biome.getNewLocalization() is new LocalMessage("biome", getStringID()),
    # so an unnamed biome prints "biome.driftlands" on the map and in the
    # biome banner exactly like an unnamed object prints its ID.
    "registerBiome": "biome",
}

# Vanilla helpers that register SEVERAL objects from one call. Not one of the
# resulting IDs appears as a literal in our source, which is how the Skystone
# Brick window shipped nameless and how the Skyiron Fence Gate shipped as
# "object.skyironfencegate": nothing was scanning for an ID nobody had written
# down. Each entry is (index of the ID argument, suffixes that need a name).
#
# The suffixes follow vanilla's own en.lang: it names the wall, the door, the
# locked door and the closed gate, and leaves the open/unlocked counterparts
# unnamed because the player never sees them in a list. Ours additionally names
# the window, because ours is craftable.
MULTI_OBJECT_REGISTRARS = {
    # WallObject.registerWallObjects(prefix, ...) -> prefix+wall, prefix+door,
    # prefix+dooropen, prefix+doorlocked, prefix+doorunlocked, prefix+window
    "registerWallObjects": (0, ("wall", "door", "doorlocked", "window")),
    # FenceGateObject.registerGatePair(fenceID, prefix, ...) -> prefix,
    # prefix+open. The ID is the SECOND argument, after the fence it connects to.
    "registerGatePair": (1, ("",)),
    # CrystalClusterObject.registerCrystalCluster(id, ...) -> id, id+"r"
    # (the "r" object is the mined-out remains, and it IS shown to the player).
    "registerCrystalCluster": (0, ("", "r")),
}

# Our own registration wrappers: they call ObjectRegistry.registerObject with a
# variable, so the ID only exists as a literal at the wrapper's CALL site.
# Whatever is not listed here is caught by check_registration_wrappers below
# rather than being quietly skipped.
LOCAL_REGISTRARS = {
    "registerPickable": "object",     # SkyObjects: pickable flowers
    "registerMeadowGrass": "object",  # SkyObjects: walk-through carpet grass
}

# Registry calls whose ID argument is legitimately a variable, keyed by the
# method that owns them. Anything else means a new wrapper was added and the
# IDs it creates are invisible to this audit - see check_registration_wrappers.
KNOWN_INDIRECT_METHODS = set(LOCAL_REGISTRARS)

# Base classes whose getLocalization() switches to mob.<id>name. Any mob that
# inherits from one of these - directly or through one of our own classes - is
# a human settler as far as the engine is concerned.
HUMAN_BASES = ("HumanMob", "HumanShop")

# Item textures these resolve through a class override rather than
# items/<id>.png: RockObject and RockOreObject build theirs from the rock/ore
# texture name. Verified against the decompiled sources.
TEXTURE_BY_CLASS = {
    "skystonerock", "aetheriumrock", "fulguriterock", "prismshardrock", "veilrock",
}


# --------------------------------------------------------------------------
# reading the tree

def source_files():
    for root, _dirs, files in os.walk(SRC):
        for name in sorted(files):
            if name.endswith(".java"):
                yield os.path.join(root, name)


def source_text():
    return "\n".join(open(path, encoding="utf-8").read() for path in source_files())


def where(path, text, offset):
    """repo-relative file:line for a regex match, so a finding names the line
    that would render the raw key rather than just the key."""
    return "%s:%d" % (os.path.relpath(path, REPO), text.count("\n", 0, offset) + 1)


def locale_entries(path):
    """{section: {key: value}} - values matter too: a mob.<id>name without the
    <name> placeholder loses the settler's actual name."""
    entries, section = {}, None
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if line.startswith("[") and line.endswith("]"):
                section = line[1:-1]
            elif line and not line.startswith("//") and "=" in line:
                key, value = line.split("=", 1)
                entries.setdefault(section, {})[key] = value
    return entries


# --------------------------------------------------------------------------
# what the mod registers

def registered_ids(text):
    found = {section: set() for section in REGISTRARS.values()}
    for call, section in REGISTRARS.items():
        for match in re.finditer(call + r'\(\s*"([^"]+)"', text):
            found[section].add(match.group(1))

    for call, section in LOCAL_REGISTRARS.items():
        for match in re.finditer(call + r'\(\s*"([^"]+)"', text):
            found[section].add(match.group(1))

    for call, (index, suffixes) in MULTI_OBJECT_REGISTRARS.items():
        args = r'\(\s*' + r'\s*,\s*'.join([r'[^,()]+'] * index + [r'"([^"]+)"'])
        for match in re.finditer(call + args, text):
            found["object"].update(match.group(1) + suffix for suffix in suffixes)

    # LevelIdentifier.getLocalization() reads [level] by the identifier's own
    # string ID, which is NOT the ID passed to LevelRegistry.registerLevel.
    found["level"] = set(re.findall(r'new LevelIdentifier\(\s*"([^"]+)"', text))

    # MatItem(stackSize, rarity, tooltipKey) resolves the tooltip through
    # Localization.translate("itemtooltip", tooltipKey) - a different section
    # from the item's own name, and one nothing else in the mod writes to.
    found["itemtooltip"] = set(re.findall(
        r'new\s+MatItem\s*\(\s*\d+\s*,\s*(?:Item\.)?Rarity\.[A-Z_]+\s*,\s*"([^"]+)"', text))
    return found


def class_supers():
    """{class name: superclass simple name} across our sources, with nested
    classes reachable as both "Inner" and "Outer.Inner" - registerMob is called
    with SpireCatMob.Black.class and friends."""
    supers = {}
    decl = re.compile(r'\bclass\s+(\w+)(?:<[^>]*>)?\s+extends\s+([\w.]+)')
    for path in source_files():
        text = open(path, encoding="utf-8").read()
        outer = None
        for match in decl.finditer(text):
            name, parent = match.group(1), match.group(2).rsplit(".", 1)[-1]
            if outer is None:
                outer = name
            else:
                supers["%s.%s" % (outer, name)] = parent
            supers[name] = parent
    return supers


def class_ref(raw):
    """"stairwaytoheaven.mobs.CloudLambMob" -> "CloudLambMob";
    "SpireCatMob.Black" stays whole (package parts are lower case)."""
    parts = raw.split(".")
    while parts and parts[0][:1].islower():
        parts.pop(0)
    return ".".join(parts)


def human_mob_ids(text, supers):
    """Registered mob IDs the engine will display through mob.<id>name.

    Two ways in. A mob whose class chain reaches HumanMob/HumanShop gets a
    settlerName in init() and from then on getLocalization() asks for
    mob.<id>name. And a mob registered as a settler is a SettlerMob, which
    Settler.onSettlerRegistryClosed requires and which in practice is always a
    HumanMob - Settler.getGenericMobName() additionally needs the plain
    mob.<id> for the settlement screens.
    """
    humans = set()
    for match in re.finditer(r'registerMob\(\s*"([^"]+)"\s*,\s*([\w.]+)\.class', text):
        mob_id, name = match.group(1), class_ref(match.group(2))
        seen = set()
        while name and name not in seen:
            seen.add(name)
            parent = supers.get(name)
            if parent is None:
                parent = supers.get(name.rsplit(".", 1)[-1])
            if parent is None:
                break
            if parent in HUMAN_BASES or parent.endswith("HumanMob"):
                humans.add(mob_id)
                break
            name = parent
    humans.update(re.findall(r'registerSettler\(\s*"([^"]+)"', text))
    return humans


def recipe_outputs(text):
    """Everything the player can craft - it needs a name AND an icon."""
    return {m.group(1) for m in re.finditer(r'new Recipe\(\s*"([^"]+)"', text)}


def local_message_refs():
    """Every (category, key, file:line) our source asks for by literal.

    A key we type by hand is exactly as likely to be missing as one a registry
    derives, and it fails the same way: SkywardStairwayObjectEntity would post
    "misc.skyreachhint" into chat.
    """
    call = re.compile(
        r'(?:new\s+LocalMessage|Localization\s*\.\s*translate)\s*\(\s*'
        r'"([^"]+)"\s*,\s*(?:"([^"]+)"|([A-Za-z_][\w.]*))', re.S)
    literals, dynamic = [], []
    for path in source_files():
        text = open(path, encoding="utf-8").read()
        for match in call.finditer(text):
            spot = where(path, text, match.start())
            if match.group(2) is not None:
                literals.append((match.group(1), match.group(2), spot))
            else:
                dynamic.append(("%s.<%s>" % (match.group(1), match.group(3)), spot))
    return literals, dynamic


def check_registration_wrappers():
    """Fail loudly when a registry call hides an ID behind a variable.

    This is the shape of every blind spot above: a helper registers real IDs,
    but the literal only exists at the helper's call site under a name this
    audit does not know. Rather than skip those silently - which is how the
    fence gate got through - a new wrapper must be added to LOCAL_REGISTRARS
    (or MULTI_OBJECT_REGISTRARS) before this passes again.
    """
    call = re.compile(r'\b(registerObject|registerTile|registerItem|registerMob|registerBiome)'
                      r'\(\s*([^,()]+)\s*,', re.S)
    method = re.compile(r'^[ \t]+(?:[\w<>\[\]]+\s+)+(\w+)\s*\([^;]*$', re.M)
    count = 0
    for path in source_files():
        text = open(path, encoding="utf-8").read()
        for match in call.finditer(text):
            if match.group(2).strip().startswith('"'):
                continue
            owners = method.findall(text[:match.start()])
            owner = owners[-1] if owners else "<unknown>"
            if owner in KNOWN_INDIRECT_METHODS:
                continue
            print("!! %s builds an ID for %s that this audit cannot see -- add %s "
                  "to LOCAL_REGISTRARS so its IDs get name-checked"
                  % (where(path, text, match.start()), match.group(1), owner))
            count += 1
    return count


# --------------------------------------------------------------------------

def main():
    text = source_text()
    ids = registered_ids(text)
    humans = human_mob_ids(text, class_supers())
    langs = {name: locale_entries(os.path.join(LOCALE, name + ".lang")) for name in LANGS}
    problems = 0

    # 1. Every registered ID needs a display name in both locales.
    for section in sorted(ids):
        for lang, entries in langs.items():
            for key in sorted(i for i in ids[section] if i not in entries.get(section, {})):
                print("!! [%s] %s has no name in %s.lang "
                      "-- the player would see \"%s.%s\"" % (section, key, lang, section, key))
                problems += 1

    # 2. Human settlers are displayed through mob.<id>name, with the settler's
    #    generated first name substituted into the <name> placeholder.
    for mob_id in sorted(humans):
        for lang, entries in langs.items():
            mobs = entries.get("mob", {})
            key = mob_id + "name"
            if key not in mobs:
                print("!! [mob] %s is a human settler, so the engine displays "
                      "it through mob.%s -- that key is missing from %s.lang and "
                      "the player would see \"mob.%s\" over his head"
                      % (mob_id, key, lang, key))
                problems += 1
            elif "<name>" not in mobs[key]:
                print("!! [mob] %s in %s.lang has no <name> placeholder -- the "
                      "settler's own name would be dropped" % (key, lang))
                problems += 1

    # 3. Keys our source names by hand, in sections our locales own.
    literals, dynamic = local_message_refs()
    for category, key, spot in literals:
        for lang, entries in langs.items():
            if category not in entries:
                continue  # a vanilla category (controls, ui, mobmsg, ...)
            if key not in entries[category]:
                print("!! [%s] %s is used at %s but missing from %s.lang "
                      "-- that call would render \"%s.%s\""
                      % (category, key, spot, lang, category, key))
                problems += 1

    # 4. A key in one locale but not the other means one language silently
    #    falls back to English, or to the ID.
    for section in sorted(set(langs["en"]) | set(langs["de"])):
        en = set(langs["en"].get(section, {}))
        de = set(langs["de"].get(section, {}))
        for key in sorted(en - de):
            print("!! [%s] %s exists in en.lang but not de.lang" % (section, key))
            problems += 1
        for key in sorted(de - en):
            print("!! [%s] %s exists in de.lang but not en.lang" % (section, key))
            problems += 1

    # 5. Anything craftable needs a name and an icon, or the crafting menu
    #    shows a raw ID, the engine's error texture, or both.
    named = {section: set(langs["en"].get(section, {})) & set(langs["de"].get(section, {}))
             for section in ("object", "tile", "item")}
    for output in sorted(recipe_outputs(text)):
        if not any(output in keys for keys in named.values()):
            print("!! craftable %s has no [object]/[tile]/[item] name in both "
                  "locales -- the crafting menu would show the raw ID" % output)
            problems += 1
        if output in TEXTURE_BY_CLASS or output in ids["tile"]:
            # TerrainSplatterTile.generateItemTexture crops the tile's own
            # texture and merges tiles/itemmask, so every floor tile is covered
            # without an items/ file. RockObject and RockOreObject resolve
            # through their rock/ore texture name. Both verified against the
            # decompiled sources.
            continue
        if not os.path.exists(os.path.join(REPO, "src", "main", "resources",
                                           "items", output + ".png")):
            print("!! craftable %s has no items/%s.png "
                  "-- the crafting menu would show an error icon" % (output, output))
            problems += 1

    # 6. And nothing may register an ID behind this audit's back.
    problems += check_registration_wrappers()

    # Keys assembled at runtime cannot be resolved from source. They are named
    # here rather than skipped in silence, because an unlisted gap is exactly
    # how the two shipped bugs happened.
    for key, spot in dynamic:
        print("-- note: %s at %s is built at runtime; this audit cannot "
              "check it" % (key, spot))

    total = sum(len(v) for v in ids.values())
    if problems:
        print("\n%d localization problem(s) across %d registered IDs." % (problems, total))
        return 1
    print("OK: %d registered IDs (%d of them human settlers needing mob.<id>name) "
          "and %d literal keys named in en.lang and de.lang, locales in sync, "
          "%d runtime-built key(s) noted above."
          % (total, len(humans), len(literals), len(dynamic)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
