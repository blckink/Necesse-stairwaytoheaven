package stairwaytoheaven.realms.steinfeld.tiles;

import java.awt.Color;

/**
 * Weathered Stone — bright heaven-stone lying in the open, still pale near
 * Eden and dulling as you walk out.
 *
 * <p><b>Borrowed art:</b> {@code tiles/skystone_splat}, the Skyreach's own bare
 * stone. Steinfeld is where the sky's material ends up
 * ({@code docs/WORLD_DESIGN.md} A3.4), so this is the sky's stone by intention;
 * {@link MistStoneTile} is the same rock two bands further out and several
 * shades greyer, and that pair is half of what makes the gradient readable on
 * the map.
 */
public class WeatheredStoneTile extends SteinfeldGroundTile {

    public WeatheredStoneTile() {
        super("skystone");
        this.mapColor = new Color(150, 160, 172);
    }

    @Override
    public int getTerrainPriority() {
        return 241;
    }
}
