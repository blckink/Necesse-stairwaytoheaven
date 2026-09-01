package stairwaytoheaven.tiles;

import java.awt.Color;

import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;

/**
 * Beetlefreak ground: the striped, flowering, wrong-coloured floor of the
 * Veil's maddest places.
 *
 * <p>Built on vanilla's {@code SpiderNestTile} setup rather than on our own
 * terrain tiles, because the artwork was drawn on that template. The detail
 * that matters and is easy to miss: SpiderNestTile passes a **third**
 * constructor argument, {@code "splattingmaskwide"} — a different alpha mask
 * from the {@code "splattingmask"} default every other terrain tile in this mod
 * uses. The blend shapes in the supplied sheet were cut for the wide mask, so
 * dropping the argument would blend it with the wrong stencil.
 *
 * <p>Otherwise it follows the vanilla original: mineable, organic, priority
 * 200, an empty loot table so mining it yields nothing, and a per-tile random
 * variant row. Vanilla grows cobwebs on its nest and spawns spiders where they
 * are; ours deliberately grows nothing, because this ground marks places that
 * are already wrong and does not need to spread.
 */
public class BeetlefreakTile extends SkyGroundTile {

    public BeetlefreakTile() {
        super(false, "beetlefreak", "splattingmaskwide");
        this.mapColor = new Color(118, 46, 158);
        this.canBeMined = true;
        this.isOrganic = true;
    }

    /** Mining it yields nothing, matching the vanilla tile it is built on. */
    @Override
    public LootTable getLootTable(Level level, int tileX, int tileY) {
        return new LootTable();
    }

    @Override
    public int getTerrainPriority() {
        return 200;
    }
}
