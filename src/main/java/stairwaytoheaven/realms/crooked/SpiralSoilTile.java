package stairwaytoheaven.realms.crooked;

import java.awt.Color;

/**
 * Spiral Soil — trodden coils of violet growth, and the ground of the Spiral
 * Fields.
 *
 * <p><b>Borrowed sheet:</b> vanilla {@code tiles/ascendedgrowth.png}, 128x128.
 * It has no {@code _splat} sibling, so it takes {@code TerrainSplatterTile}'s
 * legacy path (jar 1.3.2, generateSplattingTextures line 158) and gets four
 * variant rows stencilled through the shared 64x64 {@code splattingmask} —
 * which is safe here and not for a tiled floor, because
 * {@link CrookedGroundTile#getTerrainSprite} picks its row with
 * {@code nextInt(height / 32)} rather than with {@code tileX % columns}. That
 * modulo is what killed a client on negative coordinates once already; see
 * {@link stairwaytoheaven.tiles.CheckerFloorTile}.
 *
 * <p>Looked at against the sprite dump rather than assumed: a dark violet bed
 * with paler loops running through it, which is as near §13's "Spiral Soil" as
 * anything the game already owns.
 */
public class SpiralSoilTile extends CrookedGroundTile {

    public SpiralSoilTile() {
        super("ascendedgrowth", new Color(96, 62, 140));
        this.isOrganic = true;
    }
}
