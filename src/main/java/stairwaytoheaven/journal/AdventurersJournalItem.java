package stairwaytoheaven.journal;

import java.util.function.Supplier;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.Container;
import necesse.inventory.container.ContainerActionResult;
import necesse.inventory.container.slots.ContainerSlot;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;
import stairwaytoheaven.items.ItemDescription;

/**
 * The Adventurer's Journal ("Abenteurer-Tagebuch"): a book that opens the
 * journal window.
 *
 * <p>Modelled on vanilla's {@code RecipeBookItem} (right-click it in the
 * inventory, {@code getInventoryRightClickAction}) and
 * {@code CloudInventoryOpenItem} (use it from the hotbar, {@code onAttack}),
 * VERIFIED [jar] 1.3.2 decompiled. Both paths do the same thing on the server
 * only: build this player's book and send it. The client never decides
 * anything; it only shows what arrives.
 *
 * <p>The icon is BORROWED: vanilla's recipe book ({@code items/recipebook}),
 * listed in {@code docs/VANILLA_ASSET_MAP.md}. To give it its own art, drop
 * {@code src/main/resources/items/adventurersjournal.png} in and delete
 * {@link #loadItemTextures}.
 */
public class AdventurersJournalItem extends Item {

    /** The borrowed icon, under {@code items/}. */
    public static final String ICON = "recipebook";

    /** Distinct from the numbers vanilla's own two right-click books return. */
    private static final int OPENED = 0x5A4E0001;

    public AdventurersJournalItem() {
        super(1);
        this.rarity = Item.Rarity.UNCOMMON;
        this.worldDrawSize = 32;
        // Item's default is the bare "misc" root, which CategoryCensus counts
        // as unsorted; the journal is filed beside the Ghost Chalk and the
        // region keys, the mod's other story items.
        this.setItemCategory("misc", "questitems");
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + ICON);
    }

    @Override
    public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
        String line = ItemDescription.byKey(ItemDescription.key(this.getStringID()));
        if (line != null) {
            tooltips.add(line, 400);
        }
        tooltips.add(Localization.translate("itemtooltip", "rclickinvopentip"));
        return tooltips;
    }

    @Override
    public Supplier<ContainerActionResult> getInventoryRightClickAction(Container container, InventoryItem item,
            int slotIndex, ContainerSlot slot) {
        return () -> {
            if (container.getClient().isServer()) {
                ServerClient client = container.getClient().getServerClient();
                AdventurerJournal.sendTo(client.getServer(), client, false);
            }
            return new ContainerActionResult(OPENED);
        };
    }

    @Override
    public InventoryItem onAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
            InventoryItem item, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent) {
        if (level.isServer() && attackerMob.isPlayer) {
            ServerClient client = ((PlayerMob) attackerMob).getServerClient();
            if (client != null) {
                AdventurerJournal.sendTo(level.getServer(), client, false);
            }
        }
        return item;
    }
}
