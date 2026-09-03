package stairwaytoheaven.commands;

import java.util.HashMap;
import java.util.Map;

import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.level.maps.Level;
import java.awt.Point;

import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.worldgen.CrookedHousePreset;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.RealmLanding;

/**
 * Admin/debug: forces the Veil's ground to generate and reports
 * terrain/biome/object statistics (Level-first lock order, see
 * SkyreachStatusCommand for the deadlock story).
 *
 * <p><b>The Veil is not a world any more.</b>
 * {@code docs/PLAN_ONE_PLANE.md} retired {@code veil2} and
 * {@code WORLD_DESIGN} §41.5 moved its ground onto the plane: Gloomfen and
 * Ashen Reach into the GHOST band, the Beetlefreak Hollows into the CROOKED
 * band. So this command samples two places instead of one origin -- the fen
 * where the Ghost Realm is deepest, and the Hollows out in Crooked Beyond,
 * which is also where the Crooked House stamps.
 */
public class VeilStatusCommand extends ModularChatCommand {

    private static final int SCAN_RADIUS_TILES = 48;

    /**
     * Radius of the second, wide survey.
     *
     * The close scan above is 97x97 tiles -- far too small to say anything
     * about content that is deliberately rare. Beetlefreak Hollows cover a few
     * percent of ground and the Crooked House is placed at
     * {@link stairwaytoheaven.worldgen.CrookedHouseWorldPreset#HOUSES_PER_REGION}
     * per 16x16 region, so proving either one needs an area big enough that
     * "zero" is a real failure rather than ordinary luck. At this radius the
     * survey covers about 1000 regions, where the configured rate expects a
     * handful of houses.
     */
    private static final int SURVEY_RADIUS_TILES = 256;

    public VeilStatusCommand() {
        super("veilstatus", "Generates and inspects the Veil around the origin (debug)", PermissionLevel.ADMIN, false);
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
        Level level = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
        if (!(level instanceof SkyLevel)) {
            logs.add("FAIL: level for identifier \"" + SkyRegistry.SKYREACH_IDENTIFIER + "\" is "
                    + (level == null ? "null" : level.getClass().getSimpleName()) + " (expected SkyLevel)");
            return;
        }
        int seed = ((SkyLevel) level).getWorldGenSeed();
        // The fen, in the Ghost band (§41.5) -- and it has to be ACTUAL fen.
        // The band's landing tile is Ghost ground; the fen is about a fifth of
        // it, so a 97x97 scan centred on the landing finds none on most seeds.
        // The first run of this command after the one-plane move failed exactly
        // that way, which is what this search exists to stop.
        Point fen = findFen(seed, RealmLanding.find(seed, RealmDepth.REALM_GHOST, 0, 0));
        // The Hollows, out in the Crooked band (§41.5) -- where the house is.
        // No search needed: the survey below is 513x513, wide enough that the
        // Hollows' few percent of ground cannot hide in it.
        Point hollows = RealmLanding.find(seed, RealmDepth.REALM_CROOKED, 0, 0);
        synchronized (level) {
            int r = SCAN_RADIUS_TILES;
            level.regionManager.ensureTilesAreLoaded(fen.x - r, fen.y - r, fen.x + r, fen.y + r);
            Map<String, Integer> tiles = new HashMap<>();
            Map<String, Integer> biomes = new HashMap<>();
            Map<String, Integer> objects = new HashMap<>();
            for (int x = fen.x - r; x <= fen.x + r; x++) {
                for (int y = fen.y - r; y <= fen.y + r; y++) {
                    tiles.merge(TileRegistry.getTileStringID(level.getTileID(x, y)), 1, Integer::sum);
                    biomes.merge(BiomeRegistry.getBiome(level.getBiomeID(x, y)).getStringID(), 1, Integer::sum);
                    if (level.getObjectID(x, y) != 0) {
                        objects.merge(level.getObject(x, y).getStringID(), 1, Integer::sum);
                    }
                }
            }
            logs.add("Veil ground OK: class=" + level.getClass().getSimpleName()
                    + " identifier=" + level.getIdentifier()
                    + " dimension=" + level.getIdentifier().getOneWorldDimension()
                    + " isCave=" + level.isCave
                    + " fenSampledAt=" + fen.x + "," + fen.y
                    + " hollowSurveyAt=" + hollows.x + "," + hollows.y);
            tiles.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(e -> logs.add("  tile " + e.getKey() + " x" + e.getValue()));
            biomes.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(e -> logs.add("  biome " + e.getKey() + " x" + e.getValue()));
            objects.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(e -> logs.add("  object " + e.getKey() + " x" + e.getValue()));
        }
        surveyHollows(server, level, hollows, logs);
        logs.add("VEIL_STATUS_DONE");
    }

    /**
     * The nearest tile of the Veil's fen to a point in the Ghost band.
     *
     * <p>A square spiral in 12-tile steps, capped at ~1200 tiles so the scan
     * stays inside the Ghost band it started in. Pure noise, no world state, so
     * it agrees with what the painter wrote. Falls back to the point it was
     * given, which then reports "no fen here" honestly rather than silently
     * scanning somewhere else.
     */
    private static Point findFen(int seed, Point near) {
        int originX = stairwaytoheaven.worldgen.SkyOrigin.originX(seed);
        int originY = stairwaytoheaven.worldgen.SkyOrigin.originY(seed);
        for (int ring = 0; ring <= 100; ring++) {
            for (int i = -ring; i <= ring; i++) {
                for (int j = -ring; j <= ring; j++) {
                    if (ring > 0 && Math.abs(i) != ring && Math.abs(j) != ring) {
                        continue; // only the ring's edge
                    }
                    int x = near.x + i * 12;
                    int y = near.y + j * 12;
                    if (RealmDepth.realmAt(seed, x, y, originX, originY) != RealmDepth.REALM_GHOST) {
                        continue;
                    }
                    if (!stairwaytoheaven.realms.ghost.GhostTerrainPainter.isFen(seed, x, y)) {
                        continue;
                    }
                    if (!stairwaytoheaven.worldgen.SkyTerrainPainter.isOpenGround(
                            seed, x, y, originX, originY)) {
                        continue;
                    }
                    return new Point(x, y);
                }
            }
        }
        return near;
    }

    /**
     * The wide survey: how much of the Veil is Hollows, and did the Crooked
     * House actually stamp?
     *
     * Two independent numbers on purpose. The tile share comes from the terrain
     * the painter actually wrote, so it catches a biome that is registered but
     * never painted. The house count comes from counting beetlefreak wall
     * objects and dividing by the wall count of one house, so it catches a
     * world preset that is registered but never places -- the exact failure
     * that a compiling, well-formed preset hides.
     */
    private void surveyHollows(Server server, Level level, Point at, CommandLog logs) {
        final int r = SURVEY_RADIUS_TILES;
        final int hollowTile = SkyRegistry.beetlefreakID;
        final int wall = SkyRegistry.beetleWallID;
        final int hollowBiome = SkyRegistry.beetlefreakHollow.getID();

        long start = System.currentTimeMillis();
        int hollowTiles = 0;
        int hollowBiomeTiles = 0;
        int walls = 0;
        int doors = 0;
        int windows = 0;
        int land = 0;

        synchronized (level) {
            // Loaded in strips: one ensureTilesAreLoaded over the whole square
            // asks the region manager for ~1000 regions at once.
            for (int y0 = at.y - r; y0 <= at.y + r; y0 += 64) {
                int y1 = Math.min(y0 + 63, at.y + r);
                level.regionManager.ensureTilesAreLoaded(at.x - r, y0, at.x + r, y1);
                for (int x = at.x - r; x <= at.x + r; x++) {
                    for (int y = y0; y <= y1; y++) {
                        int tile = level.getTileID(x, y);
                        // Crooked Beyond's sea is the Spill; the fen's is
                        // murkwater. Either one is "not land" out here.
                        if (tile != SkyRegistry.murkwaterID
                                && !necesse.engine.registries.TileRegistry.getTile(tile).isLiquid) {
                            land++;
                        }
                        if (tile == hollowTile) {
                            hollowTiles++;
                        }
                        if (level.getBiomeID(x, y) == hollowBiome) {
                            hollowBiomeTiles++;
                        }
                        int object = level.getObjectID(x, y);
                        if (object == wall) {
                            walls++;
                        } else if (object == SkyRegistry.beetleDoorClosedID
                                || object == SkyRegistry.beetleDoorOpenID) {
                            doors++;
                        } else if (object == SkyRegistry.beetleWindowID) {
                            windows++;
                        }
                    }
                }
            }
        }

        int total = (2 * r + 1) * (2 * r + 1);
        logs.add(String.format(
                "hollow survey: radius=%d tiles=%d land=%d hollowGround=%d (%.2f%% of land) "
                        + "hollowBiome=%d (%.2f%%) in %dms",
                r, total, land, hollowTiles,
                land == 0 ? 0.0F : 100.0F * hollowTiles / land,
                hollowBiomeTiles, 100.0F * hollowBiomeTiles / total,
                System.currentTimeMillis() - start));
        logs.add("crooked house survey: beetlewall=" + walls + " beetledoor=" + doors
                + " beetlewindow=" + windows + " (one house = "
                + CROOKED_HOUSE_WALLS + " walls, 1 door, " + CROOKED_HOUSE_WINDOWS + " windows)");

        // Per-house completeness. The aggregate counts above cannot tell a
        // house that stamped wrong from a house that stamped half-outside the
        // surveyed square -- the first run showed 96 walls (exactly two
        // houses) but only 2 of the expected 6 windows, and there was no way
        // to tell which explanation it was. Anchoring on each door and
        // re-counting its own 15x13 box answers that directly.
        synchronized (level) {
            for (int y = at.y - r; y <= at.y + r; y++) {
                for (int x = at.x - r; x <= at.x + r; x++) {
                    int object = level.getObjectID(x, y);
                    if (object != SkyRegistry.beetleDoorClosedID
                            && object != SkyRegistry.beetleDoorOpenID) {
                        continue;
                    }
                    // The door sits at plan (7, 11), so the plan origin is here.
                    int ox = x - DOOR_PLAN_X;
                    int oy = y - DOOR_PLAN_Y;
                    boolean fullyInside = ox >= at.x - r && oy >= at.y - r
                            && ox + CrookedHousePreset.WIDTH - 1 <= at.x + r
                            && oy + CrookedHousePreset.HEIGHT - 1 <= at.y + r;
                    int w = 0, wi = 0, floor = 0;
                    for (int px = 0; px < CrookedHousePreset.WIDTH; px++) {
                        for (int py = 0; py < CrookedHousePreset.HEIGHT; py++) {
                            int o = level.getObjectID(ox + px, oy + py);
                            if (o == SkyRegistry.beetleWallID) {
                                w++;
                            } else if (o == SkyRegistry.beetleWindowID) {
                                wi++;
                            }
                            if (level.getTileID(ox + px, oy + py) == SkyRegistry.gloomwoodFloorID) {
                                floor++;
                            }
                        }
                    }
                    logs.add("  house at " + ox + "," + oy + ": walls=" + w + "/" + CROOKED_HOUSE_WALLS
                            + " windows=" + wi + "/" + CROOKED_HOUSE_WINDOWS
                            + " plankFloor=" + floor
                            + (fullyInside ? "" : " (footprint crosses the survey edge)"));
                }
            }
        }
    }

    /** Where the door sits in {@link CrookedHousePreset#PLAN}. */
    private static final int DOOR_PLAN_X = doorPlanX();
    private static final int DOOR_PLAN_Y = doorPlanY();

    private static int doorPlanX() {
        for (String row : CrookedHousePreset.PLAN) {
            int i = row.indexOf('D');
            if (i >= 0) {
                return i;
            }
        }
        return 0;
    }

    private static int doorPlanY() {
        for (int y = 0; y < CrookedHousePreset.PLAN.length; y++) {
            if (CrookedHousePreset.PLAN[y].indexOf('D') >= 0) {
                return y;
            }
        }
        return 0;
    }

    /** Wall and window counts of one Crooked House, from its own plan. */
    private static final int CROOKED_HOUSE_WALLS = countPlan('#');
    private static final int CROOKED_HOUSE_WINDOWS = countPlan('O');

    private static int countPlan(char c) {
        int n = 0;
        for (String row : CrookedHousePreset.PLAN) {
            for (int i = 0; i < row.length(); i++) {
                if (row.charAt(i) == c) {
                    n++;
                }
            }
        }
        return n;
    }
}
