package stairwaytoheaven.biomes;

import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.biomes.MobSpawnTable;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.mobs.MistserpentHead;

/**
 * Shared base of the four Skyreach sub-biomes. These biomes are painted
 * per-tile into the Skyreach's biome layer (like vanilla cave biomes); they
 * never generate as surface islands, so they carry no generation weight and no
 * surface world-gen overrides.
 *
 * <h2>How this layer is tiered, and what a spawn table can actually do</h2>
 *
 * The Skyreach is the mod's FLOOR: the rebalance pass these tables belong to
 * sets its weakest resident to fight at the level of a tier-1 incursion, and
 * the Veil above it (see {@link VeilBiome}) to tier 7. The mob classes those
 * numbers live in are changed in the same pass but in other files — the tables
 * here only decide how many of each stand on a piece of ground, and they are
 * written for the roster the pass produces, not for the one that shipped in
 * 0.6.0.
 *
 * <p>What tier 1 means is exact rather than a guess. <b>VERIFIED [jar]</b>
 * {@code BiomeMissionIncursionData.getHealthIncrease/getDamageIncrease} sum
 * {@code healthScalingPerTier}/{@code damageScalingPerTier} over
 * {@code i < tabletTier}, and both arrays start with {@code 0.0F}: at tablet
 * tier 1 the sums are zero, so a tier-1 incursion applies NO health and NO
 * damage multiplier. Tier 1 IS the unmodified mob — which is why the floor of
 * this mod is expressed as a flat stat line rather than as a multiplier.
 *
 * <p><b>Four engine facts shape every table below, all VERIFIED [jar]:</b>
 *
 * <ol>
 * <li><b>{@code MobSpawnTable.addLimited}'s searchRange is in PIXELS, not
 *     tiles.</b> It builds its candidate box with
 *     {@code GameUtils.rangeBounds(spawnTile.x * 32 + 16, ..., searchRange)}
 *     and then filters on {@code Mob.getDistance} — both pixel-space — while
 *     {@code GameUtils.rangeTileBounds} is the one that multiplies by 32. The
 *     neighbouring API reads the other way round:
 *     {@code MobSpawnLocation.checkMaxMobsAround(max, tileRange, ...)} IS in
 *     tiles. This mod's tables used to pass 60/80/96, which are 1.9/2.5/3
 *     tiles — bubbles so small that the caps could almost never bind, which is
 *     why several of the old comments describing them as an "eight-tile
 *     radius" were wrong. Every HOSTILE cap in this package now uses the named
 *     radii below so the tile count is the thing you read. (The critter and
 *     livestock entries are outside this rebalance and still carry raw pixel
 *     values; their real bound is {@code SkyBreed}'s own
 *     {@code checkMaxMobsAround}, which is in tiles.)</li>
 * <li><b>The engine already caps total pressure at four hostiles within eight
 *     tiles</b> of the spawn point ({@code HostileMob.isValidSpawnLocation}
 *     and this mod's {@code SkySpawnRules.daylightSpawn} both end in
 *     {@code checkMaxHostilesAround(4, 8, client)}), and above that at a
 *     per-player headcount: {@code EntityManager.tickMobSpawning} only spawns
 *     while {@code countMobs(...) < client.getMobSpawnCap(level)}. So a
 *     per-kind cap is not a headcount control — the engine owns the headcount.
 *     It is a MIX control: it says how much of those four one species may be.
 *     It also means a per-kind cap of 4 at eight tiles is dead weight, because
 *     {@code checkMaxHostilesAround} measures the same eight tiles as a SQUARE
 *     ({@code GameMath.squareDistance} is {@code max(|dx|, |dy|)}) that
 *     contains the circle {@code addLimited} measures. Three is the largest
 *     cap that can still bind there.</li>
 * <li><b>A cap that binds redistributes the spawn, it does not waste it.</b>
 *     {@code MobSpawnTable.getRandomMob} calls {@code addCanSpawns} first and
 *     only ticket-weights the entries that can spawn at that tile; and if the
 *     one it picks fails placement, {@code EntityManager.spawnRandomMob} loops
 *     — {@code spawnTable = spawnTable.withoutRandomMob(randomMob)} — and
 *     rolls again from what is left. Tightening an elite's cap hands its share
 *     to the rest of the table rather than leaving the ground empty, and a
 *     table with more entries fills the player's spawn cap faster rather than
 *     raising it.</li>
 * <li><b>A spawn entry for a mob that does not implement
 *     {@code isValidSpawnLocation} is silently inert.</b>
 *     {@code MobChance.spawnMob} calls it and drops the mob when it returns
 *     false, which is {@code Mob}'s default. Everything named in these tables
 *     implements it — the sky and Veil hostiles through
 *     {@code SkySpawnRules.daylightSpawn}, the Galehound and the Mistserpent
 *     through their vanilla parents, the livestock through {@code SkyBreed}.
 *     The Cloud Lamb is the one that did not; see {@link DriftlandsBiome}.</li>
 * </ol>
 */
public abstract class SkyBiome extends Biome {

    // ---- Shared cap radii, in pixels, by archetype -------------------------
    // One definition so the policy is one line and an outlier is visible.
    // Values are pixels because that is what addLimited measures (see above).

    /** Standard and fast hostiles: at most N of them among the four the engine
     *  allows within eight tiles. Three is the largest cap that can bind here. */
    public static final int RANGE_STANDARD = 8 * 32;
    /** Ranged hostiles: twelve tiles, which is nearer the distance they fight
     *  at than the distance you meet them at. */
    public static final int RANGE_RANGED = 12 * 32;
    /** Elites: sixteen tiles, so a pair is a pair across a stretch of ground
     *  rather than a pair per doorway. */
    public static final int RANGE_ELITE = 16 * 32;

    /**
     * The Mistsea serpent's spawn rule: open cloud sea, and not next to
     * another serpent.
     *
     * The terrain half is {@link MistserpentHead#IN_MISTSEA}. The count half
     * has to be hand-written because {@code addLimited}'s overloads build
     * their own {@code CanSpawnPredicate} and there is no overload that takes
     * both a cap and a terrain test — so this is {@code addLimited}'s own body
     * ({@code streamInRegionsShape} + {@code getDistance} + count) ANDed onto
     * the terrain check, which runs first because it is the cheap one.
     *
     * <p>It counts heads, not segments: a serpent is one mob with fourteen
     * {@code HostileWormMobBody} tails, each of which sets {@code isHostile}
     * itself. Vanilla treats its own giant worms exactly this way —
     * {@code DesertBiome} and {@code SlimeCaveBiome} both cap theirs at one
     * over {@code Mob.MOB_SPAWN_AREA.maxSpawnDistance * 2} — and this uses the
     * same constant at half that reach, which is one serpent in the ring of
     * sea a player can be spawned into.
     */
    public static MobSpawnTable.CanSpawnPredicate mistseaSerpent(int maxSerpents) {
        return (level, client, spawnTile, purpose) -> {
            if (!MistserpentHead.IN_MISTSEA.canSpawn(level, client, spawnTile, purpose)) {
                return false;
            }
            // Read per call, not captured: these tables are static fields, and
            // MOB_SPAWN_AREA is a mutable static the engine (or another mod)
            // may still be setting up when they initialize.
            final int searchRange = Mob.MOB_SPAWN_AREA.maxSpawnDistance;
            final int x = spawnTile.x * 32 + 16;
            final int y = spawnTile.y * 32 + 16;
            return level.entityManager.mobs
                    .streamInRegionsShape(GameUtils.rangeBounds(x, y, searchRange), 0)
                    .filter(m -> m instanceof MistserpentHead)
                    .filter(m -> m.getDistance(x, y) <= searchRange)
                    .count() < maxSerpents;
        };
    }

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
     *
     * <h2>Why the amounts moved</h2>
     *
     * The ask was that difficulty AND worth start at the level of the first
     * incursion. The mob half of that is elsewhere in this pass; this is the
     * worth half, and the engine has an exact number for what a tier-1
     * incursion pays: <b>VERIFIED [jar]</b>
     * {@code BiomeMissionIncursionData.initModifiers} sets
     * {@code LevelModifiers.LOOT} to {@code 15.0F * tabletTier} percent, and
     * that modifier reaches crates as well as corpses —
     * {@code GameObject.getObjectDroppedItems} passes
     * {@code level.buffManager.getModifier(LevelModifiers.LOOT)} straight into
     * {@code LootTable.getNewList}. So a crate standing in a tier-1 incursion
     * pays +15%. The Skyreach is an ordinary level with no LOOT modifier on it,
     * so the +15% has to be written into the table itself.
     *
     * <p>The rule applied to every entry in this file and in the four
     * sub-biomes, so the arithmetic is checkable rather than taste: <b>raise
     * the maximum by one; if that moves the expected amount less than +10%,
     * raise the minimum too; if a single item would overshoot +30%, leave the
     * entry alone.</b> The last case is not laziness — {@code LootTable.getLootAmount}
     * multiplies the rolled amount and spends the FRACTION as a chance for one
     * more item, and a {@code between(min, max)} range cannot express "15% of
     * one bar". Entries at 1-2 keep their range for that reason, which also
     * keeps the rare end of every crate rare.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                // 3-9 -> 4-10: the max alone was only +8%, so the floor moved
                // with it (expected 6.0 -> 7.0, +17%).
                LootItem.between("skystone", 4, 10),
                // 1-4 -> 1-5 (expected 2.5 -> 3.0, +20%).
                ChanceLootItem.between(0.55F, "windsilk", 1, 5),
                // 1-3 -> 1-4 (expected 2.0 -> 2.5, +25%).
                ChanceLootItem.between(0.35F, "aetheriumore", 1, 4),
                // 2-5 -> 2-6 (expected 3.5 -> 4.0, +14%).
                ChanceLootItem.between(0.20F, "cloudberry", 2, 6),
                // Unchanged: one treat is one treat. A second would be +50% on
                // a novelty food, which the baseline rung does not buy.
                ChanceLootItem.between(0.12F, "cloudpufftreat", 1, 1)
        );
    }
}
