package stairwaytoheaven.settlement;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;

/**
 * What came out of a trait swap, told to the one player who paid for it.
 *
 * <p>The result cannot be worked out on the client: the roll happens on the
 * server, and the whole point of the service is that the player does not know
 * what they are buying until it is bought.
 *
 * <h2>Why a packet and not a {@code ContainerEvent}</h2>
 *
 * A container event is what vanilla uses for exactly this shape of answer
 * ({@code ShopContainerPartyResponseEvent}), and the first cut of this feature
 * used one. It does not survive this repository's two build targets:
 * {@code ContainerEventRegistry} sits in {@code necesse.engine.registries} in
 * the 1.3.2 dedicated server every gate runs against, and in
 * {@code necesse.inventory.container.events} in the 1.3.3 client the mod is
 * shipped for. Registering it would compile against one and fail against the
 * other — i.e. either the mod cannot be gated or it cannot be played.
 *
 * <p>{@code PacketRegistry} is in the same place in both (VERIFIED against both
 * jars), so the answer travels as a plain packet and is handed to whichever
 * trait dialogue is open through {@link TraitTherapyDialogue#deliverResult}.
 */
public class PacketTraitSwapResult extends Packet {

    /** The roll found nothing legal left to hand out; nothing was charged. */
    public static final int FAILED_NO_TRAIT = 1;
    /** The player could not pay; nothing was changed. */
    public static final int FAILED_NO_COINS = 2;

    /** Which Therapist was being talked to, so a stale menu ignores this. */
    public final int therapistUniqueID;
    public final int mobUniqueID;
    public final int oldPersonalityID;
    public final int newPersonalityID;
    /** 0 on success, one of the FAILED_ codes otherwise. */
    public final int failure;

    public PacketTraitSwapResult(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.therapistUniqueID = reader.getNextInt();
        this.mobUniqueID = reader.getNextInt();
        this.oldPersonalityID = reader.getNextShortUnsigned();
        this.newPersonalityID = reader.getNextShortUnsigned();
        this.failure = reader.getNextByteUnsigned();
    }

    public PacketTraitSwapResult(int therapistUniqueID, int mobUniqueID, int oldPersonalityID,
                                 int newPersonalityID, int failure) {
        this.therapistUniqueID = therapistUniqueID;
        this.mobUniqueID = mobUniqueID;
        this.oldPersonalityID = oldPersonalityID;
        this.newPersonalityID = newPersonalityID;
        this.failure = failure;
        PacketWriter writer = new PacketWriter(this);
        writer.putNextInt(therapistUniqueID);
        writer.putNextInt(mobUniqueID);
        writer.putNextShortUnsigned(oldPersonalityID);
        writer.putNextShortUnsigned(newPersonalityID);
        writer.putNextByteUnsigned(failure);
    }

    @Override
    public void processClient(NetworkPacket packet, Client client) {
        TraitTherapyDialogue.deliverResult(this);
    }
}
