package stairwaytoheaven.realms.ghost;

import necesse.engine.registries.ObjectLayerRegistry;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyTerrainPainter;

/**
 * Per-region terrain painter of the Ghost Realm: broad dead landmasses over
 * glowing ectoplasm marsh, three sub-biomes, and object density that composes
 * groves and grave-fields rather than sprinkling props one tile at a time.
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

    public static final int ISLAND_SCALE = 56;
    public static final float ISLAND_THRESHOLD = 0.44F;
    /** Tiles inside the shoreline that stay bare, so a coast is walkable. */
    public static final float ISLAND_RIM = 0.02F;

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

    public static void paintRegion(Region region, int seed) {
        int tileWidth = region.tileLayer.region.tileWidth;
        int tileHeight = region.tileLayer.region.tileHeight;
        for (int rx = 0; rx < tileWidth; rx++) {
            for (int ry = 0; ry < tileHeight; ry++) {
                int tileX = rx + region.tileXOffset;
                int tileY = ry + region.tileYOffset;

                int biome = biomeAt(seed, tileX, tileY);
                region.biomeLayer.setBiomeByRegion(rx, ry, biomeID(biome));

                float island = SkyNoise.fbm(seed, tileX, tileY, ISLAND_SCALE, 3);
                if (island <= ISLAND_THRESHOLD) {
                    region.tileLayer.setTileByRegion(rx, ry, GhostRealm.ectoplasmID);
                    continue;
                }
                boolean patch = SkyNoise.fbm(seed + SALT_PATCH, tileX, tileY, PATCH_SCALE, 2)
                        > PATCH_THRESHOLD;
                region.tileLayer.setTileByRegion(rx, ry, groundAt(biome, patch));

                if (island <= ISLAND_THRESHOLD + ISLAND_RIM) {
                    continue; // keep shorelines walkable
                }
                int objectID = rollObject(seed, tileX, tileY, biome, patch);
                if (objectID != 0) {
                    region.objectLayer.setObjectByRegion(
                            ObjectLayerRegistry.BASE_LAYER, rx, ry, objectID);
                }
            }
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

    private static int biomeID(int biome) {
        switch (biome) {
            case ECTOMARSH:
                return GhostRealm.ectomarsh.getID();
            case BONE_ORCHARD:
                return GhostRealm.boneOrchard.getID();
            default:
                return GhostRealm.aftergarden.getID();
        }
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

    /** Is this tile dry land? Asked by the preset placer before a region exists. */
    public static boolean isLand(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed, tileX, tileY, ISLAND_SCALE, 3) > ISLAND_THRESHOLD + ISLAND_RIM;
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
