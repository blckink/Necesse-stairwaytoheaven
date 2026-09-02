package stairwaytoheaven.realms.eden;

import java.awt.Color;

/**
 * White Paradise Sand — the lagoon beaches (§5, and A3.3's <i>"white sand,
 * turquoise water"</i>).
 *
 * <p><b>Borrowed art:</b> vanilla {@code tiles/sand_splat.png}.
 *
 * <p>NOT organic, exactly like vanilla sand. The shore flora carries an
 * explicit {@code grassValidTileIDs} instead (see {@link EdenRealm}), which is
 * how reeds and lotus grow on a beach without the beach behaving like a lawn.
 */
public class ParadiseSandTile extends EdenGroundTile {

    public ParadiseSandTile() {
        super("sand");
        this.mapColor = new Color(232, 216, 178);
        this.canBeMined = true;
    }

    @Override
    public int getTerrainPriority() {
        // Above the greens, so a beach keeps a clean edge against the garden
        // instead of being splattered over by it.
        return 215;
    }
}
