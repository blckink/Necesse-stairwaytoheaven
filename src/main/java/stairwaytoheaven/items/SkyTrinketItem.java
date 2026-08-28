package stairwaytoheaven.items;

import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.trinketItem.SimpleTrinketItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;

/**
 * An accessory of the Skyreach: vanilla's {@code SimpleTrinketItem} plus the
 * mod's description line.
 *
 * <p>Accessories in Necesse are {@code TrinketItem}s — the class behind every
 * vanilla charm, boot, cloak, shield and pendant, filed under
 * {@code equipment/trinkets} by the base constructor. {@code SimpleTrinketItem}
 * is the concrete one that just names a registered {@code TrinketBuff}, and
 * {@code SimpleTrinketBuff} with no tooltip key makes the ENGINE print the
 * modifier numbers, so a stat here can never drift out of sync with a
 * hand-written locale line.
 *
 * <p>{@code TrinketItem.getTooltips} is {@code final} (TrinketItem.java:113),
 * so the description goes in through {@code getPreEnchantmentTooltips}, which
 * is where vanilla puts its own "Trinket slot" line and the buff's stats.
 *
 * <p>The loot-table argument is {@code null} deliberately, for the same reason
 * as {@link StormsteelArmor}: joining {@code TrinketsLootTable.trinkets} would
 * scatter sky accessories through ordinary surface chests. The base
 * constructor passes the single value to a varargs method, which wraps it, and
 * every element is null-checked.
 */
public class SkyTrinketItem extends SimpleTrinketItem {

    public SkyTrinketItem(Item.Rarity rarity, String buffStringID, int enchantCost) {
        super(rarity, buffStringID, enchantCost, (OneOfLootItems) null);
    }

    @Override
    public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
                                                      GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
        String line = ItemDescription.of(this.getStringID());
        if (line != null) {
            tooltips.addFirst(line);
        }
        return tooltips;
    }
}
