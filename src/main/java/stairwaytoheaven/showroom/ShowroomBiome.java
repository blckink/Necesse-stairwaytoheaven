package stairwaytoheaven.showroom;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The biome painted under the showroom: nothing spawns in it.
 *
 * <p>Spawning is decided per biome, not per level (VERIFIED [jar],
 * {@code EntityManager.tickMobSpawning} / {@code ServerClient.getMobSpawnCap}):
 * the hostile cap is multiplied by {@code getSpawnCapMod} of the biome the
 * PLAYER stands in, and the table comes from the biome of the chosen spawn
 * tile. So a showroom painted with this biome has a hostile cap of zero while
 * the player stands in it, an empty table for any tile of it, and a critter cap
 * of zero. The realm debuffs that key off a realm biome (heat, Soul Exposure)
 * do not apply either, which is what an exhibition hall wants.
 */
public class ShowroomBiome extends Biome {

    private static final MobSpawnTable EMPTY = new MobSpawnTable();

    /** Set by {@code StairwayToHeavenMod.registerBiomes}. */
    public static ShowroomBiome instance;

    /** Registry ID of the showroom biome, or -1 before registration. */
    public static int biomeID() {
        return instance == null ? -1 : instance.getID();
    }

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return EMPTY;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return EMPTY;
    }

    @Override
    public float getSpawnCapMod(Level level) {
        return 0.0F;
    }

    @Override
    public float getSpawnRateMod(Level level) {
        return 0.0F;
    }

    @Override
    public boolean canRain(Level level) {
        return false;
    }
}
