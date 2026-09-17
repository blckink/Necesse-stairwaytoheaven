"""Item icons for a wall set drawn on a vanilla wall's layout.

Vanilla's wall/door/window icons are not crops of the wall sheet: they are
hand-drawn 32x32 miniatures (items/ancientruinwall.png is a 20x28 block that
matches no 20x28 region of objects/ancientruinwall.png). A supplied wall sheet
drawn ON a vanilla layout therefore takes its icons from that vanilla wall, so
the silhouette, size and framing are vanilla's exactly, and only the colours
change.

The colours are mapped by luminance rank, not per pixel: a supplied sheet is a
redraw, not a recolour, so the same position carries a different tone in only
40-60% of the cells. Each palette is split into neutral tones and accents
(saturated colours: gems, glass, trim), and the n-th percentile of vanilla's
neutral ramp becomes the n-th percentile of ours, accents likewise.

The vanilla sprite dump (vanilla-sprites/, not in git) is needed. Without it the
shipped icons stay as they are.
"""

import colorsys
import os
from collections import Counter

from PIL import Image

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
VANILLA = os.path.join(REPO, "vanilla-sprites")


def _lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def _sat(c):
    return colorsys.rgb_to_hsv(*[x / 255 for x in c])[1]


def _accent(min_value, min_sat):
    return lambda c: max(c) > min_value and _sat(c) > min_sat


def _ramps(img, accent):
    """{is_accent: [(percentile, colour), ...]} sorted dark to light."""
    cnt = Counter(p[:3] for p in img.getdata() if p[3] > 0)
    out = {}
    for cls in (False, True):
        cols = sorted((c for c in cnt if accent(c) == cls), key=_lum)
        total = sum(cnt[c] for c in cols)
        seen = 0
        ramp = []
        for c in cols:
            ramp.append(((seen + cnt[c] / 2) / max(total, 1), c))
            seen += cnt[c]
        out[cls] = ramp
    return out


def remap_icon(vanilla_sheet, our_sheet, vanilla_icon, vanilla_accent, our_accent):
    vr = _ramps(vanilla_sheet, vanilla_accent)
    nr = _ramps(our_sheet, our_accent)
    icon = vanilla_icon.convert("RGBA").copy()
    px = icon.load()
    colours = {px[x, y][:3] for x in range(icon.width)
               for y in range(icon.height) if px[x, y][3]}
    mapping = {}
    for c in colours:
        cls = vanilla_accent(c)
        pos = {k: q for q, k in vr[cls]}
        if c in pos:
            q = pos[c]
        else:
            # An icon-only tone (vanilla's window glass has four): rank it
            # among the sheet's own tones of the same class.
            lums = [_lum(k) for _, k in vr[cls]] or [_lum(c)]
            q = sum(1 for v in lums if v <= _lum(c)) / len(lums)
        target = nr[cls] if nr[cls] else nr[not cls]
        mapping[c] = min(target, key=lambda t: abs(t[0] - q))[1]
    for y in range(icon.height):
        for x in range(icon.width):
            c = px[x, y]
            if c[3]:
                px[x, y] = tuple(mapping[c[:3]]) + (c[3],)
    return icon


def gen_wall_icons(objects_dir, items_dir, vanilla_prefix, our_prefix,
                   vanilla_accent=(80, 0.38), our_accent=(40, 0.45)):
    """items/<our>wall|door|window.png from vanilla's icons of <vanilla>.

    The accent thresholds are (minimum channel value, minimum saturation). The
    defaults suit spidercastle -> nightfell, where vanilla's pink trim sits at
    saturation 0.41 and its darkest neutral at 0.43 but channel value 61.
    """
    src = os.path.join(VANILLA, "objects", f"{vanilla_prefix}wall.png")
    if not os.path.exists(src):
        print(f"  wall_icon_remap: {src} missing, {our_prefix} icons left as shipped")
        return
    vsheet = Image.open(src).convert("RGBA")
    nsheet = Image.open(os.path.join(objects_dir, f"{our_prefix}wall.png")).convert("RGBA")
    for piece in ("wall", "door", "window"):
        vicon = Image.open(os.path.join(VANILLA, "items", f"{vanilla_prefix}{piece}.png"))
        remap_icon(vsheet, nsheet, vicon, _accent(*vanilla_accent),
                   _accent(*our_accent)).save(os.path.join(items_dir, f"{our_prefix}{piece}.png"))
