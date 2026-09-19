#!/usr/bin/env python3
"""Ein grosses Wandstueck (64x256) auf Vanillas Gemaelde-Raster ausrichten.

WOZU. `LargePaintingObject` schneidet aus dem 64x256-Blatt vier feste Felder
und zeichnet sie mit festen Versaetzen. Wer die Kunst frei ins Band malt,
haengt sie im Spiel neben die Wand: `pos(drawX, drawY-64)` bedeutet, dass
Inhalt oberhalb von Band-y 16 ueber die Wandkrone hinausragt und auf dem
Boden dahinter landet -- "halb auf Wand, halb auf Decke".

DIE MASSE, gemessen an paintinglargeabstract/castle/ship/flatgrass:

    Band 0 (y 0..64)    Vorderseite, Wand im Norden   Inhalt y 30..60
    Band 1 (y 64..128)  Seitenblick Ost               buendig rechts, Mitte y32
    Band 2 (y 128..192) Rueckseite, Wand im Sueden    Inhalt y 24..56
    Band 3 (y 192..256) Seitenblick West              buendig links, Mitte y32

Die Wandflaeche selbst ist 48 px hoch (`WallObject` zeichnet drei 16er-Reihen
ab `drawY-16`), Vanilla nutzt davon nur die unteren 30. Fuer hohe Stuecke --
eine Standuhr ist keine Postkarte -- darf `--hoehe` bis 46 gehen; darueber
steht das Stueck wieder auf dem Boden hinter der Wand. Skaliert wird immer
gleichmaessig, das Blatt wird also nicht verzerrt.

Aufruf:
    PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/align_wall_piece.py \
        hauntedwallclock --hoehe 42
"""
import argparse
import os

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PAINTINGS = os.path.join(REPO, "src", "main", "resources", "objects", "paintings")

VANILLA_HEIGHT = 30     # hoechstes Vanilla-Gemaelde (castle), y30..60
WALL_FACE = 46          # was die Wandflaeche noch traegt
FRONT_BOTTOM = 60       # Unterkante Band 0 bei Vanilla
BACK_BOTTOM = 56        # Unterkante Band 2 bei Vanilla
CENTRE_X = 31           # waagerechte Mitte bei Vanilla (nicht 32)


def _clean(art):
    """LANCZOS laesst beim Verkleinern einen Saum halbdurchsichtiger Pixel."""
    px = art.load()
    for y in range(art.height):
        for x in range(art.width):
            p = px[x, y]
            if p[3] < 24:
                px[x, y] = (0, 0, 0, 0)
            elif p[3] > 232:
                px[x, y] = (p[0], p[1], p[2], 255)
    return art


def align(path, height, dry_run=False):
    im = Image.open(path).convert("RGBA")
    if im.size != (64, 256):
        raise SystemExit("%s ist %dx%d, kein grosses Wandblatt" % ((path,) + im.size))

    bands = []
    for r in range(4):
        band = im.crop((0, r * 64, 64, r * 64 + 64))
        bb = band.getbbox()
        if bb is None:
            raise SystemExit("Band %d von %s ist leer" % (r, path))
        bands.append((band, bb))

    # Ein Faktor fuer das ganze Blatt, gesetzt von der Vorderansicht: sonst
    # passen Vorder-, Rueck- und Seitenansicht nicht mehr zueinander.
    scale = height / (bands[0][1][3] - bands[0][1][1])

    anchors = [
        lambda w, h: (CENTRE_X - w // 2, FRONT_BOTTOM - h),
        lambda w, h: (64 - w, 32 - h // 2),
        lambda w, h: (CENTRE_X - w // 2, BACK_BOTTOM - h),
        lambda w, h: (0, 32 - h // 2),
    ]

    new = Image.new("RGBA", (64, 256), (0, 0, 0, 0))
    report = []
    for r, (band, bb) in enumerate(bands):
        art = band.crop(bb)
        w = max(1, round(art.width * scale))
        h = max(1, round(art.height * scale))
        art = _clean(art.resize((w, h), Image.LANCZOS))
        cell = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        cell.paste(art, anchors[r](w, h), art)
        new.paste(cell, (0, r * 64))
        report.append((r, bb, cell.getbbox()))

    if not dry_run:
        new.save(path)
    return scale, report


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("ids", nargs="+", help="Objekt-IDs unter objects/paintings/")
    ap.add_argument("--hoehe", type=int, default=VANILLA_HEIGHT,
                    help="Hoehe der Vorderansicht in px (Vanilla 30, hoechstens %d)" % WALL_FACE)
    ap.add_argument("--probe", action="store_true", help="nur rechnen, nichts schreiben")
    args = ap.parse_args()
    if args.hoehe > WALL_FACE:
        raise SystemExit("--hoehe %d ragt ueber die Wandkrone (hoechstens %d)"
                         % (args.hoehe, WALL_FACE))

    for oid in args.ids:
        path = os.path.join(PAINTINGS, oid + ".png")
        scale, report = align(path, args.hoehe, args.probe)
        print("%s  Faktor %.3f%s" % (oid, scale, "  (Probe)" if args.probe else ""))
        for r, was, ist in report:
            print("   Band %d  %s -> %s" % (r, was, ist))


if __name__ == "__main__":
    main()
