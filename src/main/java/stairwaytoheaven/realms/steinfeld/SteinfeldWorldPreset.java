package stairwaytoheaven.realms.steinfeld;

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
 * Stamps Steinfeld's two hand-authored landmarks onto {@link SteinfeldSites}'
 * lattice — {@link GraveyardPreset} and {@link RuinedChapelPreset}.
 *
 * <h2>Why a bare {@code WorldPreset} and not a {@code SimpleGenerationPreset}</h2>
 * The same reason {@code CrookedWorldPreset} records: Steinfeld's ground is
 * painted per tile into the region's own biome layer by
 * {@link SteinfeldTerrainPainter} and never passes through vanilla's biome
 * generator, so a preset scoped to a vanilla biome weight would score zero
 * and effectively never place. A bare {@code WorldPreset}, gated only on the
 * level identifier, is what vanilla's own biome-independent structures
 * ({@code SpiderNestsWorldPreset}, {@code VampireCryptWorldPreset}) use for
 * the same reason.
 *
 * <h2>No land check</h2>
 * Steinfeld has no liquid at all ({@link SteinfeldPressure}'s own header:
 * "there is no sea, so there is no third case to price"), so every tile in
 * the realm is buildable ground and there is nothing for an {@code isLand}
 * test to rule out — unlike the Veil-adjacent realms, which stamp their
 * buildings over channels and hollows that genuinely can eat a footprint.
 */
public class SteinfeldWorldPreset extends WorldPreset {

    /** Vanilla's shared "do not overlap" board for structures. */
    private static final String OCCUPIED_BOARD = "villages";

    @Override
    public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
        return presetsRegion.identifier.equals(SkyRegistry.STEINFELD_IDENTIFIER);
    }

    @Override
    public void addToRegion(GameRandom random, LevelPresetsRegion presetsRegion,
            BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer) {
        int seed = SteinfeldLevel.worldGenSeed(presetsRegion.worldRegion.worldEntity.worldSeed);

        this.stampLattice(presetsRegion, seed, SteinfeldSites.GRAVEYARD_CELL, SteinfeldSites.SALT_GRAVEYARD,
                SteinfeldSites.GRAVEYARD_CHANCE,
                new Dimension(GraveyardPreset.WIDTH, GraveyardPreset.HEIGHT),
                SteinfeldSites.SITE_GRAVEYARD);
        this.stampLattice(presetsRegion, seed, SteinfeldSites.CHAPEL_CELL, SteinfeldSites.SALT_CHAPEL,
                SteinfeldSites.CHAPEL_CHANCE,
                new Dimension(RuinedChapelPreset.WIDTH, RuinedChapelPreset.HEIGHT),
                SteinfeldSites.SITE_CHAPEL);
    }

    /**
     * Every site of one lattice that falls inside this world-preset block —
     * one hashed candidate per cell, exactly the scan
     * {@code CrookedWorldPreset.stampLattice} and
     * {@code SteinfeldLevel.placeGuardPacks} both use. This walks the CELL
     * grid directly rather than asking {@link SteinfeldSites#nearest} at each
     * cell's corner: {@code nearest} answers "what site is closest to THIS
     * POINT" by searching a 3x3 neighbourhood, which is the right question for
     * a tile deciding its own pressure or ground and the wrong one here — it
     * would let neighbouring cells rediscover the same site, or a query point
     * miss the very cell it was meant to inspect.
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

    /** Which landmark a lattice belongs to. */
    private static Preset build(int kind, GameRandom random) {
        if (kind == SteinfeldSites.SITE_CHAPEL) {
            return new RuinedChapelPreset(random);
        }
        return new GraveyardPreset(random);
    }
}
