package stairwaytoheaven.objects;

import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.CannonBallProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * Purely cosmetic lobbed stone fired by {@link VeteranCatapultObjectEntity}.
 * Built on vanilla's {@link CannonBallProjectile}, which already gives the
 * exact shape a lobbed shot needs: {@code heightBasedOnDistance} arcs it up
 * and back down over its flight, and {@code addDrawables} spins
 * {@code this.texture} (filled from {@code ProjectileRegistry.Textures} by
 * this class's own registered ID — see {@code WarVeteran.register}) around
 * its centre and draws a shadow under it.
 *
 * <p>{@code canHitMobs = false} makes {@link necesse.entity.projectile.Projectile#canHit}
 * always false, so this can never land its own hit on a player, settler or
 * anything else — the real splash damage is applied directly in
 * {@code VeteranCatapultObjectEntity.serverTick} at fire time, exactly like
 * the turret's cosmetic arrow. {@code doesImpactDamage = false} additionally
 * stops the inherited explosion event from ever firing.
 */
public class CatapultStoneProjectile extends CannonBallProjectile {

    /** Required: ProjectileRegistry instantiates reflectively with no args. */
    public CatapultStoneProjectile() {
    }

    /**
     * Deliberately NOT the {@code CannonBallProjectile(x,y,...,owner)}
     * constructor: that one dereferences {@code owner.getLevel()}
     * immediately, and this projectile — like the turret/catapult's own
     * cosmetic {@code TrapArrowProjectile} — has no owning mob, only a
     * level supplied later by {@code level.entityManager.projectiles.add(...)}.
     * {@code applyData} (used here with a null owner, same as
     * {@code TrapArrowProjectile.setOwner(null)}) never touches the level.
     */
    public CatapultStoneProjectile(float x, float y, float targetX, float targetY, float speed, int distance) {
        this.applyData(x, y, targetX, targetY, speed, distance, new GameDamage(0.0F), 0, (Mob) null);
    }

    @Override
    public void init() {
        super.init();
        this.canHitMobs = false;
        this.doesImpactDamage = false;
    }

    /**
     * Reproduces {@code CannonBallProjectile.addDrawables} minus its final
     * {@code addShadowDrawables} call: this projectile is registered with a
     * null shadow texture path (no {@code catapultstone_shadow.png} was
     * part of this art drop), and the inherited shadow code dereferences
     * {@code shadowTexture} unconditionally rather than null-checking first
     * — calling it here would NPE every frame this stone is in flight.
     */
    @Override
    public void addDrawables(List<LevelSortedDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            OrderableDrawables overlayList, Level level, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        if (this.removed() || this.texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(this);
        int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
        int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
        final TextureDrawOptionsEnd options = this.texture.initDraw().light(light)
                .rotate(this.getAngle(), this.texture.getWidth() / 2, this.texture.getHeight() / 2)
                .pos(drawX, drawY - (int) this.getHeight());
        list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
                options.draw();
            }
        });
    }
}
