package stairwaytoheaven.realms.crooked;

import java.awt.Color;
import java.awt.Point;

import necesse.engine.util.GameRandom;
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
 * <h2>Borrowed sheets</h2>
 * Four of the five grounds below draw a VANILLA terrain sheet by literal name.
 * {@code TerrainSplatterTile.generateSplattingTextures} (jar 1.3.2, line 154)
 * asks for {@code tiles/<name>_splat} and falls back to {@code tiles/<name>},
 * and {@code GameTexture.fromFile} resolves both out of ONE flat resource map
 * with the mod's own files merged into it (ResourceEncoder.java:75-86) — the
 * same mechanism that lets {@code livestock/SkyPelt} read {@code items/milk}.
 * So a mod tile can wear the game's own art with no PNG of ours, which is what
 * {@code docs/WORLD_DESIGN.md} A4.3 asks for: a realm built out of borrowed
 * assets and fully populated beats a realm with bespoke art and nothing in it.
 * Every borrowed sheet is listed in {@code docs/realms/crooked.md} and in
 * {@code docs/VANILLA_ASSET_MAP.md} so the replacement pass can find them.
 */
public abstract class CrookedGroundTile extends TerrainSplatterTile {

    private final GameRandom drawRandom;

    protected CrookedGroundTile(String textureName, Color mapColor) {
        super(false, textureName);
        this.mapColor = mapColor;
        this.canBeMined = true;
        this.drawRandom = new GameRandom();
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
        this.drawRandom = new GameRandom();
    }

    /**
     * Vanilla's {@code RockTile}/{@code MudTile} idiom: one variant row per
     * tile, chosen from the tile position so the ground is stable across
     * save/load and identical on every client.
     */
    @Override
    public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
        int tile;
        synchronized (this.drawRandom) {
            tile = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(terrainTexture.getHeight() / 32);
        }
        return new Point(0, tile);
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
