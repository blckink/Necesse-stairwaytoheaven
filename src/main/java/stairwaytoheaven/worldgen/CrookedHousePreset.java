package stairwaytoheaven.worldgen;

import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.DOWN;
import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.RIGHT;
import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.UP;

import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets.Legend;

/**
 * The Crooked House — the only building in the Beetlefreak Hollows.
 *
 * <h2>Whose house (2026-09-24)</h2>
 * Somebody lived here and kept watch -- that was always the story, told by two
 * raven statues and a pile of rubble in an empty shell. Now the house says who:
 * a <b>watcher's house</b>. Shelves either side of the north wall with the
 * clock between two ghost lanterns; a dinner table for six in the middle of
 * the main room (chairs on all four sides, turned to it); the bed and a
 * dresser in the west bulge; the kitchen along the south wall -- pot,
 * candelabra, cabinet, the pantry barrel and a sack; one raven standing in the
 * main room and the second on its rug in the east room, looking out of the
 * window it was put there to look out of.
 *
 * <h2>Why it is drawn as a character map</h2>
 * This building's whole point is that it is NOT symmetric: the walls step in
 * and out, the left side bulges a row lower than the right, and one room hangs
 * off the east face. Written as a map you can see the silhouette in the
 * source. {@code tools/preset_seal_check.py} floods the outside and asserts no
 * interior cell is reached; {@code VeilStatusCommand} counts this plan's
 * {@code #}, {@code O} and {@code D} to check the house in the world.
 *
 * <h2>Where a window may go</h2>
 * {@code WallWindowObject.isValid} rejects itself outright when
 * {@code getWindowDir} returns -1: a window must be mid-run in a straight wall
 * of its own family. The first draft of this house put two windows in the
 * inside of the left steps and shipped with 1 of its 3; all three now sit
 * mid-run, and {@link RealmPoiPresets#plan} throws at load if one ever moves
 * into a corner. Furniture goes through the same interpreter, so both halves
 * of the bed and the table are written and every chair faces its table.
 */
public class CrookedHousePreset extends Preset {

    public static final int WIDTH = 15;
    public static final int HEIGHT = 13;

    /** The house, drawn one character per tile; ' ' writes nothing. */
    public static final String[] PLAN = {
            "  ###O###O###  ",
            "  #S==^K^==S#  ",
            "  #=========#  ",
            "###G===h====#  ",
            "#R====iTi===#  ",
            "O=====iti===###",
            "#E=====h====:V#",
            "#e==========::O",
            "O===========###",
            "#lb=c=o====k#  ",
            "###=========#  ",
            "  #####D#####  ",
            "      ,,,      ",
    };

    public CrookedHousePreset(GameRandom random) {
        super(WIDTH, HEIGHT);
        Legend legend = new Legend(SkyRegistry.gloomwoodFloorID)
                .floor('=')
                .floor(',', "beetlefreaktile")
                .rug(':', "purplecarpet")
                .wall('#', "beetlewall")
                .window('O', "beetlewindow")
                .door('D', "beetledoor")
                // The Veil's own green-flame lamp, standing either side of the clock.
                .prop('^', "ghostlantern")
                .prop('S', "deadwoodbookshelf", DOWN)
                .prop('K', "deadwoodclock", DOWN)
                .prop('G', "gloomravenstatue")
                .pair('T', 't', "deadwooddinnertable", DOWN)
                .serves('T', "oldplate").serves('t', "oldchalices")
                .chair('i', "deadwoodchair")
                .chair('h', "deadwoodchair")
                .prop('R', "deadwooddresser", RIGHT)
                .pair('E', 'e', "deadwoodbed", DOWN)
                .prop('V', "gloomravenstatue").carpet('V', "purplecarpet")
                .prop('b', "barrel")
                .prop('l', "sack")
                .prop('c', "deadwoodcandelabra")
                .prop('o', "cookingpot")
                .prop('k', "deadwoodcabinet", UP);
        RealmPoiPresets.plan(this, PLAN, legend);

        // What is left of the pantry.
        RealmPoiPresets.stock(this, PLAN, 'b', new LootTable(
                LootItem.between("veilessence", 2, 6),
                LootItem.between("cinderpearl", 1, 3),
                LootItem.between("charwood", 5, 14)
        ), random);
    }
}
