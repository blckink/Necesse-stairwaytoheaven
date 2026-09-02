package stairwaytoheaven.realms.crooked;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * The Checkerworks — the rare band of Crooked Beyond where somebody built.
 *
 * <p>Laid squares instead of ground, doors standing in the open, windows lying
 * flat in the floor, bent lanterns burning over nothing. It is the realm's
 * evidence that the place had an architect, which is what {@code WORLD_DESIGN.md}
 * §16's boss is for — and the reason that boss is deferred rather than dropped:
 * the arena it needs is this biome.
 *
 * <p>Cut out of the other two rather than tiled beside them, exactly the way the
 * Beetlefreak Hollows are cut out of the Veil. If the built ground were a third
 * of the realm it would stop reading as something that happened TO the realm.
 * {@link CrookedTerrainPainter#CHECKER_THRESHOLD} carries the measured share.
 *
 * <p><b>The hardest ground here, and deliberately.</b> All three POI kinds can
 * land on any of the three biomes, but the Checkerworks is where a player will
 * meet the Long Table, and a table nobody guards is not worth finding.
 */
public class CheckerworksBiome extends CrookedBiome {

    /**
     * The whole roster, on the one ground where all four archetypes stand
     * together.
     *
     * <p>Same lesson the Beetlefreak Hollows records: "hardest ground" cannot
     * honestly mean MORE BODIES, because the engine refuses a spawn once four
     * hostiles are inside the same eight tiles. It can only mean a worse four.
     * So the elite and the mimic both stand here, and the two ordinary bodies
     * keep the smaller share.
     *
     * <p>It is not free: <b>VERIFIED [jar]</b> {@code EntityManager
     * .spawnRandomMob} retries within the same tick — on a failed placement it
     * does {@code spawnTable = spawnTable.withoutRandomMob(randomMob)} and rolls
     * again — so a four-entry table converts ticks into spawns more often than a
     * two-entry one. The ceiling above it does not move, because the same method
     * only spawns while {@code countMobs(...) < client.getMobSpawnCap(level)}.
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(50, "crookedgolem", 2, RANGE_STANDARD)
            .addLimited(45, "doormimic", 2, RANGE_STANDARD)
            .addLimited(35, "crookedarmadillo", 2, RANGE_STANDARD)
            .addLimited(30, "rarecrookedgolem", 2, RANGE_ELITE);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /** Bridging inside the Checkerworks lays more of the same squares. */
    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        return CrookedRealm.checkerStoneTile;
    }

    /**
     * The Checkerworks' crate: the built half of the economy. Strange Fabric and
     * Reality Shard both come out of what was made rather than what grew, so
     * this is the richest of the three tables and the only one where a shard is
     * better than a long shot.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("strangefabric", 3, 8),
                ChanceLootItem.between(0.50F, "realityshard", 1, 3),
                ChanceLootItem.between(0.45F, "oddwood", 3, 8),
                ChanceLootItem.between(0.30F, "warpresin", 2, 5)
        );
    }

    /**
     * The Checkerworks' guard, and the heaviest pack in the mod.
     *
     * <p>Two elites at the anchor — the wall and the thing pretending to be
     * furniture — with the rest of the roster around them. A Long Table this
     * deep into the realm is the richest thing a player has walked to since the
     * Skyreach's aeronaut wrecks, and it is meant to read that way from across
     * the squares.
     */
    @Override
    public Guard getGuard() {
        return new Guard(
                new String[]{"rarecrookedgolem", "doormimic", "crookedgolem"},
                new String[]{"crookedgolem", "crookedarmadillo", "doormimic"},
                6, 8);
    }
}
