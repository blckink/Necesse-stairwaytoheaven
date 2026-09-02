package stairwaytoheaven.realms.ghost;

import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import stairwaytoheaven.items.ItemDescription;

/**
 * Spiritsteel Crown — the head of the Ghost Realm's reward set.
 *
 * <h2>Calibration</h2>
 * {@code docs/BALANCE.md} §7 puts Spiritsteel at <b>chest 34 / enchant 2400 /
 * EPIC</b>, one rung above the mod's own Stormsteel (which itself sits exactly
 * on vanilla's incursion floor of 29 / 1900 / EPIC). Within a set the same file
 * fixes the spread at <b>helm = chest - 3</b> and <b>greaves = chest - 10</b>,
 * a shape bracketed by vanilla's own: tungsten is 24/25/15 (-1 / -10) and
 * arcanic is 23/29/17 (-6 / -12). So this is <b>31 armour</b>.
 *
 * <p>It is a MELEE set, like Stormsteel, so the damage class is
 * {@code DamageTypeRegistry.MELEE} — the same choice
 * {@code NightsteelHelmetArmorItem} and {@code SpideriteHelmetArmorItem} make.
 *
 * <h2>Loot tables</h2>
 * Both loot-table arguments are {@code null} on purpose, for the reason
 * {@code StormsteelArmor} writes out at length: passing
 * {@code HeadArmorLootTable.headArmor} would drop tier-7 plate out of ordinary
 * surface chests and reverse the progression the whole climb is built on.
 * {@code ArmorItem.addToLootTable} null-checks each element, so a null is
 * wrapped and skipped rather than iterated.
 *
 * <h2>Borrowed art</h2>
 * The BODY sheet is a constructor argument
 * ({@code ArmorItem.loadArmorTexture} reads
 * {@code player/armor/<textureName>}), so this wears the game's own
 * {@code soulseedcrown} — a pale bone circlet that is already this realm's
 * palette. The inventory ICON is the game's {@code items/soulseedcrown} and is
 * loaded by the override below, because {@code Item.loadItemTextures} would
 * otherwise look for {@code items/spiritsteelhelmet.png}, which does not exist
 * and would draw the engine's ERR tile.
 */
public class SpiritsteelHelmet extends SetHelmetArmorItem {

    /** The set-bonus buff's registry key; resolved by name at construction. */
    public static final String SET_BONUS = "spiritsteelsetbonus";

    /** Vanilla armour sheet worn on the player's head. */
    public static final String ARMOR_TEXTURE = "soulseedcrown";
    /** Vanilla inventory icon. */
    public static final String ICON = "soulseedcrown";

    public SpiritsteelHelmet() {
        super(31, DamageTypeRegistry.MELEE, 2400,   // BALANCE §7: chest 34 - 3, enchant 2400
                (OneOfLootItems) null, (OneOfLootItems) null,
                Item.Rarity.EPIC,                    // the tier's own rarity
                ARMOR_TEXTURE,
                "spiritsteelchestplate", "spiritsteelboots",
                SET_BONUS);
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + ICON);
    }

    /**
     * The item's own description line, directly under its name.
     * {@code ArmorItem.getTooltips} is {@code final} (ArmorItem.java:212), so
     * this is the only way in, and it is where vanilla puts its own "Head slot"
     * line.
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
