package stairwaytoheaven.realms.crooked;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;

/**
 * The Door Yard — {@code WORLD_DESIGN.md} §13's <i>"doors without a house"</i>,
 * built as a place rather than as a prop.
 *
 * <p>Eleven Beetlefreak doors standing in rows on a chequered forecourt, each
 * one shut, none of them attached to anything. A player who opens one walks
 * through and is exactly where they were. That is the entire joke and it is the
 * cheapest true statement this realm can make about itself.
 *
 * <h2>Why a character map</h2>
 * Same reason {@link stairwaytoheaven.worldgen.CrookedHousePreset} uses one: the
 * layout's whole point is that it is not symmetric, and written as coordinates
 * that is unreadable and one typo away from a door in the wrong row. Written as
 * a map you can see the silhouette in the source.
 *
 * <h2>Legend</h2>
 * <pre>
 *   space  not written at all (the realm's own ground shows through)
 *   .      chequered paving (marble checker, the mod's own floor)
 *   D      a Beetlefreak door, shut, standing free
 *   L      a bent lantern
 *   C      a crooked clock
 *   W      a window lying in the ground
 *   B      the barrel (the reason to come)
 * </pre>
 *
 * <h2>Two engine facts this plan depends on</h2>
 * <ul>
 * <li><b>A free-standing door is legal.</b> Neither {@code DoorObject} nor
 *     {@code WallDoorObject} overrides {@code isValid} (VERIFIED [jar] — the
 *     method does not appear in either file), so a door with no wall beside it
 *     is not swept away on validation. Its sibling <b>window</b> is not: <b>
 *     {@code WallWindowObject.isValid} rejects itself outright</b> unless its
 *     connected walls are exactly one opposite pair, which is why there is not a
 *     single {@code beetlewindow} in this yard and the "windows in the ground"
 *     image is carried by the {@code groundwindow} prop instead. The Crooked
 *     House shipped with 1 of its 3 windows before somebody measured that.</li>
 * <li><b>No multi-tile objects.</b> {@code Preset.applyToLevel} writes IDs
 *     straight into the object layer and never runs {@code MultiTile.placeObject}
 *     (VERIFIED [jar]), so every multi-tile piece would need its second half
 *     written by hand. Everything here is single-tile, which removes that whole
 *     class of mistake.</li>
 * </ul>
 */
public class DoorYardPreset extends Preset {

    public static final int WIDTH = 17;
    public static final int HEIGHT = 13;

    /** The yard, drawn. Every row is exactly {@link #WIDTH} characters. */
    public static final String[] PLAN = {
            "  ...........    ",
            " .............   ",
            " ..D...D...D..   ",
            "..............   ",
            "..L.......C.L..  ",
            "..............W  ",
            "..D....B....D..  ",
            "..............   ",
            "..L...W....L...  ",
            "..............   ",
            " ..D...D...D..   ",
            " ...........     ",
            "  .........      ",
    };

    public DoorYardPreset(GameRandom random) {
        super(WIDTH, HEIGHT);

        final int paving = SkyRegistry.marbleCheckerID;
        final int door = SkyRegistry.beetleDoorClosedID;

        for (int y = 0; y < PLAN.length; y++) {
            String row = PLAN[y];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c == ' ') {
                    continue;
                }
                // Everything that is written at all stands on paving: the
                // chequerboard IS the yard, and a door with no floor under it
                // reads as a door that fell over rather than one left standing.
                this.setTile(x, y, paving);
                switch (c) {
                    case 'D':
                        this.setObject(x, y, door);
                        break;
                    case 'L':
                        this.setObject(x, y, CrookedRealm.bentLanternID);
                        break;
                    case 'C':
                        this.setObject(x, y, CrookedRealm.crookedClockID);
                        break;
                    case 'W':
                        this.setObject(x, y, CrookedRealm.groundWindowID);
                        break;
                    case 'B':
                        // A vanilla barrel, because the loot has to live in
                        // something the engine already knows how to open and
                        // Preset.addInventory fills whatever container object
                        // entity stands on the tile.
                        this.setObject(x, y, ObjectRegistry.getObjectID("barrel"));
                        break;
                    default:
                        break;
                }
            }
        }

        this.addInventory(LOOT, random, 7, 6, new Object[0]);
    }

    /**
     * What is behind eleven doors that go nowhere.
     *
     * <p>Weighted to Strange Fabric and Reality Shard, i.e. the BUILT half of
     * the realm's economy, because this is a made place. Amounts already carry
     * the realm's drop value ({@code SkyMobTiers.CROOKED_DROP_VALUE} = 2.5): a
     * preset barrel sits in an ordinary level with no {@code LevelModifiers.LOOT}
     * on it, so the multiplier has to be in the table.
     */
    public static final LootTable LOOT = new LootTable(
            LootItem.between("strangefabric", 6, 14),
            LootItem.between("realityshard", 2, 5),
            ChanceLootItem.between(0.50F, "warpresin", 4, 10),
            ChanceLootItem.between(0.35F, "eyeseed", 2, 5),
            ChanceLootItem.between(0.30F, "oddwood", 5, 12));
}
