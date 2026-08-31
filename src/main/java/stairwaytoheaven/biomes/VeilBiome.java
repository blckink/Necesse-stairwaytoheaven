package stairwaytoheaven.biomes;

import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;
import stairwaytoheaven.SkyRegistry;

/**
 * Shared base of the Veil sub-biomes (painted per-tile, never surface islands).
 *
 * <h2>Where this layer sits</h2>
 *
 * The Veil is the rung above the Skyreach. Where the Skyreach's weakest
 * resident fights at a tier-1 incursion — which is to say unmodified, see
 * {@link SkyBiome} — the rebalance pass puts the Veil's at the ladder's middle
 * rung, and that rung lands on an exact tablet tier rather than near one:
 * <b>VERIFIED [jar]</b> {@code BiomeMissionIncursionData.healthScalingPerTier}
 * summed over {@code i < 7} is 1.80 and {@code damageScalingPerTier} is 0.75,
 * i.e. a tier-7 incursion multiplies health by 2.80 and damage by 1.75 —
 * exactly the 2800 HP and ~230 damage the pass gives this layer against the
 * Skyreach's 1000/130. The stat lines themselves live in the mob classes,
 * which are other files in the same pass; the tables here only say how many
 * of each stand on a piece of ground.
 *
 * <p>The engine facts that shape every table in this package are written out
 * once in {@link SkyBiome}, and so are the shared cap radii these tables use
 * ({@code RANGE_STANDARD}, {@code RANGE_RANGED}, {@code RANGE_ELITE}):
 * {@code addLimited}'s searchRange is in PIXELS, the engine already caps total
 * pressure at four hostiles within eight tiles and at a per-player headcount
 * above that, and a cap that binds hands its share to the rest of the table
 * instead of leaving the ground empty. All of it applies here unchanged —
 * every Veil hostile routes through {@code SkySpawnRules.daylightSpawn}, which
 * ends in the same {@code checkMaxHostilesAround(4, 8, client)}.
 *
 * <p><b>No crate table, deliberately.</b> {@code Biome.getCrateLootTable} is
 * not overridden here because nothing in the Veil places a crate: the only
 * {@code skycrate} scatter in the mod is {@code SkyTerrainPainter}'s
 * (CRATE_CHANCE / CRATE_CHANCE_BARREN), and {@code VeilTerrainPainter} has no
 * equivalent, so an override would be code that never runs. It is worth
 * knowing that the fallback is wrong if that ever changes — the inherited
 * default hands back {@code LootTablePresets.basicCrate}, which is
 * surface-tier coins, arrows and torches. The Veil's only container today is
 * the Crooked House barrel, which carries its own table in
 * {@code worldgen/CrookedHousePreset}.
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
