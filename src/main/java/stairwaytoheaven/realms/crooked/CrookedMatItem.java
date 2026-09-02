package stairwaytoheaven.realms.crooked;

import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import stairwaytoheaven.items.SkyMatItem;

/**
 * A Crooked Beyond material, drawn with one of the game's OWN item icons.
 *
 * <h2>Why this class exists</h2>
 * The whole realm ships on borrowed art ({@code docs/WORLD_DESIGN.md} A4.3:
 * <i>"ich brauche vollständig aufgebaute biome mit geborgten assets"</i>), and
 * an item is the one place where "borrowed" needs a class rather than a string.
 * The engine's only rule about an item icon is the single line
 * {@code Item.loadItemTextures} contains — {@code this.itemTexture =
 * GameTexture.fromFile("items/" + getStringID())} (jar 1.3.2, Item.java:562) —
 * and that method is {@code protected}, so an item is free to point it
 * elsewhere. Vanilla does exactly that itself: {@code FoodConsumableItem} crops
 * a crop sheet when the food was given one, and {@code BucketItem} reads
 * {@code tiles/bucket}.
 *
 * <p>Because {@code ResourceEncoder} keeps ONE flat resource map keyed by path
 * with the mod's files merged into it (ResourceEncoder.java:75-86),
 * {@code items/ascendedshard} resolves from mod code exactly the way
 * {@code items/skystone} does. So the icon is the game's, byte for byte, with
 * no PNG of ours and nothing that can drift out of sync with one.
 *
 * <h2>What it deliberately does NOT do</h2>
 * It does not recolour. {@code livestock/SkyPelt} tints vanilla sheets into sky
 * palettes at load time, and that is the right answer for the livestock it was
 * written for — but the player has since ruled load-time recolouring out for new
 * content, so every icon here is the vanilla art unaltered. Each one is chosen
 * because it already reads as the thing it stands for, and each is listed in
 * {@code docs/realms/crooked.md} and {@code docs/VANILLA_ASSET_MAP.md} so the
 * replacement pass can find it.
 *
 * <p>{@code tools/locale_audit.py} knows this class by name through its
 * {@code ITEM_CLASS_VANILLA_ICON} table, which is what makes the borrowed path
 * <em>checked</em> against the sprite dump rather than merely skipped — a
 * mistyped vanilla path draws the same red ERR tile as a missing mod one.
 *
 * <p>It extends {@link SkyMatItem} rather than {@code MatItem} so every Crooked
 * material carries the same "what is this" tooltip line the rest of the mod's
 * materials do.
 */
public class CrookedMatItem extends SkyMatItem {

    private final String vanillaIcon;

    /**
     * @param vanillaIcon the game's own icon name, WITHOUT {@code items/} and
     *                    without the extension — e.g. {@code "ascendedshard"}.
     *                    It is constructor argument 0 because that is where
     *                    {@code locale_audit}'s {@code ITEM_CLASS_VANILLA_ICON}
     *                    entry looks for it.
     */
    public CrookedMatItem(String vanillaIcon, int stackSize, Item.Rarity rarity) {
        super(stackSize, rarity);
        this.vanillaIcon = vanillaIcon;
        this.setItemCategory("materials", "other");
    }

    @Override
    protected void loadItemTextures() {
        // Not finalized here: GameTexture.fromFile caches by path, so this hands
        // back the same instance the game's own item uses, and finalizing
        // someone else's texture out from under them is not ours to do.
        this.itemTexture = GameTexture.fromFile("items/" + this.vanillaIcon);
    }
}
