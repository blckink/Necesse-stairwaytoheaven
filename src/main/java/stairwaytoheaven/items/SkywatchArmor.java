package stairwaytoheaven.items;

import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.item.armorItem.HelmetArmorItem;

/**
 * The Warden's clothes as real armor items.
 *
 * This is the vanilla pattern for a human NPC with a distinctive silhouette:
 * the Elder is an ordinary human wearing ElderHatArmorItem, ElderShirtArmorItem
 * and ElderShoesArmorItem, which the settlement draws onto the standard body.
 * Because they are genuine armor pieces the player can also be given them, and
 * the Warden can wear anything else instead.
 *
 * Armor value is 0 across the set: these are cosmetics, not gear, and the
 * Warden is unkillable anyway. No loot-table category is passed — that would
 * add them to vanilla's shared cosmetic drop pool, where they do not belong.
 */
public final class SkywatchArmor {

    private SkywatchArmor() {
    }

    /**
     * The deep hood. Hair is hidden beneath it, and his beard draws OVER it —
     * that combination is what preserves the hooded-greybeard reading the
     * bespoke sprite used to carry.
     */
    public static class Hood extends HelmetArmorItem {
        public Hood() {
            super(0, null, 0, Item.Rarity.RARE, "skywatchhood", null);
            this.hairDrawOptions = ArmorItem.HairDrawMode.NO_HAIR;
            this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.UNDER_FACIAL_FEATURE;
        }
    }

    /** The long weathered mantle: a body sheet plus its own sleeves. */
    public static class Mantle extends ChestArmorItem {
        public Mantle() {
            super(0, 0, Item.Rarity.RARE, "wardenmantle", "wardenmantlearms", null);
        }
    }

    public static class Boots extends BootsArmorItem {
        public Boots() {
            super(0, 0, Item.Rarity.RARE, "wardenboots", null);
        }
    }
}
