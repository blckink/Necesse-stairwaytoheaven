package stairwaytoheaven.mobs;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionShooterPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.hostile.FlyingHostileMob;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.AscendedBoltProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * Storm Wisp — a crackling storm core drifting through the Stormveil, firing
 * spark bolts at range (vanilla Cryo Flake AI pattern).
 *
 * <p><b>Tier: Skyreach floor, ranged role</b> ({@link SkyMobTiers}).
 *
 * <p>The Skyreach floor is vanilla's ORDINARY ascended mob — what a player
 * meets on incursion tier 1, which VERIFIED [jar] applies no multiplier at all
 * ({@code BiomeMissionIncursionData} SUMS its per-tier scaling arrays, and
 * both start at {@code 0.0F}). Measured: {@code AscendedGolemMob.MAX_HEALTH = new
 * MaxHealthGetter(400, 750, 1000, 1300, 1800)} → 1000 HP on CLASSIC, with
 * {@code CrystalGolemMob}'s {@code new GameDamage(130.0F)} and armour 40.
 *
 * <p>The wisp never has to stand in melee, so it takes the ranged discount:
 * x0.70 HP and x0.85 damage of that floor = 700 HP / 110.5 damage, with the
 * floor's armour of 40 (it was 10). The AI shape it copies,
 * {@code CryoFlakeMob}, measures 350 HP / {@code baseDamage} 65 /
 * {@code incursionDamage} 100 / armour 20 — a mid-game mob, borrowed for its
 * behaviour only, never for its numbers.
 *
 * <p>Until this pass it was 280 HP / 55 damage / 10 armour. Loot is unchanged:
 * the Skyreach's drop value is x1.0.
 */
public class StormWispMob extends FlyingHostileMob {

    public static GameTexture texture;

    /** Skyreach drop value is x1.0 — the floor multiplies nothing. */
    public static LootTable lootTable = new LootTable(LootItem.between("stormshard", 1, 2));
    /** Skyreach floor damage 130 ({@code CrystalGolemMob.damage}) x0.85 ranged = 110.5. */
    public static GameDamage damage = SkyMobTiers.damage(SkyMobTiers.SKYREACH_DAMAGE, SkyMobTiers.ROLE_RANGED_DAMAGE);
    /** Skyreach floor 1000 HP ({@code AscendedGolemMob} on CLASSIC) x0.70 ranged = 700. */
    public static final int MAX_HEALTH = SkyMobTiers.hp(SkyMobTiers.SKYREACH_HP, SkyMobTiers.ROLE_RANGED_HP);
    /** Measured 40 on {@code CrystalGolemMob}/{@code AscendedBatMob}; armour has no role modifier. */
    public static final int ARMOR = SkyMobTiers.SKYREACH_ARMOR;

    public StormWispMob() {
        super(MAX_HEALTH);
        this.setSpeed(35.0F);
        this.setFriction(1.0F);
        this.setKnockbackModifier(0.3F);
        this.setArmor(ARMOR);
        this.moveAccuracy = 10;
        // vanilla spawn-light rules: lit areas are safe (see ZephyrRayMob note)
        this.collision = new Rectangle(-14, -14, 28, 28);
        this.hitBox = new Rectangle(-18, -18, 36, 36);
        this.selectBox = new Rectangle(-22, -22, 44, 44);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(
                this,
                new CollisionShooterPlayerChaserWandererAI<StormWispMob>(
                        null, 448, damage, 100, CooldownAttackTargetAINode.CooldownTimer.CAN_ATTACK, 2200, 384, 40000) {
                    public boolean shootAtTarget(StormWispMob mob, Mob target) {
                        if (StormWispMob.this.canAttack()) {
                            StormWispMob.this.startAttackCooldown();
                            mob.getLevel().entityManager.projectiles.add(new AscendedBoltProjectile(
                                    mob.getLevel(), mob.x, mob.y, target.x, target.y, 120.0F, 448, damage, mob));
                            return true;
                        }
                        return false;
                    }
                },
                new FlyingAIMover());
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 20; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(
                            (float) (GameRandom.globalRandom.getIntBetween(5, 20) * (GameRandom.globalRandom.nextBoolean() ? -1 : 1)),
                            (float) (GameRandom.globalRandom.getIntBetween(5, 20) * (GameRandom.globalRandom.nextBoolean() ? -1 : 1)))
                    .color(new Color(140, 122, 235));
        }
    }

    @Override
    protected void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        // Sheet layout (vanilla flying-spirit pattern): column 0 = 4 stacked
        // body frames, column 1 = matching glow overlays.
        int res = 64;
        int resHalf = res / 2;
        int drawX = camera.getDrawX(x) - resHalf;
        int drawY = camera.getDrawY(y) - resHalf;
        long time = level.getWorldEntity().getTime();
        int anim = Math.abs(GameUtils.getAnim(time, 4, 900) - 3);
        float bobbing = GameUtils.getBobbing(time, 1100) * 3.0F;
        float glowPulse = GameUtils.getAnimFloatContinuous(time, 900) / 1.5F;
        final DrawOptions body = texture
                .initDraw()
                .sprite(0, anim, res)
                .startGlowOptions(this, (long) this.getID())
                .light(light)
                .applyEnemyTracker(this, perspective)
                .pos(drawX, (int) (drawY + bobbing));
        GameLight glowLight = light.copy();
        glowLight.setLevel((float) ((int) (glowPulse * 150.0F)));
        final DrawOptions glow = texture
                .initDraw()
                .sprite(1, anim, res)
                .startGlowOptions(this, (long) this.getID())
                .light(glowLight)
                .applyEnemyTracker(this, perspective)
                .pos(drawX, (int) (drawY + bobbing));
        topList.add(tm -> {
            body.draw();
            glow.draw();
        });
    }

    /**
     * Spawns by day as well as by night; placed light still keeps it away.
     * See {@link SkySpawnRules} for the measurement behind this.
     */
    @Override
    public boolean isValidSpawnLocation(necesse.engine.network.server.Server server,
                                        necesse.engine.network.server.ServerClient client,
                                        int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
