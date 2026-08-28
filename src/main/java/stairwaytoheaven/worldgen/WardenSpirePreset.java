package stairwaytoheaven.worldgen;

import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyCloudmarbleSet;
import stairwaytoheaven.SkyFurnitureSet;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.mobs.SkyWardenMob;
import stairwaytoheaven.quest.SkywatchQuestData;

/**
 * The Warden's Spire: the Skywatch hall at the centre of the Skyreach.
 *
 * <h2>The plan</h2>
 * A double wall ring on a 21x21 plot, built to the layout the user supplied
 * (decoded in {@code docs/references/presets/warden-tower-layout.script}):
 *
 * <pre>
 *   local 3..17   outer cloudmarble ring, 3x3 corner buttresses, four doors
 *                 on the axes, eight windows between them
 *   local 4..16   the circulation corridor, gloomwood planks
 *   the octagon   inner ring with its own four doors on the same axes
 *   the chamber   37 tiles of pale cloudstone, the beacon on a chequer
 *                 plinth at the centre
 * </pre>
 *
 * The corridor's four corner pockets are the furnished rooms — refectory
 * (NW), council table (NE), the Warden's own quarters (SW), archive (SE) —
 * and the four straight galleries between them carry the benches, the lamps
 * and the way in. The central chamber stays deliberately empty apart from the
 * beacon: that is the whole point of the reference plan, and it is what makes
 * the hall read as a hall rather than a furniture shop.
 *
 * <h2>What this preset writes</h2>
 * Only local 1..19 ({@link #WRITTEN_RADIUS} tiles from the centre), and never
 * the four plot corners. Everything outside is the Warden's Forecourt, which
 * {@code SkyLandscape} composes: its lamp ring (radius 11), its chequered
 * inlay and its railing (radius 13) must survive us. The four tiles at
 * (+-9, +-9) are the railing's diagonal links — writing them would open four
 * gaps in the forecourt wall — so they are left alone on purpose.
 *
 * <h2>Multi-tile furniture</h2>
 * Benches, beds and dinner tables are pairs: {@code <id>} plus the
 * auto-registered {@code <id>2}. A preset writes object IDs straight into the
 * object layer and does NOT run multi-tile placement, so BOTH halves have to
 * be written, with the same rotation, exactly the way vanilla's own
 * {@code BenchPreset}, {@code BedDresserPreset} and {@code DinnerTablePreset}
 * do it. The counter always sits in the direction the rotation points:
 * 0 = up, 1 = right, 2 = down, 3 = left.
 *
 * <h2>Quest anchors</h2>
 * The Sky Warden is spawned through the custom-apply hook and the anchor
 * points (warden, beacon, basket) are recorded in {@link SkywatchQuestData}
 * the moment the preset is stamped. The basket tile is left EMPTY on purpose:
 * {@code SkyLevel.healCatBasket} only ever fills an empty tile, so the preset
 * reserves it and the level places the basket.
 */
public class WardenSpirePreset extends Preset {

    /** Plot size. Applied centered, so local {@code SIZE/2} is the origin. */
    public static final int SIZE = 21;
    /**
     * How far from the centre this preset writes anything, in tiles. The
     * painter oracle in {@code SkyreachStatusCommand} excludes exactly this
     * box, so the two must never drift apart.
     */
    public static final int WRITTEN_RADIUS = SIZE / 2 - 1;   // 9

    /** The Warden's post: on the beacon's plinth, facing whoever comes in. */
    public static final int WARDEN_X = 10, WARDEN_Y = 11;
    /** The dark beacon, dead centre of the chamber (and of the whole plot). */
    public static final int BEACON_X = 10, BEACON_Y = 10;
    /** The cats' basket: the Warden's quarters, beside his bed. Left empty. */
    public static final int BASKET_X = 7, BASKET_Y = 16;

    // Rotations, named. For furniture "rotation" is the direction it faces;
    // for a multi-tile pair it is also where the second half goes.
    private static final int UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3;
    // Wall decor (paintings, wall torches) points at the wall it hangs on,
    // and uses the OPPOSITE convention: 0 = wall below, 1 = wall left,
    // 2 = wall above, 3 = wall right (PaintingObject.attachesToObject).
    private static final int WALL_BELOW = 0, WALL_LEFT = 1, WALL_ABOVE = 2, WALL_RIGHT = 3;

    public WardenSpirePreset() {
        super(SIZE, SIZE);

        final int checker = SkyRegistry.marbleCheckerID;
        final int planks = SkyRegistry.gloomwoodFloorID;
        final int paving = SkyRegistry.skyroadTileID;
        final int cloudstone = SkyCloudmarbleSet.skywayTileID;

        final int wall = SkyCloudmarbleSet.cloudmarbleWallID;
        final int door = SkyCloudmarbleSet.cloudmarbleDoorID;
        final int window = SkyCloudmarbleSet.cloudmarbleWindowID;
        final int railing = SkyCloudmarbleSet.cloudmarbleFenceID;
        final int seraph = SkyCloudmarbleSet.seraphStatueID;

        final int chair = SkyFurnitureSet.skywatchChairID;
        final int table = SkyFurnitureSet.skywatchTableID;
        final int lamp = SkyFurnitureSet.skywatchCandelabraID;
        final int desk = SkyFurnitureSet.skywatchDeskID;
        final int dresser = SkyFurnitureSet.skywatchDresserID;
        final int carpet = SkyFurnitureSet.skywatchCarpetID;
        final int chalice = SkyFurnitureSet.skywatchChaliceID;
        final int candle = SkyFurnitureSet.skywatchCandleID;
        final int tome = SkyFurnitureSet.skywatchTomeID;
        final int cloudberry = SkyFurnitureSet.pottedCloudberryID;
        // The far halves of the multi-tile pieces. Registered for us by the
        // vanilla helpers, not obtainable, and written here because a preset
        // does no multi-tile placement of its own (see the class comment).
        final int bench = SkyFurnitureSet.skywatchBenchID;
        final int bench2 = ObjectRegistry.getObjectID("skywatchbench2");
        final int bed = SkyFurnitureSet.skywatchBedID;
        final int bed2 = ObjectRegistry.getObjectID("skywatchbed2");
        final int dinner = SkyFurnitureSet.skywatchDiningTableID;
        final int dinner2 = ObjectRegistry.getObjectID("skywatchdinnertable2");

        final int streetlamp = SkyRegistry.wardenCandelabraID;
        final int lantern = ObjectRegistry.getObjectID("mistglasslantern");
        final int banner = SkyRegistry.skywatchBannerID;
        final int raven = SkyRegistry.gloomRavenStatueID;

        // Any Mistsea under the footprint becomes solid ground first (the
        // ElderHousePreset liquid-fill idiom), so the hall never half-floats.
        // Bounded to what we write, so the plot's border stays the painter's.
        this.addCustomPreApplyRectEach(1, 1, 19, 19, 0, (level, levelX, levelY, dir, blackboard) -> {
            if (level.getTile(levelX, levelY).isLiquid) {
                level.setTile(levelX, levelY, SkyRegistry.cloudturfID);
                level.setObject(levelX, levelY, 0);
            }
            return null;
        });

        // ---------------------------------------------------------- ground --
        // The grounds: paved like the forecourt outside, so the two meet with
        // no seam. Every layer is cleared, or a boulder the terrain painter
        // dropped here would end up standing in the middle of the hall.
        for (int x = 1; x <= 19; x++) {
            for (int y = 1; y <= 19; y++) {
                if (isPlotCorner(x, y)) {
                    continue;
                }
                this.setTile(x, y, paving);
                this.setObject(x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.TILE_LAYER, x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, x, y, 0);
            }
        }
        // The hall's own apron: cloudmarble paving, so the building has a
        // visible base against the forecourt's grey brick and the ground
        // matches the walls standing on it.
        this.fillTile(2, 2, 17, 17, cloudstone);
        this.fillTile(4, 4, 13, 13, planks);      // the corridor floor

        // ----------------------------------------------------- outer ring ---
        for (int i = 2; i <= 18; i++) {
            this.setObject(i, 3, wall);
            this.setObject(i, 17, wall);
        }
        for (int j = 4; j <= 16; j++) {
            this.setObject(3, j, wall);
            this.setObject(17, j, wall);
        }
        // 3x3 corner buttresses, so the ring reads as masonry and not a fence
        this.fillObject(2, 2, 3, 3, wall);
        this.fillObject(16, 2, 3, 3, wall);
        this.fillObject(2, 16, 3, 3, wall);
        this.fillObject(16, 16, 3, 3, wall);
        // Windows, two per side, evenly spaced between door and buttress
        this.setObject(6, 3, window);
        this.setObject(14, 3, window);
        this.setObject(6, 17, window);
        this.setObject(14, 17, window);
        this.setObject(3, 6, window);
        this.setObject(3, 14, window);
        this.setObject(17, 6, window);
        this.setObject(17, 14, window);
        // Doors on the four axes; the threshold under them is corridor floor
        doorway(3, 10, door, planks);
        doorway(17, 10, door, planks);
        doorway(10, 3, door, planks);
        doorway(10, 17, door, planks);

        // ----------------------------------------------------- inner ring ---
        // An octagon: a square with two-tile chamfers, so the chamber has
        // eight faces and the corridor keeps a constant width round it.
        int[][] octagon = {
            {8, 6}, {9, 6}, {11, 6}, {12, 6},
            {7, 7}, {8, 7}, {12, 7}, {13, 7},
            {6, 8}, {7, 8}, {13, 8}, {14, 8},
            {6, 9}, {14, 9}, {6, 11}, {14, 11},
            {6, 12}, {7, 12}, {13, 12}, {14, 12},
            {7, 13}, {8, 13}, {12, 13}, {13, 13},
            {8, 14}, {9, 14}, {11, 14}, {12, 14},
        };
        for (int[] t : octagon) {
            this.setObject(t[0], t[1], wall);
        }
        // The chamber floor: pale cloudstone, so the sanctum reads bright
        // against the dark corridor. Rows are the octagon's own profile.
        chamberRow(7, 9, 11, cloudstone);
        chamberRow(8, 8, 12, cloudstone);
        chamberRow(9, 7, 13, cloudstone);
        chamberRow(10, 7, 13, cloudstone);
        chamberRow(11, 7, 13, cloudstone);
        chamberRow(12, 8, 12, cloudstone);
        chamberRow(13, 9, 11, cloudstone);
        // Inner doors, on the same four axes as the outer ones
        doorway(10, 6, door, cloudstone);
        doorway(10, 14, door, cloudstone);
        doorway(6, 10, door, cloudstone);
        doorway(14, 10, door, cloudstone);

        // ------------------------------------------ the Hall of the Beacon --
        // Deliberately near-empty: the beacon on its plinth, a lamp at each of
        // the chamber's four inner corners, banners flanking the north and
        // south doors and a mistglass lantern on each flat wall.
        //
        // Marble chequer appears ONLY here, as a 3x3 monument plinth, which is
        // exactly the accent use SkyRegistry.skyplinthTileID names. Paving a
        // whole room with it is the documented way to make a chequerboard
        // swallow the screen.
        this.fillTile(9, 9, 3, 3, checker);
        this.setObject(BEACON_X, BEACON_Y, SkyRegistry.wardenBeaconOffID);
        this.setObject(8, 8, lamp);
        this.setObject(12, 8, lamp);
        this.setObject(8, 12, lamp);
        this.setObject(12, 12, lamp);
        wallDecor(9, 7, banner, WALL_ABOVE);
        wallDecor(11, 7, banner, WALL_ABOVE);
        wallDecor(9, 13, banner, WALL_BELOW);
        wallDecor(11, 13, banner, WALL_BELOW);
        wallDecor(7, 9, lantern, WALL_LEFT);
        wallDecor(7, 11, lantern, WALL_LEFT);
        wallDecor(13, 9, lantern, WALL_RIGHT);
        wallDecor(13, 11, lantern, WALL_RIGHT);

        // ------------------------------------------- NW pocket: refectory ---
        // A Skywatch dinner table with a chair on every side of it, laid out
        // the way vanilla's DinnerTablePreset does: master + counter down the
        // middle, chairs turned inward. The second east-side seat is left out
        // on purpose so the pocket's inner corner stays walkable.
        this.setObject(5, 5, dinner, DOWN);
        this.setObject(5, 6, dinner2, DOWN);
        this.setObject(5, 4, chair, DOWN);      // faces the table below it
        this.setObject(5, 7, chair, UP);
        this.setObject(4, 6, chair, RIGHT);
        this.setObject(6, 5, chair, LEFT);
        this.setObject(4, 5, lamp);

        // -------------------------------------- NE pocket: council table ---
        // Two modular tables side by side make one two-tile table that the
        // chalice and the candle stand on, with a chair on each of its four
        // sides. Only four: a fifth would seal the pocket's inner corner off
        // and leave a tile of floor nothing can reach.
        this.setObject(15, 5, table);
        this.setObject(15, 6, table);
        tableDecor(15, 5, chalice);
        tableDecor(15, 6, candle);
        this.setObject(15, 4, chair, DOWN);
        this.setObject(15, 7, chair, UP);
        this.setObject(14, 5, chair, RIGHT);
        this.setObject(16, 6, chair, LEFT);
        this.setObject(16, 5, lamp);

        // ------------------------------- SW pocket: the Warden's quarters ---
        // A carpet, a real bed a settler can be assigned to, his dresser, and
        // a desk with a chair turned to it (DeskObject is a TableObject, so
        // the chair genuinely faces a table).
        this.fillObjectLayer(ObjectLayerRegistry.TILE_LAYER, 5, 14, 3, 3, carpet);
        this.setObject(4, 14, desk, RIGHT);
        this.setObject(5, 14, chair, LEFT);
        this.setObject(4, 15, dresser, RIGHT);
        this.setObject(6, 16, bed, LEFT);       // counter goes to (5,16)
        this.setObject(5, 16, bed2, LEFT);
        this.setObject(7, 14, lamp);
        // (BASKET_X, BASKET_Y) stays empty — SkyLevel puts the cats' basket there.

        // ------------------------------------------- SE pocket: archive ----
        this.setObject(14, 16, table);
        this.setObject(15, 16, table);
        tableDecor(14, 16, tome);
        tableDecor(15, 16, cloudberry);
        this.setObject(14, 15, chair, DOWN);
        // The desk sits in the corner beside the writing tables, back to the
        // east wall, with its own chair turned to it. Putting it a tile north
        // instead walls (16,15) off behind the seating.
        this.setObject(16, 15, desk, LEFT);
        this.setObject(15, 15, chair, RIGHT);
        this.setObject(13, 16, dresser, UP);
        this.setObject(13, 14, lamp);

        // --------------------------------------------------- the galleries --
        // Benches in rows, backs to the outer wall, flanking the north and
        // south doors: the entrance hall and the gallery opposite it.
        benchPair(8, 4, bench, bench2, RIGHT);
        benchPair(11, 4, bench, bench2, RIGHT);
        benchPair(9, 16, bench, bench2, LEFT);
        benchPair(12, 16, bench, bench2, LEFT);
        // The side galleries are circulation: standing lamps on the rhythm the
        // reference plan uses. Twelve candelabra in all, the same count the
        // reference layout carries - four in the chamber, four in the side
        // galleries, one in each corner room.
        this.setObject(4, 9, lamp);
        this.setObject(4, 11, lamp);
        this.setObject(16, 9, lamp);
        this.setObject(16, 11, lamp);
        // The north and south galleries take their light off the INNER ring
        // instead, so the outer wall behind the benches stays free for the
        // banners. Without these two pairs the entrance hall is the one dark
        // room in the building.
        wallDecor(9, 5, lantern, WALL_BELOW);
        wallDecor(11, 5, lantern, WALL_BELOW);
        wallDecor(9, 15, lantern, WALL_ABOVE);
        wallDecor(11, 15, lantern, WALL_ABOVE);
        // Skywatch heraldry over the benches in the entrance hall.
        wallDecor(8, 16, banner, WALL_BELOW);
        wallDecor(12, 16, banner, WALL_BELOW);

        // ------------------------------------------------------- outside ----
        // The south front is the arrival: a railed forecourt gap between two
        // street lamps, banners either side of the grand door, a raven statue
        // on each flank. The player materialises on the pad at (10,19) — see
        // SkyOrigin.ARRIVAL_OFFSET_Y — and walks in through the door.
        wallDecor(9, 18, banner, WALL_ABOVE);
        wallDecor(11, 18, banner, WALL_ABOVE);
        wallDecor(7, 18, lantern, WALL_ABOVE);
        wallDecor(13, 18, lantern, WALL_ABOVE);
        this.setObject(6, 18, raven);
        this.setObject(14, 18, raven);
        this.setObject(8, 19, streetlamp);
        this.setObject(12, 19, streetlamp);
        this.setObject(5, 19, railing);
        this.setObject(6, 19, railing);
        this.setObject(7, 19, railing);
        this.setObject(14, 19, railing);
        this.setObject(15, 19, railing);
        // The Skywatch Gate: the permanent way home, standing IN the railing
        // line east of the arrival pad. Unbreakable (see SkySideStairwayObject)
        // — it routes each player back to the stairway they ascended from.
        this.setObject(13, 19, SkyRegistry.stairwayUpID);

        // The north front is the back of the hall: two Sky Seraphs facing out
        // over the forecourt, where nothing of the building stands behind them.
        wallDecor(9, 2, banner, WALL_BELOW);
        wallDecor(11, 2, banner, WALL_BELOW);
        wallDecor(7, 2, lantern, WALL_BELOW);
        wallDecor(13, 2, lantern, WALL_BELOW);
        this.setObject(8, 1, seraph);
        this.setObject(12, 1, seraph);
        // East and west: a lantern either side of each side door.
        wallDecor(2, 7, lantern, WALL_RIGHT);
        wallDecor(2, 13, lantern, WALL_RIGHT);
        wallDecor(18, 7, lantern, WALL_LEFT);
        wallDecor(18, 13, lantern, WALL_LEFT);

        // The Warden himself + quest bookkeeping, at stamp time. The lambda
        // receives the anchor's WORLD tile coordinates; the other quest
        // points are recorded via their fixed offsets from the warden tile.
        this.addCustomApply(WARDEN_X, WARDEN_Y, 0, (level, levelX, levelY, dir, blackboard) -> {
            if (level.isServer()) {
                SkywatchQuestData quest = SkywatchQuestData.get(level);
                quest.spireX = levelX;
                quest.spireY = levelY;
                quest.beaconX = levelX + (BEACON_X - WARDEN_X);
                quest.beaconY = levelY + (BEACON_Y - WARDEN_Y);
                quest.basketX = levelX + (BASKET_X - WARDEN_X);
                quest.basketY = levelY + (BASKET_Y - WARDEN_Y);
                quest.spirePlaced = true;

                // Does this world already have its Warden? It can: bumping
                // SkyRegistry.WORLD_GENERATION starts a FRESH Skyreach, so this
                // quest data is blank even though the player recruited (and
                // paid for) a Warden who is right now standing in their
                // settlement. SkywatchWorldData is the record that survives
                // that, and if it says yes the spire is stamped ALREADY AWAKE:
                // lit beacon, no keeper. A second keeper would be a duplicate
                // of a settler the player already owns.
                stairwaytoheaven.quest.SkywatchWorldData world =
                        stairwaytoheaven.quest.SkywatchWorldData.get(level.getServer());
                if (world != null && world.wardenRecruited) {
                    quest.recruited = true;
                    quest.recruitedAuth = world.wardenAuth;
                    quest.stage = Math.max(quest.stage, 2);
                    level.setObject(quest.beaconX, quest.beaconY, SkyRegistry.wardenBeaconOnID);
                    return null;
                }

                SkyWardenMob warden = (SkyWardenMob) MobRegistry.getMob("skywarden", level);
                level.entityManager.addMob(warden, levelX * 32 + 16, levelY * 32 + 16);
            }
            return null;
        });
    }

    /**
     * The four plot corners carry the forecourt railing's diagonal links
     * (SkyLandscape.discRing at radius 13 passes through exactly these), so
     * the preset leaves them to the landscape.
     */
    private static boolean isPlotCorner(int x, int y) {
        return (x == 1 || x == 19) && (y == 1 || y == 19);
    }

    /** A doorway: the door itself plus the floor its threshold stands on. */
    private void doorway(int x, int y, int door, int floor) {
        this.setObject(x, y, door);
        this.setTile(x, y, floor);
    }

    private void chamberRow(int y, int fromX, int toX, int floor) {
        this.fillTile(fromX, y, toX - fromX + 1, 1, floor);
    }

    /** Painting / wall torch: its own tile stays clear, the rotation names the wall. */
    private void wallDecor(int x, int y, int objectID, int rotation) {
        this.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, x, y, objectID, rotation);
    }

    /** A decoration standing on top of a modular table. */
    private void tableDecor(int x, int y, int objectID) {
        this.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, x, y, objectID);
    }

    /**
     * Both halves of a bench, with the counter in the direction the rotation
     * points — vanilla's BenchPreset writes exactly this pair. RIGHT puts the
     * bench's back against the wall above it, LEFT against the wall below.
     */
    private void benchPair(int x, int y, int bench, int bench2, int rotation) {
        this.setObject(x, y, bench, rotation);
        this.setObject(x + DX[rotation], y + DY[rotation], bench2, rotation);
    }

    /** Rotation -> unit step, the same order the multi-tile counter uses. */
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DY = {-1, 0, 1, 0};

    private void fillObjectLayer(int layer, int x, int y, int width, int height, int objectID) {
        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                this.setObjectLayer(layer, i, j, objectID);
            }
        }
    }
}
