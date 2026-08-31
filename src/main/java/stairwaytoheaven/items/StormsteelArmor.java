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
 * The Skyreach is endgame content, so every number here is read off vanilla's
 * <b>incursion tier</b> — the band the mod now sits in. VERIFIED [jar], from
 * the constructors of the incursion sets:
 *
 * <pre>
 *   NightsteelHelmetArmorItem     28 armor, MELEE, enchantCost 1900, EPIC
 *   SpideriteHelmetArmorItem      28 armor, MELEE, enchantCost 1900, EPIC
 *   ArcanicHelmetArmorItem        23 armor, null,  enchantCost 1900, EPIC
 *   ArcanicChestplateArmorItem    29 armor,        enchantCost 1900, EPIC
 *   NightsteelChestplateArmorItem 29 armor,        enchantCost 1900, EPIC
 *   ArcanicBootsArmorItem         17 armor,        enchantCost 1900, EPIC
 *   NightsteelBootsArmorItem      17 armor,        enchantCost 1900, EPIC
 * </pre>
 *
 * Stormsteel is a MELEE set ({@link Helmet} passes
 * {@code DamageTypeRegistry.MELEE}), so <b>Nightsteel</b> — 28 / 29 / 17, the
 * melee set of the same tier — is what it is measured against. Ours is
 * <b>26 / 29 / 19</b>: the same 74 points of armour Nightsteel carries, spread
 * a little away from the head and into the legs, because Stormsteel's set bonus
 * spends itself on movement rather than on a helmet ability. Chest sits exactly
 * on 29, which arcanic, nightsteel, spiderite, dawn, dusk and ravenlords all
 * share. (Battlechef is on the same incursion loot tables at 23 / 2000, but it
 * is a utility set and not a floor anything should be measured against.)
 *
 * <p>{@code enchantCost} is the tier's own <b>1900</b> — those same six sets
 * all charge it — and the rarity is <b>{@code Item.Rarity.EPIC}</b>, which is
 * what every one of them carries. Under the previous calibration this set was
 * tungsten-tier (25 / 26 / 16, enchantCost 1300, UNCOMMON, measured off
 * {@code TungstenChestplateArmorItem} 25 / 1300 / UNCOMMON); that anchor is
 * gone with the rest of the mod's deep-cave framing.
 *
 * <h2>Set bonus</h2>
 *
 * A {@code SimpleSetBonusBuff} registered as {@code stormsteelsetbonus} (see
 * {@code SkyItems}), calibrated there against {@code ArcanicHelmetSetBonusBuff}
 * and {@code NightSteelHelmetSetBonusBuff}.
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

    /**
     * Storm-visored great helm. Vanilla anchor: {@code nightsteelhelmet}, the
     * melee helmet of the incursion tier — 28 armor, MELEE, enchantCost 1900,
     * EPIC. Ours takes 26 of that 28, giving the two points back to the greaves.
     */
    public static class Helmet extends SetHelmetArmorItem {
        public Helmet() {
            super(26, DamageTypeRegistry.MELEE, 1900,   // nightsteelhelmet: 28, MELEE, 1900
                    (OneOfLootItems) null, (OneOfLootItems) null,
                    Item.Rarity.EPIC,                   // nightsteelhelmet: EPIC
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

    /**
     * Plate cuirass. Vanilla anchor: {@code arcanicchestplate} — 29 armor,
     * enchantCost 1900, EPIC — which is also exactly what
     * {@code nightsteelchestplate}, {@code dawnchestplate},
     * {@code duskchestplate}, {@code spideritechestplate} and
     * {@code ravenlordschestplate} carry. Ours sits on that 29.
     */
    public static class Chestplate extends ChestArmorItem {
        public Chestplate() {
            super(29, 1900, Item.Rarity.EPIC,           // arcanicchestplate: 29, 1900, EPIC
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

    /**
     * Greaves. Vanilla anchor: {@code nightsteelboots} — 17 armor,
     * enchantCost 1900, EPIC, and {@code arcanicboots}, {@code dawnboots},
     * {@code duskboots}, {@code spideritegreaves} and {@code ravenlordsboots}
     * are all 17 too. Ours is 19: it carries the two points the helm gave up,
     * which puts it at chest−10 — the spread {@code tungstenboots} (25 chest /
     * 15 boots) and {@code agedchampiongreaves} (28 / 18) both use.
     */
    public static class Boots extends BootsArmorItem {
        public Boots() {
            super(19, 1900, Item.Rarity.EPIC,           // nightsteelboots: 17, 1900, EPIC
                    "stormsteelboots", (OneOfLootItems) null);
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
