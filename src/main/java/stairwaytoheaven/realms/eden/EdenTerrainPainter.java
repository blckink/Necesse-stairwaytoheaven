package stairwaytoheaven.realms.eden;

import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.SkyTerrainPainter;

/**
 * The Garden of Eden as a BAND of the one plane.
 *
 * <p><b>This is no longer a level's painter.</b> {@code docs/PLAN_ONE_PLANE.md}
 * retired the {@code eden2} dimension: Eden is the realm {@link RealmDepth}
 * gives depth 0.20-0.42, and {@link SkyTerrainPainter#describeTile} calls
 * {@link #describeBand} for every tile the realm pick lands here -- exactly the
 * way it already called {@code SkyOutlands} for Crooked's rim
 * ({@code WORLD_DESIGN} §41.4). Nothing below changed except where the land
 * comes from: the plane owns ONE island field and ONE waterline, and this
 * painter reads them instead of cutting its own coastline out of the same noise
 * at a different scale.
 *
 * <p><b>The shape of the realm.</b> {@code docs/WORLD_DESIGN.md} A3.3 asks for
 * <i>"waterfalls, lagoons, white sand, turquoise water, clearings with absurdly
 * large fruit"</i> and §5 for <i>"an exaggerated biological explosion: big,
 * lush, dense, warm, colourful, alive"</i>. So Eden is not the Skyreach's
 * archipelago with green paint on it: the land is broad and continuous and the
 * WATER is what is cut into it — wide lagoons with white sand beaches around
 * them, the way a tropical island reads from above. Eden's waterline is
 * therefore the lowest of any realm but Steinfeld's (0.40 against the
 * Skyreach's 0.48, see {@code SkyTerrainPainter.REALM_WATERLINE}), which leaves
 * most of the band walkable and lets the shallows form as basins rather than as
 * an ocean with islands in it.
 *
 * <p><b>The three zones (§5, three biomes minimum).</b>
 * <ul>
 * <li><b>Eden Garden</b> — the common ground. Eden Grass (the player's own
 *     supplied {@code overgrowneden} art) with Eden Moss patches, and the
 *     densest flora in the mod.</li>
 * <li><b>Eden Shallows</b> — the lagoon rim: White Paradise Sand and Turquoise
 *     Shallow Water, lotus and reeds and shells, the calm zone.</li>
 * <li><b>Eden Canopy</b> — under the giant trees. Root Floor and Rich Eden
 *     Soil, little light reaching the ground, the Knowledge Trees and the
 *     Forbidden Serpent.</li>
 * </ul>
 *
 * <p><b>Density.</b> {@code IMPLEMENTATION_RULES} §8 — compose scenes, do not
 * scatter — and A4.2, resources are scarce. Those pull in opposite directions
 * here and Eden is the one realm where the first wins on VEGETATION and the
 * second still wins on RESOURCES: leaves are everywhere, fruit and ore are not.
 * Concretely, the garden's total plant density is ~0.42 objects per land tile
 * (against the Skyreach's measured 0.31-0.38 on its richest ground), while a
 * Tree of Plenty sits at 0.4% of tiles and an Eden Copper rock at 0.35%.
 *
 * <p>Everything here is a pure function of {@code (seed, tileX, tileY)}, the
 * same contract as {@code SkyTerrainPainter} — the painter, the POI placer and
 * the pressure field all get the same answer without any world state.
 */
public final class EdenTerrainPainter {

    // ---- the land ---------------------------------------------------------

    /**
     * Big, soft landmasses: lagoons are cut into land, not islands in a sea.
     *
     * <p>The FIELD is now the plane's own
     * ({@code SkyTerrainPainter.ISLAND_SCALE}), because one connected overworld
     * has one coastline. What Eden keeps is its WATERLINE:
     * {@code SkyTerrainPainter.REALM_WATERLINE[REALM_EDEN]} = 0.40, the lowest
     * of any realm but Steinfeld, which is what makes this the greenest and
     * most walkable band. The two constants below are kept for the record and
     * for the offline harness that measured them.
     */
    public static final int ISLAND_SCALE = 92;
    public static final float ISLAND_THRESHOLD = 0.36F;
    /** Below this much above the waterline, the tile is beach rather than garden. */
    public static final float SHORE_BAND = 0.055F;

    /** Which of the three zones a tile belongs to. */
    public static final int BIOME_SCALE = 170;
    public static final float CANOPY_ABOVE = 0.585F;
    public static final int SALT_BIOME = 41;

    /** Moss/soil patches inside a zone, so no zone is one flat colour. */
    public static final int PATCH_SCALE = 22;
    public static final float PATCH_THRESHOLD = 0.63F;
    public static final int SALT_PATCH = 43;

    public static final int SALT_OBJECT_ROLL = 47;
    public static final int SALT_CANOPY_ROLL = 53;
    public static final int SALT_SHORE_ROLL = 59;

    // ---- the places -------------------------------------------------------
    //
    // Three POI lattices, each read exactly the way SkyTerrainPainter reads its
    // wreck and workshop lattices (see SkyTerrainPainter.nearestSite, which
    // this reuses rather than re-deriving): salt decides whether a cell holds a
    // site, salt+1/salt+2 place it inside the cell, salt+3 is the site's own
    // hash. Cell sizes are the dial for rarity.

    /** Knowledge Grove — the Forbidden Serpent's place. Rarest. */
    public static final int GROVE_CELL = 420;
    public static final int SALT_GROVE = 211;
    public static final float GROVE_CHANCE = 0.34F;

    /** Lagoon Shrine — on the white sand, guarded from the water side. */
    public static final int LAGOON_CELL = 340;
    public static final int SALT_LAGOON = 223;
    public static final float LAGOON_CHANCE = 0.42F;

    /** Orchard Ring — the fruit cache, the most common of the three. */
    public static final int ORCHARD_CELL = 280;
    public static final int SALT_ORCHARD = 227;
    public static final float ORCHARD_CHANCE = 0.46F;

    /**
     * Knowledge Trees. Not a POI — a single rare object — but placed off a
     * lattice rather than off a per-tile roll, because A3.3 wants the ground
     * AROUND one to change ({@link EdenPressure#KNOWLEDGE_TICKETS}) and a
     * per-tile roll gives nothing to measure a distance from.
     */
    public static final int KNOWLEDGE_CELL = 240;
    public static final int SALT_KNOWLEDGE = 229;
    public static final float KNOWLEDGE_CHANCE = 0.50F;

    // ---- describeTile packing --------------------------------------------

    public static final int BIOME_GARDEN = 0;
    public static final int BIOME_SHALLOWS = 1;
    public static final int BIOME_CANOPY = 2;

    private EdenTerrainPainter() {
    }

    /**
     * One tile of the Eden band: ground, object and sub-biome, packed the way
     * {@link SkyTerrainPainter#pack} packs every tile of the plane.
     *
     * @param island    the plane's shared island field at this tile
     * @param waterline the plane's blended waterline at this depth
     */
    public static long describeBand(int seed, int tileX, int tileY,
            float island, float waterline, float depth, float distortion) {
        int biome = bandBiomeAt(seed, tileX, tileY, island, waterline);
        int biomeClass = biomeClassOf(biome);
        int tile = bandTileAt(seed, tileX, tileY, biome, island, waterline);
        if (island <= waterline + SkyTerrainPainter.ISLAND_RIM) {
            // Water, and the first walkable ring of beach: nothing stands here
            // or checkGenerationValid would only sweep it away again.
            return SkyTerrainPainter.pack(tile, 0, biomeClass, false);
        }
        return SkyTerrainPainter.pack(tile,
                objectAt(seed, tileX, tileY, biome, island, waterline), biomeClass, false);
    }

    /** Zone code -> the plane's biome class (see {@link SkyTerrainPainter}). */
    public static int biomeClassOf(int biome) {
        if (biome == BIOME_SHALLOWS) {
            return SkyTerrainPainter.BIOME_EDEN_SHALLOWS;
        }
        return biome == BIOME_CANOPY
                ? SkyTerrainPainter.BIOME_EDEN_CANOPY
                : SkyTerrainPainter.BIOME_EDEN_GARDEN;
    }

    // ---- pure queries the POI placer and the pressure field share ---------

    /**
     * Is this tile dry Eden ground?
     *
     * <p>Asked by the POI placer and the pressure field, neither of which has a
     * region in hand. It answers against the PLANE's island field and waterline,
     * so it cannot disagree with what {@link #describeBand} paints later.
     */
    public static boolean isLand(int seed, int tileX, int tileY) {
        float depth = RealmDepth.depthAt(tileX, tileY,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed));
        float island = SkyNoise.fbm(seed, tileX, tileY, SkyTerrainPainter.ISLAND_SCALE, 3);
        return island > SkyTerrainPainter.waterlineAt(depth) + SkyTerrainPainter.ISLAND_RIM;
    }

    /** Is this tile inside the Eden band at all? */
    public static boolean isEden(int seed, int tileX, int tileY) {
        return RealmDepth.realmAt(seed, tileX, tileY,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed)) == RealmDepth.REALM_EDEN;
    }

    /** Which of the three zones, given the plane's land field. */
    public static int bandBiomeAt(int seed, int tileX, int tileY, float island, float waterline) {
        if (island <= waterline + SHORE_BAND) {
            // The water and the beach around it are one zone: a lagoon is its
            // rim as much as its middle, and a biome that stopped at the
            // waterline would put the sand in the garden's spawn table.
            return BIOME_SHALLOWS;
        }
        float zone = SkyNoise.fbm(seed + SALT_BIOME, tileX, tileY, BIOME_SCALE, 2);
        return zone > CANOPY_ABOVE ? BIOME_CANOPY : BIOME_GARDEN;
    }

    /** Which of the three zones, for a caller that has only a position. */
    public static int biomeAt(int seed, int tileX, int tileY) {
        float depth = RealmDepth.depthAt(tileX, tileY,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed));
        float island = SkyNoise.fbm(seed, tileX, tileY, SkyTerrainPainter.ISLAND_SCALE, 3);
        return bandBiomeAt(seed, tileX, tileY, island, SkyTerrainPainter.waterlineAt(depth));
    }

    public static int biomeID(int biome) {
        if (biome == BIOME_SHALLOWS) {
            return EdenRealm.shallows.getID();
        }
        return (biome == BIOME_CANOPY ? EdenRealm.canopy : EdenRealm.garden).getID();
    }

    /** The ground tile at a position, water included. */
    public static int bandTileAt(int seed, int tileX, int tileY, int biome,
            float island, float waterline) {
        if (island <= waterline) {
            return EdenRealm.shallowsTileID;
        }
        boolean patch = SkyNoise.fbm(seed + SALT_PATCH, tileX, tileY, PATCH_SCALE, 2) > PATCH_THRESHOLD;
        if (island <= waterline + SHORE_BAND) {
            return EdenRealm.paradiseSandID;
        }
        if (biome == BIOME_CANOPY) {
            return patch ? EdenRealm.edenSoilID : EdenRealm.edenRootFloorID;
        }
        // The garden: the player's own Eden grass, with moss where it thins.
        return patch ? EdenRealm.edenMossID : SkyRegistry.overgrownEdenID;
    }

    /** {@link stairwaytoheaven.worldgen.SkyTerrainPainter#nearestSite}, reused verbatim. */
    public static stairwaytoheaven.worldgen.SkyTerrainPainter.Site nearestSite(
            int seed, int tileX, int tileY, int cell, int salt, float chance) {
        return stairwaytoheaven.worldgen.SkyTerrainPainter.nearestSite(seed, tileX, tileY, cell, salt, chance);
    }

    // ---- what grows where -------------------------------------------------

    /**
     * One tile's object, or 0.
     *
     * <p>The rolls are cumulative bands on a single per-tile hash, the same
     * idiom {@code VeilTerrainPainter.rollObject} uses, so the shares below add
     * up to a density that can be read off the source instead of measured by
     * running the world.
     */
    public static int objectAt(int seed, int tileX, int tileY, int biome,
            float island, float waterline) {
        if (biome == BIOME_SHALLOWS) {
            return shoreObject(seed, tileX, tileY, island, waterline);
        }
        // A Knowledge Tree stands where its lattice put it, before anything
        // else gets a say: it is the realm's landmark, and a landmark that
        // loses a coin flip to a fern is not a landmark.
        stairwaytoheaven.worldgen.SkyTerrainPainter.Site knowledge = nearestSite(
                seed, tileX, tileY, KNOWLEDGE_CELL, SALT_KNOWLEDGE, KNOWLEDGE_CHANCE);
        if (knowledge.exists() && knowledge.tileX == tileX && knowledge.tileY == tileY
                && island > waterline + 0.10F) {
            return EdenRealm.knowledgeTreeID;
        }
        return biome == BIOME_CANOPY
                ? canopyObject(seed, tileX, tileY)
                : gardenObject(seed, tileX, tileY);
    }

    /**
     * The garden floor: ~0.42 objects per tile, and every one of them green.
     *
     * <p>Trees are the top 3.6% because A3.3 asks for trees that read <i>"much
     * larger than vanilla Necesse trees"</i> and a forest at 15% is a wall, not
     * a garden — the clearings are what make the fruit visible.
     */
    public static int gardenObject(int seed, int tileX, int tileY) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_OBJECT_ROLL);
        if (roll < 0.140F) return EdenRealm.serpentGrassID;      // 14.0% carpet
        if (roll < 0.240F) return EdenRealm.paradiseFernID;      // 10.0%
        if (roll < 0.290F) return EdenRealm.floweringVineID;     //  5.0%
        if (roll < 0.325F) return EdenRealm.redParadiseFlowerID; //  3.5%
        if (roll < 0.355F) return EdenRealm.blueParadiseFlowerID;//  3.0%
        if (roll < 0.375F) return EdenRealm.goldenOrchidID;      //  2.0%
        if (roll < 0.390F) return EdenRealm.giantMonsteraID;     //  1.5%
        if (roll < 0.404F) return EdenRealm.giantFigTreeID;      //  1.4%
        if (roll < 0.412F) return EdenRealm.paradisePalmID;      //  0.8%
        if (roll < 0.416F) return EdenRealm.treeOfPlentyID;      //  0.4%
        if (roll < 0.420F) return EdenRealm.edenBerryBushID;     //  0.4%
        if (roll < 0.4235F) return EdenRealm.sunGrapeBushID;     //  0.35%
        if (roll < 0.4265F) return EdenRealm.moonMelonBushID;    //  0.30%
        if (roll < 0.4290F) return EdenRealm.edenRockID;         //  0.25%
        if (roll < 0.4325F) return EdenRealm.edenCopperRockID;   //  0.35%
        return 0;
    }

    /**
     * Under the canopy: darker, rootier, and where the metal is.
     *
     * <p>A4.2 — <i>"rare resources stay rare enough to be a reason to
     * travel"</i>. Eden Copper is three times as common here as in the garden,
     * which is what makes the canopy worth walking into rather than around.
     */
    public static int canopyObject(int seed, int tileX, int tileY) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_CANOPY_ROLL);
        if (roll < 0.090F) return EdenRealm.adamsVineID;         //  9.0%
        if (roll < 0.150F) return EdenRealm.paradiseFernID;      //  6.0%
        if (roll < 0.185F) return EdenRealm.giantMonsteraID;     //  3.5%
        if (roll < 0.215F) return EdenRealm.giantFigTreeID;      //  3.0%
        if (roll < 0.230F) return EdenRealm.floweringVineID;     //  1.5%
        if (roll < 0.240F) return EdenRealm.goldenOrchidID;      //  1.0%
        if (roll < 0.250F) return EdenRealm.edenRockID;          //  1.0%
        if (roll < 0.261F) return EdenRealm.edenCopperRockID;    //  1.1%
        if (roll < 0.264F) return EdenRealm.edenCacheID;         //  0.3%
        return 0;
    }

    /**
     * The lagoon rim. Sparser on purpose: A4.1's calm ground has to exist
     * somewhere a player can actually stand and look at it, and the white sand
     * is the only place in Eden that is not shoulder-high in leaves.
     */
    public static int shoreObject(int seed, int tileX, int tileY, float island, float waterline) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_SHORE_ROLL);
        boolean atTheWater = island <= waterline + 0.030F;
        if (atTheWater) {
            if (roll < 0.070F) return EdenRealm.giantLotusID;
            if (roll < 0.130F) return EdenRealm.paradiseReedsID;
            if (roll < 0.150F) return EdenRealm.edenShellsID;
            return 0;
        }
        if (roll < 0.040F) return EdenRealm.paradiseReedsID;
        if (roll < 0.070F) return EdenRealm.edenShellsID;
        if (roll < 0.090F) return EdenRealm.paradisePalmID;
        if (roll < 0.100F) return EdenRealm.redParadiseFlowerID;
        return 0;
    }
}
