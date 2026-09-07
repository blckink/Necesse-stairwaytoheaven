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

    // ===== The measured floor =====
    // These are the numbers §5 of docs/BALANCE.md derives from vanilla, and
    // they do not move. They are kept as their own constants rather than folded
    // into the shipped rows below so the derivation stays checkable: every
    // shipped value is a floor times a percentage, and both halves are readable
    // out of the built jar.

    /** Skyreach floor HP: {@code AscendedGolemMob.MAX_HEALTH} on CLASSIC. */
    public static final int FLOOR_SKYREACH_HP = 1000;
    /** Skyreach floor damage: {@code CrystalGolemMob.damage}, measured 130. */
    public static final float FLOOR_SKYREACH_DAMAGE = 130.0F;
    /** Skyreach floor armour: measured 40 on crystal/ascended golem and both ascended bats. */
    public static final int FLOOR_SKYREACH_ARMOR = 40;

    /** Ghost Realm (the Veil) floor HP: Skyreach x2.80, the summed incursion-7 health curve. */
    public static final int FLOOR_VEIL_HP = 2800;
    /** Veil floor damage: Skyreach x1.75 (summed incursion-7 damage curve) rounded 227.5 → 230. */
    public static final float FLOOR_VEIL_DAMAGE = 230.0F;
    /** Veil floor armour: one hand-walked step over the measured ascended 40. */
    public static final int FLOOR_VEIL_ARMOR = 55;

    /** Crooked Beyond floor HP: Skyreach x4.00, the summed incursion-10 health curve. */
    public static final int FLOOR_CROOKED_HP = 4000;
    /** Crooked floor damage: Skyreach x2.15, the summed incursion-10 damage curve → 280. */
    public static final float FLOOR_CROOKED_DAMAGE = 280.0F;
    /** Crooked floor armour: two hand-walked steps above the measured ascended 40. */
    public static final int FLOOR_CROOKED_ARMOR = 60;

    // ===== The ascension uplift =====
    // docs/BALANCE.md §10. The player is through incursion 10 AND through the
    // mod's own first pass, so the floor above is no longer a challenge — it is
    // a starting line. Every band is lifted by its own percentage, and the
    // percentages RISE outwards, so the gap between two neighbouring realms
    // grows instead of shrinking. That is the whole reason there is one knob per
    // band rather than one global multiplier: a global one preserves the old
    // curve exactly, and the old curve is what was too flat at the top.
    //
    // Health carries the largest uplift because health only lengthens a fight.
    // Damage carries the smallest because damage is what kills a player, and a
    // 1.5x hit on a 280-damage mob would not be harder, it would be a different
    // game. Armour is one rule for every band ({@link #UPLIFT_ARMOR}) since it
    // was never on a vanilla curve to begin with.
    //
    // All arithmetic below is integer, so every shipped value is exactly
    // reproducible: floor * percent / 100. scripts/balance_check.sh recomputes
    // it from the jar rather than trusting the comment.

    /** Skyreach health uplift: x1.30. */
    public static final int UPLIFT_SKYREACH_HP = 130;
    /** Skyreach damage uplift: x1.15. */
    public static final int UPLIFT_SKYREACH_DAMAGE = 115;
    /** Skyreach aggression-range uplift: x1.25. */
    public static final int UPLIFT_SKYREACH_AGGRO = 125;

    /** Veil / Ghost Realm health uplift: x1.45. */
    public static final int UPLIFT_VEIL_HP = 145;
    /** Veil / Ghost Realm damage uplift: x1.24. */
    public static final int UPLIFT_VEIL_DAMAGE = 124;
    /** Veil / Ghost Realm aggression-range uplift: x1.40. */
    public static final int UPLIFT_VEIL_AGGRO = 140;

    /** Crooked Beyond health uplift: x1.55, the steepest rung. */
    public static final int UPLIFT_CROOKED_HP = 155;
    /** Crooked Beyond damage uplift: x1.30. */
    public static final int UPLIFT_CROOKED_DAMAGE = 130;
    /** Crooked Beyond aggression-range uplift: x1.50. */
    public static final int UPLIFT_CROOKED_AGGRO = 150;

    /**
     * Armour uplift, x1.25, the same in every band.
     *
     * <p>Armour is the one column §5 admits has no vanilla array behind it — it
     * was walked up by hand, 40 / 45 / 50 / 55 / 60. Re-spacing it by a single
     * factor keeps that hand-walked shape instead of inventing a second ladder:
     * 50 / 56 / 62 / 68 / 75, still five even steps, still under nothing
     * vanilla ships (the rolled-up {@code CrystalArmadillo} is 60).
     */
    public static final int UPLIFT_ARMOR = 125;

    // ===== The shipped rows =====

    /** Skyreach HP: the measured 1000 x1.30 = 1300. */
    public static final int SKYREACH_HP = FLOOR_SKYREACH_HP * UPLIFT_SKYREACH_HP / 100;
    /** Skyreach damage: the measured 130 x1.15 = 149.5. */
    public static final float SKYREACH_DAMAGE =
            FLOOR_SKYREACH_DAMAGE * UPLIFT_SKYREACH_DAMAGE / 100.0F;
    /** Skyreach armour: the measured 40 x1.25 = 50. */
    public static final int SKYREACH_ARMOR = FLOOR_SKYREACH_ARMOR * UPLIFT_ARMOR / 100;

    /** Ghost Realm (the Veil) HP: the floor's 2800 x1.45 = 4060. */
    public static final int VEIL_HP = FLOOR_VEIL_HP * UPLIFT_VEIL_HP / 100;
    /** Ghost Realm damage: the floor's 230 x1.24 = 285.2. */
    public static final float VEIL_DAMAGE = FLOOR_VEIL_DAMAGE * UPLIFT_VEIL_DAMAGE / 100.0F;
    /** Ghost Realm armour: the floor's 55 x1.25 = 68. */
    public static final int VEIL_ARMOR = FLOOR_VEIL_ARMOR * UPLIFT_ARMOR / 100;
    /** Ghost Realm drop value, applied to loot quantities; raw incursion 7 is x2.05. */
    public static final float VEIL_DROP_VALUE = 1.9F;

    /** Crooked Beyond HP: the floor's 4000 x1.55 = 6200. */
    public static final int CROOKED_HP = FLOOR_CROOKED_HP * UPLIFT_CROOKED_HP / 100;
    /** Crooked Beyond damage: the floor's 280 x1.30 = 364. */
    public static final float CROOKED_DAMAGE =
            FLOOR_CROOKED_DAMAGE * UPLIFT_CROOKED_DAMAGE / 100.0F;
    /** Crooked Beyond armour: the floor's 60 x1.25 = 75. */
    public static final int CROOKED_ARMOR = FLOOR_CROOKED_ARMOR * UPLIFT_ARMOR / 100;
    /** Crooked Beyond drop value: vanilla incursion tier 10 raises loot to x2.50. */
    public static final float CROOKED_DROP_VALUE = 2.5F;

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

    /** A realm's damage floor with a role percentage applied, e.g. {@code damage(SKYREACH_DAMAGE, ROLE_FAST_DAMAGE)} = 119.6. */
    public static GameDamage damage(float realmDamage, int rolePercent) {
        return new GameDamage(realmDamage * (float) rolePercent / 100.0F);
    }

    /**
     * A mob's own aggression range, lifted by its band's aggro uplift.
     *
     * <p><b>What "aggression range" is here, VERIFIED [jar].</b> Every chaser
     * tree in the game takes it as its second constructor argument and hands it
     * straight to the chaser node: {@code CollisionPlayerChaserWandererAI(
     * Supplier, int range, GameDamage, int knockback, int wanderFrequency)}
     * passes argument 2 into {@code CollisionPlayerChaserAI(int, GameDamage,
     * int)}, {@code PlayerChaserWandererAI(Supplier, int range, int knockback,
     * int wanderFrequency, boolean, boolean)} does the same, and
     * {@code CollisionShooterPlayerChaserWandererAI} passes argument 2 into the
     * chaser and its separate argument 7 into the SHOOTING node — so on a
     * shooter it is argument 2, not 7, that decides when the mob starts caring.
     * Read off the bytecode of all three with {@code javap -c}.
     *
     * <p><b>Why a per-mob base rather than one number per realm.</b> The ranges
     * a mob ships with are not noise: 384 on the slow golem against 512 on the
     * hounds is the difference between something you can walk around and
     * something that commits. Multiplying each mob's own range keeps that
     * shape and still moves the whole band, where a single per-realm range
     * would flatten every mob in the realm into the same reach.
     *
     * @param baseRange the range the mob had before the uplift, in pixels
     *                  (32 per tile).
     * @param upliftPercent one of the {@code UPLIFT_*_AGGRO} constants.
     */
    public static int aggro(int baseRange, int upliftPercent) {
        return baseRange * upliftPercent / 100;
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
