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
                    // SKY_TOLL_HOUSE, SKY_GRANGE_CELLAR and SKY_TEST_RANGE are
                    // deliberately NOT here. A kind on this lattice is §0.6's
                    // "common" rarity -- one per 72x72 cell, two cells in three
                    // -- and those three are the dossier's "once per world":
                    // their loot is a person. They are stamped by
                    // SkyLandmarkPois off SkyLevel.ensureWardenSpire instead.
                    // The toll-house rode this array from 2026-09-09 until
                    // 2026-09-10, which is why a world could hold four of it.
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
            // Chapter 02 (docs/design/chapter-02-hoards-and-mimics.md) appends
            // one guarded-treasure place to each band after the Skyreach. The
            // Skyreach's two are NOT in the row above: see SKY_HOARD_KINDS.
            {RealmPoiPresets.EDEN_CROWN_GARDEN, RealmPoiPresets.EDEN_FERMENT_HOUSE,
                    RealmPoiPresets.EDEN_HEDGE_LABYRINTH},
            {RealmPoiPresets.STEINFELD_MEMORIAL, RealmPoiPresets.STEINFELD_OSSUARY},
            {RealmPoiPresets.GHOST_ARCHIVE, RealmPoiPresets.GHOST_WEDDING_FEAST},
            {RealmPoiPresets.CROOKED_BAZAAR, RealmPoiPresets.CROOKED_HALL_OF_DOORS},
            {RealmPoiPresets.HELL_BORDER_OFFICE, RealmPoiPresets.HELL_ADMINISTRATION,
                    RealmPoiPresets.HELL_FORGE, RealmPoiPresets.HELL_CARNIVAL},
    };

    /**
     * The Skyreach's two chapter-02 places, on a lattice of their own.
     *
     * <p>Appending them to the Skyreach row above was tried first and failed
     * the gate on the second seed it met (1486191071: {@code skytower
     * accepted=0}). The band is bounded by its own depth and offers ~35-38
     * sites; {@link #skyreachRotate} deals them out by rank, so every kind
     * added to the row takes cells away from the sixteen already there — 18
     * kinds left the 49x55 Sky Tower one or two cells, and both missed land.
     * The dilution is the defect, not the tower.
     *
     * <p>So the hoards get their OWN sites: the same {@link #CELL} grid,
     * re-seeded with {@link #HOARD_SALT} and a far lower
     * {@link #HOARD_SITE_CHANCE}, walked by the same funnel. The sixteen keep
     * every cell they had — lattice 0 is computed exactly as before — and a
     * hoard that lands on one of their rectangles loses to the occupancy board
     * like any other overlap. This is also the rarity §0.6 would give a vault:
     * "per region", a handful per band, not one every few minutes.
     */
    static final int[] SKY_HOARD_KINDS = {
            RealmPoiPresets.SKY_COUNTERFEIT_TREASURY,
            RealmPoiPresets.SKY_FALLEN_OBSERVATORY,
    };
    /** Re-seeds the grid for {@link #SKY_HOARD_KINDS}; never reuse for lattice 0. */
    private static final int HOARD_SALT = 0x4A7D;
    /** Share of cells the hoard lattice offers a site in (lattice 0: {@link #SITE_CHANCE}). */
    private static final float HOARD_SITE_CHANCE = 0.16F;

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
    /**
     * On top of one of §0.6's once-per-world places.
     *
     * <p>{@link SkyLandmarkPois} stamps those off {@code ensureWardenSpire} and
     * not through the preset region, so the {@code villages} occupancy board
     * has never heard of them: without this test a Sky Town could be queued
     * straight through the Grange Cellar and the two would overwrite each
     * other's walls. Their sites are pure functions of the seed, which is what
     * makes the test possible here at all.
     */
    public static final int STAGE_NEAR_LANDMARK = 4;
    /** How many stages there are, for a caller sizing a tally array. */
    public static final int STAGE_COUNT = 5;

    /** Human-readable stage names, indexed by the STAGE_ constants. */
    public static final String[] STAGE_NAMES = {
            "accepted", "splitbyregion", "nearspire", "badground", "nearlandmark"};

    /** Tiles of clear ground kept around a once-per-world place's footprint. */
    private static final int LANDMARK_CLEARANCE = 16;

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
        java.awt.Rectangle[] landmarks = landmarkRects(seed);
        surveyLattice(seed, false, startX, startY, endX, endY, originX, originY, landmarks, visitor);
        surveyLattice(seed, true, startX, startY, endX, endY, originX, originY, landmarks, visitor);
        // Last, so on the occupancy board they come after everything the two
        // lattices queued in the same region -- and they were chosen clear of
        // all of it, so the board has nothing to reject them for.
        for (int[] rescue : skyreachRescues(seed)) {
            int centreX = rescue[1] + RealmPoiPresets.width(rescue[0]) / 2;
            int centreY = rescue[2] + RealmPoiPresets.height(rescue[0]) / 2;
            if (centreX < startX || centreX >= endX || centreY < startY || centreY >= endY) continue;
            visitor.site(rescue[0], RealmDepth.REALM_SKYREACH, rescue[1], rescue[2],
                    RealmPoiPresets.width(rescue[0]), RealmPoiPresets.height(rescue[0]), STAGE_ACCEPTED);
        }
    }

    // -----------------------------------------------------------------------
    // The Skyreach rescue: every Skyreach kind stands at least once per world.
    // -----------------------------------------------------------------------

    /** Preset regions are 64 level regions of 16 tiles (WorldPresetsRegion, jar 1.3.2). */
    private static final int PRESET_REGION_TILES = 64 * 16;
    /** Clear ground a rescued place keeps from every place the lattices accepted. */
    private static final int RESCUE_MARGIN = 16;
    /** Salt for the site an EMPTY cell offers the rescue; never used by lattice 0. */
    private static final int RESCUE_SALT = 0x7E5C;
    /** Last few seeds' rescues; a region generation asks once per preset region. */
    private static final java.util.Map<Integer, int[][]> RESCUE_CACHE =
            new java.util.LinkedHashMap<Integer, int[][]>(16, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Integer, int[][]> eldest) {
                    return size() > 8;
                }
            };

    /**
     * The places this seed's Skyreach would otherwise not have: one
     * {@code {kind, x, y}} per Skyreach kind that the two lattices leave with no
     * standing site anywhere in the band.
     *
     * <p><b>Why this exists.</b> {@link #skyreachRotate} deals the band's ~32-45
     * sites to its kinds by rank, which gives every kind about two cells. Two is
     * not many: when both of a kind's cells fail — one lands on a landmark's
     * clearance, the other fails the kind's own ground test and the next kind
     * in the rotation takes it — the kind stands nowhere in the world. Measured
     * offline over 300 seeds on 2026-09-25: <b>15 seeds (5%)</b> lost one kind,
     * spread over ten different kinds, the reef most often (it wants open cloud,
     * the rarest ground on a lattice centred on islands). Seed {@code F6mfM}
     * lost the Falling Institute exactly so: {@code nearlandmark=1}, and its
     * other cell taken by a neighbour.
     *
     * <p><b>What it does.</b> A pure function of the seed, computed over the
     * whole band (it is bounded, so this is ~25 preset regions of survey): run
     * both lattices, simulate the occupancy board per preset region, and for
     * every Skyreach kind with no surviving site, try it on the band's cells
     * nearest the spire first — each cell's own site, or for a cell the lattice
     * left empty a site hashed with {@link #RESCUE_SALT} — with the same
     * {@link #SITE_ATTEMPTS} jittered attempts, the same ground test, and a
     * {@link #RESCUE_MARGIN} of clearance from every accepted rectangle, the
     * landmarks, the spire ring and the other rescues. First fit wins.
     *
     * <p><b>Additive only.</b> Nothing the lattices place moves or changes kind;
     * on a seed where every kind already stands this returns nothing and the
     * world is byte-for-byte the one it was. On an existing save, the engine
     * recomputes presets per preset region at load and drops any whose regions
     * are already generated ({@code LevelPresetsRegion.startGenerateRegion}), so
     * a rescue appears only if it lands wholly on ground the save has not yet
     * generated, and never overwrites anything.
     */
    static int[][] skyreachRescues(int seed) {
        synchronized (RESCUE_CACHE) {
            int[][] cached = RESCUE_CACHE.get(seed);
            if (cached != null) return cached;
        }
        int[][] rescues = computeSkyreachRescues(seed);
        synchronized (RESCUE_CACHE) {
            RESCUE_CACHE.put(seed, rescues);
        }
        return rescues;
    }

    private static int[][] computeSkyreachRescues(final int seed) {
        final int originX = SkyOrigin.originX(seed);
        final int originY = SkyOrigin.originY(seed);
        java.awt.Rectangle[] landmarks = landmarkRects(seed);
        int reach = (int) (RealmDepth.bandEnd(RealmDepth.REALM_SKYREACH)
                * RealmDepth.DEPTH_SCALE) + 2 * CELL;

        // 1. What the two lattices place in the band, through a simulated board.
        final boolean[] standing = new boolean[RealmPoiPresets.COUNT];
        final java.util.List<java.awt.Rectangle> taken = new java.util.ArrayList<>();
        for (int prx = Math.floorDiv(originX - reach, PRESET_REGION_TILES);
                prx <= Math.floorDiv(originX + reach, PRESET_REGION_TILES); prx++) {
            for (int pry = Math.floorDiv(originY - reach, PRESET_REGION_TILES);
                    pry <= Math.floorDiv(originY + reach, PRESET_REGION_TILES); pry++) {
                final java.util.List<java.awt.Rectangle> board = new java.util.ArrayList<>();
                int sx = prx * PRESET_REGION_TILES;
                int sy = pry * PRESET_REGION_TILES;
                SiteVisitor boardVisitor = (kind, realm, x, y, width, height, stage) -> {
                    if (stage != STAGE_ACCEPTED) return;
                    java.awt.Rectangle rect = new java.awt.Rectangle(x, y, width, height);
                    for (java.awt.Rectangle other : board) {
                        if (other.intersects(rect)) return;
                    }
                    board.add(rect);
                    taken.add(rect);
                    if (realm == RealmDepth.REALM_SKYREACH) standing[kind] = true;
                };
                surveyLattice(seed, false, sx, sy, sx + PRESET_REGION_TILES, sy + PRESET_REGION_TILES,
                        originX, originY, landmarks, boardVisitor);
                surveyLattice(seed, true, sx, sy, sx + PRESET_REGION_TILES, sy + PRESET_REGION_TILES,
                        originX, originY, landmarks, boardVisitor);
            }
        }
        java.util.List<Integer> missing = new java.util.ArrayList<>();
        for (int kind : REALM_KINDS[RealmDepth.REALM_SKYREACH]) {
            if (!standing[kind]) missing.add(kind);
        }
        for (int kind : SKY_HOARD_KINDS) {
            if (!standing[kind]) missing.add(kind);
        }
        if (missing.isEmpty()) return new int[0][];

        // 2. The band's cells, nearest the spire first.
        java.util.List<int[]> cells = new java.util.ArrayList<>();
        for (int cx = Math.floorDiv(originX - reach, CELL); cx <= Math.floorDiv(originX + reach, CELL); cx++) {
            for (int cy = Math.floorDiv(originY - reach, CELL); cy <= Math.floorDiv(originY + reach, CELL); cy++) {
                boolean own = hasSite(seed, cx, cy);
                int gridSeed = own ? seed : seed + RESCUE_SALT;
                int sx = siteX(gridSeed, cx, cy);
                int sy = siteY(gridSeed, cx, cy);
                if (nearSpire(sx - originX, sy - originY)) continue;
                if (RealmDepth.realmAt(seed, sx, sy, originX, originY) != RealmDepth.REALM_SKYREACH) continue;
                long dx = sx - originX;
                long dy = sy - originY;
                cells.add(new int[]{cx, cy, sx, sy, (int) Math.min(Integer.MAX_VALUE, dx * dx + dy * dy)});
            }
        }
        cells.sort((a, b) -> a[4] != b[4] ? Integer.compare(a[4], b[4])
                : (a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1])));

        // 3. Each missing kind takes the first cell it fits in.
        java.util.List<int[]> rescues = new java.util.ArrayList<>();
        for (int kind : missing) {
            int width = RealmPoiPresets.width(kind);
            int height = RealmPoiPresets.height(kind);
            placed:
            for (int[] cell : cells) {
                for (int attempt = 0; attempt < SITE_ATTEMPTS; attempt++) {
                    int spotX = cell[2] + jitter(seed + RESCUE_SALT, cell[0], cell[1], kind, attempt, 0);
                    int spotY = cell[3] + jitter(seed + RESCUE_SALT, cell[0], cell[1], kind, attempt, 1);
                    if (nearSpire(spotX - originX, spotY - originY)) continue;
                    if (RealmDepth.realmAt(seed, spotX, spotY, originX, originY) != RealmDepth.REALM_SKYREACH) continue;
                    // Whole inside the preset region that owns its centre, the
                    // rule every lattice footprint keeps.
                    int regionX = Math.floorDiv(spotX, PRESET_REGION_TILES) * PRESET_REGION_TILES;
                    int regionY = Math.floorDiv(spotY, PRESET_REGION_TILES) * PRESET_REGION_TILES;
                    int x = clamp(spotX - width / 2, regionX, regionX + PRESET_REGION_TILES - width - 1);
                    int y = clamp(spotY - height / 2, regionY, regionY + PRESET_REGION_TILES - height - 1);
                    if (!inRegion(x + width / 2, y + height / 2, regionX, regionY)) continue;
                    if (intersectsLandmark(landmarks, x, y, width, height)) continue;
                    if (intersectsSpireKeep(x, y, width, height, originX, originY)) continue;
                    java.awt.Rectangle grown = new java.awt.Rectangle(x - RESCUE_MARGIN, y - RESCUE_MARGIN,
                            width + 2 * RESCUE_MARGIN, height + 2 * RESCUE_MARGIN);
                    boolean clear = true;
                    for (java.awt.Rectangle other : taken) {
                        if (other.intersects(grown)) {
                            clear = false;
                            break;
                        }
                    }
                    if (!clear) continue;
                    if (!validSite(kind, RealmDepth.REALM_SKYREACH, seed, x, y, width, height)) continue;
                    rescues.add(new int[]{kind, x, y});
                    taken.add(new java.awt.Rectangle(x, y, width, height));
                    break placed;
                }
            }
        }
        return rescues.toArray(new int[0][]);
    }

    private static boolean inRegion(int x, int y, int regionX, int regionY) {
        return x >= regionX && x < regionX + PRESET_REGION_TILES
                && y >= regionY && y < regionY + PRESET_REGION_TILES;
    }

    /**
     * One lattice of {@link #survey}. {@code hoards == false} is the lattice
     * every kind before chapter 02 stands on, computed exactly as it always
     * was; {@code true} is {@link #SKY_HOARD_KINDS}'s sparser one.
     */
    private static void surveyLattice(int seed, boolean hoards, int startX, int startY,
            int endX, int endY, int originX, int originY,
            java.awt.Rectangle[] landmarks, SiteVisitor visitor) {
        // The grid's own seed. Terrain, realms and landmarks still read the
        // world's seed; only where the cells put their sites moves.
        int gridSeed = hoards ? seed + HOARD_SALT : seed;
        for (int cellX = Math.floorDiv(startX, CELL); cellX <= Math.floorDiv(endX, CELL); cellX++) {
            for (int cellY = Math.floorDiv(startY, CELL); cellY <= Math.floorDiv(endY, CELL); cellY++) {
                if (hoards
                        ? SkyNoise.hash(gridSeed + SALT, cellX, cellY) >= HOARD_SITE_CHANCE
                        : !hasSite(seed, cellX, cellY)) continue;
                int siteX = siteX(gridSeed, cellX, cellY);
                int siteY = siteY(gridSeed, cellX, cellY);
                // Not this region's cell. Silent rather than a stage: the
                // neighbour that owns it will report it, and counting it here
                // too would make every census double-count its borders.
                if (siteX < startX || siteX >= endX || siteY < startY || siteY >= endY) continue;

                int realm = RealmDepth.realmAt(seed, siteX, siteY, originX, originY);
                if (hoards && realm != RealmDepth.REALM_SKYREACH) continue;
                int[] choices = hoards ? SKY_HOARD_KINDS : REALM_KINDS[realm];
                int rotate = -1;
                if (!hoards && realm == RealmDepth.REALM_SKYREACH) {
                    rotate = skyreachRotate(seed, cellX, cellY, originX, originY, choices.length);
                }
                if (rotate < 0) {
                    rotate = Math.min(choices.length - 1,
                            (int) (SkyNoise.hash(gridSeed + SALT + 3, cellX, cellY) * choices.length));
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
                // A cell whose own site sits on a once-per-world place is
                // reported as such rather than as bad ground: the ground there
                // is fine, it is simply taken by a building the occupancy board
                // never saw.
                boolean onLandmark = false;
                for (java.awt.Rectangle rect : landmarks) {
                    if (rect.contains(siteX, siteY)) {
                        onLandmark = true;
                        break;
                    }
                }
                if (onLandmark) {
                    int kind = choices[rotate];
                    visitor.site(kind, realm, siteX - RealmPoiPresets.width(kind) / 2,
                            siteY - RealmPoiPresets.height(kind) / 2,
                            RealmPoiPresets.width(kind), RealmPoiPresets.height(kind),
                            STAGE_NEAR_LANDMARK);
                    continue;
                }
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
                        int spotX = siteX + jitter(gridSeed, cellX, cellY, kind, attempt, 0);
                        int spotY = siteY + jitter(gridSeed, cellX, cellY, kind, attempt, 1);
                        // A nudge must not walk a site into the spire's ring.
                        if (attempt > 0 && nearSpire(spotX - originX, spotY - originY)) continue;
                        int x = clamp(spotX - width / 2, startX, highX);
                        int y = clamp(spotY - height / 2, startY, highY);
                        lastX = x;
                        lastY = y;
                        // ...and a nudged footprint must not land on one either.
                        if (intersectsLandmark(landmarks, x, y, width, height)) continue;
                        // The ring above tests a CENTRE; this tests the whole
                        // footprint after the region clamp, which can move it.
                        if (intersectsSpireKeep(x, y, width, height, originX, originY)) continue;
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

    /**
     * Half-width of the square around the spire no lattice FOOTPRINT may touch:
     * the spire plot, the Spire Village ({@code SpireVillage.RADIUS} 38), and
     * the hub island the painter guarantees ({@code SkyOrigin.HUB_RADIUS} 56)
     * with a margin. {@code SkyreachStatusCommand}'s painter oracle scans
     * exactly this square, so the two cannot drift.
     *
     * <p>{@link #SPIRE_CLEARANCE} alone did not keep it: it tests the site's
     * CENTRE, and a 49x55 Sky Tower centred 105 tiles out reaches 39 tiles
     * from the spire -- the village's own edge. Seed 6xK4d did exactly that
     * ({@code skytower@+39,+57}) and the oracle counted the tower's floor as
     * 30 painter mismatches. The region clamp can also pull a centre inside
     * the ring (offline: a Fallen Observatory centred 60 tiles out). Measured
     * over 501 seeds before this test: 5 put a footprint in this square.
     */
    public static final int SPIRE_KEEP_CLEAR = 64;

    private static boolean intersectsSpireKeep(int x, int y, int width, int height,
            int originX, int originY) {
        return x <= originX + SPIRE_KEEP_CLEAR && x + width - 1 >= originX - SPIRE_KEEP_CLEAR
                && y <= originY + SPIRE_KEEP_CLEAR && y + height - 1 >= originY - SPIRE_KEEP_CLEAR;
    }

    /** The clear ring around the spire, as one named test both callers share. */
    private static boolean nearSpire(int dx, int dy) {
        return dx * dx + dy * dy < SPIRE_CLEARANCE * SPIRE_CLEARANCE;
    }

    /**
     * The ground §0.6's once-per-world places occupy in this world, each grown
     * by {@link #LANDMARK_CLEARANCE} so a lattice place does not end up sharing
     * a wall with one. A pure function of the seed, like every other test here.
     */
    private static java.awt.Rectangle[] landmarkRects(int seed) {
        java.awt.Rectangle[] rects = new java.awt.Rectangle[SkyLandmarkPois.KINDS.length];
        for (int index = 0; index < rects.length; index++) {
            SkyLandmarkPois.Site site = SkyLandmarkPois.site(seed, index);
            int kind = SkyLandmarkPois.KINDS[index];
            rects[index] = new java.awt.Rectangle(
                    site.x - LANDMARK_CLEARANCE, site.y - LANDMARK_CLEARANCE,
                    RealmPoiPresets.width(kind) + 2 * LANDMARK_CLEARANCE,
                    RealmPoiPresets.height(kind) + 2 * LANDMARK_CLEARANCE);
        }
        return rects;
    }

    private static boolean intersectsLandmark(java.awt.Rectangle[] landmarks,
            int x, int y, int width, int height) {
        for (java.awt.Rectangle rect : landmarks) {
            if (rect.intersects(new java.awt.Rectangle(x, y, width, height))) {
                return true;
            }
        }
        return false;
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
        // Chapter 02's six: guardians, guards and mimics, read off their own
        // plans. One call, so the cast lives beside its loot tables.
        RealmPoiHoards.placeInhabitants(kind, level, x, y);
        // The three once-per-world places have no branch here, and must not
        // get one: they are not on this lattice any more, and their people,
        // their guards and their unique rewards are all placed by
        // SkyLandmarkPois, which is also the only path that CLAIMS the name in
        // SkywatchWorldData. The branch that used to stand here spawned Magpie
        // without claiming her, so a world could hold two -- one in a ledger
        // room and one beside a workshop, which is exactly what
        // residentsClaimed exists to prevent.
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
        // The Sky Tower (49x55) joins the town's rule on 2026-09-24. It is the
        // one footprint larger than the town's, and its plan is a stepped
        // silhouette whose two upper corners are unbuilt margin anyway, so
        // the corner samples guarded nothing but still cost it every cell:
        // seed 1574516053 drew `realmpoi kind skytower: accepted=0` with
        // `badground=0` for the band, the town's 2026-09-10 failure exactly.
        // Its preset carries dryRing like the town's.
        if (kind == RealmPoiPresets.SKY_TOWN || kind == RealmPoiPresets.SKY_TOWER) {
            // The catalogue's widest footprint, 57x41, and the only kind that
            // stood NOWHERE on 2026-09-10's run: `realmpoi kind skytown:
            // accepted=0 queued=0 nearest=NONE`. The nine-sample test below
            // includes the four CORNERS of the rectangle, and a 57x41 rectangle
            // whose every corner is dry is a rare thing in a realm this wet --
            // the band offers the town two or three lattice cells, and it lost
            // all of them.
            //
            // So the corners come off the test and the cross stays: centre,
            // and the middle of each edge. What the corners were guarding
            // against -- an object standing in the cloud sea -- is now guarded
            // where it actually happens, by RealmPoiPresets.dryRing, which
            // pushes the world's water off the ring of every object the preset
            // writes.
            //
            // BE PRECISE ABOUT WHAT THIS SAMPLES, because it is less than the
            // nine points did: the centre is the plaza (RealmPoiPresets.skyTown
            // puts it at 22,14 13x13) and the four edge midpoints are the ends
            // of the two carriageways. The four house blocks sit in the four
            // QUADRANTS and no sample lands on one. So the promise this test
            // now makes is "the plaza and both roads are on real ground, and
            // the rim may reach a cloud-sea edge" -- a town on a floating
            // island looks like that anyway. It does NOT any longer promise
            // that a house block is not over water: dryRing keeps such a house
            // standing, on its own floor with a cloudturf rim, which is
            // survivable but not what the dossier draws. Sampling the four
            // block centres instead of the four road ends would cost the same
            // five calls and guard the thing the sentence above claims; it is
            // written up under Offen in this pass's handover rather than
            // changed at the end of a delivery run.
            return skyLand(seed, x + width / 2, y + height / 2)
                    && skyLand(seed, x + width / 2, y)
                    && skyLand(seed, x + width / 2, y + height - 1)
                    && skyLand(seed, x, y + height / 2)
                    && skyLand(seed, x + width - 1, y + height / 2);
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
            case RealmDepth.REALM_CROOKED: return CrookedTerrainPainter.isLand(seed, x, y);
            // Hell answers for itself now that it has a painter. The two
            // methods are word for word the same -- both are the plane's shared
            // island field against its shared waterline -- so the four Hell
            // POIs land exactly where they did before.
            case RealmDepth.REALM_HELL:
                return stairwaytoheaven.realms.hell.HellTerrainPainter.isLand(seed, x, y);
            default: return false;
        }
    }

    private static boolean skyLand(int seed, int x, int y) {
        long description = SkyTerrainPainter.describeTile(seed, x, y,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed));
        return SkyTerrainPainter.descTile(description) != SkyRegistry.mistseaID;
    }
}
