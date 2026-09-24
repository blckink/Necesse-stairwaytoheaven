package stairwaytoheaven.items;

import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.GreatswordToolItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;

/**
 * Die Erinnernde Klinge — the greatsword Caspern forges once his fire is
 * lit again, the reward of the ladder's {@code swh_ladder_memory}
 * ({@code docs/design/chapter-03-spire-village.md} §5).
 *
 * <p>The same class shape as {@code realms.ghost.SpiritsteelReaver}, the Ghost
 * Realm's other two-hander: vanilla's {@link GreatswordToolItem}, its charge
 * levels and its swing, with a borrowed icon and swing sprite
 * ({@code hexedbladegreatsword}, {@code docs/VANILLA_ASSET_MAP.md} §1.8).
 *
 * <p><b>Balance.</b> The Reaver is the Ghost key's weapon at 176 → 219 and
 * enchant 2 400 ({@code docs/BALANCE.md} §7). The Blade is the Ghost chapter's
 * LAST rung — two resident chains past the key — so it sits one small step
 * above: 186 → 232 (+6%), enchant 2 600, and a slightly longer charge
 * (vanilla's hexed blade charges 200/400/600; this one 160/320/480, between it
 * and the Reaver's 150/300/450). It stays under the Crooked set's tier by
 * design: a village reward must not skip a realm.
 */
public class MemoryBlade extends GreatswordToolItem {

    public static final String ART = "hexedbladegreatsword";

    public MemoryBlade() {
        super(2600, (OneOfLootItems) null, getThreeChargeLevels(160, 320, 480));
        this.rarity = Item.Rarity.EPIC;
        this.attackDamage.setBaseValue(186.0F).setUpgradedValue(1.0F, 232.0F);
        this.attackRange.setBaseValue(100);
        this.knockback.setBaseValue(150);
        this.canBeUsedForRaids = true;
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + ART);
    }

    @Override
    protected void loadAttackTexture() {
        try {
            this.attackTexture = GameTexture.fromFileRaw("player/weapons/" + ART);
        } catch (java.io.FileNotFoundException e) {
            this.attackTexture = null;
        }
    }

    @Override
    public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
            GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
        String line = ItemDescription.of(this.getStringID());
        if (line != null) {
            tooltips.addFirst(line);
        }
        return tooltips;
    }
}
