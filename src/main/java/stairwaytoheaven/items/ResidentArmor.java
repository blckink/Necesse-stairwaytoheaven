package stairwaytoheaven.items;

import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.HelmetArmorItem;

/**
 * One piece of headwear each for the three Skyreach residents, so that the mod
 * has art of its own on the people it added.
 *
 * <h2>Why headwear and not a mob sheet</h2>
 * {@code docs/IMPLEMENTATION_RULES.md} §3 is explicit: a human settler stays on
 * the native human renderer, and a bespoke full-body sprite is the wrong answer
 * even when it would be easier to draw. Vanilla's own way of giving a settler a
 * silhouette is real clothing ITEMS on a plain body — the Elder is a human in
 * {@code elderhat} — and {@link SkywatchArmor} already follows it for the
 * Warden. These three follow it for Magpie, Halda and Ossian Vane, who until
 * 8n wore {@code trapperhat}, {@code battlechefhat} and {@code runichat}: real
 * clothes, but every pixel of them vanilla's.
 *
 * <h2>What they are worth</h2>
 * Armor value 0, like the Warden's set: these are identity, not gear. No
 * loot-table category is passed, so they stay out of vanilla's shared cosmetic
 * drop pool. Each resident sells their own spare, which is the player's way to
 * get one — see each mob's shop.
 *
 * <p>The sheets under {@code resources/player/armor/} and the three item icons
 * are written by {@code tools/asset_generator/gen_residents.py}. Unlike the
 * Warden's set there is no {@code armorSheetsExist()} switch here, because the
 * sheets landed in the same commit as the registration.
 */
public final class ResidentArmor {

    private ResidentArmor() {
    }

    /**
     * Magpie's courier cap. Hair is left on the engine's default for a helmet
     * — {@code ArmorItem.HairDrawMode} offers no {@code HAIR} constant to ask
     * for explicitly, and the default is what every vanilla hat already uses.
     */
    public static class MagpieCap extends HelmetArmorItem {
        public MagpieCap() {
            super(0, null, 0, Item.Rarity.UNCOMMON, "magpiecap", null);
        }
    }

    /** Halda's cellar kerchief, knotted at the side and stained by the vats. */
    public static class HaldaKerchief extends HelmetArmorItem {
        public HaldaKerchief() {
            super(0, null, 0, Item.Rarity.UNCOMMON, "haldakerchief", null);
        }
    }

    /**
     * Ossian Vane's working cowl, with the loupe he never takes off. Hair is
     * covered — the one place a HairDrawMode is stated, and the same constant
     * {@link SkywatchArmor.Hood} uses.
     */
    public static class VaneCowl extends HelmetArmorItem {
        public VaneCowl() {
            super(0, null, 0, Item.Rarity.UNCOMMON, "vanecowl", null);
            this.hairDrawOptions = ArmorItem.HairDrawMode.NO_HAIR;
        }
    }
}
