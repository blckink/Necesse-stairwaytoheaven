package stairwaytoheaven;

import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.inventory.item.Item;
import necesse.inventory.item.matItem.MatItem;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import stairwaytoheaven.items.GalehowlProjectileToolItem;
import stairwaytoheaven.items.TempestEdgeSwordToolItem;

/**
 * Items and recipes of the Skyreach. Tuning reference: vanilla Tungsten tier
 * (see docs/DESIGN.md §5-§7 and docs/research/game-progression-reference.md).
 */
final class SkyItems {

    private SkyItems() {
    }

    static void register() {
        // Materials
        ItemRegistry.registerItem("skystone",
                new MatItem(500).setItemCategory("materials", "minerals"), 2.0F, true);
        ItemRegistry.registerItem("aetheriumore",
                new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "ore"), 8.0F, true);
        ItemRegistry.registerItem("aetheriumbar",
                new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "bars"), 25.0F, true);
        ItemRegistry.registerItem("stormshard",
                new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 12.0F, true);
        ItemRegistry.registerItem("windsilk",
                new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 12.0F, true);
        ItemRegistry.registerItem("aurorapetal",
                new MatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 15.0F, true);

        // Forage food (v0.2.6): sky berries, eaten raw or composted like
        // vanilla forage crops (sugarbeet pattern).
        ItemRegistry.registerItem("cloudberry",
                new necesse.inventory.item.placeableItem.consumableItem.food.FoodMatItem(250, Item.Rarity.NORMAL)
                        .spoilDuration(480), 3.0F, true);

        // v0.3: Veil materials
        ItemRegistry.registerItem("veilessence",
                new MatItem(500, Item.Rarity.RARE).setItemCategory("materials", "mobdrops"), 18.0F, true);
        ItemRegistry.registerItem("cinderpearl",
                new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 14.0F, true);

        // Weapons
        ItemRegistry.registerItem("tempestedge", new TempestEdgeSwordToolItem(), 220.0F, true);
        ItemRegistry.registerItem("galehowl", new GalehowlProjectileToolItem(), 220.0F, true);

        // v0.4 "The Living Sky": logs, flowers, mosses, ores
        ItemRegistry.registerItem("nimbuswood",
                new MatItem(1000, Item.Rarity.NORMAL).setItemCategory("materials", "wood"), 1.5F, true);
        ItemRegistry.registerItem("charwood",
                new MatItem(1000, Item.Rarity.NORMAL).setItemCategory("materials", "wood"), 1.5F, true);
        ItemRegistry.registerItem("prismwood",
                new MatItem(1000, Item.Rarity.NORMAL).setItemCategory("materials", "wood"), 1.5F, true);
        ItemRegistry.registerItem("cloudbell",
                new MatItem(250, Item.Rarity.NORMAL).setItemCategory("materials", "flowers"), 2.0F, true);
        ItemRegistry.registerItem("skytulip",
                new MatItem(250, Item.Rarity.NORMAL).setItemCategory("materials", "flowers"), 2.0F, true);
        ItemRegistry.registerItem("thunderbloom",
                new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "flowers"), 6.0F, true);
        ItemRegistry.registerItem("glowfern",
                new MatItem(250, Item.Rarity.NORMAL).setItemCategory("materials", "flowers"), 3.0F, true);
        ItemRegistry.registerItem("auroralily",
                new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "flowers"), 6.0F, true);
        ItemRegistry.registerItem("staticmoss",
                new MatItem(250, Item.Rarity.NORMAL).setItemCategory("materials", "flowers"), 3.0F, true);
        ItemRegistry.registerItem("fulgurite",
                new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 12.0F, true);
        ItemRegistry.registerItem("prismshard",
                new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 12.0F, true);
    }

    /** Runs in postInit — the mod recipe registry closes right afterwards. */
    static void registerRecipes() {
        // Entry: same tungsten investment as the Deep Cave Ladder, plus a cave
        // catalyst, so the sky unlocks alongside — never before — the deep caves.
        Recipes.registerModRecipe(new Recipe(
                "skystairwaydown", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{tungstenbar, 8}, {quartz, 15}}")));

        Recipes.registerModRecipe(new Recipe(
                "aetheriumbar", 1, RecipeTechRegistry.FORGE,
                Recipes.ingredientsFromScript("{{aetheriumore, 3}}")));

        Recipes.registerModRecipe(new Recipe(
                "tempestedge", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{aetheriumbar, 8}, {stormshard, 5}}")));

        Recipes.registerModRecipe(new Recipe(
                "galehowl", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{aetheriumbar, 4}, {windsilk, 6}, {aurorapetal, 3}}")));

        // Forage loop: spin harvested wheat-grass into windsilk by hand
        Recipes.registerModRecipe(new Recipe(
                "windsilk", 1, RecipeTechRegistry.NONE,
                Recipes.ingredientsFromScript("{{windwheat, 3}}")));

        // The way down: chalk, candlewax-silk and petals — the Silver Bell is
        // the key and stays with the player (checked on use, never consumed)
        Recipes.registerModRecipe(new Recipe(
                "seancecircle", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{stormshard, 6}, {windsilk, 4}, {aurorapetal, 2}}")));
    }
}
