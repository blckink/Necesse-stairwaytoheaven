package stairwaytoheaven.settlement;

import java.util.ArrayList;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;

/**
 * "This settler's traits just changed" — sent to everyone who can see them.
 *
 * <h2>Why this is the only network code the feature needs</h2>
 *
 * A settler's personality list is not derived state that each side works out
 * for itself. {@code HumanMob.setupSpawnPacket} writes the list into the mob's
 * spawn packet and {@code applySpawnPacket} reads it back, so a client that
 * loads the settler AFTER a trait swap already sees the new trait, and a save
 * written after one already holds it ({@code addSaveData} /
 * {@code applyLoadData}, which reads the saved list back after the seed has
 * regenerated the default).
 *
 * <p>The single gap is a client that had the settler on screen when the swap
 * happened: it keeps the list it was given at spawn time until the mob leaves
 * its loaded regions. This closes that, and nothing else.
 */
public class PacketSettlerPersonalities extends Packet {

    public final int mobUniqueID;
    public final int[] personalityIDs;

    public PacketSettlerPersonalities(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.mobUniqueID = reader.getNextInt();
        int size = reader.getNextShortUnsigned();
        this.personalityIDs = new int[size];
        for (int i = 0; i < size; i++) {
            this.personalityIDs[i] = reader.getNextShortUnsigned();
        }
    }

    public PacketSettlerPersonalities(HumanMob mob) {
        ArrayList<SettlerPersonality> personalities = mob.getPersonalities();
        this.mobUniqueID = mob.getUniqueID();
        this.personalityIDs = new int[personalities.size()];
        for (int i = 0; i < this.personalityIDs.length; i++) {
            this.personalityIDs[i] = personalities.get(i).getID();
        }
        PacketWriter writer = new PacketWriter(this);
        writer.putNextInt(this.mobUniqueID);
        writer.putNextShortUnsigned(this.personalityIDs.length);
        for (int id : this.personalityIDs) {
            writer.putNextShortUnsigned(id);
        }
    }

    @Override
    public void processClient(NetworkPacket packet, Client client) {
        Level level = client.getLevel();
        if (level == null) {
            return;
        }
        Mob mob = level.entityManager.mobs.get(this.mobUniqueID, false);
        if (!(mob instanceof HumanMob)) {
            return;
        }
        HumanMob human = (HumanMob) mob;
        ArrayList<SettlerPersonality> personalities = human.getPersonalities();
        personalities.clear();
        for (int id : this.personalityIDs) {
            SettlerPersonality personality =
                    SettlerPersonalityRegistry.getNewSettlerPersonality(id, human);
            if (personality == null) {
                continue;
            }
            personality.init();
            personalities.add(personality);
        }
        // Mood lines submitted by the trait that just left have to go with it,
        // exactly as on the server side of the swap.
        human.getActiveThoughts().init();
    }
}
