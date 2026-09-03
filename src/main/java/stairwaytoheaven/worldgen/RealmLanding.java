package stairwaytoheaven.worldgen;

import java.awt.Point;

/**
 * Where a door onto a realm puts you down.
 *
 * <h2>Why this exists</h2>
 * {@code docs/PLAN_ONE_PLANE.md} item 6: the realm gates
 * ({@code EdenGateObject}, {@code GhostGateObject}, {@code CrookedDoorObject})
 * <i>"become either doors between bands on the plane or house anchors per
 * §A2.3 — not level teleports."</i> They were built as
 * {@code PortalObjectEntity}s onto {@code eden2} / {@code ghost2} /
 * {@code crooked2}, at the SAME tile coordinates on the far side, because that
 * is how a ladder pair works between two levels.
 *
 * <p>There is no far side any more. A door still has to answer "where does the
 * player come out", and on one plane the honest answer is <b>in that realm's
 * band</b> — which is a position this class computes.
 *
 * <h2>What it picks</h2>
 * The middle of the realm's own band ({@link RealmDepth}'s peak, where the
 * realm's weight is 1 and it is unambiguously itself), in the direction the
 * door was opened from, on the first tile out there that is open ground: dry
 * land, nothing standing on it, and the realm pick agreeing it belongs to the
 * realm asked for. Sweeping the angle before the radius is what keeps the
 * landing at the depth it was meant to be rather than drifting outward into
 * the next realm.
 *
 * <h2>Pure, like the rest of worldgen</h2>
 * A function of the world-generation seed and the door's position only, so a
 * door lands in the same place for every player in a world and across every
 * save and load, and it can be asked before the destination region has ever
 * been generated.
 */
public final class RealmLanding {

    /** Angular steps of the sweep. 96 puts a candidate every ~3.75 degrees. */
    private static final int ANGLE_STEPS = 96;
    /** Radial steps out from the band's inner peak to its outer one. */
    private static final int RADIUS_STEPS = 24;

    private RealmLanding() {
    }

    /**
     * A landing tile inside {@code realm}, reached from {@code (fromX, fromY)}.
     *
     * <p>Never returns null: if no open ground is found anywhere in the band —
     * which would need a seed whose whole band at that depth is Mistsea — the
     * point on the band's midline in the door's own direction is returned, and
     * the gate entity's own landing code reclaims the tile the way it always
     * has.
     */
    public static Point find(int seed, int realm, int fromX, int fromY) {
        int originX = SkyOrigin.originX(seed);
        int originY = SkyOrigin.originY(seed);

        // The direction the door was opened in, so a player who walked east to
        // find the Soul Basin arrives in the eastern Ghost Realm rather than
        // being flung to the far side of the world.
        double dx = fromX - originX;
        double dy = fromY - originY;
        double baseAngle = (dx == 0.0 && dy == 0.0) ? 0.0 : Math.atan2(dy, dx);

        float inner = RealmDepth.bandPeakStart(realm) * RealmDepth.DEPTH_SCALE;
        float outer = RealmDepth.bandPeakEnd(realm) * RealmDepth.DEPTH_SCALE;
        float mid = (inner + outer) * 0.5F;

        for (int r = 0; r <= RADIUS_STEPS; r++) {
            // Walk outward and inward from the band's middle in step, so the
            // landing stays as close to the realm's heart as the terrain allows.
            float radius = mid + (r % 2 == 0 ? 1 : -1) * (r / 2) * ((outer - inner) / RADIUS_STEPS);
            if (radius < inner) {
                radius = inner;
            }
            for (int a = 0; a < ANGLE_STEPS; a++) {
                // Alternate left and right of the door's own bearing.
                double offset = ((a + 1) / 2) * (2.0 * Math.PI / ANGLE_STEPS) * (a % 2 == 0 ? 1 : -1);
                double angle = baseAngle + offset;
                int tileX = originX + (int) Math.round(Math.cos(angle) * radius);
                int tileY = originY + (int) Math.round(Math.sin(angle) * radius);
                if (RealmDepth.realmAt(seed, tileX, tileY, originX, originY) != realm) {
                    continue;
                }
                long desc = SkyTerrainPainter.describeTile(seed, tileX, tileY, originX, originY);
                if (SkyTerrainPainter.descObject(desc) != 0
                        || SkyTerrainPainter.descBuilt(desc)
                        || !SkyTerrainPainter.isOpenGround(seed, tileX, tileY, originX, originY)) {
                    continue;
                }
                return new Point(tileX, tileY);
            }
        }
        return new Point(originX + (int) Math.round(Math.cos(baseAngle) * mid),
                originY + (int) Math.round(Math.sin(baseAngle) * mid));
    }
}
