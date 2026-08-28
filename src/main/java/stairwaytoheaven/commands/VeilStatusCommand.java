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
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.VeilLevel;
import stairwaytoheaven.worldgen.CrookedHousePreset;

/**
 * Admin/debug: forces the Veil to generate around the origin and reports
 * terrain/biome/object statistics (Level-first lock order, see
 * SkyreachStatusCommand for the deadlock story).
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
        Level level = server.world.getLevel(SkyRegistry.VEIL_IDENTIFIER);
        if (!(level instanceof VeilLevel)) {
            logs.add("FAIL: level for identifier \"" + SkyRegistry.VEIL_IDENTIFIER + "\" is "
                    + level.getClass().getSimpleName() + " (expected VeilLevel)");
            return;
        }
        synchronized (level) {
            int r = SCAN_RADIUS_TILES;
            level.regionManager.ensureTilesAreLoaded(-r, -r, r, r);
            Map<String, Integer> tiles = new HashMap<>();
            Map<String, Integer> biomes = new HashMap<>();
            Map<String, Integer> objects = new HashMap<>();
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    tiles.merge(TileRegistry.getTileStringID(level.getTileID(x, y)), 1, Integer::sum);
                    biomes.merge(BiomeRegistry.getBiome(level.getBiomeID(x, y)).getStringID(), 1, Integer::sum);
                    if (level.getObjectID(x, y) != 0) {
                        objects.merge(level.getObject(x, y).getStringID(), 1, Integer::sum);
                    }
                }
            }
            logs.add("Veil OK: class=" + level.getClass().getSimpleName()
                    + " identifier=" + level.getIdentifier()
                    + " dimension=" + level.getIdentifier().getOneWorldDimension()
                    + " isCave=" + level.isCave);
            tiles.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(e -> logs.add("  tile " + e.getKey() + " x" + e.getValue()));
            biomes.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(e -> logs.add("  biome " + e.getKey() + " x" + e.getValue()));
            objects.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(e -> logs.add("  object " + e.getKey() + " x" + e.getValue()));
        }
        surveyHollows(server, level, logs);
        logs.add("VEIL_STATUS_DONE");
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
    private void surveyHollows(Server server, Level level, CommandLog logs) {
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
            for (int y0 = -r; y0 <= r; y0 += 64) {
                int y1 = Math.min(y0 + 63, r);
                level.regionManager.ensureTilesAreLoaded(-r, y0, r, y1);
                for (int x = -r; x <= r; x++) {
                    for (int y = y0; y <= y1; y++) {
                        int tile = level.getTileID(x, y);
                        if (tile != SkyRegistry.murkwaterID) {
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
            for (int y = -r; y <= r; y++) {
                for (int x = -r; x <= r; x++) {
                    int object = level.getObjectID(x, y);
                    if (object != SkyRegistry.beetleDoorClosedID
                            && object != SkyRegistry.beetleDoorOpenID) {
                        continue;
                    }
                    // The door sits at plan (7, 11), so the plan origin is here.
                    int ox = x - DOOR_PLAN_X;
                    int oy = y - DOOR_PLAN_Y;
                    boolean fullyInside = ox >= -r && oy >= -r
                            && ox + CrookedHousePreset.WIDTH - 1 <= r
                            && oy + CrookedHousePreset.HEIGHT - 1 <= r;
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
