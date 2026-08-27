package stairwaytoheaven.mobs;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerChargingCirclingChaserAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.hostile.HostileWormMobHead;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * The Mistserpent: a serpent that swims through the Mistsea, surfacing and
 * diving the way a sea serpent breaches water.
 *
 * Built on vanilla's NON-boss worm chain (HostileWormMobHead, the same base
 * SandwormHead uses) rather than the crystal dragon's boss variant — the
 * Skyreach wants a dangerous inhabitant of the cloud sea, not an arena fight.
 * The head leads and the segments follow on the engine's own worm mathematics;
 * a FlyingAIMover is what lets it pass through the Mistsea rather than pathing
 * around it, exactly as the sandworm passes through dune sand.
 *
 * It is deliberately heavier than the Skyreach's existing roster (the playtest
 * called those too easy): more health than a Galehound pack combined, armor
 * that punishes chip damage, and a collision that hurts on every coil, so the
 * open cloud between islands stops being safe travelling ground.
 */
public class MistserpentHead extends HostileWormMobHead<MistserpentBody, MistserpentHead> {

    public static GameTexture texture;
    public static GameTexture maskTexture;
    public static GameTexture shadowTexture;

    public static LootTable lootTable = new LootTable(
            LootItem.between("stormshard", 2, 5),
            LootItem.between("aetheriumore", 1, 3));

    /** Segment spacing in pixels — vanilla's sandworm uses 20 for 20 coils. */
    public static final float LENGTH_PER_BODY_PART = 22.0F;
    /** How far the body wave travels; larger reads as a longer, lazier coil. */
    public static final float WAVE_LENGTH = 380.0F;
    public static final int TOTAL_BODY_PARTS = 14;

    public static final GameDamage HEAD_COLLISION = new GameDamage(90.0F);
    public static final GameDamage BODY_COLLISION = new GameDamage(60.0F);

    /**
     * Only in open Mistsea. The serpent swims the cloud sea; spawning it on an
     * island would put a fourteen-coil worm on ground the player walks, which
     * is neither the fantasy nor survivable. Islands stay the safe part.
     */
    public static final necesse.level.maps.biomes.MobSpawnTable.CanSpawnPredicate IN_MISTSEA =
            (level, client, tile, mobStringID) -> {
                level.regionManager.ensureTileIsLoaded(tile.x, tile.y);
                return level.getTileID(tile.x, tile.y) == stairwaytoheaven.SkyRegistry.mistseaID;
            };

    public MistserpentHead() {
        super(1500, WAVE_LENGTH, 70.0F, TOTAL_BODY_PARTS, 20.0F, -24.0F);
        this.moveAccuracy = 120;
        this.setSpeed(94.0F);
        this.setArmor(18);
        this.accelerationMod = 1.0F;
        this.decelerationMod = 1.0F;
        this.collision = new Rectangle(-16, -14, 32, 28);
        this.hitBox = new Rectangle(-20, -16, 40, 32);
        this.selectBox = new Rectangle(-20, -35, 40, 40);
    }

    @Override
    protected float getDistToBodyPart(MistserpentBody bodyPart, int index, float lastDistance) {
        return LENGTH_PER_BODY_PART;
    }

    @Override
    protected MistserpentBody createNewBodyPart(int index) {
        MistserpentBody part = index == TOTAL_BODY_PARTS - 1
                ? new MistserpentBody.Tail()
                : new MistserpentBody();
        // Vanilla shares a hit cooldown across runs of three coils, so a single
        // swing cannot rake the whole length for full damage on every segment.
        part.sharesHitCooldownWithNext = index % 3 < 2;
        part.relaysBuffsToNext = index % 3 < 2;
        if (!(part instanceof MistserpentBody.Tail)) {
            part.sprite = new Point(0, 1 + index % 4);
        }
        return part;
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new PlayerChargingCirclingChaserAI<>(null, 2560, 500, 20),
                new FlyingAIMover());
    }

    /**
     * WormMobHead declares this abstract — every worm has to say what it sounds
     * like when it moves. The sandworm shakes the ground; a serpent in cloud
     * uses the softer water-swim cue.
     */
    @Override
    protected void playMoveSound() {
        necesse.engine.sound.SoundManager.playSound(
                necesse.gfx.GameResources.watersplash,
                necesse.engine.sound.SoundEffect.effect(this).falloffDistance(1600).volume(0.5F));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        if (texture == null || !this.isVisible()) {
            return;
        }
        GameLight light = level.getLightLevel(this);
        float headAngle = GameMath.fixAngle(GameMath.getAngle(
                new java.awt.geom.Point2D.Float(this.dx, this.dy)));
        addAngledDrawable(list, this, new GameSprite(texture, 0, 0, 64), maskTexture,
                light, (int) this.height, headAngle,
                camera.getDrawX(x) - 32, camera.getDrawY(y), 64, perspective);
        this.addShadowDrawables(tileList, level, x, y, light, camera);
    }

    @Override
    protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
        if (shadowTexture == null) {
            return null;
        }
        return shadowTexture.initDraw().light(light)
                .pos(camera.getDrawX(x) - shadowTexture.getWidth() / 2,
                     camera.getDrawY(y) - shadowTexture.getHeight() / 2 + this.getBobbing(x, y));
    }
}
