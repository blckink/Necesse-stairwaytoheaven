package stairwaytoheaven.settlement;

import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.inventory.recipe.Tech;
import stairwaytoheaven.items.SkyMatItem;

/**
 * Skywatch professions: the three settlement workstations a settler runs on
 * the player's behalf, and the three materials they make.
 *
 * <h2>What a "profession" is in Necesse</h2>
 *
 * It is not a class or a job title. Read against the 1.3.2 sources, a settler
 * doing work is two separate mechanisms:
 *
 * <ul>
 * <li><b>Work zones</b> ({@code SettlementWorkZoneRegistry}, which ships
 *     exactly three: {@code forestry}, {@code husbandry}, {@code fertilize})
 *     are painted AREAS. A zone exists so a settler knows <em>where</em> to
 *     chop, shear or fertilize.</li>
 * <li><b>Workstations</b> are single objects implementing
 *     {@link necesse.level.maps.levelData.settlementData.SettlementWorkstationObject}.
 *     {@code SettlementStorageManager.assignWorkstation} accepts any object
 *     that passes {@code instanceof SettlementWorkstationObject} — that check
 *     is the whole gate — and {@code ServerSettlementData.tickJobs} then
 *     publishes a {@code UseWorkstationLevelJob} for each assigned station
 *     every tick. {@code LevelJobRegistry} files that job under the vanilla
 *     job type <b>"crafting"</b>.</li>
 * </ul>
 *
 * All three stations here are workstations, so <b>no new work zone is
 * registered and none is needed</b>: a zone would answer "where do I go
 * looking", and a station already answers it by standing on one tile. The
 * settler-facing work priority they land under is vanilla's existing
 * <b>crafting</b> priority — reusing it rather than inventing one is the same
 * choice vanilla makes for its own forge, cheese press, grain mill and
 * compost bin.
 *
 * <h2>How the player puts a settler on one</h2>
 *
 * Build the station inside the settlement, open the settlement screen's work
 * tab, click the station to make it a workstation, add the recipes it should
 * run and set each to Do Count / Do Until / Forever. Any settler whose
 * <b>crafting</b> priority is enabled will then fetch the ingredients from
 * settlement storage, walk over, work, and carry the result back — the loom
 * crafts in place, the forge and the kiln are processing stations, so the
 * settler loads them and collects the output later.
 */
public final class SkyProfessions {

    // --- Recipe techs ---------------------------------------------------
    // A Tech is what ties a recipe to the station that can run it. Vanilla's
    // own late additions (COMPOST_BIN, GRAIN_MILL, CHEESE_PRESS) are
    // registered exactly this way. The second argument is the item stringID
    // the crafting UI shows for "Made in: <tech>", i.e. the station's own
    // object item; the display name resolves through LocalMessage("tech",
    // stringID), which is why each needs a [tech] key in both locales.
    public static Tech LOOM;
    public static Tech AETHER_FORGE;
    public static Tech STORMGLASS_KILN;

    // --- Object IDs -----------------------------------------------------
    public static int windsilkLoomID;
    public static int aetherForgeID;
    public static int stormglassKilnID;

    private SkyProfessions() {
    }

    /**
     * Techs and station objects. Runs in {@code init()} — both registries
     * close immediately afterwards.
     */
    public static void register() {
        LOOM = RecipeTechRegistry.registerTech("windsilkloom", "windsilkloom");
        AETHER_FORGE = RecipeTechRegistry.registerTech("aetherforge", "aetherforge");
        STORMGLASS_KILN = RecipeTechRegistry.registerTech("stormglasskiln", "stormglasskiln");

        // Broker values match vanilla's stations: forge 20, cheesepress 20,
        // alchemytable 20. itemObtainable is true so the object can be picked
        // back up — GameObject.getLootTable only hands the object's own item
        // over when that item is obtainable.
        windsilkLoomID = ObjectRegistry.registerObject("windsilkloom",
                new WindsilkLoomObject(), 20.0F, true);
        aetherForgeID = ObjectRegistry.registerObject("aetherforge",
                new AetherForgeObject(), 20.0F, true);
        stormglassKilnID = ObjectRegistry.registerObject("stormglasskiln",
                new StormglassKilnObject(), 20.0F, true);
    }

    /**
     * The three materials the stations produce. Runs in {@code init()},
     * before {@link #registerRecipes()} can name them.
     */
    public static void registerItems() {
        // Woven on the Windsilk Loom. A bolt of cloth, so it stacks like one.
        //
        // It is NOT a mob drop. Vanilla's three cloths — `wool`, `silk`,
        // `clothscraps` (ItemRegistry.java:868, :924, :871) — sit in
        // materials/mobdrops because a sheep, a spider and a zombie drop them;
        // Skyweave comes off a loom and nothing in the game drops it. Vanilla's
        // own crafted intermediates that belong to no material family (`glass`,
        // `glassbottle`, :927-928) sit in bare `materials`, which is where this
        // belongs too.
        ItemRegistry.registerItem("skyweave",
                new SkyMatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials"),
                20.0F, true);
        // The Aether Forge's own tier: aetherium quenched in storm shard.
        //
        // Worth (incursion rebalance): a bar is 4 Aetherium Ore + 1 Storm
        // Shard (see registerRecipes below), and the Storm Shard moved to the
        // tier-1 incursion floor of 25.0F in SkyItems, so its ingredients now
        // cost 4 x 8.0F + 25.0F = 57.0F. 58.0F keeps the same razor-thin forge
        // margin the bar had before (44.0F of ore and shard into 45.0F).
        // VERIFIED [jar]: that puts the mod's endgame bar above vanilla's
        // dearest materials/bars entry, `fuelskull` at brokerValue 42.0F
        // (ItemRegistry.java:921) — deliberately, because it is bought with an
        // incursion-tier crystal that vanilla's bars never cost.
        ItemRegistry.registerItem("stormsteelbar",
                new SkyMatItem(250, Item.Rarity.RARE).setItemCategory("materials", "bars"),
                58.0F, true);
        // Fired in the Stormglass Kiln out of lightning-fused sand.
        //
        // Worth (incursion rebalance): the kiln turns 2 Fulgurite + 1 Skystone
        // into 2 panes, so one pane is 1 Fulgurite + half a Skystone — 26.0F
        // now that Fulgurite carries the tier-1 incursion floor. 32.0F
        // preserves the kiln's old +23% margin (13.0F of stock into a 16.0F
        // pane).
        // Anchor: `shadowessence` 25.0F (ItemRegistry.java:977) through the
        // Fulgurite it is fired from; for scale, vanilla's dearest
        // materials/minerals entries are the gems, `amethyst` and its four
        // siblings, at 25.0F (ItemRegistry.java:901).
        ItemRegistry.registerItem("stormglass",
                new SkyMatItem(500, Item.Rarity.UNCOMMON).setItemCategory("materials", "minerals"),
                32.0F, true);
    }

    /**
     * Recipes. Runs in {@code postInit()} — the mod recipe registry closes
     * after every mod's postInit, and it is the first point at which every
     * item and tech in the game is guaranteed to exist.
     */
    public static void registerRecipes() {
        // --- The stations themselves ------------------------------------
        // All three sit on WORKSTATION, the tech the player already has when
        // he can reach the Skyreach at all, so a station is never gated
        // behind another station.
        Recipes.registerModRecipe(new Recipe("windsilkloom", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{cloudwood, 12}, {ironbar, 4}, {windsilk, 4}}")));
        Recipes.registerModRecipe(new Recipe("aetherforge", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 20}, {ironbar, 8}, {stormshard, 4}}")));
        Recipes.registerModRecipe(new Recipe("stormglasskiln", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 16}, {ironbar, 4}, {fulgurite, 4}}")));

        // --- What each station makes ------------------------------------
        // Windsilk Loom: raw windsilk into cloth.
        Recipes.registerModRecipe(new Recipe("skyweave", 1, LOOM,
                Recipes.ingredientsFromScript("{{windsilk, 3}}")));
        // ...and the spinning half of the same machine. Windwheat already
        // twists into windsilk by hand at 3:1 (SkyItems, tech NONE); a real
        // loom does it at 2:1, which is what makes the settlement's windwheat
        // harvest worth hauling home.
        Recipes.registerModRecipe(new Recipe("windsilk", 1, LOOM,
                Recipes.ingredientsFromScript("{{windwheat, 2}}")));

        // Aether Forge: the reason to build one is the yield. The vanilla
        // forge already smelts 3 ore into 1 bar (SkyItems); this does it in
        // 2, and it is the only place stormsteel exists.
        Recipes.registerModRecipe(new Recipe("aetheriumbar", 1, AETHER_FORGE,
                Recipes.ingredientsFromScript("{{aetheriumore, 2}}")));
        Recipes.registerModRecipe(new Recipe("stormsteelbar", 1, AETHER_FORGE,
                Recipes.ingredientsFromScript("{{aetheriumore, 4}, {stormshard, 1}}")));

        // Stormglass Kiln: fulgurite is lightning-fused sand, so remelting it
        // over skystone flux is what turns it back into a usable pane.
        Recipes.registerModRecipe(new Recipe("stormglass", 2, STORMGLASS_KILN,
                Recipes.ingredientsFromScript("{{fulgurite, 2}, {skystone, 1}}")));
    }
}
