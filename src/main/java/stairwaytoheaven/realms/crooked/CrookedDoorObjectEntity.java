package stairwaytoheaven.realms.crooked;

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
 * Portal entity of the Skyreach-side Crooked Door.
 *
 * <p>The same proven flow the sky stairway and the Veil rift both use —
 * blocked-exit check, lazy level generation, counterpart placement, mob clearing
 * — with one thing that has to be different: <b>where you land</b>.
 *
 * <p>A door is opened at an arbitrary tile of the Outlands and its far side is
 * placed at the SAME coordinates, because that is how {@code PortalObjectEntity}
 * works. Crooked Beyond is mostly Spill
 * ({@link CrookedTerrainPainter#ISLAND_THRESHOLD}), so those coordinates are
 * very often open liquid. {@link #clearAndPlaceCrookedLanding} therefore does
 * what {@code VeilRiftObjectEntity} does for the marsh: it clears the 3x3
 * arrival area, places the return door, and turns any liquid in that square into
 * the realm's own ground so the player steps out onto stripes rather than into
 * green.
 */
public class CrookedDoorObjectEntity extends PortalObjectEntity {

    public CrookedDoorObjectEntity(Level level, int x, int y) {
        super(level, "crookeddoordown", x, y, SkyRegistry.CROOKED_IDENTIFIER, x, y);
        this.saveDestination = false;
    }

    @Override
    public void use(Server server, ServerClient client) {
        ComputedFunction<Level, GameMessage> isBlockingExit = new ComputedFunction<>(level -> {
            level.regionManager.ensureTilesAreLoaded(this.destinationTileX, this.destinationTileY,
                    this.destinationTileX, this.destinationTileY);
            return level.getObjectID(this.destinationTileX, this.destinationTileY) != SkyRegistry.crookedDoorUpID
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
            if (level.getObjectID(this.destinationTileX, this.destinationTileY) != SkyRegistry.crookedDoorUpID) {
                clearAndPlaceCrookedLanding(server, level, this.destinationTileX, this.destinationTileY,
                        SkyRegistry.crookedDoorUpID);
            }

            client.newStats.ladders_used.increment(1);
            this.runClearMobs(level, this.destinationTileX, this.destinationTileY);
            return true;
        }, true);
    }

    /**
     * Crooked-side variant of {@code LadderDownObjectEntity.clearAndPlaceLadder}:
     * clears the 3x3 arrival area, places the return door, and reclaims any Spill
     * in that square as {@link CrookedLevel#landingTileID()}.
     */
    public static void clearAndPlaceCrookedLanding(Server server, Level level, int tileX, int tileY,
            int doorObjectID) {
        GameObject doorObject = ObjectRegistry.getObject(doorObjectID);

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

                    doorObject.placeObject(level, currentTileX, currentTileY, 0, false);
                    if (level.getTile(currentTileX, currentTileY).isLiquid) {
                        level.setTile(currentTileX, currentTileY, CrookedLevel.landingTileID());
                    }

                    server.network.sendToClientsWithTile(
                            new PacketChangeObject(level, 0, currentTileX, currentTileY, doorObjectID),
                            level, currentTileX, currentTileY);
                } else {
                    if (shouldClearObject && obj.preventsLadderPlacement(level, currentTileX, currentTileY) == null) {
                        level.setObject(currentTileX, currentTileY, 0);
                        server.network.sendToClientsWithTile(
                                new PacketChangeObject(level, 0, currentTileX, currentTileY, 0),
                                level, currentTileX, currentTileY);
                    }

                    if (level.getTile(currentTileX, currentTileY).isLiquid) {
                        level.sendTileChangePacket(server, currentTileX, currentTileY,
                                CrookedLevel.landingTileID());
                    }
                }
            }
        }
    }
}
