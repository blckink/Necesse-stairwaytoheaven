package stairwaytoheaven.tiles;

import java.awt.Color;
import java.awt.Point;

import necesse.engine.util.GameRandom;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.gameTile.TerrainSplatterTile;
import necesse.level.maps.Level;

/**
 * Gloomfen ground: dense dark moss over cold peat. Organic — whisper reeds grow on it.
 */
public class MurkmossTile extends TerrainSplatterTile {

    private final GameRandom drawRandom;

    public MurkmossTile() {
        super(false, "murkmoss");
        this.mapColor = new Color(62, 72, 62);
        this.canBeMined = true;
        // organic soil: grass-type objects (Sky Reeds) may grow on it
        this.isOrganic = true;
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
        return 205;
    }
}
