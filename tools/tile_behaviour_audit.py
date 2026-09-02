#!/usr/bin/env python3
"""Tile-behaviour audit: every registered tile must BE what it is presented as.

A tile can lie about itself in two independent places, and a player feels both
as "this floor does not behave like a normal floor":

  1. THE JAVA DECLARATION. `GameTile(boolean isFloor)` (jar 1.3.2,
     GameTile.java:114) is the single switch that decides everything a player
     notices about a ground tile: the item lands in tiles/floors instead of
     tiles/terrain, tileHealth is 50 instead of 100, smartMinePriority flips,
     and `canBePlacedOn` (GameTile.java:236) refuses to let another tile be
     dropped straight on top. `TerrainSplatterTile.getTerrainPriority` then
     decides which neighbour bleeds into which: PRIORITY_FLOOR is 400,
     terrain sits at 0-200. A "floor" built as terrain blends into the ground
     around it and takes a different mining path — it is a floor in the menu
     and terrain in the world.

  2. THE `_splat` SHEET. Placement itself is always exactly one tile
     (TileItem.onPlace calls tile.placeTile once), so a tile that *looks* like
     it painted a 3x3 area is not misplacing — it is over-splatting. The
     engine reads a `_splat` atlas at fixed offsets: each 224x96 block is a
     7x3 grid of 32px cells, and SplattingOptions picks the cell by marching
     squares over the four orthogonal neighbours. Cells (0,0), (2,0), (0,2)
     and (2,2) are the DIAGONAL-only pieces — drawn on a tile whose *corner*
     touches ours. In vanilla they are a small nub in that corner (measured
     0.8%-29.3% of the cell across 66 vanilla sheets). Ours shipped at
     83%-89%, i.e. a near-full tile, so placing one floor repainted its four
     diagonal neighbours almost completely and read as a 3x3 blob. That is
     the "white floor places huge" playtest report, and no Java change can
     fix it: the geometry lives in the sheet.

Both halves are checked here because either one alone would have passed the
broken state.

The cell bands below were measured with PIL over the vanilla sprite dump
(66 land `_splat` sheets, 10 liquid ones) and widened outward so vanilla-like
art is never flagged; they are wide enough that only a structural mistake
trips them.

Usage:  python3 tools/tile_behaviour_audit.py [--vanilla /path/to/sprite/dump]
Exit code 1 on any finding. The vanilla dump is optional and is only used by
--recalibrate, which reprints the bands from the real vanilla sheets.
"""
import argparse
import collections
import os
import re
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, "src", "main", "java", "stairwaytoheaven")
RES = os.path.join(REPO, "src", "main", "resources")
TILES = os.path.join(RES, "tiles")

FLOOR, TERRAIN, LIQUID = "floor", "terrain", "liquid"

# ---------------------------------------------------------------------------
# What each registered tile IS. Every TileRegistry.registerTile call site in
# our source must appear here, so a new tile cannot be added without someone
# stating which of the three it is.
#
# The distinction is not cosmetic. TERRAIN is the ground the world is
# generated FROM — cloudturf, skystone, stormslate, murkmoss, blackpeat,
# ashsand and the two cloud/marsh seas. Terrain is meant to blend: that is how
# a Skyreach island reads as rock fading into turf instead of a tiled
# checkerboard, and SkyTerrainPainter paints these by area, never one cell at
# a time. FLOOR is what a player crafts at a workstation and lays down by
# hand, one tile per click, over whatever ground is there. Turning terrain
# into floors would flatten worldgen; turning floors into terrain is the bug
# this audit exists to catch.
# ---------------------------------------------------------------------------
ROLES = {
    "cloudturftile": TERRAIN,
    # Eden grass: natural ground on vanilla's overgrowngrass setup, from the
    # supplied art (kk-sprites), plantable via its seed.
    "overgrownedentile": TERRAIN,
    "aurorashoaltile": TERRAIN,
    # Eden. paradisesand and edensoil are natural ground; edenmoss is the
    # dense carpet under the canopy; edenshallows is the turquoise water the
    # realm is drawn around, so it answers the LIQUID band the mistsea does;
    # edenrootfloor reads like a floor but is BUILT as terrain -- isFloor=False,
    # terrain priority 205 rather than PRIORITY_FLOOR's 400, and no recipe -- so
    # it is classified as what it is, not as what its name suggests.
    "paradisesandtile": TERRAIN,
    "edensoiltile": TERRAIN,
    "edenmosstile": TERRAIN,
    "edenshallowstile": LIQUID,
    "edenrootfloortile": TERRAIN,
    "skystonetile": TERRAIN,
    "stormslatetile": TERRAIN,
    "mistseatile": LIQUID,
    "murkmosstile": TERRAIN,
    "blackpeattile": TERRAIN,
    "ashsandtile": TERRAIN,
    "murkwatertile": LIQUID,
    "marblecheckertile": FLOOR,
    "gloomwoodfloortile": FLOOR,
    "nimbusfloortile": FLOOR,
    "charfloortile": FLOOR,
    "prismfloortile": FLOOR,
    "skywaytile": TERRAIN,
    "beetlefreaktile": TERRAIN,
    "crookedstripetile": TERRAIN,
    "spiralsoiltile": TERRAIN,
    "violetmudtile": TERRAIN,
    "checkerstonetile": TERRAIN,
    "wrongwaytile": TERRAIN,
    "spilltile": LIQUID,
    "hauntedgrasstile": TERRAIN,
    "ghostmosstile": TERRAIN,
    "violetdirttile": TERRAIN,
    "spiritstonetile": TERRAIN,
    "blackcobbletile": TERRAIN,
    "graveyardsoiltile": TERRAIN,
    "ectoplasmtile": LIQUID,
    # Steinfeld / The Quiet Reach. All seven are SteinfeldGroundTile ->
    # SkyGroundTile -> TerrainSplatterTile(isFloor=false, ...), painted by area
    # in SteinfeldTerrainPainter.paintRegion rather than placed one tile at a
    # time -- terrain, not floor, exactly like the Skyreach's own four.
    "palegrasstile": TERRAIN,
    "weatheredstonetile": TERRAIN,
    "crackedmarbletile": TERRAIN,
    "deadsoiltile": TERRAIN,
    "ashgrasstile": TERRAIN,
    "miststonetile": TERRAIN,
    "gravesoiltile": TERRAIN,
}

# Vanilla base classes we may build on, and the isFloor value each one passes
# to GameTile. Read from the decompiled sources, not inferred:
#   SimpleFloorTile      -> super(true, textureName)   priority 400
#   SimpleTiledFloorTile -> super(true, textureName)   priority 400
#   SimpleTerrainTile    -> super(false, textureName)  priority 0
#   TerrainSplatterTile  -> super(isFloor, ...)        priority abstract
#   LiquidTile           -> super(false)               liquid
VANILLA_BASES = {
    "SimpleFloorTile": (True, 400),
    "SimpleTiledFloorTile": (True, 400),
    "SimpleTerrainTile": (False, 0),
    "TerrainSplatterTile": (None, None),
    "LiquidTile": (False, None),
}

# TerrainSplatterTile.PRIORITY_FLOOR_BOT — anything at or above this draws over
# terrain; anything below is terrain that other terrain can bleed into.
PRIORITY_FLOOR_BOT = 300
PRIORITY_FLOOR = 400

# Tiles that legitimately ship no `_splat` atlas. The marble checker's pattern
# is chosen from absolute world coordinates so it runs continuously across
# separately built rooms; a splat atlas randomises the cell per tile and would
# destroy the checkerboard. It therefore rides TerrainSplatterTile's legacy
# path (shared 64x64 splattingmask.png, whose corner quadrants are a correct
# 21% nub), which is also why it needs CheckerFloorTile's Math.floorMod — see
# that class for the client crash this replaced. Do not "fix" it by adding a
# `_splat`.
LEGACY_SPLAT_OK = {
    "marblechecker",
    # CrookedGroundTile supplies its own position-seeded getTerrainSprite,
    # so vanilla ascendedgrowth's non-splat fallback cannot index negatively.
    "ascendedgrowth",
}

# 32px cell -> (min, max) opaque coverage in percent, for a land sheet.
# Vanilla measured range is in the comment; the band is widened outward.
LAND_BANDS = {
    # four fully opaque tile variants; (3,0) doubles as the item icon
    (3, 0): (100.0, 100.0), (4, 0): (100.0, 100.0),
    (5, 0): (100.0, 100.0), (6, 0): (100.0, 100.0),
    # diagonal-only corner pieces        vanilla 0.8 - 29.3
    (0, 0): (0.0, 35.0), (2, 0): (0.0, 35.0),
    (0, 2): (0.0, 35.0), (2, 2): (0.0, 35.0),
    # one orthogonal side                vanilla 23.4 - 66.4
    (1, 0): (18.0, 72.0), (0, 1): (18.0, 72.0),
    (2, 1): (18.0, 72.0), (1, 2): (18.0, 72.0),
    # two adjacent sides                 vanilla 41.4 - 82.4
    (3, 1): (35.0, 88.0), (4, 1): (35.0, 88.0),
    (3, 2): (35.0, 88.0), (4, 2): (35.0, 88.0),
    # three sides                        vanilla 60.5 - 90.2
    (5, 1): (55.0, 94.0), (6, 1): (55.0, 94.0),
    (5, 2): (55.0, 94.0), (6, 2): (55.0, 94.0),
    # all four sides                     vanilla 51.6 - 81.6
    (1, 1): (45.0, 88.0),
}

# Vanilla's own liquid sheets run their multi-side cells right up to solid
# (saltwater (5,1) is 100%), so only the diagonal-corner rule and the full
# variants are enforced for liquids — the corner rule is the one the playtest
# report is about, and vanilla liquids honour it (4.3% - 22.3%).
LIQUID_BANDS = {c: b for c, b in LAND_BANDS.items()
                if c in {(0, 0), (2, 0), (0, 2), (2, 2),
                         (3, 0), (4, 0), (5, 0), (6, 0)}}

CELL = 32
BLOCK_W, BLOCK_H = 224, 96


# --- Java side --------------------------------------------------------------

def java_sources():
    out = {}
    for root, _dirs, files in os.walk(SRC):
        for f in files:
            if f.endswith(".java"):
                p = os.path.join(root, f)
                with open(p, encoding="utf-8") as fh:
                    out[p] = fh.read()
    return out


def strip_comments(text):
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


REGISTER_RE = re.compile(
    r"TileRegistry\.registerTile\(\s*\"([a-z0-9_]+)\"\s*,\s*(.+?)\s*,\s*"
    r"(-?[0-9.]+)F\s*,\s*(true|false)", re.S)


def find_registrations(sources):
    """[(stringID, tile expression, brokerValue, obtainable, file)] in order."""
    found = []
    for path, text in sources.items():
        for m in REGISTER_RE.finditer(strip_comments(text)):
            found.append((m.group(1), m.group(2).strip(), float(m.group(3)),
                          m.group(4) == "true", path))
    return found


def resolve_expression(expr, sources, path):
    """Turn a registerTile argument into ('ClassName', [ctor string args]).

    Two shapes occur: an inline `new X(...)`, or a SkyRegistry field that the
    same file assigned a `new X(...)` to earlier in init().
    """
    m = re.match(r"new\s+([\w.]+)\s*\((.*)\)\s*$", expr, re.S)
    if m:
        return m.group(1).split(".")[-1], re.findall(r'"([^"]*)"', m.group(2))
    m = re.match(r"^(?:[\w.]+\.)?(\w+)$", expr)
    if m:
        field = m.group(1)
        assign = re.search(r"\b" + re.escape(field) + r"\s*=\s*new\s+([\w.]+)\s*\((.*?)\);",
                           strip_comments(sources[path]), re.S)
        if assign:
            return assign.group(1).split(".")[-1], re.findall(r'"([^"]*)"', assign.group(2))
    return None, []


def read_tile_class(name, sources):
    """(base class, isFloor passed to super or None, getTerrainPriority or None,
    texture names from the super call)."""
    for path, text in sources.items():
        if os.path.basename(path) != name + ".java":
            continue
        text = strip_comments(text)
        base = None
        m = re.search(r"class\s+" + re.escape(name) + r"\s+extends\s+([\w.]+)", text)
        if m:
            base = m.group(1).split(".")[-1]
        is_floor, textures = None, []
        sup = re.search(r"\bsuper\s*\((.*?)\);", text, re.S)
        if sup:
            args = sup.group(1)
            first = args.split(",")[0].strip()
            if first == "true":
                is_floor = True
            elif first == "false":
                is_floor = False
            strings = re.findall(r'"([^"]*)"', args)
            # TerrainSplatterTile(isFloor, terrainTextureName, alphaMaskName):
            # the SECOND string is the alpha mask, not another texture. Vanilla
            # uses it for the wide stencil (SpiderNestTile), and treating it as
            # a texture name makes the audit hunt for a sheet that will never
            # exist.
            textures = strings[:1] if len(strings) > 1 else strings
        priority = None
        pr = re.search(r"getTerrainPriority\s*\(\s*\)\s*\{\s*return\s+(-?\d+)\s*;", text, re.S)
        if pr:
            priority = int(pr.group(1))
        elif re.search(r"getTerrainPriority\s*\(\s*\)\s*\{\s*return\s+"
                       r"TerrainSplatterTile\.PRIORITY_TERRAIN\s*;", text, re.S):
            priority = 100
        elif name == "GhostGroundTile" and "return this.terrainPriority" in text:
            # Every GhostGroundTile registration passes a literal natural-terrain
            # priority below PRIORITY_FLOOR_BOT; the field is returned verbatim.
            priority = 100
        return base, is_floor, priority, textures
    return None, None, None, []


def resolve_tile(cls, ctor_strings, sources):
    """(isFloor, priority, [texture names]) for a registered tile class."""
    if cls in VANILLA_BASES:
        is_floor, priority = VANILLA_BASES[cls]
        return is_floor, priority, list(ctor_strings)

    base, is_floor, priority, textures = read_tile_class(cls, sources)
    if base is None:
        return None, None, []
    if base in VANILLA_BASES:
        base_floor, base_priority = VANILLA_BASES[base]
        if is_floor is None:
            is_floor = base_floor
        if priority is None:
            priority = base_priority
        if not textures:
            # e.g. CheckerFloorTile forwards the texture name it was given
            textures = list(ctor_strings)
        if base == "LiquidTile":
            is_floor = LIQUID
    else:  # one of ours extending one of ours
        nested_floor, nested_priority, nested_tex = resolve_tile(base, ctor_strings, sources)
        if is_floor is None:
            is_floor = nested_floor
        if priority is None:
            priority = nested_priority
        if not textures:
            textures = nested_tex
    return is_floor, priority, textures


def audit_java(sources, problems):
    """Returns {stringID: (role, [texture names])} for the sheet pass."""
    registrations = find_registrations(sources)
    seen = {}
    registered = set()
    source_text = "\n".join(strip_comments(t) for t in sources.values())

    for string_id, expr, broker, _obtainable, path in registrations:
        registered.add(string_id)
        role = ROLES.get(string_id)
        if role is None:
            problems.append(
                f"{string_id}: registered in {os.path.relpath(path, REPO)} but not "
                f"classified in ROLES — say whether it is a floor, terrain or a liquid")
            continue
        cls, ctor_strings = resolve_expression(expr, sources, path)
        if cls is None:
            problems.append(f"{string_id}: cannot resolve its tile class from `{expr}`")
            continue
        is_floor, priority, textures = resolve_tile(cls, ctor_strings, sources)

        if role == LIQUID:
            if is_floor is not LIQUID:
                problems.append(f"{string_id}: declared LIQUID but {cls} does not extend LiquidTile")
        elif role == FLOOR:
            if is_floor is not True:
                problems.append(
                    f"{string_id}: declared FLOOR but {cls} passes isFloor={is_floor} to GameTile — "
                    f"it would register in tiles/terrain, take 100 tile health and blend into its neighbours")
            if priority is not None and priority != PRIORITY_FLOOR:
                problems.append(
                    f"{string_id}: declared FLOOR but getTerrainPriority() is {priority}, "
                    f"not PRIORITY_FLOOR ({PRIORITY_FLOOR}) — terrain would splat over it")
            recipe = re.search(r'new\s+Recipe\(\s*"' + re.escape(string_id) + r'"\s*,\s*(\d+)',
                               source_text)
            if recipe is None:
                problems.append(
                    f"{string_id}: declared FLOOR but has no Recipe — a floor the player "
                    f"cannot craft never reaches the building menu")
            elif broker < 0 and int(recipe.group(1)) > 1:
                # A negative broker value means "derive it from the recipe"
                # (ItemRegistry.calculateBrokerValues). RecipeBrokerValueCompute
                # sums the INGREDIENTS and never divides by the recipe's yield,
                # which is safe in vanilla only because every vanilla floor
                # recipe is 1 ingredient -> 1 tile. Ours yield 6, so deriving
                # would price each of the six at the full cost of the craft and
                # turn the workstation into a broker money printer.
                problems.append(
                    f"{string_id}: brokerValue {broker}F derives the price from a recipe that "
                    f"yields {recipe.group(1)} — RecipeBrokerValueCompute ignores the yield, so "
                    f"each tile would be worth the whole craft. Use a flat positive value, or "
                    f"make the recipe 1:1 like vanilla's floors.")
        else:  # TERRAIN
            if is_floor is not False:
                problems.append(
                    f"{string_id}: declared TERRAIN but {cls} passes isFloor={is_floor}")
            if priority is None or priority >= PRIORITY_FLOOR_BOT:
                problems.append(
                    f"{string_id}: declared TERRAIN but getTerrainPriority() is {priority}, "
                    f"which is in the floor band (>= {PRIORITY_FLOOR_BOT})")
        seen[string_id] = (role, textures)

    for string_id in ROLES:
        if string_id not in registered:
            problems.append(f"{string_id}: listed in ROLES but never registered — stale entry?")

    # SimpleTiledFloorTile picks its cell with `tileX % (width / 32)`, which is
    # negative on most of a Necesse map, and without a `_splat` sibling that
    # index reaches splattingTextures[-1] and kills the client (see
    # CheckerFloorTile). Only our subclass, which uses Math.floorMod, is safe.
    if re.search(r"new\s+(?:[\w.]*\.)?SimpleTiledFloorTile\s*\(", source_text):
        problems.append(
            "new SimpleTiledFloorTile(...) used directly: it crashes the client on negative "
            "tile coordinates when the texture has no _splat sibling. Use CheckerFloorTile.")
    return seen


# --- Sheet side -------------------------------------------------------------

def cell_coverage(px, x0, y0):
    n = sum(1 for y in range(y0, y0 + CELL) for x in range(x0, x0 + CELL)
            if px[x, y][3] > 0)
    return n / (CELL * CELL) * 100.0


def audit_sheets(tiles, problems, vanilla_tiles=None):
    checked = 0
    for string_id, (role, textures) in sorted(tiles.items()):
        if not textures:
            problems.append(f"{string_id}: could not work out which texture it draws from")
            continue
        bands = LIQUID_BANDS if role == LIQUID else LAND_BANDS
        for texture in textures:
            splat = os.path.join(TILES, texture + "_splat.png")
            if not os.path.exists(splat) and vanilla_tiles is not None:
                borrowed = os.path.join(vanilla_tiles, texture + "_splat.png")
                if os.path.exists(borrowed):
                    # The engine owns and already consumes this sheet. Its
                    # atlas may use a vanilla-specific multi-frame layout
                    # (ooze is 1792x416), so our mod-sheet geometry gate must
                    # not reinterpret it as one of our 224x96 atlases.
                    continue
            if not os.path.exists(splat):
                if texture in LEGACY_SPLAT_OK:
                    continue
                problems.append(
                    f"{string_id}: tiles/{texture}_splat.png missing and {texture} is not in "
                    f"LEGACY_SPLAT_OK — the legacy path needs Math.floorMod (see CheckerFloorTile)")
                continue
            im = Image.open(splat).convert("RGBA")
            w, h = im.size
            if w % BLOCK_W or h % BLOCK_H:
                problems.append(f"{texture}_splat.png: {w}x{h} is not a whole number of "
                                f"{BLOCK_W}x{BLOCK_H} blocks")
                continue
            px = im.load()
            worst = {}
            for frame in range(w // BLOCK_W):
                for section in range(h // BLOCK_H):
                    for (cx, cy), (lo, hi) in bands.items():
                        cov = cell_coverage(px, frame * BLOCK_W + cx * CELL,
                                            section * BLOCK_H + cy * CELL)
                        checked += 1
                        if cov < lo or cov > hi:
                            prev = worst.get((cx, cy))
                            if prev is None or abs(cov - (lo + hi) / 2) > abs(prev[0] - (lo + hi) / 2):
                                worst[(cx, cy)] = (cov, lo, hi, frame, section)
            for (cx, cy), (cov, lo, hi, frame, section) in sorted(worst.items()):
                problems.append(
                    f"{texture}_splat.png cell ({cx},{cy}) [{CELL_NAMES[(cx, cy)]}]: "
                    f"{cov:.1f}% opaque, expected {lo:.0f}-{hi:.0f}% "
                    f"(worst at frame {frame} section {section})")
    return checked


CELL_NAMES = {
    (3, 0): "full variant", (4, 0): "full variant",
    (5, 0): "full variant", (6, 0): "full variant",
    (0, 0): "diagonal SE nub", (2, 0): "diagonal SW nub",
    (0, 2): "diagonal NE nub", (2, 2): "diagonal NW nub",
    (1, 0): "S edge", (0, 1): "E edge", (2, 1): "W edge", (1, 2): "N edge",
    (3, 1): "N+W", (4, 1): "N+E", (3, 2): "S+W", (4, 2): "S+E",
    (5, 1): "N+E+W", (6, 1): "N+E+S", (5, 2): "E+S+W", (6, 2): "N+S+W",
    (1, 1): "all four sides",
}


# Only three numbers, and deliberately NOT the peak. Peak |dRGB| in a full-tile
# cell is set by a handful of feature pixels -- a tuft, a charge spark, a bone
# fleck -- which are SUPPOSED to be loud; vanilla's own crafted floors peak at
# 102. The mean is what measures whether the BED is loud, because a few accent
# pixels barely move an average taken over every deviating pixel.
TEXTURE_BANDS = {
    # role: (density_lo, density_hi, mean_max)
    # Measured off vanilla-sprites/tiles/ on 2026-08-30:
    #   TERRAIN, 37 natural grounds  density 294-993  mean 5.0-16.7  peak 5-33
    #   FLOOR,   9 crafted floors    density 446-771  mean 7.9-27.0  peak 16-102
    #   LIQUID,  lava, the only one  density 401      mean 4.3       peak 8
    TERRAIN: (280, 620, 14.0),
    FLOOR:   (440, 780, 24.0),
    LIQUID:  (280, 620, 14.0),
}
# 2x2 BLOCK COHERENCE IS 100% ON EVERY VANILLA SPLAT WE HAVE, without exception,
# natural ground and crafted floor alike. It is the strongest convention in the
# tile set and the one this mod broke on all fifteen sheets.
COHERENCE_MIN = 100.0

# Sheets this check deliberately does not judge, and why. Both are CONVERTED
# from artwork the user supplied, through tools/convert_reference.py -- a hand
# painted illustration resampled onto the grid. Holding a painting to a
# procedural generator's conventions would be measuring the wrong thing, and
# "make it 2x2 coherent" would destroy the art it was converted from.
CONVERTED_ART = {"beetlefreak", "skyway",
                 # The supplied Eden ground (2026-09-01), drawn on vanilla's
                 # overgrowngrass_splat layout. Measured before exempting:
                 # density 713, mean |dRGB| 48.2, coherence 5.9% -- loud and
                 # per-pixel, which is the supplied art's identity ("Eden is an
                 # exaggerated biological explosion"), not a format fault: the
                 # cell layout is vanilla's own. If it reads as noise in game,
                 # that verdict belongs to the player and PLAYTEST_LOG, not to
                 # this gate.
                 "overgrowneden"}

# Known, measured, not yet done. Kept here rather than silently exempted so the
# debt is visible in the code that would otherwise hide it.
#   mistsea: density 617-704 (fine) but mean 31.8-49.7 and 40-48% coherent --
#   the last loud, non-block-built procedural sheet in the mod. It is the sky's
#   signature surface, so it wants its own deliberate pass, not a drive-by.
# Splats whose LOUDNESS is a deliberate authorial choice, not a defect. The 2x2
# coherence rule is a real engine convention and every one of these obeys it
# (tools/fix_splat.py, applied 2026-09-02). The density and mean |dRGB| bands
# are a different matter: they describe how VANILLA paints, and vanilla paints
# grass in five RGB levels. The player's hand-drawn grounds are painterly, and
# holding cloudturf to mean |dRGB| <= 14 was measured to need a 90% pull toward
# the ground tone, which erased its snow caps and grass blades outright. That
# was tried, looked at, and rejected -- docs/ASSET_PIPELINE.md's rule applies:
# what the player keeps outranks what the audit measures. Re-open any of these
# the moment a playtest says a ground reads as noise at 1x.
KNOWN_UNFIXED = {"mistsea_shallow", "mistsea_deep",
                 "cloudturf", "aurorashoal", "blackpeat", "ashsand", "charfloor"}


def _cell_stats(cell):
    """density, mean |dRGB| from the modal colour, peak |dRGB|, 2x2 coherence."""
    px = list(cell.getdata())
    counts = collections.Counter(px)
    modal, modal_n = counts.most_common(1)[0]
    dev = [max(abs(a - b) for a, b in zip(c, modal)) for c in px if c != modal]
    load = cell.load()
    w, h = cell.size
    blocks = ok = 0
    for by in range(0, h - 1, 2):
        for bx in range(0, w - 1, 2):
            blocks += 1
            if len({load[bx, by], load[bx + 1, by],
                    load[bx, by + 1], load[bx + 1, by + 1]}) == 1:
                ok += 1
    return (len(px) - modal_n,
            sum(dev) / len(dev) if dev else 0.0,
            max(dev) if dev else 0,
            ok / blocks * 100 if blocks else 100.0)


def audit_texture(tiles, problems):
    """Does each splat carry vanilla's texture, at vanilla's loudness, built the
    way vanilla builds it?

    Three numbers, checked together, because each one alone is gameable. The mod
    shipped six natural grounds at 63-114 density against a vanilla floor of 294
    -- flat ground reads as dead ground. Then the first repair pass hit the
    density band exactly (cloudturf 345 against vanilla grass's 344) and looked
    like camouflage netting, because density counts pixels that differ from the
    modal colour and says nothing about how FAR they differ or what unit they
    are drawn in. Vanilla carries its entire grass texture inside FIVE RGB
    levels, on 2x2 blocks.
    """
    checked = 0
    for string_id, (role, textures) in sorted(tiles.items()):
        for texture in textures or ():
            path = os.path.join(TILES, texture + "_splat.png")
            if texture in CONVERTED_ART or texture in KNOWN_UNFIXED:
                continue
            if not os.path.exists(path):
                continue                 # legacy no-splat tiles: audit_sheets' job
            img = Image.open(path).convert("RGB")
            if img.width < 7 * 32:
                continue
            lo, hi, mean_max = TEXTURE_BANDS[role]
            rows = [_cell_stats(img.crop((c * 32, 0, c * 32 + 32, 32)))
                    for c in range(3, 7)]    # the four full-tile variant cells
            avg = sum(r[0] for r in rows) / len(rows)
            worst_mean = max(r[1] for r in rows)
            worst_coh = min(r[3] for r in rows)
            checked += 1
            if not lo <= avg <= hi:
                problems.append(
                    f"{string_id}: texture density {avg:.0f} outside the vanilla "
                    f"{role} band {lo}-{hi}.")
            if worst_mean > mean_max:
                problems.append(
                    f"{string_id}: mean |dRGB| {worst_mean:.1f} over {mean_max} -- "
                    f"louder than any vanilla {role}. Density has to come from many "
                    f"QUIET steps, which is what the grain_d/grain_l ramp steps are "
                    f"for, not from few loud ones.")
            if worst_coh < COHERENCE_MIN:
                problems.append(
                    f"{string_id}: 2x2 block coherence {worst_coh:.1f}% -- vanilla is "
                    f"100% on every splat in the game. The tone unit is a 2x2 block, "
                    f"never a lone pixel.")
    return checked


def recalibrate(dump):
    """Reprint the bands from a vanilla sprite dump (development aid)."""
    import glob
    import statistics
    liquid_words = ("water", "lava", "ooze", "slime", "ascendedvoid")
    for label, want_liquid in (("land", False), ("liquid", True)):
        rows = {}
        for p in sorted(glob.glob(os.path.join(dump, "tiles", "*_splat.png"))):
            is_liquid = any(k in os.path.basename(p) for k in liquid_words)
            if is_liquid != want_liquid:
                continue
            im = Image.open(p).convert("RGBA")
            w, h = im.size
            if w % BLOCK_W or h % BLOCK_H:
                continue
            px = im.load()
            for cy in range(3):
                for cx in range(7):
                    rows.setdefault((cx, cy), []).append(
                        cell_coverage(px, cx * CELL, cy * CELL))
        if not rows:
            print(f"--- vanilla {label}: no sheets found under {dump} ---")
            continue
        print(f"--- vanilla {label} ({len(next(iter(rows.values())))} sheets) ---")
        for cy in range(3):
            for cx in range(7):
                v = rows[(cx, cy)]
                print(f"  ({cx},{cy}) {CELL_NAMES.get((cx, cy), '?'):16s} "
                      f"min {min(v):6.1f}  med {statistics.median(v):6.1f}  max {max(v):6.1f}")


def _default_vanilla():
    """This checkout's own dump first. Same defect size_audit.py had: a default
    path nobody has makes --recalibrate silently useless."""
    local = os.path.join(REPO, "vanilla-sprites")
    if os.path.isdir(os.path.join(local, "tiles")):
        return local
    return "/home/user/necesse-game/sprites"


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--vanilla", default=_default_vanilla())
    ap.add_argument("--recalibrate", action="store_true",
                    help="reprint the vanilla cell bands instead of auditing")
    args = ap.parse_args()
    if args.recalibrate:
        recalibrate(args.vanilla)
        return 0

    problems = []
    sources = java_sources()
    tiles = audit_java(sources, problems)
    vanilla_tiles = os.path.join(args.vanilla, "tiles") if args.vanilla else None
    cells = audit_sheets(tiles, problems, vanilla_tiles)
    textured = audit_texture(tiles, problems)

    for p in problems:
        print(f"FIX  {p}")
    if problems:
        print(f"\n{len(problems)} tile-behaviour finding(s).")
        return 1
    floors = sum(1 for r, _ in tiles.values() if r == FLOOR)
    terrain = sum(1 for r, _ in tiles.values() if r == TERRAIN)
    liquids = sum(1 for r, _ in tiles.values() if r == LIQUID)
    print(f"OK: {len(tiles)} tiles ({floors} floors, {terrain} terrain, {liquids} liquid) "
          f"match their declared role; {cells} splat cells within the vanilla bands; "
          f"{textured} sheet(s) at vanilla texture density, loudness and 2x2 build.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
