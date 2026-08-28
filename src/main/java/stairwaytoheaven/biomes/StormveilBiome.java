package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Stormveil — dark slate islands under permanent thunderheads. Storm Wisps
 * crackle between the crystals; Zephyr Rays stray in from calmer air.
 */
public class StormveilBiome extends SkyBiome {

    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(60, "stormwisp", 4, 80)
            .addLimited(25, "zephyrray", 2, 80)
            // playtests: golems were Aurora-only and the Aurora Shoals are the
            // rarest biome, so many players never met one — they patrol the
            // Stormveil crystal fields too now
            .addLimited(25, "skystonegolem", 2, 96)
            // The cloud sea between the islands is not empty travelling ground
            .add(28, stairwaytoheaven.mobs.MistserpentHead.IN_MISTSEA, "mistserpent");

    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(100, "sparkbeetle", 4, 60)
            // The Thunderquill Fowl belongs to the storm ground: its down is
            // what the crystal fields charge. Table-spawnable because the mob
            // implements isValidSpawnLocation (livestock/SkyBreed); permanent
            // once placed, so the cap is what the flock settles at.
            .addLimited(40, stairwaytoheaven.livestock.SkyLivestock.THUNDERQUILL, 5, 80);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }
}
