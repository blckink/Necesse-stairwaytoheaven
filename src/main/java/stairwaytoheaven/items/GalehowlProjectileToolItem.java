package stairwaytoheaven.items;

import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.BowProjectileToolItem;
import necesse.inventory.lootTable.presets.IncursionBowWeaponsLootTable;

/**
 * Galehowl — a windsilk-strung Aetherium bow.
 *
 * <h2>Calibration</h2>
 *
 * Anchored on {@code TheCrimsonSkyProjectileToolItem}, the damage bow of
 * vanilla's incursion tier. VERIFIED [jar], from its constructor: enchantCost
 * 1900, {@code Item.Rarity.EPIC}, attackAnimTime 500, attackDamage 90 base and
 * 110.83 at upgrade tier 1, velocity 350, attackRange 1600. The tier's only
 * other bow — {@code ArachnidWebBowToolItem}, the second and last item on
 * {@code IncursionBowWeaponsLootTable.incursionBowWeapons} — confirms the cost
 * and colour at 1900 / EPIC while trading its damage away (450 ms, 45 base and
 * 58.33 upgraded, velocity 300, attackRange 800) for its web.
 *
 * <p>Galehowl keeps its shape against that anchor: the same damage band, faster
 * to draw, and noticeably faster arrows. It used to be measured off
 * {@code TungstenBowProjectileToolItem} (60 base / 114.33 upgraded / 500 ms /
 * velocity 200 / enchantCost 1300 / UNCOMMON) and read 62 / 116 / 480 / 220 /
 * 1300 / RARE; that deep-cave anchor is gone with the rest of the mod's
 * tungsten-tier framing.
 *
 * <p>The loot table moves with the numbers. It was on
 * {@code BowWeaponsLootTable.bowWeapons}, the general pool that also holds the
 * wood bow, which at 93 damage would drop an endgame bow into a starting chest.
 * It now sits where its anchor sits, on
 * {@code IncursionBowWeaponsLootTable.incursionBowWeapons}.
 */
public class GalehowlProjectileToolItem extends BowProjectileToolItem {

    public GalehowlProjectileToolItem() {
        // thecrimsonsky: 1900, and the same incursion loot table.
        super(1900, IncursionBowWeaponsLootTable.incursionBowWeapons);
        this.rarity = Item.Rarity.EPIC;                // thecrimsonsky: EPIC
        this.attackAnimTime.setBaseValue(480);         // thecrimsonsky: 500 — ours faster to draw
        // thecrimsonsky: 90 base, 110.83 at upgrade tier 1. Ours is +3% on the base
        // (the nudge the old 62 held over tungstenbow's 60) and +5% at upgrade tier 1.
        this.attackDamage.setBaseValue(93.0F).setUpgradedValue(1.0F, 116.0F);
        // The standard bow reach, and not a tier number: tungstenbow, glacialbow,
        // bowofdualism and the incursion tier's own arachnidwebbow are all 800.
        this.attackRange.setBaseValue(800);
        this.velocity.setBaseValue(385);               // thecrimsonsky: 350 — ours +10%, as the old 220 was over tungstenbow's 200
        this.attackXOffset = 12;
        this.attackYOffset = 28;
        this.canBeUsedForRaids = true;
    }
}
