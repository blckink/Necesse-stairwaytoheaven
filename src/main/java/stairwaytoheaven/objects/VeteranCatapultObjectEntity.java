package stairwaytoheaven.objects;

import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;
import stairwaytoheaven.settlement.VeteranDefense;

/**
 * The War Veteran's Catapult ({@code veterancatapult}): long range, slow
 * rate, splash. Same direct-damage-on-the-chosen-mob-only safety approach as
 * {@link VeteranTurretObjectEntity} — see its class note for why targets are
 * hand-picked instead of routed through a hit-scanning projectile.
 *
 * <p>Lives on the master (top-left) tile of the 3x3 catapult, so range is
 * measured from the footprint's centre tile.
 */
public class VeteranCatapultObjectEntity extends stairwaytoheaven.objects.VeteranTurretObjectEntity {

    public static final int CATAPULT_RANGE_PX = 25 * 32;
    private static final long CATAPULT_INTERVAL_MS = 3000L;
    private static final float CATAPULT_BASE_DAMAGE = 35.0F;
    private static final float SPLASH_RADIUS_PX = 64.0F;

    private long nextCatapultFire;

    public VeteranCatapultObjectEntity(Level level, String type, int tileX, int tileY) {
        super(level, type, tileX, tileY);
    }

    @Override
    public void serverTick() {
        // Deliberately NOT calling super.serverTick()'s turret firing logic:
        // the catapult has its own rate, range and splash. TileEntity's own
        // per-tick bookkeeping still runs via ObjectEntity's serverTick.
        if (!this.isServer()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextCatapultFire) {
            return;
        }
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        Mob target = findNearestHostile(level, this.tileX + 1, this.tileY + 1, CATAPULT_RANGE_PX);
        if (target == null) {
            return;
        }
        this.nextCatapultFire = now + CATAPULT_INTERVAL_MS;
        // Pivot is the footprint's centre tile, the same point the target
        // scan above uses, so arm and scan agree on where "the catapult" is.
        this.aimAt(target, this.tileX * 32 + 48, this.tileY * 32 + 48);
        this.markShot(now);
        float damage = CATAPULT_BASE_DAMAGE * VeteranDefense.catapultMultiplier(level);
        float tx = target.x;
        float ty = target.y;
        // Splash: every hostile within SPLASH_RADIUS_PX of the impact point,
        // not only the one that was targeted — never a player or settler,
        // since the scan below (inherited from VeteranTurretObjectEntity's
        // own filter shape) only ever considers isHostile mobs to begin with.
        for (Mob mob : level.entityManager.mobs) {
            if (mob == null || mob.removed() || !mob.isHostile || mob.getHealth() <= 0) {
                continue;
            }
            float dx = mob.x - tx;
            float dy = mob.y - ty;
            if (dx * dx + dy * dy <= SPLASH_RADIUS_PX * SPLASH_RADIUS_PX) {
                mob.isServerHit(new GameDamage(damage), mob.x, mob.y, 0.0F, null);
            }
        }

        // Visible cosmetic stone: splash damage above is already applied at
        // fire time (not on this projectile's arrival), so it does no hit
        // detection of its own (canHitMobs=false, see CatapultStoneProjectile).
        float originX = this.tileX * 32 + 48;
        float originY = this.tileY * 32 + 16;
        CatapultStoneProjectile stone = new CatapultStoneProjectile(originX, originY, tx, ty, 220.0F, 900);
        level.entityManager.projectiles.add(stone);
    }
}
