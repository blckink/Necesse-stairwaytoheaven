package stairwaytoheaven.realms.crooked;

import stairwaytoheaven.worldgen.SkyNoise;

/**
 * Where the places are in Crooked Beyond — the one lattice the whole realm
 * agrees on.
 *
 * <h2>Why a lattice and not a per-tile roll</h2>
 * {@code WORLD_DESIGN.md} A4.1, in the player's words: <i>"sie sollen mal
 * geballt kommen und ein Gebiet z.b bewachen wo es loot gibt, in anderen Ecken
 * aber nicht dauernd angeflogen kommen"</i> — and A4.5's measurement of how
 * vanilla answers it: a concentrated place is assembled at a POSITION, from
 * modular parts, and the enemies are placed beside the thing they guard. A
 * per-tile roll has no notion of position and therefore cannot express any of
 * that. So every place in this realm — all three POI kinds and the pack that
 * stands over each — is derived from one hashed site per lattice cell.
 *
 * <p>Three consumers ask this class the same questions and must get the same
 * answers: {@link CrookedTerrainPainter} (which paves the ground around a
 * site), {@link CrookedPressure} (which decides where a hostile may appear at
 * all) and {@link CrookedWorldPreset} (which stamps the building). It is
 * therefore pure noise over the level seed and the tile position, with no world
 * state read anywhere — the same contract the rest of {@code worldgen} keeps,
 * and the reason the offline answer and the live one cannot drift apart.
 *
 * <h2>The three kinds</h2>
 * <table>
 *   <caption>POI lattices</caption>
 *   <tr><th>kind</th><th>cell</th><th>chance</th><th>what stands there</th></tr>
 *   <tr><td>{@link #DOORYARD_CELL}</td><td>190</td><td>0.55</td>
 *       <td>The Door Yard — free-standing doors, no house</td></tr>
 *   <tr><td>{@link #INVERTED_CELL}</td><td>230</td><td>0.50</td>
 *       <td>The Inside-Out House — walls in the middle, floor around them</td></tr>
 *   <tr><td>{@link #LONGTABLE_CELL}</td><td>270</td><td>0.45</td>
 *       <td>The Long Table — a corridor of dinner between two doors</td></tr>
 * </table>
 *
 * <p>The cells are deliberately different sizes and coprime-ish, so the three
 * kinds do not fall into step with one another and a player crossing the realm
 * does not meet them in a repeating order. At these rates a 1000x1000-tile walk
 * passes roughly 15 doorway yards, 9 inside-out houses and 6 long tables —
 * often enough that the realm has somewhere to go, rare enough that arriving is
 * still an event.
 */
public final class CrookedSites {

    /** Kind constants, returned by {@link #kindAt}. */
    public static final int SITE_NONE = 0;
    public static final int SITE_DOORYARD = 1;
    public static final int SITE_INVERTED = 2;
    public static final int SITE_LONGTABLE = 3;

    public static final int DOORYARD_CELL = 190;
    public static final int INVERTED_CELL = 230;
    public static final int LONGTABLE_CELL = 270;

    public static final float DOORYARD_CHANCE = 0.55F;
    public static final float INVERTED_CHANCE = 0.50F;
    public static final float LONGTABLE_CHANCE = 0.45F;

    public static final int SALT_DOORYARD = 0xC0DE01;
    public static final int SALT_INVERTED = 0xC0DE11;
    public static final int SALT_LONGTABLE = 0xC0DE21;

    private CrookedSites() {
    }

    /** One site: where it is, how far the asking tile was from it, and whether it exists. */
    public static final class Site {
        public final boolean exists;
        public final int x;
        public final int y;
        public final float distance;

        Site(boolean exists, int x, int y, float distance) {
            this.exists = exists;
            this.x = x;
            this.y = y;
            this.distance = distance;
        }
    }

    private static final Site NONE = new Site(false, 0, 0, Float.MAX_VALUE);

    /**
     * The nearest site of one lattice to a tile, searching the nine cells
     * around it.
     *
     * <p>Nine and not one: a site sits anywhere inside its cell, so the nearest
     * one to a tile near a cell edge is routinely in the neighbouring cell. The
     * Skyreach's {@code SkyTerrainPainter.nearestSite} makes the same sweep for
     * the same reason.
     */
    public static Site nearest(int seed, int tileX, int tileY, int cell, int salt, float chance) {
        int cx = Math.floorDiv(tileX, cell);
        int cy = Math.floorDiv(tileY, cell);
        Site best = NONE;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                int gx = cx + ox;
                int gy = cy + oy;
                if (SkyNoise.hash(seed + salt, gx, gy) >= chance) {
                    continue;
                }
                int sx = Math.round(gx * cell + SkyNoise.hash(seed + salt + 1, gx, gy) * cell);
                int sy = Math.round(gy * cell + SkyNoise.hash(seed + salt + 2, gx, gy) * cell);
                float dx = tileX - sx;
                float dy = tileY - sy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < best.distance) {
                    best = new Site(true, sx, sy, dist);
                }
            }
        }
        return best;
    }

    /** The nearest Door Yard. */
    public static Site nearestDoorYard(int seed, int tileX, int tileY) {
        return nearest(seed, tileX, tileY, DOORYARD_CELL, SALT_DOORYARD, DOORYARD_CHANCE);
    }

    /** The nearest Inside-Out House. */
    public static Site nearestInvertedHouse(int seed, int tileX, int tileY) {
        return nearest(seed, tileX, tileY, INVERTED_CELL, SALT_INVERTED, INVERTED_CHANCE);
    }

    /** The nearest Long Table. */
    public static Site nearestLongTable(int seed, int tileX, int tileY) {
        return nearest(seed, tileX, tileY, LONGTABLE_CELL, SALT_LONGTABLE, LONGTABLE_CHANCE);
    }

    /** Distance in tiles to the nearest POI of any kind. */
    public static float siteDistance(int seed, int tileX, int tileY) {
        float d = nearestDoorYard(seed, tileX, tileY).distance;
        d = Math.min(d, nearestInvertedHouse(seed, tileX, tileY).distance);
        return Math.min(d, nearestLongTable(seed, tileX, tileY).distance);
    }

    /**
     * Which POI, if any, this exact tile is the ORIGIN of.
     *
     * <p>Used by the preset placer to skip a candidate that is not on the
     * lattice, and by the status tooling. The tile has to be the site centre
     * itself, not merely near one.
     */
    public static int kindAt(int seed, int tileX, int tileY) {
        Site yard = nearestDoorYard(seed, tileX, tileY);
        if (yard.exists && yard.x == tileX && yard.y == tileY) {
            return SITE_DOORYARD;
        }
        Site house = nearestInvertedHouse(seed, tileX, tileY);
        if (house.exists && house.x == tileX && house.y == tileY) {
            return SITE_INVERTED;
        }
        Site table = nearestLongTable(seed, tileX, tileY);
        if (table.exists && table.x == tileX && table.y == tileY) {
            return SITE_LONGTABLE;
        }
        return SITE_NONE;
    }
}
