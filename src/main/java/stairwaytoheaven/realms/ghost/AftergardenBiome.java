package stairwaytoheaven.realms.ghost;

import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The Aftergarden — poison-green haunted grass over black cobble, crooked dead
 * trees in groves, and the realm's common ground.
 *
 * <p>This is where a garden used to be. It is also the biome the realm is named
 * after, and the one a player walks most of, so it carries the whole mix:
 * standard, elite, ranged and fast.
 */
public class AftergardenBiome extends GhostBiome {

    // Weights sum to 205. The Drifter is the ground's own dead and leads at
    // 44%; the rest is one of each role so no walk through the garden is a
    // fight of a single kind.
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Standard. Cap 3 rather than 4: the engine already refuses a
            // spawn once four hostiles stand within eight tiles, so a
            // same-kind cap of four can never bind and would only crowd the
            // other three entries out of the ground.
            .addLimited(90, "drifter", 3, RANGE_STANDARD)
            // Melee. The manor's staff, still doing the rounds outside it.
            .addLimited(45, "headlessbutler", 2, RANGE_STANDARD)
            // Ranged. Two lanterns at a time is already a crossfire.
            .addLimited(40, "lanternwidow", 2, RANGE_RANGED)
            // Fast. The hound is the reason the open garden is not a stroll,
            // and it is the only thing here that closes distance quickly.
            .addLimited(30, "soulhound", 2, RANGE_STANDARD);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        return GhostRealm.hauntedGrassTile;
    }

    /**
     * The garden's guard: a bride and her staff.
     *
     * <p>Anchors are placed once each, nearest the site centre; the rabble is
     * drawn from with replacement out to {@link GhostPressure#GUARD_RADIUS}.
     * The split is what gives a guarded place a shape — five of one thing is a
     * spawn table with extra steps.
     */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"mourningbride"},
                new String[]{"headlessbutler", "drifter", "soulhound"}, 5, 7);
    }
}
