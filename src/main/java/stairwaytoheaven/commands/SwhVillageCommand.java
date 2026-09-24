package stairwaytoheaven.commands;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.quest.Quest;
import necesse.engine.registries.QuestRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.quest.ladder.QuestLadder;
import stairwaytoheaven.village.SpireVillage;

/**
 * {@code /swhvillage} — the Spire Village census, for
 * {@code scripts/integration_test.sh}.
 *
 * <p>Three questions, each measured in the running server rather than read
 * out of the source:
 * <ol>
 *   <li><b>Does the village stand?</b> Every house's preset, built exactly as
 *       it was stamped, compared object by object with the world
 *       ({@code missing=} is the count of pieces the preset placed that are
 *       not on their tile).</li>
 *   <li><b>Does everybody live at home?</b> Each resident: how many of them
 *       the sky level holds (exactly one, or none if they moved to a town),
 *       how far from their seat they stand, whether that tile is indoors, and
 *       whether their {@code home} — the anchor HumanAI wanders around — is
 *       their seat.</li>
 *   <li><b>Is every ladder step reachable?</b> Registered in
 *       {@code QuestRegistry} and buildable from its ID (the path a save
 *       takes), and handed out by its giver once every earlier step is done —
 *       asked of {@link QuestLadder#wouldOffer}, the same order and chapter
 *       rules {@link QuestLadder#converse} applies. A headless server has no
 *       player to talk to, so the conversation itself is not driven here;
 *       that is recorded as a HYPOTHESIS in the chapter-03 report.</li>
 * </ol>
 */
public class SwhVillageCommand extends ModularChatCommand {

    public SwhVillageCommand() {
        super("swhvillage", "Spire Village census: houses, residents at home, quest ladder (debug)",
                PermissionLevel.ADMIN, false);
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient, Object[] args,
            String[] errors, CommandLog logs) {
        Level level = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
        if (!(level instanceof SkyLevel)) {
            logs.add("swhvillage: FAIL no sky level");
            logs.add("VILLAGE_STATUS_DONE");
            return;
        }
        // Same lock order as /skyreachstatus: the Level monitor first.
        synchronized (level) {
            run((SkyLevel) level, server, logs);
        }
        logs.add("VILLAGE_STATUS_DONE");
    }

    private void run(SkyLevel level, Server server, CommandLog logs) {
        level.ensureWardenSpire();
        SkywatchQuestData quest = SkywatchQuestData.get(level);

        // ---- 1. the houses -------------------------------------------------
        int houses = 0;
        int expectedAll = 0;
        int missingAll = 0;
        StringBuilder firstMissing = new StringBuilder();
        if (quest.villagePlaced) {
            for (SpireVillage.House house : SpireVillage.House.values()) {
                StringBuilder first = new StringBuilder();
                int[] count = SpireVillage.standing(level, house, first);
                houses++;
                expectedAll += count[0];
                missingAll += count[1];
                if (count[1] > 0 && firstMissing.length() < 200) {
                    firstMissing.append(' ').append(house.key).append(':').append(first);
                }
                logs.add("village house " + house.key + ": rot="
                        + (house.rotation == null ? "none" : house.rotation.name())
                        + " offset=" + house.offsetX + "," + house.offsetY
                        + " objects=" + (count[0] - count[1]) + "/" + count[0]
                        + " missing=" + count[1] + first);
            }
        }
        logs.add("village stamp: placed=" + quest.villagePlaced
                + " blocked=" + SpireVillage.lastBlockReason
                + " houses=" + houses + "/" + SpireVillage.House.values().length
                + " objects=" + (expectedAll - missingAll) + "/" + expectedAll
                + " missing=" + missingAll + " " + firstMissing);

        // ---- 2. the people -------------------------------------------------
        int residents = 0;
        int seated = 0;
        int inHouse = 0;
        int nearHome = 0;
        int homed = 0;
        int duplicates = 0;
        SkywatchWorldData world = SkywatchWorldData.get(server);
        for (SpireVillage.House house : SpireVillage.House.values()) {
            if (house.resident == null) {
                continue;
            }
            residents++;
            Point seat = SpireVillage.seatOf(level, house);
            level.regionManager.ensureTilesAreLoaded(seat.x - SpireVillage.HOME_RADIUS * 2,
                    seat.y - SpireVillage.HOME_RADIUS * 2, seat.x + SpireVillage.HOME_RADIUS * 2,
                    seat.y + SpireVillage.HOME_RADIUS * 2);
            int count = 0;
            HumanMob found = null;
            for (Mob mob : level.entityManager.mobs) {
                if (house.resident.equals(mob.getStringID()) && !mob.removed()) {
                    count++;
                    if (mob instanceof HumanMob) {
                        found = (HumanMob) mob;
                    }
                }
            }
            if (count > 1) {
                duplicates++;
            }
            String line = "village resident " + house.resident + ": house=" + house.key
                    + " seat=" + seat.x + "," + seat.y + " count=" + count;
            if (found != null) {
                seated++;
                int dx = found.getTileX() - seat.x;
                int dy = found.getTileY() - seat.y;
                int dist = Math.max(Math.abs(dx), Math.abs(dy));
                boolean inside = !level.isOutside(found.getTileX(), found.getTileY());
                boolean home = found.home != null && found.home.x == seat.x && found.home.y == seat.y;
                boolean near = dist <= SpireVillage.HOME_RADIUS * 2;
                if (inside && dist <= 12) {
                    inHouse++;
                }
                if (near) {
                    nearHome++;
                }
                if (home) {
                    homed++;
                }
                line += " at=" + found.getTileX() + "," + found.getTileY() + " dist=" + dist
                        + " inside=" + (inside ? 1 : 0) + " home=" + (home ? 1 : 0)
                        + " nearhome=" + (near ? 1 : 0) + " settler=" + (found.isSettler() ? 1 : 0);
            } else {
                line += " at=none";
            }
            line += " claimed=" + (world != null && world.residentsClaimed.contains(house.resident) ? 1 : 0)
                    + " villager=" + (world != null && world.villageResidents.contains(house.resident) ? 1 : 0);
            logs.add(line);
        }
        logs.add("village residents: residents=" + residents + " seated=" + seated
                + " inhouse=" + inHouse + " nearhome=" + nearHome + " homed=" + homed
                + " duplicates=" + duplicates);

        // ---- 3. the ladder --------------------------------------------------
        int registered = 0;
        int builds = 0;
        int offered = 0;
        int custom = 0;
        Set<String> done = new HashSet<>();
        for (QuestLadder.Step step : QuestLadder.steps()) {
            int id = QuestRegistry.getQuestID(step.id);
            boolean reg = id >= 0 && QuestRegistry.getQuestID(step.questClass) == id;
            boolean built;
            try {
                Quest q = QuestRegistry.getNewQuest(step.id);
                built = q != null && step.questClass.isInstance(q) && step.newQuest() != null;
            } catch (Throwable t) {
                built = false;
            }
            // Every step before this one in ladder order is treated as done:
            // the giver must then hand THIS one out, or nobody ever will.
            String offer;
            if (step.custom) {
                offer = "custom";
                custom++;
            } else {
                QuestLadder.Step next = QuestLadder.wouldOffer(step.giver, done);
                offer = next == step ? "1" : ("0(" + (next == null ? "none" : next.id) + ")");
                if (next == step) {
                    offered++;
                }
            }
            done.add(step.id);
            if (reg) {
                registered++;
            }
            if (built) {
                builds++;
            }
            logs.add("ladder step " + step.id + ": chapter=" + step.chapter + " giver=" + step.giver
                    + " registered=" + (reg ? 1 : 0) + " builds=" + (built ? 1 : 0)
                    + " offered=" + offer + " gating=" + (step.gating ? 1 : 0));
        }
        logs.add("village ladder: steps=" + QuestLadder.steps().size() + " registered=" + registered
                + " builds=" + builds + " offered=" + offered + " custom=" + custom);
    }
}
