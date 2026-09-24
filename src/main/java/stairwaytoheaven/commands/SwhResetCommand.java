package stairwaytoheaven.commands;

import java.util.ArrayList;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.StringParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.quest.Quest;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyCloudmarbleSet;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.level.SkyRegenerator;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.veil.VeilWorldData;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.WardenSpirePreset;

/**
 * {@code /swhreset [status|quests|world|all|regenerate] [confirm]} — play an EXISTING save
 * from the top, and pick up content that shipped after it was made.
 *
 * <h2>Why this exists</h2>
 * The mod is additive and save-compatible by policy ({@code ROADMAP.md},
 * "Compatibility policy"), and that policy is exactly what makes a long-running
 * world hard to test in: it keeps every flag it has ever set, and it keeps
 * every region exactly as the build that generated it left it. Two different
 * things go wrong, and they need two different repairs.
 *
 * <ol>
 * <li><b>The story is finished and cannot be replayed.</b> The Warden has been
 *     recruited, the cats are home, the anchor is delivered, the region keys are
 *     earned — all of it recorded in {@code SkywatchQuestData},
 *     {@code SkywatchWorldData} and {@code VeilWorldData}, all of it one-way by
 *     design. {@code quests} puts those three back to zero.</li>
 * <li><b>Explored ground is frozen at the build that generated it.</b>
 *     {@code SkyLevel.onRegionGenerated} fires once per region ever, so a world
 *     walked before 2026-09-03 has no boss portals anywhere it has been and no
 *     amount of playing will produce one. {@code world} re-walks the lattices
 *     over already-generated ground — see {@link SkyLevel#retrofitArea} for
 *     what that can and cannot repair.</li>
 * </ol>
 *
 * <h2>The safety rules, and why each one is here</h2>
 * <ul>
 * <li><b>Bare {@code /swhreset} reports and changes nothing.</b> The default
 *     mode is {@code status}. A destructive command whose zero-argument form is
 *     destructive is a command that eventually eats somebody's world.</li>
 * <li><b>{@code quests} and {@code all} need the literal word
 *     {@code confirm}.</b> Without it they print exactly what they WOULD clear
 *     and stop.</li>
 * <li><b>ADMIN, like every other command this mod adds.</b></li>
 * <li><b>The surface is never touched.</b> {@code docs/DESIGN_DECISIONS.md}:
 *     <i>"Surface data is never touched by Skyreach migration. Any migration
 *     code that could reset Surface state is a bug, not a trade-off."</i>
 *     Nothing here reads or writes a surface level, an inventory, a settlement
 *     or a player's items.</li>
 * </ul>
 *
 * <h2>What a reset cannot undo, and says so</h2>
 * The three data classes hold FLAGS. Clearing them does not evict a Warden who
 * already lives in a settlement, delete a key piece already built, take back a
 * Stormsteel Vambrace already awarded, or remove a Séance Circle already drawn.
 * The report names every one of the mod's named residents still standing in the
 * world so the operator can decide what to do about them, and
 * {@code residentsClaimed} is only cleared by {@code all} — see
 * {@link SkywatchWorldData#resetProgress(boolean)} for the duplicate-Magpie
 * problem that guard exists to prevent.
 *
 * <p>The whole picture, including which build added which content and what a
 * given old save is therefore missing, is {@code docs/SAVE_COMPAT.md}.
 */
public class SwhResetCommand extends ModularChatCommand {

    /**
     * How far around the caller {@code world} repairs, in tiles.
     *
     * <p>512 is a little over one screen-load of regions in each direction and
     * about 1.05 million tiles — enough to cover a base and its surroundings in
     * one call, small enough that the scan and the forced region loads finish
     * in a few seconds rather than hanging the server thread. Repairing a whole
     * explored world is deliberately several deliberate calls rather than one
     * command that walks an unbounded area.
     */
    private static final int RETROFIT_RADIUS = 512;

    /** The mod's named one-per-world people, for the "still standing" report. */
    private static final String[] NAMED_RESIDENTS = {
            "skywarden", "wardensettler", "magpiesettler", "haldasettler", "ossiansettler",
            "eveleensettler", "mortimersettler", "caspernsettler", "eleanorsettler",
            "knottsettler",
    };

    public SwhResetCommand() {
        super("swhreset",
                "Reports, resets or retrofits this world's Stairway to Heaven state (debug)",
                PermissionLevel.ADMIN, false,
                new CmdParameter("mode",
                        new StringParameterHandler("status", "quests", "world", "all", "regenerate"), true),
                // Default NULL, "confirm" only as the autocomplete. The first
                // argument of StringParameterHandler is the DEFAULT VALUE
                // (jar 1.3.2, StringParameterHandler.java:18-21), and an
                // omitted optional parameter is filled with it
                // (CmdParameter.addDefaults). This used to read
                // StringParameterHandler("confirm"), which made every omitted
                // "confirm" arrive as the word "confirm": /swhreset quests
                // without it was destructive, the opposite of what it printed.
                new CmdParameter("confirm",
                        new StringParameterHandler(null, "confirm"), true));
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient,
                           Object[] args, String[] errors, CommandLog logs) {
        String mode = args.length > 0 && args[0] != null ? String.valueOf(args[0]) : "status";
        boolean confirmed = args.length > 1 && args[1] != null
                && "confirm".equalsIgnoreCase(String.valueOf(args[1]));

        // Before the level is asked for: a dry run on a world that has never
        // been up must not generate a sky just to report that it would delete
        // it, and the real run unloads the level rather than locking it.
        if ("regenerate".equals(mode)) {
            regenerate(server, serverClient, confirmed, logs);
            logs.add("SWH_RESET_DONE");
            return;
        }
        if ("fixture".equals(mode)) {
            RegenerateFixture.run(server,
                    args.length > 1 && args[1] != null ? String.valueOf(args[1]) : "", logs);
            logs.add("SWH_RESET_DONE");
            return;
        }

        Level level = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
        if (!(level instanceof SkyLevel)) {
            logs.add("FAIL: level for identifier \"" + SkyRegistry.SKYREACH_IDENTIFIER + "\" is "
                    + level.getClass().getSimpleName() + " (expected SkyLevel)");
            return;
        }
        SkyLevel sky = (SkyLevel) level;

        // Same lock order as SkyreachStatusCommand, and for the same reason:
        // this thread takes region locks inside retrofitArea while the server
        // thread ticks the level, and the server thread's own order is Level
        // monitor -> region locks. Taking the Level monitor first gives both
        // threads one order and removes the deadlock that command documents.
        synchronized (level) {
            switch (mode) {
                case "quests":
                    resetQuests(server, sky, confirmed, logs);
                    break;
                case "world":
                    retrofit(server, sky, serverClient, logs);
                    break;
                case "all":
                    resetQuests(server, sky, confirmed, logs);
                    if (confirmed) {
                        clearResidentClaims(server, logs);
                        retrofit(server, sky, serverClient, logs);
                    }
                    break;
                default:
                    reportStatus(server, sky, logs);
                    break;
            }
        }
        logs.add("SWH_RESET_DONE");
    }

    // ------------------------------------------------------------------
    // status
    // ------------------------------------------------------------------

    private void reportStatus(Server server, SkyLevel sky, CommandLog logs) {
        SkywatchQuestData quest = SkywatchQuestData.get(sky);
        SkywatchWorldData world = SkywatchWorldData.get(server);
        VeilWorldData veil = VeilWorldData.get(server);

        logs.add("swhreset: reporting only. Nothing was changed.");
        logs.add("  story  stage=" + quest.stage
                + " recruited=" + quest.recruited
                + " cats=" + (quest.blackHome ? "1" : "0") + (quest.tabbyHome ? "1" : "0")
                + " anchor=" + quest.anchorDone
                + " spire=" + (quest.spirePlaced ? quest.spireX + "," + quest.spireY : "unplaced"));
        if (world != null) {
            logs.add("  world  warden=" + world.wardenRecruited
                    + " eleanorPassedOn=" + world.eleanorPassedOn
                    + " edenPlants=" + world.edenPlantsGiven
                    + " crookedDoor=" + world.crookedDoorwayOpened);
            logs.add("  keys   earned=" + world.regionKeysEarned
                    + " portalsUnlocked=" + world.bossPortalsUnlocked);
            logs.add("  claims residents=" + world.residentsClaimed);
        } else {
            logs.add("  world  <no SkywatchWorldData on this world>");
        }
        if (veil != null) {
            logs.add("  fog    marks=" + veil.markCount() + " touchedFog=" + veil.fogTouchedCount());
        }
        logs.add("  quests held=" + countModQuests(server) + " journal entries across all players");

        for (String line : describeStandingResidents(sky)) {
            logs.add("  " + line);
        }
        logs.add("  portals in " + RETROFIT_RADIUS + " tiles of the spire: "
                + countPortalsAroundSpire(sky, quest));
        logs.add("run  /swhreset quests confirm   to replay the chain from the top");
        logs.add("run  /swhreset world            to retrofit ground generated by an older build");
        logs.add("run  /swhreset regenerate       to see what generating the whole sky anew would do");
    }

    // ------------------------------------------------------------------
    // regenerate
    // ------------------------------------------------------------------

    /**
     * The whole mod plane, thrown away and generated again, progress kept —
     * see {@link SkyRegenerator} for the mechanism and every decision in it.
     *
     * <h2>Which thread runs it</h2>
     * It unloads a level, and the tick loop must not be ticking that level
     * while it does. A command typed in CHAT (single player included) runs on
     * the server thread already ({@code Server.frameTick} processes packets,
     * jar 1.3.2 Server.java:251-254), so it runs now. A command typed into a
     * dedicated server's CONSOLE runs on the console thread
     * ({@code ServerLoader.handleCommand}); there it runs only while the server
     * is paused (nobody online and {@code pauseWhenEmpty}: {@code World.serverTick}
     * and {@code frameTick} are skipped, Server.java:262 and :318), and is
     * refused otherwise. Handing it to the server thread through a
     * {@code WorldData.tick} was considered and rejected: {@code WorldEntity}
     * calls those while iterating its world-data HashMap (WorldEntity.java:584),
     * and the regenerate can create world data lazily, which would throw a
     * ConcurrentModificationException in the middle of the delete.
     */
    private void regenerate(Server server, ServerClient serverClient, boolean confirmed, CommandLog logs) {
        if (!confirmed) {
            logs.add("swhreset regenerate: NOTHING WAS CHANGED - add the word 'confirm'.");
            SkyRegenerator.Plan plan = SkyRegenerator.computePlan(server);
            if (!plan.levelExists) {
                logs.add("  this world has no sky yet - there is nothing to regenerate.");
                return;
            }
            logs.add("  it would DELETE the whole sky level '" + SkyRegistry.SKYREACH_IDENTIFIER.stringID
                    + "' - every realm, the Warden's Spire and everything built up there -");
            logs.add("  and generate it again with this build. Quest progress is kept.");
            SkyRegenerator.describe(plan, logs::add);
            logs.add("  a backup of the sky's files goes to <game data>/" + SkyRegenerator.BACKUP_FOLDER
                    + "/ first.");
            return;
        }
        if (serverClient == null && !server.isPaused()) {
            logs.add("swhreset regenerate: REFUSED from the console while the server is running.");
            logs.add("  Type it in the game chat as an admin (that runs on the server thread),");
            logs.add("  or run it here while nobody is online (the server pauses and nothing ticks).");
            return;
        }
        SkyRegenerator.run(server, logs::add);
    }

    // ------------------------------------------------------------------
    // quests
    // ------------------------------------------------------------------

    private void resetQuests(Server server, SkyLevel sky, boolean confirmed, CommandLog logs) {
        SkywatchQuestData quest = SkywatchQuestData.get(sky);
        SkywatchWorldData world = SkywatchWorldData.get(server);
        VeilWorldData veil = VeilWorldData.get(server);
        int journal = countModQuests(server);

        if (!confirmed) {
            logs.add("swhreset quests: NOTHING WAS CHANGED - add the word 'confirm'.");
            logs.add("  it would clear: story stage " + quest.stage + ", recruited="
                    + quest.recruited + ", both cat flags, the anchor,");
            logs.add("  " + (world == null ? 0 : world.regionKeysEarned.size())
                    + " region key(s), " + (world == null ? 0 : world.bossPortalsUnlocked.size())
                    + " portal unlock(s), " + (veil == null ? 0 : veil.markCount())
                    + " Veil Mark(s) and the chalk ledger,");
            logs.add("  and remove " + journal + " journal quest(s) from every player.");
            return;
        }

        quest.resetProgress();
        if (world != null) {
            world.resetProgress(false);
        }
        int fogRecords = veil == null ? 0 : veil.resetProgress();
        int removed = removeModQuests(server);
        boolean warden = sky.restoreSpireWarden();

        logs.add("swhreset quests: the chain is back at the start.");
        logs.add("  story flags cleared, " + removed + " journal quest(s) removed, "
                + fogRecords + " fog/chalk record(s) dropped");
        logs.add("  spire Warden " + (warden ? "put back at " + quest.spireX + "," + quest.spireY
                : "not replaced (one is already standing, or no spire is stamped)"));
        logs.add("  resident claims KEPT - use '/swhreset all confirm' to clear those too");
        for (String line : describeStandingResidents(sky)) {
            logs.add("  " + line);
        }
        logs.add("  NOT undone (they are objects and items, not flags): key pieces already built,");
        logs.add("  quest rewards already in an inventory, Seance Circles already drawn,");
        logs.add("  and settlers who already moved into a settlement on the surface.");
    }

    private void clearResidentClaims(Server server, CommandLog logs) {
        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world == null) {
            return;
        }
        int claimed = world.residentsClaimed.size();
        world.residentsClaimed.clear();
        logs.add("swhreset all: " + claimed + " resident claim(s) cleared - worldgen may now stand");
        logs.add("  up a SECOND copy of anyone who is still alive. Remove them first if that matters.");
    }

    // ------------------------------------------------------------------
    // world
    // ------------------------------------------------------------------

    private void retrofit(Server server, SkyLevel sky, ServerClient serverClient, CommandLog logs) {
        int seed = sky.getWorldGenSeed();
        int centreX = SkyOrigin.originX(seed);
        int centreY = SkyOrigin.originY(seed);
        String where = "the Warden's Spire";
        // Around the caller when they are actually up here: a player asking for
        // a repair almost always means "the ground I am standing on", and their
        // base is rarely at the canonical origin.
        if (serverClient != null && serverClient.playerMob != null
                && SkyRegistry.SKYREACH_IDENTIFIER.equals(serverClient.getLevelIdentifier())) {
            centreX = serverClient.playerMob.getTileX();
            centreY = serverClient.playerMob.getTileY();
            where = "you";
        }
        logs.add("swhreset world: repairing " + (RETROFIT_RADIUS * 2) + "x" + (RETROFIT_RADIUS * 2)
                + " tiles around " + where + " (" + centreX + "," + centreY + ")");
        SkyLevel.RetrofitReport report = sky.retrofitArea(centreX, centreY, RETROFIT_RADIUS);
        logs.add("  " + report);
        if (report.mobsAdded == 0 && report.portalsAdded == 0) {
            logs.add("  nothing was missing here - this ground already matches the current build");
        }
        int repaved = repaveSpire(sky, SkywatchQuestData.get(sky));
        if (repaved > 0) {
            logs.add("  spire floor: " + repaved + " skyway tile(s) relaid as skyway path");
        }
        logs.add("  terrain, buildings and POI presets are NOT retrofitted: they write ground, and");
        logs.add("  explored ground may be somebody's base. Walk further out for those.");
    }

    /**
     * Relays the Warden's spire floor with the skyway path tile. Spires stamped
     * before the path tile existed are paved with the skyway splat, whose
     * frayed edges the player asked to be rid of. Only skyway tiles inside the
     * spire's own square change, and only loaded ones; nothing else is touched.
     */
    private static int repaveSpire(SkyLevel sky, SkywatchQuestData quest) {
        int from = SkyCloudmarbleSet.skywayTileID;
        int to = SkyCloudmarbleSet.skywayPathTileID;
        if (!quest.spirePlaced || from == to) {
            return 0;
        }
        int left = quest.spireX - WardenSpirePreset.WARDEN_X;
        int top = quest.spireY - WardenSpirePreset.WARDEN_Y;
        int changed = 0;
        for (int tileX = left; tileX < left + WardenSpirePreset.SIZE; tileX++) {
            for (int tileY = top; tileY < top + WardenSpirePreset.SIZE; tileY++) {
                if (!sky.regionManager.isTileLoaded(tileX, tileY) || sky.getTileID(tileX, tileY) != from) {
                    continue;
                }
                sky.setTile(tileX, tileY, to);
                sky.sendTileUpdatePacket(tileX, tileY);
                changed++;
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------
    // shared readings
    // ------------------------------------------------------------------

    /**
     * The mod's journal quests, identified by the package their class is in.
     *
     * <p>Matching on the package rather than on a list of registry IDs is
     * deliberate: a list has to be kept in step with
     * {@code StairwayToHeavenMod}'s fifteen {@code registerQuest} calls and
     * silently misses the sixteenth. Nothing outside this mod puts a class in
     * {@code stairwaytoheaven.quest}.
     */
    private static boolean isModQuest(Quest quest) {
        return quest.getClass().getName().startsWith("stairwaytoheaven.");
    }

    private static int countModQuests(Server server) {
        int count = 0;
        for (Quest quest : server.world.getQuests().getQuests()) {
            if (isModQuest(quest)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Removes every mod quest from the world manager.
     *
     * <p>Collected first and removed after, rather than removed while iterating:
     * {@code SkyQuests.removeAllOfType} does the same, for the same reason —
     * {@code QuestManager.removeQuest} mutates the collection
     * {@code getQuests()} is walking. It also broadcasts a
     * {@code PacketQuestRemove} to that quest's active clients, so every
     * player's journal empties without anyone reconnecting.
     */
    private static int removeModQuests(Server server) {
        ArrayList<Quest> doomed = new ArrayList<>();
        for (Quest quest : server.world.getQuests().getQuests()) {
            if (isModQuest(quest)) {
                doomed.add(quest);
            }
        }
        for (Quest quest : doomed) {
            server.world.getQuests().removeQuest(quest);
        }
        return doomed.size();
    }

    /** One line per named resident still alive in the sky, or one saying none is. */
    private static ArrayList<String> describeStandingResidents(SkyLevel sky) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder standing = new StringBuilder();
        for (String who : NAMED_RESIDENTS) {
            int count = 0;
            for (Mob mob : sky.entityManager.mobs) {
                if (who.equals(mob.getStringID())) {
                    count++;
                }
            }
            if (count > 0) {
                if (standing.length() > 0) {
                    standing.append(", ");
                }
                standing.append(who).append(count > 1 ? " x" + count : "");
            }
        }
        // Only the sky level is scanned, and the report says so: a settler who
        // moved in lives on the SURFACE, and this command does not read surface
        // levels at all (docs/DESIGN_DECISIONS.md).
        lines.add("standing in the sky: " + (standing.length() == 0 ? "nobody" : standing));
        lines.add("  (settlers who moved into a surface settlement are not counted - "
                + "this command never reads a surface level)");
        return lines;
    }

    private static int countPortalsAroundSpire(SkyLevel sky, SkywatchQuestData quest) {
        if (!quest.spirePlaced) {
            return 0;
        }
        int count = 0;
        for (int tileX = quest.spireX - RETROFIT_RADIUS; tileX <= quest.spireX + RETROFIT_RADIUS; tileX++) {
            for (int tileY = quest.spireY - RETROFIT_RADIUS; tileY <= quest.spireY + RETROFIT_RADIUS; tileY++) {
                // Loaded tiles only: the status mode must stay a cheap read and
                // must never force half a million tiles of region generation.
                if (!sky.regionManager.isTileLoaded(tileX, tileY)) {
                    continue;
                }
                int objectID = sky.getObjectID(tileX, tileY);
                if (objectID == 0) {
                    continue;
                }
                for (int realm = 0; realm < RealmDepth.REALM_COUNT; realm++) {
                    if (objectID == stairwaytoheaven.bosses.BossPortalObject.portalID(realm)) {
                        count++;
                        break;
                    }
                }
            }
        }
        return count;
    }
}
