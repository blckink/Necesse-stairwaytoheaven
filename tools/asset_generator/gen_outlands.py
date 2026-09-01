"""Bestiary icons for the Beetle Outlands' three ascended mobs.

The BODIES of these three are the player's own hand-drawn sheets
(`mobs/crookedgolem.png`, `mobs/rarecrookedgolem.png`,
`mobs/crookedarmadillo.png`); they are listed in `generate_assets.CONVERTED`
and this module must never write them. What it writes is the one piece the
sheets cannot supply: `MobRegistry.MobRegistryElement.loadIcon` is hard-wired
to `mobs/icons/<our stringID>.png` with no setter, so a mob without that file
shows the engine's red ERR tile in the bestiary.

The icons are portraits of the sheets, not crops of them: a 64 px walking frame
squeezed into a 32 px cell loses the read, which is exactly why vanilla draws
its own bestiary icons too (`mobs/icons/crystalgolem.png` carries 360 opaque px
against 1232 in the body cell). Colours come from `palette.CROOKED_*`, which
are sampled from those same sheets, so the bestiary entry and the animal are
the same animal.

Two size laws are obeyed here rather than guessed at (`tools/size_audit.py`
enforces them): the golems answer to `mobs/icons/crystalgolem.png` at 360
opaque px, and the armadillo to `mobs/icons/crystalarmadillo.png`, the widest
icon in vanilla's bestiary at 664 px across a 32x28 box. That is why the
armadillo's dome runs nearly wall to wall.

Everything thin is drawn as a MASS at least four pixels across, because
`Canvas.outline` overwrites every filled pixel that touches empty space: a
two-pixel limb comes out of that pass as solid outline with no core left.
"""

from px import Canvas
import palette


def _icon():
    return Canvas(32, 32)


def _sym(c, x, y, color):
    """Plot a pixel and its mirror about the canvas centre.

    The axis is 15.5 — the true middle of a 0..31 grid — so `31 - x`, the same
    convention `gen_cloudmarble` and `gen_beetlewall` mirror on. Every centred
    shape below is therefore built on 15.5 as well; a shape centred on integer
    column 16 would sit half a pixel right of the cell it is drawn in.
    """
    c.put(x, y, color)
    c.put(31 - x, y, color)


def _horn(c, points, pal, base_key, band_key):
    """A candy-cane horn: a four-wide bone shaft banded every third step.

    Four wide is the minimum that survives the outline pass with a readable
    two-pixel core, and the band is laid inside that core so the stripe is
    still there afterwards. Mirrored as it goes, so both horns match.
    """
    for i, (x, y) in enumerate(points):
        band = (i % 3) == 0
        for k in range(4):
            edge = k == 0 or k == 3
            color = pal[band_key] if (band and not edge) else pal[base_key]
            c.put(x + k, y, color)
            c.put(31 - x - k, y, color)


def _eye_golem(c, pal, cx, cy):
    """The shared body of both golems: a ringed floating eye.

    Concentric, exactly as the sheet builds it — outer halo, dark gap, bright
    iris ring, dark gap, a lit core. Every ring is at least two pixels wide so
    the outline pass eats the rim and leaves the rings.
    """
    c.ellipse(cx, cy, 9.4, 9.4, pal["ring_d"])
    c.ellipse(cx, cy, 8.4, 8.4, pal["ring"])
    c.ellipse(cx, cy, 7.2, 7.2, pal["ring_l"])
    c.ellipse(cx - 2, cy - 3, 4.2, 3.8, pal["ring_h"])
    c.ellipse(cx, cy, 5.6, 5.8, pal["dark"])
    c.ellipse(cx, cy, 4.6, 4.8, pal["iris"])
    c.ellipse(cx - 1, cy - 1, 3.4, 3.4, pal["iris_l"])
    c.ellipse(cx, cy, 2.6, 2.8, pal["dark"])
    c.ellipse(cx, cy, 1.6, 1.8, pal["iris_h"])


# The two hooks, as pixel paths rather than trigonometry: an icon is 32 px and
# a hand-placed curve reads better at 1x than a sampled arc. Left half only —
# _horn mirrors each step, and each step is four pixels wide. The path ENDS on
# the eye's upper rim, and the horns are drawn after the eye, so they read as
# growing out of it rather than floating above it.
_HORN = [(8, 9), (7, 8), (6, 7), (5, 6), (4, 5), (4, 4), (4, 3),
         (5, 2), (6, 1), (7, 1), (8, 2)]

# The skirt of drooping tendrils under the eye. The side lobe is mirrored; the
# middle one straddles the 15.5 axis and is drawn once, so its widths are even.
# Four to six wide, so a two-to-four-wide core survives the outline pass.
_SKIRT_SIDE = ((8, 23, 5), (8, 24, 5), (9, 25, 5), (9, 26, 4), (10, 27, 3))
_SKIRT_MID = ((13, 24, 6), (13, 25, 6), (13, 26, 6), (14, 27, 4), (14, 28, 4))


def _golem_body(c, pal, base_key, band_key, tendril_key, tendril_dark_key,
                claw_key):
    for x, y, w in _SKIRT_SIDE:
        for k in range(w):
            _sym(c, x + k, y, pal[tendril_key] if y < 26 else pal[tendril_dark_key])
    for x, y, w in _SKIRT_MID:
        for k in range(w):
            c.put(x + k, y, pal[tendril_key] if y < 27 else pal[tendril_dark_key])
    for side in (4, 27):                        # the sickle claws at the flanks
        c.ellipse(side, 19, 2.4, 3.4, pal[claw_key])
    for k in range(2):                          # tapered points, top and bottom
        _sym(c, 4 + k, 15, pal[claw_key])
        _sym(c, 4 + k, 23, pal[claw_key])
    _eye_golem(c, pal, 15.5, 15)
    _horn(c, _HORN, pal, base_key, band_key)


def gen_crookedgolem_icon(path):
    """Crooked Golem: violet halo, lime eye, bone-white banded horns."""
    c = _icon()
    g = palette.CROOKED_GOLEM
    _golem_body(c, g, "bone_h", "band", "ring", "ring_d", "iris")
    c.outline(palette.OUTLINE)
    # Face and sparks go in AFTER the outline pass or they get overwritten.
    c.put(12, 11, g["spark"])
    _sym(c, 4, 18, g["iris_h"])
    _sym(c, 6, 2, g["bone_h"])
    c.save(path)


def gen_rarecrookedgolem_icon(path):
    """Rare Crooked Golem: the same body plan in crimson, teal and magenta.

    Deliberately the SAME silhouette as its common sibling and a different
    palette — that is exactly the relationship the two sheets have, and a
    bestiary list where the rare one reads as a recolour of the common one is
    telling the player the truth.
    """
    c = _icon()
    r = palette.CROOKED_RARE
    _golem_body(c, r, "bone_h", "flesh", "flesh_l", "flesh_d", "flesh")
    c.outline(palette.OUTLINE)
    c.put(12, 11, r["spark"])
    _sym(c, 4, 18, r["bone"])
    _sym(c, 6, 2, r["bone_h"])
    c.save(path)


def gen_crookedarmadillo_icon(path):
    """Crooked Armadillo: a bone-white plate dome over a navy hull.

    Answers to `mobs/icons/crystalarmadillo.png`, the widest icon in vanilla's
    bestiary — 664 opaque px in a 32x28 box — so this one has to run nearly
    wall to wall or it reads as a smaller animal than the one that just rolled
    over the player.
    """
    c = _icon()
    s = palette.CROOKED_SHELL
    # Hull and legs first, so the shell overlaps them the way the sheet does.
    c.ellipse(15.5, 19, 12.4, 7.0, s["hull_d"])
    c.ellipse(15.5, 18, 11.0, 5.8, s["hull"])
    c.ellipse(15.5, 17, 8.0, 4.0, s["hull_l"])
    for x, y, w in ((4, 25, 6), (4, 26, 6), (5, 27, 5), (5, 28, 5), (6, 29, 4),
                    (11, 26, 5), (11, 27, 5), (12, 28, 4), (12, 29, 4)):
        for k in range(w):
            _sym(c, x + k, y, s["hull_d"])      # stubby front and rear legs
    c.ellipse(15.5, 13, 13.6, 9.0, s["rim"])    # plate dome, rim tone first
    c.ellipse(15.5, 13, 12.4, 8.0, s["shell_d"])
    c.ellipse(15.5, 12, 11.4, 7.0, s["shell_l"])
    c.ellipse(11.5, 9, 6.4, 4.0, s["shell_h"])  # top-left light
    # Three plate seams, two pixels thick — the shell's whole read at 1x.
    # Written as (row, half-width, inner gap) around the 15.5 axis: _sym(15 - d)
    # paints the pair (15 - d, 16 + d), so a half-width of h is 2h + 2 pixels
    # across. The third seam carries a gap because the eye socket drawn just
    # below covers x 12..20 there; without it that seam would be laid down and
    # then immediately painted over, and the icon would ship with two.
    for i, (sy, half, gap) in enumerate(((6, 4, 0), (11, 6, 0), (16, 8, 5))):
        for d in range(gap, half + 1):
            bend = d // 4
            for k in range(2):
                _sym(c, 15 - d, sy + bend + k, s["rim"] if i else s["rim_l"])
    c.ellipse(15.5, 19, 5.4, 4.0, s["hull_d"])  # eye socket sunk into the hull
    c.outline(palette.OUTLINE)
    # Eye and vents after the outline pass.
    c.ellipse(15.5, 19, 3.8, 3.0, s["eye_d"])
    c.ellipse(15.5, 19, 2.8, 2.2, s["eye"])
    c.ellipse(15.5, 19, 1.8, 1.4, s["eye_l"])
    c.put(14, 18, s["eye_h"])
    for x, y in ((7, 19), (8, 20), (7, 20), (8, 19)):
        _sym(c, x, y, s["eye"])                 # flank vents
    _sym(c, 7, 19, s["eye_l"])
    _sym(c, 9, 6, s["glow"])                    # cold rim light on the plates
    _sym(c, 15, 4, s["shell_h"])
    c.save(path)


def generate(out):
    icons = f"{out}/mobs/icons"
    gen_crookedgolem_icon(f"{icons}/crookedgolem.png")
    gen_rarecrookedgolem_icon(f"{icons}/rarecrookedgolem.png")
    gen_crookedarmadillo_icon(f"{icons}/crookedarmadillo.png")
