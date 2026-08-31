package stairwaytoheaven.arsenal;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.decorators.FailerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TeleportOnProjectileHitAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.mobs.hostile.AncientSkeletonMageMob;
import necesse.entity.projectile.AncientSkeletonMageProjectile;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Cinder Cantor — a masked singer of the old rite, still walking the ash. The
 * Ashen Reach's first real inhabitant: until now only stray Gloom Shades
 * wandered in from the fen.
 *
 * <p><b>Vanilla base:</b> {@link AncientSkeletonMageMob}, art
 * {@code mobs/ancientskeletonmage} plus its two arm sheets, composed through
 * {@code HumanDrawOptions} (a bone-and-ash robed figure with a crimson-trimmed
 * mantle — already the Ashen Reach's palette, no colour shift needed).
 * Subclassing keeps its whole character: a caster that shoots
 * {@code AncientSkeletonMageProjectile} at range 640 AND owns a
 * {@code TeleportOnProjectileHitAINode} — get hit by one of ITS bolts and it
 * blinks away in a puff of smoke, which is what makes it a different fight
 * from anything else in the mod.
 *
 * <h2>Tier</h2>
 * The ladder and the incursion measurement behind it are written out once, in
 * {@link RimeSentryMob} — the mob that sits on its floor. In short: incursion
 * tier 1 applies no multiplier at all (VERIFIED [jar]:
 * {@code BiomeMissionIncursionData}'s cumulative per-tier arrays both begin
 * {@code 0.0F}), which pins the Skyreach floor at 1000 HP / 130 damage / 40
 * armour, and summing those same arrays to <b>incursion tier 7</b> gives +1.80
 * health / +0.75 damage — the Ghost Realm's rung of 2800 HP / 230 damage /
 * 55 armour (armour has no incursion array and is walked up by hand).
 *
 * <p>The Cinder Cantor is the Ghost Realm's ranged role, so it takes the ranged
 * discount off that rung: 2800 x 0.7 = <b>1960 HP</b> and 230 x 0.85 = 195.5 →
 * <b>195 damage</b>, at the rung's full <b>55 armour</b>. Vanilla's own mage is
 * a ruins mob at 400 HP / 90 damage / 25 armour and stays exactly that; only
 * this subclass moves.
 *
 * <p><b>Why {@code init()} is overridden.</b> The bolt's damage is a
 * {@code new GameDamage(90.0F)} built inline inside vanilla's anonymous
 * {@code ConfusedPlayerChaserWandererAI} (VERIFIED [jar]), with no field to
 * write through, so the whole tree — chaser, bolt, and the
 * {@code TeleportOnProjectileHitAINode} that gives the mob its character — is
 * re-declared here against {@link #DAMAGE}. Ranges, cooldowns, projectile speed
 * and the teleport's 3s/7-tile window are vanilla's and stay vanilla's; only
 * the damage number changes.
 *
 * <p>Vanilla drops plain bone; ours keeps the bone (the Veil has no other
 * source) and adds the two Veil materials.
 */
public class CinderCantorMob extends AncientSkeletonMageMob {

    /**
     * Ghost Realm rung (incursion tier 7) 2800 x 0.7 (ranged role) = <b>1960</b>
     * on Classic. The other four difficulties reuse the ratios of the getter the
     * floor was measured from — {@code AscendedGolemMob.MAX_HEALTH}'s
     * 0.40 / 0.75 / 1.00 / 1.30 / 1.80 around Classic (VERIFIED [jar]).
     * Vanilla's mage is 400.
     */
    public static final MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(780, 1470, 1960, 2550, 3530);

    /**
     * Ghost Realm rung 230 x 0.85 (ranged role) = 195.5, snapped onto the
     * ladder's five-step damage grid = <b>195</b>; the rung's 230 is
     * {@code CrystalGolemMob.damage} (130, VERIFIED [jar]) run out to incursion
     * tier 7. Vanilla builds 90 inline inside its AI.
     */
    public static final GameDamage DAMAGE = new GameDamage(195.0F);

    /**
     * Ghost Realm rung = <b>55 armour</b>. There is no armour array in
     * {@code BiomeMissionIncursionData} (VERIFIED [jar]: it scales health and
     * damage only), so the ladder walks armour up by hand from the floor's 40 —
     * {@code CrystalGolemMob}'s {@code setArmor(40)}, matched by the rolling
     * {@code CrystalArmadillo} and {@code AscendedBatMob}. Vanilla's mage
     * wears 25.
     */
    public static final int ARMOR = 55;

    /**
     * Quantities are the Skyreach baseline x the Ghost Realm's drop-value
     * multiplier of 1.9, rounded to whole items: bone 1-3 becomes 2-6, and the
     * two Veil materials go 1-2 to 2-4. Chances are unchanged — the rung is
     * paid in stack size, so the drop still has to be earned.
     */
    public static LootTable lootTable = new LootTable(
            LootItem.between("bone", 2, 6),
            new ChanceLootItemList(0.55F, LootItem.between("cinderpearl", 2, 4)),
            new ChanceLootItemList(0.45F, LootItem.between("veilessence", 2, 4)));

    public CinderCantorMob() {
        super();
        // Registered in construction the way AscendedGolemMob registers its own
        // MAX_HEALTH: MobDifficultyChanges throws if it is touched after init().
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        // Vanilla's tree rebuilt one-for-one (640 search / 320 shoot / 40s
        // wander, 120.0F bolt at range 640 with 50 knockback, teleport on a 3s
        // cooldown within 7 tiles) against our own damage.
        ConfusedPlayerChaserWandererAI<CinderCantorMob> chaserAI =
                new ConfusedPlayerChaserWandererAI<CinderCantorMob>(null, 640, 320, 40000, false, false) {
                    @Override
                    public boolean attackTarget(CinderCantorMob mob, Mob target) {
                        if (!mob.canAttack()) {
                            return false;
                        }
                        mob.attack(target.getX(), target.getY(), false);
                        mob.getLevel().entityManager.projectiles.add(new AncientSkeletonMageProjectile(
                                mob.getLevel(), mob, mob.x, mob.y, target.x, target.y, 120.0F, 640, DAMAGE, 50));
                        this.wanderAfterAttack = GameRandom.globalRandom.getChance(0.75F);
                        return true;
                    }
                };
        chaserAI.addChildFirst(new FailerAINode<>(new TeleportOnProjectileHitAINode<CinderCantorMob>(3000, 7) {
            @Override
            public boolean teleport(CinderCantorMob mob, int x, int y) {
                if (mob.isServer()) {
                    mob.teleportAbility.runAndSend(x, y);
                    this.getBlackboard().mover.stopMoving(mob);
                }
                return true;
            }
        }));
        this.ai = new BehaviourTreeAI<>(this, chaserAI);
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
