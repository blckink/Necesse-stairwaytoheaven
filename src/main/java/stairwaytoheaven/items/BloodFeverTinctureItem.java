package stairwaytoheaven.items;

import java.awt.geom.Line2D;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.consumableItem.ConsumableItem;
import necesse.level.maps.Level;
import stairwaytoheaven.mobs.BloodFeverBuff;

/**
 * Blutfieber-Tinktur — the Doctor's cure in a bottle, decision E5 of
 * {@code docs/design/concept-nightbound-dorian.md}: <i>"Der Arzt verkauft ein
 * Heilmittel als Gegenstand ... Dann muss man nicht jedes Mal zum Arzt."</i>
 *
 * <p>Uncorked where the bitten are: every settler within
 * {@link #REACH_TILES} of the player loses {@link BloodFeverBuff}. It is the
 * player's own item, so it is used like vanilla's {@code stinkflask}
 * ({@code StinkFlaskItem}, VERIFIED [jar]: a {@code ConsumableItem} whose
 * {@code onPlace} runs the effect server side and takes one from the stack).
 *
 * <p><b>Price.</b> The Doctor's on-the-spot cure is 100 coins for everybody at
 * once; the tincture is 250 for ten tiles around wherever the player stands —
 * the concept asks for it to cost more than the visit, because it saves the
 * walk. It does nothing to the player.
 */
public class BloodFeverTinctureItem extends ConsumableItem {

    public static final String BORROWED_ICON = "greaterhealthregenpotion";
    public static final int REACH_TILES = 10;

    public BloodFeverTinctureItem() {
        super(10, true);
        this.rarity = Item.Rarity.UNCOMMON;
        this.attackAnimTime.setBaseValue(300);
        this.itemCooldownTime.setBaseValue(1000);
        this.worldDrawSize = 32;
        this.setItemCategory("consumable", "potions");
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + BORROWED_ICON);
    }

    @Override
    public String canPlace(Level level, int x, int y, PlayerMob player, Line2D playerPositionLine,
            InventoryItem item, GNDItemMap mapContent) {
        return null;
    }

    @Override
    public InventoryItem onPlace(Level level, int x, int y, PlayerMob player, int seed,
            InventoryItem item, GNDItemMap mapContent) {
        // Taken from the stack on both sides, exactly as vanilla's stink flask
        // does it, so the client's count never has to wait for a resync.
        if (this.isSingleUse(player)) {
            item.setAmount(item.getAmount() - 1);
        }
        if (level.isServer()) {
            float reach = REACH_TILES * 32.0F;
            for (necesse.entity.mobs.Mob m : level.entityManager.mobs.getInRegionByTileRange(
                    player.getTileX(), player.getTileY(), REACH_TILES)) {
                if (!m.isHuman || m.getDistance(player) > reach
                        || !m.buffManager.hasBuff(BloodFeverBuff.ID)) {
                    continue;
                }
                m.buffManager.removeBuff(BloodFeverBuff.ID, true);
            }
        }
        return item;
    }

    @Override
    public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
        String line = ItemDescription.of(this.getStringID());
        if (line != null) {
            tooltips.add(line);
        }
        return tooltips;
    }
}
