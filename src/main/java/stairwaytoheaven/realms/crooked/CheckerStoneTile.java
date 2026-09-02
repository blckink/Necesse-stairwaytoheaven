package stairwaytoheaven.realms.crooked;

import java.awt.Color;

/**
 * Checker Stone — the ground of the Checkerworks, the band of the realm where
 * somebody built.
 *
 * <p><b>Borrowed sheet:</b> vanilla {@code tiles/deepstonetiledfloor_splat.png},
 * 224x192 — dark laid squares with a repeating inset.
 *
 * <p><b>Registered as TERRAIN, not as a floor, on purpose.</b> Worldgen paints
 * it by area and it has to blend into the striped ground around it;
 * {@code GameTile(isFloor)} is the switch that decides that, and a "floor" built
 * as terrain (or the reverse) is the exact lie
 * {@code tools/tile_behaviour_audit.py} exists to catch. The mod's real
 * chequerboard FLOOR — {@code marblechecker}, craftable, world-anchored — stays
 * what it is, and this realm's presets use it as the accent it was always meant
 * to be.
 */
public class CheckerStoneTile extends CrookedGroundTile {

    public CheckerStoneTile() {
        super("deepstonetiledfloor", new Color(58, 62, 70));
    }
}
