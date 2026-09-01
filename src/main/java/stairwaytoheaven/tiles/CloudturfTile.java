package stairwaytoheaven.tiles;

import java.awt.Color;

/**
 * Default island ground of the Skyreach: pale, dense turf grown on compacted
 * cloud. Plain terrain tile following the vanilla RockTile pattern (random
 * variant row per tile position).
 */
public class CloudturfTile extends SkyGroundTile {

    public CloudturfTile() {
        super(false, "cloudturf");
        this.mapColor = new Color(176, 189, 197);
        this.canBeMined = true;
        // organic soil: grass-type objects (Sky Reeds) may grow on it
        this.isOrganic = true;
    }

    @Override
    public int getTerrainPriority() {
        return 210;
    }
}
