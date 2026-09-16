#!/usr/bin/env python3
"""Direction gate for 4-way object sheets (rows = rotation 0 up, 1 right, 2 down, 3 left).

Size and alpha were all the earlier checks looked at, and a catapult whose
"north" row was the south picture again, and whose east/west rows were a 3/4
diagonal, passed them. This gate asks: does each row face its direction?

  - alpha 0/255 only, nothing touches a cell edge (clipped art)
  - row 3 is the per-cell mirror of row 1 (not a mirrored strip: frame order)
  - row 0 and row 2 differ (alpha-mask IoU below --max-ns-iou)
  - east row is wider than north/south rows (side profile vs. end-on)
  - within a row, frames differ from frame 0 only in a limited area (same base)
  - optional --template: vanilla sheet shown next to each row in the preview

The coordinator must be able to name each row's facing in the preview
WITHOUT reading the label.

Usage: direction_sheet_check.py sheet.png --cell 96x128 [--frames 4]
       [--template tpl.png --tcell 64x64] [--preview out.png]
Exit 1 on any FAIL.
"""
import argparse
import sys

from PIL import Image, ImageDraw, ImageChops

NAMES = ["N (0, oben)", "O (1, rechts)", "S (2, unten)", "W (3, links)"]


def cells(img, cw, ch, frames):
    return [[img.crop((c * cw, r * ch, (c + 1) * cw, (r + 1) * ch)) for c in range(frames)]
            for r in range(4)]


def mask(cell):
    return cell.getchannel("A").point(lambda a: 255 if a else 0)


def bbox_wh(cell):
    b = cell.getchannel("A").getbbox()
    return (0, 0) if not b else (b[2] - b[0], b[3] - b[1])


def iou(a, b):
    ma, mb = mask(a), mask(b)
    inter = ImageChops.multiply(ma, mb).histogram()[255]
    union = ImageChops.lighter(ma, mb).histogram()[255]
    return inter / union if union else 1.0


def diff_area(a, b):
    d = ImageChops.difference(a, b).convert("L").point(lambda v: 255 if v else 0)
    return d.histogram()[255]


def main():
    p = argparse.ArgumentParser()
    p.add_argument("sheet")
    p.add_argument("--cell", required=True)
    p.add_argument("--frames", type=int, default=4)
    p.add_argument("--template")
    p.add_argument("--tcell")
    p.add_argument("--max-ns-iou", type=float, default=0.85)
    p.add_argument("--max-anim-share", type=float, default=0.35)
    p.add_argument("--preview")
    a = p.parse_args()
    cw, ch = map(int, a.cell.split("x"))
    img = Image.open(a.sheet).convert("RGBA")
    fails = []
    if img.size != (cw * a.frames, ch * 4):
        print("FAIL size", img.size, "!=", (cw * a.frames, ch * 4))
        return 1
    alphas = set(img.getchannel("A").getdata())
    if not alphas <= {0, 255}:
        fails.append(f"soft alpha values {sorted(alphas)[:6]}")
    g = cells(img, cw, ch, a.frames)
    for r in range(4):
        for c in range(a.frames):
            al = g[r][c].getchannel("A")
            if not al.getbbox():
                fails.append(f"row {r} frame {c}: empty")
                continue
            b = al.getbbox()
            if b[0] == 0 or b[1] == 0 or b[2] == cw or b[3] == ch:
                fails.append(f"row {r} frame {c}: art touches the cell edge {b}")
    for c in range(a.frames):
        if ImageChops.difference(g[3][c], g[1][c].transpose(Image.FLIP_LEFT_RIGHT)).getbbox():
            fails.append(f"frame {c}: row 3 is not the per-cell mirror of row 1")
    ns = iou(g[0][0], g[2][0])
    print(f"IoU N/S frame0 = {ns:.2f} (max {a.max_ns_iou})")
    if ns > a.max_ns_iou:
        fails.append("rows N and S are (almost) the same picture")
    wh = [bbox_wh(g[r][0]) for r in range(4)]
    print("bbox w x h per row:", wh)
    if wh[1][0] <= max(wh[0][0], wh[2][0]):
        fails.append("east row is not wider than N/S rows: no side profile")
    for r in range(4):
        for c in range(1, a.frames):
            share = diff_area(g[r][0], g[r][c]) / (cw * ch)
            if share > a.max_anim_share:
                fails.append(f"row {r} frame {c}: {share:.0%} of the cell changes vs frame 0")
    t = None
    if a.template:
        tw, th = map(int, a.tcell.split("x"))
        t = cells(Image.open(a.template).convert("RGBA"), tw, th, 1)
        print("template bbox per row:", [bbox_wh(t[r][0]) for r in range(4)],
              f"IoU N/S = {iou(t[0][0], t[2][0]):.2f}")
    if a.preview:
        S = 3
        rowh = ch * S + 24
        tpw = 0
        if t:
            k = max(1, (ch * S) // th)
            tpw = tw * k + 16
        W = 180 + tpw + cw * a.frames * S
        out = Image.new("RGBA", (W, rowh * 4), (60, 90, 60, 255))
        d = ImageDraw.Draw(out)
        for r in range(4):
            y = r * rowh + 20
            d.text((4, r * rowh + 4), NAMES[r], fill=(255, 255, 255, 255))
            x = 170
            if t:
                tc = t[r][0].resize((tw * k, th * k), Image.NEAREST)
                out.alpha_composite(tc, (x, y))
                x += tpw
            for c in range(a.frames):
                out.alpha_composite(g[r][c].resize((cw * S, ch * S), Image.NEAREST), (x + c * cw * S, y))
                d.rectangle([x + c * cw * S, y, x + (c + 1) * cw * S - 1, y + ch * S - 1],
                            outline=(0, 0, 0, 90))
        out.save(a.preview)
        print("preview:", a.preview)
    for f in fails:
        print("FAIL", f)
    print("RESULT", "FAIL" if fails else "PASS")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
