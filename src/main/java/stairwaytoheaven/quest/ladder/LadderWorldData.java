package stairwaytoheaven.quest.ladder;

import java.util.HashSet;

import necesse.engine.network.server.Server;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.worldData.WorldData;

/**
 * What the quest ladder remembers: which player turned in which step, and the
 * few world-wide effects a step switches on.
 *
 * <h2>Per player, unlike the older resident chains</h2>
 * The six chains that predate the ladder (Eveleen's fruit, Ives's vigil,
 * Mortimer's rites, Caspern's forge, Eleanor, Mr. Knott's door) record their
 * turn-in in {@code SkywatchWorldData}, WORLD-scoped, because their reward is a
 * waived settler fee and a settler is shared property. They keep that record —
 * {@link QuestLadder} reads it for them — so no existing save loses a step.
 *
 * <p>Every step the ladder added pays an item that is the player's own — a
 * trinket, a weapon, a drink — so it is recorded per character here, keyed by
 * the vanilla authentication a {@code ServerClient} carries. A second player on
 * the same world climbs the ladder for themselves.
 *
 * <p>Registered as its own {@code WorldData} ({@code StairwayToHeavenMod}) —
 * {@code WorldData}'s constructor throws for an unregistered class, which is
 * the crash {@code VeteranDefense}'s registration comment records.
 *
 * <p>Nothing in play un-records a step. {@code /swhreset quests} is the only
 * thing that may, through {@link #resetProgress}; that command is another
 * agent's to wire (see the chapter-03 report).
 */
public class LadderWorldData extends WorldData {

    public static final String KEY = "swhladder";

    /** "auth:stepID" for every step a player has turned in. */
    public final HashSet<String> done = new HashSet<>();

    /** World-wide effects a step switched on, by name. */
    public final HashSet<String> flags = new HashSet<>();

    /** Magpie's voyages honour the Skyway Writ: every route +15%. */
    public static final String FLAG_WRIT_HONOURED = "writhonoured";

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addStringArray("done", this.done.toArray(new String[0]));
        save.addStringArray("flags", this.flags.toArray(new String[0]));
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.done.clear();
        for (String entry : save.getStringArray("done", new String[0], false)) {
            if (entry != null && !entry.isEmpty()) {
                this.done.add(entry);
            }
        }
        this.flags.clear();
        for (String entry : save.getStringArray("flags", new String[0], false)) {
            if (entry != null && !entry.isEmpty()) {
                this.flags.add(entry);
            }
        }
    }

    /** The record, created on first use; null only without a world. */
    public static LadderWorldData get(Server server) {
        if (server == null || server.world == null) {
            return null;
        }
        WorldEntity worldEntity = server.world.worldEntity;
        if (worldEntity == null) {
            return null;
        }
        WorldData data = worldEntity.getWorldData(KEY);
        if (data instanceof LadderWorldData) {
            return (LadderWorldData) data;
        }
        LadderWorldData created = new LadderWorldData();
        worldEntity.addWorldData(KEY, created);
        return created;
    }

    public boolean isDone(long auth, String stepID) {
        return this.done.contains(auth + ":" + stepID);
    }

    public void markDone(long auth, String stepID) {
        this.done.add(auth + ":" + stepID);
    }

    public static boolean flag(Server server, String flag) {
        LadderWorldData data = get(server);
        return data != null && data.flags.contains(flag);
    }

    /** Clears every step and flag. Admin-only: {@code /swhreset quests}. */
    public void resetProgress() {
        this.done.clear();
        this.flags.clear();
    }

    /** Every step ID one player has turned in (for the journal). */
    public HashSet<String> doneBy(long auth) {
        HashSet<String> out = new HashSet<>();
        String prefix = auth + ":";
        for (String entry : this.done) {
            if (entry.startsWith(prefix)) {
                out.add(entry.substring(prefix.length()));
            }
        }
        return out;
    }
}
