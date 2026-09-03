package stairwaytoheaven.arsenal;

import java.awt.Point;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.HumanTexture;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.decorators.FailerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TeleportOnProjectileHitAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.mobs.hostile.AncientSkeletonMageMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.AncientSkeletonMageProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Cinder Cantor — a masked singer of the old rite, still walking the ash. The
 * Ashen Reach's first real inhabitant: until now only stray Gloom Shades
 * wandered in from the fen.
 *
 * <p><b>Vanilla base:</b> {@link AncientSkeletonMageMob} for BEHAVIOUR.
 * Subclassing keeps its whole character: a caster that shoots
 * {@code AncientSkeletonMageProjectile} at range 640 AND owns a
 * {@code TeleportOnProjectileHitAINode} — get hit by one of ITS bolts and it
 * blinks away in a puff of smoke, which is what makes it a different fight
 * from anything else in the mod.
 *
 * <h2>The art — and the two sheets that are still vanilla's</h2>
 * The BODY is ours: {@code mobs/cindercantor.png}, 448x320, drawn on the same
 * grid vanilla's {@code mobs/ancientskeletonmage} uses — eight 32px rows of
 * walk frames, the gib strip on row 8 and the staff sprite on row 9.
 * {@link #addDrawables} and {@link #spawnDeathParticles} sample it, and nothing
 * is recoloured at load time: the sheet is drawn as it was supplied.
 *
 * <p><b>A {@code HumanTexture} is THREE sheets, not one, and only the body was
 * supplied.</b> {@code MobRegistry.Textures.humanTexture(path)} expands to
 * {@code new HumanTexture(fromFile(path), fromFile(path + "arms_left"),
 * fromFile(path + "arms_right"))} (VERIFIED [jar],
 * MobRegistry.java:1830-1836), and {@code HumanDrawOptions} composes the mob out
 * of all three (HumanDrawOptions.java:131-136). {@link #texture} is therefore
 * built as OUR body over VANILLA's
 * {@code mobs/ancientskeletonmagearms_left} and
 * {@code mobs/ancientskeletonmagearms_right} (both 448x320). <b>This is visible
 * in game:</b> the Cinder Cantor walks in a robe of its own with the Ancient
 * Skeleton Mage's bare bone arms, and the arm it swings mid-cast is vanilla's
 * too.
 *
 * <p>Exactly two more sheets would finish it, both 448x320 and both on the same
 * grid as the body:
 * <ul>
 *   <li>{@code src/main/resources/mobs/cindercantorarms_left.png}</li>
 *   <li>{@code src/main/resources/mobs/cindercantorarms_right.png}</li>
 * </ul>
 * When they land, the two {@code GameTexture.fromFile} calls in
 * {@code SkyMobs.loadTextures} that currently name vanilla's
 * {@code ancientskeletonmagearms_left} and {@code ancientskeletonmagearms_right}
 * are re-pointed at the two files above, and nothing else changes. (The paths
 * are deliberately not written out as a literal {@code fromFile} call here:
 * {@code tools/locale_audit.py} scans comments too and would read a
 * not-yet-drawn path as a missing sheet.)
 *
 * <p><b>Why {@link #addDrawables} and {@link #spawnDeathParticles} are
 * overridden.</b> Vanilla reads the static
 * {@code MobRegistry.Textures.ancientSkeletonMage} inline in both — three times
 * in the draw (the {@code HumanDrawOptions} plus the item and arm sprites of the
 * attack animation) and once in the gibs — and {@code Mob} exposes no
 * per-instance texture hook (VERIFIED [jar], AncientSkeletonMageMob.java:172,
 * :209, :218, :221). Writing to that static would repaint every real Ancient
 * Skeleton Mage in every ruin in the world, so the mob is redrawn here instead.
 * {@code addDrawables} deliberately does NOT call {@code super}: that IS
 * vanilla's draw, and calling it would put the bone mage underneath ours.
 * Nothing is lost — vanilla's own first line is {@code super.addDrawables(...)},
 * which reaches {@code Mob.addDrawables}, whose body is EMPTY (VERIFIED [jar],
 * Mob.java:1734-1745); health and status bars come from
 * {@code Mob.addDrawablesLoop} around the call. The shadow is left alone: it
 * comes from {@code Mob.getShadowDrawOptions}'s shared
 * {@code MobRegistry.Textures.human_shadow}, which is not the mob's own art.
 *
 * <h2>Tier</h2>
 * The ladder and the incursion measurement behind it are written out once, in
 * {@link RimeSentryMob} — the mob that sits on its floor. In short: incursion
 * tier 1 applies no multiplier at all (VERIFIED [jar]:
 * {@code BiomeMissionIncursionData}'s cumulative per-tier arrays both begin
 * {@code 0.0F}), which pins the Skyreach floor at 1000 HP / 130 damage / 40
 * armour, and summing those same arrays to <b>incursion tier 7</b> gives +1.80
 * health / +0.75 damage — the Ghost Realm's rung of 2800 HP / 230 damage /
 * 55 armour (armour has no incursion array and is walked up by hand).
 *
 * <p>The Cinder Cantor is the Ghost Realm's ranged role, so it takes the ranged
 * discount off that rung: 2800 x 0.7 = <b>1960 HP</b> and 230 x 0.85 = 195.5 →
 * <b>195 damage</b>, at the rung's full <b>55 armour</b>. Vanilla's own mage is
 * a ruins mob at 400 HP / 90 damage / 25 armour and stays exactly that; only
 * this subclass moves.
 *
 * <p><b>Why {@code init()} is overridden.</b> The bolt's damage is a
 * {@code new GameDamage(90.0F)} built inline inside vanilla's anonymous
 * {@code ConfusedPlayerChaserWandererAI} (VERIFIED [jar]), with no field to
 * write through, so the whole tree — chaser, bolt, and the
 * {@code TeleportOnProjectileHitAINode} that gives the mob its character — is
 * re-declared here against {@link #DAMAGE}. Ranges, cooldowns, projectile speed
 * and the teleport's 3s/7-tile window are vanilla's and stay vanilla's; only
 * the damage number changes.
 *
 * <p>Vanilla drops plain bone; ours keeps the bone (the Veil has no other
 * source) and adds the two Veil materials.
 */
public class CinderCantorMob extends AncientSkeletonMageMob {

    /**
     * Ghost Realm rung (incursion tier 7) 2800 x 0.7 (ranged role) = <b>1960</b>
     * on Classic. The other four difficulties reuse the ratios of the getter the
     * floor was measured from — {@code AscendedGolemMob.MAX_HEALTH}'s
     * 0.40 / 0.75 / 1.00 / 1.30 / 1.80 around Classic (VERIFIED [jar]).
     * Vanilla's mage is 400.
     */
    public static final MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(780, 1470, 1960, 2550, 3530);

    /**
     * Ghost Realm rung 230 x 0.85 (ranged role) = 195.5, snapped onto the
     * ladder's five-step damage grid = <b>195</b>; the rung's 230 is
     * {@code CrystalGolemMob.damage} (130, VERIFIED [jar]) run out to incursion
     * tier 7. Vanilla builds 90 inline inside its AI.
     */
    public static final GameDamage DAMAGE = new GameDamage(195.0F);

    /**
     * Ghost Realm rung = <b>55 armour</b>. There is no armour array in
     * {@code BiomeMissionIncursionData} (VERIFIED [jar]: it scales health and
     * damage only), so the ladder walks armour up by hand from the floor's 40 —
     * {@code CrystalGolemMob}'s {@code setArmor(40)}, matched by the rolling
     * {@code CrystalArmadillo} and {@code AscendedBatMob}. Vanilla's mage
     * wears 25.
     */
    public static final int ARMOR = 55;

    /**
     * Our body over vanilla's two arm sheets, composed by
     * {@code SkyMobs.loadTextures} on the client only — see the class comment
     * for the two files that would make the arms ours as well. It stays null on
     * a dedicated server, which never draws, hence the guards in
     * {@link #addDrawables} and {@link #spawnDeathParticles}.
     */
    public static HumanTexture texture;

    /**
     * Vanilla's {@code AncientSkeletonMageMob.addDrawables}, ported line for
     * line with our {@link HumanTexture} in all three places it reads
     * {@code MobRegistry.Textures.ancientSkeletonMage}: the same -32 / -51 draw
     * offsets, the same walk frame, the same swim mask, the same enemy tracker,
     * the same row-9 staff on a (4,4) pivot swung against
     * {@code getAttackAnimProgress} over the row-8 arm, and the same shadow
     * pass.
     *
     * <p>Declared {@code public} because {@code AncientSkeletonMageMob} declares
     * it public; {@code Mob.addDrawables} itself is {@code protected} (VERIFIED
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
        int drawX = camera.getDrawX(x) - 22 - 10;
        int drawY = camera.getDrawY(y) - 44 - 7;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        drawY += this.getBobbing(x, y);
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
        HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, texture)
                .sprite(sprite)
                .dir(dir)
                .mask(swimMask)
                .light(light)
                .applyEnemyTracker(this, perspective);
        float animProgress = this.getAttackAnimProgress();
        if (this.isAttacking) {
            ItemAttackDrawOptions attackOptions = ItemAttackDrawOptions.start(dir)
                    .itemSprite(texture.body, 0, 9, 32)
                    .itemRotatePoint(4, 4)
                    .itemEnd()
                    .armSprite(texture.body, 0, 8, 32)
                    .swingRotation(animProgress);
            humanDrawOptions.attackAnim(attackOptions, animProgress);
        }

        final DrawOptions drawOptions = humanDrawOptions.pos(drawX, drawY);
        list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
                drawOptions.draw();
            }
        });
        this.addShadowDrawables(tileList, level, x, y, light, camera);
    }

    /**
     * Vanilla's {@code AncientSkeletonMageMob.spawnDeathParticles}, verbatim,
     * with our body sheet in place of
     * {@code MobRegistry.Textures.ancientSkeletonMage.body}. The gibs are cut
     * out of the mob's own sheet — without this a Cinder Cantor would shatter
     * into vanilla's bone chips — and the sprite indices are unchanged:
     * {@code nextInt(5)} over row 8 at 32px, the same strip on our sheet as on
     * vanilla's.
     */
    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        if (texture == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            this.getLevel().entityManager.addParticle(
                    new FleshParticle(this.getLevel(), texture.body,
                            GameRandom.globalRandom.nextInt(5), 8, 32,
                            this.x, this.y, 20.0F, knockbackX, knockbackY),
                    Particle.GType.IMPORTANT_COSMETIC);
        }
    }

    /**
     * Quantities are the Skyreach baseline x the Ghost Realm's drop-value
     * multiplier of 1.9, rounded to whole items: bone 1-3 becomes 2-6, and the
     * two Veil materials go 1-2 to 2-4. Chances are unchanged — the rung is
     * paid in stack size, so the drop still has to be earned.
     */
    public static LootTable lootTable = new LootTable(
            LootItem.between("bone", 2, 6),
            new ChanceLootItemList(0.55F, LootItem.between("cinderpearl", 2, 4)),
            new ChanceLootItemList(0.45F, LootItem.between("veilessence", 2, 4)));

    public CinderCantorMob() {
        super();
        // Registered in construction the way AscendedGolemMob registers its own
        // MAX_HEALTH: MobDifficultyChanges throws if it is touched after init().
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        // Vanilla's tree rebuilt one-for-one (640 search / 320 shoot / 40s
        // wander, 120.0F bolt at range 640 with 50 knockback, teleport on a 3s
        // cooldown within 7 tiles) against our own damage.
        ConfusedPlayerChaserWandererAI<CinderCantorMob> chaserAI =
                new ConfusedPlayerChaserWandererAI<CinderCantorMob>(null, 640, 320, 40000, false, false) {
                    @Override
                    public boolean attackTarget(CinderCantorMob mob, Mob target) {
                        if (!mob.canAttack()) {
                            return false;
                        }
                        mob.attack(target.getX(), target.getY(), false);
                        mob.getLevel().entityManager.projectiles.add(new AncientSkeletonMageProjectile(
                                mob.getLevel(), mob, mob.x, mob.y, target.x, target.y, 120.0F, 640, DAMAGE, 50));
                        this.wanderAfterAttack = GameRandom.globalRandom.getChance(0.75F);
                        return true;
                    }
                };
        chaserAI.addChildFirst(new FailerAINode<>(new TeleportOnProjectileHitAINode<CinderCantorMob>(3000, 7) {
            @Override
            public boolean teleport(CinderCantorMob mob, int x, int y) {
                if (mob.isServer()) {
                    mob.teleportAbility.runAndSend(x, y);
                    this.getBlackboard().mover.stopMoving(mob);
                }
                return true;
            }
        }));
        this.ai = new BehaviourTreeAI<>(this, chaserAI);
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /** The Veil's hostiles use the same static-light rule as the sky's. */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
