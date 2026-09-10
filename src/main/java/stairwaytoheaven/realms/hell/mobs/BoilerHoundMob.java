package stairwaytoheaven.realms.hell.mobs;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.hostile.JackalMob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import stairwaytoheaven.mobs.SkyMobTiers;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * The Boiler Hound — Hell's fast role, named by {@code docs/WORLD_DESIGN.md}
 * §17's own Infernal Fringe roster. Something that lives beside the machines
 * of A3.8's Furnace and comes out when the pressure drops.
 *
 * <h2>Body</h2>
 * Vanilla's {@code JackalMob}: the game's own lean, quick ground hound.
 * <b>VERIFIED [jar]</b> it is a plain {@code HostileMob} whose {@code init()}
 * builds its own behaviour tree — so the tree is rebuilt here against Hell's
 * damage, the same way {@code StoneMournerMob} does for its own body, rather
 * than being left on vanilla's. Its speed is untouched: the jackal is already
 * the game's fast hound, and the fast role is priced on HP and damage, not on
 * re-tuning a body that is correct.
 *
 * <p><b>The sheet is a placeholder</b> until Hell's own art lands.
 *
 * <h2>Statline</h2>
 * {@link HellTier}'s fast row: 4405 HP / 310.1 damage / 81 armour.
 */
public class BoilerHoundMob extends JackalMob {

    public static final MaxHealthGetter MAX_HEALTH =
            HellTier.health(SkyMobTiers.ROLE_FAST_HP);
    public static final GameDamage DAMAGE =
            HellTier.damage(SkyMobTiers.ROLE_FAST_DAMAGE);
    public static final int ARMOR = HellTier.ARMOR;

    /** 512 base — a hound commits — lifted by Hell's x1.55. */
    public static final int AGGRO_RANGE = HellTier.aggro(512);

    public static LootTable lootTable = new LootTable(
            ChanceLootItem.between(0.40F, "oddwood", 1, 3),
            ChanceLootItem.between(0.15F, "realityshard", 1, 2));

    public BoilerHoundMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        // Vanilla's tree carries vanilla's damage; this is the same shape on
        // OUR damage -- 100 knockback, 40s wander, exactly the arguments the
        // mod's other rebuilt chasers use.
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<>(null, AGGRO_RANGE, DAMAGE, 100, 40000));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }

    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("jackal", super.getMobIcon());
    }
}
