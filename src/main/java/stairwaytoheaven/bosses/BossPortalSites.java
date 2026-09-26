package stairwaytoheaven.bosses;

import java.awt.Point;

import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.SkyTerrainPainter;

/**
 * Where a realm's summoning stones stand, asked of the world seed alone.
 *
 * <p>{@code SkyLevel.placePortalsOf} scatters the stones on a lattice that is a
 * pure function of the seed and the cell; this walks the same lattice without
 * a level, so a key piece at home can find the stone nearest to it before the
 * stone's region has ever been generated. The two must agree: the lattice,
 * salt and site maths below are {@code placePortalsOf}'s own, line for line.
 *
 * <p>What it cannot know is whether {@code placePortalAt} really found a free
 * tile at the site — that reads the generated level. A site with no open
 * ground within {@link #SEARCH} tiles is skipped here, which is the case in
 * which the level would have found none either (the Mistsea). HYPOTHESIS for
 * the rare site whose open ground is all taken by scenery.
 */
public final class BossPortalSites {

    /** How far around a site a landing is looked for, in tiles. */
    private static final int SEARCH = 6;

    private BossPortalSites() {
    }

    /**
     * A free tile beside the summoning stone of {@code realm} nearest to
     * {@code (fromX, fromY)}; with {@code from == null}, nearest to the spire.
     * Null if the realm has no stone at all in this world.
     */
    public static Point landingNearestTo(int seed, int realm, Point from) {
        int originX = SkyOrigin.originX(seed);
        int originY = SkyOrigin.originY(seed);
        int fromX = from != null ? from.x : originX;
        int fromY = from != null ? from.y : originY;
        int cell = BossPortalObject.PORTAL_CELL;
        float chance = BossPortalObject.PORTAL_CHANCE;
        int salt = BossPortalObject.SALT_PORTAL + realm * BossPortalObject.SALT_STRIDE;
        int reach = (int) (RealmDepth.bandEnd(realm) * RealmDepth.DEPTH_SCALE) + cell;
        int minCX = Math.floorDiv(originX - reach, cell);
        int maxCX = Math.floorDiv(originX + reach, cell);
        int minCY = Math.floorDiv(originY - reach, cell);
        int maxCY = Math.floorDiv(originY + reach, cell);

        Point best = null;
        long bestDist = Long.MAX_VALUE;
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cy = minCY; cy <= maxCY; cy++) {
                if (SkyNoise.hash(seed + salt, cx, cy) >= chance) {
                    continue;
                }
                int siteX = Math.round(cx * cell + SkyNoise.hash(seed + salt + 1, cx, cy) * cell);
                int siteY = Math.round(cy * cell + SkyNoise.hash(seed + salt + 2, cx, cy) * cell);
                if (RealmDepth.realmAt(seed, siteX, siteY, originX, originY) != realm) {
                    continue;
                }
                long dx = siteX - fromX;
                long dy = siteY - fromY;
                long dist = dx * dx + dy * dy;
                if (dist >= bestDist) {
                    continue;
                }
                Point landing = openGroundAround(seed, siteX, siteY, originX, originY);
                if (landing != null) {
                    best = landing;
                    bestDist = dist;
                }
            }
        }
        return best;
    }

    /** The nearest open, empty tile to a site, ring by ring, skipping the site itself. */
    private static Point openGroundAround(int seed, int siteX, int siteY, int originX, int originY) {
        for (int r = 1; r <= SEARCH; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) {
                        continue;
                    }
                    int x = siteX + dx;
                    int y = siteY + dy;
                    long desc = SkyTerrainPainter.describeTile(seed, x, y, originX, originY);
                    if (SkyTerrainPainter.descObject(desc) == 0
                            && !SkyTerrainPainter.descBuilt(desc)
                            && SkyTerrainPainter.isOpenGround(seed, x, y, originX, originY)) {
                        return new Point(x, y);
                    }
                }
            }
        }
        return null;
    }
}
