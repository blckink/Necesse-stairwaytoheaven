package stairwaytoheaven.objects;

import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.level.maps.Level;

/**
 * Barbed wire bites: every half second, each hostile mob whose centre is on
 * the wire's tile or pressed against it takes a small hit. Same hostile-only
 * rule as the turrets ({@code isHostile}), so players and settlers walking
 * along it are never hurt.
 */
public class BarbedWireObjectEntity extends ObjectEntity {

    private static final long INTERVAL_MS = 500L;
    private static final float DAMAGE = 25.0F;
    /** How far outside the tile a mob's centre may be and still touch the wire. */
    private static final int REACH_PX = 14;

    private long nextHit;

    public BarbedWireObjectEntity(Level level, String type, int tileX, int tileY) {
        super(level, type, tileX, tileY);
        this.shouldSave = false;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (!this.isServer()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextHit) {
            return;
        }
        this.nextHit = now + INTERVAL_MS;
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        float minX = this.tileX * 32 - REACH_PX;
        float minY = this.tileY * 32 - REACH_PX;
        float maxX = this.tileX * 32 + 32 + REACH_PX;
        float maxY = this.tileY * 32 + 32 + REACH_PX;
        for (Mob mob : level.entityManager.mobs) {
            if (mob == null || mob.removed() || !mob.isHostile || mob.getHealth() <= 0) {
                continue;
            }
            if (mob.x >= minX && mob.x <= maxX && mob.y >= minY && mob.y <= maxY) {
                mob.isServerHit(new GameDamage(DAMAGE, VeteranTurretObjectEntity.ARMOR_PEN), mob.x, mob.y, 0.0F, null);
            }
        }
    }
}
