#!/usr/bin/env python3
"""Kantenkontrast eines Boden-Splats gegen seinen eigenen Innenkontrast.

WOZU. `TerrainSplatterTile.getTerrainTexture` (1.3.2 und 1.3.3, gemessen)
wuerfelt JEDE volle Bodenkachel einzeln: ein zufaelliger 96-px-Block, darin
zufaellig eine der vier vollen Zellen (Spalte 3..6, Zeile 0 je Block, 32x32).
Ein Blatt, das als durchgehende Flaeche gemalt ist (Motive laufen ueber
Zellkanten), zeigt deshalb ein sichtbares Raster: der mittlere Kontrast an der
Naht zwischen zwei zufaellig gewuerfelten Nachbarzellen ist deutlich hoeher
als der Kontrast innerhalb einer Zelle. Ein Blatt, dessen Motive innerhalb
der Zelle bleiben, zeigt keinen Unterschied.

Dieses Werkzeug misst genau das: Kante vs. Innen, als PASS/FAIL, und schreibt
zusaetzlich ein Feldbild (12x12 Kacheln, Spiellogik wie scene_preview.py,
2x skaliert) nach build/qa/splatseam/, damit man das Raster auch sieht statt
nur die Zahl zu lesen.

Aufruf:
    PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/splat_seam_audit.py \\
        src/main/resources/tiles/cloudturf_splat.png

    ... --vanilla     zusaetzlich grass/sand/junkfloor aus dem Vanilla-Dump
    ... --samples N   Anzahl gewuerfelter Nachbarpaare (Default 3000)
    ... --threshold F FAIL wenn Kante > F * Innen (Default 1.3)

Exit 0 wenn alle gepruefen Blaetter PASS sind, sonst 1.
"""
import argparse
import hashlib
import os
import random
import sys

TILE = 32
WIDTH = 224
BLOCK_H = 96
FULL_COLS = (3, 4, 5, 6)

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO, "tools"))


def default_vanilla_dir():
    from size_audit import default_vanilla
    return default_vanilla()


def load(path):
    from PIL import Image
    return Image.open(path).convert("RGBA")


def full_cells(im):
    """{(block, col): 32x32 RGB-Ausschnitt} fuer jede volle Zelle."""
    blocks = im.height // BLOCK_H
    cells = {}
    for b in range(blocks):
        for col in FULL_COLS:
            box = (col * TILE, b * BLOCK_H, col * TILE + TILE, b * BLOCK_H + TILE)
            cells[(b, col)] = im.crop(box).convert("RGB")
    return cells


def lum(px):
    r, g, b = px
    return 0.299 * r + 0.587 * g + 0.114 * b


def col_lums(im, x):
    px = im.load()
    return [lum(px[x, y]) for y in range(im.height)]


def row_lums(im, y):
    px = im.load()
    return [lum(px[x, y]) for x in range(im.width)]


def edge_diff(a, b, axis):
    """Mittlerer Betrag der Helligkeitsdifferenz an der Naht a|b."""
    if axis == "h":
        la, lb = col_lums(a, TILE - 1), col_lums(b, 0)
    else:
        la, lb = row_lums(a, TILE - 1), row_lums(b, 0)
    return sum(abs(x - y) for x, y in zip(la, lb)) / len(la)


def inner_diff(im):
    """Mittlerer Betrag der Helligkeitsdifferenz zwischen Nachbarpixeln
    innerhalb einer Zelle (waagerecht + senkrecht) -- die Referenz-'Textur'."""
    px = im.load()
    w, h = im.size
    total, n = 0.0, 0
    for y in range(h):
        for x in range(w - 1):
            total += abs(lum(px[x, y]) - lum(px[x + 1, y]))
            n += 1
    for x in range(w):
        for y in range(h - 1):
            total += abs(lum(px[x, y]) - lum(px[x, y + 1]))
            n += 1
    return total / n


def measure(path, samples=3000, threshold=1.3, seed=1):
    im = load(path)
    cells = full_cells(im)
    keys = list(cells)
    if not keys:
        return {"path": path, "error": "keine vollen Zellen gefunden (Geometrie falsch?)"}
    rng = random.Random(seed)
    edge_vals = []
    for _ in range(samples):
        k1 = rng.choice(keys)
        k2 = rng.choice(keys)
        axis = rng.choice(("h", "v"))
        edge_vals.append(edge_diff(cells[k1], cells[k2], axis))
    inner_vals = [inner_diff(cells[k]) for k in keys]
    edge_mean = sum(edge_vals) / len(edge_vals)
    inner_mean = sum(inner_vals) / len(inner_vals)
    ratio = edge_mean / inner_mean if inner_mean else float("inf")
    passed = ratio <= threshold
    return {
        "path": path, "edge": edge_mean, "inner": inner_mean, "ratio": ratio,
        "threshold": threshold, "pass": passed, "cells": len(keys),
    }


def hash01(x, y, salt):
    h = hashlib.md5(("%d,%d,%s" % (x, y, salt)).encode()).digest()
    return h[0] / 256.0


def field_image(path, tiles=12, scale=2):
    """12x12-Kachelfeld wie im Spiel gewuerfelt (scene_preview.ground_tile)."""
    from PIL import Image
    im = load(path)
    blocks = im.height // BLOCK_H
    salt = os.path.basename(path)
    canvas = Image.new("RGB", (tiles * TILE, tiles * TILE))
    for ty in range(tiles):
        for tx in range(tiles):
            block = int(hash01(tx, ty, salt + "5") * blocks)
            col = 3 + int(hash01(tx, ty, salt + "9") * 4)
            cell = im.crop((col * TILE, block * BLOCK_H, col * TILE + TILE, block * BLOCK_H + TILE))
            canvas.paste(cell.convert("RGB"), (tx * TILE, ty * TILE))
    return canvas.resize((canvas.width * scale, canvas.height * scale), Image.NEAREST)


def report(res):
    if "error" in res:
        print("%s -- FEHLER: %s" % (os.path.basename(res["path"]), res["error"]))
        return False
    status = "PASS" if res["pass"] else "FAIL"
    print("%-28s Kante %6.2f  Innen %6.2f  Verhaeltnis %5.2f  (Schwelle %.2f)  %s  [%d Zellen]"
          % (os.path.basename(res["path"]), res["edge"], res["inner"], res["ratio"],
             res["threshold"], status, res["cells"]))
    return res["pass"]


def main():
    ap = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("splats", nargs="*")
    ap.add_argument("--vanilla", action="store_true",
                     help="zusaetzlich grass/sand/junkfloor aus dem Vanilla-Dump pruefen")
    ap.add_argument("--samples", type=int, default=3000)
    ap.add_argument("--threshold", type=float, default=1.3)
    ap.add_argument("--out", default=os.path.join(REPO, "build", "qa", "splatseam"))
    args = ap.parse_args()

    paths = list(args.splats)
    if args.vanilla:
        vdir = default_vanilla_dir()
        for name in ("grass", "sand", "junkfloor"):
            p = os.path.join(vdir, "tiles", "%s_splat.png" % name)
            if os.path.exists(p):
                paths.append(p)
            else:
                print("Vanilla nicht gefunden: %s" % p)

    if not paths:
        ap.error("kein Blatt angegeben (und --vanilla nicht gesetzt)")

    os.makedirs(args.out, exist_ok=True)
    all_pass = True
    for p in paths:
        res = measure(p, samples=args.samples, threshold=args.threshold)
        ok = report(res)
        all_pass = all_pass and ok
        if "error" not in res:
            name = os.path.splitext(os.path.basename(p))[0]
            out_path = os.path.join(args.out, "%s_field.png" % name)
            field_image(p).save(out_path)
            print("  Feldbild: %s" % out_path)

    return 0 if all_pass else 1


if __name__ == "__main__":
    sys.exit(main())
