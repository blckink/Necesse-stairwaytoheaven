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
    }

    static final necesse.inventory.lootTable.LootTable cloudberryLoot =
            new necesse.inventory.lootTable.LootTable(
                    necesse.inventory.lootTable.lootItem.LootItem.between("cloudberry", 1, 2));

    private static void allowShore(String... objectStringIDs) {
        for (String stringID : objectStringIDs) {
            ObjectRegistry.getObject(ObjectRegistry.getObjectID(stringID)).canPlaceOnShore = true;
        }
    }
}
