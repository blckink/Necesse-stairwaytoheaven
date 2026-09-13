package stairwaytoheaven.mobs;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionChaserAI;
import necesse.entity.mobs.ai.behaviourTree.trees.SharkAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.hostile.SharkMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * Striped Megashark / Gestreifter Riesenhai — a black-and-white striped
 * shark three times the size of vanilla's, swimming every deep surface water
 * on the overworld. Crooked Beyond's funhouse stripes on a sea monster.
 *
 * <p><b>Vanilla parent: {@link SharkMob}.</b> Its AI, swim mask, deep-water
 * spawn check (liquid height &lt;= -10, no settlement) and sounds are
 * inherited. Changed on purpose:
 * <ul>
 * <li>Boss-class numbers, roughly the Pirate Captain's (7,750 HP at normal
 *     difficulty, VERIFIED [jar] {@code PirateCaptainMob.MAX_HEALTH}):
 *     {@link #HEALTH} health, armour 40, a {@link #BITE_DAMAGE} bite against
 *     the shark's 24 (SharkAI hard-codes {@code new GameDamage(24)} into its
 *     CollisionChaserAI, so {@link #init} rewrites that node's public
 *     {@code damage} field instead of copying the tree).</li>
 * <li>Slower than a shark: 20 cruising / 45 chasing against 30 / 65.</li>
 * <li>3x the body: collision, hit and select boxes, and the 96 px sheet drawn
 *     at 288 px.</li>
 * <li>Loot is tungsten bars, not shark parts.</li>
 * <li>Rarity: weight 1 on {@code Biome.defaultSurfaceMobs} against the shark's
 *     10 (see {@code SkyMobs.registerSurfaceSpawns}), and at most one within
 *     six spawn radii of the player.</li>
 * </ul>
 */
public class StripedMegasharkMob extends SharkMob {

    public static final int HEALTH = 8000;
    public static final float BITE_DAMAGE = 140.0F;

    public static final LootTable megaLoot = new LootTable(
            new LootItem("tungstenbar", 6),
            new ChanceLootItem(0.5F, "tungstenbar", 4),
            new LootItem("sharkscales", 3));

    /** Filled by {@code SkyMobs.loadTextures}; null on a dedicated server. */
    public static GameTexture texture;

    public StripedMegasharkMob() {
        super();
        this.setMaxHealth(HEALTH);
        this.setHealthHidden(HEALTH);
        this.setArmor(40);
        this.collision = new Rectangle(-48, -42, 96, 84);
        this.hitBox = new Rectangle(-60, -48, 120, 96);
        this.selectBox = new Rectangle(-60, -60, 120, 120);
        this.swimParticleOffset = 39;
        this.setKnockbackModifier(0.05F);
    }

    /** No icon of its own yet: the journal shows the vanilla shark's face. */
    @Override
    public GameTexture getMobIcon() {
        return BorrowedMobIcon.from("shark", super.getMobIcon());
    }

    @Override
    public LootTable getLootTable() {
        return megaLoot;
    }

    @Override
    public void init() {
        super.init();
        SharkAI<StripedMegasharkMob> tree = new SharkAI<>(800);
        setBiteDamage(tree);
        this.ai = new BehaviourTreeAI<>(this, tree, new AIMover());
        this.updateSpeed();
    }

    private static void setBiteDamage(AINode<?> node) {
        if (node instanceof CollisionChaserAI) {
            ((CollisionChaserAI<?>) node).damage = new GameDamage(BITE_DAMAGE);
        }
        for (AINode<?> child : node.debugChildren()) {
            setBiteDamage(child);
        }
    }

    @Override
    public void updateSpeed() {
        if (this.isChasing != null && this.isChasing.get()) {
            this.setSpeed(45.0F);
        } else {
            this.setSpeed(20.0F);
        }
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return new MobSpawnLocation(this, targetX, targetY)
                .checkMobSpawnLocation()
                .checkInLiquid()
                .checkMaxMobsAround(1, Mob.MOB_SPAWN_AREA.maxSpawnDistance * 6,
                        m -> m instanceof StripedMegasharkMob, client)
                .validAndApply();
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        if (texture == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            this.getLevel().entityManager.addParticle(
                    new FleshParticle(this.getLevel(), texture, i, 12, 32, this.x, this.y, 20.0F,
                            knockbackX, knockbackY),
                    Particle.GType.IMPORTANT_COSMETIC);
        }
    }

    /** SharkMob.addDrawables with our sheet, drawn at three times the cell size. */
    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 144;
        int drawY = camera.getDrawY(y) - 144;
        Point sprite = this.getAnimSprite(x, y, this.getDir());
        final DrawOptions options = texture.initDraw()
                .sprite(sprite.x, sprite.y, 96)
                .size(288, 288)
                .startGlowOptions(level, (long) this.getID())
                .light(light)
                .applyEnemyTracker(this, perspective)
                .pos(drawX, drawY);
        list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
                options.draw();
            }
        });
    }
}
