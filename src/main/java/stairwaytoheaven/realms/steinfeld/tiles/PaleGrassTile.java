package stairwaytoheaven.realms.steinfeld.tiles;

import java.awt.Color;

/**
 * Pale Grass — the grass of the middle Reach, where the colour has gone out of
 * the ground but the ground is still alive.
 *
 * <p><b>Borrowed art:</b> {@code tiles/cloudturf_splat} — the Skyreach's own
 * silver-green turf, which is the mod's palest living grass. That is not a
 * shortcut, it is the fiction: {@code docs/WORLD_DESIGN.md} A3.4 calls
 * Steinfeld <i>"the place where the sky stops working properly"</i>, and the
 * sky's own ground bleaching out here is exactly what the gradient is about.
 */
public class PaleGrassTile extends SteinfeldGroundTile {

    public PaleGrassTile() {
        super("cloudturf");
        this.mapColor = new Color(178, 190, 176);
        // Organic, so grass-type flora (Withered Grass, Widow Flower) may sit
        // on it the way Sky Reeds sit on Cloudturf.
        this.isOrganic = true;
    }

    @Override
    public int getTerrainPriority() {
        // Highest of the seven: grass creeps over stone, never the other way.
        return 246;
    }
}
