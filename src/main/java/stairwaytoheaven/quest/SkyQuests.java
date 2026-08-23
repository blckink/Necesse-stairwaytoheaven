package stairwaytoheaven.quest;

import java.util.ArrayList;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.quest.Quest;

/**
 * Helpers for the HUD quest layer of "The Warden's Call". World truth stays
 * in SkywatchQuestData; these quests are per-player journal mirrors created
 * and cleared at the same state-machine points (see docs/research/quest-api.md
 * for the vanilla give/complete contract).
 */
public final class SkyQuests {

    private SkyQuests() {
    }

    /** The client's active quest of the given type, or null. */
    public static <T extends Quest> T findHeld(ServerClient client, Class<T> type) {
        for (Quest quest : client.getQuests().keySet()) {
            if (type.isInstance(quest) && !quest.isRemoved()) {
                return type.cast(quest);
            }
        }
        return null;
    }

    /** Gives a fresh quest of the type unless the client already holds one. */
    public static <T extends Quest> T giveOnce(Server server, ServerClient client, T quest) {
        @SuppressWarnings("unchecked")
        T held = (T) findHeld(client, quest.getClass());
        if (held != null) {
            return held;
        }
        server.world.getQuests().addQuest(quest, true);
        quest.makeActiveFor(server, client);
        return quest;
    }

    /**
     * Removes every instance of the type from the world manager (broadcasts
     * PacketQuestRemove to its active clients) — used when the shared chain
     * stage advances and other players' copies become obsolete.
     */
    public static void removeAllOfType(Server server, Class<? extends Quest> type) {
        ArrayList<Quest> toRemove = new ArrayList<>();
        for (Quest quest : server.world.getQuests().getQuests()) {
            if (type.isInstance(quest)) {
                toRemove.add(quest);
            }
        }
        for (Quest quest : toRemove) {
            server.world.getQuests().removeQuest(quest);
        }
    }

    /** Pushes the world cat flags into every live SpireCatsQuest instance. */
    public static void syncCatQuests(Server server, SkywatchQuestData data) {
        for (Quest quest : server.world.getQuests().getQuests()) {
            if (quest instanceof SpireCatsQuest) {
                SpireCatsQuest cats = (SpireCatsQuest) quest;
                if (cats.blackHome != data.blackHome || cats.tabbyHome != data.tabbyHome) {
                    cats.blackHome = data.blackHome;
                    cats.tabbyHome = data.tabbyHome;
                    cats.markDirty();
                }
            }
        }
    }
}
