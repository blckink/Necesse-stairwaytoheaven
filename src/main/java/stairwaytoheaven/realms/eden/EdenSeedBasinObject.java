package stairwaytoheaven.realms.eden;

import java.awt.Color;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.realms.ghost.GhostDecoObject;
import stairwaytoheaven.util.TileText;

/**
 * The Eden Threshold: a standing bowl the player seeds to grow a way into the
 * Garden of Eden.
 *
 * <h2>Why this exists at all</h2>
 * Eden generates, is settled by Eveleen (see {@code EdenLevel}), and hands out
 * its own quest chain ({@link stairwaytoheaven.quest.EdenArrivalQuest}) — but
 * nothing in the mod ever led a player TO it. {@code docs/realms/eden.md} says
 * so outright: "no player-facing Skyreach&#8596;Eden gate ... yet." A realm with
 * a settler and a quest but no door is a realm nobody can reach, which is
 * exactly the shape the rest of this pass exists to close. This is the door.
 *
 * <h2>Why it is two objects and not one</h2>
 * {@code LadderDownObject.canPlace} (VERIFIED [jar]) hard-refuses anywhere but
 * the Surface — {@code !level.getIdentifier().equals(SURFACE_IDENTIFIER) ?
 * "notsurface" : null} — so a portal object can never be placed by hand in the
 * Skyreach. {@code stairwaytoheaven.realms.ghost.SoulBasinObject} solved the
 * identical problem for the Ghost Realm by growing the portal out of a plain, unrestricted object
 * instead of placing one directly, and this copies that exact shape: a
 * craftable basin (this class) that becomes {@link EdenGateObject} — a
 * {@code LadderDownObject} — on the same tile once fed, which is a direct
 * {@code setObject} call and therefore never asks {@code canPlace} anything at
 * all.
 *
 * <h2>The price, and why it is this one</h2>
 * {@link #SEED_COST} {@code overgrownedenseed} — Eden grass seed, which
 * {@code EveleenMob}'s own doc calls out as coming only from a sky crate today.
 * A player has to have engaged with the Skyreach's loot before the garden it
 * came from opens, which is the progression gate this door needs, and it closes
 * a small narrative loop besides: the seed is what opens the way to where it
 * grows.
 *
 * <h2>Sprite</h2>
 * Reuses the Ghost Realm's own {@code spiritbasin} world sheet and item icon,
 * exactly as {@code SoulBasinObject} does — see
 * {@link GhostDecoObject}'s own doc comment for why the icon is a second
 * constructor argument rather than the engine default. No new art; a second
 * borrow of an existing mod file is the same zero-cost reuse the rest of this
 * mod already leans on (the Ghost Gate itself reuses {@code veilrift} a second
 * time, for the identical reason).
 */
public class EdenSeedBasinObject extends GhostDecoObject {

    /**
     * Comfortably more than one sky crate's worth (the seed is not the crate's
     * only drop) and comfortably short of a grind — a small stack the player
     * usually already has by the time they have found this recipe's other two
     * ingredients.
     */
    public static final int SEED_COST = 6;

    public EdenSeedBasinObject() {
        // World sheet + icon: the game's own spiritbasin, borrowed a second
        // time. See class doc.
        super("spiritbasin", "spiritbasin", 32, new Color(90, 196, 120), null,
                "objects", "misc");
        this.setLight(70, 0.32F, 0.55F);
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
        // "You are already there" is now a question about the REALM, not about
        // which level you stand on: docs/PLAN_ONE_PLANE.md retired the realm
        // dimensions, so the band under your feet is what answers it.
        if (stairwaytoheaven.realms.ghost.SoulBasinObject.inRealm(level, x, y,
                stairwaytoheaven.worldgen.RealmDepth.REALM_EDEN)) {
            TileText.at(client, x, y, new LocalMessage("misc", "edenbasinalreadyeden"));
            return;
        }
        int seeds = player.getInv().main.getAmount(level, player,
                ItemRegistry.getItem("overgrownedenseed"), "edenseedbasin");
        if (seeds < SEED_COST) {
            TileText.at(client, x, y, new LocalMessage("misc", "edenbasinneedsseeds"));
            return;
        }
        player.getInv().main.removeItems(level, player, ItemRegistry.getItem("overgrownedenseed"),
                SEED_COST, "edenseedbasin");
        level.setObject(x, y, EdenRealm.edenGateDownID);
        level.getServer().network.sendToClientsWithTile(
                new PacketChangeObject(level, 0, x, y, EdenRealm.edenGateDownID), level, x, y);
        TileText.at(client, x, y, new LocalMessage("misc", "edengateopened"));
    }
}
