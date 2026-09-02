package stairwaytoheaven.realms.eden;

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
 * Turquoise Shallow Water — Eden's lagoons (§5, A3.3).
 *
 * <p><b>Borrowed art:</b> vanilla's own {@code tiles/saltwater_shallow_splat.png}
 * and {@code tiles/saltwater_deep_splat.png}, read through the same two-index
 * shape {@code stairwaytoheaven.tiles.MistseaTile} uses for its own pair.
 *
 * <p><b>The liquid colour is vanilla's water colour, unchanged.</b>
 * {@code WaterTile} passes {@code new Color(31, 133, 170)} and so does this.
 * That value is not decoration: {@code LiquidTile} spends it on the bucket item
 * icon ({@code LiquidTile.java:81}, an {@code applyColor} over the overlay),
 * the flat fill drawn under the liquid ({@code :388}) and the placement preview
 * ({@code :412}). Choosing a turquoise for it would be recolouring borrowed art
 * at draw time, which this realm is not allowed to do — so the water ships as
 * vanilla saltwater and the design's turquoise arrives with the art pass.
 *
 * <p><b>What this deliberately is not.</b> It is not vanilla's water tile, and
 * so it is not fishable. That is the price of owning
 * {@link #getMobSpawnPositionTickets}: Eden's lagoons are a large share of the
 * realm, nothing in its roster stands on liquid, and at vanilla's default 100
 * tickets the water would win most spawn draws and starve the guarded sites —
 * the Mistsea's measured lesson, applied rather than re-learned. Fishing in
 * Eden is listed as deferred in {@code docs/realms/eden.md}.
 */
public class EdenShallowsTile extends LiquidTile {

    /** Vanilla {@code WaterTile}'s own colour, copied rather than chosen. */
    private static final Color WATER = new Color(31, 133, 170);

    public EdenShallowsTile() {
        super(WATER, "saltwater_shallow", "saltwater_deep");
    }

    @Override
    public TextureIndexes getTextureIndexes(Level level, int tileX, int tileY, Biome biome) {
        // Two sheets, used for both the fresh and the salt slot — MistseaTile's
        // own shape. Vanilla's WaterTile carries eight because it swaps sheets
        // per biome; Eden has one sea.
        return new TextureIndexes(0, 1, 0, 1);
    }

    @Override
    public Color getLiquidColor(Level level, int tileX, int tileY, Biome biome) {
        return WATER;
    }

    @Override
    public int getMobSpawnPositionTickets(Level level, int tileX, int tileY) {
        return level instanceof EdenLevel
                ? EdenPressure.SHALLOWS_TICKETS
                : super.getMobSpawnPositionTickets(level, tileX, tileY);
    }

    @Override
    protected void addLiquidTopDrawables(LevelTileTerrainDrawOptions list,
            List<LevelSortedDrawable> sortedList, Level level, int tileX, int tileY,
            GameCamera camera, TickManager tickManager) {
        // Vanilla's WaterTile draws a second surface layer out of
        // tiles/watershallow + tiles/waterdeep on top of the splat, and tints
        // it per biome. Drawing it here would be exactly the load-time recolour
        // this realm may not do, so the splat stands alone — the same choice
        // MistseaTile made, for a different reason.
    }
}
