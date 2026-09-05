package stairwaytoheaven.realms.ghost;

import java.awt.Color;
import java.awt.Rectangle;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.registries.WorldPresetRegistry;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SimpleSetBonusBuff;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.inventory.recipe.Tech;

/** Registers the complete playable core of the Aftergarden in one place. */
public final class GhostRealm {
    private GhostRealm() {
    }

    public static AftergardenBiome aftergarden;
    public static BoneOrchardBiome boneOrchard;
    public static EctomarshBiome ectomarsh;

    public static GhostGroundTile hauntedGrassTile;
    public static GhostGroundTile ghostMossTile;
    public static GhostGroundTile violetDirtTile;

    public static int hauntedGrassID;
    public static int ghostMossID;
    public static int violetDirtID;
    public static int spiritStoneID;
    public static int blackCobbleID;
    public static int graveyardSoilID;
    public static int ectoplasmID;

    public static int crookedDeadTreeID;
    public static int bonewoodTreeID;
    public static int spiritWillowID;
    public static int lanternTreeID;
    public static int ghostLilyID;
    public static int mourningRoseID;
    public static int ectoplasmFernID;
    public static int widowVineID;
    public static int spiritMushroomID;
    public static int ghostRockID;
    public static int spectralOreRockID;
    public static int gravestoneID;
    public static int soulBasinID;
    public static int soulLoomID;
    public static int spiritForgeID;
    public static int gateDownID;
    public static int gateUpID;

    public static Tech SOUL_LOOM;
    public static Tech SPIRIT_FORGE;

    public static void register() {
        registerBiomes();
        registerTiles();
        registerItems();
        registerObjects();
        registerMobs();
        WorldPresetRegistry.registerPreset("swh_ghostrealm", new GhostWorldPreset());
    }

    private static void registerBiomes() {
        aftergarden = BiomeRegistry.registerBiome("aftergarden", new AftergardenBiome(), false);
        boneOrchard = BiomeRegistry.registerBiome("boneorchard", new BoneOrchardBiome(), false);
        ectomarsh = BiomeRegistry.registerBiome("ectomarsh", new EctomarshBiome(), false);
    }

    private static void registerTiles() {
        hauntedGrassTile = new GhostGroundTile("murkmoss", new Color(77, 116, 72), 100, true);
        ghostMossTile = new GhostGroundTile("swampgrass", new Color(45, 105, 92), 101, true);
        violetDirtTile = new GhostGroundTile("cryptash", new Color(86, 61, 100), 102, true);
        hauntedGrassID = TileRegistry.registerTile("hauntedgrasstile", hauntedGrassTile, 0.0F, false, false, true);
        ghostMossID = TileRegistry.registerTile("ghostmosstile", ghostMossTile, 0.0F, false, false, true);
        violetDirtID = TileRegistry.registerTile("violetdirttile", violetDirtTile, 0.0F, false, false, true);
        spiritStoneID = TileRegistry.registerTile("spiritstonetile",
                new GhostGroundTile("stonebrickfloor", new Color(35, 75, 78), 103, false), 0.0F, false, false, true);
        blackCobbleID = TileRegistry.registerTile("blackcobbletile",
                new GhostGroundTile("ravenfloor", new Color(28, 27, 38), 104, false), 0.0F, false, false, true);
        graveyardSoilID = TileRegistry.registerTile("graveyardsoiltile",
                new GhostGroundTile("swamprock", new Color(61, 73, 65), 99, true), 0.0F, false, false, true);
        ectoplasmID = TileRegistry.registerTile("ectoplasmtile", new EctoplasmTile(), 0.0F, false);
    }

    private static void registerItems() {
        ItemRegistry.registerItem("bonewood", new GhostMatItem("deadwoodlog", 500, Item.Rarity.UNCOMMON)
                .setItemCategory("materials", "logs"), 4.0F, true);
        ItemRegistry.registerItem("soulthread", new GhostMatItem("clothscraps", 500, Item.Rarity.RARE)
                .setItemCategory("materials", "mobdrops"), 30.0F, true);
        ItemRegistry.registerItem("spectralore", new GhostMatItem("nightsteelore", 250, Item.Rarity.RARE)
                .setItemCategory("materials", "ore"), 35.0F, true);
        ItemRegistry.registerItem("spiritsteelbar", new GhostMatItem("nightsteelbar", 250, Item.Rarity.EPIC)
                .setItemCategory("materials", "bars"), 55.0F, true);
        ItemRegistry.registerItem("spiritsteelhelmet", new SpiritsteelHelmet(), 220.0F, true);
        ItemRegistry.registerItem("spiritsteelchestplate", new SpiritsteelChestplate(), 300.0F, true);
        ItemRegistry.registerItem("spiritsteelboots", new SpiritsteelBoots(), 180.0F, true);
        // The realm's two WEAPONS (docs/FOGKEY_AND_BOSSPORTALS.md A3). Until
        // now the Aftergarden's own metal armoured you and nothing more; the
        // Ghost Guide had nothing to sell and the elite drop table paid only in
        // materials. Each weapon's own class comment names the vanilla weapon
        // of its class it is measured against.
        //
        // Broker values are hand-set, and pinned just BELOW what the Ghost
        // Guide charges in materials, exactly as SkyArsenal.registerItems pins
        // the Skyreach's five: 8 spiritsteelbar at 55.0F is 440, so the Reaver
        // is 430; 12 veilessence at 34.0F is 408, so the Bow is 400. A weapon
        // worth more than its price would make the guide a coin press.
        ItemRegistry.registerItem("spiritsteelreaver", new SpiritsteelReaver(), 430.0F, true);
        ItemRegistry.registerItem("gravewindbow", new GravewindBow(), 400.0F, true);
        BuffRegistry.registerBuff(SpiritsteelHelmet.SET_BONUS, new SimpleSetBonusBuff(
                new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 40),
                new ModifierValue<>(BuffModifiers.SPEED, 0.10F)));
    }

    private static void registerObjects() {
        crookedDeadTreeID = natural("crookeddeadtree", new GhostDecoObject("deadtree", "deadwoodtree", 32,
                new Color(45, 37, 55), new Rectangle(8, 20, 16, 12)).setDrops(new LootTable(LootItem.between("bonewood", 2, 5))));
        bonewoodTreeID = natural("bonewoodtree", new GhostDecoObject("deadwood", "deadwoodtree", 32,
                new Color(59, 55, 69), new Rectangle(8, 20, 16, 12)).setDrops(new LootTable(LootItem.between("bonewood", 3, 7))));
        spiritWillowID = natural("spiritwillow", new GhostDecoObject("willowtree", "willowtree", 32,
                new Color(45, 110, 102), new Rectangle(8, 20, 16, 12)).setDrops(new LootTable(LootItem.between("bonewood", 2, 5))));
        lanternTreeID = natural("lanterntree", new GhostDecoObject("gloomwillow", "willowtree", 32,
                new Color(95, 167, 132), new Rectangle(8, 20, 16, 12)).setDrops(new LootTable(
                        LootItem.between("bonewood", 2, 4), ChanceLootItem.between(0.35F, "ectoplasm", 1, 2))));

        ghostLilyID = plant("ghostlily", "aurorabloom", 2, new Color(175, 224, 222), "soulthread");
        mourningRoseID = plant("mourningrose", "cragbloom", 2, new Color(126, 69, 151), "soulthread");
        ectoplasmFernID = plant("ectoplasmfern", "gloomshroom", 2, new Color(61, 176, 149), "ectoplasm");
        widowVineID = plant("widowvine", "withershrub", 2, new Color(76, 126, 87), "soulthread");
        spiritMushroomID = plant("spiritmushroom", "gloomshroom", 2, new Color(72, 191, 168), "ectoplasm");

        ghostRockID = natural("ghostrock", new GhostDecoObject("veilrock", "veilrock", 32,
                new Color(49, 73, 78), new Rectangle(4, 12, 24, 20)).setDrops(new LootTable(LootItem.between("bonewood", 1, 2))));
        spectralOreRockID = natural("spectralorerock", new GhostDecoObject("cryptorerock_nightsteelore",
                "nightsteelore", 32, new Color(77, 133, 126), new Rectangle(4, 12, 24, 20))
                .setDrops(new LootTable(LootItem.between("spectralore", 1, 2))));
        gravestoneID = natural("ghostgravestone", new GhostDecoObject("cryptgravestone1", "cryptgravestone1", 32,
                new Color(72, 70, 85), new Rectangle(5, 15, 22, 17)));

        soulBasinID = ObjectRegistry.registerObject("soulbasin", new SoulBasinObject(), 25.0F, true);
        SOUL_LOOM = RecipeTechRegistry.registerTech("soulloom", "soulloom");
        SPIRIT_FORGE = RecipeTechRegistry.registerTech("spiritforge", "spiritforge");
        soulLoomID = ObjectRegistry.registerObject("soulloom",
                new GhostStationObject("windsilkloom", "caveglowalchemytable", "soulloomtip", SOUL_LOOM), 30.0F, true);
        spiritForgeID = ObjectRegistry.registerObject("spiritforge",
                new GhostStationObject("aetherforge", "forge", "spiritforgetip", SPIRIT_FORGE), 35.0F, true);
        gateDownID = ObjectRegistry.registerObject("ghostgatedown", new GhostGateObject(), 0.0F, false);
        gateUpID = ObjectRegistry.registerObject("ghostgateup", new GhostSideGateObject(), 0.0F, false);
    }

    private static int natural(String id, GhostDecoObject object) {
        return ObjectRegistry.registerObject(id, object, 0.0F, false);
    }

    private static int plant(String id, String texture, int variants, Color color, String drop) {
        return ObjectRegistry.registerObject(id,
                new GhostPlantObject(texture, variants, color, new LootTable(LootItem.between(drop, 1, 2))),
                0.0F, false);
    }

    /**
     * The Aftergarden's seven, all of them now in the player's bestiary.
     *
     * <p>The third argument is {@code countKillStat} (MobRegistry.java:824,
     * VERIFIED [jar]). All seven were {@code false} while the Skyreach's,
     * Steinfeld's and the Crooked Beyond's were {@code true}, so the whole
     * realm was invisible to the journal —
     * {@code docs/AREA_OVERVIEW.md} measured it.
     *
     * <p>Every one of these subclasses a vanilla mob and inherits its draw, so
     * none has a PNG of its own and turning the flag on would have produced
     * seven rows drawn with the engine's ERR tile.
     * {@link stairwaytoheaven.mobs.BorrowedMobIcon} is why that does not
     * happen: each returns the face of the creature it subclasses, and all
     * seven parents ({@code deepcavespirit}, {@code bonewalker},
     * {@code phantom}, {@code forestspector}, {@code mimic}, {@code jackal},
     * {@code desertcrawler}) are themselves bestiary mobs, so their icons
     * provably exist.
     */
    private static void registerMobs() {
        MobRegistry.registerMob("drifter", DrifterMob.class, true);
        MobRegistry.registerMob("headlessbutler", HeadlessButlerMob.class, true);
        MobRegistry.registerMob("lanternwidow", LanternWidowMob.class, true);
        MobRegistry.registerMob("mourningbride", MourningBrideMob.class, true);
        MobRegistry.registerMob("possessedchair", PossessedChairMob.class, true);
        MobRegistry.registerMob("soulhound", SoulHoundMob.class, true);
        MobRegistry.registerMob("coffincrawler", CoffinCrawlerMob.class, true);
    }

    public static void registerRecipes() {
        Recipes.registerModRecipe(new Recipe("soulbasin", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{ectoplasm, 12}, {veilessence, 6}, {bone, 8}}")));
        Recipes.registerModRecipe(new Recipe("soulthread", 2, SOUL_LOOM,
                Recipes.ingredientsFromScript("{{ectoplasm, 3}, {aurorafleece, 2}}")));
        Recipes.registerModRecipe(new Recipe("spiritsteelbar", 1, SPIRIT_FORGE,
                Recipes.ingredientsFromScript("{{spectralore, 3}, {ectoplasm, 2}}")));
        Recipes.registerModRecipe(new Recipe("spiritsteelhelmet", 1, SPIRIT_FORGE,
                Recipes.ingredientsFromScript("{{spiritsteelbar, 8}, {soulthread, 4}}")));
        Recipes.registerModRecipe(new Recipe("spiritsteelchestplate", 1, SPIRIT_FORGE,
                Recipes.ingredientsFromScript("{{spiritsteelbar, 12}, {soulthread, 6}}")));
        Recipes.registerModRecipe(new Recipe("spiritsteelboots", 1, SPIRIT_FORGE,
                Recipes.ingredientsFromScript("{{spiritsteelbar, 6}, {soulthread, 3}}")));
    }

    public static void loadTextures() {
        GameTexture.fromFile("tiles/murkmoss_splat");
        GameTexture.fromFile("tiles/swampgrass_splat");
        GameTexture.fromFile("tiles/cryptash_splat");
        GameTexture.fromFile("tiles/stonebrickfloor_splat");
        GameTexture.fromFile("tiles/ravenfloor_splat");
        GameTexture.fromFile("tiles/swamprock_splat");
    }
}
