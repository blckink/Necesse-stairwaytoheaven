package stairwaytoheaven;

import java.awt.Color;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.level.gameObject.FenceGateObject;
import necesse.level.gameObject.FenceObject;
import necesse.level.gameObject.WallObject;
import stairwaytoheaven.tiles.SkywayTile;

/**
 * Cloudmarble: the white-and-gold cloud masonry of the Skyway Passages.
 *
 * <p>One wall sheet feeds three readers — the wall body, the window insert and
 * the eight door cells — so {@code registerWallObjects} produces the wall, the
 * door pair and the window from a single call and a single texture. The fence
 * and its gate follow vanilla's ironfence pattern, and the passages' ground is
 * a terrain tile rather than a buildable floor.
 */
public final class SkyCloudmarbleSet {

    /**
     * Map colour for the whole set: the cloudstone body's light step, sampled
     * off the reference art. Vanilla's convention is the sprite's light tone,
     * not its shadow — a shadow tone makes a wall run vanish on the world map.
     */
    private static final Color MAP_CLOUDMARBLE = new Color(214, 228, 236);
    /** The gold trim, used where the set should read warm on the map. */
    private static final Color MAP_SKYGOLD = new Color(200, 176, 128);

    public static int cloudmarbleWallID;
    public static int cloudmarbleDoorID;
    public static int cloudmarbleWindowID;
    public static int cloudmarbleFenceID;
    public static int cloudmarbleFenceGateID;
    public static int skywayTileID;

    private SkyCloudmarbleSet() {
    }

    static void register() {
        // Wall + door pair + window from one 352x128 sheet.
        int[] wall = WallObject.registerWallObjects(
                "cloudmarble", "cloudmarblewall", 2.0F, MAP_CLOUDMARBLE, -1.0F, -1.0F);
        cloudmarbleWallID = wall[0];
        cloudmarbleDoorID = wall[1];
        cloudmarbleWindowID = wall[3];

        // Fence + gate, vanilla ironfence pattern. The three ints are the
        // fence's collision width, height and draw offset.
        cloudmarbleFenceID = ObjectRegistry.registerObject("cloudmarblefence",
                new FenceObject("cloudmarblefence", MAP_SKYGOLD, 12, 10, -26), 2.0F, true);
        cloudmarbleFenceGateID = FenceGateObject.registerGatePair(cloudmarbleFenceID,
                "cloudmarblefencegate", "cloudmarblefencegate", MAP_SKYGOLD, 12, 10, 4.0F)[0];
    }

    /**
     * The passages' ground. Registered with the other tiles rather than in
     * {@link #register()} because tile and object registries close separately.
     */
    static void registerTiles() {
        skywayTileID = TileRegistry.registerTile("skywaytile", new SkywayTile(), 1.0F, true);
    }

    static void registerRecipes() {
        // Cloudmarble is skystone bound with windsilk and finished in gold —
        // the gold is what makes it cost more than plain skystone brick.
        Recipes.registerModRecipe(new Recipe("cloudmarblewall", 4, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 4}, {windsilk, 1}, {goldbar, 1}}")));
        Recipes.registerModRecipe(new Recipe("cloudmarbledoor", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 6}, {windsilk, 2}, {goldbar, 1}}")));
        Recipes.registerModRecipe(new Recipe("cloudmarblewindow", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 4}, {stormshard, 1}, {goldbar, 1}}")));
        Recipes.registerModRecipe(new Recipe("cloudmarblefence", 4, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 2}, {goldbar, 1}}")));
        Recipes.registerModRecipe(new Recipe("cloudmarblefencegate", 1, RecipeTechRegistry.WORKSTATION,
                Recipes.ingredientsFromScript("{{skystone, 4}, {goldbar, 1}}")));
    }
}
