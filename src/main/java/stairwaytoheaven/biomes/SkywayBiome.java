package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;

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
    // come to life. A corridor is the worst place in the layer to be crowded —
    // you cannot walk around anything on a causeway — so this is the table
    // where the endgame caps bite hardest.
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Elite. 3 -> 2 over sixteen tiles: two golems block a causeway,
            // three seal it. Weight 55 -> 45 so the masonry is still what this
            // ground is made of without being most of what walks on it.
            .addLimited(45, "skystonegolem", 2, RANGE_ELITE)
            // Fast. The passages are a corridor, and a pack hunter that uses
            // one is the reason to keep walking rather than to stop and look.
            // Still three, now over eight tiles rather than 2.5, so it is one
            // pack in a stretch of causeway instead of one per doorway.
            .addLimited(45, "galehound", 3, RANGE_STANDARD)
            // Standard.
            .addLimited(35, "zephyrray", 2, RANGE_STANDARD)
            // --- content/arsenal ---
            // Ranged. The causeways are what the sentries were set to watch,
            // so this is still their densest ground — but "densest" now means
            // two within twelve tiles instead of three within three. An
            // immobile shooter on a corridor is the one thing a player cannot
            // disengage from, and at the Skyreach's new floor a third of them
            // is not a harder fight, it is a closed road. Weight 45 -> 35 for
            // the same reason.
            .addLimited(35, "rimesentry", 2, RANGE_RANGED)
            // The cloud sea between the islands is not empty travelling ground.
            // Capped at one for the reason spelled out in DriftlandsBiome:
            // fourteen isHostile segments, and it had no cap at all.
            .add(28, mistseaSerpent(1), "mistserpent");

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

    /**
     * Freight left on the passages: worked goods, not raw ones.
     *
     * The base table in {@link SkyBiome} is the common cargo; this adds what
     * only this biome gives, so a crate tells the player where they are. The
     * amounts carry the same tier-1 incursion rate as the base table, by the
     * rule stated there.
     */
    @Override
    public LootTable getCrateLootTable(necesse.level.maps.Level level, int tileX, int tileY) {
        return new LootTable(
                // 1-3 -> 1-4 (expected 2.0 -> 2.5, +25%).
                ChanceLootItem.between(0.45F, "stormglass", 1, 4),
                // Unchanged: 1-2 -> 1-3 is +33%, past the band the rule allows.
                ChanceLootItem.between(0.30F, "skyweave", 1, 2),
                // Unchanged: the bell is a single unique piece, not a stack.
                ChanceLootItem.between(0.10F, "silverbell", 1, 1),
                super.getCrateLootTable(level, tileX, tileY)
        );
    }
}
