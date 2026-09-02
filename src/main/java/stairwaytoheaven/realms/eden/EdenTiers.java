package stairwaytoheaven.realms.eden;

import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import stairwaytoheaven.mobs.SkyMobTiers;

/**
 * The Garden of Eden's rung of the mod's enemy ladder, and the one place its
 * numbers are written down.
 *
 * <p><b>Where the row comes from.</b> {@link SkyMobTiers} derives the whole
 * ladder from vanilla's own incursion scaling and states Eden's row outright:
 * <i>"Skyreach is the floor (~tier 1); Eden ~3 (1500 / 165)"</i>, with armour
 * walked up by hand because vanilla has no armour array — 40 at the Skyreach,
 * <b>45</b> here. Drop value is <b>x1.3</b>, between the Skyreach's x1.0 floor
 * and the Veil's x1.9.
 *
 * <p><b>The measured anchors, VERIFIED [jar]</b>, quoted here so a reader does
 * not have to open three files to check one number:
 * <ul>
 * <li>{@code BiomeMissionIncursionData.healthScalingPerTier} =
 *     {@code {0.00, 0.25, 0.27, ...}} and {@code getHealthIncrease()} SUMS the
 *     first {@code tabletTier} entries, so incursion 3 is +0.52 health →
 *     x1.52. The Skyreach floor is {@code AscendedGolemMob.MAX_HEALTH}'s
 *     CLASSIC slot, <b>1000</b> ({@code MaxHealthGetter(400, 750, 1000, 1300,
 *     1800)}), and 1000 x 1.52 = 1520 → the ladder's <b>1500</b>.</li>
 * <li>{@code damageScalingPerTier} = {@code {0.00, 0.15, 0.14, ...}} sums to
 *     +0.29 at tier 3 → x1.29 on {@code CrystalGolemMob.damage}'s measured
 *     <b>130</b> = 167.7, snapped onto the ladder's five-step damage grid =
 *     <b>165</b>.</li>
 * <li>{@code lootPercentIncreasePerTier = 15.0F} multiplied by the tier is
 *     x1.45 at tier 3; the ladder holds Eden just under that at <b>x1.3</b>,
 *     the same way it holds the Veil under its raw tier-7 figure.</li>
 * </ul>
 *
 * <p><b>Why this is not in {@link SkyMobTiers} itself.</b> Three other realms
 * are being built in parallel and that file is the shared one; a realm's own
 * row lives with the realm, and the ROLE percentages, the difficulty curve
 * ({@link SkyMobTiers#scaled}) and the arithmetic helpers are still taken from
 * there so the two can never disagree about what "elite" means.
 */
public final class EdenTiers {

    private EdenTiers() {
    }

    /** Eden HP floor: Skyreach's 1000 x the summed incursion-3 health curve (1.52) → 1500. */
    public static final int EDEN_HP = 1500;

    /** Eden damage floor: 130 x the summed incursion-3 damage curve (1.29) = 167.7 → 165. */
    public static final float EDEN_DAMAGE = 165.0F;

    /** Eden armour: one hand-walked step over the Skyreach's measured 40. */
    public static final int EDEN_ARMOR = 45;

    /** Eden drop value, applied to loot quantities; raw incursion 3 is x1.45. */
    public static final float EDEN_DROP_VALUE = 1.3F;

    /** The realm's standard enemy: the floor, unmodified. */
    public static MaxHealthGetter health() {
        return SkyMobTiers.scaled(EDEN_HP);
    }

    /** {@code hp(ROLE_ELITE_HP)} = 2100, {@code hp(ROLE_FAST_HP)} = 900, etc. */
    public static MaxHealthGetter health(int rolePercent) {
        return SkyMobTiers.scaled(SkyMobTiers.hp(EDEN_HP, rolePercent));
    }

    /** Classic HP after a role percentage, for the class comment to quote. */
    public static int hp(int rolePercent) {
        return SkyMobTiers.hp(EDEN_HP, rolePercent);
    }

    /** The realm's standard damage: the floor, unmodified. */
    public static GameDamage damage() {
        return new GameDamage(EDEN_DAMAGE);
    }

    /** {@code damage(ROLE_FAST_DAMAGE)} = 132, {@code damage(ROLE_RANGED_DAMAGE)} = 140.25. */
    public static GameDamage damage(int rolePercent) {
        return SkyMobTiers.damage(EDEN_DAMAGE, rolePercent);
    }

    /** A loot quantity lifted by Eden's x1.3 drop value. */
    public static int drop(int baseAmount) {
        return SkyMobTiers.drop(baseAmount, EDEN_DROP_VALUE);
    }
}
