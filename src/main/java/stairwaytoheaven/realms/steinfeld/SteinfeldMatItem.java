package stairwaytoheaven.realms.steinfeld;

import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import stairwaytoheaven.items.SkyMatItem;

/**
 * A material of Steinfeld, drawn with a vanilla icon that already exists.
 *
 * <p>The mechanism, and why the icon is a constructor argument rather than
 * the registered ID, is identical to {@code GhostMatItem} and
 * {@code CrookedMatItem} — {@code Item.loadItemTextures} is
 * {@code items/<stringID>.png} by default, this pass ships without a single
 * new PNG, and overriding it is the seam vanilla itself uses. See
 * {@code GhostMatItem}'s own doc for the full reasoning; it is not repeated
 * here because there is nothing Steinfeld-specific about it.
 *
 * <p>Which vanilla file stands in for which Steinfeld material is listed in
 * {@code docs/realms/steinfeld.md}, and {@code tools/locale_audit.py} checks
 * each one against the vanilla sprite dump via
 * {@code ITEM_CLASS_VANILLA_ICON["SteinfeldMatItem"]} rather than skipping it.
 *
 * <p>Extends {@link SkyMatItem} so every Steinfeld material carries the mod's
 * own description line ({@code itemtooltip.<id>tip}), enforced by the same
 * audit in both locales.
 */
public class SteinfeldMatItem extends SkyMatItem {

    private final String iconName;

    /**
     * @param iconName  file under {@code items/} to draw, WITHOUT the extension
     * @param stackSize how many fit in a slot
     * @param rarity    inventory rarity colour
     */
    public SteinfeldMatItem(String iconName, int stackSize, Item.Rarity rarity) {
        super(stackSize, rarity);
        this.iconName = iconName;
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + this.iconName);
    }
}
