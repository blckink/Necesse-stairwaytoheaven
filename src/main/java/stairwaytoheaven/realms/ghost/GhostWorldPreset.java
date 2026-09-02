package stairwaytoheaven.realms.ghost;

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

/** Places the three authored Ghost sites on the same lattices as pressure and guards. */
public class GhostWorldPreset extends WorldPreset {
    private static final String OCCUPIED_BOARD = "villages";
    private static final int MAUSOLEUM = 0;
    private static final int MANOR = 1;
    private static final int GRAVEYARD = 2;

    @Override
    public boolean shouldAddToRegion(LevelPresetsRegion region) {
        return region.identifier.equals(SkyRegistry.GHOST_IDENTIFIER);
    }

    @Override
    public void addToRegion(GameRandom random, LevelPresetsRegion region,
            BiomeGeneratorStack generatorStack, PerformanceTimerManager timer) {
        int seed = GhostLevel.worldGenSeed(region.worldRegion.worldEntity.worldSeed);
        stamp(region, seed, GhostTerrainPainter.MAUSOLEUM_CELL, GhostTerrainPainter.SALT_MAUSOLEUM,
                GhostTerrainPainter.MAUSOLEUM_CHANCE,
                new Dimension(MausoleumPreset.WIDTH, MausoleumPreset.HEIGHT), MAUSOLEUM);
        stamp(region, seed, GhostTerrainPainter.MANOR_CELL, GhostTerrainPainter.SALT_MANOR,
                GhostTerrainPainter.MANOR_CHANCE,
                new Dimension(HauntedManorPreset.WIDTH, HauntedManorPreset.HEIGHT), MANOR);
        stamp(region, seed, GhostTerrainPainter.GRAVEYARD_CELL, GhostTerrainPainter.SALT_GRAVEYARD,
                GhostTerrainPainter.GRAVEYARD_CHANCE,
                new Dimension(SunkenGraveyardPreset.WIDTH, SunkenGraveyardPreset.HEIGHT), GRAVEYARD);
    }

    private void stamp(final LevelPresetsRegion region, final int seed, int cell, int salt,
            float chance, final Dimension size, final int kind) {
        int startX = region.worldRegion.startTileX;
        int startY = region.worldRegion.startTileY;
        int endX = startX + region.worldRegion.tileWidth;
        int endY = startY + region.worldRegion.tileHeight;
        for (int cx = Math.floorDiv(startX, cell); cx <= Math.floorDiv(endX, cell); cx++) {
            for (int cy = Math.floorDiv(startY, cell); cy <= Math.floorDiv(endY, cell); cy++) {
                if (SkyNoise.hash(seed + salt, cx, cy) >= chance) continue;
                int siteX = Math.round(cx * cell + SkyNoise.hash(seed + salt + 1, cx, cy) * cell);
                int siteY = Math.round(cy * cell + SkyNoise.hash(seed + salt + 2, cx, cy) * cell);
                final int x = siteX - size.width / 2;
                final int y = siteY - size.height / 2;
                if (x < startX || y < startY || x + size.width >= endX || y + size.height >= endY
                        || !isValidSite(seed, x, y, size.width, size.height)
                        || region.isRectangleOccupied(OCCUPIED_BOARD, x, y, size.width, size.height)) continue;
                region.addPreset(this, x, y, size, OCCUPIED_BOARD,
                        new LevelPresetsRegion.WorldPresetPlaceFunction() {
                            @Override
                            public void place(GameRandom placeRandom, Level level, PerformanceTimerManager timer) {
                                build(kind, placeRandom).applyToLevel(level, x, y);
                            }
                        });
            }
        }
    }

    private static Preset build(int kind, GameRandom random) {
        if (kind == MANOR) return new HauntedManorPreset(random);
        if (kind == GRAVEYARD) return new SunkenGraveyardPreset(random);
        return new MausoleumPreset(random);
    }

    public static boolean isValidSite(int seed, int x, int y, int width, int height) {
        int x1 = x + width - 1;
        int y1 = y + height - 1;
        return GhostTerrainPainter.isLand(seed, x, y)
                && GhostTerrainPainter.isLand(seed, x1, y)
                && GhostTerrainPainter.isLand(seed, x, y1)
                && GhostTerrainPainter.isLand(seed, x1, y1)
                && GhostTerrainPainter.isLand(seed, x + width / 2, y + height / 2);
    }
}
