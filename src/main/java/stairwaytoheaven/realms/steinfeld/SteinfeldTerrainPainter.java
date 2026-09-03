package stairwaytoheaven.realms.steinfeld;

import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.SkyTerrainPainter;

/**
 * Steinfeld as a BAND of the one plane — and the one place the realm's whole
 * idea is actually implemented.
 *
 * <p><b>This is no longer a level's painter.</b> {@code docs/PLAN_ONE_PLANE.md}
 * retired the {@code steinfeld2} dimension: Steinfeld is the realm
 * {@link RealmDepth} gives depth 0.42-0.58, and
 * {@link SkyTerrainPainter#describeTile} calls {@link #describeBand} for every
 * tile the realm pick lands here.
 *
 * <p><b>And the gradient below is now the plane's own.</b> It used to be a
 * second distance system: a radial from a fixed gate at (0,0) over 700 tiles.
 * That was the right shape and the wrong origin — the concept has ONE distance,
 * from ONE {@code SkyOrigin} (§3), and two of them is how they drift apart. The
 * three bands are now cut out of {@link RealmDepth#localDepth}, i.e. how far
 * through Steinfeld's OWN band the tile is, so "green near Eden, grey out
 * toward the Ghost Realm" is literally true of the plane rather than true of a
 * private coordinate system that happened to agree.
 *
 * <h2>The idea</h2>
 * The player's line for this realm is <b>"order decays"</b>, and
 * {@code docs/WORLD_DESIGN.md} §7 and A3.4 say what that has to look like:
 *
 * <blockquote>Near Eden: green grass, bright stone, single Eden flowers,
 * broken angel statues. Further out: pale grass, big stone slabs, dead trees,
 * gravestones, fog.</blockquote>
 *
 * <p>That gradient is not prose to be honoured in a design doc. It is a field,
 * it is computed here, and it is the reason every function below is written as
 * a pure function of {@code (seed, tileX, tileY)}: the same question can be
 * asked by the painter, by the pressure field that decides where enemies may
 * appear, by the guard placement, and by an offline harness, and all four get
 * the same answer.
 *
 * <h2>The field: depth, and the warp that stops it being a bullseye</h2>
 * {@link #depthAt} is the distance from Steinfeld's gate ({@link #ORIGIN_X},
 * {@link #ORIGIN_Y}) divided by {@link #BAND_SPAN}, plus a low-frequency noise
 * warp. Without the warp the realm would be three concentric rings, which
 * reads as a menu rather than as country. With it the border between two bands
 * wanders by up to {@code BAND_WARP * BAND_SPAN} = ±190 tiles, so grave soil
 * reaches in toward Eden along some lines and green grass survives far out
 * along others — decay arriving unevenly, which is what decay does.
 *
 * <h2>What is borrowed, and why every sheet is somebody else's</h2>
 * Not one pixel of new art was drawn for this realm. Every ground is an
 * existing mod sheet or a vanilla sheet read by path, and the CHOICE of sheet
 * is where the gradient lives:
 *
 * <pre>
 * band            grass                stone                  soil
 * Quiet Meadow    Eden grass (mod)     Weathered Stone (mod)  —
 * Slab Fields     Pale Grass (mod)     Cracked Marble (van.)  Dead Soil (van.)
 * Grave Heath     Ash Grass (mod)      Mist Stone (vanilla)   Grave Soil (van.)
 * </pre>
 *
 * Read down the grass column and the map colour goes 48/84/0 → 178/190/176 →
 * 88/96/88: saturated green, drained pale, grey. Read down the stone column
 * and it goes bright blue-grey → mid grey → green-grey. Both columns are
 * legible on the world map at a glance, which is the test this realm had to
 * pass.
 *
 * <h2>Region safety</h2>
 * Every tile is decided from its own coordinates alone. Nothing here writes
 * into a neighbouring region, and no structure needs a preset stamped across a
 * region border, which is what lets the POIs (see {@link #poiTile} /
 * {@link #poiObject}) be real buildings in a level that streams region by
 * region.
 */
public final class SteinfeldTerrainPainter {

    private SteinfeldTerrainPainter() {
    }

    // ---- The gate, and the gradient it is the origin of ---------------------

    /**
     * The zero of the gradient: the plane's own origin, the Old Warden Spire.
     *
     * <p>Kept as named constants because the realm's documentation and its
     * offline harness both refer to them, but they are no longer a second
     * origin — {@link #depthAt} asks {@link SkyOrigin} for the real one, and
     * these two are the fallback the pure-noise harness uses when it has no
     * seed. WORLD_DESIGN §3 allows exactly one origin and this file used to
     * hold a second.
     */
    public static final int ORIGIN_X = 0;
    public static final int ORIGIN_Y = 0;

    /**
     * The span the three bands are cut out of.
     *
     * <p>Now Steinfeld's own realmDepth band rather than an invented 700-tile
     * radius: depth 0.32 to 0.70 at {@link RealmDepth#DEPTH_SCALE} 6000 is
     * 1920 to 4200 tiles, i.e. a 2280-tile walk from the Eden overlap to the
     * Ghost overlap. That is the distance the "order decays" ramp now runs
     * over, and it moves with the world-size dial instead of contradicting it.
     */
    public static final float BAND_SPAN =
            (RealmDepth.bandEnd(RealmDepth.REALM_STEINFELD)
                    - RealmDepth.bandStart(RealmDepth.REALM_STEINFELD)) * RealmDepth.DEPTH_SCALE;
    /** Low-frequency warp of the band border, as a fraction of BAND_SPAN. */
    public static final float BAND_WARP = 0.27F;
    public static final float BAND_SCALE = 260.0F;
    public static final int SALT_BAND = 401;

    /** {@code depthAt} below this is Quiet Meadow. */
    public static final float QUIET_BELOW = 0.34F;
    /** ...below this is Slab Fields; at or above it, Grave Heath. */
    public static final float SLAB_BELOW = 0.70F;

    public static final int BAND_QUIET = 0;
    public static final int BAND_SLAB = 1;
    public static final int BAND_HEATH = 2;

    // ---- Ground mix ---------------------------------------------------------

    public static final float PATCH_SCALE = 34.0F;
    public static final int SALT_PATCH = 409;
    public static final float VEIN_SCALE = 19.0F;
    public static final int SALT_VEIN = 419;
    public static final int SALT_PROP = 431;

    /** Surface codes — registry-free, so an offline harness can read the field. */
    public static final int S_EDEN_GRASS = 0;
    public static final int S_PALE_GRASS = 1;
    public static final int S_WEATHERED_STONE = 2;
    public static final int S_CRACKED_MARBLE = 3;
    public static final int S_DEAD_SOIL = 4;
    public static final int S_ASH_GRASS = 5;
    public static final int S_MIST_STONE = 6;
    public static final int S_GRAVE_SOIL = 7;

    /** Prop codes. {@link #P_NONE} is the overwhelming majority of tiles. */
    public static final int P_NONE = 0;
    public static final int P_WITHERED_TUFT = 1;
    public static final int P_PALE_REED = 2;
    public static final int P_WIDOW_FLOWER = 3;
    public static final int P_DEAD_HEAVEN_BLOOM = 4;
    public static final int P_GHOST_MUSHROOM = 5;
    public static final int P_SPIRIT_MOSS = 6;
    public static final int P_PALE_STONE_ROCK = 7;
    public static final int P_GRAVE_SALT_ROCK = 8;
    public static final int P_DEAD_TREE = 9;
    public static final int P_GRAVESTONE = 10;
    public static final int P_MOURNER_STATUE = 11;
    public static final int P_BROKEN_ANGEL = 12;
    public static final int P_CHAPEL_COLUMN = 13;
    public static final int P_HEAVEN_SLAB = 14;
    public static final int P_GRAVE_FENCE = 15;
    public static final int P_CRATE = 16;

    // ---- Guarded places -----------------------------------------------------

    /**
     * One lattice for all three POI kinds, and the kind chosen from the site's
     * own hash.
     *
     * <p>ONE lattice rather than three is a correctness decision, not a tidiness
     * one. {@link SteinfeldPressure} has to know where the guarded ground is
     * and {@link stairwaytoheaven.realms.steinfeld.SteinfeldLevel} has to put
     * the pack on it; with three lattices those two would have to agree three
     * times, and the mod has already shipped one bug of exactly that shape (the
     * guards standing where the loot is not — see {@code SkyTerrainPainter
     * .nearestSite}'s header). With one lattice they cannot disagree.
     *
     * <p>Rate: a 180x180 cell is 32,400 tiles and 60% of cells carry a site, so
     * there is roughly one POI per 54,000 tiles — one per 230x230 tile square.
     * A player walking the 500 tiles from the gate to the Grave Heath crosses
     * two or three. Rare enough to be a find; common enough that the realm has
     * somewhere to go.
     */
    public static final int POI_CELL = 180;
    public static final float POI_CHANCE = 0.60F;
    public static final int SALT_POI = 443;
    /** Tiles from a site centre that the POI itself occupies. */
    public static final float POI_RADIUS = 9.0F;

    public static final int POI_GRAVE_FIELD = 0;
    public static final int POI_RUINED_CHAPEL = 1;
    public static final int POI_STATUE_FIELD = 2;

    // ---- The field ----------------------------------------------------------

    /**
     * How far into the realm this tile is: 0 at the gate, 1 at the far edge of
     * the gradient. Pure, and the only thing every other decision hangs on.
     */
    public static float depthAt(int seed, int tileX, int tileY) {
        return localDepth(seed, tileX, tileY, RealmDepth.depthAt(tileX, tileY,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed)));
    }

    /**
     * The same gradient for a caller that already has the plane's depth — the
     * allocation-free hot path the band painter takes.
     *
     * <p>The warp is what stops the realm reading as three concentric rings:
     * the border between two bands wanders by up to
     * {@code BAND_WARP * BAND_SPAN} = ±616 tiles, so grave soil reaches in
     * toward Eden along some lines and green grass survives far out along
     * others. Decay arriving unevenly, which is what decay does.
     */
    public static float localDepth(int seed, int tileX, int tileY, float planeDepth) {
        float radial = RealmDepth.localDepth(RealmDepth.REALM_STEINFELD, planeDepth);
        float warp = (SkyNoise.fbm(seed + SALT_BAND, tileX, tileY, BAND_SCALE, 2) - 0.5F) * 2.0F * BAND_WARP;
        float depth = radial + warp;
        return depth < 0.0F ? 0.0F : depth;
    }

    /** Which of the three bands a tile belongs to. */
    public static int bandAt(int seed, int tileX, int tileY) {
        return bandFor(depthAt(seed, tileX, tileY));
    }

    /** Which of the three bands a local depth is in. */
    public static int bandFor(float depth) {
        if (depth < QUIET_BELOW) {
            return BAND_QUIET;
        }
        return depth < SLAB_BELOW ? BAND_SLAB : BAND_HEATH;
    }

    /**
     * The ground under a tile, as a registry-free surface code.
     *
     * <p>Each band has a base grass, a patch material and a vein material, and
     * the thresholds tighten outward so the bare, hard ground takes more of the
     * picture the further out you go. That is the whole "order decays" curve in
     * six numbers.
     */
    public static int surfaceAt(int seed, int tileX, int tileY) {
        int band = bandAt(seed, tileX, tileY);
        float patch = SkyNoise.fbm(seed + SALT_PATCH, tileX, tileY, PATCH_SCALE, 2);
        float vein = SkyNoise.fbm(seed + SALT_VEIN, tileX, tileY, VEIN_SCALE, 2);
        if (band == BAND_QUIET) {
            // Eden has not quite let go. Stone is the exception here, and the
            // buried slab is a rumour of what is coming.
            if (vein > 0.86F) {
                return S_CRACKED_MARBLE;
            }
            if (vein > 0.70F) {
                return S_WEATHERED_STONE;
            }
            return patch > 0.60F ? S_PALE_GRASS : S_EDEN_GRASS;
        }
        if (band == BAND_SLAB) {
            // The slabs take over and the first bare earth opens up.
            if (vein > 0.88F) {
                return S_WEATHERED_STONE;
            }
            if (vein > 0.68F) {
                return S_DEAD_SOIL;
            }
            return patch > 0.55F ? S_CRACKED_MARBLE : S_PALE_GRASS;
        }
        // Grave Heath: grass is grey, stone is cold, and the ground itself is
        // turned earth over half of it.
        if (vein > 0.86F) {
            return S_DEAD_SOIL;
        }
        if (vein > 0.66F) {
            return S_MIST_STONE;
        }
        return patch > 0.52F ? S_GRAVE_SOIL : S_ASH_GRASS;
    }

    /**
     * What stands on a tile of open country, as a registry-free prop code.
     *
     * <p><b>Density.</b> {@code docs/WORLD_DESIGN.md} A4.2 is a judgement on
     * the state this mod shipped in: <i>"nichts so im Überfluss dass man nach
     * einem Run schon so viel gesammelt hat dass man Kisten füllen kann"</i>,
     * measured at roughly one object per three walkable tiles in the Skyreach.
     * The thresholds below top out at <b>0.082</b> — about one object per
     * twelve tiles — and the two mineable nodes together take <b>0.011</b> of
     * that, one per ninety tiles. A run through the Reach fills a stack, not a
     * chest.
     *
     * <p>The mix is also a third statement of the gradient: flowers and bright
     * stone near Eden, reeds and dead wood in the middle, mushrooms, moss,
     * stray gravestones and grave salt out in the fog.
     */
    public static int propAt(int seed, int tileX, int tileY) {
        int band = bandAt(seed, tileX, tileY);
        int surface = surfaceAt(seed, tileX, tileY);
        float roll = SkyNoise.tileRoll(seed, tileX, tileY, SALT_PROP);
        if (band == BAND_QUIET) {
            if (roll < 0.030F) {
                return organic(surface) ? P_WIDOW_FLOWER : P_NONE;
            }
            if (roll < 0.048F) {
                return organic(surface) ? P_WITHERED_TUFT : P_NONE;
            }
            if (roll < 0.058F) {
                return P_PALE_STONE_ROCK;
            }
            if (roll < 0.062F) {
                return P_HEAVEN_SLAB;
            }
            // A single broken angel in the open grass, near Eden, is §7's own
            // image and the first thing the realm says about itself.
            return roll < 0.0635F ? P_BROKEN_ANGEL : P_NONE;
        }
        if (band == BAND_SLAB) {
            if (roll < 0.028F) {
                return organic(surface) ? P_WITHERED_TUFT : P_NONE;
            }
            if (roll < 0.044F) {
                return organic(surface) ? P_PALE_REED : P_NONE;
            }
            if (roll < 0.056F) {
                return P_HEAVEN_SLAB;
            }
            if (roll < 0.064F) {
                return P_PALE_STONE_ROCK;
            }
            if (roll < 0.068F) {
                return P_DEAD_TREE;
            }
            if (roll < 0.071F) {
                return organic(surface) ? P_DEAD_HEAVEN_BLOOM : P_NONE;
            }
            return roll < 0.0725F ? P_CHAPEL_COLUMN : P_NONE;
        }
        if (roll < 0.026F) {
            return organic(surface) ? P_WITHERED_TUFT : P_NONE;
        }
        if (roll < 0.040F) {
            return organic(surface) ? P_SPIRIT_MOSS : P_NONE;
        }
        if (roll < 0.050F) {
            return organic(surface) ? P_GHOST_MUSHROOM : P_NONE;
        }
        if (roll < 0.058F) {
            return P_DEAD_TREE;
        }
        if (roll < 0.066F) {
            return P_PALE_STONE_ROCK;
        }
        if (roll < 0.071F) {
            return P_GRAVE_SALT_ROCK;
        }
        if (roll < 0.078F) {
            return P_GRAVESTONE;
        }
        return roll < 0.082F ? P_MOURNER_STATUE : P_NONE;
    }

    /** Grass-type flora needs soil under it, not slab. */
    private static boolean organic(int surface) {
        return surface == S_EDEN_GRASS || surface == S_PALE_GRASS || surface == S_ASH_GRASS
                || surface == S_DEAD_SOIL || surface == S_GRAVE_SOIL;
    }

    // ---- The guarded places -------------------------------------------------

    /**
     * The nearest POI site, on the single lattice.
     *
     * <p>Same arithmetic as {@code SkyTerrainPainter.nearestSite}: {@code salt}
     * decides whether a cell carries a site, {@code salt+1} and {@code salt+2}
     * place it inside the cell, and {@code salt+3} is the site's own hash,
     * which here picks the kind.
     */
    public static Site nearestSite(int seed, int tileX, int tileY) {
        int cellX = Math.floorDiv(tileX, POI_CELL);
        int cellY = Math.floorDiv(tileY, POI_CELL);
        float best = Float.MAX_VALUE;
        int bestX = 0;
        int bestY = 0;
        float pick = 0.0F;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                int cx = cellX + ox;
                int cy = cellY + oy;
                if (SkyNoise.hash(seed + SALT_POI, cx, cy) >= POI_CHANCE) {
                    continue;
                }
                float sx = cx * POI_CELL + SkyNoise.hash(seed + SALT_POI + 1, cx, cy) * POI_CELL;
                float sy = cy * POI_CELL + SkyNoise.hash(seed + SALT_POI + 2, cx, cy) * POI_CELL;
                float dx = tileX - sx;
                float dy = tileY - sy;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < best) {
                    best = d;
                    bestX = Math.round(sx);
                    bestY = Math.round(sy);
                    pick = SkyNoise.hash(seed + SALT_POI + 3, cx, cy);
                }
            }
        }
        return new Site(best, bestX, bestY, pick);
    }

    /** What {@link #nearestSite} found. */
    public static final class Site {
        public final float distance;
        public final int tileX;
        public final int tileY;
        public final float pick;

        Site(float distance, int tileX, int tileY, float pick) {
            this.distance = distance;
            this.tileX = tileX;
            this.tileY = tileY;
            this.pick = pick;
        }

        public boolean exists() {
            return this.distance < Float.MAX_VALUE;
        }
    }

    /**
     * Which of the three POIs stands at a site.
     *
     * <p>The site's own hash chooses, but the BAND shifts the choice, so the
     * buildings decay along with the ground: near Eden a site is most often a
     * chapel that is still recognisably a chapel; out in the heath it is most
     * often a grave field. Statue fields stand everywhere, because a statue
     * field is the realm's own subject.
     */
    public static int poiKind(int seed, int siteX, int siteY, float pick) {
        int band = bandAt(seed, siteX, siteY);
        if (band == BAND_QUIET) {
            if (pick < 0.50F) {
                return POI_RUINED_CHAPEL;
            }
            return pick < 0.82F ? POI_STATUE_FIELD : POI_GRAVE_FIELD;
        }
        if (band == BAND_SLAB) {
            if (pick < 0.36F) {
                return POI_RUINED_CHAPEL;
            }
            return pick < 0.68F ? POI_STATUE_FIELD : POI_GRAVE_FIELD;
        }
        if (pick < 0.18F) {
            return POI_RUINED_CHAPEL;
        }
        return pick < 0.46F ? POI_STATUE_FIELD : POI_GRAVE_FIELD;
    }

    /**
     * The ground a POI lays over the natural terrain, or -1 for "leave it".
     *
     * <p>Written as a function of the offset from the site centre so a building
     * that straddles four regions still comes out whole: each region paints the
     * part of it that falls inside itself, and no region ever writes into
     * another. That is the whole reason these are not {@code Preset}s.
     */
    public static int poiSurface(int seed, int tileX, int tileY) {
        Site site = nearestSite(seed, tileX, tileY);
        if (!site.exists() || site.distance > POI_RADIUS) {
            return -1;
        }
        int kind = poiKind(seed, site.tileX, site.tileY, site.pick);
        int dx = tileX - site.tileX;
        int dy = tileY - site.tileY;
        if (kind == POI_GRAVE_FIELD) {
            // A walled plot of turned earth. The fence ring is the edge, so the
            // apron stops one tile inside it.
            return (Math.abs(dx) <= 7 && Math.abs(dy) <= 7) ? S_GRAVE_SOIL : -1;
        }
        if (kind == POI_RUINED_CHAPEL) {
            // A nave: long north-south, and a marble floor that has outlasted
            // its walls.
            return (Math.abs(dx) <= 5 && Math.abs(dy) <= 8) ? S_CRACKED_MARBLE : -1;
        }
        // Statue field: a bright stone apron with a marble heart, so the ring
        // of figures reads against something.
        if (site.distance <= 2.6F) {
            return S_CRACKED_MARBLE;
        }
        return site.distance <= 8.0F ? S_WEATHERED_STONE : -1;
    }

    /**
     * What a POI puts on a tile, or {@link #P_NONE}.
     *
     * <p>Loot is {@link #P_CRATE} — the mod's own salvage crate, which asks
     * {@code Level.getCrateLootTable} and so is answered by whichever Steinfeld
     * biome the crate stands in (see {@link SteinfeldBiome}). Gravestones are
     * loot too and did not have to be taught to be: vanilla's
     * {@code GravestoneObject.getLootTable} already returns the level's crate
     * table for any stone the player did not place, so breaking one open is
     * exactly what a grave field is for.
     */
    public static int poiProp(int seed, int tileX, int tileY) {
        Site site = nearestSite(seed, tileX, tileY);
        if (!site.exists() || site.distance > POI_RADIUS) {
            return P_NONE;
        }
        int kind = poiKind(seed, site.tileX, site.tileY, site.pick);
        int dx = tileX - site.tileX;
        int dy = tileY - site.tileY;
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        if (kind == POI_GRAVE_FIELD) {
            // The wall, with a gap due south for a gateway.
            if ((ax == 8 && ay <= 8) || (ay == 8 && ax <= 8)) {
                return (dy == 8 && ax <= 1) ? P_NONE : P_GRAVE_FENCE;
            }
            if (ax > 7 || ay > 7) {
                return P_NONE;
            }
            // A mourner at the head of the plot, and two crates in the corners
            // where a caretaker would have left them.
            if (dx == 0 && dy == -6) {
                return P_MOURNER_STATUE;
            }
            if (dy == -5 && (dx == -3 || dx == 3)) {
                return P_CRATE;
            }
            // Rows: every other column, every third row. Regular on purpose —
            // it is the only regular thing in the realm.
            return (Math.floorMod(dx, 2) == 0 && Math.floorMod(dy + 1, 3) == 0 && ay <= 6)
                    ? P_GRAVESTONE : P_NONE;
        }
        if (kind == POI_RUINED_CHAPEL) {
            if (ax > 5 || ay > 8) {
                return P_NONE;
            }
            // Two colonnades, three tiles apart, most of them still standing.
            if (ax == 5 && Math.floorMod(dy + 8, 3) == 0) {
                return P_CHAPEL_COLUMN;
            }
            // The angel at the head of the nave, where an altar would be.
            if (dx == 0 && dy == -7) {
                return P_BROKEN_ANGEL;
            }
            if (dy == -6 && (dx == -2 || dx == 2)) {
                return P_MOURNER_STATUE;
            }
            if ((dx == 0 && dy == 4) || (dx == -3 && dy == 1)) {
                return P_CRATE;
            }
            // Fallen roof slabs down the middle of the floor.
            return (SkyNoise.tileRoll(seed, tileX, tileY, SALT_POI + 5) < 0.10F && ay <= 7)
                    ? P_HEAVEN_SLAB : P_NONE;
        }
        // Statue field: a ring of figures at radius ~6, alternating.
        if (site.distance >= 5.4F && site.distance <= 6.6F) {
            if (Math.floorMod(dx + dy, 3) != 0) {
                return P_NONE;
            }
            return Math.floorMod(dx, 2) == 0 ? P_BROKEN_ANGEL : P_MOURNER_STATUE;
        }
        if (site.distance > 8.0F) {
            return P_NONE;
        }
        if ((dx == 0 && dy == 0) || (dx == 2 && dy == 3)) {
            return P_CRATE;
        }
        return (SkyNoise.tileRoll(seed, tileX, tileY, SALT_POI + 6) < 0.09F)
                ? P_HEAVEN_SLAB : P_NONE;
    }

    // ---- Codes -> registry --------------------------------------------------

    /** Registry tile ID for a surface code. */
    public static int tileIdOf(int surface) {
        switch (surface) {
            case S_EDEN_GRASS: return SkyRegistry.overgrownEdenID;
            case S_PALE_GRASS: return SkyRegistry.palegrassID;
            case S_WEATHERED_STONE: return SkyRegistry.weatheredstoneID;
            case S_CRACKED_MARBLE: return SkyRegistry.crackedmarbleID;
            case S_DEAD_SOIL: return SkyRegistry.deadsoilID;
            case S_ASH_GRASS: return SkyRegistry.ashgrassID;
            case S_MIST_STONE: return SkyRegistry.miststoneID;
            case S_GRAVE_SOIL: return SkyRegistry.gravesoilID;
            default: return SkyRegistry.palegrassID;
        }
    }

    /** Registry object ID for a prop code, or 0. */
    public static int objectIdOf(int prop) {
        switch (prop) {
            case P_WITHERED_TUFT: return SkyRegistry.witheredtuftID;
            case P_PALE_REED: return SkyRegistry.palereedID;
            case P_WIDOW_FLOWER: return SkyRegistry.widowflowerID;
            case P_DEAD_HEAVEN_BLOOM: return SkyRegistry.deadheavenbloomID;
            case P_GHOST_MUSHROOM: return SkyRegistry.ghostmushroomID;
            case P_SPIRIT_MOSS: return SkyRegistry.spiritmosspatchID;
            case P_PALE_STONE_ROCK: return SkyRegistry.palestonerockID;
            case P_GRAVE_SALT_ROCK: return SkyRegistry.gravesaltrockID;
            // The Veil's crooked bare tree, reused whole. The player's own
            // instruction was that the Veil's flora may serve the later
            // regions, and a dead tree is a dead tree.
            case P_DEAD_TREE: return SkyRegistry.deadtreeID;
            case P_GRAVESTONE: return SkyRegistry.steinfeldgravestoneID;
            case P_MOURNER_STATUE: return SkyRegistry.mournerstatueID;
            case P_BROKEN_ANGEL: return SkyRegistry.brokenangelID;
            case P_CHAPEL_COLUMN: return SkyRegistry.chapelcolumnID;
            case P_HEAVEN_SLAB: return SkyRegistry.heavenslabID;
            case P_GRAVE_FENCE: return SkyRegistry.gravefenceID;
            case P_CRATE: return SkyRegistry.skyCrateID;
            default: return 0;
        }
    }

    /** Registry biome ID for a band. */
    public static int biomeIdOf(int band) {
        if (band == BAND_QUIET) {
            return SkyRegistry.quietMeadow.getID();
        }
        return band == BAND_SLAB ? SkyRegistry.slabFields.getID() : SkyRegistry.graveHeath.getID();
    }

    // ---- Painting -----------------------------------------------------------

    /**
     * One tile of the Steinfeld band, packed the way
     * {@link SkyTerrainPainter#pack} packs every tile of the plane.
     *
     * <p><b>The Reach has a coast now, and did not before.</b> As a dimension
     * it had no liquid at all — every tile was buildable ground. One plane has
     * one sea, so Steinfeld gets the lowest waterline of any realm instead
     * ({@code SkyTerrainPainter.REALM_WATERLINE[REALM_STEINFELD]} = 0.34, about
     * four fifths land), and the Mistsea only shows between its far pieces.
     * That is the one thing about this realm the one-plane move genuinely
     * changed; the gradient, the grounds, the props and the POIs are untouched.
     *
     * @param island    the plane's shared island field at this tile
     * @param waterline the plane's blended waterline at this depth
     */
    public static long describeBand(int seed, int tileX, int tileY,
            float island, float waterline, float depth, float distortion) {
        int band = bandFor(localDepth(seed, tileX, tileY, depth));
        int biomeClass = biomeClassOf(band);

        if (island <= waterline) {
            return SkyTerrainPainter.pack(SkyRegistry.mistseaID, 0, biomeClass, false);
        }

        int poiSurface = poiSurface(seed, tileX, tileY);
        int surface = poiSurface >= 0 ? poiSurface : surfaceAt(seed, tileX, tileY);
        int tileID = tileIdOf(surface);

        if (island <= waterline + SkyTerrainPainter.ISLAND_RIM) {
            return SkyTerrainPainter.pack(tileID, 0, biomeClass, false); // walkable coast
        }

        // Inside a POI the building decides; the open country's own scatter is
        // suppressed there, so a chapel is a chapel and not a chapel with
        // shrubs growing through the floor.
        int prop = poiSurface >= 0 ? poiProp(seed, tileX, tileY) : propAt(seed, tileX, tileY);
        return SkyTerrainPainter.pack(tileID, objectIdOf(prop), biomeClass, false);
    }

    /** Band -> the plane's biome class (see {@link SkyTerrainPainter}). */
    public static int biomeClassOf(int band) {
        if (band == BAND_QUIET) {
            return SkyTerrainPainter.BIOME_STEINFELD_QUIET;
        }
        return band == BAND_SLAB
                ? SkyTerrainPainter.BIOME_STEINFELD_SLAB
                : SkyTerrainPainter.BIOME_STEINFELD_HEATH;
    }

    /**
     * Is this tile dry Steinfeld ground? Asked by the preset placer and the
     * pressure field, which have no region in hand.
     */
    public static boolean isLand(int seed, int tileX, int tileY) {
        float depth = RealmDepth.depthAt(tileX, tileY,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed));
        float island = SkyNoise.fbm(seed, tileX, tileY, SkyTerrainPainter.ISLAND_SCALE, 3);
        return island > SkyTerrainPainter.waterlineAt(depth) + SkyTerrainPainter.ISLAND_RIM;
    }

    /** Is this tile inside the Steinfeld band at all? */
    public static boolean isSteinfeld(int seed, int tileX, int tileY) {
        return RealmDepth.realmAt(seed, tileX, tileY,
                SkyOrigin.originX(seed), SkyOrigin.originY(seed)) == RealmDepth.REALM_STEINFELD;
    }
}
