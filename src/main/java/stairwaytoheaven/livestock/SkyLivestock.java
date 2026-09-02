package stairwaytoheaven.livestock;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.level.maps.levelData.settlementData.settler.Settler;

/**
 * The Skyreach's livestock layer: three farmable animals, three products and
 * the nine recipes that turn the products into something a player wants.
 *
 * <h2>Wiring</h2>
 * <pre>
 *   StairwayToHeavenMod.init()          -&gt; SkyLivestock.register();
 *   StairwayToHeavenMod.initResources() -&gt; SkyLivestock.loadTextures();
 *   StairwayToHeavenMod.postInit()      -&gt; SkyLivestock.registerItems();
 * </pre>
 *
 * <p>Why the split is where it is: {@code GlobalData.loadAll} closes
 * {@code ItemRegistry} and {@code MobRegistry} immediately after the
 * {@code init()} loop, so mobs and items have to be registered there;
 * {@code Recipes.closeModRecipeRegistry()} does not run until after every
 * mod's {@code postInit()}, and that is the first point at which every item a
 * recipe names is guaranteed to exist. A {@code new Ingredient(stringID, ...)}
 * whose ID is not yet a registered item does not fail — it silently becomes a
 * GLOBAL ingredient lookup instead — so registering these recipes any earlier
 * would turn a load-order accident into a recipe that is merely uncraftable
 * rather than into an error.
 *
 * <h2>The three animals</h2>
 * <table>
 *   <caption>archetype, home and product</caption>
 *   <tr><th>animal</th><th>vanilla base</th><th>biome</th><th>product</th><th>how it is taken</th></tr>
 *   <tr><td>Nimbus Yak</td><td>CowMob</td><td>Driftlands</td><td>Nimbus Milk</td>
 *       <td>bucket — {@code BucketItem} -&gt; {@code canMilk}/{@code onMilk}</td></tr>
 *   <tr><td>Thunderquill Fowl</td><td>ChickenMob</td><td>Stormveil</td><td>Storm Down</td>
 *       <td>shears — {@code ShearsItem} -&gt; {@code canShear}/{@code onShear}; also lays vanilla eggs</td></tr>
 *   <tr><td>Glimmergoat</td><td>SheepMob</td><td>Aurora Shoals</td><td>Aurora Fleece</td>
 *       <td>shears</td></tr>
 * </table>
 *
 * <p>All three are hand-fed and trough-fed by the rule every husbandry animal
 * follows — {@code HusbandryMob.canFeed} is
 * {@code item.item instanceof GrainItem} and
 * {@code FeedingTroughObjectEntity}'s inventory filter is the same test — so
 * vanilla wheat and the mod's cloudberry both work, unchanged.
 *
 * <p>The string IDs below are spelled as literals at every registration and
 * recipe call on purpose: {@code tools/locale_audit.py} finds registered IDs by
 * matching a quoted first argument to the registry and recipe calls, and an ID
 * hidden behind a constant would be an ID the audit cannot name-check — it
 * says so out loud rather than skipping it. The constants exist only for the
 * code that has to refer to these IDs from another class.
 */
public final class SkyLivestock {

    private SkyLivestock() {
    }

    // ===== IDs other code refers to =====

    public static final String NIMBUS_YAK = "nimbusyak";
    public static final String THUNDERQUILL = "thunderquill";
    public static final String GLIMMERGOAT = "glimmergoat";

    public static final String NIMBUS_MILK = "nimbusmilk";
    public static final String STORM_DOWN = "stormdown";
    public static final String AURORA_FLEECE = "aurorafleece";

    // ===== Registry IDs, for anything that needs to compare without a lookup =====

    public static int nimbusYakID;
    public static int thunderquillID;
    public static int glimmergoatID;

    public static int nimbusMilkID;
    public static int stormDownID;
    public static int auroraFleeceID;
    public static int skyCurdID;
    public static int cloudCustardID;
    public static int nimbusDraughtID;
    public static int thunderplumeID;
    public static int glimmerstridesID;

    /**
     * Mobs and items. Called from {@code init()}, while the registries are open.
     *
     * <p>The mobs are registered with the kill-statistic flag off, the way
     * vanilla registers its cow, sheep and chicken: nobody wants a scoreboard
     * for slaughtering their own herd.
     */
    public static void register() {
        nimbusYakID = MobRegistry.registerMob("nimbusyak", NimbusYakMob.class, false);
        thunderquillID = MobRegistry.registerMob("thunderquill", ThunderquillMob.class, false);
        glimmergoatID = MobRegistry.registerMob("glimmergoat", GlimmergoatMob.class, false);

        // --- the three products ---
        // Vanilla milk is a FoodConsumableItem you can drink, and so is this;
        // the difference is what it presses into.
        nimbusMilkID = ItemRegistry.registerItem("nimbusmilk",
                new SkyLivestockItems.LivestockFood("milk", 0.535F, 0.20F, 250, Item.Rarity.COMMON,
                        Settler.FOOD_SIMPLE, 20, 480, true,
                        new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 15))
                        .spoilDuration(240)
                        .setItemCategory("consumable", "rawfood"),
                4.0F, true);
        stormDownID = ItemRegistry.registerItem("stormdown",
                new SkyLivestockItems.LivestockProduce("inefficientfeather", 0.745F, 0.32F, 500,
                        Item.Rarity.UNCOMMON),
                14.0F, true);
        auroraFleeceID = ItemRegistry.registerItem("aurorafleece",
                new SkyLivestockItems.LivestockProduce("wool", 0.425F, 0.26F, 500,
                        Item.Rarity.UNCOMMON),
                16.0F, true);

        // --- what the products become ---
        skyCurdID = ItemRegistry.registerItem("skycurd",
                new SkyLivestockItems.LivestockFood("cheese", 0.525F, 0.18F, 250, Item.Rarity.NORMAL,
                        Settler.FOOD_FINE, 25, 480, false,
                        new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 25))
                        .spoilDuration(480)
                        .addGlobalIngredient("anycookedfood"),
                9.0F, true);
        cloudCustardID = ItemRegistry.registerItem("cloudcustard",
                new SkyLivestockItems.LivestockFood("cheesybeetbowl", 0.075F, 0.34F, 250,
                        Item.Rarity.NORMAL, Settler.FOOD_FINE, 30, 480, false,
                        new ModifierValue<>(BuffModifiers.SPEED, 0.06F),
                        new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.04F))
                        .spoilDuration(240)
                        .addGlobalIngredient("anycookedfood"),
                16.0F, true);
        nimbusDraughtID = ItemRegistry.registerItem("nimbusdraught",
                new SkyLivestockItems.LivestockFood("resistancepotion", 0.525F, 0.30F, 250,
                        Item.Rarity.UNCOMMON, Settler.FOOD_FINE, 15, 900, true,
                        new ModifierValue<>(BuffModifiers.ARMOR_FLAT, 6),
                        new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 20))
                        .spoilDuration(720),
                24.0F, true);
        thunderplumeID = ItemRegistry.registerItem("thunderplume",
                new SkyLivestockItems.ThunderplumeCowl(), 90.0F, true);
        glimmerstridesID = ItemRegistry.registerItem("glimmerstrides",
                new SkyLivestockItems.GlimmerstrideBoots(), 60.0F, true);
    }

    /**
     * The recipes for the items {@link #register()} created. Called from
     * {@code postInit()} — see the class comment for why that phase and not
     * {@code init()}.
     *
     * <p>Every tech used here is one the player already owns by the time the
     * Skyreach exists at all: the Stairway itself costs 8 tungsten bars at a
     * Tungsten Workstation, which is past the demonic tier the Cheese Press is
     * built at ({@code cheesepress} = 5 iron bars + 5 demonic bars at a Demonic
     * Workstation, jar Recipes.java:1147) and far past the cooking pot, the
     * alchemy table, the carpenter's bench and the tungsten anvil.
     */
    public static void registerItems() {
        // --- Nimbus Milk: the sustain line ---
        // Vanilla presses milk into cheese at the same station at the same
        // one-to-one ratio (jar Recipes.java:2759); sky milk presses into curd.
        Recipes.registerModRecipe(new Recipe(
                "skycurd", 1, RecipeTechRegistry.CHEESE_PRESS,
                Recipes.ingredientsFromScript("{{nimbusmilk, 1}}")));
        Recipes.registerModRecipe(new Recipe(
                "cloudcustard", 1, RecipeTechRegistry.COOKING_POT,
                Recipes.ingredientsFromScript("{{nimbusmilk, 2}, {cloudberry, 3}}")));
        Recipes.registerModRecipe(new Recipe(
                "nimbusdraught", 1, RecipeTechRegistry.ALCHEMY,
                Recipes.ingredientsFromScript("{{nimbusmilk, 2}, {aurorapetal, 1}}")));

        // --- Storm Down: the gear line ---
        Recipes.registerModRecipe(new Recipe(
                "thunderplume", 1, RecipeTechRegistry.TUNGSTEN_ANVIL,
                Recipes.ingredientsFromScript("{{stormdown, 12}, {aetheriumbar, 4}, {stormshard, 3}}")));
        // Windsilk had exactly two renewable sources: harvested windwheat and a
        // sheared Cloud Lamb. Down spins into it as well, which is what makes a
        // Stormveil coop worth keeping.
        Recipes.registerModRecipe(new Recipe(
                "windsilk", 2, RecipeTechRegistry.NONE,
                Recipes.ingredientsFromScript("{{stormdown, 3}}")));
        // Vanilla's net is a Demonic Workstation recipe of 10 logs and 5 wool
        // (jar Recipes.java:1202). The sky has no sheep, and it does have
        // something worth catching with a net — the Dew Snail is a NetableMob.
        // Same shape, sky materials. The output is a VANILLA item: the game
        // names and draws it, which is why tools/locale_audit.py carries it in
        // VANILLA_RECIPE_OUTPUTS rather than looking for a mod icon.
        Recipes.registerModRecipe(new Recipe(
                "net", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{stormdown, 5}, {anylog, 10}}")));

        // --- Aurora Fleece: the comfort line ---
        Recipes.registerModRecipe(new Recipe(
                "glimmerstrides", 1, RecipeTechRegistry.TUNGSTEN_ANVIL,
                Recipes.ingredientsFromScript("{{aurorafleece, 8}, {aetheriumbar, 2}}")));
        // Vanilla felts one wool into one carpet at a carpenter's bench (jar
        // Recipes.java:1812). A fleece is worth two of the Skywatch's own.
        Recipes.registerModRecipe(new Recipe(
                "skywatchcarpet", 2, RecipeTechRegistry.CARPENTER,
                Recipes.ingredientsFromScript("{{aurorafleece, 1}}")));
        // The spire cats want cloud puffs, and this is the fluffiest thing in
        // the sky. An alternative recipe, not a replacement for the windsilk one.
        Recipes.registerModRecipe(new Recipe(
                "cloudpufftreat", 4, RecipeTechRegistry.NONE,
                Recipes.ingredientsFromScript("{{aurorafleece, 1}}")));
    }

    /**
     * Client-side texture loading, called from {@code initResources()} — a
     * dedicated server never calls that method, and this is the only place in
     * the package a {@code GameTexture} is touched.
     *
     * <p>The yak wears its own three sheets; the fowl and the goat are still
     * recoloured vanilla farm-animal sheets (see {@link SkyPelt}). Either way
     * the frame grid is vanilla's — 6 columns x 4 direction rows of 64 plus the
     * 32px flesh-particle cells at y256 — and the shadow sheets stay vanilla's
     * for all three, because a shadow is a black blob and recolouring one would
     * do nothing.
     */
    public static void loadTextures() {
        // Nimbus Yak: the mod's own three sheets, drawn on vanilla's grid —
        // 6 columns x 4 direction rows of 64, then the five 32px gib cells at
        // y256 that CowMob's FleshParticle reads (spriteX 0..4, spriteY 8,
        // size 32; jar CowMob.java:96). No recolour: these ARE the yak.
        NimbusYakMob.cowTexture = GameTexture.fromFile("mobs/nimbusyak").makeFinal();
        NimbusYakMob.bullTexture = GameTexture.fromFile("mobs/nimbusyak_bull").makeFinal();
        NimbusYakMob.calfTexture = GameTexture.fromFile("mobs/nimbusyak_calf").makeFinal();

        // Thunderquill Fowl: storm violet, plus a washed-out copy for a bird
        // that has just been plucked (vanilla ships no such frame).
        ThunderquillMob.henTexture = SkyPelt.tint("mobs/chicken", "mobs/thunderquill",
                0.745F, 0.28F, 0.40F, 0.94F, 0.02F);
        ThunderquillMob.henPluckedTexture = SkyPelt.bleach(ThunderquillMob.henTexture,
                "mobs/thunderquill_plucked", 0.30F, 0.06F);
        ThunderquillMob.cockTexture = SkyPelt.tint("mobs/rooster", "mobs/thunderquill_cock",
                0.745F, 0.34F, 0.40F, 0.92F, 0.02F);
        ThunderquillMob.cockPluckedTexture = SkyPelt.bleach(ThunderquillMob.cockTexture,
                "mobs/thunderquill_cock_plucked", 0.30F, 0.06F);
        ThunderquillMob.chickTexture = SkyPelt.tintFinal("mobs/chick", "mobs/thunderquill_chick",
                0.745F, 0.22F, 0.40F, 1.0F, 0.05F);

        // Glimmergoat. Vanilla already has the sheared frames, for both sexes,
        // which is most of why the goat is a SheepMob.
        //
        // The DOE is ours now, drawn and supplied on vanilla's exact grid (four
        // direction rows of 64 plus the four gib cells at y256 that SheepMob
        // reads) -- both the woolly and the shorn state. The BUCK and the KID
        // are still SkyPelt recolours of vanilla's ram and lamb until their
        // sheets arrive, so the herd is two palettes for the moment: pink does,
        // teal buck and kid. That is temporary and visible in game.
        GlimmergoatMob.doeTexture = GameTexture.fromFile("mobs/glimmergoat").makeFinal();
        GlimmergoatMob.doeShornTexture = GameTexture.fromFile("mobs/glimmergoat_shorn").makeFinal();
        GlimmergoatMob.buckTexture = SkyPelt.tintFinal("mobs/ram", "mobs/glimmergoat_buck",
                0.425F, 0.26F, 0.40F, 0.97F, 0.03F);
        GlimmergoatMob.buckShornTexture = SkyPelt.tintFinal("mobs/ram_sheared",
                "mobs/glimmergoat_buck_shorn", 0.425F, 0.18F, 0.40F, 0.97F, 0.03F);
        GlimmergoatMob.kidTexture = SkyPelt.tintFinal("mobs/lamb", "mobs/glimmergoat_kid",
                0.425F, 0.18F, 0.40F, 1.0F, 0.06F);
    }
}
