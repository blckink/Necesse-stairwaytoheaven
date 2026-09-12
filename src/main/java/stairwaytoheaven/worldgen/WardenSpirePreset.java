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
 * The Warden's Spire: the Skywatch cathedral at the centre of the Skyreach.
 *
 * <h2>The plan</h2>
 * A cruciform hall on a 33x33 plot, built to the cathedral the user supplied
 * as a screenshot on 2026-09-12. Four arms of equal reach meet at a crossing;
 * every arm ends in a chamfered tip, so the outside silhouette steps in twice
 * instead of stopping at a flat gable:
 *
 * <pre>
 *   the crossing   the beacon on its chequer plinth, the chequered corner
 *                  quarters around it, the Warden at his post
 *   north arm      the choir: pews either side of the runner, and the apse
 *                  with its candle staffel and two Sky Seraphs
 *   south arm      the nave: three ranks of pews a side, the grand door,
 *                  and the arrival apron beyond it
 *   west arm       the refectory, and the Warden's own quarters
 *   east arm       the council table, and the archive
 * </pre>
 *
 * The processional runner is a cross of Skywatch carpet, three tiles wide,
 * laid from door to door along both axes: it is what makes the building read
 * as a cathedral from the map rather than as four corridors. Nothing stands
 * on it.
 *
 * <h2>Geometry</h2>
 * Everything is written in offsets from the plot centre {@link #C}, because
 * every consumer of this preset works in offsets too: the quest anchors, the
 * arrival pad ({@code SkyOrigin.ARRIVAL_OFFSET_Y}), and the painter oracle in
 * {@code SkyreachStatusCommand}, which excludes exactly the box of
 * {@link #WRITTEN_RADIUS} tiles around the centre. Nothing is written outside
 * that box, so the forecourt {@code SkyLandscape} composes around the plot —
 * chequered inlay, lamp ring, railing — begins where this preset stops.
 * Growing the building from 21 to 33 moved those three rings outward by the
 * same amount; see the HUB_ constants in {@code SkyLandscape}.
 *
 * <h2>Walls</h2>
 * The wall ring is derived, not typed: a tile is a wall when it is outside
 * the hall and touches the inside in any of the EIGHT directions. The eight
 * matters — with four, each chamfer step leaves the two wall tiles meeting at
 * a bare diagonal, which looks like a crack in the masonry.
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

    /** Plot size. Applied centered, so local {@link #C} is the origin. */
    public static final int SIZE = 33;
    /** The plot centre, in local coordinates. */
    public static final int C = SIZE / 2;                    // 16
    /**
     * How far from the centre this preset writes anything, in tiles. The
     * painter oracle in {@code SkyreachStatusCommand} excludes exactly this
     * box, so the two must never drift apart.
     */
    public static final int WRITTEN_RADIUS = SIZE / 2 - 1;   // 15

    /** How far an arm reaches from the centre, to the inside of its tip. */
    private static final int ARM_LENGTH = 13;
    /** Half width of an arm's interior: an arm is nine tiles across inside. */
    private static final int ARM_HALF = 5;
    /** The wall ring stands one tile beyond the hall. */
    private static final int DOOR_RING = ARM_LENGTH + 1;     // 14

    /** The Warden's post: in front of the altar, facing the nave. */
    public static final int WARDEN_X = C, WARDEN_Y = C + 1;
    /** The dark beacon, dead centre of the crossing (and of the whole plot). */
    public static final int BEACON_X = C, BEACON_Y = C;
    /** The cats' basket: the Warden's quarters in the west arm. Left empty. */
    public static final int BASKET_X = C - 9, BASKET_Y = C + 4;

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
        final int bookshelf = SkyFurnitureSet.skywatchBookshelfID;
        final int cabinet = SkyFurnitureSet.skywatchCabinetID;
        final int display = SkyFurnitureSet.skywatchDisplayID;
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
        this.addCustomPreApplyRectEach(1, 1, SIZE - 2, SIZE - 2, 0,
                (level, levelX, levelY, dir, blackboard) -> {
            if (level.getTile(levelX, levelY).isLiquid) {
                level.setTile(levelX, levelY, SkyRegistry.cloudturfID);
                level.setObject(levelX, levelY, 0);
            }
            return null;
        });

        // --------------------------------------------------- the footprint --
        // inside = the hall, ring = its masonry. Derived once, used by every
        // section below, so a change to the cross shape can never leave the
        // walls, the floor and the apron disagreeing about where the hall is.
        boolean[][] inside = new boolean[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                inside[x][y] = isHall(x - C, y - C);
            }
        }

        // ------------------------------------------------------- the apron --
        // Paved ground in a two-tile band around the hall, so the cathedral
        // stands on a base rather than in the grass, and the forecourt meets
        // it with no seam. Everything is cleared first, or a boulder the
        // terrain painter dropped here would stand inside the building.
        for (int x = 1; x < SIZE - 1; x++) {
            for (int y = 1; y < SIZE - 1; y++) {
                if (!inside[x][y] && !near(inside, x, y, 3)) {
                    continue;                       // untouched: painter's job
                }
                this.setTile(x, y, paving);
                this.setObject(x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.TILE_LAYER, x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, x, y, 0);
            }
        }

        // ------------------------------------------------- floor and walls --
        for (int x = 1; x < SIZE - 1; x++) {
            for (int y = 1; y < SIZE - 1; y++) {
                if (inside[x][y]) {
                    this.setTile(x, y, cloudstone);
                } else if (near(inside, x, y, 1)) {
                    // Outside, touching the inside in any of eight directions.
                    this.setTile(x, y, cloudstone);
                    this.setObject(x, y, wall);
                }
            }
        }

        // The processional runner: a cross of carpet three tiles wide, door
        // to door on both axes. Laid on the tile layer, so it is floor and
        // nothing ever stands on it.
        for (int d = -ARM_LENGTH; d <= ARM_LENGTH; d++) {
            for (int a = -1; a <= 1; a++) {
                carpetAt(carpet, a, d);
                carpetAt(carpet, d, a);
            }
        }
        // The four corner quarters of the crossing, chequered — the accent the
        // screenshot puts around its altar. 3x3 each, never a whole room.
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int i = 3; i <= 5; i++) {
                    for (int j = 3; j <= 5; j++) {
                        this.setTile(C + sx * i, C + sy * j, checker);
                    }
                }
            }
        }
        // Gloomwood boards under the four working rooms in the side arms, so
        // the lived-in halves read warm against the pale stone of the church.
        fillRel(planks, -12, -4, -7, -2);
        fillRel(planks, -12, 2, -7, 4);
        fillRel(planks, 7, -4, 12, -2);
        fillRel(planks, 7, 2, 12, 4);

        // ------------------------------------------------ doors and windows --
        // One door per arm, on the arm's own axis, where the runner runs out
        // of the building. Single leaves, never a bank of them: a door only
        // stands where a way goes through it.
        doorway(C, C - DOOR_RING, door, cloudstone);
        doorway(C, C + DOOR_RING, door, cloudstone);
        doorway(C - DOOR_RING, C, door, cloudstone);
        doorway(C + DOOR_RING, C, door, cloudstone);
        // Windows sit mid-run in the arms' long flanks. Between the crossing
        // and the chamfers those flanks are straight for six tiles, so every
        // one of these has wall on both sides and none lands in a corner —
        // a window in a corner is silently deleted (chapter 01, §0.3).
        for (int d : new int[]{-10, -8, 8, 10}) {
            this.setObject(C - ARM_HALF - 1, C + d, window);
            this.setObject(C + ARM_HALF + 1, C + d, window);
            this.setObject(C + d, C - ARM_HALF - 1, window);
            this.setObject(C + d, C + ARM_HALF + 1, window);
        }

        // ------------------------------------- the crossing: the high altar --
        // Deliberately near-empty. The beacon is a 180 light standing in a
        // room that is already ringed by four candelabra; the chamber once
        // carried thirty light sources and measured brighter than noon.
        fillRel(checker, -1, -1, 1, 1);
        this.setObject(BEACON_X, BEACON_Y, SkyRegistry.wardenBeaconOffID);
        rel(lamp, -2, -2); rel(lamp, 2, -2); rel(lamp, -2, 2); rel(lamp, 2, 2);

        // ------------------------------------- north arm: choir and apse ----
        // Pews either side of the runner, then the apse: two Sky Seraphs
        // flanking the tip, a candle staffel stepping in with the chamfer.
        for (int dy : new int[]{-9, -7}) {
            benchPairRel(bench, bench2, -4, dy, RIGHT);
            benchPairRel(bench, bench2, 3, dy, RIGHT);
        }
        rel(seraph, -3, -12); rel(seraph, 3, -12);
        rel(lamp, -3, -10); rel(lamp, 3, -10);
        rel(lamp, -2, -12); rel(lamp, 2, -12);
        wallDecorRel(banner, -5, -9, WALL_LEFT);
        wallDecorRel(banner, 5, -9, WALL_RIGHT);

        // ------------------------------------------- south arm: the nave ----
        // Three ranks of pews a side, the grand door at the end of the runner.
        for (int dy : new int[]{7, 9, 11}) {
            benchPairRel(bench, bench2, -4, dy, RIGHT);
            benchPairRel(bench, bench2, 3, dy, RIGHT);
        }
        rel(lamp, -3, 6); rel(lamp, 3, 6);
        rel(cloudberry, -2, 12); rel(cloudberry, 2, 12);
        wallDecorRel(banner, -2, 13, WALL_BELOW);
        wallDecorRel(banner, 2, 13, WALL_BELOW);
        wallDecorRel(lantern, -3, 13, WALL_BELOW);
        wallDecorRel(lantern, 3, 13, WALL_BELOW);

        // -------------------------------- west arm, north half: refectory ---
        // A Skywatch dinner table with a chair on every side, laid out the way
        // vanilla's DinnerTablePreset does: master + counter, chairs inward.
        relRot(dinner, -10, -4, DOWN);
        relRot(dinner2, -10, -3, DOWN);
        relRot(chair, -10, -5, DOWN);
        relRot(chair, -11, -4, RIGHT);
        relRot(chair, -9, -3, LEFT);
        rel(lamp, -12, -4);
        rel(cabinet, -12, -2);
        wallDecorRel(lantern, -10, -5, WALL_ABOVE);

        // ------------------------- west arm, south half: Warden's quarters ---
        fillLayerRel(ObjectLayerRegistry.TILE_LAYER, carpet, -11, 3, -9, 5);
        relRot(desk, -12, 2, RIGHT);
        relRot(chair, -11, 2, LEFT);
        relRot(dresser, -12, 3, RIGHT);
        relRot(bed, -11, 4, LEFT);          // counter goes one tile west
        relRot(bed2, -12, 4, LEFT);
        rel(lamp, -7, 5);
        // (BASKET_X, BASKET_Y) stays empty — SkyLevel puts the cats' basket there.

        // ------------------------------ east arm, north half: council table --
        // Two modular tables make one two-tile board, a chair on each side.
        rel(table, 10, -4); rel(table, 10, -3);
        tableDecorRel(chalice, 10, -4);
        tableDecorRel(candle, 10, -3);
        relRot(chair, 10, -5, DOWN);
        relRot(chair, 9, -4, RIGHT);
        relRot(chair, 11, -3, LEFT);
        rel(lamp, 12, -4);
        rel(display, 12, -2);
        wallDecorRel(lantern, 10, -5, WALL_ABOVE);

        // ----------------------------------- east arm, south half: archive ---
        rel(table, 10, 4); rel(table, 11, 4);
        tableDecorRel(tome, 10, 4);
        tableDecorRel(cloudberry, 11, 4);
        relRot(chair, 10, 3, DOWN);
        relRot(desk, 12, 3, LEFT);
        relRot(chair, 11, 3, RIGHT);
        rel(bookshelf, 12, 4);
        rel(lamp, 7, 5);

        // ------------------------------------------------------- outside ----
        // The south front is the arrival: the player materialises on the apron
        // two tiles below the grand door (SkyOrigin.ARRIVAL_OFFSET_Y) and walks
        // straight up the runner. Street lamps flank the approach — they are
        // what makes the cathedral readable at night from a distance — and a
        // raven statue stands on each flank of the door.
        rel(raven, -5, 14); rel(raven, 5, 14);
        rel(streetlamp, -3, 15); rel(streetlamp, 3, 15);
        rel(railing, -6, 15); rel(railing, -5, 15);
        rel(railing, 5, 15); rel(railing, 6, 15);
        // The Skywatch Gate: the permanent way home, on the apron beside the
        // arrival pad. Unbreakable (see SkySideStairwayObject) — it routes each
        // player back to the stairway they ascended from. Two tiles clear of
        // the door axis, so it never stands in the way in.
        rel(SkyRegistry.stairwayUpID, 4, 15);

        // The north front is the back of the choir: seraphs facing out over the
        // forecourt, where nothing of the building stands behind them.
        rel(seraph, -5, -14); rel(seraph, 5, -14);
        wallDecorRel(banner, -2, -15, WALL_BELOW);
        wallDecorRel(banner, 2, -15, WALL_BELOW);
        // East and west: a lantern either side of each side door, outside.
        wallDecorRel(lantern, -15, -2, WALL_RIGHT);
        wallDecorRel(lantern, -15, 2, WALL_RIGHT);
        wallDecorRel(lantern, 15, -2, WALL_LEFT);
        wallDecorRel(lantern, 15, 2, WALL_LEFT);

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
     * The cross, in offsets from the centre: two arms crossing at right
     * angles, each chamfered twice at its tip so the outer wall steps in
     * rather than ending flat.
     */
    private static boolean isHall(int dx, int dy) {
        return arm(dx, dy) || arm(dy, dx);
    }

    /** One arm: {@code across} is the width axis, {@code along} its length. */
    private static boolean arm(int across, int along) {
        int a = Math.abs(across), l = Math.abs(along);
        if (l > ARM_LENGTH || a > ARM_HALF) {
            return false;
        }
        if (l == ARM_LENGTH) {
            return a <= ARM_HALF - 2;       // the tip: five tiles across
        }
        if (l == ARM_LENGTH - 1) {
            return a <= ARM_HALF - 1;       // the first step: seven
        }
        return true;
    }

    /**
     * Is any tile within {@code reach} of (x,y) — Chebyshev, so diagonals
     * count — part of the hall? Eight-way is what seals the chamfer steps;
     * with four, every step would meet its neighbour at a bare diagonal.
     */
    private static boolean near(boolean[][] inside, int x, int y, int reach) {
        for (int i = Math.max(0, x - reach); i <= Math.min(SIZE - 1, x + reach); i++) {
            for (int j = Math.max(0, y - reach); j <= Math.min(SIZE - 1, y + reach); j++) {
                if (inside[i][j]) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A doorway: the door itself plus the floor its threshold stands on. */
    private void doorway(int x, int y, int door, int floor) {
        this.setObject(x, y, door);
        this.setTile(x, y, floor);
    }

    // ---- offset helpers: everything above speaks in tiles from the centre --

    private void rel(int objectID, int dx, int dy) {
        this.setObject(C + dx, C + dy, objectID);
    }

    private void relRot(int objectID, int dx, int dy, int rotation) {
        this.setObject(C + dx, C + dy, objectID, rotation);
    }

    private void carpetAt(int carpet, int dx, int dy) {
        this.setObjectLayer(ObjectLayerRegistry.TILE_LAYER, C + dx, C + dy, carpet);
    }

    private void fillRel(int tileID, int fromDx, int fromDy, int toDx, int toDy) {
        this.fillTile(C + fromDx, C + fromDy, toDx - fromDx + 1, toDy - fromDy + 1, tileID);
    }

    private void fillLayerRel(int layer, int objectID, int fromDx, int fromDy, int toDx, int toDy) {
        for (int x = C + fromDx; x <= C + toDx; x++) {
            for (int y = C + fromDy; y <= C + toDy; y++) {
                this.setObjectLayer(layer, x, y, objectID);
            }
        }
    }

    /** Painting / wall torch: its own tile stays clear, the rotation names the wall. */
    private void wallDecorRel(int objectID, int dx, int dy, int rotation) {
        this.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, C + dx, C + dy, objectID, rotation);
    }

    /** A decoration standing on top of a modular table. */
    private void tableDecorRel(int objectID, int dx, int dy) {
        this.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, C + dx, C + dy, objectID);
    }

    /**
     * Both halves of a bench, with the counter in the direction the rotation
     * points — vanilla's BenchPreset writes exactly this pair. RIGHT puts the
     * bench's back against the wall above it, LEFT against the wall below.
     */
    private void benchPairRel(int bench, int bench2, int dx, int dy, int rotation) {
        this.setObject(C + dx, C + dy, bench, rotation);
        this.setObject(C + dx + DX[rotation], C + dy + DY[rotation], bench2, rotation);
    }

    /** Rotation -> unit step, the same order the multi-tile counter uses. */
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DY = {-1, 0, 1, 0};
}
