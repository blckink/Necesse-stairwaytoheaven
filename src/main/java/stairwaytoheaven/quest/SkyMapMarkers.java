package stairwaytoheaven.quest;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketAddMapMarker;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MapIconRegistry;
import stairwaytoheaven.SkyRegistry;

/**
 * Delivers the Skyreach world-map markers (Warden's Spire + the player's
 * arrival stairway). Client map markers persist in the client's per-world map
 * file, so a single send is permanent; the per-player sent-sets persist with
 * the quest data so nobody gets duplicates — and a player who deletes a
 * marker on purpose is not spammed with it again.
 */
public final class SkyMapMarkers {

    private SkyMapMarkers() {
    }

    /** On ascent both positions are known: deliver spire + stairway markers. */
    public static void onAscent(ServerClient client, SkywatchQuestData quest, int stairsTileX, int stairsTileY) {
        boolean any = sendSpire(client, quest);
        if (client != null && quest.stairsMarkerAuths.add(client.authentication)) {
            client.sendPacket(new PacketAddMapMarker(MapIconRegistry.getIcon("skystairs"),
                    new LocalMessage("misc", "stairsmarker"),
                    SkyRegistry.SKYREACH_IDENTIFIER, stairsTileX, stairsTileY));
            any = true;
        }
        if (any) {
            client.sendChatMessage(new LocalMessage("misc", "markersadded"));
        }
    }

    /**
     * Rescue path via the status command: the stairway position is unknown
     * here, so only the spire marker is delivered (the stairway marker still
     * arrives on the player's next ascent).
     */
    public static void onLocator(ServerClient client, SkywatchQuestData quest) {
        if (sendSpire(client, quest)) {
            client.sendChatMessage(new LocalMessage("misc", "markersadded"));
        }
    }

    private static boolean sendSpire(ServerClient client, SkywatchQuestData quest) {
        if (client == null || !quest.spirePlaced || !quest.spireMarkerAuths.add(client.authentication)) {
            return false;
        }
        client.sendPacket(new PacketAddMapMarker(MapIconRegistry.getIcon("skyspire"),
                new LocalMessage("misc", "spiremarker"),
                SkyRegistry.SKYREACH_IDENTIFIER, quest.spireX, quest.spireY));
        return true;
    }
}
