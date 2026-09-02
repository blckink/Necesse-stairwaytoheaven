package stairwaytoheaven.realms.steinfeld.tiles;

import java.awt.Color;

/**
 * Dead Soil — bare ground where the grass has given up. The first thing in the
 * Reach that is simply absence.
 *
 * <p><b>Borrowed art:</b> vanilla {@code tiles/dirt_splat}, by path.
 */
public class DeadSoilTile extends SteinfeldGroundTile {

    public DeadSoilTile() {
        super("dirt");
        this.mapColor = new Color(98, 84, 78);
        // Bare earth still holds a root: the reeds and the Dead Heaven Bloom
        // are grass-type objects and want organic ground under them.
        this.isOrganic = true;
    }

    @Override
    public int getTerrainPriority() {
        return 244;
    }
}
