package stairwaytoheaven.realms.eden;

import necesse.level.maps.Level;
import stairwaytoheaven.worldgen.SkyNoise;

/**
 * Where hostiles are ALLOWED to appear in the Garden of Eden, and where the
 * garden is quiet.
 *
 * <p>This is Eden's copy of {@link stairwaytoheaven.worldgen.SkyPressure}, and
 * it exists as its own class for the reason that file's header gives: the
 * policy is a pure function of the seed and the tile, and each realm's sites
 * are its own. The measurements and the engine facts are identical and are not
 * restated here — read {@code SkyPressure} for why a ticket count of 0 removes
 * a tile from the spawn lottery entirely
 * ({@code EntityManager.tickMobSpawning} → {@code MobSpawnArea.getRandomTicketTile}
 * only adds a tile {@code if (tickets > 0)}), and for the vanilla scale the
 * numbers sit on ({@code GameTile}'s default 100, {@code AshTile}'s 2,
 * {@code CryptAshTile}'s 500).
 *
 * <p>{@code docs/WORLD_DESIGN.md} A4.1 is the rule, and A3.3 is why it matters
 * more here than anywhere: Eden's whole thesis is <b>beauty can be
 * dangerous</b>. That only lands if the beauty is allowed to be beautiful for
 * a while first. A garden that attacks you every four seconds is not
 * uncomfortably perfect, it is a corridor.
 *
 * <p>The three zones are the Skyreach's, with the same numbers so a player
 * reads one world and not two:
 * <ol>
 * <li><b>A guarded site</b> — the ground around one of Eden's three POIs, i.e.
 *     around the loot. {@link #GUARD_TICKETS} = 600.</li>
 * <li><b>Its approach</b> — {@link #APPROACH_TICKETS} = 100, vanilla's own
 *     default, so walking in feels like ordinary ground.</li>
 * <li><b>Everywhere else</b> — 0, except inside the <b>wilds</b>, where
 *     {@link #WILD_TICKETS} = 45.</li>
 * </ol>
 *
 * <p>Eden adds a fourth: the ground around a <b>Knowledge Tree</b>. A3.3 asks
 * for it in so many words — <i>"Around the Knowledge Tree, snakes grow more
 * common and rare resources lie about — a soft difficulty gradient that needs
 * no gate."</i> A tree is not a POI with a preset; it is one object the painter
 * scatters, so its pressure is derived from the same lattice the painter uses
 * to place it ({@link EdenTerrainPainter#nearestSite}) and reads as a wide,
 * low-tickets halo rather than a hard ring.
 */
public final class EdenPressure {

    /** On the guarded ground of a POI. Same as the Skyreach's. */
    public static final int GUARD_TICKETS = 600;

    /** The approach ring: vanilla's own default. */
    public static final int APPROACH_TICKETS = 100;

    /** The wilds: present, but thin. */
    public static final int WILD_TICKETS = 45;

    /**
     * Under a Knowledge Tree. Above the wilds and well under a guarded site:
     * the gradient A3.3 asks for is meant to be felt as "there are more snakes
     * here", not as arriving at a boss arena.
     */
    public static final int KNOWLEDGE_TICKETS = 240;

    /**
     * The turquoise shallows. Low for the same reason the Mistsea is low: the
     * water is a large share of the realm and nothing in Eden's roster stands
     * on liquid, so every draw it won would be a wasted tick. Not zero, because
     * a lagoon should not be provably safe.
     */
    public static final int SHALLOWS_TICKETS = 8;

    /** Tiles from a site centre that count as the site's own ground. */
    public static final float GUARD_RADIUS = 7.0F;

    /** Tiles from a site centre that count as its approach. */
    public static final float APPROACH_RADIUS = 15.0F;

    /** How far a Knowledge Tree's halo reaches. */
    public static final float KNOWLEDGE_RADIUS = 18.0F;

    /**
     * The wilds field. Same scale and threshold as the Skyreach's, which were
     * measured rather than guessed (see {@code SkyPressure.WILDS_THRESHOLD}:
     * 0.66 over this noise covers about a sixth of the land), so "about a sixth
     * of Eden is wild" is the same measured share and not a second guess.
     */
    public static final float WILDS_SCALE = 44.0F;
    public static final float WILDS_THRESHOLD = 0.66F;
    public static final long SALT_WILDS = 0xED0A5L;

    private EdenPressure() {
    }

    /** Distance in tiles to the nearest guarded POI, or a large number. */
    public static float siteDistance(int seed, int tileX, int tileY) {
        float grove = EdenTerrainPainter.nearestSite(seed, tileX, tileY,
                EdenTerrainPainter.GROVE_CELL, EdenTerrainPainter.SALT_GROVE,
                EdenTerrainPainter.GROVE_CHANCE).distance;
        float lagoon = EdenTerrainPainter.nearestSite(seed, tileX, tileY,
                EdenTerrainPainter.LAGOON_CELL, EdenTerrainPainter.SALT_LAGOON,
                EdenTerrainPainter.LAGOON_CHANCE).distance;
        float orchard = EdenTerrainPainter.nearestSite(seed, tileX, tileY,
                EdenTerrainPainter.ORCHARD_CELL, EdenTerrainPainter.SALT_ORCHARD,
                EdenTerrainPainter.ORCHARD_CHANCE).distance;
        return Math.min(grove, Math.min(lagoon, orchard));
    }

    /** Is this tile inside the wilds — the country where things live? */
    public static boolean isWilds(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + (int) SALT_WILDS, tileX, tileY, WILDS_SCALE, 2) > WILDS_THRESHOLD;
    }

    /** The ticket weight for one land tile: the whole policy in one function. */
    public static int spawnTickets(int seed, int tileX, int tileY) {
        float d = siteDistance(seed, tileX, tileY);
        if (d <= GUARD_RADIUS) {
            return GUARD_TICKETS;
        }
        if (d <= APPROACH_RADIUS) {
            return APPROACH_TICKETS;
        }
        // A3.3's soft gradient: nearer a Knowledge Tree, more serpents.
        if (EdenTerrainPainter.nearestSite(seed, tileX, tileY,
                EdenTerrainPainter.KNOWLEDGE_CELL, EdenTerrainPainter.SALT_KNOWLEDGE,
                EdenTerrainPainter.KNOWLEDGE_CHANCE).distance <= KNOWLEDGE_RADIUS) {
            return KNOWLEDGE_TICKETS;
        }
        return isWilds(seed, tileX, tileY) ? WILD_TICKETS : 0;
    }

    /**
     * Level-facing form, for {@code GameTile.getMobSpawnPositionTickets}.
     *
     * <p>Only the Garden of Eden is shaped this way. Eden ground can also end
     * up on a player's own floor in a surface settlement — the Eden grass seed
     * has been findable in sky crates since 0.9 — and that floor must not
     * inherit a rule written for the realm, so anything else gets vanilla's
     * default and behaves exactly as it did before this class existed.
     */
    public static int spawnTickets(Level level, int tileX, int tileY) {
        if (level instanceof stairwaytoheaven.level.SkyLevel) {
            return spawnTickets(
                    ((stairwaytoheaven.level.SkyLevel) level).getWorldGenSeed(), tileX, tileY);
        }
        return 100;
    }
}
