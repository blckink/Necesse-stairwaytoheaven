package stairwaytoheaven.realms.ghost;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import stairwaytoheaven.objects.SkyDecoObject;

/**
 * A world object of the Ghost Realm, drawn on a sheet that already exists.
 *
 * <h2>Why this class exists at all</h2>
 * {@link SkyDecoObject} already takes its WORLD sheet as a constructor
 * argument, which is most of what the Aftergarden needs — the realm ships
 * without a single new PNG, so every gravestone, coffin, urn and dead tree in
 * it points at a sheet that is already in this repository or in the game's own
 * resources. What it does not take is the ITEM icon: {@code GameObject
 * .generateItemTexture} (GameObject.java:791) is hard coded to
 * {@code items/<stringID>.png}, so a borrowed object registered obtainable
 * would put the engine's red ERR tile in the player's inventory the moment they
 * picked one up.
 *
 * <p>So the icon is a second constructor argument and
 * {@link #generateItemTexture()} reads it. That is the same seam vanilla itself
 * uses — {@code RockObject.generateItemTexture} (RockObject.java:116) reads
 * {@code items/<rockTextureName>}, its own constructor argument, rather than
 * its registered ID — and it is why {@code veilrock} can be an object called
 * one thing drawn from a file called another.
 *
 * <p>Both names resolve through {@code GameTexture.fromFile}, which reads ONE
 * flat resource map with the mod's files merged into the game's
 * ({@code ResourceEncoder.java:75-86}). {@code objects/deadtree} (ours) and
 * {@code objects/cryptgravestone1} (the game's) are looked up identically;
 * which repository a path belongs to is a fact about where to check it, not
 * about how it fails. {@code tools/locale_audit.py} checks both, and every
 * borrow is listed in {@code docs/realms/ghost.md}.
 *
 * <h2>What it deliberately does not do</h2>
 * It does not recolour anything. The sheets are drawn as they were drawn; the
 * realm's palette comes from which sheets were chosen, from the tiles under
 * them and from the light they cast. Recolouring at load time is forbidden for
 * new content by the player's own instruction.
 */
public class GhostDecoObject extends SkyDecoObject {

    private final String iconName;
    private LootTable lootTable;

    /**
     * @param textureName  world sheet under {@code objects/}, ours or the game's
     * @param iconName     inventory icon under {@code items/}, ours or the game's
     * @param variantWidth pixel width of one variant column in the world sheet
     * @param mapColor     what it looks like on the world map
     * @param collision    solid box, or null for a walk-through prop
     * @param category     item/crafting category, or empty for objects/decorations
     */
    public GhostDecoObject(String textureName, String iconName, int variantWidth,
            Color mapColor, Rectangle collision, String... category) {
        super(textureName, variantWidth, mapColor, collision, category);
        this.iconName = iconName;
    }

    /** What breaking this yields, instead of the object's own item. */
    public GhostDecoObject setDrops(LootTable lootTable) {
        this.lootTable = lootTable;
        return this;
    }

    @Override
    public GameTexture generateItemTexture() {
        return new GameTexture(GameTexture.fromFile("items/" + this.iconName));
    }

    @Override
    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
        return this.lootTable != null
                ? this.lootTable
                : super.getLootTable(level, layerID, tileX, tileY);
    }
}
