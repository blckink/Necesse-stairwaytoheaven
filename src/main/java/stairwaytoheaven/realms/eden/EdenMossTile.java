package stairwaytoheaven.realms.eden;

import java.awt.Color;

/**
 * Eden Moss — the thinner green between the grass (§5's tile list).
 *
 * <p><b>Borrowed art:</b> vanilla {@code tiles/overgrowngrass_splat.png} —
 * which is the sheet the player's own supplied {@code overgrowneden} art was
 * drawn on (see {@code stairwaytoheaven.tiles.OvergrownEdenTile}), so the two
 * greens are the same family by construction rather than by luck.
 */
public class EdenMossTile extends EdenGroundTile {

    public EdenMossTile() {
        super("overgrowngrass");
        // Vanilla OvergrownGrassTile's own map colour, unchanged.
        this.mapColor = new Color(61, 87, 0);
        this.canBeMined = true;
        this.isOrganic = true;
    }

    @Override
    public int getTerrainPriority() {
        // One below Eden grass's 200, so the player's supplied ground wins
        // every edge between the two and stays the tile you actually see.
        return 199;
    }
}
