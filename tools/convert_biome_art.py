#!/usr/bin/env python3
"""Convert the committed reference renders into shipped sprite sheets.

The art in docs/references/ was drawn by hand and supplied as smoothed renders.
Where a reference was *made from* pixel art, converting it beats redrawing it —
see tools/convert_reference.py for how that is decided and done. This script is
the reproducible record of which references become which shipped files and with
what parameters, so the sheets can be rebuilt from the committed references
instead of being opaque binaries.

Run: python3 tools/convert_biome_art.py
"""

import colorsys
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from convert_reference import modal_downsample, quantize_opaque  # noqa: E402

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "asset_generator"))
import gen_splats  # noqa: E402
from px import Canvas  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REFS = os.path.join(ROOT, "docs/references")
REFS_KK = os.path.join(ROOT, "src/main/resources/kk-sprites")
OBJECTS = os.path.join(ROOT, "src/main/resources/objects")
ITEMS = os.path.join(ROOT, "src/main/resources/items")


def strip(im, drop_marker=True):
    """Key the painted background, and the registration marker around it.

    The tree reference carries a saturated pure-red rim around every crown. It
    is a marker in the source art, not part of the tree, and it reads as a red
    halo in game. It cannot be removed by colour alone: the deeper red-orange
    variants use very similar reds *inside* the foliage, and filtering globally
    eats their crowns. So this floods inward from the image border through
    background-and-marker pixels only — a red pixel is dropped when it is part
    of the rim reachable from outside, and kept when it is foliage.
    """
    out = im.convert("RGBA")
    px = out.load()
    w, h = out.size

    def is_bg(x, y):
        r, g, b, _ = px[x, y]
        return r + g + b < 90

    def is_marker(x, y):
        r, g, b, _ = px[x, y]
        hue, light, sat = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
        deg = hue * 360
        return sat > 0.75 and (deg < 14 or deg > 348) and light < 0.66

    outside = bytearray(w * h)
    stack = []
    for x in range(w):
        for y in (0, h - 1):
            stack.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            stack.append((x, y))
    while stack:
        x, y = stack.pop()
        if not (0 <= x < w and 0 <= y < h) or outside[y * w + x]:
            continue
        if not (is_bg(x, y) or (drop_marker and is_marker(x, y))):
            continue
        outside[y * w + x] = 1
        stack.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))

    for y in range(h):
        for x in range(w):
            if outside[y * w + x]:
                px[x, y] = (0, 0, 0, 0)
            else:
                r, g, b, _ = px[x, y]
                px[x, y] = (r, g, b, 255)
    return out


def despeckle(im, min_neighbours=2):
    """Drop stranded pixels the marker strip leaves behind."""
    out = im.copy()
    px, src = out.load(), im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            if src[x, y][3] == 0:
                continue
            n = 0
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] > 0:
                    n += 1
            if n < min_neighbours:
                px[x, y] = (0, 0, 0, 0)
    return out


def build_skyseraph_tree():
    """The sky tree: one species, four variants, plain and frost-covered.

    The reference is laid out as vanilla lays a tree sheet out — column 0
    plain, column 1 snow-covered, four variants down. We cannot use that
    layout: TreeObject picks column 1 only when the tile is vanilla's snowID,
    which does not exist in the Skyreach, and spriteX has no override point.
    getTreeSpriteY *is* overridable, so the frost variants move into rows 4-7
    of a single-column sheet and SkyTreeObject picks them on cold ground.
    """
    src = Image.open(os.path.join(REFS, "skytree-reference.png"))
    grid = quantize_opaque(despeckle(strip(modal_downsample(src.convert("RGB"), 256, 512))), 30)
    # Only reference rows 0 and 2 survive conversion. In rows 1 and 3 the space
    # between crown and trunk is filled with the saturated red marker rather
    # than foliage (measured: hue 0-10, saturation 0.9-1.0 throughout the gap),
    # so keying the marker correctly leaves the crown floating. There is no
    # foliage there to recover - it would have to be drawn into the reference.
    USABLE = (0, 2)
    n = len(USABLE)
    sheet = Image.new("RGBA", (128, 128 * n * 2))
    for i, row in enumerate(USABLE):
        plain = grid.crop((0, row * 128, 128, row * 128 + 128))
        frost = grid.crop((128, row * 128, 256, row * 128 + 128))
        sheet.paste(plain, (0, i * 128))
        sheet.paste(frost, (0, (i + n) * 128))
    path = os.path.join(OBJECTS, "skyseraphtree.png")
    sheet.save(path)
    return path, sheet


def icon_from(cell, out_path, box=(2, 2, 28, 28)):
    """A 32x32 inventory icon holding the whole subject, modal-downsampled."""
    bbox = cell.getbbox()
    sub = cell.crop(bbox)
    tw, th = box[2] - box[0], box[3] - box[1]
    scale = min(tw / sub.width, th / sub.height)
    nw, nh = max(1, round(sub.width * scale)), max(1, round(sub.height * scale))
    rgb = Image.new("RGB", sub.size, (0, 0, 0))
    rgb.paste(sub.convert("RGB"), (0, 0), sub)
    small = modal_downsample(rgb, nw, nh)
    keyed = strip(small, drop_marker=False)
    icon = Image.new("RGBA", (32, 32))
    icon.paste(keyed, ((32 - nw) // 2, box[3] - nh), keyed)
    icon.save(out_path)
    return icon


def _inpaint(im, threshold=150):
    """Fill the painted holes in a reference field from its own texture.

    The floor reference is a field of cobbles with large black blobs punched
    through it - transparency painted flat, the same as everywhere else in this
    art. Blurring or averaging them closed would smear the cobbles, so each hole
    pixel is copied from a shifted position that is itself clean, smallest shift
    first, which keeps the local texture coherent.
    """
    out = im.copy()
    px, op = im.load(), out.load()
    w, h = im.size
    offsets = [(dx, dy) for r in (18, 24, 30, 36, 44)
               for dx, dy in ((r, 0), (-r, 0), (0, r), (0, -r),
                              (r, r), (-r, -r), (r, -r), (-r, r))]
    for y in range(h):
        for x in range(w):
            if sum(px[x, y]) >= threshold:
                continue
            for dx, dy in offsets:
                sx, sy = x + dx, y + dy
                if 0 <= sx < w and 0 <= sy < h and sum(px[sx, sy]) >= threshold:
                    op[x, y] = px[sx, sy]
                    break
            else:
                op[x, y] = (206, 224, 235)
    return out


def build_skyway_ground():
    """The Skyway Passages ground, from the reference field.

    The reference is a flat field of paving, not a splat atlas, so the pixels
    are the artist's and the layout is the engine's: 32px patches are cut out of
    the converted field and handed to gen_splats.build_splat, which paints them
    across a block and then masks every cell to its blend shape. That keeps the
    marching-square geometry and the diagonal-corner coverage bands that
    tile_behaviour_audit enforces, while the texture stays the supplied art.
    """
    src = Image.open(os.path.join(REFS, "skyway-floor-reference.png")).convert("RGB")
    w, h = src.size
    field = _inpaint(modal_downsample(src, round(w / 11.0), round(h / 11.0)))
    fw, fh = field.size
    fp = field.load()

    def material(block, x0, y0, salt, frame=0):
        # A stable per-cell window into the field, so the same cell of the same
        # variant always draws the same paving and the sheet stays deterministic.
        ox = (salt * 37) % (fw - 32)
        oy = (salt * 61) % (fh - 32)
        for x in range(32):
            for y in range(32):
                r, g, b = fp[ox + x, oy + y]
                block.put(x0 + x, y0 + y, (r, g, b, 255))

    tiles = os.path.join(ROOT, "src/main/resources/tiles")
    gen_splats.build_splat(os.path.join(tiles, "skyway_splat.png"), material,
                           variants=4, salt=0x5CA9)
    # the flat base tile the engine falls back to
    base = Canvas(32, 32)
    material(base, 0, 0, 0x5CA9)
    base.save(os.path.join(tiles, "skyway.png"))
    return os.path.join(tiles, "skyway_splat.png")


def repack_kk_tree(src_name, out_name):
    """Repack a supplied 2-column vanilla tree sheet into our frost layout.

    The supplied sheets follow vanilla exactly: column 0 plain, column 1
    snow-covered, four variants down. We cannot use that column, because
    TreeObject reaches for it only when the tile is vanilla's snowID, and
    spriteX has no override point - on Skyreach ground the second column would
    never draw. getTreeSpriteY *is* overridable, so the cold forms move into
    the lower half of a single-column sheet and SkyTreeObject picks the half
    from the ground. Nothing about the artwork changes; only its arrangement.
    """
    src = Image.open(os.path.join(ROOT, "src/main/resources/kk-sprites", src_name)).convert("RGBA")
    w, h = src.size
    rows = h // 128
    sheet = Image.new("RGBA", (128, 128 * rows * 2))
    for row in range(rows):
        plain = src.crop((0, row * 128, 128, row * 128 + 128))
        frost = src.crop((128, row * 128, 256, row * 128 + 128))
        sheet.paste(plain, (0, row * 128))
        sheet.paste(frost, (0, (row + rows) * 128))
    path = os.path.join(OBJECTS, out_name)
    sheet.save(path)
    return path, sheet


# Highest row vanilla ever starts each door cell on, measured over all 28
# vanilla 352x128 wall sheets. Mirrors tools/sheet_format_audit.DOOR_CELLS.
DOOR_CEILING = {3: 72, 4: 50, 5: 50, 6: 48, 7: 64, 8: 68, 9: 52, 10: 72}


def fit_door_cells(src_name, out_name):
    """Squash only the door cells whose art would draw off the top of the wall.

    WallDoorObject draws each 32x128 cell at drawY - 96, so sheet row 96 is the
    tile's top edge and anything far above it hangs in the air over the wall.
    A cell already inside vanilla's range is left byte-identical; one that is
    over gets its content scaled vertically, anchored at the bottom row, so the
    top lands exactly on the ceiling. Nearest-neighbour, so it stays pixel art.

    Faint glow above the window rows is cleared rather than scaled: it is not
    drawn content, and scaling it would drag the whole cell down for nothing.
    """
    src = Image.open(os.path.join(REFS_KK, src_name)).convert("RGBA")
    px = src.load()

    # window insert rows 2-4 must be empty; anything there is glow bleed
    cleared = 0
    for row in (2, 3, 4):
        for x in range(64, 96):
            for y in range(row * 16, row * 16 + 16):
                if px[x, y][3] > 0:
                    px[x, y] = (0, 0, 0, 0)
                    cleared += 1

    report = []
    for cell, ceiling in DOOR_CEILING.items():
        x0 = cell * 32
        ys = [y for x in range(x0, x0 + 32) for y in range(128) if px[x, y][3] > 0]
        if not ys:
            continue
        top, bot = min(ys), max(ys)
        if top >= ceiling:
            report.append((cell, top, top, 1.0))
            continue
        strip = src.crop((x0, top, x0 + 32, bot + 1))
        new_h = bot + 1 - ceiling
        squashed = strip.resize((32, new_h), Image.NEAREST)
        src.paste((0, 0, 0, 0), (x0, 0, x0 + 32, 128))
        src.paste(squashed, (x0, ceiling), squashed)
        report.append((cell, top, ceiling, new_h / strip.height))

    path = os.path.join(OBJECTS, out_name)
    src.save(path)
    return path, cleared, report


def beetle_item_icons():
    """Wall, door and window icons cut from the supplied sheet itself.

    Vanilla's wall icons are a 20x28 chunk of the material in a 32x32 slot
    (stonewall 560 opaque px, stonedoor 456, stonewindow 536), so these take
    the same crops out of the sheet the wall actually draws from rather than
    inventing a second drawing of the same material.
    """
    src = Image.open(os.path.join(OBJECTS, "beetlewall.png")).convert("RGBA")
    out = {}

    def slot(strip, name, box=(6, 2, 26, 30)):
        w, h = box[2] - box[0], box[3] - box[1]
        icon = Image.new("RGBA", (32, 32))
        fitted = strip.resize((w, h), Image.NEAREST)
        icon.paste(fitted, (box[0], box[1]), fitted)
        path = os.path.join(ITEMS, name)
        icon.save(path)
        out[name] = sum(1 for q in icon.get_flattened_data() if q[3] > 0)
        return path

    # wall: the body's front face, two tiles of course-work
    slot(src.crop((0, 48, 32, 128)), "beetlewall.png")
    # door: cell 7, the other closed head-on leaf. Cell 3 would be the obvious
    # pick but it is the one cell the fit had to squash hardest (0.53x), so it
    # carries the least mass; cell 7 needed only 0.93x and still reads as a
    # closed door with its skull crown.
    slot(src.crop((224, 86, 256, 128)), "beetledoor.png", (6, 6, 26, 30))
    # window: the front rows of the window insert, where the opening is
    slot(src.crop((64, 80, 96, 128)), "beetlewindow.png")
    return out


TILES = os.path.join(ROOT, "src/main/resources/tiles")


def copy_kk(src_name, out_dir, out_name):
    """A supplied sheet that is already ON the shipped format: copy, verbatim.

    The convert step still owns the file (one producer per path -- see
    generate_assets.py's CONVERTED guard) so the copy is reproducible from
    kk-sprites/ rather than being an opaque binary someone once placed.
    """
    src = Image.open(os.path.join(REFS_KK, src_name)).convert("RGBA")
    path = os.path.join(out_dir, out_name)
    src.save(path)
    return path, src


def main():
    path, sheet = build_skyseraph_tree()
    opaque = sum(1 for p in sheet.get_flattened_data() if p[3] > 0)
    print(f"{path}  128x1024  opaque {opaque}")
    icon = icon_from(sheet.crop((0, 0, 128, 128)), os.path.join(ITEMS, "skyseraphtree.png"))
    print(f"{os.path.join(ITEMS, 'skyseraphtree.png')}  32x32  "
          f"opaque {sum(1 for p in icon.get_flattened_data() if p[3] > 0)}")
    splat = build_skyway_ground()
    print(f"{splat}  {Image.open(splat).size}")

    # objects/beetlewall.png is NOT produced here any more.
    #
    # fit_door_cells could squash the supplied sheet's door cells back inside
    # the extents the engine draws them at, and that made the sheet pass
    # tools/sheet_format_audit.py — but the audit only guards GEOMETRY. The
    # supplied art is one continuous illustration painted across the 4x8 body
    # block, and that block is an auto-tile blob whose columns are tile HALVES
    # with a column-to-half mapping that changes between row groups. No repack
    # of those pixels can tile; the sheet had to be redrawn on the real layout,
    # which tools/asset_generator/gen_beetlewall.py now does. The supplied file
    # stays in kk-sprites/ as the source of record for the set's identity, and
    # fit_door_cells / beetle_item_icons stay below because the next supplied
    # sheet may well need them.

    # The Eden ground pair. Supplied on vanilla's exact formats (the names
    # record the source assets: overgrowngrass_splat 224x576, its seed 32x32),
    # so both are verbatim copies. The doubled t in the supplied splat's name
    # is the player's typo and is normalised here -- the shipped file matches
    # the tile's texture name "overgrowneden".
    for src, out_dir, out in (
            ("overgrowngrass_splat-overgrowneden_splatt.png", TILES, "overgrowneden_splat.png"),
            ("overgrowngrassseed-overgrownedenseed.png", ITEMS, "overgrownedenseed.png")):
        path, im = copy_kk(src, out_dir, out)
        print(f"{path}  {im.size}")

    path, sheet = repack_kk_tree("birchtree-new-cloudtree.png", "cloudtree.png")
    print(f"{path}  {sheet.size}  "
          f"opaque {sum(1 for p in sheet.get_flattened_data() if p[3] > 0)}")
    icon = icon_from(sheet.crop((0, 0, 128, 128)), os.path.join(ITEMS, "cloudtree.png"))
    print(f"{os.path.join(ITEMS, 'cloudtree.png')}  32x32  "
          f"opaque {sum(1 for p in icon.get_flattened_data() if p[3] > 0)}")


if __name__ == "__main__":
    main()
