package stairwaytoheaven.journal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import necesse.engine.quest.DeliverItemsQuest;
import necesse.inventory.item.Item;

/**
 * What a delivery quest asks for, read from the quest class itself.
 *
 * <p>The journal must not keep its own copy of "10 Storm Shard, 5 Fulgurite":
 * the numbers live in each quest's constructor ({@code SkyreachKeyQuest},
 * {@code SteinfeldVigilQuest}, ...) and a second copy is a second place for a
 * rebalance to miss. Vanilla keeps them in
 * {@code DeliverItemsQuest.objectives}, a {@code protected} list of
 * {@code ItemObjective(item, itemsAmount)} (VERIFIED [jar] 1.3.2,
 * decompiled {@code DeliverItemsQuest.java:39, :298-300}), with no public
 * getter, so this reads it by reflection from a throwaway instance.
 *
 * <p>If a game version renames those fields, the read fails, the journal shows
 * the quest without its item lines, and {@code /swhjournal} reports
 * {@code asks=0/N} — the integration test asserts all of them resolve.
 */
public final class QuestAsks {

    /** One asked item and its amount. */
    public static final class Ask {
        public final Item item;
        public final int amount;

        Ask(Item item, int amount) {
            this.item = item;
            this.amount = amount;
        }
    }

    private static final Map<Class<?>, List<Ask>> CACHE = new HashMap<>();

    private QuestAsks() {
    }

    /** The asks of a delivery quest class; empty if they cannot be read. */
    public static synchronized List<Ask> of(Class<? extends DeliverItemsQuest> type) {
        List<Ask> cached = CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        List<Ask> asks = new ArrayList<>();
        try {
            DeliverItemsQuest quest = type.getDeclaredConstructor().newInstance();
            Field objectivesField = DeliverItemsQuest.class.getDeclaredField("objectives");
            objectivesField.setAccessible(true);
            Object list = objectivesField.get(quest);
            if (list instanceof List) {
                for (Object objective : (List<?>) list) {
                    Field itemField = objective.getClass().getDeclaredField("item");
                    Field amountField = objective.getClass().getDeclaredField("itemsAmount");
                    itemField.setAccessible(true);
                    amountField.setAccessible(true);
                    Object item = itemField.get(objective);
                    if (item instanceof Item) {
                        asks.add(new Ask((Item) item, amountField.getInt(objective)));
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            asks.clear();
        }
        List<Ask> result = Collections.unmodifiableList(asks);
        CACHE.put(type, result);
        return result;
    }
}
