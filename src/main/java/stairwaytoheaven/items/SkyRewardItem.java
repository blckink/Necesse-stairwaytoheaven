package stairwaytoheaven.items;

import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;

/**
 * One of the eight unique rewards of {@code chapter-01-skyreach-cast.md} §3,
 * drawn with an icon that already exists.
 *
 * <h2>Why the icon is a constructor argument</h2>
 * {@code Item.loadItemTextures} (Item.java:569) is
 * {@code itemTexture = GameTexture.fromFile("items/" + getStringID())} — the
 * icon path is the item's own registered ID and nothing else. These eight are
 * the chapter brief's art order and nobody has drawn them, so without this each
 * would show the engine's red ERR tile in the inventory, which
 * {@code docs/IMPLEMENTATION_RULES.md} §5 calls a release blocker.
 * {@code loadItemTextures} is {@code protected}, which is the seam vanilla
 * itself uses when an item's icon is not named after it
 * ({@code FoodConsumableItem} crops a crop sheet; {@code BucketItem} reads
 * {@code tiles/bucket}), so this reads the name it was given instead.
 *
 * <p>The same class {@code realms/ghost/GhostMatItem} is, one realm over, and
 * for the same reason. Which file stands in for what is listed in
 * {@code docs/VANILLA_ASSET_MAP.md} §1.7, and {@code tools/locale_audit.py}
 * checks each borrowed path against the game's sprite dump rather than skipping
 * it — the entry is {@code "SkyRewardItem": ("arg", 0)}.
 *
 * <p>It extends {@link SkyMatItem} rather than {@code MatItem} so every reward
 * carries the mod's own description line ({@code itemtooltip.<id>tip}): a
 * player holding a Bonded Lockbox should be told what it is for, and the locale
 * audit enforces that line in both languages.
 */
public class SkyRewardItem extends SkyMatItem {

    private final String iconName;

    /**
     * @param iconName  file under {@code items/} to draw, WITHOUT the extension
     * @param stackSize how many fit in a slot; 1 for the unique keys
     * @param rarity    inventory rarity colour
     */
    public SkyRewardItem(String iconName, int stackSize, Item.Rarity rarity) {
        super(stackSize, rarity);
        this.iconName = iconName;
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + this.iconName);
    }
}
