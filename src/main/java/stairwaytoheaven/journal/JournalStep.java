package stairwaytoheaven.journal;

import java.util.ArrayList;
import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

/**
 * One story step or quest, as the journal shows it.
 *
 * <p>This is the unit a {@link JournalStepSource} produces, and deliberately
 * the only shape it has to produce: whoever owns the quest ladder only has to
 * say, per step, which realm chapter it belongs to, what it is called, who
 * gives it and where they live, and where the reading player stands on it.
 * Everything else a chapter shows (keys, bosses, residents, landmarks, lore)
 * is built by {@link JournalBuilder} from the world records and does not
 * depend on how the quests are organised.
 */
public final class JournalStep {

    /** Stable ID, for {@code /swhjournal} and for tests. Not shown. */
    public final String id;
    /** {@code RealmDepth.REALM_*} — which chapter this step is filed under. */
    public final int realm;
    public JournalStatus status = JournalStatus.LOCKED;
    /**
     * True when finishing this step is a fact about the WORLD (every player on
     * it sees it done), false when it is about the reading player only. Shown,
     * because in co-op "done" otherwise reads as a bug to the second player.
     */
    public boolean worldScoped;
    public GameMessage title;
    public GameMessage description;
    /** Who hands it out / takes it in. May be null (a place, not a person). */
    public GameMessage giver;
    /** Where that giver lives, or where the step happens. May be null. */
    public GameMessage where;
    /** Why it is locked, or what to do next. May be null. */
    public GameMessage hint;
    /** What is asked, one line each, with the player's own count where known. */
    public final List<GameMessage> objectives = new ArrayList<>();
    /** What it pays. May be null. */
    public GameMessage reward;
    /**
     * Why the player is asked for this at all, in one or two sentences. The
     * playtest of 2026-09-24: "man checkt null warum man was jetzt bauen muss
     * und was es macht". May be null.
     */
    public GameMessage why;
    /** What finishing it opens up, beyond the items in {@link #reward}. May be null. */
    public GameMessage opens;

    public JournalStep(String id, int realm) {
        this.id = id;
        this.realm = realm;
    }

    public JournalStep status(JournalStatus status) {
        this.status = status;
        return this;
    }

    void write(PacketWriter writer) {
        writer.putNextString(this.id);
        writer.putNextByteUnsigned(this.realm);
        writer.putNextByteUnsigned(this.status.ordinal());
        writer.putNextBoolean(this.worldScoped);
        JournalBook.writeMessage(writer, this.title);
        JournalBook.writeMessage(writer, this.description);
        JournalBook.writeMessage(writer, this.giver);
        JournalBook.writeMessage(writer, this.where);
        JournalBook.writeMessage(writer, this.hint);
        writer.putNextByteUnsigned(Math.min(255, this.objectives.size()));
        for (int i = 0; i < Math.min(255, this.objectives.size()); i++) {
            JournalBook.writeMessage(writer, this.objectives.get(i));
        }
        JournalBook.writeMessage(writer, this.reward);
        JournalBook.writeMessage(writer, this.why);
        JournalBook.writeMessage(writer, this.opens);
    }

    static JournalStep read(PacketReader reader) {
        String id = reader.getNextString();
        int realm = reader.getNextByteUnsigned();
        JournalStep step = new JournalStep(id, realm);
        step.status = JournalStatus.of(reader.getNextByteUnsigned());
        step.worldScoped = reader.getNextBoolean();
        step.title = JournalBook.readMessage(reader);
        step.description = JournalBook.readMessage(reader);
        step.giver = JournalBook.readMessage(reader);
        step.where = JournalBook.readMessage(reader);
        step.hint = JournalBook.readMessage(reader);
        int objectives = reader.getNextByteUnsigned();
        for (int i = 0; i < objectives; i++) {
            GameMessage objective = JournalBook.readMessage(reader);
            if (objective != null) {
                step.objectives.add(objective);
            }
        }
        step.reward = JournalBook.readMessage(reader);
        step.why = JournalBook.readMessage(reader);
        step.opens = JournalBook.readMessage(reader);
        return step;
    }
}
