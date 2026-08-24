package stairwaytoheaven.worldgen;

import necesse.engine.registries.ObjectLayerRegistry;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyRegistry;

import java.awt.Point;

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

    /**
     * Radius around the canonical spire origin kept as an open plaza: natural
     * object placement (meadows, trees, ores) is suppressed here so the hero
     * landmark's silhouette reads clean on first arrival. Slightly larger than
     * the 15x15 preset footprint plus its props.
     */
    public static final float SPIRE_GROUNDS_RADIUS = 18.0F;

    private SkyTerrainPainter() {
    }

    public static void paintRegion(Region region, int seed) {
        int tileWidth = region.tileLayer.region.tileWidth;
        int tileHeight = region.tileLayer.region.tileHeight;
        // The canonical sky origin (Old Warden Spire hub) — computed once per
        // region; every radial rule below derives from it.
        Point origin = SkyOrigin.compute(seed);
        // Tiles already claimed by the second half of a 2x1 crystal cluster
        boolean[][] reserved = new boolean[tileWidth][tileHeight];
        for (int regionTileX = 0; regionTileX < tileWidth; regionTileX++) {
            for (int regionTileY = 0; regionTileY < tileHeight; regionTileY++) {
                int tileX = regionTileX + region.tileXOffset;
                int tileY = regionTileY + region.tileYOffset;

                float biomeValue = SkyNoise.fbm(seed + SALT_BIOME, tileX, tileY, BIOME_SCALE, 2);

                // The hub guarantee: around the Old Warden Spire the island
                // mask is clamped to solid land and the biome pulled into the
                // Driftlands band, so the first ascent always lands on a
                // walkable, safe, recognizable home island — regardless of
                // what the raw noise wanted there.
                float hubDx = tileX - origin.x;
                float hubDy = tileY - origin.y;
                float hubDist = (float) Math.sqrt(hubDx * hubDx + hubDy * hubDy);
                if (hubDist < SkyOrigin.HUB_RADIUS) {
                    float force = 1.0F - hubDist / SkyOrigin.HUB_RADIUS; // 1 center → 0 rim
                    biomeValue = biomeValue + (0.5F - biomeValue) * Math.min(1.0F, force * 1.6F);
                }

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
                if (hubDist < SkyOrigin.HUB_RADIUS) {
                    float force = 1.0F - hubDist / SkyOrigin.HUB_RADIUS;
                    islandValue = Math.max(islandValue, ISLAND_THRESHOLD + 0.02F + 0.20F * force);
                }
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

                // Spire grounds: the immediate surroundings of the Old Warden
                // Spire stay an open plaza — natural objects (grass carpets,
                // trees, ores) are suppressed so nothing buries or crowds the
                // landmark's silhouette on first arrival. The preset's own
                // props (lanterns, banners, willows) stamp afterwards.
                if (hubDist < SPIRE_GROUNDS_RADIUS) {
                    continue;
                }

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

                int band = SkyOrigin.bandFor(hubDist);
                int objectID = rollObject(seed, tileX, tileY, isStormveil, isAurora, isRockPatch, band);
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
     *
     * v0.5 radial progression: the ore bands (aetherium, fulgurite, prismshard)
     * widen with the distance band from the Old Warden Spire — the outer reaches
     * pay better, so exploring outward is intrinsically rewarded. All other band
     * widths are unchanged.
     */
    public static int rollObject(int seed, int tileX, int tileY, boolean isStormveil, boolean isAurora,
                                 boolean isRockPatch, int band) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_OBJECT_ROLL);
        float oreMul = 1.0F + 0.55F * band;
        if (isStormveil) {
            if (roll < 0.035F) return SkyRegistry.stormCrystalID;
            if (roll < 0.090F) return SkyRegistry.skystoneRockID;
            float aeth = 0.090F + 0.012F * oreMul;
            if (roll < aeth) return SkyRegistry.aetheriumRockID;
            if (roll < aeth + 0.012F) return isRockPatch ? 0 : SkyRegistry.fulgurpineID;
            float fulg = aeth + 0.012F + 0.010F * oreMul;
            if (roll < fulg) return SkyRegistry.fulguriteRockID;
            if (roll < fulg + 0.028F) return SkyRegistry.staticmossID;
            if (roll < fulg + 0.040F) return isRockPatch ? 0 : SkyRegistry.thunderbloomID;
            return 0;
        }
        if (isAurora) {
            if (roll < 0.042F) return SkyRegistry.auroraBloomID;
            float aeth = 0.042F + 0.030F * oreMul;
            if (roll < aeth) return SkyRegistry.aetheriumRockID;
            if (roll < aeth + 0.056F) return SkyRegistry.skystoneRockID;
            if (roll < aeth + 0.068F) return isRockPatch ? 0 : SkyRegistry.prismabirchID;
            float prism = aeth + 0.068F + 0.010F * oreMul;
            if (roll < prism) return SkyRegistry.prismshardRockID;
            if (roll < prism + 0.028F) return isRockPatch ? 0 : SkyRegistry.glowfernID;
            if (roll < prism + 0.042F) return isRockPatch ? 0 : SkyRegistry.auroralilyID;
            return 0;
        }
        // Driftlands: grasses prefer soft ground, rocks prefer outcrops
        if (roll < 0.090F) {
            return isRockPatch ? SkyRegistry.skystoneRockID : SkyRegistry.skyreedsID;
        }
        if (roll < 0.135F) return SkyRegistry.skystoneRockID;
        float aeth = 0.135F + 0.010F * oreMul;
        if (roll < aeth) return SkyRegistry.aetheriumRockID;
        // v0.2.6 forage: generous wheat-grass, occasional berry bush
        if (roll < aeth + 0.055F) {
            return isRockPatch ? 0 : SkyRegistry.windwheatID;
        }
        if (roll < aeth + 0.067F) {
            return isRockPatch ? 0 : SkyRegistry.cloudberryBushID;
        }
        // v0.4: nimbus willows and meadow flowers
        if (roll < aeth + 0.081F) {
            return isRockPatch ? 0 : SkyRegistry.nimbuswillowID;
        }
        if (roll < aeth + 0.101F) {
            return isRockPatch ? 0 : SkyRegistry.cloudbellID;
        }
        if (roll < aeth + 0.115F) {
            return isRockPatch ? 0 : SkyRegistry.skytulipID;
        }
        return 0;
    }
}
