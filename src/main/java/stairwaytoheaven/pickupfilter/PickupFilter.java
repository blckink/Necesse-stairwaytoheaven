package stairwaytoheaven.pickupfilter;

import java.io.File;
import java.util.HashMap;

import necesse.engine.GlobalData;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.inventory.InventoryItem;
import necesse.inventory.itemFilter.ItemCategoriesFilter;

/**
 * Which items a player does not want to auto-collect.
 *
 * <p>The filter is a per-player client setting: it lives in the game's cfg
 * folder, so it follows the player across worlds, and is pushed to the server
 * whenever it changes or the inventory form is built (which happens on every
 * join). The server only keeps the latest copy per authentication in memory;
 * a client that never sent one collects everything, exactly like vanilla.
 *
 * <p>The model is vanilla's own storage filter ({@link ItemCategoriesFilter}),
 * so categories and single items toggle the same way a chest's settings do.
 */
public final class PickupFilter {

    private static final String FILE_NAME = "stairwaytoheaven_pickupfilter.cfg";

    /** The local player's filter; everything allowed until loaded. */
    private static ItemCategoriesFilter clientFilter;

    /** Latest filter per player authentication, server side. */
    private static final HashMap<Long, ItemCategoriesFilter> SERVER_FILTERS = new HashMap<>();

    private PickupFilter() {
    }

    public static synchronized ItemCategoriesFilter client() {
        if (clientFilter == null) {
            clientFilter = new ItemCategoriesFilter(true);
            load(clientFilter);
        }
        return clientFilter;
    }

    private static File file() {
        return new File(GlobalData.cfgPath() + FILE_NAME);
    }

    private static void load(ItemCategoriesFilter filter) {
        File f = file();
        if (!f.exists()) {
            return;
        }
        try {
            filter.applyLoadData(new LoadData(f));
        } catch (Exception e) {
            System.err.println("[stairwaytoheaven] pickup filter unreadable, using defaults: " + e);
        }
    }

    public static void saveClient() {
        SaveData save = new SaveData("PICKUPFILTER");
        client().addSaveData(save);
        save.saveScript(file());
    }

    /** Push the local filter to whichever server this client plays on. */
    public static void sendToServer(Client client) {
        if (client == null || client.network == null) {
            return;
        }
        client.network.sendPacket(new PacketPickupFilter(client()));
    }

    static synchronized void setServerFilter(long auth, ItemCategoriesFilter filter) {
        SERVER_FILTERS.put(auth, filter);
    }

    public static synchronized boolean allows(ServerClient client, InventoryItem item) {
        if (client == null || item == null || item.item == null) {
            return true;
        }
        ItemCategoriesFilter filter = SERVER_FILTERS.get(client.authentication);
        return filter == null || filter.isItemAllowed(item.item);
    }
}
