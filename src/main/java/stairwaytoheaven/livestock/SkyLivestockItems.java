package stairwaytoheaven.livestock;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.armorItem.HelmetArmorItem;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;
import necesse.level.maps.levelData.settlementData.settler.FoodQuality;

/**
 * The item classes behind the Skyreach's livestock produce.
 *
 * <p>Most of them draw themselves out of a recoloured vanilla texture; the
 * Glimmerstride Boots now use the player's dedicated item and armour sheets.
 * The engine's only rule
 * about an item icon is the one line {@code Item.loadItemTextures} contains —
 * {@code this.itemTexture = GameTexture.fromFile("items/" + getStringID())}
 * (jar 1.3.2, Item.java:562) — and that method is {@code protected}, so an item
 * is free to point it anywhere. Vanilla itself does exactly that:
 * {@code FoodConsumableItem.loadItemTextures} crops a crop sheet instead when
 * the food was given one, and {@code BucketItem} reads {@code tiles/bucket}.
 *
 * <p>{@link SkyPelt} explains why the source is vanilla art: the resource map
 * is flat and shared, so {@code items/milk} and {@code player/armor/clothhat}
 * resolve from mod code, and recolouring them keeps vanilla's shading,
 * silhouette and — for armour — the body-layer alignment the human renderer
 * expects.
 *
 * <p>{@code tools/locale_audit.py} knows these four classes by name: its
 * {@code ITEM_CLASS_DRAWS_ITSELF} set is the list of item classes whose icon is
 * NOT {@code items/&lt;stringID&gt;.png}, and an item class missing from it is
 * reported as having no icon file.
 */
public final class SkyLivestockItems {

    private SkyLivestockItems() {
    }

    /**
     * Food and drink pressed, cooked or brewed out of livestock produce.
     *
     * <p>The icon is left un-finalized on purpose: {@code FoodConsumableItem
     * .loadTextures} reads every pixel of {@code itemTexture} back to composite
     * the buff icon and finalizes it itself afterwards.
     */
    public static class LivestockFood extends FoodConsumableItem {

        private final String vanillaIcon;
        private final float hue;
        private final float satFloor;

        public LivestockFood(String vanillaIcon, float hue, float satFloor, int stackSize, Item.Rarity rarity,
                    FoodQuality quality, int nutrition, int buffSeconds, boolean drinkSound,
                    ModifierValue<?>... modifiers) {
            super(stackSize, rarity, quality, nutrition, buffSeconds, drinkSound, modifiers);
            this.vanillaIcon = vanillaIcon;
            this.hue = hue;
            this.satFloor = satFloor;
        }

        @Override
        protected void loadItemTextures() {
            this.itemTexture = SkyPelt.tint("items/" + this.vanillaIcon, "items/" + this.getStringID(),
                    this.hue, this.satFloor, 0.45F, 1.0F, 0.02F);
        }

        /**
         * The same "what is this" line every Skyreach material carries.
         *
         * <p>Milk is not a {@code MatItem}, so it cannot inherit
         * {@link stairwaytoheaven.items.SkyMatItem}'s version — but
         * {@code FoodConsumableItem.getTooltips} is overridable (unlike
         * {@code ArmorItem}'s and {@code TrinketItem}'s, which are final), so
         * the line goes on here directly.
         */
        @Override
        public necesse.gfx.gameTooltips.ListGameTooltips getTooltips(
                InventoryItem item, necesse.entity.mobs.PlayerMob perspective,
                necesse.engine.util.GameBlackboard blackboard) {
            necesse.gfx.gameTooltips.ListGameTooltips tooltips =
                    super.getTooltips(item, perspective, blackboard);
            String line = stairwaytoheaven.items.ItemDescription.of(this.getStringID());
            if (line != null) {
                tooltips.add(line);
            }
            return tooltips;
        }
    }

    /**
     * A raw livestock product: the thing that comes off the animal.
     *
     * <p>Extends {@link stairwaytoheaven.items.SkyMatItem} rather than
     * {@code MatItem} so Aurora Fleece carries the same
     * "what is this" line as every other Skyreach material.
     */
    public static class LivestockProduce extends stairwaytoheaven.items.SkyMatItem {

        private final String vanillaIcon;
        private final float hue;
        private final float satFloor;

        public LivestockProduce(String vanillaIcon, float hue, float satFloor, int stackSize, Item.Rarity rarity) {
            super(stackSize, rarity);
            this.vanillaIcon = vanillaIcon;
            this.hue = hue;
            this.satFloor = satFloor;
            this.setItemCategory("materials", "mobdrops");
        }

        @Override
        protected void loadItemTextures() {
            this.itemTexture = SkyPelt.tintFinal("items/" + this.vanillaIcon, "items/" + this.getStringID(),
                    this.hue, this.satFloor, 0.45F, 1.0F, 0.02F);
        }
    }

    /** The Glimmerstride Boots: felted aurora fleece, warm and quick. */
    public static class GlimmerstrideBoots extends BootsArmorItem {

        public GlimmerstrideBoots() {
            super(7, 100, Item.Rarity.UNCOMMON, "glimmerstrides", null);
        }

        @Override
        protected void loadItemTextures() {
            this.itemTexture = GameTexture.fromFile("items/glimmerstrides");
        }

        @Override
        protected void loadArmorTexture() {
            this.armorTexture = GameTexture.fromFile("player/armor/glimmerstrides");
        }

        @Override
        public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
            return new ArmorModifiers(
                    new ModifierValue<>(necesse.entity.mobs.buffs.BuffModifiers.SPEED, 0.06F),
                    new ModifierValue<>(necesse.entity.mobs.buffs.BuffModifiers.HEALTH_REGEN_FLAT, 0.4F));
        }
    }
}
