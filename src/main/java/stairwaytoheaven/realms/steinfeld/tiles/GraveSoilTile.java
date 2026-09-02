package stairwaytoheaven.realms.steinfeld.tiles;

import java.awt.Color;

/**
 * Grave Soil — near-black turned earth. The darkest ground in the realm, and
 * what a grave field is cut into.
 *
 * <p><b>Borrowed art:</b> vanilla {@code tiles/cryptash_splat}, by path.
 */
public class GraveSoilTile extends SteinfeldGroundTile {

    public GraveSoilTile() {
        super("cryptash");
        this.mapColor = new Color(48, 48, 50);
        this.isOrganic = true;
    }

    @Override
    public int getTerrainPriority() {
        return 243;
    }
}
