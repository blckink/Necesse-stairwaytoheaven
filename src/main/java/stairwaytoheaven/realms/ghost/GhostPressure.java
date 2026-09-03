package stairwaytoheaven.realms.ghost;

import necesse.level.maps.Level;
import stairwaytoheaven.worldgen.SkyNoise;

/**
 * Where the dead are ALLOWED to appear in the Aftergarden, and where the realm
 * is quiet.
 *
 * <p>The rule and the engine mechanism behind it are written out once, in
 * {@link stairwaytoheaven.worldgen.SkyPressure}. In short, and <b>VERIFIED
 * [jar]</b>: {@code EntityManager.tickMobSpawning} picks a spawn POSITION by
 * asking each candidate tile for {@code getMobSpawnPositionTickets}, and
 * {@code MobSpawnArea.getRandomTicketTile} only adds a tile to the lottery
 * {@code if (tickets > 0)}. A ground that answers 0 is not merely unlikely, it
 * is not in the draw; when every tile in the ring answers 0 the getter returns
 * null and nothing spawns at all. That is the only lever in the engine that can
 * express {@code docs/WORLD_DESIGN.md} A4.1 — <i>"sie sollen mal geballt kommen
 * und ein Gebiet z.b bewachen wo es loot gibt, in anderen Ecken aber nicht
 * dauernd angeflogen kommen"</i> — and the design note closes with the
 * instruction that rules out the alternative: <b>"Do not answer this by
 * re-weighting spawn tables."</b>
 *
 * <p>This is the Ghost Realm's own copy of that policy rather than a branch
 * added to the sky's, for two reasons. The numbers match but the SITES do not:
 * the Aftergarden's guarded places are its three POIs, found through the same
 * lattice {@link GhostTerrainPainter} scatters them with, so a pack cannot end
 * up standing where the tomb is not. And {@code SkyPressure.spawnTickets(Level,
 * ...)} deliberately answers vanilla's default 100 for every level that is not
 * the Skyreach or the Veil, so a ghost tile that inherited it would be a realm
 * with no policy at all.
 *
 * <p>Everything here is a pure function of the seed and the tile, the same
 * contract as the rest of the mod's worldgen: the painter, the preset placer
 * and the live world all get the same answer.
 */
public final class GhostPressure {

    /**
     * On the guarded ground itself — the mausoleum, the manor, the sunken
     * graveyard. Six times vanilla's default ({@code GameTile} returns 100) and
     * a little over {@code CryptAshTile}'s 500, which is the highest number
     * vanilla itself uses and is exactly this case: the floor things come out
     * of.
     */
    public static final int GUARD_TICKETS = 600;

    /**
     * The approach. Vanilla's own default, so walking up to a POI feels like
     * ordinary Necesse ground rather than like an event starting.
     */
    public static final int APPROACH_TICKETS = 100;

    /**
     * The wilds: present, but thin. Between {@code AshTile}'s 2 and the default
     * 100, so something turns up out there without a stream of it.
     */
    public static final int WILD_TICKETS = 45;

    /**
     * The ectoplasm marsh. Low rather than zero for the same reason the sky
     * gives the Mistsea 8: liquid is a large share of the realm's area and
     * nothing in the ghost roster stands on it, so every draw the water won
     * would be a wasted tick.
     */
    public static final int MARSH_TICKETS = 8;

    /** Tiles from a POI centre that count as its own ground. */
    public static final float GUARD_RADIUS = 7.0F;

    /** Tiles from a POI centre that count as its approach. */
    public static final float APPROACH_RADIUS = 15.0F;

    /**
     * The wilds field. Scale and threshold are the Veil's own measured sweep
     * (see {@code VeilTerrainPainter.HOLLOW_THRESHOLD}: 0.660 -&gt; 18.0% of
     * land), so "about a sixth of the realm is wild" is a measured share and
     * not a guess. Patches at scale 44 are a few dozen tiles across — big
     * enough to be a stretch of country, small enough that a walk crosses
     * several.
     */
    public static final float WILDS_SCALE = 44.0F;
    public static final float WILDS_THRESHOLD = 0.66F;
    public static final int SALT_WILDS = 0x6057D5;

    private GhostPressure() {
    }

    /**
     * Distance in tiles to the nearest guarded site, or a large number when
     * there is none in range.
     *
     * <p>The sites are the realm's three POI lattices, asked through the same
     * {@link GhostTerrainPainter#nearestPoiDistance} the world preset places
     * them with, so the guards cannot drift away from the loot they guard.
     */
    public static float siteDistance(int seed, int tileX, int tileY) {
        return GhostTerrainPainter.nearestPoiDistance(seed, tileX, tileY);
    }

    /** Is this tile inside the wilds — the country where things are about? */
    public static boolean isWilds(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + SALT_WILDS, tileX, tileY, WILDS_SCALE, 2) > WILDS_THRESHOLD;
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
        return isWilds(seed, tileX, tileY) ? WILD_TICKETS : 0;
    }

    /**
     * Level-facing form, for {@code GameTile.getMobSpawnPositionTickets}.
     *
     * <p>Only the Ghost Realm is shaped this way. A mod ground can also end up
     * on a player's own floor in a settlement, on the surface, or inside an
     * incursion, and none of those should inherit a rule written for the
     * Aftergarden — so anything else gets vanilla's default and behaves exactly
     * as it would without this class.
     */
    public static int spawnTickets(Level level, int tileX, int tileY) {
        // getWorldGenSeed is declared on GhostLevel, not on Level: the
        // instanceof is what makes the call possible AND what keeps the rule
        // off every other level a ghost ground can end up on.
        if (level instanceof stairwaytoheaven.level.SkyLevel) {
            return spawnTickets(
                    ((stairwaytoheaven.level.SkyLevel) level).getWorldGenSeed(), tileX, tileY);
        }
        return 100;
    }
}
