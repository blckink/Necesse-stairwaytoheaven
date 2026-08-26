package stairwaytoheaven;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.inventory.item.Item;
import necesse.inventory.item.matItem.MatItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.level.gameObject.FenceGateObject;
import necesse.level.gameObject.FenceObject;
import necesse.level.gameObject.PaintingObject;
import necesse.level.gameObject.StatueObject;
import necesse.level.gameObject.StreetlampObject;
import necesse.level.gameObject.WallObject;
import necesse.level.gameObject.furniture.FurnitureObject;
import necesse.level.gameTile.SimpleFloorTile;
import stairwaytoheaven.objects.SkyDecoObject;
import stairwaytoheaven.objects.SkyWallLightObject;

/**
 * The "Nightfell & Skylight" building set (v0.2): walls with doors and
 * windows, floors, fence + gate, lights, and gothic-sky deco. Craftable
 * pieces use sky materials; garland, cat basket and banner stay
 * quest-exclusive rewards.
 */
final class SkyBuildingSet {

    private SkyBuildingSet() {
    }

    static void register() {
        // ===== Walls (wall + door + window each, vanilla helper) =====
        int[] skystoneWall = WallObject.registerWallObjects(
                "skystonebrick", "skystonebrickwall", 2.0F, new Color(150, 158, 172), -1.0F, -1.0F);
        SkyRegistry.skystoneBrickWallID = skystoneWall[0];
        int[] nightfellWall = WallObject.registerWallObjects(
                "nightfell", "nightfellwall", 2.0F, new Color(52, 48, 66), -1.0F, -1.0F);
        SkyRegistry.nightfellWallID = nightfellWall[0];

        // ===== Floors =====
        // Checkerboard: the pattern is locked to world coordinates so it runs
        // continuously across separately built rooms, which is why this floor
        // deliberately ships WITHOUT a _splat file — a splat atlas randomizes
        // the cells per tile and would destroy the checker.
        //
        // That also makes it the only tile in the game to reach
        // TerrainSplatterTile's legacy splatting path, where vanilla's
        // SimpleTiledFloorTile crashes on negative tile coordinates. See
        // CheckerFloorTile for the stacktrace and the one-line reason.
        SkyRegistry.marbleCheckerID = TileRegistry.registerTile("marblecheckertile",
                new stairwaytoheaven.tiles.CheckerFloorTile("marblechecker", new Color(130, 128, 138)), 1.0F, true);
        SkyRegistry.gloomwoodFloorID = TileRegistry.registerTile("gloomwoodfloortile",
                new SimpleFloorTile("gloomwoodfloor", new Color(66, 52, 60)), 1.0F, true);
        // v0.4: one buildable plank floor per sky wood
        SkyRegistry.nimbusFloorID = TileRegistry.registerTile("nimbusfloortile",
                new SimpleFloorTile("nimbusfloor", new Color(150, 136, 124)), 1.0F, true);
        SkyRegistry.charFloorID = TileRegistry.registerTile("charfloortile",
                new SimpleFloorTile("charfloor", new Color(62, 58, 72)), 1.0F, true);
        SkyRegistry.prismFloorID = TileRegistry.registerTile("prismfloortile",
                new SimpleFloorTile("prismfloor", new Color(210, 196, 210)), 1.0F, true);

        // ===== Fence + gate (vanilla ironfence registration pattern) =====
        SkyRegistry.skyironFenceID = ObjectRegistry.registerObject("skyironfence",
                new FenceObject("skyironfence", new Color(62, 66, 80), 12, 10, -26), 2.0F, true);
        FenceGateObject.registerGatePair(SkyRegistry.skyironFenceID, "skyironfencegate", "skyironfencegate",
                new Color(62, 66, 80), 12, 10, 4.0F);

        // ===== Lights =====
        SkyRegistry.wardenCandelabraID = ObjectRegistry.registerObject("wardencandelabra",
                new StreetlampObject(), 30.0F, true);
        // v0.3: green-flame ghost lantern (the Veil signature light)
        SkyRegistry.ghostLanternID = ObjectRegistry.registerObject("ghostlantern",
                new StreetlampObject(), 30.0F, true);
        ObjectRegistry.registerObject("mistglasslantern",
                new SkyWallLightObject("mistglasslantern", 150, 0.52F, 0.25F), 10.0F, true);
        // Quest gift: warm flickering colored lights (obtainable so the item
        // exists, but no recipe — the Warden hands them out)
        SkyRegistry.flickerGarlandID = ObjectRegistry.registerObject("flickerlightgarland",
                new SkyWallLightObject("flickerlightgarland", 120, 0.10F, 0.45F), 50.0F, true);

        // ===== Deco =====
        ObjectRegistry.registerObject("gloomravenstatue",
                new StatueObject("gloomraven", 16, 1), 20.0F, true);
        ObjectRegistry.registerObject("gloomwillow",
                new SkyDecoObject("gloomwillow", 64, new Color(60, 48, 56),
                        new Rectangle(8, 12, 16, 16), "objects", "landscaping", "plants")
                        .setTool(ToolType.AXE), 15.0F, true);

        // ===== v0.6 prop families (tools/asset_generator/gen_props.py) =====
        // Tool behaviour is audited against the nearest vanilla archetype:
        // soft/fabric clutter breaks like vanilla clutter (ToolType.ALL),
        // woody pieces need the axe, and stone/crystal/machinery keeps the
        // GameObject pickaxe default exactly like statues and crystal clusters.
        // Spire hero accents — the observatory instruments the Spire layout
        // can build its landmark read from.
        ObjectRegistry.registerObject("skywatchtelescope",
                new SkyDecoObject("skywatchtelescope", 32, new Color(204, 160, 82),
                        new Rectangle(8, 40, 16, 24), "objects", "decorations"), 25.0F, true);
        ObjectRegistry.registerObject("skywatchastrolabe",
                new SkyDecoObject("skywatchastrolabe", 32, new Color(204, 160, 82),
                        new Rectangle(4, 40, 24, 12), "objects", "decorations"), 25.0F, true);
        // Stormveil environmental props — small reusable pieces worldgen can
        // compose later; craftable now so builders can place them.
        ObjectRegistry.registerObject("stormscreed",
                new SkyDecoObject("stormscreed", 32, new Color(66, 60, 95),
                        null, "objects", "decorations")
                        .setTool(ToolType.ALL).setObjectHealth(1), 0.0F, false);
        ObjectRegistry.registerObject("skywatchrubble",
                new SkyDecoObject("skywatchrubble", 32, new Color(126, 138, 154),
                        new Rectangle(8, 12, 16, 20), "objects", "decorations"), 0.0F, false);
        ObjectRegistry.registerObject("chargecrystal",
                new SkyDecoObject("chargecrystal", 32, new Color(122, 108, 210),
                        new Rectangle(10, 20, 12, 12), "objects", "decorations")
                        .setLight(70, 0.72F, 0.45F), 5.0F, true);
        ObjectRegistry.registerObject("withershrub",
                new SkyDecoObject("withershrub", 32, new Color(62, 58, 72),
                        new Rectangle(12, 24, 8, 8), "objects", "decorations")
                        .setTool(ToolType.ALL).setObjectHealth(1), 0.0F, false);
        // Aurora Shoals accents — the same restrained teal/rose language.
        ObjectRegistry.registerObject("aurorashards",
                new SkyDecoObject("aurorashards", 32, new Color(214, 130, 172),
                        new Rectangle(10, 20, 12, 10), "objects", "decorations")
                        .setLight(70, 0.90F, 0.40F), 5.0F, true);
        ObjectRegistry.registerObject("starfall",
                new SkyDecoObject("starfall", 32, new Color(136, 216, 206),
                        new Rectangle(10, 16, 12, 12), "objects", "decorations")
                        .setLight(80, 0.50F, 0.40F), 10.0F, true);
        // Sky oddities: rare-encounter SEEDS. Registered + craftable so they
        // exist and map builders can place them, but deliberately absent from
        // normal worldgen (docs/DESIGN.md keeps rare discoveries special).
        ObjectRegistry.registerObject("skyballoon",
                new SkyDecoObject("skyballoon", 32, new Color(196, 206, 216),
                        null, "objects", "decorations")
                        .setTool(ToolType.ALL), 0.0F, false);
        ObjectRegistry.registerObject("aeronautwreck",
                new SkyDecoObject("aeronautwreck", 48, new Color(122, 96, 72),
                        new Rectangle(8, 24, 32, 24), "objects", "decorations")
                        .setTool(ToolType.AXE), 0.0F, false);
        ObjectRegistry.registerObject("skyparcel",
                new SkyDecoObject("skyparcel", 32, new Color(122, 96, 72),
                        new Rectangle(6, 14, 20, 14), "objects", "decorations")
                        .setTool(ToolType.ALL).setObjectHealth(1), 0.0F, false);
        // Natural props must survive the shore sweep if worldgen later places
        // them near the Mistsea (same reason SkyObjects calls allowShore).
        for (String propId : new String[]{"stormscreed", "skywatchrubble",
                "chargecrystal", "withershrub", "aurorashards", "starfall"}) {
            ObjectRegistry.getObject(ObjectRegistry.getObjectID(propId)).canPlaceOnShore = true;
        }

        FurnitureObject catBasket = new FurnitureObject();
        catBasket.furnitureType = "petbed";
        SkyRegistry.catBasketID = ObjectRegistry.registerObject("catbasket", catBasket, 50.0F, true);

        PaintingObject banner = new PaintingObject(Item.Rarity.RARE);
        banner.texturePath = "skywatchbanner";
        SkyRegistry.skywatchBannerID = ObjectRegistry.registerObject("skywatchbanner", banner, 80.0F, true);

        // ===== Quest structure pieces (not player-obtainable) =====
        // UNBREAKABLE: mining the beacon/anchor would drop nothing and soft-lock
        // the quest chain, so a pickaxe must not touch them.
        SkyDecoObject beaconOff = new SkyDecoObject("wardenbeaconoff", 32,
                new Color(90, 96, 110), new Rectangle(4, 8, 24, 20)).setTool(ToolType.UNBREAKABLE);
        SkyRegistry.wardenBeaconOffID = ObjectRegistry.registerObject("wardenbeaconoff", beaconOff, 0.0F, false);
        SkyDecoObject beaconOn = new SkyDecoObject("wardenbeaconon", 32,
                new Color(186, 226, 230), new Rectangle(4, 8, 24, 20))
                .setTool(ToolType.UNBREAKABLE).setLight(180, 0.52F, 0.30F);
        SkyRegistry.wardenBeaconOnID = ObjectRegistry.registerObject("wardenbeaconon", beaconOn, 0.0F, false);
        SkyDecoObject anchor = new SkyDecoObject("skyanchor", 32,
                new Color(86, 178, 186), new Rectangle(4, 10, 24, 18))
                .setTool(ToolType.UNBREAKABLE).setLight(100, 0.50F, 0.25F);
        SkyRegistry.skyAnchorID = ObjectRegistry.registerObject("skyanchor", anchor, 0.0F, false);
    }

    static void registerItems() {
        ItemRegistry.registerItem("cloudpufftreat",
                new MatItem(50, Item.Rarity.UNCOMMON, "cloudpufftreattip").setItemCategory("materials", "mobdrops"), 5.0F, true);
        ItemRegistry.registerItem("silverbell",
                new MatItem(10, Item.Rarity.EPIC, "silverbelltip").setItemCategory("materials", "minerals"), 250.0F, true);
    }

    /** Recipes for the craftable half of the set (postInit). */
    static void registerRecipes() {
        // Walls & floors: cheap in sky materials, workstation tier
        Recipes.registerModRecipe(new Recipe("skystonebrickwall", 4, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 2}}")));
        // Doors and windows complete the wall kits (the objects/items already
        // exist via WallObject.registerWallObjects — they were just uncraftable)
        Recipes.registerModRecipe(new Recipe("skystonebrickdoor", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 3}}")));
        Recipes.registerModRecipe(new Recipe("skystonebrickwindow", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 2}}")));
        Recipes.registerModRecipe(new Recipe("nightfellwall", 4, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 2}, {stormshard, 1}}")));
        Recipes.registerModRecipe(new Recipe("nightfelldoor", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 2}, {stormshard, 1}}")));
        Recipes.registerModRecipe(new Recipe("nightfellwindow", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 1}, {stormshard, 1}}")));
        Recipes.registerModRecipe(new Recipe("marblecheckertile", 6, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 2}}")));
        Recipes.registerModRecipe(new Recipe("gloomwoodfloortile", 6, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{anylog, 2}, {stormshard, 1}}")));
        // v0.4: sky wood floors from their own logs
        Recipes.registerModRecipe(new Recipe("nimbusfloortile", 6, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{nimbuswood, 2}}")));
        Recipes.registerModRecipe(new Recipe("charfloortile", 6, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{charwood, 2}}")));
        Recipes.registerModRecipe(new Recipe("prismfloortile", 6, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{prismwood, 2}}")));
        Recipes.registerModRecipe(new Recipe("skyironfence", 4, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{ironbar, 1}, {skystone, 1}}")));
        Recipes.registerModRecipe(new Recipe("skyironfencegate", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{ironbar, 2}, {skystone, 1}}")));
        Recipes.registerModRecipe(new Recipe("ghostlantern", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{aurorapetal, 2}, {skystone, 8}}")));
        Recipes.registerModRecipe(new Recipe("wardencandelabra", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{ironbar, 2}, {stormshard, 2}, {skystone, 2}}")));
        Recipes.registerModRecipe(new Recipe("mistglasslantern", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{ironbar, 1}, {stormshard, 1}}")));
        Recipes.registerModRecipe(new Recipe("gloomravenstatue", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 12}, {stormshard, 2}}")));
        Recipes.registerModRecipe(new Recipe("gloomwillow", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{anylog, 6}, {stormshard, 1}}")));
        // v0.6 prop families (craftable so builders can compose with them;
        // the oddities stay out of worldgen — see the registration note above)
        Recipes.registerModRecipe(new Recipe("skywatchtelescope", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{ironbar, 4}, {aetheriumore, 2}, {skystone, 6}}")));
        Recipes.registerModRecipe(new Recipe("skywatchastrolabe", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{ironbar, 2}, {stormshard, 2}, {skystone, 4}}")));
        Recipes.registerModRecipe(new Recipe("stormscreed", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 1}, {stormshard, 1}}")));
        Recipes.registerModRecipe(new Recipe("skywatchrubble", 2, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 3}}")));
        Recipes.registerModRecipe(new Recipe("chargecrystal", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{stormshard, 2}, {skystone, 2}}")));
        Recipes.registerModRecipe(new Recipe("withershrub", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{charwood, 2}}")));
        Recipes.registerModRecipe(new Recipe("aurorashards", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{aurorapetal, 2}, {skystone, 2}}")));
        Recipes.registerModRecipe(new Recipe("starfall", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{prismshard, 2}, {skystone, 1}}")));
        Recipes.registerModRecipe(new Recipe("skyballoon", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{windsilk, 3}, {ironbar, 1}, {anylog, 1}}")));
        Recipes.registerModRecipe(new Recipe("aeronautwreck", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{anylog, 4}, {ironbar, 2}}")));
        Recipes.registerModRecipe(new Recipe("skyparcel", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{anylog, 2}, {ironbar, 1}}")));
        // The Warden teaches this one in the story, but it is craftable from
        // the start — the gate is finding him and learning what it's for.
        Recipes.registerModRecipe(new Recipe("cloudpufftreat", 3, RecipeTechRegistry.NONE,
                Recipes.ingredientsFromScript("{{windsilk, 1}, {aurorapetal, 2}}")));
    }
}
