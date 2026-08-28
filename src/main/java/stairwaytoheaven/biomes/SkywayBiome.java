package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Skyway Passages — pale cloudstone paving rimmed in gold, laid down by whoever
 * built the Skywatch and left behind when they went. The one Skyreach biome
 * that was made rather than grown: Sky Seraphs stand along its causeways, and
 * Cloudmarble balustrades run beside them.
 *
 * <p>It sits directly above Stormveil in the biome field, which is why the Sky
 * Seraph wears its frost form on both grounds: the passages are the cold edge
 * of the sky, and the two biomes border each other.
 */
public class SkywayBiome extends SkyBiome {

    // Built ground is patrolled ground: the golems are the passages' masonry
    // come to life, and the same addLimited cap the other tables use keeps a
    // cleared, lit stretch of causeway clear.
    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(55, "skystonegolem", 3, 96)
            // The passages are a corridor, and a pack hunter that uses one is
            // the reason to keep walking rather than to stop and look.
            .addLimited(45, "galehound", 3, 80)
            .addLimited(35, "zephyrray", 2, 80)
            // --- content/arsenal ---
            // The causeways are what the sentries were set to watch, so this
            // is their densest ground - still capped at 3, still immobile.
            .addLimited(45, "rimesentry", 3, 96)
            // The cloud sea between the islands is not empty travelling ground
            .add(28, stairwaytoheaven.mobs.MistserpentHead.IN_MISTSEA, "mistserpent");

    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(90, "zephyrfinch", 4, 60)
            .addLimited(50, "glowmoth", 3, 60);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }
}
