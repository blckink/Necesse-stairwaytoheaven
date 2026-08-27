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
            .addLimited(40, "zephyrray", 2, 80)
            // v0.4: glass-cannon dive bird — the fast counterpart to the golem
            .addLimited(40, "dawnpiercer", 2, 80)
            // The cloud sea between the islands is not empty travelling ground
            .add(28, stairwaytoheaven.mobs.MistserpentHead.IN_MISTSEA, "mistserpent");

    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(80, "glowmoth", 5, 60)
            .addLimited(60, "dewsnail", 3, 60);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }
}
