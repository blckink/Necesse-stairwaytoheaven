package stairwaytoheaven.surface;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;

/**
 * Every object and tile the three Surface POIs build from, resolved through
 * here so it can be checked.
 *
 * <h2>Why this exists</h2>
 * A preset writes raw IDs. {@code ObjectRegistry.getObjectID} and
 * {@code TileRegistry.getTileID} both answer <b>−1</b> for a name they do not
 * know ({@code GameRegistry.getElementID}), and {@code Preset.setObject(x, y,
 * −1)} means <i>leave this cell alone</i> — so one misspelled string does not
 * throw, does not warn, and does not fail the build. It quietly removes a piece
 * of the structure. A worse variant returns 0, which is {@code air}, and
 * actively clears the cell.
 *
 * <p>Routing every lookup through {@link #obj} / {@link #tile} records what was
 * asked for next to what came back, so {@code /skysurfacestatus} can report
 * {@code unresolved=0} — and the integration test can fail the day it is not.
 * IDs that come from the mod's own registration fields are recorded with
 * {@link #known} so they are covered by the same report.
 */
public final class SurfaceMaterials {

    /** requested name -> resolved ID. Insertion-ordered, for a stable report. */
    private static final Map<String, Integer> RESOLVED = new LinkedHashMap<>();

    private SurfaceMaterials() {
    }

    /** A vanilla or mod object, by string ID. */
    public static int obj(String stringID) {
        int id = ObjectRegistry.getObjectID(stringID);
        record("object:" + stringID, id);
        return id;
    }

    /** A vanilla or mod tile, by string ID. */
    public static int tile(String stringID) {
        int id = TileRegistry.getTileID(stringID);
        record("tile:" + stringID, id);
        return id;
    }

    /**
     * An ID the mod already holds in a registration field (SkyRegistry and
     * friends). Recorded under the same report so an ID that was never assigned
     * — because a registration moved to a later lifecycle phase, say — is just
     * as visible as a misspelled string.
     */
    public static int known(String label, int id) {
        record(label, id);
        return id;
    }

    private static synchronized void record(String label, int id) {
        RESOLVED.put(label, id);
    }

    /** Everything asked for so far, requested name to resolved ID. */
    public static synchronized Map<String, Integer> resolved() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(RESOLVED));
    }

    /**
     * True when the ID is a real registry entry: not −1 (unknown) and not 0,
     * which is {@code air} for objects and would clear the cell instead of
     * filling it. Tiles are checked the same way — none of ours wants tile 0.
     */
    public static boolean isResolved(int id) {
        return id > 0;
    }
}
