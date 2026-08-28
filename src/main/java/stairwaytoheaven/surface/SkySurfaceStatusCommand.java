package stairwaytoheaven.surface;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.IntParameterHandler;
import necesse.engine.commands.parameterHandlers.StringParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.WorldEventRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.WorldPreset;
import necesse.engine.world.worldPresets.WorldPresetsRegion;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.SignObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.inventory.Inventory;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;

/**
 * Admin/debug probe for the Surface POIs and the Skyfall world event. This is
 * what {@code scripts/integration_test.sh} drives.
 *
 * <p>It answers the two questions a compiling preset cannot:
 * <ol>
 *   <li><b>Are the POIs queued?</b> {@code WorldPresetRegistry.initRegion} fills
 *       one queue per 1024x1024 preset region, and
 *       {@code LevelPresetsRegion.getDebugData()} hands that queue back — so the
 *       placement decision can be measured over a whole preset region without
 *       generating a single tile of world.</li>
 *   <li><b>Do they actually stamp?</b> The queue is only an intention.
 *       {@code stamp} mode force-generates the regions of a queued POI and
 *       counts its signature object in the world, which is the only proof that
 *       the preset wrote anything.</li>
 * </ol>
 *
 * <p>{@code seed} and {@code event} split a Skyfall in two so the integration
 * test can run it across a restart: {@code seed} starts one and leaves it
 * running with shards on the ground (so the world is saved mid-event), and
 * {@code event} finishes whatever is running and checks the world is clean
 * afterwards. Both drop their shards through the same placement path a player
 * would trigger, which is the only way to observe that half of the event on a
 * headless server with nobody to follow.
 */
public class SkySurfaceStatusCommand extends ModularChatCommand {

    /** How many 1024x1024 preset regions per side the census walks by default. */
    private static final int DEFAULT_CENSUS_SIDE = 2;
    /** Tiles per preset region side ({@code WorldPresetsRegion.tileWidth}). */
    private static final int PRESET_REGION_TILES = 1024;
    /** Level regions per preset region side. */
    private static final int PRESET_REGION_REGIONS = WorldPresetsRegion.PRESET_REGION_REGION_SIZE;
    /** How many queued POIs of each kind {@code stamp} mode force-generates. */
    private static final int STAMP_PER_KIND = 2;
    /** Shards {@code seed}/{@code event} mode drop, standing in for absent players. */
    private static final int EVENT_SEED_SHARDS = 12;
    /** Half-width of the box around spawn the shard counts scan. */
    private static final int SHARD_SCAN = 24;
    /** How far from spawn {@code basket} mode looks for a tile to stand a basket on. */
    private static final int BASKET_SEARCH_RADIUS = 24;
    /**
     * Duration {@code seed} mode gives its shower. Deliberately far longer than
     * {@link SkyfallWorldEvent#DURATION_MS}: the point of that mode is to leave
     * a live event in the world file for a restart to restore, and a shower on
     * the real two-minute timer would simply expire during the restart and
     * clean itself up before anybody could look.
     */
    private static final int PROBE_DURATION_MS = 900_000;

    public SkySurfaceStatusCommand() {
        super("skysurfacestatus",
                "Reports the mod's Surface POIs and the Skyfall world event (debug)",
                PermissionLevel.ADMIN, false,
                // "" reports registration + the queued census; "stamp" also
                // force-generates queued POIs and counts them in the world;
                // "seed" starts a Skyfall and LEAVES it running (so a restart
                // has something to restore); "event" finishes whatever is
                // running and checks the world is clean again; "basket" places
                // a Cat Basket on the Surface through the real placement path,
                // which is the only way to observe the cats actually moving in.
                new CmdParameter("mode",
                        new StringParameterHandler("", "stamp", "seed", "event", "basket"), true),
                new CmdParameter("regions", new IntParameterHandler(DEFAULT_CENSUS_SIDE), true));
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors,
            CommandLog logs) {
        String mode = args.length > 0 && args[0] != null ? String.valueOf(args[0]) : "";
        int side = args.length > 1 && args[1] != null ? (Integer) args[1] : DEFAULT_CENSUS_SIDE;
        side = Math.max(1, Math.min(4, side));

        Level level = server.world.getLevel(LevelIdentifier.SURFACE_IDENTIFIER);
        if (level == null) {
            logs.add("FAIL: no surface level");
            logs.add("SKYSURFACE_STATUS_DONE");
            return;
        }
        WorldEntity world = server.world.worldEntity;

        reportRegistration(logs);
        reportMaterials(logs);

        // Console commands run on the scanner thread while the server thread
        // ticks the level. Same lock order rule as SkyreachStatusCommand: take
        // the Level monitor first, because region generation from this thread
        // otherwise takes region locks before the Level monitor and deadlocks
        // against the server thread.
        synchronized (level) {
            Map<String, List<Rectangle>> census = census(world, level, side, logs);
            if (mode.equals("stamp")) {
                stamp(server, level, census, logs);
            }
        }
        if (mode.equals("seed")) {
            seedEvent(server, level, world, logs);
        }
        if (mode.equals("event")) {
            runEvent(server, level, world, logs);
        }
        if (mode.equals("basket")) {
            // Deliberately OUTSIDE the level monitor above, like seed/event:
            // placing the basket moves the cats, which touches the Skyreach's
            // regions and entity lists too, and taking a second level's locks
            // underneath this one's is how a lock-order deadlock gets built.
            placeCatBasket(server, level, world, logs);
        }
        reportSchedule(server, level, logs);
        logs.add("SKYSURFACE_STATUS_DONE");
    }

    // -----------------------------------------------------------------------

    private void reportRegistration(CommandLog logs) {
        GameObject shard = ObjectRegistry.getObject(SkySurface.skyfallShardID);
        logs.add("surface registry: poipreset=" + (SkySurface.poiPresets == null ? "MISSING"
                : SkySurface.poiPresets.getStringID() + "#" + SkySurface.poiPresets.getID())
                + " perregion=" + SkySurfacePresets.PRESETS_PER_REGION
                + " event=" + WorldEventRegistry.getEventStringID(SkySurface.skyfallEventID)
                + "#" + SkySurface.skyfallEventID
                + " shard=" + (shard == null ? "MISSING" : shard.getStringID() + "#" + SkySurface.skyfallShardID));
        if (shard != null) {
            logs.add("surface tool " + shard.getStringID() + "=" + shard.toolType.name() + "/" + shard.objectHealth);
        }
    }

    /**
     * Every object and tile the presets name, and whether it resolved. A −1 or
     * 0 here is a preset silently missing a piece — see {@link SurfaceMaterials}.
     */
    private void reportMaterials(CommandLog logs) {
        Map<String, Integer> resolved = SurfaceMaterials.resolved();
        StringBuilder bad = new StringBuilder();
        int unresolved = 0;
        for (Map.Entry<String, Integer> entry : resolved.entrySet()) {
            if (!SurfaceMaterials.isResolved(entry.getValue())) {
                unresolved++;
                bad.append(' ').append(entry.getKey()).append('=').append(entry.getValue());
            }
        }
        logs.add("surface materials: named=" + resolved.size() + " unresolved=" + unresolved + bad);
        logs.add("surface loot: items=" + SkySurface.LOOT_ITEMS.size()
                + " unresolved=" + SkySurface.UNRESOLVED_LOOT.size()
                + (SkySurface.UNRESOLVED_LOOT.isEmpty() ? "" : " " + SkySurface.UNRESOLVED_LOOT));
    }

    // -----------------------------------------------------------------------
    // 1. the placement decision, measured over whole preset regions

    /**
     * Walks {@code side x side} preset regions starting at the spawn one and
     * counts what the world preset system queued for the Surface. Returns the
     * queued rectangles per POI kind so {@code stamp} mode can go and look.
     */
    private Map<String, List<Rectangle>> census(WorldEntity world, Level level, int side, CommandLog logs) {
        Map<String, List<Rectangle>> byKind = new LinkedHashMap<>();
        for (SkySurfacePresets.SkySurfaceGeneration gen : SkySurface.poiPresets.all()) {
            byKind.put(gen.getClass().getSimpleName(), new ArrayList<>());
        }
        int vanillaQueued = 0;
        int blockedBySpawn = 0;
        int startRegionX = level.regionManager.getRegionCoordByTile(0);
        int startRegionY = level.regionManager.getRegionCoordByTile(0);
        long started = System.currentTimeMillis();

        // Vanilla drops a queued SURFACE preset that sits near the world spawn
        // (WorldEntity.removePresetsNearbySpawn:279-303, via
        // markPresetsToNotGenerate). The queue still LISTS it -- PresetDebugData
        // exposes no "blocked" flag -- so a census that counts it and a stamp
        // pass that then finds nothing placed disagree by exactly one, which is
        // what "generated=2 placedcounter=1" was. Not a bug in the preset: the
        // guard is what keeps structures off the player's starting area.
        // So run vanilla's own test here and leave those out of the census.
        Point spawn = world.defaultSpawnTile;
        int spawnRegionX = spawn == null ? 0 : level.regionManager.getRegionCoordByTile(spawn.x);
        int spawnRegionY = spawn == null ? 0 : level.regionManager.getRegionCoordByTile(spawn.y);

        for (int rx = 0; rx < side; rx++) {
            for (int ry = 0; ry < side; ry++) {
                int levelRegionX = startRegionX + rx * PRESET_REGION_REGIONS;
                int levelRegionY = startRegionY + ry * PRESET_REGION_REGIONS;
                WorldPresetsRegion worldPresets = world.getWorldPresets(levelRegionX, levelRegionY);
                LevelPresetsRegion presets = worldPresets.getLevelRegions(LevelIdentifier.SURFACE_IDENTIFIER, 0);
                for (LevelPresetsRegion.PresetDebugData data : presets.getDebugData()) {
                    String name = data.getDebugName();
                    if (!name.startsWith(SkySurfacePresets.STRING_ID + ":")) {
                        vanillaQueued++;
                        continue;
                    }
                    int newline = name.indexOf('\n');
                    String kind = newline < 0 ? "?" : name.substring(newline + 1).trim();
                    List<Rectangle> list = byKind.get(kind);
                    if (list == null) {
                        continue;
                    }
                    if (spawn != null && isBlockedBySpawn(data, spawn, spawnRegionX, spawnRegionY)) {
                        blockedBySpawn++;
                        continue;
                    }
                    for (Rectangle rect : data.getOccupiedTileRectangles()) {
                        list.add(rect);
                    }
                }
            }
        }

        int regions = side * side;
        StringBuilder line = new StringBuilder("poi census: presetregions=" + regions
                + " tilespan=" + (side * PRESET_REGION_TILES) + "x" + (side * PRESET_REGION_TILES)
                + " blockedbyspawn=" + blockedBySpawn);
        int total = 0;
        for (Map.Entry<String, List<Rectangle>> entry : byKind.entrySet()) {
            line.append(' ').append(entry.getKey()).append('=').append(entry.getValue().size());
            total += entry.getValue().size();
        }
        line.append(" total=").append(total);
        line.append(" perpresetregion=").append(String.format(java.util.Locale.ROOT, "%.2f", total / (double) regions));
        line.append(" vanillaqueued=").append(vanillaQueued);
        line.append(" ms=").append(System.currentTimeMillis() - started);
        logs.add(line.toString());
        return byKind;
    }

    // -----------------------------------------------------------------------
    // 2. does it actually stamp?

    /**
     * Force-generates the regions of a few queued POIs and counts each one's
     * signature object in the world. A preset that is queued but writes nothing
     * shows up here as {@code objects=0}.
     */
    private void stamp(Server server, Level level, Map<String, List<Rectangle>> census, CommandLog logs) {
        for (SkySurfacePresets.SkySurfaceGeneration gen : SkySurface.poiPresets.all()) {
            String kind = gen.getClass().getSimpleName();
            List<Rectangle> queued = census.getOrDefault(kind, new ArrayList<>());
            int before = gen.placed.get();
            int generated = 0;
            int objects = 0;
            Point where = null;
            Rectangle first = null;
            for (Rectangle rect : queued) {
                if (generated >= STAMP_PER_KIND) {
                    break;
                }
                if (anyRegionGenerated(level, rect)) {
                    continue;   // vanilla would skip this one: hasAlreadyGeneratedRegion
                }
                WorldPreset.ensureRegionsAreGenerated(level, rect.x, rect.y, rect.width, rect.height);
                int found = countObjects(level, rect, gen.signatureObject());
                objects += found;
                generated++;
                if (where == null) {
                    where = new Point(rect.x, rect.y);
                    first = rect;
                }
            }
            logs.add("poi stamp: " + kind + " queued=" + queued.size()
                    + " generated=" + generated
                    + " placedcounter=" + (gen.placed.get() - before)
                    + " " + gen.signatureObject() + "objects=" + objects
                    + " at=" + (where == null ? "none" : where.x + "," + where.y));
            if (first != null) {
                logs.add("poi contents: " + kind + " " + histogram(level, first)
                        + " chestitems=" + countChestItems(level, first)
                        + " sign=" + signText(level, first));
            }
        }
    }

    /**
     * Every object standing inside a stamped POI, counted. This is what shows
     * that a multi-tile piece survived {@code Region.checkTilesGenerationValid}
     * — a tent whose four quarters are not all present, or a crystal missing
     * its {@code "r"} half, is deleted rather than half-drawn.
     */
    private String histogram(Level level, Rectangle rect) {
        Map<String, Integer> counts = new java.util.TreeMap<>();
        for (int x = rect.x; x < rect.x + rect.width; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                GameObject object = level.getObject(x, y);
                if (object != null && object.getID() != 0) {
                    counts.merge(object.getStringID(), 1, Integer::sum);
                }
            }
        }
        StringBuilder out = new StringBuilder("objects=");
        String sep = "";
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            out.append(sep).append(entry.getKey()).append('x').append(entry.getValue());
            sep = ",";
        }
        return out.toString();
    }

    /**
     * The first readable sign inside the POI, as the player would read it.
     * This is the only part of a preset that is not just object IDs: the sign's
     * text is handed over by an {@code addCustomApply} hook at stamp time, so
     * "none" here means the hook never ran, and a raw {@code misc.<key>} means
     * the locale entry is missing.
     */
    private String signText(Level level, Rectangle rect) {
        for (int x = rect.x; x < rect.x + rect.width; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                ObjectEntity objectEntity = level.entityManager.getObjectEntity(x, y);
                if (objectEntity instanceof SignObjectEntity) {
                    String text = ((SignObjectEntity) objectEntity).getSignMessage().translate();
                    text = text.replace('\n', '|');
                    return "\"" + (text.length() > 60 ? text.substring(0, 60) : text) + "\"";
                }
            }
        }
        return "none";
    }

    /** Items sitting in every container inside the POI, i.e. did the loot land. */
    private int countChestItems(Level level, Rectangle rect) {
        int items = 0;
        for (int x = rect.x; x < rect.x + rect.width; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                ObjectEntity objectEntity = level.entityManager.getObjectEntity(x, y);
                if (objectEntity == null || !objectEntity.implementsOEInventory()) {
                    continue;
                }
                Inventory inventory = ((OEInventory) objectEntity).getInventory();
                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    if (!inventory.isSlotClear(slot)) {
                        items++;
                    }
                }
            }
        }
        return items;
    }

    /**
     * Vanilla's own near-spawn test, replicated exactly.
     *
     * {@code WorldEntity.removePresetsNearbySpawn} rejects a preset whose
     * occupied tile rectangle CONTAINS the spawn tile, or whose occupied
     * region is within {@code SpawnTileFinder.CLEAR_SPAWN_REGION_RANGE} (7)
     * regions of the spawn region. Copied rather than called because the
     * predicate is passed to markPresetsToNotGenerate and never exposed, and
     * because a loosened assertion would have hidden a real placement failure
     * just as well as this false one.
     */
    private boolean isBlockedBySpawn(LevelPresetsRegion.PresetDebugData data, Point spawn,
                                     int spawnRegionX, int spawnRegionY) {
        for (Rectangle rect : data.getOccupiedTileRectangles()) {
            if (rect.contains(spawn)) {
                return true;
            }
        }
        for (Point region : data.getOccupiedRegions()) {
            if (necesse.engine.util.GameMath.squareDistance(
                    (float) region.x, (float) region.y,
                    (float) spawnRegionX, (float) spawnRegionY)
                    <= (float) necesse.engine.world.SpawnTileFinder.CLEAR_SPAWN_REGION_RANGE) {
                return true;
            }
        }
        return false;
    }

    private boolean anyRegionGenerated(Level level, Rectangle rect) {
        int startX = level.regionManager.getRegionCoordByTile(rect.x);
        int endX = level.regionManager.getRegionCoordByTile(rect.x + rect.width - 1);
        int startY = level.regionManager.getRegionCoordByTile(rect.y);
        int endY = level.regionManager.getRegionCoordByTile(rect.y + rect.height - 1);
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (level.regionManager.isRegionGenerated(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countObjects(Level level, Rectangle rect, String stringID) {
        int count = 0;
        for (int x = rect.x; x < rect.x + rect.width; x++) {
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                GameObject object = level.getObject(x, y);
                if (object != null && stringID.equals(object.getStringID())) {
                    count++;
                }
            }
        }
        return count;
    }

    // -----------------------------------------------------------------------
    // 3. the world event, start to finish

    /**
     * Starts a Skyfall and leaves it running, with shards on the ground. The
     * server is then stopped by the test while the shower is still live, which
     * is what puts a half-finished event and its shard list into the world
     * file — the state phase 2 restores.
     */
    private void seedEvent(Server server, Level level, WorldEntity world, CommandLog logs) {
        SkyfallWorldData data = SkyfallWorldData.get(server);
        if (data == null) {
            logs.add("FAIL: no skyfall world data");
            return;
        }
        SkyfallWorldEvent event = data.active();
        if (event == null) {
            event = data.start(world, PROBE_DURATION_MS);
        }
        synchronized (level) {
            level.regionManager.ensureTilesAreLoaded(-SHARD_SCAN, -SHARD_SCAN, SHARD_SCAN, SHARD_SCAN);
            event.seedShards(server, level, 0, 0, EVENT_SEED_SHARDS);
        }
        logs.add("skyfall seed: remainingms=" + event.remainingMs()
                + " placed=" + event.totalPlaced()
                + " live=" + event.liveShards()
                + " inworld=" + countShards(level));
    }

    /**
     * Finishes whatever Skyfall is running — starting and seeding one first if
     * there is none — and checks the world is clean again afterwards. Run in
     * phase 2 it also proves the restored event still knows which tiles it
     * wrote, i.e. that nothing it dropped can outlive it.
     */
    private void runEvent(Server server, Level level, WorldEntity world, CommandLog logs) {
        SkyfallWorldData data = SkyfallWorldData.get(server);
        if (data == null) {
            logs.add("FAIL: no skyfall world data");
            return;
        }
        SkyfallWorldEvent event = data.active();
        boolean restored = event != null;
        if (event == null) {
            event = data.start(world, SkyfallWorldEvent.DURATION_MS);
        }
        synchronized (level) {
            level.regionManager.ensureTilesAreLoaded(-SHARD_SCAN, -SHARD_SCAN, SHARD_SCAN, SHARD_SCAN);
            if (!restored) {
                event.seedShards(server, level, 0, 0, EVENT_SEED_SHARDS);
            }
        }
        logs.add("skyfall run: restored=" + restored + " remainingms=" + event.remainingMs()
                + " placed=" + event.totalPlaced() + " live=" + event.liveShards()
                + " inworld=" + countShards(level));

        int cleared;
        synchronized (level) {
            cleared = event.clearShards(server);
        }
        event.over();
        logs.add("skyfall clean: cleared=" + cleared + " leftbehind=" + countShards(level)
                + " over=" + event.isOver());
    }

    // -----------------------------------------------------------------------
    // 4. the cats' home

    /**
     * Puts a Cat Basket down on the SURFACE, through the same path a player's
     * own placement takes, and reports what happened to the cats.
     *
     * <p>This exists because the whole player report is that placing a basket
     * did nothing: <em>"ich habe beide gerade platziert und die sind weg oder
     * irgendwo anders dann erschienen wo ich es nicht weiss"</em>. A feature
     * that compiles and never moves a cat is exactly the failure that shipped,
     * so the integration test has to drive a real placement and then look at
     * where the cats ended up. {@code ObjectItem.onPlaceObject} is the call
     * {@code ObjectItem.onPlace} makes on the server for a player's placement
     * (ObjectItem.java:321/401) -- going through it rather than writing the
     * object layer directly is the point: the hook under test is
     * {@code GameObject.placeObject}, and only this path reaches it.
     */
    private void placeCatBasket(Server server, Level level, WorldEntity world, CommandLog logs) {
        GameObject basket = ObjectRegistry.getObject(stairwaytoheaven.SkyRegistry.catBasketID);
        if (basket == null) {
            logs.add("cat basket place: FAIL (catbasket is not registered)");
            return;
        }
        Point spawn = world.defaultSpawnTile == null ? new Point(0, 0) : world.defaultSpawnTile;
        Point first = freeTile(level, basket, spawn, null, null);
        if (first == null) {
            logs.add("cat basket place: FAIL (no free surface tile within "
                    + BASKET_SEARCH_RADIUS + " of spawn " + spawn.x + "," + spawn.y + ")");
            return;
        }
        place(server, level, basket, first);
        report(server, level, "first", first, logs);

        // The second basket. The rule the player has to be able to hold in
        // their head is "the newest one wins", and the only way to know it
        // holds is to place two.
        Point second = freeTile(level, basket, spawn, first, null);
        if (second != null) {
            place(server, level, basket, second);
            report(server, level, "second", second, logs);

            // Breaking the OLD basket must not evict anybody: a basket that is
            // not the recorded home has nothing to clear.
            destroy(server, level, first);
            report(server, level, "brokeold", first, logs);

            // Breaking the ACTIVE one must, and the cats go back to the spire
            // basket -- across the dimension boundary, the other way.
            destroy(server, level, second);
            report(server, level, "brokeactive", second, logs);
        }

        // ...and one more, so the world is left with the cats living in a
        // basket on the Surface. That is the state the restart pass checks.
        Point last = freeTile(level, basket, spawn, first, second);
        if (last != null) {
            place(server, level, basket, last);
            report(server, level, "final", last, logs);
        }
    }

    /**
     * The first tile out from spawn the basket itself says it can stand on.
     * Asking the object beats guessing: "occupied", "liquid" and the shore rules
     * are all its own.
     */
    private Point freeTile(Level level, GameObject basket, Point spawn, Point skipA, Point skipB) {
        synchronized (level) {
            for (int radius = 2; radius <= BASKET_SEARCH_RADIUS; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) {
                            continue;
                        }
                        int x = spawn.x + dx;
                        int y = spawn.y + dy;
                        if (skipA != null && skipA.x == x && skipA.y == y) {
                            continue;
                        }
                        if (skipB != null && skipB.x == x && skipB.y == y) {
                            continue;
                        }
                        level.regionManager.ensureTileIsLoaded(x, y);
                        if (basket.canPlace(level, 0, x, y, 0, true, false) == null) {
                            return new Point(x, y);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Placed OUTSIDE the level monitor, like {@code seed}/{@code event}:
     * {@code CatHome.claim} moves the cats, which reaches into another level's
     * regions and entity lists, and taking a second level's locks underneath
     * this one's is how a lock-order deadlock gets built.
     */
    private void place(Server server, Level level, GameObject basket, Point site) {
        basket.getObjectItem().onPlaceObject(basket, level, 0, site.x, site.y, 0, null, null);
        server.network.sendToClientsWithTile(
                new necesse.engine.network.packet.PacketChangeObject(level, 0, site.x, site.y, basket.getID()),
                level, site.x, site.y);
    }

    /**
     * The real break path: {@code destroyObjectOverride} runs the object's full
     * damage-to-destruction flow, and {@code DamagedObjectEntity.destroyObject}
     * (jar 1.3.2, DamagedObjectEntity.java:227) is what calls
     * {@code GameObject.onDestroyed} -- the hook under test.
     */
    private void destroy(Server server, Level level, Point site) {
        level.entityManager.destroyObjectOverride(0, site.x, site.y);
        server.network.sendToClientsWithTile(
                new necesse.engine.network.packet.PacketChangeObject(level, 0, site.x, site.y,
                        level.getObjectID(site.x, site.y)),
                level, site.x, site.y);
    }

    /** What stands on the tile, what the world record says, and where the cats are. */
    private void report(Server server, Level level, String step, Point site, CommandLog logs) {
        stairwaytoheaven.quest.CatHome.Spot home = stairwaytoheaven.quest.CatHome.placed(server);
        StringBuilder line = new StringBuilder("cat basket place: step=").append(step)
                .append(" at=").append(level.getIdentifier()).append(':')
                .append(site.x).append(',').append(site.y)
                .append(" object=").append(level.getObject(site.x, site.y).getStringID())
                .append(" recordedHome=").append(home == null ? "NONE" : home.toString());
        // Where the cats ended up. Both candidate levels are looked at, because
        // "the cat is not on this level" and "the cat is gone" are different
        // facts and only one of them is a bug.
        for (necesse.engine.util.LevelIdentifier id : new necesse.engine.util.LevelIdentifier[]{
                LevelIdentifier.SURFACE_IDENTIFIER, stairwaytoheaven.SkyRegistry.SKYREACH_IDENTIFIER}) {
            Level catLevel = id.equals(level.getIdentifier()) ? level : server.world.getLevel(id);
            int count = 0;
            if (catLevel != null) {
                for (necesse.entity.mobs.Mob mob : catLevel.entityManager.mobs) {
                    if (mob instanceof stairwaytoheaven.mobs.SpireCatMob) {
                        count++;
                    }
                }
            }
            line.append(' ').append(id).append("cats=").append(count);
        }
        logs.add(line.toString());
    }

    /** How many Fallen Skyshards are standing in the scanned box around spawn. */
    private int countShards(Level level) {
        int count = 0;
        for (int x = -SHARD_SCAN; x <= SHARD_SCAN; x++) {
            for (int y = -SHARD_SCAN; y <= SHARD_SCAN; y++) {
                if (level.getObjectID(x, y) == SkySurface.skyfallShardID) {
                    count++;
                }
            }
        }
        return count;
    }

    private void reportSchedule(Server server, Level level, CommandLog logs) {
        SkyfallWorldData data = SkyfallWorldData.get(server);
        WorldEntity world = server.world.worldEntity;
        SkyfallWorldEvent active = data == null ? null : data.active();
        logs.add("skyfall schedule: day=" + (world == null ? -1 : world.getDay())
                + " night=" + (world != null && world.isNight())
                + " nextday=" + (data == null ? -1 : data.nextDay())
                + " lastday=" + (data == null ? -1 : data.lastDay())
                + " active=" + (active != null)
                + " liveshards=" + (active == null ? 0 : active.liveShards())
                + " inworld=" + countShards(level));
    }
}
