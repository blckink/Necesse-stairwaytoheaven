package stairwaytoheaven.tiles;

import java.awt.Color;

/**
 * Charcoal slate ground of the Stormveil — dark, cracked stone charged with
 * static.
 */
public class StormslateTile extends SkyGroundTile {

    public StormslateTile() {
        super(false, "stormslate");
        this.mapColor = new Color(74, 78, 94);
        this.canBeMined = true;
    }

    @Override
    public int getTerrainPriority() {
        return 220;
    }
}
