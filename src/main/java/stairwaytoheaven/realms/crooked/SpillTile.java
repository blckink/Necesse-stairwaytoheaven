package stairwaytoheaven.realms.crooked;

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
 * The Spill — the neon-green liquid between the pieces of Crooked Beyond, and
 * the realm's sea.
 *
 * <p><b>Borrowed sheet:</b> vanilla {@code tiles/ooze_splat.png}. Its owner,
 * {@code OozeLiquidTile}, is <b>VERIFIED [jar]</b>
 * {@code super(new Color(25, 225, 25), "ooze")} — <em>one</em> texture name for
 * both depths, unlike the mod's Mistsea and Murkwater, which each pass a
 * shallow and a deep sheet. {@code LiquidTile}'s constructor is varargs, so a
 * single name is a complete call and not a truncated one.
 *
 * <p><b>Deliberately NOT vanilla's ooze behaviour.</b> {@code OozeLiquidTile}
 * poisons anything swimming in it, costs 1000 path units for a non-immune mob
 * and throws blob particles. None of that is inherited, because this extends
 * {@code LiquidTile} directly: the Spill is the thing the realm's landmasses sit
 * IN — the thing you bridge and swim — and a sea that debuffed on contact would
 * make crossing the realm a chore rather than a decision. It is exactly as
 * dangerous as the Mistsea, which is to say not at all; everything dangerous
 * here is standing on land.
 *
 * <p>Green is not a whim: {@code WORLD_DESIGN.md} §13's palette for this realm
 * is "black-and-white stripes · neon green · violet · red · cyan", and the ooze
 * sheet is the only neon green ground the game already owns.
 */
public class SpillTile extends LiquidTile {

    private static final Color SPILL_COLOR = new Color(96, 214, 74);

    public SpillTile() {
        super(SPILL_COLOR, "ooze");
    }

    @Override
    public TextureIndexes getTextureIndexes(Level level, int tileX, int tileY, Biome biome) {
        return new TextureIndexes(0, 1, 0, 1);
    }

    @Override
    public Color getLiquidColor(Level level, int tileX, int tileY, Biome biome) {
        return SPILL_COLOR;
    }

    /**
     * The Spill is most of the realm by area, and nothing lives on it.
     *
     * <p>Same reasoning as {@code MistseaTile}: at vanilla's default 100 tickets
     * the sea's sheer area would win most of the spawn lottery, and every draw
     * it won would be a wasted tick for a land mob that cannot stand on liquid,
     * while the guarded sites {@link CrookedPressure} exists to make dangerous
     * went quiet. Zero rather than the Mistsea's 8, because this realm has no
     * serpent: nothing at all is meant to come out of the green.
     */
    @Override
    public int getMobSpawnPositionTickets(Level level, int tileX, int tileY) {
        // SkyLevel, not CrookedLevel: the Crooked band is part of the one
        // plane now (docs/PLAN_ONE_PLANE.md), so this tile's own level IS the
        // sky. Anywhere else -- a settlement floor, an incursion -- keeps
        // vanilla's default.
        return level instanceof stairwaytoheaven.level.SkyLevel
                ? 0
                : super.getMobSpawnPositionTickets(level, tileX, tileY);
    }

    @Override
    protected void addLiquidTopDrawables(LevelTileTerrainDrawOptions list, List<LevelSortedDrawable> sortedList,
            Level level, int tileX, int tileY, GameCamera camera, TickManager tickManager) {
        // Flat, like the mod's other two seas. Vanilla's ooze bobs a water
        // sprite on 15% of its tiles; that reads as water, and this is meant to
        // read as something that was poured and never drained.
    }
}
