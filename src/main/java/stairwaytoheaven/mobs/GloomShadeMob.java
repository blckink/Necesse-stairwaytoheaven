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
 * Gloom Shade — a hooded wisp of the fen that drifts out of the dark and
 * claws at the living. The Veil's bread-and-butter enemy: only spawns in
 * darkness, so every lantern carves out real safety.
 *
 * <p><b>Tier: Ghost Realm (the Veil), standard role</b> ({@link SkyMobTiers}).
 * The Veil sits a whole realm above the Skyreach, at roughly incursion 7 on
 * vanilla's own curve. VERIFIED [jar]: {@code BiomeMissionIncursionData} SUMS
 * its per-tier scaling arrays, and by tier 7 {@code healthScalingPerTier}
 * totals +1.80 (x2.80) and {@code damageScalingPerTier} +0.75 (x1.75) —
 * against +3.00 (x4.00) and +1.15 (x2.15) at tier 10. Applied to the Skyreach
 * floor — the ordinary ascended mob, measured
 * {@code AscendedGolemMob.MAX_HEALTH = new MaxHealthGetter(400, 750, 1000,
 * 1300, 1800)} → 1000 HP on CLASSIC with {@code CrystalGolemMob}'s
 * {@code new GameDamage(130.0F)} and armour 40 — that is 2800 HP and 227.5
 * damage, which the ladder rounds to a flat 230, over 55 armour. The shade is
 * the fen's standard enemy and carries no role modifier, so it IS the Veil's
 * floor.
 *
 * <p>Until this pass it was 240 HP / 50 damage / no armour, a fifth of an
 * ordinary ascended mob. Its drops are lifted by the realm's x1.9 drop value.
 */
public class GloomShadeMob extends HostileMob {

    public static GameTexture texture;

    /**
     * Ghost Realm drop value is x1.9, so the essence roll goes 1-2 → 2-4
     * (1 x 1.9 = 1.9, 2 x 1.9 = 3.8). The 0.6 chance is the mob's identity and
     * stays as it was — only the quantity carries the tier.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.6F, LootItem.between("veilessence",
                    SkyMobTiers.drop(1, SkyMobTiers.VEIL_DROP_VALUE),
                    SkyMobTiers.drop(2, SkyMobTiers.VEIL_DROP_VALUE))));
    /**
     * Veil floor damage: the Skyreach floor's measured
     * {@code CrystalGolemMob.damage} of 130 x1.75 = 227.5, rounded to 230.
     * Cross-checked against the ascended fliers:
     * {@code NightSwarmBatMob.COLLISION_DAMAGE} measures
     * {@code new GameDamage(115.0F)}, so a shade hits exactly twice as hard.
     */
    public static GameDamage damage = new GameDamage(SkyMobTiers.VEIL_DAMAGE);
    /** Veil floor HP: 1000 ({@code AscendedGolemMob} on CLASSIC) x2.8 = 2800. */
    public static final int MAX_HEALTH = SkyMobTiers.VEIL_HP;
    /** One realm step over the ascended 40 measured on golem and both bats. */
    public static final int ARMOR = SkyMobTiers.VEIL_ARMOR;

    public GloomShadeMob() {
        super(MAX_HEALTH);
        this.setSpeed(40.0F);
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
        return 8;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 12; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(this.getRandomDeathSpeed(knockbackX), this.getRandomDeathSpeed(knockbackY))
                    .color(new Color(150, 200, 170));
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
        float bobbing = GameUtils.getBobbing(this.getWorldEntity().getTime(), 1300) * 3.0F;
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
