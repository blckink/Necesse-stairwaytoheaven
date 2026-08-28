package stairwaytoheaven.arsenal;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.hostile.SpiritGhoulMob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Fen Wraith — the Gloomfen's own dead, wading the murkwater and leaving a
 * burning wake behind them.
 *
 * <p><b>Vanilla base:</b> {@link SpiritGhoulMob}, art {@code mobs/spiritghoul}
 * (a translucent teal-green wraith; measured against the mod's own
 * {@code GHOSTFLAME} ramp it is already the Veil's colour and needs no shift).
 * Subclassing keeps the whole reason to pick it: a slow (speed 15) armoured
 * chaser that swims, and a {@code serverTick} that drops a
 * {@code RuneSpiritPoolEvent} every 16 units it runs on dry land — so the fen
 * fills up with pools behind it and standing still is not an option.
 *
 * <p><b>Nothing is re-tuned.</b> 275 HP / 52 damage / 20 armour sits between
 * the Veil's existing Gloom Shade (240/50) and the Skyreach's Skystone Golem
 * (520/70), which is where the Veil's slow bruiser belongs.
 *
 * <p>Vanilla's loot table is surface-cave loot (coins, amber, dryad saplings);
 * ours is replaced with Veil materials.
 */
public class FenWraithMob extends SpiritGhoulMob {

    /**
     * Veil essence is what a shade is made of and what the mod already drops
     * from the Gloom Shade; the cinder pearl is the Stormdisc's burning hub.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.65F, LootItem.between("veilessence", 1, 2)),
            new ChanceLootItemList(0.35F, LootItem.between("cinderpearl", 1, 2)));

    public FenWraithMob() {
        super();
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /** The Veil's hostiles use the same static-light rule as the sky's. */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
