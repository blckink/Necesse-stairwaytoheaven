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
            .addLimited(80, "zephyrray", 3, 80)
            // v0.4: night pack hunter of the meadows (darkness-only spawn
            // rules keep torch-lit ground safe as always)
            .addLimited(45, "galehound", 3, 80);

    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(80, "cloudlamb", 4, 60)
            .addLimited(70, "zephyrfinch", 4, 60);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }
}
