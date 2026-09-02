package stairwaytoheaven.veil;

import java.awt.Point;

import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyOrigin;

/**
 * Where the Veil is — {@code docs/WORLD_DESIGN.md} §8's "past a defined realm
 * depth".
 *
 * <h2>This is a REGION test, and that is the whole point</h2>
 *
 * §8 does not ask for a wall and explicitly forbids one:
 *
 * <blockquote>Teleport / movement abuse must be handled. Do not merely block
 * tiles — the effect is checked against the <b>world region</b>.</blockquote>
 *
 * So there is deliberately <b>no</b> tile the player is stopped on, <b>no</b>
 * boundary-crossing event, and <b>no</b> "entered the fog" hook anywhere in
 * this package. There is only this function, asked about a live world position
 * several times a second by {@link VeilWorldData}. A player who ropes in,
 * takes a portal, respawns at a bed, rides a mount, builds a bridge, logs out
 * inside and back in, or is teleported by an admin is asked the same question
 * about the same position and gets the same answer. There is nothing to step
 * over, so there is nothing to step over the top of.
 *
 * <h2>Which levels carry the realm field</h2>
 *
 * The realm field of §3 is anchored on {@link SkyOrigin} and painted on the
 * Skyreach, so the Skyreach is the level it describes. Every other level —
 * Surface, caves, and the Veil dimension the Séance Circle opens — has no
 * realm depth at all, and asking {@link RealmDepth} about a position on one
 * would be answering a question that was never posed. {@link #carriesRealmField}
 * is the single place that decides this; when Eden, Steinfeld and the Ghost
 * Realm land on their own levels rather than on the sky's biome layer, this is
 * the one method that changes.
 *
 * <h2>Everything here is a pure function</h2>
 *
 * Same contract as the rest of {@code worldgen}: level identity, seed and tile
 * position only. No world state, no caching, no per-player memory. The
 * exposure clock lives in the buff's stacks (see {@link SoulExposureBuff}), not
 * here.
 */
public final class VeilRegion {

    /**
     * The realm depth at which the fog starts — {@code docs/WORLD_DESIGN.md}
     * §8's "defined realm depth", derived rather than typed.
     *
     * <p>§8 puts the Veil between Steinfeld (§7) and the Ghost Realm (§10), and
     * §38's quest order agrees: <i>06 Whispers Beyond the Stones</i> (Steinfeld)
     * is followed by <i>07 Into the Mist</i>. So the wall stands at the first
     * depth where Steinfeld stops being wholly itself and the Ghost Realm has
     * any weight at all — which is a question {@link RealmDepth#weightOf} can
     * answer, so it is asked instead of guessed.
     *
     * <p>Deriving it matters because {@code RealmDepth.BANDS} is the concept's
     * own table and will be retuned as the realms are built. A literal 0.58F
     * here would silently stop meaning "the end of Steinfeld" the first time
     * that table moves; this cannot. Same reasoning, same shape and same house
     * style as {@code SkyOutlands.trueCrookedStart()}.
     *
     * <p>At today's bands this comes out at <b>0.581</b>, i.e. about 3486
     * tiles from the spire at {@code DEPTH_SCALE} 6000. {@link #wallDistanceTiles()}
     * computes that for logs and the status command so the number is never
     * copied into a second place.
     */
    public static final float VEIL_DEPTH = deriveVeilDepth();

    private VeilRegion() {
    }

    private static float deriveVeilDepth() {
        // 0.001 steps: the bands are written to two decimals, so a thousandth
        // is finer than the table it is reading and still exact enough that the
        // answer does not depend on the step.
        for (int i = 0; i <= 1000; i++) {
            float depth = i / 1000.0F;
            if (RealmDepth.weightOf(RealmDepth.REALM_GHOST, depth) > 0.0F
                    && RealmDepth.weightOf(RealmDepth.REALM_STEINFELD, depth) < 1.0F) {
                return depth;
            }
        }
        // Unreachable with any sane band table, and a fog that starts at the
        // world's edge is the safe failure: it gates nothing rather than
        // gating everything.
        return 1.0F;
    }

    /**
     * Does this level carry the realm field of {@code WORLD_DESIGN.md} §3?
     *
     * <p>Only the Skyreach does. The check is on the level IDENTIFIER rather
     * than on {@code instanceof SkyLevel} because the identifier is what the
     * world contract is written in — {@code SkyRegistry.SKYREACH_IDENTIFIER}
     * is the one Skyreach in a world, and a second {@code SkyLevel} stamped
     * under another identifier (a test fixture, a future incursion copy) is
     * not the world the realm field describes.
     */
    public static boolean carriesRealmField(Level level) {
        return level != null && SkyRegistry.SKYREACH_IDENTIFIER.equals(level.getIdentifier());
    }

    /**
     * The world-generation seed the realm field is anchored on for this level.
     *
     * <p>{@code SkyLevel.getWorldGenSeed()} honours an explicit non-zero
     * {@code seed} (tests and tools set one) before falling back to the world's
     * derivation, so a level that can answer is asked rather than recomputed
     * around. Anything else — including the client's copy of the level before
     * it has a seed — falls back to the same derivation the surface stairways
     * use, which is what makes every caller agree without shared state.
     */
    public static int seedOf(Level level) {
        if (level instanceof SkyLevel) {
            return ((SkyLevel) level).getWorldGenSeed();
        }
        return SkyOrigin.worldGenSeed(level == null ? null : level.getWorldEntity());
    }

    /**
     * The realm depth of a tile on a level that carries the field, or −1 when
     * the level carries none. −1 rather than 0 on purpose: 0 is a real depth
     * (it is the spire), and a caller that forgets to check
     * {@link #carriesRealmField} must not be told it is standing at home.
     */
    public static float depthAt(Level level, int tileX, int tileY) {
        if (!carriesRealmField(level)) {
            return -1.0F;
        }
        Point origin = SkyOrigin.compute(seedOf(level));
        return RealmDepth.depthAt(tileX, tileY, origin.x, origin.y);
    }

    /** Is this world position inside the Veil's fog? */
    public static boolean isInside(Level level, int tileX, int tileY) {
        float depth = depthAt(level, tileX, tileY);
        return depth >= VEIL_DEPTH;
    }

    /**
     * Is this mob standing in the fog right now?
     *
     * <p>The position is read off the mob at the moment of asking, which is the
     * whole teleport answer: however the mob got to that tile, that is the tile
     * it is judged on.
     */
    public static boolean isInside(Mob mob) {
        if (mob == null || mob.removed()) {
            return false;
        }
        return isInside(mob.getLevel(), mob.getTileX(), mob.getTileY());
    }

    /** Distance from the spire, in tiles, at which the fog starts. */
    public static float wallDistanceTiles() {
        return VEIL_DEPTH * RealmDepth.DEPTH_SCALE;
    }
}
