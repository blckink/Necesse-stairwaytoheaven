package stairwaytoheaven.tiles;

import java.awt.Color;

/**
 * Fine grey ash of the Ashen Reach — dune waste of the Veil.
 */
public class AshsandTile extends SkyGroundTile {

    public AshsandTile() {
        super(false, "ashsand");
        this.mapColor = new Color(98, 94, 92);
        this.canBeMined = true;
    }

    @Override
    public int getTerrainPriority() {
        return 203;
    }
}
