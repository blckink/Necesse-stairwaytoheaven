package stairwaytoheaven.realms.ghost;

import java.util.HashSet;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.hostile.BoneWalkerMob;
import necesse.inventory.lootTable.LootTable;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Headless Butler — still doing the rounds of a house that burned down, still
 * annoyed about being interrupted.
 *
 * <p><b>Vanilla base:</b> {@link BoneWalkerMob}, art {@code mobs/bonewalker}
 * composed through {@code HumanDrawOptions} — a walking skeleton in the
 * remains of livery, which is as close to "headless butler" as the game's own
 * sheets get. Subclassing keeps the humanoid rig, the swim mask offsets and
 * the flesh-particle death, none of which would exist on a fresh mob.
 *
 * <h2>Tier</h2>
 * Ghost Realm row, melee, no role discount: <b>2800 HP / 230 damage / 55
 * armour</b> ({@code docs/BALANCE.md} §5; the incursion-tier-7 measurement is
 * written out in {@link DrifterMob}). Vanilla's bone walker is a surface-night
 * mob at 175 HP / 30 damage / 0 armour and stays exactly that.
 *
 * <h2>"Rude."</h2>
 * {@code WORLD_DESIGN} §10 closes the Ghost Realm's design with a humour note
 * that is design and not decoration: <i>"Not every ghost is hostile. Some
 * enemies may say on death: 'Rude.'"</i> This is the mob that says it, and the
 * realm's tone depends on the line landing, so it is wired properly rather than
 * printed into chat.
 *
 * <p>The mechanism is vanilla's own {@code PacketMobChat}, which puts a
 * {@code ChatBubbleText} over the mob on every client that can see it — the
 * same packet a settler uses to talk. It is sent BEFORE {@code super.onDeath},
 * deliberately: {@code PacketMobChat.processClient} resolves the speaker with
 * {@code GameUtils.getLevelMob(uniqueID, level)} and silently drops the bubble
 * if the mob is already gone, and the death packet that removes it client-side
 * goes out after {@code onDeath} returns (Mob.java:3149-3151). Sending first
 * and letting TCP keep the order is what makes the joke arrive.
 */
public class HeadlessButlerMob extends BoneWalkerMob {

    /** Ghost Realm row = <b>2800 HP</b> on Classic. Vanilla's walker is 175. */
    public static final MaxHealthGetter MAX_HEALTH =
            new MaxHealthGetter(1120, 2100, 2800, 3640, 5040);

    /** Ghost Realm row = <b>230 damage</b>. Vanilla builds 30 inline in its AI. */
    public static final GameDamage DAMAGE = new GameDamage(230.0F);

    /** Ghost Realm row = <b>55 armour</b>. Vanilla's walker wears none. */
    public static final int ARMOR = 55;

    /**
     * The last words. Three of them, so a graveyard full of butlers is not a
     * single sound effect; "Rude." is the design document's own line and stays
     * first.
     *
     * <p>Written as three literal {@code LocalMessage}s rather than as key
     * strings picked at runtime, because a key assembled from a variable is
     * invisible to {@code tools/locale_audit.py} — it can only follow literals,
     * and a line nothing checks is a line that ships missing. Vanilla builds
     * its own {@code DeathMessageTable} the same way.
     */
    private static final GameMessage[] LAST_WORDS = {
            new LocalMessage("misc", "butlerlastwords1"),
            new LocalMessage("misc", "butlerlastwords2"),
            new LocalMessage("misc", "butlerlastwords3"),
    };

    public static LootTable lootTable = GhostLoot.bony();

    public HeadlessButlerMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        // Vanilla's own tree (512 search, 100 knockback, 40s wander) rebuilt
        // against our damage: BoneWalkerMob.init builds `new GameDamage(30.0F)`
        // as a local inside the AI constructor call, so there is no field to
        // write through. super.init() still runs first because it is what
        // rolls the mob's seasonal hat.
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedCollisionPlayerChaserWandererAI<>(() -> false, 512, DAMAGE, 100, 40000));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
        if (this.isServer() && this.getLevel() != null && this.getLevel().getServer() != null) {
            GameMessage words = LAST_WORDS[GameRandom.globalRandom.nextInt(LAST_WORDS.length)];
            this.getLevel().getServer().network.sendToClientsWithEntity(
                    new PacketMobChat(this.getUniqueID(), words), this);
        }
        super.onDeath(attacker, attackers);
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }

    /**
     * Bestiary face: it subclasses BoneWalkerMob, so it wears that creature's
     * face in the journal too. {@code Mob.getMobIcon()} is overridable and
     * {@code FormJournalEntryComponent} asks the MOB rather than the registry,
     * so this needs no PNG of its own -- see {@link stairwaytoheaven.mobs.BorrowedMobIcon}
     * for why borrowing the face is the right answer and not a shortcut.
     */
    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("bonewalker", super.getMobIcon());
    }

}
