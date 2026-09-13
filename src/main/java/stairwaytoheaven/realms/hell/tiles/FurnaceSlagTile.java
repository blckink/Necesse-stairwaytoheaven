package stairwaytoheaven.realms.hell.tiles;

import java.awt.Color;
import java.awt.Point;

import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;

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
        this.mapColor = new Color(64, 30, 22);
    }

    /** One above {@link CinderAshTile}: inside Hell, the slag wins the seam. */
    @Override
    public int getTerrainPriority() {
        return 207;
    }

    /**
     * World-anchored: the ground is one seamless 128x128 texture laid over 4x4
     * tiles, so its cracks and seams run on across tiles instead of being cut at
     * every tile edge the way {@code SkyGroundTile}'s random row pick cuts them.
     * Same idiom and same {@code floorMod} guard as
     * {@link stairwaytoheaven.tiles.CheckerFloorTile}.
     */
    @Override
    public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
        int columns = Math.max(1, terrainTexture.getWidth() / 32);
        int rows = Math.max(1, terrainTexture.getHeight() / 32);
        return new Point(Math.floorMod(tileX, columns), Math.floorMod(tileY, rows));
    }
}
