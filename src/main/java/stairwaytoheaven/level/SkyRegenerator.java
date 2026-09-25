package stairwaytoheaven.level;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import necesse.engine.GlobalData;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.TeleportResult;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.WorldFile;
import necesse.engine.world.WorldFileSystem;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.WorldPresetsRegion;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.PortalObjectEntity;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.CachedSettlementData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.worldgen.pois.SkyLandmarkPois;

/**
 * {@code /swhreset regenerate confirm}: throw the whole mod plane away and
 * generate it again with the current build, keeping the quest progress.
 *
 * <h2>Why it exists</h2>
 * The player, testing a new build on a long-running world: <i>"alle
 * Mod-Gebiete auch um den Spire werden neu generiert! Quests bisher können so
 * bleiben, aber Gebiete und Rest werden zwingend neu erstellt."</i>
 * {@code /swhreset world} cannot do that and says so: it never repaints ground
 * that exists. This does — by deleting the ground.
 *
 * <h2>The mechanism, and why this one</h2>
 * Every realm lives on ONE level, {@code skyreach2}
 * ({@code docs/PLAN_ONE_PLANE.md}), and a save stores a level as three things
 * ({@code WorldFileSystem}, jar 1.3.2): {@code levels/skyreach2.dat},
 * {@code levels/regions/skyreach2/}, {@code levels/presets/skyreach2/}.
 * {@code WorldFileSystem.deleteAllLevelFiles} removes exactly those three and
 * nothing else, through the same NIO file system the save itself is written
 * with — so it is the same call for a folder save and a {@code .zip} save.
 * Vanilla's {@code LevelManager.deleteLevel} (the path {@code /deletelevel}
 * and a closing incursion take) is: move the players off, unload the level,
 * {@code deleteAllLevelFiles}, {@code deleteSettlementsAt}. This class runs the
 * same steps itself, in that order, with three differences:
 * <ol>
 * <li>players are sent to their OWN return stairway (the Skywatch Gate's
 *     record), not to vanilla's fallback, and none is left behind;</li>
 * <li>vanilla's {@code ReturnedObjects} step is skipped, because it drops the
 *     level's returned items onto the FALLBACK level — the surface — and this
 *     command never writes to the surface;</li>
 * <li>the in-memory world-preset cache for {@code skyreach2} is dropped. Vanilla
 *     does not do that, and it matters here: {@code WorldPresetsRegion} keeps a
 *     {@code LevelPresetsRegion} per identifier for up to a minute after its
 *     last use, and that object carries per-preset "already generated" flags
 *     and a {@code generatedPresets} list it would write straight back into
 *     the folder that was just deleted.</li>
 * </ol>
 * Then {@code World.getLevel} is asked for the identifier again. The level file
 * is gone, so it asks the registered generator for a new one — exactly what
 * happens on a world's first ascent — and the spire is stamped at once.
 *
 * <h2>What survives, and why</h2>
 * <ul>
 * <li>{@link SkywatchQuestData}'s progress half, carried across by
 *     {@link SkywatchQuestData#copyProgressFrom}; its placement half is left
 *     behind so the new sky stamps the spire, the three landmarks, their
 *     guards and loot, and the cats again.</li>
 * <li>{@link SkywatchWorldData} and {@code VeilWorldData} are world data, in
 *     {@code world.dat}, and are not touched — except for two records that
 *     only describe the sky that went away: resident claims for people who
 *     lived there, and a cat home that was a basket in it. See
 *     {@link #computePlan}.</li>
 * <li>Journal quests, inventories, the surface, other levels and their
 *     settlements: not read for writing, not written.</li>
 * </ul>
 */
public final class SkyRegenerator {

    private SkyRegenerator() {
    }

    /** Where backups go: beside the saves folder, never inside a world's folder. */
    public static final String BACKUP_FOLDER = "swh-sky-backups";

    /** What a regenerate would do in this world, worked out without changing anything. */
    public static final class Plan {
        public boolean levelExists;
        public final List<String> playersInSky = new ArrayList<>();
        public final List<Integer> skySettlements = new ArrayList<>();
        /** Settler string IDs living in settlements that are NOT on the sky level. */
        public final Set<String> housedElsewhere = new TreeSet<>();
        public final Set<String> claimsKept = new TreeSet<>();
        public final Set<String> claimsReleased = new TreeSet<>();
        public boolean wardenLostWithSky;
        public boolean catHomeInSky;
        public int regionFiles;
        public int presetFiles;
        public final Set<LevelIdentifier> childLevels = new HashSet<>();
    }

    // ------------------------------------------------------------------
    // the plan: what WOULD happen
    // ------------------------------------------------------------------

    /**
     * Reads everything a regenerate decides on, and changes nothing.
     *
     * <h2>The resident claims — the one decision here that needs care</h2>
     * {@code residentsClaimed} is what stops a world holding two Magpies. After
     * a regenerate each claimed person is in exactly one of three places:
     * <ol>
     * <li><b>in a settlement on another level</b> (usually the surface): they
     *     still exist. The claim is KEPT, so the new sky never stands up a
     *     second one;</li>
     * <li><b>standing in the sky, unrecruited</b>: they are deleted with the
     *     sky. The claim is RELEASED, so the new sky can place them again —
     *     otherwise a regenerate would make them unreachable for good;</li>
     * <li><b>in a settlement on the sky level</b>: that settlement is deleted
     *     with the sky ({@code deleteSettlementsAt}, like vanilla's
     *     {@code deleteLevel}). RELEASED, for the same reason.</li>
     * </ol>
     * "Lives in a settlement elsewhere" is read from every settlement whose
     * level is not {@code skyreach2}: from memory when it is loaded, and
     * otherwise from its own file, read-only. No level is loaded to answer it.
     */
    public static Plan computePlan(Server server) {
        Plan plan = new Plan();
        LevelIdentifier sky = SkyRegistry.SKYREACH_IDENTIFIER;
        plan.levelExists = server.world.levelExists(sky);
        server.streamClients()
                .filter(c -> c != null && sky.equals(c.getLevelIdentifier()))
                .forEach(c -> plan.playersInSky.add(c.getName()));

        SettlementsWorldData settlements = SettlementsWorldData.getSettlementsData(server);
        List<CachedSettlementData> all = settlements.streamSettlements().collect(Collectors.toList());
        for (CachedSettlementData cached : all) {
            if (cached == null || cached.levelIdentifier == null) {
                continue;
            }
            if (sky.equals(cached.levelIdentifier)) {
                plan.skySettlements.add(cached.uniqueID);
            } else {
                plan.housedElsewhere.addAll(settlerIDsOf(server, settlements, cached.uniqueID));
            }
        }

        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world != null) {
            for (String claimed : world.residentsClaimed) {
                if (plan.housedElsewhere.contains(claimed)) {
                    plan.claimsKept.add(claimed);
                } else {
                    plan.claimsReleased.add(claimed);
                }
            }
            plan.wardenLostWithSky = world.wardenRecruited
                    && !plan.housedElsewhere.contains("wardensettler");
            plan.catHomeInSky = world.catHomeSet && sky.stringID.equals(world.catHomeLevel);
        }

        WorldFileSystem fs = server.world.fileSystem;
        plan.regionFiles = countFiles(fs.getLevelRegionsFolder(sky));
        plan.presetFiles = countFiles(fs.getLevelPresetsFolder(sky));
        Level loaded = server.world.levelManager.getLevel(sky);
        if (loaded != null) {
            plan.childLevels.addAll(loaded.childLevels);
        }
        return plan;
    }

    /** The settler string IDs of one settlement, without loading its level. */
    private static Set<String> settlerIDsOf(Server server, SettlementsWorldData settlements, int uniqueID) {
        Set<String> out = new HashSet<>();
        ServerSettlementData loaded = settlements.getServerData(uniqueID);
        if (loaded != null) {
            for (LevelSettler settler : loaded.getSettlers()) {
                if (settler != null && settler.settler != null) {
                    out.add(settler.settler.getStringID());
                }
            }
            return out;
        }
        // Not in memory: read its file, the way SettlementsWorldData itself
        // does (saveSettlementData writes NETWORK + SERVER, and SERVER holds a
        // SETTLERS list of SETTLER records whose "stringID" is the settler's
        // registry name; older files have the SETTLER records directly under
        // SERVER, and ServerSettlementData.applyLoadData reads both).
        try {
            WorldFile file = server.world.fileSystem.getSettlementFile(uniqueID);
            if (!file.exists()) {
                return out;
            }
            LoadData save = new LoadData(file);
            LoadData serverSave = save.getFirstLoadDataByName("SERVER");
            if (serverSave == null) {
                return out;
            }
            List<LoadData> records = new ArrayList<>(serverSave.getLoadDataByName("SETTLER"));
            LoadData list = serverSave.getFirstLoadDataByName("SETTLERS");
            if (list != null) {
                records.addAll(list.getLoadDataByName("SETTLER"));
            }
            for (LoadData record : records) {
                String id = record.getUnsafeString("stringID", null, false);
                if (id != null && !id.isEmpty()) {
                    out.add(id);
                }
            }
        } catch (Exception e) {
            System.err.println("swhreset regenerate: could not read settlement " + uniqueID + ": " + e);
        }
        return out;
    }

    private static int countFiles(WorldFile folder) {
        if (folder == null || !folder.exists()) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(pathOf(folder))) {
            return (int) walk.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            return -1;
        }
    }

    /** WorldFile keeps its NIO path protected; its own methods expose it by name. */
    private static Path pathOf(WorldFile file) {
        return file.toAbsolutePath();
    }

    /** Human description of a plan, the dry run's whole output. */
    public static void describe(Plan plan, Consumer<String> out) {
        out.accept("  sky files now: " + plan.regionFiles + " region file(s), "
                + plan.presetFiles + " preset file(s), levels/" + SkyRegistry.SKYREACH_IDENTIFIER.stringID + ".dat");
        out.accept("  players in the sky, sent to their return stairway first: "
                + (plan.playersInSky.isEmpty() ? "none" : String.join(", ", plan.playersInSky)));
        out.accept("  settlements ON the sky level, deleted with it: " + plan.skySettlements.size()
                + (plan.skySettlements.isEmpty() ? "" : " " + plan.skySettlements));
        out.accept("  resident claims kept (they live in a settlement elsewhere): "
                + (plan.claimsKept.isEmpty() ? "none" : String.join(", ", plan.claimsKept)));
        out.accept("  resident claims released (they only stood in the sky): "
                + (plan.claimsReleased.isEmpty() ? "none" : String.join(", ", plan.claimsReleased)));
        if (plan.catHomeInSky) {
            out.accept("  the cats' basket stands in the sky: the record is cleared, they go back to the spire basket");
        }
        if (plan.wardenLostWithSky) {
            out.accept("  WARNING: the Warden is recruited but lives in no settlement outside the sky."
                    + " If his settlement is up there, he is deleted with it.");
        }
        if (!plan.childLevels.isEmpty()) {
            out.accept("  REFUSED while the sky has child levels (an open incursion?): " + plan.childLevels);
        }
    }

    // ------------------------------------------------------------------
    // the run
    // ------------------------------------------------------------------

    /** What a finished run reports; also what the gate greps. */
    public static final class Result {
        public boolean done;
        public String backup = "";
        public int regionFilesDeleted;
        public int presetFilesDeleted;
        public int presetCachesDropped;
        public int playersMoved;
    }

    /**
     * Runs the whole regenerate. MUST be called on the server thread, or while
     * the server is paused: it unloads a level the tick loop would otherwise
     * be ticking. {@code SwhResetCommand} enforces that.
     */
    public static Result run(Server server, Consumer<String> out) {
        Result result = new Result();
        LevelIdentifier skyID = SkyRegistry.SKYREACH_IDENTIFIER;
        Plan plan = computePlan(server);
        if (!plan.levelExists) {
            out.accept("swhreset regenerate: this world has no sky yet - nothing to regenerate.");
            out.accept("  climb a Skyward Stairway (or run /skyreachstatus) and it is generated fresh.");
            return result;
        }
        if (!plan.childLevels.isEmpty()) {
            out.accept("swhreset regenerate: REFUSED - the sky has child levels " + plan.childLevels
                    + ". Close them (finish or abandon the incursion) and run it again.");
            return result;
        }
        if (isSaveRunning(server)) {
            // ServerSaveHandler captures a LevelSaveHandler per loaded level
            // when it is BUILT and writes over several ticks. One built before
            // the unload would write the old sky's regions back after they
            // were deleted.
            out.accept("swhreset regenerate: REFUSED - a world save is running right now."
                    + " Wait for 'Completed world save' and run it again.");
            return result;
        }

        Level sky = server.world.getLevel(skyID);
        if (!(sky instanceof SkyLevel)) {
            out.accept("swhreset regenerate: FAIL - " + skyID + " is not a SkyLevel");
            return result;
        }

        // 1. The progress half of the level's quest record.
        SkywatchQuestData carried = new SkywatchQuestData();
        SkywatchQuestData old = SkywatchQuestData.get(sky);
        carried.copyProgressFrom(old);
        String oldSpire = old.spirePlaced ? old.spireX + "," + old.spireY : "unplaced";

        // 2. Nobody may stand on a level that is about to be deleted.
        result.playersMoved = evacuate(server, carried);
        List<String> stillThere = server.streamClients()
                .filter(c -> c != null && skyID.equals(c.getLevelIdentifier()))
                .map(ServerClient::getName).collect(Collectors.toList());
        if (!stillThere.isEmpty()) {
            out.accept("swhreset regenerate: REFUSED - could not move " + stillThere
                    + " off the sky. Nothing was deleted.");
            return result;
        }
        out.accept("  players moved home: " + result.playersMoved);

        // 3. Write the sky as it is, so the backup is the sky as it WAS.
        server.world.saveLevel(sky);
        // 4. Unload WITHOUT saving (LevelManager.unloadLevel: onUnloading +
        //    dispose, no World.saveLevel). SettlementsWorldData hears it and
        //    writes the sky's settlements to their files first.
        server.world.levelManager.unloadLevel(skyID);

        // 5. Backup, then delete.
        try {
            result.backup = backup(server, plan);
            out.accept("  backup: " + result.backup);
        } catch (IOException e) {
            out.accept("swhreset regenerate: REFUSED - the backup failed (" + e
                    + "). Nothing was deleted; the sky loads again on the next visit.");
            return result;
        }
        SettlementsWorldData.getSettlementsData(server).deleteSettlementsAt(skyID);
        try {
            // Counted again rather than taken from the plan: step 3 has just
            // written every loaded region, so a sky that was never saved since
            // it last grew has more files now than the plan saw.
            result.regionFilesDeleted = countFiles(server.world.fileSystem.getLevelRegionsFolder(skyID));
            result.presetFilesDeleted = countFiles(server.world.fileSystem.getLevelPresetsFolder(skyID));
            server.world.fileSystem.deleteAllLevelFiles(skyID);
        } catch (IOException e) {
            out.accept("swhreset regenerate: FAIL - deleting the sky's files threw " + e
                    + ". The backup is at " + result.backup);
            return result;
        }
        result.presetCachesDropped = dropPresetCaches(server.world.worldEntity, skyID);

        out.accept("  deleted: " + result.regionFilesDeleted + " region file(s), "
                + result.presetFilesDeleted + " preset file(s), the level file, "
                + plan.skySettlements.size() + " sky settlement(s); preset caches dropped: "
                + result.presetCachesDropped);
        WorldFileSystem fs = server.world.fileSystem;
        int left = Math.max(0, countFiles(fs.getLevelRegionsFolder(skyID)))
                + Math.max(0, countFiles(fs.getLevelPresetsFolder(skyID)))
                + (fs.levelFileExists(skyID) ? 1 : 0);
        out.accept("  sky files left on the save before regenerating: " + left);
        if (left != 0) {
            out.accept("swhreset regenerate: FAIL - the delete left " + left + " file(s) behind."
                    + " Nothing was regenerated; the backup is at " + result.backup);
            return result;
        }

        // 6. The two world records that only described the deleted sky.
        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world != null) {
            world.residentsClaimed.removeAll(plan.claimsReleased);
            // The Spire Village's "who lives at home" record described the
            // deleted houses; the new village re-seats and re-records them.
            world.villageResidents.removeAll(plan.claimsReleased);
            if (plan.catHomeInSky) {
                world.catHomeSet = false;
                world.catHomeLevel = "";
                world.catHomeX = 0;
                world.catHomeY = 0;
            }
        }
        out.accept("  resident claims released: "
                + (plan.claimsReleased.isEmpty() ? "none" : String.join(",", plan.claimsReleased))
                + " kept: " + (plan.claimsKept.isEmpty() ? "none" : String.join(",", plan.claimsKept)));

        // 7. A new sky, from the generator, exactly like a first ascent.
        Level fresh = server.world.getLevel(skyID);
        if (!(fresh instanceof SkyLevel) || fresh == sky) {
            out.accept("swhreset regenerate: FAIL - the level did not come back as a new SkyLevel");
            return result;
        }
        SkyLevel freshSky = (SkyLevel) fresh;
        synchronized (fresh) {
            SkywatchQuestData quest = SkywatchQuestData.get(fresh);
            quest.copyProgressFrom(carried);
            freshSky.ensureWardenSpire();
            quest = SkywatchQuestData.get(fresh);
            int wardens = 0;
            for (Mob mob : fresh.entityManager.mobs) {
                if ("skywarden".equals(mob.getStringID())) {
                    wardens++;
                }
            }
            int beacon = fresh.getObjectID(quest.beaconX, quest.beaconY);
            boolean beaconOk = beacon == SkyRegistry.wardenBeaconOffID || beacon == SkyRegistry.wardenBeaconOnID;
            out.accept("  new spire=" + (quest.spirePlaced ? quest.spireX + "," + quest.spireY : "unplaced")
                    + " (was " + oldSpire + ") beacon=" + (beaconOk ? 1 : 0)
                    + " spireWardens=" + wardens
                    + " landmarks=" + quest.landmarksStamped.size() + "/" + SkyLandmarkPois.KINDS.length
                    + " cats=" + (quest.catsSpawned ? "spawned" : "not"));
            out.accept("  progress kept: stage=" + quest.stage + " recruited=" + quest.recruited
                    + " anchor=" + quest.anchorDone
                    + " catsHome=" + (quest.blackHome ? 1 : 0) + (quest.tabbyHome ? 1 : 0)
                    + " returnStairways=" + quest.returnStairs.size()
                    + (world == null ? "" : " keys=" + world.regionKeysEarned.size()
                    + " portalsUnlocked=" + world.bossPortalsUnlocked.size()
                    + " wardenRecruited=" + world.wardenRecruited));
        }
        // 8. Onto disk now rather than at the next autosave, so a crash in
        //    between cannot leave a world whose sky files are gone and whose
        //    new sky was never written.
        server.world.saveLevel(fresh);
        server.world.saveWorldEntity();
        result.done = true;
        out.accept("swhreset regenerate: DONE - the sky was generated anew with this build.");
        return result;
    }

    /**
     * Sends everyone on the sky level home: to the surface stairway they came
     * up by (the record the Skywatch Gate uses), or vanilla's fallback if they
     * have none. A player whose respawn point is a bed in the sky gets it reset
     * to the world spawn, because that bed is about to stop existing.
     */
    private static int evacuate(Server server, SkywatchQuestData quest) {
        LevelIdentifier skyID = SkyRegistry.SKYREACH_IDENTIFIER;
        List<ServerClient> inSky = server.streamClients()
                .filter(c -> c != null && skyID.equals(c.getLevelIdentifier()))
                .collect(Collectors.toList());
        int moved = 0;
        for (ServerClient client : inSky) {
            long[] home = quest.getReturnStairway(client.authentication);
            if (home != null) {
                int tileX = (int) home[0];
                int tileY = (int) home[1];
                client.changeLevelCheck(LevelIdentifier.SURFACE_IDENTIFIER, level -> {
                    level.regionManager.ensureTileIsLoaded(tileX, tileY);
                    Point spot = PortalObjectEntity.getTeleportDestinationAroundObject(
                            level, client.playerMob, tileX, tileY, true);
                    if (spot == null) {
                        spot = new Point(tileX * 32 + 16, tileY * 32 + 16);
                    }
                    return new TeleportResult(true, spot);
                }, true);
            }
            if (skyID.equals(client.getLevelIdentifier())) {
                client.changeToFallbackLevel(skyID, true);
            }
            if (skyID.equals(client.spawnLevelIdentifier)) {
                client.resetSpawnPoint(server);
            }
            if (!skyID.equals(client.getLevelIdentifier())) {
                moved++;
                client.sendChatMessage(new LocalMessage("misc", "swhregenerateevacuated"));
            }
        }
        return moved;
    }

    /**
     * Copies the sky's three save parts, plus the files of any settlement on
     * it, to {@code <game data>/swh-sky-backups/<world>-<time>/}, mirroring the
     * layout inside the save so a restore is "copy these back in".
     *
     * <p>Outside the saves folder on purpose: {@code World.loadWorldsFromPaths}
     * lists both {@code saves/worlds/} and {@code saves/}, and a backup must
     * never show up in the world list.
     */
    private static String backup(Server server, Plan plan) throws IOException {
        WorldFileSystem fs = server.world.fileSystem;
        LevelIdentifier skyID = SkyRegistry.SKYREACH_IDENTIFIER;
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String worldName = server.world.filePath == null ? "world"
                : server.world.filePath.getName().replaceAll("\\.zip$", "");
        File root = new File(GlobalData.appDataPath() + BACKUP_FOLDER + File.separator + worldName + "-" + stamp);
        Map<String, WorldFile> copy = new HashMap<>();
        copy.put("levels/" + skyID.stringID + ".dat", fs.getLevelFile(skyID));
        for (int uniqueID : plan.skySettlements) {
            copy.put("levels/settlements/" + uniqueID + ".dat", fs.getSettlementFile(uniqueID));
        }
        for (Map.Entry<String, WorldFile> entry : copy.entrySet()) {
            if (entry.getValue().exists()) {
                copyOne(pathOf(entry.getValue()), new File(root, entry.getKey()));
            }
        }
        copyTree(fs.getLevelRegionsFolder(skyID), "levels/regions/" + skyID.stringID, root);
        copyTree(fs.getLevelPresetsFolder(skyID), "levels/presets/" + skyID.stringID, root);
        return root.getAbsolutePath();
    }

    private static void copyTree(WorldFile folder, String relative, File root) throws IOException {
        if (!folder.exists()) {
            return;
        }
        Path base = pathOf(folder);
        try (Stream<Path> walk = Files.walk(base)) {
            for (Path file : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                copyOne(file, new File(root, relative + "/" + base.relativize(file).toString()));
            }
        }
    }

    private static void copyOne(Path from, File to) throws IOException {
        File parent = to.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("cannot create " + parent);
        }
        // Works across providers: a zip save's entry is copied out of the zip
        // file system onto the default one.
        Files.copy(from, to.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // ------------------------------------------------------------------
    // engine internals that have no public door
    // ------------------------------------------------------------------

    /**
     * Is a world save in flight? {@code Server.saveHandler} is private and has
     * no getter (jar 1.3.2). If reflection fails the answer is "no", and the
     * command says so, rather than refusing forever on a future engine.
     */
    static boolean isSaveRunning(Server server) {
        try {
            Field field = Server.class.getDeclaredField("saveHandler");
            field.setAccessible(true);
            return field.get(server) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            System.err.println("swhreset regenerate: cannot see Server.saveHandler (" + e + "), assuming no save runs");
            return false;
        }
    }

    /**
     * Drops every cached {@code LevelPresetsRegion} of this identifier from the
     * world's preset cache, so the new level computes its presets from scratch.
     * {@code WorldEntity.worldPresetsCache}, {@code WorldPresetsRegion.levelRegions}
     * and {@code lastLevelPresetsRegion} are protected with no remover (jar
     * 1.3.2); both classes guard them with their own monitor, which is taken
     * here the same way.
     *
     * @return how many cached entries were dropped, or -1 if reflection failed
     */
    @SuppressWarnings("unchecked")
    static int dropPresetCaches(WorldEntity worldEntity, LevelIdentifier identifier) {
        try {
            Field cacheField = WorldEntity.class.getDeclaredField("worldPresetsCache");
            cacheField.setAccessible(true);
            Field regionsField = WorldPresetsRegion.class.getDeclaredField("levelRegions");
            regionsField.setAccessible(true);
            Field lastField = WorldPresetsRegion.class.getDeclaredField("lastLevelPresetsRegion");
            lastField.setAccessible(true);
            int dropped = 0;
            synchronized (worldEntity) {
                PointHashMap<WorldPresetsRegion> cache = (PointHashMap<WorldPresetsRegion>) cacheField.get(worldEntity);
                for (WorldPresetsRegion region : new ArrayList<>(cache.values())) {
                    synchronized (region) {
                        Map<LevelIdentifier, ?> levelRegions = (Map<LevelIdentifier, ?>) regionsField.get(region);
                        if (levelRegions.remove(identifier) != null) {
                            dropped++;
                        }
                        LevelPresetsRegion last = (LevelPresetsRegion) lastField.get(region);
                        if (last != null && identifier.equals(last.identifier)) {
                            lastField.set(region, null);
                        }
                    }
                }
            }
            return dropped;
        } catch (ReflectiveOperationException | RuntimeException e) {
            System.err.println("swhreset regenerate: could not drop the preset cache (" + e + ")");
            return -1;
        }
    }
}
