package stairwaytoheaven.realms.crooked;

import necesse.engine.registries.ObjectLayerRegistry;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.worldgen.SkyNoise;

/**
 * Per-region terrain painter of Crooked Beyond.
 *
 * <p>Built the way {@link stairwaytoheaven.worldgen.SkyTerrainPainter} and
 * {@link stairwaytoheaven.worldgen.VeilTerrainPainter} are built, and it keeps
 * their contract: <b>everything here is a pure function of the level seed and
 * the tile position</b>. No world state is read anywhere, so region borders are
 * seamless, the preset placer can ask the same question the painter will answer
 * later and get the same answer, and two clients cannot disagree about what the
 * ground is.
 *
 * <h2>The shape of the realm</h2>
 * <ol>
 * <li><b>Landmasses in the Spill.</b> One fBm field at scale 62 over a
 *     threshold of 0.44, the Veil's proportions at a larger scale — bigger
 *     pieces with wider green between them, because this realm's landmarks are
 *     buildings and a building wants somewhere to stand.</li>
 * <li><b>Three bands.</b> The Striped Waste is the default. The Spiral Fields
 *     are a broad second band from a slow noise field. The Checkerworks are cut
 *     out of both by a third, small-scale, high-threshold field, so they are
 *     compact and rare rather than continental — the Beetlefreak Hollows'
 *     construction, for the Hollows' reason.</li>
 * <li><b>Wrong-way runs.</b> Long thin ridges of {@link WrongWayTile} taken from
 *     a stretched noise field: a band of black that runs across the ground like
 *     a path and then simply stops. This is the realm's theme made walkable and
 *     is deliberately not rare — A3.6's "paths that run visibly wrong" only
 *     works if you meet one.</li>
 * <li><b>Places.</b> {@link CrookedSites}' three POI lattices pave their own
 *     ground in Checker Stone and keep every natural prop off it, so a building
 *     never generates half-buried in spiral trees.</li>
 * </ol>
 *
 * <h2>Density</h2>
 * Prop rates are deliberately at the Outlands' shipped values (about 0.11
 * objects per land tile) rather than at the bright Skyreach's 0.31-0.38.
 * {@code WORLD_DESIGN.md} A4.2: a run should end with the player still wanting
 * something specific. Crooked Beyond is a place you travel to, and a landscape
 * you can see across is what makes its buildings visible from a distance.
 */
public final class CrookedTerrainPainter {

    // --- landmasses ---------------------------------------------------------

    /** Island field scale. Larger than the Veil's 56: buildings need room. */
    public static final float ISLAND_SCALE = 62.0F;
    public static final float ISLAND_THRESHOLD = 0.44F;
    /** Tiles inside the coastline where nothing is placed (see below). */
    public static final float ISLAND_RIM = 0.02F;

    // --- the three bands ----------------------------------------------------

    /** Spiral Fields: a broad, slow band, roughly a third of the land. */
    public static final float FIELDS_SCALE = 138.0F;
    public static final float FIELDS_ABOVE = 0.56F;

    /**
     * Checkerworks: compact and rare, cut out of the other two.
     *
     * <p>The threshold is taken from the measured sweep recorded in
     * {@code VeilTerrainPainter.HOLLOW_THRESHOLD} — 0.660 covers 18.0% of land,
     * 0.700 11.8%, 0.740 6.9%, 0.780 3.6%, 0.820 1.6%. 0.740 puts the built
     * ground at about <b>7% of the realm</b>: often enough that a player
     * crossing the realm meets one, rare enough that it stays the exception the
     * design calls for. The scale is 41, near the Hollows' 39, which is what
     * makes a patch a compact block instead of a continent.
     */
    public static final float CHECKER_SCALE = 41.0F;
    public static final float CHECKER_THRESHOLD = 0.74F;

    // --- the wrong-way runs -------------------------------------------------

    /**
     * The black runs, from a field sampled with X and Y at different scales.
     *
     * <p>Sampling {@code fbm(x / 5, y)} stretches the noise fivefold along one
     * axis, which turns round blobs into long ridges — the cheapest way to get
     * something that reads as a PATH rather than as a patch, and the reason this
     * is not simply a third biome band. The threshold band is narrow (a run is
     * the sliver of the field between 0.62 and 0.66) so the runs come out a few
     * tiles wide.
     */
    public static final float RUN_SCALE = 90.0F;
    public static final float RUN_STRETCH = 5.0F;
    public static final float RUN_LOW = 0.62F;
    public static final float RUN_HIGH = 0.66F;

    // --- violet mud ---------------------------------------------------------

    /** Mud pools: small, common, and what interrupts the stripes. */
    public static final float MUD_SCALE = 22.0F;
    public static final float MUD_THRESHOLD = 0.68F;

    // --- salts --------------------------------------------------------------

    public static final int SALT_FIELDS = 31;
    public static final int SALT_CHECKER = 37;
    public static final int SALT_RUN = 41;
    public static final int SALT_MUD = 43;
    public static final int SALT_OBJECT = 47;
    public static final int SALT_CRATE = 53;

    /**
     * Chance of a salvage crate on a land tile.
     *
     * <p>The Skyreach's barren grounds run 0.0016; this is the same figure. A
     * crate is the mod's only container outside a preset, and vanilla's whole
     * exploration loop is containers — the player's own words were that the sky
     * had nothing to open, unlike vanilla where <i>"es gibt immer mal wieder
     * Kisten"</i>.
     */
    public static final float CRATE_CHANCE = 0.0016F;

    private CrookedTerrainPainter() {
    }

    public static void paintRegion(Region region, int seed) {
        int tileWidth = region.tileLayer.region.tileWidth;
        int tileHeight = region.tileLayer.region.tileHeight;
        for (int rx = 0; rx < tileWidth; rx++) {
            for (int ry = 0; ry < tileHeight; ry++) {
                int tileX = rx + region.tileXOffset;
                int tileY = ry + region.tileYOffset;

                region.biomeLayer.setBiomeByRegion(rx, ry, biomeAt(seed, tileX, tileY));

                long packed = describeTile(seed, tileX, tileY);
                region.tileLayer.setTileByRegion(rx, ry, tileOf(packed));
                int objectID = objectOf(packed);
                if (objectID != 0) {
                    region.objectLayer.setObjectByRegion(
                            ObjectLayerRegistry.BASE_LAYER, rx, ry, objectID);
                }
            }
        }
    }

    // ---- the pure functions everything else asks -----------------------------

    /** Is this tile dry land? */
    public static boolean isLand(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed, tileX, tileY, ISLAND_SCALE, 3) > ISLAND_THRESHOLD;
    }

    /**
     * Is this tile far enough inside the coastline to carry something?
     *
     * <p>The realm's islands are small and most tiles border the Spill. Right
     * after region generation the liquid height map is still settling, so
     * {@code Level.isShore} reports true across a fresh island and
     * {@code Region.checkGenerationValid} would sweep away anything standing on
     * one. The Skyreach hit this exact bug and answered it two ways at once: a
     * rim margin here, and {@code canPlaceOnShore} on every natural object (see
     * {@link CrookedObjects}). Both are kept.
     */
    public static boolean isPlantable(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed, tileX, tileY, ISLAND_SCALE, 3) > ISLAND_THRESHOLD + ISLAND_RIM;
    }

    /** Which of the three bands this tile is in. */
    public static int biomeAt(int seed, int tileX, int tileY) {
        if (isChecker(seed, tileX, tileY)) {
            return CrookedRealm.checkerworks.getID();
        }
        if (isFields(seed, tileX, tileY)) {
            return CrookedRealm.spiralFields.getID();
        }
        return CrookedRealm.stripedWaste.getID();
    }

    public static boolean isFields(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + SALT_FIELDS, tileX, tileY, FIELDS_SCALE, 2) > FIELDS_ABOVE;
    }

    public static boolean isChecker(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + SALT_CHECKER, tileX, tileY, CHECKER_SCALE, 2) > CHECKER_THRESHOLD;
    }

    /** Is this tile inside a wrong-way run? */
    public static boolean isWrongWay(int seed, int tileX, int tileY) {
        float v = SkyNoise.fbm(seed + SALT_RUN, tileX / RUN_STRETCH, tileY, RUN_SCALE, 2);
        return v > RUN_LOW && v < RUN_HIGH;
    }

    /**
     * Distance in tiles to the nearest POI, and whether we are on its paved
     * apron. A site paves a disc of this radius so a building always has a
     * forecourt to stand on and to be seen across.
     */
    public static final float SITE_APRON = 11.0F;

    /**
     * Everything about one tile, packed: {@code tileID | objectID << 20}.
     *
     * <p>Packed the way {@code SkyTerrainPainter.describeTile} packs its answer,
     * and for the same reason: the placement test, the painter and any offline
     * renderer all want the same answer and must not each re-derive half of it.
     */
    public static long describeTile(int seed, int tileX, int tileY) {
        if (!isLand(seed, tileX, tileY)) {
            return pack(CrookedRealm.spillID, 0);
        }

        float siteDist = CrookedSites.siteDistance(seed, tileX, tileY);
        boolean paved = siteDist <= SITE_APRON;

        // --- ground ---
        int tileID;
        if (paved) {
            // A site paves its own forecourt, whatever band it landed in. This
            // is what makes a building visible from across the Spill instead of
            // being swallowed by the stripes.
            tileID = CrookedRealm.checkerStoneID;
        } else if (isWrongWay(seed, tileX, tileY)) {
            tileID = CrookedRealm.wrongWayID;
        } else if (isChecker(seed, tileX, tileY)) {
            tileID = CrookedRealm.checkerStoneID;
        } else if (isFields(seed, tileX, tileY)) {
            tileID = CrookedRealm.spiralSoilID;
        } else if (SkyNoise.fbm(seed + SALT_MUD, tileX, tileY, MUD_SCALE, 2) > MUD_THRESHOLD) {
            tileID = CrookedRealm.violetMudID;
        } else {
            tileID = CrookedRealm.crookedStripeID;
        }

        // --- what stands on it ---
        // Nothing on a paved forecourt (the preset owns those tiles), nothing on
        // a wrong-way run (a run you cannot see is not a run), and nothing on
        // the coastal rim (checkGenerationValid would only sweep it away).
        if (paved || tileID == CrookedRealm.wrongWayID || !isPlantable(seed, tileX, tileY)) {
            return pack(tileID, 0);
        }

        // Crates come first, before every growth rule: they are the rarest
        // thing out here and grass must not win the tile from them. Same
        // ordering mistake the Skyreach painter records.
        if (SkyNoise.tileRoll(seed, tileX, tileY, SALT_CRATE) < CRATE_CHANCE) {
            return pack(tileID, CrookedRealm.crookedCrateID);
        }

        return pack(tileID, rollObject(seed, tileX, tileY, tileID));
    }

    /**
     * The prop mix, by ground.
     *
     * <p>Rates total about 0.11 objects per land tile on the growing ground and
     * about 0.05 on the built and striped ground — the Outlands' own shipped
     * figures, so this realm reads as the same kind of place its rim does rather
     * than as a density nobody measured.
     */
    public static int rollObject(int seed, int tileX, int tileY, int groundID) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_OBJECT);

        if (groundID == CrookedRealm.spiralSoilID) {
            // The growing ground: this is where the harvest is.
            if (roll < 0.030F) return CrookedRealm.spiralTreeID;
            if (roll < 0.058F) return CrookedRealm.bentGrassID;
            if (roll < 0.078F) return CrookedRealm.eyeballShrubID;
            if (roll < 0.092F) return CrookedRealm.stripedMushroomID;
            if (roll < 0.102F) return CrookedRealm.screamingFlowerID;
            if (roll < 0.110F) return CrookedRealm.bentLanternID;
            return 0;
        }

        if (groundID == CrookedRealm.checkerStoneID) {
            // The built ground: furniture with nobody at it.
            if (roll < 0.016F) return CrookedRealm.crookedClockID;
            if (roll < 0.030F) return CrookedRealm.longChairID;
            if (roll < 0.040F) return CrookedRealm.groundWindowID;
            if (roll < 0.048F) return CrookedRealm.bentLanternID;
            if (roll < 0.054F) return CrookedRealm.teethRockID;
            return 0;
        }

        // The stripes and the mud: bare, with the massifs that make the
        // horizon. evilwall is the mod's own crystal massif, already registered
        // for the Outlands rim — reused rather than duplicated, which is what
        // keeps rim and realm looking like one place.
        if (roll < 0.022F) return CrookedRealm.teethRockID;
        if (roll < 0.034F) return stairwaytoheaven.SkyRegistry.evilwallID;
        if (roll < 0.044F) return CrookedRealm.bentGrassID;
        if (roll < 0.050F) return CrookedRealm.eyeballShrubID;
        return 0;
    }

    // ---- packing -------------------------------------------------------------

    private static long pack(int tileID, int objectID) {
        return (tileID & 0xFFFFFL) | ((long) (objectID & 0xFFFFF) << 20);
    }

    public static int tileOf(long packed) {
        return (int) (packed & 0xFFFFFL);
    }

    public static int objectOf(long packed) {
        return (int) ((packed >>> 20) & 0xFFFFFL);
    }
}
