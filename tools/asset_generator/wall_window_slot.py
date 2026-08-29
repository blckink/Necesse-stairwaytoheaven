"""The side-wall window: a slot cut ALONG the wall's roof, not a pane on it.

`WallWindowObject.getWindowDir` returns 1 for a NORTH-SOUTH wall — the LEFT and
RIGHT walls of a room — and then draws only cols 4-5 rows 0-1, over the band
`drawY-16 .. drawY+16`. In a north-south run that band is unbroken ROOF, so the
picture is the wall seen from directly above with an opening cut into it and
the player looking DOWN into the opening.

Measured off vanilla `stonewall`, `brickwall` and `granitewall` cols 4-5 rows
0-1, the grammar is fixed and it is not subtle:

  * the opening runs ALONG the wall — tall and narrow in the cell (10-12 px
    wide, ~28 px long), never a wide pane across it
  * a dark reveal on the NEAR faces of the cut, a LIT lip on the far one:
    light comes over the top-left, so it falls on the bottom and right insides
  * the glass sits at the BOTTOM of the cut and is BRIGHTER than the roof
    around it (stonewall's is (130,139,152) against a (34,35,35) roof)
  * no horizontal terminator at either end — the opening simply runs until the
    roof resumes. A band across the cell tiles into a stripe down the whole
    wall.

This shipped wrong three times before it was understood, each time as some
form of front-facing pane: the supplied art, then a "flat skylight" that was
still a frame with glazing bars, then a deliberately DARKENED pane. Darkening
a front-facing pane does not make it lie down. Only the slot shape does — the
frame and the glazing bars are what read as a standing window, not the value.

`gen_beetlewall` learned all of that and the other three wall sets never got
it, which is what a player reported from inside the Warden's Spire: *"die
Fenster sind seitlich falsch und nicht wie bei Käferwand gefixt."* This module
is that construction, extracted so one set cannot drift from the others again.

Usage: build the 32x32 cell and blit it at the strip's x offset, rows 0-31.

    cell = wall_window_slot.build(roof_at, tones)
    cell.blit_to(sheet, 64, 0)          # or sheet.paste(cell, 64, 0)
"""

from px import Canvas, mix

# The cut, measured off vanilla: 12 px wide, centred, 28 px long, and it stops
# two rows short of each end so the roof reads as continuing past it.
SX0, SX1 = 10, 21
SY0, SY1 = 2, 29


def build(roof_at, tones):
    """One 32x32 side-wall window cell.

    `roof_at(x, y)` returns the material's cap tone at cell coordinate (x, y).
    Sample the cap field SHIFTED DOWN 16 rows: the cell spans
    `[drawY-16, drawY+16)`, so its top half is the tile-above's LOWER roof
    band, and sampling unshifted puts a visible jump in the stone exactly where
    the window meets the roof above it.

    `tones` keys:
        rim_w, rim_e   two-tone lists for the roof's own west/east rims
        dark           the deepest tone; the sunk cut and the near reveals
        cap_deep, cap_base, cap_hi
        stone_base, stone_light    the LIT far lip and far reveal
        glass          a 4-step ramp dict (deep/base/light/hi)
        bar            2-tone list for the single saddle bar, or None
        stud           corner accent on the rim of the cut, or None
    """
    c = Canvas(32, 32)
    for y in range(32):
        for x in range(32):
            c.put(x, y, roof_at(x, y))

    rim_w, rim_e = tones["rim_w"], tones["rim_e"]
    for y in range(32):
        c.put(0, y, rim_w[0])
        c.put(1, y, rim_w[1])
        c.put(30, y, rim_e[0])
        c.put(31, y, rim_e[1])

    dark = tones["dark"]
    for y in range(SY0, SY1 + 1):                    # sink the whole cut
        for x in range(SX0, SX1 + 1):
            c.put(x, y, dark)

    # Reveals. Light comes over the top-left, so it falls on the FAR inside
    # faces — bottom and right — and leaves the near ones in shadow. Getting
    # this backwards is what turns a hole into a lid.
    for x in range(SX0, SX1 + 1):
        c.put(x, SY0, dark)                          # near lip, shaded
        c.put(x, SY0 + 1, tones["cap_deep"])
        c.put(x, SY1 - 1, tones["stone_base"])       # far lip, lit
        c.put(x, SY1, tones["stone_light"])
    for y in range(SY0, SY1 + 1):
        c.put(SX0, y, dark)                          # near (west) reveal
        c.put(SX0 + 1, y, tones["cap_base"])
        c.put(SX1 - 1, y, tones["cap_hi"])
        c.put(SX1, y, tones["stone_light"])          # far (east) reveal, lit

    stud = tones.get("stud")
    if stud is not None:                             # identity on the RIM only
        for sx in (SX0 + 1, SX1 - 1):
            for sy in (SY0 + 1, SY1 - 1):
                c.put(sx, sy, stud)

    # The glass, at the bottom of the cut, brighter than the roof around it.
    # Sheen bands run north-south, because so does the opening.
    g = tones["glass"]
    gcore = mix(g["deep"], g["base"], 0.55)
    for y in range(SY0 + 2, SY1 - 1):
        for x in range(SX0 + 2, SX1 - 1):
            edge = x in (SX0 + 2, SX0 + 3, SX1 - 2, SX1 - 3)
            if y <= SY0 + 4:                         # the near lip's shadow
                tone = g["deep"]
            elif y >= SY1 - 4:                       # light pooling far side
                tone = g["light"] if edge else g["base"]
            else:
                tone = g["light"] if edge else gcore
            if (x * 7 + y * 5) % 17 == 0:
                tone = g["hi"]
            c.put(x, y, tone)

    bar = tones.get("bar")
    if bar is not None:                              # one saddle bar, no more
        for x in range(SX0 + 2, SX1 - 1):
            c.put(x, 15, bar[0])
            c.put(x, 16, bar[1])
    return c
