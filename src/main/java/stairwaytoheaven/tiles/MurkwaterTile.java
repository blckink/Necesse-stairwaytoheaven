package stairwaytoheaven.tiles;

import java.awt.Color;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.LevelTileTerrainDrawOptions;
import necesse.level.gameTile.LiquidTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;

/**
 * Murkwater: the black marsh water between the Veil's landmasses. Swimmable
 * (unpleasantly), bridgeable by placing tiles over it.
 */
public class MurkwaterTile extends LiquidTile {

    private static final Color MURK_COLOR = new Color(38, 46, 48);

    public MurkwaterTile() {
        super(MURK_COLOR, "murkwater_shallow", "murkwater_deep");
    }

    @Override
    public TextureIndexes getTextureIndexes(Level level, int tileX, int tileY, Biome biome) {
        return new TextureIndexes(0, 1, 0, 1);
    }

    @Override
    public Color getLiquidColor(Level level, int tileX, int tileY, Biome biome) {
        return MURK_COLOR;
    }

    @Override
    protected void addLiquidTopDrawables(LevelTileTerrainDrawOptions list, List<LevelSortedDrawable> sortedList,
            Level level, int tileX, int tileY, GameCamera camera, TickManager tickManager) {
        // The mist surface stays flat in v0.1; drifting cloud wisps are a
        // roadmap polish item (v0.2).
    }
}
