package stairwaytoheaven;

import java.awt.Color;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.level.gameObject.ModularCarpetObject;
import necesse.level.gameObject.PotTableDecorationObject;
import necesse.level.gameObject.TableDecorationObject;
import necesse.level.gameObject.furniture.BedObject;
import necesse.level.gameObject.furniture.BenchObject;
import necesse.level.gameObject.furniture.CandelabraObject;
import necesse.level.gameObject.furniture.ChairObject;
import necesse.level.gameObject.furniture.DeskObject;
import necesse.level.gameObject.furniture.DinnerTableObject;
import necesse.level.gameObject.furniture.DresserObject;
import necesse.level.gameObject.furniture.ModularTableObject;

/**
 * The Skywatch furniture family: pale skystone, sky-iron and windsilk.
 *
 * <p>Every piece here sits on the vanilla furniture base class for its role,
 * which is the whole point of this file. A table drawn on a plain decoration
 * object looks like a table and is worth nothing: room scoring, settler jobs,
 * chairs turning to face a table and table decorations all key off
 * {@code RoomFurniture#getFurnitureType} and the interfaces these classes
 * implement. So a chair is a {@link ChairObject} (sittable, emits a sit-down
 * job), a table is a {@link ModularTableObject} (holds decorations and
 * candles), a bed is a {@link BedObject} (settlers can be assigned to it).
 *
 * <p>The multi-tile pieces register their own second half through the vanilla
 * static helpers — {@code <id>2} is created for us, is not obtainable, and
 * must never get a recipe or an item icon. See
 * {@code docs/research/furniture-formats.md} for the sheet layouts.
 */
public final class SkyFurnitureSet {

    /** Pale skystone, the family's map colour — SKYSTONE's "hi" ramp step. */
    private static final Color MAP_SKYWATCH = new Color(176, 185, 199);
    /** Windsilk white for the soft pieces (bed, carpet). */
    private static final Color MAP_WINDSILK = new Color(224, 231, 238);

    private static final String[] CATEGORY = {"objects", "furniture", "skywatch"};

    // Object IDs, kept on this class rather than SkyRegistry so the whole
    // family is declared, registered and looked up in one place.
    public static int skywatchChairID;
    public static int skywatchBenchID;
    public static int skywatchTableID;
    public static int skywatchDiningTableID;
    public static int skywatchDeskID;
    public static int skywatchDresserID;
    public static int skywatchBedID;
    public static int skywatchCandelabraID;
    public static int skywatchCarpetID;
    public static int skywatchChaliceID;
    public static int skywatchCandleID;
    public static int skywatchTomeID;
    public static int pottedCloudberryID;

    private SkyFurnitureSet() {
    }

    public static void register() {
        // The item and crafting category trees are separate managers, and both
        // reject an unknown branch with "Must first create item category ...".
        // Vanilla's furniture families run to E-E-O / E-B-O, so Skywatch sorts
        // after them.
        ItemCategory.createCategory("E-E-P", "objects", "furniture", "skywatch");
        ItemCategory.craftingManager.createCategory("E-B-P", "objects", "furniture", "skywatch");

        // --- Seating -------------------------------------------------------
        skywatchChairID = ObjectRegistry.registerObject("skywatchchair",
                new ChairObject("skywatchchair", MAP_SKYWATCH, CATEGORY), 5.0F, true);
        // registerBench also creates "skywatchbench2" (the far half, 0.0F, not
        // obtainable) and wires the two counterIDs together.
        skywatchBenchID = BenchObject.registerBench("skywatchbench", "skywatchbench",
                MAP_SKYWATCH, 10.0F, CATEGORY)[0];

        // --- Tables --------------------------------------------------------
        // ModularTableObject is the decoration/torch holder: this is the table
        // players can put a chalice or a candle on.
        skywatchTableID = ObjectRegistry.registerObject("skywatchmodulartable",
                new ModularTableObject("skywatchmodulartable", MAP_SKYWATCH, CATEGORY), 10.0F, true);
        skywatchDiningTableID = DinnerTableObject.registerDinnerTable("skywatchdinnertable",
                "skywatchdinnertable", MAP_SKYWATCH, 20.0F, CATEGORY)[0];

        // --- Work and storage furniture -----------------------------------
        skywatchDeskID = ObjectRegistry.registerObject("skywatchdesk",
                new DeskObject("skywatchdesk", MAP_SKYWATCH, CATEGORY), 10.0F, true);
        skywatchDresserID = ObjectRegistry.registerObject("skywatchdresser",
                new DresserObject("skywatchdresser", MAP_SKYWATCH, CATEGORY), 10.0F, true);

        // --- Sleeping ------------------------------------------------------
        // A real BedObject, so a settler can actually be assigned to it.
        skywatchBedID = BedObject.registerBed("skywatchbed", "skywatchbed",
                MAP_WINDSILK, 100.0F, CATEGORY)[0];

        // --- Light that counts as furniture ---------------------------------
        skywatchCandelabraID = ObjectRegistry.registerObject("skywatchcandelabra",
                new CandelabraObject("skywatchcandelabra", MAP_SKYWATCH, 50.0F, 0.12F, CATEGORY),
                10.0F, true);

        // --- Floor ---
        // ModularCarpetObject, not CarpetObject: the vanilla
        // CarpetObject.registerCarpet helper is dead code in 1.3.2. All four
        // quarter classes pass isMaster=true to their StaticMultiTile, so the
        // object registry rejects the set with "Has multiple master objects"
        // the moment it closes. Nothing in vanilla registers one - every
        // shipped carpet is a ModularCarpetObject, which autotiles from
        // objects/carpets/<id>.png plus a <id>mask.png edge mask.
        skywatchCarpetID = ObjectRegistry.registerObject("skywatchcarpet",
                new ModularCarpetObject("skywatchcarpet", MAP_WINDSILK), 25.0F, true);

        // --- Table decorations ---------------------------------------------
        // These are what makes a furnished room read as inhabited: they go on
        // top of the tables above, on the FENCE_AND_TABLE_DECOR layer.
        skywatchChaliceID = ObjectRegistry.registerObject("skywatchchalice",
                new TableDecorationObject("skywatchchalice", MAP_SKYWATCH, 12, 14), 20.0F, true);
        skywatchCandleID = ObjectRegistry.registerObject("skywatchcandle",
                new TableDecorationObject("skywatchcandle", MAP_WINDSILK, 10, 16), 20.0F, true);
        skywatchTomeID = ObjectRegistry.registerObject("skywatchtome",
                new TableDecorationObject("skywatchtome", new Color(92, 104, 150), 18, 10), 20.0F, true);
        pottedCloudberryID = ObjectRegistry.registerObject("pottedcloudberry",
                new PotTableDecorationObject("pottedcloudberry", new Color(232, 186, 120), 14, 12,
                        "pottedflower", true), 20.0F, true);
    }

    public static void registerRecipes() {
        // Skywatch furniture is skystone + sky-iron work; the soft pieces add
        // windsilk. Costs mirror the vanilla wood families (chair 4 units,
        // table 6-8, bed the expensive one).
        Recipes.registerModRecipe(new Recipe("skywatchchair", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 4}, {ironbar, 1}}")));
        Recipes.registerModRecipe(new Recipe("skywatchbench", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 6}, {ironbar, 2}}")));
        Recipes.registerModRecipe(new Recipe("skywatchmodulartable", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 6}, {ironbar, 1}}")));
        Recipes.registerModRecipe(new Recipe("skywatchdinnertable", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 10}, {ironbar, 2}}")));
        Recipes.registerModRecipe(new Recipe("skywatchdesk", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 8}, {ironbar, 2}}")));
        Recipes.registerModRecipe(new Recipe("skywatchdresser", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 8}, {windsilk, 2}}")));
        Recipes.registerModRecipe(new Recipe("skywatchbed", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 6}, {windsilk, 8}}")));
        Recipes.registerModRecipe(new Recipe("skywatchcandelabra", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{ironbar, 2}, {skystone, 4}, {aurorapetal, 1}}")));
        Recipes.registerModRecipe(new Recipe("skywatchcarpet", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{windsilk, 6}}")));
        Recipes.registerModRecipe(new Recipe("skywatchchalice", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{ironbar, 1}, {stormshard, 1}}")));
        Recipes.registerModRecipe(new Recipe("skywatchcandle", 2, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{windsilk, 1}, {aurorapetal, 1}}")));
        Recipes.registerModRecipe(new Recipe("skywatchtome", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{windsilk, 2}, {skystone, 2}}")));
        Recipes.registerModRecipe(new Recipe("pottedcloudberry", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 3}, {cloudberry, 2}}")));
    }
}
