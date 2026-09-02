package stairwaytoheaven.realms.steinfeld.tiles;

import java.awt.Color;

/**
 * Ash Grass — the grey-green mat of the outer Reach. Still a lawn, in the way
 * that a grave is still a garden.
 *
 * <p><b>Borrowed art:</b> {@code tiles/murkmoss_splat}, the Veil's fen moss.
 * The player has said the Veil's tile family may be reused for the later
 * regions, and the Reach is the region that leads into it: the ground under
 * the player in the fog band is literally the ground of the realm on the other
 * side of the fog.
 */
public class AshGrassTile extends SteinfeldGroundTile {

    public AshGrassTile() {
        super("murkmoss");
        this.mapColor = new Color(88, 96, 88);
        this.isOrganic = true;
    }

    @Override
    public int getTerrainPriority() {
        return 245;
    }
}
