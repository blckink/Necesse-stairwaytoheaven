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
    // v0.3.1: bigger, more frequent landmasses after playtests (the sky read
    // as mostly Mistsea and travel was all swimming)
    public static final float ISLAND_SCALE = 54.0F;
    public static final float ISLAND_THRESHOLD = 0.48F;
    /** Rim band above the threshold that is kept clear of objects, so island edges stay walkable. */
    public static final float ISLAND_RIM = 0.020F;

    // Sub-biome distribution (low-frequency mask)
    public static final float BIOME_SCALE = 170.0F;
    public static final float STORMVEIL_BELOW = 0.40F;
    public static final float AURORA_ABOVE = 0.72F;

    // Skystone outcrop patches on island interiors
    public static final float ROCK_PATCH_SCALE = 22.0F;
    public static final float ROCK_PATCH_THRESHOLD = 0.67F;

    // v0.4 meadow carpets: low-frequency patches where walk-through tall
    // grass covers most of the ground (vanilla lush-area density), layered
    // over the sparse per-tile rolls that fill the rest of the island.
    public static final long SALT_MEADOW = 0x4D0E;
    public static final int SALT_MEADOW_ROLL = 11;
    public static final float MEADOW_SCALE = 48.0F;
    public static final float MEADOW_THRESHOLD = 0.60F;
    public static final float MEADOW_DENSITY = 0.72F;

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

                // Meadow carpets: inside a meadow patch, dense walk-through
                // tall grass takes the tile before the sparse rolls run.
                if (!isRockPatch
                        && SkyNoise.fbm(seed + SALT_MEADOW, tileX, tileY, MEADOW_SCALE, 2) > MEADOW_THRESHOLD
                        && SkyNoise.tileRoll(seed, tileX, tileY, SALT_MEADOW_ROLL) < MEADOW_DENSITY) {
                    int meadowGrassID = isStormveil ? SkyRegistry.stormsedgeID
                            : (isAurora ? SkyRegistry.prismgrassID : SkyRegistry.tallcloudgrassID);
                    region.objectLayer.setObjectByRegion(ObjectLayerRegistry.BASE_LAYER, regionTileX, regionTileY, meadowGrassID);
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
        // Densities tuned up ~1.6x after playtests: the sky read as bare next to
        // vanilla biomes. Grass-likes are generous (walk-through), blockers moderate.
        if (isStormveil) {
            if (roll < 0.035F) return SkyRegistry.stormCrystalID;
            if (roll < 0.090F) return SkyRegistry.skystoneRockID;
            if (roll < 0.102F) return SkyRegistry.aetheriumRockID;
            // v0.4: charred pines, fulgurite glass, sparking growth
            if (roll < 0.114F) return isRockPatch ? 0 : SkyRegistry.fulgurpineID;
            if (roll < 0.124F) return SkyRegistry.fulguriteRockID;
            if (roll < 0.152F) return SkyRegistry.staticmossID;
            if (roll < 0.164F) return isRockPatch ? 0 : SkyRegistry.thunderbloomID;
            return 0;
        }
        if (isAurora) {
            if (roll < 0.042F) return SkyRegistry.auroraBloomID;
            if (roll < 0.072F) return SkyRegistry.aetheriumRockID;
            if (roll < 0.128F) return SkyRegistry.skystoneRockID;
            // v0.4: prisma birches, prismshard veins, glowing undergrowth
            if (roll < 0.140F) return isRockPatch ? 0 : SkyRegistry.prismabirchID;
            if (roll < 0.150F) return SkyRegistry.prismshardRockID;
            if (roll < 0.178F) return isRockPatch ? 0 : SkyRegistry.glowfernID;
            if (roll < 0.192F) return isRockPatch ? 0 : SkyRegistry.auroralilyID;
            return 0;
        }
        // Driftlands: grasses prefer soft ground, rocks prefer outcrops
        if (roll < 0.090F) {
            return isRockPatch ? SkyRegistry.skystoneRockID : SkyRegistry.skyreedsID;
        }
        if (roll < 0.135F) return SkyRegistry.skystoneRockID;
        if (roll < 0.145F) return SkyRegistry.aetheriumRockID;
        // v0.2.6 forage: generous wheat-grass, occasional berry bush
        if (roll < 0.200F) {
            return isRockPatch ? 0 : SkyRegistry.windwheatID;
        }
        if (roll < 0.212F) {
            return isRockPatch ? 0 : SkyRegistry.cloudberryBushID;
        }
        // v0.4: nimbus willows and meadow flowers
        if (roll < 0.226F) {
            return isRockPatch ? 0 : SkyRegistry.nimbuswillowID;
        }
        if (roll < 0.246F) {
            return isRockPatch ? 0 : SkyRegistry.cloudbellID;
        }
        if (roll < 0.260F) {
            return isRockPatch ? 0 : SkyRegistry.skytulipID;
        }
        return 0;
    }
}
