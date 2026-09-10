package stairwaytoheaven.realms.hell;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The Furnace Reach — Hell's outer band, at the far edge of the plane.
 *
 * <p>A3.8 splits Hell into four sub-themes rather than one long red corridor —
 * <b>Infernal City</b>, <b>The Furnace</b>, <b>Flesh Gardens</b> and <b>Hell
 * Carnival</b> — and this band is The Furnace: <i>"huge machines, chains,
 * cogs, ovens, lava"</i>. It is the one of the four that can be built out of
 * ground and scatter alone, which is why it is the one this pass ships. The
 * other three are buildings, and Hell already HAS four building POIs standing
 * in it ({@code RealmPoiPresets}' Border Office, Administration, Forge and
 * Carnival) — so the realm reads as all four sub-themes even with two bands,
 * and A3.8's remaining ground-level work is recorded rather than pretended.
 *
 * <p>The ground goes black here — black peat and ash sand over dead soil, with
 * the last of Crooked's colour gone — and the roster inverts: the elite stops
 * being a visitor and becomes what the band is made of.
 */
public class FurnaceReachBiome extends HellBiome {

    /**
     * Weights read as 60/45/40/55 = 200 tickets. Compare the Fringe's
     * 90/50/35/20: the Ash Spirit goes from one ticket in ten to better than
     * one in four, and its cap from one to two, which is the difference
     * between meeting an elite and being somewhere elites live.
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Standard.
            .addLimited(60, "infernalclerk", 2, RANGE_STANDARD)
            // Fast.
            .addLimited(45, "boilerhound", 3, RANGE_STANDARD)
            // Ranged.
            .addLimited(40, "ticketimp", 2, RANGE_RANGED)
            // Elite. Two over sixteen tiles: one is a problem, two is a
            // decision about which way to walk.
            .addLimited(55, "ashspirit", 2, RANGE_ELITE);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /** The Furnace's guard: two Ash Spirits over whatever is still working. */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"ashspirit", "ashspirit"},
                new String[]{"infernalclerk", "boilerhound", "ticketimp"}, 6, 8);
    }

    /**
     * What the outer band adds. Spiritsteel is the heaviest bar the mod
     * currently smelts and the Ghost Realm's own; finding it out here, in a
     * quantity the Ghost Realm never drops, is what says the walk was worth
     * it. §20's Hellsteel is what should eventually replace this line.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        LootTable common = super.getCrateLootTable(level, tileX, tileY);
        return new LootTable(
                common,
                ChanceLootItem.between(0.40F, "spiritsteelbar", 2, 5),
                ChanceLootItem.between(0.22F, "realityshard", 4, 9));
    }
}
