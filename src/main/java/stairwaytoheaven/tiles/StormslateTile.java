package stairwaytoheaven.tiles;

import java.awt.Color;
import java.awt.Point;

import necesse.engine.util.GameRandom;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.gameTile.TerrainSplatterTile;
import necesse.level.maps.Level;

/**
 * Charcoal slate ground of the Stormveil — dark, cracked stone charged with
 * static.
 */
public class StormslateTile extends TerrainSplatterTile {

    private final GameRandom drawRandom;

    public StormslateTile() {
        super(false, "stormslate");
        this.mapColor = new Color(74, 78, 94);
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
        return 220;
    }
}
