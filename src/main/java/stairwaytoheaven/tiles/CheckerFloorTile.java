package stairwaytoheaven.tiles;

import java.awt.Color;
import java.awt.Point;

import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.gameTile.SimpleTiledFloorTile;
import necesse.level.maps.Level;

/**
 * A world-anchored tiled floor whose pattern survives negative tile
 * coordinates.
 *
 * WHY THIS CLASS EXISTS — a real client crash, reported from a live save:
 *
 *   java.lang.ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 2
 *     at necesse.level.gameTile.TerrainSplatterTile.getSplattingTexture(TerrainSplatterTile.java:120)
 *
 * {@link SimpleTiledFloorTile#getTerrainSprite} picks its sprite cell with
 * {@code tileX % (width / 32)}. Java's % keeps the sign of the dividend, so on
 * any tile with a negative X or Y — which is most of a Necesse Surface map,
 * since worlds are centred on (0, 0) — it returns -1. TerrainSplatterTile then
 * indexes {@code splattingTextures[-1][...]} and the client dies. Because the
 * tile is persisted, the save could not be reopened afterwards.
 *
 * Vanilla never hits this: dryadfloor, willowfloor and palmfloor are the only
 * SimpleTiledFloorTile users and all three ship a 224x192 {@code _splat}
 * atlas, which sets {@code isUsingNewTerrainSplatting} and takes the other
 * branch of getSplattingTexture entirely — getTerrainSprite is dead code for
 * them. Our marble checker deliberately ships WITHOUT a _splat (an atlas would
 * randomize the cells per tile and destroy the checkerboard), which makes it
 * the only tile in the game that actually reaches the legacy path.
 *
 * The fix is the smallest one that keeps both the pattern and the save:
 * {@link Math#floorMod} instead of %. The tile's stringID, registration and
 * texture are untouched, so a marble checker floor already written into a world
 * stays exactly the tile it was and simply renders correctly from now on.
 *
 * KEEP THE PATTERN WORLD-ANCHORED: the checkerboard is chosen from absolute
 * tile coordinates on purpose, so it runs continuously across separately built
 * rooms instead of restarting at each floor patch. Do not "fix" this by adding
 * a _splat atlas.
 */
public class CheckerFloorTile extends SimpleTiledFloorTile {

    public CheckerFloorTile(String textureName, Color mapColor) {
        super(textureName, mapColor);
    }

    @Override
    public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
        int columns = Math.max(1, terrainTexture.getWidth() / 32);
        int rows = Math.max(1, terrainTexture.getHeight() / 32);
        return new Point(Math.floorMod(tileX, columns), Math.floorMod(tileY, rows));
    }
}
