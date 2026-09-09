package stairwaytoheaven.worldgen.pois;

import java.awt.Dimension;

import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.util.GameRandom;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.WorldPreset;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.realms.crooked.CrookedTerrainPainter;
import stairwaytoheaven.realms.eden.EdenTerrainPainter;
import stairwaytoheaven.realms.ghost.GhostTerrainPainter;
import stairwaytoheaven.realms.steinfeld.SteinfeldTerrainPainter;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.SkyTerrainPainter;

/** Places the thirteen inhabited POIs into their realm bands on {@code skyreach2}. */
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
     */
    private static final int SITE_ATTEMPTS = 16;
    /** How far a nudged attempt may move from the cell's own site, in tiles. */
    private static final int SITE_JITTER = CELL / 2;

    private static final int[][] REALM_KINDS = {
            {RealmPoiPresets.SKY_TOWER, RealmPoiPresets.SKY_TOWN,
                    RealmPoiPresets.SKY_TOLL_BRIDGE, RealmPoiPresets.SKY_INN,
                    RealmPoiPresets.SKY_TOLL_HOUSE},
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
     * Rotating keeps the mix even across cells while letting the ground have the
     * last word, which is also what puts the big presets where there is room.
     */
    public static void survey(int seed, int startX, int startY, int endX, int endY, SiteVisitor visitor) {
        int originX = SkyOrigin.originX(seed);
        int originY = SkyOrigin.originY(seed);
        for (int cellX = Math.floorDiv(startX, CELL); cellX <= Math.floorDiv(endX, CELL); cellX++) {
            for (int cellY = Math.floorDiv(startY, CELL); cellY <= Math.floorDiv(endY, CELL); cellY++) {
                if (SkyNoise.hash(seed + SALT, cellX, cellY) >= SITE_CHANCE) continue;
                int siteX = Math.round(cellX * CELL + SkyNoise.hash(seed + SALT + 1, cellX, cellY) * CELL);
                int siteY = Math.round(cellY * CELL + SkyNoise.hash(seed + SALT + 2, cellX, cellY) * CELL);
                // Not this region's cell. Silent rather than a stage: the
                // neighbour that owns it will report it, and counting it here
                // too would make every census double-count its borders.
                if (siteX < startX || siteX >= endX || siteY < startY || siteY >= endY) continue;

                int realm = RealmDepth.realmAt(seed, siteX, siteY, originX, originY);
                int[] choices = REALM_KINDS[realm];
                int rotate = Math.min(choices.length - 1,
                        (int) (SkyNoise.hash(seed + SALT + 3, cellX, cellY) * choices.length));

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
                        // Which of the thirteen this rectangle is. Without it the
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
    }

    private static void spawn(Level level, String mobID, int tileX, int tileY) {
        necesse.entity.mobs.Mob mob = necesse.engine.registries.MobRegistry.getMob(mobID, level);
        if (mob == null) return;
        mob.canDespawn = false;
        level.entityManager.addMob(mob, tileX * 32 + 16, tileY * 32 + 16);
    }

    private static boolean validSite(int kind, int realm, int seed, int x, int y, int width, int height) {
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
