package stairwaytoheaven.journal;

/**
 * Where one journal entry stands for the player reading it.
 *
 * <p>Four states, the ones the player asked to see ("locked / available at
 * &lt;NPC&gt; / active with objective / done with reward received"). The
 * one-letter {@link #code} is what {@code /swhjournal} prints, so the
 * integration test can assert a whole chapter in one regex.
 */
public enum JournalStatus {
    /** Something earlier has to happen first; the hint says what. */
    LOCKED('L'),
    /** Nothing stands in the way; the giver and the place say where to go. */
    AVAILABLE('A'),
    /** The player holds it (or the world is in the middle of it). */
    ACTIVE('T'),
    /** Finished, reward paid. */
    DONE('D');

    public final char code;

    JournalStatus(char code) {
        this.code = code;
    }

    public static JournalStatus of(int ordinal) {
        JournalStatus[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : LOCKED;
    }
}
