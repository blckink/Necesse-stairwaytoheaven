package stairwaytoheaven.realms.steinfeld.mobs;

import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import stairwaytoheaven.mobs.SkyMobTiers;

/**
 * Steinfeld's rung of the ladder, in one place, so no mob in the realm carries
 * a bare number and no two of them disagree about what the realm is worth.
 *
 * <h2>The measurement</h2>
 * The derivation is written out once in {@link SkyMobTiers} and in
 * {@code stairwaytoheaven.arsenal.RimeSentryMob}. In short, <b>VERIFIED
 * [jar]</b>: {@code BiomeMissionIncursionData} scales incursion enemies through
 * two cumulative per-tier arrays,
 * {@code healthScalingPerTier = {0.00, 0.25, 0.27, 0.29, 0.31, 0.33, 0.35,
 * 0.38, 0.40, 0.42}} and
 * {@code damageScalingPerTier = {0.00, 0.15, 0.14, 0.13, 0.12, 0.11, 0.10,
 * 0.12, 0.13, 0.15}}, and {@code getHealthIncrease()} / {@code getDamageIncrease()}
 * SUM the first {@code tabletTier} entries. Both arrays begin at {@code 0.0F},
 * so tier 1 applies nothing and tier 1 IS the raw ascended roster: 1000 HP
 * ({@code AscendedGolemMob.MAX_HEALTH} on CLASSIC), 130 damage
 * ({@code CrystalGolemMob.damage}) and armour 40.
 *
 * <p>Summed to <b>tier 5</b> that is +1.12 health and +0.54 damage, i.e.
 * <b>x2.12</b> and <b>x1.54</b>. Applied to the floor: 1000 x 2.12 = 2120 and
 * 130 x 1.54 = 200.2, which {@code docs/BALANCE.md} §5 reads as the realm row
 * <b>2100 HP / 200 damage / 50 armour, drop value x1.6</b>. Armour is the one
 * column with no incursion array, so the ladder walks it up by hand from the
 * measured 40: Eden 45, Steinfeld 50, Ghost Realm 55.
 *
 * <h2>The roles</h2>
 * Applied on top of the realm row and never stacked — a mob picks the one that
 * describes it. Elite x1.4 HP; ranged x0.7 HP and x0.85 damage; fast x0.6 HP
 * and x0.8 damage. Steinfeld's four residents are one of each, which is what
 * makes its guard packs shaped rather than numerous:
 *
 * <pre>
 * Stone Mourner   standard   2100 / 200 / 50
 * Hollow Angel    elite      2940 / 200 / 50
 * Grave Crow      ranged     1470 / 170 / 50
 * Lost Pilgrim    fast       1260 / 160 / 50
 * </pre>
 */
public final class SteinfeldTier {

    private SteinfeldTier() {
    }

    /** Realm HP: the Skyreach floor of 1000 x 2.12, the summed incursion-5 health curve. */
    public static final int HP = 2100;
    /** Realm damage: the floor's 130 x 1.54 = 200.2, read as 200. */
    public static final float DAMAGE = 200.0F;
    /** Realm armour: one hand-walked step over Eden's 45, two over the measured 40. */
    public static final int ARMOR = 50;
    /** Realm drop value, applied to loot quantities. */
    public static final float DROP_VALUE = 1.6F;

    /**
     * A role's health as a full difficulty curve.
     *
     * <p>{@link SkyMobTiers#scaled} applies the ratios of the getter the floor
     * itself was measured from — {@code AscendedGolemMob.MAX_HEALTH}'s
     * 0.40 / 0.75 / 1.00 / 1.30 / 1.80 around CLASSIC — so a rung holds on all
     * five difficulties instead of only the middle one. Steinfeld's base row
     * comes out as {@code (840, 1575, 2100, 2730, 3780)}, which is exactly the
     * line {@code docs/BALANCE.md} §6 prints for this realm.
     */
    public static MaxHealthGetter health(int rolePercent) {
        return SkyMobTiers.scaled(SkyMobTiers.hp(HP, rolePercent));
    }

    /** A role's damage off the realm row. */
    public static GameDamage damage(int rolePercent) {
        return SkyMobTiers.damage(DAMAGE, rolePercent);
    }

    /** A loot quantity lifted by the realm's x1.6 drop value. */
    public static int drop(int baseAmount) {
        return SkyMobTiers.drop(baseAmount, DROP_VALUE);
    }
}
