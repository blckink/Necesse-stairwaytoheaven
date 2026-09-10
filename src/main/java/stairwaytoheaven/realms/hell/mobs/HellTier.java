package stairwaytoheaven.realms.hell.mobs;

import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import stairwaytoheaven.mobs.SkyMobTiers;

/**
 * Hell's rung of the ladder, in one place, so no mob in the realm carries a
 * bare number and no two of them disagree about what the realm is worth.
 *
 * <h2>Where the floor comes from</h2>
 * The derivation is written out once in {@link SkyMobTiers}. In short,
 * <b>VERIFIED [jar]</b>: {@code BiomeMissionIncursionData} scales incursion
 * enemies through two CUMULATIVE per-tier arrays and
 * {@code getHealthIncrease()} / {@code getDamageIncrease()} SUM the first
 * {@code tabletTier} entries. Summed to tier 10 that is +3.00 health and
 * +1.15 damage, i.e. x4.00 and x2.15 on the Skyreach floor of 1000 HP / 130
 * damage — which is exactly Crooked Beyond's row.
 *
 * <p><b>Hell is the tier past the end of the arrays.</b> Both arrays STOP at
 * ten entries, and vanilla then adds a flat
 * {@code undefinedHealthScalingPerTier = 0.45F} and
 * {@code undefinedDamageScalingPerTier = 0.04F} per further tier
 * ({@code BiomeMissionIncursionData}:68-69, the same two constants
 * {@link stairwaytoheaven.bosses.SkyBossLadder#UNDEFINED_HEALTH_SCALING_PER_TIER}
 * already carries). One step past Crooked is therefore <b>x4.45 health and
 * x2.19 damage</b>: 1000 x 4.45 = 4450 and 130 x 2.19 = 284.7, read as
 * <b>4450 HP / 285 damage</b>. Armour has no vanilla array at all and is
 * hand-walked, one step per band — 40 / 45 / 50 / 55 / 60 / <b>65</b>.
 *
 * <h2>The uplift</h2>
 * {@code docs/BALANCE.md} §10's ascension uplift rises outwards so the gaps
 * between neighbouring bands widen rather than close. Hell continues the
 * steps the five bands below it already walk — health 130 / 135 / 140 / 145 /
 * 155 / <b>165</b>, damage 115 / 118 / 121 / 124 / 130 / <b>136</b>,
 * aggression 125 / … / 150 / <b>155</b>. Armour keeps the one shared
 * {@link SkyMobTiers#UPLIFT_ARMOR} of x1.25, as every band does.
 *
 * <p>Health carries the largest uplift because health only lengthens a fight;
 * damage carries the smallest because damage is what kills a player. That is
 * the same reasoning every band below uses, and it is why Hell's damage step
 * (+6 points of uplift) is smaller than its health step (+10).
 *
 * <h2>The shipped row, against the rung below it</h2>
 * <pre>
 *                   HP     damage   armour   drop
 * Crooked Beyond    6200    364.0      75    x2.50
 * Hell              7342    387.6      81    x2.75
 * </pre>
 * Every column is strictly above Crooked's, which is what
 * {@code scripts/balance_check.sh} asserts rather than assumes.
 *
 * <h2>The roles</h2>
 * Applied on top of the realm row and never stacked — a mob picks the one that
 * describes it. Elite x1.4 HP; ranged x0.7 HP and x0.85 damage; fast x0.6 HP
 * and x0.8 damage. Hell's four residents are one of each, the same shape
 * Steinfeld's four have:
 *
 * <pre>
 * Infernal Clerk   standard   7342 / 387.6 / 81
 * Ash Spirit       elite     10278 / 387.6 / 81
 * Ticket Imp       ranged     5139 / 329.5 / 81
 * Boiler Hound     fast       4405 / 310.1 / 81
 * </pre>
 */
public final class HellTier {

    private HellTier() {
    }

    /**
     * Measured HP floor: the Skyreach floor of 1000 x4.45 — the summed
     * incursion-10 health curve plus one {@code undefinedHealthScalingPerTier}
     * step of 0.45.
     */
    public static final int FLOOR_HP = 4450;
    /** Measured damage floor: the Skyreach floor's 130 x2.19 = 284.7, read as 285. */
    public static final float FLOOR_DAMAGE = 285.0F;
    /** Measured armour floor: one hand-walked step over Crooked's 60. */
    public static final int FLOOR_ARMOR = 65;

    /** Hell's rung of the ascension uplift: x1.65 health, over Crooked's x1.55. */
    public static final int UPLIFT_HP = 165;
    /** Damage uplift: x1.36, over Crooked's x1.30. */
    public static final int UPLIFT_DAMAGE = 136;
    /** Aggression-range uplift: x1.55, over Crooked's x1.50. */
    public static final int UPLIFT_AGGRO = 155;

    /** Realm HP: the measured 4450 x1.65 = 7342. */
    public static final int HP = FLOOR_HP * UPLIFT_HP / 100;
    /** Realm damage: the measured 285 x1.36 = 387.6. */
    public static final float DAMAGE = FLOOR_DAMAGE * UPLIFT_DAMAGE / 100.0F;
    /** Realm armour: the measured 65 x1.25 = 81. */
    public static final int ARMOR = FLOOR_ARMOR * SkyMobTiers.UPLIFT_ARMOR / 100;
    /**
     * Realm drop value, applied to loot quantities. Crooked Beyond takes
     * vanilla's own tier-10 figure of x2.50 as-is; Hell walks one step past it,
     * exactly as its health and damage columns do.
     */
    public static final float DROP_VALUE = 2.75F;

    /**
     * A role's health as a full difficulty curve.
     *
     * <p>{@link SkyMobTiers#scaled} applies the ratios of the getter the floor
     * itself was measured from — {@code AscendedGolemMob.MAX_HEALTH}'s
     * 0.40 / 0.75 / 1.00 / 1.30 / 1.80 around CLASSIC — so the rung holds on
     * all five difficulties instead of only the middle one.
     */
    public static MaxHealthGetter health(int rolePercent) {
        return SkyMobTiers.scaled(SkyMobTiers.hp(HP, rolePercent));
    }

    /** A role's damage off the realm row. */
    public static GameDamage damage(int rolePercent) {
        return SkyMobTiers.damage(DAMAGE, rolePercent);
    }

    /** A loot quantity lifted by the realm's x2.75 drop value. */
    public static int drop(int baseAmount) {
        return SkyMobTiers.drop(baseAmount, DROP_VALUE);
    }

    /** A mob's own aggression range, lifted by the realm's x1.55; see {@link SkyMobTiers#aggro}. */
    public static int aggro(int baseRange) {
        return SkyMobTiers.aggro(baseRange, UPLIFT_AGGRO);
    }
}
