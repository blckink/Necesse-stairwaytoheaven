package stairwaytoheaven.objects;

import java.awt.Color;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;

/**
 * The Seance Circle: a chalk ring with candle stubs, craftable and placeable
 * anywhere on the surface. Interacting while carrying the Silver Bell (the
 * Warden's gift — checked, never consumed) tears it open into the Rift.
 */
public class SeanceCircleObject extends SkyDecoObject {

    public SeanceCircleObject() {
        super("seancecircle", 32, new Color(120, 150, 130), null, "objects", "misc");
        this.setLight(60, 0.38F, 0.45F);
    }

    @Override
    public boolean canInteract(Level level, int x, int y, PlayerMob player) {
        return true;
    }

    @Override
    public void interact(Level level, int x, int y, PlayerMob player) {
        if (!level.isServer() || !player.isServerClient()) {
            return;
        }
        ServerClient client = player.getServerClient();
        if (SkyRegistry.VEIL_IDENTIFIER.equals(level.getIdentifier())) {
            client.sendChatMessage(new LocalMessage("misc", "seancealreadyveil"));
            return;
        }
        int bells = player.getInv().main.getAmount(level, player,
                ItemRegistry.getItem("silverbell"), "seance");
        if (bells <= 0) {
            client.sendChatMessage(new LocalMessage("misc", "seanceneedbell"));
            return;
        }
        // the bell is the key, not the fuel — it stays with the player
        level.setObject(x, y, SkyRegistry.veilRiftDownID);
        level.getServer().network.sendToClientsWithTile(
                new PacketChangeObject(level, 0, x, y, SkyRegistry.veilRiftDownID),
                level, x, y);
        client.sendChatMessage(new LocalMessage("misc", "riftopened"));
    }
}
