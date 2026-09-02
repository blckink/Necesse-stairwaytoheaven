package stairwaytoheaven.realms.eden;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.realms.ghost.GhostMatItem;

/** Registry and resource boundary for the playable Garden of Eden core. */
public final class EdenRealm {
    private EdenRealm() {
    }

    public static EdenGardenBiome garden;
    public static EdenCanopyBiome canopy;
    public static EdenShallowsBiome shallows;

    public static ParadiseSandTile paradiseSandTile;
    public static int paradiseSandID;
    public static int shallowsTileID;
    public static int edenMossID;
    public static int edenSoilID;
    public static int edenRootFloorID;

    // Eden currently borrows complete vanilla object archetypes. These are
    // aliases, not duplicate registrations, so their sprites, drops and tool
    // behaviour stay native until the dedicated Eden art/content pass lands.
    public static int serpentGrassID;
    public static int paradiseFernID;
    public static int floweringVineID;
    public static int redParadiseFlowerID;
    public static int blueParadiseFlowerID;
    public static int goldenOrchidID;
    public static int giantMonsteraID;
    public static int giantFigTreeID;
    public static int paradisePalmID;
    public static int treeOfPlentyID;
    public static int edenBerryBushID;
    public static int sunGrapeBushID;
    public static int moonMelonBushID;
    public static int edenRockID;
    public static int edenCopperRockID;
    public static int edenCacheID;
    public static int adamsVineID;
    public static int giantLotusID;
    public static int paradiseReedsID;
    public static int edenShellsID;
    public static int knowledgeTreeID;

    // The Eden Gate — see EdenSeedBasinObject for why it is three objects.
    public static int edenSeedBasinID;
    public static int edenGateDownID;
    public static int edenGateUpID;

    public static void register() {
        registerBiomes();
        registerTiles();
        registerItems();
        registerObjects();
        registerMobs();
        registerGate();
    }

    private static void registerBiomes() {
        garden = BiomeRegistry.registerBiome("edengarden", new EdenGardenBiome(), false);
        canopy = BiomeRegistry.registerBiome("edencanopy", new EdenCanopyBiome(), false);
        shallows = BiomeRegistry.registerBiome("edenshallows", new EdenShallowsBiome(), false);
    }

    private static void registerTiles() {
        paradiseSandTile = new ParadiseSandTile();
        paradiseSandID = TileRegistry.registerTile("paradisesandtile", paradiseSandTile,
                0.0F, false, false, true);
        shallowsTileID = TileRegistry.registerTile("edenshallowstile", new EdenShallowsTile(),
                0.0F, false);
        edenMossID = TileRegistry.registerTile("edenmosstile", new EdenMossTile(),
                0.0F, false, false, true);
        edenSoilID = TileRegistry.registerTile("edensoiltile", new EdenSoilTile(),
                0.0F, false, false, true);
        edenRootFloorID = TileRegistry.registerTile("edenrootfloortile", new EdenRootFloorTile(),
                0.0F, false, false, true);
    }

    private static void registerItems() {
        ItemRegistry.registerItem("edenwood", new GhostMatItem("palmlog", 500, Item.Rarity.UNCOMMON)
                .setItemCategory("materials", "logs"), 10.0F, true);
        ItemRegistry.registerItem("edensap", new GhostMatItem("dryadbranch", 500, Item.Rarity.UNCOMMON)
                .setItemCategory("materials"), 10.0F, true);
        ItemRegistry.registerItem("paradiseapple", new GhostMatItem("apple", 500, Item.Rarity.UNCOMMON)
                .setItemCategory("materials"), 10.0F, true);
        ItemRegistry.registerItem("serpentscale", new GhostMatItem("sharkscales", 500, Item.Rarity.RARE)
                .setItemCategory("materials", "mobdrops"), 10.0F, true);
        ItemRegistry.registerItem("venomfang", new GhostMatItem("fangoftheprotector", 500, Item.Rarity.RARE)
                .setItemCategory("materials", "mobdrops"), 10.0F, true);
        ItemRegistry.registerItem("goldenpollen", new GhostMatItem("honey", 500, Item.Rarity.RARE)
                .setItemCategory("materials", "flowers"), 10.0F, true);
        ItemRegistry.registerItem("knowledgecutting", new GhostMatItem("dryadsapling", 500, Item.Rarity.EPIC)
                .setItemCategory("materials"), 10.0F, true);
        ItemRegistry.registerItem("paradisecoconut", new GhostMatItem("coconut", 500, Item.Rarity.UNCOMMON)
                .setItemCategory("materials"), 10.0F, true);
        ItemRegistry.registerItem("edenberry", new GhostMatItem("blueberry", 500, Item.Rarity.UNCOMMON)
                .setItemCategory("materials"), 10.0F, true);
        ItemRegistry.registerItem("moonmelon", new GhostMatItem("frozenberry", 500, Item.Rarity.UNCOMMON)
                .setItemCategory("materials"), 10.0F, true);
        ItemRegistry.registerItem("sungrape", new GhostMatItem("raspberry", 500, Item.Rarity.UNCOMMON)
                .setItemCategory("materials"), 10.0F, true);
        ItemRegistry.registerItem("edencopperore", new GhostMatItem("ivyore", 500, Item.Rarity.RARE)
                .setItemCategory("materials", "ore"), 10.0F, true);
        ItemRegistry.registerItem("edenbronzebar", new GhostMatItem("ivybar", 500, Item.Rarity.EPIC)
                .setItemCategory("materials", "bars"), 10.0F, true);
    }

    private static void registerObjects() {
        serpentGrassID = object("grass");
        paradiseFernID = object("swampgrass");
        floweringVineID = object("grass");
        redParadiseFlowerID = object("redflowerpatch");
        blueParadiseFlowerID = object("blueflowerpatch");
        goldenOrchidID = object("yellowflowerpatch");
        giantMonsteraID = object("swampgrass");
        giantFigTreeID = object("bananatree");
        paradisePalmID = object("palmtree");
        treeOfPlentyID = object("appletree");
        edenBerryBushID = object("blackberrybush");
        sunGrapeBushID = object("blueberrybush");
        moonMelonBushID = object("blueberrybush");
        edenRockID = object("rock");
        // The registry ID is ivyoreswamp; its world texture is objects/ivyore.
        edenCopperRockID = object("ivyoreswamp");
        edenCacheID = SkyRegistry.skyCacheID;
        adamsVineID = object("swampgrass");
        giantLotusID = object("blueflowerpatch");
        paradiseReedsID = object("reeds");
        edenShellsID = object("seashell");
        knowledgeTreeID = object("dryadtree");
    }

    private static int object(String id) {
        int objectID = ObjectRegistry.getObjectID(id);
        if (objectID < 0) {
            throw new IllegalStateException("Missing vanilla Eden stand-in object: " + id);
        }
        return objectID;
    }

    private static void registerMobs() {
        MobRegistry.registerMob("edenserpent", EdenSerpentMob.class, false);
        MobRegistry.registerMob("bloommaw", BloomMawMob.class, false);
        MobRegistry.registerMob("jealousvine", JealousVineMob.class, false);
        MobRegistry.registerMob("goldenhornet", GoldenHornetMob.class, false);
        MobRegistry.registerMob("forbiddenserpent", ForbiddenSerpentMob.class, false);
    }

    /**
     * The Eden Gate — the only way into this realm today. See
     * {@link EdenSeedBasinObject} for the full reasoning; in short: the down
     * and up gate objects are {@code LadderDownObject}/its return half and are
     * registered unobtainable, exactly like the Ghost Gate and the Veil rift,
     * because a door into another dimension is opened at a place rather than
     * carried in a backpack. The basin that grows into the down gate IS
     * obtainable — it is the thing a player actually crafts.
     */
    private static void registerGate() {
        edenSeedBasinID = ObjectRegistry.registerObject("edenseedbasin", new EdenSeedBasinObject(), 20.0F, true);
        edenGateDownID = ObjectRegistry.registerObject("edengatedown", new EdenGateObject(), 0.0F, false);
        edenGateUpID = ObjectRegistry.registerObject("edengateup", new EdenSideGateObject(), 0.0F, false);
        ((EdenGateObject) ObjectRegistry.getObject(edenGateDownID)).ladderUpObjectID = edenGateUpID;
    }

    public static void registerRecipes() {
        Recipes.registerModRecipe(new Recipe("edenbronzebar", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{edencopperore, 3}, {edensap, 1}}")));
        // The Eden Threshold: aetherium proves the player has worked the
        // Aether Forge, skystone and windwheat are the sky's own common stone
        // and grain -- the recipe is Skyreach materials, matching how the
        // FIRST stairway (tungstenbar + quartz, deep-cave materials) is built
        // on the Surface out of the tier below it rather than the tier ahead.
        Recipes.registerModRecipe(new Recipe("edenseedbasin", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{aetheriumbar, 4}, {skystone, 10}, {windwheat, 8}}")));
    }

    /** Client-only vanilla stand-ins; the dedicated server never calls this. */
    public static void loadTextures() {
        EdenSerpentMob.texture = GameTexture.fromFile("mobs/crocodile");
        BloomMawMob.texture = GameTexture.fromFile("mobs/stabbybush");
        JealousVineMob.texture = GameTexture.fromFile("mobs/dryadsentinel");
        GoldenHornetMob.texture = GameTexture.fromFile("mobs/bee");
        ForbiddenSerpentMob.texture = GameTexture.fromFile("mobs/dragonwhelp");
    }
}
