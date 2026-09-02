package stairwaytoheaven.realms.crooked;

import java.awt.Dimension;

import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.util.GameRandom;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.WorldPreset;
import necesse.level.maps.Level;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.SkyNoise;

/**
 * Stamps Crooked Beyond's three buildings onto the lattice
 * {@link CrookedSites} defines.
 *
 * <h2>Why a bare {@code WorldPreset} and not a {@code SimpleGenerationPreset}</h2>
 * The same reason {@link stairwaytoheaven.worldgen.CrookedHouseWorldPreset}
 * records, and it is worth not rediscovering: the tidier route weights each
 * entry by {@code LevelPresetsRegion.biomeIDWeights}, and those weights are
 * sampled from {@code worldEntity.getGeneratorStack().getLazyBiomeID(...)}
 * (LevelPresetsRegion.java:62-116) — the VANILLA biome generator. This realm's
 * biomes are painted per tile by {@link CrookedTerrainPainter} into the region's
 * biome layer and never pass through that generator, so a preset scoped to one
 * of them would score a biome weight of 0, be clamped to a single ticket against
 * the entry's thousands, and effectively never place. Vanilla's own
 * biome-independent structures ({@code SpiderNestsWorldPreset},
 * {@code VampireCryptWorldPreset}) extend {@code WorldPreset} directly for the
 * same reason.
 *
 * <h2>Why the sites are a lattice and not random tries</h2>
 * The Crooked House scatters itself with {@code findRandomPresetTile} and 400
 * attempts, because the Beetlefreak Hollows it needs are 0.129% of tiles and a
 * random try almost always misses. This realm is the opposite problem: it is
 * ALL Crooked ground, so random tries would succeed constantly and the buildings
 * would be weather. A4.1 asks for places, so a place is where the lattice says
 * and nowhere else — and because the same lattice also drives
 * {@link CrookedPressure} (where hostiles may appear) and
 * {@link CrookedTerrainPainter} (which paves the forecourt), all three cannot
 * drift apart.
 *
 * <h2>What still has to be checked at the site</h2>
 * A lattice cell says WHERE, not WHETHER. The footprint's four corners and its
 * centre all have to be dry land, exactly the five-point test the Crooked House
 * uses — corners alone happily straddle a channel, the centre alone lets three
 * quarters of a building hang over the Spill.
 */
public class CrookedWorldPreset extends WorldPreset {

    /** Vanilla's shared "do not overlap" board for structures. */
    private static final String OCCUPIED_BOARD = "villages";

    @Override
    public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
        // Identifier only. hasAnyOfBiome() would consult the vanilla biome
        // weights, which never know about our painted biomes -- see the class
        // comment.
        return presetsRegion.identifier.equals(SkyRegistry.CROOKED_IDENTIFIER);
    }

    @Override
    public void addToRegion(GameRandom random, final LevelPresetsRegion presetsRegion,
            BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer) {
        final int seed = CrookedLevel.worldGenSeed(presetsRegion.worldRegion.worldEntity.worldSeed);

        this.stampLattice(presetsRegion, seed, CrookedSites.DOORYARD_CELL, CrookedSites.SALT_DOORYARD,
                CrookedSites.DOORYARD_CHANCE,
                new Dimension(DoorYardPreset.WIDTH, DoorYardPreset.HEIGHT),
                CrookedSites.SITE_DOORYARD);
        this.stampLattice(presetsRegion, seed, CrookedSites.INVERTED_CELL, CrookedSites.SALT_INVERTED,
                CrookedSites.INVERTED_CHANCE,
                new Dimension(InvertedHousePreset.WIDTH, InvertedHousePreset.HEIGHT),
                CrookedSites.SITE_INVERTED);
        this.stampLattice(presetsRegion, seed, CrookedSites.LONGTABLE_CELL, CrookedSites.SALT_LONGTABLE,
                CrookedSites.LONGTABLE_CHANCE,
                new Dimension(LongTablePreset.WIDTH, LongTablePreset.HEIGHT),
                CrookedSites.SITE_LONGTABLE);
    }

    /**
     * Every site of one lattice that falls inside this world-preset block.
     *
     * <p>The block is 64x64 level regions = 1024x1024 tiles, so a 190-tile cell
     * puts five or six candidates in it per axis. Each is derived exactly the
     * way {@link CrookedSites#nearest} derives it, so the building lands on the
     * tile the pressure field and the painter already agreed was a site.
     *
     * <p>The preset is stamped so that its CENTRE is the site tile, which is what
     * makes {@link CrookedTerrainPainter#SITE_APRON}'s paved disc a forecourt
     * around the building rather than a patch beside it.
     */
    private void stampLattice(final LevelPresetsRegion presetsRegion, final int seed, int cell, int salt,
            float chance, final Dimension size, final int kind) {
        int startX = presetsRegion.worldRegion.startTileX;
        int startY = presetsRegion.worldRegion.startTileY;
        int endX = startX + presetsRegion.worldRegion.tileWidth;
        int endY = startY + presetsRegion.worldRegion.tileHeight;

        for (int cx = Math.floorDiv(startX, cell); cx <= Math.floorDiv(endX, cell); cx++) {
            for (int cy = Math.floorDiv(startY, cell); cy <= Math.floorDiv(endY, cell); cy++) {
                if (SkyNoise.hash(seed + salt, cx, cy) >= chance) {
                    continue;
                }
                int siteX = Math.round(cx * cell + SkyNoise.hash(seed + salt + 1, cx, cy) * cell);
                int siteY = Math.round(cy * cell + SkyNoise.hash(seed + salt + 2, cx, cy) * cell);
                final int tileX = siteX - size.width / 2;
                final int tileY = siteY - size.height / 2;
                // The whole footprint has to be inside this block: a preset that
                // straddles the boundary would be stamped twice, once by each
                // block, and the occupied board only protects within a block.
                if (tileX < startX || tileY < startY
                        || tileX + size.width >= endX || tileY + size.height >= endY) {
                    continue;
                }
                if (!isValidSite(seed, tileX, tileY, size.width, size.height)) {
                    continue;
                }
                if (presetsRegion.isRectangleOccupied(OCCUPIED_BOARD, tileX, tileY, size.width, size.height)) {
                    continue;
                }
                presetsRegion.addPreset(this, tileX, tileY, size, OCCUPIED_BOARD,
                        new LevelPresetsRegion.WorldPresetPlaceFunction() {
                            @Override
                            public void place(GameRandom placeRandom, Level level, PerformanceTimerManager timer) {
                                build(kind, placeRandom).applyToLevel(level, tileX, tileY);
                            }
                        });
            }
        }
    }

    /** Which building a lattice belongs to. */
    private static Preset build(int kind, GameRandom random) {
        switch (kind) {
            case CrookedSites.SITE_INVERTED:
                return new InvertedHousePreset(random);
            case CrookedSites.SITE_LONGTABLE:
                return new LongTablePreset(random);
            default:
                return new DoorYardPreset(random);
        }
    }

    /**
     * Four corners plus the centre must be dry land.
     *
     * <p>Corners alone would happily straddle a channel; the centre alone would
     * let three quarters of the building hang over the Spill. Same five-point
     * test the Crooked House ships with, and it asks
     * {@link CrookedTerrainPainter} the same pure question the painter will
     * answer when the region is actually painted — no world state, so the
     * decision is stable across a save/load and identical on every client.
     */
    public static boolean isValidSite(int seed, int tileX, int tileY, int width, int height) {
        int x1 = tileX + width - 1;
        int y1 = tileY + height - 1;
        int cx = tileX + width / 2;
        int cy = tileY + height / 2;
        return CrookedTerrainPainter.isLand(seed, tileX, tileY)
                && CrookedTerrainPainter.isLand(seed, x1, tileY)
                && CrookedTerrainPainter.isLand(seed, tileX, y1)
                && CrookedTerrainPainter.isLand(seed, x1, y1)
                && CrookedTerrainPainter.isLand(seed, cx, cy);
    }
}
