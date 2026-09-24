package stairwaytoheaven.journal;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

/**
 * Client to server: "send me my journal again" — the form's refresh button.
 *
 * <p>Carries nothing. The server answers only the client that asked, with
 * that client's own book, so no player can read another's progress through
 * it; {@link AdventurerJournal#sendTo} rate-limits it.
 */
public class PacketJournalRequest extends Packet {

    public PacketJournalRequest(byte[] data) {
        super(data);
    }

    public PacketJournalRequest() {
    }

    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient client) {
        if (server != null && client != null) {
            AdventurerJournal.sendTo(server, client, true);
        }
    }
}
