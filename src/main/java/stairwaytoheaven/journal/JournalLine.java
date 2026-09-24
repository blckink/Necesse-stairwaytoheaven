package stairwaytoheaven.journal;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

/**
 * One short line of a chapter that is not a quest: a realm fact (key earned,
 * boss defeated), a resident, a landmark or a lore entry.
 *
 * <p>Texts travel as {@link GameMessage}s, never as translated strings, so a
 * German client on an English server still reads German: the server says
 * WHICH key, the client translates it.
 */
public final class JournalLine {

    public final JournalStatus status;
    public final GameMessage title;
    /** May be null. */
    public final GameMessage detail;

    public JournalLine(JournalStatus status, GameMessage title, GameMessage detail) {
        this.status = status;
        this.title = title;
        this.detail = detail;
    }

    void write(PacketWriter writer) {
        writer.putNextByteUnsigned(this.status.ordinal());
        JournalBook.writeMessage(writer, this.title);
        JournalBook.writeMessage(writer, this.detail);
    }

    static JournalLine read(PacketReader reader) {
        JournalStatus status = JournalStatus.of(reader.getNextByteUnsigned());
        GameMessage title = JournalBook.readMessage(reader);
        GameMessage detail = JournalBook.readMessage(reader);
        return new JournalLine(status, title, detail);
    }
}
