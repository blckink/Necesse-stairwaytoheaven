package stairwaytoheaven.objects;

import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.networkField.IntNetworkField;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.projectile.TrapArrowProjectile;
import necesse.level.maps.Level;

/**
 * The War Veteran's Auto Turret ({@code veteranturret}): a stationary
 * 1x1 object that scans for hostile mobs and shoots them on its own.
 *
 * <h2>Why targets are picked by hand rather than through
 * {@code GameUtils.streamTargets}</h2>
 *
 * That helper (used by e.g. {@code MakeshiftTurretMob}) is built around an
 * attacking MOB's own team, and this turret has no owning mob to hand it —
 * it is a placed object. Rather than fabricate one, target selection here is a direct scan of
 * {@code level.entityManager.mobs} filtered on the same {@code isHostile}
 * flag {@code MakeshiftTurretMob}'s own filter checks, and damage is applied
 * directly through {@code Mob.isServerHit(...)} on the one mob this code
 * chose — never through a hit-scanning projectile that could catch a
 * player or settler standing nearby. The flying arrow this fires has
 * {@code canHitMobs = false}: it is the muzzle's visual effect only, not
 * the thing that deals damage.
 */
public class VeteranTurretObjectEntity extends ObjectEntity {

    public static final int RANGE_PX = 14 * 32;
    private static final long FIRE_INTERVAL_MS = 200L;
    /** Damage per shot at defense level 0; VeteranDefense scales this up. */
    private static final float BASE_DAMAGE = 6.0F;

    private long nextFireTime;
    /**
     * Local wall-clock time of the last shot. On the client it is set when the
     * server's shot counter below arrives, so the firing frames follow the
     * real shots instead of "a hostile is in range".
     */
    public long lastShotTime;
    /** Server-side shot counter, synced to clients; every change is one shot. */
    public final IntNetworkField shotCount = this.registerNetworkField(new IntNetworkField(0) {
        @Override
        public void onChanged(Integer value) {
            super.onChanged(value);
            VeteranTurretObjectEntity.this.lastShotTime = System.currentTimeMillis();
        }
    });

    /**
     * Sheet row the emplacement is currently facing, or -1 while it has never
     * had a target. Written by the server when a shot is taken, synced like
     * {@link #shotCount}; the object's draw code prefers it over the
     * placement rotation. Not the placement rotation itself: that is build
     * data, it is what the multi-tile footprint was laid out with, and
     * rewriting it every time a mob walks past would fight the footprint.
     */
    public final IntNetworkField aimRow = this.registerNetworkField(new IntNetworkField(-1));

    public VeteranTurretObjectEntity(Level level, String type, int tileX, int tileY) {
        super(level, type, tileX, tileY);
        this.shouldSave = false;
    }

    /**
     * Turns the emplacement towards {@code target}, snapped to the four
     * directions the sheet holds. {@code (centreX, centreY)} is the pivot in
     * world pixels — the tile centre for the turret, the 3x3 footprint's
     * centre for the catapult.
     *
     * <p>Row numbering is the sheet's, documented on
     * {@code VeteranTurretObject}: 0 north, 1 east, 2 south, 3 west. World y
     * grows downwards, so a target below the emplacement is south.
     */
    protected void aimAt(Mob target, float centreX, float centreY) {
        float dx = target.x - centreX;
        float dy = target.y - centreY;
        int row;
        if (Math.abs(dx) > Math.abs(dy)) {
            row = dx > 0 ? 1 : 3;
        } else {
            row = dy > 0 ? 2 : 0;
        }
        if (this.aimRow.get() != row) {
            this.aimRow.set(row);
        }
    }

    /**
     * The row the emplacement at this tile is facing, or {@code fallback}
     * (the placement rotation) while it has not aimed at anything yet.
     */
    public static int aimRow(Level level, int tileX, int tileY, int fallback) {
        ObjectEntity entity = level.entityManager.getObjectEntity(tileX, tileY);
        if (!(entity instanceof VeteranTurretObjectEntity)) {
            return fallback;
        }
        int row = ((VeteranTurretObjectEntity) entity).aimRow.get();
        return row < 0 || row > 3 ? fallback : row;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (!this.isServer()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextFireTime) {
            return;
        }
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        Mob target = findNearestHostile(level, this.tileX, this.tileY, RANGE_PX);
        if (target == null) {
            return;
        }
        this.nextFireTime = now + FIRE_INTERVAL_MS;
        this.aimAt(target, this.tileX * 32 + 16, this.tileY * 32 + 16);
        this.markShot(now);
        float damage = BASE_DAMAGE * stairwaytoheaven.settlement.VeteranDefense.turretMultiplier(level);
        target.isServerHit(new GameDamage(damage), target.x, target.y, 0.0F, null);

        float originX = this.tileX * 32 + 16;
        float originY = this.tileY * 32 + 16;
        TrapArrowProjectile visual = new TrapArrowProjectile(originX, originY, target.x, target.y,
                new GameDamage(0.0F), null);
        // Cosmetic only: the hit above already applied the real damage to the
        // one mob this code chose, and this flying arrow must never be able
        // to hit anything on its own — a player standing between the turret
        // and its target must never take this arrow's damage.
        visual.canHitMobs = false;
        level.entityManager.projectiles.add(visual);
    }

    /** Records one shot locally and tells the clients about it. */
    protected void markShot(long now) {
        this.lastShotTime = now;
        this.shotCount.set(this.shotCount.get() + 1);
    }

    /**
     * Firing frame (1-3) for {@code frameMs} per frame after the last shot,
     * idle frame 0 otherwise. Looked up by the object's draw code.
     */
    public static int firingFrame(Level level, int tileX, int tileY, long frameMs) {
        ObjectEntity entity = level.entityManager.getObjectEntity(tileX, tileY);
        if (!(entity instanceof VeteranTurretObjectEntity)) {
            return 0;
        }
        long since = System.currentTimeMillis() - ((VeteranTurretObjectEntity) entity).lastShotTime;
        if (since < 0 || since >= 3 * frameMs) {
            return 0;
        }
        return 1 + (int) (since / frameMs);
    }

    /**
     * Nearest hostile mob in range — never a player, settler or passive
     * animal, all of which have {@code isHostile == false}.
     */
    public static Mob findNearestHostile(Level level, int tileX, int tileY, int rangePx) {
        float centerX = tileX * 32 + 16;
        float centerY = tileY * 32 + 16;
        Mob nearest = null;
        float nearestDist = Float.MAX_VALUE;
        for (Mob mob : level.entityManager.mobs) {
            if (mob == null || mob.removed() || !mob.isHostile || mob.getHealth() <= 0) {
                continue;
            }
            float dx = mob.x - centerX;
            float dy = mob.y - centerY;
            float dist = dx * dx + dy * dy;
            if (dist <= (float) rangePx * rangePx && dist < nearestDist) {
                nearest = mob;
                nearestDist = dist;
            }
        }
        return nearest;
    }
}
