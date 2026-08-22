package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Stormveil — dark slate islands under permanent thunderheads. Storm Wisps
 * crackle between the crystals; Zephyr Rays stray in from calmer air.
 */
public class StormveilBiome extends SkyBiome {

    public static final MobSpawnTable mobs = new MobSpawnTable()
            .add(70, "stormwisp")
            .add(30, "zephyrray");

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }
}
