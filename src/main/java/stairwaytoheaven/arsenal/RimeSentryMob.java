package stairwaytoheaven.arsenal;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.StationaryPlayerShooterAI;
import necesse.entity.mobs.hostile.FrostSentryMob;
import necesse.entity.projectile.FrostSentryProjectile;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Rime Sentry — a piece of Skywatch frost machinery still standing on the
 * causeways, still firing at anything that walks past.
 *
 * <p><b>Vanilla base:</b> {@link FrostSentryMob}, whose art is
 * {@code mobs/frostsentry} (a pale ice-blue crystal bud, verified against the
 * sprite dump — it needs no colour shift to sit in the Skyreach palette). The
 * class is subclassed rather than copied, so the rendering, the wobble
 * animation, the human shadow, the death particles, {@code canBePushed = false}
 * and the ground-pillar trail its projectile leaves all come from vanilla
 * unchanged.
 *
 * <h2>The tier ladder, and where its floor is measured</h2>
 * This is the canonical copy: {@link AuroraFlakeMob}, {@link FenWraithMob} and
 * {@link CinderCantorMob} state their own rung and link back here rather than
 * restating the derivation, so there is one place to correct.
 *
 * <p>The mod is endgame content now — its <em>weakest</em> enemy has to be at
 * least as dangerous as an incursion's weakest. <b>VERIFIED [jar]:</b>
 * {@code BiomeMissionIncursionData} scales an incursion through two cumulative
 * per-tier arrays, {@code healthScalingPerTier} = {@code {0.0, 0.25, 0.27, 0.29,
 * 0.31, 0.33, 0.35, 0.38, 0.4, 0.42}} and {@code damageScalingPerTier} =
 * {@code {0.0, 0.15, 0.14, 0.13, 0.12, 0.11, 0.1, 0.12, 0.13, 0.15}}. Both
 * <em>begin</em> at {@code 0.0F}, so <b>tier 1 applies no multiplier at all</b>
 * and tier 1 is simply the raw roster. Summed, tier 7 is +1.80 health / +0.75
 * damage and tier 10 is +3.00 / +1.15 — HP x4.00, damage x2.15.
 *
 * <h2>The floor: 1000 HP / 130 damage / 40 armour</h2>
 * Damage and armour are read straight off the tier-1 roster (VERIFIED [jar]):
 * {@code CrystalHollowBiome.mobs} is {@code crystalgolem} +
 * {@code crystalarmadillo}, {@code CrystalGolemMob.damage} is
 * {@code GameDamage(130)}, and it wears {@code setArmor(40)} — the same 40 the
 * rolling {@code CrystalArmadillo} and {@code AscendedBatMob} carry.
 *
 * <p>The 1000 HP deliberately is <em>not</em> that roster's body:
 * {@code CrystalGolemMob} is {@code super(500)}. It is the Classic slot of
 * {@code AscendedGolemMob.MAX_HEALTH} = {@code MaxHealthGetter(400, 750, 1000,
 * 1300, 1800)} — the Ascended Wizard's summoned golem, which is registered
 * non-spawning ({@code registerMob("ascendedgolem", …, false, false, false)})
 * and so appears in no spawn table at all. That is the point rather than an
 * oversight: the mod is meant to start <em>above</em> incursion trash, so the
 * floor is pinned to an endgame-boss body instead of the 500 the crystal hollow
 * walks around with.
 *
 * <h2>The realms</h2>
 * Skyreach is the floor (~tier 1); Eden ~3 (1500 / 165), Steinfeld ~5
 * (2100 / 200), Ghost Realm ~7 (2800 / 230), Crooked Beyond ~10 (4000 / 280),
 * Hell past 10 (5500 / 340). Armour is the one column the incursion tiers do
 * <b>not</b> touch — there is no armour array — so the ladder walks it up by
 * hand: 40 / 45 / 50 / 55 / 60 / 70. Within a realm an elite takes x1.4 HP, a
 * ranged mob x0.7 HP and x0.85 damage, a fast mob x0.6 HP and x0.8 damage, and
 * the resulting damage is snapped onto the ladder's five-step grid.
 *
 * <p>The Rime Sentry fills the Skyreach's ranged, immobile turret role, so it
 * takes the ranged discount off the floor: 1000 x 0.7 = <b>700 HP</b> and
 * 130 x 0.85 = 110.5 → <b>110 damage</b>, at the floor's full <b>40 armour</b> —
 * a turret trades bulk for reach, not protection.
 *
 * <p>Vanilla's own sentry is a snow-cave mob at 120 HP / 17 damage / 5 armour
 * and stays exactly that; only this subclass moves. Its damage lives in a
 * {@code public static GameDamage} field that the AI closes over, so it cannot
 * be re-tuned per subclass without mutating the vanilla static — which would
 * change the real Frost Sentry in every snow deep cave in the world.
 * {@code init()} therefore rebuilds the same {@code StationaryPlayerShooterAI}
 * shape against our own {@link #DAMAGE}.
 */
public class RimeSentryMob extends FrostSentryMob {

    /**
     * Skyreach floor 1000 HP x 0.7 (ranged/immobile role) = 700 on Classic. The
     * other four difficulties reuse the ratios of the getter the floor itself
     * was measured from — {@code AscendedGolemMob.MAX_HEALTH}'s
     * 0.40 / 0.75 / 1.00 / 1.30 / 1.80 around Classic (VERIFIED [jar]) — so the
     * floor holds on every difficulty and not only on the one it was read off.
     * Vanilla's Frost Sentry is 120.
     */
    public static final MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(280, 525, 700, 910, 1260);

    /**
     * Skyreach floor 130 damage ({@code CrystalGolemMob.damage}, VERIFIED [jar])
     * x 0.85 for the ranged role = 110.5, snapped onto the ladder's five-step
     * damage grid = 110.
     * Vanilla's {@code FrostSentryMob.damage} is 17 and is deliberately left
     * alone — it is a shared static.
     */
    public static final GameDamage DAMAGE = new GameDamage(110.0F);

    /**
     * The floor's armour, unreduced: {@code CrystalGolemMob} sets 40, the
     * rolling {@code CrystalArmadillo} rolls at 40 and {@code AscendedBatMob}
     * wears 40 (VERIFIED [jar]). Vanilla's Frost Sentry wears 5.
     */
    public static final int ARMOR = 40;

    /**
     * Fulgurite is what the Skyreave and the Thunderhead are banded with, and
     * a storm shard is what the machinery ran on. Killing sentries is the
     * fulgurite route that does not need a pickaxe.
     *
     * <p>Quantities are unchanged on purpose: the Skyreach sits at drop-value
     * x1.0 because it <em>is</em> the baseline the deeper realms multiply
     * against.
     */
    public static LootTable lootTable = new LootTable(
            new ChanceLootItemList(0.7F, LootItem.between("fulgurite", 1, 2)),
            new ChanceLootItemList(0.4F, LootItem.between("stormshard", 1, 2)));

    public RimeSentryMob() {
        super();
        // Registered in construction the way AscendedGolemMob registers its own
        // MAX_HEALTH: MobDifficultyChanges throws if it is touched after init().
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this, new StationaryPlayerShooterAI<RimeSentryMob>(352) {
            @Override
            public void shootTarget(RimeSentryMob mob, Mob target) {
                FrostSentryProjectile projectile = new FrostSentryProjectile(
                        mob.getLevel(), mob, mob.x, mob.y, target.x, target.y, 78.0F, 544, DAMAGE, 50);
                projectile.x -= projectile.dx * 20.0F;
                projectile.y -= projectile.dy * 20.0F;
                RimeSentryMob.this.attack((int) (mob.x + projectile.dx * 100.0F),
                        (int) (mob.y + projectile.dy * 100.0F), false);
                mob.getLevel().entityManager.projectiles.add(projectile);
            }
        });
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * Same rule the rest of the sky roster uses: the Skyreach is dangerous at
     * noon, and placed light still keeps it clear. See {@link SkySpawnRules}.
     */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
