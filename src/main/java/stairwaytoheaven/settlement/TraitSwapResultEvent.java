package stairwaytoheaven.settlement;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.events.ContainerEvent;

/**
 * What came out of a trait swap, told to the one player who paid for it.
 *
 * <p>The result cannot be worked out on the client: the roll happens on the
 * server, and the whole point of the service is that the player does not know
 * what they are buying until it is bought. Vanilla answers exactly this shape
 * of question with a {@link ContainerEvent} — see
 * {@code ShopContainerPartyResponseEvent}, sent back through
 * {@code applyAndSendToClient} after the server has decided — so this is that,
 * registered in {@code SkyTherapy.register}.
 */
public class TraitSwapResultEvent extends ContainerEvent {

    /** The roll found nothing legal left to hand out; nothing was charged. */
    public static final int FAILED_NO_TRAIT = 1;
    /** The player could not pay; nothing was changed. */
    public static final int FAILED_NO_COINS = 2;

    public final int mobUniqueID;
    public final int oldPersonalityID;
    public final int newPersonalityID;
    /** 0 on success, one of the FAILED_ codes otherwise. */
    public final int failure;

    public TraitSwapResultEvent(int mobUniqueID, int oldPersonalityID, int newPersonalityID,
                                int failure) {
        this.mobUniqueID = mobUniqueID;
        this.oldPersonalityID = oldPersonalityID;
        this.newPersonalityID = newPersonalityID;
        this.failure = failure;
    }

    public TraitSwapResultEvent(PacketReader reader) {
        super(reader);
        this.mobUniqueID = reader.getNextInt();
        this.oldPersonalityID = reader.getNextShortUnsigned();
        this.newPersonalityID = reader.getNextShortUnsigned();
        this.failure = reader.getNextByteUnsigned();
    }

    @Override
    public void write(PacketWriter writer) {
        writer.putNextInt(this.mobUniqueID);
        writer.putNextShortUnsigned(this.oldPersonalityID);
        writer.putNextShortUnsigned(this.newPersonalityID);
        writer.putNextByteUnsigned(this.failure);
    }
}
