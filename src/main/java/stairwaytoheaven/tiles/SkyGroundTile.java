package stairwaytoheaven.tiles;

import java.awt.Point;

import necesse.engine.util.GameRandom;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.gameTile.TerrainSplatterTile;
import necesse.level.maps.Level;
import stairwaytoheaven.worldgen.SkyPressure;

/**
 * Shared base of every natural ground this mod grows: the four Skyreach
 * terrains, the Skyway's paving, and the Veil's five.
 *
 * <p>It exists for two reasons.
 *
 * <p><b>One.</b> All nine of them carried a byte-identical
 * {@code getTerrainSprite} and its {@code drawRandom} — the vanilla
 * {@code RockTile} pattern of picking a variant row from the tile position.
 * Nine copies of one method is nine places to fix a rendering bug in.
 *
 * <p><b>Two, and the reason it was written now.</b> It is the one place the
 * whole mod can answer the question "may a hostile appear on this tile?".
 * {@code docs/WORLD_DESIGN.md} A4.1 — <i>"sie sollen mal geballt kommen und ein
 * Gebiet bewachen wo es loot gibt, in anderen Ecken aber nicht dauernd
 * angeflogen kommen"</i> — cannot be answered from a spawn table, which knows
 * only weights, and the design note says so explicitly. It can be answered
 * here, because {@code EntityManager.tickMobSpawning} picks the spawn POSITION
 * by asking each candidate tile for its ticket count, and drops a tile
 * returning zero out of the draw entirely. {@link SkyPressure} holds the policy
 * and the measurements; this class is only the hook it hangs on.
 */
public abstract class SkyGroundTile extends TerrainSplatterTile {

    private final GameRandom drawRandom;

    protected SkyGroundTile(boolean isFloor, String textureName) {
        super(isFloor, textureName);
        this.drawRandom = new GameRandom();
    }

    /** With an explicit alpha mask, the way the Beetlefreak ground needs. */
    protected SkyGroundTile(boolean isFloor, String textureName, String alphaMaskTextureName) {
        super(isFloor, textureName, alphaMaskTextureName);
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
     * How strongly a hostile spawn is drawn to this tile — the sky's quiet.
     *
     * See {@link SkyPressure} for what the numbers mean and where they come
     * from. Vanilla's default is 100; this returns 0 across most open ground,
     * which is what makes walking between places calm, and 600 on the ground
     * around a wreck or a workshop, which is what makes arriving at one not.
     */
    @Override
    public int getMobSpawnPositionTickets(Level level, int tileX, int tileY) {
        return SkyPressure.spawnTickets(level, tileX, tileY);
    }
}
