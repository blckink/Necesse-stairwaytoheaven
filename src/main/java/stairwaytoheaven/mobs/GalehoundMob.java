package stairwaytoheaven.mobs;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.hostile.HostileMob;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * Galehound — a wind-wolf that hunts the Driftlands meadows at night. Fast
 * ground melee in packs (vanilla night-predator pattern); daylight and torch
 * light keep it away like every sky spawn.
 *
 * <p><b>Tier: Skyreach floor, fast role</b> ({@link SkyMobTiers}).
 *
 * <p>The Skyreach floor is vanilla's ORDINARY ascended mob — what a player
 * meets on incursion tier 1, which VERIFIED [jar] applies no multiplier at all
 * ({@code BiomeMissionIncursionData} SUMS its per-tier scaling arrays, and
 * both start at {@code 0.0F}). Measured: {@code AscendedGolemMob.MAX_HEALTH = new
 * MaxHealthGetter(400, 750, 1000, 1300, 1800)} → 1000 HP on CLASSIC, with
 * {@code CrystalGolemMob}'s {@code new GameDamage(130.0F)} and armour 40.
 *
 * <p>The hound trades staying power for speed, so it takes the fast role's
 * x0.60 HP and x0.80 damage of that floor = 600 HP / 104 damage, and the
 * floor's armour of 40 (it had none). 104 sits between the two measured
 * ascended fliers, {@code AscendedBatMob.COLLISION_DAMAGE = new
 * GameDamage(90.0F)} and {@code NightSwarmBatMob}'s {@code 115.0F}, both of
 * which also carry armour 40.
 *
 * <p>Until this pass it was 260 HP / 50 damage / no armour. Loot is unchanged:
 * the Skyreach's drop value is x1.0.
 */
public class GalehoundMob extends HostileMob {

    public static GameTexture texture;

    /** Skyreach drop value is x1.0 — the floor multiplies nothing. */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.6F, LootItem.between("windsilk", 1, 2)),
            new ChanceLootItemList(0.25F, LootItem.between("cloudberry", 1, 2)));
    /** Skyreach floor damage 130 ({@code CrystalGolemMob.damage}) x0.80 fast = 104. */
    public static GameDamage damage = SkyMobTiers.damage(SkyMobTiers.SKYREACH_DAMAGE, SkyMobTiers.ROLE_FAST_DAMAGE);
    /** Skyreach floor 1000 HP ({@code AscendedGolemMob} on CLASSIC) x0.60 fast = 600. */
    public static final int MAX_HEALTH = SkyMobTiers.hp(SkyMobTiers.SKYREACH_HP, SkyMobTiers.ROLE_FAST_HP);
    /** Measured 40 on {@code AscendedBatMob}/{@code NightSwarmBatMob}; armour has no role modifier. */
    public static final int ARMOR = SkyMobTiers.SKYREACH_ARMOR;

    public GalehoundMob() {
        super(MAX_HEALTH);
        this.setSpeed(58.0F);
        this.setFriction(3.0F);
        this.setArmor(ARMOR);
        this.collision = new Rectangle(-12, -8, 24, 16);
        this.hitBox = new Rectangle(-14, -12, 28, 24);
        this.selectBox = new Rectangle(-16, -34, 32, 42);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this, new ConfusedCollisionPlayerChaserWandererAI<>(null, 512, damage, 90, 40000));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 14; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(this.getRandomDeathSpeed(knockbackX), this.getRandomDeathSpeed(knockbackY))
                    .color(new Color(172, 186, 202));
        }
    }

    private float getRandomDeathSpeed(float knockback) {
        return knockback / 2.0F + (float) (necesse.engine.util.GameRandom.globalRandom.getIntBetween(5, 15)
                * (necesse.engine.util.GameRandom.globalRandom.nextBoolean() ? -1 : 1));
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 32;
        int drawY = camera.getDrawY(y) - 44;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        final TextureDrawOptionsEnd drawOptions = texture
                .initDraw()
                .sprite(sprite.x, sprite.y, 64)
                .startGlowOptions(this, (long) this.getID())
                .light(light)
                .applyEnemyTracker(this, perspective)
                .pos(drawX, drawY);
        list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
                drawOptions.draw();
            }
        });
        this.addShadowDrawables(tileList, level, x, y, light, camera);
    }
}
