package stairwaytoheaven.realms.hell.tiles;

import java.awt.Color;

import stairwaytoheaven.tiles.SkyGroundTile;

/**
 * Hell's dark ground: cooled slag crust with the embers still showing through.
 *
 * <p>The other half of the pair {@link CinderAshTile} explains — the two of
 * them take over exactly the share {@code ashsand} and {@code blackpeat} held
 * in {@link stairwaytoheaven.realms.hell.HellTerrainPainter}, and nothing else
 * changes. {@code deadsoil} and {@code miststone} keep their place in the
 * Furnace mix; they are Steinfeld's and are borrowed on purpose.
 *
 * <p>Its sheet is the darker of the two so the Furnace reads as the far edge
 * of the plane, with the orange specks doing the heat rather than the base
 * value: a ground the player walks over for a whole band cannot be the
 * brightest thing on the screen.
 */
public class FurnaceSlagTile extends SkyGroundTile {

    public FurnaceSlagTile() {
        super(false, "furnaceslag");
        this.mapColor = new Color(38, 40, 50);
    }

    /** One above {@link CinderAshTile}: inside Hell, the slag wins the seam. */
    @Override
    public int getTerrainPriority() {
        return 207;
    }
}
