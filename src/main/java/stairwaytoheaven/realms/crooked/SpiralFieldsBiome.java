package stairwaytoheaven.realms.crooked;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The Spiral Fields — the part of Crooked Beyond that is still growing, badly.
 *
 * <p>Violet coils underfoot, spiral trees, screaming flowers and shrubs with
 * eyes in them. {@code WORLD_DESIGN.md} A3.6 asks for <i>"lanterns growing out
 * of plants ... trees shaped as spirals, eye-trunks, bones or worms"</i>, and
 * this is the band where all of that is the normal vegetation rather than a
 * shock.
 *
 * <p><b>It is also the realm's harvest ground.</b> Oddwood, Warp Resin and Eye
 * Seed all come off things that grow here, which is why it carries the softest
 * of the three spawn tables: A4.2 says a run should leave the player still
 * wanting something specific, and a gathering ground that fights back every
 * forty seconds is a gathering ground nobody returns to.
 */
public class SpiralFieldsBiome extends CrookedBiome {

    /**
     * Two entries, both fast/standard, no elite.
     *
     * <p>The Fields are where the player has both hands busy. The Tongue Plant
     * is the one that belongs here — a thing that was scenery until it moved —
     * and the armadillo is the one that comes at you from across the field. The
     * Rare Crooked Golem is deliberately absent: an event-tier body in a
     * harvest biome turns gathering into an ambush, which is the complaint A4.1
     * exists to answer.
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(70, "tongueplant", 3, RANGE_STANDARD)
            .addLimited(40, "crookedarmadillo", 2, RANGE_STANDARD);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /**
     * Bridging in the Fields reclaims the spiral soil rather than the stripes —
     * the one place in the realm where the base's answer is overridden, because
     * the Fields' whole identity is the ground they grow out of.
     */
    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        return CrookedRealm.spiralSoilTile;
    }

    /**
     * The Fields' crate: the growing half of the realm's economy, weighted
     * toward what grows.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("oddwood", 6, 14),
                ChanceLootItem.between(0.60F, "warpresin", 3, 8),
                ChanceLootItem.between(0.45F, "eyeseed", 2, 4),
                ChanceLootItem.between(0.20F, "strangefabric", 1, 3)
        );
    }

    /**
     * The Fields' guard: no anchor, three of the thing that was a plant.
     *
     * <p>A guarded place needs a shape, and this one's shape is that the shape
     * is not obvious. Everything standing round the loot here looks like the
     * vegetation the player has been walking through for the last five minutes.
     */
    @Override
    public Guard getGuard() {
        return new Guard(
                new String[]{"tongueplant", "tongueplant"},
                new String[]{"tongueplant", "crookedarmadillo", "crookedgolem"},
                5, 7);
    }
}
