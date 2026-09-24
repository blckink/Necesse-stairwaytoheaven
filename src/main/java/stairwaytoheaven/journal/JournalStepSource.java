package stairwaytoheaven.journal;

import java.util.List;

/**
 * Where the journal's story steps come from.
 *
 * <p>THE PLUG FOR THE QUEST LADDER. The journal was built while the NPCs were
 * being reorganised into the Spire Village with a new quest ladder, so the
 * step list is behind this interface rather than wired to the quest classes:
 *
 * <ul>
 * <li>{@link LegacyQuestSource} is the implementation today. It reads the
 *     existing quests and world flags exactly as {@code docs/KOMPLETTUEBERSICHT.md}
 *     §1-§2 documents them.</li>
 * <li>When the ladder lands, write a {@code QuestLadderSource implements
 *     JournalStepSource} that walks {@code QuestLadder}'s steps and maps each
 *     one to a {@link JournalStep} — realm, title, giver, where, the status
 *     the ladder reports for {@code ctx.auth}, objectives, reward — and install
 *     it with {@link AdventurerJournal#setStepSource} from the ladder's own
 *     registration (or change the one default in {@code AdventurerJournal}).
 *     Nothing else in this package changes: chapters, residents, landmarks,
 *     lore, keys, bosses, the packet and the form are all independent of how
 *     the steps were produced.</li>
 * </ul>
 *
 * <p>Contract: server-side only; must not throw for a player who has never
 * ascended (the Skyreach record may be null); must not change any state —
 * opening a book is a read. Order matters: steps are shown in the order
 * returned, filed under {@link JournalStep#realm}.
 */
public interface JournalStepSource {

    /** Short name for {@code /swhjournal}, e.g. {@code "legacy"} or {@code "ladder"}. */
    String name();

    /** Every story step, for the player (or world) {@code ctx} describes. */
    List<JournalStep> buildSteps(JournalContext ctx);
}
