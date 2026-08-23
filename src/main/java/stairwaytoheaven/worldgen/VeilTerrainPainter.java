package stairwaytoheaven.worldgen;

import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyRegistry;

/**
 * Per-region terrain painter of the Veil: broad marsh landmasses over black
 * murkwater, two sub-biomes (Gloomfen common, Ashen Reach uncommon), calm
 * object density with glowing shrooms as the natural light source.
 */
public final class VeilTerrainPainter {

    public static final int ISLAND_SCALE = 46;
    public static final float ISLAND_THRESHOLD = 0.50F;
    public static final int BIOME_SCALE = 150;
    public static final float ASHEN_BELOW = 0.30F;
    public static final int PATCH_SCALE = 20;
    public static final float PATCH_THRESHOLD = 0.66F;
    public static final int SALT_BIOME = 5;
    public static final int SALT_PATCH = 9;
    public static final int SALT_OBJECT_ROLL = 13;

    private VeilTerrainPainter() {
    }

    public static void paintRegion(Region region, int seed) {
        int tileWidth = region.tileLayer.region.tileWidth;
        int tileHeight = region.tileLayer.region.tileHeight;
        for (int rx = 0; rx < tileWidth; rx++) {
            for (int ry = 0; ry < tileHeight; ry++) {
                int tileX = rx + region.tileXOffset;
                int tileY = ry + region.tileYOffset;

                float biomeValue = SkyNoise.fbm(seed + SALT_BIOME, tileX, tileY, BIOME_SCALE, 2);
                boolean isAshen = biomeValue < ASHEN_BELOW;
                region.biomeLayer.setBiomeByRegion(rx, ry,
                        (isAshen ? SkyRegistry.ashenReach : SkyRegistry.gloomfen).getID());

                float islandValue = SkyNoise.fbm(seed, tileX, tileY, ISLAND_SCALE, 3);
                if (islandValue <= ISLAND_THRESHOLD) {
                    region.tileLayer.setTileByRegion(rx, ry, SkyRegistry.murkwaterID);
                    continue;
                }
                boolean isPatch = SkyNoise.fbm(seed + SALT_PATCH, tileX, tileY, PATCH_SCALE, 2) > PATCH_THRESHOLD;
                int tileID;
                if (isAshen) {
                    tileID = isPatch ? SkyRegistry.blackpeatID : SkyRegistry.ashsandID;
                } else {
                    tileID = isPatch ? SkyRegistry.blackpeatID : SkyRegistry.murkmossID;
                }
                region.tileLayer.setTileByRegion(rx, ry, tileID);

                if (islandValue <= ISLAND_THRESHOLD + 0.02F) {
                    continue; // keep shorelines walkable
                }
                int objectID = rollObject(seed, tileX, tileY, isAshen, isPatch);
                if (objectID != 0) {
                    region.objectLayer.setObjectByRegion(
                            necesse.engine.registries.ObjectLayerRegistry.BASE_LAYER, rx, ry, objectID);
                }
            }
        }
    }

    public static int rollObject(int seed, int tileX, int tileY, boolean isAshen, boolean isPatch) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_OBJECT_ROLL);
        if (isAshen) {
            if (roll < 0.085F) return SkyRegistry.veilrockID;
            if (roll < 0.105F) return SkyRegistry.ashbonesID;
            if (roll < 0.112F) return SkyRegistry.gloomshroomID;
            return 0;
        }
        if (roll < 0.085F) {
            return isPatch ? SkyRegistry.veilrockID : SkyRegistry.whisperreedsID;
        }
        if (roll < 0.110F) return SkyRegistry.veilrockID;
        if (roll < 0.132F) return SkyRegistry.gloomshroomID;
        if (roll < 0.143F) return SkyRegistry.deadtreeID;
        if (roll < 0.147F) return SkyRegistry.ashbonesID;
        return 0;
    }
}
