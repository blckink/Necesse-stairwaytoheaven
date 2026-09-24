package stairwaytoheaven.quest.ladder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.engine.quest.Quest;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import stairwaytoheaven.journal.JournalContext;
import stairwaytoheaven.journal.JournalStatus;
import stairwaytoheaven.journal.JournalStep;
import stairwaytoheaven.journal.JournalStepSource;
import stairwaytoheaven.journal.LegacyQuestSource;
import stairwaytoheaven.journal.QuestAsks;
import stairwaytoheaven.quest.RealmVisitQuest;
import stairwaytoheaven.quest.SkyQuests;

/**
 * The Adventurer's Journal reading the Spire Village's quest ladder.
 *
 * <p>{@code journal.JournalStepSource} was left as a plug for exactly this.
 * What the journal shows per realm chapter is:
 * <ul>
 *   <li>the Warden's own line — finding the spire, recruiting him, the cats,
 *       the anchor, the fog, the chalk, the Mark and the six region keys —
 *       exactly as {@link LegacyQuestSource} builds it, because the ladder does
 *       not touch any of it; and</li>
 *   <li>every ladder step, in ladder order, with the status
 *       {@link QuestLadder} reports for this player, the resident who gives it,
 *       where it sends the player, what is still missing (with the reader's
 *       own counts), and what it pays.</li>
 * </ul>
 * The six resident chains the legacy source also listed (Eveleen's two,
 * Ives's vigil, Mortimer's rites, Caspern's forge, Eleanor, Mr. Knott's door)
 * are ladder steps now and are taken from the ladder, not twice.
 *
 * <p>Read-only and server-side, as the interface demands; a player who never
 * ascended simply sees chapter I available and everything else locked.
 */
public final class QuestLadderSource implements JournalStepSource {

    /** Legacy step IDs the ladder now owns. */
    private static final Set<String> REPLACED = new HashSet<>();

    static {
        REPLACED.add("edenreach");
        REPLACED.add("edenplants");
        REPLACED.add("steinfeldvigil");
        REPLACED.add("mortimerrites");
        REPLACED.add("caspernforge");
        REPLACED.add("eleanor");
        REPLACED.add("crookeddoor");
    }

    private final LegacyQuestSource legacy = new LegacyQuestSource();

    @Override
    public String name() {
        return "ladder";
    }

    @Override
    public List<JournalStep> buildSteps(JournalContext ctx) {
        List<JournalStep> legacySteps = this.legacy.buildSteps(ctx);
        List<JournalStep> ladderSteps = new ArrayList<>();
        for (QuestLadder.Step step : QuestLadder.steps()) {
            ladderSteps.add(toJournal(ctx, step));
        }
        // Realm by realm: the Warden's line first, then the village's.
        List<JournalStep> out = new ArrayList<>();
        for (int realm = 0; realm < QuestLadder.CHAPTERS; realm++) {
            for (JournalStep s : legacySteps) {
                if (s.realm == realm && !REPLACED.contains(s.id)) {
                    out.add(s);
                }
            }
            for (JournalStep s : ladderSteps) {
                if (s.realm == realm) {
                    out.add(s);
                }
            }
        }
        // Anything filed under a realm outside the six (none today) is kept.
        for (JournalStep s : legacySteps) {
            if ((s.realm < 0 || s.realm >= QuestLadder.CHAPTERS) && !REPLACED.contains(s.id)) {
                out.add(s);
            }
        }
        return out;
    }

    /** Chapter 1..6 is realm 0..5 ({@code RealmDepth.REALM_SKYREACH}..HELL). */
    private static JournalStep toJournal(JournalContext ctx, QuestLadder.Step step) {
        JournalStep js = new JournalStep(step.id, step.chapter - 1);
        js.title = new LocalMessage("quests", step.titleKey());
        js.description = new LocalMessage("quests", step.descKey());
        js.giver = MobRegistry.getLocalization(step.giver);
        js.where = new LocalMessage("misc", step.targetKey());
        js.reward = new LocalMessage("quests", step.rewardKey());
        js.worldScoped = step.isWorldScoped();

        QuestLadder.Status status = ctx.client != null
                ? QuestLadder.status(ctx.client, step)
                : QuestLadder.statusOffline(ctx.server, ctx.auth, step);
        switch (status) {
            case DONE:
                js.status = JournalStatus.DONE;
                return js;
            case ACTIVE:
            case READY:
                js.status = JournalStatus.ACTIVE;
                break;
            case AVAILABLE:
                js.status = JournalStatus.AVAILABLE;
                break;
            default:
                js.status = JournalStatus.LOCKED;
                break;
        }
        if (js.status == JournalStatus.LOCKED) {
            js.hint = lockHint(ctx, step);
            return js;
        }
        // What is asked, with the reader's own counts, then who takes it.
        if (DeliverItemsQuest.class.isAssignableFrom(step.questClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends DeliverItemsQuest> type = (Class<? extends DeliverItemsQuest>) step.questClass;
            for (QuestAsks.Ask ask : QuestAsks.of(type)) {
                GameMessage itemName = ItemRegistry.getLocalization(ask.item.getID());
                int have = ctx.have(ask.item);
                if (have < 0) {
                    js.objectives.add(new LocalMessage("journal", "objdeliver")
                            .addReplacement("amount", String.valueOf(ask.amount))
                            .addReplacement("item", itemName));
                } else {
                    LocalMessage line = new LocalMessage("journal", "objdeliverhave")
                            .addReplacement("amount", String.valueOf(ask.amount))
                            .addReplacement("item", itemName)
                            .addReplacement("have", String.valueOf(have));
                    js.objectives.add(check(have >= ask.amount, line));
                }
            }
        } else if (RealmVisitQuest.class.isAssignableFrom(step.questClass)) {
            Quest held = ctx.client == null ? null : SkyQuests.findHeld(ctx.client, step.questClass);
            boolean reached = held instanceof RealmVisitQuest && ((RealmVisitQuest) held).reached;
            js.objectives.add(check(reached, new LocalMessage("quests",
                    step.id.replace("_", "") + "obj")));
        } else if ("swh_eleanor".equals(step.id)) {
            js.objectives.add(new LocalMessage("quests", "swheleanorobj"));
        }
        js.objectives.add(new LocalMessage("quests", "swhladderturnin",
                "name", MobRegistry.getLocalization(step.giver)));
        return js;
    }

    /** Why a step is locked: its chapter, or the giver's earlier step. */
    private static GameMessage lockHint(JournalContext ctx, QuestLadder.Step step) {
        if (!QuestLadder.chapterOpen(ctx.server, ctx.auth, step.chapter)) {
            return new LocalMessage("misc", "swhladderhintchapter",
                    "chapter", new LocalMessage("misc", QuestLadder.chapterKey(step.chapter)));
        }
        for (QuestLadder.Step before : QuestLadder.stepsOf(step.giver)) {
            if (before == step) {
                break;
            }
            if (!before.custom && !before.isDone(ctx.server, ctx.auth)) {
                return new LocalMessage("journal", "hintafter",
                        "step", new LocalMessage("quests", before.titleKey()));
            }
        }
        return null;
    }

    private static GameMessage check(boolean done, GameMessage line) {
        return done ? new LocalMessage("journal", "objdone", "line", line)
                : new LocalMessage("journal", "objopen", "line", line);
    }
}
