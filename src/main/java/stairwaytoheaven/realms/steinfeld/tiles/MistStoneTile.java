package stairwaytoheaven.realms.steinfeld.tiles;

import java.awt.Color;

/**
 * Mist Stone — the same rock as {@link WeatheredStoneTile}, out where the fog
 * sits on it. Cold, green-grey, no shine left.
 *
 * <p><b>Borrowed art:</b> vanilla {@code tiles/rock_splat}, by path — the
 * game's own plain surface rock, and the dull end of the bright/dull stone
 * pair the gradient is built on.
 */
public class MistStoneTile extends SteinfeldGroundTile {

    public MistStoneTile() {
        super("rock");
        this.mapColor = new Color(74, 84, 84);
    }

    @Override
    public int getTerrainPriority() {
        return 242;
    }
}
