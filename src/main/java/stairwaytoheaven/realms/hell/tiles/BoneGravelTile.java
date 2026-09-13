package stairwaytoheaven.realms.hell.tiles;

import java.awt.Color;
import java.awt.Point;

import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;

import stairwaytoheaven.tiles.SkyGroundTile;

/**
 * Bone Gravel: wine-red grit strewn with teeth, vertebrae and the odd tiny
 * skull.
 *
 * <p>It takes over the share the Veil's {@code deadsoil} held in
 * {@link stairwaytoheaven.realms.hell.HellTerrainPainter#groundAt}, which was a
 * placeholder borrowed from another realm.
 *
 * <p>One seamless 128x128 texture, no {@code _splat}, laid world-anchored the
 * way {@link CinderAshTile} is.
 */
public class BoneGravelTile extends SkyGroundTile {

    public BoneGravelTile() {
        super(false, "bonegravel");
        this.mapColor = new Color(110, 26, 30);
    }

    /** One above {@link BrimstoneCrustTile} (208). */
    @Override
    public int getTerrainPriority() {
        return 209;
    }

    @Override
    public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
        int columns = Math.max(1, terrainTexture.getWidth() / 32);
        int rows = Math.max(1, terrainTexture.getHeight() / 32);
        return new Point(Math.floorMod(tileX, columns), Math.floorMod(tileY, rows));
    }
}
