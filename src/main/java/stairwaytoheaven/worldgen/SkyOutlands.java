package stairwaytoheaven.worldgen;

import stairwaytoheaven.SkyRegistry;

/**
 * The Outlands: Crooked Beyond's wrong ground, and the rule that keeps it out
 * past everything else.
 *
 * <h2>What this is for</h2>
 * {@code docs/WORLD_DESIGN.md} §41.4 is blunt about what these are: <i>"The
 * Beetle Outlands ARE Crooked Beyond ... striped, violet, wrong,
 * Beetlejuice-based — that is §13 exactly."</i> They were the first realm
 * expressed the right way — a realm as a distance-gated biome on the sky plane
 * rather than as a dimension behind a door — which is why
 * {@code docs/PLAN_ONE_PLANE.md} names this file and {@code OutlandsBiome} as
 * the pattern every other realm now follows.
 *
 * <h2>The distance ramp, and the interim that is now over</h2>
 * This class used to start the wrong ground at 900 tiles, and its own comment
 * recorded why: Crooked Beyond's real band is depth 0.70-0.88, i.e. 4200 tiles
 * out ({@link RealmDepth}), but Eden, Steinfeld and the Ghost Realm did not
 * exist, and moving Crooked to its true band would have emptied the world from
 * 900 to 4200 and brought back the complaint that created this region — <i>"das
 * gebiet ist einfach zu weiss und hell und wir brauchen kontrast"</i>. That
 * comment ended with an instruction: <b>"The day Eden and Steinfeld land,
 * delete this constant and let RealmDepth answer."</b> They have landed, and
 * as of the one-plane refactor they are bands of this same level, so this is
 * that deletion. {@link #WRONG_START} and {@link #WRONG_FULL} are now derived
 * from Crooked's own band and nowhere else.
 *
 * <h2>Where it is asked</h2>
 * Only from inside the Crooked band, by
 * {@code realms.crooked.CrookedTerrainPainter}. The Skyreach no longer has
 * wrong ground at all: the contrast it needed is now supplied by the four
 * realms standing between the spire and here.
 *
 * <h2>Everything here is pure noise</h2>
 * Same contract as the rest of worldgen: a function of the seed and the tile
 * position only, so region borders are seamless, the offline map renderer and
 * the live world agree, and the structure placer can ask the same question the
 * painter will answer later.
 */
public final class SkyOutlands {

    /**
     * No wrong ground inside this many tiles of the spire, ever.
     *
     * <p>Crooked Beyond's own band start ({@link RealmDepth#bandStart}, realm
     * {@link RealmDepth#REALM_CROOKED} = depth 0.70) in tiles: 4200 at
     * {@link RealmDepth#DEPTH_SCALE} 6000. Derived, never typed — turning the
     * world-size dial moves this with it, which is the whole reason
     * {@code RealmDepth} holds one constant instead of nine.
     *
     * <p>It is a hard floor, not a low probability: the realms before Crooked
     * can never roll one, so "the spire is safe" stays a fact about the world
     * rather than a likelihood.
     */
    public static final float WRONG_START =
            RealmDepth.bandStart(RealmDepth.REALM_CROOKED) * RealmDepth.DEPTH_SCALE;

    /**
     * Distance at which wrong regions reach their full share of the land:
     * Crooked's peak end (depth 0.88), i.e. 5280 tiles. Past it the realm is
     * as wrong as it gets, which is also where Hell's band takes over.
     */
    public static final float WRONG_FULL =
            RealmDepth.bandPeakEnd(RealmDepth.REALM_CROOKED) * RealmDepth.DEPTH_SCALE;

    /**
     * Where Crooked Beyond's inner edge belongs, kept as a named alias of
     * {@link #WRONG_START} so the status oracle can still print both and show
     * that they now agree.
     */
    public static final float CROOKED_TRUE_START = WRONG_START;

    /** The band start {@link RealmDepth} actually derives, for the record. */
    public static float trueCrookedStart() {
        // Walk out until the realm field first gives Crooked any weight.
        for (int d = 0; d <= (int) RealmDepth.DEPTH_SCALE; d += 10) {
            if (RealmDepth.weightOf(RealmDepth.REALM_CROOKED, RealmDepth.depthFor(d)) > 0.0F) {
                return d;
            }
        }
        return RealmDepth.DEPTH_SCALE;
    }

    /** Patch threshold at {@link #WRONG_START} (rare) and at {@link #WRONG_FULL} (common). */
    public static final float THRESHOLD_NEAR = 0.82F;
    public static final float THRESHOLD_FAR = 0.62F;

    /**
     * Patch size. Larger than the Veil's Hollows (39) because the sky's islands
     * are themselves larger: at 39 a wrong region kept coming out smaller than
     * the island holding it, which reads as a stain on normal ground rather
     * than as somewhere you have arrived.
     */
    public static final float PATCH_SCALE = 52.0F;

    /** Blackpeat interrupts the striped ground, exactly as it does in the Veil. */
    public static final float PEAT_SCALE = 20.0F;
    public static final float PEAT_THRESHOLD = 0.66F;

    public static final long SALT_WRONG = 0xB33715L;
    public static final long SALT_WRONG_PEAT = 0xB33716L;
    public static final int SALT_WRONG_OBJECT = 151;

    /**
     * Boss-portal lattice. One cell holds at most one portal site, so portals
     * are placed AT A PLACE rather than rolled per tile — the player asked for
     * "an bestimmten stellen, nicht random".
     *
     * 260 tiles is a little under the distance the ramp needs to change
     * noticeably, so the far sky carries them steadily without two ever landing
     * within sight of each other.
     */
    public static final int PORTAL_CELL = 260;
    public static final long SALT_PORTAL = 0xB0551EL;

    private SkyOutlands() {
    }

    /**
     * How wrong the world is allowed to get at this distance: 0 at and inside
     * {@link #WRONG_START}, rising to 1 at {@link #WRONG_FULL}.
     */
    public static float wrongness(float hubDist) {
        if (hubDist <= WRONG_START) {
            return 0.0F;
        }
        if (hubDist >= WRONG_FULL) {
            return 1.0F;
        }
        return (hubDist - WRONG_START) / (WRONG_FULL - WRONG_START);
    }

    /**
     * Is this tile inside an Outland region?
     *
     * The distance floor is checked FIRST and returns before the noise is
     * sampled: near the spire the answer must be no for every seed, not merely
     * for most of them.
     */
    public static boolean isWrong(int seed, int tileX, int tileY, float hubDist) {
        float ramp = wrongness(hubDist);
        if (ramp <= 0.0F) {
            return false;
        }
        float threshold = THRESHOLD_NEAR + (THRESHOLD_FAR - THRESHOLD_NEAR) * ramp;
        return SkyNoise.fbm(seed + SALT_WRONG, tileX, tileY, PATCH_SCALE, 2) > threshold;
    }

    /**
     * Same question for callers that only have tile coordinates and the origin
     * (the structure placer, the offline renderer, the status oracle).
     */
    public static boolean isWrong(int seed, int tileX, int tileY, int originX, int originY) {
        float dx = tileX - originX;
        float dy = tileY - originY;
        return isWrong(seed, tileX, tileY, (float) Math.sqrt(dx * dx + dy * dy));
    }

    /** The striped ground, interrupted by blackpeat so the stripes stay loud. */
    public static int groundTile(int seed, int tileX, int tileY) {
        boolean peat = SkyNoise.fbm(seed + SALT_WRONG_PEAT, tileX, tileY, PEAT_SCALE, 2) > PEAT_THRESHOLD;
        return peat ? SkyRegistry.blackpeatID : SkyRegistry.beetlefreakID;
    }

    /**
     * The Outlands' prop mix: the Veil's own, and deliberately sparser than any
     * sky ground. Dead trees and bones instead of grass and flowers; nothing
     * that grows.
     *
     * Rates are the Beetlefreak Hollows' own shipped values
     * ({@link VeilTerrainPainter#rollHollowObject}), so a region here reads as
     * the same place the Veil's Hollows were, rather than as a new density
     * nobody measured.
     */
    public static int rollObject(int seed, int tileX, int tileY) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_WRONG_OBJECT);
        if (roll < 0.055F) return SkyRegistry.deadtreeID;
        if (roll < 0.085F) return SkyRegistry.ashbonesID;
        if (roll < 0.100F) return SkyRegistry.veilrockID;
        if (roll < 0.108F) return SkyRegistry.gloomshroomID;
        return 0;
    }

    /**
     * Is this the exact tile a boss portal stands on?
     *
     * One hashed site per {@link #PORTAL_CELL} lattice cell, and the site only
     * becomes a portal if it landed on wrong ground. So portals exist only in
     * the Outlands, at most one per cell, at a position that is a pure function
     * of the seed — the same portal, in the same place, for every player in a
     * world and across every save/load.
     *
     * The caller still has to confirm the tile is land and unbuilt; this
     * answers the "is this the place" half only.
     */
    public static boolean isPortalSite(int seed, int tileX, int tileY, float hubDist) {
        int cellX = Math.floorDiv(tileX, PORTAL_CELL);
        int cellY = Math.floorDiv(tileY, PORTAL_CELL);
        int siteX = cellX * PORTAL_CELL
                + (int) (SkyNoise.hash(seed + SALT_PORTAL, cellX, cellY) * PORTAL_CELL);
        int siteY = cellY * PORTAL_CELL
                + (int) (SkyNoise.hash(seed + SALT_PORTAL + 1, cellX, cellY) * PORTAL_CELL);
        if (tileX != siteX || tileY != siteY) {
            return false;
        }
        return isWrong(seed, tileX, tileY, hubDist);
    }
}
