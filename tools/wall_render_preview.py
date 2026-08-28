#!/usr/bin/env python3
"""Wall contact sheet: assemble a 352x128 wall sheet the way the ENGINE does.

`sheet_format_audit.py` proves a wall sheet's cells are in the right PLACE and
the right SIZE. It cannot prove the art inside them joins up, and that is the
failure mode a player actually sees: "kein Rand stimmt" -- every cell drawn as
a nice little picture, none of them agreeing with its neighbour.

The only way to see that before shipping is to run the engine's own cell
selection over a synthetic level and look at the result. This is a line-by-line
port of `necesse.level.gameObject.WallObject.addWallDrawOptions` (both
overloads), `WallWindowObject.addWallDrawOptions` and the two
`WallDoorObject` draw paths, from the 1.3.2 decompile.

What the port establishes, and what the art therefore has to satisfy:

  A wall tile is drawn as SIX 16px cells over three 16px bands --
  drawY-16 (the top/roof band), drawY (upper half) and drawY+16 (lower half) --
  so a tile occupies 48px of screen and consecutive tiles OVERLAP by 16px.

  Left half is drawn at drawX, right half at drawX+16, and WHICH COLUMN holds
  which half is not constant down the sheet:

      rows 0, 3, 4   col 0 = left/closed   col 2 = left/open
                     col 1 = right/open    col 3 = right/closed
      rows 1, 2      col 0 = left/closed   col 1 = left/open
                     col 2 = right/open    col 3 = right/closed

  Columns 1 and 2 SWAP roles between the two row groups. Paint a single
  continuous picture across cols 0..3 and half the sheet renders mirrored.

  Vertical grammar for a run of N tiles, top to bottom:
      row 0                        (top cap, 16px)
      (row 2, row 1) x (N-1)       (repeating body, 32px per tile)
      row 3, row 4                 (foot, 32px)

  Cells (2,5) (3,5) (2,6) (3,6) (2,7) (3,7) are UNREACHABLE from WallObject:
  the public overload passes the same boolean[] as adj, sameWall AND isWall, so
  every `isWall[n] && !sameWall[n]` branch is dead. They are kept filled to
  match vanilla, but nothing renders them.

Usage:
    python3 tools/wall_render_preview.py                 # every mod wall sheet
    python3 tools/wall_render_preview.py --sheet a.png --sheet b.png
    python3 tools/wall_render_preview.py --out build/qa

Writes <out>/wall_<name>_dark.png and <out>/wall_<name>_light.png, 4x nearest.
"""
import argparse
import os
import sys

from PIL import Image, ImageDraw

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")

def _vanilla_sprites():
    """Local-only QA reference; never required for the build."""
    env = os.environ.get("NECESSE_SPRITES")
    if env:
        return env
    game = os.environ.get("NECESSE_GAME_DIR")
    cands = []
    if game:
        cands.append(os.path.join(os.path.dirname(os.path.abspath(game)),
                                  "sprites"))
    cands.append(os.path.expanduser("~/necesse-game/sprites"))
    cands.append("/home/user/necesse-game/sprites")
    for c in cands:
        if os.path.isdir(c):
            return c
    return cands[-1]


VANILLA_SPRITES = _vanilla_sprites()

MOD_WALL_SHEETS = ("beetlewall", "cloudmarblewall",
                   "skystonebrickwall", "nightfellwall")

# Level.adjacentGetters order: 0 TL, 1 T, 2 TR, 3 L, 4 R, 5 BL, 6 B, 7 BR
ADJ = ((-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1))

TL, T, TR, L, R, BL, B, BR = range(8)


class Scene:
    """A tiny synthetic level: '#' wall, 'D'/'d' door, 'O' window, '.' empty."""

    def __init__(self, title, rows, doorbase=None):
        self.title = title
        self.rows = rows
        self.h = len(rows)
        self.w = max(len(r) for r in rows)
        # per-tile stored object rotation for doors, default 0
        self.doorbase = doorbase or {}

    def at(self, x, y):
        if y < 0 or y >= self.h:
            return "."
        row = self.rows[y]
        if x < 0 or x >= len(row):
            return "."
        return row[x]

    def is_wall(self, x, y):
        return self.at(x, y) in "#O"

    def is_door(self, x, y):
        return self.at(x, y) in "Dd"

    def attaches(self, x, y):
        """WallDoorObject.attachesToObject: wall/fence/rock and NOT a door."""
        return self.is_wall(x, y)


class WallRenderer:
    """Port of the engine's wall draw. Collects (sort, seq, img, x, y)."""

    def __init__(self, sheet, outline=None):
        self.sheet = sheet
        self.outline = outline
        self.ops = []
        self.seq = 0

    # --- texture accessors -------------------------------------------------
    def sprite16(self, tex, col, row):
        return tex.crop((col * 16, row * 16, col * 16 + 16, row * 16 + 16))

    def sprite_door(self, tex, idx):
        return tex.crop((idx * 32, 0, idx * 32 + 32, 128))

    def add(self, img, x, y, sort):
        self.ops.append((sort, self.seq, img, x, y))
        self.seq += 1

    # --- WallObject.addWallDrawOptions (protected 11-arg overload) ----------
    def wall_cells(self, adj, force_draw_top, force_remove_bot):
        """Return [(col, row, dx, dy)] exactly as the engine emits them.

        adj is the 8-boolean neighbour array; the public overload passes it as
        adj, sameWall AND isWall, so isWall[n] == adj[n] here (see module doc).
        """
        out = []
        top, left, right = adj[T], adj[L], adj[R]
        bot_left, bot, bot_right = adj[BL], adj[B], adj[BR]
        top_left, top_right = adj[TL], adj[TR]
        is_wall_top = adj[T]

        if not top:
            out.append((2 if left else 0, 0, 0, -16))
            out.append((1 if right else 3, 0, 16, -16))
        else:
            if is_wall_top and force_draw_top:      # !sameWall[1] is dead here
                if not left:
                    out.append((0, 1, 0, -16))
                elif not top_left:
                    out.append((0, 7, 0, -16))
                if not right:
                    out.append((3, 1, 16, -16))
                elif not top_right:
                    out.append((1, 7, 16, -16))
            if left and (not bot or not bot_left) and top_left:
                if right and top_right:
                    out.append((2, 1, 16, -16))
                out.append((1, 1, 0, -16))
            if right and (not bot or not bot_right) and top_right:
                out.append((2, 1, 16, -16))
                if left and top_left:
                    out.append((1, 1, 0, -16))

        if bot:
            if left:
                if bot_left:
                    out.append((1, 2, 0, 0))
                    if not force_remove_bot:
                        out.append((1, 1, 0, 16))
                else:
                    out.append((0, 5, 0, 0))        # isWall[5] branch is dead
                    if not force_remove_bot:
                        out.append((0, 6, 0, 16))
            elif bot_left:
                out.append((0, 2, 0, 0))
                if not force_remove_bot:
                    out.append((0, 7, 0, 16))
            else:
                out.append((0, 2, 0, 0))
                if not force_remove_bot:
                    out.append((0, 1, 0, 16))

            if right:
                if bot_right:
                    out.append((2, 2, 16, 0))
                    if not force_remove_bot:
                        out.append((2, 1, 16, 16))
                else:
                    out.append((1, 5, 16, 0))       # isWall[7] branch is dead
                    if not force_remove_bot:
                        out.append((1, 6, 16, 16))
            elif bot_right:
                out.append((3, 2, 16, 0))
                if not force_remove_bot:
                    out.append((1, 7, 16, 16))
            else:
                out.append((3, 2, 16, 0))
                if not force_remove_bot:
                    out.append((3, 1, 16, 16))
        else:
            # botWall = isWall[6] == adj[B] == False here, so the (2,5) (3,5)
            # (2,6) (3,6) branches are unreachable.
            if left:
                out.append((2, 3, 0, 0))
                out.append((2, 4, 0, 16))
            else:
                out.append((0, 3, 0, 0))
                out.append((0, 4, 0, 16))
            if right:
                out.append((1, 3, 16, 0))
                out.append((1, 4, 16, 16))
            else:
                out.append((3, 3, 16, 0))
                out.append((3, 4, 16, 16))
        return out

    # --- WallWindowObject.getWindowDir / addWallDrawOptions -----------------
    @staticmethod
    def window_dir(up, right, bot, left):
        if up and bot:
            return 1 if (not left and not right) else -1
        if not left or not right:
            return -1
        return 0 if (not up and not bot) else -1

    def window_cells(self, adj):
        d = self.window_dir(adj[T], adj[R], adj[B], adj[L])
        if d == 1:
            return [(4, 0, 0, -16), (5, 0, 16, -16),
                    (4, 1, 0, 0), (5, 1, 16, 0)]
        return [(4, 2, 0, -64), (5, 2, 16, -64),
                (4, 3, 0, -48), (5, 3, 16, -48),
                (4, 4, 0, -32), (5, 4, 16, -32),
                (4, 5, 0, -16), (5, 5, 16, -16),
                (4, 6, 0, 0), (5, 6, 16, 0),
                (4, 7, 0, 16), (5, 7, 16, 16)]

    # --- scene walk --------------------------------------------------------
    def render_scene(self, scene, ox, oy):
        for ty in range(scene.h):
            for tx in range(scene.w):
                ch = scene.at(tx, ty)
                if ch == ".":
                    continue
                dx, dy = ox + tx * 32, oy + ty * 32
                if ch in "Dd":
                    self.draw_door(scene, tx, ty, dx, dy, ch == "d")
                    continue
                adj = [scene.is_wall(tx + o[0], ty + o[1]) for o in ADJ]
                # isWallDrawingTop() is true only for windows
                force_draw_top = (adj[T] and scene.at(tx, ty - 1) == "O")
                force_remove_bot = (adj[B] and scene.at(tx, ty + 1) == "O")
                if ch == "O":
                    cells = self.window_cells(adj)
                else:
                    cells = self.wall_cells(adj, force_draw_top,
                                            force_remove_bot)
                sort = ty * 32 + 20
                for col, row, cx, cy in cells:
                    self.add(self.sprite16(self.sheet, col, row),
                             dx + cx, dy + cy, sort)
                    if self.outline is not None:
                        self.add(self.sprite16(self.outline, col, row),
                                 dx + cx, dy + cy, sort)

    # --- WallDoorObject.addDrawables ---------------------------------------
    def rotate_towards_attachment(self, scene, tx, ty, base):
        top = scene.attaches(tx, ty - 1)
        bot = scene.attaches(tx, ty + 1)
        left = scene.attaches(tx - 1, ty)
        right = scene.attaches(tx + 1, ty)
        rot = base
        if top or bot:
            rot = 1 if base in (0, 1) else 3
        elif left or right:
            rot = 0 if base in (0, 1) else 2
        return rot

    def should_mirror(self, scene, tx, ty, rot):
        if rot == 0:
            return scene.is_door(tx + 1, ty)
        if rot == 1:
            return scene.is_door(tx, ty - 1)
        if rot == 2:
            return scene.is_door(tx - 1, ty)
        return scene.is_door(tx, ty + 1)

    def draw_door(self, scene, tx, ty, dx, dy, is_open):
        base = scene.doorbase.get((tx, ty), 0)
        rot = self.rotate_towards_attachment(scene, tx, ty, base)
        mirror = self.should_mirror(scene, tx, ty, rot)
        shift = 0
        if not is_open:
            idx = {0: 3, 1: 5, 2: 7, 3: 9}[rot]
            sort_y = 28 if rot in (0, 2) else 20
            mirror_ok = rot in (0, 2)
        else:
            idx = {0: 4, 1: 6, 2: 8, 3: 10}[rot]
            mirror_ok = rot in (0, 2)
            if rot == 0:
                sort_y = 20
            elif rot == 1:
                if mirror and scene.is_wall(tx, ty + 1):
                    shift += 26
                sort_y = 28 if mirror else 4
            elif rot == 2:
                sort_y = 20
            else:
                if scene.is_wall(tx, ty + 1):
                    shift += 8
                if mirror:
                    shift -= 26
                sort_y = 4 if mirror else 28
        img = self.sprite_door(self.sheet, idx)
        if mirror and mirror_ok:
            img = img.transpose(Image.FLIP_LEFT_RIGHT)
        self.add(img, dx, dy + shift - 96, ty * 32 + sort_y)
        if self.outline is not None:
            oimg = self.sprite_door(self.outline, idx)
            if mirror and mirror_ok:
                oimg = oimg.transpose(Image.FLIP_LEFT_RIGHT)
            self.add(oimg, dx, dy + shift - 96, ty * 32 + sort_y)

    # --- flush -------------------------------------------------------------
    def paint(self, canvas):
        for _sort, _seq, img, x, y in sorted(self.ops, key=lambda o: (o[0],
                                                                     o[1])):
            canvas.alpha_composite(img, (x, y))


# ---------------------------------------------------------------------------
# scenes
# ---------------------------------------------------------------------------
SCENES = [
    Scene("solid block 6x3", [
        "######",
        "######",
        "######",
    ]),
    Scene("L corner", [
        "###...",
        "#.....",
        "#.....",
        "#.....",
    ]),
    Scene("single + pairs", [
        "#.##.#",
        "....#.",
        "....#.",
    ]),
    Scene("T junction + hole", [
        "#####.",
        "..#...",
        "..#...",
        "..#...",
    ]),
    Scene("door in E-W wall (rot 0 / rot 2 / pair)", [
        "#D#D##",
        "......",
    ], doorbase={(3, 0): 2}),
    Scene("door open E-W + door in N-S wall", [
        "#d#.#.",
        "....D.",
        "....#.",
    ]),
    Scene("windows: E-W run and N-S run", [
        "#O#.#.",
        "....O.",
        "....#.",
    ]),
    Scene("room with window + door", [
        "######",
        "#....#",
        "O....#",
        "#....#",
        "##D###",
    ]),
]


def tile_backdrop(splat_name, size):
    """A 32x32 plain variant from a _splat atlas, tiled to `size`."""
    p = os.path.join(RES, "tiles", splat_name + "_splat.png")
    if not os.path.exists(p):
        return None
    atlas = Image.open(p).convert("RGBA")
    tile = atlas.crop((3 * 32, 0, 4 * 32, 32))
    out = Image.new("RGBA", size)
    for y in range(0, size[1], 32):
        for x in range(0, size[0], 32):
            out.alpha_composite(tile, (x, y))
    return out


def build_sheet(sheet_path, outline_path, backdrop, flat_rgb, label):
    sheet = Image.open(sheet_path).convert("RGBA")
    if sheet.size != (352, 128):
        raise SystemExit("%s is %s, expected (352, 128)" % (sheet_path,
                                                            sheet.size))
    outline = None
    if outline_path and os.path.exists(outline_path):
        outline = Image.open(outline_path).convert("RGBA")

    pad = 16
    cols = 2
    cell_w = max(s.w for s in SCENES) * 32 + pad * 2
    cell_h = max(s.h for s in SCENES) * 32 + pad * 2 + 40
    rows = (len(SCENES) + cols - 1) // cols
    W, H = cell_w * cols + 24, cell_h * rows + 56

    if backdrop is not None:
        canvas = Image.new("RGBA", (W, H), flat_rgb)
        bd = backdrop if backdrop.size == (W, H) else None
        if bd is None:
            bd = Image.new("RGBA", (W, H))
            t = backdrop.crop((0, 0, 32, 32))
            for y in range(0, H, 32):
                for x in range(0, W, 32):
                    bd.alpha_composite(t, (x, y))
        canvas.alpha_composite(bd)
    else:
        canvas = Image.new("RGBA", (W, H), flat_rgb)

    r = WallRenderer(sheet, outline)
    for i, sc in enumerate(SCENES):
        cx = 12 + (i % cols) * cell_w
        cy = 44 + (i // cols) * cell_h
        r.render_scene(sc, cx + pad, cy + pad + 24)
    r.paint(canvas)

    d = ImageDraw.Draw(canvas)
    fg = (255, 255, 255, 255) if sum(flat_rgb[:3]) < 320 else (0, 0, 0, 255)
    d.text((12, 14), label, fill=fg)
    for i, sc in enumerate(SCENES):
        cx = 12 + (i % cols) * cell_w
        cy = 44 + (i // cols) * cell_h
        d.text((cx + 4, cy + 4), "%d. %s" % (i + 1, sc.title), fill=fg)
    return canvas.resize((W * 4, H * 4), Image.NEAREST)


MOCK = Scene("house", [
    "..........",
    ".########.",
    ".#......#.",
    ".O......O.",
    ".#......#.",
    ".###DD###.",
    "..........",
])


def build_mock(sheet_path, outline_path, label):
    """The 1x in-context mock: how a player actually sees it.

    A 4x contact sheet flatters everything. The gate that matters is a
    building at game zoom on the two grounds this set is placed on — the
    Veil's Stormslate and the Skyreach's Cloudturf — plus a 3x copy of the
    same pixels for reading the detail without changing it."""
    sheet = Image.open(sheet_path).convert("RGBA")
    outline = None
    if outline_path and os.path.exists(outline_path):
        outline = Image.open(outline_path).convert("RGBA")
    W, H = MOCK.w * 32, MOCK.h * 32
    panels = []
    for splat, flat in (("stormslate", (24, 22, 32, 255)),
                        ("cloudturf", (206, 214, 226, 255))):
        bd = tile_backdrop(splat, (W, H))
        panel = Image.new("RGBA", (W, H), flat)
        if bd is not None:
            panel.alpha_composite(bd)
        r = WallRenderer(sheet, outline)
        r.render_scene(MOCK, 0, 0)
        r.paint(panel)
        panels.append(panel)

    gap = 8
    strip = Image.new("RGBA", (W * 2 + gap, H), (0, 0, 0, 0))
    strip.alpha_composite(panels[0], (0, 0))
    strip.alpha_composite(panels[1], (W + gap, 0))
    big = strip.resize((strip.width * 3, strip.height * 3), Image.NEAREST)
    out = Image.new("RGBA", (big.width, H + big.height + 40), (128, 128, 132, 255))
    out.alpha_composite(strip, ((big.width - strip.width) // 2, 26))
    out.alpha_composite(big, (0, H + 34))
    d = ImageDraw.Draw(out)
    d.text((8, 8), "%s  --  1x in context (Stormslate | Cloudturf), then 3x"
           % label, fill=(16, 16, 20, 255))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sheet", action="append", default=[],
                    help="path to a 352x128 wall sheet (repeatable)")
    ap.add_argument("--out", default=os.path.join(REPO, "build", "qa"))
    ap.add_argument("--vanilla", action="append", default=[],
                    help="vanilla wall name from the sprite dump, e.g. stonewall")
    args = ap.parse_args()

    sheets = []
    if args.sheet:
        for p in args.sheet:
            sheets.append((os.path.splitext(os.path.basename(p))[0], p))
    else:
        for n in MOD_WALL_SHEETS:
            p = os.path.join(RES, "objects", n + ".png")
            if os.path.exists(p):
                sheets.append((n, p))
    for n in args.vanilla:
        p = os.path.join(VANILLA_SPRITES, "objects", n + ".png")
        if os.path.exists(p):
            sheets.append(("vanilla_" + n, p))
        else:
            print("missing vanilla sheet: %s" % p, file=sys.stderr)

    outline = os.path.join(VANILLA_SPRITES, "objects", "walloutlines.png")
    if not os.path.exists(outline):
        outline = None

    os.makedirs(args.out, exist_ok=True)
    dark = tile_backdrop("stormslate", (32, 32))
    light = tile_backdrop("cloudturf", (32, 32))

    written = []
    for name, path in sheets:
        for tag, bd, flat in (("dark", dark, (24, 22, 32, 255)),
                              ("light", light, (226, 230, 238, 255))):
            img = build_sheet(path, outline, bd, flat,
                              "%s  --  %s backdrop  --  4x nearest" % (name,
                                                                       tag))
            fp = os.path.join(args.out, "wall_%s_%s.png" % (name, tag))
            img.convert("RGB").save(fp)
            written.append(fp)
        fp = os.path.join(args.out, "wall_%s_mock.png" % name)
        build_mock(path, outline, name).convert("RGB").save(fp)
        written.append(fp)
    for fp in written:
        print(fp)


if __name__ == "__main__":
    main()
