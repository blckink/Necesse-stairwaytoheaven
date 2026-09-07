package stairwaytoheaven.worldgen.pois;

import java.util.Arrays;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.SignObjectEntity;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.quest.SkywatchWorldData;

/**
 * The fourteen authored Skyreach places of
 * {@code docs/design/chapter-01-skyreach-pois.md}, built from that dossier's own
 * room plans.
 *
 * <h2>The plan IS the source</h2>
 * Every POI below carries the dossier's ASCII plan verbatim as a {@code String[]}
 * plus its own legend. Nothing is re-drawn in coordinates: a reviewer can hold
 * this file beside §2.1–§2.14 and compare character for character, and
 * {@link #width(int)}/{@link #height(int)} are READ OFF the plan rather than
 * declared beside it, so a mistyped row cannot silently shift a building. A row
 * longer than the plan's first row throws at load, because
 * {@link RealmPoiWorldPreset#onRegistryClosed} builds all fourteen.
 *
 * <h2>What a blank cell means</h2>
 * {@code '.'} writes NOTHING — {@code Preset.applyToLevel} skips a tile or
 * object of {@code -1}, which is what a fresh {@code Preset} holds. That is the
 * opposite of {@link RealmPoiPresets}'s {@code blank()}, which clears its whole
 * rectangle to object 0 and so bulldozes the terrain painter's own flora. These
 * places are mostly deliberate empty space (the dossier counts 425 untouched
 * tiles in the Institute alone, 352 in the Anvil, 502 of open Mistsea in the
 * Reef), and that space has to stay the meadow, the crag or the cloud sea the
 * painter grew.
 *
 * <h2>Six props the dossier asked for do not exist</h2>
 * §4 orders {@code skywatchstele}, {@code cloudspringfont}, {@code prismchime},
 * {@code skywaywaystone}, {@code sovereignaltar} and {@code fermentationvat} as
 * new art. None is registered. Rather than ship six error textures — a release
 * blocker under {@code IMPLEMENTATION_RULES.md} §5 — each is stood in for by a
 * registered object that already carries art, locale and behaviour, in the
 * dossier's own "reuse before you request" spirit (§1) and its own stated
 * fallback for the chimes (§2.9). The substitutions are listed in
 * {@code docs/design/realm-poi-worldgen.md} and in {@code state/decisions.json}.
 *
 * <table>
 *   <tr><th>asked for</th><th>stands in</th><th>why that one</th></tr>
 *   <tr><td>{@code skywatchstele}</td><td>vanilla {@code sign}</td>
 *       <td>a stele's whole job is to be read, and a sign really is readable —
 *           {@code SignObjectEntity.setMessage}, the pattern
 *           {@code AeronautCampPreset} already uses</td></tr>
 *   <tr><td>{@code cloudspringfont}</td><td>{@code starfall}</td>
 *       <td>the lit centrepiece the plans use it as</td></tr>
 *   <tr><td>{@code prismchime}</td><td>{@code chargecrystal}</td>
 *       <td>§2.9's own fallback: a lit crystal column</td></tr>
 *   <tr><td>{@code skywaywaystone}</td><td>vanilla {@code waystone}</td>
 *       <td>§3.3's fast-travel network, working, for nothing</td></tr>
 *   <tr><td>{@code sovereignaltar}</td><td>{@code skywatchdisplay}</td>
 *       <td>a pedestal; the Sovereign chain is ROADMAP v0.6, and §2.6 forbids
 *           shipping an altar with a summon it cannot honour</td></tr>
 *   <tr><td>{@code fermentationvat}</td><td>vanilla {@code cheesepress}</td>
 *       <td>the cast brief's own archetype for it</td></tr>
 * </table>
 */
public final class SkyreachPoiPresets {

    public static final int WAYSIDE_SHRINE = 0;
    public static final int SHEPHERDS_FOLD = 1;
    public static final int FALLING_INSTITUTE = 2;
    public static final int NIGHTFELL_REDOUBT = 3;
    public static final int AETHER_MANUFACTORY = 4;
    public static final int SOVEREIGNS_ANVIL = 5;
    public static final int PASSAGE_WAYHOUSE = 6;
    public static final int UNOPENED_GATE = 7;
    public static final int PRISM_CHOIR = 8;
    public static final int SERPENTS_REEF = 9;
    public static final int DEWKEEPERS_HUT = 10;
    public static final int SKYWAY_TOLLHOUSE = 11;
    public static final int GRANGE_CELLAR = 12;
    public static final int TEST_RANGE = 13;
    public static final int COUNT = 14;

    /** Furniture rotation is the direction the piece FACES (dossier §0.2). */
    private static final int UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3;
    /** Wall-decor rotation is where the WALL is (dossier §0.2). */
    private static final int WALL_BELOW = 0, WALL_LEFT = 1, WALL_ABOVE = 2, WALL_RIGHT = 3;

    private SkyreachPoiPresets() {
    }

    public static String key(int kind) {
        switch (kind) {
            case WAYSIDE_SHRINE: return "waysideshrine";
            case SHEPHERDS_FOLD: return "shepherdsfold";
            case FALLING_INSTITUTE: return "fallinginstitute";
            case NIGHTFELL_REDOUBT: return "nightfellredoubt";
            case AETHER_MANUFACTORY: return "aethermanufactory";
            case SOVEREIGNS_ANVIL: return "sovereignsanvil";
            case PASSAGE_WAYHOUSE: return "passagewayhouse";
            case UNOPENED_GATE: return "unopenedgate";
            case PRISM_CHOIR: return "prismchoir";
            case SERPENTS_REEF: return "serpentsreef";
            case DEWKEEPERS_HUT: return "dewkeepershut";
            case SKYWAY_TOLLHOUSE: return "skywaytollhouse";
            case GRANGE_CELLAR: return "grangecellar";
            case TEST_RANGE: return "stormveiltestrange";
            default: throw new IllegalArgumentException("Unknown Skyreach POI " + kind);
        }
    }

    /** The plan itself, so size and content can never disagree. */
    private static String[] plan(int kind) {
        switch (kind) {
            case WAYSIDE_SHRINE: return WAYSIDE;
            case SHEPHERDS_FOLD: return FOLD;
            case FALLING_INSTITUTE: return INSTITUTE;
            case NIGHTFELL_REDOUBT: return REDOUBT;
            case AETHER_MANUFACTORY: return MANUFACTORY;
            case SOVEREIGNS_ANVIL: return ANVIL;
            case PASSAGE_WAYHOUSE: return WAYHOUSE;
            case UNOPENED_GATE: return GATE;
            case PRISM_CHOIR: return CHOIR;
            case SERPENTS_REEF: return REEF;
            case DEWKEEPERS_HUT: return HUT;
            case SKYWAY_TOLLHOUSE: return TOLLHOUSE;
            case GRANGE_CELLAR: return CELLAR;
            case TEST_RANGE: return RANGE;
            default: throw new IllegalArgumentException("Unknown Skyreach POI " + kind);
        }
    }

    public static int width(int kind) {
        return plan(kind)[0].length();
    }

    public static int height(int kind) {
        return plan(kind).length;
    }

    // =======================================================================
    // The legend — one table per POI, so a plan character means one thing.
    // =======================================================================

    /**
     * What each plan character writes.
     *
     * <p>Indexed by the character itself. {@code -1} in either array means
     * "leave this alone", which is what makes {@code '.'} free.
     */
    private static final class Legend {
        private final int[] tile = new int[128];
        private final int[] object = new int[128];
        private final byte[] rotation = new byte[128];
        private final int[] layer = new int[128];
        private final int[][] scatter = new int[128][];
        private final float[] coverage = new float[128];
        /** Applied to every mapped character that does not name its own tile. */
        private final int ground;

        Legend(int ground) {
            this.ground = ground;
            Arrays.fill(this.tile, -1);
            Arrays.fill(this.object, -1);
        }

        /** Bare floor: writes the ground and clears whatever stood on it. */
        Legend floor(char c) {
            this.tile[c] = this.ground;
            this.object[c] = 0;
            return this;
        }

        /** Bare floor of its own kind. */
        Legend floor(char c, String tileID) {
            this.tile[c] = tile(tileID);
            this.object[c] = 0;
            return this;
        }

        Legend prop(char c, String objectID) {
            return prop(c, objectID, 0);
        }

        Legend prop(char c, String objectID, int rotation) {
            this.tile[c] = this.ground;
            this.object[c] = object(objectID);
            this.rotation[c] = (byte) rotation;
            return this;
        }

        /** A prop that keeps the terrain painter's own ground under it. */
        Legend loose(char c, String objectID) {
            return loose(c, objectID, 0);
        }

        Legend loose(char c, String objectID, int rotation) {
            this.object[c] = object(objectID);
            this.rotation[c] = (byte) rotation;
            return this;
        }

        Legend prop(char c, String objectID, int rotation, String tileID) {
            this.tile[c] = tile(tileID);
            this.object[c] = object(objectID);
            this.rotation[c] = (byte) rotation;
            return this;
        }

        /**
         * Wall decor — a banner or a wall lamp. Its own tile must stay floor or
         * the banner replaces the masonry and opens a hole in the building
         * (dossier §0.2).
         */
        Legend decor(char c, String objectID, int wallDir) {
            this.tile[c] = this.ground;
            this.object[c] = 0;
            this.layer[c] = ObjectLayerRegistry.WALL_DECOR;
            this.scatter[c] = new int[]{object(objectID)};
            this.coverage[c] = 1.0F;
            this.rotation[c] = (byte) wallDir;
            return this;
        }

        /**
         * A formation rather than a sprinkle: one of these, this often, on this
         * ground. Rims and reef spines are drawn as a band in the plan and the
         * dossier asks for ~50% coverage inside it.
         */
        Legend rubble(char c, String tileID, float coverage, String... objectIDs) {
            this.tile[c] = tileID == null ? this.ground : tile(tileID);
            this.object[c] = 0;
            this.coverage[c] = coverage;
            int[] ids = new int[objectIDs.length];
            for (int i = 0; i < objectIDs.length; i++) {
                ids[i] = object(objectIDs[i]);
            }
            this.scatter[c] = ids;
            return this;
        }
    }

    /**
     * Walks a plan and writes it.
     *
     * @throws IllegalStateException on a row longer than the first — a
     *         transcription slip that would otherwise shift a whole building
     */
    private static void stamp(Preset p, String[] rows, Legend legend, GameRandom random) {
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            if (row.length() > p.width) {
                throw new IllegalStateException("Plan row " + y + " is " + row.length()
                        + " wide, the plan is " + p.width);
            }
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c == '.' || c == ' ') {
                    continue;
                }
                if (legend.tile[c] < 0 && legend.object[c] < 0 && legend.scatter[c] == null) {
                    throw new IllegalStateException("Plan character '" + c + "' has no legend entry");
                }
                if (legend.tile[c] >= 0) {
                    p.setTile(x, y, legend.tile[c]);
                }
                if (legend.object[c] >= 0) {
                    p.setObject(x, y, legend.object[c], legend.rotation[c]);
                }
                int[] choices = legend.scatter[c];
                if (choices != null && choices.length > 0
                        && (legend.coverage[c] >= 1.0F || random.getChance(legend.coverage[c]))) {
                    p.setObjectLayer(legend.layer[c], x, y,
                            choices[random.nextInt(choices.length)], legend.rotation[c]);
                }
            }
        }
    }

    private static int object(String id) {
        int value = ObjectRegistry.getObjectID(id);
        if (value < 0) throw new IllegalStateException("Missing Skyreach POI object: " + id);
        return value;
    }

    private static int tile(String id) {
        int value = TileRegistry.getTileID(id);
        if (value < 0) throw new IllegalStateException("Missing Skyreach POI tile: " + id);
        return value;
    }

    /**
     * A readable board, standing in for the dossier's {@code skywatchstele}.
     *
     * <p>The message is set at stamp time off the object entity, exactly as
     * {@code AeronautCampPreset} does it — a sign written at preset-build time
     * has no entity yet.
     */
    private static void stele(Preset p, int x, int y, int rotation, final String localeKey) {
        p.setObject(x, y, object("sign"), rotation);
        p.addCustomApply(x, y, 0, (level, levelX, levelY, dir, blackboard) -> {
            ObjectEntity entity = level.entityManager.getObjectEntity(levelX, levelY);
            if (entity instanceof SignObjectEntity) {
                ((SignObjectEntity) entity).setMessage(new LocalMessage("misc", localeKey));
            }
            return null;
        });
    }

    /**
     * One of the three residents, standing in the place the cast brief puts
     * them, at stamp time.
     *
     * <p>The guard is {@link SkywatchWorldData}'s shared claim — the same record
     * {@code SkyLevel.placeResident} takes before it stands one up beside a
     * workstation, and the same one {@code SkyArrivals} takes when a resident
     * moves into the player's settlement. Without it a world could hold two
     * Magpies: the POI and the workshop path do not see each other's mobs.
     */
    private static void resident(Preset p, int x, int y, final String mobID) {
        p.addCustomApply(x, y, 0, (level, levelX, levelY, dir, blackboard) -> {
            if (!level.isServer()) {
                return null;
            }
            if (SkywatchWorldData.residentClaimed(level.getServer(), mobID)) {
                return null;
            }
            Mob mob = MobRegistry.getMob(mobID, level);
            if (mob == null) {
                return null;
            }
            mob.canDespawn = false;
            level.entityManager.addMob(mob, levelX * 32 + 16, levelY * 32 + 16);
            SkywatchWorldData.claimResident(level.getServer(), mobID);
            return null;
        });
    }

    public static Preset build(int kind, GameRandom random) {
        String[] rows = plan(kind);
        Preset p = new Preset(rows[0].length(), rows.length);
        switch (kind) {
            case WAYSIDE_SHRINE: wayside(p, rows, random); break;
            case SHEPHERDS_FOLD: fold(p, rows, random); break;
            case FALLING_INSTITUTE: institute(p, rows, random); break;
            case NIGHTFELL_REDOUBT: redoubt(p, rows, random); break;
            case AETHER_MANUFACTORY: manufactory(p, rows, random); break;
            case SOVEREIGNS_ANVIL: anvil(p, rows, random); break;
            case PASSAGE_WAYHOUSE: wayhouse(p, rows, random); break;
            case UNOPENED_GATE: gate(p, rows, random); break;
            case PRISM_CHOIR: choir(p, rows, random); break;
            case SERPENTS_REEF: reef(p, rows, random); break;
            case DEWKEEPERS_HUT: hut(p, rows, random); break;
            case SKYWAY_TOLLHOUSE: tollhouse(p, rows, random); break;
            case GRANGE_CELLAR: cellar(p, rows, random); break;
            case TEST_RANGE: range(p, rows, random); break;
            default: throw new IllegalArgumentException("Unknown Skyreach POI " + kind);
        }
        return p;
    }

    // =======================================================================
    // 2.1 Skywatch Wayside — 11x9, the connective tissue
    // =======================================================================

    private static final String[] WAYSIDE = {
            "...........",
            ".|||||||||.",
            ".|Bb,,,Bb|.",
            ".|v,,S,,w|.",
            ".|,,,,,,,|.",
            ".|k,,c,,r|.",
            ".|,,,,,,,|.",
            ".L,,,,,,,L.",
            "...........",
    };

    private static void wayside(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("skyroadtile"))
                .floor(',')
                .prop('|', "cloudmarblefence")
                .prop('B', "skywatchbench", RIGHT)
                .prop('b', "skywatchbench2", RIGHT)
                .prop('k', "skywatchcabinet", RIGHT)
                .prop('c', "skywatchcandelabra")
                .prop('L', "wardencandelabra")
                .prop('v', "skytulip")
                .prop('w', "cloudbell")
                .prop('r', "cloudberrybush")
                .floor('S');
        stamp(p, rows, l, random);
        stele(p, 5, 3, DOWN, "swhwaysidestele");
        // "It always has something in it" — the drip-feed that eventually
        // points at the once-per-world places (dossier §3.1).
        p.addInventory(SkyreachPoiLoot.WAYSIDE, random, 1, 5);
    }

    // =======================================================================
    // 2.2 The Shepherd's Fold — 21x17, a cottage and a pasture
    // =======================================================================

    private static final String[] FOLD = {
            ".....................",
            ".|||||||||||||||||||.",
            ".|.......####O####.|.",
            ".|.T.....#W======#.|.",
            ".|......LO=====eE#.|.",
            ".|.......#ch=====#.|.",
            ".|..w....#=mmh==rO.|.",
            ".|.....bb#=h=====#.|.",
            ".|..u...bO=====c=#.|.",
            ".|.......#k===os=#.|.",
            ".|.......####D####.|.",
            ".|ffGff.....,,,....|.",
            ".|f...f....L,,,L...|.",
            ".|f...fT....,,,....|.",
            ".|f...f.....,,,....|.",
            ".ffffffffffffGffffff.",
            ".....................",
    };

    private static void fold(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("nimbusfloortile"))
                .floor('=')
                .floor(',', "skyroadtile")
                .prop('#', "skystonebrickwall")
                .prop('O', "skystonebrickwindow")
                .prop('D', "skystonebrickdoor")
                .loose('|', "skyironfence")
                .loose('f', "skyironfence")
                .loose('G', "skyironfencegate")
                .prop('W', "windsilkloom", RIGHT)
                .prop('E', "skywatchbed", LEFT)
                .prop('e', "skywatchbed2", LEFT)
                .prop('r', "skywatchdresser", LEFT)
                .prop('m', "skywatchmodulartable")
                .prop('h', "skywatchchair", DOWN)
                .prop('k', "skywatchcabinet", UP)
                .prop('s', "skywatchbookshelf", UP)
                .prop('o', "skywatchclock", UP)
                .prop('c', "skywatchcandelabra")
                .loose('L', "wardencandelabra")
                .loose('w', "starfall")
                .loose('u', "cloudberrybush")
                .loose('T', "cloudtree")
                .loose('b', "cloudberrybush");
        stamp(p, rows, l, random);
        // The pasture is more than half of the plot and stays empty on purpose;
        // the meadow grass the Driftlands painter already grows is the read.
        p.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, 11, 6, object("skywatchchalice"));
        p.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, 12, 6, object("skywatchtome"));
        p.fillTile(11, 6, 3, 3, tile("skywatchcarpettile"));
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 11, 3, object("skywatchbanner"), WALL_ABOVE);
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 15, 3, object("skywatchbanner"), WALL_ABOVE);
        p.addInventory(SkyreachPoiLoot.FOLD, random, 10, 9);
    }

    // =======================================================================
    // 2.3 The Institute of Applied Falling — 25x21, the joke with a sequel
    // =======================================================================

    private static final String[] INSTITUTE = {
            ".........................",
            ".........................",
            ".....L....L....L.........",
            "..s=============.........",
            "..=============..........",
            ".................W.......",
            ".................s.......",
            "...................b.....",
            "...................p.....",
            "..................W......",
            "..................s......",
            "................p........",
            "...............Wr........",
            "..............s..........",
            ".........Lxxxxxxxxx......",
            ".........x########x......",
            ".........xWk######x......",
            ".........x###Y####x......",
            ".........x########x......",
            ".........xxxxxxxxxL......",
            ".........................",
    };

    private static void institute(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("nimbusfloortile"))
                .floor('=')
                .floor('#', "skystonetile")
                .floor('Y', "skystonetile")
                .loose('L', "wardencandelabra")
                .loose('W', "aeronautwreck")
                .loose('b', "skyballoon")
                .loose('p', "skyparcel")
                .loose('r', "skywatchrubble")
                .prop('k', "skywatchcabinet", DOWN, "skystonetile")
                .rubble('x', "skystonetile", 0.55F, "skystonerock", "skyscree", "skywatchrubble");
        // 's' is a stele; its own tile stays whatever the plan's neighbours are.
        l.tile['s'] = -1;
        l.object['s'] = -1;
        stamp(p, rows, l, random);
        // Four numbered entries of the test log, read in descending order.
        stele(p, 2, 3, RIGHT, "swhinstitutestele1");
        stele(p, 17, 6, DOWN, "swhinstitutestele2");
        stele(p, 18, 10, DOWN, "swhinstitutestele3");
        // The last one is the pointer to POI 14: "the Board has relocated
        // testing to the Stormveil, where the weather is more honest."
        stele(p, 14, 13, DOWN, "swhinstitutestele4");
        // Test Subject VII. It is still there. It has always been there.
        p.addCustomApply(13, 17, 0, (level, levelX, levelY, dir, blackboard) -> {
            if (level.isServer()) {
                Mob yak = MobRegistry.getMob(stairwaytoheaven.livestock.SkyLivestock.NIMBUS_YAK, level);
                if (yak != null) {
                    yak.canDespawn = false;
                    level.entityManager.addMob(yak, levelX * 32 + 16, levelY * 32 + 16);
                }
            }
            return null;
        });
        p.addInventory(SkyreachPoiLoot.INSTITUTE, random, 11, 16);
    }

    // =======================================================================
    // 2.4 Nightfell Redoubt — 25x25, the hostile one
    // =======================================================================

    private static final String[] REDOUBT = {
            ".........................",
            ".........................",
            ".........................",
            "...#####O###D###O#####...",
            "...###=q=========q=###...",
            "...###=============###...",
            "...#=#######=========#...",
            "...#q#kk=sc#========q#...",
            "...O=#=====#=========O...",
            "...#=O=====#=========#...",
            "...#=#Bb==v#====L====#...",
            "...#=###D###=========#...",
            "...D======S=P=S======D...",
            "...#=====g=rrr=g=====#...",
            "...#=========###D###=#...",
            "...#=========#==m=r#=#...",
            "...O====L====#eE===#=O...",
            "...#q========#==c==#q#...",
            "...#=========#k==eEO=#...",
            "...###=======#########...",
            "...###=q=========q=###...",
            "...#####O###D###O#####...",
            ".........................",
            ".........................",
            ".........................",
    };

    private static void redoubt(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("charfloortile"))
                .floor('=')
                .prop('#', "nightfellwall")
                .prop('O', "nightfellwindow")
                .prop('D', "nightfelldoor")
                .decor('q', "mistglasslantern", WALL_ABOVE)
                .prop('L', "wardencandelabra")
                .prop('c', "skywatchcandelabra")
                .prop('k', "skywatchcabinet", DOWN)
                .prop('s', "skywatchbookshelf", DOWN)
                .prop('v', "barrel")
                .prop('B', "skywatchbench", RIGHT)
                .prop('b', "skywatchbench2", RIGHT)
                .prop('e', "skywatchbed2", LEFT)
                .prop('E', "skywatchbed", LEFT)
                .prop('m', "skywatchmodulartable")
                .prop('r', "skywatchdresser", LEFT)
                .prop('S', "stormscreed")
                .prop('g', "chargecrystal")
                // The pedestal in the middle of the court, left EMPTY on
                // purpose: somebody already came for whatever stood on it.
                .prop('P', "skywatchdisplay");
        stamp(p, rows, l, random);
        // The four lanterns that do not hang from the north wall. decor() can
        // only carry one direction, and a lantern facing the wrong way hangs
        // off the outside of the building.
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 7, 20, object("mistglasslantern"), WALL_BELOW);
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 17, 20, object("mistglasslantern"), WALL_BELOW);
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 4, 7, object("mistglasslantern"), WALL_LEFT);
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 4, 17, object("mistglasslantern"), WALL_LEFT);
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 20, 7, object("mistglasslantern"), WALL_RIGHT);
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 20, 17, object("mistglasslantern"), WALL_RIGHT);
        p.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, 16, 15, object("skywatchtome"));
        // The armoury vault: the two cabinets and the barrel.
        p.addInventory(SkyreachPoiLoot.REDOUBT_VAULT, random, 6, 7);
        p.addInventory(SkyreachPoiLoot.REDOUBT, random, 7, 7);
        p.addInventory(SkyreachPoiLoot.REDOUBT, random, 10, 10);
        p.addInventory(SkyreachPoiLoot.REDOUBT, random, 14, 18);
    }

    // =======================================================================
    // 2.5 The Aether Manufactory — 27x21, the multi-room interior
    // =======================================================================

    private static final String[] MANUFACTORY = {
            "...........................",
            "...........................",
            "..####O######O######O####..",
            "..#===q=====rq===#=kkk==#..",
            "..#====:::::=====#====c=#..",
            "..#F===:::::====g#======#..",
            "..O====:::::===S=D=====vO..",
            "..#q===:::::=XX=q#======#..",
            "..#====:::::=====#======#..",
            "..#K===:::::====g#=ss===#..",
            "..D====:::::===S=###D####..",
            "..#q===:::::====q#====n=#..",
            "..#====:::::=====#=dh==o#..",
            "..#W===:::::====gD======#..",
            "..O====:::::===S=#====c=O..",
            "..#====:::::XX===#===P==#..",
            "..#==rr:::::=====#=mm===#..",
            "..#===q======q===#======#..",
            "..####O##D##########O####..",
            "...........................",
            "...........................",
    };

    private static void manufactory(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("gloomwoodfloortile"))
                .floor('=')
                .floor(':', "skywatchcarpettile")
                .prop('#', "skystonebrickwall")
                .prop('O', "skystonebrickwindow")
                .prop('D', "skystonebrickdoor")
                .prop('F', "aetherforge", RIGHT)
                .prop('K', "stormglasskiln", RIGHT)
                .prop('W', "windsilkloom", RIGHT)
                .prop('g', "chargecrystal")
                .prop('S', "stormscreed")
                .prop('r', "skywatchrubble")
                .decor('q', "mistglasslantern", WALL_ABOVE)
                .prop('c', "skywatchcandelabra")
                .prop('k', "skywatchcabinet", DOWN)
                .prop('s', "skywatchbookshelf", UP)
                .prop('v', "barrel")
                .prop('d', "skywatchdesk", RIGHT)
                .prop('h', "skywatchchair", LEFT)
                .prop('o', "skywatchclock", LEFT)
                .prop('m', "skywatchmodulartable")
                .prop('P', "skywatchdisplay")
                .decor('n', "skywatchbanner", WALL_ABOVE)
                .floor('X');
        stamp(p, rows, l, random);
        // Both halves of each crystal cluster: CrystalClusterObject registers
        // <id> and <id>r, and a lone half is a lone half (dossier §0.2).
        p.setObject(13, 7, object("stormcrystal"));
        p.setObject(14, 7, object("stormcrystalr"));
        p.setObject(12, 15, object("stormcrystal"));
        p.setObject(13, 15, object("stormcrystalr"));
        p.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, 19, 16, object("skywatchtome"));
        p.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, 20, 16, object("skywatchchalice"));
        // The deepest material cache in the mod, and Sovereign Shard II on the
        // counting room's display stand.
        p.addInventory(SkyreachPoiLoot.MANUFACTORY, random, 19, 3);
        p.addInventory(SkyreachPoiLoot.MANUFACTORY, random, 20, 3);
        p.addInventory(SkyreachPoiLoot.MANUFACTORY, random, 21, 3);
        p.addInventory(SkyreachPoiLoot.MANUFACTORY, random, 23, 6);
        p.addInventory(SkyreachPoiLoot.MANUFACTORY_DISPLAY, random, 21, 15);
    }

    // =======================================================================
    // 2.6 The Sovereign's Anvil — 29x29, the arena
    // =======================================================================

    private static final String[] ANVIL = {
            ".............................",
            ".............................",
            "...........xxxLxxx...........",
            ".........xxx;;;;;xxx.........",
            ".......xxx;;;;;;;;;xxx.......",
            "......xx;;;|||G|||;;;xx......",
            ".....xx;;|||,,,,,|||;;xx.....",
            "....xx;;||,,,,,,,,,||;;xx....",
            "....x;;|A,,,g,,,g,,,A|;;x....",
            "...xx;||,,,,,,,,,,,,,||;xx...",
            "...x;;|,,,,,,,,,,,,,,,|;;x...",
            "..xx;||,,,,,,,,,,,,,,,||;xx..",
            "..x;;|,,g,,,+++++,,,g,,|;;x..",
            "..x;;|,,,,,,+++++,,,,,,|;;x..",
            "..L;;G,,,,,,++V++,,,,,,G;;L..",
            "..x;;|,,,,,,+++++,,,,,,|;;x..",
            "..x;;|,,g,,,+++++,,,g,,|;;x..",
            "..xx;||,,,,,,,,,,,,,,,||;xx..",
            "...x;;|,,,,,,,,,,,,,,,|;;x...",
            "...xx;||,,,,,,,,,,,,,||;xx...",
            "....x;;|A,,,g,,,g,,,A|;;x....",
            "....xx;;||,,,,,,,,,||;;xx....",
            ".....xx;;|||,,,,,|||;;xx.....",
            "......xx;;;|||G|||;;;xx......",
            ".......xxx;;;;;;;;;xxx.......",
            ".........xxx;;;;;xxx.........",
            "...........xxxLxxx...........",
            ".............................",
            ".............................",
    };

    private static void anvil(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("charfloortile"))
                .floor(',')
                .floor(';', "stormslatetile")
                .floor('+', "marblecheckertile")
                .prop('|', "cloudmarblefence", 0, "stormslatetile")
                .prop('G', "cloudmarblefencegate", 0, "stormslatetile")
                .prop('A', "seraphstatue")
                .prop('g', "chargecrystal")
                .prop('L', "wardencandelabra", 0, "stormslatetile")
                // The altar's stand-in. §2.6: do not ship the altar with a
                // summon it cannot honour — the Storm Sovereign is v0.6.
                .prop('V', "skywatchdisplay", DOWN, "marblecheckertile")
                .rubble('x', "stormslatetile", 0.55F, "skystonerock", "skyscree", "skywatchrubble");
        stamp(p, rows, l, random);
        p.addInventory(SkyreachPoiLoot.ANVIL, random, 14, 14);
    }

    // =======================================================================
    // 2.7 The Passage Wayhouse — 19x15, the fast-travel network
    // =======================================================================

    private static final String[] WAYHOUSE = {
            "...................",
            "...###O###O###.....",
            "...#cn=====eE#.....",
            "...#=hT=hT===#..Y..",
            "...O==th=th==#.....",
            "...#=========#..w..",
            "...#k=====c==O.....",
            "...#=======eE#.....",
            "...#s=====o==#.Y...",
            "...#####D#####.....",
            "..L,,,y,,,y,,,L....",
            "..,,,,,,s,,,,,,....",
            "...................",
            "...................",
            "...................",
    };

    private static void wayhouse(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("gloomwoodfloortile"))
                .floor('=')
                .floor(',', "skywaytile")
                .prop('#', "cloudmarblewall")
                .prop('O', "cloudmarblewindow")
                .prop('D', "cloudmarbledoor")
                .prop('T', "skywatchdinnertable", DOWN)
                .prop('t', "skywatchdinnertable2", DOWN)
                .prop('h', "skywatchchair", RIGHT)
                .prop('e', "skywatchbed2", LEFT)
                .prop('E', "skywatchbed", LEFT)
                .prop('k', "skywatchcabinet", RIGHT)
                .prop('o', "skywatchclock", UP)
                .prop('c', "skywatchcandelabra")
                .decor('n', "skywatchbanner", WALL_ABOVE)
                // §3.3's Skyway network. Vanilla's own waystone is the flow the
                // dossier asks to be built on, so the shortcut is real rather
                // than a prop that looks like one.
                .prop('y', "waystone", 0, "skywaytile")
                .prop('L', "wardencandelabra", 0, "skywaytile")
                .loose('Y', "cloudtree")
                .loose('w', "starfall");
        // The bookshelf shares 's' with the stele outside; inside it is the shelf.
        l.tile['s'] = l.ground;
        l.object['s'] = object("skywatchbookshelf");
        l.rotation['s'] = (byte) UP;
        stamp(p, rows, l, random);
        // ...and outside it is the wayhouse's own board, written after the walk.
        p.setTile(8, 11, tile("skywaytile"));
        stele(p, 8, 11, DOWN, "swhwayhousestele");
        // Three chairs turned the other way: the west seat of each table.
        p.setObject(7, 4, object("skywatchchair"), LEFT);
        p.setObject(10, 4, object("skywatchchair"), LEFT);
        p.addInventory(SkyreachPoiLoot.WAYHOUSE, random, 4, 6);
    }

    // =======================================================================
    // 2.8 The Unopened Gate — 27x23, the story beat
    // =======================================================================

    private static final String[] GATE = {
            "...........................",
            "..........|||G|||..........",
            "........|||,,L,,|||........",
            ".......|L,,,,,,,,,L|.......",
            "......||,,,,,,,,,,,||......",
            ".....||,,,,,,,,,,,,,||.....",
            "....|L,,,,,,,,,,,,,,,L|....",
            "....|,,,,,,,,,,,,,,,,,|....",
            "...||,,,,,,,,,,,,,,,,,||...",
            "...|,,,,,,+k+++k+,,,,,,|...",
            "...|,,,,,,+++++++,,,,,,|...",
            "...GL,,,,,A+++++A,,,,,LG...",
            "...|,,,,,,+++++++,,,,,,|...",
            "...|,,,,,,+++P+++,,,,,,|...",
            "...||,,,,,,,,,,,,,,,,,||...",
            "....|,,,,,,,,,,,,,,,,,|....",
            "....|L,,,,,,,,,,,,,,,L|....",
            ".....||,,,,,,,,,,,,,||.....",
            "......||,,,,,,,,,,,||......",
            ".......|L,,,,,,,,,L|.......",
            "........|||,,L,,|||........",
            "..........|||G|||..........",
            "...........................",
    };

    private static void gate(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("skywaytile"))
                .floor(',')
                .floor('+', "marblecheckertile")
                .prop('|', "cloudmarblefence")
                .prop('G', "cloudmarblefencegate")
                .prop('L', "wardencandelabra")
                // 96px of art on one tile, and the five-tile gap between the
                // two of them IS the gate. Nobody took it. It was never hung.
                .prop('A', "seraphstatue", 0, "marblecheckertile")
                .prop('k', "skywatchcabinet", DOWN, "marblecheckertile")
                .prop('P', "skywatchdisplay", 0, "marblecheckertile");
        stamp(p, rows, l, random);
        // Page VII, the last one, on the pedestal.
        p.addInventory(SkyreachPoiLoot.GATE_PEDESTAL, random, 13, 13);
        p.addInventory(SkyreachPoiLoot.GATE, random, 11, 9);
        p.addInventory(SkyreachPoiLoot.GATE, random, 15, 9);
        // The only dense stand of Sky Seraph in the world, in clusters of 2-3
        // outside the ring rather than a uniform sprinkle.
        int[][] grove = {{2, 4}, {3, 5}, {24, 4}, {23, 6}, {2, 18}, {4, 19}, {24, 18}};
        for (int[] at : grove) {
            p.setObject(at[0], at[1], object("skyseraphtree"));
        }
        int[][] saplings = {{4, 3}, {22, 4}, {3, 20}, {23, 19}, {13, 22}};
        for (int[] at : saplings) {
            p.setObject(at[0], at[1], object("skyseraphsapling"));
        }
    }

    // =======================================================================
    // 2.9 The Prism Choir — 21x21, seven notes
    // =======================================================================

    private static final String[] CHOIR = {
            ".....................",
            ".....................",
            "........||G||........",
            "......|||,,,|||......",
            "....|||,,,,,,,|||....",
            "....|,,,,,C,,,,,|....",
            "...||,,,,,,,,,,,||...",
            "...|,,C,,,P,,,C,,|...",
            "..||,,,,,,,,,,,,,||..",
            "..|,,,,,,,,,,,,,,,|..",
            "..G,,,,,,,w,,,,,,,G..",
            "..|,,C,,,,,,,,,C,,|..",
            "..||,,,,,,,,,,,,,||..",
            "...|,,,,,,,,,,,,,|...",
            "...||,,,,,,,,,,,||...",
            "....|,,,C,,,C,,,|....",
            "....|||,,,,,,,|||....",
            "......|||,,,|||......",
            "........||G||........",
            ".....................",
            ".....................",
    };

    private static void choir(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("prismfloortile"))
                .floor(',')
                .prop('|', "cloudmarblefence")
                .prop('G', "cloudmarblefencegate")
                // §2.9's own stated fallback for the chimes, taken because the
                // prismchime sheet does not exist: a lit crystal column.
                .prop('C', "chargecrystal")
                .prop('w', "starfall")
                .prop('P', "skywatchdisplay");
        stamp(p, rows, l, random);
        p.setObject(10, 1, object("wardencandelabra"));
        p.setObject(10, 19, object("wardencandelabra"));
        p.setObject(1, 10, object("wardencandelabra"));
        p.setObject(19, 10, object("wardencandelabra"));
        // Aurora bloom pairs outside the ring — both halves written.
        int[][] blooms = {{3, 3}, {17, 4}, {4, 17}};
        for (int[] at : blooms) {
            p.setObject(at[0], at[1], object("aurorabloom"));
            p.setObject(at[0] + 1, at[1], object("aurorabloomr"));
        }
        p.addInventory(SkyreachPoiLoot.CHOIR, random, 10, 7);
    }

    // =======================================================================
    // 2.10 The Serpent's Reef — 25x25, 123 land tiles in open cloud
    // =======================================================================

    private static final String[] REEF = {
            "~~~~~~~~~~~~~~~~~~~~~~~~~",
            "~~~~~~~~~~~~~~~~~~~~~~~~~",
            "~~~~~~~~~~~~~~~~~~~~~~~~~",
            "~~~~~~~~~~~~~~~~~~~~~~~~~",
            "~~~~~~~~~~~~~~~~~~~~~~~~~",
            "~~~~~~~~~~~~~~~~~~~~~~~~~",
            "~~~~~~~~~###=~~~~~~~~~~~~",
            "~~~~~~~=#=====~~~~~~~~~~~",
            "~~~~~~=#======~~~~~~~~~~~",
            "~~~~~~#=====~~~~~~~~~~~~~",
            "~~~~~L#===~~~~~~~~~~~~~~~",
            "~~~~=====~~~~~~~~~~~~~~~~",
            "~~~~=#===~~~~~~~~~~~~~~~~",
            "~~~~a#==~~~~~~~~~====~~~~",
            "~~~~=#==~~~~~~~~~=p==~~~~",
            "~~~~====~~~~~~~~=====~~~~",
            "~~~~~=#=~~~~~~~~==p=L~~~~",
            "~~~~~~a#=~~~~~~~====~~~~~",
            "~~~~~~==f=W=b=W=#==~~~~~~",
            "~~~~~~==k#=P=r=#===~~~~~~",
            "~~~~~~~====#=#===~~~~~~~~",
            "~~~~~~~~~~======~~~~~~~~~",
            "~~~~~~~~~~~~=~~~~~~~~~~~~",
            "~~~~~~~~~~~~~~~~~~~~~~~~~",
            "~~~~~~~~~~~~~~~~~~~~~~~~~",
    };

    private static void reef(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("skystonetile"))
                .floor('=')
                .prop('W', "aeronautwreck", RIGHT)
                .prop('b', "skyballoon")
                .prop('f', "skywatchrubble")
                .prop('r', "skywatchrubble")
                .prop('k', "skywatchcabinet", UP)
                .prop('P', "skywatchdisplay")
                .prop('a', "aetheriumrock")
                .prop('p', "prismshardrock")
                .prop('L', "wardencandelabra")
                .rubble('#', "skystonetile", 0.85F, "skystonerock", "skyscree");
        // '~' is the Mistsea the painter already has; leaving it alone is what
        // keeps the crescent reading as geology rather than as a stamped square.
        l.tile['~'] = -1;
        l.object['~'] = -1;
        stamp(p, rows, l, random);
        // The reef glows from a shore at night; that is its navigational job.
        p.setObject(7, 8, object("starfall"));
        p.setObject(5, 12, object("starfall"));
        p.setObject(16, 18, object("starfall"));
        // One Ledger Page on the pedestal, always: the second reliable source,
        // so a player who hates roads can still finish the collection.
        p.addInventory(SkyreachPoiLoot.REEF_PEDESTAL, random, 11, 19);
        p.addInventory(SkyreachPoiLoot.REEF, random, 8, 19);
    }

    // =======================================================================
    // 2.11 The Dew-Keeper's Hut — 13x13, a small empty house with a story
    // =======================================================================

    private static final String[] HUT = {
            ".............",
            "...####O####.",
            "...#c====eE#.",
            "...#==h====#.",
            "..sD==mm===O.",
            "...#k==h===#.",
            "...#o=====r#.",
            "...#########.",
            "..L..........",
            "..ffffGffff..",
            "..f.g...g.f..",
            "..f..gw...f..",
            "..fffffffff..",
    };

    private static void hut(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("prismfloortile"))
                .floor('=')
                .prop('#', "skystonebrickwall")
                .prop('O', "skystonebrickwindow")
                .prop('D', "skystonebrickdoor")
                .prop('c', "skywatchcandelabra")
                .prop('e', "skywatchbed2", LEFT)
                .prop('E', "skywatchbed", LEFT)
                .prop('r', "skywatchdresser", LEFT)
                .prop('m', "skywatchmodulartable")
                .prop('h', "skywatchchair", DOWN)
                .prop('k', "skywatchcabinet", RIGHT)
                .prop('o', "cookingpot")
                .loose('L', "wardencandelabra")
                .loose('f', "skyironfence")
                .loose('G', "skyironfencegate")
                .loose('g', "glowfern")
                .loose('w', "starfall");
        l.tile['s'] = -1;
        l.object['s'] = -1;
        stamp(p, rows, l, random);
        // The keeper went to the Choir to hear the seven notes and did not come
        // back — a common POI pointing at a per-region one.
        stele(p, 2, 4, RIGHT, "swhdewkeeperstele");
        p.setObject(7, 5, object("skywatchchair"), UP);
        p.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, 6, 4, object("skywatchtome"));
        p.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, 7, 4, object("pottedcloudberry"));
        // A cooking starter kit beside a working cooking pot.
        p.addInventory(SkyreachPoiLoot.HUT, random, 4, 5);
        p.addInventory(SkyreachPoiLoot.HUT, random, 10, 6);
    }

    // =======================================================================
    // 2.12 The Skyway Toll-House — 23x19, Magpie
    // =======================================================================

    private static final String[] TOLLHOUSE = {
            ".......................",
            ".......................",
            "..####O#########O####..",
            "..#q======q=#====ss=#..",
            "..#dh=====c=#=dh====#..",
            "..#=========D=======O..",
            "..O===+++===#=c=P==k#..",
            "..#===+++===#=======#..",
            "..#===+++===#########..",
            "..#===+++r==#=======#..",
            "..#===+++===#=k===k=#..",
            "..O=========D===P===O..",
            "..#Bb=====c=#=v===v=#..",
            "..#q======q=#===q===#..",
            "..#####D########O####..",
            ".,,,,L,,,,,,,,,,,L,,,,.",
            ".,,,,,,,,,,S,,,,,,,,,,.",
            ",,,,,,,,,,,,,,,,,,,,,,,",
            ",,,,,,,,,,,,,,,,,,,,,,,",
    };

    private static void tollhouse(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("gloomwoodfloortile"))
                .floor('=')
                .floor('+', "marblecheckertile")
                .floor(',', "skywaytile")
                .prop('#', "cloudmarblewall")
                .prop('O', "cloudmarblewindow")
                .prop('D', "cloudmarbledoor")
                .decor('q', "mistglasslantern", WALL_ABOVE)
                .prop('c', "skywatchcandelabra")
                .prop('L', "wardencandelabra", 0, "skywaytile")
                .prop('d', "skywatchdesk", RIGHT)
                .prop('h', "skywatchchair", LEFT)
                .prop('s', "skywatchbookshelf", DOWN)
                .prop('k', "skywatchcabinet", DOWN)
                .prop('B', "skywatchbench", RIGHT)
                .prop('b', "skywatchbench2", RIGHT)
                .prop('P', "skywatchdisplay")
                .prop('v', "barrel")
                .prop('r', "skywatchrubble")
                .floor('S', "skywaytile");
        stamp(p, rows, l, random);
        // The three lanterns that hang from a wall below or beside them.
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 3, 13, object("mistglasslantern"), WALL_BELOW);
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 10, 13, object("mistglasslantern"), WALL_BELOW);
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 16, 13, object("mistglasslantern"), WALL_BELOW);
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 19, 6, object("skywatchbanner"), WALL_ABOVE);
        // The Ledger of Undelivered Post, on Magpie's own desk.
        p.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, 14, 4, object("skywatchtome"));
        // The tariff board, still legible.
        stele(p, 11, 16, DOWN, "swhtollhousestele");
        // The rewards: the Writ in the ledger room, the Lockbox in the vault.
        p.addInventory(SkyreachPoiLoot.TOLLHOUSE_WRIT, random, 16, 6);
        p.addInventory(SkyreachPoiLoot.TOLLHOUSE_LOCKBOX, random, 16, 11);
        p.addInventory(SkyreachPoiLoot.TOLLHOUSE, random, 19, 6);
        p.addInventory(SkyreachPoiLoot.TOLLHOUSE, random, 14, 10);
        p.addInventory(SkyreachPoiLoot.TOLLHOUSE, random, 18, 10);
        p.addInventory(SkyreachPoiLoot.TOLLHOUSE, random, 14, 12);
        p.addInventory(SkyreachPoiLoot.TOLLHOUSE, random, 18, 12);
        // Magpie, in the ledger room, on the far side of the vault door.
        resident(p, 18, 5, stairwaytoheaven.settlement.SkySettlers.MAGPIE);
    }

    // =======================================================================
    // 2.13 The Grange Cellar — 21x19, Halda
    // =======================================================================

    private static final String[] CELLAR = {
            ".....................",
            ".....................",
            ".###....######....##.",
            ".#.......r......r..#.",
            ".#..%%%%%%%%%%%....#.",
            ".#..%V=V==%===%....#.",
            ".#..%=====%=v=%....#.",
            ".#..%V=V==%===%....#.",
            ".#..%==Z==D===%....#.",
            ".#..%q====%=c=%....#.",
            ".#..%=====%==k%....#.",
            ".#..%=q===%===%....#.",
            ".#..%%%D%%%%%%%....#.",
            ".#.................#.",
            ".##....######....###.",
            ".....................",
            ".....................",
            ".....................",
            ".....................",
    };

    private static void cellar(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("gloomwoodfloortile"))
                .floor('=')
                .loose('#', "skystonebrickwall")
                .prop('%', "nightfellwall")
                .prop('D', "nightfelldoor")
                .loose('r', "skywatchrubble")
                // The cast brief's own archetype for the Fermentation Vat:
                // "built like the CHEESE PRESS — the settler loads it, walks
                // away, and collects later". Vanilla's press is that object.
                .prop('V', "cheesepress")
                .decor('q', "mistglasslantern", WALL_LEFT)
                .prop('c', "skywatchcandelabra")
                .prop('k', "skywatchcabinet", LEFT)
                .floor('Z')
                .prop('v', "barrel", 0, "marblecheckertile");
        stamp(p, rows, l, random);
        p.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, 6, 11, object("mistglasslantern"), WALL_BELOW);
        // The Warden's Round: one aged barrel, deepest cell, once per world.
        p.addInventory(SkyreachPoiLoot.CELLAR_ROUND, random, 12, 6);
        p.addInventory(SkyreachPoiLoot.CELLAR, random, 13, 10);
        // Halda is in the deep cell, behind the partition — which is why she is
        // still alive.
        resident(p, 12, 10, stairwaytoheaven.settlement.SkySettlers.HALDA);
    }

    // =======================================================================
    // 2.14 The Test Range — 27x23, Ossian Vane
    // =======================================================================

    private static final String[] RANGE = {
            "...........................",
            "...........................",
            "...........................",
            "................##O#.####..",
            "................#t=====a#..",
            "...xxxxx........O=======#..",
            "...x:W:x........#==P====#..",
            "...x:::x........Dk======#..",
            "...xx:px........#=d=h==gO..",
            "....xxxxx.......#=======#..",
            "................#s=====c#..",
            "................#########..",
            "...........................",
            "........xxxxxxx............",
            ".......x::b:::x............",
            ".......x:N:W::x............",
            "..xxxx.x::::::x............",
            ".xx::xx.xxxxxx.............",
            ".x:W:px....................",
            ".x::::x....................",
            "..xxxx.....................",
            "...........................",
            "...........................",
    };

    private static void range(Preset p, String[] rows, GameRandom random) {
        Legend l = new Legend(tile("charfloortile"))
                .floor('=')
                .floor(':', "skystonetile")
                .floor('N', "skystonetile")
                .prop('#', "nightfellwall")
                .prop('O', "nightfellwindow")
                // Shut, and it stays shut: the prototype locked it from inside.
                // The way in is the gap at (20,3) where the north wall came
                // down, and a player who walks the whole west face looking for
                // a handle has been told the story without a line of text.
                .prop('D', "nightfelldoor")
                .prop('t', "skywatchtelescope")
                .prop('a', "skywatchastrolabe")
                .prop('P', "skywatchdisplay")
                .prop('k', "skywatchcabinet", RIGHT)
                .prop('d', "skywatchdesk", RIGHT)
                .prop('h', "skywatchchair", LEFT)
                .prop('g', "chargecrystal")
                .prop('s', "skywatchbookshelf", UP)
                .prop('c', "skywatchcandelabra")
                .loose('W', "aeronautwreck")
                .loose('b', "skyballoon")
                .loose('p', "skyparcel")
                .rubble('x', null, 0.50F, "skystonerock", "skyscree", "stormscreed");
        stamp(p, rows, l, random);
        // The Storm Lens Core on the display, the Casings in the cabinet.
        p.addInventory(SkyreachPoiLoot.RANGE_CORE, random, 19, 6);
        p.addInventory(SkyreachPoiLoot.RANGE, random, 17, 7);
        // Vane, on the far side of a door he cannot open from inside either.
        resident(p, 20, 6, stairwaytoheaven.settlement.SkySettlers.OSSIAN);
    }
}
