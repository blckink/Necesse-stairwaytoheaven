package stairwaytoheaven.pickupfilter;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.itemFilter.ItemCategoriesFilter;

/**
 * Client to server: the sender's complete pickup filter.
 *
 * <p>A plain packet rather than a container event for the same reason as
 * {@code PacketTraitSwapResult}: {@code PacketRegistry} sits in the same place
 * in both jars this mod is built against.
 */
public class PacketPickupFilter extends Packet {

    private final ItemCategoriesFilter filter;

    public PacketPickupFilter(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.filter = new ItemCategoriesFilter(true);
        this.filter.readPacket(reader);
    }

    public PacketPickupFilter(ItemCategoriesFilter filter) {
        this.filter = filter;
        PacketWriter writer = new PacketWriter(this);
        filter.writePacket(writer);
    }

    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient client) {
        if (client != null) {
            PickupFilter.setServerFilter(client.authentication, this.filter);
        }
    }
}
