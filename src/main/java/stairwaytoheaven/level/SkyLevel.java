package stairwaytoheaven.level;

import java.awt.Point;

import necesse.engine.registries.MobRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.level.maps.BiomeGeneratorStackLevel;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyTerrainPainter;
import stairwaytoheaven.worldgen.WardenSpirePreset;

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
        // Above the cloud ceiling. Storm weather is a roadmap feature (v0.3).
        return false;
    }

    private int structureHealCounter;

    @Override
    public void serverTick() {
        super.serverTick();
        // The spire is stamped on the FIRST ASCENT near the player's arrival
        // stairway (see ensureWardenSpire(anchor)); the tick only maintains an
        // already-placed spire: cat spawns + healing the quest beacon.
        SkywatchQuestData quest = SkywatchQuestData.get(this);
        if (!quest.spirePlaced) {
            return;
        }
        if (!quest.catsSpawned) {
            this.spawnSpireCats(quest);
        }
        if (++this.structureHealCounter >= 200) {
            this.structureHealCounter = 0;
            this.healQuestStructure(quest);
        }
    }

    /**
     * Restores the quest beacon if it went missing (older jars allowed mining
     * it, which dropped nothing and would soft-lock the chain). Only touches
     * loaded regions — never forces region loads from the tick.
     */
    private void healQuestStructure(SkywatchQuestData quest) {
        if (!this.regionManager.isTileLoaded(quest.beaconX, quest.beaconY)) {
            return;
        }
        int current = this.getObjectID(quest.beaconX, quest.beaconY);
        if (current == SkyRegistry.wardenBeaconOffID || current == SkyRegistry.wardenBeaconOnID) {
            return;
        }
        int wanted = quest.stage >= 2 ? SkyRegistry.wardenBeaconOnID : SkyRegistry.wardenBeaconOffID;
        this.setObject(quest.beaconX, quest.beaconY, wanted);
        if (this.getServer() != null) {
            this.getServer().network.sendToClientsWithTile(
                    new necesse.engine.network.packet.PacketChangeObject(this, 0, quest.beaconX, quest.beaconY, wanted),
                    this, quest.beaconX, quest.beaconY);
        }
    }

    /**
     * Lazily stamps the Warden's Spire and spawns the spire cats, exactly
     * once per world (persisted in SkywatchQuestData). Anchored to the given
     * point — the player's arrival stairway on the first ascent — so the
     * spire always sits within walking distance of where they come up
     * (playtests: a spire anchored to the world origin could be hundreds of
     * tiles from a base far from spawn). The status command passes the origin
     * as a headless fallback.
     */
    public void ensureWardenSpire(int anchorX, int anchorY) {
        SkywatchQuestData quest = SkywatchQuestData.get(this);
        if (!quest.spirePlaced) {
            Point site = this.findSpireSite(anchorX, anchorY);
            int half = WardenSpirePreset.SIZE / 2;
            this.regionManager.ensureTilesAreLoaded(site.x - half - 2, site.y - half - 2,
                    site.x + half + 2, site.y + half + 2);
            new WardenSpirePreset().applyToLevelCentered(this, site.x, site.y);
            // applyToLevel runs the custom-apply hook, which sets spirePlaced
            // and the quest anchor points; guard against a silent failure so
            // we never re-stamp every tick.
            quest.spirePlaced = true;
        }
        if (!quest.catsSpawned && quest.spirePlaced) {
            this.spawnSpireCats(quest);
        }
    }

    /**
     * Deterministic spire site: sweep outward from the anchor and take the
     * first spot whose 15x15 footprint is solidly on Driftlands land. Starts
     * close (radius 20) so the spire lands near the arrival stairway.
     */
    private Point findSpireSite(int anchorX, int anchorY) {
        int seed = this.getWorldGenSeed();
        for (int radius = 20; radius <= 400; radius += 6) {
            for (int angleStep = 0; angleStep < 24; angleStep++) {
                double angle = (angleStep / 24.0 + (radius % 12) / 24.0) * Math.PI * 2;
                int x = anchorX + (int) Math.round(Math.cos(angle) * radius);
                int y = anchorY + (int) Math.round(Math.sin(angle) * radius);
                if (this.isSolidDriftland(seed, x, y, 7)) {
                    return new Point(x, y);
                }
            }
        }
        return new Point(anchorX, anchorY); // pathological seed: land here regardless
    }

    private boolean isSolidDriftland(int seed, int cx, int cy, int half) {
        for (int dx = -half; dx <= half; dx += half) {
            for (int dy = -half; dy <= half; dy += half) {
                float island = SkyNoise.fbm(seed, cx + dx, cy + dy, SkyTerrainPainter.ISLAND_SCALE, 3);
                if (island <= SkyTerrainPainter.ISLAND_THRESHOLD + 0.03F) {
                    return false;
                }
            }
        }
        float biome = SkyNoise.fbm(seed + SkyTerrainPainter.SALT_BIOME, cx, cy, SkyTerrainPainter.BIOME_SCALE, 2);
        return biome >= SkyTerrainPainter.STORMVEIL_BELOW && biome <= SkyTerrainPainter.AURORA_ABOVE;
    }

    private void spawnSpireCats(SkywatchQuestData quest) {
        int seed = this.getWorldGenSeed();
        Point blackLair = this.findLairSite(seed, quest, true);
        Point tabbyLair = this.findLairSite(seed, quest, false);
        quest.blackLairX = blackLair.x;
        quest.blackLairY = blackLair.y;
        quest.tabbyLairX = tabbyLair.x;
        quest.tabbyLairY = tabbyLair.y;

        this.regionManager.ensureTileIsLoaded(blackLair.x, blackLair.y);
        this.entityManager.addMob(MobRegistry.getMob("spirecatblack", this), blackLair.x * 32 + 16, blackLair.y * 32 + 16);
        this.regionManager.ensureTileIsLoaded(tabbyLair.x, tabbyLair.y);
        this.entityManager.addMob(MobRegistry.getMob("spirecattabby", this), tabbyLair.x * 32 + 16, tabbyLair.y * 32 + 16);
        quest.catsSpawned = true;
    }

    /** First land spot in the right sub-biome, sweeping outward from the spire. */
    private Point findLairSite(int seed, SkywatchQuestData quest, boolean stormveil) {
        for (int radius = 48; radius <= 600; radius += 8) {
            for (int angleStep = 0; angleStep < 20; angleStep++) {
                double angle = (angleStep / 20.0 + (stormveil ? 0.0 : 0.5) / 20.0 + (radius % 16) / 40.0) * Math.PI * 2;
                int x = quest.spireX + (int) Math.round(Math.cos(angle) * radius);
                int y = quest.spireY + (int) Math.round(Math.sin(angle) * radius);
                float island = SkyNoise.fbm(seed, x, y, SkyTerrainPainter.ISLAND_SCALE, 3);
                if (island <= SkyTerrainPainter.ISLAND_THRESHOLD + SkyTerrainPainter.ISLAND_RIM) {
                    continue;
                }
                float biome = SkyNoise.fbm(seed + SkyTerrainPainter.SALT_BIOME, x, y, SkyTerrainPainter.BIOME_SCALE, 2);
                boolean matches = stormveil
                        ? biome < SkyTerrainPainter.STORMVEIL_BELOW
                        : biome > SkyTerrainPainter.AURORA_ABOVE;
                if (matches) {
                    return new Point(x, y);
                }
            }
        }
        // Fallback: beside the spire, so the quest is never soft-locked
        return new Point(quest.spireX + (stormveil ? -3 : 3), quest.spireY + 3);
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
