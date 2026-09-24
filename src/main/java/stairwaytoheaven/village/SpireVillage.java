package stairwaytoheaven.village;

import java.awt.Point;
import java.util.function.Supplier;

import necesse.engine.network.server.Server;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.Level;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetRotation;
import necesse.level.maps.presets.PresetUtils;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.settlement.SkySettlers;
import stairwaytoheaven.worldgen.SkyLandscape;
import stairwaytoheaven.worldgen.SkyOrigin;

/**
 * Das Himmelsdorf — the Spire Village: one house per named resident, in a ring
 * around the Old Warden Spire.
 *
 * <h2>Why it exists</h2>
 * The player, 2026-09-24: <i>"Ich denke wir müssen alle NPCs, die man auf
 * Himmelsebene irgendwo finden kann, zentral um den Spire in Häusern wohnen
 * lassen und Stück für Stück Quests von ihnen kriegen, für die man immer weiter
 * in tiefe Gebiete ziehen muss."</i> Recorded in {@code DESIGN_DECISIONS.md};
 * it supersedes the older rule that each resident is FOUND in their own realm.
 * The whole layout, every house and the quest ladder are in
 * {@code docs/design/chapter-03-spire-village.md}.
 *
 * <h2>The layout, in tiles from the spire's centre</h2>
 * <pre>
 *   r &lt;= 21.5         the Warden's Forecourt — SkyLandscape's, never touched
 *   Chebyshev 23..25   the Ring Lane (Ringgasse), paved
 *   |x| or |y| &lt;= 1   the four avenues on the spire's door axes, paved out to 38
 *   Chebyshev 26..35   eight side blocks and four corner plots — the houses
 *   Chebyshev 36..37   the Outer Lane (Außenweg), paved
 *   Chebyshev 38       the verge, with a street lamp every eight tiles
 * </pre>
 * Everything up to Chebyshev 38 lies inside the radius the terrain painter
 * guarantees as land ({@code SkyOrigin.HUB_RADIUS} 56: the farthest corner is
 * 53.7 tiles out), so no house can stand on a shore.
 *
 * <h2>Once per world, and again after a regeneration</h2>
 * Stamped lazily from {@code SkyLevel.ensureWardenSpire}, exactly the way the
 * spire and the landmarks are, and remembered in
 * {@link SkywatchQuestData#villagePlaced}. The residents are a second,
 * independent question, answered by {@link SkywatchWorldData#residentsClaimed}
 * like every other named person in the mod — a building is a fact about this
 * Skyreach, a person is a fact about the world.
 *
 * <p>An existing save gets the village on its next ascent — unless a
 * settlement claims any region of its footprint, in which case nothing is
 * written and the census says {@code blocked=settlement}. That is the one place
 * a player may have built beside the spire, and the stamp overwrites the whole
 * ring.
 */
public final class SpireVillage {

    private SpireVillage() {
    }

    /** Chebyshev extent of everything the village writes, in tiles. */
    public static final int RADIUS = 38;
    /** The forecourt disc SkyLandscape owns; nothing at or inside it is written. */
    public static final float FORECOURT = SkyLandscape.HUB_COURT_RADIUS + 0.5F;
    public static final int RING_LANE_INNER = 23;
    public static final int RING_LANE_OUTER = 25;
    public static final int OUTER_LANE_INNER = 36;
    public static final int OUTER_LANE_OUTER = 37;

    /** How far a resident strolls from their door by day (HumanAI's own default). */
    public static final int HOME_RADIUS = 10;

    /** One plot of the village: a plan, where it stands, how it is turned, who lives there. */
    public enum House {
        // side blocks, clockwise from the arrival (south)
        MAGPIE("magpie", SkySettlers.MAGPIE, SpireVillagePlans::magpie, SpireVillagePlans.MAGPIE_PLAN,
                3, 26, PresetRotation.HALF_180),
        HALDA("halda", SkySettlers.HALDA, SpireVillagePlans::halda, SpireVillagePlans.HALDA_PLAN,
                26, 3, PresetRotation.CLOCKWISE),
        OSSIAN("ossian", SkySettlers.OSSIAN, SpireVillagePlans::ossian, SpireVillagePlans.OSSIAN_PLAN,
                26, -25, PresetRotation.CLOCKWISE),
        EVELEEN("eveleen", SkySettlers.EVELEEN, SpireVillagePlans::eveleen, SpireVillagePlans.EVELEEN_PLAN,
                3, -35, null),
        IVES("ives", SkySettlers.IVES, SpireVillagePlans::ives, SpireVillagePlans.IVES_PLAN,
                -25, -35, null),
        MORTIMER("mortimer", SkySettlers.MORTIMER, SpireVillagePlans::mortimer, SpireVillagePlans.MORTIMER_PLAN,
                -35, -25, PresetRotation.ANTI_CLOCKWISE),
        CASPERN("caspern", SkySettlers.CASPERN, SpireVillagePlans::caspern, SpireVillagePlans.CASPERN_PLAN,
                -35, 3, PresetRotation.ANTI_CLOCKWISE),
        MARKET("market", null, SpireVillagePlans::market, SpireVillagePlans.MARKET_PLAN,
                -25, 26, PresetRotation.HALF_180),
        // corner plots
        KNOTT("knott", SkySettlers.KNOTT, SpireVillagePlans::knott, SpireVillagePlans.KNOTT_PLAN,
                -35, -35, PresetRotation.HALF_180),
        ELEANOR("eleanor", SkySettlers.ELEANOR, SpireVillagePlans::eleanor, SpireVillagePlans.ELEANOR_PLAN,
                -35, 26, null),
        EDENBED("edenbed", null, SpireVillagePlans::edenBed, SpireVillagePlans.EDENBED_PLAN,
                26, -35, PresetRotation.HALF_180),
        RING("ring", null, SpireVillagePlans::ring, SpireVillagePlans.RING_PLAN,
                26, 26, null);

        public final String key;
        /** The resident's mob ID, or null for a place nobody lives in. */
        public final String resident;
        private final Supplier<Preset> build;
        public final String[] plan;
        /** Top-left corner of the TURNED preset, in tiles from the spire centre. */
        public final int offsetX;
        public final int offsetY;
        /** null = drawn as it stands. */
        public final PresetRotation rotation;

        House(String key, String resident, Supplier<Preset> build, String[] plan,
              int offsetX, int offsetY, PresetRotation rotation) {
            this.key = key;
            this.resident = resident;
            this.build = build;
            this.plan = plan;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.rotation = rotation;
        }

        /** The plan, built fresh and turned to face its street. */
        public Preset preset() {
            Preset canonical = this.build.get();
            if (this.rotation == null) {
                return canonical;
            }
            try {
                return canonical.rotate(this.rotation);
            } catch (necesse.level.maps.presets.PresetRotateException e) {
                // tryRotate would hand back the UNturned copy and the house
                // would stand with its back to the street. Fail loudly instead;
                // validateAll turns this into a boot failure.
                throw new IllegalStateException("Spire Village house " + this.key
                        + " cannot be turned " + this.rotation + ": " + e.getMessage(), e);
            }
        }

        /** Where the resident's '@' lands, in tiles from the spire centre; null if none. */
        public Point seatOffset() {
            for (int y = 0; y < this.plan.length; y++) {
                int x = this.plan[y].indexOf('@');
                if (x >= 0) {
                    int w = this.plan[0].length();
                    int h = this.plan.length;
                    Point p = this.rotation == null ? new Point(x, y)
                            : PresetUtils.getRotatedPointInSpace(x, y, w, h, this.rotation);
                    return new Point(this.offsetX + p.x, this.offsetY + p.y);
                }
            }
            return null;
        }

        /** Width and height of the turned preset. */
        public int turnedWidth() {
            boolean quarter = this.rotation == PresetRotation.CLOCKWISE
                    || this.rotation == PresetRotation.ANTI_CLOCKWISE;
            return quarter ? this.plan.length : this.plan[0].length();
        }

        public int turnedHeight() {
            boolean quarter = this.rotation == PresetRotation.CLOCKWISE
                    || this.rotation == PresetRotation.ANTI_CLOCKWISE;
            return quarter ? this.plan[0].length() : this.plan.length;
        }
    }

    /** The house a resident lives in, or null if they have none in the village. */
    public static House houseOf(String mobStringID) {
        for (House house : House.values()) {
            if (mobStringID != null && mobStringID.equals(house.resident)) {
                return house;
            }
        }
        return null;
    }

    /** The resident's home tile in THIS world, or null. */
    public static Point seatOf(Level level, House house) {
        Point offset = house == null ? null : house.seatOffset();
        if (offset == null) {
            return null;
        }
        Point origin = SkyOrigin.compute(SkyOrigin.worldGenSeed(level.getWorldEntity()));
        return new Point(origin.x + offset.x, origin.y + offset.y);
    }

    /**
     * Builds every house in the rotation it is used in. Called at
     * {@code postInit}, when every object and tile the legends name is
     * registered: a slip in a plan — or a piece the engine refuses to turn —
     * stops the server there, not in a player's village.
     */
    public static void validateAll() {
        for (House house : House.values()) {
            Preset p = house.preset();
            if (p.width != house.turnedWidth() || p.height != house.turnedHeight()) {
                throw new IllegalStateException("Spire Village house " + house.key
                        + " turned to " + p.width + "x" + p.height + ", expected "
                        + house.turnedWidth() + "x" + house.turnedHeight());
            }
        }
    }

    // ------------------------------------------------------------------
    // Stamping
    // ------------------------------------------------------------------

    /** What {@link #ensure} did, for the census. */
    public static String lastBlockReason = "none";

    /**
     * Stamps the village if this Skyreach does not have it yet, then seats
     * every resident the world does not already have somewhere.
     *
     * @return true if the village stands (now or before)
     */
    public static boolean ensure(Level level) {
        if (level == null || level.isClient()) {
            return false;
        }
        SkywatchQuestData quest = SkywatchQuestData.get(level);
        if (!quest.spirePlaced) {
            return false;
        }
        Point origin = SkyOrigin.compute(SkyOrigin.worldGenSeed(level.getWorldEntity()));
        if (!quest.villagePlaced) {
            level.regionManager.ensureTilesAreLoaded(origin.x - RADIUS - 1, origin.y - RADIUS - 1,
                    origin.x + RADIUS + 1, origin.y + RADIUS + 1);
            String blocked = blockedBy(level, origin);
            lastBlockReason = blocked;
            if (blocked != null) {
                return false;
            }
            stamp(level, origin);
            quest.villagePlaced = true;
        }
        lastBlockReason = "none";
        seatResidents(level);
        return true;
    }

    /** Why the ring cannot be written, or null. */
    private static String blockedBy(Level level, Point origin) {
        necesse.engine.world.worldData.SettlementsWorldData settlements =
                level.getServer() == null ? null
                        : necesse.engine.world.worldData.SettlementsWorldData.getSettlementsData(
                                level.getServer());
        if (settlements == null) {
            return null;
        }
        for (int x = origin.x - RADIUS; x <= origin.x + RADIUS; x += 8) {
            for (int y = origin.y - RADIUS; y <= origin.y + RADIUS; y += 8) {
                if (settlements.hasSettlementAtTile(level, x, y)) {
                    return "settlement";
                }
            }
        }
        return null;
    }

    private static void stamp(Level level, Point origin) {
        base().applyToLevel(level, origin.x - RADIUS, origin.y - RADIUS);
        for (House house : House.values()) {
            house.preset().applyToLevel(level, origin.x + house.offsetX, origin.y + house.offsetY);
        }
    }

    /**
     * The ground of the whole ring: lanes, avenues, turf, lamps and the
     * forecourt corner gardens. Everything it writes clears every object
     * layer first, so no road lamp or boulder the painter left stands inside
     * a house; the forecourt disc and the spire are written as -1 (untouched).
     */
    public static Preset base() {
        int size = RADIUS * 2 + 1;
        Preset p = new Preset(size, size);
        int turf = SkyRegistry.cloudturfID;
        int road = SkyRegistry.skyroadTileID;
        int lamp = SkyRegistry.wardenCandelabraID;
        int layers = p.objects.length;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                if (Math.sqrt(dx * dx + dy * dy) <= FORECOURT) {
                    continue;
                }
                int x = dx + RADIUS;
                int y = dy + RADIUS;
                int c = Math.max(Math.abs(dx), Math.abs(dy));
                boolean avenue = Math.abs(dx) <= 1 || Math.abs(dy) <= 1;
                int tile;
                if (avenue || (c >= RING_LANE_INNER && c <= RING_LANE_OUTER)
                        || (c >= OUTER_LANE_INNER && c <= OUTER_LANE_OUTER)) {
                    tile = road;
                } else {
                    tile = turf;
                }
                p.setTile(x, y, tile);
                for (int layer = 0; layer < layers; layer++) {
                    p.setObjectLayer(layer, x, y, 0);
                }
                int object = 0;
                if (!avenue && c == RADIUS) {
                    // The verge: a street lamp every eight tiles along each side.
                    int along = Math.abs(dx) == RADIUS ? dy : dx;
                    if (along % 8 == 0 && Math.abs(along) >= 8) {
                        object = lamp;
                    }
                } else if (c < RING_LANE_INNER && !avenue) {
                    // Between the forecourt rail and the Ring Lane: four small
                    // gardens, a lamp where the lane turns and one at each side.
                    int ax = Math.abs(dx);
                    int ay = Math.abs(dy);
                    if ((ax == 19 && ay == 19) || (ax == 22 && ay == 12) || (ax == 12 && ay == 22)) {
                        object = lamp;
                    } else if (stairwaytoheaven.worldgen.SkyNoise.hash(0x51A6E, dx, dy) < 0.28F) {
                        object = stairwaytoheaven.worldgen.SkyNoise.hash(0x51A6F, dx, dy) < 0.5F
                                ? SkyRegistry.skytulipID : SkyRegistry.cloudbellID;
                    }
                }
                if (object != 0) {
                    p.setObject(x, y, object);
                }
            }
        }
        return p;
    }

    // ------------------------------------------------------------------
    // The people
    // ------------------------------------------------------------------

    /**
     * Stands every resident who is not claimed anywhere yet in their own
     * doorway-side seat, home-bound. Idempotent: a claimed resident — seated
     * here earlier, recruited into a town, or found somewhere by an older
     * build — is never seated a second time.
     */
    public static void seatResidents(Level level) {
        Server server = level.getServer();
        if (server == null || !SkywatchQuestData.get(level).villagePlaced) {
            return;
        }
        for (House house : House.values()) {
            if (house.resident == null) {
                continue;
            }
            if (SkySettlers.ELEANOR.equals(house.resident) && SkywatchWorldData.eleanorGone(server)) {
                continue;
            }
            if (SkywatchWorldData.residentClaimed(server, house.resident)) {
                continue;
            }
            Point seat = seatOf(level, house);
            if (seat == null) {
                continue;
            }
            level.regionManager.ensureTilesAreLoaded(seat.x - 2, seat.y - 2, seat.x + 2, seat.y + 2);
            Mob mob = MobRegistry.getMob(house.resident, level);
            if (mob == null) {
                continue;
            }
            mob.canDespawn = false;
            if (mob instanceof HumanMob) {
                // Vanilla's own anchor: HumanAI's wanderer returns mob.home as
                // the base tile of anyone who is not a settler, strolls within
                // its search radius of it by day and walks INSIDE the house it
                // stands in at night (HumanAI.java:163-201, WandererAINode
                // findNewPositionInsideBase — VERIFIED [jar] in the decompiled
                // source). HumanMob saves and loads the field, so the home
                // survives a restart without a line of ours.
                ((HumanMob) mob).home = new Point(seat.x, seat.y);
            }
            level.entityManager.addMob(mob, seat.x * 32 + 16, seat.y * 32 + 16);
            SkywatchWorldData.claimResident(server, house.resident);
            SkywatchWorldData.markVillageResident(server, house.resident);
        }
    }

    /**
     * Brings a resident an older build left somewhere in the sky home to the
     * village. Called from the resident's own server tick, so it only ever
     * touches a mob that is loaded — the save's scattered Magpie moves in the
     * first time her region is walked into, not before.
     *
     * @return true if the mob was moved
     */
    public static boolean bringHome(HumanMob mob) {
        Level level = mob.getLevel();
        if (level == null || level.isClient() || !(level instanceof stairwaytoheaven.level.SkyLevel)) {
            return false;
        }
        if (mob.home != null || mob.isSettler() || mob.isVisitor()) {
            return false;
        }
        if (!SkywatchQuestData.get(level).villagePlaced) {
            return false;
        }
        House house = houseOf(mob.getStringID());
        Point seat = seatOf(level, house);
        if (seat == null) {
            return false;
        }
        mob.home = new Point(seat.x, seat.y);
        if (Math.abs(mob.getTileX() - seat.x) > HOME_RADIUS * 2
                || Math.abs(mob.getTileY() - seat.y) > HOME_RADIUS * 2) {
            level.regionManager.ensureTilesAreLoaded(seat.x - 2, seat.y - 2, seat.x + 2, seat.y + 2);
            mob.setPos(seat.x * 32 + 16, seat.y * 32 + 16, true);
            mob.sendMovementPacket(true);
        }
        Server server = level.getServer();
        if (server != null) {
            SkywatchWorldData.markVillageResident(server, mob.getStringID());
        }
        return true;
    }

    /**
     * For the upcoming {@code /swhreset regenerate}: forget that this
     * Skyreach has its village and that its residents live there, so the next
     * {@link #ensure} stamps and seats them again.
     *
     * <p>Only residents recorded in {@link SkywatchWorldData#villageResidents}
     * lose their claim — they were standing in the regions being deleted. A
     * resident who moved into a town is not in that set and keeps their claim,
     * so the world can never grow a second one.
     */
    public static void prepareRegeneration(Level skyLevel) {
        if (skyLevel == null || skyLevel.isClient()) {
            return;
        }
        SkywatchQuestData.get(skyLevel).villagePlaced = false;
        SkywatchWorldData world = SkywatchWorldData.get(skyLevel.getServer());
        if (world != null) {
            world.residentsClaimed.removeAll(world.villageResidents);
            world.villageResidents.clear();
        }
    }

    // ------------------------------------------------------------------
    // Census
    // ------------------------------------------------------------------

    /** {expected, missing} objects of one house as it should stand in this world. */
    public static int[] standing(Level level, House house, StringBuilder firstMissing) {
        Point origin = SkyOrigin.compute(SkyOrigin.worldGenSeed(level.getWorldEntity()));
        Preset p = house.preset();
        int x0 = origin.x + house.offsetX;
        int y0 = origin.y + house.offsetY;
        level.regionManager.ensureTilesAreLoaded(x0, y0, x0 + p.width - 1, y0 + p.height - 1);
        int expected = 0;
        int missing = 0;
        for (int x = 0; x < p.width; x++) {
            for (int y = 0; y < p.height; y++) {
                int wanted = p.getObject(ObjectLayerRegistry.BASE_LAYER, x, y);
                if (wanted <= 0) {
                    continue;
                }
                expected++;
                int standing = level.getObjectID(x0 + x, y0 + y);
                if (standing == wanted) {
                    continue;
                }
                necesse.level.gameObject.GameObject placed =
                        necesse.engine.registries.ObjectRegistry.getObject(wanted);
                if (placed instanceof necesse.level.gameObject.SwitchObject
                        && ((necesse.level.gameObject.SwitchObject) placed).counterID == standing) {
                    continue;   // an opened door
                }
                missing++;
                if (firstMissing != null && missing <= 8) {
                    firstMissing.append(' ').append(x).append(',').append(y).append('=')
                            .append(standing).append("!=").append(wanted);
                }
            }
        }
        return new int[]{expected, missing};
    }
}
