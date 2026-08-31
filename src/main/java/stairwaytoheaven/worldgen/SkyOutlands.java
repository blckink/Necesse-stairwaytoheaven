package stairwaytoheaven.worldgen;

import stairwaytoheaven.SkyRegistry;

/**
 * The Outlands: the sky's wrong places, and the rule that keeps them away from
 * the spire.
 *
 * <h2>What this is for</h2>
 * The Skyreach reads as one bright, pale, friendly world from horizon to
 * horizon. The player's own words: <i>"das gebiet ist einfach zu weiß und hell
 * und wir brauchen kontrast"</i>. The contrast used to live in a whole second
 * dimension (the Veil, behind a Seance Circle), which meant almost no player
 * ever saw it — a dimension is a door, and a door is a thing you have to be
 * told about.
 *
 * So the contrast moves into the sky itself, and is gated by DISTANCE instead
 * of by a door. You arrive at the spire, everything is bright and safe, and the
 * further out you walk the more often the world goes wrong. Nothing is thrown
 * away: the ground, the props and the building these regions are made of are
 * the Veil's own, already drawn, already registered.
 *
 * <h2>The distance ramp</h2>
 * Nothing wrong exists inside {@link #WRONG_START} tiles of the origin — that
 * is a hard floor, not a low probability, so the home region can never roll one
 * and the promise "the spire is safe" is a fact about the world rather than a
 * likelihood. Beyond it the patch threshold falls linearly to
 * {@link #WRONG_FULL}, so wrong regions start as an occasional shock and end as
 * the normal state of the far sky.
 *
 * The threshold values are taken from the Veil's own measured sweep
 * ({@link VeilTerrainPainter#HOLLOW_THRESHOLD}, whose comment records the whole
 * curve: 0.660 -> 18.0% of land, 0.700 -> 11.8%, 0.740 -> 6.9%, 0.780 -> 3.6%,
 * 0.820 -> 1.6%). {@link #THRESHOLD_NEAR} 0.82 therefore starts them at about
 * 1.6% of land just past the floor, and {@link #THRESHOLD_FAR} 0.62 ends near
 * 25% in the outer reaches — common enough to define the far sky, never so
 * common that the bright world stops existing.
 *
 * <h2>Everything here is pure noise</h2>
 * Same contract as the rest of worldgen: a function of the seed and the tile
 * position only, so region borders are seamless, the offline map renderer and
 * the live world agree, and the structure placer can ask the same question the
 * painter will answer later.
 */
public final class SkyOutlands {

    /**
     * No wrong ground within this many tiles of the spire, ever.
     *
     * The player asked for the change to start "ca 1000m" out from the tower.
     * A Necesse tile is the world's metre, and the spire's own guaranteed hub
     * island is {@link SkyOrigin#HUB_RADIUS} = 56, so 900 puts the first
     * possible wrong ground a real walk away from home while still landing
     * inside the first progression band's outer half
     * ({@link SkyOrigin#CORE_RADIUS} = 700).
     */
    public static final float WRONG_START = 900.0F;

    /** Distance at which wrong regions reach their full share of the land. */
    public static final float WRONG_FULL = 3000.0F;

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
