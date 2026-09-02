package stairwaytoheaven.realms.crooked;

import java.awt.Color;

/**
 * Crooked Stripe — the realm's default ground, and the one sheet here that is
 * already ours.
 *
 * <p>{@code tiles/beetlefreak_splat.png} is the striped, flowering, wrong ground
 * drawn for the Veil's Beetlefreak Hollows and then reused for the Skyreach's
 * Outlands rim. Crooked Beyond is the place both of those were pointing at, so
 * it inherits the stripes rather than inventing a second set of them — that is
 * exactly what makes the rim read as a rim once the realm exists.
 *
 * <p>The {@code "splattingmaskwide"} argument is not optional; see
 * {@link CrookedGroundTile}'s three-argument constructor for the trap.
 */
public class CrookedStripeTile extends CrookedGroundTile {

    public CrookedStripeTile() {
        super("beetlefreak", "splattingmaskwide", new Color(118, 46, 158));
        this.isOrganic = true;
    }

    /**
     * Above the realm's other grounds, the way {@code BeetlefreakTile} sits at
     * 200 above the Veil's: the stripes are what bleeds outward into a
     * neighbour, not what gets bled into.
     */
    @Override
    public int getTerrainPriority() {
        return 200;
    }
}
