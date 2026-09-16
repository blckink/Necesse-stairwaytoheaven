#!/usr/bin/env python3
"""Szenen-Vorschau: Boden, Wand, Objekte und Figuren zusammen auf einem Bild.

WOZU. Jedes andere Werkzeug zeigt ein Blatt allein (sprite_gallery,
area_sheets, asset_intake). Die Frage des Spielers ist eine andere: *wie
wirkt das zusammen?* Dieses Werkzeug legt die ausgelieferten Blätter so
übereinander, wie das Spiel sie zeichnet, und schreibt je Szene ein PNG in
Spielgröße mal zwei.

WAS ECHT IST UND WAS NICHT:
  * Wände laufen durch `wall_render_preview.WallRenderer`, den Zeile-für-Zeile-
    Nachbau von `WallObject.addWallDrawOptions` samt Türen und Fenstern.
  * Boden: die glatte Kachel jeder Variante aus dem `_splat`-Atlas. Übergänge
    zwischen zwei Böden zeichnet das Werkzeug NICHT; die Kante ist hart.
  * Objekte: die Unterkanten-Regel, die jede Objektklasse benutzt
    (`drawX - w/2 + 16`, `drawY - h + 32`), erste Drehung bzw. Variante.
  * Figuren: Vanilla-Haut aus dem Sprite-Dump (Grundton, nicht eingefärbt),
    darüber Stiefel, Brust, Kopf und Arme des Outfits, Frame 0 der
    Blickrichtung nach unten.
  * Wandbilder: die Vorderansicht des großen Gemäldes, auf die Wandfläche
    gelegt -- Lage ungefähr, nicht aus dem Code nachgerechnet.
  * Kein Licht, keine Schatten, keine Animation.

Aufruf:
    PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/scene_preview.py [ZIELORDNER]
"""
import hashlib
import os
import sys

from PIL import Image, ImageDraw

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")
sys.path.insert(0, os.path.join(REPO, "tools"))

from size_audit import default_vanilla  # noqa: E402

os.environ.setdefault("NECESSE_SPRITES", default_vanilla())
import wall_render_preview as wrp  # noqa: E402

VANILLA = os.environ["NECESSE_SPRITES"]
SCALE = 2


def img(rel):
    return Image.open(os.path.join(RES, rel)).convert("RGBA")


def cell(rel, x, y, w, h):
    return img(rel).crop((x, y, x + w, y + h))


def hash01(x, y, salt):
    h = hashlib.md5(("%d,%d,%s" % (x, y, salt)).encode()).digest()
    return h[0] / 255.0


def ground_tile(splat, variant):
    """Die glatte 32x32-Kachel einer Variante: Spalte 3, Zeile 0 jedes
    96 px hohen Variantenblocks."""
    atlas = img("tiles/%s_splat.png" % splat)
    variants = atlas.height // 96
    v = variant % variants
    return atlas.crop((96, v * 96, 128, v * 96 + 32))


class Stage:
    """Ein Raster aus Kacheln. Böden per Zeichen, Wände per Scene, Objekte
    mit Sortierung nach ihrer Unterkante wie im Spiel."""

    def __init__(self, title, ground_rows, legend, walls, wall_sheet):
        self.title = title
        self.ground_rows = ground_rows
        self.legend = legend
        self.h = len(ground_rows)
        self.w = max(len(r) for r in ground_rows)
        self.scene = wrp.Scene(title, walls)
        self.renderer = wrp.WallRenderer(img(wall_sheet)) if wall_sheet else None
        self.extra = []
        self.labels = []

    def put(self, image, tx, ty, label=None, sort_bias=16, dx=0, dy=0):
        """Unterkanten-Regel: mittig auf der Kachel, Fuß auf ihrer Unterkante."""
        x = tx * 32 + 16 - image.width // 2 + dx
        y = ty * 32 + 32 - image.height + dy
        self.extra.append((ty * 32 + sort_bias, image, x, y))
        if label:
            self.labels.append((label, tx * 32 + 16, ty * 32 + 34))

    def render(self, top_pad=72, bottom_pad=24):
        W, H = self.w * 32, self.h * 32 + top_pad + bottom_pad
        canvas = Image.new("RGBA", (W, H), (20, 20, 26, 255))
        for ty, row in enumerate(self.ground_rows):
            for tx, ch in enumerate(row):
                splat = self.legend.get(ch)
                if splat is None:
                    continue
                v = int(hash01(tx, ty, splat) * 6)
                canvas.alpha_composite(ground_tile(splat, v), (tx * 32, ty * 32 + top_pad))
        r = self.renderer or wrp.WallRenderer(None)
        if self.renderer:
            r.render_scene(self.scene, 0, top_pad)
        for sort, image, x, y in self.extra:
            r.add(image, x, y + top_pad, sort)
        r.paint(canvas)
        out = canvas.resize((W * SCALE, H * SCALE), Image.NEAREST)
        d = ImageDraw.Draw(out)
        for text, cx, cy in self.labels:
            px, py = cx * SCALE - len(text) * 3, (cy + top_pad) * SCALE
            px = max(4, min(px, W * SCALE - len(text) * 6 - 4))
            d.rectangle((px - 2, py - 1, px + len(text) * 6 + 2, py + 11), fill=(0, 0, 0, 170))
            d.text((px, py), text, fill=(255, 230, 150, 255))
        d.rectangle((0, 0, W * SCALE, 22), fill=(0, 0, 0, 200))
        d.text((8, 6), self.title, fill=(255, 255, 255, 255))
        return out


def figure(outfit=None, row=0):
    """Ein Mensch, Frame 0, Blickrichtung `row` (0 = nach unten)."""
    f = Image.new("RGBA", (64, 64))

    def layer(path):
        if not os.path.exists(path):
            return
        s = Image.open(path).convert("RGBA")
        if s.height >= (row + 1) * 64:
            f.alpha_composite(s.crop((0, row * 64, 64, row * 64 + 64)))

    skin = os.path.join(VANILLA, "player", "skin")
    armor = os.path.join(RES, "player", "armor")
    layer(os.path.join(skin, "body.png"))
    layer(os.path.join(skin, "feet.png"))
    if outfit:
        layer(os.path.join(armor, outfit + "boots.png"))
        layer(os.path.join(armor, outfit + "chest.png"))
    layer(os.path.join(skin, "head.png"))
    if outfit:
        layer(os.path.join(armor, outfit + "head.png"))
    for side in ("left", "right"):
        layer(os.path.join(skin, "arms_%s.png" % side))
        if outfit:
            layer(os.path.join(armor, "%sarms_%s.png" % (outfit, side)))
    return f


def scatter(stage, sprite_rel, variants, cells, salt, density, label=None):
    """Streut ein 32x32-Pflanzenblatt (Varianten nebeneinander) über Kacheln."""
    sheet = img(sprite_rel)
    first = True
    for tx, ty in cells:
        if hash01(tx, ty, salt) < density:
            v = int(hash01(ty, tx, salt) * variants)
            stage.put(sheet.crop((v * 32, 0, v * 32 + 32, 32)), tx, ty,
                      label if first else None, sort_bias=8)
            first = False


def skyreach():
    ground = [
        "cccccccccccccccccccccccccccc",
        "cccccccccccccccccccccccccccc",
        "cccccccccccccccccccccccccccc",
        "cccccccccccccccccccccccccccc",
        "cccccccccccccccccccccccccccc",
        "ccccccccccccccccccccssssssss",
        "cccccccccccccccccccsssssssss",
        "ccccccccccccccccccssssssssss",
        "cccccccccccccccccsssssssssss",
        "ccccccccccccccccssssssssssss",
        "cccccccccccccccsssssssssssss",
        "cccccccccccccccsssssssssssss",
    ]
    walls = [
        "............................",
        ".#######....................",
        ".#.....#....................",
        ".O.....O....................",
        ".#.....#....................",
        ".##DD###....................",
    ]
    st = Stage("Himmelsreich: Cloudturf + Stormslate, Cloudmarble-Haus, Pflanzen, Baeume, Golem, Megahai",
               ground, {"c": "cloudturf", "s": "stormslate"}, walls, "objects/cloudmarblewall.png")
    meadow = [(x, y) for y in range(6, 12) for x in range(0, 14)]
    scatter(st, "objects/tallcloudgrass.png", 4, meadow[:30], "tcg", 0.55, "tallcloudgrass")
    scatter(st, "objects/prismgrass.png", 4, meadow[30:60], "pg", 0.55, "prismgrass")
    scatter(st, "objects/windwheat.png", 4, [(x, y) for y in range(1, 5) for x in range(10, 16)],
            "ww", 0.7, "windwheat")
    scatter(st, "objects/skyreeds.png", 4, [(x, y) for y in range(9, 12) for x in range(16, 21)],
            "sr", 0.7, "skyreeds")
    st.put(cell("objects/prismabirch.png", 0, 0, 128, 128), 18, 3, "prismabirch")
    st.put(cell("objects/prismabirch.png", 0, 128, 128, 128), 21, 1)
    st.put(cell("objects/skyseraphtree.png", 0, 0, 128, 128), 25, 3, "skyseraphtree")
    st.put(cell("objects/skyseraphtree.png", 0, 128, 128, 128), 16, 7)
    st.put(cell("mobs/skystonegolem.png", 0, 0, 128, 128), 11, 9, "skystonegolem", dy=16)
    st.put(cell("mobs/stripedmegashark.png", 0, 608, 240, 304), 24, 11, "stripedmegashark", dy=40)
    st.put(figure(), 4, 3, "Spieler")
    return st.render()


def twilight():
    ground = [
        "mmmmmmmmmmmmmmmmmmmmmmmmm",
        "mmmmmmmmmmmmmmmmmmmmmmmmm",
        "mggggggggggggmmmmmmmmmmmm",
        "mggggggggggggmmmmmmmmmmmm",
        "mggggggggggggmmmmmmmmmmmm",
        "mggggggggggggmmmmmmmmmmmm",
        "mggggggggggggmmmmmmmmmmmm",
        "mggggggggggggmmmmmmmmmmmm",
        "mmmmmmmmmmmmmmmmmmmmmmmmm",
        "mmmmmmmmmmmmmmmmmmmmmmmmm",
        "mmmmmmmmmmmmmmmmmmmmmmmmm",
    ]
    walls = [
        ".........................",
        "##############...........",
        "#............#...........",
        "#............#...........",
        "#............#...........",
        "#............#...........",
        "#............#...........",
        "#............#...........",
        "######DD######...........",
    ]
    st = Stage("Twilight Merchant: Beetlefreak-Wand, Duesterholz-Boden, Moebel, Wandstuecke, Haendler + Outfits",
               ground, {"m": "murkmoss", "g": "gloomwoodfloor"}, walls, "objects/beetlewall.png")
    # Wandstücke auf die Fläche der oberen Wand
    for piece, tx in (("walleye", 3), ("magicmirror", 9)):
        p = os.path.join(RES, "objects", "paintings", piece + ".png")
        if os.path.exists(p):
            front = Image.open(p).convert("RGBA").crop((0, 0, 64, 64))
            st.extra.append((1 * 32 + 21, front, tx * 32, 1 * 32 - 44))
            st.labels.append((piece, tx * 32 + 32, 1 * 32 + 6))
    st.put(cell("objects/coffinbed.png", 0, 0, 64, 64), 1, 3, "coffinbed", dx=8)
    st.put(cell("objects/hauntedclock.png", 0, 0, 32, 64), 6, 2, "hauntedclock")
    st.put(cell("objects/skullcandelabra.png", 0, 0, 32, 64), 12, 2, "skullcandelabra")
    st.put(cell("objects/electricchair.png", 0, 0, 32, 64), 11, 5, "electricchair")
    st.put(cell("objects/twilightsarcophagus.png", 0, 0, 32, 96), 12, 7, "twilightsarcophagus")
    st.put(img("objects/thingbox.png"), 8, 4, "thingbox")
    st.put(figure("twilightsuit"), 6, 5, "Haendler (twilightsuit)")
    outfits = ["skeleton", "grinclown", "hockeyslasher", "dreamstalker", "screamrobe",
               "pincushion", "widowgown", "stitchedmonster", "pumpkinscarecrow", "hauntedpuppet"]
    for i, o in enumerate(outfits):
        tx, ty = 15 + (i % 5) * 2, 2 + (i // 5) * 3
        st.put(figure(o), tx, ty, o)
    st.put(cell("objects/hangingtree.png", 0, 0, 128, 128), 21, 10, "hangingtree")
    st.put(img("objects/witchcauldron.png").crop((0, 0, 64, 64)), 3, 10, "witchcauldron")
    st.put(cell("objects/sandwormtombstone.png", 0, 0, 32, 64), 9, 10, "sandwormtombstone")
    return st.render()


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(REPO, "build", "qa", "scenes")
    os.makedirs(out, exist_ok=True)
    for name, fn in (("szene_himmelsreich.png", skyreach), ("szene_twilight.png", twilight)):
        path = os.path.join(out, name)
        fn().convert("RGB").save(path)
        print(path)


if __name__ == "__main__":
    main()
