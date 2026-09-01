package stairwaytoheaven.tiles;

import java.awt.Color;

/**
 * Gloomfen ground: dense dark moss over cold peat. Organic — whisper reeds grow on it.
 */
public class MurkmossTile extends SkyGroundTile {

    public MurkmossTile() {
        super(false, "murkmoss");
        this.mapColor = new Color(62, 72, 62);
        this.canBeMined = true;
        // organic soil: grass-type objects (Sky Reeds) may grow on it
        this.isOrganic = true;
    }

    @Override
    public int getTerrainPriority() {
        return 205;
    }
}
