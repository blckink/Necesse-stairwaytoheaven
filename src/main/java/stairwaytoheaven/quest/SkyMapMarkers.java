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
 *
 * <p><b>Nothing here announces itself.</b> Each of these three methods used to
 * follow its markers with a chat line ("misc.markersadded",
 * "misc.catmarkersadded") saying that markers had been added. Those keys are
 * gone: the marker IS the notification, it carries its own name
 * ("misc.spiremarker", "misc.stairsmarker", "misc.catmarkerblack",
 * "misc.catmarkertabby") on the world map, and it stays there permanently
 * instead of scrolling past once. Both lines were also sent from inside a
 * level-change callback, where the client is on the loading screen — the worst
 * possible moment to tell somebody anything.
 */
public final class SkyMapMarkers {

    private SkyMapMarkers() {
    }

    /** On ascent both positions are known: deliver spire + stairway markers. */
    public static void onAscent(ServerClient client, SkywatchQuestData quest, int stairsTileX, int stairsTileY) {
        sendSpire(client, quest);
        if (client != null && quest.stairsMarkerAuths.add(client.authentication)) {
            client.sendPacket(new PacketAddMapMarker(MapIconRegistry.getIcon("skystairs"),
                    new LocalMessage("misc", "stairsmarker"),
                    SkyRegistry.SKYREACH_IDENTIFIER, stairsTileX, stairsTileY));
        }
    }

    /**
     * Rescue path via the status command: the stairway position is unknown
     * here, so only the spire marker is delivered (the stairway marker still
     * arrives on the player's next ascent).
     */
    public static void onLocator(ServerClient client, SkywatchQuestData quest) {
        sendSpire(client, quest);
    }

    /**
     * The two cat lairs, delivered when the cats become an objective. Without
     * these the quest names two cats and gives the player a whole dimension to
     * search; the lairs are fixed at world generation, so pointing at them
     * costs nothing and turns the quest into a journey rather than a sweep.
     */
    public static void sendCatLairs(ServerClient client, SkywatchQuestData quest) {
        if (client == null || !quest.catsSpawned || !quest.catMarkerAuths.add(client.authentication)) {
            return;
        }
        client.sendPacket(new PacketAddMapMarker(MapIconRegistry.getIcon("skycat"),
                new LocalMessage("misc", "catmarkerblack"),
                SkyRegistry.SKYREACH_IDENTIFIER, quest.blackLairX, quest.blackLairY));
        client.sendPacket(new PacketAddMapMarker(MapIconRegistry.getIcon("skycat"),
                new LocalMessage("misc", "catmarkertabby"),
                SkyRegistry.SKYREACH_IDENTIFIER, quest.tabbyLairX, quest.tabbyLairY));
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
