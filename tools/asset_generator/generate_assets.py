#!/usr/bin/env python3
"""Stairway to Heaven — deterministic asset pipeline.

Regenerates every mod texture into src/main/resources/. All output is seeded:
running this twice produces byte-identical PNGs, so art diffs stay reviewable.

Usage:  python3 tools/asset_generator/generate_assets.py [--out <resources-dir>]

Sheet formats are documented in docs/research/asset-formats.md; palette and
style rules in docs/assets-style-guide.md.
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import palette  # noqa: E402
import gen_tiles  # noqa: E402
import gen_rocks
import gen_serpent  # noqa: E402
import gen_objects  # noqa: E402
import gen_mobs  # noqa: E402
import gen_armor
import gen_items  # noqa: E402
import gen_misc  # noqa: E402
import gen_npcs
import gen_critters  # noqa: E402
import gen_veil  # noqa: E402
import gen_splats  # noqa: E402
import gen_walls  # noqa: E402
import gen_props  # noqa: E402
import gen_furniture  # noqa: E402
import gen_trees  # noqa: E402
import gen_skyfurniture  # noqa: E402
import gen_cloudmarble  # noqa: E402
import gen_beetlewall  # noqa: E402
import gen_arsenal  # noqa: E402
import gen_professions  # noqa: E402
import gen_skygear  # noqa: E402


# Files owned by tools/convert_biome_art.py, which converts the supplied
# reference art. Two producers writing the same path meant whichever ran last
# won, silently; the guard at the end of main() fails loudly instead.
CONVERTED = ("tiles/skyway.png", "tiles/skyway_splat.png",
             "objects/skyseraphtree.png", "items/skyseraphtree.png",
             "objects/statues/seraph.png", "items/seraphstatue.png",
             "objects/cloudtree.png", "items/cloudtree.png",
             "objects/nimbuswillow.png")


def _stamp(out, rel):
    """(mtime_ns, size) of a converted file, or None when it is not there.

    The guard used to ask whether the file EXISTS after the run, which is true
    of every one of them in a normal checkout — so `generate_assets.py` run the
    documented way, against src/main/resources, always failed and could only
    ever be run into an empty directory. What it has to ask is whether THIS RUN
    wrote it, so the stamp is taken before anything is generated and compared
    after. A converted file that appeared, or whose bytes were replaced, still
    fails exactly as loudly as before.
    """
    try:
        st = os.stat(os.path.join(out, rel))
    except OSError:
        return None
    return (st.st_mtime_ns, st.st_size)


def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", default=os.path.join(repo_root, "src", "main", "resources"))
    args = parser.parse_args()
    out = args.out

    before = {rel: _stamp(out, rel) for rel in CONVERTED}

    for sub in ("tiles", "objects", "objects/statues", "items", "mobs", "mobs/icons", "player/weapons",
                "projectiles", "locale", "ui/mapicons", "particles"):
        os.makedirs(os.path.join(out, sub), exist_ok=True)

    # Terrain + liquid: modern _splat atlases (the 1.3.2 renderer's primary
    # path; the marble checker deliberately stays legacy — see gen_furniture)
    # v0.5 art sprint: sky grounds ship 6 variants like vanilla grass (was 3) —
    # richer repetition breakup across large meadows.
    gen_splats.build_splat(f"{out}/tiles/cloudturf_splat.png", gen_splats.material_cloudturf, 6, 0xC1,
                           features=gen_splats.features_cloudturf)
    gen_splats.build_splat(f"{out}/tiles/aurorashoal_splat.png", gen_splats.material_auroraturf, 5, 0xA5,
                           features=gen_splats.features_auroraturf)
    gen_splats.build_splat(f"{out}/tiles/skystone_splat.png", gen_splats.material_skystone, 5, 0x51,
                           features=gen_splats.features_skystone)
    gen_splats.build_splat(f"{out}/tiles/stormslate_splat.png", gen_splats.material_stormslate, 6, 0x57,
                           features=gen_splats.features_stormslate)
    gen_splats.build_splat(f"{out}/tiles/gloomwoodfloor_splat.png", gen_splats.material_gloomwood, 2, 0x6D,
                           features=gen_splats.features_gloomwood)
    gen_splats.build_splat(f"{out}/tiles/mistsea_shallow_splat.png", gen_splats.material_mist(False), 1, 0x315E, frames=8)
    gen_splats.build_splat(f"{out}/tiles/mistsea_deep_splat.png", gen_splats.material_mist(True), 1, 0xD1EE, frames=8)
    gen_furniture.gen_marblechecker(f"{out}/tiles/marblechecker.png")

    # v0.3: the Veil — terrain, water, flora, rift, lantern, shade
    gen_splats.build_splat(f"{out}/tiles/murkmoss_splat.png", gen_veil.material_murkmoss, 3, 0x3E,
                           features=gen_veil.features_murkmoss)
    gen_splats.build_splat(f"{out}/tiles/blackpeat_splat.png", gen_veil.material_blackpeat, 2, 0xB1,
                           features=gen_veil.features_blackpeat)
    gen_splats.build_splat(f"{out}/tiles/ashsand_splat.png", gen_veil.material_ashsand, 3, 0xA5,
                           features=gen_veil.features_ashsand)
    gen_splats.build_splat(f"{out}/tiles/murkwater_shallow_splat.png", gen_veil.material_murkwater(False), 1, 0x3E77, frames=8)
    gen_splats.build_splat(f"{out}/tiles/murkwater_deep_splat.png", gen_veil.material_murkwater(True), 1, 0xDEE7, frames=8)
    # remove the superseded legacy strips so only one source of truth ships
    for legacy in ("cloudturf.png", "skystone.png", "stormslate.png", "mistsea_shallow.png", "mistsea_deep.png"):
        legacy_path = os.path.join(out, "tiles", legacy)
        if os.path.exists(legacy_path):
            os.remove(legacy_path)

    # Rocks + ore overlay
    # v0.6 rock family: 8 Skystone variants / 6 Veilrock variants (vanilla
    # rock ships 4, caverock 8 — two variants read as repetition in game).
    gen_rocks.gen_rock_sheet(f"{out}/objects/skystonerock.png", palette.SKYSTONE, variants=8)
    gen_rocks.gen_ore_sheet(f"{out}/objects/aetheriumore.png", palette.AETHERIUM)

    # Objects
    gen_objects.gen_stairway_down(f"{out}/objects/skystairwaydown.png")
    gen_objects.gen_stairway_up(f"{out}/objects/skystairwayup.png")
    gen_objects.gen_windwheat(f"{out}/objects/windwheat.png")
    gen_objects.gen_cloudberrybush(f"{out}/objects/cloudberrybush.png")
    gen_objects.gen_cloudberrysapling(f"{out}/objects/cloudberrysapling.png")
    gen_rocks.gen_rock_sheet(f"{out}/objects/veilrock.png", palette.VEILROCK, variants=6, salt=0x3E1F)
    gen_veil.gen_whisperreeds(f"{out}/objects/whisperreeds.png")
    gen_veil.gen_gloomshroom(f"{out}/objects/gloomshroom.png")
    gen_veil.gen_ashbones(f"{out}/objects/ashbones.png")
    gen_veil.gen_seancecircle(f"{out}/objects/seancecircle.png")
    gen_veil.gen_riftdown(f"{out}/objects/veilriftdown.png")
    gen_veil.gen_riftup(f"{out}/objects/veilriftup.png")
    gen_veil.gen_ghostlantern(f"{out}/objects/ghostlantern.png")
    gen_veil.gen_deadtree(f"{out}/objects/deadtree.png")
    gen_objects.gen_crystal_cluster(f"{out}/objects/stormcrystal.png", palette.STORMCRYSTAL, 0x57C7)
    gen_objects.gen_aurorabloom(f"{out}/objects/aurorabloom.png")
    gen_objects.gen_skyreeds(f"{out}/objects/skyreeds.png")
    gen_objects.gen_mapicons(f"{out}/ui/mapicons")

    # ===== v0.4 "The Living Sky": per-biome fill =====
    # Trees (vanilla TreeObject 128px cells) + leaf particles + saplings
    # objects/nimbuswillow.png is NOT generated: it is supplied art, kept in
    # src/main/resources/kk-sprites/ and copied in as-is. gen_nimbuswillow
    # stays available in gen_trees for reference, but calling it here would
    # overwrite the supplied sheet on the next run. Its sapling, leaves and log
    # icon are still ours and are still generated below.
    gen_trees.gen_fulgurpine(f"{out}/objects/fulgurpine.png")
    gen_trees.gen_prismabirch(f"{out}/objects/prismabirch.png")
    gen_trees.gen_nimbuswillow_leaves(f"{out}/particles/nimbusleaves.png")
    gen_trees.gen_fulgurpine_leaves(f"{out}/particles/fulgurleaves.png")
    gen_trees.gen_prismabirch_leaves(f"{out}/particles/prismaleaves.png")
    gen_trees.gen_saplings(f"{out}/objects")
    gen_trees.gen_nimbuswood_item(f"{out}/items/nimbuswood.png")
    gen_trees.gen_charwood_item(f"{out}/items/charwood.png")
    gen_trees.gen_prismwood_item(f"{out}/items/prismwood.png")
    # Plants + meadow carpet grasses + ore overlays
    gen_objects.gen_cloudbell(f"{out}/objects/cloudbell.png")
    gen_objects.gen_skytulip(f"{out}/objects/skytulip.png")
    gen_objects.gen_staticmoss(f"{out}/objects/staticmoss.png")
    gen_objects.gen_thunderbloom(f"{out}/objects/thunderbloom.png")
    gen_objects.gen_glowfern(f"{out}/objects/glowfern.png")
    gen_objects.gen_auroralily(f"{out}/objects/auroralily.png")
    gen_objects.gen_tallcloudgrass(f"{out}/objects/tallcloudgrass.png")
    gen_objects.gen_stormsedge(f"{out}/objects/stormsedge.png")
    gen_objects.gen_prismgrass(f"{out}/objects/prismgrass.png")
    gen_objects.gen_fulguriteore(f"{out}/objects/fulguriteore.png")
    gen_objects.gen_prismshardore(f"{out}/objects/prismshardore.png")
    gen_objects.gen_cloudbell_item(f"{out}/items/cloudbell.png")
    gen_objects.gen_skytulip_item(f"{out}/items/skytulip.png")
    gen_objects.gen_thunderbloom_item(f"{out}/items/thunderbloom.png")
    gen_objects.gen_auroralily_item(f"{out}/items/auroralily.png")
    gen_objects.gen_glowfern_item(f"{out}/items/glowfern.png")
    gen_objects.gen_staticmoss_item(f"{out}/items/staticmoss.png")
    gen_objects.gen_fulgurite_item(f"{out}/items/fulgurite.png")
    gen_objects.gen_prismshard_item(f"{out}/items/prismshard.png")
    # Buildable wood floors
    gen_splats.build_splat(f"{out}/tiles/nimbusfloor_splat.png", gen_splats.material_nimbusfloor, 2, 0x81B,
                           features=gen_splats.features_nimbusfloor)
    gen_splats.build_splat(f"{out}/tiles/charfloor_splat.png", gen_splats.material_charfloor, 2, 0xC4A,
                           features=gen_splats.features_charfloor)
    gen_splats.build_splat(f"{out}/tiles/prismfloor_splat.png", gen_splats.material_prismfloor, 2, 0x981,
                           features=gen_splats.features_prismfloor)
    # Fauna: two critters, two enemies, bestiary icons
    gen_critters.gen_critters_v04(f"{out}/mobs")
    gen_critters.gen_critter_icons_v04(f"{out}/mobs/icons")
    gen_mobs.gen_galehound(f"{out}/mobs/galehound.png")
    gen_mobs.gen_dawnpiercer(f"{out}/mobs/dawnpiercer.png")
    gen_mobs.gen_icons_v04(f"{out}/mobs/icons")

    # Mobs + bestiary icons
    gen_mobs.gen_zephyrray(f"{out}/mobs/zephyrray.png")
    gen_mobs.gen_stormwisp(f"{out}/mobs/stormwisp.png")
    gen_mobs.gen_skystonegolem(f"{out}/mobs/skystonegolem.png")
    gen_mobs.gen_icons(f"{out}/mobs/icons")
    # The Mistserpent: worm sheet, dive mask, shadow and bestiary icon.
    gen_serpent.gen_mistserpent(f"{out}/mobs", f"{out}/mobs/icons")

    # NPCs: the Sky Warden and the spire cats
    gen_npcs.gen_warden(f"{out}/mobs/skywarden.png")
    gen_npcs.gen_cats(f"{out}/mobs/spirecatblack.png", f"{out}/mobs/spirecattabby.png")
    gen_npcs.gen_npc_icons(f"{out}/mobs/icons")
    gen_critters.gen_critters(f"{out}/mobs")
    gen_critters.gen_critter_icons(f"{out}/mobs/icons")
    gen_veil.gen_gloomshade(f"{out}/mobs/gloomshade.png")
    gen_veil.gen_shade_icon(f"{out}/mobs/icons/gloomshade.png")

    # Item icons
    gen_items.gen_skystone(f"{out}/items/skystone.png")
    gen_items.gen_aetheriumore(f"{out}/items/aetheriumore.png")
    gen_items.gen_aetheriumbar(f"{out}/items/aetheriumbar.png")
    gen_items.gen_stormshard(f"{out}/items/stormshard.png")
    gen_items.gen_windsilk(f"{out}/items/windsilk.png")
    gen_items.gen_aurorapetal(f"{out}/items/aurorapetal.png")
    # Tree and sapling item icons: GameObject.generateItemTexture loads
    # items/<stringID>, and TreeObject/TreeSaplingObject do not override it,
    # so without these the crafting menu shows the engine error texture.
    gen_items.gen_sapling_item_icons(f"{out}/objects", f"{out}/items")
    gen_items.gen_tree_item_icons(f"{out}/items")
    # The Warden's clothing as real armor sheets on the human body
    # (player/armor/) plus their 32x32 item icons.
    gen_armor.gen_all(out)
    gen_items.gen_tempestedge_icon(f"{out}/items/tempestedge.png")
    gen_items.gen_galehowl_icon(f"{out}/items/galehowl.png")
    gen_items.gen_skystonerock_item(f"{out}/items/skystonerock.png")
    gen_items.gen_skyreeds_item(f"{out}/items/skyreeds.png")
    gen_items.gen_stairway_item(f"{out}/items/skystairwaydown.png")
    gen_items.gen_windwheat_item(f"{out}/items/windwheat.png")
    gen_items.gen_cloudberrybush_item(f"{out}/items/cloudberrybush.png")
    gen_items.gen_cloudberry_item(f"{out}/items/cloudberry.png")
    gen_veil.gen_veil_item_icons(f"{out}/items")
    gen_items.gen_crystal_item(f"{out}/items/stormcrystal.png", palette.STORMCRYSTAL, 0x1CE1)
    gen_items.gen_crystal_item(f"{out}/items/aurorabloom.png", palette.AURORA, 0x1CE2)

    # Held weapon sprites
    gen_items.gen_tempestedge_held(f"{out}/player/weapons/tempestedge.png")
    gen_items.gen_galehowl_held(f"{out}/player/weapons/galehowl.png")

    # v0.6: building set, quest structure pieces, NPC-adjacent deco
    gen_walls.gen_walls(f"{out}/objects")
    # v0.6 prop families: Spire hero accents, Stormveil/Aurora environmental
    # props, sky-oddity seeds (see gen_props.py docstring for the split)
    gen_props.gen_all(f"{out}/objects")
    gen_props.gen_prop_icons(f"{out}/items")
    gen_furniture.gen_skyironfence(f"{out}/objects/skyironfence.png")
    gen_furniture.gen_skyironfencegate(f"{out}/objects/skyironfencegate.png")
    gen_furniture.gen_candelabra(f"{out}/objects/wardencandelabra.png")
    gen_furniture.gen_wall_light(f"{out}/objects/mistglasslantern.png", "lantern")
    gen_furniture.gen_wall_light(f"{out}/objects/flickerlightgarland.png", "garland")
    gen_furniture.gen_gloomraven_statue(f"{out}/objects/statues/gloomraven.png")
    gen_furniture.gen_gloomwillow(f"{out}/objects/gloomwillow.png")
    gen_furniture.gen_catbasket(f"{out}/objects/catbasket.png")
    gen_furniture.gen_banner_painting(f"{out}/objects/skywatchbanner.png")
    gen_furniture.gen_beacon(f"{out}/objects/wardenbeaconoff.png", False)
    gen_furniture.gen_beacon(f"{out}/objects/wardenbeaconon.png", True)
    gen_furniture.gen_skyanchor(f"{out}/objects/skyanchor.png")
    # v0.7 stone barrens: what grows on the grey skystone ground
    gen_objects.gen_skylichen(f"{out}/objects/skylichen.png")
    gen_objects.gen_skylichen_item(f"{out}/items/skylichen.png")
    gen_objects.gen_cragbloom(f"{out}/objects/cragbloom.png")
    gen_objects.gen_cragbloom_item(f"{out}/items/cragbloom.png")
    gen_objects.gen_skyscree(f"{out}/objects/skyscree.png")
    gen_objects.gen_skyscree_item(f"{out}/items/skyscree.png")

    # Skywatch furniture family, on the vanilla furniture base classes
    gen_skyfurniture.generate(f"{out}/objects", f"{out}/items")
    # Cloudmarble masonry. The Skyway ground it used to draw now comes from
    # tools/convert_biome_art.py instead — see the note in gen_cloudmarble.
    # The WALL, however, is drawn here again: shipping the supplied
    # illustration as the sheet put 10,858 colours and a near-white cap into a
    # format vanilla builds out of ~20, which is what "die Wände blenden fast"
    # was. Same call the Beetlefreak wall already makes, same reason.
    gen_cloudmarble.generate(f"{out}/objects", f"{out}/items", f"{out}/tiles")
    # Beetlefreak masonry. The supplied sheet in kk-sprites/ is a continuous
    # illustration, not an auto-tile blob, so it could not tile however it was
    # repacked; tools/convert_biome_art.py no longer produces this wall and
    # gen_beetlewall redraws it on the layout the engine actually reads. See
    # tools/wall_render_preview.py for the composed proof.
    gen_beetlewall.generate(f"{out}/objects", f"{out}/items")
    # Skywatch professions: the three settlement workstations, the four spire
    # furniture pieces on their vanilla base classes, and the materials the
    # stations make.
    gen_professions.generate(f"{out}/objects", f"{out}/items")
    # Sky Seraph tree companions (the tree itself is converted reference art)
    gen_trees.gen_skyseraphsapling(f"{out}/objects", f"{out}/items")
    gen_trees.gen_seraphwood_item(f"{out}/items/seraphwood.png")
    gen_trees.gen_skyseraphtree_leaves(f"{out}/particles/seraphleaves.png")
    # Cloud Tree companions (the tree itself is supplied art, see kk-sprites)
    gen_trees.gen_cloudsapling(f"{out}/objects", f"{out}/items")
    gen_trees.gen_cloudwood_item(f"{out}/items/cloudwood.png")
    gen_trees.gen_cloudtree_leaves(f"{out}/particles/cloudleaves.png")

    gen_furniture.gen_v2_item_icons(f"{out}/items")
    gen_furniture.gen_set_icons(f"{out}/items")

    # Skyreach gear (content/itempolish): the Stormsteel plate set on real
    # player/armor sheets, plus the three accessory icons. Draws on gen_armor's
    # measured human anatomy and on gen_professions' stormsteel ramp, so the
    # armour cannot drift away from either the body or the bar it is forged
    # from. See tools/asset_generator/gen_skygear.py.
    gen_skygear.generate(out)

    # Sky Arsenal (content/arsenal): five craftable weapons, their
    # projectile sprites, and the bestiary icons for the five mobs the
    # stream adds. See tools/asset_generator/gen_arsenal.py.
    gen_arsenal.generate(out)

    # Mod preview
    gen_misc.gen_preview(f"{out}/preview.png")

    clash = [rel for rel in CONVERTED
             if _stamp(out, rel) != before.get(rel)]
    if clash:
        raise SystemExit(
            "generate_assets wrote files owned by tools/convert_biome_art.py: "
            + ", ".join(clash)
            + "\nOne producer per file. Drop it here, or drop the conversion.")

    print(f"Assets written to {out}")


if __name__ == "__main__":
    main()
