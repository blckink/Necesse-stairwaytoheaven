package stairwaytoheaven.realms.eden;

import necesse.engine.util.GameUtils;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;
import stairwaytoheaven.biomes.GuardedBiome;

/**
 * Shared base of the Garden of Eden's three zones.
 *
 * <p>These biomes are painted per tile into Eden's own biome layer by
 * {@link EdenTerrainPainter}, exactly like the Skyreach's and the Veil's; they
 * never generate as surface islands, so they are registered with
 * {@code countInStats = false} and carry no world-gen overrides.
 *
 * <p><b>How Eden is tiered.</b> {@link EdenTiers} holds the row and its
 * derivation. The tables here decide only how many of each thing stand on a
 * piece of ground, and every engine fact they are written against is stated
 * once in {@code stairwaytoheaven.biomes.SkyBiome} rather than restated here —
 * in particular that {@code MobSpawnTable.addLimited}'s searchRange is in
 * PIXELS, that the engine already caps total pressure at four hostiles within
 * eight tiles, and that a cap which binds redistributes the spawn instead of
 * wasting it.
 *
 * <p><b>One trap is repeated, because it cost this mod a mob.</b>
 * {@code MobSpawnTable.getRandomMob} (MobSpawnTable.java:131-138) filters by
 * each entry's predicate FIRST and only then draws by weight. An entry with no
 * terrain predicate therefore stays in every draw and fails later, at placement
 * — which is how the Mistserpent spent releases being rolled and dropped. Every
 * hostile in Eden implements {@code isValidSpawnLocation} through
 * {@link EdenSpawnRules}, and the two water-shy entries carry an explicit
 * terrain predicate.
 *
 * <p><b>Ambient rate.</b> A4.1's other half. Eden runs LOWER than the Skyreach
 * (0.45 against 0.55) for the reason A3.3 gives: the realm's whole thesis is
 * that beauty can be dangerous, and that only lands if the beauty gets to be
 * beautiful first. The fights are placed ({@link EdenLevel#onRegionGenerated}),
 * not rolled.
 */
public abstract class EdenBiome extends Biome implements GuardedBiome {

    /** Standard and fast hostiles: at most N within eight tiles (pixels — see SkyBiome). */
    public static final int RANGE_STANDARD = 8 * 32;
    /** Elites: sixteen tiles, so a pair is a pair across a stretch of ground. */
    public static final int RANGE_ELITE = 16 * 32;

    /**
     * Nothing in Eden's roster can stand on the lagoons, and a spawn entry that
     * rolls and then fails placement is the Mistserpent bug. This is the
     * terrain half of every land entry's predicate.
     */
    public static final MobSpawnTable.CanSpawnPredicate ON_LAND =
            (level, client, spawnTile, purpose) ->
                    !level.getTile(spawnTile.x, spawnTile.y).isLiquid;

    /**
     * "On land, and at most {@code maxMobs} of this species within
     * {@code searchRange} pixels."
     *
     * <p>Hand-written for the same reason {@code SkyBiome.mistseaSerpent} is:
     * {@code MobSpawnTable.addLimited} builds its own {@code CanSpawnPredicate}
     * and there is no overload taking BOTH a cap and a terrain test
     * (MobSpawnTable.java:50-69). So this is {@code addLimited}'s own body —
     * {@code streamInRegionsShape} + {@code getDistance} + count — ANDed onto
     * {@link #ON_LAND}, which runs first because it is the cheap one and
     * because it is the half that must never be skipped.
     *
     * <p>{@code searchRange} is in PIXELS, not tiles: {@code addLimited} builds
     * its box with {@code GameUtils.rangeBounds}, which is pixel-space, while
     * {@code GameUtils.rangeTileBounds} is the one that multiplies by 32. The
     * {@code RANGE_*} constants above are written as {@code n * 32} so the tile
     * count is the thing you read.
     */
    public static MobSpawnTable.CanSpawnPredicate onLandLimited(
            final String mobStringID, final int maxMobs, final int searchRange) {
        return (level, client, spawnTile, purpose) -> {
            if (level.getTile(spawnTile.x, spawnTile.y).isLiquid) {
                return false;
            }
            final int x = spawnTile.x * 32 + 16;
            final int y = spawnTile.y * 32 + 16;
            return level.entityManager.mobs
                    .streamInRegionsShape(GameUtils.rangeBounds(x, y, searchRange), 0)
                    .filter(m -> mobStringID.equals(m.getStringID()))
                    .filter(m -> m.getDistance(x, y) <= searchRange)
                    .count() < maxMobs;
        };
    }

    @Override
    public float getSpawnRateMod(Level level) {
        return super.getSpawnRateMod(level) * 0.45F;
    }

    @Override
    public float getSpawnCapMod(Level level) {
        return super.getSpawnCapMod(level) * 0.75F;
    }

    @Override
    public boolean canRain(Level level) {
        // Eden is uncomfortably perfect (A3.3). It does not rain in Eden.
        return false;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return new MobSpawnTable();
    }

    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        // Bridging a lagoon reclaims beach, not dirt — which is also what a
        // player would expect to be standing on at a waterline.
        return EdenRealm.paradiseSandTile;
    }

    /**
     * What an Eden cache holds, before the zone adds its own.
     *
     * <p>Amounts carry Eden's x1.3 drop value ({@link EdenTiers#EDEN_DROP_VALUE},
     * derived there from vanilla's own {@code lootPercentIncreasePerTier}), and
     * they are deliberately food and materials rather than bars: A4.2 says a
     * run should leave the player still wanting something specific, and in Eden
     * that something is Eden Copper, which is mined and never found.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("edenwood", 4, 10),
                ChanceLootItem.between(0.55F, "edenberry", 2, 6),
                ChanceLootItem.between(0.40F, "paradisecoconut", 1, 3),
                ChanceLootItem.between(0.30F, "edencopperore", 2, 5),
                ChanceLootItem.between(0.22F, "sungrape", 2, 5),
                // The one entry that is a reason to open the NEXT one: a
                // cutting is how a player takes a Knowledge Tree home, and it
                // is otherwise only carried by the Forbidden Serpent.
                ChanceLootItem.between(0.06F, "knowledgecutting", 1, 1)
        );
    }
}
