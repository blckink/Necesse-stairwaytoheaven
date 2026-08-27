package stairwaytoheaven;

import necesse.engine.commands.CommandsManager;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.LevelDataRegistry;
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
import stairwaytoheaven.biomes.GloomfenBiome;
import stairwaytoheaven.biomes.AshenReachBiome;
import stairwaytoheaven.commands.SkyreachStatusCommand;
import stairwaytoheaven.commands.VeilStatusCommand;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.level.VeilLevel;
import stairwaytoheaven.objects.SkySideStairwayObject;
import stairwaytoheaven.objects.SkywardStairwayObject;
import stairwaytoheaven.objects.SeanceCircleObject;
import stairwaytoheaven.objects.VeilRiftObject;
import stairwaytoheaven.objects.VeilSideRiftObject;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.tiles.CloudturfTile;
import stairwaytoheaven.tiles.MistseaTile;
import stairwaytoheaven.tiles.SkystoneTile;
import stairwaytoheaven.tiles.StormslateTile;
import stairwaytoheaven.tiles.MurkmossTile;
import stairwaytoheaven.tiles.BlackpeatTile;
import stairwaytoheaven.tiles.AshsandTile;
import stairwaytoheaven.tiles.MurkwaterTile;

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
        SkyBuildingSet.register();
        SkyMobs.register();
        SkyItems.register();
        SkyBuildingSet.registerItems();
        LevelDataRegistry.registerLevelData(SkywatchQuestData.KEY, SkywatchQuestData.class);
        // HUD quest layer: real journal/sidebar quests mirroring the Warden
        // chain (see docs/research/quest-api.md). Items must already be
        // registered — the delivery quests reference them by stringID.
        necesse.engine.registries.QuestRegistry.registerQuest("swh_findspire", stairwaytoheaven.quest.FindSpireQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_recruitwarden", stairwaytoheaven.quest.RecruitWardenQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_beacon", stairwaytoheaven.quest.BeaconDeliveryQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_cats", stairwaytoheaven.quest.SpireCatsQuest.class);
        necesse.engine.registries.QuestRegistry.registerQuest("swh_anchor", stairwaytoheaven.quest.AnchorDeliveryQuest.class);
        // World-map icons for the auto-placed markers (spire + return
        // stairway). Textures load client-side via GameResources; the
        // registration itself is texture-free and server-safe.
        necesse.engine.registries.MapIconRegistry.registerIcon("skyspire",
                new necesse.level.maps.mapData.TextureGameMapIcon("ui/mapicons/skyspire"));
        necesse.engine.registries.MapIconRegistry.registerIcon("skystairs",
                new necesse.level.maps.mapData.TextureGameMapIcon("ui/mapicons/skystairs"));
        necesse.engine.registries.MapIconRegistry.registerIcon("skycat",
                new necesse.level.maps.mapData.TextureGameMapIcon("ui/mapicons/skycat"));
    }

    private void registerDimension() {
        // Vertical layout: veil(-3) < deepcave(-2) < cave(-1) < surface(0) < skyreach(+1)
        LevelIdentifier.IDENTIFIER_TO_DIMENSION.put(SkyRegistry.SKYREACH_IDENTIFIER.stringID, SkyRegistry.SKY_DIMENSION);
        LevelRegistry.registerLevel("skylevel", SkyLevel.class);
        LevelIdentifier.IDENTIFIER_TO_DIMENSION.put(SkyRegistry.VEIL_IDENTIFIER.stringID, SkyRegistry.VEIL_DIMENSION);
        LevelRegistry.registerLevel("veillevel", VeilLevel.class);
    }

    private void registerBiomes() {
        // countInStats=false: sub-biomes of the Skyreach never generate as
        // surface islands (same flag vanilla uses for its non-surface biomes)
        SkyRegistry.driftlands = BiomeRegistry.registerBiome("driftlands", new DriftlandsBiome(), false);
        SkyRegistry.stormveil = BiomeRegistry.registerBiome("stormveil", new StormveilBiome(), false);
        SkyRegistry.auroraShoals = BiomeRegistry.registerBiome("aurorashoals", new AuroraShoalsBiome(), false);
        SkyRegistry.gloomfen = BiomeRegistry.registerBiome("gloomfen", new GloomfenBiome(), false);
        SkyRegistry.ashenReach = BiomeRegistry.registerBiome("ashenreach", new AshenReachBiome(), false);
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

        SkyRegistry.murkmossTile = new MurkmossTile();
        SkyRegistry.murkmossID = TileRegistry.registerTile("murkmosstile", SkyRegistry.murkmossTile, 1.0F, true);
        SkyRegistry.blackpeatID = TileRegistry.registerTile("blackpeattile", new BlackpeatTile(), 1.0F, true);
        SkyRegistry.ashsandID = TileRegistry.registerTile("ashsandtile", new AshsandTile(), 1.0F, true);
        SkyRegistry.murkwaterID = TileRegistry.registerTile("murkwatertile", new MurkwaterTile(), 0.0F, false);
    }

    private void registerObjects() {
        SkyRegistry.stairwayDown = new SkywardStairwayObject();
        SkyRegistry.stairwayUp = new SkySideStairwayObject();
        SkyRegistry.stairwayDownID = ObjectRegistry.registerObject("skystairwaydown", SkyRegistry.stairwayDown, 20.0F, true);
        SkyRegistry.stairwayUpID = ObjectRegistry.registerObject("skystairwayup", SkyRegistry.stairwayUp, 0.0F, false);
        SkyRegistry.stairwayDown.ladderUpObjectID = SkyRegistry.stairwayUpID;

        // The Veil's rift pair + the seance circle that opens it
        SkyRegistry.veilRiftDown = new VeilRiftObject();
        SkyRegistry.veilRiftUp = new VeilSideRiftObject();
        SkyRegistry.veilRiftDownID = ObjectRegistry.registerObject("veilriftdown", SkyRegistry.veilRiftDown, 0.0F, false);
        SkyRegistry.veilRiftUpID = ObjectRegistry.registerObject("veilriftup", SkyRegistry.veilRiftUp, 0.0F, false);
        SkyRegistry.veilRiftDown.ladderUpObjectID = SkyRegistry.veilRiftUpID;
        SkyRegistry.seanceCircleID = ObjectRegistry.registerObject("seancecircle", new SeanceCircleObject(), 15.0F, true);

        SkyObjects.register();
    }

    public void initResources() {
        SkyMobs.loadTextures();
    }

    public void postInit() {
        SkyItems.registerRecipes();
        SkyBuildingSet.registerRecipes();
        SkyBuildingSet.resolveWorldgenMaterials();
        registerWorldGenerator();
        CommandsManager.registerServerCommand(new SkyreachStatusCommand());
        CommandsManager.registerServerCommand(new VeilStatusCommand());
    }

    private void registerWorldGenerator() {
        WorldGenerator.registerGenerator(new WorldGenerator() {
            @Override
            public Level getNewLevel(LevelIdentifier levelIdentifier, Server server, GameBlackboard blackboard) {
                if (levelIdentifier.equals(SkyRegistry.SKYREACH_IDENTIFIER)) {
                    return new SkyLevel(levelIdentifier, 0, 0, server.world.worldEntity, blackboard.getInt("seed"));
                }
                if (levelIdentifier.equals(SkyRegistry.VEIL_IDENTIFIER)) {
                    return new VeilLevel(levelIdentifier, 0, 0, server.world.worldEntity, blackboard.getInt("seed"));
                }
                return null;
            }
        });
    }

    public void dispose() {
    }
}
