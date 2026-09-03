package stairwaytoheaven.realms.ghost;

import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.SkyTerrainPainter;
import stairwaytoheaven.worldgen.VeilTerrainPainter;

/**
 * The Ghost Realm as a BAND of the one plane: broad dead landmasses over
 * glowing ectoplasm marsh, five sub-biomes, and object density that composes
 * groves and grave-fields rather than sprinkling props one tile at a time.
 *
 * <p><b>This is no longer a level's painter.</b> {@code docs/PLAN_ONE_PLANE.md}
 * retired the {@code ghost2} dimension: the Ghost Realm is the realm
 * {@link RealmDepth} gives depth 0.60-0.80, and
 * {@link SkyTerrainPainter#describeTile} calls {@link #describeBand} for every
 * tile the realm pick lands here.
 *
 * <h2>The Veil moved in ({@code WORLD_DESIGN} §41.5)</h2>
 * <blockquote>Gloomfen and Ashen Reach ... are a Ghost Realm in everything but
 * name (§10). They move from the {@code veil2} dimension into the one world at
 * the Ghost Realm's realmDepth band.</blockquote>
 * They are here, as a fourth and fifth ground cut across the other three by
 * {@link #FEN_SCALE}'s own field: murkmoss, ash sand and blackpeat under
 * whisperreeds, gloom shrooms, dead trees and ash bones, painted by
 * {@link VeilTerrainPainter}'s own shipped mix. The Veil's level is gone; not
 * one tile of its ground is.
 *
 * <h2>The three grounds</h2>
 * <ul>
 * <li><b>Aftergarden</b> — the common ground and the realm's name: haunted
 *     grass gone poison-green, with black cobble showing through where a path
 *     used to run. This is the garden after everyone left.</li>
 * <li><b>Bone Orchard</b> — pale violet dirt and spirit stone, planted in rows
 *     of dead trees. Drier, harder, and where the bonewood is.</li>
 * <li><b>Ectomarsh</b> — ghost moss and graveyard soil half under the
 *     ectoplasm, the wet end of the realm and the one that glows.</li>
 * </ul>
 *
 * <h2>Why the numbers look like the Veil's</h2>
 * They are the Veil's, deliberately. {@code VeilTerrainPainter}'s island scale
 * and threshold produce landmasses a player can walk for a while without
 * meeting water, and that shape was measured rather than guessed (see that
 * class's header for the sweep). The Aftergarden wants the same shape with a
 * different palette on it, so re-deriving the constants would only risk
 * getting a solved problem wrong.
 *
 * <p>Everything here is a pure function of {@code (seed, tileX, tileY)}, the
 * same contract as the rest of the mod's worldgen: {@link GhostWorldPreset}
 * asks these functions the same questions before a region is ever generated,
 * and has to get the same answers.
 */
public final class GhostTerrainPainter {

    // ---- Landmass ---------------------------------------------------------

    /**
     * Kept for the record: the shape this realm was measured at.
     *
     * <p>The FIELD is now the plane's own
     * ({@code SkyTerrainPainter.ISLAND_SCALE}) because one connected overworld
     * has one coastline, and 0.44 survived as this realm's WATERLINE in
     * {@code SkyTerrainPainter.REALM_WATERLINE} — so the landmasses come out
     * the same proportion of the ground they always did.
     */
    public static final int ISLAND_SCALE = 56;
    public static final float ISLAND_THRESHOLD = 0.44F;
    /** Tiles inside the shoreline that stay bare, so a coast is walkable. */
    public static final float ISLAND_RIM = 0.02F;

    // ---- The Veil's fen, §41.5 ---------------------------------------------

    /**
     * Where the Ghost Realm is the Veil instead.
     *
     * <p>A coarse field, cut across the other three grounds rather than tiled
     * beside them — the construction the Beetlefreak Hollows already used, for
     * the same reason: the fen has to be a place you arrive IN, not a fourth
     * stripe of a menu. 0.62 puts it at roughly a fifth of the band, so the
     * Aftergarden stays the realm's normal state and the fen stays the wet,
     * black, whispering exception §41.5 describes.
     *
     * <p>Inside it, {@link VeilTerrainPainter#ASHEN_BELOW} splits Gloomfen from
     * Ashen Reach off the Veil's OWN biome field and threshold, so the two
     * grounds keep the proportions they shipped with.
     */
    public static final float FEN_SCALE = 118.0F;
    public static final float FEN_THRESHOLD = 0.62F;
    public static final int SALT_FEN = 79;

    // ---- Sub-biomes -------------------------------------------------------

    public static final int BIOME_SCALE = 150;
    /** Below this the ground is Ectomarsh; above ORCHARD_ABOVE it is Bone Orchard. */
    public static final float MARSH_BELOW = 0.34F;
    public static final float ORCHARD_ABOVE = 0.64F;

    /** Second-order patch noise: what breaks up each biome's main ground. */
    public static final int PATCH_SCALE = 20;
    public static final float PATCH_THRESHOLD = 0.66F;

    public static final int SALT_BIOME = 61;
    public static final int SALT_PATCH = 67;
    public static final int SALT_OBJECT = 71;
    public static final int SALT_GROVE = 73;

    // ---- Groves -----------------------------------------------------------

    /**
     * The composition field, and the answer to {@code IMPLEMENTATION_RULES}
     * §8 ("worldgen should compose scenes, not scatter objects").
     *
     * <p>A per-tile roll alone gives uniform confetti. This is a second, much
     * coarser noise field that decides where the trees are ALLOWED to be at
     * all: inside a grove the tree roll is answered, outside it every tree
     * result falls through to the ground cover. Scale 26 makes a grove about a
     * screen across; the threshold puts roughly a fifth of the land inside one,
     * which leaves four fifths of open ground to walk through and see the grove
     * from.
     */
    public static final float GROVE_SCALE = 26.0F;
    public static final float GROVE_THRESHOLD = 0.58F;

    // ---- POI lattices -----------------------------------------------------
    //
    // Three separate lattices rather than one with a three-way pick, because
    // they are three different densities: a mausoleum is a roadside find, a
    // manor is a landmark, and a sunken graveyard sits in the marsh. Each is
    // read the same way SkyTerrainPainter reads its wreck and workshop sites --
    // a jittered point per lattice cell, kept only when the cell's own hash
    // wins its chance -- so the same function answers "where is the nearest
    // one" for the pressure field and "may I build here" for the world preset.

    /** Mausoleum: the common tomb, roughly one per 2.6 cells of 170 tiles. */
    public static final int MAUSOLEUM_CELL = 170;
    public static final int SALT_MAUSOLEUM = 401;
    public static final float MAUSOLEUM_CHANCE = 0.38F;

    /** Haunted manor: the realm's landmark, rarer and much bigger. */
    public static final int MANOR_CELL = 380;
    public static final int SALT_MANOR = 409;
    public static final float MANOR_CHANCE = 0.34F;

    /** Sunken graveyard: an open walled field, at the marsh's edge. */
    public static final int GRAVEYARD_CELL = 240;
    public static final int SALT_GRAVEYARD = 419;
    public static final float GRAVEYARD_CHANCE = 0.36F;

    private GhostTerrainPainter() {
    }

    // ---- Painting ---------------------------------------------------------

    /**
     * One tile of the Ghost band, packed the way {@link SkyTerrainPainter#pack}
     * packs every tile of the plane.
     *
     * @param island    the plane's shared island field at this tile
     * @param waterline the plane's blended waterline at this depth
     */
    public static long describeBand(int seed, int tileX, int tileY,
            float island, float waterline, float depth, float distortion) {
        boolean fen = isFen(seed, tileX, tileY);
        boolean ashen = fen && SkyNoise.fbm(seed + VeilTerrainPainter.SALT_BIOME, tileX, tileY,
                VeilTerrainPainter.BIOME_SCALE, 2) < VeilTerrainPainter.ASHEN_BELOW;
        int biome = fen ? -1 : biomeAt(seed, tileX, tileY);
        int biomeClass = fen
                ? (ashen ? SkyTerrainPainter.BIOME_ASHEN_REACH : SkyTerrainPainter.BIOME_GLOOMFEN)
                : biomeClassOf(biome);

        if (island <= waterline) {
            // The fen's water is the Veil's black murkwater; the realm's own is
            // the ectoplasm that gives the Aftergarden its light.
            return SkyTerrainPainter.pack(
                    fen ? SkyRegistry.murkwaterID : GhostRealm.ectoplasmID, 0, biomeClass, false);
        }

        boolean patch = SkyNoise.fbm(seed + SALT_PATCH, tileX, tileY, PATCH_SCALE, 2)
                > PATCH_THRESHOLD;
        int tileID = fen ? fenGround(seed, tileX, tileY, ashen) : groundAt(biome, patch);
        if (island <= waterline + ISLAND_RIM) {
            return SkyTerrainPainter.pack(tileID, 0, biomeClass, false); // shorelines stay walkable
        }
        int objectID = fen
                ? VeilTerrainPainter.rollObject(seed, tileX, tileY, ashen, isFenPatch(seed, tileX, tileY))
                : rollObject(seed, tileX, tileY, biome, patch);
        return SkyTerrainPainter.pack(tileID, objectID, biomeClass, false);
    }

    /** Is this tile the Veil's fen rather than the Aftergarden? (§41.5) */
    public static boolean isFen(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + SALT_FEN, tileX, tileY, FEN_SCALE, 2) > FEN_THRESHOLD;
    }

    /** The Veil's own patch field: what breaks its grounds with blackpeat. */
    private static boolean isFenPatch(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + VeilTerrainPainter.SALT_PATCH, tileX, tileY,
                VeilTerrainPainter.PATCH_SCALE, 2) > VeilTerrainPainter.PATCH_THRESHOLD;
    }

    /** The fen's ground: the Veil's, unchanged. */
    private static int fenGround(int seed, int tileX, int tileY, boolean ashen) {
        if (isFenPatch(seed, tileX, tileY)) {
            return SkyRegistry.blackpeatID;
        }
        return ashen ? SkyRegistry.ashsandID : SkyRegistry.murkmossID;
    }

    /** Ground code -> the plane's biome class (see {@link SkyTerrainPainter}). */
    public static int biomeClassOf(int biome) {
        switch (biome) {
            case ECTOMARSH:
                return SkyTerrainPainter.BIOME_GHOST_ECTOMARSH;
            case BONE_ORCHARD:
                return SkyTerrainPainter.BIOME_GHOST_ORCHARD;
            default:
                return SkyTerrainPainter.BIOME_GHOST_AFTERGARDEN;
        }
    }

    // ---- Pure queries the preset placer and the pressure field share -------

    public static final int AFTERGARDEN = 0;
    public static final int BONE_ORCHARD = 1;
    public static final int ECTOMARSH = 2;

    /** Which of the three grounds this tile belongs to. */
    public static int biomeAt(int seed, int tileX, int tileY) {
        float value = SkyNoise.fbm(seed + SALT_BIOME, tileX, tileY, BIOME_SCALE, 2);
        if (value < MARSH_BELOW) {
            return ECTOMARSH;
        }
        return value > ORCHARD_ABOVE ? BONE_ORCHARD : AFTERGARDEN;
    }

    /** The ground tile a biome shows, and what its patch noise breaks it with. */
    public static int groundAt(int biome, boolean patch) {
        switch (biome) {
            case ECTOMARSH:
                return patch ? GhostRealm.graveyardSoilID : GhostRealm.ghostMossID;
            case BONE_ORCHARD:
                return patch ? GhostRealm.spiritStoneID : GhostRealm.violetDirtID;
            default:
                return patch ? GhostRealm.blackCobbleID : GhostRealm.hauntedGrassID;
        }
    }

    /**
     * Is this tile dry Ghost ground? Asked by the preset placer and the pressure
     * field, neither of which has a region in hand.
     *
     * <p>It answers against the PLANE's island field and waterline, so it cannot
     * disagree with what {@link #describeBand} paints later.
     */
    public static boolean isLand(int seed, int tileX, int tileY) {
        float depth = RealmDepth.depthAt(tileX, tileY,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed));
        float island = SkyNoise.fbm(seed, tileX, tileY, SkyTerrainPainter.ISLAND_SCALE, 3);
        return island > SkyTerrainPainter.waterlineAt(depth) + ISLAND_RIM;
    }

    /** Is this tile inside the Ghost band at all? */
    public static boolean isGhost(int seed, int tileX, int tileY) {
        return RealmDepth.realmAt(seed, tileX, tileY,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed)) == RealmDepth.REALM_GHOST;
    }

    /** Is this tile inside a grove — the field that clusters the trees? */
    public static boolean isGrove(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + SALT_GROVE, tileX, tileY, GROVE_SCALE, 2) > GROVE_THRESHOLD;
    }

    /**
     * Tiles to the nearest POI of any of the three lattices.
     *
     * <p>{@link GhostPressure} reads this to decide which ground a hostile may
     * appear on, and {@link GhostLevel} reads it to place the pack itself, so
     * the guards and the loot cannot come apart.
     */
    public static float nearestPoiDistance(int seed, int tileX, int tileY) {
        float nearest = SkyTerrainPainter.nearestSite(seed, tileX, tileY,
                MAUSOLEUM_CELL, SALT_MAUSOLEUM, MAUSOLEUM_CHANCE).distance;
        nearest = Math.min(nearest, SkyTerrainPainter.nearestSite(seed, tileX, tileY,
                MANOR_CELL, SALT_MANOR, MANOR_CHANCE).distance);
        nearest = Math.min(nearest, SkyTerrainPainter.nearestSite(seed, tileX, tileY,
                GRAVEYARD_CELL, SALT_GRAVEYARD, GRAVEYARD_CHANCE).distance);
        return nearest;
    }

    /**
     * What stands on one tile, or 0.
     *
     * <p>Two things shape this and neither is a flat probability.
     *
     * <p><b>Trees only grow in groves.</b> The tree band of the roll is gated
     * on {@link #isGrove}, so a dead orchard is a place with edges rather than
     * a uniform forest, and the open ground between groves stays open — which
     * is what makes the grove visible as a thing at all.
     *
     * <p><b>Total density is deliberately below the sky's.</b>
     * {@code WORLD_DESIGN} A4.2 is a judgement on the CURRENT state — <i>"nichts
     * so im Überfluss dass man nach einem Run schon so viel gesammelt hat dass
     * man Kisten füllen kann"</i> — and the sky's own measured figure was 0.31
     * to 0.38 objects per walkable tile. The bands below total 0.12 in the
     * Aftergarden, 0.155 in the Bone Orchard (its trees are the reason to go)
     * and 0.10 in the marsh. A run through the realm builds with what it
     * gathers; it does not retire on it.
     */
    public static int rollObject(int seed, int tileX, int tileY, int biome, boolean patch) {
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_OBJECT);
        boolean grove = isGrove(seed, tileX, tileY);
        switch (biome) {
            case ECTOMARSH:
                // The wet end: reeds and glowing growth, almost no wood.
                if (roll < 0.045F) return GhostRealm.widowVineID;
                if (roll < 0.075F) return GhostRealm.spiritMushroomID;
                if (roll < 0.090F) return GhostRealm.ectoplasmFernID;
                if (grove && roll < 0.100F) return GhostRealm.spiritWillowID;
                return 0;
            case BONE_ORCHARD:
                // Rows of dead wood on hard ground, and the realm's ore.
                if (grove && roll < 0.070F) return GhostRealm.bonewoodTreeID;
                if (grove && roll < 0.100F) return GhostRealm.crookedDeadTreeID;
                if (roll < 0.120F) return GhostRealm.ghostRockID;
                if (roll < 0.132F) return GhostRealm.spectralOreRockID;
                if (roll < 0.145F) return GhostRealm.gravestoneID;
                if (roll < 0.155F) return GhostRealm.mourningRoseID;
                return 0;
            default:
                // The garden: flowers first, wood only inside a grove.
                if (roll < 0.040F) return GhostRealm.ghostLilyID;
                if (roll < 0.062F) return GhostRealm.mourningRoseID;
                if (roll < 0.078F) return GhostRealm.ectoplasmFernID;
                if (grove && roll < 0.098F) return GhostRealm.crookedDeadTreeID;
                if (grove && roll < 0.106F) return GhostRealm.lanternTreeID;
                if (roll < 0.116F) return GhostRealm.ghostRockID;
                if (roll < 0.120F) return GhostRealm.gravestoneID;
                return 0;
        }
    }
}
