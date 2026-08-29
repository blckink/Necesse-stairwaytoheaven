package stairwaytoheaven;

import java.awt.Color;

import necesse.engine.registries.ObjectRegistry;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.CrystalClusterObject;
import necesse.level.gameObject.FruitBushObject;
import necesse.level.gameObject.GrassObject;
import necesse.level.gameObject.RockObject;
import necesse.level.gameObject.RockOreObject;

/**
 * Natural objects of the Skyreach: mineable rocks and ore, glowing crystals and
 * decorative wind grass. All built from vanilla object classes so mining,
 * tool tiers, map colors and loot behave exactly like their underground
 * counterparts.
 */
final class SkyObjects {

    private static final String[] SKY_CATEGORY = {"objects", "landscaping", "rocksandores"};

    private SkyObjects() {
    }

    static void register() {
        RockObject skystoneRock = new RockObject("skystonerock", new Color(126, 138, 154), "skystone", SKY_CATEGORY);
        SkyRegistry.skystoneRockID = ObjectRegistry.registerObject("skystonerock", skystoneRock, -1.0F, true);

        SkyRegistry.aetheriumRockID = ObjectRegistry.registerObject(
                "aetheriumrock",
                new RockOreObject(skystoneRock, "oremask", "aetheriumore", new Color(112, 194, 201), "aetheriumore", SKY_CATEGORY),
                -1.0F, true);

        // registerCrystalCluster registers the object plus its rotated variant
        // and links them, exactly like vanilla cave crystals.
        CrystalClusterObject.registerCrystalCluster("stormcrystal", new Color(122, 108, 210), 0.72F, "stormshard", 30.0F, true, SKY_CATEGORY);
        SkyRegistry.stormCrystalID = ObjectRegistry.getObjectID("stormcrystal");
        SkyRegistry.stormCrystalRID = ObjectRegistry.getObjectID("stormcrystalr");

        CrystalClusterObject.registerCrystalCluster("aurorabloom", new Color(214, 130, 172), 0.90F, "aurorapetal", 30.0F, true, SKY_CATEGORY);
        SkyRegistry.auroraBloomID = ObjectRegistry.getObjectID("aurorabloom");
        SkyRegistry.auroraBloomRID = ObjectRegistry.getObjectID("aurorabloomr");

        GrassObject skyreeds = new GrassObject("skyreeds", 4);
        skyreeds.mapColor = new Color(168, 184, 178);
        SkyRegistry.skyreedsID = ObjectRegistry.registerObject("skyreeds", skyreeds, 1.0F, true);

        // v0.2.6 forage plants (Driftlands): harvestable wheat-grass and a
        // berry bush that drops food instead of its own object item.
        GrassObject windwheat = new GrassObject("windwheat", 4);
        windwheat.mapColor = new Color(196, 196, 156);
        SkyRegistry.windwheatID = ObjectRegistry.registerObject("windwheat", windwheat, 1.0F, true);

        // The Cloudberry Bush is a REAL BUSH now (v0.9), and that one change
        // answers two player reports at once: "man kriegt nur eine Beere beim
        // Abbauen statt wie bei den Vanilla Bueschen die Buesche abbauen kann
        // und wieder aufbauen damit die Beeren nachwachsen" and "die Buesche
        // sind auch viel zu klein".
        //
        // It had been a GrassObject -- the trampled-grass archetype. That is
        // ONE-SHOT (harvest it and it is gone from the world) and it is drawn
        // on a 32px cell. Both complaints were the same mistake.
        //
        // FruitBushObject, read from the 1.3.2 decompile and matching vanilla's
        // three berry bushes verbatim except where noted:
        //   * it carries a FruitGrowerObjectEntity, so fruit REGROWS in stages
        //     and harvesting only resets the stage -- the bush stays;
        //   * it publishes a HarvestFruitLevelJob, so a settler will pick it;
        //   * breaking it drops the SEED, never the fruit (getLootTable returns
        //     seedStringID alone), which is the growth gate that keeps
        //     replanting from being an infinite-berry exploit;
        //   * its sheet is 64px cells, variants across and stages down, so the
        //     plant is finally two tiles of bush instead of one of grass.
        //
        // fruitPerStage is 1.5 against vanilla's 1.0. getFruitDropCount sums it
        // once per stage, so a full bush gives 3 rather than 2 -- deliberately
        // above vanilla because cloudberries are the Skyreach's only trough
        // feed until a Cellarer's Spent Grain exists. maxStage 2 and the
        // 900/1800s grow times are vanilla's, unchanged.
        FruitBushObject cloudberryBush = new FruitBushObject("cloudberrybush",
                "cloudberrysapling", 900.0F, 1800.0F, "cloudberry", 1.5F, 2,
                new Color(150, 172, 160));
        // Vanilla's own registration flags for a berry bush: no light, NOT
        // item-obtainable (you replant the sapling, not the bush), and the
        // trailing true is what puts it on the object layer that plants use.
        SkyRegistry.cloudberryBushID = ObjectRegistry.registerObject("cloudberrybush",
                cloudberryBush, 0.0F, false, false, true);
        // The sapling. SaplingObject's validTiles default to vanilla
        // grass/dirt/snow -- NONE of which exist in the Skyreach -- so
        // cloudturftile has to be passed explicitly or a player could never
        // replant one where it came from. The mod's tree saplings document the
        // same trap; skywaytile is included so a Skyway garden works too.
        SkyRegistry.cloudberrySaplingID = ObjectRegistry.registerObject("cloudberrysapling",
                new necesse.level.gameObject.SaplingObject("cloudberrysapling",
                        new Color(150, 172, 160), "cloudberrybush", 1200, 2100, false,
                        "cloudturftile", "skywaytile"),
                30.0F, true);

        // Sky islands are small: most tiles border the Mistsea, and right after
        // region generation the liquid height map is still settling, so
        // Level.isShore reports true across fresh islands. Without this flag
        // Region.checkGenerationValid would sweep every crystal/reed away
        // (verified via the skyreachstatus diagnostics).
        allowShore("stormcrystal", "stormcrystalr", "aurorabloom", "aurorabloomr", "skyreeds",
                "windwheat", "cloudberrybush");

        registerLivingSky(skystoneRock);
        registerVeilObjects();
    }

    /** v0.4 "The Living Sky": per-biome trees, plants, meadow grasses, ores. */
    private static void registerLivingSky(RockObject skystoneRock) {
        // --- Trees (vanilla TreeObject: axe, log drops, sapling drops, map icon) ---
        SkyRegistry.nimbuswillowID = ObjectRegistry.registerObject("nimbuswillow",
                new necesse.level.gameObject.TreeObject("nimbuswillow", "nimbuswood", "nimbussapling",
                        new Color(198, 210, 214), 32, 60, 120, "nimbusleaves"),
                0.0F, false, false, true);
        SkyRegistry.fulgurpineID = ObjectRegistry.registerObject("fulgurpine",
                new necesse.level.gameObject.TreeObject("fulgurpine", "charwood", "fulgursapling",
                        new Color(76, 90, 104), 32, 60, 120, "fulgurleaves"),
                0.0F, false, false, true);
        SkyRegistry.prismabirchID = ObjectRegistry.registerObject("prismabirch",
                new necesse.level.gameObject.TreeObject("prismabirch", "prismwood", "prismasapling",
                        new Color(210, 196, 210), 32, 60, 120, "prismaleaves"),
                0.0F, false, false, true);
        // v0.8 Sky Seraph: the one tree whose sheet is a single column with a
        // frost half, because TreeObject's snow column is gated on vanilla's
        // snowID and would never draw here. See SkyTreeObject.
        SkyRegistry.skySeraphTreeID = ObjectRegistry.registerObject("skyseraphtree",
                new stairwaytoheaven.objects.SkyTreeObject("skyseraphtree", "seraphwood",
                        "skyseraphsapling", new Color(224, 118, 10), 32, 60, 120, "seraphleaves",
                        "stormslatetile", "skywaytile"),
                0.0F, false, false, true);
        // The Seraph's own ground is the Skyway paving it generates on, and
        // Cloudturf so a player can carry one home to the Driftlands. Without
        // skywaytile here the biome's own tree could not be replanted in its
        // own biome: TreeSaplingObject refuses to place on anything outside
        // this list.
        // v0.9 Cloud Tree: the Driftlands' own tree, from supplied art on the
        // vanilla birch sheet. Same single-column-with-a-frost-half layout as
        // the Seraph, for the same reason (see SkyTreeObject).
        SkyRegistry.cloudTreeID = ObjectRegistry.registerObject("cloudtree",
                new stairwaytoheaven.objects.SkyTreeObject("cloudtree", "cloudwood",
                        "cloudsapling", new Color(214, 228, 240), 42, 70, 110, "cloudleaves",
                        "stormslatetile", "skywaytile"),
                0.0F, false, false, true);
        SkyRegistry.cloudSaplingID = ObjectRegistry.registerObject("cloudsapling",
                new necesse.level.gameObject.TreeSaplingObject("cloudsapling", new Color(214, 228, 240),
                        "cloudtree", 1800, 2700, true, "cloudturftile"),
                5.0F, true);
        SkyRegistry.skySeraphSaplingID = ObjectRegistry.registerObject("skyseraphsapling",
                new necesse.level.gameObject.TreeSaplingObject("skyseraphsapling", new Color(224, 118, 10),
                        "skyseraphtree", 1800, 2700, true, "skywaytile", "cloudturftile"),
                5.0F, true);
        SkyRegistry.nimbusSaplingID = ObjectRegistry.registerObject("nimbussapling",
                new necesse.level.gameObject.TreeSaplingObject("nimbussapling", new Color(198, 210, 214),
                        "nimbuswillow", 1800, 2700, true, "cloudturftile"),
                5.0F, true);
        SkyRegistry.fulgurSaplingID = ObjectRegistry.registerObject("fulgursapling",
                new necesse.level.gameObject.TreeSaplingObject("fulgursapling", new Color(76, 90, 104),
                        "fulgurpine", 1800, 2700, true, "stormslatetile"),
                5.0F, true);
        SkyRegistry.prismaSaplingID = ObjectRegistry.registerObject("prismasapling",
                new necesse.level.gameObject.TreeSaplingObject("prismasapling", new Color(210, 196, 210),
                        "prismabirch", 1800, 2700, true, "cloudturftile"),
                5.0F, true);

        // --- Plants: pickable flowers and glowing growth ---
        SkyRegistry.cloudbellID = registerPickable("cloudbell", 2, new Color(112, 138, 204), "cloudbell", 1, 2);
        SkyRegistry.skytulipID = registerPickable("skytulip", 3, new Color(226, 130, 162), "skytulip", 1, 1);
        SkyRegistry.thunderbloomID = registerPickable("thunderbloom", 2, new Color(140, 116, 198), "thunderbloom", 1, 1);
        SkyRegistry.glowfernID = registerPickable("glowfern", 2, new Color(104, 172, 156), "glowfern", 1, 2);
        SkyRegistry.auroralilyID = registerPickable("auroralily", 2, new Color(214, 150, 190), "auroralily", 1, 1);
        SkyRegistry.staticmossID = registerPickable("staticmoss", 2, new Color(86, 108, 116), "staticmoss", 1, 2);

        // --- v0.7 stone barrens: what grows on the grey skystone ground ---
        // Measured over three seeds and 235k natural land tiles, that ground
        // carried 0.032 objects per tile against 0.311-0.384 on every other
        // ground in the world, and its whole content was stone blocks. These
        // three are its vegetation and its geology; SkyTerrainPainter.screeAt
        // lays them down as formations, not as a per-tile sprinkle.
        SkyRegistry.skylichenID = registerPickable("skylichen", 3,
                new Color(140, 172, 164), "skylichen", 1, 2);
        SkyRegistry.cragbloomID = registerPickable("cragbloom", 2,
                new Color(108, 134, 112), "cragbloom", 1, 1);
        SkyRegistry.skyscreeID = registerPickable("skyscree", 2,
                new Color(124, 134, 152), "skyscree", 1, 2);

        // --- Dense meadow tall grasses: walk-through carpets (drop nothing,
        // clear on a swing like vanilla tall grass) ---
        SkyRegistry.tallcloudgrassID = registerMeadowGrass("tallcloudgrass", new Color(186, 202, 186));
        SkyRegistry.stormsedgeID = registerMeadowGrass("stormsedge", new Color(96, 110, 128));
        SkyRegistry.prismgrassID = registerMeadowGrass("prismgrass", new Color(206, 190, 214));

        // --- Ores (same RockOreObject mask idiom as aetherium) ---
        SkyRegistry.fulguriteRockID = ObjectRegistry.registerObject("fulguriterock",
                new RockOreObject(skystoneRock, "oremask", "fulguriteore", new Color(222, 196, 140), "fulgurite", SKY_CATEGORY),
                -1.0F, true);
        SkyRegistry.prismshardRockID = ObjectRegistry.registerObject("prismshardrock",
                new RockOreObject(skystoneRock, "oremask", "prismshardore", new Color(186, 156, 214), "prismshard", SKY_CATEGORY),
                -1.0F, true);

        allowShore("nimbuswillow", "fulgurpine", "prismabirch", "skyseraphtree",
                "nimbussapling", "fulgursapling", "prismasapling", "skyseraphsapling",
                "cloudbell", "skytulip", "thunderbloom", "glowfern", "auroralily", "staticmoss",
                "tallcloudgrass", "stormsedge", "prismgrass",
                "fulguriterock", "prismshardrock");
    }

    /** GrassObject variant that drops a material when cleared. */
    private static int registerPickable(String stringID, int variants, Color mapColor,
            String lootItem, int min, int max) {
        final necesse.inventory.lootTable.LootTable loot = new necesse.inventory.lootTable.LootTable(
                necesse.inventory.lootTable.lootItem.LootItem.between(lootItem, min, max));
        GrassObject plant = new GrassObject(stringID, variants) {
            @Override
            public necesse.inventory.lootTable.LootTable getLootTable(
                    necesse.level.maps.Level level, int layerID, int tileX, int tileY) {
                return loot;
            }
        };
        plant.mapColor = mapColor;
        int id = ObjectRegistry.registerObject(stringID, plant, 1.0F, true);
        ObjectRegistry.getObject(id).canPlaceOnShore = true;
        return id;
    }

    /** Walk-through carpet grass: 4 variants, no drops. */
    private static int registerMeadowGrass(String stringID, Color mapColor) {
        GrassObject grass = new GrassObject(stringID, 4);
        grass.mapColor = mapColor;
        int id = ObjectRegistry.registerObject(stringID, grass, 0.0F, false);
        ObjectRegistry.getObject(id).canPlaceOnShore = true;
        return id;
    }

    /** Natural objects of the Veil (v0.3): fen flora, ash bones, dark rock. */
    private static void registerVeilObjects() {
        RockObject veilrock = new RockObject("veilrock", new Color(70, 66, 84), "stone", SKY_CATEGORY);
        SkyRegistry.veilrockID = ObjectRegistry.registerObject("veilrock", veilrock, -1.0F, true);

        GrassObject whisperreeds = new GrassObject("whisperreeds", 4);
        whisperreeds.mapColor = new Color(96, 110, 96);
        SkyRegistry.whisperreedsID = ObjectRegistry.registerObject("whisperreeds", whisperreeds, 1.0F, true);

        // glowing fen shroom: the Veil's natural light source, replantable.
        // Tool behaviour audited against vanilla soft flora (ToolType.ALL,
        // 1 HP) — the GameObject pickaxe default was wrong for a plant.
        stairwaytoheaven.objects.SkyDecoObject gloomshroom = new stairwaytoheaven.objects.SkyDecoObject(
                "gloomshroom", 32, new Color(122, 196, 160), null, "objects", "decorations")
                .setTool(ToolType.ALL).setObjectHealth(1)
                .setLight(70, 0.40F, 0.40F);
        SkyRegistry.gloomshroomID = ObjectRegistry.registerObject("gloomshroom", gloomshroom, 5.0F, true);

        // half-buried ribcage: harvest node for Cinder Pearls (drops no item of
        // itself). Matches the vanilla CowSkeletonObject: ALL, 50 HP.
        stairwaytoheaven.objects.SkyDecoObject ashbones = new stairwaytoheaven.objects.SkyDecoObject(
                "ashbones", 32, new Color(180, 174, 166), null, "objects", "decorations") {
            @Override
            public necesse.inventory.lootTable.LootTable getLootTable(
                    necesse.level.maps.Level level, int layerID, int tileX, int tileY) {
                return ashbonesLoot;
            }
        }.setTool(ToolType.ALL).setObjectHealth(50);
        SkyRegistry.ashbonesID = ObjectRegistry.registerObject("ashbones", ashbones, 0.0F, false);

        // crooked bare tree: woody trunk, so axe like every TreeObject.
        stairwaytoheaven.objects.SkyDecoObject deadtree = new stairwaytoheaven.objects.SkyDecoObject(
                "deadtree", 48, new Color(60, 52, 58), null, "objects", "decorations")
                .setTool(ToolType.AXE);
        SkyRegistry.deadtreeID = ObjectRegistry.registerObject("deadtree", deadtree, 4.0F, true);

        allowShore("veilrock", "whisperreeds", "gloomshroom", "ashbones", "deadtree");
    }

    static final necesse.inventory.lootTable.LootTable ashbonesLoot =
            new necesse.inventory.lootTable.LootTable(
                    necesse.inventory.lootTable.lootItem.LootItem.between("cinderpearl", 1, 1));


    private static void allowShore(String... objectStringIDs) {
        for (String stringID : objectStringIDs) {
            ObjectRegistry.getObject(ObjectRegistry.getObjectID(stringID)).canPlaceOnShore = true;
        }
    }
}
