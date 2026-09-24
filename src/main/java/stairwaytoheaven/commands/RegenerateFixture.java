package stairwaytoheaven.commands;

import java.awt.Point;

import necesse.engine.commands.CommandLog;
import necesse.engine.network.server.Server;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyOrigin;

/**
 * {@code /swhreset fixture [check]} — the test fixture {@code scripts/regenerate_check.sh}
 * drives. NOT for a real world: it writes progress nobody earned.
 *
 * <p>Bare: puts a marker far out in the sky and one near the surface spawn,
 * and records progress as if the chain had been played (the Warden recruited,
 * the Skyreach key earned and its portals unlocked). {@code check}: reports
 * whether each marker is still there and what the progress records say, and
 * changes nothing.
 *
 * <p>A marker is a lava tile with a storage box on it. Neither is anything
 * the sky's worldgen paints, so "present=0" after a regenerate means the
 * ground really was generated again, and "present=1" on the surface means it
 * was left alone.
 */
final class RegenerateFixture {

    private RegenerateFixture() {
    }

    /** Far outside the spire's 512-tile retrofit box, in the Skyreach band. */
    static final int SKY_MARKER_DX = 1400;
    static final int SKY_MARKER_DY = 300;
    /** A few tiles off the surface spawn. */
    static final int SURFACE_MARKER_DX = 6;
    static final int SURFACE_MARKER_DY = 6;

    static void run(Server server, String sub, CommandLog logs) {
        boolean check = "check".equalsIgnoreCase(sub);
        Level sky = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
        if (!(sky instanceof SkyLevel)) {
            logs.add("regenerate fixture: FAIL - no SkyLevel");
            return;
        }
        int seed = ((SkyLevel) sky).getWorldGenSeed();
        Point skyMarker = new Point(SkyOrigin.originX(seed) + SKY_MARKER_DX,
                SkyOrigin.originY(seed) + SKY_MARKER_DY);
        Level surface = server.world.getLevel(LevelIdentifier.SURFACE_IDENTIFIER);
        Point spawn = server.world.worldEntity.spawnTile;
        Point surfaceMarker = new Point(spawn.x + SURFACE_MARKER_DX, spawn.y + SURFACE_MARKER_DY);

        if (!check) {
            synchronized (sky) {
                place(sky, skyMarker);
            }
            synchronized (surface) {
                place(surface, surfaceMarker);
            }
            SkywatchQuestData quest;
            synchronized (sky) {
                quest = SkywatchQuestData.get(sky);
                quest.recruited = true;
                quest.recruitedAuth = 1L;
                quest.stage = Math.max(quest.stage, 2);
            }
            SkywatchWorldData world = SkywatchWorldData.get(server);
            if (world != null) {
                world.markRecruited(1L);
                SkywatchWorldData.markRegionKeyEarned(server, 0);
                world.unlockBossPortals(0);
            }
            logs.add("regenerate fixture: placed markers and recorded progress (a TEST fixture)");
        }

        boolean skyPresent;
        synchronized (sky) {
            skyPresent = present(sky, skyMarker);
        }
        boolean surfacePresent;
        synchronized (surface) {
            surfacePresent = present(surface, surfaceMarker);
        }
        SkywatchQuestData quest = SkywatchQuestData.get(sky);
        SkywatchWorldData world = SkywatchWorldData.get(server);
        logs.add("regenerate fixture: skymarker=" + skyMarker.x + "," + skyMarker.y
                + " present=" + (skyPresent ? 1 : 0)
                + " surfacemarker=" + surfaceMarker.x + "," + surfaceMarker.y
                + " present=" + (surfacePresent ? 1 : 0));
        logs.add("regenerate fixture: progress stage=" + quest.stage
                + " recruited=" + quest.recruited
                + " catsHome=" + (quest.blackHome ? 1 : 0) + (quest.tabbyHome ? 1 : 0)
                + " wardenRecruited=" + (world != null && world.wardenRecruited)
                + " skyreachKey=" + (world != null && world.regionKeysEarned.contains(RealmDepth.keyOf(0)))
                + " skyreachPortals=" + (world != null && world.bossPortalsUnlocked(0))
                + " claims=" + (world == null ? "[]" : new java.util.TreeSet<>(world.residentsClaimed)));
    }

    private static void place(Level level, Point at) {
        level.regionManager.ensureTileIsLoaded(at.x, at.y);
        level.setTile(at.x, at.y, TileRegistry.lavaID);
        level.setObject(at.x, at.y, ObjectRegistry.getObjectID("storagebox"));
    }

    private static boolean present(Level level, Point at) {
        level.regionManager.ensureTileIsLoaded(at.x, at.y);
        return level.getTileID(at.x, at.y) == TileRegistry.lavaID
                && level.getObjectID(at.x, at.y) == ObjectRegistry.getObjectID("storagebox");
    }
}
