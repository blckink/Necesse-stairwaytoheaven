package stairwaytoheaven.journal;

import java.util.ArrayList;
import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

/**
 * Everything one player's journal shows, built on the server and sent whole.
 *
 * <p>The journal never reads state on the client. Quest progress is
 * server-side ({@code SkywatchQuestData} is level data the client never
 * receives, {@code SkywatchWorldData} and {@code VeilWorldData} are world data
 * that are never packed into a world packet), so the server builds this
 * summary for the one player who asked and ships it in a
 * {@link PacketJournalOpen}. It is a snapshot: reopening the journal (or its
 * refresh button) asks again.
 */
public final class JournalBook {

    /** Bumped when the wire format changes; a mismatch reads as an empty book. */
    public static final int FORMAT = 1;

    /** Which {@link JournalStepSource} built the steps, for the status line. */
    public String sourceName = "";
    public final List<JournalChapter> chapters = new ArrayList<>();

    public JournalChapter chapter(int realm) {
        for (JournalChapter chapter : this.chapters) {
            if (chapter.realm == realm) {
                return chapter;
            }
        }
        return null;
    }

    public int stepCount() {
        int steps = 0;
        for (JournalChapter chapter : this.chapters) {
            steps += chapter.steps.size();
        }
        return steps;
    }

    public void write(PacketWriter writer) {
        writer.putNextShortUnsigned(FORMAT);
        writer.putNextString(this.sourceName == null ? "" : this.sourceName);
        writer.putNextByteUnsigned(this.chapters.size());
        for (JournalChapter chapter : this.chapters) {
            chapter.write(writer);
        }
    }

    public static JournalBook read(PacketReader reader) {
        JournalBook book = new JournalBook();
        if (reader.getNextShortUnsigned() != FORMAT) {
            return book;
        }
        book.sourceName = reader.getNextString();
        int chapters = reader.getNextByteUnsigned();
        for (int i = 0; i < chapters; i++) {
            book.chapters.add(JournalChapter.read(reader));
        }
        return book;
    }

    /** The whole book as a packet body, e.g. to measure or round-trip it. */
    public Packet toPacket() {
        Packet packet = new Packet();
        this.write(new PacketWriter(packet));
        return packet;
    }

    static void writeMessage(PacketWriter writer, GameMessage message) {
        writer.putNextBoolean(message != null);
        if (message != null) {
            message.writePacket(writer);
        }
    }

    static GameMessage readMessage(PacketReader reader) {
        return reader.getNextBoolean() ? GameMessage.fromPacket(reader) : null;
    }
}
