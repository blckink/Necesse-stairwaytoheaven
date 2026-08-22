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
 */
public class StormWispMob extends FlyingHostileMob {

    public static GameTexture texture;

    public static LootTable lootTable = new LootTable(LootItem.between("stormshard", 1, 2));
    public static GameDamage damage = new GameDamage(55.0F);

    public StormWispMob() {
        super(280);
        this.setSpeed(35.0F);
        this.setFriction(1.0F);
        this.setKnockbackModifier(0.3F);
        this.setArmor(10);
        this.moveAccuracy = 10;
        this.spawnLightThreshold = new ModifierValue<>(BuffModifiers.MOB_SPAWN_LIGHT_THRESHOLD, 0).min(150, Integer.MAX_VALUE);
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
        int res = texture.getWidth();
        int resHalf = res / 2;
        int drawX = camera.getDrawX(x) - resHalf;
        int drawY = camera.getDrawY(y) - resHalf;
        long time = level.getWorldEntity().getTime();
        float bobbing = GameUtils.getBobbing(time, 1100) * 3.0F;
        float glowPulse = GameUtils.getAnimFloatContinuous(time, 900) / 1.5F;
        final DrawOptions body = texture
                .initDraw()
                .sprite(0, 0, res)
                .startGlowOptions(this, (long) this.getID())
                .light(light)
                .applyEnemyTracker(this, perspective)
                .pos(drawX, (int) (drawY + bobbing));
        GameLight glowLight = light.copy();
        glowLight.setLevel((float) ((int) (glowPulse * 150.0F)));
        final DrawOptions glow = texture
                .initDraw()
                .sprite(0, 1, res)
                .startGlowOptions(this, (long) this.getID())
                .light(glowLight)
                .applyEnemyTracker(this, perspective)
                .pos(drawX, (int) (drawY + bobbing));
        topList.add(tm -> {
            body.draw();
            glow.draw();
        });
    }
}
