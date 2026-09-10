package stairwaytoheaven.mobs;

import java.awt.Point;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.decorators.FailerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TeleportOnProjectileHitAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.hostile.AncientSkeletonMageMob;
import necesse.entity.projectile.AncientSkeletonMageProjectile;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * Prototype Nine — Ossian Vane's own security demonstrator, which woke up and
 * locked the workshop from the inside.
 *
 * <p>{@code chapter-01-skyreach-cast.md} §2: <i>"ranged — plays dead among the
 * wrecks, then fights at distance with a winding arc"</i>, built on
 * <i>"vanilla's caster archetype ({@code AncientSkeletonMageMob}, the pattern
 * the Cinder Cantor already uses)"</i>. It lies at (9,15) of §2.14's plan, in
 * the largest crater among the wrecks, and is placed by
 * {@code SkyLandmarkPois} alone.
 *
 * <h2>The art: none</h2>
 * {@link stairwaytoheaven.arsenal.CinderCantorMob} is the same subclass with a
 * supplied sheet, and overrides {@code addDrawables} and
 * {@code spawnDeathParticles} to wear it. This one deliberately does NOT: it
 * inherits both, so it is drawn and shattered out of
 * {@code MobRegistry.Textures.ancientSkeletonMage} by vanilla's own code —
 * §1.3's pattern, zero new pixels. That a half-buried machine reads as an
 * ancient mage is the stand-in this build accepts and
 * {@code docs/VANILLA_ASSET_MAP.md} records; {@link #getMobIcon} keeps the
 * bestiary honest about it rather than showing the ERR tile.
 *
 * <h2>Tier</h2>
 * The Skyreach's RANGED rung: {@code hp(SKYREACH_HP, ROLE_RANGED_HP)} health,
 * {@code damage(SKYREACH_DAMAGE, ROLE_RANGED_DAMAGE)} on the bolt, the band's
 * armour. The Cinder Cantor stands on the same rung one realm deeper; the
 * arithmetic and the incursion measurement behind both are written out once in
 * {@code arsenal/RimeSentryMob}. Vanilla's mage is a ruins mob at 400 HP / 90
 * damage / 25 armour and stays exactly that.
 *
 * <h2>Why {@code init()} is overridden</h2>
 * The same reason the Cinder Cantor's is: the bolt's damage is a
 * {@code new GameDamage(90.0F)} built inline inside vanilla's anonymous
 * {@code ConfusedPlayerChaserWandererAI} with no field to write through, so the
 * whole tree — chaser, bolt, and the {@code TeleportOnProjectileHitAINode} that
 * makes it blink away when its own bolt comes back at it — is re-declared here
 * against {@link #DAMAGE}. Ranges, cooldowns, projectile speed and the
 * teleport's 3s/7-tile window are vanilla's and stay vanilla's.
 */
public class PrototypeNineMob extends AncientSkeletonMageMob {

    /** Skyreach ranged rung on Classic, with vanilla's own five-difficulty ratios. */
    public static final MaxHealthGetter MAX_HEALTH = SkyMobTiers.scaled(
            SkyMobTiers.hp(SkyMobTiers.SKYREACH_HP, SkyMobTiers.ROLE_RANGED_HP));

    /** The ranged role's damage. Vanilla builds 90 inline inside its AI. */
    public static final GameDamage DAMAGE = SkyMobTiers.damage(
            SkyMobTiers.SKYREACH_DAMAGE, SkyMobTiers.ROLE_RANGED_DAMAGE);

    /** The band's armour. Vanilla's mage wears 25. */
    public static final int ARMOR = SkyMobTiers.SKYREACH_ARMOR;

    /** The chaser tree's own range argument: the measured 640 x1.25 = 800. */
    public static final int AGGRO_RANGE = SkyMobTiers.aggro(640, SkyMobTiers.UPLIFT_SKYREACH_AGGRO);

    /**
     * §2's drop column: <i>"Storm Lens Core (once), Aetherwright's Casing
     * (repeatable), scrap"</i>.
     *
     * <p>The Core is guaranteed rather than a chance roll, for the reason the
     * Mother is on the Sourvat Bloom: it is Ossian Vane's recruit key, and a
     * failed roll would be a world in which he can never be hired. The Casing
     * is the repeatable half — §3 calls this mob the standing source of the
     * gate material for the tier past Stormsteel, which is what makes the test
     * range worth re-entering once the place has re-armed.
     */
    public static LootTable lootTable = new LootTable(
            new LootItem("stormlenscore"),
            LootItem.between("aetherwrightcasing", 1, 2),
            LootItem.between("stormglass", 2, 5),
            new ChanceLootItemList(0.50F, LootItem.between("stormsteelbar", 1, 2)),
            ChanceLootItem.between(0.70F, "coin", 300, 900));

    public PrototypeNineMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        ConfusedPlayerChaserWandererAI<PrototypeNineMob> chaserAI =
                new ConfusedPlayerChaserWandererAI<PrototypeNineMob>(null, AGGRO_RANGE, 320, 40000, false, false) {
                    @Override
                    public boolean attackTarget(PrototypeNineMob mob, Mob target) {
                        if (!mob.canAttack()) {
                            return false;
                        }
                        mob.attack(target.getX(), target.getY(), false);
                        mob.getLevel().entityManager.projectiles.add(new AncientSkeletonMageProjectile(
                                mob.getLevel(), mob, mob.x, mob.y, target.x, target.y, 120.0F, 640, DAMAGE, 50));
                        this.wanderAfterAttack = GameRandom.globalRandom.getChance(0.75F);
                        return true;
                    }

                    /**
                     * PLAYERS only. Vanilla's default stream is
                     * {@code streamPlayersAndHumans}, which accepts any visible
                     * human on a team — and Ossian Vane is standing 14 tiles
                     * away inside the same workshop. A demonstrator that spends
                     * the fight shooting at an immortal instrumentwright is
                     * neither the encounter §2.14 describes nor one the player
                     * can win their way into.
                     */
                    @Override
                    public GameAreaStream<Mob> streamPossibleTargets(PrototypeNineMob mob,
                            Point base, TargetFinderDistance<PrototypeNineMob> distance) {
                        return super.streamPossibleTargets(mob, base, distance)
                                .filter(m -> m.isPlayer);
                    }
                };
        chaserAI.addChildFirst(new FailerAINode<>(new TeleportOnProjectileHitAINode<PrototypeNineMob>(3000, 7) {
            @Override
            public boolean teleport(PrototypeNineMob mob, int x, int y) {
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

    /** The Stormveil's own static-light rule, like every other sky hostile. */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }

    /** The face of the creature whose body it wears. See {@link BorrowedMobIcon}. */
    @Override
    public GameTexture getMobIcon() {
        return BorrowedMobIcon.from("ancientskeletonmage", super.getMobIcon());
    }
}
