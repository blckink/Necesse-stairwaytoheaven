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
import gen_rocks  # noqa: E402
import gen_objects  # noqa: E402
import gen_mobs  # noqa: E402
import gen_items  # noqa: E402
import gen_misc  # noqa: E402
import gen_npcs  # noqa: E402
import gen_splats  # noqa: E402
import gen_walls  # noqa: E402
import gen_furniture  # noqa: E402


def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", default=os.path.join(repo_root, "src", "main", "resources"))
    args = parser.parse_args()
    out = args.out

    for sub in ("tiles", "objects", "objects/statues", "items", "mobs", "mobs/icons", "player/weapons", "locale"):
        os.makedirs(os.path.join(out, sub), exist_ok=True)

    # Terrain + liquid: modern _splat atlases (the 1.3.2 renderer's primary
    # path; the marble checker deliberately stays legacy — see gen_furniture)
    gen_splats.build_splat(f"{out}/tiles/cloudturf_splat.png", gen_splats.material_cloudturf, 3, 0xC1,
                           features=gen_splats.features_cloudturf)
    gen_splats.build_splat(f"{out}/tiles/skystone_splat.png", gen_splats.material_skystone, 3, 0x51,
                           features=gen_splats.features_skystone)
    gen_splats.build_splat(f"{out}/tiles/stormslate_splat.png", gen_splats.material_stormslate, 3, 0x57,
                           features=gen_splats.features_stormslate)
    gen_splats.build_splat(f"{out}/tiles/gloomwoodfloor_splat.png", gen_splats.material_gloomwood, 2, 0x6D,
                           features=gen_splats.features_gloomwood)
    gen_splats.build_splat(f"{out}/tiles/mistsea_shallow_splat.png", gen_splats.material_mist(False), 1, 0x315E, frames=8)
    gen_splats.build_splat(f"{out}/tiles/mistsea_deep_splat.png", gen_splats.material_mist(True), 1, 0xD1EE, frames=8)
    gen_furniture.gen_marblechecker(f"{out}/tiles/marblechecker.png")
    # remove the superseded legacy strips so only one source of truth ships
    for legacy in ("cloudturf.png", "skystone.png", "stormslate.png", "mistsea_shallow.png", "mistsea_deep.png"):
        legacy_path = os.path.join(out, "tiles", legacy)
        if os.path.exists(legacy_path):
            os.remove(legacy_path)

    # Rocks + ore overlay
    gen_rocks.gen_rock_sheet(f"{out}/objects/skystonerock.png", palette.SKYSTONE)
    gen_rocks.gen_ore_sheet(f"{out}/objects/aetheriumore.png", palette.AETHERIUM)

    # Objects
    gen_objects.gen_stairway_down(f"{out}/objects/skystairwaydown.png")
    gen_objects.gen_stairway_up(f"{out}/objects/skystairwayup.png")
    gen_objects.gen_crystal_cluster(f"{out}/objects/stormcrystal.png", palette.STORMCRYSTAL, 0x57C7)
    gen_objects.gen_aurorabloom(f"{out}/objects/aurorabloom.png")
    gen_objects.gen_skyreeds(f"{out}/objects/skyreeds.png")

    # Mobs + bestiary icons
    gen_mobs.gen_zephyrray(f"{out}/mobs/zephyrray.png")
    gen_mobs.gen_stormwisp(f"{out}/mobs/stormwisp.png")
    gen_mobs.gen_skystonegolem(f"{out}/mobs/skystonegolem.png")
    gen_mobs.gen_icons(f"{out}/mobs/icons")

    # NPCs: the Sky Warden and the spire cats
    gen_npcs.gen_warden(f"{out}/mobs/skywarden.png")
    gen_npcs.gen_cats(f"{out}/mobs/spirecatblack.png", f"{out}/mobs/spirecattabby.png")
    gen_npcs.gen_npc_icons(f"{out}/mobs/icons")

    # Item icons
    gen_items.gen_skystone(f"{out}/items/skystone.png")
    gen_items.gen_aetheriumore(f"{out}/items/aetheriumore.png")
    gen_items.gen_aetheriumbar(f"{out}/items/aetheriumbar.png")
    gen_items.gen_stormshard(f"{out}/items/stormshard.png")
    gen_items.gen_windsilk(f"{out}/items/windsilk.png")
    gen_items.gen_aurorapetal(f"{out}/items/aurorapetal.png")
    gen_items.gen_tempestedge_icon(f"{out}/items/tempestedge.png")
    gen_items.gen_galehowl_icon(f"{out}/items/galehowl.png")
    gen_items.gen_skystonerock_item(f"{out}/items/skystonerock.png")
    gen_items.gen_skyreeds_item(f"{out}/items/skyreeds.png")
    gen_items.gen_stairway_item(f"{out}/items/skystairwaydown.png")
    gen_items.gen_crystal_item(f"{out}/items/stormcrystal.png", palette.STORMCRYSTAL, 0x1CE1)
    gen_items.gen_crystal_item(f"{out}/items/aurorabloom.png", palette.AURORA, 0x1CE2)

    # Held weapon sprites
    gen_items.gen_tempestedge_held(f"{out}/player/weapons/tempestedge.png")
    gen_items.gen_galehowl_held(f"{out}/player/weapons/galehowl.png")

    # v0.2: building set, quest structure pieces, NPC-adjacent deco
    gen_walls.gen_walls(f"{out}/objects")
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
    gen_furniture.gen_v2_item_icons(f"{out}/items")
    gen_furniture.gen_set_icons(f"{out}/items")

    # Mod preview
    gen_misc.gen_preview(f"{out}/preview.png")

    print(f"Assets written to {out}")


if __name__ == "__main__":
    main()
