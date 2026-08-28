package stairwaytoheaven.arsenal;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.summonToolItem.SummonToolItem;
import necesse.inventory.lootTable.presets.SummonWeaponsLootTable;

/**
 * Skywatch Whistle — blow it and a Watch Mote peels off the old frost
 * machinery and circles you.
 *
 * <p>{@code SummonToolItem(mobStringID, followPosition, summonSpaceTaken,
 * enchantCost, lootTableCategory)}: the first argument is the MobRegistry
 * stringID the focus spawns, so {@code "watchmote"} must be registered before
 * this item is ever used (both happen in {@code init()}, see
 * {@link SkyArsenal}). {@code summonType} is left at the default
 * {@code "summonedmob"} — a permanent follower that counts against the
 * player's summon slots, not the temporary kind
 * {@code FrostPiercerSummonToolItem} uses.
 *
 * <p><b>Calibrated against {@code CryoStaffSummonToolItem}</b>
 * (29 dmg, UNCOMMON, enchant cost 1450, FLYING_CIRCLE, one slot each) — the
 * deep-cave summon focus that calls vanilla's own player cryo flake. Ours is
 * two points above it and identical in every other respect.
 */
public class SkywatchWhistleSummonToolItem extends SummonToolItem {

    public SkywatchWhistleSummonToolItem() {
        super("watchmote", FollowPosition.FLYING_CIRCLE, 1.0F, 1450, SummonWeaponsLootTable.summonWeapons);
        this.rarity = Item.Rarity.RARE;
        this.attackDamage.setBaseValue(31.0F).setUpgradedValue(1.0F, 41.0F);
        this.canBeUsedForRaids = true;
    }

    /** See the note in {@link SkyreaveGlaiveToolItem}. Appended after
     * SummonToolItem's own slot/space lines, which the super call adds. */
    @Override
    public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
                                                     GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "skywatchwhistletip"));
        return tooltips;
    }
}
