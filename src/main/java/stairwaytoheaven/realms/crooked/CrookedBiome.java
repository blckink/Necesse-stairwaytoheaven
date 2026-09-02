package stairwaytoheaven.realms.crooked;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;
import stairwaytoheaven.biomes.GuardedBiome;

/**
 * Shared base of the three Crooked Beyond sub-biomes.
 *
 * <h2>Where this realm sits on the ladder</h2>
 * Incursion tier ~10, the top of {@code docs/BALANCE.md}'s table and of
 * {@link stairwaytoheaven.mobs.SkyMobTiers}: <b>4000 HP / 280 damage / 60
 * armour</b>, drop value x2.5. <b>VERIFIED [jar]</b>:
 * {@code BiomeMissionIncursionData.healthScalingPerTier} summed over the first
 * ten entries is +3.00 and {@code damageScalingPerTier} is +1.15, i.e. x4.00
 * health and x2.15 damage against the ascended floor of 1000 / 130 — which is
 * exactly this row. Loot is the flatter curve,
 * {@code lootPercentIncreasePerTier = 15.0F} times the tier = x2.50. The stat
 * lines live in the mob classes; these tables only say how many of each stand
 * on a piece of ground.
 *
 * <h2>Three engine facts every table here depends on</h2>
 * All three are written out in full in {@link stairwaytoheaven.biomes.SkyBiome}
 * and restated only as a reminder:
 * <ul>
 * <li>{@code addLimited}'s searchRange is in <b>PIXELS</b>, not tiles. The
 *     shared archetype radii below are {@code 8*32}, {@code 12*32} and
 *     {@code 16*32} for that reason; passing 60 or 96 gives a cap that cannot
 *     bind, which is a bug this repo has already shipped once.</li>
 * <li>The engine refuses a spawn outright once <b>four</b> hostiles are inside
 *     the same eight tiles, so no cap above three can bind at standard range.
 *     A cap of 6 is decoration.</li>
 * <li><b>{@code MobSpawnTable.getRandomMob} filters by entry predicate FIRST,
 *     then draws by weight</b> (MobSpawnTable.java:131-138). An entry with no
 *     terrain predicate stays in every draw and fails later, wasting the tick.
 *     Every hostile named in these tables therefore implements
 *     {@code isValidSpawnLocation} — either through
 *     {@link stairwaytoheaven.mobs.SkySpawnRules#daylightSpawn} or through the
 *     vanilla {@code HostileMob} chain it inherits. The mod lost its
 *     Mistserpent to exactly this and its Cloud Lamb to the sibling case, where
 *     the mob inherits {@code Mob}'s {@code return false} and the entry can
 *     never place at all.</li>
 * </ul>
 *
 * <h2>What this base does NOT set</h2>
 * No ambient light override. Crooked Beyond is not a cave
 * ({@link CrookedLevel} leaves {@code isCave} false), so it follows the world's
 * day/night cycle exactly as the Skyreach does — which is deliberate: black
 * stripes, a chequerboard and a neon-green sea only read as themselves in
 * light, and this realm's whole identity is what it looks like.
 */
public abstract class CrookedBiome extends Biome implements GuardedBiome {

    /** Standard and fast hostiles. Pixels — see the class comment. */
    public static final int RANGE_STANDARD = 8 * 32;
    /** Ranged hostiles: twelve tiles, nearer the distance they fight at. */
    public static final int RANGE_RANGED = 12 * 32;
    /** Elites: sixteen tiles, so a pair is a pair across a stretch of ground. */
    public static final int RANGE_ELITE = 16 * 32;

    /**
     * Ambient spawn rate, on the same policy as the mod's other two layers but
     * one step lower.
     *
     * <p>Vanilla's precedent for a place you explore rather than survive is
     * {@code SettlementRuinsBiome} at 0.3 rate / 0.5 cap and
     * {@code TempleBiome} at 0.75 / 0.75. The Skyreach and the Veil both run
     * 0.55 / 0.75. This realm runs 0.40 / 0.65, and the reason is the roster
     * rather than a change of heart about A4.1: an ambient body here is 4000 HP
     * and hits for 280, so the same rate that reads as "the world is inhabited"
     * in the sky reads as harassment down here. The pressure field
     * ({@link CrookedPressure}) already decides WHERE; this decides how often
     * the engine bothers asking.
     */
    @Override
    public float getSpawnRateMod(Level level) {
        return super.getSpawnRateMod(level) * 0.40F;
    }

    @Override
    public float getSpawnCapMod(Level level) {
        return super.getSpawnCapMod(level) * 0.65F;
    }

    @Override
    public boolean canRain(Level level) {
        return false;
    }

    /**
     * No critter table by default.
     *
     * <p>{@link StripedWasteBiome} overrides this with the Stripe Beetle, which
     * is the realm's one catchable animal. The other two grounds are
     * deliberately lifeless: the Spiral Fields grow, and the Checkerworks were
     * built, and neither is somewhere anything chooses to live.
     */
    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return new MobSpawnTable();
    }

    /**
     * Bridging the Spill reclaims the striped ground.
     *
     * <p>Same reasoning the Beetlefreak Hollows and the Outlands shipped with:
     * the wrongness is the point, and a player who bridges a channel in Crooked
     * Beyond should not be handing themselves a patch of ordinary world back.
     */
    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        return CrookedRealm.crookedStripeTile;
    }

    /**
     * What a Crooked crate holds: the realm's own five materials, at this
     * realm's drop value.
     *
     * <p>Amounts are the Skyreach's baseline crate multiplied by the tier-10
     * loot figure ({@code SkyMobTiers.CROOKED_DROP_VALUE} = 2.5), because a
     * container standing in an ordinary level carries no {@code LevelModifiers
     * .LOOT} of its own — the multiplier has to be written into the table. Each
     * of the five has a consumer somewhere in the realm's own economy (A4.5:
     * a material with no sink is clutter), and the two rarest are gated behind
     * a chance rather than a bigger range so the top of the table stays rare.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("oddwood", 4, 10),
                ChanceLootItem.between(0.55F, "warpresin", 2, 6),
                ChanceLootItem.between(0.40F, "strangefabric", 2, 5),
                ChanceLootItem.between(0.30F, "eyeseed", 1, 3),
                ChanceLootItem.between(0.18F, "realityshard", 1, 2)
        );
    }
}
