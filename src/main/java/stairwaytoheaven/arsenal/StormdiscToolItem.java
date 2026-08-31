package stairwaytoheaven.arsenal;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.BoomerangToolItem;
import necesse.inventory.lootTable.presets.IncursionThrowWeaponsLootTable;

/**
 * Stormdisc — a thrown Aetherium ring with a cinderpearl burning in its hub.
 * The throwable of the tier.
 *
 * <p>{@code BoomerangToolItem(enchantCost, lootTableCategory, projectileID)}:
 * the third argument is a ProjectileRegistry stringID, looked up at attack time
 * through {@code ProjectileRegistry.getProjectile(...)}, so
 * {@code "stormdisc"} must be registered as a projectile as well as an item
 * (both happen in {@code init()}, see {@link SkyArsenal}).
 *
 * <p>{@code stackSize} is the number of discs that may be in the air at once
 * ({@code canAttack} compares {@code getBoomerangsUsage()} against
 * {@code min(amount, stackSize)}), and a stack must be FULL before the weapon
 * can be enchanted or upgraded — which is why the recipe hands over a whole
 * set at a time.
 *
 * <p><b>Calibrated against {@code NightRazorBoomerangToolItem}</b> — VERIFIED
 * [jar], the sole member of {@code IncursionThrowWeaponsLootTable} and so the
 * whole of vanilla's incursion-tier boomerang class: enchant cost 1900, 300 ms
 * throw, {@code attackDamage} 70.0 rising to 81.66669 at forge tier 1, range
 * 600, velocity 220, stack 4, knockback 50, resilience 0.5, hit width 18.0.
 * Shape, speed, cost and rarity are taken from it directly.
 *
 * <p>The 400 ms {@code attackCooldownTime} the deep-cave calibration carried is
 * gone: NightRazor sets none, and {@code Item.cooldown} defaults to -1 so
 * {@code attackCooldownTime} stays empty and reads 0.
 *
 * <p><b>Damage is set above it, deliberately.</b> Necesse subtracts armour flat
 * — {@code DamageType.getDamageReduction} is {@code armor * 0.5F} against a
 * player-owned attack, VERIFIED [jar] — so a Skyreach enemy at 40 armour takes
 * 20 off every disc and a Veil enemy at 70 armour takes 35, four times per
 * volley. The upgrade ratio is NightRazor's exactly
 * (81.66669 / 70.0 = 1.166667), applied to a base of 150.0, giving 175.00005
 * at forge tier 1 — the floor of the 175-200 band the top of vanilla's own
 * {@code attackDamage} distribution occupies. The floor rather than the middle
 * because four discs are in the air at once and each one carries the full
 * number.
 *
 * <p>Rarity is the tier's rather than NightRazor's own RARE:
 * {@code ArcanicChestplateArmorItem} is 29 armour / enchant 1900 / EPIC, and
 * EPIC is what the incursion weapon tables are mostly made of. VERIFIED [jar].
 */
public class StormdiscToolItem extends BoomerangToolItem {

    public StormdiscToolItem() {
        // Loot pool: the incursion throw table. See the note in
        // SkyreaveGlaiveToolItem — the loot table handed to ToolItem's
        // constructor is what decides where in the game this can be found.
        super(1900, IncursionThrowWeaponsLootTable.incursionThrowWeapons, "stormdisc"); // NightRazor enchant 1900
        this.rarity = Item.Rarity.EPIC;                      // incursion tier; ArcanicChestplate is EPIC
        this.attackAnimTime.setBaseValue(300);               // NightRazor 300 ms
        // 150.0 x NightRazor's own 81.66669/70.0 upgrade ratio; see the note above
        this.attackDamage.setBaseValue(150.0F).setUpgradedValue(1.0F, 175.00005F);
        this.attackRange.setBaseValue(600);                  // NightRazor 600
        this.velocity.setBaseValue(220);                     // NightRazor 220
        this.knockback.setBaseValue(50);                     // NightRazor 50
        this.resilienceGain.setBaseValue(0.5F);              // NightRazor 0.5
        this.stackSize = 4;                                  // NightRazor 4 in the air at once
        this.itemAttackerProjectileCanHitWidth = 18.0F;      // NightRazor 18.0
        // Raid loadouts — see the note in SkyreaveGlaiveToolItem.
        this.canBeUsedForRaids = true;
        this.useForRaidsOnlyIfObtained = true;               // NightRazor true
        this.raidTicketsModifier = 0.5F;                     // NightRazor 0.5F
    }

    /** See the note in {@link SkyreaveGlaiveToolItem}. Appended after
     * BoomerangToolItem's own "throws N at once" line. */
    @Override
    public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
                                                     GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "stormdisctip"));
        return tooltips;
    }
}
