package stairwaytoheaven;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SimpleSetBonusBuff;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.SimpleTrinketBuff;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import stairwaytoheaven.items.GalehowlProjectileToolItem;
import stairwaytoheaven.items.SkyMatItem;
import stairwaytoheaven.items.SkyTrinketItem;
import stairwaytoheaven.items.StormsteelArmor;
import stairwaytoheaven.items.TempestEdgeSwordToolItem;

/**
 * Items and recipes of the Skyreach. Tuning reference: vanilla Tungsten tier
 * (see docs/DESIGN.md §5-§7 and docs/research/game-progression-reference.md).
 */
public final class SkyItems {
    /**
     * Every item this mod registers, for the live category probe in
     * {@code SkyreachStatusCommand}. A plain list because the registry
     * has no "items from this mod" query, and a probe that silently
     * skips an item is worse than no probe. Kept honest by
     * {@code tools/item_category_audit.py}, which fails if this list and
     * the registerItem calls in the source drift apart.
     */
    public static final String[] ALL_ITEM_IDS = {
            "aetheriumbar", "aetheriumore", "auroralocket", "aurorafleece", "aurorapetal",
            "charwood", "cinderpearl", "cloudberry", "cloudcustard", "cloudpufftreat",
            "cloudwood", "dewsnail", "fulgurite", "galehowl", "glimmerstrides",
            "nimbusdraught", "nimbusmilk", "nimbuswood", "prismcaller", "prismshard",
            "prismwood", "seraphwood", "silverbell", "skycurd", "skyreave", "skystone",
            "skywatchhood", "skywatchwhistle", "skyweave", "stormdisc", "stormdown",
            "stormglass", "stormshard", "stormsteelbar", "stormsteelboots",
            "stormsteelchestplate", "stormsteelhelmet", "stormsteelvambrace",
            "tempestedge", "thunderhead", "thunderplume", "veilessence", "wardenboots",
            "wardenmantle", "windsilk", "zephyrharness"
    };


    private SkyItems() {
    }

    static void register() {
        // Materials
        ItemRegistry.registerItem("skystone",
                new SkyMatItem(500).setItemCategory("materials", "minerals"), 2.0F, true);
        ItemRegistry.registerItem("aetheriumore",
                new SkyMatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "ore"), 8.0F, true);
        ItemRegistry.registerItem("aetheriumbar",
                new SkyMatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "bars"), 25.0F, true);
        // Worth (incursion rebalance): this pass prices the Skyreach's
        // materials against an incursion's own loot instead of the tungsten
        // tier they were first tuned to.
        //
        // VERIFIED [jar]: the entire biome loot of the first (tier-1)
        // incursion is `deepstone` 0.1F, `tungstenore` 6.0F, `ectoplasm`
        // 12.0F, `shadowessence` 25.0F, `upgradeshard` 8.0F and
        // `alchemyshard` 8.0F (JournalRegistry.java:265). The crystal is the
        // dear one, and it comes off the two sources a Storm Shard does: a
        // node you mine — `shadowessencedeeprock`, a RockOreObject dropping
        // one per rock (ObjectRegistry.java:1538-1544) — and the mobs, where
        // every boss adds one and the incursion's own boss 20-25
        // (ForestDeepCaveIncursionBiome.java:50, :98). `shadowessence`
        // registers at brokerValue 25.0F (ItemRegistry.java:977).
        //
        // Storm Shards, Fulgurite and Prism Shards are the Skyreach's version
        // of that crystal and sat at 12.0F, under half the tier-1 floor.
        // Anchor: `shadowessence` 25.0F.
        ItemRegistry.registerItem("stormshard",
                new SkyMatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 25.0F, true);
        ItemRegistry.registerItem("windsilk",
                new SkyMatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 12.0F, true);
        // A PETAL, not a mineral. Vanilla files every picked flower under
        // materials/flowers -- FlowerObject.java:50 sets that category for the
        // whole family, and ItemRegistry.java:1954 puts mushroom there by hand
        // -- and materials/flowers is created by the engine itself
        // (ItemCategory.java:220, sort key C-G-A). Aurora Petals came off an
        // aurorabloom, so they sort with the flowers.
        ItemRegistry.registerItem("aurorapetal",
                new SkyMatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "flowers"), 15.0F, true);
        // The Dew Snail itself, in hand. NetToolItem.hitMob ends with
        // target.remove(0, 0, attacker, true) -- it drops the mob's loot table
        // and nothing else, so whatever that table says IS what catching gives
        // you. Ours said "35% chance of a prismshard", which meant two catches
        // in three returned nothing at all and the snail simply vanished.
        // Vanilla's netted critters drop THEMSELVES, always: FireflyMob's whole
        // loot table is `new LootTable(new LootItem(itemStringID))`. This is
        // that item.
        ItemRegistry.registerItem("dewsnail",
                new SkyMatItem(50, Item.Rarity.UNCOMMON).setItemCategory("materials", "mobdrops"), 20.0F, true);

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
        //
        // Worth (incursion rebalance): the Veil is the mod's second realm and
        // its dead sit around incursion tier 7, so its two materials are
        // priced a rung above the Skyreach's tier-1 floor. VERIFIED [jar]:
        // vanilla runs that ladder itself, in EssenceMatItem tiers — tier 1
        // (`shadowessence` and its three siblings) 25.0F, tier 2
        // (`slimeessence`, `bloodessence`, `spideressence`) 30.0F, tier 3
        // (`crystalessence`, `radiatedessence`) 35.0F, ItemRegistry.java:977,
        // :1001, :1010 — so the Veil is priced on the rungs rather than on a
        // flat multiplier, which after the Skyreach floor moved to 25.0F would
        // have left the two realms 8% apart.
        //
        // Veil Essence is the realm's signature drop: Gloom Shades, Fen
        // Wraiths and Cinder Cantors all pay in it and the Crooked House chest
        // holds 2-6. Anchor: `crystalessence` 35.0F, vanilla's top essence —
        // ours lands just under it at 34.0F, which is also exactly the realm's
        // 1.9 on the 18.0F it carried.
        ItemRegistry.registerItem("veilessence",
                new SkyMatItem(500, Item.Rarity.RARE).setItemCategory("materials", "mobdrops"), 34.0F, true);
        // The Cinder Pearl is the Veil's common mineral — knocked out of
        // ashbones and dropped by the same wraiths — so it takes the rung
        // below. Anchor: `slimeessence`, the tier-2 essence, at brokerValue
        // 30.0F (ItemRegistry.java:1001): a clear step above the Skyreach's
        // 25.0F crystals and below the Veil Essence above it.
        ItemRegistry.registerItem("cinderpearl",
                new SkyMatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 30.0F, true);

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
                new SkyMatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
        ItemRegistry.registerItem("seraphwood",
                new SkyMatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
        ItemRegistry.registerItem("nimbuswood",
                new SkyMatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
        // Charwood is the Veil's building wood, but it deliberately does NOT
        // take the Veil's x1.9: the fulgurpine it drops from grows in the
        // Skyreach's own Stormveil (SkyTerrainPainter.java:499, 1086), and
        // VERIFIED [jar] vanilla prices every log flat at 2.0F no matter how
        // deep it grows — `oaklog`, `deadwoodlog` and even `dryadlog` are all
        // 2.0F (ItemRegistry.java:845-855). Realm worth does not belong in a
        // material the player can farm by the stack.
        ItemRegistry.registerItem("charwood",
                new SkyMatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
        ItemRegistry.registerItem("prismwood",
                new SkyMatItem(500, "anylog").setItemCategory("materials", "logs"), 2.0F, true);
        // Both are mined out of a Skyreach rock (fulguriterock /
        // prismshardrock, SkyObjects.java:197-201), so both take the same
        // anchor as the Storm Shard above: vanilla `shadowessence` 25.0F
        // (ItemRegistry.java:977), the crystal a tier-1 incursion's essence
        // rock drops.
        ItemRegistry.registerItem("fulgurite",
                new SkyMatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 25.0F, true);
        ItemRegistry.registerItem("prismshard",
                new SkyMatItem(250, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"), 25.0F, true);

        registerGear();
    }

    /**
     * The buffs behind the Skyreach's accessories and armour set.
     *
     * <p>{@code BuffRegistry.registerBuff} (BuffRegistry.java:968) refuses only
     * client-side-only mods; ours is not one. The registry closes with every
     * other registry right after {@code init()}, and both
     * {@code SimpleTrinketItem.getBuffs} and {@code SetHelmetArmorItem.getSetBuff}
     * resolve their buff by string ID at USE time, so the order inside
     * {@code init()} does not matter — only that it is inside it.
     *
     * <p>Every {@code SimpleTrinketBuff} here is built WITHOUT a tooltip key.
     * That is the variant whose {@code getTrinketTooltip} walks the modifiers
     * and asks the engine for each one's own tooltip
     * (SimpleTrinketBuff.java:49), so the numbers in the tooltip are the
     * numbers in this file and cannot drift; the flavour sentence comes from
     * {@code itemtooltip.<id>tip} instead, like every other mod item.
     */
    private static void registerBuffs() {
        // Stormsteel Vambrace. Anchors: vanilla `vambrace`
        // (BuffRegistry.java:815, RESILIENCE_GAIN 0.5F) and vanilla
        // `chainshirt` (:816, MAX_RESILIENCE_FLAT 50). Ours is the vambrace at
        // full value plus half a chainshirt — vanilla itself combines the two
        // at full value into `manica`, which is EPIC, so this sits deliberately
        // between them.
        BuffRegistry.registerBuff("stormsteelvambracetrinket", new SimpleTrinketBuff(
                new ModifierValue<>(BuffModifiers.RESILIENCE_GAIN, 0.5F),
                new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 25)));

        // Aurora Locket. Anchors: vanilla `frozenheart` (:754,
        // MAX_HEALTH_FLAT 50, UNCOMMON) and vanilla `regenpendant`
        // (COMBAT_HEALTH_REGEN_FLAT 0.5F, COMMON). Ours takes 60% of the
        // heart's health and all of the pendant's regen in one slot.
        BuffRegistry.registerBuff("auroralockettrinket", new SimpleTrinketBuff(
                new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 30),
                new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 0.5F)));

        // Zephyr Harness. Anchors: vanilla `trackerboot` (:679, SPEED 0.1F) and
        // vanilla `zephyrcharm` (:682, STAMINA_CAPACITY 0.5F). Ours is the
        // boot's speed at full value and 60% of the charm's stamina.
        BuffRegistry.registerBuff("zephyrharnesstrinket", new SimpleTrinketBuff(
                new ModifierValue<>(BuffModifiers.SPEED, 0.10F),
                new ModifierValue<>(BuffModifiers.STAMINA_CAPACITY, 0.30F)));

        // The Stormsteel set bonus. Anchor: GlacialHelmetBonusBuff, the NEXT
        // tier's set, which grants MAX_RESILIENCE_FLAT 20 + RESILIENCE_GAIN
        // 0.2F (plus icicles). Ours takes less of the resilience and spends the
        // rest on being light.
        BuffRegistry.registerBuff(StormsteelArmor.SET_BONUS, new SimpleSetBonusBuff(
                new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 15),
                new ModifierValue<>(BuffModifiers.SPEED, 0.05F)));
    }

    /**
     * What the Skyreach's materials are FOR: one armour set and three
     * accessories. Every value is calibrated in {@link StormsteelArmor} and in
     * {@link #registerBuffs()} against the named vanilla item.
     *
     * <p>Broker values: tungsten's armour is 110 / 160 / 80 and its trinket
     * band runs 100–300, so these sit just above their anchors.
     */
    private static void registerGear() {
        registerBuffs();

        ItemRegistry.registerItem("stormsteelhelmet", new StormsteelArmor.Helmet(), 130.0F, true);
        ItemRegistry.registerItem("stormsteelchestplate", new StormsteelArmor.Chestplate(), 190.0F, true);
        ItemRegistry.registerItem("stormsteelboots", new StormsteelArmor.Boots(), 95.0F, true);

        // Rarity follows the anchor: vanilla `vambrace` is RARE, `frozenheart`
        // and `zephyrcharm` are UNCOMMON. Enchant costs sit at the top of the
        // vanilla trinket band (200–600).
        ItemRegistry.registerItem("stormsteelvambrace",
                new SkyTrinketItem(Item.Rarity.RARE, "stormsteelvambracetrinket", 500), 180.0F, true);
        ItemRegistry.registerItem("auroralocket",
                new SkyTrinketItem(Item.Rarity.UNCOMMON, "auroralockettrinket", 500), 260.0F, true);
        ItemRegistry.registerItem("zephyrharness",
                new SkyTrinketItem(Item.Rarity.UNCOMMON, "zephyrharnesstrinket", 400), 160.0F, true);
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

        registerGearRecipes();
    }

    /**
     * The recipes that give the mod's own materials somewhere to go.
     *
     * <p>Stormsteel used to be a dead end: the Aether Forge was the only place
     * in the game that made it and nothing in the game consumed it, so the
     * station's headline product bought the player nothing. The armour set and
     * the vambrace are its four consumers.
     *
     * <p>Techs are vanilla's own split, unchanged: armour is forged at the
     * {@code TUNGSTEN_ANVIL} beside {@code tungstenhelmet} /
     * {@code tungstenchestplate} / {@code tungstenboots} (Recipes.java:984-986),
     * and trinkets are assembled at the {@code TUNGSTEN_WORKSTATION} beside
     * {@code manica}, {@code lifependant}, {@code spellstone} and
     * {@code bonehilt} (:944-950). The player owns both by the time the sky
     * exists — the Stairway itself is a Tungsten Workstation recipe.
     *
     * <p>Costs are scaled off tungsten's own: helmet 12 bars + 12 obsidian,
     * chest 16 + 16, boots 8 + 8. One Stormsteel bar costs 4 Aetherium Ore and
     * a Storm Shard at the Aether Forge, so the bar counts are lower and the
     * second ingredient is the sky's own glass and cloth.
     */
    private static void registerGearRecipes() {
        Recipes.registerModRecipe(new Recipe(
                "stormsteelhelmet", 1, RecipeTechRegistry.TUNGSTEN_ANVIL,
                Recipes.ingredientsFromScript("{{stormsteelbar, 8}, {stormglass, 10}, {skyweave, 4}}")));
        Recipes.registerModRecipe(new Recipe(
                "stormsteelchestplate", 1, RecipeTechRegistry.TUNGSTEN_ANVIL,
                Recipes.ingredientsFromScript("{{stormsteelbar, 11}, {stormglass, 14}, {skyweave, 6}}")));
        Recipes.registerModRecipe(new Recipe(
                "stormsteelboots", 1, RecipeTechRegistry.TUNGSTEN_ANVIL,
                Recipes.ingredientsFromScript("{{stormsteelbar, 6}, {stormglass, 8}, {skyweave, 3}}")));

        Recipes.registerModRecipe(new Recipe(
                "stormsteelvambrace", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{stormsteelbar, 6}, {skyweave, 4}, {aetheriumbar, 2}}")));
        Recipes.registerModRecipe(new Recipe(
                "auroralocket", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{aurorapetal, 10}, {aurorafleece, 6}, {stormglass, 4}}")));
        Recipes.registerModRecipe(new Recipe(
                "zephyrharness", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{windsilk, 12}, {stormdown, 8}, {skyweave, 4}}")));

        // The Dew Snail was the mod's other dead end: a critter the player can
        // catch with a net whose item nothing wanted. Siggi and Peanut want it.
        // Same output as the two treat recipes SkyBuildingSet and SkyLivestock
        // already register, at the same tech, so this is a third way to make a
        // treat rather than a new item.
        Recipes.registerModRecipe(new Recipe(
                "cloudpufftreat", 2, RecipeTechRegistry.NONE,
                Recipes.ingredientsFromScript("{{dewsnail, 1}, {windsilk, 1}}")));
    }
}
