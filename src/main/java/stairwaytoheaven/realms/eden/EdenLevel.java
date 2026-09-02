package stairwaytoheaven.realms.eden;

import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.Mob;
import necesse.level.maps.BiomeGeneratorStackLevel;
import necesse.level.maps.regionSystem.Region;

/** Infinite generated level for Eden, dimension +2. */
public class EdenLevel extends BiomeGeneratorStackLevel {

    public EdenLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
        super(identifier, width, height, worldEntity);
        setup();
    }

    public EdenLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity, int seed) {
        super(identifier, width, height, worldEntity, seed);
        setup();
    }

    private void setup() {
        this.isCave = false;
        this.baseBiome = EdenRealm.garden;
    }

    public int getWorldGenSeed() {
        if (this.seed != 0) {
            return this.seed;
        }
        return worldGenSeed(this.getWorldEntity() != null ? this.getWorldEntity().worldSeed : null);
    }

    public static int worldGenSeed(String worldSeed) {
        int derived = (worldSeed != null ? worldSeed.hashCode() : 0) ^ 0x3DE05A11;
        return derived != 0 ? derived : 1;
    }

    @Override
    public void generateRegion(Region region) {
        super.generateRegion(region);
        this.startDirtyRegionTracking();
        int presetGenerationUniqueID = this.getWorldEntity().startPresetGenerationInRegion(region, this.seed);
        try {
            EdenTerrainPainter.paintRegion(region, this.getWorldGenSeed());
        } finally {
            this.getWorldEntity().runPresetGenerationInRegion(presetGenerationUniqueID, region, this.seed);
            this.removeDirtyRegion(region.regionX, region.regionY);
        }
    }

    @Override
    public void onRegionGenerated(Region region, boolean skipGenerateForced) {
        super.onRegionGenerated(region, skipGenerateForced);
        region.checkGenerationValid();
        placeResident(region);
    }

    @Override
    public boolean canRain() {
        return false;
    }

    /**
     * Eveleen, the Eden Botanist, standing wherever a Knowledge Tree grew.
     *
     * <p>Mirrors {@code stairwaytoheaven.level.SkyLevel#placeResident} exactly —
     * deterministic from the level seed and the region, one per world (shared
     * with the settlement-arrival route via
     * {@link stairwaytoheaven.quest.SkywatchWorldData#residentClaimed}), never
     * near a workstation because Eden has none, but always near the ONE
     * landmark this realm actually has that says "somebody who works this
     * ground would stand here": the Knowledge Tree {@code EdenTerrainPainter}
     * already scatters through the Canopy. §5's own unlock line —
     * "unlocked by discovering an Eden island + collecting three Eden
     * plants" — is what {@link EdenArrivalQuest} and {@link EdenPlantsQuest}
     * turn into a quest chain; finding her here is the "discovering an Eden
     * island" half made literal.
     */
    private void placeResident(Region region) {
        if (this.isClient()) {
            return;
        }
        long seed = (this.getWorldGenSeed() * 0x9E3779B97F4A7C15L)
                ^ ((long) region.regionX * 0x2E4B8F17L)
                ^ ((long) region.regionY * 0x5A8C3F61L);
        GameRandom random = new GameRandom(seed);
        if (!random.getChance(RESIDENT_REGION_CHANCE)) {
            return;
        }
        String who = "eveleensettler";
        for (Mob existing : this.entityManager.mobs) {
            if (who.equals(existing.getStringID())) {
                return;
            }
        }
        necesse.engine.network.server.Server server = this.getServer();
        if (server != null && stairwaytoheaven.quest.SkywatchWorldData.residentClaimed(server, who)) {
            return;
        }
        for (int attempt = 0; attempt < 40; attempt++) {
            int tileX = region.tileXOffset + random.getIntBetween(2, region.tileWidth - 3);
            int tileY = region.tileYOffset + random.getIntBetween(2, region.tileHeight - 3);
            if (!this.isTileWithinBounds(tileX, tileY) || this.isSolidTile(tileX, tileY)) {
                continue;
            }
            if (this.getObjectID(tileX, tileY) != 0) {
                continue;
            }
            if (!this.hasKnowledgeTreeNear(tileX, tileY)) {
                continue;
            }
            Mob mob = MobRegistry.getMob(who, this);
            if (mob == null) {
                return;
            }
            mob.canDespawn = false;
            this.entityManager.addMob(mob, tileX * 32 + 16, tileY * 32 + 16);
            if (server != null) {
                stairwaytoheaven.quest.SkywatchWorldData.claimResident(server, who);
            }
            return;
        }
    }

    /** Is a Knowledge Tree within three tiles? */
    private boolean hasKnowledgeTreeNear(int tileX, int tileY) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                if (this.getObjectID(tileX + dx, tileY + dy) == EdenRealm.knowledgeTreeID) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Rarer than the Skyreach's own residents (0.16F): a Knowledge Tree is
     * itself rare (§5: "rare worldgen object"), so a region has to both roll a
     * tree AND roll Eveleen. Kept the same order of magnitude so she is found
     * inside a normal afternoon of exploring, not after circling the realm.
     */
    private static final float RESIDENT_REGION_CHANCE = 0.35F;
}
