#!/usr/bin/env python3
"""Size audit: measure every mod sprite against its closest vanilla analogue.

Playtest rule this enforces: an asset that reads smaller/thinner than its
vanilla counterpart feels wrong in game (the warden and the seance circle
both shipped undersized before this audit existed). For each mapped pair the
script measures the opaque-pixel bounding box and fill of ONE representative
cell and prints the ratio ours/vanilla; anything under the threshold is
flagged FIX.

Usage:  python3 tools/size_audit.py [--vanilla /path/to/sprite/dump]
The vanilla dump is never committed; default path matches the dev container.
"""
import argparse
import os
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")

# (mod path, mod cell (x, y, w, h) or None=whole, vanilla path, vanilla cell,
#  note). Cells pick a representative variant/frame of each sheet.
PAIRS = [
    ("objects/seancecircle.png", None,
     "objects/fallenaltar.png", ("auto", 32, 64), "ritual set piece (per-tile column)"),
    ("objects/veilriftdown.png", None,
     "objects/ladderdown.png", None, "descending portal"),
    ("objects/skystairwaydown.png", None,
     "objects/ladderdown.png", None, "descending portal"),
    ("objects/statues/gloomraven.png", None,
     "objects/statues/angelicstatue.png", ("auto", 64, 64), "statue (densest 64px cell)"),
    ("objects/wardencandelabra.png", (0, 0, 32, 96),
     "objects/copperstreetlamp.png", (0, 0, 32, 96), "streetlamp (on half)"),
    ("objects/mistglasslantern.png", (0, 0, 32, 32),
     "objects/walltorch.png", ("auto", 32, 32), "wall light (densest cell)"),
    # Ours was a FIXED (0,0,32,32) crop while the vanilla side is "auto"
    # (densest cell) and the note claimed densest for both. That asymmetry was
    # invisible for as long as our banner drew the SAME picture in all four
    # rotation rows -- 738 px four times over, which is the rotation-variety bug
    # itself. The moment the sheet got four real views, the fixed crop started
    # measuring row 0, the foreshortened over-the-cap view, against vanilla's
    # face-on row, and reported 0.55 on art that had just been corrected.
    # Compare densest to densest, as the note always said.
    ("objects/skywatchbanner.png", ("auto", 32, 32),
     "objects/bannerofpeace.png", ("auto", 32, 32), "wall banner (densest 32px cell)"),
    ("objects/gloomshroom.png", (0, 0, 32, 32),
     "objects/mushroom.png", ("auto", 32, 32), "mushroom (densest cell)"),
    ("objects/skystonerock.png", (0, 0, 32, 64),
     "objects/caverock.png", (0, 0, 32, 64), "rock node (full column)"),
    ("objects/stormcrystal.png", (0, 0, 32, 64),
     "objects/crystalwall.png", (0, 0, 32, 48), "crystal cluster (half of 2x1)"),
    ("objects/windwheat.png", (0, 0, 32, 32),
     "objects/swampgrass.png", (0, 0, 32, 32), "grass clump"),
    ("objects/skyreeds.png", (0, 0, 32, 32),
     "objects/deepswamptallgrass.png", (0, 0, 32, 32), "tall grass"),
    ("objects/gloomwillow.png", (0, 0, 64, 112),
     "objects/deadwood.png", ("auto", 32, 112), "dead tree deco (per-tile column)"),
    ("objects/wardenbeaconoff.png", None,
     "objects/bannerstand.png", None, "tall quest prop"),
    ("mobs/skywarden.png", (0, 0, 64, 64),
     None, None, "humanoid: compare by hand vs player (28px head)"),
    # v0.5 art pass: the assets the last playtest called out as too thin.
    # Ore overlays are masked onto the rock, so compare overlay to overlay;
    # mob sheets compare their densest 64px frame against a vanilla quadruped
    # of the same footprint.
    ("objects/aetheriumore.png", ("auto", 32, 32),
     "objects/ironore.png", ("auto", 32, 32), "ore overlay (densest 32px cell)"),
    ("mobs/mistserpent.png", ("auto", 64, 64),
     "mobs/sandworm.png", ("auto", 64, 64), "worm mob (densest 64px frame)"),
    ("mobs/galehound.png", ("auto", 64, 64),
     "mobs/boar.png", ("auto", 64, 64), "quadruped mob (densest 64px frame)"),
    ("mobs/skystonegolem.png", ("auto", 64, 64),
     "mobs/boar.png", ("auto", 64, 64), "heavy mob (densest 64px frame)"),
    # v0.4 saplings (chunky mini-trees like vanilla, not thin shoots)
    ("objects/nimbussapling.png", None,
     "objects/willowsapling.png", None, "sapling"),
    ("objects/fulgursapling.png", None,
     "objects/pinesapling.png", None, "sapling"),
    ("objects/prismasapling.png", None,
     "objects/birchsapling.png", None, "sapling"),
    # v0.7 stone barrens. Lichen and cragbloom are ground flora, so they answer
    # to a vanilla grass clump. Scree does NOT: it is loose broken stuff lying
    # on the ground, and vanilla's own analogue for that is the debris family
    # (cratesdebris' densest 32px cell carries 156 opaque px) -- measuring a
    # rubble heap against a tall grass clump would be asking it to be a
    # different kind of object.
    ("objects/skylichen.png", ("auto", 32, 32),
     "objects/swampgrass.png", ("auto", 32, 32), "stone crust (densest cell)"),
    ("objects/cragbloom.png", ("auto", 32, 32),
     "objects/swampgrass.png", ("auto", 32, 32), "cushion flower (densest cell)"),
    ("objects/skyscree.png", ("auto", 32, 32),
     "objects/cratesdebris.png", ("auto", 32, 32), "ground debris (densest cell)"),
    # v0.7 fence: cell by cell against the vanilla fence the engine draws the
    # same way. col 0 is the post, which is the cell that carries the sprite.
    ("objects/skyironfence.png", (0, 0, 32, 64),
     "objects/ironfence.png", (0, 0, 32, 64), "fence post cell (col 0)"),
    ("objects/skyironfencegate.png", (32, 0, 64, 64),
     "objects/ironfencegate.png", (32, 0, 64, 64), "fence gate closed cell (col 1)"),
    # v0.8 Skywatch furniture. A stone chair answers to a stone throne, not to
    # oakchair; the 128x128 pieces compare region-for-region because their
    # sheets are four VIEWS (two 64x64 blocks + two 32x96 strips), not columns.
    ("objects/skywatchchair.png", (64, 0, 32, 64),
     "objects/dungeonchair.png", (64, 0, 32, 64), "chair (front rotation column)"),
    ("objects/skywatchbench.png", (0, 64, 64, 64),
     "objects/oakbench.png", (0, 64, 64, 64), "bench (front 64x64 block)"),
    ("objects/skywatchbench.png", (64, 32, 32, 96),
     "objects/oakbench.png", (64, 32, 32, 96), "bench (side 32x96 strip)"),
    ("objects/skywatchmodulartable.png", None,
     "objects/oakmodulartable.png", None, "modular table (whole 96x64 atlas)"),
    ("objects/skywatchdinnertable.png", (0, 0, 64, 64),
     "objects/oakdinnertable.png", (0, 0, 64, 64), "dinner table (horizontal block)"),
    ("objects/skywatchdinnertable.png", (64, 32, 32, 96),
     "objects/oakdinnertable.png", (64, 32, 32, 96), "dinner table (vertical strip)"),
    ("objects/skywatchdesk.png", (64, 0, 32, 64),
     "objects/oakdesk.png", (64, 0, 32, 64), "desk (front rotation column)"),
    ("objects/skywatchdresser.png", (64, 0, 32, 64),
     "objects/oakdresser.png", (64, 0, 32, 64), "dresser (front rotation column)"),
    ("objects/skywatchbed.png", (0, 0, 64, 64),
     "objects/oakbed.png", (0, 0, 64, 64), "bed (horizontal block)"),
    ("objects/skywatchbed.png", (64, 32, 32, 96),
     "objects/oakbed.png", (64, 32, 32, 96), "bed (vertical strip)"),
    ("objects/skywatchcandelabra.png", (64, 0, 32, 64),
     "objects/oakcandelabra.png", (64, 0, 32, 64), "candelabra (front rotation column)"),
    ("objects/skywatchchalice.png", None,
     "objects/goldchalice.png", None, "table decoration (cup)"),
    ("objects/pottedcloudberry.png", None,
     "objects/decorativepot1.png", None, "table decoration (potted plant)"),
    # v0.8 Seraph statue, converted from reference art. Vanilla's own tall
    # single-figure statue is the blacksmith at 96x160.
    ("objects/statues/seraph.png", None,
     "objects/statues/blacksmithstatue.png", None, "tall single-figure statue"),
    # v0.8 Cloudmarble. Stone fence and gate are the analogues, not the iron
    # ones: this is masonry, and iron rails are far thinner per cell.
    ("objects/cloudmarblefence.png", (0, 0, 32, 64),
     "objects/stonefence.png", (0, 0, 32, 64), "fence post cell (col 0)"),
    ("objects/cloudmarblefencegate.png", (32, 0, 64, 64),
     "objects/stonefencegate.png", (32, 0, 64, 64), "fence gate closed cell (col 1)"),
    ("items/cloudmarblewall.png", None, "items/stonewall.png", None, "wall item icon"),
    ("items/cloudmarblewindow.png", None, "items/stonewindow.png", None, "window item icon"),
    ("items/cloudmarblefence.png", None, "items/stonefence.png", None, "fence item icon"),
    ("items/cloudmarblefencegate.png", None, "items/stonefencegate.png", None, "gate item icon"),
    # Beetlefreak: supplied wall art, icons cut from the sheet it draws from.
    ("items/beetlewall.png", None, "items/stonewall.png", None, "wall item icon"),
    ("items/beetledoor.png", None, "items/stonedoor.png", None, "door item icon"),
    ("items/beetlewindow.png", None, "items/stonewindow.png", None, "window item icon"),
    # v0.9 Sky Seraph tree companions. The tree itself is converted reference
    # art with no vanilla analogue; its companions answer to oak, the vanilla
    # round-crown broadleaf. Vanilla names the log item oaklog, not oakwood.
    ("objects/skyseraphsapling.png", None,
     "objects/oaksapling.png", None, "sapling"),
    ("items/skyseraphsapling.png", None,
     "items/oaksapling.png", None, "sapling item icon"),
    ("items/seraphwood.png", None,
     "items/oaklog.png", None, "log item icon"),
    ("particles/seraphleaves.png", ("auto", 20, 20),
     "particles/oakleaves.png", ("auto", 20, 20), "leaf particle (densest 20px frame)"),
    # Cloud Tree companions. The Cloud Tree is supplied art drawn on the BIRCH
    # sheet, so birch is the analogue, not oak. Vanilla names the log birchlog.
    ("objects/cloudsapling.png", None,
     "objects/birchsapling.png", None, "sapling"),
    ("items/cloudsapling.png", None,
     "items/birchsapling.png", None, "sapling item icon"),
    ("items/cloudwood.png", None,
     "items/birchlog.png", None, "log item icon"),
    ("particles/cloudleaves.png", ("auto", 20, 20),
     "particles/birchleaves.png", ("auto", 20, 20), "leaf particle (densest 20px frame)"),
    # content/arsenal: the craftable weapon tier. A weapon icon answers to the
    # vanilla weapon of the SAME CLASS at the same tier, because vanilla draws
    # a glaive, a greatbow, a staff, a summon focus and a boomerang at very
    # different masses inside the same 32px cell (440 / 328 / 352 / 400 / 464).
    # These are the same items each weapon's class comment calibrates its
    # damage against, so one table row covers art and balance alike.
    ("items/skyreave.png", None,
     "items/quartzglaive.png", None, "glaive item icon"),
    ("items/thunderhead.png", None,
     "items/tungstengreatbow.png", None, "greatbow item icon"),
    ("items/prismcaller.png", None,
     "items/quartzstaff.png", None, "magic staff item icon"),
    ("items/skywatchwhistle.png", None,
     "items/batcage.png", None, "summon focus item icon"),
    ("items/stormdisc.png", None,
     "items/tungstenboomerang.png", None, "boomerang item icon"),
    # Mid-attack sprites: vanilla pairs a fixed canvas size with the item's
    # attackXOffset/attackYOffset pivot, so ours use the same canvases.
    ("player/weapons/skyreave.png", None,
     "player/weapons/quartzglaive.png", None, "glaive attack sprite"),
    ("player/weapons/thunderhead.png", None,
     "player/weapons/tungstengreatbow.png", None, "greatbow attack sprite"),
    ("player/weapons/prismcaller.png", None,
     "player/weapons/quartzstaff.png", None, "staff attack sprite"),
    # Projectiles. The prism bolt reuses vanilla's shared bolt_shadow, so only
    # the bolt itself is ours; the storm disc ships its own shadow.
    ("projectiles/prismbolt.png", None,
     "projectiles/quartzbolt.png", None, "magic bolt projectile"),
    ("projectiles/stormdisc.png", None,
     "projectiles/frostboomerang.png", None, "boomerang projectile"),
    ("projectiles/stormdisc_shadow.png", None,
     "projectiles/frostboomerang_shadow.png", None, "boomerang projectile shadow"),
    # Bestiary icons for the four new enemies and the Watch Mote. Their BODIES
    # are vanilla textures loaded at runtime (MobRegistry.Textures), but
    # MobRegistry.loadIcon is hard-wired to mobs/icons/<our stringID> with no
    # setter, so the icon is the one piece that has to be ours. Each answers to
    # the vanilla icon of the mob whose sheet it wears.
    ("mobs/icons/rimesentry.png", None,
     "mobs/icons/frostsentry.png", None, "bestiary icon"),
    ("mobs/icons/auroraflake.png", None,
     "mobs/icons/cryoflake.png", None, "bestiary icon"),
    ("mobs/icons/fenwraith.png", None,
     "mobs/icons/spiritghoul.png", None, "bestiary icon"),
    ("mobs/icons/cindercantor.png", None,
     "mobs/icons/ancientskeletonmage.png", None, "bestiary icon"),
    ("mobs/icons/watchmote.png", None,
     "mobs/icons/playercryoflake.png", None, "bestiary icon"),
    # Skywatch professions. The four spire furniture pieces answer to the oak
    # family, which is the vanilla set the engine draws with exactly the same
    # code; the front column (rotation 2) and one side column (rotation 1) are
    # measured, because they are the two silhouettes the piece actually has.
    ("objects/skywatchbookshelf.png", (64, 0, 32, 128),
     "objects/oakbookshelf.png", (64, 0, 32, 128), "bookshelf (front rotation column)"),
    ("objects/skywatchbookshelf.png", (32, 0, 32, 128),
     "objects/oakbookshelf.png", (32, 0, 32, 128), "bookshelf (side rotation column)"),
    ("objects/skywatchcabinet.png", (64, 0, 32, 128),
     "objects/oakcabinet.png", (64, 0, 32, 128), "cabinet (front rotation column)"),
    ("objects/skywatchcabinet.png", (32, 0, 32, 128),
     "objects/oakcabinet.png", (32, 0, 32, 128), "cabinet (side rotation column)"),
    ("objects/skywatchclock.png", (64, 0, 32, 64),
     "objects/oakclock.png", (64, 0, 32, 64), "clock (front rotation column)"),
    ("objects/skywatchclock.png", (32, 0, 32, 64),
     "objects/oakclock.png", (32, 0, 32, 64), "clock (side rotation column)"),
    ("objects/skywatchdisplay.png", (0, 0, 32, 32),
     "objects/oakdisplay.png", (0, 0, 32, 32), "display stand (one rotation cell)"),
    # The three workstations. The loom is a CraftingStationObject like the
    # alchemy table; the forge and the kiln are processing stations two tiles
    # tall, and vanilla's own two-tile station is the forge. The forge's fire
    # strip is a separate 32px animation row and is measured against vanilla's.
    ("objects/windsilkloom.png", (64, 0, 32, 64),
     "objects/alchemytable.png", (64, 0, 32, 64), "loom (front rotation column)"),
    ("objects/windsilkloom.png", (32, 0, 32, 64),
     "objects/alchemytable.png", (32, 0, 32, 64), "loom (side rotation column)"),
    ("objects/aetherforge.png", (64, 0, 32, 64),
     "objects/forge.png", (64, 0, 32, 64), "forge body (front rotation column)"),
    # The fire cell is addressed explicitly, not with "auto": the densest 32px
    # cell of this sheet is a body column, so an auto scan would compare the
    # forge's masonry against vanilla's flame and call it four times too big.
    ("objects/aetherforge.png", (0, 64, 32, 32),
     "objects/forge.png", (0, 64, 32, 32), "forge fire frame (animation row)"),
    ("objects/stormglasskiln.png", (64, 0, 32, 64),
     "objects/forge.png", (64, 0, 32, 64), "kiln (front rotation column)"),
    ("objects/stormglasskiln_on.png", (64, 0, 32, 64),
     "objects/forge.png", (64, 0, 32, 64), "kiln lit (front rotation column)"),
    # Icons. A workstation icon answers to a workstation icon, a bar to a bar,
    # a bolt of cloth to vanilla's silk and a pane to vanilla's glass.
    ("items/windsilkloom.png", None, "items/alchemytable.png", None, "station item icon"),
    ("items/aetherforge.png", None, "items/forge.png", None, "station item icon"),
    ("items/stormglasskiln.png", None, "items/cheesepress.png", None, "station item icon"),
    ("items/skywatchbookshelf.png", None, "items/oakbookshelf.png", None, "furniture item icon"),
    ("items/skywatchcabinet.png", None, "items/oakcabinet.png", None, "furniture item icon"),
    ("items/skywatchclock.png", None, "items/oakclock.png", None, "furniture item icon"),
    ("items/skywatchdisplay.png", None, "items/oakdisplay.png", None, "furniture item icon"),
    ("items/skyweave.png", None, "items/silk.png", None, "cloth item icon"),
    ("items/stormsteelbar.png", None, "items/ironbar.png", None, "metal bar item icon"),
    ("items/stormglass.png", None, "items/glass.png", None, "glass item icon"),
    # Skyreach gear (content/itempolish). The Stormsteel set is calibrated
    # against vanilla's TUNGSTEN set in every dimension the game measures --
    # armour value, enchant cost, rarity -- so it answers to tungsten here too,
    # sheet for sheet and icon for icon. The accessories answer to the vanilla
    # trinket each one's buff was calibrated against.
    ("player/armor/stormsteelhelmet.png", None,
     "player/armor/tungstenhelmet.png", None, "helmet armor sheet"),
    ("player/armor/stormsteelchest.png", None,
     "player/armor/tungstenchest.png", None, "chest armor sheet"),
    ("player/armor/stormsteelboots.png", None,
     "player/armor/tungstenboots.png", None, "boots armor sheet"),
    ("player/armor/stormsteelarms_left.png", None,
     "player/armor/tungstenarms_left.png", None, "chest arms sheet (left)"),
    ("player/armor/stormsteelarms_right.png", None,
     "player/armor/tungstenarms_right.png", None, "chest arms sheet (right)"),
    ("items/stormsteelhelmet.png", None,
     "items/tungstenhelmet.png", None, "helmet item icon"),
    ("items/stormsteelchestplate.png", None,
     "items/tungstenchestplate.png", None, "chestplate item icon"),
    ("items/stormsteelboots.png", None,
     "items/tungstenboots.png", None, "boots item icon"),
    ("items/stormsteelvambrace.png", None,
     "items/vambrace.png", None, "trinket item icon"),
    ("items/auroralocket.png", None,
     "items/frozenheart.png", None, "trinket item icon"),
    ("items/zephyrharness.png", None,
     "items/airvessel.png", None, "trinket item icon"),
    # The thin-icon batch. These twelve shipped between 29 and 117 opaque px
    # while every vanilla 32x32 item icon in the dump carries 288-712 (median
    # 440) -- tempestedge, one of the mod's two original weapons, was a 45px
    # hairline. None of them had a row here, which is the whole reason they
    # shipped: this audit only ever sees what it is pointed at, and 207 of the
    # mod's 307 PNGs were pointed at nothing. Each analogue below is the
    # vanilla icon whose CONSTRUCTION the redraw was briefed against, so the
    # gate and the brief rest on the same fact.
    ("items/flickerlightgarland.png", None,
     "items/silk.png", None, "strung deco item icon"),
    ("items/tempestedge.png", None,
     "items/quartzglaive.png", None, "sword item icon"),
    ("items/veilessence.png", None,
     "items/resistancepotion.png", None, "essence item icon"),
    ("items/ghostlantern.png", None,
     "items/oakclock.png", None, "lantern item icon (tall, narrow)"),
    ("items/wardencandelabra.png", None,
     "items/oakclock.png", None, "candelabra item icon (tall, narrow)"),
    ("items/stormshard.png", None,
     "items/glass.png", None, "mineral shard item icon"),
    ("items/aeronautwreck.png", None,
     "items/airvessel.png", None, "wreckage item icon"),
    ("items/fulgurite.png", None,
     "items/glass.png", None, "mineral item icon"),
    ("items/galehowl.png", None,
     "items/tungstengreatbow.png", None, "bow item icon"),
    ("items/glowfern.png", None,
     "items/birchsapling.png", None, "soft plant item icon"),
    ("items/withershrub.png", None,
     "items/birchsapling.png", None, "shrub item icon"),
    ("items/aurorapetal.png", None,
     "items/inefficientfeather.png", None, "petal item icon"),
    # Both held sprites share their icon's drawing helper (_tempest_blade,
    # _galehowl_bow), so a thickened blade reaches them whether or not anyone
    # meant it to. Rows here make that an intended, watched output instead of
    # collateral nobody looked at.
    #
    # They are deliberately MANUAL rows, not ratios. These two sit on a 32x32
    # canvas while every other held weapon in the mod matches vanilla's own
    # much larger sheets (skyreave 96x95 vs quartzglaive 104x88; thunderhead
    # 22x62 vs tungstengreatbow 20x60). An opaque-mass ratio between canvases
    # that differ by 3x measures the canvas, not the drawing, and would need an
    # ACCEPTED floor so low it gated nothing. The canvas question is real and
    # open -- see docs/CURRENT_STATE.md -- but it is a rendering-geometry
    # change, not an art change, so it is not silently folded in here.
    ("player/weapons/tempestedge.png", None,
     None, None, "sword held sprite: 32x32 canvas, cf. skyreave at 96x95"),
    ("player/weapons/galehowl.png", None,
     None, None, "bow held sprite: 32x32 canvas, cf. thunderhead at 22x62"),
    # The world-sprite batch. Same cause as the icons above -- no row, never
    # measured -- but these are what the player sees standing in the level, not
    # a thumbnail in a slot. What makes each one damning is that the mod already
    # had a sibling clearing the same bar against the same reference: the
    # ghost lantern sat at 0.29 of copperstreetlamp while wardencandelabra was
    # at 0.76, the garland at 0.17 of walltorch while mistglasslantern was at
    # 1.18, and both new ore overlays under 0.40 of ironore while aetheriumore
    # was at 1.23. The mod knew how; nothing was checking.
    ("objects/ghostlantern.png", (0, 0, 32, 96),
     "objects/copperstreetlamp.png", (0, 0, 32, 96), "streetlamp (on half)"),
    ("objects/flickerlightgarland.png", ("auto", 32, 32),
     "objects/walltorch.png", ("auto", 32, 32), "wall light (densest cell)"),
    ("objects/fulguriteore.png", ("auto", 32, 32),
     "objects/ironore.png", ("auto", 32, 32), "ore overlay (densest 32px cell)"),
    ("objects/prismshardore.png", ("auto", 32, 32),
     "objects/ironore.png", ("auto", 32, 32), "ore overlay (densest 32px cell)"),
    # The salvage crate answers to vanilla's own crates, sheet for sheet: same
    # 192x64 six-variant layout, same ground line, so a field of ours and a
    # field of vanilla's read at the same scale.
    ("objects/skycrate.png", ("auto", 32, 64),
     "objects/crates.png", ("auto", 32, 64), "salvage crate (densest variant)"),
    ("objects/skyanchor.png", None,
     "objects/bannerstand.png", None, "tall quest prop"),
    ("objects/catbasket.png", None,
     "objects/decorativepot1.png", None, "pet bed / small furniture"),
]

THRESHOLD = 0.75

# Per-sprite accepted minimums: our silhouette is legitimately lighter than
# the closest vanilla analogue (candles + ground ring vs a solid stone altar;
# a perched bird vs a robed figure; slim crystal shards vs a crystal wall).
# Reviewed on 4x contact sheets — they read correctly in game at these masses.
ACCEPTED = {
    "objects/seancecircle.png": 0.55,
    "objects/statues/gloomraven.png": 0.65,
    "objects/stormcrystal.png": 0.70,
    # a bare weeping willow is airier than a solid vanilla dead tree
    "objects/gloomwillow.png": 0.45,
}


def measure(img, cell):
    if cell is not None and cell[0] == "auto":
        # Scan every cell of the given size and measure the DENSEST one:
        # vanilla sheets often leave the top-left cell empty (rotation rows,
        # off-states), which would make a fixed corner crop meaningless.
        _, cw, ch = cell
        best = (0, 0, 0)
        for cx in range(0, max(img.width - cw + 1, 1), cw):
            for cy in range(0, max(img.height - ch + 1, 1), ch):
                got = measure(img.crop((cx, cy, cx + cw, cy + ch)), None)
                if got[2] > best[2]:
                    best = got
        return best
    if cell is not None:
        img = img.crop((cell[0], cell[1], cell[0] + cell[2], cell[1] + cell[3]))
    px = img.load()
    xs, ys, opaque = [], [], 0
    for x in range(img.width):
        for y in range(img.height):
            if px[x, y][3] > 24:
                xs.append(x)
                ys.append(y)
                opaque += 1
    if not xs:
        return (0, 0, 0)
    return (max(xs) - min(xs) + 1, max(ys) - min(ys) + 1, opaque)


def default_vanilla():
    """Where the sprite dump actually is, preferring this checkout's own copy.

    The default used to be a hard-coded dev-container path. On any machine
    without that exact directory EVERY pair resolved to "vanilla ref missing",
    the flag count stayed 0, and the audit printed "0 sprite(s) flagged" while
    comparing nothing at all -- which is what AGENTS.md documents as the way to
    run it. `vanilla-sprites/` is gitignored on purpose but is where the dump
    lives in a working checkout, so look there first.
    """
    local = os.path.join(REPO, "vanilla-sprites")
    if os.path.isdir(os.path.join(local, "items")):
        return local
    return "/home/user/necesse-game/sprites"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vanilla", default=default_vanilla())
    args = parser.parse_args()

    rows = []
    for ours, ocell, theirs, vcell, note in PAIRS:
        opath = os.path.join(RES, ours)
        if not os.path.exists(opath):
            rows.append((ours, note, None, "missing mod file"))
            continue
        ow, oh, oarea = measure(Image.open(opath).convert("RGBA"), ocell)
        if theirs is None:
            rows.append((ours, note, None, f"ours {ow}x{oh} ({oarea}px) - manual check"))
            continue
        vpath = os.path.join(args.vanilla, theirs)
        if not os.path.exists(vpath):
            rows.append((ours, note, None, f"vanilla ref missing: {theirs}"))
            continue
        vw, vh, varea = measure(Image.open(vpath).convert("RGBA"), vcell)
        ratio = oarea / varea if varea else 0
        limit = ACCEPTED.get(ours, THRESHOLD)
        verdict = "OK" if ratio >= limit else "FIX"
        rows.append((ours, note, ratio,
                     f"ours {ow}x{oh} ({oarea}px) vs {theirs.split('/')[-1]} {vw}x{vh} ({varea}px) -> {ratio:.2f} {verdict}"))

    flagged = 0
    for ours, note, ratio, detail in rows:
        limit = ACCEPTED.get(ours, THRESHOLD)
        mark = "!!" if ratio is not None and ratio < limit else "  "
        if ratio is not None and ratio < limit:
            flagged += 1
        print(f"{mark} {ours:40s} [{note}] {detail}")
    compared = sum(1 for _, _, ratio, _ in rows if ratio is not None)
    manual = sum(1 for _, _, ratio, detail in rows
                 if ratio is None and "manual check" in detail)
    unref = len(rows) - compared - manual

    print(f"\n{flagged} sprite(s) flagged below {THRESHOLD:.0%} of vanilla mass.")
    print(f"{compared} of {len(rows)} row(s) actually compared against the dump at "
          f"{args.vanilla} ({manual} manual, {unref} without a reference).")
    if compared == 0:
        print("FAIL: nothing was measured. A run that compares no sprite is not a "
              "pass -- point --vanilla at a sprite dump.")
        return 1
    return 1 if flagged else 0


if __name__ == "__main__":
    sys.exit(main())
