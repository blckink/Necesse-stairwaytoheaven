package stairwaytoheaven.surface;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import stairwaytoheaven.objects.SkyDecoObject;

/**
 * The <b>Fallen Skyshard</b>: what a {@link SkyfallWorldEvent} leaves on the
 * ground. Break it and it gives up the sky material it is made of.
 *
 * <h2>Art</h2>
 * It deliberately shares the mod's existing {@code objects/starfall} sprite —
 * it is the same substance, a lit sliver of sky-stone, and a Skyfall is where a
 * Starfall comes from. {@code SkyDecoObject} takes the texture NAME as a
 * constructor argument rather than reading the registered string ID, so no new
 * sheet is introduced and no generator output changes.
 *
 * <h2>Not obtainable as an item</h2>
 * Registered with {@code itemObtainable = false}, exactly the way vanilla
 * registers things that exist only to be broken. That is why it needs no
 * {@code items/skyfallshard.png}: {@code ItemRegistry.isObtainable} gates both
 * {@code GameObject.getLootTable}'s default and the creative list, so the icon
 * is never drawn. The loot is not the default — {@link #getLootTable} is
 * overridden, and {@code GameObject.getObjectDroppedItems} calls that override
 * whatever the item's obtainability says.
 *
 * <h2>Loot</h2>
 * Mod materials only, in small amounts. Skystone and Storm Shards are Skyreach
 * building and crafting materials; the Stairway itself is built from vanilla
 * tungsten and quartz, so nothing here brings the sky forward. Aetherium Ore is
 * deliberately NOT in this table: the ore feeds the mod's weapon tier, and a
 * repeatable night event is the wrong place to farm it — the rare crater POI
 * gives a taste of it instead, once, per crater.
 */
public class SkyfallShardObject extends SkyDecoObject {

    public static final String STRING_ID = "skyfallshard";

    public static final LootTable SHARD_LOOT = new LootTable(
            LootItem.between("skystone", 1, 3),
            new ChanceLootItem(0.18F, "stormshard", 1),
            new ChanceLootItem(0.10F, "aurorapetal", 1));

    public SkyfallShardObject() {
        super("starfall", 32, new Color(136, 216, 206), new Rectangle(10, 16, 12, 12),
                "objects", "decorations");
        // Clutter, not masonry: it breaks with anything, the way vanilla's
        // small ground debris does (docs/IMPLEMENTATION_RULES.md rule 4).
        this.setTool(ToolType.ALL);
        this.setObjectHealth(1);
        this.setLight(80, 0.50F, 0.40F);
    }

    @Override
    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
        return SHARD_LOOT;
    }
}
