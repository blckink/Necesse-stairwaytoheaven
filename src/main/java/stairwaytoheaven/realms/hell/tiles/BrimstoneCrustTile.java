package stairwaytoheaven.realms.hell.tiles;

import java.awt.Color;
import java.awt.Point;

import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;

import stairwaytoheaven.tiles.SkyGroundTile;

/**
 * Brimstone Crust: ochre sulphur with bubbling vents, the bright ground of the
 * Furnace Reach.
 *
 * <p>It takes over the share the Veil's {@code miststone} held in
 * {@link stairwaytoheaven.realms.hell.HellTerrainPainter#groundAt} — a
 * placeholder, since that tile floors the Veil too. Much lighter than
 * {@link CinderAshTile} on purpose, so Hell's grounds read against each other.
 *
 * <p>One seamless 128x128 texture, no {@code _splat}, laid world-anchored the
 * way {@link CinderAshTile} is.
 */
public class BrimstoneCrustTile extends SkyGroundTile {

    public BrimstoneCrustTile() {
        super(false, "brimstonecrust");
        this.mapColor = new Color(176, 132, 40);
    }

    /** The next free rung above {@link FurnaceSlagTile} (207). */
    @Override
    public int getTerrainPriority() {
        return 208;
    }

    @Override
    public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
        int columns = Math.max(1, terrainTexture.getWidth() / 32);
        int rows = Math.max(1, terrainTexture.getHeight() / 32);
        return new Point(Math.floorMod(tileX, columns), Math.floorMod(tileY, rows));
    }
}
