package stairwaytoheaven.realms.eden;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.ComputedFunction;
import necesse.entity.objectEntity.PortalObjectEntity;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkyQuests;
import stairwaytoheaven.quest.SkywatchWorldData;

/**
 * Portal entity of the living-world side of the Eden Gate.
 *
 * <p>The same proven flow the Ghost Gate, the Veil rift and the sky stairway
 * all use: check the far side is not blocked, generate it lazily, place the
 * return gate, clear the mobs standing on the arrival tile. It adds one thing
 * of its own — turning any liquid in the arrival square into Eden's own soil
 * rather than the Aftergarden's grass — and one piece of quest wiring: a
 * player's first step through this gate is also the first line of
 * {@link stairwaytoheaven.quest.EdenArrivalQuest}, exactly as a player's first
 * ascent of the Skyward Stairway hands {@code FindSpireQuest}.
 */
public class EdenGateObjectEntity extends PortalObjectEntity {

    public EdenGateObjectEntity(Level level, int x, int y) {
        super(level, "edengatedown", x, y, SkyRegistry.EDEN_IDENTIFIER, x, y);
        this.saveDestination = false;
    }

    @Override
    public void use(Server server, ServerClient client) {
        ComputedFunction<Level, GameMessage> isBlockingExit = new ComputedFunction<>(level -> {
            level.regionManager.ensureTilesAreLoaded(this.destinationTileX, this.destinationTileY,
                    this.destinationTileX, this.destinationTileY);
            return level.getObjectID(this.destinationTileX, this.destinationTileY) != EdenRealm.edenGateUpID
                    ? level.preventsLadderPlacement(this.destinationTileX, this.destinationTileY)
                    : null;
        });
        if (server.world.levelManager.isLoaded(this.getDestinationIdentifier())) {
            Level level = server.world.getLevel(this.getDestinationIdentifier());
            GameMessage error = isBlockingExit.get(level);
            if (error != null) {
                client.sendChatMessage(error);
                return;
            }
        }

        this.teleportClientToAroundDestination(client, level -> {
            if (!isBlockingExit.isComputed()) {
                GameMessage error = isBlockingExit.get(level);
                if (error != null) {
                    client.sendChatMessage(error);
                    return false;
                }
            }

            level.regionManager.ensureTileIsLoaded(this.destinationTileX, this.destinationTileY);
            if (level.getObjectID(this.destinationTileX, this.destinationTileY) != EdenRealm.edenGateUpID) {
                clearAndPlaceEdenLanding(server, level, this.destinationTileX, this.destinationTileY,
                        EdenRealm.edenGateUpID);
            }

            client.newStats.ladders_used.increment(1);
            this.runClearMobs(level, this.destinationTileX, this.destinationTileY);

            // First line of the chain. Guarded on the world record rather than
            // on "does the client hold one": a player who already delivered the
            // three plants and steps back through the gate must not be handed
            // the signpost again — the chain is shared world progression, the
            // same trade-off SkywardStairwayObjectEntity makes for FindSpireQuest.
            if (!SkywatchWorldData.edenPlantsGiven(server)) {
                SkyQuests.giveOnce(server, client, new stairwaytoheaven.quest.EdenArrivalQuest());
            }
            return true;
        }, true);
    }

    /**
     * Eden-side variant of {@code LadderDownObjectEntity.clearAndPlaceLadder}:
     * clears the 3x3 arrival area, places the return gate, and turns any liquid
     * in it into Rich Eden Soil — Eden's shallow lagoons are common enough at
     * the coast that a gate whose far end lands in one would routinely drop the
     * player in the water.
     */
    public static void clearAndPlaceEdenLanding(Server server, Level level, int tileX, int tileY,
            int gateObjectID) {
        GameObject gateObject = ObjectRegistry.getObject(gateObjectID);

        for (int i = -1; i <= 1; i++) {
            int currentTileX = tileX + i;

            for (int j = -1; j <= 1; j++) {
                int currentTileY = tileY + j;
                level.regionManager.ensureTileIsLoaded(currentTileX, currentTileY);
                GameObject obj = level.getObject(currentTileX, currentTileY);
                boolean shouldClearObject = obj.isClearedOnLadderPlacement(level, currentTileX, currentTileY);
                if (i == 0 && j == 0) {
                    if (!shouldClearObject) {
                        level.entityManager.destroyObjectOverride(0, currentTileX, currentTileY);
                    }

                    gateObject.placeObject(level, currentTileX, currentTileY, 0, false);
                    if (level.getTile(currentTileX, currentTileY).isLiquid) {
                        level.setTile(currentTileX, currentTileY, EdenRealm.edenSoilID);
                    }

                    server.network.sendToClientsWithTile(
                            new PacketChangeObject(level, 0, currentTileX, currentTileY, gateObjectID),
                            level, currentTileX, currentTileY);
                } else {
                    if (shouldClearObject && obj.preventsLadderPlacement(level, currentTileX, currentTileY) == null) {
                        level.setObject(currentTileX, currentTileY, 0);
                        server.network.sendToClientsWithTile(
                                new PacketChangeObject(level, 0, currentTileX, currentTileY, 0),
                                level, currentTileX, currentTileY);
                    }

                    if (level.getTile(currentTileX, currentTileY).isLiquid) {
                        level.sendTileChangePacket(server, currentTileX, currentTileY, EdenRealm.edenSoilID);
                    }
                }
            }
        }
    }
}
