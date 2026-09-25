package stairwaytoheaven.realms.eden;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import necesse.engine.util.GameRandom;
import necesse.entity.objectEntity.FruitGrowerObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.FruitTreeObject;
import necesse.level.maps.Level;

/**
 * Tree of Plenty / Baum der Fülle. Its sprite hangs apples, grapes and other
 * fruit from the same branches, but a vanilla {@link FruitTreeObject} only
 * knows one fruit ID — so every harvest came back as Paradise Apples. This
 * tree rolls each harvested fruit separately from the Eden garden fruits.
 *
 * <p>Both harvest paths go through the entity: the player's interact
 * ({@code harvest -> getHarvestSplitItems}) and the settler job
 * ({@code HarvestFruitLevelJob -> getHarvestItems}). Saved trees pick the new
 * entity up on load, because {@code ObjectEntitySave} asks the object for a
 * fresh entity and then applies the saved stage; the entity type string stays
 * vanilla's {@code fruitgrow}.
 */
public class TreeOfPlentyObject extends FruitTreeObject {

    /** Weight per fruit; the apple stays the common one, as before. */
    private static final Map<String, Integer> FRUIT_WEIGHTS = new LinkedHashMap<>();
    static {
        FRUIT_WEIGHTS.put("paradiseapple", 45);
        FRUIT_WEIGHTS.put("sungrape", 20);
        FRUIT_WEIGHTS.put("edenberry", 20);
        FRUIT_WEIGHTS.put("moonmelon", 15);
    }

    public TreeOfPlentyObject() {
        super("treeofplenty", "sprucelog", "applesapling",
                900.0F, 1800.0F, "paradiseapple", 1.5F, 4,
                new Color(74, 168, 75), 30, 60, 100, "appleleaves");
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new MixedFruitGrowerObjectEntity(level, x, y, (int) (minGrowTimeSeconds * 1000.0F),
                (int) (maxGrowTimeSeconds * 1000.0F), maxStage, fruitStringID, fruitPerStage);
    }

    /** Splits a fruit count into the weighted mix, one roll per fruit. */
    static Map<String, Integer> rollFruitMix(int count, GameRandom random) {
        int total = 0;
        for (int weight : FRUIT_WEIGHTS.values()) {
            total += weight;
        }
        Map<String, Integer> mix = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            int roll = random.nextInt(total);
            for (Map.Entry<String, Integer> entry : FRUIT_WEIGHTS.entrySet()) {
                roll -= entry.getValue();
                if (roll < 0) {
                    mix.merge(entry.getKey(), 1, Integer::sum);
                    break;
                }
            }
        }
        return mix;
    }

    public static class MixedFruitGrowerObjectEntity extends FruitGrowerObjectEntity {

        public MixedFruitGrowerObjectEntity(Level level, int x, int y, int minGrowTimeInMs, int maxGrowTimeInMs,
                                            int maxStage, String fruitStringID, float fruitPerStage) {
            super(level, x, y, minGrowTimeInMs, maxGrowTimeInMs, maxStage, fruitStringID, fruitPerStage);
        }

        @Override
        public ArrayList<InventoryItem> getHarvestItems() {
            ArrayList<InventoryItem> out = new ArrayList<>();
            rollFruitMix(getFruitDropCount(), GameRandom.globalRandom)
                    .forEach((id, amount) -> out.add(new InventoryItem(id, amount)));
            return out;
        }

        @Override
        public ArrayList<InventoryItem> getHarvestSplitItems() {
            ArrayList<InventoryItem> out = new ArrayList<>();
            rollFruitMix(getFruitDropCount(), GameRandom.globalRandom)
                    .forEach((id, amount) -> new LootItem(id, amount).splitItems(5)
                            .addItems(out, GameRandom.globalRandom, 1.0F, new Object[0]));
            return out;
        }
    }
}
