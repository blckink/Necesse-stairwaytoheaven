package stairwaytoheaven.realms.ghost;

import java.awt.Color;

import necesse.inventory.lootTable.LootTable;
import necesse.level.gameObject.GrassObject;
import necesse.level.maps.Level;

/**
 * A soft plant of the Ghost Realm: ghost lilies, ectoplasm fern, mourning
 * roses, widow vine.
 *
 * <p>Archetype: vanilla's {@link GrassObject}, which is what every pickable
 * flower and clump of grass in the game is — swing at it with anything, it
 * comes away in one hit, and it drops whatever its loot table says. That is
 * {@code IMPLEMENTATION_RULES} §4's answer for soft flora, and it is why these
 * are not {@code GameObject}s with the pickaxe default.
 *
 * <p>The sheet is {@code GrassObject}'s own first constructor argument, so
 * these borrow the Veil's and the Skyreach's existing plant sheets rather than
 * needing new art (see {@code docs/realms/ghost.md}).
 *
 * <h2>Why they are registered unobtainable</h2>
 * {@code ObjectRegistry.onRegister} gives every registered object an
 * {@code ObjectItem}, and {@code GameObject.getLootTable} hands that item over
 * whenever it is obtainable — which would need an {@code items/<id>.png} that
 * does not exist, and would put the engine's ERR tile in the inventory. These
 * are natural growth, not furniture: a player clears one and gets the
 * MATERIAL, which is what the loot table below is. Vanilla registers its own
 * wild mushroom the same way and ships no icon for it either.
 */
public class GhostPlantObject extends GrassObject {

    private final LootTable lootTable;

    /**
     * @param textureName sheet under {@code objects/} — ours, borrowed
     * @param variants    how many variant columns the sheet carries
     * @param mapColor    what it looks like on the world map
     * @param lootTable   what clearing one yields
     */
    public GhostPlantObject(String textureName, int variants, Color mapColor, LootTable lootTable) {
        super(textureName, variants);
        this.mapColor = mapColor;
        this.lootTable = lootTable;
    }

    @Override
    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
        return this.lootTable;
    }
}
