package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Aurora Shoals — the rare Skyreach biome: shallow mist banks under cold dawn
 * light, rich in Aetherium. Guarded by Skystone Golems.
 */
public class AuroraShoalsBiome extends SkyBiome {

    public static final MobSpawnTable mobs = new MobSpawnTable()
            .add(60, "skystonegolem")
            .add(40, "zephyrray");

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }
}
