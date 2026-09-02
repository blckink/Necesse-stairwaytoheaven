package stairwaytoheaven.realms.eden;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Eden Shallows — white sand, turquoise lagoons, lotus and reeds (A3.3:
 * <i>"waterfalls, lagoons, white sand, turquoise water"</i>).
 *
 * <p><b>This is the calm zone, deliberately.</b> A4.1 says open ground between
 * places must be quiet enough that a player can sort an inventory, and A4.3
 * says a realm needs somewhere worth standing still in. In Eden that place is
 * the beach: its table is one entry, the hornet that comes off the water, and
 * its ambient share is the smallest in the realm. What makes the shallows worth
 * crossing is the Lagoon Shrine standing on them — a guarded site with a real
 * fight in it, on ground that is otherwise silent.
 */
public class EdenShallowsBiome extends EdenBiome {

    /**
     * One entry. The Golden Hornet is the only thing in Eden's roster that
     * reads correctly over open water — it flies, and A3.3's dangerous beauty
     * is at its plainest as an insect over a lagoon.
     *
     * <p>{@link #onLandLimited} still applies: a hornet is placed on a tile
     * like every other mob, and an entry that rolled a water tile would fail at
     * placement rather than be skipped in the draw (MobSpawnTable.java:131-138).
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            .add(100, onLandLimited("goldenhornet", 2, RANGE_STANDARD), "goldenhornet");

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /** Quieter still than the rest of Eden: this is the zone that is safe. */
    @Override
    public float getSpawnRateMod(Level level) {
        return super.getSpawnRateMod(level) * 0.5F;
    }

    /** The shore pays in what washes up on it. */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                ChanceLootItem.between(0.55F, "paradisecoconut", 2, 5),
                ChanceLootItem.between(0.30F, "sungrape", 3, 7),
                super.getCrateLootTable(level, tileX, tileY)
        );
    }

    /**
     * The shrine's guard: hornets over the water and serpents on the sand.
     *
     * <p>No anchor, unlike the other two zones. The Lagoon Shrine's danger is
     * that it is in the open — there is nowhere to put your back — so the pack
     * is wide and mobile rather than built around one heavy thing.
     */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"goldenhornet"},
                new String[]{"goldenhornet", "edenserpent", "edenserpent"}, 5, 7);
    }
}
