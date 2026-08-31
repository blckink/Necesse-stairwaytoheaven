package stairwaytoheaven.mobs;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.buffs.BuffModifiers;
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
 * Zephyr Ray — a sleek manta gliding over the Mistsea that swoops at intruders.
 * Fast, fragile melee flier of the Driftlands (vanilla crypt bat AI pattern).
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
 * <p>"Fragile" is now measured against that floor rather than against nothing:
 * the fast role's x0.60 HP and x0.80 damage give 600 HP / 104 damage, with the
 * floor's armour of 40 (it had none). Both ascended fliers agree on the
 * bracket — {@code AscendedBatMob.COLLISION_DAMAGE = new GameDamage(90.0F)}
 * and {@code NightSwarmBatMob}'s {@code 115.0F}, each with armour 40.
 *
 * <p>Until this pass it was 220 HP / 45 damage / no armour. Loot is unchanged:
 * the Skyreach's drop value is x1.0.
 */
public class ZephyrRayMob extends HostileMob {

    public static GameTexture texture;

    /** Skyreach drop value is x1.0 — the floor multiplies nothing. */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.6F, LootItem.between("windsilk", 1, 2)));
    /** Skyreach floor damage 130 ({@code CrystalGolemMob.damage}) x0.80 fast = 104. */
    public static GameDamage damage = SkyMobTiers.damage(SkyMobTiers.SKYREACH_DAMAGE, SkyMobTiers.ROLE_FAST_DAMAGE);
    /** Skyreach floor 1000 HP ({@code AscendedGolemMob} on CLASSIC) x0.60 fast = 600. */
    public static final int MAX_HEALTH = SkyMobTiers.hp(SkyMobTiers.SKYREACH_HP, SkyMobTiers.ROLE_FAST_HP);
    /** Measured 40 on {@code AscendedBatMob}/{@code NightSwarmBatMob}; armour has no role modifier. */
    public static final int ARMOR = SkyMobTiers.SKYREACH_ARMOR;

    public ZephyrRayMob() {
        super(MAX_HEALTH);
        // Difficulty curve on vanilla's own ratios, so this rung holds on all
        // five difficulties rather than only Classic. Must be set here:
        // MobDifficultyChanges throws if it is touched after init().
        this.difficultyChanges.setMaxHealth(SkyMobTiers.scaled(MAX_HEALTH));
        this.setSpeed(52.0F);
        this.setFriction(2.0F);
        this.setArmor(ARMOR);
        // Vanilla spawn-light rules: torch-lit and daylit areas stay SAFE.
        // (v0.1 raised the threshold for daytime spawns — playtests showed that
        // breaks the game's core "light = safety" contract, so nights and dark
        // corners are the Skyreach's danger windows now.)
        this.collision = new Rectangle(-10, -7, 20, 14);
        this.hitBox = new Rectangle(-14, -12, 28, 24);
        this.selectBox = new Rectangle(-16, -40, 32, 40);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this, new ConfusedCollisionPlayerChaserWandererAI<>(null, 512, damage, 80, 40000));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public int getFlyingHeight() {
        return 18;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 12; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(this.getRandomDeathSpeed(knockbackX), this.getRandomDeathSpeed(knockbackY))
                    .color(new Color(203, 216, 226));
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
        int drawY = camera.getDrawY(y) - 51;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        float bobbing = GameUtils.getBobbing(this.getWorldEntity().getTime(), 900) * 4.0F;
        drawY = (int) ((float) drawY + bobbing);
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
