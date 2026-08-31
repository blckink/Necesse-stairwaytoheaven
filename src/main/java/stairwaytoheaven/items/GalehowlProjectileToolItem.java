package stairwaytoheaven.items;

import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.BowProjectileToolItem;
import necesse.inventory.lootTable.presets.IncursionBowWeaponsLootTable;

/**
 * Galehowl — a windsilk-strung Aetherium bow.
 *
 * <h2>Why the damage is where it is</h2>
 *
 * The same flat-subtraction armour that drives {@link TempestEdgeSwordToolItem}:
 * {@code DamageType.getDamageReduction(float armor, boolean isItemsVsItems)}
 * returns {@code armor * 0.5F} for player-vs-mob (DamageType.java:133-135,
 * VERIFIED [jar]), so the Skyreach's 40-armour floor eats a flat 20 off every
 * arrow and the higher rungs eat 27.5 and 35.
 *
 * <h2>Calibration</h2>
 *
 * Vanilla weapon damage is {@code attackDamage.setBaseValue(B)
 * .setUpgradedValue(1.0F, U)} in the constructor. Measured across
 * {@code necesse/inventory/item/toolItem/projectileToolItem/}, VERIFIED [jar]:
 *
 * <pre>
 *   MyceliumGreatbowProjectileToolItem 160 → 210.00  (greatbow)
 *   GoldGreatbowProjectileToolItem      52 → 180.83  (greatbow)
 *   VoidGreatbowProjectileToolItem      65 → 175.00  (greatbow)
 *   WoodBowProjectileToolItem           12 → 151.67  (bow, a low-base outlier)
 *   TungstenBowProjectileToolItem       60 → 114.33  (bow)
 *   AntiqueBowProjectileToolItem        95 → 112.00  (bow)
 *   TheCrimsonSkyProjectileToolItem     90 → 110.83  EPIC, 1900, 500 ms (incursion bow)
 *   ArachnidWebBowToolItem              45 →  58.33  EPIC, 1900, 450 ms (incursion bow)
 * </pre>
 *
 * Galehowl is aimed at <b>145 → 178</b>. That is past every plain bow vanilla
 * ships and into the greatbows' band, which is deliberate for the same reason
 * as the blade: the enemies this bow is now shot at are not the incursion's.
 * It sits a little under Tempest Edge's 182 because
 * {@code ArrowItem.modDamage} (ArrowItem.java:49-51) <b>adds</b> the arrow's
 * own damage on top of the bow's — 5 for a stone arrow up to 17 for a
 * spiderite one — so the bow's number is not the whole shot.
 *
 * <p>The ratio follows the bow of the same class and tier:
 * {@code TheCrimsonSkyProjectileToolItem} runs 90 → 110.83, a ratio of 1.2315;
 * ours is 145 → 178, a ratio of 1.2276.
 *
 * <p>Everything else keeps the shape it always had against that anchor: faster
 * to draw (480 ms against 500) and noticeably faster arrows (velocity 385
 * against 350, the +10% the old 220 held over the tungsten bow's 200). It used
 * to be measured off {@code TungstenBowProjectileToolItem} (60 → 114.33 /
 * 500 ms / velocity 200 / enchantCost 1300 / UNCOMMON) and read
 * 62 / 116 / 480 / 220 / 1300 / RARE.
 *
 * <p>The loot table moves with the numbers. It was on
 * {@code BowWeaponsLootTable.bowWeapons}, the general pool that also holds the
 * wood bow, which at 145 damage would drop an endgame bow into a starting
 * chest. It now sits where the tier sits, on
 * {@code IncursionBowWeaponsLootTable.incursionBowWeapons}.
 */
public class GalehowlProjectileToolItem extends BowProjectileToolItem {

    public GalehowlProjectileToolItem() {
        // thecrimsonsky: enchantCost 1900, and the same incursion loot table.
        super(1900, IncursionBowWeaponsLootTable.incursionBowWeapons);
        this.rarity = Item.Rarity.EPIC;                // thecrimsonsky: EPIC
        this.attackAnimTime.setBaseValue(480);         // thecrimsonsky: 500 — ours faster to draw
        // 145 → 178 is a ratio of 1.2276 against thecrimsonsky's 90 → 110.83 (1.2315).
        // 178 lands between voidgreatbow's 175 and goldgreatbow's 180.83, and under
        // tempestedge's 182 because the arrow adds its own 5–17 on top of this number.
        this.attackDamage.setBaseValue(145.0F).setUpgradedValue(1.0F, 178.0F);
        // The standard bow reach, and not a tier number: tungstenbow, glacialbow,
        // bowofdualism and the incursion tier's own arachnidwebbow are all 800.
        this.attackRange.setBaseValue(800);
        this.velocity.setBaseValue(385);               // thecrimsonsky: 350 — ours +10%, as the old 220 was over tungstenbow's 200
        this.attackXOffset = 12;
        this.attackYOffset = 28;
        this.canBeUsedForRaids = true;
    }
}
