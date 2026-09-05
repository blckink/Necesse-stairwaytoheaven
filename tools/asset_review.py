#!/usr/bin/env python3
"""Put a finished sheet in front of the player, with the context to judge it.

A measurement can prove a sheet is on-format. It cannot say whether the thing
reads as what it is meant to be -- so nothing reaches the game before the
player has looked at it. This builds what they need in order to look:

  * the vanilla original and the candidate **side by side**, same scale, so
    "did the frames stay where they were" is visible rather than asserted,
  * both at 1x and at 3x -- the 1x strip is the honest one, that is the size a
    player sees it at,
  * a checked context line: what it is, which realm, what it replaces, whether
    the size and the frame grid still hold.

With `--ask` it delivers that image to the phone and blocks on an approve/
reject/correct decision. Without it, it just writes the files and prints the
context -- which is what a remote session should do, since it has no phone and
no aethergate.

Usage:
    PYTHONPATH=/home/blackoffset/dev/pylib python3 tools/asset_review.py \\
        edenserpent build/asset-briefs/edenserpent/candidate.png
    ... --ask --job 20260905-204834-37996     deliver it and wait for a verdict
"""
import argparse
import json
import os
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO, "tools"))
QA = os.path.join(REPO, "build", "qa", "review")
AETHER = os.path.expanduser("~/aethergate")

from asset_worklist import build as build_worklist  # noqa: E402
from asset_brief import guess_cell                  # noqa: E402

DARK, LIGHT = (32, 32, 40, 255), (208, 208, 200, 255)


def compare_sheet(vanilla, candidate, out):
    """Vanilla left, candidate right, at 3x over dark and light, 1x beneath."""
    from PIL import Image, ImageDraw
    with Image.open(vanilla) as a, Image.open(candidate) as b:
        a, b = a.convert("RGBA"), b.convert("RGBA")
        w = max(a.width, b.width)
        h = max(a.height, b.height)
        pad, label = 8, 16
        scale = 3 if max(w, h) <= 260 else (2 if max(w, h) <= 520 else 1)

        cell_w, cell_h = w * scale, h * scale
        total_w = pad * 3 + cell_w * 2
        total_h = label + pad + cell_h * 2 + pad + label + h + pad * 2

        im = Image.new("RGBA", (total_w, total_h), (18, 18, 22, 255))
        d = ImageDraw.Draw(im)
        d.text((pad, 3), "VANILLA (base)", fill=(180, 180, 190, 255))
        d.text((pad * 2 + cell_w, 3), "CANDIDATE", fill=(255, 180, 90, 255))

        for col, img in ((0, a), (1, b)):
            big = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
            x = pad + col * (cell_w + pad)
            for row, ground in ((0, DARK), (1, LIGHT)):
                y = label + pad + row * cell_h
                d.rectangle([x, y, x + cell_w - 1, y + cell_h - 1], fill=ground)
                im.alpha_composite(big, (x, y))

        y1 = label + pad + cell_h * 2 + pad
        d.text((pad, y1), "1x -- the size a player actually sees:",
               fill=(180, 180, 190, 255))
        for col, img in ((0, a), (1, b)):
            im.alpha_composite(img, (pad + col * (cell_w + pad), y1 + label))
        im.save(out)
    return out


def check(item, candidate):
    """The measurable half of the verdict."""
    from PIL import Image
    notes = []
    ok = True
    want = item.get("actual_size") or item.get("declared_size")
    with Image.open(candidate) as im:
        got = list(im.size)
        colours = len(im.convert("RGBA").getcolors(maxcolors=1 << 24) or [])
        alpha = im.convert("RGBA").getchannel("A")
        transparent = alpha.getextrema()[0] < 255

    if want and got != want:
        if want and got[0] % want[0] == 0 and got[1] % want[1] == 0 \
                and got[0] // want[0] == got[1] // want[1]:
            notes.append("size %dx%d is an exact %dx upscale of %dx%d -- "
                         "asset_intake can downsample it"
                         % (*got, got[0] // want[0], *want))
        else:
            notes.append("SIZE WRONG: %dx%d, needs %dx%d" % (*got, *want))
            ok = False
    else:
        notes.append("size %dx%d exact" % tuple(got))

    if not transparent:
        notes.append("BACKGROUND NOT TRANSPARENT -- fully opaque")
        ok = False
    if colours > 300:
        notes.append("%d colours -- a smoothed render, not pixel art "
                     "(shipped sheets carry 19-38)" % colours)
        ok = False
    else:
        notes.append("%d colours" % colours)

    cell, _ = guess_cell(item)
    if cell and want:
        notes.append("frame grid %dpx: %d cols x %d rows"
                     % (cell, want[0] // cell, want[1] // cell))
    return ok, notes


def ask(job, title, detail, image):
    """Deliver the image, then block on the player's verdict."""
    out = os.path.join(AETHER, "jobs", job, "out")
    os.makedirs(out, exist_ok=True)
    name = os.path.basename(image)
    if os.path.abspath(image) != os.path.abspath(os.path.join(out, name)):
        import shutil
        shutil.copy2(image, os.path.join(out, name))

    url = None
    try:
        r = subprocess.run([os.path.join(AETHER, "bin", "artifact.sh"),
                            "push", job, name],
                           capture_output=True, text=True, timeout=120)
        got = json.loads(r.stdout or "{}")
        if got.get("artifacts"):
            url = got["artifacts"][0]["url"]
    except Exception as exc:
        print("artifact push failed (%s) -- asking without a link" % exc,
              file=sys.stderr)

    body = detail + ("\n\nBild: " + url if url else "\n\n(kein Bild-Link)")
    p = subprocess.run([os.path.join(AETHER, "bin", "approve"), title,
                        "--risiko", "hoch", "--job", job, "--detail", body],
                       capture_output=True, text=True)
    try:
        verdict = json.loads(p.stdout or "{}")
    except json.JSONDecodeError:
        verdict = {"decision": "unbekannt", "raw": p.stdout}
    verdict["_url"] = url
    verdict["_exit"] = p.returncode
    return verdict


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("id")
    ap.add_argument("candidate")
    ap.add_argument("--ask", action="store_true",
                    help="deliver to the phone and wait for a verdict")
    ap.add_argument("--job", help="aethergate job id (required with --ask)")
    args = ap.parse_args()

    if args.ask and not args.job:
        ap.error("--ask needs --job")

    rows, _, _ = build_worklist()
    item = next((r for r in rows if r["id"] == args.id), None)
    if item is None:
        print("%s is not an open row in the worklist" % args.id, file=sys.stderr)
        return 1

    os.makedirs(QA, exist_ok=True)
    out = os.path.join(QA, "%s_review.png" % args.id)
    compare_sheet(item["vanilla_path"], args.candidate, out)
    ok, notes = check(item, args.candidate)

    detail = ("%s\nRealm: %s\nErsetzt: %s (vanilla)\n\nMessung:\n%s"
              % (item["what"], item["realm"], item["standin"],
                 "\n".join("  - " + n for n in notes)))
    print(detail)
    print("\nVergleichsbild: %s" % os.path.relpath(out, REPO))
    if not ok:
        print("\nMessung nicht bestanden -- das geht so nicht ins Spiel.")

    if not args.ask:
        return 0 if ok else 2

    verdict = ask(args.job,
                  "Sprite %s freigeben? (%s)" % (args.id, item["realm"]),
                  detail, out)
    print("\nVerdikt: %s" % json.dumps(verdict, ensure_ascii=False))
    ex = verdict.get("_exit")
    if ex == 0:
        print("FREIGEGEBEN -- weiter mit asset_intake --apply und dem Abhaken.")
        return 0
    if ex == 4:
        print("KORREKTUR: %s" % verdict.get("antwort", ""))
        return 4
    if ex == 3:
        print("GEPARKT -- Uebergabe schreiben und beenden.")
        return 3
    print("ABGELEHNT.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
