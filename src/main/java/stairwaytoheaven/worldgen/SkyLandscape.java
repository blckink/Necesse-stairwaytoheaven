package stairwaytoheaven.worldgen;

/**
 * The BUILT landscape of the Skyreach: the Skywatch road network, the designed
 * places it connects, and the gates that mark the thresholds between them.
 *
 * <h2>Why this exists</h2>
 *
 * Playtests kept reporting the Skyreach as "zu leer" — not because there were
 * too few objects, but because everything in it had grown rather than been
 * built. {@link SkyTerrainPainter#outcropAt} already fixed that for geology by
 * replacing per-tile probability with a formation field. This class does the
 * same thing one level up: it adds a layer that reads as *authored*, so the
 * player finds roads to follow, places that were laid out by someone, and
 * gates that say "you are arriving somewhere".
 *
 * <h2>The one idea everything here is built on</h2>
 *
 * A lattice of NODES joined by EDGES, evaluated in a smoothly WARPED copy of
 * the world.
 *
 * <ul>
 *   <li>The world is cut into {@link #ROAD_CELL} cells. Each cell holds one
 *       node at a hashed position inside it. The cell containing the canonical
 *       sky origin holds the Old Warden Spire instead, so the hub is a road
 *       junction by construction.</li>
 *   <li>Each node hashes two possible links — east and south — into existence.
 *       Empty links are the point: they turn a grid into a network with
 *       junctions, corners and dead ends. The hub's four links are forced, so
 *       four roads always leave the spire.</li>
 *   <li>A tile is on a road when its WARPED position is within
 *       {@link #ROAD_HALF_WIDTH} of one of those straight segments. The warp is
 *       one smooth low-frequency displacement field applied to the query point
 *       — never to the segments — so the whole network bends together and
 *       junctions stay joined while the roads themselves wander with the land
 *       instead of ruling straight lines across it.</li>
 *   <li>The warp is attenuated to zero near a node, so architecture (courts,
 *       gates, the spire forecourt) sits on crisp world-aligned geometry and
 *       the roads leave it dead straight before they start to bend. Formal at
 *       the monument, organic in the wild.</li>
 * </ul>
 *
 * Everything is a pure function of (worldGenSeed, tileX, tileY, origin) and
 * reads only a 4x4 block of lattice cells, so cost is constant per tile,
 * region borders are seamless, a structure straddling two regions generates
 * identically from either side, and every client in a multiplayer world gets
 * the same world from the server.
 *
 * <h2>What the caller gets</h2>
 *
 * {@link #at} returns a packed int: low byte a SURFACE code (what the ground
 * becomes), second byte a PROP code (what stands on it). The codes are
 * deliberately registry-free — this class never touches
 * {@code SkyRegistry} — so the field can be rendered and calibrated offline,
 * and the mapping from code to tile/object lives in one place in
 * {@link SkyTerrainPainter}.
 */
public final class SkyLandscape {

    private SkyLandscape() {
    }

    // ------------------------------------------------------------------
    // Result codes
    // ------------------------------------------------------------------

    // Surface codes double as their own priority: a tile takes the HIGHEST
    // code anything claiming it asks for.
    //
    // One paving stone builds the whole network — roads, aprons and courts —
    // because it is one civilisation's road system and it should read as one.
    // What separates a court from a road is composition (a railing, a lamp
    // ring, a monument, planting), not a second floor material. The first
    // calibration render tried a distinct court floor and produced a
    // 26-tile-wide grey lake around the spire.

    /** Nothing built here — the natural painter owns this tile. */
    public static final int SURFACE_NONE = 0;
    /** Designed planting: keeps the natural ground, but no wild growth. */
    public static final int SURFACE_GARDEN = 1;
    /** Open paved ground of a designed place. */
    public static final int SURFACE_COURT = 2;
    /** Paved like a road, but allowed to carry road furniture. */
    public static final int SURFACE_APRON = 3;
    /** Decorative chequered inlay: a band or platform inside a court. */
    public static final int SURFACE_INLAY = 4;
    /** The road surface itself. Always kept walkable and prop-free. */
    public static final int SURFACE_ROAD = 5;
    /**
     * The centre of a designed place. Ranks ABOVE the road on purpose: the
     * roads converge on it and stop there, instead of paving straight over the
     * monument they were built to reach.
     */
    public static final int SURFACE_PLINTH = 6;

    public static final int PROP_NONE = 0;
    /** Deliberately empty: a designed place needs its open ground. */
    public static final int PROP_CLEAR = 1;
    public static final int PROP_LAMP = 2;
    public static final int PROP_FENCE = 3;
    /** Gate pillar — a block of the biome's wall material. */
    public static final int PROP_PILLAR = 4;
    public static final int PROP_STATUE = 5;
    public static final int PROP_TREE = 6;
    public static final int PROP_FLOWER = 7;
    public static final int PROP_GRASS = 8;
    /** Small lit crystal accent (biome-specific). */
    public static final int PROP_ACCENT = 9;
    public static final int PROP_RUBBLE = 10;
    /** Observatory instrument: telescope or astrolabe. */
    public static final int PROP_INSTRUMENT = 11;

    public static int surfaceOf(int packed) {
        return packed & 0xFF;
    }

    public static int propOf(int packed) {
        return (packed >> 8) & 0xFF;
    }

    private static int pack(int surface, int prop) {
        return surface | (prop << 8);
    }

    // ------------------------------------------------------------------
    // Tuning. Calibrated at SCREEN scale (roughly 40x22 tiles visible),
    // not on whole-world overviews — see scripts/sky_map_render.sh.
    // ------------------------------------------------------------------

    /** Lattice cell holding one road node. Larger = sparser network. */
    public static final int ROAD_CELL = 72;
    /** Node jitter inside its cell, as a fraction of the cell. */
    public static final float NODE_INSET = 0.24F;
    public static final float NODE_JITTER = 0.52F;
    /** Candidate positions tried per node; the one over land wins. */
    public static final int NODE_CANDIDATES = 5;
    /** Chance that a node links east / south to its neighbour. */
    public static final float ROAD_LINK_CHANCE = 0.58F;

    /** Wavelength and peak displacement of the road warp, in tiles. */
    public static final float ROAD_WARP_SCALE = 110.0F;
    public static final float ROAD_WARP_AMPLITUDE = 26.0F;
    /** Distance past a node's own radius over which the warp fades back in. */
    public static final float WARP_RAMP = 34.0F;

    /** Half width of the paved carriageway: 1.4 gives a 3-tile road. */
    public static final float ROAD_HALF_WIDTH = 1.4F;

    /** Spacing of roadside furniture points along an edge, in tiles. */
    public static final float WAYPOINT_SPACING = 14.0F;
    /** Waypoint kind shares: lamp pair, milestone, roadside bed. */
    public static final float WAYPOINT_LAMP = 0.54F;
    public static final float WAYPOINT_MILESTONE = 0.74F;

    /** Chance a node carries a designed place at all. */
    public static final float STATION_CHANCE = 0.66F;
    public static final int STATION_MIN_RADIUS = 7;
    public static final int STATION_RADIUS_SPAN = 4;

    /** Gate stands this far outside the place it guards. */
    public static final float GATE_OFFSET = 3.5F;
    /** Half depth of a gate along the road: 0.85 gives a 2-tile-deep gate. */
    public static final float GATE_DEPTH = 0.85F;

    /** The Warden's Forecourt: paved apron around the spire. */
    public static final float HUB_INNER_CLEAR = 5.0F;
    public static final float HUB_COURT_RADIUS = 13.0F;
    /** Chequered inlay ring inside the forecourt, so it is not one flat field. */
    public static final float HUB_INLAY_INNER = 8.5F;
    public static final float HUB_INLAY_OUTER = 10.0F;
    /** Radius of the ring of candelabra standing on the forecourt. */
    public static final float HUB_LAMP_RADIUS = 11.0F;
    public static final int HUB_LAMP_COUNT = 6;
    /** No built object may stand closer than this — the spire preset owns it. */
    public static final float HUB_PROP_MIN = 10.5F;

    // Independent noise/hash layers.
    public static final long SALT_ROAD_NODE = 0x5D0A01L;
    public static final long SALT_ROAD_LINK = 0x5D0B17L;
    public static final long SALT_ROAD_WARP_X = 0x5D0C2FL;
    public static final long SALT_ROAD_WARP_Y = 0x5D0C41L;
    public static final long SALT_STATION = 0x5D0D53L;
    public static final long SALT_WAYPOINT = 0x5D0E67L;
    public static final int SALT_BED = 53;
    public static final int SALT_COURT = 59;

    /** Cells cached per call: (cellX-1 .. cellX+2) x (cellY-1 .. cellY+2). */
    private static final int NODE_SPAN_CELLS = 4;
    private static final int NODES = NODE_SPAN_CELLS * NODE_SPAN_CELLS;

    // ------------------------------------------------------------------
    // The field
    // ------------------------------------------------------------------

    /**
     * The built landscape at one tile.
     *
     * @return {@code surface | prop << 8}; {@link #SURFACE_NONE} when nothing
     *         was built here.
     */
    public static int at(int seed, int tileX, int tileY, int originX, int originY) {
        float hubDx = tileX - originX;
        float hubDy = tileY - originY;
        float hubDist = (float) Math.sqrt(hubDx * hubDx + hubDy * hubDy);
        // The spire preset owns its own footprint outright.
        if (hubDist < HUB_INNER_CLEAR) {
            return SURFACE_NONE;
        }

        int cellX = Math.floorDiv(tileX, ROAD_CELL);
        int cellY = Math.floorDiv(tileY, ROAD_CELL);
        int hubCellX = Math.floorDiv(originX, ROAD_CELL);
        int hubCellY = Math.floorDiv(originY, ROAD_CELL);

        // --- node cache: position, designed-place radius, kind, biome ---
        float[] nodeX = new float[NODES];
        float[] nodeY = new float[NODES];
        float[] nodeRadius = new float[NODES];
        int[] nodeKind = new int[NODES];
        int[] nodeBiome = new int[NODES];
        float clearance = Float.MAX_VALUE;

        for (int i = 0; i < NODES; i++) {
            int cx = cellX + (i >> 2) - 1;
            int cy = cellY + (i & 3) - 1;
            boolean isHub = cx == hubCellX && cy == hubCellY;
            float px;
            float py;
            if (isHub) {
                px = originX;
                py = originY;
            } else {
                long packedNode = nodeSite(seed, cx, cy);
                px = (int) (packedNode >> 32);
                py = (int) packedNode;
            }
            nodeX[i] = px;
            nodeY[i] = py;

            if (isHub) {
                // The hub's designed place is the forecourt, laid out below.
                nodeKind[i] = -1;
                nodeRadius[i] = HUB_COURT_RADIUS;
            } else if (SkyNoise.hash(seed + SALT_STATION, cx, cy) < STATION_CHANCE) {
                nodeKind[i] = (int) (SkyNoise.hash(seed + SALT_STATION + 1, cx, cy) * 3.0F);
                nodeRadius[i] = STATION_MIN_RADIUS
                        + (int) (SkyNoise.hash(seed + SALT_STATION + 2, cx, cy) * STATION_RADIUS_SPAN);
            } else {
                nodeKind[i] = -1;
                nodeRadius[i] = 0.0F;
            }
            nodeBiome[i] = biomeClassAt(seed, px, py, originX, originY);

            float dx = tileX - px;
            float dy = tileY - py;
            float d = (float) Math.sqrt(dx * dx + dy * dy) - Math.max(nodeRadius[i], 4.0F);
            if (d < clearance) {
                clearance = d;
            }
        }

        // --- warped query point ---
        // One displacement field for the whole network: bending the QUERY, not
        // the segments, is what keeps junctions joined while the roads curve.
        float atten = clamp01(clearance / WARP_RAMP);
        float amp = ROAD_WARP_AMPLITUDE * atten;
        float wx = tileX;
        float wy = tileY;
        if (amp > 0.0F) {
            wx += (SkyNoise.fbm(seed + SALT_ROAD_WARP_X, tileX, tileY, ROAD_WARP_SCALE, 2) - 0.5F) * amp;
            wy += (SkyNoise.fbm(seed + SALT_ROAD_WARP_Y, tileX, tileY, ROAD_WARP_SCALE, 2) - 0.5F) * amp;
        }

        int surface = SURFACE_NONE;
        int prop = PROP_NONE;

        // --- roads, gates and roadside furniture ---
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                int cx = cellX + ox;
                int cy = cellY + oy;
                int a = nodeIndex(ox, oy);
                for (int dir = 0; dir < 2; dir++) {
                    if (!linkExists(seed, cx, cy, dir, hubCellX, hubCellY)) {
                        continue;
                    }
                    int b = dir == 0 ? nodeIndex(ox + 1, oy) : nodeIndex(ox, oy + 1);
                    int packed = alongEdge(seed, cx, cy, dir, wx, wy,
                            nodeX[a], nodeY[a], nodeRadius[a], nodeBiome[a],
                            nodeX[b], nodeY[b], nodeRadius[b], nodeBiome[b],
                            tileX, tileY);
                    if (packed != 0) {
                        surface = Math.max(surface, surfaceOf(packed));
                        if (propOf(packed) != PROP_NONE) {
                            prop = propOf(packed);
                        }
                    }
                }
            }
        }

        // --- designed places at the nodes ---
        for (int i = 0; i < NODES; i++) {
            if (nodeKind[i] < 0) {
                continue;
            }
            int packed = station(seed, nodeKind[i], (int) nodeRadius[i],
                    tileX - Math.round(nodeX[i]), tileY - Math.round(nodeY[i]), tileX, tileY);
            if (packed != 0) {
                surface = Math.max(surface, surfaceOf(packed));
                if (propOf(packed) != PROP_NONE && prop == PROP_NONE) {
                    prop = propOf(packed);
                }
            }
        }

        // --- the Warden's Forecourt ---
        if (hubDist <= HUB_COURT_RADIUS + 0.5F) {
            int hubSurface;
            if (hubDist > HUB_COURT_RADIUS - 0.5F) {
                hubSurface = SURFACE_APRON;                  // the railing line
            } else if (hubDist >= HUB_INLAY_INNER && hubDist <= HUB_INLAY_OUTER) {
                hubSurface = SURFACE_INLAY;                  // chequered ring
            } else {
                hubSurface = SURFACE_COURT;
            }
            surface = Math.max(surface, hubSurface);
            if (prop == PROP_NONE) {
                if (hubDist > HUB_COURT_RADIUS - 0.5F) {
                    prop = PROP_FENCE;                       // the forecourt wall
                } else if (isHubLamp(tileX, tileY, originX, originY)) {
                    prop = PROP_LAMP;
                } else {
                    prop = PROP_CLEAR;
                }
            }
        }

        if (surface == SURFACE_NONE) {
            return SURFACE_NONE;
        }
        // The carriageway stays walkable, always. This is also what opens every
        // fence ring exactly where a road runs through it, for free.
        if (surface == SURFACE_ROAD) {
            prop = PROP_NONE;
        }
        // Never build inside the spire preset's reach.
        if (hubDist < HUB_PROP_MIN && prop != PROP_CLEAR) {
            prop = PROP_NONE;
        }
        return pack(surface, prop);
    }

    // ------------------------------------------------------------------
    // Lattice helpers
    // ------------------------------------------------------------------

    /**
     * The designed place nearest to (fromX, fromY), searching outward through
     * the lattice, or null if there is none within {@code cellRadius} cells.
     *
     * Used by the offline map renderer to frame a screen on a real waystation
     * instead of a random patch of sky, and by /skyreachstatus to assert that
     * the world the server generated actually contains the one the field
     * predicts. Not on any generation hot path.
     *
     * @return {@code {tileX, tileY, kind, radius}}
     */
    public static int[] designedPlaceNear(int seed, int fromX, int fromY,
                                          int originX, int originY, int cellRadius) {
        int cellX = Math.floorDiv(fromX, ROAD_CELL);
        int cellY = Math.floorDiv(fromY, ROAD_CELL);
        int hubCellX = Math.floorDiv(originX, ROAD_CELL);
        int hubCellY = Math.floorDiv(originY, ROAD_CELL);
        int[] best = null;
        long bestDist = Long.MAX_VALUE;
        for (int oy = -cellRadius; oy <= cellRadius; oy++) {
            for (int ox = -cellRadius; ox <= cellRadius; ox++) {
                int cx = cellX + ox;
                int cy = cellY + oy;
                if (cx == hubCellX && cy == hubCellY) {
                    continue;                       // the hub has the forecourt
                }
                if (SkyNoise.hash(seed + SALT_STATION, cx, cy) >= STATION_CHANCE) {
                    continue;
                }
                int kind = (int) (SkyNoise.hash(seed + SALT_STATION + 1, cx, cy) * 3.0F);
                int radius = STATION_MIN_RADIUS
                        + (int) (SkyNoise.hash(seed + SALT_STATION + 2, cx, cy) * STATION_RADIUS_SPAN);
                long packedNode = nodeSite(seed, cx, cy);
                int px = (int) (packedNode >> 32);
                int py = (int) packedNode;
                long dx = px - fromX;
                long dy = py - fromY;
                long d = dx * dx + dy * dy;
                if (d < bestDist) {
                    bestDist = d;
                    best = new int[]{px, py, kind, radius};
                }
            }
        }
        return best;
    }

    /**
     * Where the node of lattice cell (cx, cy) stands, packed as
     * {@code x << 32 | y}.
     *
     * {@link #NODE_CANDIDATES} hashed positions are tried inside the cell and
     * the one over the strongest land signal wins. Without this, roughly a
     * third of all nodes drown: the sky is 38% Mistsea, and a waystation whose
     * node fell in the sea is a whole set piece the player can never find,
     * with roads that walk into the water at both ends.
     *
     * Five candidates against the real island mask sits on the ceiling this can
     * reach: measured over three seeds, ~70% of lattice cells contain a patch
     * of land big enough to hold a designed place at all, and this finds one in
     * ~66% of them. Spending more candidates buys nothing; the remaining
     * misses are cells that are simply open sky.
     */
    private static long nodeSite(int seed, int cx, int cy) {
        int bestX = 0;
        int bestY = 0;
        float bestLand = -1.0F;
        for (int c = 0; c < NODE_CANDIDATES; c++) {
            float px = cx * ROAD_CELL
                    + (NODE_INSET + SkyNoise.hash(seed + SALT_ROAD_NODE + c * 2L, cx, cy) * NODE_JITTER) * ROAD_CELL;
            float py = cy * ROAD_CELL
                    + (NODE_INSET + SkyNoise.hash(seed + SALT_ROAD_NODE + c * 2L + 1L, cx, cy) * NODE_JITTER) * ROAD_CELL;
            float land = SkyNoise.fbm(seed, px, py, SkyTerrainPainter.ISLAND_SCALE, 3);
            if (land > bestLand) {
                bestLand = land;
                bestX = Math.round(px);
                bestY = Math.round(py);
            }
        }
        return ((long) bestX << 32) | (bestY & 0xFFFFFFFFL);
    }

    private static int nodeIndex(int ox, int oy) {
        return ((ox + 1) << 2) | (oy + 1);
    }

    /**
     * Does the node in cell (cx, cy) link east (dir 0) or south (dir 1)?
     * The four links touching the hub cell are forced, so the spire always has
     * four roads leaving it.
     */
    private static boolean linkExists(int seed, int cx, int cy, int dir, int hubCellX, int hubCellY) {
        if (cx == hubCellX && cy == hubCellY) {
            return true;
        }
        if (dir == 0 ? (cx + 1 == hubCellX && cy == hubCellY) : (cx == hubCellX && cy + 1 == hubCellY)) {
            return true;
        }
        return SkyNoise.hash(seed + SALT_ROAD_LINK + dir, cx, cy) < ROAD_LINK_CHANCE;
    }

    /** Sub-biome of a world position, with the same hub pull the painter applies. */
    private static int biomeClassAt(int seed, float x, float y, int originX, int originY) {
        float b = SkyNoise.fbm(seed + SkyTerrainPainter.SALT_BIOME, x, y, SkyTerrainPainter.BIOME_SCALE, 2);
        float dx = x - originX;
        float dy = y - originY;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < SkyOrigin.HUB_RADIUS) {
            float force = 1.0F - d / SkyOrigin.HUB_RADIUS;
            b = b + (0.5F - b) * Math.min(1.0F, force * 1.6F);
        }
        if (b < SkyTerrainPainter.STORMVEIL_BELOW) {
            return 0;
        }
        return b > SkyTerrainPainter.AURORA_ABOVE ? 2 : 1;
    }

    private static float clamp01(float v) {
        if (v <= 0.0F) {
            return 0.0F;
        }
        if (v >= 1.0F) {
            return 1.0F;
        }
        return v * v * (3.0F - 2.0F * v);
    }

    // ------------------------------------------------------------------
    // One road edge: carriageway, gates, roadside furniture
    // ------------------------------------------------------------------

    private static int alongEdge(int seed, int cx, int cy, int dir, float wx, float wy,
                                 float ax, float ay, float ar, int abiome,
                                 float bx, float by, float br, int bbiome,
                                 int tileX, int tileY) {
        float ex = bx - ax;
        float ey = by - ay;
        float len2 = ex * ex + ey * ey;
        if (len2 < 1.0F) {
            return 0;
        }
        float len = (float) Math.sqrt(len2);
        float t = ((wx - ax) * ex + (wy - ay) * ey) / len2;
        if (t < 0.0F) {
            t = 0.0F;
        } else if (t > 1.0F) {
            t = 1.0F;
        }
        float px = ax + ex * t;
        float py = ay + ey * t;
        float ddx = wx - px;
        float ddy = wy - py;
        float perp = (float) Math.sqrt(ddx * ddx + ddy * ddy);
        if (perp > ROAD_HALF_WIDTH + 7.0F) {
            return 0;                                // nothing this edge owns
        }
        float s = t * len;
        // Signed side, so roadside pockets can pick one bank of the road.
        float side = (ex * (wy - ay) - ey * (wx - ax)) >= 0.0F ? 1.0F : -1.0F;

        // --- gates: at the approach to a designed place, and where the road
        // --- crosses from one sub-biome into another.
        int gate = gateAt(s, len, ar, br, abiome != bbiome);
        if (gate != 0) {
            if (perp <= ROAD_HALF_WIDTH) {
                return pack(SURFACE_ROAD, PROP_NONE);            // the opening
            }
            if (perp <= ROAD_HALF_WIDTH + 2.0F) {
                return pack(SURFACE_APRON, PROP_PILLAR);
            }
            if (perp <= ROAD_HALF_WIDTH + 3.2F) {
                return pack(SURFACE_APRON, PROP_LAMP);
            }
            if (perp <= ROAD_HALF_WIDTH + 5.4F) {
                return pack(SURFACE_APRON, PROP_FENCE);
            }
            return 0;
        }

        if (perp <= ROAD_HALF_WIDTH) {
            return pack(SURFACE_ROAD, PROP_NONE);
        }

        // --- roadside furniture at evenly spaced waypoints ---
        // Kept well clear of both ends: a designed place has its own lamps and
        // railings, and roadside furniture crowding into it was the first
        // calibration render's worst failure.
        if (s < Math.max(ar, 4.0F) + 8.0F || s > len - (Math.max(br, 4.0F) + 8.0F)) {
            return 0;
        }
        int steps = Math.round(len / WAYPOINT_SPACING);
        if (steps < 2) {
            return 0;
        }
        float step = len / steps;
        int k = Math.round(s / step);
        if (k <= 0 || k >= steps) {
            return 0;                                 // the ends belong to the nodes
        }
        float along = s - k * step;
        float kind = SkyNoise.hash(seed + SALT_WAYPOINT + dir * 3L, cx * 131 + k, cy);
        float pickSide = SkyNoise.hash(seed + SALT_WAYPOINT + 1 + dir * 3L, cx * 131 + k, cy) < 0.5F ? 1.0F : -1.0F;

        if (kind < WAYPOINT_LAMP) {
            // A lit pair flanking the road. Roads are findable at night.
            if (Math.abs(along) <= 0.6F && isVergeTile(perp)) {
                return pack(SURFACE_APRON, PROP_LAMP);
            }
            return 0;
        }
        if (kind < WAYPOINT_MILESTONE) {
            // A waystone: a lamp and a heap of Skywatch rubble on one bank.
            if (side != pickSide) {
                return 0;
            }
            if (Math.abs(along) <= 0.6F && isVergeTile(perp)) {
                return pack(SURFACE_APRON, PROP_LAMP);
            }
            if (Math.abs(along) <= 1.6F && perp > ROAD_HALF_WIDTH + 1.5F && perp <= ROAD_HALF_WIDTH + 2.6F) {
                return pack(SURFACE_APRON, PROP_RUBBLE);
            }
            return 0;
        }
        // A tended bed beside the road, fenced on three sides.
        if (side != pickSide || Math.abs(along) > 2.7F) {
            return 0;
        }
        if (perp <= ROAD_HALF_WIDTH + 0.6F) {
            return 0;
        }
        if (perp > ROAD_HALF_WIDTH + 4.4F) {
            return 0;
        }
        if (perp <= ROAD_HALF_WIDTH + 1.5F) {
            return pack(SURFACE_APRON, PROP_CLEAR);              // the verge
        }
        if (Math.abs(along) > 1.8F || perp > ROAD_HALF_WIDTH + 3.5F) {
            return pack(SURFACE_APRON, PROP_FENCE);
        }
        float fill = SkyNoise.tileRoll(seed, tileX, tileY, SALT_BED);
        if (fill < 0.52F) {
            return pack(SURFACE_GARDEN, PROP_FLOWER);
        }
        if (fill < 0.70F) {
            return pack(SURFACE_GARDEN, PROP_GRASS);
        }
        return pack(SURFACE_GARDEN, PROP_CLEAR);
    }

    /**
     * The single tile-ring immediately outside the carriageway.
     *
     * Narrow on purpose: a wider band catches TWO rings on a diagonal road
     * (perpendicular distances there step by ~0.71, not 1), and the calibration
     * render showed every lamp pair coming out as a clump of four.
     */
    private static boolean isVergeTile(float perp) {
        return perp > ROAD_HALF_WIDTH + 0.3F && perp <= ROAD_HALF_WIDTH + 1.15F;
    }

    /**
     * Is arc position {@code s} inside a gate on an edge of length {@code len}?
     * Gates stand just outside whatever they guard, and one more stands where
     * the road crosses a sub-biome border.
     */
    private static int gateAt(float s, float len, float startRadius, float endRadius, boolean biomeChange) {
        if (startRadius > 0.0F) {
            float g = startRadius + GATE_OFFSET;
            if (g < len * 0.4F && Math.abs(s - g) <= GATE_DEPTH) {
                return 1;
            }
        }
        if (endRadius > 0.0F) {
            float g = len - (endRadius + GATE_OFFSET);
            if (g > len * 0.6F && Math.abs(s - g) <= GATE_DEPTH) {
                return 1;
            }
        }
        if (biomeChange && Math.abs(s - len * 0.5F) <= GATE_DEPTH) {
            return 1;
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Designed places
    // ------------------------------------------------------------------

    /**
     * The three designed places, laid out in world-aligned tile geometry
     * (the warp is attenuated to nothing here, so these stay crisp):
     *
     * <ul>
     *   <li>0 — <b>Garden Court</b>: a round fenced plot, four paved spokes, a
     *       statue on a marble plinth, quartered flower beds, corner trees.</li>
     *   <li>1 — <b>Waystation Square</b>: a paved square inside a railing, four
     *       corner lamps, an observatory instrument at the centre, weathered
     *       rubble and lit accents scattered over the flags.</li>
     *   <li>2 — <b>Overlook Terrace</b>: a stepped rectangle — planted apron,
     *       raised inner platform, railing all round, an instrument and lamps
     *       looking out over the edge.</li>
     * </ul>
     *
     * @param dx tile position relative to the (integer-snapped) node
     */
    private static int station(int seed, int kind, int radius, int dx, int dy, int tileX, int tileY) {
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int mx = Math.max(ax, ay);
        int mn = Math.min(ax, ay);
        if (kind == 0) {
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > radius + 0.5F) {
                return 0;
            }
            if (mx <= 1) {
                return dx == 0 && dy == 0
                        ? pack(SURFACE_PLINTH, PROP_STATUE)
                        : pack(SURFACE_PLINTH, PROP_CLEAR);
            }
            if (mn <= 1) {
                return pack(SURFACE_ROAD, PROP_NONE);            // four spokes
            }
            if (d > radius - 0.5F) {
                return pack(SURFACE_GARDEN, PROP_FENCE);
            }
            if (ax == ay && ax == radius - 3) {
                return pack(SURFACE_GARDEN, PROP_TREE);
            }
            if (mn == 2 && mx == radius - 2) {
                return pack(SURFACE_APRON, PROP_LAMP);           // spoke mouths
            }
            float fill = SkyNoise.tileRoll(seed, tileX, tileY, SALT_BED);
            if (fill < 0.42F) {
                return pack(SURFACE_GARDEN, PROP_FLOWER);
            }
            if (fill < 0.62F) {
                return pack(SURFACE_GARDEN, PROP_GRASS);
            }
            return pack(SURFACE_GARDEN, PROP_CLEAR);
        }
        if (kind == 1) {
            if (mx > radius) {
                return 0;
            }
            if (mx <= 1) {
                return dx == 0 && dy == 0
                        ? pack(SURFACE_PLINTH, PROP_INSTRUMENT)
                        : pack(SURFACE_PLINTH, PROP_CLEAR);
            }
            if (mx == radius) {
                return pack(SURFACE_APRON, PROP_FENCE);          // railing
            }
            if (mn <= 1) {
                return pack(SURFACE_ROAD, PROP_NONE);
            }
            if (ax == ay && ax == radius - 2) {
                return pack(SURFACE_APRON, PROP_LAMP);
            }
            int field = mx <= radius - 4 ? SURFACE_INLAY : SURFACE_COURT;
            float fill = SkyNoise.tileRoll(seed, tileX, tileY, SALT_COURT);
            if (fill < 0.055F) {
                return pack(field, PROP_RUBBLE);
            }
            if (fill < 0.095F) {
                return pack(field, PROP_ACCENT);
            }
            return pack(field, PROP_CLEAR);
        }
        // kind 2 — Overlook Terrace
        int w = radius;
        int h = radius - 2;
        if (ax > w || ay > h) {
            return 0;
        }
        if (ax == w || ay == h) {
            return pack(SURFACE_APRON, PROP_FENCE);              // railing
        }
        if (ax <= w - 3 && ay <= h - 2) {
            if (ax <= 1 && ay <= 1) {
                return dx == 0 && dy == 0
                        ? pack(SURFACE_PLINTH, PROP_INSTRUMENT)
                        : pack(SURFACE_PLINTH, PROP_CLEAR);
            }
            if (ay == 0 && ax == w - 4) {
                return pack(SURFACE_APRON, PROP_LAMP);
            }
            if (ax == w - 5 && ay == h - 3) {
                return pack(SURFACE_COURT, PROP_ACCENT);
            }
            int field = ay <= h - 4 ? SURFACE_INLAY : SURFACE_COURT;
            float fill = SkyNoise.tileRoll(seed, tileX, tileY, SALT_COURT);
            if (fill < 0.05F) {
                return pack(field, PROP_RUBBLE);
            }
            return pack(field, PROP_CLEAR);
        }
        if (ay == 0) {
            return pack(SURFACE_ROAD, PROP_NONE);                // the way in
        }
        float fill = SkyNoise.tileRoll(seed, tileX, tileY, SALT_BED);
        if (fill < 0.34F) {
            return pack(SURFACE_GARDEN, PROP_FLOWER);
        }
        if (fill < 0.48F) {
            return pack(SURFACE_GARDEN, PROP_GRASS);
        }
        return pack(SURFACE_GARDEN, PROP_CLEAR);
    }

    /** One of the candelabra standing in a ring on the Warden's Forecourt. */
    private static boolean isHubLamp(int tileX, int tileY, int originX, int originY) {
        for (int k = 0; k < HUB_LAMP_COUNT; k++) {
            double angle = k * (2.0 * Math.PI / HUB_LAMP_COUNT) + Math.PI / HUB_LAMP_COUNT;
            int lx = originX + (int) Math.round(Math.cos(angle) * HUB_LAMP_RADIUS);
            int ly = originY + (int) Math.round(Math.sin(angle) * HUB_LAMP_RADIUS);
            if (tileX == lx && tileY == ly) {
                return true;
            }
        }
        return false;
    }
}
