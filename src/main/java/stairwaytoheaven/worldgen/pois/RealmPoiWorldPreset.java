package stairwaytoheaven.worldgen.pois;

import java.awt.Dimension;

import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.util.GameRandom;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.WorldPreset;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.realms.crooked.CrookedTerrainPainter;
import stairwaytoheaven.realms.eden.EdenTerrainPainter;
import stairwaytoheaven.realms.ghost.GhostTerrainPainter;
import stairwaytoheaven.realms.steinfeld.SteinfeldTerrainPainter;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.SkyTerrainPainter;

/** Places the thirteen inhabited POIs into their realm bands on {@code skyreach2}. */
public class RealmPoiWorldPreset extends WorldPreset {
    public static final String STRING_ID = "swh_realmpois";
    private static final String OCCUPIED_BOARD = "villages";
    private static final int CELL = 220;
    private static final float SITE_CHANCE = 0.42F;
    private static final int SALT = 0x61A7;

    private static final int[][] REALM_KINDS = {
            {RealmPoiPresets.SKY_TOWER, RealmPoiPresets.SKY_TOWN,
                    RealmPoiPresets.SKY_TOLL_BRIDGE, RealmPoiPresets.SKY_INN},
            {RealmPoiPresets.EDEN_CROWN_GARDEN, RealmPoiPresets.EDEN_FERMENT_HOUSE},
            {RealmPoiPresets.STEINFELD_MEMORIAL},
            {RealmPoiPresets.GHOST_ARCHIVE},
            {RealmPoiPresets.CROOKED_BAZAAR},
            {RealmPoiPresets.HELL_BORDER_OFFICE, RealmPoiPresets.HELL_ADMINISTRATION,
                    RealmPoiPresets.HELL_FORGE, RealmPoiPresets.HELL_CARNIVAL},
    };

    /** Resolve every tile/object once when registries close, so a typo fails at load instead of far out in the world. */
    @Override
    public void onRegistryClosed() {
        for (int kind = 0; kind < RealmPoiPresets.COUNT; kind++) {
            RealmPoiPresets.build(kind, new GameRandom(0L));
        }
    }

    @Override
    public boolean shouldAddToRegion(LevelPresetsRegion region) {
        return region.identifier.equals(SkyRegistry.SKYREACH_IDENTIFIER);
    }

    @Override
    public void addToRegion(GameRandom random, final LevelPresetsRegion region,
            BiomeGeneratorStack generatorStack, PerformanceTimerManager timer) {
        final int seed = SkyOrigin.worldGenSeed(region.worldRegion.worldEntity);
        int startX = region.worldRegion.startTileX;
        int startY = region.worldRegion.startTileY;
        int endX = startX + region.worldRegion.tileWidth;
        int endY = startY + region.worldRegion.tileHeight;

        for (int cellX = Math.floorDiv(startX, CELL); cellX <= Math.floorDiv(endX, CELL); cellX++) {
            for (int cellY = Math.floorDiv(startY, CELL); cellY <= Math.floorDiv(endY, CELL); cellY++) {
                if (SkyNoise.hash(seed + SALT, cellX, cellY) >= SITE_CHANCE) continue;
                int siteX = Math.round(cellX * CELL + SkyNoise.hash(seed + SALT + 1, cellX, cellY) * CELL);
                int siteY = Math.round(cellY * CELL + SkyNoise.hash(seed + SALT + 2, cellX, cellY) * CELL);
                int realm = RealmDepth.realmAt(seed, siteX, siteY,
                        SkyOrigin.originX(seed), SkyOrigin.originY(seed));
                int[] choices = REALM_KINDS[realm];
                int choice = Math.min(choices.length - 1,
                        (int) (SkyNoise.hash(seed + SALT + 3, cellX, cellY) * choices.length));
                final int kind = choices[choice];
                final int width = RealmPoiPresets.width(kind);
                final int height = RealmPoiPresets.height(kind);
                final int x = siteX - width / 2;
                final int y = siteY - height / 2;

                // One preset region owns the entire footprint. This prevents a
                // boundary structure being queued twice by neighbouring blocks.
                if (x < startX || y < startY || x + width >= endX || y + height >= endY) continue;
                // The canonical Warden Spire remains the uncluttered first landmark.
                int dx = siteX - SkyOrigin.originX(seed);
                int dy = siteY - SkyOrigin.originY(seed);
                if (dx * dx + dy * dy < 100 * 100) continue;
                if (!validSite(kind, realm, seed, x, y, width, height)) continue;
                if (region.isRectangleOccupied(OCCUPIED_BOARD, x, y, width, height)) continue;

                region.addPreset(this, x, y, new Dimension(width, height), OCCUPIED_BOARD,
                        new LevelPresetsRegion.WorldPresetPlaceFunction() {
                            @Override
                            public void place(GameRandom placeRandom, Level level, PerformanceTimerManager placeTimer) {
                                RealmPoiPresets.build(kind, placeRandom).applyToLevel(level, x, y);
                            }
                        });
            }
        }
    }

    private static boolean validSite(int kind, int realm, int seed, int x, int y, int width, int height) {
        if (kind == RealmPoiPresets.SKY_TOLL_BRIDGE) {
            int middleY = y + height / 2;
            return skyLand(seed, x + width / 2, y)
                    && skyLand(seed, x + width / 2, y + height - 1)
                    && !skyLand(seed, x, middleY)
                    && !skyLand(seed, x + width - 1, middleY);
        }
        // Nine samples, rather than corners alone: a large town must not bridge
        // a cloud-sea inlet through the middle of a house block.
        for (int sx = 0; sx <= 2; sx++) {
            for (int sy = 0; sy <= 2; sy++) {
                int tileX = x + sx * (width - 1) / 2;
                int tileY = y + sy * (height - 1) / 2;
                if (!land(realm, seed, tileX, tileY)) return false;
            }
        }
        return true;
    }

    private static boolean land(int realm, int seed, int x, int y) {
        switch (realm) {
            case RealmDepth.REALM_SKYREACH: return skyLand(seed, x, y);
            case RealmDepth.REALM_EDEN: return EdenTerrainPainter.isLand(seed, x, y);
            case RealmDepth.REALM_STEINFELD: return SteinfeldTerrainPainter.isLand(seed, x, y);
            case RealmDepth.REALM_GHOST: return GhostTerrainPainter.isLand(seed, x, y);
            case RealmDepth.REALM_CROOKED:
            case RealmDepth.REALM_HELL: return CrookedTerrainPainter.isLand(seed, x, y);
            default: return false;
        }
    }

    private static boolean skyLand(int seed, int x, int y) {
        long description = SkyTerrainPainter.describeTile(seed, x, y,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed));
        return SkyTerrainPainter.descTile(description) != SkyRegistry.mistseaID;
    }
}
