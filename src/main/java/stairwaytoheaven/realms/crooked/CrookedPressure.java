package stairwaytoheaven.realms.crooked;

import necesse.level.maps.Level;
import stairwaytoheaven.worldgen.SkyNoise;

/**
 * Where hostiles are ALLOWED to appear in Crooked Beyond, and where the realm
 * is quiet.
 *
 * <p>This is the realm's copy of {@link stairwaytoheaven.worldgen.SkyPressure},
 * and everything that class proves holds here unchanged. The short version, so
 * this file does not have to be read against that one:
 *
 * <blockquote><b>VERIFIED [jar]</b> {@code EntityManager.tickMobSpawning} picks
 * the spawn POSITION by asking each candidate tile for
 * {@code getMobSpawnPositionTickets}, and {@code MobSpawnArea.getRandomTicketTile}
 * only enters a tile in the lottery {@code if (tickets > 0)}. A ground that
 * returns 0 is not unlikely — it is not in the draw at all, and when every tile
 * in the ring returns 0 the getter hands back null and the tick spawns nothing.
 * Vanilla uses the same dial in both directions: {@code GameTile}'s default is
 * 100, {@code AshTile} returns 2, {@code CryptAshTile} returns 500.</blockquote>
 *
 * <p>{@code WORLD_DESIGN.md} A4.1 says explicitly <b>"Do not answer this by
 * re-weighting spawn tables"</b>, and this is the lever that answers it instead.
 *
 * <h2>What is different from the Skyreach's copy, and why</h2>
 * <ol>
 * <li><b>The sites are this realm's own.</b>
 *     {@code stairwaytoheaven.worldgen.SkyPressure} finds its guarded ground
 *     through the Skyreach's wreck and workshop lattices. Crooked Beyond has
 *     neither; it has {@link CrookedSites}' three POI kinds, and the pressure
 *     field is derived from those, so a pack can never end up standing where
 *     the loot is not.</li>
 * <li><b>The wilds are thinner.</b> The sky runs a wilds field over about a
 *     sixth of its land at 45 tickets. Here it is a TENTH of the land at 30. The
 *     reason is the roster: every ambient hostile in this realm is an
 *     incursion-10 body (4000 HP / 280 damage), and a drizzle of those is not
 *     atmosphere, it is an ambush every forty seconds. The realm's whole budget
 *     is meant to be spent at its doors.</li>
 * </ol>
 *
 * <p>It does NOT place the guards — a pack that only exists while you are inside
 * the spawn ring is still weather. The packs are placed at generation and are
 * persistent, in {@link CrookedLevel#placeGuardPacks}. <b>VERIFIED [jar]</b>
 * {@code tickMobSpawning} counts only {@code m.isHostile && m.canDespawn}
 * against the spawn cap, so a placed pack costs the ambient budget nothing, and
 * a site you walked away from is still guarded when you come back for it.
 */
public final class CrookedPressure {

    /**
     * On the guarded ground itself. The Skyreach's own figure, and for the same
     * reason: six times vanilla's default, a little over {@code CryptAshTile}'s
     * 500, because unlike the crypt this has to win a lottery it shares with a
     * ring that is mostly Spill.
     */
    public static final int GUARD_TICKETS = 600;

    /** The approach. Vanilla's own default, so crossing it feels like ordinary ground. */
    public static final int APPROACH_TICKETS = 100;

    /**
     * The wilds: present, but thinner than the sky's 45. See the class comment —
     * an incursion-10 body is not ambience.
     */
    public static final int WILD_TICKETS = 30;

    /** Tiles from a site centre that count as the site's own ground. */
    public static final float GUARD_RADIUS = 8.0F;

    /** Tiles from a site centre that count as its approach. */
    public static final float APPROACH_RADIUS = 17.0F;

    /**
     * The wilds field. The scale is the Skyreach's (44 — patches a few dozen
     * tiles across, big enough to be a stretch of country and small enough that
     * a walk crosses several); the threshold is raised from 0.66 to 0.74.
     *
     * <p>Both numbers come off the same measured sweep, recorded in
     * {@code VeilTerrainPainter.HOLLOW_THRESHOLD}: 0.660 covers 18.0% of land,
     * 0.700 11.8%, 0.740 6.9%, 0.780 3.6%. So "about a tenth of the land is
     * wild" is a measured share rather than a guess, and it is deliberately
     * about half the sky's.
     */
    public static final float WILDS_SCALE = 44.0F;
    public static final float WILDS_THRESHOLD = 0.74F;
    public static final long SALT_WILDS = 0xC7011D5L;

    private CrookedPressure() {
    }

    /** Is this tile inside the wilds — the country where something may be about? */
    public static boolean isWilds(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + SALT_WILDS, tileX, tileY, WILDS_SCALE, 2) > WILDS_THRESHOLD;
    }

    /** The ticket weight for one land tile: the whole policy in one function. */
    public static int spawnTickets(int seed, int tileX, int tileY) {
        float d = CrookedSites.siteDistance(seed, tileX, tileY);
        if (d <= GUARD_RADIUS) {
            return GUARD_TICKETS;
        }
        if (d <= APPROACH_RADIUS) {
            return APPROACH_TICKETS;
        }
        return isWilds(seed, tileX, tileY) ? WILD_TICKETS : 0;
    }

    /**
     * Level-facing form, for {@code GameTile.getMobSpawnPositionTickets}.
     *
     * <p>Only Crooked Beyond is shaped this way. A Crooked ground can also end
     * up on a player's own floor in a settlement, on the surface, or inside an
     * incursion — none of which should inherit a rule written for this realm —
     * so anything else gets vanilla's default and behaves exactly as it would
     * without this class.
     */
    public static int spawnTickets(Level level, int tileX, int tileY) {
        if (level instanceof stairwaytoheaven.level.SkyLevel) {
            return spawnTickets(
                    ((stairwaytoheaven.level.SkyLevel) level).getWorldGenSeed(), tileX, tileY);
        }
        return 100;
    }
}
