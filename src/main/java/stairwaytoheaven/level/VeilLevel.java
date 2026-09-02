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

    // Mortimer, Caspern and Eleanor used to be placed here (an onRegionGenerated
    // override calling settlement.VeilResidents.place) while the Ghost Realm
    // was still being built — see that class's own doc comment. The Ghost
    // Realm has since shipped and is now their one home
    // (realms.ghost.GhostLevel.onRegionGenerated), so the override was removed
    // rather than left as a no-op: a resident who might be behind either of two
    // different portals is a resident the player cannot reliably go looking
    // for, and the Ghost Realm needing "a reason to go there" is the entire
    // point of that move. veil2 keeps generating everything else
    // (VeilTerrainPainter's ground, its own Gloom Shades) exactly as before.

    @Override
    public boolean canRain() {
        return false;
    }
}
