package stairwaytoheaven.realms.ghost;

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
 * Ectoplasm: the turquoise soup the Aftergarden's landmasses sit in.
 *
 * <p>Archetype: {@link LiquidTile}, the same one the Veil's Murkwater uses, so
 * it swims, it can be bridged by placing a tile over it, and the biome's
 * {@code getUnderLiquidTile} decides what a bridge reclaims. The two sheet
 * names are constructor arguments, which is what lets this borrow the game's
 * own swamp-water splat pair rather than needing new art —
 * {@code GameTexture.fromFile} reads one flat resource map with the mod's files
 * merged into the game's, so a vanilla path and a mod path resolve alike.
 *
 * <p>The <b>colour is ours</b>. {@code getLiquidColor} is what the engine tints
 * the water body and the shore foam with, and it is a per-tile field every
 * vanilla liquid sets by hand — not a recolour of the borrowed sheet. Vanilla's
 * swamp water is a muddy green; ectoplasm is cold turquoise, which is the half
 * of {@code WORLD_DESIGN} §A3.5's palette that has to glow.
 *
 * <p>{@code getTextureIndexes(0, 1, 0, 1)} is Murkwater's, unchanged: index 0
 * is the shallow sheet and index 1 the deep one, and fresh and salt use the
 * same pair because the Aftergarden has only one kind of water.
 */
public class EctoplasmTile extends LiquidTile {

    /**
     * Cold turquoise, the realm's signature. Deliberately far from vanilla's
     * swamp green: the sheet is borrowed, the colour is the design.
     */
    private static final Color ECTO_COLOR = new Color(52, 154, 142);

    public EctoplasmTile() {
        super(ECTO_COLOR, "swampfreshwater_shallow", "swampfreshwater_deep");
    }

    @Override
    public TextureIndexes getTextureIndexes(Level level, int tileX, int tileY, Biome biome) {
        return new TextureIndexes(0, 1, 0, 1);
    }

    @Override
    public Color getLiquidColor(Level level, int tileX, int tileY, Biome biome) {
        return ECTO_COLOR;
    }

    /**
     * The marsh is in the spawn lottery, barely. See
     * {@link GhostPressure#MARSH_TICKETS}: zero would be wrong (a realm whose
     * water is invisible to the spawner behaves oddly at the shoreline) and
     * vanilla's 100 would let the water's sheer area starve the guarded sites.
     */
    @Override
    public int getMobSpawnPositionTickets(Level level, int tileX, int tileY) {
        // SkyLevel, not GhostLevel: the Ghost band is part of the one plane
        // now (docs/PLAN_ONE_PLANE.md).
        return level instanceof stairwaytoheaven.level.SkyLevel
                ? GhostPressure.MARSH_TICKETS : 100;
    }

    @Override
    protected void addLiquidTopDrawables(LevelTileTerrainDrawOptions list,
            List<LevelSortedDrawable> sortedList, Level level, int tileX, int tileY,
            GameCamera camera, TickManager tickManager) {
        // Flat surface, like the Veil's Murkwater. Drifting wisps over the
        // ectoplasm would be new art and this realm ships without any.
    }
}
