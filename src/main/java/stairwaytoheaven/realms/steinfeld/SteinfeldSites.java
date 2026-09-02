package stairwaytoheaven.realms.steinfeld;

import stairwaytoheaven.worldgen.SkyNoise;

/**
 * Where Steinfeld's two HAND-AUTHORED landmarks are — a second, independent
 * lattice alongside {@link SteinfeldTerrainPainter}'s own organic POI system.
 *
 * <h2>Why a second lattice rather than teaching the organic one a fourth kind</h2>
 * {@link SteinfeldTerrainPainter}'s {@code POI_CELL}/{@code SALT_POI} lattice
 * already paints a grave field, a ruined chapel or a statue field at every
 * site it rolls — one roughly every 230x230 tiles — entirely procedurally,
 * tile by tile, inside {@code paintRegion}. That system is complete and it is
 * what makes the realm's open country feel inhabited rather than empty. A
 * hand-authored {@code Preset} stamped on the SAME site would collide with
 * it: both write to the same tiles in the same region-generation pass, and
 * the result is either the preset overwriting the organic scatter it stood
 * on or the two fighting over the same ground.
 *
 * <p>A second, independent, much RARER lattice avoids the collision
 * entirely and gives Steinfeld a second kind of place: not just where the
 * ground is different, but where somebody built something specific once. The
 * two cell sizes are large enough, and different enough from each other and
 * from {@code POI_CELL} = 180, that a landmark and an organic site sharing
 * ground is a rare, harmless coincidence rather than a routine collision —
 * the same tolerance vanilla's own overlapping structure systems accept.
 *
 * <h2>The two kinds</h2>
 * <table>
 *   <caption>Landmark lattices</caption>
 *   <tr><th>kind</th><th>cell</th><th>chance</th><th>what stands there</th></tr>
 *   <tr><td>{@link #GRAVEYARD_CELL}</td><td>620</td><td>0.42</td>
 *       <td>{@link GraveyardPreset} — a walled plot, hand-laid</td></tr>
 *   <tr><td>{@link #CHAPEL_CELL}</td><td>740</td><td>0.38</td>
 *       <td>{@link RuinedChapelPreset} — a roofless nave</td></tr>
 * </table>
 *
 * <h2>Guarded, like everything else A4.1 governs</h2>
 * {@link SteinfeldPressure} and {@link SteinfeldLevel#placeGuardPacks} both
 * consult this lattice too, exactly as they consult the organic one, so a
 * hand-authored landmark is guarded before the player arrives the same way an
 * organic site is — A4.1 does not stop applying just because the building was
 * drawn by hand.
 */
public final class SteinfeldSites {

    private SteinfeldSites() {
    }

    /** Kind constants, returned by {@link #kindAt}. */
    public static final int SITE_NONE = 0;
    public static final int SITE_GRAVEYARD = 1;
    public static final int SITE_CHAPEL = 2;

    public static final int GRAVEYARD_CELL = 620;
    public static final int CHAPEL_CELL = 740;

    public static final float GRAVEYARD_CHANCE = 0.42F;
    public static final float CHAPEL_CHANCE = 0.38F;

    public static final int SALT_GRAVEYARD = 0x57E1FA01;
    public static final int SALT_CHAPEL = 0x57E1FB01;

    /** One site: where it is, how far the asking tile was, and whether it exists. */
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
     * around it — {@code CrookedSites.nearest} verbatim, because a site sits
     * anywhere inside its cell and the nearest one to a tile near a cell edge
     * is routinely in the neighbouring cell.
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

    public static Site nearestGraveyard(int seed, int tileX, int tileY) {
        return nearest(seed, tileX, tileY, GRAVEYARD_CELL, SALT_GRAVEYARD, GRAVEYARD_CHANCE);
    }

    public static Site nearestChapel(int seed, int tileX, int tileY) {
        return nearest(seed, tileX, tileY, CHAPEL_CELL, SALT_CHAPEL, CHAPEL_CHANCE);
    }

    /** Distance in tiles to the nearest landmark of either kind. */
    public static float siteDistance(int seed, int tileX, int tileY) {
        float d = nearestGraveyard(seed, tileX, tileY).distance;
        return Math.min(d, nearestChapel(seed, tileX, tileY).distance);
    }

    /**
     * Which landmark, if any, this exact tile is the ORIGIN of. Used by the
     * preset placer to skip a candidate not on the lattice, and by
     * {@link SteinfeldLevel#placeGuardPacks} to find the guard's anchor tile.
     */
    public static int kindAt(int seed, int tileX, int tileY) {
        Site graveyard = nearestGraveyard(seed, tileX, tileY);
        if (graveyard.exists && graveyard.x == tileX && graveyard.y == tileY) {
            return SITE_GRAVEYARD;
        }
        Site chapel = nearestChapel(seed, tileX, tileY);
        if (chapel.exists && chapel.x == tileX && chapel.y == tileY) {
            return SITE_CHAPEL;
        }
        return SITE_NONE;
    }
}
