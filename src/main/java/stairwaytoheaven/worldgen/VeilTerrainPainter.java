package stairwaytoheaven.worldgen;

import stairwaytoheaven.SkyRegistry;

/**
 * The Veil's ground, kept whole after the Veil stopped being a world.
 *
 * <p>{@code docs/PLAN_ONE_PLANE.md} retired the {@code veil2} dimension and
 * {@code WORLD_DESIGN} §41.5 says where its ground went:
 *
 * <blockquote>Gloomfen and Ashen Reach, with murkwater, blackpeat, murkmoss,
 * ashsand, deadtree, ashbones, gloomshroom, whisperreeds and the Gloom Shade,
 * are a Ghost Realm in everything but name (§10). They move from the
 * {@code veil2} dimension into the one world at the Ghost Realm's realmDepth
 * band.</blockquote>
 *
 * <p>So this class no longer paints a region — there is no level to paint. It
 * is now the LIBRARY those two bands read:
 *
 * <ul>
 *   <li>{@code realms.ghost.GhostTerrainPainter} calls {@link #rollObject} and
 *       the biome split below for the fen inside the Ghost band, which is
 *       Gloomfen and Ashen Reach exactly as they shipped.</li>
 *   <li>{@code realms.crooked.CrookedTerrainPainter} calls {@link #isHollow}
 *       and {@link #rollHollowObject} for the Beetlefreak Hollows inside the
 *       Crooked band, which is where §41.5 puts them.</li>
 * </ul>
 *
 * <p>Every number below is the one that was measured for the Veil. Nothing was
 * retuned by the move, so both bands read as the place the Veil was.
 */
public final class VeilTerrainPainter {

    /** Kept for the record: the shape the Veil's own landmasses were measured at. */
    public static final int ISLAND_SCALE = 56;
    public static final float ISLAND_THRESHOLD = 0.44F;
    public static final int BIOME_SCALE = 150;
    public static final float ASHEN_BELOW = 0.30F;
    public static final int PATCH_SCALE = 20;
    public static final float PATCH_THRESHOLD = 0.66F;
    public static final int SALT_BIOME = 5;
    public static final int SALT_PATCH = 9;
    public static final int SALT_OBJECT_ROLL = 13;

    /**
     * Beetlefreak Hollows: a THIRD band, cut out of the other two rather than
     * tiled beside them.
     *
     * A small scale (39) makes the patches compact instead of continental, and
     * a high threshold makes them rare -- the fen must stay the Veil's normal
     * state or the wrongness stops reading as wrong.
     *
     * The threshold is MEASURED, not guessed. 0.615 was the first guess and it
     * painted 23.9% of walkable ground (the veilstatus survey said so, which is
     * why that survey exists): a quarter of the layer is not a rare wrong place,
     * it is a third landscape. Sweeping the same noise offline over 2.0M land
     * tiles gave 0.660 -> 18.0%, 0.700 -> 11.8%, 0.740 -> 6.9%, 0.780 -> 3.6%,
     * 0.820 -> 1.6%. 0.780 is the value that matches the intent.
     */
    public static final int HOLLOW_SCALE = 39;
    public static final float HOLLOW_THRESHOLD = 0.78F;
    public static final int SALT_HOLLOW = 21;
    public static final int SALT_HOLLOW_OBJECT = 23;

    private VeilTerrainPainter() {
    }

    public static int rollObject(int seed, int tileX, int tileY, boolean isAshen, boolean isPatch) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_OBJECT_ROLL);
        if (isAshen) {
            if (roll < 0.085F) return SkyRegistry.veilrockID;
            if (roll < 0.105F) return SkyRegistry.ashbonesID;
            if (roll < 0.112F) return SkyRegistry.gloomshroomID;
            return 0;
        }
        if (roll < 0.085F) {
            return isPatch ? SkyRegistry.veilrockID : SkyRegistry.whisperreedsID;
        }
        if (roll < 0.110F) return SkyRegistry.veilrockID;
        if (roll < 0.132F) return SkyRegistry.gloomshroomID;
        if (roll < 0.143F) return SkyRegistry.deadtreeID;
        if (roll < 0.147F) return SkyRegistry.ashbonesID;
        return 0;
    }

    /**
     * Is this tile inside a Beetlefreak Hollow? Pure noise, no world state, so
     * the Crooked House placement test can ask the same question the painter
     * will answer later and get the same answer.
     */
    public static boolean isHollow(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + SALT_HOLLOW, tileX, tileY, HOLLOW_SCALE, 2) > HOLLOW_THRESHOLD;
    }

    /**
     * Is this tile dry land? Same question, same reason — but against the ONE
     * plane's coastline, because that is the ground the Hollows are painted on
     * now.
     */
    public static boolean isLand(int seed, int tileX, int tileY) {
        float depth = RealmDepth.depthAt(tileX, tileY,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed));
        float island = SkyNoise.fbm(seed, tileX, tileY, SkyTerrainPainter.ISLAND_SCALE, 3);
        return island > SkyTerrainPainter.waterlineAt(depth) + SkyTerrainPainter.ISLAND_RIM;
    }

    /**
     * The Hollows' own prop mix: sparser than the fen and made of the wrong
     * things. Dead trees and bones cluster here; the reeds and the healthy
     * shrooms do not grow in it at all.
     */
    public static int rollHollowObject(int seed, int tileX, int tileY) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_HOLLOW_OBJECT);
        if (roll < 0.055F) return SkyRegistry.deadtreeID;
        if (roll < 0.085F) return SkyRegistry.ashbonesID;
        if (roll < 0.100F) return SkyRegistry.veilrockID;
        if (roll < 0.108F) return SkyRegistry.gloomshroomID;
        return 0;
    }
}
