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
import java.awt.Point;

import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.RealmLanding;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.util.TileText;

/**
 * A DOOR BETWEEN BANDS, not a level teleport.
 *
 * <p>{@code docs/PLAN_ONE_PLANE.md} item 6: <i>"The realm gate objects that
 * were built as level portals become either doors between bands on the plane or
 * house anchors per §A2.3 -- not level teleports."</i> This is the first kind.
 * The realm it opens onto is a BAND of the sky plane now, so the destination
 * identifier is {@code skyreach2} and the destination TILE is a landing inside
 * that band, computed by {@link RealmLanding} from the world seed and the
 * door's own position. The door still means "you are arriving somewhere", and
 * the somewhere is a place the player could also have walked to.
 *
 * <p><b>It no longer places a return gate.</b> A ladder pair only makes sense
 * between two levels; on one plane both halves stand on the same level and the
 * return half would send the player to the tile it is standing on. The way back
 * is to walk -- which is what a connected overworld is for -- until §A2.3's
 * Warden's-house anchors land. The arrival square is still cleared and any
 * liquid in it still reclaimed, so nobody is dropped into the water.
 */
public class CrookedDoorObjectEntity extends PortalObjectEntity {

    public CrookedDoorObjectEntity(Level level, int x, int y) {
        this(level, x, y, landing(level, x, y));
    }

    private CrookedDoorObjectEntity(Level level, int x, int y, Point landing) {
        super(level, "crookeddoordown", x, y, SkyRegistry.SKYREACH_IDENTIFIER, landing.x, landing.y);
        this.saveDestination = false;
    }

    /** Where this door puts the player down inside Crooked Beyond's band. */
    private static Point landing(Level level, int x, int y) {
        return RealmLanding.find(SkyOrigin.worldGenSeed(level.getWorldEntity()),
                RealmDepth.REALM_CROOKED, x, y);
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
            clearAndPlaceCrookedLanding(server, level, this.destinationTileX, this.destinationTileY, 0);

            client.newStats.ladders_used.increment(1);
            this.runClearMobs(level, this.destinationTileX, this.destinationTileY);

            // First line of the Crooked chain, mirroring how the Skyward
            // Stairway hands FindSpireQuest on a player's first ascent. Guarded
            // on the world record, not on "does the client hold one": a player
            // who already paid Mr. Knott his three materials must not be handed
            // the signpost again on their next walk through a door.
            if (!stairwaytoheaven.quest.SkywatchWorldData.crookedDoorwayOpened(server)) {
                stairwaytoheaven.quest.SkyQuests.giveOnce(server, client,
                        new stairwaytoheaven.quest.CrookedArrivalQuest());
            }
            return true;
        }, true);
    }

    /**
     * Crooked-side variant of {@code LadderDownObjectEntity.clearAndPlaceLadder}:
     * clears the 3x3 arrival area and reclaims any Spill in that square as the
     * realm's own striped ground.
     *
     * <p>{@code doorObjectID} 0 places nothing at the centre, which is what the
     * one-plane door passes: there is no return half any more.
     */
    public static void clearAndPlaceCrookedLanding(Server server, Level level, int tileX, int tileY,
            int doorObjectID) {
        GameObject doorObject = doorObjectID != 0 ? ObjectRegistry.getObject(doorObjectID) : null;

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

                    // doorObjectID 0 means "place nothing": the one-plane door
                    // has no return half to stand here (see the class header).
                    if (doorObject != null) {
                        doorObject.placeObject(level, currentTileX, currentTileY, 0, false);
                    } else {
                        level.setObject(currentTileX, currentTileY, 0);
                    }
                    if (level.getTile(currentTileX, currentTileY).isLiquid) {
                        level.setTile(currentTileX, currentTileY, CrookedRealm.crookedStripeID);
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
                                CrookedRealm.crookedStripeID);
                    }
                }
            }
        }
    }
}
