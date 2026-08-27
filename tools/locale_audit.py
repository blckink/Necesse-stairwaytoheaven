#!/usr/bin/env python3
"""Nothing the engine builds from one of our IDs may reach a player as a raw
key or as the error texture, and the two locales must not drift apart.

A missing key is never silently absent. Localization.getTranslation falls back
to a DebugTranslationElement whose text is literally "<category>.<key>", so the
player sees the internal ID in the world, in chat, in the crafting menu or over
an NPC's head. A missing TEXTURE is not silently absent either:
GameTexture.fromFile (jar 1.3.2, GameTexture.java:163) swallows the
FileNotFoundException and hands back GameResources.error - the 32x32 red "ERR"
tile - so the player sees ERR sitting in his inventory. Three of those have
already shipped:

  * the Stairway itself showed "skystairwaydown" because it had a tooltip but
    no [object] name;
  * a settler NPC showed "mob.wardensettlername", because HumanMob.getLocalization
    (jar 1.3.2, HumanMob.java:1653) returns mob.<stringID>NAME - not mob.<stringID> -
    for any human that has been given a settler name, and every HumanMob gets one
    in init() (HumanMob.java:1575). The audit only knew about mob.<stringID>.
  * Gloomshroom and Whisper Reeds showed ERR where their item icon belongs,
    because ObjectRegistry.onRegister (ObjectRegistry.java:2051) gives EVERY
    registered object an ObjectItem, ObjectItem.loadItemTextures
    (ObjectItem.java:69) asks the object for generateItemTexture, and the
    GameObject default (GameObject.java:767) is items/<stringID>.png. The
    audit only checked recipe outputs, and neither of those two is craftable -
    the player gets them by breaking one, because GameObject.getLootTable
    (GameObject.java:278) hands the object's own item back whenever that item
    is obtainable.

Every time the audit passed. The lesson is the same each time: the audit must
follow the ID from the registration call to the key - or the file - the ENGINE
actually asks for, including everything our registration calls create without
our source ever writing the string down. Each check below exists because of one
such path.

Usage: python3 tools/locale_audit.py   (exit 1 on any finding)
"""
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, "src", "main", "java", "stairwaytoheaven")
RESOURCES = os.path.join(REPO, "src", "main", "resources")
LOCALE = os.path.join(RESOURCES, "locale")
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

# --------------------------------------------------------------------------
# where an item icon really comes from
#
# ObjectItem/TileItem/Item all ask their content for a texture, and only the
# DEFAULT implementation is items/<stringID>.png. A class that overrides
# generateItemTexture reads a different file, usually named after a constructor
# argument rather than after the object's own ID. Listing the exempt IDs by
# hand - which is what this audit used to do - hides both halves of that: a new
# object of an exempt class is never checked, and the file the exempt class
# DOES need is never checked either. veilrock sat in exactly that hole: it is a
# RockObject, so the old list skipped it, and RockObject.generateItemTexture
# (RockObject.java:111) wants items/veilrock.png, which did not exist.
#
# So: map the CLASS to the file its override reads, as (resource directory,
# index of the constructor argument that names it). Every class not listed here
# falls through to the engine default, items/<stringID>.png.
ITEM_TEXTURE_BY_CLASS = {
    # RockObject.generateItemTexture -> items/<rockTexture>, the first
    # constructor argument, which is NOT the registered string ID.
    "RockObject": ("items", 0),
    # RockOreObject.generateItemTexture (RockOreObject.java:177) paints
    # objects/<oreTexture> - argument 2 - through items/oremask onto the parent
    # rock's item texture. oremask is vanilla's, and the parent rock is
    # registered separately and audited on its own row, so the ore overlay is
    # the only file this registration owes.
    "RockOreObject": ("objects", 2),
    # TerrainSplatterTile.generateItemTexture (TerrainSplatterTile.java:63)
    # crops tiles/<terrainTexture>_splat.png, or tiles/<terrainTexture>.png
    # when there is no splat sheet, and multiplies tiles/itemmask over it. The
    # texture name is argument 1 of TerrainSplatterTile(isFloor, textureName)
    # and argument 0 of the SimpleFloorTile/SimpleTiledFloorTile shorthand;
    # tile_texture_name below follows our subclasses' super() calls to it.
    "TerrainSplatterTile": ("tiles", 1),
    "SimpleFloorTile": ("tiles", 0),
    "SimpleTiledFloorTile": ("tiles", 0),
    # LiquidTile.generateItemTexture (LiquidTile.java:76) tints vanilla's
    # tiles/bucket with the liquid colour - nothing of ours is involved.
    "LiquidTile": None,
    # FlowerObject hands out a FlowerObjectItem carrying the object's own
    # sheet (FlowerObject.java:171); WallTrapObject (WallTrapObject.java:166),
    # MaskedPressurePlateObject (:68), SingleOreRockSmall (:129) and
    # GameLogicGate (:81) all rebuild theirs from the wall/plate/rock texture.
    # None of ours use them yet; they are listed so that the day one does, the
    # audit does not invent a missing file.
    "FlowerObject": None,
    "WallTrapObject": None,
    "MaskedPressurePlateObject": None,
    "SingleOreRockSmall": None,
    "GameLogicGate": None,
}

# Vanilla helpers that register several objects from one call, and which of the
# resulting IDs get an obtainable item - i.e. one the player can hold and whose
# icon is therefore drawn. Read out of the helpers themselves:
# WallObject.registerWallObjects (WallObject.java:477-481) passes itemObtainable
# to the wall and to registerDoorPair's closed door only; the window, the open
# door, the locked door and the unlocked door are all registered false.
# FenceGateObject.registerGatePair (FenceGateObject.java:203) registers the
# closed gate true and the open one false. CrystalClusterObject
# .registerCrystalCluster (CrystalClusterObject.java:50-52) passes its own
# isObtainable flag to the cluster and registers the mined-out "r" remains
# false. Anything registered false is unreachable (ItemRegistry.isObtainable
# gates both GameObject.getLootTable and the creative list) and needs no icon.
MULTI_OBJECT_HELD_SUFFIXES = {
    "registerWallObjects": (0, ("wall", "door")),
    "registerGatePair": (1, ("",)),
    "registerCrystalCluster": (0, ("",)),
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


# --------------------------------------------------------------------------
# what the player can end up holding, and what draws it

# Item classes that load their own texture instead of items/<stringID>.png.
# None of ours use one yet; they are named so that the first mod cosmetic or
# bucket does not get reported as a missing file.
ITEM_CLASS_DRAWS_ITSELF = {
    "BucketItem", "WigArmorItem", "ShoesArmorItem", "ShirtArmorItem",
    "LogicGateItem", "CustomObjectItem",
}


def call_sites(text, name):
    """(offset, [argument expressions]) for every call to `name` in `text`.

    Registrations here run over several lines and carry nested calls, colours
    and anonymous subclasses, so the arguments cannot be split with a regex -
    the parser below tracks bracket depth and string literals.
    """
    out = []
    for match in re.finditer(r'\b' + name + r'\s*\(', text):
        i, depth, in_string = match.end(), 1, False
        while i < len(text) and depth:
            char = text[i]
            if in_string:
                if char == "\\":
                    i += 1
                elif char == '"':
                    in_string = False
            elif char == '"':
                in_string = True
            elif char in "([{":
                depth += 1
            elif char in ")]}":
                depth -= 1
            i += 1
        out.append((match.start(), split_arguments(text[match.end():i - 1])))
    return out


def split_arguments(body):
    args, current, depth, in_string = [], "", 0, False
    index = 0
    while index < len(body):
        char = body[index]
        if in_string:
            current += char
            if char == "\\":
                current += body[index + 1]
                index += 2
                continue
            if char == '"':
                in_string = False
        elif char == '"':
            in_string = True
            current += char
        elif char in "([{":
            depth += 1
            current += char
        elif char in ")]}":
            depth -= 1
            current += char
        elif char == "," and depth == 0:
            args.append(" ".join(current.split()))
            current = ""
        else:
            current += char
        index += 1
    if current.strip():
        args.append(" ".join(current.split()))
    return args


def literal(expression):
    """The string a Java expression IS, or None if it is computed."""
    if expression and expression.startswith('"') and expression.endswith('"'):
        return expression[1:-1]
    return None


def construction(expression, text):
    """(class simple name, [constructor arguments]) behind an expression.

    `new GrassObject("skyreeds", 4)` answers itself; `skystoneRock` and
    `SkyRegistry.stairwayDown` are followed back to the `= new X(...)` that
    built them, because that is where the texture-naming argument lives.
    """
    match = re.match(r'new\s+([\w.]+)\s*\(', expression)
    haystack = expression
    if match is None:
        name = expression.split(".")[-1].strip()
        if not re.fullmatch(r'\w+', name or ""):
            return None, []
        match = re.search(r'\b' + name + r'\s*=\s*new\s+([\w.]+)\s*\(', text)
        if match is None:
            return None, []
        haystack = text[match.start():]
    qualified = match.group(1)
    sites = call_sites(haystack, r'new\s+' + re.escape(qualified))
    return qualified.rsplit(".", 1)[-1], (sites[0][1] if sites else [])


def our_constructor(class_name):
    """([parameter names], [super() arguments]) for one of our classes.

    Our tiles name their sheet in their own super() call - AshsandTile passes
    "ashsand" up to TerrainSplatterTile - so the texture the engine reads is
    only reachable by walking that call.
    """
    for path in source_files():
        text = open(path, encoding="utf-8").read()
        for offset, params in call_sites(text, r'(?:public|protected|private)\s+'
                                         + re.escape(class_name)):
            tail = text[offset:]
            supers = call_sites(tail[:tail.find("}")], "super")
            if supers:
                names = [p.split()[-1] for p in params if p.split()]
                return names, supers[0][1]
    return None, None


def texture_argument(class_name, args, target, index, supers):
    """The literal at `args[index]` of `target`'s constructor, seen from a
    subclass. Walks our own constructors, substituting each parameter for the
    argument the registration actually passed."""
    seen = set()
    while class_name != target:
        if class_name in seen:
            return None
        seen.add(class_name)
        params, super_args = our_constructor(class_name)
        if super_args is None:
            return None
        args = [args[params.index(a)] if a in params and params.index(a) < len(args)
                else a for a in super_args]
        class_name = supers.get(class_name)
        if class_name is None:
            return None
    return literal(args[index]) if index < len(args) else None


# Returned when a class IS in the table but the texture-naming argument is not
# a literal. Skipping that quietly would rebuild the exact blind spot this
# check exists to close, so the caller reports it instead.
UNRESOLVED = "<computed at runtime>"


def icon_paths(class_name, args, supers):
    """Files that must exist for this content's icon to draw, as a list of
    acceptable alternatives; None means the engine builds the icon out of
    something that is not ours (vanilla art, or another registration audited
    on its own row)."""
    name = class_name
    seen = set()
    while name and name not in seen:
        seen.add(name)
        if name in ITEM_TEXTURE_BY_CLASS:
            rule = ITEM_TEXTURE_BY_CLASS[name]
            if rule is None:
                return None
            directory, index = rule
            texture = texture_argument(class_name, args, name, index, supers)
            if texture is None:
                return UNRESOLVED
            if directory == "tiles":
                return ["tiles/%s_splat.png" % texture, "tiles/%s.png" % texture]
            return ["%s/%s.png" % (directory, texture)]
        name = supers.get(name)
    return []  # engine default: items/<stringID>.png


def held_content(recipes):
    """Every ID the player can end up holding, and the icon file behind it.

    Reachable means any of three things, and the icon is drawn for all three:
    the registration is obtainable, so GameObject.getLootTable hands the item
    over the moment the thing is broken; it is obtainable in creative, so it
    sits in ItemRegistry.getCreativeItems (ItemRegistry.java:3398); or a recipe
    makes it. Everything registered unreachable is skipped, exactly as vanilla
    does - it ships no items/wildmushroom.png either.
    """
    supers = class_supers()
    whole = source_text()
    wrapper_ids = {
        wrapper: [literal(args[0]) for _o, args in call_sites(whole, wrapper)
                  if literal(args[0])]
        for wrapper in LOCAL_REGISTRARS}
    method = re.compile(r'^[ \t]+(?:[\w<>\[\]]+\s+)+(\w+)\s*\([^;]*$', re.M)
    rows = []

    for path in source_files():
        text = open(path, encoding="utf-8").read()
        for call, kind in (("registerObject", "object"), ("registerTile", "tile"),
                           ("registerItem", "item")):
            for offset, args in call_sites(text, call):
                if len(args) < 4:
                    continue
                spot = where(path, text, offset)
                ids = [literal(args[0])] if literal(args[0]) else None
                if ids is None:
                    owners = method.findall(text[:offset])
                    ids = wrapper_ids.get(owners[-1] if owners else "", [])
                if args[3] not in ("true", "false"):
                    rows.append((spot, kind, literal(args[0]) or args[0],
                                 UNRESOLVED, args[3]))
                    continue
                obtainable = args[3] == "true"
                creative = args[5] == "true" if len(args) > 5 and args[5] in (
                    "true", "false") else obtainable
                class_name, ctor = construction(args[1], text)
                for string_id in ids:
                    if not (obtainable or creative or string_id in recipes):
                        continue
                    if kind == "item":
                        wanted = ([] if class_name not in ITEM_CLASS_DRAWS_ITSELF
                                  else None)
                    else:
                        wanted = icon_paths(class_name, ctor, supers)
                    rows.append((spot, kind, string_id, wanted, class_name))

    # IDs the vanilla multi-object helpers create. Our source never writes them
    # down, which is how a whole family could ship iconless without a single
    # grep hitting anything.
    for call, (index, suffixes) in MULTI_OBJECT_HELD_SUFFIXES.items():
        for path in source_files():
            body = open(path, encoding="utf-8").read()
            for offset, args in call_sites(body, call):
                prefix = literal(args[index]) if index < len(args) else None
                if prefix is None:
                    continue
                if call == "registerCrystalCluster" and "false" in args[5:6]:
                    continue
                for suffix in suffixes:
                    rows.append((where(path, body, offset), "object",
                                 prefix + suffix, [], call))

    # A recipe reaches the player whatever the registration flags say, so
    # anything craftable that no registration row already covers is added here
    # with the engine default. This is how the wall windows are covered: they
    # are registered unobtainable and are still sold in the crafting menu.
    covered = {row[2] for row in rows}
    for output in sorted(recipes - covered):
        rows.append(("recipe", "object", output, [], "Recipe"))

    best = {}
    for row in rows:
        # A real registration knows more than the recipe fallback does.
        if row[2] not in best or best[row[2]][4] == "Recipe":
            best[row[2]] = row
    return sorted(best.values(), key=lambda row: row[2])


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

    # 5. Anything craftable needs a name, or the crafting menu shows a raw ID.
    #    Its icon is check 6's business, along with everything else holdable.
    named = {section: set(langs["en"].get(section, {})) & set(langs["de"].get(section, {}))
             for section in ("object", "tile", "item")}
    recipes = recipe_outputs(text)
    for output in sorted(recipes):
        if not any(output in keys for keys in named.values()):
            print("!! craftable %s has no [object]/[tile]/[item] name in both "
                  "locales -- the crafting menu would show the raw ID" % output)
            problems += 1

    # 6. Everything the player can hold needs the file that draws it. A recipe
    #    output was never the whole set: Gloomshroom and Whisper Reeds are not
    #    craftable, they are picked, and picking one puts an ERR tile in the
    #    inventory. So the question this asks is not "is it craftable" but
    #    "can the player end up holding it" - see held_content.
    icons = 0
    for spot, kind, string_id, wanted, class_name in held_content(recipes):
        if wanted is None:
            continue  # drawn from vanilla art, or from another audited row
        if wanted is UNRESOLVED:
            print("!! %s %s at %s is registered through %s, which this audit "
                  "cannot follow to the file that draws it -- teach "
                  "ITEM_TEXTURE_BY_CLASS about it rather than leaving it unseen"
                  % (kind, string_id, spot, class_name))
            problems += 1
            continue
        icons += 1
        wanted = wanted or ["items/%s.png" % string_id]
        if any(os.path.exists(os.path.join(RESOURCES, *path.split("/")))
               for path in wanted):
            continue
        print("!! %s %s (%s, registered at %s) has no %s -- the player can hold "
              "it and the inventory would draw the engine's ERR texture"
              % (kind, string_id, class_name, spot, " or ".join(wanted)))
        problems += 1

    # 7. And nothing may register an ID behind this audit's back.
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
          "%d holdable ID(s) with a real icon file, %d runtime-built key(s) "
          "noted above." % (total, len(humans), len(literals), icons, len(dynamic)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
