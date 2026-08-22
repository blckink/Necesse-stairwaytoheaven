package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Driftlands — the common Skyreach biome: silver-green isles, soft wind, home
 * of the Zephyr Ray.
 */
public class DriftlandsBiome extends SkyBiome {

    public static final MobSpawnTable mobs = new MobSpawnTable()
            .add(100, "zephyrray");

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }
}
