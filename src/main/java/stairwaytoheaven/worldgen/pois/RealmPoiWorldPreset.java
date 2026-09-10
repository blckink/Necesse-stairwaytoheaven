package stairwaytoheaven.worldgen.pois;

import java.awt.Dimension;

import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.util.GameRandom;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.WorldPreset;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.livestock.SkyLivestock;
import stairwaytoheaven.realms.crooked.CrookedTerrainPainter;
import stairwaytoheaven.realms.eden.EdenTerrainPainter;
import stairwaytoheaven.realms.ghost.GhostTerrainPainter;
import stairwaytoheaven.realms.steinfeld.SteinfeldTerrainPainter;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.SkyTerrainPainter;

/** Places the twenty-five inhabited POIs into their realm bands on {@code skyreach2}. */
public class RealmPoiWorldPreset extends WorldPreset {
    public static final String STRING_ID = "swh_realmpois";
    private static final String OCCUPIED_BOARD = "villages";
    private static final int CELL = 220;
    private static final float SITE_CHANCE = 0.42F;
    private static final int SALT = 0x61A7;
    /** Radius in tiles of the clear ring the Warden Spire keeps around itself. */
    private static final int SPIRE_CLEARANCE = 100;
    /**
     * Placements tried per kind before the cell falls through to the next one.
     *
     * <p>The same idea as vanilla's {@code findRandomPresetTile}, which
     * {@code CrookedHouseWorldPreset} already leans on with 400 attempts. One
     * shot at the cell's exact centre is a coin flip against the Mistsea: the
     * Skyreach is islands over open cloud, and measured on 2026-09-07 it threw
     * away 22 of 34 candidate cells in the home band on the first sample alone.
     *
     * <p><b>16 until 2026-09-10, and raised when the band reached sixteen
     * kinds.</b> {@link #skyreachRotate} hands each kind its own cells by rank,
     * so the number of cells a kind gets is the band's site count divided by
     * the kind count — and §2.8, §2.9 and §2.10 took that from ~2.9 to ~2.1.
     * With two cells, whether a kind stands anywhere in the world is decided by
     * whether those two cells pass its ground test, and that is a coin flip for
     * the one kind whose test wants a terrain FEATURE rather than just land:
     * {@link #validSite} asks the Toll Bridge for a strait to span. Measured on
     * seed 1485253616 it drew {@code accepted=0} while all fifteen other kinds
     * held 2-4, with {@code badground=0} for the band as a whole — the same
     * shape of failure {@code skyreachRotate} was written to end, one level
     * further down.
     *
     * <p>Attempt 0 is still the cell's own site, so nothing that already stood
     * somewhere moves; the extra attempts only sample more of the same
     * {@link #SITE_JITTER} box before a cell is given up on. Cost is paid only
     * where a kind is failing anyway.
     */
    private static final int SITE_ATTEMPTS = 48;
    /** How far a nudged attempt may move from the cell's own site, in tiles. */
    private static final int SITE_JITTER = CELL / 2;

    private static final int[][] REALM_KINDS = {
            {RealmPoiPresets.SKY_TOWER, RealmPoiPresets.SKY_TOWN,
                    RealmPoiPresets.SKY_TOLL_BRIDGE, RealmPoiPresets.SKY_INN,
                    RealmPoiPresets.SKY_TOLL_HOUSE,
                    RealmPoiPresets.SKY_WAYSIDE_SHRINE,
                    RealmPoiPresets.SKY_DEW_KEEPERS_HUT,
                    RealmPoiPresets.SKY_SHEPHERDS_FOLD,
                    RealmPoiPresets.SKY_FALLING_INSTITUTE,
                    RealmPoiPresets.SKY_PASSAGE_WAYHOUSE,
                    RealmPoiPresets.SKY_NIGHTFELL_REDOUBT,
                    RealmPoiPresets.SKY_AETHER_MANUFACTORY,
                    RealmPoiPresets.SKY_SOVEREIGNS_ANVIL,
                    RealmPoiPresets.SKY_UNOPENED_GATE,
                    RealmPoiPresets.SKY_PRISM_CHOIR,
                    RealmPoiPresets.SKY_SERPENTS_REEF},
            {RealmPoiPresets.EDEN_CROWN_GARDEN, RealmPoiPresets.EDEN_FERMENT_HOUSE},
            {RealmPoiPresets.STEINFELD_MEMORIAL},
            {RealmPoiPresets.GHOST_ARCHIVE},
            {RealmPoiPresets.CROOKED_BAZAAR},
            {RealmPoiPresets.HELL_BORDER_OFFICE, RealmPoiPresets.HELL_ADMINISTRATION,
                    RealmPoiPresets.HELL_FORGE, RealmPoiPresets.HELL_CARNIVAL},
    };

    /** Resolve every tile/object once when registries close, so a typo fails at load instead of far out in the world. */
    @Override
    public void onRegistryClosed() {
        for (int kind = 0; kind < RealmPoiPresets.COUNT; kind++) {
            RealmPoiPresets.build(kind, new GameRandom(0L));
        }
    }

    @Override
    public boolean shouldAddToRegion(LevelPresetsRegion region) {
        return region.identifier.equals(SkyRegistry.SKYREACH_IDENTIFIER);
    }

    // -----------------------------------------------------------------------
    // The funnel, named once, so the census and the placer cannot drift apart.
    // A candidate that survives every stage is ACCEPTED; anything else records
    // the FIRST stage that rejected it. Ordinals are the order of the tests.
    // -----------------------------------------------------------------------

    /** Every test passed: this site is offered to the preset region. */
    public static final int STAGE_ACCEPTED = 0;
    /** The region is too small to hold the footprint at all. */
    public static final int STAGE_SPLIT_BY_REGION = 1;
    /** Inside the 100-tile ring that keeps the Warden Spire uncluttered. */
    public static final int STAGE_NEAR_SPIRE = 2;
    /** {@link #validSite} said no to every kind the realm offers. */
    public static final int STAGE_BAD_GROUND = 3;
    /** How many stages there are, for a caller sizing a tally array. */
    public static final int STAGE_COUNT = 4;

    /** Human-readable stage names, indexed by the STAGE_ constants. */
    public static final String[] STAGE_NAMES = {
            "accepted", "splitbyregion", "nearspire", "badground"};

    /** What {@link #survey} reports for each candidate site of a preset region. */
    public interface SiteVisitor {
        /**
         * @param kind   the {@link RealmPoiPresets} kind that took the cell, or
         *               the last one tried if none could
         * @param realm  the {@link RealmDepth} realm the site's centre is in
         * @param x      left tile of the footprint
         * @param y      top tile of the footprint
         * @param stage  {@link #STAGE_ACCEPTED} or why the cell stayed empty
         */
        void site(int kind, int realm, int x, int y, int width, int height, int stage);
    }

    /**
     * Every candidate site one preset region owns, and what became of it.
     *
     * <p>This is the placement decision itself, not a model of it:
     * {@link #addToRegion} is a caller. Anything that wants to COUNT the
     * catalogue — {@code /skyreachstatus pois}, and through it
     * {@code scripts/integration_test.sh} — asks here, so a census can never
     * report a funnel the world does not actually run. The one test left out is
     * the occupied-rectangle board, which is region state rather than a pure
     * function of the seed; the census reads that back off the real queue.
     *
     * <h2>Which region owns a cell</h2>
     * The region containing the cell's site CENTRE, and it alone. Preset regions
     * tile the plane without overlap, so that is exactly one region per cell —
     * which is what keeps a structure near a border from being queued twice.
     * The older rule ("the whole footprint must fall inside one region") looked
     * like the same thing and was not: a footprint straddling a border was
     * rejected by BOTH neighbours and simply vanished. Measured over the realm
     * disc on 2026-09-07 that was <b>35% of every candidate in the world</b>,
     * and the loss fell hardest on the widest presets — Sky Town at 57 and Hell
     * Administration at 61 — which is a large part of why the player found none
     * of them. The footprint is now nudged inside its owning region instead.
     *
     * <h2>Which kind takes a cell</h2>
     * The realm's kinds are tried in a per-cell rotated order and the first one
     * whose ground test passes takes the site. The old rule picked one kind up
     * front and dropped the cell when that kind did not fit, so a cell that
     * could have carried a Sky Inn was thrown away for being unable to carry a
     * Sky Town — and the four-kind Skyreach and Hell bands lost three quarters
     * of their chances to a coin flip made before anyone looked at the ground.
     * Rotating lets the ground have the last word, which is what puts the big
     * presets where there is room.
     *
     * <p>Where the rotation comes from is a second question, and outside
     * Skyreach it is a per-cell hash. That spreads the mix but does not
     * GUARANTEE it, and Skyreach is the one band where the difference shows:
     * see {@link #skyreachRotate}.
     */
    public static void survey(int seed, int startX, int startY, int endX, int endY, SiteVisitor visitor) {
        int originX = SkyOrigin.originX(seed);
        int originY = SkyOrigin.originY(seed);
        for (int cellX = Math.floorDiv(startX, CELL); cellX <= Math.floorDiv(endX, CELL); cellX++) {
            for (int cellY = Math.floorDiv(startY, CELL); cellY <= Math.floorDiv(endY, CELL); cellY++) {
                if (!hasSite(seed, cellX, cellY)) continue;
                int siteX = siteX(seed, cellX, cellY);
                int siteY = siteY(seed, cellX, cellY);
                // Not this region's cell. Silent rather than a stage: the
                // neighbour that owns it will report it, and counting it here
                // too would make every census double-count its borders.
                if (siteX < startX || siteX >= endX || siteY < startY || siteY >= endY) continue;

                int realm = RealmDepth.realmAt(seed, siteX, siteY, originX, originY);
                int[] choices = REALM_KINDS[realm];
                int rotate = -1;
                if (realm == RealmDepth.REALM_SKYREACH) {
                    rotate = skyreachRotate(seed, cellX, cellY, originX, originY, choices.length);
                }
                if (rotate < 0) {
                    rotate = Math.min(choices.length - 1,
                            (int) (SkyNoise.hash(seed + SALT + 3, cellX, cellY) * choices.length));
                }

                if (nearSpire(siteX - originX, siteY - originY)) {
                    // The canonical Warden Spire remains the uncluttered first landmark.
                    int kind = choices[rotate];
                    visitor.site(kind, realm, siteX - RealmPoiPresets.width(kind) / 2,
                            siteY - RealmPoiPresets.height(kind) / 2,
                            RealmPoiPresets.width(kind), RealmPoiPresets.height(kind), STAGE_NEAR_SPIRE);
                    continue;
                }

                int lastKind = choices[rotate];
                int lastX = siteX;
                int lastY = siteY;
                int lastStage = STAGE_BAD_GROUND;
                kinds:
                for (int i = 0; i < choices.length; i++) {
                    int kind = choices[(rotate + i) % choices.length];
                    int width = RealmPoiPresets.width(kind);
                    int height = RealmPoiPresets.height(kind);
                    // Centred on the site, then nudged whole inside the region
                    // that owns it. x + width must stay strictly below endX,
                    // matching the bound addPreset's rectangle needs.
                    int highX = endX - width - 1;
                    int highY = endY - height - 1;
                    lastKind = kind;
                    if (highX < startX || highY < startY) {
                        lastStage = STAGE_SPLIT_BY_REGION;
                        lastX = siteX - width / 2;
                        lastY = siteY - height / 2;
                        continue;
                    }
                    for (int attempt = 0; attempt < SITE_ATTEMPTS; attempt++) {
                        int spotX = siteX + jitter(seed, cellX, cellY, kind, attempt, 0);
                        int spotY = siteY + jitter(seed, cellX, cellY, kind, attempt, 1);
                        // A nudge must not walk a site into the spire's ring.
                        if (attempt > 0 && nearSpire(spotX - originX, spotY - originY)) continue;
                        int x = clamp(spotX - width / 2, startX, highX);
                        int y = clamp(spotY - height / 2, startY, highY);
                        lastX = x;
                        lastY = y;
                        if (validSite(kind, realm, seed, x, y, width, height)) {
                            visitor.site(kind, realm, x, y, width, height, STAGE_ACCEPTED);
                            lastStage = STAGE_ACCEPTED;
                            break kinds;
                        }
                    }
                    lastStage = STAGE_BAD_GROUND;
                }
                if (lastStage != STAGE_ACCEPTED) {
                    visitor.site(lastKind, realm, lastX, lastY,
                            RealmPoiPresets.width(lastKind), RealmPoiPresets.height(lastKind), lastStage);
                }
            }
        }
    }

    private static int clamp(int value, int low, int high) {
        return value < low ? low : (value > high ? high : value);
    }

    // The three questions a lattice cell answers, named once. The funnel and
    // the rank below both ask them, and a rank computed from a different site
    // than the one the funnel places would balance a world nobody generates.

    /** Whether this cell offers a site at all. */
    private static boolean hasSite(int seed, int cellX, int cellY) {
        return SkyNoise.hash(seed + SALT, cellX, cellY) < SITE_CHANCE;
    }

    private static int siteX(int seed, int cellX, int cellY) {
        return Math.round(cellX * CELL + SkyNoise.hash(seed + SALT + 1, cellX, cellY) * CELL);
    }

    private static int siteY(int seed, int cellX, int cellY) {
        return Math.round(cellY * CELL + SkyNoise.hash(seed + SALT + 2, cellX, cellY) * CELL);
    }

    /**
     * Which kind takes a SKYREACH cell: the cell's rank among the whole band's
     * sites, modulo the number of kinds.
     *
     * <p>Every other band picks its rotation from a per-cell hash, which spreads
     * the mix without guaranteeing it. That was good enough while Skyreach held
     * four kinds and stopped being good enough at thirteen. Measured on
     * 2026-09-10, seed 1524204744, over the whole realm disc: the band offers
     * <b>38 sites</b> — it is the only band bounded by its own depth
     * ({@code RealmDepth} gives Skyreach weight only below depth 0.30, i.e.
     * 1800 tiles) while carrying the most kinds. Thirty-eight independent draws
     * over thirteen bins leave a bin empty about half the time, and that run
     * left two: the Passage Wayhouse and the Sovereign's Anvil stood nowhere in
     * the world. The catalogue gate had therefore been a coin flip since the
     * band passed ten kinds, failing on arithmetic rather than on a defect.
     *
     * <p>A rank cannot be drawn from a hash, but it can be COUNTED: whether a
     * cell holds a site and which realm that site falls in are pure functions of
     * the seed, and Skyreach is bounded, so the band's sites can be walked in
     * scan order from anywhere. Cell number <i>n</i> of the band takes kind
     * {@code n % kinds}, and with 38 sites over 13 kinds every kind gets two or
     * three. It costs one pass over the ~400 lattice cells the band's box holds,
     * per Skyreach site, and no state.
     *
     * @return the rotation, or -1 if the cell is not inside the band's box at
     *         all — in which case the caller keeps the hash rotation rather than
     *         inventing a rank.
     */
    private static int skyreachRotate(int seed, int cellX, int cellY,
            int originX, int originY, int kinds) {
        // The band's own reach, read off RealmDepth rather than hard-coded, plus
        // the cell a site can be jittered out of and one cell of slack.
        int reach = (int) (RealmDepth.bandEnd(RealmDepth.REALM_SKYREACH)
                * RealmDepth.DEPTH_SCALE) + 2 * CELL;
        int firstX = Math.floorDiv(originX - reach, CELL);
        int lastX = Math.floorDiv(originX + reach, CELL);
        int firstY = Math.floorDiv(originY - reach, CELL);
        int lastY = Math.floorDiv(originY + reach, CELL);
        if (cellX < firstX || cellX > lastX || cellY < firstY || cellY > lastY) {
            return -1;
        }
        // Turned by the seed, or the north-west corner of the band would be the
        // Sky Tower in every world ever generated.
        int offset = Math.min(kinds - 1,
                (int) (SkyNoise.hash(seed + SALT + 5, 0, 0) * kinds));
        int rank = 0;
        for (int cx = firstX; cx <= lastX; cx++) {
            for (int cy = firstY; cy <= lastY; cy++) {
                if (!hasSite(seed, cx, cy)) continue;
                if (cx == cellX && cy == cellY) {
                    return Math.floorMod(rank + offset, kinds);
                }
                if (RealmDepth.realmAt(seed, siteX(seed, cx, cy), siteY(seed, cx, cy),
                        originX, originY) == RealmDepth.REALM_SKYREACH) {
                    rank++;
                }
            }
        }
        return -1;
    }

    /**
     * The offset of one placement attempt, in tiles. Attempt 0 is always the
     * cell's own site, so a place that already stood where the old rule put it
     * still stands there.
     */
    private static int jitter(int seed, int cellX, int cellY, int kind, int attempt, int axis) {
        if (attempt == 0) {
            return 0;
        }
        float roll = SkyNoise.hash(seed + SALT + 4 + axis * 64L + attempt * 8L + kind,
                cellX, cellY);
        return Math.round((roll * 2.0F - 1.0F) * SITE_JITTER);
    }

    /** The clear ring around the spire, as one named test both callers share. */
    private static boolean nearSpire(int dx, int dy) {
        return dx * dx + dy * dy < SPIRE_CLEARANCE * SPIRE_CLEARANCE;
    }

    @Override
    public void addToRegion(GameRandom random, final LevelPresetsRegion region,
            BiomeGeneratorStack generatorStack, PerformanceTimerManager timer) {
        final int seed = SkyOrigin.worldGenSeed(region.worldRegion.worldEntity);
        int startX = region.worldRegion.startTileX;
        int startY = region.worldRegion.startTileY;
        int endX = startX + region.worldRegion.tileWidth;
        int endY = startY + region.worldRegion.tileHeight;

        survey(seed, startX, startY, endX, endY, new SiteVisitor() {
            @Override
            public void site(final int kind, int realm, final int x, final int y, int width, int height, int stage) {
                if (stage != STAGE_ACCEPTED) return;
                if (region.isRectangleOccupied(OCCUPIED_BOARD, x, y, width, height)) return;

                region.addPreset(RealmPoiWorldPreset.this, x, y, new Dimension(width, height), OCCUPIED_BOARD,
                        new LevelPresetsRegion.WorldPresetPlaceFunction() {
                            @Override
                            public void place(GameRandom placeRandom, Level level, PerformanceTimerManager placeTimer) {
                                RealmPoiPresets.build(kind, placeRandom).applyToLevel(level, x, y);
                                placeInhabitants(kind, level, x, y);
                            }
                        })
                        // Which of the twenty-five this rectangle is. Without it the
                        // queue only says "swh_realmpois", and a census can count
                        // records but not tell a Sky Inn from a Hell Carnival.
                        .setDebugName(RealmPoiPresets.key(kind));
            }
        });
    }

    /**
     * The people a stamped POI comes with, at the tile its plan puts them on.
     *
     * <p>A {@link necesse.level.maps.presets.Preset} carries tiles and objects
     * and nothing else, so an inhabitant cannot be part of one; the place
     * closure is the first point that holds a {@link Level}. Server side only —
     * the client receives the mob over the wire, and spawning it on both ends
     * is how you get two of somebody.
     *
     * <p>{@code canDespawn = false}, like {@code SkyLevel.placeResident}: a
     * settler the player walked past has to still be there on the way back, or
     * a once-per-world recruit is lost to a chunk unload.
     */
    private static void placeInhabitants(int kind, Level level, int x, int y) {
        if (level.isClient()) return;
        if (kind == RealmPoiPresets.SKY_TOLL_HOUSE) {
            // Plan  2.12: Magpie waits at (18,5) in the ledger room.
            spawn(level, "magpiesettler", x + 18, y + 5);
        }
        if (kind == RealmPoiPresets.SKY_DEW_KEEPERS_HUT) {
            // Plan  2.11: five Dew Snails inside the run, which is the whole
            // reason to walk into the hut -- the netting loop the mod built and
            // then hid in open terrain where nobody meets five at once. Inside
            // the fence, clear of the gate at (6,9) and of the glowferns.
            for (int[] at : new int[][]{{3, 10}, {6, 10}, {9, 11}, {4, 11}, {8, 11}}) {
                spawn(level, "dewsnail", x + at[0], y + at[1]);
            }
        }
        if (kind == RealmPoiPresets.SKY_SHEPHERDS_FOLD) {
            // Plan  2.2: the flock in the pasture west of the cottage, on the
            // meadow tiles the plan leaves unwritten and clear of its trees,
            // bushes and lamp. The section was written around the Cloud Lamb;
            // its own 2026-09-02 note records that the mob is gone and that
            // the fold reads as Glimmergoats and a Nimbus Yak instead.
            for (int[] at : new int[][]{{3, 6}, {6, 4}, {5, 9}, {7, 5}}) {
                spawn(level, SkyLivestock.GLIMMERGOAT, x + at[0], y + at[1]);
            }
            spawn(level, SkyLivestock.NIMBUS_YAK, x + 6, y + 6);
        }
        if (kind == RealmPoiPresets.SKY_FALLING_INSTITUTE) {
            // Plan  2.3: Test Subject VII, in the exact centre of the crater,
            // completely unharmed. It is the punchline, so it does not wander
            // off before the player has walked the arc down to it.
            spawn(level, SkyLivestock.NIMBUS_YAK, x + 13, y + 17);
        }
        if (kind == RealmPoiPresets.SKY_NIGHTFELL_REDOUBT) {
            // Plan §2.4's garrison, minus the half of it that has no art. The
            // four Skywatch Revenants and three Fulgur Shades are §4 work
            // orders, and MobRegistry.getMob of an unregistered name returns
            // null, which spawn() drops on the floor without a word. What is
            // registered stands exactly where the section puts it: two golems
            // on the corridor ring, two sentries flanking the north and south
            // doors. That leaves the compound defended but under-garrisoned,
            // which is written up rather than padded out with other mobs.
            for (int[] at : new int[][]{{5, 13}, {19, 13}}) {
                spawn(level, "skystonegolem", x + at[0], y + at[1]);
            }
            for (int[] at : new int[][]{{12, 5}, {12, 19}}) {
                spawn(level, "rimesentry", x + at[0], y + at[1]);
            }
        }
        if (kind == RealmPoiPresets.SKY_AETHER_MANUFACTORY) {
            // Plan §2.5: the two Rime Sentries, in the machine hall. Its three
            // Skywatch Revenants share the Redoubt's missing sheet.
            for (int[] at : new int[][]{{5, 15}, {15, 4}}) {
                spawn(level, "rimesentry", x + at[0], y + at[1]);
            }
        }
        if (kind == RealmPoiPresets.SKY_SOVEREIGNS_ANVIL) {
            // Plan §2.6: "2 Skystone Golems patrolling the terrace at (7,7) and
            // (21,21); Storm Wisps drifting the rim. The arena floor is empty
            // until the player fills it." The wisps go on the terrace rather
            // than on the rim itself, because the rim's own tiles are the rock
            // formation and half of them are solid.
            for (int[] at : new int[][]{{7, 7}, {21, 21}}) {
                spawn(level, "skystonegolem", x + at[0], y + at[1]);
            }
            for (int[] at : new int[][]{{14, 3}, {14, 25}}) {
                spawn(level, "stormwisp", x + at[0], y + at[1]);
            }
        }
        if (kind == RealmPoiPresets.SKY_PRISM_CHOIR) {
            // Plan §2.9: "2 Aurora Flakes at (8,10) and (12,10)". The
            // Dawnpiercers the section has diving in from outside the ring are
            // the Shoals' own spawn rather than residents of the place, so
            // they are not planted here.
            for (int[] at : new int[][]{{8, 10}, {12, 10}}) {
                spawn(level, "auroraflake", x + at[0], y + at[1]);
            }
        }
        if (kind == RealmPoiPresets.SKY_SERPENTS_REEF) {
            // Plan §2.10, and the whole reason the place exists: the
            // Mistserpent spawns IN_MISTSEA, in open water where nobody swims,
            // so most players will never meet the mod's only worm chain. Here
            // it is guaranteed. On a '~' cell east of the crescent, which is
            // the water it circles; MobRegistry.getMob runs WormMobHead.init,
            // which is what builds the body and tail behind the head.
            spawn(level, "mistserpent", x + 12, y + 12);
            // The Reefmaw in the wreck is §4 new art and unregistered.
        }
        // SKY_UNOPENED_GATE has no branch on purpose. Plan §2.8 posts two
        // Skywatch Revenants ON the dais, "exactly where a pair of gate wardens
        // would stand"; they share the Redoubt's missing sheet, so the gate
        // stands unguarded rather than garrisoned with somebody else's mob.
    }

    private static void spawn(Level level, String mobID, int tileX, int tileY) {
        necesse.entity.mobs.Mob mob = necesse.engine.registries.MobRegistry.getMob(mobID, level);
        if (mob == null) return;
        mob.canDespawn = false;
        level.entityManager.addMob(mob, tileX * 32 + 16, tileY * 32 + 16);
    }

    private static boolean validSite(int kind, int realm, int seed, int x, int y, int width, int height) {
        if (kind == RealmPoiPresets.SKY_SERPENTS_REEF) {
            // The one kind that wants OPEN CLOUD. §2.10 puts the reef "in the
            // Mistsea, between islands", and its preset paints its own 123
            // land tiles into 625 of water, so what the world has to supply is
            // sea and not shore -- the ordinary nine-sample land test below
            // would reject it everywhere it belongs. Inverted rather than
            // relaxed: a reef stamped half onto an island would carve the
            // island's own ground into water.
            for (int sx = 0; sx <= 2; sx++) {
                for (int sy = 0; sy <= 2; sy++) {
                    if (skyLand(seed, x + sx * (width - 1) / 2, y + sy * (height - 1) / 2)) {
                        return false;
                    }
                }
            }
            return true;
        }
        if (kind == RealmPoiPresets.SKY_TOLL_BRIDGE) {
            // Both road ends must reach real ground, or the bridge leads
            // nowhere; and the cloud stream it spans must run off at least one
            // side, or it is a bridge over a puddle it dug itself.
            //
            // It used to demand open cloud off BOTH sides — a perfectly
            // symmetric 31-tile strait. Counted over a whole realm disc on
            // 2026-09-07, on three seeds, that rule left 0, 0 and 1 toll
            // bridges in the world: the kind was effectively unshippable. The
            // preset paints its own stream across the middle
            // (RealmPoiPresets.tollBridge fills rows 9-13 with mistsea), so what
            // the world has to supply is a stream to JOIN, not one to match.
            int middleY = y + height / 2;
            return skyLand(seed, x + width / 2, y)
                    && skyLand(seed, x + width / 2, y + height - 1)
                    && (!skyLand(seed, x, middleY) || !skyLand(seed, x + width - 1, middleY));
        }
        // Nine samples, rather than corners alone: a large town must not bridge
        // a cloud-sea inlet through the middle of a house block.
        for (int sx = 0; sx <= 2; sx++) {
            for (int sy = 0; sy <= 2; sy++) {
                int tileX = x + sx * (width - 1) / 2;
                int tileY = y + sy * (height - 1) / 2;
                if (!land(realm, seed, tileX, tileY)) return false;
            }
        }
        return true;
    }

    private static boolean land(int realm, int seed, int x, int y) {
        switch (realm) {
            case RealmDepth.REALM_SKYREACH: return skyLand(seed, x, y);
            case RealmDepth.REALM_EDEN: return EdenTerrainPainter.isLand(seed, x, y);
            case RealmDepth.REALM_STEINFELD: return SteinfeldTerrainPainter.isLand(seed, x, y);
            case RealmDepth.REALM_GHOST: return GhostTerrainPainter.isLand(seed, x, y);
            case RealmDepth.REALM_CROOKED:
            case RealmDepth.REALM_HELL: return CrookedTerrainPainter.isLand(seed, x, y);
            default: return false;
        }
    }

    private static boolean skyLand(int seed, int x, int y) {
        long description = SkyTerrainPainter.describeTile(seed, x, y,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed));
        return SkyTerrainPainter.descTile(description) != SkyRegistry.mistseaID;
    }
}
