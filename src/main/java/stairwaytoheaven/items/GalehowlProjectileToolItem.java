package stairwaytoheaven.items;

import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.BowProjectileToolItem;
import necesse.inventory.lootTable.presets.BowWeaponsLootTable;

/**
 * Galehowl — a windsilk-strung Aetherium bow. Tungsten-tier sidegrade: same
 * damage band as the tungsten bow (60 dmg / 500 ms / velocity 200) but faster
 * to draw and with noticeably faster arrows.
 */
public class GalehowlProjectileToolItem extends BowProjectileToolItem {

    public GalehowlProjectileToolItem() {
        super(1300, BowWeaponsLootTable.bowWeapons);
        this.rarity = Item.Rarity.RARE;
        this.attackAnimTime.setBaseValue(480);
        this.attackDamage.setBaseValue(62.0F).setUpgradedValue(1.0F, 116.0F);
        this.attackRange.setBaseValue(800);
        this.velocity.setBaseValue(220);
        this.attackXOffset = 12;
        this.attackYOffset = 28;
        this.canBeUsedForRaids = true;
    }
}
