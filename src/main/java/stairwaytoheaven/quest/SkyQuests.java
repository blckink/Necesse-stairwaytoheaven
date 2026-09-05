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

    /**
     * One resident's own side-chain: ask once, take the delivery, pay once.
     *
     * <p>Ives, Mortimer and Caspern all run the identical three-state machine —
     * the one {@code EveleenMob.advanceEdenChain} wrote first and
     * {@code KnottMob} copied — and a fourth and fifth hand-rolled copy of it
     * is how two of them would end up subtly different. The shape, and the
     * reason for every guard in it:
     *
     * <ol>
     * <li><b>Already paid?</b> Nothing happens, ever again. The record is
     *     {@code SkywatchWorldData.residentChainsDone}, which is world-scoped
     *     because the reward waives a settler's recruit fee and a settler is
     *     shared property.</li>
     * <li><b>Not holding the quest?</b> Hand it out and say the ask.</li>
     * <li><b>Holding it but short?</b> Say nothing and open the shop. The
     *     journal already shows what is missing and how much, and a second
     *     bubble would delete the greeting one ({@code ChatBubbleText.java
     *     :67-76}, VERIFIED [jar]).</li>
     * <li><b>Holding it and carrying the goods?</b> Complete — which is what
     *     removes the delivered items — clear the journal entry, write the
     *     world record, and let the caller pay.</li>
     * </ol>
     *
     * <p>Deliberately NOT gated on {@code !isSettler()}: that gate is the bug
     * {@code EveleenMob.interact}'s own comment documents at length. A player
     * may recruit any of these three at full price on the first meeting, and if
     * the chain could then never be turned in its reward would be permanently
     * unreachable — the {@code swh_beacon} failure again. Every branch here is
     * idempotent, so running it on every conversation, settler or not, costs
     * nothing.
     *
     * <p>The bubble is the CALLER's to say, not this method's: each of these
     * people has their own voice, and {@code SkySettlerMob.bubble} is protected
     * so a helper in this package could not speak for them anyway. The return
     * value says which line to use, and {@link Step#NOTHING} means say nothing
     * at all.
     *
     * @param quest a fresh instance of the chain's quest, used only if the
     *              player is not already holding one
     */
    public enum Step {
        /** Already paid, still short, or not this conversation's business. */
        NOTHING,
        /** The quest was just handed out — say the ask. */
        ASKED,
        /** The delivery was accepted — pay the reward and say the done line. */
        PAID,
    }

    public static Step advanceResidentChain(Server server, ServerClient client,
            String chainKey, necesse.engine.quest.DeliverItemsQuest quest) {
        if (server == null || client == null
                || SkywatchWorldData.residentChainDone(server, chainKey)) {
            return Step.NOTHING;
        }
        @SuppressWarnings("unchecked")
        Class<? extends necesse.engine.quest.DeliverItemsQuest> type =
                (Class<? extends necesse.engine.quest.DeliverItemsQuest>) quest.getClass();
        necesse.engine.quest.DeliverItemsQuest held = findHeld(client, type);
        if (held == null) {
            giveOnce(server, client, quest);
            return Step.ASKED;
        }
        if (!held.canComplete(client)) {
            return Step.NOTHING;
        }
        held.complete(client);
        removeAllOfType(server, type);
        SkywatchWorldData.markResidentChainDone(server, chainKey);
        return Step.PAID;
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
