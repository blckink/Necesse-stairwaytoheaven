package stairwaytoheaven.realms.crooked;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;

/**
 * The Long Table — {@code WORLD_DESIGN.md} §13's <i>"absurdly long chairs"</i>
 * given somewhere to be absurd, and the richest place in the realm.
 *
 * <p>A hall with no roof and no walls: twenty-one paces of chequered floor
 * running between a door at each end, chairs down both sides of a table that is
 * only a table because the chairs say so, clocks along it that do not agree,
 * lanterns overhead, and the barrel at the far end where the host would sit.
 * It is the room the Architect (§16) would receive you in, which is exactly why
 * it is here and he is not — the arena needs to exist before the fight does.
 *
 * <p>Its shape is deliberately a CORRIDOR rather than a yard. The other two POIs
 * are places you walk around; this is a place that makes you walk down it, past
 * every guard in the pack, to reach the thing at the end. That is the guarded
 * shape A4.1 asks for, expressed in geometry rather than in numbers.
 *
 * <h2>Legend</h2>
 * <pre>
 *   space  not written at all
 *   .      chequered paving       ,  gloomwood planks (the runner down the middle)
 *   D      a Beetlefreak door, shut, at each end
 *   H      long chair             C  crooked clock
 *   L      bent lantern           W  window lying in the ground
 *   B      the barrel
 * </pre>
 */
public class LongTablePreset extends Preset {

    public static final int WIDTH = 23;
    public static final int HEIGHT = 9;

    /** The hall, drawn. Every row is exactly {@link #WIDTH} characters. */
    public static final String[] PLAN = {
            "   ...............     ",
            "  ..L.....C.....L..    ",
            " .HHHHHHHHHHHHHHHHH.   ",
            "D,,,,,,,,,,,,,,,,,,,,B ",
            " .HHHHHHHHHHHHHHHHH.   ",
            "  ..L..W..C..W..L..    ",
            "   ...............     ",
            "    .............      ",
            "     ....D....         ",
    };

    public LongTablePreset(GameRandom random) {
        super(WIDTH, HEIGHT);

        final int paving = SkyRegistry.marbleCheckerID;
        final int planks = SkyRegistry.gloomwoodFloorID;
        final int door = SkyRegistry.beetleDoorClosedID;

        for (int y = 0; y < PLAN.length; y++) {
            String row = PLAN[y];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c == ' ') {
                    continue;
                }
                // The runner down the middle is planks; everything else is the
                // chequerboard. A hall you can see the length of is the point.
                this.setTile(x, y, c == ',' || c == 'D' || c == 'B' ? planks : paving);
                switch (c) {
                    case 'D':
                        this.setObject(x, y, door);
                        break;
                    case 'H':
                        this.setObject(x, y, CrookedRealm.longChairID);
                        break;
                    case 'C':
                        this.setObject(x, y, CrookedRealm.crookedClockID);
                        break;
                    case 'L':
                        this.setObject(x, y, CrookedRealm.bentLanternID);
                        break;
                    case 'W':
                        this.setObject(x, y, CrookedRealm.groundWindowID);
                        break;
                    case 'B':
                        this.setObject(x, y, ObjectRegistry.getObjectID("barrel"));
                        break;
                    default:
                        break; // '.' and ',' are bare floor
                }
            }
        }

        this.addInventory(LOOT, random, 21, 3, new Object[0]);
    }

    /**
     * What is at the head of the table.
     *
     * <p>The best table in the realm, and the only one that hands out Reality
     * Shards in a quantity worth planning around — which is what makes the walk
     * past the pack a decision rather than a chore. Amounts carry the realm's
     * drop value ({@code CROOKED_DROP_VALUE} = 2.5); see
     * {@link DoorYardPreset#LOOT} for why the multiplier lives in the table.
     */
    public static final LootTable LOOT = new LootTable(
            LootItem.between("realityshard", 4, 9),
            LootItem.between("strangefabric", 8, 16),
            LootItem.between("warpresin", 5, 12),
            ChanceLootItem.between(0.60F, "eyeseed", 4, 8),
            ChanceLootItem.between(0.50F, "oddwood", 8, 16),
            ChanceLootItem.between(0.30F, "stripedshell", 2, 4));
}
