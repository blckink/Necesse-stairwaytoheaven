package stairwaytoheaven.items;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;

/**
 * A drink Halda brews for the quest ladder: vanilla's
 * {@link FoodConsumableItem}, exactly as {@link WardensRoundItem} is, with a
 * borrowed icon (constructor argument 0, known to
 * {@code tools/locale_audit.py}'s {@code ITEM_CLASS_VANILLA_ICON}).
 *
 * <p>The texture is loaded with {@code forceNotFinalize}: the food base class
 * reads it back pixel by pixel for its particles, the reason
 * {@link WardensRoundItem} gives for the same flag.
 */
public class LadderFoodItem extends FoodConsumableItem {

    private final String iconName;

    public LadderFoodItem(String iconName, int stackSize, Item.Rarity rarity,
                          necesse.level.maps.levelData.settlementData.settler.FoodQuality quality,
                          int nutrition, int durationSeconds, ModifierValue<?>... modifiers) {
        super(stackSize, rarity, quality, nutrition, durationSeconds, modifiers);
        this.iconName = iconName;
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + this.iconName, true);
    }

    @Override
    public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective,
            GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
        String line = ItemDescription.of(this.getStringID());
        if (line != null) {
            tooltips.add(line);
        }
        return tooltips;
    }
}
