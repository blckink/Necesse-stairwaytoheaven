import java.awt.Point;
import java.lang.reflect.Constructor;

import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.gameTile.SimpleTiledFloorTile;
import necesse.level.gameTile.TerrainSplatterTile;

/**
 * Headless reproduction of the Marble Checker client crash, and proof of the fix.
 *
 * The dedicated-server integration test cannot catch this class of bug: the
 * crash is in TerrainSplatterTile.getSplattingTexture, which only ever runs on
 * a rendering client. This harness exercises the same arithmetic without a GL
 * context by building a GameTextureSection by hand (it is a plain bounds
 * holder, so a null GameTexture is fine) and indexing the splattingTextures
 * array exactly the way the renderer does.
 *
 * Run: scripts/tile_sprite_check.sh
 */
public class TileSpriteCheck {

    /** marblechecker.png is 64x64, so the legacy path builds a [2][2] array. */
    static final int TEX = 64;
    static final int CELLS = TEX / 32;
    static final int RANGE = 600;

    public static void main(String[] args) throws Exception {
        GameTextureSection section = new GameTextureSection(null, 0, TEX, 0, TEX);
        Object[][] splattingTextures = new Object[CELLS][CELLS];

        TerrainSplatterTile vanilla = new SimpleTiledFloorTile("marblechecker", java.awt.Color.GRAY);
        TerrainSplatterTile ours = instantiate("stairwaytoheaven.tiles.CheckerFloorTile");

        int vanillaFailures = sweep(vanilla, section, splattingTextures);
        int ourFailures = sweep(ours, section, splattingTextures);

        System.out.println("swept tile coordinates " + (-RANGE) + ".." + RANGE + " on a " + TEX + "x" + TEX
                + " texture (splattingTextures is [" + CELLS + "][" + CELLS + "])");
        System.out.println("  vanilla SimpleTiledFloorTile : " + vanillaFailures + " out-of-bounds sprite indices");
        System.out.println("  our CheckerFloorTile         : " + ourFailures + " out-of-bounds sprite indices");

        if (vanillaFailures == 0) {
            System.out.println("FAIL: the harness no longer reproduces the original crash, so it proves nothing.");
            System.exit(1);
        }
        if (ourFailures != 0) {
            System.out.println("FAIL: the mod's checker floor still indexes out of bounds and will crash clients.");
            System.exit(1);
        }
        // The pattern must still be a checkerboard anchored to world coordinates.
        Point a = ours.getTerrainSprite(section, null, -4, -4);
        Point b = ours.getTerrainSprite(section, null, -3, -4);
        Point c = ours.getTerrainSprite(section, null, 0, 0);
        if (a.equals(b) || !a.equals(c)) {
            System.out.println("FAIL: the checker pattern is no longer world-anchored ("
                    + a + " / " + b + " / " + c + ")");
            System.exit(1);
        }
        System.out.println("PASS: sprite indices stay in bounds and the checkerboard stays world-anchored.");
    }

    private static int sweep(TerrainSplatterTile tile, GameTextureSection section, Object[][] splattingTextures) {
        int failures = 0;
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                Point sprite = tile.getTerrainSprite(section, null, x, y);
                try {
                    Object unused = splattingTextures[sprite.x][sprite.y];
                } catch (ArrayIndexOutOfBoundsException e) {
                    failures++;
                }
            }
        }
        return failures;
    }

    private static TerrainSplatterTile instantiate(String className) throws Exception {
        Class<?> c = Class.forName(className);
        Constructor<?> ctor = c.getConstructor(String.class, java.awt.Color.class);
        return (TerrainSplatterTile) ctor.newInstance("marblechecker", java.awt.Color.GRAY);
    }
}
