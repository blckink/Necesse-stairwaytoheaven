package stairwaytoheaven.arsenal;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.hostile.CryoFlakeMob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Aurora Flake — a drifting crystal that hangs over the shoals and throws
 * shards at whatever crosses the mist bank.
 *
 * <p><b>Vanilla base:</b> {@link CryoFlakeMob}, art {@code mobs/cryoflake}
 * (a blue-violet six-point crystal with its own glow row at sprite y=1; it
 * sits inside the Aurora Shoals' cold-dawn palette as drawn and needs no
 * colour shift). Everything that makes it work is inherited: the
 * {@code FlyingAIMover} + {@code CollisionShooterPlayerChaserWandererAI} that
 * closes to 384 and fires {@code CryoMissileProjectile} on a 2s cooldown, the
 * spinning two-layer draw, the chime sounds and the shatter particles.
 *
 * <p><b>Nothing is re-tuned.</b> Vanilla's flake is 350 HP / 65 damage /
 * 20 armour, which already lands inside the mod's own band (Zephyr Ray 220/45
 * … Skystone Golem 520/70) — it is the flying artillery piece the Aurora
 * Shoals lacked. Only the loot table and the spawn rule are ours.
 *
 * <p>Note the inherited {@code getLootTable} would have handed out
 * {@code glacialshard}, a deep-cave snow material with no place in the sky;
 * ours replaces it outright rather than adding to it.
 */
public class AuroraFlakeMob extends CryoFlakeMob {

    /**
     * The Prismcaller is built out of prismshards, and this is the mob that
     * carries them in the air rather than in a rock.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.65F, LootItem.between("prismshard", 1, 3)),
            new ChanceLootItemList(0.35F, LootItem.between("aurorapetal", 1, 2)));

    public AuroraFlakeMob() {
        super();
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * {@code FlyingHostileMob.isValidSpawnLocation} measures AMBIENT + static
     * light, which is 150 in daylight against a threshold of 0 — no flier
     * would ever be placed while the sun is up. {@link SkySpawnRules} swaps
     * that one check for the static-light one; the mob's own
     * {@code checkSpawnLocation} (not-solid, not-indoors) is still applied,
     * because {@code MobSpawnLocation.checkMobSpawnLocation} delegates to it.
     */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
