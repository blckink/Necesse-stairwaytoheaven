package stairwaytoheaven.arsenal;

import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionShooterPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.hostile.CryoFlakeMob;
import necesse.entity.projectile.CryoMissileProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Aurora Flake — a drifting crystal that hangs over the shoals and throws
 * shards at whatever crosses the mist bank.
 *
 * <p><b>Vanilla base:</b> {@link CryoFlakeMob} for BEHAVIOUR only — the chime
 * sounds, the shatter particles and the spinning two-layer draw. The art is
 * ours: {@code mobs/auroraflake.png}, sampled by {@link #addDrawables}.
 *
 * <p><b>The sheet format.</b> {@code CryoFlakeMob.addDrawables} reads
 * {@code int res = texture.getWidth()} and then draws {@code sprite(0, 0, res)}
 * over {@code sprite(0, 1, res)} (VERIFIED [jar], CryoFlakeMob.java:133-157).
 * So this is not the 384x320 walking grid: it is <b>64x128 — ONE column, TWO
 * cells</b>, a body over a pulse layer, and the cell size is the texture's own
 * width. Both are rotated about {@code (res/2, res/2)}, so anything not centred
 * on that pivot orbits instead of spinning; vanilla's own flake and ours are
 * both centred on (30.5, 30.5) with the pulse layer's 32 sparkles on the arm
 * tips. {@code tools/resheet_mob.py --layout spinner} is what conforms a
 * supplied file to it.
 *
 * <h2>Tier</h2>
 * The ladder and the incursion measurement behind it are written out once, in
 * {@link RimeSentryMob} — the mob that sits on its floor. In short:
 * {@code BiomeMissionIncursionData}'s cumulative per-tier arrays both begin
 * {@code 0.0F}, so incursion tier 1 applies no multiplier at all (VERIFIED
 * [jar]), and the Skyreach — the mod's lowest realm — is pinned to that tier at
 * <b>1000 HP / 130 damage / 40 armour</b>.
 *
 * <p>The Aurora Flake is the Skyreach's fast role: it flies, closes and kites,
 * so it takes the fast discount off the floor — 1000 x 0.6 = <b>600 HP</b> and
 * 130 x 0.8 = 104, snapped onto the ladder's five-step damage grid =
 * <b>105 damage</b>,
 * at the floor's full <b>40 armour</b>. Speed is what it trades bulk for, not
 * protection.
 *
 * <p><b>Why {@code init()} is overridden.</b> Vanilla's flake is a snow
 * deep-cave mob at 350 HP / 65 damage / 20 armour, and that damage is not
 * reachable from a subclass: {@code CryoFlakeMob.init} picks between the
 * statics {@code baseDamage} (65) and {@code incursionDamage} (100) into a
 * local that its anonymous AI closes over. Writing to either static would
 * re-tune every real Cryo Flake in the world, so we rebuild the identical
 * {@code CollisionShooterPlayerChaserWandererAI} + {@code FlyingAIMover} shape
 * against our own {@link #DAMAGE} — the same move {@link RimeSentryMob} makes
 * for its base. Search range, shoot range, cooldown, knockback and missile
 * speed are vanilla's (448 / 384 / 2000ms / 100 / 100.0F) and stay vanilla's;
 * only the damage number changes.
 *
 * <p>The one branch of {@code CryoFlakeMob.init} we do <em>not</em> reproduce is
 * its {@code getLevel() instanceof IncursionLevel} bump to 450 HP / 30 armour:
 * that fires only inside a real incursion, and the mod's realms are ordinary
 * {@code Level}s in their own dimension (VERIFIED [jar]). Should this mob ever
 * be placed on an incursion level, vanilla's bump would <em>demote</em> it —
 * both of those numbers are below this rung.
 *
 * <p>Note the inherited {@code getLootTable} would have handed out
 * {@code glacialshard}, a deep-cave snow material with no place in the sky;
 * ours replaces it outright rather than adding to it.
 */
public class AuroraFlakeMob extends CryoFlakeMob {

    /**
     * Skyreach floor 1000 HP x 0.6 (fast role) = 600 on Classic, with the other
     * four difficulties on the ratios of the getter the floor was measured from
     * — {@code AscendedGolemMob.MAX_HEALTH}'s 0.40 / 0.75 / 1.00 / 1.30 / 1.80
     * around Classic (VERIFIED [jar]). Vanilla's flake is 350.
     */
    public static final MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(240, 450, 600, 780, 1080);

    /**
     * Skyreach floor 130 damage ({@code CrystalGolemMob.damage}, VERIFIED [jar])
     * x 0.8 for the fast role = 104, snapped onto the ladder's five-step damage
     * grid = 105. Vanilla's {@code CryoFlakeMob.baseDamage} (65) and
     * {@code incursionDamage} (100) are both left alone — they are shared
     * statics that the real Cryo Flake reads.
     */
    public static final GameDamage DAMAGE = new GameDamage(105.0F);

    /**
     * The floor's armour, unreduced: {@code CrystalGolemMob} sets 40, the
     * rolling {@code CrystalArmadillo} rolls at 40 and {@code AscendedBatMob}
     * wears 40 (VERIFIED [jar]). Vanilla's flake wears 20.
     */
    public static final int ARMOR = 40;

    /**
     * Our sheet, filled by {@code SkyMobs.loadTextures} on the client only. It
     * stays null on a dedicated server, which never draws, hence the guard in
     * {@link #addDrawables}.
     */
    public static GameTexture texture;

    /**
     * Vanilla's {@code CryoFlakeMob.addDrawables}, ported line for line with
     * our sheet in place of {@code MobRegistry.Textures.cryoFlake}.
     *
     * <p>The override exists for the same reason the Fen Wraith's does: vanilla
     * reads its texture from a static inline and {@code Mob} exposes no
     * per-instance texture hook, so the only way to change the art without
     * repainting every real Cryo Flake in the world is to redraw the mob
     * ourselves (VERIFIED [jar]).
     *
     * <p>It deliberately does NOT call {@code super.addDrawables}: that IS
     * vanilla's draw, and calling it would put the blue Cryo Flake underneath
     * ours. Nothing is lost by dropping it. {@code CryoFlakeMob}'s own first
     * line is {@code super.addDrawables(...)}, and nothing between it and
     * {@code Mob} overrides the method — {@code FlyingHostileMob},
     * {@code FlyingTargetMob} and {@code AttackAnimMob} all inherit it
     * untouched, and {@code Mob.addDrawables} has an empty body (VERIFIED
     * [jar], Mob.java:1734-1745). Health and status bars come from
     * {@code Mob.addDrawablesLoop} around this call, not from inside it, so
     * they are unaffected — the same finding the Fen Wraith's override rests
     * on.
     */
    @Override
    protected void addDrawables(List<MobDrawable> list, OrderableDrawables tileList,
            OrderableDrawables topList, Level level, int x, int y,
            TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int res = texture.getWidth();
        int resHalf = res / 2;
        int drawX = camera.getDrawX(x) - resHalf;
        int drawY = camera.getDrawY(y) - resHalf;
        long time = level.getWorldEntity().getTime();
        float rotation = GameUtils.getTimeRotation(time, 4);
        float glowLight = GameUtils.getAnimFloatContinuous(time, 1000) / 1.5F;
        DrawOptions body = texture.initDraw()
                .sprite(0, 0, res)
                .rotate(rotation * (float) (this.dx < 0.0F ? -1 : 1), resHalf, resHalf)
                .startGlowOptions(this, (long) this.getID())
                .light(light)
                .applyEnemyTracker(this, perspective)
                .pos(drawX, drawY);
        GameLight glowLightLevel = light.copy();
        glowLightLevel.setLevel((float) ((int) (glowLight * 150.0F)));
        DrawOptions glow = texture.initDraw()
                .sprite(0, 1, res)
                .rotate(rotation * (float) (this.dx < 0.0F ? -1 : 1), resHalf, resHalf)
                .startGlowOptions(this, (long) this.getID())
                .light(glowLightLevel)
                .applyEnemyTracker(this, perspective)
                .pos(drawX, drawY);
        topList.add(tm -> {
            body.draw();
            glow.draw();
        });
    }

    /**
     * The Prismcaller is built out of prismshards, and this is the mob that
     * carries them in the air rather than in a rock.
     *
     * <p>Quantities are unchanged on purpose: the Skyreach sits at drop-value
     * x1.0 because it <em>is</em> the baseline the deeper realms multiply
     * against.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.65F, LootItem.between("prismshard", 1, 3)),
            new ChanceLootItemList(0.35F, LootItem.between("aurorapetal", 1, 2)));

    public AuroraFlakeMob() {
        super();
        // Registered in construction the way AscendedGolemMob registers its own
        // MAX_HEALTH: MobDifficultyChanges throws if it is touched after init().
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(
                this,
                new CollisionShooterPlayerChaserWandererAI<AuroraFlakeMob>(
                        null, 448, DAMAGE, 100,
                        CooldownAttackTargetAINode.CooldownTimer.CAN_ATTACK, 2000, 384, 40000) {
                    @Override
                    public boolean shootAtTarget(AuroraFlakeMob mob, Mob target) {
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
