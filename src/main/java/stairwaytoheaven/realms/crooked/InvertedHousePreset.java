package stairwaytoheaven.realms.crooked;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;

/**
 * The Inside-Out House — a house whose rooms are on the outside.
 *
 * <p>{@code WORLD_DESIGN.md} A3.6 asks for a realm where <i>"the rules of the
 * world decay"</i> and lists <i>"windows lying on the floor"</i> as the kind of
 * image that sells it. This is that idea at building scale: a sealed block of
 * Beetlefreak masonry with a shut door and no way in stands in the middle, and
 * everything that ought to be inside it — the plank floor, the chairs, the
 * clock, the lit lantern, the pantry — is laid out around it in the open, in
 * the shape of the rooms it should have had.
 *
 * <p>It is deliberately the one POI here with a real interior, so that the
 * interior can be the joke: you can see the house, you can open its door, and
 * behind the door is more house.
 *
 * <h2>Legend</h2>
 * <pre>
 *   space  not written at all (the realm's own ground shows through)
 *   #      beetlefreak wall
 *   D      the door (shut, and it opens onto masonry)
 *   ,      gloomwood planks — the floor of the rooms, which are outdoors
 *   L      bent lantern      C  crooked clock
 *   H      long chair        W  window lying in the ground
 *   B      the barrel
 * </pre>
 *
 * <h2>Sealing</h2>
 * The masonry block is SOLID: every cell of it is a wall, so there is no
 * interior cell for {@code tools/preset_seal_check.py}'s flood to reach and no
 * hole for it to find. That is on purpose — a hollow core would be a room the
 * player could break into and find empty, which turns the joke into a
 * disappointment. The plank cells outside it are floor, not interior, and are
 * meant to be walked on.
 */
public class InvertedHousePreset extends Preset {

    public static final int WIDTH = 15;
    public static final int HEIGHT = 13;

    /** The house, drawn. Every row is exactly {@link #WIDTH} characters. */
    public static final String[] PLAN = {
            "  ,,,,,,,,,,,  ",
            " ,,,,,,,,,,,,, ",
            " ,,L,,,,,,,C,, ",
            " ,,,,#####,,,, ",
            " ,,,,#####,,,, ",
            ",,H,,#####,,H, ",
            ",,,,,##D##,,,, ",
            ",,B,,,,,,,,,,, ",
            " ,,,,,,,,,,,W, ",
            " ,,H,,,,,,H,,, ",
            " ,,,,,,,,,,,,, ",
            "  ,,,,L,,,,,,  ",
            "   ,,,,,,,,,   ",
    };

    public InvertedHousePreset(GameRandom random) {
        super(WIDTH, HEIGHT);

        final int wall = SkyRegistry.beetleWallID;
        final int door = SkyRegistry.beetleDoorClosedID;
        final int planks = SkyRegistry.gloomwoodFloorID;

        for (int y = 0; y < PLAN.length; y++) {
            String row = PLAN[y];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c == ' ') {
                    continue;
                }
                // Planks under everything, including under the masonry: a house
                // built ON its own floor is what makes "the rooms are outside"
                // read as a mistake rather than as a courtyard.
                this.setTile(x, y, planks);
                switch (c) {
                    case '#':
                        this.setObject(x, y, wall);
                        break;
                    case 'D':
                        this.setObject(x, y, door);
                        break;
                    case 'L':
                        this.setObject(x, y, CrookedRealm.bentLanternID);
                        break;
                    case 'C':
                        this.setObject(x, y, CrookedRealm.crookedClockID);
                        break;
                    case 'H':
                        this.setObject(x, y, CrookedRealm.longChairID);
                        break;
                    case 'W':
                        this.setObject(x, y, CrookedRealm.groundWindowID);
                        break;
                    case 'B':
                        this.setObject(x, y, ObjectRegistry.getObjectID("barrel"));
                        break;
                    default:
                        break; // ',' is bare floor
                }
            }
        }

        this.addInventory(LOOT, random, 2, 7, new Object[0]);
    }

    /**
     * The pantry of a house with no inside.
     *
     * <p>Weighted toward what GREW rather than what was built — Oddwood, Warp
     * Resin, Eye Seed — because this is the one POI that reads as somewhere
     * people lived rather than somewhere something was made. Amounts carry the
     * realm's drop value ({@code CROOKED_DROP_VALUE} = 2.5) for the reason
     * {@link DoorYardPreset#LOOT} records.
     */
    public static final LootTable LOOT = new LootTable(
            LootItem.between("oddwood", 8, 18),
            LootItem.between("warpresin", 4, 10),
            ChanceLootItem.between(0.55F, "eyeseed", 3, 7),
            ChanceLootItem.between(0.40F, "strangefabric", 3, 8),
            ChanceLootItem.between(0.22F, "realityshard", 1, 3));
}
