package stairwaytoheaven.realms.ghost;

import java.awt.Color;

import necesse.level.maps.Level;
import stairwaytoheaven.tiles.SkyGroundTile;

/**
 * One natural ground of the Ghost Realm.
 *
 * <p>Six of the realm's seven terrains are instances of this ONE class rather
 * than six subclasses, because nothing distinguishes them but four values: the
 * sheet, the map colour, the terrain priority and whether soft flora may grow
 * on them. Six near-identical files would be six places to fix the same
 * rendering bug in, which is the reason {@link SkyGroundTile} exists in the
 * first place.
 *
 * <h2>Borrowed sheets</h2>
 * The texture name is a constructor argument, not the registered string ID, so
 * a ghost ground can point at a sheet that already exists — one of the Veil's
 * or one of the game's own. {@code GameTexture.fromFile} reads a single flat
 * resource map with the mod's files merged into the game's
 * ({@code ResourceEncoder.java:75-86}), so {@code tiles/murkmoss_splat} and
 * {@code tiles/ravenfloor_splat} resolve exactly alike. Every borrow is listed
 * in {@code docs/realms/ghost.md}.
 *
 * <p>The <b>map colour is ours</b>, and that is not a recolour of anything: it
 * is the per-tile {@code mapColor} field every vanilla tile sets by hand, and
 * it is what makes the Aftergarden read as petrol, violet and poison green on
 * the world map even where the borrowed sheet came from a swamp.
 *
 * <h2>Why it overrides the spawn hook</h2>
 * {@link SkyGroundTile#getMobSpawnPositionTickets} routes to
 * {@code SkyPressure}, which deliberately answers vanilla's default 100 for
 * every level that is not the Skyreach or the Veil. Inheriting that unchanged
 * would give the Ghost Realm no A4.1 policy at all — open ground would be as
 * loud as a guarded tomb. {@link GhostPressure} is the realm's own copy.
 */
public class GhostGroundTile extends SkyGroundTile {

    private final int terrainPriority;

    /**
     * @param textureName    sheet name under {@code tiles/}, ours or the game's
     * @param mapColor       what this ground looks like on the world map
     * @param terrainPriority who splats over whom, on
     *                       {@code TerrainSplatterTile}'s own scale
     * @param organic        may soft flora (grass objects) grow on it
     */
    public GhostGroundTile(String textureName, Color mapColor, int terrainPriority, boolean organic) {
        super(false, textureName);
        this.mapColor = mapColor;
        this.terrainPriority = terrainPriority;
        this.canBeMined = true;
        this.isOrganic = organic;
    }

    @Override
    public int getTerrainPriority() {
        return this.terrainPriority;
    }

    /**
     * The Aftergarden's own quiet. See {@link GhostPressure} for the numbers
     * and the engine fact they hang on.
     */
    @Override
    public int getMobSpawnPositionTickets(Level level, int tileX, int tileY) {
        return GhostPressure.spawnTickets(level, tileX, tileY);
    }
}
