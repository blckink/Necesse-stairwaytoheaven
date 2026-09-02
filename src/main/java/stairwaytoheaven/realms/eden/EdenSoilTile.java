package stairwaytoheaven.realms.eden;

import java.awt.Color;

/**
 * Rich Eden Soil — the dark, wet earth under the canopy (§5's tile list).
 *
 * <p><b>Borrowed art:</b> vanilla {@code tiles/mud_splat.png}. Measured against
 * {@code tools/tile_behaviour_audit.py}'s bands (which were themselves
 * calibrated over 66 vanilla sheets): every cell inside the vanilla range.
 *
 * <p>Organic, so the realm's flora grows on it — VERIFIED [jar]
 * {@code GrassObject.runGrassCanPlace} (GrassObject.java:253) refuses a
 * non-organic tile outright when the plant carries no explicit tile list.
 */
public class EdenSoilTile extends EdenGroundTile {

    public EdenSoilTile() {
        super("mud");
        this.mapColor = new Color(96, 66, 38);
        this.canBeMined = true;
        this.isOrganic = true;
    }

    @Override
    public int getTerrainPriority() {
        // Under Eden grass (200, vanilla overgrowngrass's own value) and under
        // the root floor, so soil never eats a clearing it borders.
        return 190;
    }
}
