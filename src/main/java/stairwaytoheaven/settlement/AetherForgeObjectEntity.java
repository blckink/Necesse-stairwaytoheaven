package stairwaytoheaven.settlement;

import necesse.entity.objectEntity.AnyLogFueledProcessingTechInventoryObjectEntity;
import necesse.inventory.InventoryItem;
import necesse.inventory.recipe.Recipe;
import necesse.level.maps.Level;

/**
 * The Aether Forge's inventory. Straight off vanilla's
 * {@code ProcessingForgeObjectEntity}: an
 * {@link AnyLogFueledProcessingTechInventoryObjectEntity}, which fixes the fuel
 * at two slots of anything carrying the vanilla {@code anylog} global
 * ingredient — every sky wood the mod registers already is one — and processes
 * whichever recipe of its techs the input slots can currently satisfy.
 *
 * <p>Constructor arguments after {@code (level, type, x, y)} are
 * {@code inputSlots, outputSlots, fuelAlwaysOn, fuelRunsOutWhenNotProcessing,
 * runningOutOfFuelResetsProcessingTime, techs...} — the same
 * {@code 2, 2, false, false, true} vanilla's forge passes.
 *
 * <p>Slower than the vanilla forge on purpose: 11 seconds a bar against the
 * forge's 8, because it doubles the yield of aetherium ore and unlocks a bar
 * the vanilla forge cannot make.
 */
public class AetherForgeObjectEntity extends AnyLogFueledProcessingTechInventoryObjectEntity {

    /** One log runs the forge for this long. Vanilla's forge burns 40s. */
    public static int logFuelTime = 40000;
    /** Time for one craft. Vanilla's forge is 8000. */
    public static int recipeProcessTime = 11000;

    public AetherForgeObjectEntity(Level level, int x, int y) {
        super(level, "aetherforge", x, y, 2, 2, false, false, true, SkyProfessions.AETHER_FORGE);
    }

    @Override
    public int getFuelTime(InventoryItem item) {
        return logFuelTime;
    }

    @Override
    public int getProcessTime(Recipe recipe) {
        return recipeProcessTime;
    }

    @Override
    public boolean shouldBeAbleToChangeKeepFuelRunning() {
        return false;
    }
}
