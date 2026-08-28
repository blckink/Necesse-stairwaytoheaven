package stairwaytoheaven.arsenal;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.BoomerangToolItem;
import necesse.inventory.lootTable.presets.ThrowWeaponsLootTable;

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
 * set of three rather than one at a time.
 *
 * <p><b>Calibrated against {@code TungstenBoomerangToolItem}</b>
 * (60 dmg / 300 ms anim / 400 ms cooldown / range 600 / velocity 180 /
 * knockback 100 / stack 4, UNCOMMON). Ours throws a slightly heavier disc
 * slightly further, and only three at a time instead of four.
 */
public class StormdiscToolItem extends BoomerangToolItem {

    public StormdiscToolItem() {
        super(1300, ThrowWeaponsLootTable.throwWeapons, "stormdisc");
        this.rarity = Item.Rarity.RARE;
        this.attackAnimTime.setBaseValue(300);
        this.attackCooldownTime.setBaseValue(400);
        this.attackDamage.setBaseValue(64.0F).setUpgradedValue(1.0F, 96.0F);
        this.attackRange.setBaseValue(640);
        this.velocity.setBaseValue(190);
        this.knockback.setBaseValue(105);
        this.resilienceGain.setBaseValue(0.75F);
        this.stackSize = 3;
        this.itemAttackerProjectileCanHitWidth = 18.0F;
        this.canBeUsedForRaids = true;
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
