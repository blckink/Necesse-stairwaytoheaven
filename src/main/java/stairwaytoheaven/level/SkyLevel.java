package stairwaytoheaven.level;

import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.level.maps.BiomeGeneratorStackLevel;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.SkyTerrainPainter;

/**
 * The Skyreach: the persistent one-world dimension one layer above the surface
 * (dimension +1), mirroring how CaveLevel/DeepCaveLevel sit below it.
 *
 * Infinite, generated region-by-region from the world seed. Not a cave
 * ({@code isCave} stays false), so it follows the world's day/night ambient
 * light like the surface does.
 */
public class SkyLevel extends BiomeGeneratorStackLevel {

    /**
     * Required by LevelRegistry: the game reconstructs registered levels through
     * this exact constructor signature when loading a saved world (the seed is
     * restored afterwards via applyLoadData, same as vanilla cave levels).
     */
    public SkyLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
        super(identifier, width, height, worldEntity);
        this.setup();
    }

    /** Used on first generation, when the world generator supplies the seed. */
    public SkyLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity, int seed) {
        super(identifier, width, height, worldEntity, seed);
        this.setup();
    }

    private void setup() {
        this.isCave = false;
        this.baseBiome = SkyRegistry.driftlands;
    }

    @Override
    public void generateRegion(Region region) {
        super.generateRegion(region);
        this.startDirtyRegionTracking();
        int presetGenerationUniqueID = this.getWorldEntity().startPresetGenerationInRegion(region, this.seed);
        try {
            SkyTerrainPainter.paintRegion(region, this.getWorldGenSeed());
        } finally {
            this.getWorldEntity().runPresetGenerationInRegion(presetGenerationUniqueID, region, this.seed);
            this.removeDirtyRegion(region.regionX, region.regionY);
        }
    }

    @Override
    public void onRegionGenerated(Region region, boolean skipGenerateForced) {
        super.onRegionGenerated(region, skipGenerateForced);
        region.checkGenerationValid();
    }

    @Override
    public boolean canRain() {
        // Above the cloud ceiling. Storm weather is a roadmap feature (v0.2).
        return false;
    }

    /**
     * Seed used by the terrain painter. The lazy level-creation path passes no
     * explicit seed (vanilla cave levels then fall back to the world's shared
     * generator stack), so we derive a per-world seed from the persisted world
     * seed string, salted so the sky never mirrors another layer's layout. An
     * explicit non-zero seed (tests, tools) takes precedence.
     */
    public int getWorldGenSeed() {
        if (this.seed != 0) {
            return this.seed;
        }
        String worldSeed = this.getWorldEntity() != null ? this.getWorldEntity().worldSeed : null;
        int derived = (worldSeed != null ? worldSeed.hashCode() : 0) ^ 0x5EED51CE;
        return derived != 0 ? derived : 1;
    }
}
