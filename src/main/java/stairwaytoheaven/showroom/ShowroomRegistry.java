package stairwaytoheaven.showroom;

import java.util.ArrayList;
import java.util.List;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;

/**
 * Which registry IDs belong to this mod, and to which of its realms.
 *
 * <p>The showroom's material gallery shows "every mod object once" and "every
 * mod tile once". Nothing on a {@code GameObject} says which mod registered it,
 * but registry IDs are handed out in registration order and vanilla's
 * {@code registerCore()} runs before any mod's {@code init()} — so the IDs this
 * mod owns are exactly the ones handed out while {@code StairwayToHeavenMod.init}
 * ran. {@link #mark} is called at the start of each realm's registration block
 * and {@link #end} once at the very end; an ID then belongs to the last mark
 * whose start is at or below it.
 *
 * <p>Purely bookkeeping: it registers nothing and changes no ID.
 */
public final class ShowroomRegistry {

    private static final class Mark {
        final String group;
        final int objectStart;
        final int tileStart;

        Mark(String group, int objectStart, int tileStart) {
            this.group = group;
            this.objectStart = objectStart;
            this.tileStart = tileStart;
        }
    }

    private static final List<Mark> MARKS = new ArrayList<>();
    private static int objectEnd = -1;
    private static int tileEnd = -1;

    private ShowroomRegistry() {
    }

    private static int objectCount() {
        return ObjectRegistry.getObjectsCount();
    }

    private static int tileCount() {
        // getTileStringIDs() throws while the registry is open (it is, during
        // init); streamTiles() is the registry's own list, unguarded.
        return (int) TileRegistry.streamTiles().count();
    }

    /** Everything registered from now until the next mark belongs to {@code group}. */
    public static void mark(String group) {
        MARKS.add(new Mark(group, objectCount(), tileCount()));
    }

    /** Closes the mod's range. Call once, last thing in {@code init()}. */
    public static void end() {
        objectEnd = objectCount();
        tileEnd = tileCount();
    }

    /** The group of a mod object ID, or null for vanilla (or another mod). */
    public static String groupOfObject(int id) {
        if (MARKS.isEmpty() || id < MARKS.get(0).objectStart || (objectEnd >= 0 && id >= objectEnd)) {
            return null;
        }
        String group = null;
        for (Mark m : MARKS) {
            if (id >= m.objectStart) group = m.group;
        }
        return group;
    }

    /** The group of a mod tile ID, or null for vanilla (or another mod). */
    public static String groupOfTile(int id) {
        if (MARKS.isEmpty() || id < MARKS.get(0).tileStart || (tileEnd >= 0 && id >= tileEnd)) {
            return null;
        }
        String group = null;
        for (Mark m : MARKS) {
            if (id >= m.tileStart) group = m.group;
        }
        return group;
    }

    public static int objectStart() {
        return MARKS.isEmpty() ? 0 : MARKS.get(0).objectStart;
    }

    public static int objectEnd() {
        return objectEnd < 0 ? objectCount() : objectEnd;
    }

    public static int tileStart() {
        return MARKS.isEmpty() ? 0 : MARKS.get(0).tileStart;
    }

    public static int tileEnd() {
        return tileEnd < 0 ? tileCount() : tileEnd;
    }
}
