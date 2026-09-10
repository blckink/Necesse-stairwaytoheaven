package stairwaytoheaven.realms.hell;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The Infernal Fringe — Hell's inner band, and the realm's whole transition.
 *
 * <p>{@code docs/WORLD_DESIGN.md} §17 defines it in one line: <i>"A
 * transition. Still Crooked Beyond, with the first hell elements."</i> That is
 * exactly what {@link HellTerrainPainter} paints here — Crooked's own violet
 * mud and stripe still under your feet, black peat and ash sand opening up
 * between them, and more of the second the further out you walk. A player
 * crossing from Crooked Beyond into Hell should not meet an edge; they should
 * notice, some minutes later, that the ground stopped being Crooked's a while
 * ago.
 *
 * <h2>The roster</h2>
 * §17 names its four enemies, and they are the realm's four: Infernal Clerk,
 * Ticket Imp, Boiler Hound, Ash Spirit. The fringe gets the two lighter ones
 * in quantity and the elite as a visitor — an elite at more than one in ten
 * would make the transition a wall, and a transition is supposed to be a walk.
 */
public class InfernalFringeBiome extends HellBiome {

    /**
     * Weights read as 90/50/35/20 = 195 tickets: the Clerk is the ground's own
     * resident at 46%, the hounds are what fill the gaps, the imp is the
     * reason to look up, and the Ash Spirit is a visitor at one in ten.
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Standard.
            .addLimited(90, "infernalclerk", 3, RANGE_STANDARD)
            // Fast.
            .addLimited(50, "boilerhound", 3, RANGE_STANDARD)
            // Ranged.
            .addLimited(35, "ticketimp", 2, RANGE_RANGED)
            // Elite. One over sixteen tiles: a problem, not yet a decision.
            .addLimited(20, "ashspirit", 1, RANGE_ELITE);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /** The fringe's guard: a clerk with a counter's worth of hounds behind it. */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"infernalclerk", "infernalclerk"},
                new String[]{"boilerhound", "ticketimp", "infernalclerk"}, 5, 7);
    }

    /**
     * What the inner band adds: the last of Crooked Beyond, in Hell's
     * quantities. Nothing here is a Hell material, because §20's six do not
     * exist yet — see {@link HellBiome#getCrateLootTable}.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        LootTable common = super.getCrateLootTable(level, tileX, tileY);
        return new LootTable(
                common,
                ChanceLootItem.between(0.35F, "strangefabric", 2, 5));
    }
}
