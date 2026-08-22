package stairwaytoheaven;

import necesse.engine.commands.CommandsManager;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.LevelRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.network.server.Server;
import necesse.engine.world.WorldGenerator;
import necesse.level.maps.Level;
import stairwaytoheaven.biomes.AuroraShoalsBiome;
import stairwaytoheaven.biomes.DriftlandsBiome;
import stairwaytoheaven.biomes.StormveilBiome;
import stairwaytoheaven.commands.SkyreachStatusCommand;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.objects.SkySideStairwayObject;
import stairwaytoheaven.objects.SkywardStairwayObject;
import stairwaytoheaven.tiles.CloudturfTile;
import stairwaytoheaven.tiles.MistseaTile;
import stairwaytoheaven.tiles.SkystoneTile;
import stairwaytoheaven.tiles.StormslateTile;

/**
 * Stairway to Heaven — adds the Skyreach, a persistent sky dimension one layer
 * above the surface, reached through a craftable stairway pair.
 *
 * Lifecycle (see docs/ARCHITECTURE.md):
 *  - init():          all registry entries (dimension, level, biomes, tiles,
 *                     objects, mobs, items) — registries close right after.
 *  - initResources(): client-side texture loading for mobs.
 *  - postInit():      recipes, loot hooks and the WorldGenerator that
 *                     instantiates the Skyreach (that registry closes after
 *                     postInit).
 */
@ModEntry
public class StairwayToHeavenMod {

    public void init() {
        registerDimension();
        registerBiomes();
        registerTiles();
        registerObjects();
        SkyMobs.register();
        SkyItems.register();
    }

    private void registerDimension() {
        // Vertical layout: deepcave(-2) < cave(-1) < surface(0) < skyreach(+1)
        LevelIdentifier.IDENTIFIER_TO_DIMENSION.put(SkyRegistry.SKYREACH_IDENTIFIER.stringID, SkyRegistry.SKY_DIMENSION);
        LevelRegistry.registerLevel("skylevel", SkyLevel.class);
    }

    private void registerBiomes() {
        // countInStats=false: sub-biomes of the Skyreach never generate as
        // surface islands (same flag vanilla uses for its non-surface biomes)
        SkyRegistry.driftlands = BiomeRegistry.registerBiome("driftlands", new DriftlandsBiome(), false);
        SkyRegistry.stormveil = BiomeRegistry.registerBiome("stormveil", new StormveilBiome(), false);
        SkyRegistry.auroraShoals = BiomeRegistry.registerBiome("aurorashoals", new AuroraShoalsBiome(), false);
    }

    private void registerTiles() {
        SkyRegistry.cloudturfTile = new CloudturfTile();
        SkyRegistry.skystoneTile = new SkystoneTile();
        SkyRegistry.stormslateTile = new StormslateTile();
        SkyRegistry.mistseaTile = new MistseaTile();

        // "...tile" suffix mirrors vanilla naming (rocktile vs. the "stone"
        // item) and keeps tile items from colliding with material items.
        SkyRegistry.cloudturfID = TileRegistry.registerTile("cloudturftile", SkyRegistry.cloudturfTile, 1.0F, true);
        SkyRegistry.skystoneTileID = TileRegistry.registerTile("skystonetile", SkyRegistry.skystoneTile, 1.0F, true);
        SkyRegistry.stormslateID = TileRegistry.registerTile("stormslatetile", SkyRegistry.stormslateTile, 1.0F, true);
        SkyRegistry.mistseaID = TileRegistry.registerTile("mistseatile", SkyRegistry.mistseaTile, 0.0F, false);
    }

    private void registerObjects() {
        SkyRegistry.stairwayDown = new SkywardStairwayObject();
        SkyRegistry.stairwayUp = new SkySideStairwayObject();
        SkyRegistry.stairwayDownID = ObjectRegistry.registerObject("skystairwaydown", SkyRegistry.stairwayDown, 20.0F, true);
        SkyRegistry.stairwayUpID = ObjectRegistry.registerObject("skystairwayup", SkyRegistry.stairwayUp, 0.0F, false);
        SkyRegistry.stairwayDown.ladderUpObjectID = SkyRegistry.stairwayUpID;

        SkyObjects.register();
    }

    public void initResources() {
        SkyMobs.loadTextures();
    }

    public void postInit() {
        SkyItems.registerRecipes();
        registerWorldGenerator();
        CommandsManager.registerServerCommand(new SkyreachStatusCommand());
    }

    private void registerWorldGenerator() {
        WorldGenerator.registerGenerator(new WorldGenerator() {
            @Override
            public Level getNewLevel(LevelIdentifier levelIdentifier, Server server, GameBlackboard blackboard) {
                if (levelIdentifier.equals(SkyRegistry.SKYREACH_IDENTIFIER)) {
                    return new SkyLevel(levelIdentifier, 0, 0, server.world.worldEntity, blackboard.getInt("seed"));
                }
                return null;
            }
        });
    }

    public void dispose() {
    }
}
