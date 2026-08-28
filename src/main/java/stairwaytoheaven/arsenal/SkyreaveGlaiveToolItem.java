package stairwaytoheaven.arsenal;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.glaiveToolItem.GlaiveToolItem;
import necesse.inventory.lootTable.presets.GlaiveWeaponsLootTable;

/**
 * Skyreave — an Aetherium double-crescent on a cloudwood haft. The swung
 * melee weapon of the tier: {@link GlaiveToolItem} sweeps a full circle around
 * the wielder (its {@code getHitboxes} walks a {@code LineHitbox} around
 * {@code attackRange / 2}), so it trades the Tempest Edge's single-target
 * punch for crowd control against Skyreach packs.
 *
 * <p><b>Calibrated against {@code QuartzGlaiveToolItem}</b>
 * (49 dmg / 500 ms / range 140 / knockback 100, UNCOMMON) — the deep-cave
 * glaive of the same tier the mod sits in. Vanilla's next step up,
 * {@code CryoGlaiveToolItem}, is 60 dmg / 400 ms / range 160 and is boss loot.
 * Ours sits between them and stays below the Cryo Glaive on every axis, in the
 * same place the Tempest Edge sits relative to the tungsten sword.
 */
public class SkyreaveGlaiveToolItem extends GlaiveToolItem {

    public SkyreaveGlaiveToolItem() {
        super(1300, GlaiveWeaponsLootTable.glaiveWeapons);
        this.rarity = Item.Rarity.RARE;
        this.attackAnimTime.setBaseValue(460);
        this.attackDamage.setBaseValue(54.0F).setUpgradedValue(1.0F, 99.0F);
        this.attackRange.setBaseValue(150);
        this.knockback.setBaseValue(105);
        this.width = 20.0F;
        // The rotation pivot of player/weapons/skyreave.png, which is 96x96
        // with the grip at its exact centre. Vanilla pairs 108x92 with 50/50
        // (QuartzGlaive) and a smaller sheet with 40/40 (FrostGlaive) — the
        // offsets ARE the sprite's pivot, not a free-floating tuning number.
        this.attackXOffset = 48;
        this.attackYOffset = 48;
        this.canBeUsedForRaids = true;
    }

    /**
     * The item's flavour line. Overriding this is what actually PUTS a
     * description on the item: an [itemtooltip] locale key nothing calls is
     * dead text, which is why every weapon in this package names its own key.
     */
    @Override
    public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
                                                     GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "skyreavetip"));
        return tooltips;
    }
}
