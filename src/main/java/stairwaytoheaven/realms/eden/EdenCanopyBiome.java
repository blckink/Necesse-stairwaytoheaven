package stairwaytoheaven.realms.eden;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Eden Canopy — under the giant trees: root floor, rich soil, the Knowledge
 * Trees, and the Forbidden Serpent that lives near them.
 *
 * <p>A3.3: <i>"Around the Knowledge Tree, snakes grow more common and rare
 * resources lie about — a soft difficulty gradient that needs no gate."</i>
 * Both halves are here. The gradient is
 * {@link EdenPressure#KNOWLEDGE_TICKETS} — nearer a Knowledge Tree, more of
 * everything spawns — and the resources are
 * {@link EdenTerrainPainter#canopyObject}, which puts Eden Copper at three
 * times the garden's rate and is the only place the Eden Cache appears.
 *
 * <p>This is the zone a player comes back to, which is A4.2's actual test:
 * after one run through Eden you should still want something specific, and that
 * something is bronze.
 */
public class EdenCanopyBiome extends EdenBiome {

    /**
     * The Jealous Vine is the canopy's standard enemy — A3.3's <i>"living
     * vines"</i>, and the thing that makes walking between trunks different
     * from walking across a meadow. The Forbidden Serpent is the elite, capped
     * at two over sixteen tiles so a pair is a pair across a stretch of forest
     * rather than a pair per clearing.
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            .add(45, onLandLimited("jealousvine", 3, RANGE_STANDARD), "jealousvine")
            .add(30, onLandLimited("edenserpent", 3, RANGE_STANDARD), "edenserpent")
            .add(15, onLandLimited("bloommaw", 2, RANGE_STANDARD), "bloommaw")
            // Elite. The realm's hardest ambient roll, and still only 10% of
            // the table: an elite you meet constantly is a standard enemy with
            // more health.
            .add(10, onLandLimited("forbiddenserpent", 2, RANGE_ELITE), "forbiddenserpent");

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /** The canopy is the loud zone. Still under vanilla, still nothing like a drizzle. */
    @Override
    public float getSpawnRateMod(Level level) {
        return super.getSpawnRateMod(level) * 1.25F;
    }

    /** The deep pays in metal. */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                ChanceLootItem.between(0.60F, "edencopperore", 4, 9),
                ChanceLootItem.between(0.28F, "edenbronzebar", 1, 3),
                ChanceLootItem.between(0.25F, "serpentscale", 2, 5),
                super.getCrateLootTable(level, tileX, tileY)
        );
    }

    /**
     * The Knowledge Grove's guard: the Forbidden Serpent, with vines.
     *
     * <p>This is the hardest placed pack in the realm and it is the only place
     * the elite is guaranteed rather than rolled — A4.1's whole point is that
     * the budget is spent HERE instead of being smeared over the map.
     */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"forbiddenserpent", "jealousvine"},
                new String[]{"edenserpent", "jealousvine", "bloommaw"}, 5, 7);
    }
}
