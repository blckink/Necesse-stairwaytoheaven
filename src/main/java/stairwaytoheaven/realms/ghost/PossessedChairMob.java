package stairwaytoheaven.realms.ghost;

import java.util.HashSet;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.hostile.MimicMob;
import necesse.inventory.lootTable.LootTable;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Possessed Chair — the piece of furniture in the manor that has been watching
 * you since you came in.
 *
 * <p><b>Vanilla base:</b> {@link MimicMob}, art {@code mobs/mimic}.
 * {@code WORLD_DESIGN} §10 asks for "deco wakes", and the mimic is the game's
 * own implementation of exactly that mechanic and the only one: it snaps itself
 * to the centre of a tile and sits perfectly still as a piece of furniture
 * ({@code isDisguised}), and the moment it moves it puffs into a thing with
 * legs and chases. Re-implementing that on a fresh mob would be re-writing
 * {@code MimicMob.tickIsDisguised} and its network handling for no gain.
 *
 * <h2>Tier</h2>
 * Ghost Realm row, no role discount: <b>2800 HP</b>, <b>230 damage</b>,
 * <b>55 armour</b> ({@code docs/BALANCE.md} §5). Vanilla's mimic is a dungeon
 * mob at 600 HP / 20 armour and stays exactly that.
 *
 * <h2>Why there is no init() override</h2>
 * This is the one mob in the realm whose damage is <b>reachable by
 * assignment</b>. {@code MimicMob} rolls its hit as
 * {@code new GameDamage(GameRandom.getIntBetween(mob.minDamageRoll,
 * mob.maxDamageRoll))} inside its own AI, and both bounds are public instance
 * fields (MimicMob.java:59-60). Setting them in the constructor re-tunes the
 * attack without touching the tree at all — so the disguise logic, the
 * network sync and the {@code MimicAI} sequence stay vanilla's, unmodified and
 * un-re-declared.
 *
 * <p>Vanilla's spread is 14..112 around a mean of 63. Held at the same ratio
 * against the row's 230 (x3.65) that is <b>51..409, mean 230</b> — the same
 * gamble vanilla built, on this realm's rung.
 */
public class PossessedChairMob extends MimicMob {

    /**
     * Ghost Realm row = <b>2800 HP</b> on Classic, spread on
     * {@code AscendedGolemMob.MAX_HEALTH}'s measured ratios (VERIFIED [jar]).
     * Vanilla's mimic is 600.
     */
    public static final MaxHealthGetter MAX_HEALTH =
            new MaxHealthGetter(1120, 2100, 2800, 3640, 5040);

    /** Ghost Realm row = <b>55 armour</b>. Vanilla's mimic wears 20. */
    public static final int ARMOR = 55;

    /** 230 x (14/63) rounded — the low end of vanilla's own dice, rescaled. */
    public static final int MIN_DAMAGE_ROLL = 51;
    /** 230 x (112/63) rounded — the high end, rescaled. Mean lands on 230. */
    public static final int MAX_DAMAGE_ROLL = 409;

    public static LootTable lootTable = GhostLoot.ambusher();

    /** It is, after all, still furniture. */
    private static final GameMessage LAST_WORDS = new LocalMessage("misc", "chairlastwords");

    public PossessedChairMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
        this.minDamageRoll = MIN_DAMAGE_ROLL;
        this.maxDamageRoll = MAX_DAMAGE_ROLL;
    }

    /**
     * Ours, not vanilla's.
     *
     * <p>{@code MimicMob.getLootTable} builds a table whose first entry is a
     * {@code mimicchest} — the vanilla furniture it was pretending to be — plus
     * whatever a dungeon generator stuffed into its {@code loot} list. Neither
     * belongs in the Aftergarden: nothing here places one from a dungeon
     * generator, so the list is always empty, and a mimic chest is a dungeon
     * prop from five tiers below.
     */
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

    /**
     * Bestiary face: it subclasses MimicMob, so it wears that creature's
     * face in the journal too. {@code Mob.getMobIcon()} is overridable and
     * {@code FormJournalEntryComponent} asks the MOB rather than the registry,
     * so this needs no PNG of its own -- see {@link stairwaytoheaven.mobs.BorrowedMobIcon}
     * for why borrowing the face is the right answer and not a shortcut.
     */
    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("mimic", super.getMobIcon());
    }

}
