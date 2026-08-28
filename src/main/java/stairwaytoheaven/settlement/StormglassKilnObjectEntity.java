package stairwaytoheaven.settlement;

import java.awt.Color;

import necesse.engine.util.GameRandom;
import necesse.entity.objectEntity.ProcessingTechInventoryObjectEntity;
import necesse.entity.particle.Particle;
import necesse.level.maps.Level;

/**
 * The Stormglass Kiln's inventory, on vanilla's {@code CheesePressObjectEntity}
 * pattern: a {@link ProcessingTechInventoryObjectEntity} with two input and two
 * output slots and no fuel at all. The kiln's heat is the fulgurite's own
 * stored lightning, which is why this one does not burn logs.
 *
 * <p>Constructor arguments after {@code (level, type, x, y)} are
 * {@code inputSlots, outputSlots, techs...}.
 */
public class StormglassKilnObjectEntity extends ProcessingTechInventoryObjectEntity {

    /** Time for one firing. Vanilla's cheese press takes 60s. */
    public static int recipeProcessTime = 30000;

    public StormglassKilnObjectEntity(Level level, int x, int y) {
        super(level, "stormglasskiln", x, y, 2, 2, SkyProfessions.STORMGLASS_KILN);
    }

    @Override
    public int getProcessTime() {
        return recipeProcessTime;
    }

    @Override
    public void clientTick() {
        super.clientTick();
        if (this.isProcessing() && GameRandom.globalRandom.nextInt(10) == 0) {
            int startHeight = 24 + GameRandom.globalRandom.nextInt(16);
            this.getLevel().entityManager
                    .addParticle((float) (this.tileX * 32 + GameRandom.globalRandom.getIntBetween(10, 22)),
                            (float) (this.tileY * 32 + 32), Particle.GType.COSMETIC)
                    .color(new Color(196, 176, 148))
                    .heightMoves((float) startHeight, (float) (startHeight + 20))
                    .lifeTime(1000);
        }
    }
}
