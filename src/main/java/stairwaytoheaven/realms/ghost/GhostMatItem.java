package stairwaytoheaven.realms.ghost;

import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import stairwaytoheaven.items.SkyMatItem;

/**
 * A material of the Ghost Realm, drawn with an icon that already exists.
 *
 * <h2>Why the icon is a constructor argument</h2>
 * {@code Item.loadItemTextures} (Item.java:569) is
 * {@code itemTexture = GameTexture.fromFile("items/" + getStringID())} — the
 * icon path is the item's own registered ID and nothing else. The Aftergarden
 * ships without a single new PNG, so every one of its materials would draw the
 * engine's red ERR tile in the inventory. {@code loadItemTextures} is
 * {@code protected}, which is the seam vanilla itself uses when an item's icon
 * is not named after it ({@code FoodConsumableItem} crops a crop sheet;
 * {@code BucketItem} reads {@code tiles/bucket}), so this overrides it and
 * reads the name it was given instead.
 *
 * <p>Every name passed here points at the GAME's own resources. That is not a
 * shortcut, it is the brief: {@code GameTexture.fromFile} reads one flat
 * resource map with the mod's files merged into the game's
 * ({@code ResourceEncoder.java:75-86}), so {@code items/silk} resolves from mod
 * code exactly as {@code items/veilessence} does, and a borrowed icon is
 * replaced later in one pass ({@code docs/VANILLA_ASSET_MAP.md}). Which file
 * stands in for what is listed in {@code docs/realms/ghost.md}, and
 * {@code tools/locale_audit.py} checks each of them against the game's sprite
 * dump rather than skipping them.
 *
 * <p>It extends {@link SkyMatItem} rather than {@code MatItem} so every ghost
 * material carries the mod's own description line
 * ({@code itemtooltip.<id>tip}) — a player holding Soul Thread should be told
 * it is a textile, which is exactly the complaint that class was written for.
 * The locale audit enforces that line in both languages.
 */
public class GhostMatItem extends SkyMatItem {

    private final String iconName;

    /**
     * @param iconName  file under {@code items/} to draw, WITHOUT the extension
     * @param stackSize how many fit in a slot
     * @param rarity    inventory rarity colour
     */
    public GhostMatItem(String iconName, int stackSize, Item.Rarity rarity) {
        super(stackSize, rarity);
        this.iconName = iconName;
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + this.iconName);
    }
}
