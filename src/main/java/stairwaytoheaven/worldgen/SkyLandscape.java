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
    /**
     * A small basin of open cloud sea at the heart of a designed place: the
     * Skyreach's own well. Highest of all, so nothing paves over it.
     */
    public static final int SURFACE_POOL = 7;

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
    /**
     * The fence GATE, standing in the opening a road makes in a fence ring.
     * An enclosure whose entrance is a gap reads as a broken fence; the same
     * enclosure with a gate in the gap reads as a place with a way in.
     */
    public static final int PROP_GATE = 12;
    public static final int PROP_RUBBLE = 10;
    /** Observatory instrument: telescope or astrolabe. */
    public static final int PROP_INSTRUMENT = 11;
    // PROP_GATE = 12, declared with the fence above.

    // ------------------------------------------------------------------
    // The Skyway Passages (v0.8).
    //
    // A road whose two ends both stand in the Skyway is not a road, it is a
    // PASSAGE: a causeway with a balustrade down both sides, buttressed where
    // it passes a gate, and marked at intervals by a Sky Seraph on a plinth.
    // These four codes are what makes it one, and they resolve to Cloudmarble
    // unconditionally rather than per tile — a passage that runs on past the
    // biome border keeps its own material instead of changing halfway.
    // ------------------------------------------------------------------

    /** Cloudmarble balustrade running the length of a passage. */
    public static final int PROP_RAIL = 13;
    /** The balustrade's gate, standing where a carriageway breaks it. */
    public static final int PROP_RAIL_GATE = 14;
    /** Cloudmarble block: the pier of a passage gate. */
    public static final int PROP_BUTTRESS = 15;
    /** A Sky Seraph on the verge, marking a stage along the passage. */
    public static final int PROP_MONUMENT = 16;
    /** A fruiting bush in an orchard row. */
    public static final int PROP_BUSH = 17;
    /** A crate standing in a market square. */
    public static final int PROP_CRATE = 18;

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

    /**
     * Outer edge of the passage balustrade, measured from the road centre.
     *
     * It starts where {@link #isVergeTile} starts and is exactly
     * {@link #FENCE_MIN_THICKNESS} deep, which is not a round number chosen
     * for looks: a fence attaches to its four ORTHOGONAL neighbours only, and
     * a band thinner than 1.6 tiles rasterises into a diagonal staircase of
     * unconnected posts on any road that is not axis-aligned. A balustrade
     * that is a row of loose posts is not a balustrade.
     */
    public static final float RAIL_OUTER =
            ROAD_HALF_WIDTH + 0.3F + SkyLandscape.FENCE_MIN_THICKNESS;

    /** Spacing of roadside furniture points along an edge, in tiles. */
    public static final float WAYPOINT_SPACING = 14.0F;
    /** Waypoint kind shares: lamp pair, milestone, roadside bed. */
    public static final float WAYPOINT_LAMP = 0.54F;
    public static final float WAYPOINT_MILESTONE = 0.74F;

    /**
     * Chance a node carries a designed place at all.
     *
     * <p>Was 0.66, i.e. two junctions in three were a plaza. The player, after
     * walking the Skyreach (2026-09-23): "zu oft wiederholte Anordnungen wie
     * diese Plätze bei denen in der Mitte immer random Deko Objekt steht das man
     * dann viel zu oft findet". A place stops being a place once the next one is
     * over the next hill; most junctions are now just junctions.
     */
    public static final float STATION_CHANCE = 0.42F;
    public static final int STATION_MIN_RADIUS = 7;
    public static final int STATION_RADIUS_SPAN = 4;
    /** How many designed-place kinds {@link #station} knows. */
    public static final int STATION_KINDS = 4;

    /**
     * Straight paving laid outward from a used entrance before the road is
     * allowed to turn toward its neighbour. Without it a road leaving at an
     * angle clips the railing beside its own gateway.
     */
    public static final int SPOKE_STUB = 3;

    /** Half depth of a border gateway along the road: 0.85 gives a 2-tile-deep gate. */
    public static final float GATE_DEPTH = 0.85F;
    /**
     * A border gateway stands only on a road at least this long, and only this
     * far from a designed place's entrance. Short edges between two plazas were
     * where three gateways used to stack up inside twenty tiles.
     */
    public static final float BORDER_GATE_MIN_LENGTH = 44.0F;
    public static final float BORDER_GATE_CLEARANCE = 18.0F;

    /** Link bits: which of a node's four roads exist. +x, +y, -x, -y. */
    public static final int LINK_E = 1;
    public static final int LINK_S = 2;
    public static final int LINK_W = 4;
    public static final int LINK_N = 8;

    /** The Warden's Forecourt: paved apron around the spire. */
    public static final float HUB_INNER_CLEAR = 5.0F;
    /**
     * All four rings below moved outward when the spire became the 33x33
     * cathedral: its arms now reach 14 tiles from the origin and its apron
     * two further, so a ring at the old radius would have been drawn UNDER
     * the building. Read them against {@code WardenSpirePreset.WRITTEN_RADIUS}
     * (15) — the inlay is the first thing outside the preset's own box.
     */
    public static final float HUB_COURT_RADIUS = 21.0F;
    /** Chequered inlay ring inside the forecourt, so it is not one flat field. */
    public static final float HUB_INLAY_INNER = 16.5F;
    public static final float HUB_INLAY_OUTER = 18.0F;
    /** Radius of the ring of candelabra standing on the forecourt. */
    public static final float HUB_LAMP_RADIUS = 19.0F;
    public static final int HUB_LAMP_COUNT = 6;
    /**
     * How far to either side of a door axis nothing from the forecourt
     * furniture may stand. The Spire's four doors are on the axes through
     * the origin, and a streetlamp is 86px of opaque sprite: one standing
     * even a tile off the line still reads as blocking the entrance.
     */
    public static final int DOOR_AXIS_CLEARANCE = 1;
    /** No built object may stand closer than this — the spire preset owns it. */
    public static final float HUB_PROP_MIN = 16.5F;

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
        int[] nodeLinks = new int[NODES];
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
                nodeKind[i] = stationKind(seed, cx, cy);
                nodeRadius[i] = STATION_MIN_RADIUS
                        + (int) (SkyNoise.hash(seed + SALT_STATION + 2, cx, cy) * STATION_RADIUS_SPAN);
            } else {
                nodeKind[i] = -1;
                nodeRadius[i] = 0.0F;
            }
            nodeBiome[i] = biomeClassAt(seed, px, py, originX, originY);
            nodeLinks[i] = links(seed, cx, cy, hubCellX, hubCellY);

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
                    // The road runs between the two ENTRANCES it uses, not
                    // between the two centres. A road aimed at a plaza's
                    // centre crossed its railing wherever the angle put it --
                    // five to seven fence gates abreast on a diagonal -- and
                    // then a second gateway stood 3.5 tiles further out on
                    // the same road. Now each place is entered through the
                    // one gate on the side the road comes from.
                    float reachA = mouthReach(nodeKind[a], nodeRadius[a], dir == 0 ? LINK_E : LINK_S);
                    float reachB = mouthReach(nodeKind[b], nodeRadius[b], dir == 0 ? LINK_W : LINK_N);
                    float sx = nodeX[a] + (dir == 0 ? reachA : 0.0F);
                    float sy = nodeY[a] + (dir == 0 ? 0.0F : reachA);
                    float bx = nodeX[b] - (dir == 0 ? reachB : 0.0F);
                    float by = nodeY[b] - (dir == 0 ? 0.0F : reachB);
                    int packed = alongEdge(seed, cx, cy, dir, wx, wy,
                            sx, sy, nodeBiome[a], bx, by, nodeBiome[b],
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
            int cx = cellX + (i >> 2) - 1;
            int cy = cellY + (i & 3) - 1;
            int packed = station(seed, nodeKind[i], (int) nodeRadius[i], nodeLinks[i],
                    SkyNoise.hash(seed + SALT_STATION + 3, cx, cy),
                    tileX - Math.round(nodeX[i]), tileY - Math.round(nodeY[i]), tileX, tileY);
            if (packed != 0) {
                surface = Math.max(surface, surfaceOf(packed));
                if (propOf(packed) != PROP_NONE && prop == PROP_NONE) {
                    prop = propOf(packed);
                }
            }
        }

        // --- the Warden's Forecourt ---
        int hubEntrance = entrance((int) hubDx, (int) hubDy, Math.round(HUB_COURT_RADIUS),
                LINK_E | LINK_S | LINK_W | LINK_N);
        if (hubEntrance != 0) {
            // The four roads meet the forecourt wall on the spire's own door
            // axes, through one framed gateway each.
            surface = Math.max(surface, surfaceOf(hubEntrance));
            prop = propOf(hubEntrance);
        } else if (hubDist <= HUB_COURT_RADIUS + 0.5F) {
            boolean onRail = discRing((int) hubDx, (int) hubDy, HUB_COURT_RADIUS);
            int hubSurface;
            if (onRail) {
                hubSurface = SURFACE_APRON;                  // the railing line
            } else if (hubDist >= HUB_INLAY_INNER && hubDist <= HUB_INLAY_OUTER) {
                hubSurface = SURFACE_INLAY;                  // chequered ring
            } else {
                hubSurface = SURFACE_COURT;
            }
            surface = Math.max(surface, hubSurface);
            if (prop == PROP_NONE) {
                if (onRail) {
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
        // The carriageway stays walkable, always. The only gates are the ones a
        // designed place puts in its own entrances (see #entrance); a fence or
        // balustrade that some OTHER road happens to cross is simply opened.
        // Turning every such crossing into a gate is what stood gates across
        // junctions, beside gateways and in rows along diagonal roads.
        if (surface == SURFACE_ROAD && prop != PROP_GATE) {
            prop = PROP_NONE;
        }
        // Never build inside the spire preset's reach.
        if (hubDist < HUB_PROP_MIN && prop != PROP_CLEAR) {
            prop = PROP_NONE;
        }
        return pack(surface, prop);
    }

    // ------------------------------------------------------------------
    // Fence geometry
    // ------------------------------------------------------------------

    /**
     * The minimum thickness, in tiles, of a STRAIGHT fence band that still
     * comes out as one connected run at every angle.
     *
     * {@link necesse.level.gameObject.FenceObject} attaches to its four
     * ORTHOGONAL neighbours only, so a fence band thin enough to step
     * diagonally on the tile grid is not a fence at all — it is a row of
     * unconnected posts, and that is exactly what the player saw. Rasterising
     * a band at every angle from 0 to 90 degrees: at 0.9 and 1.4 tiles thick
     * the band falls into fragments (the largest holds 2–3% of its tiles); at
     * 1.6 it is one component at every angle, with no lone posts anywhere in
     * the sweep. Any road-side fence must therefore be at least this thick.
     */
    public static final float FENCE_MIN_THICKNESS = 1.6F;

    /**
     * Is (dx, dy) on the fence ring of a disc of the given radius?
     *
     * Taking the ring as the annulus |d - r| &lt;= 0.5 is one tile thick, which
     * looks right written down and is wrong here for the reason above: a thin
     * digital circle steps diagonally near its 45-degree points. Measured over
     * radii 7..20, that rule leaves 60–70% of the ring as lone posts and dead
     * ends.
     *
     * The 8-neighbour inner boundary — inside the disc, with at least one of
     * the eight neighbours outside — is a single closed loop in which EVERY
     * tile has exactly two orthogonal neighbours, at every radius tested
     * (7, 8, 9, 10, 11, 13, 20: degrees 2/2/2/2/2/2/2, no exceptions). It runs
     * two tiles wide across the diagonals, and that second tile IS the fix.
     */
    public static boolean discRing(int dx, int dy, float radius) {
        float r2 = (radius + 0.5F) * (radius + 0.5F);
        if (dx * dx + dy * dy > r2) {
            return false;
        }
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                int nx = dx + ox;
                int ny = dy + oy;
                if (nx * nx + ny * ny > r2) {
                    return true;
                }
            }
        }
        return false;
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
                int kind = stationKind(seed, cx, cy);
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

    /**
     * Sub-biome of a world position, with the same hub pull the painter
     * applies. The banding itself is deliberately NOT duplicated here: it is
     * {@link SkyTerrainPainter#biomeClassOf}, so a new sub-biome cannot be
     * added to the painter and silently forgotten by the road network — which
     * would leave the gates that mark a biome crossing standing at the wrong
     * borders.
     */
    private static int biomeClassAt(int seed, float x, float y, int originX, int originY) {
        float b = SkyNoise.fbm(seed + SkyTerrainPainter.SALT_BIOME, x, y, SkyTerrainPainter.BIOME_SCALE, 2);
        float dx = x - originX;
        float dy = y - originY;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < SkyOrigin.HUB_RADIUS) {
            float force = 1.0F - d / SkyOrigin.HUB_RADIUS;
            b = b + (0.5F - b) * Math.min(1.0F, force * 1.6F);
        }
        return SkyTerrainPainter.biomeClassOf(b);
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

    /**
     * One road between two points: the entrances it uses at a designed place,
     * or the node itself at a plain junction.
     */
    private static int alongEdge(int seed, int cx, int cy, int dir, float wx, float wy,
                                 float ax, float ay, int abiome,
                                 float bx, float by, int bbiome,
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

        // A PASSAGE is an edge whose BOTH ends stand in the Skyway. Deciding
        // it from the two node biomes rather than per tile is what keeps one
        // causeway a single object: it carries the same balustrade from end to
        // end, including across the stretch where the ground underneath it
        // happens to be something else.
        boolean passage = abiome == SkyTerrainPainter.BIOME_SKYWAY
                && bbiome == SkyTerrainPainter.BIOME_SKYWAY;

        // --- the border gateway: where the road crosses from one sub-biome
        // --- into another, and nowhere else. The approach gateway that used
        // --- to stand 3.5 tiles outside every plaza is gone: the plaza's own
        // --- entrance IS its gate, and two in a row read as a mistake.
        if (abiome != bbiome && isBorderGate(s, len)) {
            // A gate is: opening, pillar, WING, lit end — in that order,
            // outward from the road. The wing has to START at the pillar,
            // because a fence attaches to a wall: the old layout put the lamp
            // band between them, so the wing stood detached in open ground as
            // a 2x2 patch of loose posts rather than a run leaving the gate.
            if (perp <= ROAD_HALF_WIDTH) {
                return pack(SURFACE_ROAD, PROP_NONE);            // the opening
            }
            if (perp <= ROAD_HALF_WIDTH + 1.6F) {
                return pack(SURFACE_APRON, passage ? PROP_BUTTRESS : PROP_PILLAR);
            }
            if (perp <= ROAD_HALF_WIDTH + 4.8F) {
                return pack(SURFACE_APRON, passage ? PROP_RAIL : PROP_FENCE);   // the wing
            }
            if (perp <= ROAD_HALF_WIDTH + 5.5F) {
                // 0.7 tiles: one ring deep. A 1.2-wide band caught two rings
                // on a slanted road and stood the lamps in stacks, the same
                // failure isVergeTile was already narrowed to avoid.
                return pack(SURFACE_APRON, PROP_LAMP);           // its lit end
            }
            return 0;
        }

        if (perp <= ROAD_HALF_WIDTH) {
            return pack(SURFACE_ROAD, PROP_NONE);
        }

        // --- roadside furniture at evenly spaced waypoints ---
        // Kept well clear of both ends: a designed place has its own lamps and
        // railings, and roadside furniture crowding into it was the first
        // calibration render's worst failure. The balustrade observes the same
        // clearance, so it stops short of a court instead of running into it.
        if (s < 12.0F || s > len - 12.0F) {
            return 0;
        }
        int steps = Math.round(len / WAYPOINT_SPACING);
        if (steps < 2) {
            return rail(passage, perp);
        }
        float step = len / steps;
        int k = Math.round(s / step);
        if (k <= 0 || k >= steps) {
            return rail(passage, perp);                // the ends belong to the nodes
        }
        float along = s - k * step;
        float kind = SkyNoise.hash(seed + SALT_WAYPOINT + dir * 3L, cx * 131 + k, cy);
        float pickSide = SkyNoise.hash(seed + SALT_WAYPOINT + 1 + dir * 3L, cx * 131 + k, cy) < 0.5F ? 1.0F : -1.0F;

        if (kind < WAYPOINT_LAMP) {
            // A lit pair flanking the road. Roads are findable at night.
            if (Math.abs(along) <= 0.6F && isVergeTile(perp)) {
                return pack(SURFACE_APRON, PROP_LAMP);
            }
            return rail(passage, perp);
        }
        if (kind < WAYPOINT_MILESTONE) {
            // A waystone: a lamp and a heap of Skywatch rubble on one bank.
            //
            // On a passage the lamp becomes a SERAPH instead: the milestones
            // are 14 tiles apart and one waypoint in five is a milestone, so
            // this is one statue roughly every 70 tiles of causeway — a stage
            // you walk between, not a colonnade. Statues stand on ONE bank
            // (the milestone already picks a side), which is what keeps a
            // 3-tile-wide sprite from meeting its opposite number across a
            // 3-tile road.
            if (side != pickSide) {
                return rail(passage, perp);
            }
            if (Math.abs(along) <= 0.6F && isVergeTile(perp)) {
                return pack(SURFACE_APRON, passage ? PROP_MONUMENT : PROP_LAMP);
            }
            if (Math.abs(along) <= 1.6F && perp > ROAD_HALF_WIDTH + 1.5F && perp <= ROAD_HALF_WIDTH + 2.6F) {
                return pack(SURFACE_APRON, PROP_RUBBLE);
            }
            return rail(passage, perp);
        }
        // A tended bed beside the road, fenced on three sides and open to the
        // path. Every border band is at least FENCE_MIN_THICKNESS wide: the
        // old bed drew its ends and its far side 0.9 tiles thick, which on a
        // slanted road rasterises into a diagonal staircase, and a diagonal
        // staircase of fence tiles is a row of unconnected posts.
        if (side != pickSide || Math.abs(along) > 3.8F) {
            return rail(passage, perp);
        }
        if (perp <= ROAD_HALF_WIDTH + 0.6F) {
            return 0;
        }
        if (perp > ROAD_HALF_WIDTH + 5.6F) {
            return rail(passage, perp);
        }
        if (perp <= ROAD_HALF_WIDTH + 1.5F) {
            return pack(SURFACE_APRON, PROP_CLEAR);              // the verge
        }
        if (Math.abs(along) > 2.2F || perp > ROAD_HALF_WIDTH + 4.0F) {
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
     * The passage balustrade at this perpendicular distance, or 0.
     *
     * Returned as the FALLBACK of every waypoint branch rather than as a
     * separate pass, so anything the roadside already puts on the verge — a
     * lamp pair, a waystone, the open mouth of a tended bed — wins the tile
     * and the balustrade simply stops on either side of it. That is what turns
     * a continuous railing into a railing with gaps you can step through,
     * which is what a causeway actually needs.
     */
    private static int rail(boolean passage, float perp) {
        if (!passage) {
            return 0;
        }
        return perp > ROAD_HALF_WIDTH + 0.3F && perp <= RAIL_OUTER
                ? pack(SURFACE_APRON, PROP_RAIL)
                : 0;
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
     * Is arc position {@code s} inside the border gateway of an edge of length
     * {@code len}? One gateway, at the middle, and only on a road long enough
     * that it stands well clear of the entrances at both ends.
     */
    private static boolean isBorderGate(float s, float len) {
        return len >= BORDER_GATE_MIN_LENGTH
                && len * 0.5F >= BORDER_GATE_CLEARANCE
                && Math.abs(s - len * 0.5F) <= GATE_DEPTH;
    }

    // ------------------------------------------------------------------
    // Designed places
    // ------------------------------------------------------------------

    /**
     * Which designed place stands at the node of cell (cx, cy). Shares, from
     * the hash: Garden Court 30%, Waystation Square 25%, Overlook Terrace 20%,
     * Orchard 25%.
     */
    public static int stationKind(int seed, int cx, int cy) {
        float k = SkyNoise.hash(seed + SALT_STATION + 1, cx, cy);
        if (k < 0.30F) {
            return 0;
        }
        if (k < 0.55F) {
            return 1;
        }
        if (k < 0.75F) {
            return 2;
        }
        return 3;
    }

    /**
     * Which of the node's four roads exist, as {@link #LINK_E} | {@link #LINK_S}
     * | {@link #LINK_W} | {@link #LINK_N}. The east and south roads are the
     * node's own; the west and north ones belong to its neighbours.
     */
    private static int links(int seed, int cx, int cy, int hubCellX, int hubCellY) {
        int mask = 0;
        if (linkExists(seed, cx, cy, 0, hubCellX, hubCellY)) {
            mask |= LINK_E;
        }
        if (linkExists(seed, cx, cy, 1, hubCellX, hubCellY)) {
            mask |= LINK_S;
        }
        if (linkExists(seed, cx - 1, cy, 0, hubCellX, hubCellY)) {
            mask |= LINK_W;
        }
        if (linkExists(seed, cx, cy - 1, 1, hubCellX, hubCellY)) {
            mask |= LINK_N;
        }
        return mask;
    }

    /**
     * How far from its node a road meets a designed place on the side
     * {@code link} names: the railing's distance on that axis, plus the
     * straight stub laid outward from the gate. 0 for a plain junction.
     */
    private static float mouthReach(int kind, float radius, int link) {
        if (radius <= 0.0F) {
            return 0.0F;
        }
        return railDistance(kind, Math.round(radius), link) + SPOKE_STUB;
    }

    /**
     * Distance from the centre to the enclosure's railing along the axis of
     * {@code link}. Only the Overlook Terrace is not square: its short sides
     * are {@code radius - 2} out.
     */
    private static int railDistance(int kind, int radius, int link) {
        if (kind == 2 && (link == LINK_N || link == LINK_S)) {
            return radius - 2;
        }
        return radius;
    }

    /** The side a tile lies on, as a link bit, if it is within the 3-wide axis band; else 0. */
    private static int axisSide(int dx, int dy) {
        if (Math.abs(dy) <= 1 && dx != 0 && Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? LINK_E : LINK_W;
        }
        if (Math.abs(dx) <= 1 && dy != 0 && Math.abs(dy) > Math.abs(dx)) {
            return dy > 0 ? LINK_S : LINK_N;
        }
        return 0;
    }

    /**
     * The entrance of an enclosure whose railing stands {@code rail} tiles out
     * on every axis the mask opens: the three-wide gate in the railing, a wall
     * pillar framing it on each side, and the straight stub of road outside it.
     *
     * <p>ONE gateway per road. The pillars are what make three fence gates
     * abreast read as a single gate rather than a gap in a fence, and a fence
     * attaches to a wall, so the railing runs straight into them.
     *
     * @return packed surface/prop, or 0 when the tile is not part of an entrance
     */
    private static int entrance(int dx, int dy, int rail, int mask) {
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int along;
        int across;
        int side;
        if (ax >= ay) {
            along = ax;
            across = ay;
            side = dx > 0 ? LINK_E : LINK_W;
        } else {
            along = ay;
            across = ax;
            side = dy > 0 ? LINK_S : LINK_N;
        }
        if ((mask & side) == 0 || across > 2 || along < rail || along > rail + SPOKE_STUB) {
            return 0;
        }
        if (along == rail) {
            return across <= 1 ? pack(SURFACE_ROAD, PROP_GATE) : pack(SURFACE_APRON, PROP_PILLAR);
        }
        return across <= 1 ? pack(SURFACE_ROAD, PROP_NONE) : 0;
    }

    /**
     * The designed places, laid out in world-aligned tile geometry (the warp
     * is attenuated to nothing here, so these stay crisp). Each one opens a
     * gate only on the sides a road actually arrives from; the others stay
     * closed railing.
     *
     * <ul>
     *   <li>0 — <b>Garden Court</b>: a round fenced plot, quartered flower
     *       beds, corner trees. Its heart is a statue, a single great tree, a
     *       cloud-sea basin or one lamp, chosen per court.</li>
     *   <li>1 — <b>Waystation Square</b>: a paved square inside a railing with
     *       four corner lamps. Its heart is an instrument, a basin, or a
     *       market of crates round a lamp -- or it is left open.</li>
     *   <li>2 — <b>Overlook Terrace</b>: a planted apron round a raised
     *       platform; the instrument stands at one end looking out, never in
     *       the middle.</li>
     *   <li>3 — <b>Orchard</b>: trees in rows with fruiting bushes between them
     *       inside a fence, crossed by the paths the roads bring in.</li>
     * </ul>
     *
     * <p>Until 2026-09-23 there were three kinds and every one of them stood a
     * statue or an instrument on its centre tile, and two nodes in three had
     * one: the player met the same composition around every corner.
     *
     * @param links    which sides have a road ({@link #LINK_E} etc.)
     * @param variant  the place's own hash in [0,1), for its per-place choices
     * @param dx       tile position relative to the (integer-snapped) node
     */
    private static int station(int seed, int kind, int radius, int links, float variant,
                               int dx, int dy, int tileX, int tileY) {
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int mx = Math.max(ax, ay);
        int mn = Math.min(ax, ay);
        int side = axisSide(dx, dy);
        boolean openSpoke = side != 0 && (links & side) != 0;

        // Entrances first: the gate, its pillars and the stub outside. The
        // railing's distance depends on the axis only for the terrace.
        int facing = ax >= ay ? (dx > 0 ? LINK_E : LINK_W) : (dy > 0 ? LINK_S : LINK_N);
        int gate = entrance(dx, dy, railDistance(kind, radius, facing), links);
        if (gate != 0) {
            return gate;
        }

        if (kind == 0) {
            // --- Garden Court ---
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > radius + 0.5F) {
                return 0;
            }
            int heart = heartOf(0, variant);
            if (heart == HEART_POOL && mx <= 2) {
                return mx <= 1 ? pack(SURFACE_POOL, PROP_NONE) : pack(SURFACE_PLINTH, PROP_CLEAR);
            }
            if (mx <= 1) {
                if (heart == HEART_TREE) {
                    return dx == 0 && dy == 0 ? pack(SURFACE_GARDEN, PROP_TREE)
                            : pack(SURFACE_GARDEN, PROP_CLEAR);
                }
                if (dx == 0 && dy == 0) {
                    return pack(SURFACE_PLINTH, heart == HEART_LAMP ? PROP_LAMP : PROP_STATUE);
                }
                return pack(SURFACE_PLINTH, PROP_CLEAR);
            }
            if (discRing(dx, dy, radius)) {
                return pack(SURFACE_GARDEN, PROP_FENCE);
            }
            if (openSpoke) {
                return pack(SURFACE_ROAD, PROP_NONE);
            }
            if (ax == ay && ax == radius - 3) {
                return pack(SURFACE_GARDEN, PROP_TREE);
            }
            if (mn == 2 && mx == radius - 2 && (links & sideOfSpokeMouth(dx, dy)) != 0) {
                return pack(SURFACE_APRON, PROP_LAMP);           // beside a used way in
            }
            return bed(seed, tileX, tileY, 0.42F, 0.62F);
        }

        if (kind == 1) {
            // --- Waystation Square ---
            if (mx > radius) {
                return 0;
            }
            if (mx == radius) {
                return pack(SURFACE_APRON, PROP_FENCE);           // railing
            }
            int heart = heartOf(1, variant);
            if (heart == HEART_POOL && mx <= 2) {
                return mx <= 1 ? pack(SURFACE_POOL, PROP_NONE) : pack(SURFACE_PLINTH, PROP_CLEAR);
            }
            if (mx <= 1) {
                if (dx == 0 && dy == 0 && heart == HEART_INSTRUMENT) {
                    return pack(SURFACE_PLINTH, PROP_INSTRUMENT);
                }
                if (dx == 0 && dy == 0 && heart == HEART_MARKET) {
                    return pack(SURFACE_PLINTH, PROP_LAMP);
                }
                return pack(heart == HEART_OPEN ? SURFACE_INLAY : SURFACE_PLINTH, PROP_CLEAR);
            }
            if (openSpoke) {
                return pack(SURFACE_ROAD, PROP_NONE);
            }
            if (ax == ay && ax == radius - 2) {
                return pack(SURFACE_APRON, PROP_LAMP);
            }
            int field = mx <= radius - 4 ? SURFACE_INLAY : SURFACE_COURT;
            if (heart == HEART_MARKET && mn >= 2 && mx <= radius - 3) {
                // Stalls: a short row of crates in two opposite quadrants,
                // standing back from the paths so the square stays walkable.
                boolean quadrant = (dx > 0) == (dy > 0);
                if (quadrant == (variant < 0.5F) && ay == 3 && ax >= 3 && ax <= 4) {
                    return pack(field, PROP_CRATE);
                }
            }
            return pack(field, PROP_CLEAR);
        }

        if (kind == 2) {
            // --- Overlook Terrace ---
            int w = radius;
            int h = radius - 2;
            if (ax > w || ay > h) {
                return 0;
            }
            if (ax == w || ay == h) {
                return pack(SURFACE_APRON, PROP_FENCE);           // railing
            }
            if (ax <= w - 3 && ay <= h - 2) {
                // The platform. Its middle is left open and so are the paths
                // across it; the instrument stands at the end that looks out,
                // beside the path rather than on it, and a pair of lamps
                // answers it at the other end.
                int end = variant < 0.5F ? 1 : -1;
                if (ay == 2 && ax == w - 4) {
                    return dx * end > 0
                            ? (dy > 0 ? pack(SURFACE_PLINTH, PROP_INSTRUMENT) : pack(SURFACE_PLINTH, PROP_CLEAR))
                            : pack(SURFACE_APRON, PROP_LAMP);
                }
                return pack(ay <= h - 4 ? SURFACE_INLAY : SURFACE_COURT, PROP_CLEAR);
            }
            if (openSpoke) {
                return pack(SURFACE_ROAD, PROP_NONE);           // the way in, across the apron
            }
            return bed(seed, tileX, tileY, 0.34F, 0.48F);
        }

        // --- kind 3: Orchard ---
        if (mx > radius) {
            return 0;
        }
        if (mx == radius) {
            return pack(SURFACE_GARDEN, PROP_FENCE);
        }
        if (openSpoke) {
            return pack(SURFACE_ROAD, PROP_NONE);
        }
        if (mx <= 1) {
            return pack(SURFACE_ROAD, PROP_NONE);                // where the paths cross
        }
        if (mx == radius - 1) {
            return pack(SURFACE_GARDEN, PROP_CLEAR);             // headland inside the fence
        }
        // Rows: a tree every third tile along a row, the rows three apart,
        // a bush midway between two trees. Offset by one so no tree lands on
        // the paths' edge.
        int rx = Math.floorMod(dx + 1, 3);
        int ry = Math.floorMod(dy + 1, 3);
        if (ry == 0 && rx == 0 && mn >= 2) {
            return pack(SURFACE_GARDEN, PROP_TREE);
        }
        if (ry == 0 && rx == 2 && mn >= 2) {
            return pack(SURFACE_GARDEN, PROP_BUSH);
        }
        return SkyNoise.tileRoll(seed, tileX, tileY, SALT_BED) < 0.25F
                ? pack(SURFACE_GARDEN, PROP_GRASS)
                : pack(SURFACE_GARDEN, PROP_CLEAR);
    }

    /** What stands at the heart of a place. */
    private static final int HEART_STATUE = 0;
    private static final int HEART_TREE = 1;
    private static final int HEART_POOL = 2;
    private static final int HEART_LAMP = 3;
    private static final int HEART_INSTRUMENT = 4;
    private static final int HEART_MARKET = 5;
    private static final int HEART_OPEN = 6;

    /**
     * The heart of a Garden Court (kind 0) or Waystation Square (kind 1),
     * from the place's own hash. Rescaled from the upper part of the hash so it
     * is independent of the market's quadrant choice, which reads the lower.
     */
    private static int heartOf(int kind, float variant) {
        float v = (variant * 7.0F) % 1.0F;
        if (kind == 0) {
            if (v < 0.30F) {
                return HEART_STATUE;
            }
            if (v < 0.60F) {
                return HEART_TREE;
            }
            if (v < 0.85F) {
                return HEART_POOL;
            }
            return HEART_LAMP;
        }
        if (v < 0.30F) {
            return HEART_INSTRUMENT;
        }
        if (v < 0.50F) {
            return HEART_POOL;
        }
        if (v < 0.75F) {
            return HEART_MARKET;
        }
        return HEART_OPEN;
    }

    /** The side whose spoke mouth the lamp tile at |mn|==2 flanks. */
    private static int sideOfSpokeMouth(int dx, int dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? LINK_E : LINK_W;
        }
        return dy > 0 ? LINK_S : LINK_N;
    }

    /** A planted bed: flowers below {@code flower}, grass below {@code grass}, else bare. */
    private static int bed(int seed, int tileX, int tileY, float flower, float grass) {
        float fill = SkyNoise.tileRoll(seed, tileX, tileY, SALT_BED);
        if (fill < flower) {
            return pack(SURFACE_GARDEN, PROP_FLOWER);
        }
        if (fill < grass) {
            return pack(SURFACE_GARDEN, PROP_GRASS);
        }
        return pack(SURFACE_GARDEN, PROP_CLEAR);
    }

    /**
     * One of the candelabra standing in a ring on the Warden's Forecourt.
     *
     * <p>The phase is a quarter step, not a half step, and that is the whole
     * point of it. With six lamps at a half step the ring lands on angles
     * 30/90/150/210/270/330 degrees, and 90 and 270 are exactly the south and
     * north axes — so a lamp stood on the tile directly in front of the
     * Spire's grand door, on the line the player walks in on. A quarter step
     * gives (11,3) (3,11) (-8,8) (-11,-3) (-3,-11) (8,-8): all six survive and
     * none is on an axis.
     *
     * <p>The axis guard below is not redundant with that. It is what keeps the
     * approaches clear if the count, the radius or the phase is ever changed
     * again, which is how the lamp got in front of the door in the first
     * place. All four of the Spire's doors sit on an axis through the origin.
     */
    private static boolean isHubLamp(int tileX, int tileY, int originX, int originY) {
        for (int k = 0; k < HUB_LAMP_COUNT; k++) {
            double angle = k * (2.0 * Math.PI / HUB_LAMP_COUNT) + Math.PI / (2 * HUB_LAMP_COUNT);
            int dx = (int) Math.round(Math.cos(angle) * HUB_LAMP_RADIUS);
            int dy = (int) Math.round(Math.sin(angle) * HUB_LAMP_RADIUS);
            if (Math.abs(dx) <= DOOR_AXIS_CLEARANCE || Math.abs(dy) <= DOOR_AXIS_CLEARANCE) {
                continue;   // would stand on a door approach
            }
            if (tileX == originX + dx && tileY == originY + dy) {
                return true;
            }
        }
        return false;
    }
}
