package stairwaytoheaven.settlement;

import java.awt.Color;
import java.util.ArrayList;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import stairwaytoheaven.realms.ghost.GhostDecoObject;
import stairwaytoheaven.util.TileText;

/**
 * Die Blutschale — decision E3 of {@code docs/design/concept-nightbound-dorian.md}:
 * <i>"Ein neues Möbelstück, die Blutschale, aus der er selbst trinkt: Du legst
 * Blutfläschchen hinein. Er trinkt nachts daraus, bevor er beißt."</i>
 *
 * <p>Click it with Blood Vials in your inventory and they go in (up to
 * {@link NightboundWorldData#BOWL_CAPACITY}); click it with none and it says
 * how many it holds. {@code VampireSettlerMob} drinks one from the nearest
 * filled bowl on his level when his thirst runs dry, BEFORE he would bite —
 * which is the whole point: a player who keeps the bowl filled never finds a
 * bitten settler. Breaking the bowl gives the vials back.
 *
 * <p><b>No new art.</b> It wears the game's own {@code spiritbasin} — a stone
 * bowl — as world sheet and icon, the third borrow of that sheet (the Soul
 * Basin and the Eden Threshold are the others), recorded in
 * {@code docs/VANILLA_ASSET_MAP.md}. Dorian sells it; there is no recipe.
 */
public class BloodBowlObject extends GhostDecoObject {

    public BloodBowlObject() {
        super("spiritbasin", "spiritbasin", 32, new Color(120, 24, 32), null,
                "objects", "furniture");
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
        NightboundWorldData data = NightboundWorldData.get(level.getServer());
        if (data == null) {
            return;
        }
        int stock = data.stock(level, x, y);
        int carried = player.getInv().main.getAmount(level, player,
                ItemRegistry.getItem("bloodvial"), "bloodbowl");
        int room = NightboundWorldData.BOWL_CAPACITY - stock;
        if (carried > 0 && room > 0) {
            int put = Math.min(carried, room);
            player.getInv().main.removeItems(level, player, ItemRegistry.getItem("bloodvial"),
                    put, "bloodbowl");
            data.setStock(level, x, y, stock + put);
            stock += put;
        }
        TileText.at(client, x, y, new LocalMessage("misc", "bloodbowlstock",
                "count", String.valueOf(stock),
                "max", String.valueOf(NightboundWorldData.BOWL_CAPACITY)));
    }

    @Override
    public void onDestroyed(Level level, int layerID, int x, int y, Attacker attacker,
            ServerClient client, ArrayList<ItemPickupEntity> itemsDropped) {
        if (level.isServer()) {
            NightboundWorldData data = NightboundWorldData.get(level.getServer());
            int stock = data == null ? 0 : data.stock(level, x, y);
            if (stock > 0) {
                data.setStock(level, x, y, 0);
                level.entityManager.pickups.add(new ItemPickupEntity(level,
                        new InventoryItem("bloodvial", stock), x * 32 + 16, y * 32 + 16, 0.0F, 0.0F));
            }
        }
        super.onDestroyed(level, layerID, x, y, attacker, client, itemsDropped);
    }

    /**
     * Drinks one vial from the filled bowl nearest to (tileX, tileY) within
     * {@code reachTiles}, or returns false. Server side.
     */
    public static boolean drinkNear(Level level, int tileX, int tileY, int reachTiles) {
        if (level == null || level.isClient() || BloodBowlObject.id <= 0) {
            return false;
        }
        NightboundWorldData data = NightboundWorldData.get(level.getServer());
        if (data == null) {
            return false;
        }
        int[] best = null;
        long bestDist = Long.MAX_VALUE;
        for (int[] bowl : data.filledBowls(level)) {
            long dx = bowl[0] - tileX;
            long dy = bowl[1] - tileY;
            long d = dx * dx + dy * dy;
            if (d > (long) reachTiles * reachTiles || d >= bestDist) {
                continue;
            }
            // Stale record: the bowl is gone (another mod, a world edit).
            if (level.regionManager.isTileLoaded(bowl[0], bowl[1])
                    && level.getObjectID(bowl[0], bowl[1]) != BloodBowlObject.id) {
                data.setStock(level, bowl[0], bowl[1], 0);
                continue;
            }
            best = bowl;
            bestDist = d;
        }
        if (best == null) {
            return false;
        }
        data.setStock(level, best[0], best[1], data.stock(level, best[0], best[1]) - 1);
        return true;
    }

    /** The registered object ID, set at registration. */
    public static int id = -1;
}
