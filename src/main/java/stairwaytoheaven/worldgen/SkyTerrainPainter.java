package stairwaytoheaven.worldgen;

import necesse.engine.registries.ObjectLayerRegistry;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyCloudmarbleSet;
import stairwaytoheaven.SkyRegistry;

import java.awt.Point;

/**
 * Paints one region of the Skyreach: floating islands over the Mistsea, with the
 * four sky sub-biomes written into the region's biome layer — the exact same
 * per-tile mechanism vanilla cave levels use, so spawn tables, music and crate
 * loot all resolve through {@code Level.getBiome(tileX, tileY)}.
 *
 * All shapes derive from {@link SkyNoise}, which is a pure function of the world
 * seed and tile coordinates: region borders are seamless and generation is fully
 * deterministic.
 */
public final class SkyTerrainPainter {

    // Noise field salts (independent noise layers)
    public static final long SALT_BIOME = 0x1B903;
    public static final long SALT_ROCK_PATCH = 0x2C511;
    public static final int SALT_OBJECT_ROLL = 7;

    // Island shape
    // v0.3.1: bigger, more frequent landmasses after playtests (the sky read
    // as mostly Mistsea and travel was all swimming)
    public static final float ISLAND_SCALE = 54.0F;
    public static final float ISLAND_THRESHOLD = 0.48F;
    /** Rim band above the threshold that is kept clear of objects, so island edges stay walkable. */
    public static final float ISLAND_RIM = 0.020F;

    // Sub-biome distribution (low-frequency mask)
    //
    // Four exclusive bands of ONE noise field, in ascending order:
    //
    //   b <  STORMVEIL_BELOW    Stormveil      dark slate under thunderheads
    //   b <  SKYWAY_BELOW       Skyway         the built passages
    //   b <= AURORA_ABOVE       Driftlands     the common silver-green isles
    //   b >  AURORA_ABOVE       Aurora Shoals  the rare cold-dawn shallows
    //
    // The Skyway band was cut out of the Driftlands' lower edge rather than
    // added at one end, so it BORDERS Stormveil: the Sky Seraph shows its
    // frost form on stormslate and on skyway (SkyTreeObject), and that only
    // reads as one cold region of the sky if the two grounds actually touch.
    // Measured over three seeds and 810,855 land samples, the biome field
    // spends 13.3% of its land in 0.40-0.47, which puts the passages at
    // Aurora's share (13.5%) and leaves the Driftlands the clear majority.
    //
    // What that produced, measured over EIGHT seeds and 2,197,075 natural
    // land tiles (objects per tile, built layer excluded):
    //
    //   Skyway       / skyway        237749 land   0.371
    //   Driftlands   / cloudturf     884154 land   0.358
    //   Stormveil    / stormslate    307474 land   0.307
    //   AuroraShoals / cloudturf     221213 land   0.322
    //   Skyway       / skystone       44392 land   0.389   (its barrens)
    //   Driftlands   / skystone      168470 land   0.328
    //   Stormveil    / skystone       56796 land   0.359
    //   AuroraShoals / skystone       38099 land   0.330
    //
    //   land share:  Driftlands 53.7%  Stormveil 18.6%
    //                Skyway     14.6%  AuroraShoals 13.1%
    //
    // The passages sit at the TOP of the band the other grounds occupy, not
    // below it. That was not free: the first cut measured 0.297 and tied the
    // emptiest ground in the world, because a paved biome has no obvious wild
    // growth to fill it with — see rollObject for what it was filled with in
    // the end, and why the scatter band is wider than the Driftlands'.
    public static final float BIOME_SCALE = 170.0F;
    public static final float STORMVEIL_BELOW = 0.40F;
    public static final float SKYWAY_BELOW = 0.47F;
    public static final float AURORA_ABOVE = 0.72F;

    // Skystone outcrop patches on island interiors
    public static final float ROCK_PATCH_SCALE = 22.0F;
    public static final float ROCK_PATCH_THRESHOLD = 0.67F;

    // ------------------------------------------------------------------
    // v0.5.1 rock geology.
    //
    // Rocks used to come out of the same per-tile probability roll as the
    // plants, which places them independently: statistically even, visually a
    // graveyard of single blocks on a grid (playtest 2026-08-24). They are now
    // placed by a *formation field* instead.
    //
    // The world is cut into OUTCROP_CELL lattice cells; a fraction of the
    // cells carry one outcrop SITE at a hashed position inside the cell. A
    // site is an ellipse (hashed radius, elongation and rotation, so
    // formations come out as compact knots, ridges and short veins rather
    // than discs) plus, most of the time, a smaller second lobe offset in a
    // hashed direction — that is what produces L-shaped and asymmetric
    // outcrops. A low-frequency noise field then wobbles the boundary so no
    // edge is ever elliptical. Everything is derived from
    // hash(seed, cellX, cellY) and per-tile noise, so it stays a pure
    // function of seed and tile coordinates: region borders are seamless and
    // a formation straddling two regions generates identically from either
    // side.
    //
    // Empty cells are the point: they are what produces the large gaps
    // between formations that the outcrop needs in order to read as a find.
    // ------------------------------------------------------------------

    /** Lattice cell that holds at most one outcrop site. Larger = larger gaps. */
    public static final int OUTCROP_CELL = 16;
    /** Fraction of lattice cells that carry a formation at all. */
    public static final float OUTCROP_CHANCE = 0.46F;
    /** Boundary wobble, in units of the formation's own radius. */
    public static final float OUTCROP_EDGE_AMOUNT = 0.86F;
    public static final float OUTCROP_EDGE_SCALE = 6.0F;
    /** How far outside the solid formation loose scree can still appear. */
    public static final float OUTCROP_APRON = 0.62F;
    /** Chance an apron tile carries a loose stone (debris around the outcrop). */
    public static final float SCREE_CHANCE = 0.22F;
    /** Chance a tile far from every formation carries a lone stone. */
    public static final float SOLITARY_CHANCE = 0.0030F;
    /** Ore share of a formation's tiles at distance band 0, +ORE_BAND_STEP per band. */
    public static final float ORE_SHARE_CORE = 0.30F;
    public static final float ORE_BAND_STEP = 0.10F;
    /** Scale of the noise that clumps ore into veins inside a formation. */
    public static final float ORE_VEIN_SCALE = 9.0F;

    public static final long SALT_OUTCROP = 0x7C0C1L;
    public static final long SALT_OUTCROP_EDGE = 0x7C0E9L;
    public static final long SALT_OUTCROP_VEIN = 0x7C0EFL;
    public static final int SALT_ORE_ROLL = 29;
    public static final int SALT_SCREE = 31;
    public static final int SALT_SOLITARY = 37;

    // Packed bits of outcropAt()
    private static final int OUTCROP_SOLID = 1;
    private static final int OUTCROP_APRON_BIT = 2;
    private static final int OUTCROP_RICH_SHIFT = 2;
    private static final int OUTCROP_MINERAL_SHIFT = 10;

    // ------------------------------------------------------------------
    // v0.5.1 aurora colonies.
    //
    // Same lattice trick at a much smaller scale: aurora flora grows in
    // colonies of roughly 1-5 with an occasional richer patch, instead of
    // independent per-tile rolls that spread it evenly over the whole biome
    // (playtest 2026-08-24: "colonies look mirrored and procedural").
    // ------------------------------------------------------------------

    public static final int AURORA_COLONY_CELL = 7;
    public static final float AURORA_COLONY_CHANCE = 0.72F;
    /** Fraction of a colony's footprint that actually grows a plant. */
    public static final float AURORA_COLONY_FILL = 0.62F;
    /** Share of a colony's plants that are blooms rather than lilies. */
    public static final float AURORA_BLOOM_SHARE = 0.70F;
    public static final float AURORA_EDGE_AMOUNT = 0.70F;
    public static final float AURORA_EDGE_SCALE = 4.0F;

    public static final long SALT_AURORA_COLONY = 0xA0C01L;
    public static final long SALT_AURORA_EDGE = 0xA0C2FL;
    public static final int SALT_AURORA_FILL = 41;
    public static final int SALT_AURORA_PICK = 43;

    private static final float TAU = 6.2831855F;

    // ------------------------------------------------------------------
    // v0.7 stone barrens.
    //
    // isRockPatch turns any biome's ground grey and then rollObject answers
    // `isRockPatch ? 0 : plant` for almost every plant, meadow carpets are
    // gated on !isRockPatch and aurora colonies are too. Measured over three
    // seeds and 235,528 natural land tiles that left:
    //
    //   Driftlands / cloudturf     157863 land   0.384 objects/tile
    //   AuroraShoals / cloudturf    14481 land   0.347
    //   Stormveil / stormslate      28586 land   0.311
    //   Stormveil / skystone         4871 land   0.099
    //   AuroraShoals / skystone      3247 land   0.044   (rock only)
    //   Driftlands / skystone       26480 land   0.032   (rock only)
    //
    // 14.7% of all land, four to twelve times emptier than anything else, and
    // nothing on it but stone blocks — "die Welt mit grauen Boeden viel leerer
    // und hat eigentlich nur paar einzelne Steinbloecke".
    //
    // The fix is the same one the rock pass used: a FORMATION field, not a
    // per-tile probability. Barren growth belongs to lichen beds, which are
    // exactly what colonises bare plate in the real world, and the density is
    // calibrated on 40x22 SCREEN renders of barren ground, never on an
    // overview (docs/TECHNICAL_LEARNINGS.md).
    public static final int SCREE_CELL = 9;
    /** Chance a lattice cell carries a bed at all — gaps are the point. */
    public static final float SCREE_BED_CHANCE = 0.80F;
    /** Fraction of a bed's footprint that actually carries something. */
    public static final float SCREE_FILL = 0.72F;
    public static final float SCREE_EDGE_AMOUNT = 0.75F;
    public static final float SCREE_EDGE_SCALE = 5.0F;
    /**
     * Shares within a bed: lichen, cragbloom, scree, boulder, then accent.
     *
     * The boulder share exists because the first screen-scale render of the
     * new barrens was uniformly FLAT — lichen, bloom and scree are all low
     * 32px objects, so 40x22 tiles of them read as one field of pebbles. The
     * meadow gets its relief from trees; nothing tall grows on bare plate, so
     * the barrens get theirs from boulders standing in the scree.
     */
    public static final float SCREE_LICHEN_SHARE = 0.50F;
    public static final float SCREE_BLOOM_SHARE = 0.68F;
    public static final float SCREE_RUBBLE_SHARE = 0.86F;
    public static final float SCREE_BOULDER_SHARE = 0.93F;
    /**
     * Sparse growth OUTSIDE the beds, so the open plate between them is never
     * dead flat. Tuned so the Driftlands barrens land inside the 0.31-0.38
     * objects/tile band the three vegetated grounds occupy: at 0.11 they
     * measured 0.280, still the emptiest ground in the world.
     */
    public static final float SCREE_STRAY_CHANCE = 0.150F;

    public static final long SALT_SCREE_BED = 0x5C4EE1L;
    public static final long SALT_SCREE_EDGE = 0x5C4EE2L;
    public static final int SALT_SCREE_FILL = 71;
    public static final int SALT_SCREE_PICK = 73;
    public static final int SALT_SCREE_STRAY = 79;

    // v0.4 meadow carpets: low-frequency patches where walk-through tall
    // grass covers most of the ground (vanilla lush-area density), layered
    // over the sparse per-tile rolls that fill the rest of the island.
    public static final long SALT_MEADOW = 0x4D0E;
    public static final int SALT_MEADOW_ROLL = 11;
    public static final float MEADOW_SCALE = 48.0F;
    public static final float MEADOW_THRESHOLD = 0.60F;
    public static final float MEADOW_DENSITY = 0.72F;

    /**
     * Radius around the canonical spire origin kept as an open plaza: natural
     * object placement (meadows, trees, ores) is suppressed here so the hero
     * landmark's silhouette reads clean on first arrival. Slightly larger than
     * the 15x15 preset footprint plus its props.
     */
    public static final float SPIRE_GROUNDS_RADIUS = 18.0F;

    /**
     * v0.7 built landscape: the Skywatch roads, the designed places they
     * connect and the gates between them all come from
     * {@link SkyLandscape}, which decides them as registry-free SURFACE/PROP
     * codes. This is the one place those codes become real tiles and objects,
     * so the field stays renderable offline (scripts/sky_map_render.sh) and the
     * material choices stay in one readable table.
     */
    public static final int SALT_BUILT_PICK = 67;

    /** Salvage-crate placement. Rare on purpose: a crate you meet every
     *  screen stops being a find. */
    public static final int SALT_CRATE = 131;
    /** Roughly one crate per 900 land tiles, and three times that on the
     *  bare stone, which is where wreckage would actually come to rest and
     *  which the player reported as the emptiest ground in the sky. */
    public static final float CRATE_CHANCE = 0.0011F;
    public static final float CRATE_CHANCE_BARREN = 0.0033F;

    /**
     * Sub-biome classes as {@link #describeTile} reports them.
     *
     * These are CLASSES, not registry IDs, and they are what the offline map
     * renderer and the /skyreachstatus oracle read. Appending a new one is
     * safe; renumbering an existing one silently rewrites every dump ever
     * taken, so new classes go on the end.
     */
    public static final int BIOME_STORMVEIL = 0;
    public static final int BIOME_DRIFTLANDS = 1;
    public static final int BIOME_AURORA = 2;
    public static final int BIOME_SKYWAY = 3;

    /** Sub-biome class -> the registered biome the region layer stores. */
    public static int biomeRegistryID(int biomeClass) {
        switch (biomeClass) {
            case BIOME_STORMVEIL:
                return SkyRegistry.stormveil.getID();
            case BIOME_AURORA:
                return SkyRegistry.auroraShoals.getID();
            case BIOME_SKYWAY:
                return SkyRegistry.skyway.getID();
            default:
                return SkyRegistry.driftlands.getID();
        }
    }

    /** The sub-biome class of a raw biome-noise value. One place, one rule. */
    public static int biomeClassOf(float biomeValue) {
        if (biomeValue < STORMVEIL_BELOW) {
            return BIOME_STORMVEIL;
        }
        if (biomeValue < SKYWAY_BELOW) {
            return BIOME_SKYWAY;
        }
        return biomeValue > AURORA_ABOVE ? BIOME_AURORA : BIOME_DRIFTLANDS;
    }


    /**
     * The formation field. Returns 0 for a tile that belongs to no outcrop, or
     * a packed int: bit 0 solid, bit 1 apron, bits 2-9 the formation's ore
     * share as a 0-255 fraction, bits 10+ a per-formation mineral pick.
     *
     * Only the 3x3 lattice cells around the tile are examined, so cost is
     * constant per tile and a formation whose site sits in a neighbouring
     * region generates identically from either side of the border.
     */
    public static int outcropAt(int seed, int tileX, int tileY) {
        int cellX = Math.floorDiv(tileX, OUTCROP_CELL);
        int cellY = Math.floorDiv(tileY, OUTCROP_CELL);
        float best = Float.MAX_VALUE;
        float bestRich = 0.0F;
        int bestMineral = 0;

        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                int cx = cellX + ox;
                int cy = cellY + oy;
                if (SkyNoise.hash(seed + SALT_OUTCROP, cx, cy) >= OUTCROP_CHANCE) {
                    continue;
                }
                // Site position, shape and orientation, all hashed off the
                // same lattice point so the whole formation is decided by one
                // cell and never disagrees between neighbouring tiles.
                float siteX = cx * OUTCROP_CELL + SkyNoise.hash(seed + SALT_OUTCROP + 1, cx, cy) * OUTCROP_CELL;
                float siteY = cy * OUTCROP_CELL + SkyNoise.hash(seed + SALT_OUTCROP + 2, cx, cy) * OUTCROP_CELL;
                float radius = 0.9F + SkyNoise.hash(seed + SALT_OUTCROP + 3, cx, cy) * 1.0F;
                boolean isLarge = SkyNoise.hash(seed + SALT_OUTCROP + 4, cx, cy) < 0.13F;
                if (isLarge) {
                    radius *= 2.1F;
                }
                float elongation = 1.0F + SkyNoise.hash(seed + SALT_OUTCROP + 5, cx, cy) * 1.4F;
                float angle = SkyNoise.hash(seed + SALT_OUTCROP + 6, cx, cy) * TAU;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);

                float rich = 0.35F + SkyNoise.hash(seed + SALT_OUTCROP + 7, cx, cy) * 0.5F;
                if (isLarge) {
                    rich = Math.min(1.0F, rich + 0.25F);   // jackpot formations
                }
                int mineral = (int) (SkyNoise.hash(seed + SALT_OUTCROP + 8, cx, cy) * 3.0F);

                float d = lobeDistance(tileX, tileY, siteX, siteY, radius, elongation, cos, sin);

                // Most formations carry a smaller second lobe offset in a
                // hashed direction. That is what turns discs into L-shapes,
                // ridges and short veins.
                if (SkyNoise.hash(seed + SALT_OUTCROP + 9, cx, cy) < 0.72F) {
                    float lobeAngle = SkyNoise.hash(seed + SALT_OUTCROP + 10, cx, cy) * TAU;
                    float reach = radius * (0.8F + SkyNoise.hash(seed + SALT_OUTCROP + 11, cx, cy) * 0.9F);
                    float lobeX = siteX + (float) Math.cos(lobeAngle) * reach;
                    float lobeY = siteY + (float) Math.sin(lobeAngle) * reach;
                    float lobeR = radius * (0.45F + SkyNoise.hash(seed + SALT_OUTCROP + 12, cx, cy) * 0.35F);
                    d = Math.min(d, lobeDistance(tileX, tileY, lobeX, lobeY, lobeR, elongation, cos, sin));
                }

                if (d < best) {
                    best = d;
                    bestRich = rich;
                    bestMineral = mineral;
                }
            }
        }
        if (best == Float.MAX_VALUE) {
            return 0;
        }
        // Wobble the boundary so no formation edge is ever a clean ellipse.
        float wobble = SkyNoise.fbm(seed + SALT_OUTCROP_EDGE, tileX, tileY, OUTCROP_EDGE_SCALE, 2) - 0.5F;
        float edge = best - wobble * OUTCROP_EDGE_AMOUNT;
        if (edge > 1.0F + OUTCROP_APRON) {
            return 0;
        }
        int flags = edge <= 1.0F ? OUTCROP_SOLID : OUTCROP_APRON_BIT;
        int richBits = Math.max(0, Math.min(255, (int) (bestRich * 255.0F)));
        return flags | (richBits << OUTCROP_RICH_SHIFT) | (bestMineral << OUTCROP_MINERAL_SHIFT);
    }

    /** Normalized distance into an elongated, rotated lobe (1.0 = its edge). */
    private static float lobeDistance(int tileX, int tileY, float siteX, float siteY,
                                      float radius, float elongation, float cos, float sin) {
        float dx = tileX - siteX;
        float dy = tileY - siteY;
        float u = (dx * cos + dy * sin) / (radius * elongation);
        float v = (-dx * sin + dy * cos) / radius;
        return (float) Math.sqrt(u * u + v * v);
    }

    /**
     * Picks the object for a tile that belongs to an outcrop, or 0 for none.
     * Ore lives INSIDE formations, clumped by its own vein noise, and the ore
     * share widens with the distance band so the outer reaches pay better.
     */
    public static int rollOutcropObject(int seed, int tileX, int tileY, int outcrop,
                                        int biomeClass, int band) {
        boolean solid = (outcrop & OUTCROP_SOLID) != 0;
        if (!solid) {
            // Apron: loose scree scattered around the formation, which is what
            // makes an outcrop look eroded rather than stamped.
            if (SkyNoise.tileRoll(seed, tileX, tileY, SALT_SCREE) >= SCREE_CHANCE) {
                return 0;
            }
            return SkyRegistry.skystoneRockID;
        }
        float rich = ((outcrop >> OUTCROP_RICH_SHIFT) & 0xFF) / 255.0F;
        float oreShare = Math.min(0.85F, (ORE_SHARE_CORE + ORE_BAND_STEP * band) * rich);
        float vein = SkyNoise.fbm(seed + SALT_OUTCROP_VEIN, tileX, tileY, ORE_VEIN_SCALE, 2);
        if (vein > 1.0F - oreShare && SkyNoise.tileRoll(seed, tileX, tileY, SALT_ORE_ROLL) < 0.85F) {
            int mineral = (outcrop >> OUTCROP_MINERAL_SHIFT) & 0x3;
            if (biomeClass == BIOME_STORMVEIL) {
                return mineral == 0 ? SkyRegistry.fulguriteRockID : SkyRegistry.aetheriumRockID;
            }
            if (biomeClass == BIOME_AURORA) {
                return mineral == 0 ? SkyRegistry.prismshardRockID : SkyRegistry.aetheriumRockID;
            }
            if (biomeClass == BIOME_SKYWAY) {
                // The passages were quarried out of their own bedrock: the
                // fulgurite the Stormveil next door carries surfaces here too.
                return mineral == 0 ? SkyRegistry.fulguriteRockID : SkyRegistry.aetheriumRockID;
            }
            return SkyRegistry.aetheriumRockID;
        }
        return SkyRegistry.skystoneRockID;
    }

    /**
     * SURFACE code -> ground tile. Two materials build everything: one paving
     * stone for the whole road system and its courts, and the Marble Checker as
     * a small high-contrast accent for inlay bands and monument plinths. A
     * garden keeps the terrain it grew on and only loses its wild growth.
     */
    public static int builtTile(int surface, int naturalTileID) {
        if (surface == SkyLandscape.SURFACE_ROAD
                || surface == SkyLandscape.SURFACE_APRON
                || surface == SkyLandscape.SURFACE_COURT) {
            return SkyRegistry.skyroadTileID != 0 ? SkyRegistry.skyroadTileID : naturalTileID;
        }
        if (surface == SkyLandscape.SURFACE_INLAY || surface == SkyLandscape.SURFACE_PLINTH) {
            return SkyRegistry.skyplinthTileID != 0 ? SkyRegistry.skyplinthTileID : naturalTileID;
        }
        return naturalTileID;                    // SURFACE_GARDEN keeps its soil
    }

    /**
     * PROP code -> object, chosen per sub-biome so a Stormveil waystation is
     * built out of Stormveil material and planted with Stormveil growth. This
     * is the whole biome-specific vocabulary of the built layer in one table.
     *
     * <h2>Two kinds of code</h2>
     *
     * Most codes are MATERIAL-NEUTRAL: a fence is whatever the local
     * civilisation builds fences out of, so it resolves against the tile's own
     * sub-biome. The Skyway Passages entries below are what make a waystation
     * standing in the passages come out in Cloudmarble with a Seraph on its
     * plinth, instead of the Skywatch's iron and its Gloom Raven.
     *
     * The PASSAGE codes (RAIL, RAIL_GATE, BUTTRESS, MONUMENT) are the
     * exception, and always Cloudmarble. A Skyway causeway that runs on past
     * the biome border is still a Skyway causeway: it keeps its own balustrade
     * rather than changing material halfway along its length, which is exactly
     * what a per-tile lookup would do to it.
     */
    public static int builtObject(int prop, int seed, int tileX, int tileY, int biomeClass) {
        boolean isStormveil = biomeClass == BIOME_STORMVEIL;
        boolean isAurora = biomeClass == BIOME_AURORA;
        boolean isSkyway = biomeClass == BIOME_SKYWAY;
        switch (prop) {
            case SkyLandscape.PROP_LAMP:
                return SkyRegistry.wardenCandelabraID;
            case SkyLandscape.PROP_FENCE:
                return isSkyway ? SkyCloudmarbleSet.cloudmarbleFenceID : SkyRegistry.skyironFenceID;
            case SkyLandscape.PROP_GATE:
                return isSkyway ? SkyCloudmarbleSet.cloudmarbleFenceGateID : SkyRegistry.skyironFenceGateID;
            case SkyLandscape.PROP_PILLAR:
                if (isSkyway) {
                    return SkyCloudmarbleSet.cloudmarbleWallID;
                }
                return isStormveil ? SkyRegistry.nightfellWallID : SkyRegistry.skystoneBrickWallID;
            case SkyLandscape.PROP_STATUE:
                return isSkyway ? SkyCloudmarbleSet.seraphStatueID : SkyRegistry.gloomRavenStatueID;
            case SkyLandscape.PROP_TREE:
                if (isSkyway) {
                    // Alternate along a planted run so an avenue reads as two
                    // species rather than one repeated silhouette. Position
                    // parity, not a roll, so it stays stable and mirrored.
                    return ((tileX + tileY) & 1) == 0
                            ? SkyRegistry.skySeraphTreeID
                            : SkyRegistry.cloudTreeID;
                }
                return isStormveil ? SkyRegistry.fulgurpineID
                        : (isAurora ? SkyRegistry.prismabirchID : SkyRegistry.nimbuswillowID);
            case SkyLandscape.PROP_FLOWER: {
                float pick = SkyNoise.tileRoll(seed, tileX, tileY, SALT_BUILT_PICK);
                if (isStormveil) {
                    return pick < 0.62F ? SkyRegistry.thunderbloomID : SkyRegistry.staticmossID;
                }
                if (isAurora) {
                    return pick < 0.62F ? SkyRegistry.auroralilyID : SkyRegistry.glowfernID;
                }
                if (isSkyway) {
                    return pick < 0.58F ? SkyRegistry.cloudbellID : SkyRegistry.skylichenID;
                }
                return pick < 0.55F ? SkyRegistry.skytulipID : SkyRegistry.cloudbellID;
            }
            case SkyLandscape.PROP_GRASS:
                return isStormveil ? SkyRegistry.stormsedgeID
                        : (isAurora ? SkyRegistry.prismgrassID : SkyRegistry.tallcloudgrassID);
            case SkyLandscape.PROP_ACCENT:
                return biomeAccent(biomeClass);
            case SkyLandscape.PROP_RUBBLE:
                return SkyRegistry.skywatchRubbleID;
            case SkyLandscape.PROP_INSTRUMENT:
                return SkyNoise.tileRoll(seed, tileX, tileY, SALT_BUILT_PICK) < 0.5F
                        ? SkyRegistry.skywatchTelescopeID : SkyRegistry.skywatchAstrolabeID;

            // --- the passage vocabulary: always Cloudmarble ---
            case SkyLandscape.PROP_RAIL:
                return SkyCloudmarbleSet.cloudmarbleFenceID;
            case SkyLandscape.PROP_RAIL_GATE:
                return SkyCloudmarbleSet.cloudmarbleFenceGateID;
            case SkyLandscape.PROP_BUTTRESS:
                return SkyCloudmarbleSet.cloudmarbleWallID;
            case SkyLandscape.PROP_MONUMENT:
                return SkyCloudmarbleSet.seraphStatueID;
            default:
                return 0;                       // PROP_NONE / PROP_CLEAR
        }
    }

    /**
     * Aurora flora colonies: the same lattice trick at a much smaller scale,
     * so blooms and lilies grow in patches of roughly one to five with real
     * gaps between them instead of an even per-tile sprinkle.
     */
    public static int auroraColonyObject(int seed, int tileX, int tileY) {
        int cellX = Math.floorDiv(tileX, AURORA_COLONY_CELL);
        int cellY = Math.floorDiv(tileY, AURORA_COLONY_CELL);
        float best = Float.MAX_VALUE;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                int cx = cellX + ox;
                int cy = cellY + oy;
                if (SkyNoise.hash(seed + SALT_AURORA_COLONY, cx, cy) >= AURORA_COLONY_CHANCE) {
                    continue;
                }
                float siteX = cx * AURORA_COLONY_CELL
                        + SkyNoise.hash(seed + SALT_AURORA_COLONY + 1, cx, cy) * AURORA_COLONY_CELL;
                float siteY = cy * AURORA_COLONY_CELL
                        + SkyNoise.hash(seed + SALT_AURORA_COLONY + 2, cx, cy) * AURORA_COLONY_CELL;
                float radius = 1.0F + SkyNoise.hash(seed + SALT_AURORA_COLONY + 3, cx, cy) * 2.4F;
                float dx = tileX - siteX;
                float dy = tileY - siteY;
                best = Math.min(best, (float) Math.sqrt(dx * dx + dy * dy) / radius);
            }
        }
        if (best == Float.MAX_VALUE) {
            return 0;
        }
        float wobble = SkyNoise.fbm(seed + SALT_AURORA_EDGE, tileX, tileY, AURORA_EDGE_SCALE, 2) - 0.5F;
        if (best - wobble * AURORA_EDGE_AMOUNT > 1.0F) {
            return 0;
        }
        if (SkyNoise.tileRoll(seed, tileX, tileY, SALT_AURORA_FILL) >= AURORA_COLONY_FILL) {
            return 0;
        }
        return SkyNoise.tileRoll(seed, tileX, tileY, SALT_AURORA_PICK) < AURORA_BLOOM_SHARE
                ? SkyRegistry.auroraBloomID
                : SkyRegistry.auroralilyID;
    }

    /**
     * What grows on the grey skystone ground at this tile, or 0.
     *
     * Beds of lichen on a lattice, exactly like {@link #auroraColonyObject}:
     * one hashed site per cell, a wobbled boundary, a fill roll inside. The
     * pick inside a bed is lichen first (the ground cover), then cragbloom
     * (the warm accent), then scree heaps, then one lit biome accent, so a
     * barren reads as bed → flower → rubble → a point of light, the same
     * vegetation → outcrop → reward rhythm the meadow already has.
     *
     * Outside every bed a small stray roll keeps the open plate from reading
     * as a paved floor.
     */
    public static int screeObject(int seed, int tileX, int tileY, int biomeClass) {
        int cellX = Math.floorDiv(tileX, SCREE_CELL);
        int cellY = Math.floorDiv(tileY, SCREE_CELL);
        float best = Float.MAX_VALUE;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                int cx = cellX + ox;
                int cy = cellY + oy;
                if (SkyNoise.hash(seed + SALT_SCREE_BED, cx, cy) >= SCREE_BED_CHANCE) {
                    continue;
                }
                float siteX = cx * SCREE_CELL
                        + SkyNoise.hash(seed + SALT_SCREE_BED + 1, cx, cy) * SCREE_CELL;
                float siteY = cy * SCREE_CELL
                        + SkyNoise.hash(seed + SALT_SCREE_BED + 2, cx, cy) * SCREE_CELL;
                float radius = 1.6F + SkyNoise.hash(seed + SALT_SCREE_BED + 3, cx, cy) * 3.2F;
                float dx = tileX - siteX;
                float dy = tileY - siteY;
                best = Math.min(best, (float) Math.sqrt(dx * dx + dy * dy) / radius);
            }
        }
        boolean inBed = false;
        if (best != Float.MAX_VALUE) {
            float wobble = SkyNoise.fbm(seed + SALT_SCREE_EDGE, tileX, tileY, SCREE_EDGE_SCALE, 2) - 0.5F;
            inBed = best - wobble * SCREE_EDGE_AMOUNT <= 1.0F;
        }
        if (inBed) {
            if (SkyNoise.tileRoll(seed, tileX, tileY, SALT_SCREE_FILL) >= SCREE_FILL) {
                return 0;
            }
        } else if (SkyNoise.tileRoll(seed, tileX, tileY, SALT_SCREE_STRAY) >= SCREE_STRAY_CHANCE) {
            return 0;
        }
        float pick = SkyNoise.tileRoll(seed, tileX, tileY, SALT_SCREE_PICK);
        if (pick < SCREE_LICHEN_SHARE) {
            return SkyRegistry.skylichenID;
        }
        if (pick < SCREE_BLOOM_SHARE) {
            return SkyRegistry.cragbloomID;
        }
        if (pick < SCREE_RUBBLE_SHARE) {
            return SkyRegistry.skyscreeID;
        }
        if (pick < SCREE_BOULDER_SHARE) {
            // A boulder standing in the scree: the barrens' vertical relief,
            // and a mining reward inside the bed rather than out on its own.
            return SkyRegistry.skystoneRockID;
        }
        // One lit accent per bed or so: the barrens get the same "there is
        // something over there" hook the other grounds get from their flowers.
        return biomeAccent(biomeClass);
    }

    /**
     * The one lit accent of a sub-biome. Shared by the barrens beds and the
     * built landscape's PROP_ACCENT so a biome's point of light is the same
     * object wherever it is placed.
     */
    private static int biomeAccent(int biomeClass) {
        if (biomeClass == BIOME_STORMVEIL) {
            return SkyRegistry.chargeCrystalID;
        }
        if (biomeClass == BIOME_AURORA) {
            return SkyRegistry.auroraShardsID;
        }
        // Skyway and Driftlands both take the Starfall: gold on pale ground.
        return SkyRegistry.starfallID;
    }

    private SkyTerrainPainter() {
    }

    // ------------------------------------------------------------------
    // The per-tile decision, and the region painter that writes it out.
    //
    // describeTile() is the SINGLE source of truth for what a Skyreach tile
    // becomes. paintRegion() only writes its answer into the region, and the
    // offline map renderer (scripts/sky_map_render.sh) and the /skyreachstatus
    // oracle call the very same function — so a calibration render and the
    // world the player walks on can never drift apart.
    // ------------------------------------------------------------------

    /** Ground tile of a describeTile() result. */
    public static int descTile(long desc) {
        return (int) (desc & 0xFFFFFL);
    }

    /** Object of a describeTile() result, 0 for none. */
    public static int descObject(long desc) {
        return (int) ((desc >>> 20) & 0xFFFFFL);
    }

    /** Sub-biome CLASS of a describeTile() result (see BIOME_* above). */
    public static int descBiome(long desc) {
        return (int) ((desc >>> 40) & 0xFFFFFL);
    }

    /** True when the built landscape (road, court, garden) owns this tile. */
    public static boolean descBuilt(long desc) {
        return (desc & (1L << 60)) != 0L;
    }

    private static long pack(int tileID, int objectID, int biomeID, boolean built) {
        return (tileID & 0xFFFFFL)
                | ((objectID & 0xFFFFFL) << 20)
                | ((biomeID & 0xFFFFFL) << 40)
                | (built ? 1L << 60 : 0L);
    }

    /**
     * What one Skyreach tile becomes: ground, object and sub-biome, as a pure
     * function of the world-generation seed, the tile position and the
     * canonical origin.
     *
     * Order of precedence, highest first:
     * <ol>
     *   <li>Mistsea, wherever the island mask says there is no land.</li>
     *   <li>The BUILT landscape ({@link SkyLandscape}): roads, designed places,
     *       gates. It paves its own ground and no wild growth appears on it.</li>
     *   <li>The spire grounds, kept clear so the landmark reads on arrival.</li>
     *   <li>Island rims, kept clear so coastlines stay walkable.</li>
     *   <li>Geology: the outcrop formation field owns its tiles outright.</li>
     *   <li>Aurora colonies, meadow carpets, then the even vegetation scatter.</li>
     * </ol>
     */
    public static long describeTile(int seed, int tileX, int tileY, int originX, int originY) {
        float biomeValue = SkyNoise.fbm(seed + SALT_BIOME, tileX, tileY, BIOME_SCALE, 2);

        // The hub guarantee: around the Old Warden Spire the island mask is
        // clamped to solid land and the biome pulled into the Driftlands band,
        // so the first ascent always lands on a walkable, safe, recognizable
        // home island — regardless of what the raw noise wanted there.
        float hubDx = tileX - originX;
        float hubDy = tileY - originY;
        float hubDist = (float) Math.sqrt(hubDx * hubDx + hubDy * hubDy);
        if (hubDist < SkyOrigin.HUB_RADIUS) {
            float force = 1.0F - hubDist / SkyOrigin.HUB_RADIUS; // 1 center → 0 rim
            biomeValue = biomeValue + (0.5F - biomeValue) * Math.min(1.0F, force * 1.6F);
        }

        // A CLASS, not a registry ID: describeTile stays callable from the
        // offline map renderer, which has no biome registry.
        int biomeID = biomeClassOf(biomeValue);
        boolean isStormveil = biomeID == BIOME_STORMVEIL;
        boolean isAurora = biomeID == BIOME_AURORA;

        float islandValue = SkyNoise.fbm(seed, tileX, tileY, ISLAND_SCALE, 3);
        if (hubDist < SkyOrigin.HUB_RADIUS) {
            float force = 1.0F - hubDist / SkyOrigin.HUB_RADIUS;
            islandValue = Math.max(islandValue, ISLAND_THRESHOLD + 0.02F + 0.20F * force);
        }
        if (islandValue <= ISLAND_THRESHOLD) {
            return pack(SkyRegistry.mistseaID, 0, biomeID, false);
        }

        boolean isRockPatch = SkyNoise.fbm(seed + SALT_ROCK_PATCH, tileX, tileY, ROCK_PATCH_SCALE, 2) > ROCK_PATCH_THRESHOLD;
        int groundID;
        if (isRockPatch) {
            // Bare bedrock surfaces through every biome's ground, the passages
            // included — that is what SkywayTile's terrain priority of 260 is
            // for: where the paving meets an outcrop it stays on top of the
            // blend instead of being overgrown by the plateau.
            groundID = SkyRegistry.skystoneTileID;
        } else if (isStormveil) {
            groundID = SkyRegistry.stormslateID;
        } else if (biomeID == BIOME_SKYWAY) {
            groundID = SkyCloudmarbleSet.skywayTileID;
        } else if (isAurora) {
            // The Shoals used to fall through to cloudturf with everything
            // else, so the rarest biome wore the commonest floor. isAurora was
            // computed above and never spent.
            groundID = SkyRegistry.auroraShoalID;
        } else {
            groundID = SkyRegistry.cloudturfID;
        }

        boolean rimSafe = islandValue > ISLAND_THRESHOLD + ISLAND_RIM;

        // --- the built landscape ---
        int built = SkyLandscape.at(seed, tileX, tileY, originX, originY);
        if (built != SkyLandscape.SURFACE_NONE) {
            int builtTileID = builtTile(SkyLandscape.surfaceOf(built), groundID);
            // Nothing stands on a coastline tile: checkGenerationValid would
            // only sweep it away again (the paving itself is fine, and a path
            // tile even bridges the shore below it).
            int builtObjectID = rimSafe
                    ? builtObject(SkyLandscape.propOf(built), seed, tileX, tileY, biomeID)
                    : 0;
            return pack(builtTileID, builtObjectID, biomeID, true);
        }

        // Spire grounds: the immediate surroundings of the Old Warden Spire
        // stay an open plaza — natural objects (grass carpets, trees, ores) are
        // suppressed so nothing buries or crowds the landmark's silhouette on
        // first arrival. The preset's own props stamp afterwards.
        if (hubDist < SPIRE_GROUNDS_RADIUS || !rimSafe) {
            return pack(groundID, 0, biomeID, false);
        }

        int band = SkyOrigin.bandFor(hubDist);

        // Geology first. Rocks and ore no longer come out of the same per-tile
        // roll as the plants — they belong to formations, and a formation owns
        // its tiles outright so a plant cannot grow in the middle of an outcrop
        // and break its silhouette.
        int objectID = 0;
        int outcrop = outcropAt(seed, tileX, tileY);
        if (outcrop != 0) {
            objectID = rollOutcropObject(seed, tileX, tileY, outcrop, biomeID, band);
            if (objectID == 0 && (outcrop & OUTCROP_SOLID) != 0) {
                return pack(groundID, 0, biomeID, false);   // bare formation floor
            }
        } else if (SkyNoise.tileRoll(seed, tileX, tileY, SALT_SOLITARY) < SOLITARY_CHANCE) {
            objectID = SkyRegistry.skystoneRockID;          // the rare lone stone
        }

        // Aurora flora grows in colonies, not as an even sprinkle.
        if (objectID == 0 && isAurora && !isRockPatch) {
            objectID = auroraColonyObject(seed, tileX, tileY);
        }

        // Salvage crates come FIRST, before every growth rule, or grass would
        // win the tile and the rarest thing in the world would be the thing
        // most easily crowded out.
        if (objectID == 0 && SkyNoise.tileRoll(seed, tileX, tileY, SALT_CRATE)
                < (isRockPatch ? CRATE_CHANCE_BARREN : CRATE_CHANCE)) {
            return pack(groundID, SkyRegistry.skyCrateID, biomeID, false);
        }

        // The stone barrens have their own beds. This runs where every other
        // growth rule is switched OFF by isRockPatch, which is what made the
        // grey ground the emptiest thing in the world.
        if (objectID == 0 && isRockPatch) {
            objectID = screeObject(seed, tileX, tileY, biomeID);
            if (objectID != 0) {
                return pack(groundID, objectID, biomeID, false);
            }
        }

        // Meadow carpets: inside a meadow patch, dense walk-through tall grass
        // takes the tile before the sparse rolls run.
        if (objectID == 0
                && !isRockPatch
                && SkyNoise.fbm(seed + SALT_MEADOW, tileX, tileY, MEADOW_SCALE, 2) > MEADOW_THRESHOLD
                && SkyNoise.tileRoll(seed, tileX, tileY, SALT_MEADOW_ROLL) < MEADOW_DENSITY) {
            // Skyway keeps the Driftlands' grass on purpose: what carpets a
            // paved way is what pushes up between its flagstones, and the
            // passages are old.
            objectID = isStormveil ? SkyRegistry.stormsedgeID
                    : (isAurora ? SkyRegistry.prismgrassID : SkyRegistry.tallcloudgrassID);
            return pack(groundID, objectID, biomeID, false);
        }

        if (objectID == 0) {
            objectID = rollObject(seed, tileX, tileY, biomeID, isRockPatch, band);
        }
        return pack(groundID, objectID, biomeID, false);
    }

    public static void paintRegion(Region region, int seed) {
        int tileWidth = region.tileLayer.region.tileWidth;
        int tileHeight = region.tileLayer.region.tileHeight;
        // The canonical sky origin (Old Warden Spire hub) — computed once per
        // region; every radial rule derives from it.
        Point origin = SkyOrigin.compute(seed);
        // Tiles already claimed by the second half of a 2x1 crystal cluster
        boolean[][] reserved = new boolean[tileWidth][tileHeight];
        for (int regionTileX = 0; regionTileX < tileWidth; regionTileX++) {
            for (int regionTileY = 0; regionTileY < tileHeight; regionTileY++) {
                int tileX = regionTileX + region.tileXOffset;
                int tileY = regionTileY + region.tileYOffset;

                long desc = describeTile(seed, tileX, tileY, origin.x, origin.y);
                region.biomeLayer.setBiomeByRegion(regionTileX, regionTileY, biomeRegistryID(descBiome(desc)));
                region.tileLayer.setTileByRegion(regionTileX, regionTileY, descTile(desc));

                int objectID = descObject(desc);
                if (objectID == 0 || reserved[regionTileX][regionTileY]) {
                    continue;
                }
                if (objectID == SkyRegistry.stormCrystalID || objectID == SkyRegistry.auroraBloomID) {
                    // Crystal clusters are 2x1 multi-tile objects: base + "r"
                    // counterpart on the tile to the right. Both halves must be
                    // written, or Region.checkGenerationValid removes the torso.
                    int counterID = objectID == SkyRegistry.stormCrystalID
                            ? SkyRegistry.stormCrystalRID
                            : SkyRegistry.auroraBloomRID;
                    long right = describeTile(seed, tileX + 1, tileY, origin.x, origin.y);
                    // The right half needs open land: not Mistsea, and not a
                    // road or court, which must stay clear.
                    if (regionTileX + 1 < tileWidth
                            && descTile(right) != SkyRegistry.mistseaID
                            && !descBuilt(right)) {
                        region.objectLayer.setObjectByRegion(ObjectLayerRegistry.BASE_LAYER, regionTileX, regionTileY, objectID);
                        region.objectLayer.setObjectByRegion(ObjectLayerRegistry.BASE_LAYER, regionTileX + 1, regionTileY, counterID);
                        reserved[regionTileX + 1][regionTileY] = true;
                    }
                    continue;
                }
                region.objectLayer.setObjectByRegion(ObjectLayerRegistry.BASE_LAYER, regionTileX, regionTileY, objectID);
            }
        }
    }

    /**
     * Picks the natural VEGETATION for a land tile, or 0 for none. Chances are
     * exclusive bands of a single per-tile roll, so total density stays easy to
     * reason about.
     *
     * v0.5.1: rocks, ore and aurora flora left this function. They are placed
     * by the formation and colony fields above, because an independent per-tile
     * roll spreads them statistically evenly — which is exactly what made the
     * sky read as a graveyard of single blocks on a grid. What remains here is
     * the scatter that genuinely should be even: grasses, flowers and trees.
     */
    public static int rollObject(int seed, int tileX, int tileY, int biomeClass,
                                 boolean isRockPatch, int band) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_OBJECT_ROLL);
        if (biomeClass == BIOME_SKYWAY) {
            // Calibrated against the three grounds that already exist rather
            // than guessed. Roughly 27% of a sky ground's tiles are claimed
            // before this roll ever runs (outcrop formations, then the meadow
            // carpet), so a scatter of width w yields about 0.73w — which is
            // why this band is WIDER than the Driftlands' 0.190 and still
            // produces a comparable ground. First cut ran 0.142 and measured
            // 0.297, tying the emptiest ground in the world; see the header
            // table for where it landed in the end.
            // Two species share the passages' tree band rather than widening
            // it: the Seraph on the open verges, the Cloud Tree among the rest
            // of the cloud material. Splitting 0.022 in half keeps the measured
            // 0.371 objects/tile exactly where it was calibrated.
            if (roll < 0.011F) return isRockPatch ? 0 : SkyRegistry.skySeraphTreeID;
            if (roll < 0.022F) return isRockPatch ? 0 : SkyRegistry.cloudTreeID;
            if (roll < 0.085F) return SkyRegistry.skylichenID;      // in the joints
            if (roll < 0.130F) return isRockPatch ? 0 : SkyRegistry.cloudbellID;
            if (roll < 0.165F) return isRockPatch ? 0 : SkyRegistry.skytulipID;
            if (roll < 0.198F) return SkyRegistry.skyscreeID;       // broken paving
            if (roll < 0.215F) return SkyRegistry.starfallID;
            return 0;
        }
        if (biomeClass == BIOME_STORMVEIL) {
            if (roll < 0.035F) return SkyRegistry.stormCrystalID;
            if (roll < 0.055F) return isRockPatch ? 0 : SkyRegistry.fulgurpineID;
            if (roll < 0.095F) return SkyRegistry.staticmossID;
            if (roll < 0.115F) return isRockPatch ? 0 : SkyRegistry.thunderbloomID;
            return 0;
        }
        if (biomeClass == BIOME_AURORA) {
            if (roll < 0.020F) return isRockPatch ? 0 : SkyRegistry.prismabirchID;
            if (roll < 0.050F) return isRockPatch ? 0 : SkyRegistry.glowfernID;
            return 0;
        }
        // Driftlands: reeds prefer soft ground, then forage, then trees/flowers
        if (roll < 0.075F) return isRockPatch ? 0 : SkyRegistry.skyreedsID;
        if (roll < 0.130F) return isRockPatch ? 0 : SkyRegistry.windwheatID;
        if (roll < 0.142F) return isRockPatch ? 0 : SkyRegistry.cloudberryBushID;
        if (roll < 0.156F) return isRockPatch ? 0 : SkyRegistry.nimbuswillowID;
        if (roll < 0.176F) return isRockPatch ? 0 : SkyRegistry.cloudbellID;
        if (roll < 0.190F) return isRockPatch ? 0 : SkyRegistry.skytulipID;
        return 0;
    }
}
