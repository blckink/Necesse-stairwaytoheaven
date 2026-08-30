package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;

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
            // --- content/arsenal ---
            // Flying artillery over the mist banks: the shoals had nothing
            // that shoots back at range. Same weight as the Dawnpiercer.
            .addLimited(40, "auroraflake", 2, 80)
            // The cloud sea between the islands is not empty travelling ground
            .add(28, stairwaytoheaven.mobs.MistserpentHead.IN_MISTSEA, "mistserpent");

    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(80, "glowmoth", 5, 60)
            .addLimited(60, "dewsnail", 3, 60)
            // The Glimmergoat is the rarest of the three sky herds because the
            // Aurora Shoals are the rarest ground; its fleece is the material
            // the Glimmerstride boots are made of. Table-spawnable because the
            // mob implements isValidSpawnLocation (livestock/SkyBreed).
            .addLimited(30, stairwaytoheaven.livestock.SkyLivestock.GLIMMERGOAT, 4, 90);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }

    /**
     * The Shoals are the richest ground in the sky, and their crates say so.
     *
     * The base table in {@link SkyBiome} is the common cargo; this adds what
     * only this biome gives, so a crate tells the player where they are.
     */
    @Override
    public LootTable getCrateLootTable(necesse.level.maps.Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("aurorapetal", 2, 5),
                ChanceLootItem.between(0.45F, "prismshard", 1, 4),
                ChanceLootItem.between(0.15F, "aetheriumbar", 1, 2),
                super.getCrateLootTable(level, tileX, tileY)
        );
    }
}
