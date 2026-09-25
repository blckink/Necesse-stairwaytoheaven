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
        List<JournalStep> steps = new LegacyQuestSource().buildSteps(new JournalContext(client.getServer(), client));
        JournalStep step = pick(steps, JournalStatus.ACTIVE);
        if (step == null) {
            step = pick(steps, JournalStatus.AVAILABLE);
        }
        if (step == null) {
            return null;
        }
        GameMessageBuilder out = new GameMessageBuilder();
        if (step.id.equals("recruitwarden")) {
            out.append(new LocalMessage("misc", "wardenintro2")).append(" ")
                    .append(new LocalMessage("misc", "wardenrecruit1"));
        } else if (step.id.startsWith("key")) {
            // SkyWardenMob.RegionKey's askKey: "wardenkeyask" + RealmDepth.keyOf.
            String askKey = "wardenkeyask" + step.id.substring(3);
            out.append(new LocalMessage("misc", askKey));
        } else {
            String dialogKey = "wardendialog" + step.id; // wardendialogcats, wardendialoganchor
            out.append(new LocalMessage("misc", dialogKey));
        }
        out.append("\n\n").append(new LocalMessage("misc", "wardendialogtask", "title", step.title));
        for (GameMessage objective : step.objectives) {
            out.append("\n").append(objective);
        }
        line(out, "whylabel", step.why);
        line(out, "wherelabel", step.where);
        line(out, "openslabel", step.opens);
        line(out, "rewardlabel", step.reward);
        out.append("\n\n").append(new LocalMessage("misc", "wardendialogjournal"));
        return out;
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

    private static void line(GameMessageBuilder out, String labelKey, GameMessage text) {
        if (text != null) {
            out.append("\n").append(new LocalMessage("journal", labelKey)).append(" ").append(text);
        }
    }
}
