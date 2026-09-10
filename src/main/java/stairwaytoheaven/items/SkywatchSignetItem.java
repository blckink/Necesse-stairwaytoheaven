package stairwaytoheaven.items;

import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;

/**
 * The Skywatch Signet — reward 8 of {@code chapter-01-skyreach-cast.md} §3:
 * <i>"a trinket in the shipped accessory family that reveals unexplored
 * Skyreach structures on the map within a radius. For: the 'I cannot find
 * anything up here' problem, as a reward rather than a UI feature."</i>
 *
 * <p>That is not a bespoke feature here, because the engine already has it.
 * VERIFIED [jar]: {@code BuffModifiers.EXTENDED_MAP_DISCOVER_RANGE} is the
 * modifier {@code ClientDiscoveredMap.discover} (ClientDiscoveredMap.java:247)
 * reads to widen how far a walking player uncovers the world map, and vanilla
 * grants it through exactly one trinket — {@code piratetelescopetrinket},
 * {@code new SimpleTrinketBuff("piratetelescopemap", EXTENDED_MAP_DISCOVER_RANGE
 * true)} (BuffRegistry.java:690). {@code TREASURE_HUNTER} is the second half of
 * the same wish: it is what makes crates and vending machines light up through
 * a wall ({@code RandomCrateObject.java:128}), i.e. it makes the structures the
 * wider map turns up worth walking to. Its vanilla anchor is
 * {@code treasurepotion} (BuffRegistry.java:449).
 *
 * <p>Both modifiers are booleans, so the Signet is a strictly additive utility
 * accessory: it takes no combat budget from the three stat trinkets the mod
 * already ships, and it therefore declares no {@code addDisables} against them.
 * It DOES disable {@code piratetelescope}, for the reason
 * {@code SkyItems.registerGear} gives for every other exclusion — vanilla never
 * lets the same shape stack with itself once an item carries the whole of it.
 *
 * <p><b>The icon</b> is {@code items/emptypendant}, borrowed rather than drawn,
 * the way {@link SkyRewardItem} explains and {@code docs/VANILLA_ASSET_MAP.md}
 * §1.7 records. {@code TrinketItem} never reads the texture back, so unlike
 * {@link WardensRoundItem} this one may finalize normally.
 */
public class SkywatchSignetItem extends SkyTrinketItem {

    /** The file under {@code items/} this draws; named for {@code tools/locale_audit.py}. */
    public static final String ICON = "emptypendant";

    public SkywatchSignetItem(Item.Rarity rarity, String buffStringID, int enchantCost) {
        super(rarity, buffStringID, enchantCost);
    }

    @Override
    protected void loadItemTextures() {
        this.itemTexture = GameTexture.fromFile("items/" + ICON);
    }
}
