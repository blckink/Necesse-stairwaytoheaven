package stairwaytoheaven.tiles;

import java.awt.Color;

/**
 * Weathered rock of the sky islands; forms the outcrop plateaus where Skystone
 * rocks and Aetherium veins surface.
 */
public class SkystoneTile extends SkyGroundTile {

    public SkystoneTile() {
        super(false, "skystone");
        this.mapColor = new Color(122, 132, 148);
        this.canBeMined = true;
    }

    @Override
    public int getTerrainPriority() {
        return 230;
    }
}
