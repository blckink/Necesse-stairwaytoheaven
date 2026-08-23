package stairwaytoheaven.biomes;

import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;
import stairwaytoheaven.SkyRegistry;

/**
 * Shared base of the Veil sub-biomes (painted per-tile, never surface islands).
 */
public abstract class VeilBiome extends Biome {

    @Override
    public boolean canRain(Level level) {
        return false;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return new MobSpawnTable();
    }

    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        // Bridging the murkwater reclaims moss, not dirt.
        return SkyRegistry.murkmossTile;
    }
}
