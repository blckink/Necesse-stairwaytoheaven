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
import java.awt.Point;

import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.RealmLanding;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.util.TileText;

/**
 * Portal entity of the seance rift: same proven flow as the sky stairway
 * (blocked-exit check, lazy generation, mob clearing), landing on reclaimed
 * murkmoss when the far side is water.
 *
 * <p><b>Its destination is the Ghost band of the one plane</b>, not a Veil
 * dimension — see {@link VeilRiftObject}'s header and
 * {@code docs/PLAN_ONE_PLANE.md}. Like the three realm doors it no longer
 * places a return rift: both halves would stand on the same level, so the
 * return half would send the player to the tile it is standing on.
 */
public class VeilRiftObjectEntity extends PortalObjectEntity {

    public VeilRiftObjectEntity(Level level, int x, int y) {
        this(level, x, y, landing(level, x, y));
    }

    private VeilRiftObjectEntity(Level level, int x, int y, Point landing) {
        super(level, "veilriftdown", x, y, SkyRegistry.SKYREACH_IDENTIFIER, landing.x, landing.y);
        this.saveDestination = false;
    }

    /** Where the seance puts the player down inside the Ghost Realm's band. */
    private static Point landing(Level level, int x, int y) {
        return RealmLanding.find(SkyOrigin.worldGenSeed(level.getWorldEntity()),
                RealmDepth.REALM_GHOST, x, y);
    }

    @Override
    public void use(Server server, ServerClient client) {
        ComputedFunction<Level, GameMessage> isBlockingExit = new ComputedFunction<>(level -> {
            level.regionManager.ensureTilesAreLoaded(this.destinationTileX, this.destinationTileY, this.destinationTileX, this.destinationTileY);
            return level.getObjectID(this.destinationTileX, this.destinationTileY) != SkyRegistry.veilRiftUpID
                    ? level.preventsLadderPlacement(this.destinationTileX, this.destinationTileY)
                    : null;
        });
        if (server.world.levelManager.isLoaded(this.getDestinationIdentifier())) {
            Level level = server.world.getLevel(this.getDestinationIdentifier());
            GameMessage error = isBlockingExit.get(level);
            if (error != null) {
                TileText.at(client, this.tileX, this.tileY, error);
                return;
            }
        }

        this.teleportClientToAroundDestination(client, level -> {
            if (!isBlockingExit.isComputed()) {
                GameMessage error = isBlockingExit.get(level);
                if (error != null) {
                    TileText.at(client, this.tileX, this.tileY, error);
                    return false;
                }
            }

            level.regionManager.ensureTileIsLoaded(this.destinationTileX, this.destinationTileY);
            clearAndPlaceVeilLanding(server, level, this.destinationTileX, this.destinationTileY, 0);

            client.newStats.ladders_used.increment(1);
            this.runClearMobs(level, this.destinationTileX, this.destinationTileY);
            return true;
        }, true);
    }

    /**
     * Clears the 3x3 arrival area and turns any liquid in it into a murkmoss
     * landing, so a rift whose far end falls in the fen does not drop the player
     * in the water.
     *
     * <p>{@code stairwayObjectID} 0 places nothing at the centre, which is what
     * the one-plane rift passes: there is no return half any more.
     */
    public static void clearAndPlaceVeilLanding(Server server, Level level, int tileX, int tileY, int stairwayObjectID) {
        GameObject stairwayObject = stairwayObjectID != 0
                ? ObjectRegistry.getObject(stairwayObjectID) : null;

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

                    if (stairwayObject != null) {
                        stairwayObject.placeObject(level, currentTileX, currentTileY, 0, false);
                    } else {
                        level.setObject(currentTileX, currentTileY, 0);
                    }
                    if (level.getTile(currentTileX, currentTileY).isLiquid) {
                        level.setTile(currentTileX, currentTileY, SkyRegistry.murkmossID);
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
                        level.sendTileChangePacket(server, currentTileX, currentTileY, SkyRegistry.murkmossID);
                    }
                }
            }
        }
    }
}
