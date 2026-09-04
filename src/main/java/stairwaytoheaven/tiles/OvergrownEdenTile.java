package stairwaytoheaven.tiles;

import java.awt.Color;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.SimulatePriorityList;

/**
 * Eden grass: the first ground of the Garden of Eden, from the player's own
 * supplied art (kk-sprites/overgrowngrass_splat-overgrowneden_splatt.png).
 *
 * <p>{@code docs/WORLD_DESIGN.md} §5: Eden is <i>"an exaggerated biological
 * explosion: big, lush, dense, warm, colourful, alive"</i>, and this is its
 * "Eden Grass" tile. The supplied name records the vanilla asset it was drawn
 * on — {@code overgrowngrass} — so the setup mirrors vanilla's
 * {@code OvergrownGrassTile} line for line, <b>VERIFIED [jar]</b>:
 *
 * <ul>
 * <li>it GROWS: an empty tile of it sprouts vanilla's {@code grass} object over
 *     time (the same borrowed object vanilla grows; A4.3 says to build with
 *     borrowed assets and swap the art later), both on live ticks and through
 *     {@code addSimulateLogic} so it keeps growing while nobody is there;</li>
 * <li>it SPREADS to adjacent dirt at vanilla's own rate — meaningless in the
 *     sky, which has no dirt, and exactly right on the surface, where a planted
 *     patch slowly turns a garden into a garden;</li>
 * <li>mining it drops its seed 4% of the time, vanilla's rate, so a found patch
 *     is a renewable source of more Eden rather than a one-off.</li>
 * </ul>
 *
 * <p>Until the Eden realm ships as a complete chapter (A4.3: complete or not at
 * all), no worldgen paints this tile — it enters the world through the seed,
 * found in sky crates. The ground exists, propagates and is the realm's first
 * brick; the realm follows.
 *
 * <p>Extends {@link SkyGroundTile}, so on the Skyreach and in the Veil it
 * answers the A4.1 pressure field like every other mod ground; anywhere else
 * (a surface garden, a settlement floor) it behaves exactly like vanilla
 * ground.
 */
public class OvergrownEdenTile extends SkyGroundTile {

    /** Vanilla OvergrownGrassTile's own rates, VERIFIED [jar] (lines 21-22). */
    public static final double GROW_CHANCE = GameMath.getAverageSuccessRuns(3500.0);
    public static final double SPREAD_CHANCE = GameMath.getAverageSuccessRuns(550.0);

    public OvergrownEdenTile() {
        super(false, "overgrowneden");
        // Deep Eden green: darker and warmer than vanilla overgrowngrass's
        // (61, 87, 0), sampled from the supplied splat's dominant tone.
        this.mapColor = new Color(38, 92, 22);
        this.canBeMined = true;
        this.isOrganic = true;
    }

    @Override
    public LootTable getLootTable(Level level, int tileX, int tileY) {
        // Vanilla's exact seed-back rate (OvergrownGrassTile line 37).
        return new LootTable(new ChanceLootItem(0.04F, "overgrownedenseed"));
    }

    @Override
    public void addSimulateLogic(Level level, int x, int y, long ticks,
            SimulatePriorityList list, boolean sendChanges) {
        // Vanilla's own offline-growth helper, pointed at Eden's grass object.
        necesse.level.gameTile.OvergrownGrassTile.addSimulateGrow(
                level, x, y, GROW_CHANCE, ticks, "overgrowngrass", list, sendChanges);
    }

    @Override
    public double spreadToDirtChance() {
        return SPREAD_CHANCE;
    }

    @Override
    public void tick(Level level, int x, int y) {
        // OvergrownGrassTile.tick verbatim: sprout an Eden grass tuft on an
        // empty tile at the grow rate.
        if (level.isServer()) {
            if (level.getObjectID(x, y) == 0 && GameRandom.globalRandom.getChance(GROW_CHANCE)) {
                GameObject grass = ObjectRegistry.getObject(ObjectRegistry.getObjectID("overgrowngrass"));
                if (grass.canPlace(level, x, y, 0, false) == null) {
                    grass.placeObject(level, x, y, 0, false);
                    level.objectLayer.setIsPlayerPlaced(x, y, false);
                    level.sendObjectUpdatePacket(x, y);
                }
            }
        }
    }

    @Override
    public int getTerrainPriority() {
        // Vanilla overgrowngrass's own 200: under every sky ground (204-260),
        // so a planted patch never swallows the terrain around it.
        return 200;
    }
}
