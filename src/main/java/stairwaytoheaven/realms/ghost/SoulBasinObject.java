package stairwaytoheaven.realms.ghost;

import java.awt.Color;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;

/**
 * The Soul Basin: a standing bowl of cold fire that opens the way into the
 * Aftergarden when it is filled.
 *
 * <p>{@code WORLD_DESIGN} §10 lists the Spirit Basin among the realm's world
 * objects; this is that object with a job. It is the Ghost Realm's counterpart
 * to the Seance Circle — craftable, placeable anywhere in the living world, and
 * useless until the player brings what it wants.
 *
 * <p><b>What it wants is {@link #ECTOPLASM_COST} ectoplasm, and it CONSUMES
 * it.</b> That is the one place this deliberately differs from the Seance
 * Circle, which only checks for the Silver Bell and never takes it: the bell is
 * a key the Warden gave you, and a key you keep. Ectoplasm is a material the
 * dead are made of, and pouring it into the basin is the price of the door. The
 * player therefore has to have been to the Veil (whose shades drop ectoplasm,
 * as vanilla's own deep-cave spirits do) before the Aftergarden opens at all,
 * which is the progression gate this realm needs and the reason the cost is not
 * cosmetic.
 *
 * <p>Once filled the basin becomes a {@link GhostGateObject} on the same tile,
 * exactly the way the circle becomes the rift.
 */
public class SoulBasinObject extends GhostDecoObject {

    /**
     * Enough that a player cannot open it by accident on the way past a single
     * shade, small enough that it is not a grind: vanilla's own deep-cave
     * spirit drops 1-2 ectoplasm and the Ghost Realm's own Drifter drops 2-4,
     * so this is a handful of kills or one Veil run.
     */
    public static final int ECTOPLASM_COST = 12;

    public SoulBasinObject() {
        // World sheet: the game's own spiritbasin (a stone bowl of pale fire).
        // Icon: the game's own items/spiritbasin. Both borrowed by literal
        // path -- see docs/realms/ghost.md.
        super("spiritbasin", "spiritbasin", 32, new Color(84, 190, 176), null,
                "objects", "misc");
        this.setLight(80, 0.47F, 0.55F);
    }

    @Override
    public boolean canInteract(Level level, int x, int y, PlayerMob player) {
        return true;
    }

    /** Is this tile inside the given realm's band of the sky plane? */
    public static boolean inRealm(Level level, int tileX, int tileY, int realm) {
        if (!SkyRegistry.SKYREACH_IDENTIFIER.equals(level.getIdentifier())) {
            return false;
        }
        int seed = stairwaytoheaven.worldgen.SkyOrigin.worldGenSeed(level.getWorldEntity());
        return stairwaytoheaven.worldgen.RealmDepth.realmAt(seed, tileX, tileY,
                stairwaytoheaven.worldgen.SkyOrigin.originX(seed),
                stairwaytoheaven.worldgen.SkyOrigin.originY(seed)) == realm;
    }

    @Override
    public void interact(Level level, int x, int y, PlayerMob player) {
        if (!level.isServer() || !player.isServerClient()) {
            return;
        }
        ServerClient client = player.getServerClient();
        // "You are already there" is now a question about the REALM, not about
        // which level you stand on: docs/PLAN_ONE_PLANE.md retired the realm
        // dimensions, so the band under your feet is what answers it.
        if (inRealm(level, x, y, stairwaytoheaven.worldgen.RealmDepth.REALM_GHOST)) {
            client.sendChatMessage(new LocalMessage("misc", "basinalreadyghost"));
            return;
        }
        int ectoplasm = player.getInv().main.getAmount(level, player,
                ItemRegistry.getItem("ectoplasm"), "soulbasin");
        if (ectoplasm < ECTOPLASM_COST) {
            client.sendChatMessage(new LocalMessage("misc", "basinneedsectoplasm"));
            return;
        }
        player.getInv().main.removeItems(level, player, ItemRegistry.getItem("ectoplasm"),
                ECTOPLASM_COST, "soulbasin");
        level.setObject(x, y, GhostRealm.gateDownID);
        level.getServer().network.sendToClientsWithTile(
                new PacketChangeObject(level, 0, x, y, GhostRealm.gateDownID), level, x, y);
        client.sendChatMessage(new LocalMessage("misc", "ghostgateopened"));
    }
}
