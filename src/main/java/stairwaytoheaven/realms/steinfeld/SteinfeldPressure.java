package stairwaytoheaven.realms.steinfeld;

import necesse.level.maps.Level;
import stairwaytoheaven.worldgen.SkyNoise;

/**
 * Where hostiles are ALLOWED to appear in Steinfeld, and where the Reach is
 * quiet.
 *
 * <p>This is Steinfeld's copy of {@link stairwaytoheaven.worldgen.SkyPressure},
 * and the reasoning, the engine measurement and the vanilla numbers it sits on
 * are written out once in that file. The short version, because it is the rule
 * that decides how the realm feels:
 *
 * <blockquote><i>"es nervt aber wenn die alle 2 Sekunden ueberall angreifen ...
 * sie sollen mal geballt kommen und ein Gebiet z.b bewachen wo es loot gibt in
 * anderen Ecken aber nicht dauernd angeflogen kommen"</i>
 * — {@code docs/WORLD_DESIGN.md} A4.1</blockquote>
 *
 * <p><b>VERIFIED [jar]</b> {@code EntityManager.tickMobSpawning} picks the
 * spawn POSITION by asking each candidate tile for
 * {@code getMobSpawnPositionTickets}, and {@code MobSpawnArea.getRandomTicketTile}
 * only enters a tile in the lottery {@code if (tickets > 0)}. A ground that
 * returns 0 is not unlikely, it is absent; when the whole ring returns 0 the
 * getter returns null and nothing spawns at all. Vanilla uses the same dial in
 * both directions — {@code GameTile}'s default is 100, {@code AshTile} returns
 * 2, {@code CryptAshTile} returns 500 — and the numbers below sit on that
 * scale.
 *
 * <p>Two things make Steinfeld's version simpler than the sky's. There is one
 * POI lattice instead of two, so the guarded ground cannot drift away from the
 * loot; and there is no sea, so there is no third case to price.
 *
 * <p>{@link SteinfeldSites}' two hand-authored landmarks are a SECOND lattice
 * (see that class's header for why), and {@link #siteDistance} takes the
 * nearer of the two so a landmark is guarded on exactly the same terms as an
 * organic site -- A4.1 does not stop applying because the building was drawn
 * by hand rather than grown from noise.
 */
public final class SteinfeldPressure {

    private SteinfeldPressure() {
    }

    /**
     * On the ground a POI stands on. The sky's own 600, kept rather than
     * re-derived so the two realms read the same when the player walks into a
     * guarded place in either.
     */
    public static final int GUARD_TICKETS = 600;

    /** The approach. Vanilla's default, so crossing it feels like ordinary ground. */
    public static final int APPROACH_TICKETS = 100;

    /**
     * The wilds: present, but thin. Between {@code AshTile}'s 2 and the default
     * 100, so something turns up out there without a stream of it.
     */
    public static final int WILD_TICKETS = 45;

    /** Tiles from a site centre that count as the site's own ground. */
    public static final float GUARD_RADIUS = 7.0F;
    /** Tiles from a site centre that count as its approach. */
    public static final float APPROACH_RADIUS = 15.0F;

    /**
     * The wilds field.
     *
     * <p>Scale and threshold are the values the Veil's own offline sweep
     * measured (see {@code VeilTerrainPainter.HOLLOW_THRESHOLD}: 0.660 covers
     * 18.0% of ground at this noise), so "about a sixth of the Reach is wild"
     * is measured rather than guessed. Patches at scale 44 are a few dozen
     * tiles across — big enough to be a stretch of country, small enough that a
     * walk crosses several.
     */
    public static final float WILDS_SCALE = 44.0F;
    public static final float WILDS_THRESHOLD = 0.66F;
    public static final long SALT_WILDS = 0x57E11FL;

    /**
     * Distance to the nearest guarded place, on WHICHEVER lattice is closer:
     * the painter's own organic POI sites, or {@link SteinfeldSites}' two
     * hand-authored landmarks.
     */
    public static float siteDistance(int seed, int tileX, int tileY) {
        float organic = SteinfeldTerrainPainter.nearestSite(seed, tileX, tileY).distance;
        float landmark = SteinfeldSites.siteDistance(seed, tileX, tileY);
        return Math.min(organic, landmark);
    }

    /** Is this tile inside the wilds — the country where things live? */
    public static boolean isWilds(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + SALT_WILDS, tileX, tileY, WILDS_SCALE, 2) > WILDS_THRESHOLD;
    }

    /** The ticket weight for one tile: the whole policy in one function. */
    public static int spawnTickets(int seed, int tileX, int tileY) {
        float d = siteDistance(seed, tileX, tileY);
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
     * <p>The instanceof is what makes {@code getWorldGenSeed} reachable — it is
     * declared on the mod's levels, not on {@code Level} — and it is also what
     * keeps this rule OFF every other level a Steinfeld ground can end up on: a
     * player's own floor in a settlement, the surface, an incursion. Those get
     * vanilla's default and behave exactly as they did before.
     */
    public static int spawnTickets(Level level, int tileX, int tileY) {
        if (level instanceof stairwaytoheaven.level.SkyLevel) {
            return spawnTickets(
                    ((stairwaytoheaven.level.SkyLevel) level).getWorldGenSeed(), tileX, tileY);
        }
        return 100;
    }
}
