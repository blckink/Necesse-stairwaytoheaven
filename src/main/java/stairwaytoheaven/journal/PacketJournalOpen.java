package stairwaytoheaven.journal;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;

/**
 * Server to client: one player's journal, built from server state, and the
 * instruction to show it.
 *
 * <p>A plain packet rather than a container, for the reason
 * {@code settlement/PacketTraitSwapResult} records: {@code PacketRegistry} is
 * in the same place in the 1.3.2 jar every gate builds against and the 1.3.3
 * client the mod ships for, and a custom container's registry call is the
 * part of that pair nobody has verified.
 */
public class PacketJournalOpen extends Packet {

    public final JournalBook book;

    public PacketJournalOpen(byte[] data) {
        super(data);
        this.book = JournalBook.read(new PacketReader(this));
    }

    public PacketJournalOpen(JournalBook book) {
        this.book = book;
        book.write(new PacketWriter(this));
    }

    @Override
    public void processClient(NetworkPacket packet, Client client) {
        JournalForm.show(client, this.book);
    }
}
