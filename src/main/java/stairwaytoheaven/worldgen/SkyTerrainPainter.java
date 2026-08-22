package stairwaytoheaven.worldgen;

import necesse.engine.registries.ObjectLayerRegistry;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyRegistry;

/**
 * Paints one region of the Skyreach: floating islands over the Mistsea, with the
 * three sky sub-biomes written into the region's biome layer — the exact same
 * per-tile mechanism vanilla cave levels use, so spawn tables, music and crate
 * loot all resolve through {@code Level.getBiome(tileX, tileY)}.
 *
 * All shapes derive from {@link SkyNoise}, which is a pure function of the world
 * seed and tile coordinates: region borders are seamless and generation is fully
 * deterministic.
 */
public final class SkyTerrainPainter {

    // Noise field salts (independent noise layers)
    public static final long SALT_BIOME = 0x1B903;
    public static final long SALT_ROCK_PATCH = 0x2C511;
    public static final int SALT_OBJECT_ROLL = 7;

    // Island shape
    public static final float ISLAND_SCALE = 42.0F;
    public static final float ISLAND_THRESHOLD = 0.55F;
    /** Rim band above the threshold that is kept clear of objects, so island edges stay walkable. */
    public static final float ISLAND_RIM = 0.020F;

    // Sub-biome distribution (low-frequency mask)
    public static final float BIOME_SCALE = 170.0F;
    public static final float STORMVEIL_BELOW = 0.40F;
    public static final float AURORA_ABOVE = 0.72F;

    // Skystone outcrop patches on island interiors
    public static final float ROCK_PATCH_SCALE = 22.0F;
    public static final float ROCK_PATCH_THRESHOLD = 0.67F;

    private SkyTerrainPainter() {
    }

    public static void paintRegion(Region region, int seed) {
        int tileWidth = region.tileLayer.region.tileWidth;
        int tileHeight = region.tileLayer.region.tileHeight;
        // Tiles already claimed by the second half of a 2x1 crystal cluster
        boolean[][] reserved = new boolean[tileWidth][tileHeight];
        for (int regionTileX = 0; regionTileX < tileWidth; regionTileX++) {
            for (int regionTileY = 0; regionTileY < tileHeight; regionTileY++) {
                int tileX = regionTileX + region.tileXOffset;
                int tileY = regionTileY + region.tileYOffset;

                float biomeValue = SkyNoise.fbm(seed + SALT_BIOME, tileX, tileY, BIOME_SCALE, 2);
                boolean isStormveil = biomeValue < STORMVEIL_BELOW;
                boolean isAurora = biomeValue > AURORA_ABOVE;

                int biomeID;
                if (isStormveil) {
                    biomeID = SkyRegistry.stormveil.getID();
                } else if (isAurora) {
                    biomeID = SkyRegistry.auroraShoals.getID();
                } else {
                    biomeID = SkyRegistry.driftlands.getID();
                }
                region.biomeLayer.setBiomeByRegion(regionTileX, regionTileY, biomeID);

                float islandValue = SkyNoise.fbm(seed, tileX, tileY, ISLAND_SCALE, 3);
                if (islandValue <= ISLAND_THRESHOLD) {
                    region.tileLayer.setTileByRegion(regionTileX, regionTileY, SkyRegistry.mistseaID);
                    continue;
                }

                boolean isRockPatch = SkyNoise.fbm(seed + SALT_ROCK_PATCH, tileX, tileY, ROCK_PATCH_SCALE, 2) > ROCK_PATCH_THRESHOLD;
                int groundID;
                if (isRockPatch) {
                    groundID = SkyRegistry.skystoneTileID;
                } else if (isStormveil) {
                    groundID = SkyRegistry.stormslateID;
                } else {
                    groundID = SkyRegistry.cloudturfID;
                }
                region.tileLayer.setTileByRegion(regionTileX, regionTileY, groundID);

                // Keep island rims object-free so coastlines stay walkable
                if (islandValue <= ISLAND_THRESHOLD + ISLAND_RIM || reserved[regionTileX][regionTileY]) {
                    continue;
                }

                int objectID = rollObject(seed, tileX, tileY, isStormveil, isAurora, isRockPatch);
                if (objectID == 0) {
                    continue;
                }
                if (objectID == SkyRegistry.stormCrystalID || objectID == SkyRegistry.auroraBloomID) {
                    // Crystal clusters are 2x1 multi-tile objects: base + "r"
                    // counterpart on the tile to the right. Both halves must be
                    // written, or Region.checkGenerationValid removes the torso.
                    int counterID = objectID == SkyRegistry.stormCrystalID
                            ? SkyRegistry.stormCrystalRID
                            : SkyRegistry.auroraBloomRID;
                    if (regionTileX + 1 < tileWidth
                            && SkyNoise.fbm(seed, tileX + 1, tileY, ISLAND_SCALE, 3) > ISLAND_THRESHOLD + ISLAND_RIM) {
                        region.objectLayer.setObjectByRegion(ObjectLayerRegistry.BASE_LAYER, regionTileX, regionTileY, objectID);
                        region.objectLayer.setObjectByRegion(ObjectLayerRegistry.BASE_LAYER, regionTileX + 1, regionTileY, counterID);
                        reserved[regionTileX + 1][regionTileY] = true;
                    }
                    continue;
                }
                region.objectLayer.setObjectByRegion(ObjectLayerRegistry.BASE_LAYER, regionTileX, regionTileY, objectID);
            }
        }
    }

    /**
     * Picks the natural object for a land tile, or 0 for none. Chances are exclusive
     * bands of a single per-tile roll, so total density stays easy to reason about.
     */
    public static int rollObject(int seed, int tileX, int tileY, boolean isStormveil, boolean isAurora, boolean isRockPatch) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_OBJECT_ROLL);
        if (isStormveil) {
            if (roll < 0.022F) return SkyRegistry.stormCrystalID;
            if (roll < 0.055F) return SkyRegistry.skystoneRockID;
            if (roll < 0.063F) return SkyRegistry.aetheriumRockID;
            return 0;
        }
        if (isAurora) {
            if (roll < 0.026F) return SkyRegistry.auroraBloomID;
            if (roll < 0.048F) return SkyRegistry.aetheriumRockID;
            if (roll < 0.080F) return SkyRegistry.skystoneRockID;
            return 0;
        }
        // Driftlands: reeds prefer soft ground, rocks prefer outcrops
        if (roll < 0.055F) {
            return isRockPatch ? SkyRegistry.skystoneRockID : SkyRegistry.skyreedsID;
        }
        if (roll < 0.085F) return SkyRegistry.skystoneRockID;
        if (roll < 0.093F) return SkyRegistry.aetheriumRockID;
        return 0;
    }
}
