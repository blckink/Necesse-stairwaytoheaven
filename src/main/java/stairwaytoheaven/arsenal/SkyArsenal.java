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
 * Every weapon here is the Skyreach's answer to the INCURSION-tier vanilla
 * weapon of the same class — the members of {@code IncursionGlaiveWeaponsLootTable},
 * {@code IncursionGreatbowWeaponsLootTable}, {@code IncursionMagicWeaponsLootTable},
 * {@code IncursionSummonWeaponsLootTable} and {@code IncursionThrowWeaponsLootTable},
 * which sit at enchant cost 1900 alongside {@code ArcanicChestplateArmorItem}
 * (29 armour / enchant 1900 / EPIC), vanilla's incursion-tier armour. See the
 * calibration note in each item class for the weapon it is measured against and
 * the measured numbers. VERIFIED [jar].
 *
 * <p>Each weapon is also registered INTO its incursion loot table rather than
 * the general one for its class: {@code ToolItem}'s constructor calls
 * {@code addToLootTable} on whatever pool it is handed, so that argument is
 * what decides where in the game the weapon can be found, and none of these
 * should turn up in the weapon chests of a fresh character any more.
 *
 * <p>They are crafted at {@code TUNGSTEN_WORKSTATION}, the same station the
 * Skyward Stairway itself uses: it is the one tech a player who can reach the
 * Skyreach at all is guaranteed to own, because the stairway recipe requires
 * it. The station is a gate on getting to the Skyreach, not on the tier of what
 * is made there — the ingredients are all Skyreach and Veil materials.
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
     * <p>The four hostiles are subclasses of vanilla mobs for BEHAVIOUR only;
     * all four now draw their own sheets. Vanilla resolves a mob's art from a
     * static field on {@code MobRegistry.Textures} read inline inside
     * {@code addDrawables}, and {@code Mob} exposes no per-instance texture
     * hook, so each of the four overrides {@code addDrawables} (and, where the
     * gibs are cut out of the same sheet, {@code spawnDeathParticles}) and
     * samples a {@code public static} texture of its own. Those fields are
     * filled by {@code SkyMobs.loadTextures}, alongside every other mob sheet
     * the mod ships — see {@link #loadTextures()} for why they are not filled
     * here.
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
     * mod materials whose own values are hand-set too.
     *
     * <p>The tier they sit in is vanilla's: the incursion weapons these five
     * are calibrated against register at 400.0F (slimeglaive, nightpiercer,
     * slimestaff, phantompopper), 350.0F (orbofslimes) and 150.0F
     * (nightrazorboomerang), against 200.0F-250.0F for the deep-cave weapons
     * they replace (quartzglaive, quartzstaff, tungstengreatbow). VERIFIED
     * [jar].
     *
     * <p>The exact number is then pinned just BELOW what the recipe costs in
     * materials, because {@code registerItem}'s float IS the broker value and
     * shipping reads it straight off {@code InventoryItem.getBrokerValue}. If a
     * weapon were worth more than its ingredients the recipe would be a coin
     * press: 8 aetheriumbar at 25 + 8 stormshard at 12 + 4 cinderpearl at 14 +
     * 3 veilessence at 18 is 406 for four Stormdiscs, so a disc is worth 100
     * and the set is worth 400. Every recipe here loses 6-18 coins the same
     * way, which is the relationship they had before the re-tier and the one
     * vanilla crafting has.
     */
    public static void registerItems() {
        // 292 in materials (8x25 aetheriumbar + 6x12 fulgurite + 10x2 cloudwood)
        skyreaveID = ItemRegistry.registerItem("skyreave", new SkyreaveGlaiveToolItem(), 280.0F, true);
        // 418 in materials (10x25 + 8x12 windsilk + 4x12 fulgurite + 12x2 seraphwood)
        thunderheadID = ItemRegistry.registerItem("thunderhead", new ThunderheadGreatbowToolItem(), 400.0F, true);
        // 308 in materials (4x25 + 10x12 prismshard + 6x12 stormshard + 8x2 prismwood)
        prismcallerID = ItemRegistry.registerItem("prismcaller", new PrismcallerStaffToolItem(), 300.0F, true);
        // 318 in materials (6x25 + 8x12 stormshard + 6x12 windsilk)
        skywatchwhistleID = ItemRegistry.registerItem("skywatchwhistle", new SkywatchWhistleSummonToolItem(), 310.0F, true);
        // 406 in materials for a set of four, so 100 a disc
        stormdiscID = ItemRegistry.registerItem("stormdisc", new StormdiscToolItem(), 100.0F, true);
    }

    /**
     * Client-only texture loading, from {@code initResources()}.
     *
     * <p>There is deliberately nothing to do here. Item icons and the
     * mid-attack sprites load themselves through {@code Item.loadTextures}'s
     * stringID convention ({@code items/<id>.png},
     * {@code player/weapons/<id>.png}); projectile sprites load through
     * {@code ProjectileRegistry.Textures.load()}; bestiary icons through
     * {@code MobRegistry.loadMobIcons()}; and the Watch Mote draws from
     * {@code MobRegistry.Textures}, which vanilla fills in the same boot step.
     *
     * <p>The four hostiles DO each own a sheet, but those are loaded in
     * {@code SkyMobs.loadTextures} with every other mob sheet in the mod rather
     * than split across two lists — one place to look for "which mob draws
     * what". This method exists so the hook in
     * {@code StairwayToHeavenMod.initResources} stays stable if that ever
     * changes.
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

        // Throwable: a full set of four at once, matching StormdiscToolItem's
        // stackSize (NightRazorBoomerangToolItem's 4, VERIFIED [jar]).
        // BoomerangToolItem refuses to enchant or upgrade a partial stack
        // (isEnchantable checks amount >= stackSize), so the recipe has to hand
        // over a whole set or the player cannot use the forge on it at all.
        Recipes.registerModRecipe(new Recipe(
                "stormdisc", 4, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                Recipes.ingredientsFromScript("{{aetheriumbar, 8}, {stormshard, 8}, {cinderpearl, 4}, {veilessence, 3}}")));
    }
}
