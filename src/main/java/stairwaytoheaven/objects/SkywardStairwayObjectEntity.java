package stairwaytoheaven.objects;

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
 * Portal entity of the surface-side stairway. Mirrors the vanilla
 * LadderDownObjectEntity flow (blocked-exit check, lazy level generation,
 * counterpart placement, mob clearing, ladder-use stat) with one difference:
 * where the vanilla ladder fills destination liquid with dirt, ascending onto
 * the Mistsea forms a small Cloudturf landing instead — dirt islands in the sky
 * would break the scene.
 */
public class SkywardStairwayObjectEntity extends PortalObjectEntity {

    public SkywardStairwayObjectEntity(Level level, int x, int y) {
        super(level, "skystairwaydown", x, y, SkyRegistry.SKYREACH_IDENTIFIER, x, y);
        this.saveDestination = false;
    }

    @Override
    public void use(Server server, ServerClient client) {
        ComputedFunction<Level, GameMessage> isBlockingExit = new ComputedFunction<>(level -> {
            level.regionManager.ensureTilesAreLoaded(this.destinationTileX, this.destinationTileY, this.destinationTileX, this.destinationTileY);
            return level.getObjectID(this.destinationTileX, this.destinationTileY) != SkyRegistry.stairwayUpID
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
            if (level.getObjectID(this.destinationTileX, this.destinationTileY) != SkyRegistry.stairwayUpID) {
                clearAndPlaceSkyLanding(server, level, this.destinationTileX, this.destinationTileY, SkyRegistry.stairwayUpID);
            }

            client.newStats.ladders_used.increment(1);
            this.runClearMobs(level, this.destinationTileX, this.destinationTileY);
            if (stairwaytoheaven.quest.SkywatchQuestData.get(level).stage == 0) {
                // First quest hook: a flicker over the mist points toward the Warden
                client.sendChatMessage(new necesse.engine.localization.message.LocalMessage("misc", "skyreachhint"));
            }
            return true;
        }, true);
    }

    /**
     * Sky-side variant of LadderDownObjectEntity.clearAndPlaceLadder: clears the
     * 3x3 arrival area, places the return stairway, and turns any Mistsea tiles
     * into a Cloudturf landing.
     */
    public static void clearAndPlaceSkyLanding(Server server, Level level, int tileX, int tileY, int stairwayObjectID) {
        GameObject stairwayObject = ObjectRegistry.getObject(stairwayObjectID);

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

                    stairwayObject.placeObject(level, currentTileX, currentTileY, 0, false);
                    if (level.getTile(currentTileX, currentTileY).isLiquid) {
                        level.setTile(currentTileX, currentTileY, SkyRegistry.cloudturfID);
                    }

                    server.network.sendToClientsWithTile(
                            new PacketChangeObject(level, 0, currentTileX, currentTileY, stairwayObjectID),
                            level, currentTileX, currentTileY);
                } else {
                    if (shouldClearObject && obj.preventsLadderPlacement(level, currentTileX, currentTileY) == null) {
                        level.setObject(currentTileX, currentTileY, 0);
                        server.network.sendToClientsWithTile(
                                new PacketChangeObject(level, 0, currentTileX, currentTileY, 0),
                                level, currentTileX, currentTileY);
                    }

                    if (level.getTile(currentTileX, currentTileY).isLiquid) {
                        level.sendTileChangePacket(server, currentTileX, currentTileY, SkyRegistry.cloudturfID);
                    }
                }
            }
        }
    }
}
