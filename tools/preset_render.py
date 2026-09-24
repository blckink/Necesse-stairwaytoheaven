#!/usr/bin/env python3
"""Render every showroom exhibit (building, gallery, realm ground) top-down.

WHAT IT DRAWS FROM. Not from the Java sources and not from a model of them:
from `/swhshowroom export`, which the real game writes after `/swhshowroom
build` has stamped every preset with `Preset.applyToLevel` -- every tile, every
object on every object layer (base, carpets, wall decor, table decor) with its
rotation, over each exhibit's capture rectangle, plus a `registry.json` that
names each tile/object's class chain, texture fields and multi-tile shape.
`scripts/preset_render.sh` boots a dedicated server and produces that export;
on the player's PC the same export comes from typing `/swhshowroom export` in
game (it lands in `<Necesse settings>/swh-export/`).

WHAT IS FAITHFUL AND WHAT IS NOT (it says so in each picture's footer too):
  * Ground: the tile's real texture -- a `_splat` atlas's full cell (random
    block and cell, as TerrainSplatterTile does), a tiled floor's cell, a path's
    body cell. Transitions between two grounds are NOT splatted: edges are hard.
  * Walls, doors, windows: `tools/wall_render_preview.WallRenderer`, the
    line-by-line port of WallObject/WallWindowObject/WallDoorObject drawing,
    one wall family at a time.
  * Fences and gates, trees (128px grid), rocks: the same rules
    scripts/sky_map_render.py uses.
  * Everything else: the sheet's first cell for the object's rotation, bottom-
    anchored on its multi-tile footprint. Furniture sheets that hold the four
    rotations side by side are cut by rotation; per-class offsets (bookshelf
    +32, clock, animation frames) are NOT reproduced. Carpets are drawn flat.
  * A sprite that is not on disk is drawn as a labelled colour block in its map
    colour and listed in the legend. Vanilla sprites need a dump
    (`/swhdumpsprites` in game, then --sprites <settings>/swh-sprites).
  * No lighting, no shadows, no animation. The in-game `/swhshots` is the
    faithful picture; this is the fast one.

Usage:
    python3 tools/preset_render.py --data build/qa/presets/data [--sprites DIR] [ids...]
    python3 tools/preset_render.py --data %APPDATA%/Necesse/swh-export --sprites %APPDATA%/Necesse/swh-sprites
Output: <out>/<exhibit>.png (default build/qa/presets/), 1 px = 1 game pixel.
"""
import argparse
import json
import os
import re
import sys
import zlib

from PIL import Image, ImageDraw

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")
sys.path.insert(0, os.path.join(REPO, "tools"))

TILE = 32


def default_sprites():
    for env in ("NECESSE_SPRITES", "NECESSE_VANILLA_SPRITES"):
        v = os.environ.get(env)
        if v and os.path.isdir(v):
            return v
    try:
        from size_audit import default_vanilla
        return default_vanilla()
    except Exception:  # noqa: BLE001 - size_audit is optional here
        return "/home/user/necesse-game/sprites"


def h32(x, y, salt):
    return zlib.crc32(("%d,%d,%d" % (x, y, salt)).encode())


class Sprites:
    """Mod resources first, then the vanilla dump, then the dump's _mod/ copy."""

    def __init__(self, vanilla):
        self.vanilla = vanilla if vanilla and os.path.isdir(vanilla) else None
        self.cache = {}
        self.used_vanilla = set()

    def find(self, folder, name):
        if not name:
            return None
        key = (folder, name)
        if key in self.cache:
            return self.cache[key]
        img = None
        cands = [os.path.join(RES, folder, name + ".png")]
        if self.vanilla:
            cands.append(os.path.join(self.vanilla, folder, name + ".png"))
            cands.append(os.path.join(self.vanilla, "_mod", folder, name + ".png"))
        for i, p in enumerate(cands):
            if os.path.isfile(p):
                img = Image.open(p).convert("RGBA")
                if i == 1:
                    self.used_vanilla.add(folder + "/" + name)
                break
        if img is None and "/" not in name:
            # Some classes load from a subfolder (objects/statues/seraph,
            # objects/carpets/skywatchcarpet) and keep only the base name.
            rel = self.index(folder).get(name)
            if rel is not None:
                img = Image.open(rel).convert("RGBA")
        self.cache[key] = img
        return img

    def index(self, folder):
        key = ("__index__", folder)
        if key not in self.cache:
            found = {}
            roots = [os.path.join(RES, folder)]
            if self.vanilla:
                roots += [os.path.join(self.vanilla, folder), os.path.join(self.vanilla, "_mod", folder)]
            for root in roots:
                for dirpath, _dirs, files in os.walk(root):
                    for f in files:
                        if f.endswith(".png"):
                            found.setdefault(f[:-4], os.path.join(dirpath, f))
            self.cache[key] = found
        return self.cache[key]


# --------------------------------------------------------------- tiles ------

def splat_cell(img, x, y):
    blocks = max(1, img.height // 96)
    b = h32(x, y, 5) % blocks
    col = 3 + h32(x, y, 9) % 4
    return img.crop((col * TILE, b * 96, col * TILE + TILE, b * 96 + TILE))


def tile_sprite(info, x, y, sprites):
    tex = info["textures"]
    cls = info["classes"]
    tid = info["id"]
    if info.get("liquid"):
        base = tid[:-4] if tid.endswith("tile") else tid
        for name in (base + "_shallow_splat", base + "_splat", base + "_deep_splat"):
            img = sprites.find("tiles", name)
            if img is not None and img.width >= 7 * TILE:
                return splat_cell(img, x, y)
        return None
    name = tex.get("terrainTextureName") or tex.get("textureName") or tid
    if "EdgedTiledTexture" in cls or "PathTiledTile" in cls:
        img = sprites.find("tiles", name)
        if img is not None and img.width >= 96:
            return img.crop((64, 0, 96, TILE))
    splat = sprites.find("tiles", name + "_splat")
    if splat is not None and splat.width >= 7 * TILE:
        return splat_cell(splat, x, y)
    img = sprites.find("tiles", name)
    if img is None:
        return None
    cols = max(1, img.width // TILE)
    rows = max(1, img.height // TILE)
    if "SimpleTiledFloorTile" in cls or "CheckerFloorTile" in cls:
        cx, cy = x % cols, y % rows
    else:
        cx, cy = h32(x, y, 3) % cols, h32(x, y, 4) % rows
    return img.crop((cx * TILE, cy * TILE, cx * TILE + TILE, cy * TILE + TILE))


# ------------------------------------------------------------- objects ------

DOOR_RE = re.compile(r"door(open|locked|unlocked)?$")
WINDOW_RE = re.compile(r"window$")


def wall_family(info, registry_by_id):
    """The texture name of the wall sheet an object is drawn from, or None."""
    cls = info["classes"]
    if "WallObject" in cls or "WallWindowObject" in cls:
        return info["textures"].get("textureName") or info["id"]
    if "WallDoorObject" in cls or any(c.startswith("WallDoor") for c in cls):
        prefix = DOOR_RE.sub("", info["id"])
        wall = registry_by_id.get(prefix + "wall")
        if wall is not None:
            return wall["textures"].get("textureName") or wall["id"]
        return prefix + "wall"
    return None


def texture_name(info):
    t = info["textures"]
    for k in ("textureName", "texturePath", "textureID", "texture"):
        if k in t:
            return re.sub(r"^objects/", "", t[k])
    for v in t.values():
        if "outline" not in v:
            return re.sub(r"^objects/", "", v)
    return info["id"]


def is_furniture(cls):
    return any(c in cls for c in ("FurnitureObject", "LampObject", "TableObject",
                                  "ChairObject", "CraftingStationObject"))


def fence_links(cls):
    return any(c in cls for c in ("FenceObject", "FenceGateObject", "WallObject", "RockObject"))


def color_of(info, oid):
    c = info.get("color")
    if c:
        return tuple(c) + (255,)
    v = zlib.crc32(str(oid).encode())
    return (80 + v % 150, 80 + (v >> 8) % 150, 80 + (v >> 16) % 150, 255)


class Canvas:
    def __init__(self, w, h):
        self.img = Image.new("RGBA", (w * TILE, h * TILE), (14, 16, 22, 255))
        self.ops = []
        self.seq = 0

    def add(self, sprite, x, y, sort):
        self.ops.append((sort, self.seq, sprite, x, y))
        self.seq += 1

    def flush(self):
        for _s, _q, sprite, x, y in sorted(self.ops, key=lambda o: (o[0], o[1])):
            self.img.alpha_composite(sprite, (int(x), int(y))) if x >= 0 and y >= 0 else \
                self._paste_clipped(sprite, int(x), int(y))

    def _paste_clipped(self, sprite, x, y):
        cx, cy = max(0, -x), max(0, -y)
        if cx >= sprite.width or cy >= sprite.height:
            return
        self.img.alpha_composite(sprite.crop((cx, cy, sprite.width, sprite.height)), (x + cx, y + cy))


def block(w, h, color, label):
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle((2, 2, w - 3, h - 3), fill=color, outline=(0, 0, 0, 255))
    lum = color[0] * 0.3 + color[1] * 0.59 + color[2] * 0.11
    d.text((4, 3), label[:max(2, w // 6)], fill=(0, 0, 0, 255) if lum > 140 else (255, 255, 255, 255))
    return im


def render(ex, reg, sprites, layer_names):
    w, h = ex["w"], ex["h"]
    canvas = Canvas(w, h)
    tiles = reg["tiles"]
    objs = reg["objects"]
    by_id = {v["id"]: v for v in objs.values()}
    fallback = {}
    approx = set()

    # ground
    for y, row in enumerate(ex["tiles"]):
        for x, t in enumerate(row):
            info = tiles.get(str(t))
            if info is None:
                continue
            sp = tile_sprite(info, ex["x"] + x, ex["y"] + y, sprites)
            if sp is None:
                c = color_of(info, t)
                sp = Image.new("RGBA", (TILE, TILE), c)
                fallback.setdefault("tile " + info["id"], c)
            canvas.img.alpha_composite(sp, (x * TILE, y * TILE))

    layer_of = {int(k): v for k, v in layer_names.items()}
    grid = {}
    for x, y, layer, oid, rot in ex["objects"]:
        grid[(x, y, layer)] = (oid, rot)

    def base_at(x, y):
        v = grid.get((x, y, 0))
        return objs.get(str(v[0])) if v else None

    # walls, one family at a time through the engine port
    families = {}
    for (x, y, layer), (oid, rot) in grid.items():
        if layer != 0:
            continue
        info = objs.get(str(oid))
        if info is None:
            continue
        fam = wall_family(info, by_id)
        if fam:
            families.setdefault(fam, {})[(x, y)] = (info, rot)
    drawn_walls = set()
    try:
        import wall_render_preview as wrp
    except Exception:  # noqa: BLE001
        wrp = None
    for fam, cells in families.items():
        sheet = sprites.find("objects", fam)
        if sheet is None or wrp is None or sheet.width < 352:
            continue
        rows = []
        doorbase = {}
        for y in range(h):
            line = []
            for x in range(w):
                cell = cells.get((x, y))
                if cell is None:
                    line.append(".")
                    continue
                info, rot = cell
                cls = info["classes"]
                if "WallWindowObject" in cls:
                    line.append("O")
                elif "WallObject" in cls:
                    line.append("#")
                else:
                    line.append("d" if info["id"].endswith("open") else "D")
                    doorbase[(x, y)] = rot
            rows.append("".join(line))
        scene = wrp.Scene(fam, rows, doorbase=doorbase)
        wr = wrp.WallRenderer(sheet)
        wr.render_scene(scene, 0, 0)
        for sort, _seq, sp, px, py in wr.ops:
            canvas.add(sp, px, py, sort)
        drawn_walls.update(cells.keys())

    for (x, y, layer), (oid, rot) in sorted(grid.items(), key=lambda kv: (kv[0][1], kv[0][0], kv[0][2])):
        info = objs.get(str(oid))
        if info is None:
            continue
        if layer == 0 and (x, y) in drawn_walls:
            continue
        cls = info["classes"]
        lname = layer_of.get(layer, str(layer))
        mt = info["multitile"][rot % 4] if info.get("multitile") else [0, 0, 1, 1, True]
        mx, my, mw, mh, master = mt
        if not master:
            continue
        fx, fy = x - mx, y - my
        tex = texture_name(info)
        img = sprites.find("objects", tex)
        sort = (fy + mh - 1) * TILE + 16
        if lname == "tile":
            sort = -1000
        elif lname == "wallDecor":
            sort += 9
        elif lname == "tableDecor":
            sort += 12
        if img is None:
            c = color_of(info, oid)
            sp = block(mw * TILE, mh * TILE, c, info["id"])
            canvas.add(sp, fx * TILE, fy * TILE, sort)
            fallback.setdefault(info["id"], c)
            continue
        if "TreeObject" in cls:
            rows_ = max(1, img.height // 128)
            v = h32(ex["x"] + x, ex["y"] + y, 3) % rows_
            canvas.add(img.crop((0, v * 128, min(128, img.width), (v + 1) * 128)), x * TILE - 48, y * TILE - 96, sort)
            continue
        if "RockObject" in cls and img.height >= 96:
            variants = max(1, img.width // TILE)
            v = h32(ex["x"] + x, ex["y"] + y, 3) % variants
            canvas.add(img.crop((v * TILE, 48, (v + 1) * TILE, 96)), x * TILE, y * TILE - 16, sort)
            continue
        if "ModularCarpetObject" in cls:
            cols, rows_ = max(1, img.width // TILE), max(1, img.height // TILE)
            cx, cy = (ex["x"] + x) % cols, (ex["y"] + y) % rows_
            canvas.add(img.crop((cx * TILE, cy * TILE, cx * TILE + TILE, cy * TILE + TILE)), x * TILE, y * TILE, sort)
            continue
        if "FenceObject" in cls and img.width >= 5 * TILE:
            def link(dx, dy):
                n = base_at(x + dx, y + dy)
                return n is not None and fence_links(n["classes"])
            cell = Image.new("RGBA", (TILE, img.height), (0, 0, 0, 0))
            if link(0, -1):
                cell.alpha_composite(img.crop((TILE, 0, 2 * TILE, img.height)))
            cell.alpha_composite(img.crop((0, 0, TILE, img.height)))
            if link(0, 1):
                cell.alpha_composite(img.crop((2 * TILE, 0, 3 * TILE, img.height)))
            if link(-1, 0):
                cell.alpha_composite(img.crop((3 * TILE, 0, 4 * TILE, img.height)))
            if link(1, 0):
                cell.alpha_composite(img.crop((4 * TILE, 0, 5 * TILE, img.height)))
            canvas.add(cell, x * TILE, y * TILE + TILE - img.height, sort)
            continue
        if "FenceGateObject" in cls and img.width >= 5 * TILE:
            up = base_at(x, y - 1)
            down = base_at(x, y + 1)
            vertical = (up is not None and fence_links(up["classes"])) or \
                       (down is not None and fence_links(down["classes"]))
            if not vertical:
                canvas.add(img.crop((TILE, 0, 2 * TILE, img.height)), x * TILE, y * TILE + TILE - img.height + 14, sort)
            else:
                post = img.crop((2 * TILE, 0, 3 * TILE, img.height))
                canvas.add(img.crop((4 * TILE, 0, 5 * TILE, img.height)), x * TILE, y * TILE + TILE - img.height - 14, sort)
                canvas.add(post, x * TILE, y * TILE + TILE - img.height - 14, sort)
                canvas.add(post, x * TILE, y * TILE + TILE - img.height + 14, sort)
            continue
        # generic: rotation column for furniture, a hashed variant otherwise
        fw = mw * TILE
        sp = img
        if img.width > fw and img.width % fw == 0:
            n = img.width // fw
            if is_furniture(cls) and n >= 4:
                col = rot % 4
            else:
                col = h32(ex["x"] + x, ex["y"] + y, 7) % n
            sp = img.crop((col * fw, 0, (col + 1) * fw, img.height))
        if lname == "tile":
            sp = sp.crop((0, 0, min(sp.width, mw * TILE), min(sp.height, mh * TILE)))
            canvas.add(sp, fx * TILE, fy * TILE, sort)
        elif lname == "tableDecor":
            canvas.add(sp, fx * TILE + (fw - sp.width) // 2, (fy + mh) * TILE - sp.height - 14, sort)
        elif lname == "wallDecor":
            canvas.add(sp, fx * TILE + (fw - sp.width) // 2, (fy + mh) * TILE - sp.height - 20, sort)
        else:
            canvas.add(sp, fx * TILE + (fw - sp.width) // 2, (fy + mh) * TILE - sp.height, sort)
        if is_furniture(cls) or img.height > (mh + 3) * TILE:
            approx.add(info["id"])
    canvas.flush()

    # footprint outline, so where the preset ends is visible
    d = ImageDraw.Draw(canvas.img)
    fpx, fpy, fpw, fph = ex["footprint"]
    d.rectangle((fpx * TILE, fpy * TILE, (fpx + fpw) * TILE - 1, (fpy + fph) * TILE - 1),
                outline=(255, 210, 90, 110))
    # what each sign says (the gallery's names), as a caption on the sign
    for sx, sy, text in ex.get("signs", []):
        label = text.split("\n")[0]
        label = re.sub(r"^(Showroom|Ausstellung): ", "", label)[:18]
        if not label:
            continue
        tw = 6 * len(label)
        px = max(0, min(canvas.img.width - tw - 4, sx * TILE + 16 - tw // 2))
        # neighbouring captions in a dense gallery row alternate height
        py = sy * TILE + 20 + (12 if (sx // 3) % 2 else 0)
        d.rectangle((px - 2, py - 1, px + tw + 2, py + 11), fill=(0, 0, 0, 190))
        d.text((px, py), label, fill=(255, 240, 180, 255))
    return canvas.img, fallback, approx


def compose(ex, body, fallback, approx, sprites_dir):
    lines = ["%s  [%s, %s]  %dx%d tiles at %d,%d  (footprint %dx%d)" % (
        ex["id"], ex["kind"], ex["group"], ex["w"], ex["h"], ex["x"], ex["y"],
        ex["footprint"][2], ex["footprint"][3])]
    lines.append("sprites: mod resources + %s" % (sprites_dir or "NO vanilla dump (vanilla = colour blocks)"))
    if approx:
        lines.append("approximate cell (per-class offsets not reproduced): " + ", ".join(sorted(approx)[:12])
                     + (" ..." if len(approx) > 12 else ""))
    legend = sorted(fallback.items())
    per_row = max(1, body.width // 190)
    legend_rows = (len(legend) + per_row - 1) // per_row
    top = 14 * len(lines) + 8
    bottom = 16 * legend_rows + (8 if legend else 0)
    out = Image.new("RGBA", (max(body.width, 600), body.height + top + bottom), (8, 9, 12, 255))
    out.alpha_composite(body, (0, top))
    d = ImageDraw.Draw(out)
    for i, line in enumerate(lines):
        d.text((6, 4 + 14 * i), line, fill=(255, 235, 170, 255) if i == 0 else (190, 190, 200, 255))
    for i, (name, c) in enumerate(legend):
        cx = 6 + (i % per_row) * 190
        cy = top + body.height + 4 + (i // per_row) * 16
        d.rectangle((cx, cy + 1, cx + 11, cy + 12), fill=c, outline=(0, 0, 0, 255))
        d.text((cx + 16, cy + 1), ("no sprite: " if not legend_rows else "") + name[:28], fill=(220, 220, 220, 255))
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--data", default=os.path.join(REPO, "build", "qa", "presets", "data"))
    ap.add_argument("--out", default=os.path.join(REPO, "build", "qa", "presets"))
    ap.add_argument("--sprites", default=default_sprites(),
                    help="vanilla sprite dump (from /swhdumpsprites); missing = colour blocks")
    ap.add_argument("ids", nargs="*")
    args = ap.parse_args()
    reg = json.load(open(os.path.join(args.data, "registry.json"), encoding="utf-8"))
    ids = args.ids or json.load(open(os.path.join(args.data, "index.json"), encoding="utf-8"))
    sprites = Sprites(args.sprites)
    os.makedirs(args.out, exist_ok=True)
    layer_names = reg.get("layers", {"0": "base", "1": "tile", "2": "wallDecor", "3": "tableDecor"})
    missing_total = set()
    for eid in ids:
        path = os.path.join(args.data, eid + ".json")
        if not os.path.isfile(path):
            print("skip %s: no export" % eid)
            continue
        ex = json.load(open(path, encoding="utf-8"))
        body, fallback, approx = render(ex, reg, sprites, layer_names)
        img = compose(ex, body, fallback, approx, sprites.vanilla)
        out = os.path.join(args.out, eid + ".png")
        img.save(out)
        missing_total.update(fallback)
        print("%-28s %4dx%-4d  colour blocks: %d" % (eid, img.width, img.height, len(fallback)))
    print("rendered %d exhibit(s) into %s; %d distinct tile/object(s) without a sprite%s"
          % (len(ids), args.out, len(missing_total),
             "" if sprites.vanilla else " (no vanilla dump: pass --sprites)"))


if __name__ == "__main__":
    main()
