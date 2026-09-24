package stairwaytoheaven.realms.ghost;

import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.DOWN;
import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.LEFT;
import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.RIGHT;
import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.WALL_ABOVE;

import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets.Legend;

/**
 * The Haunted Manor -- the house the Headless Butler still keeps.
 *
 * <p>Until 2026-09-24 this was a nightfell box with two tables for two in it.
 * Now it is a small manor read from its front door: a dining hall where the
 * butler has laid the long table for guests who never came (the serving side
 * along the north wall is left clear, the chairs are all on the far side and
 * at the head), a tea corner and a sideboard; behind the partition the
 * master's bedroom (bed, dresser, clock, a violet rug) and the study, where
 * the desk faces the window and the bone chest -- the manor's one real
 * reward -- sits in the corner behind the shelves.
 *
 * <p>Built through {@link RealmPoiPresets#plan}, so every rule the dossier
 * writes down once is checked here too: both halves of the bed and the dinner
 * tables, every chair turned to its table, windows mid-run in their own wall
 * family, table decoration only on tables, wall candles on {@code WALL_DECOR}.
 * Four lights over ~95 floor tiles (1 per 24): a little brighter than the
 * dossier's band, because the Aftergarden has no daylight to come in through
 * the windows.
 */
public class HauntedManorPreset extends Preset {
    public static final int WIDTH = 15;
    public static final int HEIGHT = 13;

    /** The manor, drawn one character per tile; '.' writes nothing. */
    public static final String[] PLAN = {
            "...............",
            ".###O#####O###.",
            ".#E=R=K#S^=SS#.",
            ".#e:::=#==x=P#.",
            ".O=:::=#==i==O.",
            ".#c====#====B#.",
            ".####D###D####.",
            ".#=^=======^=#.",
            ".#hTtTt==imiC#.",
            ".O=hhhh======O.",
            ".#==========K#.",
            ".###O##D##O###.",
            "......,,,......",
    };

    public HauntedManorPreset(GameRandom random) {
        super(WIDTH, HEIGHT);
        Legend legend = new Legend(SkyRegistry.gloomwoodFloorID)
                .floor('=')
                .floor(',', "spiritstonetile")
                .rug(':', "purplecarpet")
                .wall('#', "nightfellwall")
                .window('O', "nightfellwindow")
                .door('D', "nightfelldoor")
                .floor('^').decor('^', "wallcandle", WALL_ABOVE)
                .pair('E', 'e', "deadwoodbed", DOWN)
                .prop('R', "deadwooddresser", DOWN)
                .prop('K', "deadwoodclock", DOWN)
                .prop('S', "deadwoodbookshelf", DOWN)
                .table('x', "deadwooddesk")
                .prop('P', "deadwooddisplay")
                .prop('c', "deadwoodcandelabra")
                .prop('B', "bonechest")
                // The laid table: a dinner table in two pairs on a velvet rug,
                // set for guests, with the chairs standing on the rug with it.
                .pair('T', 't', "deadwooddinnertable", RIGHT)
                .carpet('T', "velourcarpet").carpet('t', "velourcarpet")
                .serves('T', "diningset", "oldchalices")
                .serves('t', "reddiningset", "bloodgoblet")
                .chair('h', "deadwoodchair").carpet('h', "velourcarpet")
                .chair('i', "deadwoodchair")
                .table('m', "deadwoodmodulartable", "teapot")
                .prop('C', "deadwoodcabinet", LEFT);
        RealmPoiPresets.plan(this, PLAN, legend);
        // The realm's two weapons live here as well as on the elite drop table
        // (GhostLoot.elite) -- docs/FOGKEY_AND_BOSSPORTALS.md A3.3: "the same
        // ghost weapons also drop randomly in the Ghost region, so a player who
        // never trades still finds them." A manor chest is the one place in the
        // Aftergarden where finding a weapon reads as finding a weapon.
        //
        // 20% each, against the 30% this same chest already gives a single
        // Spiritsteel bar: a finished weapon is worth about eight of those bars
        // (see GhostGuideMob's price list), but there is only one manor and one
        // chest in it, so the chance is a rung below the bar rather than an
        // order of magnitude below it the way the repeatable mob table is.
        RealmPoiPresets.stock(this, PLAN, 'B', new LootTable(
                LootItem.between("ectoplasm", 10, 18),
                LootItem.between("soulthread", 5, 10),
                ChanceLootItem.between(0.65F, "spectralore", 4, 8),
                ChanceLootItem.between(0.30F, "spiritsteelbar", 1, 3),
                ChanceLootItem.between(0.20F, "spiritsteelreaver", 1, 1),
                ChanceLootItem.between(0.20F, "gravewindbow", 1, 1)), random);
    }
}
