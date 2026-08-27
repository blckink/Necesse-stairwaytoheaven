package stairwaytoheaven.mobs;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameUtils;
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
 * Dawnpiercer — a crystal-crested dive bird of the Aurora Shoals: fast and
 * fragile with a hard-hitting swoop (glass-cannon counterpart to the golem's
 * tankiness). Same melee-flier pattern as the Zephyr Ray.
 */
public class DawnpiercerMob extends HostileMob {

    public static GameTexture texture;

    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.5F, LootItem.between("prismshard", 1, 2)),
            new ChanceLootItemList(0.4F, LootItem.between("aurorapetal", 1, 1)));
    public static GameDamage damage = new GameDamage(60.0F);

    public DawnpiercerMob() {
        super(180);
        this.setSpeed(64.0F);
        this.setFriction(2.0F);
        this.collision = new Rectangle(-10, -7, 20, 14);
        this.hitBox = new Rectangle(-14, -12, 28, 24);
        this.selectBox = new Rectangle(-16, -40, 32, 40);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this, new ConfusedCollisionPlayerChaserWandererAI<>(null, 512, damage, 70, 40000));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public int getFlyingHeight() {
        return 20;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 12; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(this.getRandomDeathSpeed(knockbackX), this.getRandomDeathSpeed(knockbackY))
                    .color(new Color(140, 220, 226));
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
        float bobbing = GameUtils.getBobbing(this.getWorldEntity().getTime(), 800) * 4.0F;
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
