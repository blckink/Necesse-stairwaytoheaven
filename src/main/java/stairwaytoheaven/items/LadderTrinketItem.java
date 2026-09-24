package stairwaytoheaven.items;

import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;

/**
 * An accessory the Spire Village's quest ladder pays — one per step, found
 * nowhere else ({@code docs/design/chapter-03-spire-village.md} §5).
 *
 * <p>{@link SkyTrinketItem} with a borrowed icon: nobody has drawn these yet,
 * so each names a vanilla item icon as constructor argument 0, the pattern
 * {@link SkyRewardItem} and {@code tools/locale_audit.py}'s
 * {@code ITEM_CLASS_VANILLA_ICON} already know. Dropping
 * {@code items/<own id>.png} into the resources and deleting the override is
 * the whole swap; every row is in {@code docs/VANILLA_ASSET_MAP.md} §1.8.
 * {@code TrinketItem} never reads the texture back, so it may finalize.
 */
public class LadderTrinketItem extends SkyTrinketItem {

    private final String iconName;

    /**
     * @param iconName vanilla file under {@code items/}, without extension
     */
    public LadderTrinketItem(String iconName, Item.Rarity rarity, String buffStringID, int enchantCost) {
        super(rarity, buffStringID, enchantCost);
        this.iconName = iconName;
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + this.iconName);
    }
}
