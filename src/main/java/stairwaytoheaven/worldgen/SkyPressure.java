package stairwaytoheaven.worldgen;

import necesse.level.maps.Level;

/**
 * Where hostiles are ALLOWED to appear, and where the world is quiet.
 *
 * <h2>The complaint this answers</h2>
 * The player, after finishing incursion 10:
 *
 * <blockquote><i>"es nervt aber wenn die alle 2 Sekunden ueberall angreifen und
 * man nichts in Ruhe ins Inventar tun kann etc.. sie sollen mal geballt kommen
 * und ein Gebiet z.b bewachen wo es loot gibt in anderen Ecken aber nicht
 * dauernd angeflogen kommen"</i></blockquote>
 *
 * {@code docs/WORLD_DESIGN.md} A4.1 turns that into a rule, and closes with the
 * instruction that matters here: <b>"Do not answer this by re-weighting spawn
 * tables."</b> A {@link necesse.level.maps.biomes.MobSpawnTable} is a weighted
 * roll with no notion of place; re-weighting it changes WHICH thing walks up to
 * you every few seconds, never WHETHER one does.
 *
 * <h2>The lever the engine actually has</h2>
 * <b>VERIFIED [jar]</b> {@code EntityManager.tickMobSpawning} does not roll a
 * position uniformly. It calls
 * {@code getMobSpawnTile(level, x, y, MOB_SPAWN_AREA, ticketsGetter)} with
 *
 * <pre>tile -&gt; inBounds &amp;&amp; !isSolidTile
 *          ? level.getTile(tile.x, tile.y).getMobSpawnPositionTickets(level, tile.x, tile.y)
 *          : 0</pre>
 *
 * and {@code MobSpawnArea.getRandomTicketTile} adds a tile to the lottery only
 * {@code if (tickets > 0)}. <b>A ground that returns 0 is not merely unlikely —
 * it is not in the draw at all</b>, and when every tile in the ring returns 0
 * the getter returns null and {@code tickMobSpawning} returns without spawning
 * anything. So a tile can say "nothing appears here", and that is exactly the
 * sentence the player asked for.
 *
 * <p>Vanilla uses the same dial, in both directions: {@code GameTile}'s default
 * is 100, {@code AshTile} returns <b>2</b> (dead ground you cross), and
 * {@code CryptAshTile} returns <b>500</b> (the floor things come out of). The
 * numbers below sit on that scale rather than on a new one.
 *
 * <h2>The three zones</h2>
 * <ol>
 * <li><b>A guarded site</b> — the ground around an aeronaut wreck or a Skywatch
 *     workshop, i.e. around the loot. {@link #GUARD_TICKETS}. This is where the
 *     fight is, and it is where reinforcements arrive during it.</li>
 * <li><b>Its approach</b> — the ring you cross on the way in, at roughly
 *     vanilla's ordinary weight, so a site announces itself before you are
 *     standing in it.</li>
 * <li><b>Everywhere else</b> — 0, except inside the <b>wilds</b>: a coarse
 *     noise field covering about a sixth of the land, where the world is
 *     genuinely wild and something may be about. Open ground between places is
 *     silent, which is the half of A4.1 that cannot be bought any other way.</li>
 * </ol>
 *
 * <h2>What this deliberately does NOT do</h2>
 * It does not place the guards. A pack that only exists while you are inside
 * the spawn ring is still weather; the pack itself is placed at generation and
 * is persistent ({@code canDespawn = false}), in
 * {@code SkyLevel.placeGuardPack}. Two consequences worth knowing:
 * <b>VERIFIED [jar]</b> {@code tickMobSpawning} counts only
 * {@code m.isHostile && m.canDespawn} against the spawn cap, so a placed pack
 * costs the ambient budget nothing; and it never despawns, so a site you walked
 * away from is still guarded when you come back for it.
 *
 * <p>Everything here is a pure function of the seed and the tile, the same
 * contract as the rest of {@code worldgen} — the painter, the offline renderer
 * and the live world all get the same answer.
 */
public final class SkyPressure {

    /**
     * On the guarded ground itself. Six times vanilla's default and a little
     * over {@code CryptAshTile}'s 500, because unlike the crypt this has to win
     * a lottery it shares with a ring that is mostly Mistsea.
     */
    public static final int GUARD_TICKETS = 600;

    /**
     * The approach. Vanilla's own default, so crossing it feels like ordinary
     * Necesse ground rather than like an event.
     */
    public static final int APPROACH_TICKETS = 100;

    /**
     * The wilds: present, but thin. Between {@code AshTile}'s 2 and the default
     * 100, so something turns up out there without a stream of it.
     */
    public static final int WILD_TICKETS = 45;

    /**
     * The Mistsea. Low rather than zero because the sea is 61% of the sky and
     * the {@link stairwaytoheaven.mobs.MistserpentHead} is the only thing that
     * spawns on it: at the default 100 the sea's sheer area would win most
     * draws and starve the guarded sites.
     *
     * <p>8 -&gt; 16. Against the wilds' 45 the sea still draws roughly a third as
     * often per tile, which over 61% of the map is about half the sky's ambient
     * pressure by area — but almost none of it used to reach the serpent,
     * because the biome tables let land mobs into a sea draw and they lose it
     * afterwards on the liquid check. That half is fixed in the tables (see any
     * biome's {@code mistseaSerpent} entry); this is the other half, and the
     * one-per-spawn-ring cap is what keeps the result occasional.
     */
    public static final int MISTSEA_TICKETS = 16;

    /** Tiles from a site centre that count as the site's own ground. */
    public static final float GUARD_RADIUS = 7.0F;

    /** Tiles from a site centre that count as its approach. */
    public static final float APPROACH_RADIUS = 15.0F;

    /**
     * The wilds field. Scale and threshold are the Veil's own measured sweep
     * (see {@link SkyOutlands}'s header, quoting
     * {@link VeilTerrainPainter#HOLLOW_THRESHOLD}: 0.660 -&gt; 18.0% of land),
     * so "about a sixth of the land is wild" is a measured share rather than a
     * guess. Patches at scale 44 are a few dozen tiles across — big enough to
     * be a stretch of country, small enough that a walk crosses several.
     */
    public static final float WILDS_SCALE = 44.0F;
    public static final float WILDS_THRESHOLD = 0.66F;
    public static final long SALT_WILDS = 0x5711D5L;

    private SkyPressure() {
    }

    /**
     * Distance in tiles to the nearest guarded site, or a large number when
     * there is none in range.
     *
     * The sites are the two things the sky already builds that are worth
     * guarding, and they are found through the same lattice the painter placed
     * them with — {@link SkyTerrainPainter#nearestSite} — so the guards cannot
     * drift away from the loot they guard.
     */
    public static float siteDistance(int seed, int tileX, int tileY) {
        float wreck = SkyTerrainPainter.nearestSite(seed, tileX, tileY,
                SkyTerrainPainter.WRECK_CELL, SkyTerrainPainter.SALT_WRECK,
                SkyTerrainPainter.WRECK_CHANCE).distance;
        float shop = SkyTerrainPainter.nearestSite(seed, tileX, tileY,
                SkyTerrainPainter.WORKSHOP_CELL, SkyTerrainPainter.SALT_WORKSHOP,
                SkyTerrainPainter.WORKSHOP_CHANCE).distance;
        return Math.min(wreck, shop);
    }

    /** Is this tile inside the wilds — the country where things live? */
    public static boolean isWilds(int seed, int tileX, int tileY) {
        return SkyNoise.fbm(seed + SALT_WILDS, tileX, tileY, WILDS_SCALE, 2) > WILDS_THRESHOLD;
    }

    /**
     * The ticket weight for one land tile: the whole policy in one function.
     */
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
     * <p>Only the Skyreach and the Veil are shaped this way. A mod ground can
     * also end up on a player's own floor in a settlement, on the surface, or
     * inside an incursion, and none of those should inherit a rule written for
     * open sky — so anything else gets vanilla's default and behaves exactly as
     * it did before this class existed.
     */
    public static int spawnTickets(Level level, int tileX, int tileY) {
        // getWorldGenSeed is declared on the two mod levels, not on Level: the
        // instanceof is what makes the call possible AND what keeps the rule
        // off every other level a mod ground can end up on.
        if (level instanceof stairwaytoheaven.level.SkyLevel) {
            return spawnTickets(((stairwaytoheaven.level.SkyLevel) level).getWorldGenSeed(),
                    tileX, tileY);
        }
        if (level instanceof stairwaytoheaven.level.VeilLevel) {
            return spawnTickets(((stairwaytoheaven.level.VeilLevel) level).getWorldGenSeed(),
                    tileX, tileY);
        }
        return 100;
    }
}
