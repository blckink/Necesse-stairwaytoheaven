package stairwaytoheaven.biomes;

import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.biomes.MobSpawnTable;
import stairwaytoheaven.SkyRegistry;

/**
 * Shared base of the three Skyreach sub-biomes. These biomes are painted
 * per-tile into the Skyreach's biome layer (like vanilla cave biomes); they
 * never generate as surface islands, so they carry no generation weight and no
 * surface world-gen overrides.
 */
public abstract class SkyBiome extends Biome {

    @Override
    public boolean canRain(Level level) {
        // Above the cloud ceiling; storms arrive with the v0.2 weather events.
        return false;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return new MobSpawnTable();
    }

    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        // Placing tiles over the Mistsea reclaims Cloudturf, not dirt.
        return SkyRegistry.cloudturfTile;
    }

    /**
     * What a salvage crate holds up here.
     *
     * Vanilla's whole exploration loop is containers: {@code RandomCrateObject}
     * asks {@code Level.getCrateLootTable}, which asks the biome. Until now the
     * Skyreach never registered a crate at all, so the sky had nothing to open
     * — the player's own report was that there is nothing to find, unlike
     * vanilla where "es gibt immer mal wieder Kisten".
     *
     * This is the common cargo every sky crate can hold. Each sub-biome adds
     * what only IT gives, so opening a crate tells you where you are.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("skystone", 3, 9),
                ChanceLootItem.between(0.55F, "windsilk", 1, 4),
                ChanceLootItem.between(0.35F, "aetheriumore", 1, 3),
                ChanceLootItem.between(0.20F, "cloudberry", 2, 5),
                ChanceLootItem.between(0.12F, "cloudpufftreat", 1, 1)
        );
    }
}
