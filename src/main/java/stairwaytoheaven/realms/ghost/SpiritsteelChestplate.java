package stairwaytoheaven.realms.ghost;

import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import stairwaytoheaven.items.ItemDescription;

/**
 * Spiritsteel Plate — the anchor of the Ghost Realm's reward set, and the piece
 * every other number in it is measured from.
 *
 * <h2>Calibration</h2>
 * {@code docs/BALANCE.md} §7: <b>34 armour / enchant 2400 / EPIC</b>. That is
 * one rung above the mod's Stormsteel (29 / 1900 / EPIC), which sits exactly on
 * vanilla's incursion floor — {@code ArcanicChestplateArmorItem} and the five
 * other incursion chests all carry 29 at enchant cost 1900 (VERIFIED [jar]). So
 * the Aftergarden's plate is the first armour in the game, vanilla included,
 * that is better than the incursion set, which is what a realm five tiers up
 * the mod's own ladder owes the player.
 *
 * <h2>Borrowed art</h2>
 * The body and arms sheets are constructor arguments
 * ({@code ChestArmorItem.loadArmorTexture} reads
 * {@code player/armor/<bodyTextureName>} and
 * {@code player/armor/<armsTextureName>_left} / {@code _right}), so this wears
 * the game's own soulseed plate. The inventory icon is the game's
 * {@code items/soulseedchestplate}, loaded by the override below.
 */
public class SpiritsteelChestplate extends ChestArmorItem {

    /** Vanilla armour sheets worn on the player's body and arms. */
    public static final String BODY_TEXTURE = "soulseedchest";
    public static final String ARMS_TEXTURE = "soulseedarms";
    /** Vanilla inventory icon. */
    public static final String ICON = "soulseedchestplate";

    public SpiritsteelChestplate() {
        super(34, 2400, Item.Rarity.EPIC,   // BALANCE §7: Spiritsteel chest 34 / enchant 2400
                BODY_TEXTURE, ARMS_TEXTURE, (OneOfLootItems) null);
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
