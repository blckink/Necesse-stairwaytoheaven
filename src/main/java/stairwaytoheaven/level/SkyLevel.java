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
import stairwaytoheaven.worldgen.SkyOrigin;
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
     * once per world (persisted in SkywatchQuestData).
     *
     * v0.5: the spire is THE canonical sky origin (see {@link SkyOrigin}) —
     * every Stairway ascends to it, and the terrain painter guarantees a solid
     * Driftlands hub island around this exact position, so no site search is
     * needed anymore (the old player-anchored sweep is gone: stairway placement
     * on the surface must not influence sky geography, or players could
     * relocate the hub — and with it the whole difficulty gradient — by
     * placing stairways far from spawn).
     */
    public void ensureWardenSpire() {
        SkywatchQuestData quest = SkywatchQuestData.get(this);
        if (!quest.spirePlaced) {
            Point site = SkyOrigin.compute(this.getWorldGenSeed());
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

    /** First land spot in the right sub-biome, sweeping outward from the spire. */
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
     *
     * The derivation itself lives in {@link SkyOrigin#worldGenSeed} so surface
     * code (stairway portals) computes identical sky positions.
     */
    public int getWorldGenSeed() {
        if (this.seed != 0) {
            return this.seed;
        }
        return SkyOrigin.worldGenSeed(this.getWorldEntity());
    }
}
