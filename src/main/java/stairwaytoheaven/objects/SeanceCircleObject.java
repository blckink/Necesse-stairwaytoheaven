package stairwaytoheaven.objects;

import java.awt.Color;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyOrigin;

/**
 * The Seance Circle: a chalk ring with candle stubs, craftable and placeable
 * anywhere on the surface. Interacting while carrying the Silver Bell (the
 * Warden's gift — checked, never consumed) tears it open into the Rift.
 *
 * <p>{@code docs/PLAN_ONE_PLANE.md} §A2.3: <b>the seance is fast travel to the
 * GHOST BAND of the one plane.</b> The Rift it opens is the same object it
 * always was and lands on the same ground it always did — that ground is now
 * the fen inside the Ghost Realm's band ({@code WORLD_DESIGN} §41.5) instead of
 * a dimension of its own.
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
        // In the sky a circle is a SUMMONING ring, not a door.
        //
        // Worldgen stands one at a hashed site inside every Beetle Outland
        // (SkyOutlands.isPortalSite), which is what the player asked the
        // circles to become: "seance zirkel wuerde ich stattdessen als
        // boss-portal nehmen ... an bestimmten stellen, nicht random".
        //
        // What is NOT here yet is the thing it calls. The sky has no boss, so
        // the ring says so instead of opening a rift into a layer this
        // direction is folding away. Wiring the summon is the next step and it
        // needs a boss mob to exist first; until then this is deliberately an
        // honest dead end rather than a door that contradicts the design.
        if (SkyRegistry.SKYREACH_IDENTIFIER.equals(level.getIdentifier())) {
            if (level.getBiome(x, y) == SkyRegistry.outlands) {
                level.setObject(x, y, SkyRegistry.crookedDoorDownID);
                level.getServer().network.sendToClientsWithTile(
                        new PacketChangeObject(level, 0, x, y, SkyRegistry.crookedDoorDownID),
                        level, x, y);
                client.sendChatMessage(new LocalMessage("misc", "crookeddooropened"));
                return;
            }
            // Standing in the world of the dead already: there is nowhere for
            // a seance to send you. The REALM answers that now, not the level.
            int seed = SkyOrigin.worldGenSeed(level.getWorldEntity());
            int realm = RealmDepth.realmAt(seed, x, y,
                    SkyOrigin.originX(seed), SkyOrigin.originY(seed));
            if (realm >= RealmDepth.REALM_GHOST) {
                client.sendChatMessage(new LocalMessage("misc", "seancealreadyveil"));
                return;
            }
            client.sendChatMessage(new LocalMessage("misc", "seancesilent"));
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
