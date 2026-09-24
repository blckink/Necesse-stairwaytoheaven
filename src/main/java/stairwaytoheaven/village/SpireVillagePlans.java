package stairwaytoheaven.village;

import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets.Legend;

/**
 * The Spire Village's twelve room plans, one character per tile.
 *
 * <p>{@code docs/design/chapter-03-spire-village.md} §3 draws every one of them;
 * the arrays below ARE those maps, character for character, and
 * {@code tools/plan_transcription_audit.py} fails the gate the moment the two
 * drift apart. They are read through {@link RealmPoiPresets#plan}, the dossier
 * interpreter chapter 01 already built, so every rule it enforces at load —
 * windows mid-run (§0.3), fences with a neighbour (§0.4), both halves of a pair
 * with one rotation, chairs turned to a table, decorations only on a holder —
 * holds here too, and a slip throws when {@link SpireVillage#validateAll} runs
 * at {@code postInit} instead of in a player's village.
 *
 * <h2>The canonical frame</h2>
 * Every house is drawn with its FRONT — the wall with the street door — as the
 * bottom row. A side house is 23 wide and 10 deep, a corner plot 10 x 10.
 * {@link SpireVillage} turns each plan with vanilla's own
 * {@code Preset.rotate} so the front faces the street its block sits on; the
 * engine carries every furniture rotation, wall-decor side and far half of a
 * pair along ({@code Preset.rotateData}, {@code MultiTile.getPresetRotation},
 * VERIFIED [jar] in the decompiled 1.3.2 source).
 *
 * <h2>Shared characters</h2>
 * <pre>
 *   .   nothing — the village's own base pass (turf) shows through
 *   =   the room's floor      :  carpet      @  where the resident lives
 *   #   wall   O  window   D  door
 *   ^ v &lt; &gt;  a wall lantern, hanging from the wall above / below / left / right
 *   h   chair (turned to its table by the interpreter)
 *   E e bed, head and foot      N n  bench, both halves
 * </pre>
 * Everything else is named per plan in its legend below and in the dossier.
 */
public final class SpireVillagePlans {

    private SpireVillagePlans() {
    }

    // Furniture rotation: the direction a piece FACES.
    private static final int UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3;
    // Wall decor: where the WALL is (chapter-01 §0.2).
    private static final int WALL_BELOW = 0, WALL_LEFT = 1, WALL_ABOVE = 2, WALL_RIGHT = 3;

    // =====================================================================
    // Magpie — "Das Kontor der Elster": shop, stockroom, office.
    // =====================================================================
    public static final String[] MAGPIE_PLAN = {
            ".###O####O#######O####.",
            ".#lls==ppp=CC=#RE=SSK#.",
            ".#s=====p====>#=e=::=#.",
            ".O============#===::=O.",
            ".####D#########=xh==c#.",
            ".#P====@===PP=D======#.",
            ".#l===ttt====>#<====C#.",
            ".O=Nn=========#hth===#.",
            ".#<===========#====c=#.",
            ".###O##D###O#####O####.",
    };

    // =====================================================================
    // Halda — "Die Kellerschänke": bar room, brew room, her own room.
    // =====================================================================
    public static final String[] HALDA_PLAN = {
            ".###O###O######O###O##.",
            ".#ggg=o=lls#RE======C#.",
            ".#========s#=e===hm==#.",
            ".#<=======>#<=======c#.",
            ".#####D#########D#####.",
            ".#gg=@=====hh====hh=c#.",
            ".O=bbbb===htth==htth=O.",
            ".#<========hh====hh=>#.",
            ".#Nn=================#.",
            ".####O###D####O###O###.",
    };

    // =====================================================================
    // Ossian — "Das kleine Archiv": stacks, reading tables, the workshop.
    // =====================================================================
    public static final String[] OSSIAN_PLAN = {
            ".####O####O#######O###.",
            ".#SSS=SSS==SSSK#RE=SC#.",
            ".#=============#=e===#.",
            ".#=SSS====SSS=>#<===c#.",
            ".O=============#=xh==O.",
            ".#=htth==htth==#====P#.",
            ".#======@======D====P#.",
            ".#A===========X#=jj==#.",
            ".#<===========>#=hh=c#.",
            ".###O###D###O#####O###.",
    };

    // =====================================================================
    // Eveleen — "Das Gewächshaus": planting beds under a glass front.
    // =====================================================================
    public static final String[] EVELEEN_PLAN = {
            ".##O#O#O#O#O#O###O#O##.",
            ".#_fgyf_=_ygfy_#RE=SC#.",
            ".#_gyfg_=_fgyf_#=e===#.",
            ".O=============#<===cO.",
            ".#B_B_B_=_B_B_B#=xh==#.",
            ".#=ppp==@===ss=D=====#.",
            ".O=h===========#<=th=O.",
            ".#_fyg__=_gyf__#=====#.",
            ".#<===========>#Nn=c=#.",
            ".##O#O##D##O#O###O#O##.",
    };

    // =====================================================================
    // Ives — "Die Küsterei": a chapel, his cell, and a stonemason's yard.
    // =====================================================================
    public static final String[] IVES_PLAN = {
            ".###O#####O###.........",
            ".#RE=Sxh==C=Kc#FFFFFFF.",
            ".#=e=========>#.G.H.GF.",
            ".######D#######......F.",
            ".O==Q==V==Q===O.H.G.HF.",
            ".#=nN==@==nN==#......F.",
            ".#=nN=====nN==#......F.",
            ".O============O......F.",
            ".#<==========>#FFFgFFF.",
            ".###O##D##O####........",
    };

    // =====================================================================
    // Mortimer — "Das Bestattungshaus": showroom, workshop, parlour.
    // =====================================================================
    public static final String[] MORTIMER_PLAN = {
            ".###O###O######O###O##.",
            ".#lss=jj=ll#RE==WK==C#.",
            ".#====hh==>#<e=======#.",
            ".#<========#=hth====c#.",
            ".#####D#########D#####.",
            ".#=Y=QY=QY===G=H=U==c#.",
            ".O=y==y==y======@====O.",
            ".#<==========bbb====>#.",
            ".#Nn=================#.",
            ".####O###D####O###O###.",
    };

    // =====================================================================
    // Caspern — "Die kalte Schmiede": the forge hall and a back room.
    // =====================================================================
    public static final String[] CASPERN_PLAN = {
            ".####O####O#######O###.",
            ".#F=A=I=J=llsC=#RE=SC#.",
            ".#=============#=e===#.",
            ".O============>#<===cO.",
            ".#Z=Z==@=mm=W==#=xh==#.",
            ".#=======hh====#=====#.",
            ".O=============D====PO.",
            ".#ll==========>#<th==#.",
            ".#<============#Nn=c=#.",
            ".###O###D###O#####O###.",
    };

    // =====================================================================
    // Mr. Knott — "Das Haus der vielen Türen" (corner plot).
    // =====================================================================
    public static final String[] KNOTT_PLAN = {
            "..........",
            ".##O##O##.",
            ".#P=#=RK#.",
            ".#==#===#.",
            ".#D###D##.",
            ".#=th==c#.",
            ".O==@===O.",
            ".#E=====#.",
            ".#e====S#.",
            ".###D####.",
    };

    // =====================================================================
    // Eleanor — "Das Küchenhaus" (corner plot).
    // =====================================================================
    public static final String[] ELEANOR_PLAN = {
            "..........",
            ".##O##O##.",
            ".#okkkC=#.",
            ".#==@===#.",
            ".O=htth=O.",
            ".#======#.",
            ".#E=R=c>#.",
            ".#e=====#.",
            ".###D####.",
            ".f.f..f.f.",
    };

    // =====================================================================
    // Der Marktplatz — stalls, the well, the notice board.
    // =====================================================================
    public static final String[] MARKET_PLAN = {
            ".,,,,,,,,,,,,,,,,,,,,,.",
            ".L,,,,,Z,,,,,,,,,,,,,L.",
            ".,,,,,,,,,||||,,,,,,,,.",
            ".,,X,s,,,,|~~|,,,l,l,,.",
            ".,,aaa,,,,|~~|,,,bbb,,.",
            ".,,,,,,,,,|g||,,,,,,,,.",
            ".,,,,,,,,,,,,,,,,,,,,,.",
            ".,**,,,,,,,,,,,,,,,**,.",
            ".L,,,,,,,Nn,,,Nn,,,,,L.",
            ".,,,,,,,,,,,,,,,,,,,,,.",
    };

    // =====================================================================
    // Der Übungsring — the village's sparring ring (corner plot).
    // =====================================================================
    public static final String[] RING_PLAN = {
            "..........",
            ".||||||||.",
            ".|,,,,,,|.",
            ".|,W,,W,|.",
            ".|,,,,,,|.",
            ".|,,W,,,|.",
            ".|,,,,,,|.",
            ".|,,,,,,|.",
            ".|||g||||.",
            "L........L",
    };

    // =====================================================================
    // Das Edenbeet — Eveleen's walled patch of Eden (corner plot).
    // =====================================================================
    public static final String[] EDENBED_PLAN = {
            "..........",
            ".FFFFFFFF.",
            ".F%%%%%%F.",
            ".F%B%%B%F.",
            ".F%%%%%%F.",
            ".F%%T%%%F.",
            ".F%%%%%%F.",
            ".F%y%%r%F.",
            ".FFFgFFFF.",
            "..........",
    };

    // =====================================================================
    // Legends
    // =====================================================================

    /** The shared house skeleton: floor, walls of one family, lanterns, chairs, a bed. */
    private static Legend house(int ground, String wall, String window, String door) {
        return new Legend(ground)
                .floor('=').floor('@')
                .wall('#', wall).window('O', window).door('D', door)
                .floor('^').decor('^', "mistglasslantern", WALL_ABOVE)
                .floor('v').decor('v', "mistglasslantern", WALL_BELOW)
                .floor('<').decor('<', "mistglasslantern", WALL_LEFT)
                .floor('>').decor('>', "mistglasslantern", WALL_RIGHT);
    }

    /** A fresh preset of {@code rows}' own size, written through {@code legend}. */
    private static Preset apply(String[] rows, Legend legend) {
        Preset p = new Preset(rows[0].length(), rows.length);
        RealmPoiPresets.plan(p, rows, legend);
        return p;
    }

    public static Preset magpie() {
        Legend legend = house(SkyRegistry.gloomwoodFloorID,
                "skystonebrickwall", "skystonebrickwindow", "skystonebrickdoor")
                .rug(':', "skywatchcarpet")
                .chair('h', "skywatchchair")
                .table('x', "skywatchdesk")
                // The counter, and the office tea table, in reading order.
                .table('t', "skywatchmodulartable",
                        "stackofpaper", "tableclock", "quillandparchment", "teapot")
                .pair('E', 'e', "skywatchbed", DOWN)
                .pair('N', 'n', "skywatchbench", RIGHT)
                .prop('R', "skywatchdresser", DOWN)
                .prop('S', "skywatchbookshelf", DOWN)
                .prop('K', "skywatchclock", DOWN)
                .prop('C', "skywatchcabinet", DOWN)
                .prop('P', "skywatchdisplay")
                .prop('c', "skywatchcandelabra")
                .prop('l', "barrel")
                .prop('s', "sack")
                // Undelivered post, stacked where it was never collected.
                .prop('p', "skyparcel")
                .turns(20, 6, LEFT);
        return apply(MAGPIE_PLAN, legend);
    }

    public static Preset halda() {
        Legend legend = house(SkyRegistry.gloomwoodFloorID,
                "skystonebrickwall", "skystonebrickwindow", "skystonebrickdoor")
                .chair('h', "skywatchchair")
                // The bar, the guest tables and her own tea table.
                .table('b', "skywatchmodulartable", "mug", "mug", "plate", "stewpot")
                .table('t', "skywatchmodulartable", "mug", "plate", "mug", "cuttingboard")
                .table('m', "skywatchmodulartable", "pottedcloudberry", "skywatchcandle")
                .pair('E', 'e', "skywatchbed", DOWN)
                .pair('N', 'n', "skywatchbench", RIGHT)
                .prop('R', "skywatchdresser", DOWN)
                .prop('C', "skywatchcabinet", DOWN)
                .prop('c', "skywatchcandelabra")
                .prop('g', "largekeg")
                .prop('o', "cookingpot")
                .prop('l', "barrel")
                .prop('s', "sack");
        return apply(HALDA_PLAN, legend);
    }

    public static Preset ossian() {
        Legend legend = house(SkyRegistry.gloomwoodFloorID,
                "skystonebrickwall", "skystonebrickwindow", "skystonebrickdoor")
                .chair('h', "skywatchchair")
                .table('x', "skywatchdesk")
                .table('t', "skywatchmodulartable",
                        "stackedbooks", "skywatchtome", "quillandparchment", "stackofpaper")
                .table('j', "skywatchmodulartable", "largeglobe", "farseersorb")
                .pair('E', 'e', "skywatchbed", DOWN)
                .prop('S', "skywatchbookshelf", DOWN)
                .prop('K', "skywatchclock", DOWN)
                .prop('R', "skywatchdresser", DOWN)
                .prop('C', "skywatchcabinet", DOWN)
                .prop('P', "skywatchdisplay")
                .prop('c', "skywatchcandelabra")
                .prop('A', "skywatchastrolabe")
                .prop('X', "skywatchtelescope");
        return apply(OSSIAN_PLAN, legend);
    }

    public static Preset eveleen() {
        Legend legend = house(stairwaytoheaven.realms.eden.EdenRealm.edenRootFloorID,
                "palmwall", "palmwindow", "palmdoor")
                .floor('_', "farmland")
                .chair('h', "skywatchchair")
                .table('x', "palmdesk")
                .table('p', "palmmodulartable", "pottedflower1", "pottedplant2", "pottedflower4")
                .table('t', "palmmodulartable", "pottedcloudberry")
                .pair('E', 'e', "palmbed", DOWN)
                .pair('N', 'n', "palmbench", RIGHT)
                .prop('f', "redflowerpatch").paves('f', "farmland")
                .prop('g', "blueflowerpatch").paves('g', "farmland")
                .prop('y', "yellowflowerpatch").paves('y', "farmland")
                .prop('B', "blackberrybush").paves('B', "farmland")
                .prop('R', "palmdresser", DOWN)
                .prop('S', "palmbookshelf", DOWN)
                .prop('C', "palmcabinet", DOWN)
                .prop('c', "palmcandelabra")
                .prop('s', "sack");
        return apply(EVELEEN_PLAN, legend);
    }

    public static Preset ives() {
        Legend legend = house(SkyRegistry.crackedmarbleID,
                "skystonebrickwall", "skystonebrickwindow", "skystonebrickdoor")
                .chair('h', "birchchair")
                .table('x', "skywatchdesk")
                .pair('E', 'e', "skywatchbed", DOWN)
                // The chapel's pews face the altar, i.e. north: their backs to
                // the door, the far half to the west (rotation LEFT).
                .pair('N', 'n', "birchbench", LEFT)
                .prop('R', "skywatchdresser", DOWN)
                .prop('S', "skywatchbookshelf", DOWN)
                .prop('C', "skywatchcabinet", DOWN)
                .prop('K', "skywatchclock", DOWN)
                .prop('c', "skywatchcandelabra")
                .prop('Q', "stonecandlepedestal")
                .prop('V', "vase")
                // The yard: what he sells, standing where a buyer can see it.
                .loose('G', "gravestone1")
                .loose('H', "gravestone2")
                .fence('F', "stonefence")
                .fence('g', "stonefencegate");
        return apply(IVES_PLAN, legend);
    }

    public static Preset mortimer() {
        Legend legend = house(tile("cryptpath"),
                "skystonebrickwall", "skystonebrickwindow", "skystonebrickdoor")
                .chair('h', "bonechair")
                .table('j', "bonemodulartable", "skywatchcandle", "skull")
                .table('t', "bonemodulartable", "teapot")
                .table('b', "bonemodulartable", "skull", "stackofpaper", "oldchalices")
                .pair('E', 'e', "bonebed", DOWN)
                .pair('N', 'n', "bonebench", RIGHT)
                .pair('Y', 'y', "cryptcoffin", DOWN)
                .prop('R', "bonedresser", DOWN)
                .prop('W', "bonebookshelf", DOWN)
                .prop('K', "boneclock", DOWN)
                .prop('C', "bonecabinet", DOWN)
                .prop('c', "bonecandelabra")
                .prop('Q', "stonecandlepedestal")
                .prop('G', "gravestone1")
                .prop('H', "gravestone2")
                .prop('U', "cryptgravestone1")
                .prop('l', "barrel")
                .prop('s', "sack");
        return apply(MORTIMER_PLAN, legend);
    }

    public static Preset caspern() {
        Legend legend = house(SkyRegistry.gloomwoodFloorID,
                "skystonebrickwall", "skystonebrickwindow", "skystonebrickdoor")
                .chair('h', "skywatchchair")
                .table('x', "skywatchdesk")
                .table('m', "skywatchmodulartable", "forgottenblade", "stackofpaper")
                .table('t', "skywatchmodulartable", "mug")
                .pair('E', 'e', "skywatchbed", DOWN)
                .pair('N', 'n', "skywatchbench", RIGHT)
                .prop('F', "forge")
                .prop('A', "aetherforge")
                .prop('I', "ironanvil")
                .prop('J', "tungstenanvil")
                .prop('Z', "armorstand")
                .prop('W', "trainingdummy")
                .prop('R', "skywatchdresser", DOWN)
                .prop('S', "skywatchbookshelf", DOWN)
                .prop('C', "skywatchcabinet", DOWN)
                .prop('P', "skywatchdisplay")
                .prop('c', "skywatchcandelabra")
                .prop('l', "barrel")
                .prop('s', "sack");
        return apply(CASPERN_PLAN, legend);
    }

    public static Preset knott() {
        Legend legend = house(stairwaytoheaven.realms.crooked.CrookedRealm.crookedStripeID,
                "arcanicwall", "arcanicwindow", "arcanicdoor")
                .chair('h', "bonechair")
                .table('t', "bonemodulartable", "voidcube")
                .pair('E', 'e', "bonebed", DOWN)
                .prop('P', "bonedisplay")
                .prop('R', "bonedresser", DOWN)
                .prop('K', "boneclock", DOWN)
                .prop('S', "bonebookshelf", UP)
                .prop('c', "bonecandelabra");
        return apply(KNOTT_PLAN, legend);
    }

    public static Preset eleanor() {
        Legend legend = house(SkyRegistry.gloomwoodFloorID,
                "skystonebrickwall", "skystonebrickwindow", "skystonebrickdoor")
                .chair('h', "skywatchchair")
                // The kitchen she remembers, and the table for two.
                .table('k', "skywatchmodulartable", "cuttingboard", "plate", "teapot")
                .table('t', "skywatchmodulartable", "plate", "mug")
                .pair('E', 'e', "skywatchbed", DOWN)
                .prop('o', "cookingpot")
                .prop('C', "skywatchcabinet", DOWN)
                .prop('R', "skywatchdresser", DOWN)
                .prop('c', "skywatchcandelabra")
                .loose('f', "redflowerpatch");
        return apply(ELEANOR_PLAN, legend);
    }

    public static Preset market() {
        Legend legend = new Legend(stairwaytoheaven.SkyCloudmarbleSet.skywayTileID)
                .floor(',')
                .floor('~', "mistseatile")
                .fence('|', "cloudmarblefence")
                .fence('g', "cloudmarblefencegate")
                .prop('L', "wardencandelabra")
                .prop('Z', "sign")
                .prop('X', "sack")
                .prop('s', "sack")
                .prop('l', "barrel")
                // Eveleen's flower stall and Halda's drinks stall.
                .table('a', "skywatchmodulartable", "pottedflower1", "pottedplant3", "pottedflower5")
                .table('b', "skywatchmodulartable", "mug", "teapot", "plate")
                .pair('N', 'n', "skywatchbench", RIGHT)
                .prop('*', "skytulip").paves('*', "cloudturftile");
        Preset p = apply(MARKET_PLAN, legend);
        // The notice board: one line, the way chapter 02's signs carry theirs.
        for (int y = 0; y < MARKET_PLAN.length; y++) {
            int x = MARKET_PLAN[y].indexOf('Z');
            if (x < 0) {
                continue;
            }
            p.addCustomApply(x, y, 0, (level, levelX, levelY, dir, blackboard) -> {
                necesse.entity.objectEntity.ObjectEntity entity =
                        level.entityManager.getObjectEntity(levelX, levelY);
                if (entity instanceof necesse.entity.objectEntity.SignObjectEntity) {
                    ((necesse.entity.objectEntity.SignObjectEntity) entity).setMessage(
                            new necesse.engine.localization.message.LocalMessage(
                                    "misc", "swhsignvillage"));
                }
                return null;
            });
        }
        return p;
    }

    public static Preset ring() {
        Legend legend = new Legend(SkyRegistry.skyroadTileID)
                .floor(',')
                .fence('|', "cloudmarblefence")
                .fence('g', "cloudmarblefencegate")
                .prop('W', "trainingdummy")
                .loose('L', "wardencandelabra");
        return apply(RING_PLAN, legend);
    }

    public static Preset edenBed() {
        Legend legend = new Legend(tile("overgrownedentile"))
                .floor('%')
                .fence('F', "woodfence")
                .fence('g', "woodfencegate")
                .prop('B', "blackberrybush")
                .prop('T', "knowledgetree")
                .prop('y', "yellowflowerpatch")
                .prop('r', "redflowerpatch");
        return apply(EDENBED_PLAN, legend);
    }

    private static int tile(String id) {
        int value = necesse.engine.registries.TileRegistry.getTileID(id);
        if (value < 0) {
            throw new IllegalStateException("Spire Village plan names unknown tile " + id);
        }
        return value;
    }
}
