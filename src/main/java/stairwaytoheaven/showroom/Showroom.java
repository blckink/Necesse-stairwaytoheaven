package stairwaytoheaven.showroom;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketRegionData;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.SignObjectEntity;
import necesse.entity.pickup.PickupEntity;
import necesse.level.gameObject.DoorObject;
import necesse.level.gameObject.FenceGateObject;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.WallDoorObject;
import necesse.level.gameObject.WallObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.realms.crooked.DoorYardPreset;
import stairwaytoheaven.realms.crooked.InvertedHousePreset;
import stairwaytoheaven.realms.crooked.LongTablePreset;
import stairwaytoheaven.realms.ghost.HauntedManorPreset;
import stairwaytoheaven.realms.ghost.MausoleumPreset;
import stairwaytoheaven.realms.ghost.SunkenGraveyardPreset;
import stairwaytoheaven.realms.steinfeld.GraveyardPreset;
import stairwaytoheaven.realms.steinfeld.RuinedChapelPreset;
import stairwaytoheaven.surface.AeronautCampPreset;
import stairwaytoheaven.surface.SkyFragmentCraterPreset;
import stairwaytoheaven.surface.SkywardShrinePreset;
import stairwaytoheaven.worldgen.CrookedHousePreset;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.RealmLanding;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.SkyTerrainPainter;
import stairwaytoheaven.village.SpireVillage;
import stairwaytoheaven.worldgen.WardenSpirePreset;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;

/**
 * The showroom: every building, every material and every realm's ground,
 * stamped side by side on flat, lit, empty ground in the sky level, so the
 * player can walk through it (and {@code /swhshots} can photograph it) instead
 * of hunting the world for one example of each.
 *
 * <h2>Where it is, and why nothing real can be there</h2>
 * A fixed rectangle whose top-left corner is tile {@link #ANCHOR_X},
 * {@link #ANCHOR_Y} of the {@code skyreach2} level. The spire sits within 576
 * tiles of (0, 0) ({@code SkyOrigin.compute}) and the last realm band ends at
 * {@code RealmDepth.DEPTH_SCALE} = 6000 tiles from it, so the showroom starts
 * about 27,000 tiles out — past the end of Hell, where no quest, landmark or
 * travel rule ever sends a player. Everything the showroom writes stays inside
 * {@link Layout#bounds}; the command refuses to touch anything outside it.
 *
 * <h2>What an exhibit is</h2>
 * Each exhibit is one real {@link Preset}: the RealmPoiPresets catalogue entry,
 * the standalone preset class, or a gallery preset built here from the
 * registries. It is applied with {@code Preset.applyToLevel}, the same call
 * worldgen uses, so what stands in the showroom is what the world generator
 * writes — minus the mobs: guards, residents and packs are placed by the
 * WORLD presets and by {@code SkyLevel.onRegionGenerated}, never by a
 * {@code Preset}, and the showroom removes every mob its ground generated with.
 * The one preset whose stamp-time hook has side effects — the Warden's Spire,
 * which records the quest anchor and spawns the Warden — has its custom
 * applies stripped here.
 *
 * <h2>Determinism</h2>
 * The layout is a pure function of the registries (so a client can compute the
 * same exhibit positions as the server — {@code /swhshots} depends on that),
 * and every preset is built from a fixed seed, so {@code build} twice writes
 * the same thing twice and {@code check} can rebuild what {@code build} wrote.
 */
public final class Showroom {

    /** Top-left tile of the showroom in the sky level. See the class comment. */
    public static final int ANCHOR_X = 20000;
    public static final int ANCHOR_Y = 20000;
    /** Rows wrap after this many tiles. */
    public static final int ROW_WIDTH = 320;
    /** Tiles of free ground between an exhibit and the edge of its cell. */
    public static final int PAD = 5;
    /** Cleared margin around the whole showroom. */
    public static final int MARGIN = 8;

    public static final String SURFACE = "surface";
    /** Section order: one section (row group) per realm, then the surface. */
    public static final String[] GROUPS = {
            "skyreach", "eden", "steinfeld", "ghostrealm", "crookedbeyond", "hell", SURFACE};

    /** Width and height of a realm ground sample (one Necesse screen is ~40x22). */
    public static final int SAMPLE_W = 48;
    public static final int SAMPLE_H = 28;

    private static Layout cached;

    private Showroom() {
    }

    // ======================================================================
    // Layout
    // ======================================================================

    /** Builds a preset for an exhibit; {@code seed} is the world generation seed. */
    public interface PresetFactory {
        Preset build(int seed);
    }

    public static final class Exhibit {
        public final String id;
        public final String group;
        /** poi, preset, realm, tiles, walls, objects */
        public final String kind;
        public final int width;
        public final int height;
        final PresetFactory factory;
        /** Top-left tile of the preset footprint. */
        public int x;
        public int y;
        public int cellX;
        public int cellY;
        public int cellW;
        public int cellH;

        Exhibit(String id, String group, String kind, int width, int height, PresetFactory factory) {
            this.id = id;
            this.group = group;
            this.kind = kind;
            this.width = width;
            this.height = height;
            this.factory = factory;
        }

        public int signX() {
            return x + width / 2;
        }

        public int signY() {
            return y + height + 1;
        }

        /** Where {@code goto} puts the player: just below the sign, facing the exhibit. */
        public int viewX() {
            return signX();
        }

        public int viewY() {
            return signY() + 1;
        }

        /**
         * The rectangle a screenshot of this exhibit covers: the footprint plus
         * the sign row, and four tiles of headroom because tall sprites
         * (trees, doors, statues) are drawn up to three tiles above their tile.
         */
        public Rectangle captureRect() {
            return new Rectangle(x - 3, y - 5, width + 6, height + 11);
        }

        public Rectangle cellRect() {
            return new Rectangle(cellX, cellY, cellW, cellH);
        }
    }

    public static final class Layout {
        public final List<Exhibit> exhibits = new ArrayList<>();
        public final Map<String, Exhibit> byId = new LinkedHashMap<>();
        public Rectangle bounds;
    }

    /** The layout, computed once per game session (registries must be closed). */
    public static synchronized Layout layout() {
        if (cached == null) {
            cached = computeLayout();
        }
        return cached;
    }

    public static Exhibit find(String id) {
        return id == null ? null : layout().byId.get(id.toLowerCase());
    }

    private static Layout computeLayout() {
        Layout layout = new Layout();
        int rowY = ANCHOR_Y;
        int maxX = ANCHOR_X;
        for (String group : GROUPS) {
            List<Exhibit> section = sectionOf(group);
            int cursorX = ANCHOR_X;
            int rowH = 0;
            for (Exhibit e : section) {
                int cw = e.width + 2 * PAD;
                int ch = e.height + 2 * PAD + 2;
                if (cursorX > ANCHOR_X && cursorX + cw > ANCHOR_X + ROW_WIDTH) {
                    rowY += rowH;
                    cursorX = ANCHOR_X;
                    rowH = 0;
                }
                e.cellX = cursorX;
                e.cellY = rowY;
                e.cellW = cw;
                e.cellH = ch;
                e.x = cursorX + PAD;
                e.y = rowY + PAD;
                cursorX += cw;
                rowH = Math.max(rowH, ch);
                maxX = Math.max(maxX, cursorX);
                layout.exhibits.add(e);
                layout.byId.put(e.id, e);
            }
            rowY += rowH + 3;
        }
        layout.bounds = new Rectangle(ANCHOR_X - MARGIN, ANCHOR_Y - MARGIN,
                maxX - ANCHOR_X + 2 * MARGIN, rowY - ANCHOR_Y + 2 * MARGIN);
        return layout;
    }

    private static int realmOfGroup(String group) {
        for (int r = 0; r < RealmDepth.REALM_COUNT; r++) {
            if (RealmDepth.keyOf(r).equals(group)) return r;
        }
        return -1;
    }

    private static List<Exhibit> sectionOf(String group) {
        List<Exhibit> out = new ArrayList<>();
        int realm = realmOfGroup(group);
        if (realm >= 0) {
            final int r = realm;
            out.add(new Exhibit("realm-" + group, group, "realm", SAMPLE_W, SAMPLE_H,
                    seed -> realmSample(seed, r)));
        }
        Exhibit tiles = tilesExhibit(group);
        if (tiles != null) out.add(tiles);
        Exhibit walls = wallsExhibit(group);
        if (walls != null) out.add(walls);
        out.addAll(objectExhibits(group));
        if (realm >= 0) {
            for (int kind = 0; kind < RealmPoiPresets.COUNT; kind++) {
                if (RealmPoiPresets.realm(kind) != realm) continue;
                final int k = kind;
                out.add(new Exhibit(RealmPoiPresets.key(kind), group, "poi",
                        RealmPoiPresets.width(kind), RealmPoiPresets.height(kind),
                        seed -> RealmPoiPresets.build(k, new GameRandom(7919L * (k + 1)))));
            }
        }
        out.addAll(standalonePresets(group));
        return out;
    }

    /** Every preset class that is not a RealmPoiPresets kind, by the realm it stands in. */
    private static List<Exhibit> standalonePresets(String group) {
        List<Exhibit> out = new ArrayList<>();
        switch (group) {
            case "skyreach":
                out.add(new Exhibit("wardenspire", group, "preset", WardenSpirePreset.SIZE, WardenSpirePreset.SIZE,
                        seed -> {
                            Preset p = new WardenSpirePreset();
                            // Its stamp-time hook writes the quest anchor and
                            // spawns the Warden. Not in an exhibition.
                            p.customApplies.clear();
                            return p;
                        }));
                // The whole Spire Village as the world stamps it: the ring's
                // ground, the spire in the forecourt, the twelve houses at their
                // offsets. No residents -- SpireVillage.seatResidents seats them.
                int villageSize = SpireVillage.RADIUS * 2 + 1;
                out.add(new Exhibit("spirevillage", group, "preset", villageSize, villageSize,
                        seed -> {
                            int r = SpireVillage.RADIUS;
                            Preset p = SpireVillage.base();
                            Preset spire = new WardenSpirePreset();
                            spire.customApplies.clear();
                            p.applyPreset(r - WardenSpirePreset.SIZE / 2, r - WardenSpirePreset.SIZE / 2, spire);
                            for (SpireVillage.House house : SpireVillage.House.values()) {
                                p.applyPreset(r + house.offsetX, r + house.offsetY, house.preset());
                            }
                            return p;
                        }));
                break;
            case "steinfeld":
                out.add(new Exhibit("graveyard", group, "preset", GraveyardPreset.WIDTH, GraveyardPreset.HEIGHT,
                        seed -> new GraveyardPreset(new GameRandom(101))));
                out.add(new Exhibit("ruinedchapel", group, "preset", RuinedChapelPreset.WIDTH, RuinedChapelPreset.HEIGHT,
                        seed -> new RuinedChapelPreset(new GameRandom(102))));
                break;
            case "ghostrealm":
                out.add(new Exhibit("hauntedmanor", group, "preset", HauntedManorPreset.WIDTH, HauntedManorPreset.HEIGHT,
                        seed -> new HauntedManorPreset(new GameRandom(201))));
                out.add(new Exhibit("mausoleum", group, "preset", MausoleumPreset.WIDTH, MausoleumPreset.HEIGHT,
                        seed -> new MausoleumPreset(new GameRandom(202))));
                out.add(new Exhibit("sunkengraveyard", group, "preset", SunkenGraveyardPreset.WIDTH, SunkenGraveyardPreset.HEIGHT,
                        seed -> new SunkenGraveyardPreset(new GameRandom(203))));
                break;
            case "crookedbeyond":
                out.add(new Exhibit("crookedhouse", group, "preset", CrookedHousePreset.WIDTH, CrookedHousePreset.HEIGHT,
                        seed -> new CrookedHousePreset(new GameRandom(301))));
                out.add(new Exhibit("invertedhouse", group, "preset", InvertedHousePreset.WIDTH, InvertedHousePreset.HEIGHT,
                        seed -> new InvertedHousePreset(new GameRandom(302))));
                out.add(new Exhibit("longtable", group, "preset", LongTablePreset.WIDTH, LongTablePreset.HEIGHT,
                        seed -> new LongTablePreset(new GameRandom(303))));
                out.add(new Exhibit("dooryard", group, "preset", DoorYardPreset.WIDTH, DoorYardPreset.HEIGHT,
                        seed -> new DoorYardPreset(new GameRandom(304))));
                break;
            case SURFACE:
                out.add(new Exhibit("aeronautcamp", group, "preset", AeronautCampPreset.WIDTH, AeronautCampPreset.HEIGHT,
                        seed -> new AeronautCampPreset(new GameRandom(401))));
                out.add(new Exhibit("skyfragmentcrater", group, "preset", SkyFragmentCraterPreset.SIZE, SkyFragmentCraterPreset.SIZE,
                        seed -> new SkyFragmentCraterPreset(new GameRandom(402))));
                out.add(new Exhibit("skywardshrine", group, "preset", SkywardShrinePreset.SIZE, SkywardShrinePreset.SIZE,
                        seed -> new SkywardShrinePreset(new GameRandom(403))));
                break;
            default:
                break;
        }
        return out;
    }

    // ---------------------------------------------------------- gallery ----

    private static int id(String objectID) {
        return ObjectRegistry.getObjectID(objectID);
    }

    /** A gallery sign inside a preset, carrying {@code text} once applied. */
    static void presetSign(Preset p, int x, int y, String text) {
        int sign = id("sign");
        if (sign < 0) return;
        p.setObject(x, y, sign, 2);
        p.addCustomApply(x, y, 0, (level, levelX, levelY, dir, blackboard) -> {
            ObjectEntity entity = level.entityManager.getObjectEntity(levelX, levelY);
            if (entity instanceof SignObjectEntity) {
                ((SignObjectEntity) entity).setMessage(
                        new LocalMessage("misc", "swhshowroomsign", "name", text));
            }
            return null;
        });
    }

    private static List<Integer> modTiles(String group) {
        List<Integer> out = new ArrayList<>();
        for (int t = ShowroomRegistry.tileStart(); t < ShowroomRegistry.tileEnd(); t++) {
            if (group.equals(ShowroomRegistry.groupOfTile(t))) out.add(t);
        }
        return out;
    }

    /** Every mod tile of a group as a 3x3 patch with its ID on a sign below. */
    private static Exhibit tilesExhibit(String group) {
        List<Integer> tiles = modTiles(group);
        if (tiles.isEmpty()) return null;
        int cols = Math.min(10, tiles.size());
        int rows = (tiles.size() + cols - 1) / cols;
        int w = cols * 4;
        int h = rows * 5;
        return new Exhibit("tiles-" + group, group, "tiles", w, h, seed -> {
            Preset p = new Preset(w, h);
            for (int i = 0; i < tiles.size(); i++) {
                int sx = (i % cols) * 4;
                int sy = (i / cols) * 5;
                p.fillTile(sx, sy, 3, 3, tiles.get(i));
                presetSign(p, sx + 1, sy + 3, TileRegistry.getTileStringID(tiles.get(i)));
            }
            return p;
        });
    }

    /** Mod wall families of a group: {wall, door or -1, window or -1}. */
    private static List<int[]> wallFamilies(String group) {
        List<int[]> out = new ArrayList<>();
        for (int o = ShowroomRegistry.objectStart(); o < ShowroomRegistry.objectEnd(); o++) {
            if (!group.equals(ShowroomRegistry.groupOfObject(o))) continue;
            GameObject obj = ObjectRegistry.getObject(o);
            if (!(obj instanceof WallObject) || obj.getClass() != WallObject.class) continue;
            String sid = obj.getStringID();
            if (!sid.endsWith("wall")) continue;
            String prefix = sid.substring(0, sid.length() - 4);
            out.add(new int[]{o, id(prefix + "door"), id(prefix + "window")});
        }
        return out;
    }

    /**
     * Every mod wall family as a closed 7x5 room: two windows in the top wall
     * (a window may only sit mid-run in a straight wall — TECHNICAL_LEARNINGS),
     * a door in the bottom wall, vanilla wood floor inside.
     */
    private static Exhibit wallsExhibit(String group) {
        List<int[]> families = wallFamilies(group);
        if (families.isEmpty()) return null;
        int cols = Math.min(5, families.size());
        int rows = (families.size() + cols - 1) / cols;
        int w = cols * 9;
        int h = rows * 9;
        return new Exhibit("walls-" + group, group, "walls", w, h, seed -> {
            Preset p = new Preset(w, h);
            int floor = TileRegistry.getTileID("woodfloor");
            for (int i = 0; i < families.size(); i++) {
                int[] f = families.get(i);
                int ox = (i % cols) * 9 + 1;
                int oy = (i / cols) * 9 + 1;
                String[] plan = {"WWNWNWW", "W.....W", "W.....W", "W.....W", "WWWDWWW"};
                for (int y = 0; y < plan.length; y++) {
                    for (int x = 0; x < plan[y].length(); x++) {
                        char c = plan[y].charAt(x);
                        int obj = f[0];
                        if (c == 'N' && f[2] > 0) obj = f[2];
                        if (c == 'D' && f[1] > 0) obj = f[1];
                        if (c == '.') {
                            if (floor >= 0) p.setTile(ox + x, oy + y, floor);
                            p.setObject(ox + x, oy + y, 0);
                        } else {
                            p.setObject(ox + x, oy + y, obj, 0);
                        }
                    }
                }
                presetSign(p, ox + 3, oy + 6, ObjectRegistry.getObjectStringID(f[0]));
            }
            return p;
        });
    }

    /** Whether an object is shown in the object gallery (and not as a wall/part). */
    private static boolean galleryObject(GameObject obj) {
        if (obj == null || obj.getID() <= 0) return false;
        if (obj instanceof WallObject || obj instanceof WallDoorObject) return false;
        if (obj instanceof DoorObject && !(obj instanceof FenceGateObject)) return false;
        if (!obj.isMultiTileMaster()) return false;
        String sid = obj.getStringID();
        // The open/unlocked twin of a gate or door: the closed one is shown.
        for (String suffix : new String[]{"open", "unlocked", "locked"}) {
            if (sid.endsWith(suffix) && sid.length() > suffix.length()
                    && ObjectRegistry.getObjectID(sid.substring(0, sid.length() - suffix.length())) >= 0) {
                return false;
            }
        }
        return true;
    }

    /** Front-facing rotation: every gallery piece is shown as the player walks up to it. */
    static final int GALLERY_ROTATION = 2;

    private static final class Slot {
        final int objectID;
        final MultiTile multiTile;
        int x;
        int y;

        Slot(int objectID, MultiTile multiTile) {
            this.objectID = objectID;
            this.multiTile = multiTile;
        }

        int w() {
            return multiTile.width;
        }

        int h() {
            return multiTile.height;
        }
    }

    /**
     * Every mod object of a group once, row-packed, each with its string ID on
     * a sign directly below it. Split into several exhibits so no single one
     * outgrows a screenshot.
     */
    private static List<Exhibit> objectExhibits(String group) {
        List<Slot> slots = new ArrayList<>();
        for (int o = ShowroomRegistry.objectStart(); o < ShowroomRegistry.objectEnd(); o++) {
            if (!group.equals(ShowroomRegistry.groupOfObject(o))) continue;
            GameObject obj = ObjectRegistry.getObject(o);
            if (!galleryObject(obj)) continue;
            slots.add(new Slot(o, obj.getMultiTile(GALLERY_ROTATION)));
        }
        List<Exhibit> out = new ArrayList<>();
        final int maxW = 44;
        final int maxH = 34;
        List<Slot> current = new ArrayList<>();
        int cx = 0;
        int cy = 0;
        int rowH = 0;
        int usedW = 0;
        int part = 1;
        for (Slot s : slots) {
            int sw = s.w() + 2;
            int sh = s.h() + 5;
            if (cx > 0 && cx + sw > maxW) {
                cy += rowH;
                cx = 0;
                rowH = 0;
            }
            if (cy > 0 && cy + sh > maxH) {
                out.add(objectExhibit(group, part++, current, usedW, cy + rowH));
                current = new ArrayList<>();
                cx = 0;
                cy = 0;
                rowH = 0;
                usedW = 0;
            }
            // headroom 3 above the footprint for tall sprites, sign below it
            s.x = cx + 1;
            s.y = cy + 3;
            current.add(s);
            cx += sw;
            usedW = Math.max(usedW, cx);
            rowH = Math.max(rowH, sh);
        }
        if (!current.isEmpty()) {
            out.add(objectExhibit(group, part, current, usedW, cy + rowH));
        }
        return out;
    }

    private static Exhibit objectExhibit(String group, int part, List<Slot> slots, int w, int h) {
        String id = "objects-" + group + (part > 1 ? "-" + part : "");
        return new Exhibit(id, group, "objects", Math.max(w, 1), Math.max(h, 1), seed -> {
            Preset p = new Preset(Math.max(w, 1), Math.max(h, 1));
            for (Slot s : slots) {
                placeGalleryObject(p, s);
            }
            return p;
        });
    }

    private static void placeGalleryObject(Preset p, Slot s) {
        GameObject obj = ObjectRegistry.getObject(s.objectID);
        MultiTile mt = s.multiTile;
        int masterX = s.x + mt.x;
        int masterY = s.y + mt.y;
        LinkedHashSet<Integer> layers = obj.getValidObjectLayers();
        int layer = ObjectLayerRegistry.BASE_LAYER;
        int rotation = GALLERY_ROTATION;
        if (!layers.contains(ObjectLayerRegistry.BASE_LAYER)) {
            if (layers.contains(ObjectLayerRegistry.WALL_DECOR)) {
                // Wall decor points at the wall it hangs on (rotation 2 = wall
                // above) and needs one there.
                layer = ObjectLayerRegistry.WALL_DECOR;
                int wall = id("stonewall");
                if (wall > 0) p.setObject(masterX, masterY - 1, wall, 0);
                rotation = 2;
            } else if (layers.contains(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR)) {
                // A table decoration needs a DecorationHolder under it; a
                // modular table is one (TECHNICAL_LEARNINGS, furnishing pass).
                layer = ObjectLayerRegistry.FENCE_AND_TABLE_DECOR;
                int table = id("oakmodulartable");
                if (table > 0) p.setObject(masterX, masterY, table, 0);
                rotation = 0;
            } else if (layers.contains(ObjectLayerRegistry.TILE_LAYER)) {
                layer = ObjectLayerRegistry.TILE_LAYER;
            } else if (!layers.isEmpty()) {
                layer = layers.iterator().next();
            }
        }
        final int fl = layer;
        final int rot = rotation;
        mt.streamIDs(masterX, masterY).forEach(c -> {
            if (c.tileX >= 0 && c.tileY >= 0 && c.tileX < p.width && c.tileY < p.height) {
                p.setObjectLayer(fl, c.tileX, c.tileY, c.value, rot);
            }
        });
        int signY = s.y + mt.height;
        if (signY < p.height) {
            presetSign(p, s.x, signY, obj.getStringID());
        }
    }

    // ------------------------------------------------------ realm sample ----

    /**
     * One screen of a realm's real ground and natural objects, read from
     * {@code SkyTerrainPainter.describeTile} — the pure function
     * {@code paintRegion} writes into the world — at the realm's landing point.
     * Nothing is read from or written to the real world to make it.
     */
    static Preset realmSample(int seed, int realm) {
        Point site = sampleSite(seed, realm);
        int ox = SkyOrigin.originX(seed);
        int oy = SkyOrigin.originY(seed);
        int sx = site.x - SAMPLE_W / 2;
        int sy = site.y - SAMPLE_H / 2;
        Preset p = new Preset(SAMPLE_W, SAMPLE_H);
        boolean[][] taken = new boolean[SAMPLE_W][SAMPLE_H];
        for (int i = 0; i < SAMPLE_W; i++) {
            for (int j = 0; j < SAMPLE_H; j++) {
                long desc = SkyTerrainPainter.describeTile(seed, sx + i, sy + j, ox, oy);
                p.setTile(i, j, SkyTerrainPainter.descTile(desc));
                if (!taken[i][j]) p.setObject(i, j, 0);
            }
        }
        for (int i = 0; i < SAMPLE_W; i++) {
            for (int j = 0; j < SAMPLE_H; j++) {
                if (taken[i][j]) continue;
                long desc = SkyTerrainPainter.describeTile(seed, sx + i, sy + j, ox, oy);
                int obj = SkyTerrainPainter.descObject(desc);
                if (obj <= 0) continue;
                GameObject go = ObjectRegistry.getObject(obj);
                if (go == null) continue;
                MultiTile mt = go.getMultiTile(0);
                boolean fits = true;
                List<int[]> parts = new ArrayList<>();
                final int fi = i;
                final int fj = j;
                mt.streamIDs(fi, fj).forEach(c -> parts.add(new int[]{c.tileX, c.tileY, c.value}));
                for (int[] c : parts) {
                    // The painter writes a multi-tile's far half itself; one
                    // that would leave the sample, or land on a half already
                    // written, is dropped whole, as the painter drops it.
                    if (c[0] < 0 || c[1] < 0 || c[0] >= SAMPLE_W || c[1] >= SAMPLE_H || taken[c[0]][c[1]]) {
                        fits = false;
                        break;
                    }
                }
                if (!fits) continue;
                for (int[] c : parts) {
                    p.setObject(c[0], c[1], c[2], 0);
                    taken[c[0]][c[1]] = true;
                }
            }
        }
        return p;
    }

    /**
     * The landing point of the realm with the most land and the most standing
     * objects around it, out of eight directions from the spire. A landing is
     * open ground by definition; the score is there so the sample shows the
     * realm's ground and flora rather than a strip of coast.
     */
    static Point sampleSite(int seed, int realm) {
        int ox = SkyOrigin.originX(seed);
        int oy = SkyOrigin.originY(seed);
        Point best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int a = 0; a < 8; a++) {
            double angle = a * Math.PI / 4.0;
            Point p = RealmLanding.find(seed, realm, ox + (int) Math.round(Math.cos(angle) * 1000),
                    oy + (int) Math.round(Math.sin(angle) * 1000));
            int score = 0;
            for (int i = -SAMPLE_W / 2; i < SAMPLE_W / 2; i += 2) {
                for (int j = -SAMPLE_H / 2; j < SAMPLE_H / 2; j += 2) {
                    long d = SkyTerrainPainter.describeTile(seed, p.x + i, p.y + j, ox, oy);
                    GameTile t = TileRegistry.getTile(SkyTerrainPainter.descTile(d));
                    if (t != null && !t.isLiquid) score += 2;
                    if (SkyTerrainPainter.descObject(d) != 0) score += 3;
                    if (RealmDepth.realmAt(seed, p.x + i, p.y + j, ox, oy) != realm) score -= 4;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    /** The most common dry tile of the realm sample — the realm's ground. */
    static int groundOf(int seed, String group) {
        if (SURFACE.equals(group)) return TileRegistry.getTileID("grasstile");
        int realm = realmOfGroup(group);
        if (realm < 0) return SkyRegistry.cloudturfID;
        Preset sample = realmSample(seed, realm);
        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < sample.width; i++) {
            for (int j = 0; j < sample.height; j++) {
                int t = sample.getTile(i, j);
                GameTile tile = t >= 0 ? TileRegistry.getTile(t) : null;
                if (tile == null || tile.isLiquid) continue;
                counts.merge(t, 1, Integer::sum);
            }
        }
        int best = SkyRegistry.cloudturfID;
        int bestN = -1;
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestN) {
                bestN = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    // ======================================================================
    // World operations (server side)
    // ======================================================================

    public static final class Report {
        public final List<String> lines = new ArrayList<>();
        public int exhibits;
        public int expected;
        public int missing;
        public int signsOk;
        public int mobs;

        void add(String line) {
            lines.add(line);
        }
    }

    private static void loadAll(Level level, Rectangle r) {
        level.regionManager.ensureTilesAreLoaded(r.x, r.y, r.x + r.width - 1, r.y + r.height - 1);
    }

    /** Removes every mob and dropped item standing in the rectangle; players are never mobs. */
    static int clearEntities(Level level, Rectangle r) {
        int removed = 0;
        int rx0 = level.regionManager.getRegionCoordByTile(r.x);
        int ry0 = level.regionManager.getRegionCoordByTile(r.y);
        int rx1 = level.regionManager.getRegionCoordByTile(r.x + r.width - 1);
        int ry1 = level.regionManager.getRegionCoordByTile(r.y + r.height - 1);
        for (int rx = rx0; rx <= rx1; rx++) {
            for (int ry = ry0; ry <= ry1; ry++) {
                List<Mob> mobs = new ArrayList<>();
                for (Mob m : level.entityManager.mobs.getInRegion(rx, ry)) mobs.add(m);
                for (Mob m : mobs) {
                    if (r.contains(m.getTileX(), m.getTileY())) {
                        m.remove();
                        removed++;
                    }
                }
                List<PickupEntity> pickups = new ArrayList<>();
                for (PickupEntity pe : level.entityManager.pickups.getInRegion(rx, ry)) pickups.add(pe);
                for (PickupEntity pe : pickups) pe.remove();
            }
        }
        return removed;
    }

    static int countMobs(Level level, Rectangle r) {
        int n = 0;
        int rx0 = level.regionManager.getRegionCoordByTile(r.x);
        int ry0 = level.regionManager.getRegionCoordByTile(r.y);
        int rx1 = level.regionManager.getRegionCoordByTile(r.x + r.width - 1);
        int ry1 = level.regionManager.getRegionCoordByTile(r.y + r.height - 1);
        for (int rx = rx0; rx <= rx1; rx++) {
            for (int ry = ry0; ry <= ry1; ry++) {
                for (Mob m : level.entityManager.mobs.getInRegion(rx, ry)) {
                    if (!m.removed() && r.contains(m.getTileX(), m.getTileY())) n++;
                }
            }
        }
        return n;
    }

    /**
     * Writes a tile through its region with {@code forceDontUpdate}, the way
     * {@code ClearAreaServerCommand} does. {@code Level.setTile} would also
     * recompute liquid, light, splatting and settlement room stats per tile;
     * over the showroom's ~170,000 tiles that cost a minute of server thread
     * (measured: build totalMs=68871 through Level.setTile). The caches are
     * rebuilt once per region afterwards by {@link #refreshRegions}.
     */
    static void setTileFast(Level level, int x, int y, int tile) {
        Region region = level.regionManager.getRegion(
                level.regionManager.getRegionCoordByTile(x), level.regionManager.getRegionCoordByTile(y), true);
        if (region == null) {
            level.setTile(x, y, tile);
            return;
        }
        region.tileLayer.setTileByRegion(x - region.tileXOffset, y - region.tileYOffset, tile, true);
    }

    /** Flat base floor, no objects, no wires, showroom biome, over the whole rectangle. */
    static void flatten(Level level, Rectangle r, int baseTile, int biome) {
        int layers = ObjectLayerRegistry.getTotalLayers();
        for (int x = r.x; x < r.x + r.width; x++) {
            for (int y = r.y; y < r.y + r.height; y++) {
                Region region = level.regionManager.getRegion(
                        level.regionManager.getRegionCoordByTile(x), level.regionManager.getRegionCoordByTile(y), true);
                int rx = x - region.tileXOffset;
                int ry = y - region.tileYOffset;
                if (level.getTileID(x, y) != baseTile) {
                    region.tileLayer.setTileByRegion(rx, ry, baseTile, true);
                }
                for (int layer = 0; layer < layers; layer++) {
                    if (level.getObjectID(layer, x, y) != 0) {
                        region.objectLayer.setObjectByRegion(layer, rx, ry, 0, true);
                    }
                    if (level.getObjectRotation(layer, x, y) != 0) {
                        level.objectLayer.setObjectRotation(layer, x, y, 0);
                    }
                }
                if (level.regionManager.getWireData(x, y) != 0) {
                    level.regionManager.setWireData(x, y, (byte) 0);
                }
                level.logicLayer.clearLogicGate(x, y);
                if (biome >= 0 && level.getBiomeID(x, y) != biome) {
                    region.biomeLayer.setBiomeByRegion(rx, ry, biome, true);
                }
            }
        }
    }

    /** Recomputes the region caches the way ClearAreaServerCommand does and resends them. */
    static void refreshRegions(Server server, Level level, Rectangle r) {
        int rx0 = level.regionManager.getRegionCoordByTile(r.x);
        int ry0 = level.regionManager.getRegionCoordByTile(r.y);
        int rx1 = level.regionManager.getRegionCoordByTile(r.x + r.width - 1);
        int ry1 = level.regionManager.getRegionCoordByTile(r.y + r.height - 1);
        for (int rx = rx0; rx <= rx1; rx++) {
            for (int ry = ry0; ry <= ry1; ry++) {
                Region region = level.regionManager.getRegion(rx, ry, false);
                if (region == null) continue;
                region.updateLiquidManager();
                region.updateSplattingManager();
                region.updateSubRegions();
                region.updateLight();
                if (server == null) continue;
                final int frx = rx;
                final int fry = ry;
                PacketRegionData packet = null;
                for (ServerClient c : (Iterable<ServerClient>) server.streamClients()::iterator) {
                    if (c.hasRegionLoaded(level, frx, fry)) {
                        if (packet == null) packet = new PacketRegionData(region);
                        c.sendPacket(packet);
                    }
                }
            }
        }
    }

    public static boolean isBuilt(Level level) {
        Layout layout = layout();
        if (layout.exhibits.isEmpty()) return false;
        Exhibit first = layout.exhibits.get(0);
        if (!level.regionManager.isTileLoaded(first.signX(), first.signY())
                && !level.regionManager.isRegionGenerated(
                level.regionManager.getRegionCoordByTile(first.signX()),
                level.regionManager.getRegionCoordByTile(first.signY()))) {
            return false;
        }
        level.regionManager.ensureTileIsLoaded(first.signX(), first.signY());
        return level.getObjectID(first.signX(), first.signY()) == id("sign");
    }

    /** Builds (or rebuilds) the whole showroom. Idempotent. */
    public static Report build(Server server, SkyLevel level) {
        Report report = new Report();
        Layout layout = layout();
        int seed = level.getWorldGenSeed();
        Rectangle b = layout.bounds;
        long t0 = System.currentTimeMillis();
        loadAll(level, b);
        long t1 = System.currentTimeMillis();
        int base = TileRegistry.getTileID("stonefloor");
        if (base < 0) base = SkyRegistry.cloudturfID;
        int biome = ShowroomBiome.biomeID();
        flatten(level, b, base, biome);
        int removed = clearEntities(level, b);

        Map<String, Integer> grounds = new HashMap<>();
        for (String g : GROUPS) grounds.put(g, groundOf(seed, g));

        int sign = id("sign");
        int lamp = id("ironstreetlamp");
        for (Exhibit e : layout.exhibits) {
            int ground = grounds.getOrDefault(e.group, base);
            Rectangle cell = e.cellRect();
            for (int x = cell.x + 1; x < cell.x + cell.width - 1; x++) {
                for (int y = cell.y + 1; y < cell.y + cell.height - 1; y++) {
                    setTileFast(level, x, y, ground);
                }
            }
            Preset p = e.factory.build(seed);
            p.applyToLevel(level, e.x, e.y);
            if (sign > 0) {
                ObjectRegistry.getObject(sign).placeObject(level, 0, e.signX(), e.signY(), 2, false);
                ObjectEntity entity = level.entityManager.getObjectEntity(e.signX(), e.signY());
                if (entity instanceof SignObjectEntity) {
                    ((SignObjectEntity) entity).setMessage(
                            new LocalMessage("misc", "swhshowroomsign", "name", e.id));
                }
            }
            if (lamp > 0) {
                GameObject lampObj = ObjectRegistry.getObject(lamp);
                int lx0 = cell.x + 1;
                int ly0 = cell.y + 1;
                int lx1 = cell.x + cell.width - 2;
                int ly1 = cell.y + cell.height - 2;
                for (int[] c : new int[][]{{lx0, ly0}, {lx1, ly0}, {lx0, ly1}, {lx1, ly1},
                        {e.signX() - 2, e.signY()}, {e.signX() + 2, e.signY()}}) {
                    if (level.getObjectID(c[0], c[1]) == 0) {
                        lampObj.placeObject(level, 0, c[0], c[1], 0, false);
                    }
                }
            }
            report.exhibits++;
        }
        // Anything a preset's own hooks put down (none today) goes too.
        removed += clearEntities(level, b);
        refreshRegions(server, level, b);
        report.add("SHOWROOM build: exhibits=" + report.exhibits + " bounds=" + b.x + "," + b.y + " "
                + b.width + "x" + b.height + " regionsLoadedMs=" + (t1 - t0)
                + " totalMs=" + (System.currentTimeMillis() - t0) + " mobsRemoved=" + removed);
        return report;
    }

    /** Flattens the showroom rectangle to bare floor. Touches nothing outside it. */
    public static Report clear(Server server, SkyLevel level) {
        Report report = new Report();
        Rectangle b = layout().bounds;
        loadAll(level, b);
        int base = TileRegistry.getTileID("stonefloor");
        if (base < 0) base = SkyRegistry.cloudturfID;
        flatten(level, b, base, ShowroomBiome.biomeID());
        int removed = clearEntities(level, b);
        refreshRegions(server, level, b);
        report.add("SHOWROOM clear: bounds=" + b.x + "," + b.y + " " + b.width + "x" + b.height
                + " mobsRemoved=" + removed);
        return report;
    }

    /**
     * Rebuilds every exhibit's preset from its fixed seed and compares it,
     * object layer by object layer, with what is standing in the world.
     */
    public static Report check(SkyLevel level) {
        Report report = new Report();
        Layout layout = layout();
        int seed = level.getWorldGenSeed();
        loadAll(level, layout.bounds);
        int sign = id("sign");
        int layers = ObjectLayerRegistry.getTotalLayers();
        for (Exhibit e : layout.exhibits) {
            Preset p = e.factory.build(seed);
            int expected = 0;
            int placed = 0;
            List<String> missing = new ArrayList<>();
            for (int i = 0; i < p.width; i++) {
                for (int j = 0; j < p.height; j++) {
                    for (int layer = 0; layer < layers; layer++) {
                        int want = p.getObject(layer, i, j);
                        if (want <= 0) continue;
                        expected++;
                        int got = level.getObjectID(layer, e.x + i, e.y + j);
                        if (got == want) {
                            placed++;
                        } else if (missing.size() < 6) {
                            missing.add(ObjectRegistry.getObjectStringID(want) + "@" + i + "," + j + "/L" + layer
                                    + (got > 0 ? "(found " + ObjectRegistry.getObjectStringID(got) + ")" : ""));
                        }
                    }
                }
            }
            boolean signOk = level.getObjectID(e.signX(), e.signY()) == sign;
            if (signOk) report.signsOk++;
            report.exhibits++;
            report.expected += expected;
            report.missing += expected - placed;
            report.add("SHOWROOM exhibit=" + e.id + " group=" + e.group + " kind=" + e.kind
                    + " at=" + e.x + "," + e.y + " size=" + e.width + "x" + e.height
                    + " expected=" + expected + " placed=" + placed + " missing=" + (expected - placed)
                    + " sign=" + (signOk ? "ok" : "MISSING")
                    + (missing.isEmpty() ? "" : " first=" + String.join(";", missing)));
        }
        report.mobs = countMobs(level, layout.bounds);
        report.add("SHOWROOM_CHECK exhibits=" + report.exhibits + " expected=" + report.expected
                + " missing=" + report.missing + " signs=" + report.signsOk + "/" + report.exhibits
                + " mobs=" + report.mobs);
        return report;
    }

    // ======================================================================
    // Export (for tools/preset_render.py)
    // ======================================================================

    private static String json(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
            else b.append(c);
        }
        return b.append('"').toString();
    }

    /**
     * Writes, per exhibit, what is standing in the world over its capture
     * rectangle — every tile and every object layer with rotation — plus one
     * {@code registry.json} describing each tile and object that appears:
     * class chain, texture-name fields, multi-tile shape and map colour. The
     * offline renderer draws from exactly this, so it renders what the
     * showroom really holds, engine deletions included.
     */
    public static Report export(SkyLevel level, File dir) throws IOException {
        Report report = new Report();
        Layout layout = layout();
        loadAll(level, layout.bounds);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
        }
        int layers = ObjectLayerRegistry.getTotalLayers();
        TreeSet<Integer> usedTiles = new TreeSet<>();
        TreeSet<Integer> usedObjects = new TreeSet<>();
        StringBuilder index = new StringBuilder("[\n");
        boolean firstIndex = true;
        for (Exhibit e : layout.exhibits) {
            Rectangle r = e.captureRect();
            StringBuilder out = new StringBuilder();
            out.append("{\"id\":").append(json(e.id))
                    .append(",\"group\":").append(json(e.group))
                    .append(",\"kind\":").append(json(e.kind))
                    .append(",\"x\":").append(r.x).append(",\"y\":").append(r.y)
                    .append(",\"w\":").append(r.width).append(",\"h\":").append(r.height)
                    .append(",\"footprint\":[").append(e.x - r.x).append(',').append(e.y - r.y).append(',')
                    .append(e.width).append(',').append(e.height).append(']')
                    .append(",\"tiles\":[");
            for (int j = 0; j < r.height; j++) {
                out.append(j == 0 ? "[" : ",[");
                for (int i = 0; i < r.width; i++) {
                    int t = level.getTileID(r.x + i, r.y + j);
                    usedTiles.add(t);
                    out.append(i == 0 ? "" : ",").append(t);
                }
                out.append(']');
            }
            out.append("],\"objects\":[");
            boolean first = true;
            for (int j = 0; j < r.height; j++) {
                for (int i = 0; i < r.width; i++) {
                    for (int layer = 0; layer < layers; layer++) {
                        int o = level.getObjectID(layer, r.x + i, r.y + j);
                        if (o <= 0) continue;
                        usedObjects.add(o);
                        out.append(first ? "" : ",").append('[').append(i).append(',').append(j).append(',')
                                .append(layer).append(',').append(o).append(',')
                                .append(level.getObjectRotation(layer, r.x + i, r.y + j)).append(']');
                        first = false;
                    }
                }
            }
            // Sign texts, so an offline render can name what each sign labels.
            out.append("],\"signs\":[");
            boolean firstSign = true;
            for (int j = 0; j < r.height; j++) {
                for (int i = 0; i < r.width; i++) {
                    ObjectEntity entity = level.entityManager.getObjectEntity(r.x + i, r.y + j);
                    if (!(entity instanceof SignObjectEntity)) continue;
                    necesse.engine.localization.message.GameMessage msg =
                            ((SignObjectEntity) entity).getSignMessage();
                    String text = msg == null ? "" : msg.translate();
                    out.append(firstSign ? "" : ",").append('[').append(i).append(',').append(j).append(',')
                            .append(json(text)).append(']');
                    firstSign = false;
                }
            }
            out.append("]}\n");
            write(new File(dir, e.id + ".json"), out.toString());
            index.append(firstIndex ? "" : ",\n").append(json(e.id));
            firstIndex = false;
            report.exhibits++;
        }
        index.append("\n]\n");
        write(new File(dir, "index.json"), index.toString());
        write(new File(dir, "registry.json"), registryJson(usedTiles, usedObjects));
        report.add("SHOWROOM export: exhibits=" + report.exhibits + " tiles=" + usedTiles.size()
                + " objects=" + usedObjects.size() + " dir=" + dir.getAbsolutePath());
        return report;
    }

    private static void write(File f, String text) throws IOException {
        try (Writer w = new OutputStreamWriter(new java.io.FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(text);
        }
    }

    private static String classChain(Object o) {
        StringBuilder b = new StringBuilder("[");
        Class<?> c = o.getClass();
        boolean first = true;
        while (c != null && c != Object.class) {
            b.append(first ? "" : ",").append(json(c.getSimpleName().isEmpty() ? c.getName() : c.getSimpleName()));
            first = false;
            c = c.getSuperclass();
        }
        return b.append(']').toString();
    }

    /** Every String field whose name mentions a texture, up the class chain. */
    private static String textureFields(Object o) {
        Map<String, String> found = new LinkedHashMap<>();
        Class<?> c = o.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != String.class || java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                String n = f.getName().toLowerCase();
                if (!n.contains("texture") && !n.contains("sprite")) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(o);
                    if (v != null && !found.containsKey(f.getName())) found.put(f.getName(), v.toString());
                } catch (Throwable ignored) {
                    // a field we cannot read is a field we do not report
                }
            }
            c = c.getSuperclass();
        }
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : found.entrySet()) {
            b.append(first ? "" : ",").append(json(e.getKey())).append(':').append(json(e.getValue()));
            first = false;
        }
        return b.append('}').toString();
    }

    private static String color(java.awt.Color c) {
        return c == null ? "null" : "[" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "]";
    }

    private static String registryJson(TreeSet<Integer> tiles, TreeSet<Integer> objects) {
        StringBuilder b = new StringBuilder("{\"layers\":{");
        for (int layer = 0; layer < ObjectLayerRegistry.getTotalLayers(); layer++) {
            b.append(layer == 0 ? "" : ",").append(json(String.valueOf(layer))).append(':')
                    .append(json(ObjectLayerRegistry.getLayerStringID(layer)));
        }
        b.append("},\n\"tiles\":{");
        boolean first = true;
        for (int t : tiles) {
            GameTile tile = TileRegistry.getTile(t);
            if (tile == null) continue;
            b.append(first ? "\n" : ",\n").append(json(String.valueOf(t))).append(":{")
                    .append("\"id\":").append(json(tile.getStringID()))
                    .append(",\"mod\":").append(ShowroomRegistry.groupOfTile(t) != null)
                    .append(",\"liquid\":").append(tile.isLiquid)
                    .append(",\"classes\":").append(classChain(tile))
                    .append(",\"textures\":").append(textureFields(tile))
                    .append(",\"color\":").append(color(tile.mapColor)).append('}');
            first = false;
        }
        b.append("\n},\"objects\":{");
        first = true;
        for (int o : objects) {
            GameObject obj = ObjectRegistry.getObject(o);
            if (obj == null) continue;
            StringBuilder mts = new StringBuilder("[");
            for (int rot = 0; rot < 4; rot++) {
                MultiTile mt = obj.getMultiTile(rot);
                mts.append(rot == 0 ? "" : ",").append('[').append(mt.x).append(',').append(mt.y).append(',')
                        .append(mt.width).append(',').append(mt.height).append(',').append(mt.isMaster).append(']');
            }
            mts.append(']');
            b.append(first ? "\n" : ",\n").append(json(String.valueOf(o))).append(":{")
                    .append("\"id\":").append(json(obj.getStringID()))
                    .append(",\"mod\":").append(ShowroomRegistry.groupOfObject(o) != null)
                    .append(",\"classes\":").append(classChain(obj))
                    .append(",\"textures\":").append(textureFields(obj))
                    .append(",\"multitile\":").append(mts)
                    .append(",\"solid\":").append(obj.isSolid)
                    .append(",\"color\":").append(color(obj.mapColor)).append('}');
            first = false;
        }
        b.append("\n}}\n");
        return b.toString();
    }

    /** The exhibit IDs, for {@code list} and autocompletion. */
    public static List<String> ids() {
        List<String> out = new ArrayList<>(layout().byId.keySet());
        return Collections.unmodifiableList(out);
    }
}
