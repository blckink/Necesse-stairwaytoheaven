package stairwaytoheaven.realms.crooked;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.hostile.DryadSentinelMob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkyMobTiers;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Tongue Plant — {@code WORLD_DESIGN.md} §13's own name for the thing that was
 * scenery until it moved, and the Spiral Fields' resident.
 *
 * <h2>Vanilla base: {@link DryadSentinelMob}</h2>
 * A mass of leaves standing on a nest of writhing roots, which stays perfectly
 * still — {@code getSpeed()} returns 0 until {@code isAwakened()} — and then
 * gets up. That behaviour is the entire character, and it is inherited rather
 * than rebuilt: the waking animation, the growl, the sleeping pose, the zero
 * knockback and the sinking/swim masks all come from vanilla untouched.
 *
 * <p><b>Borrowed sheet:</b> vanilla {@code mobs/dryadsentinel.png} (plus its
 * shadow sheet), inherited because nothing here overrides {@code addDrawables}.
 * Looked at rather than assumed: an orange leaf-canopy above a tangle of
 * root-tongues. In the Spiral Fields it reads as one of the growing things until
 * the growing thing turns round, which is exactly A3.6's <i>"eyes in plants"</i>
 * joke played with roots instead of eyes.
 *
 * <h2>Two overrides, and why each one is necessary</h2>
 * <ol>
 * <li><b>{@link #init()}</b> — vanilla builds its chase AI around a
 *     {@code new GameDamage(60.0F)} constructed inline inside {@code init()}
 *     (VERIFIED [jar], DryadSentinelMob.java:122). There is no field to retune,
 *     so the only way onto this realm's row is to rebuild the same
 *     {@code CollisionPlayerChaserWandererAI} shape — same 960 chase range, same
 *     200 stopping distance, same 40000 ms give-up, same 500 ms attack-move
 *     cooldown — against {@link #DAMAGE}.</li>
 * <li><b>{@link #serverTick()}</b> — vanilla only wakes a sentinel when it is
 *     hit, when a Banner of War is up, or when the LEVEL carries
 *     {@code LevelModifiers.SPIRIT_CORRUPTED} and a player comes within 320px
 *     (VERIFIED [jar], serverTick and {@code isSpiritCorrupted}). That modifier
 *     belongs to the Dryad boss fight and is never set in Crooked Beyond, so
 *     without this override a Tongue Plant would stand there being furniture
 *     until somebody swung at it — which is a fine trap and a useless guard.
 *     The override runs vanilla's OWN {@code awakenAbility} at vanilla's OWN
 *     range, so the sound, the animation and the network packet are all the
 *     ones the mob already ships.</li>
 * </ol>
 *
 * <h2>Where its numbers come from</h2>
 * The realm row, {@link SkyMobTiers}: Crooked Beyond is incursion tier ~10,
 * <b>4000 HP / 280 damage / 60 armour</b>. This is the realm's STANDARD enemy —
 * no role modifier — so it takes the row unchanged. Vanilla's sentinel is 1000
 * HP / 60 damage / armour 25 and stays exactly that; only this subclass moves.
 */
public class TonguePlantMob extends DryadSentinelMob {

    /**
     * Crooked row 4000 HP on Classic, spread across the five difficulties with
     * the ratios of the getter the floor was measured from
     * ({@code AscendedGolemMob.MAX_HEALTH}: 0.40 / 0.75 / 1.00 / 1.30 / 1.80).
     */
    public static final MaxHealthGetter MAX_HEALTH = SkyMobTiers.scaled(SkyMobTiers.CROOKED_HP);

    /** Crooked row damage. Vanilla's sentinel hits for 60. */
    public static final GameDamage DAMAGE = new GameDamage(SkyMobTiers.CROOKED_DAMAGE);

    /** Crooked row armour. Vanilla's sentinel wears 25. */
    public static final int ARMOR = SkyMobTiers.CROOKED_ARMOR;

    /**
     * Vanilla's own wake radius, restated because {@link #serverTick()} relies
     * on it: {@code DryadSentinelMob.getNearestPlayer} uses
     * {@code checkInRange = 320} pixels, i.e. ten tiles.
     */
    public static final int WAKE_RANGE_TILES = 10;

    /**
     * What a Tongue Plant is made of.
     *
     * <p>Oddwood and Eye Seed, at the realm's drop value
     * ({@code CROOKED_DROP_VALUE} = 2.5, the measured tier-10 loot figure). This
     * is deliberately the SAME two materials the Spiral Fields' flora drops,
     * because that is the joke: the thing that attacked you was the harvest.
     * Vanilla's table hands out dryad logs, saplings, amber and an apple — a
     * different realm's economy entirely.
     */
    public static final LootTable lootTable = new LootTable(
            LootItem.between("oddwood", 5, 12),
            ChanceLootItem.between(0.55F, "eyeseed", 2, 5),
            ChanceLootItem.between(0.30F, "warpresin", 2, 5),
            ChanceLootItem.between(0.12F, "realityshard", 1, 1));

    public TonguePlantMob() {
        super();
        // MobDifficultyChanges throws if it is touched after init().
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    /**
     * Vanilla's {@code init()}, rebuilt against {@link #DAMAGE}.
     *
     * <p>{@code super.init()} IS called, and it does install vanilla's
     * 60-damage tree first — that is unavoidable, because Java gives no way to
     * skip one level of a super chain and {@code HostileMob.init}'s own setup
     * has to run. The replacement below then overwrites {@code this.ai}, so the
     * vanilla tree is constructed and immediately dropped. That costs one
     * allocation per spawn and is the same trade
     * {@link stairwaytoheaven.arsenal.FenWraithMob} and
     * {@link stairwaytoheaven.arsenal.CinderCantorMob} already make for exactly
     * this reason. Every value below is vanilla's, line for line, except the
     * damage.
     */
    @Override
    public void init() {
        super.init();
        this.isHostile = false;
        CollisionPlayerChaserWandererAI<TonguePlantMob> tree =
                new CollisionPlayerChaserWandererAI<>(null, 960, DAMAGE, 200, 40000);
        tree.collisionPlayerChaserAI.collisionChaserAINode.attackMoveCooldown = 500;
        this.ai = new BehaviourTreeAI<>(this, tree, new AIMover());
    }

    /**
     * Wake when somebody walks past, not only when somebody swings.
     *
     * <p>See the class comment for the vanilla condition this replaces. The
     * guard on {@code startWakingUpTime} is vanilla's own and is what keeps this
     * from re-triggering the ability every tick.
     */
    @Override
    public void serverTick() {
        super.serverTick();
        if (this.isServer() && this.startWakingUpTime == 0L && this.getNearestPlayer() != null) {
            this.awakenAbility.runAndSend(this.getTime());
        }
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * Daylight spawning — a plant that only grows at night is not a plant.
     *
     * <p>See {@link SkySpawnRules} for the measurement: on a non-cave level the
     * ambient light is 150 by day and {@code HostileMob}'s threshold is 0, so
     * without this override the Spiral Fields would be empty every daylight hour
     * and full after dusk. The swap is to the STATIC light check, so a lit camp
     * still keeps them off.
     */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
