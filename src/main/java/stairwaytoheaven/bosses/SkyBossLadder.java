package stairwaytoheaven.bosses;

import stairwaytoheaven.worldgen.RealmDepth;

/**
 * Which boss belongs to which realm, and how hard it is.
 *
 * <p>This is {@code docs/FOGKEY_AND_BOSSPORTALS.md} §B4 made executable, and it
 * is the ONLY place those numbers live. {@link BossPortalObjectEntity} asks it
 * what to spawn; {@link BossScaling} asks it how hard; nothing else decides
 * either. A second copy of this table is how a portal ends up summoning a boss
 * the design never picked, at a tier nobody wrote down.
 *
 * <h2>Where every number comes from</h2>
 *
 * <p><b>The bosses.</b> Each id is the {@code bossMobStringID} of one vanilla
 * {@code IncursionBiome} — the mob that biome's incursion ends on. Nothing is
 * invented: the mod reuses the game's own endgame roster because §B4's floor is
 * the player's own <i>"mindestens Niveau der 1. Incursion"</i>. Each row names
 * its incursion biome class, so the claim is checkable.
 *
 * <p><b>The base health.</b> The CLASSIC (third) slot of that boss's
 * {@code MaxHealthGetter} — the world difficulty this mod balances against.
 * {@code MaxHealthGetter(casual, adventure, CLASSIC, hard, brutal)}
 * (MaxHealthGetter.java:18). Two of them, the Cryo Queen and the Pest Warden,
 * carry a SECOND, larger table for incursions; the constructor installs the
 * base one and {@code init()} only swaps in the incursion table when
 * {@code getLevel() instanceof IncursionLevel} (CryoQueenMob.java:139, :256).
 * The sky plane is not one, so the base table is what a portal-spawned boss
 * actually wears, and it is the number quoted here.
 *
 * <p><b>The tier multipliers.</b> Vanilla's own incursion curve, copied
 * verbatim from {@code BiomeMissionIncursionData} (:66-67) rather than
 * transcribed as products, so the ladder cannot drift from the game's. Both
 * arrays are CUMULATIVE and both start at {@code 0.0F} — tier 1 applies no
 * multiplier at all. Summing them reproduces §B4's table exactly:
 *
 * <pre>
 *   tier   xHP    xdamage
 *     8    3.18     1.87
 *     9    3.58     2.00
 *    10    4.00     2.15
 * </pre>
 *
 * <p><b>VERIFIED [jar]</b> for every line above, against the decompiled 1.3.2
 * sources.
 *
 * <h2>Hell has no portal</h2>
 * §B4 reserves {@code mutanthydra} (Scrapyard, 80 000 CLASSIC) for Hell and
 * gives it no tier, because Hell has no painter yet
 * ({@code docs/PLAN_ONE_PLANE.md}, "two known holes"). {@link #forRealm} returns
 * {@code null} there, and {@link BossPortalObject} registers no Hell portal, so
 * the reservation costs nothing until Hell exists.
 */
public final class SkyBossLadder {

    /**
     * Vanilla's cumulative per-tier damage scaling.
     * <b>VERIFIED [jar]</b> {@code BiomeMissionIncursionData.java:66}.
     */
    public static final float[] DAMAGE_SCALING_PER_TIER = {
            0.0F, 0.15F, 0.14F, 0.13F, 0.12F, 0.11F, 0.1F, 0.12F, 0.13F, 0.15F,
    };

    /**
     * Vanilla's cumulative per-tier health scaling.
     * <b>VERIFIED [jar]</b> {@code BiomeMissionIncursionData.java:67}.
     */
    public static final float[] HEALTH_SCALING_PER_TIER = {
            0.0F, 0.25F, 0.27F, 0.29F, 0.31F, 0.33F, 0.35F, 0.38F, 0.4F, 0.42F,
    };

    /**
     * What vanilla adds per tier past the end of its own arrays.
     * <b>VERIFIED [jar]</b> {@code BiomeMissionIncursionData.java:68-69}. No
     * row uses a tier above 10 today; they are here so the arithmetic stays
     * vanilla's the day one does.
     */
    public static final float UNDEFINED_DAMAGE_SCALING_PER_TIER = 0.04F;
    public static final float UNDEFINED_HEALTH_SCALING_PER_TIER = 0.45F;

    private SkyBossLadder() {
    }

    /**
     * The health multiplier at a tablet tier, exactly as
     * {@code BiomeMissionIncursionData.getHealthIncrease} (:175-185) computes
     * it — except that vanilla returns the INCREASE as a percentage and this
     * returns the multiplier, because that is what a buff modifier wants.
     *
     * <p>Tier 8 is 3.18, tier 9 is 3.58, tier 10 is 4.00 — §B4's own column.
     */
    public static float healthMultiplier(int tier) {
        float increase = 0.0F;
        for (int i = 0; i < tier && i < HEALTH_SCALING_PER_TIER.length; i++) {
            increase += HEALTH_SCALING_PER_TIER[i];
        }
        if (tier > HEALTH_SCALING_PER_TIER.length) {
            increase += (tier - HEALTH_SCALING_PER_TIER.length) * UNDEFINED_HEALTH_SCALING_PER_TIER;
        }
        return 1.0F + increase;
    }

    /**
     * The damage multiplier at a tablet tier, the same way
     * {@code BiomeMissionIncursionData.getDamageIncrease} (:161-173) does it.
     *
     * <p>Tier 8 is 1.87, tier 9 is 2.00, tier 10 is 2.15 — §B4's own column.
     */
    public static float damageMultiplier(int tier) {
        float increase = 0.0F;
        for (int i = 0; i < tier && i < DAMAGE_SCALING_PER_TIER.length; i++) {
            increase += DAMAGE_SCALING_PER_TIER[i];
        }
        if (tier > DAMAGE_SCALING_PER_TIER.length) {
            increase += (tier - DAMAGE_SCALING_PER_TIER.length) * UNDEFINED_DAMAGE_SCALING_PER_TIER;
        }
        return 1.0F + increase;
    }

    /** One realm's boss: who it is, where vanilla keeps it, and how hard. */
    public static final class Boss {

        /** {@link RealmDepth}'s realm constant this boss belongs to. */
        public final int realm;

        /** The vanilla mob string ID {@code MobRegistry.getMob} is handed. */
        public final String mobStringID;

        /** The vanilla {@code IncursionBiome} this boss is the boss OF. */
        public final String vanillaIncursion;

        /** CLASSIC slot of the boss's own {@code MaxHealthGetter}. */
        public final int baseHealthClassic;

        /** The incursion tablet tier this portal fights at (§B4). */
        public final int tier;

        Boss(int realm, String mobStringID, String vanillaIncursion,
                int baseHealthClassic, int tier) {
            this.realm = realm;
            this.mobStringID = mobStringID;
            this.vanillaIncursion = vanillaIncursion;
            this.baseHealthClassic = baseHealthClassic;
            this.tier = tier;
        }

        public float healthMultiplier() {
            return SkyBossLadder.healthMultiplier(this.tier);
        }

        public float damageMultiplier() {
            return SkyBossLadder.damageMultiplier(this.tier);
        }

        /**
         * What the boss actually walks out with, for logs and for checking the
         * ladder against §B4's "final HP" column: 57 240, 127 200, 157 520,
         * 161 100, 208 000.
         */
        public int finalHealth() {
            return Math.round(this.baseHealthClassic * this.healthMultiplier());
        }
    }

    /**
     * The ladder, indexed by realm. §B4's table, unchanged.
     *
     * <p>It is monotone on purpose — 57k, 127k, 158k, 161k, 208k — so walking
     * outwards is walking up. Which other incursion bosses are left unused, and
     * why, is recorded in §B4; nothing here should grow a row without that
     * document growing one first.
     */
    private static final Boss[] BY_REALM = new Boss[RealmDepth.REALM_COUNT];

    static {
        // Snow Deep Cave -> SnowDeepCaveIncursionBiome.java:31 super("cryoqueen").
        // 18 000 = CryoQueenMob.BASE_MAX_HEALTH (:110) CLASSIC slot. Tier 8 ->
        // x3.18 = 57 240.
        BY_REALM[RealmDepth.REALM_SKYREACH] =
                new Boss(RealmDepth.REALM_SKYREACH, "cryoqueen", "SnowDeepCaveIncursionBiome", 18000, 8);

        // Moon Arena -> MoonArenaIncursionBiome.java:29 super("moonlightdancer").
        // 40 000 = MoonlightDancerMob.MAX_HEALTH (:111) CLASSIC slot. Tier 8 ->
        // x3.18 = 127 200.
        BY_REALM[RealmDepth.REALM_EDEN] =
                new Boss(RealmDepth.REALM_EDEN, "moonlightdancer", "MoonArenaIncursionBiome", 40000, 8);

        // Settlement Ruins -> SettlementRuinsIncursionBiome.java:46
        // super("ascendedwizard"). 44 000 = AscendedWizardMob.MAX_HEALTH (:167)
        // CLASSIC slot. Tier 9 -> x3.58 = 157 520.
        BY_REALM[RealmDepth.REALM_STEINFELD] =
                new Boss(RealmDepth.REALM_STEINFELD, "ascendedwizard", "SettlementRuinsIncursionBiome", 44000, 9);

        // Swamp Deep Cave -> SwampDeepCaveIncursionBiome.java:31
        // super("pestwarden"). 45 000 = PestWardenHead.BASE_MAX_HEALTH (:107)
        // CLASSIC slot. Tier 9 -> x3.58 = 161 100.
        BY_REALM[RealmDepth.REALM_GHOST] =
                new Boss(RealmDepth.REALM_GHOST, "pestwarden", "SwampDeepCaveIncursionBiome", 45000, 9);

        // Crystal Hollow -> CrystalHollowIncursionBiome.java:28
        // super("crystaldragon"). 52 000 = CrystalDragonHead.MAX_HEALTH (:103)
        // CLASSIC slot. Tier 10 -> x4.00 = 208 000.
        BY_REALM[RealmDepth.REALM_CROOKED] =
                new Boss(RealmDepth.REALM_CROOKED, "crystaldragon", "CrystalHollowIncursionBiome", 52000, 10);

        // Hell: reserved, not built. §B4 holds mutanthydra (Scrapyard,
        // ScrapyardIncursionBiome.java:28; MutantHydraBossMob.MAX_HEALTH (:105)
        // CLASSIC = 80 000) for it and gives it no tier, so there is no row and
        // no portal until the realm itself exists.
        BY_REALM[RealmDepth.REALM_HELL] = null;
    }

    /**
     * This realm's boss, or {@code null} where the ladder has no rung yet.
     *
     * <p>{@code null} is a real answer, not a failure: Hell is reserved, and a
     * caller that gets one must simply do nothing rather than substitute
     * something.
     */
    public static Boss forRealm(int realm) {
        if (realm < 0 || realm >= BY_REALM.length) {
            return null;
        }
        return BY_REALM[realm];
    }
}
