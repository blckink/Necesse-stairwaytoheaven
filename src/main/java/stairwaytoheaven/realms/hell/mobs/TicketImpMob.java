package stairwaytoheaven.realms.hell.mobs;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.manager.EntityManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.mobs.hostile.CrazedRavenMob;
import necesse.entity.projectile.CrazedRavenFeatherProjectile;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import stairwaytoheaven.mobs.SkyMobTiers;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * The Ticket Imp — Hell's ranged role, from §17's Infernal Fringe roster. The
 * Department of Eternal Processing (§18) issues numbers; this is what hands
 * them out, from above, at speed, whether or not you asked.
 *
 * <h2>Body</h2>
 * Vanilla's {@code CrazedRavenMob}, the same flying harasser Steinfeld's Grave
 * Crow borrows — the game's own small airborne shooter, and the archetype a
 * darting imp reads as. The projectile is vanilla's own raven feather;
 * <b>both the sheet and the projectile art are placeholders</b> until Hell's
 * own pass lands.
 *
 * <h2>Statline</h2>
 * {@link HellTier}'s ranged row: 5139 HP / 329.5 damage / 81 armour.
 */
public class TicketImpMob extends CrazedRavenMob {

    public static final MaxHealthGetter MAX_HEALTH =
            HellTier.health(SkyMobTiers.ROLE_RANGED_HP);
    public static final GameDamage DAMAGE =
            HellTier.damage(SkyMobTiers.ROLE_RANGED_DAMAGE);
    public static final int ARMOR = HellTier.ARMOR;

    /** 480 base, as the Grave Crow's is, lifted by Hell's x1.55. */
    public static final int AGGRO_RANGE = HellTier.aggro(480);

    public static LootTable lootTable = new LootTable(
            ChanceLootItem.between(0.25F, "realityshard", 1, 2),
            ChanceLootItem.between(0.10F, "oddwood", 1, 2));

    public TicketImpMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedPlayerChaserWandererAI<TicketImpMob>(() -> false, AGGRO_RANGE, 320, 20000, false, false) {
                    @Override
                    public boolean attackTarget(TicketImpMob mob, Mob target) {
                        if (!mob.canAttack()) {
                            return false;
                        }
                        fireTickets(mob, target);
                        this.wanderAfterAttack = GameRandom.globalRandom.getChance(0.75F);
                        return true;
                    }
                });
    }

    /** Two per volley, thirty degrees apart, on OUR damage rather than vanilla's. */
    private static void fireTickets(TicketImpMob mob, Mob target) {
        EntityManager entityManager = mob.getLevel().entityManager;
        mob.attack(target.getX(), target.getY(), false);
        for (int i = 0; i < 2; i++) {
            CrazedRavenFeatherProjectile projectile = new CrazedRavenFeatherProjectile(
                    mob.getLevel(), mob.x, mob.y, target.x, target.y, 80.0F, 576, DAMAGE, mob, 50);
            projectile.setAngle(projectile.getAngle() - 30.0F + (float) (i * 60));
            entityManager.projectiles.add(projectile);
        }
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
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("crazedraven", super.getMobIcon());
    }
}
