package stairwaytoheaven.items;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;
import necesse.level.maps.levelData.settlementData.settler.Settler;

/**
 * The Warden's Round — reward 5 of {@code chapter-01-skyreach-cast.md} §3.
 *
 * <p>"One aged barrel, deepest cell of the grange cellar, once per world. A
 * single unique consumable with a long, strong Skywatch buff; afterwards only
 * Halda can make more." Halda's Fermentation Vat is not built (it is the
 * brief's own new station and needs new art), so today the barrel in the deep
 * cell is the only one there is — which is exactly what the brief calls the
 * once-per-world half of the reward.
 *
 * <h2>Numbers</h2>
 * The anchor is the mod's own top drink, {@code nimbusdraught}: ARMOR_FLAT 6 +
 * MAX_RESILIENCE_FLAT 20 over 900 seconds ({@code SkyLivestock.register}). The
 * Round is the endgame consumable slot, so it takes twice that duration and a
 * strictly larger shape — flat health on top of both — and it does NOT spoil,
 * because it is the barrel that outlasted the household.
 *
 * <h2>The icon</h2>
 * {@code items/skywatchchalice} — the mod's own Skywatch cup, which is what the
 * Round is served in. Borrowed rather than drawn, the way
 * {@link SkyRewardItem} explains and {@code docs/VANILLA_ASSET_MAP.md} §1.7
 * records. The texture is deliberately left un-finalized: {@code
 * FoodConsumableItem.loadTextures} reads every pixel of {@code itemTexture}
 * back to composite the buff icon and finalizes it itself afterwards, which is
 * the same reason {@code SkyLivestockItems.LivestockFood} leaves its own alone.
 */
public class WardensRoundItem extends FoodConsumableItem {

    /** The file under {@code items/} this draws; named for {@code tools/locale_audit.py}. */
    public static final String ICON = "skywatchchalice";

    public WardensRoundItem() {
        super(1, Item.Rarity.RARE, Settler.FOOD_FINE, 30, 1800, true,
                new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 60),
                new ModifierValue<>(BuffModifiers.ARMOR_FLAT, 12),
                new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 40));
    }

    @Override
    protected void loadItemTextures() {
        // forceNotFinalize: loadTextures() above reads this back pixel by pixel.
        this.itemTexture = GameTexture.fromFile("items/" + ICON, true);
    }

    /**
     * The same "what is this" line every Skyreach reward carries.
     * {@code FoodConsumableItem.getTooltips} is overridable (unlike
     * {@code ArmorItem}'s and {@code TrinketItem}'s, which are final).
     */
    @Override
    public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective,
            GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
        String line = ItemDescription.of(this.getStringID());
        if (line != null) {
            tooltips.add(line);
        }
        return tooltips;
    }
}
