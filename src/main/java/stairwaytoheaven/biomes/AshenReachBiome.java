package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Ashen Reach — the Veil's grey dune waste, home of the future Ashwyrm. For
 * now only stray shades wander in from the fen.
 */
public class AshenReachBiome extends VeilBiome {

    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(100, "gloomshade", 2, 80);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }
}
