package stairwaytoheaven.mobs;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
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
 * Skystone Golem — an animated hulk of island rock guarding the Aurora Shoals.
 * Slow, heavily armored melee bruiser; the tank of the Skyreach roster.
 */
public class SkystoneGolemMob extends HostileMob {

    public static GameTexture texture;

    public static LootTable lootTable = new LootTable(
            LootItem.between("skystone", 2, 4),
            new ChanceLootItemList(0.4F, LootItem.between("aetheriumore", 1, 2)));
    public static GameDamage damage = new GameDamage(70.0F);

    public SkystoneGolemMob() {
        super(520);
        this.setSpeed(18.0F);
        this.setFriction(3.0F);
        this.setArmor(30);
        this.setKnockbackModifier(0.25F);
        // vanilla spawn-light rules: lit areas are safe (see ZephyrRayMob note)
        this.collision = new Rectangle(-12, -8, 24, 16);
        this.hitBox = new Rectangle(-16, -14, 32, 28);
        this.selectBox = new Rectangle(-16, -44, 32, 52);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this, new ConfusedCollisionPlayerChaserWandererAI<>(null, 384, damage, 150, 40000));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public int getRockSpeed() {
        return 24;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 16; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(
                            (float) (necesse.engine.util.GameRandom.globalRandom.getIntBetween(3, 12)
                                    * (necesse.engine.util.GameRandom.globalRandom.nextBoolean() ? -1 : 1)),
                            (float) (necesse.engine.util.GameRandom.globalRandom.getIntBetween(3, 12)
                                    * (necesse.engine.util.GameRandom.globalRandom.nextBoolean() ? -1 : 1)))
                    .color(new Color(122, 132, 148));
        }
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
        drawY += this.getBobbing(x, y);
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
