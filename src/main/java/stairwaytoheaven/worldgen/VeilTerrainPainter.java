package stairwaytoheaven.worldgen;

import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyRegistry;

/**
 * Per-region terrain painter of the Veil: broad marsh landmasses over black
 * murkwater, two sub-biomes (Gloomfen common, Ashen Reach uncommon), calm
 * object density with glowing shrooms as the natural light source.
 */
public final class VeilTerrainPainter {

    public static final int ISLAND_SCALE = 56;
    public static final float ISLAND_THRESHOLD = 0.44F;
    public static final int BIOME_SCALE = 150;
    public static final float ASHEN_BELOW = 0.30F;
    public static final int PATCH_SCALE = 20;
    public static final float PATCH_THRESHOLD = 0.66F;
    public static final int SALT_BIOME = 5;
    public static final int SALT_PATCH = 9;
    public static final int SALT_OBJECT_ROLL = 13;

    /**
     * Beetlefreak Hollows: a THIRD band, cut out of the other two rather than
     * tiled beside them.
     *
     * A small scale (39) makes the patches compact instead of continental, and
     * a high threshold makes them rare -- the fen must stay the Veil's normal
     * state or the wrongness stops reading as wrong.
     *
     * The threshold is MEASURED, not guessed. 0.615 was the first guess and it
     * painted 23.9% of walkable ground (the veilstatus survey said so, which is
     * why that survey exists): a quarter of the layer is not a rare wrong place,
     * it is a third landscape. Sweeping the same noise offline over 2.0M land
     * tiles gave 0.660 -> 18.0%, 0.700 -> 11.8%, 0.740 -> 6.9%, 0.780 -> 3.6%,
     * 0.820 -> 1.6%. 0.780 is the value that matches the intent.
     */
    public static final int HOLLOW_SCALE = 39;
    public static final float HOLLOW_THRESHOLD = 0.78F;
    public static final int SALT_HOLLOW = 21;
    public static final int SALT_HOLLOW_OBJECT = 23;

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
                boolean isHollow = isHollow(seed, tileX, tileY);
                region.biomeLayer.setBiomeByRegion(rx, ry, isHollow
                        ? SkyRegistry.beetlefreakHollow.getID()
                        : (isAshen ? SkyRegistry.ashenReach : SkyRegistry.gloomfen).getID());

                float islandValue = SkyNoise.fbm(seed, tileX, tileY, ISLAND_SCALE, 3);
                if (islandValue <= ISLAND_THRESHOLD) {
                    region.tileLayer.setTileByRegion(rx, ry, SkyRegistry.murkwaterID);
                    continue;
                }
                boolean isPatch = SkyNoise.fbm(seed + SALT_PATCH, tileX, tileY, PATCH_SCALE, 2) > PATCH_THRESHOLD;
                int tileID;
                if (isHollow) {
                    // Blackpeat still shows through: the striped ground reads
                    // far louder with something plain interrupting it, and the
                    // patch noise is what keeps the edge from being a circle.
                    tileID = isPatch ? SkyRegistry.blackpeatID : SkyRegistry.beetlefreakID;
                } else if (isAshen) {
                    tileID = isPatch ? SkyRegistry.blackpeatID : SkyRegistry.ashsandID;
                } else {
                    tileID = isPatch ? SkyRegistry.blackpeatID : SkyRegistry.murkmossID;
                }
                region.tileLayer.setTileByRegion(rx, ry, tileID);

                if (islandValue <= ISLAND_THRESHOLD + 0.02F) {
                    continue; // keep shorelines walkable
                }
                int objectID = isHollow
                        ? rollHollowObject(seed, tileX, tileY)
                        : rollObject(seed, tileX, tileY, isAshen, isPatch);
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

    /**
     * Is this tile inside a Beetlefreak Hollow? Pure noise, no world state, so
     * the Crooked House placement test can ask the same question the painter
     * will answer later and get the same answer.
     */
    public static boolean isHollow(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + SALT_HOLLOW, tileX, tileY, HOLLOW_SCALE, 2) > HOLLOW_THRESHOLD;
    }

    /** Is this tile dry land? Same question, same reason. */
    public static boolean isLand(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed, tileX, tileY, ISLAND_SCALE, 3) > ISLAND_THRESHOLD + 0.02F;
    }

    /**
     * The Hollows' own prop mix: sparser than the fen and made of the wrong
     * things. Dead trees and bones cluster here; the reeds and the healthy
     * shrooms do not grow in it at all.
     */
    public static int rollHollowObject(int seed, int tileX, int tileY) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_HOLLOW_OBJECT);
        if (roll < 0.055F) return SkyRegistry.deadtreeID;
        if (roll < 0.085F) return SkyRegistry.ashbonesID;
        if (roll < 0.100F) return SkyRegistry.veilrockID;
        if (roll < 0.108F) return SkyRegistry.gloomshroomID;
        return 0;
    }
}
