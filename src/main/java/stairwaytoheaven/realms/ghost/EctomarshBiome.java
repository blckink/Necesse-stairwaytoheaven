package stairwaytoheaven.realms.ghost;

import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The Ectomarsh — ghost moss and graveyard soil half under the ectoplasm, and
 * the only ground in the realm that gives off its own light.
 *
 * <p>It is close country: short sightlines, water in the way, and the biome
 * where the things that ambush live. So it leads with the Coffin Crawler, which
 * sits still until you are beside it, and the Soul Hound, which does not give
 * you the distance a caster would need.
 */
public class EctomarshBiome extends GhostBiome {

    // Weights sum to 200. Nothing ranged, on purpose: a caster in a marsh
    // spends its life shooting at reeds, and the two ambushers are what the
    // ground is FOR.
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Standard, and at home here — the marsh is where a drifter drifts.
            .addLimited(80, "drifter", 3, RANGE_STANDARD)
            // Fast. Short sightlines are exactly where speed hurts.
            .addLimited(50, "soulhound", 3, RANGE_STANDARD)
            // The ambusher. It disguises itself and waits, so a low weight is
            // not a rare enemy, it is a rare SURPRISE — and the cap of 2 keeps
            // the surprise from becoming a minefield.
            .addLimited(45, "coffincrawler", 2, RANGE_STANDARD)
            // Elite. One bride, drowned in her own garden.
            .addLimited(25, "mourningbride", 1, RANGE_ELITE);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        return GhostRealm.ghostMossTile;
    }

    /** The marsh's guard: things that were already there when you arrived. */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"mourningbride", "coffincrawler"},
                new String[]{"drifter", "soulhound", "possessedchair"}, 5, 7);
    }
}
