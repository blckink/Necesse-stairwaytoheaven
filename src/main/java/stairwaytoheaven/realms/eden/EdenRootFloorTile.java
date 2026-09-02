package stairwaytoheaven.realms.eden;

import java.awt.Color;

/**
 * Root Floor — the canopy's own ground, woven out of the giant trees standing
 * on it (§5's tile list).
 *
 * <p><b>Borrowed art:</b> vanilla {@code tiles/ancientroots_splat.png}.
 */
public class EdenRootFloorTile extends EdenGroundTile {

    public EdenRootFloorTile() {
        super("ancientroots");
        this.mapColor = new Color(74, 52, 40);
        this.canBeMined = true;
        this.isOrganic = true;
    }

    @Override
    public int getTerrainPriority() {
        // Above the two greens and the soil: where roots reach, roots win.
        return 205;
    }
}
