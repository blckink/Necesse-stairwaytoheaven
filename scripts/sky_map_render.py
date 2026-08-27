#!/usr/bin/env python3
"""Composite a SkyMapDump into a 1:1 picture of what the player actually sees.

The point of this tool is stated in docs/TECHNICAL_LEARNINGS.md: a worldgen
field must be calibrated at SCREEN scale. Necesse shows roughly 40x22 tiles at
default zoom, so a 40x22 render at 32 px/tile is one screen, pixel for pixel.
A 300-tile overview is useful for checking network topology and nothing else --
it makes a good 8-tile garden look like a speck and a carpet look like texture.

Terrain is composited from the REAL tile sheets (the mod's `_splat` atlases and
vanilla's edged path sheet), because the ground is most of the screen and the
question "does this road read" is mostly a ground question. Objects are
composited from their real sprites using each family's bottom-anchor draw rule
(`drawY - height + 32`, `drawX - width/2 + 16`), which is what every object
class in the game does; edge/connection variants and lighting are not
simulated, so fences and walls read slightly plainer here than in game.

Usage: scripts/sky_map_render.sh   (builds the dump and calls this)
"""

import os
import sys
import zlib
from collections import Counter

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD_TILES = os.path.join(REPO, "src/main/resources/tiles")
MOD_OBJECTS = os.path.join(REPO, "src/main/resources/objects")
GAME_SPRITES = os.environ.get("NECESSE_SPRITES", "/home/user/necesse-game/sprites")

TILE = 32

# ---------------------------------------------------------------- terrain ----
# A `_splat` atlas is stacked 224x96 blocks; cells (3,0)..(6,0) are the four
# fully-opaque "nothing is blending here" variants (docs/research/splat-format.md).
SPLAT_FULL_CELLS = [(3, 0), (4, 0), (5, 0), (6, 0)]

TERRAIN = {
    "cloudturf": ("mod", "cloudturf_splat.png", "splat"),
    "skystoneTile": ("mod", "skystone_splat.png", "splat"),
    "stormslate": ("mod", "stormslate_splat.png", "splat"),
    "mistsea": ("mod", "mistsea_shallow_splat.png", "splat"),
    "skyplinth": ("mod", "marblechecker.png", "checker"),
    # vanilla snowstonepath.png: EdgedTiledTexture, body cells live at x >= 64
    "skyroad": ("game", "snowstonepath.png", "edged"),
}

# ---------------------------------------------------------------- objects ----
# name -> (source, file, variant_width). Height comes from the file; every
# object is drawn bottom-anchored on its tile.
OBJECTS = {
    # flora
    "skyreeds": ("mod", "skyreeds.png", 32),
    "windwheat": ("mod", "windwheat.png", 32),
    "cloudberryBush": ("mod", "cloudberrybush.png", 32),
    "cloudbell": ("mod", "cloudbell.png", 32),
    "skytulip": ("mod", "skytulip.png", 32),
    "staticmoss": ("mod", "staticmoss.png", 32),
    "thunderbloom": ("mod", "thunderbloom.png", 32),
    "glowfern": ("mod", "glowfern.png", 32),
    "auroralily": ("mod", "auroralily.png", 32),
    "tallcloudgrass": ("mod", "tallcloudgrass.png", 32),
    "stormsedge": ("mod", "stormsedge.png", 32),
    "prismgrass": ("mod", "prismgrass.png", 32),
    # trees
    "nimbuswillow": ("mod", "nimbuswillow.png", 128),
    "fulgurpine": ("mod", "fulgurpine.png", 128),
    "prismabirch": ("mod", "prismabirch.png", 128),
    # geology
    "skystoneRock": ("mod", "skystonerock.png", "rock"),
    "aetheriumRock": ("mod", "skystonerock.png", "rock"),
    "fulguriteRock": ("mod", "skystonerock.png", "rock"),
    "prismshardRock": ("mod", "skystonerock.png", "rock"),
    "stormCrystal": ("mod", "stormcrystal.png", 64),
    "stormCrystalR": (None, None, None),
    "auroraBloom": ("mod", "aurorabloom.png", 64),
    "auroraBloomR": (None, None, None),
    # built landscape
    "wardenCandelabra": ("mod", "wardencandelabra.png", 32),
    "skylichen": ("mod", "skylichen.png", 32),
    "cragbloom": ("mod", "cragbloom.png", 32),
    "skyscree": ("mod", "skyscree.png", 32),
    "skyironFence": ("mod", "skyironfence.png", "fence"),
    "skyironFenceGate": ("mod", "skyironfencegate.png", "gate"),
    "skystoneBrickWall": ("mod", "skystonebrickwall.png", "wall"),
    "nightfellWall": ("mod", "nightfellwall.png", "wall"),
    "gloomRavenStatue": ("mod", "statues/gloomraven.png", 32),
    "skywatchRubble": ("mod", "skywatchrubble.png", 32),
    "chargeCrystal": ("mod", "chargecrystal.png", 32),
    "auroraShards": ("mod", "aurorashards.png", 32),
    "starfall": ("mod", "starfall.png", 32),
    "skywatchTelescope": ("mod", "skywatchtelescope.png", 32),
    "skywatchAstrolabe": ("mod", "skywatchastrolabe.png", 32),
}

_cache = {}


def load(source, name):
    key = (source, name)
    if key not in _cache:
        base = MOD_TILES if source == "modtile" else (MOD_OBJECTS if source == "mod" else GAME_SPRITES)
        if source == "game":
            base = os.path.join(GAME_SPRITES, "tiles")
        path = os.path.join(base, name)
        _cache[key] = Image.open(path).convert("RGBA")
    return _cache[key]


def hash2(x, y, salt=0):
    return zlib.crc32(("%d,%d,%d" % (x, y, salt)).encode())


def terrain_sprite(name, x, y):
    spec = TERRAIN.get(name)
    if spec is None:
        return None
    source, filename, kind = spec
    if source == "mod":
        img = load("modtile", filename)
    else:
        img = load("game", filename)
    if kind == "splat":
        sections = max(1, img.height // 96)
        section = hash2(x, y, 5) % sections
        cx, cy = SPLAT_FULL_CELLS[hash2(x, y, 9) % 4]
        return img.crop((cx * TILE, section * 96 + cy * TILE, (cx + 1) * TILE, section * 96 + (cy + 1) * TILE))
    if kind == "checker":
        cols = max(1, img.width // TILE)
        rows = max(1, img.height // TILE)
        cx, cy = x % cols, y % rows
        return img.crop((cx * TILE, cy * TILE, (cx + 1) * TILE, (cy + 1) * TILE))
    # edged: body cells start two columns in
    return img.crop((64, 0, 64 + TILE, TILE))


# What a fence links to, per FenceObject.attachesToObject: other fences, walls
# and rocks. Needed because a lone post and a connected railing are completely
# different amounts of visual mass, and the railing is the one being judged.
FENCE_LINKS = {"skyironFence", "skyironFenceGate", "skystoneBrickWall", "nightfellWall",
               "skystoneRock", "aetheriumRock", "fulguriteRock", "prismshardRock"}


def object_sprite(name, x, y, neighbours=None):
    spec = OBJECTS.get(name)
    if spec is None or spec[0] is None:
        return None
    source, filename, mode = spec
    img = load(source, filename)
    if mode == "rock":
        # RockObject: the visible body of a lone rock is the stacked face pair,
        # rows 3/4 of a 16px grid, drawn from 16px above the tile.
        variants = max(1, img.width // TILE)
        v = hash2(x, y, 3) % variants
        return img.crop((v * TILE, 3 * 16, (v + 1) * TILE, 3 * 16 + 48)), 48 - TILE
    if mode == "fence":
        # FenceObject.addDrawables: column 0 is the post, 1/2 the vertical
        # connectors, 3 left, 4 right; all bottom-anchored on the tile.
        top, bot, left, right = neighbours or (None, None, None, None)
        cell = Image.new("RGBA", (TILE, img.height), (0, 0, 0, 0))
        if top in FENCE_LINKS:
            cell.alpha_composite(img.crop((TILE, 0, TILE * 2, img.height)))
        cell.alpha_composite(img.crop((0, 0, TILE, img.height)))
        if bot in FENCE_LINKS:
            cell.alpha_composite(img.crop((TILE * 2, 0, TILE * 3, img.height)))
        if left in FENCE_LINKS:
            cell.alpha_composite(img.crop((TILE * 3, 0, TILE * 4, img.height)))
        if right in FENCE_LINKS:
            cell.alpha_composite(img.crop((TILE * 4, 0, TILE * 5, img.height)))
        return cell, img.height - TILE
    if mode == "gate":
        # FenceGateObject.addDrawables: rotation 0/2 (attached east-west) draws
        # the whole closed cell, column 1, at the tile. Rotation 1/3 (attached
        # north-south) draws column 2 twice, at drawY-14 and drawY+14, with the
        # closed leaf, column 4, at drawY-14.
        top, bot, left, right = neighbours or (None, None, None, None)
        vertical = top in FENCE_LINKS or bot in FENCE_LINKS
        cell = Image.new("RGBA", (TILE, img.height + 16), (0, 0, 0, 0))
        if not vertical:
            cell.alpha_composite(img.crop((TILE, 0, TILE * 2, img.height)), (0, 14))
        else:
            post = img.crop((TILE * 2, 0, TILE * 3, img.height))
            cell.alpha_composite(img.crop((TILE * 4, 0, TILE * 5, img.height)), (0, 0))
            cell.alpha_composite(post, (0, 0))
            cell.alpha_composite(post, (0, 28))
        return cell, cell.height - TILE - 14
    if mode == "wall":
        # Wall sheets are connection atlases; cell (0,1) is a plain body block.
        return img.crop((0, TILE, TILE, TILE * 2)), 0
    width = mode
    variants = max(1, img.width // width)
    v = hash2(x, y, 3) % variants
    return img.crop((v * width, 0, (v + 1) * width, img.height)), img.height - TILE


def render(dump_path, out_path, scale=1):
    header = None
    rows = []
    with open(dump_path) as f:
        for line in f:
            if line.startswith("#"):
                header = line.strip()
                continue
            rows.append([cell.split("/") for cell in line.split()])
    h = len(rows)
    w = len(rows[0]) if h else 0
    meta = dict(part.split("=") for part in header[2:].split())
    x0, y0 = int(meta["x0"]), int(meta["y0"])

    img = Image.new("RGBA", (w * TILE, h * TILE), (12, 14, 20, 255))
    for ry, row in enumerate(rows):
        for rx, cell in enumerate(row):
            sprite = terrain_sprite(cell[0], x0 + rx, y0 + ry)
            if sprite is not None:
                img.alpha_composite(sprite, (rx * TILE, ry * TILE))
    # Objects after all terrain, in row order, so taller sprites overlap the
    # row behind them exactly like the game's sorted drawable list does.
    def obj(rx, ry):
        if 0 <= ry < h and 0 <= rx < w:
            return rows[ry][rx][1]
        return None

    for ry, row in enumerate(rows):
        for rx, cell in enumerate(row):
            got = object_sprite(cell[1], x0 + rx, y0 + ry,
                                (obj(rx, ry - 1), obj(rx, ry + 1), obj(rx - 1, ry), obj(rx + 1, ry)))
            if got is None:
                continue
            sprite, overhang = got
            px = rx * TILE - (sprite.width - TILE) // 2
            py = ry * TILE - overhang
            img.alpha_composite(sprite, (max(0, px), max(0, py)),
                                (max(0, -px), max(0, -py), sprite.width, sprite.height))
    if scale != 1:
        img = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
    img.save(out_path)
    return rows, meta


def stats(rows):
    tiles = Counter()
    objects = Counter()
    built = 0
    for row in rows:
        for cell in row:
            tiles[cell[0]] += 1
            if cell[1] != "-":
                objects[cell[1]] += 1
            if cell[3] == "B":
                built += 1
    total = sum(tiles.values())
    land = total - tiles.get("mistsea", 0)
    return tiles, objects, built, total, land


def main():
    dump = sys.argv[1]
    out = sys.argv[2]
    scale = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    rows, meta = render(dump, out, scale)
    tiles, objects, built, total, land = stats(rows)
    print("%s  %sx%s tiles at %s,%s" % (os.path.basename(out), meta["w"], meta["h"], meta["x0"], meta["y0"]))
    print("  land %d/%d (%.0f%%)   built %d (%.1f%% of land)"
          % (land, total, 100.0 * land / max(1, total), built, 100.0 * built / max(1, land)))
    road = tiles.get("skyroad", 0)
    inlay = tiles.get("skyplinth", 0)
    print("  paved %d (%.1f%% of land)  chequered accent %d (%.1f%%)"
          % (road, 100.0 * road / max(1, land), inlay, 100.0 * inlay / max(1, land)))
    top = ", ".join("%s x%d" % kv for kv in objects.most_common(10))
    print("  objects: %s" % (top or "none"))


if __name__ == "__main__":
    main()
