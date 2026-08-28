package stairwaytoheaven.arsenal;

import java.awt.Color;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.GreatbowProjectileToolItem;
import necesse.inventory.lootTable.presets.GreatbowWeaponsLootTable;

/**
 * Thunderhead — a seraphwood greatbow strung with windsilk.
 *
 * <p>A greatbow is not a bow with bigger numbers: {@code GreatbowProjectileToolItem}
 * installs a {@code GreatbowAttackHandler} and scales velocity, range, damage,
 * knockback and resilience by the charge percentage, so an uncharged shot lands
 * at 5-40% of these values. That is why the base damage looks enormous next to
 * the Galehowl's 62 — it is the fully-drawn number.
 *
 * <p>Like every bow in the game this class holds no arrow: at attack time
 * {@code BowProjectileToolItem.getArrowItem} asks the wielder for its equipped
 * {@code ArrowItem} and the ARROW builds the projectile
 * (verified in {@code BowProjectileToolItem} / {@code ArrowItem.getProjectile}).
 *
 * <p><b>Calibrated against {@code TungstenGreatbowProjectileToolItem}</b>
 * (120 dmg / 600 ms / range 1200 / velocity 400, UNCOMMON). Vanilla's next
 * greatbow, {@code MyceliumGreatbowProjectileToolItem}, is 160 / 1400 / 425 and
 * needs a deep-cave biome ore; ours stays between them.
 */
public class ThunderheadGreatbowToolItem extends GreatbowProjectileToolItem {

    public ThunderheadGreatbowToolItem() {
        super(1300, GreatbowWeaponsLootTable.greatbowWeapons);
        this.rarity = Item.Rarity.RARE;
        this.attackAnimTime.setBaseValue(620);
        this.attackDamage.setBaseValue(126.0F).setUpgradedValue(1.0F, 178.0F);
        this.attackRange.setBaseValue(1250);
        this.velocity.setBaseValue(415);
        // Pivot of player/weapons/thunderhead.png (24x64), the same sheet size
        // and offsets vanilla uses for its own greatbow attack sprite.
        this.attackXOffset = 10;
        this.attackYOffset = 36;
        this.particleColor = new Color(136, 216, 220);   // palette.AETHERIUM light
        this.canBeUsedForRaids = true;
    }

    /**
     * Added through the greatbow's own tooltip hook rather than
     * getPreEnchantmentTooltips, so our line lands next to vanilla's
     * "greatbowtip" charge explanation instead of above it.
     */
    @Override
    protected void addExtraBowTooltips(ListGameTooltips tooltips, InventoryItem item, PlayerMob perspective,
                                       GameBlackboard blackboard) {
        super.addExtraBowTooltips(tooltips, item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "thunderheadtip"));
    }
}
