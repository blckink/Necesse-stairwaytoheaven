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
 * The Mistsea: the endless cloud-ocean between the sky islands. Mechanically a
 * liquid like water (swimmable, bridgeable by placing tiles over it), visually
 * a slow, pale sea of rolling mist.
 */
public class MistseaTile extends LiquidTile {

    private static final Color MIST_COLOR = new Color(188, 202, 214);

    public MistseaTile() {
        super(MIST_COLOR, "mistsea_shallow", "mistsea_deep");
    }

    @Override
    public TextureIndexes getTextureIndexes(Level level, int tileX, int tileY, Biome biome) {
        return new TextureIndexes(0, 1, 0, 1);
    }

    @Override
    public Color getLiquidColor(Level level, int tileX, int tileY, Biome biome) {
        return MIST_COLOR;
    }

    @Override
    protected void addLiquidTopDrawables(LevelTileTerrainDrawOptions list, List<LevelSortedDrawable> sortedList,
            Level level, int tileX, int tileY, GameCamera camera, TickManager tickManager) {
        // The mist surface stays flat in v0.1; drifting cloud wisps are a
        // roadmap polish item (v0.2).
    }
}
