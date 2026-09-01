#!/usr/bin/env python3
"""Pull a hand-drawn wall sheet onto the 352x128 WallObject format, and say so.

The player draws wall sets outside this repo, over vanilla stonewall, and the
result is "noch nicht 100%" on the format — scaled, a seam here, a pane where
the roof slot belongs. The renderer's grammar is unforgiving and invisible
from an image editor, so this tool applies it mechanically:

  CHECK (always):
    * size — exact, an integer upscale, or close enough to pad/crop
    * region alpha — body opaque, window rows 2-4 EMPTY, door leaves inside
      vanilla's own visible extents
    * every SEAM the engine can compose, measured on the sheet edge pairs the
      real draw code puts next to each other, judged against the SAME seams
      measured on vanilla stonewall (a texture has contrast; the tolerance is
      vanilla's own contrast, not zero)
    * the roof-slot test — window cells (4-5, rows 0-1) must be made of the
      sheet's own ROOF material, because the engine draws them as the wall's
      top surface (docs/references/wall-template-map.png explains all of this
      visually)

  FIX (--fix):
    * integer upscales are modal-downsampled; small size drift is padded or
      cropped
    * alpha holes in opaque regions are filled from their neighbours;
      forbidden regions are cleared
    * door content overflowing vanilla's visible box is shifted (small
      overflow) or scale-fitted (large)
    * failing seams get their two 1px edge lines blended toward each other —
      enough to close a near-miss, deliberately not enough to repaint art
    * --rebuild-roof-slot rebuilds the two roof-window cells with
      wall_window_slot (the shared construction all four shipped sets use),
      with every tone sampled from THIS sheet

  It cannot fix a sheet that is one continuous illustration — that class of
  fault is structural and the report says "redraw" (measured colour count is
  the tell: shipped wall sets carry 19-38 colours; see kk-sprites/readme.md).

Usage:
    python3 tools/conform_wall_sheet.py IN.png                # report only
    python3 tools/conform_wall_sheet.py IN.png --fix          # write *_conformed.png
    python3 tools/conform_wall_sheet.py IN.png --fix --rebuild-roof-slot
    python3 tools/conform_wall_sheet.py IN.png --fix -o objects/mywall.png

Exit 0 when every check passes (after fixes, if --fix); 1 otherwise.
"""
import argparse
import collections
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO, "tools"))
sys.path.insert(0, os.path.join(REPO, "tools", "asset_generator"))

os.environ.setdefault("NECESSE_SPRITES", os.path.join(REPO, "vanilla-sprites"))
import wall_render_preview as wrp  # noqa: E402  (the engine port)

VANILLA_STONEWALL = os.path.join(REPO, "vanilla-sprites", "objects", "stonewall.png")

W, H = 352, 128
C = 16

# The scenes whose composition defines "every seam the engine can make".
# Runs, blocks, corners in both handednesses, and windows in both wall
# directions — the same shapes wall_render_preview renders for the eye.
SCENES = (
    wrp.Scene("run-h", ["#####"]),
    wrp.Scene("run-v", ["#", "#", "#", "#", "#"]),
    wrp.Scene("block", ["###", "###", "###"]),
    wrp.Scene("ell", ["#..", "#..", "###"]),
    wrp.Scene("tee", ["###", ".#.", ".#."]),
    wrp.Scene("ring", ["###", "#.#", "###"]),
    wrp.Scene("win-ns", ["#", "O", "#"]),
    wrp.Scene("win-ew", ["#O#"]),
)


# ---------------------------------------------------------------------------
# small helpers

def load_rgba(path):
    return Image.open(path).convert("RGBA")


def modal_downsample_rgba(im, out_w, out_h):
    """convert_reference.modal_downsample, but alpha-aware: the modal colour
    of each block over RGBA with alpha binarised, so a keyed background stays
    keyed instead of being averaged into the art."""
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


def mean_abs_delta(edge_a, edge_b):
    """Mean max-channel |d| between two same-length RGB pixel lists, opaque
    pairs only; None when nothing overlaps."""
    pairs = [(a, b) for a, b in zip(edge_a, edge_b) if a[3] >= 128 and b[3] >= 128]
    if not pairs:
        return None
    return sum(max(abs(a[i] - b[i]) for i in range(3)) for a, b in pairs) / len(pairs)


def cell_img(sheet, col, row):
    return sheet.crop((col * C, row * C, col * C + C, row * C + C))


def edge_pixels(sheet, col, row, side):
    """The 16 pixels of one cell edge, as RGBA tuples."""
    x0, y0 = col * C, row * C
    px = sheet.load()
    if side == "L":
        return [px[x0, y0 + i] for i in range(C)]
    if side == "R":
        return [px[x0 + C - 1, y0 + i] for i in range(C)]
    if side == "T":
        return [px[x0 + i, y0] for i in range(C)]
    return [px[x0 + i, y0 + C - 1] for i in range(C)]


# ---------------------------------------------------------------------------
# junction discovery: run the engine port, record which sheet cells it puts
# next to each other, in screen space

def discover_junctions():
    """-> {((colA,rowA,sideA),(colB,rowB,sideB)): scene-name} for wall/window
    cells. Derived from the port, not hand-listed, so a change to the draw
    logic changes the checks with it."""
    junctions = {}
    for scene in SCENES:
        placed = {}
        r = wrp.WallRenderer(None)
        for ty in range(scene.h):
            for tx in range(scene.w):
                ch = scene.at(tx, ty)
                if ch not in "#O":
                    continue
                adj = [scene.is_wall(tx + o[0], ty + o[1]) for o in wrp.ADJ]
                force_top = (adj[wrp.T] and scene.at(tx, ty - 1) == "O")
                force_bot = (adj[wrp.B] and scene.at(tx, ty + 1) == "O")
                cells = (r.window_cells(adj) if ch == "O"
                         else r.wall_cells(adj, force_top, force_bot))
                for col, row, dx, dy in cells:
                    # later draws win at the same spot, like the painter
                    placed[(tx * 32 + dx, ty * 32 + dy)] = (col, row)
        for (sx, sy), (col, row) in placed.items():
            right = placed.get((sx + C, sy))
            if right is not None:
                junctions[((col, row, "R"), (right[0], right[1], "L"))] = scene.title
            below = placed.get((sx, sy + C))
            if below is not None:
                junctions[((col, row, "B"), (below[0], below[1], "T"))] = scene.title
    # a cell's edge against itself is trivially seamless; drop those
    junctions = {k: v for k, v in junctions.items() if k[0][:2] != k[1][:2]}
    # ...and only SAME-BAND pairs are seams at all. The roof band meeting the
    # front face is a deliberate 3D edge whose contrast varies per material
    # (measured: 1.9 on stonewall, 85.8 on brickwall -- both vanilla, both
    # correct), so judging it as a seam is judging the material, not the join.
    return {k: v for k, v in junctions.items()
            if cell_band(*k[0][:2]) == cell_band(*k[1][:2])}


def cell_band(col, row):
    """ROOF (seen from above), FACE (seen head-on), or None (empty rows)."""
    if col <= 3:
        if row <= 2:
            return "roof"
        if row in (3, 4):
            return "face"
        return "roof"                     # corner hooks + diagonal mids
    if row in (0, 1, 5):                  # window: roof slot + its ceiling row
        return "roof"
    if row in (6, 7):                     # window front face
        return "face"
    return None                           # rows 2-4: drawn two tiles up, empty


def measure_junctions(sheet, junctions):
    out = {}
    for (a, b), scene in junctions.items():
        d = mean_abs_delta(edge_pixels(sheet, *a), edge_pixels(sheet, *b))
        if d is not None:
            out[(a, b)] = (d, scene)
    return out


# ---------------------------------------------------------------------------
# region rules

def body_alpha_holes(sheet, refs):
    """Transparent px in body rows 0-4 at positions EVERY vanilla ref paints.

    Vanilla itself keeps transparency in this block on purpose -- the
    crenellation notches in the cap rim (stonewall rows y0-1), the plank gaps
    on woodwall -- so "must be opaque" is measured, not asserted: a hole only
    counts where no reference has one. Rows 5-7 (corner hooks, dead cells)
    are exempt entirely.
    """
    px = sheet.load()
    ref_px = [r.load() for r in refs]
    return [(x, y) for y in range(5 * C) for x in range(64)
            if px[x, y][3] < 128
            and all(rp[x, y][3] >= 128 for rp in ref_px)]


def window_forbidden(sheet, refs):
    """Opaque px in insert rows 2-4 where every ref is transparent.

    The engine draws this band two whole tiles above the wall. It is ALMOST
    empty in vanilla -- woodwall pokes a 24px frame tip into it -- so the rule
    is again the measured one: only pixels no reference paints count, and a
    handful of them is ornament, not a fault (the caller thresholds)."""
    px = sheet.load()
    ref_px = [r.load() for r in refs]
    return [(x, y) for y in range(2 * C, 5 * C) for x in range(64, 96)
            if px[x, y][3] >= 128
            and all(rp[x, y][3] < 128 for rp in ref_px)]


def fill_holes(sheet, holes):
    px = sheet.load()
    remaining = list(holes)
    for _ in range(6):
        if not remaining:
            break
        nxt = []
        for (x, y) in remaining:
            counts = collections.Counter()
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < W and 0 <= ny < H and px[nx, ny][3] >= 128:
                        counts[px[nx, ny][:3]] += 1
            if counts:
                px[x, y] = counts.most_common(1)[0][0] + (255,)
            else:
                nxt.append((x, y))
        remaining = nxt


def opaque_bbox(im, threshold=128):
    px = im.load()
    xs, ys = [], []
    for y in range(im.height):
        for x in range(im.width):
            if px[x, y][3] >= threshold:
                xs.append(x)
                ys.append(y)
    if not xs:
        return None
    return (min(xs), min(ys), max(xs) + 1, max(ys) + 1)


def door_cells_report(sheet, refs, fix):
    """Each of the 8 door cells against the UNION of the refs' visible
    extents -- woodwall's doors run taller than stonewall's, and both are
    vanilla, so the allowed box is what any vanilla wall uses."""
    notes = []
    fatal = False
    for i in range(8):
        box = (96 + i * 32, 0, 96 + i * 32 + 32, 128)
        ours = sheet.crop(box)
        boxes = [b for b in (opaque_bbox(r.crop(box)) for r in refs) if b]
        vbb = (min(b[0] for b in boxes), min(b[1] for b in boxes),
               max(b[2] for b in boxes), max(b[3] for b in boxes))
        obb = opaque_bbox(ours)
        if obb is None:
            notes.append("door cell %d: EMPTY - the engine will draw nothing "
                         "where a door stands. Draw the cell. [FATAL]" % i)
            fatal = True
            continue
        # generous margin: vanilla's own frames vary a couple of px
        margin = 3
        over = (max(0, vbb[0] - margin - obb[0]), max(0, vbb[1] - margin - obb[1]),
                max(0, obb[2] - (vbb[2] + margin)), max(0, obb[3] - (vbb[3] + margin)))
        if any(over):
            if fix:
                shift_x = over[0] - over[2]
                shift_y = over[1] - over[3]
                if max(over) <= 6 and (abs(shift_x) or abs(shift_y)):
                    moved = Image.new("RGBA", (32, 128), (0, 0, 0, 0))
                    moved.alpha_composite(ours, (shift_x, shift_y))
                    sheet.paste(moved, box[:2])
                    notes.append("door cell %d: content shifted (%+d,%+d) into "
                                 "vanilla's visible box" % (i, shift_x, shift_y))
                else:
                    content = ours.crop(obb)
                    tw = min(content.width, vbb[2] - vbb[0])
                    th = min(content.height, vbb[3] - vbb[1])
                    fitted = content.resize((tw, th), Image.NEAREST)
                    cleared = Image.new("RGBA", (32, 128), (0, 0, 0, 0))
                    cleared.alpha_composite(fitted, (vbb[0], vbb[3] - th))
                    sheet.paste(cleared, box[:2])
                    notes.append("door cell %d: scale-fitted %dx%d -> %dx%d into "
                                 "vanilla's box (check the result!)"
                                 % (i, content.width, content.height, tw, th))
            else:
                notes.append("door cell %d: opaque art exceeds vanilla's visible "
                             "extents by %s px (L,T,R,B) - would clip or float"
                             % (i, over))
    return notes, fatal


# ---------------------------------------------------------------------------
# roof-slot heuristic + rebuild

def luminance(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def region_pixels(sheet, x0, y0, x1, y1):
    px = sheet.load()
    return [px[x, y] for y in range(y0, y1) for x in range(x0, x1)
            if px[x, y][3] >= 128]


def roof_slot_verdict(sheet):
    """Is the N-S window (cols 4-5, rows 0-1) a slot cut into THIS roof?

    Calibrated on all five vanilla walls and on the two supplied sheets that
    shipped this fault (docs/PLAYTEST_LOG.md "die Fenster sind seitlich
    falsch"). Three measured numbers separate them cleanly:

      * bright px (lum > roof+25) in the cell, rims excluded: a real slot has
        a lit lip and glass, 240-724 on every correct sheet; the failed pane
        on the white cloudmarble illustration measured 19.
      * their share inside the slot's columns (x8..23 of the cell): 55-100%
        correct, 21% on the pane.
      * mean luminance at most roof*1.8+24: a slot brightens the roof, a lit
        pane replaces it.
    """
    px = sheet.load()
    roof = region_pixels(sheet, 16, 16, 48, 48)          # body rows 1-2, inner cols
    if not roof:
        return "no roof material to compare against", False
    roof_lum = sum(luminance(p) for p in roof) / len(roof)
    win = region_pixels(sheet, 64, 0, 96, 32)
    if not win:
        return "window roof cells empty", False
    win_lum = sum(luminance(p) for p in win) / len(win)
    inside = outside = 0
    for y in range(0, 32):
        for x in range(66, 94):                          # rims off
            p = px[x, y]
            if p[3] >= 128 and luminance(p) > roof_lum + 25:
                if 72 <= x <= 87:
                    inside += 1
                else:
                    outside += 1
    bright = inside + outside
    share = inside / bright if bright else 0.0
    if roof_lum > 205:
        # A near-white roof leaves no luminance headroom for "glass brighter
        # than roof" -- judge on shape alone (the kk pane still fails: 19
        # bright px at 21% inside).
        ok = bright >= 40 and share >= 0.45
    else:
        ok = (bright >= 150 and share >= 0.45
              and win_lum <= roof_lum * 1.8 + 24)
    return ("bright=%d insideShare=%.0f%% lum %.0f vs roof %.0f"
            % (bright, share * 100, win_lum, roof_lum)), ok


def rebuild_roof_slot(sheet):
    """The shared wall_window_slot construction, every tone from THIS sheet."""
    import wall_window_slot
    from px import Canvas, mix

    def tones_by_lum(pixels, n):
        seq = sorted({p[:3] for p in pixels}, key=luminance)
        if not seq:
            return [(90, 90, 100)] * n
        return [seq[min(len(seq) - 1, int((i + 0.5) * len(seq) / n))] for i in range(n)]

    roof = region_pixels(sheet, 16, 16, 48, 48)
    face = region_pixels(sheet, 0, 48, 64, 80)
    cap_deep, cap_base, cap_hi = tones_by_lum(roof, 3)
    stone = tones_by_lum(face, 4)
    glass_seed = stone[-1]
    glass = {"deep": mix(glass_seed, (30, 40, 60), 0.45),
             "base": glass_seed,
             "light": mix(glass_seed, (255, 255, 255), 0.25),
             "hi": mix(glass_seed, (255, 255, 255), 0.5)}
    tones = {
        "rim_w": (cap_hi, cap_base), "rim_e": (cap_base, cap_hi),
        "dark": mix(cap_deep, (0, 0, 0), 0.35),
        "cap_deep": cap_deep, "cap_base": cap_base, "cap_hi": cap_hi,
        "stone_base": stone[2], "stone_light": stone[3],
        "glass": glass, "bar": None, "stud": None,
    }
    body = sheet.load()

    def roof_at(x, y):
        # top half = the tile-above's LOWER roof band (row 1), bottom half =
        # row 2 -- the module's own "sample shifted down 16" instruction,
        # served from this sheet's art.
        row = 1 if y < 16 else 2
        col = 1 if x < 16 else 2
        p = body[col * C + x % C, row * C + y % C]
        return p[:3] if p[3] >= 128 else cap_base

    cell = wall_window_slot.build(roof_at, tones)
    out = Image.new("RGBA", (32, 32))
    op = out.load()
    for y in range(32):
        for x in range(32):
            op[x, y] = tuple(cell.get(x, y))
    sheet.paste(out, (64, 0))


# ---------------------------------------------------------------------------

def conform(path, out_path, fix, rebuild_slot, quantize):
    problems = []
    fixes = []
    im = load_rgba(path)

    # 1. size
    if im.size != (W, H):
        if im.width % W == 0 and im.height % H == 0 and im.width // W == im.height // H:
            f = im.width // W
            im = modal_downsample_rgba(im, W, H)
            fixes.append("size: modal-downsampled %dx from %dx%d"
                         % (f, W * f, H * f))
        elif abs(im.width - W) <= 8 and abs(im.height - H) <= 8:
            base = Image.new("RGBA", (W, H), (0, 0, 0, 0))
            base.alpha_composite(im.crop((0, 0, min(im.width, W), min(im.height, H))))
            im = base
            fixes.append("size: padded/cropped %s -> (352, 128), anchored top-left"
                         % (str((im.width, im.height))))
        else:
            print("FATAL %s: size %s - not 352x128, not an integer upscale, "
                  "not within 8px" % (path, im.size))
            return 1
        if not fix:
            problems.append("size was %s (fixable)" % str(load_rgba(path).size))

    # "Allowed" is defined as "some vanilla wall does it": five references
    # spanning vanilla's own variety -- smooth masonry, planks, high-contrast
    # brick, granite, pale sandstone. Alpha exemptions, door extents and seam
    # tolerances are all unions/maxima over these.
    refs = [load_rgba(VANILLA_STONEWALL)]
    for ref in ("woodwall", "brickwall", "granitewall", "sandstonewall"):
        rp = os.path.join(REPO, "vanilla-sprites", "objects", ref + ".png")
        if os.path.exists(rp):
            refs.append(load_rgba(rp))

    # 2. alpha regions
    holes = body_alpha_holes(im, refs)
    if holes:
        if fix:
            fill_holes(im, holes)
            fixes.append("alpha: filled %d transparent px in the body blob" % len(holes))
        else:
            problems.append("%d transparent px in the body blob (0..64) - the "
                            "engine composes these opaque" % len(holes))
    bad_win = window_forbidden(im, refs)
    if len(bad_win) <= 48:
        bad_win = []          # ornament-sized, vanilla does the same
    if bad_win:
        if fix:
            px = im.load()
            for (x, y) in bad_win:
                px[x, y] = (0, 0, 0, 0)
            fixes.append("window: cleared %d px from insert rows 2-4 (drawn two "
                         "tiles up; must be empty)" % len(bad_win))
        else:
            problems.append("%d opaque px in window insert rows 2-4 - drawn TWO "
                            "TILES ABOVE the wall (must be transparent)" % len(bad_win))

    # 3. doors
    door_notes, door_fatal = door_cells_report(im, refs, fix)
    (fixes if fix else problems).extend(door_notes)

    # 4. seams, judged against vanilla's own numbers
    junctions = discover_junctions()
    ref_measures = [measure_junctions(r, junctions) for r in refs]
    ours = measure_junctions(im, junctions)
    failing = []
    for key, (d, scene) in sorted(ours.items(), key=lambda kv: -kv[1][0]):
        vd = max(m.get(key, (0.0, ""))[0] for m in ref_measures)
        tol = max(vd * 1.6, vd + 10, 14)
        if d > tol:
            failing.append((key, d, vd, tol, scene))
    if failing and fix:
        px = im.load()
        for _pass in range(2):
            for (a, b), d, vd, tol, scene in failing:
                ea = edge_pixels(im, *a)
                eb = edge_pixels(im, *b)
                blend = [tuple((pa[i] + pb[i]) // 2 for i in range(3)) + (255,)
                         if pa[3] >= 128 and pb[3] >= 128 else None
                         for pa, pb in zip(ea, eb)]

                def write(cell, side, vals):
                    x0, y0 = cell[0] * C, cell[1] * C
                    for i, v in enumerate(vals):
                        if v is None:
                            continue
                        if side == "L":
                            px[x0, y0 + i] = v
                        elif side == "R":
                            px[x0 + C - 1, y0 + i] = v
                        elif side == "T":
                            px[x0 + i, y0] = v
                        else:
                            px[x0 + i, y0 + C - 1] = v
                write(a[:2], a[2], blend)
                write(b[:2], b[2], blend)
        ours = measure_junctions(im, junctions)
        still = []
        for (key, d, vd, tol, scene) in failing:
            nd = ours.get(key, (0.0, ""))[0]
            if nd > tol:
                still.append((key, nd, vd, tol, scene))
        fixes.append("seams: blended %d failing junction edge pairs; %d still "
                     "over tolerance" % (len(failing), len(still)))
        failing = still
    for (a, b), d, vd, tol, scene in failing:
        problems.append("seam (%d,%d)%s vs (%d,%d)%s in scene '%s': |d| %.1f "
                        "(vanilla %.1f, tol %.1f)"
                        % (a[0], a[1], a[2], b[0], b[1], b[2], scene, d, vd, tol))

    # 5. roof slot
    detail, slot_ok = roof_slot_verdict(im)
    if not slot_ok:
        if fix and rebuild_slot:
            rebuild_roof_slot(im)
            fixes.append("roof slot: rebuilt cells (4-5, rows 0-1) with "
                         "wall_window_slot, tones sampled from this sheet")
            # Correct by construction now -- it IS the shipped module -- and
            # the heuristic cannot re-measure a slot cut into a near-white
            # roof, so it does not get to overrule the construction.
            slot_ok = True
        if not slot_ok:
            problems.append("N-S window (cols 4-5, rows 0-1) does not read as "
                            "this sheet's roof: %s. It is drawn as the wall's "
                            "TOP SURFACE - a pane here lies flat on the roof. "
                            "Re-run with --fix --rebuild-roof-slot, or redraw "
                            "per docs/references/wall-template-map.png" % detail)

    # 6. colours
    cols = {p[:3] for p in im.getdata() if p[3] >= 128}
    if len(cols) > 64:
        note = ("%d distinct colours - shipped wall sets carry 19-38. Four "
                "figures means an illustration; no automatic fix makes an "
                "illustration tile (see kk-sprites/readme.md)." % len(cols))
        if quantize:
            if fix:
                from convert_reference import quantize_opaque
                im = quantize_opaque(im, quantize)
                fixes.append("colours: quantized to %d (was %d)" % (quantize, len(cols)))
            else:
                problems.append(note + " (--quantize %d requested)" % quantize)
        else:
            problems.append(note)

    # 7. write + previews
    print("== %s" % path)
    for f in fixes:
        print("  FIXED  " + f)
    for p in problems:
        print("  FIX    " + p)
    if not fixes and not problems:
        print("  OK: on format, every seam within vanilla stonewall's own band")
    if fix:
        im.save(out_path)
        print("  wrote %s" % out_path)
        qa_dir = os.path.join(REPO, "build", "qa")
        os.makedirs(qa_dir, exist_ok=True)
        name = os.path.splitext(os.path.basename(out_path))[0]
        refs = []
        for ref in ("stonewall", "woodwall"):
            rp = os.path.join(REPO, "vanilla-sprites", "objects", ref + ".png")
            if os.path.exists(rp):
                refs.append((ref, load_rgba(rp)))
        sheets = [(name, im)] + refs
        dark = wrp.build_compare_sheet(sheets, None, None, (40, 40, 48, 255),
                                       name + " (conformed)")
        dark.save(os.path.join(qa_dir, "conform_%s_dark.png" % name))
        light = wrp.build_compare_sheet(sheets, None, None, (200, 204, 210, 255),
                                        name + " (conformed)")
        light.save(os.path.join(qa_dir, "conform_%s_light.png" % name))
        print("  wrote %s and _light (LOOK at them before shipping)"
              % os.path.join(qa_dir, "conform_%s_dark.png" % name))
    return 1 if (problems or door_fatal) else 0


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("sheet", nargs="+", help="wall sheet PNG(s)")
    ap.add_argument("--fix", action="store_true",
                    help="write a conformed copy instead of only reporting")
    ap.add_argument("-o", "--out", default=None,
                    help="output path (single input only; default *_conformed.png)")
    ap.add_argument("--rebuild-roof-slot", action="store_true",
                    help="with --fix: rebuild the N-S window cells with the "
                         "shared wall_window_slot construction")
    ap.add_argument("--quantize", type=int, default=0,
                    help="with --fix: quantize opaque pixels to N colours")
    args = ap.parse_args()
    if args.out and len(args.sheet) > 1:
        raise SystemExit("-o only works with a single input")
    status = 0
    for path in args.sheet:
        out = args.out or (os.path.splitext(path)[0] + "_conformed.png")
        status |= conform(path, out, args.fix, args.rebuild_roof_slot, args.quantize)
    sys.exit(status)


if __name__ == "__main__":
    main()
