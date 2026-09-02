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
 * <p>The sheet is the Crystal Dragon's: 320x1792, read as eight 224px rows
 * drawn top-down and rotated to the segment's heading. Row 0 is the head, row 1
 * the shoulder the head carries, and rows 1-7 the seven coils.
 * {@code sprite} selects which coil this segment shows.
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

    /**
     * The Crystal Dragon's body draw, ported: {@code sprite(0, spriteY, 224)}
     * at 130px from {@code camX - 112}, angled toward the NEXT segment, with
     * the shadow sheet 40px lower and the body lit through
     * {@code minLevelCopy(100)} (VERIFIED [jar], CrystalDragonBody.java:152-180).
     * A coil with nothing in front of it has no heading and is not drawn --
     * vanilla's own guard.
     */
    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        if (MistserpentHead.texture == null || !this.isVisible() || this.next == null) {
            return;
        }
        GameLight light = level.getLightLevel(this);
        int drawX = camera.getDrawX(x) - MistserpentHead.DRAW_OFFSET;
        int drawY = camera.getDrawY(y);
        float angle = necesse.engine.util.GameMath.fixAngle(necesse.engine.util.GameMath.getAngle(
                new java.awt.geom.Point2D.Float(this.next.x - x,
                        this.next.y - this.next.height - (y - this.height))));
        necesse.entity.mobs.WormMobHead.addAngledDrawable(
                list, this,
                new GameSprite(MistserpentHead.texture, this.sprite.x, this.sprite.y,
                        MistserpentHead.SHEET_CELL),
                null, light.minLevelCopy(100.0F), (int) this.height, angle,
                drawX, drawY, MistserpentHead.DRAW_SIZE, perspective);
        if (MistserpentHead.shadowTexture != null) {
            final MobDrawable shadow = necesse.entity.mobs.WormMobHead.getAngledDrawable(
                    this,
                    new GameSprite(MistserpentHead.shadowTexture, this.sprite.x, this.sprite.y,
                            MistserpentHead.SHEET_CELL),
                    null, light, (int) this.height, angle,
                    drawX, drawY + MistserpentHead.SHADOW_DROP, MistserpentHead.DRAW_SIZE, perspective);
            tileList.add(shadow::draw);
        }
    }

    /**
     * The last coil. On this sheet the tail is simply the last body row, which
     * {@code createNewBodyPart} already assigns, so this subclass exists only
     * to give the chain's end its own registered mob ID.
     */
    public static class Tail extends MistserpentBody {
        public Tail() {
            super(MistserpentHead.MAX_HEALTH);
            this.sprite = new Point(0, MistserpentHead.TOTAL_BODY_PARTS);
        }
    }
}
