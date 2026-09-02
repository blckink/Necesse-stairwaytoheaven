package stairwaytoheaven.realms.steinfeld;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The Grave Heath — the outer band: grey grass, black turned earth, mist
 * stone, gravestones standing on their own in open country, and fog.
 *
 * <p>{@code docs/WORLD_DESIGN.md} A3.4 is what this band is for. Beyond it is
 * the Veil, and the ground here is already the Veil's ground — the same moss
 * sheet, the same dead trees, the same mushroom light. A player who reaches the
 * heath has been shown where they are going without being told.
 */
public class GraveHeathBiome extends SteinfeldBiome {

    /**
     * The heaviest table in the realm.
     *
     * <p>50/60/45/55 = 210 tickets, and the elite is now a quarter of them.
     * The Lost Pilgrim steps back from the lead it held in the meadow, because
     * out here the ghosts are mostly not enemies at all — the ones that still
     * are have been out here longest.
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Fast.
            .addLimited(50, "lostpilgrim", 3, RANGE_STANDARD)
            // Standard.
            .addLimited(60, "stonemourner", 3, RANGE_STANDARD)
            // Ranged. The heath is open ground and a crow owns open ground.
            .addLimited(45, "gravecrow", 2, RANGE_RANGED)
            // Elite.
            .addLimited(55, "hollowangel", 2, RANGE_ELITE);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /**
     * The heath's guard: two hollow angels, and everything else as rabble. This
     * is the realm's hardest standing fight and it is standing on the realm's
     * best loot.
     */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"hollowangel", "hollowangel", "stonemourner"},
                new String[]{"stonemourner", "gravecrow", "lostpilgrim"}, 6, 8);
    }

    /**
     * What the outer band adds: the séance materials.
     *
     * <p>Spirit Moss and Echo Shards are what {@code docs/WORLD_DESIGN.md} §9's
     * quest asks for — five moss and three shards for A CALL TO THE OTHER SIDE
     * — and the heath is the only ground that hands them over in quantity. That
     * is the reason to cross the whole realm, and it is deliberately the LAST
     * band rather than the first.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        LootTable common = super.getCrateLootTable(level, tileX, tileY);
        return new LootTable(
                common,
                LootItem.between("spiritmoss", 2, 5),
                ChanceLootItem.between(0.50F, "echoshard", 1, 3),
                ChanceLootItem.between(0.40F, "gravesalt", 3, 8),
                // Veil Essence: the fog on the other side is already leaking in.
                ChanceLootItem.between(0.18F, "veilessence", 1, 3));
    }
}
