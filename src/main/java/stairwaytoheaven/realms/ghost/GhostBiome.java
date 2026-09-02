package stairwaytoheaven.realms.ghost;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;
import stairwaytoheaven.biomes.GuardedBiome;

/**
 * Shared base of the three Aftergarden sub-biomes (painted per tile by
 * {@link GhostTerrainPainter}, never generated as surface islands).
 *
 * <h2>Where this realm sits</h2>
 * Incursion tier 7: <b>2800 HP / 230 damage / 55 armour</b>, drop value x1.9
 * ({@code docs/BALANCE.md} §5). <b>VERIFIED [jar]</b>
 * {@code BiomeMissionIncursionData.healthScalingPerTier} summed over
 * {@code i < 7} is 1.80 and {@code damageScalingPerTier} is 0.75, so a tier-7
 * incursion multiplies health by 2.80 and damage by 1.75 against the mod's
 * measured floor of 1000/130 — 2800 and 227.5, taken as 230. The stat lines
 * live in the mob classes; the tables here only say how many of each stand on a
 * piece of ground.
 *
 * <h2>The engine facts every table below is written against</h2>
 * All <b>VERIFIED [jar]</b> and written out at length in
 * {@code stairwaytoheaven.biomes.SkyBiome}:
 * <ul>
 * <li>{@code MobSpawnTable.addLimited}'s searchRange argument is in
 *     <b>PIXELS</b>, not tiles. The named radii below exist so nobody writes
 *     "80" again meaning eighty tiles and getting two and a half.</li>
 * <li>The engine refuses a hostile spawn outright once four hostiles are
 *     already within eight tiles ({@code checkMaxHostilesAround(4, 8, client)},
 *     which every ghost hostile reaches through
 *     {@code SkySpawnRules.daylightSpawn}), so a same-kind cap of four can
 *     never be the thing that binds.</li>
 * <li>{@code MobSpawnTable.getRandomMob} filters by each entry's predicate
 *     FIRST and only then draws by weight (MobSpawnTable.java:131-138). An
 *     entry that passes the filter and then fails elsewhere still consumed the
 *     draw. That bug cost this mod its Mistserpent, and it is why nothing in
 *     these tables carries a terrain predicate it cannot honour: every entry
 *     here is a land mob and every table is only ever read on land.</li>
 * </ul>
 *
 * <h2>Ambient rate</h2>
 * The same policy the sky and the Veil use, and for the same reason: A4.1 is
 * about WHERE the pressure is, not how much of it exists. Vanilla's own
 * precedent is {@code SettlementRuinsBiome} at 0.3/0.5 and {@code TempleBiome}
 * at 0.75/0.75.
 */
public abstract class GhostBiome extends Biome implements GuardedBiome {

    /** Eight tiles, in the pixels {@code addLimited} actually counts. */
    public static final int RANGE_STANDARD = 256;
    /** Twelve tiles — the distance a caster actually fights at. */
    public static final int RANGE_RANGED = 384;
    /** Sixteen tiles: an elite's ground is bigger than a footsoldier's. */
    public static final int RANGE_ELITE = 512;

    @Override
    public boolean canRain(Level level) {
        return false;
    }

    /**
     * No critters. The Ghost animals of {@code WORLD_DESIGN} §12 — Spirit
     * Sheep, Grave Chicken, Ecto Slug, Ghost Koi, Soul Moth — are a separate
     * job and are NOT built in this pass; an empty table is honest about that,
     * where an entry naming a mob that does not exist would be a silent
     * failure. Note also that livestock could never ride a critter table
     * anyway: {@code Mob.isValidSpawnLocation} returns false and no husbandry
     * class overrides it, which is the bug that ate this mod's Cloud Lambs.
     */
    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return new MobSpawnTable();
    }

    /**
     * Bridging the ectoplasm reclaims the biome's own ground rather than dirt.
     * Each sub-biome answers with what it is made of.
     */
    @Override
    public abstract GameTile getUnderLiquidTile(Level level, int tileX, int tileY);

    @Override
    public float getSpawnRateMod(Level level) {
        return super.getSpawnRateMod(level) * 0.55F;
    }

    @Override
    public float getSpawnCapMod(Level level) {
        return super.getSpawnCapMod(level) * 0.75F;
    }

    /**
     * What is in a grave-urn, and in the barrels the realm's three POIs stand
     * over.
     *
     * <p>The inherited default is {@code LootTablePresets.basicCrate} —
     * surface-tier coins, arrows and torches — which would be wrong by five
     * tiers here, and the Veil's own note explains why leaving it is a trap
     * worth closing before it fires. Quantities carry the realm's x1.9 drop
     * multiplier ({@code docs/BALANCE.md} §5).
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("ectoplasm", 4, 9),
                ChanceLootItem.between(0.55F, "bonewood", 4, 10),
                ChanceLootItem.between(0.45F, "soulthread", 2, 5),
                ChanceLootItem.between(0.30F, "spectralore", 2, 5),
                ChanceLootItem.between(0.18F, "spiritsteelbar", 1, 2),
                ChanceLootItem.between(0.12F, "bone", 3, 8));
    }
}
