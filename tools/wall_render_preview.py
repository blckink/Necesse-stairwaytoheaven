#!/usr/bin/env python3
"""Wall contact sheet: assemble a 352x128 wall sheet the way the ENGINE does,
and put the vanilla sheets straight underneath it in the same scene.

`sheet_format_audit.py` proves a wall sheet's cells are in the right PLACE and
the right SIZE. It cannot prove the art inside them joins up, and that is the
failure mode a player actually sees: "kein Rand stimmt" -- every cell drawn as
a nice little picture, none of them agreeing with its neighbour.

The only way to see that before shipping is to run the engine's own cell
selection over a synthetic level and look at the result. This is a line-by-line
port of `necesse.level.gameObject.WallObject.addWallDrawOptions` (both
overloads), `WallWindowObject.addWallDrawOptions` and the two
`WallDoorObject` draw paths, from the 1.3.2 decompile.

THE COMPARISON IS THE POINT. A scene showing only our own sheet answers "does
it tile", and nothing else. It cannot answer "is this door as tall as a door"
or "is this the view a side wall is supposed to show", because both of those
are judgements against vanilla, and the eye cannot hold vanilla in memory
across a session. Two faults shipped past this tool for exactly that reason:
a door whose leaf was a third of vanilla's, and a side-wall window drawing a
front-facing pane where vanilla draws the wall's top surface with a hole in it.
Both are obvious the moment stonewall is drawn in the same scene one strip
down. So every scene here renders OURS, then each reference wall, same layout,
same scale, same backdrop.

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

  Windows have two completely different views out of one 32px strip.
  `WallWindowObject.getWindowDir` returns 1 when the wall runs NORTH-SOUTH --
  the LEFT and RIGHT walls of a room -- and then only cells (4,0) (5,0) (4,1)
  (5,1) are drawn, over the band drawY-16..drawY+16. That band is the wall's
  TOP SURFACE, so the art there is a hole looked down into, not a pane looked
  at. It returns 0 for an EAST-WEST wall -- the top and bottom walls -- and
  draws rows 2..7 as the wall's front, where a front-facing pane is correct.
  Scene "windows in the LEFT and RIGHT walls" exercises the first branch and
  "windows in the TOP and BOTTOM walls" the second.

Usage:
    python3 tools/wall_render_preview.py                 # every mod wall sheet
    python3 tools/wall_render_preview.py --sheet a.png --sheet b.png
    python3 tools/wall_render_preview.py --out build/qa
    python3 tools/wall_render_preview.py --refs stonewall,brickwall

Writes, per sheet, <out>/wall_<name>_dark.png and _light.png (4x nearest
comparison contact sheets) and <out>/wall_<name>_mock.png (1x in context on
Stormslate and Cloudturf, then 3x, with the reference walls beneath).
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

# The two vanilla walls every scene is judged against. stonewall is the plain
# masonry the door and window extents were measured off; woodwall is the
# ornate one, so "our set is decorative" is never an excuse for a shape that
# vanilla's own decorative wall does not have.
DEFAULT_REFS = ("stonewall", "woodwall")

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

    def has_door(self):
        return any(self.is_door(x, y)
                   for y in range(self.h) for x in range(self.w))


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
#
# Between them these have to reach every branch of the port above, because a
# branch no scene reaches is art nobody looked at:
#   * shapes      -- solid block, L corner, free-standing runs (both axes)
#   * windows LR  -- getWindowDir 1, cells (4,0) (5,0) (4,1) (5,1)
#   * windows TB  -- getWindowDir 0, cells (4,2)..(5,7)
#   * doors shut  -- cells 3, 5, 7, 9 (all four rotations)
#   * doors open  -- cells 4, 6, 8, 10
# ---------------------------------------------------------------------------
SCENES = [
    Scene("shapes: solid block, L corner, free-standing runs", [
        "####.####.#.",
        "####.#....#.",
        "####.#....#.",
        "............",
        ".####.......",
        "............",
    ]),
    Scene("windows in the LEFT and RIGHT walls (getWindowDir 1: "
          "the wall's TOP SURFACE, looked down into)", [
              "######",
              "O....#",
              "#....O",
              "O....#",
              "######",
          ]),
    Scene("windows in the TOP and BOTTOM walls (getWindowDir 0: "
          "the wall's FRONT, a front-facing pane)", [
              "#O#O##",
              "#....#",
              "#....#",
              "#....#",
              "##O#O#",
          ]),
    Scene("closed doors: rot 0 and rot 2 in the top wall, "
          "rot 1 and rot 3 in the side walls", [
              "#D#D##",
              "#....#",
              "D....D",
              "#....#",
              "######",
          ], doorbase={(3, 0): 2, (5, 2): 2}),
    Scene("open doors: the same four rotations", [
        "#d#d##",
        "#....#",
        "d....d",
        "#....#",
        "######",
    ], doorbase={(3, 0): 2, (5, 2): 2}),
]

SIDE_PAD = 16
BOT_PAD = 28
# A door cell is 128px drawn at drawY-96, so a scene with a door needs 96px of
# air above its top tile row or the crown is cropped and "is the crown right"
# becomes unanswerable.
DOOR_TOP_PAD = 104
PLAIN_TOP_PAD = 40


def tile_backdrop(splat_name, size):
    """A 32x32 plain variant from a _splat atlas, tiled to `size`."""
    p = os.path.join(RES, "tiles", splat_name + "_splat.png")
    if not os.path.exists(p):
        return None
    atlas = Image.open(p).convert("RGBA")
    tile = atlas.crop((3 * 32, 0, 4 * 32, 32))
    out = Image.new("RGBA", (size[0], size[1]))
    for y in range(0, size[1], 32):
        for x in range(0, size[0], 32):
            out.alpha_composite(tile, (x, y))
    return out


def load_sheet(path):
    sheet = Image.open(path).convert("RGBA")
    if sheet.size != (352, 128):
        raise SystemExit("%s is %s, expected (352, 128)" % (path, sheet.size))
    return sheet


def render_strip(scene, sheet, outline, tile, flat):
    """One scene drawn from one sheet, at 1x."""
    top = DOOR_TOP_PAD if scene.has_door() else PLAIN_TOP_PAD
    w = scene.w * 32 + SIDE_PAD * 2
    h = scene.h * 32 + top + BOT_PAD
    panel = Image.new("RGBA", (w, h), flat)
    if tile is not None:
        for y in range(0, h, 32):
            for x in range(0, w, 32):
                panel.alpha_composite(tile, (x, y))
    r = WallRenderer(sheet, outline)
    r.render_scene(scene, SIDE_PAD, top)
    r.paint(panel)
    return panel


def _wrap(text, width):
    words, lines, cur = text.split(), [], ""
    for wd in words:
        if cur and len(cur) + 1 + len(wd) > width:
            lines.append(cur)
            cur = wd
        else:
            cur = (cur + " " + wd).strip()
    if cur:
        lines.append(cur)
    return lines


def build_compare_sheet(sheets, outline, tile, flat, label, zoom=4):
    """One contact sheet: every scene, our sheet on top, references beneath.

    `sheets` is [(name, Image), ...] with ours first.
    """
    fg = (255, 255, 255, 255) if sum(flat[:3]) < 320 else (0, 0, 0, 255)
    accent = (255, 214, 110, 255) if sum(flat[:3]) < 320 else (150, 70, 0, 255)
    frame = (255, 255, 255, 40) if sum(flat[:3]) < 320 else (0, 0, 0, 40)
    LBL = 11
    blocks = []
    for sc in SCENES:
        strips = [(nm, render_strip(sc, sh, outline, tile, flat))
                  for nm, sh in sheets]
        bw = max(s.width for _, s in strips)
        title_lines = _wrap(sc.title, max(24, bw // 6))
        bh = sum(s.height + LBL for _, s in strips) + 10 * len(title_lines) + 8
        blk = Image.new("RGBA", (bw, bh), (0, 0, 0, 0))
        d = ImageDraw.Draw(blk)
        y = 0
        for ln in title_lines:
            d.text((2, y), ln, fill=accent)
            y += 10
        y += 4
        for i, (nm, s) in enumerate(strips):
            d.text((2, y), ("%s   (OURS)" % nm) if i == 0 else
                   ("%s   (vanilla reference)" % nm), fill=fg)
            y += LBL
            blk.alpha_composite(s, (0, y))
            d.rectangle([0, y, bw - 1, y + s.height - 1], outline=frame)
            y += s.height
        blocks.append(blk)

    # Wide scenes get their own full-width row; the rest are packed into two
    # columns, shortest-first, so the sheet stays roughly square.
    wide = [b for b in blocks if b.width > 260]
    narrow = [b for b in blocks if b.width <= 260]
    colw = max([b.width for b in narrow] or [0])
    cols = [[], []]
    heights = [0, 0]
    for b in narrow:
        i = 0 if heights[0] <= heights[1] else 1
        cols[i].append(b)
        heights[i] += b.height + 12
    W = max(max([b.width for b in wide] or [0]), colw * 2 + 12) + 16
    H = sum(b.height + 12 for b in wide) + max(heights) + 30

    canvas = Image.new("RGBA", (W, H), flat)
    if tile is not None:
        for y in range(0, H, 32):
            for x in range(0, W, 32):
                canvas.alpha_composite(tile, (x, y))
    d = ImageDraw.Draw(canvas)
    d.text((8, 6), label, fill=fg)
    y = 22
    for b in wide:
        canvas.alpha_composite(b, (8, y))
        y += b.height + 12
    for i, col in enumerate(cols):
        cy = y
        for b in col:
            canvas.alpha_composite(b, (8 + i * (colw + 12), cy))
            cy += b.height + 12
    return canvas.resize((W * zoom, H * zoom), Image.NEAREST)


MOCK = Scene("house", [
    "..........",
    ".########.",
    ".#......#.",
    ".O......O.",
    ".#......#.",
    ".###DD###.",
    "..........",
])


def build_mock(sheets, outline, label):
    """The 1x in-context mock: how a player actually sees it.

    A 4x contact sheet flatters everything. The gate that matters is a
    building at game zoom on the two grounds this set is placed on -- the
    Veil's Stormslate and the Skyreach's Cloudturf -- plus a 3x copy of the
    same pixels for reading the detail without changing it. Each reference
    wall gets the same house right underneath, because "does this read as a
    building" is only answerable next to a building that does."""
    W, H = MOCK.w * 32, MOCK.h * 32
    gap = 8
    rows = []
    for nm, sheet in sheets:
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
        strip = Image.new("RGBA", (W * 2 + gap, H), (0, 0, 0, 0))
        strip.alpha_composite(panels[0], (0, 0))
        strip.alpha_composite(panels[1], (W + gap, 0))
        rows.append((nm, strip))

    big_w = rows[0][1].width * 3
    out_h = 30 + sum(r.height + 14 for _, r in rows) \
        + 16 + sum(r.height * 3 + 14 for _, r in rows)
    out = Image.new("RGBA", (max(big_w, rows[0][1].width) + 8, out_h),
                    (128, 128, 132, 255))
    d = ImageDraw.Draw(out)
    d.text((8, 6), "%s  --  1x in context (Stormslate | Cloudturf)"
           % label, fill=(16, 16, 20, 255))
    y = 22
    for nm, strip in rows:
        d.text((8, y), nm, fill=(16, 16, 20, 255))
        y += 12
        out.alpha_composite(strip, (0, y))
        y += strip.height + 2
    y += 8
    d.text((8, y), "the same pixels at 3x", fill=(16, 16, 20, 255))
    y += 14
    for nm, strip in rows:
        d.text((8, y), nm, fill=(16, 16, 20, 255))
        y += 12
        big = strip.resize((strip.width * 3, strip.height * 3), Image.NEAREST)
        out.alpha_composite(big, (0, y))
        y += big.height + 2
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sheet", action="append", default=[],
                    help="path to a 352x128 wall sheet (repeatable)")
    ap.add_argument("--out", default=os.path.join(REPO, "build", "qa"))
    ap.add_argument("--vanilla", action="append", default=[],
                    help="vanilla wall name from the sprite dump, rendered as "
                         "a subject of its own as well as a reference")
    ap.add_argument("--refs", default=",".join(DEFAULT_REFS),
                    help="comma-separated vanilla wall names drawn beneath "
                         "every scene (default: %s)" % ",".join(DEFAULT_REFS))
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

    refs = []
    for n in [r.strip() for r in args.refs.split(",") if r.strip()]:
        p = os.path.join(VANILLA_SPRITES, "objects", n + ".png")
        if os.path.exists(p):
            refs.append((n, load_sheet(p)))
        else:
            print("reference wall not on this machine, skipping: %s" % p,
                  file=sys.stderr)
    if not refs:
        print("WARNING: no vanilla reference walls found under %s -- the "
              "comparison strips are the point of this tool, so treat a run "
              "without them as inconclusive." % VANILLA_SPRITES,
              file=sys.stderr)

    outline_path = os.path.join(VANILLA_SPRITES, "objects", "walloutlines.png")
    outline = Image.open(outline_path).convert("RGBA") \
        if os.path.exists(outline_path) else None

    os.makedirs(args.out, exist_ok=True)
    dark = tile_backdrop("stormslate", (32, 32))
    light = tile_backdrop("cloudturf", (32, 32))

    written = []
    for name, path in sheets:
        stack = [(name, load_sheet(path))] + refs
        for tag, bd, flat in (("dark", dark, (24, 22, 32, 255)),
                              ("light", light, (226, 230, 238, 255))):
            img = build_compare_sheet(
                stack, outline, bd, flat,
                "%s vs %s  --  %s backdrop  --  4x nearest"
                % (name, "/".join(n for n, _ in refs) or "(no reference)", tag))
            fp = os.path.join(args.out, "wall_%s_%s.png" % (name, tag))
            img.convert("RGB").save(fp)
            written.append(fp)
        fp = os.path.join(args.out, "wall_%s_mock.png" % name)
        build_mock(stack, outline, name).convert("RGB").save(fp)
        written.append(fp)
    for fp in written:
        print(fp)


if __name__ == "__main__":
    main()
