package stairwaytoheaven.arsenal;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.StationaryPlayerShooterAI;
import necesse.entity.mobs.hostile.FrostSentryMob;
import necesse.entity.projectile.FrostSentryProjectile;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Rime Sentry — a piece of Skywatch frost machinery still standing on the
 * causeways, still firing at anything that walks past.
 *
 * <p><b>Vanilla base:</b> {@link FrostSentryMob}, whose art is
 * {@code mobs/frostsentry} (a pale ice-blue crystal bud, verified against the
 * sprite dump — it needs no colour shift to sit in the Skyreach palette). The
 * class is subclassed rather than copied, so the rendering, the wobble
 * animation, the human shadow, the death particles, {@code canBePushed = false}
 * and the ground-pillar trail its projectile leaves all come from vanilla
 * unchanged.
 *
 * <p><b>What is overridden and why.</b> Vanilla's sentry is a snow-cave mob at
 * 120 HP and 17 damage; the Skyreach's own roster runs 180-520 HP and 45-70
 * damage (Zephyr Ray 220/45, Galehound 260/50, Storm Wisp 280/55, Skystone
 * Golem 520/70). Its damage lives in a {@code public static GameDamage} field
 * that the AI closes over, so it cannot be re-tuned per subclass without
 * mutating the vanilla static — which would change the real Frost Sentry in
 * every snow deep cave in the world. {@code init()} therefore rebuilds the
 * same {@code StationaryPlayerShooterAI} shape against our own damage, and
 * {@code setMaxHealth}/{@code setHealthHidden} are applied the way
 * {@code CryoFlakeMob.init} applies its own incursion bump.
 */
public class RimeSentryMob extends FrostSentryMob {

    /** Between the Storm Wisp (55) and the Skystone Golem (70): it cannot move. */
    public static final GameDamage DAMAGE = new GameDamage(58.0F);
    /** Immobile, so it is squishier than anything that can chase you. */
    public static final int HEALTH = 210;

    /**
     * Fulgurite is what the Skyreave and the Thunderhead are banded with, and
     * a storm shard is what the machinery ran on. Killing sentries is the
     * fulgurite route that does not need a pickaxe.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.7F, LootItem.between("fulgurite", 1, 2)),
            new ChanceLootItemList(0.4F, LootItem.between("stormshard", 1, 2)));

    public RimeSentryMob() {
        super();
        this.setArmor(12);
    }

    @Override
    public void init() {
        super.init();
        this.setMaxHealth(HEALTH);
        this.setHealthHidden(this.getMaxHealth());
        this.ai = new BehaviourTreeAI<>(this, new StationaryPlayerShooterAI<RimeSentryMob>(352) {
            @Override
            public void shootTarget(RimeSentryMob mob, Mob target) {
                FrostSentryProjectile projectile = new FrostSentryProjectile(
                        mob.getLevel(), mob, mob.x, mob.y, target.x, target.y, 78.0F, 544, DAMAGE, 50);
                projectile.x -= projectile.dx * 20.0F;
                projectile.y -= projectile.dy * 20.0F;
                RimeSentryMob.this.attack((int) (mob.x + projectile.dx * 100.0F),
                        (int) (mob.y + projectile.dy * 100.0F), false);
                mob.getLevel().entityManager.projectiles.add(projectile);
            }
        });
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * Same rule the rest of the sky roster uses: the Skyreach is dangerous at
     * noon, and placed light still keeps it clear. See {@link SkySpawnRules}.
     */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
