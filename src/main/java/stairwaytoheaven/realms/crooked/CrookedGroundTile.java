package stairwaytoheaven.realms.crooked;

import java.awt.Color;
import java.awt.Point;

import necesse.gfx.gameTexture.GameTextureSection;
import necesse.inventory.lootTable.LootTable;
import necesse.level.gameTile.TerrainSplatterTile;
import necesse.level.maps.Level;

/**
 * Shared base of every ground Crooked Beyond is made of.
 *
 * <p>It is the realm's copy of {@link stairwaytoheaven.tiles.SkyGroundTile} and
 * exists for the same two reasons that one does — one implementation of the
 * variant-row pick, and one place to answer "may a hostile appear on this
 * tile?" — but it is a separate class rather than a subclass because the answer
 * to the second question is a different one. {@code SkyGroundTile} routes to
 * {@link stairwaytoheaven.worldgen.SkyPressure}, whose policy is keyed to the
 * Skyreach's wreck and workshop lattices; this realm has its own places worth
 * guarding and its own lattice ({@link CrookedSites}), so it routes to
 * {@link CrookedPressure}. Sharing the base would have meant one of the two
 * levels silently reading the other's map.
 *
 * <h2>Sheets</h2>
 * Checker Stone, Spiral Soil, Violet Mud and the Stripe ground draw the mod's
 * own seamless 128x128 textures ({@code crookedchecker}, {@code crookedspiral},
 * {@code crookedmud}, {@code crookedstripe}) with no {@code _splat} sibling.
 * {@code TerrainSplatterTile.generateOldTerrainSplatting} (jar 1.3.3) cuts such
 * a texture into a {@code [width/32][height/32]} cell grid, cell (i,j) being
 * texture cell (i,j), which is what lets {@link #getTerrainSprite} lay it
 * world-anchored. The Wrong-Way ground still borrows vanilla's
 * {@code ascendedvoid} by literal name; {@code docs/realms/crooked.md} and
 * {@code docs/VANILLA_ASSET_MAP.md} list what is still borrowed.
 */
public abstract class CrookedGroundTile extends TerrainSplatterTile {

    protected CrookedGroundTile(String textureName, Color mapColor) {
        super(false, textureName);
        this.mapColor = mapColor;
        this.canBeMined = true;
    }

    /**
     * With an explicit alpha mask, the way the striped ground needs.
     *
     * <p>Vanilla's {@code SpiderNestTile} — the tile the Beetlefreak artwork was
     * cut for — passes {@code "splattingmaskwide"} instead of the
     * {@code "splattingmask"} default every other terrain tile uses, and the
     * blend shapes in that sheet only line up with the wide stencil. Losing this
     * argument is a silent, wrong blend rather than an error; see
     * {@link stairwaytoheaven.tiles.BeetlefreakTile}, which found it first.
     */
    protected CrookedGroundTile(String textureName, String alphaMaskTextureName, Color mapColor) {
        super(false, textureName, alphaMaskTextureName);
        this.mapColor = mapColor;
        this.canBeMined = true;
    }

    /**
     * World-anchored, the way {@link stairwaytoheaven.tiles.CheckerFloorTile}
     * draws the marble checker: the realm's grounds are seamless 128x128
     * textures, and a cell picked from absolute tile coordinates makes the whole
     * texture run on across 4x4 tiles and on into the next copy without a seam.
     * The random row pick this replaced cut every motif larger than one tile at
     * every tile edge. {@code floorMod}, not {@code %}: negative coordinates
     * returned -1 and killed the client once already.
     */
    @Override
    public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
        int columns = Math.max(1, terrainTexture.getWidth() / 32);
        int rows = Math.max(1, terrainTexture.getHeight() / 32);
        return new Point(Math.floorMod(tileX, columns), Math.floorMod(tileY, rows));
    }

    /**
     * Nothing in Crooked Beyond is carried home in a stack.
     *
     * <p>Every ground here is registered {@code obtainable = false} (see
     * {@link CrookedRealm#registerTiles()}), the way vanilla registers
     * {@code spidernesttile} and {@code ascendedvoidtile}: it is placed by
     * worldgen and by presets, never mined into an inventory. An empty loot
     * table is what makes the mining swing agree with that, instead of leaving
     * the tile mineable-but-worthless.
     */
    @Override
    public LootTable getLootTable(Level level, int tileX, int tileY) {
        return new LootTable();
    }

    /**
     * Terrain, not floor. {@code PRIORITY_TERRAIN} is 100 and the floor band
     * starts at 300 — see {@code tools/tile_behaviour_audit.py}, which fails
     * the build if a tile declared TERRAIN answers inside the floor band.
     */
    @Override
    public int getTerrainPriority() {
        return TerrainSplatterTile.PRIORITY_TERRAIN;
    }

    /**
     * How strongly a hostile spawn is drawn to this tile — the realm's quiet.
     *
     * See {@link CrookedPressure} for the numbers and the engine behaviour they
     * rest on. Most open ground answers 0, which takes the tile out of the
     * spawn lottery entirely rather than making it merely unlikely.
     */
    @Override
    public int getMobSpawnPositionTickets(Level level, int tileX, int tileY) {
        return CrookedPressure.spawnTickets(level, tileX, tileY);
    }
}
