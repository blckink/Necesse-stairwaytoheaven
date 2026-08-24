package stairwaytoheaven;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.inventory.item.Item;
import necesse.inventory.item.matItem.MatItem;
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
import necesse.level.gameTile.SimpleTiledFloorTile;
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
        // Checkerboard: SimpleTiledFloorTile locks the pattern to world
        // coordinates; deliberately ships WITHOUT a _splat file (a splat
        // atlas would randomize the cells and destroy the checker).
        SkyRegistry.marbleCheckerID = TileRegistry.registerTile("marblecheckertile",
                new SimpleTiledFloorTile("marblechecker", new Color(130, 128, 138)), 1.0F, true);
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
                new SkyDecoObject("gloomwillow", 48, new Color(60, 48, 56),
                        new Rectangle(8, 12, 16, 16), "objects", "landscaping", "plants"), 15.0F, true);

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
                new Color(90, 96, 110), new Rectangle(4, 8, 24, 20));
        beaconOff.toolType = necesse.inventory.item.toolItem.ToolType.UNBREAKABLE;
        SkyRegistry.wardenBeaconOffID = ObjectRegistry.registerObject("wardenbeaconoff", beaconOff, 0.0F, false);
        SkyDecoObject beaconOn = new SkyDecoObject("wardenbeaconon", 32,
                new Color(186, 226, 230), new Rectangle(4, 8, 24, 20)).setLight(180, 0.52F, 0.30F);
        beaconOn.toolType = necesse.inventory.item.toolItem.ToolType.UNBREAKABLE;
        SkyRegistry.wardenBeaconOnID = ObjectRegistry.registerObject("wardenbeaconon", beaconOn, 0.0F, false);
        SkyDecoObject anchor = new SkyDecoObject("skyanchor", 32,
                new Color(86, 178, 186), new Rectangle(4, 10, 24, 18)).setLight(100, 0.50F, 0.25F);
        anchor.toolType = necesse.inventory.item.toolItem.ToolType.UNBREAKABLE;
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
        Recipes.registerModRecipe(new Recipe("nightfellwall", 4, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 2}, {stormshard, 1}}")));
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
                Recipes.ingredientsFromScript("{{cinderpearl, 2}, {skystone, 8}}")));
        Recipes.registerModRecipe(new Recipe("wardencandelabra", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{ironbar, 2}, {stormshard, 2}, {skystone, 2}}")));
        Recipes.registerModRecipe(new Recipe("mistglasslantern", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{ironbar, 1}, {stormshard, 1}}")));
        Recipes.registerModRecipe(new Recipe("gloomravenstatue", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 12}, {stormshard, 2}}")));
        Recipes.registerModRecipe(new Recipe("gloomwillow", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{anylog, 6}, {stormshard, 1}}")));
        // The Warden teaches this one in the story, but it is craftable from
        // the start — the gate is finding him and learning what it's for.
        Recipes.registerModRecipe(new Recipe("cloudpufftreat", 3, RecipeTechRegistry.NONE,
                Recipes.ingredientsFromScript("{{windsilk, 1}, {aurorapetal, 2}}")));
    }
}
