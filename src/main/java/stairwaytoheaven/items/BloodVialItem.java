package stairwaytoheaven.items;

import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;

/**
 * Blood Vial / Blutfläschchen — what the vampire brings home from a night hunt.
 *
 * <p>An ordinary material in every respect: it stacks, it sells, and the
 * planned Daywalk quest will ask for a dozen of them. What is unusual is the
 * icon, which is vanilla's red potion borrowed by literal path
 * ({@code Item.loadItemTextures} is the only hook that names the file, and it
 * names {@code items/<stringID>}). That keeps the item shippable before it has
 * art of its own — the same trade the mod's borrowed mob icons make — and the
 * placeholder is one line to replace once a real vial is drawn.
 */
public class BloodVialItem extends SkyMatItem {

    /** Vanilla's health potion: a small red bottle, which is near enough. */
    public static final String BORROWED_ICON = "items/healthpotion";

    public BloodVialItem() {
        super(500, Item.Rarity.UNCOMMON);
        this.setItemCategory("materials", "mobdrops");
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile(BORROWED_ICON);
    }
}
