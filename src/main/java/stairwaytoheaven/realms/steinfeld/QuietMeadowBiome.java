package stairwaytoheaven.realms.steinfeld;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The Quiet Meadow — the inner band, where Steinfeld has not finished dying.
 *
 * <p>Eden's own green grass still covers most of it, the stone is still the
 * bright stone of the sky, and the only things that say where you are are a
 * single broken angel standing in the grass and the first buried slab. This is
 * the half of the realm the player is supposed to find beautiful, which is what
 * makes the other two bands mean anything.
 */
public class QuietMeadowBiome extends SteinfeldBiome {

    /**
     * The thinnest roster in the realm, and on purpose.
     *
     * <p>Pilgrims drift through the meadow and a crow or two works the edges;
     * nothing here is an anchor. The heavy things stand further out, so a
     * player who walks in from the gate meets the realm before the realm meets
     * them.
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Fast. The Lost Pilgrim is what the meadow has instead of wildlife:
            // it drifts, it is translucent, and it is the first thing that tells
            // the player these are people. Cap 3 over eight tiles — the engine
            // refuses a fourth hostile in that square anyway (SkyBiome), so
            // three is the largest cap that can bind.
            .addLimited(110, "lostpilgrim", 3, RANGE_STANDARD)
            // Ranged. One crow, working the treeline.
            .addLimited(40, "gravecrow", 2, RANGE_RANGED);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /**
     * The meadow's guard: pilgrims around a single mourner.
     *
     * <p>A chapel out here is guarded by one heavy thing and a crowd, which is
     * a fight a player can read from the doorway. Deeper in it stops being one.
     */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"stonemourner"},
                new String[]{"lostpilgrim", "lostpilgrim", "gravecrow"}, 4, 5);
    }

    /**
     * What the inner band adds to {@link SteinfeldBiome}'s common cargo.
     *
     * <p>Eden's grass seed is the meadow's signature: it is the one place in
     * the mod outside a Skyreach crate that hands it over, and finding it here
     * is the container telling the player which direction Eden is.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        LootTable common = super.getCrateLootTable(level, tileX, tileY);
        return new LootTable(
                common,
                ChanceLootItem.between(0.22F, "overgrownedenseed", 1, 2),
                // Skystone: the sky's own material, still lying about up here.
                ChanceLootItem.between(0.30F, "skystone", 4, 10),
                LootItem.between("palestone", 2, 5));
    }
}
