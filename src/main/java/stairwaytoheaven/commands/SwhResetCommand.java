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
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.veil.VeilWorldData;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyOrigin;

/**
 * {@code /swhreset [status|quests|world|all] [confirm]} — play an EXISTING save
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
                        new StringParameterHandler("status", "quests", "world", "all"), true),
                new CmdParameter("confirm",
                        new StringParameterHandler("confirm"), true));
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient,
                           Object[] args, String[] errors, CommandLog logs) {
        String mode = args.length > 0 && args[0] != null ? String.valueOf(args[0]) : "status";
        boolean confirmed = args.length > 1 && args[1] != null
                && "confirm".equalsIgnoreCase(String.valueOf(args[1]));

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
        logs.add("  terrain, buildings and POI presets are NOT retrofitted: they write ground, and");
        logs.add("  explored ground may be somebody's base. Walk further out for those.");
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
