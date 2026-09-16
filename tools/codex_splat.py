#!/usr/bin/env python3
"""Codex-Vorlage fuer ein Boden-Splat: Zellraster-Vorlage, Referenzen, Brief.

WOZU. Ein Modell, das "ein Splat" gemalt bekommt, malt eine durchgehende
Flaeche -- genau das Format, das `TerrainSplatterTile.getTerrainTexture`
NICHT so liest (siehe splat_seam_audit.py). Dieses Werkzeug bereitet einen
Codex-Lauf so vor, dass der Brief die Zellgeometrie hart vorgibt: die 24
vollen 32x32-Zellen (Spalte 3..6, Zeile 0 jedes 96px-Blocks) sind der
einzige Ort, an dem neu gemalt wird; die 17 Uebergangszellen je Block
(Alpha-Formen fuer Nachbarboeden) bleiben unangetastet.

Aufruf (bereitet nur vor, startet Codex NICHT automatisch -- max. ein
Codex-Lauf gleichzeitig, siehe Auftrag):

    PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/codex_splat.py \\
        cloudturf --theme "sattes Gruen, hellgruene Huegelkuppen, tuerkis Akzente" \\
        --like src/main/resources/tiles/cloudturf_splat.png

Schreibt nach build/codexsplat/<NAME>/:
    grid_template.png   das aktuelle/like-Blatt mit rot markierten Vollzellen
    like_splat.png       Kopie des Referenzblatts (Farben/Alpha-Vorlage)
    junkfloor_ref.png    Vanilla-Referenz fuer "wirkt nahtlos"
    brief.md             fertiger Codex-Auftragstext
    RUN.sh               der codex-exec-Befehl, fertig zum Ausfuehren

Nach dem Codex-Lauf (liefert splat.png mit neu gemalten Vollzellen):

    PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/codex_splat.py \\
        cloudturf --compose build/codexsplat/cloudturf/splat.png

setzt die 24 neuen Vollzellen in eine Kopie des Referenzblatts ein und
behaelt fuer die 17 Uebergangszellen je Block die ALPHA-Maske des
Referenzblatts, aber die Farbe aus der jeweils naechsten neuen Vollzelle
--- die Textur bleibt so weltverankert (kein Stempel-Bruch an Kacheln).
"""
import argparse
import os
import sys

TILE = 32
WIDTH = 224
BLOCK_H = 96
FULL_COLS = (3, 4, 5, 6)

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_VANILLA_SPLAT = None


def out_dir(name):
    d = os.path.join(REPO, "build", "codexsplat", name)
    os.makedirs(d, exist_ok=True)
    return d


def load(path):
    from PIL import Image
    return Image.open(path).convert("RGBA")


def grid_template(like_path, out_path):
    """Referenzblatt mit rotem Rahmen um jede volle Zelle + Beschriftung."""
    from PIL import Image, ImageDraw
    im = load(like_path).copy()
    blocks = im.height // BLOCK_H
    scale = 3
    canvas = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    d = ImageDraw.Draw(canvas)
    for b in range(blocks):
        for col in FULL_COLS:
            x0 = col * TILE * scale
            y0 = b * BLOCK_H * scale
            x1 = x0 + TILE * scale
            y1 = y0 + TILE * scale
            d.rectangle((x0, y0, x1 - 1, y1 - 1), outline=(255, 0, 0, 255), width=2)
    d.text((4, 4), "rot = volle 32x32-Zelle, hier neu malen; sonst nichts anfassen",
           fill=(255, 0, 0, 255))
    canvas.save(out_path)


def full_cell_positions(blocks):
    """Reihenfolge der 24 (bei 6 Bloecken) Vollzellen-Koordinaten."""
    return [(b, col) for b in range(blocks) for col in FULL_COLS]


def write_brief(path, name, theme, like_rel, junkfloor_rel):
    text = """Necesse-Mod: Boden-Splat "%s" neu malen, NAHTLOS gegen zufaelliges Zell-Wuerfeln.

FORMAT (hart): 224 px breit, Vielfaches von 96 px hoch. Jeder 96px-Block ist
ein 7x3-Raster aus 32x32-Zellen. Spalte 3..6, Zeile 0 jedes Blocks sind VOLLE,
komplett opake 32x32-Bodenkacheln -- siehe grid_template.png, rot markiert.
Alle anderen Zellen sind Uebergangs-Alphaformen fuer Nachbarboeden, die FASST
DU NICHT AN.

WARUM: Das Spiel wuerfelt fuer JEDE Bodenkachel einzeln Block + eine der vier
vollen Zellen. Benachbarte Kacheln zeigen fast immer zwei unabhaengig
gewuerfelte Zellen nebeneinander. Motive, die ueber eine Zellkante laufen,
werden beim Spielen an zufaelliger Stelle zerschnitten: sichtbares Raster.

AUFTRAG: Male die vollen 32x32-Zellen so, dass jedes Motiv VOLLSTAENDIG
INNERHALB einer Zelle liegt, mind. 2px ruhiger Grund als Rand an allen vier
Kanten, Randfarbe zwischen allen Vollzellen nahezu identisch. Referenz fuer
"wirkt nahtlos": junkfloor_ref.png (Vanilla).

FARBWELT: %s (siehe like_splat.png fuer die aktuelle Farbgebung).
Necesse-Pixelart-Stil: klare Kanten, wenige Farbabstufungen, kein
Softgradient, kein Rauschen.

WAS DU NICHT ANFASST: alle Zellen ausserhalb der rot markierten Vollzellen
(grid_template.png) -- deren Alpha-Form wird spaeter automatisch aus
like_splat.png uebernommen; du musst dort nichts Passendes liefern.

TECHNIK: Endkunst NICHT mit PIL-Formen, sondern ueber den eingebauten
image_gen-Master erzeugen, dann per Modalfarben-Downscale auf das exakte
Pixelraster bringen, auf hoechstens ca. 40 Farben reduzieren, Alpha hart (0
oder 255). PIL falls fuer Downscale/Palette noetig:
PYTHONPATH=/home/blackoffset/dev/pylib python3 -- kein venv, nichts laden.

AUSGABE: NUR nach build/ -- konkret build/codexsplat/%s/splat.png, gleiche
Groesse wie like_splat.png, RGBA.

Referenzbilder liegen neben diesem Brief: grid_template.png, like_splat.png,
junkfloor_ref.png.
""" % (name, theme, name)
    with open(path, "w") as f:
        f.write(text)


def prepare(name, theme, like):
    import shutil
    d = out_dir(name)
    like_dst = os.path.join(d, "like_splat.png")
    shutil.copyfile(like, like_dst)
    grid_template(like, os.path.join(d, "grid_template.png"))
    sys.path.insert(0, os.path.join(REPO, "tools"))
    from size_audit import default_vanilla
    jf = os.path.join(default_vanilla(), "tiles", "junkfloor_splat.png")
    jf_dst = os.path.join(d, "junkfloor_ref.png")
    if os.path.exists(jf):
        shutil.copyfile(jf, jf_dst)
    write_brief(os.path.join(d, "brief.md"), name, theme,
                "like_splat.png", "junkfloor_ref.png")
    run_sh = os.path.join(d, "RUN.sh")
    with open(run_sh, "w") as f:
        f.write("#!/bin/sh\n# hoechstens EIN Codex-Lauf gleichzeitig (siehe Auftrag)\n")
        f.write('BRIEF="$(cat "%s")"\n' % os.path.join(d, "brief.md"))
        f.write(
            "nohup /home/blackoffset/.npm-global/bin/codex exec "
            "-C %s -s workspace-write --skip-git-repo-check "
            "-i %s -i %s -o %s \"$BRIEF\" > %s 2>&1 &\n"
            % (REPO, like_dst, jf_dst, os.path.join(d, "last.txt"), os.path.join(d, "codex_run.log"))
        )
        f.write("echo PID=$!\n")
    os.chmod(run_sh, 0o755)
    print("vorbereitet: %s" % d)
    print("  Start: sh %s" % run_sh)
    return d


def compose(name, new_path):
    """Setzt die neuen Vollzellen in eine Kopie des like-Blatts; Uebergangs-
    zellen behalten die Alpha-Maske des like-Blatts, Farbe aus der naechsten
    neuen Vollzelle im selben Block (weltverankert, kein Stempel-Bruch)."""
    d = out_dir(name)
    like_path = os.path.join(d, "like_splat.png")
    like = load(like_path)
    new = load(new_path)
    if new.size != like.size:
        print("WARNUNG: Groesse %dx%d != Vorlage %dx%d -- keine automatische "
              "Skalierung (LANCZOS wuerde Pixelart zerstoeren). Abbruch."
              % (new.width, new.height, like.width, like.height))
        return None
    out = like.copy()
    px_out = out.load()
    px_new = new.load()
    px_like = like.load()
    blocks = like.height // BLOCK_H
    for b in range(blocks):
        # 1. Vollzellen direkt uebernehmen, opak erzwingen.
        for col in FULL_COLS:
            x0, y0 = col * TILE, b * BLOCK_H
            for y in range(y0, y0 + TILE):
                for x in range(x0, x0 + TILE):
                    r, g, bl, a = px_new[x, y]
                    px_out[x, y] = (r, g, bl, 255)
        # 2. Uebergangszellen: Alpha vom like-Blatt, Farbe von der Vollzelle
        #    Spalte 3 desselben Blocks (eine feste, immer vorhandene Quelle).
        src_col = 3
        for row in range(3):
            for col in range(7):
                if row == 0 and col in FULL_COLS:
                    continue
                x0, y0 = col * TILE, b * BLOCK_H + row * TILE
                for yy in range(TILE):
                    for xx in range(TILE):
                        x, y = x0 + xx, y0 + yy
                        a = px_like[x, y][3]
                        if a == 0:
                            px_out[x, y] = (0, 0, 0, 0)
                        else:
                            sx = src_col * TILE + xx
                            sy = b * BLOCK_H + yy
                            r, g, bl, _ = px_out[sx, sy]
                            px_out[x, y] = (r, g, bl, a)
    out_path = os.path.join(d, "splat_composed.png")
    out.save(out_path)
    print("komponiert: %s" % out_path)
    return out_path


def main():
    ap = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("name")
    ap.add_argument("--theme", default="wie Vorlage")
    ap.add_argument("--like", help="Referenzblatt (Format + Alpha-Uebergaenge)")
    ap.add_argument("--compose", metavar="NEW_SPLAT_PNG",
                     help="Codex-Ergebnis mit den 17 Uebergangszellen aus --like zusammensetzen")
    args = ap.parse_args()

    if args.compose:
        compose(args.name, args.compose)
        return
    if not args.like:
        ap.error("--like ist erforderlich, wenn nicht --compose gesetzt ist")
    prepare(args.name, args.theme, args.like)


if __name__ == "__main__":
    main()
