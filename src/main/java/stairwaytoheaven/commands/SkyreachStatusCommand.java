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
import stairwaytoheaven.level.SkyLevel;

/**
 * Admin/debug command: forces the Skyreach to generate around the world origin
 * and reports terrain/biome statistics. This is what the headless integration
 * test drives (scripts/integration_test.sh) and it doubles as an in-game
 * diagnostics tool for bug reports.
 */
public class SkyreachStatusCommand extends ModularChatCommand {

    private static final int SCAN_RADIUS_TILES = 64;

    public SkyreachStatusCommand() {
        super("skyreachstatus", "Generates and inspects the Skyreach around the origin (debug)", PermissionLevel.ADMIN, false);
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
        Level level = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
        if (!(level instanceof SkyLevel)) {
            logs.add("FAIL: level for identifier \"" + SkyRegistry.SKYREACH_IDENTIFIER + "\" is " + level.getClass().getSimpleName()
                    + " (expected SkyLevel) - is the world generator registered?");
            return;
        }

        // Console commands run on the scanner thread while the server thread
        // ticks the level. The server thread's lock order is Level monitor ->
        // region locks (spawning, ensureWardenSpire); region generation from
        // THIS thread takes region locks first and then needs the Level
        // monitor inside generateRegion -> classic deadlock (reproduced once
        // critter spawning made server-side region activity constant). Taking
        // the Level monitor up front gives both threads the same lock order.
        synchronized (level) {
            runLocked(level, server, serverClient, logs);
        }
    }

    private void runLocked(Level level, Server server, ServerClient serverClient, CommandLog logs) {
        int r = SCAN_RADIUS_TILES;
        level.regionManager.ensureTilesAreLoaded(-r, -r, r, r);

        Map<String, Integer> tileCounts = new HashMap<>();
        Map<String, Integer> biomeCounts = new HashMap<>();
        Map<String, Integer> objectCounts = new HashMap<>();
        int total = 0;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                total++;
                tileCounts.merge(TileRegistry.getTileStringID(level.getTileID(x, y)), 1, Integer::sum);
                biomeCounts.merge(BiomeRegistry.getBiome(level.getBiomeID(x, y)).getStringID(), 1, Integer::sum);
                int objectID = level.getObjectID(x, y);
                if (objectID != 0) {
                    objectCounts.merge(level.getObject(x, y).getStringID(), 1, Integer::sum);
                }
            }
        }

        logs.add("Skyreach OK: class=" + level.getClass().getSimpleName()
                + " identifier=" + level.getIdentifier()
                + " dimension=" + level.getIdentifier().getOneWorldDimension()
                + " isCave=" + level.isCave);
        logs.add("Scanned " + total + " tiles in radius " + r + ":");
        tileCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> logs.add("  tile " + e.getKey() + " x" + e.getValue()));
        biomeCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> logs.add("  biome " + e.getKey() + " x" + e.getValue()));
        objectCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> logs.add("  object " + e.getKey() + " x" + e.getValue()));

        diagnosePlacement(level, logs);
        diagnoseGeneration((SkyLevel) level, logs);
        diagnoseQuest((SkyLevel) level, logs);
        locateFromPlayer((SkyLevel) level, serverClient, logs);
        logs.add("SKYREACH_STATUS_DONE");
    }

    /**
     * When a player runs the command while standing in the Skyreach, print their
     * position plus distance and compass direction to the quest landmarks.
     */
    private void locateFromPlayer(SkyLevel level, ServerClient serverClient, CommandLog logs) {
        if (serverClient == null || serverClient.playerMob == null
                || !SkyRegistry.SKYREACH_IDENTIFIER.equals(serverClient.getLevelIdentifier())) {
            logs.add("locator: run this while standing in the Skyreach to get directions to the spire and cats");
            return;
        }
        stairwaytoheaven.quest.SkywatchQuestData quest = stairwaytoheaven.quest.SkywatchQuestData.get(level);
        int px = serverClient.playerMob.getTileX();
        int py = serverClient.playerMob.getTileY();
        logs.add("you are at " + px + "," + py);
        if (quest.spirePlaced) {
            logs.add("  Warden's Spire: " + bearing(px, py, quest.spireX, quest.spireY));
        }
        if (quest.catsSpawned) {
            if (!quest.blackHome) {
                logs.add("  Siggi (black cat): " + bearing(px, py, quest.blackLairX, quest.blackLairY));
            }
            if (!quest.tabbyHome) {
                logs.add("  Peanut (tabby cat): " + bearing(px, py, quest.tabbyLairX, quest.tabbyLairY));
            }
        }
    }

    /** "312 tiles NW, at -42,-74" (screen directions: N = up, W = left). */
    private static String bearing(int px, int py, int tx, int ty) {
        int dx = tx - px;
        int dy = ty - py;
        int dist = (int) Math.round(Math.sqrt((double) dx * dx + (double) dy * dy));
        if (dist == 0) {
            return "right here (" + tx + "," + ty + ")";
        }
        String ns = dy < 0 ? "N" : "S";
        String ew = dx < 0 ? "W" : "E";
        String dir;
        if (Math.abs(dx) > 2 * Math.abs(dy)) {
            dir = ew;
        } else if (Math.abs(dy) > 2 * Math.abs(dx)) {
            dir = ns;
        } else {
            dir = ns + ew;
        }
        return dist + " tiles " + dir + ", at " + tx + "," + ty;
    }

    /** Verifies the Warden's Spire, the NPCs and the quest data integrity. */
    private void diagnoseQuest(SkyLevel level, CommandLog logs) {
        stairwaytoheaven.quest.SkywatchQuestData quest = stairwaytoheaven.quest.SkywatchQuestData.get(level);
        logs.add("quest: stage=" + quest.stage + " spirePlaced=" + quest.spirePlaced
                + " spire=" + quest.spireX + "," + quest.spireY
                + " beacon=" + quest.beaconX + "," + quest.beaconY
                + " catsSpawned=" + quest.catsSpawned
                + " blackLair=" + quest.blackLairX + "," + quest.blackLairY
                + " tabbyLair=" + quest.tabbyLairX + "," + quest.tabbyLairY);
        if (quest.spirePlaced) {
            level.regionManager.ensureTileIsLoaded(quest.beaconX, quest.beaconY);
            String beaconObj = level.getObject(quest.beaconX, quest.beaconY).getStringID();
            String spireFloor = TileRegistry.getTileStringID(level.getTileID(quest.spireX, quest.spireY));
            logs.add("spire check: beaconObject=" + beaconObj + " wardenFloor=" + spireFloor);
        }
        long wardens = 0, cats = 0;
        for (necesse.entity.mobs.Mob mob : level.entityManager.mobs) {
            String id = mob.getStringID();
            if (id.equals("skywarden")) {
                wardens++;
            } else if (id.startsWith("spirecat")) {
                cats++;
            }
        }
        logs.add("npc check: wardens=" + wardens + " cats=" + cats + " (loaded regions only)");
    }

    /** Recomputes what the painter SHOULD have placed and probes a live set/get. */
    private void diagnoseGeneration(SkyLevel level, CommandLog logs) {
        int seed = level.getWorldGenSeed();
        int r = SCAN_RADIUS_TILES;
        Map<Integer, Integer> expected = new HashMap<>();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                float islandValue = stairwaytoheaven.worldgen.SkyNoise.fbm(seed, x, y,
                        stairwaytoheaven.worldgen.SkyTerrainPainter.ISLAND_SCALE, 3);
                if (islandValue <= stairwaytoheaven.worldgen.SkyTerrainPainter.ISLAND_THRESHOLD
                        + stairwaytoheaven.worldgen.SkyTerrainPainter.ISLAND_RIM) {
                    continue;
                }
                float biomeValue = stairwaytoheaven.worldgen.SkyNoise.fbm(
                        seed + stairwaytoheaven.worldgen.SkyTerrainPainter.SALT_BIOME, x, y,
                        stairwaytoheaven.worldgen.SkyTerrainPainter.BIOME_SCALE, 2);
                boolean isStormveil = biomeValue < stairwaytoheaven.worldgen.SkyTerrainPainter.STORMVEIL_BELOW;
                boolean isAurora = biomeValue > stairwaytoheaven.worldgen.SkyTerrainPainter.AURORA_ABOVE;
                boolean isRockPatch = stairwaytoheaven.worldgen.SkyNoise.fbm(
                        seed + stairwaytoheaven.worldgen.SkyTerrainPainter.SALT_ROCK_PATCH, x, y,
                        stairwaytoheaven.worldgen.SkyTerrainPainter.ROCK_PATCH_SCALE, 2)
                        > stairwaytoheaven.worldgen.SkyTerrainPainter.ROCK_PATCH_THRESHOLD;
                int objectID = stairwaytoheaven.worldgen.SkyTerrainPainter.rollObject(seed, x, y, isStormveil, isAurora, isRockPatch);
                if (objectID != 0) {
                    expected.merge(objectID, 1, Integer::sum);
                }
            }
        }
        logs.add("painter expectation (seed=" + seed + "):");
        expected.forEach((id, n) -> logs.add("  expected " + necesse.engine.registries.ObjectRegistry.getObject(id).getStringID() + " x" + n));
    }

    /** Reports why natural objects would be rejected on sky ground, and the registered IDs. */
    private void diagnosePlacement(Level level, CommandLog logs) {
        logs.add("IDs: stormcrystal=" + SkyRegistry.stormCrystalID + " aurorabloom=" + SkyRegistry.auroraBloomID
                + " skyreeds=" + SkyRegistry.skyreedsID + " skystonerock=" + SkyRegistry.skystoneRockID
                + " aetheriumrock=" + SkyRegistry.aetheriumRockID);
        Map<String, Integer> crystalErrors = new HashMap<>();
        Map<String, Integer> reedErrors = new HashMap<>();
        int checked = 0;
        for (int x = -SCAN_RADIUS_TILES; x <= SCAN_RADIUS_TILES && checked < 300; x++) {
            for (int y = -SCAN_RADIUS_TILES; y <= SCAN_RADIUS_TILES && checked < 300; y++) {
                if (level.getTileID(x, y) == SkyRegistry.mistseaID || level.getObjectID(x, y) != 0) {
                    continue;
                }
                checked++;
                String ce = necesse.engine.registries.ObjectRegistry.getObject(SkyRegistry.stormCrystalID)
                        .canPlace(level, 0, x, y, 0, false, false);
                crystalErrors.merge(ce == null ? "OK" : ce, 1, Integer::sum);
                String re = necesse.engine.registries.ObjectRegistry.getObject(SkyRegistry.skyreedsID)
                        .canPlace(level, 0, x, y, 0, false, false);
                reedErrors.merge(re == null ? "OK" : re, 1, Integer::sum);
            }
        }
        logs.add("placement check on " + checked + " free land tiles:");
        crystalErrors.forEach((k, v) -> logs.add("  stormcrystal: " + k + " x" + v));
        reedErrors.forEach((k, v) -> logs.add("  skyreeds: " + k + " x" + v));
    }
}
