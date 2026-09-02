package stairwaytoheaven.realms.ghost;

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

/**
 * Portal entity of the living-world side of the Ghost Gate.
 *
 * <p>The same proven flow the Veil rift and the sky stairway already use:
 * check the far side is not blocked, generate it lazily, place the return gate,
 * clear the mobs standing on the arrival tile. The one thing it adds is the
 * landing: the Aftergarden is mostly ectoplasm at its coasts, so a gate whose
 * far end lands in liquid would drop the player into the marsh. Any liquid in
 * the 3x3 arrival square is turned into haunted grass first.
 */
public class GhostGateObjectEntity extends PortalObjectEntity {

    public GhostGateObjectEntity(Level level, int x, int y) {
        super(level, "ghostgatedown", x, y, SkyRegistry.GHOST_IDENTIFIER, x, y);
        this.saveDestination = false;
    }

    @Override
    public void use(Server server, ServerClient client) {
        ComputedFunction<Level, GameMessage> isBlockingExit = new ComputedFunction<>(level -> {
            level.regionManager.ensureTilesAreLoaded(this.destinationTileX, this.destinationTileY,
                    this.destinationTileX, this.destinationTileY);
            return level.getObjectID(this.destinationTileX, this.destinationTileY) != GhostRealm.gateUpID
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
            if (level.getObjectID(this.destinationTileX, this.destinationTileY) != GhostRealm.gateUpID) {
                clearAndPlaceGhostLanding(server, level, this.destinationTileX, this.destinationTileY,
                        GhostRealm.gateUpID);
            }

            client.newStats.ladders_used.increment(1);
            this.runClearMobs(level, this.destinationTileX, this.destinationTileY);
            return true;
        }, true);
    }

    /**
     * Ghost-side variant of {@code LadderDownObjectEntity.clearAndPlaceLadder}:
     * clears the 3x3 arrival area, places the return gate, and turns any
     * ectoplasm in it into a haunted-grass landing.
     */
    public static void clearAndPlaceGhostLanding(Server server, Level level, int tileX, int tileY,
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
                        level.setTile(currentTileX, currentTileY, GhostRealm.hauntedGrassID);
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
                        level.sendTileChangePacket(server, currentTileX, currentTileY, GhostRealm.hauntedGrassID);
                    }
                }
            }
        }
    }
}
