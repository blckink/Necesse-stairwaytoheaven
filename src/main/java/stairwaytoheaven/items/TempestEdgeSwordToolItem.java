package stairwaytoheaven.items;

import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.swordToolItem.SwordToolItem;
import necesse.inventory.lootTable.presets.IncursionCloseRangeWeaponsLootTable;

/**
 * Tempest Edge — Aetherium blade of the Skyreach.
 *
 * <h2>Why the damage is where it is</h2>
 *
 * Necesse armour is a flat subtraction, not a percentage:
 * {@code DamageType.getDamageReduction(float armor, boolean isItemsVsItems)}
 * returns {@code armor * 0.5F} for player-vs-mob (DamageType.java:133-135,
 * VERIFIED [jar]). The Skyreach's enemies now floor at 40 armour, so every
 * swing loses a flat 20 before it lands, and the higher rungs take 27.5 and 35.
 * A blade calibrated on the craftable incursion sword's 105 would be handing a
 * third of each hit to armour, which is what makes a rebalanced enemy take a
 * hundred swings rather than ten.
 *
 * <h2>Calibration</h2>
 *
 * Vanilla weapon damage is not a constant — it is
 * {@code attackDamage.setBaseValue(B).setUpgradedValue(1.0F, U)} in the
 * constructor, B unupgraded and U fully upgraded. Measured across
 * {@code necesse/inventory/item/toolItem/swordToolItem/}, VERIFIED [jar]:
 *
 * <pre>
 *   SurvivorWhipToolItem       55 → 190.00   EPIC,   650, 800 ms (a slow whip)
 *   VoidClawSwordToolItem     160 → 186.67   UNIQUE, 2000, 120 ms
 *   AntiqueSwordSwordToolItem  92 → 116.67
 *   GemstoneLongswordToolItem  90 → 105.00   EPIC,   1900, 300 ms (incursion)
 *   AgedChampionSwordToolItem  60 →  81.67   (mid-tier, for scale)
 * </pre>
 *
 * So the top of the one-handed distribution is ~186–190, and the greatswords
 * run past it to {@code GlacialGreatswordToolItem} 180 → 221.67. Tempest Edge
 * is aimed at <b>156 → 182</b>: just under the ceiling that vanilla reserves
 * for its UNIQUE claw, and far above the 90 → 105 the tier's craftable sword
 * carries, because the mod's enemies are not the tier's enemies.
 *
 * <p>The ratio is not invented: 156 → 182 is exactly 7/6, which is the ratio
 * both {@code GemstoneLongswordToolItem} (90 → 105) and
 * {@code VoidClawSwordToolItem} (160 → 186.67) use.
 *
 * <p>Everything else stays the shape it always had — a hair faster than the
 * incursion sword, a blade's reach rather than a longsword's. It used to be
 * measured off {@code TungstenSwordToolItem} (65 → 93.33 / 300 ms /
 * enchantCost 1300 / UNCOMMON) and read 68 / 97 / 290 / 1300 / RARE.
 *
 * <p>The loot table moves with the numbers. It was on
 * {@code CloseRangeWeaponsLootTable.closeRangeWeapons}, the general pool that
 * also holds the wood sword, which at 156 damage would drop an endgame blade
 * into a starting chest. It now sits where the tier sits, on
 * {@code IncursionCloseRangeWeaponsLootTable.incursionCloseRangeWeapons}.
 */
public class TempestEdgeSwordToolItem extends SwordToolItem {

    public TempestEdgeSwordToolItem() {
        // gemstonelongsword: enchantCost 1900, and the same incursion loot table.
        super(1900, IncursionCloseRangeWeaponsLootTable.incursionCloseRangeWeapons);
        this.rarity = Item.Rarity.EPIC;                            // gemstonelongsword: EPIC
        this.attackAnimTime.setBaseValue(290);                     // gemstonelongsword: 300 — ours a touch faster
        // 156 → 182 is exactly 7/6, the ratio gemstonelongsword (90 → 105) and
        // voidclaw (160 → 186.67) both use; 182 sits just under voidclaw's 186.67,
        // the top of vanilla's one-handed sword distribution.
        this.attackDamage.setBaseValue(156.0F).setUpgradedValue(1.0F, 182.0F);
        // A blade, not a reach weapon: tungstensword's 80, not gemstonelongsword's 120.
        this.attackRange.setBaseValue(80);
        this.knockback.setBaseValue(110);                          // tungstensword: 100, gemstonelongsword: 75
        this.canBeUsedForRaids = true;
    }
}
