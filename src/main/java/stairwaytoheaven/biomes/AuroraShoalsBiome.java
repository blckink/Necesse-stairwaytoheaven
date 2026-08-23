package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Aurora Shoals — the rare Skyreach biome: shallow mist banks under cold dawn
 * light, rich in Aetherium. Guarded by Skystone Golems.
 */
public class AuroraShoalsBiome extends SkyBiome {

    // rare but hard: few golems, capped tight
    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(55, "skystonegolem", 3, 96)
            .addLimited(50, "zephyrray", 2, 80);

    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(100, "glowmoth", 5, 60);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }
}
