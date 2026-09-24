package stairwaytoheaven.journal;

import java.util.ArrayList;
import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

/**
 * One realm's chapter: its story steps, then what the player has earned and
 * found there.
 */
public final class JournalChapter {

    /** {@code RealmDepth.REALM_*}. */
    public final int realm;
    public GameMessage name;
    /** One line: where the realm lies and what it is like. */
    public GameMessage intro;
    public final List<JournalStep> steps = new ArrayList<>();
    /** Visited, region key, key piece standing, boss. */
    public final List<JournalLine> facts = new ArrayList<>();
    public final List<JournalLine> residents = new ArrayList<>();
    public final List<JournalLine> landmarks = new ArrayList<>();
    public final List<JournalLine> lore = new ArrayList<>();

    public JournalChapter(int realm) {
        this.realm = realm;
    }

    /** Steps finished, for the chapter button. */
    public int doneSteps() {
        int done = 0;
        for (JournalStep step : this.steps) {
            if (step.status == JournalStatus.DONE) {
                done++;
            }
        }
        return done;
    }

    /** One status letter per step, in order — what {@code /swhjournal} prints. */
    public String codes() {
        StringBuilder out = new StringBuilder();
        for (JournalStep step : this.steps) {
            out.append(step.status.code);
        }
        return out.toString();
    }

    void write(PacketWriter writer) {
        writer.putNextByteUnsigned(this.realm);
        JournalBook.writeMessage(writer, this.name);
        JournalBook.writeMessage(writer, this.intro);
        writer.putNextShortUnsigned(this.steps.size());
        for (JournalStep step : this.steps) {
            step.write(writer);
        }
        writeLines(writer, this.facts);
        writeLines(writer, this.residents);
        writeLines(writer, this.landmarks);
        writeLines(writer, this.lore);
    }

    static JournalChapter read(PacketReader reader) {
        JournalChapter chapter = new JournalChapter(reader.getNextByteUnsigned());
        chapter.name = JournalBook.readMessage(reader);
        chapter.intro = JournalBook.readMessage(reader);
        int steps = reader.getNextShortUnsigned();
        for (int i = 0; i < steps; i++) {
            chapter.steps.add(JournalStep.read(reader));
        }
        readLines(reader, chapter.facts);
        readLines(reader, chapter.residents);
        readLines(reader, chapter.landmarks);
        readLines(reader, chapter.lore);
        return chapter;
    }

    private static void writeLines(PacketWriter writer, List<JournalLine> lines) {
        writer.putNextShortUnsigned(lines.size());
        for (JournalLine line : lines) {
            line.write(writer);
        }
    }

    private static void readLines(PacketReader reader, List<JournalLine> into) {
        int size = reader.getNextShortUnsigned();
        for (int i = 0; i < size; i++) {
            into.add(JournalLine.read(reader));
        }
    }
}
