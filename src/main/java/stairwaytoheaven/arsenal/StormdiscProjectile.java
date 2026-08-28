package stairwaytoheaven.arsenal;

import java.awt.Color;

import necesse.entity.projectile.boomerangProjectile.TungstenBoomerangProjectile;

/**
 * The Stormdisc in flight.
 *
 * <p>Built on vanilla's {@link TungstenBoomerangProjectile}: {@code init()}
 * sets width 18, height 18 and {@code bouncing = 100} (the wall-bounce that
 * makes a boomerang feel like one), {@code getAngle()} doubles the spin rate,
 * and {@code addDrawables} rotates {@code this.texture} about its own centre.
 * {@code Projectile.init} resolves {@code texture}/{@code shadowTexture} from
 * the projectile's own registered ID, so this subclass draws
 * {@code projectiles/stormdisc.png} and {@code projectiles/stormdisc_shadow.png}
 * without any texture code.
 *
 * <p>Only the particle colour changes: vanilla's tungsten disc returns
 * {@code null} (no particles), ours trails cinderpearl green.
 */
public class StormdiscProjectile extends TungstenBoomerangProjectile {

    /** palette.GHOSTFLAME "glow" — the green a cinderpearl burns with. */
    private static final Color CINDER = new Color(122, 214, 164);

    /** Required: ProjectileRegistry instantiates reflectively with no args. */
    public StormdiscProjectile() {
    }

    @Override
    public Color getParticleColor() {
        return CINDER;
    }
}
