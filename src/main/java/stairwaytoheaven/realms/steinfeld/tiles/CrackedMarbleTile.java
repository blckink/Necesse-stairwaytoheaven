package stairwaytoheaven.realms.steinfeld.tiles;

import java.awt.Color;

/**
 * Cracked Heaven Marble — the big slabs. Paving somebody laid, now simply
 * terrain: the Slab Fields are named after it.
 *
 * <p><b>Borrowed art:</b> vanilla {@code tiles/stonetiledfloor_splat}, read out
 * of the game's own resources by path. Vanilla registers that sheet as a
 * crafted FLOOR ({@code SimpleFloorTile}); this is deliberately TERRAIN,
 * because in Steinfeld the slabs are the ground rather than something a player
 * put down — {@link SteinfeldGroundTile} is what makes them behave like ground
 * (blended edges, terrain mining, and the spawn-ticket hook A4.1 needs).
 * Nothing is copied into this mod's resources; a copy would be a fork and a
 * fork does not swap out cleanly (see {@code docs/VANILLA_ASSET_MAP.md}).
 */
public class CrackedMarbleTile extends SteinfeldGroundTile {

    public CrackedMarbleTile() {
        super("stonetiledfloor");
        this.mapColor = new Color(120, 126, 134);
    }

    @Override
    public int getTerrainPriority() {
        // Lowest of the seven: everything else creeps over the buried paving.
        return 240;
    }
}
