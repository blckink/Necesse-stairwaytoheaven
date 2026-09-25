package stairwaytoheaven;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import necesse.engine.GameEventListener;
import necesse.engine.GameEvents;
import necesse.engine.events.ServerStartEvent;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameUtils;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.placeableItem.MobSpawnItem;
import necesse.inventory.item.placeableItem.objectItem.ObjectItem;
import necesse.inventory.item.placeableItem.tileItem.TileItem;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.level.gameObject.GameObject;

/**
 * Where every item this mod registers ends up for the player: inventory sort
 * category, crafting-menu category, obtainability, broker value, rarity.
 *
 * <p>Why it exists (2026-09-24): the player saw "Großteil als Objekte" -- most of
 * the mod's things sorted under the vanilla "objects" root or under "misc",
 * because {@code GameObject.itemCategoryTree} defaults to {@code {"objects"}}
 * and {@code Item}'s to {@code {"misc"}} (jar 1.3.2, GameObject.java:113,
 * Item.java:180). Nothing in the build notices that, so this prints one line
 * per item at server start and a summary line the integration test asserts on.
 *
 * <p>Read the categories out of the engine's own managers, not out of our
 * source: {@code ObjectItem} copies its object's tree at construction time, so
 * a {@code setItemCategory} on the object AFTER {@code registerObject} does
 * nothing, and only the manager knows what actually stuck.
 *
 * <p>Output, one line per item:
 * {@code swhcat item <id> kind=<item|object|tile|mob> class=<C> cat=<a.b> craft=<a.b> obt=<0|1> creative=<0|1> broker=<v> rarity=<R> loot=<self|custom|-> recipe=<tech|->}
 * and one summary:
 * {@code swhcat census: items=N obtainable=N bad=N objectsroot=N miscroot=N tilesroot=N craftroot=N}
 * where {@code bad} counts listed items a player can hold (obtainable or in
 * creative) whose inventory category is a bare fallback root ("objects",
 * "misc", "tiles"), plus recipe results whose crafting category is one
 * ({@link #isBadCraftRoot}) -- the state this class exists to catch.
 * {@code scripts/integration_test.sh} asserts {@code bad=0}.
 * Every such item is also printed as {@code swhcat BAD <id> ...}.
 */
public final class CategoryCensus {

    private static boolean logged;

    private CategoryCensus() {
    }

    public static void register() {
        GameEvents.addListener(ServerStartEvent.class, new GameEventListener<ServerStartEvent>() {
            @Override
            public void onEvent(ServerStartEvent event) {
                if (logged) {
                    return;
                }
                logged = true;
                try {
                    run();
                } catch (Throwable t) {
                    System.out.println("swhcat census: FAILED " + t);
                }
                try {
                    probeLoot(event);
                } catch (Throwable t) {
                    System.out.println("swhcat loot: FAILED " + t);
                }
                try {
                    probeHarvest(event);
                } catch (Throwable t) {
                    System.out.println("swhcat harvest: FAILED " + t);
                }
            }
        });
    }

    private static boolean isOurs(Item item) {
        LoadedMod mod = ItemRegistry.getItemMod(item.getID());
        return mod != null && "stairwaytoheaven".equals(mod.id);
    }

    private static String tree(ItemCategory c) {
        if (c == null) {
            return "NONE";
        }
        return GameUtils.join(c.getStringIDTree(false), ".");
    }

    /** A bare root that a real registration never means to land in. */
    static boolean isBareRoot(String tree) {
        return tree.equals("NONE") || tree.equals("objects") || tree.equals("misc") || tree.equals("tiles");
    }

    /**
     * The crafting tree is flatter: its "tiles" root has no children at all
     * (ItemCategory.java:205), so a floor crafted under "tiles" is correct and
     * only the fallbacks -- Item's "misc", GameObject's bare "objects" -- are
     * findings for a recipe result.
     */
    static boolean isBadCraftRoot(String tree) {
        return tree.equals("NONE") || tree.equals("objects") || tree.equals("misc");
    }

    private static String name(Class<?> c) {
        while (c != null && c.getSimpleName().isEmpty()) {
            c = c.getSuperclass();
        }
        return c == null ? "?" : c.getSimpleName();
    }

    private static String lootSource(GameObject object) {
        try {
            Method m = object.getClass().getMethod("getLootTable",
                    necesse.level.maps.Level.class, int.class, int.class, int.class);
            return m.getDeclaringClass() == GameObject.class ? "self" : "custom";
        } catch (Throwable t) {
            return "?";
        }
    }

    /**
     * What a piece breaks into, natural vs player-placed, asked of the real
     * {@code getLootTable} on a throwaway 8x8 level that is never added to the
     * world and never saved. Prints
     * {@code swhcat loot <id> natural=[..] placed=[..]}.
     */
    static final String[] LOOT_PROBES = {"deadtree", "stormscreed", "skywatchrubble", "chargecrystal",
            "aurorashards", "starfall", "withershrub", "skyreeds", "whisperreeds", "overgrowngrass",
            "windwheat", "gloomshroom", "skywatchtelescope"};

    private static void probeLoot(ServerStartEvent event) {
        necesse.level.maps.Level level = new necesse.level.maps.Level(
                new necesse.engine.util.LevelIdentifier("swhcatprobe"), 8, 8, event.server.world.worldEntity);
        for (String id : LOOT_PROBES) {
            int objectID = necesse.engine.registries.ObjectRegistry.getObjectID(id);
            if (objectID < 0) {
                System.out.println("swhcat loot " + id + " MISSING");
                continue;
            }
            GameObject object = necesse.engine.registries.ObjectRegistry.getObject(objectID);
            StringBuilder sb = new StringBuilder("swhcat loot " + id);
            for (boolean byPlayer : new boolean[]{false, true}) {
                level.objectLayer.setObject(0, 3, 3, objectID);
                level.objectLayer.setIsPlayerPlaced(0, 3, 3, byPlayer);
                necesse.inventory.lootTable.LootList list = new necesse.inventory.lootTable.LootList();
                object.getLootTable(level, 0, 3, 3).addPossibleLoot(list);
                java.util.TreeSet<String> ids = new java.util.TreeSet<>();
                for (Item i : list.getItems()) {
                    ids.add(i.getStringID());
                }
                sb.append(byPlayer ? " placed=" : " natural=").append(ids);
            }
            System.out.println(sb);
        }
    }

    /**
     * What a fruit tree hands out, asked of its real object entity on a
     * throwaway level: both the player's harvest (split items) and the
     * settler job's harvest, over enough stages to see every fruit. Prints
     * {@code swhcat harvest <id> fruits=[..]}.
     */
    static final String[] HARVEST_PROBES = {"treeofplenty"};

    private static void probeHarvest(ServerStartEvent event) {
        necesse.level.maps.Level level = new necesse.level.maps.Level(
                new necesse.engine.util.LevelIdentifier("swhharvestprobe"), 8, 8, event.server.world.worldEntity);
        for (String id : HARVEST_PROBES) {
            int objectID = necesse.engine.registries.ObjectRegistry.getObjectID(id);
            if (objectID < 0) {
                System.out.println("swhcat harvest " + id + " MISSING");
                continue;
            }
            GameObject object = necesse.engine.registries.ObjectRegistry.getObject(objectID);
            necesse.entity.objectEntity.ObjectEntity entity = object.getNewObjectEntity(level, 3, 3);
            if (!(entity instanceof necesse.entity.objectEntity.FruitGrowerObjectEntity)) {
                System.out.println("swhcat harvest " + id + " NOFRUIT");
                continue;
            }
            necesse.entity.objectEntity.FruitGrowerObjectEntity grower =
                    (necesse.entity.objectEntity.FruitGrowerObjectEntity) entity;
            java.util.TreeSet<String> ids = new java.util.TreeSet<>();
            for (int i = 0; i < 200; i++) {
                grower.setRandomStage(new necesse.engine.util.GameRandom(i));
                for (necesse.inventory.InventoryItem item : grower.getHarvestSplitItems()) {
                    ids.add(item.item.getStringID());
                }
                for (necesse.inventory.InventoryItem item : grower.getHarvestItems()) {
                    ids.add(item.item.getStringID());
                }
            }
            System.out.println("swhcat harvest " + id + " fruits=" + ids);
        }
    }

    private static void run() {
        TreeMap<String, String> lines = new TreeMap<>();
        List<String> bad = new ArrayList<>();
        int items = 0, obtainable = 0, objectsRoot = 0, miscRoot = 0, tilesRoot = 0, craftRoot = 0;
        for (Item item : ItemRegistry.getItems()) {
            if (!isOurs(item)) {
                continue;
            }
            items++;
            int id = item.getID();
            String kind = "item";
            String cls = name(item.getClass());
            String loot = "-";
            boolean listed = true;
            if (item instanceof ObjectItem) {
                kind = "object";
                GameObject object = ((ObjectItem) item).getObject();
                if (object != null) {
                    cls = name(object.getClass());
                    loot = lootSource(object);
                    listed = object.shouldShowInItemList();
                }
            } else if (item instanceof TileItem) {
                kind = "tile";
                if (((TileItem) item).getTile() != null) {
                    cls = name(((TileItem) item).getTile().getClass());
                }
            } else if (item instanceof MobSpawnItem) {
                kind = "mob";
            }
            String cat = tree(ItemCategory.masterManager.getItemsCategory(item));
            String craft = tree(ItemCategory.craftingManager.getItemsCategory(item));
            boolean obt = ItemRegistry.isObtainable(id);
            boolean creative = ItemRegistry.isObtainableInCreative(id);
            String rarity;
            try {
                rarity = String.valueOf(item.getRarity(new InventoryItem(item)));
            } catch (Throwable t) {
                rarity = "?";
            }
            String recipe = "-";
            try {
                List<Recipe> recipes = Recipes.getRecipesFromResult(id);
                if (recipes != null && !recipes.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Recipe r : recipes) {
                        if (sb.length() > 0) {
                            sb.append('+');
                        }
                        sb.append(r.tech == null ? "none" : r.tech.getStringID());
                    }
                    recipe = sb.toString();
                }
            } catch (Throwable t) {
                recipe = "?";
            }
            if (obt) {
                obtainable++;
            }
            if (cat.equals("objects")) objectsRoot++;
            if (cat.equals("misc")) miscRoot++;
            if (cat.equals("tiles")) tilesRoot++;
            if (isBadCraftRoot(craft) && !recipe.equals("-")) craftRoot++;
            String line = item.getStringID() + " kind=" + kind + " class=" + cls + " cat=" + cat
                    + " craft=" + craft + " obt=" + (obt ? 1 : 0) + " creative=" + (creative ? 1 : 0)
                    + " listed=" + (listed ? 1 : 0)
                    + " broker=" + String.format(Locale.ROOT, "%.1f", ItemRegistry.getBrokerValue(id))
                    + " rarity=" + rarity + " loot=" + loot + " recipe=" + recipe;
            lines.put(item.getStringID(), line);
            // A slave half of a multi-tile object is never shown in any list,
            // so its category is invisible and not a finding.
            if ((obt || creative) && listed && isBareRoot(cat)) {
                bad.add(line);
            } else if (listed && !recipe.equals("-") && isBadCraftRoot(craft)) {
                bad.add(line + " (craft category is a bare root)");
            }
        }
        for (String line : lines.values()) {
            System.out.println("swhcat item " + line);
        }
        for (String line : bad) {
            System.out.println("swhcat BAD " + line);
        }
        System.out.println("swhcat census: items=" + items + " obtainable=" + obtainable
                + " bad=" + bad.size() + " objectsroot=" + objectsRoot + " miscroot=" + miscRoot
                + " tilesroot=" + tilesRoot + " craftroot=" + craftRoot);
    }
}
