package stairwaytoheaven.realms.ghost;

import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import stairwaytoheaven.items.ItemDescription;

/**
 * Spiritsteel Greaves.
 *
 * <h2>Calibration</h2>
 * {@code docs/BALANCE.md} §7: within a set, <b>greaves = chest - 10</b>, so
 * 34 - 10 = <b>24 armour</b>, at the set's enchant cost of <b>2400</b> and
 * <b>EPIC</b>. The -10 spread is vanilla's own: tungsten is 25 chest / 15 boots
 * and {@code agedchampiongreaves} is 28 / 18.
 *
 * <h2>Borrowed art</h2>
 * Boot sheet {@code player/armor/soulseedboots} and icon
 * {@code items/soulseedboots}, both the game's own, both named rather than
 * derived from the string ID — see {@link SpiritsteelHelmet} for why the icon
 * needs an override at all.
 */
public class SpiritsteelBoots extends BootsArmorItem {

    /** Vanilla armour sheet worn on the player's feet. */
    public static final String ARMOR_TEXTURE = "soulseedboots";
    /** Vanilla inventory icon. */
    public static final String ICON = "soulseedboots";

    public SpiritsteelBoots() {
        super(24, 2400, Item.Rarity.EPIC,   // BALANCE §7: chest 34 - 10, enchant 2400
                ARMOR_TEXTURE, (OneOfLootItems) null);
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + ICON);
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
