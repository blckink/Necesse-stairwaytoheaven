package stairwaytoheaven.realms.eden;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;

/**
 * Golden Hornet — the fast air enemy (§5: <i>"Golden Hornet — fast air
 * enemy"</i>), and A3.3's <i>"aggressive paradise insects"</i>.
 *
 * <p><b>Borrowed art:</b> vanilla {@code mobs/bee} — a gold-and-black insect,
 * 64x128, i.e. two 32px animation frames over four direction rows. It is the
 * sheet vanilla's own {@code BeeFollowingMob} flies on, and it is drawn here
 * with that mob's offsets ({@code drawX - 16}, {@code drawY - 22}) and its
 * two-frame {@code getAnimSprite} — which is not optional: the default
 * {@code Mob.getAnimSprite} returns columns 0-5, and this sheet has two.
 * NOT subclassed, because {@code BeeFollowingMob} is a summon that belongs to a
 * player.
 *
 * <p><b>Tier: Eden floor, FAST role</b> ({@link EdenTiers}) —
 * {@code 1500 x 0.60} = <b>900 HP</b> and {@code 165 x 0.80} = <b>132
 * damage</b>, at the realm's full <b>45 armour</b> (armour carries no role
 * modifier: vanilla's own fast flier, {@code AscendedBatMob}, wears the same 40
 * as the golem it flies past). The role percentages are
 * {@code SkyMobTiers.ROLE_FAST_HP/ROLE_FAST_DAMAGE}, shared with the Skyreach's
 * Galehound so "fast" means one thing across the mod.
 *
 * <p><b>Capped hard, everywhere.</b> Both biome tables allow at most two within
 * eight tiles. A fast flier is the single mob most able to reproduce the
 * complaint A4.1 exists to answer — <i>"nicht dauernd angeflogen kommen"</i> —
 * and the cap, not the weight, is the lever that stops it (see
 * {@code SkyBiome}: a cap that binds redistributes the spawn rather than
 * wasting it).
 */
public class GoldenHornetMob extends EdenHostileMob {

    /** Loaded in {@link EdenRealm#loadTextures()} from the GAME's own resources. */
    public static GameTexture texture;

    /** Eden floor 1500 x 0.60 (fast role) = 900 on CLASSIC. */
    public static final MaxHealthGetter MAX_HEALTH =
            EdenTiers.health(stairwaytoheaven.mobs.SkyMobTiers.ROLE_FAST_HP);
    /** Eden floor 165 x 0.80 (fast role) = 132. */
    public static final GameDamage DAMAGE =
            EdenTiers.damage(stairwaytoheaven.mobs.SkyMobTiers.ROLE_FAST_DAMAGE);
    /** The realm's armour, unreduced — armour carries no role modifier. */
    public static final int ARMOR = EdenTiers.EDEN_ARMOR;

    /**
     * Drops, at Eden's x1.3 drop value. Golden Pollen is the realm's only
     * flying-mob material and the Eden Press's flux, so the hornet is a
     * resource and not only an interruption.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.70F, LootItem.between("goldenpollen",
                    EdenTiers.drop(1), EdenTiers.drop(2))),
            new ChanceLootItemList(0.20F, LootItem.between("venomfang", 1, 1)));

    private final int animationOffset = GameRandom.globalRandom.nextInt(900);

    public GoldenHornetMob() {
        super(EdenTiers.hp(stairwaytoheaven.mobs.SkyMobTiers.ROLE_FAST_HP));
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        this.setSpeed(72.0F);
        this.setFriction(1.2F);
        this.setKnockbackModifier(0.6F);
        this.collision = new Rectangle(-9, -7, 18, 14);
        this.hitBox = new Rectangle(-13, -16, 26, 30);
        this.selectBox = new Rectangle(-20, -34, 40, 32);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<>(null, 520, DAMAGE, 60, 40000));
    }

    /** It flies. BeeFollowingMob's own height, so it clears the same things. */
    @Override
    public int getFlyingHeight() {
        return 20;
    }

    @Override
    public boolean canPushMob(Mob other) {
        return other instanceof GoldenHornetMob;
    }

    /**
     * Two frames, not six.
     *
     * <p>{@code Mob.getAnimSprite} (Mob.java:3680) returns column 0-5 — idle,
     * four walk frames and a liquid frame — and {@code mobs/bee} is 64 pixels
     * wide, i.e. TWO columns. Using the default would read past the right edge
     * of the sheet on every moving frame. This is
     * {@code BeeFollowingMob.getAnimSprite} verbatim.
     */
    @Override
    public Point getAnimSprite(int x, int y, int dir) {
        long time = this.getTime() + new GameRandom((long) this.getUniqueID()).nextInt(200);
        return new Point(GameUtils.getAnim(time, 2, 200), dir);
    }

    /** Set by {@link #addDrawables} immediately before the base class draws. */
    private int flightBob;

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList,
            OrderableDrawables topList, Level level, int x, int y, TickManager tickManager,
            GameCamera camera, PlayerMob perspective) {
        // The bob is what sells a flier, and the shared base draws flat. Same
        // 1000ms cycle and 5px lift BeeFollowingMob uses, with a per-mob random
        // phase so a pair is never in lockstep.
        this.flightBob = (int) (GameUtils.getBobbing(level.getTime() + this.animationOffset, 1000) * 5.0F) - 6;
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
    }

    @Override
    protected int drawOffsetY() {
        return -22 + this.flightBob;
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    protected GameTexture sheet() {
        return texture;
    }

    @Override
    protected int spriteSize() {
        return 32;
    }

    @Override
    protected int drawOffsetX() {
        return -16;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        for (int i = 0; i < 10; i++) {
            this.getLevel().entityManager
                    .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
                    .movesConstant(EdenSerpentMob.deathSpeed(knockbackX), EdenSerpentMob.deathSpeed(knockbackY))
                    .color(new Color(224, 178, 32));
        }
    }
}
