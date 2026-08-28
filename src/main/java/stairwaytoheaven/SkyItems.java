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
        // The Dew Snail itself, in hand. NetToolItem.hitMob ends with
        // target.remove(0, 0, attacker, true) -- it drops the mob's loot table
        // and nothing else, so whatever that table says IS what catching gives
        // you. Ours said "35% chance of a prismshard", which meant two catches
        // in three returned nothing at all and the snail simply vanished.
        // Vanilla's netted critters drop THEMSELVES, always: FireflyMob's whole
        // loot table is `new LootTable(new LootItem(itemStringID))`. This is
        // that item.
        ItemRegistry.registerItem("dewsnail",
                new MatItem(50, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 20.0F, true);

        // Forage food (v0.2.6): sky berries, eaten raw or composted like
        // vanilla forage crops (sugarbeet pattern).
        //
        // ...and, since v0.7, ANIMAL FEED. GrainItem is a FoodMatItem subclass
        // that adds exactly one thing: it is the class vanilla's husbandry
        // system recognises as food. HusbandryMob.canFeed is
        // `item.item instanceof GrainItem` and FeedingTroughObjectEntity's
        // inventory filter is the same check (jar 1.3.2,
        // FeedingTroughObjectEntity.java:182) — a hard class test with no
        // registry or tag behind it, so wheat was the ONLY thing in the game
        // that could go in a trough. A player who caught Cloud Lambs in the sky
        // asked "was muss in Trog bei wolkenschafen?" and the honest answer was
        // "surface wheat, there is nothing up here". Now the berry that grows
        // on Skyreach cloudberry bushes is feed, in the trough and in hand.
        // Everything the berry already was — food, spoil timer, value, icon,
        // both locales — is inherited unchanged.
        ItemRegistry.registerItem("cloudberry",
                new necesse.inventory.item.placeableItem.consumableItem.food.GrainItem(250, Item.Rarity.NORMAL)
                        .spoilDuration(480), 3.0F, true);

        // v0.3: Veil materials
        ItemRegistry.registerItem("veilessence",
                new MatItem(500, Item.Rarity.RARE).setItemCategory("materials", "mobdrops"), 18.0F, true);
        ItemRegistry.registerItem("cinderpearl",
                new MatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 14.0F, true);

        // Weapons
        // The Warden's clothes, as real armor items on the vanilla human body
        // (the Elder pattern). This is what lets him be a HumanMob with a
        // distinctive silhouette instead of a bespoke sprite sheet.
        //
        // Kept behind the same switch as WardenIdentity.dress: an ArmorItem
        // loads its sheets during resource load, and registering one whose
        // player/armor/ sheets do not exist yet would pull in the engine's
        // error texture for no benefit. Flip both together when the art lands.
        if (stairwaytoheaven.mobs.WardenIdentity.armorSheetsExist()) {
            ItemRegistry.registerItem("skywatchhood",
                    new stairwaytoheaven.items.SkywatchArmor.Hood(), 0.0F, true);
            ItemRegistry.registerItem("wardenmantle",
                    new stairwaytoheaven.items.SkywatchArmor.Mantle(), 0.0F, true);
            ItemRegistry.registerItem("wardenboots",
                    new stairwaytoheaven.items.SkywatchArmor.Boots(), 0.0F, true);
        }
        ItemRegistry.registerItem("tempestedge", new TempestEdgeSwordToolItem(), 220.0F, true);
        ItemRegistry.registerItem("galehowl", new GalehowlProjectileToolItem(), 220.0F, true);

        // v0.4 "The Living Sky": logs and ores. The pickable plants are NOT
        // registered here — their GrassObject registration already creates an
        // item of the same stringID (the windwheat pattern), and that auto-item
        // is what the plants drop.
        ItemRegistry.registerItem("cloudwood",
                new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
        ItemRegistry.registerItem("seraphwood",
                new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
        ItemRegistry.registerItem("nimbuswood",
                new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
        ItemRegistry.registerItem("charwood",
                new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
        ItemRegistry.registerItem("prismwood",
                new MatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
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
