package stairwaytoheaven.realms.steinfeld;

import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.DOWN;
import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.LEFT;

import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets.Legend;

/**
 * A roofless nave, hand-laid rather than grown from the noise field —
 * {@code docs/WORLD_DESIGN.md} §7's "broken angel statues" made into a PLACE
 * rather than a scatter, on {@link SteinfeldSites}' own rare lattice.
 *
 * <h2>A chapel, not a colonnade (2026-09-24)</h2>
 * Until now: an angel, two mourners, eight pillars, two slabs and a crate.
 * Now the nave is furnished as the chapel it was. At the head, the broken
 * angel where the altar-piece stood, between two candle pedestals still lit;
 * before it the altar, a birch table carrying the chalices -- their gold gone
 * dark, which A3.4 names as the one sign of Skyreach's own material ageing --
 * with a mourner at either end and the sacristy chest in the corner. The
 * lectern stands on the gospel side with the book still on it. Down the nave,
 * pews either side of the aisle, all facing the altar; the roof came down on
 * one of them, and a slab and its rubble lie where the pew was. Pale grass and
 * widow flowers break up through the marble at the door end.
 *
 * <p>No walls: a roof and its walls are what a RUIN has lost, and open
 * colonnades read as "roofless" the moment the sky shows between them.
 *
 * <p>The loot moved from a {@code skycrate} -- a {@code RandomCrateObject},
 * which has no inventory and so never held any of it -- into the sacristy
 * {@code birchchest}.
 */
public class RuinedChapelPreset extends Preset {

    public static final int WIDTH = 11;
    public static final int HEIGHT = 15;

    /** The nave, drawn one character per tile. */
    public static final String[] PLAN = {
            ";;;k;A;k;;;",
            ";XM;TTT;M;;",
            ";;;;;;;;;;;",
            ";L;;x;;;;L;",
            ";;nN;;;nN;;",
            ";;;;;;;;;;;",
            ";LnN;;;nNL;",
            ";;;;;;;;;;;",
            ";;Sr;;;nN;;",
            ";L;;;;;;;L;",
            ";;nN;;;nN;;",
            ";;;;;;;;;;;",
            ";L;;;;;;;L;",
            ";;w;;;;;p;;",
            ";;;;;;;;;;;",
    };

    public RuinedChapelPreset(GameRandom random) {
        super(WIDTH, HEIGHT);
        Legend legend = new Legend(SkyRegistry.crackedmarbleID)
                .floor(';')
                .prop('A', "brokenangel")
                .prop('M', "mournerstatue")
                .prop('k', "stonecandlepedestal")
                .prop('X', "birchchest", DOWN)
                .table('T', "birchmodulartable", "oldchalices", "spilledgoldchalice", "goldchalice")
                .table('x', "birchmodulartable", "stackedbooks")
                .prop('L', "chapelcolumn")
                // Pews: turned to 3, the far half west, facing the altar.
                .pair('N', 'n', "birchbench", LEFT)
                .prop('S', "heavenslab")
                .prop('r', "skywatchrubble")
                // A GrassObject is deleted off inorganic ground, and marble is
                // not organic: the weeds bring their own patch of pale grass.
                .prop('w', "widowflower").paves('w', "palegrasstile")
                .prop('p', "palereed").paves('p', "palegrasstile");
        RealmPoiPresets.plan(this, PLAN, legend);
        RealmPoiPresets.stock(this, PLAN, 'X', LOOT, random);
    }

    /**
     * What a chapel this far from the door still keeps. Cracked Heaven
     * Marble is the nave's own floor, sold back to the player who breaks in;
     * Echo Shard is more common here than anywhere else in the realm — a
     * chapel is where the apparitions §7 mentions were seen last. Quantities
     * at the realm's x1.6 drop value.
     */
    public static final LootTable LOOT = new LootTable(
            LootItem.between("palestone", 6, 14),
            ChanceLootItem.between(0.50F, "echoshard", 1, 3),
            ChanceLootItem.between(0.35F, "spiritmoss", 2, 4),
            ChanceLootItem.between(0.22F, "gravesalt", 2, 5));
}
