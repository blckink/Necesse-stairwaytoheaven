package stairwaytoheaven.realms.ghost;

import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The Bone Orchard — violet dirt and petrol spirit stone, planted in rows of
 * bonewood that never leafed.
 *
 * <p>It is the realm's <b>resource</b> ground: bonewood grows here and the
 * spectral ore is only in this rock, so it is the biome a player comes back to
 * on purpose. {@code WORLD_DESIGN} A4.5 is the reason that matters — scarcity
 * comes from demand, and everything the Soul Loom and the Spirit Forge want
 * starts in this orchard.
 *
 * <p>Its fight is the slow one. Open rows with long sightlines are where a
 * stationary caster and a thing that will not stop walking both work, so the
 * orchard leads with the Lantern Widow and backs her with butlers.
 */
public class BoneOrchardBiome extends GhostBiome {

    // Weights sum to 190. The ranged entry is the plurality at 39%, which is
    // what makes the orchard a different fight from the garden rather than the
    // same fight on another ground.
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Ranged, and the orchard's own. Cap 2 measured over twelve tiles
            // — the distance it actually shoots at, not the two and a half
            // tiles an unconverted pixel range would have meant.
            .addLimited(75, "lanternwidow", 2, RANGE_RANGED)
            // Melee. Still walking the rows.
            .addLimited(55, "headlessbutler", 3, RANGE_STANDARD)
            // Standard, thinner here than in the garden: a drifter has nothing
            // to drift around out on the hard ground.
            .addLimited(40, "drifter", 2, RANGE_STANDARD)
            // Elite. One bride to a stretch of orchard, over sixteen tiles.
            .addLimited(20, "mourningbride", 1, RANGE_ELITE);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        return GhostRealm.violetDirtTile;
    }

    /** The orchard's guard: lanterns at range, which is what open rows are for. */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"mourningbride", "lanternwidow"},
                new String[]{"headlessbutler", "lanternwidow", "coffincrawler"}, 5, 7);
    }
}
