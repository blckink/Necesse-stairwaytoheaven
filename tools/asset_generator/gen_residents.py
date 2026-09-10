"""Point 8n — five band trophies and three residents' own headwear.

WHY THIS IS A RECOLOUR AND NOT A DRAWING. 7n's method, restated by the order:
vanilla as the template, colour count under 38, frames normalised. Every
template below is a real vanilla sprite, read out of the player's own dump.

WHERE THE DUMP IS, because this cost a wrong turn once. `vanilla-sprites/` in
the repo is gitignored and EMPTY, and the client's `res.data` is not a zip, so
the two obvious places both come up dry. The dump the art tooling actually uses
lives outside the repo at the path `tools/size_audit.py` prints —
`/home/blackoffset/dev/Necesse sprites` — with 3,850 item icons and the full
`player/armor/` set in it. Check what size_audit reports before concluding that
vanilla art is unavailable.

The three hats take the very sheets their wearers were already dressed in:
Magpie's `trapperhat`, Halda's `battlechefhat`, Ossian's `runichat`. That is the
point rather than a shortcut — the silhouette the player has been looking at
does not change, only whose art it is.

HOW THE RECOLOUR WORKS. Each output is the template's own alpha and silhouette,
with its colour replaced by a per-realm luminance ramp: a pixel's brightness
picks a stop, the stop supplies the colour. That is what keeps the colour count
low by construction (one ramp = at most `len(ramp)` opaque colours) instead of
by a reduction pass afterwards, and it keeps the shape exactly as drawn, which
is the half of a sprite a generator has no business improvising.

The three hats add ONE accent each on top of the ramp, worked out per 32x32
cell from that cell's own opaque bounding box rather than from an assumed frame
order — so it lands on the hat in every direction and every animation frame
without this file needing to know which cell is which.
"""
import os

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SHIPPED = os.path.join(REPO, "src", "main", "resources")

def _vanilla_dump():
    """Where the vanilla sprites are, resolved exactly as size_audit does.

    Same order and same fallbacks as `tools/size_audit.default_vanilla`, so a
    machine on which the audit measures something is a machine on which this
    generator runs, and vice versa. NOT `vanilla-sprites/` in the repo: that
    one is gitignored and empty, and the client's `res.data` is not a zip.
    """
    for guess in (os.path.join(REPO, "vanilla-sprites"),
                  os.environ.get("NECESSE_SPRITE_DUMP") or "",
                  os.path.expanduser("~/dev/Necesse sprites"),
                  "/home/user/necesse-game/sprites"):
        if guess and os.path.isdir(os.path.join(guess, "items")):
            return guess
    return None

CELL = 32

# id -> (template item icon, ramp dark..light)
#
# The ramps are each realm's own palette per WORLD_DESIGN.md §36: Skyreach
# bright white/cream/light blue, Eden highly saturated green, Steinfeld pale,
# the Ghost band cold teal, Crooked Beyond violet against bone.
TROPHIES = {
    "skystoneheart": ("omnicrystal", [
        (0x2E, 0x3C, 0x55), (0x4C, 0x68, 0x90), (0x7E, 0xA6, 0xD2),
        (0xBB, 0xDA, 0xF2), (0xF2, 0xF7, 0xFB), (0xFF, 0xE9, 0xB8),
    ]),
    "bloomfang": ("crystalstone", [
        (0x24, 0x3A, 0x1E), (0x3C, 0x6B, 0x2C), (0x63, 0xA8, 0x3E),
        (0xA8, 0xD6, 0x62), (0xE6, 0xF0, 0xC0), (0xFF, 0xFC, 0xEA),
    ]),
    # Reversed on purpose: the template is a bright bolt of vanilla silk and
    # the item is a MOURNING band, so the ramp is walked the other way and the
    # cloth's lit face becomes its darkest. Nothing else here inverts.
    "mourningband": ("silk", [
        (0x1B, 0x1B, 0x20), (0x35, 0x35, 0x3E), (0x5A, 0x5A, 0x66),
        (0x8B, 0x8B, 0x96), (0xBD, 0xBD, 0xC4), (0xE4, 0xE2, 0xDD),
    ][::-1]),
    "soulcollar": ("emptypendant", [
        (0x14, 0x26, 0x2B), (0x1F, 0x45, 0x4C), (0x2F, 0x74, 0x7B),
        (0x54, 0xAD, 0xAA), (0x92, 0xDA, 0xCE), (0xDA, 0xF6, 0xEE),
    ]),
    "stripedhorn": ("bone", [
        (0x22, 0x14, 0x33), (0x42, 0x22, 0x60), (0x6E, 0x3A, 0x92),
        (0xB0, 0x8A, 0x5E), (0xE2, 0xCE, 0x9A), (0xF7, 0xEF, 0xD6),
    ]),
}

# id -> (vanilla armour sheet, accent, ramp). Each resident's own current hat.
HATS = {
    "magpiecap": ("trapperhat", "brim", [
        (0x1A, 0x17, 0x14), (0x33, 0x2A, 0x22), (0x55, 0x45, 0x33),
        (0x82, 0x69, 0x4B), (0xB5, 0x97, 0x70), (0xE0, 0xC8, 0xA2),
    ]),
    "haldakerchief": ("battlechefhat", "knot", [
        (0x3A, 0x18, 0x1C), (0x63, 0x25, 0x2B), (0x93, 0x3A, 0x3E),
        (0xC0, 0x60, 0x5C), (0xE2, 0x99, 0x90), (0xF6, 0xD8, 0xCE),
    ]),
    "vanecowl": ("runichat", "lens", [
        (0x16, 0x1C, 0x2C), (0x27, 0x33, 0x4E), (0x3E, 0x53, 0x7A),
        (0x60, 0x80, 0xAA), (0x96, 0xB6, 0xD6), (0xD6, 0xE8, 0xF6),
    ]),
}

# The one bright colour each accent writes. Kept out of the ramp so an accent
# is always distinguishable from the cloth it sits on.
ACCENT = {
    "brim": (0x2A, 0x22, 0x1B),
    "knot": (0xF7, 0xE6, 0xB0),
    "lens": (0xFF, 0xD5, 0x5E),
}


def _ramp_pixel(rgba, ramp):
    r, g, b, a = rgba
    if a == 0:
        return (0, 0, 0, 0)
    # Rec. 601 luma, which is what the eye reads a pixel-art shade as.
    lum = (299 * r + 587 * g + 114 * b) // 1000
    stop = lum * len(ramp) // 256
    if stop >= len(ramp):
        stop = len(ramp) - 1
    return ramp[stop] + (a,)


def _recolour(src, ramp):
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    px = src.load()
    op = out.load()
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            op[x, y] = _ramp_pixel(px[x, y], ramp)
    return out


def _accent(img, kind):
    """One signature mark per hat, placed off each cell's own opaque box.

    Nothing is written outside pixels the template already made opaque, so the
    silhouette the engine draws is the hood's, unchanged — only its colour and
    this mark differ between the three residents.
    """
    colour = ACCENT[kind] + (255,)
    px = img.load()
    w, h = img.size
    for cy in range(0, h, CELL):
        for cx in range(0, w, CELL):
            box = [(x, y)
                   for y in range(cy, min(cy + CELL, h))
                   for x in range(cx, min(cx + CELL, w))
                   if px[x, y][3] > 0]
            if not box:
                continue
            ys = [p[1] for p in box]
            xs = [p[0] for p in box]
            top, bottom, left, right = min(ys), max(ys), min(xs), max(xs)
            if bottom == top:
                continue
            if kind == "brim":
                # A courier's peaked cap: the lowest two rows of the hat, dark.
                rows = {bottom, bottom - 1}
                for x, y in box:
                    if y in rows:
                        px[x, y] = colour
            elif kind == "knot":
                # A kerchief knotted at the side: the outermost column, bright.
                edge = {left, right}
                for x, y in box:
                    if x in edge and y > top:
                        px[x, y] = colour
            elif kind == "lens":
                # A wright's loupe: two lit pixels on the brow row.
                brow = top + max(1, (bottom - top) // 3)
                row = sorted(x for x, y in box if y == brow)
                for x in row[:2]:
                    px[x, brow] = colour
    return img


def _write(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    return len(img.getcolors(65536) or [])


def _cut_icon(sheet):
    """The item icon, taken off the finished sheet rather than drawn again.

    The order for 8n: "Icons werden aus dem fertigen Sheet gezogen und nicht
    separat gezeichnet". For a helmet that is exact — a helmet cell IS 32x32,
    so the cut needs no scaling at all, the 1:1 case `tools/asset_intake.py`
    documents as the only one it will take.

    Cell (0, 2), not (0, 0): the sheet's first row is the back of the head, so
    cutting the origin cell would put the back of a hat in the inventory. Row 2
    is the south-facing row — the face opening is symmetric there — which is
    the view every vanilla helmet icon shows.
    """
    return sheet.crop((0, 2 * CELL, CELL, 3 * CELL))


def generate(out):
    """Write the eight sprites, or say why not and leave the shipped ones be.

    Without the dump this returns rather than raising. The eight PNGs are
    committed, so a machine with no vanilla sprites still builds a correct jar
    and `generate_assets.py` still exits 0 -- which is the gate OVERVIEW.md
    records. Raising here would turn "this one generator cannot run" into
    "the whole asset gate is red".
    """
    vanilla = _vanilla_dump()
    if vanilla is None:
        print("gen_residents (8n): skipped -- no vanilla sprite dump found.\n"
              "  Point NECESSE_SPRITE_DUMP at one, or see tools/size_audit.py.\n"
              "  The eight shipped PNGs are left untouched.")
        return

    report = []

    for name, (template, ramp) in sorted(TROPHIES.items()):
        src = Image.open(os.path.join(vanilla, "items", template + ".png")).convert("RGBA")
        n = _write(_recolour(src, ramp), os.path.join(out, "items", name + ".png"))
        report.append(f"  items/{name}.png  <- items/{template}.png  {n} colours")

    for name, (template, kind, ramp) in sorted(HATS.items()):
        src = Image.open(os.path.join(vanilla, "player", "armor", template + ".png")).convert("RGBA")
        sheet = _accent(_recolour(src, ramp), kind)
        n = _write(sheet, os.path.join(out, "player", "armor", name + ".png"))
        m = _write(_cut_icon(sheet), os.path.join(out, "items", name + ".png"))
        report.append(f"  player/armor/{name}.png  <- vanilla {template}  {n} colours"
                      f"  (icon cut from the south row: {m} colours)")

    print("gen_residents (8n):")
    for line in report:
        print(line)
