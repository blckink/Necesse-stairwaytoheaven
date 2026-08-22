package stairwaytoheaven.items;

import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.swordToolItem.SwordToolItem;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;

/**
 * Tempest Edge — Aetherium blade of the Skyreach. Tungsten-tier sidegrade:
 * slightly harder hitting and a touch faster than the tungsten sword
 * (65 dmg / 300 ms), paid for with sky materials.
 */
public class TempestEdgeSwordToolItem extends SwordToolItem {

    public TempestEdgeSwordToolItem() {
        super(1300, CloseRangeWeaponsLootTable.closeRangeWeapons);
        this.rarity = Item.Rarity.RARE;
        this.attackAnimTime.setBaseValue(290);
        this.attackDamage.setBaseValue(68.0F).setUpgradedValue(1.0F, 97.0F);
        this.attackRange.setBaseValue(80);
        this.knockback.setBaseValue(110);
        this.canBeUsedForRaids = true;
    }
}
