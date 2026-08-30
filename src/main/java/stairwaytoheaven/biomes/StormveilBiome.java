package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;

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

    /**
     * The Stormveil pays in shards and fulgurite; a bar of stormsteel is the rare one.
     *
     * The base table in {@link SkyBiome} is the common cargo; this adds what
     * only this biome gives, so a crate tells the player where they are.
     */
    @Override
    public LootTable getCrateLootTable(necesse.level.maps.Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("stormshard", 2, 6),
                ChanceLootItem.between(0.40F, "fulgurite", 1, 3),
                ChanceLootItem.between(0.15F, "stormsteelbar", 1, 2),
                super.getCrateLootTable(level, tileX, tileY)
        );
    }
}
