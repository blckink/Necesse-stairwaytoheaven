package stairwaytoheaven.mobs;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.HostileWormMobBody;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * One coil of the Mistserpent. Built on vanilla's non-boss worm chain
 * (SandwormBody is the same shape) so the segments follow the head through the
 * cloud sea on the engine's own worm mathematics.
 *
 * <p>The sheet is drawn top-down and rotated to the segment's heading, so it
 * holds one orientation only: row 0 is the head, rows 1-4 are body coils and
 * row 5 is the tail. {@code sprite} selects which coil this segment shows.
 *
 * <p><b>This class owns no balance.</b> A worm's health and armour live on the
 * head — VERIFIED [jar], see the note on {@link MistserpentHead}. The values
 * passed to {@code super} below are overwritten from the master before the coil
 * has taken a single tick, so tuning them would change nothing; the real
 * numbers are {@link MistserpentHead#MAX_HEALTH} and
 * {@link MistserpentHead#ARMOR}. What this class does own is
 * {@link #getCollisionDamage}, exactly as {@code SandwormBody} does
 * (SandwormBody.java:71) — a coil that does not override it deals nothing.
 */
public class MistserpentBody extends HostileWormMobBody<MistserpentHead, MistserpentBody> {

    public Point sprite = new Point(0, 1);

    public MistserpentBody() {
        this(MistserpentHead.MAX_HEALTH);
    }

    /**
     * @param health mirrors {@link MistserpentHead#MAX_HEALTH} so a coil is
     *     never briefly wrong on a client that draws it before the first tick.
     *     It is not a tuning knob: {@code WormMobHead.init()} assigns the head's
     *     pool over it as the chain is built (WormMobHead.java:219-220) and
     *     {@code WormMobBody.tickMaster()} re-mirrors health and armour off the
     *     master every tick thereafter (WormMobBody.java:150-163).
     */
    protected MistserpentBody(int health) {
        super(health);
        // Same story as the health above — overwritten from the head's
        // getArmorFlat() on the first tick (WormMobBody.java:155). Kept equal
        // to MistserpentHead.ARMOR so the two never read as different mobs.
        this.setArmor(MistserpentHead.ARMOR);
        this.collision = new Rectangle(-18, -14, 36, 28);
        this.hitBox = new Rectangle(-22, -18, 44, 36);
        this.selectBox = new Rectangle(-28, -52, 56, 58);
    }

    /**
     * 130 per coil. Vanilla analogue: {@code CrystalGolemMob.damage =
     * GameDamage(130.0F)} (CrystalGolemMob.java:57), an incursion tier 1
     * inhabitant and therefore the mod's damage floor. VERIFIED [jar].
     * Why a coil is not scaled down from the head the way
     * {@code SandwormBody} is: see {@link MistserpentHead#HEAD_COLLISION}.
     */
    @Override
    public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
        return MistserpentHead.BODY_COLLISION;
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        if (MistserpentHead.texture == null || !this.isVisible()) {
            return;
        }
        GameLight light = level.getLightLevel(this);
        // Body coils use the unangled draw: the worm chain already orients each
        // segment through its own position, so only the HEAD is rotated to its
        // heading. Vanilla's SandwormBody does exactly this.
        necesse.entity.mobs.WormMobHead.addDrawable(
                list, this,
                new GameSprite(MistserpentHead.texture, this.sprite.x, this.sprite.y, 64),
                MistserpentHead.maskTexture, light, (int) this.height,
                camera.getDrawX(x) - 32, camera.getDrawY(y), 64, perspective);
        this.addShadowDrawables(tileList, level, x, y, light, camera);
    }

    @Override
    protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
        GameTexture shadow = MistserpentHead.shadowTexture;
        if (shadow == null) {
            return null;
        }
        return shadow.initDraw().light(light)
                .pos(camera.getDrawX(x) - shadow.getWidth() / 2,
                     camera.getDrawY(y) - shadow.getHeight() / 2 + this.getBobbing(x, y));
    }

    /** The last coil: the sheet's tail cell, and no segment follows it. */
    public static class Tail extends MistserpentBody {
        public Tail() {
            super(MistserpentHead.MAX_HEALTH);
            this.sprite = new Point(0, 5);
        }
    }
}
