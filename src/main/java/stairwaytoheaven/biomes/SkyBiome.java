package stairwaytoheaven.biomes;

import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
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
}
