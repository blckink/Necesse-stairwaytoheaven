package stairwaytoheaven;

import java.awt.Color;

import necesse.engine.registries.ObjectRegistry;
import necesse.level.gameObject.CrystalClusterObject;
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

        GrassObject cloudberryBush = new GrassObject("cloudberrybush", 2) {
            @Override
            public necesse.inventory.lootTable.LootTable getLootTable(
                    necesse.level.maps.Level level, int layerID, int tileX, int tileY) {
                return cloudberryLoot;
            }
        };
        cloudberryBush.mapColor = new Color(150, 172, 160);
        SkyRegistry.cloudberryBushID = ObjectRegistry.registerObject("cloudberrybush", cloudberryBush, 1.0F, true);

        // Sky islands are small: most tiles border the Mistsea, and right after
        // region generation the liquid height map is still settling, so
        // Level.isShore reports true across fresh islands. Without this flag
        // Region.checkGenerationValid would sweep every crystal/reed away
        // (verified via the skyreachstatus diagnostics).
        allowShore("stormcrystal", "stormcrystalr", "aurorabloom", "aurorabloomr", "skyreeds",
                "windwheat", "cloudberrybush");

        registerVeilObjects();
    }

    /** Natural objects of the Veil (v0.3): fen flora, ash bones, dark rock. */
    private static void registerVeilObjects() {
        RockObject veilrock = new RockObject("veilrock", new Color(70, 66, 84), "stone", SKY_CATEGORY);
        SkyRegistry.veilrockID = ObjectRegistry.registerObject("veilrock", veilrock, -1.0F, true);

        GrassObject whisperreeds = new GrassObject("whisperreeds", 4);
        whisperreeds.mapColor = new Color(96, 110, 96);
        SkyRegistry.whisperreedsID = ObjectRegistry.registerObject("whisperreeds", whisperreeds, 1.0F, true);

        // glowing fen shroom: the Veil's natural light source, replantable
        stairwaytoheaven.objects.SkyDecoObject gloomshroom = new stairwaytoheaven.objects.SkyDecoObject(
                "gloomshroom", 32, new Color(122, 196, 160), null, "objects", "decorations")
                .setLight(70, 0.40F, 0.40F);
        SkyRegistry.gloomshroomID = ObjectRegistry.registerObject("gloomshroom", gloomshroom, 5.0F, true);

        // half-buried ribcage: harvest node for Cinder Pearls (drops no item of itself)
        stairwaytoheaven.objects.SkyDecoObject ashbones = new stairwaytoheaven.objects.SkyDecoObject(
                "ashbones", 32, new Color(180, 174, 166), null, "objects", "decorations") {
            @Override
            public necesse.inventory.lootTable.LootTable getLootTable(
                    necesse.level.maps.Level level, int layerID, int tileX, int tileY) {
                return ashbonesLoot;
            }
        };
        SkyRegistry.ashbonesID = ObjectRegistry.registerObject("ashbones", ashbones, 0.0F, false);

        stairwaytoheaven.objects.SkyDecoObject deadtree = new stairwaytoheaven.objects.SkyDecoObject(
                "deadtree", 48, new Color(60, 52, 58), null, "objects", "decorations");
        SkyRegistry.deadtreeID = ObjectRegistry.registerObject("deadtree", deadtree, 4.0F, true);

        allowShore("veilrock", "whisperreeds", "gloomshroom", "ashbones", "deadtree");
    }

    static final necesse.inventory.lootTable.LootTable ashbonesLoot =
            new necesse.inventory.lootTable.LootTable(
                    necesse.inventory.lootTable.lootItem.LootItem.between("cinderpearl", 1, 1));

    static final necesse.inventory.lootTable.LootTable cloudberryLoot =
            new necesse.inventory.lootTable.LootTable(
                    necesse.inventory.lootTable.lootItem.LootItem.between("cloudberry", 1, 2));

    private static void allowShore(String... objectStringIDs) {
        for (String stringID : objectStringIDs) {
            ObjectRegistry.getObject(ObjectRegistry.getObjectID(stringID)).canPlaceOnShore = true;
        }
    }
}
