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

    // The crystal fields keep their roster and their shares; what changed with
    // the endgame pass is that the caps are now written over a radius that can
    // hold (see SkyBiome: addLimited's searchRange is PIXELS, so the old 80/96
    // were 2.5 and 3 tiles).
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Standard, and the biome's signature. Cap 4 -> 3: four wisps was
            // never reachable anyway (the engine allows four hostiles within
            // the same eight tiles, so a cap of four can never bind — see
            // SkyBiome), and three of a tier-1-incursion mob is already the
            // whole local budget minus the room its neighbours need.
            .addLimited(60, "stormwisp", 3, RANGE_STANDARD)
            .addLimited(25, "zephyrray", 2, RANGE_STANDARD)
            // playtests: golems were Aurora-only and the Aurora Shoals are the
            // rarest biome, so many players never met one — they patrol the
            // Stormveil crystal fields too now. Elite, so the pair is measured
            // over sixteen tiles like everywhere else in the layer.
            .addLimited(25, "skystonegolem", 2, RANGE_ELITE)
            // --- content/arsenal ---
            // Ranged. Skywatch frost machinery, still firing. Immobile, so it
            // is capped tight and weighted below the wandering hostiles: a
            // stationary shooter you cannot walk away from stacks badly, and
            // at the new floor two of them twelve tiles apart is already a
            // crossfire you have to break rather than outlast.
            .addLimited(30, "rimesentry", 2, RANGE_RANGED)
            // Strays in from the Aurora Shoals, the way the Zephyr Ray does —
            // and it is the same ranged mob there, so it gets the same radius.
            .addLimited(20, "auroraflake", 2, RANGE_RANGED)
            // The cloud sea between the islands is not empty travelling ground.
            // Capped at one for the reason spelled out in DriftlandsBiome:
            // fourteen isHostile segments, and it had no cap at all.
            .add(28, mistseaSerpent(1), "mistserpent");

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
     * only this biome gives, so a crate tells the player where they are. The
     * amounts carry the same tier-1 incursion rate as the base table, by the
     * rule stated there.
     */
    @Override
    public LootTable getCrateLootTable(necesse.level.maps.Level level, int tileX, int tileY) {
        return new LootTable(
                // 2-6 -> 2-7 (expected 4.0 -> 4.5, +13%).
                LootItem.between("stormshard", 2, 7),
                // 1-3 -> 1-4 (expected 2.0 -> 2.5, +25%).
                ChanceLootItem.between(0.40F, "fulgurite", 1, 4),
                // Unchanged: a third bar would be +33% on the rarest line in
                // the table, past the band the rule allows, and the rare end
                // of a crate is supposed to stay rare.
                ChanceLootItem.between(0.15F, "stormsteelbar", 1, 2),
                super.getCrateLootTable(level, tileX, tileY)
        );
    }
}
