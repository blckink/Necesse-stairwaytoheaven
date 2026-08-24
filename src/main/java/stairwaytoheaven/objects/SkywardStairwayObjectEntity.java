package stairwaytoheaven.objects;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.ComputedFunction;
import necesse.entity.objectEntity.PortalObjectEntity;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkyMapMarkers;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.worldgen.SkyOrigin;

import java.awt.Point;

/**
 * Portal entity of the surface-side Stairway to Heaven.
 *
 * v0.5 DESIGN: the stairway is a PORTAL, not a coordinate ladder. No matter
 * where on the surface it is placed or used, it always opens onto the canonical
 * Skyreach arrival point at the Old Warden Spire hub — the radial center of the
 * whole sky. This kills the placement exploit outright: a player cannot walk
 * 10,000 tiles from spawn, place another stairway, and skip straight into a
 * distant high-intensity region. Progression radiates from the hub; every
 * ascent starts there.
 *
 * The return trip is the Skywatch Gate at the spire (see
 * {@link SkywatchGateObjectEntity}), which routes each player back to the
 * stairway they ascended from — recorded server-side here at ascent time.
 *
 * The vanilla ladder-entity flow (blocked-exit check, lazy level generation,
 * mob clearing, ladder-use stat) is kept; the old auto-placed return stairway
 * is gone, and a Mistsea landing at the arrival tile still forms Cloudturf as
 * a belt-and-braces measure (the painter already guarantees hub land).
 */
public class SkywardStairwayObjectEntity extends PortalObjectEntity {

    public SkywardStairwayObjectEntity(Level level, int x, int y) {
        super(level, "skystairwaydown", x, y, SkyRegistry.SKYREACH_IDENTIFIER,
                SkyOrigin.arrival(level.getWorldEntity()).x,
                SkyOrigin.arrival(level.getWorldEntity()).y);
        this.saveDestination = false;
    }

    @Override
    public void use(Server server, ServerClient client) {
        int arrivalX = this.destinationTileX;
        int arrivalY = this.destinationTileY;
        ComputedFunction<Level, GameMessage> isBlockingExit = new ComputedFunction<>(level -> {
            level.regionManager.ensureTilesAreLoaded(arrivalX, arrivalY, arrivalX, arrivalY);
            return level.preventsLadderPlacement(arrivalX, arrivalY);
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

            level.regionManager.ensureTileIsLoaded(arrivalX, arrivalY);
            clearArrivalLanding(server, level, arrivalX, arrivalY);

            client.newStats.ladders_used.increment(1);
            this.runClearMobs(level, arrivalX, arrivalY);
            if (level instanceof stairwaytoheaven.level.SkyLevel) {
                // Stamp the spire at the canonical origin (idempotent) — the
                // hub exists before the player takes a step.
                ((stairwaytoheaven.level.SkyLevel) level).ensureWardenSpire();
            }
            SkywatchQuestData quest = SkywatchQuestData.get(level);
            // Bind this stairway as the player's way home (the Skywatch Gate
            // at the spire reads this back).
            quest.setReturnStairway(client.authentication, this.tileX, this.tileY);
            if (quest.stage == 0) {
                // First quest hook: a flicker over the mist points toward the
                // Warden.
                String directionWord = new LocalMessage("misc",
                        SkywatchQuestData.directionKey(arrivalX, arrivalY, quest.spireX, quest.spireY)).translate();
                client.sendChatMessage(new LocalMessage(
                        "misc", "skyreachhint", "dir", directionWord));
                // Journal entry "find the spire" — completed by the Warden's
                // first dialogue.
                stairwaytoheaven.quest.SkyQuests.giveOnce(server, client, new stairwaytoheaven.quest.FindSpireQuest());
            }
            SkyMapMarkers.onAscent(client, quest, arrivalX, arrivalY);
            return true;
        }, true);
    }

    /**
     * Clears the 3x3 arrival area (objects the vanilla ladder flow would clear)
     * and turns any Mistsea tile into Cloudturf — the arrival pad without the
     * old auto-placed return stairway. The Skywatch Gate at the spire is the
     * way home now.
     */
    public static void clearArrivalLanding(Server server, Level level, int tileX, int tileY) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int currentTileX = tileX + i;
                int currentTileY = tileY + j;
                level.regionManager.ensureTileIsLoaded(currentTileX, currentTileY);
                necesse.level.gameObject.GameObject obj = level.getObject(currentTileX, currentTileY);
                boolean shouldClearObject = obj.isClearedOnLadderPlacement(level, currentTileX, currentTileY);
                if (i == 0 && j == 0) {
                    if (!shouldClearObject && level.getObjectID(currentTileX, currentTileY) != 0) {
                        level.entityManager.destroyObjectOverride(0, currentTileX, currentTileY);
                    }
                    if (level.getTile(currentTileX, currentTileY).isLiquid) {
                        level.setTile(currentTileX, currentTileY, SkyRegistry.cloudturfID);
                    }
                } else if (shouldClearObject
                        && obj.preventsLadderPlacement(level, currentTileX, currentTileY) == null) {
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
