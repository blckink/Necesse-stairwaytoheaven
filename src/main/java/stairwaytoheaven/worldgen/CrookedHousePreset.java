package stairwaytoheaven.worldgen;

import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;

/**
 * The Crooked House — the only building in the Beetlefreak Hollows.
 *
 * <h2>Why it is drawn as a character map</h2>
 * Every other preset in this mod writes tile coordinates one call at a time,
 * which is fine for a symmetric plan like the spire. This building's whole
 * point is that it is NOT symmetric: the walls step in and out, the left side
 * bulges a row lower than the right, and one room hangs off the east face.
 * Written as coordinates that is unreadable and one typo away from a hole in
 * the wall; written as a map you can see the silhouette in the source. The
 * map is verified sealed by {@code tools/preset_seal_check.py}, which floods
 * the outside and asserts no interior cell is reached.
 *
 * <h2>Legend</h2>
 * <pre>
 *   space  not written at all (the Hollows' own ground shows through)
 *   #      beetlefreak wall
 *   O      beetlefreak window
 *   D      beetlefreak door
 *   .      interior: gloomwood planks
 * </pre>
 *
 * <h2>Where a window may go</h2>
 * Not anywhere in the wall. {@code WallWindowObject.isValid} (line 131) rejects
 * itself outright when {@code getWindowDir} returns -1, and that method (line
 * 75) only accepts a window whose connected walls are exactly one opposite
 * pair: up+down with neither side, or left+right with neither end. A window
 * tucked into a CORNER has an up and a left neighbour, scores -1, and is
 * silently deleted the moment the level validates it.
 *
 * The first draft put two windows at the inside of the left steps and shipped a
 * house with 1 of its 3 windows; the veilstatus per-house probe is what caught
 * it. All three now sit mid-run in a straight wall.
 *
 * Furniture is placed after the map, by coordinate, because there are only a
 * dozen pieces and each one wants a rotation.
 *
 * <h2>Multi-tile</h2>
 * None on purpose. {@code Preset.applyToLevel} writes IDs straight into the
 * object layer and never runs {@code MultiTile.placeObject}, so every
 * multi-tile piece would need its {@code <id>2} half written by hand (see
 * {@link WardenSpirePreset}). This house furnishes itself entirely from
 * single-tile props, which removes that whole class of mistake.
 */
public class CrookedHousePreset extends Preset {

    public static final int WIDTH = 15;
    public static final int HEIGHT = 13;

    /** The house, drawn. Every row is exactly {@link #WIDTH} characters. */
    public static final String[] PLAN = {
            "  ###########  ",
            "  #.........#  ",
            "  #.........#  ",
            "###.........#  ",
            "#...........#  ",
            "O...........###",
            "#.............#",
            "#.............O",
            "O...........###",
            "#...........#  ",
            "###.........#  ",
            "  #####D#####  ",
            "               ",
    };

    public CrookedHousePreset(GameRandom random) {
        super(WIDTH, HEIGHT);

        final int wall = SkyRegistry.beetleWallID;
        final int door = SkyRegistry.beetleDoorClosedID;
        final int window = SkyRegistry.beetleWindowID;
        final int planks = SkyRegistry.gloomwoodFloorID;

        for (int y = 0; y < PLAN.length; y++) {
            String row = PLAN[y];
            for (int x = 0; x < row.length(); x++) {
                switch (row.charAt(x)) {
                    case '#':
                        this.setObject(x, y, wall);
                        break;
                    case 'O':
                        this.setObject(x, y, window);
                        break;
                    case 'D':
                        // The threshold gets floor too, or the doorway reads as
                        // a gap in the ground when you stand in it.
                        this.setTile(x, y, planks);
                        this.setObject(x, y, door);
                        break;
                    case '.':
                        this.setTile(x, y, planks);
                        break;
                    default:
                        break; // untouched
                }
            }
        }

        // ===== The one lit corner =====
        // A ghost lantern each side of the door, and one deeper in. The Veil
        // has no daylight, so these three are the only reason the interior is
        // legible at all from the doorway.
        this.setObject(5, 10, SkyRegistry.ghostLanternID);
        this.setObject(9, 10, SkyRegistry.ghostLanternID);
        this.setObject(7, 2, SkyRegistry.ghostLanternID);

        // ===== The east room: whoever lived here kept watch =====
        this.setObject(12, 6, SkyRegistry.gloomRavenStatueID);
        this.setObject(12, 7, SkyRegistry.skywatchRubbleID);

        // ===== The main room =====
        this.setObject(3, 4, SkyRegistry.gloomRavenStatueID);
        this.setObject(10, 4, SkyRegistry.skywatchRubbleID);
        this.setObject(4, 8, SkyRegistry.skywatchRubbleID);
        this.setObject(3, 1, SkyRegistry.skywatchRubbleID);
        this.setObject(9, 1, SkyRegistry.veilrockID);

        // ===== What is left of the pantry =====
        // A vanilla barrel, because the loot has to live in something the
        // engine already knows how to open, and Preset.addInventory (line 1674)
        // fills whatever container object entity stands on the tile.
        this.setObject(2, 6, necesse.engine.registries.ObjectRegistry.getObjectID("barrel"));
        this.addInventory(new LootTable(
                LootItem.between("veilessence", 2, 6),
                LootItem.between("cinderpearl", 1, 3),
                LootItem.between("charwood", 5, 14)
        ), random, 2, 6, new Object[0]);
    }

}
