package stairwaytoheaven.objects;

import java.awt.Color;

import necesse.engine.registries.TileRegistry;
import necesse.level.gameObject.TreeObject;
import necesse.level.maps.Level;

/**
 * A Skyreach tree that can show a frost-covered form.
 *
 * <p>Vanilla puts the snow-covered version of a tree in a second sprite
 * <em>column</em>, but {@code TreeObject.addDrawables} only reaches for it when
 * the tile underneath is {@code TileRegistry.snowID}:
 *
 * <pre>
 * if (texture.getWidth() &gt; spriteRes &amp;&amp; level.getTileID(tileX, tileY) == TileRegistry.snowID) {
 *     spriteX = 1;
 * }
 * </pre>
 *
 * <p>{@code spriteX} is computed inline with no override point, and the
 * Skyreach has no vanilla snow, so on our ground that second column would never
 * be drawn. {@code getTreeSpriteY} <em>is</em> overridable, so the frost forms
 * live in the lower half of a single-column sheet instead: rows
 * {@code 0..n-1} are the plain variants and {@code n..2n-1} the frosted ones,
 * and this picks the half from the ground the tree stands on.
 *
 * <p>The variant within a half still comes from the tile seed, so a tree keeps
 * its shape when the ground around it changes, and the engine's own random
 * mirroring still doubles the apparent variety on top.
 */
public class SkyTreeObject extends TreeObject {

    private final String[] frostTileStringIDs;
    private int[] frostTiles;

    public SkyTreeObject(String textureName, String logStringID, String saplingStringID,
                         Color mapColor, int leavesCenterWidth, int leavesMinHeight,
                         int leavesMaxHeight, String leavesTextureName,
                         String... frostTileStringIDs) {
        super(textureName, logStringID, saplingStringID, mapColor,
                leavesCenterWidth, leavesMinHeight, leavesMaxHeight, leavesTextureName);
        this.frostTileStringIDs = frostTileStringIDs;
    }

    /**
     * Resolved on first draw rather than in the constructor: object
     * registration and tile registration are separate passes, and depending on
     * one from inside the other is the kind of ordering dependency that breaks
     * the moment somebody reorders two lines in the mod's init.
     */
    private int[] frostTiles() {
        if (this.frostTiles == null) {
            int[] ids = new int[this.frostTileStringIDs.length];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = TileRegistry.getTileID(this.frostTileStringIDs[i]);
            }
            this.frostTiles = ids;
        }
        return this.frostTiles;
    }

    private boolean isFrostGround(Level level, int tileX, int tileY) {
        int tile = level.getTileID(tileX, tileY);
        for (int frost : this.frostTiles()) {
            if (tile == frost) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getTreeSpriteY(Level level, int tileX, int tileY, int spriteRes) {
        int rows = this.texture.getHeight() / spriteRes;
        int half = Math.max(1, rows / 2);
        int variant = super.getTreeSpriteY(level, tileX, tileY, spriteRes) % half;
        return this.isFrostGround(level, tileX, tileY) ? variant + half : variant;
    }
}
