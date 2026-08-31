package stairwaytoheaven.mobs;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerChargingCirclingChaserAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.hostile.HostileWormMobHead;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * The Mistserpent: a serpent that swims through the Mistsea, surfacing and
 * diving the way a sea serpent breaches water.
 *
 * <p>Built on vanilla's NON-boss worm chain (HostileWormMobHead, the same base
 * SandwormHead uses) rather than the crystal dragon's boss variant — the
 * Skyreach wants a dangerous inhabitant of the cloud sea, not an arena fight.
 * The head leads and the segments follow on the engine's own worm mathematics;
 * a FlyingAIMover is what lets it pass through the Mistsea rather than pathing
 * around it, exactly as the sandworm passes through dune sand.
 *
 * <p><b>Tier: Skyreach elite.</b> The mod is endgame content, so the weakest
 * thing in it has to already stand where incursion tier 1 starts. Tier 1 adds
 * nothing — it <i>is</i> the baseline. VERIFIED [jar]:
 * {@code BiomeMissionIncursionData.getHealthIncrease()} and
 * {@code getDamageIncrease()} sum {@code healthScalingPerTier} and
 * {@code damageScalingPerTier} (BiomeMissionIncursionData.java:66-67) over
 * indices {@code 0..tier-1} and hand the sum to
 * {@code LevelModifiers.ENEMY_MAX_HEALTH} / {@code ENEMY_DAMAGE} as a
 * fractional <i>increase</i>. Both arrays open on {@code 0.0F}, so tier 1 sums
 * to zero. (They are additive per-tier fractions, not cumulative multipliers —
 * do not read a tier-N number off them by multiplying.)
 *
 * <p>The tier-1 inhabitant to measure against is {@code CrystalGolemMob}:
 * registered {@code crystalgolem} (MobRegistry.java:515) and spawned by
 * {@code CrystalHollowBiome.mobs} (CrystalHollowBiome.java:17), at
 * {@code super(500)} / {@code GameDamage(130.0F)} / {@code setArmor(40)}
 * (CrystalGolemMob.java:70, :57, :72). VERIFIED [jar]. This rebalance sets the
 * mod's floor at <b>1000 HP / 130 damage / 40 armour</b>: damage and armour are
 * that mob's own, the health is doubled and read instead off
 * {@code AscendedGolemMob.MAX_HEALTH = MaxHealthGetter(400, 750, 1000, 1300,
 * 1800)} (AscendedGolemMob.java:35) at its Classic value. Be honest about that
 * half — the Ascended Golem is <b>not</b> a tier-1 spawn. It is an add the
 * Ascended Wizard summons, registered unspawnable (MobRegistry.java:679) and
 * absent from every {@code MobSpawnTable}, and it removes itself after 20 s
 * ({@code deathTime = 20000}, AscendedGolemMob.java:34). 1000 is deliberate
 * headroom for players ten incursions deep, not a measurement of tier 1.
 *
 * <p>The serpent is the sky's one big roaming threat, so it sits an elite step
 * above the floor: 1000 x 1.4 health, and the floor's own damage and armour.
 *
 * <p><b>Where a worm's health lives: on the head, and only there.</b> VERIFIED
 * [jar]. {@code WormMobHead.init()} copies the head's pool onto every segment
 * as it builds the chain ({@code bodyPart.setMaxHealth(this.getMaxHealth());
 * bodyPart.setHealthHidden(this.getHealth());} — WormMobHead.java:219-220);
 * {@code WormMobBody.tickMaster()} re-mirrors max health, current health and
 * armour off the master on every tick (WormMobBody.java:150-163);
 * {@code WormMobBody.getHealth()/getMaxHealth()} delegate to the head
 * (WormMobBody.java:236-258); and {@code WormMobBody.setHealthHidden()} pushes
 * every point of damage a coil takes back onto the master
 * (WormMobBody.java:288-295). One shared pool, owned by {@link #MAX_HEALTH}
 * here. The number in {@link MistserpentBody}'s constructor is a spawn-instant
 * placeholder that never survives a tick.
 */
public class MistserpentHead extends HostileWormMobHead<MistserpentBody, MistserpentHead> {

    public static GameTexture texture;
    public static GameTexture maskTexture;
    public static GameTexture shadowTexture;

    /**
     * Stormshard 2-5 plus Aetherium ore 1-3, both guaranteed. Quantities are
     * unchanged by the endgame pass on purpose: the Skyreach's drop-value
     * multiplier is 1.0, so nothing here is scaled. Worth noting for whoever
     * picks the loot work up — measured against the rest of the roster this is
     * thin for an elite. The Storm Wisp is a 280 HP trash mob and already drops
     * stormshard 1-2; five Wisps cost less than one serpent and pay more. And
     * the serpent has no drop that is its own, so killing it unlocks nothing.
     */
    public static LootTable lootTable = new LootTable(
            LootItem.between("stormshard", 2, 5),
            LootItem.between("aetheriumore", 1, 3));

    /** Segment spacing in pixels — vanilla's sandworm uses 20 for 20 coils. */
    public static final float LENGTH_PER_BODY_PART = 22.0F;
    /** How far the body wave travels; larger reads as a longer, lazier coil. */
    public static final float WAVE_LENGTH = 380.0F;
    public static final int TOTAL_BODY_PARTS = 14;

    /**
     * 1400 = the mod's 1000 HP floor x 1.4 for an elite.
     *
     * <p>Vanilla analogue: {@code AscendedGolemMob.MAX_HEALTH =
     * MaxHealthGetter(400, 750, 1000, 1300, 1800)} (AscendedGolemMob.java:35),
     * measured at <b>1000</b> on Classic, which this rebalance adopts as the
     * mod's health floor. VERIFIED [jar]. What that mob actually is, and why the
     * floor is twice the health of the real tier-1 spawn, is in the class note
     * above — it is headroom, not a measurement.
     *
     * <p>Flat, the way vanilla's own worms are: {@code SandwormHead} passes a
     * literal 1200 and re-sets 1800 by hand on an incursion level
     * (SandwormHead.java:61 and :98-101). A {@code MaxHealthGetter} would in
     * fact work on a worm head — {@code WormMobHead.init()} calls
     * {@code super.init()} first (WormMobHead.java:195), so {@code Mob.init()}'s
     * {@code difficultyChanges.init()} (Mob.java:697) has already applied the
     * difficulty health before the chain copies {@code getMaxHealth()} onto the
     * segments at WormMobHead.java:219. VERIFIED [jar]. It is not used here
     * because this pass states one number per mob at the Classic baseline across
     * the whole roster; a difficulty curve belongs on every mob at once, and
     * would go in the constructor as
     * {@code this.difficultyChanges.setMaxHealth(getter)} exactly as
     * {@code AscendedGolemMob} does it (AscendedGolemMob.java:38).
     *
     * <p>The previous value was 1500, set before there was a floor to sit on.
     * It is a small step down, and the pass still makes the serpent far more
     * dangerous: see {@link #ARMOR} and {@link #HEAD_COLLISION}.
     */
    public static final int MAX_HEALTH = 1400;

    /**
     * 40 = the floor's armour exactly.
     *
     * <p>Vanilla analogue: {@code CrystalGolemMob}'s {@code this.setArmor(40)}
     * (CrystalGolemMob.java:72) — the armour of an incursion tier 1 inhabitant.
     * VERIFIED [jar]. The old value was 18, which is below the tier the mod now
     * starts at and is what let chip damage work on it.
     *
     * <p>Set on the head only: {@code WormMobBody.tickMaster()} copies
     * {@code m.getArmorFlat()} onto every coil each tick
     * (WormMobBody.java:155), so this one number armours the whole chain.
     */
    public static final int ARMOR = 40;

    /**
     * 130 on the head, and 130 on every coil.
     *
     * <p>Vanilla analogue: {@code CrystalGolemMob.damage = new
     * GameDamage(130.0F)} (CrystalGolemMob.java:57) — the damage of an
     * incursion tier 1 inhabitant, and so this mod's damage floor.
     * VERIFIED [jar].
     *
     * <p>Vanilla's sandworm splits head and body (105 / 75,
     * SandwormHead.java:51-52), and this class used to copy that shape with
     * 90 / 60. A floor has to hold for every hitbox a creature presents,
     * though, and 14 of this creature's 15 hitboxes are coils — scaling the
     * body down by the sandworm's ratio would put the part of the serpent a
     * player actually swims into <i>below</i> incursion tier 1, which is the
     * exact complaint this pass exists to answer. So both are the floor.
     *
     * <p>That is not 14 x 130 per pass. VERIFIED [jar]:
     * {@code WormMobHead.modifyBodyPart} hands every segment the <i>head's own</i>
     * {@code collisionHitCooldowns} instance (WormMobHead.java:188-189), so the
     * whole chain shares one cooldown set and a player raked along the coils
     * takes one collision hit per window, not one per segment. Uniform 130 is a
     * floor, not a shredder.
     *
     * <p>Both constants existed before this pass and were <b>never read</b>.
     * The class did not override {@code getCollisionDamage}, and
     * {@code Mob.getCollisionDamage} returns {@code null} by default
     * (Mob.java:2124-2125), which means "no collision hit"; neither
     * {@code WormMobHead} nor {@code HostileWormMobHead} overrides it either.
     * The AI is no help — {@code PlayerChargingCirclingChaserAI} only chases
     * and circles and carries no {@code GameDamage} at all. A worm's entire
     * offence is its collision, which is why {@code SandwormHead} and
     * {@code SandwormBody} both override that method (SandwormHead.java:121,
     * SandwormBody.java:71). The serpent therefore dealt <b>zero damage</b> in
     * play. VERIFIED [jar]. It is wired up in
     * {@link #getCollisionDamage(Mob, boolean, ServerClient)} below and in
     * {@link MistserpentBody}.
     */
    public static final GameDamage HEAD_COLLISION = new GameDamage(130.0F);
    /** @see #HEAD_COLLISION — every coil hits at the floor too, and for why. */
    public static final GameDamage BODY_COLLISION = new GameDamage(130.0F);

    /**
     * Only in open Mistsea. The serpent swims the cloud sea; spawning it on an
     * island would put a fourteen-coil worm on ground the player walks, which
     * is neither the fantasy nor survivable. Islands stay the safe part.
     */
    public static final necesse.level.maps.biomes.MobSpawnTable.CanSpawnPredicate IN_MISTSEA =
            (level, client, tile, mobStringID) -> {
                level.regionManager.ensureTileIsLoaded(tile.x, tile.y);
                return level.getTileID(tile.x, tile.y) == stairwaytoheaven.SkyRegistry.mistseaID;
            };

    public MistserpentHead() {
        super(MAX_HEALTH, WAVE_LENGTH, 70.0F, TOTAL_BODY_PARTS, 20.0F, -24.0F);
        // Difficulty curve on vanilla's own ratios, so this rung holds on all
        // five difficulties rather than only Classic. Safe on a worm head:
        // WormMobHead.init() calls super.init() first (:195), so Mob.init()'s
        // difficultyChanges.init() has already applied the health before the
        // chain copies it out to the segments.
        this.difficultyChanges.setMaxHealth(SkyMobTiers.scaled(MAX_HEALTH));
        this.moveAccuracy = 120;
        this.setSpeed(94.0F);
        this.setArmor(ARMOR);
        this.accelerationMod = 1.0F;
        this.decelerationMod = 1.0F;
        this.collision = new Rectangle(-16, -14, 32, 28);
        this.hitBox = new Rectangle(-20, -16, 40, 32);
        this.selectBox = new Rectangle(-20, -35, 40, 40);
    }

    @Override
    protected float getDistToBodyPart(MistserpentBody bodyPart, int index, float lastDistance) {
        return LENGTH_PER_BODY_PART;
    }

    @Override
    protected MistserpentBody createNewBodyPart(int index) {
        MistserpentBody part = index == TOTAL_BODY_PARTS - 1
                ? new MistserpentBody.Tail()
                : new MistserpentBody();
        // Vanilla shares a hit cooldown across runs of three coils, so a single
        // swing cannot rake the whole length for full damage on every segment.
        part.sharesHitCooldownWithNext = index % 3 < 2;
        part.relaysBuffsToNext = index % 3 < 2;
        if (!(part instanceof MistserpentBody.Tail)) {
            part.sprite = new Point(0, 1 + index % 4);
        }
        return part;
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new PlayerChargingCirclingChaserAI<>(null, 2560, 500, 20),
                new FlyingAIMover());
    }

    /**
     * A worm has no attack node; what it swims through is the whole attack.
     * Vanilla analogue: {@code SandwormHead.getCollisionDamage}
     * (SandwormHead.java:121), the only place its 105/120 is ever spent.
     */
    @Override
    public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
        return HEAD_COLLISION;
    }

    /**
     * WormMobHead declares this abstract — every worm has to say what it sounds
     * like when it moves. The sandworm shakes the ground; a serpent in cloud
     * uses the softer water-swim cue.
     */
    @Override
    protected void playMoveSound() {
        necesse.engine.sound.SoundManager.playSound(
                necesse.gfx.GameResources.watersplash,
                necesse.engine.sound.SoundEffect.effect(this).falloffDistance(1600).volume(0.5F));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        if (texture == null || !this.isVisible()) {
            return;
        }
        GameLight light = level.getLightLevel(this);
        float headAngle = GameMath.fixAngle(GameMath.getAngle(
                new java.awt.geom.Point2D.Float(this.dx, this.dy)));
        addAngledDrawable(list, this, new GameSprite(texture, 0, 0, 64), maskTexture,
                light, (int) this.height, headAngle,
                camera.getDrawX(x) - 32, camera.getDrawY(y), 64, perspective);
        this.addShadowDrawables(tileList, level, x, y, light, camera);
    }

    @Override
    protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
        if (shadowTexture == null) {
            return null;
        }
        return shadowTexture.initDraw().light(light)
                .pos(camera.getDrawX(x) - shadowTexture.getWidth() / 2,
                     camera.getDrawY(y) - shadowTexture.getHeight() / 2 + this.getBobbing(x, y));
    }
}
