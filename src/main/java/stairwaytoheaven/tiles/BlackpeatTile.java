package stairwaytoheaven.tiles;

import java.awt.Color;
import java.awt.Point;

import necesse.engine.util.GameRandom;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.gameTile.TerrainSplatterTile;
import necesse.level.maps.Level;

/**
 * Wet black peat patches breaking up the Gloomfen moss.
 */
public class BlackpeatTile extends TerrainSplatterTile {

    private final GameRandom drawRandom;

    public BlackpeatTile() {
        super(false, "blackpeat");
        this.mapColor = new Color(44, 39, 44);
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

    @Override
    public int getTerrainPriority() {
        return 204;
    }
}
