#!/usr/bin/env python3
"""Draw-rectangle audit: is every opaque pixel of a sheet one the engine draws?

`sheet_format_audit.py` holds a few sheets to vanilla's exact row bands and
`rotation_variety_audit.py` proves cells are not duplicates. Neither answers
the player's "abgeschnitten" -- art that sits where the renderer never reads,
or that runs across the edge of the sub-rectangle it IS read through, so the
game shows a piece with a hard cut through it.

This tool knows, per vanilla base class, exactly which sub-rectangles of the
sheet the renderer reads for each rotation / state and at which screen offset.
All reads below were copied out of the decompiled 1.3.2 sources
(`./gradlew decompileToSources`), class by class:

  PaintingObject      32x128. rot 0 (wall BELOW) row 2 at drawY+8; rot 1 (wall
                      LEFT) row 3 at drawY-16; rot 2 (wall ABOVE) row 0 at
                      drawY-32; rot 3 (wall RIGHT) row 1 at drawY-16.
                      Row 0 is therefore the face-on view, row 2 the back.
  LargePaintingObject 64x256, two objects (<id>, <id>2). rot 0: (0,128)+(32,128)
                      32x64 at -16; rot 1: col 0 rows 192..255 (two 32 cells,
                      stacked) at -16; rot 2: 64x64 at y0 at -64 (halves read
                      mirrored, since the multi-tile flips); rot 3: col 1 rows
                      64..127 at -16.
  WallTorchObject     64x128, sprite(active?0:1, s, 32). s 0 = wall above at
                      drawY-32, 1 = wall right at -16, 2 = wall below at 0,
                      3 = wall left at -16 (getSprite).
  StreetlampObject    32x192, sprite(0, active?0:1, 32, 96) at drawY-64.
  rotation columns    sprite(rotation % 4, 0, 32, h). Chair, Desk, Dresser,
                      Clock, Lamp/Candelabra, Sarcophagus, CraftingStation at
                      drawY-h+32; Bookshelf, Cabinet at drawY-h+64; DisplayStand
                      at drawY-(h-32). LampObject ALSO loads <id>_off through
                      GameTexture.fromFile, which falls back to the ERR texture
                      -- so a candelabra without _off shows ERR when switched
                      off.
  Bed / DinnerTable   128x128, two objects. rot 0 col 3 rows 32..127, rot 1
                      x0..63 y64..127, rot 2 col 2 rows 32..127, rot 3 x0..63
                      y0..63. x64..127 y0..31 is never read.
  Bench               same, but rot 0 reads col 2 and rot 2 reads col 3.
  StatueObject        spriteCount columns of width/spriteCount, whole height.
  TableDecoration     width/32 columns of 32, whole height.
  SkyDecoObject (ours) variantWidth columns, whole height.

Checks, per sheet:
  size      the renderer's fixed size, where it has one
  unread    opaque pixels outside every rectangle the engine reads: art the
            player never sees
  empty     a rotation/state whose reads are all transparent: it vanishes
  cut       a read rectangle whose edge pixel is opaque AND the sheet pixel just
            across that edge is opaque too, while in that rotation's composite
            the screen neighbour is NOT that pixel -- the picture continues in
            the file and is cut on screen. Runs of 3+ pixels are flagged. Two
            views that each end in their own dark contour and merely touch
            (a full-width bookcase back beside its side view) are not a cut.
  side      wall pieces: the wall-left view must sit left of the wall-right
            view (centroid), or the piece hangs off the wrong edge.

Usage:  python3 tools/draw_rect_audit.py [-v]
Exit 1 on any FIX.
"""
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")


# A read = (sx0, sy0, sx1, sy1, screen_x, screen_y) relative to the tile's
# top-left. A view = (label, [reads]).

def painting():
    return [("rot0 wall below", [(0, 64, 32, 96, 0, 8)]),
            ("rot1 wall left", [(0, 96, 32, 128, 0, -16)]),
            ("rot2 wall above", [(0, 0, 32, 32, 0, -32)]),
            ("rot3 wall right", [(0, 32, 32, 64, 0, -16)])]


def large_painting():
    return [("rot0 wall below", [(0, 128, 32, 192, 0, -16), (32, 128, 64, 192, 32, -16)]),
            ("rot1 wall left", [(0, 192, 32, 224, 0, -16), (0, 224, 32, 256, 0, 16)]),
            ("rot2 wall above", [(32, 0, 64, 64, 0, -64), (0, 0, 32, 64, -32, -64)]),
            ("rot3 wall right", [(32, 96, 64, 128, 0, -16), (32, 64, 64, 96, 0, -48)])]


def wall_torch():
    dy = {0: -32, 1: -16, 2: 0, 3: -16}
    names = {0: "wall above", 1: "wall right", 2: "wall below", 3: "wall left"}
    views = []
    for col, state in ((0, "lit"), (1, "unlit")):
        for s in range(4):
            views.append(("%s %s" % (state, names[s]),
                          [(col * 32, s * 32, col * 32 + 32, s * 32 + 32, 0, dy[s])]))
    return views


def streetlamp():
    return [("lit", [(0, 0, 32, 96, 0, -64)]), ("unlit", [(0, 96, 32, 192, 0, -64)])]


def columns(h, dy, n=4, w=32, dx=0):
    return [("rot%d" % r, [(r * w, 0, r * w + w, h, dx, dy)]) for r in range(n)]


def bed_like(bench=False):
    c0, c2 = (64, 96) if bench else (96, 64)
    return [("rot0", [(c0, 32, c0 + 32, 128, 0, -64)]),
            ("rot1", [(0, 64, 32, 128, 0, -32), (32, 64, 64, 128, 32, -32)]),
            ("rot2", [(c2, 32, c2 + 32, 128, 0, -32)]),
            ("rot3", [(32, 0, 64, 64, 0, -32), (0, 0, 32, 64, -32, -32)])]


# file -> (class label, expected size or None, views, extra sheets required)
def spec():
    s = {}
    for rel in ("skywatchbanner.png", "paintings/salonmirror.png",
                "paintings/eyepainting.png", "paintings/shrunkenheadtrophy.png"):
        s[rel] = ("PaintingObject", (32, 128), painting(), ())
    for rel in ("paintings/walleye.png", "paintings/hauntedwallclock.png",
                "paintings/magicmirror.png"):
        s[rel] = ("LargePaintingObject", (64, 256), large_painting(), ())
    for rel in ("mistglasslantern.png", "flickerlightgarland.png", "salonsign.png"):
        s[rel] = ("WallTorchObject", (64, 128), wall_torch(), ())
    for rel in ("wardencandelabra.png", "ghostlantern.png"):
        s[rel] = ("StreetlampObject", (32, 192), streetlamp(), ())
    for rel, cls in (("skywatchchair.png", "ChairObject"), ("electricchair.png", "ChairObject"),
                     ("salonchair.png", "ChairObject"), ("skywatchdesk.png", "DeskObject"),
                     ("skywatchdresser.png", "DresserObject"), ("skywatchclock.png", "ClockObject"),
                     ("hauntedclock.png", "ClockObject"), ("windsilkloom.png", "CraftingStationObject"),
                     ("stormglasskiln.png", "(ours) rotation columns"),
                     ("stormglasskiln_on.png", "(ours) rotation columns")):
        s[rel] = (cls, (128, 64), columns(64, -32), ())
    s["twilightsarcophagus.png"] = ("SarcophagusObject", (128, 96), columns(96, -64), ())
    for rel in ("skywatchcandelabra.png", "skullcandelabra.png"):
        s[rel] = ("CandelabraObject", (128, 64), columns(64, -32), ("_off",))
        s[rel[:-4] + "_off.png"] = ("CandelabraObject off", (128, 64), columns(64, -32), ())
    for rel, cls in (("skywatchbookshelf.png", "BookshelfObject"),
                     ("skywatchcabinet.png", "CabinetObject")):
        s[rel] = (cls, (128, 128), columns(128, -64), ())
    s["skywatchdisplay.png"] = ("DisplayStandObject", (128, 32), columns(32, 0), ())
    s["aetherforge.png"] = ("(ours) forge body + fire strip", (128, 96),
                            columns(64, -32) + [("fire%d" % f, [(f * 32, 64, f * 32 + 32, 96, 0, 0)])
                                                for f in range(4)], ())
    s["skywatchbed.png"] = ("BedObject", (128, 128), bed_like(), ("_mask",))
    s["coffinbed.png"] = ("BedObject", (128, 128), bed_like(), ("_mask",))
    s["skywatchdinnertable.png"] = ("DinnerTableObject", (128, 128), bed_like(), ())
    s["skywatchbench.png"] = ("BenchObject", (128, 128), bed_like(bench=True), ())
    for rel in ("skywatchcandle.png", "skywatchchalice.png", "skywatchtome.png",
                "pottedcloudberry.png", "thingbox.png"):
        s[rel] = ("TableDecorationObject", (32, 32), columns(32, 0, n=1), ())
    for rel in ("saloncashregister.png", "salonproducts.png"):
        s[rel] = ("TableDecorationObject", (128, 32), columns(32, 0), ())
    s["statues/gloomraven.png"] = ("StatueObject(16, 1)", None, columns(96, -64, n=1, w=64, dx=-16), ())
    s["statues/seraph.png"] = ("StatueObject(32, 1)", None, columns(192, -160, n=1, w=96, dx=-32), ())
    s["catbasket.png"] = ("CatBasketObject (whole texture)", None, columns(32, 0, n=1), ())
    return s


def alpha_grid(im):
    px = im.load()
    return [[px[x, y][3] > 0 for x in range(im.width)] for y in range(im.height)]


def is_contour(p):
    """A dark contour pixel. Two views that each close with their own contour
    may touch across a cell edge without either being cut; a picture that
    continues across the edge has body colour on at least one side."""
    return max(p[:3]) < 72


def check(rel, cls, size, views, extras, verbose):
    fixes, notes = [], []
    path = os.path.join(RES, "objects", rel)
    if not os.path.exists(path):
        return fixes, notes, False
    im = Image.open(path).convert("RGBA")
    if size and im.size != size:
        fixes.append("%s: %s, %s reads a %s sheet" % (rel, im.size, cls, size))
        return fixes, notes, True
    for suffix in extras:
        extra = os.path.join(RES, "objects", rel[:-4] + suffix + ".png")
        if not os.path.exists(extra):
            fixes.append("%s: no %s%s.png -- %s loads it with GameTexture.fromFile, "
                         "which falls back to the ERR texture" % (rel, rel[:-4], suffix, cls))
    A = alpha_grid(im)
    PX = im.load()
    W, H = im.size

    # unread pixels
    read = [[False] * W for _ in range(H)]
    for _, reads in views:
        for sx0, sy0, sx1, sy1, _, _ in reads:
            for y in range(sy0, min(sy1, H)):
                for x in range(sx0, min(sx1, W)):
                    read[y][x] = True
    lost = [(x, y) for y in range(H) for x in range(W) if A[y][x] and not read[y][x]]
    if lost:
        xs = [p[0] for p in lost]
        ys = [p[1] for p in lost]
        fixes.append("%s: %d opaque px the engine never reads (x%d..%d y%d..%d)"
                     % (rel, len(lost), min(xs), max(xs), min(ys), max(ys)))

    for label, reads in views:
        # empty
        if not any(A[y][x] for sx0, sy0, sx1, sy1, _, _ in reads
                   for y in range(sy0, sy1) for x in range(sx0, sx1)):
            fixes.append("%s %s: every read is transparent -- this %s renders nothing"
                         % (rel, label, "state" if "lit" in label else "rotation"))
            continue
        # composite map: screen pixel -> sheet pixel
        screen = {}
        for sx0, sy0, sx1, sy1, dx, dy in reads:
            for y in range(sy0, sy1):
                for x in range(sx0, sx1):
                    screen[(dx + x - sx0, dy + y - sy0)] = (x, y)
        # cut edges
        for sx0, sy0, sx1, sy1, dx, dy in reads:
            edges = (("left", [(sx0, y) for y in range(sy0, sy1)], (-1, 0)),
                     ("right", [(sx1 - 1, y) for y in range(sy0, sy1)], (1, 0)),
                     ("top", [(x, sy0) for x in range(sx0, sx1)], (0, -1)),
                     ("bottom", [(x, sy1 - 1) for x in range(sx0, sx1)], (0, 1)))
            for name, pts, (ex, ey) in edges:
                run = best = 0
                where = None
                for (x, y) in pts:
                    nx, ny = x + ex, y + ey
                    cut = False
                    if (A[y][x] and 0 <= nx < W and 0 <= ny < H and A[ny][nx]
                            and not (is_contour(PX[x, y]) and is_contour(PX[nx, ny]))):
                        scr = (dx + x - sx0 + ex, dy + y - sy0 + ey)
                        cut = screen.get(scr) != (nx, ny)
                    run = run + 1 if cut else 0
                    if run > best:
                        best, where = run, (x, y)
                if best >= 3:
                    fixes.append("%s %s: art runs across the %s edge of the read "
                                 "rect (%d,%d)-(%d,%d) for %d px near sheet (%d,%d) "
                                 "-- cut off in game"
                                 % (rel, label, name, sx0, sy0, sx1, sy1, best, *where))
    # PaintingObject: rot 2 (the wall ABOVE, the usual north wall) reads ROW 0,
    # drawn onto the wall face at drawY-32 -- that row is the face-on view the
    # player looks at. Row 2 is read for a wall BELOW (the piece's back, seen
    # over the wall's cap, drawn at +8 so its lower 8px sink into the wall).
    # A sheet with its foreshortened view in row 0 shows the stub on every
    # normal wall: the Skywatch Banner shipped exactly that way.
    if cls == "PaintingObject":
        def mass(r):
            return sum(A[y][x] for y in range(r * 32, r * 32 + 32) for x in range(32))
        front, back = mass(0), mass(2)
        notes.append("%s face-on row 0: %d px, back row 2: %d px" % (rel, front, back))
        if front < 0.9 * back:
            fixes.append("%s: row 0 (wall above, the face-on view every north wall "
                         "shows) has %d opaque px, row 2 (wall below, the back) has "
                         "%d -- the two views are swapped" % (rel, front, back))
        low = [y for y in range(88, 96) for x in range(32) if A[y][x]]
        if low:
            notes.append("%s back row 2 uses its bottom 8 rows (%d px) -- the "
                         "engine draws it at +8, so those sit on the wall below"
                         % (rel, len(low)))
    # wall side: centroid of the left-wall view must be left of the right-wall one
    if cls in ("PaintingObject", "WallTorchObject", "LargePaintingObject"):
        cen = {}
        for label, reads in views:
            if "unlit" in label:
                continue
            xs = [x - sx0 for sx0, sy0, sx1, sy1, _, _ in reads
                  for y in range(sy0, sy1) for x in range(sx0, sx1) if A[y][x]]
            if xs:
                cen["left" if "wall left" in label else
                    "right" if "wall right" in label else label] = sum(xs) / len(xs)
        if "left" in cen and "right" in cen:
            notes.append("%s side centroids: wall-left view x%.1f, wall-right view x%.1f"
                         % (rel, cen["left"], cen["right"]))
            if cen["left"] > cen["right"] + 1:
                fixes.append("%s: the wall-LEFT view sits right of the wall-RIGHT view "
                             "(x%.1f vs x%.1f) -- it hangs off the wrong edge; swap the "
                             "two side rows" % (rel, cen["left"], cen["right"]))
    return fixes, notes, True


def main():
    verbose = "-v" in sys.argv
    fixes, notes, seen = [], [], 0
    for rel, (cls, size, views, extras) in sorted(spec().items()):
        f, n, ok = check(rel, cls, size, views, extras, verbose)
        if not ok:
            notes.append("%s: not shipped, skipped" % rel)
            continue
        seen += 1
        fixes += f
        notes += n
    if verbose:
        for n in notes:
            print("     " + n)
    for f in fixes:
        print("FIX  " + f)
    if fixes:
        print("\n%d problem(s) in %d sheets." % (len(fixes), seen))
        return 1
    print("OK: %d sheets -- every opaque pixel is inside a rectangle the engine "
          "reads, no rotation is empty, no art crosses a read edge, wall pieces "
          "hang on the side they name." % seen)
    return 0


if __name__ == "__main__":
    sys.exit(main())
