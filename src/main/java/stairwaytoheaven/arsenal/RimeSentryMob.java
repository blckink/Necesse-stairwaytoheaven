package stairwaytoheaven.arsenal;

import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.StationaryPlayerShooterAI;
import necesse.entity.mobs.hostile.FrostSentryMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.FrostSentryProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Rime Sentry — a piece of Skywatch frost machinery still standing on the
 * causeways, still firing at anything that walks past.
 *
 * <p><b>Vanilla base:</b> {@link FrostSentryMob} for BEHAVIOUR — the wobble
 * animation, the human shadow, {@code canBePushed = false} and the ground-pillar
 * trail its projectile leaves all come from vanilla unchanged. The ART is ours:
 * {@code mobs/rimesentry.png}, 192x32 — the same six 32px cells vanilla's
 * {@code mobs/frostsentry} is, cell 0 the standing sentry and cells 1-5 the
 * pillar frames. {@link #addDrawables} and {@link #spawnDeathParticles} sample
 * it; nothing is recoloured at load time, the sheet is drawn as it was supplied.
 *
 * <p><b>Why those two are overridden.</b> Vanilla reads its sheet from the
 * static {@code MobRegistry.Textures.frostSentry} inline in both methods and
 * {@code Mob} exposes no per-instance texture hook (VERIFIED [jar],
 * FrostSentryMob.java:109 and :156), so the only way to put our art on the mob
 * without repainting every real Frost Sentry in every snow deep cave is to
 * redraw it ourselves. The shadow's own texture is deliberately left alone:
 * {@code FrostSentryMob.getShadowDrawOptions} returns
 * {@code MobRegistry.Textures.human_shadow} (FrostSentryMob.java:179), a shared
 * blob that is not the sentry's own art, so that method is inherited untouched
 * and {@link #addDrawables} still ends on the same {@code addShadowDrawables}
 * call vanilla ends on.
 *
 * <p>{@link #addDrawables} deliberately does NOT call {@code super}: that IS
 * vanilla's draw, and calling it would put the blue Frost Sentry underneath
 * ours. Nothing is lost by dropping it — {@code FrostSentryMob}'s own first
 * line is {@code super.addDrawables(...)}, which reaches
 * {@code Mob.addDrawables}, whose body is EMPTY (VERIFIED [jar],
 * Mob.java:1734-1745); health and status bars are added by
 * {@code Mob.addDrawablesLoop} around this call, not from inside it. Same
 * finding the Aurora Flake's and Fen Wraith's overrides rest on.
 *
 * <p><b>Known gap: the pillar trail is still vanilla's.</b> Cells 1-5 of our
 * sheet are drawn but never reached, so the frost pillars the projectile lays
 * down stay the vanilla ice-blue. This is not fixable from here:
 * {@code FrostSentryProjectile.onMoveTick} constructs
 * {@code FrostSentryMob.FrostPillar} directly (VERIFIED [jar],
 * FrostSentryProjectile.java:82-86) and that constructor hard-codes
 * {@code MobRegistry.Textures.frostSentry} sprites 1-5
 * (FrostSentryMob.java:207-211). A projectile subclass could not reach it
 * either — the list the pillar handler reads is
 * {@code private final GroundPillarList<FrostSentryMob.FrostPillar> pillars}
 * (FrostSentryProjectile.java:27), so nothing outside that class can add to it.
 * Left alone rather than half-wired.
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
     * Our sheet, filled by {@code SkyMobs.loadTextures} on the client only. It
     * stays null on a dedicated server, which never draws, hence the guards in
     * {@link #addDrawables} and {@link #spawnDeathParticles}.
     */
    public static GameTexture texture;

    /**
     * Vanilla's {@code FrostSentryMob.addDrawables}, ported line for line with
     * our sheet in place of {@code MobRegistry.Textures.frostSentry}: the same
     * -15 / -26 draw offsets, the same bobbing and sinking, the same 10px lift
     * in liquid, the same triangle-wave squash off {@code getAttackAnimProgress}
     * and the same shadow pass. Only the {@link GameTexture} is ours.
     *
     * <p>Declared {@code public} because {@code FrostSentryMob} declares it
     * public; {@code Mob.addDrawables} itself is {@code protected} (VERIFIED
     * [jar]), and an override may widen but never narrow.
     */
    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList,
            OrderableDrawables topList, Level level, int x, int y,
            TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 15;
        int drawY = camera.getDrawY(y) - 26;
        drawY += this.getBobbing(x, y);
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        if (this.inLiquid(x, y)) {
            drawY -= 10;
        }

        float animProgress = GameMath.limit(this.getAttackAnimProgress(), 0.0F, 1.0F);
        float wiggle;
        if (animProgress < 0.5F) {
            wiggle = animProgress * 2.0F;
        } else {
            wiggle = Math.abs((animProgress - 0.5F) * 2.0F - 1.0F);
        }

        int pixelChange = (int) (wiggle * 5.0F);
        final DrawOptions body = texture.initDraw()
                .sprite(0, 0, 32)
                .size(32 - pixelChange * 2, 32 - pixelChange)
                .startGlowOptions(this, (long) this.getID())
                .light(light)
                .applyEnemyTracker(this, perspective)
                .pos(drawX + pixelChange, drawY + pixelChange);
        list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
                body.draw();
            }
        });
        if (this.inLiquid(x, y)) {
            y -= 10;
        }

        this.addShadowDrawables(tileList, level, x, y, light, camera);
    }

    /**
     * Vanilla's {@code FrostSentryMob.spawnDeathParticles}, verbatim, with our
     * sheet in place of {@code MobRegistry.Textures.frostSentry}. The gibs are
     * cut out of the mob's own sheet — without this a Rime Sentry would shatter
     * into vanilla's ice-blue shards — and the sprite indices are unchanged:
     * {@code 1 + nextInt(5)} is cells 1-5 at 32px, which on our sheet is the
     * same strip it is on vanilla's.
     */
    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        if (texture == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            this.getLevel().entityManager.addParticle(
                    new FleshParticle(this.getLevel(), texture,
                            1 + GameRandom.globalRandom.nextInt(5), 0, 32,
                            this.x, this.y, 20.0F, knockbackX, knockbackY),
                    Particle.GType.IMPORTANT_COSMETIC);
        }
    }

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
