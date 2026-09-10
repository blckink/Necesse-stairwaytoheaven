package stairwaytoheaven.mobs;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionShooterPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.hostile.CryoFlakeMob;
import necesse.entity.projectile.CryoMissileProjectile;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * A Vatling — one of the floating things the {@link SourvatBloomMob} keeps
 * letting out of the burst vat.
 *
 * <p>{@code chapter-01-skyreach-cast.md} §2 puts the adds on <i>"the small
 * floating flake archetype ({@code CryoFlakeMob}, which {@code AuroraFlakeMob}
 * already proves)"</i>. This is that mob with the Bloom's numbers: the AI is
 * {@code AuroraFlakeMob}'s re-declaration of vanilla's shooter tree against our
 * own damage, and nothing else changes.
 *
 * <h2>The art: none, and deliberately not even a recolour</h2>
 * {@code AuroraFlakeMob} carries a supplied sheet and overrides
 * {@code addDrawables} to wear it. This one does NOT: it inherits
 * {@code CryoFlakeMob.addDrawables} whole and is drawn out of
 * {@code MobRegistry.Textures.cryoFlake}, which is §1.3's pattern and costs
 * zero pixels. {@link #getMobIcon} hands the bestiary the cryo flake's face.
 *
 * <h2>Tier: an ADD, not an encounter</h2>
 * Half the Skyreach's fast rung — {@code hp(SKYREACH_HP, ROLE_FAST_HP) / 2}.
 * The ladder's role percentages price a mob you meet on its own; a Vatling is
 * never met on its own, because the Bloom makes another one every time it is
 * hit (up to {@link SourvatBloomMob#MAX_VATLINGS}). Five adds at the full fast
 * rung would be five times a stand-alone encounter stacked on top of an elite
 * boss in a 35-tile room. Halved, the swarm as a whole lands at about two and
 * a half fast mobs, which is what the room can hold. Damage and armour are the
 * fast rung's own, so a Vatling still hurts; it just does not also tank.
 * Vanilla's flake is 350 HP / 65 damage / 20 armour and stays exactly that.
 */
public class VatlingMob extends CryoFlakeMob {

    /** Half the Skyreach fast rung on Classic — see the class comment. */
    public static final MaxHealthGetter MAX_HEALTH = SkyMobTiers.scaled(
            SkyMobTiers.hp(SkyMobTiers.SKYREACH_HP, SkyMobTiers.ROLE_FAST_HP) / 2);

    /** The fast role's damage, unreduced. */
    public static final GameDamage DAMAGE = SkyMobTiers.damage(
            SkyMobTiers.SKYREACH_DAMAGE, SkyMobTiers.ROLE_FAST_DAMAGE);

    /** The band's armour. */
    public static final int ARMOR = SkyMobTiers.SKYREACH_ARMOR;

    /** The chaser tree's own range argument: the measured 448 x1.25 = 560. */
    public static final int AGGRO_RANGE = SkyMobTiers.aggro(448, SkyMobTiers.UPLIFT_SKYREACH_AGGRO);

    /**
     * What was in the vat. Small, because there are up to five of them — the
     * Bloom itself carries the Mother.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.45F, LootItem.between("cloudberry", 1, 2)),
            new ChanceLootItemList(0.20F, new LootItem("windsilk")));

    public VatlingMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        // A Vatling exists because the Bloom made it. Left to despawn it would
        // evaporate the moment the player backed out of the cellar door.
        this.canDespawn = false;
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(
                this,
                new CollisionShooterPlayerChaserWandererAI<VatlingMob>(
                        null, AGGRO_RANGE, DAMAGE, 100,
                        CooldownAttackTargetAINode.CooldownTimer.CAN_ATTACK, 2000, 384, 40000) {
                    @Override
                    public boolean shootAtTarget(VatlingMob mob, Mob target) {
                        if (!mob.canAttack()) {
                            return false;
                        }
                        mob.attackSoundAbility.runAndSend();
                        mob.startAttackCooldown();
                        mob.getLevel().entityManager.projectiles.add(new CryoMissileProjectile(
                                mob.getLevel(), mob, mob.x, mob.y, target.x, target.y, 100.0F, 448, DAMAGE, 100));
                        return true;
                    }
                },
                new FlyingAIMover());
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * It is never spawned by the spawner — the Bloom makes it — but the answer
     * has to be honest anyway, and it is the same one every sky flier gives.
     * See {@link SkySpawnRules}.
     */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }

    /** The face of the creature whose body it wears. See {@link BorrowedMobIcon}. */
    @Override
    public GameTexture getMobIcon() {
        return BorrowedMobIcon.from("cryoflake", super.getMobIcon());
    }
}
