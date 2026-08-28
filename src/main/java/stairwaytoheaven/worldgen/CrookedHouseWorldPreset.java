package stairwaytoheaven.worldgen;

import java.awt.Dimension;
import java.awt.Point;

import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.util.GameRandom;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.WorldPreset;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.VeilLevel;

/**
 * Scatters the {@link CrookedHousePreset} through the Beetlefreak Hollows.
 *
 * <h2>Why this is a bare {@code WorldPreset} and not a {@code SimpleGenerationPreset}</h2>
 * The tidier-looking route would be a {@code SimpleGenerationPreset} added to a
 * {@code GenerationPresetsWorldPreset}, the way vanilla's surface structures
 * are registered. It does not work here, and the reason is worth writing down.
 *
 * That path weights each entry by {@code LevelPresetsRegion.biomeIDWeights},
 * and those weights are sampled from
 * {@code worldEntity.getGeneratorStack().getLazyBiomeID(...)}
 * (LevelPresetsRegion.java:62-116) — the VANILLA biome generator. Our Veil
 * biomes are painted per tile by {@link VeilTerrainPainter} into the region's
 * biome layer and never pass through that generator, so a preset scoped to
 * {@code beetlefreakHollow} would score a biome weight of 0, be clamped to a
 * single ticket against the entry's thousands, and effectively never place.
 * Vanilla's own biome-independent structures ({@code SpiderNestsWorldPreset},
 * {@code VampireCryptWorldPreset}) extend {@code WorldPreset} directly for the
 * same reason, so that is what this does.
 *
 * <h2>How a site is chosen</h2>
 * {@link VeilTerrainPainter} is pure noise over the world seed, so this asks it
 * the same questions the painter will answer when the region is actually
 * painted: every corner and the centre of the footprint must be dry land
 * inside a Hollow. No world state is consulted, which is what makes the choice
 * stable across a save/load and identical on every client.
 */
public class CrookedHouseWorldPreset extends WorldPreset {

    /**
     * Houses per 16x16 region, and tries per house.
     *
     * Both are derived from a measurement rather than picked. With the Hollows
     * at their shipped rarity, a 15x13 footprint whose four corners AND centre
     * all land inside one fits on 0.129% of tiles (swept offline over 2.0M
     * tiles). So a single random try almost always misses, and the number of
     * tries is what actually sets the rate: at 400 tries a house point converts
     * about 40% of the time. 0.015 points per region then works out to roughly
     * one house per 60-70 regions -- rare enough to be a find, common enough
     * that a player who explores the Veil will meet one.
     *
     * Note the unit trap: {@code getTotalPoints} divides the WORLD-PRESET
     * region (64x64 level regions = 1024x1024 tiles) by 256, so this figure is
     * multiplied by ~4096 per block, not by 1.
     */
    public static final float HOUSES_PER_REGION = 0.015F;

    /** Tries per house point before giving up. See above -- this is the dial. */
    private static final int SITE_ATTEMPTS = 400;

    /** Vanilla's shared "do not overlap" board for structures. */
    private static final String OCCUPIED_BOARD = "villages";

    @Override
    public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
        // Identifier only. hasAnyOfBiome() would consult the vanilla biome
        // weights, which never know about our painted biomes (see the class
        // comment), so it would always answer false.
        return presetsRegion.identifier.equals(SkyRegistry.VEIL_IDENTIFIER);
    }

    /**
     * Every corner plus the centre of the footprint must be dry Hollow ground.
     * Corners alone would happily straddle a channel; the centre alone would
     * let three quarters of the house hang over water.
     */
    public boolean isValidSite(int seed, int tileX, int tileY, int width, int height) {
        int x1 = tileX + width - 1;
        int y1 = tileY + height - 1;
        int cx = tileX + width / 2;
        int cy = tileY + height / 2;
        return good(seed, tileX, tileY)
                && good(seed, x1, tileY)
                && good(seed, tileX, y1)
                && good(seed, x1, y1)
                && good(seed, cx, cy);
    }

    private static boolean good(int seed, int tileX, int tileY) {
        return VeilTerrainPainter.isLand(seed, tileX, tileY)
                && VeilTerrainPainter.isHollow(seed, tileX, tileY);
    }

    @Override
    public void addToRegion(GameRandom random, final LevelPresetsRegion presetsRegion,
                            BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer) {
        final int seed = VeilLevel.worldGenSeed(presetsRegion.worldRegion.worldEntity.worldSeed);
        final Dimension size = new Dimension(CrookedHousePreset.WIDTH, CrookedHousePreset.HEIGHT);
        int total = getTotalPoints(random, presetsRegion, HOUSES_PER_REGION);

        for (int i = 0; i < total; i++) {
            Point tile = findRandomPresetTile(random, presetsRegion, SITE_ATTEMPTS, size, OCCUPIED_BOARD,
                    (tileX, tileY) -> isValidSite(seed, tileX, tileY, size.width, size.height));
            if (tile == null) {
                continue;
            }
            presetsRegion.addPreset(this, tile.x, tile.y, size, OCCUPIED_BOARD,
                    new LevelPresetsRegion.WorldPresetPlaceFunction() {
                        @Override
                        public void place(GameRandom placeRandom, Level level, PerformanceTimerManager timer) {
                            new CrookedHousePreset(placeRandom).applyToLevel(level, tile.x, tile.y);
                        }
                    });
        }
    }
}
