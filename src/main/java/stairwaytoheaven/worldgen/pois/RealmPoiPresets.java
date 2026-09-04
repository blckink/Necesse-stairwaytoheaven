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
    public static final int COUNT = 13;

    private static final int UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3;

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
            default: throw new IllegalArgumentException("Unknown realm POI " + kind);
        }
    }

    public static int realm(int kind) {
        if (kind <= SKY_INN) return 0;
        if (kind <= EDEN_FERMENT_HOUSE) return 1;
        if (kind == STEINFELD_MEMORIAL) return 2;
        if (kind == GHOST_ARCHIVE) return 3;
        if (kind == CROOKED_BAZAAR) return 4;
        return 5;
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
        windows(p, window, new int[][]{{10,25},{16,25},{32,25},{38,25},{12,19},{36,19},{17,11},{31,11},{21,5},{27,5}});
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
}
