package stairwaytoheaven.items;

import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.matItem.MatItem;

/**
 * A {@link MatItem} that says what it is.
 *
 * <p>A plain {@code MatItem} shows the player its name and, if some {@code Tech}
 * happens to consume it, one generic "crafting material" sentence. Aurora Petal
 * read exactly the same as Storm Shard read exactly the same as Skyweave. This
 * subclass appends the item's own description line from
 * {@code itemtooltip.<stringID>tip} — see {@link ItemDescription} for why the
 * missing-entry case is handled by comparing against the engine's debug
 * fallback rather than by asking whether the translation exists.
 *
 * <p>Everything else about {@code MatItem} is inherited untouched: the four
 * constructor shapes, {@code dropsAsMatDeathPenalty}, the {@code "materials"}
 * default category (and any {@code setItemCategory} override at the
 * registration site), the {@code "material"} keyword, global ingredients, stack
 * size and rarity. The broker value is a registration argument, not an item
 * field, so it is untouched by construction.
 *
 * <p>The three-argument {@code (stackSize, rarity, tooltipKey)} shape vanilla
 * uses for a hand-written blurb still works, and when that key happens to be
 * the item's own {@code <stringID>tip} the line is printed once, not twice:
 * {@code MatItem.getTooltips} has already added it.
 */
public class SkyMatItem extends MatItem {

    public SkyMatItem(int stackSize, String... globalIngredients) {
        super(stackSize, globalIngredients);
    }

    public SkyMatItem(int stackSize, Item.Rarity rarity, String... globalIngredients) {
        super(stackSize, rarity, globalIngredients);
    }

    // Fixed-arity, so it wins overload resolution over the varargs form above
    // for a single trailing String -- exactly as MatItem's own pair does.
    public SkyMatItem(int stackSize, Item.Rarity rarity, String tooltipKey) {
        super(stackSize, rarity, tooltipKey);
    }

    public SkyMatItem(int stackSize, Item.Rarity rarity, String tooltipKey, String... globalIngredients) {
        super(stackSize, rarity, tooltipKey, globalIngredients);
    }

    @Override
    public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
        String key = ItemDescription.key(this.getStringID());
        if (!key.equals(this.tooltipKey)) {
            String line = ItemDescription.byKey(key);
            if (line != null) {
                tooltips.add(line, this.tooltipMaxLength);
            }
        }
        return tooltips;
    }
}
