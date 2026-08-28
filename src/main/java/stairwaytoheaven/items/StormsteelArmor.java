package stairwaytoheaven.items;

import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;

/**
 * The Stormsteel plate: the Skyreach's own armour set.
 *
 * <h2>Calibration</h2>
 *
 * Every number here was read off vanilla's <b>Tungsten set</b>, which is the
 * tier the Skyreach sits at (the Stairway costs 8 tungsten bars at a Tungsten
 * Workstation, so the sky opens beside the deep caves and not before):
 *
 * <pre>
 *   TungstenHelmetArmorItem     24 armor, MELEE, enchantCost 1300, UNCOMMON
 *   TungstenChestplateArmorItem 25 armor,        enchantCost 1300, UNCOMMON
 *   TungstenBootsArmorItem      15 armor,        enchantCost 1300, UNCOMMON
 * </pre>
 *
 * Stormsteel is one step above that and deliberately still under the next
 * vanilla tier, <b>Glacial</b> (helmet 24 / chest 24 / boots 16, enchantCost
 * 1450), so a player who found the sky is not skipping the deep-cave metals:
 * <b>25 / 26 / 16</b> at tungsten's own 1300 enchant cost and tungsten's own
 * {@code Item.Rarity.UNCOMMON} — vanilla keeps armour UNCOMMON all the way up
 * to Ancient Fossil, so a "rarer" colour here would be a lie about the tier.
 *
 * <p>Broker values follow the same rule: tungsten's 110 / 160 / 80, nudged to
 * 130 / 190 / 95.
 *
 * <h2>Set bonus</h2>
 *
 * A {@code SimpleSetBonusBuff} registered as {@code stormsteelsetbonus} (see
 * {@code SkyItems}). Calibrated against {@code GlacialHelmetBonusBuff}, the
 * neighbouring tier's set, which grants +20 max resilience and +20% resilience
 * gain: ours takes less of the resilience (+15 flat) and spends the rest on the
 * one thing sky plate should be, which is light (+5% movement speed).
 *
 * <h2>Loot tables</h2>
 *
 * Both loot-table arguments are {@code null} on purpose. Passing
 * {@code HeadArmorLootTable.headArmor} / {@code ArmorSetsLootTable.armorSets}
 * would drop Skyreach plate out of ordinary surface chests, which reverses the
 * progression the whole dimension is built on. {@code ArmorItem.addToLootTable}
 * and {@code SetHelmetArmorItem.addToArmorSetLootTable} both null-check each
 * element, and the constructors pass a single value rather than an array, so a
 * null argument is wrapped and skipped rather than iterated.
 */
public final class StormsteelArmor {

    /** The set-bonus buff's registry key; {@link Helmet} resolves it by name. */
    public static final String SET_BONUS = "stormsteelsetbonus";

    private StormsteelArmor() {
    }

    /** Storm-visored great helm. Vanilla anchor: {@code tungstenhelmet} (24). */
    public static class Helmet extends SetHelmetArmorItem {
        public Helmet() {
            super(25, DamageTypeRegistry.MELEE, 1300,
                    (OneOfLootItems) null, (OneOfLootItems) null,
                    Item.Rarity.UNCOMMON,
                    "stormsteelhelmet", "stormsteelchestplate", "stormsteelboots",
                    SET_BONUS);
        }

        /**
         * The item's own description line, directly under its name and above
         * the slot line.
         *
         * <p>{@code ArmorItem.getTooltips} is {@code final} (ArmorItem.java:212),
         * so this is the only way in — and it is where vanilla puts its own
         * "Head slot" / "Chest slot" lines. Each piece calls
         * {@link ItemDescription} itself rather than through a shared helper,
         * so {@code tools/locale_audit.py} can attribute the call to the class
         * that actually prints it and demand the locale line for exactly the
         * items that show one.
         */
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

    /** Plate cuirass. Vanilla anchor: {@code tungstenchestplate} (25). */
    public static class Chestplate extends ChestArmorItem {
        public Chestplate() {
            super(26, 1300, Item.Rarity.UNCOMMON,
                    "stormsteelchest", "stormsteelarms", (OneOfLootItems) null);
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

    /** Greaves. Vanilla anchor: {@code tungstenboots} (15). */
    public static class Boots extends BootsArmorItem {
        public Boots() {
            super(16, 1300, Item.Rarity.UNCOMMON, "stormsteelboots", (OneOfLootItems) null);
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
}
