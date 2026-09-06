#!/usr/bin/env python3
"""One command from a generated image to a shippable sprite.

The workflow this replaces was: generate something in an image tool, open
Photoshop, guess the grid, nudge cells by hand, drop it in, find out in game
that it was wrong. The player's own words: "ohne manuelles rumgebastel in
Photoshop wie bisher".

What this does instead, per file in src/main/resources/kk-sprites/:

  1. READS THE NAME. The folder's convention already carries the intent
     (see kk-sprites/readme.md):
         <name>.png                      replaces our sprite of that name
         <name>-new-<ourname>.png        our <ourname>, drawn on vanilla <name>
         <folder>-<name>-now-<ourname>   same, with a folder hint
     The target path is looked up in the shipped resources, so the required
     SIZE and CLASS come from the file we already have rather than a guess.
  2. MEASURES IT. Exact size, integer upscale, smoothed render, or none of
     those.
  3. CONVERTS. An integer upscale is modal-downsampled (the most common colour
     of each source block, so a keyed background stays keyed instead of being
     averaged into the art). A smoothed render goes through
     convert_reference's grid detection first.
  4. CONFORMS + PREVIEWS by class. Walls get the full seam/region treatment
     from conform_wall_sheet; everything else gets the checks its class has
     (alpha, cell grid, size audit) and a 4x contact sheet on a light and a
     dark ground.
  5. REPORTS. One line per file, and a path to look at.

  6. CUTS THE ICON, for an object sheet that also has an items/<name>.png.
     The icon is taken from the finished sheet instead of being drawn a second
     time, so it cannot drift from the object it stands for -- but ONLY where
     the cut is 1:1: the first frame's art has to fit the 32x32 slot already.
     32 of the 76 shipped object/item pairs do. The rest need a drawn glyph,
     and the tool says so rather than shrinking a tree into a green smudge
     (tools/asset_templates.py, template_item). An existing icon is only
     replaced when docs/ASSET_REQUESTS.md still lists it as borrowed.

Mob icons are deliberately NOT cut here, and need no extractor: every
registered mob either ships its own mobs/icons/<id>.png (26 of them, none of
them borrowed) or overrides getMobIcon() through mobs/BorrowedMobIcon (19),
and the remaining 11 are countKillStat=false, so they have no journal row at
all. See docs/ASSET_SWAP_PIPELINE.md for the one case that still needs a hand.

Nothing is written into src/main/resources unless you pass --apply, and even
then only for files whose class checks passed.

    python3 tools/asset_intake.py                    # report on everything new
    python3 tools/asset_intake.py --apply            # ...and ship what passes
    python3 tools/asset_intake.py foo.png            # one file, anywhere
"""
import argparse
import collections
import os
import subprocess
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO, "tools"))
RES = os.path.join(REPO, "src", "main", "resources")
KK = os.path.join(RES, "kk-sprites")
QA = os.path.join(REPO, "build", "qa", "intake")

# Where a bare name is looked for, in order. First hit wins.
SEARCH_DIRS = ("objects", "objects/statues", "mobs", "mobs/icons", "items",
               "tiles", "player/armor", "player/weapons", "projectiles")


def candidates_for(stem):
    """Every shipped resource a supplied file's name could mean.

    The convention is read left to right; only the LAST name in a `-new-` /
    `-now-` chain is ours -- the earlier ones name the vanilla asset the art
    was drawn ON, which is a format reference, not a target. A folder prefix
    may be glued on the front and, per kk-sprites/readme.md, IS NOT TRUSTED:
    the evilwall pair arrived with `items-` on the 128x208 object sheet and
    `objects-` on the 32x32 icon. So every plausible reading is collected and
    the SIZE decides, because size cannot lie.
    """
    name = stem
    for sep in ("-now-", "-new-"):
        if sep in name:
            name = name.split(sep)[-1]
    if name.endswith("-new"):
        name = name[:-4]
    names = {name}
    if "-" in name:
        names.add(name.split("-", 1)[1])       # drop a folder-ish prefix
    # a splat's name may carry a typo in the suffix; normalise the common one
    for n in list(names):
        if n.endswith("_splatt"):
            names.add(n[:-1])
    out = []
    for cand in sorted(names):
        for d in SEARCH_DIRS:
            rel = os.path.join(d, cand + ".png")
            full = os.path.join(RES, rel)
            if os.path.exists(full):
                out.append((rel, Image.open(full).size))
    return out


def target_for(stem, supplied_size):
    """Pick the candidate whose size the supplied file can actually become.

    Exact match wins, then an integer upscale, then anything. Without this the
    32x32 evilwall ICON resolves to the 128x208 object SHEET purely because
    'objects' sorts before 'items' in the search order -- which is the same
    do-not-trust-the-prefix trap the readme records.
    """
    cands = candidates_for(stem)
    if not cands:
        return None, None, []
    exact = [c for c in cands if c[1] == supplied_size]
    if exact:
        return exact[0][0], exact[0][1], cands
    scaled = [c for c in cands
              if supplied_size[0] % c[1][0] == 0 and supplied_size[1] % c[1][1] == 0
              and supplied_size[0] // c[1][0] == supplied_size[1] // c[1][1]]
    if scaled:
        return scaled[0][0], scaled[0][1], cands
    return cands[0][0], cands[0][1], cands


def near_names(stem, limit=4):
    """Shipped resources whose name shares a long run with this one -- so a
    typo ('_splatt') gets a suggestion instead of a shrug."""
    key = stem.lower().replace("_", "").replace("-", "")
    hits = []
    for d in SEARCH_DIRS:
        dd = os.path.join(RES, d)
        if not os.path.isdir(dd):
            continue
        for f in os.listdir(dd):
            if not f.endswith(".png"):
                continue
            cand = os.path.splitext(f)[0].lower().replace("_", "").replace("-", "")
            n = 0
            for i in range(len(cand)):
                for j in range(i + 4, len(cand) + 1):
                    if cand[i:j] in key:
                        n = max(n, j - i)
            if n >= 6:
                hits.append((n, os.path.join(d, f)))
    hits.sort(reverse=True)
    return [h[1] for h in hits[:limit]]


def classify(rel, size):
    """Asset class from the shipped path and size. Every rule here is a fact
    about how the engine reads that path (docs/research/asset-formats.md)."""
    if rel is None:
        return "unknown"
    d = os.path.dirname(rel)
    if d == "tiles":
        return "splat" if rel.endswith("_splat.png") else "tile"
    if d == "items" or (size == (32, 32) and d.startswith("mobs/icons")):
        return "item"
    if d.startswith("mobs"):
        if size and size[0] % 6 == 0 and size[1] >= 256:
            return "mob"
        return "mob-other"
    if d.startswith("objects"):
        return "wall" if size == (352, 128) else "object"
    return "other"


def modal_downsample_rgba(im, out_w, out_h):
    w, h = im.size
    px = im.load()
    out = Image.new("RGBA", (out_w, out_h))
    op = out.load()
    for oy in range(out_h):
        y0, y1 = oy * h // out_h, max(oy * h // out_h + 1, (oy + 1) * h // out_h)
        for ox in range(out_w):
            x0, x1 = ox * w // out_w, max(ox * w // out_w + 1, (ox + 1) * w // out_w)
            counts = collections.Counter()
            for y in range(y0, y1):
                for x in range(x0, x1):
                    r, g, b, a = px[x, y]
                    counts[(r, g, b, 255) if a >= 128 else (0, 0, 0, 0)] += 1
            op[ox, oy] = counts.most_common(1)[0][0]
    return out


def fit_to(im, want):
    """(image, note). Exact / integer upscale / near miss / refuse."""
    if im.size == want:
        return im, "size exact"
    if (im.width % want[0] == 0 and im.height % want[1] == 0
            and im.width // want[0] == im.height // want[1]):
        f = im.width // want[0]
        return modal_downsample_rgba(im, *want), "downsampled %dx (modal)" % f
    if im.width * im.height == want[0] * want[1]:
        return None, ("REPACK, not a resize: %dx%d holds the same pixels as "
                      "%dx%d but in a different arrangement. Which cell goes "
                      "where is a per-asset decision -- see the repack "
                      "functions in tools/convert_biome_art.py (repack_kk_tree "
                      "is the worked example)." % (im.size + want))
    if abs(im.width - want[0]) <= 8 and abs(im.height - want[1]) <= 8:
        base = Image.new("RGBA", want, (0, 0, 0, 0))
        base.alpha_composite(im.crop((0, 0, min(im.width, want[0]),
                                      min(im.height, want[1]))))
        return base, "padded/cropped from %dx%d" % im.size
    return None, ("REFUSED: %dx%d against %dx%d -- not exact, not an integer "
                  "upscale, not within 8px. Regenerate at the template size, "
                  "or at an exact 2x/3x/4x of it." % (im.size + want))


def contact_sheet(im, label, out_path, zoom=4):
    """4x on a dark and a light ground, side by side, at the top; 1x beneath."""
    from PIL import ImageDraw, ImageFont
    try:
        font = ImageFont.truetype(
            "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf", 14)
    except OSError:
        font = ImageFont.load_default()
    w, h = im.size
    zw, zh = w * zoom, h * zoom
    pad, head = 12, 26
    sheet = Image.new("RGBA", (zw * 2 + pad * 3, zh + head + pad * 2 + h + 16),
                      (24, 24, 30, 255))
    for i, ground in enumerate(((42, 42, 52, 255), (206, 210, 216, 255))):
        panel = Image.new("RGBA", (zw, zh), ground)
        panel.alpha_composite(im.resize((zw, zh), Image.NEAREST))
        sheet.alpha_composite(panel, (pad + i * (zw + pad), head))
    sheet.alpha_composite(im, (pad, head + zh + pad))
    d = ImageDraw.Draw(sheet)
    d.text((pad, 6), "%s   %dx%d   4x on dark / on light, then 1x"
           % (label, w, h), fill=(235, 235, 240), font=font)
    sheet.save(out_path)
    return out_path


# The only column pitches an object sheet is documented to use. Generic
# objects and furniture are "32 x however many variants/rotations"
# (docs/research/asset-formats.md 3; docs/research/furniture-formats.md:40-55).
# SingleRockObject reserves a 64px slot and draws its left 32
# (asset-formats.md:211). TreeObject uses 128px cells
# (tools/asset_generator/gen_trees.py:4-10). Nothing else is guessed.
CELL_PITCHES = (32, 64, 128)


def straddling_rows(sheet, pitch):
    """How badly the art crosses the cell boundaries this pitch implies.

    Counted as rows where an opaque pixel sits on BOTH sides of a boundary --
    i.e. one drawing running through the seam. A pitch that splits a tree
    canopy down the middle scores in the hundreds; a real column boundary
    between two rotations scores zero.
    """
    alpha = sheet.getchannel("A").load()
    worst = 0
    for x in range(pitch, sheet.width, pitch):
        n = sum(1 for y in range(sheet.height)
                if alpha[x - 1, y] >= 128 and alpha[x, y] >= 128)
        worst = max(worst, n)
    return worst


def column_pitch(sheet):
    """Width of the sheet's first frame, measured rather than assumed.

    The smallest documented pitch whose seams the art does not run through
    wins. When none is clean the WHOLE sheet is treated as one frame -- the
    safe direction, because a too-wide frame can only make the derived icon
    look too big and be refused, while a too-narrow one would silently cut a
    tree trunk out of its canopy and call it an icon.

    The cost of erring that way: two variants that TOUCH, with no transparent
    column between them, are indistinguishable from one drawing crossing the
    seam, so their sheet counts as a single frame and its icon is refused.
    objects/aetheriumore and objects/gloomshroom (both 64x32 with 32-wide art)
    are the two shipped examples.
    """
    for pitch in CELL_PITCHES:
        if sheet.width % pitch or sheet.width == pitch:
            continue
        if straddling_rows(sheet, pitch) <= 2:
            return pitch
    return sheet.width


def icon_from_object_sheet(sheet, size=(32, 32)):
    """(icon, note). The first frame cut 1:1 into the inventory slot, or None.

    An icon is only derived when the frame's art ALREADY fits the slot, so the
    cut is pixel-for-pixel and cannot be wrong. Anything bigger would have to
    be scaled down, and a shrunken world sprite is not a Necesse icon -- see
    tools/asset_templates.py template_item(): "a tree's icon is a compact
    glyph, not the 128x1024 world sheet scaled down". Those stay drawn.
    """
    frame = sheet.crop((0, 0, column_pitch(sheet), sheet.height))
    bbox = frame.getchannel("A").getbbox()
    if bbox is None:
        return None, "first %dpx frame is empty" % frame.width
    art = frame.crop(bbox)
    if art.width > size[0] or art.height > size[1]:
        return None, ("first frame's art is %dx%d, too big for a %dx%d slot -- "
                      "an icon for this one has to be DRAWN as a compact glyph"
                      % (art.width, art.height, size[0], size[1]))
    icon = Image.new("RGBA", size, (0, 0, 0, 0))
    icon.alpha_composite(art, ((size[0] - art.width) // 2,
                               (size[1] - art.height) // 2))
    note = "cut 1:1 from the first %dpx frame, art %dx%d" % (
        frame.width, art.width, art.height)
    if art.width > size[0] - 4 or art.height > size[1] - 4:
        note += " (fills past the 28x28 safe area)"
    return icon, note


def companion_item(rel):
    """(relative path, slot size, exists) for an object's same-name item icon.

    A missing file is not automatically a missing icon: most objects that ship
    without one are the auto-registered multi-tile halves, which must NOT have
    an icon (tools/furniture_audit.py rule 7). So an icon is only invented for
    a name docs/ASSET_REQUESTS.md explicitly still lists as a borrowed
    items/ stand-in -- those are the ones waiting for exactly this file.
    """
    if not rel or not rel.startswith("objects/"):
        return None
    name = os.path.basename(rel)
    item_rel = os.path.join("items", name)
    item_path = os.path.join(RES, item_rel)
    if os.path.isfile(item_path):
        with Image.open(item_path) as current:
            return item_rel, current.size, True
    if icon_is_still_borrowed(os.path.splitext(name)[0]):
        return item_rel, (32, 32), False
    return None


def icon_is_still_borrowed(name, _cache={}):
    """Is items/<name>.png still a vanilla loan, or is it our own art?

    PROJECT.md: "Keine gelieferte Handzeichnung wird ueberschrieben." A
    derived icon may replace a stand-in we borrowed; it may not replace
    something that was drawn for us. docs/ASSET_REQUESTS.md is the loan
    register and asset_worklist reads it, so ask that rather than guess.
    """
    if "rows" not in _cache:
        try:
            import asset_worklist
            open_rows, _broken, _done = asset_worklist.build()
        except Exception:
            _cache["rows"] = None
        else:
            _cache["rows"] = {
                r["id"].strip(" ,.").lower() for r in open_rows
                if (r.get("standin") or "").startswith("items/")}
    rows = _cache["rows"]
    if rows is None:
        return None                     # register unreadable: caller decides
    return name.lower() in rows


def run(cmd):
    p = subprocess.run(cmd, capture_output=True, text=True, cwd=REPO)
    return p.returncode, (p.stdout or "") + (p.stderr or "")


def process(path, apply_it, overwrite_icons=False):
    stem = os.path.splitext(os.path.basename(path))[0]
    im = Image.open(path).convert("RGBA")
    rel, want, cands = target_for(stem, im.size)
    klass = classify(rel, want)
    print("== %s" % os.path.basename(path))
    if rel is None:
        print("   supplied %dx%d -- no shipped sprite of that name." % im.size)
        near = near_names(stem)
        if near:
            print("   Did you mean one of: %s" % ", ".join(near))
        print("   Otherwise this is NEW art: say where it goes, or name the file")
        print("   after the sprite it replaces (see kk-sprites/readme.md).")
        return 1
    print("   target   %s   class %s   wants %dx%d" % (rel, klass, want[0], want[1]))
    if len(cands) > 1:
        print("   (name also matched %s -- size chose)"
              % ", ".join(c[0] for c in cands if c[0] != rel))

    fitted, note = fit_to(im, want)
    print("   size     %s" % note)
    if fitted is None:
        return 1

    os.makedirs(QA, exist_ok=True)
    staged = os.path.join(QA, stem + "_staged.png")
    fitted.save(staged)

    ok = True
    if klass == "wall":
        code, out = run([sys.executable, "tools/conform_wall_sheet.py", staged,
                         "--fix", "-o", os.path.join(QA, stem + "_conformed.png")])
        for line in out.splitlines():
            if line.startswith("  "):
                print("   " + line.strip())
        ok = (code == 0)
        if os.path.exists(os.path.join(QA, stem + "_conformed.png")):
            fitted = Image.open(os.path.join(QA, stem + "_conformed.png")).convert("RGBA")
    else:
        # class checks that do not need the game: transparency present where a
        # sprite needs it, and the cell grid divides evenly.
        opaque = sum(1 for p in fitted.get_flattened_data() if p[3] >= 128)
        total = fitted.width * fitted.height
        if klass in ("mob", "object", "item") and opaque == total:
            print("   FIX      fully opaque -- a sprite needs a transparent "
                  "background, or it ships as a rectangle")
            ok = False
        if klass == "mob" and fitted.width % 6:
            print("   FIX      width %d is not 6 columns" % fitted.width)
            ok = False
        if klass == "splat" and (fitted.width % 224 or fitted.height % 96):
            print("   FIX      %dx%d is not 224*frames x 96*variants" % fitted.size)
            ok = False
        cols = len({p[:3] for p in fitted.get_flattened_data() if p[3] >= 128})
        print("   colours  %d%s" % (cols, "  (four figures means a smoothed "
              "render, not pixel art -- see docs/ASSET_PIPELINE.md)"
              if cols > 999 else ""))

    preview = contact_sheet(fitted, stem, os.path.join(QA, stem + "_preview.png"))
    print("   preview  %s" % os.path.relpath(preview, REPO))

    derived, item_rel = None, None
    companion = companion_item(rel) if klass == "object" else None
    if ok and companion:
        item_rel, item_size, item_exists = companion
        icon, note = icon_from_object_sheet(fitted, item_size)
        print("   icon     %s: %s" % (item_rel, note))
        if icon is not None:
            icon.save(os.path.join(QA, stem + "_item_staged.png"))
            icon_preview = contact_sheet(
                icon, stem + " item icon",
                os.path.join(QA, stem + "_item_preview.png"))
            print("   preview  %s" % os.path.relpath(icon_preview, REPO))
            # An icon that is still a borrowed stand-in may be paid back; one
            # that was drawn for us is not ours to overwrite (PROJECT.md).
            borrowed = icon_is_still_borrowed(os.path.splitext(
                os.path.basename(item_rel))[0])
            if not item_exists or borrowed or overwrite_icons:
                derived = icon
            elif borrowed is None:
                print("   icon     KEPT: the loan register could not be read, "
                      "so the shipped icon is left alone (--overwrite-icons)")
            else:
                print("   icon     KEPT: %s is not on the borrowed list, so it "
                      "is our own art (--overwrite-icons to replace it)"
                      % item_rel)

    if apply_it and ok:
        dest = os.path.join(RES, rel)
        fitted.save(dest)
        print("   APPLIED  -> %s" % rel)
        if derived is not None:
            derived.save(os.path.join(RES, item_rel))
            print("   APPLIED  -> %s (icon cut from the sheet%s)"
                  % (item_rel, "" if companion[2] else ", new file"))
        print("            remember: add it to generate_assets.py's CONVERTED")
        print("            guard, or the next generator run overwrites it")
    elif apply_it:
        print("   NOT APPLIED (checks above failed)")
    return 0 if ok else 1


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("files", nargs="*",
                    help="image files (default: everything in kk-sprites/)")
    ap.add_argument("--apply", action="store_true",
                    help="write passing files into src/main/resources")
    ap.add_argument("--overwrite-icons", action="store_true",
                    help="also replace an items/<name>.png that is NOT on the "
                         "borrowed list -- i.e. art that was drawn for us")
    args = ap.parse_args()
    files = args.files
    if not files:
        files = sorted(os.path.join(KK, f) for f in os.listdir(KK)
                       if f.lower().endswith(".png"))
    status = 0
    for f in files:
        status |= process(f, args.apply, args.overwrite_icons)
        print()
    print("previews in %s" % os.path.relpath(QA, REPO))
    return status


if __name__ == "__main__":
    sys.exit(main())
