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
        return worldGenSeed(this.getWorldEntity() != null ? this.getWorldEntity().worldSeed : null);
    }

    /**
     * The Veil's terrain seed, derived from the world seed alone.
     *
     * Static and level-free on purpose: the Crooked House placement test runs
     * inside the world-preset system, which is handed a {@code WorldEntity} but
     * never a {@code Level}. It has to be able to ask
     * {@code VeilTerrainPainter.isHollow(...)} the same question the painter
     * will answer later, and get the same answer, without a level in hand.
     */
    public static int worldGenSeed(String worldSeed) {
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

    // ==== settler residents (owner: settlement/VeilResidents) ==============
    /**
     * Mortimer, Caspern and Eleanor stand somewhere in the Veil.
     *
     * All of the rules live in {@code settlement/VeilResidents} rather than
     * here: this level file is shared with the Ghost Realm work, and one call
     * is the smallest hook that can put people in it. See that class for why
     * the Veil is their home today and what happens when the Ghost Realm
     * arrives.
     */
    @Override
    public void onRegionGenerated(Region region, boolean skipGenerateForced) {
        super.onRegionGenerated(region, skipGenerateForced);
        stairwaytoheaven.settlement.VeilResidents.place(this, region, this.getWorldGenSeed());
    }
    // ==== end settler residents ============================================

    @Override
    public boolean canRain() {
        return false;
    }
}
