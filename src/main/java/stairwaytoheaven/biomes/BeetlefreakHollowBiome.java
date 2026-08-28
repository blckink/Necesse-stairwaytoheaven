package stairwaytoheaven.biomes;

import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;
import stairwaytoheaven.SkyRegistry;

/**
 * Beetlefreak Hollows — the Veil's rare wrong place.
 *
 * The other two Veil sub-biomes are landscape; this one is a symptom. It is
 * cut out of them by its own noise band rather than tiling with them, so it
 * always reads as something that happened TO the fen rather than a region of
 * it: striped ground, crooked masonry, and the shades that gather around it.
 *
 * Deliberately the densest shade population in the layer. The Hollows is where
 * the Crooked House stands, and a house nobody guards is not worth finding.
 */
public class BeetlefreakHollowBiome extends VeilBiome {

    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(100, "gloomshade", 6, 60);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /**
     * Bridging inside the Hollows reclaims the striped ground, not the fen's
     * moss — the wrongness is the point, and a player who bridges a channel
     * here should not be handing themselves a patch of normal marsh.
     */
    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        return SkyRegistry.beetlefreakTile;
    }
}
