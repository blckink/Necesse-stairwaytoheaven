package stairwaytoheaven.journal;

import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;

/**
 * What the Warden says when the dialogue window opens: the task he has
 * handed this player, read from the same journal step the book shows.
 *
 * <p>The playtest of 2026-09-24 read his quests off speech bubbles: "Durch die
 * Textblasen ist null lesbar was man machen soll". A bubble fades and is
 * replaced by the next one ({@code ChatBubbleText.init}); the dialogue
 * window's intro text is a scrollable {@code FormFairTypeLabel} that stays
 * open until the player closes it ({@code DialogueForm.addText},
 * VERIFIED [jar] 1.3.3). {@code HumanShop.interact} asks for it only after
 * {@code SkyWardenMob.interact} has handed out or turned in whatever was due
 * (HumanShop.java:247, VERIFIED [jar]), so the task shown is the one just
 * given, not the one before it.
 *
 * <p>Nothing here decides anything: it builds the player's
 * {@link LegacyQuestSource} steps and words the first live one of the Warden's,
 * so the dialogue and the journal cannot disagree.
 */
public final class WardenBrief {

    /** The steps he hands out on being spoken to, in the order he does. */
    private static final String[] ASKS = {
            "recruitwarden", "cats", "anchor",
            "keyskyreach", "keyeden", "keysteinfeld", "keyghostrealm", "keycrookedbeyond", "keyhell"};

    private WardenBrief() {
    }

    /**
     * The dialogue intro for this player, or null when the Warden has nothing
     * open for them (the caller then falls back to his small talk).
     */
    public static GameMessage dialogue(ServerClient client) {
        if (client == null || client.getServer() == null) {
            return null;
        }
        List<JournalStep> steps = AdventurerJournal.stepSource().buildSteps(
                new JournalContext(client.getServer(), client));
        JournalStep step = pick(steps, JournalStatus.READY);
        if (step == null) {
            step = pick(steps, JournalStatus.ACTIVE);
        }
        if (step == null) {
            step = pick(steps, JournalStatus.AVAILABLE);
        }
        if (step == null) {
            return null;
        }
        GameMessage line;
        if (step.id.equals("recruitwarden")) {
            line = new GameMessageBuilder().append(new LocalMessage("misc", "wardenintro2")).append(" ")
                    .append(new LocalMessage("misc", "wardenrecruit1"));
        } else if (step.id.startsWith("key")) {
            // SkyWardenMob.RegionKey's askKey: "wardenkeyask" + RealmDepth.keyOf.
            String askKey = "wardenkeyask" + step.id.substring(3);
            line = new LocalMessage("misc", askKey);
        } else {
            String dialogKey = "wardendialog" + step.id; // wardendialogcats, wardendialoganchor
            line = new LocalMessage("misc", dialogKey);
        }
        return task(line, step);
    }

    /**
     * The one shape every quest giver's dialogue has (2026-09-26, the player:
     * "total nervig ... bei warden, im Buch und als Aufgabe quests zu haben"):
     * the giver's own words, the task and what is still missing, and a pointer
     * to the Sky Chronicle. Why, where, what it opens and the reward live in
     * the Chronicle only; the vanilla quest tracker keeps the progress on
     * screen. The same builder serves the Warden and the Spire Village.
     */
    /**
     * The marker over the Warden's head for {@link #dialogue}'s pick, in the
     * same order: READY -> yellow "?", the recruitment offer -> yellow "!",
     * ACTIVE -> grey "?".
     */
    public static int markerCode(ServerClient client) {
        if (client == null || client.getServer() == null) {
            return stairwaytoheaven.quest.ladder.QuestMarkerSync.NONE;
        }
        List<JournalStep> steps = AdventurerJournal.stepSource().buildSteps(
                new JournalContext(client.getServer(), client));
        if (pick(steps, JournalStatus.READY) != null) {
            return stairwaytoheaven.quest.ladder.QuestMarkerSync.READY;
        }
        // "!" only for the recruitment: the other asks (cats, anchor, keys)
        // stay AVAILABLE until the player acts out in the world, not by
        // talking, so a "!" for them would never go away.
        JournalStep offered = pick(steps, JournalStatus.AVAILABLE);
        if (offered != null && offered.id.equals("recruitwarden")) {
            return stairwaytoheaven.quest.ladder.QuestMarkerSync.NEW;
        }
        if (pick(steps, JournalStatus.ACTIVE) != null) {
            return stairwaytoheaven.quest.ladder.QuestMarkerSync.ACTIVE;
        }
        return stairwaytoheaven.quest.ladder.QuestMarkerSync.NONE;
    }

    public static GameMessage task(GameMessage line, JournalStep step) {
        GameMessageBuilder out = new GameMessageBuilder();
        if (line != null) {
            out.append(line).append("\n\n");
        }
        out.append(new LocalMessage("misc", "wardendialogtask", "title", step.title));
        for (GameMessage objective : step.objectives) {
            if (isTurnInLine(objective)) {
                continue; // "bring it to <this person>" - the reader is talking to them
            }
            out.append("\n").append(objective);
        }
        if (step.status == JournalStatus.READY) {
            out.append("\n\n").append(new LocalMessage("misc", "questbriefready"));
        }
        out.append("\n\n").append(new LocalMessage("misc", "wardendialogjournal"));
        return out;
    }

    private static boolean isTurnInLine(GameMessage objective) {
        return objective instanceof LocalMessage
                && ("swhladderturnin".equals(((LocalMessage) objective).key)
                        || "swhreturnwarden".equals(((LocalMessage) objective).key));
    }

    private static JournalStep pick(List<JournalStep> steps, JournalStatus status) {
        for (String id : ASKS) {
            for (JournalStep step : steps) {
                if (step.id.equals(id) && step.status == status) {
                    return step;
                }
            }
        }
        return null;
    }
}
