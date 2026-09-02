package stairwaytoheaven.realms.steinfeld;

import java.awt.Color;

import necesse.inventory.lootTable.LootTable;
import necesse.level.gameObject.GrassObject;
import necesse.level.maps.Level;

/**
 * A soft plant of Steinfeld: Pale Reed, Widow Flower, Dead Heaven Bloom, Ghost
 * Mushroom, Spirit Moss.
 *
 * <p>Archetype: vanilla's {@link GrassObject}, the same base
 * {@code GhostPlantObject} builds on and for the same reason —
 * {@code IMPLEMENTATION_RULES} §4's answer for soft flora: one hit, no
 * pickaxe default, drops whatever its loot table says. The sheet is
 * {@code GrassObject}'s own first constructor argument, so every one of these
 * borrows an existing mod sheet (see {@code docs/realms/steinfeld.md}) rather
 * than needing new art.
 *
 * <p>Registered non-obtainable, exactly like {@code GhostPlantObject}: these
 * are natural growth, not furniture, and a player clears one for whatever the
 * loot table hands back. Most of Steinfeld's five give nothing at all —
 * they exist to be looked at, not farmed, which is its own kind of honesty:
 * {@code docs/WORLD_DESIGN.md} A4.5 says a material with no consumer is
 * either loot or clutter, and Pale Reed, Widow Flower, Dead Heaven Bloom and
 * Ghost Mushroom are deliberately clutter — atmosphere, not inventory. Only
 * Spirit Moss carries a real material, because only Spirit Moss is asked for
 * anywhere else (§9's séance quest).
 */
public class SteinfeldPlantObject extends GrassObject {

    private final LootTable lootTable;

    /**
     * @param textureName sheet under {@code objects/} — an existing mod or
     *                     vanilla sheet, never a new one
     * @param variants     how many variant columns the sheet carries
     * @param mapColor     what it looks like on the world map
     * @param lootTable    what clearing one yields; {@code new LootTable()} for
     *                     pure decoration
     */
    public SteinfeldPlantObject(String textureName, int variants, Color mapColor, LootTable lootTable) {
        super(textureName, variants);
        this.mapColor = mapColor;
        this.lootTable = lootTable;
    }

    @Override
    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
        return this.lootTable;
    }
}
