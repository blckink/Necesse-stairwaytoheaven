package stairwaytoheaven.objects;

import necesse.inventory.lootTable.LootTable;
import necesse.level.gameObject.GrassObject;
import necesse.level.maps.Level;

/**
 * Ground cover that behaves like vanilla's own grass when it is cut.
 *
 * <p>Vanilla {@code SurfaceGrassObject.getLootTable} (jar 1.3.2,
 * SurfaceGrassObject.java:22-30): grass the PLAYER planted gives itself back;
 * grass the world grew gives a small chance of worm bait and a 1% chance of its
 * seed, never a stack of "grass" objects. Our reeds and Eden grass were plain
 * {@code GrassObject}s registered obtainable, so the default
 * {@code GameObject.getLootTable} handed every cut blade back as a placeable
 * object -- clearing a patch of sky meadow filled the bag with objects
 * (docs/ITEM_CATEGORIES.md, 2026-09-24). Same IDs, same registration; only the
 * natural drop changes, so saves are untouched.
 */
public class NaturalGrassObject extends GrassObject {

    private final LootTable naturalLoot;

    public NaturalGrassObject(String textureName, int density, LootTable naturalLoot) {
        super(textureName, density);
        this.naturalLoot = naturalLoot;
    }

    @Override
    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
        if (level.objectLayer.isPlayerPlaced(tileX, tileY)) {
            return super.getLootTable(level, layerID, tileX, tileY);
        }
        return this.naturalLoot;
    }
}
