package stairwaytoheaven.settlement;

import necesse.engine.network.server.Server;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.worldData.WorldData;
import necesse.level.maps.Level;

/**
 * A world-scoped, save-compatible defense level for the War Veteran's
 * turrets and catapults: higher-grade ammo handed to him raises this level,
 * which scales every turret/catapult's damage.
 *
 * <h2>The player-facing half</h2>
 *
 * The level, its save/load and the multiplier the turret/catapult read are
 * defined here. {@link #raiseLevel} is called from
 * {@code VeteranAmmoDialogue}, the War Veteran's "hand over ammunition" talk
 * option: it takes a rising stack of arrows (stone/iron/fire/frost/poison)
 * plus bombs (iron bomb, then dynamite stick) out of the player's inventory
 * through vanilla's own {@code Ingredient}/{@code Recipe.craft}, then raises
 * this level by one. See that class for the exact tiers.
 *
 * <p>{@code WorldData} rather than {@code LevelData}, following
 * {@code SkywatchWorldData}'s own reasoning: a settlement's defense effort is
 * a fact about the WORLD, not about one level, and this shape survives a
 * level being regenerated or unloaded.
 */
public class VeteranDefense extends WorldData {

    public static final String KEY = "veterandefense";
    public static final int MAX_LEVEL = 5;

    public int level = 0;

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addInt("level", this.level);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.level = save.getInt("level", this.level, false);
    }

    public static VeteranDefense get(Server server) {
        if (server == null || server.world == null) {
            return null;
        }
        WorldEntity worldEntity = server.world.worldEntity;
        if (worldEntity == null) {
            return null;
        }
        WorldData data = worldEntity.getWorldData(KEY);
        if (data instanceof VeteranDefense) {
            return (VeteranDefense) data;
        }
        VeteranDefense created = new VeteranDefense();
        worldEntity.addWorldData(KEY, created);
        return created;
    }

    /** Never downgrades; caps at {@link #MAX_LEVEL}. */
    public void raiseLevel(int amount) {
        this.level = Math.min(MAX_LEVEL, this.level + Math.max(0, amount));
    }

    private static VeteranDefense forLevel(Level level) {
        if (level == null || level.getServer() == null) {
            return null;
        }
        return get(level.getServer());
    }

    /** +25% turret damage per defense level, at level 0 = unchanged. */
    public static float turretMultiplier(Level level) {
        VeteranDefense data = forLevel(level);
        return 1.0F + (data == null ? 0 : data.level) * 0.25F;
    }

    /** +40% catapult damage per defense level. */
    public static float catapultMultiplier(Level level) {
        VeteranDefense data = forLevel(level);
        return 1.0F + (data == null ? 0 : data.level) * 0.40F;
    }
}
