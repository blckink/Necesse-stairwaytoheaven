package stairwaytoheaven.level;

import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.level.maps.BiomeGeneratorStackLevel;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.VeilTerrainPainter;

/**
 * The Veil: the mod's afterlife layer below the deep caves (dimension -3).
 * Permanent night — {@code isCave} is true, so light comes only from what
 * players and glowing flora provide, and the vanilla "light = safety" spawn
 * contract shapes the whole layer.
 */
public class VeilLevel extends BiomeGeneratorStackLevel {

    public VeilLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
        super(identifier, width, height, worldEntity);
        this.setup();
    }

    public VeilLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity, int seed) {
        super(identifier, width, height, worldEntity, seed);
        this.setup();
    }

    private void setup() {
        this.isCave = true;
        this.baseBiome = SkyRegistry.gloomfen;
    }

    public int getWorldGenSeed() {
        if (this.seed != 0) {
            return this.seed;
        }
        String worldSeed = this.getWorldEntity() != null ? this.getWorldEntity().worldSeed : null;
        int derived = (worldSeed != null ? worldSeed.hashCode() : 0) ^ 0x7E11B310;
        return derived != 0 ? derived : 1;
    }

    @Override
    public void generateRegion(Region region) {
        super.generateRegion(region);
        this.startDirtyRegionTracking();
        int presetGenerationUniqueID = this.getWorldEntity().startPresetGenerationInRegion(region, this.seed);
        try {
            VeilTerrainPainter.paintRegion(region, this.getWorldGenSeed());
        } finally {
            this.getWorldEntity().runPresetGenerationInRegion(presetGenerationUniqueID, region, this.seed);
            this.removeDirtyRegion(region.regionX, region.regionY);
        }
    }

    @Override
    public boolean canRain() {
        return false;
    }
}
