package stairwaytoheaven.settlement;

import java.util.HashMap;
import java.util.Map;

import necesse.engine.network.server.Server;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.worldData.WorldData;
import necesse.level.maps.Level;

/**
 * How many Blood Vials stand in each Blood Bowl — decision E3 of
 * {@code docs/design/concept-nightbound-dorian.md}: <i>"Du legst
 * Blutfläschchen hinein. Er trinkt nachts daraus, bevor er beißt."</i>
 *
 * <p>The bowl borrows the Soul Basin's world sheet ({@code BloodBowlObject}),
 * which is a plain deco object with no object entity to hold an inventory, so
 * the stock lives here, keyed by level and tile. World-scoped rather than
 * level data because a settlement — and Dorian — can be on any level.
 *
 * <p>Registered as its own {@code WorldData} in {@code StairwayToHeavenMod};
 * an unregistered one throws in its constructor.
 */
public class NightboundWorldData extends WorldData {

    public static final String KEY = "swhnightbound";

    /** The most any one bowl holds: enough for a week of nights. */
    public static final int BOWL_CAPACITY = 12;

    private final HashMap<String, Integer> bowls = new HashMap<>();

    public static String key(Level level, int tileX, int tileY) {
        return level.getIdentifier().stringID + ":" + tileX + ":" + tileY;
    }

    public int stock(Level level, int tileX, int tileY) {
        return this.bowls.getOrDefault(key(level, tileX, tileY), 0);
    }

    public void setStock(Level level, int tileX, int tileY, int amount) {
        String k = key(level, tileX, tileY);
        if (amount <= 0) {
            this.bowls.remove(k);
        } else {
            this.bowls.put(k, Math.min(BOWL_CAPACITY, amount));
        }
    }

    /** Every bowl on this level that still holds something, as tile pairs. */
    public java.util.List<int[]> filledBowls(Level level) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        String prefix = level.getIdentifier().stringID + ":";
        for (Map.Entry<String, Integer> e : this.bowls.entrySet()) {
            if (e.getValue() > 0 && e.getKey().startsWith(prefix)) {
                String[] parts = e.getKey().substring(prefix.length()).split(":");
                try {
                    out.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
                } catch (RuntimeException ignored) {
                    // a malformed key is skipped, never fatal
                }
            }
        }
        return out;
    }

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        String[] keys = new String[this.bowls.size()];
        int[] values = new int[this.bowls.size()];
        int i = 0;
        for (Map.Entry<String, Integer> e : this.bowls.entrySet()) {
            keys[i] = e.getKey();
            values[i] = e.getValue();
            i++;
        }
        save.addStringArray("bowlKeys", keys);
        save.addIntArray("bowlValues", values);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.bowls.clear();
        String[] keys = save.getStringArray("bowlKeys", new String[0], false);
        int[] values = save.getIntArray("bowlValues", new int[0], false);
        for (int i = 0; i < Math.min(keys.length, values.length); i++) {
            if (keys[i] != null && values[i] > 0) {
                this.bowls.put(keys[i], values[i]);
            }
        }
    }

    public static NightboundWorldData get(Server server) {
        if (server == null || server.world == null) {
            return null;
        }
        WorldEntity worldEntity = server.world.worldEntity;
        if (worldEntity == null) {
            return null;
        }
        WorldData data = worldEntity.getWorldData(KEY);
        if (data instanceof NightboundWorldData) {
            return (NightboundWorldData) data;
        }
        NightboundWorldData created = new NightboundWorldData();
        worldEntity.addWorldData(KEY, created);
        return created;
    }
}
