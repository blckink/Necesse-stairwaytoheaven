package stairwaytoheaven.realms.ghost;

import java.util.HashSet;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.hostile.ForestSpectorMob;
import necesse.inventory.lootTable.LootTable;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Mourning Bride — the Aftergarden's elite, and the reason a guarded place in
 * this realm is a fight rather than a chore.
 *
 * <p><b>Vanilla base:</b> {@link ForestSpectorMob}, art
 * {@code mobs/forestspector}: a drifting veiled shape that leaves an
 * after-image behind it. Subclassing keeps the {@code FlyingAIMover}, the
 * after-image draw, and — the reason it is this mob and not another — its
 * attack, which puts vanilla's {@code SPIRIT_HAUNTED} debuff on the player and
 * runs a soul-drain event between the two of them. Nothing else in the mod's
 * roster does anything to the player except damage.
 *
 * <h2>Tier</h2>
 * Ghost Realm row with the ELITE modifier ({@code docs/BALANCE.md} §6: HP x1.4,
 * damage x1.0): 2800 x 1.4 = <b>3920 HP</b>, <b>230 damage</b>, <b>55
 * armour</b>. Vanilla's spector is a forest-cave mob at 250 HP / 20 armour and
 * stays exactly that.
 *
 * <h2>Why the tree is rebuilt rather than inherited</h2>
 * This is the one mob in the realm whose vanilla attack carries <b>no
 * {@code GameDamage} at all</b>. {@code ForestSpectorMob}'s {@code attackTarget}
 * only applies the debuff and spawns the drain event; the harm comes from
 * {@code SpiritHauntedBuff}, whose numbers live inside a registered buff and
 * cannot be re-tuned per mob without registering a second buff. Inheriting it
 * unchanged would put an elite on the realm's hardest rung that hits for
 * whatever a forest cave hits for — the balance contract would simply not
 * apply to it.
 *
 * <p>So the tree is rebuilt with the same 448 search / 200 attack range / 40s
 * wander and the same line-of-sight test, and the attack now does BOTH: a
 * server hit for the row's {@link #DAMAGE}, and vanilla's own six-second
 * {@code SPIRIT_HAUNTED}. The haunt is what makes her memorable; the hit is
 * what puts her on the ladder. The drain EVENT is deliberately not re-created —
 * it is purely cosmetic particles between the two mobs
 * ({@code ForestSpectorDrainSoulLevelEvent} does nothing but spawn them), and
 * re-declaring a client-side effect from a rebuilt tree is a way to double it.
 */
public class MourningBrideMob extends ForestSpectorMob {

    /**
     * Ghost Realm row x1.4 (elite) = <b>3920 HP</b> on Classic, spread on
     * {@code AscendedGolemMob.MAX_HEALTH}'s measured ratios (VERIFIED [jar]).
     * Vanilla's spector is 250.
     */
    public static final MaxHealthGetter MAX_HEALTH =
            new MaxHealthGetter(1568, 2940, 3920, 5096, 7056);

    /**
     * Ghost Realm row, <b>230 damage</b> — the elite modifier is x1.0 on
     * damage, because an elite is meant to be a longer fight and not a
     * one-shot.
     */
    public static final GameDamage DAMAGE = new GameDamage(230.0F);

    /** Ghost Realm row = <b>55 armour</b>. Vanilla's spector wears 20. */
    public static final int ARMOR = 55;

    /** Knockback on her touch, matching the realm's other melee attackers. */
    public static final float KNOCKBACK = 50.0F;

    /** Vanilla's own duration for the haunt, unchanged. */
    public static final float HAUNT_SECONDS = 6.0F;

    public static LootTable lootTable = GhostLoot.elite();

    /**
     * The bride is not entirely hostile about it. {@code WORLD_DESIGN} §10:
     * <i>"Not every ghost is hostile."</i> — she is, but she has manners.
     */
    private static final GameMessage LAST_WORDS = new LocalMessage("misc", "bridelastwords");

    public MourningBrideMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedPlayerChaserWandererAI<MourningBrideMob>(null, 448, 200, 40000, false, false) {
                    @Override
                    public boolean canHitTarget(MourningBrideMob mob, float fromX, float fromY, Mob target) {
                        return ChaserAINode.hasLineOfSightToTarget(mob, fromX, fromY, -10.0F, target, 10.0F);
                    }

                    @Override
                    public boolean attackTarget(MourningBrideMob mob, Mob target) {
                        if (!mob.canAttack()) {
                            return false;
                        }
                        mob.attack(target.getX(), target.getY(), false);
                        target.isServerHit(DAMAGE, target.x - mob.x, target.y - mob.y, KNOCKBACK, mob);
                        target.buffManager.addBuff(new ActiveBuff(
                                BuffRegistry.Debuffs.SPIRIT_HAUNTED, target, HAUNT_SECONDS, mob), true);
                        return true;
                    }
                }, new FlyingAIMover());
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * Sent BEFORE {@code super.onDeath} on purpose — see
     * {@link HeadlessButlerMob#onDeath} for why the order is what makes the
     * line arrive.
     */
    @Override
    protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
        if (this.isServer() && this.getLevel() != null && this.getLevel().getServer() != null) {
            this.getLevel().getServer().network.sendToClientsWithEntity(
                    new PacketMobChat(this.getUniqueID(), LAST_WORDS), this);
        }
        super.onDeath(attacker, attackers);
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
