package stairwaytoheaven.mobs;

import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;

/**
 * The one place the mod's enemy statline is derived, so no mob carries a bare
 * number and no two mobs disagree about what a realm is worth.
 *
 * <p><b>THE MEASUREMENT.</b> VERIFIED [jar]: Necesse scales incursion enemies
 * from two per-tier arrays in
 * {@code necesse.level.maps.incursion.BiomeMissionIncursionData}:
 *
 * <pre>
 * damageScalingPerTier = {0.00, 0.15, 0.14, 0.13, 0.12, 0.11, 0.10, 0.12, 0.13, 0.15}
 * healthScalingPerTier = {0.00, 0.25, 0.27, 0.29, 0.31, 0.33, 0.35, 0.38, 0.40, 0.42}
 * </pre>
 *
 * {@code getDamageIncrease()} and {@code getHealthIncrease()} SUM the first
 * {@code tabletTier} entries — they do not multiply them — and hand the total
 * to {@code LevelModifiers.ENEMY_DAMAGE} / {@code ENEMY_MAX_HEALTH}. Both
 * arrays begin at {@code 0.0F}, so <b>incursion tier 1 adds nothing at all:
 * tier 1 IS the un-scaled ascended statline.</b> The running totals are
 * +0.75 damage / +1.80 health at tier 7 (x1.75 / x2.80) and +1.15 / +3.00 at
 * tier 10 (x2.15 / x4.00). Loot is a separate, flatter curve:
 * {@code lootPercentIncreasePerTier = 15.0F} multiplied by the tier outright,
 * so x1.15 at tier 1, x2.05 at tier 7 and x2.50 at tier 10.
 *
 * <p><b>THE FLOOR.</b> Tier 1 therefore measures as the ordinary ascended mob
 * itself. VERIFIED [jar]:
 * {@code AscendedGolemMob.MAX_HEALTH = new MaxHealthGetter(400, 750, 1000,
 * 1300, 1800)} — 1000 on {@code GameDifficulty.CLASSIC} — and it extends
 * {@code CrystalGolemMob}, which is {@code new GameDamage(130.0F)}, armour 40,
 * speed 20. The ascended fliers agree on the armour:
 * {@code AscendedBatMob} is {@code COLLISION_DAMAGE = new GameDamage(90.0F)}
 * with armour 40, and {@code NightSwarmBatMob} is
 * {@code new GameDamage(115.0F)} with armour 40.
 *
 * <p><b>THE LADDER.</b> The Stairway is content for a player who is already
 * through incursion 10, so its weakest enemy starts at that floor rather than
 * below it and the realms climb vanilla's own curve from there:
 *
 * <pre>
 * realm                 ~incursion    HP     damage   armour   drop value
 * Skyreach (the floor)      1        1000      130      40        x1.0
 * Ghost Realm (the Veil)    7        2800      230      55        x1.9
 * Crooked Beyond           10        4000      280      60        x2.5
 * </pre>
 *
 * The HP column is the floor times the summed health curve (x2.80 at 7, x4.00
 * at 10) and the damage column times the summed damage curve, rounded to whole
 * tens (130 x1.75 = 227.5 → 230 at 7; 130 x2.15 = 279.5 → 280 at 10). Armour
 * is not on a vanilla curve — 40 is the measured ascended value and the two
 * upper realms take one deliberate step each. The drop column takes the
 * measured tier-10 loot figure as-is and holds the Veil just under its raw
 * tier-7 x2.05.
 *
 * <p><b>These are CLASSIC numbers on every difficulty.</b> The anchor registers
 * a per-difficulty spread through {@code difficultyChanges}, but VERIFIED
 * [jar] {@code MobDifficultyChanges.forceRunChanges} only runs changes a mob
 * registered for itself — there is no implicit scaling — and ordinary vanilla
 * hostiles register none ({@code CrystalGolemMob} is a flat
 * {@code super(500)}). The mod's enemies are ordinary hostiles and are flat
 * too, so the table is read against the anchor's CLASSIC column.
 *
 * <p><b>ROLES</b> are applied on top of a realm's floor, as whole percents so
 * the arithmetic is exact rather than float-rounded: elite x1.40 HP; ranged
 * x0.70 HP / x0.85 damage; fast x0.60 HP / x0.80 damage. Armour carries no
 * role modifier — vanilla's own fast flier, {@code AscendedBatMob}, wears the
 * same 40 as the golem. A mob with no role is the realm's standard enemy and
 * takes the floor unchanged.
 */
public final class SkyMobTiers {

    private SkyMobTiers() {
    }

    /** Skyreach floor HP: {@code AscendedGolemMob.MAX_HEALTH} on CLASSIC. */
    public static final int SKYREACH_HP = 1000;
    /** Skyreach floor damage: {@code CrystalGolemMob.damage}, measured 130. */
    public static final float SKYREACH_DAMAGE = 130.0F;
    /** Skyreach floor armour: measured 40 on crystal/ascended golem and both ascended bats. */
    public static final int SKYREACH_ARMOR = 40;

    /** Ghost Realm (the Veil) HP: floor x2.80, the summed incursion-7 health curve. */
    public static final int VEIL_HP = 2800;
    /** Ghost Realm damage: floor x1.75 (the summed incursion-7 damage curve) rounded 227.5 → 230. */
    public static final float VEIL_DAMAGE = 230.0F;
    /** Ghost Realm armour: one step over the measured ascended 40. */
    public static final int VEIL_ARMOR = 55;
    /** Ghost Realm drop value, applied to loot quantities; raw incursion 7 is x2.05. */
    public static final float VEIL_DROP_VALUE = 1.9F;

    /** Elite role: x1.40 HP, damage and armour unchanged. */
    public static final int ROLE_ELITE_HP = 140;
    /** Ranged role: x0.70 HP — it never has to stand in melee. */
    public static final int ROLE_RANGED_HP = 70;
    /** Ranged role: x0.85 damage. */
    public static final int ROLE_RANGED_DAMAGE = 85;
    /** Fast role: x0.60 HP — speed is what it trades staying power for. */
    public static final int ROLE_FAST_HP = 60;
    /** Fast role: x0.80 damage. */
    public static final int ROLE_FAST_DAMAGE = 80;

    /**
     * A Classic health value as a full difficulty curve.
     *
     * <p>Vanilla scales endgame mobs with world difficulty rather than shipping
     * one number: {@code AscendedGolemMob.MAX_HEALTH = MaxHealthGetter(400,
     * 750, 1000, 1300, 1800)} — CASUAL / ADVENTURE / <b>CLASSIC</b> / HARD /
     * BRUTAL, i.e. the ratios 0.40 / 0.75 / 1.00 / 1.30 / 1.80.
     *
     * <p>This applies those same ratios to any Classic value, so a rung of the
     * ladder holds on all five difficulties instead of only the middle one.
     * Apply it in the constructor via
     * {@code this.difficultyChanges.setMaxHealth(...)} —
     * {@code MobDifficultyChanges} throws if it is touched after {@code init()}.
     */
    public static MaxHealthGetter scaled(int classicHealth) {
        return new MaxHealthGetter(
                Math.round(classicHealth * 0.40F),
                Math.round(classicHealth * 0.75F),
                classicHealth,
                Math.round(classicHealth * 1.30F),
                Math.round(classicHealth * 1.80F));
    }

    /** A realm's HP floor with a role percentage applied, e.g. {@code hp(SKYREACH_HP, ROLE_ELITE_HP)} = 1400. */
    public static int hp(int realmHealth, int rolePercent) {
        return realmHealth * rolePercent / 100;
    }

    /** A realm's damage floor with a role percentage applied, e.g. {@code damage(SKYREACH_DAMAGE, ROLE_FAST_DAMAGE)} = 104. */
    public static GameDamage damage(float realmDamage, int rolePercent) {
        return new GameDamage(realmDamage * (float) rolePercent / 100.0F);
    }

    /**
     * A loot quantity lifted by a realm's drop value, e.g.
     * {@code drop(2, VEIL_DROP_VALUE)} = 4. Rounded rather than truncated so a
     * 1-item roll still moves; the Skyreach's own x1.0 is the floor and
     * multiplies nothing, which is why its tables are written plainly.
     */
    public static int drop(int baseAmount, float dropValue) {
        return Math.round((float) baseAmount * dropValue);
    }
}
