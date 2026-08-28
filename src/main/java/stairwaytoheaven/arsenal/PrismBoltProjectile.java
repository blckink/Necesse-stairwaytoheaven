package stairwaytoheaven.arsenal;

import java.awt.Color;

import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.projectile.QuartzBoltProjectile;
import necesse.entity.trails.Trail;
import necesse.level.maps.Level;

/**
 * The Prismcaller's bolt.
 *
 * <p>Built on vanilla's {@link QuartzBoltProjectile}, which is the exact shape
 * a staff bolt needs: {@code init()} sets height 18, piercing 1 and
 * {@code givesLight}, and {@code addDrawables} spins {@code this.texture}
 * around its own centre. {@code Projectile.init} fills {@code texture} and
 * {@code shadowTexture} from {@code ProjectileRegistry.Textures} by the
 * projectile's own registered ID, so subclassing gives us vanilla's behaviour
 * with OUR sprite — see {@code SkyArsenal.register}.
 *
 * <p>Only the particle/trail colour is overridden, which is exactly what
 * separates vanilla's own bolt variants from each other.
 */
public class PrismBoltProjectile extends QuartzBoltProjectile {

    /** palette.PRISMSHARD "base" — the same violet the icon and sprite use. */
    private static final Color PRISM = new Color(186, 156, 214);

    /** Required: ProjectileRegistry instantiates reflectively with no args. */
    public PrismBoltProjectile() {
    }

    public PrismBoltProjectile(Level level, Mob owner, float x, float y, float targetX, float targetY,
                               float speed, int distance, GameDamage damage, int knockback) {
        super(level, owner, x, y, targetX, targetY, speed, distance, damage, knockback);
    }

    @Override
    public Color getParticleColor() {
        return PRISM;
    }

    @Override
    public Trail getTrail() {
        return new Trail(this, this.getLevel(), PRISM, 12.0F, 500, 18.0F);
    }
}
