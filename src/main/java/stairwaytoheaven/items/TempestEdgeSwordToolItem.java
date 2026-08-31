package stairwaytoheaven.items;

import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.swordToolItem.SwordToolItem;
import necesse.inventory.lootTable.presets.IncursionCloseRangeWeaponsLootTable;

/**
 * Tempest Edge — Aetherium blade of the Skyreach.
 *
 * <h2>Calibration</h2>
 *
 * Anchored on {@code GemstoneLongswordToolItem}, the plain sword of vanilla's
 * incursion tier. VERIFIED [jar], from its constructor: enchantCost 1900,
 * {@code Item.Rarity.EPIC}, attackAnimTime 300, attackDamage 90 base and
 * 105.0 at upgrade tier 1, attackRange 120, knockback 75. The tier's other two
 * melee weapons agree on the band — {@code BloodClawToolItem} 1900 / EPIC and
 * {@code PerfectStormSwordToolItem} 2000 / EPIC (75 base, 87.5 upgraded).
 *
 * <p>Tempest Edge keeps the shape it has always had against its anchor: a hair
 * harder hitting and a touch faster, paid for with sky materials. It used to be
 * measured off {@code TungstenSwordToolItem} (65 base / 93.33 upgraded / 300 ms
 * / enchantCost 1300 / UNCOMMON) and read 68 / 97 / 290 / 1300 / RARE; that
 * deep-cave anchor is gone with the rest of the mod's tungsten-tier framing.
 *
 * <p>The loot table moves with the numbers. It was on
 * {@code CloseRangeWeaponsLootTable.closeRangeWeapons}, the general pool that
 * also holds the wood sword, which at 94 damage would drop an endgame blade
 * into a starting chest. It now sits where its anchor sits, on
 * {@code IncursionCloseRangeWeaponsLootTable.incursionCloseRangeWeapons}.
 */
public class TempestEdgeSwordToolItem extends SwordToolItem {

    public TempestEdgeSwordToolItem() {
        // gemstonelongsword: 1900, and the same incursion loot table.
        super(1900, IncursionCloseRangeWeaponsLootTable.incursionCloseRangeWeapons);
        this.rarity = Item.Rarity.EPIC;                            // gemstonelongsword: EPIC
        this.attackAnimTime.setBaseValue(290);                     // gemstonelongsword: 300 — ours a touch faster
        // gemstonelongsword: 90 base, 105.0 at upgrade tier 1. Ours is +4% at both ends,
        // the same nudge the old 68 held over tungstensword's 65.
        this.attackDamage.setBaseValue(94.0F).setUpgradedValue(1.0F, 110.0F);
        // A blade, not a reach weapon: tungstensword's 80, not gemstonelongsword's 120.
        this.attackRange.setBaseValue(80);
        this.knockback.setBaseValue(110);                          // tungstensword: 100, gemstonelongsword: 75
        this.canBeUsedForRaids = true;
    }
}
