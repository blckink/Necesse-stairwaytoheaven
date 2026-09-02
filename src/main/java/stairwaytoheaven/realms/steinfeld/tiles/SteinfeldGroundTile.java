package stairwaytoheaven.realms.steinfeld.tiles;

import stairwaytoheaven.realms.steinfeld.SteinfeldPressure;
import stairwaytoheaven.tiles.SkyGroundTile;
import necesse.level.maps.Level;

/**
 * Shared base of Steinfeld's seven grounds.
 *
 * <p>It exists for one reason only, and it is the same reason
 * {@link SkyGroundTile} exists: a ground tile is where the engine asks "may a
 * hostile appear here?". {@code EntityManager.tickMobSpawning} picks the spawn
 * POSITION by asking each candidate tile for its ticket count and drops a tile
 * returning zero out of the draw entirely, so this override is the only place
 * {@code docs/WORLD_DESIGN.md} A4.1 — <i>"sie sollen mal geballt kommen und ein
 * Gebiet bewachen wo es loot gibt, in anderen Ecken aber nicht dauernd
 * angeflogen kommen"</i> — can be answered.
 *
 * <p>{@code SkyGroundTile}'s own answer routes to
 * {@link stairwaytoheaven.worldgen.SkyPressure}, which knows the Skyreach and
 * the Veil and hands every other level vanilla's flat 100. Steinfeld has its
 * own guarded places on its own lattice, so it needs its own field; everything
 * else — the {@code drawRandom} terrain-variant pick that keeps a splat sheet
 * from tiling visibly — is inherited unchanged.
 */
public abstract class SteinfeldGroundTile extends SkyGroundTile {

    protected SteinfeldGroundTile(String textureName) {
        super(false, textureName);
        this.canBeMined = true;
    }

    /**
     * How strongly a hostile spawn is drawn to this tile.
     *
     * See {@link SteinfeldPressure} for the numbers and where each came from.
     * Zero across most open ground, which is what makes the walk between
     * places quiet, and 600 on the ground a POI stands on, which is what makes
     * arriving at one not.
     */
    @Override
    public int getMobSpawnPositionTickets(Level level, int tileX, int tileY) {
        return SteinfeldPressure.spawnTickets(level, tileX, tileY);
    }
}
