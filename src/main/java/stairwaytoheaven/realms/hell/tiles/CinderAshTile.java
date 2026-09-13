package stairwaytoheaven.realms.hell.tiles;

import java.awt.Color;
import java.awt.Point;

import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;

import stairwaytoheaven.tiles.SkyGroundTile;

/**
 * Hell's pale ground: burnt grit and cinder dust, the Infernal Fringe's floor.
 *
 * <p>It replaces nothing. Until now Hell painted the Veil's {@code ashsand}
 * and the Gloomfen's {@code blackpeat} — a placeholder {@code docs/STATUS.md}
 * 6n put there on purpose ("notfalls mit Platzhaltern aus vorhandenen
 * Vanilla-Kacheln"), because those two tiles also floor the Ghost band, Crooked
 * Beyond and the Outlands. Repainting them for Hell would have repainted three
 * other realms with it, so Hell gets its own pair instead and the shared tiles
 * stay exactly as they were.
 *
 * <p>The sheet is a stamped splat, not a generated one: only the flat texture
 * came from an image model, and {@code tools/splat_from_texture.py} pressed it
 * through {@code ashsand_splat}'s alpha, so all 21 cell shapes are still the
 * engine's own (commit 49ea020). {@code tools/splat_check.py} is the gate that
 * says so per file.
 */
public class CinderAshTile extends SkyGroundTile {

    public CinderAshTile() {
        super(false, "cinderash");
        this.mapColor = new Color(46, 24, 20);
    }

    /**
     * 206, the first free rung above the two tiles it takes over from
     * ({@code ashsand} 203, {@code blackpeat} 204) and above Crooked's violet
     * mud and stripe (200): where the Fringe mixes Hell's ground into
     * Crooked's, Hell's is what draws over the seam.
     */
    @Override
    public int getTerrainPriority() {
        return 206;
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
