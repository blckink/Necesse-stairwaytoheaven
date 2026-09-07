package stairwaytoheaven.worldgen.pois;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Locale;

import necesse.engine.commands.CommandLog;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.WorldPresetsRegion;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyOrigin;

/**
 * How many of the thirteen inhabited places a world really stands up, and how
 * far the nearest one is from the tile the stairway drops the player on.
 *
 * <h2>Why this exists</h2>
 * The catalogue in {@link RealmPoiPresets} was registered on 2026-09-04 and the
 * player never found any of it. Nothing could have told him: {@code
 * scripts/integration_test.sh} counts the SURFACE POIs, which are a different
 * system ({@code swhsurfacepois}), and the word {@code realmpoi} did not occur
 * anywhere in it. The thirteen could have been generating zero times for days
 * and every gate would still have been green. This class is the gate that was
 * missing.
 *
 * <h2>The three questions, kept apart</h2>
 * <ol>
 *   <li><b>What does the placer decide?</b> {@link RealmPoiWorldPreset#survey}
 *       is the decision itself — {@code addToRegion} is one of its two callers
 *       — so the funnel printed here is the world's own, not a re-derivation
 *       that can drift. It is a pure function of the seed, so it can be walked
 *       across the whole realm disc without generating a tile.</li>
 *   <li><b>Does the engine agree?</b> A decision is not a queue: the preset
 *       region still applies the shared {@code villages} occupancy board, which
 *       is region state rather than seed. So the same boxes are walked a second
 *       time and the REAL queue read back
 *       ({@code LevelPresetsRegion.getDebugData}). {@code accepted} minus
 *       {@code queued} is exactly what the board took.</li>
 *   <li><b>Does anything get written?</b> A queue is only an intention. The
 *       nearest place of all is force-generated and its objects counted, which
 *       is the only proof that a preset writes a world.</li>
 * </ol>
 *
 * <p>What it cannot answer is whether the result READS as a town to a player.
 * That is {@code [game]}, and this is {@code [run]}.
 */
public final class RealmPoiCensus {

    /**
     * Half-width in tiles of the disc the funnel walks, rounded to whole preset
     * regions.
     *
     * <p>Six preset regions. {@link RealmDepth#DEPTH_SCALE} is 6000 and Hell's
     * band opens at depth 0.80, so 6144 reaches every band including the clamp
     * past the far edge where depth is 1.0 and the realm is always Hell. A
     * smaller disc would report a missing Hell kind that is merely out of shot.
     */
    private static final int CENSUS_RADIUS_TILES = 6 * WorldPresetsRegion.PRESET_REGION_REGION_SIZE * 16;

    /** Tiles per preset region side ({@code WorldPresetsRegion.tileWidth}). */
    private static final int PRESET_REGION_TILES = 1024;

    /** Level regions per preset region side. */
    private static final int PRESET_REGION_REGIONS = WorldPresetsRegion.PRESET_REGION_REGION_SIZE;

    private RealmPoiCensus() {
    }

    /** One accepted site, kept so the nearest of each kind can be looked up. */
    private static final class Site {
        final int kind;
        final int x;
        final int y;
        final int width;
        final int height;
        final int distance;

        Site(int kind, int x, int y, int width, int height, int distance) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.distance = distance;
        }
    }

    /**
     * Prints the census. Call from inside the level monitor: the queue lookup
     * and the stamp both take region locks, and the command's own comment
     * records why that order matters.
     */
    public static void run(SkyLevel level, CommandLog logs) {
        long started = System.currentTimeMillis();
        final int seed = level.getWorldGenSeed();
        Point arrival = SkyOrigin.arrival(seed);
        Point origin = SkyOrigin.compute(seed);

        // ---- 1. the placement decision, over the whole realm disc ----------
        final int[][] byRealmStage = new int[RealmDepth.REALM_COUNT][RealmPoiWorldPreset.STAGE_COUNT];
        final int[] acceptedByKind = new int[RealmPoiPresets.COUNT];
        final int[] candidates = {0};

        int firstRegionX = Math.floorDiv(origin.x - CENSUS_RADIUS_TILES, PRESET_REGION_TILES);
        int lastRegionX = Math.floorDiv(origin.x + CENSUS_RADIUS_TILES, PRESET_REGION_TILES);
        int firstRegionY = Math.floorDiv(origin.y - CENSUS_RADIUS_TILES, PRESET_REGION_TILES);
        int lastRegionY = Math.floorDiv(origin.y + CENSUS_RADIUS_TILES, PRESET_REGION_TILES);
        int boxes = 0;
        for (int prx = firstRegionX; prx <= lastRegionX; prx++) {
            for (int pry = firstRegionY; pry <= lastRegionY; pry++) {
                boxes++;
                int startX = prx * PRESET_REGION_TILES;
                int startY = pry * PRESET_REGION_TILES;
                RealmPoiWorldPreset.survey(seed, startX, startY,
                        startX + PRESET_REGION_TILES, startY + PRESET_REGION_TILES,
                        (kind, realm, x, y, width, height, stage) -> {
                            candidates[0]++;
                            byRealmStage[realm][stage]++;
                            if (stage != RealmPoiWorldPreset.STAGE_ACCEPTED) {
                                return;
                            }
                            acceptedByKind[kind]++;
                        });
            }
        }

        for (int realm = 0; realm < RealmDepth.REALM_COUNT; realm++) {
            StringBuilder line = new StringBuilder("realmpoi funnel ").append(RealmDepth.keyOf(realm)).append(':');
            int total = 0;
            for (int stage = 0; stage < RealmPoiWorldPreset.STAGE_COUNT; stage++) {
                total += byRealmStage[realm][stage];
            }
            line.append(" candidates=").append(total);
            for (int stage = 0; stage < RealmPoiWorldPreset.STAGE_COUNT; stage++) {
                line.append(' ').append(RealmPoiWorldPreset.STAGE_NAMES[stage])
                        .append('=').append(byRealmStage[realm][stage]);
            }
            logs.add(line.toString());
        }

        // ---- 2. what did the engine's own queue really take? ---------------
        // The same boxes again, this time read off the preset regions the world
        // built. The difference between the two counts is the occupancy board:
        // a site the decision accepted can still lose its ground to a structure
        // already standing there, and that loss is invisible in the funnel.
        final int[] queuedByKind = new int[RealmPoiPresets.COUNT];
        final Site[] nearestQueuedByKind = new Site[RealmPoiPresets.COUNT];
        int queuedTotal = 0;
        int unnamed = 0;
        WorldEntity world = level.getWorldEntity();
        LevelIdentifier identifier = SkyRegistry.SKYREACH_IDENTIFIER;
        long queueStarted = System.currentTimeMillis();
        for (int prx = firstRegionX; prx <= lastRegionX; prx++) {
            for (int pry = firstRegionY; pry <= lastRegionY; pry++) {
                WorldPresetsRegion worldPresets = world.getWorldPresets(
                        prx * PRESET_REGION_REGIONS, pry * PRESET_REGION_REGIONS);
                LevelPresetsRegion presets = worldPresets.getLevelRegions(identifier, 0);
                for (LevelPresetsRegion.PresetDebugData data : presets.getDebugData()) {
                    String name = data.getDebugName();
                    if (!name.startsWith(RealmPoiWorldPreset.STRING_ID + ":")) {
                        continue;
                    }
                    int newline = name.indexOf('\n');
                    int kind = kindOf(newline < 0 ? "" : name.substring(newline + 1).trim());
                    if (kind < 0) {
                        unnamed++;
                        continue;
                    }
                    for (Rectangle rect : data.getOccupiedTileRectangles()) {
                        queuedTotal++;
                        queuedByKind[kind]++;
                        int distance = tileDistance(rect.x + rect.width / 2, rect.y + rect.height / 2,
                                arrival.x, arrival.y);
                        if (nearestQueuedByKind[kind] == null
                                || distance < nearestQueuedByKind[kind].distance) {
                            nearestQueuedByKind[kind] =
                                    new Site(kind, rect.x, rect.y, rect.width, rect.height, distance);
                        }
                    }
                }
            }
        }
        long queueMs = System.currentTimeMillis() - queueStarted;

        int kindsAccepted = 0;
        int kindsQueued = 0;
        int acceptedTotal = 0;
        Site nearestOfAll = null;
        for (int kind = 0; kind < RealmPoiPresets.COUNT; kind++) {
            Site queued = nearestQueuedByKind[kind];
            acceptedTotal += acceptedByKind[kind];
            if (acceptedByKind[kind] > 0) {
                kindsAccepted++;
            }
            if (queuedByKind[kind] > 0) {
                kindsQueued++;
            }
            StringBuilder line = new StringBuilder("realmpoi kind ").append(RealmPoiPresets.key(kind))
                    .append(": realm=").append(RealmDepth.keyOf(RealmPoiPresets.realm(kind)))
                    .append(" size=").append(RealmPoiPresets.width(kind)).append('x')
                    .append(RealmPoiPresets.height(kind))
                    .append(" accepted=").append(acceptedByKind[kind])
                    .append(" queued=").append(queuedByKind[kind]);
            if (queued == null) {
                line.append(" nearest=NONE");
            } else {
                line.append(" nearest=").append(queued.distance)
                        .append(" at=").append(queued.x).append(',').append(queued.y);
                if (nearestOfAll == null || queued.distance < nearestOfAll.distance) {
                    nearestOfAll = queued;
                }
            }
            logs.add(line.toString());
        }

        // ---- 3. does anything get written? --------------------------------
        stampNearest(level, nearestOfAll, logs);

        logs.add("realmpoi census: seed=" + seed
                + " arrival=" + arrival.x + "," + arrival.y
                + " radius=" + CENSUS_RADIUS_TILES
                + " boxes=" + boxes
                + " candidates=" + candidates[0]
                + " accepted=" + acceptedTotal
                + " queued=" + queuedTotal
                + " unnamed=" + unnamed
                + " kinds=" + kindsAccepted + "/" + RealmPoiPresets.COUNT
                + " queuedkinds=" + kindsQueued + "/" + RealmPoiPresets.COUNT
                + " nearest=" + (nearestOfAll == null
                        ? "NONE" : RealmPoiPresets.key(nearestOfAll.kind) + "@" + nearestOfAll.distance)
                + " queuems=" + queueMs
                + " ms=" + (System.currentTimeMillis() - started));
    }

    /** The kind a queued rectangle's debug name belongs to, or -1 if unnamed. */
    private static int kindOf(String key) {
        for (int kind = 0; kind < RealmPoiPresets.COUNT; kind++) {
            if (RealmPoiPresets.key(kind).equals(key)) {
                return kind;
            }
        }
        return -1;
    }

    /**
     * Force-generates the nearest place and counts what is standing in it.
     *
     * <p>This is the assertion that separates "queued" from "built". A preset
     * that resolves no objects still queues a rectangle, and the difference is
     * invisible everywhere except here.
     */
    private static void stampNearest(Level level, Site site, CommandLog logs) {
        if (site == null) {
            logs.add("realmpoi stamp: NO SITE to stamp");
            return;
        }
        long started = System.currentTimeMillis();
        level.regionManager.ensureTilesAreLoaded(site.x, site.y,
                site.x + site.width - 1, site.y + site.height - 1);
        int objects = 0;
        for (int x = site.x; x < site.x + site.width; x++) {
            for (int y = site.y; y < site.y + site.height; y++) {
                if (level.getObjectID(x, y) != 0) {
                    objects++;
                }
            }
        }
        int area = site.width * site.height;
        logs.add("realmpoi stamp: kind=" + RealmPoiPresets.key(site.kind)
                + " at=" + site.x + "," + site.y
                + " objects=" + objects + "/" + area
                + " fill=" + String.format(Locale.ROOT, "%.1f%%", 100.0 * objects / area)
                + " ms=" + (System.currentTimeMillis() - started));
    }


    private static int tileDistance(int x, int y, int toX, int toY) {
        double dx = x - toX;
        double dy = y - toY;
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy));
    }
}
