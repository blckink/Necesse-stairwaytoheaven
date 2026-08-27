package stairwaytoheaven.tiles;

import java.awt.Color;
import java.awt.Point;

import necesse.engine.util.GameRandom;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.gameTile.TerrainSplatterTile;
import necesse.level.maps.Level;

/**
 * The paved cloud of the Skyway Passages: white cloudstone cobbles set in pale
 * blue cloud, rimmed in gold. The ground of the passages themselves rather
 * than a buildable floor, so it is a terrain tile like Cloudturf and Skystone
 * — {@code super(false, ...)}, mineable, and it blends through its own
 * {@code _splat} atlas.
 */
public class SkywayTile extends TerrainSplatterTile {

    private final GameRandom drawRandom;

    public SkywayTile() {
        super(false, "skyway");
        this.mapColor = new Color(214, 228, 236);
        this.canBeMined = true;
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
     * Above Skystone (230) and Cloudturf, so a passage laid across an outcrop
     * stays visible as a passage rather than being overgrown by the plateau.
     */
    @Override
    public int getTerrainPriority() {
        return 260;
    }
}
