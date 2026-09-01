package stairwaytoheaven.tiles;

import java.awt.Color;

/**
 * Wet black peat patches breaking up the Gloomfen moss.
 */
public class BlackpeatTile extends SkyGroundTile {

    public BlackpeatTile() {
        super(false, "blackpeat");
        this.mapColor = new Color(44, 39, 44);
        this.canBeMined = true;
    }

    @Override
    public int getTerrainPriority() {
        return 204;
    }
}
