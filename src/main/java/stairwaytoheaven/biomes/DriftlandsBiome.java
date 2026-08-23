package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Driftlands — the common Skyreach biome: silver-green isles, soft wind, home
 * of the Zephyr Ray.
 */
public class DriftlandsBiome extends SkyBiome {

    // addLimited caps local pressure: no more than N of a kind near the
    // spawn point, so a cleared, lit area STAYS calm.
    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(100, "zephyrray", 3, 80);

    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(100, "cloudlamb", 4, 60);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }
}
