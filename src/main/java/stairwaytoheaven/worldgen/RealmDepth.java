package stairwaytoheaven.worldgen;

/**
 * How far from home the world is, and which realm that makes it.
 *
 * <p>This is the spine of {@code docs/WORLD_DESIGN.md} §3. The concept is one
 * connected overworld whose mood changes with distance from the Skyreach tower:
 * distance becomes {@link #depthAt} (0.0 at the spire, 1.0 at the far end), and
 * depth becomes biome WEIGHTS rather than hard zones, so neighbouring realms
 * overlap and no seed looks like a set of rings.
 *
 * <h2>Everything that decides a realm lives here</h2>
 * Before this class, distance rules were scattered: {@code SkyOrigin} had
 * CORE/MID radii for ore density, {@code SkyOutlands} had its own 900/3000
 * ramp. Two places deciding "how far out is far" is how they drift apart. Those
 * stay for what they do, but the REALM question is answered only here.
 *
 * <h2>The one dial</h2>
 * {@link #DEPTH_SCALE} is the single number that sets how big the world is, and
 * {@code docs/WORLD_DESIGN.md} §42.1 records it as the decision that had to be
 * made before anything downstream could be built. It is deliberately one
 * constant: changing the world's size must never mean editing nine bands.
 *
 * <h2>Everything here is a pure function</h2>
 * Same contract as the rest of worldgen — seed and tile position only, no world
 * state — so region borders are seamless, the offline renderer and the live
 * world agree, and a structure placer can ask the same question the painter
 * will answer later.
 */
public final class RealmDepth {

    /**
     * Distance in tiles at which {@link #depthAt} reaches 1.0.
     *
     * <p><b>This is the world's size, and it is the dial to turn.</b> At 6000
     * the concept's bands land like this:
     *
     * <pre>
     *   Skyreach         0.00-0.15      0 - 900
     *   Eden             0.20-0.42   1200 - 2520
     *   Steinfeld        0.42-0.58   2520 - 3480
     *   Ghost Realm      0.60-0.80   3600 - 4800
     *   Crooked Beyond   0.70-0.88   4200 - 5280
     *   Hell             0.90-1.00   5400 - 6000+
     * </pre>
     *
     * <p>6000 was chosen over the 12000 first sketched in WORLD_DESIGN §42.1
     * because §40 requires the player to keep RETURNING to earlier realms for
     * materials, and §42.2 records that this mod has no travel system at all
     * yet. Twelve thousand tiles each way, repeatedly, across an island world
     * split by open Mistsea, is a loop nobody runs twice. Six is already
     * generous and is the largest number that stays honest until waypoints
     * exist. <b>Revisit this the day travel lands.</b>
     */
    public static final float DEPTH_SCALE = 6000.0F;

    // ------------------------------------------------------------------
    // The realms, in progression order. Ordinals ARE the progression, and
    // they are read by the map dumps, so append rather than renumber.
    // ------------------------------------------------------------------

    public static final int REALM_SKYREACH = 0;
    public static final int REALM_EDEN = 1;
    public static final int REALM_STEINFELD = 2;
    public static final int REALM_GHOST = 3;
    public static final int REALM_CROOKED = 4;
    public static final int REALM_HELL = 5;
    public static final int REALM_COUNT = 6;

    /**
     * Each realm's band as [start, peak-start, peak-end, end] in depth.
     *
     * <p>Read as a trapezoid: weight climbs from {@code start} to
     * {@code peakStart}, is full between the peaks, and falls to zero at
     * {@code end}. The overlaps between consecutive rows are what §3 means by
     * "benachbarte Progressionsstufen überlappen stark" — at depth 0.75 both
     * Ghost and Crooked have weight, and which one an island gets is decided by
     * noise, not by a boundary.
     *
     * <p>Numbers are WORLD_DESIGN §3's own table, converted from its overlapping
     * ranges into trapezoids that reproduce the same overlaps.
     */
    private static final float[][] BANDS = {
            //             start  peakStart peakEnd  end
            /* SKYREACH  */ {0.00F, 0.00F, 0.15F, 0.30F},
            /* EDEN      */ {0.10F, 0.20F, 0.42F, 0.48F},
            /* STEINFELD */ {0.32F, 0.42F, 0.58F, 0.70F},
            /* GHOST     */ {0.48F, 0.60F, 0.80F, 0.88F},
            /* CROOKED   */ {0.70F, 0.80F, 0.88F, 0.94F},
            /* HELL      */ {0.80F, 0.90F, 1.00F, 1.00F},
    };

    /**
     * Scale of the noise that picks between realms where their bands overlap.
     *
     * <p>Large on purpose: an island should belong to one realm, and its
     * neighbour a few hundred tiles away may belong to the next. At a small
     * scale the overlap would shred into a checkerboard of two realms, which is
     * the opposite of what §3 asks for.
     */
    public static final float PICK_SCALE = 220.0F;
    public static final long SALT_REALM_PICK = 0x2EA1;

    /**
     * Scale and salt of the {@code distortion} field (§3): how WRONG an island
     * is within its own realm. A Ghost island at low distortion is a classic
     * graveyard; at high distortion it has floating gravestones and bent trees.
     *
     * <p>Independent of the realm pick, so a realm's calm and mad versions
     * appear at the same distance rather than the madness simply being further
     * out — that is what stops the world from reading as concentric rings even
     * where one realm runs for a thousand tiles.
     */
    public static final float DISTORTION_SCALE = 130.0F;
    public static final long SALT_DISTORTION = 0xD1570;

    private RealmDepth() {
    }

    /** Depth of a distance in tiles: 0.0 at the origin, 1.0 at DEPTH_SCALE. */
    public static float depthFor(float distanceFromOrigin) {
        if (distanceFromOrigin <= 0.0F) {
            return 0.0F;
        }
        float d = distanceFromOrigin / DEPTH_SCALE;
        return d >= 1.0F ? 1.0F : d;
    }

    /** Depth of a tile, given the world's origin. */
    public static float depthAt(int tileX, int tileY, int originX, int originY) {
        float dx = tileX - originX;
        float dy = tileY - originY;
        return depthFor((float) Math.sqrt(dx * dx + dy * dy));
    }

    /**
     * A realm's weight at a given depth: 0 outside its band, 1 across its peak,
     * linear on the two shoulders.
     */
    public static float weightOf(int realm, float depth) {
        float[] b = BANDS[realm];
        if (depth <= b[0] || depth >= b[3]) {
            return 0.0F;
        }
        if (depth < b[1]) {
            return b[1] <= b[0] ? 1.0F : (depth - b[0]) / (b[1] - b[0]);
        }
        if (depth > b[2]) {
            return b[3] <= b[2] ? 1.0F : (b[3] - depth) / (b[3] - b[2]);
        }
        return 1.0F;
    }

    /**
     * Which realm a tile belongs to.
     *
     * <p>The pick is weighted, not nearest-band: every realm with weight at this
     * depth gets a slice of the 0..1 noise value proportional to that weight,
     * and one low-frequency noise field decides which slice the tile falls in.
     * Because the field is smooth and coarse, neighbouring tiles overwhelmingly
     * land in the same slice, so realms come out as regions rather than static.
     */
    public static int realmAt(int seed, int tileX, int tileY, int originX, int originY) {
        float depth = depthAt(tileX, tileY, originX, originY);
        return realmForDepth(seed, tileX, tileY, depth);
    }

    /** The same pick for a depth already computed (allocation-free hot path). */
    public static int realmForDepth(int seed, int tileX, int tileY, float depth) {
        float total = 0.0F;
        for (int r = 0; r < REALM_COUNT; r++) {
            total += weightOf(r, depth);
        }
        if (total <= 0.0F) {
            // Past the last band's end, or a gap nobody covers: the deepest
            // realm owns it. Depth 1.0 must always be Hell, never "nothing".
            return depth > 0.5F ? REALM_HELL : REALM_SKYREACH;
        }
        float roll = SkyNoise.fbm(seed + SALT_REALM_PICK, tileX, tileY, PICK_SCALE, 2) * total;
        float running = 0.0F;
        for (int r = 0; r < REALM_COUNT; r++) {
            running += weightOf(r, depth);
            if (roll < running) {
                return r;
            }
        }
        return REALM_COUNT - 1;
    }

    /**
     * How wrong this tile is within its own realm, 0..1 (§3's {@code
     * distortion}).
     *
     * <p>Rises with depth as well as with its own noise: the same realm should
     * be madder at its outer edge than at its inner one, which is what keeps a
     * long realm from reading as flat.
     */
    public static float distortionAt(int seed, int tileX, int tileY, float depth) {
        float field = SkyNoise.fbm(seed + SALT_DISTORTION, tileX, tileY, DISTORTION_SCALE, 3);
        float blended = field * 0.65F + depth * 0.35F;
        return blended < 0.0F ? 0.0F : (blended > 1.0F ? 1.0F : blended);
    }

    /** The depth at which a realm's band opens (its trapezoid's left foot). */
    public static float bandStart(int realm) {
        return BANDS[realm][0];
    }

    /** The depth at which a realm's band closes (its trapezoid's right foot). */
    public static float bandEnd(int realm) {
        return BANDS[realm][3];
    }

    /** The depth at which a realm's band reaches full weight. */
    public static float bandPeakStart(int realm) {
        return BANDS[realm][1];
    }

    /** The last depth at which a realm still has full weight. */
    public static float bandPeakEnd(int realm) {
        return BANDS[realm][2];
    }

    /**
     * How far through its OWN band a depth is: 0 at the band's inner foot, 1 at
     * its outer one, clamped.
     *
     * <p>This is what lets a realm keep an internal gradient — Steinfeld's
     * "order decays" ramp is the clearest case — without inventing a second
     * distance system beside this one. A realm asks how deep into ITSELF the
     * tile is; the plane still answers with one distance from one origin.
     */
    public static float localDepth(int realm, float depth) {
        float[] b = BANDS[realm];
        float span = b[3] - b[0];
        if (span <= 0.0F) {
            return 0.0F;
        }
        float local = (depth - b[0]) / span;
        return local < 0.0F ? 0.0F : (local > 1.0F ? 1.0F : local);
    }

    /** Stable lowercase key per realm — locale, ledger and map dumps share it. */
    public static String keyOf(int realm) {
        switch (realm) {
            case REALM_EDEN: return "eden";
            case REALM_STEINFELD: return "steinfeld";
            case REALM_GHOST: return "ghostrealm";
            case REALM_CROOKED: return "crookedbeyond";
            case REALM_HELL: return "hell";
            default: return "skyreach";
        }
    }
}
