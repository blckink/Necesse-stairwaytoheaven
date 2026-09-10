package stairwaytoheaven.worldgen.pois;

import java.awt.Rectangle;

import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyCloudmarbleSet;
import stairwaytoheaven.SkyFurnitureSet;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.realms.crooked.CrookedRealm;
import stairwaytoheaven.realms.eden.EdenRealm;
import stairwaytoheaven.realms.ghost.GhostRealm;

/**
 * The authored POI catalogue for the single Skyreach plane.
 *
 * <p>These are deliberately code-built presets. They still use Necesse's real
 * wall, door, window, chair, table, bed, storage, crafting and decoration
 * objects; the code form makes the rules visible and reviewable: roads are
 * reserved first, buildings sit beside them, every entrance receives a path,
 * and the furniture never closes the one-tile circulation spine.
 */
public final class RealmPoiPresets {
    public static final int SKY_TOWER = 0;
    public static final int SKY_TOWN = 1;
    public static final int SKY_TOLL_BRIDGE = 2;
    public static final int SKY_INN = 3;
    public static final int EDEN_CROWN_GARDEN = 4;
    public static final int EDEN_FERMENT_HOUSE = 5;
    public static final int STEINFELD_MEMORIAL = 6;
    public static final int GHOST_ARCHIVE = 7;
    public static final int CROOKED_BAZAAR = 8;
    public static final int HELL_BORDER_OFFICE = 9;
    public static final int HELL_ADMINISTRATION = 10;
    public static final int HELL_FORGE = 11;
    public static final int HELL_CARNIVAL = 12;
    /**
     * POI 2.12 of {@code docs/design/chapter-01-skyreach-pois.md}, the first of
     * that dossier's fourteen to be built.
     *
     * <p>APPENDED, not inserted between the Skyreach kinds it belongs with:
     * {@link RealmPoiWorldPreset#survey} rotates through {@link
     * RealmPoiWorldPreset#REALM_KINDS} by ordinal, so renumbering the existing
     * twelve would move every already-generated region's kind under its own
     * queued rectangle. Realm membership comes from {@link #realm} instead of
     * from the ordinal ranges it used to read.
     */
    public static final int SKY_TOLL_HOUSE = 13;
    /**
     * POI 2.1 of the dossier, and the first place built by {@link #plan} rather
     * than typed out in {@code setObject} calls. APPENDED, for the reason
     * {@link #SKY_TOLL_HOUSE} states.
     */
    public static final int SKY_WAYSIDE_SHRINE = 14;
    /** POI 2.11 of the dossier, the second {@link #plan}-built place. */
    public static final int SKY_DEW_KEEPERS_HUT = 15;
    /** POI 2.2 of the dossier: the cottage and the pasture around it. */
    public static final int SKY_SHEPHERDS_FOLD = 16;
    /** POI 2.3 of the dossier: the launch ramp, the falling arc, the crater. */
    public static final int SKY_FALLING_INSTITUTE = 17;
    /** POI 2.7 of the dossier: the inn on the Skyway. */
    public static final int SKY_PASSAGE_WAYHOUSE = 18;
    public static final int COUNT = 19;

    private static final int UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3;
    /** Wall-decor rotation: where the WALL is, not where the piece faces (§0.2). */
    private static final int WALL_BELOW = 0, WALL_LEFT = 1, WALL_ABOVE = 2, WALL_RIGHT = 3;

    private RealmPoiPresets() {
    }

    public static int width(int kind) {
        switch (kind) {
            case SKY_TOWER: return 49;
            case SKY_TOWN: return 57;
            case SKY_TOLL_BRIDGE: return 31;
            case SKY_INN: return 17;
            case EDEN_CROWN_GARDEN: return 45;
            case EDEN_FERMENT_HOUSE: return 19;
            case STEINFELD_MEMORIAL: return 23;
            case GHOST_ARCHIVE: return 25;
            case CROOKED_BAZAAR: return 27;
            case HELL_BORDER_OFFICE: return 23;
            case HELL_ADMINISTRATION: return 61;
            case HELL_FORGE: return 29;
            case HELL_CARNIVAL: return 39;
            case SKY_TOLL_HOUSE: return 23;
            // Read OFF the plan, never declared beside it: a mistyped row would
            // otherwise shift the whole building against its own footprint.
            case SKY_WAYSIDE_SHRINE: return WAYSIDE_PLAN[0].length();
            case SKY_DEW_KEEPERS_HUT: return HUT_PLAN[0].length();
            case SKY_SHEPHERDS_FOLD: return FOLD_PLAN[0].length();
            case SKY_FALLING_INSTITUTE: return INSTITUTE_PLAN[0].length();
            case SKY_PASSAGE_WAYHOUSE: return WAYHOUSE_PLAN[0].length();
            default: throw new IllegalArgumentException("Unknown realm POI " + kind);
        }
    }

    public static int height(int kind) {
        switch (kind) {
            case SKY_TOWER: return 55;
            case SKY_TOWN: return 41;
            case SKY_TOLL_BRIDGE: return 23;
            case SKY_INN: return 15;
            case EDEN_CROWN_GARDEN: return 35;
            case EDEN_FERMENT_HOUSE: return 17;
            case STEINFELD_MEMORIAL: return 23;
            case GHOST_ARCHIVE: return 21;
            case CROOKED_BAZAAR: return 21;
            case HELL_BORDER_OFFICE: return 19;
            case HELL_ADMINISTRATION: return 45;
            case HELL_FORGE: return 23;
            case HELL_CARNIVAL: return 31;
            case SKY_TOLL_HOUSE: return 19;
            case SKY_WAYSIDE_SHRINE: return WAYSIDE_PLAN.length;
            case SKY_DEW_KEEPERS_HUT: return HUT_PLAN.length;
            case SKY_SHEPHERDS_FOLD: return FOLD_PLAN.length;
            case SKY_FALLING_INSTITUTE: return INSTITUTE_PLAN.length;
            case SKY_PASSAGE_WAYHOUSE: return WAYHOUSE_PLAN.length;
            default: throw new IllegalArgumentException("Unknown realm POI " + kind);
        }
    }

    /**
     * Stable lowercase key per kind.
     *
     * <p>The census and the preset debug names share it, so a queued rectangle
     * in the world can be traced back to the row of the catalogue that asked
     * for it. Ordinals are not enough for that: the debug name is a string the
     * engine hands back, and a number in it reads as an index into whatever the
     * reader assumes. Append rather than rename — scripts grep these.
     */
    public static String key(int kind) {
        switch (kind) {
            case SKY_TOWER: return "skytower";
            case SKY_TOWN: return "skytown";
            case SKY_TOLL_BRIDGE: return "skytollbridge";
            case SKY_INN: return "skyinn";
            case EDEN_CROWN_GARDEN: return "edencrowngarden";
            case EDEN_FERMENT_HOUSE: return "edenfermenthouse";
            case STEINFELD_MEMORIAL: return "steinfeldmemorial";
            case GHOST_ARCHIVE: return "ghostarchive";
            case CROOKED_BAZAAR: return "crookedbazaar";
            case HELL_BORDER_OFFICE: return "hellborderoffice";
            case HELL_ADMINISTRATION: return "helladministration";
            case HELL_FORGE: return "hellforge";
            case HELL_CARNIVAL: return "hellcarnival";
            case SKY_TOLL_HOUSE: return "skywaytollhouse";
            case SKY_WAYSIDE_SHRINE: return "waysideshrine";
            case SKY_DEW_KEEPERS_HUT: return "dewkeepershut";
            case SKY_SHEPHERDS_FOLD: return "shepherdsfold";
            case SKY_FALLING_INSTITUTE: return "fallinginstitute";
            case SKY_PASSAGE_WAYHOUSE: return "passagewayhouse";
            default: throw new IllegalArgumentException("Unknown realm POI " + kind);
        }
    }

    /**
     * Which realm band a kind belongs to.
     *
     * <p>Written as an explicit switch rather than the ordinal ranges it used
     * to be ({@code kind <= SKY_INN} and friends). Those ranges were only ever
     * true while the catalogue happened to be sorted by realm, and the first
     * kind appended past {@code HELL_CARNIVAL} silently became a Hell POI.
     * A new kind that forgets this switch now fails loudly at load, in
     * {@link RealmPoiWorldPreset#onRegistryClosed}, instead of generating in
     * the wrong half of the world.
     */
    public static int realm(int kind) {
        switch (kind) {
            case SKY_TOWER: case SKY_TOWN: case SKY_TOLL_BRIDGE: case SKY_INN:
            case SKY_TOLL_HOUSE: case SKY_WAYSIDE_SHRINE: case SKY_DEW_KEEPERS_HUT:
            case SKY_SHEPHERDS_FOLD: case SKY_FALLING_INSTITUTE:
            case SKY_PASSAGE_WAYHOUSE:
                return 0;
            case EDEN_CROWN_GARDEN: case EDEN_FERMENT_HOUSE: return 1;
            case STEINFELD_MEMORIAL: return 2;
            case GHOST_ARCHIVE: return 3;
            case CROOKED_BAZAAR: return 4;
            case HELL_BORDER_OFFICE: case HELL_ADMINISTRATION:
            case HELL_FORGE: case HELL_CARNIVAL:
                return 5;
            default: throw new IllegalArgumentException("Unknown realm POI " + kind);
        }
    }

    public static Preset build(int kind, GameRandom random) {
        switch (kind) {
            case SKY_TOWER: return skyTower();
            case SKY_TOWN: return skyTown();
            case SKY_TOLL_BRIDGE: return tollBridge();
            case SKY_INN: return skyInn();
            case EDEN_CROWN_GARDEN: return crownGarden();
            case EDEN_FERMENT_HOUSE: return fermentHouse();
            case STEINFELD_MEMORIAL: return memorial();
            case GHOST_ARCHIVE: return ghostArchive();
            case CROOKED_BAZAAR: return crookedBazaar();
            case HELL_BORDER_OFFICE: return borderOffice();
            case HELL_ADMINISTRATION: return hellAdministration();
            case HELL_FORGE: return hellForge();
            case HELL_CARNIVAL: return hellCarnival();
            case SKY_TOLL_HOUSE: return skywayTollHouse();
            case SKY_WAYSIDE_SHRINE: return waysideShrine();
            case SKY_DEW_KEEPERS_HUT: return dewKeepersHut();
            case SKY_SHEPHERDS_FOLD: return shepherdsFold();
            case SKY_FALLING_INSTITUTE: return fallingInstitute();
            case SKY_PASSAGE_WAYHOUSE: return passageWayhouse();
            default: throw new IllegalArgumentException("Unknown realm POI " + kind);
        }
    }

    private static Preset blank(int kind) {
        Preset p = new Preset(width(kind), height(kind));
        for (int x = 0; x < p.width; x++) {
            for (int y = 0; y < p.height; y++) {
                p.setObject(x, y, 0);
                p.setObjectLayer(ObjectLayerRegistry.TILE_LAYER, x, y, 0);
                p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, x, y, 0);
                p.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, x, y, 0);
            }
        }
        return p;
    }

    private static int object(String id) {
        int value = ObjectRegistry.getObjectID(id);
        if (value < 0) throw new IllegalStateException("Missing POI object: " + id);
        return value;
    }

    private static int tile(String id) {
        int value = TileRegistry.getTileID(id);
        if (value < 0) throw new IllegalStateException("Missing POI tile: " + id);
        return value;
    }

    private static void road(Preset p, int x, int y, int w, int h, int floor) {
        p.fillTile(x, y, w, h, floor);
        p.fillObject(x, y, w, h, 0);
    }

    /** Builds the boundary of a union of rectangles, producing real L/T/U footprints. */
    private static void building(Preset p, int floor, int wall, Rectangle... rooms) {
        boolean[][] inside = new boolean[p.width][p.height];
        for (Rectangle r : rooms) {
            for (int x = r.x; x < r.x + r.width; x++) {
                for (int y = r.y; y < r.y + r.height; y++) {
                    inside[x][y] = true;
                    p.setTile(x, y, floor);
                    p.setObject(x, y, 0);
                }
            }
        }
        for (int x = 0; x < p.width; x++) {
            for (int y = 0; y < p.height; y++) {
                if (!inside[x][y]) continue;
                if (x == 0 || y == 0 || x == p.width - 1 || y == p.height - 1
                        || !inside[x - 1][y] || !inside[x + 1][y]
                        || !inside[x][y - 1] || !inside[x][y + 1]) {
                    p.setObject(x, y, wall);
                }
            }
        }
    }

    private static void door(Preset p, int x, int y, int id) {
        p.setObject(x, y, id);
    }

    private static void windows(Preset p, int id, int[][] positions) {
        for (int[] at : positions) p.setObject(at[0], at[1], id);
    }

    private static void tableForFour(Preset p, int x, int y, int table, int chair) {
        p.setObject(x, y, table);
        p.setObject(x - 1, y, chair, RIGHT);
        p.setObject(x + 1, y, chair, LEFT);
        p.setObject(x, y - 1, chair, DOWN);
        p.setObject(x, y + 1, chair, UP);
    }

    private static void bed(Preset p, int x, int y, String id, int rotation) {
        int master = object(id);
        int counter = object(id + "2");
        p.setObject(x, y, master, rotation);
        int dx = rotation == RIGHT ? 1 : rotation == LEFT ? -1 : 0;
        int dy = rotation == DOWN ? 1 : rotation == UP ? -1 : 0;
        p.setObject(x + dx, y + dy, counter, rotation);
    }

    private static void furnishHome(Preset p, int x, int y, String family) {
        int table = object(family + "modulartable");
        int chair = object(family + "chair");
        tableForFour(p, x + 2, y + 2, table, chair);
        bed(p, x + 5, y + 2, family + "bed", DOWN);
        p.setObject(x + 6, y + 5, object(family + "dresser"));
        p.setObject(x + 2, y + 5, object(family + "candelabra"));
    }

    private static Preset skyTower() {
        Preset p = blank(SKY_TOWER);
        int path = SkyRegistry.skyroadTileID;
        int floor = SkyCloudmarbleSet.skywayTileID;
        int wall = SkyCloudmarbleSet.cloudmarbleWallID;
        int door = SkyCloudmarbleSet.cloudmarbleDoorID;
        int window = SkyCloudmarbleSet.cloudmarbleWindowID;
        road(p, 23, 39, 3, 16, path);
        road(p, 7, 40, 35, 3, path);
        // Stepped/arched silhouette: wide transept below a narrowing nave.
        building(p, floor, wall,
                new Rectangle(5, 25, 39, 17), new Rectangle(12, 14, 25, 13),
                new Rectangle(17, 7, 15, 9), new Rectangle(21, 3, 7, 6));
        door(p, 24, 41, door);
        door(p, 24, 25, door);
        door(p, 24, 14, door);
        door(p, 24, 7, door);
        // (16,25) and (32,25) used to be in this row and were never in the
        // world: y=25 is the transept's north wall only OUTSIDE the nave's own
        // rectangle (x 12..36), so those two sat on interior floor with no wall
        // beside them, WallWindowObject.getWindowDir returned -1 and the engine
        // deleted both -- exactly the failure chapter-01-skyreach-pois.md §0.3
        // is written about. Moved to the transept's south wall, mid-run.
        // `realmpoi kind ... badwindows=` now counts this for every kind.
        windows(p, window, new int[][]{{10,25},{38,25},{16,41},{32,41},{12,19},{36,19},{17,11},{31,11},{21,5},{27,5}});
        // Central processional aisle is x=24 and remains clear.
        int chair = SkyFurnitureSet.skywatchChairID;
        int table = SkyFurnitureSet.skywatchTableID;
        for (int y : new int[]{30, 34, 38}) {
            p.setObject(18, y, table); p.setObject(17, y, chair, RIGHT); p.setObject(19, y, chair, LEFT);
            p.setObject(30, y, table); p.setObject(29, y, chair, RIGHT); p.setObject(31, y, chair, LEFT);
        }
        p.setObject(9, 29, SkyFurnitureSet.skywatchBookshelfID);
        p.setObject(9, 33, SkyFurnitureSet.skywatchCabinetID);
        p.setObject(39, 29, SkyFurnitureSet.skywatchDisplayID);
        p.setObject(39, 33, SkyFurnitureSet.skywatchClockID);
        p.setObject(19, 19, SkyFurnitureSet.skywatchDeskID, RIGHT);
        p.setObject(20, 19, chair, LEFT);
        bed(p, 29, 18, "skywatchbed", DOWN);
        for (int[] at : new int[][]{{8,39},{40,39},{14,23},{34,23},{20,9},{28,9},{22,4},{26,4}}) {
            p.setObject(at[0], at[1], SkyFurnitureSet.skywatchCandelabraID);
        }
        p.setObject(24, 4, SkyCloudmarbleSet.seraphStatueID);
        return p;
    }

    private static Preset skyTown() {
        Preset p = blank(SKY_TOWN);
        int road = SkyRegistry.skyroadTileID;
        int floor = SkyRegistry.gloomwoodFloorID;
        int wall = SkyCloudmarbleSet.cloudmarbleWallID;
        int door = SkyCloudmarbleSet.cloudmarbleDoorID;
        int window = SkyCloudmarbleSet.cloudmarbleWindowID;
        road(p, 0, 19, 57, 3, road);
        road(p, 27, 0, 3, 41, road);
        road(p, 22, 14, 13, 13, road); // plaza
        // Pond and bench are beside the road, never on it.
        p.fillTile(4, 5, 9, 6, SkyRegistry.mistseaID);
        p.setObject(14, 8, SkyFurnitureSet.skywatchBenchID, DOWN);
        p.setObject(14, 9, object("skywatchbench2"), DOWN);
        p.setObject(28, 20, SkyCloudmarbleSet.seraphStatueID);
        // Five occupied parcels, all at least two tiles away from a carriageway.
        building(p, floor, wall, new Rectangle(3, 25, 14, 11), new Rectangle(12, 32, 8, 6));
        building(p, floor, wall, new Rectangle(37, 25, 17, 12), new Rectangle(34, 30, 6, 7));
        building(p, floor, wall, new Rectangle(18, 3, 8, 11), new Rectangle(13, 3, 7, 7));
        building(p, floor, wall, new Rectangle(33, 3, 17, 12), new Rectangle(45, 12, 7, 5));
        door(p, 15, 25, door); door(p, 38, 25, door); door(p, 25, 12, door); door(p, 34, 13, door);
        road(p, 15, 22, 1, 3, road); road(p, 38, 22, 1, 3, road);
        road(p, 26, 12, 1, 8, road); road(p, 30, 13, 4, 1, road);
        windows(p, window, new int[][]{{7,25},{12,35},{44,25},{49,36},{18,3},{23,3},{38,3},{45,3}});
        furnishHome(p, 5, 27, "skywatch"); furnishHome(p, 40, 27, "skywatch");
        furnishHome(p, 15, 5, "skywatch"); furnishHome(p, 37, 5, "skywatch");
        return p;
    }

    private static Preset tollBridge() {
        Preset p = blank(SKY_TOLL_BRIDGE);
        int road = SkyRegistry.skyroadTileID;
        // A cloud stream links the east and west edges; the north/south road crosses it.
        p.fillTile(0, 9, 31, 5, SkyRegistry.mistseaID);
        road(p, 14, 0, 3, 23, road);
        int wall = SkyCloudmarbleSet.cloudmarbleWallID, door = SkyCloudmarbleSet.cloudmarbleDoorID;
        building(p, SkyRegistry.gloomwoodFloorID, wall, new Rectangle(3, 2, 9, 7));
        building(p, SkyRegistry.gloomwoodFloorID, wall, new Rectangle(19, 14, 9, 7));
        door(p, 11, 5, door); door(p, 19, 17, door);
        road(p, 12, 5, 2, 1, road); road(p, 17, 17, 2, 1, road);
        furnishHome(p, 4, 2, "skywatch"); furnishHome(p, 20, 14, "skywatch");
        return p;
    }

    /**
     * POI 2.12, the Skyway Toll-House, transcribed tile-for-tile from the plan
     * in {@code docs/design/chapter-01-skyreach-pois.md} §2.12.
     *
     * <p>Three rooms behind one shell: the weighing hall west of the x=12
     * partition, and east of it the ledger room (Magpie) above the y=8
     * partition and the vault (the Bonded Lockbox) below it. The two display
     * stands are the Writ and the Lockbox; the tome on Magpie's desk is the
     * Ledger of Undelivered Post. The mobs are placed by
     * {@link RealmPoiWorldPreset}, which is the only caller that has a level.
     *
     * <p>The dossier's {@code skywatchstele} on the apron is left out: it is
     * new art this build does not have, and {@link #object} would throw at
     * load rather than quietly drop it.
     */
    private static Preset skywayTollHouse() {
        Preset p = blank(SKY_TOLL_HOUSE);
        int floor = SkyRegistry.gloomwoodFloorID;
        int wall = SkyCloudmarbleSet.cloudmarbleWallID;
        int doorID = SkyCloudmarbleSet.cloudmarbleDoorID;
        int window = SkyCloudmarbleSet.cloudmarbleWindowID;

        // The apron the passage runs across, laid before the shell so the
        // building's own south wall keeps its tile.
        road(p, 1, 15, 21, 2, SkyCloudmarbleSet.skywayTileID);
        road(p, 0, 17, 23, 1, SkyCloudmarbleSet.skywayTileID);

        building(p, floor, wall, new Rectangle(2, 2, 19, 13));
        // Internal partitions: x=12 splits hall from east wing, y=8 splits the
        // east wing into ledger room above and vault below.
        for (int y = 3; y <= 13; y++) p.setObject(12, y, wall);
        for (int x = 13; x <= 19; x++) p.setObject(x, 8, wall);
        door(p, 7, 14, doorID);
        door(p, 12, 5, doorID);
        door(p, 12, 11, doorID);
        // Every one mid-run in a straight wall; a window in a corner is
        // silently deleted (§0.3).
        windows(p, window, new int[][]{{6, 2}, {16, 2}, {16, 14}, {2, 6}, {2, 11}, {20, 5}, {20, 11}});

        // The chequer weighbridge, accent scale: 15 tiles, never a whole room.
        p.fillTile(6, 6, 3, 5, SkyRegistry.marbleCheckerID);

        int chair = SkyFurnitureSet.skywatchChairID;
        int desk = SkyFurnitureSet.skywatchDeskID;
        int cabinet = SkyFurnitureSet.skywatchCabinetID;
        int display = SkyFurnitureSet.skywatchDisplayID;
        int candelabra = SkyFurnitureSet.skywatchCandelabraID;

        // Weighing hall.
        p.setObject(3, 4, desk, RIGHT);
        p.setObject(4, 4, chair, LEFT);
        p.setObject(3, 12, object("skywatchbench"), RIGHT);
        p.setObject(4, 12, object("skywatchbench2"), RIGHT);
        p.setObject(9, 9, object("skywatchrubble"));
        p.setObject(10, 4, candelabra);
        p.setObject(10, 12, candelabra);

        // Ledger room: Magpie's desk carries the Ledger of Undelivered Post,
        // the display stand the Skyway Writ.
        p.setObject(14, 4, desk, RIGHT);
        p.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, 14, 4, object("skywatchtome"));
        p.setObject(15, 4, chair, LEFT);
        p.setObject(17, 3, SkyFurnitureSet.skywatchBookshelfID, DOWN);
        p.setObject(18, 3, SkyFurnitureSet.skywatchBookshelfID, DOWN);
        p.setObject(19, 6, cabinet, LEFT);
        p.setObject(16, 6, display);
        p.setObject(14, 6, candelabra);

        // Vault: bonded cargo nobody ever came back for, and the Lockbox.
        p.setObject(14, 10, cabinet, DOWN);
        p.setObject(18, 10, cabinet, DOWN);
        p.setObject(14, 12, object("barrel"));
        p.setObject(18, 12, object("barrel"));
        p.setObject(16, 11, display);

        // Wall lights. Rotation is where the WALL is, and the decor's own tile
        // must be floor -- these all sit on the floor row beside the masonry.
        for (int[] at : new int[][]{{3, 3}, {10, 3}}) {
            p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, at[0], at[1], object("mistglasslantern"), DOWN);
        }
        for (int[] at : new int[][]{{3, 13}, {10, 13}, {16, 13}}) {
            p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, at[0], at[1], object("mistglasslantern"), UP);
        }
        p.setObject(5, 15, object("wardencandelabra"));
        p.setObject(17, 15, object("wardencandelabra"));
        return p;
    }

    private static Preset skyInn() {
        Preset p = blank(SKY_INN);
        int floor = SkyRegistry.gloomwoodFloorID, wall = SkyCloudmarbleSet.cloudmarbleWallID;
        int door = SkyCloudmarbleSet.cloudmarbleDoorID, window = SkyCloudmarbleSet.cloudmarbleWindowID;
        building(p, floor, wall, new Rectangle(1, 1, 15, 13), new Rectangle(11, 0, 5, 4));
        door(p, 8, 1, door); door(p, 8, 13, door);
        windows(p, window, new int[][]{{4,1},{12,1},{1,5},{15,5},{4,13},{12,13}});
        // x=8 is the straight north-south one-tile aisle between both doors.
        int table = SkyFurnitureSet.skywatchTableID, chair = SkyFurnitureSet.skywatchChairID;
        tableForFour(p, 4, 5, table, chair); tableForFour(p, 12, 5, table, chair);
        // Counter, kitchen and storage in the west rear; private room east rear.
        for (int x = 2; x <= 6; x++) p.setObject(x, 9, table);
        p.setObject(2, 11, stairwaytoheaven.settlement.SkyProfessions.stormglassKilnID, RIGHT);
        p.setObject(6, 11, SkyFurnitureSet.skywatchCabinetID);
        door(p, 11, 9, door);
        bed(p, 13, 10, "skywatchbed", DOWN);
        p.setObject(10, 11, SkyFurnitureSet.skywatchDresserID);
        return p;
    }

    private static Preset crownGarden() {
        Preset p = blank(EDEN_CROWN_GARDEN);
        int road = EdenRealm.edenRootFloorID;
        road(p, 0, 16, 45, 3, road); road(p, 21, 0, 3, 35, road);
        int wall = object("palmwall"), door = object("palmdoor"), window = object("palmwindow");
        building(p, EdenRealm.edenRootFloorID, wall, new Rectangle(3, 4, 14, 10), new Rectangle(12, 10, 7, 5));
        building(p, EdenRealm.edenRootFloorID, wall, new Rectangle(28, 22, 14, 10), new Rectangle(26, 27, 5, 6));
        door(p, 16, 13, door); door(p, 28, 27, door);
        road(p, 17, 13, 4, 1, road); road(p, 24, 27, 4, 1, road);
        windows(p, window, new int[][]{{6,4},{13,4},{31,31},{38,31}});
        furnishHome(p, 5, 6, "palm"); furnishHome(p, 31, 24, "palm");
        // A substantial field/clearing with a soft meadow edge, not a tiny patch.
        p.fillTile(3, 21, 15, 11, EdenRealm.edenSoilID);
        for (int x = 5; x <= 16; x += 3) for (int y = 23; y <= 30; y += 2) p.setObject(x, y, EdenRealm.serpentGrassID);
        p.setObject(36, 8, EdenRealm.edenSeedBasinID);
        return p;
    }

    private static Preset fermentHouse() {
        Preset p = blank(EDEN_FERMENT_HOUSE);
        int wall = object("palmwall"), door = object("palmdoor"), window = object("palmwindow");
        building(p, EdenRealm.edenRootFloorID, wall, new Rectangle(1, 2, 17, 13), new Rectangle(12, 1, 6, 5));
        door(p, 9, 14, door); door(p, 12, 7, door);
        windows(p, window, new int[][]{{4,2},{9,2},{15,2},{1,7},{17,10}});
        road(p, 9, 15, 1, 2, EdenRealm.edenRootFloorID);
        int table = object("palmmodulartable"), chair = object("palmchair");
        tableForFour(p, 5, 7, table, chair);
        for (int x : new int[]{13,15}) for (int y : new int[]{9,12}) p.setObject(x, y, object("barrel"));
        p.setObject(4, 12, EdenRealm.edenSeedBasinID);
        p.setObject(7, 12, object("palmcabinet"));
        return p;
    }

    private static Preset memorial() {
        Preset p = blank(STEINFELD_MEMORIAL);
        int floor = SkyRegistry.crackedmarbleID;
        road(p, 10, 0, 3, 23, floor); road(p, 0, 10, 23, 3, floor);
        p.fillTile(5, 5, 13, 13, floor);
        p.setObject(11, 11, SkyRegistry.mournerstatueID);
        for (int[] at : new int[][]{{5,5},{17,5},{5,17},{17,17}}) p.setObject(at[0], at[1], SkyRegistry.chapelcolumnID);
        for (int[] at : new int[][]{{7,7},{15,7},{7,15},{15,15}}) p.setObject(at[0], at[1], object("cryptgravestone1"));
        p.setObject(8, 11, object("stonecandlepedestal"));
        p.setObject(14, 11, object("stonecandlepedestal"));
        return p;
    }

    private static Preset ghostArchive() {
        Preset p = blank(GHOST_ARCHIVE);
        int wall = SkyRegistry.nightfellWallID, door = SkyRegistry.nightfellDoorID, window = SkyRegistry.beetleWindowID;
        building(p, GhostRealm.blackCobbleID, wall,
                new Rectangle(1, 3, 23, 16), new Rectangle(8, 1, 9, 4), new Rectangle(18, 8, 6, 10));
        door(p, 12, 18, door); door(p, 12, 3, door); door(p, 18, 11, door);
        windows(p, window, new int[][]{{5,3},{19,3},{1,8},{1,14},{23,7},{23,15}});
        road(p, 12, 19, 1, 2, GhostRealm.spiritStoneID);
        int shelf = object("bonebookshelf"), table = object("bonemodulartable"), chair = object("bonechair");
        for (int x : new int[]{4,7,17,20}) for (int y : new int[]{6,15}) p.setObject(x, y, shelf);
        tableForFour(p, 8, 10, table, chair); tableForFour(p, 16, 10, table, chair);
        p.setObject(12, 6, GhostRealm.soulBasinID);
        p.setObject(12, 14, object("bonechest"));
        for (int[] at : new int[][]{{3,5},{21,5},{3,17},{21,17}}) p.setObject(at[0], at[1], object("deadwoodcandelabra"));
        return p;
    }

    private static Preset crookedBazaar() {
        Preset p = blank(CROOKED_BAZAAR);
        int road = CrookedRealm.checkerStoneID;
        road(p, 0, 9, 27, 3, road);
        // Three actual stalls; their doors are deliberately separated, never a door heap.
        int wall = object("arcanicwall"), door = object("arcanicdoor"), window = object("arcanicwindow");
        building(p, CrookedRealm.crookedStripeID, wall, new Rectangle(2, 2, 7, 6));
        building(p, CrookedRealm.crookedStripeID, wall, new Rectangle(10, 13, 8, 6), new Rectangle(15, 16, 5, 4));
        building(p, CrookedRealm.crookedStripeID, wall, new Rectangle(19, 2, 6, 6));
        door(p, 5, 7, door); door(p, 13, 13, door); door(p, 22, 7, door);
        road(p, 5, 8, 1, 1, road); road(p, 13, 12, 1, 1, road); road(p, 22, 8, 1, 1, road);
        windows(p, window, new int[][]{{3,2},{7,2},{11,18},{18,18},{20,2},{24,2}});
        for (int[] at : new int[][]{{4,4},{6,4},{12,16},{16,16},{21,4},{23,4}}) p.setObject(at[0], at[1], CrookedRealm.crookedCrateID);
        p.setObject(9, 6, CrookedRealm.bentLanternID); p.setObject(18, 14, CrookedRealm.bentLanternID);
        return p;
    }

    private static Preset borderOffice() {
        Preset p = blank(HELL_BORDER_OFFICE);
        int floor = tile("factoryfloor"), wall = object("factorywall"), door = object("factorydoor"), window = object("factorywindow");
        road(p, 10, 0, 3, 19, tile("scrapfloor"));
        building(p, floor, wall, new Rectangle(2, 3, 19, 13), new Rectangle(16, 2, 5, 7));
        door(p, 11, 15, door); door(p, 11, 3, door); door(p, 16, 8, door);
        windows(p, window, new int[][]{{5,3},{17,3},{2,8},{20,12},{6,15},{17,15}});
        int table = object("oakmodulartable"), chair = object("oakchair");
        for (int y : new int[]{6,10,13}) { p.setObject(8, y, table); p.setObject(7, y, chair, RIGHT); p.setObject(9, y, chair, LEFT); }
        p.setObject(17, 11, object("demonchest"));
        p.setObject(4, 6, object("scraplamp")); p.setObject(18, 6, object("scraplamp"));
        return p;
    }

    private static Preset hellAdministration() {
        Preset p = blank(HELL_ADMINISTRATION);
        int road = tile("scrapfloor"), floor = tile("factoryfloor");
        int wall = object("factorywall"), door = object("factorydoor"), window = object("factorywindow");
        road(p, 29, 0, 3, 45, road); road(p, 0, 21, 61, 3, road);
        // Four dense wings around a public cross; none occupies the road.
        building(p, floor, wall, new Rectangle(4, 4, 21, 14), new Rectangle(18, 14, 8, 5));
        building(p, floor, wall, new Rectangle(35, 4, 22, 14), new Rectangle(34, 13, 8, 6));
        building(p, floor, wall, new Rectangle(4, 27, 22, 14), new Rectangle(18, 25, 8, 5));
        building(p, floor, wall, new Rectangle(35, 27, 22, 14), new Rectangle(34, 25, 8, 5));
        for (int[] at : new int[][]{{24,17},{36,17},{24,27},{36,27}}) door(p, at[0], at[1], door);
        for (int[] at : new int[][]{{24,18},{36,18},{24,24},{36,24}}) road(p, at[0], at[1], 1, 3, road);
        windows(p, window, new int[][]{{8,4},{14,4},{20,4},{40,4},{47,4},{53,4},{8,40},{14,40},{20,40},{40,40},{47,40},{53,40}});
        int table = object("oakmodulartable"), chair = object("oakchair"), shelf = object("oakbookshelf");
        for (int[] at : new int[][]{{10,9},{20,9},{41,9},{51,9},{10,34},{20,34},{41,34},{51,34}}) tableForFour(p, at[0], at[1], table, chair);
        for (int[] at : new int[][]{{6,6},{23,6},{37,6},{55,6},{6,38},{23,38},{37,38},{55,38}}) p.setObject(at[0], at[1], shelf);
        for (int[] at : new int[][]{{27,20},{33,20},{27,24},{33,24}}) p.setObject(at[0], at[1], object("scraplamp"));
        return p;
    }

    private static Preset hellForge() {
        Preset p = blank(HELL_FORGE);
        int road = tile("scrapfloor"), floor = tile("basaltfloor");
        int wall = object("basaltwall"), door = object("basaltdoor"), window = object("basaltwindow");
        road(p, 13, 0, 3, 23, road);
        building(p, floor, wall, new Rectangle(2, 4, 25, 15), new Rectangle(20, 2, 7, 7));
        door(p, 14, 18, door); door(p, 14, 4, door); door(p, 20, 8, door);
        windows(p, window, new int[][]{{6,4},{22,4},{2,9},{26,13},{7,18},{22,18}});
        for (int[] at : new int[][]{{5,8},{9,8},{5,13},{9,13}}) p.setObject(at[0], at[1], object("demonicanvil"));
        p.setObject(19, 12, object("demonicworkstation"));
        p.setObject(23, 13, object("fuelskullencasing"));
        p.setObject(18, 16, object("demonchest"));
        p.setObject(4, 16, object("scraplamp")); p.setObject(24, 16, object("scraplamp"));
        return p;
    }

    private static Preset hellCarnival() {
        Preset p = blank(HELL_CARNIVAL);
        int path = tile("junkfloor");
        road(p, 18, 0, 3, 31, path); road(p, 0, 14, 39, 3, path);
        // Central carousel ring and four side stalls leave the road cross open.
        int fence = object("jailfence");
        for (int x = 14; x <= 24; x++) { p.setObject(x, 9, fence); p.setObject(x, 21, fence); }
        for (int y = 10; y <= 20; y++) { p.setObject(14, y, fence); p.setObject(24, y, fence); }
        p.setObject(19, 9, 0); p.setObject(19, 21, 0); p.setObject(14, 15, 0); p.setObject(24, 15, 0);
        p.setObject(19, 15, object("chieftainsthrone"));
        int wall = object("factorywall"), door = object("factorydoor");
        building(p, tile("factoryfloor"), wall, new Rectangle(3, 3, 9, 7));
        building(p, tile("factoryfloor"), wall, new Rectangle(27, 3, 9, 7));
        building(p, tile("factoryfloor"), wall, new Rectangle(3, 21, 9, 7));
        building(p, tile("factoryfloor"), wall, new Rectangle(27, 21, 9, 7));
        door(p, 7, 9, door); door(p, 31, 9, door); door(p, 7, 21, door); door(p, 31, 21, door);
        for (int[] at : new int[][]{{6,6},{9,6},{30,6},{33,6},{6,24},{9,24},{30,24},{33,24}}) p.setObject(at[0], at[1], object("crate"));
        for (int[] at : new int[][]{{12,12},{26,12},{12,18},{26,18}}) p.setObject(at[0], at[1], object("scraplamp"));
        return p;
    }

    // =======================================================================
    // The plan interpreter
    //
    // docs/design/chapter-01-skyreach-pois.md holds fourteen room plans drawn
    // one character per tile. Transcribing each of them into setObject calls by
    // hand -- the way the fourteen above were built -- means
    // re-deciding the dossier's four written-once rules fourteen times, and
    // getting one of them wrong fourteen ways. So the plan itself is the source
    // and the rules live here, once:
    //
    //   0.2  a multi-tile piece writes BOTH halves with the SAME rotation,
    //        the far half in the direction the rotation points
    //   0.2  wall decor goes on ObjectLayerRegistry.WALL_DECOR and its OWN tile
    //        must not be masonry, or the banner opens a hole in the building
    //   0.2  table decorations go on FENCE_AND_TABLE_DECOR, on a table tile
    //   0.3  a window sits mid-run: exactly one opposite pair of connected
    //        walls, the other axis open. WallWindowObject.getWindowDir returns
    //        -1 otherwise and the engine silently deletes it
    //   0.4  a straight fence run has orthogonal neighbours; a lone post is the
    //        thing the player already complained about
    //
    // Every one of these throws at load rather than in the world:
    // RealmPoiWorldPreset.onRegistryClosed builds every kind when the registries
    // close, so a plan that breaks a rule fails the game's startup, not a
    // player's walk. Rings and curves (POIs 6, 8, 9) still need
    // SkyLandscape.discRing and are out of this interpreter's scope.
    // =======================================================================

    /** How many distinct plan characters a legend can carry; plain ASCII. */
    private static final int LEGEND_SIZE = 128;

    /**
     * The character table one plan is read through.
     *
     * <p>{@code ground} is the floor written under every character that does not
     * name a tile of its own, so a plan's props stand on its own paving rather
     * than on whatever the terrain painter grew. Characters declared with
     * {@link #loose} keep that painter ground on purpose -- a streetlamp beside
     * a hut, a fence across a meadow.
     */
    private static final class Legend {
        private final int ground;
        private final int[] tile = new int[LEGEND_SIZE];
        private final int[] object = new int[LEGEND_SIZE];
        private final byte[] rotation = new byte[LEGEND_SIZE];
        /** Far half of a multi-tile piece, and the plan character that must mark it. */
        private final int[] counter = new int[LEGEND_SIZE];
        private final char[] counterChar = new char[LEGEND_SIZE];
        /** Decorations for a table character, handed out in plan reading order. */
        private final int[][] tableDecor = new int[LEGEND_SIZE][];
        private final int[] tableDecorNext = new int[LEGEND_SIZE];
        /** WALL_DECOR object, with {@link #rotation} holding where the wall is. */
        private final int[] wallDecor = new int[LEGEND_SIZE];
        /** TILE_LAYER object -- a carpet, which is the only thing that lives there. */
        private final int[] rug = new int[LEGEND_SIZE];
        /** Formation characters: what is scattered, and over what share of the cells. */
        private final int[][] formation = new int[LEGEND_SIZE][];
        private final float[] coverage = new float[LEGEND_SIZE];
        /** Counts as connected masonry for the window and wall-decor rules. */
        private final boolean[] masonry = new boolean[LEGEND_SIZE];
        private final boolean[] isWindow = new boolean[LEGEND_SIZE];
        private final boolean[] isFence = new boolean[LEGEND_SIZE];
        private final boolean[] isTable = new boolean[LEGEND_SIZE];
        private final boolean[] isChair = new boolean[LEGEND_SIZE];
        private final boolean[] known = new boolean[LEGEND_SIZE];
        /** Single tiles where the plan's character means a second thing. */
        private final java.util.HashMap<Integer, Character> override =
                new java.util.HashMap<>();
        /** Single tiles where a character's piece is turned differently. */
        private final java.util.HashMap<Integer, Byte> turn =
                new java.util.HashMap<>();

        Legend(int ground) {
            this.ground = ground;
            java.util.Arrays.fill(this.tile, -1);
            java.util.Arrays.fill(this.object, -1);
            java.util.Arrays.fill(this.counter, -1);
            java.util.Arrays.fill(this.wallDecor, -1);
            java.util.Arrays.fill(this.rug, -1);
        }

        private Legend mark(char c) {
            if (c >= LEGEND_SIZE) {
                throw new IllegalStateException("Plan character '" + c + "' is not ASCII");
            }
            if (this.known[c]) {
                throw new IllegalStateException("Plan character '" + c + "' is declared twice");
            }
            this.known[c] = true;
            return this;
        }

        /** The plan's own paving: writes the ground and clears what stood on it. */
        Legend floor(char c) {
            mark(c);
            this.tile[c] = this.ground;
            this.object[c] = 0;
            return this;
        }

        /** Paving of its own kind -- an inlay, a weighbridge, a terrace. */
        Legend floor(char c, String tileID) {
            floor(c);
            this.tile[c] = tile(tileID);
            return this;
        }

        Legend prop(char c, String objectID) {
            return prop(c, objectID, UP);
        }

        /** A piece standing on the plan's own ground. */
        Legend prop(char c, String objectID, int rotation) {
            mark(c);
            this.tile[c] = this.ground;
            this.object[c] = object(objectID);
            this.rotation[c] = (byte) rotation;
            return this;
        }

        /** A piece that keeps the terrain painter's own ground under it. */
        Legend loose(char c, String objectID) {
            mark(c);
            this.object[c] = object(objectID);
            return this;
        }

        /**
         * A multi-tile piece, both halves, same rotation (§0.2).
         *
         * <p>{@code Preset.applyToLevel} never runs {@code MultiTile.placeObject},
         * so the far half is written here or it is not written at all -- and a
         * bed with no foot is a bed the player cannot sleep in. The far half
         * lands in the direction the rotation points, and {@code counterChar}
         * must be the character the plan draws there: that is what makes the
         * ASCII map and the object agree instead of merely coexist.
         */
        Legend pair(char c, char counterChar, String objectID, int rotation) {
            prop(c, objectID, rotation);
            mark(counterChar);
            this.counter[c] = object(objectID + "2");
            this.counterChar[c] = counterChar;
            // The far half is written by its master. Its own character carries
            // the ground so the floor under it stays the room's floor.
            this.tile[counterChar] = this.ground;
            // A two-tile TABLE is a table on both its halves: §2.7 seats four
            // chairs at two dinner tables and two of them face the far half.
            // {@code DinnerTable2Object extends TableObject}, so
            // ChairObject.facesTable really does accept that tile -- this asks
            // the engine's own question rather than assuming the answer.
            if (ObjectRegistry.getObject(this.object[c]) instanceof
                    necesse.level.gameObject.TableObjectInterface) {
                this.isTable[c] = true;
                this.isTable[counterChar] = true;
            }
            return this;
        }

        /** Masonry: wall, and the two things that count as connected wall. */
        Legend wall(char c, String objectID) {
            prop(c, objectID);
            this.masonry[c] = true;
            return this;
        }

        /**
         * A door. NOT masonry: {@code WallDoorObject extends DoorObject}, and
         * only {@code WallObject} and {@code WallWindowObject} put themselves in
         * {@code WallObject.connectedWalls}, so a window beside a door is a
         * window with an open side.
         */
        Legend door(char c, String objectID) {
            return prop(c, objectID);
        }

        /** A window, checked against §0.3 wherever the plan places it. */
        Legend window(char c, String objectID) {
            wall(c, objectID);
            this.isWindow[c] = true;
            return this;
        }

        /** A fence or gate, checked against §0.4 for lone posts. */
        Legend fence(char c, String objectID) {
            loose(c, objectID);
            this.isFence[c] = true;
            return this;
        }

        /**
         * A table, and the decorations that stand ON it (§0.2).
         *
         * <p>Table decorations live on {@code FENCE_AND_TABLE_DECOR} and cannot
         * stand on bare floor, so this is the only way the interpreter will
         * write one -- there is deliberately no "decoration" character. Where a
         * plan draws one character for several tables carrying different pieces
         * (§2.11 draws {@code mm} for a tome and a potted cloudberry), the list
         * is handed out in plan reading order, left to right and top to bottom.
         */
        Legend table(char c, String tableID, String... decorIDs) {
            prop(c, tableID);
            // "Tischdeko nur auf Tischen" is only worth anything if the thing
            // called a table really is one. ChairObject.facesTable and
            // TableDecorationObject both test for this interface and nothing
            // else, so it is the same question the engine asks.
            if (!(ObjectRegistry.getObject(this.object[c]) instanceof
                    necesse.level.gameObject.TableObjectInterface)) {
                throw new IllegalStateException("Table character '" + c + "' is "
                        + tableID + ", which is not a TableObjectInterface;"
                        + " a decoration cannot stand on it");
            }
            this.isTable[c] = true;
            int[] decor = new int[decorIDs.length];
            for (int i = 0; i < decorIDs.length; i++) {
                decor[i] = layered(decorIDs[i], ObjectLayerRegistry.FENCE_AND_TABLE_DECOR,
                        "a table decoration");
            }
            this.tableDecor[c] = decor;
            return this;
        }

        /**
         * A chair, whose rotation is READ off the plan rather than declared
         * (§0.2: "a chair at a table is turned toward the table").
         *
         * <p>{@code ChairObject.facesTable} checks exactly the tile the rotation
         * points at -- 0 above, 1 right, 2 below, 3 left -- so {@link #plan}
         * turns each chair toward the orthogonally adjacent table character and
         * throws if there is none. One character can therefore serve every chair
         * in a room whatever side of the table it sits on, which is how the
         * dossier draws them.
         */
        Legend chair(char c, String objectID) {
            prop(c, objectID);
            this.isChair[c] = true;
            return this;
        }

        /**
         * Wall decor -- a banner or a wall lamp (§0.2).
         *
         * <p>{@code wallDir} is where the WALL is, not where the piece faces.
         * The character marks the FLOOR tile the piece hangs from, so this
         * LAYERS onto a character already declared as floor: {@code
         * floor('n').decor('n', "skywatchbanner", WALL_ABOVE)}. Hanging one off
         * a wall character is refused here, because a banner written onto
         * masonry replaces it and opens a hole in the building -- the failure
         * the dossier calls out by name. {@link #plan} then checks that the wall
         * it says it hangs from is really drawn there.
         */
        Legend decor(char c, String objectID, int wallDir) {
            if (c >= LEGEND_SIZE || !this.known[c]) {
                throw new IllegalStateException("Wall decor '" + c
                        + "' must first be declared as the floor tile it hangs from");
            }
            if (this.object[c] != 0) {
                throw new IllegalStateException("Wall decor '" + c
                        + "' sits on a character that carries an object of its own"
                        + (this.masonry[c] ? " -- masonry, which it would replace,"
                                + " opening a hole in the building" : ""));
            }
            this.wallDecor[c] = layered(objectID, ObjectLayerRegistry.WALL_DECOR, "wall decor");
            this.rotation[c] = (byte) wallDir;
            return this;
        }

        /**
         * A character the plan draws that this build deliberately does not
         * write, with the reason in the caller. Used for the dossier's unbuilt
         * art: {@link #object} would throw at load rather than quietly drop it,
         * and shipping an error texture is a release blocker
         * ({@code docs/IMPLEMENTATION_RULES.md} §5).
         */
        Legend pending(char c, boolean keepGround) {
            mark(c);
            if (keepGround) {
                this.tile[c] = this.ground;
                this.object[c] = 0;
            }
            return this;
        }

        /**
         * One tile where the plan's character is read as {@code meaning}
         * instead.
         *
         * <p>A legend is otherwise a per-character table, and that is what
         * makes a plan reviewable. §2.7's own legend breaks it exactly once:
         * it draws {@code 's'} for the bookshelf inside the wayhouse AND for
         * the stele out on the apron, "s (interior) … s (y11)". The plan is
         * transcribed verbatim (the dossier is design intent and is not
         * rewritten to match a build), so the second meaning is named here, by
         * coordinate. {@link #plan} checks the tile is really drawn.
         */
        Legend reads(int x, int y, char meaning) {
            if (meaning >= LEGEND_SIZE || !this.known[meaning]) {
                throw new IllegalStateException("Plan tile " + x + "," + y
                        + " is read as '" + meaning + "', which has no legend entry");
            }
            this.override.put((x << 16) | y, meaning);
            return this;
        }

        /**
         * One tile where the piece is turned differently from its character.
         *
         * <p>A legend is a per-character table; a rotation is a per-tile fact,
         * and the dossier states it per tile in every section's object table.
         * Most plans get away with the two being the same thing. §2.4 and §2.5
         * do not: both draw ONE lantern character on all four inner faces of
         * their shell, and §0.2's wall-decor rotation is where the WALL is, so
         * the same character needs all four values. §2.4 does it again with the
         * cabinet — the armoury's vault backs onto the north wall, the
         * bunkroom's chest onto the south.
         *
         * <p>This is where those rows of the object table are transcribed. It
         * overrides whatever the character declared, including the direction a
         * {@link #chair} was turned and the side a {@link #pair}'s far half
         * lands on; {@link #plan} checks the tile is really drawn.
         */
        Legend turns(int x, int y, int rotation) {
            this.turn.put((x << 16) | y, (byte) rotation);
            return this;
        }

        /**
         * A carpet, on the one layer the engine keeps carpets on.
         *
         * <p>VERIFIED [jar]: {@code ModularCarpetObject}'s constructor adds
         * {@code ObjectLayerRegistry.TILE_LAYER} and nothing else, which is
         * also the layer {@code WardenSpirePreset} lays its own carpet on.
         * Writing one with {@code setObject} would put it on the base layer,
         * where it takes the tile the furniture standing on the rug needs.
         */
        Legend rug(char c, String objectID) {
            floor(c);
            this.rug[c] = layered(objectID, ObjectLayerRegistry.TILE_LAYER, "a carpet");
            return this;
        }

        /**
         * A formation rather than a fill: the character carries its pieces on
         * {@code coverage} of its own cells, and only there.
         *
         * <p>Every other character is one tile, one object. §2.6's rim is the
         * one place in the dossier where that is not what is drawn: 92 cells of
         * {@code 'x'} described as "skystonerock, skyscree and skywatchrubble
         * ... scattered as a formation, ~55% coverage". Writing a rock on all
         * 92 would not be a denser version of that — it would <b>seal the
         * arena</b>. VERIFIED [jar]: {@code RockObject} passes a full 32x32
         * collision to {@code GameObject}, which sets {@code isSolid} and
         * {@code RegionType.WALL}; the rim's only four gaps hold a
         * {@code StreetlampObject}, itself solid on an 10x10 box. The place
         * whose whole point is walking into it would be reachable only with a
         * pickaxe.
         *
         * <p>Deterministic, from the tile's own coordinates: the same plan has
         * to produce the same preset in {@link
         * RealmPoiWorldPreset#onRegistryClosed}, in the census, and at every
         * placement in the world.
         */
        Legend scatter(char c, float coverage, String... objectIDs) {
            mark(c);
            int[] pieces = new int[objectIDs.length];
            for (int i = 0; i < objectIDs.length; i++) {
                pieces[i] = object(objectIDs[i]);
            }
            this.formation[c] = pieces;
            this.coverage[c] = coverage;
            return this;
        }
    }

    /**
     * The salt {@link Legend#scatter} hashes its tile coordinates with. Fixed,
     * because a formation that moved between builds would be a different
     * building every time the preset is asked for.
     */
    private static final long SCATTER_SALT = 0x5C47L;

    /**
     * An object that really belongs on the layer a legend wants to put it on.
     *
     * <p>{@code GameObject.getValidObjectLayers} is the engine's own answer:
     * {@code TableDecorationObject} adds {@code FENCE_AND_TABLE_DECOR} and
     * {@code SkyWallLightObject} adds {@code WALL_DECOR}. Writing an ordinary
     * object onto one of those layers is not refused by {@code Preset} and
     * produces a piece that draws in the wrong place and cannot be removed.
     */
    private static int layered(String id, int layer, String what) {
        int value = object(id);
        if (!ObjectRegistry.getObject(value).getValidObjectLayers().contains(layer)) {
            throw new IllegalStateException(id + " is not " + what
                    + ": its valid object layers are "
                    + ObjectRegistry.getObject(value).getValidObjectLayers()
                    + ", not " + layer);
        }
        return value;
    }

    /** The character at {@code (x,y)} of a plan, or {@code '.'} off its edge. */
    private static char at(String[] rows, int x, int y) {
        if (y < 0 || y >= rows.length) return '.';
        String row = rows[y];
        if (x < 0 || x >= row.length()) return '.';
        return row.charAt(x);
    }

    private static boolean masonryAt(String[] rows, Legend legend, int x, int y) {
        char c = at(rows, x, y);
        return c < LEGEND_SIZE && legend.masonry[c];
    }

    /**
     * The rotation that turns a chair at {@code (x,y)} toward its table, in
     * {@code ChairObject.facesTable}'s own terms, or -1 if no side has one.
     */
    private static int towardTable(String[] rows, Legend legend, int x, int y) {
        int[][] sides = {{UP, 0, -1}, {RIGHT, 1, 0}, {DOWN, 0, 1}, {LEFT, -1, 0}};
        for (int[] side : sides) {
            char c = at(rows, x + side[1], y + side[2]);
            if (c < LEGEND_SIZE && legend.isTable[c]) {
                return side[0];
            }
        }
        return -1;
    }

    /**
     * Writes one of the dossier's room plans into a preset.
     *
     * <p>{@code '.'} and a space write NOTHING -- {@code Preset} holds -1 in
     * every cell and {@code applyToLevel} skips -1 (verified in the decompiled
     * {@code Preset.applyToLevel}), so the terrain painter's own meadow, crag or
     * cloud sea shows through the margins. That is the opposite of {@link
     * #blank}, which clears its whole rectangle and so bulldozes them; a plan
     * preset must not be blanked first.
     *
     * @throws IllegalStateException on any transcription slip or any breach of
     *         §0.2-§0.4 -- see this section's header for the list
     */
    private static void plan(Preset p, String[] rows, Legend legend) {
        // Every named tile has to be a tile the plan really draws on. An
        // override on empty margin would move a piece nobody can see it move.
        for (java.util.Map.Entry<Integer, Character> entry : legend.override.entrySet()) {
            int x = entry.getKey() >> 16;
            int y = entry.getKey() & 0xFFFF;
            char drawn = at(rows, x, y);
            if (drawn == '.' || drawn == ' ') {
                throw new IllegalStateException("Plan tile " + x + "," + y
                        + " is read as '" + entry.getValue()
                        + "', but the plan draws nothing there");
            }
        }
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            if (row.length() != rows[0].length()) {
                throw new IllegalStateException("Plan row " + y + " is " + row.length()
                        + " wide, row 0 is " + rows[0].length()
                        + " -- a dropped character shifts the whole building");
            }
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                Character second = legend.override.get((x << 16) | y);
                if (second != null) c = second;
                if (c == '.' || c == ' ') continue;
                if (c >= LEGEND_SIZE || !legend.known[c]) {
                    throw new IllegalStateException(
                            "Plan character '" + c + "' at " + x + "," + y + " has no legend entry");
                }
                String where = "'" + c + "' at " + x + "," + y;

                // 0.3: a window in a corner -- or beside a door -- is silently
                // deleted by the engine. Refuse it here instead.
                if (legend.isWindow[c]) {
                    boolean up = masonryAt(rows, legend, x, y - 1);
                    boolean down = masonryAt(rows, legend, x, y + 1);
                    boolean left = masonryAt(rows, legend, x - 1, y);
                    boolean right = masonryAt(rows, legend, x + 1, y);
                    boolean vertical = up && down && !left && !right;
                    boolean horizontal = left && right && !up && !down;
                    if (!vertical && !horizontal) {
                        throw new IllegalStateException("Window " + where
                                + " is not mid-run in a straight wall (walls up=" + up
                                + " right=" + right + " down=" + down + " left=" + left
                                + "); WallWindowObject.getWindowDir would delete it");
                    }
                }

                // 0.4: a straight run is a connected fence; a lone post is not.
                if (legend.isFence[c]) {
                    char n = at(rows, x, y - 1), e = at(rows, x + 1, y);
                    char s = at(rows, x, y + 1), w = at(rows, x - 1, y);
                    boolean joined = (n < LEGEND_SIZE && legend.isFence[n])
                            || (e < LEGEND_SIZE && legend.isFence[e])
                            || (s < LEGEND_SIZE && legend.isFence[s])
                            || (w < LEGEND_SIZE && legend.isFence[w]);
                    if (!joined) {
                        throw new IllegalStateException("Fence " + where
                                + " has no orthogonal neighbour; FenceObject.attachesToObject"
                                + " would draw a lone post");
                    }
                }

                // 0.2: a chair is turned toward the table it sits at, and the
                // plan says which side that is.
                int rotation = legend.rotation[c];
                if (legend.isChair[c]) {
                    rotation = towardTable(rows, legend, x, y);
                    if (rotation < 0) {
                        throw new IllegalStateException("Chair " + where
                                + " has no table on any orthogonal side;"
                                + " ChairObject.facesTable would find nothing to sit at");
                    }
                }

                if (legend.tile[c] >= 0) {
                    p.setTile(x, y, legend.tile[c]);
                }
                if (legend.object[c] >= 0) {
                    p.setObject(x, y, legend.object[c], rotation);
                }

                // 0.2: both halves, same rotation, far half where the rotation
                // points -- and the plan has to draw it there.
                if (legend.counter[c] >= 0) {
                    int dx = rotation == RIGHT ? 1 : rotation == LEFT ? -1 : 0;
                    int dy = rotation == DOWN ? 1 : rotation == UP ? -1 : 0;
                    char drawn = at(rows, x + dx, y + dy);
                    if (drawn != legend.counterChar[c]) {
                        throw new IllegalStateException("Multi-tile piece " + where
                                + " points its far half at " + (x + dx) + "," + (y + dy)
                                + ", where the plan draws '" + drawn + "' and not '"
                                + legend.counterChar[c] + "'");
                    }
                    p.setObject(x + dx, y + dy, legend.counter[c], rotation);
                }

                // 0.2: table decoration, on a table, on its own layer.
                int[] decor = legend.tableDecor[c];
                if (decor != null && decor.length > 0) {
                    p.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, x, y,
                            decor[legend.tableDecorNext[c]++ % decor.length]);
                }

                // 0.2: wall decor hangs from masonry. That its OWN tile is not
                // masonry is settled in Legend.decor, where the character is
                // declared; what only the plan can answer is whether the wall it
                // names is really drawn on that side.
                if (legend.wallDecor[c] >= 0) {
                    int wallDir = legend.rotation[c];
                    int wx = x + (wallDir == WALL_RIGHT ? 1 : wallDir == WALL_LEFT ? -1 : 0);
                    int wy = y + (wallDir == WALL_BELOW ? 1 : wallDir == WALL_ABOVE ? -1 : 0);
                    if (!masonryAt(rows, legend, wx, wy)) {
                        throw new IllegalStateException("Wall decor " + where
                                + " says its wall is at " + wx + "," + wy
                                + ", where the plan draws '" + at(rows, wx, wy) + "'");
                    }
                    p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, x, y,
                            legend.wallDecor[c], wallDir);
                }
            }
        }
    }

    /**
     * POI 2.1, the Skywatch Wayside, from the plan in §2.1 verbatim.
     *
     * <p>The dossier's smallest place and the world's connective tissue: a paved
     * pocket off the road, a balustrade on three sides, benches with their backs
     * to the rail, and a locked offering cabinet. 3 lights in 99 tiles = 1 per
     * 33, inside §0.5's 25-40 band for a designed open-air place.
     *
     * <p>The stele at (5,3) is left out: {@code skywatchstele} is §4's new art
     * and is not registered, and with it goes the Warden's Ledger drip-feed
     * (§3.1) this POI exists to carry. Its tile stays paving so the pocket still
     * reads whole.
     */
    private static final String[] WAYSIDE_PLAN = {
            "...........",
            ".|||||||||.",
            ".|Bb,,,Bb|.",
            ".|v,,S,,w|.",
            ".|,,,,,,,|.",
            ".|k,,c,,r|.",
            ".|,,,,,,,|.",
            ".L,,,,,,,L.",
            "...........",
    };

    private static Preset waysideShrine() {
        Preset p = new Preset(width(SKY_WAYSIDE_SHRINE), height(SKY_WAYSIDE_SHRINE));
        Legend legend = new Legend(SkyRegistry.skyroadTileID)
                .floor(',')
                .fence('|', "cloudmarblefence")
                // Backs to the north rail, both halves, rotation 1 (§2.1).
                .pair('B', 'b', "skywatchbench", RIGHT)
                .prop('k', "skywatchcabinet", RIGHT)
                .prop('c', "skywatchcandelabra")
                .loose('L', "wardencandelabra")
                // Planting, not scatter: the pocket's own three plants.
                .prop('v', "skytulip")
                .prop('w', "cloudbell")
                .prop('r', "cloudberrybush")
                .pending('S', true);
        plan(p, WAYSIDE_PLAN, legend);
        return p;
    }

    /**
     * POI 2.11, the Dew-Keeper's Hut, from the plan in §2.11 verbatim.
     *
     * <p>A small dwelling with nobody in it: one room of 35 tiles, a snail run
     * outside, the bed made. Two windows, both mid-run -- (7,1) north and (11,4)
     * east -- which is exactly what {@link #plan}'s §0.3 test proves rather than
     * assumes. The five Dew Snails in the run are placed by
     * {@link RealmPoiWorldPreset}, the only caller that holds a level.
     *
     * <p>The stele at (2,4) and the cracked cistern at (6,11) are left out for
     * the reason {@link #waysideShrine} gives: both are §4 new art. They stand
     * on the run's own ground, so their tiles are left to the terrain painter.
     */
    private static final String[] HUT_PLAN = {
            ".............",
            "...####O####.",
            "...#c====eE#.",
            "...#==h====#.",
            "..sD==mm===O.",
            "...#k==h===#.",
            "...#o=====r#.",
            "...#########.",
            "..L..........",
            "..ffffGffff..",
            "..f.g...g.f..",
            "..f..gw...f..",
            "..fffffffff..",
    };

    private static Preset dewKeepersHut() {
        Preset p = new Preset(width(SKY_DEW_KEEPERS_HUT), height(SKY_DEW_KEEPERS_HUT));
        Legend legend = new Legend(SkyRegistry.prismFloorID)
                .floor('=')
                .wall('#', "skystonebrickwall")
                .window('O', "skystonebrickwindow")
                .door('D', "skystonebrickdoor")
                // Head to the east wall, both halves, rotation 3 (§2.11).
                .pair('E', 'e', "skywatchbed", LEFT)
                .prop('r', "skywatchdresser", LEFT)
                .table('m', "skywatchmodulartable", "skywatchtome", "pottedcloudberry")
                .chair('h', "skywatchchair")
                .prop('k', "skywatchcabinet", RIGHT)
                .prop('o', "cookingpot")
                .prop('c', "skywatchcandelabra")
                .loose('L', "wardencandelabra")
                .fence('f', "skyironfence")
                .fence('G', "skyironfencegate")
                .loose('g', "glowfern")
                .pending('s', false)
                .pending('w', false);
        plan(p, HUT_PLAN, legend);
        return p;
    }

    /**
     * POI 2.2, the Shepherd's Fold, from the plan in §2.2 verbatim.
     *
     * <p>The cottage the mod promised and never built, inside a plot that is
     * deliberately more than half empty grazing. Four windows, all mid-run:
     * (13,2) north, (9,4) and (9,8) west, (17,6) east — the north one has the
     * plot fence running past it at (13,1), which is not masonry and so does
     * not close the run ({@code WallObject.connectedWalls} holds walls and
     * wall-windows only). The pasture's own meadow is the terrain painter's:
     * every {@code '.'} is left unwritten, which is where §2.2's
     * {@code tallcloudgrass} already grows ({@code SkyTerrainPainter}).
     *
     * <p>Three things the section asks for are NOT here, and each for the same
     * reason: the plan is the source and it does not draw them.
     * <ul>
     *   <li>{@code cloudspringfont} at (4,6) is §4 new art, like the steles —
     *       {@link #object} would throw at load rather than drop it quietly.
     *   <li>The {@code feedingtrough} at (4,8): vanilla registers it as a
     *       two-tile piece ({@code feedingtrough} + {@code feedingtrough2},
     *       {@code MultiTile(0,1,1,2)}), and §0.2 wants both halves drawn. The
     *       plan draws one {@code 'u'}, so this ships no trough rather than
     *       half of one.
     *   <li>The two {@code skywatchbanner} and the nine-tile carpet appear in
     *       §2.2's object table but in no cell of its map. Writing them beside
     *       the interpreter would be the hand-transcription this whole section
     *       exists to replace.
     * </ul>
     *
     * <p>The flock — Glimmergoats and a Nimbus Yak, per the section's own
     * 2026-09-02 note that the Cloud Lamb is gone — is placed by
     * {@link RealmPoiWorldPreset}, the only caller that holds a level. So is
     * Wren: she is not registered, and the fold stands without her.
     */
    private static final String[] FOLD_PLAN = {
            ".....................",
            ".|||||||||||||||||||.",
            ".|.......####O####.|.",
            ".|.T.....#W======#.|.",
            ".|......LO=====eE#.|.",
            ".|.......#ch=====#.|.",
            ".|..w....#=mmh==rO.|.",
            ".|.....bb#=h=====#.|.",
            ".|..u...bO=====c=#.|.",
            ".|.......#k===os=#.|.",
            ".|.......####D####.|.",
            ".|ffGff.....,,,....|.",
            ".|f...f....L,,,L...|.",
            ".|f...fT....,,,....|.",
            ".|f...f.....,,,....|.",
            ".ffffffffffffGffffff.",
            ".....................",
    };

    private static Preset shepherdsFold() {
        Preset p = new Preset(width(SKY_SHEPHERDS_FOLD), height(SKY_SHEPHERDS_FOLD));
        Legend legend = new Legend(SkyRegistry.nimbusFloorID)
                .floor('=')
                .floor(',', "snowstonepathtile")
                .wall('#', "skystonebrickwall")
                .window('O', "skystonebrickwindow")
                .door('D', "skystonebrickdoor")
                // The plot rail and the lambing pen are the same fence; two
                // characters because the plan reads better with two.
                .fence('|', "skyironfence")
                .fence('f', "skyironfence")
                .fence('G', "skyironfencegate")
                // Head to the east wall, both halves, rotation 3 (§2.2).
                .pair('E', 'e', "skywatchbed", LEFT)
                .prop('r', "skywatchdresser", LEFT)
                .prop('W', "windsilkloom", RIGHT)
                // One two-tile table drawn as two: chalice west, tome east, in
                // the plan's own reading order.
                .table('m', "skywatchmodulartable", "skywatchchalice", "skywatchtome")
                .chair('h', "skywatchchair")
                .prop('k', "skywatchcabinet")
                .prop('s', "skywatchbookshelf")
                .prop('o', "skywatchclock")
                .prop('c', "skywatchcandelabra")
                .loose('L', "wardencandelabra")
                .loose('T', "cloudtree")
                .loose('b', "cloudberrybush")
                .pending('w', false)
                .pending('u', false);
        plan(p, FOLD_PLAN, legend);
        return p;
    }

    /**
     * POI 2.3, the Institute of Applied Falling, from the plan in §2.3
     * verbatim.
     *
     * <p>A plank ramp that stops in mid-air, six machines falling away from its
     * end in a widening arc, and a crater. 425 of its 525 tiles are the meadow
     * the terrain painter already grows and this preset writes nothing into —
     * that empty middle is the joke, so the arc's props are {@link
     * Legend#loose} and keep the ground they landed on.
     *
     * <p>The four {@code skywatchstele} that carry the test log are §4 new art
     * and are left out; without them the sequel pointer to §2.14 goes with
     * them, which is written up rather than faked. Test Subject VII — the
     * Nimbus Yak in the middle of the crater, unharmed, chewing — is a mob and
     * is placed by {@link RealmPoiWorldPreset}; its tile is crater floor here
     * so the punchline stands on something.
     */
    private static final String[] INSTITUTE_PLAN = {
            ".........................",
            ".........................",
            ".....L....L....L.........",
            "..s=============.........",
            "..=============..........",
            ".................W.......",
            ".................s.......",
            "...................b.....",
            "...................p.....",
            "..................W......",
            "..................s......",
            "................p........",
            "...............Wr........",
            "..............s..........",
            ".........Lxxxxxxxxx......",
            ".........x########x......",
            ".........xWk######x......",
            ".........x###Y####x......",
            ".........x########x......",
            ".........xxxxxxxxxL......",
            ".........................",
    };

    private static Preset fallingInstitute() {
        Preset p = new Preset(width(SKY_FALLING_INSTITUTE), height(SKY_FALLING_INSTITUTE));
        // The crater floor is the ground: it is what the cabinet and the yak
        // stand on. The ramp names its own decking.
        Legend legend = new Legend(tile("skystonetile"))
                .floor('#')
                .floor('=', "nimbusfloortile")
                // The rim is drawn as scree AND rock; one character is one
                // object, and the rock is what makes a crater read as a crater.
                .loose('x', "skystonerock")
                .loose('W', "aeronautwreck")
                .loose('b', "skyballoon")
                .loose('p', "skyparcel")
                .loose('r', "skywatchrubble")
                .loose('L', "wardencandelabra")
                // The reward, back to the crater's north wall of rim (§2.3).
                .prop('k', "skywatchcabinet", DOWN)
                .pending('s', false)
                .pending('Y', true);
        plan(p, INSTITUTE_PLAN, legend);
        return p;
    }

    /**
     * POI 2.7, the Passage Wayhouse, from the plan in §2.7 verbatim.
     *
     * <p>A cloudmarble inn with two real beds and a cache, on ground that
     * currently offers neither. Four windows, all mid-run: (6,1) and (10,1)
     * north, (3,4) west, (13,6) east. Two dinner tables seat four chairs, and
     * two of those chairs face the tables' FAR halves — which is why {@link
     * Legend#pair} marks both halves of a table as one.
     *
     * <p>The two {@code skywaywaystone} at (6,10) and (10,10) are the reason
     * the section exists and are §4 new art; their tiles stay apron paving so
     * the approach still reads whole, and the fast-travel network they carry is
     * left unbuilt rather than mocked up. The {@code skywatchstele} at (8,11)
     * goes with them — §2.7's legend draws it with the same {@code 's'} as the
     * bookshelf inside the house, so {@link Legend#reads} names that one tile.
     *
     * <p>Rows y12-y14 are drawn as the causeway that is already there. Nothing
     * places this preset on one yet ({@link RealmPoiWorldPreset} has no
     * on-a-road test), so they are written as what they are drawn as: two rows
     * of apron and a balustrade along the south edge. On a real passage that is
     * the paving and the rail the passage already has.
     */
    private static final String[] WAYHOUSE_PLAN = {
            "...................",
            "...###O###O###.....",
            "...#cn=====eE#.....",
            "...#=hT=hT===#..Y..",
            "...O==th=th==#.....",
            "...#=========#..w..",
            "...#k=====c==O.....",
            "...#=======eE#.....",
            "...#s=====o==#.Y...",
            "...#####D#####.....",
            "..L,,,y,,,y,,,L....",
            "..,,,,,,s,,,,,,....",
            ",,,,,,,,,,,,,,,,,,,",
            ",,,,,,,,,,,,,,,,,,,",
            "|||||||||||||||||||",
    };

    private static Preset passageWayhouse() {
        Preset p = new Preset(width(SKY_PASSAGE_WAYHOUSE), height(SKY_PASSAGE_WAYHOUSE));
        Legend legend = new Legend(SkyRegistry.gloomwoodFloorID)
                .floor('=')
                .floor(',', "skywaytile")
                // The waystone is unbuilt art; the apron under it is not.
                .floor('y', "skywaytile")
                .wall('#', "cloudmarblewall")
                .window('O', "cloudmarblewindow")
                .door('D', "cloudmarbledoor")
                .fence('|', "cloudmarblefence")
                // Both dinner tables run north-south, rotation 2 (§2.7).
                .pair('T', 't', "skywatchdinnertable", DOWN)
                .chair('h', "skywatchchair")
                // Both beds head to the east wall, rotation 3 (§2.7).
                .pair('E', 'e', "skywatchbed", LEFT)
                .prop('k', "skywatchcabinet", RIGHT)
                .prop('s', "skywatchbookshelf")
                .prop('o', "skywatchclock")
                .prop('c', "skywatchcandelabra")
                .floor('n')
                .decor('n', "skywatchbanner", WALL_ABOVE)
                .loose('L', "wardencandelabra")
                .loose('Y', "cloudtree")
                .pending('w', false)
                // §2.7's own legend gives 's' two meanings; out here it is the
                // stele, which is not built, so the apron simply runs through.
                .reads(8, 11, ',');
        plan(p, WAYHOUSE_PLAN, legend);
        return p;
    }
}
