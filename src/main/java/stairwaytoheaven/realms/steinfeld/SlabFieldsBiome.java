package stairwaytoheaven.realms.steinfeld;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The Slab Fields — the middle band: pale grass over cracked heaven marble,
 * dead soil opening up between the slabs, and the first dead trees.
 *
 * <p>This is where the realm stops looking like the edge of a garden and starts
 * looking like the floor of something that fell down. It is also where the
 * Stone Mourners are: statues that have been standing among the slabs long
 * enough to be part of the landscape until one of them moves.
 */
public class SlabFieldsBiome extends SteinfeldBiome {

    /**
     * The realm's full mix, for the first time.
     *
     * <p>Weights read as 90/45/40/30 = 205 tickets: the mourner is the ground's
     * own resident at 44%, the pilgrims are what fills the gaps, the crow is
     * the reason to look up, and the hollow angel is a visitor rather than a
     * fixture — an elite at more than one in seven would make the open field a
     * fight, and the open field is supposed to be a walk.
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Standard. Slow, armoured, and it starts as scenery.
            .addLimited(90, "stonemourner", 3, RANGE_STANDARD)
            // Fast.
            .addLimited(45, "lostpilgrim", 3, RANGE_STANDARD)
            // Ranged.
            .addLimited(40, "gravecrow", 2, RANGE_RANGED)
            // Elite. Two over sixteen tiles: one is a problem, two is a
            // decision about which way to walk.
            .addLimited(30, "hollowangel", 2, RANGE_ELITE);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /**
     * The Slab Fields' guard: a hollow angel over a knot of mourners.
     */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"hollowangel", "stonemourner"},
                new String[]{"stonemourner", "lostpilgrim", "gravecrow"}, 5, 7);
    }

    /**
     * What the middle band adds: the working materials. Pale Stone is the
     * realm's building stock and this is the band it comes out of in quantity.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        LootTable common = super.getCrateLootTable(level, tileX, tileY);
        return new LootTable(
                common,
                LootItem.between("palestone", 5, 12),
                ChanceLootItem.between(0.35F, "gravesalt", 2, 6),
                // Charred wood off the dead trees — the Veil's own material,
                // and the first place outside the Veil that gives it.
                ChanceLootItem.between(0.28F, "charwood", 3, 8));
    }
}
