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

        // Weapons
        ItemRegistry.registerItem("tempestedge", new TempestEdgeSwordToolItem(), 220.0F, true);
        ItemRegistry.registerItem("galehowl", new GalehowlProjectileToolItem(), 220.0F, true);
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
    }
}
