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
            // --- content/arsenal ---
            // Skywatch frost machinery, still firing. Immobile, so it is
            // capped tight and weighted below the wandering hostiles: a
            // stationary shooter you cannot walk away from stacks badly.
            .addLimited(30, "rimesentry", 2, 96)
            // Strays in from the Aurora Shoals, the way the Zephyr Ray does.
            .addLimited(20, "auroraflake", 2, 80)
            // The cloud sea between the islands is not empty travelling ground
            .add(28, stairwaytoheaven.mobs.MistserpentHead.IN_MISTSEA, "mistserpent");

    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(100, "sparkbeetle", 4, 60);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }
}
