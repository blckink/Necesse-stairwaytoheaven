package stairwaytoheaven.journal;

import java.util.ArrayList;
import java.util.List;

import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.PacketRegistry;
import necesse.inventory.item.Item;
import stairwaytoheaven.worldgen.RealmDepth;

/**
 * {@code /swhjournal} — proves, on a real server, that the journal is wired:
 * the item is registered as the right class, both packets are registered, every
 * delivery quest's asks can be read, and the book builds for every connected
 * player (or for the world alone when nobody is connected), survives its own
 * packet round trip, and names no missing locale key.
 *
 * <p>Run by a player, it also opens that player's journal, which is the quick
 * way to look at the window without the item.
 *
 * <p>Prints {@code SWH_JOURNAL_DONE} last, for {@code scripts/integration_test.sh}.
 * Admin-only; it changes nothing.
 */
public class JournalCommand extends ModularChatCommand {

    public JournalCommand() {
        super("swhjournal", "Checks the Adventurer's Journal and prints each player's chapter status (debug)",
                PermissionLevel.ADMIN, false);
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient,
            Object[] args, String[] errors, CommandLog logs) {
        try {
            this.run(server, serverClient, logs);
        } catch (RuntimeException e) {
            logs.add("journal check: FAIL exception=" + e);
            e.printStackTrace();
        }
        logs.add("SWH_JOURNAL_DONE");
    }

    private void run(Server server, ServerClient caller, CommandLog logs) {
        Item item = ItemRegistry.getItem(AdventurerJournal.ITEM_ID);
        boolean itemOK = item instanceof AdventurersJournalItem;
        int openID = PacketRegistry.getPacketID(PacketJournalOpen.class);
        int requestID = PacketRegistry.getPacketID(PacketJournalRequest.class);
        boolean packetsOK = openID >= 0 && requestID >= 0;

        List<Class<? extends DeliverItemsQuest>> quests = LegacyQuestSource.deliveryQuests();
        int asksRead = 0;
        for (Class<? extends DeliverItemsQuest> type : quests) {
            if (!QuestAsks.of(type).isEmpty()) {
                asksRead++;
            }
        }
        JournalWorldData data = JournalWorldData.get(server);
        logs.add("journal check: item=" + AdventurerJournal.ITEM_ID + " id=" + (item == null ? -1 : item.getID())
                + " class=" + (item == null ? "NONE" : item.getClass().getSimpleName())
                + " packets=" + (packetsOK ? "OK" : "MISSING") + " asks=" + asksRead + "/" + quests.size()
                + " source=" + AdventurerJournal.stepSource().name()
                + " given=" + (data == null ? -1 : data.givenCount())
                + " sightings=" + (data == null ? -1 : data.seenCount()));

        List<ServerClient> readers = new ArrayList<>();
        for (ServerClient connected : server.getClients()) {
            if (connected != null) {
                readers.add(connected);
            }
        }
        if (readers.isEmpty()) {
            readers.add(null); // the world-only book
        }
        for (ServerClient reader : readers) {
            this.report(server, reader, logs);
        }
        if (caller != null) {
            AdventurerJournal.sendTo(server, caller, false);
            logs.add("journal: opened for " + caller.getName());
        }
    }

    private void report(Server server, ServerClient reader, CommandLog logs) {
        String who = reader == null ? "world" : reader.getName();
        JournalBook book = AdventurerJournal.build(server, reader);
        int bytes = book.toPacket().getSize();
        JournalBook back = JournalBook.read(new PacketReader(book.toPacket()));
        boolean roundTrip = back.chapters.size() == book.chapters.size() && back.stepCount() == book.stepCount();
        for (int i = 0; roundTrip && i < book.chapters.size(); i++) {
            roundTrip = book.chapters.get(i).codes().equals(back.chapters.get(i).codes())
                    && book.chapters.get(i).residents.size() == back.chapters.get(i).residents.size()
                    && book.chapters.get(i).lore.size() == back.chapters.get(i).lore.size();
        }
        int[] counts = new int[2]; // messages, missing
        for (JournalChapter chapter : back.chapters) {
            count(chapter.name, counts);
            count(chapter.intro, counts);
            for (JournalStep step : chapter.steps) {
                count(step.title, counts);
                count(step.description, counts);
                count(step.giver, counts);
                count(step.where, counts);
                count(step.hint, counts);
                count(step.reward, counts);
                count(step.why, counts);
                count(step.opens, counts);
                for (GameMessage objective : step.objectives) {
                    count(objective, counts);
                }
            }
            for (List<JournalLine> lines : java.util.Arrays.asList(chapter.facts, chapter.residents, chapter.landmarks, chapter.lore)) {
                for (JournalLine line : lines) {
                    count(line.title, counts);
                    count(line.detail, counts);
                }
            }
        }
        StringBuilder codes = new StringBuilder();
        for (JournalChapter chapter : book.chapters) {
            codes.append(' ').append(RealmDepth.keyOf(chapter.realm)).append('=').append(chapter.codes());
        }
        logs.add("journal book: reader=" + who + " chapters=" + book.chapters.size() + " steps=" + book.stepCount()
                + " bytes=" + bytes + " roundtrip=" + (roundTrip ? "OK" : "FAIL")
                + " messages=" + counts[0] + " missing=" + counts[1]);
        logs.add("journal state: reader=" + who + codes);

        // The Warden's steps must each say what, why and for what, in both
        // languages -- the English-only check above cannot see a German gap.
        int warden = 0;
        int complete = 0;
        int missingDe = 0;
        for (JournalChapter chapter : back.chapters) {
            for (JournalStep step : chapter.steps) {
                if (!LegacyQuestSource.WARDEN_STEPS.contains(step.id)) {
                    continue;
                }
                warden++;
                boolean whole = true;
                for (GameMessage part : new GameMessage[] {step.title, step.description, step.why, step.opens, step.reward}) {
                    if (part == null || part.isMissingKey(Localization.English)) {
                        whole = false;
                    } else if (part.isMissingKey(Localization.German)) {
                        whole = false;
                        missingDe++;
                        System.out.println("journal missing German key in step " + step.id + ": " + part.translate());
                    }
                }
                if (whole) {
                    complete++;
                }
            }
        }
        logs.add("journal warden: reader=" + who + " steps=" + warden + "/" + LegacyQuestSource.WARDEN_STEPS.size()
                + " complete=" + complete + " missingde=" + missingDe);
    }

    private static void count(GameMessage message, int[] counts) {
        if (message == null) {
            return;
        }
        counts[0]++;
        // Translating walks every nested replacement, so a broken message
        // throws here rather than in a player's window.
        message.translate();
        if (message.isMissingKey(Localization.English)) {
            counts[1]++;
            System.out.println("journal missing key in: " + message.translate());
        }
    }
}
