package stairwaytoheaven.realms.ghost;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;

/**
 * The Mausoleum — the Aftergarden's common tomb, and the smallest of its three
 * POIs.
 *
 * <h2>Drawn as a character map</h2>
 * Same technique {@code CrookedHousePreset} uses and for the same reason: a
 * building whose walls step in and out is unreadable as a list of coordinates
 * and one typo away from a hole. Written as a map, the silhouette is visible in
 * the source. Legend:
 *
 * <pre>
 *   space  not written at all (the realm's own ground shows through)
 *   #      crypt wall
 *   D      crypt door
 *   .      interior: black cobble
 *   ,      apron: spirit stone, outside the walls
 * </pre>
 *
 * <h2>Every object in it is the game's own</h2>
 * The Aftergarden ships without new art, so the tomb is built from vanilla
 * objects resolved by string ID at construction time —
 * {@code cryptwall}, {@code cryptdoor}, {@code cryptcoffin}, {@code cryptcolumn},
 * {@code cryptgravestone1/2}, {@code candle}, {@code vases}, {@code bonechest}.
 * That is not a compromise: a crypt built out of the game's own crypt is
 * exactly what a graveyard realm should look like, and it costs nothing to
 * replace later. Resolving by ID at construction rather than in a static
 * initialiser matters — a preset is built when a region generates, long after
 * every registry has closed, so the lookups cannot run too early.
 *
 * <h2>No multi-tile objects</h2>
 * Deliberately none. {@code Preset.applyToLevel} writes IDs straight into the
 * object layer and never runs {@code MultiTile.placeObject}, so a multi-tile
 * piece would need its second half written by hand and would break the moment
 * anyone edited the map. Everything here is single-tile.
 */
public class MausoleumPreset extends Preset {

    public static final int WIDTH = 11;
    public static final int HEIGHT = 11;

    /** The tomb, drawn. Every row is exactly {@link #WIDTH} characters. */
    public static final String[] PLAN = {
            ",,,,,,,,,,,",
            ",,#######,,",
            ",,#.....#,,",
            ",,#.....#,,",
            "###.....###",
            "#.........#",
            "#.........#",
            "###.....###",
            ",,#.....#,,",
            ",,###D###,,",
            ",,,,,,,,,,,",
    };

    public MausoleumPreset(GameRandom random) {
        super(WIDTH, HEIGHT);

        final int wall = ObjectRegistry.getObjectID("cryptwall");
        final int door = ObjectRegistry.getObjectID("cryptdoor");
        final int floor = GhostRealm.blackCobbleID;
        final int apron = GhostRealm.spiritStoneID;
        final int coffin = ObjectRegistry.getObjectID("cryptcoffin");
        final int column = ObjectRegistry.getObjectID("cryptcolumn");
        final int candle = ObjectRegistry.getObjectID("candle");
        final int urn = ObjectRegistry.getObjectID("vases");
        final int chest = ObjectRegistry.getObjectID("bonechest");
        final int gravestone = ObjectRegistry.getObjectID("cryptgravestone1");

        for (int y = 0; y < PLAN.length; y++) {
            String row = PLAN[y];
            for (int x = 0; x < row.length(); x++) {
                switch (row.charAt(x)) {
                    case '#':
                        this.setTile(x, y, floor);
                        this.setObject(x, y, wall);
                        break;
                    case 'D':
                        // The threshold gets floor too, or the doorway reads as
                        // a gap in the ground when you are standing in it.
                        this.setTile(x, y, floor);
                        this.setObject(x, y, door);
                        break;
                    case '.':
                        this.setTile(x, y, floor);
                        break;
                    case ',':
                        this.setTile(x, y, apron);
                        break;
                    default:
                        break;
                }
            }
        }

        // ===== Inside: four columns, the sarcophagus, and light =====
        this.setObject(3, 4, column);
        this.setObject(7, 4, column);
        this.setObject(3, 7, column);
        this.setObject(7, 7, column);
        // The occupant, dead centre and lying across the room.
        this.setObject(5, 5, coffin);
        this.setObject(4, 8, candle);
        this.setObject(6, 8, candle);
        this.setObject(1, 5, candle);
        this.setObject(9, 6, candle);
        // Two urns in the corners the columns leave.
        this.setObject(2, 3, urn);
        this.setObject(8, 3, urn);

        // ===== What the tomb was actually built to hold =====
        // A vanilla bone chest, because the loot has to live in something the
        // engine already knows how to open: Preset.addInventory fills whatever
        // container object entity stands on the tile.
        this.setObject(5, 2, chest);
        this.addInventory(new LootTable(
                LootItem.between("ectoplasm", 6, 14),
                LootItem.between("bonewood", 5, 12),
                ChanceLootItem.between(0.60F, "soulthread", 3, 8),
                ChanceLootItem.between(0.45F, "spectralore", 3, 7),
                ChanceLootItem.between(0.25F, "spiritsteelbar", 1, 3),
                ChanceLootItem.between(0.20F, "bone", 5, 12)
        ), random, 5, 2, new Object[0]);

        // ===== Outside: the family that could not afford a tomb of their own =====
        this.setObject(1, 1, gravestone);
        this.setObject(9, 1, gravestone);
        this.setObject(1, 9, gravestone);
        this.setObject(9, 9, gravestone);
    }
}
