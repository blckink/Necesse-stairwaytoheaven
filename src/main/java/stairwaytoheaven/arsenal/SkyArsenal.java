package stairwaytoheaven.arsenal;

import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;

/**
 * The Sky Arsenal — the mod's craftable weapon tier and the enemies that feed
 * it.
 *
 * <p>Owned entirely by this package. {@code StairwayToHeavenMod} calls exactly
 * four methods and nothing else here reaches into {@code SkyRegistry},
 * {@code SkyItems} or {@code SkyMobs}:
 *
 * <pre>
 *   init():          SkyArsenal.register();  SkyArsenal.registerItems();
 *   initResources(): SkyArsenal.loadTextures();
 *   postInit():      SkyArsenal.registerRecipes();
 * </pre>
 *
 * <h2>Why the order inside {@code init()} matters</h2>
 * {@code register()} runs first because {@link SkywatchWhistleSummonToolItem}
 * names its summon by MobRegistry stringID and {@link StormdiscToolItem} names
 * its projectile by ProjectileRegistry stringID — both registries must already
 * hold those IDs when the items are constructed. Every registry in
 * {@code GlobalData.loadAll}'s list (ItemRegistry, MobRegistry,
 * ProjectileRegistry included) closes immediately after the mods' {@code init()}
 * loop, so all three have to happen there; recipes are the one thing that must
 * wait for {@code postInit()}.
 *
 * <h2>Tier and station</h2>
 * Every weapon here is the Skyreach's answer to a deep-cave (tungsten-era)
 * vanilla weapon of the same class — see the calibration note in each item
 * class. They are crafted at {@code TUNGSTEN_WORKSTATION}, the same station
 * the Tempest Edge, the Galehowl and the Skyward Stairway itself use: it is the
 * one tech a player who can reach the Skyreach at all is guaranteed to own,
 * because the stairway recipe requires it.
 */
public final class SkyArsenal {

    private SkyArsenal() {
    }

    // --- registered IDs -----------------------------------------------------
    public static int skyreaveID;
    public static int thunderheadID;
    public static int prismcallerID;
    public static int skywatchwhistleID;
    public static int stormdiscID;

    public static int prismboltProjectileID;
    public static int stormdiscProjectileID;

    /**
     * The enemies this stream adds, in the order the spawn probe should print
     * them. {@code SkyreachStatusCommand} appends this to its own probe list so
     * a new enemy cannot ship without its accepted-lit/accepted-dark counts
     * being measured. Kept here so the command never has to know the names.
     */
    public static final String[] PROBE_MOB_IDS = {
            "rimesentry", "auroraflake", "fenwraith", "cindercantor",
    };

    /**
     * Mobs and projectiles. Runs from {@code init()}; both registries close
     * right after the mod loop.
     *
     * <p>The four hostiles are subclasses of vanilla mobs and wear vanilla body
     * sheets: {@code MobRegistry.Textures} is a compiled vanilla class loaded
     * once by {@code GameResources.loadTextures()}, and an unoverridden
     * {@code addDrawables} reads those fields directly. That is why
     * {@link #loadTextures()} has nothing to load for them.
     */
    public static void register() {
        // Projectiles first: the boomerang resolves "stormdisc" by stringID.
        // Texture paths are relative to projectiles/ and are resolved by
        // ProjectileRegistry.Textures.load() on the client. The prism bolt
        // deliberately reuses vanilla's shared bolt_shadow, exactly as
        // vanilla's own quartzbolt registration does.
        prismboltProjectileID = ProjectileRegistry.registerProjectile(
                "prismbolt", PrismBoltProjectile.class, "prismbolt", "bolt_shadow");
        stormdiscProjectileID = ProjectileRegistry.registerProjectile(
                "stormdisc", StormdiscProjectile.class, "stormdisc", "stormdisc_shadow");

        // Skyreach hostiles
        MobRegistry.registerMob("rimesentry", RimeSentryMob.class, true);
        MobRegistry.registerMob("auroraflake", AuroraFlakeMob.class, true);
        // Veil hostiles
        MobRegistry.registerMob("fenwraith", FenWraithMob.class, true);
        MobRegistry.registerMob("cindercantor", CinderCantorMob.class, true);
        // The Skywatch Whistle's companion. countKillStat=false: a summon is
        // not something the player kills, and no spawn item either — it is
        // reached only through the weapon.
        MobRegistry.registerMob("watchmote", WatchMoteFollowingMob.class, false);
    }

    /**
     * The five weapons. Runs from {@code init()}, after {@link #register()}.
     *
     * <p>Broker values are hand-set rather than left to {@code -1.0F}: the
     * auto-computation walks the cheapest recipe, and these recipes are made of
     * mod materials whose own values are hand-set too. 240 puts them just above
     * the Tempest Edge and Galehowl at 220, which is what they cost to make.
     */
    public static void registerItems() {
        skyreaveID = ItemRegistry.registerItem("skyreave", new SkyreaveGlaiveToolItem(), 240.0F, true);
        thunderheadID = ItemRegistry.registerItem("thunderhead", new ThunderheadGreatbowToolItem(), 260.0F, true);
        prismcallerID = ItemRegistry.registerItem("prismcaller", new PrismcallerStaffToolItem(), 240.0F, true);
        skywatchwhistleID = ItemRegistry.registerItem("skywatchwhistle", new SkywatchWhistleSummonToolItem(), 240.0F, true);
        stormdiscID = ItemRegistry.registerItem("stormdisc", new StormdiscToolItem(), 90.0F, true);
    }

    /**
     * Client-only texture loading, from {@code initResources()}.
     *
     * <p>There is deliberately nothing to do here. Item icons and the
     * mid-attack sprites load themselves through {@code Item.loadTextures}'s
     * stringID convention ({@code items/<id>.png},
     * {@code player/weapons/<id>.png}); projectile sprites load through
     * {@code ProjectileRegistry.Textures.load()}; bestiary icons through
     * {@code MobRegistry.loadMobIcons()}; and the four enemies plus the Watch
     * Mote draw from {@code MobRegistry.Textures}, which vanilla fills in the
     * same boot step. The method exists so the hook in
     * {@code StairwayToHeavenMod.initResources} stays stable if a future
     * arsenal mob ever does need its own sheet.
     */
    public static void loadTextures() {
    }

    /**
     * Recipes. Runs from {@code postInit()} — the mod recipe registry stays
     * open until just after the mods' postInit loop, and postInit is the only
     * point where every referenced item is guaranteed to exist.
     */
    public static void registerRecipes() {
        // Melee: sky-metal crescents on a cloudwood pole.
        Recipes.registerModRecipe(new Recipe(
                "skyreave", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{aetheriumbar, 8}, {fulgurite, 6}, {cloudwood, 10}}")));

        // Ranged: a seraphwood stave, windsilk string, fulgurite banding.
        Recipes.registerModRecipe(new Recipe(
                "thunderhead", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{aetheriumbar, 10}, {windsilk, 8}, {fulgurite, 4}, {seraphwood, 12}}")));

        // Magic: prismwood shaft, a shoal prismshard for the head.
        Recipes.registerModRecipe(new Recipe(
                "prismcaller", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{aetheriumbar, 4}, {prismshard, 10}, {stormshard, 6}, {prismwood, 8}}")));

        // Summon: brass, storm shards and a windsilk lanyard.
        Recipes.registerModRecipe(new Recipe(
                "skywatchwhistle", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{aetheriumbar, 6}, {stormshard, 8}, {windsilk, 6}}")));

        // Throwable: a full set of three at once. BoomerangToolItem refuses to
        // enchant or upgrade a partial stack (isEnchantable checks
        // amount >= stackSize), so handing out one at a time would leave the
        // player unable to use the forge on it until they crafted three.
        Recipes.registerModRecipe(new Recipe(
                "stormdisc", 3, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{aetheriumbar, 6}, {stormshard, 6}, {cinderpearl, 3}, {veilessence, 2}}")));
    }
}
