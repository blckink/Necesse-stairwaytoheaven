package stairwaytoheaven.realms.eden;

import java.awt.Point;

import necesse.engine.util.GameRandom;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.gameTile.TerrainSplatterTile;
import necesse.level.maps.Level;

/**
 * Shared base of the Garden of Eden's four natural grounds.
 *
 * <p>It is the twin of {@code stairwaytoheaven.tiles.SkyGroundTile} and exists
 * for the same two reasons — one copy of the vanilla {@code RockTile} variant
 * pick, and one place to answer <i>"may a hostile appear on this tile?"</i> —
 * but it hangs on {@link EdenPressure} rather than on the Skyreach's field.
 *
 * <p><b>Why a second base class and not a shared one.</b> {@code SkyGroundTile}
 * routes every tile through {@code SkyPressure.spawnTickets(Level, ...)}, which
 * knows the Skyreach and the Veil by name and answers vanilla's default 100 for
 * everything else. Teaching it about Eden would put three realms' policy in one
 * file that three parallel streams are editing, and would make the Skyreach's
 * quiet depend on a class in Eden's package. A realm owns its own pressure.
 *
 * <p><b>The art is borrowed.</b> Every texture name passed up to
 * {@code TerrainSplatterTile} here is a VANILLA sheet
 * ({@code tiles/<name>_splat.png}), not one of ours. That resolves because the
 * engine has one flat resource map keyed by path with the mod's files merged
 * into it ({@code ResourceEncoder.java:75-86}) — the same fact that lets
 * {@code GameTexture.fromFile("mobs/cow")} work from mod code. See
 * {@code docs/realms/eden.md} for the full borrowed-art table.
 */
public abstract class EdenGroundTile extends TerrainSplatterTile {

    private final GameRandom drawRandom;

    protected EdenGroundTile(String vanillaTextureName) {
        super(false, vanillaTextureName);
        this.drawRandom = new GameRandom();
    }

    @Override
    public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
        int tile;
        synchronized (this.drawRandom) {
            tile = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(terrainTexture.getHeight() / 32);
        }
        return new Point(0, tile);
    }

    /**
     * How strongly a hostile spawn is drawn to this tile — Eden's quiet.
     *
     * <p>See {@link EdenPressure} for what the numbers mean. Vanilla's default
     * is 100; this returns 0 across most of the garden, which is what lets a
     * player stand in a clearing and sort their inventory, and 600 on the
     * ground around a POI, which is what makes arriving at one not.
     */
    @Override
    public int getMobSpawnPositionTickets(Level level, int tileX, int tileY) {
        return EdenPressure.spawnTickets(level, tileX, tileY);
    }
}
