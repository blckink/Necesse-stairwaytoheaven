package stairwaytoheaven.objects;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.ComputedFunction;
import necesse.entity.objectEntity.PortalObjectEntity;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.util.TileText;

/**
 * The Skywatch Gate: the permanent return portal at the Old Warden Spire.
 *
 * v0.5 DESIGN: with the stairway recast as a one-way portal to the hub, the
 * way HOME lives here. The gate routes each player back to the surface
 * stairway they last ascended from (recorded server-side per client auth at
 * ascent time — see {@link SkywardStairwayObjectEntity#use}). If the bound
 * stairway was broken in the meantime, the gate re-places it on arrival, so a
 * player can never strand themselves in the sky by mining their own base
 * entrance. A player with no binding at all (never ascended from the surface —
 * e.g. teleported in by an admin) gets a polite refusal instead of a random
 * destination — floating over the gate itself, the way vanilla's own
 * {@code EggNestObject} answers a player who pokes it (see {@link TileText}).
 */
public class SkywatchGateObjectEntity extends PortalObjectEntity {

    public SkywatchGateObjectEntity(Level level, int x, int y) {
        // The stored destination is a dummy: use() resolves the real one
        // per player from SkywatchQuestData.
        super(level, "skystairwayup", x, y, necesse.engine.util.LevelIdentifier.SURFACE_IDENTIFIER, x, y);
        this.saveDestination = false;
    }

    @Override
    public void use(Server server, ServerClient client) {
        Level skyLevel = this.getLevel();
        SkywatchQuestData quest = SkywatchQuestData.get(skyLevel);
        long[] returnTile = quest.getReturnStairway(client.authentication);
        if (returnTile == null) {
            TileText.at(client, this.tileX, this.tileY, new LocalMessage("misc", "gatenobinding"));
            return;
        }
        int targetX = (int) returnTile[0];
        int targetY = (int) returnTile[1];

        ComputedFunction<Level, GameMessage> isBlockingExit = new ComputedFunction<>(level -> {
            level.regionManager.ensureTilesAreLoaded(targetX, targetY, targetX, targetY);
            // The bound stairway itself is the intended exit — only complain
            // if something else blocks the tile.
            return level.getObjectID(targetX, targetY) == SkyRegistry.stairwayDownID
                    ? null
                    : level.preventsLadderPlacement(targetX, targetY);
        });
        if (server.world.levelManager.isLoaded(this.getDestinationIdentifier())) {
            Level surface = server.world.getLevel(this.getDestinationIdentifier());
            GameMessage error = isBlockingExit.get(surface);
            if (error != null) {
                TileText.at(client, this.tileX, this.tileY, error);
                return;
            }
        }

        // Do NOT call teleportClientToAroundDestination: it reads this entity's
        // destinationTileX/Y, which is a dummy here (the gate resolves a
        // DIFFERENT destination per player). Worse, it reads them inside a
        // lambda that can run later, once the surface has loaded — so writing
        // the fields per player would be a multiplayer race, one player's
        // destination landing another player's jump.
        //
        // This is the same call vanilla's method makes, with targetX/targetY
        // captured in locals instead. The old code computed them correctly and
        // then teleported to the dummy, which is why the return gate put
        // everyone back on the surface at their SKY coordinates.
        this.teleportBoundClient(client, targetX, targetY, level -> {
            if (!isBlockingExit.isComputed()) {
                GameMessage error = isBlockingExit.get(level);
                if (error != null) {
                    TileText.at(client, this.tileX, this.tileY, error);
                    return false;
                }
            }

            level.regionManager.ensureTileIsLoaded(targetX, targetY);
            // Re-place the bound stairway if it was broken while the player
            // was in the sky — the gate always leaves a way back down.
            if (level.getObjectID(targetX, targetY) != SkyRegistry.stairwayDownID) {
                restoreBoundStairway(server, level, targetX, targetY);
            }

            client.newStats.ladders_used.increment(1);
            this.runClearMobs(level, targetX, targetY);
            return true;
        });
    }

    /**
     * Vanilla's teleportClientToAroundDestination with the destination passed
     * in rather than read off the entity, so each player lands on their own
     * bound stairway.
     */
    private void teleportBoundClient(ServerClient client, int targetX, int targetY,
                                     java.util.function.Predicate<Level> validCheck) {
        client.changeLevelCheck(this.getDestinationIdentifier(), null, level -> {
            if (!validCheck.test(level)) {
                return new necesse.engine.util.TeleportResult(false, null);
            }
            java.awt.Point spot = getTeleportDestinationAroundObject(
                    level, client.playerMob, targetX, targetY, true);
            if (spot == null) {
                spot = new java.awt.Point(targetX * 32 + 16, targetY * 32 + 16);
            }
            return new necesse.engine.util.TeleportResult(true, spot);
        }, true);
    }

    /**
     * Surface-side variant of the vanilla ladder landing: clears the 3x3 area,
     * re-places the bound stairway, and fills liquid with dirt (the vanilla
     * ladder idiom — this is the player's home terrain, not sky ground).
     */
    private static void restoreBoundStairway(Server server, Level level, int tileX, int tileY) {
        GameObject stairway = ObjectRegistry.getObject(SkyRegistry.stairwayDownID);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int currentTileX = tileX + i;
                int currentTileY = tileY + j;
                level.regionManager.ensureTileIsLoaded(currentTileX, currentTileY);
                GameObject obj = level.getObject(currentTileX, currentTileY);
                boolean shouldClearObject = obj.isClearedOnLadderPlacement(level, currentTileX, currentTileY);
                if (i == 0 && j == 0) {
                    if (!shouldClearObject && level.getObjectID(currentTileX, currentTileY) != 0) {
                        level.entityManager.destroyObjectOverride(0, currentTileX, currentTileY);
                    }
                    stairway.placeObject(level, currentTileX, currentTileY, 0, false);
                    if (level.getTile(currentTileX, currentTileY).isLiquid) {
                        level.setTile(currentTileX, currentTileY, TileRegistry.dirtID);
                    }
                    server.network.sendToClientsWithTile(
                            new PacketChangeObject(level, 0, currentTileX, currentTileY, SkyRegistry.stairwayDownID),
                            level, currentTileX, currentTileY);
                } else if (shouldClearObject
                        && obj.preventsLadderPlacement(level, currentTileX, currentTileY) == null) {
                    level.setObject(currentTileX, currentTileY, 0);
                    server.network.sendToClientsWithTile(
                            new PacketChangeObject(level, 0, currentTileX, currentTileY, 0),
                            level, currentTileX, currentTileY);
                }
                if (level.getTile(currentTileX, currentTileY).isLiquid) {
                    level.sendTileChangePacket(server, currentTileX, currentTileY, TileRegistry.dirtID);
                }
            }
        }
    }
}
